package com.ucc.attendance.desktop.controller;

import com.ucc.attendance.desktop.ApiClient;
import com.ucc.attendance.desktop.App;
import com.ucc.attendance.desktop.SessionManager;
import com.ucc.attendance.desktop.util.FxUtils;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

import java.util.List;

public class AnalyticsController {

    @FXML private Label lecturerLabel;

    // Chart fields
    @FXML private BarChart<String, Number> attendanceChart;
    @FXML private CategoryAxis courseAxis;
    @FXML private NumberAxis rateAxis;

    // Table fields
    @FXML private TableView<ApiClient.CourseAnalytics> metricsTable;
    @FXML private TableColumn<ApiClient.CourseAnalytics, String> codeCol;
    @FXML private TableColumn<ApiClient.CourseAnalytics, String> nameCol;
    @FXML private TableColumn<ApiClient.CourseAnalytics, Long> sessionsHeldCol;
    @FXML private TableColumn<ApiClient.CourseAnalytics, Long> totalPresentCol;
    @FXML private TableColumn<ApiClient.CourseAnalytics, String> avgRateCol;

    @FXML
    public void initialize() {
        lecturerLabel.setText(SessionManager.getLecturerName());

        // Setup table columns
        codeCol.setCellValueFactory(new PropertyValueFactory<>("courseCode"));
        nameCol.setCellValueFactory(new PropertyValueFactory<>("courseName"));
        sessionsHeldCol.setCellValueFactory(new PropertyValueFactory<>("sessionsHeld"));
        totalPresentCol.setCellValueFactory(new PropertyValueFactory<>("totalPresent"));
        
        avgRateCol.setCellValueFactory(cellData -> {
            int rate = cellData.getValue().averageRatePercent();
            return new javafx.beans.property.SimpleStringProperty(rate + "%");
        });

        loadAnalyticsData();
    }

    private void loadAnalyticsData() {
        new Thread(() -> {
            try {
                ApiClient.LecturerAnalyticsResponse res = ApiClient.getLecturerAnalytics();
                
                Platform.runLater(() -> {
                    List<ApiClient.CourseAnalytics> courses = res.courses();
                    metricsTable.setItems(FXCollections.observableArrayList(courses));

                    // Build Chart Series
                    XYChart.Series<String, Number> series = new XYChart.Series<>();
                    series.setName("Attendance Rate (%)");

                    for (ApiClient.CourseAnalytics ca : courses) {
                        series.getData().add(new XYChart.Data<>(ca.courseCode(), ca.averageRatePercent()));
                    }

                    attendanceChart.getData().clear();
                    attendanceChart.getData().add(series);
                });
            } catch (Exception e) {
                Platform.runLater(() -> FxUtils.showError("Load Error", "Could not load analytics data:\n" + e.getMessage()));
            }
        }).start();
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
    private void handleLogout() {
        SessionManager.clearSession();
        App.showLogin();
    }
}
