import { apiRequest } from '../../../services/api'
export const attendanceService = {
  attendance: () => apiRequest('/attendance'),
  schedules: () => apiRequest('/schedules'),
  createScheduleEntry: data => apiRequest('/schedule-entries',{method:'POST',body:JSON.stringify(data)})
}
