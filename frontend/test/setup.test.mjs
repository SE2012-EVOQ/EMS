import test from 'node:test'
import assert from 'node:assert/strict'
import { loadEntryState, setupService } from '../src/services/setupService.js'
import { clearCsrfToken } from '../src/services/api.js'

const json = (data, status = 200) => new Response(JSON.stringify(data), { status, headers: { 'Content-Type': 'application/json' } })

test('fresh installation chooses setup without requesting an authenticated session', async () => {
  globalThis.fetch = async () => json({ required: true })
  const entry = await loadEntryState(() => assert.fail('must not fetch a user before setup'))
  assert.deepEqual(entry, { setupRequired: true, user: null })
})
test('completed installation chooses normal login even without a current session', async () => {
  globalThis.fetch = async () => json({ required: false })
  const entry = await loadEntryState(async () => { throw Object.assign(new Error('Sign in required'), { status: 401 }) })
  assert.deepEqual(entry, { setupRequired: false, user: null })
})
test('existing authenticated account survives a completed setup status', async () => {
  globalThis.fetch = async () => json({ required: false })
  const user = { username: 'manager', role: 'MANAGER_ADMIN' }
  assert.deepEqual(await loadEntryState(async () => user), { setupRequired: false, user })
})
test('failed setup status does not masquerade as an open installation', async () => {
  globalThis.fetch = async () => json({ message: 'Unavailable' }, 503)
  await assert.rejects(loadEntryState(() => assert.fail('must not fetch a user after status failure')), error => error.status === 503)
})
test('malformed setup status is unavailable rather than an assumed normal login', async () => {
  globalThis.fetch = async () => json({})
  await assert.rejects(loadEntryState(() => assert.fail('must not fetch a user after malformed status')))
})
test('setup submission sends JSON through the existing CSRF-protected session', async () => {
  clearCsrfToken()
  const details = { username: 'first.admin', password: 'test-only-long-password' }
  globalThis.fetch = async (url, options) => {
    if (url.endsWith('/auth/csrf')) return json({ headerName: 'X-CSRF-TOKEN', token: 'setup-token' })
    assert.ok(url.endsWith('/auth/setup'))
    assert.equal(options.method, 'POST')
    assert.equal(options.credentials, 'include')
    assert.equal(options.headers.get('X-CSRF-TOKEN'), 'setup-token')
    assert.deepEqual(JSON.parse(options.body), details)
    return json({ required: false }, 201)
  }
  assert.deepEqual(await setupService.create(details), { required: false })
})
test('a stale setup form receives the completion conflict instead of another account', async () => {
  clearCsrfToken()
  globalThis.fetch = async url => url.endsWith('/auth/csrf') ? json({ headerName: 'X-CSRF-TOKEN', token: 'setup-token' }) : json({ message: 'Setup already completed' }, 409)
  await assert.rejects(setupService.create({}), error => error.status === 409)
})
