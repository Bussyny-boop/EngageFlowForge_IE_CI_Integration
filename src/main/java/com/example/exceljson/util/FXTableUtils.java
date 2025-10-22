package com.example.exceljson.util;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.util.*;

/**
 * Utility class for populating JavaFX TableViews dynamically
 * from a list of Map<String, String> rows.
 */
public class FXTableUtils {

    /**
     * Populates the given TableView with rows and columns based on the map keys.
     * Each map key becomes a column header; each row provides the values.
     */
    public static void populate(TableView<Map<String, String>> table, List<Map<String, String>> rows) {
        table.getColumns().clear();
        table.getItems().clear();

        if (rows == null || rows.isEmpty()) {
            return;
        }

        // Collect all keys (columns)
        LinkedHashSet<String> keys = new LinkedHashSet<>();
        for (Map<String, String> row : rows) {
            keys.addAll(row.keySet());
        }

        // Create a column for each key
        for (String key : keys) {
            TableColumn<Map<String, String>, String> column = new TableColumn<>(key);
            column.setCellValueFactory(data ->
                    new SimpleStringProperty(
                            Optional.ofNullable(data.getValue().get(key)).orElse("")
                    )
            );
            column.setPrefWidth(180);
            table.getColumns().add(column);
        }

        // Add all rows to the table
        table.setItems(FXCollections.observableArrayList(rows));
    }
}
