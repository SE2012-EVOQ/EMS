import Card from '../../../components/common/Card'
import EmptyState from '../../../components/common/EmptyState'
import MetricCard from '../../../components/common/MetricCard'

const metrics = [
  ['Active employees', 'Users'],
  ['Pending leave', 'CalendarClock'],
  ['Present today', 'BadgeCheck'],
  ['Assigned assets', 'Laptop']
]

export default function DashboardPage() {
  return <>
    <div className="grid grid-cols-2 xl:grid-cols-4 gap-3 sm:gap-4 mb-6">
      {metrics.map(([label, icon]) => <MetricCard key={label} label={label} value="—" icon={icon} />)}
    </div>
    <Card>
      <EmptyState icon="LayoutDashboard" title="No dashboard data available" description="Business module data is not connected yet. Metrics will appear when the modules are implemented." />
    </Card>
  </>
}
