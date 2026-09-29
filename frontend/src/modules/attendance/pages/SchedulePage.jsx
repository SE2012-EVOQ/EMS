import Card from '../../../components/common/Card'
import PageHeader from '../../../components/common/PageHeader'
import ScheduleCalendar from '../components/ScheduleCalendar'

export default function SchedulePage() {
  return <>
    <PageHeader title="Schedule" description="Working schedules will appear here when the module is connected." />
    <Card>
      <ScheduleCalendar entries={[]} />
    </Card>
  </>
}
