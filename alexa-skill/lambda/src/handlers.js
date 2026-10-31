'use strict';

const Alexa = require('ask-sdk-core');
const commands = require('./commands');
const S = require('./speech');
const { DeviceNotPairedError, DeviceUnreachableError } = require('./errors');

const PAIRING_CODE_PATTERN = /^[1-9]\d{5}$/;

function isRequest(h, type) {
  return Alexa.getRequestType(h.requestEnvelope) === type;
}

function isIntent(h, ...names) {
  return isRequest(h, 'IntentRequest') && names.includes(Alexa.getIntentName(h.requestEnvelope));
}

function userId(h) {
  return Alexa.getUserId(h.requestEnvelope);
}

function pendingUnpair(h) {
  return h.attributesManager.getSessionAttributes().confirmUnpair === true;
}

/**
 * "Alexa, pede pro Sintoniza pausar" chega com sessão nova: responde e encerra.
 * Depois de "Alexa, abrir Sintoniza" a sessão fica aberta para comandos em sequência
 * ("baixo", "baixo", "ok") sem repetir o nome da skill.
 */
function isOneShot(h) {
  const session = h.requestEnvelope.session;
  return !session || session.new;
}

function speak(h, speech) {
  return h.responseBuilder.speak(speech).withShouldEndSession(true).getResponse();
}

function ask(h, speech, reprompt = speech) {
  return h.responseBuilder.speak(speech).reprompt(reprompt).getResponse();
}

function slotValue(h, name) {
  const slot = Alexa.getSlot(h.requestEnvelope, name);
  return slot && slot.value && slot.value !== '?' ? slot.value : null;
}

/** Valor canônico (id) do slot quando bate com o catálogo; senão só o que foi falado. */
function resolvedSlot(h, name) {
  const slot = Alexa.getSlot(h.requestEnvelope, name);
  if (!slot || !slot.value) return null;

  const authorities = (slot.resolutions && slot.resolutions.resolutionsPerAuthority) || [];
  for (const authority of authorities) {
    if (authority.status && authority.status.code === 'ER_SUCCESS_MATCH' && authority.values && authority.values.length) {
      const { id, name: canonical } = authority.values[0].value;
      return { id, name: canonical, spoken: slot.value };
    }
  }
  return { id: null, name: slot.value, spoken: slot.value };
}

/** "avançar 5 minutos" → 300 s; "avançar" sem tempo → padrão do app. */
function seekCommand(h, action) {
  const spoken = slotValue(h, 'duration');
  const seconds = commands.parseDuration(spoken);
  if (spoken && !seconds) return { ask: S.whichDuration };
  const command = commands.media(action, seconds);
  const speech = seconds ? S.seeking(action === 'fast_forward', command.seconds) : S.ok;
  return { command, speech };
}

async function sendAndRespond(h, backend, command, speech) {
  try {
    await backend.sendCommand(userId(h), command);
  } catch (err) {
    if (err instanceof DeviceNotPairedError) return ask(h, S.notPaired, S.askCode);
    if (err instanceof DeviceUnreachableError) return speak(h, S.unreachable);
    throw err;
  }
  return isOneShot(h) ? speak(h, speech) : ask(h, speech, S.reprompt);
}

/**
 * Cria um handler de comando. `build` devolve `{ command, speech }` para enviar,
 * ou `{ ask }` quando falta informação.
 */
function commandHandler(backend, intentName, build) {
  return {
    canHandle: (h) => isIntent(h, intentName),
    async handle(h) {
      const result = build(h);
      if (result.ask) return ask(h, result.ask);
      return sendAndRespond(h, backend, result.command, result.speech);
    },
  };
}

function createHandlers(backend) {
  const fixed = (command, speech = S.ok) => () => ({ command, speech });

  return [
    {
      canHandle: (h) => isRequest(h, 'LaunchRequest'),
      async handle(h) {
        const paired = await backend.isPaired(userId(h));
        return paired ? ask(h, S.welcomePaired, S.reprompt) : ask(h, S.welcomeNotPaired, S.askCode);
      },
    },

    {
      canHandle: (h) => isIntent(h, 'PairIntent'),
      async handle(h) {
        const code = slotValue(h, 'code');
        if (!code) return ask(h, S.askCode);
        if (!PAIRING_CODE_PATTERN.test(code)) return ask(h, S.invalidCodeFormat, S.askCode);

        const { status } = await backend.pairWithCode(userId(h), code);
        if (status === 'ok') return isOneShot(h) ? speak(h, S.paired) : ask(h, S.paired, S.reprompt);
        if (status === 'expired') return speak(h, S.codeExpired);
        return ask(h, S.codeNotFound, S.askCode);
      },
    },

    // Desparear é destrutivo e a Alexa às vezes confunde frases ("abrir o unitv"),
    // então sempre confirma antes.
    {
      canHandle: (h) => isIntent(h, 'UnpairIntent'),
      handle(h) {
        h.attributesManager.setSessionAttributes({ confirmUnpair: true });
        return ask(h, S.confirmUnpair);
      },
    },
    {
      canHandle: (h) => isIntent(h, 'AMAZON.YesIntent') && pendingUnpair(h),
      async handle(h) {
        h.attributesManager.setSessionAttributes({});
        const existed = await backend.unpair(userId(h));
        return speak(h, existed ? S.unpaired : S.wasNotPaired);
      },
    },
    {
      canHandle: (h) => isIntent(h, 'AMAZON.NoIntent') && pendingUnpair(h),
      handle(h) {
        h.attributesManager.setSessionAttributes({});
        return speak(h, S.keptPaired);
      },
    },

    commandHandler(backend, 'OpenAppIntent', (h) => {
      const app = resolvedSlot(h, 'app');
      if (!app) return { ask: S.whichApp };
      return { command: commands.openApp(app.id, app.spoken), speech: S.opening(app.name) };
    }),

    commandHandler(backend, 'YouTubeSearchIntent', (h) => {
      const query = slotValue(h, 'query');
      if (!query) return { ask: S.whatToSearch };
      return { command: commands.youtubeSearch(query), speech: S.searching(query) };
    }),

    commandHandler(backend, 'PauseIntent', fixed(commands.media('pause'))),
    commandHandler(backend, 'ResumeIntent', fixed(commands.media('play'))),
    commandHandler(backend, 'NextIntent', fixed(commands.media('next'))),
    commandHandler(backend, 'PreviousIntent', fixed(commands.media('previous'))),
    commandHandler(backend, 'FastForwardIntent', (h) => seekCommand(h, 'fast_forward')),
    commandHandler(backend, 'RewindIntent', (h) => seekCommand(h, 'rewind')),

    commandHandler(backend, 'VolumeUpIntent', (h) => ({
      command: commands.volumeUp(slotValue(h, 'steps')),
      speech: S.ok,
    })),
    commandHandler(backend, 'VolumeDownIntent', (h) => ({
      command: commands.volumeDown(slotValue(h, 'steps')),
      speech: S.ok,
    })),
    commandHandler(backend, 'SetVolumeIntent', (h) => {
      const level = slotValue(h, 'level');
      if (level === null || Number.isNaN(Number.parseInt(level, 10))) return { ask: S.whichLevel };
      const command = commands.volumeSet(level);
      return { command, speech: S.volumeSet(command.level) };
    }),
    commandHandler(backend, 'MuteIntent', fixed(commands.mute())),
    commandHandler(backend, 'UnmuteIntent', fixed(commands.unmute())),

    commandHandler(backend, 'NavigateIntent', (h) => {
      const direction = resolvedSlot(h, 'direction');
      if (!direction || !direction.id) return { ask: S.whichDirection };
      return { command: commands.key(direction.id, slotValue(h, 'times')), speech: S.ok };
    }),

    {
      canHandle: (h) => isIntent(h, 'AMAZON.HelpIntent'),
      handle: (h) => ask(h, S.help, S.reprompt),
    },
    {
      canHandle: (h) => isIntent(h, 'AMAZON.CancelIntent', 'AMAZON.StopIntent', 'AMAZON.NavigateHomeIntent'),
      handle: (h) => speak(h, S.bye),
    },
    {
      // "Sim"/"não" fora de uma confirmação pendente.
      canHandle: (h) => isIntent(h, 'AMAZON.FallbackIntent', 'AMAZON.YesIntent', 'AMAZON.NoIntent'),
      handle: (h) => ask(h, S.fallback, S.reprompt),
    },
    {
      canHandle: (h) => isRequest(h, 'SessionEndedRequest'),
      handle: (h) => h.responseBuilder.getResponse(),
    },
  ];
}

const errorHandler = {
  canHandle: () => true,
  handle(h, error) {
    console.error('Erro na skill:', error);
    return speak(h, S.error);
  },
};

/** Monta a skill com um backend injetável (Firebase em produção, fake nos testes). */
function buildSkill(backend) {
  return Alexa.SkillBuilders.custom()
    .addRequestHandlers(...createHandlers(backend))
    .addErrorHandlers(errorHandler)
    .withCustomUserAgent('sintoniza/0.1');
}

module.exports = { buildSkill };
