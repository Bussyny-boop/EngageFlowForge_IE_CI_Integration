package com.example.exceljson;

import javafx.beans.property.StringProperty;
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
import java.util.function.Function;

/**
 * AppController – Enhanced GUI for Engage Rules Generator
 * --------------------------------------------------------
 * Features:
 *  - Load Excel workbook (NurseCalls + Clinicals + Units)
 *  - Edit tables directly
 *  - Save edited Excel workbook
 *  - Generate JSON preview (full-screen)
 *  - Save JSON to file
 */
public class AppController {

    private ExcelParserV2 parser;

    private final TableView<UnitRow> tblUnits = new TableView<>();
    private final TableView<FlowRow> tblNurseCalls = new TableView<>();
    private final TableView<FlowRow> tblClinicals = new TableView<>();
    private final TextArea jsonPreview = new TextArea();
    private final Button btnOpen = new Button("📂 Open Excel…");
    private final Button btnSaveEdited = new Button("💾 Save Edited Excel…");
    private final Button btnPreview = new Button("🧩 Generate JSON Preview");
    private final Button btnSaveJson = new Button("📤 Save JSON File…");

    private File currentFile;

    public void start(Stage stage) {
        stage.setTitle("Engage Rules Generator – JSON Builder");

        BorderPane root = new BorderPane();
        root.setPadding(new Insets(10));

        // ---- Top toolbar ----
        HBox top = new HBox(10);
        top.getChildren().addAll(btnOpen, btnSaveEdited, btnPreview, btnSaveJson);
        root.setTop(top);

        // ---- TabPane for tables ----
        TabPane tabs = new TabPane();
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        Tab tabUnits = new Tab("🏢 Unit Breakdown", tblUnits);
        Tab tabNurse = new Tab("💬 Nurse Calls", tblNurseCalls);
        Tab tabClin = new Tab("⚕️ Patient Monitoring", tblClinicals);

        // Fullscreen JSON preview
        ScrollPane jsonPane = new ScrollPane(jsonPreview);
        jsonPane.setFitToWidth(true);
        jsonPane.setFitToHeight(true);
        jsonPreview.setEditable(false);
        jsonPreview.setWrapText(true);
        jsonPreview.setStyle("-fx-font-family: 'Consolas', 'Courier New', monospace; -fx-font-size: 13px;");

        Tab tabJson = new Tab("🧾 JSON Preview", jsonPane);

        tabs.getTabs().addAll(tabUnits, tabNurse, tabClin, tabJson);
        root.setCenter(tabs);

        // ---- Button Actions ----
        btnOpen.setOnAction(e -> openExcel(stage));
        btnSaveEdited.setOnAction(e -> saveEditedExcel(stage));
        btnPreview.setOnAction(e -> generateJsonPreview());
        btnSaveJson.setOnAction(e -> saveJsonFile(stage));

        setDataActionsEnabled(false);

        // ---- Scene ----
        Scene scene = new Scene(root, 1500, 900);
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
            resetPreview();
            setDataActionsEnabled(true);
            showAlert(Alert.AlertType.INFORMATION, "✅ Success", "Excel file loaded successfully!\n" +
                    "Units: " + parser.getUnits().size() +
                    ", NurseCalls: " + parser.getNurseCalls().size() +
                    ", Clinicals: " + parser.getClinicals().size());
        } catch (Exception ex) {
            parser = null;
            clearTables();
            resetPreview();
            setDataActionsEnabled(false);
            showAlert(Alert.AlertType.ERROR, "❌ Error", "Failed to load Excel file:\n" + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private void populateTables() {
        // Units
        tblUnits.setItems(FXCollections.observableArrayList(parser.getUnits()));
        tblUnits.getColumns().setAll(
                makeEditableCol("Facility", UnitRow::facilityProperty),
                makeEditableCol("Common Unit Name", UnitRow::unitNameProperty),
                makeEditableCol("Configuration Group", UnitRow::configGroupProperty)
        );

        // Nurse Calls
        tblNurseCalls.setItems(FXCollections.observableArrayList(parser.getNurseCalls()));
        tblNurseCalls.getColumns().setAll(
                makeEditableCol("Config Group", FlowRow::configGroupProperty),
                makeEditableCol("Alarm Name", FlowRow::alarmNameProperty),
                makeEditableCol("Priority", FlowRow::priorityProperty),
                makeEditableCol("Ringtone", FlowRow::ringtoneProperty),
                makeEditableCol("Response Options", FlowRow::responseOptionsProperty),
                makeEditableCol("1st Recipient", FlowRow::r1Property),
                makeEditableCol("2nd Recipient", FlowRow::r2Property),
                makeEditableCol("3rd Recipient", FlowRow::r3Property),
                makeEditableCol("4th Recipient", FlowRow::r4Property)
        );

        // Clinicals
        tblClinicals.setItems(FXCollections.observableArrayList(parser.getClinicals()));
        tblClinicals.getColumns().setAll(
                makeEditableCol("Config Group", FlowRow::configGroupProperty),
                makeEditableCol("Alarm Name", FlowRow::alarmNameProperty),
                makeEditableCol("Priority", FlowRow::priorityProperty),
                makeEditableCol("Ringtone", FlowRow::ringtoneProperty),
                makeEditableCol("Response Options", FlowRow::responseOptionsProperty),
                makeEditableCol("1st Recipient", FlowRow::r1Property),
                makeEditableCol("2nd Recipient", FlowRow::r2Property),
                makeEditableCol("3rd Recipient", FlowRow::r3Property),
                makeEditableCol("4th Recipient", FlowRow::r4Property),
                makeEditableCol("Fail Safe Recipient", FlowRow::failSafeProperty)
        );

        tblUnits.setEditable(true);
        tblNurseCalls.setEditable(true);
        tblClinicals.setEditable(true);
    }

    private void saveEditedExcel(Stage stage) {
        if (parser == null) {
            showAlert(Alert.AlertType.ERROR, "❗ Error", "No Excel file loaded yet!");
            return;
        }
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel Files", "*.xlsx"));
        fc.setInitialFileName("Edited_Configuration.xlsx");
        File out = fc.showSaveDialog(stage);
        if (out == null) return;

        try {
            parser.exportEditedExcel(out);
            showAlert(Alert.AlertType.INFORMATION, "✅ Saved", "Edited Excel file saved successfully:\n" + out.getAbsolutePath());
        } catch (Exception ex) {
            showAlert(Alert.AlertType.ERROR, "❌ Error", "Failed to save edited Excel:\n" + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private void generateJsonPreview() {
        if (parser == null) {
            showAlert(Alert.AlertType.ERROR, "❗ Error", "Load a file first!");
            return;
        }
        try {
            Map<String, Object> json = parser.toJson();
            String preview = ExcelParserV2.pretty(json, 0);
            jsonPreview.setText(preview);
            btnSaveJson.setDisable(false);
            showAlert(Alert.AlertType.INFORMATION, "✅ Success", "JSON Preview generated successfully!");
        } catch (Exception ex) {
            btnSaveJson.setDisable(true);
            showAlert(Alert.AlertType.ERROR, "❌ Error", "Failed to generate JSON preview:\n" + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private void saveJsonFile(Stage stage) {
        if (parser == null) {
            showAlert(Alert.AlertType.ERROR, "❗ Error", "No data to save. Please load a file and generate JSON first.");
            return;
        }

        if (jsonPreview.getText().isBlank()) {
            showAlert(Alert.AlertType.ERROR, "❗ Error", "Generate the JSON preview before saving the file.");
            return;
        }

        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON Files", "*.json"));
        fc.setInitialFileName("Engage_Rules_Output.json");
        File outFile = fc.showSaveDialog(stage);
        if (outFile == null) return;

        try {
            parser.writeJson(outFile);
            showAlert(Alert.AlertType.INFORMATION, "✅ Saved", "JSON file saved successfully:\n" + outFile.getAbsolutePath());
        } catch (Exception ex) {
            showAlert(Alert.AlertType.ERROR, "❌ Error", "Failed to save JSON file:\n" + ex.getMessage());
            ex.printStackTrace();
        }
    }

    // --------------------------------
    // Helpers
    // --------------------------------

    private void setDataActionsEnabled(boolean enabled) {
        btnSaveEdited.setDisable(!enabled);
        btnPreview.setDisable(!enabled);
        if (!enabled) {
            btnSaveJson.setDisable(true);
        } else {
            btnSaveJson.setDisable(jsonPreview.getText().isBlank());
        }
    }

    private void resetPreview() {
        jsonPreview.clear();
        btnSaveJson.setDisable(true);
    }

    private void clearTables() {
        tblUnits.getItems().clear();
        tblNurseCalls.getItems().clear();
        tblClinicals.getItems().clear();
    }

    private <T> TableColumn<T, String> makeEditableCol(String title, String fieldName) {
        TableColumn<T, String> col = new TableColumn<>(title);
        col.setCellValueFactory(cellData -> accessor.apply(cellData.getValue()));
        col.setCellFactory(TextFieldTableCell.forTableColumn());
        col.setOnEditCommit(evt -> {
            StringProperty property = accessor.apply(evt.getRowValue());
            if (property != null) {
                property.set(evt.getNewValue());
            }
        });
        col.setPrefWidth(150);
        return col;
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
