package com.ucc.attendance.service;

import com.ucc.attendance.domain.*;
import com.ucc.attendance.dto.StatsDtos;
import com.ucc.attendance.exception.ApiException;
import com.ucc.attendance.repository.*;
import com.ucc.attendance.security.UserPrincipal;
import com.ucc.attendance.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@SuppressWarnings("null")
public class StatsService {

    private final AttendanceSessionRepository sessionRepository;
    private final AttendanceRecordRepository attendanceRecordRepository;
    private final StudentRepository studentRepository;
    private final CourseRepository courseRepository;
    private final LecturerRepository lecturerRepository;

    public StatsDtos.LecturerStats lecturerStats(Long sessionId) {
        UserPrincipal lecturer = SecurityUtils.requireRole(UserRole.LECTURER);
        AttendanceSession session;
        if (sessionId != null) {
            session = sessionRepository.findById(sessionId)
                    .orElseThrow(() -> new ApiException("NOT_FOUND", "Session not found", HttpStatus.NOT_FOUND));
            if (!session.getLecturer().getId().equals(lecturer.getUserId())) {
                throw new ApiException("FORBIDDEN", "Not your session", HttpStatus.FORBIDDEN);
            }
        } else {
            List<AttendanceSession> sessions = sessionRepository.findByLecturerIdAndStatusOrderByCreatedAtDesc(
                    lecturer.getUserId(), SessionStatus.ACTIVE);
            if (sessions.isEmpty()) {
                return buildLecturerOverallStats(lecturer.getUserId());
            }
            session = sessions.get(0);
        }
        return buildLecturerStats(session);
    }

    private StatsDtos.LecturerStats buildLecturerOverallStats(Long lecturerId) {
        List<Course> courses = courseRepository.findByLecturerId(lecturerId);
        long enrolled = courses.stream()
                .mapToLong(c -> studentRepository.findByCourseId(c.getId()).size())
                .sum();

        List<AttendanceSession> sessions = sessionRepository.findByLecturerIdOrderByCreatedAtDesc(lecturerId);
        long totalPresent = 0;
        long totalSlots = 0;

        for (AttendanceSession s : sessions) {
            long sessEnrolled = studentRepository.findByCourseId(s.getCourse().getId()).size();
            long sessPresent = attendanceRecordRepository.countBySessionId(s.getId());
            totalPresent += sessPresent;
            totalSlots += sessEnrolled;
        }

        long absent = Math.max(0, totalSlots - totalPresent);
        int rate = totalSlots == 0 ? 0 : (int) Math.round((totalPresent * 100.0) / totalSlots);

        return new StatsDtos.LecturerStats(
                enrolled,
                totalPresent,
                absent,
                rate,
                null,
                null
        );
    }


    public StatsDtos.StudentStats studentStats() {
        UserPrincipal student = SecurityUtils.requireRole(UserRole.STUDENT);
        Student s = studentRepository.findById(student.getUserId())
                .orElseThrow(() -> new ApiException("NOT_FOUND", "Student not found", HttpStatus.NOT_FOUND));

        long attended = attendanceRecordRepository.findByStudentIdOrderByAttendanceTimeDesc(student.getUserId()).size();
        long enrolledCourses = s.getCourses().size();
        long totalSessions = sessionRepository.findAll().stream()
                .filter(sess -> sess.getStatus() == SessionStatus.CLOSED || sess.getStatus() == SessionStatus.ACTIVE)
                .filter(sess -> s.getCourses().stream().anyMatch(c -> c.getId().equals(sess.getCourse().getId())))
                .count();
        if (totalSessions == 0 && enrolledCourses > 0) {
            totalSessions = attended;
        }
        long missed = Math.max(0, totalSessions - attended);
        int rate = totalSessions == 0 ? 0 : (int) Math.round((attended * 100.0) / totalSessions);

        return new StatsDtos.StudentStats(totalSessions, attended, missed, rate);
    }

    public StatsDtos.LecturerAnalyticsResponse lecturerAnalytics() {
        UserPrincipal lecturer = SecurityUtils.requireRole(UserRole.LECTURER);
        Lecturer l = lecturerRepository.findById(lecturer.getUserId())
                .orElseThrow(() -> new ApiException("NOT_FOUND", "Lecturer not found", HttpStatus.NOT_FOUND));

        List<StatsDtos.CourseAnalytics> items = new ArrayList<>();
        for (Course course : l.getCourses()) {
            List<AttendanceSession> sessions = sessionRepository.findAll().stream()
                    .filter(s -> s.getCourse().getId().equals(course.getId()))
                    .filter(s -> s.getLecturer().getId().equals(l.getId()))
                    .toList();
            long sessionsHeld = sessions.size();
            long totalPresent = sessions.stream()
                    .mapToLong(s -> attendanceRecordRepository.countBySessionId(s.getId()))
                    .sum();
            long enrolled = studentRepository.findByCourseId(course.getId()).size();
            long totalSlots = enrolled * Math.max(sessionsHeld, 1);
            int avgRate = totalSlots == 0 ? 0 : (int) Math.round((totalPresent * 100.0) / totalSlots);
            items.add(new StatsDtos.CourseAnalytics(
                    course.getCourseCode(),
                    course.getCourseName(),
                    sessionsHeld,
                    totalPresent,
                    totalSlots,
                    avgRate
            ));
        }
        return new StatsDtos.LecturerAnalyticsResponse(items);
    }

    private StatsDtos.LecturerStats buildLecturerStats(AttendanceSession session) {
        long enrolled = studentRepository.findByCourseId(session.getCourse().getId()).size();
        long present = attendanceRecordRepository.countBySessionId(session.getId());
        long absent = Math.max(0, enrolled - present);
        int rate = enrolled == 0 ? 0 : (int) Math.round((present * 100.0) / enrolled);
        return new StatsDtos.LecturerStats(
                enrolled,
                present,
                absent,
                rate,
                session.getId(),
                session.getCourse().getCourseCode()
        );
    }
}
