package com.ucc.attendance.desktop.util;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.TableColumn;

import java.util.function.Function;
import java.util.function.ToLongFunction;

/**
 * Binds TableView columns to Java record accessors.
 * PropertyValueFactory only works with JavaBean getters (getXxx), not record accessors.
 */
public final class TableColumns {

    private TableColumns() {}

    public static <S> void text(TableColumn<S, String> column, Function<S, String> getter) {
        column.setCellValueFactory(cell -> {
            S row = cell.getValue();
            if (row == null) {
                return new SimpleStringProperty("");
            }
            String value = getter.apply(row);
            return new SimpleStringProperty(value != null ? value : "");
        });
    }

    public static <S> void numberLong(TableColumn<S, Long> column, ToLongFunction<S> getter) {
        column.setCellValueFactory(cell -> {
            S row = cell.getValue();
            if (row == null) {
                return new ReadOnlyObjectWrapper<>(0L);
            }
            return new ReadOnlyObjectWrapper<>(getter.applyAsLong(row));
        });
    }

    public static <S> void bool(TableColumn<S, String> column, Function<S, Boolean> getter, String trueLabel, String falseLabel) {
        column.setCellValueFactory(cell -> {
            S row = cell.getValue();
            if (row == null) {
                return new SimpleStringProperty("");
            }
            Boolean value = getter.apply(row);
            return new SimpleStringProperty(Boolean.TRUE.equals(value) ? trueLabel : falseLabel);
        });
    }
}
