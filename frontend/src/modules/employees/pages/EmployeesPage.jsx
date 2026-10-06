import React, { useEffect, useState, useMemo } from 'react'
import {
  UserPlus,
  Search,
  RefreshCw,
  AlertCircle,
  CheckCircle2
} from 'lucide-react'

import Card from '../../../components/common/Card'
import EmptyState from '../../../components/common/EmptyState'
import { useAuth } from '../../../context/AuthContext'

import EmployeeTable from '../components/EmployeeTable'
import EmployeeProfileModal from '../components/EmployeeProfileModal'
import CreateEmployeeModal from '../components/CreateEmployeeModal'
import EditOfficialModal from '../components/EditOfficialModal'
import EditContactModal from '../components/EditContactModal'
import { employeeService } from '../services/employeeService'

const FILTER_PILLS = ['All', 'Management', 'Vision', 'Platform', 'Inactive']

export default function EmployeesPage() {
  const { user } = useAuth()
  const isManager = user?.role === 'MANAGER_ADMIN'

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

  // Filtered employees calculation matching demo tabs
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

      // 2. Filter Pills ('All', 'Management', 'Vision', 'Platform', 'Inactive')
      if (selectedFilter === 'Inactive') {
        return emp.status === 'INACTIVE'
      }

      // For All, Management, Vision, Platform, hide Inactive unless specifically filtered
      if (emp.status === 'INACTIVE') {
        return false
      }

      if (selectedFilter === 'Management') {
        const dept = emp.department?.name || ''
        const team = emp.team?.name || ''
        return dept.includes('Management') || team.includes('Management')
      }

      if (selectedFilter === 'Vision') {
        const team = emp.team?.name || ''
        return team.includes('Vision')
      }

      if (selectedFilter === 'Platform') {
        const team = emp.team?.name || ''
        return team.includes('Platform')
      }

      return true
    })
  }, [employees, searchQuery, selectedFilter])

  // Handlers for Employee Actions
  const handleCreateEmployee = async (payload) => {
    await employeeService.create(payload)
    showToast('Employee added.')
    loadData()
  }

  const handleUpdateOfficial = async (id, payload) => {
    await employeeService.updateOfficial(id, payload)
    showToast('Employee record updated.')
    loadData()
  }

  const handleUpdateContact = async (id, payload) => {
    await employeeService.updateContact(id, payload)
    showToast('Contact details updated.')
    loadData()
  }

  const handleToggleStatus = async (employee) => {
    const isCurrentlyActive = employee.status === 'ACTIVE'
    const newStatus = isCurrentlyActive ? 'INACTIVE' : 'ACTIVE'
    const confirmMsg = isCurrentlyActive
      ? `Deactivate ${employee.fullName}? Historical records will be retained.`
      : `Re-activate ${employee.fullName}?`

    if (window.confirm(confirmMsg)) {
      try {
        await employeeService.changeStatus(employee.id, newStatus)
        showToast(isCurrentlyActive ? 'Employee deactivated; history retained.' : 'Employee activated.')
        loadData()
      } catch (err) {
        showToast(err.message || 'Status change failed', true)
      }
    }
  }

  return (
    <div className="space-y-4">
      {/* Toast Notification */}
      {toast && (
        <div className="fixed top-6 right-6 z-[100] max-w-sm pointer-events-auto transition-all animate-in fade-in slide-in-from-top-2">
          <div className={`p-4 rounded-2xl shadow-xl border flex items-center gap-3 ${
            toast.isError
              ? 'bg-app-pink-bg text-app-pink border-app-pink/20'
              : 'bg-white text-gray-900 border-app-border'
          }`}>
            {!toast.isError && <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0" />}
            <span className="text-xs font-bold">{toast.message}</span>
          </div>
        </div>
      )}

      {/* Directory Section Header & Add Employee Action */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pt-1 pb-1">
        <div>
          <h2 className="text-xl sm:text-2xl font-extrabold tracking-tight txt">
            Employee directory
          </h2>
          <p className="text-xs text-app-muted muted font-medium mt-0.5">
            Maintain official employee and organization records.
          </p>
        </div>

        {isManager && (
          <button
            onClick={() => setCreateModalOpen(true)}
            className="bg-[#1A1D1F] dark-primary hover:bg-black text-white px-4 sm:px-5 py-2.5 rounded-full text-xs sm:text-sm font-bold tracking-wide transition flex items-center gap-2 shadow-sm self-start sm:self-auto"
          >
            <UserPlus className="w-4 h-4" />
            <span>Add employee</span>
          </button>
        )}
      </div>

      {/* Filter Pills & Search Bar Toolbar */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-3">
        <div className="flex gap-2 overflow-x-auto pb-1">
          {FILTER_PILLS.map(filter => (
            <button
              key={filter}
              onClick={() => setSelectedFilter(filter)}
              className={`px-3.5 py-2 rounded-full text-xs font-bold shrink-0 transition ${
                selectedFilter === filter
                  ? 'bg-[#1A1D1F] dark-primary text-white shadow-sm'
                  : 'surface bg-white border border-gray-200 text-gray-700 hover:bg-gray-50'
              }`}
            >
              {filter}
            </button>
          ))}
        </div>

        <div className="flex items-center gap-2">
          <div className="relative flex-1 sm:w-64">
            <Search className="w-3.5 h-3.5 text-app-muted absolute left-3.5 top-1/2 -translate-y-1/2" />
            <input
              type="text"
              value={searchQuery}
              onChange={e => setSearchQuery(e.target.value)}
              placeholder="Search employees..."
              className="w-full pl-9 pr-3 py-2 rounded-full bg-white surface border border-gray-200 text-xs font-medium outline-none focus:border-gray-400 placeholder:text-app-muted"
            />
          </div>

          <button
            onClick={loadData}
            title="Refresh directory"
            className="w-8 h-8 rounded-full surface bg-white border border-gray-200 flex items-center justify-center text-app-muted hover:text-gray-900 transition shrink-0"
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
            title="No matching employees"
            description={
              searchQuery || selectedFilter !== 'All'
                ? 'No employee records match the current filter or search criteria.'
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
          />
        )}
      </Card>

      {/* Modals */}
      <CreateEmployeeModal
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
    </div>
  )
}
