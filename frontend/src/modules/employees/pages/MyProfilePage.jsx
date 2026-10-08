import { useEffect, useState } from 'react'
import { Edit3 } from 'lucide-react'
import { useAuth } from '../../../context/AuthContext'
import Card from '../../../components/common/Card'
import PageHeader from '../../../components/common/PageHeader'
import RequestFeedback from '../../../components/common/RequestFeedback'
import StatusBadge from '../../../components/common/StatusBadge'
import { fmtDate } from '../../../components/common/date'
import { requestErrorMessage } from '../../../components/common/requestError'
import EditContactModal from '../components/EditContactModal'
import { employeeService } from '../services/employeeService'

const roleLabels = { MANAGER_ADMIN: 'Manager / Admin', SUPERVISOR: 'Supervisor', EMPLOYEE: 'Employee' }

function Detail({ label, value }) {
  return <div className="min-w-0"><dt className="text-xs text-app-muted">{label}</dt><dd className="mt-1.5 text-sm font-semibold break-words whitespace-pre-line">{value || '—'}</dd></div>
}

export function ProfileDetails({ employee, user, onEditContact }) {
  const initials = [employee.firstName, employee.lastName].map(name => name?.trim()?.[0] || '').join('').toUpperCase() || '?'
  const ownProfile = employee.id === user?.employeeId
  const employment = [
    ['First name', employee.firstName], ['Last name', employee.lastName], ['Work email', employee.email],
    ['Department', employee.department?.name], ['Team / project', employee.team?.name],
    ['Supervisor', employee.supervisor?.fullName], ['Hire date', employee.hireDate ? fmtDate(employee.hireDate) : null]
  ]
  return <>
    <Card className="mb-5 flex flex-wrap items-center gap-4">
      <div className="h-14 w-14 shrink-0 rounded-full bg-[#DEE8FF] avatar-soft text-[#3B5BDB] flex items-center justify-center text-xl font-extrabold">{initials}</div>
      <div className="min-w-0 flex-1"><h2 className="text-xl font-extrabold break-words">{employee.fullName}</h2><p className="mt-1 text-sm text-app-muted">{employee.jobTitle || '—'}</p><p className="mt-2 text-xs font-semibold text-app-muted">E{String(employee.id).padStart(3, '0')}</p></div>
      <StatusBadge status={employee.status} />
    </Card>
    <div className="grid gap-5 lg:grid-cols-3">
      <Card className="lg:col-span-2"><h2 className="mb-5 text-sm font-bold">Employment</h2><dl className="grid gap-x-6 gap-y-5 sm:grid-cols-2">{employment.map(([label, value]) => <Detail key={label} label={label} value={value} />)}</dl></Card>
      <Card>
        <div className="mb-5 flex items-center justify-between gap-3"><h2 className="text-sm font-bold">Contact</h2>{ownProfile && <button type="button" onClick={onEditContact} className="inline-flex items-center gap-1.5 rounded-full border border-app-border px-3 py-2 text-xs font-bold hover:bg-app-subtle"><Edit3 className="h-3.5 w-3.5" />Edit contact</button>}</div>
        <dl className="space-y-5"><Detail label="Phone number" value={employee.phone} /><Detail label="Home address" value={employee.address} /></dl>
        <h2 className="mt-6 border-t border-app-border pt-5 text-sm font-bold">Account</h2>
        <dl className="mt-4 space-y-5"><Detail label="Username" value={user?.username} /><Detail label="Role" value={roleLabels[employee.account?.role || user?.role]} />{employee.account && <Detail label="Account status" value={employee.account.active ? 'Active' : 'Inactive'} />}</dl>
      </Card>
    </div>
  </>
}

export default function MyProfilePage() {
  const { user } = useAuth()
  const [employee, setEmployee] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [reload, setReload] = useState(0)
  const [editing, setEditing] = useState(false)
  const [message, setMessage] = useState('')

  useEffect(() => {
    let active = true
    setLoading(true); setError(''); setEmployee(null)
    employeeService.getMe().then(profile => { if (active) setEmployee(profile) })
      .catch(err => { if (active) setError(requestErrorMessage(err, 'Profile')) })
      .finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [user?.employeeId, reload])

  async function saveContact(id, payload) {
    if (id !== user.employeeId || id !== employee.id) throw new Error('You can only update your own contact details.')
    const updated = await employeeService.updateContact(user.employeeId, payload)
    setEmployee(updated)
    setMessage('Contact details updated.')
  }

  return <>
    <PageHeader primary title="My profile" />
    <RequestFeedback loading={loading} error={error} loadingText="Loading your profile…" onRetry={() => setReload(value => value + 1)} />
    {message && <p role="status" className="mb-4 text-xs font-semibold text-app-green">{message}</p>}
    {!loading && !error && employee && <ProfileDetails employee={employee} user={user} onEditContact={() => { setMessage(''); setEditing(true) }} />}
    <EditContactModal key={editing ? 'contact-open' : 'contact-closed'} open={editing} employee={employee} onClose={() => setEditing(false)} onSubmit={saveContact} />
  </>
}
