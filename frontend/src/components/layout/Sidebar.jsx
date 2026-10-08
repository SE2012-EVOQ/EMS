import { BarChart3, BadgeCheck, CalendarDays, CalendarRange, LayoutGrid, Laptop, Moon, Sun, UserRound, Users, X } from 'lucide-react'
import { useEffect, useRef } from 'react'
import { NavLink } from 'react-router-dom'
import { useTheme } from '../../context/ThemeContext'
import AccountMenu from './AccountMenu'
import { useAuth } from '../../context/AuthContext'
import { navigationForRole } from '../../app/roleAccess'

const icons = { BarChart3, BadgeCheck, CalendarDays, CalendarRange, LayoutGrid, Laptop, UserRound, Users }

export default function Sidebar({ open, onClose }) {
  const { theme, setTheme } = useTheme()
  const { user } = useAuth()
  const navItems = navigationForRole(user?.role)
  const drawer = useRef(null)
  const close = useRef(onClose)
  close.current = onClose
  useEffect(() => {
    if (!open) return
    const previous = document.activeElement
    const links = () => [...drawer.current.querySelectorAll('button, a')].filter(element => element.getClientRects().length)
    links()[0]?.focus()
    const handleKey = event => {
      if (event.key === 'Escape') { event.preventDefault(); close.current() }
      if (event.key === 'Tab') {
        const elements = links()
        const first = elements[0], last = elements[elements.length - 1]
        if (event.shiftKey && document.activeElement === first) { event.preventDefault(); last?.focus() }
        else if (!event.shiftKey && document.activeElement === last) { event.preventDefault(); first?.focus() }
      }
    }
    document.addEventListener('keydown', handleKey)
    return () => { document.removeEventListener('keydown', handleKey); if (previous?.isConnected) previous.focus() }
  }, [open])
  return (
    <>
      <div className={`fixed inset-0 z-30 bg-black/20 transition-all duration-300 lg:hidden ${open ? 'opacity-100 backdrop-blur-sm' : 'opacity-0 pointer-events-none'}`} onClick={onClose} />
      <aside ref={drawer} role={open ? 'dialog' : undefined} aria-modal={open || undefined} aria-label="Navigation" id="sidebar" className={`fixed lg:static z-40 inset-y-0 left-0 w-[232px] shrink-0 p-4 lg:p-5 flex flex-col justify-between bg-app-bg transition-transform duration-300 ease-out ${open ? 'translate-x-0 visible' : '-translate-x-full invisible lg:translate-x-0 lg:visible'}`}>
        <div className="min-h-0 flex flex-col">
          <div className="flex items-center justify-between px-2 mb-6 shrink-0">
            <div className="flex items-center gap-3">
              <div className="brandmark w-10 h-10 rounded-full border-2 border-black flex items-center justify-center p-1 bg-white"><div className="w-full h-full rounded-full border-2 border-black grid grid-cols-2 grid-rows-2 overflow-hidden"><div className="bg-black"/><div/><div/><div className="bg-black"/></div></div>
              <div><div className="text-sm font-extrabold tracking-tight txt">EVOQ</div><div className="text-[10px] font-semibold text-app-muted muted">Employee system</div></div>
            </div>
            <button aria-label="Close menu" className="lg:hidden w-9 h-9 rounded-full hover:bg-white" onClick={onClose}><X className="w-4 h-4 mx-auto" /></button>
          </div>
          <nav aria-label="Main navigation" className="space-y-1.5 overflow-y-auto pr-1">
            {navItems.map(([to,label,icon]) => { const Icon = icons[icon]; return <NavLink key={to} to={to} onClick={onClose} className={({isActive}) => `w-full flex items-center gap-3 px-4 py-3 rounded-2xl text-xs font-bold transition ${isActive ? 'nav-active bg-white shadow-sm txt' : 'text-app-muted muted hover:bg-white/70 hover-surface'}`}><Icon className="w-4 h-4" /><span>{label}</span></NavLink> })}
          </nav>
        </div>
        <div className="pt-4 shrink-0 space-y-3">
          <div className="flex items-center gap-2"><div className="theme-switcher flex items-center"><button aria-label="Dark theme" aria-pressed={theme === 'dark'} onClick={() => setTheme('dark')} className={`theme-button flex items-center justify-center ${theme === 'dark' ? 'theme-selected' : 'text-app-muted'}`}><Moon className="w-4 h-4" /></button><button aria-label="Light theme" aria-pressed={theme === 'light'} onClick={() => setTheme('light')} className={`theme-button flex items-center justify-center ${theme === 'light' ? 'theme-selected' : 'text-app-muted'}`}><Sun className="w-4 h-4" /></button></div><span className="text-[10px] font-semibold text-app-muted muted ml-1">Display</span></div>
          <AccountMenu onNavigate={onClose} />
        </div>
      </aside>
    </>
  )
}
