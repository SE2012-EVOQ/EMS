import { useCallback, useEffect, useState } from 'react'
import { CalendarPlus, LoaderCircle, Send } from 'lucide-react'
import Card from '../../../components/common/Card'
import RequestFeedback from '../../../components/common/RequestFeedback'
import { requestErrorMessage } from '../../../components/common/requestError'
import PageHeader from '../../../components/common/PageHeader'
import { useAuth } from '../../../context/AuthContext'
import ScheduleCalendar from '../components/ScheduleCalendar'
import ScheduleEditor from '../components/ScheduleEditor'
import TodayAttendanceActions from '../components/TodayAttendanceActions'
import { scheduleService } from '../services/scheduleService'
import { attendanceService } from '../services/attendanceService'

const rangeAroundToday = businessDate => {
  const offset = days => {
    const date = new Date(`${businessDate}T00:00:00Z`)
    date.setUTCDate(date.getUTCDate() + days)
    return date.toISOString().slice(0, 10)
  }
  return { from: offset(-7), to: offset(21) }
}

export default function SchedulePage() {
  const { user } = useAuth()
  const canManage = user?.role === 'SUPERVISOR' || user?.role === 'MANAGER_ADMIN'
  const [view, setView] = useState('team')
  const showingTeam = canManage && view === 'team'
  const [businessDate, setBusinessDate] = useState(null)
  const [range, setRange] = useState({ from: '', to: '' })
  const [teams, setTeams] = useState([])
  const [teamId, setTeamId] = useState('')
  const [employees, setEmployees] = useState([])
  const [schedules, setSchedules] = useState([])
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')
  const [lookupError, setLookupError] = useState('')
  const [teamsLoading, setTeamsLoading] = useState(false)
  const [teamsReady, setTeamsReady] = useState(false)
  const [employeesLoading, setEmployeesLoading] = useState(false)
  const [editor, setEditor] = useState(null)
  const [reload, setReload] = useState(0)

  useEffect(() => {
    if (businessDate) setRange(current => current.from || current.to ? current : rangeAroundToday(businessDate))
  }, [businessDate])

  useEffect(() => {
    if (!showingTeam) { setLookupError(''); return }
    setTeamsLoading(true); setTeamsReady(false); setLookupError(''); setEmployees([])
    let active = true
    scheduleService.teams().then(data => { if (active) { setTeams(data); setTeamsReady(true); setTeamId(current => data.some(team => String(team.id) === current) ? current : String(data[0]?.id || '')) } })
      .catch(err => { if (active) setLookupError(requestErrorMessage(err, 'Schedule team selection')) })
      .finally(() => { if (active) setTeamsLoading(false) })
    return () => { active = false }
  }, [showingTeam, reload])

  useEffect(() => {
    let active = true
    setSchedules([]); setError(''); setEmployees([]); setEmployeesLoading(false)
    if ((showingTeam && !teamsReady) || !range.from || !range.to || range.from > range.to) {
      setSchedules([])
      setLoading(false)
      if (range.from && range.to && range.from > range.to) setError('Choose an end date on or after the start date.')
      return () => { active = false }
    }
    setLoading(true); setError('')
    const request = showingTeam ? teamId ? scheduleService.team(teamId, range, true) : Promise.resolve([]) : scheduleService.mine(range)
    request.then(data => { if (active) setSchedules(data) }).catch(err => { if (active) { setSchedules([]); setError(requestErrorMessage(err, 'Schedule')) } })
      .finally(() => { if (active) setLoading(false) })
    if (showingTeam && teamId) {
      setEmployeesLoading(true)
      scheduleService.employees(teamId).then(data => { if (active) setEmployees(data) })
        .catch(err => { if (active) setError(requestErrorMessage(err, 'Team member list')) })
        .finally(() => { if (active) setEmployeesLoading(false) })
    }
    else setEmployees([])
    return () => { active = false }
  }, [showingTeam, teamsReady, teamId, range.from, range.to, reload])

  const refresh = useCallback(() => setReload(value => value + 1), [])
  const save = () => { setEditor(null); refresh() }
  const create = async () => {
    setSaving(true); setError('')
    try {
      const today = await attendanceService.today()
      setBusinessDate(today.date)
      if (range.to < today.date) {
        setError('The selected period ends before the current business date. Choose a current or future period.')
        return
      }
      setEditor({ mode: 'create' })
    } catch (err) { setError(requestErrorMessage(err, 'Schedule')) } finally { setSaving(false) }
  }
  const publish = async id => {
    setSaving(true); setError('')
    try { await scheduleService.publish(id); refresh() } catch (err) { setError(requestErrorMessage(err, 'Schedule')) } finally { setSaving(false) }
  }
  const edit = async id => {
    setSaving(true); setError('')
    try {
      const [schedule, today] = await Promise.all([scheduleService.get(id), attendanceService.today()])
      setBusinessDate(today.date)
      setEditor({ mode: 'edit', schedule })
    }
    catch (err) { setError(requestErrorMessage(err, 'Schedule')) }
    finally { setSaving(false) }
  }
  const discard = async id => {
    if (!window.confirm('Discard this draft schedule and all its entries?')) return
    setSaving(true); setError('')
    try { await scheduleService.discardDraft(id); refresh() }
    catch (err) { setError(requestErrorMessage(err, 'Schedule')) }
    finally { setSaving(false) }
  }

  return <>
    <PageHeader primary title="Schedule" actions={showingTeam && <button type="button" disabled={saving || loading || teamsLoading || employeesLoading || Boolean(error || lookupError) || !teamId || !employees.length || !businessDate || !range.from || !range.to || range.from > range.to || range.to < businessDate} onClick={create} className="inline-flex items-center gap-2 rounded-2xl bg-[#1A1D1F] dark-primary px-4 py-2.5 text-xs font-bold text-white disabled:opacity-50"><CalendarPlus className="h-4 w-4" />Create schedule</button>} />
    <TodayAttendanceActions onBusinessDate={setBusinessDate} />
    {canManage && <div className="mb-5 flex gap-2" role="tablist" aria-label="Schedule views">
      <button type="button" role="tab" aria-selected={showingTeam} onClick={() => { setEditor(null); setView('team') }} className={`rounded-full px-4 py-2.5 text-xs font-bold ${showingTeam ? 'bg-[#1A1D1F] dark-primary text-white' : 'surface border border-app-border bg-white txt'}`}>Team schedules</button>
      <button type="button" role="tab" aria-selected={!showingTeam} onClick={() => { setEditor(null); setView('mine') }} className={`rounded-full px-4 py-2.5 text-xs font-bold ${!showingTeam ? 'bg-[#1A1D1F] dark-primary text-white' : 'surface border border-app-border bg-white txt'}`}>My shifts</button>
    </div>}
    <div className="mb-5 flex flex-wrap items-end gap-3">
      <label className="text-xs font-bold text-app-muted">From<input type="date" value={range.from} onChange={event => setRange(current => ({ ...current, from: event.target.value }))} className="mt-1 block rounded-xl border border-app-border bg-white px-3 py-2 txt" /></label>
      <label className="text-xs font-bold text-app-muted">To<input type="date" value={range.to} onChange={event => setRange(current => ({ ...current, to: event.target.value }))} className="mt-1 block rounded-xl border border-app-border bg-white px-3 py-2 txt" /></label>
    {showingTeam && teams.length > 0 && <label className="text-xs font-bold text-app-muted">Team<select id="schedule-team" disabled={!teamsReady || teamsLoading} value={teamId} onChange={event => setTeamId(event.target.value)} className="mt-1 block min-w-0 w-full sm:w-56 rounded-xl border border-app-border bg-white px-3 py-2.5 text-xs font-semibold txt">{teams.map(team => <option key={team.id} value={team.id}>{team.name}</option>)}</select></label>}
      <button type="button" disabled={!businessDate} onClick={() => setRange(rangeAroundToday(businessDate))} className="h-10 rounded-xl border border-app-border px-3 py-2 text-xs font-bold txt disabled:opacity-50">Today’s range</button>
    </div>

    <RequestFeedback error={lookupError || error} onRetry={refresh} />
    {showingTeam && teamsReady && !teamsLoading && !teams.length && !error && !lookupError && <p className="mb-4 text-xs text-app-muted">Create a team and assign active employees to it before scheduling shifts.</p>}
    {showingTeam && teamsReady && !teamsLoading && !loading && !employeesLoading && teams.length > 0 && !employees.length && !error && !lookupError && <p className="mb-4 text-xs text-app-muted">This team has no active employees available to schedule.</p>}
    {businessDate && !error && !lookupError && (loading || (showingTeam && (teamsLoading || employeesLoading)) ? <Card><div role="status" className="flex items-center justify-center gap-2 py-12 text-xs font-semibold text-app-muted"><LoaderCircle className="h-4 w-4 animate-spin" />Loading schedules…</div></Card>
      : !range.from || !range.to ? <p className="text-sm text-app-muted">Choose a start and end date to view schedules.</p>
      : !schedules.length ? <Card><ScheduleCalendar entries={[]} /></Card>
        : <div className="space-y-5">{schedules.map(schedule => <Card key={schedule.id}>
          <div className="mb-4 flex flex-wrap items-start justify-between gap-3"><div><div className="flex items-center gap-2"><h2 className="text-sm font-extrabold txt">{schedule.teamName}</h2><span className={`rounded-full px-2.5 py-1 text-[9px] font-extrabold uppercase tracking-wider ${schedule.status === 'PUBLISHED' ? 'bg-app-green-bg text-app-green' : 'bg-app-subtle text-app-muted'}`}>{schedule.status}</span></div><p className="mt-1 text-xs text-app-muted">{schedule.periodStart} – {schedule.periodEnd} · {schedule.entries.length} entries</p></div>
            {showingTeam && <div className="flex gap-2"><button type="button" onClick={() => edit(schedule.id)} disabled={saving} className="rounded-xl border border-app-border px-3 py-2 text-xs font-bold txt disabled:opacity-50">Edit</button>{schedule.status !== 'PUBLISHED' && <><button type="button" onClick={() => discard(schedule.id)} disabled={saving} className="rounded-xl border border-app-border px-3 py-2 text-xs font-bold txt disabled:opacity-50">Discard draft</button><button type="button" onClick={() => publish(schedule.id)} disabled={saving} className="inline-flex items-center gap-2 rounded-xl bg-[#1A1D1F] dark-primary px-3 py-2 text-xs font-bold text-white disabled:opacity-50"><Send className="h-3.5 w-3.5" />Publish</button></>}</div>}
          </div>
          <ScheduleCalendar entries={schedule.entries} showEmployees={showingTeam} />
        </Card>)}</div>)}
    {showingTeam && editor && businessDate && <ScheduleEditor schedule={editor.schedule} teamId={teamId} employees={employees} initialPeriod={range} businessDate={businessDate} onClose={() => setEditor(null)} onSaved={save} />}
  </>
}
