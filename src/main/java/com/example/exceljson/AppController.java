package com.example.exceljson;

import javafx.collections.*;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.util.Map;

/**
 * AppController — GUI for Engage Rules Generator (ExcelParserV2 integration)
 *
 * Features:
 *  - Load Excel
 *  - View/Edit tables (Units, NurseCalls, Clinicals)
 *  - Save edited Excel
 *  - Generate JSON Preview
 */
public class AppController {

    private ExcelParserV2 parser;

    private final TableView<ExcelParserV2.UnitRow> tblUnits = new TableView<>();
    private final TableView<ExcelParserV2.FlowRow> tblNurseCalls = new TableView<>();
    private final TableView<ExcelParserV2.FlowRow> tblClinicals = new TableView<>();
    private final TextArea jsonPreview = new TextArea();

    private File currentFile;

    public void start(Stage stage) {
        stage.setTitle("Engage Rules Generator – JSON Builder");

        BorderPane root = new BorderPane();
        root.setPadding(new Insets(10));

        // ---- Top toolbar ----
        HBox top = new HBox(10);
        Button btnOpen = new Button("Open Excel…");
        Button btnSaveEdited = new Button("Save Edited Excel…");
        Button btnPreview = new Button("Generate JSON Preview");
        top.getChildren().addAll(btnOpen, btnSaveEdited, btnPreview);
        root.setTop(top);

        // ---- TabPane for tables ----
        TabPane tabs = new TabPane();
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        Tab tabUnits = new Tab("Units", tblUnits);
        Tab tabNurse = new Tab("Nurse Calls", tblNurseCalls);
        Tab tabClin = new Tab("Patient Monitoring", tblClinicals);
        Tab tabJson = new Tab("JSON Preview", new ScrollPane(jsonPreview));
        tabs.getTabs().addAll(tabUnits, tabNurse, tabClin, tabJson);
        root.setCenter(tabs);

        jsonPreview.setEditable(false);
        jsonPreview.setWrapText(true);
        jsonPreview.setStyle("-fx-font-family: 'Consolas', 'Courier New', monospace;");

        // ---- Button Actions ----
        btnOpen.setOnAction(e -> openExcel(stage));
        btnSaveEdited.setOnAction(e -> saveEditedExcel(stage));
        btnPreview.setOnAction(e -> generateJsonPreview());

        // ---- Scene ----
        Scene scene = new Scene(root, 1300, 800);
        stage.setScene(scene);
        stage.show();
    }

    // --------------------------------
    // Handlers
    // --------------------------------

    private void openExcel(Stage stage) {
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel Files", "*.xlsx", "*.xls"));
        currentFile = fc.showOpenDialog(stage);
        if (currentFile == null) return;

        try {
            parser = new ExcelParserV2(
                    "Unit Breakdown",        // Sheet 1
                    "Nurse call",            // Sheet 2
                    "Patient Monitoring"     // Sheet 3
            );
            parser.load(currentFile);
            populateTables();
            jsonPreview.setText("");
            showAlert("Success", "Excel file loaded successfully!");
        } catch (Exception ex) {
            showAlert("Error", "Failed to load Excel file:\n" + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private void populateTables() {
        // Units
        tblUnits.setItems(FXCollections.observableArrayList(parser.getUnits()));
        tblUnits.getColumns().setAll(
                makeEditableCol("Facility", "facility"),
                makeEditableCol("Common Unit Name", "unitName"),
                makeEditableCol("Configuration Group", "configGroup")
        );

        // Nurse Calls
        tblNurseCalls.setItems(FXCollections.observableArrayList(parser.getNurseCalls()));
        tblNurseCalls.getColumns().setAll(
                makeEditableCol("Config Group", "configGroup"),
                makeEditableCol("Alarm Name", "alarmName"),
                makeEditableCol("Priority", "priority"),
                makeEditableCol("Ringtone", "ringtone"),
                makeEditableCol("Response Options", "responseOptions"),
                makeEditableCol("1st Recipient", "r1"),
                makeEditableCol("2nd Recipient", "r2"),
                makeEditableCol("3rd Recipient", "r3"),
                makeEditableCol("4th Recipient", "r4")
        );

        // Clinicals
        tblClinicals.setItems(FXCollections.observableArrayList(parser.getClinicals()));
        tblClinicals.getColumns().setAll(
                makeEditableCol("Config Group", "configGroup"),
                makeEditableCol("Alarm Name", "alarmName"),
                makeEditableCol("Priority", "priority"),
                makeEditableCol("Ringtone", "ringtone"),
                makeEditableCol("Response Options", "responseOptions"),
                makeEditableCol("1st Recipient", "r1"),
                makeEditableCol("2nd Recipient", "r2"),
                makeEditableCol("3rd Recipient", "r3"),
                makeEditableCol("4th Recipient", "r4"),
                makeEditableCol("Fail Safe Recipient", "failSafe")
        );

        tblUnits.setEditable(true);
        tblNurseCalls.setEditable(true);
        tblClinicals.setEditable(true);
    }

    private void saveEditedExcel(Stage stage) {
        if (parser == null) {
            showAlert("Error", "No Excel file loaded yet!");
            return;
        }
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel Files", "*.xlsx"));
        fc.setInitialFileName("Edited_Configuration.xlsx");
        File out = fc.showSaveDialog(stage);
        if (out == null) return;

        try {
            parser.exportEditedExcel(out);
            showAlert("Saved", "Edited Excel file saved successfully:\n" + out.getAbsolutePath());
        } catch (Exception ex) {
            showAlert("Error", "Failed to save edited Excel:\n" + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private void generateJsonPreview() {
        if (parser == null) {
            showAlert("Error", "Load a file first!");
            return;
        }
        try {
            Map<String, Object> json = parser.toJson();
            String preview = ExcelParserV2.pretty(json, 0);
            jsonPreview.setText(preview);
            showAlert("Success", "JSON Preview generated successfully!");
        } catch (Exception ex) {
            showAlert("Error", "Failed to generate JSON preview:\n" + ex.getMessage());
            ex.printStackTrace();
        }
    }

    // --------------------------------
    // Helper functions
    // --------------------------------

    private <T> TableColumn<T, String> makeEditableCol(String title, String fieldName) {
        TableColumn<T, String> col = new TableColumn<>(title);
        col.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>(fieldName));
        col.setCellFactory(TextFieldTableCell.forTableColumn());
        col.setOnEditCommit(evt -> {
            try {
                var field = evt.getRowValue().getClass().getDeclaredField(fieldName);
                field.setAccessible(true);
                field.set(evt.getRowValue(), evt.getNewValue());
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        col.setPrefWidth(150);
        return col;
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
