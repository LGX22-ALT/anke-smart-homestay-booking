import assert from 'node:assert/strict'
import test from 'node:test'
import { aiProviders, detectAiProvider, canReuseAiKey } from '../src/constants/aiProviders.js'

test('presets resolve saved endpoint/model pairs without rewriting custom configurations', () => {
  for (const provider of aiProviders) {
    for (const model of provider.models) {
      assert.equal(detectAiProvider(provider.endpoint, model), provider.id)
    }
  }
  assert.equal(
    detectAiProvider('https://gateway.example/v1/chat/completions', 'my-model'),
    'custom',
  )
  assert.equal(detectAiProvider(aiProviders[0].endpoint, 'my-private-model'), 'custom')
  assert.equal(detectAiProvider('', ''), 'deepseek')
})

test('saved credentials cannot be reused for a different endpoint', () => {
  const saved = 'https://api.deepseek.com/chat/completions'
  assert.equal(canReuseAiKey(true, saved, saved), true)
  assert.equal(canReuseAiKey(false, saved, saved), false)
  assert.equal(canReuseAiKey(true, saved, 'https://other.example/chat/completions'), false)
  assert.equal(canReuseAiKey(true, saved, saved + '/'), false)
})
