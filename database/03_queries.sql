-- EVOQ Employee Management System
-- Identified SQL queries grouped by student/member ownership
USE evoq_ems;

-- ============================================================
-- Member 1: Roashan.J - IT25100943
-- Employee & Organization Management
-- ============================================================

-- Q1.1 List active employees with department, team/project and supervisor.
SELECT e.employee_id,
       CONCAT(e.first_name, ' ', e.last_name) AS employee_name,
       d.name AS department,
       tp.name AS team_project,
       CONCAT(s.first_name, ' ', s.last_name) AS supervisor,
       e.job_title
FROM employee e
JOIN department d ON d.department_id = e.department_id
LEFT JOIN team_project tp ON tp.team_id = e.team_id
LEFT JOIN employee s ON s.employee_id = e.supervisor_id
WHERE e.status = 'ACTIVE'
ORDER BY d.name, e.first_name, e.last_name;

-- Q1.2 Count active employees in each department.
SELECT d.department_id,
       d.name AS department,
       COUNT(e.employee_id) AS active_employee_count
FROM department d
LEFT JOIN employee e
       ON e.department_id = d.department_id
      AND e.status = 'ACTIVE'
GROUP BY d.department_id, d.name
ORDER BY active_employee_count DESC, d.name;

-- Q1.3 List employees reporting to a selected supervisor (example: employee_id = 2).
SELECT e.employee_id,
       CONCAT(e.first_name, ' ', e.last_name) AS employee_name,
       e.job_title,
       e.email,
       e.status
FROM employee e
WHERE e.supervisor_id = 2
ORDER BY e.first_name, e.last_name;

-- Q1.4 Show employees grouped by current team/project.
SELECT tp.name AS team_project,
       e.employee_id,
       CONCAT(e.first_name, ' ', e.last_name) AS employee_name,
       e.job_title
FROM team_project tp
LEFT JOIN employee e ON e.team_id = tp.team_id
ORDER BY tp.name, e.first_name, e.last_name;

-- Q1.5 Find an employee by partial name, email or job title.
SELECT employee_id, first_name, last_name, email, job_title, status
FROM employee
WHERE CONCAT(first_name, ' ', last_name) LIKE '%engine%'
   OR email LIKE '%engine%'
   OR job_title LIKE '%engine%'
ORDER BY first_name, last_name;

-- ============================================================
-- Member 2: Samarappuli V. D. B - IT25100959
-- Leave Management
-- ============================================================

-- Q2.1 View leave balance by leave type for a selected employee (example: employee_id = 4).
SELECT e.employee_id,
       CONCAT(e.first_name, ' ', e.last_name) AS employee_name,
       lt.name AS leave_type,
       lb.available_days,
       lb.used_days
FROM leave_balance lb
JOIN employee e ON e.employee_id = lb.employee_id
JOIN leave_type lt ON lt.leave_type_id = lb.leave_type_id
WHERE lb.employee_id = 4
ORDER BY lt.name;

-- Q2.2 View complete leave request history for a selected employee.
SELECT lr.leave_request_id,
       lt.name AS leave_type,
       lr.start_date,
       lr.end_date,
       lr.reason,
       lr.status,
       lr.submitted_date
FROM leave_request lr
JOIN leave_type lt ON lt.leave_type_id = lr.leave_type_id
WHERE lr.employee_id = 4
ORDER BY lr.submitted_date DESC;

-- Q2.3 Show pending leave requests belonging to employees supervised by a selected supervisor.
SELECT lr.leave_request_id,
       e.employee_id,
       CONCAT(e.first_name, ' ', e.last_name) AS employee_name,
       lt.name AS leave_type,
       lr.start_date,
       lr.end_date,
       lr.reason,
       lr.submitted_date
FROM leave_request lr
JOIN employee e ON e.employee_id = lr.employee_id
JOIN leave_type lt ON lt.leave_type_id = lr.leave_type_id
WHERE e.supervisor_id = 2
  AND lr.status = 'PENDING'
ORDER BY lr.submitted_date;

-- Q2.4 Leave summary by leave type and request status.
SELECT lt.name AS leave_type,
       lr.status,
       COUNT(*) AS request_count,
       SUM(DATEDIFF(lr.end_date, lr.start_date) + 1) AS requested_days
FROM leave_request lr
JOIN leave_type lt ON lt.leave_type_id = lr.leave_type_id
GROUP BY lt.name, lr.status
ORDER BY lt.name, lr.status;

-- Q2.5 List currently approved leave overlapping a selected date range.
SELECT e.employee_id,
       CONCAT(e.first_name, ' ', e.last_name) AS employee_name,
       lt.name AS leave_type,
       lr.start_date,
       lr.end_date
FROM leave_request lr
JOIN employee e ON e.employee_id = lr.employee_id
JOIN leave_type lt ON lt.leave_type_id = lr.leave_type_id
WHERE lr.status = 'APPROVED'
  AND lr.start_date <= '2026-09-30'
  AND lr.end_date >= '2026-09-28'
ORDER BY lr.start_date, employee_name;

-- ============================================================
-- Member 3: Ahmed Nadhi - IT25101023
-- Attendance & Scheduling Management
-- ============================================================

-- Q3.1 View an employee's schedule for a selected date range.
SELECT e.employee_id,
       CONCAT(e.first_name, ' ', e.last_name) AS employee_name,
       tp.name AS team_project,
       se.work_date,
       se.start_time,
       se.end_time,
       se.notes,
       s.status AS schedule_status
FROM schedule_entry se
JOIN schedule s ON s.schedule_id = se.schedule_id
JOIN employee e ON e.employee_id = se.employee_id
JOIN team_project tp ON tp.team_id = s.team_id
WHERE se.employee_id = 4
  AND se.work_date BETWEEN '2026-09-28' AND '2026-10-04'
ORDER BY se.work_date, se.start_time;

-- Q3.2 View a team's published schedule for a selected period.
SELECT tp.name AS team_project,
       se.work_date,
       CONCAT(e.first_name, ' ', e.last_name) AS employee_name,
       se.start_time,
       se.end_time,
       se.notes
FROM schedule s
JOIN team_project tp ON tp.team_id = s.team_id
JOIN schedule_entry se ON se.schedule_id = s.schedule_id
JOIN employee e ON e.employee_id = se.employee_id
WHERE s.team_id = 1
  AND s.status = 'PUBLISHED'
  AND se.work_date BETWEEN '2026-09-28' AND '2026-10-04'
ORDER BY se.work_date, se.start_time, employee_name;

-- Q3.3 Attendance summary per employee over a selected date range.
SELECT e.employee_id,
       CONCAT(e.first_name, ' ', e.last_name) AS employee_name,
       COUNT(ar.attendance_id) AS recorded_days,
       SUM(CASE WHEN ar.status = 'PRESENT' THEN 1 ELSE 0 END) AS present_days,
       SUM(CASE WHEN ar.status = 'ABSENT' THEN 1 ELSE 0 END) AS absent_days,
       ROUND(SUM(ar.working_hours), 2) AS total_working_hours,
       ROUND(AVG(ar.working_hours), 2) AS average_working_hours
FROM employee e
LEFT JOIN attendance_record ar
       ON ar.employee_id = e.employee_id
      AND ar.attendance_date BETWEEN '2026-09-28' AND '2026-09-30'
GROUP BY e.employee_id, e.first_name, e.last_name
ORDER BY e.employee_id;

-- Q3.4 Detect overlapping schedule entries for the same employee on the same date.
SELECT a.employee_id,
       CONCAT(e.first_name, ' ', e.last_name) AS employee_name,
       a.work_date,
       a.schedule_entry_id AS first_entry,
       b.schedule_entry_id AS conflicting_entry,
       a.start_time AS first_start,
       a.end_time AS first_end,
       b.start_time AS second_start,
       b.end_time AS second_end
FROM schedule_entry a
JOIN schedule_entry b
  ON a.employee_id = b.employee_id
 AND a.work_date = b.work_date
 AND a.schedule_entry_id < b.schedule_entry_id
 AND a.start_time < b.end_time
 AND a.end_time > b.start_time
JOIN employee e ON e.employee_id = a.employee_id
ORDER BY a.employee_id, a.work_date;

-- Q3.5 Detect schedule entries that conflict with approved leave.
SELECT se.schedule_entry_id,
       e.employee_id,
       CONCAT(e.first_name, ' ', e.last_name) AS employee_name,
       se.work_date,
       se.start_time,
       se.end_time,
       lr.leave_request_id,
       lt.name AS leave_type,
       lr.start_date,
       lr.end_date
FROM schedule_entry se
JOIN employee e ON e.employee_id = se.employee_id
JOIN leave_request lr
  ON lr.employee_id = se.employee_id
 AND lr.status = 'APPROVED'
 AND se.work_date BETWEEN lr.start_date AND lr.end_date
JOIN leave_type lt ON lt.leave_type_id = lr.leave_type_id
ORDER BY se.work_date, employee_name;

-- ============================================================
-- Member 4: V.J.Shaarugshan - IT25101699
-- Asset & Equipment Management
-- ============================================================

-- Q4.1 List assets currently available for assignment.
SELECT asset_id, asset_name, asset_type, serial_number, status
FROM asset
WHERE status = 'AVAILABLE'
ORDER BY asset_type, asset_name;

-- Q4.2 View all currently assigned assets with employee details.
SELECT aa.assignment_id,
       a.asset_id,
       a.asset_name,
       a.asset_type,
       a.serial_number,
       e.employee_id,
       CONCAT(e.first_name, ' ', e.last_name) AS employee_name,
       aa.assigned_date,
       aa.status AS assignment_status
FROM asset_assignment aa
JOIN asset a ON a.asset_id = aa.asset_id
JOIN employee e ON e.employee_id = aa.employee_id
WHERE aa.status = 'ACTIVE'
  AND aa.returned_date IS NULL
ORDER BY employee_name, a.asset_type;

-- Q4.3 View assets assigned to a selected employee.
SELECT a.asset_id,
       a.asset_name,
       a.asset_type,
       a.serial_number,
       aa.assigned_date,
       aa.returned_date,
       aa.status
FROM asset_assignment aa
JOIN asset a ON a.asset_id = aa.asset_id
WHERE aa.employee_id = 4
ORDER BY aa.assigned_date DESC;

-- Q4.4 View complete assignment history for a selected asset.
SELECT a.asset_id,
       a.asset_name,
       a.serial_number,
       CONCAT(e.first_name, ' ', e.last_name) AS employee_name,
       aa.assigned_date,
       aa.returned_date,
       aa.status
FROM asset_assignment aa
JOIN asset a ON a.asset_id = aa.asset_id
JOIN employee e ON e.employee_id = aa.employee_id
WHERE aa.asset_id = 5
ORDER BY aa.assigned_date DESC;

-- Q4.5 Identify active asset assignments belonging to deactivated employees.
SELECT e.employee_id,
       CONCAT(e.first_name, ' ', e.last_name) AS employee_name,
       a.asset_id,
       a.asset_name,
       a.serial_number,
       aa.assigned_date
FROM employee e
JOIN asset_assignment aa ON aa.employee_id = e.employee_id
JOIN asset a ON a.asset_id = aa.asset_id
WHERE e.status = 'INACTIVE'
  AND aa.status = 'ACTIVE'
  AND aa.returned_date IS NULL
ORDER BY e.employee_id, a.asset_id;
