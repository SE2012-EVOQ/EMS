import { useEffect, useMemo, useState } from 'react'
import LeaveSetup from '../components/LeaveSetup'
import PendingLeaveRequests from '../components/PendingLeaveRequests'
import RequestFeedback from '../../../components/common/RequestFeedback'
import StatusBadge from '../../../components/common/StatusBadge'
import { fmtDate } from '../../../components/common/date'
import Card from '../../../components/common/Card'
import PageHeader from '../../../components/common/PageHeader'
import { apiRequest } from '../../../services/api'
import {
  approveLeaveRequest,
  getAllLeaveRequests,
  getLeaveTypes,
  getMyLeave,
  getPendingLeaveRequests,
  rejectLeaveRequest,
  submitLeaveRequest
} from '../services/leaveService'

const formatDate = value => value ? fmtDate(value) : '—'

export default function LeavePage() {
  const [user, setUser] = useState(null)
  const [overview, setOverview] = useState({
    balances: [],
    requests: []
  })
  const [types, setTypes] = useState([])
  const [teamRequests, setTeamRequests] = useState([])
  const [managerRequests, setManagerRequests] = useState([])
  const [form, setForm] = useState({
    leaveTypeId: '',
    startDate: '',
    endDate: '',
    reason: ''
  })
  const [loading, setLoading] = useState(true)
  const [submitting, setSubmitting] = useState(false)
  const [readError, setReadError] = useState('')
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')

  async function loadPage() {
    setLoading(true)
    setReadError('')

    try {
      const currentUser = await apiRequest('/auth/me')

      setUser(currentUser)

      const [myLeave, leaveTypes] = await Promise.all([
        getMyLeave(),
        getLeaveTypes()
      ])

      setOverview(myLeave)
      setTypes(leaveTypes)

      if (['SUPERVISOR', 'MANAGER_ADMIN'].includes(currentUser.role)) setTeamRequests(await getPendingLeaveRequests())
      if (currentUser.role === 'MANAGER_ADMIN') setManagerRequests(await getAllLeaveRequests())
    } catch (err) {
      setReadError(`Leave information unavailable: ${err.message || 'Read failed'}`)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadPage()
  }, [])

  const selectedBalance = useMemo(
    () => overview.balances.find(
      balance => String(balance.leaveTypeId) === String(form.leaveTypeId)
    ),
    [overview.balances, form.leaveTypeId]
  )

  const requestedDays = useMemo(() => {
    if (!form.startDate || !form.endDate) return 0

    const start = new Date(`${form.startDate}T00:00:00`)
    const end = new Date(`${form.endDate}T00:00:00`)

    const difference = Math.floor(
      (end - start) / (1000 * 60 * 60 * 24)
    )

    return difference >= 0 ? difference + 1 : 0
  }, [form.startDate, form.endDate])

  async function handleSubmit(event) {
    event.preventDefault()
    setError('')
    setMessage('')

    if (!form.leaveTypeId || !form.startDate || !form.endDate) {
      setError('Please select a leave type and both dates.')
      return
    }

    if (requestedDays <= 0) {
      setError('End date must be on or after the start date.')
      return
    }

    if (
      selectedBalance &&
      Number(selectedBalance.availableDays) < requestedDays
    ) {
      setError('The selected leave balance is not sufficient.')
      return
    }

    setSubmitting(true)

    try {
      await submitLeaveRequest({
        leaveTypeId: Number(form.leaveTypeId),
        startDate: form.startDate,
        endDate: form.endDate,
        reason: form.reason || null
      })

      setMessage('Leave request submitted.')
      setForm({
        leaveTypeId: '',
        startDate: '',
        endDate: '',
        reason: ''
      })

      await loadPage()
    } catch (err) {
      setError(err.message || 'Could not submit leave request')
    } finally {
      setSubmitting(false)
    }
  }

  async function handleDecision(id, approve) {
    setError('')
    setMessage('')

    try {
      if (approve) {
        await approveLeaveRequest(id)
        setMessage('Leave request approved.')
      } else {
        await rejectLeaveRequest(id)
        setMessage('Leave request rejected.')
      }

      await loadPage()
    } catch (err) {
      setError(err.message || 'Could not update leave request')
    }
  }

  if (readError) return <><PageHeader primary title="Leave" /><RequestFeedback error={readError} loading={loading} onRetry={loadPage} /></>

  if (loading) {
    return (
      <>
        <PageHeader
          primary title="Leave"
        />
        <Card>
          <div className="p-8 text-center text-app-muted">
            Loading leave information...
          </div>
        </Card>
      </>
    )
  }

  return (
    <>
      <PageHeader
        primary title="Leave"
      />

      <div className="space-y-5">
        {user?.role === 'MANAGER_ADMIN' && <LeaveSetup types={types} onSaved={async () => {
          try { const [updatedTypes, mine] = await Promise.all([getLeaveTypes(), getMyLeave()]); setTypes(updatedTypes); setOverview(mine) }
          catch (err) { setReadError(`Leave information unavailable: ${err.message}`) }
        }} />}
        {!overview.balances.length && <p className="text-sm text-app-muted">No leave balances configured. Ask your Manager/Admin to set your entitlement.</p>}
        {error && (
          <div role="alert" className="rounded-xl bg-app-pink-bg px-4 py-3 text-sm text-app-pink">
            {error}
          </div>
        )}

        {message && (
          <div role="status" className="rounded-xl bg-app-green-bg px-4 py-3 text-sm text-app-green">
            {message}
          </div>
        )}

        <div className="grid grid-cols-2 gap-3 lg:grid-cols-3">
          {overview.balances.map(balance => (
            <Card key={balance.id}>
              <div className="text-sm text-app-muted">
                {balance.leaveType}
              </div>
              <div className="mt-2 text-3xl font-extrabold tabular-nums">
                {balance.availableDays}
              </div>
              <div className="text-sm text-app-muted">
                available days
              </div>
              <div className="mt-2 text-xs text-app-muted">
                Used: {balance.usedDays}
              </div>
            </Card>
          ))}
        </div>

        {user && (
          <Card>
            <h2 className="text-lg font-semibold mb-1">
              Request leave
            </h2>
            <p className="text-sm text-app-muted mt-1 mb-4">
              {user?.role === 'MANAGER_ADMIN' ? 'You can approve your request below.' : 'Your supervisor or manager will review your request.'}
            </p>

            <form
              onSubmit={handleSubmit}
              className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3"
            >
              <label className="flex flex-col gap-2">
                <span className="text-sm font-medium">Leave type</span>
                <select
                  value={form.leaveTypeId}
                  onChange={e =>
                    setForm({ ...form, leaveTypeId: e.target.value })
                  }
                  className="rounded-xl border border-gray-200 px-3 py-2"
                >
                  <option value="">Select leave type</option>
                  {types.map(type => (
                    <option key={type.id} value={type.id}>
                      {type.name}
                    </option>
                  ))}
                </select>
              </label>


              <label className="flex flex-col gap-2">
                <span className="text-sm font-medium">Start date</span>
                <input
                  type="date"
                  value={form.startDate}
                  onChange={e =>
                    setForm({ ...form, startDate: e.target.value })
                  }
                  className="rounded-xl border border-gray-200 px-3 py-2"
                />
              </label>

              <label className="flex flex-col gap-2">
                <span className="text-sm font-medium">End date</span>
                <input
                  type="date"
                  min={form.startDate || undefined}
                  value={form.endDate}
                  onChange={e =>
                    setForm({ ...form, endDate: e.target.value })
                  }
                  className="rounded-xl border border-gray-200 px-3 py-2"
                />
              </label>

              <label className="sm:col-span-2 lg:col-span-3 flex flex-col gap-2">
                <span className="text-sm font-medium">Reason</span>
                <textarea
                  value={form.reason}
                  onChange={e =>
                    setForm({ ...form, reason: e.target.value })
                  }
                  rows="2"
                  placeholder="Optional reason"
                  className="rounded-xl border border-gray-200 px-3 py-2"
                />
              </label>

              <div className="sm:col-span-2 lg:col-span-3 flex flex-wrap items-center justify-between gap-3">
                <div className="text-sm text-app-muted">
                  Requested days: <strong>{requestedDays}</strong>
                  {selectedBalance && (
                    <>
                      {' '}
                      / Available: <strong>{selectedBalance.availableDays}</strong>
                    </>
                  )}
                </div>

                <button
                  type="submit"
                  disabled={submitting}
                  className="rounded-xl bg-black dark-primary px-5 py-2.5 text-white disabled:opacity-50"
                >
                  {submitting ? 'Submitting...' : 'Submit request'}
                </button>
              </div>
            </form>
          </Card>
        )}

        <Card>
          <div className="flex items-center justify-between mb-4">
            <div>
              <h2 className="text-lg font-semibold">
                My leave history
              </h2>
            </div>
            <button
              onClick={loadPage}
              className="rounded-xl border border-gray-200 px-4 py-2 text-sm"
            >
              Refresh
            </button>
          </div>

          {overview.requests.length === 0 ? (
            <div className="py-10 text-center text-app-muted">
              No leave requests found.
            </div>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead>
                  <tr className="border-b text-left text-app-muted">
                    <th className="py-3 pr-4">Type</th>
                    <th className="py-3 pr-4">Start</th>
                    <th className="py-3 pr-4">End</th>
                    <th className="py-3 pr-4">Days</th>
                    <th className="py-3 pr-4">Status</th>
                    <th className="py-3">Reason</th>
                  </tr>
                </thead>
                <tbody>
                  {overview.requests.map(request => (
                    <tr key={request.id} className="border-b last:border-0">
                      <td className="py-3 pr-4">{request.leaveType}</td>
                      <td className="py-3 pr-4">
                        {formatDate(request.startDate)}
                      </td>
                      <td className="py-3 pr-4">
                        {formatDate(request.endDate)}
                      </td>
                      <td className="py-3 pr-4">{request.days}</td>
                      <td className="py-3 pr-4">
                        <StatusBadge status={request.status} />
                      </td>
                      <td className="py-3">{request.reason || '-'}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </Card>

        {['SUPERVISOR', 'MANAGER_ADMIN'].includes(user?.role) && (
          <PendingLeaveRequests requests={teamRequests} user={user} onDecision={handleDecision} />
        )}

        {user?.role === 'MANAGER_ADMIN' && (
          <Card>
            <h2 className="text-lg font-semibold mb-4">
              All leave requests
            </h2>

            {managerRequests.length === 0 ? (
              <div className="py-8 text-center text-app-muted">
                No leave requests found.
              </div>
            ) : (
              <div className="overflow-x-auto">
                <table className="w-full text-sm">
                  <thead>
                    <tr className="border-b text-left text-app-muted">
                      <th className="py-3 pr-4">Employee</th>
                      <th className="py-3 pr-4">Type</th>
                      <th className="py-3 pr-4">Dates</th>
                      <th className="py-3">Status</th>
                    </tr>
                  </thead>
                  <tbody>
                    {managerRequests.map(request => (
                      <tr key={request.id} className="border-b last:border-0">
                        <td className="py-3 pr-4">
                          {request.employeeName}
                        </td>
                        <td className="py-3 pr-4">
                          {request.leaveType}
                        </td>
                        <td className="py-3 pr-4">
                          {formatDate(request.startDate)}
                          {' '}–{' '}
                          {formatDate(request.endDate)}
                        </td>
                        <td className="py-3">
                          <StatusBadge status={request.status} />
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </Card>
        )}
      </div>
    </>
  )
}
