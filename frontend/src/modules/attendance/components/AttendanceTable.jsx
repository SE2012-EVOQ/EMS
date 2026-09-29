import { Pencil } from 'lucide-react'
import EmptyState from '../../../components/common/EmptyState'
import StatusBadge from '../../../components/common/StatusBadge'
import { fmtDate } from '../../../components/common/date'

export default function AttendanceTable({ rows = [], employeeById, canEdit = false, onEdit }) {
  if (!rows.length) return <EmptyState icon="ClipboardList" title="No attendance records available" description="Attendance data and corrections are not connected yet." />

  return <div className="overflow-x-auto">
    <table className="w-full min-w-[720px]">
      <thead>
        <tr className="text-left text-[10px] uppercase tracking-wider text-app-muted muted">
          <th className="pb-3">Employee</th><th className="pb-3">Date</th><th className="pb-3">Status</th>
          <th className="pb-3">In</th><th className="pb-3">Out</th><th className="pb-3">Hours</th>
          <th className="pb-3">Note</th>{canEdit && <th />}
        </tr>
      </thead>
      <tbody>{rows.map(record => <tr key={record.id} className="border-t border-app-border">
        <td className="py-3 text-xs font-bold txt">{employeeById?.(record.employee)?.name || '—'}</td>
        <td className="py-3 text-xs text-app-muted muted">{fmtDate(record.date)}</td>
        <td className="py-3"><StatusBadge status={record.status} /></td>
        <td className="py-3 text-xs font-semibold txt">{record.checkIn || '—'}</td>
        <td className="py-3 text-xs font-semibold txt">{record.checkOut || '—'}</td>
        <td className="py-3 text-xs font-semibold txt">{record.hours ?? '—'}</td>
        <td className="py-3 text-[10px] text-app-muted muted">{record.note || '—'}</td>
        {canEdit && <td className="py-3 text-right"><button onClick={() => onEdit?.(record)} className="w-8 h-8 rounded-full hover:bg-app-subtle" aria-label="Edit attendance"><Pencil className="w-3.5 h-3.5 mx-auto" /></button></td>}
      </tr>)}</tbody>
    </table>
  </div>
}
