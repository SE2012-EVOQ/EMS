import React, { useEffect, useState } from 'react'
import Modal from '../../../components/common/Modal'
import StatusBadge from '../../../components/common/StatusBadge'
import { employeeService } from '../services/employeeService'
import { Mail, Phone, MapPin, Calendar, Building, Users, Shield, User, Edit3, UserX, UserCheck } from 'lucide-react'

function getInitials(emp) {
  if (!emp) return '?'
  const f = emp.firstName?.trim() || ''
  const l = emp.lastName?.trim() || ''
  if (f && l) {
    const fChar = f.replace(/[^a-zA-Z]/g, '')[0] || f[0]
    const lChar = l.replace(/[^a-zA-Z]/g, '')[0] || l[0]
    return (fChar + lChar).toUpperCase()
  }
  if (emp.fullName) {
    const parts = emp.fullName.trim().split(/\s+/)
    if (parts.length > 1) {
      const p1 = parts[0].replace(/[^a-zA-Z]/g, '')[0] || parts[0][0]
      const p2 = parts[parts.length - 1].replace(/[^a-zA-Z]/g, '')[0] || parts[parts.length - 1][0]
      return (p1 + p2).toUpperCase()
    }
    return parts[0].slice(0, 2).toUpperCase()
  }
  return 'EP'
}

export default function EmployeeProfileModal({
  employee,
  open,
  onClose,
  currentUser,
  onEditOfficial,
  onEditContact,
  onToggleStatus
}) {
  const [directReports, setDirectReports] = useState([])
  const [loadingReports, setLoadingReports] = useState(false)

  const isManager = currentUser?.role === 'MANAGER_ADMIN'
  const isSelf = currentUser?.employeeId === employee?.id
  const canEditContact = isManager || isSelf

  useEffect(() => {
    if (open && employee?.id) {
      setLoadingReports(true)
      employeeService.getDirectReports(employee.id)
        .then(reports => setDirectReports(reports || []))
        .catch(() => setDirectReports([]))
        .finally(() => setLoadingReports(false))
    }
  }, [open, employee?.id])

  if (!employee) return null

  const code = `E${String(employee.id).padStart(3, '0')}`
  const initials = getInitials(employee)
  const statusLabel = employee.status === 'ACTIVE' ? 'Active' : 'Inactive'

  const footer = (
    <div className="flex items-center justify-between w-full">
      <div className="flex items-center gap-2">
        {isManager && (
          <button
            type="button"
            onClick={() => {
              onClose()
              onToggleStatus(employee)
            }}
            className={`px-4 py-2 rounded-full text-xs font-bold transition flex items-center gap-1.5 ${
              employee.status === 'ACTIVE'
                ? 'bg-app-pink-bg text-app-pink hover:opacity-80'
                : 'bg-app-green-bg text-app-green hover:opacity-80'
            }`}
          >
            {employee.status === 'ACTIVE' ? <UserX className="w-3.5 h-3.5" /> : <UserCheck className="w-3.5 h-3.5" />}
            {employee.status === 'ACTIVE' ? 'Deactivate' : 'Activate'}
          </button>
        )}
      </div>

      <div className="flex items-center gap-2">
        {canEditContact && (
          <button
            type="button"
            onClick={() => {
              onClose()
              onEditContact(employee)
            }}
            className="px-4 py-2.5 rounded-full border border-gray-200 text-xs font-bold hover:bg-gray-50 transition"
          >
            Edit contact
          </button>
        )}

        {isManager && (
          <button
            type="button"
            onClick={() => {
              onClose()
              onEditOfficial(employee)
            }}
            className="px-5 py-2.5 rounded-full bg-[#1A1D1F] hover:bg-black text-white text-xs font-bold transition flex items-center gap-1.5"
          >
            <Edit3 className="w-3.5 h-3.5" />
            Edit record
          </button>
        )}
      </div>
    </div>
  )

  return (
    <Modal
      open={open}
      title={employee.fullName}
      subtitle={`${code} · ${employee.jobTitle}`}
      onClose={onClose}
      footer={footer}
      size="max-w-2xl"
    >
      <div className="space-y-6">
        {/* Profile Card Summary */}
        <div className="flex flex-col sm:flex-row items-center sm:items-start gap-4 p-4 rounded-2xl bg-app-subtle subtle">
          <div className="w-16 h-16 rounded-full bg-[#DEE8FF] avatar-soft text-[#3B5BDB] font-extrabold flex items-center justify-center text-xl shrink-0">
            {initials}
          </div>
          <div className="flex-1 text-center sm:text-left">
            <h3 className="text-base font-extrabold txt">{employee.fullName}</h3>
            <p className="text-xs text-app-muted muted font-medium mt-0.5">{employee.jobTitle}</p>
            <div className="mt-2.5 flex items-center justify-center sm:justify-start gap-2 flex-wrap">
              <StatusBadge status={statusLabel} />
              <span className="px-2.5 py-1 rounded-full bg-white text-[10px] font-bold text-app-muted border border-app-border">
                {code}
              </span>
              {employee.account?.role && (
                <span className="inline-flex items-center gap-1 text-[10px] text-app-blue bg-app-blue-bg px-2.5 py-1 rounded-full font-bold">
                  <Shield className="w-3 h-3" />
                  {employee.account.role}
                </span>
              )}
            </div>
          </div>
        </div>

        {/* Organization Information Grid */}
        <div>
          <h4 className="text-[10px] font-extrabold text-app-muted uppercase tracking-wider mb-3">
            Employment Information
          </h4>
          <div className="grid sm:grid-cols-2 gap-3 text-xs">
            <div className="p-3.5 bg-app-subtle subtle rounded-2xl">
              <div className="text-[10px] text-app-muted uppercase font-extrabold tracking-wider flex items-center gap-1.5 mb-1.5">
                <Building className="w-3.5 h-3.5 text-app-muted" />
                Department
              </div>
              <div className="font-bold txt text-sm">{employee.department?.name || 'Not Assigned'}</div>
            </div>

            <div className="p-3.5 bg-app-subtle subtle rounded-2xl">
              <div className="text-[10px] text-app-muted uppercase font-extrabold tracking-wider flex items-center gap-1.5 mb-1.5">
                <Users className="w-3.5 h-3.5 text-app-muted" />
                Team / Current Project
              </div>
              <div className="font-bold txt text-sm">{employee.team?.name || 'General / None'}</div>
            </div>

            <div className="p-3.5 bg-app-subtle subtle rounded-2xl">
              <div className="text-[10px] text-app-muted uppercase font-extrabold tracking-wider flex items-center gap-1.5 mb-1.5">
                <User className="w-3.5 h-3.5 text-app-muted" />
                Reporting Supervisor
              </div>
              <div className="font-bold txt text-sm">
                {employee.supervisor?.fullName || '— (Direct Report to Head)'}
              </div>
            </div>

            <div className="p-3.5 bg-app-subtle subtle rounded-2xl">
              <div className="text-[10px] text-app-muted uppercase font-extrabold tracking-wider flex items-center gap-1.5 mb-1.5">
                <Calendar className="w-3.5 h-3.5 text-app-muted" />
                Joined Date
              </div>
              <div className="font-bold txt text-sm">{employee.hireDate}</div>
            </div>
          </div>
        </div>

        {/* Contact Information */}
        <div>
          <h4 className="text-[10px] font-extrabold text-app-muted uppercase tracking-wider mb-3">
            Contact Details
          </h4>
          <div className="space-y-2.5 text-xs bg-app-subtle subtle p-4 rounded-2xl">
            <div className="flex items-center gap-3">
              <Mail className="w-4 h-4 text-app-muted shrink-0" />
              <span className="font-semibold txt">{employee.email}</span>
            </div>
            <div className="flex items-center gap-3">
              <Phone className="w-4 h-4 text-app-muted shrink-0" />
              <span className="font-semibold txt">{employee.phone || 'No phone number on record'}</span>
            </div>
            <div className="flex items-center gap-3">
              <MapPin className="w-4 h-4 text-app-muted shrink-0" />
              <span className="font-semibold txt">{employee.address || 'No residential address on record'}</span>
            </div>
          </div>
        </div>

        {/* Direct Reports / Subordinates if any */}
        {directReports.length > 0 && (
          <div>
            <h4 className="text-[10px] font-extrabold text-app-muted uppercase tracking-wider mb-2.5">
              Direct Subordinates ({directReports.length})
            </h4>
            <div className="space-y-2 max-h-36 overflow-y-auto">
              {directReports.map(sub => (
                <div key={sub.id} className="flex items-center justify-between p-2.5 bg-app-subtle subtle rounded-xl text-xs">
                  <div className="flex items-center gap-2.5">
                    <div className="w-7 h-7 rounded-full bg-[#DEE8FF] avatar-soft text-[#3B5BDB] font-extrabold flex items-center justify-center text-[9px]">
                      {getInitials(sub)}
                    </div>
                    <div>
                      <div className="font-bold txt">{sub.fullName}</div>
                      <div className="text-[10px] text-app-muted">{sub.jobTitle}</div>
                    </div>
                  </div>
                  <StatusBadge status={sub.status === 'ACTIVE' ? 'Active' : 'Inactive'} />
                </div>
              ))}
            </div>
          </div>
        )}
      </div>
    </Modal>
  )
}
