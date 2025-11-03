package com.example.exceljson;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.prefs.Preferences;

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
    @FXML private TextField edgeRefNameField;
    @FXML private TextField vcsRefNameField;
    @FXML private Button resetDefaultsButton;
    @FXML private Button resetPathsButton;
    @FXML private TextArea jsonPreview;
    @FXML private Label statusLabel;

    // ---------- Config Group Filters ----------
    @FXML private ComboBox<String> unitConfigGroupFilter;
    @FXML private ComboBox<String> nurseConfigGroupFilter;
    @FXML private ComboBox<String> clinicalConfigGroupFilter;

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
    @FXML private TableColumn<ExcelParserV5.FlowRow, String> nurseDeviceBCol;
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
    @FXML private TableColumn<ExcelParserV5.FlowRow, String> clinicalDeviceBCol;
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
    private boolean lastGeneratedWasNurseSide = true; // Track last generated JSON type

    // ---------- Filtered Lists for Tables ----------
    private ObservableList<ExcelParserV5.UnitRow> unitsFullList;
    private FilteredList<ExcelParserV5.UnitRow> unitsFilteredList;
    
    private ObservableList<ExcelParserV5.FlowRow> nurseCallsFullList;
    private FilteredList<ExcelParserV5.FlowRow> nurseCallsFilteredList;
    
    private ObservableList<ExcelParserV5.FlowRow> clinicalsFullList;
    private FilteredList<ExcelParserV5.FlowRow> clinicalsFilteredList;

    // ---------- Directory Persistence ----------
    private File lastExcelDir = null;
    private File lastJsonDir = null;

    private static final String PREF_KEY_LAST_EXCEL_DIR = "lastExcelDir";
    private static final String PREF_KEY_LAST_JSON_DIR = "lastJsonDir";

    // ---------- Initialization ----------
    @FXML
    public void initialize() {
        parser = new ExcelParserV5();

        // Load saved directories from preferences
        Preferences prefs = Preferences.userNodeForPackage(AppController.class);
        String excelDirPath = prefs.get(PREF_KEY_LAST_EXCEL_DIR, null);
        String jsonDirPath = prefs.get(PREF_KEY_LAST_JSON_DIR, null);

        if (excelDirPath != null) {
            lastExcelDir = new File(excelDirPath);
        }
        if (jsonDirPath != null) {
            lastJsonDir = new File(jsonDirPath);
        }

        initializeUnitColumns();
        initializeNurseColumns();
        initializeClinicalColumns();
        initializeFilters();

        // Set default reference names
        if (edgeRefNameField != null) edgeRefNameField.setText("OutgoingWCTP");
        if (vcsRefNameField != null) vcsRefNameField.setText("VMP");

        setJsonButtonsEnabled(false);
        setExcelButtonsEnabled(false);

        loadButton.setOnAction(e -> loadExcel());
        if (saveExcelButton != null) saveExcelButton.setOnAction(e -> saveExcel());
        if (saveExcelAsButton != null) saveExcelAsButton.setOnAction(e -> saveExcelAs());
        generateNurseJsonButton.setOnAction(e -> generateJson(true));
        generateClinicalJsonButton.setOnAction(e -> generateJson(false));
        exportNurseJsonButton.setOnAction(e -> exportJson(true));
        exportClinicalJsonButton.setOnAction(e -> exportJson(false));
        if (resetDefaultsButton != null) resetDefaultsButton.setOnAction(e -> resetDefaults());
        if (resetPathsButton != null) resetPathsButton.setOnAction(e -> resetPaths());

        // --- Interface reference name bindings ---
        // Update parser whenever text changes or Enter is pressed
        if (edgeRefNameField != null) {
            edgeRefNameField.setOnAction(e -> updateInterfaceRefs());
        }
        if (vcsRefNameField != null) {
            vcsRefNameField.setOnAction(e -> updateInterfaceRefs());
        }

        // Also update parser when focus is lost (user tabs or clicks away)
        if (edgeRefNameField != null) {
            edgeRefNameField.focusedProperty().addListener((obs, oldV, newV) -> { 
                if (!newV) updateInterfaceRefs(); 
            });
        }
        if (vcsRefNameField != null) {
            vcsRefNameField.focusedProperty().addListener((obs, oldV, newV) -> { 
                if (!newV) updateInterfaceRefs(); 
            });
        }
    }

    // ---------- Load Excel ----------
    private void loadExcel() {
        try {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Select Excel Workbook");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel Files", "*.xlsx"));

            if (lastExcelDir != null && lastExcelDir.exists()) {
                chooser.setInitialDirectory(lastExcelDir);
            }

            File file = chooser.showOpenDialog(getStage());
            if (file == null) return;

            // Remember directory
            rememberDirectory(file, true);

            parser.load(file);
            currentExcelFile = file;

            jsonPreview.setText(parser.getLoadSummary());

            refreshTables();
            tableUnits.refresh();
            tableNurseCalls.refresh();
            tableClinicals.refresh();

            setJsonButtonsEnabled(true);
            setExcelButtonsEnabled(true);
            
            updateStatusLabel(); // Update status with filter counts
            
            int movedCount = parser.getEmdanMovedCount();
            if (movedCount > 0) {
                showInfo("✅ Excel loaded successfully\nMoved " + movedCount + " EMDAN rows to Clinicals");
            } else {
                showInfo("✅ Excel loaded successfully");
            }
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

            if (lastExcelDir != null && lastExcelDir.exists()) {
                chooser.setInitialDirectory(lastExcelDir);
            }

            File out = chooser.showSaveDialog(getStage());
            if (out == null) return;

            // Remember directory
            rememberDirectory(out, true);

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
            applyInterfaceReferences(); // Apply interface references

            boolean useAdvanced = (mergeFlowsCheckbox != null && mergeFlowsCheckbox.isSelected());

            // Create a temporary parser with only filtered data
            ExcelParserV5 filteredParser = createFilteredParser();

            String json = nurseSide
                    ? ExcelParserV5.pretty(filteredParser.buildNurseCallsJson(useAdvanced))
                    : ExcelParserV5.pretty(filteredParser.buildClinicalsJson(useAdvanced));

            jsonPreview.setText(json);
            statusLabel.setText(nurseSide ? "Generated NurseCall JSON" : "Generated Clinical JSON");
            lastGeneratedJson = json;
            lastGeneratedWasNurseSide = nurseSide; // Track the last generated type
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
            applyInterfaceReferences(); // Apply interface references

            boolean useAdvanced = (mergeFlowsCheckbox != null && mergeFlowsCheckbox.isSelected());

            FileChooser chooser = new FileChooser();
            chooser.setTitle(nurseSide ? "Export NurseCall JSON" : "Export Clinical JSON");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON Files", "*.json"));
            chooser.setInitialFileName(nurseSide ? "NurseCalls.json" : "Clinicals.json");

            if (lastJsonDir != null && lastJsonDir.exists()) {
                chooser.setInitialDirectory(lastJsonDir);
            }

            File file = chooser.showSaveDialog(getStage());
            if (file == null) return;

            // Remember directory
            rememberDirectory(file, false);

            // Create a temporary parser with only filtered data
            ExcelParserV5 filteredParser = createFilteredParser();

            if (nurseSide) filteredParser.writeNurseCallsJson(file, useAdvanced);
            else filteredParser.writeClinicalsJson(file, useAdvanced);

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
    // Create a parser with only the filtered data
    private ExcelParserV5 createFilteredParser() {
        ExcelParserV5 filteredParser = new ExcelParserV5();
        
        // Copy interface references
        filteredParser.setInterfaceReferences(
            edgeRefNameField != null ? edgeRefNameField.getText().trim() : "OutgoingWCTP",
            vcsRefNameField != null ? vcsRefNameField.getText().trim() : "VMP"
        );
        
        // Add filtered units
        if (unitsFilteredList != null) {
            filteredParser.units.addAll(unitsFilteredList);
            filteredParser.rebuildUnitMaps();
        }
        
        // Add filtered nurse calls
        if (nurseCallsFilteredList != null) {
            filteredParser.nurseCalls.addAll(nurseCallsFilteredList);
        }
        
        // Add filtered clinicals
        if (clinicalsFilteredList != null) {
            filteredParser.clinicals.addAll(clinicalsFilteredList);
        }
        
        return filteredParser;
    }

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
        if (unitsFullList != null) {
            parser.units.clear();
            parser.units.addAll(unitsFullList);
            parser.rebuildUnitMaps();
        }
        if (nurseCallsFullList != null) {
            parser.nurseCalls.clear();
            parser.nurseCalls.addAll(nurseCallsFullList);
        }
        if (clinicalsFullList != null) {
            parser.clinicals.clear();
            parser.clinicals.addAll(clinicalsFullList);
        }
    }

    private void showInfo(String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Info");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.getDialogPane().setStyle("-fx-font-size: 13px;");
        alert.showAndWait();
    }

    private void showError(String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText("Operation Failed");
        alert.setContentText(msg);
        alert.getDialogPane().setStyle("-fx-font-size: 13px;");
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
        setupEditable(nurseDeviceBCol, f -> f.deviceB, (f, v) -> f.deviceB = v);
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
        setupEditable(clinicalDeviceBCol, f -> f.deviceB, (f, v) -> f.deviceB = v);
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

    // ---------- Initialize Filters ----------
    private void initializeFilters() {
        // Initialize filter ComboBoxes
        if (unitConfigGroupFilter != null) {
            unitConfigGroupFilter.setOnAction(e -> applyUnitFilter());
        }
        if (nurseConfigGroupFilter != null) {
            nurseConfigGroupFilter.setOnAction(e -> applyNurseFilter());
        }
        if (clinicalConfigGroupFilter != null) {
            clinicalConfigGroupFilter.setOnAction(e -> applyClinicalFilter());
        }
    }

    // ---------- Update Filter Options ----------
    private void updateFilterOptions() {
        // Collect unique config groups from Units (both Nurse and Clinical groups)
        Set<String> unitConfigGroups = new LinkedHashSet<>();
        if (parser != null && parser.units != null) {
            for (ExcelParserV5.UnitRow unit : parser.units) {
                if (unit.nurseGroup != null && !unit.nurseGroup.trim().isEmpty()) {
                    unitConfigGroups.add(unit.nurseGroup.trim());
                }
                if (unit.clinGroup != null && !unit.clinGroup.trim().isEmpty()) {
                    unitConfigGroups.add(unit.clinGroup.trim());
                }
            }
        }
        
        // Collect unique config groups from Nurse Calls
        Set<String> nurseConfigGroups = new LinkedHashSet<>();
        if (parser != null && parser.nurseCalls != null) {
            for (ExcelParserV5.FlowRow flow : parser.nurseCalls) {
                if (flow.configGroup != null && !flow.configGroup.trim().isEmpty()) {
                    nurseConfigGroups.add(flow.configGroup.trim());
                }
            }
        }
        
        // Collect unique config groups from Clinicals
        Set<String> clinicalConfigGroups = new LinkedHashSet<>();
        if (parser != null && parser.clinicals != null) {
            for (ExcelParserV5.FlowRow flow : parser.clinicals) {
                if (flow.configGroup != null && !flow.configGroup.trim().isEmpty()) {
                    clinicalConfigGroups.add(flow.configGroup.trim());
                }
            }
        }
        
        // Update Unit filter
        if (unitConfigGroupFilter != null) {
            List<String> unitOptions = new ArrayList<>();
            unitOptions.add("All");
            unitOptions.addAll(unitConfigGroups);
            unitConfigGroupFilter.setItems(FXCollections.observableArrayList(unitOptions));
            unitConfigGroupFilter.getSelectionModel().select(0); // Select "All" by default
        }
        
        // Update Nurse Call filter
        if (nurseConfigGroupFilter != null) {
            List<String> nurseOptions = new ArrayList<>();
            nurseOptions.add("All");
            nurseOptions.addAll(nurseConfigGroups);
            nurseConfigGroupFilter.setItems(FXCollections.observableArrayList(nurseOptions));
            nurseConfigGroupFilter.getSelectionModel().select(0); // Select "All" by default
        }
        
        // Update Clinical filter
        if (clinicalConfigGroupFilter != null) {
            List<String> clinicalOptions = new ArrayList<>();
            clinicalOptions.add("All");
            clinicalOptions.addAll(clinicalConfigGroups);
            clinicalConfigGroupFilter.setItems(FXCollections.observableArrayList(clinicalOptions));
            clinicalConfigGroupFilter.getSelectionModel().select(0); // Select "All" by default
        }
    }

    // ---------- Apply Filters ----------
    private void applyUnitFilter() {
        if (unitsFilteredList == null) return;
        
        String selected = unitConfigGroupFilter.getSelectionModel().getSelectedItem();
        if (selected == null || selected.equals("All")) {
            unitsFilteredList.setPredicate(unit -> true); // Show all
        } else {
            unitsFilteredList.setPredicate(unit -> 
                selected.equals(unit.nurseGroup) || selected.equals(unit.clinGroup)
            );
        }
        
        updateStatusLabel();
    }

    private void applyNurseFilter() {
        if (nurseCallsFilteredList == null) return;
        
        String selected = nurseConfigGroupFilter.getSelectionModel().getSelectedItem();
        if (selected == null || selected.equals("All")) {
            nurseCallsFilteredList.setPredicate(flow -> true); // Show all
        } else {
            nurseCallsFilteredList.setPredicate(flow -> selected.equals(flow.configGroup));
        }
        
        updateStatusLabel();
    }

    private void applyClinicalFilter() {
        if (clinicalsFilteredList == null) return;
        
        String selected = clinicalConfigGroupFilter.getSelectionModel().getSelectedItem();
        if (selected == null || selected.equals("All")) {
            clinicalsFilteredList.setPredicate(flow -> true); // Show all
        } else {
            clinicalsFilteredList.setPredicate(flow -> selected.equals(flow.configGroup));
        }
        
        updateStatusLabel();
    }

    private void updateStatusLabel() {
        if (statusLabel == null) return;
        
        int unitsShown = unitsFilteredList != null ? unitsFilteredList.size() : 0;
        int unitsTotal = unitsFullList != null ? unitsFullList.size() : 0;
        int nurseShown = nurseCallsFilteredList != null ? nurseCallsFilteredList.size() : 0;
        int nurseTotal = nurseCallsFullList != null ? nurseCallsFullList.size() : 0;
        int clinicalShown = clinicalsFilteredList != null ? clinicalsFilteredList.size() : 0;
        int clinicalTotal = clinicalsFullList != null ? clinicalsFullList.size() : 0;
        
        String status = String.format("Units: %d/%d | Nurse Calls: %d/%d | Clinicals: %d/%d", 
            unitsShown, unitsTotal, nurseShown, nurseTotal, clinicalShown, clinicalTotal);
        
        if (currentExcelFile != null) {
            status = currentExcelFile.getName() + " | " + status;
        }
        
        statusLabel.setText(status);
    }

    private void refreshTables() {
        // Create full observable lists from parser data
        unitsFullList = FXCollections.observableArrayList(parser.units);
        nurseCallsFullList = FXCollections.observableArrayList(parser.nurseCalls);
        clinicalsFullList = FXCollections.observableArrayList(parser.clinicals);
        
        // Create filtered lists
        unitsFilteredList = new FilteredList<>(unitsFullList, unit -> true);
        nurseCallsFilteredList = new FilteredList<>(nurseCallsFullList, flow -> true);
        clinicalsFilteredList = new FilteredList<>(clinicalsFullList, flow -> true);
        
        // Set filtered lists to tables
        if (tableUnits != null) tableUnits.setItems(unitsFilteredList);
        if (tableNurseCalls != null) tableNurseCalls.setItems(nurseCallsFilteredList);
        if (tableClinicals != null) tableClinicals.setItems(clinicalsFilteredList);
        
        // Update filter options
        updateFilterOptions();
    }

    // ---------- Reset Defaults ----------
    private void resetDefaults() {
        if (edgeRefNameField != null) edgeRefNameField.setText("OutgoingWCTP");
        if (vcsRefNameField != null) vcsRefNameField.setText("VMP");
        statusLabel.setText("Reset interface reference names to defaults");
    }

    // ---------- Apply Interface References ----------
    private void applyInterfaceReferences() {
        if (parser != null) {
            parser.setInterfaceReferences(
                edgeRefNameField != null ? edgeRefNameField.getText().trim() : "OutgoingWCTP",
                vcsRefNameField != null ? vcsRefNameField.getText().trim() : "VMP"
            );
        }
    }

    // ---------- Update Interface References (Live Update) ----------
    private void updateInterfaceRefs() {
        if (parser == null) return;
        
        // Apply the new interface references using existing method
        applyInterfaceReferences();

        // Optional: live update preview if JSON is already generated
        if (!lastGeneratedJson.isEmpty()) {
            try {
                // Rebuild the JSON based on the last generated type using filtered data
                boolean useAdvanced = (mergeFlowsCheckbox != null && mergeFlowsCheckbox.isSelected());
                ExcelParserV5 filteredParser = createFilteredParser();
                var json = ExcelParserV5.pretty(
                    lastGeneratedWasNurseSide ? filteredParser.buildNurseCallsJson(useAdvanced) : filteredParser.buildClinicalsJson(useAdvanced)
                );
                jsonPreview.setText(json);
                lastGeneratedJson = json;
                statusLabel.setText("Updated JSON with new interface references");
            } catch (Exception ex) {
                showError("Failed to refresh preview: " + ex.getMessage());
            }
        }
    }

    private static String safe(String v) { return v == null ? "" : v; }

    // ---------- Remember Directory ----------
    private void rememberDirectory(File file, boolean isExcel) {
        if (file == null) return;
        File dir = file.getParentFile();
        if (dir == null || !dir.exists()) return;

        Preferences prefs = Preferences.userNodeForPackage(AppController.class);
        if (isExcel) {
            lastExcelDir = dir;
            prefs.put(PREF_KEY_LAST_EXCEL_DIR, dir.getAbsolutePath());
        } else {
            lastJsonDir = dir;
            prefs.put(PREF_KEY_LAST_JSON_DIR, dir.getAbsolutePath());
        }
    }

    // ---------- Reset Paths ----------
    private void resetPaths() {
        try {
            Preferences prefs = Preferences.userNodeForPackage(AppController.class);
            prefs.remove(PREF_KEY_LAST_EXCEL_DIR);
            prefs.remove(PREF_KEY_LAST_JSON_DIR);

            lastExcelDir = null;
            lastJsonDir = null;

            statusLabel.setText("File paths reset to default");
            showInfo("✅ Default file paths restored.\nNext time you load or save, file chooser will open in the system home directory.");

        } catch (Exception ex) {
            showError("Failed to reset paths: " + ex.getMessage());
        }
    }
}
