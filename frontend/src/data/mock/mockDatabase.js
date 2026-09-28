const seedDatabase = {
    roles: [
      {id:'R-EMP',name:'EMPLOYEE',label:'Employee'},
      {id:'R-SUP',name:'SUPERVISOR',label:'Supervisor / Team Lead'},
      {id:'R-ADM',name:'MANAGER_ADMIN',label:'Manager / Admin'}
    ],
    departments: [
      {id:'D001',name:'Management'},
      {id:'D002',name:'Engineering'}
    ],
    teamProjects: [
      {id:'TP-MGT',name:'Management',project:'Operations',department:'D001'},
      {id:'TP-VIS',name:'Vision',project:'Vision Edge',department:'D002'},
      {id:'TP-PLT',name:'Platform',project:'EMS Portal',department:'D002'}
    ],
    employees: [
      {id:'E001',name:'A. Perera',email:'a.perera@evoq.ai',phone:'+94 77 245 1001',address:'Colombo 05',title:'Director / Manager',joined:'2024-02-05',department:'Management',project:'Operations',team:'Management',supervisor:null,status:'Active',avatar:'AP'},
      {id:'E002',name:'Sarah Fernando',email:'sarah@evoq.ai',phone:'+94 71 880 2240',address:'Rajagiriya',title:'Computer Vision Lead',joined:'2024-05-12',department:'Engineering',project:'Vision Edge',team:'Vision',supervisor:'E001',status:'Active',avatar:'SF'},
      {id:'E003',name:'Ravin Silva',email:'ravin@evoq.ai',phone:'+94 76 312 6638',address:'Nugegoda',title:'ML Engineer',joined:'2025-01-15',department:'Engineering',project:'Vision Edge',team:'Vision',supervisor:'E002',status:'Active',avatar:'RS'},
      {id:'E004',name:'Nadeem Hassan',email:'nadeem@evoq.ai',phone:'+94 77 900 3172',address:'Dehiwala',title:'Software Engineer',joined:'2025-03-03',department:'Engineering',project:'EMS Portal',team:'Platform',supervisor:'E006',status:'Active',avatar:'NH'},
      {id:'E005',name:'Ishara Jayasinghe',email:'ishara@evoq.ai',phone:'+94 70 432 9090',address:'Kottawa',title:'CV Engineer',joined:'2025-03-21',department:'Engineering',project:'Vision Edge',team:'Vision',supervisor:'E002',status:'Active',avatar:'IJ'},
      {id:'E006',name:'Maya de Silva',email:'maya@evoq.ai',phone:'+94 72 182 4401',address:'Battaramulla',title:'Platform Team Lead',joined:'2024-09-09',department:'Engineering',project:'EMS Portal',team:'Platform',supervisor:'E001',status:'Active',avatar:'MD'},
      {id:'E007',name:'Kavindu Peris',email:'kavindu@evoq.ai',phone:'+94 75 522 7681',address:'Maharagama',title:'Frontend Engineer',joined:'2025-06-10',department:'Engineering',project:'EMS Portal',team:'Platform',supervisor:'E006',status:'Active',avatar:'KP'},
      {id:'E008',name:'Dinithi Ramanayake',email:'dinithi@evoq.ai',phone:'+94 77 224 0898',address:'Kaduwela',title:'QA Engineer',joined:'2025-08-14',department:'Engineering',project:'EMS Portal',team:'Platform',supervisor:'E006',status:'Active',avatar:'DR'},
      {id:'E009',name:'Shenal Dias',email:'shenal@evoq.ai',phone:'+94 76 990 1120',address:'Mount Lavinia',title:'Research Intern',joined:'2026-02-02',department:'Engineering',project:'Vision Edge',team:'Vision',supervisor:'E002',status:'Active',avatar:'SD'},
      {id:'E010',name:'Anuki Wijeratne',email:'anuki@evoq.ai',phone:'+94 74 663 4420',address:'Malabe',title:'UI/UX Designer',joined:'2026-05-18',department:'Engineering',project:'EMS Portal',team:'Platform',supervisor:'E006',status:'Active',avatar:'AW'}
    ],
    userAccounts: [
      {id:'U001',employee:'E001',username:'a.perera',role:'MANAGER_ADMIN',active:true},
      {id:'U002',employee:'E002',username:'sarah',role:'SUPERVISOR',active:true},
      {id:'U003',employee:'E003',username:'ravin',role:'EMPLOYEE',active:true},
      {id:'U004',employee:'E004',username:'nadeem',role:'EMPLOYEE',active:true},
      {id:'U005',employee:'E005',username:'ishara',role:'EMPLOYEE',active:true},
      {id:'U006',employee:'E006',username:'maya',role:'SUPERVISOR',active:true},
      {id:'U007',employee:'E007',username:'kavindu',role:'EMPLOYEE',active:true},
      {id:'U008',employee:'E008',username:'dinithi',role:'EMPLOYEE',active:true},
      {id:'U009',employee:'E009',username:'shenal',role:'EMPLOYEE',active:true},
      {id:'U010',employee:'E010',username:'anuki',role:'EMPLOYEE',active:true}
    ],
    leaveBalances: {
      E001:{Annual:5,Medical:6,Casual:2}, E002:{Annual:8,Medical:6,Casual:3}, E003:{Annual:9,Medical:6,Casual:3},
      E004:{Annual:10,Medical:5,Casual:2}, E005:{Annual:8,Medical:7,Casual:4}, E006:{Annual:7,Medical:7,Casual:2},
      E007:{Annual:10,Medical:7,Casual:3}, E008:{Annual:10,Medical:6,Casual:4}, E009:{Annual:7,Medical:4,Casual:2}, E010:{Annual:11,Medical:7,Casual:4}
    },
    leaveRequests: [
      {id:'LV-1049',employee:'E008',type:'Annual',from:'2026-09-30',to:'2026-10-01',days:2,reason:'Family travel',status:'Approved',submitted:'2026-09-22',decidedBy:'E006',decidedAt:'2026-09-23'},
      {id:'LV-1048',employee:'E004',type:'Annual',from:'2026-09-28',to:'2026-09-29',days:2,reason:'Family event',status:'Pending',submitted:'2026-09-21',decidedBy:null,decidedAt:null},
      {id:'LV-1047',employee:'E007',type:'Medical',from:'2026-09-18',to:'2026-09-18',days:1,reason:'Medical appointment',status:'Approved',submitted:'2026-09-17',decidedBy:'E006',decidedAt:'2026-09-17'},
      {id:'LV-1046',employee:'E003',type:'Casual',from:'2026-09-25',to:'2026-09-25',days:1,reason:'Personal errand',status:'Pending',submitted:'2026-09-16',decidedBy:null,decidedAt:null},
      {id:'LV-1045',employee:'E008',type:'Annual',from:'2026-09-11',to:'2026-09-12',days:2,reason:'Travel',status:'Approved',submitted:'2026-09-03',decidedBy:'E006',decidedAt:'2026-09-04'},
      {id:'LV-1044',employee:'E005',type:'Medical',from:'2026-09-02',to:'2026-09-03',days:2,reason:'Flu',status:'Approved',submitted:'2026-09-01',decidedBy:'E002',decidedAt:'2026-09-01'},
      {id:'LV-1043',employee:'E009',type:'Casual',from:'2026-08-28',to:'2026-08-28',days:1,reason:'Personal',status:'Rejected',submitted:'2026-08-25',decidedBy:'E002',decidedAt:'2026-08-26'},
      {id:'LV-1042',employee:'E010',type:'Annual',from:'2026-08-20',to:'2026-08-21',days:2,reason:'Family trip',status:'Approved',submitted:'2026-08-10',decidedBy:'E006',decidedAt:'2026-08-11'}
    ],
    attendance: [],
    schedules: [
      {id:'SCH-001',team:'Platform',periodStart:'2026-09-28',periodEnd:'2026-10-02',status:'Published',createdBy:'E006',createdAt:'2026-09-24',updatedAt:'2026-09-27'},
      {id:'SCH-002',team:'Vision',periodStart:'2026-09-28',periodEnd:'2026-10-02',status:'Published',createdBy:'E002',createdAt:'2026-09-24',updatedAt:'2026-09-27'}
    ],
    scheduleEntries: [],
    assets: [
      {id:'AST-001',name:'MacBook Pro 14"',category:'Laptop',serial:'MBP-M3-0241',status:'Assigned'},
      {id:'AST-002',name:'Dell UltraSharp 27"',category:'Monitor',serial:'DL-U27-1183',status:'Assigned'},
      {id:'AST-003',name:'NVIDIA Jetson Orin',category:'Specialized device',serial:'NV-JO-8812',status:'Assigned'},
      {id:'AST-004',name:'MacBook Air 13"',category:'Laptop',serial:'MBA-M2-4503',status:'Available'},
      {id:'AST-005',name:'Logitech MX Keys',category:'Peripheral',serial:'LG-MXK-7202',status:'Assigned'},
      {id:'AST-006',name:'RealSense D455',category:'Camera',serial:'RS-D455-310',status:'Maintenance'},
      {id:'AST-007',name:'ThinkPad P1',category:'Laptop',serial:'LN-P1-4411',status:'Damaged'},
      {id:'AST-008',name:'LG 32UN880',category:'Monitor',serial:'LG-32-2298',status:'Assigned'},
      {id:'AST-009',name:'iPad Air',category:'Tablet',serial:'IP-AIR-0092',status:'Available'}
    ],
    assetAssignments: [
      {id:'AA-001',asset:'AST-001',employee:'E004',assignedDate:'2025-03-03',returnedDate:null,status:'Active'},
      {id:'AA-002',asset:'AST-002',employee:'E007',assignedDate:'2025-06-10',returnedDate:null,status:'Active'},
      {id:'AA-003',asset:'AST-003',employee:'E003',assignedDate:'2025-02-01',returnedDate:null,status:'Active'},
      {id:'AA-004',asset:'AST-005',employee:'E010',assignedDate:'2026-05-18',returnedDate:null,status:'Active'},
      {id:'AA-005',asset:'AST-008',employee:'E006',assignedDate:'2024-09-09',returnedDate:null,status:'Active'},
      {id:'AA-006',asset:'AST-006',employee:'E005',assignedDate:'2026-04-12',returnedDate:'2026-07-18',status:'Returned'}
    ],
    notifications: [
      {title:'Leave request pending',detail:'Nadeem Hassan requested 2 days annual leave.',time:'18 min ago',icon:'calendar-clock'},
      {title:'Asset needs attention',detail:'RealSense D455 is currently under maintenance.',time:'2h ago',icon:'wrench'},
      {title:'Schedule published',detail:'Platform schedule is published for 28 Sep – 02 Oct.',time:'Yesterday',icon:'clock-3'}
    ]
  };


export const DEMO_TODAY = '2026-09-28'
export const DEMO_WEEK = ['2026-09-28','2026-09-29','2026-09-30','2026-10-01','2026-10-02']

const scheduleTemplates = {
  Platform: [
    ['2026-09-28','09:00','17:30','Office','Sprint planning 9:30 AM'],
    ['2026-09-29','09:00','17:30','Office','Normal day'],
    ['2026-09-30','10:00','18:00','Flexible','Core hours 10–15'],
    ['2026-10-01','09:00','17:30','Remote','Remote collaboration day'],
    ['2026-10-02','09:00','16:30','Office','Demo at 3 PM']
  ],
  Vision: [
    ['2026-09-28','08:30','17:00','Lab','Dataset calibration'],
    ['2026-09-29','08:30','17:00','Lab','Model evaluation'],
    ['2026-09-30','09:30','17:30','Flexible','Field test prep'],
    ['2026-10-01','07:30','16:00','Field','Field validation'],
    ['2026-10-02','09:00','17:00','Lab','Results review']
  ]
}

export function createMockDatabase() {
  const db = structuredClone(seedDatabase)
  let scheduleEntryCounter = 1

  db.schedules.forEach(schedule => {
    const members = db.employees.filter(e => e.team === schedule.team && e.status === 'Active')
    ;(scheduleTemplates[schedule.team] || []).forEach(([date,start,end,mode,note]) => {
      members.forEach(employee => {
        const onApprovedLeave = db.leaveRequests.some(
          leave => leave.employee === employee.id && leave.status === 'Approved' && date >= leave.from && date <= leave.to
        )
        if (!onApprovedLeave) {
          db.scheduleEntries.push({
            id: `SE-${String(scheduleEntryCounter++).padStart(3,'0')}`,
            scheduleId: schedule.id,
            employee: employee.id,
            date, start, end, mode, note
          })
        }
      })
    })
  })

  const attendancePeople = db.employees.filter(e => e.status === 'Active')
  const dates = ['2026-09-21','2026-09-22','2026-09-23','2026-09-24','2026-09-25',DEMO_TODAY]
  dates.forEach((date, di) => attendancePeople.forEach((employee, ei) => {
    let status = 'Present'
    if ((di + ei) % 19 === 0) status = 'Absent'
    else if ((di * 3 + ei) % 13 === 0) status = 'Late'
    db.attendance.push({
      id: `AT-${date}-${employee.id}`,
      employee: employee.id,
      date,
      status,
      checkIn: status === 'Absent' ? '—' : status === 'Late' ? '09:34' : employee.team === 'Vision' ? '08:42' : '09:02',
      checkOut: status === 'Absent' ? '—' : '17:28',
      hours: status === 'Absent' ? 0 : status === 'Late' ? 7.9 : 8.4,
      note: status === 'Late' ? 'Traffic delay' : ''
    })
  }))

  return db
}
