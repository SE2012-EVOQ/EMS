import { useEffect, useMemo, useState } from 'react'
import { CalendarDays, Clock3, ClipboardCheck, Plus, UserRoundCheck } from 'lucide-react'
import Card from '../../../components/common/Card'
import PageHeader from '../../../components/common/PageHeader'
import RequestFeedback from '../../../components/common/RequestFeedback'
import { requestErrorMessage } from '../../../components/common/requestError'
import { useAuth } from '../../../context/AuthContext'
import AttendanceTable from '../components/AttendanceTable'
import AttendanceEditor from '../components/AttendanceEditor'
import { attendanceService } from '../services/attendanceService'

const monthRange = businessDate => {
  const date = new Date(`${businessDate}T00:00:00Z`)
  return { from: `${businessDate.slice(0, 7)}-01`, to: new Date(Date.UTC(date.getUTCFullYear(), date.getUTCMonth() + 1, 0)).toISOString().slice(0, 10) }
}

export default function AttendancePage() {
  const { user } = useAuth()
  const role = user?.role
  const canManage = role === 'MANAGER_ADMIN'
  const isSupervisor = role === 'SUPERVISOR'
  const [view, setView] = useState('mine')
  const [businessDate, setBusinessDate] = useState(null)
  const [range, setRange] = useState({ from: '', to: '' })
  const [status, setStatus] = useState('ALL')
  const [employeeId, setEmployeeId] = useState('')
  const [teams, setTeams] = useState([])
  const [teamId, setTeamId] = useState('')
  const [employees, setEmployees] = useState([])
  const [rows, setRows] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [lookupError, setLookupError] = useState('')
  const [setupLoading, setSetupLoading] = useState(true)
  const [ready, setReady] = useState(false)
  const [reload, setReload] = useState(0)
  const [editor, setEditor] = useState(null)
  const [openingEditor, setOpeningEditor] = useState(false)

  const recordException = async () => {
    setOpeningEditor(true); setLookupError('')
    try {
      const today = await attendanceService.today()
      setBusinessDate(today.date)
      setEditor({ mode: 'create' })
    } catch (err) { setLookupError(requestErrorMessage(err, 'Attendance setup')) } finally { setOpeningEditor(false) }
  }

  useEffect(() => {
    let active = true
    setLookupError(''); setSetupLoading(true); setReady(false); setRows([])
    Promise.all([attendanceService.today(), isSupervisor ? attendanceService.teams() : canManage ? attendanceService.employees() : Promise.resolve([])])
      .then(([today, options]) => {
        if (!active) return
        setBusinessDate(today.date)
        setRange(current => current.from || current.to ? current : monthRange(today.date))
        if (isSupervisor) {
          setTeams(options)
          setTeamId(current => options.some(team => String(team.id) === current) ? current : String(options[0]?.id || ''))
        }
        if (canManage) setEmployees(options)
        setReady(true)
      }).catch(err => { if (active) setLookupError(requestErrorMessage(err, 'Attendance setup')) })
      .finally(() => { if (active) setSetupLoading(false) })
    return () => { active = false }
  }, [isSupervisor, canManage, reload])

  useEffect(() => {
    let active = true
    setRows([]); setError('')
    if (!ready || !range.from || !range.to) {
      setLoading(false)
      return () => { active = false }
    }
    if (range.from > range.to) {
      setRows([])
      setError('The end date must be on or after the start date.')
      setLoading(false)
      return () => { active = false }
    }
    if (view === 'team' && !teamId) {
      setRows([])
      setError('')
      setLoading(false)
      return () => { active = false }
    }
    setLoading(true)
    setError('')
    const request = view === 'team' ? attendanceService.team(teamId, range)
      : view === 'all' ? attendanceService.all({ ...range, employeeId })
        : attendanceService.mine(range)
    request.then(data => { if (active) setRows(data) })
      .catch(err => { if (active) { setRows([]); setError(requestErrorMessage(err, 'Attendance records')) } })
      .finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [ready, view, range.from, range.to, teamId, employeeId, reload])

  const visibleRows = useMemo(() => status === 'ALL' ? rows : rows.filter(row => row.status === status), [rows, status])
  const stats = useMemo(() => ({
    days: visibleRows.length,
    present: visibleRows.filter(row => row.status === 'PRESENT').length,
    late: visibleRows.filter(row => row.status === 'LATE').length,
    absent: visibleRows.filter(row => row.status === 'ABSENT').length,
    hours: visibleRows.reduce((total, row) => total + Number(row.hours || 0), 0).toFixed(2)
  }), [visibleRows])
  const tabs = [{ id: 'mine', label: 'My attendance' }]
  if (isSupervisor) tabs.push({ id: 'team', label: 'Team attendance' })
  if (canManage) tabs.push({ id: 'all', label: 'All records' })

  return <>
    <PageHeader primary title="Attendance" actions={canManage && <button type="button" disabled={openingEditor || !businessDate || !ready || loading || Boolean(error || lookupError)} onClick={recordException} className="inline-flex items-center gap-2 rounded-2xl bg-[#1A1D1F] px-4 py-2.5 text-xs font-bold text-white disabled:opacity-50"><Plus className="h-4 w-4" />Record exception</button>} />
    <div className="mb-5 flex gap-2 overflow-x-auto" role="tablist" aria-label="Attendance views">
      {tabs.map(tab => <button key={tab.id} type="button" role="tab" aria-selected={view === tab.id} onClick={() => setView(tab.id)} className={`shrink-0 rounded-full px-4 py-2.5 text-xs font-bold ${view === tab.id ? 'bg-[#1A1D1F] text-white' : 'surface border border-app-border bg-white txt'}`}>{tab.label}</button>)}
    </div>
    <Card className="mb-5">
      <fieldset disabled={!ready || setupLoading} className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <label className="block text-[10px] font-extrabold uppercase tracking-wider text-app-muted muted">From<input type="date" value={range.from} onChange={event => setRange(current => ({ ...current, from: event.target.value }))} className="mt-2 w-full rounded-xl border border-app-border bg-app-subtle px-3 py-2.5 text-xs font-semibold txt" /></label>
        <label className="block text-[10px] font-extrabold uppercase tracking-wider text-app-muted muted">To<input type="date" value={range.to} onChange={event => setRange(current => ({ ...current, to: event.target.value }))} className="mt-2 w-full rounded-xl border border-app-border bg-app-subtle px-3 py-2.5 text-xs font-semibold txt" /></label>
        <label className="block text-[10px] font-extrabold uppercase tracking-wider text-app-muted muted">Status<select value={status} onChange={event => setStatus(event.target.value)} className="mt-2 w-full rounded-xl border border-app-border bg-app-subtle px-3 py-2.5 text-xs font-semibold txt"><option value="ALL">All statuses</option><option value="PRESENT">Present</option><option value="LATE">Late</option><option value="ABSENT">Absent</option><option value="LEAVE">Leave</option></select></label>
        {view === 'team' ? <label className="block text-[10px] font-extrabold uppercase tracking-wider text-app-muted muted">Team<select value={teamId} onChange={event => setTeamId(event.target.value)} className="mt-2 w-full rounded-xl border border-app-border bg-app-subtle px-3 py-2.5 text-xs font-semibold txt"><option value="">No assigned team</option>{teams.map(team => <option key={team.id} value={team.id}>{team.name}</option>)}</select></label>
          : view === 'all' ? <label className="block text-[10px] font-extrabold uppercase tracking-wider text-app-muted muted">Employee<select value={employeeId} onChange={event => setEmployeeId(event.target.value)} className="mt-2 w-full rounded-xl border border-app-border bg-app-subtle px-3 py-2.5 text-xs font-semibold txt"><option value="">All employees</option>{employees.map(employee => <option key={employee.id} value={employee.id}>{employee.name}</option>)}</select></label> : null}
      </fieldset>
    </Card>
    {ready && !loading && !error && !lookupError && range.from && range.to && (view !== 'team' || teamId) && <div className="mb-5 grid grid-cols-2 gap-3 lg:grid-cols-5">
      {[['Recorded days', stats.days, CalendarDays], ['Present records', stats.present, UserRoundCheck], ['Late records', stats.late, Clock3], ['Absent records', stats.absent, ClipboardCheck], ['Recorded hours', stats.hours, Clock3]].map(([label, value, Icon]) => <div key={label} className="surface rounded-[24px] border border-app-border/50 bg-white p-4 shadow-card sm:p-5"><div className="flex items-center gap-2 text-xs font-semibold text-app-muted muted"><Icon className="h-4 w-4" />{label}</div><div className="mt-3 text-2xl font-extrabold tracking-tight txt sm:text-3xl">{value}</div></div>)}
    </div>}
    <Card>
      <RequestFeedback error={lookupError || error} loading={setupLoading || (ready && loading)} loadingText="Loading attendance…" onRetry={() => setReload(value => value + 1)} />
      {ready && !loading && !error && !lookupError && (range.from && range.to ? view === 'team' && !teamId ? <p className="py-6 text-sm text-app-muted">No team assigned.</p> : <AttendanceTable rows={visibleRows} canEdit={canManage} onEdit={record => setEditor({ mode: 'correct', record })} /> : <p className="py-6 text-sm text-app-muted">Choose a start and end date to view attendance.</p>)}
    </Card>
    {editor && <AttendanceEditor record={editor.record} employees={employees} businessDate={businessDate} onClose={() => setEditor(null)} onSaved={date => { setEditor(null); setView('all'); setEmployeeId(''); setStatus('ALL'); setRange(monthRange(date)); setReload(value => value + 1) }} />}
  </>
}
