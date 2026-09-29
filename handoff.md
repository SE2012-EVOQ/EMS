# EVOQ EMS — Ahmed Nadhi Attendance & Scheduling handoff

**Updated:** 29 September 2026 (Asia/Colombo)
**Repository:** `/Users/nadhi/Main/Uni/Y2S1 - current/OOAD/Project/EMS`
**Branch:** `Nadhi` (verified)
**State:** Attendance & Scheduling implementation, integration assertions, and supervisor personal attendance UI are on branch `Nadhi`.
**Boundary:** Frozen schema, shared authentication, and teammate-owned Employee, Leave, and Asset business modules were not modified.

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

## REST API

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

## Files

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

## Verification

- Full backend suite against local MySQL: **31 passed** (8 Attendance service, 7 Schedule service, 5 Attendance API security, 3 Schedule API security, 1 MySQL workflow, 2 password, 3 auth HTTP flow, 1 account persistence, 1 application context). The MySQL workflow test creates and publishes a schedule, checks in and out with fixed server clocks, verifies `8.10` hours, and rejects repeated check-in/check-out. Its transaction rolls back.
- Rechecked after the demo: `set -a; source .env; ./mvnw -q -DargLine=-javaagent:/Users/nadhi/.m2/repository/org/mockito/mockito-core/5.23.0/mockito-core-5.23.0.jar test` exited 0, and every Surefire report shows zero failures and errors. The MySQL workflow test requires the local database and dev credentials to be available; a run without database access cannot pass that integration test.
- Backend: `./mvnw -q -DskipTests package` passed.
- Frontend: `npm run build` passed after the supervisor visibility fix. Vite reported its large-bundle advisory for the ~877 kB generated JS bundle.
- `git diff --check` passed.
- Branch verified as exactly `Nadhi` before final delivery.
- The local MySQL application context passed Hibernate schema validation against the existing frozen schema. The dev fixture assigned the demo supervisor and employee to `Development Scheduling Team` and set the employee's supervisor. The fixture itself created no business rows.
- Manual browser demo on the dev profile: `demo.supervisor` created a draft with a 2026-09-29 19:00–21:00 entry for `demo.employee` and published it. The employee saw that published shift, checked in during the server window, checked out, and saw the completed PRESENT row in attendance history. The supervisor saw the row in Team attendance; `demo.manager` saw it in All records and the correction form. Immediate checkout showed `0.00` hours as expected for a duration under half a minute. Current local database counts after the demo are one schedule, one schedule entry, one attendance record, zero leave requests, and zero assets. These three business rows were produced through the real UI workflow, not seeded by the fixture.
- No frozen schema SQL, shared auth, or teammate-owned Employee, Leave, or Asset business file differs from `main`.

## Remaining integration limitation

The service-only employee/date uniqueness and row locks are not backed by a new schema constraint, by design, because the schema is frozen. Sequential duplicate rejection was verified against MySQL, but simultaneous competing requests were not load tested. Legacy/direct SQL can bypass the service invariant. The frozen DATE + TIME model still cannot represent a checkout after midnight accurately.
