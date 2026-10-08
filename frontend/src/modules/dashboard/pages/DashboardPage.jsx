import ModuleSummaries from '../ModuleSummaries'
import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { useAuth } from '../../../context/AuthContext'
import Card from '../../../components/common/Card'
import PageHeader from '../../../components/common/PageHeader'
import MetricCard from '../../../components/common/MetricCard'
import RequestFeedback from '../../../components/common/RequestFeedback'
import { requestErrorMessage } from '../../../components/common/requestError'
import TodayAttendanceActions from '../../attendance/components/TodayAttendanceActions'
import { ReportMetrics } from '../../attendance/reporting/AttendanceReport'
import { reportService } from '../../attendance/reporting/reportService'

export default function DashboardPage() {
  const { user } = useAuth()
  const [teams, setTeams] = useState([])
  const [teamId, setTeamId] = useState('')
  const [ready, setReady] = useState(false)
  const [setupLoading, setSetupLoading] = useState(true)
  const [setupError, setSetupError] = useState('')
  const [data, setData] = useState(null)
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  const [reload, setReload] = useState(0)
  useEffect(() => {
    let active = true
    setReady(false); setSetupLoading(true); setSetupError(''); setData(null)
    reportService.options({ scope: 'MINE' }).then(options => {
      if (!active) return
      setTeams(options.teams)
      setTeamId(current => options.teams.some(team => String(team.id) === current) ? current : String(options.teams[0]?.id || ''))
      setReady(true)
    }).catch(err => { if (active) setSetupError(requestErrorMessage(err, 'Dashboard')) })
      .finally(() => { if (active) setSetupLoading(false) })
    return () => { active = false }
  }, [reload])
  const scope = user?.role === 'MANAGER_ADMIN' ? 'ORGANIZATION' : user?.role === 'SUPERVISOR' && teamId ? 'TEAM' : 'MINE'
  useEffect(() => {
    if (!ready) return
    let active = true
    let requestId = 0
    const refresh = () => {
      const id = ++requestId
      setError(''); setLoading(true); setData(null)
      reportService.dashboard({ scope, teamId: scope === 'TEAM' ? teamId : '' }).then(result => { if (active && id === requestId) setData(result) })
        .catch(err => { if (active && id === requestId) setError(requestErrorMessage(err, 'Dashboard')) })
        .finally(() => { if (active && id === requestId) setLoading(false) })
    }
    refresh()
    const timer = window.setInterval(() => { if (!document.hidden) refresh() }, 30000)
    return () => { active = false; window.clearInterval(timer) }
  }, [ready, scope, teamId])
  const title = scope === 'ORGANIZATION' ? 'Organization attendance today' : scope === 'TEAM' ? 'Team attendance today' : 'My attendance today'
  const busy = setupLoading || (ready && loading)
  const retry = () => setReload(value => value + 1)
  return <>
    <PageHeader primary title="Dashboard" actions={<button type="button" onClick={retry} disabled={busy} className="rounded-xl border border-app-border px-4 py-2 text-xs font-bold disabled:opacity-50">Refresh dashboard</button>} />
    <TodayAttendanceActions />
    <Card className="mb-5"><div className="flex flex-wrap items-center justify-between gap-3"><h2 className="text-sm font-bold">{title}</h2>{scope === 'TEAM' && <label className="text-xs">Team<select aria-label="Dashboard team" className="ml-3 rounded-xl border border-app-border p-2" value={teamId} onChange={e => setTeamId(e.target.value)} disabled={!ready}>{teams.map(t => <option key={t.id} value={t.id}>{t.name}</option>)}</select></label>}<Link to="/reports" className="text-xs font-bold underline">Open attendance reports</Link></div>
      {data && <p className="mt-2 text-xs text-app-muted">{data.attendance.businessDate} · {data.attendance.businessTimezone}</p>}
      {user?.role === 'SUPERVISOR' && !teamId && ready && <p className="mt-2 text-xs text-app-muted">No team assigned. Showing your attendance.</p>}
    </Card>
    <RequestFeedback error={setupError || (ready ? error : '')} loading={busy} loadingText="Loading dashboard…" onRetry={retry} />
    {ready && !loading && !error && data && <><div className="mb-5 grid grid-cols-2 gap-3"><MetricCard label="Published shifts today" value={data.publishedShifts} icon="CalendarDays" /><MetricCard label="Open check-ins" value={data.attendance.totals.openCheckIns} icon="Clock3" /></div><ReportMetrics counts={data.attendance.totals} />
      <p className="text-xs text-app-muted">Approved leave is counted separately; totals may overlap. {data.attendance.totals.overlapDays} leave days also have attendance records.</p></>}
    <ModuleSummaries reload={reload} />
  </>
}
