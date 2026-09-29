import { useState } from 'react'
import { Outlet, useLocation } from 'react-router-dom'
import Sidebar from './Sidebar'
import Header from './Header'

export default function AppLayout() {
  const [mobileOpen, setMobileOpen] = useState(false)
  const { pathname } = useLocation()
  return <div id="app-shell" className="w-screen h-screen bg-app-bg overflow-hidden flex"><Sidebar open={mobileOpen} onClose={() => setMobileOpen(false)} /><main className="flex-1 min-w-0 h-screen flex flex-col overflow-hidden"><Header onMenu={() => setMobileOpen(true)} />{!pathname.startsWith('/account/') && <div className="px-4 sm:px-6 lg:px-8 pb-3 text-[11px] font-semibold text-app-muted">Module records shown below are prototype data until their APIs are connected.</div>}<div className="flex-1 overflow-y-auto px-4 sm:px-6 lg:px-8 pb-7"><Outlet /></div></main></div>
}
