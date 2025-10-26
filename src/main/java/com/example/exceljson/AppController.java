package com.example.exceljson;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.scene.control.cell.TextFieldTableCell;

import java.io.File;
import java.util.Map;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class AppController {

    @FXML private TableView<ExcelParserV5.UnitRow> tableUnits;
    @FXML private TableView<ExcelParserV5.FlowRow> tableNurseCalls;
    @FXML private TableView<ExcelParserV5.FlowRow> tableClinicals;
    @FXML private TextArea jsonPreview;
    @FXML private Button btnLoad;
    @FXML private Button btnGenerate;
    @FXML private Button btnSave;

    private ExcelParserV5 parser;
    private File loadedFile;

    private final ObservableList<ExcelParserV5.UnitRow> unitsData = FXCollections.observableArrayList();
    private final ObservableList<ExcelParserV5.FlowRow> nurseData = FXCollections.observableArrayList();
    private final ObservableList<ExcelParserV5.FlowRow> clinicalData = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        parser = new ExcelParserV5();
        setupTable(tableUnits, ExcelParserV5.UnitRow.class);
        setupTable(tableNurseCalls, ExcelParserV5.FlowRow.class);
        setupTable(tableClinicals, ExcelParserV5.FlowRow.class);

        jsonPreview.setEditable(false);
    }

    // -------------------------------------------------------------------------
    // Table setup with editable columns
    private <T> void setupTable(TableView<T> table, Class<T> clazz) {
        table.setEditable(true);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        if (clazz == ExcelParserV5.UnitRow.class) {
            addColumn(table, "Facility", "facility");
            addColumn(table, "Common Unit Name(s)", "unitNames");
            addColumn(table, "Nurse Call Config", "nurseGroup");
            addColumn(table, "Patient Monitor Config", "clinGroup");
            addColumn(table, "No Caregiver Group", "noCareGroup");
            addColumn(table, "Comments", "comments");
        } else if (clazz == ExcelParserV5.FlowRow.class) {
            addColumn(table, "Type", "type");
            addColumn(table, "Config Group", "configGroup");
            addColumn(table, "Alarm Name", "alarmName");
            addColumn(table, "Sending Name", "sendingName");
            addColumn(table, "Priority", "priorityRaw");
            addColumn(table, "Device A", "deviceA");
            addColumn(table, "Ringtone", "ringtone");
            addColumn(table, "Response Options", "responseOptions");
            addColumn(table, "T1", "t1");
            addColumn(table, "R1", "r1");
            addColumn(table, "T2", "t2");
            addColumn(table, "R2", "r2");
            addColumn(table, "T3", "t3");
            addColumn(table, "R3", "r3");
            addColumn(table, "T4", "t4");
            addColumn(table, "R4", "r4");
            addColumn(table, "T5", "t5");
            addColumn(table, "R5", "r5");
        }
    }

    private <T> void addColumn(TableView<T> table, String title, String fieldName) {
        TableColumn<T, String> col = new TableColumn<>(title);
        col.setCellValueFactory(cd -> {
            try {
                var field = cd.getValue().getClass().getField(fieldName);
                Object val = field.get(cd.getValue());
                return new SimpleStringProperty(val == null ? "" : val.toString());
            } catch (Exception e) {
                return new SimpleStringProperty("");
            }
        });
        col.setCellFactory(TextFieldTableCell.forTableColumn());
        col.setOnEditCommit(evt -> {
            try {
                var field = evt.getRowValue().getClass().getField(fieldName);
                field.set(evt.getRowValue(), evt.getNewValue());
            } catch (Exception e) {
                showAlert("Error updating cell: " + e.getMessage());
            }
        });
        table.getColumns().add(col);
    }

    // -------------------------------------------------------------------------
    // Load Excel
    @FXML
    public void onLoadExcel(ActionEvent event) {
        try {
            FileChooser chooser = new FileChooser();
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel Files", "*.xlsx"));
            chooser.setTitle("Select Configuration Excel File");

            File file = chooser.showOpenDialog(((Stage) btnLoad.getScene().getWindow()));
            if (file == null) return;

            parser.load(file);
            loadedFile = file;

            unitsData.setAll(parser.units);
            nurseData.setAll(parser.nurseCalls);
            clinicalData.setAll(parser.clinicals);

            tableUnits.setItems(unitsData);
            tableNurseCalls.setItems(nurseData);
            tableClinicals.setItems(clinicalData);

            showAlert(parser.getLoadSummary());
        } catch (Exception e) {
            showAlert("❌ Error loading Excel: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // -------------------------------------------------------------------------
    // Generate JSON preview (two separate files in memory)
    @FXML
    public void onGenerateJson(ActionEvent event) {
        try {
            Map<String, Object> nurseJson = parser.buildNurseCallsJson();
            Map<String, Object> clinicalJson = parser.buildClinicalsJson();

            String nursePretty = ExcelParserV5.pretty(nurseJson);
            String clinicalPretty = ExcelParserV5.pretty(clinicalJson);

            jsonPreview.setText(
                    "===== NurseCalls JSON =====\n" +
                    nursePretty + "\n\n" +
                    "===== Clinicals JSON =====\n" +
                    clinicalPretty
            );

            showAlert("✅ JSON Preview generated successfully!");
        } catch (Exception e) {
            showAlert("❌ Error generating JSON: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // -------------------------------------------------------------------------
    // Save updated Excel data (if edits made)
    @FXML
    public void onSaveExcel(ActionEvent event) {
        try {
            if (loadedFile == null) {
                showAlert("No Excel file loaded.");
                return;
            }
            // Auto-save back to Excel file (overwriting for now)
            Workbook wb = new XSSFWorkbook();
            Sheet sheet = wb.createSheet("Updated Data");
            // Could be expanded to fully reserialize original structure
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("Facility");
            header.createCell(1).setCellValue("Common Unit Name");
            header.createCell(2).setCellValue("Nurse Call Config");
            header.createCell(3).setCellValue("Patient Monitor Config");

            int r = 1;
            for (ExcelParserV5.UnitRow u : parser.units) {
                Row row = sheet.createRow(r++);
                row.createCell(0).setCellValue(u.facility);
                row.createCell(1).setCellValue(u.unitNames);
                row.createCell(2).setCellValue(u.nurseGroup);
                row.createCell(3).setCellValue(u.clinGroup);
            }

            File outFile = new File(loadedFile.getParent(), "Updated_" + loadedFile.getName());
            try (var fos = new java.io.FileOutputStream(outFile)) {
                wb.write(fos);
            }
            wb.close();

            showAlert("✅ Changes saved to: " + outFile.getAbsolutePath());
        } catch (Exception e) {
            showAlert("❌ Error saving Excel: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // -------------------------------------------------------------------------
    // Utility popup alerts
    private void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Engage Rules Generator");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}
