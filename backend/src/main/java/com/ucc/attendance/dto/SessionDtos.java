package com.ucc.attendance.dto;

import com.ucc.attendance.domain.SessionStatus;
import com.ucc.attendance.domain.SessionType;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.List;

public final class SessionDtos {

    private SessionDtos() {}

    public record CreateSessionRequest(
            @NotNull Long courseId,
            SessionType sessionType
    ) {}

    public record SessionResponse(
            Long id,
            Long courseId,
            String courseCode,
            String courseName,
            SessionStatus status,
            SessionType sessionType,
            Instant createdAt,
            long presentCount
    ) {}

    public record QrResponse(
            String token,
            String qrImageBase64,
            Instant expiresAt
    ) {}

    public record AttendanceRow(
            Long studentId,
            String name,
            String indexNumber,
            boolean present,
            Instant attendanceTime
    ) {}

    public record SessionAttendanceResponse(
            Long sessionId,
            String courseCode,
            List<AttendanceRow> rows,
            long presentCount,
            long enrolledCount
    ) {}
}
