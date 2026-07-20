# Sunrise Dental Clinic — Frontend (React SPA)

The **presentation tier** of the 3-tier system. A React + TypeScript single-page app (Vite)
that consumes the Spring Boot REST API. Role-aware: Patient, Receptionist, Dentist, Admin.

## Stack
- **React 18 + TypeScript**, **Vite 5** build
- **react-router-dom** for role-based routing + route guards
- **Recharts** for the admin analytics dashboard
- Plain CSS design system (light/dark aware) — no CSS framework dependency

## Run

```bash
npm install
npm run dev      # http://localhost:5173  (proxies /api -> http://localhost:8080)
npm run build    # type-check + production build into dist/
npm run preview  # serve the production build
```

The Vite dev-server proxies `/api` to the backend (`VITE_API_TARGET`, default
`http://localhost:8080`) so there are **no CORS issues in development**.

## Authentication (dev vs prod)
- **Dev/QA:** the login screen sets `X-User-Uid` + `X-User-Role` headers, which the backend
  `DevHeaderAuthFilter` reads. This lets the whole UI run and be demoed without Firebase.
- **Prod:** swap the header attachment in `src/api/client.ts` for a Firebase
  `Authorization: Bearer <idToken>` header — nothing else changes.

## Works offline / ahead of the backend
`src/api/client.ts#liveOrMock` calls the real endpoint first and falls back to bundled
sample data (`src/api/mock.ts`) on 404 / network error. So the SPA is **fully demoable with
no backend running**, and each screen goes live automatically as its endpoint lands.

### API contract this frontend expects

| Area | Endpoint | Status |
|------|----------|--------|
| Availability | `GET /api/availability?dentistId&date`, `GET /api/availability/week?from&to` | ✅ live (SlotController) |
| Sessions | `POST /api/sessions` | ✅ live |
| Appointments | `POST /api/appointments`, `GET /api/appointments/{no}`, `POST …/cancel`, `POST …/complete` | ✅ live |
| Bills | `POST /api/appointments/{no}/bill`, `GET …/bill` | ✅ live |
| Patients | `POST /api/patients`, `GET /api/patients/{id}` | ✅ live |
| Auth/lockout | `POST /api/auth/{failed,successful}-login`, `GET /api/auth/lock-status`, `POST /api/auth/unlock` | ✅ live |
| Reference | `GET /api/dentists`, `GET /api/treatments` | ⏳ mock (endpoint pending) |
| Reports | `GET /api/admin/reports/{income,footfall}` | ⏳ mock (endpoint pending) |
| AI | `POST /api/ai/{triage,help}` | ⏳ mock (endpoint pending) |
| AI config | `GET/PUT /api/admin/ai-settings` | ⏳ mock (endpoint pending) |
| Staff | `GET /api/admin/staff` | ⏳ mock (endpoint pending) |

## Structure
```
src/
  api/         client (fetch + dev headers + mock fallback), typed endpoints, DTO types, mock data
  auth/        AuthContext (localStorage session) + RequireRole route guard
  components/  Layout (role sidebar), UI primitives, Steps indicator
  pages/       login, help, patient/*, reception/*, dentist/*, admin/*
```

## Reuse note
Patterns adopted from the Idasara Academy platform (see `../docs/idasara-reuse/`): the
role-based route guard + per-role navigation, the multi-step booking wizard's step indicator,
the family-keyed dashboard queries, and the "hint, not gospel" auth-claim handling.
