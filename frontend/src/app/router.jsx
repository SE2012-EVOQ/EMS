import { Navigate, createBrowserRouter } from 'react-router-dom'
import RequireAuth from './RequireAuth'
import LoginPage from './LoginPage'
import FirstRunSetupPage from './FirstRunSetupPage'
import ChangePasswordPage from './ChangePasswordPage'
import AppLayout from '../components/layout/AppLayout'
import DashboardPage from '../modules/dashboard/pages/DashboardPage'
import EmployeesPage from '../modules/employees/pages/EmployeesPage'
import LeavePage from '../modules/leave/pages/LeavePage'
import AttendancePage from '../modules/attendance/pages/AttendancePage'
import SchedulePage from '../modules/attendance/pages/SchedulePage'
import AssetsPage from '../modules/assets/pages/AssetsPage'
import ReportsPage from '../modules/reports/pages/ReportsPage'
import MyProfilePage from '../modules/employees/pages/MyProfilePage'
import TeamWorkspace from './TeamWorkspace'

export const router = createBrowserRouter([
  { path: '/login', element: <LoginPage /> },
  { path: '/setup', element: <FirstRunSetupPage /> },
  {
    path: '/',
    element: <RequireAuth><AppLayout /></RequireAuth>,
    children: [
      { index: true, element: <Navigate to="/dashboard" replace /> },
      { path: 'dashboard', element: <DashboardPage /> },
      { path: 'account/password', element: <ChangePasswordPage /> },
      { path: 'profile', element: <MyProfilePage /> },
      { path: 'employees', element: <TeamWorkspace fallback="/profile"><EmployeesPage /></TeamWorkspace> },
      { path: 'leave', element: <LeavePage /> },
      { path: 'attendance', element: <AttendancePage /> },
      { path: 'schedule', element: <SchedulePage /> },
      { path: 'assets', element: <AssetsPage /> },
      { path: 'reports', element: <TeamWorkspace><ReportsPage /></TeamWorkspace> },
      { path: '*', element: <Navigate to="/dashboard" replace /> }
    ]
  }
])
