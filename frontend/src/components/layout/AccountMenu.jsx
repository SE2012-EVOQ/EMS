import { ChevronUp } from 'lucide-react'
import { useEffect, useRef, useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../../context/AuthContext'

const roleLabels = { MANAGER_ADMIN: 'Manager / Admin', SUPERVISOR: 'Supervisor', EMPLOYEE: 'Employee' }

export default function AccountMenu({ onNavigate }) {
  const { pathname } = useLocation()
  const navigate = useNavigate()
  const { user, logout } = useAuth()
  const [profileOpen, setProfileOpen] = useState(false)
  const [logoutError, setLogoutError] = useState('')
  const [signingOut, setSigningOut] = useState(false)
  const profile = useRef(null)
  const trigger = useRef(null)

  useEffect(() => { setProfileOpen(false) }, [pathname])
  useEffect(() => {
    if (!profileOpen) return
    const outside = event => { if (!profile.current?.contains(event.target)) setProfileOpen(false) }
    const escape = event => { if (event.key === 'Escape') { setProfileOpen(false); trigger.current?.focus() } }
    document.addEventListener('pointerdown', outside)
    document.addEventListener('keydown', escape)
    return () => { document.removeEventListener('pointerdown', outside); document.removeEventListener('keydown', escape) }
  }, [profileOpen])

  const signOut = async () => {
    setLogoutError(''); setSigningOut(true)
    try { await logout(); onNavigate?.(); navigate('/login', { replace: true }) }
    catch (error) { setLogoutError(error.message) }
    finally { setSigningOut(false) }
  }

  return <div ref={profile} className="relative">
    <button type="button" ref={trigger} onClick={() => setProfileOpen(open => !open)} className="flex w-full items-center gap-2.5 rounded-2xl p-2 text-left hover:bg-white/70 hover-surface" aria-label="Account menu" aria-expanded={profileOpen} aria-controls={profileOpen ? 'account-menu' : undefined}>
      <span className="w-9 h-9 shrink-0 rounded-full bg-[#DEE8FF] avatar-soft text-[#3B5BDB] flex items-center justify-center text-[11px] font-extrabold">{user?.username?.slice(0, 2).toUpperCase() || '?'}</span>
      <span className="min-w-0 flex-1"><span className="block truncate text-xs font-bold txt">{user?.username}</span><span className="block text-[10px] text-app-muted mt-0.5">{roleLabels[user?.role] || 'Account'}</span></span>
      <ChevronUp className={`w-3.5 h-3.5 shrink-0 text-app-muted transition-transform ${profileOpen ? 'rotate-180' : ''}`} />
    </button>
    {profileOpen && <div id="account-menu" className="floating-panel absolute left-0 bottom-full mb-2 z-50 w-full surface bg-white rounded-2xl border border-app-border shadow-2xl p-2">
      <button type="button" onClick={() => { setProfileOpen(false); onNavigate?.(); navigate('/account/password') }} className="w-full text-left px-3 py-2.5 rounded-xl text-xs font-bold hover:bg-app-subtle">Change password</button>
      <button type="button" onClick={signOut} disabled={signingOut} className="w-full text-left px-3 py-2.5 rounded-xl text-xs font-bold hover:bg-app-subtle">{signingOut ? 'Signing out…' : 'Sign out'}</button>
      {logoutError && <p role="alert" className="px-3 py-2 text-xs text-app-pink">{logoutError}</p>}
    </div>}
  </div>
}
