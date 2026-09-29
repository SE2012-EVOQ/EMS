import Card from '../../../components/common/Card'
import PageHeader from '../../../components/common/PageHeader'
import AttendanceTable from '../components/AttendanceTable'

export default function AttendancePage() {
  return <>
    <PageHeader title="Attendance records" description="Attendance history and corrections will appear here when the module is connected." />
    <Card>
      <AttendanceTable rows={[]} />
    </Card>
  </>
}
