import { useCallback, useEffect, useMemo, useState } from 'react'
import { CalendarPlus, LoaderCircle, Send } from 'lucide-react'
import Card from '../../../components/common/Card'
import PageHeader from '../../../components/common/PageHeader'
import { useAuth } from '../../../context/AuthContext'
import ScheduleCalendar from '../components/ScheduleCalendar'
import ScheduleEditor from '../components/ScheduleEditor'
import TodayAttendanceActions from '../components/TodayAttendanceActions'
import { scheduleService } from '../services/scheduleService'

const dateString = date => `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
const rangeAroundToday = () => {
  const now = new Date()
  return { from: dateString(new Date(now.getFullYear(), now.getMonth(), now.getDate() - 7)), to: dateString(new Date(now.getFullYear(), now.getMonth(), now.getDate() + 21)) }
}

export default function SchedulePage() {
  const { user } = useAuth()
  const supervisor = user?.role === 'SUPERVISOR'
  const range = useMemo(rangeAroundToday, [])
  const [teams, setTeams] = useState([])
  const [teamId, setTeamId] = useState('')
  const [employees, setEmployees] = useState([])
  const [schedules, setSchedules] = useState([])
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')
  const [editor, setEditor] = useState(null)
  const [reload, setReload] = useState(0)

  useEffect(() => {
    if (!supervisor) return
    let active = true
    scheduleService.teams().then(data => { if (active) { setTeams(data); setTeamId(current => current || String(data[0]?.id || '')) } })
      .catch(err => { if (active) setError(err.message) })
    return () => { active = false }
  }, [supervisor, reload])

  useEffect(() => {
    let active = true
    setLoading(true); setError('')
    const request = supervisor && teamId ? scheduleService.team(teamId, range, true) : supervisor ? Promise.resolve([]) : scheduleService.mine(range)
    request.then(data => { if (active) setSchedules(data) }).catch(err => { if (active) { setSchedules([]); setError(err.message) } })
      .finally(() => { if (active) setLoading(false) })
    if (supervisor && teamId) scheduleService.employees(teamId).then(data => { if (active) setEmployees(data) }).catch(err => { if (active) setError(err.message) })
    return () => { active = false }
  }, [supervisor, teamId, range, reload])

  const refresh = useCallback(() => setReload(value => value + 1), [])
  const save = () => { setEditor(null); refresh() }
  const publish = async id => {
    setSaving(true); setError('')
    try { await scheduleService.publish(id); refresh() } catch (err) { setError(err.message) } finally { setSaving(false) }
  }

  return <>
    <PageHeader title="Schedule" description={supervisor ? 'Plan, validate and publish shifts for your permitted team.' : 'View your published shifts and record attendance for today.'} actions={supervisor && <button type="button" disabled={!teamId || !employees.length} onClick={() => setEditor({ mode: 'create' })} className="inline-flex items-center gap-2 rounded-2xl bg-[#1A1D1F] px-4 py-2.5 text-xs font-bold text-white disabled:opacity-50"><CalendarPlus className="h-4 w-4" />Create schedule</button>} />
    {!supervisor && <TodayAttendanceActions />}
    {supervisor && teams.length > 0 && <div className="mb-5 flex items-center gap-3"><label htmlFor="schedule-team" className="text-xs font-bold text-app-muted">Team</label><select id="schedule-team" value={teamId} onChange={event => setTeamId(event.target.value)} className="min-w-56 rounded-xl border border-app-border bg-white px-3 py-2.5 text-xs font-semibold txt">{teams.map(team => <option key={team.id} value={team.id}>{team.name}</option>)}</select></div>}
    {error && <div role="alert" className="mb-4 rounded-xl bg-app-pink-bg px-4 py-3 text-xs font-semibold text-app-pink">{error}</div>}
    {loading ? <Card><div role="status" className="flex items-center justify-center gap-2 py-12 text-xs font-semibold text-app-muted"><LoaderCircle className="h-4 w-4 animate-spin" />Loading schedules…</div></Card>
      : !schedules.length ? <Card><ScheduleCalendar entries={[]} /></Card>
        : <div className="space-y-5">{schedules.map(schedule => <Card key={schedule.id}>
          <div className="mb-4 flex flex-wrap items-start justify-between gap-3"><div><div className="flex items-center gap-2"><h2 className="text-sm font-extrabold txt">{schedule.teamName}</h2><span className={`rounded-full px-2.5 py-1 text-[9px] font-extrabold uppercase tracking-wider ${schedule.status === 'PUBLISHED' ? 'bg-app-green-bg text-app-green' : 'bg-app-subtle text-app-muted'}`}>{schedule.status}</span></div><p className="mt-1 text-xs text-app-muted">{schedule.periodStart} – {schedule.periodEnd} · {schedule.entries.length} entries</p></div>
            {supervisor && <div className="flex gap-2"><button type="button" onClick={() => setEditor({ mode: 'edit', schedule })} className="rounded-xl border border-app-border px-3 py-2 text-xs font-bold txt">Edit</button>{schedule.status !== 'PUBLISHED' && <button type="button" onClick={() => publish(schedule.id)} disabled={saving} className="inline-flex items-center gap-2 rounded-xl bg-[#1A1D1F] px-3 py-2 text-xs font-bold text-white disabled:opacity-50"><Send className="h-3.5 w-3.5" />Publish</button>}</div>}
          </div>
          <ScheduleCalendar entries={schedule.entries} showEmployees={supervisor} />
        </Card>)}</div>}
    {editor && <ScheduleEditor schedule={editor.schedule} teamId={teamId} employees={employees} initialPeriod={range} onClose={() => setEditor(null)} onSaved={save} />}
  </>
}
