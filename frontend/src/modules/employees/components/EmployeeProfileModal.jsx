import React, { useEffect, useState } from 'react'
import Modal from '../../../components/common/Modal'
import StatusBadge from '../../../components/common/StatusBadge'
import { employeeService } from '../services/employeeService'
import { Mail, Phone, MapPin, Calendar, Briefcase, Building, Users, Shield, User } from 'lucide-react'

export default function EmployeeProfileModal({ employee, open, onClose }) {
  const [directReports, setDirectReports] = useState([])
  const [loadingReports, setLoadingReports] = useState(false)

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

  const formatStatus = (status) => {
    if (!status) return 'Inactive'
    if (status === 'ACTIVE') return 'Active'
    if (status === 'INACTIVE') return 'Inactive'
    if (status === 'SUSPENDED') return 'Pending'
    if (status === 'ON_LEAVE') return 'Leave'
    return status
  }

  return (
    <Modal open={open} title="Employee Profile" onClose={onClose}>
      <div className="space-y-6">
        {/* Header Summary */}
        <div className="flex items-center gap-4 pb-4 border-b border-gray-100">
          <div className="w-14 h-14 rounded-full bg-blue-100 text-blue-700 font-bold flex items-center justify-center text-xl shrink-0">
            {employee.firstName?.[0]}{employee.lastName?.[0]}
          </div>
          <div>
            <h3 className="text-lg font-bold text-gray-900">{employee.fullName}</h3>
            <p className="text-sm text-gray-500 font-medium">{employee.jobTitle}</p>
            <div className="mt-1 flex items-center gap-2">
              <StatusBadge status={formatStatus(employee.status)} />
              {employee.account?.role && (
                <span className="inline-flex items-center gap-1 text-xs text-indigo-700 bg-indigo-50 px-2.5 py-0.5 rounded-full font-semibold">
                  <Shield className="w-3 h-3" />
                  {employee.account.role}
                </span>
              )}
            </div>
          </div>
        </div>

        {/* Official Organization Details */}
        <div>
          <h4 className="text-xs font-bold text-gray-400 uppercase tracking-wider mb-3">Organization Details</h4>
          <div className="grid grid-cols-2 gap-3 text-sm">
            <div className="p-3 bg-gray-50 rounded-xl">
              <div className="text-xs text-gray-500 flex items-center gap-1.5 mb-1">
                <Building className="w-3.5 h-3.5 text-gray-400" />
                Department
              </div>
              <div className="font-semibold text-gray-800">{employee.department?.name || 'Not Assigned'}</div>
            </div>

            <div className="p-3 bg-gray-50 rounded-xl">
              <div className="text-xs text-gray-500 flex items-center gap-1.5 mb-1">
                <Users className="w-3.5 h-3.5 text-gray-400" />
                Team / Project
              </div>
              <div className="font-semibold text-gray-800">{employee.team?.name || 'No Project'}</div>
            </div>

            <div className="p-3 bg-gray-50 rounded-xl">
              <div className="text-xs text-gray-500 flex items-center gap-1.5 mb-1">
                <User className="w-3.5 h-3.5 text-gray-400" />
                Reporting Supervisor
              </div>
              <div className="font-semibold text-gray-800">{employee.supervisor?.fullName || 'None (Direct Report to Head)'}</div>
            </div>

            <div className="p-3 bg-gray-50 rounded-xl">
              <div className="text-xs text-gray-500 flex items-center gap-1.5 mb-1">
                <Calendar className="w-3.5 h-3.5 text-gray-400" />
                Joined Date
              </div>
              <div className="font-semibold text-gray-800">{employee.hireDate}</div>
            </div>
          </div>
        </div>

        {/* Contact Information */}
        <div>
          <h4 className="text-xs font-bold text-gray-400 uppercase tracking-wider mb-3">Contact Information</h4>
          <div className="space-y-2 text-sm bg-gray-50 p-3.5 rounded-xl">
            <div className="flex items-center gap-2.5 text-gray-700">
              <Mail className="w-4 h-4 text-gray-400 shrink-0" />
              <span>{employee.email}</span>
            </div>
            <div className="flex items-center gap-2.5 text-gray-700">
              <Phone className="w-4 h-4 text-gray-400 shrink-0" />
              <span>{employee.phone || 'No phone number on record'}</span>
            </div>
            <div className="flex items-center gap-2.5 text-gray-700">
              <MapPin className="w-4 h-4 text-gray-400 shrink-0" />
              <span>{employee.address || 'No address provided'}</span>
            </div>
          </div>
        </div>

        {/* Direct Reports (Subordinates) */}
        {directReports.length > 0 && (
          <div>
            <h4 className="text-xs font-bold text-gray-400 uppercase tracking-wider mb-2.5">
              Direct Subordinates ({directReports.length})
            </h4>
            <div className="space-y-2 max-h-36 overflow-y-auto">
              {directReports.map(sub => (
                <div key={sub.id} className="flex items-center justify-between p-2.5 bg-gray-50 rounded-lg text-sm">
                  <div className="flex items-center gap-2">
                    <div className="w-7 h-7 rounded-full bg-blue-100 text-blue-700 font-bold flex items-center justify-center text-xs">
                      {sub.firstName?.[0]}{sub.lastName?.[0]}
                    </div>
                    <div>
                      <div className="font-semibold text-gray-800">{sub.fullName}</div>
                      <div className="text-xs text-gray-500">{sub.jobTitle}</div>
                    </div>
                  </div>
                  <StatusBadge status={formatStatus(sub.status)} />
                </div>
              ))}
            </div>
          </div>
        )}

        <div className="pt-2 flex justify-end">
          <button
            type="button"
            onClick={onClose}
            className="px-4 py-2 bg-gray-100 hover:bg-gray-200 text-gray-700 font-semibold rounded-xl text-sm transition"
          >
            Close
          </button>
        </div>
      </div>
    </Modal>
  )
}
