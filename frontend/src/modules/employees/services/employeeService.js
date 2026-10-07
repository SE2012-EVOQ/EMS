import { apiRequest } from '../../../services/api'

const buildQuery = (params = {}) => {
  const searchParams = new URLSearchParams()
  if (params.departmentId) searchParams.set('departmentId', params.departmentId)
  if (params.teamId) searchParams.set('teamId', params.teamId)
  if (params.status) searchParams.set('status', params.status)
  if (params.search) searchParams.set('search', params.search)
  const query = searchParams.toString()
  return query ? `?${query}` : ''
}

export const employeeService = {
  getAll: (params) => apiRequest(`/employees${buildQuery(params)}`),
  getSupervisorCandidates: () => apiRequest('/employees/supervisor-candidates'),
  getMe: () => apiRequest('/employees/me'),
  getById: (id) => apiRequest(`/employees/${id}`),
  getDirectReports: (id) => apiRequest(`/employees/${id}/direct-reports`),
  create: (data) => apiRequest('/employees', { method: 'POST', body: JSON.stringify(data) }),
  updateOfficial: (id, data) => apiRequest(`/employees/${id}/official`, { method: 'PUT', body: JSON.stringify(data) }),
  updateContact: (id, data) => apiRequest(`/employees/${id}/contact`, { method: 'PUT', body: JSON.stringify(data) }),
  changeStatus: (id, status) => apiRequest(`/employees/${id}/status`, { method: 'PATCH', body: JSON.stringify({ status }) }),

  // Organization lookups
  getDepartments: () => apiRequest('/organization/departments'),
  createDepartment: (data) => apiRequest('/organization/departments', { method: 'POST', body: JSON.stringify(data) }),
  getTeams: () => apiRequest('/organization/teams'),
  createTeam: (data) => apiRequest('/organization/teams', { method: 'POST', body: JSON.stringify(data) })
}
