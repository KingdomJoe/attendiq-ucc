package com.ucc.attendance.desktop.controller;

import com.ucc.attendance.desktop.ApiClient;
import com.ucc.attendance.desktop.App;
import com.ucc.attendance.desktop.util.FxUtils;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

public class SessionController {

    @FXML private Label courseLabel;
    @FXML private Label typeLabel;
    @FXML private Label refreshLabel;
    @FXML private Label manualCodeLabel;
    @FXML private Label attendanceCountLabel;
    @FXML private ImageView qrImageView;

    @FXML private TableView<ApiClient.AttendanceRow> attendanceTable;
    @FXML private TableColumn<ApiClient.AttendanceRow, String> indexCol;
    @FXML private TableColumn<ApiClient.AttendanceRow, String> nameCol;
    @FXML private TableColumn<ApiClient.AttendanceRow, String> timeCol;

    private Long sessionId;
    private Timeline qrTimeline;
    private Timeline attendanceTimeline;
    private int countdownSeconds = 5;

    @FXML
    public void initialize() {
        indexCol.setCellValueFactory(new PropertyValueFactory<>("indexNumber"));
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));

        timeCol.setCellValueFactory(cellData -> {
            Instant time = cellData.getValue().attendanceTime();
            if (time == null) return new javafx.beans.property.SimpleStringProperty("-");
            String formatted = DateTimeFormatter.ofPattern("HH:mm:ss")
                    .withZone(ZoneId.systemDefault())
                    .format(time);
            return new javafx.beans.property.SimpleStringProperty(formatted);
        });
    }

    public void setSessionId(Long sessionId) {
        this.sessionId = sessionId;
        loadSessionDetails();
        startPolling();
    }

    private void loadSessionDetails() {
        new Thread(() -> {
            try {
                ApiClient.SessionResponse s = ApiClient.getSession(sessionId);
                Platform.runLater(() -> {
                    courseLabel.setText(s.courseCode() + " - " + s.courseName());
                    typeLabel.setText("ACTIVE " + s.sessionType() + " SESSION");
                });
            } catch (Exception e) {
                Platform.runLater(() -> FxUtils.showError("Session Load Error", "Could not load session info:\n" + e.getMessage()));
            }
        }).start();
    }

    private void startPolling() {
        // Timeline for fetching QR code and manual fallback code every 5 seconds
        qrTimeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            countdownSeconds--;
            if (countdownSeconds <= 0) {
                refreshQrCode();
                countdownSeconds = 5;
            }
            refreshLabel.setText("Refreshing in " + countdownSeconds + "s...");
        }));
        qrTimeline.setCycleCount(Timeline.INDEFINITE);
        qrTimeline.play();

        // Initial fetch of QR code immediately
        refreshQrCode();

        // Timeline for polling attendance list every 3 seconds
        attendanceTimeline = new Timeline(new KeyFrame(Duration.seconds(3), event -> refreshAttendanceRoster()));
        attendanceTimeline.setCycleCount(Timeline.INDEFINITE);
        attendanceTimeline.play();

        // Initial fetch of attendance roster immediately
        refreshAttendanceRoster();
    }

    private void stopPolling() {
        if (qrTimeline != null) {
            qrTimeline.stop();
        }
        if (attendanceTimeline != null) {
            attendanceTimeline.stop();
        }
    }

    private void refreshQrCode() {
        new Thread(() -> {
            try {
                ApiClient.QrResponse qr = ApiClient.getSessionQr(sessionId);
                Platform.runLater(() -> {
                    manualCodeLabel.setText(qr.token());
                    try {
                        byte[] imageBytes = Base64.getDecoder().decode(qr.qrImageBase64());
                        Image image = new Image(new ByteArrayInputStream(imageBytes));
                        qrImageView.setImage(image);
                    } catch (Exception ex) {
                        System.err.println("QR image decode failed: " + ex.getMessage());
                    }
                });
            } catch (Exception e) {
                System.err.println("Failed to fetch session QR code: " + e.getMessage());
            }
        }).start();
    }

    private void refreshAttendanceRoster() {
        new Thread(() -> {
            try {
                ApiClient.SessionAttendanceResponse att = ApiClient.getSessionAttendance(sessionId);
                Platform.runLater(() -> {
                    attendanceCountLabel.setText(att.presentCount() + " / " + att.enrolledCount());
                    
                    // Show only students who are checked-in (present == true)
                    List<ApiClient.AttendanceRow> checkedInList = att.rows().stream()
                            .filter(ApiClient.AttendanceRow::present)
                            .sorted((r1, r2) -> {
                                if (r1.attendanceTime() == null) return 1;
                                if (r2.attendanceTime() == null) return -1;
                                return r2.attendanceTime().compareTo(r1.attendanceTime()); // Show newest check-ins first
                            })
                            .collect(Collectors.toList());

                    attendanceTable.setItems(FXCollections.observableArrayList(checkedInList));
                });
            } catch (Exception e) {
                System.err.println("Failed to fetch attendance roster: " + e.getMessage());
            }
        }).start();
    }

    @FXML
    private void handleClose() {
        if (!FxUtils.showConfirmation("Close Session", "Are you sure you want to close this session? Students will no longer be able to check in.")) {
            return;
        }

        stopPolling();
        new Thread(() -> {
            try {
                ApiClient.closeSession(sessionId);
                Platform.runLater(() -> {
                    FxUtils.showInfo("Session Closed", "Attendance session is now closed.");
                    viewSessionSummary();
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    FxUtils.showError("Close Error", "Could not close session:\n" + e.getMessage());
                    startPolling(); // resume polling if failed
                });
            }
        }).start();
    }

    @FXML
    private void handleBack() {
        stopPolling();
        App.showDashboard();
    }

    private void viewSessionSummary() {
        try {
            FXMLLoader loader = App.getLoader("session-summary.fxml");
            Scene scene = new Scene(loader.load(), 1000, 700);
            scene.getStylesheets().add(App.class.getResource("/css/style.css").toExternalForm());
            
            SessionSummaryController controller = loader.getController();
            controller.setSessionId(sessionId);

            Stage stage = App.getPrimaryStage();
            stage.setScene(scene);
            stage.setTitle("AttendIQ — Session Summary");
        } catch (Exception e) {
            e.printStackTrace();
            FxUtils.showError("Navigation Error", "Could not navigate to session summary:\n" + e.getMessage());
        }
    }
}
