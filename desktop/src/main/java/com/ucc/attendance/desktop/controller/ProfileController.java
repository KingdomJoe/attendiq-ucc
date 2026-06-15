package com.ucc.attendance.desktop.controller;

import com.ucc.attendance.desktop.ApiClient;
import com.ucc.attendance.desktop.App;
import com.ucc.attendance.desktop.SessionManager;
import com.ucc.attendance.desktop.util.FxUtils;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

/**
 * Modal profile editor for updating display name and optional password.
 */
public class ProfileController {

    @FXML private Label accountLabel;
    @FXML private TextField nameField;
    @FXML private PasswordField currentPasswordField;
    @FXML private PasswordField newPasswordField;

    private Runnable onSaved = () -> {};

    @FXML
    public void initialize() {
        loadProfile();
    }

    public void setOnSaved(Runnable onSaved) {
        this.onSaved = onSaved != null ? onSaved : () -> {};
    }

    private void loadProfile() {
        new Thread(() -> {
            try {
                ApiClient.MeResponse me = ApiClient.getMe();
                Platform.runLater(() -> {
                    nameField.setText(me.displayName());
                    String account = me.emailOrCode();
                    if (me.indexNumber() != null && !me.indexNumber().isBlank()) {
                        account += " · " + me.indexNumber();
                    }
                    accountLabel.setText("Signed in as " + account);
                });
            } catch (Exception e) {
                Platform.runLater(() -> FxUtils.showError("Profile Error", "Could not load profile:\n" + e.getMessage()));
            }
        }).start();
    }

    @FXML
    private void handleSave() {
        String name = nameField.getText() == null ? "" : nameField.getText().trim();
        if (name.isEmpty()) {
            FxUtils.showError("Input Error", "Display name is required.");
            return;
        }
        String currentPassword = currentPasswordField.getText();
        String newPassword = newPasswordField.getText();
        if (newPassword != null && !newPassword.isBlank() && (currentPassword == null || currentPassword.isBlank())) {
            FxUtils.showError("Input Error", "Enter your current password to set a new password.");
            return;
        }
        if (newPassword != null && !newPassword.isBlank() && newPassword.length() < 8) {
            FxUtils.showError("Input Error", "New password must be at least 8 characters.");
            return;
        }

        new Thread(() -> {
            try {
                ApiClient.MeResponse updated = ApiClient.updateProfile(
                        name,
                        blankToNull(currentPassword),
                        blankToNull(newPassword));
                Platform.runLater(() -> {
                    SessionManager.setDisplayName(updated.displayName());
                    currentPasswordField.clear();
                    newPasswordField.clear();
                    FxUtils.showInfo("Profile Updated", "Your profile has been saved.");
                    onSaved.run();
                    closeDialog();
                });
            } catch (Exception e) {
                Platform.runLater(() -> FxUtils.showError("Profile Error", "Could not save profile:\n" + e.getMessage()));
            }
        }).start();
    }

    @FXML
    private void handleCancel() {
        closeDialog();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private void closeDialog() {
        Stage stage = (Stage) nameField.getScene().getWindow();
        stage.close();
    }

    public static void show(Runnable onSaved) {
        try {
            javafx.fxml.FXMLLoader loader = App.getLoader("profile.fxml");
            javafx.scene.Parent root = loader.load();
            ProfileController controller = loader.getController();
            controller.setOnSaved(onSaved);

            Stage dialog = new Stage();
            dialog.initOwner(App.getPrimaryStage());
            dialog.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            dialog.setTitle("AttendIQ — Edit Profile");
            javafx.scene.Scene scene = new javafx.scene.Scene(root);
            scene.getStylesheets().add(App.class.getResource("/css/style.css").toExternalForm());
            dialog.setScene(scene);
            dialog.showAndWait();
        } catch (Exception e) {
            FxUtils.showError("Profile Error", "Could not open profile editor:\n" + e.getMessage());
        }
    }
}
