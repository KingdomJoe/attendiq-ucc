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
}
