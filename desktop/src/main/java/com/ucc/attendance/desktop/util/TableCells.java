package com.ucc.attendance.desktop.util;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.layout.HBox;

import java.util.function.Function;

public final class TableCells {

    private TableCells() {}

    public static <S> void statusChip(TableColumn<S, String> column) {
        column.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || item.isBlank()) {
                    setGraphic(null);
                    setText(null);
                    return;
                }
                Label chip = new Label(item);
                String normalized = item.toUpperCase();
                if (normalized.contains("ACTIVE") || normalized.contains("PRESENT") || normalized.contains("OPEN")) {
                    chip.getStyleClass().add("status-active");
                } else if (normalized.contains("CLOSED") || normalized.contains("ABSENT") || normalized.contains("MISSED")) {
                    chip.getStyleClass().add("status-closed");
                } else {
                    chip.getStyleClass().add("status-neutral");
                }
                HBox box = new HBox(chip);
                box.setAlignment(Pos.CENTER_LEFT);
                setGraphic(box);
                setText(null);
            }
        });
    }

    public static <S> void activeBadge(TableColumn<S, String> column, Function<S, Boolean> isActive) {
        column.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                S row = getTableRow() != null ? getTableRow().getItem() : null;
                if (empty || row == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }
                boolean active = Boolean.TRUE.equals(isActive.apply(row));
                Label chip = new Label(active ? "Active" : "Inactive");
                chip.getStyleClass().add(active ? "status-active" : "status-neutral");
                HBox box = new HBox(chip);
                box.setAlignment(Pos.CENTER_LEFT);
                setGraphic(box);
                setText(null);
            }
        });
    }
}
