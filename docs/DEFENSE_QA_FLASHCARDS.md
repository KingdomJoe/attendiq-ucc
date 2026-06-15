# AttendIQ — Defense Q&A Flashcards

Print double-sided or use for mock viva practice. **50+ questions** with model answers.

---

## Section A: Problem & Product (1–8)

### A1. What problem does AttendIQ solve?
**Proxy attendance and slow manual roll calls.** The system ties presence to a live lecturer session with time-limited, signed, single-use QR tokens.

### A2. Who are the target users?
**Students** (mark attendance, view history) and **lecturers** (run sessions, view analytics). Admins are planned but not implemented.

### A3. What makes this "smart" vs a paper register?
**Real-time validation**, rotating QR codes, device checks, digital records, analytics, and CSV export — all centralized in PostgreSQL.

### A4. What is the product name and institution context?
**AttendIQ** — built for UCC (University of Cape Coast) beta testing with seeded departments and courses.

### A5. Is this web-only?
**No — hybrid.** Web for students and lecturers; **JavaFX desktop** optimized for lecturer QR presentation on Windows.

### A6. What session types are supported?
**LECTURE, PRACTICAL, TUTORIAL** (enum `SessionType`).

### A7. How does a student join a course?
By **course code**, **enrollment link** (`/enroll/{token}`), or from an available-courses list after login.

### A8. What is not implemented from the PRD?
**Admin role**, department-wide analytics, full PWA offline mode, advanced institutional reporting.

---

## Section B: Architecture (9–18)

### B9. Describe the high-level architecture in one sentence.
**Monolithic Spring Boot backend** serving REST + Thymeleaf web UI and a **JavaFX desktop client**, backed by **PostgreSQL** on Render.

### B10. Why one backend for web and desktop?
**Single source of truth** for business logic — no duplicated rules, one database, one deployment for the API and web UI.

### B11. How does web authentication differ from desktop?
| Web | Desktop |
|-----|---------|
| JWT in **HttpOnly cookie** | JWT in **SessionManager** (memory) |
| Form login via `WebController` | `POST /auth/login` via `ApiClient` |
| `GET /logout` clears cookie | `SessionManager.clearSession()` |

### B12. How does `JwtAuthenticationFilter` extract tokens?
**First** `Authorization: Bearer` header (desktop), **then** `token` cookie (web).

### B13. Why stateless sessions?
**Scalability and API consistency** — JWT carries identity; no server-side HTTP session store needed.

### B14. What is the Maven module structure?
Root `pom.xml` → **`backend`** (Spring Boot) + **`desktop`** (JavaFX).

### B15. Where is the web UI if there is no React app?
**Thymeleaf templates** in `backend/src/main/resources/templates/` with static JS/CSS.

### B16. Why was React removed?
**Operational simplicity** — one JAR deploy, no npm/CORS pipeline, faster solo-dev iteration. Commit `fc0c3e1`.

### B17. What is Flyway's role?
**Versioned SQL migrations** (V1–V5). Hibernate `ddl-auto: validate` — schema never auto-mutates in production.

### B18. What is the deployment target?
**Render** for web/API; **GitHub Releases** for desktop JAR and Windows `.exe`.

---

## Section C: Security & Anti-Fraud (19–32)

### C19. List the anti-fraud layers.
1. Authenticated student  
2. Index number matches account  
3. Enrolled in course  
4. QR expires in **5 seconds**  
5. QR nonce **single-use** (`consumed` flag)  
6. **One attendance per student** per session (DB unique)  
7. **One device fingerprint** per session (DB unique)

### C20. What is the QR TTL and why 5 seconds?
Configured in `application.yml` as `app.qr.ttl-seconds: 5`. Short enough to defeat screenshot sharing; long enough for camera latency.

### C21. TRICKY: Can students fake the device fingerprint?
**Partially.** Fingerprint is client-supplied (browser hash). It's **one layer**, not sole security. Index match, enrollment, auth, and single-use QR provide defense in depth. **Next step:** geofencing or SSO.

### C22. TRICKY: Why is CSRF disabled?
**Trade-off** for JWT cookie auth with stateless API. Mitigated by HttpOnly cookies and same-origin forms. **Improvement:** re-enable CSRF tokens on Thymeleaf forms.

### C23. TRICKY: Can a QR JWT be used to log in as a user?
**No.** `JwtAuthenticationFilter` skips tokens with a `type` claim — QR tokens only work for `POST /attendance/scan`.

### C24. TRICKY: Two students scan the same QR simultaneously?
**First wins** — nonce marked consumed; second gets `QR_CONSUMED`. DB transactions + unique constraints handle races.

### C25. How are passwords stored?
**BCrypt** via Spring `PasswordEncoder` in `AuthService`.

### C26. How are lecturer accounts protected from impersonation?
**Pre-seeded `lecturer_codes`** — code claimed once at registration (Flyway V4).

### C27. What prevents replay of an old QR screenshot?
**5s expiry** + **nonce consumption** + **JWT signature** + hash stored in `qr_tokens`.

### C28. What data is logged per attendance record?
**Device fingerprint, IP address, user-agent, timestamp**, status PRESENT.

### C29. What happens if a friend tries to scan with their phone while logged in as themselves for another student?
**Blocked** — submitted index number must match the authenticated student's account.

### C30. How do you prevent one phone marking for two students?
**Unique constraint** on `(session_id, device_fingerprint)` in `attendance_records`.

### C31. Is the system production-ready on Render free tier?
**Beta/pilot ready**, not enterprise production. Cold starts, single instance, sleep on idle — needs paid tier, monitoring, backups for full production.

### C32. Single point of failure?
**Yes** — monolith + single Postgres. Future: replicas, load balancer, Redis for QR cache.

---

## Section D: Database & API (33–42)

### D33. Name the core tables.
`departments`, `courses`, `students`, `lecturers`, `lecturer_courses`, `student_courses`, `attendance_sessions`, `qr_tokens`, `attendance_records`, `lecturer_codes`.

### D34. ORM used?
**Spring Data JPA / Hibernate.**

### D35. Key REST endpoints for attendance flow?
`POST /sessions` → `GET /sessions/{id}/qr` → `POST /attendance/scan` → `GET /sessions/{id}/attendance`.

### D36. Who can call `POST /sessions`?
**Lecturers only** — `@PreAuthorize` on `SessionController`.

### D37. What does `GET /api/lecturer/analytics` return?
**Per-course metrics** for charts (desktop analytics + web dashboards).

### D38. How is CSV export done?
`GET /sessions/{id}/attendance/export` via `ExportService`; desktop uses `CsvExportHelper`.

### D39. What is course "focus"?
**Active course flag** (V5 migration) — dashboard metrics follow the lecturer's focused course.

### D40. What is an enrollment token?
**Public link** `/enroll/{token}` letting students join a course (and optionally register).

### D41. How are stale lecturer sessions handled?
`StaleSessionReconciler` + `closeAllActiveSessions` when creating a new session.

### D42. CORS configuration?
`APP_CORS_ORIGINS` environment variable in `application.yml`.

---

## Section E: Desktop & Web UI (43–50)

### E43. Why JavaFX for desktop?
**Native Windows presenter** — fullscreen QR, auto-refresh, reliable projection vs browser tab management.

### E44. How many FXML screens?
**10** — login, lecturer dashboard/courses/detail/session/summary/analytics, student dashboard/course/history.

### E45. Can students scan QR on desktop?
**No** — attendance marking is **web-only** (camera on phone).

### E46. How does desktop talk to the backend?
`ApiClient.java` using `java.net.http.HttpClient` with Bearer token from `SessionManager`.

### E47. Desktop entry point?
`Launcher.java` → `Application.launch(App.class)` for fat-JAR compatibility.

### E48. Student QR scanning options on web?
**Camera** (html5-qrcode), **upload QR image**, **manual session code**.

### E49. What JavaFX UI bugs were fixed?
Table binding, CSV export, session presenter layout, analytics chart flicker (commit `35cfccd`).

### E50. IDE shows unresolved imports in desktop — is code broken?
**Usually false positive.** Run `mvn compile -pl desktop` — see `desktop_ide_error_triage.md`.

---

## Section F: Implementation History & Process (51–55)

### F51. How many commits on main and what does that imply?
**9 commits** — each is a **major phase**, not daily granularity. Story: React → Thymeleaf → desktop → production.

### F52. TRICKY: Was removing React a step backward?
**No for this context** — strategic simplification for solo/small team and single deploy target. Trade-off: less SPA interactivity.

### F53. What was the biggest auth bug fixed?
**Web logout broken** by Spring's default LogoutFilter — fixed with custom `GET /logout` in `WebController`.

### F54. How is seed data managed?
**Flyway-only** after V4 — removed runtime `DataSeeder.java` for reproducibility.

### F55. How would you test end-to-end?
Follow `docs/E2E-VALIDATION.md`; demo accounts `LEC001` / `student@ucc.edu.gh`.

---

## Section G: Future Work & AI (56–60)

### G56. Top 3 recommended future improvements?
1. **Admin dashboard** — department oversight  
2. **Campus geofencing** — Wi-Fi/GPS fraud layer  
3. **AI attendance-risk prediction** — flag at-risk students early

### G57. How could LLMs help lecturers?
**Natural-language analytics** — "Which students dropped below 75% in CSC201?" over aggregated stats (with PII guardrails).

### G58. How could ML help without LLMs?
**Logistic regression** on attendance trends to predict exam disqualification risk.

### G59. What about facial recognition?
**Research direction only** — high privacy/ethics bar; not in current scope.

### G60. Architecture evolution path?
Monolith today → API gateway + auth service + attendance service + Redis QR cache + ML worker tomorrow.

---

## Mock Viva Rapid-Fire Drill

Practice answering in **under 15 seconds** each:

| # | Question | One-word anchor |
|---|----------|-----------------|
| 1 | ORM? | JPA/Hibernate |
| 2 | Hashing? | BCrypt |
| 3 | QR lib? | ZXing |
| 4 | QR TTL? | 5 seconds |
| 5 | Roles? | STUDENT, LECTURER |
| 6 | DB? | PostgreSQL 16 |
| 7 | Migrations? | Flyway V1–V5 |
| 8 | Java version? | 17 |
| 9 | Desktop HTTP? | java.net.http |
| 10 | CI? | GitHub Actions |

---

## Tricky Four — Deep Practice

Spend 5 minutes each rehearsing aloud:

1. **Fingerprint spoofing** → defense in depth + geofencing next  
2. **CSRF disabled** → trade-off + HttpOnly + future CSRF tokens  
3. **QR JWT vs auth JWT** → `type` claim ignored by filter  
4. **React removal** → deploy simplicity for solo dev / beta
