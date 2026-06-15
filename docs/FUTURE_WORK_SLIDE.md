# AttendIQ — Future Work (Closing Slide)

Use this as your final presentation slide before Q&A. Three pillars recommended by the project plan.

---

## Slide Title

**What's Next for AttendIQ**

---

## Pillar 1: Admin Dashboard

**Problem today:** Only students and lecturers have accounts. Department heads cannot see system-wide metrics without individual lecturer exports.

**Proposed solution:**
- New `ADMIN` role with department-scoped access
- Dashboard: enrollment counts, session activity, attendance rates per course/department
- Lecturer code management UI (replace manual DB seeding)
- Aligns with PRD §4.3

**Why mention it:** Shows you understand institutional deployment beyond classroom beta.

**Talking point:** *"We built for the lecture hall first; the admin layer is how this scales to a faculty-wide rollout."*

---

## Pillar 2: Campus Geofencing

**Problem today:** Device fingerprint is client-supplied and can be spoofed by a determined attacker. A student could theoretically scan from outside campus if they receive a live QR quickly enough.

**Proposed solution:**
- **Wi-Fi SSID check** — attendance only valid on university network
- **GPS geofence** — optional mobile browser geolocation API with campus polygon
- Server-side validation in `AttendanceService.scan()` before recording presence

**Why mention it:** Direct answer to the #1 tricky viva question about fingerprint spoofing.

**Talking point:** *"QR proves timing; geofencing proves place. Together they close the proxy-attendance gap."*

---

## Pillar 3: AI-Powered Attendance Risk Alerts

**Problem today:** Lecturers see raw stats but must manually identify students at risk of falling below attendance thresholds.

**Proposed solution (phased):**

| Phase | Approach | Data source |
|-------|----------|-------------|
| 1 — ML baseline | Logistic regression on attendance rate trends | `attendance_records` per student/course |
| 2 — Lecturer NL query | LLM with tool-calling over aggregated stats (no raw PII to external APIs) | `/api/lecturer/analytics`, CSV exports |
| 3 — Anomaly detection | Flag shared fingerprints across accounts, burst scans from one IP | `attendance_records` metadata |

**Example outputs:**
- *"12 students in CSC201 are below 75% attendance — intervention recommended."*
- *"Unusual pattern: 3 accounts scanned from same device fingerprint this week."*

**Ethics guardrail:** On-prem or university-approved LLM; student consent for any biometric or external AI processing.

**Talking point:** *"We don't need AI to take attendance — we need it to act on attendance data before students fail out silently."*

---

## Optional Bullet (If Time Permits)

- **SSO / university IdP** — SAML/OIDC instead of local passwords
- **Redis QR cache** — scale nonce expiry at high concurrency
- **PWA re-enable** — offline student history viewing
- **Integration tests** — `@SpringBootTest` for concurrent scan races

---

## One-Slide Visual Layout (Copy to PowerPoint)

```
┌─────────────────────────────────────────────────────────────┐
│                    What's Next for AttendIQ                  │
├─────────────────┬─────────────────┬─────────────────────────┤
│  ADMIN          │  GEOLOCATION    │  AI / ANALYTICS         │
│  Dashboard      │  Campus fence   │  Risk alerts            │
│                 │                 │                         │
│  Dept oversight │  Wi-Fi + GPS    │  ML + NL queries        │
│  Code mgmt      │  Fraud layer    │  Early intervention     │
└─────────────────┴─────────────────┴─────────────────────────┘
         Beta today  →  Institution-ready tomorrow
```

---

## 30-Second Closing Script

*"AttendIQ works today for live classroom attendance — rotating QR, real-time roster, hybrid web and desktop. To move from beta to institution-scale, we need three things: an admin layer for departments, geofencing so presence means on-campus, and intelligent analytics so lecturers catch at-risk students before it's too late. The foundation — API, database, fraud constraints — is already built for all three."*

---

## Architecture Evolution (Backup Slide)

```
Today:  [ Spring Boot Monolith ] → [ PostgreSQL ]

Future: [ API Gateway ]
           ├── Auth Service (SSO)
           ├── Attendance Service → Redis (QR cache)
           ├── Notification Service (email alerts)
           └── ML Worker (risk scoring)
                    └── PostgreSQL
```

---

## Related Docs

- [EXECUTIVE_SUMMARY.md](EXECUTIVE_SUMMARY.md) — Section 8
- [DEFENSE_QA_FLASHCARDS.md](DEFENSE_QA_FLASHCARDS.md) — Section G (AI questions)
- [PROJECT_MASTERY_GUIDE.md](PROJECT_MASTERY_GUIDE.md) — Full index
