'use strict';

const crypto = require('crypto');

/**
 * Protocolo de comandos skill -> app (ver docs/ARQUITETURA.md).
 * Mudanças incompatíveis devem incrementar PROTOCOL_VERSION.
 */
const PROTOCOL_VERSION = 1;

const MAX_KEY_REPEAT = 10;
const DEFAULT_VOLUME_STEP_PERCENT = 10;

function clampInt(value, min, max, fallback) {
  const n = Number.parseInt(value, 10);
  if (Number.isNaN(n)) return fallback;
  return Math.min(max, Math.max(min, n));
}

const openApp = (app, name) => ({ type: 'open_app', app: app || null, name: name || null });

const youtubeSearch = (query) => ({ type: 'youtube_search', query });

const MAX_SEEK_SECONDS = 6 * 60 * 60;

/**
 * action: play | pause | next | previous | fast_forward | rewind
 * seconds: só para fast_forward/rewind; omitido = padrão do app (15 s).
 */
function media(action, seconds) {
  const command = { type: 'media', action };
  if (seconds) command.seconds = Math.min(seconds, MAX_SEEK_SECONDS);
  return command;
}

/**
 * Converte a duração ISO 8601 do slot AMAZON.DURATION ("PT5M", "PT1H30M", "PT45S")
 * em segundos. Devolve null se não for uma duração de tempo válida (ex.: "P2D", "?").
 */
function parseDuration(value) {
  const match = /^PT(?:(\d+(?:\.\d+)?)H)?(?:(\d+(?:\.\d+)?)M)?(?:(\d+(?:\.\d+)?)S)?$/.exec(value || '');
  if (!match || value === 'PT') return null;
  const [, h = 0, m = 0, s = 0] = match;
  const seconds = Math.round(Number(h) * 3600 + Number(m) * 60 + Number(s));
  return seconds > 0 ? seconds : null;
}

/** Sobe/desce em pontos percentuais do volume máximo. */
const volumeUp = (percent) => ({
  type: 'volume',
  action: 'up',
  percent: clampInt(percent, 1, 100, DEFAULT_VOLUME_STEP_PERCENT),
});

const volumeDown = (percent) => ({
  type: 'volume',
  action: 'down',
  percent: clampInt(percent, 1, 100, DEFAULT_VOLUME_STEP_PERCENT),
});

const volumeSet = (level) => ({ type: 'volume', action: 'set', level: clampInt(level, 0, 100, 0) });

const mute = () => ({ type: 'volume', action: 'mute' });

const unmute = () => ({ type: 'volume', action: 'unmute' });

/** key: up | down | left | right | ok | back | home */
const key = (name, times) => ({ type: 'key', key: name, times: clampInt(times, 1, MAX_KEY_REPEAT, 1) });

/** Envelope enviado no campo `cmd` da mensagem FCM. */
function envelope(command, now = Date.now()) {
  return { v: PROTOCOL_VERSION, id: crypto.randomUUID(), sentAt: now, ...command };
}

module.exports = {
  PROTOCOL_VERSION,
  clampInt,
  openApp,
  youtubeSearch,
  media,
  parseDuration,
  volumeUp,
  volumeDown,
  volumeSet,
  mute,
  unmute,
  key,
  envelope,
};
