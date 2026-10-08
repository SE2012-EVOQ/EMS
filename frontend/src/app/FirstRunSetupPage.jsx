import { useState } from 'react'
import { Navigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { setupService } from '../services/setupService'
import RequestFeedback from '../components/common/RequestFeedback'
import { requestErrorMessage } from '../components/common/requestError'

const empty = { firstName: '', lastName: '', email: '', hireDate: '', departmentName: '', jobTitle: '', username: '', password: '' }

export default function FirstRunSetupPage() {
  const { user, loading, setupRequired, connectionError, refresh, finishSetup } = useAuth()
  const [form, setForm] = useState(empty)
  const [confirmation, setConfirmation] = useState('')
  const [error, setError] = useState('')
  const [fields, setFields] = useState({})
  const [submitting, setSubmitting] = useState(false)

  if (loading) return <div role="status" className="min-h-dvh grid place-items-center text-sm text-app-muted">Checking setup…</div>
  if (connectionError) return <div className="min-h-dvh grid place-items-center p-6"><RequestFeedback error={connectionError} onRetry={refresh} /></div>
  if (user) return <Navigate to="/dashboard" replace />
  if (!setupRequired) return <Navigate to="/login" replace state={{ message: 'Administrator setup is complete. Sign in to continue.' }} />

  const change = event => setForm(current => ({ ...current, [event.target.name]: event.target.value }))
  const input = (name, label, options = {}) => <label className="block text-xs font-bold txt">{label}
    <input name={name} value={form[name]} onChange={change} required disabled={submitting}
      aria-invalid={!!fields[name]} aria-describedby={fields[name] ? `setup-${name}-error` : undefined}
      className="h-11 mt-2 w-full rounded-2xl border border-app-border bg-app-subtle px-4 py-2.5 text-sm" {...options} />
    {fields[name] && <span id={`setup-${name}-error`} className="mt-1 block text-app-pink font-normal">{fields[name]}</span>}
  </label>

  const submit = async event => {
    event.preventDefault()
    setError(''); setFields({})
    if (form.password !== confirmation) { setError('Passwords do not match.'); return }
    if (new TextEncoder().encode(form.password).length > 72) { setFields({ password: 'Choose a shorter password.' }); return }
    setSubmitting(true)
    try {
      const details = Object.fromEntries(Object.entries(form).map(([key, value]) => [key, key === 'password' ? value : value.trim()]))
      await setupService.create(details)
      setForm(empty); setConfirmation('')
      finishSetup()
    } catch (failure) {
      setError(requestErrorMessage(failure, 'Administrator setup'))
      setFields(failure.fieldErrors || {})
      // Another operator may have completed setup while this form was open.
      if (failure.status === 409) await refresh()
    } finally {
      setSubmitting(false)
    }
  }

  return <main className="min-h-dvh bg-app-bg flex items-center justify-center p-5">
    <div className="surface bg-white border border-app-border rounded-[28px] w-full max-w-2xl p-7 sm:p-9">
      <div className="w-12 h-12 rounded-full bg-black dark-primary text-white grid place-items-center font-black text-sm mb-7">E</div>
      <p className="text-[10px] uppercase tracking-[.22em] font-bold text-app-muted">EVOQ EMPLOYEE SYSTEM</p>
      <h1 className="text-3xl font-extrabold txt mt-2">Create administrator</h1>
      <p className="text-sm text-app-muted mt-2">Set up your profile and administrator account.</p>
      <form onSubmit={submit} className="mt-7 space-y-5">
        <div className="grid sm:grid-cols-2 gap-4">
          {input('firstName', 'First name', { autoComplete: 'given-name', maxLength: 100 })}
          {input('lastName', 'Last name', { autoComplete: 'family-name', maxLength: 100 })}
          {input('email', 'Email', { type: 'email', autoComplete: 'email', maxLength: 150 })}
          {input('hireDate', 'Hire date', { type: 'date' })}
          {input('departmentName', 'Department', { maxLength: 100 })}
          {input('jobTitle', 'Job title', { autoComplete: 'organization-title', maxLength: 100 })}
        </div>
        {input('username', 'Username', { autoComplete: 'username', minLength: 3, maxLength: 100, pattern: '[A-Za-z0-9][A-Za-z0-9._\\-]{2,99}' })}
        <p className="text-xs text-app-muted">Letters, numbers, dots, underscores or hyphens. Start with a letter or number.</p>
        <div className="grid sm:grid-cols-2 gap-4">
          {input('password', 'Password', { type: 'password', autoComplete: 'new-password', minLength: 6, maxLength: 72 })}
          <label className="block text-xs font-bold txt">Confirm password
            <input type="password" autoComplete="new-password" required minLength={6} maxLength={72} disabled={submitting}
              value={confirmation} onChange={event => setConfirmation(event.target.value)}
              className="h-11 mt-2 w-full rounded-2xl border border-app-border bg-app-subtle px-4 py-2.5 text-sm" />
          </label>
        </div>
        <p className="text-xs text-app-muted">At least 6 characters.</p>
        <RequestFeedback error={error} />
        <button disabled={submitting} className="w-full rounded-full bg-[#1A1D1F] dark-primary text-white py-3 text-sm font-bold disabled:opacity-50">
          {submitting ? 'Creating administrator…' : 'Create administrator'}
        </button>
      </form>
    </div>
  </main>
}
