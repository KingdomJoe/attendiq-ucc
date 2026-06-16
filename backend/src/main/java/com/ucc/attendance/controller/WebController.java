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
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Controller
@RequiredArgsConstructor
@SuppressWarnings("null")
public class WebController {

    private final AuthService authService;
    private final SessionService sessionService;
    private final StatsService statsService;
    private final AttendanceService attendanceService;
    private final CourseService courseService;
    private final DepartmentRepository departmentRepository;
    private final AttendanceRecordRepository attendanceRecordRepository;

    // ── Index / Login Page ──────────────────────────────────────────

    @GetMapping("/")
    public String index(@RequestParam(required = false) String tab, Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal) {
            if (principal.getRole() == UserRole.STUDENT) {
                return "redirect:/student";
            } else if (principal.getRole() == UserRole.LECTURER) {
                return "redirect:/lecturer";
            }
        }
        model.addAttribute("departments", departmentRepository.findAll());
        model.addAttribute("tab", tab);
        if (!model.containsAttribute("loginForm")) {
            model.addAttribute("loginForm", new WebForms.LoginForm());
        }
        if (!model.containsAttribute("studentRegisterForm")) {
            model.addAttribute("studentRegisterForm", new WebForms.StudentRegisterForm());
        }
        if (!model.containsAttribute("lecturerRegisterForm")) {
            model.addAttribute("lecturerRegisterForm", new WebForms.LecturerRegisterForm());
        }
        if (!model.containsAttribute("forgotPasswordForm")) {
            WebForms.ForgotPasswordForm forgot = new WebForms.ForgotPasswordForm();
            forgot.setRole(UserRole.STUDENT);
            model.addAttribute("forgotPasswordForm", forgot);
        }
        return "login";
    }

    @GetMapping("/login")
    public String loginPage() {
        return "redirect:/?tab=login";
    }

    @PostMapping("/login")
    public String login(
            @Valid @ModelAttribute("loginForm") WebForms.LoginForm form,
            BindingResult bindingResult,
            HttpServletResponse response,
            Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("departments", departmentRepository.findAll());
            model.addAttribute("tab", "login");
            return "login";
        }
        try {
            AuthDtos.AuthResponse authRes = authService.login(
                    new AuthDtos.LoginRequest(form.getIdentifier(), form.getPassword(), form.getRole()));
            setTokenCookie(response, authRes.token());
            if (form.getRole() == UserRole.STUDENT) {
                return "redirect:/student";
            }
            return "redirect:/lecturer";
        } catch (Exception e) {
            String errorMsg = e.getMessage() != null ? e.getMessage() : "Invalid credentials";
            model.addAttribute("error", errorMsg);
            model.addAttribute("departments", departmentRepository.findAll());
            model.addAttribute("tab", "login");
            return "login";
        }
    }

    @GetMapping("/register/student")
    public String registerStudentPage() {
        return "redirect:/?tab=register-student";
    }

    @PostMapping("/register/student")
    public String registerStudent(
            @Valid @ModelAttribute("studentRegisterForm") WebForms.StudentRegisterForm form,
            BindingResult bindingResult,
            HttpServletResponse response,
            Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("departments", departmentRepository.findAll());
            model.addAttribute("tab", "register-student");
            return "login";
        }
        try {
            AuthDtos.AuthResponse authRes = authService.registerStudent(
                    new AuthDtos.StudentRegisterRequest(
                            form.getName(), form.getEmail(), form.getIndexNumber(), form.getDepartmentCode(), form.getPassword())
            );
            setTokenCookie(response, authRes.token());
            return "redirect:/student";
        } catch (Exception e) {
            String errorMsg = e.getMessage() != null ? e.getMessage() : "Registration failed";
            model.addAttribute("error", errorMsg);
            model.addAttribute("departments", departmentRepository.findAll());
            model.addAttribute("tab", "register-student");
            return "login";
        }
    }

    @GetMapping("/register/lecturer")
    public String registerLecturerPage() {
        return "redirect:/?tab=register-lecturer";
    }

    @PostMapping("/register/lecturer")
    public String registerLecturer(
            @Valid @ModelAttribute("lecturerRegisterForm") WebForms.LecturerRegisterForm form,
            BindingResult bindingResult,
            HttpServletResponse response,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("departments", departmentRepository.findAll());
            model.addAttribute("tab", "register-lecturer");
            return "login";
        }
        try {
            AuthDtos.AuthResponse authRes = authService.registerLecturer(
                    new AuthDtos.LecturerRegisterRequest(
                            form.getName(), form.getLecturerCode(), form.getDepartmentCode(), form.getPassword())
            );
            setTokenCookie(response, authRes.token());
            redirectAttributes.addFlashAttribute("success", "Lecturer registered successfully. You can manage sessions via this web portal or the Windows desktop app.");
            return "redirect:/lecturer";
        } catch (Exception e) {
            String errorMsg = e.getMessage() != null ? e.getMessage() : "Registration failed";
            model.addAttribute("error", errorMsg);
            model.addAttribute("departments", departmentRepository.findAll());
            model.addAttribute("tab", "register-lecturer");
            return "login";
        }
    }

    @PostMapping("/forgot-password")
    public String forgotPassword(
            @Valid @ModelAttribute("forgotPasswordForm") WebForms.ForgotPasswordForm form,
            BindingResult bindingResult,
            Model model) {
        if (!form.getNewPassword().equals(form.getConfirmPassword())) {
            bindingResult.rejectValue("confirmPassword", "mismatch", "Passwords do not match");
        }
        if (bindingResult.hasErrors()) {
            model.addAttribute("departments", departmentRepository.findAll());
            model.addAttribute("tab", "forgot-password");
            return "login";
        }
        try {
            authService.resetPassword(new AuthDtos.ForgotPasswordRequest(
                    form.getIdentifier().trim(),
                    form.getRole(),
                    form.getNewPassword(),
                    form.getConfirmPassword()));
            model.addAttribute("success", "Password updated. You can sign in with your new password.");
            model.addAttribute("departments", departmentRepository.findAll());
            model.addAttribute("tab", "login");
            model.addAttribute("loginForm", new WebForms.LoginForm());
            return "login";
        } catch (ApiException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("departments", departmentRepository.findAll());
            model.addAttribute("tab", "forgot-password");
            return "login";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage() != null ? e.getMessage() : "Password reset failed");
            model.addAttribute("departments", departmentRepository.findAll());
            model.addAttribute("tab", "forgot-password");
            return "login";
        }
    }

    @GetMapping("/profile")
    public String profilePage(Model model) {
        AuthDtos.MeResponse me = authService.me();
        WebForms.ProfileForm form = new WebForms.ProfileForm();
        form.setName(me.displayName());
        model.addAttribute("user", me);
        model.addAttribute("profileForm", form);
        return "profile";
    }

    @PostMapping("/profile")
    public String updateProfile(
            @Valid @ModelAttribute("profileForm") WebForms.ProfileForm form,
            BindingResult bindingResult,
            Model model) {
        AuthDtos.MeResponse me = authService.me();
        model.addAttribute("user", me);
        if (form.getNewPassword() != null && !form.getNewPassword().isBlank()
                && form.getNewPassword().length() < 8) {
            bindingResult.rejectValue("newPassword", "size", "New password must be at least 8 characters");
        }
        if (form.getNewPassword() != null && !form.getNewPassword().isBlank()
                && (form.getCurrentPassword() == null || form.getCurrentPassword().isBlank())) {
            bindingResult.rejectValue("currentPassword", "required", "Current password is required to set a new password");
        }
        if (bindingResult.hasErrors()) {
            return "profile";
        }
        try {
            AuthDtos.MeResponse updated = authService.updateProfile(
                    new AuthDtos.UpdateProfileRequest(form.getName(), form.getCurrentPassword(), form.getNewPassword()));
            form.setName(updated.displayName());
            form.setCurrentPassword(null);
            form.setNewPassword(null);
            model.addAttribute("profileForm", form);
            model.addAttribute("success", "Profile updated successfully.");
            return "profile";
        } catch (ApiException e) {
            model.addAttribute("error", e.getMessage());
            return "profile";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage() != null ? e.getMessage() : "Profile update failed");
            return "profile";
        }
    }

    @GetMapping("/logout")
    public String logout(HttpServletResponse response) {
        clearTokenCookie(response);
        SecurityContextHolder.clearContext();
        return "redirect:/";
    }

    @GetMapping("/lecturer-download")
    public String lecturerDownload() {
        return "lecturer-download";
    }

    // ── Student Workspace ───────────────────────────────────────────

    @GetMapping("/student")
    public String studentWorkspace(Model model) {
        SecurityUtils.requireRole(UserRole.STUDENT);
        AuthDtos.MeResponse me = authService.me();
        model.addAttribute("user", me);
        model.addAttribute("stats", statsService.studentStats());
        model.addAttribute("history", attendanceService.history());
        model.addAttribute("enrolledCourses", courseService.listCourses());
        model.addAttribute("availableCourses", courseService.listAvailableCourses());
        return "student";
    }

    // ── Session Code Manual Entry (no-JS fallback) ──────────────────

    @PostMapping("/student/scan-code")
    public String scanByCode(
            @RequestParam String sessionCode,
            @RequestParam String indexNumber,
            @RequestParam(required = false) String clientDeviceId,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {
        try {
            AttendanceDtos.ScanResponse scanResponse = attendanceService.scan(
                    new AttendanceDtos.ScanRequest(sessionCode.trim(), indexNumber, clientDeviceId, null),
                    request
            );

            redirectAttributes.addFlashAttribute("scanSuccess", true);
            redirectAttributes.addFlashAttribute("successCourse", scanResponse.courseCode());
            String formattedTime = scanResponse.attendanceTime() != null ?
                    DateTimeFormatter.ofPattern("hh:mm a").withZone(ZoneId.systemDefault())
                            .format(scanResponse.attendanceTime()) : "Now";
            redirectAttributes.addFlashAttribute("markTime", formattedTime);
            return "redirect:/student";
        } catch (ApiException e) {
            redirectAttributes.addFlashAttribute("error", scanErrorMessage(e));
            return "redirect:/student";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage() != null ? e.getMessage() : "Invalid session code");
            return "redirect:/student";
        }
    }

    // ── Student Course Management ───────────────────────────────────

    @PostMapping("/student/courses/join")
    public String joinCourse(
            @RequestParam String courseCode,
            RedirectAttributes redirectAttributes) {
        try {
            courseService.joinCourse(courseCode);
            redirectAttributes.addFlashAttribute("success", "Joined " + courseCode.toUpperCase() + " successfully.");
            return "redirect:/student";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/student";
        }
    }

    @PostMapping("/student/courses/{id}/leave")
    public String leaveCourse(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes) {
        try {
            courseService.leaveCourse(id);
            redirectAttributes.addFlashAttribute("success", "Removed course from My Courses.");
            return "redirect:/student";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/student";
        }
    }

    @GetMapping("/student/course/{id}")
    public String studentCourseDetails(@PathVariable Long id, Model model) {
        UserPrincipal studentPrincipal = SecurityUtils.requireRole(UserRole.STUDENT);
        AuthDtos.MeResponse me = authService.me();

        CourseDtos.CourseDetailResponse detail = courseService.getCourseDetail(id);

        java.util.Set<Long> attendedSessionIds = attendanceRecordRepository.findByStudentIdOrderByAttendanceTimeDesc(studentPrincipal.getUserId()).stream()
                .filter(r -> r.getSession().getCourse().getId().equals(id))
                .map(r -> r.getSession().getId())
                .collect(java.util.stream.Collectors.toSet());

        long totalSessions = detail.sessions().stream()
                .filter(sess -> sess.status() == SessionStatus.CLOSED || sess.status() == SessionStatus.ACTIVE)
                .count();
        long attended = attendedSessionIds.size();
        long missed = Math.max(0, totalSessions - attended);
        int rate = totalSessions == 0 ? 0 : (int) Math.round((attended * 100.0) / totalSessions);

        model.addAttribute("user", me);
        model.addAttribute("course", detail.course());
        model.addAttribute("classmates", detail.roster());
        model.addAttribute("sessions", detail.sessions());
        model.addAttribute("attendedSessionIds", attendedSessionIds);
        model.addAttribute("stats", new StatsDtos.StudentStats(totalSessions, attended, missed, rate));

        return "student-course";
    }

    // ── Lecturer Workspace ──────────────────────────────────────────

    @GetMapping("/lecturer")
    public String lecturerWorkspace(Model model) {
        SecurityUtils.requireRole(UserRole.LECTURER);
        AuthDtos.MeResponse me = authService.me();
        model.addAttribute("user", me);
        model.addAttribute("stats", statsService.lecturerStats(null));
        model.addAttribute("courses", courseService.listCourses());
        model.addAttribute("assignableCourses", courseService.listAssignableCourses(null));
        model.addAttribute("departments", departmentRepository.findAll());
        model.addAttribute("sessions", sessionService.listMySessions());
        courseService.findFocusedCourse(me.userId()).ifPresent(course ->
                model.addAttribute("focusedCourseCode", course.getCourseCode()));
        return "lecturer";
    }

    @PostMapping("/lecturer/courses/assign")
    public String webAssignCourse(
            @RequestParam Long courseId,
            RedirectAttributes redirectAttributes) {
        try {
            CourseDtos.CourseResponse course = courseService.assignCourseToLecturer(courseId);
            redirectAttributes.addFlashAttribute("success", "Assigned to " + course.courseCode() + " successfully.");
            return "redirect:/lecturer";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/lecturer";
        }
    }

    @PostMapping("/lecturer/courses/{id}/focus")
    public String webFocusCourse(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes) {
        try {
            CourseDtos.CourseResponse course = courseService.setFocusedCourse(id);
            redirectAttributes.addFlashAttribute("success", course.courseCode() + " is now your active course.");
            return "redirect:/lecturer";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/lecturer";
        }
    }

    @GetMapping("/enroll/{token}")
    public String enrollPage(@PathVariable String token, Model model) {
        CourseDtos.CourseResponse course = courseService.getPublicCourseSummary(token);
        model.addAttribute("course", course);
        model.addAttribute("token", token);
        model.addAttribute("departmentCode", course.departmentCode());
        return "enroll";
    }

    @PostMapping("/enroll/{token}")
    public String enrollSubmit(
            @PathVariable String token,
            @RequestParam String name,
            @RequestParam String email,
            @RequestParam String indexNumber,
            @RequestParam String departmentCode,
            @RequestParam String password,
            HttpServletResponse response,
            RedirectAttributes redirectAttributes) {
        try {
            AuthDtos.AuthResponse authRes = authService.registerStudentForCourse(
                    token,
                    new AuthDtos.StudentRegisterRequest(name, email, indexNumber, departmentCode, password)
            );
            setTokenCookie(response, authRes.token());
            redirectAttributes.addFlashAttribute("success", "Welcome! You are now enrolled in the course.");
            return "redirect:/student";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage() != null ? e.getMessage() : "Enrollment failed");
            return "redirect:/enroll/" + token;
        }
    }

    @PostMapping("/lecturer/sessions/create")
    public String webCreateSession(
            @RequestParam Long courseId,
            @RequestParam(required = false) SessionType sessionType,
            RedirectAttributes redirectAttributes) {
        try {
            SessionDtos.SessionResponse session = sessionService.createSession(new SessionDtos.CreateSessionRequest(courseId, sessionType));
            return "redirect:/lecturer/session/" + session.id();
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/lecturer";
        }
    }

    @GetMapping("/lecturer/course/{id}")
    public String lecturerCourseDetails(@PathVariable Long id, Model model) {
        SecurityUtils.requireRole(UserRole.LECTURER);
        AuthDtos.MeResponse me = authService.me();
        CourseDtos.CourseDetailResponse detail = courseService.getCourseDetail(id);
        model.addAttribute("user", me);
        model.addAttribute("course", detail.course());
        model.addAttribute("roster", detail.roster());
        model.addAttribute("enrollmentUrl", detail.enrollmentUrl());
        return "lecturer-course";
    }

    @GetMapping("/lecturer/session/{id}")
    public String lecturerSessionDetails(@PathVariable Long id, Model model) {
        SecurityUtils.requireRole(UserRole.LECTURER);
        AuthDtos.MeResponse me = authService.me();
        SessionDtos.SessionResponse session = sessionService.getSession(id);
        SessionDtos.SessionAttendanceResponse attendance = sessionService.getAttendance(id);
        model.addAttribute("user", me);
        // Must not be named "session" — Thymeleaf reserves ${session} for HttpSession.
        model.addAttribute("attendanceSession", session);
        model.addAttribute("attendance", attendance);
        return "lecturer-session";
    }

    @PostMapping("/lecturer/sessions/{id}/close")
    public String webCloseSession(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            sessionService.closeSession(id);
            redirectAttributes.addFlashAttribute("success", "Session closed successfully.");
            return "redirect:/lecturer";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/lecturer";
        }
    }

    // ── Helper Methods ──────────────────────────────────────────────

    private void setTokenCookie(HttpServletResponse response, String token) {
        Cookie cookie = new Cookie("token", token);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(480 * 60); // 8 hours
        response.addCookie(cookie);
    }

    private void clearTokenCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie("token", "");
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }

    private static String scanErrorMessage(ApiException e) {
        return switch (e.getErrorCode()) {
            case "INVALID_INDEX" -> "Index number does not match your account.";
            case "ALREADY_MARKED" -> "You are already marked present for this session.";
            case "DEVICE_ALREADY_USED" -> "This device has already been used to mark attendance in this session.";
            case "QR_EXPIRED" -> "This QR code has expired. Ask your lecturer to refresh it.";
            case "NOT_ENROLLED" -> "You are not enrolled in this course.";
            case "QR_CONSUMED" -> "This QR code has already been used.";
            case "SESSION_CLOSED" -> "This attendance session has ended.";
            default -> e.getMessage();
        };
    }
}
