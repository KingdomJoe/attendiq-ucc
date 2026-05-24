package com.ucc.attendance.controller;

import com.ucc.attendance.dto.AttendanceDtos;
import com.ucc.attendance.service.AttendanceService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/attendance")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService attendanceService;

    @PostMapping("/scan")
    @PreAuthorize("hasRole('STUDENT')")
    public AttendanceDtos.ScanResponse scan(
            @Valid @RequestBody AttendanceDtos.ScanRequest request,
            HttpServletRequest httpRequest) {
        return attendanceService.scan(request, httpRequest);
    }

    @GetMapping("/history")
    @PreAuthorize("hasRole('STUDENT')")
    public List<AttendanceDtos.HistoryItem> history() {
        return attendanceService.history();
    }
}
