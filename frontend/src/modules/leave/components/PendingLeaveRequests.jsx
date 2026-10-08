import Card from '../../../components/common/Card'

const date = value => value ? new Date(`${value}T00:00:00`).toLocaleDateString() : '-'

export default function PendingLeaveRequests({ requests, user, onDecision }) {
  const manager = user?.role === 'MANAGER_ADMIN'
  if (!manager && user?.role !== 'SUPERVISOR') return null
  return <Card>
    <h2 className="text-lg font-semibold mb-1">Pending leave approvals</h2>
    <p className="text-sm text-gray-500 mt-1 mb-4">
      Review requests from your active direct reports in your current team.
      {manager && ' You can also approve your own leave.'}
    </p>
    {!requests.length ? <div className="py-8 text-center text-gray-500">No pending requests in your approval scope.</div> :
      <div className="space-y-3">{requests.map(request => {
        const own = request.employeeId === user.employeeId
        if (request.status !== 'PENDING' || (own && !manager)) return null
        return <div key={request.id} className="flex flex-col gap-3 rounded-xl border border-gray-200 p-4 md:flex-row md:items-center md:justify-between">
          <div>
            <div className="font-medium">{request.employeeName}</div>
            <div className="text-sm text-gray-500">{request.leaveType} · {date(request.startDate)} to {date(request.endDate)} · {request.days} day(s)</div>
            {request.reason && <div className="mt-1 text-sm text-gray-600">{request.reason}</div>}
          </div>
          <div className="flex gap-2">
            <button onClick={() => onDecision(request.id, true)} className="rounded-xl bg-black px-4 py-2 text-sm text-white">{own ? 'Approve my leave' : 'Approve'}</button>
            {!own && <button onClick={() => onDecision(request.id, false)} className="rounded-xl border border-gray-200 px-4 py-2 text-sm">Reject</button>}
          </div>
        </div>
      })}</div>}
  </Card>
}
