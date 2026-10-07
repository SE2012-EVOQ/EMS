import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { useAuth } from '../../../context/AuthContext'
import Card from '../../../components/common/Card'
import PageHeader from '../../../components/common/PageHeader'
import MetricCard from '../../../components/common/MetricCard'
import TodayAttendanceActions from '../../attendance/components/TodayAttendanceActions'
import { ReportMetrics } from '../../attendance/reporting/AttendanceReport'
import { reportService } from '../../attendance/reporting/reportService'

export default function DashboardPage() {
  const { user } = useAuth()
  const [teams, setTeams] = useState([])
  const [teamId, setTeamId] = useState('')
  const [ready, setReady] = useState(false)
  const [data, setData] = useState(null)
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(true)
  const [reload, setReload] = useState(0)
  useEffect(() => {
    let active = true
    reportService.options({ scope: 'MINE' }).then(options => {
      if (!active) return
      setTeams(options.teams); setTeamId(current => current || String(options.teams[0]?.id || '')); setReady(true)
    }).catch(err => { if (active) { setError(err.message); setLoading(false) } })
    return () => { active = false }
  }, [reload])
  const scope = user?.role === 'MANAGER_ADMIN' ? 'ORGANIZATION' : user?.role === 'SUPERVISOR' && teamId ? 'TEAM' : 'MINE'
  useEffect(() => {
    if (!ready) return
    let active = true
    const refresh = () => {
      setError(''); setLoading(true)
      reportService.dashboard({ scope, teamId: scope === 'TEAM' ? teamId : '' }).then(result => { if (active) setData(result) })
        .catch(err => { if (active) { setData(null); setError(err.message) } }).finally(() => { if (active) setLoading(false) })
    }
    setData(null); refresh()
    const timer = window.setInterval(() => { if (!document.hidden) refresh() }, 30000)
    return () => { active = false; window.clearInterval(timer) }
  }, [ready, scope, teamId, reload])
  const title = scope === 'ORGANIZATION' ? 'Organization attendance today' : scope === 'TEAM' ? 'Team attendance today' : 'My attendance today'
  return <>
    <PageHeader title="Dashboard" description="Attendance and published shifts from the business server." actions={<button type="button" onClick={() => setReload(v => v + 1)} className="rounded-xl border border-app-border px-4 py-2 text-xs font-bold">Refresh dashboard</button>} />
    <TodayAttendanceActions />
    <Card className="mb-5"><div className="flex flex-wrap items-center justify-between gap-3"><h2 className="text-sm font-bold">{title}</h2>{scope === 'TEAM' && <label className="text-xs">Team<select aria-label="Dashboard team" className="ml-3 rounded-xl border border-app-border p-2" value={teamId} onChange={e => setTeamId(e.target.value)}>{teams.map(t => <option key={t.id} value={t.id}>{t.name}</option>)}</select></label>}<Link to="/reports" className="text-xs font-bold underline">Open attendance reports</Link></div>
      {data && <p className="mt-2 text-xs text-app-muted">{data.attendance.businessDate} · {data.attendance.businessTimezone}</p>}
      {user?.role === 'SUPERVISOR' && !teamId && ready && <p className="mt-2 text-xs text-app-muted">No permitted team is assigned; showing your own attendance.</p>}
    </Card>
    {error && <p role="alert" className="mb-5 rounded-xl bg-app-pink-bg p-4 text-sm text-app-pink">{error}</p>}
    {loading && <p role="status" className="mb-5 text-sm text-app-muted">Refreshing dashboard…</p>}
    {data && <><div className="mb-5 grid grid-cols-2 gap-3"><MetricCard label="Published shifts today" value={data.publishedShifts} icon="CalendarDays" /><MetricCard label="Open check-ins" value={data.attendance.totals.openCheckIns} icon="Clock3" /></div><ReportMetrics counts={data.attendance.totals} />
      <Card><p className="text-xs text-app-muted">Recorded statuses and approved leave are independent; counts are not additive. {data.attendance.totals.overlapDays} leave dates also have records. Absences appear after existing reconciliation records them; missing rows are not counted as absent. Other modules' dashboard metrics await their owners' authorized data sources.</p></Card></>}
  </>
}
