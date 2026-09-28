USE evoq_ems;

-- Negative Constraint Test 1: UNIQUE constraint on employee.email
-- Expected result: FAIL with duplicate-entry error because the email already exists.
INSERT INTO employee (
    department_id,
    team_id,
    first_name,
    last_name,
    email,
    hire_date,
    job_title,
    status
)
VALUES (
    1,
    1,
    'Test',
    'Duplicate',
    'arjun.jayasinghe@evoq.example',
    '2026-01-01',
    'Test Employee',
    'ACTIVE'
);

-- Negative Constraint Test 2: CHECK constraint on schedule_entry time range
-- Expected result: FAIL because end_time must be greater than start_time.
INSERT INTO schedule_entry (
    schedule_id,
    employee_id,
    work_date,
    start_time,
    end_time,
    notes
)
VALUES (
    1,
    4,
    '2026-10-01',
    '17:00:00',
    '09:00:00',
    'Constraint validation test'
);
