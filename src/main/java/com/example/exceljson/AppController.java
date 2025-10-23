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


// Top bar
HBox top = new HBox(10);
Button btnOpen = new Button("Open Excel…");
Button btnOpenCfg = new Button("Open Config…");
Button btnExport = new Button("Export JSON…");
Button btnRefresh = new Button("Refresh Preview");
btnExport.setDisable(true);
top.getChildren().addAll(btnOpen, btnOpenCfg, btnRefresh, btnExport);
root.setTop(top);


// TabPane center
TabPane tabs = new TabPane();
Tab tabUnits = new Tab("Unit Breakdown", unitTable);
Tab tabNC = new Tab("Nurse Call", nurseCallTable);
Tab tabPM = new Tab("Patient Monitoring", patientMonTable);
Tab tabCfg = new Tab("Config", configPreview);
Tab tabJSON = new Tab("JSON Preview", jsonPreview);
for (Tab t : List.of(tabUnits, tabNC, tabPM, tabCfg, tabJSON)) t.setClosable(false);
tabs.getTabs().addAll(tabUnits, tabNC, tabPM, tabCfg, tabJSON);
root.setCenter(tabs);


jsonPreview.setWrapText(true);
jsonPreview.setEditable(false);
jsonPreview.setStyle("-fx-font-family: 'Consolas', 'Courier New', monospace;");
configPreview.setWrapText(true);
configPreview.setEditable(false);
configPreview.setStyle("-fx-font-family: 'Consolas', 'Courier New', monospace;");


btnOpen.setOnAction(e -> {
FileChooser fc = new FileChooser();
fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel Workbooks", "*.xlsx", "*.xls"));
File file = fc.showOpenDialog(null);
if (file != null) {
currentExcel = file;
reload();
btnExport.setDisable(false);
}
});


btnOpenCfg.setOnAction(e -> {
FileChooser fc = new FileChooser();
fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Config Files (YAML/JSON)", "*.yml", "*.yaml", "*.json"));
File file = fc.showOpenDialog(null);
if (file != null) {
currentConfig = file;
try {
}
