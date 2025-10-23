package com.example.exceljson;

import com.example.exceljson.util.FXTableUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;

import java.io.File;
import java.nio.file.Files;
import java.util.*;

public class AppController {

    private final TextArea jsonPreview = new TextArea();
    private final TextArea configPreview = new TextArea();
    private final TableView<Map<String, String>> unitTable = new TableView<>();
    private final TableView<Map<String, String>> nurseCallTable = new TableView<>();
    private final TableView<Map<String, String>> patientMonTable = new TableView<>();

    private File currentExcel;
    private File currentConfig;
    private ExcelParser parser;
    private Config config = Config.defaultConfig();

    public BorderPane buildUI() {
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(10));

        // --- Top toolbar ---
        HBox top = new HBox(10);
        Button btnOpen = new Button("Open Excel…");
        Button btnOpenCfg = new Button("Open Config…");
        Button btnRefresh = new Button("Refresh Preview");
        Button btnExport = new Button("Export JSON…");
        btnExport.setDisable(true);
        top.getChildren().addAll(btnOpen, btnOpenCfg, btnRefresh, btnExport);
        root.setTop(top);

        // --- Center Tabs ---
        TabPane tabs = new TabPane();
        Tab tabUnits = new Tab("Unit Breakdown", unitTable);
        Tab tabNC = new Tab("Nurse Call", nurseCallTable);
        Tab tabPM = new Tab("Patient Monitoring", patientMonTable);
        Tab tabCfg = new Tab("Config", configPreview);
        Tab tabJSON = new Tab("JSON Preview", jsonPreview);
        for (Tab t : List.of(tabUnits, tabNC, tabPM, tabCfg, tabJSON)) {
            t.setClosable(false);
        }
        tabs.getTabs().addAll(tabUnits, tabNC, tabPM, tabCfg, tabJSON);
        root.setCenter(tabs);

        // --- Styling ---
        jsonPreview.setWrapText(true);
        jsonPreview.setEditable(false);
        jsonPreview.setStyle("-fx-font-family: 'Consolas', 'Courier New', monospace;");
        configPreview.setWrapText(true);
        configPreview.setEditable(false);
        configPreview.setStyle("-fx-font-family: 'Consolas', 'Courier New', monospace;");

        // --- Button Actions ---

        // Open Excel file
        btnOpen.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Excel Workbooks", "*.xlsx", "*.xls")
            );
            File file = fc.showOpenDialog(null);
            if (file != null) {
                currentExcel = file;
                reload();
                btnExport.setDisable(false);
            }
        });

        // Open Config file
        btnOpenCfg.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Config Files (YAML/JSON)", "*.yml", "*.yaml", "*.json")
            );
            File file = fc.showOpenDialog(null);
            if (file != null) {
                currentConfig = file;
                try {
                    ObjectMapper mapper;
                    if (file.getName().toLowerCase().endsWith(".json")) {
                        mapper = new ObjectMapper();
                    } else {
                        mapper = new ObjectMapper(new YAMLFactory());
                    }
                    String text = Files.readString(file.toPath());
                    configPreview.setText(text);
                    config = mapper.readValue(text, Config.class);
                } catch (Exception ex) {
                    configPreview.setText("Error reading config: " + ex.getMessage());
                    ex.printStackTrace();
                }
            }
        });

        // Refresh preview (reload Excel)
        btnRefresh.setOnAction(e -> reload());

        // Export JSON
        btnExport.setOnAction(e -> handleExport());

        return root;
    }

    // --- Reload Excel workbook and update tables ---
    private void reload() {
        try {
            if (currentExcel == null) return;
            parser = new ExcelParser(config);
            parser.load(currentExcel);
            FXTableUtils.populate(unitTable, parser.getUnitBreakdownRows());
            FXTableUtils.populate(nurseCallTable, parser.getNurseCallRows());
            FXTableUtils.populate(patientMonTable, parser.getPatientMonitoringRows());

            // Build JSON preview
            Map<String, Object> json = parser.buildJson();
            ObjectWriter writer = new ObjectMapper().writerWithDefaultPrettyPrinter();
            jsonPreview.setText(writer.writeValueAsString(json));
        } catch (Exception ex) {
            jsonPreview.setText("Error loading Excel: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    // --- Export JSON to file ---
    private void handleExport() {
        try {
            if (parser == null) {
                jsonPreview.setText("No data loaded to export.");
                return;
            }
            Map<String, Object> json = parser.buildJson();
            ObjectWriter writer = new ObjectMapper().writerWithDefaultPrettyPrinter();

            FileChooser fc = new FileChooser();
            fc.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("JSON Files", "*.json")
            );
            File out = fc.showSaveDialog(null);
            if (out != null) {
                writer.writeValue(out, json);
            }
        } catch (Exception e) {
            jsonPreview.setText("Error exporting JSON: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
