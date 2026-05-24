package com.ucc.attendance.repository;

import com.ucc.attendance.domain.AttendanceRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface AttendanceRecordRepository extends JpaRepository<AttendanceRecord, Long> {

    List<AttendanceRecord> findBySessionId(Long sessionId);

    @Query("SELECT ar FROM AttendanceRecord ar JOIN FETCH ar.session s JOIN FETCH s.course WHERE ar.student.id = :studentId ORDER BY ar.attendanceTime DESC")
    List<AttendanceRecord> findByStudentIdOrderByAttendanceTimeDesc(Long studentId);

    @Query("SELECT COUNT(ar) FROM AttendanceRecord ar WHERE ar.session.id = :sessionId")
    long countBySessionId(Long sessionId);

    boolean existsBySessionIdAndStudentId(Long sessionId, Long studentId);

    boolean existsBySessionIdAndDeviceFingerprint(Long sessionId, String deviceFingerprint);
}
