import React, { useState, useEffect } from 'react'
import Modal from '../../../components/common/Modal'

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

export default function EditOfficialModal({
  open,
  employee,
  onClose,
  onSubmit,
  departments = [],
  teams = [],
  supervisors = []
}) {
  const [formData, setFormData] = useState({
    jobTitle: '',
    departmentId: '',
    teamId: '',
    supervisorId: '',
    status: 'ACTIVE',
    role: ''
  })

  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState(null)

  useEffect(() => {
    if (employee) {
      setFormData({
        jobTitle: employee.jobTitle || '',
        departmentId: employee.department?.id ? String(employee.department.id) : '',
        teamId: employee.team?.id ? String(employee.team.id) : '',
        supervisorId: employee.supervisor?.id ? String(employee.supervisor.id) : '',
        status: employee.status || 'ACTIVE',
        role: employee.account?.role || ''
      })
    }
  }, [employee])

  const handleChange = (field, value) => {
    setFormData(prev => ({ ...prev, [field]: value }))
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    setError(null)
    setSubmitting(true)

    try {
      const payload = {
        jobTitle: formData.jobTitle.trim(),
        departmentId: Number(formData.departmentId),
        teamId: formData.teamId ? Number(formData.teamId) : null,
        supervisorId: formData.supervisorId ? Number(formData.supervisorId) : null,
        status: formData.status,
        role: formData.role || null
      }

      await onSubmit(employee.id, payload)
      onClose()
    } catch (err) {
      setError(err.message || 'Failed to update official information')
    } finally {
      setSubmitting(false)
    }
  }

  if (!employee) return null

  // Filter out self from supervisor list
  const eligibleSupervisors = supervisors.filter(s => s.id !== employee.id)

  return (
    <Modal open={open} title={`Edit Official Info: ${employee.fullName}`} onClose={onClose}>
      <form onSubmit={handleSubmit} className="space-y-4">
        {error && (
          <div className="p-3 text-xs text-red-700 bg-red-50 border border-red-200 rounded-xl">
            {error}
          </div>
        )}

        <FormField label="Job Title" required>
          <input
            type="text"
            required
            value={formData.jobTitle}
            onChange={e => handleChange('jobTitle', e.target.value)}
            className="w-full px-3 py-2 text-sm border border-gray-200 rounded-xl focus:outline-none focus:border-blue-500"
          />
        </FormField>

        <div className="grid grid-cols-2 gap-3">
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
        </div>

        <div className="grid grid-cols-2 gap-3">
          <FormField label="Supervisor">
            <select
              value={formData.supervisorId}
              onChange={e => handleChange('supervisorId', e.target.value)}
              className="w-full px-3 py-2 text-sm border border-gray-200 rounded-xl focus:outline-none focus:border-blue-500 bg-white"
            >
              <option value="">None (Reports to Head)</option>
              {eligibleSupervisors.map(s => (
                <option key={s.id} value={s.id}>{s.fullName}</option>
              ))}
            </select>
          </FormField>

          <FormField label="Lifecycle Status" required>
            <select
              value={formData.status}
              onChange={e => handleChange('status', e.target.value)}
              className="w-full px-3 py-2 text-sm border border-gray-200 rounded-xl focus:outline-none focus:border-blue-500 bg-white"
            >
              <option value="ACTIVE">Active</option>
              <option value="INACTIVE">Inactive</option>
              <option value="SUSPENDED">Suspended</option>
              <option value="ON_LEAVE">On Leave</option>
            </select>
          </FormField>
        </div>

        {employee.account && (
          <FormField label="Account Role">
            <select
              value={formData.role}
              onChange={e => handleChange('role', e.target.value)}
              className="w-full px-3 py-2 text-sm border border-gray-200 rounded-xl focus:outline-none focus:border-blue-500 bg-white"
            >
              <option value="EMPLOYEE">Employee</option>
              <option value="SUPERVISOR">Supervisor</option>
              <option value="MANAGER_ADMIN">Manager / Admin</option>
            </select>
          </FormField>
        )}

        <div className="flex justify-end gap-2 pt-3 border-t border-gray-100">
          <button
            type="button"
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
            {submitting ? 'Saving...' : 'Save Changes'}
          </button>
        </div>
      </form>
    </Modal>
  )
}
