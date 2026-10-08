import test, { after } from 'node:test'
import assert from 'node:assert/strict'
import React from 'react'
import { renderToStaticMarkup } from 'react-dom/server'
import { createServer } from 'vite'

const server = await createServer({ server: { watch: null }, appType: 'custom' })
after(() => server.close())
const { default: PendingLeaveRequests } = await server.ssrLoadModule('/src/modules/leave/components/PendingLeaveRequests.jsx')
const { getPendingLeaveRequests } = await server.ssrLoadModule('/src/modules/leave/services/leaveService.js')
const request = { id: 3, employeeId: 1, employeeName: 'Fixture', leaveType: 'Personal',
  startDate: '2026-10-09', endDate: '2026-10-09', days: 1, status: 'PENDING' }
const render = (role, rows) => renderToStaticMarkup(React.createElement(PendingLeaveRequests, {
  requests: rows, user: { employeeId: 1, role }, onDecision: () => {}
}))

test('Manager own pending leave exposes the explicitly authorized self-approval action', () => {
  const html = render('MANAGER_ADMIN', [request])
  assert.match(html, /Approve my leave/)
  assert.doesNotMatch(html, />Reject<\/button>/)
  assert.match(html, /You can also approve your own leave/)
})

test('both approver roles see approve and reject on direct-report queue entries', () => {
  for (const role of ['SUPERVISOR', 'MANAGER_ADMIN']) {
    const html = render(role, [{ ...request, employeeId: 2 }])
    assert.match(html, />Approve<\/button>/)
    assert.match(html, />Reject<\/button>/)
    assert.doesNotMatch(html, /Approve my leave/)
  }
})

test('Employee, Supervisor own leave and already decided requests expose no decision buttons', () => {
  for (const html of [render('EMPLOYEE', [request]), render('SUPERVISOR', [request]),
    render('MANAGER_ADMIN', [{ ...request, status: 'APPROVED' }]), render('MANAGER_ADMIN', [{ ...request, status: 'REJECTED' }])]) {
    assert.doesNotMatch(html, /<button/)
  }
})

test('approval queue request uses the common authenticated endpoint with no employee override', async () => {
  const rows = [request]
  globalThis.fetch = async (url, options) => {
    assert.ok(url.endsWith('/leave/pending'))
    assert.equal(options.credentials, 'include')
    return new Response(JSON.stringify(rows), { headers: { 'Content-Type': 'application/json' } })
  }
  assert.deepEqual(await getPendingLeaveRequests(), rows)
})
