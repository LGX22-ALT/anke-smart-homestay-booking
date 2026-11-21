import assert from 'node:assert/strict'
import test from 'node:test'
import { validateAvatarFile, validateProfileName } from '../src/utils/profileValidation.js'

test('profile names trim whitespace and reject blank or oversized values', () => {
  assert.equal(validateProfileName('  小林的旅途  '), '')
  assert.match(validateProfileName('   '), /名称/)
  assert.match(validateProfileName('a'.repeat(65)), /64/)
})
test('avatar selection accepts supported files and rejects empty, oversized or unsupported ones', () => {
  assert.equal(validateAvatarFile({ type: 'image/png', size: 1024 }), '')
  assert.equal(validateAvatarFile({ type: 'image/jpeg', size: 5 * 1024 * 1024 }), '')
  assert.match(validateAvatarFile({ type: 'image/png', size: 0 }), /空/)
  assert.match(validateAvatarFile({ type: 'image/png', size: 5 * 1024 * 1024 + 1 }), /5MB/)
  assert.match(validateAvatarFile({ type: 'image/svg+xml', size: 50 }), /JPG/)
})
