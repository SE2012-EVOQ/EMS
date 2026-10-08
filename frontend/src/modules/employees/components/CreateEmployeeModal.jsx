import React, { useState } from 'react'
import Modal from '../../../components/common/Modal'
import { createEmployeePayload } from './employeeForm'

function FormField({ label, required, children }) {
  return (
    <div className="space-y-1">
      <label className="block text-xs font-semibold text-gray-700">
        {label} {required && <span className="text-red-500">*</span>}
      </label>
      {children}
    </div>
  )
}

export default function CreateEmployeeModal({
  open,
  onClose,
  onSubmit,
  departments = [],
  teams = [],
  supervisors = []
}) {
  const [formData, setFormData] = useState({
    firstName: '',
    lastName: '',
    email: '',
    phone: '',
    address: '',
    jobTitle: '',
    hireDate: new Date().toISOString().split('T')[0],
    departmentId: '',
    teamId: '',
    supervisorId: '',
    status: 'ACTIVE',
    createAccount: false,
    username: '',
    password: '',
    role: 'EMPLOYEE'
  })

  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState(null)

  const handleChange = (field, value) => {
    setFormData(prev => ({ ...prev, [field]: value }))
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    setError(null)
    setSubmitting(true)

    try {
      const payload = createEmployeePayload(formData)

      await onSubmit(payload)
      onClose()
    } catch (err) {
      setError(err.message || 'Failed to onboard employee')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <Modal open={open} title="Create Employee" onClose={() => !submitting && onClose()}>
      <form onSubmit={handleSubmit} className="space-y-4">
        {error && (
          <div className="p-3 text-xs text-red-700 bg-red-50 border border-red-200 rounded-xl">
            {error}
          </div>
        )}

        <div className="grid grid-cols-2 gap-3">
          <FormField label="First Name" required>
            <input
              type="text"
              required
              value={formData.firstName}
              onChange={e => handleChange('firstName', e.target.value)}
              placeholder="e.g. David"
              className="w-full px-3 py-2 text-sm border border-gray-200 rounded-xl focus:outline-none focus:border-blue-500"
            />
          </FormField>

          <FormField label="Last Name" required>
            <input
              type="text"
              required
              value={formData.lastName}
              onChange={e => handleChange('lastName', e.target.value)}
              placeholder="e.g. Perera"
              className="w-full px-3 py-2 text-sm border border-gray-200 rounded-xl focus:outline-none focus:border-blue-500"
            />
          </FormField>
        </div>

        <div className="grid grid-cols-2 gap-3">
          <FormField label="Work Email" required>
            <input
              type="email"
              required
              value={formData.email}
              onChange={e => handleChange('email', e.target.value)}
              placeholder="david@evoq.com"
              className="w-full px-3 py-2 text-sm border border-gray-200 rounded-xl focus:outline-none focus:border-blue-500"
            />
          </FormField>

          <FormField label="Phone Number">
            <input
              type="text"
              value={formData.phone}
              onChange={e => handleChange('phone', e.target.value)}
              placeholder="077 123 4567"
              className="w-full px-3 py-2 text-sm border border-gray-200 rounded-xl focus:outline-none focus:border-blue-500"
            />
          </FormField>
        </div>

        <div className="grid grid-cols-2 gap-3">
          <FormField label="Job Title" required>
            <input
              type="text"
              required
              value={formData.jobTitle}
              onChange={e => handleChange('jobTitle', e.target.value)}
              placeholder="e.g. Software Engineer"
              className="w-full px-3 py-2 text-sm border border-gray-200 rounded-xl focus:outline-none focus:border-blue-500"
            />
          </FormField>

          <FormField label="Hire Date" required>
            <input
              type="date"
              required
              value={formData.hireDate}
              onChange={e => handleChange('hireDate', e.target.value)}
              className="w-full px-3 py-2 text-sm border border-gray-200 rounded-xl focus:outline-none focus:border-blue-500"
            />
          </FormField>
        </div>

        <div className="grid grid-cols-3 gap-3">
          <FormField label="Department" required>
            <select
              required
              value={formData.departmentId}
              onChange={e => handleChange('departmentId', e.target.value)}
              className="w-full px-3 py-2 text-sm border border-gray-200 rounded-xl focus:outline-none focus:border-blue-500 bg-white"
            >
              <option value="">Select Dept</option>
              {departments.map(d => (
                <option key={d.id} value={d.id}>{d.name}</option>
              ))}
            </select>
          </FormField>

          <FormField label="Team / Project">
            <select
              value={formData.teamId}
              onChange={e => handleChange('teamId', e.target.value)}
              className="w-full px-3 py-2 text-sm border border-gray-200 rounded-xl focus:outline-none focus:border-blue-500 bg-white"
            >
              <option value="">No Project</option>
              {teams.map(t => (
                <option key={t.id} value={t.id}>{t.name}</option>
              ))}
            </select>
          </FormField>

          <FormField label="Supervisor">
            <select
              value={formData.supervisorId}
              onChange={e => handleChange('supervisorId', e.target.value)}
              className="w-full px-3 py-2 text-sm border border-gray-200 rounded-xl focus:outline-none focus:border-blue-500 bg-white"
            >
              <option value="">None (Head)</option>
              {supervisors.map(s => (
                <option key={s.id} value={s.id}>{s.fullName}</option>
              ))}
            </select>
          </FormField>
        </div>

        <FormField label="Home Address">
          <input
            type="text"
            value={formData.address}
            onChange={e => handleChange('address', e.target.value)}
            placeholder="e.g. 123 Galle Road, Colombo"
            className="w-full px-3 py-2 text-sm border border-gray-200 rounded-xl focus:outline-none focus:border-blue-500"
          />
        </FormField>

        <FormField label="Lifecycle Status" required>
          <select aria-label="Lifecycle Status" required value={formData.status} onChange={e => handleChange('status', e.target.value)}
            className="w-full px-3 py-2 text-sm border border-gray-200 rounded-xl focus:outline-none focus:border-blue-500 bg-white">
            <option value="ACTIVE">Active</option>
            <option value="INACTIVE">Inactive</option>
          </select>
          <p className="text-xs text-gray-600">Inactive employees cannot log in. A login account created for them stays disabled until activation.</p>
        </FormField>

        {/* Login Account Section */}
        <div className="pt-2 border-t border-gray-100">
          <label className="flex items-center gap-2 cursor-pointer text-sm font-semibold text-gray-800">
            <input
              type="checkbox"
              checked={formData.createAccount}
              onChange={e => handleChange('createAccount', e.target.checked)}
              className="rounded text-blue-600 focus:ring-0"
            />
            <span>Create System Login Account</span>
          </label>

          {formData.createAccount && (
            <div className="grid grid-cols-3 gap-3 mt-3 p-3 bg-gray-50 rounded-xl">
              <FormField label="Username" required>
                <input
                  type="text"
                  required={formData.createAccount}
                  value={formData.username}
                  onChange={e => handleChange('username', e.target.value)}
                  placeholder="e.g. david.p"
                  className="w-full px-2.5 py-1.5 text-xs border border-gray-200 rounded-lg focus:outline-none focus:border-blue-500 bg-white"
                />
              </FormField>

              <FormField label="Temporary Password" required>
                <input
                  type="password"
                  required={formData.createAccount}
                  minLength={6}
                  value={formData.password}
                  onChange={e => handleChange('password', e.target.value)}
                  placeholder="Min 6 chars"
                  className="w-full px-2.5 py-1.5 text-xs border border-gray-200 rounded-lg focus:outline-none focus:border-blue-500 bg-white"
                />
              </FormField>

              <FormField label="System Role" required>
                <select
                  value={formData.role}
                  onChange={e => handleChange('role', e.target.value)}
                  className="w-full px-2.5 py-1.5 text-xs border border-gray-200 rounded-lg focus:outline-none focus:border-blue-500 bg-white"
                >
                  <option value="EMPLOYEE">Employee</option>
                  <option value="SUPERVISOR">Supervisor</option>
                  <option value="MANAGER_ADMIN">Manager / Admin</option>
                </select>
              </FormField>
            </div>
          )}
        </div>

        <div className="flex justify-end gap-2 pt-3 border-t border-gray-100">
          <button
            type="button"
            disabled={submitting}
            onClick={onClose}
            className="px-4 py-2 text-sm font-semibold text-gray-600 hover:bg-gray-100 rounded-xl transition"
          >
            Cancel
          </button>
          <button
            type="submit"
            disabled={submitting}
            className="px-5 py-2 text-sm font-semibold text-white bg-blue-600 hover:bg-blue-700 disabled:opacity-50 rounded-xl transition"
          >
            {submitting ? 'Creating...' : 'Create Employee'}
          </button>
        </div>
      </form>
    </Modal>
  )
}
