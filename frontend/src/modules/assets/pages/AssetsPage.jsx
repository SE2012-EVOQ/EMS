import Card from '../../../components/common/Card'
import EmptyState from '../../../components/common/EmptyState'
import PageHeader from '../../../components/common/PageHeader'

export default function AssetsPage() {
  return <>
    <PageHeader title="Assets & equipment" description="Company assets and assignments will appear here when the module is connected." />
    <Card>
      <EmptyState icon="Laptop" title="No asset records available" description="The asset register and assignment actions are not connected yet." />
    </Card>
  </>
}
