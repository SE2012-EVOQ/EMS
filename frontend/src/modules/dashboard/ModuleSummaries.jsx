import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { ArrowUpRight, CalendarDays, Laptop, Users } from 'lucide-react'
import { apiRequest } from '../../services/api'
import Card from '../../components/common/Card'
import RequestFeedback from '../../components/common/RequestFeedback'

const modules = [
  { id: 'employee', title: 'Employees', icon: Users, primary: ['Employees in scope'], primaryLabel: 'Total employees' },
  { id: 'leave', title: 'Leave', icon: CalendarDays, primary: ['Pending requests'], primaryLabel: 'Pending requests', context: 'Leave requests' },
  { id: 'asset', title: 'Assets', icon: Laptop, primary: ['Registered assets', 'Currently assigned assets'], primaryLabel: null }
]
const labels = {
  'Active employees': 'Active', 'Inactive employees': 'Inactive',
  'Departments configured': 'Departments', 'Departments represented': 'Departments',
  'Teams / projects configured': 'Teams / projects', 'Teams represented': 'Teams / projects',
  'Registered assets': 'Registered assets', 'Currently assigned assets': 'Assigned to you',
  'Available assets': 'Available', 'Other asset statuses': 'Other statuses',
  'Active assignments': 'Assigned', 'Returned assignments': 'Returned',
  'Approved requests': 'Approved', 'Rejected requests': 'Rejected',
  'Available days (current balances)': 'Available days', 'Used days (current balances)': 'Used days'
}
const scopeLabels = { MINE: 'Your overview', SELF_AND_TEAM: 'You & your team' }
const formatValue = value => typeof value === 'number' ? value.toLocaleString(undefined, { maximumFractionDigits: 2 }) : value

function Summary({ module, reload }) {
  const { id, title, icon: Icon } = module
  const endpoint = `/${id}-reports`
  const [data, setData] = useState(null)
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(true)
  const [retry, setRetry] = useState(0)
  useEffect(() => {
    let active = true
    let requestId = 0
    const refresh = () => {
      const id = ++requestId
      setError(''); setLoading(true)
      apiRequest(endpoint).then(report => { if (active && id === requestId) setData(report) }).catch(err => { if (active && id === requestId) setError(`${title} unavailable: ${err.message}`) }).finally(() => { if (active && id === requestId) setLoading(false) })
    }
    setData(null)
    refresh()
    const timer = window.setInterval(() => { if (!document.hidden) refresh() }, 30000)
    return () => { active = false; window.clearInterval(timer) }
  }, [endpoint, title, reload, retry])
  const entries = Object.entries(data?.summary || {})
  const primaryKey = module.primary.find(key => Object.hasOwn(data?.summary || {}, key)) || entries[0]?.[0]
  const supporting = entries.filter(([key]) => key !== primaryKey && key !== module.context
    && !(primaryKey === 'Currently assigned assets' && key === 'Active assignments'))
  const contextValue = data?.summary?.[module.context]

  return <Card className="flex h-full min-w-0 flex-col" aria-busy={loading}>
    <div className="flex items-center gap-2 text-app-muted"><Icon className="h-4 w-4 shrink-0" /><h2 className="text-sm font-bold txt">{title}</h2></div>
    {scopeLabels[data?.scope] && <p className="mt-1 text-[10px] text-app-muted">{scopeLabels[data.scope]}</p>}
    <div className="mt-5 flex-1">
      <RequestFeedback error={error} loading={loading && !data} onRetry={() => setRetry(x => x + 1)} />
      {data && !error && primaryKey && <>
        <dl className="flex items-end justify-between gap-3">
          <div className="min-w-0"><dt className="text-xs font-medium text-app-muted">{module.primaryLabel || labels[primaryKey] || primaryKey}</dt><dd className="mt-2 text-3xl font-extrabold tracking-tight tabular-nums txt">{formatValue(data.summary[primaryKey])}</dd></div>
          {contextValue !== undefined && <div className="pb-1 text-right"><dt className="text-[10px] text-app-muted">Total requests</dt><dd className="mt-1 text-sm font-bold tabular-nums txt">{formatValue(contextValue)}</dd></div>}
        </dl>
        {supporting.length > 0 && <dl className="mt-5 grid grid-cols-2 gap-x-5 gap-y-4 border-t border-app-border pt-4">{supporting.map(([label, value]) => <div key={label} className="min-w-0"><dt className="text-xs text-app-muted leading-4">{labels[label] || label}</dt><dd className="mt-1 text-lg font-bold tabular-nums txt" title={label}>{formatValue(value)}</dd></div>)}</dl>}
      </>}
    </div>
    <Link to={`/reports?source=${id}`} aria-label={`View ${title.toLowerCase()} report`} className="mt-5 flex items-center justify-between gap-2 border-t border-app-border pt-3 text-xs font-bold text-app-muted hover:text-app-text">View report<ArrowUpRight className="h-4 w-4" /></Link>
  </Card>
}
export default function ModuleSummaries({ reload, role }) {
  return <div className="mt-5 grid gap-4 lg:grid-cols-3">{modules.map(module => <Summary key={module.id} module={role === 'SUPERVISOR' ? { ...module, title: module.id === 'employee' ? 'My team' : module.id === 'asset' ? 'My assets' : module.title } : module} reload={reload} />)}</div>
}
