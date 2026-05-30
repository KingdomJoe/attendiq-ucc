package com.ucc.attendance.service;

import com.ucc.attendance.domain.*;
import com.ucc.attendance.dto.CourseDtos;
import com.ucc.attendance.exception.ApiException;
import com.ucc.attendance.repository.*;
import com.ucc.attendance.security.UserPrincipal;
import com.ucc.attendance.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CourseService {

    private final CourseRepository courseRepository;
    private final DepartmentRepository departmentRepository;
    private final LecturerRepository lecturerRepository;
    private final StudentRepository studentRepository;

    public List<CourseDtos.CourseResponse> listCourses() {
        UserPrincipal user = SecurityUtils.currentUser();
        List<Course> courses = switch (user.getRole()) {
            case LECTURER -> courseRepository.findByLecturerId(user.getUserId());
            case STUDENT -> courseRepository.findByStudentId(user.getUserId());
        };
        if (courses.isEmpty() && user.getRole() == UserRole.LECTURER) {
            courses = courseRepository.findAllOrdered();
        }
        return courses.stream().map(this::toResponse).toList();
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
        l.getCourses().add(course);
        lecturerRepository.save(l);

        return toResponse(course);
    }

    @Transactional
    public void assignCourseToLecturer(Long courseId) {
        UserPrincipal lecturerPrincipal = SecurityUtils.requireRole(UserRole.LECTURER);
        Lecturer lecturer = lecturerRepository.findById(lecturerPrincipal.getUserId())
                .orElseThrow(() -> new ApiException("NOT_FOUND", "Lecturer not found", HttpStatus.NOT_FOUND));
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ApiException("NOT_FOUND", "Course not found", HttpStatus.NOT_FOUND));

        if (lecturer.getCourses().stream().anyMatch(c -> c.getId().equals(course.getId()))) {
            throw new ApiException("ALREADY_ASSIGNED", "You are already assigned to this course", HttpStatus.CONFLICT);
        }
        lecturer.getCourses().add(course);
        lecturerRepository.save(lecturer);
    }

    @Transactional
    public void joinCourse(String courseCode) {
        UserPrincipal studentPrincipal = SecurityUtils.requireRole(UserRole.STUDENT);
        Student student = studentRepository.findById(studentPrincipal.getUserId())
                .orElseThrow(() -> new ApiException("NOT_FOUND", "Student not found", HttpStatus.NOT_FOUND));
        Course course = courseRepository.findByCourseCodeIgnoreCase(courseCode.trim())
                .orElseThrow(() -> new ApiException("NOT_FOUND", "Course with code '" + courseCode + "' not found", HttpStatus.NOT_FOUND));

        if (student.getCourses().stream().anyMatch(c -> c.getId().equals(course.getId()))) {
            throw new ApiException("ALREADY_ENROLLED", "You are already enrolled in this course", HttpStatus.CONFLICT);
        }
        student.getCourses().add(course);
        studentRepository.save(student);
    }

    public List<CourseDtos.CourseResponse> listAvailableCourses() {
        UserPrincipal studentPrincipal = SecurityUtils.requireRole(UserRole.STUDENT);
        List<Course> enrolled = courseRepository.findByStudentId(studentPrincipal.getUserId());
        Set<Long> enrolledIds = enrolled.stream().map(Course::getId).collect(Collectors.toSet());
        return courseRepository.findAllOrdered().stream()
                .filter(c -> !enrolledIds.contains(c.getId()))
                .map(this::toResponse)
                .toList();
    }

    private CourseDtos.CourseResponse toResponse(Course c) {
        return new CourseDtos.CourseResponse(
                c.getId(),
                c.getCourseCode(),
                c.getCourseName(),
                c.getDepartment().getCode(),
                c.getDepartment().getName()
        );
    }
}

