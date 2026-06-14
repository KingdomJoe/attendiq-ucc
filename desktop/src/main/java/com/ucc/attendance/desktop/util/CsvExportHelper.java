package com.ucc.attendance.desktop.util;

import com.ucc.attendance.desktop.ApiClient;
import com.ucc.attendance.desktop.App;
import javafx.application.Platform;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.FileOutputStream;

public final class CsvExportHelper {

    private CsvExportHelper() {}

    public static void exportSessionCsv(Long sessionId, String courseCode, Stage owner) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Attendance Report");
        String safeCode = courseCode != null ? courseCode : "session";
        fileChooser.setInitialFileName("attendance-" + safeCode + "-session-" + sessionId + ".csv");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files (*.csv)", "*.csv"));

        Stage dialogOwner = owner != null ? owner : App.getPrimaryStage();
        File file = fileChooser.showSaveDialog(dialogOwner);
        if (file == null) {
            return;
        }

        new Thread(() -> {
            try {
                byte[] csvBytes = ApiClient.downloadAttendanceCsv(sessionId);
                try (FileOutputStream out = new FileOutputStream(file)) {
                    out.write(csvBytes);
                }
                Platform.runLater(() -> FxUtils.showInfo("Export Successful",
                        "Attendance report saved to:\n" + file.getAbsolutePath()));
            } catch (Exception e) {
                Platform.runLater(() -> FxUtils.showError("Export Failed",
                        "Could not export CSV:\n" + e.getMessage()));
            }
        }).start();
    }
}
