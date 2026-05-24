package com.ucc.attendance.repository;

import com.ucc.attendance.domain.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface CourseRepository extends JpaRepository<Course, Long> {

    Optional<Course> findByCourseCodeIgnoreCase(String courseCode);

    @Query("SELECT c FROM Course c JOIN c.department d ORDER BY c.courseCode")
    List<Course> findAllOrdered();

    @Query("SELECT c FROM Lecturer l JOIN l.courses c WHERE l.id = :lecturerId")
    List<Course> findByLecturerId(Long lecturerId);

    @Query("SELECT c FROM Student s JOIN s.courses c WHERE s.id = :studentId")
    List<Course> findByStudentId(Long studentId);
}
