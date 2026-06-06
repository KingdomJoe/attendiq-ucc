package com.ucc.attendance.repository;

import com.ucc.attendance.domain.LecturerCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LecturerCodeRepository extends JpaRepository<LecturerCode, Long> {
    Optional<LecturerCode> findByCodeIgnoreCase(String code);
}
