import { Bell, Menu, Plus, Search } from 'lucide-react'
import { useMemo, useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { useEms } from '../../context/EmsContext'

const meta = {
  '/dashboard':['Dashboard','Organization and work overview'], '/employees':['Employees','Employee & organization management'],
  '/leave':['Leave','Requests, balances and decision history'], '/attendance':['Attendance','Attendance records, hours and corrections'],
  '/schedule':['Schedule','Shared team working schedules'], '/assets':['Assets','Company equipment and assignment tracking'],
  '/reports':['Reports','Employee, leave, attendance and asset summaries']
}

export default function Header({ onMenu }) {
  const { pathname } = useLocation()
  const navigate = useNavigate()
  const { db, role, setRole, profile, currentEmployee } = useEms()
  const [search, setSearch] = useState('')
  const [profileOpen, setProfileOpen] = useState(false)
  const [notificationsOpen, setNotificationsOpen] = useState(false)
  const [searchOpen, setSearchOpen] = useState(false)
  const [title, subtitle] = meta[pathname] || ['EVOQ EMS','Employee management system']

  const results = useMemo(() => {
    const q = search.trim().toLowerCase()
    if (!q) return []
    return db.employees.filter(e => `${e.name} ${e.email} ${e.title} ${e.team}`.toLowerCase().includes(q)).slice(0,5)
  }, [db.employees, search])

  const quickCreate = () => {
    if (role === 'supervisor') navigate('/schedule?create=1')
    else if (role === 'employee') navigate('/leave?create=1')
    else navigate('/employees?create=1')
  }

  return <header className="shrink-0 px-4 sm:px-6 lg:px-8 pt-4 lg:pt-6 pb-3 flex items-center justify-between gap-3">
    <div className="flex items-center gap-3 min-w-0"><button className="lg:hidden w-10 h-10 bg-white surface rounded-full shadow-sm flex items-center justify-center" onClick={onMenu}><Menu className="w-4 h-4" /></button><div className="min-w-0"><div className="flex items-center gap-2"><h1 className="text-2xl sm:text-3xl font-extrabold tracking-tight txt clip-text">{title}</h1><span className="hidden sm:inline-flex text-[10px] px-2 py-1 rounded-full bg-app-blue-bg text-app-blue font-bold">{profile.label}</span></div><div className="hidden sm:block text-xs text-app-muted muted font-medium mt-0.5">{subtitle}</div></div></div>
    <div className="flex items-center gap-2 sm:gap-3 shrink-0 relative">
      <div className="relative hidden md:block w-60 xl:w-72"><Search className="w-4 h-4 text-gray-400 absolute left-4 top-1/2 -translate-y-1/2"/><input value={search} onChange={e => {setSearch(e.target.value);setSearchOpen(true)}} onFocus={() => setSearchOpen(true)} placeholder="Search employees..." className="w-full pl-11 pr-4 py-2.5 bg-white surface border border-transparent rounded-full text-sm font-medium placeholder-gray-400 focus:outline-none focus:border-gray-300 shadow-sm"/>{searchOpen && search && <div className="floating-panel absolute right-0 top-12 z-50 w-[390px] max-h-[420px] overflow-y-auto surface bg-white rounded-[24px] shadow-2xl border border-app-border p-2">{results.length ? results.map(e => <button key={e.id} onClick={() => {navigate('/employees');setSearchOpen(false);setSearch('')}} className="w-full p-3 rounded-2xl hover:bg-app-subtle text-left"><div className="text-xs font-bold txt">{e.name}</div><div className="text-[10px] text-app-muted muted">{e.title} · {e.email}</div></button>) : <div className="p-4 text-xs text-app-muted muted">No employees found.</div>}</div>}</div>
      <button onClick={quickCreate} className="bg-[#1A1D1F] dark-primary hover:bg-black text-white px-4 sm:px-5 py-2.5 rounded-full text-xs sm:text-sm font-bold tracking-wide flex items-center gap-2"><Plus className="w-4 h-4"/><span className="hidden sm:inline">Create</span></button>
      <button onClick={() => {setNotificationsOpen(v=>!v);setProfileOpen(false)}} className="w-10 h-10 bg-white surface rounded-full flex items-center justify-center shadow-sm hover:bg-gray-50 hover-surface relative"><Bell className="w-4 h-4"/><span className="absolute top-1.5 right-1.5 w-2 h-2 rounded-full bg-app-pink"/></button>
      <button onClick={() => {setProfileOpen(v=>!v);setNotificationsOpen(false)}} className="w-10 h-10 rounded-full bg-[#DEE8FF] avatar-soft shadow-sm flex items-center justify-center text-[11px] font-extrabold">{currentEmployee.avatar}</button>
      {notificationsOpen && <div className="floating-panel absolute right-12 top-12 z-50 w-[340px] surface bg-white rounded-[24px] border border-app-border shadow-2xl p-4"><div className="text-sm font-extrabold mb-3 txt">Notifications</div>{db.notifications.map((n,i)=><div key={i} className="py-3 border-t border-app-border first:border-0"><div className="text-xs font-bold txt">{n.title}</div><div className="text-[10px] text-app-muted muted mt-1">{n.detail}</div><div className="text-[9px] text-app-muted muted mt-1">{n.time}</div></div>)}</div>}
      {profileOpen && <div className="floating-panel absolute right-0 top-12 z-50 w-[290px] surface bg-white rounded-[24px] border border-app-border shadow-2xl p-3"><div className="p-3"><div className="text-sm font-extrabold txt">{currentEmployee.name}</div><div className="text-[10px] text-app-muted muted mt-1">{currentEmployee.email}</div></div><div className="border-t border-app-border pt-2"><div className="px-3 py-2 text-[9px] uppercase tracking-wider font-extrabold text-app-muted muted">Demo role</div>{Object.entries({admin:'Manager / Admin',supervisor:'Supervisor / Team Lead',employee:'Employee'}).map(([key,label])=><button key={key} onClick={()=>{setRole(key);setProfileOpen(false);navigate('/dashboard')}} className={`w-full text-left px-3 py-2.5 rounded-xl text-xs font-bold ${role===key?'bg-app-subtle':'hover:bg-app-subtle'}`}>{label}</button>)}</div></div>}
    </div>
  </header>
}
