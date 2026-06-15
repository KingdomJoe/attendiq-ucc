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

/**
 * Student home screen with stats, course enrollment, leave-course, and profile access.
 */
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
    @FXML private Button leaveCourseButton;

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

        enrolledCoursesListView.getSelectionModel().selectedIndexProperty().addListener((obs, oldIdx, newIdx) -> {
            boolean hasSelection = newIdx != null && newIdx.intValue() >= 0 && myCourses != null
                    && newIdx.intValue() < myCourses.size();
            leaveCourseButton.setDisable(!hasSelection);
        });

        loadStudentDashboardData();
    }

    private void loadStudentDashboardData() {
        new Thread(() -> {
            StringBuilder errors = new StringBuilder();

            ApiClient.StudentStats stats = null;
            try {
                stats = ApiClient.getStudentStats();
            } catch (Exception e) {
                errors.append("Stats: ").append(e.getMessage()).append("\n");
            }

            List<ApiClient.CourseResponse> courses = List.of();
            try {
                courses = ApiClient.getCourses();
            } catch (Exception e) {
                errors.append("Enrolled courses: ").append(e.getMessage()).append("\n");
            }

            List<ApiClient.CourseResponse> availCourses = List.of();
            try {
                availCourses = ApiClient.getAvailableCourses();
            } catch (Exception e) {
                errors.append("Available courses: ").append(e.getMessage()).append("\n");
            }

            final ApiClient.StudentStats finalStats = stats;
            final List<ApiClient.CourseResponse> finalCourses = courses;
            final List<ApiClient.CourseResponse> finalAvail = availCourses;
            final String errorText = errors.toString().trim();

            Platform.runLater(() -> {
                if (finalStats != null) {
                    totalSessionsLabel.setText(String.valueOf(finalStats.totalSessions()));
                    attendedLabel.setText(String.valueOf(finalStats.attended()));
                    missedLabel.setText(String.valueOf(finalStats.missed()));
                    rateLabel.setText(finalStats.ratePercent() + "%");
                }

                myCourses = finalCourses;
                availableCourses = finalAvail;

                enrolledCoursesListView.getItems().clear();
                for (ApiClient.CourseResponse c : finalCourses) {
                    enrolledCoursesListView.getItems().add(c.courseCode() + " — " + c.courseName());
                }
                if (enrolledCoursesListView.getItems().isEmpty()) {
                    enrolledCoursesListView.setPlaceholder(new Label("No enrolled courses yet. Join one on the right."));
                }

                availableCoursesComboBox.getItems().clear();
                for (ApiClient.CourseResponse c : finalAvail) {
                    availableCoursesComboBox.getItems().add(c.courseCode() + " — " + c.courseName());
                }

                if (!errorText.isEmpty()) {
                    FxUtils.showError("Load Error", "Some dashboard data could not be loaded:\n" + errorText);
                }
            });
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
    private void handleLeaveCourse() {
        int selectedIdx = enrolledCoursesListView.getSelectionModel().getSelectedIndex();
        if (selectedIdx < 0 || myCourses == null || selectedIdx >= myCourses.size()) {
            FxUtils.showError("Input Error", "Select a course to leave.");
            return;
        }
        ApiClient.CourseResponse course = myCourses.get(selectedIdx);
        if (!FxUtils.showConfirmation("Leave Course",
                "Remove " + course.courseCode() + " from your enrolled courses?")) {
            return;
        }
        leaveCourseButton.setDisable(true);
        new Thread(() -> {
            try {
                ApiClient.leaveCourse(course.id());
                Platform.runLater(() -> {
                    FxUtils.showInfo("Course Removed", course.courseCode() + " has been removed from My Courses.");
                    loadStudentDashboardData();
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    leaveCourseButton.setDisable(false);
                    FxUtils.showError("Leave Course Error", "Could not leave course:\n" + e.getMessage());
                });
            }
        }).start();
    }

    @FXML
    private void handleProfile() {
        ProfileController.show(() -> {
            studentNameLabel.setText(SessionManager.getDisplayName());
            welcomeLabel.setText("Welcome, " + SessionManager.getDisplayName());
        });
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
