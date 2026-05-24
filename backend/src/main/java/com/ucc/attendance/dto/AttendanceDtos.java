package com.ucc.attendance.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.Instant;

public final class AttendanceDtos {

    private AttendanceDtos() {}

    public record ScanRequest(
            @NotBlank String token,
            @NotBlank String deviceFingerprint,
            @NotBlank String indexNumber
    ) {}

    public record ScanResponse(
            String message,
            String courseCode,
            Instant attendanceTime
    ) {}

    public record HistoryItem(
            Long sessionId,
            String courseCode,
            String courseName,
            Instant attendanceTime,
            String status
    ) {}
}
