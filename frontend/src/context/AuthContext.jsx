import { createContext, useContext, useEffect, useState } from 'react'
import { authService } from '../services/authService'
import { onSessionExpired } from '../services/api'
import { loadEntryState } from '../services/setupService'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null)
  const [loading, setLoading] = useState(true)
  const [connectionError, setConnectionError] = useState('')
  const [setupRequired, setSetupRequired] = useState(null)

  const refresh = async () => {
    setLoading(true)
    setConnectionError('')
    try {
      const entry = await loadEntryState(authService.me)
      setSetupRequired(entry.setupRequired)
      setUser(entry.user)
    } catch (error) {
      setUser(null)
      setSetupRequired(null)
      setConnectionError('Could not check the system. Try again.')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    const unsubscribe = onSessionExpired(() => { setUser(null); setConnectionError(''); setLoading(false) })
    refresh()
    return unsubscribe
  }, [])

  const login = async credentials => {
    const authenticatedUser = await authService.login(credentials)
    setUser(authenticatedUser)
    setSetupRequired(false)
    setConnectionError('')
    return authenticatedUser
  }

  const logout = async () => {
    await authService.logout()
    setUser(null)
  }

  const finishSetup = () => { setSetupRequired(false); setUser(null); setConnectionError('') }

  return <AuthContext.Provider value={{ user, loading, setupRequired, connectionError, refresh, finishSetup, login, logout }}>
    {children}
  </AuthContext.Provider>
}

export function useAuth() {
  const context = useContext(AuthContext)
  if (!context) throw new Error('useAuth must be used inside AuthProvider')
  return context
}
