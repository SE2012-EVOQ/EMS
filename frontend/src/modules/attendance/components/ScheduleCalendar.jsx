import EmptyState from '../../../components/common/EmptyState'
import { fmtShort } from '../../../components/common/date'

const initials = name => (name || '?').split(/\s+/).map(part => part[0]).slice(0, 2).join('').toUpperCase()

export default function ScheduleCalendar({ entries = [], showEmployees = false }) {
  if (!entries.length) return <EmptyState icon="CalendarRange" title="No schedule entries" description="Published shifts will appear here when they are assigned." />

  const dates = [...new Set(entries.map(entry => entry.workDate))].sort()
  const shiftsFor = date => entries.filter(entry => entry.workDate === date).sort((a, b) => a.startTime.localeCompare(b.startTime))

  return <div className="overflow-x-auto">
    <div className="calendar-grid" style={{ gridTemplateColumns: `86px repeat(${dates.length}, minmax(150px, 1fr))`, minWidth: `${Math.max(620, dates.length * 150 + 150)}px` }}>
      <div className="calendar-cell bg-app-subtle subtle text-[10px] font-bold text-app-muted muted flex items-center">DATE</div>
      {dates.map(date => <div key={date} className="calendar-cell bg-app-subtle subtle">
        <div className="text-xs font-extrabold txt">{new Date(`${date}T00:00:00`).toLocaleDateString('en-GB', { weekday: 'short' })}</div>
        <div className="mt-1 text-[10px] text-app-muted muted">{fmtShort(date)}</div>
      </div>)}
      <div className="calendar-cell text-[10px] font-bold text-app-muted muted">SHIFTS</div>
      {dates.map(date => <div key={date} className="calendar-cell space-y-2">
        {shiftsFor(date).map(entry => <div key={entry.id} className="rounded-xl bg-app-blue-bg px-2.5 py-2">
          <div className="text-[11px] font-extrabold txt">{entry.startTime?.slice(0, 5)}–{entry.endTime?.slice(0, 5)}</div>
          {showEmployees && <div className="mt-1 text-[10px] font-semibold text-app-muted">{entry.employeeName}</div>}
          {entry.notes && <div className="mt-1 text-[10px] text-app-muted">{entry.notes}</div>}
        </div>)}
        <div className="text-[10px] text-app-muted">{shiftsFor(date).length} {shiftsFor(date).length === 1 ? 'shift' : 'shifts'}</div>
      </div>)}
      {showEmployees && <><div className="calendar-cell text-[10px] font-bold text-app-muted muted">TEAM COVERAGE</div>
        {dates.map(date => { const people = [...new Map(shiftsFor(date).map(entry => [entry.employeeId, entry.employeeName])).entries()]; return <div key={date} className="calendar-cell"><div className="flex -space-x-2">{people.slice(0, 6).map(([id, name]) => <div key={id} title={name} className="flex h-7 w-7 items-center justify-center rounded-full border-2 border-white bg-[#DEE8FF] avatar-soft text-[8px] font-extrabold">{initials(name)}</div>)}</div><div className="mt-3 text-[10px] text-app-muted">{people.length} scheduled</div></div> })}
      </>}
    </div>
  </div>
}
