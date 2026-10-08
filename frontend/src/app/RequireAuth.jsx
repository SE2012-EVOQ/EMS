import { Navigate, useLocation } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'

export default function RequireAuth({ children }) {
  const { user, loading, setupRequired, connectionError, refresh } = useAuth()
  const location = useLocation()

  if (loading) return <div className="min-h-dvh grid place-items-center text-sm text-app-muted">Checking your session…</div>
  if (connectionError) return <div className="min-h-dvh grid place-items-center p-6">
    <div className="text-center"><p className="text-sm mb-4">{connectionError}</p>
      <button onClick={refresh} className="rounded-full bg-black dark-primary text-white px-5 py-2 text-sm">Retry</button></div>
  </div>
  if (setupRequired) return <Navigate to="/setup" replace />
  if (!user) return <Navigate to="/login" replace state={{ from: location.pathname }} />
  return children
}
