# EVOQ EMS handoff

**Updated:** 6 October 2026 (Asia/Colombo)

**Branch:** `Nadhi`

**Reviewed HEAD:** `3883c0d` (`docs: track and refresh project handoff`)

**Previous handoff:** 3 October 2026; earlier implementation review covered `00aacfc`.

**Schema:** The existing 13-table MySQL schema is unchanged.

This handoff is ready for review of Ahmed Nadhi's Attendance & Scheduling second cleanup slice: AS-03 working-hours consistency, D-05 backdating protection, and SH-03/D-07 business timezone. The user approved the first slice (AS-01, immediate AS-02, and AS-04); those changes are preserved. Both slices remain uncommitted on `Nadhi`. The latest full backend result is **81 passed**. No schema, Employee/Leave/Asset business logic, shared authentication/security, Dashboard, or Reports changes were made in either slice. Stop for review; do not commit, push, or continue to AS-05/AS-06/AS-07.

## Initial audit/setup on 6 October

- Reviewed the frontend, backend, schema, test coverage, Attendance use-case scenario, UML content, README, and previous handoff. Code review findings still need focused browser reproduction and regression checks during implementation.
- Installed the required Intel macOS runtimes and project dependencies. The existing frontend packages were for Apple Silicon; `npm ci` reinstalled the locked dependencies for this Mac without changing the lockfile.
- Initialized a new private local MySQL instance and the project schema, preserving `backend/.env` and applying its existing password to the local root account. No old database was dropped. The older 3 October database counts describe a previous environment and are replaced by the current snapshot below.
- Verified the production frontend build, packaged backend, all 58 backend tests, and a real backend health response.
- Added the module/shared planner backlog and explicit decision dependencies below. This document update is the only tracked project change from this work.

Implementation history remains in Git: `5e2cfe2` adjusted Employee/Leave UI; `00aacfc` implemented scheduling, attendance, leave integration, regression tests, README updates, and use-case diagram edits; `3883c0d` tracked the previous handoff.

## First post-audit cleanup — 6 October 2026, approved and uncommitted

**Historical scope/results of the first slice:** Only AS-01, AS-02 today's state, and AS-04 editor defaults. The user subsequently approved these changes and authorized the second slice below. No commit or push. Employee, Leave, and Asset business logic, shared authentication/security, `database/01_schema.sql`, Dashboard, Reports, working-hours policy (AS-03), timezone policy, and backend backdating authorization were not changed during this first slice. The second slice supersedes its browser-local date and historical default behavior.

### Pre-edit checks and working tree

- Ran `git status`, `git branch --show-current`, read this latest handoff, and inspected Attendance/Scheduling frontend, backend, DTO/controller, read-only leave reader, and existing tests.
- Branch was and remains `Nadhi`; HEAD remains `3883c0d`.
- The only pre-existing uncommitted change was `handoff.md`, containing the earlier 6 October audit/setup update. That content was preserved and extended here.
- `git diff HEAD -- database/01_schema.sql` was empty before editing and after implementation. No schema script was executed.

### Exact files modified

1. `backend/src/main/java/com/evoq/ems/attendance/service/AttendanceService.java`
2. `backend/src/test/java/com/evoq/ems/attendance/AttendanceServiceTests.java`
3. `backend/src/test/java/com/evoq/ems/attendance/AttendanceApiSecurityTests.java`
4. `frontend/src/modules/attendance/components/ScheduleCalendar.jsx`
5. `frontend/src/modules/attendance/components/ScheduleEditor.jsx`
6. `frontend/src/modules/attendance/components/TodayAttendanceActions.jsx`
7. `frontend/src/styles/global.css` — only the Attendance calendar grid rule.
8. `handoff.md` — preserves the earlier uncommitted update and adds cleanup results.

### Implemented behavior

- **AS-01:** `ScheduleCalendar` derives the visible date list from the returned entries. Its headers, shift cells, and optional team coverage all render that same list, but shared CSS previously allocated exactly five date tracks. The component now sets `86px repeat(dates.length, minmax(150px, 1fr))`; CSS retains only `display: grid`. Existing dynamic minimum width and horizontal overflow remain. No page redesign or change to which dates are shown.
- **AS-02:** `AttendanceService.today` checks the existing read-only `ApprovedLeaveReader` for the existing server-clock date before falling back to a missing-shift state. With no attendance record, approved leave returns `checkInState: ON_LEAVE` whether or not a published entry remains. Without leave or a shift, it returns `NO_SCHEDULE`; a normal published shift still follows its check-in window. The existing DTO/API contract already represents `ON_LEAVE`, so no DTO or endpoint change was needed. Leave with no shift returns `scheduled: false`, null shift fields, disabled check-in/out, and null attendance. The UI reads the backend state and shows an **Approved leave** heading plus **Approved leave today**, rather than a **No shift assigned** heading. No attendance row is created, and no leave decision/write is performed. Existing attendance-record precedence is preserved pending D-03.
- **AS-04:** The initial new shift and **Add entry** both clamp the existing browser-local today date to the currently selected schedule period: today inside the period; period start for a wholly future period; period end for a wholly historical period. Add entry uses the current edited period bounds. Existing entry dates are preserved; existing work-date min/max and backend period validation remain. Explicit historical scheduling is still permitted by the existing backend; this fix does not choose a backdating policy or configure a business timezone.

### Tests and exact results

Added three `AttendanceServiceTests` regressions:

- approved leave spanning today + no published shift → `ON_LEAVE`, null shift/attendance, no repository save;
- no approved leave + no published shift → `NO_SCHEDULE`;
- published shift + no approved leave at scheduled start → `AVAILABLE` with check-in enabled.

Added an employee today-response API test for `ON_LEAVE` with no shift/attendance, and extended the existing anonymous-access test to cover `/api/attendance-records/today`. No frontend test framework exists, and none was added.

| Verification | Result |
| --- | --- |
| `ScheduleServiceTests` | 14 passed |
| `AttendanceServiceTests` | 15 passed |
| `ScheduleApiSecurityTests` | 5 passed |
| `AttendanceApiSecurityTests` | 6 passed |
| Targeted run total | **40 passed; 0 failures, errors, or skips; exit 0** |
| Full backend suite with local MySQL | **62 passed; 0 failures, errors, or skips; exit 0**, including all 3 transactional MySQL workflow tests |
| `./mvnw -q -DskipTests package` | **Passed; exit 0** |
| `npm run build` | **Passed; exit 0**; existing >500 kB JS chunk warning remains |
| `git diff --check` | **Passed; exit 0** |
| Full diff inspection | Reviewed application/test changes and the complete handoff diff, including its pre-existing update |

Backend commands used the existing `.env` and Mockito agent setup documented below. Targeted command: `./mvnw -q -DargLine="-javaagent:$HOME/.m2/repository/org/mockito/mockito-core/5.23.0/mockito-core-5.23.0.jar" -Dtest=ScheduleServiceTests,AttendanceServiceTests,ScheduleApiSecurityTests,AttendanceApiSecurityTests test`. Full-suite command omits `-Dtest`. Maven output was kept in `/private/tmp/ems-cleanup-targeted-tests.log`, `/private/tmp/ems-cleanup-full-tests.log`, and `/private/tmp/ems-cleanup-package.log`; counts were checked from Surefire XML reports.

**Browser verification:** Mounted the actual `SchedulePage`, calendar, editor, and today-actions components in a temporary local Vite harness using mocked API responses. Inspected 1-, 3-, 5-, and 7-day headers/shift/coverage alignment visually and checked rendered grid tracks/positions. At a 390px viewport, the calendar retained all seven tracks in its scroll container; horizontal scrolling reached later days with all rows aligned. Checked initial and added shifts for today's default, future-period start, historical-period end, and changed editor bounds. Loading an existing 29-entry schedule and adding a shift preserved all 29 original dates, including historical entries. An employee fixture with `ON_LEAVE`, no shift, and no attendance displayed **Approved leave** with both attendance actions disabled. This is browser rendering verification with fixtures, not a live leave-approval/login workflow. The temporary harness files were removed, the Vite server stopped, and the temporary viewport override reset. Screenshots are saved outside the repository in this chat's visualization directory (`seven-day-calendar-verification.jpg`, `approved-leave-verification.jpg`).

### Final Git state and unresolved decisions

`git status --short` after cleanup (all tracked modifications; no new untracked files):

```text
 M backend/src/main/java/com/evoq/ems/attendance/service/AttendanceService.java
 M backend/src/test/java/com/evoq/ems/attendance/AttendanceApiSecurityTests.java
 M backend/src/test/java/com/evoq/ems/attendance/AttendanceServiceTests.java
 M frontend/src/modules/attendance/components/ScheduleCalendar.jsx
 M frontend/src/modules/attendance/components/ScheduleEditor.jsx
 M frontend/src/modules/attendance/components/TodayAttendanceActions.jsx
 M frontend/src/styles/global.css
 M handoff.md
```

No new policy decision was made in the first slice. D-02/D-05/D-07 were unresolved at that point and have since received explicit V1 instructions, recorded below. D-03 (approved leave against existing/worked attendance and report precedence) remains open. The first slice's historical-period-end default has been superseded by the approved backend backdating rule.

## Second post-audit cleanup — 6 October 2026, awaiting review

**Scope:** AS-03, D-05, and SH-03/D-07 only. Preserve the approved first slice. No commit or push, destructive database initialization, schema change, shared security change, attendance audit history, lateness change, overnight/multiple-shift expansion, or Dashboard/Reports implementation. Leave integration remains read-only from Attendance/Scheduling.

### Pre-edit checks and explicit policy answers

- Ran `git status`, `git branch --show-current`, read the latest handoff, and inspected all eight existing first-slice modifications before editing. No unexpected modifications were present. Branch `Nadhi`, HEAD `3883c0d`, and the unchanged schema were confirmed.
- D-02: scheduled self-attendance standard hours run from actual server check-in to `min(actual checkout, scheduled end)`, retaining seconds and HALF_UP two-decimal rounding. No lunch deduction, overtime, or payroll logic.
- D-05 creation: a new entry cannot have a date before the current business date.
- User clarification for publication: **reject every past date when publishing drafts**, including legacy drafts and drafts that became historical while waiting. Existing published history remains readable.
- User clarification for updates: **preserve an existing entry's original historical date; block moving it to a different past date**. New historical entries cannot be added to an old schedule. No automatic rewriting or deletion of historical rows.
- D-07: configurable business timezone, default **Asia/Colombo**, with deterministic fixed-Clock tests and no JVM-wide timezone change.

### Exact files modified in this slice

1. `backend/src/main/java/com/evoq/ems/attendance/config/AttendanceClockConfig.java`
2. `backend/src/main/java/com/evoq/ems/attendance/service/AttendanceService.java`
3. `backend/src/main/java/com/evoq/ems/attendance/service/ScheduleService.java`
4. `backend/src/main/resources/application.properties`
5. `backend/src/test/java/com/evoq/ems/attendance/AttendanceServiceTests.java`
6. `backend/src/test/java/com/evoq/ems/attendance/ScheduleServiceTests.java`
7. `backend/src/test/java/com/evoq/ems/attendance/AttendanceReconciliationServiceTests.java`
8. `backend/src/test/java/com/evoq/ems/attendance/AttendanceMySqlWorkflowTests.java`
9. `backend/src/test/java/com/evoq/ems/attendance/config/AttendanceClockConfigTests.java` — new, untracked.
10. `frontend/src/modules/attendance/pages/SchedulePage.jsx`
11. `frontend/src/modules/attendance/components/ScheduleEditor.jsx`
12. `frontend/src/modules/attendance/components/TodayAttendanceActions.jsx`
13. `frontend/src/modules/attendance/pages/AttendancePage.jsx`
14. `frontend/src/modules/attendance/components/AttendanceEditor.jsx`
15. `handoff.md`

Three other files remain modified from the approved first slice and were not edited in this slice: `backend/src/test/java/com/evoq/ems/attendance/AttendanceApiSecurityTests.java`, `frontend/src/modules/attendance/components/ScheduleCalendar.jsx`, and `frontend/src/styles/global.css`. Total working tree: 17 tracked modified files and one new untracked test file.

### Implementation and API/UI behavior

- **AS-03:** Manual checkout preserves its actual server-recorded timestamp but caps the standard-hours calculation at the published shift's end. Automatic reconciliation already closes at scheduled end using the same seconds-based `calculateHours` helper. For a 09:05:06 check-in, checkout at 16:40:13 yields 7.59; at 17:00:00 or 17:20:37 yields 7.92. Duplicate manual checkout and manual checkout after reconciliation remain conflicts; reconciliation after a completed manual checkout leaves the actual timestamp and capped hours unchanged.
- **D-05 backend:** `ScheduleService` receives the business Clock. New rows and changed dates are validated before entry mutation against a single business date for the operation. Existing historical dates may remain unchanged under existing scope/attendance/period validation. Draft publication rejects any historical work date before status mutation. Reads and existing published schedules are not rejected merely because they are historical. Period validation and explicit entry removal remain intact.
- **SH-03/D-07:** `ems.business-timezone=${EMS_BUSINESS_TIMEZONE:Asia/Colombo}` configures `Clock.system(ZoneId.of(...))`. Spring configuration can override the property; the environment override is `EMS_BUSINESS_TIMEZONE`. Invalid zone configuration fails startup rather than silently falling back. Attendance today, check-in windows, checkout, scheduling validation, reconciliation, and the existing leave/schedule coordinator all consume this Clock. No JVM-global timezone setting is changed.
- **Business-date synchronization:** No endpoint or DTO extension was needed. The existing `/api/attendance-records/today` response's `date` is authoritative. Today actions pass it to SchedulePage, whose initial/reset ranges use calendar arithmetic from that ISO date. Opening create/edit refreshes the server date; a range that has become wholly historical cannot open a new editor. New periods/rows default to the later of business today and period start, with native date limits and submit validation. Existing historical rows retain their original date. Historical schedules remain viewable.
- **Attendance frontend:** The initial attendance month and new Manager/Admin exception date use the same server response. Opening an exception refreshes that date. Existing correction dates and historical exception inputs remain available; no new attendance-correction policy was introduced. The UI continues to use backend today/check-in states and backend working hours without reconstructing those rules locally.
- **Approved first slice preserved:** Dynamic calendar tracks, responsive overflow, independent approved-leave detection, and the Approved leave UI remain. No leave decision or automatic LEAVE attendance record is introduced.

### Tests added/changed and exact results

- `AttendanceServiceTests`: deterministic before/end/after checkout cases with seconds and an exact HALF_UP rounding boundary; actual timestamps retained; duplicate manual conflict; Colombo business-midnight today/check-in availability; existing unscheduled exception behavior preserved. The prior three approved-leave/today regressions remain.
- `ScheduleServiceTests`: business-date new-entry rejection at a UTC/Colombo boundary; adding a historical row to an existing schedule; changed past date rejected before entry mutation; original historical date retained; legacy historical draft publication rejection; draft becoming historical at midnight; existing published historical read/publication behavior.
- `AttendanceReconciliationServiceTests`: fixed-Clock automatic end checkout, idempotence and manual-after-job conflict; pending range based on the configured business date; job after manual checkout preserves the actual time and capped hours.
- `AttendanceClockConfigTests`: default Colombo with JVM timezone unchanged, configuration override to Pacific/Auckland, and invalid-zone failure.
- `AttendanceMySqlWorkflowTests`: late manual checkout expectation updated from 8.10 to capped 7.88 while retaining actual 17:13. Transactional workflows still roll back test business rows.
- Existing Attendance/Schedule API security tests rerun; security implementation unchanged. No frontend testing framework added.

| Verification | Exact result |
| --- | --- |
| `AttendanceServiceTests` | 21 passed |
| `ScheduleServiceTests` | 21 passed |
| `AttendanceReconciliationServiceTests` | 7 passed |
| `AttendanceClockConfigTests` | 3 passed |
| `AttendanceApiSecurityTests` | 6 passed |
| `ScheduleApiSecurityTests` | 5 passed |
| Targeted total | **63 passed; 0 failures, errors, skips; exit 0** |
| Full backend suite with existing local MySQL | **81 passed across 14 classes; 0 failures, errors, skips; exit 0**, including all 3 transactional MySQL workflows |
| `./mvnw -q -DskipTests package` | **Passed; exit 0** |
| Final `npm run build` | **Passed; exit 0**; Vite 7.3.6, 1,624 modules; existing >500 kB chunk warning remains |
| `git diff --check` | **Passed; exit 0** |
| Schema check | `git diff --exit-code HEAD -- database/01_schema.sql`: **empty; exit 0** |
| Diff inspection | Reviewed complete production/test/new-file changes and handoff updates; approved first-slice changes retained |

Backend commands ran from `backend/` with the existing `.env` and Mockito agent:

```bash
source .env
./mvnw -q -DargLine="-javaagent:$HOME/.m2/repository/org/mockito/mockito-core/5.23.0/mockito-core-5.23.0.jar" -Dtest=AttendanceServiceTests,ScheduleServiceTests,AttendanceReconciliationServiceTests,AttendanceClockConfigTests,AttendanceApiSecurityTests,ScheduleApiSecurityTests test
./mvnw -q -DargLine="-javaagent:$HOME/.m2/repository/org/mockito/mockito-core/5.23.0/mockito-core-5.23.0.jar" test
./mvnw -q -DskipTests package
```

Logs: `/private/tmp/ems-policy-targeted-tests.log`, `/private/tmp/ems-policy-full-tests.log`, `/private/tmp/ems-policy-package.log`. Counts were checked against Surefire XML. No schema recreation was performed.

**Browser verification:** A temporary local Vite harness mounted the actual SchedulePage/AttendancePage/editor/today components with mocked API responses. The fixture server date was 7 October while the browser date was 6 October. New period/shift default and minimum were 7 October; submitting 6 October triggered native validation, and valid 7 October creation reached the fixture API. Historical 5 October shifts remained visible/editable with their original date; moving to 6 October was rejected. A wholly historical range disabled creation. Employee check-in availability followed the backend fixture date/state, and late actual checkout 17:20 displayed backend standard hours 7.92. Attendance exception default was 7 October, with historical dates still available. Advancing the fixture server date to 8 October before opening a cached editor refreshed its default/minimum to 8 October; an expired one-day period could not open a new editor. This verifies UI rendering/validation with fixtures, not live production time travel or authenticated end-to-end workflows; deterministic backend tests prove the date/hour calculations. Harness files were removed and Vite stopped. Screenshots outside the repo: `business-date-default-verification.jpg`, `backdating-validation-verification.jpg`, `standard-hours-display-verification.jpg` in this chat's visualization directory.

### Final working tree and remaining edges

`git status --short --untracked-files=all`:

```text
 M backend/src/main/java/com/evoq/ems/attendance/config/AttendanceClockConfig.java
 M backend/src/main/java/com/evoq/ems/attendance/service/AttendanceService.java
 M backend/src/main/java/com/evoq/ems/attendance/service/ScheduleService.java
 M backend/src/main/resources/application.properties
 M backend/src/test/java/com/evoq/ems/attendance/AttendanceApiSecurityTests.java
 M backend/src/test/java/com/evoq/ems/attendance/AttendanceMySqlWorkflowTests.java
 M backend/src/test/java/com/evoq/ems/attendance/AttendanceReconciliationServiceTests.java
 M backend/src/test/java/com/evoq/ems/attendance/AttendanceServiceTests.java
 M backend/src/test/java/com/evoq/ems/attendance/ScheduleServiceTests.java
 M frontend/src/modules/attendance/components/AttendanceEditor.jsx
 M frontend/src/modules/attendance/components/ScheduleCalendar.jsx
 M frontend/src/modules/attendance/components/ScheduleEditor.jsx
 M frontend/src/modules/attendance/components/TodayAttendanceActions.jsx
 M frontend/src/modules/attendance/pages/AttendancePage.jsx
 M frontend/src/modules/attendance/pages/SchedulePage.jsx
 M frontend/src/styles/global.css
 M handoff.md
?? backend/src/test/java/com/evoq/ems/attendance/config/AttendanceClockConfigTests.java
```

- No unresolved conflict blocks the three approved policies. D-03 leave/worked-attendance/report precedence remains outside scope; lateness, historical supervisor access, audit history, overnight shifts, and shared security remain unchanged.
- Unscheduled Manager/Admin exceptional open records have no scheduled end; their existing actual-elapsed checkout calculation remains. Manager/Admin exception/correction calculations are also unchanged. A future decision to cap or otherwise change those paths requires an explicit rule.
- A malformed open row checked in at/after its shift end conflicts rather than producing zero/negative capped hours; Manager/Admin correction remains the remedy. Multiple published shifts also remain a conflict.
- Manual late checkout retains the actual time, while automatic checkout records scheduled end; equivalent standard hours are equal by design. Sequential duplicate/job behavior is tested; simultaneous load testing is still AS-08.
- A form already open across business midnight may become invalid on save; backend validation rejects the stale historical date without rewriting it. The user must choose a current valid date. Opening/reopening editors refreshes business date.
- Existing historical schedules are preserved; the agreed update rule permits retaining historical dates under existing validation. The backdating rule does not add a general ban on editing other historical schedule fields.

**STOP for review. Do not proceed to AS-05/AS-06/AS-07, shared session/security, Dashboard/Reports, commit, or push.**

## Local Mac setup

Installed on this Mac (`x86_64`, macOS 15.8):

| Dependency | Installed version / location |
| --- | --- |
| Java / javac | Eclipse Temurin JDK 21.0.12.1; `~/Library/Java/JavaVirtualMachines/jdk-21.0.12.1+1/Contents/Home` |
| Node.js / npm | Node 22.23.3, npm 10.9.9; `~/.local/share/ems/node` |
| Maven | 3.9.16, installed by the project wrapper; `mvn` linked under `~/.local/bin` |
| MySQL | Community Server 8.4.11; `~/.local/share/ems/mysql` |
| Frontend packages | Installed with `npm ci --no-audit --no-fund` from the existing lockfile |
| Backend packages | Downloaded by Maven; backend compilation/package succeeded |

`.zprofile` and `.zshrc` load `~/.local/share/ems/env.sh`. Open a new terminal or run `source ~/.zprofile`. These installation paths are machine-specific and should not be hardcoded into application code or a portable CI configuration.

MySQL binds to `127.0.0.1:3306`; its data directory is `~/.local/share/ems/mysql-data`. It remains running. Use `ems-mysql-start` after a Mac restart, `ems-mysql` for the configured local SQL client, and `ems-mysql-stop` to stop it. Private client credentials are stored with mode 600; do not copy credentials into this handoff. This setup uses explicit start/stop helpers, not a login service.

Full local instructions: `~/.local/share/ems/SETUP.md` (outside the repository). Start the backend from `backend/` with `source .env` then `./mvnw spring-boot:run`, and the frontend from `frontend/` with `npm run dev`. Defaults are API port 8080 and frontend port 5173. The temporary packaged backend used for verification was stopped; the frontend development server was not left running.

## Current system

The React/Vite frontend, Spring Boot backend, and MySQL database have connected workflows for Employee/Organization, Leave, Attendance/Scheduling, and Assets. Shared session authentication, CSRF protection, role-based account data, password change, layout/theme, and health endpoint are present. Dashboard and Reports still contain placeholders. The SQL scripts in `database/` remain the physical schema source of truth; `database/01_schema.sql` drops and recreates the database and must not be rerun against data to retain.

Employee/Organization supports directory search/filter, profiles and direct reports, Manager/Admin onboarding and official/status edits, self/admin contact edits, and department/team create APIs. There is still no dedicated department/team management page. Leave supports balances, requests, supervisor decisions, and a Manager/Admin overview. Asset supports registration, assignment, return, and active-assignment display, while edit/status/history page controls and stronger authorization remain work to do.

### Scheduling and attendance behavior

- A Supervisor can draft and publish shifts for active direct reports in their team. Manager/Admin can manage any team and schedule any active member of that team, including a supervisor. Manager/Admin and Supervisor have **Team schedules** and **My shifts** views; employees see their own published shifts. A Manager/Admin account itself need not belong to a team to manage team schedules.
- A draft does **not** reserve employee time. Publishing checks approved leave and conflicts with published shifts. The current rule is one published shift per employee per day, with times ending on the same date. A draft can be discarded. Saving changes to a published schedule takes effect immediately.
- Updating a schedule preserves omitted entries. Removal uses explicit `removedEntryIds`, with ownership and attendance checks. This prevents a filtered supervisor view or date range from deleting hidden shifts. Supervisors cannot change another supervisor's entries; Manager/Admin can handle orphaned drafts and team-wide schedules.
- Employees check in only for their own published shift, from scheduled start through the next 30 minutes, stopping earlier if the shift ends. Self check-in records `PRESENT`. Manager/Admin can later mark `LATE` manually. Approved leave blocks self check-in even if a published entry remains.
- The employee can check out on the same day. The attendance service no longer rejects checkout solely because an employee was deactivated after check-in; account/session access still applies. Time inputs preserve seconds, and attendance correction forms preserve the existing note until edited. Manager/Admin employee options and historical exceptions include inactive employees.
- Scheduled self-attendance standard hours use actual check-in through the earlier of actual checkout and scheduled end, with seconds and HALF_UP two-decimal rounding. Actual manual checkout timestamps are retained. No break deduction or overtime/payroll calculation is implemented.
- New schedule entries cannot precede the business date. Existing historical entry dates can be retained but cannot move to another past date; draft publication rejects all past dates. Historical published schedules remain readable. Scheduling/attendance use `ems.business-timezone` (default Asia/Colombo), and frontend date defaults reuse the server today response.
- A scheduled job runs every minute over published shifts from the last 366 days. At or after shift end, it closes an open check-in at the **scheduled end time**, or marks a missed check-in `ABSENT`. It is idempotent. Approved leave does not become an automatic absence. The UI refreshes today's check-in state every 30 seconds and when the tab becomes visible, and shows an explicit absence state.
- Approving leave removes future published entries within the approved dates in the same transaction. Approval fails if an entry selected for removal already has attendance. A shift that has already started is not removed; its approved leave still blocks a new check-in and prevents automatic absence. Schedule publication also rejects approved-leave conflicts.
- Schedule updates/publication use a schedule row lock and employee row locks; leave decision and balance reads use pessimistic locks. These reduce competing-request races but have not been load-tested. The schema has no new uniqueness or correction-audit table.

### API changes

| Endpoint | Current access / behavior |
| --- | --- |
| `GET /api/schedules/me?from&to` | Any authenticated active employee; own published entries. |
| `GET /api/schedules/teams`, `/teams/{teamId}/employees`, `/teams/{teamId}?from&to&includeDrafts` | Supervisor for assigned team/direct reports; Manager/Admin for all teams/active members. |
| `GET /api/schedules/{id}` | Supervisor-scoped or Manager/Admin full schedule. |
| `POST /api/schedules`, `PUT /api/schedules/{id}`, `POST /api/schedules/{id}/publish` | Supervisor-scoped or Manager/Admin scheduling. Update accepts explicit `removedEntryIds`. |
| `DELETE /api/schedules/{id}` | Discard a draft; published schedules cannot be discarded. |
| `GET /api/attendance-records/today`, `POST /check-in`, `POST /check-out` | Server-controlled self attendance. |
| `GET /api/attendance-records/me`, `/teams`, `/teams/{teamId}` | Own or supervisor-scoped history. |
| `GET /api/attendance-records`, `/employees`, `POST /exceptions`, `PUT /{id}` | Manager/Admin all records, employee options, exceptional records, and correction. |
| Leave approval/rejection endpoints | Existing supervisor decision endpoints; approval now coordinates future published shifts. |

Existing Employee, Organization, Leave, and Asset endpoint details remain in their controllers and README. `ResponseStatusException` now has a dedicated shared API handler so intended Leave 4xx responses are returned instead of becoming generic 500s.

## Earlier audit verification on 6 October 2026 (before cleanup)

With the newly configured local `dev` MySQL database available:

- **Backend:** compilation and packaging passed. A fresh full run produced **58 tests passed, zero failures/errors/skips**, including 45 Attendance/Scheduling tests. Coverage includes services, API role/CSRF checks, application contexts, and three transactional MySQL workflow tests. Test business rows roll back.
- **Coverage limit:** the MySQL leave-removal test invokes `ApprovedLeaveScheduleCoordinator` directly; it does not cover the complete `LeaveService.approve` transaction, request status, balance deduction, and failure rollback. Existing tests are not a browser walkthrough or simultaneous-request/load test. There are no configured frontend automated test scripts.
- **Frontend:** `npm run build` passed. Vite still warns that the generated JS chunk exceeds 500 kB.
- **Live backend:** the packaged application connected to MySQL, started on port 8080, and `GET /api/health` returned HTTP 200 with `status: UP`. This was a health check, not a fresh end-to-end login/attendance browser review.
- **Repository:** `git diff --check` passed. No application source or schema was changed. The previous 3 October handoff recorded XML parsing of the four edited use-case diagrams; this review read their content, but did not perform a visual layout audit.

Historical evidence: on 2 October, real Manager/Admin and Supervisor sessions successfully read the scheduling APIs. That check was recorded in the previous handoff; it was not repeated on 6 October.

To rerun the backend suite on this machine, from `backend/`:

```bash
source "$HOME/.local/share/ems/env.sh"
source .env
./mvnw -q -DargLine="-javaagent:$HOME/.m2/repository/org/mockito/mockito-core/5.23.0/mockito-core-5.23.0.jar" test
```

The Mockito version/path above matches this machine's current Maven dependencies; a portable setup should resolve it from Maven rather than copying an absolute username path. The frontend check is `cd frontend && npm run build`. The dev profile restores reserved demo accounts and a scheduling team; it does not seed schedule/attendance/leave-request/asset business rows. Attendance/Scheduling now use the configured business Clock, default Asia/Colombo; see the second-slice verification above.

### Local database snapshot

Read-only counts on 6 October 2026 after installation/testing; these describe the new local instance, not data guaranteed on another developer's machine:

| Table | Rows | Table | Rows |
| --- | ---: | --- | ---: |
| department | 1 | team_project | 1 |
| employee | 3 | user_account | 3 |
| leave_type | 0 | leave_balance | 0 |
| leave_request | 0 | schedule | 0 |
| schedule_entry | 0 | attendance_record | 0 |
| asset | 0 | asset_assignment | 0 |

The `role` table contains the three required roles. Leave types/balances are currently initialized by the Leave service when its overview/types endpoints are used; the installation did not invoke those endpoints.

## Planner backlog: Attendance & Scheduling

The first slice is approved and uncommitted. AS-03, D-05, and SH-03/D-07 are now implemented locally and await review. Other scope below remains **open**. Each newly authorized implementation should include its relevant regression checks.

| ID | Priority | Task and current evidence | Acceptance checks / dependency |
| --- | --- | --- | --- |
| AS-01 | High | **Approved, uncommitted:** dynamic calendar columns follow the rendered dates. | Browser fixtures verified 1, 3, 5, and 7 dates, header/shift/coverage alignment, and narrow-screen horizontal scrolling. See first-slice verification above. |
| AS-02 | High | **Today-state fix approved, uncommitted; reporting remains open.** Approved leave is checked before the no-shift fallback; `ON_LEAVE` is displayed without creating an attendance row. | Required service/API regressions pass; approved-leave UI fixture checked. Resolve D-03 before defining precedence against existing attendance, reports, or row creation. |
| AS-03 | High | **Implemented, awaiting review:** scheduled self-attendance hours cap at scheduled end; manual actual timestamps retained. | D-02 V1 approved. Fixed-Clock before/end/after, automatic, duplicate, and manual/job sequence tests pass. Simultaneous load testing remains AS-08. |
| AS-04 | High | **Initial defaults approved; business-date/backdating extension awaiting review.** New shifts default to server business today or future period start; wholly historical periods cannot create new shifts. | D-05 V1 approved and enforced in the backend. Original historical dates retained; new/changed past dates rejected. Browser server-date boundary and date-limit fixtures pass. |
| AS-05 | Medium | **Define historical supervisor access.** Team attendance queries include only current active direct reports; deactivated/transferred employees disappear from that view. | Resolve D-06 with the Employee module. Preserve role scope and prevent access to unrelated employees. The schema does not store historical team/supervisor membership; do not claim historical ownership can be reconstructed from current fields. |
| AS-06 | Medium | **Add correction accountability.** Manager/Admin overwrites attendance; notes are optional; no immutable actor/time/before/after trail exists. | Resolve D-04. Validate any required correction reason. A permanent audit store needs coordinated schema approval; a mutable note alone does not satisfy immutable audit history. |
| AS-07 | Medium | **Add attendance summaries and CSV export.** The attendance page has filtered totals, but no complete report/export workflow. | Employee/team summaries for present, late, absent, leave, and hours; filters/export agree; permissions enforced in the backend. Resolve D-01/D-02/D-03 as applicable. Coordinate the common Reports page with SH-06; absence must follow scheduled work, not every missing calendar-day record. |
| AS-08 | Medium | **Strengthen workflow/concurrency coverage.** Existing service/API tests and three MySQL workflows are sequential; there are no frontend automated test scripts. | Browser workflows for all three roles; simultaneous check-in/publication and checkout/reconciliation; full leave approval including balance/status/shift deletion and failure rollback; date/time boundaries and regressions for AS-01–AS-07. Plan isolated fixtures and avoid destructive schema initialization in tests. |
| AS-09 | Medium | **Align OOAD documents and module guidance.** The use-case scenario/activity flow describes save followed by publish; implementation has a separate draft/publish lifecycle and additional attendance automation. | Scenario/diagrams reflect drafts, explicit publication, check-in/out, absence/auto-checkout, leave integration, and final actor permissions. Distinguish conceptual domain methods from service-layer implementation; visually review diagrams. Update stale README statements. |

Primary implementation files:

- `frontend/src/modules/attendance/pages/SchedulePage.jsx`
- `frontend/src/modules/attendance/components/ScheduleCalendar.jsx`, `ScheduleEditor.jsx`, `TodayAttendanceActions.jsx`
- `frontend/src/styles/global.css`
- `backend/src/main/java/com/evoq/ems/attendance/service/AttendanceService.java`, `ScheduleService.java`, `AttendanceReconciliationService.java`, `ApprovedLeaveScheduleCoordinator.java`
- `backend/src/main/java/com/evoq/ems/attendance/integration/EmployeeTeamReader.java`, `ApprovedLeaveReader.java`
- `backend/src/test/java/com/evoq/ems/attendance/`
- `Diagrams/module_3_attendance/` and the Full System diagrams

## Planner backlog: shared functionality

Shared changes must be coordinated across module owners; this is not authorization to take over another member's module. SH-03's Attendance/Scheduling business-clock scope is implemented and awaiting review; other items below remain **open**.

| ID | Priority | Task and current evidence | Acceptance checks / dependency |
| --- | --- | --- | --- |
| SH-01 | High | **Invalidate/revalidate existing sessions after account changes.** `AccountPrincipal` caches role/active state at login. Employee updates change database accounts without explicitly revoking existing sessions. | A logged-in user loses revoked privileges after deactivation/demotion; subsequent requests follow the agreed policy. Cover existing sessions, not only fresh logins. Define how deactivation interacts with closing an existing check-in. |
| SH-02 | High | **Enforce a consistent role/access matrix.** Asset controllers have no role restrictions beyond shared authentication; employee directory/profile read scope also needs a decision. | Agree endpoint permissions for self/direct reports/all employees. Backend rejects unauthorized reads/writes even when called directly. Coordinate asset fixes with its owner and cover register/update/status/assign/return/history. |
| SH-03 | High | **Attendance/Scheduling scope implemented, awaiting review:** configured business Clock defaults to Asia/Colombo; frontend reuses the today API date. | Default/override/invalid-zone and business-midnight tests pass; browser fixtures verify server/browser disagreement and fresh editor dates. No JVM-global change. Other modules' timezone policy is outside this task. |
| SH-04 | High | **Provide legitimate first-admin setup.** SQL seeds roles; the dev profile creates public demonstration accounts. | A fresh non-dev installation can securely provision its first real admin through a documented, controlled process. Dev credentials are not the production bootstrap. |
| SH-05 | Medium | **Connect role-aware dashboard metrics.** Dashboard currently renders dashes and an empty state. | Real active-employee, pending-leave, present-today, and assigned-asset metrics; agreed role scope; clear loading/error/empty states. Define each metric consistently with module rules. |
| SH-06 | Medium | **Implement the common Reports page.** It currently has no connected sources or exports. | Reports use module-owned summaries, enforce backend access, and provide consistent filters/exports. Reuse AS-07 rather than building competing attendance calculations. |
| SH-07 | Medium | **Standardize validation, errors, and session expiry.** Shared APIs provide `fieldErrors`, but module forms often display only the generic message; auth context is not a central response to every later 401. | Show actionable field errors, clear retry behavior, and a consistent return to login on expired sessions. Distinguish permission failure from session expiry; retain existing CSRF protection. |
| SH-08 | Medium | **Automate checks and prepare deployment.** Local builds/tests work; portable CI/deployment and service configuration remain unfinished. | CI builds frontend and tests backend against an isolated MySQL fixture, resolves tool/agent paths portably, and keeps secrets outside source. Document runtime versions, timezone, origins/cookies, database setup/migrations, and process startup. Avoid destructive schema scripts against retained data. |

Useful shared entry points: `auth/AccountPrincipal.java`, `config/SecurityConfig.java`, `employee/service/EmployeeService.java`, `attendance/config/AttendanceClockConfig.java`, `application.properties`, `frontend/src/services/api.js`, `frontend/src/context/AuthContext.jsx`, and the Dashboard/Reports pages.

## Decisions required before dependent implementation

| ID | Decision | Current behavior / planning constraint |
| --- | --- | --- |
| D-01 | **Late attendance:** does any part of the 30-minute check-in window count as late? | All self check-ins currently record `PRESENT`; Manager/Admin can manually mark `LATE`. Preserve this until an explicit rule is chosen. |
| D-02 | **V1 agreed and implemented, awaiting review:** scheduled-end cap. | Scheduled self-attendance uses actual check-in to min(actual checkout, scheduled end); seconds, HALF_UP two decimals. No breaks/overtime/payroll. Exception/correction calculations remain unchanged. |
| D-03 | **Leave precedence:** approval after start/worked attendance, and how leave appears in attendance reports? | Only future starts are removed. Started shifts can remain; leave blocks check-in/automatic absence. Attendance and approved-leave data may need a combined report instead of inserting duplicate/conflicting rows. |
| D-04 | **Correction reason/audit:** mandatory reason, immutable history, retention, and who can read it? | Notes are optional; the current schema has no audit table. Coordinate schema changes before implementing permanent history. |
| D-05 | **V1 agreed and implemented, awaiting review:** no new historical schedule entries; historical attendance corrections remain Manager/Admin exceptions. | All draft past dates block publication. Existing entry dates may retain their original past value; changing to a different past date is blocked. Published history stays readable. No silent deletion or rewrite. |
| D-06 | **Historical scope:** should supervisors see former/inactive reports, and by which ownership rule? | Current membership determines access. Historical assignments are not persisted; a richer historical rule may require new data/schema. |
| D-07 | **V1 agreed and implemented, awaiting review:** Asia/Colombo by default, configurable. | `ems.business-timezone` / `EMS_BUSINESS_TIMEZONE`; Attendance/Scheduling use that Clock, frontend reuses server today. No JVM-global or developer-zone dependency. |
| D-08 | **Shift scope:** are overnight or multiple daily shifts required for this submission? | V1 intentionally supports one same-day published shift. DATE plus TIME cannot represent checkout after midnight correctly; expanding scope requires model/rule changes. |

## Suggested implementation sequence

1. Review the uncommitted second slice (AS-03, D-05, SH-03/D-07), retaining the approved first slice. Stop until a new instruction; do not commit or push.
2. Only with new authorization, plan remaining module/shared work in coordination with its owners. Shared SH-01/SH-02 security/session work is not authorized by this cleanup.
3. Resolve historical access, leave precedence, late rules, and audit requirements (D-01/D-03/D-04/D-06). Implement the agreed AS-05/AS-06 work and coordinate any schema change.
4. Implement AS-07 with SH-05/SH-06. Attach regression checks to each change, then finish broader AS-08 and shared security/session tests.
5. Complete AS-09, SH-04/SH-07/SH-08, and a full role-based demo/review. First-admin/security work must be complete before a non-dev deployment, even if scheduled later than local UI fixes.

Completion should be checked against acceptance criteria, not a guessed percentage. A passing existing suite does not make the open backlog complete.

## Remaining system limits and other owners' work

- Attendance employee/date uniqueness and published-shift rules are enforced in services, without new database constraints; direct SQL can bypass them. Existing row locks reduce races but are not proof of concurrency correctness. Any stronger constraints need group coordination.
- Reconciliation only backfills the past 366 days. Cancellation of all future entries can leave an empty published schedule; decide whether that lifecycle needs clearer UI before adding new states.
- Leave uses inclusive calendar days and hardcoded opening balances. Weekend/holiday/half-day rules, annual resets, leave-type administration, and initialization behavior need the Leave owner's review. The UI currently exposes submission only to the `EMPLOYEE` role even though supervisor/admin actors also inherit employee capabilities in the UML; agree the intended policy.
- Asset input validation, atomic assignment/return, duplicate/concurrent assignment protection, and edit/status/history UI remain Asset-owner tasks alongside SH-02. The service currently saves asset status and assignment separately without a service transaction.
- Employee team/department management UI, supervisor eligibility, directory/profile scope, and team-transfer effects need the Employee owner's review and coordination with AS-05/SH-02.
- The shared Modal lacks a complete keyboard/focus/dialog accessibility treatment; include this in shared UI polishing if it is in submission scope. Shift templates, copying a previous week, clearer conflict details, pagination, and batching name lookups are useful optional improvements after the required backlog. They are not prerequisites for preserving the current V1 scope.
- README still understates Leave/Asset frontend integration. Diagrams and role inheritance must be checked against final behavior, not just XML validity.

## Copyable planner brief

> Review this handoff against the current `Nadhi` checkout. The first slice (AS-01, immediate AS-02/AS-04) is approved and preserved. The second slice (AS-03, D-05, SH-03/D-07) is implemented and awaits review; both remain uncommitted. The V1 hours, backdating, and business-zone decisions are recorded above. Latest full MySQL suite: 81 passed. Stop for review; do not proceed to AS-05/AS-06/AS-07, shared security/session work, Dashboard/Reports, commit, or push. Future planning needs new authorization and must respect module ownership, open decisions D-01/D-03/D-04/D-06/D-08, the frozen schema, existing permissions/CSRF/attendance protections, and the fixture-based browser verification limits.
