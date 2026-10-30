'use strict';

const { createFirebaseBackend } = require('./src/firebaseBackend');
const { buildSkill } = require('./src/handlers');

exports.handler = buildSkill(createFirebaseBackend()).lambda();
