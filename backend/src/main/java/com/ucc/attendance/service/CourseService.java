package com.ucc.attendance.service;

import com.ucc.attendance.domain.*;
import com.ucc.attendance.dto.CourseDtos;
import com.ucc.attendance.exception.ApiException;
import com.ucc.attendance.repository.*;
import com.ucc.attendance.security.UserPrincipal;
import com.ucc.attendance.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@SuppressWarnings("null")
public class CourseService {

    private final CourseRepository courseRepository;
    private final DepartmentRepository departmentRepository;
    private final LecturerRepository lecturerRepository;
    private final LecturerCourseRepository lecturerCourseRepository;
    private final StudentRepository studentRepository;
    private final AttendanceSessionRepository sessionRepository;
    private final AttendanceRecordRepository attendanceRecordRepository;
    private final SessionService sessionService;

    @Value("${app.public-base-url:http://localhost:8080}")
    private String publicBaseUrl;

    public List<CourseDtos.CourseResponse> listCourses() {
        UserPrincipal user = SecurityUtils.currentUser();
        return switch (user.getRole()) {
            case LECTURER -> lecturerCourseRepository.findByLecturerIdWithCourse(user.getUserId()).stream()
                    .map(link -> toResponse(link.getCourse(), link.isActive()))
                    .toList();
            case STUDENT -> courseRepository.findByStudentId(user.getUserId()).stream()
                    .map(course -> toResponse(course, true))
                    .toList();
        };
    }

    public List<CourseDtos.CourseResponse> listAssignableCourses(String departmentCode) {
        UserPrincipal lecturer = SecurityUtils.requireRole(UserRole.LECTURER);
        Set<Long> assignedIds = lecturerCourseRepository.findByLecturerIdWithCourse(lecturer.getUserId()).stream()
                .map(link -> link.getCourse().getId())
                .collect(Collectors.toSet());

        List<Course> pool = departmentCode == null || departmentCode.isBlank()
                ? courseRepository.findAllOrdered()
                : courseRepository.findByDepartmentCodeOrderByCourseCode(departmentCode.trim());

        return pool.stream()
                .filter(course -> !assignedIds.contains(course.getId()))
                .map(course -> toResponse(course, false))
                .toList();
    }

    @Transactional
    public CourseDtos.CourseResponse createCourse(CourseDtos.CreateCourseRequest req) {
        UserPrincipal lecturer = SecurityUtils.requireRole(UserRole.LECTURER);
        if (courseRepository.findByCourseCodeIgnoreCase(req.courseCode()).isPresent()) {
            throw new ApiException("COURSE_EXISTS", "Course code already exists", HttpStatus.CONFLICT);
        }
        Department dept = departmentRepository.findByCodeIgnoreCase(req.departmentCode())
                .orElseThrow(() -> new ApiException("INVALID_DEPARTMENT", "Department not found", HttpStatus.BAD_REQUEST));

        Course course = Course.builder()
                .courseCode(req.courseCode().toUpperCase())
                .courseName(req.courseName())
                .department(dept)
                .build();
        course = courseRepository.save(course);

        Lecturer l = lecturerRepository.findById(lecturer.getUserId())
                .orElseThrow(() -> new ApiException("NOT_FOUND", "Lecturer not found", HttpStatus.NOT_FOUND));
        boolean firstAssignment = l.getCourseLinks().isEmpty();
        if (firstAssignment) {
            deactivateAllCourses(l);
        }
        l.linkCourse(course, firstAssignment);
        lecturerRepository.save(l);

        return toResponse(course, firstAssignment);
    }

    @Transactional
    public CourseDtos.CourseResponse assignCourseToLecturer(Long courseId) {
        UserPrincipal lecturerPrincipal = SecurityUtils.requireRole(UserRole.LECTURER);
        Lecturer lecturer = lecturerRepository.findById(lecturerPrincipal.getUserId())
                .orElseThrow(() -> new ApiException("NOT_FOUND", "Lecturer not found", HttpStatus.NOT_FOUND));
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ApiException("NOT_FOUND", "Course not found", HttpStatus.NOT_FOUND));

        if (lecturer.hasCourse(course)) {
            throw new ApiException("ALREADY_ASSIGNED", "You are already assigned to this course", HttpStatus.CONFLICT);
        }

        boolean firstAssignment = lecturer.getCourseLinks().isEmpty();
        if (firstAssignment) {
            deactivateAllCourses(lecturer);
        }
        lecturer.linkCourse(course, firstAssignment);
        lecturerRepository.save(lecturer);

        return toResponse(course, firstAssignment);
    }

    @Transactional
    public CourseDtos.CourseResponse setFocusedCourse(Long courseId) {
        UserPrincipal lecturerPrincipal = SecurityUtils.requireRole(UserRole.LECTURER);
        Lecturer lecturer = lecturerRepository.findById(lecturerPrincipal.getUserId())
                .orElseThrow(() -> new ApiException("NOT_FOUND", "Lecturer not found", HttpStatus.NOT_FOUND));

        LecturerCourse link = lecturerCourseRepository.findByLecturerIdAndCourseId(lecturer.getId(), courseId)
                .orElseThrow(() -> new ApiException("FORBIDDEN", "Course not assigned to you", HttpStatus.FORBIDDEN));

        deactivateAllCourses(lecturer);
        link.setActive(true);
        lecturerRepository.save(lecturer);

        return toResponse(link.getCourse(), true);
    }

    @Transactional
    public void joinCourse(String courseCode) {
        UserPrincipal studentPrincipal = SecurityUtils.requireRole(UserRole.STUDENT);
        Student student = studentRepository.findById(studentPrincipal.getUserId())
                .orElseThrow(() -> new ApiException("NOT_FOUND", "Student not found", HttpStatus.NOT_FOUND));
        Course course = courseRepository.findByCourseCodeIgnoreCase(courseCode.trim())
                .orElseThrow(() -> new ApiException("NOT_FOUND", "Course with code '" + courseCode + "' not found", HttpStatus.NOT_FOUND));
        enrollStudent(student, course);
    }

    @Transactional
    public void leaveCourse(Long courseId) {
        UserPrincipal studentPrincipal = SecurityUtils.requireRole(UserRole.STUDENT);
        Student student = studentRepository.findById(studentPrincipal.getUserId())
                .orElseThrow(() -> new ApiException("NOT_FOUND", "Student not found", HttpStatus.NOT_FOUND));
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ApiException("NOT_FOUND", "Course not found", HttpStatus.NOT_FOUND));

        boolean removed = student.getCourses().removeIf(c -> c.getId().equals(course.getId()));
        if (!removed) {
            throw new ApiException("NOT_ENROLLED", "You are not enrolled in this course", HttpStatus.BAD_REQUEST);
        }
        studentRepository.save(student);
    }

    @Transactional
    public void joinCourseByToken(String enrollmentToken) {
        UserPrincipal studentPrincipal = SecurityUtils.requireRole(UserRole.STUDENT);
        Student student = studentRepository.findById(studentPrincipal.getUserId())
                .orElseThrow(() -> new ApiException("NOT_FOUND", "Student not found", HttpStatus.NOT_FOUND));
        Course course = courseRepository.findByEnrollmentToken(enrollmentToken.trim())
                .orElseThrow(() -> new ApiException("NOT_FOUND", "Invalid enrollment link", HttpStatus.NOT_FOUND));
        enrollStudent(student, course);
    }

    public Course getCourseByEnrollmentToken(String enrollmentToken) {
        return courseRepository.findByEnrollmentToken(enrollmentToken.trim())
                .orElseThrow(() -> new ApiException("NOT_FOUND", "Invalid enrollment link", HttpStatus.NOT_FOUND));
    }

    public CourseDtos.CourseResponse getPublicCourseSummary(String enrollmentToken) {
        return toResponse(getCourseByEnrollmentToken(enrollmentToken), false);
    }

    public List<CourseDtos.CourseResponse> listAvailableCourses() {
        UserPrincipal studentPrincipal = SecurityUtils.requireRole(UserRole.STUDENT);
        List<Course> enrolled = courseRepository.findByStudentId(studentPrincipal.getUserId());
        Set<Long> enrolledIds = enrolled.stream().map(Course::getId).collect(Collectors.toSet());
        return courseRepository.findAllOrdered().stream()
                .filter(c -> !enrolledIds.contains(c.getId()))
                .map(course -> toResponse(course, false))
                .toList();
    }

    public CourseDtos.CourseDetailResponse getCourseDetail(Long courseId) {
        UserPrincipal user = SecurityUtils.currentUser();
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ApiException("NOT_FOUND", "Course not found", HttpStatus.NOT_FOUND));

        if (user.getRole() == UserRole.LECTURER) {
            Lecturer l = lecturerRepository.findById(user.getUserId())
                    .orElseThrow(() -> new ApiException("NOT_FOUND", "Lecturer not found", HttpStatus.NOT_FOUND));
            if (!l.hasCourse(course)) {
                throw new ApiException("FORBIDDEN", "Course not assigned to lecturer", HttpStatus.FORBIDDEN);
            }
            sessionService.reconcileStaleActiveSessions(user.getUserId());
        } else if (user.getRole() == UserRole.STUDENT) {
            Student s = studentRepository.findById(user.getUserId())
                    .orElseThrow(() -> new ApiException("NOT_FOUND", "Student not found", HttpStatus.NOT_FOUND));
            if (s.getCourses().stream().noneMatch(c -> c.getId().equals(course.getId()))) {
                throw new ApiException("FORBIDDEN", "You are not enrolled in this course", HttpStatus.FORBIDDEN);
            }
        }

        List<Student> students = studentRepository.findByCourseId(courseId);
        List<CourseDtos.StudentResponse> roster = students.stream()
                .map(s -> new CourseDtos.StudentResponse(s.getId(), s.getName(), s.getEmail(), s.getIndexNumber()))
                .sorted(Comparator.comparing(CourseDtos.StudentResponse::name, String.CASE_INSENSITIVE_ORDER))
                .toList();

        List<AttendanceSession> sessions = sessionRepository.findByCourseIdOrderByCreatedAtDesc(courseId);
        List<com.ucc.attendance.dto.SessionDtos.SessionResponse> sessionResponses = sessions.stream()
                .map(sessionService::toSessionResponse)
                .toList();

        boolean active = user.getRole() == UserRole.LECTURER
                && lecturerCourseRepository.findByLecturerIdAndCourseId(user.getUserId(), courseId)
                .map(LecturerCourse::isActive)
                .orElse(false);

        return new CourseDtos.CourseDetailResponse(
                toResponse(course, active),
                roster,
                sessionResponses,
                buildEnrollmentUrl(course)
        );
    }

    public CourseDtos.StudentAttendanceGrid getStudentAttendanceGrid(Long courseId, Long studentId) {
        UserPrincipal user = SecurityUtils.currentUser();
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ApiException("NOT_FOUND", "Course not found", HttpStatus.NOT_FOUND));

        if (user.getRole() == UserRole.LECTURER) {
            Lecturer lecturer = lecturerRepository.findById(user.getUserId())
                    .orElseThrow(() -> new ApiException("NOT_FOUND", "Lecturer not found", HttpStatus.NOT_FOUND));
            if (!lecturer.hasCourse(course)) {
                throw new ApiException("FORBIDDEN", "Course not assigned to lecturer", HttpStatus.FORBIDDEN);
            }
        } else if (user.getRole() == UserRole.STUDENT && !user.getUserId().equals(studentId)) {
            throw new ApiException("FORBIDDEN", "You can only view your own attendance grid", HttpStatus.FORBIDDEN);
        }

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ApiException("NOT_FOUND", "Student not found", HttpStatus.NOT_FOUND));

        Set<Long> attendedSessionIds = attendanceRecordRepository.findByStudentIdOrderByAttendanceTimeDesc(studentId).stream()
                .filter(record -> record.getSession().getCourse().getId().equals(courseId))
                .map(record -> record.getSession().getId())
                .collect(Collectors.toSet());

        List<CourseDtos.AttendanceCell> cells = sessionRepository.findByCourseIdOrderByCreatedAtDesc(courseId).stream()
                .filter(session -> session.getStatus() == SessionStatus.CLOSED || session.getStatus() == SessionStatus.ACTIVE)
                .sorted(Comparator.comparing(AttendanceSession::getCreatedAt))
                .map(session -> new CourseDtos.AttendanceCell(
                        session.getId(),
                        session.getCreatedAt(),
                        attendedSessionIds.contains(session.getId())
                ))
                .toList();

        long presentCount = cells.stream().filter(CourseDtos.AttendanceCell::present).count();
        return new CourseDtos.StudentAttendanceGrid(
                studentId,
                student.getName(),
                cells,
                presentCount,
                cells.size()
        );
    }

    public Optional<Course> findFocusedCourse(Long lecturerId) {
        return lecturerCourseRepository.findActiveByLecturerId(lecturerId).map(LecturerCourse::getCourse);
    }

    public String buildEnrollmentUrl(Course course) {
        return publicBaseUrl + "/enroll/" + course.getEnrollmentToken();
    }

    private void enrollStudent(Student student, Course course) {
        if (student.getCourses().stream().anyMatch(c -> c.getId().equals(course.getId()))) {
            throw new ApiException("ALREADY_ENROLLED", "You are already enrolled in this course", HttpStatus.CONFLICT);
        }
        student.getCourses().add(course);
        studentRepository.save(student);
    }

    private void deactivateAllCourses(Lecturer lecturer) {
        lecturer.getCourseLinks().forEach(link -> link.setActive(false));
    }

    private CourseDtos.CourseResponse toResponse(Course course, boolean active) {
        return new CourseDtos.CourseResponse(
                course.getId(),
                course.getCourseCode(),
                course.getCourseName(),
                course.getDepartment().getCode(),
                course.getDepartment().getName(),
                active
        );
    }
}
