package com.ucc.attendance.desktop;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * JavaFX Desktop Application entry point for AttendIQ (Lecturer Client).
 * Connects to the Spring Boot REST API backend.
 */
public class App extends Application {

    private static Stage primaryStage;

    @Override
    public void start(Stage stage) throws Exception {
        primaryStage = stage;
        stage.setTitle("AttendIQ — Lecturer Dashboard");
        stage.setMinWidth(900);
        stage.setMinHeight(600);

        showLogin();

        stage.show();
    }

    /**
     * Navigate to a new scene by loading an FXML file.
     */
    public static void navigateTo(String fxmlFile, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(App.class.getResource("/fxml/" + fxmlFile));
            Parent root = loader.load();
            Scene scene = new Scene(root, 1000, 700);
            scene.getStylesheets().add(App.class.getResource("/css/style.css").toExternalForm());
            primaryStage.setScene(scene);
            primaryStage.setTitle("AttendIQ — " + title);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Get the FXML loader for a given FXML file (when controller needs to be accessed).
     */
    public static FXMLLoader getLoader(String fxmlFile) {
        return new FXMLLoader(App.class.getResource("/fxml/" + fxmlFile));
    }

    public static void showLogin() {
        navigateTo("login.fxml", "Login");
    }

    public static void showDashboard() {
        if ("STUDENT".equals(SessionManager.getUserRole())) {
            navigateTo("student-dashboard.fxml", "Student Dashboard");
        } else {
            navigateTo("dashboard.fxml", "Lecturer Dashboard");
        }
    }

    public static Stage getPrimaryStage() {
        return primaryStage;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
