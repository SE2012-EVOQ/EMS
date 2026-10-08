import AttendanceReport from '../attendance/reporting/AttendanceReport'
import EmployeeReport from '../employees/reporting/EmployeeReport'
import LeaveReport from '../leave/reporting/LeaveReport'
import AssetReport from '../assets/reporting/AssetReport'

export const reportSources = [
  { id: 'attendance', label: 'Attendance', Component: AttendanceReport },
  { id: 'employee', label: 'Employee', Component: EmployeeReport },
  { id: 'leave', label: 'Leave', Component: LeaveReport },
  { id: 'asset', label: 'Asset', Component: AssetReport }
]
