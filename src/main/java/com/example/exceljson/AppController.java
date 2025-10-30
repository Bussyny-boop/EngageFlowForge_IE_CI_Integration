package com.example.exceljson;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class AppController {

    @FXML private Button loadButton;
    @FXML private Button saveExcelButton;
    @FXML private Button saveExcelAsButton;
    @FXML private Button generateNurseJsonButton;
    @FXML private Button generateClinicalJsonButton;
    @FXML private Button exportNurseJsonButton;
    @FXML private Button exportClinicalJsonButton;
    @FXML private TextArea jsonPreview;
    @FXML private Label statusLabel;

    // Units
    @FXML private TableView<ExcelParserV5.UnitRow> tableUnits;
    @FXML private TableColumn<ExcelParserV5.UnitRow, String> unitFacilityCol;
    @FXML private TableColumn<ExcelParserV5.UnitRow, String> unitNamesCol;
    @FXML private TableColumn<ExcelParserV5.UnitRow, String> unitNurseGroupCol;
    @FXML private TableColumn<ExcelParserV5.UnitRow, String> unitClinicalGroupCol;
    @FXML private TableColumn<ExcelParserV5.UnitRow, String> unitNoCareGroupCol;
    @FXML private TableColumn<ExcelParserV5.UnitRow, String> unitCommentsCol;

    // NurseCalls
    @FXML private TableView<ExcelParserV5.FlowRow> tableNurseCalls;
    @FXML private TableColumn<ExcelParserV5.FlowRow, String> nurseConfigGroupCol;
    @FXML private TableColumn<ExcelParserV5.FlowRow, String> nurseAlarmNameCol;
    @FXML private TableColumn<ExcelParserV5.FlowRow, String> nurseSendingNameCol;
    @FXML private TableColumn<ExcelParserV5.FlowRow, String> nursePriorityCol;
    @FXML private TableColumn<ExcelParserV5.FlowRow, String> nurseDeviceACol;
    @FXML private TableColumn<ExcelParserV5.FlowRow, String> nurseRingtoneCol;
    @FXML private TableColumn<ExcelParserV5.FlowRow, String> nurseT1Col;
    @FXML private TableColumn<ExcelParserV5.FlowRow, String> nurseR1Col;
    @FXML private TableColumn<ExcelParserV5.FlowRow, String> nurseT2Col;
    @FXML private TableColumn<ExcelParserV5.FlowRow, String> nurseR2Col;
    @FXML private TableColumn<ExcelParserV5.FlowRow, String> nurseT3Col;
    @FXML private TableColumn<ExcelParserV5.FlowRow, String> nurseR3Col;
    @FXML private TableColumn<ExcelParserV5.FlowRow, String> nurseT4Col;
    @FXML private TableColumn<ExcelParserV5.FlowRow, String> nurseR4Col;
    @FXML private TableColumn<ExcelParserV5.FlowRow, String> nurseT5Col;
    @FXML private TableColumn<ExcelParserV5.FlowRow, String> nurseR5Col;

    // Clinicals
    @FXML private TableView<ExcelParserV5.FlowRow> tableClinicals;
    @FXML private TableColumn<ExcelParserV5.FlowRow, String> clinicalConfigGroupCol;
    @FXML private TableColumn<ExcelParserV5.FlowRow, String> clinicalAlarmNameCol;
    @FXML private TableColumn<ExcelParserV5.FlowRow, String> clinicalSendingNameCol;
    @FXML private TableColumn<ExcelParserV5.FlowRow, String> clinicalPriorityCol;
    @FXML private TableColumn<ExcelParserV5.FlowRow, String> clinicalDeviceACol;
    @FXML private TableColumn<ExcelParserV5.FlowRow, String> clinicalRingtoneCol;
    @FXML private TableColumn<ExcelParserV5.FlowRow, String> clinicalT1Col;
    @FXML private TableColumn<ExcelParserV5.FlowRow, String> clinicalR1Col;
    @FXML private TableColumn<ExcelParserV5.FlowRow, String> clinicalT2Col;
    @FXML private TableColumn<ExcelParserV5.FlowRow, String> clinicalR2Col;
    @FXML private TableColumn<ExcelParserV5.FlowRow, String> clinicalT3Col;
    @FXML private TableColumn<ExcelParserV5.FlowRow, String> clinicalR3Col;
    @FXML private TableColumn<ExcelParserV5.FlowRow, String> clinicalT4Col;
    @FXML private TableColumn<ExcelParserV5.FlowRow, String> clinicalR4Col;
    @FXML private TableColumn<ExcelParserV5.FlowRow, String> clinicalT5Col;
    @FXML private TableColumn<ExcelParserV5.FlowRow, String> clinicalR5Col;

    private ExcelParserV5 parser;
    private File currentExcelFile;
    private String lastGeneratedJson = "";

    @FXML
    public void initialize() {
        parser = new ExcelParserV5();

        initializeUnitColumns();
        initializeNurseColumns();
        initializeClinicalColumns();

        setJsonButtonsEnabled(false);
        setExcelButtonsEnabled(false);

        loadButton.setOnAction(e -> loadExcel());
        if (saveExcelButton != null) saveExcelButton.setOnAction(e -> saveExcel());
        if (saveExcelAsButton != null) saveExcelAsButton.setOnAction(e -> saveExcelAs());
        generateNurseJsonButton.setOnAction(e -> generateJson(true));
        generateClinicalJsonButton.setOnAction(e -> generateJson(false));
        exportNurseJsonButton.setOnAction(e -> exportJson(true));
        exportClinicalJsonButton.setOnAction(e -> exportJson(false));
    }

    // -------------------- LOAD EXCEL --------------------
    private void loadExcel() {
        try {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Select Excel Workbook");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel Files", "*.xlsx"));
            File file = chooser.showOpenDialog(getStage());
            if (file == null) return;

            parser.load(file);
            currentExcelFile = file;

            String summary = parser.getLoadSummary();
            jsonPreview.setText(summary);
            statusLabel.setText("Excel loaded: " + file.getName());
            refreshTables();
            setJsonButtonsEnabled(true);
            setExcelButtonsEnabled(true);
            showInfo("✅ Excel loaded successfully");

        } catch (Exception ex) {
            showError("Failed to load Excel: " + ex.getMessage());
        }
    }

    // -------------------- SAVE (Excel) --------------------
    private void saveExcel() {
        try {
            if (parser == null) {
                showError("Please load and edit an Excel file first.");
                return;
            }
            if (currentExcelFile == null) {
                saveExcelAs();
                return;
            }

            parser.writeExcel(currentExcelFile);
            showInfo("💾 Excel saved to:\n" + currentExcelFile.getAbsolutePath());
        } catch (Exception ex) {
            showError("Error saving Excel: " + ex.getMessage());
        }
    }

    private void saveExcelAs() {
        try {
            if (parser == null) {
                showError("Please load and edit an Excel file first.");
                return;
            }

            syncEditsToParser();

            FileChooser chooser = new FileChooser();
            chooser.setTitle("Save As (Excel)");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel Files", "*.xlsx"));
            chooser.setInitialFileName("Edited_EngageRules.xlsx");
            File out = chooser.showSaveDialog(getStage());
            if (out == null) return;

            parser.writeExcel(out);
            currentExcelFile = out;
            setExcelButtonsEnabled(true);
            showInfo("💾 Excel saved to:\n" + out.getAbsolutePath());
        } catch (Exception ex) {
            showError("Error saving Excel: " + ex.getMessage());
        }
    }

    // -------------------- GENERATE JSON --------------------
    private void generateJson(boolean nurseSide) {
        try {
            if (parser == null) {
                showError("Please load an Excel file first.");
                return;
            }

            syncEditsToParser();

            if (nurseSide) {
                var json = ExcelParserV5.pretty(parser.buildNurseCallsJson());
                jsonPreview.setText(json);
                statusLabel.setText("Generated NurseCall JSON");
                lastGeneratedJson = json;
            } else {
                var json = ExcelParserV5.pretty(parser.buildClinicalsJson());
                jsonPreview.setText(json);
                statusLabel.setText("Generated Clinical JSON");
                lastGeneratedJson = json;
            }

        } catch (Exception ex) {
            showError("Error generating JSON: " + ex.getMessage());
        }
    }

    // -------------------- EXPORT JSON --------------------
    private void exportJson(boolean nurseSide) {
        try {
            if (parser == null) {
                showError("Please load an Excel file first.");
                return;
            }

            syncEditsToParser();

            FileChooser chooser = new FileChooser();
            chooser.setTitle(nurseSide ? "Export NurseCall JSON" : "Export Clinical JSON");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON Files", "*.json"));
            chooser.setInitialFileName(nurseSide ? "NurseCalls.json" : "Clinicals.json");

            File file = chooser.showSaveDialog(getStage());
            if (file == null) return;

            if (nurseSide) parser.writeNurseCallsJson(file); else parser.writeClinicalsJson(file);

            showInfo("✅ JSON saved to:\n" + file.getAbsolutePath());
        } catch (Exception ex) {
            showError("Error exporting JSON: " + ex.getMessage());
        }
    }

    // -------------------- HELPERS --------------------
    private void setJsonButtonsEnabled(boolean enabled) {
        generateNurseJsonButton.setDisable(!enabled);
        generateClinicalJsonButton.setDisable(!enabled);
        exportNurseJsonButton.setDisable(!enabled);
        exportClinicalJsonButton.setDisable(!enabled);
    }

    private void setExcelButtonsEnabled(boolean enabled) {
        if (saveExcelButton != null) saveExcelButton.setDisable(!enabled);
        if (saveExcelAsButton != null) saveExcelAsButton.setDisable(!enabled);
    }

    // -------------------- SYNC CURRENT TABLE DATA BACK TO PARSER --------------------
    private void syncEditsToParser() {
        if (parser == null) return;

        if (tableUnits != null && tableUnits.getItems() != null) {
            parser.units.clear();
            parser.units.addAll(tableUnits.getItems());
        }
        if (tableNurseCalls != null && tableNurseCalls.getItems() != null) {
            parser.nurseCalls.clear();
            parser.nurseCalls.addAll(tableNurseCalls.getItems());
        }
        if (tableClinicals != null && tableClinicals.getItems() != null) {
            parser.clinicals.clear();
            parser.clinicals.addAll(tableClinicals.getItems());
        }
    }

    private void showInfo(String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Info");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }

    private void showError(String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText("Operation Failed");
        alert.setContentText(msg);
        alert.showAndWait();
    }

    private Stage getStage() {
        return (Stage) jsonPreview.getScene().getWindow();
    }

    // ====== Column initializers ======

    private void initializeUnitColumns() {
        if (tableUnits != null) tableUnits.setEditable(true);

        setupEditable(unitFacilityCol, r -> safe(r.facility), (r, v) -> r.facility = safe(v));
        setupEditable(unitNamesCol, r -> safe(r.unitNames), (r, v) -> r.unitNames = safe(v));
        setupEditable(unitNurseGroupCol, r -> safe(r.nurseGroup), (r, v) -> r.nurseGroup = safe(v));
        setupEditable(unitClinicalGroupCol, r -> safe(r.clinGroup), (r, v) -> r.clinGroup = safe(v));
        setupEditable(unitNoCareGroupCol, r -> safe(r.noCareGroup), (r, v) -> r.noCareGroup = safe(v));
        setupEditable(unitCommentsCol, r -> safe(r.comments), (r, v) -> r.comments = safe(v));
    }

    private void initializeNurseColumns() {
        if (tableNurseCalls != null) tableNurseCalls.setEditable(true);

        setupEditable(nurseConfigGroupCol, f -> safe(f.configGroup), (f, v) -> f.configGroup = safe(v));
        setupEditable(nurseAlarmNameCol, f -> safe(f.alarmName), (f, v) -> f.alarmName = safe(v));
        setupEditable(nurseSendingNameCol, f -> safe(f.sendingName), (f, v) -> f.sendingName = safe(v));
        setupEditable(nursePriorityCol, f -> safe(f.priorityRaw), (f, v) -> f.priorityRaw = safe(v));
        setupEditable(nurseDeviceACol, f -> safe(f.deviceA), (f, v) -> f.deviceA = safe(v));
        setupEditable(nurseRingtoneCol, f -> safe(f.ringtone), (f, v) -> f.ringtone = safe(v));

        setupEditable(nurseT1Col, f -> safe(f.t1), (f, v) -> f.t1 = safe(v));
        setupEditable(nurseR1Col, f -> safe(f.r1), (f, v) -> f.r1 = safe(v));
        setupEditable(nurseT2Col, f -> safe(f.t2), (f, v) -> f.t2 = safe(v));
        setupEditable(nurseR2Col, f -> safe(f.r2), (f, v) -> f.r2 = safe(v));
        setupEditable(nurseT3Col, f -> safe(f.t3), (f, v) -> f.t3 = safe(v));
        setupEditable(nurseR3Col, f -> safe(f.r3), (f, v) -> f.r3 = safe(v));
        setupEditable(nurseT4Col, f -> safe(f.t4), (f, v) -> f.t4 = safe(v));
        setupEditable(nurseR4Col, f -> safe(f.r4), (f, v) -> f.r4 = safe(v));
        setupEditable(nurseT5Col, f -> safe(f.t5), (f, v) -> f.t5 = safe(v));
        setupEditable(nurseR5Col, f -> safe(f.r5), (f, v) -> f.r5 = safe(v));
    }

    private void initializeClinicalColumns() {
        if (tableClinicals != null) tableClinicals.setEditable(true);

        setupEditable(clinicalConfigGroupCol, f -> safe(f.configGroup), (f, v) -> f.configGroup = safe(v));
        setupEditable(clinicalAlarmNameCol, f -> safe(f.alarmName), (f, v) -> f.alarmName = safe(v));
        setupEditable(clinicalSendingNameCol, f -> safe(f.sendingName), (f, v) -> f.sendingName = safe(v));
        setupEditable(clinicalPriorityCol, f -> safe(f.priorityRaw), (f, v) -> f.priorityRaw = safe(v));
        setupEditable(clinicalDeviceACol, f -> safe(f.deviceA), (f, v) -> f.deviceA = safe(v));
        setupEditable(clinicalRingtoneCol, f -> safe(f.ringtone), (f, v) -> f.ringtone = safe(v));

        setupEditable(clinicalT1Col, f -> safe(f.t1), (f, v) -> f.t1 = safe(v));
        setupEditable(clinicalR1Col, f -> safe(f.r1), (f, v) -> f.r1 = safe(v));
        setupEditable(clinicalT2Col, f -> safe(f.t2), (f, v) -> f.t2 = safe(v));
        setupEditable(clinicalR2Col, f -> safe(f.r2), (f, v) -> f.r2 = safe(v));
        setupEditable(clinicalT3Col, f -> safe(f.t3), (f, v) -> f.t3 = safe(v));
        setupEditable(clinicalR3Col, f -> safe(f.r3), (f, v) -> f.r3 = safe(v));
        setupEditable(clinicalT4Col, f -> safe(f.t4), (f, v) -> f.t4 = safe(v));
        setupEditable(clinicalR4Col, f -> safe(f.r4), (f, v) -> f.r4 = safe(v));
        setupEditable(clinicalT5Col, f -> safe(f.t5), (f, v) -> f.t5 = safe(v));
        setupEditable(clinicalR5Col, f -> safe(f.r5), (f, v) -> f.r5 = safe(v));
    }

    // Generic helper to wire editable TextField columns that commit on Enter/defocus
    private <R> void setupEditable(TableColumn<R, String> col,
                                   Function<R, String> getter,
                                   BiConsumer<R, String> setter) {
        if (col == null) return;
        col.setCellValueFactory(data -> new SimpleStringProperty(safe(getter.apply(data.getValue()))));
        col.setCellFactory(TextFieldTableCell.forTableColumn());
        col.setOnEditCommit(ev -> {
            R row = ev.getRowValue();
            setter.accept(row, ev.getNewValue());
            if (col.getTableView() != null) col.getTableView().refresh();
        });
    }

    private void refreshTables() {
        if (tableUnits != null) {
            tableUnits.setItems(FXCollections.observableArrayList(parser.units));
        }
        if (tableNurseCalls != null) {
            tableNurseCalls.setItems(FXCollections.observableArrayList(parser.nurseCalls));
        }
        if (tableClinicals != null) {
            tableClinicals.setItems(FXCollections.observableArrayList(parser.clinicals));
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
