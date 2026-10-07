-- Non-destructive runtime role seed for a fresh non-dev install.
-- Select the intended existing database in the client before applying this file.
INSERT INTO role (name, description) VALUES
('MANAGER_ADMIN', 'Manager and system administrator'),
('SUPERVISOR', 'Team supervisor'),
('EMPLOYEE', 'Employee self service')
ON DUPLICATE KEY UPDATE name = VALUES(name);
