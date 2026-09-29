import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { authService } from '../services/authService'

export default function ChangePasswordPage() {
  const navigate = useNavigate()
  const [currentPassword, setCurrentPassword] = useState('')
  const [newPassword, setNewPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [error, setError] = useState('')
  const [saved, setSaved] = useState(false)
  const [submitting, setSubmitting] = useState(false)

  const submit = async event => {
    event.preventDefault()
    setError('')
    setSaved(false)
    if (newPassword !== confirmPassword) {
      setError('New passwords do not match')
      return
    }
    setSubmitting(true)
    try {
      await authService.changePassword({ currentPassword, newPassword })
      setCurrentPassword('')
      setNewPassword('')
      setConfirmPassword('')
      setSaved(true)
    } catch (failure) {
      setError(failure.message)
    } finally {
      setSubmitting(false)
    }
  }

  return <div className="max-w-xl mx-auto mt-6 sm:mt-10">
    <button onClick={() => navigate('/dashboard')} className="text-xs font-bold text-app-muted mb-5">← Back to dashboard</button>
    <div className="surface bg-white border border-app-border rounded-[28px] p-6 sm:p-8">
      <h2 className="text-2xl font-extrabold txt">Change password</h2>
      <p className="text-sm text-app-muted mt-2">Enter your current password, then choose a new one with at least 8 characters.</p>
      <form onSubmit={submit} className="mt-7 space-y-4">
        {[
          ['Current password', currentPassword, setCurrentPassword, 'current-password'],
          ['New password', newPassword, setNewPassword, 'new-password'],
          ['Confirm new password', confirmPassword, setConfirmPassword, 'new-password']
        ].map(([label, value, setter, autocomplete]) => <label key={label} className="block text-xs font-bold txt">{label}
          <input type="password" autoComplete={autocomplete} required value={value}
            onChange={event => setter(event.target.value)}
            className="mt-2 w-full rounded-2xl border border-app-border bg-app-subtle px-4 py-3 text-sm" />
        </label>)}
        {error && <p role="alert" className="rounded-2xl bg-app-pink-bg text-app-pink p-3 text-xs font-semibold">{error}</p>}
        {saved && <p role="status" className="rounded-2xl bg-app-green-bg text-app-green p-3 text-xs font-semibold">Password updated.</p>}
        <button disabled={submitting} className="rounded-full bg-[#1A1D1F] text-white px-6 py-3 text-xs font-bold">
          {submitting ? 'Saving…' : 'Update password'}
        </button>
      </form>
    </div>
  </div>
}
