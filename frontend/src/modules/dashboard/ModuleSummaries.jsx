import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { apiRequest } from '../../services/api'
import Card from '../../components/common/Card'
import RequestFeedback from '../../components/common/RequestFeedback'

function Summary({ title, endpoint, reload }) {
  const [data, setData] = useState(null)
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(true)
  const [retry, setRetry] = useState(0)
  useEffect(() => {
    let active = true
    let requestId = 0
    const refresh = () => {
      const id = ++requestId
      setData(null); setError(''); setLoading(true)
      apiRequest(endpoint).then(report => { if (active && id === requestId) setData(report) }).catch(err => { if (active && id === requestId) setError(`${title} summary unavailable: ${err.message}`) }).finally(() => { if (active && id === requestId) setLoading(false) })
    }
    refresh()
    const timer = window.setInterval(() => { if (!document.hidden) refresh() }, 30000)
    return () => { active = false; window.clearInterval(timer) }
  }, [endpoint, title, reload, retry])
  return <Card className="mt-5"><h2 className="mb-3 text-sm font-bold">{title} summary</h2>
    <RequestFeedback error={error} loading={loading} onRetry={() => setRetry(x => x + 1)} />
    {data && !loading && !error && <><p className="mb-3 text-xs text-app-muted">Scope: {data.scope.replaceAll('_', ' ').toLowerCase()}</p><dl className="grid grid-cols-2 gap-3">{Object.entries(data.summary).map(([label, value]) => <div key={label}><dt className="text-xs text-app-muted">{label}</dt><dd className="text-xl font-bold">{value}</dd></div>)}</dl></>}
    <Link to="/reports" className="mt-4 inline-block text-xs font-bold underline">Open reports</Link>
  </Card>
}
export default function ModuleSummaries({ reload }) {
  return <div className="grid gap-4 md:grid-cols-3"><Summary title="Employee / organization" endpoint="/employee-reports" reload={reload} /><Summary title="Leave" endpoint="/leave-reports" reload={reload} /><Summary title="Asset" endpoint="/asset-reports" reload={reload} /></div>
}
