# EVOQ EMS Frontend

React and Vite provide the shared application shell, session login, protected routing, and page structure for the four business modules.

## Run locally

Start MySQL and the Spring Boot backend first. From `backend/`, load your ignored `.env` with `source .env`, then run `./mvnw spring-boot:run`.

In a second terminal, from `frontend/`:

```bash
npm install
npm run dev
```

Open `http://localhost:5173`. The API defaults to `http://localhost:8080/api`; set `VITE_API_BASE_URL` in an ignored `frontend/.env` only if the backend uses another origin or port. Run `npm test` and `npm run build` to check the tests and production bundle.

## Current behavior

- Before login, the app checks the backend's first-run setup status. A fresh installation shows a first-administrator form; after creation it opens normal Login and permanently closes setup. Failed status reads show unavailable with Retry. The login page uses the real Spring Security session API.
- Protected routes require `/api/auth/me` to return an authenticated user. Requests include the session cookie and CSRF header.
- The account menu shows the authenticated username and database role, and provides logout and change password.
- Employee, Leave, Attendance/Scheduling and Asset workflows use the real backend APIs. Employee sees own records; Supervisor sees self plus permitted active direct reports in their current team; Manager/Admin manages the organization. Backend authorization remains authoritative.
- Employee management includes organization creation, active Supervisor/Manager selection, official name/email/hire-date edits and nullable contact clearing. Deactivation provides an asset review and Return → Deactivate sequence with explicit outstanding-equipment warnings.
- Manager/Admin configures Leave types and manual cumulative entitlements. Every role can submit own leave. Reads never initialize hidden default grants; errors show unavailable with Retry.
- My Assets shows own equipment/history for Employee/Supervisor. Managers can register/edit equipment, assign/return it and select an employee or asset for history.
- Reports connects Employee, Leave, Attendance and Asset sources, including scoped summaries/tables and CSV exports. Dashboard shows real module summaries, with independent Retry and successful empty states. The UI does not fabricate metrics or business records.
- The SQL seed contains only the three required roles. Apply the separate `07_first_run_setup.sql` migration and use the non-seeding `local` profile to try browser setup. Opting in to `dev` with `DEV_DEMO_PASSWORD` creates reserved accounts `demo.manager`, `demo.supervisor`, and `demo.employee` and closes first-run setup. Normal onboarding can optionally provision later accounts. See the root README for setup and remaining policy decisions.

## Structure

```text
src/
├── app/                 Router, login, protected route, change password
├── components/common/   Reusable UI elements
├── components/layout/   Header and sidebar
├── context/             Auth and theme state
├── modules/             Dashboard and connected business workflows
├── services/            Auth and shared API client
└── styles/              Shared styles
```

The module folders are reserved for the team members who own their business workflows. Shared API calls belong in service files, while backend authorization remains authoritative. The UI must not simulate successful business operations before their real APIs exist.
