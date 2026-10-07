import { useEffect, useState } from 'react'
import { apiRequest } from '../../../services/api'
import { useAuth } from '../../../context/AuthContext'
import { employeeService } from '../../employees/services/employeeService'
import Card from '../../../components/common/Card'
import RequestFeedback from '../../../components/common/RequestFeedback'
import { reportCsv } from '../reportExport'

export default function ModuleReport({ endpoint, title, dates = false, employees = false, status = false }) {
  const { user } = useAuth()
  const chooseEmployee = employees && (title === 'Leave' ? user.role !== 'EMPLOYEE' : user.role === 'MANAGER_ADMIN')
  const [choices, setChoices] = useState([])
  const [choiceError, setChoiceError] = useState('')
  const [filters, setFilters] = useState({ from: '', to: '', employeeId: '', status: '' })
  const [query, setQuery] = useState('')
  const [data, setData] = useState(null)
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(true)
  const [reload, setReload] = useState(0)
  useEffect(() => {
    let active = true; setChoiceError(''); setChoices([])
    if (chooseEmployee) employeeService.getAll().then(rows => { if (active) setChoices(rows) }).catch(err => { if (active) setChoiceError(`Employee filters unavailable: ${err.message}`) })
    return () => { active = false }
  }, [chooseEmployee, reload])
  useEffect(() => {
    let active = true; setLoading(true); setError(''); setData(null)
    apiRequest(`${endpoint}${query}`).then(report => { if (active) setData(report) }).catch(err => { if (active) setError(`${title} report unavailable: ${err.message}`) }).finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [endpoint, title, query, reload])
  function exportCsv() {
    const url = URL.createObjectURL(new Blob([reportCsv(data)], { type: 'text/csv;charset=utf-8' }))
    const link = document.createElement('a'); link.href = url; link.download = `${title.toLowerCase()}-report.csv`; link.click(); URL.revokeObjectURL(url)
  }
  return <>
    <Card className="mb-5"><h2 className="mb-4 font-bold">{title} report</h2>
      <form className="flex flex-wrap items-end gap-3" onSubmit={e => { e.preventDefault(); const params = new URLSearchParams(Object.entries(filters).filter(([,v]) => v)); setQuery(params.size ? `?${params}` : ''); setReload(x => x + 1) }}>
        {dates && <>{['from', 'to'].map(field => <label key={field}>{field === 'from' ? 'From' : 'To'}<input type="date" className="block rounded-xl border p-2" value={filters[field]} min={field === 'to' ? filters.from || undefined : undefined} onChange={e => setFilters({ ...filters, [field]: e.target.value })} /></label>)}</>}
        {chooseEmployee && <label>Employee<select className="block rounded-xl border p-2" disabled={!!choiceError} value={filters.employeeId} onChange={e => setFilters({ ...filters, employeeId: e.target.value })}><option value="">All permitted employees</option>{choices.map(c => <option key={c.id} value={c.id}>{c.fullName}</option>)}</select></label>}
        {status && <label>Status<select className="block rounded-xl border p-2" value={filters.status} onChange={e => setFilters({ ...filters, status: e.target.value })}>{['', 'ACTIVE', 'INACTIVE', 'SUSPENDED', 'ON_LEAVE'].map(s => <option key={s} value={s}>{s || 'All statuses'}</option>)}</select></label>}
        <button className="rounded-xl border px-4 py-2" disabled={loading}>Refresh report</button>
        <button type="button" className="rounded-xl border px-4 py-2" disabled={!data || loading || !!error} onClick={exportCsv}>Export CSV</button>
      </form>
      {data && <p className="mt-3 text-sm text-app-muted">Scope: {data.scope.replaceAll('_', ' ').toLowerCase()}</p>}
    </Card>
    <RequestFeedback error={choiceError} onRetry={() => setReload(x => x + 1)} />
    <RequestFeedback loading={loading} error={error} onRetry={() => setReload(x => x + 1)} />
    {data && !loading && !error && <>
      <div className="mb-5 grid grid-cols-2 gap-3 md:grid-cols-3">{Object.entries(data.summary).map(([label, value]) => <Card key={label}><p className="text-sm text-app-muted">{label}</p><p className="mt-2 text-2xl font-bold">{value}</p></Card>)}</div>
      {data.tables.map(table => <Card key={table.title} className="mb-5"><h3 className="mb-4 font-bold">{table.title}</h3>
        {!table.rows.length ? <p className="text-sm text-app-muted">No records match this view.</p> : <div className="overflow-x-auto"><table className="w-full text-left text-sm"><thead><tr>{table.columns.map(c => <th key={c} className="p-2">{c}</th>)}</tr></thead><tbody>{table.rows.map((row, index) => <tr key={index} className="border-t border-app-border">{row.map((value, col) => <td key={col} className="p-2">{value || '—'}</td>)}</tr>)}</tbody></table></div>}
      </Card>)}
    </>}
  </>
}
