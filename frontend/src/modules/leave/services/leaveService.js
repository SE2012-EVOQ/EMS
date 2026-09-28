import { apiRequest } from '../../../services/api'
export const leaveService = { list: () => apiRequest('/leave-requests'), create: data => apiRequest('/leave-requests',{method:'POST',body:JSON.stringify(data)}) }
