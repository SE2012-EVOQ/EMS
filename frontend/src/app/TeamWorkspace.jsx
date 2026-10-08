import { Navigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { hasTeamWorkspace } from './roleAccess'

export default function TeamWorkspace({ children, fallback = '/dashboard' }) {
  const { user } = useAuth()
  return hasTeamWorkspace(user?.role) ? children : <Navigate to={fallback} replace />
}
