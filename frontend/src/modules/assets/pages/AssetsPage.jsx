import { useEffect, useState } from 'react'
import { useAuth } from '../../../context/AuthContext'
import Card from '../../../components/common/Card'
import PageHeader from '../../../components/common/PageHeader'
import RequestFeedback from '../../../components/common/RequestFeedback'
import Modal from '../../../components/common/Modal'
import { employeeService } from '../../employees/services/employeeService'
import { getAllAssets, registerAsset, updateAsset, assignAsset, returnAsset, getAllAssignments, getAssignmentsByEmployee, getAssetHistory } from '../services/assetService'

const inputClass = 'block w-full rounded-xl border border-app-border bg-transparent p-2'
const buttonClass = 'rounded-xl border border-app-border px-4 py-2 text-sm font-semibold disabled:opacity-50'
const blankAsset = { assetName: '', assetType: 'Laptop', serialNumber: '', status: 'AVAILABLE' }

export function AssignmentTable({ rows, assets, employees, onReturn, busy }) {
  if (!rows.length) return <p className="text-sm text-app-muted">No assignments in this view.</p>
  return <div className="overflow-x-auto"><table className="w-full text-left text-sm"><thead><tr>{['Asset', 'Employee', 'Assigned', 'Returned', 'Status', ...(onReturn ? ['Action'] : [])].map(c => <th className="p-2" key={c}>{c}</th>)}</tr></thead>
    <tbody>{rows.map(a => <tr className="border-t border-app-border" key={a.assignmentId}>
      <td className="p-2">{a.assetName || assets.find(asset => asset.assetId === a.assetId)?.assetName || `Asset #${a.assetId}`}</td>
      <td className="p-2">{employees.find(e => e.id === a.employeeId)?.fullName || `Employee #${a.employeeId}`}</td>
      <td className="p-2">{a.assignedDate}</td><td className="p-2">{a.returnedDate || '—'}</td><td className="p-2">{a.assignmentStatus}</td>
      {onReturn && <td className="p-2">{a.assignmentStatus === 'ASSIGNED' && <button disabled={busy} className={buttonClass} onClick={() => onReturn(a.assignmentId)}>Return</button>}</td>}
    </tr>)}</tbody></table></div>
}

export default function AssetsPage() {
  const { user } = useAuth()
  const manager = user?.role === 'MANAGER_ADMIN'
  const [data, setData] = useState(null)
  const [readError, setReadError] = useState('')
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')
  const [busy, setBusy] = useState(false)
  const [reload, setReload] = useState(0)
  const [selectedEmployee, setSelectedEmployee] = useState('')
  const [assetId, setAssetId] = useState('')
  const [employeeId, setEmployeeId] = useState('')
  const [editor, setEditor] = useState(null)
  const [assetForm, setAssetForm] = useState(blankAsset)
  const [historyAsset, setHistoryAsset] = useState('')
  const [history, setHistory] = useState(null)
  const [historyError, setHistoryError] = useState('')
  const [historyReload, setHistoryReload] = useState(0)
  useEffect(() => {
    let active = true; setLoading(true); setReadError(''); setData(null)
    Promise.all([
      getAllAssets(),
      manager ? employeeService.getAll() : employeeService.getMe().then(me => [me]),
      manager ? selectedEmployee ? getAssignmentsByEmployee(selectedEmployee) : getAllAssignments() : getAssignmentsByEmployee(user.employeeId)
    ]).then(([assets, employees, assignments]) => { if (active) setData({ assets, employees, assignments }) })
      .catch(err => { if (active) setReadError(`Assets unavailable: ${err.message}`) }).finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [manager, user.employeeId, selectedEmployee, reload])
  useEffect(() => {
    let active = true; setHistory(null); setHistoryError('')
    if (historyAsset && manager) getAssetHistory(historyAsset).then(rows => { if (active) setHistory(rows) }).catch(err => { if (active) setHistoryError(`Asset history unavailable: ${err.message}`) })
    return () => { active = false }
  }, [historyAsset, manager, historyReload, reload])
  async function act(action, success) {
    setBusy(true); setError(''); setMessage('')
    try { await action(); setMessage(success); setReload(x => x + 1) }
    catch (err) { setError(err.message || 'Asset action failed') } finally { setBusy(false) }
  }
  function edit(asset) { setAssetForm(asset ? { ...asset } : { ...blankAsset }); setEditor(asset?.assetId || 'new'); setError('') }
  const doReturn = id => act(() => returnAsset(id), 'Asset returned.')
  return <>
    <PageHeader title={manager ? 'Assets & equipment' : 'My Assets'} description={manager ? 'Register, assign, return and review equipment history.' : 'Equipment assigned to you and your assignment history.'} actions={<button disabled={busy || loading} className={buttonClass} onClick={() => setReload(x => x + 1)}>Refresh</button>} />
    <RequestFeedback loading={loading} error={readError} loadingText="Loading assets…" onRetry={() => setReload(x => x + 1)} />
    {error && <p role="alert" className="mb-4 text-red-700">{error}</p>}{message && <p role="status" className="mb-4 text-green-700">{message}</p>}
    {data && !loading && !readError && <>
      {manager && <Card className="mb-5"><button disabled={busy} className={buttonClass} onClick={() => edit(null)}>Register asset</button>
        <form className="mt-4 grid gap-3 md:grid-cols-3" onSubmit={e => { e.preventDefault(); act(async () => { await assignAsset(assetId, employeeId); setAssetId(''); setEmployeeId('') }, 'Asset assigned.') }}>
          <label>Available asset<select required className={inputClass} value={assetId} onChange={e => setAssetId(e.target.value)}><option value="">Select asset</option>{data.assets.filter(a => a.status === 'AVAILABLE').map(a => <option key={a.assetId} value={a.assetId}>{a.assetName} · {a.serialNumber}</option>)}</select></label>
          <label>Active employee<select required className={inputClass} value={employeeId} onChange={e => setEmployeeId(e.target.value)}><option value="">Select employee</option>{data.employees.filter(e => e.status === 'ACTIVE').map(e => <option key={e.id} value={e.id}>{e.fullName}</option>)}</select></label>
          <button disabled={busy} className={buttonClass}>Assign asset</button>
        </form></Card>}
      <Card className="mb-5"><h2 className="mb-4 font-bold">{manager ? 'Asset inventory' : 'Currently assigned to me'}</h2>
        {!data.assets.length ? <p>No assets in this view.</p> : <div className="overflow-x-auto"><table className="w-full text-left text-sm"><thead><tr>{['Name', 'Type', 'Serial number', 'Status', ...(manager ? ['Actions'] : [])].map(c => <th className="p-2" key={c}>{c}</th>)}</tr></thead><tbody>
          {data.assets.map(a => <tr className="border-t border-app-border" key={a.assetId}><td className="p-2">{a.assetName}</td><td className="p-2">{a.assetType}</td><td className="p-2">{a.serialNumber}</td><td className="p-2">{a.status}</td>
            {manager && <td className="p-2"><button disabled={busy} className={buttonClass} onClick={() => edit(a)}>Edit / status</button><button className={`${buttonClass} ml-2`} onClick={() => setHistoryAsset(String(a.assetId))}>History</button></td>}</tr>)}
        </tbody></table></div>}
      </Card>
      <Card className="mb-5"><h2 className="mb-4 font-bold">{manager ? 'Employee assignments & history' : 'My assignment history'}</h2>
        {manager && <label className="mb-4 block">Employee<select className={inputClass} value={selectedEmployee} disabled={busy} onChange={e => setSelectedEmployee(e.target.value)}><option value="">All employees</option>{data.employees.map(e => <option key={e.id} value={e.id}>{e.fullName} · {e.status}</option>)}</select></label>}
        <AssignmentTable rows={data.assignments} assets={data.assets} employees={data.employees} onReturn={manager ? doReturn : null} busy={busy} />
      </Card>
      {manager && historyAsset && <Card><div className="mb-4 flex justify-between"><h2 className="font-bold">History for {data.assets.find(a => String(a.assetId) === historyAsset)?.assetName || `Asset #${historyAsset}`}</h2><button onClick={() => setHistoryAsset('')}>Close history</button></div>
        <RequestFeedback loading={!history && !historyError} error={historyError} onRetry={() => setHistoryReload(x => x + 1)} />
        {history && <AssignmentTable rows={history} assets={data.assets} employees={data.employees} onReturn={doReturn} busy={busy} />}
      </Card>}
    </>}
    {manager && editor && <Modal open title={editor === 'new' ? 'Register asset' : 'Edit asset / status'} onClose={() => { if (!busy) setEditor(null) }}>
      {error && <p role="alert" className="mb-3 text-red-700">{error}</p>}
      <form className="space-y-4" onSubmit={e => { e.preventDefault(); act(async () => { await (editor === 'new' ? registerAsset(assetForm) : updateAsset(editor, assetForm)); setEditor(null) }, 'Asset saved.') }}>
        {[['assetName', 'Name', 150], ['assetType', 'Type', 100], ['serialNumber', 'Serial number', 150]].map(([field, label, max]) => <label className="block" key={field}>{label}<input required maxLength={max} className={inputClass} value={assetForm[field]} onChange={e => setAssetForm({ ...assetForm, [field]: e.target.value })} /></label>)}
        <label className="block">Status<input required maxLength={30} disabled={assetForm.status === 'ASSIGNED'} className={inputClass} value={assetForm.status} onChange={e => setAssetForm({ ...assetForm, status: e.target.value })} /></label>
        <p className="text-sm text-app-muted">AVAILABLE allows assignment. Other unassigned statuses can describe the equipment's condition. ASSIGNED is set through assignment.</p>
        {assetForm.status === 'ASSIGNED' && <p className="text-sm text-app-muted">Return the active assignment before changing status.</p>}
        <button disabled={busy} className={buttonClass}>{busy ? 'Saving…' : 'Save asset'}</button>
      </form>
    </Modal>}
  </>
}
