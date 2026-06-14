package com.ucc.attendance.desktop;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

public class ApiClient {
    private static final String BASE_URL = System.getProperty("api.url", 
            System.getenv("API_URL") != null ? System.getenv("API_URL") : "http://localhost:8080");
    private static final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private static final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    // ── DTO Records for Desktop Client ───────────────────────────────────

    public record LoginRequest(String identifier, String password, String role) {}
    public record StudentRegisterRequest(String name, String email, String indexNumber, String departmentCode, String password) {}
    public record LecturerRegisterRequest(String name, String lecturerCode, String departmentCode, String password) {}
    public record AuthResponse(String token, String role, Long userId, String displayName) {}
    public record DepartmentResponse(String code, String name) {}
    
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CourseResponse(
            Long id,
            String courseCode,
            String courseName,
            String departmentCode,
            String departmentName,
            boolean active
    ) {}

    public record CreateCourseRequest(
            String courseCode,
            String courseName,
            String departmentCode
    ) {}

    public record StudentResponse(
            Long id,
            String name,
            String email,
            String indexNumber
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CourseDetailResponse(
            CourseResponse course,
            List<StudentResponse> roster,
            List<SessionResponse> sessions,
            String enrollmentUrl
    ) {}

    public record CreateSessionRequest(
            Long courseId,
            String sessionType
    ) {}

    public record SessionResponse(
            Long id,
            Long courseId,
            String courseCode,
            String courseName,
            String status,
            String sessionType,
            Instant createdAt,
            Instant closedAt,
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

    public record LecturerStats(
            long enrolled,
            long present,
            long absent,
            int ratePercent,
            Long sessionId,
            String courseCode
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

    public record StudentStats(
            long totalSessions,
            long attended,
            long missed,
            int ratePercent
    ) {}

    public record HistoryItem(
            Long sessionId,
            String courseCode,
            String courseName,
            Instant attendanceTime,
            String status
    ) {}

    // ── Helper HTTP Methods ──────────────────────────────────────────

    private static HttpRequest.Builder requestBuilder(String path) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + path))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json");
        
        String token = SessionManager.getJwtToken();
        if (token != null) {
            builder.header("Authorization", "Bearer " + token);
        }
        return builder;
    }

    private static <T> T sendRequest(HttpRequest request, Class<T> responseType) throws IOException, InterruptedException {
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            return mapper.readValue(response.body(), responseType);
        } else {
            throw new RuntimeException("API Error (HTTP " + response.statusCode() + "): " + getErrorMessage(response.body(), response.statusCode()));
        }
    }

    private static <T> T sendRequest(HttpRequest request, TypeReference<T> responseType) throws IOException, InterruptedException {
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            return mapper.readValue(response.body(), responseType);
        } else {
            throw new RuntimeException("API Error (HTTP " + response.statusCode() + "): " + getErrorMessage(response.body(), response.statusCode()));
        }
    }

    private static String getErrorMessage(String responseBody, int statusCode) {
        try {
            var node = mapper.readTree(responseBody);
            if (node.has("message")) {
                String msg = node.get("message").asText();
                if (statusCode == 409) {
                    return msg + ". Try signing in instead — use your email (students) or lecturer code, and select the correct role.";
                }
                return msg;
            }
        } catch (Exception ignored) {}
        if (statusCode == 409) {
            return responseBody + ". Try signing in instead — you may have already registered via the web app.";
        }
        return responseBody;
    }

    private static String getErrorMessage(String responseBody) {
        return getErrorMessage(responseBody, 0);
    }

    /**
     * Returns the configured API base URL (for diagnostics).
     */
    public static String getBaseUrl() {
        return BASE_URL;
    }

    /**
     * Wraps API call exceptions with user-friendly messages.
     */
    private static <T> T safeExecute(ApiCall<T> call) throws Exception {
        try {
            return call.execute();
        } catch (ConnectException e) {
            throw new RuntimeException("Cannot connect to server at " + BASE_URL + ". Make sure the backend is running.");
        } catch (java.net.http.HttpTimeoutException e) {
            throw new RuntimeException("Connection to " + BASE_URL + " timed out. Is the server running?");
        }
    }

    @FunctionalInterface
    private interface ApiCall<T> {
        T execute() throws Exception;
    }

    // ── API Methods ──────────────────────────────────────────────────

    public static AuthResponse login(String username, String password, String role) throws Exception {
        return safeExecute(() -> {
            LoginRequest req = new LoginRequest(username, password, role);
            String json = mapper.writeValueAsString(req);
            HttpRequest request = requestBuilder("/auth/login")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();
            return sendRequest(request, AuthResponse.class);
        });
    }

    public static AuthResponse registerStudent(String name, String email, String indexNumber, 
                                                String departmentCode, String password) throws Exception {
        return safeExecute(() -> {
            StudentRegisterRequest req = new StudentRegisterRequest(name, email, indexNumber, departmentCode, password);
            String json = mapper.writeValueAsString(req);
            HttpRequest request = requestBuilder("/auth/student/register")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();
            return sendRequest(request, AuthResponse.class);
        });
    }

    public static AuthResponse registerLecturer(String name, String lecturerCode, 
                                                 String departmentCode, String password) throws Exception {
        return safeExecute(() -> {
            LecturerRegisterRequest req = new LecturerRegisterRequest(name, lecturerCode, departmentCode, password);
            String json = mapper.writeValueAsString(req);
            HttpRequest request = requestBuilder("/auth/lecturer/register")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();
            return sendRequest(request, AuthResponse.class);
        });
    }

    public static List<DepartmentResponse> getDepartments() throws Exception {
        return safeExecute(() -> {
            HttpRequest request = requestBuilder("/departments")
                    .GET()
                    .build();
            return sendRequest(request, new TypeReference<List<DepartmentResponse>>() {});
        });
    }

    public static List<CourseResponse> getCourses() throws Exception {
        HttpRequest request = requestBuilder("/courses")
                .GET()
                .build();
        return sendRequest(request, new TypeReference<List<CourseResponse>>() {});
    }

    public static List<SessionResponse> getSessions() throws Exception {
        HttpRequest request = requestBuilder("/sessions")
                .GET()
                .build();
        return sendRequest(request, new TypeReference<List<SessionResponse>>() {});
    }

    public static CourseResponse createCourse(String code, String name, String deptCode) throws Exception {
        CreateCourseRequest req = new CreateCourseRequest(code, name, deptCode);
        String json = mapper.writeValueAsString(req);
        HttpRequest request = requestBuilder("/courses")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        return sendRequest(request, CourseResponse.class);
    }

    public static CourseDetailResponse getCourseDetail(Long courseId) throws Exception {
        HttpRequest request = requestBuilder("/courses/" + courseId)
                .GET()
                .build();
        return sendRequest(request, CourseDetailResponse.class);
    }

    public static SessionResponse createSession(Long courseId, String sessionType) throws Exception {
        CreateSessionRequest req = new CreateSessionRequest(courseId, sessionType);
        String json = mapper.writeValueAsString(req);
        HttpRequest request = requestBuilder("/sessions")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        return sendRequest(request, SessionResponse.class);
    }

    public static SessionResponse getSession(Long sessionId) throws Exception {
        HttpRequest request = requestBuilder("/sessions/" + sessionId)
                .GET()
                .build();
        return sendRequest(request, SessionResponse.class);
    }

    public static QrResponse getSessionQr(Long sessionId) throws Exception {
        HttpRequest request = requestBuilder("/sessions/" + sessionId + "/qr")
                .GET()
                .build();
        return sendRequest(request, QrResponse.class);
    }

    public static SessionResponse closeSession(Long sessionId) throws Exception {
        HttpRequest request = requestBuilder("/sessions/" + sessionId + "/close")
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        return sendRequest(request, SessionResponse.class);
    }

    public static SessionAttendanceResponse getSessionAttendance(Long sessionId) throws Exception {
        HttpRequest request = requestBuilder("/sessions/" + sessionId + "/attendance")
                .GET()
                .build();
        return sendRequest(request, SessionAttendanceResponse.class);
    }

    public static LecturerStats getLecturerStats(Long sessionId) throws Exception {
        String url = "/api/lecturer/stats" + (sessionId != null ? "?sessionId=" + sessionId : "");
        HttpRequest request = requestBuilder(url)
                .GET()
                .build();
        return sendRequest(request, LecturerStats.class);
    }

    public static LecturerAnalyticsResponse getLecturerAnalytics() throws Exception {
        HttpRequest request = requestBuilder("/api/lecturer/analytics")
                .GET()
                .build();
        return sendRequest(request, LecturerAnalyticsResponse.class);
    }

    public static byte[] downloadAttendanceCsv(Long sessionId) throws Exception {
        HttpRequest request = requestBuilder("/sessions/" + sessionId + "/attendance/export")
                .GET()
                .build();
        HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() == 200) {
            return response.body();
        } else {
            throw new RuntimeException("CSV export failed with HTTP " + response.statusCode());
        }
    }

    public static StudentStats getStudentStats() throws Exception {
        HttpRequest request = requestBuilder("/api/student/stats")
                .GET()
                .build();
        return sendRequest(request, StudentStats.class);
    }

    public static List<HistoryItem> getStudentHistory() throws Exception {
        HttpRequest request = requestBuilder("/attendance/history")
                .GET()
                .build();
        return sendRequest(request, new TypeReference<List<HistoryItem>>() {});
    }

    public static List<CourseResponse> getAvailableCourses() throws Exception {
        HttpRequest request = requestBuilder("/courses/available")
                .GET()
                .build();
        return sendRequest(request, new TypeReference<List<CourseResponse>>() {});
    }

    public static void joinCourse(String courseCode) throws Exception {
        String encoded = java.net.URLEncoder.encode(courseCode, java.nio.charset.StandardCharsets.UTF_8);
        HttpRequest request = requestBuilder("/courses/join?courseCode=" + encoded)
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new RuntimeException("Join course failed: " + getErrorMessage(response.body(), response.statusCode()));
        }
    }

    public static List<CourseResponse> getAssignableCourses(String departmentCode) throws Exception {
        return safeExecute(() -> {
            String path = "/courses/assignable";
            if (departmentCode != null && !departmentCode.isBlank()) {
                path += "?departmentCode=" + java.net.URLEncoder.encode(departmentCode, java.nio.charset.StandardCharsets.UTF_8);
            }
            HttpRequest request = requestBuilder(path).GET().build();
            return sendRequest(request, new TypeReference<List<CourseResponse>>() {});
        });
    }

    public static CourseResponse assignCourse(Long courseId) throws Exception {
        return safeExecute(() -> {
            HttpRequest request = requestBuilder("/courses/assign?courseId=" + courseId)
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();
            return sendRequest(request, CourseResponse.class);
        });
    }

    public static CourseResponse focusCourse(Long courseId) throws Exception {
        return safeExecute(() -> {
            HttpRequest request = requestBuilder("/courses/" + courseId + "/focus")
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();
            return sendRequest(request, CourseResponse.class);
        });
    }
}
