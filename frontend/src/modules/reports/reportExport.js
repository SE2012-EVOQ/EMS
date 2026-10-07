export function reportCsv(report) {
  const cell = value => {
    const text = String(value ?? '')
    const safe = /^[\s\u0000-\u001f]*[=+\-@]/.test(text) ? `'${text}` : text
    return `"${safe.replaceAll('"', '""')}"`
  }
  const rows = [['Scope', report.scope], ['Metric', 'Value'], ...Object.entries(report.summary)]
  for (const table of report.tables) rows.push([], [table.title], table.columns, ...table.rows)
  return rows.map(row => row.map(cell).join(',')).join('\r\n') + '\r\n'
}
