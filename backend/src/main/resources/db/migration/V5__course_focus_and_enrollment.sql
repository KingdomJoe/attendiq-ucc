-- Per-lecturer course focus (single active course drives dashboard metrics)
ALTER TABLE lecturer_courses ADD COLUMN active BOOLEAN NOT NULL DEFAULT FALSE;

WITH first_course AS (
    SELECT DISTINCT ON (lecturer_id) lecturer_id, course_id
    FROM lecturer_courses
    ORDER BY lecturer_id, course_id
)
UPDATE lecturer_courses lc
SET active = TRUE
FROM first_course fc
WHERE lc.lecturer_id = fc.lecturer_id AND lc.course_id = fc.course_id;

-- Unique enrollment link per course
ALTER TABLE courses ADD COLUMN enrollment_token VARCHAR(64);

UPDATE courses
SET enrollment_token = md5(random()::text || id::text || clock_timestamp()::text)
WHERE enrollment_token IS NULL;

ALTER TABLE courses ALTER COLUMN enrollment_token SET NOT NULL;
CREATE UNIQUE INDEX idx_courses_enrollment_token ON courses (enrollment_token);
