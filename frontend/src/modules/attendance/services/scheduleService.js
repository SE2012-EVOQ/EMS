import { apiRequest } from '../../../services/api'

const query = ({ from, to }) => new URLSearchParams({ from, to }).toString()

export const scheduleService = {
  mine: range => apiRequest(`/schedules/me?${query(range)}`),
  teams: () => apiRequest('/schedules/teams'),
  team: (teamId, range, includeDrafts = true) => apiRequest(`/schedules/teams/${teamId}?${query(range)}&includeDrafts=${includeDrafts}`),
  get: id => apiRequest(`/schedules/${id}`),
  employees: teamId => apiRequest(`/schedules/teams/${teamId}/employees`),
  create: schedule => apiRequest('/schedules', { method: 'POST', body: JSON.stringify(schedule) }),
  update: (id, schedule) => apiRequest(`/schedules/${id}`, { method: 'PUT', body: JSON.stringify(schedule) }),
  publish: id => apiRequest(`/schedules/${id}/publish`, { method: 'POST' }),
  discardDraft: id => apiRequest(`/schedules/${id}`, { method: 'DELETE' })
}
