import { BarChart3, CalendarRange, ClipboardList, Inbox, Laptop, Users } from 'lucide-react'

const icons = { BarChart3, CalendarRange, ClipboardList, Inbox, Laptop, Users }

export default function EmptyState({ icon = 'Inbox', title, description }) {
  const Icon = icons[icon] || Inbox
  return <div className="py-8 text-center"><div className="w-11 h-11 rounded-full bg-app-subtle subtle flex items-center justify-center mx-auto"><Icon className="w-4 h-4" /></div><div className="text-xs font-bold mt-3 txt">{title}</div>{description && <p className="text-xs text-app-muted muted mt-1 max-w-sm mx-auto leading-relaxed">{description}</p>}</div>
}
