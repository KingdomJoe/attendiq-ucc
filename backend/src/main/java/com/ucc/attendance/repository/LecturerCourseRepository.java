package com.ucc.attendance.repository;

import com.ucc.attendance.domain.LecturerCourse;
import com.ucc.attendance.domain.LecturerCourseId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface LecturerCourseRepository extends JpaRepository<LecturerCourse, LecturerCourseId> {

    @Query("SELECT lc FROM LecturerCourse lc JOIN FETCH lc.course c JOIN FETCH c.department WHERE lc.lecturer.id = :lecturerId ORDER BY c.courseCode")
    List<LecturerCourse> findByLecturerIdWithCourse(Long lecturerId);

    Optional<LecturerCourse> findByLecturerIdAndCourseId(Long lecturerId, Long courseId);

    @Query("SELECT lc FROM LecturerCourse lc JOIN FETCH lc.course WHERE lc.lecturer.id = :lecturerId AND lc.active = true")
    Optional<LecturerCourse> findActiveByLecturerId(Long lecturerId);
}
