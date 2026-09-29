import { apiRequest } from '../../../services/api'

const query = ({ from, to, employeeId }) => {
  const params = new URLSearchParams({ from, to })
  if (employeeId) params.set('employeeId', employeeId)
  return params.toString()
}

export const attendanceService = {
  mine: range => apiRequest(`/attendance-records/me?${query(range)}`),
  teams: () => apiRequest('/attendance-records/teams'),
  team: (teamId, range) => apiRequest(`/attendance-records/teams/${teamId}?${query(range)}`),
  employees: () => apiRequest('/attendance-records/employees'),
  all: range => apiRequest(`/attendance-records?${query(range)}`),
  today: () => apiRequest('/attendance-records/today'),
  checkIn: () => apiRequest('/attendance-records/check-in', { method: 'POST' }),
  checkOut: () => apiRequest('/attendance-records/check-out', { method: 'POST' }),
  createException: record => apiRequest('/attendance-records/exceptions', { method: 'POST', body: JSON.stringify(record) }),
  correct: (id, record) => apiRequest(`/attendance-records/${id}`, { method: 'PUT', body: JSON.stringify(record) })
}
