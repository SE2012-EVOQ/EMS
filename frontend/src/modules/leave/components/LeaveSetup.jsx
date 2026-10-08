import { useEffect, useState } from 'react'
import Card from '../../../components/common/Card'
import RequestFeedback from '../../../components/common/RequestFeedback'
import { employeeService } from '../../employees/services/employeeService'
import { getEmployeeLeaveSetup, saveLeaveType, setLeaveEntitlement } from '../services/leaveService'

export default function LeaveSetup({ types, onSaved }) {
  const [employees, setEmployees] = useState(null)
  const [employeeId, setEmployeeId] = useState('')
  const [balances, setBalances] = useState(null)
  const [typeId, setTypeId] = useState('')
  const [days, setDays] = useState('')
  const [editId, setEditId] = useState('')
  const [name, setName] = useState('')
  const [description, setDescription] = useState('')
  const [peopleError, setPeopleError] = useState('')
  const [balanceError, setBalanceError] = useState('')
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')
  const [busy, setBusy] = useState(false)
  const [reload, setReload] = useState(0)
  useEffect(() => {
    let active = true; setEmployees(null); setPeopleError('')
    employeeService.getAll().then(list => { if (active) setEmployees(list) }).catch(err => { if (active) setPeopleError(`Employee choices unavailable: ${err.message}`) })
    return () => { active = false }
  }, [reload])
  useEffect(() => {
    let active = true; setBalances(null); setBalanceError(''); setDays('')
    if (employeeId) getEmployeeLeaveSetup(employeeId).then(data => { if (active) setBalances(data.balances) }).catch(err => { if (active) setBalanceError(`Leave setup unavailable: ${err.message}`) })
    return () => { active = false }
  }, [employeeId, reload])
  useEffect(() => {
    const balance = balances?.find(b => String(b.leaveTypeId) === typeId)
    setDays(balance ? String(Number(balance.availableDays) + Number(balance.usedDays)) : '')
  }, [balances, typeId])
  async function save(action) {
    setBusy(true); setError(''); setMessage('')
    try { await action(); setMessage('Leave setup saved.'); setReload(x => x + 1); await onSaved() }
    catch (err) { setError(err.message || 'Leave setup failed') } finally { setBusy(false) }
  }
  const inputClass = 'block w-full rounded-xl border p-2'
  return <Card><details><summary className="cursor-pointer font-semibold">Leave setup</summary>
    <details className="my-4 text-xs text-app-muted"><summary>How entitlements work</summary><p className="mt-2 leading-relaxed">Available days equal total entitlement minus used days. Used days are retained. There are no automatic grants, resets or accruals. Requests count calendar days, including both dates.</p></details>
    {error && <p role="alert" className="text-app-pink mb-3">{error}</p>}{message && <p role="status" className="mb-3 text-app-green">{message}</p>}
    <form className="mb-5 grid gap-3 md:grid-cols-2 items-end" onSubmit={e => { e.preventDefault(); save(async () => { await saveLeaveType(editId, { name, description: description || null }); setEditId(''); setName(''); setDescription('') }) }}>
      <label>Leave type<select className={inputClass} value={editId} onChange={e => { const type = types.find(t => String(t.id) === e.target.value); setEditId(e.target.value); setName(type?.name || ''); setDescription(type?.description || '') }}><option value="">Create new type</option>{types.map(t => <option key={t.id} value={t.id}>{t.name}</option>)}</select></label>
      <label>Name<input required maxLength={100} className={inputClass} value={name} onChange={e => setName(e.target.value)} /></label>
      <label>Description<input maxLength={255} className={inputClass} value={description} onChange={e => setDescription(e.target.value)} /></label>
      <button disabled={busy} className="h-10 rounded-xl border p-2 disabled:opacity-50">{busy ? 'Saving…' : editId ? 'Update leave type' : 'Create leave type'}</button>
    </form>
    <RequestFeedback loading={!employees && !peopleError} error={peopleError} onRetry={() => setReload(x => x + 1)} />
    {employees && <form className="grid gap-3 md:grid-cols-2 items-end" onSubmit={e => { e.preventDefault(); save(() => setLeaveEntitlement(employeeId, typeId, Number(days))) }}>
      <label>Employee<select required className={inputClass} value={employeeId} onChange={e => setEmployeeId(e.target.value)}><option value="">Select employee</option>{employees.map(emp => <option key={emp.id} value={emp.id}>{emp.fullName} · {emp.status}</option>)}</select></label>
      <label>Leave type<select required className={inputClass} value={typeId} onChange={e => setTypeId(e.target.value)}><option value="">Select type</option>{types.map(t => <option key={t.id} value={t.id}>{t.name}</option>)}</select></label>
      {employeeId && <RequestFeedback loading={!balances && !balanceError} error={balanceError} onRetry={() => setReload(x => x + 1)} />}
      {balances && <><label>Total entitlement (days)<input required type="number" min="0" max="999.99" step="0.01" className={inputClass} value={days} onChange={e => setDays(e.target.value)} /></label>
        <button disabled={busy || !employeeId || !typeId} className="h-10 rounded-xl bg-black dark-primary p-2 text-white disabled:opacity-50">Save entitlement</button>
        <div className="md:col-span-2 text-sm">{balances.length ? balances.map(b => <p key={b.id}>{b.leaveType}: {b.availableDays} available, {b.usedDays} used</p>) : <p>No balances configured. Enter an opening grant.</p>}</div></>}
    </form>}
  </details></Card>
}
