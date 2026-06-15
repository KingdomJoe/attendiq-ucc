# AttendIQ — Project Mastery Guide

Complete study and presentation reference for defense prep, team briefings, and professor Q&A.

**Production:** https://ucc-attendance-system.onrender.com

---

## Quick Navigation

| Document | Purpose |
|----------|---------|
| [TEAM_BRIEFING_SCRIPT.md](TEAM_BRIEFING_SCRIPT.md) | Analogies + 10-minute team presentation |
| [DEFENSE_QA_FLASHCARDS.md](DEFENSE_QA_FLASHCARDS.md) | 60 Q&A cards including tricky viva questions |
| [PRESENTATION_OUTLINE.md](PRESENTATION_OUTLINE.md) | Slide-by-slide deck outline |
| [EXECUTIVE_SUMMARY.md](EXECUTIVE_SUMMARY.md) | 2-page report appendix |
| [DEMO_RUNBOOK.md](DEMO_RUNBOOK.md) | Step-by-step live demo script |
| [FUTURE_WORK_SLIDE.md](FUTURE_WORK_SLIDE.md) | Closing slide: admin, geofencing, AI |

---

## Part 1: System Overview (Analogy)

**One sentence:** AttendIQ is a digital roll-call where the lecturer displays a constantly changing ticket, students scan it with their phone, and the server acts as impartial referee.

See [TEAM_BRIEFING_SCRIPT.md](TEAM_BRIEFING_SCRIPT.md) for nightclub and airport analogies, role breakdown, and the 10-minute script.

---

## Part 2: Tech Stack

| Layer | Technology |
|-------|------------|
| Language | Java 17 |
| Backend | Spring Boot 3.3.5, Spring Security 6, JWT (jjwt 0.12.6) |
| Web UI | Thymeleaf + vanilla JS + CSS |
| Desktop | JavaFX 21 + FXML |
| QR | ZXing 3.5.3 |
| Database | PostgreSQL 16 + Flyway V1–V5 |
| Hosting | Render + Docker Compose (local) |
| CI/CD | GitHub Actions (desktop JAR + Windows EXE) |

**Note:** README mentions HTMX; codebase uses Thymeleaf + vanilla JS. Initial React/Vite frontend was removed in commit `fc0c3e1`.

---

## Part 3: Implementation Timeline

| Phase | Commit | Highlights |
|-------|--------|------------|
| 0 Foundation | `be21e0b` | Spring Boot API, React UI, Flyway V1–V3, anti-fraud constraints |
| 1 Web consolidation | `fc0c3e1` | Removed React; Thymeleaf + PWA scaffolding |
| 2 UX audit | `4ed7c3e` | Course pages, session summaries, camera fixes |
| 3 Institutional data | `c91dce7` | Lecturer codes, Flyway V4 seed data |
| 4 Hybrid desktop | `f03f4c3` | JavaFX module, dual auth fixes, CI pipeline |
| 5 Course depth | `35cfccd` | Enrollment links, focus course, desktop UI fixes |
| 6 Production | `e2928cd`, `38356b5` | Render wiring, jpackage Windows installer |

---

## Part 4: Key Issues & Solutions

| Problem | Solution |
|---------|----------|
| Cookie vs Bearer auth mismatch | Dual-path `JwtAuthenticationFilter` |
| React deploy complexity | Pivot to Thymeleaf monolith |
| Camera QR failures | Upload fallback + manual code entry |
| Lecturer impersonation | `lecturer_codes` table (V4) |
| Proxy attendance | Index match + device fingerprint + single-use QR |
| Multiple active sessions | `StaleSessionReconciler` |
| Desktop silent login failures | Daemon threads + exception handlers |

Full table in plan Part 4; code references in [architecture_reference.md](architecture_reference.md).

---

## Part 5: Defense Questions

- **Straightforward (8):** problem, JWT, PostgreSQL, QR TTL, fraud prevention, roles, desktop rationale, Flyway
- **Tricky (10):** fingerprint limits, CSRF, QR JWT vs auth JWT, race conditions, React removal, Render tier, stale sessions, PRD gaps, SPOF, testing
- **Rapid fire:** ORM, BCrypt, ZXing, session types, CORS, etc.

All with model answers: [DEFENSE_QA_FLASHCARDS.md](DEFENSE_QA_FLASHCARDS.md)

---

## Part 6–9: Demo, Files, Credentials

**Demo credentials:**
- Lecturer: `LEC001` / `lecturer123`
- Student: `student@ucc.edu.gh` / `student123`

**Files to know cold:**

| Topic | File |
|-------|------|
| QR issue/validate | `backend/.../service/SessionService.java` |
| Attendance scan | `backend/.../service/AttendanceService.java` |
| Dual auth | `backend/.../security/JwtAuthenticationFilter.java` |
| Web routes | `backend/.../controller/WebController.java` |
| Desktop API | `desktop/.../ApiClient.java` |
| Schema | `backend/src/main/resources/db/migration/V1__init.sql` |

**Live demo:** [DEMO_RUNBOOK.md](DEMO_RUNBOOK.md)

**Future work closing slide:** [FUTURE_WORK_SLIDE.md](FUTURE_WORK_SLIDE.md)

---

## Architecture Diagrams

### Three-layer model

```
[ Web Browser ]     [ JavaFX Desktop ]
        \                 /
         \               /
          [ Spring Boot ]
                |
          [ PostgreSQL ]
```

### Attendance scan sequence

1. Lecturer `POST /sessions`
2. Loop: `GET /sessions/{id}/qr` (5s TTL, new nonce each time)
3. Student `POST /attendance/scan` (token + index + fingerprint)
4. Server validates JWT, consumes nonce, checks enrollment + uniqueness
5. Lecturer `GET /sessions/{id}/attendance` — live roster updates

---

## Related Project Docs

- [APP-PRD.md](../APP-PRD.md) — product requirements
- [architecture_reference.md](architecture_reference.md) — hybrid auth deep dive
- [safe_change_patterns.md](safe_change_patterns.md) — refactoring rules
- [E2E-VALIDATION.md](E2E-VALIDATION.md) — acceptance checklist
