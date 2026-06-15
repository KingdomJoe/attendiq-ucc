# AttendIQ — Team Briefing Script

Use this when explaining the project to teammates who may be technical or non-technical. Two tracks are provided; mix as needed.

**Production:** https://ucc-attendance-system.onrender.com

---

## The One-Sentence Pitch

AttendIQ is a digital roll-call system where the lecturer holds a constantly changing ticket on screen, and students prove they are physically present by scanning it with their phone — the server is the impartial referee.

---

## Analogy A: Nightclub Bouncer (Non-Technical Teammates)

| Real world | AttendIQ |
|------------|----------|
| Nightclub door | Lecturer's live attendance session |
| Bouncer with a stamp that changes every 5 seconds | Rotating QR code (expires in 5s, single-use) |
| Your ID card | Student account + index number match |
| One person per wristband | One attendance per student per session; one scan per device |
| Guest list | Course enrollment roster |
| Security camera log | Attendance records (IP, browser, timestamp) |
| Manager's back office | Lecturer dashboard (live roster, analytics, CSV export) |
| VIP booth screen for the DJ | JavaFX desktop presenter (fullscreen QR, live check-ins) |

**Say this:** "Think of the lecturer as the venue manager who opens the door for exactly one class period. The QR code is like a wristband stamp that changes every five seconds so you can't text a photo to a friend. The server checks your ID, confirms you're on the guest list, and logs who came in."

---

## Analogy B: Airport Boarding (Technical Teammates)

| Aviation | AttendIQ |
|----------|----------|
| Gate opens for one flight | Attendance session for one course |
| Boarding pass barcode | QR JWT token with session ID + nonce |
| Barcode scanned once at gate | Nonce marked `consumed` in `qr_tokens` |
| Passport name must match ticket | Index number must match logged-in student |
| Passenger manifest | Course enrollment roster |
| TSA / gate agent system | Spring Boot `SessionService` + `AttendanceService` |
| Airline ops dashboard | Lecturer analytics + CSV export |

**Say this:** "We issue short-lived boarding passes, not login credentials. A QR token cannot be reused as an auth token — the JWT filter ignores QR-type claims. The database enforces one passenger per seat via unique constraints on session + student and session + device."

---

## Three-Layer Mental Model (Draw on Whiteboard)

```
[ Web Browser ]     [ JavaFX Desktop ]
  Students scan       Lecturer presents QR
        \                 /
         \               /
          [ Spring Boot ]  ← one brain
          REST + Thymeleaf
                |
          [ PostgreSQL ]   ← one memory
```

**Key talking point:** One backend, one database. Web and desktop are two remote controls for the same system — not two separate apps with duplicate business logic.

---

## Who Does What

### Student (web-first)
1. Register with institutional email
2. Join courses (code or enrollment link)
3. Scan QR during class (camera, upload image, or manual code)
4. View history and per-course heatmaps

Desktop shows courses and history but **does not scan QR** — the phone is the attendance device by design.

### Lecturer (web + desktop)
1. Register with pre-issued lecturer code
2. Assign courses from department catalog
3. Start session → display rotating QR
4. Watch live roster → close session → export CSV / analytics

### Admin
Not built yet. Listed as future scope in the PRD.

---

## 10-Minute Presentation Script

| Minute | Topic | What to say |
|--------|-------|-------------|
| 0–1 | Problem | Manual roll call wastes lecture time. Students can sign for absent friends. Records are scattered. |
| 1–2 | Solution | Lecturer opens a session. Screen shows a QR that refreshes every 5 seconds. Students scan with their phone. Server records presence instantly. |
| 2–4 | Architecture | One Spring Boot server on Render, PostgreSQL database. Students use the web app. Lecturers can use web or Windows desktop for projecting the QR. Same API, same data. |
| 4–6 | Security | Five layers: (1) student must be logged in, (2) index number must match account, (3) must be enrolled in the course, (4) QR expires in 5s and is single-use, (5) one device fingerprint per session. |
| 6–8 | Live demo | Lecturer login → start session → QR on screen. Student on phone → scan → roster updates. See [DEMO_RUNBOOK.md](DEMO_RUNBOOK.md). |
| 8–9 | Build history | Started with React SPA; pivoted to Thymeleaf for simpler deployment. Added JavaFX desktop for classroom presenter. Shipped to Render with Windows installer via GitHub Actions. |
| 9–10 | What's next | Admin dashboard, campus geofencing, AI attendance-risk alerts. See [FUTURE_WORK_SLIDE.md](FUTURE_WORK_SLIDE.md). |

---

## Team Roles Framing (Solo Developer → Team Language)

Even if you built everything alone, frame it so teammates understand ownership lanes:

| Lane | Responsibility | Key files |
|------|----------------|-----------|
| Backend brain | Business rules, API, database | `backend/.../service/`, Flyway migrations |
| Web face | Student/lecturer browser UI | `backend/.../templates/`, `static/` |
| Desktop stage | Lecturer presenter + analytics | `desktop/.../fxml/`, `ApiClient.java` |
| DevOps lane | Docker, Render, CI releases | `docker-compose.yml`, `.github/workflows/` |

---

## FAQ Your Teammates Will Ask

**"Why not just use Google Forms?"**  
Forms have no live session control, no rotating codes, no enrollment binding, and no fraud checks.

**"Why a desktop app?"**  
Projectors and fullscreen QR refresh work better in a dedicated presenter window than a browser tab that might sleep or lose focus.

**"Can a student mark attendance from home?"**  
They need a valid QR from a live session. Without geofencing (future work), a student with a shared screenshot has a narrow 5-second window; fingerprint and index checks block casual proxying.

**"Is our data safe?"**  
Passwords are BCrypt-hashed. JWTs are HttpOnly cookies on web. Production secrets live in Render env vars, not in the repo.

---

## Handoff Checklist for New Team Members

1. Read [README.md](../README.md) — run locally with `docker compose up -d` + `mvn spring-boot:run -pl backend`
2. Skim [architecture_reference.md](architecture_reference.md) — understand cookie vs Bearer auth
3. Walk through [DEMO_RUNBOOK.md](DEMO_RUNBOOK.md) once on production
4. Review [DEFENSE_QA_FLASHCARDS.md](DEFENSE_QA_FLASHCARDS.md) before any presentation
