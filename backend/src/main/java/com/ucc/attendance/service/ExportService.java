package com.ucc.attendance.service;

import com.ucc.attendance.dto.SessionDtos;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class ExportService {

    private final SessionService sessionService;
    private static final DateTimeFormatter TIME_FMT =
            DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault());

    public byte[] exportSessionAttendanceCsv(Long sessionId) {
        SessionDtos.SessionAttendanceResponse data = sessionService.getAttendance(sessionId);
        StringBuilder sb = new StringBuilder();
        sb.append("Name,Index Number,Status,Time\n");
        for (SessionDtos.AttendanceRow row : data.rows()) {
            String status = row.present() ? "Present" : "Absent";
            String time = row.attendanceTime() != null ? TIME_FMT.format(row.attendanceTime()) : "";
            sb.append(csvEscape(row.name())).append(',')
                    .append(csvEscape(row.indexNumber())).append(',')
                    .append(status).append(',')
                    .append(time).append('\n');
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static String csvEscape(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
