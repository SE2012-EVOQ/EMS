# EVOQ EMS Frontend

React, Vite, React Router and Tailwind CSS provide the application UI. Start MySQL and the backend using the [root README](../README.md#local-run-with-existing-data), then run from this directory:

```bash
npm ci
npm run dev
```

Open [http://localhost:5173](http://localhost:5173). `npm run build` creates `dist/`; `npm run preview` opens the built frontend and still needs the backend running.

## Source structure

```text
src/
  main.jsx             Application entry
  app/                 Routes, login, first setup and account pages
  components/common/   Shared UI elements
  components/layout/   Sidebar, account menu and application layout
  context/             Authentication and theme state
  modules/             Employee, Leave, Attendance, Asset, Dashboard and Reports
  services/            Shared API, authentication and setup clients
  styles/              Shared styles
```

Modules separate page components, reusable feature components, and API services. Reports combine module data through `src/modules/reports/reportSources.js`. [Module workflows](../docs/modules.md) document role permissions and business rules.

The API defaults to `http://localhost:8080/api`. To change it, copy `.env.example` to `.env`, update `VITE_API_BASE_URL` and restart Vite. The backend's `FRONTEND_ORIGIN` must match the browser's exact origin.

`src/services/api.js` sends session cookies, obtains CSRF tokens and handles errors. A session-expiry 401 returns to login; permission/CSRF 403 errors stay within the session. The backend enforces authorization for every request.
