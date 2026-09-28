# EVOQ Employee Management System

Employee Management System developed for the EVOQ client project.

The system uses a React frontend, Spring Boot REST backend, and MySQL relational database.

## Project Structure

```text
EMS/
├── frontend/                  React + Vite frontend
│   ├── src/
│   │   ├── app/              Routing
│   │   ├── components/       Shared UI components
│   │   ├── context/          Shared React context
│   │   ├── data/mock/        Temporary prototype/mock data
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

To verify the database:

```bash
mysql -u root -p < database/04_validation.sql
```

The validation queries should report no integrity problems.

`03_queries.sql` contains project/demo queries.

`05_negative_constraint_tests.sql` intentionally attempts invalid operations to demonstrate database constraints. Do not treat expected errors from this file as application failures.

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

Run:

```bash
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

Example macOS/Linux:

```bash
export DB_USERNAME=root
export DB_PASSWORD="your-local-mysql-password"
./mvnw spring-boot:run
```

Or for a single command:

```bash
DB_USERNAME=root DB_PASSWORD="your-local-mysql-password" ./mvnw spring-boot:run
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
DB_USERNAME=root DB_PASSWORD="your-password" ./mvnw spring-boot:run
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

Temporary prototype data belongs under:

```text
src/data/mock/
```

Mock data should gradually be removed as real backend endpoints are connected.

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
Spring Security dependency
/api/health endpoint
Finalized database schema
Sample database dataset
Database validation scripts
```

Still under development:

```text
Real authentication
Role-based authorization
Employee API integration
Leave API integration
Attendance & Scheduling API integration
Asset API integration
Removal of remaining frontend mock data
Testing
Deployment
```

The current frontend is based on the approved UI prototype. Some displayed data is still temporary mock data and will be replaced by REST API responses from Spring Boot as module implementation progresses.
