# AttendIQ

**Fraud-resistant classroom attendance for universities** — rotating QR sessions, JWT auth, live lecturer dashboards, and CSV export.

Hybrid deployment for UCC beta testing: **Spring Boot web app** on Render + **JavaFX desktop client** for lecturers.

| Environment | URL |
|-------------|-----|
| **Production web** | https://ucc-attendance-system.onrender.com |
| **Production API** | https://ucc-attendance-system.onrender.com |
| **Desktop downloads** | [GitHub Releases](https://github.com/KingdomJoe/attendiq-ucc/releases) |

## Features

- Student & lecturer registration with institutional email validation
- **5-second rotating QR codes** tied to signed session tokens
- Real-time attendance roster (web + desktop presenter view)
- Lecturer course assignment, focus course, enrollment links
- Session types (Lecture / Lab), CSV export, analytics charts
- PWA-friendly student web portal

## Tech stack

| Layer | Stack |
|-------|--------|
| API + Web UI | Java 17, Spring Boot 3, Thymeleaf, HTMX, Spring Security, JWT, ZXing |
| Desktop (lecturers) | JavaFX 21, FXML, `java.net.http` REST client |
| Database | PostgreSQL 16, Flyway |
| Hosting | [Render](https://render.com) |

## Beta testers — quick start

### Students & lecturers (browser)

Open **https://ucc-attendance-system.onrender.com** on any device (phone, tablet, laptop).

| Role | Login | Password |
|------|-------|----------|
| Lecturer | `LEC001` | `lecturer123` |
| Student | `student@ucc.edu.gh` | `student123` |

### Lecturers (Windows desktop)

1. Go to **[Releases](https://github.com/KingdomJoe/attendiq-ucc/releases)** and download the latest:
   - **`AttendIQ-Desktop-*-Setup.exe`** — recommended (includes Java runtime)
   - **`attendance-desktop-*.jar`** — requires Java 17+ installed
2. Run the installer or `java -jar attendance-desktop-*.jar`
3. Sign in as lecturer — the app connects to production automatically

> New releases are published when a version tag is pushed (e.g. `v1.0.0-beta.1`).

## Local development

```bash
# Database + API
docker compose up -d

# Backend (port 8080)
mvn spring-boot:run -pl backend

# Desktop (points at localhost via pom.xml dev option)
mvn javafx:run -pl desktop
```

Render environment variables for production are documented in [`render-env.example`](render-env.example).

## Project layout

```
├── backend/          # Spring Boot REST API + Thymeleaf web UI
├── desktop/          # JavaFX lecturer (and student) desktop client
├── scripts/          # Desktop launcher + jpackage build script
├── docs/             # Architecture guides, E2E checklist
├── render-env.example
└── docker-compose.yml
```

## Documentation

- [APP-PRD.md](APP-PRD.md) — product requirements
- [docs/architecture_reference.md](docs/architecture_reference.md) — hybrid web + desktop architecture
- [docs/E2E-VALIDATION.md](docs/E2E-VALIDATION.md) — acceptance tests
- [render-env.example](render-env.example) — Render production env vars

## License

MIT (add your institution’s policy if deploying on-campus.)
