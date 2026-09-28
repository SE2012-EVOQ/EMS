import * as Icons from 'lucide-react'

export default function EmptyState({ icon = 'Inbox', title, description }) {
  const Icon = Icons[icon] || Icons.Inbox
  return <div className="py-8 text-center"><div className="w-11 h-11 rounded-full bg-app-subtle subtle flex items-center justify-center mx-auto"><Icon className="w-4 h-4" /></div><div className="text-xs font-bold mt-3 txt">{title}</div><div className="text-[10px] text-app-muted muted mt-1">{description}</div></div>
}
