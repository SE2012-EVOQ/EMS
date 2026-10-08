const styles = {
  Suspended:'bg-app-amber-bg text-app-amber', 'On Leave':'bg-app-blue-bg text-app-blue',
  Approved:'bg-app-green-bg text-app-green', Active:'bg-app-green-bg text-app-green', Present:'bg-app-green-bg text-app-green',
  Leave:'bg-app-blue-bg text-app-blue', Available:'bg-app-green-bg text-app-green', Assigned:'bg-app-blue-bg text-app-blue',
  Pending:'bg-app-amber-bg text-app-amber', Late:'bg-app-amber-bg text-app-amber', Maintenance:'bg-app-amber-bg text-app-amber',
  Rejected:'bg-app-pink-bg text-app-pink', Absent:'bg-app-pink-bg text-app-pink', Damaged:'bg-app-pink-bg text-app-pink',
  Lost:'bg-app-pink-bg text-app-pink', Retired:'bg-gray-100 text-app-muted', Inactive:'bg-gray-100 text-app-muted',
  Remote:'bg-app-blue-bg text-app-blue', Office:'bg-app-green-bg text-app-green', Lab:'bg-app-green-bg text-app-green',
  Field:'bg-app-amber-bg text-app-amber', Flexible:'bg-[#F2EEFF] text-[#7A5AE8] status-flex', Returned:'bg-gray-100 text-app-muted'
}

export default function StatusBadge({ status }) {
  const label = status && status === status.toUpperCase()
    ? status.toLowerCase().replaceAll('_', ' ').replace(/\b\w/g, letter => letter.toUpperCase()) : status
  return <span title={status} className={`inline-flex whitespace-nowrap px-2.5 py-1 rounded-full text-[10px] font-bold ${styles[label] || 'bg-gray-100 text-app-muted'}`}>{label}</span>
}
