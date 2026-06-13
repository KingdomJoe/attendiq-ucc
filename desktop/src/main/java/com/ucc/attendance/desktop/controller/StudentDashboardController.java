package com.ucc.attendance.desktop.controller;

import com.ucc.attendance.desktop.ApiClient;
import com.ucc.attendance.desktop.SessionManager;
import com.ucc.attendance.desktop.App;
import com.ucc.attendance.desktop.util.FxUtils;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.stage.Stage;

import java.util.List;

public class StudentDashboardController {

    @FXML private Label studentNameLabel;
    @FXML private Label studentIndexLabel;
    @FXML private Label welcomeLabel;

    @FXML private Label totalSessionsLabel;
    @FXML private Label attendedLabel;
    @FXML private Label missedLabel;
    @FXML private Label rateLabel;

    @FXML private ListView<String> enrolledCoursesListView;
    @FXML private ComboBox<String> availableCoursesComboBox;
    @FXML private TextField manualCodeField;
    @FXML private Button joinCourseButton;

    private List<ApiClient.CourseResponse> myCourses;
    private List<ApiClient.CourseResponse> availableCourses;

    @FXML
    public void initialize() {
        studentNameLabel.setText(SessionManager.getDisplayName());
        studentIndexLabel.setText(SessionManager.getIdentifier());
        welcomeLabel.setText("Welcome, " + SessionManager.getDisplayName());

        // Add double click handler toListView to view course details
        enrolledCoursesListView.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
                int selectedIdx = enrolledCoursesListView.getSelectionModel().getSelectedIndex();
                if (selectedIdx >= 0 && selectedIdx < myCourses.size()) {
                    ApiClient.CourseResponse selectedCourse = myCourses.get(selectedIdx);
                    viewCourseDetails(selectedCourse);
                }
            }
        });

        loadStudentDashboardData();
    }

    private void loadStudentDashboardData() {
        new Thread(() -> {
            try {
                ApiClient.StudentStats stats = ApiClient.getStudentStats();
                List<ApiClient.CourseResponse> courses = ApiClient.getCourses();
                List<ApiClient.CourseResponse> availCourses = ApiClient.getAvailableCourses();

                Platform.runLater(() -> {
                    myCourses = courses;
                    availableCourses = availCourses;

                    // Update stats labels
                    totalSessionsLabel.setText(String.valueOf(stats.totalSessions()));
                    attendedLabel.setText(String.valueOf(stats.attended()));
                    missedLabel.setText(String.valueOf(stats.missed()));
                    rateLabel.setText(stats.ratePercent() + "%");

                    // Populate my courses
                    enrolledCoursesListView.getItems().clear();
                    for (ApiClient.CourseResponse c : courses) {
                        enrolledCoursesListView.getItems().add(c.courseCode() + " — " + c.courseName());
                    }

                    // Populate available courses combobox
                    availableCoursesComboBox.getItems().clear();
                    for (ApiClient.CourseResponse c : availCourses) {
                        availableCoursesComboBox.getItems().add(c.courseCode() + " — " + c.courseName());
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> FxUtils.showError("Load Error", "Failed to load student dashboard:\n" + e.getMessage()));
            }
        }).start();
    }

    @FXML
    private void handleJoinCourse() {
        String courseCode = "";
        
        // Manual field takes preference
        if (!manualCodeField.getText().trim().isEmpty()) {
            courseCode = manualCodeField.getText().trim();
        } else {
            int selectedIdx = availableCoursesComboBox.getSelectionModel().getSelectedIndex();
            if (selectedIdx >= 0) {
                courseCode = availableCourses.get(selectedIdx).courseCode();
            }
        }

        if (courseCode.isEmpty()) {
            FxUtils.showError("Input Error", "Please enter a course code or choose from the dropdown.");
            return;
        }

        final String codeToJoin = courseCode;
        joinCourseButton.setDisable(true);

        new Thread(() -> {
            try {
                ApiClient.joinCourse(codeToJoin);
                Platform.runLater(() -> {
                    joinCourseButton.setDisable(false);
                    manualCodeField.clear();
                    FxUtils.showInfo("Joined Course", "Successfully enrolled in " + codeToJoin);
                    loadStudentDashboardData();
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    joinCourseButton.setDisable(false);
                    FxUtils.showError("Enrollment Error", "Failed to join course:\n" + e.getMessage());
                });
            }
        }).start();
    }

    private void viewCourseDetails(ApiClient.CourseResponse course) {
        // Open StudentCourseDetail view (to be implemented)
        try {
            javafx.fxml.FXMLLoader loader = App.getLoader("student-course-detail.fxml");
            javafx.scene.Scene scene = new javafx.scene.Scene(loader.load(), 1000, 700);
            scene.getStylesheets().add(App.class.getResource("/css/style.css").toExternalForm());
            
            StudentCourseDetailController controller = loader.getController();
            controller.setCourseId(course.id());

            Stage stage = App.getPrimaryStage();
            stage.setScene(scene);
            stage.setTitle("AttendIQ — " + course.courseCode() + " Details");
        } catch (Exception e) {
            e.printStackTrace();
            FxUtils.showError("Navigation Error", "Could not load course details:\n" + e.getMessage());
        }
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
}
