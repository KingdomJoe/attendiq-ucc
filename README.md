# AttendIQ

**Fraud-resistant classroom attendance for universities** — dynamic QR sessions, JWT auth, device fingerprinting, and live lecturer dashboards.

Built for UCC (University of Cape Coast) as a full-stack MVP: Spring Boot API + React (AttendIQ UI).

## Features

- Student & lecturer registration with institutional email validation
- **5-second rotating QR codes** tied to signed session tokens
- Real-time attendance list with present/absent status
- Device fingerprint + index verification to reduce proxy attendance
- Session types (Lecture / Practical / Tutorial), CSV export, basic analytics
- Demo login for quick exploration

## Tech stack

| Layer | Stack |
|-------|--------|
| API | Java 17, Spring Boot 3, Spring Security, JWT, ZXing |
| Database | PostgreSQL 16, Flyway |
| Web | React 18, TypeScript, Vite, Tailwind CSS |

## Quick start

```bash
# 1. API + database (Docker)
docker compose up -d --build

# 2. Frontend
cd frontend
cp .env.example .env
npm install
npm run dev
```

- App: http://localhost:5173  
- API: http://localhost:8080/api  

### Demo accounts

| Role | Login | Password |
|------|-------|----------|
| Lecturer | `LEC001` | `lecturer123` |
| Student | `student@ucc.edu.gh` | `student123` |

## Project layout

```
├── backend/          # Spring Boot REST API
├── frontend/         # AttendIQ React app
├── docs/               # PRD notes, prototype analysis, E2E checklist
├── APP-PRD.md          # Product requirements
└── docker-compose.yml
```

## Documentation

- [APP-PRD.md](APP-PRD.md) — product requirements
- [docs/PROTOTYPE-NOTES.md](docs/PROTOTYPE-NOTES.md) — AttendIQ UI mapping
- [docs/E2E-VALIDATION.md](docs/E2E-VALIDATION.md) — acceptance tests

## License

MIT (add your institution’s policy if deploying on-campus.)
