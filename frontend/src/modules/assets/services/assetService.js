import { apiRequest } from '../../../services/api'
export const assetService = { list: () => apiRequest('/assets'), create: data => apiRequest('/assets',{method:'POST',body:JSON.stringify(data)}) }
