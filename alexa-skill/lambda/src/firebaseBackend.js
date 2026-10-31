'use strict';

const crypto = require('crypto');
const path = require('path');
const { envelope } = require('./commands');
const { DeviceNotPairedError, DeviceUnreachableError } = require('./errors');

const PAIRING_CODE_TTL_MS = 10 * 60 * 1000;
// Comando que não chega em 30 s é descartado: melhor não executar do que executar atrasado.
const COMMAND_TTL_MS = 30 * 1000;
const UNREACHABLE_FCM_CODES = new Set([
  'messaging/registration-token-not-registered',
  'messaging/invalid-registration-token',
]);

/** O userId da Alexa nunca é gravado em claro. */
function hashUserId(userId) {
  return crypto.createHash('sha256').update(userId).digest('hex');
}

/**
 * Backend real: Firestore guarda o vínculo Alexa -> TV, FCM entrega os comandos.
 * A chave da conta de serviço fica em lambda/firebase-service-account.json
 * (fora do Git; ver docs/AUTO-HOSPEDAGEM.md).
 */
function createFirebaseBackend({ credentialsPath } = {}) {
  let services;

  // Inicialização preguiçosa: só carrega o firebase-admin quando precisa.
  function getServices() {
    if (!services) {
      const { initializeApp, cert } = require('firebase-admin/app');
      const { getFirestore, FieldValue } = require('firebase-admin/firestore');
      const { getMessaging } = require('firebase-admin/messaging');

      const file = credentialsPath || path.join(__dirname, '..', 'firebase-service-account.json');
      const app = initializeApp({ credential: cert(require(file)) }, 'sintoniza');
      const db = getFirestore(app);
      // REST evita o custo do gRPC no cold start da Lambda.
      db.settings({ preferRest: true });
      services = { db, messaging: getMessaging(app), FieldValue };
    }
    return services;
  }

  function linkRef(userId) {
    return getServices().db.collection('links').doc(hashUserId(userId));
  }

  async function getDevice(userId) {
    const { db } = getServices();
    const link = await linkRef(userId).get();
    if (!link.exists) throw new DeviceNotPairedError();

    const device = await db.collection('devices').doc(link.get('uid')).get();
    if (!device.exists || !device.get('fcmToken')) {
      // A TV se desvinculou pelo app: limpa o link órfão.
      await link.ref.delete();
      throw new DeviceNotPairedError();
    }
    return device.data();
  }

  return {
    /** @returns {Promise<{status: 'ok' | 'invalid' | 'expired'}>} */
    async pairWithCode(userId, code) {
      const { db, FieldValue } = getServices();
      const codeRef = db.collection('pairingCodes').doc(String(code));

      return db.runTransaction(async (tx) => {
        const snap = await tx.get(codeRef);
        if (!snap.exists) return { status: 'invalid' };

        // Código é de uso único, mesmo se expirado.
        tx.delete(codeRef);
        const createdAt = snap.get('createdAt');
        if (!createdAt || Date.now() - createdAt.toMillis() > PAIRING_CODE_TTL_MS) {
          return { status: 'expired' };
        }

        const uid = snap.get('uid');
        tx.set(linkRef(userId), { uid, linkedAt: FieldValue.serverTimestamp() });
        tx.set(db.collection('devices').doc(uid), { pairedAt: FieldValue.serverTimestamp() }, { merge: true });
        return { status: 'ok' };
      });
    },

    async isPaired(userId) {
      return (await linkRef(userId).get()).exists;
    },

    /** @returns {Promise<boolean>} se havia TV pareada */
    async unpair(userId) {
      const { db, FieldValue } = getServices();
      const ref = linkRef(userId);
      const link = await ref.get();
      if (!link.exists) return false;
      await ref.delete();

      // Era a última Alexa desta TV: tira o "Pareado" da tela e faz o app voltar a mostrar o código.
      const uid = link.get('uid');
      const others = await db.collection('links').where('uid', '==', uid).limit(1).get();
      if (others.empty) {
        await db.collection('devices').doc(uid)
          .update({ pairedAt: FieldValue.delete() })
          .catch((err) => console.warn('Não limpei pairedAt de', uid, err.code));
      }
      return true;
    },

    async sendCommand(userId, command) {
      const device = await getDevice(userId);
      try {
        await getServices().messaging.send({
          token: device.fcmToken,
          data: { cmd: JSON.stringify(envelope(command)) },
          android: { priority: 'high', ttl: COMMAND_TTL_MS },
        });
      } catch (err) {
        if (UNREACHABLE_FCM_CODES.has(err.code)) throw new DeviceUnreachableError(err.message);
        throw err;
      }
    },
  };
}

module.exports = { createFirebaseBackend, hashUserId, PAIRING_CODE_TTL_MS };
