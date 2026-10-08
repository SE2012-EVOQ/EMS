import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { ArrowUpRight, CalendarDays, Laptop } from 'lucide-react'
import { useAuth } from '../../context/AuthContext'
import Card from '../../components/common/Card'
import PageHeader from '../../components/common/PageHeader'
import RequestFeedback from '../../components/common/RequestFeedback'
import { requestErrorMessage } from '../../components/common/requestError'
import TodayAttendanceActions from '../attendance/components/TodayAttendanceActions'
import { getMyLeave } from '../leave/services/leaveService'
import { getAssignmentsByEmployee } from '../assets/services/assetService'

export function PersonalOverview({ leave, assets, leaveError, assetsError, onRetry }) {
  const assigned = assets?.filter(asset => asset.assignmentStatus === 'ASSIGNED') || []
  const pending = leave?.requests.filter(request => request.status === 'PENDING').length || 0
  const linkClass = 'mt-5 flex items-center justify-between border-t border-app-border pt-3 text-xs font-bold text-app-muted hover:text-app-text'
  return <div className="grid gap-4 md:grid-cols-2">
    <Card className="flex min-w-0 flex-col">
      <div className="mb-5 flex items-center justify-between gap-3"><h2 className="flex items-center gap-2 text-sm font-bold"><CalendarDays className="h-4 w-4 text-app-muted" />My leave</h2>{pending > 0 && <span className="rounded-full bg-app-amber-bg px-2.5 py-1 text-[10px] font-bold text-app-amber">{pending} pending</span>}</div>
      <div className="flex-1"><RequestFeedback loading={!leave && !leaveError} error={leaveError} onRetry={onRetry} />
        {leave && !leaveError && (leave.balances.length ? <dl className="grid grid-cols-2 gap-4 sm:grid-cols-3">{leave.balances.map(balance => <div key={balance.leaveTypeId} className="min-w-0"><dt className="text-xs text-app-muted">{balance.leaveType}</dt><dd className="mt-2"><span className="text-3xl font-extrabold tabular-nums">{balance.availableDays}</span><span className="mt-1 block text-[10px] text-app-muted">days available</span></dd></div>)}</dl> : <p className="text-xs text-app-muted">No leave balances assigned yet.</p>)}
      </div>
      <Link to="/leave" className={linkClass}>Request or view leave<ArrowUpRight className="h-4 w-4" /></Link>
    </Card>
    <Card className="flex min-w-0 flex-col">
      <h2 className="mb-5 flex items-center gap-2 text-sm font-bold"><Laptop className="h-4 w-4 text-app-muted" />My assets</h2>
      <div className="flex-1"><RequestFeedback loading={!assets && !assetsError} error={assetsError} onRetry={onRetry} />
        {assets && !assetsError && <><p className="text-3xl font-extrabold tabular-nums">{assigned.length}</p><p className="mt-1 text-xs text-app-muted">Currently assigned</p>{assigned.length > 0 && <ul className="mt-4 space-y-2 text-xs font-medium">{assigned.slice(0, 3).map(asset => <li key={asset.assignmentId} className="truncate">{asset.assetName || `Asset #${asset.assetId}`}</li>)}</ul>}</>}
      </div>
      <Link to="/assets" className={linkClass}>View my assets<ArrowUpRight className="h-4 w-4" /></Link>
    </Card>
  </div>
}

export default function PersonalDashboard() {
  const { user } = useAuth()
  const [leave, setLeave] = useState(null)
  const [assets, setAssets] = useState(null)
  const [leaveError, setLeaveError] = useState('')
  const [assetsError, setAssetsError] = useState('')
  const [reload, setReload] = useState(0)

  useEffect(() => {
    let active = true
    setLeave(null); setAssets(null); setLeaveError(''); setAssetsError('')
    getMyLeave().then(data => { if (active) setLeave(data) })
      .catch(err => { if (active) setLeaveError(requestErrorMessage(err, 'Leave balances')) })
    getAssignmentsByEmployee(user.employeeId).then(data => { if (active) setAssets(data) })
      .catch(err => { if (active) setAssetsError(requestErrorMessage(err, 'Assigned assets')) })
    return () => { active = false }
  }, [user.employeeId, reload])

  const loading = (!leave && !leaveError) || (!assets && !assetsError)
  return <>
    <PageHeader primary title="Dashboard" actions={<button type="button" disabled={loading} onClick={() => setReload(value => value + 1)} className="rounded-xl border border-app-border px-4 py-2 text-xs font-bold disabled:opacity-50">Refresh</button>} />
    <TodayAttendanceActions />
    <PersonalOverview leave={leave} assets={assets} leaveError={leaveError} assetsError={assetsError} onRetry={() => setReload(value => value + 1)} />
  </>
}
