import React, { useState, useEffect } from 'react'
import Modal from '../../../components/common/Modal'

function FormField({ label, required, children }) {
  return (
    <div className="space-y-1">
      <label className="block text-[10px] uppercase tracking-wider font-extrabold text-app-muted muted">
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
    firstName: '', lastName: '', email: '', hireDate: '',
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
        firstName: employee.firstName || '', lastName: employee.lastName || '', email: employee.email || '', hireDate: employee.hireDate || '',
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
        firstName: formData.firstName.trim(), lastName: formData.lastName.trim(), email: formData.email.trim(), hireDate: formData.hireDate,
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

  const footer = (
    <>
          <button
            type="button"
            disabled={submitting}
            onClick={onClose}
            className="px-4 py-2.5 rounded-full text-xs font-bold text-app-muted hover:bg-app-subtle transition"
          >
            Cancel
          </button>
          <button
            type="submit"
            form="official-employee-form"
            disabled={submitting}
            className="px-5 py-2.5 rounded-full text-xs font-bold text-white bg-[#1A1D1F] hover:bg-black disabled:opacity-50 transition"
          >
            {submitting ? 'Saving...' : 'Save Changes'}
          </button>
    </>
  )

  return (
    <Modal open={open} title="Edit employee record" subtitle={`Official organization record for ${employee.fullName}`} footer={footer} size="max-w-2xl" onClose={() => !submitting && onClose()}>
      <form id="official-employee-form" onSubmit={handleSubmit} className="space-y-4">
        {error && (
          <div className="p-3 text-xs text-app-pink bg-app-pink-bg rounded-2xl font-semibold">
            {error}
          </div>
        )}

        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
          {[['firstName', 'First name', 'text', 100], ['lastName', 'Last name', 'text', 100], ['email', 'Email', 'email', 150], ['hireDate', 'Hire date', 'date']].map(([field, label, type, max]) => <FormField key={field} label={label} required><input required type={type} maxLength={max} value={formData[field]} onChange={e => handleChange(field, e.target.value)} className="w-full mt-2 px-4 py-3 rounded-2xl bg-app-subtle subtle border border-app-border text-xs font-semibold txt" /></FormField>)}
        </div>
        <FormField label="Job Title" required>
          <input
            type="text"
            required
            value={formData.jobTitle}
            onChange={e => handleChange('jobTitle', e.target.value)}
            className="w-full mt-2 px-4 py-3 rounded-2xl bg-app-subtle subtle border border-app-border focus:border-gray-400 outline-none text-xs font-semibold txt"
          />
        </FormField>

        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <FormField label="Department" required>
            <select
              required
              value={formData.departmentId}
              onChange={e => handleChange('departmentId', e.target.value)}
              className="w-full mt-2 px-4 py-3 rounded-2xl bg-app-subtle subtle border border-app-border focus:border-gray-400 outline-none text-xs font-semibold txt"
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
              className="w-full mt-2 px-4 py-3 rounded-2xl bg-app-subtle subtle border border-app-border focus:border-gray-400 outline-none text-xs font-semibold txt"
            >
              <option value="">No Project</option>
              {teams.map(t => (
                <option key={t.id} value={t.id}>{t.name}</option>
              ))}
            </select>
          </FormField>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <FormField label="Supervisor">
            <select
              value={formData.supervisorId}
              onChange={e => handleChange('supervisorId', e.target.value)}
              className="w-full mt-2 px-4 py-3 rounded-2xl bg-app-subtle subtle border border-app-border focus:border-gray-400 outline-none text-xs font-semibold txt"
            >
              <option value="">None (Reports to Head)</option>
              {eligibleSupervisors.map(s => (
                <option key={s.id} value={s.id}>{s.fullName}</option>
              ))}
            </select>
          </FormField>

          <FormField label="Lifecycle Status" required>
            <select
              aria-label="Lifecycle Status"
              value={formData.status}
              onChange={e => handleChange('status', e.target.value)}
              className="w-full mt-2 px-4 py-3 rounded-2xl bg-app-subtle subtle border border-app-border focus:border-gray-400 outline-none text-xs font-semibold txt"
            >
              <option value="ACTIVE">Active</option>
              <option value="INACTIVE">Inactive</option>
              <option value="SUSPENDED">Suspended</option>
              <option value="ON_LEAVE">On Leave</option>
            </select>
            <p className="text-xs text-app-muted">Only Active employees can log in. Changing status also updates their linked account.</p>
          </FormField>
        </div>

        {employee.account && (
          <FormField label="Account Role">
            <select
              value={formData.role}
              onChange={e => handleChange('role', e.target.value)}
              className="w-full mt-2 px-4 py-3 rounded-2xl bg-app-subtle subtle border border-app-border focus:border-gray-400 outline-none text-xs font-semibold txt"
            >
              <option value="EMPLOYEE">Employee</option>
              <option value="SUPERVISOR">Supervisor</option>
              <option value="MANAGER_ADMIN">Manager / Admin</option>
            </select>
          </FormField>
        )}


      </form>
    </Modal>
  )
}
