package com.example.exceljson;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.awt.Desktop;
import java.io.File;

/**
 * AppController – JavaFX UI
 * - Live-edit tables (commit on tab-out)
 * - Popup progress messages while loading
 * - Popup confirmation + "Open Folder" after Save/Generate
 */
public class AppController {

    private ExcelParserV4 parser;

    private final TableView<UnitRow> tblUnits = new TableView<>();
    private final TableView<FlowRow> tblNurse = new TableView<>();
    private final TableView<FlowRow> tblClin  = new TableView<>();
    private final TextArea jsonPreview = new TextArea();

    private File currentExcel;

    public void start(Stage stage) {
        stage.setTitle("Engage Rules Generator – JSON Builder");

        BorderPane root = new BorderPane();
        root.setPadding(new Insets(10));

        HBox top = new HBox(10);
        Button btnOpen = new Button("Open Excel…");
        Button btnSave = new Button("Save Edited Excel…");
        Button btnGen  = new Button("Generate JSON Preview");
        top.getChildren().addAll(btnOpen, btnSave, btnGen);
        root.setTop(top);

        TabPane tabs = new TabPane();
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        Tab tUnits = new Tab("Unit Breakdown", tblUnits);
        Tab tNurse = new Tab("Nurse Calls", tblNurse);
        Tab tClin  = new Tab("Patient Monitoring", tblClin);
        Tab tJson  = new Tab("JSON Preview", new ScrollPane(jsonPreview));
        tabs.getTabs().addAll(tUnits, tNurse, tClin, tJson);
        root.setCenter(tabs);

        jsonPreview.setEditable(false);
        jsonPreview.setWrapText(true);
        jsonPreview.setStyle("-fx-font-family: 'Consolas', 'Courier New', monospace;");

        // Actions
        btnOpen.setOnAction(e -> openExcel(stage));
        btnSave.setOnAction(e -> saveEdited(stage));
        btnGen.setOnAction(e -> generateJson());

        Scene scene = new Scene(root, 1300, 820);
        stage.setScene(scene);
        stage.show();
    }

    // ------------------------------------------------------
    // Open Excel (with popup progress)
    // ------------------------------------------------------
    private void openExcel(Stage stage) {
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel", "*.xlsx", "*.xls"));
        File f = fc.showOpenDialog(stage);
        if (f == null) return;

        try {
            popup("📘 Loading Excel File…", null);

            parser = new ExcelParserV4();
            popup("✅ Reading Unit Breakdown tab…", null);
            // load() will parse all tabs; popups are informational per your request
            parser.load(f);

            popup("✅ Reading Nurse Call tab…", null);
            popup("✅ Reading Patient Monitoring tab…", null);
            popup("✅ Linking units to configuration groups…", null);

            currentExcel = f;
            populateTables();

            popup("✅ Loaded successfully",
                  "Units: " + parser.units.size() +
                  "\nNurse Calls: " + parser.nurseCalls.size() +
                  "\nClinicals: " + parser.clinicals.size());
        } catch (Exception ex) {
            error("Load failed", ex.getMessage());
            ex.printStackTrace();
        }
    }

    // ------------------------------------------------------
    // Populate tables (live-editable, commit on tab-out)
    // ------------------------------------------------------
    private void populateTables() {
        // Units table
        tblUnits.setItems(FXCollections.observableArrayList(parser.units));
        tblUnits.getColumns().setAll(
                colEditable("Facility", UnitRow::facilityProperty),
                colEditable("Common Unit Name", UnitRow::unitNameProperty),
                colEditable("Nurse Call", UnitRow::nurseCallGroupProperty),
                colEditable("Patient Monitoring", UnitRow::patientMonitoringGroupProperty),
                colEditable("No Caregiver Alert Number or Group", UnitRow::noCaregiverGroupProperty)
        );
        tblUnits.setEditable(true);

        // NurseCalls table
        tblNurse.setItems(FXCollections.observableArrayList(parser.nurseCalls));
        tblNurse.getColumns().setAll(
                colEditable("Configuration Group", FlowRow::configGroupProperty),
                colEditable("Alarm Name", FlowRow::alarmNameProperty),
                colEditable("Sending System Name", FlowRow::sendingNameProperty),
                colEditable("Priority (normal/high/urgent)", FlowRow::priorityProperty),
                colEditable("Device - A", FlowRow::deviceAProperty),
                colEditable("Ringtone", FlowRow::ringtoneProperty),
                colEditable("Response Options", FlowRow::responseOptionsProperty),
                colEditable("Time to 1st", FlowRow::t1Property),
                colEditable("1st Recipient", FlowRow::r1Property),
                colEditable("Time to 2nd", FlowRow::t2Property),
                colEditable("2nd Recipient", FlowRow::r2Property),
                colEditable("Time to 3rd", FlowRow::t3Property),
                colEditable("3rd Recipient", FlowRow::r3Property),
                colEditable("Time to 4th", FlowRow::t4Property),
                colEditable("4th Recipient", FlowRow::r4Property),
                colEditable("EMDAN", FlowRow::emdanProperty),
                colEditable("Comments", FlowRow::commentsProperty)
        );
        tblNurse.setEditable(true);

        // Clinicals table
        tblClin.setItems(FXCollections.observableArrayList(parser.clinicals));
        tblClin.getColumns().setAll(
                colEditable("Configuration Group", FlowRow::configGroupProperty),
                colEditable("Alarm Name", FlowRow::alarmNameProperty),
                colEditable("Sending System Name", FlowRow::sendingNameProperty),
                colEditable("Priority (normal/high/urgent)", FlowRow::priorityProperty),
                colEditable("Device - A", FlowRow::deviceAProperty),
                colEditable("Ringtone", FlowRow::ringtoneProperty),
                colEditable("Response Options", FlowRow::responseOptionsProperty),
                colEditable("Time to 1st", FlowRow::t1Property),
                colEditable("1st Recipient", FlowRow::r1Property),
                colEditable("Time to 2nd", FlowRow::t2Property),
                colEditable("2nd Recipient", FlowRow::r2Property),
                colEditable("EMDAN", FlowRow::emdanProperty),
                colEditable("Comments", FlowRow::commentsProperty)
        );
        tblClin.setEditable(true);
    }

    // Generic editable text column that commits on tab-out/click-away
    private <T> TableColumn<T, String> colEditable(
            String title,
            java.util.function.Function<T, javafx.beans.property.StringProperty> prop) {

        TableColumn<T, String> col = new TableColumn<>(title);
        col.setCellValueFactory(data -> prop.apply(data.getValue()));
        col.setCellFactory(TextFieldTableCell.forTableColumn());
        col.setOnEditCommit(evt -> {
            try {
                prop.apply(evt.getRowValue()).set(evt.getNewValue());
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        col.setEditable(true);
        col.setPrefWidth(200);
        return col;
    }

    // ------------------------------------------------------
    // Save edited Excel
    // ------------------------------------------------------
    private void saveEdited(Stage stage) {
        if (parser == null) { error("No file", "Load an Excel file first."); return; }

        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel", "*.xlsx"));
        fc.setInitialFileName("Edited_Configuration.xlsx");
        File out = fc.showSaveDialog(stage);
        if (out == null) return;

        try {
            parser.exportEditedExcel(out);
            popupOpenFolder("💾 Excel file saved successfully",
                    out.getAbsolutePath(),
                    out.getParentFile());
        } catch (Exception ex) {
            error("Save failed", ex.getMessage());
            ex.printStackTrace();
        }
    }

    // ------------------------------------------------------
    // Generate JSON
    // ------------------------------------------------------
    private void generateJson() {
        if (parser == null) { error("No data", "Load a file first."); return; }
        try {
            var ncFile = parser.writeNurseCallsJson();
            var clFile = parser.writeClinicalsJson();
            String preview = "// " + ncFile.getName() + "\n"
                    + ExcelParserV4.pretty(parser.buildNurseCallsJson()) + "\n\n"
                    + "// " + clFile.getName() + "\n"
                    + ExcelParserV4.pretty(parser.buildClinicalsJson());
            jsonPreview.setText(preview);

            popupOpenFolder("✅ JSON Files Written",
                    ncFile.getAbsolutePath() + "\n" + clFile.getAbsolutePath(),
                    ncFile.getParentFile());
        } catch (Exception ex) {
            error("JSON error", ex.getMessage());
            ex.printStackTrace();
        }
    }

    // ------------------------------------------------------
    // Popups
    // ------------------------------------------------------
    private void popup(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, msg == null ? "" : msg, ButtonType.OK);
        a.setHeaderText(null);
        a.setTitle(title);
        a.showAndWait();
    }
    private void popupOpenFolder(String title, String msg, File folder) {
        ButtonType open = new ButtonType("Open Folder", ButtonBar.ButtonData.OK_DONE);
        ButtonType close = new ButtonType("Close", ButtonBar.ButtonData.CANCEL_CLOSE);
        Alert a = new Alert(Alert.AlertType.INFORMATION, msg, open, close);
        a.setHeaderText(null);
        a.setTitle(title);
        a.showAndWait().ifPresent(bt -> {
            if (bt == open && folder != null && folder.exists()) {
                try { Desktop.getDesktop().open(folder); } catch (Exception ignored) {}
            }
        });
    }
    private void error(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR, msg == null ? "" : msg, ButtonType.OK);
        a.setHeaderText(null);
        a.setTitle(title);
        a.showAndWait();
    }
}
