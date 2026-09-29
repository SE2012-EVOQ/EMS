# EVOQ EMS Frontend

React and Vite provide the shared application shell, session login, protected routing, and page structure for the four business modules.

## Run locally

Start MySQL and the Spring Boot backend first. From `backend/`, load your ignored `.env` with `source .env`, then run `./mvnw spring-boot:run`.

In a second terminal, from `frontend/`:

```bash
npm install
npm run dev
```

Open `http://localhost:5173`. The API defaults to `http://localhost:8080/api`; set `VITE_API_BASE_URL` in an ignored `frontend/.env` only if the backend uses another origin or port. Run `npm run build` to check the production bundle.

## Current behavior

- The login page uses the real Spring Security session API. Development accounts sign in through that API like other users.
- Protected routes require `/api/auth/me` to return an authenticated user. Requests include the session cookie and CSRF header.
- The account menu shows the authenticated username and database role, and provides logout and change password.
- The dashboard and Employee, Leave, Attendance, Schedule, Asset, and Reports routes retain their layout and honest empty states. Their business APIs are not integrated yet. They do not display or modify fictional records.
- The SQL seed contains only the three required roles. Starting the backend with the explicit `dev` profile creates three reserved access accounts: `demo.manager`, `demo.supervisor`, and `demo.employee`. With the shared example `DEV_DEMO_PASSWORD=EvoqDemo2026!`, each uses `EvoqDemo2026!`. These accounts provide access to the protected shell and empty module pages; normal Employee onboarding and first Manager/Admin bootstrap remain open project work.

## Structure

```text
src/
├── app/                 Router, login, protected route, change password
├── components/common/   Reusable UI elements
├── components/layout/   Header and sidebar
├── context/             Auth and theme state
├── modules/             Dashboard and business page shells
├── services/            Auth and shared API client
└── styles/              Shared styles
```

The module folders are reserved for the team members who own their business workflows. Shared API calls belong in service files, while backend authorization remains authoritative. The UI must not simulate successful business operations before their real APIs exist.
