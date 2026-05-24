package com.ucc.attendance.controller;

import com.ucc.attendance.dto.SessionDtos;
import com.ucc.attendance.service.ExportService;
import com.ucc.attendance.service.SessionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/sessions")
@RequiredArgsConstructor
@PreAuthorize("hasRole('LECTURER')")
public class SessionController {

    private final SessionService sessionService;
    private final ExportService exportService;

    @GetMapping
    public List<SessionDtos.SessionResponse> list() {
        return sessionService.listMySessions();
    }

    @PostMapping
    public SessionDtos.SessionResponse create(@Valid @RequestBody SessionDtos.CreateSessionRequest request) {
        return sessionService.createSession(request);
    }

    @GetMapping("/{id}")
    public SessionDtos.SessionResponse get(@PathVariable Long id) {
        return sessionService.getSession(id);
    }

    @GetMapping("/{id}/qr")
    public SessionDtos.QrResponse qr(@PathVariable Long id) {
        return sessionService.issueQr(id);
    }

    @PostMapping("/{id}/close")
    public SessionDtos.SessionResponse close(@PathVariable Long id) {
        return sessionService.closeSession(id);
    }

    @GetMapping("/{id}/attendance")
    public SessionDtos.SessionAttendanceResponse attendance(@PathVariable Long id) {
        return sessionService.getAttendance(id);
    }

    @GetMapping("/{id}/attendance/export")
    public ResponseEntity<byte[]> exportAttendance(@PathVariable Long id) {
        byte[] csv = exportService.exportSessionAttendanceCsv(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=attendance-session-" + id + ".csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv);
    }
}
