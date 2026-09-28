# EVOQ EMS Frontend

React/Vite migration of the original `Demo_Fixed.html` EVOQ Employee Management System prototype.

## Why this structure

The prototype was a single HTML file containing layout, styles, hardcoded data, navigation and feature logic. This project separates those responsibilities so each group member can work mainly inside their own module without repeatedly editing one shared file.

The original prototype is kept as `Demo_Fixed_original_reference.html` for visual comparison only. It is not loaded by the React app.

## Stack

- React
- Vite
- React Router
- Tailwind CSS through npm
- Lucide React icons
- JavaScript

The prototype already uses Tailwind utility classes extensively. The migration therefore installs Tailwind through npm instead of keeping the prototype's CDN script. This preserves the current design while removing CDN/runtime configuration from the app. Custom prototype styles are in `src/styles/global.css`.

## Run

```bash
npm install
npm run dev
```

Then open the Vite URL, normally `http://localhost:5173`.

Production build:

```bash
npm run build
npm run preview
```

## Project structure

```text
src/
├── app/
│   └── router.jsx
├── components/
│   ├── common/
│   └── layout/
├── context/
│   ├── EmsContext.jsx
│   └── ThemeContext.jsx
├── data/mock/
│   └── mockDatabase.js
├── modules/
│   ├── dashboard/
│   ├── employees/
│   ├── leave/
│   ├── attendance/
│   ├── assets/
│   └── reports/
├── services/
│   ├── api.js
│   └── authService.js
└── styles/
    ├── global.css
    └── variables.css
```

## Suggested member ownership

| Member | Student ID | Module | Main frontend folder |
|---|---|---|---|
| Roashan.J | IT25100943 | Employee & Organization Management | `src/modules/employees/` |
| Samarappuli V. D. B | IT25100959 | Leave Management | `src/modules/leave/` |
| Ahmed Nadhi | IT25101023 | Attendance & Scheduling Management | `src/modules/attendance/` |
| V.J.Shaarugshan | IT25101699 | Asset & Equipment Management | `src/modules/assets/` |
| Group | — | shared layout/auth/router/dashboard/reports | `src/components/`, `src/context/`, `src/app/` |

## Current migration state

The React app includes:

- responsive shared sidebar/header layout
- light/dark mode
- React Router navigation
- demo role switching for Manager/Admin, Supervisor and Employee
- dashboard views by role
- employee directory/profile and basic add/edit/deactivate UI
- leave balances, requests and supervisor approval/rejection
- attendance list, filters and Manager/Admin correction UI
- team schedule calendar and schedule-entry editing
- schedule overlap validation
- approved-leave conflict validation
- asset register, assignment and return UI
- role-scoped reports with CSV export
- isolated mock data matching the original prototype
- API service scaffolding for Spring Boot integration

## Mock data vs real backend

For now `EmsContext.jsx` uses `src/data/mock/mockDatabase.js`. This is intentional: the first milestone is to make the prototype modular before connecting the backend.

When the Spring Boot API is ready, feature modules should call their service files instead:

```text
React page/component
        ↓
module service
        ↓
src/services/api.js
        ↓
Spring Boot REST API
        ↓
MySQL evoq_ems
```

Do not put `fetch()` calls directly throughout page components.

## Planned backend endpoints

These names are scaffolding, not a frozen API contract:

```text
GET    /api/employees
GET    /api/employees/{id}
GET    /api/leave-requests
POST   /api/leave-requests
GET    /api/attendance
GET    /api/schedules
POST   /api/schedule-entries
GET    /api/assets
POST   /api/assets
```

Agree the final Spring Boot controller routes as a group before replacing the mock context.

## Ahmed's module

Attendance & Scheduling lives in:

```text
src/modules/attendance/
├── components/
│   ├── AttendanceTable.jsx
│   └── ScheduleCalendar.jsx
├── pages/
│   ├── AttendancePage.jsx
│   └── SchedulePage.jsx
└── services/
    └── attendanceService.js
```

The schedule form already checks:

1. end time is after start time;
2. the employee does not have approved leave on the work date; and
3. the employee does not already have an overlapping schedule entry.

These checks must also be implemented in the Spring Boot service. Frontend validation is not a security or integrity boundary.

## Git workflow

Recommended branch names:

```text
feature/employee-management
feature/leave-management
feature/attendance-scheduling
feature/asset-management
```

Keep shared layout changes small and coordinate before changing `src/components/layout`, `src/context`, or `src/app/router.jsx`.

## Next milestone

1. Run and visually compare this project with `Demo_Fixed_original_reference.html`.
2. Fix any visual differences the group wants to keep from the prototype.
3. Freeze the shared frontend shell.
4. Create the Spring Boot backend structure.
5. Implement Attendance & Scheduling as the first complete frontend → REST → MySQL vertical slice.
6. Replace mock calls module-by-module.
