package com.ucc.attendance.controller;

import com.ucc.attendance.domain.*;
import com.ucc.attendance.dto.*;
import com.ucc.attendance.repository.*;
import com.ucc.attendance.security.UserPrincipal;
import com.ucc.attendance.service.*;
import com.ucc.attendance.exception.ApiException;
import com.ucc.attendance.util.SecurityUtils;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class WebController {

    private final AuthService authService;
    private final SessionService sessionService;
    private final StatsService statsService;
    private final AttendanceService attendanceService;
    private final CourseService courseService;
    private final CourseRepository courseRepository;
    private final DepartmentRepository departmentRepository;
    private final AttendanceSessionRepository sessionRepository;
    private final StudentRepository studentRepository;
    private final LecturerRepository lecturerRepository;
    private final AttendanceRecordRepository attendanceRecordRepository;


    @GetMapping("/")
    public String index(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal) {
            if (principal.getRole() == UserRole.STUDENT) {
                return "redirect:/student";
            } else if (principal.getRole() == UserRole.LECTURER) {
                return "redirect:/lecturer";
            }
        }
        model.addAttribute("departments", departmentRepository.findAll());
        return "login";
    }

    @PostMapping("/login")
    @ResponseBody
    public ResponseEntity<String> login(
            @RequestParam String identifier,
            @RequestParam String password,
            @RequestParam UserRole role,
            HttpServletResponse response) {
        try {
            AuthDtos.AuthResponse authRes = authService.login(new AuthDtos.LoginRequest(identifier, password, role));
            setTokenCookie(response, authRes.token());
            String redirectUrl = role == UserRole.STUDENT ? "/student" : "/lecturer";
            return ResponseEntity.ok()
                    .header("HX-Redirect", redirectUrl)
                    .body("");
        } catch (Exception e) {
            String errorMsg = e.getMessage() != null ? e.getMessage() : "Invalid credentials";
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("<div class=\"rounded-lg bg-red-50 px-4 py-3 text-sm text-red-800\">" + errorMsg + "</div>");
        }
    }

    @PostMapping("/register/student")
    @ResponseBody
    public ResponseEntity<String> registerStudent(
            @RequestParam String name,
            @RequestParam String email,
            @RequestParam String indexNumber,
            @RequestParam String departmentCode,
            @RequestParam String password,
            HttpServletResponse response) {
        try {
            AuthDtos.AuthResponse authRes = authService.registerStudent(
                    new AuthDtos.StudentRegisterRequest(name, email, indexNumber, departmentCode, password)
            );
            setTokenCookie(response, authRes.token());
            return ResponseEntity.ok()
                    .header("HX-Redirect", "/student")
                    .body("");
        } catch (Exception e) {
            String errorMsg = e.getMessage() != null ? e.getMessage() : "Registration failed";
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("<div class=\"rounded-lg bg-red-50 px-4 py-3 text-sm text-red-800\">" + errorMsg + "</div>");
        }
    }

    @PostMapping("/register/lecturer")
    @ResponseBody
    public ResponseEntity<String> registerLecturer(
            @RequestParam String name,
            @RequestParam String lecturerCode,
            @RequestParam String departmentCode,
            @RequestParam String password,
            HttpServletResponse response) {
        try {
            AuthDtos.AuthResponse authRes = authService.registerLecturer(
                    new AuthDtos.LecturerRegisterRequest(name, lecturerCode, departmentCode, password)
            );
            setTokenCookie(response, authRes.token());
            return ResponseEntity.ok()
                    .header("HX-Redirect", "/lecturer")
                    .body("");
        } catch (Exception e) {
            String errorMsg = e.getMessage() != null ? e.getMessage() : "Registration failed";
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("<div class=\"rounded-lg bg-red-50 px-4 py-3 text-sm text-red-800\">" + errorMsg + "</div>");
        }
    }

    @GetMapping("/logout")
    public String logout(HttpServletResponse response) {
        clearTokenCookie(response);
        SecurityContextHolder.clearContext();
        return "redirect:/";
    }

    @GetMapping("/lecturer")
    @PreAuthorize("hasRole('LECTURER')")
    public String lecturerWorkspace(Model model) {
        UserPrincipal lecturer = SecurityUtils.requireRole(UserRole.LECTURER);
        AuthDtos.MeResponse me = authService.me();
        
        List<AttendanceSession> activeSessions = sessionRepository.findByLecturerIdAndStatusOrderByCreatedAtDesc(
                lecturer.getUserId(), SessionStatus.ACTIVE
        );
        AttendanceSession activeSession = activeSessions.isEmpty() ? null : activeSessions.get(0);

        model.addAttribute("user", me);
        model.addAttribute("courses", courseRepository.findByLecturerId(lecturer.getUserId()));
        model.addAttribute("allCourses", courseRepository.findAllOrdered());
        model.addAttribute("departments", departmentRepository.findAll());
        model.addAttribute("activeSession", activeSession);

        if (activeSession != null) {
            model.addAttribute("stats", statsService.lecturerStats(activeSession.getId()));
            model.addAttribute("attendance", sessionService.getAttendance(activeSession.getId()));
            try {
                model.addAttribute("qr", sessionService.issueQr(activeSession.getId()));
            } catch (Exception ignored) {}
        } else {
            model.addAttribute("stats", statsService.lecturerStats(null));
        }

        return "lecturer";
    }

    @PostMapping("/lecturer/session")
    @PreAuthorize("hasRole('LECTURER')")
    @ResponseBody
    public ResponseEntity<String> startSession(
            @RequestParam Long courseId,
            @RequestParam SessionType sessionType) {
        try {
            sessionService.createSession(new SessionDtos.CreateSessionRequest(courseId, sessionType));
            return ResponseEntity.ok()
                    .header("HX-Redirect", "/lecturer")
                    .body("");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("<div class=\"rounded-lg bg-red-50 px-4 py-3 text-sm text-red-800\">" + e.getMessage() + "</div>");
        }
    }

    @PostMapping("/lecturer/session/{id}/close")
    @PreAuthorize("hasRole('LECTURER')")
    @ResponseBody
    public ResponseEntity<String> closeSession(@PathVariable Long id) {
        try {
            sessionService.closeSession(id);
            return ResponseEntity.ok()
                    .header("HX-Redirect", "/lecturer/session/" + id + "/summary")
                    .body("");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("<div class=\"rounded-lg bg-red-50 px-4 py-3 text-sm text-red-800\">" + e.getMessage() + "</div>");
        }
    }

    @GetMapping("/lecturer/session/qr")
    @PreAuthorize("hasRole('LECTURER')")
    public String getSessionQr(Model model) {
        UserPrincipal lecturer = SecurityUtils.requireRole(UserRole.LECTURER);
        List<AttendanceSession> activeSessions = sessionRepository.findByLecturerIdAndStatusOrderByCreatedAtDesc(
                lecturer.getUserId(), SessionStatus.ACTIVE
        );
        if (activeSessions.isEmpty()) {
            return "fragments/lecturer-fragments :: qr-container-empty";
        }
        AttendanceSession activeSession = activeSessions.get(0);
        try {
            SessionDtos.QrResponse qr = sessionService.issueQr(activeSession.getId());
            model.addAttribute("qr", qr);
            return "fragments/lecturer-fragments :: qr-container";
        } catch (Exception e) {
            return "fragments/lecturer-fragments :: qr-container-empty";
        }
    }

    @GetMapping("/lecturer/session/attendance")
    @PreAuthorize("hasRole('LECTURER')")
    public String getSessionAttendance(Model model) {
        UserPrincipal lecturer = SecurityUtils.requireRole(UserRole.LECTURER);
        List<AttendanceSession> activeSessions = sessionRepository.findByLecturerIdAndStatusOrderByCreatedAtDesc(
                lecturer.getUserId(), SessionStatus.ACTIVE
        );
        if (activeSessions.isEmpty()) {
            model.addAttribute("attendance", new SessionDtos.SessionAttendanceResponse(null, null, List.of(), 0, 0));
        } else {
            model.addAttribute("attendance", sessionService.getAttendance(activeSessions.get(0).getId()));
        }
        return "fragments/lecturer-fragments :: attendance-rows";
    }

    @GetMapping("/lecturer/session/stats")
    @PreAuthorize("hasRole('LECTURER')")
    public String getLecturerStats(Model model) {
        UserPrincipal lecturer = SecurityUtils.requireRole(UserRole.LECTURER);
        List<AttendanceSession> activeSessions = sessionRepository.findByLecturerIdAndStatusOrderByCreatedAtDesc(
                lecturer.getUserId(), SessionStatus.ACTIVE
        );
        AttendanceSession activeSession = activeSessions.isEmpty() ? null : activeSessions.get(0);
        if (activeSession != null) {
            model.addAttribute("stats", statsService.lecturerStats(activeSession.getId()));
        } else {
            model.addAttribute("stats", new StatsDtos.LecturerStats(0, 0, 0, 0, null, null));
        }
        return "fragments/lecturer-fragments :: stats-grid";
    }

    @GetMapping("/lecturer/analytics")
    @PreAuthorize("hasRole('LECTURER')")
    public String getLecturerAnalytics(Model model) {
        StatsDtos.LecturerAnalyticsResponse analytics = statsService.lecturerAnalytics();
        model.addAttribute("analytics", analytics);
        return "fragments/lecturer-fragments :: analytics-list";
    }

    // ── Lecturer Course Management ──────────────────────────────────────

    @GetMapping("/lecturer/courses")
    @PreAuthorize("hasRole('LECTURER')")
    public String getLecturerCourses(Model model) {
        UserPrincipal lecturer = SecurityUtils.requireRole(UserRole.LECTURER);
        model.addAttribute("courses", courseRepository.findByLecturerId(lecturer.getUserId()));
        model.addAttribute("allCourses", courseRepository.findAllOrdered());
        return "fragments/lecturer-fragments :: course-list";
    }

    @PostMapping("/lecturer/course")
    @PreAuthorize("hasRole('LECTURER')")
    @ResponseBody
    public ResponseEntity<String> createCourse(
            @RequestParam String courseCode,
            @RequestParam String courseName,
            @RequestParam String departmentCode) {
        try {
            courseService.createCourse(new CourseDtos.CreateCourseRequest(courseCode, courseName, departmentCode));
            return ResponseEntity.ok()
                    .header("HX-Redirect", "/lecturer")
                    .body("");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("<div class=\"rounded-lg bg-red-50 px-4 py-3 text-sm text-red-800\">" + e.getMessage() + "</div>");
        }
    }

    @PostMapping("/lecturer/course/assign")
    @PreAuthorize("hasRole('LECTURER')")
    @ResponseBody
    public ResponseEntity<String> assignCourse(@RequestParam Long courseId) {
        try {
            courseService.assignCourseToLecturer(courseId);
            return ResponseEntity.ok()
                    .header("HX-Redirect", "/lecturer")
                    .body("");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("<div class=\"rounded-lg bg-red-50 px-4 py-3 text-sm text-red-800\">" + e.getMessage() + "</div>");
        }
    }

    @GetMapping("/student")
    @PreAuthorize("hasRole('STUDENT')")
    public String studentWorkspace(Model model) {
        UserPrincipal student = SecurityUtils.requireRole(UserRole.STUDENT);
        AuthDtos.MeResponse me = authService.me();
        model.addAttribute("user", me);
        model.addAttribute("stats", statsService.studentStats());
        model.addAttribute("history", attendanceService.history());
        model.addAttribute("enrolledCourses", courseRepository.findByStudentId(student.getUserId()));
        model.addAttribute("availableCourses", courseService.listAvailableCourses());
        return "student";
    }

    @PostMapping("/student/scan")
    @PreAuthorize("hasRole('STUDENT')")
    public String confirmAttendance(
            @RequestParam String token,
            @RequestParam String deviceFingerprint,
            @RequestParam String indexNumber,
            Model model,
            HttpServletRequest request) {
        try {
            AttendanceDtos.ScanResponse scanResponse = attendanceService.scan(
                    new AttendanceDtos.ScanRequest(token, deviceFingerprint, indexNumber), request
            );
            model.addAttribute("successCourse", scanResponse.courseCode());
            String formattedTime = scanResponse.attendanceTime() != null ?
                    DateTimeFormatter.ofPattern("hh:mm a").withZone(ZoneId.systemDefault())
                            .format(scanResponse.attendanceTime()) : "Now";
            model.addAttribute("markTime", formattedTime);
            return "fragments/student-fragments :: success-container";
        } catch (Exception e) {
            model.addAttribute("scanError", e.getMessage() != null ? e.getMessage() : "Attendance marking failed");
            model.addAttribute("token", token);
            model.addAttribute("indexNumber", indexNumber);
            return "fragments/student-fragments :: confirm-form-error";
        }
    }

    @GetMapping("/student/stats")
    @PreAuthorize("hasRole('STUDENT')")
    public String getStudentStats(Model model) {
        model.addAttribute("stats", statsService.studentStats());
        return "fragments/student-fragments :: stats-grid";
    }

    @GetMapping("/student/history")
    @PreAuthorize("hasRole('STUDENT')")
    public String getStudentHistory(Model model) {
        model.addAttribute("history", attendanceService.history());
        return "fragments/student-fragments :: history-rows";
    }

    // ── Student Course Management ───────────────────────────────────────

    @GetMapping("/student/courses")
    @PreAuthorize("hasRole('STUDENT')")
    public String getStudentCourses(Model model) {
        UserPrincipal student = SecurityUtils.requireRole(UserRole.STUDENT);
        model.addAttribute("enrolledCourses", courseRepository.findByStudentId(student.getUserId()));
        model.addAttribute("availableCourses", courseService.listAvailableCourses());
        return "fragments/student-fragments :: course-list";
    }

    @PostMapping("/student/courses/join")
    @PreAuthorize("hasRole('STUDENT')")
    @ResponseBody
    public ResponseEntity<String> joinCourse(@RequestParam String courseCode) {
        try {
            courseService.joinCourse(courseCode);
            return ResponseEntity.ok()
                    .header("HX-Redirect", "/student")
                    .body("");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("<div class=\"rounded-lg bg-red-50 px-4 py-3 text-sm text-red-800\">" + e.getMessage() + "</div>");
        }
    }

    private void setTokenCookie(HttpServletResponse response, String token) {
        Cookie cookie = new Cookie("token", token);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(480 * 60); // 8 hours (same as JWT configuration expiration)
        response.addCookie(cookie);
    }

    private void clearTokenCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie("token", "");
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }

    // ── Classroom & Summary Mappings ─────────────────────────────────────

    @GetMapping("/lecturer/session/{id}/summary")
    @PreAuthorize("hasRole('LECTURER')")
    public String sessionSummary(@PathVariable Long id, Model model) {
        UserPrincipal lecturer = SecurityUtils.requireRole(UserRole.LECTURER);
        AuthDtos.MeResponse me = authService.me();
        
        AttendanceSession session = sessionRepository.findById(id)
                .orElseThrow(() -> new ApiException("SESSION_NOT_FOUND", "Session not found", HttpStatus.NOT_FOUND));
                
        if (!session.getLecturer().getId().equals(lecturer.getUserId())) {
            throw new ApiException("UNAUTHORIZED", "Unauthorized to view this session", HttpStatus.FORBIDDEN);
        }
        
        model.addAttribute("user", me);
        model.addAttribute("session", session);
        model.addAttribute("stats", statsService.lecturerStats(id));
        model.addAttribute("attendance", sessionService.getAttendance(id));
        
        List<AttendanceSession> allSessions = sessionRepository.findByLecturerIdOrderByCreatedAtDesc(lecturer.getUserId());
        model.addAttribute("allSessions", allSessions);
        
        return "session-summary";
    }

    public record StudentRosterRow(
            Long id,
            String name,
            String indexNumber,
            long sessionsAttended,
            int attendanceRate
    ) {}

    @GetMapping("/lecturer/course/{id}")
    @PreAuthorize("hasRole('LECTURER')")
    public String lecturerCourseDetails(@PathVariable Long id, Model model) {
        UserPrincipal lecturer = SecurityUtils.requireRole(UserRole.LECTURER);
        AuthDtos.MeResponse me = authService.me();
        
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new ApiException("COURSE_NOT_FOUND", "Course not found", HttpStatus.NOT_FOUND));
        
        Lecturer l = lecturerRepository.findById(lecturer.getUserId())
                .orElseThrow(() -> new ApiException("LECTURER_NOT_FOUND", "Lecturer not found", HttpStatus.NOT_FOUND));
        if (l.getCourses().stream().noneMatch(c -> c.getId().equals(course.getId()))) {
            throw new ApiException("UNAUTHORIZED", "Not your course", HttpStatus.FORBIDDEN);
        }
        
        List<Student> enrolled = studentRepository.findByCourseId(course.getId());
        List<AttendanceSession> sessions = sessionRepository.findAll().stream()
                .filter(s -> s.getCourse().getId().equals(course.getId()))
                .sorted((s1, s2) -> s2.getCreatedAt().compareTo(s1.getCreatedAt()))
                .toList();

        long totalSessions = sessions.stream()
                .filter(s -> s.getStatus() == SessionStatus.CLOSED || s.getStatus() == SessionStatus.ACTIVE)
                .count();

        List<StudentRosterRow> roster = enrolled.stream()
                .map(s -> {
                    long attended = attendanceRecordRepository.findByStudentIdOrderByAttendanceTimeDesc(s.getId()).stream()
                            .filter(r -> r.getSession().getCourse().getId().equals(course.getId()))
                            .count();
                    int rate = totalSessions == 0 ? 0 : (int) Math.round((attended * 100.0) / totalSessions);
                    return new StudentRosterRow(s.getId(), s.getName(), s.getIndexNumber(), attended, rate);
                })
                .sorted((r1, r2) -> r1.name().compareToIgnoreCase(r2.name()))
                .toList();

        model.addAttribute("user", me);
        model.addAttribute("course", course);
        model.addAttribute("roster", roster);
        model.addAttribute("sessions", sessions);
        model.addAttribute("totalSessions", totalSessions);
        
        return "lecturer-course";
    }

    @GetMapping("/student/course/{id}")
    @PreAuthorize("hasRole('STUDENT')")
    public String studentCourseDetails(@PathVariable Long id, Model model) {
        UserPrincipal studentPrincipal = SecurityUtils.requireRole(UserRole.STUDENT);
        AuthDtos.MeResponse me = authService.me();
        
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new ApiException("COURSE_NOT_FOUND", "Course not found", HttpStatus.NOT_FOUND));
        
        Student s = studentRepository.findById(studentPrincipal.getUserId())
                .orElseThrow(() -> new ApiException("STUDENT_NOT_FOUND", "Student not found", HttpStatus.NOT_FOUND));
        if (s.getCourses().stream().noneMatch(c -> c.getId().equals(course.getId()))) {
            throw new ApiException("UNAUTHORIZED", "You are not enrolled in this course", HttpStatus.FORBIDDEN);
        }
        
        List<Student> classmates = studentRepository.findByCourseId(course.getId()).stream()
                .sorted((c1, c2) -> c1.getName().compareToIgnoreCase(c2.getName()))
                .toList();
        
        List<AttendanceSession> sessions = sessionRepository.findAll().stream()
                .filter(sess -> sess.getCourse().getId().equals(course.getId()))
                .sorted((s1, s2) -> s2.getCreatedAt().compareTo(s1.getCreatedAt()))
                .toList();
        
        List<AttendanceRecord> records = attendanceRecordRepository.findByStudentIdOrderByAttendanceTimeDesc(s.getId()).stream()
                .filter(r -> r.getSession().getCourse().getId().equals(course.getId()))
                .toList();
        
        long totalSessions = sessions.stream()
                .filter(sess -> sess.getStatus() == SessionStatus.CLOSED || sess.getStatus() == SessionStatus.ACTIVE)
                .count();
        long attended = records.size();
        long missed = Math.max(0, totalSessions - attended);
        int rate = totalSessions == 0 ? 0 : (int) Math.round((attended * 100.0) / totalSessions);
        
        model.addAttribute("user", me);
        model.addAttribute("course", course);
        model.addAttribute("classmates", classmates);
        model.addAttribute("sessions", sessions);
        model.addAttribute("records", records);
        model.addAttribute("stats", new StatsDtos.StudentStats(totalSessions, attended, missed, rate));
        
        return "student-course";
    }

    @PostMapping("/lecturer/course/{id}/add-student")
    @PreAuthorize("hasRole('LECTURER')")
    @ResponseBody
    public ResponseEntity<String> addStudentToCourse(
            @PathVariable Long id,
            @RequestParam String indexNumber) {
        try {
            Course course = courseRepository.findById(id)
                    .orElseThrow(() -> new ApiException("NOT_FOUND", "Course not found", HttpStatus.NOT_FOUND));
            Student student = studentRepository.findByIndexNumberIgnoreCase(indexNumber.trim())
                    .orElseThrow(() -> new ApiException("NOT_FOUND", "Student with index number '" + indexNumber + "' not found", HttpStatus.NOT_FOUND));
            
            if (student.getCourses().stream().anyMatch(c -> c.getId().equals(course.getId()))) {
                throw new ApiException("ALREADY_ENROLLED", "Student is already enrolled in this course", HttpStatus.CONFLICT);
            }
            
            student.getCourses().add(course);
            studentRepository.save(student);
            
            return ResponseEntity.ok()
                    .header("HX-Redirect", "/lecturer/course/" + id)
                    .body("");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("<div class=\"rounded-lg bg-red-50 px-4 py-3 text-sm text-red-800\">" + e.getMessage() + "</div>");
        }
    }

    @PostMapping("/lecturer/course/{id}/remove-student")
    @PreAuthorize("hasRole('LECTURER')")
    @ResponseBody
    public ResponseEntity<String> removeStudentFromCourse(
            @PathVariable Long id,
            @RequestParam Long studentId) {
        try {
            Course course = courseRepository.findById(id)
                    .orElseThrow(() -> new ApiException("NOT_FOUND", "Course not found", HttpStatus.NOT_FOUND));
            Student student = studentRepository.findById(studentId)
                    .orElseThrow(() -> new ApiException("NOT_FOUND", "Student not found", HttpStatus.NOT_FOUND));
            
            student.getCourses().remove(course);
            studentRepository.save(student);
            
            return ResponseEntity.ok()
                    .header("HX-Redirect", "/lecturer/course/" + id)
                    .body("");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("<div class=\"rounded-lg bg-red-50 px-4 py-3 text-sm text-red-800\">" + e.getMessage() + "</div>");
        }
    }
}
