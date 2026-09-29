import { useEffect, useMemo, useState } from 'react'
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

function formatDate(date) {
  if (!date) return '-'
  return new Date(`${date}T00:00:00`).toLocaleDateString()
}

function statusClass(status) {
  if (status === 'APPROVED') return 'bg-green-100 text-green-700'
  if (status === 'REJECTED') return 'bg-red-100 text-red-700'
  return 'bg-yellow-100 text-yellow-700'
}

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
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')

  async function loadPage() {
    setLoading(true)
    setError('')

    try {
      const currentUser = await apiRequest('/auth/me')

      setUser(currentUser)

      const [myLeave, leaveTypes] = await Promise.all([
        getMyLeave(),
        getLeaveTypes()
      ])

      setOverview(myLeave)
      setTypes(leaveTypes)

      if (currentUser.role === 'SUPERVISOR') {
        try {
          setTeamRequests(await getPendingLeaveRequests())
        } catch {
          setTeamRequests([])
        }
      }

      if (currentUser.role === 'MANAGER_ADMIN') {
        try {
          setManagerRequests(await getAllLeaveRequests())
        } catch {
          setManagerRequests([])
        }
      }
    } catch (err) {
      setError(err.message || 'Could not load leave information')
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

      setMessage('Leave request submitted successfully.')
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

  if (loading) {
    return (
      <>
        <PageHeader
          title="Leave management"
          description="Leave balances, requests and decisions."
        />
        <Card>
          <div className="p-8 text-center text-gray-500">
            Loading leave information...
          </div>
        </Card>
      </>
    )
  }

  return (
    <>
      <PageHeader
        title="Leave management"
        description="Leave balances, requests and decisions."
      />

      {error && (
        <div className="mb-4 rounded-xl bg-red-50 px-4 py-3 text-sm text-red-700">
          {error}
        </div>
      )}

      {message && (
        <div className="mb-4 rounded-xl bg-green-50 px-4 py-3 text-sm text-green-700">
          {message}
        </div>
      )}

      <div className="grid gap-4 md:grid-cols-3 mb-6">
        {overview.balances.map(balance => (
          <Card key={balance.id}>
            <div className="p-5">
              <div className="text-sm text-gray-500">
                {balance.leaveType}
              </div>
              <div className="mt-2 text-3xl font-semibold">
                {balance.availableDays}
              </div>
              <div className="text-sm text-gray-500">
                available days
              </div>
              <div className="mt-3 text-sm text-gray-500">
                Used: {balance.usedDays}
              </div>
            </div>
          </Card>
        ))}
      </div>

      {user?.role === 'EMPLOYEE' && (
        <Card>
          <div className="p-6">
            <h2 className="text-lg font-semibold mb-1">
              Submit leave request
            </h2>
            <p className="text-sm text-gray-500 mb-5">
              Request leave for approval by your supervisor.
            </p>

            <form
              onSubmit={handleSubmit}
              className="grid gap-4 md:grid-cols-2"
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

              <div className="hidden md:block" />

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
                  value={form.endDate}
                  onChange={e =>
                    setForm({ ...form, endDate: e.target.value })
                  }
                  className="rounded-xl border border-gray-200 px-3 py-2"
                />
              </label>

              <label className="md:col-span-2 flex flex-col gap-2">
                <span className="text-sm font-medium">Reason</span>
                <textarea
                  value={form.reason}
                  onChange={e =>
                    setForm({ ...form, reason: e.target.value })
                  }
                  rows="3"
                  placeholder="Optional reason"
                  className="rounded-xl border border-gray-200 px-3 py-2"
                />
              </label>

              <div className="md:col-span-2 flex items-center justify-between gap-4">
                <div className="text-sm text-gray-500">
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
                  className="rounded-xl bg-black px-5 py-2.5 text-white disabled:opacity-50"
                >
                  {submitting ? 'Submitting...' : 'Submit request'}
                </button>
              </div>
            </form>
          </div>
        </Card>
      )}

      <Card>
        <div className="p-6">
          <div className="flex items-center justify-between mb-4">
            <div>
              <h2 className="text-lg font-semibold">
                {user?.role === 'EMPLOYEE'
                  ? 'My leave history'
                  : 'Leave requests'}
              </h2>
              <p className="text-sm text-gray-500">
                Request dates, type and current decision status.
              </p>
            </div>
            <button
              onClick={loadPage}
              className="rounded-xl border border-gray-200 px-4 py-2 text-sm"
            >
              Refresh
            </button>
          </div>

          {overview.requests.length === 0 ? (
            <div className="py-10 text-center text-gray-500">
              No leave requests found.
            </div>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead>
                  <tr className="border-b text-left text-gray-500">
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
                        <span
                          className={`rounded-full px-3 py-1 text-xs font-medium ${statusClass(request.status)}`}
                        >
                          {request.status}
                        </span>
                      </td>
                      <td className="py-3">{request.reason || '-'}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      </Card>

      {user?.role === 'SUPERVISOR' && (
        <Card>
          <div className="p-6 mt-6">
            <h2 className="text-lg font-semibold">
              Pending team leave requests
            </h2>
            <p className="text-sm text-gray-500 mb-4">
              Review and decide requests from your direct reports.
            </p>

            {teamRequests.length === 0 ? (
              <div className="py-8 text-center text-gray-500">
                No pending team requests.
              </div>
            ) : (
              <div className="space-y-3">
                {teamRequests.map(request => (
                  <div
                    key={request.id}
                    className="flex flex-col gap-3 rounded-xl border border-gray-200 p-4 md:flex-row md:items-center md:justify-between"
                  >
                    <div>
                      <div className="font-medium">
                        {request.employeeName}
                      </div>
                      <div className="text-sm text-gray-500">
                        {request.leaveType} · {formatDate(request.startDate)}
                        {' '}to{' '}
                        {formatDate(request.endDate)} · {request.days} day(s)
                      </div>
                      {request.reason && (
                        <div className="mt-1 text-sm text-gray-600">
                          {request.reason}
                        </div>
                      )}
                    </div>

                    <div className="flex gap-2">
                      <button
                        onClick={() => handleDecision(request.id, true)}
                        className="rounded-xl bg-black px-4 py-2 text-sm text-white"
                      >
                        Approve
                      </button>
                      <button
                        onClick={() => handleDecision(request.id, false)}
                        className="rounded-xl border border-gray-200 px-4 py-2 text-sm"
                      >
                        Reject
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        </Card>
      )}

      {user?.role === 'MANAGER_ADMIN' && (
        <Card>
          <div className="p-6 mt-6">
            <h2 className="text-lg font-semibold">
              All leave requests
            </h2>

            {managerRequests.length === 0 ? (
              <div className="py-8 text-center text-gray-500">
                No leave requests found.
              </div>
            ) : (
              <div className="overflow-x-auto">
                <table className="w-full text-sm">
                  <thead>
                    <tr className="border-b text-left text-gray-500">
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
                          <span
                            className={`rounded-full px-3 py-1 text-xs font-medium ${statusClass(request.status)}`}
                          >
                            {request.status}
                          </span>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        </Card>
      )}
    </>
  )
}
