import { useMemo, useState } from 'react'
import { Plus, Trash2 } from 'lucide-react'
import Modal from '../../../components/common/Modal'
import { FormField, SelectField } from '../../../components/common/FormField'
import { scheduleService } from '../services/scheduleService'

const emptyEntry = (date, employees) => ({ employeeId: employees[0]?.id ? String(employees[0].id) : '', workDate: date, startTime: '09:00', endTime: '17:00', notes: '' })

export default function ScheduleEditor({ schedule, teamId, employees, initialPeriod, onClose, onSaved }) {
  const [periodStart, setPeriodStart] = useState(schedule?.periodStart || initialPeriod.from)
  const [periodEnd, setPeriodEnd] = useState(schedule?.periodEnd || initialPeriod.to)
  const [entries, setEntries] = useState(() => schedule?.entries?.map(entry => ({
    id: entry.id, employeeId: String(entry.employeeId), workDate: entry.workDate,
    startTime: entry.startTime?.slice(0, 8), endTime: entry.endTime?.slice(0, 8), notes: entry.notes || ''
  })) || [emptyEntry(initialPeriod.from, employees)])
  const [removedEntryIds, setRemovedEntryIds] = useState([])
  const [editedEntryIds, setEditedEntryIds] = useState([])
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')
  const employeeOptions = useMemo(() => employees.map(employee => <option key={employee.id} value={employee.id}>{employee.name}</option>), [employees])

  const updateEntry = (index, key, value) => {
    const entry = entries[index]
    if (entry.id) setEditedEntryIds(current => current.includes(entry.id) ? current : [...current, entry.id])
    setEntries(current => current.map((item, i) => i === index ? { ...item, [key]: value } : item))
  }
  const removeEntry = index => {
    const entry = entries[index]
    if (entry.id) setRemovedEntryIds(current => [...current, entry.id])
    setEntries(current => current.filter((_, i) => i !== index))
  }
  const submit = async event => {
    event.preventDefault()
    setSaving(true); setError('')
    const payload = { teamId: Number(teamId), periodStart, periodEnd, removedEntryIds, entries: entries
      .filter(entry => !entry.id || editedEntryIds.includes(entry.id)).map(entry => ({
      ...(entry.id ? { id: entry.id } : {}), employeeId: Number(entry.employeeId), workDate: entry.workDate,
      startTime: entry.startTime, endTime: entry.endTime, notes: entry.notes.trim() || null
    })) }
    try {
      if (schedule) await scheduleService.update(schedule.id, payload)
      else await scheduleService.create(payload)
      onSaved()
    } catch (err) { setError(err.message) } finally { setSaving(false) }
  }

  return <Modal open onClose={() => { if (!saving) onClose() }} title={schedule ? 'Update team schedule' : 'Create team schedule'} subtitle={schedule?.status === 'PUBLISHED' ? 'Changes to this published schedule are visible to employees as soon as you save.' : 'Drafts do not reserve employee time. Conflicts with published shifts are checked when you publish.'}>
    <form onSubmit={submit} className="space-y-5">
      <div className="grid gap-4 sm:grid-cols-2">
        <FormField label="Period starts" type="date" value={periodStart} onChange={event => setPeriodStart(event.target.value)} required />
        <FormField label="Period ends" type="date" value={periodEnd} onChange={event => setPeriodEnd(event.target.value)} required />
      </div>
      <div className="space-y-3">
        <div className="flex items-center justify-between"><h3 className="text-xs font-extrabold txt">Schedule entries</h3><button type="button" onClick={() => setEntries(current => [...current, emptyEntry(periodStart, employees)])} className="inline-flex items-center gap-1 text-xs font-bold txt"><Plus className="h-3.5 w-3.5" />Add entry</button></div>
        {!employees.length && <p className="rounded-xl bg-app-subtle p-3 text-xs text-app-muted">No active direct reports are assigned to this team.</p>}
        {entries.map((entry, index) => <div key={entry.id || index} className="rounded-2xl border border-app-border p-3">
          <div className="mb-3 flex items-center justify-between"><span className="text-[10px] font-bold uppercase tracking-wider text-app-muted">Entry {index + 1}</span><button type="button" aria-label="Remove entry" onClick={() => removeEntry(index)} className="rounded-lg p-1 text-app-muted hover:text-app-pink"><Trash2 className="h-4 w-4" /></button></div>
          <div className="grid gap-3 sm:grid-cols-2">
            <SelectField label="Employee" value={entry.employeeId} onChange={event => updateEntry(index, 'employeeId', event.target.value)} required><option value="">Choose employee</option>{!employees.some(employee => String(employee.id) === entry.employeeId) && <option value={entry.employeeId}>Employee #{entry.employeeId} (historical)</option>}{employeeOptions}</SelectField>
            <FormField label="Work date" type="date" value={entry.workDate} min={periodStart} max={periodEnd} onChange={event => updateEntry(index, 'workDate', event.target.value)} required />
            <FormField label="Start" type="time" step="1" value={entry.startTime} onChange={event => updateEntry(index, 'startTime', event.target.value)} required />
            <FormField label="End" type="time" step="1" value={entry.endTime} onChange={event => updateEntry(index, 'endTime', event.target.value)} required />
          </div>
          <FormField label="Note (optional)" value={entry.notes} onChange={event => updateEntry(index, 'notes', event.target.value)} maxLength={255} />
        </div>)}
      </div>
      {error && <p role="alert" className="rounded-xl bg-app-pink-bg px-4 py-3 text-xs font-semibold text-app-pink">{error}</p>}
      <div className="flex justify-end gap-2"><button type="button" disabled={saving} onClick={onClose} className="rounded-xl border border-app-border px-4 py-2.5 text-xs font-bold txt">Cancel</button><button type="submit" disabled={saving || !employees.length || !entries.length} className="rounded-xl bg-[#1A1D1F] px-4 py-2.5 text-xs font-bold text-white disabled:opacity-50">{saving ? 'Saving…' : schedule?.status === 'PUBLISHED' ? 'Save published changes' : 'Save draft'}</button></div>
    </form>
  </Modal>
}
