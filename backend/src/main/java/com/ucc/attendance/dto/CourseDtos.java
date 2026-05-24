package com.ucc.attendance.dto;

import jakarta.validation.constraints.NotBlank;

public final class CourseDtos {

    private CourseDtos() {}

    public record CourseResponse(
            Long id,
            String courseCode,
            String courseName,
            String departmentCode,
            String departmentName
    ) {}

    public record CreateCourseRequest(
            @NotBlank String courseCode,
            @NotBlank String courseName,
            @NotBlank String departmentCode
    ) {}
}
