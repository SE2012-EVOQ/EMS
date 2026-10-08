export const hasTeamWorkspace = role => role === 'MANAGER_ADMIN' || role === 'SUPERVISOR'

export function navigationForRole(role) {
  const team = hasTeamWorkspace(role)
  return [
    ['/dashboard', 'Dashboard', 'LayoutGrid'],
    ['/profile', 'My profile', 'UserRound'],
    ...(team ? [['/employees', role === 'SUPERVISOR' ? 'My team' : 'Employees', 'Users']] : []),
    ['/leave', team ? 'Leave' : 'My leave', 'CalendarDays'],
    ['/attendance', team ? 'Attendance' : 'My attendance', 'BadgeCheck'],
    ['/schedule', team ? 'Schedule' : 'My schedule', 'CalendarRange'],
    ['/assets', role === 'MANAGER_ADMIN' ? 'Assets' : 'My assets', 'Laptop'],
    ...(team ? [['/reports', 'Reports', 'BarChart3']] : [])
  ]
}
