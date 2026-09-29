import { Menu } from 'lucide-react'
import { useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../../context/AuthContext'

const meta = {
  '/dashboard': ['Dashboard', 'Organization and work overview'],
  '/employees': ['Employees', 'Employee & organization management'],
  '/leave': ['Leave', 'Requests, balances and decision history'],
  '/attendance': ['Attendance', 'Attendance records and hours'],
  '/schedule': ['Schedule', 'Team working schedules'],
  '/assets': ['Assets', 'Company equipment and assignments'],
  '/reports': ['Reports', 'Employee, leave, attendance and asset summaries']
}

const roleLabels = {
  MANAGER_ADMIN: 'Manager / Admin',
  SUPERVISOR: 'Supervisor',
  EMPLOYEE: 'Employee'
}

export default function Header({ onMenu }) {
  const { pathname } = useLocation()
  const navigate = useNavigate()
  const { user, logout } = useAuth()
  const [profileOpen, setProfileOpen] = useState(false)
  const [logoutError, setLogoutError] = useState('')
  const [title, subtitle] = meta[pathname] || ['EVOQ EMS', 'Employee management system']
  const roleLabel = roleLabels[user?.role] || 'Account'

  const signOut = async () => {
    setLogoutError('')
    try {
      await logout()
      navigate('/login', { replace: true })
    } catch (error) {
      setLogoutError(error.message)
    }
  }

  return <header className="shrink-0 px-4 sm:px-6 lg:px-8 pt-4 lg:pt-6 pb-3 flex items-center justify-between gap-3">
    <div className="flex items-center gap-3 min-w-0">
      <button className="lg:hidden w-10 h-10 bg-white surface rounded-full shadow-sm flex items-center justify-center" onClick={onMenu} aria-label="Open menu"><Menu className="w-4 h-4" /></button>
      <div className="min-w-0">
        <div className="flex items-center gap-2">
          <h1 className="text-2xl sm:text-3xl font-extrabold tracking-tight txt clip-text">{title}</h1>
          <span className="hidden sm:inline-flex text-[10px] px-2 py-1 rounded-full bg-app-blue-bg text-app-blue font-bold">{roleLabel}</span>
        </div>
        <div className="hidden sm:block text-xs text-app-muted muted font-medium mt-0.5">{subtitle}</div>
      </div>
    </div>
    <div className="relative shrink-0">
      <button onClick={() => setProfileOpen(open => !open)} className="w-10 h-10 rounded-full bg-[#DEE8FF] avatar-soft shadow-sm flex items-center justify-center text-[11px] font-extrabold" aria-label="Account menu">{user?.username?.slice(0, 2).toUpperCase()}</button>
      {profileOpen && <div className="floating-panel absolute right-0 top-12 z-50 w-[290px] surface bg-white rounded-[24px] border border-app-border shadow-2xl p-3">
        <div className="p-3">
          <div className="text-sm font-extrabold txt">{user?.username}</div>
          <div className="text-[10px] text-app-muted muted mt-1">{roleLabel}</div>
        </div>
        <div className="border-t border-app-border pt-2">
          <button onClick={() => { setProfileOpen(false); navigate('/account/password') }} className="w-full text-left px-3 py-2.5 rounded-xl text-xs font-bold hover:bg-app-subtle">Change password</button>
          <button onClick={signOut} className="w-full text-left px-3 py-2.5 rounded-xl text-xs font-bold hover:bg-app-subtle">Sign out</button>
          {logoutError && <p role="alert" className="px-3 py-2 text-xs text-app-pink">{logoutError}</p>}
        </div>
      </div>}
    </div>
  </header>
}
