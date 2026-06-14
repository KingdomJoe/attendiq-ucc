package com.ucc.attendance.desktop.controller;

import com.ucc.attendance.desktop.ApiClient;
import com.ucc.attendance.desktop.SessionManager;
import com.ucc.attendance.desktop.App;
import com.ucc.attendance.desktop.util.FxUtils;
import com.ucc.attendance.desktop.util.TableCells;
import com.ucc.attendance.desktop.util.TableColumns;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class StudentCourseDetailController {

    @FXML private Label studentNameLabel;
    @FXML private Label studentIndexLabel;

    @FXML private Label courseHeaderLabel;
    @FXML private Label departmentLabel;

    @FXML private Label courseSessionsLabel;
    @FXML private Label attendedLabel;
    @FXML private Label missedLabel;
    @FXML private Label rateLabel;

    @FXML private TableView<StudentSessionHistoryRow> courseHistoryTable;
    @FXML private TableColumn<StudentSessionHistoryRow, String> dateCol;
    @FXML private TableColumn<StudentSessionHistoryRow, String> statusCol;

    @FXML private TableView<ApiClient.StudentResponse> classmatesTable;
    @FXML private TableColumn<ApiClient.StudentResponse, String> nameCol;
    @FXML private TableColumn<ApiClient.StudentResponse, String> indexCol;

    private Long courseId;

    public void setCourseId(Long courseId) {
        this.courseId = courseId;
        loadCourseDetailData();
    }

    @FXML
    public void initialize() {
        studentNameLabel.setText(SessionManager.getDisplayName());
        studentIndexLabel.setText(SessionManager.getIdentifier());

        // Setup classmates columns
        TableColumns.text(nameCol, ApiClient.StudentResponse::name);
        TableColumns.text(indexCol, ApiClient.StudentResponse::indexNumber);

        dateCol.setCellValueFactory(cellData -> {
            if (cellData.getValue() == null || cellData.getValue().date() == null) {
                return new javafx.beans.property.SimpleStringProperty("");
            }
            Instant time = cellData.getValue().date();
            String formatted = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
                    .withZone(ZoneId.systemDefault())
                    .format(time);
            return new javafx.beans.property.SimpleStringProperty(formatted);
        });
        TableColumns.text(statusCol, StudentSessionHistoryRow::status);
        TableCells.statusChip(statusCol);
    }

    private void loadCourseDetailData() {
        if (courseId == null) return;

        new Thread(() -> {
            try {
                ApiClient.CourseDetailResponse detail = ApiClient.getCourseDetail(courseId);
                List<ApiClient.HistoryItem> history = ApiClient.getStudentHistory();

                Platform.runLater(() -> {
                    // Update header
                    courseHeaderLabel.setText(detail.course().courseCode() + " — " + detail.course().courseName());
                    departmentLabel.setText("Department: " + detail.course().departmentName());

                    // Populate Classmates
                    classmatesTable.setItems(FXCollections.observableArrayList(detail.roster()));

                    // Filter student's history for this specific course
                    Set<Long> attendedSessionIds = history.stream()
                            .filter(h -> h.courseCode().equalsIgnoreCase(detail.course().courseCode()))
                            .map(ApiClient.HistoryItem::sessionId)
                            .collect(Collectors.toSet());

                    // Map all sessions of this course to check presence
                    List<StudentSessionHistoryRow> rows = new ArrayList<>();
                    long attendedCount = 0;
                    for (ApiClient.SessionResponse s : detail.sessions()) {
                        boolean present = attendedSessionIds.contains(s.id());
                        if (present) {
                            attendedCount++;
                        }
                        rows.add(new StudentSessionHistoryRow(s.createdAt(), present ? "Present" : "Absent"));
                    }

                    courseHistoryTable.setItems(FXCollections.observableArrayList(rows));

                    // Calculate stats
                    long totalSessions = detail.sessions().size();
                    long missedCount = Math.max(0, totalSessions - attendedCount);
                    int rate = totalSessions == 0 ? 0 : (int) Math.round((attendedCount * 100.0) / totalSessions);

                    courseSessionsLabel.setText(String.valueOf(totalSessions));
                    attendedLabel.setText(String.valueOf(attendedCount));
                    missedLabel.setText(String.valueOf(missedCount));
                    rateLabel.setText(rate + "%");
                });
            } catch (Exception e) {
                Platform.runLater(() -> FxUtils.showError("Load Error", "Failed to load course details:\n" + e.getMessage()));
            }
        }).start();
    }

    @FXML
    private void handleDashboardNav() {
        App.showDashboard();
    }

    @FXML
    private void handleHistoryNav() {
        App.navigateTo("student-history.fxml", "Attendance History");
    }

    @FXML
    private void handleLogout() {
        SessionManager.clearSession();
        App.showLogin();
    }

    // Row helper representation
    public record StudentSessionHistoryRow(
            Instant date,
            String status
    ) {}
}
