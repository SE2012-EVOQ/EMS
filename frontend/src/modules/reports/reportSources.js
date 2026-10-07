import AttendanceReport from '../attendance/reporting/AttendanceReport'

// Module owners can register a component with its own authorized summary/export API.
// Shared Reports supplies navigation only; it does not calculate another module's report.
export const reportSources = [
  { id: 'attendance', label: 'Attendance', Component: AttendanceReport },
  { id: 'employee', label: 'Employee', owner: 'Employee module owner' },
  { id: 'leave', label: 'Leave', owner: 'Leave module owner' },
  { id: 'asset', label: 'Asset', owner: 'Asset module owner' }
]
