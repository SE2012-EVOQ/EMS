import { BarChart3, BadgeCheck, CalendarDays, CalendarRange, LayoutGrid, Laptop, Moon, Sun, Users, X } from 'lucide-react'
import { NavLink } from 'react-router-dom'
import { useTheme } from '../../context/ThemeContext'

const navItems = [
  ['/dashboard','Dashboard',LayoutGrid], ['/employees','Employees',Users], ['/leave','Leave',CalendarDays],
  ['/attendance','Attendance',BadgeCheck], ['/schedule','Schedule',CalendarRange], ['/assets','Assets',Laptop], ['/reports','Reports',BarChart3]
]

export default function Sidebar({ open, onClose }) {
  const { theme, setTheme } = useTheme()
  return (
    <>
      <div className={`fixed inset-0 z-30 bg-black/20 transition-all duration-300 lg:hidden ${open ? 'opacity-100 backdrop-blur-sm' : 'opacity-0 pointer-events-none'}`} onClick={onClose} />
      <aside id="sidebar" className={`fixed lg:static z-40 inset-y-0 left-0 w-[270px] shrink-0 p-5 lg:p-6 flex flex-col justify-between bg-app-bg transition-transform duration-300 ease-out ${open ? 'translate-x-0' : '-translate-x-full lg:translate-x-0'}`}>
        <div className="min-h-0">
          <div className="flex items-center justify-between px-2 mb-8">
            <div className="flex items-center gap-3">
              <div className="brandmark w-10 h-10 rounded-full border-2 border-black flex items-center justify-center p-1 bg-white"><div className="w-full h-full rounded-full border-2 border-black grid grid-cols-2 grid-rows-2 overflow-hidden"><div className="bg-black"/><div/><div/><div className="bg-black"/></div></div>
              <div><div className="text-sm font-extrabold tracking-tight txt">EVOQ</div><div className="text-[10px] font-semibold text-app-muted muted">Employee system</div></div>
            </div>
            <button className="lg:hidden w-9 h-9 rounded-full hover:bg-white" onClick={onClose}><X className="w-4 h-4 mx-auto" /></button>
          </div>
          <nav className="space-y-1.5 overflow-y-auto pr-1">
            {navItems.map(([to,label,Icon]) => <NavLink key={to} to={to} onClick={onClose} className={({isActive}) => `w-full flex items-center gap-3 px-4 py-3 rounded-2xl text-xs font-bold transition ${isActive ? 'nav-active bg-white shadow-sm txt' : 'text-app-muted muted hover:bg-white/70 hover-surface'}`}><Icon className="w-4 h-4" /><span>{label}</span></NavLink>)}
          </nav>
        </div>
        <div className="pt-4">
          <div className="flex items-center gap-2"><div className="theme-switcher flex items-center"><button onClick={() => setTheme('dark')} className={`theme-button flex items-center justify-center ${theme === 'dark' ? 'theme-selected' : 'text-app-muted'}`}><Moon className="w-4 h-4" /></button><button onClick={() => setTheme('light')} className={`theme-button flex items-center justify-center ${theme === 'light' ? 'theme-selected' : 'text-app-muted'}`}><Sun className="w-4 h-4" /></button></div><span className="text-[10px] font-semibold text-app-muted muted ml-1">Display</span></div>
        </div>
      </aside>
    </>
  )
}
