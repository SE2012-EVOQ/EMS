import { useEffect, useState } from 'react'
import Modal from '../../../components/common/Modal'
import RequestFeedback from '../../../components/common/RequestFeedback'
import { getAssignmentsByEmployee, getAllAssets, returnAsset } from '../../assets/services/assetService'

export default function DeactivationReview({ employee, onClose, onConfirm }) {
  const [assignments, setAssignments] = useState(null)
  const [assets, setAssets] = useState([])
  const [loading, setLoading] = useState(true)
  const [readError, setReadError] = useState('')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)
  const [reload, setReload] = useState(0)
  useEffect(() => {
    let active = true
    setLoading(true); setReadError(''); setAssignments(null)
    Promise.all([getAssignmentsByEmployee(employee.id), getAllAssets()]).then(([history, inventory]) => {
      if (active) { setAssignments(history.filter(a => a.assignmentStatus === 'ASSIGNED')); setAssets(inventory) }
    }).catch(err => { if (active) setReadError(`Asset review unavailable: ${err.message}`) }).finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [employee.id, reload])
  async function act(action) {
    setBusy(true); setError('')
    try { await action() } catch (err) { setError(err.message || 'Action failed') } finally { setBusy(false) }
  }
  return <Modal open title={`Review assets before deactivating ${employee.fullName}`} onClose={() => { if (!busy) onClose() }}>
    <p className="mb-4 text-sm">Return assigned equipment, then deactivate the employee. Deactivation disables their login and retains history. Outstanding equipment does not automatically block deactivation.</p>
    <RequestFeedback loading={loading} error={readError} onRetry={() => setReload(x => x + 1)} />
    {error && <p role="alert" className="mb-3 text-red-700">{error}</p>}
    {assignments && (assignments.length ? <ul className="mb-4 space-y-3">{assignments.map(a => <li key={a.assignmentId} className="flex items-center justify-between gap-3">
      <span>{assets.find(asset => asset.assetId === a.assetId)?.assetName || `Asset #${a.assetId}`} · assigned {a.assignedDate}</span>
      <button disabled={busy} className="rounded-xl border px-3 py-2 disabled:opacity-50" onClick={() => act(async () => { await returnAsset(a.assignmentId); setReload(x => x + 1) })}>Return</button>
    </li>)}</ul> : <p className="mb-4">No active asset assignments.</p>)}
    {!!assignments?.length && <p role="status" className="mb-4 font-semibold text-amber-700">{assignments.length} active assignment(s) still need return.</p>}
    <div className="flex justify-end gap-3"><button disabled={busy} onClick={onClose}>Cancel</button>
      <button disabled={busy || loading} className="rounded-xl bg-red-600 px-4 py-2 text-white disabled:opacity-50" onClick={() => act(onConfirm)}>{busy ? 'Saving…' : readError ? 'Deactivate without asset review' : assignments?.length ? 'Deactivate with outstanding assets' : 'Deactivate employee'}</button></div>
  </Modal>
}
