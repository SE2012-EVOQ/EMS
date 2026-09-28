import { Plus } from 'lucide-react'
import { useEffect, useMemo, useState } from 'react'
import Card from '../../../components/common/Card'
import FilterPills from '../../../components/common/FilterPills'
import MetricCard from '../../../components/common/MetricCard'
import Modal from '../../../components/common/Modal'
import PageHeader from '../../../components/common/PageHeader'
import { FormField, SelectField } from '../../../components/common/FormField'
import { useEms } from '../../../context/EmsContext'
import AttendanceTable from '../components/AttendanceTable'

export default function AttendancePage() {
  const { db, role, allowedAttendance, allowedEmployees, employeeById, saveAttendance }=useEms()
  const [filter,setFilter]=useState('All')
  const [editing,setEditing]=useState(null)
  const rows=useMemo(()=>allowedAttendance().filter(x=>filter==='All'||x.status===filter).sort((a,b)=>b.date.localeCompare(a.date)),[db,role,filter])
  const avg=rows.length?(rows.reduce((s,x)=>s+x.hours,0)/rows.length).toFixed(1):0
  return <><PageHeader title="Attendance records" description={role==='admin'?'Manage attendance notes and corrections.':role==='supervisor'?'Attendance information for your team.':'Your recorded attendance history.'} actions={role==='admin'&&<button onClick={()=>setEditing({employee:allowedEmployees()[0]?.id||'',date:'2026-09-28',status:'Present',checkIn:'09:00',checkOut:'17:00',hours:8,note:''})} className="bg-[#1A1D1F] dark-primary text-white rounded-full px-4 py-2.5 text-xs font-bold flex items-center gap-2"><Plus className="w-4 h-4"/>Add / correct record</button>}/><div className="grid grid-cols-3 gap-3 mb-6"><MetricCard label="Records" value={rows.length} icon="ClipboardList"/><MetricCard label="Avg. hours" value={avg} icon="Clock3"/><MetricCard label="Late / absent" value={rows.filter(x=>['Late','Absent'].includes(x.status)).length} icon="CircleAlert"/></div><FilterPills items={['All','Present','Late','Absent','Leave']} value={filter} onChange={setFilter}/><Card><AttendanceTable rows={rows} employeeById={employeeById} canEdit={role==='admin'} onEdit={setEditing}/></Card><AttendanceModal record={editing} employees={allowedEmployees()} open={!!editing} onClose={()=>setEditing(null)} onSave={r=>{saveAttendance(r);setEditing(null)}}/></>
}

function AttendanceModal({record,employees,open,onClose,onSave}) {
  const [form,setForm]=useState(record||{})
  useEffect(()=>setForm(record||{}),[record])
  if(!record)return null
  const set=(k,v)=>setForm(f=>({...f,[k]:v}))
  return <Modal open={open} onClose={onClose} title={record.id?'Correct attendance':'Add attendance'} subtitle="Record attendance status, times, working hours and notes." footer={<><button onClick={onClose} className="px-4 py-2.5 rounded-full border border-gray-200 text-xs font-bold">Cancel</button><button onClick={()=>onSave(form)} className="px-4 py-2.5 rounded-full bg-[#1A1D1F] dark-primary text-white text-xs font-bold">Save</button></>}><div className="grid sm:grid-cols-2 gap-4"><SelectField label="Employee" value={form.employee||''} onChange={e=>set('employee',e.target.value)}>{employees.map(e=><option key={e.id} value={e.id}>{e.name}</option>)}</SelectField><FormField label="Date" type="date" value={form.date||''} onChange={e=>set('date',e.target.value)}/><SelectField label="Status" value={form.status||'Present'} onChange={e=>set('status',e.target.value)}>{['Present','Late','Absent','Leave'].map(x=><option key={x}>{x}</option>)}</SelectField><FormField label="Working hours" type="number" step="0.1" value={form.hours??0} onChange={e=>set('hours',e.target.value)}/><FormField label="Check in" type="time" value={form.checkIn==='—'?'':form.checkIn||''} onChange={e=>set('checkIn',e.target.value)}/><FormField label="Check out" type="time" value={form.checkOut==='—'?'':form.checkOut||''} onChange={e=>set('checkOut',e.target.value)}/><FormField label="Note" value={form.note||''} onChange={e=>set('note',e.target.value)}/></div></Modal>
}
