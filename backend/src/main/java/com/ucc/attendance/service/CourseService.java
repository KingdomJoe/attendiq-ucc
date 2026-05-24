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
