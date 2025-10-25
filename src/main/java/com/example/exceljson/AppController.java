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

import java.io.File;

public class AppController {

    private ExcelParserV3 parser;

    private final TableView<ExcelParserV3.UnitRow> tblUnits = new TableView<>();
    private final TableView<FlowRow> tblNurse = new TableView<>();
    private final TableView<FlowRow> tblClin = new TableView<>();
    private final TextArea jsonPreview = new TextArea();

    private File currentExcel;

    public void start(Stage stage) {
        stage.setTitle("Engage Rules Generator – JSON Builder");

        BorderPane root = new BorderPane();
        root.setPadding(new Insets(10));

        // top bar
        HBox top = new HBox(10);
        Button btnOpen = new Button("Open Excel…");
        Button btnSaveEdited = new Button("Save Edited Excel…");
        Button btnGenerate = new Button("Generate JSON Preview");
        top.getChildren().addAll(btnOpen, btnSaveEdited, btnGenerate);
        root.setTop(top);

        // tabs
        TabPane tabs = new TabPane();
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        Tab tUnits = new Tab("Unit Breakdown", tblUnits);
        Tab tNurse = new Tab("Nurse Calls", tblNurse);
        Tab tClin = new Tab("Patient Monitoring", tblClin);
        Tab tJson = new Tab("JSON Preview", new ScrollPane(jsonPreview));
        tabs.getTabs().addAll(tUnits, tNurse, tClin, tJson);
        root.setCenter(tabs);

        jsonPreview.setEditable(false);
        jsonPreview.setWrapText(true);
        jsonPreview.setStyle("-fx-font-family: 'Consolas', 'Courier New', monospace;");

        // actions
        btnOpen.setOnAction(e -> openExcel(stage));
        btnSaveEdited.setOnAction(e -> saveEdited(stage));
        btnGenerate.setOnAction(e -> generateJson());

        Scene scene = new Scene(root, 1300, 800);
        stage.setScene(scene);
        stage.show();
    }

    private void openExcel(Stage stage) {
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel", "*.xlsx", "*.xls"));
        File f = fc.showOpenDialog(stage);
        if (f == null) return;

        try {
            parser = new ExcelParserV3();
            parser.load(f);
            currentExcel = f;
            populateTables();
            info("Loaded", "Excel loaded successfully.");
        } catch (Exception ex) {
            error("Load failed", ex.getMessage());
            ex.printStackTrace();
        }
    }

    private void populateTables() {
        // Units
        tblUnits.setItems(FXCollections.observableArrayList(parser.units));
        tblUnits.getColumns().setAll(
                colEditable("Facility", ExcelParserV3.UnitRow::facilityProperty),
                colEditable("Common Unit Name", ExcelParserV3.UnitRow::unitNameProperty),
                checkboxCol("Nurse Call", ExcelParserV3.UnitRow::isInNurseCall, ExcelParserV3.UnitRow::setInNurseCall),
                checkboxCol("Patient Monitoring", ExcelParserV3.UnitRow::isInPatientMonitoring, ExcelParserV3.UnitRow::setInPatientMonitoring),
                colEditable("No Caregiver Alert Number or Group", ExcelParserV3.UnitRow::noCaregiverGroupProperty)
        );
        tblUnits.setEditable(true);

        // Nurse Calls
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

        // Clinicals
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

    private void saveEdited(Stage stage) {
        if (parser == null) { error("No file", "Load an Excel file first."); return; }
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel", "*.xlsx"));
        fc.setInitialFileName("Edited_Configuration.xlsx");
        File out = fc.showSaveDialog(stage);
        if (out == null) return;
        try {
            parser.exportEditedExcel(out);
            info("Saved", "Edited Excel saved:\n" + out.getAbsolutePath());
        } catch (Exception ex) {
            error("Save failed", ex.getMessage());
            ex.printStackTrace();
        }
    }

    private void generateJson() {
        if (parser == null) { error("No data", "Load a file first."); return; }
        try {
            var ncFile = parser.writeNurseCallsJson();
            var clFile = parser.writeClinicalsJson();
            String preview = ""
                    + "// " + ncFile.getName() + "\n"
                    + ExcelParserV3.pretty(parser.buildNurseCallsJson()) + "\n\n"
                    + "// " + clFile.getName() + "\n"
                    + ExcelParserV3.pretty(parser.buildClinicalsJson());
            jsonPreview.setText(preview);
            info("JSON written",
                    "Saved:\n" + ncFile.getAbsolutePath() + "\n" + clFile.getAbsolutePath() +
                    "\n(Also shown in the preview tab)");
        } catch (Exception ex) {
            error("JSON error", ex.getMessage());
            ex.printStackTrace();
        }
    }

    // ---- Table column helpers ----
    private <T> TableColumn<T, String> colEditable(String title, java.util.function.Function<T, javafx.beans.property.StringProperty> prop) {
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
        col.setPrefWidth(180);
        return col;
    }

    private <T> TableColumn<T, Boolean> checkboxCol(
            String title,
            java.util.function.Function<T, Boolean> getter,
            java.util.function.BiConsumer<T, Boolean> setter) {

        TableColumn<T, Boolean> col = new TableColumn<>(title);
        col.setCellValueFactory(data -> new javafx.beans.property.SimpleBooleanProperty(getter.apply(data.getValue())));
        col.setCellFactory(tc -> {
            CheckBoxTableCell<T, Boolean> cell = new CheckBoxTableCell<>();
            cell.setSelectedStateCallback(index -> {
                if (index.intValue() < 0 || index.intValue() >= tblUnits.getItems().size()) {
                    return new javafx.beans.property.SimpleBooleanProperty(false);
                }
                T item = tblUnits.getItems().get(index.intValue());
                return new javafx.beans.property.SimpleBooleanProperty(getter.apply(item));
            });
            return cell;
        });
        col.setEditable(true);
        col.setOnEditCommit(evt -> setter.accept(evt.getRowValue(), evt.getNewValue()));
        col.setPrefWidth(150);
        return col;
    }

    // ---- Alerts ----
    private void info(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
        a.setTitle(title); a.setHeaderText(null); a.showAndWait();
    }
    private void error(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);
        a.setTitle(title); a.setHeaderText(null); a.showAndWait();
    }
}
