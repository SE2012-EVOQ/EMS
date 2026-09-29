import Card from '../../../components/common/Card'
import EmptyState from '../../../components/common/EmptyState'
import PageHeader from '../../../components/common/PageHeader'

export default function LeavePage() {
  return <>
    <PageHeader title="Leave management" description="Leave balances, requests and decisions will appear here when the module is connected." />
    <Card>
      <EmptyState icon="CalendarX" title="No leave records available" description="Leave data and request actions are not connected yet." />
    </Card>
  </>
}
