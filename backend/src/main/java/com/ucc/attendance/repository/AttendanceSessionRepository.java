package com.ucc.attendance.repository;

import com.ucc.attendance.domain.AttendanceSession;
import com.ucc.attendance.domain.SessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AttendanceSessionRepository extends JpaRepository<AttendanceSession, Long> {
    List<AttendanceSession> findByLecturerIdOrderByCreatedAtDesc(Long lecturerId);
    List<AttendanceSession> findByLecturerIdAndStatusOrderByCreatedAtDesc(Long lecturerId, SessionStatus status);
}
