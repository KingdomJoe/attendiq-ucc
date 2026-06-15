package com.ucc.attendance.desktop.controller;

import com.ucc.attendance.desktop.ApiClient;
import com.ucc.attendance.desktop.SessionManager;
import com.ucc.attendance.desktop.App;
import com.ucc.attendance.desktop.util.FxUtils;
import com.ucc.attendance.desktop.util.TableCells;
import com.ucc.attendance.desktop.util.TableColumns;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Full attendance history table for the logged-in student with live search filter.
 */
public class StudentHistoryController {

    @FXML private Label studentNameLabel;
    @FXML private Label studentIndexLabel;
    @FXML private TextField searchHistoryField;

    @FXML private TableView<ApiClient.HistoryItem> historyTable;
    @FXML private TableColumn<ApiClient.HistoryItem, String> courseCodeCol;
    @FXML private TableColumn<ApiClient.HistoryItem, String> courseNameCol;
    @FXML private TableColumn<ApiClient.HistoryItem, String> timeCol;
    @FXML private TableColumn<ApiClient.HistoryItem, String> statusCol;

    private ObservableList<ApiClient.HistoryItem> allHistory = FXCollections.observableArrayList();
    private FilteredList<ApiClient.HistoryItem> filteredHistory;

    @FXML
    public void initialize() {
        studentNameLabel.setText(SessionManager.getDisplayName());
        studentIndexLabel.setText(SessionManager.getIdentifier());

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

        filteredHistory = new FilteredList<>(allHistory, item -> true);
        historyTable.setItems(filteredHistory);
        searchHistoryField.textProperty().addListener((obs, oldVal, newVal) -> applyHistoryFilter(newVal));

        loadHistoryData();
    }

    private void applyHistoryFilter(String query) {
        String q = query == null ? "" : query.trim().toLowerCase();
        filteredHistory.setPredicate(item -> {
            if (q.isEmpty()) {
                return true;
            }
            String time = item.attendanceTime() != null
                    ? DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault()).format(item.attendanceTime())
                    : "";
            return item.courseCode().toLowerCase().contains(q)
                    || item.courseName().toLowerCase().contains(q)
                    || item.status().toLowerCase().contains(q)
                    || time.toLowerCase().contains(q);
        });
    }

    private void loadHistoryData() {
        new Thread(() -> {
            try {
                List<ApiClient.HistoryItem> history = ApiClient.getStudentHistory();
                Platform.runLater(() -> allHistory.setAll(history));
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
    private void handleProfile() {
        ProfileController.show(() -> {
            studentNameLabel.setText(SessionManager.getDisplayName());
        });
    }

    @FXML
    private void handleLogout() {
        SessionManager.clearSession();
        App.showLogin();
    }
}
