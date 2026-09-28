import * as Icons from 'lucide-react'

export default function MetricCard({ label, value, icon = 'Circle', delta, positive = true }) {
  const Icon = Icons[icon] || Icons.Circle
  return (
    <div className="surface bg-white rounded-[26px] p-5 border border-app-border/50 shadow-card min-w-0">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-2 text-xs font-semibold text-app-muted muted"><Icon className="w-4 h-4" />{label}</div>
        {delta && <span className={`text-[10px] font-bold ${positive ? 'text-app-green bg-app-green-bg' : 'text-app-pink bg-app-pink-bg'} px-2 py-1 rounded-full`}>{delta}</span>}
      </div>
      <div className="text-3xl font-extrabold tracking-tight mt-4 txt">{value}</div>
    </div>
  )
}
