package com.ucc.attendance.repository;

import com.ucc.attendance.domain.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface StudentRepository extends JpaRepository<Student, Long> {
    Optional<Student> findByEmailIgnoreCase(String email);
    Optional<Student> findByIndexNumberIgnoreCase(String indexNumber);
    boolean existsByEmailIgnoreCase(String email);
    boolean existsByIndexNumberIgnoreCase(String indexNumber);

    @Query("SELECT s FROM Student s JOIN s.courses c WHERE c.id = :courseId")
    List<Student> findByCourseId(Long courseId);
}
