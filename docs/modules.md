# Module workflows and access

This guide describes the implemented application. It covers all four modules at the same level: setup, role access, workflow rules, reporting and remaining limits. Frozen OOAD artifacts are retained separately and are not rewritten to reflect later implementation refinements.

## Shared access and reporting

Every account is linked to an employee and one role: `EMPLOYEE`, `SUPERVISOR` or `MANAGER_ADMIN`. Each authenticated request revalidates the account's enabled status, identity, role and password hash. Account changes invalidate existing sessions on their next request; they do not cancel an operation already running.

Employee, Leave and Attendance team access uses current membership. A Supervisor's permitted direct reports must be active, assigned to that Supervisor and in the same current team. Manager organization reads include inactive employees. Asset access for Employee and Supervisor is own-only; supervising an employee does not grant their equipment history.

Dashboard and Reports connect all four modules. Failed sources display errors and Retry independently; empty tables/zero counts are shown only after successful reads. CSV exports use the authorized response and protect text cells against spreadsheet-formula interpretation. There is no combined productivity or attendance/Leave percentage.

## Employee & Organization Management

### Setup and access

Manager/Admin creates departments and teams, onboards employees and optionally provisions login accounts. Directory, profile and Employee reports show an Employee's own identity, a Supervisor's own identity plus permitted direct reports, or the Manager's organization-wide scope. Supervisors cannot query another Supervisor's direct-report list.

### Workflows and rules

- Managers edit name, email, hire date, title, department, team, assigned supervisor, lifecycle status and linked account role. Emails remain unique. Team and supervisor assignments are optional.
- Supervisor candidates must have an active employee identity and an enabled Supervisor or Manager/Admin account. Official editing excludes self from supervisor selection.
- Creation offers Active/Inactive. Editing retains Active, Inactive, Suspended and On Leave. Inactive creation disables any provisioned login; status changes synchronize the linked account. Inactive identities cannot log in, and disabling an account revokes an existing session on its next request.
- Employees may update their own phone/address; Managers may update any employee's contact details. Contact updates replace both fields, and blank/null clears a field.
- Deactivation opens an equipment review with Return → Deactivate actions. It preserves employee and assignment history. The Manager can deliberately continue with outstanding assets or an unavailable review after the displayed warning; mandatory clearance is not enforced.

### Reporting and limits

Employee reports show scoped workforce, organization and lifecycle facts. Directory filtering/search works within the authorized records. Historical team ownership and compulsory asset clearance remain policy decisions; current scope does not reconstruct past membership. Password recovery, audit trails and forced first-login password changes are not implemented.

## Leave Management

### Setup and access

Manager/Admin creates/edits Leave types and explicitly configures an employee/type's total cumulative entitlement. Every role can view own balances/history and submit own requests. Supervisors and Managers receive only their authorized pending approvals. Manager organization-wide request history is an oversight view and does not grant approval authority for unrelated employees.

### Workflows and rules

- A new balance starts with the entered grant. Changing an existing entitlement preserves used days and sets available days to total entitlement minus usage. It cannot be below used days, negative or above 999.99 days.
- Normal reads do not create grants. There are no implicit default allocations or automatic yearly resets/accruals.
- Requests use inclusive calendar days and existing date, overlap and available-balance validation. Weekends and holidays are not excluded.
- Supervisor or Manager/Admin may approve/reject a pending request only for an active assigned direct report in the approver's same non-null current team. Decisions recheck locked current membership, so a stale queue cannot authorize a transferred, reassigned or inactive employee.
- An active Manager/Admin may approve their own pending Leave, including without a team/supervisor. Normal balance and Attendance protections still apply. Supervisor self-decisions and Manager self-rejection remain forbidden.
- Approval updates usage and coordinates removal of future published shifts in the approved dates. Attendance-protected entries block removal and roll back approval. No automatic cross-team escalation, reassignment or cancellation occurs; excluded requests remain stored and visible to Manager oversight.

### Reporting and limits

Leave reports include request status counts, history and current balances in the permitted scope. Date filters select overlapping requests and retain whole-request calendar days; current balances are independent of that filter. Annual grants, reset/accrual rules, holidays, half-days and resolving requests without an eligible assigned approver remain business decisions.

## Attendance & Scheduling Management

### Setup and access

Scheduling requires teams and eligible employee assignments. Supervisors manage schedules/attendance for permitted active direct reports. Manager/Admin can manage any team and existing attendance exceptions/corrections. Every role can view own published shifts and use own check-in/out; draft shifts are hidden from employees.

### Workflows and rules

- A new schedule is a draft until explicitly published. Drafts do not reserve employee time and may be discarded. Updates to a published schedule take effect immediately. Omitting an entry from an update preserves it; removal must be explicit.
- Validation checks period/date/time limits, overlap and approved Leave. Publication permits one same-day shift per employee/business date. Overnight and multiple daily published shifts are not supported. New entries and draft publication cannot introduce past work dates; historical entries remain readable and cannot move to another past date.
- Self check-in uses the authenticated employee and server time, from scheduled start through the next 30 minutes while the shift has not ended. It records Present; Manager corrections can record existing exceptions such as Late. Approved Leave is detected even after its shift is removed.
- Manual checkout keeps the actual timestamp. Standard hours are actual check-in to the earlier of actual checkout and scheduled end, preserving seconds and rounding HALF_UP to two decimals. Break deductions, overtime and payroll are not calculated.
- A minute-by-minute job checks published shifts from the past 366 days. At shift end it closes an open check-in at scheduled end or records Absent for a missed shift. Completed records are unchanged; repeated processing is idempotent. Approved Leave is skipped and does not create an automatic Leave attendance row.
- All business dates/windows use `EMS_BUSINESS_TIMEZONE` (default `Asia/Colombo`) and the server clock. Browser and JVM default timezones do not define the attendance date.

### Reporting and limits

Attendance reports provide date/team/employee filters, employee summaries, daily facts and CSV using the same scope/calculation. The maximum date-range difference is 366 days. Manager reports may include inactive employees; team filtering uses current membership.

Present, Late, Absent and Leave counts describe stored attendance statuses; Present and Late are separate. Hours sum stored rows, including corrections; missing calendar-day rows are not inferred as Absent. Approved Leave dates are shown separately, including dates without shifts, and overlap with recorded attendance is visible. These facts are not additive. Combined attendance/Leave precedence (D-03), historical team ownership (D-06), and further lateness/correction/audit policy remain unresolved.

## Asset & Equipment Management

### Setup and access

Manager/Admin registers and edits equipment, assigns/returns it and reads employee/asset assignment histories. Employee and Supervisor My Assets show only their own current equipment and assignment history. Asset reports use own scope for these roles; Managers may use organization or selected-employee scope.

### Workflows and rules

- Assignment requires an active employee and an Available asset. Employee and asset locking coordinates the status/link update in one transaction.
- Return and status edits serialize on the asset. Repeated returns conflict. An active assignment must be returned before a manual status change; Assigned is set through assignment.
- Manager-entered condition statuses fit the existing 30-character field. Only Available permits assignment; no additional fixed condition taxonomy is imposed.
- Assignment history retains the asset's current name after return/reassignment, while own inventory remains current-only. History does not expose a new assignee to the previous employee. No assignment-time name snapshot is stored.
- Return/deactivation review preserves history; employee deactivation does not automatically return equipment.

### Reporting and limits

Asset reports provide authorized inventory/status and assignment facts. Current asset names can differ from names at the time of issue. Mandatory clearance, condition taxonomy and historical-name/audit requirements need separate business decisions before enforcement or schema changes.

## First setup across all modules

After [first-administrator setup](../README.md#fresh-database), configure the organization and employee accounts first. Then set Leave types/entitlements, create and publish eligible schedules, and register/assign equipment. A blank installation has no automatic grants, shifts or assets. Use Dashboard and Reports to explore each role's own and permitted views.
