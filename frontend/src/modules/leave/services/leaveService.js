import { apiRequest } from '../../../services/api'

export function getMyLeave() {
  return apiRequest('/leave/me')
}

export function getLeaveTypes() {
  return apiRequest('/leave/types')
}

export function submitLeaveRequest(data) {
  return apiRequest('/leave/requests', {
    method: 'POST',
    body: JSON.stringify(data)
  })
}

export function getPendingLeaveRequests() {
  return apiRequest('/leave/supervisor/pending')
}

export function getAllLeaveRequests() {
  return apiRequest('/leave/all')
}

export function approveLeaveRequest(id) {
  return apiRequest(`/leave/requests/${id}/approve`, {
    method: 'POST'
  })
}

export function rejectLeaveRequest(id) {
  return apiRequest(`/leave/requests/${id}/reject`, {
    method: 'POST'
  })
}
