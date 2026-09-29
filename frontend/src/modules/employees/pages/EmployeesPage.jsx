import Card from '../../../components/common/Card'
import EmptyState from '../../../components/common/EmptyState'
import PageHeader from '../../../components/common/PageHeader'

export default function EmployeesPage() {
  return <>
    <PageHeader title="Employee directory" description="Employee and organization records will appear here when the module is connected." />
    <Card>
      <EmptyState icon="Users" title="No employee records available" description="Employee onboarding and directory data are not connected yet." />
    </Card>
  </>
}
