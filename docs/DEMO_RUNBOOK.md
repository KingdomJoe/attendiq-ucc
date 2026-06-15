# AttendIQ — Live Demo Runbook

Step-by-step script for presenting the full attendance flow. Tested against production API.

**URL:** https://ucc-attendance-system.onrender.com

---

## Prerequisites

| Item | Details |
|------|---------|
| Lecturer account | `LEC001` / `lecturer123` |
| Student account | `student@ucc.edu.gh` / `student123` |
| Devices | Laptop (lecturer) + phone (student), or two browser profiles |
| Network | Internet access; allow 30–90s for Render cold start |
| Optional | AttendIQ Desktop `.exe` from GitHub Releases |

---

## Cold Start Warning

Render free tier **sleeps when idle**. First request after sleep may take **30–90 seconds**.

**Before presenting:**
1. Open https://ucc-attendance-system.onrender.com in a tab 2 minutes early
2. Or hit `GET /departments` to wake the server
3. Keep a screenshot/video backup if the network fails

---

## Demo Path A: Web-Only (Recommended for Presentation)

### Step 1 — Wake the server (30 sec before demo)

Open in browser: `https://ucc-attendance-system.onrender.com/departments`

Expected: JSON array of departments (e.g. CSC, ENG, …).

### Step 2 — Lecturer: sign in (laptop, Browser A)

1. Go to https://ucc-attendance-system.onrender.com
2. Login tab → Role: **Lecturer**
3. Identifier: `LEC001`, Password: `lecturer123`
4. Submit → redirect to `/lecturer`

**Say:** "The lecturer sees their dashboard — stats, courses, and session controls."

### Step 3 — Lecturer: start a session

1. Ensure a course is assigned (demo account has seeded courses)
2. Select session type: **Lecture**
3. Click **Start Session**
4. You land on `/lecturer/session/{id}` with a **rotating QR code**

**Say:** "This QR changes every 5 seconds. It's a signed, single-use token — a screenshot won't work for long."

### Step 4 — Student: sign in (phone, Browser B or incognito)

1. Go to https://ucc-attendance-system.onrender.com
2. Login → Role: **Student**
3. Email: `student@ucc.edu.gh`, Password: `student123`
4. Redirect to `/student`

### Step 5 — Student: mark attendance

**Option A — Camera (best for demo):**
1. Allow camera permission
2. Point at the lecturer's QR on screen
3. Confirm success toast/message

**Option B — Manual code (fallback if camera fails):**
1. Note the session code shown on lecturer screen
2. Enter code in manual scan field on student page

**Option C — Upload QR image:**
1. Screenshot the QR on lecturer screen
2. Upload via student scan page (must be fresh — within 5s)

**Say:** "The server checks: logged-in student, index number match, course enrollment, QR not expired, device not already used."

### Step 6 — Lecturer: show live roster update

1. Return to lecturer session page
2. Attendance table should show the student as **Present**
3. Present count increments

**Say:** "Real-time roster — no manual tick marks."

### Step 7 — Close session and export (optional)

1. Click **Close Session**
2. Download **CSV export** if available
3. Show attendance rate

---

## Demo Path B: Desktop Lecturer + Web Student

### Step 1 — Launch desktop

```bat
scripts\launch-attendiq-desktop.bat
```

Or run `AttendIQ-Desktop-*-Setup.exe` from GitHub Releases.

### Step 2 — Desktop login

- Identifier: `LEC001`
- Password: `lecturer123`
- Role: Lecturer

Desktop connects to production automatically (`api.properties`).

### Step 3 — Start session from dashboard

1. Select course and session type
2. Click **Start Session**
3. `session.fxml` opens with fullscreen QR option

### Step 4 — Student scans via web (phone)

Same as Demo Path A, Steps 4–5.

### Step 5 — Show desktop live table

Attendance rows update on the session screen.

---

## API Verification (Scripted / Pre-Demo Check)

Run from PowerShell (allow up to 90s timeout for cold start):

```powershell
$base = "https://ucc-attendance-system.onrender.com"

# 1. Public endpoint
Invoke-RestMethod -Uri "$base/departments" -TimeoutSec 90

# 2. Lecturer login
$body = '{"identifier":"LEC001","password":"lecturer123","role":"LECTURER"}'
$login = Invoke-RestMethod -Uri "$base/auth/login" -Method POST -Body $body -ContentType "application/json" -TimeoutSec 90
$headers = @{ Authorization = "Bearer $($login.token)" }

# 3. List courses and sessions
Invoke-RestMethod -Uri "$base/courses" -Headers $headers -TimeoutSec 60
Invoke-RestMethod -Uri "$base/sessions" -Headers $headers -TimeoutSec 60
```

**Last verified:** Production API responds after cold start. `/departments` may timeout on first request; retry after 60s.

---

## Troubleshooting During Demo

| Issue | Fix |
|-------|-----|
| Page won't load | Render cold start — wait 60s, refresh |
| Camera blocked | Use manual session code or QR upload |
| "Already marked" | Student already attended this session — use another student or new session |
| "Not enrolled" | Student must join the course first on `/student` |
| Desktop can't connect | Check internet; API URL is production in `api.properties` |
| QR expired | Normal — wait for refresh (5s) and scan again |

---

## What to Say During Each Beat

| Beat | Key phrase |
|------|------------|
| Problem | "Five minutes of roll call, five ways to cheat." |
| QR rotation | "Like a boarding pass that expires in five seconds." |
| Scan | "The server is the referee — not the lecturer's spreadsheet." |
| Roster | "Everyone sees the same truth in the database." |
| Close | "Export to CSV for departmental records." |

---

## Post-Demo Q&A Anchors

If asked during demo:
- **"Is this secure?"** → Walk through 7 layers in [DEFENSE_QA_FLASHCARDS.md](DEFENSE_QA_FLASHCARDS.md) Section C
- **"Why desktop?"** → Fullscreen projector UX
- **"Why not React?"** → One deploy, solo dev speed — see implementation timeline

---

## Related Docs

- [TEAM_BRIEFING_SCRIPT.md](TEAM_BRIEFING_SCRIPT.md) — 10-minute narrative
- [PRESENTATION_OUTLINE.md](PRESENTATION_OUTLINE.md) — Slide 16 is this demo
- [E2E-VALIDATION.md](E2E-VALIDATION.md) — Full acceptance checklist
