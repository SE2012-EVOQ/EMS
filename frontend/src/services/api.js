const API_BASE_URL = import.meta.env?.VITE_API_BASE_URL || 'http://localhost:8080/api'

let csrfToken = null
const expiryListeners = new Set()
export function onSessionExpired(listener) {
  expiryListeners.add(listener)
  return () => expiryListeners.delete(listener)
}

export function clearCsrfToken() {
  csrfToken = null
}

async function getCsrfToken() {
  if (csrfToken) return csrfToken
  const response = await fetch(`${API_BASE_URL}/auth/csrf`, { credentials: 'include' })
  if (!response.ok) {
    const error = new Error('Could not start a secure session. Try again.')
    error.status = response.status
    if (response.status === 401) { clearCsrfToken(); expiryListeners.forEach(listener => listener()) }
    throw error
  }
  csrfToken = await response.json()
  return csrfToken
}

export async function apiRequest(path, options = {}) {
  const method = (options.method || 'GET').toUpperCase()
  const headers = new Headers(options.headers || {})
  if (options.body && !(options.body instanceof URLSearchParams) && !(options.body instanceof FormData)) {
    headers.set('Content-Type', 'application/json')
  }
  if (!['GET', 'HEAD', 'OPTIONS'].includes(method)) {
    const csrf = await getCsrfToken()
    headers.set(csrf.headerName, csrf.token)
  }

  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...options,
    method,
    headers,
    credentials: 'include'
  })
  if (!response.ok) {
    if (response.status === 401) {
      clearCsrfToken()
      if (path !== '/auth/login') expiryListeners.forEach(listener => listener())
    }
    if (response.status === 403) clearCsrfToken()
    const errorBody = await response.json().catch(() => null)
    const error = new Error(errorBody?.message || `API request failed: ${response.status}`)
    error.status = response.status
    error.fieldErrors = errorBody?.fieldErrors || {}
    throw error
  }
  if (response.status === 204) return null
  return options.responseType === 'blob' ? response.blob() : response.json()
}
