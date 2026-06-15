# AttendIQ — Executive Summary

**Document type:** Project report appendix (≈2 pages)  
**Product:** AttendIQ — Smart Attendance System for UCC  
**Author:** Joseph Fiah (sole developer)  
**Status:** Beta — production deployed at https://ucc-attendance-system.onrender.com

---

## 1. Overview

AttendIQ is a fraud-resistant classroom attendance management system designed for university environments. It replaces manual roll calls with lecturer-controlled sessions that display **rotating QR codes** (5-second TTL, single-use). Students mark attendance by scanning with a mobile browser; the server validates identity, enrollment, and device uniqueness in real time.

The system uses a **hybrid architecture**: a Spring Boot web application (students and lecturers) plus a JavaFX desktop client optimized for lecturers presenting QR codes on classroom projectors. Both clients share a single REST API and PostgreSQL database deployed on Render.

---

## 2. Problem Statement

Traditional attendance methods suffer from proxy signing, time waste, inaccurate records, and lack of real-time monitoring. AttendIQ addresses these through dynamic QR sessions, student verification (index number match), device fingerprinting, and centralized digital records with analytics and CSV export.

---

## 3. Solution Summary

| Capability | Description |
|------------|-------------|
| Session management | Lecturers start/close sessions per course (Lecture, Practical, Tutorial) |
| Dynamic QR | ZXing-generated codes with JWT + nonce; expires in 5 seconds |
| Student marking | Web camera scan, QR image upload, or manual code entry |
| Anti-fraud | Auth + enrollment + index match + single-use QR + per-device limit |
| Course management | Assignment, enrollment links, focus course, roster views |
| Analytics | Lecturer stats, per-course metrics, attendance heatmaps |
| Export | CSV download per closed session |
| Desktop presenter | Fullscreen QR, live roster, Windows installer via jpackage |

**Roles implemented:** Student, Lecturer. Admin dashboard is planned future work.

---

## 4. Technical Architecture

**Stack:** Java 17, Spring Boot 3.3.5, Spring Security 6, JWT, Thymeleaf, JavaFX 21, PostgreSQL 16, Flyway, ZXing, Docker, Render, GitHub Actions.

**Structure:** Maven multi-module monorepo (`backend` + `desktop`).

**Authentication:**
- Web: JWT in HttpOnly cookie
- Desktop: JWT in memory, `Authorization: Bearer` header
- `JwtAuthenticationFilter` supports both paths; QR tokens are excluded from auth (scan-only)

**Database:** 10+ tables with Flyway migrations V1–V5. Unique constraints on `(session_id, student_id)` and `(session_id, device_fingerprint)` enforce attendance integrity.

---

## 5. Development History

The project evolved through nine major commits:

1. **Foundation** — Spring Boot API, React SPA, PostgreSQL, anti-fraud schema  
2. **Web consolidation** — Removed React (~3,500 lines); adopted Thymeleaf for single-artifact deployment  
3. **UX audit** — Course detail pages, session summaries, camera fixes  
4. **Institutional model** — Lecturer registration codes, seeded UCC departments/courses  
5. **Hybrid desktop** — Full JavaFX module, auth hardening, CI pipeline  
6. **Course depth** — Enrollment tokens, focus course, stale session reconciliation  
7. **Production** — Render deployment, Windows EXE installer  

Key architectural decision: **prioritize operational simplicity** (one deployable JAR, one database) over SPA complexity for a solo-developer beta.

---

## 6. Challenges and Resolutions

| Challenge | Resolution |
|-----------|------------|
| Separate React deploy + CORS | Unified Thymeleaf in backend |
| Web/desktop auth mismatch | Dual-path JWT filter; custom logout |
| QR camera browser issues | Upload + manual code fallbacks |
| Unauthorized lecturer signup | Pre-seeded `lecturer_codes` (one-time claim) |
| Screenshot QR sharing | 5s TTL + consumed nonce |
| Multiple open sessions | Auto-close on new session + reconciler job |

---

## 7. Testing and Validation

- Demo accounts: Lecturer `LEC001`, Student `student@ucc.edu.gh`
- E2E checklist: `docs/E2E-VALIDATION.md`
- API verification: departments, auth, sessions, courses endpoints
- Beta distribution: GitHub Releases (`AttendIQ-Desktop-*-Setup.exe`)

**Limitation:** Render free tier introduces cold-start latency; suitable for pilot, not enterprise SLA.

---

## 8. Future Work

**Near-term:**
- Admin dashboard for department-wide oversight
- Campus geofencing (Wi-Fi/GPS) as additional fraud layer
- CSRF hardening on web forms; integration tests for scan races

**AI / analytics:**
- Attendance risk prediction (ML on historical records)
- Natural-language analytics for lecturers (LLM with PII guardrails)
- Anomaly detection (shared fingerprints, burst scans)

**Architecture evolution:** Redis QR cache, SSO with university IdP, optional service decomposition at scale.

---

## 9. Conclusion

AttendIQ delivers a working beta of a secure, real-time attendance system tailored for UCC. The hybrid web + desktop model balances student accessibility (phone browser) with lecturer classroom needs (projected QR). The implementation demonstrates full-stack Java development, database design with integrity constraints, JWT security patterns, and cloud deployment — with a clear roadmap for institutional hardening and intelligent analytics.

---

**References:** `README.md`, `APP-PRD.md`, `docs/PROJECT_MASTERY_GUIDE.md`, `docs/architecture_reference.md`
