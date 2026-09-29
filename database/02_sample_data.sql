-- EVOQ Employee Management System
-- Required role reference data. No fictional employees or demo records are seeded.
USE evoq_ems;

INSERT INTO role (name, description) VALUES
('EMPLOYEE', 'Standard employee access'),
('SUPERVISOR', 'Employee access plus team supervision functions'),
('MANAGER_ADMIN', 'Employee access plus organization-wide administration functions');
