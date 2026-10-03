# EVOQ EMS handoff

**Updated:** 3 October 2026 (Asia/Colombo)

**Branch:** `Nadhi`

**Previous handoff:** 30 September 2026, reviewing `7fb7c58`

**Implementation commit since that handoff:** `00aacfc` (based on `5e2cfe2`)

**Schema:** The existing 13-table MySQL schema is unchanged.

This is the current handoff for the repository. The previous handoff was an ignored local file; this revision is tracked in Git. Its older 29–30 September test counts, database snapshot, and statements that leave approval did not affect schedules are superseded by the details below.

## What changed since the previous handoff

- Commit `5e2cfe2` adjusted the Employee creation button and Leave page layout after the previous handoff's reviewed HEAD.
- Commit `00aacfc` contains the scheduling, attendance, and leave-integration fixes, updated README, four use-case diagram edits, and regression tests. The diagram edits add actor associations in the Employee/Organization, Leave, Attendance, and Asset use cases; their XML formatting was also normalized. These diagram edits existed in the working tree before the scheduling fixes and were included because this handoff covers all work since the previous handoff.
- This handoff and the `.gitignore` change that makes it trackable follow the implementation commit. Use `git log -2 --oneline` to see both commit IDs after pulling `Nadhi`.

## Current system

The React/Vite frontend, Spring Boot backend, and MySQL database have connected workflows for Employee/Organization, Leave, Attendance/Scheduling, and Assets. Shared session authentication, CSRF protection, role-based account data, password change, layout/theme, and health endpoint are present. Dashboard and Reports still contain placeholders. The SQL scripts in `database/` remain the physical schema source of truth; `database/01_schema.sql` drops and recreates the database and must not be rerun against data to retain.

Employee/Organization supports directory search/filter, profiles and direct reports, Manager/Admin onboarding and official/status edits, self/admin contact edits, and department/team create APIs. There is still no dedicated department/team management page. Leave supports balances, requests, supervisor decisions, and a Manager/Admin overview. Asset supports registration, assignment, return, and active-assignment display, while edit/status/history page controls and stronger authorization remain work to do.

### Scheduling and attendance behavior

- A Supervisor can draft and publish shifts for active direct reports in their team. Manager/Admin can manage any team and schedule any active member of that team, including a supervisor. Manager/Admin and Supervisor have **Team schedules** and **My shifts** views; employees see their own published shifts. A Manager/Admin account itself need not belong to a team to manage team schedules.
- A draft does **not** reserve employee time. Publishing checks approved leave and conflicts with published shifts. The current rule is one published shift per employee per day, with times ending on the same date. A draft can be discarded. Saving changes to a published schedule takes effect immediately.
- Updating a schedule preserves omitted entries. Removal uses explicit `removedEntryIds`, with ownership and attendance checks. This prevents a filtered supervisor view or date range from deleting hidden shifts. Supervisors cannot change another supervisor's entries; Manager/Admin can handle orphaned drafts and team-wide schedules.
- Employees check in only for their own published shift, from scheduled start through the next 30 minutes, stopping earlier if the shift ends. Self check-in records `PRESENT`. Manager/Admin can later mark `LATE` manually. Approved leave blocks self check-in even if a published entry remains.
- The employee can check out on the same day. The attendance service no longer rejects checkout solely because an employee was deactivated after check-in; account/session access still applies. Time inputs preserve seconds, and attendance correction forms preserve the existing note until edited. Manager/Admin employee options and historical exceptions include inactive employees.
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

## Verification

On 3 October 2026, with the local `dev` MySQL database available:

- **Backend:** 58 tests passed, zero failures/errors/skips. This includes service, security, application-context, and transactional MySQL workflow coverage for scheduling, absence reconciliation, and leave shift removal. The MySQL workflow test rows roll back.
- **Frontend:** `npm run build` passed. Vite still warns that the generated JS chunk exceeds 500 kB.
- **Repository:** `git diff --check` passed; the four changed `.drawio` files parse as XML. Visual layout of those diagrams was not reviewed in this handoff.
- **Live API check on 2 October:** after starting the current backend, real `demo.manager` and `demo.supervisor` sessions both received HTTP 200 for own shifts, managed teams, and team schedules. Manager/Admin also received HTTP 200 for the team's schedulable employees. This resolved the older running backend's “Access denied” screenshot; no data was created by the live API check.

To rerun the backend suite on this machine, from `backend/`:

```bash
set -a
source .env
set +a
./mvnw -q -DargLine=-javaagent:/Users/nadhi/.m2/repository/org/mockito/mockito-core/5.23.0/mockito-core-5.23.0.jar test
```

The Mockito agent path is local to this machine. The frontend check is `cd frontend && npm run build`. Start the app with `./mvnw spring-boot:run` from `backend/` after loading `.env`, and `npm run dev` from `frontend/`. Defaults are API port 8080 and frontend port 5173. The dev profile restores reserved demo accounts and a scheduling team; it does not seed schedule/attendance/leave-request/asset business rows. Configure the server JVM's business time zone for attendance; `Clock.systemDefaultZone()` supplies server dates and times.

### Local database snapshot

Read-only counts on 3 October 2026; these describe this machine, not guaranteed seed data:

| Table | Rows | Table | Rows |
| --- | ---: | --- | ---: |
| department | 1 | team_project | 1 |
| employee | 4 | user_account | 4 |
| leave_type | 3 | leave_balance | 9 |
| leave_request | 0 | schedule | 2 |
| schedule_entry | 2 | attendance_record | 2 |
| asset | 0 | asset_assignment | 0 |

## Decisions still needed and known limits

1. **Working hours:** Current hours are elapsed check-in to check-out, rounded to two decimals. The team has not yet decided whether breaks are deducted or overtime is calculated separately. Do not infer payroll rules from the current field.
2. **Correction audit:** Manager/Admin can overwrite an attendance record, but the frozen schema has no immutable who/when/before/after audit trail. Adding one requires an agreed schema change. This decision remains open.
3. **Leave on an already-started or worked shift:** Only future shift starts are removed automatically. Approved leave prevents an automatic absence when a started shift remains, but approval against already-worked attendance and the desired retroactive correction workflow still need a policy.
4. **Schema and concurrency:** Employee/date attendance uniqueness and published-shift rules are enforced in services, not new database constraints. Direct SQL can bypass them; parallel-request behavior has not had a dedicated load test. DATE plus TIME cannot represent overnight shifts or checkout after midnight correctly. The automatic reconciliation job only backfills the last 366 days.
5. **Other modules:** Leave uses inclusive calendar days and hardcoded opening balances, without weekend/holiday/half-day rules or annual reset administration. Asset authorization, input validation, transactional assignment/return, and history/edit/status UI need review. Employee directory read scope, first legitimate admin bootstrap, Dashboard metrics, Reports/exports, and deployment setup remain open.
6. **Docs and diagrams:** Parts of README outside its new scheduling section may still describe the earlier Leave/Asset UI state. The four edited use-case diagrams should receive a visual review and be checked against final role decisions.
