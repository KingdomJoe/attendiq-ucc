CREATE TABLE departments (
    id          BIGSERIAL PRIMARY KEY,
    code        VARCHAR(20) NOT NULL UNIQUE,
    name        VARCHAR(255) NOT NULL
);

CREATE TABLE courses (
    id            BIGSERIAL PRIMARY KEY,
    course_code   VARCHAR(50) NOT NULL UNIQUE,
    course_name   VARCHAR(255) NOT NULL,
    department_id BIGINT NOT NULL REFERENCES departments (id)
);

CREATE TABLE students (
    id             BIGSERIAL PRIMARY KEY,
    name           VARCHAR(255) NOT NULL,
    email          VARCHAR(255) NOT NULL UNIQUE,
    index_number   VARCHAR(50) NOT NULL UNIQUE,
    password_hash  VARCHAR(255) NOT NULL,
    department_id  BIGINT NOT NULL REFERENCES departments (id),
    created_at     TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE lecturers (
    id             BIGSERIAL PRIMARY KEY,
    name           VARCHAR(255) NOT NULL,
    lecturer_code  VARCHAR(50) NOT NULL UNIQUE,
    password_hash  VARCHAR(255) NOT NULL,
    department_id  BIGINT NOT NULL REFERENCES departments (id),
    created_at     TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE lecturer_courses (
    lecturer_id BIGINT NOT NULL REFERENCES lecturers (id) ON DELETE CASCADE,
    course_id   BIGINT NOT NULL REFERENCES courses (id) ON DELETE CASCADE,
    PRIMARY KEY (lecturer_id, course_id)
);

CREATE TABLE student_courses (
    student_id BIGINT NOT NULL REFERENCES students (id) ON DELETE CASCADE,
    course_id  BIGINT NOT NULL REFERENCES courses (id) ON DELETE CASCADE,
    PRIMARY KEY (student_id, course_id)
);

CREATE TABLE attendance_sessions (
    id          BIGSERIAL PRIMARY KEY,
    lecturer_id BIGINT NOT NULL REFERENCES lecturers (id),
    course_id   BIGINT NOT NULL REFERENCES courses (id),
    status      VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    closed_at   TIMESTAMPTZ
);

CREATE TABLE qr_tokens (
    id           BIGSERIAL PRIMARY KEY,
    session_id   BIGINT NOT NULL REFERENCES attendance_sessions (id) ON DELETE CASCADE,
    token_hash   VARCHAR(64) NOT NULL,
    nonce        VARCHAR(64) NOT NULL,
    issued_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    expires_at   TIMESTAMPTZ NOT NULL,
    consumed     BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_qr_tokens_session ON qr_tokens (session_id);
CREATE INDEX idx_qr_tokens_nonce ON qr_tokens (nonce);

CREATE TABLE attendance_records (
    id                 BIGSERIAL PRIMARY KEY,
    session_id         BIGINT NOT NULL REFERENCES attendance_sessions (id) ON DELETE CASCADE,
    student_id         BIGINT NOT NULL REFERENCES students (id),
    device_fingerprint VARCHAR(128) NOT NULL,
    ip_address         VARCHAR(45),
    user_agent         TEXT,
    attendance_time    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    status             VARCHAR(20) NOT NULL DEFAULT 'PRESENT',
    CONSTRAINT uq_attendance_session_student UNIQUE (session_id, student_id),
    CONSTRAINT uq_attendance_session_device UNIQUE (session_id, device_fingerprint)
);

CREATE INDEX idx_attendance_session ON attendance_records (session_id);
