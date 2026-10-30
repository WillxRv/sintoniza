'use strict';

const test = require('node:test');
const assert = require('node:assert/strict');
const { buildSkill } = require('../src/handlers');
const { DeviceNotPairedError, DeviceUnreachableError } = require('../src/errors');

const USER = 'amzn1.ask.account.TESTE';

function fakeBackend({ paired = true, codes = {}, sendError = null } = {}) {
  const sent = [];
  return {
    sent,
    paired,
    async isPaired() {
      return this.paired;
    },
    async pairWithCode(_user, code) {
      const status = codes[code] || 'invalid';
      if (status === 'ok') this.paired = true;
      return { status };
    },
    async unpair() {
      const was = this.paired;
      this.paired = false;
      return was;
    },
    async sendCommand(user, command) {
      if (sendError) throw sendError;
      if (!this.paired) throw new DeviceNotPairedError();
      sent.push({ user, command });
    },
  };
}

function slot(name, value) {
  if (value === undefined) return { name };
  if (typeof value === 'string') return { name, value };
  const s = { name, value: value.spoken };
  s.resolutions = {
    resolutionsPerAuthority: [
      value.id
        ? { authority: 'custom', status: { code: 'ER_SUCCESS_MATCH' }, values: [{ value: { id: value.id, name: value.name } }] }
        : { authority: 'custom', status: { code: 'ER_SUCCESS_NO_MATCH' } },
    ],
  };
  return s;
}

function envelope(request, { newSession = true } = {}) {
  return {
    version: '1.0',
    session: {
      new: newSession,
      sessionId: 'sessao',
      application: { applicationId: 'skill' },
      user: { userId: USER },
    },
    context: {
      System: {
        application: { applicationId: 'skill' },
        user: { userId: USER },
        device: { deviceId: 'echo', supportedInterfaces: {} },
        apiEndpoint: 'https://api.amazonalexa.com',
      },
    },
    request: { requestId: 'req', timestamp: new Date().toISOString(), locale: 'pt-BR', ...request },
  };
}

function intent(name, slots = {}, opts) {
  const mapped = Object.fromEntries(Object.entries(slots).map(([k, v]) => [k, slot(k, v)]));
  return envelope({ type: 'IntentRequest', intent: { name, confirmationStatus: 'NONE', slots: mapped } }, opts);
}

async function run(backend, env) {
  const { response } = await buildSkill(backend).create().invoke(env);
  return {
    speech: response.outputSpeech ? response.outputSpeech.ssml : '',
    ended: response.shouldEndSession === true,
    reprompt: Boolean(response.reprompt),
  };
}

test('abrir app conhecido envia id canônico e encerra no modo one-shot', async () => {
  const backend = fakeBackend();
  const res = await run(backend, intent('OpenAppIntent', { app: { spoken: 'netflics', id: 'netflix', name: 'netflix' } }));
  assert.deepEqual(backend.sent[0].command, { type: 'open_app', app: 'netflix', name: 'netflics' });
  assert.match(res.speech, /Abrindo netflix/);
  assert.equal(res.ended, true);
});

test('abrir app desconhecido envia só o nome falado', async () => {
  const backend = fakeBackend();
  await run(backend, intent('OpenAppIntent', { app: { spoken: 'claro tv' } }));
  assert.deepEqual(backend.sent[0].command, { type: 'open_app', app: null, name: 'claro tv' });
});

test('abrir app sem nome pergunta qual', async () => {
  const backend = fakeBackend();
  const res = await run(backend, intent('OpenAppIntent', { app: undefined }));
  assert.equal(backend.sent.length, 0);
  assert.match(res.speech, /Qual aplicativo/);
  assert.equal(res.reprompt, true);
});

test('em sessão aberta, comando mantém a sessão para o próximo', async () => {
  const backend = fakeBackend();
  const res = await run(backend, intent('PauseIntent', {}, { newSession: false }));
  assert.deepEqual(backend.sent[0].command, { type: 'media', action: 'pause' });
  assert.equal(res.ended, false);
  assert.equal(res.reprompt, true);
});

test('navegação com repetição é limitada a 10', async () => {
  const backend = fakeBackend();
  await run(backend, intent('NavigateIntent', { direction: { spoken: 'pra baixo', id: 'down', name: 'baixo' }, times: '50' }));
  assert.deepEqual(backend.sent[0].command, { type: 'key', key: 'down', times: 10 });
});

test('navegação com direção não reconhecida pergunta de novo', async () => {
  const backend = fakeBackend();
  const res = await run(backend, intent('NavigateIntent', { direction: { spoken: 'diagonal' } }));
  assert.equal(backend.sent.length, 0);
  assert.match(res.speech, /Não entendi a direção/);
});

test('volume: padrão de 10% e nível limitado a 100', async () => {
  const backend = fakeBackend();
  await run(backend, intent('VolumeUpIntent', { steps: undefined }));
  await run(backend, intent('SetVolumeIntent', { level: '150' }));
  assert.deepEqual(backend.sent[0].command, { type: 'volume', action: 'up', percent: 10 });
  assert.deepEqual(backend.sent[1].command, { type: 'volume', action: 'set', level: 100 });
});

test('volume sem nível pergunta o número', async () => {
  const backend = fakeBackend();
  const res = await run(backend, intent('SetVolumeIntent', { level: '?' }));
  assert.equal(backend.sent.length, 0);
  assert.match(res.speech, /zero a cem/);
});

test('busca no youtube escapa caracteres de SSML', async () => {
  const backend = fakeBackend();
  const res = await run(backend, intent('YouTubeSearchIntent', { query: 'tom & jerry' }));
  assert.deepEqual(backend.sent[0].command, { type: 'youtube_search', query: 'tom & jerry' });
  assert.match(res.speech, /tom &amp; jerry/);
});

test('comando sem TV pareada pede o código e mantém a sessão', async () => {
  const backend = fakeBackend({ paired: false });
  const res = await run(backend, intent('PauseIntent'));
  assert.match(res.speech, /ainda não está pareada/);
  assert.equal(res.ended, false);
});

test('TV inalcançável avisa e encerra', async () => {
  const backend = fakeBackend({ sendError: new DeviceUnreachableError() });
  const res = await run(backend, intent('PauseIntent'));
  assert.match(res.speech, /Não consegui falar com a TV/);
  assert.equal(res.ended, true);
});

test('pareamento: código válido, inválido, expirado e mal formatado', async () => {
  const backend = fakeBackend({ paired: false, codes: { 482913: 'ok', 111111: 'expired' } });

  assert.match((await run(backend, intent('PairIntent', { code: '12' }))).speech, /seis dígitos/);
  assert.match((await run(backend, intent('PairIntent', { code: '012345' }))).speech, /seis dígitos/);
  assert.match((await run(backend, intent('PairIntent', { code: '999999' }))).speech, /Não encontrei/);
  assert.match((await run(backend, intent('PairIntent', { code: '111111' }))).speech, /expirou/);

  const ok = await run(backend, intent('PairIntent', { code: '482913' }));
  assert.match(ok.speech, /pareada/);
  assert.equal(backend.paired, true);
});

test('abrir a skill sem TV pareada explica o pareamento', async () => {
  const res = await run(fakeBackend({ paired: false }), envelope({ type: 'LaunchRequest' }));
  assert.match(res.speech, /código de seis dígitos/);
  assert.equal(res.ended, false);
});

test('desparear', async () => {
  const backend = fakeBackend();
  assert.match((await run(backend, intent('UnpairIntent'))).speech, /esqueci sua TV/);
  assert.match((await run(backend, intent('UnpairIntent'))).speech, /Não tem nenhuma TV/);
});

test('erro inesperado do backend vira mensagem amigável', async () => {
  const backend = fakeBackend({ sendError: new Error('boom') });
  const original = console.error;
  console.error = () => {};
  try {
    const res = await run(backend, intent('PauseIntent'));
    assert.match(res.speech, /algo deu errado/);
  } finally {
    console.error = original;
  }
});
