# AttendIQ Prototype Analysis

**Source files:**
- [`smart_attendance_system_prototype.html`](../smart_attendance_system_prototype.html) (interactive mock)
- [`prototype-full.png`](prototype-full.png) (agent-browser capture)
- [Claude share](https://claude.ai/share/987b5dfd-cb9d-432b-9917-936e71c7c9a2)

---

## Architecture (prototype)

Single-page app with three screens toggled via `.scr.on`:

| Screen ID | Purpose |
|-----------|---------|
| `scr-auth` | Unified login/register |
| `scr-lecturer` | Session control + live attendance |
| `scr-student` | Scanner + history |

No backend — QR drawn on canvas (`drawQR`), attendance simulated with `simJoin()` timeouts.

**Production mapping:** React Router workspaces + Spring Boot API; real signed QR from ZXing/JWT.

---

## Brand and design tokens

| Token | Value |
|-------|--------|
| Product name | **AttendIQ** |
| Primary | `#1D9E75` |
| Primary hover | `#0F6E56` |
| Present badge bg/text | `#EAF3DE` / `#3B6D11` |
| Absent badge bg/text | `#FCEBEB` / `#A32D2D` |
| Live badge | `#E1F5EE` / `#0F6E56` |
| Body font | system-ui, 13px |
| Label style | 11px uppercase, letter-spacing 0.04em |
| Card | 0.5px border, rounded-lg, white background |
| Metrics grid | 4 columns (`g4`) |

Icons: Tabler Icons (`ti ti-*`).

---

## Screen 1: Auth (`scr-auth`)

**Nav:** AttendIQ logo + “Secure · JWT” chip.

**Controls:**
- Role tabs: Student | Lecturer
- Mode tabs: Login | Register
- Register-only: Full name, Index number / Lecturer code (label switches)
- Email (students) or identifier field; Password
- Primary: Continue
- Demo Student | Demo Lecturer (skip form)

**Routes (production):** `/` → `AuthPage`

---

## Screen 2: Lecturer (`scr-lecturer`)

**Nav:** AttendIQ + chip “Dr. Name · CSC” + logout.

**Metrics (4):** Enrolled | Present | Absent | Rate (%)

**Left card — Session control**
- Idle: course `<select>`, session type (Lecture/Practical/Tutorial), Start session
- Active: QR 220×220 + SVG countdown ring (5s), token `ATT-XXXXXXXX`, “Refreshing in Ns”, End session, Live badge

**Right card — Attendance**
- Table: Student, Index, Time, Status (Present badge)
- Row animation on new present (`row-new`)
- Footer: Export CSV, Analytics (stub toast in prototype)

**Info bar:** Explains 5s cryptographic QR rotation.

**State machine:**

```
idle → (start) → active → (poll QR 5s, poll attendance 3s) → (end) → idle
```

**Routes (production):** `/lecturer` → `LecturerWorkspace`

---

## Screen 3: Student (`scr-student`)

**Nav:** AttendIQ + chip “Name · Index” + logout.

**Metrics (4):** Sessions | Attended | Missed | Rate (%)

**Left card — Mark attendance**
- `idle`: scanner box placeholder + Open scanner
- `scanning`: animated corners + scan line + “Detecting QR…”
- `success`: green check ring, course name, time + device chips, Scan another

**Production adds `confirm` state** (index verification — PRD requirement).

**Right card — History**
- Table: Course, Date, Status (Present/Absent badges)
- Info bar: 75% eligibility warning

**Routes (production):** `/student` → `StudentWorkspace`

---

## Prototype vs production

| Feature | Prototype | Production |
|---------|-----------|------------|
| QR | Fake canvas pattern | Server Base64 PNG + signed JWT |
| Token | Random `ATT-*` string | JWT `token` field in QrResponse |
| Attendance | `simJoin` delays | `POST /attendance/scan` |
| Index verify | None | Required after scan |
| Session type | UI select only | `session_type` column + API |
| Export CSV | Toast only | `GET /sessions/{id}/attendance/export` |
| Analytics | Toast only | `GET /lecturer/analytics` |
| Stats | Hardcoded 32 | `/lecturer/stats`, `/student/stats` |

---

## API additions for parity

| Method | Path |
|--------|------|
| GET | `/lecturer/stats?sessionId=` |
| GET | `/student/stats` |
| GET | `/lecturer/analytics` |
| GET | `/sessions/{id}/attendance/export` |
| POST | `/sessions` body includes `sessionType` |
