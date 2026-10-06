-- EVOQ Employee Management System
-- Required role, department, team/project and official employee reference data
USE evoq_ems;

-- 1. Roles
INSERT INTO role (name, description) VALUES
('EMPLOYEE', 'Standard employee access'),
('SUPERVISOR', 'Employee access plus team supervision functions'),
('MANAGER_ADMIN', 'Employee access plus organization-wide administration functions')
ON DUPLICATE KEY UPDATE description=VALUES(description);

-- 2. Departments
INSERT INTO department (name, description) VALUES
('Management', 'Executive and operational management'),
('Engineering', 'Product engineering and research'),
('Development Access', 'Local development login accounts only')
ON DUPLICATE KEY UPDATE description=VALUES(description);

-- 3. Teams / Projects
INSERT INTO team_project (name, description) VALUES
('Management · Operations', 'Operations and executive leadership'),
('Vision · Vision Edge', 'Computer vision and edge machine learning'),
('Platform · EMS Portal', 'Core enterprise management platform engineering'),
('Development Scheduling Team', 'Reserved for local attendance and scheduling development')
ON DUPLICATE KEY UPDATE description=VALUES(description);

-- 4. Initial Leave Types
INSERT INTO leave_type (name, description) VALUES
('Annual', 'Standard paid annual vacation leave'),
('Medical', 'Paid medical and sick leave'),
('Casual', 'Paid personal and casual leave')
ON DUPLICATE KEY UPDATE description=VALUES(description);
