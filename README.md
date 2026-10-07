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
│   └── 05_negative_constraint_tests.sql
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
```

Enter your own local MySQL password when prompted.
`02_sample_data.sql` now inserts only the three required roles:
`EMPLOYEE`, `SUPERVISOR`, and `MANAGER_ADMIN`. It creates no employees,
accounts, departments, teams, leave, attendance, schedules, or assets. A
freshly initialized database has no login account.

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

The database contains 13 main tables:

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
also enables the explicit `dev` profile and its public demo login password:

```dotenv
export DB_USERNAME="root"
export DB_PASSWORD="your-local-mysql-password"
export SPRING_PROFILES_ACTIVE="dev"
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

## Development sign-in

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
pages now use their APIs; leave, dashboard, and reports still await integration.
A legitimate first Manager/Admin bootstrap remains separate work.

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

Employee, attendance, schedule, and asset pages now load data from their APIs.
Employee and attendance/schedule pages also provide management actions. The asset
page currently lists records; its register, update, assign, and return actions
are available through the API but do not yet have page controls. Leave, dashboard,
and reports still show empty states.

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
Asset page listing records from the API
Authentication, employee, and attendance/scheduling tests
```

Still under development:

```text
First Manager/Admin account bootstrap
Complete module-specific role authorization
Leave API integration
Asset page controls for register, update, assign, return, and assignment history
Dashboard metrics and reports integration
Broader integration testing, including asset workflows
Deployment
```

The frontend does not invent business records or dashboard metrics. Connected
pages show database records or a genuine empty state; dashboard, leave, and
reports still await their data sources.
