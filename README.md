# EVOQ Employee Management System

Employee Management System developed for the EVOQ client project.

The system uses a React frontend, Spring Boot REST backend, and MySQL relational database.

## Start both development servers

Start MySQL first. The `evoq_ems` database must already exist (see [Database Setup](#database-setup)).
The frontend and backend are separate processes: opening the frontend does not start the API.
For a new checkout, run `cp backend/.env.example backend/.env` from the repository
root and replace the MySQL password in `backend/.env` with your own.

In **terminal 1**, from the repository root (macOS/Linux):

```bash
cd backend
source .env
./mvnw spring-boot:run
```

`backend/.env` is a local, Git-ignored file. Copy `backend/.env.example` and
set your own `DB_USERNAME` and `DB_PASSWORD`. Spring Boot does not read this
file automatically; `source .env` loads it into the terminal before Maven
starts. Keep this terminal open. Wait for `Started EvoqEmsBackendApplication`,
then check [http://localhost:8080/api/health](http://localhost:8080/api/health).

In **terminal 2**, from the repository root:

```bash
cd frontend
npm install
npm run dev
```

Open [http://localhost:5173](http://localhost:5173). Leave this terminal open too.
`npm install` is only needed the first time or after dependencies change. If
the frontend opens but sign-in fails, check that the backend terminal is still
running and the health URL responds. If `SERVER_PORT` differs from `8080`,
set `VITE_API_BASE_URL` to the matching `http://localhost:<port>/api` in
`frontend/.env`.

## Project Structure

```text
EMS/
├── frontend/                  React + Vite frontend
│   ├── src/
│   │   ├── app/              Routing
│   │   ├── components/       Shared UI components
│   │   ├── context/          Shared React context
│   │   ├── modules/
│   │   │   ├── employees/    Employee & Organization Management
│   │   │   ├── leave/        Leave Management
│   │   │   ├── attendance/   Attendance & Scheduling Management
│   │   │   └── assets/       Asset & Equipment Management
│   │   ├── services/         Shared API/auth services
│   │   └── styles/           Shared styles
│   ├── package.json
│   └── vite.config.js
│
├── backend/                   Spring Boot REST API
│   ├── src/main/java/com/evoq/ems/
│   │   ├── config/           Shared configuration
│   │   ├── auth/             Shared authentication/authorization
│   │   ├── common/           Shared DTOs/exceptions
│   │   ├── employee/         Employee & Organization backend
│   │   ├── leave/            Leave backend
│   │   ├── attendance/       Attendance & Scheduling backend
│   │   └── asset/            Asset backend
│   ├── src/main/resources/
│   ├── pom.xml
│   ├── mvnw
│   └── mvnw.cmd
│
├── database/
│   ├── 01_schema.sql
│   ├── 02_sample_data.sql
│   ├── 03_queries.sql
│   ├── 04_validation.sql
│   ├── 05_negative_constraint_tests.sql
│   ├── 06_runtime_roles.sql
│   └── 07_first_run_setup.sql
│
└── Diagrams/
    ├── Full_System/
    ├── module_1_employee_organization/
    ├── module_2_leave_management/
    ├── module_3_attendance/
    └── module_4_asset/
```

## Technology Stack

Frontend:

- React
- Vite
- React Router
- Tailwind CSS
- npm

Backend:

- Java
- Spring Boot
- Spring Web
- Spring Data JPA / Hibernate
- Spring Security
- Bean Validation
- Maven

Database:

- MySQL
- Database name: `evoq_ems`

## Development Requirements

Install the following before running the project:

### Java

Java 21 or newer is recommended.

Check:

```bash
java -version
```

The project includes the Maven Wrapper, so a separate Maven installation is not required.

### Node.js

Use a Node.js version compatible with the project's Vite version.

Check:

```bash
node -v
npm -v
```

### MySQL

Install MySQL 8.x and make sure the MySQL server is running.

Check:

```bash
mysql --version
```

## First-Time Setup

Clone the repository:

```bash
git clone <repository-url>
cd EMS
```

Do not commit local passwords, `.env` files, `node_modules`, Maven build output, or IDE configuration.

---

# Database Setup

The database scripts are stored in:

```text
database/
```

The schema script creates the `evoq_ems` database and all required tables, relationships, constraints, and indexes.

> WARNING: `01_schema.sql` contains `DROP DATABASE IF EXISTS evoq_ems`. Running it again will delete and recreate the local EVOQ database.

For initial setup:

```bash
mysql -u root -p < database/01_schema.sql
mysql -u root -p < database/02_sample_data.sql
mysql -u root -p evoq_ems < database/07_first_run_setup.sql
```

Enter your own local MySQL password when prompted.
`02_sample_data.sql` now inserts only the three required roles:
`EMPLOYEE`, `SUPERVISOR`, and `MANAGER_ADMIN`. It creates no employees,
accounts, departments, teams, leave, attendance, schedules, or assets. A
freshly initialized database has no login account. The separate additive
`07_first_run_setup.sql` migration adds a persistent setup marker without changing
the finalized schema file. Apply it to existing databases too; it closes setup
when any account exists and never resets an already completed installation.

To verify the database:

```bash
mysql -u root -p < database/04_validation.sql
```

The validation queries should report no integrity problems.

`03_queries.sql` contains project/demo queries.

`05_negative_constraint_tests.sql` uses sample IDs and must not be run against
an empty database. It intentionally attempts invalid operations to demonstrate
database constraints after suitable test records exist.

## Database Tables

The database contains 13 main tables, plus the runtime `first_run_setup` marker
added by the separate first-run migration:

```text
role
user_account
department
team_project
employee
leave_type
leave_balance
leave_request
schedule
schedule_entry
attendance_record
asset
asset_assignment
```

The SQL files in `database/` are the source of truth for the physical relational schema.

---

## Scheduling and Attendance Rules

- Supervisors manage schedules for their active direct reports in their permitted team. Manager/Admin can manage any team, including a supervisor's own shift. Both roles can also view their own published shifts. Saving a new schedule creates a draft; employees see it only after explicit publication. Drafts do not reserve employee time and may be discarded. Saving changes to an already published schedule makes those changes visible immediately. Entry removal must be explicit; omission from an update preserves the entry.
- Scheduling validates the selected period, same-day start/end times, overlaps and approved leave. Publication enforces one published shift per employee per business date. V1 supports neither overnight nor multiple daily published shifts. New entries cannot use past business dates, and publishing a draft rejects all past work dates. Existing historical entries may retain their date, but cannot move to a different past date; published history remains readable.
- Employees view their published shift and the backend's authoritative today state. Approved leave is detected even if its shift was removed. Self check-in uses the authenticated employee and server date/time, from scheduled start through the next 30 minutes, provided the shift has not ended. It records `PRESENT`; Manager/Admin can perform existing exceptions/corrections, including `LATE`. Supervisors can view permitted team attendance; Manager/Admin can view organization attendance.
- Manual checkout preserves the actual server-recorded timestamp. Standard working hours run from actual check-in to `min(actual checkout, scheduled shift end)`, preserving seconds and rounding HALF_UP to two decimals. There is no break deduction, overtime workflow or payroll calculation.
- A background job checks published shifts from the past 366 days every minute. At scheduled end it closes an open check-in at that end time with the same standard-hours calculation, or records `ABSENT` for a missed shift. Completed records remain unchanged; repeated reconciliation is idempotent. Approved leave is skipped and never creates an automatic `LEAVE` attendance record.
- Existing Leave approval removes future published shifts within the approved dates. Attendance-protected entries prevent removal and approval rolls back; Attendance/Scheduling does not approve or reject leave.
- Attendance & Scheduling uses the explicit business timezone configured by `ems.business-timezone` (environment override `EMS_BUSINESS_TIMEZONE`), default **Asia/Colombo**. Today state, check-in windows, backdating validation and reconciliation use the same server Clock. The frontend reuses the today response's business date for schedule defaults and date limits; browser/JVM default timezones do not define the attendance date.

---

# Backend Setup

Move into the backend:

```bash
cd backend
```

### macOS / Linux

If required, make the Maven wrapper executable:

```bash
chmod +x mvnw
```

Load the local configuration and run:

```bash
source .env
./mvnw spring-boot:run
```

### Windows

Run:

```powershell
mvnw.cmd spring-boot:run
```

The backend runs on:

```text
http://localhost:8080
```

Health check:

```text
http://localhost:8080/api/health
```

Expected response:

```json
{
  "application": "EVOQ EMS Backend",
  "status": "UP"
}
```

## Backend Database Configuration

Database credentials must not be committed to Git.

The backend reads local database configuration from environment variables.

`DB_USERNAME` and `DB_PASSWORD` set the database credentials. `DB_URL` can
override the default local MySQL URL, and `SERVER_PORT` can override the
default port `8080`.

The backend allows browser API requests from `http://localhost:5173` by
default. Set `FRONTEND_ORIGIN` to the frontend's exact origin when it runs
elsewhere.

Example macOS/Linux:

```bash
source .env
./mvnw spring-boot:run
```

For a new checkout, copy `backend/.env.example` to the ignored
`backend/.env` and replace the MySQL password with your own. The example
uses the non-seeding `local` profile. The demo password is used only if you
explicitly switch to `dev`:

```dotenv
export DB_USERNAME="root"
export DB_PASSWORD="your-local-mysql-password"
export SPRING_PROFILES_ACTIVE="local"
export DEV_DEMO_PASSWORD="EvoqDemo2026!"
```

Windows PowerShell:

```powershell
$env:DB_USERNAME="root"
$env:DB_PASSWORD="your-local-mysql-password"
.\mvnw.cmd spring-boot:run
```

Each developer uses their own local MySQL credentials.

Never commit a real database password.

The backend connects to:

```text
jdbc:mysql://localhost:3306/evoq_ems
```

Hibernate is configured to validate the existing database schema rather than replace it.

## First administrator and development sign-in

With the `local` profile and an empty installation, open
[http://localhost:5173](http://localhost:5173). The app shows **Create first
administrator** before Login. Enter your actual name, email, hire date, department,
job title, username and a password of at least 16 characters (at most 72 UTF-8
bytes). Setup creates the department, active Employee and enabled Manager/Admin
account together, then sends you to Login. No manual identity insert or password
file is needed.

The backend permanently records completion. Account deactivation, demotion,
deletion or restarts do not reopen setup. Both the setup API and screen close;
competing submissions cannot create two first administrators. A failed status
read shows unavailable with Retry rather than assuming the database is empty.
Existing installations with any accounts use normal Login, even if no active
Manager remains. Setup is not account recovery.

For optional demo logins, explicitly use `SPRING_PROFILES_ACTIVE=dev`:

The SQL initialization creates only the three required roles. When the backend
starts with `SPRING_PROFILES_ACTIVE=dev` and `DEV_DEMO_PASSWORD` set, it creates
one reserved development department and three minimal linked Employee and
UserAccount records. Restarting in `dev` reuses those rows, restores their
roles and active status, and reapplies the configured password if it changed.
No schedules, attendance, leave, assets, or other business records are created.
Outside the `dev` profile, startup does not create demo accounts.

With the example password `EvoqDemo2026!`, sign in at
[http://localhost:5173](http://localhost:5173) using:

| Role | Username | Password |
| --- | --- | --- |
| Manager/Admin | `demo.manager` | `EvoqDemo2026!` |
| Supervisor | `demo.supervisor` | `EvoqDemo2026!` |
| Employee | `demo.employee` | `EvoqDemo2026!` |

The example password is public and is only for local development. Use the same
`DEV_DEMO_PASSWORD` value across your team if you want identical credentials.
Do not enable the `dev` profile against a production database. These accounts
let you inspect the protected app. Employee, attendance, schedule, and asset
pages use their APIs. Dashboard and Reports connect all four modules.
First-run migration and runtime setup are documented in [deployment/README.md](deployment/README.md).

Login uses a server-side Spring Security session. The browser stores only the
HTTP-only `JSESSIONID` cookie; React sends it with `credentials: include`.
React first requests `GET /api/auth/csrf` and sends that token in a header on
login, logout, change-password, and other modifying requests. After login it
fetches a fresh token because Spring rotates the session's CSRF state.
`GET /api/auth/me` returns only user ID, employee ID, username, and role.

An authenticated user can change a temporary password from the account menu.
Forced first-login password change is deferred because the frozen schema has
no flag for it. Employee onboarding can optionally create a login account;
the dev initializer is only for reserved demo access.

---

# Frontend Setup

Open another terminal:

```bash
cd frontend
```

Install dependencies:

```bash
npm install
```

Start the development server:

```bash
npm run dev
```

The frontend runs on:

```text
http://localhost:5173
```

For a production build:

```bash
npm run build
```

The generated build is placed in:

```text
frontend/dist/
```

Do not commit `node_modules/` or `dist/`.

---

# Frontend API Configuration

The frontend communicates with the Spring Boot REST API.

The default development API is:

```text
http://localhost:8080/api
```

The frontend API service reads:

```text
VITE_API_BASE_URL
```

For normal local development the default is sufficient.

If another API URL is required, create:

```text
frontend/.env
```

with:

```env
VITE_API_BASE_URL=http://localhost:8080/api
```

Do not commit personal `.env` files.

---

# Running the Complete System

Three components are required.

### 1. MySQL

Make sure MySQL is running and `evoq_ems` has been created using the database scripts.

### 2. Backend

From `backend/`:

macOS/Linux:

```bash
source .env
./mvnw spring-boot:run
```

Windows:

```powershell
$env:DB_USERNAME="root"
$env:DB_PASSWORD="your-password"
.\mvnw.cmd spring-boot:run
```

Verify:

```text
http://localhost:8080/api/health
```

### 3. Frontend

From `frontend/`:

```bash
npm install
npm run dev
```

Open:

```text
http://localhost:5173
```

Development architecture:

```text
Browser
   |
   v
React / Vite
localhost:5173
   |
   | REST / JSON
   v
Spring Boot
localhost:8080
   |
   | JPA / JDBC
   v
MySQL
evoq_ems
```

---

# Module Ownership

To reduce merge conflicts, each member should primarily work inside their assigned module.

| Member | Module |
|---|---|
| Roashan J. | Employee & Organization Management |
| Samarappuli V. D. B | Leave Management |
| Ahmed Nadhi | Attendance & Scheduling Management |
| V. J. Shaarugshan | Asset & Equipment Management |

Shared infrastructure includes authentication, authorization, application configuration, common API/error handling, shared layout/components, and database integration.

Changes to shared infrastructure should be coordinated before merging.

---

# Backend Module Boundaries

```text
employee/
    Employee & Organization Management

leave/
    Leave Management

attendance/
    Attendance & Scheduling Management

asset/
    Asset & Equipment Management

auth/
    Shared authentication and authorization

config/
    Shared Spring configuration

common/
    Shared DTOs and exception handling
```

Do not place module-specific business logic inside another member's module.

---

# Frontend Module Boundaries

```text
src/modules/employees/
src/modules/leave/
src/modules/attendance/
src/modules/assets/
```

Reusable application-wide UI belongs under:

```text
src/components/
```

Shared API functionality belongs under:

```text
src/services/
```

Employee, Leave, Attendance/Scheduling and Asset pages load their APIs and expose role-appropriate workflows. Manager/Admin can create departments/teams, onboard/edit employees, configure Leave types and entitlements, and register/edit/assign/return assets. Employees and Supervisors have a My Assets view; Managers can select an employee or an asset to inspect assignment history. Failed reads show unavailable/error with Retry; summaries and empty states appear only after successful reads. Dashboard and Reports connect all four modules.

---

# Git Workflow

Before starting work:

```bash
git pull
```

Create a branch for your work:

```bash
git checkout -b feature/<feature-name>
```

Examples:

```text
feature/attendance-scheduling
feature/leave-management
feature/employee-management
feature/asset-management
```

Make focused commits.

Example:

```bash
git add .
git commit -m "implement attendance schedule API"
```

Push:

```bash
git push -u origin feature/<feature-name>
```

Avoid directly modifying another member's module unless the team has agreed to the change.

Shared files should be changed carefully because they can affect every module.

---

# Important Development Rules

Do not commit:

```text
node_modules/
frontend/dist/
backend/target/
.env
.env.*
.DS_Store
IDE-specific files
database passwords
```

Do not store passwords in source code.

Do not let Hibernate recreate the project database.

Do not change the finalized relational schema without coordinating the change with the group.

Do not implement business logic belonging to another member's module.

Keep frontend API calls inside service files rather than directly scattering `fetch()` calls throughout UI components.

Keep backend responsibilities separated into controller, service, repository, entity, and DTO layers.

---

# Current Development Status

Working:

```text
React/Vite frontend foundation
Spring Boot backend foundation
MySQL database
Spring Data JPA connection
Session authentication, CSRF protection, and account password changes
/api/health endpoint
Finalized database schema
Required role reference data
Database validation scripts
Employee and organization API with directory, onboarding, and edit UI
Attendance and scheduling APIs with role-aware pages and workflows
Asset register and assignment APIs (register, update, assign, return, and history)
Role-scoped My Assets, employee-filtered history and Manager asset controls
Explicit Manager Leave setup and inherited employee self-service for every role
Employee/Leave/Asset permission, rollback and real MySQL concurrency tests
Attendance reporting plus Employee/Leave/Asset summaries, reports and CSV exports
Session revocation after account changes and central frontend expiry handling
Browser first-administrator setup with permanent completion marker
External production configuration and optional operator bootstrap
Native/container deployment templates and local verification commands
```

Still under development:

```text
Client decisions on annual Leave rules, holidays, half-days, resets and accruals
D-03 combined attendance/leave precedence and other unresolved Attendance policies
Historical team ownership, pagination and larger-volume reporting
Production deployment validation
```

The frontend does not invent business records or dashboard metrics. Connected
pages show database records or a genuine empty state. All report sources and
Dashboard summaries use backend-authorized data; failed reads remain distinct from empty data.


## Attendance reporting and dashboard

Reports opens the Attendance source and includes Employee, Leave and Asset sources. The registry (`frontend/src/modules/reports/reportSources.js`) registers each module's report component. Employee/Leave/Asset services supply scoped summaries and tables at `GET /api/employee-reports`, `/api/leave-reports`, and `/api/asset-reports`; their shared renderer exports only the authorized response, quoting cells and neutralizing spreadsheet formulas. Attendance retains its own report/filter/CSV APIs.

Attendance reports provide employee summaries and daily facts, date/team/employee filters, and an authorized CSV export. API: `GET /api/attendance-reports`, `/options`, `/csv`, `/dashboard`; scope is `MINE`, `TEAM` or `ORGANIZATION`. Employees can read only their own data. Supervisors use their current assigned team and active direct reports. Manager/Admin can filter organization data, including inactive employee records; team filters use current membership. Existing date-range validation (maximum 366-day difference) applies to JSON and CSV. Export uses the same authorization/calculation and quotes cells/neutralizes spreadsheet formulas.

Present/Late/Absent/LEAVE counts reflect **stored attendance statuses**; present and late are separate. Recorded hours are summed from existing rows, preserving the existing manual/automatic cap and administrative corrections. Open rows keep their stored hours. Missing calendar-day records are not counted as absent. Approved leave is shown separately as distinct calendar dates covered in the selected range per employee, including days with no remaining shift. It does not create LEAVE attendance rows or recalculate Leave balances. Overlap counts make recorded attendance plus approved leave visible. These facts are not additive: combined leave precedence/attendance percentages are deferred until D-03 is decided. Current-membership scope is preserved pending D-06; lateness and correction/audit policies remain unchanged.

Dashboard shows the backend's own today state/check-in/out, published-shift counts, open check-ins and the same attendance summaries. Employee view is own data; Supervisor uses permitted team data (own fallback if no team is assigned); Manager/Admin sees organization attendance. It refreshes every 30 seconds while visible and supports manual refresh. The Employee/organization, Leave and Asset panels show summaries from their respective report services, refresh every 30 seconds while visible, and retry independently on failure. Counts describe stored records in the displayed scope; no inferred productivity, attendance percentage or combined attendance/leave metric is introduced.

## Employee, Leave and Asset workflows

Employee directory/profile/report access is enforced in the backend: Employee=self; Supervisor=self plus current active direct reports in the Supervisor's assigned team; Manager/Admin=all, including inactive employees. A Supervisor cannot supply another Supervisor ID to the direct-report API. Managers can edit first/last name, email, hire date, department, team, supervisor, title, status and linked account role. Emails remain unique. Supervisor selection requires an active employee with an enabled Supervisor or Manager/Admin account. Contact PUT replaces both nullable phone/address fields; null or blank clears stored values. Creating an inactive employee creates a disabled login when account provisioning is requested.

Organization management appears in the Employee page for Manager/Admin and uses existing department/team creation APIs. Department workforce counts are Manager-only; other Employee pages derive department/team labels from their authorized employee records.

Leave setup is explicit: Manager/Admin creates or edits types and sets each employee/type's **total cumulative entitlement**. A new balance starts with the entered opening grant; an existing balance preserves used days and sets available days to total entitlement minus used days. The grant cannot be below used days, negative, or exceed 999.99 days (the existing decimal field capacity). Increasing a grant adds capacity; reducing it never erases usage. There are no default 14/10/5-day grants, automatic yearly resets, accruals or writes on normal reads. Existing configured balances remain unchanged until a Manager explicitly updates them. Requests keep the existing inclusive calendar-day calculation and overlap/balance validation; weekend/holiday exclusions and half-day requests are not introduced. Every role can submit own requests; only the assigned Supervisor can approve/reject, and self-approval is refused. Manager approval rights and the approver for top-level/unassigned employees need a separate policy decision.

APIs: `POST /api/leave/types`, `PUT /api/leave/types/{id}`, `GET /api/leave/setup/employees/{id}`, and `PUT /api/leave/setup/employees/{employeeId}/types/{typeId}` with `{ "entitlementDays": 20 }`. Setup is Manager-only. The Leave report includes request status counts, request history and current balances; date filters select requests overlapping the range and show whole-request calendar days. Current balances are explicitly independent of date filters. Employee/Leave reports use the same self/current-team/organization scope. Asset reports use own scope for Employee/Supervisor and organization or selected-employee scope for Manager/Admin.

Only Manager/Admin registers, updates, assigns or returns equipment and reads arbitrary assignment histories. Employee/Supervisor inventory reads expose only their own currently assigned assets; their employee-filtered assignment endpoint exposes only their own history. Asset assignment validates an active employee and the asset before mutations, locks employee then asset rows, and saves status/link in one transaction. Return and status edits serialize on the asset row; repeated returns conflict. An active assignment must be returned before manually changing status, and `ASSIGNED` is set only through assignment. Manager-entered condition statuses are supported within the existing 30-character field; only `AVAILABLE` permits assignment. No unconfirmed fixed condition taxonomy is imposed.

Employee deactivation opens an asset review: return each outstanding assignment, then deactivate. It warns clearly and still allows deliberate deactivation with outstanding assets, or without a successful asset review, because no automatic deactivation-blocking rule has been agreed. The sequence retains employee and assignment history and disables login. Any mandatory asset-clearance policy requires group approval.

Finalized OOAD artifacts and `database/01_schema.sql` remain frozen. Leave annual entitlement amounts/reset/accrual/holiday/half-day rules, historical ownership and unresolved Attendance policies remain decisions for the group/client. The manual cumulative-grant rule uses existing fields and requires no schema change.

Run the complete checks with local MySQL configured (the integration suite creates isolated fixture rows and rolls back or cleans up them):

```bash
cd backend
set -a
source .env
set +a
./mvnw -q test
cd ../frontend
node --test test/*.test.mjs
npm run build
cd ..
git diff --check
```

## Shared sessions and production setup

Cached sessions are revalidated against current account identity/active flag/role/password hash before endpoint authorization. Revocation or role/password changes invalidate the session and return 401. Frontend API handling clears expired authentication/CSRF state centrally; authenticated permission failures stay 403. An account change cannot undo an already-running request. Employee and Asset endpoints also enforce the role/ownership boundaries documented below.

See [deployment/README.md](deployment/README.md) for browser first-administrator setup, its additive migration, external secrets, `prod` profile, fresh-only non-destructive schema installation, native/container configuration and remaining owner security gaps. `ems.business-timezone` / `EMS_BUSINESS_TIMEZONE` remains configurable, default Asia/Colombo. Do not run destructive schema/sample scripts against retained data. Finalized OOAD diagrams/scenarios remain frozen and do not follow later implementation refinements automatically.
