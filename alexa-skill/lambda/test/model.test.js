'use strict';

// Confere se o modelo de interação e o código estão em sincronia.

const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('fs');
const path = require('path');

const model = JSON.parse(
  fs.readFileSync(path.join(__dirname, '..', '..', 'skill-package', 'interactionModels', 'custom', 'pt-BR.json'), 'utf8'),
).interactionModel.languageModel;

const handlersSource = fs.readFileSync(path.join(__dirname, '..', 'src', 'handlers.js'), 'utf8');

test('todo intent do modelo tem handler', () => {
  for (const intent of model.intents) {
    assert.ok(handlersSource.includes(`'${intent.name}'`), `sem handler para ${intent.name}`);
  }
});

test('todo slot usado nas frases está declarado', () => {
  for (const intent of model.intents) {
    const declared = new Set((intent.slots || []).map((s) => s.name));
    for (const sample of intent.samples) {
      for (const [, name] of sample.matchAll(/\{(\w+)\}/g)) {
        assert.ok(declared.has(name), `${intent.name}: slot {${name}} não declarado em "${sample}"`);
      }
    }
  }
});

test('não há frases duplicadas entre intents', () => {
  const seen = new Map();
  for (const intent of model.intents) {
    for (const sample of intent.samples) {
      const key = sample.toLowerCase();
      assert.ok(!seen.has(key), `"${sample}" está em ${seen.get(key)} e ${intent.name}`);
      seen.set(key, intent.name);
    }
  }
});

test('ids de NAV_KEY batem com as teclas que o app entende', () => {
  const navKey = model.types.find((t) => t.name === 'NAV_KEY');
  const ids = navKey.values.map((v) => v.id).sort();
  assert.deepEqual(ids, ['back', 'down', 'home', 'left', 'ok', 'right', 'up']);
});
