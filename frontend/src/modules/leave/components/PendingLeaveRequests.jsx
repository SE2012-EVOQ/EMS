import Card from '../../../components/common/Card'

import { fmtDate } from '../../../components/common/date'

const date = value => value ? fmtDate(value) : '—'

export default function PendingLeaveRequests({ requests, user, onDecision }) {
  const manager = user?.role === 'MANAGER_ADMIN'
  if (!manager && user?.role !== 'SUPERVISOR') return null
  return <Card>
    <h2 className="text-lg font-semibold mb-1">Pending leave approvals</h2>
    <p className="text-sm text-app-muted mt-1 mb-4">
      Review your team’s requests.
      {manager && ' You can also approve your own leave.'}
    </p>
    {!requests.length ? <div className="py-8 text-center text-app-muted">No pending requests.</div> :
      <div className="space-y-3">{requests.map(request => {
        const own = request.employeeId === user.employeeId
        if (request.status !== 'PENDING' || (own && !manager)) return null
        return <div key={request.id} className="flex flex-col gap-3 rounded-xl border border-gray-200 p-4 md:flex-row md:items-center md:justify-between">
          <div>
            <div className="font-medium">{request.employeeName}</div>
            <div className="text-sm text-app-muted">{request.leaveType} · {date(request.startDate)} to {date(request.endDate)} · {request.days} day(s)</div>
            {request.reason && <div className="mt-1 text-sm text-app-muted">{request.reason}</div>}
          </div>
          <div className="flex gap-2">
            <button onClick={() => onDecision(request.id, true)} className="rounded-xl bg-black dark-primary px-4 py-2 text-sm text-white">{own ? 'Approve my leave' : 'Approve'}</button>
            {!own && <button onClick={() => onDecision(request.id, false)} className="rounded-xl border border-gray-200 px-4 py-2 text-sm">Reject</button>}
          </div>
        </div>
      })}</div>}
  </Card>
}
