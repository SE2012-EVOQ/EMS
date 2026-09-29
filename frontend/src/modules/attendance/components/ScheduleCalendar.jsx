import EmptyState from '../../../components/common/EmptyState'
import { fmtShort } from '../../../components/common/date'

export default function ScheduleCalendar({ entries = [], employeeById }) {
  if (!entries.length) return <EmptyState icon="CalendarRange" title="No schedule data available" description="Schedules are not connected yet." />

  const dates = [...new Set(entries.map(entry => entry.date))].sort()
  const coverage = date => entries.filter(entry => entry.date === date)

  return <div className="overflow-x-auto">
    <div className="calendar-grid">
      <div className="calendar-cell bg-app-subtle subtle text-[10px] font-bold text-app-muted muted flex items-center">DATES</div>
      {dates.map(date => <div key={date} className="calendar-cell bg-app-subtle subtle">
        <div className="text-xs font-extrabold txt">{new Date(date + 'T00:00').toLocaleDateString('en-GB', { weekday: 'short' })}</div>
        <div className="text-[10px] text-app-muted muted mt-1">{fmtShort(date)}</div>
      </div>)}
      <div className="calendar-cell text-[10px] font-bold text-app-muted muted">Coverage</div>
      {dates.map(date => {
        const day = coverage(date)
        return <div key={date} className="calendar-cell">
          <div className="text-xs font-extrabold txt">{day.length} {day.length === 1 ? 'entry' : 'entries'}</div>
          <div className="text-[10px] text-app-muted muted mt-2">{day.map(entry => <div key={entry.id}>{entry.start}–{entry.end} · {entry.mode}</div>)}</div>
        </div>
      })}
      <div className="calendar-cell text-[10px] font-bold text-app-muted muted">People</div>
      {dates.map(date => {
        const ids = [...new Set(coverage(date).map(entry => entry.employee))]
        return <div key={date} className="calendar-cell">
          <div className="flex -space-x-2 mt-1">{ids.slice(0, 6).map(id => {
            const employee = employeeById?.(id)
            return <div key={id} title={employee?.name || String(id)} className="w-7 h-7 rounded-full bg-[#DEE8FF] avatar-soft border-2 border-white flex items-center justify-center text-[8px] font-extrabold">{employee?.avatar || '?'}</div>
          })}</div>
          <div className="text-[10px] text-app-muted muted mt-3">{ids.length} scheduled</div>
        </div>
      })}
    </div>
  </div>
}
