import React, { useEffect, useState, useMemo } from 'react'
import {
  UserPlus,
  Search,
  RefreshCw,
  AlertCircle,
  CheckCircle2
} from 'lucide-react'

import PageHeader from '../../../components/common/PageHeader'
import Card from '../../../components/common/Card'
import EmptyState from '../../../components/common/EmptyState'
import { useAuth } from '../../../context/AuthContext'

import OrganizationManagement from '../components/OrganizationManagement'
import DeactivationReview from '../components/DeactivationReview'
import MetricCard from '../../../components/common/MetricCard'
import EmployeeTable from '../components/EmployeeTable'
import EmployeeProfileModal from '../components/EmployeeProfileModal'
import CreateEmployeeModal from '../components/CreateEmployeeModal'
import EditOfficialModal from '../components/EditOfficialModal'
import EditContactModal from '../components/EditContactModal'
import { employeeService } from '../services/employeeService'

export default function EmployeesPage() {
  const { user } = useAuth()
  const isManager = user?.role === 'MANAGER_ADMIN'

  const [supervisors, setSupervisors] = useState([])
  const [deactivation, setDeactivation] = useState(null)
  const [statusFilter, setStatusFilter] = useState('ALL')
  const [employees, setEmployees] = useState([])
  const [departments, setDepartments] = useState([])
  const [teams, setTeams] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [toast, setToast] = useState(null)

  // Filters & Search
  const [searchQuery, setSearchQuery] = useState('')
  const [selectedFilter, setSelectedFilter] = useState('All')

  // Modals state
  const [profileModalOpen, setProfileModalOpen] = useState(false)
  const [selectedEmployee, setSelectedEmployee] = useState(null)

  const [createModalOpen, setCreateModalOpen] = useState(false)

  const [editOfficialOpen, setEditOfficialOpen] = useState(false)
  const [editingOfficialEmp, setEditingOfficialEmp] = useState(null)

  const [editContactOpen, setEditContactOpen] = useState(false)
  const [editingContactEmp, setEditingContactEmp] = useState(null)

  const showToast = (message, isError = false) => {
    setToast({ message, isError })
    setTimeout(() => setToast(null), 3500)
  }

  const loadData = async () => {
    setLoading(true)
    setError(null)
    try {
      const [empList, deptList, teamList, candidates] = await Promise.all([
        employeeService.getAll(),
        isManager ? employeeService.getDepartments() : Promise.resolve(null),
        isManager ? employeeService.getTeams() : Promise.resolve(null),
        isManager ? employeeService.getSupervisorCandidates() : Promise.resolve([])
      ])
      setEmployees(empList)
      setDepartments(deptList || [...new Map(empList.filter(e => e.department).map(e => [e.department.id, e.department])).values()])
      setTeams(teamList || [...new Map(empList.filter(e => e.team).map(e => [e.team.id, e.team])).values()])
      setSupervisors(candidates)
    } catch (err) {
      setError(err.message || 'Failed to load employee directory')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadData()
  }, [])

  const filterPills = useMemo(() => ['All', ...new Set([...departments.map(d => d.name), ...teams.map(t => t.name)])], [departments, teams])

  // Filter within the backend-authorized directory.
  const filteredEmployees = useMemo(() => {
    return employees.filter(emp => {
      // 1. Search keyword filter
      if (searchQuery.trim()) {
        const query = searchQuery.toLowerCase().trim()
        const code = `e${String(emp.id).padStart(3, '0')}`
        const matchName = emp.fullName?.toLowerCase().includes(query)
        const matchEmail = emp.email?.toLowerCase().includes(query)
        const matchTitle = emp.jobTitle?.toLowerCase().includes(query)
        const matchCode = code.includes(query)
        if (!matchName && !matchEmail && !matchTitle && !matchCode) return false
      }

      if (statusFilter !== 'ALL' && emp.status !== statusFilter) return false
      if (selectedFilter !== 'All' && emp.department?.name !== selectedFilter && emp.team?.name !== selectedFilter) return false

      return true
    })
  }, [employees, searchQuery, selectedFilter, statusFilter])

  // Handlers for Employee Actions
  const handleCreateEmployee = async (payload) => {
    await employeeService.create(payload)
    showToast('Employee added.')
    loadData()
  }

  const handleUpdateOfficial = async (id, payload) => {
    if (payload.status === 'INACTIVE' && editingOfficialEmp.status !== 'INACTIVE') {
      await new Promise((resolve, reject) => setDeactivation({ employee: editingOfficialEmp,
        action: () => employeeService.updateOfficial(id, payload), resolve, reject }))
    } else await employeeService.updateOfficial(id, payload)
    showToast('Employee record updated.')
    loadData()
  }

  const handleUpdateContact = async (id, payload) => {
    await employeeService.updateContact(id, payload)
    showToast('Contact details updated.')
    loadData()
  }

  const handleToggleStatus = async (employee) => {
    if (employee.status === 'ACTIVE') {
      setDeactivation({ employee, action: () => employeeService.changeStatus(employee.id, 'INACTIVE') })
      return
    }
    try {
      await employeeService.changeStatus(employee.id, 'ACTIVE')
      showToast('Employee activated'); await loadData()
    } catch (err) { showToast(err.message || 'Status change failed', true) }
  }

  return (
    <div className="space-y-4">
      {/* Toast Notification */}
      {toast && (
        <div className="fixed top-6 right-6 z-[100] max-w-sm pointer-events-auto transition-all animate-in fade-in slide-in-from-top-2">
          <div className={`p-4 rounded-2xl shadow-xl border flex items-center gap-3 ${
            toast.isError
              ? 'bg-app-pink-bg text-app-pink border-app-pink/20'
              : 'bg-white txt border-app-border'
          }`}>
            {!toast.isError && <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0" />}
            <span className="text-xs font-bold">{toast.message}</span>
          </div>
        </div>
      )}

      {/* Directory Section Header & Add Employee Action */}
      <PageHeader primary title={isManager ? 'Employees' : 'My team'} actions={isManager && <button
        onClick={() => setCreateModalOpen(true)} disabled={loading || Boolean(error)}
        className="bg-[#1A1D1F] dark-primary hover:bg-black text-white px-4 py-2.5 rounded-full text-xs font-bold inline-flex items-center gap-2">
        <UserPlus className="w-4 h-4" />Add employee
      </button>} />

      {!loading && !error && <>
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
          <MetricCard label="Employees" value={employees.length} icon="Users" />
          <MetricCard label="Active employees" value={employees.filter(e => e.status === 'ACTIVE').length} icon="UserCheck" positive />
          <MetricCard label="Departments" value={departments.length} icon="Building2" />
          <MetricCard label="Teams" value={teams.length} icon="FolderGit2" />
        </div>
        {isManager && <OrganizationManagement onSaved={loadData} />}
      </>}

      {/* Filter Pills & Search Bar Toolbar */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-3">
        <div className="flex gap-2 min-w-0 flex-1 overflow-x-auto pb-1">
          {filterPills.map(filter => (
            <button
              key={filter}
              aria-pressed={selectedFilter === filter}
              onClick={() => setSelectedFilter(filter)}
              className={`px-3.5 py-2 rounded-full text-xs font-bold shrink-0 transition ${
                selectedFilter === filter
                  ? 'bg-[#1A1D1F] dark-primary text-white shadow-sm'
                  : 'surface bg-white border border-gray-200 txt hover:bg-gray-50'
              }`}
            >
              {filter}
            </button>
          ))}
        </div>

        <div className="flex items-center gap-2">
          <select aria-label="Employee status filter" value={statusFilter} onChange={e => setStatusFilter(e.target.value)} className="rounded-full surface bg-white border border-app-border px-3 py-2 text-xs">
            <option value="ALL">All statuses</option><option value="ACTIVE">Active</option><option value="INACTIVE">Inactive</option><option value="SUSPENDED">Suspended</option><option value="ON_LEAVE">On Leave</option>
          </select>
          <div className="relative flex-1 sm:w-64">
            <Search className="w-3.5 h-3.5 text-app-muted absolute left-3.5 top-1/2 -translate-y-1/2" />
            <input
              type="search"
              aria-label="Search employees"
              value={searchQuery}
              onChange={e => setSearchQuery(e.target.value)}
              placeholder="Search employees..."
              className="w-full pl-9 pr-3 py-2 rounded-full bg-white surface border border-gray-200 text-xs font-medium outline-none focus:border-gray-400 placeholder:text-app-muted"
            />
          </div>

          <button
            onClick={loadData}
            title="Refresh directory"
            aria-label="Refresh directory"
            disabled={loading}
            className="w-10 h-10 rounded-full surface bg-white border border-gray-200 flex items-center justify-center text-app-muted hover:txt transition shrink-0"
          >
            <RefreshCw className={`w-3.5 h-3.5 ${loading ? 'animate-spin' : ''}`} />
          </button>
        </div>
      </div>

      {/* Main Table Card */}
      <Card>
        {loading ? (
          <div className="py-16 flex flex-col items-center justify-center text-app-muted gap-3">
            <RefreshCw className="w-6 h-6 animate-spin text-[#1A1D1F]" />
            <span className="text-xs font-bold">Loading employees...</span>
          </div>
        ) : error ? (
          <div className="py-12 flex flex-col items-center justify-center text-app-pink gap-2">
            <AlertCircle className="w-6 h-6" />
            <span className="text-xs font-bold">{error}</span>
            <button onClick={loadData} className="text-xs underline font-bold mt-1">Try again</button>
          </div>
        ) : filteredEmployees.length === 0 ? (
          <EmptyState
            icon="Users"
            title={searchQuery || selectedFilter !== 'All' || statusFilter !== 'ALL' ? 'No matching employees' : 'No employees yet'}
            description={
              searchQuery || selectedFilter !== 'All' || statusFilter !== 'ALL'
                ? 'Try another search or filter.'
                : 'Add an employee to get started.'
            }
          />
        ) : (
          <EmployeeTable
            employees={filteredEmployees}
            currentUser={user}
            onViewProfile={(emp) => {
              setSelectedEmployee(emp)
              setProfileModalOpen(true)
            }}
          />
        )}
      </Card>

      {/* Modals */}
      <CreateEmployeeModal
        key={createModalOpen ? 'create-open' : 'create-closed'}
        open={createModalOpen}
        onClose={() => setCreateModalOpen(false)}
        onSubmit={handleCreateEmployee}
        departments={departments}
        teams={teams}
        supervisors={supervisors}
      />

      <EditOfficialModal
        open={editOfficialOpen}
        employee={editingOfficialEmp}
        onClose={() => {
          setEditOfficialOpen(false)
          setEditingOfficialEmp(null)
        }}
        onSubmit={handleUpdateOfficial}
        departments={departments}
        teams={teams}
        supervisors={supervisors}
      />

      <EditContactModal
        open={editContactOpen}
        employee={editingContactEmp}
        onClose={() => {
          setEditContactOpen(false)
          setEditingContactEmp(null)
        }}
        onSubmit={handleUpdateContact}
      />

      <EmployeeProfileModal
        open={profileModalOpen}
        employee={selectedEmployee}
        currentUser={user}
        onClose={() => {
          setProfileModalOpen(false)
          setSelectedEmployee(null)
        }}
        onEditOfficial={(emp) => {
          setEditingOfficialEmp(emp)
          setEditOfficialOpen(true)
        }}
        onEditContact={(emp) => {
          setEditingContactEmp(emp)
          setEditContactOpen(true)
        }}
        onToggleStatus={handleToggleStatus}
      />
      {deactivation && <DeactivationReview key={deactivation.employee.id} employee={deactivation.employee}
        onClose={() => { deactivation.reject?.(new Error('Deactivation cancelled')); setDeactivation(null) }}
        onConfirm={async () => { await deactivation.action(); deactivation.resolve?.(); setDeactivation(null); showToast('Employee deactivated'); await loadData() }} />}
    </div>
  )
}
