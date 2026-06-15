package com.ucc.attendance.desktop.controller;

import com.ucc.attendance.desktop.ApiClient;
import com.ucc.attendance.desktop.SessionManager;
import com.ucc.attendance.desktop.App;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.util.List;

/**
 * Login, registration, and department selection for students and lecturers.
 */
public class LoginController {

    // ── Shared UI ───────────────────────────────────────
    @FXML private Label errorLabel;
    @FXML private Label successLabel;
    @FXML private Label statusLabel;

    // ── Tab Toggles ────────────────────────────────────
    @FXML private ToggleGroup roleGroup;
    @FXML private ToggleButton signInTab;
    @FXML private ToggleButton studentRegisterTab;
    @FXML private ToggleButton lecturerRegisterTab;

    // ── Sign In Pane ───────────────────────────────────
    @FXML private VBox signInPane;
    @FXML private RadioButton loginAsLecturer;
    @FXML private RadioButton loginAsStudent;
    @FXML private ToggleGroup loginRoleGroup;
    @FXML private Label identifierLabel;
    @FXML private TextField loginIdentifierField;
    @FXML private PasswordField loginPasswordField;
    @FXML private Button loginButton;

    // ── Student Register Pane ──────────────────────────
    @FXML private VBox studentRegisterPane;
    @FXML private TextField studentNameField;
    @FXML private TextField studentEmailField;
    @FXML private TextField studentIndexField;
    @FXML private ComboBox<String> studentDeptCombo;
    @FXML private PasswordField studentPasswordField;
    @FXML private Button studentRegisterButton;

    // ── Lecturer Register Pane ─────────────────────────
    @FXML private VBox lecturerRegisterPane;
    @FXML private TextField lecturerNameField;
    @FXML private TextField lecturerCodeField;
    @FXML private ComboBox<String> lecturerDeptCombo;
    @FXML private PasswordField lecturerPasswordField;
    @FXML private Button lecturerRegisterButton;

    // ── Department data cache ──────────────────────────
    private List<ApiClient.DepartmentResponse> departments;

    @FXML
    public void initialize() {
        errorLabel.setVisible(false);
        successLabel.setVisible(false);
        statusLabel.setText("Connecting to " + ApiClient.getBaseUrl() + "...");

        // Tab switching logic
        roleGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            clearMessages();
            showPane(newVal);
        });

        // Login role sub-toggle: change identifier label
        loginRoleGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == loginAsStudent) {
                identifierLabel.setText("Student Email");
                loginIdentifierField.setPromptText("e.g. student@ucc.edu.gh");
            } else {
                identifierLabel.setText("Lecturer Code");
                loginIdentifierField.setPromptText("e.g. UCC-LEC-A7X9");
            }
        });

        // Load departments in background for the registration dropdowns
        loadDepartments();
    }

    /**
     * Switches visible form pane based on selected tab toggle.
     */
    private void showPane(Toggle selected) {
        signInPane.setVisible(selected == signInTab);
        signInPane.setManaged(selected == signInTab);
        studentRegisterPane.setVisible(selected == studentRegisterTab);
        studentRegisterPane.setManaged(selected == studentRegisterTab);
        lecturerRegisterPane.setVisible(selected == lecturerRegisterTab);
        lecturerRegisterPane.setManaged(selected == lecturerRegisterTab);
    }

    /**
     * Loads departments from the API for registration dropdowns.
     */
    private void loadDepartments() {
        new Thread(() -> {
            try {
                departments = ApiClient.getDepartments();
                Platform.runLater(() -> {
                    statusLabel.setText("Connected to " + ApiClient.getBaseUrl());
                    statusLabel.setStyle("-fx-text-fill: #22c55e; -fx-font-size: 11px;");
                    for (ApiClient.DepartmentResponse dept : departments) {
                        String item = dept.code() + " — " + dept.name();
                        studentDeptCombo.getItems().add(item);
                        lecturerDeptCombo.getItems().add(item);
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    statusLabel.setText("⚠ Cannot reach server: " + ApiClient.getBaseUrl());
                    statusLabel.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 11px;");
                    System.err.println("Failed to load departments: " + e.getMessage());
                });
            }
        }).start();
    }

    /**
     * Extracts the department code from a combo selection like "CSC — Computer Science".
     */
    private String extractDeptCode(String comboValue) {
        if (comboValue == null || comboValue.isEmpty()) return null;
        return comboValue.split(" — ")[0].trim();
    }

    // ── Sign In Handler ────────────────────────────────

    @FXML
    private void handleLogin() {
        String identifier = loginIdentifierField.getText().trim();
        String password = loginPasswordField.getText();
        String role = loginAsLecturer.isSelected() ? "LECTURER" : "STUDENT";

        if (identifier.isEmpty() || password.isEmpty()) {
            showError("Please fill in all fields.");
            return;
        }

        loginButton.setDisable(true);
        clearMessages();

        runInBackground(() -> {
            try {
                ApiClient.AuthResponse res = ApiClient.login(identifier, password, role);
                Platform.runLater(() -> {
                    loginButton.setDisable(false);
                    SessionManager.setSession(res.token(), res.displayName(), identifier, role);
                    if (!App.showDashboard()) {
                        SessionManager.clearSession();
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    loginButton.setDisable(false);
                    showError(e.getMessage() != null ? e.getMessage() : "Login failed. Please try again.");
                });
            }
        });
    }

    // ── Student Register Handler ───────────────────────

    @FXML
    private void handleStudentRegister() {
        String name = studentNameField.getText().trim();
        String email = studentEmailField.getText().trim();
        String index = studentIndexField.getText().trim();
        String deptCode = extractDeptCode(studentDeptCombo.getValue());
        String password = studentPasswordField.getText();

        if (name.isEmpty() || email.isEmpty() || index.isEmpty() || deptCode == null || password.isEmpty()) {
            showError("Please fill in all fields.");
            return;
        }
        if (password.length() < 8) {
            showError("Password must be at least 8 characters.");
            return;
        }

        studentRegisterButton.setDisable(true);
        clearMessages();

        runInBackground(() -> {
            try {
                ApiClient.AuthResponse res = ApiClient.registerStudent(name, email, index, deptCode, password);
                Platform.runLater(() -> {
                    studentRegisterButton.setDisable(false);
                    SessionManager.setSession(res.token(), res.displayName(), email, "STUDENT");
                    App.showDashboard();
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    studentRegisterButton.setDisable(false);
                    showError(e.getMessage() != null ? e.getMessage() : "Registration failed. Please try again.");
                });
            }
        });
    }

    // ── Lecturer Register Handler ──────────────────────

    @FXML
    private void handleLecturerRegister() {
        String name = lecturerNameField.getText().trim();
        String code = lecturerCodeField.getText().trim();
        String deptCode = extractDeptCode(lecturerDeptCombo.getValue());
        String password = lecturerPasswordField.getText();

        if (name.isEmpty() || code.isEmpty() || deptCode == null || password.isEmpty()) {
            showError("Please fill in all fields.");
            return;
        }
        if (password.length() < 8) {
            showError("Password must be at least 8 characters.");
            return;
        }

        lecturerRegisterButton.setDisable(true);
        clearMessages();

        runInBackground(() -> {
            try {
                ApiClient.AuthResponse res = ApiClient.registerLecturer(name, code, deptCode, password);
                Platform.runLater(() -> {
                    lecturerRegisterButton.setDisable(false);
                    SessionManager.setSession(res.token(), res.displayName(), code, "LECTURER");
                    App.showDashboard();
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    lecturerRegisterButton.setDisable(false);
                    showError(e.getMessage() != null ? e.getMessage() : "Registration failed. Please try again.");
                });
            }
        });
    }

    // ── UI Helpers ──────────────────────────────────────

    private void showError(String message) {
        successLabel.setVisible(false);
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }

    private void showSuccess(String message) {
        errorLabel.setVisible(false);
        successLabel.setText(message);
        successLabel.setVisible(true);
    }

    private void clearMessages() {
        errorLabel.setVisible(false);
        successLabel.setVisible(false);
    }

    /**
     * Runs a task on a daemon background thread with an uncaught exception handler
     * to prevent silent failures.
     */
    private void runInBackground(Runnable task) {
        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.setUncaughtExceptionHandler((t, e) -> {
            System.err.println("Uncaught exception in thread " + t.getName() + ": " + e.getMessage());
            e.printStackTrace();
            Platform.runLater(() -> showError("Unexpected error: " + e.getMessage()));
        });
        thread.start();
    }
}
