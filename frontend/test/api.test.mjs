import test from 'node:test'
import assert from 'node:assert/strict'
import { apiRequest, clearCsrfToken, onSessionExpired } from '../src/services/api.js'

const response = (status, data) => new Response(JSON.stringify(data), { status, headers: { 'Content-Type': 'application/json' } })
test('a later 401 clears the session centrally', async () => {
  let expired = 0
  const unsubscribe = onSessionExpired(() => expired++)
  globalThis.fetch = async () => response(401, { message: 'Sign in required' })
  await assert.rejects(apiRequest('/attendance-reports'), error => error.status === 401)
  assert.equal(expired, 1); unsubscribe()
})
test('permission 403 does not clear authenticated state', async () => {
  let expired = 0
  const unsubscribe = onSessionExpired(() => expired++)
  globalThis.fetch = async () => response(403, { message: 'Access denied' })
  await assert.rejects(apiRequest('/attendance-reports'), error => error.status === 403)
  assert.equal(expired, 0); unsubscribe()
})
test('failed login does not masquerade as expiry', async () => {
  clearCsrfToken(); let expired = 0
  const unsubscribe = onSessionExpired(() => expired++)
  globalThis.fetch = async url => url.endsWith('/auth/csrf') ? response(200, { headerName: 'X-CSRF-TOKEN', token: 'test-token' }) : response(401, { message: 'Invalid username or password' })
  await assert.rejects(apiRequest('/auth/login', { method: 'POST', body: new URLSearchParams() }))
  assert.equal(expired, 0); unsubscribe()
})
test('expired CSRF acquisition also clears the session', async () => {
  clearCsrfToken(); let expired = 0
  const unsubscribe = onSessionExpired(() => expired++)
  globalThis.fetch = async () => response(401, { message: 'Sign in required' })
  await assert.rejects(apiRequest('/attendance-records/check-in', { method: 'POST' }), error => error.status === 401)
  assert.equal(expired, 1); unsubscribe()
})
test('CSV uses the same authenticated request and returns a Blob', async () => {
  globalThis.fetch = async (_, options) => { assert.equal(options.credentials, 'include'); return new Response('Employee,Date\r\n', { headers: { 'Content-Type': 'text/csv' } }) }
  const blob = await apiRequest('/attendance-reports/csv', { responseType: 'blob' })
  assert.equal(await blob.text(), 'Employee,Date\r\n')
})
