import { apiRequest } from '../../../services/api'

export function getAllAssets() {
  return apiRequest('/assets')
}

export function getAssetById(assetId) {
  return apiRequest(`/assets/${assetId}`)
}

export function registerAsset(asset) {
  return apiRequest('/assets', {
    method: 'POST',
    body: JSON.stringify(asset)
  })
}

export function updateAsset(assetId, asset) {
  return apiRequest(`/assets/${assetId}`, {
    method: 'PUT',
    body: JSON.stringify(asset)
  })
}

export function updateAssetStatus(assetId, status) {
  return apiRequest(
    `/assets/${assetId}/status?status=${encodeURIComponent(status)}`,
    {
      method: 'PATCH'
    }
  )
}

export function assignAsset(assetId, employeeId) {
  return apiRequest(
    `/asset-assignments/assign?assetId=${assetId}&employeeId=${employeeId}`,
    {
      method: 'POST'
    }
  )
}

export function returnAsset(assignmentId) {
  return apiRequest(`/asset-assignments/${assignmentId}/return`, {
    method: 'PUT'
  })
}

export function getAllAssignments() {
  return apiRequest('/asset-assignments')
}

export function getAssignmentsByEmployee(employeeId) {
  return apiRequest(`/asset-assignments/employee/${employeeId}`)
}

export function getAssetHistory(assetId) {
  return apiRequest(`/asset-assignments/asset/${assetId}`)
}