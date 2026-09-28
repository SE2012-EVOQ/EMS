import { apiRequest } from '../../../services/api'
export const employeeService = { list: () => apiRequest('/employees'), get: id => apiRequest(`/employees/${id}`) }
