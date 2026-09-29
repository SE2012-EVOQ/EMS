import Card from '../../../components/common/Card'
import EmptyState from '../../../components/common/EmptyState'
import PageHeader from '../../../components/common/PageHeader'

export default function ReportsPage() {
  return <>
    <PageHeader title="Reports" description="Reports will use real employee, leave, attendance and asset data when those modules are connected." />
    <Card>
      <EmptyState icon="BarChart3" title="No report data available" description="There are no connected report sources or exports yet." />
    </Card>
  </>
}
