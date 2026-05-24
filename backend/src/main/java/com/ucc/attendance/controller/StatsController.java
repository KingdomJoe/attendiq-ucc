package com.ucc.attendance.controller;

import com.ucc.attendance.dto.StatsDtos;
import com.ucc.attendance.service.StatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class StatsController {

    private final StatsService statsService;

    @GetMapping("/lecturer/stats")
    @PreAuthorize("hasRole('LECTURER')")
    public StatsDtos.LecturerStats lecturerStats(@RequestParam(required = false) Long sessionId) {
        return statsService.lecturerStats(sessionId);
    }

    @GetMapping("/student/stats")
    @PreAuthorize("hasRole('STUDENT')")
    public StatsDtos.StudentStats studentStats() {
        return statsService.studentStats();
    }

    @GetMapping("/lecturer/analytics")
    @PreAuthorize("hasRole('LECTURER')")
    public StatsDtos.LecturerAnalyticsResponse analytics() {
        return statsService.lecturerAnalytics();
    }
}
