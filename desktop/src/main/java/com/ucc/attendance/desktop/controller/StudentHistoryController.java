package com.ucc.attendance.desktop.controller;

import com.ucc.attendance.desktop.ApiClient;
import com.ucc.attendance.desktop.SessionManager;
import com.ucc.attendance.desktop.App;
import com.ucc.attendance.desktop.util.FxUtils;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class StudentHistoryController {

    @FXML private Label studentNameLabel;
    @FXML private Label studentIndexLabel;

    @FXML private TableView<ApiClient.HistoryItem> historyTable;
    @FXML private TableColumn<ApiClient.HistoryItem, String> courseCodeCol;
    @FXML private TableColumn<ApiClient.HistoryItem, String> courseNameCol;
    @FXML private TableColumn<ApiClient.HistoryItem, String> timeCol;
    @FXML private TableColumn<ApiClient.HistoryItem, String> statusCol;

    @FXML
    public void initialize() {
        studentNameLabel.setText(SessionManager.getDisplayName());
        studentIndexLabel.setText(SessionManager.getIdentifier());

        // Setup columns
        courseCodeCol.setCellValueFactory(new PropertyValueFactory<>("courseCode"));
        courseNameCol.setCellValueFactory(new PropertyValueFactory<>("courseName"));
        
        timeCol.setCellValueFactory(cellData -> {
            Instant time = cellData.getValue().attendanceTime();
            String formatted = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                    .withZone(ZoneId.systemDefault())
                    .format(time);
            return new javafx.beans.property.SimpleStringProperty(formatted);
        });
        
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));

        loadHistoryData();
    }

    private void loadHistoryData() {
        new Thread(() -> {
            try {
                List<ApiClient.HistoryItem> history = ApiClient.getStudentHistory();
                Platform.runLater(() -> {
                    historyTable.setItems(FXCollections.observableArrayList(history));
                });
            } catch (Exception e) {
                Platform.runLater(() -> FxUtils.showError("Load Error", "Failed to load history data:\n" + e.getMessage()));
            }
        }).start();
    }

    @FXML
    private void handleDashboardNav() {
        App.showDashboard();
    }

    @FXML
    private void handleLogout() {
        SessionManager.clearSession();
        App.showLogin();
    }
}
