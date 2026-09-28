import { Navigate, createBrowserRouter } from 'react-router-dom'
import AppLayout from '../components/layout/AppLayout'
import DashboardPage from '../modules/dashboard/pages/DashboardPage'
import EmployeesPage from '../modules/employees/pages/EmployeesPage'
import LeavePage from '../modules/leave/pages/LeavePage'
import AttendancePage from '../modules/attendance/pages/AttendancePage'
import SchedulePage from '../modules/attendance/pages/SchedulePage'
import AssetsPage from '../modules/assets/pages/AssetsPage'
import ReportsPage from '../modules/reports/pages/ReportsPage'

export const router = createBrowserRouter([
  {
    path: '/',
    element: <AppLayout />,
    children: [
      { index: true, element: <Navigate to="/dashboard" replace /> },
      { path: 'dashboard', element: <DashboardPage /> },
      { path: 'employees', element: <EmployeesPage /> },
      { path: 'leave', element: <LeavePage /> },
      { path: 'attendance', element: <AttendancePage /> },
      { path: 'schedule', element: <SchedulePage /> },
      { path: 'assets', element: <AssetsPage /> },
      { path: 'reports', element: <ReportsPage /> },
      { path: '*', element: <Navigate to="/dashboard" replace /> }
    ]
  }
])
