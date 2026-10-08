# EVOQ Employee Management System

EVOQ EMS is an employee management system developed for the EVOQ client project. It provides centralized workforce administration through a React frontend, a Spring Boot REST API, and a MySQL database.

## System Overview

| Module | Functionality |
| --- | --- |
| Employee & Organization Management | Employee records, departments, teams, onboarding, and lifecycle management |
| Leave Management | Leave types, entitlements, requests, and approval workflows |
| Attendance & Scheduling Management | Schedule publication, employee check-in/out, attendance reconciliation, and reporting |
| Asset & Equipment Management | Equipment inventory, assignments, returns, and assignment history |
| Dashboard & Reports | Role-scoped summaries, module reports, filters, and CSV exports |

The system supports Employee, Supervisor, and Manager/Admin roles. The backend enforces role and ownership permissions. Shared authentication uses server-side sessions, CSRF protection, and account validation.

## Technology Stack

| Layer | Technologies |
| --- | --- |
| Frontend | React, Vite, React Router, Tailwind CSS |
| Backend | Java 21, Spring Boot, Spring Security, Spring Data JPA, Bean Validation |
| Database | MySQL 8.x, InnoDB |
| Build tools | Maven Wrapper, npm |

The frontend communicates with the backend through REST endpoints. Backend controllers handle HTTP requests, services enforce business rules and transaction boundaries, and repositories manage database persistence.

## Project Structure

```text
backend/     Java API, configuration and Maven Wrapper
frontend/    React application and Vite configuration
database/    Schema, setup scripts and example queries
Diagrams/    UML diagrams and use-case scenarios
docs/        Module workflows and role permissions
```

## Prerequisites

- Java 21 (Maven Wrapper included).
- Node.js 22.12+ in the 22.x line and npm.
- MySQL 8.x running locally, with the `mysql` client.

## Local Run with Existing Data

Start MySQL before launching the application. Normal startup uses the existing database; `01_schema.sql` is only required for database initialization or an intentional reset.

### Backend

On a new checkout, create the local configuration file from the repository root:

```bash
cp backend/.env.example backend/.env
```

Set the MySQL credentials in `backend/.env` and use `SPRING_PROFILES_ACTIVE="local"`. The default database is `evoq_ems` on `localhost:3306`.

Start the backend in a separate terminal. On macOS/Linux, run from the repository root:

```bash
cd backend
set -a
source .env
set +a
./mvnw spring-boot:run
```

Spring Boot does not load `.env` automatically; the commands above export its values. The API is available on port `8080` after startup. The [health endpoint](http://localhost:8080/api/health) reports the application status.

Windows PowerShell can set the variables directly instead of sourcing `.env`:

```powershell
cd backend
$env:DB_USERNAME = "root"
$env:DB_PASSWORD = Read-Host "Local MySQL password"
$env:SPRING_PROFILES_ACTIVE = "local"
.\mvnw.cmd spring-boot:run
```

### Frontend

Start the frontend in another terminal, from the repository root:

```bash
cd frontend
npm ci
npm run dev
```

The application is available at [http://localhost:5173](http://localhost:5173). Both servers must remain running. Use `npm ci` on a new checkout or after dependency changes.

### Existing Database Updates

For an older database missing runtime roles or the setup marker, stop the backend, open `mysql -u root -p evoq_ems` from the repository root, and run:

```sql
SOURCE database/06_runtime_roles.sql;
SOURCE database/07_first_run_setup.sql;
EXIT;
```

These two scripts preserve existing data and completed setup.

## Fresh Database

**Data reset:** `01_schema.sql` drops and recreates `evoq_ems`, deleting all existing records. Use this procedure only for a new installation or an intentional local reset. Back up retained data before proceeding.

Stop the backend. From the repository root, open `mysql -u root -p` and run:

```sql
SOURCE database/01_schema.sql;
USE evoq_ems;
SOURCE database/06_runtime_roles.sql;
SOURCE database/07_first_run_setup.sql;
EXIT;
```

Start both servers with the `local` profile. Complete **Create first administrator** in the frontend using the administrator's profile and a password of at least 6 characters (maximum 72 UTF-8 bytes). Setup creates the department, active employee, and Manager/Admin account. After signing in, configure departments, teams, employees, Leave types and entitlements, schedules, and assets through the application.

Completed setup stays closed even if accounts are later removed or disabled.

## Configuration

| Variable | Default / purpose |
| --- | --- |
| `DB_USERNAME`, `DB_PASSWORD` | Local MySQL credentials |
| `DB_URL` | JDBC URL; defaults to local `evoq_ems` |
| `SPRING_PROFILES_ACTIVE` | `local` for normal operation; `dev` for optional demo seeding |
| `SERVER_PORT` | Backend port, default `8080` |
| `FRONTEND_ORIGIN` | Browser origin, default `http://localhost:5173` |
| `EMS_BUSINESS_TIMEZONE` | Attendance business timezone, default `Asia/Colombo` |
| `DEV_DEMO_PASSWORD` | Password for seeded accounts in `dev` |

To change the API URL, copy `frontend/.env.example` to `frontend/.env`, update `VITE_API_BASE_URL`, and restart Vite. Set `FRONTEND_ORIGIN` to the browser's exact origin, including its port. Use a consistent hostname for frontend and backend URLs.

### Optional Demo Data

The `dev` profile creates or refreshes demo employees, organization links, and login accounts using `DEV_DEMO_PASSWORD`. Example usernames include `a.perera` / `demo.manager`, `sarah` / `demo.supervisor`, and `ravin` / `demo.employee`. Demo seeding closes first-administrator setup and may refresh seeded records on every startup. Use a disposable local database for this profile. Switching to `local` stops seeding and retains existing records.

## Database Scripts

| Database file | Purpose |
| --- | --- |
| `01_schema.sql` | Finalized 13-table schema; drops/recreates `evoq_ems` |
| `02_sample_data.sql` | Optional departments, teams, roles and Leave types; no employees/accounts |
| `03_queries.sql` | Reference queries for module data and reporting |
| `06_runtime_roles.sql` | Repeatable role seed for the selected database |
| `07_first_run_setup.sql` | Adds the permanent setup marker (14th table) |

## Build

```bash
(cd backend && ./mvnw package)
(cd frontend && npm run build)
```

Build artifacts are generated in `backend/target/` and `frontend/dist/`. These directories, installed dependencies, local `.env` files, and database backups are excluded from version control.

## Documentation

| Document | Contents |
| --- | --- |
| [Module workflows](docs/modules.md) | Business rules, role permissions, reporting, and current limitations |
| [Backend guide](backend/README.md) | API organization and runtime configuration |
| [Frontend guide](frontend/README.md) | UI organization and API integration |
| [OOAD artifacts](Diagrams/) | UML diagrams and use-case scenarios |

The finalized database schema and OOAD artifacts are retained as project references. Schema and business-policy changes require team coordination.

## Module Ownership

| Member | Module |
| --- | --- |
| Roashan J. | Employee & Organization Management |
| Samarappuli V. D. B | Leave Management |
| Ahmed Nadhi | Attendance & Scheduling Management |
| V. J. Shaarugshan | Asset & Equipment Management |
