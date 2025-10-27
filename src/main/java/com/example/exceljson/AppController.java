package com.example.exceljson;

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

    private ExcelParserV5 parser;
    private File currentExcelFile;
    private String lastGeneratedJson = "";

    @FXML
    public void initialize() {
        parser = new ExcelParserV5();

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
}
