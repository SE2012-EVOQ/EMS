export function requestErrorMessage(error, subject) {
  if (error instanceof TypeError || /Failed to fetch|NetworkError|Load failed/i.test(error?.message || '')) {
    return `Cannot connect. ${subject} is unavailable. Check your connection and try again.`
  }
  if (error?.status >= 500) return `${subject} is temporarily unavailable. Please try again.`
  return error?.message || `${subject} could not be loaded. Please try again.`
}
