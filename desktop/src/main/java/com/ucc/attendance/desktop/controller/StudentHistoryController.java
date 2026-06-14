package com.ucc.attendance.desktop.controller;

import com.ucc.attendance.desktop.ApiClient;
import com.ucc.attendance.desktop.SessionManager;
import com.ucc.attendance.desktop.App;
import com.ucc.attendance.desktop.util.FxUtils;
import com.ucc.attendance.desktop.util.TableCells;
import com.ucc.attendance.desktop.util.TableColumns;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;

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
        TableColumns.text(courseCodeCol, ApiClient.HistoryItem::courseCode);
        TableColumns.text(courseNameCol, ApiClient.HistoryItem::courseName);

        timeCol.setCellValueFactory(cellData -> {
            if (cellData.getValue() == null || cellData.getValue().attendanceTime() == null) {
                return new javafx.beans.property.SimpleStringProperty("");
            }
            Instant time = cellData.getValue().attendanceTime();
            String formatted = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
                    .withZone(ZoneId.systemDefault())
                    .format(time);
            return new javafx.beans.property.SimpleStringProperty(formatted);
        });

        TableColumns.text(statusCol, ApiClient.HistoryItem::status);
        TableCells.statusChip(statusCol);

        historyTable.getStyleClass().add("structured-table");

        historyTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        historyTable.setPlaceholder(new Label("No attendance records yet."));

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
