package com.ucc.attendance.desktop.controller;

import com.ucc.attendance.desktop.ApiClient;
import com.ucc.attendance.desktop.App;
import com.ucc.attendance.desktop.SessionManager;
import com.ucc.attendance.desktop.util.FxUtils;
import com.ucc.attendance.desktop.util.TableCells;
import com.ucc.attendance.desktop.util.TableColumns;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;

/**
 * Lecturer course list with assign-course workflow and navigation to course detail.
 */
public class CourseController {

    @FXML private Label lecturerLabel;

    @FXML private ComboBox<String> departmentComboBox;
    @FXML private ComboBox<String> assignableCourseComboBox;
    @FXML private Label previewCodeLabel;
    @FXML private Label previewNameLabel;
    @FXML private Label previewDeptLabel;
    @FXML private Button assignButton;

    @FXML private TableView<ApiClient.CourseResponse> coursesTable;
    @FXML private TableColumn<ApiClient.CourseResponse, String> codeCol;
    @FXML private TableColumn<ApiClient.CourseResponse, String> nameCol;
    @FXML private TableColumn<ApiClient.CourseResponse, String> deptCol;
    @FXML private TableColumn<ApiClient.CourseResponse, String> activeCol;
    @FXML private TableColumn<ApiClient.CourseResponse, Void> actionsCol;

    private List<ApiClient.DepartmentResponse> departments = new ArrayList<>();
    private List<ApiClient.CourseResponse> assignableCourses = new ArrayList<>();

    @FXML
    public void initialize() {
        lecturerLabel.setText(SessionManager.getLecturerName());

        TableColumns.text(codeCol, ApiClient.CourseResponse::courseCode);
        TableColumns.text(nameCol, ApiClient.CourseResponse::courseName);
        TableColumns.text(deptCol, c -> c.departmentCode() + " — " + c.departmentName());
        TableColumns.text(activeCol, c -> c.active() ? "Active" : "Inactive");
        TableCells.activeBadge(activeCol, ApiClient.CourseResponse::active);

        actionsCol.setCellFactory(col -> new TableCell<>() {
            private final Button focusBtn = new Button("Set Active");
            {
                focusBtn.getStyleClass().addAll("btn", "btn-secondary");
                focusBtn.setOnAction(e -> {
                    ApiClient.CourseResponse course = getTableView().getItems().get(getIndex());
                    if (course != null && !course.active()) {
                        setFocusedCourse(course);
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                    return;
                }
                ApiClient.CourseResponse course = getTableRow().getItem();
                focusBtn.setDisable(course.active());
                HBox box = new HBox(focusBtn);
                box.setAlignment(Pos.CENTER);
                setGraphic(course.active() ? null : box);
            }
        });

        coursesTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        coursesTable.setPlaceholder(new Label("No assigned courses yet. Use Assign Course on the right."));

        coursesTable.setRowFactory(tv -> {
            TableRow<ApiClient.CourseResponse> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    openCourseDetail(row.getItem());
                }
            });
            return row;
        });

        departmentComboBox.setOnAction(e -> loadAssignableCoursesForDepartment());
        assignableCourseComboBox.setOnAction(e -> updatePreview());
        assignButton.setOnAction(e -> handleAssignCourse());

        loadDepartments();
        loadCourses();
    }

    private void loadDepartments() {
        new Thread(() -> {
            try {
                List<ApiClient.DepartmentResponse> depts = ApiClient.getDepartments();
                Platform.runLater(() -> {
                    departments = depts;
                    departmentComboBox.getItems().clear();
                    for (ApiClient.DepartmentResponse d : depts) {
                        departmentComboBox.getItems().add(d.code() + " — " + d.name());
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> FxUtils.showError("Load Error", "Could not load departments:\n" + e.getMessage()));
            }
        }).start();
    }

    private String selectedDepartmentCode() {
        String selected = departmentComboBox.getValue();
        if (selected == null || selected.isBlank()) {
            return null;
        }
        return selected.split(" — ")[0].trim();
    }

    private void loadAssignableCoursesForDepartment() {
        String deptCode = selectedDepartmentCode();
        if (deptCode == null) {
            assignableCourses = List.of();
            assignableCourseComboBox.getItems().clear();
            clearPreview();
            return;
        }

        new Thread(() -> {
            try {
                List<ApiClient.CourseResponse> courses = ApiClient.getAssignableCourses(deptCode);
                Platform.runLater(() -> {
                    assignableCourses = courses;
                    assignableCourseComboBox.getItems().clear();
                    for (ApiClient.CourseResponse c : courses) {
                        assignableCourseComboBox.getItems().add(c.courseCode() + " — " + c.courseName());
                    }
                    assignableCourseComboBox.getSelectionModel().clearSelection();
                    clearPreview();
                });
            } catch (Exception e) {
                Platform.runLater(() -> FxUtils.showError("Load Error", "Could not load assignable courses:\n" + e.getMessage()));
            }
        }).start();
    }

    private void updatePreview() {
        int idx = assignableCourseComboBox.getSelectionModel().getSelectedIndex();
        if (idx < 0 || idx >= assignableCourses.size()) {
            clearPreview();
            return;
        }
        ApiClient.CourseResponse c = assignableCourses.get(idx);
        previewCodeLabel.setText("Course code: " + c.courseCode());
        previewNameLabel.setText("Course name: " + c.courseName());
        previewDeptLabel.setText("Department: " + c.departmentCode() + " — " + c.departmentName());
    }

    private void clearPreview() {
        previewCodeLabel.setText("Course code: —");
        previewNameLabel.setText("Course name: —");
        previewDeptLabel.setText("Department: —");
    }

    private void loadCourses() {
        new Thread(() -> {
            try {
                List<ApiClient.CourseResponse> courses = ApiClient.getCourses();
                Platform.runLater(() -> coursesTable.setItems(FXCollections.observableArrayList(courses)));
            } catch (Exception e) {
                Platform.runLater(() -> FxUtils.showError("Load Error", "Could not load assigned courses:\n" + e.getMessage()));
            }
        }).start();
    }

    @FXML
    private void handleAssignCourse() {
        int idx = assignableCourseComboBox.getSelectionModel().getSelectedIndex();
        if (idx < 0) {
            FxUtils.showError("Input Error", "Select a department and course to assign.");
            return;
        }

        ApiClient.CourseResponse selected = assignableCourses.get(idx);
        assignButton.setDisable(true);

        new Thread(() -> {
            try {
                ApiClient.assignCourse(selected.id());
                Platform.runLater(() -> {
                    assignButton.setDisable(false);
                    FxUtils.showInfo("Course Assigned", selected.courseCode() + " is now on your roster.");
                    assignableCourseComboBox.getSelectionModel().clearSelection();
                    clearPreview();
                    loadAssignableCoursesForDepartment();
                    loadCourses();
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    assignButton.setDisable(false);
                    FxUtils.showError("Assign Error", "Could not assign course:\n" + e.getMessage());
                });
            }
        }).start();
    }

    private void setFocusedCourse(ApiClient.CourseResponse course) {
        new Thread(() -> {
            try {
                ApiClient.focusCourse(course.id());
                Platform.runLater(() -> {
                    FxUtils.showInfo("Active Course", course.courseCode() + " is now your focused course.");
                    loadCourses();
                });
            } catch (Exception e) {
                Platform.runLater(() -> FxUtils.showError("Focus Error", "Could not set active course:\n" + e.getMessage()));
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
