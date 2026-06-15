package com.ucc.attendance.desktop.controller;

import com.ucc.attendance.desktop.ApiClient;
import com.ucc.attendance.desktop.App;
import com.ucc.attendance.desktop.util.FxUtils;
import com.ucc.attendance.desktop.util.TableColumns;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Live presenter view: rotating QR code display and real-time attendance roster polling.
 */
public class SessionController {

    @FXML private Label courseLabel;
    @FXML private Label typeLabel;
    @FXML private Label refreshLabel;
    @FXML private Label manualCodeLabel;
    @FXML private Label attendanceCountLabel;
    @FXML private ImageView qrImageView;
    @FXML private Button backButton;
    @FXML private Button fullscreenQrButton;
    @FXML private Button closeSessionButton;

    @FXML private TableView<ApiClient.AttendanceRow> attendanceTable;
    @FXML private TableColumn<ApiClient.AttendanceRow, String> indexCol;
    @FXML private TableColumn<ApiClient.AttendanceRow, String> nameCol;
    @FXML private TableColumn<ApiClient.AttendanceRow, String> timeCol;

    private Long sessionId;
    private Timeline qrTimeline;
    private Timeline attendanceTimeline;
    private int countdownSeconds = 5;
    private Stage fullscreenStage;
    private volatile boolean qrPollErrorAlerted;
    private volatile boolean rosterPollErrorAlerted;

    @FXML
    public void initialize() {
        TableColumns.text(indexCol, ApiClient.AttendanceRow::indexNumber);
        TableColumns.text(nameCol, ApiClient.AttendanceRow::name);

        timeCol.setCellValueFactory(cellData -> {
            if (cellData.getValue() == null || cellData.getValue().attendanceTime() == null) {
                return new javafx.beans.property.SimpleStringProperty("-");
            }
            Instant time = cellData.getValue().attendanceTime();
            String formatted = DateTimeFormatter.ofPattern("HH:mm:ss")
                    .withZone(ZoneId.systemDefault())
                    .format(time);
            return new javafx.beans.property.SimpleStringProperty(formatted);
        });

        attendanceTable.getStyleClass().add("structured-table");
        attendanceTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        attendanceTable.setPlaceholder(new Label("Waiting for check-ins…"));

        backButton.setOnAction(e -> handleBack());
        fullscreenQrButton.setOnAction(e -> openQrFullscreen());
        closeSessionButton.setOnAction(e -> handleClose());
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
                    courseLabel.setText(s.courseCode() + " — " + s.courseName());
                    typeLabel.setText("ACTIVE " + s.sessionType() + " SESSION");
                });
            } catch (Exception e) {
                Platform.runLater(() -> FxUtils.showError("Session Load Error", "Could not load session info:\n" + e.getMessage()));
            }
        }).start();
    }

    private void startPolling() {
        qrTimeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            countdownSeconds--;
            if (countdownSeconds <= 0) {
                refreshQrCode();
                countdownSeconds = 5;
            }
            refreshLabel.setText("Refreshing in " + countdownSeconds + "s…");
        }));
        qrTimeline.setCycleCount(Timeline.INDEFINITE);
        qrTimeline.play();

        refreshQrCode();

        attendanceTimeline = new Timeline(new KeyFrame(Duration.seconds(2), event -> refreshAttendanceRoster()));
        attendanceTimeline.setCycleCount(Timeline.INDEFINITE);
        attendanceTimeline.play();

        refreshAttendanceRoster();
    }

    private void stopPolling() {
        if (qrTimeline != null) {
            qrTimeline.stop();
        }
        if (attendanceTimeline != null) {
            attendanceTimeline.stop();
        }
        closeQrFullscreen();
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
                        qrPollErrorAlerted = false;
                    } catch (Exception ex) {
                        if (!qrPollErrorAlerted) {
                            qrPollErrorAlerted = true;
                            FxUtils.showError("QR Display Error", "Could not render QR image:\n" + ex.getMessage());
                        }
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    if (!qrPollErrorAlerted) {
                        qrPollErrorAlerted = true;
                        FxUtils.showError("QR Refresh Error", "Failed to fetch session QR code:\n" + e.getMessage());
                    }
                });
            }
        }).start();
    }

    private void refreshAttendanceRoster() {
        new Thread(() -> {
            try {
                ApiClient.SessionAttendanceResponse att = ApiClient.getSessionAttendance(sessionId);
                Platform.runLater(() -> {
                    attendanceCountLabel.setText(att.presentCount() + " / " + att.enrolledCount());

                    List<ApiClient.AttendanceRow> checkedInList = att.rows().stream()
                            .filter(ApiClient.AttendanceRow::present)
                            .sorted((r1, r2) -> {
                                if (r1.attendanceTime() == null) return 1;
                                if (r2.attendanceTime() == null) return -1;
                                return r2.attendanceTime().compareTo(r1.attendanceTime());
                            })
                            .collect(Collectors.toList());

                    attendanceTable.setItems(FXCollections.observableArrayList(checkedInList));
                    rosterPollErrorAlerted = false;
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    if (!rosterPollErrorAlerted) {
                        rosterPollErrorAlerted = true;
                        FxUtils.showError("Attendance Sync Error", "Failed to refresh attendance roster:\n" + e.getMessage());
                    }
                });
            }
        }).start();
    }

    private void openQrFullscreen() {
        if (qrImageView.getImage() == null) {
            FxUtils.showError("QR Not Ready", "Wait for the QR code to load, then try again.");
            return;
        }
        if (fullscreenStage != null && fullscreenStage.isShowing()) {
            fullscreenStage.toFront();
            return;
        }

        ImageView bigQr = new ImageView();
        bigQr.imageProperty().bind(qrImageView.imageProperty());
        bigQr.setPreserveRatio(true);
        bigQr.getStyleClass().add("qr-fullscreen-image");

        Label hint = new Label("Press ESC or click anywhere to exit full screen");
        hint.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 14px;");

        Label courseHint = new Label();
        courseHint.textProperty().bind(courseLabel.textProperty());
        courseHint.setStyle("-fx-text-fill: #f8fafc; -fx-font-size: 20px; -fx-font-weight: bold;");
        courseHint.setWrapText(true);
        courseHint.setMaxWidth(900);

        VBox box = new VBox(24, courseHint, bigQr, hint);
        box.setAlignment(Pos.CENTER);
        box.getStyleClass().add("qr-stage");
        box.setMaxWidth(Double.MAX_VALUE);
        box.setMaxHeight(Double.MAX_VALUE);

        StackPane root = new StackPane(box);
        root.getStyleClass().add("qr-fullscreen-root");
        root.setOnMouseClicked(e -> {
            if (e.getTarget() == root) {
                closeQrFullscreen();
            }
        });

        Scene scene = new Scene(root);
        scene.getStylesheets().add(App.class.getResource("/css/style.css").toExternalForm());
        scene.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                closeQrFullscreen();
            }
        });

        fullscreenStage = new Stage();
        fullscreenStage.initOwner(App.getPrimaryStage());
        fullscreenStage.initModality(Modality.NONE);
        fullscreenStage.setTitle("AttendIQ — QR Code");
        fullscreenStage.setScene(scene);

        Rectangle2D bounds = Screen.getPrimary().getVisualBounds();
        fullscreenStage.setX(bounds.getMinX());
        fullscreenStage.setY(bounds.getMinY());
        fullscreenStage.setWidth(bounds.getWidth());
        fullscreenStage.setHeight(bounds.getHeight());

        fullscreenStage.setOnShown(e -> {
            double size = Math.min(scene.getWidth(), scene.getHeight()) - 160;
            bigQr.setFitWidth(Math.max(size, 320));
        });

        fullscreenStage.setOnCloseRequest(e -> fullscreenStage = null);
        fullscreenStage.show();
    }

    private void closeQrFullscreen() {
        if (fullscreenStage != null) {
            fullscreenStage.close();
            fullscreenStage = null;
        }
    }

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
                    startPolling();
                });
            }
        }).start();
    }

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
