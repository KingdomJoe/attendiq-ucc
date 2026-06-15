package com.ucc.attendance.desktop.controller;

import com.ucc.attendance.desktop.ApiClient;
import com.ucc.attendance.desktop.SessionManager;
import com.ucc.attendance.desktop.App;
import com.ucc.attendance.desktop.util.CsvExportHelper;
import com.ucc.attendance.desktop.util.FxUtils;
import com.ucc.attendance.desktop.util.TableCells;
import com.ucc.attendance.desktop.util.TableColumns;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Lecturer home screen: stats, start session, and searchable recent sessions table.
 */
public class DashboardController {

    @FXML private Label lecturerLabel;
    @FXML private Label welcomeLabel;
    @FXML private Label totalStudentsLabel;
    @FXML private Label activeSessionsLabel;
    @FXML private Label avgAttendanceLabel;

    @FXML private ComboBox<String> courseComboBox;
    @FXML private ComboBox<String> sessionTypeComboBox;
    @FXML private Button startSessionButton;

    // Active session card fields
    @FXML private VBox activeSessionCard;
    @FXML private Label activeSessionCourseLabel;

    // Table fields
    @FXML private TextField searchSessionsField;
    @FXML private TableView<ApiClient.SessionResponse> sessionsTable;
    @FXML private TableColumn<ApiClient.SessionResponse, String> courseCodeCol;
    @FXML private TableColumn<ApiClient.SessionResponse, String> courseNameCol;
    @FXML private TableColumn<ApiClient.SessionResponse, String> dateCol;
    @FXML private TableColumn<ApiClient.SessionResponse, String> typeCol;
    @FXML private TableColumn<ApiClient.SessionResponse, Long> presentCol;
    @FXML private TableColumn<ApiClient.SessionResponse, String> statusCol;
    @FXML private TableColumn<ApiClient.SessionResponse, ApiClient.SessionResponse> exportCol;

    private List<ApiClient.CourseResponse> coursesList;
    private ApiClient.SessionResponse activeSession;
    private ObservableList<ApiClient.SessionResponse> allSessions = FXCollections.observableArrayList();
    private FilteredList<ApiClient.SessionResponse> filteredSessions;

    @FXML
    public void initialize() {
        lecturerLabel.setText(SessionManager.getLecturerName());
        welcomeLabel.setText("Welcome, " + SessionManager.getLecturerName());

        sessionTypeComboBox.setItems(FXCollections.observableArrayList("LECTURE", "LAB"));
        sessionTypeComboBox.setValue("LECTURE");

        // Record accessors — PropertyValueFactory does not work with Java records
        TableColumns.text(courseCodeCol, ApiClient.SessionResponse::courseCode);
        TableColumns.text(courseNameCol, ApiClient.SessionResponse::courseName);
        dateCol.setCellValueFactory(cellData -> {
            if (cellData.getValue() == null || cellData.getValue().createdAt() == null) {
                return new javafx.beans.property.SimpleStringProperty("");
            }
            Instant createdAt = cellData.getValue().createdAt();
            String formatted = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
                    .withZone(ZoneId.systemDefault())
                    .format(createdAt);
            return new javafx.beans.property.SimpleStringProperty(formatted);
        });
        TableColumns.text(typeCol, s -> s.sessionType() != null ? s.sessionType() : "");
        TableColumns.numberLong(presentCol, ApiClient.SessionResponse::presentCount);
        TableColumns.text(statusCol, s -> s.status() != null ? s.status() : "");
        TableCells.statusChip(statusCol);

        exportCol.setSortable(false);
        exportCol.setResizable(false);
        exportCol.setMinWidth(92);
        exportCol.setPrefWidth(92);
        exportCol.setMaxWidth(92);
        exportCol.setCellValueFactory(param -> new ReadOnlyObjectWrapper<>(param.getValue()));
        exportCol.setCellFactory(col -> new TableCell<>() {
            private final Hyperlink exportLink = new Hyperlink("CSV");
            {
                exportLink.getStyleClass().add("table-action-link");
                exportLink.setOnAction(e -> {
                    ApiClient.SessionResponse session = getItem();
                    if (session != null) {
                        CsvExportHelper.exportSessionCsv(session.id(), session.courseCode(), App.getPrimaryStage());
                    }
                });
            }

            @Override
            protected void updateItem(ApiClient.SessionResponse session, boolean empty) {
                super.updateItem(session, empty);
                if (empty || session == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }
                setText(null);
                setGraphic(exportLink);
            }
        });

        sessionsTable.getStyleClass().addAll("structured-table", "sessions-table");
        sessionsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_SUBSEQUENT_COLUMNS);
        sessionsTable.setPlaceholder(new Label("No sessions yet. Start a new session above."));

        // Add double click handler to table row to view summary
        sessionsTable.setRowFactory(tv -> {
            TableRow<ApiClient.SessionResponse> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (!row.isEmpty())) {
                    ApiClient.SessionResponse rowData = row.getItem();
                    viewSessionSummary(rowData);
                }
            });
            return row;
        });

        // Hide active session card initially
        activeSessionCard.setVisible(false);

        filteredSessions = new FilteredList<>(allSessions, session -> true);
        sessionsTable.setItems(filteredSessions);
        searchSessionsField.textProperty().addListener((obs, oldVal, newVal) -> applySessionFilter(newVal));

        loadDashboardData();
    }

    private void applySessionFilter(String query) {
        String q = query == null ? "" : query.trim().toLowerCase();
        filteredSessions.setPredicate(session -> {
            if (q.isEmpty()) {
                return true;
            }
            String date = session.createdAt() != null
                    ? DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault()).format(session.createdAt())
                    : "";
            return session.courseCode().toLowerCase().contains(q)
                    || session.courseName().toLowerCase().contains(q)
                    || session.sessionType().toLowerCase().contains(q)
                    || session.status().toLowerCase().contains(q)
                    || date.toLowerCase().contains(q);
        });
    }

    private void loadDashboardData() {
        new Thread(() -> {
            try {
                // Fetch stats, courses, and sessions in parallel
                ApiClient.LecturerStats stats = ApiClient.getLecturerStats(null);
                List<ApiClient.CourseResponse> courses = ApiClient.getCourses();
                List<ApiClient.SessionResponse> sessions = ApiClient.getSessions();

                Platform.runLater(() -> {
                    coursesList = courses;
                    courseComboBox.getItems().clear();
                    for (ApiClient.CourseResponse c : courses) {
                        String label = c.courseCode() + " — " + c.courseName();
                        if (c.active()) {
                            label += " (active)";
                        }
                        courseComboBox.getItems().add(label);
                    }

                    totalStudentsLabel.setText(String.valueOf(stats.enrolled()));
                    activeSessionsLabel.setText(String.valueOf(stats.sessionId() != null ? 1 : 0));
                    avgAttendanceLabel.setText(stats.ratePercent() + "%");

                    allSessions.setAll(sessions);

                    // Check for active session
                    activeSession = null;
                    for (ApiClient.SessionResponse s : sessions) {
                        if ("ACTIVE".equalsIgnoreCase(s.status())) {
                            activeSession = s;
                            break;
                        }
                    }

                    if (activeSession != null) {
                        activeSessionCourseLabel.setText(activeSession.courseCode() + " - " + activeSession.courseName());
                        activeSessionCard.setVisible(true);
                        startSessionButton.setDisable(true);
                    } else {
                        activeSessionCard.setVisible(false);
                        startSessionButton.setDisable(false);
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> FxUtils.showError("Load Error", "Failed to load dashboard data:\n" + e.getMessage()));
            }
        }).start();
    }

    @FXML
    private void handleStartSession() {
        int courseIdx = courseComboBox.getSelectionModel().getSelectedIndex();
        if (courseIdx < 0) {
            FxUtils.showError("Input Error", "Please select a course to start a session.");
            return;
        }

        ApiClient.CourseResponse selectedCourse = coursesList.get(courseIdx);
        String sessionType = sessionTypeComboBox.getValue();

        startSessionButton.setDisable(true);

        new Thread(() -> {
            try {
                ApiClient.SessionResponse session = ApiClient.createSession(selectedCourse.id(), sessionType);
                Platform.runLater(() -> {
                    startSessionButton.setDisable(false);
                    openSessionPresenter(session.id());
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    startSessionButton.setDisable(false);
                    FxUtils.showError("Session Error", "Could not start session:\n" + e.getMessage());
                });
            }
        }).start();
    }

    @FXML
    private void handleOpenActiveSession() {
        if (activeSession != null) {
            openSessionPresenter(activeSession.id());
        }
    }

    @FXML
    private void handleCloseActiveSession() {
        if (activeSession == null) return;
        if (!FxUtils.showConfirmation("Confirm Close", "Are you sure you want to close this attendance session?")) {
            return;
        }

        new Thread(() -> {
            try {
                ApiClient.closeSession(activeSession.id());
                Platform.runLater(() -> {
                    FxUtils.showInfo("Session Closed", "The attendance session has been closed.");
                    loadDashboardData();
                });
            } catch (Exception e) {
                Platform.runLater(() -> FxUtils.showError("Session Error", "Could not close session:\n" + e.getMessage()));
            }
        }).start();
    }

    private void openSessionPresenter(Long sessionId) {
        try {
            FXMLLoader loader = App.getLoader("session.fxml");
            Scene scene = new Scene(loader.load(), 1000, 700);
            scene.getStylesheets().add(App.class.getResource("/css/style.css").toExternalForm());
            
            SessionController controller = loader.getController();
            controller.setSessionId(sessionId);

            Stage stage = App.getPrimaryStage();
            stage.setScene(scene);
            stage.setTitle("AttendIQ — Live Presenter View");
        } catch (Exception e) {
            e.printStackTrace();
            FxUtils.showError("Navigation Error", "Could not open presenter view:\n" + e.getMessage());
        }
    }

    private void viewSessionSummary(ApiClient.SessionResponse session) {
        if ("ACTIVE".equalsIgnoreCase(session.status())) {
            openSessionPresenter(session.id());
            return;
        }
        try {
            FXMLLoader loader = App.getLoader("session-summary.fxml");
            Scene scene = new Scene(loader.load(), 1000, 700);
            scene.getStylesheets().add(App.class.getResource("/css/style.css").toExternalForm());
            
            SessionSummaryController controller = loader.getController();
            controller.setSessionId(session.id());

            Stage stage = App.getPrimaryStage();
            stage.setScene(scene);
            stage.setTitle("AttendIQ — Session Summary");
        } catch (Exception e) {
            e.printStackTrace();
            FxUtils.showError("Navigation Error", "Could not open session summary:\n" + e.getMessage());
        }
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
    private void handleProfile() {
        ProfileController.show(() -> {
            if (lecturerLabel != null) {
                lecturerLabel.setText(SessionManager.getDisplayName());
            }
            if (welcomeLabel != null) {
                welcomeLabel.setText("Welcome, " + SessionManager.getDisplayName());
            }
        });
    }

    @FXML
    private void handleLogout() {
        SessionManager.clearSession();
        App.showLogin();
    }
}
