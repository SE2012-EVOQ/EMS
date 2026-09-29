import { Navigate, useLocation } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'

export default function RequireAuth({ children }) {
  const { user, loading, connectionError, refresh } = useAuth()
  const location = useLocation()

  if (loading) return <div className="min-h-screen grid place-items-center text-sm text-gray-500">Checking your session…</div>
  if (connectionError) return <div className="min-h-screen grid place-items-center p-6">
    <div className="text-center"><p className="text-sm mb-4">{connectionError}</p>
      <button onClick={refresh} className="rounded-full bg-black text-white px-5 py-2 text-sm">Retry</button></div>
  </div>
  if (!user) return <Navigate to="/login" replace state={{ from: location.pathname }} />
  return children
}
