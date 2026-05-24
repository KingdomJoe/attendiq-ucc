package com.ucc.attendance.dto;

import java.util.List;

public final class StatsDtos {

    private StatsDtos() {}

    public record LecturerStats(
            long enrolled,
            long present,
            long absent,
            int ratePercent,
            Long sessionId,
            String courseCode
    ) {}

    public record StudentStats(
            long totalSessions,
            long attended,
            long missed,
            int ratePercent
    ) {}

    public record CourseAnalytics(
            String courseCode,
            String courseName,
            long sessionsHeld,
            long totalPresent,
            long totalEnrolledSlots,
            int averageRatePercent
    ) {}

    public record LecturerAnalyticsResponse(List<CourseAnalytics> courses) {}
}
