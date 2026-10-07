import { createContext, useContext, useEffect, useState } from 'react'
import { authService } from '../services/authService'
import { onSessionExpired } from '../services/api'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null)
  const [loading, setLoading] = useState(true)
  const [connectionError, setConnectionError] = useState('')

  const refresh = async () => {
    setLoading(true)
    setConnectionError('')
    try {
      setUser(await authService.me())
    } catch (error) {
      setUser(null)
      if (error.status !== 401) setConnectionError('Could not connect to the backend. Try again.')
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
    setConnectionError('')
    return authenticatedUser
  }

  const logout = async () => {
    await authService.logout()
    setUser(null)
  }

  return <AuthContext.Provider value={{ user, loading, connectionError, refresh, login, logout }}>
    {children}
  </AuthContext.Provider>
}

export function useAuth() {
  const context = useContext(AuthContext)
  if (!context) throw new Error('useAuth must be used inside AuthProvider')
  return context
}
