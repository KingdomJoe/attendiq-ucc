# Product Requirements Document (PRD)

## Smart Attendance System Application

---

# 1. Project Overview

### Product Name

**Smart Attendance System**

### Product Type

Web-Based Attendance Management Application

### Purpose

The Smart Attendance System is a secure web application designed to automate classroom attendance tracking using dynamic QR code technology, student verification, and lecturer-controlled session management.

The system minimizes manual attendance fraud, improves attendance accuracy, and enables real-time attendance monitoring for lecturers and institutions.

---

# 2. Problem Statement

Traditional attendance systems suffer from:

* Proxy attendance (“signing for friends”)
* Time-consuming manual roll calls
* Poor attendance records management
* Lack of real-time monitoring
* Difficulties verifying student identity

This system solves these problems through:

* Dynamic QR code attendance sessions
* Student identity verification
* Device-based validation
* Lecturer-controlled attendance generation
* Secure authentication systems

---

# 3. Objectives

### Primary Objectives

* Digitize classroom attendance
* Prevent attendance fraud
* Enable lecturers to manage attendance sessions
* Provide real-time attendance validation
* Maintain centralized attendance records

### Secondary Objectives

* Improve classroom efficiency
* Generate attendance analytics
* Support departmental and course-based access control

---

# 4. Target Users

## 4.1 Students

Students can:

* Register accounts
* Join courses
* Scan QR codes
* Verify attendance
* View attendance history

---

## 4.2 Lecturers

Lecturers can:

* Register/login
* Create attendance sessions
* Generate dynamic QR codes
* View attendance records
* Manage courses/classes

---

## 4.3 Administrators (Optional Future Expansion)

Admins can:

* Manage departments
* Approve lecturers
* Manage courses
* View system-wide analytics

---

# 5. Tech Stack

| Layer              | Technology                              |
| ------------------ | --------------------------------------- |
| Backend            | Java                                    |
| Framework          | Spring Boot                             |
| Frontend           | HTML/CSS/JavaScript (or React optional) |
| Database           | MySQL/PostgreSQL                        |
| Authentication     | Spring Security + JWT                   |
| QR Code Generation | ZXing Library                           |
| Hosting            | Render / Railway / AWS                  |
| Version Control    | Git + GitHub                            |

---

# 6. Core Functional Requirements

# 6.1 Authentication System

## Student Authentication

Students must register with:

* Full name
* Student email
* Index number
* Password

### Validation Rules

* Email must be institutional email
* Index number must be unique
* Password encryption required

---

## Lecturer Authentication

Lecturers register with:

* Full name
* Lecturer ID / Unique Lecturer Code
* Department
* Course code
* Password

### Validation Rules

* Lecturer code must be unique
* Lecturer tied to specific department/course

---

# 6.2 Course & Department Management

Each:

* Department has unique identifier
* Course has unique course code
* Lecturer assigned to specific courses

### Example

| Department       | Code |
| ---------------- | ---- |
| Computer Science | CSC  |

| Course           | Code   |
| ---------------- | ------ |
| Java Programming | CSC301 |

---

# 6.3 Attendance Session Creation

Only lecturers can create attendance sessions.

### Lecturer Workflow

1. Login
2. Select course
3. Create attendance session
4. Generate QR code
5. Students scan QR code

---

# 6.4 Dynamic QR Code System

### Requirements

* QR code refreshes every 5 seconds
* QR code expires automatically
* QR code tied to:

  * Lecturer
  * Course
  * Session
  * Timestamp

### QR Payload Example

```json
{
  "sessionId": "ATT-2026-001",
  "courseCode": "CSC301",
  "timestamp": "2026-05-23T10:30:00"
}
```

---

# 6.5 QR Code Attendance Workflow

## Student Attendance Flow

1. Student logs in
2. Opens scanner
3. Scans QR code
4. System validates:

   * QR validity
   * Student identity
   * Device metadata
   * Session status
5. Student enters/verifies index number
6. Attendance marked present
7. Confirmation displayed

---

# 6.6 Device Constraint System

The system should collect:

* Device ID
* Browser metadata
* IP address
* Device type

### Purpose

To reduce:

* Multiple attendance submissions
* Fraudulent scans
* Session abuse

### Rules

* One attendance submission per device per session
* Suspicious duplicate attempts flagged

---

# 6.7 Attendance Verification UI

### Lecturer Dashboard

Display:

* Student names
* Index numbers
* Attendance status

### UI Indicators

| Status  | Indicator        |
| ------- | ---------------- |
| Present | Green check icon |
| Absent  | Empty checkbox   |

---

# 6.8 Attendance Records

The system stores:

* Student details
* Attendance timestamps
* Device metadata
* Session details
* Lecturer details

---

# 7. Non-Functional Requirements

## Security

* Password hashing
* JWT authentication
* HTTPS deployment
* Session expiration
* CSRF protection

---

## Performance

* QR generation under 1 second
* Attendance validation under 2 seconds
* Support multiple concurrent scans

---

## Scalability

The system should support:

* Multiple departments
* Hundreds of students
* Multiple simultaneous classes

---

## Reliability

* Automatic session timeout
* Error handling
* Duplicate prevention

---

# 8. System Constraints

| Constraint            | Description                         |
| --------------------- | ----------------------------------- |
| Device ID             | Detect and validate device metadata |
| QR Expiration         | Maximum 5 seconds                   |
| Domain Access         | Web-only application                |
| Lecturer Authority    | Only lecturers generate QR          |
| Identity Verification | Index number required               |
| Course Restriction    | Unique department/course mapping    |

---

# 9. Database Design (High-Level)

## Student Table

| Field        |
| ------------ |
| id           |
| name         |
| email        |
| index_number |
| password     |
| department   |

---

## Lecturer Table

| Field         |
| ------------- |
| id            |
| name          |
| lecturer_code |
| department    |
| course_code   |
| password      |

---

## Course Table

| Field       |
| ----------- |
| id          |
| course_name |
| course_code |
| department  |

---

## Attendance Session Table

| Field       |
| ----------- |
| id          |
| lecturer_id |
| course_id   |
| qr_token    |
| created_at  |
| expires_at  |

---

## Attendance Table

| Field           |
| --------------- |
| id              |
| student_id      |
| session_id      |
| device_id       |
| attendance_time |
| status          |

---

# 10. User Flow

# Lecturer Flow

```text
Login
   ↓
Create Session
   ↓
Generate Dynamic QR
   ↓
Students Scan
   ↓
View Attendance List
```

---

# Student Flow

```text
Login
   ↓
Scan QR Code
   ↓
Verify Index Number
   ↓
Attendance Confirmed
```

---

# 11. API Modules (Suggested)

| Module         | Purpose                     |
| -------------- | --------------------------- |
| Auth API       | Login/Register              |
| QR API         | Generate/Validate QR        |
| Attendance API | Mark attendance             |
| Course API     | Manage courses              |
| User API       | Student/Lecturer management |

---

# 12. Suggested Architecture

```text
Frontend (Web App)
        ↓
REST API (Spring Boot)
        ↓
Authentication Layer
        ↓
Business Logic Layer
        ↓
Database (MySQL/PostgreSQL)
```

---

# 13. Risks & Challenges

| Risk                | Mitigation           |
| ------------------- | -------------------- |
| QR sharing          | 5-second expiration  |
| Proxy attendance    | Device validation    |
| Duplicate scans     | One scan per session |
| Network issues      | Retry mechanisms     |
| Unauthorized access | JWT authentication   |

---

# 14. Future Improvements

## Phase 2 Features

* Facial recognition verification
* GPS classroom geofencing
* Attendance analytics dashboard
* Email notifications
* Export attendance to Excel/PDF
* Mobile app version
* Admin management portal

---

# 15. MVP Scope

## Included in MVP

- Student registration/login
- Lecturer registration/login
- Course creation
- Dynamic QR generation
- QR scanning attendance
- Attendance dashboard
- Device metadata validation

---

## Excluded from MVP

- Facial recognition
- GPS tracking
- Mobile app
- AI fraud detection
- Advanced analytics

---

# 16. Success Metrics

| Metric                          | Goal         |
| ------------------------------- | ------------ |
| Attendance marking speed        | < 10 seconds |
| QR validation success rate      | > 95%        |
| Duplicate attendance prevention | 100%         |
| System uptime                   | > 99%        |

---

# 17. Recommended Development Structure

## Backend Team

Focus on:

* Spring Boot APIs
* Authentication
* QR logic
* Database models

---

## Frontend Team

Focus on:

* Authentication pages
* QR scanner UI
* Lecturer dashboard
* Attendance visualization

---

## QA/Testing

Test:

* QR expiration
* Duplicate attendance prevention
* Authentication security
* Session handling

---

# 18. Critical Technical Recommendations

## 1. Use JWT Authentication

This is cleaner and scalable for web applications.

---

## 2. Avoid Permanent QR Codes

Permanent QR codes defeat the anti-proxy objective.

---

## 3. Device ID Limitations

Browsers cannot access true hardware IDs directly for privacy reasons.

Instead use:

* Browser fingerprinting
* Cookies
* User agent metadata
* IP logging

---

## 4. Add Session Validation

Attendance should only work during active class sessions.

---

# 19. Final Product Vision

The Smart Attendance System aims to become a secure, scalable, and efficient academic attendance platform capable of supporting universities and institutions through automated verification, fraud prevention, and real-time attendance tracking.

---

# Appendix: Implementation Notes

This appendix records engineering decisions for the MVP build (not part of the original PRD).

## Institutional email

Student registration validates email against a configurable pattern (default: `@ucc.edu.gh`). Update `app.student-email-pattern` in `backend/src/main/resources/application.yml` when the official UCC domain is confirmed.

## Device identification

Browsers do not expose hardware device IDs. The MVP uses **FingerprintJS** on the client plus server-side storage of IP address and User-Agent. A unique constraint on `(session_id, device_fingerprint)` blocks duplicate marks from the same device.

## QR security

QR codes encode a **short-lived signed token** (HMAC/JWT), not raw JSON. Tokens expire after 5 seconds and are tied to `sessionId`, `courseCode`, and a single-use `nonce`. Lecturer UI polls for a new QR image every 5 seconds.

## Stack (MVP)

| Layer    | Technology                          |
| -------- | ----------------------------------- |
| Backend  | Java 17, Spring Boot 3, Spring Security, JWT |
| Database | PostgreSQL 16, Flyway               |
| Frontend | React 18, TypeScript, Vite          |
| QR       | ZXing (server), html5-qrcode (client) |
