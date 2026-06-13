package com.ucc.attendance.desktop.controller;

import com.ucc.attendance.desktop.ApiClient;
import com.ucc.attendance.desktop.App;
import com.ucc.attendance.desktop.SessionManager;
import com.ucc.attendance.desktop.util.FxUtils;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class CourseDetailController {

    @FXML private Label lecturerLabel;
    @FXML private Label courseLabel;
    @FXML private Label deptLabel;

    // Students table
    @FXML private TableView<ApiClient.StudentResponse> studentsTable;
    @FXML private TableColumn<ApiClient.StudentResponse, String> studentIndexCol;
    @FXML private TableColumn<ApiClient.StudentResponse, String> studentNameCol;

    // Sessions table
    @FXML private TableView<ApiClient.SessionResponse> sessionsTable;
    @FXML private TableColumn<ApiClient.SessionResponse, String> sessionDateCol;
    @FXML private TableColumn<ApiClient.SessionResponse, String> sessionTypeCol;
    @FXML private TableColumn<ApiClient.SessionResponse, Long> sessionPresentCol;
    @FXML private TableColumn<ApiClient.SessionResponse, String> sessionStatusCol;

    private Long courseId;

    @FXML
    public void initialize() {
        lecturerLabel.setText(SessionManager.getLecturerName());

        // Setup student roster table columns
        studentIndexCol.setCellValueFactory(new PropertyValueFactory<>("indexNumber"));
        studentNameCol.setCellValueFactory(new PropertyValueFactory<>("name"));

        // Setup session history table columns
        sessionDateCol.setCellValueFactory(cellData -> {
            Instant createdAt = cellData.getValue().createdAt();
            String formatted = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
                    .withZone(ZoneId.systemDefault())
                    .format(createdAt);
            return new javafx.beans.property.SimpleStringProperty(formatted);
        });
        sessionTypeCol.setCellValueFactory(new PropertyValueFactory<>("sessionType"));
        sessionPresentCol.setCellValueFactory(new PropertyValueFactory<>("presentCount"));
        sessionStatusCol.setCellValueFactory(new PropertyValueFactory<>("status"));

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
    }

    public void setCourseId(Long courseId) {
        this.courseId = courseId;
        loadCourseDetail();
    }

    private void loadCourseDetail() {
        new Thread(() -> {
            try {
                ApiClient.CourseDetailResponse res = ApiClient.getCourseDetail(courseId);
                Platform.runLater(() -> {
                    courseLabel.setText(res.course().courseCode() + " - " + res.course().courseName());
                    deptLabel.setText(res.course().departmentCode() + " | " + res.course().departmentName());

                    studentsTable.setItems(FXCollections.observableArrayList(res.roster()));
                    sessionsTable.setItems(FXCollections.observableArrayList(res.sessions()));
                });
            } catch (Exception e) {
                Platform.runLater(() -> FxUtils.showError("Load Error", "Could not load course details:\n" + e.getMessage()));
            }
        }).start();
    }

    private void viewSessionSummary(ApiClient.SessionResponse session) {
        if ("ACTIVE".equalsIgnoreCase(session.status())) {
            try {
                FXMLLoader loader = App.getLoader("session.fxml");
                Scene scene = new Scene(loader.load(), 1000, 700);
                scene.getStylesheets().add(App.class.getResource("/css/style.css").toExternalForm());
                
                SessionController controller = loader.getController();
                controller.setSessionId(session.id());

                Stage stage = App.getPrimaryStage();
                stage.setScene(scene);
                stage.setTitle("AttendIQ — Live Presenter View");
            } catch (Exception e) {
                e.printStackTrace();
                FxUtils.showError("Navigation Error", "Could not open presenter view:\n" + e.getMessage());
            }
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
    private void handleDashboardNav() {
        App.navigateTo("dashboard.fxml", "Dashboard");
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
