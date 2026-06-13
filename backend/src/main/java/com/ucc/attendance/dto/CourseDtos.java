package com.ucc.attendance.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

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

    public record StudentResponse(
            Long id,
            String name,
            String email,
            String indexNumber
    ) {}

    public record CourseDetailResponse(
            CourseResponse course,
            List<StudentResponse> roster,
            List<SessionDtos.SessionResponse> sessions
    ) {}
}
