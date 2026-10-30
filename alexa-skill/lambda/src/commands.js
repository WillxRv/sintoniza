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

/** action: play | pause | next | previous | fast_forward | rewind */
const media = (action) => ({ type: 'media', action });

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
  volumeUp,
  volumeDown,
  volumeSet,
  mute,
  unmute,
  key,
  envelope,
};
