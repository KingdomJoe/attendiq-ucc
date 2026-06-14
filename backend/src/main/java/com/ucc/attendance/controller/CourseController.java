package com.ucc.attendance.controller;

import com.ucc.attendance.dto.CourseDtos;
import com.ucc.attendance.service.CourseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/courses")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;

    @GetMapping
    public List<CourseDtos.CourseResponse> list() {
        return courseService.listCourses();
    }

    @PostMapping
    @PreAuthorize("hasRole('LECTURER')")
    public CourseDtos.CourseResponse create(@Valid @RequestBody CourseDtos.CreateCourseRequest request) {
        return courseService.createCourse(request);
    }

    @GetMapping("/assignable")
    @PreAuthorize("hasRole('LECTURER')")
    public List<CourseDtos.CourseResponse> listAssignable(@RequestParam(required = false) String departmentCode) {
        return courseService.listAssignableCourses(departmentCode);
    }

    @PostMapping("/assign")
    @PreAuthorize("hasRole('LECTURER')")
    public CourseDtos.CourseResponse assign(@RequestParam Long courseId) {
        return courseService.assignCourseToLecturer(courseId);
    }

    @PostMapping("/{id}/focus")
    @PreAuthorize("hasRole('LECTURER')")
    public CourseDtos.CourseResponse focus(@PathVariable Long id) {
        return courseService.setFocusedCourse(id);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('LECTURER', 'STUDENT')")
    public CourseDtos.CourseDetailResponse get(@PathVariable Long id) {
        return courseService.getCourseDetail(id);
    }

    @GetMapping("/{courseId}/students/{studentId}/attendance-grid")
    @PreAuthorize("hasAnyRole('LECTURER', 'STUDENT')")
    public CourseDtos.StudentAttendanceGrid attendanceGrid(
            @PathVariable Long courseId,
            @PathVariable Long studentId) {
        return courseService.getStudentAttendanceGrid(courseId, studentId);
    }

    @GetMapping("/available")
    @PreAuthorize("hasRole('STUDENT')")
    public List<CourseDtos.CourseResponse> listAvailable() {
        return courseService.listAvailableCourses();
    }

    @PostMapping("/join")
    @PreAuthorize("hasRole('STUDENT')")
    public void join(@RequestParam String courseCode) {
        courseService.joinCourse(courseCode);
    }

    @PostMapping("/join/{token}")
    @PreAuthorize("hasRole('STUDENT')")
    public void joinByToken(@PathVariable String token) {
        courseService.joinCourseByToken(token);
    }

    @PostMapping("/{id}/leave")
    @PreAuthorize("hasRole('STUDENT')")
    public void leave(@PathVariable Long id) {
        courseService.leaveCourse(id);
    }
}
