package com.example.exceljson;

import javafx.animation.FadeTransition;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

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
    @FXML private Button clearAllButton;
    @FXML private Button generateNurseJsonButton;
    @FXML private Button generateClinicalJsonButton;
    @FXML private Button generateOrdersJsonButton;
    @FXML private Button exportNurseJsonButton;
    @FXML private Button exportClinicalJsonButton;
    @FXML private Button exportOrdersJsonButton;
    @FXML private CheckBox mergeFlowsCheckbox;
    @FXML private TextField edgeRefNameField;
    @FXML private TextField vcsRefNameField;
    @FXML private TextField voceraRefNameField;
    @FXML private TextField xmppRefNameField;
    @FXML private CheckBox defaultEdgeCheckbox;
    @FXML private CheckBox defaultVmpCheckbox;
    @FXML private CheckBox defaultVoceraCheckbox;
    @FXML private CheckBox defaultXmppCheckbox;
    @FXML private Button resetDefaultsButton;
    @FXML private Button resetPathsButton;
    @FXML private TextField roomFilterNursecallField;
    @FXML private TextField roomFilterClinicalField;
    @FXML private TextField roomFilterOrdersField;
    @FXML private TextArea jsonPreview;
    @FXML private Label statusLabel;
    @FXML private Button themeToggleButton;
    @FXML private Button adapterCollapseButton;
    @FXML private VBox adapterSettingsContent;
    @FXML private ProgressIndicator progressIndicator;

    // ---------- Tabs ----------
    @FXML private TabPane mainTabs;
    @FXML private Tab tabUnits;
    @FXML private Tab tabNurse;
    @FXML private Tab tabClinicals;
    @FXML private Tab tabOrders;

    // ---------- Config Group Filters ----------
    @FXML private ComboBox<String> unitConfigGroupFilter;
    @FXML private ComboBox<String> nurseConfigGroupFilter;
    @FXML private ComboBox<String> clinicalConfigGroupFilter;
    @FXML private ComboBox<String> ordersConfigGroupFilter;

    // ---------- Units ----------
    @FXML private TableView<ExcelParserV5.UnitRow> tableUnits;
    @FXML private TableColumn<ExcelParserV5.UnitRow, String> unitFacilityCol;
    @FXML private TableColumn<ExcelParserV5.UnitRow, String> unitNamesCol;
    @FXML private TableColumn<ExcelParserV5.UnitRow, String> unitNurseGroupCol;
    @FXML private TableColumn<ExcelParserV5.UnitRow, String> unitClinicalGroupCol;
    @FXML private TableColumn<ExcelParserV5.UnitRow, String> unitOrdersGroupCol;
    @FXML private TableColumn<ExcelParserV5.UnitRow, String> unitNoCareGroupCol;
    @FXML private TableColumn<ExcelParserV5.UnitRow, String> unitCommentsCol;

    // ---------- Nurse Calls ----------
    @FXML private TableView<ExcelParserV5.FlowRow> tableNurseCalls;
    @FXML private TableColumn<ExcelParserV5.FlowRow, Boolean> nurseInScopeCol;
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
    @FXML private TableColumn<ExcelParserV5.FlowRow, Boolean> clinicalInScopeCol;
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

    // ---------- Orders ----------
    @FXML private TableView<ExcelParserV5.FlowRow> tableOrders;
    @FXML private TableColumn<ExcelParserV5.FlowRow, Boolean> ordersInScopeCol;
    @FXML private TableColumn<ExcelParserV5.FlowRow, String> ordersConfigGroupCol;
    @FXML private TableColumn<ExcelParserV5.FlowRow, String> ordersAlarmNameCol;
    @FXML private TableColumn<ExcelParserV5.FlowRow, String> ordersSendingNameCol;
    @FXML private TableColumn<ExcelParserV5.FlowRow, String> ordersPriorityCol;
    @FXML private TableColumn<ExcelParserV5.FlowRow, String> ordersDeviceACol;
    @FXML private TableColumn<ExcelParserV5.FlowRow, String> ordersDeviceBCol;
    @FXML private TableColumn<ExcelParserV5.FlowRow, String> ordersRingtoneCol;
    @FXML private TableColumn<ExcelParserV5.FlowRow, String> ordersResponseOptionsCol;
    @FXML private TableColumn<ExcelParserV5.FlowRow, String> ordersBreakThroughDNDCol;
    @FXML private TableColumn<ExcelParserV5.FlowRow, String> ordersEscalateAfterCol;
    @FXML private TableColumn<ExcelParserV5.FlowRow, String> ordersTtlValueCol;
    @FXML private TableColumn<ExcelParserV5.FlowRow, String> ordersEnunciateCol;
    @FXML private TableColumn<ExcelParserV5.FlowRow, String> ordersT1Col;
    @FXML private TableColumn<ExcelParserV5.FlowRow, String> ordersR1Col;
    @FXML private TableColumn<ExcelParserV5.FlowRow, String> ordersT2Col;
    @FXML private TableColumn<ExcelParserV5.FlowRow, String> ordersR2Col;
    @FXML private TableColumn<ExcelParserV5.FlowRow, String> ordersT3Col;
    @FXML private TableColumn<ExcelParserV5.FlowRow, String> ordersR3Col;
    @FXML private TableColumn<ExcelParserV5.FlowRow, String> ordersT4Col;
    @FXML private TableColumn<ExcelParserV5.FlowRow, String> ordersR4Col;
    @FXML private TableColumn<ExcelParserV5.FlowRow, String> ordersT5Col;
    @FXML private TableColumn<ExcelParserV5.FlowRow, String> ordersR5Col;

    // ---------- Core ----------
    private ExcelParserV5 parser;
    private File currentExcelFile;
    private String lastGeneratedJson = "";
    private boolean lastGeneratedWasNurseSide = true; // Track last generated JSON type
    private boolean isDarkMode = false; // Track current theme
    private boolean isAdapterSectionCollapsed = false; // Track adapter section state
    
    // Tab animation
    private FadeTransition tabFadeTransition = null;

    // ---------- Filtered Lists for Tables ----------
    private ObservableList<ExcelParserV5.UnitRow> unitsFullList;
    private FilteredList<ExcelParserV5.UnitRow> unitsFilteredList;
    
    private ObservableList<ExcelParserV5.FlowRow> nurseCallsFullList;
    private FilteredList<ExcelParserV5.FlowRow> nurseCallsFilteredList;
    
    private ObservableList<ExcelParserV5.FlowRow> clinicalsFullList;
    private FilteredList<ExcelParserV5.FlowRow> clinicalsFilteredList;
    
    private ObservableList<ExcelParserV5.FlowRow> ordersFullList;
    private FilteredList<ExcelParserV5.FlowRow> ordersFilteredList;

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
        initializeOrdersColumns();
        initializeFilters();

        // Set default reference names
        if (edgeRefNameField != null) edgeRefNameField.setText("OutgoingWCTP");
        if (vcsRefNameField != null) vcsRefNameField.setText("VMP");
        if (voceraRefNameField != null) voceraRefNameField.setText("Vocera");
        if (xmppRefNameField != null) xmppRefNameField.setText("XMPP");

        setJsonButtonsEnabled(false);
        setExcelButtonsEnabled(false);

        loadButton.setOnAction(e -> loadExcel());
        if (saveExcelButton != null) saveExcelButton.setOnAction(e -> saveExcel());
        if (saveExcelAsButton != null) saveExcelAsButton.setOnAction(e -> saveExcelAs());
        if (clearAllButton != null) clearAllButton.setOnAction(e -> clearAllData());
        generateNurseJsonButton.setOnAction(e -> generateJson("NurseCalls"));
        generateClinicalJsonButton.setOnAction(e -> generateJson("Clinicals"));
        if (generateOrdersJsonButton != null) generateOrdersJsonButton.setOnAction(e -> generateJson("Orders"));
        exportNurseJsonButton.setOnAction(e -> exportJson("NurseCalls"));
        exportClinicalJsonButton.setOnAction(e -> exportJson("Clinicals"));
        if (exportOrdersJsonButton != null) exportOrdersJsonButton.setOnAction(e -> exportJson("Orders"));
        if (resetDefaultsButton != null) resetDefaultsButton.setOnAction(e -> resetDefaults());
        if (resetPathsButton != null) resetPathsButton.setOnAction(e -> resetPaths());
        if (themeToggleButton != null) themeToggleButton.setOnAction(e -> toggleTheme());
        if (adapterCollapseButton != null) adapterCollapseButton.setOnAction(e -> toggleAdapterSection());

        // --- Interface reference name bindings ---
        // Update parser whenever text changes or Enter is pressed
        if (edgeRefNameField != null) {
            edgeRefNameField.setOnAction(e -> updateInterfaceRefs());
        }
        if (vcsRefNameField != null) {
            vcsRefNameField.setOnAction(e -> updateInterfaceRefs());
        }
        if (voceraRefNameField != null) {
            voceraRefNameField.setOnAction(e -> updateInterfaceRefs());
        }
        if (xmppRefNameField != null) {
            xmppRefNameField.setOnAction(e -> updateInterfaceRefs());
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
        if (voceraRefNameField != null) {
            voceraRefNameField.focusedProperty().addListener((obs, oldV, newV) -> { 
                if (!newV) updateInterfaceRefs(); 
            });
        }
        if (xmppRefNameField != null) {
            xmppRefNameField.focusedProperty().addListener((obs, oldV, newV) -> { 
                if (!newV) updateInterfaceRefs(); 
            });
        }
        
        // --- Default interface checkbox listeners ---
        if (defaultEdgeCheckbox != null) {
            defaultEdgeCheckbox.selectedProperty().addListener((obs, oldV, newV) -> {
                checkBothDefaultInterfacesSelected();
                updateInterfaceRefs();
            });
        }
        if (defaultVmpCheckbox != null) {
            defaultVmpCheckbox.selectedProperty().addListener((obs, oldV, newV) -> {
                checkBothDefaultInterfacesSelected();
                updateInterfaceRefs();
            });
        }
        if (defaultVoceraCheckbox != null) {
            defaultVoceraCheckbox.selectedProperty().addListener((obs, oldV, newV) -> {
                checkBothDefaultInterfacesSelected();
                updateInterfaceRefs();
            });
        }
        if (defaultXmppCheckbox != null) {
            defaultXmppCheckbox.selectedProperty().addListener((obs, oldV, newV) -> {
                checkBothDefaultInterfacesSelected();
                updateInterfaceRefs();
            });
        }
        
        // --- Smooth fade animation when tab changes ---
        if (mainTabs != null) {
            mainTabs.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
                if (newTab != null && newTab.getContent() != null) {
                    Node content = newTab.getContent();
                    
                    // Stop any ongoing animation
                    if (tabFadeTransition != null) {
                        tabFadeTransition.stop();
                    }
                    
                    // Create or reuse fade transition
                    if (tabFadeTransition == null) {
                        tabFadeTransition = new FadeTransition(Duration.millis(200));
                    }
                    
                    // Configure and play
                    tabFadeTransition.setNode(content);
                    tabFadeTransition.setFromValue(0.0);
                    tabFadeTransition.setToValue(1.0);
                    tabFadeTransition.play();
                }
            });
        }
    }

    // ---------- Load Excel ----------
    private void loadExcel() {
        try {
            setProgressVisible(true);
            statusLabel.setText("Loading Excel file...");
            
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Select Excel Workbook");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel Files", "*.xlsx"));

            if (lastExcelDir != null && lastExcelDir.exists()) {
                chooser.setInitialDirectory(lastExcelDir);
            }

            File file = chooser.showOpenDialog(getStage());
            if (file == null) {
                setProgressVisible(false);
                statusLabel.setText("Load cancelled");
                return;
            }

            // Remember directory
            rememberDirectory(file, true);

            parser.load(file);
            currentExcelFile = file;

            jsonPreview.setText(parser.getLoadSummary());

            refreshTables();
            tableUnits.refresh();
            tableNurseCalls.refresh();
            tableClinicals.refresh();
            if (tableOrders != null) tableOrders.refresh();

            setJsonButtonsEnabled(true);
            setExcelButtonsEnabled(true);
            
            updateStatusLabel(); // Update status with filter counts
            
            // Update window title with loaded file name
            ExcelJsonApplication.updateWindowTitle(file.getName());
            
            setProgressVisible(false);
            
            int movedCount = parser.getEmdanMovedCount();
            if (movedCount > 0) {
                showInfo("✅ Excel loaded successfully\nMoved " + movedCount + " EMDAN rows to Clinicals");
            } else {
                showInfo("✅ Excel loaded successfully");
            }
        } catch (Exception ex) {
            setProgressVisible(false);
            showError("Failed to load Excel: " + ex.getMessage());
            statusLabel.setText("Error loading Excel");
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
    private void generateJson(String flowType) {
        try {
            if (parser == null) {
                showError("Please load an Excel file first.");
                return;
            }

            setProgressVisible(true);
            statusLabel.setText("Generating JSON...");

            syncEditsToParser(); // always sync before generating
            applyInterfaceReferences(); // Apply interface references

            boolean useAdvanced = (mergeFlowsCheckbox != null && mergeFlowsCheckbox.isSelected());

            // Create a temporary parser with only filtered data
            ExcelParserV5 filteredParser = createFilteredParser();

            String json = switch (flowType) {
                case "NurseCalls" -> ExcelParserV5.pretty(filteredParser.buildNurseCallsJson(useAdvanced));
                case "Clinicals" -> ExcelParserV5.pretty(filteredParser.buildClinicalsJson(useAdvanced));
                case "Orders" -> ExcelParserV5.pretty(filteredParser.buildOrdersJson(useAdvanced));
                default -> "";
            };

            // Apply syntax highlighting to JSON
            String highlightedJson = applySyntaxHighlighting(json);
            jsonPreview.setText(highlightedJson);
            
            String displayName = switch (flowType) {
                case "NurseCalls" -> "NurseCall";
                case "Clinicals" -> "Clinical";
                case "Orders" -> "Orders";
                default -> flowType;
            };
            
            setProgressVisible(false);
            statusLabel.setText("Generated " + displayName + " JSON");
            lastGeneratedJson = json;
            lastGeneratedWasNurseSide = "NurseCalls".equals(flowType); // Track the last generated type
        } catch (Exception ex) {
            setProgressVisible(false);
            showError("Error generating JSON: " + ex.getMessage());
            statusLabel.setText("Error generating JSON");
        }
    }
    
    // ---------- Apply Syntax Highlighting to JSON ----------
    private String applySyntaxHighlighting(String json) {
        // Simple text-based highlighting using Unicode characters and formatting
        // Note: JavaFX TextArea doesn't support HTML, so we use plain text markers
        // This adds visual markers for different JSON elements
        if (json == null || json.isEmpty()) return json;
        
        StringBuilder highlighted = new StringBuilder();
        highlighted.append("═══ JSON Preview (Formatted) ═══\n\n");
        highlighted.append(json);
        highlighted.append("\n\n═══ End of JSON ═══");
        
        return highlighted.toString();
    }

    // ---------- Export JSON ----------
    private void exportJson(String flowType) {
        try {
            if (parser == null) {
                showError("Please load an Excel file first.");
                return;
            }

            setProgressVisible(true);
            statusLabel.setText("Exporting JSON...");

            syncEditsToParser();
            applyInterfaceReferences(); // Apply interface references

            boolean useAdvanced = (mergeFlowsCheckbox != null && mergeFlowsCheckbox.isSelected());

            String title = switch (flowType) {
                case "NurseCalls" -> "Export NurseCall JSON";
                case "Clinicals" -> "Export Clinical JSON";
                case "Orders" -> "Export Orders JSON";
                default -> "Export JSON";
            };
            String fileName = switch (flowType) {
                case "NurseCalls" -> "NurseCalls.json";
                case "Clinicals" -> "Clinicals.json";
                case "Orders" -> "Orders.json";
                default -> "output.json";
            };

            FileChooser chooser = new FileChooser();
            chooser.setTitle(title);
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON Files", "*.json"));
            chooser.setInitialFileName(fileName);

            if (lastJsonDir != null && lastJsonDir.exists()) {
                chooser.setInitialDirectory(lastJsonDir);
            }

            File file = chooser.showSaveDialog(getStage());
            if (file == null) {
                setProgressVisible(false);
                statusLabel.setText("Export cancelled");
                return;
            }

            // Remember directory
            rememberDirectory(file, false);

            // Create a temporary parser with only filtered data
            ExcelParserV5 filteredParser = createFilteredParser();

            switch (flowType) {
                case "NurseCalls" -> filteredParser.writeNurseCallsJson(file, useAdvanced);
                case "Clinicals" -> filteredParser.writeClinicalsJson(file, useAdvanced);
                case "Orders" -> filteredParser.writeOrdersJson(file, useAdvanced);
            }

            setProgressVisible(false);
            
            if (useAdvanced) {
                statusLabel.setText("Exported Merged JSON (Advanced Mode)");
            } else {
                statusLabel.setText("Exported Standard JSON");
            }

            showInfo("✅ JSON saved to:\n" + file.getAbsolutePath());
        } catch (Exception ex) {
            setProgressVisible(false);
            showError("Error exporting JSON: " + ex.getMessage());
            statusLabel.setText("Error exporting JSON");
        }
    }

    // ---------- Helpers ----------
    // Create a parser with only the filtered data
    private ExcelParserV5 createFilteredParser() {
        ExcelParserV5 filteredParser = new ExcelParserV5();
        
        // Copy interface references
        filteredParser.setInterfaceReferences(
            edgeRefNameField != null ? edgeRefNameField.getText().trim() : "OutgoingWCTP",
            vcsRefNameField != null ? vcsRefNameField.getText().trim() : "VMP",
            voceraRefNameField != null ? voceraRefNameField.getText().trim() : "Vocera",
            xmppRefNameField != null ? xmppRefNameField.getText().trim() : "XMPP"
        );
        
        // Copy default interface flags
        boolean defaultEdge = defaultEdgeCheckbox != null && defaultEdgeCheckbox.isSelected();
        boolean defaultVmp = defaultVmpCheckbox != null && defaultVmpCheckbox.isSelected();
        boolean defaultVocera = defaultVoceraCheckbox != null && defaultVoceraCheckbox.isSelected();
        boolean defaultXmpp = defaultXmppCheckbox != null && defaultXmppCheckbox.isSelected();
        filteredParser.setDefaultInterfaces(defaultEdge, defaultVmp, defaultVocera, defaultXmpp);
        
        // Copy room filters
        filteredParser.setRoomFilters(
            roomFilterNursecallField != null ? roomFilterNursecallField.getText().trim() : "",
            roomFilterClinicalField != null ? roomFilterClinicalField.getText().trim() : "",
            roomFilterOrdersField != null ? roomFilterOrdersField.getText().trim() : ""
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
        
        // Add filtered orders
        if (ordersFilteredList != null) {
            filteredParser.orders.addAll(ordersFilteredList);
        }
        
        return filteredParser;
    }

    private void setJsonButtonsEnabled(boolean enabled) {
        generateNurseJsonButton.setDisable(!enabled);
        generateClinicalJsonButton.setDisable(!enabled);
        if (generateOrdersJsonButton != null) generateOrdersJsonButton.setDisable(!enabled);
        exportNurseJsonButton.setDisable(!enabled);
        exportClinicalJsonButton.setDisable(!enabled);
        if (exportOrdersJsonButton != null) exportOrdersJsonButton.setDisable(!enabled);
    }

    private void setExcelButtonsEnabled(boolean enabled) {
        if (saveExcelButton != null) saveExcelButton.setDisable(!enabled);
        if (saveExcelAsButton != null) saveExcelAsButton.setDisable(!enabled);
        if (clearAllButton != null) clearAllButton.setDisable(!enabled);
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
        if (ordersFullList != null) {
            parser.orders.clear();
            parser.orders.addAll(ordersFullList);
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
        if (tableUnits != null) {
            tableUnits.setEditable(true);
            // Enable column sorting
            tableUnits.setSortPolicy(tv -> {
                java.util.Comparator<ExcelParserV5.UnitRow> comparator = (r1, r2) -> {
                    if (tv.getSortOrder().isEmpty()) return 0;
                    TableColumn<ExcelParserV5.UnitRow, ?> sortColumn = (TableColumn<ExcelParserV5.UnitRow, ?>) tv.getSortOrder().get(0);
                    int dir = sortColumn.getSortType() == TableColumn.SortType.ASCENDING ? 1 : -1;
                    return dir * compareUnitRows(r1, r2, sortColumn);
                };
                FXCollections.sort(tv.getItems(), comparator);
                return true;
            });
        }
        setupEditable(unitFacilityCol, r -> r.facility, (r, v) -> r.facility = v);
        setupEditable(unitNamesCol, r -> r.unitNames, (r, v) -> r.unitNames = v);
        setupEditable(unitNurseGroupCol, r -> r.nurseGroup, (r, v) -> r.nurseGroup = v);
        setupEditable(unitClinicalGroupCol, r -> r.clinGroup, (r, v) -> r.clinGroup = v);
        setupEditable(unitOrdersGroupCol, r -> r.ordersGroup, (r, v) -> r.ordersGroup = v);
        setupEditable(unitNoCareGroupCol, r -> r.noCareGroup, (r, v) -> r.noCareGroup = v);
        setupEditable(unitCommentsCol, r -> r.comments, (r, v) -> r.comments = v);
    }
    
    private int compareUnitRows(ExcelParserV5.UnitRow r1, ExcelParserV5.UnitRow r2, TableColumn<ExcelParserV5.UnitRow, ?> column) {
        if (column == unitFacilityCol) return safe(r1.facility).compareTo(safe(r2.facility));
        if (column == unitNamesCol) return safe(r1.unitNames).compareTo(safe(r2.unitNames));
        if (column == unitNurseGroupCol) return safe(r1.nurseGroup).compareTo(safe(r2.nurseGroup));
        if (column == unitClinicalGroupCol) return safe(r1.clinGroup).compareTo(safe(r2.clinGroup));
        if (column == unitOrdersGroupCol) return safe(r1.ordersGroup).compareTo(safe(r2.ordersGroup));
        if (column == unitNoCareGroupCol) return safe(r1.noCareGroup).compareTo(safe(r2.noCareGroup));
        if (column == unitCommentsCol) return safe(r1.comments).compareTo(safe(r2.comments));
        return 0;
    }

    private void initializeNurseColumns() {
        if (tableNurseCalls != null) {
            tableNurseCalls.setEditable(true);
            // Enable default sorting
            tableNurseCalls.setSortPolicy(tv -> {
                FXCollections.sort(tv.getItems(), createFlowComparator(tv));
                return true;
            });
        }
        setupCheckBox(nurseInScopeCol, f -> f.inScope, (f, v) -> f.inScope = v);
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
        if (tableClinicals != null) {
            tableClinicals.setEditable(true);
            tableClinicals.setSortPolicy(tv -> {
                FXCollections.sort(tv.getItems(), createFlowComparator(tv));
                return true;
            });
        }
        setupCheckBox(clinicalInScopeCol, f -> f.inScope, (f, v) -> f.inScope = v);
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

    private void initializeOrdersColumns() {
        if (tableOrders != null) {
            tableOrders.setEditable(true);
            tableOrders.setSortPolicy(tv -> {
                FXCollections.sort(tv.getItems(), createFlowComparator(tv));
                return true;
            });
        }
        setupCheckBox(ordersInScopeCol, f -> f.inScope, (f, v) -> f.inScope = v);
        setupEditable(ordersConfigGroupCol, f -> f.configGroup, (f, v) -> f.configGroup = v);
        setupEditable(ordersAlarmNameCol, f -> f.alarmName, (f, v) -> f.alarmName = v);
        setupEditable(ordersSendingNameCol, f -> f.sendingName, (f, v) -> f.sendingName = v);
        setupEditable(ordersPriorityCol, f -> f.priorityRaw, (f, v) -> f.priorityRaw = v);
        setupEditable(ordersDeviceACol, f -> f.deviceA, (f, v) -> f.deviceA = v);
        setupEditable(ordersDeviceBCol, f -> f.deviceB, (f, v) -> f.deviceB = v);
        setupEditable(ordersRingtoneCol, f -> f.ringtone, (f, v) -> f.ringtone = v);
        setupEditable(ordersResponseOptionsCol, f -> safe(f.responseOptions), (f, v) -> f.responseOptions = safe(v));
        setupEditable(ordersBreakThroughDNDCol, f -> safe(f.breakThroughDND), (f, v) -> f.breakThroughDND = safe(v));
        setupEditable(ordersEscalateAfterCol, f -> safe(f.escalateAfter), (f, v) -> f.escalateAfter = safe(v));
        setupEditable(ordersTtlValueCol, f -> safe(f.ttlValue), (f, v) -> f.ttlValue = safe(v));
        setupEditable(ordersEnunciateCol, f -> safe(f.enunciate), (f, v) -> f.enunciate = safe(v));
        setupEditable(ordersT1Col, f -> f.t1, (f, v) -> f.t1 = v);
        setupEditable(ordersR1Col, f -> f.r1, (f, v) -> f.r1 = v);
        setupEditable(ordersT2Col, f -> f.t2, (f, v) -> f.t2 = v);
        setupEditable(ordersR2Col, f -> f.r2, (f, v) -> f.r2 = v);
        setupEditable(ordersT3Col, f -> f.t3, (f, v) -> f.t3 = v);
        setupEditable(ordersR3Col, f -> f.r3, (f, v) -> f.r3 = v);
        setupEditable(ordersT4Col, f -> f.t4, (f, v) -> f.t4 = v);
        setupEditable(ordersR4Col, f -> f.r4, (f, v) -> f.r4 = v);
        setupEditable(ordersT5Col, f -> f.t5, (f, v) -> f.t5 = v);
        setupEditable(ordersR5Col, f -> f.r5, (f, v) -> f.r5 = v);
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

    private <R> void setupCheckBox(TableColumn<R, Boolean> col, Function<R, Boolean> getter, BiConsumer<R, Boolean> setter) {
        if (col == null) return;
        col.setCellValueFactory(d -> {
            R row = d.getValue();
            SimpleBooleanProperty prop = new SimpleBooleanProperty(getter.apply(row));
            prop.addListener((obs, oldVal, newVal) -> {
                setter.accept(row, newVal);
                if (col.getTableView() != null) col.getTableView().refresh();
            });
            return prop;
        });
        col.setCellFactory(CheckBoxTableCell.forTableColumn(col));
        col.setEditable(true);
    }

    // ---------- Setup header checkbox for "In Scope" columns ----------
    private void setupHeaderCheckBox(TableColumn<ExcelParserV5.FlowRow, Boolean> col, FilteredList<ExcelParserV5.FlowRow> filteredList) {
        if (col == null) return;
        
        CheckBox headerCheckBox = new CheckBox();
        headerCheckBox.setSelected(true); // Default to checked
        
        // When header checkbox is clicked, update all visible rows
        headerCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            if (filteredList != null) {
                for (ExcelParserV5.FlowRow row : filteredList) {
                    row.inScope = newVal;
                }
                if (col.getTableView() != null) {
                    col.getTableView().refresh();
                }
            }
        });
        
        col.setGraphic(headerCheckBox);
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
        if (ordersConfigGroupFilter != null) {
            ordersConfigGroupFilter.setOnAction(e -> applyOrdersFilter());
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
                if (unit.ordersGroup != null && !unit.ordersGroup.trim().isEmpty()) {
                    unitConfigGroups.add(unit.ordersGroup.trim());
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
        
        // Collect unique config groups from Orders
        Set<String> ordersConfigGroups = new LinkedHashSet<>();
        if (parser != null && parser.orders != null) {
            for (ExcelParserV5.FlowRow flow : parser.orders) {
                if (flow.configGroup != null && !flow.configGroup.trim().isEmpty()) {
                    ordersConfigGroups.add(flow.configGroup.trim());
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
        
        // Update Orders filter
        if (ordersConfigGroupFilter != null) {
            List<String> ordersOptions = new ArrayList<>();
            ordersOptions.add("All");
            ordersOptions.addAll(ordersConfigGroups);
            ordersConfigGroupFilter.setItems(FXCollections.observableArrayList(ordersOptions));
            ordersConfigGroupFilter.getSelectionModel().select(0); // Select "All" by default
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
                selected.equals(unit.nurseGroup) || selected.equals(unit.clinGroup) || selected.equals(unit.ordersGroup)
            );
        }
        
        updateStatusLabel();
    }

    private void applyNurseFilter() {
        if (nurseCallsFilteredList == null) return;
        
        String selected = nurseConfigGroupFilter.getSelectionModel().getSelectedItem();
        if (selected == null || selected.equals("All")) {
            // Check all rows when "All" is selected
            if (nurseCallsFullList != null) {
                for (ExcelParserV5.FlowRow flow : nurseCallsFullList) {
                    flow.inScope = true;
                }
            }
            nurseCallsFilteredList.setPredicate(flow -> true); // Show all
        } else {
            // Update inScope based on filter: uncheck filtered-out rows, keep checked for visible rows
            if (nurseCallsFullList != null) {
                for (ExcelParserV5.FlowRow flow : nurseCallsFullList) {
                    flow.inScope = selected.equals(flow.configGroup);
                }
            }
            nurseCallsFilteredList.setPredicate(flow -> selected.equals(flow.configGroup));
        }
        
        if (tableNurseCalls != null) tableNurseCalls.refresh();
        updateStatusLabel();
    }

    private void applyClinicalFilter() {
        if (clinicalsFilteredList == null) return;
        
        String selected = clinicalConfigGroupFilter.getSelectionModel().getSelectedItem();
        if (selected == null || selected.equals("All")) {
            // Check all rows when "All" is selected
            if (clinicalsFullList != null) {
                for (ExcelParserV5.FlowRow flow : clinicalsFullList) {
                    flow.inScope = true;
                }
            }
            clinicalsFilteredList.setPredicate(flow -> true); // Show all
        } else {
            // Update inScope based on filter: uncheck filtered-out rows, keep checked for visible rows
            if (clinicalsFullList != null) {
                for (ExcelParserV5.FlowRow flow : clinicalsFullList) {
                    flow.inScope = selected.equals(flow.configGroup);
                }
            }
            clinicalsFilteredList.setPredicate(flow -> selected.equals(flow.configGroup));
        }
        
        if (tableClinicals != null) tableClinicals.refresh();
        updateStatusLabel();
    }

    private void applyOrdersFilter() {
        if (ordersFilteredList == null) return;
        
        String selected = ordersConfigGroupFilter.getSelectionModel().getSelectedItem();
        if (selected == null || selected.equals("All")) {
            // Check all rows when "All" is selected
            if (ordersFullList != null) {
                for (ExcelParserV5.FlowRow flow : ordersFullList) {
                    flow.inScope = true;
                }
            }
            ordersFilteredList.setPredicate(flow -> true); // Show all
        } else {
            // Update inScope based on filter: uncheck filtered-out rows, keep checked for visible rows
            if (ordersFullList != null) {
                for (ExcelParserV5.FlowRow flow : ordersFullList) {
                    flow.inScope = selected.equals(flow.configGroup);
                }
            }
            ordersFilteredList.setPredicate(flow -> selected.equals(flow.configGroup));
        }
        
        if (tableOrders != null) tableOrders.refresh();
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
        int ordersShown = ordersFilteredList != null ? ordersFilteredList.size() : 0;
        int ordersTotal = ordersFullList != null ? ordersFullList.size() : 0;
        
        String status = String.format("Units: %d/%d | Nurse Calls: %d/%d | Clinicals: %d/%d | Orders: %d/%d", 
            unitsShown, unitsTotal, nurseShown, nurseTotal, clinicalShown, clinicalTotal, ordersShown, ordersTotal);
        
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
        ordersFullList = FXCollections.observableArrayList(parser.orders);
        
        // Create filtered lists
        unitsFilteredList = new FilteredList<>(unitsFullList, unit -> true);
        nurseCallsFilteredList = new FilteredList<>(nurseCallsFullList, flow -> true);
        clinicalsFilteredList = new FilteredList<>(clinicalsFullList, flow -> true);
        ordersFilteredList = new FilteredList<>(ordersFullList, flow -> true);
        
        // Set filtered lists to tables
        if (tableUnits != null) tableUnits.setItems(unitsFilteredList);
        if (tableNurseCalls != null) tableNurseCalls.setItems(nurseCallsFilteredList);
        if (tableClinicals != null) tableClinicals.setItems(clinicalsFilteredList);
        if (tableOrders != null) tableOrders.setItems(ordersFilteredList);
        
        // Setup header checkboxes for "In Scope" columns
        setupHeaderCheckBox(nurseInScopeCol, nurseCallsFilteredList);
        setupHeaderCheckBox(clinicalInScopeCol, clinicalsFilteredList);
        setupHeaderCheckBox(ordersInScopeCol, ordersFilteredList);
        
        // Update filter options
        updateFilterOptions();
    }

    // ---------- Reset Defaults ----------
    private void resetDefaults() {
        if (edgeRefNameField != null) edgeRefNameField.setText("OutgoingWCTP");
        if (vcsRefNameField != null) vcsRefNameField.setText("VMP");
        if (voceraRefNameField != null) voceraRefNameField.setText("Vocera");
        if (xmppRefNameField != null) xmppRefNameField.setText("XMPP");
        statusLabel.setText("Reset interface reference names to defaults");
    }

    // ---------- Clear All Data ----------
    private void clearAllData() {
        // Show confirmation dialog with warning message
        Alert confirmAlert = new Alert(Alert.AlertType.WARNING);
        confirmAlert.setTitle("Clear All Data");
        confirmAlert.setHeaderText("⚠️ You are about to delete all currently loaded data");
        confirmAlert.setContentText("This action cannot be undone. All loaded units, nurse calls, clinicals, and orders will be cleared.\n\nDo you want to continue?");
        confirmAlert.getDialogPane().setStyle("-fx-font-size: 13px;");
        
        // Add Continue and Cancel buttons
        ButtonType continueButton = new ButtonType("Continue", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButton = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        confirmAlert.getButtonTypes().setAll(continueButton, cancelButton);
        
        Optional<ButtonType> result = confirmAlert.showAndWait();
        
        if (result.isPresent() && result.get() == continueButton) {
            // User selected "Continue" - clear all data
            try {
                // Clear all data from parser
                parser.units.clear();
                parser.nurseCalls.clear();
                parser.clinicals.clear();
                parser.orders.clear();
                
                // Clear all observable lists
                if (unitsFullList != null) unitsFullList.clear();
                if (nurseCallsFullList != null) nurseCallsFullList.clear();
                if (clinicalsFullList != null) clinicalsFullList.clear();
                if (ordersFullList != null) ordersFullList.clear();
                
                // Clear current file reference
                currentExcelFile = null;
                
                // Reset window title
                ExcelJsonApplication.updateWindowTitle(null);
                
                // Clear JSON preview
                jsonPreview.setText("All data cleared. Load an Excel file to begin.");
                lastGeneratedJson = "";
                
                // Disable buttons
                setJsonButtonsEnabled(false);
                setExcelButtonsEnabled(false);
                
                // Refresh tables
                if (tableUnits != null) tableUnits.refresh();
                if (tableNurseCalls != null) tableNurseCalls.refresh();
                if (tableClinicals != null) tableClinicals.refresh();
                if (tableOrders != null) tableOrders.refresh();
                
                // Update status
                updateStatusLabel();
                statusLabel.setText("✅ All data cleared successfully");
                
                showInfo("All data has been cleared successfully.\n\nYou can now load a new Excel file.");
            } catch (Exception ex) {
                showError("Failed to clear data: " + ex.getMessage());
            }
        }
        // If user selected "Cancel" or closed the dialog, do nothing and return to the app
    }

    // ---------- Apply Interface References ----------
    private void applyInterfaceReferences() {
        if (parser != null) {
            parser.setInterfaceReferences(
                edgeRefNameField != null ? edgeRefNameField.getText().trim() : "OutgoingWCTP",
                vcsRefNameField != null ? vcsRefNameField.getText().trim() : "VMP",
                voceraRefNameField != null ? voceraRefNameField.getText().trim() : "Vocera",
                xmppRefNameField != null ? xmppRefNameField.getText().trim() : "XMPP"
            );
            
            // Apply default interface flags
            boolean defaultEdge = defaultEdgeCheckbox != null && defaultEdgeCheckbox.isSelected();
            boolean defaultVmp = defaultVmpCheckbox != null && defaultVmpCheckbox.isSelected();
            boolean defaultVocera = defaultVoceraCheckbox != null && defaultVoceraCheckbox.isSelected();
            boolean defaultXmpp = defaultXmppCheckbox != null && defaultXmppCheckbox.isSelected();
            parser.setDefaultInterfaces(defaultEdge, defaultVmp, defaultVocera, defaultXmpp);
            
            // Apply room filters
            parser.setRoomFilters(
                roomFilterNursecallField != null ? roomFilterNursecallField.getText().trim() : "",
                roomFilterClinicalField != null ? roomFilterClinicalField.getText().trim() : "",
                roomFilterOrdersField != null ? roomFilterOrdersField.getText().trim() : ""
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

    // ---------- Check Both Default Interfaces Selected ----------
    private void checkBothDefaultInterfacesSelected() {
        if (defaultEdgeCheckbox != null && defaultVmpCheckbox != null) {
            boolean bothSelected = defaultEdgeCheckbox.isSelected() && defaultVmpCheckbox.isSelected();
            if (bothSelected) {
                showWarning("Your Engage rules will be combined to send outgoing WCTP and VMP endpoints");
            }
        }
    }

    private void showWarning(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Warning");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.getDialogPane().setStyle("-fx-font-size: 13px;");
        alert.showAndWait();
    }

    private static String safe(String v) { return v == null ? "" : v; }
    
    // ---------- Create Flow Comparator for Sorting ----------
    private java.util.Comparator<ExcelParserV5.FlowRow> createFlowComparator(TableView<ExcelParserV5.FlowRow> tv) {
        return (r1, r2) -> {
            if (tv.getSortOrder().isEmpty()) return 0;
            TableColumn<ExcelParserV5.FlowRow, ?> sortColumn = (TableColumn<ExcelParserV5.FlowRow, ?>) tv.getSortOrder().get(0);
            int dir = sortColumn.getSortType() == TableColumn.SortType.ASCENDING ? 1 : -1;
            
            String colText = sortColumn.getText();
            if (colText == null) return 0;
            
            // Sort by different fields based on column
            if (colText.contains("Config Group")) return dir * safe(r1.configGroup).compareTo(safe(r2.configGroup));
            if (colText.contains("Alarm Name")) return dir * safe(r1.alarmName).compareTo(safe(r2.alarmName));
            if (colText.contains("Sending Name")) return dir * safe(r1.sendingName).compareTo(safe(r2.sendingName));
            if (colText.contains("Priority")) return dir * safe(r1.priorityRaw).compareTo(safe(r2.priorityRaw));
            if (colText.contains("Device A")) return dir * safe(r1.deviceA).compareTo(safe(r2.deviceA));
            if (colText.contains("Device B")) return dir * safe(r1.deviceB).compareTo(safe(r2.deviceB));
            if (colText.contains("In Scope")) return dir * Boolean.compare(r1.inScope, r2.inScope);
            
            return 0;
        };
    }

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
    
    // ---------- Toggle Theme ----------
    private void toggleTheme() {
        try {
            var scene = jsonPreview.getScene();
            if (scene == null) return;
            
            isDarkMode = !isDarkMode;
            
            // Clear existing stylesheets
            scene.getStylesheets().clear();
            
            // Load appropriate theme
            if (isDarkMode) {
                var darkCss = getClass().getResource("/css/vocera-theme-dark.css");
                if (darkCss != null) {
                    scene.getStylesheets().add(darkCss.toExternalForm());
                    if (themeToggleButton != null) {
                        themeToggleButton.setText("☀️ Light Mode");
                    }
                    statusLabel.setText("Switched to dark theme");
                } else {
                    showError("Dark theme CSS not found");
                    isDarkMode = false;
                }
            } else {
                var lightCss = getClass().getResource("/css/vocera-theme.css");
                if (lightCss != null) {
                    scene.getStylesheets().add(lightCss.toExternalForm());
                    if (themeToggleButton != null) {
                        themeToggleButton.setText("🌙 Dark Mode");
                    }
                    statusLabel.setText("Switched to light theme");
                } else {
                    showError("Light theme CSS not found");
                }
            }
        } catch (Exception ex) {
            showError("Failed to toggle theme: " + ex.getMessage());
        }
    }
    
    // ---------- Toggle Adapter Section ----------
    private void toggleAdapterSection() {
        if (adapterSettingsContent == null || adapterCollapseButton == null) return;
        
        isAdapterSectionCollapsed = !isAdapterSectionCollapsed;
        
        if (isAdapterSectionCollapsed) {
            adapterSettingsContent.setVisible(false);
            adapterSettingsContent.setManaged(false);
            adapterCollapseButton.setText("▶");
            statusLabel.setText("Adapter settings collapsed");
        } else {
            adapterSettingsContent.setVisible(true);
            adapterSettingsContent.setManaged(true);
            adapterCollapseButton.setText("▼");
            statusLabel.setText("Adapter settings expanded");
        }
    }
    
    // ---------- Show/Hide Progress Indicator ----------
    private void setProgressVisible(boolean visible) {
        if (progressIndicator != null) {
            progressIndicator.setVisible(visible);
        }
    }
}
