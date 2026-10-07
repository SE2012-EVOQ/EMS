import { useState } from 'react'
import Card from '../../../components/common/Card'
import EmptyState from '../../../components/common/EmptyState'
import PageHeader from '../../../components/common/PageHeader'
import { reportSources } from '../reportSources'

export default function ReportsPage() {
  const [selected, setSelected] = useState('attendance')
  const source = reportSources.find(item => item.id === selected)
  const Component = source.Component
  return <>
    <PageHeader primary title="Reports" />
    <div className="mb-5 flex gap-2 overflow-x-auto" role="tablist" aria-label="Report sources">{reportSources.map(item => <button key={item.id} type="button" role="tab" aria-selected={selected === item.id} onClick={() => setSelected(item.id)} className={`rounded-full px-4 py-2.5 text-xs font-bold ${selected === item.id ? 'bg-[#1A1D1F] text-white' : 'surface border border-app-border'}`}>{item.label}</button>)}</div>
    <div role="tabpanel" aria-label={`${source.label} report`}>{Component ? <Component /> : <Card><EmptyState icon="BarChart3" title={`${source.label} report unavailable`} description="This report is not available in this version." /></Card>}</div>
  </>
}
