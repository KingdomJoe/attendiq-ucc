# E2E Validation Checklist (AttendIQ)

## Prerequisites

1. Start **Docker Desktop**
2. `docker compose up -d --build`
3. `cd frontend && npm run dev`

## Demo credentials

| Role | Login | Password |
|------|-------|----------|
| Lecturer | `LEC001` | `lecturer123` |
| Student | `student@ucc.edu.gh` | `student123` |

## AttendIQ UI checks

| # | Test | Expected |
|---|------|----------|
| 1 | Landing `/` | AttendIQ branding, role/mode tabs, demo buttons |
| 2 | Demo Lecturer | Navigates to `/lecturer` workspace |
| 3 | Demo Student | Navigates to `/student` workspace |
| 4 | Metric cards | 4 metrics on lecturer and student pages |
| 5 | Session type | Lecture/Practical/Tutorial select before start |
| 6 | QR countdown ring | SVG ring counts down; QR refreshes every 5s |
| 7 | Live badge | Shows when session active |
| 8 | Attendance row animation | New present rows highlight briefly |
| 9 | Export CSV | Downloads `attendance-session-{id}.csv` |
| 10 | Analytics panel | Toggles per-course stats |
| 11 | Student scanner flow | idle → scanning → confirm index → success |
| 12 | History badges | Present (green) on history table |

## API smoke

```bash
curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"identifier":"LEC001","password":"lecturer123","role":"LECTURER"}'
```

## Build verification

- Frontend: `cd frontend && npm run build`
- Backend: `docker compose build backend` (requires Docker engine running)
