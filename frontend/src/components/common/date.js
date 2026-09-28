export const fmtDate = value => new Date(`${value}T00:00:00`).toLocaleDateString('en-GB', { day: '2-digit', month: 'short', year: 'numeric' })
export const fmtShort = value => new Date(`${value}T00:00:00`).toLocaleDateString('en-GB', { day: '2-digit', month: 'short' })
