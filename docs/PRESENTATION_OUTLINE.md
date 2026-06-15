# AttendIQ — Presentation Outline (Slide-by-Slide)

Use this as a PowerPoint/Google Slides blueprint. ~18 slides, ~15–20 minutes with demo.

---

## Slide 1: Title

**AttendIQ — Fraud-Resistant Classroom Attendance**

- Joseph Fiah / UCC
- Smart Attendance System
- https://ucc-attendance-system.onrender.com

---

## Slide 2: The Problem

- Manual roll call wastes 5–10 minutes per lecture
- Proxy attendance ("signing for friends")
- Paper/spreadsheet records are error-prone
- No real-time visibility for lecturers

**Speaker note:** Open with a relatable classroom moment.

---

## Slide 3: Our Solution (One Sentence)

**Digital roll-call with a QR code that changes every 5 seconds — students scan with their phone; the server records presence instantly.**

---

## Slide 4: Demo Video / Live Demo Teaser

Screenshot: lecturer session screen with QR + student phone scanning.

**Speaker note:** "We'll show this live in a few minutes."

---

## Slide 5: Who Uses It

| Role | Primary client | Key actions |
|------|----------------|-------------|
| Student | Web (phone) | Scan QR, view history, join courses |
| Lecturer | Web + Desktop | Start session, project QR, analytics, CSV |
| Admin | *Future* | Department oversight |

---

## Slide 6: Architecture — One Brain, Two Doors

Diagram: Web + Desktop → Spring Boot → PostgreSQL

**Talking point:** Not two apps — one API, one database, two clients.

---

## Slide 7: Tech Stack

| Layer | Choice |
|-------|--------|
| Backend | Java 17, Spring Boot 3.3.5 |
| Web | Thymeleaf + vanilla JS |
| Desktop | JavaFX 21 |
| DB | PostgreSQL 16, Flyway |
| Deploy | Render + GitHub Actions |

---

## Slide 8: The Attendance Flow

Sequence diagram (5 steps):
1. Lecturer starts session
2. Server issues rotating QR (JWT + nonce)
3. Student scans
4. Server validates (auth, enrollment, fingerprint, single-use)
5. Live roster updates

---

## Slide 9: Anti-Fraud Design

Seven layers (bullet list):
- 5-second QR expiry
- Single-use nonce
- Signed JWT
- Logged-in student only
- Index number match
- Course enrollment check
- One device per session

---

## Slide 10: Database Model (Simplified)

Core entities: Department → Course → Student/Lecturer enrollments → Session → QR tokens → Attendance records

**Speaker note:** Unique constraints enforce one student + one device per session.

---

## Slide 11: Hybrid Auth

| Web | Desktop |
|-----|---------|
| HttpOnly cookie | Bearer header |
| Thymeleaf forms | ApiClient REST |

Same JWT, different transport.

---

## Slide 12: Implementation Journey

Timeline bar:
- May 2026: Spring Boot + React prototype
- Pivot: Thymeleaf monolith
- JavaFX desktop module
- Render production + Windows installer

9 commits, each a major phase.

---

## Slide 13: Challenges We Solved

Top 5 (from git history):
1. React → Thymeleaf deployment simplification
2. Dual-client auth (cookie vs Bearer)
3. Camera QR compatibility (fallbacks)
4. Lecturer code validation
5. Desktop UI binding + silent failures

---

## Slide 14: Screenshots — Student Web

- Student dashboard: scan, history, courses
- Course heatmap

---

## Slide 15: Screenshots — Lecturer Desktop

- Live QR presenter (fullscreen)
- Analytics chart
- CSV export

---

## Slide 16: LIVE DEMO

Follow [DEMO_RUNBOOK.md](DEMO_RUNBOOK.md):
1. Lecturer login → start session
2. Student phone scan
3. Show roster update

**Fallback:** pre-recorded video if Render is cold-starting.

---

## Slide 17: Future Work (Closing)

See [FUTURE_WORK_SLIDE.md](FUTURE_WORK_SLIDE.md) — three pillars:
1. **Admin dashboard** — institutional oversight
2. **Campus geofencing** — location-based fraud prevention
3. **AI risk alerts** — predict at-risk students early

---

## Slide 18: Q&A

**Backup slide:** Demo credentials, GitHub repo, documentation index.

- Lecturer: `LEC001` / `lecturer123`
- Student: `student@ucc.edu.gh` / `student123`
- Docs: `docs/PROJECT_MASTERY_GUIDE.md`

---

## Appendix Slides (Optional)

- A1: Full REST API table
- A2: Flyway migration history V1–V5
- A3: PRD gaps (admin, PWA, advanced reporting)
- A4: Architecture evolution (monolith → microservices)

---

## Timing Guide

| Section | Minutes |
|---------|---------|
| Problem + solution | 3 |
| Architecture + security | 5 |
| Implementation story | 3 |
| Live demo | 5 |
| Future work + Q&A | 4 |
