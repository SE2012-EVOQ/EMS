-- EVOQ Employee Management System
-- Sample dataset for SE2032 demonstration
USE evoq_ems;

INSERT INTO role (name, description) VALUES
('EMPLOYEE', 'Standard employee access'),
('SUPERVISOR', 'Employee access plus team supervision functions'),
('MANAGER_ADMIN', 'Employee access plus organization-wide administration functions');

INSERT INTO department (name, description) VALUES
('Engineering', 'Computer vision and software engineering'),
('Operations', 'Deployment, support and operational activities'),
('Administration', 'Company administration and coordination');

INSERT INTO team_project (name, description) VALUES
('Vision Core', 'Core computer vision development team'),
('Edge Deployment', 'Deployment and integration team'),
('Internal Operations', 'Internal administration and operational support');

-- Create manager first so later employees can reference the manager as supervisor.
INSERT INTO employee
(department_id, team_id, supervisor_id, first_name, last_name, email, phone, address, hire_date, job_title, status)
VALUES
(3, 3, NULL, 'Maya', 'Fernando', 'maya.fernando@evoq.example', '+94-70-000-1001', 'Colombo', '2023-01-15', 'Director / Manager', 'ACTIVE');

INSERT INTO employee
(department_id, team_id, supervisor_id, first_name, last_name, email, phone, address, hire_date, job_title, status)
VALUES
(1, 1, 1, 'Dilan', 'Perera', 'dilan.perera@evoq.example', '+94-70-000-1002', 'Colombo', '2023-05-10', 'Senior Computer Vision Engineer', 'ACTIVE'),
(2, 2, 1, 'Nethmi', 'Silva', 'nethmi.silva@evoq.example', '+94-70-000-1003', 'Gampaha', '2023-07-03', 'Deployment Lead', 'ACTIVE'),
(1, 1, 2, 'Arjun', 'Jayasinghe', 'arjun.jayasinghe@evoq.example', '+94-70-000-1004', 'Colombo', '2024-02-12', 'Computer Vision Engineer', 'ACTIVE'),
(1, 1, 2, 'Kavindi', 'Senanayake', 'kavindi.senanayake@evoq.example', '+94-70-000-1005', 'Kaduwela', '2024-04-22', 'Software Engineer', 'ACTIVE'),
(1, 1, 2, 'Ruwan', 'Dias', 'ruwan.dias@evoq.example', '+94-70-000-1006', 'Moratuwa', '2024-08-05', 'Machine Learning Engineer', 'ACTIVE'),
(2, 2, 3, 'Ishara', 'Gunawardena', 'ishara.gunawardena@evoq.example', '+94-70-000-1007', 'Negombo', '2024-09-16', 'Deployment Engineer', 'ACTIVE'),
(2, 2, 3, 'Fathima', 'Nazeer', 'fathima.nazeer@evoq.example', '+94-70-000-1008', 'Colombo', '2025-01-20', 'Support Engineer', 'ACTIVE'),
(2, 2, 3, 'Tharindu', 'Wijesinghe', 'tharindu.wijesinghe@evoq.example', '+94-70-000-1009', 'Gampaha', '2025-06-02', 'Field Engineer', 'ACTIVE'),
(3, 3, 1, 'Sajini', 'Abeysekara', 'sajini.abeysekara@evoq.example', '+94-70-000-1010', 'Colombo', '2025-08-11', 'Operations Coordinator', 'ACTIVE');

-- Password hashes are demonstration placeholders and should be replaced with BCrypt hashes during application setup.
INSERT INTO user_account (employee_id, role_id, username, password_hash, active) VALUES
(1, 3, 'maya.fernando', 'DEMO_HASH_REPLACE_DURING_SETUP', TRUE),
(2, 2, 'dilan.perera', 'DEMO_HASH_REPLACE_DURING_SETUP', TRUE),
(3, 2, 'nethmi.silva', 'DEMO_HASH_REPLACE_DURING_SETUP', TRUE),
(4, 1, 'arjun.jayasinghe', 'DEMO_HASH_REPLACE_DURING_SETUP', TRUE),
(5, 1, 'kavindi.senanayake', 'DEMO_HASH_REPLACE_DURING_SETUP', TRUE),
(6, 1, 'ruwan.dias', 'DEMO_HASH_REPLACE_DURING_SETUP', TRUE),
(7, 1, 'ishara.gunawardena', 'DEMO_HASH_REPLACE_DURING_SETUP', TRUE),
(8, 1, 'fathima.nazeer', 'DEMO_HASH_REPLACE_DURING_SETUP', TRUE),
(9, 1, 'tharindu.wijesinghe', 'DEMO_HASH_REPLACE_DURING_SETUP', TRUE),
(10, 1, 'sajini.abeysekara', 'DEMO_HASH_REPLACE_DURING_SETUP', TRUE);

INSERT INTO leave_type (name, description) VALUES
('Annual Leave', 'Planned annual leave'),
('Medical / Sick Leave', 'Leave for illness or medical reasons'),
('Casual Leave', 'Short personal or casual leave');

INSERT INTO leave_balance (employee_id, leave_type_id, available_days, used_days)
SELECT e.employee_id, lt.leave_type_id,
       CASE lt.leave_type_id WHEN 1 THEN 10.00 WHEN 2 THEN 6.00 ELSE 5.00 END,
       CASE lt.leave_type_id WHEN 1 THEN 2.00 WHEN 2 THEN 1.00 ELSE 0.00 END
FROM employee e CROSS JOIN leave_type lt;

INSERT INTO leave_request
(employee_id, leave_type_id, start_date, end_date, reason, status, submitted_date) VALUES
(4, 1, '2026-09-14', '2026-09-15', 'Personal leave', 'APPROVED', '2026-09-05 09:15:00'),
(5, 2, '2026-09-21', '2026-09-21', 'Medical appointment', 'APPROVED', '2026-09-19 18:20:00'),
(6, 3, '2026-09-30', '2026-09-30', 'Personal matter', 'PENDING', '2026-09-27 10:30:00'),
(7, 1, '2026-10-05', '2026-10-07', 'Family event', 'PENDING', '2026-09-26 14:00:00'),
(8, 2, '2026-09-18', '2026-09-19', 'Illness', 'APPROVED', '2026-09-18 07:45:00'),
(9, 3, '2026-09-25', '2026-09-25', 'Personal appointment', 'REJECTED', '2026-09-22 12:10:00'),
(10, 1, '2026-10-12', '2026-10-13', 'Planned leave', 'PENDING', '2026-09-28 08:35:00');

INSERT INTO schedule (team_id, period_start, period_end, status, created_at, updated_at) VALUES
(1, '2026-09-28', '2026-10-04', 'PUBLISHED', '2026-09-26 10:00:00', '2026-09-26 10:00:00'),
(2, '2026-09-28', '2026-10-04', 'PUBLISHED', '2026-09-26 11:00:00', '2026-09-26 11:00:00'),
(3, '2026-09-28', '2026-10-04', 'PUBLISHED', '2026-09-26 12:00:00', '2026-09-26 12:00:00');

INSERT INTO schedule_entry
(schedule_id, employee_id, work_date, start_time, end_time, notes) VALUES
(1, 2, '2026-09-28', '09:00:00', '17:00:00', 'Team lead coverage'),
(1, 4, '2026-09-28', '09:00:00', '17:00:00', 'Vision pipeline development'),
(1, 5, '2026-09-28', '10:00:00', '18:00:00', 'Backend integration'),
(1, 6, '2026-09-28', '09:00:00', '17:00:00', 'Model evaluation'),
(1, 4, '2026-09-29', '09:00:00', '17:00:00', 'Vision pipeline development'),
(1, 5, '2026-09-29', '10:00:00', '18:00:00', 'Backend integration'),
(1, 6, '2026-09-29', '09:00:00', '17:00:00', 'Model evaluation'),
(2, 3, '2026-09-28', '08:30:00', '16:30:00', 'Deployment coordination'),
(2, 7, '2026-09-28', '08:30:00', '16:30:00', 'Client deployment'),
(2, 8, '2026-09-28', '09:00:00', '17:00:00', 'Support coverage'),
(2, 9, '2026-09-28', '08:30:00', '16:30:00', 'Field installation'),
(3, 1, '2026-09-28', '09:00:00', '17:00:00', 'Management'),
(3, 10, '2026-09-28', '09:00:00', '17:00:00', 'Operations coordination');

INSERT INTO attendance_record
(employee_id, attendance_date, check_in_time, check_out_time, status, working_hours, notes) VALUES
(1, '2026-09-28', '08:55:00', '17:05:00', 'PRESENT', 8.17, NULL),
(2, '2026-09-28', '08:58:00', '17:02:00', 'PRESENT', 8.07, NULL),
(3, '2026-09-28', '08:25:00', '16:35:00', 'PRESENT', 8.17, NULL),
(4, '2026-09-28', '09:03:00', '17:08:00', 'PRESENT', 8.08, NULL),
(5, '2026-09-28', '10:08:00', '18:03:00', 'PRESENT', 7.92, 'Late check-in noted'),
(6, '2026-09-28', '08:57:00', '17:01:00', 'PRESENT', 8.07, NULL),
(7, '2026-09-28', '08:28:00', '16:31:00', 'PRESENT', 8.05, NULL),
(8, '2026-09-28', '09:01:00', '17:04:00', 'PRESENT', 8.05, NULL),
(9, '2026-09-28', '08:33:00', '16:36:00', 'PRESENT', 8.05, NULL),
(10, '2026-09-28', '09:00:00', '17:00:00', 'PRESENT', 8.00, NULL),
(4, '2026-09-29', '09:02:00', '17:00:00', 'PRESENT', 7.97, NULL),
(5, '2026-09-29', '09:58:00', '18:05:00', 'PRESENT', 8.12, NULL),
(6, '2026-09-29', NULL, NULL, 'ABSENT', 0.00, 'Attendance pending correction');

INSERT INTO asset (asset_name, asset_type, serial_number, status) VALUES
('Dell Latitude 7440', 'Laptop', 'EVOQ-LAP-001', 'ASSIGNED'),
('Lenovo ThinkPad T14', 'Laptop', 'EVOQ-LAP-002', 'ASSIGNED'),
('LG UltraFine 27', 'Monitor', 'EVOQ-MON-001', 'ASSIGNED'),
('NVIDIA Jetson Orin', 'Specialized Device', 'EVOQ-DEV-001', 'ASSIGNED'),
('Dell P2422H', 'Monitor', 'EVOQ-MON-002', 'AVAILABLE'),
('HP EliteBook 840', 'Laptop', 'EVOQ-LAP-003', 'AVAILABLE'),
('Intel RealSense D455', 'Specialized Device', 'EVOQ-DEV-002', 'ASSIGNED'),
('Logitech Brio', 'Camera', 'EVOQ-CAM-001', 'AVAILABLE');

INSERT INTO asset_assignment
(asset_id, employee_id, assigned_date, returned_date, status) VALUES
(1, 4, '2026-02-01', NULL, 'ACTIVE'),
(2, 7, '2026-03-15', NULL, 'ACTIVE'),
(3, 5, '2026-04-01', NULL, 'ACTIVE'),
(4, 6, '2026-05-10', NULL, 'ACTIVE'),
(7, 9, '2026-06-20', NULL, 'ACTIVE'),
(5, 8, '2026-01-10', '2026-06-30', 'RETURNED');
