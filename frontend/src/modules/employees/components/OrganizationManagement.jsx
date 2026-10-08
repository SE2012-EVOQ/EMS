import { useState } from 'react'
import Card from '../../../components/common/Card'
import { employeeService } from '../services/employeeService'

export default function OrganizationManagement({ departments, teams, onSaved }) {
  const [kind, setKind] = useState('department')
  const [name, setName] = useState('')
  const [description, setDescription] = useState('')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  async function submit(e) {
    e.preventDefault(); setBusy(true); setError('')
    try {
      await (kind === 'department' ? employeeService.createDepartment : employeeService.createTeam)({ name: name.trim(), description: description.trim() || null })
      setName(''); setDescription(''); await onSaved()
    } catch (err) { setError(err.message || 'Organization update failed') }
    finally { setBusy(false) }
  }
  return <Card className="mb-5"><details><summary className="cursor-pointer text-sm font-bold">Organization management</summary>
    <div className="my-4 grid gap-4 md:grid-cols-2">
      <div><h3 className="font-semibold">Departments</h3>{departments.length ? <ul>{departments.map(d => <li key={d.id}>{d.name} · {d.employeeCount} active employees</li>)}</ul> : <p>No departments configured.</p>}</div>
      <div><h3 className="font-semibold">Teams / projects</h3>{teams.length ? <ul>{teams.map(t => <li key={t.id}>{t.name}</li>)}</ul> : <p>No teams configured.</p>}</div>
    </div>
    {error && <p role="alert" className="mb-3 text-red-700">{error}</p>}
    <form onSubmit={submit} className="grid gap-3 md:grid-cols-2">
      <label>Create<select className="block w-full rounded-xl border p-2" value={kind} onChange={e => setKind(e.target.value)}><option value="department">Department</option><option value="team">Team / project</option></select></label>
      <label>Name<input required maxLength={100} className="block w-full rounded-xl border p-2" value={name} onChange={e => setName(e.target.value)} /></label>
      <label>Description<input maxLength={255} className="block w-full rounded-xl border p-2" value={description} onChange={e => setDescription(e.target.value)} /></label>
      <button disabled={busy} className="rounded-xl bg-blue-600 p-2 text-white disabled:opacity-50">{busy ? 'Creating…' : `Create ${kind === 'department' ? 'department' : 'team / project'}`}</button>
    </form>
  </details></Card>
}
