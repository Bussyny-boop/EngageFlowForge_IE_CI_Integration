# Excel→JSON JavaFX App (Windows) – Multi‑Tab GUI

This project builds a Windows‑friendly **JavaFX GUI** that reads your Excel workbook (multiple sheets), parses **Unit Breakdown**, **Nurse Call**, and **Patient Monitoring**, and exports a JSON matching your `2025-10-22.json` structure (with `version`, `alarmAlertDefinitions`, and `deliveryFlows`).

---

## ✅ Features

* **Multi‑tab GUI**: separate tabs to preview parsed sheets and final JSON.
* **Flexible header detection**: finds the real header row even when there are notes/merged cells above it.
* **Mapping rules** aligned to your plan (can be extended via aliases).
* **Apache POI** for Excel, **Jackson** for JSON.
* **JavaFX** for the interface.

---

## 🧰 Requirements

* JDK **17** (or newer)
* Maven **3.9+**
* Windows 10/11

---

## 🗂️ Project Layout

```
excel-json-app/
├─ pom.xml
└─ src/
   └─ main/
      ├─ java/
      │  └─ com/example/exceljson/
      │     ├─ Main.java
      │     ├─ AppController.java
      │     ├─ ExcelParser.java
      │     ├─ HeaderFinder.java
      │     ├─ MappingAliases.java
      │     ├─ model/
      │     │  ├─ AlarmAlertDefinition.java
      │     │  ├─ ValuePair.java
      │     │  ├─ DeliveryFlow.java
      │     │  ├─ Destination.java
      │     │  ├─ InterfaceRef.java
      │     │  ├─ ParameterAttribute.java
      │     │  ├─ UnitRef.java
      │     │  └─ Condition.java
      │     └─ util/
      │        └─ FXTableUtils.java
      └─ resources/
         └─ application.css (optional)
```

> **Tip:** You can paste these files exactly into the structure above. Build with Maven and run.

---

## 📦 `pom.xml`

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>
  <groupId>com.example</groupId>
  <artifactId>excel-json-app</artifactId>
  <version>1.1.0</version>
  <properties>
    <maven.compiler.source>17</maven.compiler.source>
    <maven.compiler.target>17</maven.compiler.target>
    <javafx.version>21.0.3</javafx.version>
    <jackson.version>2.17.2</jackson.version>
    <poi.version>5.2.5</poi.version>
  </properties>

  <dependencies>
    <!-- JavaFX -->
    <dependency>
      <groupId>org.openjfx</groupId>
      <artifactId>javafx-controls</artifactId>
      <version>${javafx.version}</version>
    </dependency>
    <dependency>
      <groupId>org.openjfx</groupId>
      <artifactId>javafx-graphics</artifactId>
      <version>${javafx.version}</version>
    </dependency>

    <!-- Jackson JSON -->
    <dependency>
      <groupId>com.fasterxml.jackson.core</groupId>
      <artifactId>jackson-databind</artifactId>
      <version>${jackson.version}</version>
    </dependency>
    <dependency>
      <groupId>com.fasterxml.jackson.core</groupId>
      <artifactId>jackson-core</artifactId>
      <version>${jackson.version}</version>
    </dependency>
    <dependency>
      <groupId>com.fasterxml.jackson.core</groupId>
      <artifactId>jackson-annotations</artifactId>
      <version>${jackson.version}</version>
    </dependency>
    <!-- YAML config support -->
    <dependency>
      <groupId>com.fasterxml.jackson.dataformat</groupId>
      <artifactId>jackson-dataformat-yaml</artifactId>
      <version>${jackson.version}</version>
    </dependency>

    <!-- Apache POI for Excel -->
    <dependency>
      <groupId>org.apache.poi</groupId>
      <artifactId>poi</artifactId>
      <version>${poi.version}</version>
    </dependency>
    <dependency>
      <groupId>org.apache.poi</groupId>
      <artifactId>poi-ooxml</artifactId>
      <version>${poi.version}</version>
    </dependency>
  </dependencies>

  <build>
    <plugins>
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-compiler-plugin</artifactId>
        <version>3.11.0</version>
        <configuration>
          <release>17</release>
        </configuration>
      </plugin>
      <!-- Run with: mvn javafx:run -->
      <plugin>
        <groupId>org.openjfx</groupId>
        <artifactId>javafx-maven-plugin</artifactId>
        <version>0.0.8</version>
        <configuration>
          <mainClass>com.example.exceljson.Main</mainClass>
        </configuration>
      </plugin>
    </plugins>
  </build>
</project>
```

---

## 🖥️ `Main.java`

```java
package com.example.exceljson;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

public class Main extends Application {
    @Override
    public void start(Stage stage) {
        AppController controller = new AppController();
        BorderPane root = controller.buildUI();
        Scene scene = new Scene(root, 1200, 800);
        stage.setTitle("Excel → JSON Converter (Multi‑Tab)");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}
```

---

## 🧩 `AppController.java`

````java
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
                    loadConfig(currentConfig);
                    configPreview.setText(Files.readString(currentConfig.toPath()));
                    reload();
                } catch (Exception ex) {
                    showError("Failed to load config", ex);
                }
            }
        });

        btnRefresh.setOnAction(e -> reload());

        btnExport.setOnAction(e -> {
            if (parser == null) return;
            try {
                Map<String, Object> rootJson = parser.buildJson();
                ObjectWriter ow = new ObjectMapper().writerWithDefaultPrettyPrinter();
                String jsonText = ow.writeValueAsString(rootJson);

                FileChooser fc = new FileChooser();
                fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON Files", "*.json"));
                fc.setInitialFileName("export.json");
                File out = fc.showSaveDialog(null);
                if (out != null) {
                    Files.writeString(out.toPath(), jsonText);
                    showInfo("Saved", "JSON exported to:
" + out.getAbsolutePath());
                }
            } catch (Exception ex) {
                showError("Export failed", ex);
            }
        });

        return root;
    }

    private void reload() {
        if (currentExcel == null) return;
        try {
            parser = new ExcelParser(config);
            parser.load(currentExcel);
            populateTables();
            buildAndPreviewJSON();
        } catch (Exception ex) {
            showError("Failed to parse Excel", ex);
        }
    }

    private void loadConfig(File file) throws Exception {
        String name = file.getName().toLowerCase();
        if (name.endsWith(".json")) {
            ObjectMapper om = new ObjectMapper();
            this.config = om.readValue(file, Config.class);
        } else {
            ObjectMapper ym = new ObjectMapper(new YAMLFactory());
            this.config = ym.readValue(file, Config.class);
        }
        this.config = Config.mergeWithDefaults(this.config); // ensure fallbacks
    }

    private void populateTables() {
        // Unit Breakdown
        List<Map<String, String>> units = parser.getUnitBreakdownRows();
        FXTableUtils.populate(unitTable, units);
        // Nurse Call
        List<Map<String, String>> nc = parser.getNurseCallRows();
        FXTableUtils.populate(nurseCallTable, nc);
        // Patient Monitoring
        List<Map<String, String>> pm = parser.getPatientMonitoringRows();
        FXTableUtils.populate(patientMonTable, pm);
    }

    private void buildAndPreviewJSON() throws Exception {
        Map<String, Object> rootJson = parser.buildJson();
        ObjectWriter ow = new ObjectMapper().writerWithDefaultPrettyPrinter();
        jsonPreview.setText(ow.writeValueAsString(rootJson));
    }

    private void showError(String title, Exception ex) {
        ex.printStackTrace();
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(title);
        a.setHeaderText(title);
        a.setContentText(ex.getMessage());
        a.showAndWait();
    }
    private void showInfo(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title);
        a.setHeaderText(title);
        a.setContentText(msg);
        a.showAndWait();
    }
}
```java
package com.example.exceljson;

import com.example.exceljson.model.*;
import com.example.exceljson.util.FXTableUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;

import java.io.File;
import java.nio.file.Files;
import java.util.*;

public class AppController {
    private final TextArea jsonPreview = new TextArea();
    private final TableView<Map<String, String>> unitTable = new TableView<>();
    private final TableView<Map<String, String>> nurseCallTable = new TableView<>();
    private final TableView<Map<String, String>> patientMonTable = new TableView<>();

    private File currentExcel;
    private ExcelParser parser;

    public BorderPane buildUI() {
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(10));

        // Top bar
        HBox top = new HBox(10);
        Button btnOpen = new Button("Open Excel…");
        Button btnExport = new Button("Export JSON…");
        Button btnRefresh = new Button("Refresh Preview");
        btnExport.setDisable(true);
        top.getChildren().addAll(btnOpen, btnRefresh, btnExport);
        root.setTop(top);

        // TabPane center
        TabPane tabs = new TabPane();
        Tab tabUnits = new Tab("Unit Breakdown", unitTable);
        Tab tabNC = new Tab("Nurse Call", nurseCallTable);
        Tab tabPM = new Tab("Patient Monitoring", patientMonTable);
        Tab tabJSON = new Tab("JSON Preview", jsonPreview);
        tabUnits.setClosable(false); tabNC.setClosable(false);
        tabPM.setClosable(false); tabJSON.setClosable(false);
        tabs.getTabs().addAll(tabUnits, tabNC, tabPM, tabJSON);
        root.setCenter(tabs);

        jsonPreview.setWrapText(true);
        jsonPreview.setEditable(false);
        jsonPreview.setStyle("-fx-font-family: 'Consolas', 'Courier New', monospace;");

        btnOpen.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel Workbooks", "*.xlsx", "*.xls"));
            File file = fc.showOpenDialog(null);
            if (file != null) {
                currentExcel = file;
                parser = new ExcelParser();
                try {
                    parser.load(currentExcel);
                    populateTables();
                    buildAndPreviewJSON();
                    btnExport.setDisable(false);
                } catch (Exception ex) {
                    showError("Failed to parse Excel", ex);
                }
            }
        });

        btnRefresh.setOnAction(e -> {
            if (parser != null) {
                try {
                    populateTables();
                    buildAndPreviewJSON();
                } catch (Exception ex) {
                    showError("Refresh failed", ex);
                }
            }
        });

        btnExport.setOnAction(e -> {
            if (parser == null) return;
            try {
                Map<String, Object> rootJson = parser.buildJson();
                ObjectWriter ow = new ObjectMapper().writerWithDefaultPrettyPrinter();
                String jsonText = ow.writeValueAsString(rootJson);

                FileChooser fc = new FileChooser();
                fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON Files", "*.json"));
                fc.setInitialFileName("export.json");
                File out = fc.showSaveDialog(null);
                if (out != null) {
                    Files.writeString(out.toPath(), jsonText);
                    showInfo("Saved", "JSON exported to:\n" + out.getAbsolutePath());
                }
            } catch (Exception ex) {
                showError("Export failed", ex);
            }
        });

        return root;
    }

    private void populateTables() {
        // Unit Breakdown
        List<Map<String, String>> units = parser.getUnitBreakdownRows();
        FXTableUtils.populate(unitTable, units);

        // Nurse Call
        List<Map<String, String>> nc = parser.getNurseCallRows();
        FXTableUtils.populate(nurseCallTable, nc);

        // Patient Monitoring
        List<Map<String, String>> pm = parser.getPatientMonitoringRows();
        FXTableUtils.populate(patientMonTable, pm);
    }

    private void buildAndPreviewJSON() throws Exception {
        Map<String, Object> rootJson = parser.buildJson();
        ObjectWriter ow = new ObjectMapper().writerWithDefaultPrettyPrinter();
        jsonPreview.setText(ow.writeValueAsString(rootJson));
    }

    private void showError(String title, Exception ex) {
        ex.printStackTrace();
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(title);
        a.setHeaderText(title);
        a.setContentText(ex.getMessage());
        a.showAndWait();
    }
    private void showInfo(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title);
        a.setHeaderText(title);
        a.setContentText(msg);
        a.showAndWait();
    }
}
````

---

## 🔎 `HeaderFinder.java`

```java
package com.example.exceljson;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;

import java.util.*;

public class HeaderFinder {
    /** Find the header row by scanning first N rows for the one that contains the most expected headers. */
    public static int findHeaderRow(Sheet sheet, Set<String> expectedTokens, int scanRows) {
        int bestRow = -1;
        int bestScore = 0;
        for (int r = 0; r < Math.min(scanRows, sheet.getPhysicalNumberOfRows()); r++) {
            Row row = sheet.getRow(r);
            if (row == null) continue;
            int nonEmpty = 0; int hits = 0;
            for (int c = 0; c < row.getLastCellNum(); c++) {
                String val = getString(row, c);
                if (!val.isBlank()) {
                    nonEmpty++;
                    String norm = normalize(val);
                    if (expectedTokens.contains(norm)) hits++;
                }
            }
            int score = hits * 10 + nonEmpty; // weigh matches
            if (score > bestScore) { bestScore = score; bestRow = r; }
        }
        return bestRow;
    }

    public static String getString(Row row, int col) {
        if (row == null) return "";
        try {
            var cell = row.getCell(col);
            if (cell == null) return "";
            return switch (cell.getCellType()) {
                case STRING -> cell.getStringCellValue();
                case NUMERIC -> String.valueOf(cell.getNumericCellValue());
                case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
                default -> "";
            };
        } catch (Exception e) { return ""; }
    }

    public static String normalize(String s) {
        return s.trim().toLowerCase().replaceAll("[^a-z0-9]+", " ").replaceAll(" +", " ").trim();
    }
}
```

---

## 🧭 `MappingAliases.java`

````java
package com.example.exceljson;

import java.util.*;

public class MappingAliases {
    // Defaults; can be overridden by Config
    public Set<String> CFG_GROUP = set("configuration group", "config group");
    public Set<String> ALERT_NAME_COMMON = set("common alert or alarm name", "alarm name", "common alert name");
    public Set<String> SENDING_NAME = set("sending system alert name", "sending system alarm name");
    public Set<String> PRIORITY = set("priority");
    public Set<String> DEVICE_A = set("device a", "device", "device a ");
    public Set<String> RINGTONE_A = set("ringtone device a", "ringtone");
    public Set<String> T1 = set("time to 1st recipient", "time to first recipient", "delay to 1st");
    public Set<String> R1 = set("1st recipient", "first recipient");
    public Set<String> T2 = set("time to 2nd recipient", "time to second recipient", "delay to 2nd");
    public Set<String> R2 = set("2nd recipient", "second recipient");
    public Set<String> RESPONSE = set("response options", "response option");
    public Set<String> EMDAN = set("emdan compliant", "emdan compliant?");
    public Set<String> COMMENTS = set("comments", "comment");
    // Unit Breakdown
    public Set<String> FACILITY = set("facility");
    public Set<String> UNIT_NAME = set("common unit name", "unit", "unit name");
    // NEW: column linking units to flows
    public Set<String> UNIT_GROUPS = set("configuration group", "config group", "applies to", "workflow group", "delivery flow");

    public static Set<String> set(String... vals) { return new HashSet<>(Arrays.asList(vals)); }
}
```java
package com.example.exceljson;

import java.util.*;

public class MappingAliases {
    // Define normalized header aliases per logical field
    public static final Set<String> CFG_GROUP = set("configuration group", "config group");
    public static final Set<String> ALERT_NAME_COMMON = set("common alert or alarm name", "alarm name", "common alert name");
    public static final Set<String> SENDING_NAME = set("sending system alert name", "sending system alarm name");
    public static final Set<String> PRIORITY = set("priority");
    public static final Set<String> DEVICE_A = set("device a", "device", "device a ");
    public static final Set<String> RINGTONE_A = set("ringtone device a", "ringtone");

    public static final Set<String> T1 = set("time to 1st recipient", "time to first recipient", "delay to 1st");
    public static final Set<String> R1 = set("1st recipient", "first recipient");
    public static final Set<String> T2 = set("time to 2nd recipient", "time to second recipient", "delay to 2nd");
    public static final Set<String> R2 = set("2nd recipient", "second recipient");

    public static final Set<String> RESPONSE = set("response options", "response option");
    public static final Set<String> EMDAN = set("emdan compliant", "emdan compliant?");
    public static final Set<String> COMMENTS = set("comments", "comment");

    // Unit Breakdown
    public static final Set<String> FACILITY = set("facility");
    public static final Set<String> UNIT_NAME = set("common unit name", "unit", "unit name");

    public static Set<String> set(String... vals) { return new HashSet<>(Arrays.asList(vals)); }
}
````

---

## 🧮 `ExcelParser.java`

````java
package com.example.exceljson;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.util.*;
import java.util.stream.Collectors;

import static com.example.exceljson.HeaderFinder.*;

public class ExcelParser {
    private final Config config;
    private Workbook wb;

    // Raw rows as maps (for preview tables)
    private List<Map<String,String>> unitRows = new ArrayList<>();
    private List<Map<String,String>> nurseCallRows = new ArrayList<>();
    private List<Map<String,String>> patientMonRows = new ArrayList<>();

    public ExcelParser(Config config) { this.config = config; }

    public void load(File excel) throws Exception {
        try (FileInputStream fis = new FileInputStream(excel)) {
            wb = new XSSFWorkbook(fis);
        }
        parseUnitBreakdown();
        parseNurseCall();
        parsePatientMonitoring();
    }

    public List<Map<String,String>> getUnitBreakdownRows(){ return unitRows; }
    public List<Map<String,String>> getNurseCallRows(){ return nurseCallRows; }
    public List<Map<String,String>> getPatientMonitoringRows(){ return patientMonRows; }

    // ---- Parsing helpers ----
    private Map<String,Integer> headerIndex(Row header) {
        Map<String,Integer> map = new LinkedHashMap<>();
        for (int c = 0; c < header.getLastCellNum(); c++) {
            String key = normalize(getString(header, c));
            if (!key.isBlank()) map.put(key, c);
        }
        return map;
    }
    private String get(Row row, Map<String,Integer> hmap, Set<String> aliases){
        for (String a : aliases) {
            Integer idx = hmap.get(a);
            if (idx != null) return HeaderFinder.getString(row, idx).trim();
        }
        return "";
    }

    private void parseUnitBreakdown() {
        Sheet sheet = wb.getSheet(config.sheets.unitBreakdown);
        if (sheet == null) return;
        Set<String> expected = new HashSet<>();
        expected.addAll(config.aliases.FACILITY);
        expected.addAll(config.aliases.UNIT_NAME);
        expected.addAll(config.aliases.UNIT_GROUPS);

        int headerRowIdx = HeaderFinder.findHeaderRow(sheet, expected, 30);
        if (headerRowIdx < 0) return;
        Row header = sheet.getRow(headerRowIdx);
        Map<String,Integer> hmap = headerIndex(header);

        for (int r = headerRowIdx + 1; r <= sheet.getLastRowNum(); r++) {
            Row row = sheet.getRow(r);
            if (row == null) continue;
            String facility = get(row, hmap, config.aliases.FACILITY);
            String unit = get(row, hmap, config.aliases.UNIT_NAME);
            String groups = get(row, hmap, config.aliases.UNIT_GROUPS);
            if (facility.isBlank() && unit.isBlank() && groups.isBlank()) continue;

            Map<String,String> map = new LinkedHashMap<>();
            map.put("Facility", facility);
            map.put("Common Unit Name", unit);
            map.put("Groups", groups);
            unitRows.add(map);
        }
    }

    private void parseNurseCall() {
        Sheet sheet = wb.getSheet(config.sheets.nurseCall);
        if (sheet == null) return;
        Set<String> expected = new HashSet<>() {{
            addAll(config.aliases.CFG_GROUP);
            addAll(config.aliases.ALERT_NAME_COMMON);
            addAll(config.aliases.SENDING_NAME);
            addAll(config.aliases.PRIORITY);
        }};
        int headerRowIdx = HeaderFinder.findHeaderRow(sheet, expected, 40);
        if (headerRowIdx < 0) return;
        Row header = sheet.getRow(headerRowIdx);
        Map<String,Integer> hmap = headerIndex(header);

        for (int r = headerRowIdx + 1; r <= sheet.getLastRowNum(); r++) {
            Row row = sheet.getRow(r);
            if (row == null) continue;
            String cfg = get(row, hmap, config.aliases.CFG_GROUP);
            String alert = get(row, hmap, config.aliases.ALERT_NAME_COMMON);
            String send = get(row, hmap, config.aliases.SENDING_NAME);
            if (cfg.isBlank() && alert.isBlank() && send.isBlank()) continue;

            Map<String,String> m = new LinkedHashMap<>();
            m.put("Configuration Group", cfg);
            m.put("Common Alert or Alarm Name", alert);
            m.put("Sending System Alert Name", send);
            m.put("Priority", get(row, hmap, config.aliases.PRIORITY));
            m.put("Device - A", get(row, hmap, config.aliases.DEVICE_A));
            m.put("Ringtone Device - A", get(row, hmap, config.aliases.RINGTONE_A));
            m.put("Time to 1st Recipient", get(row, hmap, config.aliases.T1));
            m.put("1st Recipient", get(row, hmap, config.aliases.R1));
            m.put("Time to 2nd Recipient", get(row, hmap, config.aliases.T2));
            m.put("2nd Recipient", get(row, hmap, config.aliases.R2));
            m.put("Response Options", get(row, hmap, config.aliases.RESPONSE));
            m.put("EMDAN Compliant?", get(row, hmap, config.aliases.EMDAN));
            m.put("Comments", get(row, hmap, config.aliases.COMMENTS));
            nurseCallRows.add(m);
        }
    }

    private void parsePatientMonitoring() {
        Sheet sheet = wb.getSheet(config.sheets.patientMonitoring);
        if (sheet == null) return;
        Set<String> expected = new HashSet<>() {{
            addAll(config.aliases.CFG_GROUP);
            addAll(config.aliases.ALERT_NAME_COMMON);
            addAll(config.aliases.SENDING_NAME);
            addAll(config.aliases.PRIORITY);
        }};
        int headerRowIdx = HeaderFinder.findHeaderRow(sheet, expected, 40);
        if (headerRowIdx < 0) return;
        Row header = sheet.getRow(headerRowIdx);
        Map<String,Integer> hmap = headerIndex(header);

        for (int r = headerRowIdx + 1; r <= sheet.getLastRowNum(); r++) {
            Row row = sheet.getRow(r);
            if (row == null) continue;
            String cfg = get(row, hmap, config.aliases.CFG_GROUP);
            String alert = get(row, hmap, config.aliases.ALERT_NAME_COMMON); // "Alarm Name"
            String send = get(row, hmap, config.aliases.SENDING_NAME);
            if (cfg.isBlank() && alert.isBlank() && send.isBlank()) continue;

            Map<String,String> m = new LinkedHashMap<>();
            m.put("Configuration Group", cfg);
            m.put("Alarm Name", alert);
            m.put("Sending System Alarm Name", send);
            m.put("Priority", get(row, hmap, config.aliases.PRIORITY));
            m.put("Device - A", get(row, hmap, config.aliases.DEVICE_A));
            m.put("1st Recipient", get(row, hmap, config.aliases.R1));
            m.put("2nd Recipient", get(row, hmap, config.aliases.R2));
            m.put("Response Options", get(row, hmap, config.aliases.RESPONSE));
            patientMonRows.add(m);
        }
    }

    // ---- Build JSON in desired structure ----
    public Map<String, Object> buildJson() {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("version", config.outputVersion);

        // alarmAlertDefinitions (from both NC + PM)
        List<Map<String, Object>> definitions = new ArrayList<>();

        for (var r : nurseCallRows) {
            String name = coalesce(r.get("Common Alert or Alarm Name"), r.get("Alarm Name"));
            String send = coalesce(r.get("Sending System Alert Name"), r.get("Sending System Alarm Name"));
            if (name == null || name.isBlank()) continue;
            Map<String,Object> def = new LinkedHashMap<>();
            def.put("name", name);
            def.put("type", "NurseCall");
            List<Map<String,String>> values = new ArrayList<>();
            if (send != null && !send.isBlank()) values.add(Map.of("category","SendingSystem","value", send));
            def.put("values", values);
            definitions.add(def);
        }
        for (var r : patientMonRows) {
            String name = coalesce(r.get("Alarm Name"), r.get("Common Alert or Alarm Name"));
            String send = coalesce(r.get("Sending System Alarm Name"), r.get("Sending System Alert Name"));
            if (name == null || name.isBlank()) continue;
            Map<String,Object> def = new LinkedHashMap<>();
            def.put("name", name);
            def.put("type", "PatientMonitoring");
            List<Map<String,String>> values = new ArrayList<>();
            if (send != null && !send.isBlank()) values.add(Map.of("category","SendingSystem","value", send));
            def.put("values", values);
            definitions.add(def);
        }
        // de-duplicate by name+type
        definitions = definitions.stream()
                .collect(Collectors.toMap(
                        m -> (m.get("name")+"|"+m.get("type")),
                        m -> m,
                        (a,b)->a,
                        LinkedHashMap::new
                ))
                .values().stream().collect(Collectors.toList());
        root.put("alarmAlertDefinitions", definitions);

        // deliveryFlows grouped by Configuration Group
        Map<String, Map<String,Object>> flowsByGroup = new LinkedHashMap<>();

        // Nurse Call rows
        for (var r : nurseCallRows) {
            String cfg = nvl(r.get("Configuration Group"), "General NC");
            Map<String,Object> flow = flowsByGroup.computeIfAbsent(cfg, k -> baseFlow(k, nvl(r.get("Priority"), "Medium")));
            // alarmsAlerts link
            String alert = nvl(r.get("Common Alert or Alarm Name"), null);
            if (alert != null) addToSetList(flow, "alarmsAlerts", alert);
            // interfaces and params
            addInterfaces(flow, r.get("Device - A"));
            addParam(flow, 0, "RingtoneDeviceA", r.get("Ringtone Device - A"));
            addParam(flow, 0, "ResponseOptions", r.get("Response Options"));
            addParam(flow, 0, "EMDAN", r.get("EMDAN Compliant?"));
            addParam(flow, 0, "Comments", r.get("Comments"));
            // recipients with parsing
            addRecipients(flow, r.get("Time to 1st Recipient"), r.get("1st Recipient"), 1);
            addRecipients(flow, r.get("Time to 2nd Recipient"), r.get("2nd Recipient"), 2);
        }
        // Patient Monitoring rows
        for (var r : patientMonRows) {
            String cfg = nvl(r.get("Configuration Group"), "General PM");
            Map<String,Object> flow = flowsByGroup.computeIfAbsent(cfg, k -> baseFlow(k, nvl(r.get("Priority"), "Medium")));
            String alert = nvl(r.get("Alarm Name"), null);
            if (alert != null) addToSetList(flow, "alarmsAlerts", alert);
            addInterfaces(flow, r.get("Device - A"));
            addParam(flow, 0, "ResponseOptions", r.get("Response Options"));
            // recipients
            addRecipients(flow, null, r.get("1st Recipient"), 1);
            addRecipients(flow, null, r.get("2nd Recipient"), 2);
        }

        // units scoping
        Map<String, List<Map<String,String>>> unitsByGroup = indexUnitsByGroup(unitRows);
        for (var entry : flowsByGroup.entrySet()) {
            String group = entry.getKey();
            Map<String,Object> flow = entry.getValue();
            List<Map<String,String>> unitRefs = unitsByGroup.getOrDefault(group, Collections.emptyList());
            if (unitRefs.isEmpty() && config.attachAllUnitsIfNoMatch) {
                // fallback attach all
                unitRefs = allUnits(unitRows);
            }
            if (!unitRefs.isEmpty()) flow.put("units", unitRefs);
            flow.putIfAbsent("status", "Enabled");
        }

        root.put("deliveryFlows", new ArrayList<>(flowsByGroup.values()));
        return root;
    }

    private static Map<String,Object> baseFlow(String name, String priority) {
        Map<String,Object> flow = new LinkedHashMap<>();
        flow.put("name", name);
        flow.put("priority", priority);
        flow.put("status", "Enabled");
        flow.put("alarmsAlerts", new ArrayList<>());
        flow.put("conditions", new ArrayList<>());
        flow.put("destinations", new ArrayList<>());
        flow.put("interfaces", new ArrayList<>());
        flow.put("parameterAttributes", new ArrayList<>());
        flow.put("units", new ArrayList<>());
        return flow;
    }

    private void addInterfaces(Map<String,Object> flow, String deviceA) {
        if (deviceA == null || deviceA.isBlank()) return;
        List<Map<String,String>> ifaces = (List<Map<String,String>>) flow.get("interfaces");
        ifaces.add(Map.of("componentName", deviceA, "referenceName", deviceA));
    }

    private void addParam(Map<String,Object> flow, int order, String name, String value) {
        if (value == null || value.isBlank()) return;
        List<Map<String,Object>> params = (List<Map<String,Object>>) flow.get("parameterAttributes");
        Map<String,Object> p = new LinkedHashMap<>();
        p.put("destinationOrder", order);
        p.put("name", name);
        p.put("value", value);
        params.add(p);
    }

    private void addRecipients(Map<String,Object> flow, String delayStr, String recipients, int order) {
        if ((recipients == null || recipients.isBlank()) && (delayStr == null || delayStr.isBlank())) return;
        List<Map<String,Object>> dests = (List<Map<String,Object>>) flow.get("destinations");
        // parse recipients (split by comma/semicolon/newline)
        List<String> items = splitList(recipients);
        List<String> groups = new ArrayList<>();
        List<String> roles = new ArrayList<>();
        for (String item : items) {
            String trimmed = item.trim();
            if (trimmed.isEmpty()) continue;
            if (trimmed.matches(config.recipientParsing.functionalRoleRegex)) {
                roles.add(trimmed);
            } else {
                groups.add(trimmed);
            }
        }
        Map<String,Object> d = new LinkedHashMap<>();
        d.put("order", order);
        d.put("delayTime", parseIntSafe(delayStr));
        d.put("recipientType", roles.isEmpty() ? "Groups" : "Mixed");
        d.put("destinationType", "Mobile");
        d.put("users", new ArrayList<>());
        d.put("groups", groups);
        d.put("functionalRoles", roles);
        d.put("presenceConfig", "");
        dests.add(d);
    }

    private static List<String> splitList(String s) {
        if (s == null) return List.of();
        return Arrays.stream(s.split("[;,
]"))
                .map(String::trim)
                .filter(x -> !x.isEmpty())
                .collect(Collectors.toList());
    }

    private Map<String, List<Map<String,String>>> indexUnitsByGroup(List<Map<String,String>> units) {
        Map<String, List<Map<String,String>>> map = new LinkedHashMap<>();
        for (var r : units) {
            String facility = nvl(r.get("Facility"), null);
            String name = nvl(r.get("Common Unit Name"), null);
            String groups = nvl(r.get("Groups"), "");
            if (facility == null || name == null) continue;
            List<String> groupList = splitList(groups);
            if (groupList.isEmpty()) continue;
            Map<String,String> unitRef = Map.of("facilityName", facility, "name", name);
            for (String g : groupList) {
                map.computeIfAbsent(g, k -> new ArrayList<>()).add(unitRef);
            }
        }
        return map;
    }

    private static List<Map<String,String>> allUnits(List<Map<String,String>> unitsRaw) {
        List<Map<String,String>> out = new ArrayList<>();
        for (var r : unitsRaw) {
            String facility = nvl(r.get("Facility"), null);
            String name = nvl(r.get("Common Unit Name"), null);
            if (facility != null && name != null) out.add(Map.of("facilityName", facility, "name", name));
        }
        return out;
    }

    private static Integer parseIntSafe(String s) {
        try {
            if (s == null) return 0;
            s = s.trim();
            if (s.isEmpty()) return 0;
            String digits = s.replaceAll("[^0-9]", "");
            if (digits.isEmpty()) return 0;
            return Integer.parseInt(digits);
        } catch (Exception e) { return 0; }
    }

    private static String nvl(String s, String d) { return (s == null || s.isBlank()) ? d : s; }
    private static String coalesce(String... vals) {
        for (String v : vals) if (v != null && !v.isBlank()) return v; return null;
    }
}
```java
package com.example.exceljson;

import com.example.exceljson.model.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.util.*;
import java.util.stream.Collectors;

import static com.example.exceljson.HeaderFinder.*;

public class ExcelParser {
    private Workbook wb;

    // Raw rows as maps (for preview tables)
    private List<Map<String,String>> unitRows = new ArrayList<>();
    private List<Map<String,String>> nurseCallRows = new ArrayList<>();
    private List<Map<String,String>> patientMonRows = new ArrayList<>();

    public void load(File excel) throws Exception {
        try (FileInputStream fis = new FileInputStream(excel)) {
            wb = new XSSFWorkbook(fis);
        }
        parseUnitBreakdown();
        parseNurseCall();
        parsePatientMonitoring();
    }

    public List<Map<String,String>> getUnitBreakdownRows(){ return unitRows; }
    public List<Map<String,String>> getNurseCallRows(){ return nurseCallRows; }
    public List<Map<String,String>> getPatientMonitoringRows(){ return patientMonRows; }

    // ---- Parsing helpers ----
    private static Map<String,Integer> headerIndex(Row header) {
        Map<String,Integer> map = new LinkedHashMap<>();
        for (int c = 0; c < header.getLastCellNum(); c++) {
            String key = normalize(getString(header, c));
            if (!key.isBlank()) map.put(key, c);
        }
        return map;
    }
    private static String get(Row row, Map<String,Integer> hmap, Set<String> aliases){
        for (String a : aliases) {
            Integer idx = hmap.get(a);
            if (idx != null) return HeaderFinder.getString(row, idx).trim();
        }
        return "";
    }

    private void parseUnitBreakdown() {
        Sheet sheet = wb.getSheet("Unit Breakdown");
        if (sheet == null) return;
        Set<String> expected = new HashSet<>();
        expected.addAll(MappingAliases.FACILITY);
        expected.addAll(MappingAliases.UNIT_NAME);

        int headerRowIdx = HeaderFinder.findHeaderRow(sheet, expected, 20);
        if (headerRowIdx < 0) return;
        Row header = sheet.getRow(headerRowIdx);
        Map<String,Integer> hmap = headerIndex(header);

        for (int r = headerRowIdx + 1; r <= sheet.getLastRowNum(); r++) {
            Row row = sheet.getRow(r);
            if (row == null) continue;
            String facility = get(row, hmap, MappingAliases.FACILITY);
            String unit = get(row, hmap, MappingAliases.UNIT_NAME);
            if (facility.isBlank() && unit.isBlank()) continue;

            Map<String,String> map = new LinkedHashMap<>();
            map.put("Facility", facility);
            map.put("Common Unit Name", unit);
            unitRows.add(map);
        }
    }

    private void parseNurseCall() {
        Sheet sheet = wb.getSheet("Nurse Call");
        if (sheet == null) return;
        Set<String> expected = new HashSet<>() {{
            addAll(MappingAliases.CFG_GROUP);
            addAll(MappingAliases.ALERT_NAME_COMMON);
            addAll(MappingAliases.SENDING_NAME);
            addAll(MappingAliases.PRIORITY);
        }};
        int headerRowIdx = HeaderFinder.findHeaderRow(sheet, expected, 30);
        if (headerRowIdx < 0) return;
        Row header = sheet.getRow(headerRowIdx);
        Map<String,Integer> hmap = headerIndex(header);

        for (int r = headerRowIdx + 1; r <= sheet.getLastRowNum(); r++) {
            Row row = sheet.getRow(r);
            if (row == null) continue;
            String cfg = get(row, hmap, MappingAliases.CFG_GROUP);
            String alert = get(row, hmap, MappingAliases.ALERT_NAME_COMMON);
            String send = get(row, hmap, MappingAliases.SENDING_NAME);
            if (cfg.isBlank() && alert.isBlank() && send.isBlank()) continue;

            Map<String,String> m = new LinkedHashMap<>();
            m.put("Configuration Group", cfg);
            m.put("Common Alert or Alarm Name", alert);
            m.put("Sending System Alert Name", send);
            m.put("Priority", get(row, hmap, MappingAliases.PRIORITY));
            m.put("Device - A", get(row, hmap, MappingAliases.DEVICE_A));
            m.put("Ringtone Device - A", get(row, hmap, MappingAliases.RINGTONE_A));
            m.put("Time to 1st Recipient", get(row, hmap, MappingAliases.T1));
            m.put("1st Recipient", get(row, hmap, MappingAliases.R1));
            m.put("Time to 2nd Recipient", get(row, hmap, MappingAliases.T2));
            m.put("2nd Recipient", get(row, hmap, MappingAliases.R2));
            m.put("Response Options", get(row, hmap, MappingAliases.RESPONSE));
            m.put("EMDAN Compliant?", get(row, hmap, MappingAliases.EMDAN));
            m.put("Comments", get(row, hmap, MappingAliases.COMMENTS));
            nurseCallRows.add(m);
        }
    }

    private void parsePatientMonitoring() {
        Sheet sheet = wb.getSheet("Patient Monitoring");
        if (sheet == null) return;
        Set<String> expected = new HashSet<>() {{
            addAll(MappingAliases.CFG_GROUP);
            addAll(MappingAliases.ALERT_NAME_COMMON); // covers "Alarm Name"
            addAll(MappingAliases.SENDING_NAME);
            addAll(MappingAliases.PRIORITY);
        }};
        int headerRowIdx = HeaderFinder.findHeaderRow(sheet, expected, 30);
        if (headerRowIdx < 0) return;
        Row header = sheet.getRow(headerRowIdx);
        Map<String,Integer> hmap = headerIndex(header);

        for (int r = headerRowIdx + 1; r <= sheet.getLastRowNum(); r++) {
            Row row = sheet.getRow(r);
            if (row == null) continue;
            String cfg = get(row, hmap, MappingAliases.CFG_GROUP);
            String alert = get(row, hmap, MappingAliases.ALERT_NAME_COMMON); // "Alarm Name"
            String send = get(row, hmap, MappingAliases.SENDING_NAME);
            if (cfg.isBlank() && alert.isBlank() && send.isBlank()) continue;

            Map<String,String> m = new LinkedHashMap<>();
            m.put("Configuration Group", cfg);
            m.put("Alarm Name", alert);
            m.put("Sending System Alarm Name", send);
            m.put("Priority", get(row, hmap, MappingAliases.PRIORITY));
            m.put("Device - A", get(row, hmap, MappingAliases.DEVICE_A));
            m.put("1st Recipient", get(row, hmap, MappingAliases.R1));
            m.put("2nd Recipient", get(row, hmap, MappingAliases.R2));
            m.put("Response Options", get(row, hmap, MappingAliases.RESPONSE));
            patientMonRows.add(m);
        }
    }

    // ---- Build JSON in desired structure ----
    public Map<String, Object> buildJson() {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("version", "1.0");

        // alarmAlertDefinitions (from both NC + PM)
        List<Map<String, Object>> definitions = new ArrayList<>();

        for (var r : nurseCallRows) {
            String name = coalesce(r.get("Common Alert or Alarm Name"), r.get("Alarm Name"));
            String send = coalesce(r.get("Sending System Alert Name"), r.get("Sending System Alarm Name"));
            if (name == null || name.isBlank()) continue;
            Map<String,Object> def = new LinkedHashMap<>();
            def.put("name", name);
            def.put("type", "NurseCall");
            List<Map<String,String>> values = new ArrayList<>();
            if (send != null && !send.isBlank()) {
                values.add(Map.of("category","SendingSystem","value", send));
            }
            def.put("values", values);
            definitions.add(def);
        }
        for (var r : patientMonRows) {
            String name = coalesce(r.get("Alarm Name"), r.get("Common Alert or Alarm Name"));
            String send = coalesce(r.get("Sending System Alarm Name"), r.get("Sending System Alert Name"));
            if (name == null || name.isBlank()) continue;
            Map<String,Object> def = new LinkedHashMap<>();
            def.put("name", name);
            def.put("type", "PatientMonitoring");
            List<Map<String,String>> values = new ArrayList<>();
            if (send != null && !send.isBlank()) {
                values.add(Map.of("category","SendingSystem","value", send));
            }
            def.put("values", values);
            definitions.add(def);
        }
        // de-duplicate by name+type
        definitions = definitions.stream()
                .collect(Collectors.toMap(
                        m -> (m.get("name")+"|"+m.get("type")),
                        m -> m,
                        (a,b)->a,
                        LinkedHashMap::new
                ))
                .values().stream().collect(Collectors.toList());
        root.put("alarmAlertDefinitions", definitions);

        // deliveryFlows grouped by Configuration Group
        Map<String, Map<String,Object>> flowsByGroup = new LinkedHashMap<>();

        // helper to add destination
        java.util.function.BiConsumer<Map<String,Object>, Map<String,String>> addDestinations = (flow, row) -> {
            List<Map<String,Object>> dests = (List<Map<String,Object>>) flow.get("destinations");
            if (dests == null) { dests = new ArrayList<>(); flow.put("destinations", dests); }

            addDest(dests, 1, row.get("Time to 1st Recipient"), row.get("1st Recipient"));
            addDest(dests, 2, row.get("Time to 2nd Recipient"), row.get("2nd Recipient"));
        };

        // Nurse Call rows
        for (var r : nurseCallRows) {
            String cfg = nvl(r.get("Configuration Group"), "General NC");
            Map<String,Object> flow = flowsByGroup.computeIfAbsent(cfg, k -> baseFlow(k, nvl(r.get("Priority"), "Medium")));
            // alarmsAlerts link
            String alert = nvl(r.get("Common Alert or Alarm Name"), null);
            if (alert != null) addToSetList(flow, "alarmsAlerts", alert);
            // interfaces and params
            addInterfaces(flow, r.get("Device - A"));
            addParam(flow, 0, "RingtoneDeviceA", r.get("Ringtone Device - A"));
            addParam(flow, 0, "ResponseOptions", r.get("Response Options"));
            addParam(flow, 0, "EMDAN", r.get("EMDAN Compliant?"));
            addParam(flow, 0, "Comments", r.get("Comments"));
            // recipients
            addDestinations.accept(flow, r);
        }
        // Patient Monitoring rows
        for (var r : patientMonRows) {
            String cfg = nvl(r.get("Configuration Group"), "General PM");
            Map<String,Object> flow = flowsByGroup.computeIfAbsent(cfg, k -> baseFlow(k, nvl(r.get("Priority"), "Medium")));
            String alert = nvl(r.get("Alarm Name"), null);
            if (alert != null) addToSetList(flow, "alarmsAlerts", alert);
            addInterfaces(flow, r.get("Device - A"));
            addParam(flow, 0, "ResponseOptions", r.get("Response Options"));
            // recipients
            List<Map<String,Object>> dests = (List<Map<String,Object>>) flow.get("destinations");
            if (dests == null) { dests = new ArrayList<>(); flow.put("destinations", dests); }
            addDest(dests, 1, null, r.get("1st Recipient"));
            addDest(dests, 2, null, r.get("2nd Recipient"));
        }

        // units (from Unit Breakdown)
        List<Map<String,String>> unitsRaw = unitRows;
        List<Map<String,String>> unitRefs = new ArrayList<>();
        for (var r : unitsRaw) {
            String facility = nvl(r.get("Facility"), null);
            String name = nvl(r.get("Common Unit Name"), null);
            if (facility != null && name != null) {
                unitRefs.add(Map.of("facilityName", facility, "name", name));
            }
        }
        for (var f : flowsByGroup.values()) {
            if (!unitRefs.isEmpty()) f.put("units", unitRefs);
            f.putIfAbsent("status", "Enabled");
        }

        root.put("deliveryFlows", new ArrayList<>(flowsByGroup.values()));
        return root;
    }

    private static Map<String,Object> baseFlow(String name, String priority) {
        Map<String,Object> flow = new LinkedHashMap<>();
        flow.put("name", name);
        flow.put("priority", priority);
        flow.put("status", "Enabled");
        flow.put("alarmsAlerts", new ArrayList<>());
        flow.put("conditions", new ArrayList<>());
        flow.put("destinations", new ArrayList<>());
        flow.put("interfaces", new ArrayList<>());
        flow.put("parameterAttributes", new ArrayList<>());
        flow.put("units", new ArrayList<>());
        return flow;
    }

    private static void addToSetList(Map<String,Object> obj, String key, String value) {
        List<String> list = (List<String>) obj.computeIfAbsent(key, k -> new ArrayList<String>());
        if (!list.contains(value)) list.add(value);
    }

    private static void addInterfaces(Map<String,Object> flow, String deviceA) {
        if (deviceA == null || deviceA.isBlank()) return;
        List<Map<String,String>> ifaces = (List<Map<String,String>>) flow.get("interfaces");
        ifaces.add(Map.of("componentName", deviceA, "referenceName", deviceA));
    }

    private static void addParam(Map<String,Object> flow, int order, String name, String value) {
        if (value == null || value.isBlank()) return;
        List<Map<String,Object>> params = (List<Map<String,Object>>) flow.get("parameterAttributes");
        Map<String,Object> p = new LinkedHashMap<>();
        p.put("destinationOrder", order);
        p.put("name", name);
        p.put("value", value);
        params.add(p);
    }

    private static void addDest(List<Map<String,Object>> dests, int order, String delayStr, String recipient) {
        if ((recipient == null || recipient.isBlank()) && (delayStr == null || delayStr.isBlank())) return;
        Map<String,Object> d = new LinkedHashMap<>();
        d.put("order", order);
        d.put("delayTime", parseIntSafe(delayStr));
        d.put("recipientType", "Groups");
        d.put("destinationType", "Mobile");
        d.put("users", new ArrayList<>());
        d.put("groups", recipient == null ? new ArrayList<>() : List.of(recipient));
        d.put("functionalRoles", new ArrayList<>());
        d.put("presenceConfig", "");
        dests.add(d);
    }

    private static Integer parseIntSafe(String s) {
        try {
            if (s == null) return 0;
            s = s.trim();
            if (s.isEmpty()) return 0;
            // accept values like "10", "10 min", "10 minutes"
            String digits = s.replaceAll("[^0-9]", "");
            if (digits.isEmpty()) return 0;
            return Integer.parseInt(digits);
        } catch (Exception e) { return 0; }
    }

    private static String nvl(String s, String d) { return (s == null || s.isBlank()) ? d : s; }
    private static String coalesce(String... vals) {
        for (String v : vals) if (v != null && !v.isBlank()) return v; return null;
    }
}
````

---

## 🆕 `Config.java`

```java
package com.example.exceljson;

import java.util.*;

public class Config {
    public String outputVersion = "1.0";
    public boolean attachAllUnitsIfNoMatch = true;

    public Sheets sheets = new Sheets();
    public MappingAliases aliases = defaults().aliases; // will be overwritten by merge
    public RecipientParsing recipientParsing = new RecipientParsing();

    public static class Sheets {
        public String unitBreakdown = "Unit Breakdown";
        public String nurseCall = "Nurse Call";
        public String patientMonitoring = "Patient Monitoring";
    }

    public static class RecipientParsing {
        // Regex to classify functional roles; default detects leading "VAssign:" or contains "[Room]"
        public String functionalRoleRegex = "^(?i)(vassign:.*|.*\[room\].*)$";
    }

    public static Config defaultConfig() { return defaults(); }

    private static Config defaults() {
        Config c = new Config();
        c.aliases = new MappingAliases();
        return c;
    }

    public static Config mergeWithDefaults(Config override) {
        Config base = defaults();
        if (override == null) return base;
        Config out = new Config();
        out.outputVersion = nvl(override.outputVersion, base.outputVersion);
        out.attachAllUnitsIfNoMatch = override.attachAllUnitsIfNoMatch;

        out.sheets = new Sheets();
        out.sheets.unitBreakdown = nvl(override.sheets.unitBreakdown, base.sheets.unitBreakdown);
        out.sheets.nurseCall = nvl(override.sheets.nurseCall, base.sheets.nurseCall);
        out.sheets.patientMonitoring = nvl(override.sheets.patientMonitoring, base.sheets.patientMonitoring);

        out.aliases = mergeAliases(base.aliases, override.aliases);

        out.recipientParsing = new RecipientParsing();
        out.recipientParsing.functionalRoleRegex = nvl(
                override.recipientParsing == null ? null : override.recipientParsing.functionalRoleRegex,
                base.recipientParsing.functionalRoleRegex
        );
        return out;
    }

    private static MappingAliases mergeAliases(MappingAliases a, MappingAliases b) {
        if (b == null) return a;
        MappingAliases r = new MappingAliases();
        r.CFG_GROUP = or(b.CFG_GROUP, a.CFG_GROUP);
        r.ALERT_NAME_COMMON = or(b.ALERT_NAME_COMMON, a.ALERT_NAME_COMMON);
        r.SENDING_NAME = or(b.SENDING_NAME, a.SENDING_NAME);
        r.PRIORITY = or(b.PRIORITY, a.PRIORITY);
        r.DEVICE_A = or(b.DEVICE_A, a.DEVICE_A);
        r.RINGTONE_A = or(b.RINGTONE_A, a.RINGTONE_A);
        r.T1 = or(b.T1, a.T1);
        r.R1 = or(b.R1, a.R1);
        r.T2 = or(b.T2, a.T2);
        r.R2 = or(b.R2, a.R2);
        r.RESPONSE = or(b.RESPONSE, a.RESPONSE);
        r.EMDAN = or(b.EMDAN, a.EMDAN);
        r.COMMENTS = or(b.COMMENTS, a.COMMENTS);
        r.FACILITY = or(b.FACILITY, a.FACILITY);
        r.UNIT_NAME = or(b.UNIT_NAME, a.UNIT_NAME);
        r.UNIT_GROUPS = or(b.UNIT_GROUPS, a.UNIT_GROUPS);
        return r;
    }

    private static Set<String> or(Set<String> left, Set<String> right) {
        return (left != null && !left.isEmpty()) ? left : right;
    }

    private static String nvl(String v, String d) { return (v == null || v.isBlank()) ? d : v; }
}
```

---

## 🧱 Model classes (POJOs) – optional

> These are provided if you later prefer strong typing + Jackson annotations. They are not required for this version since we construct JSON via `Map`.

### `model/AlarmAlertDefinition.java`

```java
package com.example.exceljson.model;
public class AlarmAlertDefinition { /* optional */ }
```

*(Similarly for `ValuePair`, `DeliveryFlow`, `Destination`, `InterfaceRef`, `ParameterAttribute`, `UnitRef`, `Condition`)*

---

## 🧰 `util/FXTableUtils.java`

```java
package com.example.exceljson.util;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.util.*;

public class FXTableUtils {
    public static void populate(TableView<Map<String, String>> table, List<Map<String, String>> rows) {
        table.getColumns().clear();
        table.getItems().clear();
        if (rows == null || rows.isEmpty()) return;

        // union of keys
        LinkedHashSet<String> keys = new LinkedHashSet<>();
        rows.forEach(m -> keys.addAll(m.keySet()));

        for (String k : keys) {
            TableColumn<Map<String, String>, String> col = new TableColumn<>(k);
            col.setCellValueFactory(data -> new SimpleStringProperty(Optional.ofNullable(data.getValue().get(k)).orElse("")));
            col.setPrefWidth(180);
            table.getColumns().add(col);
        }
        table.setItems(FXCollections.observableArrayList(rows));
    }
}
```

---

## ▶️ Build & Run

```bash
# From the project root
mvn clean package
mvn javafx:run
```

Or build a fat JAR (you can then run via `java -jar` – note you’ll need JavaFX modules on path unless you use jlink):

```bash
mvn -DskipTests package
```

The runnable class is `com.example.exceljson.Main`.

---

## 🧪 How to Use

1. **Open Excel…** and select your workbook (e.g., `Configuration_file.xlsx`).
2. Review parsed rows in the **Unit Breakdown**, **Nurse Call**, **Patient Monitoring** tabs.
3. Switch to **JSON Preview** to confirm the structure matches your target (like `2025-10-22.json`).
4. Click **Export JSON…** to write the final file.

---

## 🛠️ Tuning Header Detection / Aliases

* You can now load an external **config file** (`Open Config…`) in **YAML or JSON** to override:

  * **Sheet names** (e.g., rename tabs without code changes)
  * **Header aliases** (column names)
  * **Recipient parsing rule** (regex for functional roles)
  * **Unit scoping behavior** (`attachAllUnitsIfNoMatch`)

### Example `config.yml`

```yaml
outputVersion: "1.0"
attachAllUnitsIfNoMatch: true

sheets:
  unitBreakdown: "Unit Breakdown"
  nurseCall: "Nurse Call"
  patientMonitoring: "Patient Monitoring"

aliases:
  CFG_GROUP: ["configuration group", "config group", "workflow group"]
  ALERT_NAME_COMMON: ["common alert or alarm name", "alarm name"]
  SENDING_NAME: ["sending system alert name", "sending system alarm name"]
  PRIORITY: ["priority"]
  DEVICE_A: ["device - a", "device"]
  RINGTONE_A: ["ringtone device - a", "ringtone"]
  T1: ["time to 1st recipient", "time to first recipient", "delay to 1st"]
  R1: ["1st recipient", "first recipient"]
  T2: ["time to 2nd recipient", "time to second recipient", "delay to 2nd"]
  R2: ["2nd recipient", "second recipient"]
  RESPONSE: ["response options"]
  EMDAN: ["emdan compliant", "emdan compliant?"]
  COMMENTS: ["comments"]
  FACILITY: ["facility"]
  UNIT_NAME: ["common unit name", "unit"]
  UNIT_GROUPS: ["configuration group", "applies to", "workflow group", "delivery flow"]

recipientParsing:
  functionalRoleRegex: "^(?i)(vassign:.*|.*\[room\].*)$"
```

> Place your group names (from **Unit Breakdown** column matched by `UNIT_GROUPS`) as comma‑ or semicolon‑separated values. Those units will attach **only** to matching `Configuration Group` flows. If no match exists and `attachAllUnitsIfNoMatch: true`, all units will be attached as a fallback.

---

## 🔄 Mapping Recap (implemented)

* **alarmAlertDefinitions** built from both **Nurse Call** and **Patient Monitoring** rows.
* **deliveryFlows** grouped by **Configuration Group**. For each group, we:

  * Add each alarm name to `alarmsAlerts`.
  * Map **Device - A** into `interfaces[].componentName/referenceName`.
  * Map **Response Options**, **EMDAN**, **Comments** into `parameterAttributes`.
  * Build `destinations` from **1st/2nd Recipient** (with optional delays).
  * Attach **units** from **Unit Breakdown** (facility & unit name pairs) to every flow (adjust if you want per‑group scoping).

> If you’d like **units to be scoped to specific flows** (e.g., only certain units should attach to a given Configuration Group), provide the mapping rule (e.g., a column that ties units to group), and we’ll adjust `buildJson()` accordingly.

---

## 📌 Notes / Assumptions

* Delay parsing accepts values like `10`, `10 min`, or `10 minutes` and converts to integer minutes.
* Recipients are stored under `destinations[].groups`. If you prefer `functionalRoles` for entries like `VAssign: [Room] Nurse`, we can detect that pattern and place them accordingly.
* `status` defaults to `Enabled`.
* `version` is set to `1.0` (change as needed).

---

## 📣 Next Enhancements (on request)

* Configurable **mapping file** (YAML/JSON) so you can tweak column names without code changes.
* Add **CSV export** of parsed rows for auditing.
* Add a **diff view** that compares the generated JSON against an existing baseline (your `2025-10-22.json`).
* Smarter recipient parsing (split multiple recipients, detect `VAssign:` → functional role vs group, etc.).

---

If you want me to tailor the recipients/units scoping or add a mapping file now, tell me the exact rules and I’ll update the code accordingly.
