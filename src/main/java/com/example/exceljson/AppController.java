package com.example.exceljson;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;

public class AppController {

    private ExcelParserV4 parser;
    private final TableView<UnitRow> tblUnits = new TableView<>();
    private final TableView<FlowRow> tblNurse = new TableView<>();
    private final TableView<FlowRow> tblClin = new TableView<>();
    private final TextArea jsonPreview = new TextArea();

    public void start(Stage stage) {
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(10));

        HBox top = new HBox(10);
        Button btnOpen = new Button("Open Excel…");
        Button btnGen = new Button("Generate JSON Preview");
        top.getChildren().addAll(btnOpen, btnGen);
        root.setTop(top);

        TabPane tabs = new TabPane();
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        Tab tUnits = new Tab("Unit Breakdown", tblUnits);
        Tab tNurse = new Tab("Nurse Calls", tblNurse);
        Tab tClin = new Tab("Patient Monitoring", tblClin);

        VBox jsonBox = new VBox(jsonPreview);
        VBox.setVgrow(jsonPreview, Priority.ALWAYS);
        jsonPreview.setStyle("-fx-font-family: 'Consolas'; -fx-font-size: 14;");
        jsonPreview.setWrapText(true);

        Tab tJson = new Tab("JSON Preview", jsonBox);
        tabs.getTabs().addAll(tUnits, tNurse, tClin, tJson);

        root.setCenter(tabs);
        Scene scene = new Scene(root, 1400, 850);
        stage.setScene(scene);
        stage.setTitle("Engage Rules Generator – Excel to JSON");
        stage.show();

        btnOpen.setOnAction(e -> openExcel(stage));
        btnGen.setOnAction(e -> generateJson());
    }

    private void openExcel(Stage stage) {
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel Files", "*.xlsx"));
        File file = fc.showOpenDialog(stage);
        if (file == null) return;

        try {
            parser = new ExcelParserV4();
            parser.load(file);
            populateTables();
            popup("✅ Excel Loaded", parser.getLoadSummary());
        } catch (Exception ex) {
            error("Error", ex.getMessage());
        }
    }

    private void populateTables() {
        tblUnits.setItems(FXCollections.observableArrayList(parser.units));
        tblUnits.getColumns().setAll(
                col("Facility", UnitRow::facilityProperty),
                col("Common Unit Name", UnitRow::unitNameProperty),
                col("Nurse Call Group", UnitRow::nurseCallGroupProperty),
                col("Patient Monitoring Group", UnitRow::patientMonitoringGroupProperty),
                col("No Caregiver Group", UnitRow::noCaregiverGroupProperty)
        );
        tblUnits.setEditable(true);

        tblNurse.setItems(FXCollections.observableArrayList(parser.nurseCalls));
        tblNurse.getColumns().setAll(
                col("Config Group", FlowRow::configGroupProperty),
                col("Alarm Name", FlowRow::alarmNameProperty),
                col("Sending Name", FlowRow::sendingNameProperty),
                col("Priority", FlowRow::priorityProperty),
                col("Ringtone", FlowRow::ringtoneProperty),
                col("1st Recipient", FlowRow::r1Property),
                col("2nd Recipient", FlowRow::r2Property)
        );
        tblNurse.setEditable(true);

        tblClin.setItems(FXCollections.observableArrayList(parser.clinicals));
        tblClin.getColumns().setAll(
                col("Config Group", FlowRow::configGroupProperty),
                col("Alarm Name", FlowRow::alarmNameProperty),
                col("Sending Name", FlowRow::sendingNameProperty),
                col("Priority", FlowRow::priorityProperty),
                col("Ringtone", FlowRow::ringtoneProperty),
                col("1st Recipient", FlowRow::r1Property),
                col("2nd Recipient", FlowRow::r2Property)
        );
        tblClin.setEditable(true);
    }

    private <T> TableColumn<T,String> col(String title, java.util.function.Function<T, javafx.beans.property.StringProperty> prop){
        TableColumn<T,String> c = new TableColumn<>(title);
        c.setCellValueFactory(d -> prop.apply(d.getValue()));
        c.setCellFactory(TextFieldTableCell.forTableColumn());
        c.setOnEditCommit(e -> prop.apply(e.getRowValue()).set(e.getNewValue()));
        c.setPrefWidth(180);
        return c;
    }

    private void generateJson() {
        try {
            String preview = ExcelParserV4.pretty(parser.buildNurseCallsJson()) + "\n\n" +
                    ExcelParserV4.pretty(parser.buildClinicalsJson());
            jsonPreview.setText(preview);
            jsonPreview.setScrollTop(0);
            popup("✅ JSON Generated", "Preview updated. JSON files ready to export.");
        } catch (Exception ex) {
            error("JSON Error", ex.getMessage());
        }
    }

    private void popup(String title, String msg){
        Alert a = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
        a.setHeaderText(null); a.setTitle(title); a.showAndWait();
    }
    private void error(String title, String msg){
        Alert a = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);
        a.setHeaderText(null); a.setTitle(title); a.showAndWait();
    }
}
