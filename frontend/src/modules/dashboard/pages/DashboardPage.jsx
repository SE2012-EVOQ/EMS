import ModuleSummaries from '../ModuleSummaries'
import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { useAuth } from '../../../context/AuthContext'
import PageHeader from '../../../components/common/PageHeader'
import RequestFeedback from '../../../components/common/RequestFeedback'
import { requestErrorMessage } from '../../../components/common/requestError'
import TodayAttendanceActions from '../../attendance/components/TodayAttendanceActions'
import { ReportMetrics } from '../../attendance/reporting/AttendanceReport'
import { reportService } from '../../attendance/reporting/reportService'
import { hasTeamWorkspace } from '../../../app/roleAccess'
import PersonalDashboard from '../PersonalDashboard'

export default function DashboardPage() {
  const { user } = useAuth()
  return hasTeamWorkspace(user?.role) ? <ManagementDashboard role={user.role} /> : <PersonalDashboard />
}

function ManagementDashboard({ role }) {
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
  const scope = role === 'MANAGER_ADMIN' ? 'ORGANIZATION' : teamId ? 'TEAM' : 'MINE'
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
    <PageHeader primary title="Dashboard" actions={<button type="button" onClick={retry} disabled={busy} className="rounded-xl border border-app-border px-4 py-2 text-xs font-bold disabled:opacity-50">Refresh</button>} />
    <TodayAttendanceActions />
    <section className="mb-4"><div className="flex flex-wrap items-center justify-between gap-3"><h2 className="text-sm font-bold">{title}</h2>{scope === 'TEAM' && <label className="text-xs">Team<select aria-label="Dashboard team" className="ml-3 rounded-xl border border-app-border p-2" value={teamId} onChange={e => setTeamId(e.target.value)} disabled={!ready}>{teams.map(t => <option key={t.id} value={t.id}>{t.name}</option>)}</select></label>}<Link to="/reports" className="text-xs font-bold underline">View reports</Link></div>
      {data && <p className="mt-2 text-xs text-app-muted">{data.attendance.businessDate} · {data.attendance.businessTimezone}</p>}
      {role === 'SUPERVISOR' && !teamId && ready && <p className="mt-2 text-xs text-app-muted">No team assigned. Showing your attendance.</p>}
    </section>
    <RequestFeedback error={setupError || (ready ? error : '')} loading={busy} loadingText="Loading dashboard…" onRetry={retry} />
    {ready && !loading && !error && data && <><ReportMetrics counts={data.attendance.totals} leading={[
      ['Published shifts', data.publishedShifts, 'CalendarDays'],
      ['Open check-ins', data.attendance.totals.openCheckIns, 'Clock3']
    ]} />
      {data.attendance.totals.overlapDays > 0 && <p className="text-xs text-app-muted">{data.attendance.totals.overlapDays} leave days also have attendance records.</p>}</>}
    <ModuleSummaries reload={reload} role={role} />
  </>
}
