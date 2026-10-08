import test, { after } from 'node:test'
import assert from 'node:assert/strict'
import React from 'react'
import { renderToStaticMarkup } from 'react-dom/server'
import { createServer } from 'vite'
import { createEmployeePayload } from '../src/modules/employees/components/employeeForm.js'

// Transform the actual JSX through the project's Vite configuration.
const server = await createServer({ server: { watch: null }, appType: 'custom' })
after(() => server.close())
const { default: CreateEmployeeModal } = await server.ssrLoadModule('/src/modules/employees/components/CreateEmployeeModal.jsx')
const { default: EditOfficialModal } = await server.ssrLoadModule('/src/modules/employees/components/EditOfficialModal.jsx')
const { default: EmployeeProfileModal } = await server.ssrLoadModule('/src/modules/employees/components/EmployeeProfileModal.jsx')
const { default: EmployeeTable } = await server.ssrLoadModule('/src/modules/employees/components/EmployeeTable.jsx')
const { AssignmentTable } = await server.ssrLoadModule('/src/modules/assets/pages/AssetsPage.jsx')
const render = (component, props) => renderToStaticMarkup(React.createElement(component, props))

test('Manager creation offers Active by default and an explicit Inactive option', () => {
  const html = render(CreateEmployeeModal, { open: true })
  const selector = html.match(/<select[^>]*aria-label="Lifecycle Status"[^>]*>(.*?)<\/select>/s)?.[1]
  assert.ok(selector, 'creation must expose the status selector')
  assert.match(selector, /<option value="ACTIVE" selected="">Active<\/option>/)
  assert.match(selector, /<option value="INACTIVE">Inactive<\/option>/)
  assert.match(html, /Inactive employees cannot log in/)
})

test('official editor retains Active and Inactive lifecycle choices and account explanation', () => {
  const html = render(EditOfficialModal, { open: true, employee: { id: 1, fullName: 'Fixture' } })
  const selector = html.match(/<select[^>]*aria-label="Lifecycle Status"[^>]*>(.*?)<\/select>/s)?.[1]
  assert.ok(selector)
  assert.match(selector, /value="ACTIVE"/)
  assert.match(selector, /value="INACTIVE"/)
  assert.match(html, /Changing status also updates their linked account/)
})

test('returned assignment uses its response label when current inventory is empty or reassigned', () => {
  const rows = [{ assignmentId: 3, assetId: 2, assetName: 'Returned laptop', employeeId: 1,
    assignedDate: '2026-01-01', returnedDate: '2026-01-02', assignmentStatus: 'RETURNED' }]
  for (const assets of [[], [{ assetId: 9, assetName: 'Other equipment' }], [{ assetId: 2, assetName: 'Stale inventory label' }]]) {
    const html = render(AssignmentTable, { rows, assets, employees: [{ id: 1, fullName: 'Fixture' }] })
    assert.match(html, /Returned laptop/)
    assert.match(html, /RETURNED/)
    assert.doesNotMatch(html, /Asset #2|Other equipment|Stale inventory label/)
  }
})

test('creation sends the selected lifecycle status alongside optional login provisioning', () => {
  const form = { firstName: ' Test ', lastName: ' Employee ', email: ' test@example.invalid ',
    phone: '', address: '', jobTitle: ' Engineer ', hireDate: '2026-01-01', departmentId: '2',
    teamId: '', supervisorId: '', username: ' test.employee ', password: 'test-password', role: 'EMPLOYEE' }
  for (const status of ['ACTIVE', 'INACTIVE']) {
    for (const createAccount of [false, true]) {
      const payload = createEmployeePayload({ ...form, status, createAccount })
      assert.equal(payload.status, status)
      assert.equal(payload.createAccount, createAccount)
      assert.equal(payload.departmentId, 2)
      assert.equal(payload.teamId, null)
      assert.equal(payload.supervisorId, null)
      assert.equal(payload.username, createAccount ? 'test.employee' : null)
      assert.equal(payload.password, createAccount ? 'test-password' : null)
      assert.equal(payload.role, createAccount ? 'EMPLOYEE' : null)
    }
  }
})

test('reconciled modal footer submits its real form and retains official identity fields', () => {
  const creation = render(CreateEmployeeModal, { open: true })
  assert.match(creation, /id="create-employee-form"/)
  assert.match(creation, /type="submit" form="create-employee-form"/)
  const official = render(EditOfficialModal, { open: true, employee: { id: 1, fullName: 'Fixture' } })
  assert.match(official, /id="official-employee-form"/)
  assert.match(official, /type="submit" form="official-employee-form"/)
  for (const field of ['First name', 'Last name', 'Email', 'Hire date']) assert.ok(official.includes(field))
})

test('profile action presentation preserves Manager administration and own-only contact editing', () => {
  const employee = { id: 2, firstName: 'Test', lastName: 'Employee', fullName: 'Test Employee', status: 'ACTIVE' }
  const manager = render(EmployeeProfileModal, { open: true, employee, currentUser: { employeeId: 1, role: 'MANAGER_ADMIN' } })
  for (const action of ['Deactivate', 'Edit contact', 'Edit record']) assert.ok(manager.includes(action))
  const self = render(EmployeeProfileModal, { open: true, employee, currentUser: { employeeId: 2, role: 'EMPLOYEE' } })
  assert.ok(self.includes('Edit contact')); assert.doesNotMatch(self, /Deactivate|Edit record/)
  const supervisor = render(EmployeeProfileModal, { open: true, employee, currentUser: { employeeId: 1, role: 'SUPERVISOR' } })
  assert.doesNotMatch(supervisor, /Edit contact|Deactivate|Edit record/)
})

test('combined directory and profile retain Suspended and On Leave labels', () => {
  for (const [status, label] of [['SUSPENDED', 'Suspended'], ['ON_LEAVE', 'On Leave']]) {
    const employee = { id: 2, fullName: 'Fixture', status }
    for (const html of [render(EmployeeTable, { employees: [employee] }), render(EmployeeProfileModal, { open: true, employee })]) {
      assert.ok(html.includes(label))
      assert.doesNotMatch(html, />Inactive<\/span>/)
    }
  }
})
