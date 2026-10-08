import { Pencil } from 'lucide-react'
import EmptyState from '../../../components/common/EmptyState'
import StatusBadge from '../../../components/common/StatusBadge'
import { fmtDate } from '../../../components/common/date'

const displayTime = value => value ? value.slice(0, 5) : '—'
const displayStatus = value => value ? value[0] + value.slice(1).toLowerCase() : '—'

export default function AttendanceTable({ rows = [], canEdit = false, onEdit }) {
  if (!rows.length) return <EmptyState icon="ClipboardList" title="No attendance records" description="Try another date range or status." />

  return <><div className="hidden overflow-x-auto md:block">
    <table className="w-full min-w-[720px]">
      <thead>
        <tr className="text-left text-[10px] uppercase tracking-wider text-app-muted muted">
          <th className="pb-3">Employee</th><th className="pb-3">Date</th><th className="pb-3">Status</th>
          <th className="pb-3">In</th><th className="pb-3">Out</th><th className="pb-3">Hours</th>
          <th className="pb-3">Note</th>{canEdit && <th />}
        </tr>
      </thead>
      <tbody>{rows.map(record => <tr key={record.id} className="border-t border-app-border">
        <td className="py-3 text-xs font-bold txt">{record.employeeName}</td>
        <td className="py-3 text-xs text-app-muted muted">{fmtDate(record.date)}</td>
        <td className="py-3"><StatusBadge status={displayStatus(record.status)} /></td>
        <td className="py-3 text-xs font-semibold txt">{displayTime(record.checkIn)}</td>
        <td className="py-3 text-xs font-semibold txt">{displayTime(record.checkOut)}</td>
        <td className="py-3 text-xs font-semibold txt">{Number(record.hours).toFixed(2)}</td>
        <td className="py-3 text-[10px] text-app-muted muted">{record.note || '—'}</td>
        {canEdit && <td className="py-3 text-right"><button type="button" onClick={() => onEdit?.(record)} className="w-8 h-8 rounded-full hover:bg-app-subtle" aria-label={`Correct attendance for ${record.employeeName} on ${record.date}`}><Pencil className="w-3.5 h-3.5 mx-auto" /></button></td>}
      </tr>)}</tbody>
    </table>
  </div><div className="space-y-3 md:hidden">{rows.map(record => <div key={record.id} className="rounded-2xl border border-app-border p-4"><div className="flex items-start justify-between gap-3"><div><div className="text-xs font-extrabold txt">{record.employeeName}</div><div className="mt-1 text-xs text-app-muted muted">{fmtDate(record.date)}</div></div><StatusBadge status={displayStatus(record.status)} /></div><div className="mt-4 grid grid-cols-3 gap-2 text-xs"><div><span className="text-app-muted muted">In</span><div className="mt-1 font-bold txt">{displayTime(record.checkIn)}</div></div><div><span className="text-app-muted muted">Out</span><div className="mt-1 font-bold txt">{displayTime(record.checkOut)}</div></div><div><span className="text-app-muted muted">Hours</span><div className="mt-1 font-bold txt">{Number(record.hours).toFixed(2)}</div></div></div>{record.note && <p className="mt-3 text-xs text-app-muted muted">{record.note}</p>}{canEdit && <button type="button" onClick={() => onEdit?.(record)} className="mt-3 inline-flex items-center gap-2 text-xs font-bold txt"><Pencil className="h-3.5 w-3.5" />Correct record</button>}</div>)}</div></>
}
