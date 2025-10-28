package com.example.exceljson;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;

public class AppController {

    @FXML private Button loadButton;
    @FXML private Button generateNurseJsonButton;
    @FXML private Button generateClinicalJsonButton;
    @FXML private Button exportNurseJsonButton;
    @FXML private Button exportClinicalJsonButton;
    @FXML private TextArea jsonPreview;
    @FXML private Label statusLabel;

    @FXML private TableView<ExcelParserV5.UnitRow> tableUnits;
    @FXML private TableColumn<ExcelParserV5.UnitRow, String> unitFacilityCol;
    @FXML private TableColumn<ExcelParserV5.UnitRow, String> unitNamesCol;
    @FXML private TableColumn<ExcelParserV5.UnitRow, String> unitNurseGroupCol;
    @FXML private TableColumn<ExcelParserV5.UnitRow, String> unitClinicalGroupCol;
    @FXML private TableColumn<ExcelParserV5.UnitRow, String> unitNoCareGroupCol;
    @FXML private TableColumn<ExcelParserV5.UnitRow, String> unitCommentsCol;

    @FXML private TableView<ExcelParserV5.FlowRow> tableNurseCalls;
    @FXML private TableColumn<ExcelParserV5.FlowRow, String> nurseConfigGroupCol;
    @FXML private TableColumn<ExcelParserV5.FlowRow, String> nurseAlarmNameCol;
    @FXML private TableColumn<ExcelParserV5.FlowRow, String> nurseSendingNameCol;
    @FXML private TableColumn<ExcelParserV5.FlowRow, String> nursePriorityCol;
    @FXML private TableColumn<ExcelParserV5.FlowRow, String> nurseDeviceACol;
    @FXML private TableColumn<ExcelParserV5.FlowRow, String> nurseRingtoneCol;

    @FXML private TableView<ExcelParserV5.FlowRow> tableClinicals;
    @FXML private TableColumn<ExcelParserV5.FlowRow, String> clinicalConfigGroupCol;
    @FXML private TableColumn<ExcelParserV5.FlowRow, String> clinicalAlarmNameCol;
    @FXML private TableColumn<ExcelParserV5.FlowRow, String> clinicalSendingNameCol;
    @FXML private TableColumn<ExcelParserV5.FlowRow, String> clinicalPriorityCol;
    @FXML private TableColumn<ExcelParserV5.FlowRow, String> clinicalDeviceACol;
    @FXML private TableColumn<ExcelParserV5.FlowRow, String> clinicalRingtoneCol;

    private ExcelParserV5 parser;
    private File currentExcelFile;
    private String lastGeneratedJson = "";

    @FXML
    public void initialize() {
        parser = new ExcelParserV5();

        initializeUnitColumns();
        initializeNurseColumns();
        initializeClinicalColumns();

        // disable buttons until Excel is loaded
        setJsonButtonsEnabled(false);

        loadButton.setOnAction(e -> loadExcel());
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
            chooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("Excel Files", "*.xlsx")
            );
            File file = chooser.showOpenDialog(getStage());
            if (file == null) return;

            parser.load(file);
            currentExcelFile = file;

            String summary = parser.getLoadSummary();
            jsonPreview.setText(summary);
            statusLabel.setText("Excel loaded: " + file.getName());
            refreshTables();
            setJsonButtonsEnabled(true);
            showInfo("✅ Excel loaded successfully");

        } catch (Exception ex) {
            showError("Failed to load Excel: " + ex.getMessage());
        }
    }

    // -------------------- GENERATE JSON --------------------
    private void generateJson(boolean nurseSide) {
        try {
            if (parser == null) {
                showError("Please load an Excel file first.");
                return;
            }

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

            FileChooser chooser = new FileChooser();
            chooser.setTitle(nurseSide ? "Export NurseCall JSON" : "Export Clinical JSON");
            chooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("JSON Files", "*.json")
            );
            chooser.setInitialFileName(nurseSide ? "NurseCalls.json" : "Clinicals.json");

            File file = chooser.showSaveDialog(getStage());
            if (file == null) return;

            if (nurseSide) {
                parser.writeNurseCallsJson(file);
            } else {
                parser.writeClinicalsJson(file);
            }

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

    private void initializeUnitColumns() {
        if (unitFacilityCol != null) {
            unitFacilityCol.setCellValueFactory(data -> new SimpleStringProperty(safe(data.getValue().facility)));
        }
        if (unitNamesCol != null) {
            unitNamesCol.setCellValueFactory(data -> new SimpleStringProperty(safe(data.getValue().unitNames)));
        }
        if (unitNurseGroupCol != null) {
            unitNurseGroupCol.setCellValueFactory(data -> new SimpleStringProperty(safe(data.getValue().nurseGroup)));
        }
        if (unitClinicalGroupCol != null) {
            unitClinicalGroupCol.setCellValueFactory(data -> new SimpleStringProperty(safe(data.getValue().clinGroup)));
        }
        if (unitNoCareGroupCol != null) {
            unitNoCareGroupCol.setCellValueFactory(data -> new SimpleStringProperty(safe(data.getValue().noCareGroup)));
        }
        if (unitCommentsCol != null) {
            unitCommentsCol.setCellValueFactory(data -> new SimpleStringProperty(safe(data.getValue().comments)));
        }
    }

    private void initializeNurseColumns() {
        if (nurseConfigGroupCol != null) {
            nurseConfigGroupCol.setCellValueFactory(data -> new SimpleStringProperty(safe(data.getValue().configGroup)));
        }
        if (nurseAlarmNameCol != null) {
            nurseAlarmNameCol.setCellValueFactory(data -> new SimpleStringProperty(safe(data.getValue().alarmName)));
        }
        if (nurseSendingNameCol != null) {
            nurseSendingNameCol.setCellValueFactory(data -> new SimpleStringProperty(safe(data.getValue().sendingName)));
        }
        if (nursePriorityCol != null) {
            nursePriorityCol.setCellValueFactory(data -> new SimpleStringProperty(safe(data.getValue().priorityRaw)));
        }
        if (nurseDeviceACol != null) {
            nurseDeviceACol.setCellValueFactory(data -> new SimpleStringProperty(safe(data.getValue().deviceA)));
        }
        if (nurseRingtoneCol != null) {
            nurseRingtoneCol.setCellValueFactory(data -> new SimpleStringProperty(safe(data.getValue().ringtone)));
        }
    }

    private void initializeClinicalColumns() {
        if (clinicalConfigGroupCol != null) {
            clinicalConfigGroupCol.setCellValueFactory(data -> new SimpleStringProperty(safe(data.getValue().configGroup)));
        }
        if (clinicalAlarmNameCol != null) {
            clinicalAlarmNameCol.setCellValueFactory(data -> new SimpleStringProperty(safe(data.getValue().alarmName)));
        }
        if (clinicalSendingNameCol != null) {
            clinicalSendingNameCol.setCellValueFactory(data -> new SimpleStringProperty(safe(data.getValue().sendingName)));
        }
        if (clinicalPriorityCol != null) {
            clinicalPriorityCol.setCellValueFactory(data -> new SimpleStringProperty(safe(data.getValue().priorityRaw)));
        }
        if (clinicalDeviceACol != null) {
            clinicalDeviceACol.setCellValueFactory(data -> new SimpleStringProperty(safe(data.getValue().deviceA)));
        }
        if (clinicalRingtoneCol != null) {
            clinicalRingtoneCol.setCellValueFactory(data -> new SimpleStringProperty(safe(data.getValue().ringtone)));
        }
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
