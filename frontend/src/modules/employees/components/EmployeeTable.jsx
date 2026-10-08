import React from 'react'
import { ChevronRight } from 'lucide-react'
import StatusBadge from '../../../components/common/StatusBadge'

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

export default function EmployeeTable({
  employees = [],
  currentUser,
  onViewProfile
}) {
  return (
    <div className="overflow-x-auto">
      <table className="w-full min-w-[780px]">
        <thead>
          <tr className="text-left text-[10px] uppercase tracking-wider text-app-muted muted">
            <th className="pb-3 font-extrabold">Employee</th>
            <th className="pb-3 font-extrabold">Job title</th>
            <th className="pb-3 font-extrabold">Team / Project</th>
            <th className="pb-3 font-extrabold">Supervisor</th>
            <th className="pb-3 font-extrabold">Status</th>
            <th className="pb-3"></th>
          </tr>
        </thead>
        <tbody>
          {employees.map((emp) => {
            const code = `E${String(emp.id).padStart(3, '0')}`
            const initials = getInitials(emp)
            const teamName = emp.team?.name || 'General'
            const deptName = emp.department?.name || '—'
            const supervisorName = emp.supervisor?.fullName || '—'
            const statusLabel = { ACTIVE: 'Active', INACTIVE: 'Inactive', SUSPENDED: 'Suspended', ON_LEAVE: 'On Leave' }[emp.status] || emp.status

            return (
              <tr
                key={emp.id}
                onClick={() => onViewProfile(emp)}
                className="border-t border-app-border hover:bg-app-subtle/60 hover-surface cursor-pointer transition"
              >
                <td className="py-4">
                  <div className="flex items-center gap-3">
                    <div className="w-10 h-10 rounded-full bg-[#DEE8FF] avatar-soft flex items-center justify-center text-[10px] font-extrabold text-[#3B5BDB] shrink-0">
                      {initials}
                    </div>
                    <div>
                      <div className="text-xs font-bold txt">{emp.fullName}</div>
                      <div className="text-[10px] text-app-muted muted mt-0.5">
                        {code} · {emp.email}
                      </div>
                    </div>
                  </div>
                </td>

                <td className="py-4 text-xs font-semibold txt">
                  {emp.jobTitle}
                </td>

                <td className="py-4">
                  <div className="text-xs font-bold txt">{teamName}</div>
                  <div className="text-[10px] text-app-muted muted mt-0.5">{deptName}</div>
                </td>

                <td className="py-4 text-xs text-app-muted muted">
                  {supervisorName}
                </td>

                <td className="py-4">
                  <StatusBadge status={statusLabel} />
                </td>

                <td className="py-4 text-right pr-2">
                  <div className="w-8 h-8 rounded-full hover:bg-app-subtle flex items-center justify-center ml-auto">
                    <ChevronRight className="w-4 h-4 text-app-muted" />
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
