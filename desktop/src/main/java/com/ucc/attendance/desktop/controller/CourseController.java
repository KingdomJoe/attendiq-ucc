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

import java.util.List;

public class CourseController {

    @FXML private Label lecturerLabel;
    
    // Create Course Form
    @FXML private TextField codeField;
    @FXML private TextField nameField;
    @FXML private TextField deptCodeField;

    // Table view
    @FXML private TableView<ApiClient.CourseResponse> coursesTable;
    @FXML private TableColumn<ApiClient.CourseResponse, String> codeCol;
    @FXML private TableColumn<ApiClient.CourseResponse, String> nameCol;
    @FXML private TableColumn<ApiClient.CourseResponse, String> deptCol;

    @FXML
    public void initialize() {
        lecturerLabel.setText(SessionManager.getLecturerName());

        codeCol.setCellValueFactory(new PropertyValueFactory<>("courseCode"));
        nameCol.setCellValueFactory(new PropertyValueFactory<>("courseName"));
        deptCol.setCellValueFactory(new PropertyValueFactory<>("departmentName"));

        coursesTable.setRowFactory(tv -> {
            TableRow<ApiClient.CourseResponse> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (!row.isEmpty())) {
                    ApiClient.CourseResponse rowData = row.getItem();
                    openCourseDetail(rowData);
                }
            });
            return row;
        });

        loadCourses();
    }

    private void loadCourses() {
        new Thread(() -> {
            try {
                List<ApiClient.CourseResponse> courses = ApiClient.getCourses();
                Platform.runLater(() -> coursesTable.setItems(FXCollections.observableArrayList(courses)));
            } catch (Exception e) {
                Platform.runLater(() -> FxUtils.showError("Load Error", "Could not load courses:\n" + e.getMessage()));
            }
        }).start();
    }

    @FXML
    private void handleCreateCourse() {
        String code = codeField.getText().trim();
        String name = nameField.getText().trim();
        String deptCode = deptCodeField.getText().trim();

        if (code.isEmpty() || name.isEmpty() || deptCode.isEmpty()) {
            FxUtils.showError("Input Error", "Please fill in all fields to create a course.");
            return;
        }

        new Thread(() -> {
            try {
                ApiClient.createCourse(code, name, deptCode);
                Platform.runLater(() -> {
                    FxUtils.showInfo("Success", "Course created and assigned successfully.");
                    codeField.clear();
                    nameField.clear();
                    deptCodeField.clear();
                    loadCourses();
                });
            } catch (Exception e) {
                Platform.runLater(() -> FxUtils.showError("Error", "Could not create course:\n" + e.getMessage()));
            }
        }).start();
    }

    private void openCourseDetail(ApiClient.CourseResponse course) {
        try {
            FXMLLoader loader = App.getLoader("course-detail.fxml");
            Scene scene = new Scene(loader.load(), 1000, 700);
            scene.getStylesheets().add(App.class.getResource("/css/style.css").toExternalForm());
            
            CourseDetailController controller = loader.getController();
            controller.setCourseId(course.id());

            Stage stage = App.getPrimaryStage();
            stage.setScene(scene);
            stage.setTitle("AttendIQ — Course Details");
        } catch (Exception e) {
            e.printStackTrace();
            FxUtils.showError("Navigation Error", "Could not open course details:\n" + e.getMessage());
        }
    }

    @FXML
    private void handleDashboardNav() {
        App.navigateTo("dashboard.fxml", "Dashboard");
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
