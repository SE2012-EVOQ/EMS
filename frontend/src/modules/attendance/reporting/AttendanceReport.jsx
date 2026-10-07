import { useEffect, useState } from 'react'
import { Download, RefreshCw } from 'lucide-react'
import { useAuth } from '../../../context/AuthContext'
import Card from '../../../components/common/Card'
import MetricCard from '../../../components/common/MetricCard'
import { attendanceService } from '../services/attendanceService'
import { reportService } from './reportService'

const inputClass = 'mt-2 w-full rounded-xl border border-app-border bg-app-subtle px-3 py-2.5 text-xs font-semibold txt'
const labelClass = 'block text-[10px] font-extrabold uppercase tracking-wider text-app-muted muted'
export function ReportMetrics({ counts }) {
  const metrics = [['Present records', counts.present, 'BadgeCheck'], ['Late records', counts.late, 'Clock3'],
    ['Absent records', counts.absent, 'UserRoundX'], ['Recorded leave', counts.recordedLeave, 'CalendarDays'],
    ['Approved leave days', counts.approvedLeaveDays, 'CalendarCheck'], ['Recorded hours', Number(counts.workingHours).toFixed(2), 'Clock3']]
  return <div className="mb-5 grid grid-cols-2 gap-3 lg:grid-cols-3">{metrics.map(([label, value, icon]) => <MetricCard key={label} label={label} value={value} icon={icon} />)}</div>
}

export default function AttendanceReport() {
  const { user } = useAuth()
  const [filters, setFilters] = useState({ scope: 'MINE', from: '', to: '', teamId: '', employeeId: '' })
  const [teams, setTeams] = useState([])
  const [employees, setEmployees] = useState([])
  const [loaded, setLoaded] = useState(null)
  const [loading, setLoading] = useState(false)
  const [exporting, setExporting] = useState(false)
  const [error, setError] = useState('')
  const [optionsError, setOptionsError] = useState('')
  const [reload, setReload] = useState(0)
  useEffect(() => {
    let active = true
    Promise.all([attendanceService.today(), reportService.options({ scope: 'MINE' })]).then(([today, options]) => {
      if (!active) return
      setTeams(options.teams)
      setFilters(current => current.from ? current : { ...current, from: `${today.date.slice(0, 7)}-01`, to: today.date })
    }).catch(err => { if (active) setOptionsError(err.message) })
    return () => { active = false }
  }, [reload])
  useEffect(() => {
    let active = true
    setOptionsError(''); setEmployees([])
    if (filters.scope === 'TEAM' && !filters.teamId) return () => { active = false }
    reportService.options({ scope: filters.scope, teamId: filters.scope === 'MINE' ? '' : filters.teamId }).then(data => {
      if (active) setEmployees(data.employees)
    }).catch(err => { if (active) setOptionsError(err.message) })
    return () => { active = false }
  }, [filters.scope, filters.teamId, reload])
  useEffect(() => {
    let active = true
    setLoaded(null); setError(''); setLoading(false)
    if (!filters.from || !filters.to || (filters.scope === 'TEAM' && !filters.teamId)) return () => { active = false }
    if (filters.from > filters.to) { setError('The end date must be on or after the start date.'); return () => { active = false } }
    setLoading(true)
    reportService.report(filters).then(report => { if (active) setLoaded({ report, filters: { ...filters } }) })
      .catch(err => { if (active) setError(err.message) }).finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [filters.scope, filters.from, filters.to, filters.teamId, filters.employeeId, reload])
  const set = (key, value) => setFilters(current => ({ ...current, [key]: value, ...(key === 'scope' ? { teamId: '', employeeId: '' } : key === 'teamId' ? { employeeId: '' } : {}) }))
  const exportCsv = async () => {
    setExporting(true); setError('')
    try {
      const blob = await reportService.csv(loaded.filters)
      const url = URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = url; link.download = `attendance-${loaded.filters.from}-${loaded.filters.to}.csv`
      document.body.appendChild(link); link.click(); link.remove()
      window.setTimeout(() => URL.revokeObjectURL(url), 1000)
    } catch (err) { setError(err.message) } finally { setExporting(false) }
  }
  const report = loaded?.report
  return <>
    <Card className="mb-5">
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-5">
        <label className={labelClass}>View<select className={inputClass} value={filters.scope} onChange={e => set('scope', e.target.value)}>
          <option value="MINE">My attendance</option>{user?.role === 'SUPERVISOR' && <option value="TEAM">My team</option>}{user?.role === 'MANAGER_ADMIN' && <><option value="TEAM">Team</option><option value="ORGANIZATION">Organization</option></>}
        </select></label>
        <label className={labelClass}>From<input type="date" className={inputClass} value={filters.from} onChange={e => set('from', e.target.value)} /></label>
        <label className={labelClass}>To<input type="date" className={inputClass} value={filters.to} onChange={e => set('to', e.target.value)} /></label>
        {filters.scope !== 'MINE' && <label className={labelClass}>Team<select className={inputClass} value={filters.teamId} onChange={e => set('teamId', e.target.value)}><option value="">{filters.scope === 'TEAM' ? 'Choose a team' : 'All teams'}</option>{teams.map(team => <option key={team.id} value={team.id}>{team.name}</option>)}</select></label>}
        {filters.scope !== 'MINE' && <label className={labelClass}>Employee<select className={inputClass} value={filters.employeeId} onChange={e => set('employeeId', e.target.value)}><option value="">All permitted employees</option>{employees.map(employee => <option key={employee.id} value={employee.id}>{employee.name}</option>)}</select></label>}
      </div>
      <div className="mt-4 flex flex-wrap items-center gap-3">
        <button type="button" onClick={() => setReload(v => v + 1)} className="inline-flex items-center gap-2 rounded-xl border border-app-border px-4 py-2 text-xs font-bold"><RefreshCw className="h-4 w-4" />Refresh</button>
        <button type="button" onClick={exportCsv} disabled={!loaded || loading || exporting} className="inline-flex items-center gap-2 rounded-xl bg-[#1A1D1F] px-4 py-2 text-xs font-bold text-white disabled:opacity-50"><Download className="h-4 w-4" />{exporting ? 'Exporting…' : 'Export CSV'}</button>
        {report && <span className="text-xs text-app-muted">Business date {report.businessDate} · {report.businessTimezone}</span>}
      </div>
    </Card>
    {(error || optionsError) && <p role="alert" className="mb-5 rounded-xl bg-app-pink-bg p-4 text-sm text-app-pink">{error || optionsError}</p>}
    {loading && <p role="status" className="mb-5 text-sm text-app-muted">Loading attendance report…</p>}
    {!loading && !error && !report && <p className="mb-5 text-sm text-app-muted">Choose a valid date range and report scope to continue.</p>}
    {report && <>
      <ReportMetrics counts={report.totals} />
      <Card className="mb-5"><p className="text-xs text-app-muted">Counts use recorded statuses; Present and Late are separate. Hours use stored attendance hours, including existing administrative corrections; open check-ins contribute their stored hours. Approved leave counts distinct covered calendar dates per employee in this range, independently of recorded attendance. These counts are not additive. Missing records are not inferred as absences.</p>
        <p className="mt-2 text-xs text-app-muted">{report.totals.overlapDays} approved-leave days also have attendance records; {report.totals.openCheckIns} open check-ins. Leave/attendance precedence remains unresolved. Team scope uses current membership; supervisors see current active direct reports.</p></Card>
      <Card className="mb-5"><h3 className="mb-4 text-sm font-extrabold txt">Employee summaries</h3>
        <div className="overflow-x-auto"><table className="w-full text-left text-xs"><thead><tr>{['Employee', 'Present', 'Late', 'Absent', 'Recorded leave', 'Approved leave days', 'Hours'].map(v => <th key={v} className="whitespace-nowrap p-3 text-app-muted">{v}</th>)}</tr></thead>
          <tbody>{report.employees.map(e => <tr key={e.employeeId} className="border-t border-app-border"><td className="p-3 font-bold">{e.employeeName}</td>{[e.counts.present, e.counts.late, e.counts.absent, e.counts.recordedLeave, e.counts.approvedLeaveDays, Number(e.counts.workingHours).toFixed(2)].map((v, i) => <td key={i} className="p-3">{v}</td>)}</tr>)}</tbody></table></div>
        {!report.employees.length && <p className="p-3 text-sm text-app-muted">No employees in this permitted scope.</p>}
      </Card>
      <Card><h3 className="mb-4 text-sm font-extrabold txt">Daily facts · {report.days.length}</h3><div className="overflow-x-auto"><table className="w-full text-left text-xs"><thead><tr>{['Employee', 'Date', 'Recorded status', 'Check in', 'Check out', 'Hours', 'Approved leave'].map(v => <th key={v} className="whitespace-nowrap p-3 text-app-muted">{v}</th>)}</tr></thead>
        <tbody>{report.days.map(d => <tr key={`${d.employeeId}-${d.date}`} className="border-t border-app-border"><td className="p-3 font-bold">{d.employeeName}</td><td className="whitespace-nowrap p-3">{d.date}</td><td className="p-3">{d.recordedStatus || 'No attendance record'}</td><td className="p-3">{d.checkIn || '—'}</td><td className="p-3">{d.checkOut || '—'}</td><td className="p-3">{Number(d.workingHours).toFixed(2)}</td><td className="p-3">{d.approvedLeave ? 'Yes' : 'No'}</td></tr>)}</tbody></table></div>{!report.days.length && <p className="p-3 text-sm text-app-muted">No recorded attendance or approved leave in this range.</p>}</Card>
    </>}
  </>
}
