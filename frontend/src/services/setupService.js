import { apiRequest } from './api.js'

export const setupService = {
  status: () => apiRequest('/auth/setup'),
  create: details => apiRequest('/auth/setup', { method: 'POST', body: JSON.stringify(details) })
}

// A failed status read is an unavailable installation, never an assumed empty one.
export async function loadEntryState(me) {
  const { required } = await setupService.status()
  if (typeof required !== 'boolean') throw new Error('Could not read installation setup status. Try again.')
  if (required) return { setupRequired: true, user: null }
  try {
    return { setupRequired: false, user: await me() }
  } catch (error) {
    if (error.status === 401) return { setupRequired: false, user: null }
    throw error
  }
}
