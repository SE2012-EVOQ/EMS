import React from 'react'
import { User, Mail, Phone, MapPin, Shield, Edit2, UserCheck, Eye } from 'lucide-react'
import StatusBadge from '../../../components/common/StatusBadge'

export default function EmployeeTable({
  employees = [],
  currentUser,
  onViewProfile,
  onEditOfficial,
  onEditContact,
  onToggleStatus
}) {
  const isManager = currentUser?.role === 'MANAGER_ADMIN'

  const formatStatus = (status) => {
    if (!status) return 'Inactive'
    if (status === 'ACTIVE') return 'Active'
    if (status === 'INACTIVE') return 'Inactive'
    if (status === 'SUSPENDED') return 'Pending'
    if (status === 'ON_LEAVE') return 'Leave'
    return status
  }

  return (
    <div className="overflow-x-auto">
      <table className="w-full text-left text-sm">
        <thead className="border-b border-gray-100 bg-gray-50/50 text-xs font-semibold text-gray-500 uppercase tracking-wider">
          <tr>
            <th className="py-3.5 px-4">Employee</th>
            <th className="py-3.5 px-4">Department & Team</th>
            <th className="py-3.5 px-4">Role / Title</th>
            <th className="py-3.5 px-4">Supervisor</th>
            <th className="py-3.5 px-4">Status</th>
            <th className="py-3.5 px-4 text-right">Actions</th>
          </tr>
        </thead>
        <tbody className="divide-y divide-gray-100">
          {employees.map((emp) => {
            const isSelf = currentUser?.employeeId === emp.id
            const canEditContact = isManager || isSelf

            return (
              <tr key={emp.id} className="hover:bg-gray-50/70 transition-colors">
                <td className="py-3.5 px-4">
                  <div className="flex items-center gap-3">
                    <div className="w-9 h-9 rounded-full bg-blue-100 text-blue-700 font-bold flex items-center justify-center text-xs shrink-0">
                      {emp.firstName?.[0]}{emp.lastName?.[0]}
                    </div>
                    <div>
                      <div className="font-semibold text-gray-900">{emp.fullName}</div>
                      <div className="text-xs text-gray-500 flex items-center gap-1.5 mt-0.5">
                        <Mail className="w-3 h-3 text-gray-400" />
                        {emp.email}
                      </div>
                    </div>
                  </div>
                </td>

                <td className="py-3.5 px-4">
                  <div className="text-gray-900 font-medium">{emp.department?.name || '—'}</div>
                  <div className="text-xs text-gray-500">{emp.team?.name || 'No team'}</div>
                </td>

                <td className="py-3.5 px-4">
                  <div className="text-gray-900 font-medium">{emp.jobTitle}</div>
                  {emp.account?.role && (
                    <span className="inline-flex items-center gap-1 text-[11px] text-gray-500 mt-0.5">
                      <Shield className="w-3 h-3 text-indigo-500" />
                      {emp.account.role}
                    </span>
                  )}
                </td>

                <td className="py-3.5 px-4">
                  {emp.supervisor ? (
                    <div>
                      <div className="text-gray-900 font-medium text-xs">{emp.supervisor.fullName}</div>
                      <div className="text-[11px] text-gray-500">{emp.supervisor.jobTitle}</div>
                    </div>
                  ) : (
                    <span className="text-xs text-gray-400">None</span>
                  )}
                </td>

                <td className="py-3.5 px-4">
                  <StatusBadge status={formatStatus(emp.status)} />
                </td>

                <td className="py-3.5 px-4 text-right">
                  <div className="flex items-center justify-end gap-1.5">
                    <button
                      onClick={() => onViewProfile(emp)}
                      title="View Details"
                      className="p-1.5 rounded-lg hover:bg-gray-100 text-gray-600 transition"
                    >
                      <Eye className="w-4 h-4" />
                    </button>

                    {canEditContact && (
                      <button
                        onClick={() => onEditContact(emp)}
                        title="Update Contact Info"
                        className="p-1.5 rounded-lg hover:bg-blue-50 text-blue-600 transition"
                      >
                        <Phone className="w-4 h-4" />
                      </button>
                    )}

                    {isManager && (
                      <>
                        <button
                          onClick={() => onEditOfficial(emp)}
                          title="Edit Official Information"
                          className="p-1.5 rounded-lg hover:bg-amber-50 text-amber-600 transition"
                        >
                          <Edit2 className="w-4 h-4" />
                        </button>
                        <button
                          onClick={() => onToggleStatus(emp)}
                          title={emp.status === 'ACTIVE' ? 'Deactivate Employee' : 'Activate Employee'}
                          className={`p-1.5 rounded-lg transition ${
                            emp.status === 'ACTIVE'
                              ? 'hover:bg-red-50 text-red-500'
                              : 'hover:bg-green-50 text-green-600'
                          }`}
                        >
                          <UserCheck className="w-4 h-4" />
                        </button>
                      </>
                    )}
                  </div>
                </td>
              </tr>
            )
          })}
        </tbody>
      </table>
    </div>
  )
}
