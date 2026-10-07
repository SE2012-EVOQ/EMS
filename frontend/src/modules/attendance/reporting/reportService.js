import { apiRequest } from '../../../services/api'

const query = filters => new URLSearchParams(Object.entries(filters).filter(([, value]) => value !== '' && value != null)).toString()
export const reportService = {
  options: filters => apiRequest(`/attendance-reports/options?${query(filters)}`),
  report: filters => apiRequest(`/attendance-reports?${query(filters)}`),
  dashboard: filters => apiRequest(`/attendance-reports/dashboard?${query(filters)}`),
  csv: filters => apiRequest(`/attendance-reports/csv?${query(filters)}`, { responseType: 'blob' })
}
