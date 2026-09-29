import { useState } from 'react'
import { Navigate, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'

export default function LoginPage() {
  const { user, loading, login, connectionError } = useAuth()
  const location = useLocation()
  const navigate = useNavigate()
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [submitting, setSubmitting] = useState(false)

  if (loading) return <div className="min-h-screen grid place-items-center text-sm text-gray-500">Checking your session…</div>
  if (user) return <Navigate to="/dashboard" replace />

  const submit = async event => {
    event.preventDefault()
    setError('')
    setSubmitting(true)
    try {
      await login({ username: username.trim(), password })
      navigate(location.state?.from || '/dashboard', { replace: true })
    } catch (failure) {
      setError(failure.message)
    } finally {
      setSubmitting(false)
    }
  }

  return <div className="min-h-screen bg-app-bg flex items-center justify-center p-5">
    <div className="login-card surface bg-white border border-app-border rounded-[28px] w-full max-w-md p-7 sm:p-9">
      <div className="brandmark w-12 h-12 rounded-full bg-black text-white grid place-items-center font-black text-sm mb-7">E</div>
      <p className="text-[10px] uppercase tracking-[.22em] font-bold text-app-muted">EVOQ EMPLOYEE SYSTEM</p>
      <h1 className="text-3xl font-extrabold txt mt-2">Sign in</h1>
      <p className="text-sm text-app-muted mt-2">Use your assigned employee account.</p>
      <form onSubmit={submit} className="mt-8 space-y-4">
        <label className="block text-xs font-bold txt">Username
          <input autoComplete="username" required value={username} onChange={event => setUsername(event.target.value)}
            className="mt-2 w-full rounded-2xl border border-app-border bg-app-subtle px-4 py-3 text-sm" />
        </label>
        <label className="block text-xs font-bold txt">Password
          <input type="password" autoComplete="current-password" required value={password}
            onChange={event => setPassword(event.target.value)}
            className="mt-2 w-full rounded-2xl border border-app-border bg-app-subtle px-4 py-3 text-sm" />
        </label>
        {(error || connectionError) && <p role="alert" className="rounded-2xl bg-app-pink-bg text-app-pink p-3 text-xs font-semibold">{error || connectionError}</p>}
        <button disabled={submitting} className="w-full rounded-full bg-[#1A1D1F] text-white py-3 text-sm font-bold">
          {submitting ? 'Signing in…' : 'Sign in'}
        </button>
      </form>
    </div>
  </div>
}
