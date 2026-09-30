import React, { useEffect, useState, useMemo } from 'react'
import {
  Users,
  UserCheck,
  Building2,
  FolderGit2,
  UserPlus,
  Search,
  Filter,
  RefreshCw,
  AlertCircle
} from 'lucide-react'

import Card from '../../../components/common/Card'
import EmptyState from '../../../components/common/EmptyState'
import MetricCard from '../../../components/common/MetricCard'
import PageHeader from '../../../components/common/PageHeader'
import FilterPills from '../../../components/common/FilterPills'
import { useAuth } from '../../../context/AuthContext'

import EmployeeTable from '../components/EmployeeTable'
import EmployeeProfileModal from '../components/EmployeeProfileModal'
import CreateEmployeeModal from '../components/CreateEmployeeModal'
import EditOfficialModal from '../components/EditOfficialModal'
import EditContactModal from '../components/EditContactModal'
import { employeeService } from '../services/employeeService'

export default function EmployeesPage() {
  const { user } = useAuth()
  const isManager = user?.role === 'MANAGER_ADMIN'

  const [employees, setEmployees] = useState([])
  const [departments, setDepartments] = useState([])
  const [teams, setTeams] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [notification, setNotification] = useState(null)

  // Filters & Search
  const [searchQuery, setSearchQuery] = useState('')
  const [selectedDeptPill, setSelectedDeptPill] = useState('All')
  const [statusFilter, setStatusFilter] = useState('ALL')

  // Modals state
  const [profileModalOpen, setProfileModalOpen] = useState(false)
  const [selectedEmployee, setSelectedEmployee] = useState(null)

  const [createModalOpen, setCreateModalOpen] = useState(false)

  const [editOfficialOpen, setEditOfficialOpen] = useState(false)
  const [editingOfficialEmp, setEditingOfficialEmp] = useState(null)

  const [editContactOpen, setEditContactOpen] = useState(false)
  const [editingContactEmp, setEditingContactEmp] = useState(null)

  const showNotification = (msg, isError = false) => {
    setNotification({ text: msg, isError })
    setTimeout(() => setNotification(null), 4000)
  }

  const loadData = async () => {
    setLoading(true)
    setError(null)
    try {
      const [empList, deptList, teamList] = await Promise.all([
        employeeService.getAll(),
        employeeService.getDepartments().catch(() => []),
        employeeService.getTeams().catch(() => [])
      ])
      setEmployees(empList || [])
      setDepartments(deptList || [])
      setTeams(teamList || [])
    } catch (err) {
      setError(err.message || 'Failed to load employee directory')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadData()
  }, [])

  // Department filter pills list
  const deptPillItems = useMemo(() => {
    return ['All', ...departments.map(d => d.name)]
  }, [departments])

  // Filtered employees calculation
  const filteredEmployees = useMemo(() => {
    return employees.filter(emp => {
      // Search keyword filter
      if (searchQuery.trim()) {
        const query = searchQuery.toLowerCase()
        const matchName = emp.fullName?.toLowerCase().includes(query)
        const matchEmail = emp.email?.toLowerCase().includes(query)
        const matchTitle = emp.jobTitle?.toLowerCase().includes(query)
        if (!matchName && !matchEmail && !matchTitle) return false
      }

      // Department filter
      if (selectedDeptPill !== 'All') {
        if (emp.department?.name !== selectedDeptPill) return false
      }

      // Status filter
      if (statusFilter !== 'ALL') {
        if (emp.status !== statusFilter) return false
      }

      return true
    })
  }, [employees, searchQuery, selectedDeptPill, statusFilter])

  // Summary Metrics
  const activeCount = employees.filter(e => e.status === 'ACTIVE').length
  const totalCount = employees.length

  // Handlers for Employee Actions
  const handleCreateEmployee = async (payload) => {
    await employeeService.create(payload)
    showNotification('Employee onboarded successfully!')
    loadData()
  }

  const handleUpdateOfficial = async (id, payload) => {
    await employeeService.updateOfficial(id, payload)
    showNotification('Official information updated successfully!')
    loadData()
  }

  const handleUpdateContact = async (id, payload) => {
    await employeeService.updateContact(id, payload)
    showNotification('Contact information updated successfully!')
    loadData()
  }

  const handleToggleStatus = async (employee) => {
    const newStatus = employee.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE'
    const confirmMsg = employee.status === 'ACTIVE'
      ? `Are you sure you want to deactivate ${employee.fullName}? Their login account will also be disabled.`
      : `Activate ${employee.fullName}?`

    if (window.confirm(confirmMsg)) {
      try {
        await employeeService.changeStatus(employee.id, newStatus)
        showNotification(`Employee ${newStatus === 'ACTIVE' ? 'activated' : 'deactivated'} successfully!`)
        loadData()
      } catch (err) {
        showNotification(err.message || 'Status change failed', true)
      }
    }
  }

  return (
    <>
      <PageHeader
        title="Employee Directory"
        description="Comprehensive management of organization personnel, departments, teams, and hierarchical reporting."
        actions={
          isManager && (
            <button
              onClick={() => setCreateModalOpen(true)}
              className="inline-flex items-center gap-2 px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white font-semibold rounded-xl text-sm transition shadow-sm"
            >
              <UserPlus className="w-4 h-4" />
              Create Employee
            </button>
          )
        }
      />

      {/* Notifications / Alerts */}
      {notification && (
        <div className={`p-4 mb-4 rounded-2xl text-sm font-semibold flex items-center justify-between border ${
          notification.isError
            ? 'bg-red-50 text-red-700 border-red-200'
            : 'bg-emerald-50 text-emerald-800 border-emerald-200'
        }`}>
          <span>{notification.text}</span>
          <button onClick={() => setNotification(null)} className="text-xs opacity-70 hover:opacity-100">✕</button>
        </div>
      )}

      {/* Metrics Row */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-6">
        <MetricCard label="Total Personnel" value={totalCount} icon="Users" />
        <MetricCard label="Active Workforce" value={activeCount} icon="UserCheck" positive={true} />
        <MetricCard label="Departments" value={departments.length} icon="Building2" />
        <MetricCard label="Teams / Projects" value={teams.length} icon="FolderGit2" />
      </div>

      {/* Search & Filter Toolbar */}
      <Card className="mb-6">
        <div className="flex flex-col md:flex-row gap-3 items-stretch md:items-center justify-between mb-4">
          <div className="relative flex-1">
            <Search className="w-4 h-4 text-gray-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
            <input
              type="text"
              value={searchQuery}
              onChange={e => setSearchQuery(e.target.value)}
              placeholder="Search by name, email, or job title..."
              className="w-full pl-10 pr-4 py-2 text-sm border border-gray-200 rounded-xl focus:outline-none focus:border-blue-500 bg-gray-50/50"
            />
          </div>

          <div className="flex items-center gap-2">
            <div className="flex items-center gap-1.5 px-3 py-2 border border-gray-200 rounded-xl bg-white text-xs text-gray-600 shrink-0">
              <Filter className="w-3.5 h-3.5 text-gray-400" />
              <span>Status:</span>
              <select
                value={statusFilter}
                onChange={e => setStatusFilter(e.target.value)}
                className="bg-transparent font-semibold focus:outline-none cursor-pointer"
              >
                <option value="ALL">All Statuses</option>
                <option value="ACTIVE">Active</option>
                <option value="INACTIVE">Inactive</option>
                <option value="SUSPENDED">Suspended</option>
                <option value="ON_LEAVE">On Leave</option>
              </select>
            </div>

            <button
              onClick={loadData}
              title="Refresh Directory"
              className="p-2 border border-gray-200 rounded-xl hover:bg-gray-50 text-gray-500 transition"
            >
              <RefreshCw className={`w-4 h-4 ${loading ? 'animate-spin' : ''}`} />
            </button>
          </div>
        </div>

        {/* Department Pills */}
        <FilterPills
          items={deptPillItems}
          value={selectedDeptPill}
          onChange={setSelectedDeptPill}
        />
      </Card>

      {/* Employee List Table or Empty State */}
      <Card>
        {loading ? (
          <div className="py-12 flex flex-col items-center justify-center text-gray-400 gap-3">
            <RefreshCw className="w-6 h-6 animate-spin text-blue-600" />
            <span className="text-sm font-medium">Loading employee records...</span>
          </div>
        ) : error ? (
          <div className="py-8 flex flex-col items-center justify-center text-red-600 gap-2">
            <AlertCircle className="w-6 h-6" />
            <span className="text-sm font-semibold">{error}</span>
            <button onClick={loadData} className="text-xs text-blue-600 underline mt-1">Try again</button>
          </div>
        ) : filteredEmployees.length === 0 ? (
          <EmptyState
            icon="Users"
            title="No matching employees found"
            description={
              searchQuery || selectedDeptPill !== 'All' || statusFilter !== 'ALL'
                ? 'Try adjusting your search criteria or clearing selected filters.'
                : 'No employee records are available in the organization directory yet.'
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
        supervisors={employees}
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
        supervisors={employees}
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
        onClose={() => {
          setProfileModalOpen(false)
          setSelectedEmployee(null)
        }}
      />
    </>
  )
}
