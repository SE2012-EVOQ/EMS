import React, { useState, useEffect } from 'react'
import Modal from '../../../components/common/Modal'

export default function EditOfficialModal({
  open,
  employee,
  onClose,
  onSubmit,
  departments = [],
  teams = [],
  supervisors = []
}) {
  const [jobTitle, setJobTitle] = useState('')
  const [departmentId, setDepartmentId] = useState('')
  const [teamId, setTeamId] = useState('')
  const [supervisorId, setSupervisorId] = useState('')
  const [status, setStatus] = useState('ACTIVE')
  const [role, setRole] = useState('')

  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState(null)

  useEffect(() => {
    if (employee) {
      setJobTitle(employee.jobTitle || '')
      setDepartmentId(employee.department?.id ? String(employee.department.id) : '')
      setTeamId(employee.team?.id ? String(employee.team.id) : '')
      setSupervisorId(employee.supervisor?.id ? String(employee.supervisor.id) : '')
      setStatus(employee.status || 'ACTIVE')
      setRole(employee.account?.role || '')
    }
  }, [employee])

  if (!employee) return null

  const handleSubmit = async (e) => {
    e.preventDefault()
    setError(null)
    setSubmitting(true)

    try {
      const payload = {
        jobTitle: jobTitle.trim(),
        departmentId: Number(departmentId),
        teamId: teamId ? Number(teamId) : null,
        supervisorId: supervisorId ? Number(supervisorId) : null,
        status,
        role: role || null
      }

      await onSubmit(employee.id, payload)
      onClose()
    } catch (err) {
      setError(err.message || 'Failed to update official information')
    } finally {
      setSubmitting(false)
    }
  }

  const eligibleSupervisors = supervisors.filter(s => s.id !== employee.id && s.status === 'ACTIVE')

  const footer = (
    <>
      <button
        type="button"
        onClick={onClose}
        disabled={submitting}
        className="px-4 py-2.5 rounded-full border border-gray-200 text-xs font-bold hover:bg-gray-50 transition"
      >
        Cancel
      </button>
      <button
        type="button"
        onClick={handleSubmit}
        disabled={submitting}
        className="px-5 py-2.5 rounded-full bg-[#1A1D1F] hover:bg-black text-white text-xs font-bold transition disabled:opacity-50"
      >
        {submitting ? 'Saving...' : 'Save'}
      </button>
    </>
  )

  return (
    <Modal
      open={open}
      title="Edit employee record"
      subtitle={`Official organization record for ${employee.fullName}`}
      onClose={onClose}
      footer={footer}
      size="max-w-2xl"
    >
      <form onSubmit={handleSubmit} className="space-y-4">
        {error && (
          <div className="p-3 text-xs text-app-pink bg-app-pink-bg rounded-2xl font-semibold">
            {error}
          </div>
        )}

        <div className="grid sm:grid-cols-2 gap-4">
          <label className="block sm:col-span-2">
            <span className="text-[10px] uppercase tracking-wider font-extrabold text-app-muted muted">
              Job title *
            </span>
            <input
              type="text"
              required
              value={jobTitle}
              onChange={e => setJobTitle(e.target.value)}
              className="w-full mt-2 px-4 py-3 rounded-2xl bg-app-subtle subtle border border-app-border focus:border-gray-400 outline-none text-xs font-semibold txt"
            />
          </label>

          <label className="block">
            <span className="text-[10px] uppercase tracking-wider font-extrabold text-app-muted muted">
              Department *
            </span>
            <select
              required
              value={departmentId}
              onChange={e => setDepartmentId(e.target.value)}
              className="w-full mt-2 px-4 py-3 rounded-2xl bg-app-subtle subtle border border-app-border focus:border-gray-400 outline-none text-xs font-semibold txt"
            >
              <option value="">Select Department</option>
              {departments.map(d => (
                <option key={d.id} value={d.id}>{d.name}</option>
              ))}
            </select>
          </label>

          <label className="block">
            <span className="text-[10px] uppercase tracking-wider font-extrabold text-app-muted muted">
              Team / Project
            </span>
            <select
              value={teamId}
              onChange={e => setTeamId(e.target.value)}
              className="w-full mt-2 px-4 py-3 rounded-2xl bg-app-subtle subtle border border-app-border focus:border-gray-400 outline-none text-xs font-semibold txt"
            >
              <option value="">No Project</option>
              {teams.map(t => (
                <option key={t.id} value={t.id}>{t.name}</option>
              ))}
            </select>
          </label>

          <label className="block">
            <span className="text-[10px] uppercase tracking-wider font-extrabold text-app-muted muted">
              Reporting supervisor
            </span>
            <select
              value={supervisorId}
              onChange={e => setSupervisorId(e.target.value)}
              className="w-full mt-2 px-4 py-3 rounded-2xl bg-app-subtle subtle border border-app-border focus:border-gray-400 outline-none text-xs font-semibold txt"
            >
              <option value="">— (Direct Report to Head)</option>
              {eligibleSupervisors.map(s => (
                <option key={s.id} value={s.id}>
                  E{String(s.id).padStart(3, '0')} · {s.fullName}
                </option>
              ))}
            </select>
          </label>

          <label className="block">
            <span className="text-[10px] uppercase tracking-wider font-extrabold text-app-muted muted">
              Status *
            </span>
            <select
              value={status}
              onChange={e => setStatus(e.target.value)}
              className="w-full mt-2 px-4 py-3 rounded-2xl bg-app-subtle subtle border border-app-border focus:border-gray-400 outline-none text-xs font-semibold txt"
            >
              <option value="ACTIVE">Active</option>
              <option value="INACTIVE">Inactive</option>
              <option value="SUSPENDED">Suspended</option>
              <option value="ON_LEAVE">On Leave</option>
            </select>
          </label>

          {employee.account && (
            <label className="block sm:col-span-2">
              <span className="text-[10px] uppercase tracking-wider font-extrabold text-app-muted muted">
                System Account Role
              </span>
              <select
                value={role}
                onChange={e => setRole(e.target.value)}
                className="w-full mt-2 px-4 py-3 rounded-2xl bg-app-subtle subtle border border-app-border focus:border-gray-400 outline-none text-xs font-semibold txt"
              >
                <option value="EMPLOYEE">Employee</option>
                <option value="SUPERVISOR">Supervisor / Team Lead</option>
                <option value="MANAGER_ADMIN">Manager / Admin</option>
              </select>
            </label>
          )}
        </div>
      </form>
    </Modal>
  )
}
