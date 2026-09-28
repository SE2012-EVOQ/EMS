import { Pencil, Plus } from 'lucide-react'
import { useEffect, useMemo, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import Card from '../../../components/common/Card'
import Modal from '../../../components/common/Modal'
import PageHeader from '../../../components/common/PageHeader'
import StatusBadge from '../../../components/common/StatusBadge'
import { FormField, SelectField } from '../../../components/common/FormField'
import { fmtDate } from '../../../components/common/date'
import { DEMO_WEEK } from '../../../data/mock/mockDatabase'
import { useEms } from '../../../context/EmsContext'
import ScheduleCalendar from '../components/ScheduleCalendar'

export default function SchedulePage() {
  const { db, role, currentEmployee, directReports, entriesForTeam, employeeById, saveScheduleEntry }=useEms()
  const teams=role==='admin'?['Platform','Vision','Management']:[currentEmployee.team]
  const [team,setTeam]=useState(teams[0])
  const [editing,setEditing]=useState(null)
  const [error,setError]=useState('')
  const [params,setParams]=useSearchParams()
  useEffect(()=>{if(!teams.includes(team))setTeam(teams[0])},[role])
  useEffect(()=>{if(params.get('create')==='1'&&role==='supervisor'){setEditing(newEntry(currentEmployee,directReports()));params.delete('create');setParams(params,{replace:true})}},[params,role])
  const entries=useMemo(()=>role==='employee'?db.scheduleEntries.filter(x=>x.employee===currentEmployee.id&&DEMO_WEEK.includes(x.date)):entriesForTeam(team).filter(x=>DEMO_WEEK.includes(x.date)),[db,role,team,currentEmployee.id])
  const canEdit=role==='supervisor'
  const members=[currentEmployee,...directReports()].filter((e,i,a)=>e.team===currentEmployee.team&&a.findIndex(x=>x.id===e.id)===i)
  const save=value=>{try{saveScheduleEntry(value);setEditing(null);setError('')}catch(e){setError(e.message)}}
  return <><PageHeader title={role==='employee'?'My schedule':'Team schedule'} description={canEdit?'Create/update employee schedule entries. Conflicts with approved leave and overlapping assignments are blocked.':role==='admin'?'Company schedule visibility.':'Your current scheduled work periods.'} actions={canEdit&&<button onClick={()=>setEditing(newEntry(currentEmployee,directReports()))} className="bg-[#1A1D1F] dark-primary text-white rounded-full px-4 py-2.5 text-xs font-bold flex items-center gap-2"><Plus className="w-4 h-4"/>Add schedule entry</button>}/>{teams.length>1&&<div className="flex gap-2 mb-4">{teams.map(t=><button key={t} onClick={()=>setTeam(t)} className={`px-3 py-2 rounded-full text-xs font-bold ${team===t?'bg-[#1A1D1F] dark-primary text-white':'surface bg-white border border-gray-200'}`}>{t}</button>)}</div>}<Card><ScheduleCalendar entries={entries} employeeById={employeeById}/></Card><Card className="mt-6"><div className="flex items-center justify-between mb-4"><div><h3 className="text-sm font-extrabold txt">Schedule entries</h3><p className="text-[10px] text-app-muted muted mt-1">Each row represents one employee assignment in the schedule.</p></div></div><div className="overflow-x-auto"><table className="w-full min-w-[760px]"><thead><tr className="text-left text-[10px] uppercase tracking-wider text-app-muted muted"><th className="pb-3">Employee</th><th className="pb-3">Date</th><th className="pb-3">Time</th><th className="pb-3">Mode</th><th className="pb-3">Note</th>{canEdit&&<th/>}</tr></thead><tbody>{[...entries].sort((a,b)=>a.date.localeCompare(b.date)||a.start.localeCompare(b.start)).map(x=><tr key={x.id} className="border-t border-app-border"><td className="py-3 text-xs font-bold txt">{employeeById(x.employee)?.name}</td><td className="py-3 text-xs text-app-muted muted">{fmtDate(x.date)}</td><td className="py-3 text-xs font-semibold txt">{x.start} – {x.end}</td><td className="py-3"><StatusBadge status={x.mode}/></td><td className="py-3 text-[10px] text-app-muted muted">{x.note||'—'}</td>{canEdit&&<td className="py-3 text-right"><button onClick={()=>setEditing(x)} className="w-8 h-8 rounded-full hover:bg-app-subtle"><Pencil className="w-3.5 h-3.5 mx-auto"/></button></td>}</tr>)}</tbody></table></div></Card><ScheduleModal record={editing} members={members} open={!!editing} error={error} onClose={()=>{setEditing(null);setError('')}} onSave={save}/></>
}

const newEntry=(currentEmployee,directReports)=>({employee:(directReports()[0]||currentEmployee).id,date:'2026-09-30',start:'09:00',end:'17:00',mode:'Office',note:'Normal working period'})

function ScheduleModal({record,members,open,error,onClose,onSave}) {
  const [form,setForm]=useState(record||{})
  useEffect(()=>setForm(record||{}),[record])
  if(!record)return null
  const set=(k,v)=>setForm(f=>({...f,[k]:v}))
  return <Modal open={open} onClose={onClose} title={record.id?'Update schedule entry':'Add schedule entry'} subtitle="Schedule validation blocks overlaps and approved-leave conflicts." footer={<><button onClick={onClose} className="px-4 py-2.5 rounded-full border border-gray-200 text-xs font-bold">Cancel</button><button onClick={()=>onSave(form)} className="px-4 py-2.5 rounded-full bg-[#1A1D1F] dark-primary text-white text-xs font-bold">Save entry</button></>}><div className="grid sm:grid-cols-2 gap-4"><SelectField label="Employee" value={form.employee||''} onChange={e=>set('employee',e.target.value)}>{members.map(e=><option key={e.id} value={e.id}>{e.name}</option>)}</SelectField><FormField label="Work date" type="date" value={form.date||''} onChange={e=>set('date',e.target.value)}/><FormField label="Start time" type="time" value={form.start||''} onChange={e=>set('start',e.target.value)}/><FormField label="End time" type="time" value={form.end||''} onChange={e=>set('end',e.target.value)}/><SelectField label="Mode" value={form.mode||'Office'} onChange={e=>set('mode',e.target.value)}>{['Office','Remote','Flexible','Lab','Field'].map(x=><option key={x}>{x}</option>)}</SelectField><FormField label="Note" value={form.note||''} onChange={e=>set('note',e.target.value)}/>{error&&<div className="sm:col-span-2 text-xs font-bold text-app-pink bg-app-pink-bg p-3 rounded-2xl">{error}</div>}</div></Modal>
}
