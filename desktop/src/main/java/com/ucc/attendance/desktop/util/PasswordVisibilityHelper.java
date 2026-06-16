package com.ucc.attendance.desktop.util;

import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

/**
 * Toggles visibility for JavaFX password fields via a companion plain-text field.
 */
public final class PasswordVisibilityHelper {

    private PasswordVisibilityHelper() {}

    public static void wire(HBox container, PasswordField passwordField, Button toggleButton) {
        TextField plainField = new TextField();
        plainField.setPromptText(passwordField.getPromptText());
        plainField.getStyleClass().addAll(passwordField.getStyleClass());
        plainField.setPrefWidth(passwordField.getPrefWidth());
        plainField.setMaxWidth(passwordField.getMaxWidth());
        plainField.setVisible(false);
        plainField.setManaged(false);
        plainField.textProperty().bindBidirectional(passwordField.textProperty());

        int index = container.getChildren().indexOf(passwordField);
        if (index >= 0) {
            container.getChildren().add(index + 1, plainField);
        } else {
            container.getChildren().add(plainField);
        }
        HBox.setHgrow(plainField, Priority.ALWAYS);

        toggleButton.setFocusTraversable(false);
        toggleButton.getStyleClass().add("password-toggle");
        toggleButton.setText("\uD83D\uDC41");
        toggleButton.setOnAction(event -> {
            boolean reveal = !plainField.isVisible();
            plainField.setVisible(reveal);
            plainField.setManaged(reveal);
            passwordField.setVisible(!reveal);
            passwordField.setManaged(!reveal);
            if (reveal) {
                plainField.requestFocus();
                plainField.positionCaret(plainField.getText().length());
            } else {
                passwordField.requestFocus();
                passwordField.positionCaret(passwordField.getText().length());
            }
        });
    }
}
