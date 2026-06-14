package com.ucc.attendance.service;

import com.ucc.attendance.config.AppProperties;
import com.ucc.attendance.domain.*;
import com.ucc.attendance.dto.SessionDtos;
import com.ucc.attendance.exception.ApiException;
import com.ucc.attendance.repository.*;
import com.ucc.attendance.security.UserPrincipal;
import com.ucc.attendance.util.SecurityUtils;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@SuppressWarnings("null")
public class SessionService {

    private final AttendanceSessionRepository sessionRepository;
    private final CourseRepository courseRepository;
    private final LecturerRepository lecturerRepository;
    private final StudentRepository studentRepository;
    private final AttendanceRecordRepository attendanceRecordRepository;
    private final QrTokenRepository qrTokenRepository;
    private final com.ucc.attendance.security.JwtService jwtService;
    private final QrImageService qrImageService;
    private final AppProperties appProperties;

    @Transactional
    public SessionDtos.SessionResponse createSession(SessionDtos.CreateSessionRequest req) {
        UserPrincipal lecturer = SecurityUtils.requireRole(UserRole.LECTURER);
        Lecturer l = lecturerRepository.findById(lecturer.getUserId())
                .orElseThrow(() -> new ApiException("NOT_FOUND", "Lecturer not found", HttpStatus.NOT_FOUND));
        Course course = courseRepository.findById(req.courseId())
                .orElseThrow(() -> new ApiException("NOT_FOUND", "Course not found", HttpStatus.NOT_FOUND));

        boolean assigned = l.getCourses().stream().anyMatch(c -> c.getId().equals(course.getId()));
        if (!assigned && !l.getCourses().isEmpty()) {
            throw new ApiException("FORBIDDEN", "Course not assigned to lecturer", HttpStatus.FORBIDDEN);
        }
        if (l.getCourses().isEmpty()) {
            l.linkCourse(course, true);
            lecturerRepository.save(l);
        }

        closeAllActiveSessions(lecturer.getUserId());

        SessionType sessionType = req.sessionType() != null ? req.sessionType() : SessionType.LECTURE;
        AttendanceSession session = AttendanceSession.builder()
                .lecturer(l)
                .course(course)
                .status(SessionStatus.ACTIVE)
                .sessionType(sessionType)
                .build();
        session = sessionRepository.save(session);
        return toSessionResponse(session);
    }

    public SessionDtos.SessionResponse getSession(Long id) {
        AttendanceSession session = getSessionForLecturer(id);
        return toSessionResponse(session);
    }

    @Transactional
    public List<SessionDtos.SessionResponse> listMySessions() {
        UserPrincipal lecturer = SecurityUtils.requireRole(UserRole.LECTURER);
        reconcileStaleActiveSessions(lecturer.getUserId());
        return sessionRepository.findByLecturerIdOrderByCreatedAtDesc(lecturer.getUserId()).stream()
                .map(this::toSessionResponse)
                .toList();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void reconcileStaleActiveSessions(Long lecturerId) {
        List<AttendanceSession> actives = sessionRepository.findByLecturerIdAndStatusOrderByCreatedAtDesc(
                lecturerId, SessionStatus.ACTIVE);
        if (actives.size() <= 1) {
            return;
        }
        Instant now = Instant.now();
        Long keepId = actives.get(0).getId();
        for (AttendanceSession session : actives) {
            if (session.getId().equals(keepId)) {
                continue;
            }
            session.setStatus(SessionStatus.CLOSED);
            if (session.getClosedAt() == null) {
                session.setClosedAt(now);
            }
            sessionRepository.save(session);
        }
    }

    @Transactional
    public SessionDtos.QrResponse issueQr(Long sessionId) {
        AttendanceSession session = getSessionForLecturer(sessionId);
        if (session.getStatus() != SessionStatus.ACTIVE) {
            throw new ApiException("SESSION_CLOSED", "Session is not active", HttpStatus.BAD_REQUEST);
        }

        String nonce = UUID.randomUUID().toString().replace("-", "");
        Instant expiresAt = Instant.now().plusSeconds(appProperties.qr().ttlSeconds());
        String token = jwtService.createQrToken(
                session.getId(),
                session.getCourse().getCourseCode(),
                nonce,
                expiresAt
        );

        QrToken qrToken = QrToken.builder()
                .session(session)
                .tokenHash(hashToken(token))
                .nonce(nonce)
                .expiresAt(expiresAt)
                .consumed(false)
                .build();
        qrTokenRepository.save(qrToken);

        String image = qrImageService.toBase64Png(token, 320);
        return new SessionDtos.QrResponse(token, image, expiresAt);
    }

    @Transactional
    public SessionDtos.SessionResponse closeSession(Long sessionId) {
        AttendanceSession session = getSessionForLecturer(sessionId);
        session.setStatus(SessionStatus.CLOSED);
        session.setClosedAt(Instant.now());
        session = sessionRepository.save(session);
        return toSessionResponse(session);
    }

    public SessionDtos.SessionAttendanceResponse getAttendance(Long sessionId) {
        AttendanceSession session = getSessionForLecturer(sessionId);
        List<Student> enrolled = studentRepository.findByCourseId(session.getCourse().getId());

        Map<Long, AttendanceRecord> presentByStudent = attendanceRecordRepository.findBySessionId(sessionId).stream()
                .collect(Collectors.toMap(ar -> ar.getStudent().getId(), ar -> ar));

        List<SessionDtos.AttendanceRow> rows = enrolled.stream()
                .map(s -> {
                    AttendanceRecord ar = presentByStudent.get(s.getId());
                    return new SessionDtos.AttendanceRow(
                            s.getId(),
                            s.getName(),
                            s.getIndexNumber(),
                            ar != null,
                            ar != null ? ar.getAttendanceTime() : null
                    );
                })
                .sorted(Comparator.comparing(SessionDtos.AttendanceRow::name))
                .toList();

        long presentCount = rows.stream().filter(SessionDtos.AttendanceRow::present).count();
        return new SessionDtos.SessionAttendanceResponse(
                sessionId,
                session.getCourse().getCourseCode(),
                rows,
                presentCount,
                enrolled.size()
        );
    }

    public AttendanceSession validateQrToken(String token) {
        Claims claims;
        try {
            claims = jwtService.parseQrClaims(token);
        } catch (Exception e) {
            throw new ApiException("QR_INVALID", "Invalid QR code", HttpStatus.BAD_REQUEST);
        }

        String nonce = claims.get("nonce", String.class);
        Long sessionId = claims.get("sessionId", Long.class);

        QrToken qrToken = qrTokenRepository.findByNonce(nonce)
                .orElseThrow(() -> new ApiException("QR_INVALID", "QR token not recognized", HttpStatus.BAD_REQUEST));

        if (qrToken.isConsumed()) {
            throw new ApiException("QR_CONSUMED", "QR code already used", HttpStatus.BAD_REQUEST);
        }
        if (qrToken.getExpiresAt().isBefore(Instant.now())) {
            throw new ApiException("QR_EXPIRED", "QR code has expired", HttpStatus.BAD_REQUEST);
        }
        if (!hashToken(token).equals(qrToken.getTokenHash())) {
            throw new ApiException("QR_INVALID", "Invalid QR code", HttpStatus.BAD_REQUEST);
        }

        AttendanceSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ApiException("SESSION_NOT_FOUND", "Session not found", HttpStatus.NOT_FOUND));

        if (session.getStatus() != SessionStatus.ACTIVE) {
            throw new ApiException("SESSION_CLOSED", "Attendance session is closed", HttpStatus.BAD_REQUEST);
        }

        qrToken.setConsumed(true);
        qrTokenRepository.save(qrToken);
        return session;
    }

    private AttendanceSession getSessionForLecturer(Long id) {
        UserPrincipal lecturer = SecurityUtils.requireRole(UserRole.LECTURER);
        AttendanceSession session = sessionRepository.findById(id)
                .orElseThrow(() -> new ApiException("NOT_FOUND", "Session not found", HttpStatus.NOT_FOUND));
        if (!session.getLecturer().getId().equals(lecturer.getUserId())) {
            throw new ApiException("FORBIDDEN", "Not your session", HttpStatus.FORBIDDEN);
        }
        return session;
    }

    public SessionDtos.SessionResponse toSessionResponse(AttendanceSession session) {
        long count = attendanceRecordRepository.countBySessionId(session.getId());
        return new SessionDtos.SessionResponse(
                session.getId(),
                session.getCourse().getId(),
                session.getCourse().getCourseCode(),
                session.getCourse().getCourseName(),
                session.getStatus(),
                session.getSessionType(),
                session.getCreatedAt(),
                session.getClosedAt(),
                count
        );
    }

    private void closeAllActiveSessions(Long lecturerId) {
        List<AttendanceSession> actives = sessionRepository.findByLecturerIdAndStatusOrderByCreatedAtDesc(
                lecturerId, SessionStatus.ACTIVE);
        if (actives.isEmpty()) {
            return;
        }
        Instant now = Instant.now();
        for (AttendanceSession session : actives) {
            session.setStatus(SessionStatus.CLOSED);
            if (session.getClosedAt() == null) {
                session.setClosedAt(now);
            }
            sessionRepository.save(session);
        }
    }

    static String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
