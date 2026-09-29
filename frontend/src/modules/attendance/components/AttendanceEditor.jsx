import { useState } from 'react'
import Modal from '../../../components/common/Modal'
import { FormField, SelectField } from '../../../components/common/FormField'
import { attendanceService } from '../services/attendanceService'

const localDate = () => {
  const now = new Date()
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`
}

const initialValues = record => ({
  employeeId: String(record?.employeeId || ''),
  date: record?.date || localDate(),
  status: record?.status || 'PRESENT',
  checkIn: record?.checkIn?.slice(0, 5) || '',
  checkOut: record?.checkOut?.slice(0, 5) || '',
  note: ''
})

export default function AttendanceEditor({ record, employees, onClose, onSaved }) {
  const [values, setValues] = useState(() => initialValues(record))
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')
  const worked = values.status === 'PRESENT' || values.status === 'LATE'
  const update = (key, value) => setValues(current => ({ ...current, [key]: value }))

  const submit = async event => {
    event.preventDefault()
    setError('')
    setSaving(true)
    const payload = {
      employeeId: Number(values.employeeId), date: values.date, status: values.status,
      checkIn: worked ? values.checkIn : null,
      checkOut: worked ? values.checkOut : null,
      note: values.note.trim() || null
    }
    try {
      if (record) await attendanceService.correct(record.id, {
        status: payload.status, checkIn: payload.checkIn, checkOut: payload.checkOut, note: payload.note
      })
      else await attendanceService.createException(payload)
      onSaved(payload.date)
    } catch (err) {
      setError(err.message)
    } finally {
      setSaving(false)
    }
  }

  return <Modal open onClose={() => { if (!saving) onClose() }} title={record ? 'Correct attendance exception' : 'Record attendance exception'} subtitle={record ? 'Employee and date stay fixed; the server recalculates hours.' : 'For missed check-ins, missed check-outs or other exceptions. Normal attendance uses employee check-in.'}>
    <form onSubmit={submit} className="space-y-4">
      <SelectField label="Employee" value={values.employeeId} onChange={event => update('employeeId', event.target.value)} required disabled={Boolean(record)}>
        <option value="">Select an employee</option>
        {employees.map(employee => <option key={employee.id} value={employee.id}>{employee.name}</option>)}
        {record && !employees.some(employee => String(employee.id) === values.employeeId) && <option value={values.employeeId}>{record.employeeName}</option>}
      </SelectField>
      <div className="grid gap-4 sm:grid-cols-2">
        <FormField label="Date" type="date" value={values.date} onChange={event => update('date', event.target.value)} required disabled={Boolean(record)} />
        <SelectField label="Status" value={values.status} onChange={event => update('status', event.target.value)} required>
          <option value="PRESENT">Present</option><option value="LATE">Late</option><option value="ABSENT">Absent</option><option value="LEAVE">Leave</option>
        </SelectField>
      </div>
      <div className="grid gap-4 sm:grid-cols-2">
        <FormField label="Check in" type="time" value={values.checkIn} onChange={event => update('checkIn', event.target.value)} disabled={!worked} required={worked} />
        <FormField label="Check out (optional for an open record)" type="time" value={values.checkOut} onChange={event => update('checkOut', event.target.value)} disabled={!worked} />
      </div>
      <p className="text-xs text-app-muted muted">{worked ? 'Hours are calculated by the server. This version supports same-day shifts.' : 'Absent and leave statuses are administrative exceptions; no Leave workflow is changed.'}</p>
      <label className="block"><span className="text-[10px] font-extrabold uppercase tracking-wider text-app-muted muted">Note (optional)</span><textarea value={values.note} onChange={event => update('note', event.target.value)} maxLength={255} rows={3} className="mt-2 w-full rounded-2xl border border-transparent bg-app-subtle px-4 py-3 text-xs font-semibold outline-none focus:border-gray-300" /></label>
      {error && <p role="alert" className="rounded-xl bg-app-pink-bg px-4 py-3 text-xs font-semibold text-app-pink">{error}</p>}
      <div className="flex justify-end gap-2 pt-2"><button type="button" onClick={onClose} disabled={saving} className="rounded-xl border border-app-border px-4 py-2.5 text-xs font-bold txt">Cancel</button><button type="submit" disabled={saving} className="rounded-xl bg-[#1A1D1F] px-4 py-2.5 text-xs font-bold text-white disabled:opacity-50">{saving ? 'Saving…' : record ? 'Save correction' : 'Record exception'}</button></div>
    </form>
  </Modal>
}
