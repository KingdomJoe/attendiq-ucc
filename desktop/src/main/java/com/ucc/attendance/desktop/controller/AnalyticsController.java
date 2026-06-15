package com.ucc.attendance.desktop.controller;

import com.ucc.attendance.desktop.ApiClient;
import com.ucc.attendance.desktop.App;
import com.ucc.attendance.desktop.SessionManager;
import com.ucc.attendance.desktop.util.FxUtils;
import com.ucc.attendance.desktop.util.TableColumns;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;

/**
 * Per-course attendance analytics charts and filterable course breakdown table.
 */
public class AnalyticsController {

    @FXML private Label lecturerLabel;
    @FXML private Label chartSubtitleLabel;
    @FXML private Button refreshButton;
    @FXML private FlowPane courseFilterBar;

    @FXML private BarChart<String, Number> attendanceChart;
    @FXML private CategoryAxis courseAxis;
    @FXML private NumberAxis rateAxis;

    @FXML private TableView<ApiClient.CourseAnalytics> metricsTable;
    @FXML private TableColumn<ApiClient.CourseAnalytics, String> codeCol;
    @FXML private TableColumn<ApiClient.CourseAnalytics, String> nameCol;
    @FXML private TableColumn<ApiClient.CourseAnalytics, Long> sessionsHeldCol;
    @FXML private TableColumn<ApiClient.CourseAnalytics, Long> totalPresentCol;
    @FXML private TableColumn<ApiClient.CourseAnalytics, String> avgRateCol;

    private final ToggleGroup courseFilterGroup = new ToggleGroup();
    private List<ApiClient.CourseAnalytics> allCourses = List.of();
    private String selectedCourseCode = null;
    private Timeline refreshTimeline;

    @FXML
    public void initialize() {
        lecturerLabel.setText(SessionManager.getLecturerName());

        TableColumns.text(codeCol, ApiClient.CourseAnalytics::courseCode);
        TableColumns.text(nameCol, ApiClient.CourseAnalytics::courseName);
        TableColumns.numberLong(sessionsHeldCol, ApiClient.CourseAnalytics::sessionsHeld);
        TableColumns.numberLong(totalPresentCol, ApiClient.CourseAnalytics::totalPresent);
        avgRateCol.setCellValueFactory(cell -> {
            ApiClient.CourseAnalytics row = cell.getValue();
            if (row == null) {
                return new javafx.beans.property.SimpleStringProperty("");
            }
            return new javafx.beans.property.SimpleStringProperty(row.averageRatePercent() + "%");
        });

        metricsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        metricsTable.setPlaceholder(new Label("No analytics data yet. Assign courses and run sessions first."));

        refreshButton.setOnAction(e -> handleRefresh());

        attendanceChart.setAnimated(false);
        attendanceChart.setLegendVisible(false);
        attendanceChart.setCategoryGap(12);
        attendanceChart.setBarGap(6);
        attendanceChart.setMinHeight(240);

        rateAxis.setAutoRanging(false);
        rateAxis.setLowerBound(0);
        rateAxis.setUpperBound(100);
        rateAxis.setTickUnit(10);

        refreshTimeline = new Timeline(new KeyFrame(Duration.seconds(30), e -> loadAnalyticsData()));
        refreshTimeline.setCycleCount(Timeline.INDEFINITE);
        refreshTimeline.play();

        loadAnalyticsData();
    }

    private void loadAnalyticsData() {
        new Thread(() -> {
            try {
                ApiClient.LecturerAnalyticsResponse res = ApiClient.getLecturerAnalytics();
                Platform.runLater(() -> applyAnalytics(res.courses()));
            } catch (Exception e) {
                Platform.runLater(() -> FxUtils.showError("Load Error", "Could not load analytics data:\n" + e.getMessage()));
            }
        }).start();
    }

    private void applyAnalytics(List<ApiClient.CourseAnalytics> courses) {
        List<String> newCodes = courses.stream().map(ApiClient.CourseAnalytics::courseCode).toList();
        List<String> oldCodes = allCourses.stream().map(ApiClient.CourseAnalytics::courseCode).toList();
        boolean filtersChanged = !newCodes.equals(oldCodes);

        allCourses = new ArrayList<>(courses);
        if (filtersChanged) {
            rebuildCourseFilters();
        }
        renderChart();
        metricsTable.setItems(FXCollections.observableArrayList(filteredCourses()));
    }

    private void rebuildCourseFilters() {
        courseFilterBar.getChildren().clear();

        ToggleButton allBtn = new ToggleButton("All Courses");
        allBtn.getStyleClass().add("course-chip");
        allBtn.setToggleGroup(courseFilterGroup);
        allBtn.setSelected(selectedCourseCode == null);
        allBtn.setOnAction(e -> {
            selectedCourseCode = null;
            chartSubtitleLabel.setText("All courses");
            renderChart();
            metricsTable.setItems(FXCollections.observableArrayList(filteredCourses()));
        });
        courseFilterBar.getChildren().add(allBtn);

        for (ApiClient.CourseAnalytics ca : allCourses) {
            ToggleButton chip = new ToggleButton(ca.courseCode());
            chip.getStyleClass().add("course-chip");
            chip.setToggleGroup(courseFilterGroup);
            chip.setSelected(ca.courseCode().equals(selectedCourseCode));
            chip.setOnAction(e -> {
                selectedCourseCode = ca.courseCode();
                chartSubtitleLabel.setText(ca.courseCode() + " — " + ca.courseName());
                renderChart();
                metricsTable.setItems(FXCollections.observableArrayList(filteredCourses()));
            });
            courseFilterBar.getChildren().add(chip);
        }

        if (selectedCourseCode != null && allCourses.stream().noneMatch(c -> c.courseCode().equals(selectedCourseCode))) {
            selectedCourseCode = null;
            chartSubtitleLabel.setText("All courses");
            allBtn.setSelected(true);
        }
    }

    private List<ApiClient.CourseAnalytics> filteredCourses() {
        if (selectedCourseCode == null) {
            return allCourses;
        }
        return allCourses.stream()
                .filter(c -> c.courseCode().equals(selectedCourseCode))
                .toList();
    }

    private void renderChart() {
        List<ApiClient.CourseAnalytics> data = filteredCourses();

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Attendance Rate (%)");

        for (ApiClient.CourseAnalytics ca : data) {
            series.getData().add(new XYChart.Data<>(ca.courseCode(), ca.averageRatePercent()));
        }

        attendanceChart.getData().clear();
        if (!series.getData().isEmpty()) {
            attendanceChart.getData().add(series);
            styleChartBars(series);
        }
    }

    private void styleChartBars(XYChart.Series<String, Number> series) {
        Platform.runLater(() -> {
            for (XYChart.Data<String, Number> point : series.getData()) {
                if (point.getNode() != null) {
                    point.getNode().setStyle("-fx-bar-fill: #38bdf8;");
                } else {
                    point.nodeProperty().addListener((obs, oldNode, newNode) -> {
                        if (newNode != null) {
                            newNode.setStyle("-fx-bar-fill: #38bdf8;");
                        }
                    });
                }
            }
        });
    }

    @FXML
    private void handleRefresh() {
        refreshButton.setDisable(true);
        new Thread(() -> {
            try {
                ApiClient.LecturerAnalyticsResponse res = ApiClient.getLecturerAnalytics();
                Platform.runLater(() -> {
                    applyAnalytics(res.courses());
                    refreshButton.setDisable(false);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    refreshButton.setDisable(false);
                    FxUtils.showError("Refresh Error", e.getMessage());
                });
            }
        }).start();
    }

    @FXML
    private void handleDashboardNav() {
        stopRefresh();
        App.navigateTo("dashboard.fxml", "Dashboard");
    }

    @FXML
    private void handleCoursesNav() {
        stopRefresh();
        App.navigateTo("courses.fxml", "Courses");
    }

    @FXML
    private void handleLogout() {
        stopRefresh();
        SessionManager.clearSession();
        App.showLogin();
    }

    private void stopRefresh() {
        if (refreshTimeline != null) {
            refreshTimeline.stop();
        }
    }
}
