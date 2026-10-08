import { useLayoutEffect, useRef, useState } from 'react'
import { Outlet, useLocation } from 'react-router-dom'
import { Menu } from 'lucide-react'
import Sidebar from './Sidebar'

export default function AppLayout() {
  const [mobileOpen, setMobileOpen] = useState(false)
  const { pathname } = useLocation()
  const content = useRef(null)
  useLayoutEffect(() => { content.current?.scrollTo({ top: 0 }) }, [pathname])
  return <div id="app-shell" className="w-full h-dvh bg-app-bg overflow-hidden flex">
    <a href="#main-content" className="skip-link">Skip to content</a>
    <Sidebar open={mobileOpen} onClose={() => setMobileOpen(false)} />
    <button type="button" onClick={() => setMobileOpen(true)} aria-label="Open menu" aria-expanded={mobileOpen} aria-controls="sidebar" className="fixed bottom-5 left-4 z-20 lg:hidden w-11 h-11 surface bg-white border border-app-border rounded-full shadow-lg flex items-center justify-center"><Menu className="w-5 h-5" /></button>
    <main id="main-content" tabIndex={-1} className="flex-1 min-w-0 h-dvh flex flex-col overflow-hidden">
      <div ref={content} className="flex-1 min-h-0 overflow-y-auto px-4 sm:px-6 lg:px-8 pt-6 pb-24 lg:pb-6"><div className="w-full max-w-[1600px] mx-auto"><Outlet /></div></div>
    </main>
  </div>
}
