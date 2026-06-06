-- ============================================================================
-- V4: Lecturer registration codes table, performance indexes, and seed data
-- ============================================================================

-- 1. Create lecturer_codes table
CREATE TABLE lecturer_codes (
    id            BIGSERIAL PRIMARY KEY,
    code          VARCHAR(20)  NOT NULL UNIQUE,
    department_id BIGINT       NOT NULL REFERENCES departments(id),
    claimed       BOOLEAN      NOT NULL DEFAULT FALSE,
    claimed_by    BIGINT       REFERENCES lecturers(id),
    claimed_at    TIMESTAMPTZ
);

CREATE INDEX idx_lecturer_codes_code       ON lecturer_codes(code);
CREATE INDEX idx_lecturer_codes_department ON lecturer_codes(department_id);

-- 2. Performance indexes on existing tables
CREATE INDEX IF NOT EXISTS idx_attendance_sessions_course  ON attendance_sessions(course_id);
CREATE INDEX IF NOT EXISTS idx_attendance_records_student  ON attendance_records(student_id);
CREATE INDEX IF NOT EXISTS idx_students_department         ON students(department_id);
CREATE INDEX IF NOT EXISTS idx_lecturers_department        ON lecturers(department_id);

-- 3. Seed departments (CSC already exists from V2)
INSERT INTO departments (code, name) VALUES
    ('MAT', 'Mathematics'),
    ('STA', 'Statistics'),
    ('PHY', 'Physics'),
    ('CHE', 'Chemistry'),
    ('BIO', 'Biology'),
    ('ECO', 'Economics'),
    ('BUS', 'Business Administration'),
    ('EDU', 'Education'),
    ('ENG', 'English');

-- 4. Seed courses (CSC301 already exists from V2)
-- We need department IDs. Use subqueries to resolve them safely.
INSERT INTO courses (course_code, course_name, department_id) VALUES
    -- Computer Science (CSC101, CSC201 are new; CSC301 already exists)
    ('CSC101', 'Introduction to Computing',    (SELECT id FROM departments WHERE code = 'CSC')),
    ('CSC201', 'Data Structures & Algorithms', (SELECT id FROM departments WHERE code = 'CSC')),
    -- Mathematics
    ('MAT101', 'Calculus I',                   (SELECT id FROM departments WHERE code = 'MAT')),
    ('MAT201', 'Linear Algebra',               (SELECT id FROM departments WHERE code = 'MAT')),
    ('MAT301', 'Numerical Methods',            (SELECT id FROM departments WHERE code = 'MAT')),
    -- Statistics
    ('STA101', 'Introduction to Statistics',   (SELECT id FROM departments WHERE code = 'STA')),
    ('STA201', 'Probability Theory',           (SELECT id FROM departments WHERE code = 'STA')),
    -- Physics
    ('PHY101', 'Mechanics',                    (SELECT id FROM departments WHERE code = 'PHY')),
    ('PHY201', 'Electromagnetism',             (SELECT id FROM departments WHERE code = 'PHY')),
    ('PHY301', 'Quantum Physics',              (SELECT id FROM departments WHERE code = 'PHY')),
    -- Chemistry
    ('CHE101', 'General Chemistry',            (SELECT id FROM departments WHERE code = 'CHE')),
    ('CHE201', 'Organic Chemistry',            (SELECT id FROM departments WHERE code = 'CHE')),
    -- Biology
    ('BIO101', 'Cell Biology',                 (SELECT id FROM departments WHERE code = 'BIO')),
    ('BIO201', 'Genetics',                     (SELECT id FROM departments WHERE code = 'BIO')),
    ('BIO301', 'Ecology',                      (SELECT id FROM departments WHERE code = 'BIO')),
    -- Economics
    ('ECO101', 'Principles of Economics',      (SELECT id FROM departments WHERE code = 'ECO')),
    ('ECO201', 'Microeconomics',               (SELECT id FROM departments WHERE code = 'ECO')),
    ('ECO301', 'Macroeconomics',               (SELECT id FROM departments WHERE code = 'ECO')),
    -- Business Administration
    ('BUS101', 'Introduction to Business',     (SELECT id FROM departments WHERE code = 'BUS')),
    ('BUS201', 'Financial Accounting',         (SELECT id FROM departments WHERE code = 'BUS')),
    -- Education
    ('EDU101', 'Foundations of Education',      (SELECT id FROM departments WHERE code = 'EDU')),
    ('EDU201', 'Curriculum Development',        (SELECT id FROM departments WHERE code = 'EDU')),
    -- English
    ('ENG101', 'English Composition',           (SELECT id FROM departments WHERE code = 'ENG')),
    ('ENG201', 'English Literature',            (SELECT id FROM departments WHERE code = 'ENG'));

-- 5. Seed 30 pre-generated lecturer registration codes (3 per department)
INSERT INTO lecturer_codes (code, department_id) VALUES
    -- Computer Science
    ('UCC-LEC-A7X9', (SELECT id FROM departments WHERE code = 'CSC')),
    ('UCC-LEC-B3M2', (SELECT id FROM departments WHERE code = 'CSC')),
    ('UCC-LEC-C8K4', (SELECT id FROM departments WHERE code = 'CSC')),
    -- Mathematics
    ('UCC-LEC-D2N7', (SELECT id FROM departments WHERE code = 'MAT')),
    ('UCC-LEC-E5P1', (SELECT id FROM departments WHERE code = 'MAT')),
    ('UCC-LEC-F9R3', (SELECT id FROM departments WHERE code = 'MAT')),
    -- Statistics
    ('UCC-LEC-G4T6', (SELECT id FROM departments WHERE code = 'STA')),
    ('UCC-LEC-H1V8', (SELECT id FROM departments WHERE code = 'STA')),
    ('UCC-LEC-J6W2', (SELECT id FROM departments WHERE code = 'STA')),
    -- Physics
    ('UCC-LEC-K3Y5', (SELECT id FROM departments WHERE code = 'PHY')),
    ('UCC-LEC-L8Z1', (SELECT id FROM departments WHERE code = 'PHY')),
    ('UCC-LEC-M2A4', (SELECT id FROM departments WHERE code = 'PHY')),
    -- Chemistry
    ('UCC-LEC-N7B9', (SELECT id FROM departments WHERE code = 'CHE')),
    ('UCC-LEC-P1C6', (SELECT id FROM departments WHERE code = 'CHE')),
    ('UCC-LEC-Q5D3', (SELECT id FROM departments WHERE code = 'CHE')),
    -- Biology
    ('UCC-LEC-R9E8', (SELECT id FROM departments WHERE code = 'BIO')),
    ('UCC-LEC-S4F2', (SELECT id FROM departments WHERE code = 'BIO')),
    ('UCC-LEC-T8G5', (SELECT id FROM departments WHERE code = 'BIO')),
    -- Economics
    ('UCC-LEC-U3H7', (SELECT id FROM departments WHERE code = 'ECO')),
    ('UCC-LEC-V7J1', (SELECT id FROM departments WHERE code = 'ECO')),
    ('UCC-LEC-W2K4', (SELECT id FROM departments WHERE code = 'ECO')),
    -- Business Administration
    ('UCC-LEC-X6L9', (SELECT id FROM departments WHERE code = 'BUS')),
    ('UCC-LEC-Y1M3', (SELECT id FROM departments WHERE code = 'BUS')),
    ('UCC-LEC-Z5N6', (SELECT id FROM departments WHERE code = 'BUS')),
    -- Education
    ('UCC-LEC-A2P8', (SELECT id FROM departments WHERE code = 'EDU')),
    ('UCC-LEC-B6Q1', (SELECT id FROM departments WHERE code = 'EDU')),
    ('UCC-LEC-C9R4', (SELECT id FROM departments WHERE code = 'EDU')),
    -- English
    ('UCC-LEC-D4S7', (SELECT id FROM departments WHERE code = 'ENG')),
    ('UCC-LEC-E8T2', (SELECT id FROM departments WHERE code = 'ENG')),
    ('UCC-LEC-F3U5', (SELECT id FROM departments WHERE code = 'ENG'));
