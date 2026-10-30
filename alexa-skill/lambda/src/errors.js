'use strict';

/** O usuário da Alexa não tem TV pareada (ou a TV foi desvinculada). */
class DeviceNotPairedError extends Error {
  constructor(message = 'device not paired') {
    super(message);
    this.name = 'DeviceNotPairedError';
  }
}

/** O token FCM da TV não é mais válido (app desinstalado, dados limpos…). */
class DeviceUnreachableError extends Error {
  constructor(message = 'device unreachable') {
    super(message);
    this.name = 'DeviceUnreachableError';
  }
}

module.exports = { DeviceNotPairedError, DeviceUnreachableError };
