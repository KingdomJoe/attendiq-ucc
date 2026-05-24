package com.ucc.attendance.repository;

import com.ucc.attendance.domain.Lecturer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LecturerRepository extends JpaRepository<Lecturer, Long> {
    Optional<Lecturer> findByLecturerCodeIgnoreCase(String lecturerCode);
    boolean existsByLecturerCodeIgnoreCase(String lecturerCode);
}
