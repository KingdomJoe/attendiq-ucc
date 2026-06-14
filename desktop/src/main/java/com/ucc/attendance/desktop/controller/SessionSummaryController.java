package com.ucc.attendance.desktop.controller;

import com.ucc.attendance.desktop.ApiClient;
import com.ucc.attendance.desktop.App;
import com.ucc.attendance.desktop.SessionManager;
import com.ucc.attendance.desktop.util.CsvExportHelper;
import com.ucc.attendance.desktop.util.FxUtils;
import com.ucc.attendance.desktop.util.TableColumns;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class SessionSummaryController {

    @FXML private Label lecturerLabel;
    @FXML private Label sessionMetaLabel;
    @FXML private Label enrolledCountLabel;
    @FXML private Label presentCountLabel;
    @FXML private Label attendanceRateLabel;

    @FXML private TableView<ApiClient.AttendanceRow> rosterTable;
    @FXML private TableColumn<ApiClient.AttendanceRow, String> indexCol;
    @FXML private TableColumn<ApiClient.AttendanceRow, String> nameCol;
    @FXML private TableColumn<ApiClient.AttendanceRow, Boolean> statusCol;
    @FXML private TableColumn<ApiClient.AttendanceRow, String> timeCol;

    private Long sessionId;
    private String courseCode;

    @FXML
    public void initialize() {
        lecturerLabel.setText(SessionManager.getLecturerName());

        TableColumns.text(indexCol, ApiClient.AttendanceRow::indexNumber);
        TableColumns.text(nameCol, ApiClient.AttendanceRow::name);

        statusCol.setCellValueFactory(cellData -> {
            ApiClient.AttendanceRow row = cellData.getValue();
            if (row == null) {
                return new javafx.beans.property.SimpleObjectProperty<>(null);
            }
            return new javafx.beans.property.SimpleObjectProperty<>(row.present());
        });
        statusCol.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Boolean present, boolean empty) {
                super.updateItem(present, empty);
                if (empty || present == null) {
                    setText(null);
                    setStyle(null);
                } else {
                    if (present) {
                        setText("Present");
                        setStyle("-fx-text-fill: #10b981; -fx-font-weight: bold;"); // Green
                    } else {
                        setText("Absent");
                        setStyle("-fx-text-fill: #ef4444; -fx-font-weight: bold;"); // Red
                    }
                }
            }
        });

        timeCol.setCellValueFactory(cellData -> {
            if (cellData.getValue() == null || cellData.getValue().attendanceTime() == null) {
                return new javafx.beans.property.SimpleStringProperty("-");
            }
            Instant time = cellData.getValue().attendanceTime();
            String formatted = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                    .withZone(ZoneId.systemDefault())
                    .format(time);
            return new javafx.beans.property.SimpleStringProperty(formatted);
        });
    }

    public void setSessionId(Long sessionId) {
        this.sessionId = sessionId;
        loadSummaryData();
    }

    private void loadSummaryData() {
        new Thread(() -> {
            try {
                ApiClient.SessionResponse s = ApiClient.getSession(sessionId);
                ApiClient.SessionAttendanceResponse att = ApiClient.getSessionAttendance(sessionId);

                Platform.runLater(() -> {
                    courseCode = s.courseCode();
                    String formattedDate = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
                            .withZone(ZoneId.systemDefault())
                            .format(s.createdAt());
                    sessionMetaLabel.setText(s.courseCode() + " - " + s.courseName() + " | " + formattedDate + " (" + s.sessionType() + ")");

                    enrolledCountLabel.setText(String.valueOf(att.enrolledCount()));
                    presentCountLabel.setText(String.valueOf(att.presentCount()));
                    
                    long enrolled = att.enrolledCount();
                    long present = att.presentCount();
                    int rate = enrolled == 0 ? 0 : (int) Math.round((present * 100.0) / enrolled);
                    attendanceRateLabel.setText(rate + "%");

                    rosterTable.setItems(FXCollections.observableArrayList(att.rows()));
                });
            } catch (Exception e) {
                Platform.runLater(() -> FxUtils.showError("Load Error", "Could not load session summary:\n" + e.getMessage()));
            }
        }).start();
    }

    @FXML
    private void handleExportCsv() {
        CsvExportHelper.exportSessionCsv(sessionId, courseCode, App.getPrimaryStage());
    }

    @FXML
    private void handleBack() {
        App.showDashboard();
    }

    @FXML
    private void handleCoursesNav() {
        App.navigateTo("courses.fxml", "Courses");
    }

    @FXML
    private void handleAnalyticsNav() {
        App.navigateTo("analytics.fxml", "Analytics");
    }

    @FXML
    private void handleLogout() {
        SessionManager.clearSession();
        App.showLogin();
    }
}
