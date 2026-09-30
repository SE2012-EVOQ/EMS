import { useEffect, useState } from 'react'
import {
  Laptop,
  Plus,
  RefreshCw,
  UserPlus,
  RotateCcw,
  Package
} from 'lucide-react'

import Card from '../../../components/common/Card'
import EmptyState from '../../../components/common/EmptyState'
import PageHeader from '../../../components/common/PageHeader'
import StatusBadge from '../../../components/common/StatusBadge'

import {
  getAllAssets,
  registerAsset,
  assignAsset,
  returnAsset,
  getAllAssignments
} from '../services/assetService'

import { employeeService } from '../../employees/services/employeeService'

export default function AssetsPage() {
  const [assets, setAssets] = useState([])
  const [employees, setEmployees] = useState([])
  const [assignments, setAssignments] = useState([])

  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')

  const [showRegister, setShowRegister] = useState(false)
  const [showAssign, setShowAssign] = useState(false)

  const [assetForm, setAssetForm] = useState({
    assetName: '',
    assetType: 'Laptop',
    serialNumber: '',
    status: 'AVAILABLE'
  })

  const [assignmentForm, setAssignmentForm] = useState({
    assetId: '',
    employeeId: ''
  })

  useEffect(() => {
    loadData()
  }, [])

  async function loadData() {
    try {
      setLoading(true)
      setError('')

      const [assetData, employeeData, assignmentData] =
        await Promise.all([
          getAllAssets(),
          employeeService.getAll(),
          getAllAssignments()
        ])

      setAssets(Array.isArray(assetData) ? assetData : [])
      setEmployees(Array.isArray(employeeData) ? employeeData : [])
      setAssignments(Array.isArray(assignmentData) ? assignmentData : [])
    } catch (err) {
      setError(err.message || 'Could not load asset data')
    } finally {
      setLoading(false)
    }
  }

  async function handleRegister(event) {
    event.preventDefault()

    try {
      setError('')
      setMessage('')

      await registerAsset(assetForm)

      setAssetForm({
        assetName: '',
        assetType: 'Laptop',
        serialNumber: '',
        status: 'AVAILABLE'
      })

      setShowRegister(false)
      setMessage('Asset registered successfully.')
      await loadData()
    } catch (err) {
      setError(err.message || 'Could not register asset')
    }
  }

  async function handleAssign(event) {
    event.preventDefault()

    if (!assignmentForm.assetId || !assignmentForm.employeeId) {
      setError('Please select both an asset and an employee.')
      return
    }

    try {
      setError('')
      setMessage('')

      await assignAsset(
        assignmentForm.assetId,
        assignmentForm.employeeId
      )

      setAssignmentForm({
        assetId: '',
        employeeId: ''
      })

      setShowAssign(false)
      setMessage('Asset assigned successfully.')
      await loadData()
    } catch (err) {
      setError(err.message || 'Could not assign asset')
    }
  }

  async function handleReturn(assignmentId) {
    try {
      setError('')
      setMessage('')

      await returnAsset(assignmentId)

      setMessage('Asset returned successfully.')
      await loadData()
    } catch (err) {
      setError(err.message || 'Could not return asset')
    }
  }

  function displayStatus(status) {
    if (!status) return 'Unknown'

    return status
      .toLowerCase()
      .split('_')
      .map(word => word.charAt(0).toUpperCase() + word.slice(1))
      .join(' ')
  }

  function employeeName(employeeId) {
    const employee = employees.find(
      item => Number(item.id) === Number(employeeId)
    )

    if (!employee) return `Employee #${employeeId}`

    if (employee.fullName) return employee.fullName

    const name = `${employee.firstName || ''} ${employee.lastName || ''}`.trim()

    return name || `Employee #${employeeId}`
  }

  function assetName(assetId) {
    const asset = assets.find(
      item => Number(item.assetId) === Number(assetId)
    )

    return asset
      ? `${asset.assetName} (${asset.serialNumber})`
      : `Asset #${assetId}`
  }

  const availableAssets = assets.filter(
    asset => asset.status?.toUpperCase() === 'AVAILABLE'
  )

  const activeAssignments = assignments.filter(
    assignment =>
      assignment.assignmentStatus?.toUpperCase() === 'ASSIGNED'
  )

  return (
    <>
      <PageHeader
        title="Assets & equipment"
        description="Register, assign and track company equipment."
      />

      <div className="flex flex-wrap gap-3 mb-5">
        <button
          onClick={() => setShowRegister(!showRegister)}
          className="inline-flex items-center gap-2 rounded-xl bg-black px-4 py-2.5 text-xs font-bold text-white"
        >
          <Plus className="w-4 h-4" />
          Register Asset
        </button>

        <button
          onClick={() => setShowAssign(!showAssign)}
          className="inline-flex items-center gap-2 rounded-xl border border-app-border px-4 py-2.5 text-xs font-bold txt"
        >
          <UserPlus className="w-4 h-4" />
          Assign Asset
        </button>

        <button
          onClick={loadData}
          className="inline-flex items-center gap-2 rounded-xl border border-app-border px-4 py-2.5 text-xs font-bold txt"
        >
          <RefreshCw className="w-4 h-4" />
          Refresh
        </button>
      </div>

      {message && (
        <div className="mb-4 rounded-xl border border-green-200 bg-green-50 p-3 text-xs font-semibold text-green-700">
          {message}
        </div>
      )}

      {error && (
        <div className="mb-4 rounded-xl border border-red-200 bg-red-50 p-3 text-xs font-semibold text-red-600">
          {error}
        </div>
      )}

      {showRegister && (
        <Card>
          <h2 className="mb-4 text-sm font-bold txt">
            Register New Asset
          </h2>

          <form
            onSubmit={handleRegister}
            className="grid gap-4 md:grid-cols-2"
          >
            <input
              required
              placeholder="Asset name"
              value={assetForm.assetName}
              onChange={event =>
                setAssetForm({
                  ...assetForm,
                  assetName: event.target.value
                })
              }
              className="rounded-xl border border-app-border bg-transparent px-4 py-3 text-sm txt"
            />

            <select
              value={assetForm.assetType}
              onChange={event =>
                setAssetForm({
                  ...assetForm,
                  assetType: event.target.value
                })
              }
              className="rounded-xl border border-app-border bg-transparent px-4 py-3 text-sm txt"
            >
              <option>Laptop</option>
              <option>Monitor</option>
              <option>Desktop</option>
              <option>Mobile Phone</option>
              <option>Specialized Device</option>
              <option>Other</option>
            </select>

            <input
              required
              placeholder="Serial number"
              value={assetForm.serialNumber}
              onChange={event =>
                setAssetForm({
                  ...assetForm,
                  serialNumber: event.target.value
                })
              }
              className="rounded-xl border border-app-border bg-transparent px-4 py-3 text-sm txt"
            />

            <button
              type="submit"
              className="rounded-xl bg-black px-4 py-3 text-xs font-bold text-white"
            >
              Save Asset
            </button>
          </form>
        </Card>
      )}

      {showAssign && (
        <div className="mt-5">
          <Card>
            <h2 className="mb-4 text-sm font-bold txt">
              Assign Asset to Employee
            </h2>

            <form
              onSubmit={handleAssign}
              className="grid gap-4 md:grid-cols-3"
            >
              <select
                required
                value={assignmentForm.assetId}
                onChange={event =>
                  setAssignmentForm({
                    ...assignmentForm,
                    assetId: event.target.value
                  })
                }
                className="rounded-xl border border-app-border bg-transparent px-4 py-3 text-sm txt"
              >
                <option value="">Select available asset</option>

                {availableAssets.map(asset => (
                  <option
                    key={asset.assetId}
                    value={asset.assetId}
                  >
                    {asset.assetName} - {asset.serialNumber}
                  </option>
                ))}
              </select>

              <select
                required
                value={assignmentForm.employeeId}
                onChange={event =>
                  setAssignmentForm({
                    ...assignmentForm,
                    employeeId: event.target.value
                  })
                }
                className="rounded-xl border border-app-border bg-transparent px-4 py-3 text-sm txt"
              >
                <option value="">Select employee</option>

                {employees.map(employee => (
                  <option
                    key={employee.id}
                    value={employee.id}
                  >
                    {employee.fullName ||
                      `${employee.firstName || ''} ${employee.lastName || ''}`}
                  </option>
                ))}
              </select>

              <button
                type="submit"
                className="rounded-xl bg-black px-4 py-3 text-xs font-bold text-white"
              >
                Assign Asset
              </button>
            </form>
          </Card>
        </div>
      )}

      <div className="mt-5">
        <Card>
          <div className="mb-5 flex items-center justify-between">
            <div>
              <h2 className="text-sm font-bold txt">
                Asset Inventory
              </h2>
              <p className="mt-1 text-[11px] text-app-muted muted">
                {assets.length} registered assets
              </p>
            </div>

            <Package className="w-5 h-5 text-app-muted" />
          </div>

          {loading && (
            <p className="text-sm text-app-muted muted">
              Loading assets...
            </p>
          )}

          {!loading && assets.length === 0 && (
            <EmptyState
              icon="Laptop"
              title="No asset records available"
              description="Register your first company asset to get started."
            />
          )}

          {!loading && assets.length > 0 && (
            <div className="overflow-x-auto">
              <table className="w-full text-left">
                <thead>
                  <tr className="border-b border-app-border">
                    <th className="pb-3 text-[10px] uppercase tracking-wider text-app-muted">
                      Asset
                    </th>
                    <th className="pb-3 text-[10px] uppercase tracking-wider text-app-muted">
                      Type
                    </th>
                    <th className="pb-3 text-[10px] uppercase tracking-wider text-app-muted">
                      Serial Number
                    </th>
                    <th className="pb-3 text-[10px] uppercase tracking-wider text-app-muted">
                      Status
                    </th>
                  </tr>
                </thead>

                <tbody>
                  {assets.map(asset => (
                    <tr
                      key={asset.assetId}
                      className="border-b border-app-border/60 last:border-0"
                    >
                      <td className="py-4">
                        <div className="flex items-center gap-3">
                          <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-app-subtle subtle">
                            <Laptop className="w-4 h-4" />
                          </div>

                          <div>
                            <div className="text-xs font-bold txt">
                              {asset.assetName}
                            </div>

                            <div className="mt-1 text-[10px] text-app-muted muted">
                              Asset #{asset.assetId}
                            </div>
                          </div>
                        </div>
                      </td>

                      <td className="py-4 text-xs font-semibold txt">
                        {asset.assetType}
                      </td>

                      <td className="py-4 text-xs text-app-muted muted">
                        {asset.serialNumber}
                      </td>

                      <td className="py-4">
                        <StatusBadge
                          status={displayStatus(asset.status)}
                        />
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </Card>
      </div>

      <div className="mt-5">
        <Card>
          <h2 className="mb-1 text-sm font-bold txt">
            Active Asset Assignments
          </h2>

          <p className="mb-5 text-[11px] text-app-muted muted">
            Track equipment currently assigned to employees.
          </p>

          {activeAssignments.length === 0 ? (
            <p className="text-xs text-app-muted muted">
              No active asset assignments.
            </p>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-left">
                <thead>
                  <tr className="border-b border-app-border">
                    <th className="pb-3 text-[10px] uppercase text-app-muted">
                      Asset
                    </th>

                    <th className="pb-3 text-[10px] uppercase text-app-muted">
                      Employee
                    </th>

                    <th className="pb-3 text-[10px] uppercase text-app-muted">
                      Assigned Date
                    </th>

                    <th className="pb-3 text-[10px] uppercase text-app-muted">
                      Status
                    </th>

                    <th className="pb-3 text-[10px] uppercase text-app-muted">
                      Action
                    </th>
                  </tr>
                </thead>

                <tbody>
                  {activeAssignments.map(assignment => (
                    <tr
                      key={assignment.assignmentId}
                      className="border-b border-app-border/60 last:border-0"
                    >
                      <td className="py-4 text-xs font-semibold txt">
                        {assetName(assignment.assetId)}
                      </td>

                      <td className="py-4 text-xs txt">
                        {employeeName(assignment.employeeId)}
                      </td>

                      <td className="py-4 text-xs text-app-muted muted">
                        {assignment.assignedDate}
                      </td>

                      <td className="py-4">
                        <StatusBadge
                          status={displayStatus(
                            assignment.assignmentStatus
                          )}
                        />
                      </td>

                      <td className="py-4">
                        <button
                          onClick={() =>
                            handleReturn(assignment.assignmentId)
                          }
                          className="inline-flex items-center gap-1 rounded-lg border border-app-border px-3 py-2 text-[11px] font-bold txt"
                        >
                          <RotateCcw className="w-3 h-3" />
                          Return
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </Card>
      </div>
    </>
  )
}