-- EVOQ Employee Management System
-- Database validation and viva/demo checks
USE evoq_ems;

-- 1. Confirm expected tables exist.
SHOW TABLES;

-- 2. Confirm row counts after sample-data population.
SELECT 'role' AS table_name, COUNT(*) AS row_count FROM role
UNION ALL SELECT 'department', COUNT(*) FROM department
UNION ALL SELECT 'team_project', COUNT(*) FROM team_project
UNION ALL SELECT 'employee', COUNT(*) FROM employee
UNION ALL SELECT 'user_account', COUNT(*) FROM user_account
UNION ALL SELECT 'leave_type', COUNT(*) FROM leave_type
UNION ALL SELECT 'leave_balance', COUNT(*) FROM leave_balance
UNION ALL SELECT 'leave_request', COUNT(*) FROM leave_request
UNION ALL SELECT 'schedule', COUNT(*) FROM schedule
UNION ALL SELECT 'schedule_entry', COUNT(*) FROM schedule_entry
UNION ALL SELECT 'attendance_record', COUNT(*) FROM attendance_record
UNION ALL SELECT 'asset', COUNT(*) FROM asset
UNION ALL SELECT 'asset_assignment', COUNT(*) FROM asset_assignment;

-- 3. Orphan checks. Every result should be 0.
SELECT COUNT(*) AS orphan_user_accounts
FROM user_account ua LEFT JOIN employee e ON e.employee_id = ua.employee_id
WHERE e.employee_id IS NULL;

SELECT COUNT(*) AS orphan_leave_requests
FROM leave_request lr LEFT JOIN employee e ON e.employee_id = lr.employee_id
WHERE e.employee_id IS NULL;

SELECT COUNT(*) AS orphan_schedule_entries
FROM schedule_entry se LEFT JOIN schedule s ON s.schedule_id = se.schedule_id
WHERE s.schedule_id IS NULL;

SELECT COUNT(*) AS orphan_attendance_records
FROM attendance_record ar LEFT JOIN employee e ON e.employee_id = ar.employee_id
WHERE e.employee_id IS NULL;

SELECT COUNT(*) AS orphan_asset_assignments
FROM asset_assignment aa LEFT JOIN asset a ON a.asset_id = aa.asset_id
WHERE a.asset_id IS NULL;

-- 4. Check the 1:1 UserAccount-to-Employee implementation.
SELECT employee_id, COUNT(*) AS account_count
FROM user_account
GROUP BY employee_id
HAVING COUNT(*) > 1;
-- Expected: no rows.

-- 5. Check one LeaveBalance per employee/leave-type pair.
SELECT employee_id, leave_type_id, COUNT(*) AS duplicate_count
FROM leave_balance
GROUP BY employee_id, leave_type_id
HAVING COUNT(*) > 1;
-- Expected: no rows.

-- 6. Detect invalid date/time data. Each query should return no rows.
SELECT * FROM leave_request WHERE end_date < start_date;
SELECT * FROM schedule WHERE period_end < period_start;
SELECT * FROM schedule_entry WHERE end_time <= start_time;
SELECT * FROM attendance_record WHERE working_hours < 0;
SELECT * FROM asset_assignment
WHERE returned_date IS NOT NULL AND returned_date < assigned_date;

-- 7. Business-rule validation: overlapping schedule entries.
SELECT a.schedule_entry_id, b.schedule_entry_id, a.employee_id, a.work_date
FROM schedule_entry a
JOIN schedule_entry b
  ON a.employee_id = b.employee_id
 AND a.work_date = b.work_date
 AND a.schedule_entry_id < b.schedule_entry_id
 AND a.start_time < b.end_time
 AND a.end_time > b.start_time;
-- Expected for clean schedule data: no rows.

-- 8. Business-rule validation: published schedule vs approved leave conflicts.
SELECT se.schedule_entry_id, se.employee_id, se.work_date, lr.leave_request_id
FROM schedule_entry se
JOIN leave_request lr
  ON lr.employee_id = se.employee_id
 AND lr.status = 'APPROVED'
 AND se.work_date BETWEEN lr.start_date AND lr.end_date;
-- Expected for clean schedule data: no rows. If rows appear, the schedule service must block publication.

-- 9. Business-rule validation: more than one active assignment for the same asset.
SELECT asset_id, COUNT(*) AS active_assignment_count
FROM asset_assignment
WHERE status = 'ACTIVE' AND returned_date IS NULL
GROUP BY asset_id
HAVING COUNT(*) > 1;
-- Expected: no rows.

-- 10. Useful schema inspection for viva/demo.
SHOW CREATE TABLE employee;
SHOW CREATE TABLE leave_balance;
SHOW CREATE TABLE schedule_entry;
SHOW CREATE TABLE asset_assignment;
