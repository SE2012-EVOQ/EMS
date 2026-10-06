import { useEffect, useState } from 'react'
import { Clock3, LogIn, LogOut, RefreshCw } from 'lucide-react'
import { attendanceService } from '../services/attendanceService'

const stateLabel = {
  NO_SCHEDULE: 'No published shift today', NOT_OPEN: 'Check-in opens at the scheduled start',
  AVAILABLE: 'Check-in is available', WINDOW_CLOSED: 'Check-in window closed',
  CHECKED_IN: 'Checked in', COMPLETED: 'Shift attendance completed', ALREADY_RECORDED: 'Attendance already recorded',
  ON_LEAVE: 'Approved leave today', ABSENT: 'Marked absent after scheduled shift'
}

export default function TodayAttendanceActions({ onBusinessDate }) {
  const [today, setToday] = useState(null)
  const [loading, setLoading] = useState(true)
  const [working, setWorking] = useState(null)
  const [error, setError] = useState('')
  const refresh = async () => {
    setError('')
    try {
      const status = await attendanceService.today()
      setToday(status)
      onBusinessDate?.(status.date)
    } catch (err) { setError(err.message) } finally { setLoading(false) }
  }
  useEffect(() => {
    refresh()
    const timer = window.setInterval(() => { if (!document.hidden) refresh() }, 30000)
    const onVisible = () => { if (!document.hidden) refresh() }
    document.addEventListener('visibilitychange', onVisible)
    return () => { window.clearInterval(timer); document.removeEventListener('visibilitychange', onVisible) }
  }, [onBusinessDate])
  const act = async (action, operation) => {
    setWorking(action); setError('')
    try { await operation(); await refresh() } catch (err) { setError(err.message) } finally { setWorking(null) }
  }

  return <section className="surface mb-5 rounded-[24px] border border-app-border/50 bg-white p-5 shadow-card sm:p-6" aria-label="Today's attendance">
    <div className="flex flex-wrap items-start justify-between gap-4">
      <div><div className="flex items-center gap-2 text-xs font-bold text-app-muted"><Clock3 className="h-4 w-4" />TODAY'S PUBLISHED SHIFT</div>
        {loading ? <p role="status" className="mt-3 text-sm text-app-muted">Loading today…</p> : <>
          <h2 className="mt-2 text-xl font-extrabold txt">{today?.checkInState === 'ON_LEAVE' ? 'Approved leave' : today?.scheduled ? `${today.scheduledStart?.slice(0, 5)} – ${today.scheduledEnd?.slice(0, 5)}` : 'No shift assigned'}</h2>
          <p className="mt-1 text-xs font-semibold text-app-muted">{stateLabel[today?.checkInState] || 'Attendance status unavailable'}{today?.date ? ` · ${today.date}` : ''}</p>
          {today?.attendance && <p className="mt-2 text-xs text-app-muted">Check in {today.attendance.checkIn?.slice(0, 5) || '—'} · Check out {today.attendance.checkOut?.slice(0, 5) || '—'} · {Number(today.attendance.hours || 0).toFixed(2)} hours</p>}
        </>}</div>
      <div className="flex gap-2">
        <button type="button" onClick={refresh} disabled={working} title="Refresh server status" className="rounded-xl border border-app-border p-2.5 text-app-muted"><RefreshCw className="h-4 w-4" /></button>
        <button type="button" onClick={() => act('in', attendanceService.checkIn)} disabled={loading || working || !today?.canCheckIn} className="inline-flex items-center gap-2 rounded-xl bg-[#1A1D1F] px-4 py-2.5 text-xs font-bold text-white disabled:opacity-50"><LogIn className="h-4 w-4" />{working === 'in' ? 'Checking in…' : 'Check in'}</button>
        <button type="button" onClick={() => act('out', attendanceService.checkOut)} disabled={loading || working || !today?.canCheckOut} className="inline-flex items-center gap-2 rounded-xl bg-[#1A1D1F] px-4 py-2.5 text-xs font-bold text-white disabled:opacity-50"><LogOut className="h-4 w-4" />{working === 'out' ? 'Checking out…' : 'Check out'}</button>
      </div>
    </div>
    <p className="mt-3 text-[10px] text-app-muted">The server decides whether check-in is allowed and records all attendance times.</p>
    {error && <p role="alert" className="mt-3 rounded-xl bg-app-pink-bg px-4 py-3 text-xs font-semibold text-app-pink">{error}</p>}
  </section>
}
