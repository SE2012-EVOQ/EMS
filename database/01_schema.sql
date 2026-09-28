-- EVOQ Employee Management System
-- SE2032 Database Management Systems
-- MySQL 8.x schema

DROP DATABASE IF EXISTS evoq_ems;
CREATE DATABASE evoq_ems CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE evoq_ems;

CREATE TABLE role (
    role_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255)
) ENGINE=InnoDB;

CREATE TABLE department (
    department_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(255)
) ENGINE=InnoDB;

CREATE TABLE team_project (
    team_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(255)
) ENGINE=InnoDB;

CREATE TABLE employee (
    employee_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    department_id BIGINT NOT NULL,
    team_id BIGINT NULL,
    supervisor_id BIGINT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    phone VARCHAR(30),
    address VARCHAR(255),
    hire_date DATE NOT NULL,
    job_title VARCHAR(100) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    CONSTRAINT fk_employee_department
        FOREIGN KEY (department_id) REFERENCES department(department_id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_employee_team
        FOREIGN KEY (team_id) REFERENCES team_project(team_id)
        ON UPDATE CASCADE ON DELETE SET NULL,
    CONSTRAINT fk_employee_supervisor
        FOREIGN KEY (supervisor_id) REFERENCES employee(employee_id)
        ON UPDATE CASCADE ON DELETE SET NULL
) ENGINE=InnoDB;

CREATE INDEX idx_employee_department ON employee(department_id);
CREATE INDEX idx_employee_team ON employee(team_id);
CREATE INDEX idx_employee_supervisor ON employee(supervisor_id);
CREATE INDEX idx_employee_status ON employee(status);

CREATE TABLE user_account (
    user_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_id BIGINT NOT NULL UNIQUE,
    role_id BIGINT NOT NULL,
    username VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT fk_user_account_employee
        FOREIGN KEY (employee_id) REFERENCES employee(employee_id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_user_account_role
        FOREIGN KEY (role_id) REFERENCES role(role_id)
        ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB;

CREATE INDEX idx_user_account_role ON user_account(role_id);

CREATE TABLE leave_type (
    leave_type_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(255)
) ENGINE=InnoDB;

CREATE TABLE leave_balance (
    balance_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    leave_type_id BIGINT NOT NULL,
    available_days DECIMAL(5,2) NOT NULL DEFAULT 0,
    used_days DECIMAL(5,2) NOT NULL DEFAULT 0,
    CONSTRAINT uq_leave_balance_employee_type UNIQUE (employee_id, leave_type_id),
    CONSTRAINT chk_leave_balance_available CHECK (available_days >= 0),
    CONSTRAINT chk_leave_balance_used CHECK (used_days >= 0),
    CONSTRAINT fk_leave_balance_employee
        FOREIGN KEY (employee_id) REFERENCES employee(employee_id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_leave_balance_type
        FOREIGN KEY (leave_type_id) REFERENCES leave_type(leave_type_id)
        ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB;

CREATE INDEX idx_leave_balance_type ON leave_balance(leave_type_id);

CREATE TABLE leave_request (
    leave_request_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    leave_type_id BIGINT NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    reason VARCHAR(500),
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    submitted_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_leave_request_dates CHECK (end_date >= start_date),
    CONSTRAINT fk_leave_request_employee
        FOREIGN KEY (employee_id) REFERENCES employee(employee_id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_leave_request_type
        FOREIGN KEY (leave_type_id) REFERENCES leave_type(leave_type_id)
        ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB;

CREATE INDEX idx_leave_request_employee_status_dates
    ON leave_request(employee_id, status, start_date, end_date);
CREATE INDEX idx_leave_request_type ON leave_request(leave_type_id);

CREATE TABLE schedule (
    schedule_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    team_id BIGINT NOT NULL,
    period_start DATE NOT NULL,
    period_end DATE NOT NULL,
    status VARCHAR(30) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT chk_schedule_period CHECK (period_end >= period_start),
    CONSTRAINT fk_schedule_team
        FOREIGN KEY (team_id) REFERENCES team_project(team_id)
        ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB;

CREATE INDEX idx_schedule_team_period ON schedule(team_id, period_start, period_end);

CREATE TABLE schedule_entry (
    schedule_entry_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    schedule_id BIGINT NOT NULL,
    employee_id BIGINT NOT NULL,
    work_date DATE NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    notes VARCHAR(255),
    CONSTRAINT chk_schedule_entry_time CHECK (end_time > start_time),
    CONSTRAINT fk_schedule_entry_schedule
        FOREIGN KEY (schedule_id) REFERENCES schedule(schedule_id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_schedule_entry_employee
        FOREIGN KEY (employee_id) REFERENCES employee(employee_id)
        ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB;

CREATE INDEX idx_schedule_entry_employee_date_time
    ON schedule_entry(employee_id, work_date, start_time, end_time);
CREATE INDEX idx_schedule_entry_schedule ON schedule_entry(schedule_id);

CREATE TABLE attendance_record (
    attendance_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    attendance_date DATE NOT NULL,
    check_in_time TIME NULL,
    check_out_time TIME NULL,
    status VARCHAR(30) NOT NULL,
    working_hours DECIMAL(5,2) NOT NULL DEFAULT 0,
    notes VARCHAR(255),
    CONSTRAINT chk_attendance_time CHECK (
        check_out_time IS NULL OR check_in_time IS NULL OR check_out_time >= check_in_time
    ),
    CONSTRAINT chk_attendance_hours CHECK (working_hours >= 0),
    CONSTRAINT fk_attendance_employee
        FOREIGN KEY (employee_id) REFERENCES employee(employee_id)
        ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB;

CREATE INDEX idx_attendance_employee_date
    ON attendance_record(employee_id, attendance_date);
CREATE INDEX idx_attendance_date_status
    ON attendance_record(attendance_date, status);

CREATE TABLE asset (
    asset_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    asset_name VARCHAR(150) NOT NULL,
    asset_type VARCHAR(100) NOT NULL,
    serial_number VARCHAR(150) NOT NULL UNIQUE,
    status VARCHAR(30) NOT NULL
) ENGINE=InnoDB;

CREATE INDEX idx_asset_status_type ON asset(status, asset_type);

CREATE TABLE asset_assignment (
    assignment_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    asset_id BIGINT NOT NULL,
    employee_id BIGINT NOT NULL,
    assigned_date DATE NOT NULL,
    returned_date DATE NULL,
    status VARCHAR(30) NOT NULL,
    CONSTRAINT chk_asset_assignment_dates CHECK (
        returned_date IS NULL OR returned_date >= assigned_date
    ),
    CONSTRAINT fk_asset_assignment_asset
        FOREIGN KEY (asset_id) REFERENCES asset(asset_id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_asset_assignment_employee
        FOREIGN KEY (employee_id) REFERENCES employee(employee_id)
        ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB;

CREATE INDEX idx_asset_assignment_employee_status
    ON asset_assignment(employee_id, status);
CREATE INDEX idx_asset_assignment_asset_status
    ON asset_assignment(asset_id, status);
