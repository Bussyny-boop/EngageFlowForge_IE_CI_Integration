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

    // ---------- UI Elements ----------
    @FXML private Button loadButton;
    @FXML private Button saveExcelButton;
    @FXML private Button saveExcelAsButton;
    @FXML private Button generateNurseJsonButton;
    @FXML private Button generateClinicalJsonButton;
    @FXML private Button exportNurseJsonButton;
    @FXML private Button exportClinicalJsonButton;
    @FXML private CheckBox mergeFlowsCheckbox;
    @FXML private TextArea jsonPreview;
    @FXML private Label statusLabel;

    // ---------- Units ----------
    @FXML private TableView<ExcelParserV5.UnitRow> tableUnits;
    @FXML private TableColumn<ExcelParserV5.UnitRow, String> unitFacilityCol;
    @FXML private TableColumn<ExcelParserV5.UnitRow, String> unitNamesCol;
    @FXML private TableColumn<ExcelParserV5.UnitRow, String> unitNurseGroupCol;
    @FXML private TableColumn<ExcelParserV5.UnitRow, String> unitClinicalGroupCol;
    @FXML private TableColumn<ExcelParserV5.UnitRow, String> unitNoCareGroupCol;
    @FXML private TableColumn<ExcelParserV5.UnitRow, String> unitCommentsCol;

    // ---------- Nurse Calls ----------
    @FXML private TableView<ExcelParserV5.FlowRow> tableNurseCalls;
    @FXML private TableColumn<ExcelParserV5.FlowRow, String> nurseConfigGroupCol;
    @FXML private TableColumn<ExcelParserV5.FlowRow, String> nurseAlarmNameCol;
    @FXML private TableColumn<ExcelParserV5.FlowRow, String> nurseSendingNameCol;
    @FXML private TableColumn<ExcelParserV5.FlowRow, String> nursePriorityCol;
    @FXML private TableColumn<ExcelParserV5.FlowRow, String> nurseDeviceACol;
    @FXML private TableColumn<ExcelParserV5.FlowRow, String> nurseRingtoneCol;
    @FXML private TableColumn<ExcelParserV5.FlowRow, String> nurseResponseOptionsCol;
    @FXML private TableColumn<ExcelParserV5.FlowRow, String> nurseBreakThroughDNDCol;
    @FXML private TableColumn<ExcelParserV5.FlowRow, String> nurseEscalateAfterCol;
    @FXML private TableColumn<ExcelParserV5.FlowRow, String> nurseTtlValueCol;
    @FXML private TableColumn<ExcelParserV5.FlowRow, String> nurseEnunciateCol;
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

    // ---------- Clinicals ----------
    @FXML private TableView<ExcelParserV5.FlowRow> tableClinicals;
    @FXML private TableColumn<ExcelParserV5.FlowRow, String> clinicalConfigGroupCol;
    @FXML private TableColumn<ExcelParserV5.FlowRow, String> clinicalAlarmNameCol;
    @FXML private TableColumn<ExcelParserV5.FlowRow, String> clinicalSendingNameCol;
    @FXML private TableColumn<ExcelParserV5.FlowRow, String> clinicalPriorityCol;
    @FXML private TableColumn<ExcelParserV5.FlowRow, String> clinicalDeviceACol;
    @FXML private TableColumn<ExcelParserV5.FlowRow, String> clinicalRingtoneCol;
    @FXML private TableColumn<ExcelParserV5.FlowRow, String> clinicalResponseOptionsCol;
    @FXML private TableColumn<ExcelParserV5.FlowRow, String> clinicalBreakThroughDNDCol;
    @FXML private TableColumn<ExcelParserV5.FlowRow, String> clinicalEscalateAfterCol;
    @FXML private TableColumn<ExcelParserV5.FlowRow, String> clinicalTtlValueCol;
    @FXML private TableColumn<ExcelParserV5.FlowRow, String> clinicalEnunciateCol;
    @FXML private TableColumn<ExcelParserV5.FlowRow, String> clinicalEmdanCol;
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

    // ---------- Core ----------
    private ExcelParserV5 parser;
    private File currentExcelFile;
    private String lastGeneratedJson = "";

    // ---------- Initialization ----------
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

    // ---------- Load Excel ----------
    private void loadExcel() {
        try {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Select Excel Workbook");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel Files", "*.xlsx"));
            File file = chooser.showOpenDialog(getStage());
            if (file == null) return;

            parser.load(file);
            currentExcelFile = file;

            jsonPreview.setText(parser.getLoadSummary());
            
            int movedCount = parser.getEmdanMovedCount();
            String rowText = (movedCount == 1) ? " EMDAN row moved" : " EMDAN rows moved";
            statusLabel.setText("Loaded Excel. " + movedCount + rowText + " to Clinical tab.");

            refreshTables();
            tableUnits.refresh();
            tableNurseCalls.refresh();
            tableClinicals.refresh();

            setJsonButtonsEnabled(true);
            setExcelButtonsEnabled(true);
            showInfo("✅ Excel loaded successfully");
        } catch (Exception ex) {
            showError("Failed to load Excel: " + ex.getMessage());
        }
    }

    // ---------- Save Excel ----------
    private void saveExcel() {
        // Always prompt for a new file location
        saveExcelAs();
    }

    // ---------- Save As (Generated) ----------
    private void saveExcelAs() {
        try {
            if (parser == null) {
                showError("Please load and edit an Excel file first.");
                return;
            }
            syncEditsToParser();

            // Default name suggestion
            String baseName = "Edited_EngageRules";
            if (currentExcelFile != null) {
                String name = currentExcelFile.getName();
                if (name.toLowerCase().endsWith(".xlsx")) name = name.substring(0, name.length() - 5);
                baseName = name;
            }
            String newName = baseName + "_Generated.xlsx";

            FileChooser chooser = new FileChooser();
            chooser.setTitle("Save Generated Excel");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel Files", "*.xlsx"));
            chooser.setInitialFileName(newName);
            File out = chooser.showSaveDialog(getStage());
            if (out == null) return;

            if (out.exists()) {
                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                confirm.setTitle("Overwrite File?");
                confirm.setHeaderText("File already exists");
                confirm.setContentText("The file \"" + out.getName() + "\" already exists. Overwrite?");
                if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;
            }

            parser.writeExcel(out);
            showInfo("💾 Excel generated successfully:\n" + out.getAbsolutePath());
        } catch (Exception ex) {
            showError("Error saving Excel: " + ex.getMessage());
        }
    }

    // ---------- Generate JSON ----------
    private void generateJson(boolean nurseSide) {
        try {
            if (parser == null) {
                showError("Please load an Excel file first.");
                return;
            }

            syncEditsToParser(); // always sync before generating

            boolean useAdvanced = (mergeFlowsCheckbox != null && mergeFlowsCheckbox.isSelected());

            String json = nurseSide
                    ? ExcelParserV5.pretty(parser.buildNurseCallsJson(useAdvanced))
                    : ExcelParserV5.pretty(parser.buildClinicalsJson(useAdvanced));

            jsonPreview.setText(json);
            statusLabel.setText(nurseSide ? "Generated NurseCall JSON" : "Generated Clinical JSON");
            lastGeneratedJson = json;
        } catch (Exception ex) {
            showError("Error generating JSON: " + ex.getMessage());
        }
    }

    // ---------- Export JSON ----------
    private void exportJson(boolean nurseSide) {
        try {
            if (parser == null) {
                showError("Please load an Excel file first.");
                return;
            }

            syncEditsToParser();

            boolean useAdvanced = (mergeFlowsCheckbox != null && mergeFlowsCheckbox.isSelected());

            FileChooser chooser = new FileChooser();
            chooser.setTitle(nurseSide ? "Export NurseCall JSON" : "Export Clinical JSON");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON Files", "*.json"));
            chooser.setInitialFileName(nurseSide ? "NurseCalls.json" : "Clinicals.json");

            File file = chooser.showSaveDialog(getStage());
            if (file == null) return;

            if (nurseSide) parser.writeNurseCallsJson(file, useAdvanced);
            else parser.writeClinicalsJson(file, useAdvanced);

            if (useAdvanced) {
                statusLabel.setText("Exported Merged JSON (Advanced Mode)");
            } else {
                statusLabel.setText("Exported Standard JSON");
            }

            showInfo("✅ JSON saved to:\n" + file.getAbsolutePath());
        } catch (Exception ex) {
            showError("Error exporting JSON: " + ex.getMessage());
        }
    }

    // ---------- Helpers ----------
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

    // ---------- Sync table edits back to parser ----------
    private void syncEditsToParser() {
        if (parser == null) return;
        if (tableUnits != null && tableUnits.getItems() != null) {
            parser.units.clear();
            parser.units.addAll(tableUnits.getItems());
            parser.rebuildUnitMaps();
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

    // ---------- Table column setup ----------
    private void initializeUnitColumns() {
        if (tableUnits != null) tableUnits.setEditable(true);
        setupEditable(unitFacilityCol, r -> r.facility, (r, v) -> r.facility = v);
        setupEditable(unitNamesCol, r -> r.unitNames, (r, v) -> r.unitNames = v);
        setupEditable(unitNurseGroupCol, r -> r.nurseGroup, (r, v) -> r.nurseGroup = v);
        setupEditable(unitClinicalGroupCol, r -> r.clinGroup, (r, v) -> r.clinGroup = v);
        setupEditable(unitNoCareGroupCol, r -> r.noCareGroup, (r, v) -> r.noCareGroup = v);
        setupEditable(unitCommentsCol, r -> r.comments, (r, v) -> r.comments = v);
    }

    private void initializeNurseColumns() {
        if (tableNurseCalls != null) tableNurseCalls.setEditable(true);
        setupEditable(nurseConfigGroupCol, f -> f.configGroup, (f, v) -> f.configGroup = v);
        setupEditable(nurseAlarmNameCol, f -> f.alarmName, (f, v) -> f.alarmName = v);
        setupEditable(nurseSendingNameCol, f -> f.sendingName, (f, v) -> f.sendingName = v);
        setupEditable(nursePriorityCol, f -> f.priorityRaw, (f, v) -> f.priorityRaw = v);
        setupEditable(nurseDeviceACol, f -> f.deviceA, (f, v) -> f.deviceA = v);
        setupEditable(nurseRingtoneCol, f -> f.ringtone, (f, v) -> f.ringtone = v);
        setupEditable(nurseResponseOptionsCol, f -> safe(f.responseOptions), (f, v) -> f.responseOptions = safe(v));
        setupEditable(nurseBreakThroughDNDCol, f -> safe(f.breakThroughDND), (f, v) -> f.breakThroughDND = safe(v));
        setupEditable(nurseEscalateAfterCol, f -> safe(f.escalateAfter), (f, v) -> f.escalateAfter = safe(v));
        setupEditable(nurseTtlValueCol, f -> safe(f.ttlValue), (f, v) -> f.ttlValue = safe(v));
        setupEditable(nurseEnunciateCol, f -> safe(f.enunciate), (f, v) -> f.enunciate = safe(v));
        setupEditable(nurseT1Col, f -> f.t1, (f, v) -> f.t1 = v);
        setupEditable(nurseR1Col, f -> f.r1, (f, v) -> f.r1 = v);
        setupEditable(nurseT2Col, f -> f.t2, (f, v) -> f.t2 = v);
        setupEditable(nurseR2Col, f -> f.r2, (f, v) -> f.r2 = v);
        setupEditable(nurseT3Col, f -> f.t3, (f, v) -> f.t3 = v);
        setupEditable(nurseR3Col, f -> f.r3, (f, v) -> f.r3 = v);
        setupEditable(nurseT4Col, f -> f.t4, (f, v) -> f.t4 = v);
        setupEditable(nurseR4Col, f -> f.r4, (f, v) -> f.r4 = v);
        setupEditable(nurseT5Col, f -> f.t5, (f, v) -> f.t5 = v);
        setupEditable(nurseR5Col, f -> f.r5, (f, v) -> f.r5 = v);
    }

    private void initializeClinicalColumns() {
        if (tableClinicals != null) tableClinicals.setEditable(true);
        setupEditable(clinicalConfigGroupCol, f -> f.configGroup, (f, v) -> f.configGroup = v);
        setupEditable(clinicalAlarmNameCol, f -> f.alarmName, (f, v) -> f.alarmName = v);
        setupEditable(clinicalSendingNameCol, f -> f.sendingName, (f, v) -> f.sendingName = v);
        setupEditable(clinicalPriorityCol, f -> f.priorityRaw, (f, v) -> f.priorityRaw = v);
        setupEditable(clinicalDeviceACol, f -> f.deviceA, (f, v) -> f.deviceA = v);
        setupEditable(clinicalRingtoneCol, f -> f.ringtone, (f, v) -> f.ringtone = v);
        setupEditable(clinicalResponseOptionsCol, f -> safe(f.responseOptions), (f, v) -> f.responseOptions = safe(v));
        setupEditable(clinicalBreakThroughDNDCol, f -> safe(f.breakThroughDND), (f, v) -> f.breakThroughDND = safe(v));
        setupEditable(clinicalEscalateAfterCol, f -> safe(f.escalateAfter), (f, v) -> f.escalateAfter = safe(v));
        setupEditable(clinicalTtlValueCol, f -> safe(f.ttlValue), (f, v) -> f.ttlValue = safe(v));
        setupEditable(clinicalEnunciateCol, f -> safe(f.enunciate), (f, v) -> f.enunciate = safe(v));
        setupEditable(clinicalEmdanCol, f -> safe(f.emdan), (f, v) -> f.emdan = safe(v));
        setupEditable(clinicalT1Col, f -> f.t1, (f, v) -> f.t1 = v);
        setupEditable(clinicalR1Col, f -> f.r1, (f, v) -> f.r1 = v);
        setupEditable(clinicalT2Col, f -> f.t2, (f, v) -> f.t2 = v);
        setupEditable(clinicalR2Col, f -> f.r2, (f, v) -> f.r2 = v);
        setupEditable(clinicalT3Col, f -> f.t3, (f, v) -> f.t3 = v);
        setupEditable(clinicalR3Col, f -> f.r3, (f, v) -> f.r3 = v);
        setupEditable(clinicalT4Col, f -> f.t4, (f, v) -> f.t4 = v);
        setupEditable(clinicalR4Col, f -> f.r4, (f, v) -> f.r4 = v);
        setupEditable(clinicalT5Col, f -> f.t5, (f, v) -> f.t5 = v);
        setupEditable(clinicalR5Col, f -> f.r5, (f, v) -> f.r5 = v);
    }

    private <R> void setupEditable(TableColumn<R, String> col, Function<R, String> getter, BiConsumer<R, String> setter) {
        if (col == null) return;
        col.setCellValueFactory(d -> new SimpleStringProperty(safe(getter.apply(d.getValue()))));
        col.setCellFactory(TextFieldTableCell.forTableColumn());
        col.setOnEditCommit(ev -> {
            R row = ev.getRowValue();
            setter.accept(row, ev.getNewValue());
            if (col.getTableView() != null) col.getTableView().refresh();
        });
    }

    private void refreshTables() {
        if (tableUnits != null) tableUnits.setItems(FXCollections.observableArrayList(parser.units));
        if (tableNurseCalls != null) tableNurseCalls.setItems(FXCollections.observableArrayList(parser.nurseCalls));
        if (tableClinicals != null) tableClinicals.setItems(FXCollections.observableArrayList(parser.clinicals));
    }

    private static String safe(String v) { return v == null ? "" : v; }
}
