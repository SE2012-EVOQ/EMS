import React, { useState, useEffect } from 'react'
import Modal from '../../../components/common/Modal'

export default function CreateEmployeeModal({
  open,
  onClose,
  onSubmit,
  departments = [],
  teams = [],
  supervisors = []
}) {
  const [fullName, setFullName] = useState('')
  const [email, setEmail] = useState('')
  const [phone, setPhone] = useState('')
  const [address, setAddress] = useState('')
  const [jobTitle, setJobTitle] = useState('')
  const [hireDate, setHireDate] = useState(() => new Date().toISOString().split('T')[0])
  const [departmentId, setDepartmentId] = useState('')
  const [teamId, setTeamId] = useState('')
  const [supervisorId, setSupervisorId] = useState('')

  // System user account creation
  const [createAccount, setCreateAccount] = useState(true)
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('EvoqDemo2026!')
  const [role, setRole] = useState('EMPLOYEE')

  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState(null)

  // Initialize department and team defaults when options load
  useEffect(() => {
    if (departments.length > 0 && !departmentId) {
      const eng = departments.find(d => d.name.toLowerCase().includes('engineer'))
      setDepartmentId(eng ? String(eng.id) : String(departments[0].id))
    }
  }, [departments, departmentId])

  useEffect(() => {
    if (teams.length > 0 && !teamId) {
      setTeamId(String(teams[0].id))
    }
  }, [teams, teamId])

  // Automatically suggest username when email changes
  const handleEmailChange = (val) => {
    setEmail(val)
    if (!username || username === email.split('@')[0]) {
      setUsername(val.split('@')[0])
    }
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    setError(null)

    const trimmedName = fullName.trim()
    if (!trimmedName) {
      setError('Full name is required.')
      return
    }

    const nameParts = trimmedName.split(/\s+/)
    let firstName = nameParts[0]
    let lastName = nameParts.slice(1).join(' ')
    if (!lastName) {
      lastName = '-'
    }

    if (!departmentId) {
      setError('Please select a department.')
      return
    }

    setSubmitting(true)

    try {
      const payload = {
        firstName,
        lastName,
        email: email.trim(),
        phone: phone.trim() || null,
        address: address.trim() || null,
        jobTitle: jobTitle.trim(),
        hireDate,
        departmentId: Number(departmentId),
        teamId: teamId ? Number(teamId) : null,
        supervisorId: supervisorId ? Number(supervisorId) : null,
        createAccount,
        username: createAccount ? (username.trim() || email.split('@')[0]) : null,
        password: createAccount ? password : null,
        role: createAccount ? role : null
      }

      await onSubmit(payload)
      onClose()
      // Reset form
      setFullName('')
      setEmail('')
      setPhone('')
      setAddress('')
      setJobTitle('')
      setUsername('')
      setPassword('EvoqDemo2026!')
    } catch (err) {
      setError(err.message || 'Failed to add employee record')
    } finally {
      setSubmitting(false)
    }
  }

  const activeSupervisors = supervisors.filter(s => s.status === 'ACTIVE')

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
      title="Add employee"
      subtitle="Official employee and organization information."
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
          <label className="block">
            <span className="text-[10px] uppercase tracking-wider font-extrabold text-app-muted muted">
              Full name *
            </span>
            <input
              type="text"
              required
              value={fullName}
              onChange={e => setFullName(e.target.value)}
              placeholder="e.g. David Perera"
              className="w-full mt-2 px-4 py-3 rounded-2xl bg-app-subtle subtle border border-app-border focus:border-gray-400 outline-none text-xs font-semibold txt"
            />
          </label>

          <label className="block">
            <span className="text-[10px] uppercase tracking-wider font-extrabold text-app-muted muted">
              Email *
            </span>
            <input
              type="email"
              required
              value={email}
              onChange={e => handleEmailChange(e.target.value)}
              placeholder="e.g. david.perera@evoq.ai"
              className="w-full mt-2 px-4 py-3 rounded-2xl bg-app-subtle subtle border border-app-border focus:border-gray-400 outline-none text-xs font-semibold txt"
            />
          </label>

          <label className="block">
            <span className="text-[10px] uppercase tracking-wider font-extrabold text-app-muted muted">
              Phone
            </span>
            <input
              type="text"
              value={phone}
              onChange={e => setPhone(e.target.value)}
              placeholder="e.g. +94 77 123 4567"
              className="w-full mt-2 px-4 py-3 rounded-2xl bg-app-subtle subtle border border-app-border focus:border-gray-400 outline-none text-xs font-semibold txt"
            />
          </label>

          <label className="block">
            <span className="text-[10px] uppercase tracking-wider font-extrabold text-app-muted muted">
              Address
            </span>
            <input
              type="text"
              value={address}
              onChange={e => setAddress(e.target.value)}
              placeholder="e.g. Colombo 05"
              className="w-full mt-2 px-4 py-3 rounded-2xl bg-app-subtle subtle border border-app-border focus:border-gray-400 outline-none text-xs font-semibold txt"
            />
          </label>

          <label className="block">
            <span className="text-[10px] uppercase tracking-wider font-extrabold text-app-muted muted">
              Job title *
            </span>
            <input
              type="text"
              required
              value={jobTitle}
              onChange={e => setJobTitle(e.target.value)}
              placeholder="e.g. Software Engineer"
              className="w-full mt-2 px-4 py-3 rounded-2xl bg-app-subtle subtle border border-app-border focus:border-gray-400 outline-none text-xs font-semibold txt"
            />
          </label>

          <label className="block">
            <span className="text-[10px] uppercase tracking-wider font-extrabold text-app-muted muted">
              Joined date *
            </span>
            <input
              type="date"
              required
              value={hireDate}
              onChange={e => setHireDate(e.target.value)}
              className="w-full mt-2 px-4 py-3 rounded-2xl bg-app-subtle subtle border border-app-border focus:border-gray-400 outline-none text-xs font-semibold txt"
            />
          </label>

          <label className="block">
            <span className="text-[10px] uppercase tracking-wider font-extrabold text-app-muted muted">
              Department *
            </span>
            <select
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

          <label className="block sm:col-span-2">
            <span className="text-[10px] uppercase tracking-wider font-extrabold text-app-muted muted">
              Supervisor
            </span>
            <select
              value={supervisorId}
              onChange={e => setSupervisorId(e.target.value)}
              className="w-full mt-2 px-4 py-3 rounded-2xl bg-app-subtle subtle border border-app-border focus:border-gray-400 outline-none text-xs font-semibold txt"
            >
              <option value="">— (None / Top Leadership)</option>
              {activeSupervisors.map(s => (
                <option key={s.id} value={s.id}>
                  E{String(s.id).padStart(3, '0')} · {s.fullName} ({s.jobTitle})
                </option>
              ))}
            </select>
          </label>
        </div>

        {/* User Account Provisioning */}
        <div className="pt-4 border-t border-app-border mt-4">
          <label className="flex items-center gap-2.5 cursor-pointer">
            <input
              type="checkbox"
              checked={createAccount}
              onChange={e => setCreateAccount(e.target.checked)}
              className="w-4 h-4 rounded text-[#1A1D1F] focus:ring-0 cursor-pointer"
            />
            <span className="text-xs font-extrabold txt">
              Create system login user account
            </span>
          </label>

          {createAccount && (
            <div className="grid sm:grid-cols-3 gap-3 mt-3 p-4 bg-app-subtle subtle rounded-2xl border border-app-border/40">
              <label className="block">
                <span className="text-[10px] uppercase tracking-wider font-extrabold text-app-muted muted">
                  Username
                </span>
                <input
                  type="text"
                  required={createAccount}
                  value={username}
                  onChange={e => setUsername(e.target.value)}
                  placeholder="e.g. david.p"
                  className="w-full mt-1.5 px-3 py-2 rounded-xl bg-white border border-app-border text-xs font-semibold outline-none"
                />
              </label>

              <label className="block">
                <span className="text-[10px] uppercase tracking-wider font-extrabold text-app-muted muted">
                  Temporary password
                </span>
                <input
                  type="password"
                  required={createAccount}
                  value={password}
                  onChange={e => setPassword(e.target.value)}
                  placeholder="Min 6 chars"
                  className="w-full mt-1.5 px-3 py-2 rounded-xl bg-white border border-app-border text-xs font-semibold outline-none"
                />
              </label>

              <label className="block">
                <span className="text-[10px] uppercase tracking-wider font-extrabold text-app-muted muted">
                  System role
                </span>
                <select
                  value={role}
                  onChange={e => setRole(e.target.value)}
                  className="w-full mt-1.5 px-3 py-2 rounded-xl bg-white border border-app-border text-xs font-semibold outline-none"
                >
                  <option value="EMPLOYEE">Employee</option>
                  <option value="SUPERVISOR">Supervisor / Team Lead</option>
                  <option value="MANAGER_ADMIN">Manager / Admin</option>
                </select>
              </label>
            </div>
          )}
        </div>
      </form>
    </Modal>
  )
}
