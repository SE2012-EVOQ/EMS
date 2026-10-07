import test from 'node:test'
import assert from 'node:assert/strict'
import { requestErrorMessage } from '../src/components/common/requestError.js'

test('connection failure has useful unavailable guidance', () => {
  const message = requestErrorMessage(new TypeError('Failed to fetch'), 'Attendance')
  assert.match(message, /Cannot connect/)
  assert.match(message, /Attendance is unavailable/)
  assert.doesNotMatch(message, /Failed to fetch/)
})
test('server errors do not expose implementation messages', () => {
  const error = Object.assign(new Error('SQL exception'), { status: 500 })
  assert.equal(requestErrorMessage(error, 'Dashboard'), 'Dashboard is temporarily unavailable. Please try again.')
})
test('permission and validation messages remain actionable', () => {
  for (const [status, message] of [[403, 'Access denied'], [400, 'The end date must be on or after the start date.'], [409, 'Attendance already exists']]) {
    assert.equal(requestErrorMessage(Object.assign(new Error(message), { status }), 'Attendance'), message)
  }
})
