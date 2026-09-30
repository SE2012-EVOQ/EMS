# EVOQ EMS — current project and Attendance & Scheduling handoff

**Updated:** 30 September 2026 (Asia/Colombo)
**Repository:** `/Users/nadhi/Main/Uni/Y2S1 - current/OOAD/Project/EMS`
**Branch:** `Nadhi` (verified)
**Reviewed HEAD:** `7fb7c58` — Merge pull request #14 from SE2012-EVOQ/Shaarugshan.
**State:** Employee & Organization, Leave, Attendance & Scheduling, and Asset modules now have backend APIs and connected frontend workflows. Dashboard and Reports still show placeholders. All 36 existing backend tests and the frontend production build pass; Leave and Asset workflows do not yet have dedicated automated tests.
**Scope:** This update documents the current checkout, local database, and changes since the previous handoff. It preserves the original Attendance & Scheduling implementation details below. No application code or schema was changed by this handoff update.

## What happened after the previous handoff

The first handoff was committed as `635ddf0` on 29 September at 19:36, following the attendance implementation in `c613bce`. Its latest revision was `97d47c1` at 21:59 on the same day. Times below are Asia/Colombo and come from local Git history.

| Date/time | Commit / merge | What changed |
| --- | --- | --- |
| 29 Sep, 19:39 | `bd53382`, PR #8, `roashan` | Employee & Organization backend, employee service tests, API client, directory/profile components, onboarding and editing modals integrated. Includes shared auth entity/repository support for account provisioning. |
| 29 Sep, 19:41 | `aa58e2e`, PR #9, `Nadhi` | Attendance workflow and original handoff integrated into the shared history. |
| 29 Sep, 20:01–21:37 | `ed0c60c`, `1474807` | Asset register/update/status and assignment/return/history backend added; unavailable-asset and repeated-return validation added. |
| 29 Sep, 21:58 | `c2f96a1`, PR #10, `Shaarugshan` | Asset backend integrated. |
| 29 Sep, 21:59–22:07 | `97d47c1`, `1b92352`, PR #11, `Nadhi` | Supervisor Schedule page gained its personal attendance card. Check-in/out controls stay visible but disabled when unavailable, with separate action progress text. The handoff was revised accordingly. |
| 29 Sep, 22:23–22:43 | `0d6292b`, `62da922`, PR #12 | Initial Asset listing page and API service integrated. |
| 29 Sep, 23:32 | `5eceea0` | README updated to the then-current project status. Some of those status statements are now out of date. |
| 30 Sep, 01:21–09:44 | `6a66bb2`, `e0ed520`, PR #13, `feature/leave-management` | Leave entities/repositories/service/controller and connected balances, requests, supervisor decisions, and manager overview UI integrated. |
| 30 Sep, 09:50–09:58 | `d11846d`, `7fb7c58`, PR #14 | Asset registration, assignment, and return controls plus active-assignment display integrated. The commit title says “Complete asset management frontend”, but edit/status/history controls remain absent from the page. |

The checkout now includes all these merges. Attendance backend files and attendance frontend files have no further committed changes after `97d47c1`; `database/` has no changes between the original handoff commit and reviewed HEAD.

### Git and work in progress

- Current branch is `Nadhi`, at `7fb7c58`, matching the locally recorded `origin/main`.
- Local `main` is still at `5eceea0`, four commits behind its recorded upstream. `Nadhi` is two commits ahead of recorded `origin/Nadhi` (`e0ed520`). These are local ref observations; this update did not fetch or push.
- During this review, an independent uncommitted change appeared in `frontend/src/modules/leave/pages/LeavePage.jsx`: it adds a `space-y-5` content wrapper and removes extra padding wrappers/margins around shared Cards. It changes layout, not Leave business logic. That change was preserved and is separate from this documentation edit.
- `handoff.md` is already tracked, despite the `/handoff.md` entry in `.gitignore`; updates to this file still appear in Git diffs.

## What is available now

| Area / owner | Current implementation | Remaining work |
| --- | --- | --- |
| Shared foundation | React 19/Vite frontend, Spring Boot 4.1.1 backend targeting Java 21, MySQL frozen 13-table schema, session login/logout, CSRF, password change, authenticated routes, shared layout/theme and health endpoint. | First legitimate Manager/Admin bootstrap, deployment configuration and complete module authorization review. |
| Employee & Organization — Roashan J. | Directory with search/department/status filters and real employee counts; profiles and direct reports; Manager/Admin onboarding with optional BCrypt-backed account creation; official details, role and status editing; contact editing for self or Manager/Admin. Department/team read and create APIs. | Broader API/security/integration tests and agreed directory/direct-report read scope. Organization creation is exposed through the API/client; there is no dedicated organization management page. |
| Leave — Samarappuli V. D. B | Real balances and own request history; EMPLOYEE submission form; assigned SUPERVISOR pending queue and approve/reject actions; MANAGER_ADMIN all-request overview. | Dedicated tests, Leave/Schedule conflict policy in both directions, concurrent balance/decision handling, and error handling review. |
| Attendance & Scheduling — Ahmed Nadhi | Validated team schedule draft/update/publication, approved-leave conflict checks, published personal shifts, server-driven self check-in/out, scoped attendance history, read-only supervisor team view and Manager/Admin corrections/exceptions. | Existing schema limitations, competing-request verification, deployment time zone and integration checks against new Leave/Employee workflows. Full rules and API details below. |
| Asset & Equipment — V. J. Shaarugshan | Register form, asset list/status summary, available-asset assignment form using employee directory options, active assignments and Return action; backend/client also expose update, status, employee assignments and history. | Page controls for editing/status changes/full history; role and employee scope enforcement; transactional assignment/return consistency, validation and tests. |
| Dashboard / Reports | Routes and presentational pages exist. Dashboard values are `—`; Reports has no connected sources or exports. | Real aggregate APIs, metrics, reports and exports. |

### Employee and shared-auth integration

`employee/` now contains Department, TeamProject, Employee and EmployeeStatus entities, repositories, transactional services, validated DTOs, module errors and controllers. `frontend/src/modules/employees/` contains the directory page, API service, table and profile/onboarding/edit components.

The Employee integration added lookup/mutation support in `auth/RoleRepository.java`, `auth/UserAccount.java` and `auth/UserAccountRepository.java`. Existing shared login/security behavior remains in place. Official/status updates synchronize linked account activity, and official updates can change its role. Attendance still reads employee/team/supervisor IDs through `EmployeeTeamReader`; Leave uses the Employee entity/repository; Assets loads directory options through the Employee API.

### Leave behavior and setup side effects

- `LeaveService.getMyLeave()` and `getTypes()` call `ensureLeaveSetup()`. Reading these endpoints can create missing leave types and balances for **all active employees**, in every profile; this is separate from the dev account/team initializers.
- Default types are Annual Leave (14 opening days), Medical / Sick Leave (10), and Casual Leave (5). Existing balances are retained. These are hardcoded opening balances, with no annual entitlement/reset administration implemented.
- Requested days count every calendar day inclusively; there is no weekend/holiday exclusion or half-day flow.
- Submission derives employee ID from the session, requires an active employee, valid date range/type/configured balance, sufficient available days, and no overlapping PENDING or APPROVED request. Submission does not reserve balance.
- Only the requester's assigned supervisor can approve/reject, and only while PENDING. Approval rechecks available balance, subtracts days and increases used days; rejection leaves balance unchanged. Manager/Admin overview is read only.
- Backend submission is available to any authenticated account; the current page displays the submission form only for EMPLOYEE. Supervisors/managers still fetch their personal balances/history.
- Scheduling reads APPROVED `leave_request` rows directly. Leave approval currently does not query schedules, modify attendance, or remove an existing published shift. Setting attendance status LEAVE still does not create a LeaveRequest.

### Asset behavior and integration limits

- Entity JSON uses `assetId`, `assetName`, `assetType`, `serialNumber`, `status`; assignments use `assignmentId`, `assetId`, `employeeId`, `assignedDate`, `returnedDate`, `assignmentStatus`.
- Registration defaults an omitted/blank status to AVAILABLE. Assignment requires AVAILABLE, records the server date and sets asset status ASSIGNED. Return rejects an already-returned assignment, records the server date and sets the asset AVAILABLE again.
- The page loads assets, employees and all assignments together. It displays active ASSIGNED records; returned records and per-asset history are available via APIs but not displayed as a full history view. The employee picker does not filter inactive employees.
- Asset controllers have no module role annotations and the page has no role checks. Shared security requires authentication and CSRF for mutations, but every signed-in role can currently access organization-wide Asset reads and writes. Asset services do not validate employee activity or intended assignment scope.
- Assignment/return each save asset and assignment separately without a service-level transaction or locking. Atomic rollback and competing assignment protection remain unverified. Asset requests accept entities without DTO/Bean Validation; generic missing-record exceptions also need consistent API status handling.

## Additional REST APIs now present

All paths below are protected by shared session authentication. Listed role rules describe current controller behavior.

| Method and path | Current authorization / purpose |
| --- | --- |
| `GET /api/employees?departmentId&teamId&status&search`, `GET /api/employees/{id}` | Authenticated directory/detail reads; no self/team scope restriction in these handlers. |
| `GET /api/employees/me` | Session user's profile. |
| `GET /api/employees/{id}/direct-reports` | SUPERVISOR or MANAGER_ADMIN; supplied supervisor ID is not restricted to the current user. |
| `POST /api/employees` | MANAGER_ADMIN onboarding, optionally provisioning a login account. |
| `PUT /api/employees/{id}/official`, `PATCH /api/employees/{id}/status` | MANAGER_ADMIN official information / status. |
| `PUT /api/employees/{id}/contact` | Self or MANAGER_ADMIN. |
| `GET /api/organization/departments`, `/departments/{id}`, `/teams`, `/teams/{id}` | Authenticated organization lookups. |
| `POST /api/organization/departments`, `/teams` | MANAGER_ADMIN organization creation. |
| `GET /api/leave/me`, `/types` | Personal overview / type lookup; can initialize types and balances. |
| `POST /api/leave/requests` | Authenticated self submission. |
| `GET /api/leave/supervisor/pending` | SUPERVISOR assigned-report pending queue. |
| `POST /api/leave/requests/{id}/approve`, `/reject` | SUPERVISOR plus assigned-supervisor service check. |
| `GET /api/leave/all` | MANAGER_ADMIN organization overview. |
| `GET /api/assets`, `/assets/{id}` | Authenticated asset listing/details. |
| `POST /api/assets`, `PUT /api/assets/{id}`, `PATCH /api/assets/{id}/status?status=...` | Authenticated register/update/status commands; module role rules absent. |
| `POST /api/asset-assignments/assign?assetId=...&employeeId=...` | Authenticated assignment. |
| `PUT /api/asset-assignments/{assignmentId}/return` | Authenticated return. |
| `GET /api/asset-assignments`, `/employee/{employeeId}`, `/asset/{assetId}` | Authenticated all assignments, employee assignments, asset history. |

## Current local database snapshot

Read-only counts taken on 30 September after the successful backend test run. These describe this machine's database, not seeded records guaranteed on another checkout. No reset or manual business-data insertion was performed for this update.

| Table | Rows |
| --- | ---: |
| `department` | 1 |
| `team_project` | 1 |
| `employee` | 3 |
| `user_account` | 3 |
| `leave_type` | 3 |
| `leave_balance` | 9 |
| `leave_request` | 0 |
| `schedule` | 2 |
| `schedule_entry` | 2 |
| `attendance_record` | 1 |
| `asset` | 1 |
| `asset_assignment` | 2 |

The older demo snapshot below had one schedule, one schedule entry, one attendance record, no leave requests and no assets. Current counts show additional schedule and Asset data plus configured Leave balances; they do not by themselves verify the full workflows or establish who created those rows.

## Current verification — 30 September 2026

- **Backend suite: 36 passed, zero failures/errors/skips**, against local MySQL. Breakdown: Attendance service 8, Schedule service 7, Attendance API security 5, Schedule API security 3, MySQL attendance workflow 1, Employee service 5, Password 2, Auth HTTP flow 3, Account persistence 1, application context 1. The five Employee tests cover onboarding, duplicate email rejection, self-supervision rejection, contact updates and linked-account deactivation.
- The first sandboxed run passed 34 tests but could not open the local MySQL socket (`SocketException: Operation not permitted`), causing the two database-backed context tests to error. Rerunning with permitted local database access passed all 36. This was an execution restriction, not a demonstrated application regression.
- Backend `./mvnw -q -DskipTests package` passed. The context tests validate current entity mappings against the frozen schema.
- Frontend `npm run build` passed, including the current uncommitted Leave layout change. Vite 7.3.6 generated approximately 895 kB JS (244 kB gzip), with its existing >500 kB chunk advisory. The frontend has no configured test or lint script.
- `git diff --check` passed for the documentation and current working-tree changes.
- Surefire reports are in `backend/target/surefire-reports/`. The MySQL attendance workflow creates test schedule/attendance rows transactionally and rolls them back; dev application startup can still restore the reserved demo accounts/team.
- No new browser walkthrough was performed for this documentation update. Original attendance demo evidence is retained below. Passing this suite does not demonstrate Leave/Asset workflows, for which no dedicated test classes exist.

To rerun backend tests from `backend/` on this machine, load the ignored local environment without printing its contents:

```bash
set -a
source .env
set +a
./mvnw -q -DargLine=-javaagent:/Users/nadhi/.m2/repository/org/mockito/mockito-core/5.23.0/mockito-core-5.23.0.jar test
```

The Mockito agent path is specific to this machine/cache; adjust it on another machine. MySQL, the existing schema, database credentials and the dev demo password must be available for the integration/context tests.

## Running and continuing work

1. Start the existing MySQL database. For a new machine, follow README setup; **do not rerun `database/01_schema.sql` against data you need**, because it drops and recreates `evoq_ems`. `02_sample_data.sql` seeds only the three roles.
2. Copy `backend/.env.example` to ignored `backend/.env` if absent and set local database credentials. From `backend/`, load it with `set -a; source .env; set +a`, then run `./mvnw spring-boot:run`. Defaults: API `http://localhost:8080/api`, health `http://localhost:8080/api/health`.
3. From `frontend/`, install dependencies if needed, then `npm run dev`; open `http://localhost:5173`. Set `VITE_API_BASE_URL` if changing backend address and `FRONTEND_ORIGIN` if changing frontend origin.
4. With the example dev settings, use `demo.manager`, `demo.supervisor`, or `demo.employee`; the public example password is `EvoqDemo2026!`, and the actual local value comes from `DEV_DEMO_PASSWORD`. The dev initializers create/restore reserved accounts and the scheduling team, not schedule/attendance/Asset/Leave request rows. Leave reads can separately create default types/balances.
5. Configure the JVM's intended business time zone for Attendance (`Clock.systemDefaultZone()`), for example `TZ=Asia/Colombo` in the launch environment. JDBC `serverTimezone=UTC` does not itself set that Clock's zone.

### Remaining work and suggested order

1. Agree and enforce module authorization and read scope, especially Assets and Employee directory/direct-report APIs. Keep existing Attendance self/team/admin boundaries intact.
2. Verify new cross-module flows: onboard an active employee with account, team and supervisor; publish its schedule; record attendance; submit/decide Leave; register/assign/return equipment. Cover denied actions and inactive employees as well as the successful path.
3. Define what happens if Leave is approved after a shift is published. Current schedule create/update/publish checks approved Leave, but reverse approval and attendance check-in do not reconcile it. Add dedicated Leave/Asset API and MySQL workflow tests, including simultaneous requests.
4. Review Leave error responses: `LeaveService` throws `ResponseStatusException`, while shared `ApiExceptionHandler` has a catch-all `Exception` handler and no dedicated mapping for it. Source inspection suggests intended 400/403/404/409 errors may become generic 500 responses; this was not reproduced by the existing suite. Also review parallel default-balance setup and decision updates, and the UI's swallowed supervisor/manager read errors that currently look like empty queues.
5. Make Asset assignment/return transactional and validate its inputs/employee eligibility; add missing edit/status/history UI. Preserve the frozen schema unless the team explicitly agrees to change it.
6. Connect Dashboard metrics and Reports/exports; implement the first legitimate Manager/Admin bootstrap and deployment configuration. Forced first-login password change is still deferred because the frozen schema has no supporting flag.
7. Update README statements that Leave is still an empty state and Assets is listing-only. They describe `5eceea0`, before PRs #13/#14, and are no longer current. This task only updates the handoff.

## Attendance & Scheduling implementation retained from the previous handoff

The original module work did not alter the frozen schema, shared authentication or teammate-owned business modules. Later teammate merges added their modules and shared auth support as documented above; the original boundary statement applies to the Attendance work, not the whole current repository.

## Implemented workflow

Supervisor creates or updates a team schedule and publishes it. Users see only their own published shifts in their personal attendance card; supervisors also see the permitted team schedule planner. An authenticated user can check in only for their own published shift on the server's current date and only from the scheduled start through exactly 30 minutes after it, inclusive. The server creates a PRESENT row with its own employee/date/time and `0.00` hours. After that check-in, the employee can check out on the same server date; the server stores checkout and calculates hours rounded HALF_UP to two decimals.

The frontend reads the server's today state for check-in availability. Check in and Check out remain visible but disabled when the action is unavailable, including for supervisors with no personal published shift. Commands accept no client attendance payload. Attendance reads remain scoped to self, supervisor's active direct reports in the supervisor's team, or organization-wide for MANAGER_ADMIN. Supervisor team attendance is read only. Manager/Admin can correct rows or create an exceptional record for missed/incorrect attendance; notes are optional. Attendance LEAVE does not call LeaveRequest. No approval, biometric, GPS, or automatic lateness threshold was added. A valid self check-in is PRESENT; LATE remains an administrative status.

## Schedule rules and frozen-schema consequence

- `Schedule` maps `schedule`; `ScheduleEntry` maps `schedule_entry`; both use scalar IDs and make no schema changes.
- Period end must be on/after period start. Entry date must be within the period; end time must be after start time on the same date. Employees must be active, assigned to the schedule team, and an active direct report of the authorizing supervisor.
- Overlap is `existing.start_time < new.end_time AND existing.end_time > new.start_time`, same employee and date; shifts that touch at an endpoint do not overlap.
- Create/update/publish check approved leave with an inclusive date-range query. Leave integration is read only.
- Drafts may contain multiple non-overlapping entries for an employee/date. Publishing rejects more than one entry per employee/date and rejects an already-published entry on that date in another schedule.
- The frozen attendance table has no `schedule_entry_id`, and no unique employee/date key. Attendance therefore uses the approved v1 policy of one row per employee/date. Employee-row locks serialize check-in, exceptional creation, schedule update, and publication within this module; direct SQL can bypass this service-only invariant.
- If attendance exists for an employee/date, schedule update cannot change or remove that entry's employee, date, start, or end. Notes remain editable. No revision history was introduced.
- The DATE + TIME schema cannot model checkout across midnight. Checkout is accepted only against an open same-server-date record and must be later than check-in. Such cases require Manager/Admin handling.
- Time uses an injectable module `Clock` configured with the server's default time zone. Tests use fixed UTC clocks. Deployment must set the intended business time zone.

## Attendance & Scheduling REST API

| Method and path | Authorization | Behavior |
| --- | --- | --- |
| `GET /api/schedules/me?from&to` | Authenticated | Own published schedule entries |
| `GET /api/schedules/teams` | SUPERVISOR | Authorized team option |
| `GET /api/schedules/teams/{teamId}/employees` | SUPERVISOR | Active direct reports available for assignment |
| `GET /api/schedules/teams/{teamId}?from&to&includeDrafts` | SUPERVISOR | Permitted team schedule and entries |
| `GET /api/schedules/{scheduleId}` | SUPERVISOR or MANAGER_ADMIN | Authorized schedule details |
| `POST /api/schedules` | SUPERVISOR | Create validated draft |
| `PUT /api/schedules/{scheduleId}` | SUPERVISOR | Update period/entries; published status is retained |
| `POST /api/schedules/{scheduleId}/publish` | SUPERVISOR | Revalidate and publish |
| `GET /api/attendance-records/today` | Authenticated | Today's published entry, attendance, and server action state |
| `POST /api/attendance-records/check-in` | Authenticated | Self check-in; empty body |
| `POST /api/attendance-records/check-out` | Authenticated | Self check-out; empty body |
| `GET /api/attendance-records/me?from&to` | Authenticated | Own attendance |
| `GET /api/attendance-records/teams` and `/teams/{teamId}` | SUPERVISOR | Team/direct-report attendance |
| `GET /api/attendance-records` and `/employees` | MANAGER_ADMIN | Organization attendance and employee options |
| `POST /api/attendance-records/exceptions` | MANAGER_ADMIN | Exceptional record creation |
| `PUT /api/attendance-records/{id}` | MANAGER_ADMIN | Correct status/times/notes; employee/date stay fixed |

## Attendance & Scheduling files

### Added

```text
backend/src/main/java/com/evoq/ems/attendance/config/AttendanceClockConfig.java
backend/src/main/java/com/evoq/ems/attendance/domain/Schedule.java
backend/src/main/java/com/evoq/ems/attendance/domain/ScheduleEntry.java
backend/src/main/java/com/evoq/ems/attendance/repository/ScheduleRepository.java
backend/src/main/java/com/evoq/ems/attendance/repository/ScheduleEntryRepository.java
backend/src/main/java/com/evoq/ems/attendance/service/ScheduleService.java
backend/src/main/java/com/evoq/ems/attendance/web/ScheduleController.java
backend/src/main/java/com/evoq/ems/attendance/web/ScheduleDtos.java
backend/src/test/java/com/evoq/ems/attendance/ScheduleServiceTests.java
backend/src/test/java/com/evoq/ems/attendance/ScheduleApiSecurityTests.java
backend/src/test/java/com/evoq/ems/attendance/AttendanceMySqlWorkflowTests.java
frontend/src/modules/attendance/services/scheduleService.js
frontend/src/modules/attendance/components/ScheduleEditor.jsx
frontend/src/modules/attendance/components/TodayAttendanceActions.jsx
```

### Modified

```text
backend/src/main/java/com/evoq/ems/attendance/domain/AttendanceRecord.java
backend/src/main/java/com/evoq/ems/attendance/integration/ApprovedLeaveReader.java
backend/src/main/java/com/evoq/ems/attendance/integration/EmployeeTeamReader.java
backend/src/main/java/com/evoq/ems/attendance/repository/AttendanceRecordRepository.java
backend/src/main/java/com/evoq/ems/attendance/service/AttendanceService.java
backend/src/main/java/com/evoq/ems/attendance/web/AttendanceController.java
backend/src/main/java/com/evoq/ems/attendance/web/AttendanceDtos.java
backend/src/main/java/com/evoq/ems/attendance/web/AttendanceModuleExceptionHandler.java
backend/src/test/java/com/evoq/ems/attendance/AttendanceServiceTests.java
backend/src/test/java/com/evoq/ems/attendance/AttendanceApiSecurityTests.java
frontend/src/modules/attendance/components/AttendanceEditor.jsx
frontend/src/modules/attendance/components/AttendanceTable.jsx
frontend/src/modules/attendance/components/ScheduleCalendar.jsx
frontend/src/modules/attendance/pages/AttendancePage.jsx
frontend/src/modules/attendance/pages/SchedulePage.jsx
frontend/src/modules/attendance/services/attendanceService.js
```

The existing dev-only `DevAttendanceTeamInitializer` was retained unchanged. It provides `Development Scheduling Team`, assigns `demo.supervisor`, assigns `demo.employee`, and sets the employee's supervisor. It creates no schedule, schedule entry, attendance, leave, or asset records.

No `ScheduleConflictService` was added; schedule, overlap, and leave validation are kept together in `ScheduleService`, with the read-only leave query in `ApprovedLeaveReader`.

## Historical verification — 29 September 2026

These results and demo details were recorded in the previous handoff. Use the 30 September verification and database snapshot above for current status.

- Full backend suite against local MySQL: **31 passed** (8 Attendance service, 7 Schedule service, 5 Attendance API security, 3 Schedule API security, 1 MySQL workflow, 2 password, 3 auth HTTP flow, 1 account persistence, 1 application context). The MySQL workflow test creates and publishes a schedule, checks in and out with fixed server clocks, verifies `8.10` hours, and rejects repeated check-in/check-out. Its transaction rolls back.
- Rechecked after the demo: `set -a; source .env; ./mvnw -q -DargLine=-javaagent:/Users/nadhi/.m2/repository/org/mockito/mockito-core/5.23.0/mockito-core-5.23.0.jar test` exited 0, and every Surefire report shows zero failures and errors. The MySQL workflow test requires the local database and dev credentials to be available; a run without database access cannot pass that integration test.
- Backend: `./mvnw -q -DskipTests package` passed.
- Frontend: `npm run build` passed after the supervisor visibility fix. Vite reported its large-bundle advisory for the ~877 kB generated JS bundle.
- `git diff --check` passed.
- Branch verified as exactly `Nadhi` before final delivery.
- The local MySQL application context passed Hibernate schema validation against the existing frozen schema. The dev fixture assigned the demo supervisor and employee to `Development Scheduling Team` and set the employee's supervisor. The fixture itself created no business rows.
- Manual browser demo on the dev profile: `demo.supervisor` created a draft with a 2026-09-29 19:00–21:00 entry for `demo.employee` and published it. The employee saw that published shift, checked in during the server window, checked out, and saw the completed PRESENT row in attendance history. The supervisor saw the row in Team attendance; `demo.manager` saw it in All records and the correction form. Immediate checkout showed `0.00` hours as expected for a duration under half a minute. The local database counts recorded after that demo were one schedule, one schedule entry, one attendance record, zero leave requests, and zero assets. These three business rows were produced through the real UI workflow, not seeded by the fixture.
- At the original delivery, no frozen schema SQL, shared auth, or teammate-owned Employee, Leave, or Asset business file differed from the then-current `main`. This was a historical branch-boundary check; subsequent merges have added those modules.

## Remaining integration limitation

The service-only employee/date uniqueness and row locks are not backed by a new schema constraint, by design, because the schema is frozen. Sequential duplicate rejection was verified against MySQL, but simultaneous competing requests were not load tested. Legacy/direct SQL can bypass the service invariant. The frozen DATE + TIME model still cannot represent a checkout after midnight accurately.
