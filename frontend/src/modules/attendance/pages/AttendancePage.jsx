import { useEffect, useMemo, useState } from 'react'
import { CalendarDays, Clock3, ClipboardCheck, Plus, UserRoundCheck } from 'lucide-react'
import Card from '../../../components/common/Card'
import PageHeader from '../../../components/common/PageHeader'
import { useAuth } from '../../../context/AuthContext'
import AttendanceTable from '../components/AttendanceTable'
import AttendanceEditor from '../components/AttendanceEditor'
import { attendanceService } from '../services/attendanceService'

const dateString = date => `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
const initialRange = () => {
  const now = new Date()
  return { from: dateString(new Date(now.getFullYear(), now.getMonth(), 1)), to: dateString(new Date(now.getFullYear(), now.getMonth() + 1, 0)) }
}

export default function AttendancePage() {
  const { user } = useAuth()
  const role = user?.role
  const canManage = role === 'MANAGER_ADMIN'
  const isSupervisor = role === 'SUPERVISOR'
  const [view, setView] = useState('mine')
  const [range, setRange] = useState(initialRange)
  const [status, setStatus] = useState('ALL')
  const [employeeId, setEmployeeId] = useState('')
  const [teams, setTeams] = useState([])
  const [teamId, setTeamId] = useState('')
  const [employees, setEmployees] = useState([])
  const [rows, setRows] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [lookupError, setLookupError] = useState('')
  const [reload, setReload] = useState(0)
  const [editor, setEditor] = useState(null)

  useEffect(() => {
    let active = true
    setLookupError('')
    if (isSupervisor) {
      attendanceService.teams().then(data => {
        if (!active) return
        setTeams(data)
        setTeamId(current => current || String(data[0]?.id || ''))
      }).catch(err => { if (active) setLookupError(err.message) })
    }
    if (canManage) {
      attendanceService.employees().then(data => { if (active) setEmployees(data) })
        .catch(err => { if (active) setLookupError(err.message) })
    }
    return () => { active = false }
  }, [isSupervisor, canManage, reload])

  useEffect(() => {
    let active = true
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
      .catch(err => { if (active) { setRows([]); setError(err.message) } })
      .finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [view, range.from, range.to, teamId, employeeId, reload])

  const visibleRows = useMemo(() => status === 'ALL' ? rows : rows.filter(row => row.status === status), [rows, status])
  const stats = useMemo(() => ({
    days: visibleRows.length,
    present: visibleRows.filter(row => row.status === 'PRESENT' || row.status === 'LATE').length,
    absent: visibleRows.filter(row => row.status === 'ABSENT').length,
    hours: visibleRows.reduce((total, row) => total + Number(row.hours || 0), 0).toFixed(2)
  }), [visibleRows])
  const tabs = [{ id: 'mine', label: 'My attendance' }]
  if (isSupervisor) tabs.push({ id: 'team', label: 'Team attendance' })
  if (canManage) tabs.push({ id: 'all', label: 'All records' })

  return <>
    <PageHeader title="Attendance records" description="Review employee check-ins, check-outs and administrative exceptions." actions={canManage && <button type="button" onClick={() => setEditor({ mode: 'create' })} className="inline-flex items-center gap-2 rounded-2xl bg-[#1A1D1F] px-4 py-2.5 text-xs font-bold text-white"><Plus className="h-4 w-4" />Record exception</button>} />
    <div className="mb-5 flex gap-2 overflow-x-auto" role="tablist" aria-label="Attendance views">
      {tabs.map(tab => <button key={tab.id} type="button" role="tab" aria-selected={view === tab.id} onClick={() => setView(tab.id)} className={`shrink-0 rounded-full px-4 py-2.5 text-xs font-bold ${view === tab.id ? 'bg-[#1A1D1F] text-white' : 'surface border border-app-border bg-white txt'}`}>{tab.label}</button>)}
    </div>
    <Card className="mb-5">
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <label className="block text-[10px] font-extrabold uppercase tracking-wider text-app-muted muted">From<input type="date" value={range.from} onChange={event => setRange(current => ({ ...current, from: event.target.value }))} className="mt-2 w-full rounded-xl border border-app-border bg-app-subtle px-3 py-2.5 text-xs font-semibold txt" /></label>
        <label className="block text-[10px] font-extrabold uppercase tracking-wider text-app-muted muted">To<input type="date" value={range.to} onChange={event => setRange(current => ({ ...current, to: event.target.value }))} className="mt-2 w-full rounded-xl border border-app-border bg-app-subtle px-3 py-2.5 text-xs font-semibold txt" /></label>
        <label className="block text-[10px] font-extrabold uppercase tracking-wider text-app-muted muted">Status<select value={status} onChange={event => setStatus(event.target.value)} className="mt-2 w-full rounded-xl border border-app-border bg-app-subtle px-3 py-2.5 text-xs font-semibold txt"><option value="ALL">All statuses</option><option value="PRESENT">Present</option><option value="LATE">Late</option><option value="ABSENT">Absent</option><option value="LEAVE">Leave</option></select></label>
        {view === 'team' ? <label className="block text-[10px] font-extrabold uppercase tracking-wider text-app-muted muted">Team<select value={teamId} onChange={event => setTeamId(event.target.value)} className="mt-2 w-full rounded-xl border border-app-border bg-app-subtle px-3 py-2.5 text-xs font-semibold txt"><option value="">No assigned team</option>{teams.map(team => <option key={team.id} value={team.id}>{team.name}</option>)}</select></label>
          : view === 'all' ? <label className="block text-[10px] font-extrabold uppercase tracking-wider text-app-muted muted">Employee<select value={employeeId} onChange={event => setEmployeeId(event.target.value)} className="mt-2 w-full rounded-xl border border-app-border bg-app-subtle px-3 py-2.5 text-xs font-semibold txt"><option value="">All employees</option>{employees.map(employee => <option key={employee.id} value={employee.id}>{employee.name}</option>)}</select></label> : null}
      </div>
    </Card>
    <div className="mb-5 grid grid-cols-2 gap-3 lg:grid-cols-4">
      {[['Recorded days', stats.days, CalendarDays], ['Present', stats.present, UserRoundCheck], ['Absent', stats.absent, ClipboardCheck], ['Total hours', stats.hours, Clock3]].map(([label, value, Icon]) => <div key={label} className="surface rounded-[24px] border border-app-border/50 bg-white p-4 shadow-card sm:p-5"><div className="flex items-center gap-2 text-xs font-semibold text-app-muted muted"><Icon className="h-4 w-4" />{label}</div><div className="mt-3 text-2xl font-extrabold tracking-tight txt sm:text-3xl">{value}</div></div>)}
    </div>
    <Card>
      <div className="mb-4 flex items-center justify-between gap-3"><div><h3 className="text-sm font-extrabold txt">Records</h3><p className="mt-1 text-xs text-app-muted muted">Employee check-in/out is the normal workflow. This table includes administrative exceptions.</p></div><span className="text-xs font-bold text-app-muted muted">{visibleRows.length} records</span></div>
      {error && <div role="alert" className="mb-4 flex items-center justify-between gap-3 rounded-xl bg-app-pink-bg px-4 py-3 text-xs text-app-pink"><span>{error}</span><button type="button" onClick={() => setReload(value => value + 1)} className="font-bold underline">Retry</button></div>}
      {lookupError && <div role="alert" className="mb-4 flex items-center justify-between gap-3 rounded-xl bg-app-pink-bg px-4 py-3 text-xs text-app-pink"><span>{lookupError}</span><button type="button" onClick={() => setReload(value => value + 1)} className="font-bold underline">Retry</button></div>}
      {loading ? <div role="status" className="py-12 text-center text-xs font-semibold text-app-muted muted">Loading attendance…</div> : !error && <AttendanceTable rows={visibleRows} canEdit={canManage} onEdit={record => setEditor({ mode: 'correct', record })} />}
    </Card>
    {editor && <AttendanceEditor record={editor.record} employees={employees} onClose={() => setEditor(null)} onSaved={date => { const selected = new Date(`${date}T00:00:00`); setEditor(null); setView('all'); setEmployeeId(''); setStatus('ALL'); setRange({ from: dateString(new Date(selected.getFullYear(), selected.getMonth(), 1)), to: dateString(new Date(selected.getFullYear(), selected.getMonth() + 1, 0)) }); setReload(value => value + 1) }} />}
  </>
}
