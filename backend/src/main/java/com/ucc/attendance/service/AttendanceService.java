package com.ucc.attendance.service;

import com.ucc.attendance.domain.*;
import com.ucc.attendance.dto.AttendanceDtos;
import com.ucc.attendance.exception.ApiException;
import com.ucc.attendance.repository.AttendanceRecordRepository;
import com.ucc.attendance.repository.StudentRepository;
import com.ucc.attendance.security.UserPrincipal;
import com.ucc.attendance.util.DeviceFingerprintUtil;
import com.ucc.attendance.util.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@SuppressWarnings("null")
public class AttendanceService {

    private final SessionService sessionService;
    private final StudentRepository studentRepository;
    private final AttendanceRecordRepository attendanceRecordRepository;

    @Transactional
    public AttendanceDtos.ScanResponse scan(AttendanceDtos.ScanRequest req, HttpServletRequest httpRequest) {
        UserPrincipal studentPrincipal = SecurityUtils.requireRole(UserRole.STUDENT);
        Student student = studentRepository.findById(studentPrincipal.getUserId())
                .orElseThrow(() -> new ApiException("NOT_FOUND", "Student not found", HttpStatus.NOT_FOUND));

        if (!student.getIndexNumber().equalsIgnoreCase(req.indexNumber().trim())) {
            throw new ApiException("INVALID_INDEX", "Index number does not match your account", HttpStatus.BAD_REQUEST);
        }

        AttendanceSession session = sessionService.resolveQrToken(req.token());

        boolean enrolled = student.getCourses().stream()
                .anyMatch(c -> c.getId().equals(session.getCourse().getId()));
        if (!enrolled) {
            throw new ApiException("NOT_ENROLLED", "You are not enrolled in this course", HttpStatus.FORBIDDEN);
        }

        if (attendanceRecordRepository.existsBySessionIdAndStudentId(session.getId(), student.getId())) {
            throw new ApiException("ALREADY_MARKED", "You are already marked present for this session", HttpStatus.CONFLICT);
        }

        String fingerprint = DeviceFingerprintUtil.resolve(
                req.clientDeviceId(), req.deviceFingerprint(), httpRequest);

        if (attendanceRecordRepository.existsBySessionIdAndDeviceFingerprint(session.getId(), fingerprint)) {
            throw new ApiException("DEVICE_ALREADY_USED", "This device has already been used to mark attendance in this session", HttpStatus.CONFLICT);
        }

        AttendanceRecord record = AttendanceRecord.builder()
                .session(session)
                .student(student)
                .deviceFingerprint(fingerprint)
                .ipAddress(httpRequest.getRemoteAddr())
                .userAgent(httpRequest.getHeader("User-Agent"))
                .status(AttendanceStatus.PRESENT)
                .build();

        try {
            attendanceRecordRepository.save(record);
        } catch (DataIntegrityViolationException e) {
            if (attendanceRecordRepository.existsBySessionIdAndStudentId(session.getId(), student.getId())) {
                throw new ApiException("ALREADY_MARKED", "You are already marked present for this session", HttpStatus.CONFLICT);
            }
            throw new ApiException("DEVICE_ALREADY_USED", "This device has already been used to mark attendance in this session", HttpStatus.CONFLICT);
        }

        sessionService.consumeQrToken(req.token());

        return new AttendanceDtos.ScanResponse(
                "Attendance confirmed",
                session.getCourse().getCourseCode(),
                record.getAttendanceTime()
        );
    }

    public List<AttendanceDtos.HistoryItem> history() {
        UserPrincipal student = SecurityUtils.requireRole(UserRole.STUDENT);
        return attendanceRecordRepository.findByStudentIdOrderByAttendanceTimeDesc(student.getUserId()).stream()
                .map(ar -> new AttendanceDtos.HistoryItem(
                        ar.getSession().getId(),
                        ar.getSession().getCourse().getCourseCode(),
                        ar.getSession().getCourse().getCourseName(),
                        ar.getAttendanceTime(),
                        "PRESENT"
                ))
                .toList();
    }
}
