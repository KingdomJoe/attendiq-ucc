ALTER TABLE attendance_sessions
    ADD COLUMN session_type VARCHAR(20) NOT NULL DEFAULT 'LECTURE';
