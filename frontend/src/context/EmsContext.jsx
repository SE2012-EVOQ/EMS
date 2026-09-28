import { createContext, useContext, useMemo, useState } from 'react'
import { createMockDatabase, DEMO_TODAY } from '../data/mock/mockDatabase'

const EmsContext = createContext(null)

const roleProfiles = {
  admin: { label: 'Manager / Admin', employeeId: 'E001' },
  supervisor: { label: 'Supervisor / Team Lead', employeeId: 'E006' },
  employee: { label: 'Employee', employeeId: 'E004' }
}

export function EmsProvider({ children }) {
  const [db, setDb] = useState(() => createMockDatabase())
  const [role, setRole] = useState('admin')

  const profile = roleProfiles[role]
  const currentEmployee = db.employees.find(e => e.id === profile.employeeId)
  const employeeById = id => db.employees.find(e => e.id === id)
  const directReports = (supervisorId = currentEmployee.id) => db.employees.filter(e => e.supervisor === supervisorId && e.status === 'Active')
  const teamForSupervisor = () => currentEmployee.team
  const currentAssetAssignment = assetId => db.assetAssignments.find(a => a.asset === assetId && !a.returnedDate && a.status === 'Active')
  const assetAssignmentsForEmployee = employeeId => db.assetAssignments.filter(a => a.employee === employeeId && !a.returnedDate && a.status === 'Active')
  const scheduleForTeam = team => db.schedules.find(s => s.team === team && s.status === 'Published')
  const entriesForTeam = team => {
    const schedule = scheduleForTeam(team)
    return schedule ? db.scheduleEntries.filter(e => e.scheduleId === schedule.id) : []
  }
  const approvedLeaveOn = (employeeId, date) => db.leaveRequests.find(l => l.employee === employeeId && l.status === 'Approved' && date >= l.from && date <= l.to)

  const allowedEmployees = () => {
    if (role === 'admin') return db.employees
    if (role === 'employee') return db.employees.filter(e => e.id === currentEmployee.id)
    return db.employees.filter(e => e.id === currentEmployee.id || e.supervisor === currentEmployee.id)
  }

  const allowedLeave = () => {
    if (role === 'admin') return db.leaveRequests
    if (role === 'employee') return db.leaveRequests.filter(x => x.employee === currentEmployee.id)
    const ids = [currentEmployee.id, ...directReports().map(e => e.id)]
    return db.leaveRequests.filter(x => ids.includes(x.employee))
  }

  const allowedAttendance = () => {
    if (role === 'admin') return db.attendance
    if (role === 'employee') return db.attendance.filter(x => x.employee === currentEmployee.id)
    const ids = [currentEmployee.id, ...directReports().map(e => e.id)]
    return db.attendance.filter(x => ids.includes(x.employee))
  }

  const allowedAssets = () => {
    if (role === 'admin') return db.assets
    const mine = new Set(assetAssignmentsForEmployee(currentEmployee.id).map(a => a.asset))
    return db.assets.filter(a => mine.has(a.id))
  }

  const requestLeave = ({ type, from, to, reason }) => {
    if (!currentEmployee.supervisor) throw new Error('No reporting supervisor is configured for this employee.')
    if (!from || !to || to < from) throw new Error('Enter a valid leave date range.')
    const days = Math.round((new Date(to) - new Date(from)) / 86400000) + 1
    const balance = db.leaveBalances[currentEmployee.id]?.[type] || 0
    if (days > balance) throw new Error(`Only ${balance} ${type.toLowerCase()} leave days are available.`)
    setDb(prev => {
      const next = structuredClone(prev)
      const numeric = Math.max(...next.leaveRequests.map(x => Number(x.id.split('-')[1]))) + 1
      next.leaveRequests.unshift({
        id: `LV-${numeric}`,
        employee: currentEmployee.id,
        type, from, to, days,
        reason: reason || '—',
        status: 'Pending',
        submitted: DEMO_TODAY,
        decidedBy: null,
        decidedAt: null
      })
      return next
    })
  }

  const decideLeave = (id, status) => setDb(prev => {
    const next = structuredClone(prev)
    const request = next.leaveRequests.find(x => x.id === id)
    if (!request) return prev
    const employee = next.employees.find(e => e.id === request.employee)
    if (role !== 'supervisor' || employee?.supervisor !== currentEmployee.id || request.status !== 'Pending') return prev
    request.status = status
    request.decidedBy = currentEmployee.id
    request.decidedAt = DEMO_TODAY
    if (status === 'Approved') {
      next.leaveBalances[request.employee][request.type] = Math.max(0, next.leaveBalances[request.employee][request.type] - request.days)
    }
    return next
  })

  const saveAttendance = record => setDb(prev => {
    const next = structuredClone(prev)
    const existing = next.attendance.find(x => x.id === record.id) || next.attendance.find(x => x.employee === record.employee && x.date === record.date)
    const normalized = {
      ...record,
      id: existing?.id || `AT-${record.date}-${record.employee}`,
      hours: ['Absent','Leave'].includes(record.status) ? 0 : Number(record.hours || 0),
      checkIn: ['Absent','Leave'].includes(record.status) ? '—' : record.checkIn || '—',
      checkOut: ['Absent','Leave'].includes(record.status) ? '—' : record.checkOut || '—'
    }
    if (existing) Object.assign(existing, normalized)
    else next.attendance.push(normalized)
    return next
  })

  const saveScheduleEntry = entry => {
    if (entry.end <= entry.start) throw new Error('End time must be after start time.')
    const leave = approvedLeaveOn(entry.employee, entry.date)
    if (leave) throw new Error(`Cannot schedule this employee: approved ${leave.type.toLowerCase()} leave covers ${entry.date}.`)
    const overlap = db.scheduleEntries.find(x => x.employee === entry.employee && x.date === entry.date && x.id !== entry.id && entry.start < x.end && x.start < entry.end)
    if (overlap) throw new Error(`Schedule overlaps ${overlap.start}–${overlap.end} on ${entry.date}.`)

    setDb(prev => {
      const next = structuredClone(prev)
      let parent = next.schedules.find(s => s.team === currentEmployee.team && s.status === 'Published')
      if (!parent) {
        parent = { id: `SCH-${String(next.schedules.length + 1).padStart(3,'0')}`, team: currentEmployee.team, periodStart: entry.date, periodEnd: entry.date, status: 'Published', createdBy: currentEmployee.id, createdAt: DEMO_TODAY, updatedAt: DEMO_TODAY }
        next.schedules.push(parent)
      }
      const existing = next.scheduleEntries.find(x => x.id === entry.id)
      if (existing) Object.assign(existing, { ...entry, scheduleId: parent.id })
      else next.scheduleEntries.push({ ...entry, id: `SE-${String(next.scheduleEntries.length + 1).padStart(3,'0')}`, scheduleId: parent.id })
      return next
    })
  }

  const saveEmployee = employee => setDb(prev => {
    const next = structuredClone(prev)
    const existing = next.employees.find(e => e.id === employee.id)
    if (existing) Object.assign(existing, employee)
    else {
      const nextId = `E${String(Math.max(...next.employees.map(x => Number(x.id.slice(1)))) + 1).padStart(3,'0')}`
      next.employees.push({ ...employee, id: nextId, status: 'Active', avatar: employee.name.split(' ').map(x => x[0]).slice(0,2).join('').toUpperCase() })
      next.leaveBalances[nextId] = { Annual: 12, Medical: 7, Casual: 4 }
    }
    return next
  })

  const deactivateEmployee = id => setDb(prev => {
    const next = structuredClone(prev)
    const employee = next.employees.find(e => e.id === id)
    if (employee) employee.status = 'Inactive'
    const account = next.userAccounts.find(a => a.employee === id)
    if (account) account.active = false
    return next
  })

  const registerAsset = asset => setDb(prev => {
    const next = structuredClone(prev)
    const nextId = `AST-${String(Math.max(...next.assets.map(a => Number(a.id.split('-')[1]))) + 1).padStart(3,'0')}`
    next.assets.push({ ...asset, id: nextId, status: 'Available' })
    return next
  })

  const assignAsset = (assetId, employeeId) => setDb(prev => {
    const next = structuredClone(prev)
    const asset = next.assets.find(a => a.id === assetId)
    const active = next.assetAssignments.find(a => a.asset === assetId && !a.returnedDate && a.status === 'Active')
    if (!asset || active || asset.status !== 'Available') return prev
    next.assetAssignments.push({ id: `AA-${String(next.assetAssignments.length + 1).padStart(3,'0')}`, asset: assetId, employee: employeeId, assignedDate: DEMO_TODAY, returnedDate: null, status: 'Active' })
    asset.status = 'Assigned'
    return next
  })

  const returnAsset = assetId => setDb(prev => {
    const next = structuredClone(prev)
    const assignment = next.assetAssignments.find(a => a.asset === assetId && !a.returnedDate && a.status === 'Active')
    const asset = next.assets.find(a => a.id === assetId)
    if (assignment) { assignment.returnedDate = DEMO_TODAY; assignment.status = 'Returned' }
    if (asset) asset.status = 'Available'
    return next
  })

  const value = useMemo(() => ({
    db, role, setRole, roleProfiles, profile, currentEmployee, employeeById, directReports,
    teamForSupervisor, currentAssetAssignment, assetAssignmentsForEmployee, scheduleForTeam,
    entriesForTeam, approvedLeaveOn, allowedEmployees, allowedLeave, allowedAttendance, allowedAssets,
    requestLeave, decideLeave, saveAttendance, saveScheduleEntry, saveEmployee, deactivateEmployee,
    registerAsset, assignAsset, returnAsset
  }), [db, role])

  return <EmsContext.Provider value={value}>{children}</EmsContext.Provider>
}

export function useEms() {
  const context = useContext(EmsContext)
  if (!context) throw new Error('useEms must be used inside EmsProvider')
  return context
}
