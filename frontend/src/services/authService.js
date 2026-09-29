import { apiRequest, clearCsrfToken } from './api'

export const authService = {
  async login({ username, password }) {
    const body = new URLSearchParams({ username, password })
    const user = await apiRequest('/auth/login', { method: 'POST', body })
    clearCsrfToken()
    return user
  },
  me: () => apiRequest('/auth/me'),
  async logout() {
    await apiRequest('/auth/logout', { method: 'POST' })
    clearCsrfToken()
  },
  changePassword: passwords => apiRequest('/auth/change-password', {
    method: 'POST', body: JSON.stringify(passwords)
  })
}
