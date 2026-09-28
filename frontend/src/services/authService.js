import { apiRequest } from './api'

export const authService = {
  login: credentials => apiRequest('/auth/login', { method: 'POST', body: JSON.stringify(credentials) }),
  me: () => apiRequest('/auth/me')
}
