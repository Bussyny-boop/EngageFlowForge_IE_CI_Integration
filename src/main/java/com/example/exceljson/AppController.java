package com.example.exceljson;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.TranslateTransition;
import javafx.animation.ParallelTransition;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import com.example.exceljson.util.TextAreaTableCell;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.concurrent.Task;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.Slider;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.text.TextFlow;
import javafx.scene.text.Text;
import javafx.scene.paint.Color;
import org.apache.poi.ss.usermodel.*;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javafx.geometry.Side;
import javafx.geometry.Insets;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.prefs.Preferences;
import javax.imageio.ImageIO;
import net.sourceforge.plantuml.SourceStringReader;
import net.sourceforge.plantuml.FileFormat;
import net.sourceforge.plantuml.FileFormatOption;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

public class AppController {

    // ---------- New UI Elements for Redesigned Layout ----------
    @FXML private Label currentFileLabel;
    @FXML private Label jsonModeLabel;
    @FXML private ProgressBar statusProgressBar;
    @FXML private Button settingsButton;
    @FXML private Button helpButton;
    @FXML private Button closeSettingsButton;
    @FXML private VBox settingsDrawer;
    @FXML private StackPane contentStack;
    @FXML private BorderPane unitsView;
    @FXML private BorderPane nurseCallsView;
    @FXML private BorderPane clinicalsView;
    @FXML private BorderPane ordersView;
    @FXML private ToggleButton navUnits;
    @FXML private ToggleButton navNurseCalls;
    @FXML private ToggleButton navClinicals;
    @FXML private ToggleButton navOrders;
    @FXML private ToggleGroup navigationGroup;
    @FXML private BorderPane sidebarContainer;
    @FXML private VBox sidebarContent;
    @FXML private Button sidebarToggleButton;
    @FXML private Label loadDataLabel;
    @FXML private VBox loadButtonsContainer;
    @FXML private Label exportJsonLabel;
    @FXML private VBox exportButtonsContainer;

    // ---------- UI Elements ----------
    @FXML private Button loadNdwButton;
    @FXML private Button loadXmlButton;
    @FXML private Button loadJsonButton;
    @FXML private Button saveExcelButton;
    @FXML private Button saveExcelAsButton;
    @FXML private Button clearAllButton;
    @FXML private Button generateJsonButton;
    @FXML private Button exportNurseJsonButton;
    @FXML private Button exportClinicalJsonButton;
    @FXML private Button exportOrdersJsonButton;
    @FXML private Button visualFlowButton;
    @FXML private Button themeToggleButton;
    @FXML private CheckBox noMergeCheckbox;
    @FXML private CheckBox mergeByConfigGroupCheckbox;  // "Merge by Config Group" checkbox
    @FXML private CheckBox mergeAcrossConfigGroupCheckbox;  // "Merge Across Config Group" checkbox
    @FXML private CheckBox combineConfigGroupCheckbox;  // "Combine Config Group" toggle
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
    @FXML private Spinner<Integer> loadedTimeoutSpinner; // legacy support if present
    @FXML private Slider loadedTimeoutSlider;
    @FXML private Label loadedTimeoutValueLabel;
    @FXML private TextField loadedTimeoutMinField;
    @FXML private TextField loadedTimeoutMaxField;
    @FXML private TextField roomFilterNursecallField;
    @FXML private TextField roomFilterClinicalField;
    @FXML private TextField roomFilterOrdersField;
    @FXML private TextArea jsonPreview;
    @FXML private Label statusLabel;
    
    // ---------- Voice Group Validation ----------
    @FXML private Button loadVoiceGroupButton;
    @FXML private Button clearVoiceGroupButton;
    @FXML private Label voiceGroupStatsLabel;
    
    private final Set<String> loadedVoiceGroups = new HashSet<>();
    private ContextMenu suggestionPopup;
    // Lightweight pattern for quick keyword detection (full parsing done by VoiceGroupValidator)
    private static final Pattern VGROUP_KEYWORD_PATTERN = Pattern.compile("(?i)(?:VGroup|Group):");

    // ---------- Custom Tab Mappings ----------
    @FXML private TextField customTabNameField;
    @FXML private ComboBox<String> customTabFlowTypeCombo;
    @FXML private Button addCustomTabButton;
    @FXML private ListView<String> customTabMappingsList;
    @FXML private Label customTabStatsLabel;

    // ---------- Config Group Filters ----------
    @FXML private ComboBox<String> unitConfigGroupFilter;
    @FXML private ComboBox<String> nurseConfigGroupFilter;
    @FXML private ComboBox<String> clinicalConfigGroupFilter;
    @FXML private ComboBox<String> ordersConfigGroupFilter;
    @FXML private TextField nurseAlarmNameFilter;
    @FXML private TextField clinicalAlarmNameFilter;
    @FXML private TextField ordersAlarmNameFilter;

    // ---------- Units ----------
    @FXML private TableView<ExcelParserV5.UnitRow> tableUnits;
    @FXML private TableColumn<ExcelParserV5.UnitRow, String> unitFacilityCol;
    @FXML private TableColumn<ExcelParserV5.UnitRow, String> unitNamesCol;
    @FXML private TableColumn<ExcelParserV5.UnitRow, String> unitPodRoomFilterCol;
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
    
    // ---------- Custom Tab Mappings Data ----------
    private final Map<String, String> customTabMappings = new LinkedHashMap<>();
    private final ObservableList<String> customTabMappingsDisplay = FXCollections.observableArrayList();
    
    // Map of custom tab name to its dynamic TableColumn in the Units table
    private final Map<String, TableColumn<ExcelParserV5.UnitRow, String>> customUnitColumns = new LinkedHashMap<>();
    
    // ---------- Button Text Storage for Animation/Progress ----------
    private final Map<Button, String> originalButtonTexts = new HashMap<>();
    private final Map<ToggleButton, String> originalToggleTexts = new HashMap<>();

    // ---------- Directory Persistence ----------
    private File lastExcelDir = null;
    private File lastJsonDir = null;

    private static final String APP_VERSION = "3.0.0";
    private static final String PREF_KEY_LAST_EXCEL_DIR = "lastExcelDir";
    private static final String PREF_KEY_LAST_JSON_DIR = "lastJsonDir";
    private static final String PREF_KEY_DARK_MODE = "darkMode";
    private static final String PREF_KEY_SIDEBAR_COLLAPSED = "sidebarCollapsed";
    private static final String PREF_KEY_CUSTOM_TAB_MAPPINGS = "customTabMappings";
    private static final String PREF_KEY_LOADED_TIMEOUT_SECONDS = "loadedTimeoutSeconds";
    private static final String PREF_KEY_LOADED_TIMEOUT_MIN = "loadedTimeoutMin";
    private static final String PREF_KEY_LOADED_TIMEOUT_MAX = "loadedTimeoutMax";
    private static final String PREF_KEY_COMBINE_CONFIG_GROUP = "combineConfigGroup";
    
    private boolean isDarkMode = false;
    private boolean isSidebarCollapsed = false;
    private boolean isTopBarCollapsed = false; // Track if top bar buttons show icons only
    private double loadedTimeoutSeconds = 600.0; // Default to persistent (10 minutes = never auto-clear)
    private double loadedTimeoutMin = 3.0;
    private double loadedTimeoutMax = 600.0; // 600 = 10 minutes, acts as "persistent"
    
    // Window width threshold for collapsing top bar buttons
    private static final double TOP_BAR_COLLAPSE_WIDTH = 900.0;
    
    // ---------- Combine Config Group Backup Storage ----------
    private List<ExcelParserV5.FlowRow> originalNurseCalls = null;
    private List<ExcelParserV5.FlowRow> originalClinicals = null;
    private List<ExcelParserV5.FlowRow> originalOrders = null;

    // ---------- Initialization ----------
    @FXML
    public void initialize() {
        parser = new ExcelParserV5();

        // Load saved directories from preferences
        Preferences prefs = Preferences.userNodeForPackage(AppController.class);
        String excelDirPath = prefs.get(PREF_KEY_LAST_EXCEL_DIR, null);
        String jsonDirPath = prefs.get(PREF_KEY_LAST_JSON_DIR, null);
        isDarkMode = prefs.getBoolean(PREF_KEY_DARK_MODE, false);
        isSidebarCollapsed = prefs.getBoolean(PREF_KEY_SIDEBAR_COLLAPSED, false);
        boolean combineConfigGroup = prefs.getBoolean(PREF_KEY_COMBINE_CONFIG_GROUP, false);
        // Load min/max and current timeout, with validation
        loadedTimeoutMin = clamp(safeParseDouble(prefs.get(PREF_KEY_LOADED_TIMEOUT_MIN, "3"), 3.0), 1.0, 600.0);
        loadedTimeoutMax = clamp(safeParseDouble(prefs.get(PREF_KEY_LOADED_TIMEOUT_MAX, "600"), 600.0), 2.0, 600.0);
        if (loadedTimeoutMax <= loadedTimeoutMin) {
            loadedTimeoutMax = Math.min(loadedTimeoutMin + 1, 600.0);
        }
        loadedTimeoutSeconds = clamp(safeParseDouble(prefs.get(PREF_KEY_LOADED_TIMEOUT_SECONDS, "600"), 600.0), loadedTimeoutMin, loadedTimeoutMax);

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

        if (loadNdwButton != null) loadNdwButton.setOnAction(e -> loadNdw());
        if (loadXmlButton != null) loadXmlButton.setOnAction(e -> loadXml());
        if (loadJsonButton != null) loadJsonButton.setOnAction(e -> loadJson());
        if (saveExcelButton != null) saveExcelButton.setOnAction(e -> saveExcel());
        if (saveExcelAsButton != null) saveExcelAsButton.setOnAction(e -> saveExcelAs());
        if (clearAllButton != null) clearAllButton.setOnAction(e -> clearAllData());
        generateJsonButton.setOnAction(e -> generateCombinedJson());
        exportNurseJsonButton.setOnAction(e -> exportJson("NurseCalls"));
        exportClinicalJsonButton.setOnAction(e -> exportJson("Clinicals"));
        if (exportOrdersJsonButton != null) exportOrdersJsonButton.setOnAction(e -> exportJson("Orders"));
        if (visualFlowButton != null) visualFlowButton.setOnAction(e -> generateVisualFlow());
        if (resetDefaultsButton != null) resetDefaultsButton.setOnAction(e -> resetDefaults());
        if (resetPathsButton != null) resetPathsButton.setOnAction(e -> resetPaths());
        
        // ---------- Voice Group Validation ----------
        if (loadVoiceGroupButton != null) loadVoiceGroupButton.setOnAction(e -> loadVoiceGroups());
        if (clearVoiceGroupButton != null) clearVoiceGroupButton.setOnAction(e -> clearVoiceGroups());
        if (themeToggleButton != null) {
            themeToggleButton.setOnAction(e -> toggleTheme());
            updateThemeButton();
        }
        
        // Setup sidebar toggle
        if (sidebarToggleButton != null) {
            sidebarToggleButton.setOnAction(e -> toggleSidebar());
            storeOriginalButtonTexts(); // Store original texts before applying state
            applySidebarState(); // Apply saved state
        }

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
        
        // --- Navigation and Settings Drawer setup ---
        setupNavigation();
        setupSettingsDrawer();
        setupCustomTabMappings();
        setupLoadedTimeoutControl();
        setupTopBarResponsive(); // Setup responsive top bar buttons
        
        // Set combine config group checkbox state from preferences
        if (combineConfigGroupCheckbox != null) {
            combineConfigGroupCheckbox.setSelected(prefs.getBoolean(PREF_KEY_COMBINE_CONFIG_GROUP, false));
        }
        
        // --- Merge Flows checkbox mutual exclusion logic (three-way) ---
        if (noMergeCheckbox != null && mergeByConfigGroupCheckbox != null && mergeAcrossConfigGroupCheckbox != null) {
            // When noMergeCheckbox is selected, deselect the other two
            noMergeCheckbox.selectedProperty().addListener((obs, oldV, newV) -> {
                if (newV) {
                    mergeByConfigGroupCheckbox.setSelected(false);
                    mergeAcrossConfigGroupCheckbox.setSelected(false);
                }
                updateJsonModeLabel();
            });
            
            // When mergeByConfigGroupCheckbox is selected, deselect the other two
            mergeByConfigGroupCheckbox.selectedProperty().addListener((obs, oldV, newV) -> {
                if (newV) {
                    noMergeCheckbox.setSelected(false);
                    mergeAcrossConfigGroupCheckbox.setSelected(false);
                }
                updateJsonModeLabel();
            });
            
            // When mergeAcrossConfigGroupCheckbox is selected, deselect the other two
            mergeAcrossConfigGroupCheckbox.selectedProperty().addListener((obs, oldV, newV) -> {
                if (newV) {
                    noMergeCheckbox.setSelected(false);
                    mergeByConfigGroupCheckbox.setSelected(false);
                }
                updateJsonModeLabel();
            });
        } else if (mergeByConfigGroupCheckbox != null && mergeAcrossConfigGroupCheckbox != null) {
            // Fallback for backward compatibility if noMergeCheckbox doesn't exist
            mergeByConfigGroupCheckbox.selectedProperty().addListener((obs, oldV, newV) -> {
                if (newV && mergeAcrossConfigGroupCheckbox.isSelected()) {
                    mergeAcrossConfigGroupCheckbox.setSelected(false);
                }
                updateJsonModeLabel();
            });
            
            mergeAcrossConfigGroupCheckbox.selectedProperty().addListener((obs, oldV, newV) -> {
                if (newV && mergeByConfigGroupCheckbox.isSelected()) {
                    mergeByConfigGroupCheckbox.setSelected(false);
                }
                updateJsonModeLabel();
            });
        } else if (mergeByConfigGroupCheckbox != null) {
            // Fallback for backward compatibility if only one checkbox exists
            mergeByConfigGroupCheckbox.selectedProperty().addListener((obs, oldV, newV) -> {
                updateJsonModeLabel();
            });
        }
        
        // Initialize labels
        updateJsonModeLabel();
        updateCurrentFileLabel();
        
        // --- Combine Config Group toggle logic ---
        if (combineConfigGroupCheckbox != null) {
            combineConfigGroupCheckbox.selectedProperty().addListener((obs, oldV, newV) -> {
                Preferences preferences = Preferences.userNodeForPackage(AppController.class);
                preferences.putBoolean(PREF_KEY_COMBINE_CONFIG_GROUP, newV);
                
                if (newV) {
                    // Toggle on - combine rows with identical columns but different config groups
                    combineConfigGroupRows();
                } else {
                    // Toggle off - revert to original data
                    revertCombinedConfigGroupRows();
                }
            });
            
            // Apply initial state if enabled
            if (combineConfigGroupCheckbox.isSelected()) {
                combineConfigGroupRows();
            }
        }
    }

    // ---------- Loaded Timeout Control Setup ----------
    private void setupLoadedTimeoutControl() {
        // Preferred: slider (current UI)
        if (loadedTimeoutSlider != null) {
            loadedTimeoutSlider.setMin(loadedTimeoutMin);
            loadedTimeoutSlider.setMax(loadedTimeoutMax);
            loadedTimeoutSlider.setBlockIncrement(1);
            loadedTimeoutSlider.setValue(clamp(loadedTimeoutSeconds, loadedTimeoutMin, loadedTimeoutMax));
            updateLoadedTimeoutLabel(loadedTimeoutSlider.getValue());
            loadedTimeoutSlider.valueProperty().addListener((obs, oldV, newV) -> {
                if (newV == null) return;
                double v = newV.doubleValue();
                updateLoadedTimeoutLabel(v);
                int iv = (int) Math.round(v);
                loadedTimeoutSeconds = iv;
                Preferences prefs = Preferences.userNodeForPackage(AppController.class);
                prefs.put(PREF_KEY_LOADED_TIMEOUT_SECONDS, String.valueOf(iv));
            });
        }

        // Min/Max fields
        if (loadedTimeoutMinField != null) {
            loadedTimeoutMinField.setText(String.valueOf((int) Math.round(loadedTimeoutMin)));
            loadedTimeoutMinField.focusedProperty().addListener((obs, was, isNow) -> {
                if (!isNow) applyMinMaxFromFields();
            });
            loadedTimeoutMinField.setOnAction(e -> applyMinMaxFromFields());
        }
        if (loadedTimeoutMaxField != null) {
            loadedTimeoutMaxField.setText(String.valueOf((int) Math.round(loadedTimeoutMax)));
            loadedTimeoutMaxField.focusedProperty().addListener((obs, was, isNow) -> {
                if (!isNow) applyMinMaxFromFields();
            });
            loadedTimeoutMaxField.setOnAction(e -> applyMinMaxFromFields());
        }

        // Backward compatibility: spinner (if present in older layouts)
        if (loadedTimeoutSpinner != null) {
            int initial = (int) Math.round(loadedTimeoutSeconds);
            SpinnerValueFactory.IntegerSpinnerValueFactory vf =
                new SpinnerValueFactory.IntegerSpinnerValueFactory(3, 120, initial, 1);
            loadedTimeoutSpinner.setValueFactory(vf);
            loadedTimeoutSpinner.setEditable(true);
            loadedTimeoutSpinner.valueProperty().addListener((obs, oldV, newV) -> {
                if (newV == null) return;
                loadedTimeoutSeconds = newV.doubleValue();
                Preferences prefs = Preferences.userNodeForPackage(AppController.class);
                prefs.put(PREF_KEY_LOADED_TIMEOUT_SECONDS, String.valueOf(newV));
            });
            var editor = loadedTimeoutSpinner.getEditor();
            if (editor != null) {
                editor.focusedProperty().addListener((obs, was, isNow) -> {
                    if (!isNow) {
                        try {
                            int v = Integer.parseInt(editor.getText().trim());
                            v = Math.max(3, Math.min(120, v));
                            loadedTimeoutSpinner.getValueFactory().setValue(v);
                        } catch (Exception ignored) {}
                    }
                });
            }
        }
    }

    private void updateLoadedTimeoutLabel(double value) {
        if (loadedTimeoutValueLabel != null) {
            int iv = (int) Math.round(value);
            if (iv >= 600) {
                loadedTimeoutValueLabel.setText(iv + "s (persistent)");
            } else {
                loadedTimeoutValueLabel.setText(iv + "s");
            }
        }
    }

    private void applyMinMaxFromFields() {
        try {
            int min = loadedTimeoutMinField != null ? Integer.parseInt(loadedTimeoutMinField.getText().trim()) : (int) loadedTimeoutMin;
            int max = loadedTimeoutMaxField != null ? Integer.parseInt(loadedTimeoutMaxField.getText().trim()) : (int) loadedTimeoutMax;
            min = (int) clamp(min, 1, 600);
            max = (int) clamp(max, 2, 600);
            if (max <= min) max = Math.min(min + 1, 600);
            loadedTimeoutMin = min;
            loadedTimeoutMax = max;

            Preferences prefs = Preferences.userNodeForPackage(AppController.class);
            prefs.put(PREF_KEY_LOADED_TIMEOUT_MIN, String.valueOf(min));
            prefs.put(PREF_KEY_LOADED_TIMEOUT_MAX, String.valueOf(max));

            if (loadedTimeoutSlider != null) {
                loadedTimeoutSlider.setMin(min);
                loadedTimeoutSlider.setMax(max);
                double clamped = clamp(loadedTimeoutSeconds, min, max);
                loadedTimeoutSlider.setValue(clamped);
                loadedTimeoutSeconds = clamped;
                prefs.put(PREF_KEY_LOADED_TIMEOUT_SECONDS, String.valueOf((int) Math.round(clamped)));
                updateLoadedTimeoutLabel(clamped);
            }
        } catch (Exception ignored) { }
    }

    private static double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }

    private static double safeParseDouble(String s, double fallback) {
        if (s == null) return fallback;
        try {
            return Double.parseDouble(s.trim());
        } catch (Exception ex) {
            return fallback;
        }
    }
    
    // ---------- Top Bar Responsive Setup ----------
    /**
     * Sets up responsive behavior for top bar buttons.
     * When window width is below threshold, buttons show icons only.
     */
    private void setupTopBarResponsive() {
        // Note: Original button texts are stored in storeOriginalButtonTexts() 
        // which is called during sidebar setup
        
        // Setup window width listener when scene becomes available
        if (contentStack != null) {
            contentStack.sceneProperty().addListener((obs, oldScene, newScene) -> {
                if (newScene != null && newScene.getWindow() != null) {
                    setupWindowWidthListener(newScene.getWindow());
                }
            });
            
            // If scene already exists, setup immediately
            if (contentStack.getScene() != null && contentStack.getScene().getWindow() != null) {
                setupWindowWidthListener(contentStack.getScene().getWindow());
            }
        }
    }
    
    /**
     * Sets up listener for window width changes to toggle top bar button display
     */
    private void setupWindowWidthListener(javafx.stage.Window window) {
        window.widthProperty().addListener((obs, oldWidth, newWidth) -> {
            if (newWidth != null) {
                updateTopBarButtonDisplay(newWidth.doubleValue());
            }
        });
        
        // Apply initial state
        updateTopBarButtonDisplay(window.getWidth());
    }
    
    /**
     * Updates top bar button display based on window width
     */
    private void updateTopBarButtonDisplay(double windowWidth) {
        boolean shouldCollapse = windowWidth < TOP_BAR_COLLAPSE_WIDTH;
        
        if (shouldCollapse && !isTopBarCollapsed) {
            collapseTopBarButtons();
            isTopBarCollapsed = true;
        } else if (!shouldCollapse && isTopBarCollapsed) {
            expandTopBarButtons();
            isTopBarCollapsed = false;
        }
    }
    
    /**
     * Collapse top bar buttons to show icons only
     */
    private void collapseTopBarButtons() {
        setCollapsedButton(settingsButton, "/icons/settings.png", "Settings");
        setCollapsedButton(saveExcelButton, "/icons/save.png", "Save");
        setCollapsedButton(themeToggleButton, "/icons/darkmode.png", isDarkMode ? "Light Mode" : "Dark Mode");
        setCollapsedButton(helpButton, "/icons/help.png", "Help");
    }
    
    /**
     * Expand top bar buttons to show full text
     */
    private void expandTopBarButtons() {
        restoreButtonText(settingsButton);
        restoreButtonText(saveExcelButton);
        restoreButtonText(themeToggleButton);
        restoreButtonText(helpButton);
    }
    
    // ---------- Navigation Setup ----------
    private void setupNavigation() {
        if (navUnits != null) {
            navUnits.setOnAction(e -> showView(unitsView));
        }
        if (navNurseCalls != null) {
            navNurseCalls.setOnAction(e -> showView(nurseCallsView));
        }
        if (navClinicals != null) {
            navClinicals.setOnAction(e -> showView(clinicalsView));
        }
        if (navOrders != null) {
            navOrders.setOnAction(e -> showView(ordersView));
        }
        
        // Default to Units view
        showView(unitsView);
    }
    
    // ---------- Show View ----------
    private void showView(BorderPane viewToShow) {
        if (unitsView != null) unitsView.setVisible(viewToShow == unitsView);
        if (nurseCallsView != null) nurseCallsView.setVisible(viewToShow == nurseCallsView);
        if (clinicalsView != null) clinicalsView.setVisible(viewToShow == clinicalsView);
        if (ordersView != null) ordersView.setVisible(viewToShow == ordersView);
        
        // Apply enhanced fade and slide transition
        if (viewToShow != null) {
            // Fade in animation
            FadeTransition fade = new FadeTransition(Duration.millis(300), viewToShow);
            fade.setFromValue(0.0);
            fade.setToValue(1.0);
            
            // Slide in from the right animation
            TranslateTransition slide = new TranslateTransition(Duration.millis(300), viewToShow);
            slide.setFromX(30);
            slide.setToX(0);
            
            // Combine both animations
            ParallelTransition transition = new ParallelTransition(fade, slide);
            transition.play();
        }
    }
    
    // ---------- Settings Drawer Setup ----------
    private void setupSettingsDrawer() {
        if (settingsButton != null) {
            settingsButton.setOnAction(e -> toggleSettingsDrawer());
        }
        if (closeSettingsButton != null) {
            closeSettingsButton.setOnAction(e -> toggleSettingsDrawer());
        }
        if (helpButton != null) {
            helpButton.setOnAction(e -> showHelp());
        }
        
        // Add click event filter to auto-close settings drawer when clicking anywhere in the app
        // We'll attach this to the scene once it's available
        if (contentStack != null && settingsDrawer != null) {
            // Setup auto-close when scene becomes available
            contentStack.sceneProperty().addListener((obs, oldScene, newScene) -> {
                if (newScene != null) {
                    setupSettingsAutoClose(newScene);
                    // Limit settings drawer height to 75% of the window height to ensure bottom buttons are reachable
                    settingsDrawer.maxHeightProperty().bind(newScene.heightProperty().multiply(0.75));
                }
            });
            
            // If scene already exists, setup immediately
            if (contentStack.getScene() != null) {
                setupSettingsAutoClose(contentStack.getScene());
                // Limit settings drawer height to 75% of the window height to ensure bottom buttons are reachable
                settingsDrawer.maxHeightProperty().bind(contentStack.getScene().heightProperty().multiply(0.75));
            }
        }
    }
    
    /**
     * Sets up auto-close functionality for the settings drawer.
     * When user clicks anywhere outside the settings drawer, it automatically closes.
     */
    private void setupSettingsAutoClose(javafx.scene.Scene scene) {
        scene.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_CLICKED, event -> {
            // Only process if settings drawer is visible
            if (settingsDrawer != null && settingsDrawer.isVisible()) {
                // Check if the click was outside the settings drawer
                Node target = event.getPickResult().getIntersectedNode();
                
                // Walk up the scene graph to see if the target is inside the settings drawer or settings button
                boolean clickedInsideDrawer = false;
                boolean clickedSettingsButton = false;
                Node current = target;
                
                while (current != null) {
                    if (current == settingsDrawer) {
                        clickedInsideDrawer = true;
                        break;
                    }
                    if (current == settingsButton) {
                        clickedSettingsButton = true;
                        break;
                    }
                    current = current.getParent();
                }
                
                // Close drawer if clicked outside and not on settings button (button has its own toggle)
                if (!clickedInsideDrawer && !clickedSettingsButton) {
                    toggleSettingsDrawer();
                }
            }
        });
    }
    
    // ---------- Toggle Settings Drawer ----------
    private void toggleSettingsDrawer() {
        if (settingsDrawer != null) {
            boolean isVisible = settingsDrawer.isVisible();
            
            if (!isVisible) {
                // Show with animation
                settingsDrawer.setVisible(true);
                settingsDrawer.setManaged(true);
                
                // Slide down animation
                TranslateTransition slide = new TranslateTransition(Duration.millis(300), settingsDrawer);
                slide.setFromY(-settingsDrawer.getHeight());
                slide.setToY(0);
                
                // Fade in animation
                FadeTransition fade = new FadeTransition(Duration.millis(300), settingsDrawer);
                fade.setFromValue(0.0);
                fade.setToValue(1.0);
                
                ParallelTransition transition = new ParallelTransition(slide, fade);
                transition.play();
            } else {
                // Hide with animation
                TranslateTransition slide = new TranslateTransition(Duration.millis(250), settingsDrawer);
                slide.setFromY(0);
                slide.setToY(-50);
                
                FadeTransition fade = new FadeTransition(Duration.millis(250), settingsDrawer);
                fade.setFromValue(1.0);
                fade.setToValue(0.0);
                
                ParallelTransition transition = new ParallelTransition(slide, fade);
                transition.setOnFinished(e -> {
                    settingsDrawer.setVisible(false);
                    settingsDrawer.setManaged(false);
                });
                transition.play();
            }
        }
    }
    
    // ---------- Show Help ----------
    private void showHelp() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Engage FlowForge 2.0 - Help & Guide");
        alert.setHeaderText("📖 Engage FlowForge 2.0 - User Guide");
        
        String helpContent = 
                "═══════════════════════════════════════════════════════════\n" +
                "OVERVIEW\n" +
                "═══════════════════════════════════════════════════════════\n" +
                "Engage FlowForge converts Vocera Engage Excel configuration\n" +
                "sheets into JSON rule files for nurse call and patient\n" +
                "monitoring systems.\n\n" +
                
                "═══════════════════════════════════════════════════════════\n" +
                "GETTING STARTED\n" +
                "═══════════════════════════════════════════════════════════\n" +
                "1️⃣  Load Data: Click '📄 Load NDW' to import Excel workbook\n" +
                "   • Also supports XML ('📋 Load Engage XML') and\n" +
                "     JSON ('📥 Load Engage Rules') imports\n\n" +
                
                "2️⃣  Navigate Tabs:\n" +
                "   📊 Units - View facility, unit, and config group mappings\n" +
                "   🔔 Nurse Calls - Configure nurse call alarms\n" +
                "   🏥 Clinicals - Configure clinical/patient monitoring alarms\n" +
                "   💊 Orders - Configure order-based workflows\n\n" +
                
                "3️⃣  Edit Data: Double-click any cell to edit configuration\n\n" +
                
                "4️⃣  Generate JSON:\n" +
                "   • Click '👁️ Preview JSON' to view generated rules\n" +
                "   • Click export buttons to save:\n" +
                "     🩺 Export Nursecall\n" +
                "     🧬 Export Clinicals\n" +
                "     📦 Export Orders\n\n" +
                
                "═══════════════════════════════════════════════════════════\n" +
                "KEY FEATURES\n" +
                "═══════════════════════════════════════════════════════════\n" +
                "📂 Multi-Format Import: Excel (XLSX), XML, and JSON\n" +
                "✏️  Inline Editing: Double-click cells to edit directly\n" +
                "🔍 Filtering: Filter by configuration group in each tab\n" +
                "🔀 Visual CallFlow: Generate PlantUML flow diagrams\n" +
                "🌓 Dark/Light Themes: Click ⚙️ Settings to toggle themes\n" +
                "💾 Auto-Save: Changes persist across sessions\n" +
                "🗑️  Clear All: Reset all data to start fresh\n\n" +
                
                "═══════════════════════════════════════════════════════════\n" +
                "SIDEBAR NAVIGATION\n" +
                "═══════════════════════════════════════════════════════════\n" +
                "• Click ◀/▶ at the top to collapse/expand the sidebar\n" +
                "• When collapsed, icons show:\n" +
                "  📊 Units  |  🔔 Nurse Calls  |  🏥 Clinicals  |  💊 Orders\n" +
                "• Hover over icons for tooltips when collapsed\n\n" +
                
                "═══════════════════════════════════════════════════════════\n" +
                "ADVANCED OPTIONS (⚙️ Settings)\n" +
                "═══════════════════════════════════════════════════════════\n" +
                "Merge Modes:\n" +
                "• No Merge: Each alarm generates separate JSON rules\n" +
                "• Merge by Config Group: Combines flows within same config\n" +
                "• Merge Across Config Group: Combines flows across all configs\n\n" +
                
                "Adapter References:\n" +
                "• Edge Ref Name: OutgoingWCTP interface (default: OutgoingWCTP)\n" +
                "• VCS Ref Name: VMP interface (default: VMP)\n" +
                "• Vocera Ref Name: Custom Vocera reference\n" +
                "• XMPP Ref Name: XMPP messaging interface\n\n" +
                
                "Custom Tab Mappings:\n" +
                "• Map additional Excel sheets to flow types\n" +
                "• Dynamically add custom columns to Units table\n\n" +
                
                "═══════════════════════════════════════════════════════════\n" +
                "KEYBOARD SHORTCUTS\n" +
                "═══════════════════════════════════════════════════════════\n" +
                "• Double-click: Edit cell\n" +
                "• Enter: Confirm edit\n" +
                "• Esc: Cancel edit\n" +
                "• Tab: Navigate between cells\n\n" +
                
                "═══════════════════════════════════════════════════════════\n" +
                "TIPS & BEST PRACTICES\n" +
                "═══════════════════════════════════════════════════════════\n" +
                "✓ Use Config Group filters to focus on specific units\n" +
                "✓ Preview JSON before exporting to verify rules\n" +
                "✓ Save Excel files periodically to preserve changes\n" +
                "✓ Use Clear All to reset if data becomes corrupted\n" +
                "✓ Check EMDAN Compliant for clinical alarm reclassification\n\n" +
                
                "═══════════════════════════════════════════════════════════\n" +
                "TROUBLESHOOTING\n" +
                "═══════════════════════════════════════════════════════════\n" +
                "❓ Can't load file? Ensure it's a valid XLSX/XML/JSON format\n" +
                "❓ Missing data? Check if correct sheets exist in workbook\n" +
                "❓ Export fails? Verify all required fields are populated\n" +
                "❓ Need help? Check USER_GUIDE.md in installation folder\n\n" +
                
                "═══════════════════════════════════════════════════════════\n" +
                "Version: " + APP_VERSION + " | Java-based application with JavaFX GUI\n" +
                "For detailed documentation, see USER_GUIDE.md\n" +
                "═══════════════════════════════════════════════════════════";
        
        alert.setContentText(helpContent);
        alert.getDialogPane().setStyle("-fx-font-size: 12px; -fx-font-family: 'Consolas', 'Monaco', monospace;");
        alert.getDialogPane().setPrefSize(750, 700);
        alert.setResizable(true);
        alert.showAndWait();
    }
    
    // ---------- Custom Tab Mappings Setup ----------
    private void setupCustomTabMappings() {
        // Initialize ComboBox with flow types
        if (customTabFlowTypeCombo != null) {
            customTabFlowTypeCombo.setItems(FXCollections.observableArrayList(
                "NurseCalls", "Clinicals", "Orders"
            ));
            customTabFlowTypeCombo.getSelectionModel().selectFirst();
        }
        
        // Bind ListView to display list
        if (customTabMappingsList != null) {
            customTabMappingsList.setItems(customTabMappingsDisplay);
            
            // Allow double-click to remove mapping
            customTabMappingsList.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2) {
                    String selected = customTabMappingsList.getSelectionModel().getSelectedItem();
                    if (selected != null) {
                        removeCustomTabMapping(selected);
                    }
                }
            });
        }
        
        // Add button action
        if (addCustomTabButton != null) {
            addCustomTabButton.setOnAction(e -> addCustomTabMapping());
        }
        
        // Load saved mappings from preferences
        loadCustomTabMappings();
    }
    
    // ---------- Add Custom Tab Mapping ----------
    private void addCustomTabMapping() {
        if (customTabNameField == null || customTabFlowTypeCombo == null) {
            return;
        }
        
        String tabName = customTabNameField.getText();
        String flowType = customTabFlowTypeCombo.getValue();
        
        if (tabName == null || tabName.trim().isEmpty()) {
            if (statusLabel != null) {
                statusLabel.setText("⚠️ Please enter a custom tab name");
            }
            return;
        }
        
        if (flowType == null) {
            if (statusLabel != null) {
                statusLabel.setText("⚠️ Please select a flow type");
            }
            return;
        }
        
        tabName = tabName.trim();
        
        // Check if mapping already exists
        if (customTabMappings.containsKey(tabName)) {
            if (statusLabel != null) {
                statusLabel.setText("⚠️ Mapping for '" + tabName + "' already exists");
            }
            return;
        }
        
        // Add the mapping
        customTabMappings.put(tabName, flowType);
        updateCustomTabMappingsDisplay();
        saveCustomTabMappings();
        
        // Create the dynamic column in Units table
        addCustomUnitColumn(tabName);
        
        // Update parser
        parser.setCustomTabMappings(customTabMappings);
        
        // Clear input fields
        customTabNameField.clear();
        customTabFlowTypeCombo.getSelectionModel().selectFirst();
        
        if (statusLabel != null) {
            statusLabel.setText("✅ Added custom tab mapping: " + tabName + " → " + flowType);
        }
    }
    
    // ---------- Remove Custom Tab Mapping ----------
    private void removeCustomTabMapping(String displayText) {
        // Parse the display text to get the tab name
        // Format is "TabName → FlowType"
        String[] parts = displayText.split(" → ");
        if (parts.length < 1) return;
        
        String tabName = parts[0].trim();
        customTabMappings.remove(tabName);
        updateCustomTabMappingsDisplay();
        saveCustomTabMappings();
        
        // Remove the dynamic column from Units table
        removeCustomUnitColumn(tabName);
        
        // Update parser
        parser.setCustomTabMappings(customTabMappings);
        
        if (statusLabel != null) {
            statusLabel.setText("✅ Removed custom tab mapping: " + tabName);
        }
    }
    
    // ---------- Update Custom Tab Mappings Display ----------
    private void updateCustomTabMappingsDisplay() {
        customTabMappingsDisplay.clear();
        for (Map.Entry<String, String> entry : customTabMappings.entrySet()) {
            customTabMappingsDisplay.add(entry.getKey() + " → " + entry.getValue());
        }
    }
    
    // ---------- Save Custom Tab Mappings ----------
    private void saveCustomTabMappings() {
        Preferences prefs = Preferences.userNodeForPackage(AppController.class);
        
        // Serialize mappings as "tabName1:flowType1;tabName2:flowType2;..."
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : customTabMappings.entrySet()) {
            if (sb.length() > 0) sb.append(";");
            sb.append(entry.getKey()).append(":").append(entry.getValue());
        }
        
        prefs.put(PREF_KEY_CUSTOM_TAB_MAPPINGS, sb.toString());
    }
    
    // ---------- Load Custom Tab Mappings ----------
    private void loadCustomTabMappings() {
        Preferences prefs = Preferences.userNodeForPackage(AppController.class);
        String mappingsStr = prefs.get(PREF_KEY_CUSTOM_TAB_MAPPINGS, "");
        
        customTabMappings.clear();
        
        if (!mappingsStr.isEmpty()) {
            String[] pairs = mappingsStr.split(";");
            for (String pair : pairs) {
                String[] parts = pair.split(":", 2);
                if (parts.length == 2) {
                    customTabMappings.put(parts[0], parts[1]);
                }
            }
        }
        
        updateCustomTabMappingsDisplay();
        
        // Create dynamic columns for loaded custom tabs
        for (String customTabName : customTabMappings.keySet()) {
            addCustomUnitColumn(customTabName);
        }
        
        // Update parser with loaded mappings
        if (parser != null) {
            parser.setCustomTabMappings(customTabMappings);
        }
    }
    
    // ---------- Update Labels ----------
    private void updateJsonModeLabel() {
        if (jsonModeLabel != null) {
            boolean isNoMerge = noMergeCheckbox != null && noMergeCheckbox.isSelected();
            boolean isMergedByConfigGroup = mergeByConfigGroupCheckbox != null && mergeByConfigGroupCheckbox.isSelected();
            boolean isMergedAcrossConfigGroup = mergeAcrossConfigGroupCheckbox != null && mergeAcrossConfigGroupCheckbox.isSelected();
            
            String mode = "Standard";
            if (isMergedByConfigGroup) {
                mode = "Merge Multiple Config Groups";
            } else if (isMergedAcrossConfigGroup) {
                mode = "Merge by Single Config Group";
            } else if (isNoMerge) {
                mode = "Standard";
            }
            
            jsonModeLabel.setText("JSON: " + mode);
        }
    }
    
    private void updateCurrentFileLabel() {
        if (currentFileLabel != null) {
            if (currentExcelFile != null) {
                currentFileLabel.setText("📄 " + currentExcelFile.getName());
            } else {
                currentFileLabel.setText("No file loaded");
            }
        }
    }
    
    /**
     * Determines the current merge mode based on checkbox selections.
     */
    private ExcelParserV5.MergeMode getCurrentMergeMode() {
        boolean isNoMerge = noMergeCheckbox != null && noMergeCheckbox.isSelected();
        boolean isMergeByConfigGroup = mergeByConfigGroupCheckbox != null && mergeByConfigGroupCheckbox.isSelected();
        boolean isMergeAcrossConfigGroup = mergeAcrossConfigGroupCheckbox != null && mergeAcrossConfigGroupCheckbox.isSelected();
        
        if (isMergeByConfigGroup) {
            return ExcelParserV5.MergeMode.MERGE_BY_CONFIG_GROUP;
        } else if (isMergeAcrossConfigGroup) {
            return ExcelParserV5.MergeMode.MERGE_ACROSS_CONFIG_GROUP;
        } else {
            // Default to NONE if noMergeCheckbox is selected or nothing is selected
            return ExcelParserV5.MergeMode.NONE;
        }
    }
    
    /**
     * Converts merge mode enum to human-readable text
     */
    private String getMergeModeText(ExcelParserV5.MergeMode mergeMode) {
        return switch (mergeMode) {
            case MERGE_BY_CONFIG_GROUP -> "Merged Multiple Config Groups";
            case MERGE_ACROSS_CONFIG_GROUP -> "Merged by Single Config Group";
            case NONE -> "Standard (No Merge)";
        };
    }
    
    // ---------- Progress Bar Helpers ----------
    private void showProgressBar(String statusMessage) {
        if (statusLabel != null) {
            statusLabel.setText(statusMessage);
        }
        if (statusProgressBar != null) {
            statusProgressBar.setProgress(-1); // Indeterminate progress
            statusProgressBar.setVisible(true);
            statusProgressBar.setManaged(true);
        }
    }
    
    private void hideProgressBar() {
        if (statusProgressBar != null) {
            statusProgressBar.setVisible(false);
            statusProgressBar.setManaged(false);
        }
    }

    // ---------- Button State Helpers ----------
    private void setButtonLoading(Button button, boolean loading) {
        if (button == null) return;
        boolean isCollapsed = sidebarContent.getStyleClass().contains("sidebar-collapsed");
        if (loading) {
            button.getStyleClass().remove("loaded");
            if (!button.getStyleClass().contains("loading")) {
                button.getStyleClass().add("loading");
            }
            // Save original text once
            if (!button.getProperties().containsKey("origText")) {
                button.getProperties().put("origText", button.getText());
            }
            // Show spinner
            javafx.scene.control.ProgressIndicator pi = new javafx.scene.control.ProgressIndicator();
            pi.setPrefSize(12, 12);
            pi.setProgress(-1);
            button.setGraphic(pi);
            button.setGraphicTextGap(8);
            
            // Only update text if sidebar is expanded
            if (!isCollapsed) {
                String base = String.valueOf(button.getProperties().get("origText"));
                button.setText(base + " — Loading…");
            }
            button.setTooltip(new Tooltip("Loading…"));
            button.setDisable(true);
        } else {
            button.getStyleClass().remove("loading");
            // Restore text and graphic when not loading
            Object orig = button.getProperties().get("origText");
            if (orig != null) button.setText(String.valueOf(orig));
            button.setGraphic(null);
            // If no loaded state, remove tooltip
            if (!button.getStyleClass().contains("loaded")) {
                button.setTooltip(null);
            }
            button.setDisable(false);
        }
    }

    private void setButtonLoaded(Button button, boolean loaded) {
        if (button == null) return;
        boolean isCollapsed = sidebarContent.getStyleClass().contains("sidebar-collapsed");
        button.getStyleClass().remove("loading");
        if (loaded) {
            if (!button.getStyleClass().contains("loaded")) {
                button.getStyleClass().add("loaded");
            }
            // Ensure original text captured
            if (!button.getProperties().containsKey("origText")) {
                button.getProperties().put("origText", button.getText());
            }
            // Only update text if sidebar is expanded
            if (!isCollapsed) {
                String base = String.valueOf(button.getProperties().get("origText"));
                button.setText(base + " ✓");
            }
            button.setGraphic(null);
            button.setTooltip(new Tooltip("Last load succeeded"));
        } else {
            button.getStyleClass().remove("loaded");
            Object orig = button.getProperties().get("origText");
            if (orig != null) button.setText(String.valueOf(orig));
            button.setGraphic(null);
            button.setTooltip(null);
        }
    }

    private void autoClearLoaded(Button button, double seconds) {
        if (button == null) return;
        // If timeout >= 600 seconds (10 minutes), keep loaded state persistent (don't auto-clear)
        if (seconds >= 600.0) {
            return; // Skip auto-clear, button stays loaded indefinitely
        }
        PauseTransition pause = new PauseTransition(Duration.seconds(seconds));
        pause.setOnFinished(e -> {
            // Only clear if still showing as loaded and not currently loading
            if (button.getStyleClass().contains("loaded") && !button.getStyleClass().contains("loading")) {
                setButtonLoaded(button, false);
            }
        });
        pause.play();
    }

    // ---------- Load NDW (Excel) ----------
    private void loadNdw() {
        try {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Select NDW Excel Workbook");
            chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Excel Files", "*.xlsx")
            );

            if (lastExcelDir != null && lastExcelDir.exists()) {
                chooser.setInitialDirectory(lastExcelDir);
            }

            File file = chooser.showOpenDialog(getStage());
            if (file == null) return;

            // Remember directory
            rememberDirectory(file, true);

            // Indicate loading state
            setButtonLoading(loadNdwButton, true);
            showProgressBar("📥 Loading Excel file...");

            Task<Void> task = new Task<>() {
                @Override
                protected Void call() throws Exception {
                    parser.load(file);
                    return null;
                }
            };

            task.setOnSucceeded(ev -> {
                try {
                    String loadSummary = parser.getLoadSummary();
                    currentExcelFile = file;
                    updateCurrentFileLabel();
                    jsonPreview.setText(loadSummary);

                    // Normalize Unit tab data: commas → newlines
                    normalizeAllUnitRows();

                    refreshTables();
                    tableUnits.refresh();
                    tableNurseCalls.refresh();
                    tableClinicals.refresh();
                    if (tableOrders != null) tableOrders.refresh();

                    setJsonButtonsEnabled(true);
                    setExcelButtonsEnabled(true);

                    // Hide progress and update status
                    hideProgressBar();
                    updateStatusLabel();
                    updateCustomTabStats();

                    // Mark button loaded
                    setButtonLoading(loadNdwButton, false);
                    setButtonLoaded(loadNdwButton, true);
                    autoClearLoaded(loadNdwButton, loadedTimeoutSeconds);

                    // Reapply sidebar state to restore icons if collapsed
                    applySidebarState();

                    // Build success message
                    StringBuilder successMsg = new StringBuilder("✅ Excel loaded successfully");

                    int nurseFlows = parser.nurseCalls.size();
                    int clinicalFlows = parser.clinicals.size();
                    int ordersFlows = parser.orders.size();
                    int unitsCount = parser.getUnitsCount();
                    int nurseGroups = parser.getNurseConfigGroupCount();
                    int clinicalGroups = parser.getClinicalConfigGroupCount();
                    int ordersGroups = parser.getOrdersConfigGroupCount();
                    int totalGroups = parser.getTotalConfigGroupCount();

                    successMsg.append("\n\nLoaded ")
                            .append(nurseFlows).append(" Nurse Calls, ")
                            .append(clinicalFlows).append(" Clinicals, and ")
                            .append(ordersFlows).append(" Orders flows.\n")
                            .append("Units: ").append(unitsCount).append("\n")
                            .append("Config Groups — Nurse: ").append(nurseGroups)
                            .append(", Clinical: ").append(clinicalGroups)
                            .append(", Orders: ").append(ordersGroups)
                            .append(" (Total: ").append(totalGroups).append(")");

                    int movedCount = parser.getEmdanMovedCount();
                    if (movedCount > 0) {
                        successMsg.append("\n\nMoved ").append(movedCount).append(" EMDAN rows to Clinicals");
                    }

                    Map<String, Integer> customTabCounts = parser.getCustomTabRowCounts();
                    if (!customTabCounts.isEmpty()) {
                        int totalCustomRows = customTabCounts.values().stream().mapToInt(Integer::intValue).sum();
                        if (totalCustomRows > 0) {
                            successMsg.append("\n\nCustom Tabs Processed:");
                            for (Map.Entry<String, Integer> entry : customTabCounts.entrySet()) {
                                String flowType = customTabMappings.get(entry.getKey());
                                successMsg.append("\n  • ").append(entry.getKey())
                                         .append(" → ").append(flowType)
                                         .append(": ").append(entry.getValue()).append(" rows");
                            }
                        }
                    }

                    showInfo(successMsg.toString());
                } finally {
                    setButtonLoading(loadNdwButton, false);
                }
            });

            task.setOnFailed(ev -> {
                setButtonLoading(loadNdwButton, false);
                hideProgressBar();
                Throwable ex = task.getException();
                showError("Failed to load file: " + (ex != null ? ex.getMessage() : "Unknown error"));
            });

            Thread th = new Thread(task);
            th.setDaemon(true);
            th.start();
        } catch (Exception ex) {
            setButtonLoading(loadNdwButton, false);
            hideProgressBar();
            showError("Failed to load file: " + ex.getMessage());
        }
    }

    // ---------- Load Engage XML ----------
    private void loadXml() {
        try {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Select Engage XML File");
            chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("XML Files", "*.xml")
            );

            if (lastExcelDir != null && lastExcelDir.exists()) {
                chooser.setInitialDirectory(lastExcelDir);
            }

            File file = chooser.showOpenDialog(getStage());
            if (file == null) return;

            // Remember directory
            rememberDirectory(file, true);

            // Loading state + progress
            setButtonLoading(loadXmlButton, true);
            showProgressBar("📥 Loading XML file...");

            Task<Void> task = new Task<>() {
                private XmlParser xmlParser;
                @Override
                protected Void call() throws Exception {
                    xmlParser = new XmlParser();
                    xmlParser.load(file);
                    return null;
                }
                @Override
                protected void succeeded() {
                    try {
                        // Transfer data from XML parser to main parser lists
                        parser.units.clear();
                        parser.units.addAll(xmlParser.getUnits());
                        parser.nurseCalls.clear();
                        parser.nurseCalls.addAll(xmlParser.getNurseCalls());
                        parser.clinicals.clear();
                        parser.clinicals.addAll(xmlParser.getClinicals());
                        parser.orders.clear();
                        parser.orders.addAll(xmlParser.getOrders());

                        String loadSummary = xmlParser.getLoadSummary();

                        currentExcelFile = file;
                        updateCurrentFileLabel();
                        jsonPreview.setText(loadSummary);

                        // Normalize Unit tab data: commas → newlines
                        normalizeAllUnitRows();

                        refreshTables();
                        tableUnits.refresh();
                        tableNurseCalls.refresh();
                        tableClinicals.refresh();
                        if (tableOrders != null) tableOrders.refresh();

                        setJsonButtonsEnabled(true);
                        setExcelButtonsEnabled(true);

                        // Hide progress and update status
                        hideProgressBar();
                        updateStatusLabel();
                        updateCustomTabStats();

                        // Mark button loaded
                        setButtonLoading(loadXmlButton, false);
                        setButtonLoaded(loadXmlButton, true);
                        autoClearLoaded(loadXmlButton, loadedTimeoutSeconds);

                        // Reapply sidebar state to restore icons if collapsed
                        applySidebarState();

                        // Success message
                        int nurseFlows = parser.nurseCalls.size();
                        int clinicalFlows = parser.clinicals.size();
                        int ordersFlows = parser.orders.size();
                        int unitsCount = parser.units.size();

                        int nurseGroups = (int) parser.nurseCalls.stream()
                            .map(fr -> fr.configGroup)
                                .filter(s -> s != null && !s.isBlank())
                                .distinct()
                                .count();
                        int clinicalGroups = (int) parser.clinicals.stream()
                            .map(fr -> fr.configGroup)
                                .filter(s -> s != null && !s.isBlank())
                                .distinct()
                                .count();
                        int ordersGroups = (int) parser.orders.stream()
                            .map(fr -> fr.configGroup)
                                .filter(s -> s != null && !s.isBlank())
                                .distinct()
                                .count();
                        int totalGroups = nurseGroups + clinicalGroups + ordersGroups;

                        showInfo("✅ XML loaded successfully\n\n" +
                            "Loaded " + nurseFlows + " Nurse Calls, " + clinicalFlows + " Clinicals, and " + ordersFlows + " Orders flows.\n" +
                            "Units: " + unitsCount + "\n" +
                            "Config Groups — Nurse: " + nurseGroups + ", Clinical: " + clinicalGroups + ", Orders: " + ordersGroups +
                            " (Total: " + totalGroups + ")");
                    } finally {
                        setButtonLoading(loadXmlButton, false);
                    }
                }
                @Override
                protected void failed() {
                    setButtonLoading(loadXmlButton, false);
                    hideProgressBar();
                    Throwable ex = getException();
                    showError("Failed to load XML file: " + (ex != null ? ex.getMessage() : "Unknown error"));
                }
            };

            Thread th = new Thread(task);
            th.setDaemon(true);
            th.start();
        } catch (Exception ex) {
            setButtonLoading(loadXmlButton, false);
            hideProgressBar();
            showError("Failed to load XML file: " + ex.getMessage());
        }
    }

    // ---------- Load JSON ----------
    private void loadJson() {
        try {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Select JSON File to Load");
            chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("JSON Files", "*.json")
            );

            if (lastJsonDir != null && lastJsonDir.exists()) {
                chooser.setInitialDirectory(lastJsonDir);
            }

            File file = chooser.showOpenDialog(getStage());
            if (file == null) return;

            // Remember directory
            rememberDirectory(file, false);

            // Loading state + progress
            setButtonLoading(loadJsonButton, true);
            showProgressBar("📥 Loading JSON file...");

            Task<Void> task = new Task<>() {
                @Override
                protected Void call() throws Exception {
                    parser.loadJson(file);
                    return null;
                }
            };

            task.setOnSucceeded(ev -> {
                try {
                    currentExcelFile = null; // Clear current Excel file reference
                    updateCurrentFileLabel(); // Update file label

                    StringBuilder loadSummary = new StringBuilder();
                    loadSummary.append("✅ JSON loaded successfully\n\n");
                    loadSummary.append("Loaded:\n".formatted());
                    loadSummary.append("  • %d Nurse Call flows\n".formatted(parser.nurseCalls.size()));
                    loadSummary.append("  • %d Clinical flows\n".formatted(parser.clinicals.size()));
                    loadSummary.append("  • %d Orders flows\n".formatted(parser.orders.size()));
                    loadSummary.append("  • %d Units\n".formatted(parser.getUnitsCount()));
                    loadSummary.append("  • Config Groups: %d (Nurse), %d (Clinical), %d (Orders) — Total %d\n".formatted(
                        parser.getNurseConfigGroupCount(),
                        parser.getClinicalConfigGroupCount(),
                        parser.getOrdersConfigGroupCount(),
                        parser.getTotalConfigGroupCount()
                    ));
                    loadSummary.append("\n⚠️ Note: Units data may be incomplete when loading from JSON.\n");
                    loadSummary.append("Some fields may not be fully populated.\n");
                    loadSummary.append("Consider loading the original Excel file for complete data.");

                    jsonPreview.setText(loadSummary.toString());

                    // Normalize Unit tab data: commas → newlines
                    normalizeAllUnitRows();

                    refreshTables();
                    tableUnits.refresh();
                    tableNurseCalls.refresh();
                    tableClinicals.refresh();
                    if (tableOrders != null) tableOrders.refresh();

                    setJsonButtonsEnabled(true);
                    setExcelButtonsEnabled(true);

                    // Hide progress and update status
                    hideProgressBar();
                    updateStatusLabel();

                    // Mark button loaded
                    setButtonLoading(loadJsonButton, false);
                    setButtonLoaded(loadJsonButton, true);
                    autoClearLoaded(loadJsonButton, loadedTimeoutSeconds);

                    // Reapply sidebar state to restore icons if collapsed
                    applySidebarState();

                    showInfo("✅ JSON loaded successfully\n\n" +
                        "Loaded " + parser.nurseCalls.size() + " Nurse Calls, " +
                        parser.clinicals.size() + " Clinicals, and " +
                        parser.orders.size() + " Orders flows.\n" +
                        "Units: " + parser.getUnitsCount() + "\n" +
                        "Config Groups — Nurse: " + parser.getNurseConfigGroupCount() +
                        ", Clinical: " + parser.getClinicalConfigGroupCount() +
                        ", Orders: " + parser.getOrdersConfigGroupCount() +
                        " (Total: " + parser.getTotalConfigGroupCount() + ")\n\n" +
                        "⚠️ Note: Some data may be incomplete when loading from JSON.");
                } finally {
                    setButtonLoading(loadJsonButton, false);
                }
            });

            task.setOnFailed(ev -> {
                setButtonLoading(loadJsonButton, false);
                hideProgressBar();
                Throwable ex = task.getException();
                showError("Failed to load JSON file: " + (ex != null ? ex.getMessage() : "Unknown error"));
            });

            Thread th = new Thread(task);
            th.setDaemon(true);
            th.start();
        } catch (Exception ex) {
            setButtonLoading(loadJsonButton, false);
            hideProgressBar();
            showError("Failed to load JSON file: " + ex.getMessage());
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

            // Show progress
            showProgressBar("💾 Saving Excel file...");
            
            parser.writeExcel(out);
            
            // Hide progress and show success
            hideProgressBar();
            if (statusLabel != null) {
                statusLabel.setText("✅ Excel saved successfully");
            }
            showInfo("💾 Excel generated successfully:\n" + out.getAbsolutePath());
        } catch (Exception ex) {
            hideProgressBar();
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

            syncEditsToParser(); // always sync before generating
            applyInterfaceReferences(); // Apply interface references

            ExcelParserV5.MergeMode mergeMode = getCurrentMergeMode();

            // Create a temporary parser with only filtered data
            ExcelParserV5 filteredParser = createFilteredParser();

            String json = switch (flowType) {
                case "NurseCalls" -> ExcelParserV5.pretty(filteredParser.buildNurseCallsJson(mergeMode));
                case "Clinicals" -> ExcelParserV5.pretty(filteredParser.buildClinicalsJson(mergeMode));
                case "Orders" -> ExcelParserV5.pretty(filteredParser.buildOrdersJson(mergeMode));
                default -> "";
            };

            jsonPreview.setText(json);
            String displayName = switch (flowType) {
                case "NurseCalls" -> "NurseCall";
                case "Clinicals" -> "Clinical";
                case "Orders" -> "Orders";
                default -> flowType;
            };
            statusLabel.setText("Generated " + displayName + " JSON");
            lastGeneratedJson = json;
            lastGeneratedWasNurseSide = "NurseCalls".equals(flowType); // Track the last generated type
        } catch (Exception ex) {
            showError("Error generating JSON: " + ex.getMessage());
        }
    }

    // ---------- Generate Combined JSON ----------
    private void generateCombinedJson() {
        try {
            if (parser == null) {
                showError("Please load an Excel file first.");
                return;
            }

            syncEditsToParser(); // always sync before generating
            applyInterfaceReferences(); // Apply interface references

            ExcelParserV5.MergeMode mergeMode = getCurrentMergeMode();

            // Create a temporary parser with only filtered data
            ExcelParserV5 filteredParser = createFilteredParser();

            // Build JSON for all three types
            StringBuilder combinedJson = new StringBuilder();
            
            // Add NurseCalls JSON
            try {
                String nurseJson = ExcelParserV5.pretty(filteredParser.buildNurseCallsJson(mergeMode));
                combinedJson.append("=== NurseCalls JSON ===\n\n");
                combinedJson.append(nurseJson);
                combinedJson.append("\n\n");
            } catch (Exception ex) {
                combinedJson.append("=== NurseCalls JSON ===\n\n");
                combinedJson.append("Error generating NurseCalls JSON: ").append(ex.getMessage());
                combinedJson.append("\n\n");
            }
            
            // Add Clinicals JSON
            try {
                String clinicalJson = ExcelParserV5.pretty(filteredParser.buildClinicalsJson(mergeMode));
                combinedJson.append("=== Clinicals JSON ===\n\n");
                combinedJson.append(clinicalJson);
                combinedJson.append("\n\n");
            } catch (Exception ex) {
                combinedJson.append("=== Clinicals JSON ===\n\n");
                combinedJson.append("Error generating Clinicals JSON: ").append(ex.getMessage());
                combinedJson.append("\n\n");
            }
            
            // Add Orders JSON
            try {
                String ordersJson = ExcelParserV5.pretty(filteredParser.buildOrdersJson(mergeMode));
                combinedJson.append("=== Orders JSON ===\n\n");
                combinedJson.append(ordersJson);
            } catch (Exception ex) {
                combinedJson.append("=== Orders JSON ===\n\n");
                combinedJson.append("Error generating Orders JSON: ").append(ex.getMessage());
            }

            jsonPreview.setText(combinedJson.toString());
            lastGeneratedJson = combinedJson.toString();
            statusLabel.setText("Generated combined JSON for all selected rows");
        } catch (Exception ex) {
            showError("Error generating combined JSON: " + ex.getMessage());
        }
    }

    // ---------- Export JSON ----------
    private void exportJson(String flowType) {
        try {
            if (parser == null) {
                showError("Please load an Excel file first.");
                return;
            }

            syncEditsToParser();
            applyInterfaceReferences(); // Apply interface references

            ExcelParserV5.MergeMode mergeMode = getCurrentMergeMode();

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
            if (file == null) return;

            // Remember directory
            rememberDirectory(file, false);

            // Show progress with enhanced dialog
            String displayName = switch (flowType) {
                case "NurseCalls" -> "NurseCall";
                case "Clinicals" -> "Clinical";
                case "Orders" -> "Orders";
                default -> "JSON";
            };
            
            // Show export status dialog
            showExportStatusDialog(displayName, file, mergeMode, () -> {
                try {
                    // Create a temporary parser with only filtered data
                    ExcelParserV5 filteredParser = createFilteredParser();

                    switch (flowType) {
                        case "NurseCalls" -> filteredParser.writeNurseCallsJson(file, mergeMode);
                        case "Clinicals" -> filteredParser.writeClinicalsJson(file, mergeMode);
                        case "Orders" -> filteredParser.writeOrdersJson(file, mergeMode);
                    }
                    return true;
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            });

        } catch (Exception ex) {
            hideProgressBar();
            showError("Error exporting JSON: " + ex.getMessage());
        }
    }
    
    /**
     * Shows export status dialog with progress indication
     */
    private void showExportStatusDialog(String flowType, File destination, ExcelParserV5.MergeMode mergeMode, java.util.concurrent.Callable<Boolean> exportTask) {
        // Create a modal stage for the export status
        javafx.stage.Stage dialog = new javafx.stage.Stage();
        dialog.initOwner(getStage());
        dialog.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        dialog.setTitle("Exporting " + flowType + " JSON");
        dialog.setResizable(false);
        
        // Create content
        VBox content = new VBox(15);
        content.setStyle("-fx-padding: 20; -fx-alignment: center;");
        
        Label titleLabel = new Label("📤 Exporting " + flowType + " JSON");
        titleLabel.setStyle("-fx-font-size: 16; -fx-font-weight: bold;");
        
        Label fileLabel = new Label("Destination: " + destination.getName());
        fileLabel.setStyle("-fx-font-size: 12;");
        
        Label pathLabel = new Label(destination.getParent());
        pathLabel.setStyle("-fx-font-size: 10; -fx-text-fill: gray;");
        
        String modeText = getMergeModeText(mergeMode);
        Label modeLabel = new Label("Mode: " + modeText);
        modeLabel.setStyle("-fx-font-size: 11;");
        
        ProgressBar progressBar = new ProgressBar();
        progressBar.setPrefWidth(300);
        progressBar.setProgress(-1); // Indeterminate
        
        Label statusLabel = new Label("Exporting...");
        statusLabel.setStyle("-fx-font-size: 12;");
        
        content.getChildren().addAll(titleLabel, fileLabel, pathLabel, modeLabel, progressBar, statusLabel);
        
        javafx.scene.Scene scene = new javafx.scene.Scene(content, 400, 250);
        dialog.setScene(scene);
        
        // Run export in background task
        Task<Boolean> task = new Task<>() {
            @Override
            protected Boolean call() throws Exception {
                updateMessage("Generating JSON...");
                return exportTask.call();
            }
        };
        
        statusLabel.textProperty().bind(task.messageProperty());
        
        task.setOnSucceeded(e -> {
            statusLabel.textProperty().unbind();
            statusLabel.setText("✅ Export completed successfully!");
            statusLabel.setStyle("-fx-font-size: 12; -fx-text-fill: green; -fx-font-weight: bold;");
            progressBar.setProgress(1.0);
            
            // Auto-close after 1.5 seconds
            PauseTransition pause = new PauseTransition(Duration.seconds(1.5));
            pause.setOnFinished(evt -> {
                dialog.close();
                this.statusLabel.setText("✅ Exported " + getMergeModeText(mergeMode) + " JSON");
                hideProgressBar();
            });
            pause.play();
        });
        
        task.setOnFailed(e -> {
            statusLabel.textProperty().unbind();
            statusLabel.setText("❌ Export failed!");
            statusLabel.setStyle("-fx-font-size: 12; -fx-text-fill: red; -fx-font-weight: bold;");
            progressBar.setProgress(0);
            hideProgressBar();
            
            PauseTransition pause = new PauseTransition(Duration.seconds(2));
            pause.setOnFinished(evt -> {
                dialog.close();
                showError("Error exporting JSON: " + task.getException().getMessage());
            });
            pause.play();
        });
        
        // Start the task in a background thread
        new Thread(task).start();
        
        // Show progress in status bar too
        showProgressBar("📤 Exporting " + flowType + " JSON...");
        
        // Show dialog
        dialog.show();
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
        generateJsonButton.setDisable(!enabled);
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
        if (jsonPreview == null) return null;
        var scene = jsonPreview.getScene();
        if (scene == null) return null;
        var window = scene.getWindow();
        if (window instanceof Stage stage) {
            return stage;
        }
        return null;
    }

    // ---------- Table column setup ----------
    private void initializeUnitColumns() {
        if (tableUnits != null) tableUnits.setEditable(true);
        // Preserve commas in Facility; no newline conversion
        setupEditable(unitFacilityCol, r -> r.facility, (r, v) -> r.facility = v);
        setupEditableUnit(unitNamesCol, r -> r.unitNames, (r, v) -> r.unitNames = v);
        setupEditableUnit(unitPodRoomFilterCol, r -> r.podRoomFilter, (r, v) -> r.podRoomFilter = v);
        setupEditableUnit(unitNurseGroupCol, r -> r.nurseGroup, (r, v) -> r.nurseGroup = v);
        setupEditableUnit(unitClinicalGroupCol, r -> r.clinGroup, (r, v) -> r.clinGroup = v);
        setupEditableUnit(unitOrdersGroupCol, r -> r.ordersGroup, (r, v) -> r.ordersGroup = v);
        setupEditableUnit(unitNoCareGroupCol, r -> r.noCareGroup, (r, v) -> r.noCareGroup = v);
        setupEditableUnit(unitCommentsCol, r -> r.comments, (r, v) -> r.comments = v);
    }

    private void initializeNurseColumns() {
        if (tableNurseCalls != null) tableNurseCalls.setEditable(true);
        setupCheckBox(nurseInScopeCol, f -> f.inScope, (f, v) -> f.inScope = v);
        setupEditable(nurseConfigGroupCol, f -> f.configGroup, (f, v) -> f.configGroup = v);
        setupEditable(nurseAlarmNameCol, f -> f.alarmName, (f, v) -> f.alarmName = v);
        setupEditable(nurseSendingNameCol, f -> f.sendingName, (f, v) -> f.sendingName = v);
        setupEditable(nursePriorityCol, f -> f.priorityRaw, (f, v) -> f.priorityRaw = v);
        setupDeviceAColumn(nurseDeviceACol);
        setupDeviceBColumn(nurseDeviceBCol);
        setupEditable(nurseRingtoneCol, f -> f.ringtone, (f, v) -> f.ringtone = v);
        setupEditable(nurseResponseOptionsCol, f -> safe(f.responseOptions), (f, v) -> f.responseOptions = safe(v));
        setupEditable(nurseBreakThroughDNDCol, f -> safe(f.breakThroughDND), (f, v) -> f.breakThroughDND = safe(v));
        setupEditable(nurseEscalateAfterCol, f -> safe(f.escalateAfter), (f, v) -> f.escalateAfter = safe(v));
        setupEditable(nurseTtlValueCol, f -> safe(f.ttlValue), (f, v) -> f.ttlValue = safe(v));
        setupEditable(nurseEnunciateCol, f -> safe(f.enunciate), (f, v) -> f.enunciate = safe(v));
        setupEditable(nurseT1Col, f -> f.t1, (f, v) -> f.t1 = v);
        setupFirstRecipientColumn(nurseR1Col, f -> f.r1, (f, v) -> f.r1 = v);
        setupEditable(nurseT2Col, f -> f.t2, (f, v) -> f.t2 = v);
        setupOtherRecipientColumn(nurseR2Col, f -> f.r2, (f, v) -> f.r2 = v);
        setupEditable(nurseT3Col, f -> f.t3, (f, v) -> f.t3 = v);
        setupOtherRecipientColumn(nurseR3Col, f -> f.r3, (f, v) -> f.r3 = v);
        setupEditable(nurseT4Col, f -> f.t4, (f, v) -> f.t4 = v);
        setupOtherRecipientColumn(nurseR4Col, f -> f.r4, (f, v) -> f.r4 = v);
        setupEditable(nurseT5Col, f -> f.t5, (f, v) -> f.t5 = v);
        setupOtherRecipientColumn(nurseR5Col, f -> f.r5, (f, v) -> f.r5 = v);
        
        // Make "In Scope" column sticky (always visible on left)
        makeStickyColumn(tableNurseCalls, nurseInScopeCol);
    }

    private void initializeClinicalColumns() {
        if (tableClinicals != null) tableClinicals.setEditable(true);
        setupCheckBox(clinicalInScopeCol, f -> f.inScope, (f, v) -> f.inScope = v);
        setupEditable(clinicalConfigGroupCol, f -> f.configGroup, (f, v) -> f.configGroup = v);
        setupEditable(clinicalAlarmNameCol, f -> f.alarmName, (f, v) -> f.alarmName = v);
        setupEditable(clinicalSendingNameCol, f -> f.sendingName, (f, v) -> f.sendingName = v);
        setupEditable(clinicalPriorityCol, f -> f.priorityRaw, (f, v) -> f.priorityRaw = v);
        setupDeviceAColumn(clinicalDeviceACol);
        setupDeviceBColumn(clinicalDeviceBCol);
        setupEditable(clinicalRingtoneCol, f -> f.ringtone, (f, v) -> f.ringtone = v);
        setupEditable(clinicalResponseOptionsCol, f -> safe(f.responseOptions), (f, v) -> f.responseOptions = safe(v));
        setupEditable(clinicalBreakThroughDNDCol, f -> safe(f.breakThroughDND), (f, v) -> f.breakThroughDND = safe(v));
        setupEditable(clinicalEscalateAfterCol, f -> safe(f.escalateAfter), (f, v) -> f.escalateAfter = safe(v));
        setupEditable(clinicalTtlValueCol, f -> safe(f.ttlValue), (f, v) -> f.ttlValue = safe(v));
        setupEditable(clinicalEnunciateCol, f -> safe(f.enunciate), (f, v) -> f.enunciate = safe(v));
        setupEditable(clinicalEmdanCol, f -> safe(f.emdan), (f, v) -> f.emdan = safe(v));
        setupEditable(clinicalT1Col, f -> f.t1, (f, v) -> f.t1 = v);
        setupFirstRecipientColumn(clinicalR1Col, f -> f.r1, (f, v) -> f.r1 = v);
        setupEditable(clinicalT2Col, f -> f.t2, (f, v) -> f.t2 = v);
        setupOtherRecipientColumn(clinicalR2Col, f -> f.r2, (f, v) -> f.r2 = v);
        setupEditable(clinicalT3Col, f -> f.t3, (f, v) -> f.t3 = v);
        setupOtherRecipientColumn(clinicalR3Col, f -> f.r3, (f, v) -> f.r3 = v);
        setupEditable(clinicalT4Col, f -> f.t4, (f, v) -> f.t4 = v);
        setupOtherRecipientColumn(clinicalR4Col, f -> f.r4, (f, v) -> f.r4 = v);
        setupEditable(clinicalT5Col, f -> f.t5, (f, v) -> f.t5 = v);
        setupOtherRecipientColumn(clinicalR5Col, f -> f.r5, (f, v) -> f.r5 = v);
        
        // Make "In Scope" column sticky (always visible on left)
        makeStickyColumn(tableClinicals, clinicalInScopeCol);
    }

    private void initializeOrdersColumns() {
        if (tableOrders != null) tableOrders.setEditable(true);
        setupCheckBox(ordersInScopeCol, f -> f.inScope, (f, v) -> f.inScope = v);
        setupEditable(ordersConfigGroupCol, f -> f.configGroup, (f, v) -> f.configGroup = v);
        setupEditable(ordersAlarmNameCol, f -> f.alarmName, (f, v) -> f.alarmName = v);
        setupEditable(ordersSendingNameCol, f -> f.sendingName, (f, v) -> f.sendingName = v);
        setupEditable(ordersPriorityCol, f -> f.priorityRaw, (f, v) -> f.priorityRaw = v);
        setupDeviceAColumn(ordersDeviceACol);
        setupDeviceBColumn(ordersDeviceBCol);
        setupEditable(ordersRingtoneCol, f -> f.ringtone, (f, v) -> f.ringtone = v);
        setupEditable(ordersResponseOptionsCol, f -> safe(f.responseOptions), (f, v) -> f.responseOptions = safe(v));
        setupEditable(ordersBreakThroughDNDCol, f -> safe(f.breakThroughDND), (f, v) -> f.breakThroughDND = safe(v));
        setupEditable(ordersEscalateAfterCol, f -> safe(f.escalateAfter), (f, v) -> f.escalateAfter = safe(v));
        setupEditable(ordersTtlValueCol, f -> safe(f.ttlValue), (f, v) -> f.ttlValue = safe(v));
        setupEditable(ordersEnunciateCol, f -> safe(f.enunciate), (f, v) -> f.enunciate = safe(v));
        setupEditable(ordersT1Col, f -> f.t1, (f, v) -> f.t1 = v);
        setupFirstRecipientColumn(ordersR1Col, f -> f.r1, (f, v) -> f.r1 = v);
        setupEditable(ordersT2Col, f -> f.t2, (f, v) -> f.t2 = v);
        setupOtherRecipientColumn(ordersR2Col, f -> f.r2, (f, v) -> f.r2 = v);
        setupEditable(ordersT3Col, f -> f.t3, (f, v) -> f.t3 = v);
        setupOtherRecipientColumn(ordersR3Col, f -> f.r3, (f, v) -> f.r3 = v);
        setupEditable(ordersT4Col, f -> f.t4, (f, v) -> f.t4 = v);
        setupOtherRecipientColumn(ordersR4Col, f -> f.r4, (f, v) -> f.r4 = v);
        setupEditable(ordersT5Col, f -> f.t5, (f, v) -> f.t5 = v);
        setupOtherRecipientColumn(ordersR5Col, f -> f.r5, (f, v) -> f.r5 = v);
        
        // Make "In Scope" column sticky (always visible on left)
        makeStickyColumn(tableOrders, ordersInScopeCol);
    }

    private <R> void setupEditable(TableColumn<R, String> col, Function<R, String> getter, BiConsumer<R, String> setter) {
        if (col == null) return;
        col.setCellValueFactory(d -> new SimpleStringProperty(safe(getter.apply(d.getValue()))));
        col.setCellFactory(TextAreaTableCell.forTableColumn());
        col.setOnEditCommit(ev -> {
            R row = ev.getRowValue();
            setter.accept(row, ev.getNewValue());
            if (col.getTableView() != null) col.getTableView().refresh();
        });
    }

    // ---------- Units-specific helpers: commas → newlines ----------
    private static String commaToNewlines(String s) {
        if (s == null) return null;
        return s.replaceAll("\\s*,\\s*", "\n");
    }

    private void normalizeUnitRow(ExcelParserV5.UnitRow r) {
        if (r == null) return;
        r.unitNames = commaToNewlines(r.unitNames);
        r.podRoomFilter = commaToNewlines(r.podRoomFilter);
        r.nurseGroup = commaToNewlines(r.nurseGroup);
        r.clinGroup = commaToNewlines(r.clinGroup);
        r.ordersGroup = commaToNewlines(r.ordersGroup);
        r.noCareGroup = commaToNewlines(r.noCareGroup);
        r.comments = commaToNewlines(r.comments);
    }

    private void normalizeAllUnitRows() {
        if (parser == null || parser.units == null) return;
        for (ExcelParserV5.UnitRow r : parser.units) {
            normalizeUnitRow(r);
        }
    }

    private void setupEditableUnit(TableColumn<ExcelParserV5.UnitRow, String> col,
                                   Function<ExcelParserV5.UnitRow, String> getter,
                                   BiConsumer<ExcelParserV5.UnitRow, String> setter) {
        if (col == null) return;
        col.setCellValueFactory(d -> new SimpleStringProperty(safe(getter.apply(d.getValue()))));
        col.setCellFactory(TextAreaTableCell.forTableColumn());
        col.setOnEditCommit(ev -> {
            ExcelParserV5.UnitRow row = ev.getRowValue();
            String newVal = ev.getNewValue();
            newVal = commaToNewlines(newVal);
            setter.accept(row, newVal);
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
    
    /**
     * Sets up an editable column for Device-A with validation highlighting.
     * Cells that don't contain valid recipient keywords are highlighted with light orange.
     * Blank cells are NOT highlighted.
     * Valid keywords (case-insensitive): VCS, Edge, XMPP, Vocera, VMP, OutgoingWCTP
     */
    private void setupDeviceAColumn(TableColumn<ExcelParserV5.FlowRow, String> col) {
        setupValidatedColumn(col, f -> f.deviceA, (f, v) -> f.deviceA = v, false, 
            item -> parser != null && parser.hasValidRecipientKeyword(item));
    }
    
    /**
     * Sets up an editable column for Device-B with validation highlighting.
     * Cells that don't contain valid recipient keywords are highlighted with light orange.
     * Blank cells are NOT highlighted.
     * Valid keywords (case-insensitive): VCS, Edge, XMPP, Vocera
     */
    private void setupDeviceBColumn(TableColumn<ExcelParserV5.FlowRow, String> col) {
        setupValidatedColumn(col, f -> f.deviceB, (f, v) -> f.deviceB = v, false, 
            item -> parser != null && parser.hasValidRecipientKeyword(item));
    }
    
    /**
     * Sets up an editable column for the 1st recipient (R1) with validation highlighting.
     * Cells are highlighted with light orange when:
     * - The cell is blank/empty, OR
     * - No valid recipient keywords are found
     * 
     * Valid keywords (case-insensitive): VCS, Edge, XMPP, Vocera
     */
    private void setupFirstRecipientColumn(TableColumn<ExcelParserV5.FlowRow, String> col, 
                                           Function<ExcelParserV5.FlowRow, String> getter, 
                                           BiConsumer<ExcelParserV5.FlowRow, String> setter) {
        setupValidatedColumn(col, getter, setter, true, 
            item -> parser != null && parser.isValidFirstRecipient(item));
    }
    
    /**
     * Sets up an editable column for recipients 2-5 (R2-R5) with validation highlighting.
     * Cells are highlighted with light orange when:
     * - No valid recipient keywords are found
     * 
     * Blank/empty cells are NOT highlighted for R2-R5.
     * Valid keywords (case-insensitive): VCS, Edge, XMPP, Vocera
     */
    private void setupOtherRecipientColumn(TableColumn<ExcelParserV5.FlowRow, String> col, 
                                           Function<ExcelParserV5.FlowRow, String> getter, 
                                           BiConsumer<ExcelParserV5.FlowRow, String> setter) {
        setupValidatedColumn(col, getter, setter, true, 
            item -> parser != null && parser.isValidOtherRecipient(item));
    }
    
    // ---------- Dynamic Custom Unit Columns ----------
    /**
     * Creates a dynamic column for a custom tab mapping in the Units table.
     * The column is inserted before the "No Caregiver Group" column.
     */
    private void addCustomUnitColumn(String customTabName) {
        if (customUnitColumns.containsKey(customTabName)) {
            return; // Column already exists
        }
        
        if (tableUnits == null) return;
        
        // Create the new column
        TableColumn<ExcelParserV5.UnitRow, String> newColumn = new TableColumn<>(customTabName + " Group");
        newColumn.setPrefWidth(160.0);
        
        // Set up cell value factory to read/write from customGroups map
        newColumn.setCellValueFactory(d -> {
            ExcelParserV5.UnitRow row = d.getValue();
            String value = row.customGroups.getOrDefault(customTabName, "");
            return new SimpleStringProperty(value);
        });
        
        // Make the column editable
        newColumn.setCellFactory(TextAreaTableCell.forTableColumn());
        newColumn.setOnEditCommit(ev -> {
            ExcelParserV5.UnitRow row = ev.getRowValue();
            row.customGroups.put(customTabName, ev.getNewValue());
            if (parser != null) {
                parser.rebuildUnitMaps();
            }
            tableUnits.refresh();
        });
        
        // Find the index of the "No Caregiver Group" column
        int noCareIndex = tableUnits.getColumns().indexOf(unitNoCareGroupCol);
        if (noCareIndex >= 0) {
            // Insert before "No Caregiver Group"
            tableUnits.getColumns().add(noCareIndex, newColumn);
        } else {
            // Fallback: add at the end if No Caregiver column not found
            tableUnits.getColumns().add(newColumn);
        }
        
        // Track the column
        customUnitColumns.put(customTabName, newColumn);
    }
    
    /**
     * Removes a dynamic column for a custom tab mapping from the Units table.
     */
    private void removeCustomUnitColumn(String customTabName) {
        TableColumn<ExcelParserV5.UnitRow, String> column = customUnitColumns.remove(customTabName);
        if (column != null && tableUnits != null) {
            tableUnits.getColumns().remove(column);
        }
        
        // Also remove the custom group data from all unit rows
        if (parser != null) {
            for (ExcelParserV5.UnitRow unitRow : parser.units) {
                unitRow.customGroups.remove(customTabName);
            }
            parser.rebuildUnitMaps();
        }
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
                if (col.getTableView() != null) col.getTableView().refresh();
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
        
        // Initialize alarm name filter text fields
        if (nurseAlarmNameFilter != null) {
            nurseAlarmNameFilter.textProperty().addListener((obs, oldV, newV) -> applyNurseFilter());
        }
        if (clinicalAlarmNameFilter != null) {
            clinicalAlarmNameFilter.textProperty().addListener((obs, oldV, newV) -> applyClinicalFilter());
        }
        if (ordersAlarmNameFilter != null) {
            ordersAlarmNameFilter.textProperty().addListener((obs, oldV, newV) -> applyOrdersFilter());
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
        
        String selectedConfigGroup = nurseConfigGroupFilter != null ? nurseConfigGroupFilter.getSelectionModel().getSelectedItem() : null;
        String searchText = nurseAlarmNameFilter != null ? nurseAlarmNameFilter.getText() : "";
        String searchFilter = searchText != null ? searchText.trim().toLowerCase() : "";
        
        nurseCallsFilteredList.setPredicate(flow -> {
            // Config group filter
            boolean configMatch = selectedConfigGroup == null || selectedConfigGroup.equals("All") || selectedConfigGroup.equals(flow.configGroup);
            
            // Search all columns
            boolean searchMatch = searchFilter.isEmpty() || matchesAnyColumn(flow, searchFilter);
            
            return configMatch && searchMatch;
        });
        
        // Update inScope based on filter
        if (nurseCallsFullList != null) {
            for (ExcelParserV5.FlowRow flow : nurseCallsFullList) {
                boolean configMatch = selectedConfigGroup == null || selectedConfigGroup.equals("All") || selectedConfigGroup.equals(flow.configGroup);
                boolean searchMatch = searchFilter.isEmpty() || matchesAnyColumn(flow, searchFilter);
                flow.inScope = configMatch && searchMatch;
            }
        }
        
        if (tableNurseCalls != null) tableNurseCalls.refresh();
        updateStatusLabel();
    }

    /**
     * Helper method to check if a FlowRow matches the search filter in any column.
     * Searches all text fields in the FlowRow for case-insensitive partial matches.
     * 
     * @param flow The FlowRow to search
     * @param searchFilter The lowercase search string
     * @return true if any column contains the search filter
     */
    private boolean matchesAnyColumn(ExcelParserV5.FlowRow flow, String searchFilter) {
        if (flow == null || searchFilter == null || searchFilter.trim().isEmpty()) {
            return true;
        }
        
        // Search all string fields in FlowRow
        return (flow.alarmName != null && flow.alarmName.toLowerCase().contains(searchFilter)) ||
               (flow.sendingName != null && flow.sendingName.toLowerCase().contains(searchFilter)) ||
               (flow.priorityRaw != null && flow.priorityRaw.toLowerCase().contains(searchFilter)) ||
               (flow.deviceA != null && flow.deviceA.toLowerCase().contains(searchFilter)) ||
               (flow.deviceB != null && flow.deviceB.toLowerCase().contains(searchFilter)) ||
               (flow.ringtone != null && flow.ringtone.toLowerCase().contains(searchFilter)) ||
               (flow.responseOptions != null && flow.responseOptions.toLowerCase().contains(searchFilter)) ||
               (flow.breakThroughDND != null && flow.breakThroughDND.toLowerCase().contains(searchFilter)) ||
               (flow.multiUserAccept != null && flow.multiUserAccept.toLowerCase().contains(searchFilter)) ||
               (flow.escalateAfter != null && flow.escalateAfter.toLowerCase().contains(searchFilter)) ||
               (flow.ttlValue != null && flow.ttlValue.toLowerCase().contains(searchFilter)) ||
               (flow.enunciate != null && flow.enunciate.toLowerCase().contains(searchFilter)) ||
               (flow.emdan != null && flow.emdan.toLowerCase().contains(searchFilter)) ||
               (flow.configGroup != null && flow.configGroup.toLowerCase().contains(searchFilter)) ||
               (flow.customTabSource != null && flow.customTabSource.toLowerCase().contains(searchFilter)) ||
               (flow.t1 != null && flow.t1.toLowerCase().contains(searchFilter)) ||
               (flow.r1 != null && flow.r1.toLowerCase().contains(searchFilter)) ||
               (flow.t2 != null && flow.t2.toLowerCase().contains(searchFilter)) ||
               (flow.r2 != null && flow.r2.toLowerCase().contains(searchFilter)) ||
               (flow.t3 != null && flow.t3.toLowerCase().contains(searchFilter)) ||
               (flow.r3 != null && flow.r3.toLowerCase().contains(searchFilter)) ||
               (flow.t4 != null && flow.t4.toLowerCase().contains(searchFilter)) ||
               (flow.r4 != null && flow.r4.toLowerCase().contains(searchFilter)) ||
               (flow.t5 != null && flow.t5.toLowerCase().contains(searchFilter)) ||
               (flow.r5 != null && flow.r5.toLowerCase().contains(searchFilter));
    }

    private void applyClinicalFilter() {
        if (clinicalsFilteredList == null) return;
        
        String selectedConfigGroup = clinicalConfigGroupFilter != null ? clinicalConfigGroupFilter.getSelectionModel().getSelectedItem() : null;
        String searchText = clinicalAlarmNameFilter != null ? clinicalAlarmNameFilter.getText() : "";
        String searchFilter = searchText != null ? searchText.trim().toLowerCase() : "";
        
        clinicalsFilteredList.setPredicate(flow -> {
            // Config group filter
            boolean configMatch = selectedConfigGroup == null || selectedConfigGroup.equals("All") || selectedConfigGroup.equals(flow.configGroup);
            
            // Search all columns
            boolean searchMatch = searchFilter.isEmpty() || matchesAnyColumn(flow, searchFilter);
            
            return configMatch && searchMatch;
        });
        
        // Update inScope based on filter
        if (clinicalsFullList != null) {
            for (ExcelParserV5.FlowRow flow : clinicalsFullList) {
                boolean configMatch = selectedConfigGroup == null || selectedConfigGroup.equals("All") || selectedConfigGroup.equals(flow.configGroup);
                boolean searchMatch = searchFilter.isEmpty() || matchesAnyColumn(flow, searchFilter);
                flow.inScope = configMatch && searchMatch;
            }
        }
        
        if (tableClinicals != null) tableClinicals.refresh();
        updateStatusLabel();
    }

    private void applyOrdersFilter() {
        if (ordersFilteredList == null) return;
        
        String selectedConfigGroup = ordersConfigGroupFilter != null ? ordersConfigGroupFilter.getSelectionModel().getSelectedItem() : null;
        String searchText = ordersAlarmNameFilter != null ? ordersAlarmNameFilter.getText() : "";
        String searchFilter = searchText != null ? searchText.trim().toLowerCase() : "";
        
        ordersFilteredList.setPredicate(flow -> {
            // Config group filter
            boolean configMatch = selectedConfigGroup == null || selectedConfigGroup.equals("All") || selectedConfigGroup.equals(flow.configGroup);
            
            // Search all columns
            boolean searchMatch = searchFilter.isEmpty() || matchesAnyColumn(flow, searchFilter);
            
            return configMatch && searchMatch;
        });
        
        // Update inScope based on filter
        if (ordersFullList != null) {
            for (ExcelParserV5.FlowRow flow : ordersFullList) {
                boolean configMatch = selectedConfigGroup == null || selectedConfigGroup.equals("All") || selectedConfigGroup.equals(flow.configGroup);
                boolean searchMatch = searchFilter.isEmpty() || matchesAnyColumn(flow, searchFilter);
                flow.inScope = configMatch && searchMatch;
            }
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
        
        String status = "Units: %d/%d | Nurse Calls: %d/%d | Clinicals: %d/%d | Orders: %d/%d".formatted(
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
        confirmAlert.setHeaderText("⚠️ You are about to reset the application");
        confirmAlert.setContentText("This action cannot be undone. All loaded data (units, nurse calls, clinicals, orders), voice groups, and settings will be cleared and reset to defaults.\n\nDo you want to continue?");
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
                
                updateCurrentFileLabel(); // Update file label
                
                // Clear JSON preview
                jsonPreview.setText("All data cleared. Load an Excel file to begin.");
                lastGeneratedJson = "";
                
                // Disable buttons
                setJsonButtonsEnabled(false);
                setExcelButtonsEnabled(false);

                // Reset load button states
                setButtonLoaded(loadNdwButton, false);
                setButtonLoaded(loadXmlButton, false);
                setButtonLoaded(loadJsonButton, false);
                setButtonLoading(loadNdwButton, false);
                setButtonLoading(loadXmlButton, false);
                setButtonLoading(loadJsonButton, false);
                if (loadNdwButton != null) loadNdwButton.setTooltip(null);
                if (loadXmlButton != null) loadXmlButton.setTooltip(null);
                if (loadJsonButton != null) loadJsonButton.setTooltip(null);
                
                // Clear voice groups data and button state
                synchronized(loadedVoiceGroups) {
                    loadedVoiceGroups.clear();
                }
                updateVoiceGroupStats();
                setButtonLoaded(loadVoiceGroupButton, false);
                setButtonLoading(loadVoiceGroupButton, false);
                if (loadVoiceGroupButton != null) loadVoiceGroupButton.setTooltip(null);
                
                // Also clear the Clear Voice Group button state
                if (clearVoiceGroupButton != null) {
                    setButtonLoaded(clearVoiceGroupButton, false);
                    setButtonLoading(clearVoiceGroupButton, false);
                    clearVoiceGroupButton.setTooltip(null);
                }
                
                // Reset all settings to defaults
                // Reset interface reference names to defaults (reusing existing logic)
                if (edgeRefNameField != null) edgeRefNameField.setText("OutgoingWCTP");
                if (vcsRefNameField != null) vcsRefNameField.setText("VMP");
                if (voceraRefNameField != null) voceraRefNameField.setText("Vocera");
                if (xmppRefNameField != null) xmppRefNameField.setText("XMPP");
                
                // Clear default interface checkboxes
                if (defaultEdgeCheckbox != null) defaultEdgeCheckbox.setSelected(false);
                if (defaultVmpCheckbox != null) defaultVmpCheckbox.setSelected(false);
                if (defaultVoceraCheckbox != null) defaultVoceraCheckbox.setSelected(false);
                if (defaultXmppCheckbox != null) defaultXmppCheckbox.setSelected(false);
                
                // Clear merge-related checkboxes
                if (noMergeCheckbox != null) noMergeCheckbox.setSelected(false);
                if (mergeByConfigGroupCheckbox != null) mergeByConfigGroupCheckbox.setSelected(false);
                if (mergeAcrossConfigGroupCheckbox != null) mergeAcrossConfigGroupCheckbox.setSelected(false);
                if (combineConfigGroupCheckbox != null) combineConfigGroupCheckbox.setSelected(false);
                
                // Clear room filter fields
                if (roomFilterNursecallField != null) roomFilterNursecallField.setText("");
                if (roomFilterClinicalField != null) roomFilterClinicalField.setText("");
                if (roomFilterOrdersField != null) roomFilterOrdersField.setText("");
                
                // Refresh tables properly by recreating filtered lists
                refreshTables();
                
                // Update status
                updateStatusLabel();
                statusLabel.setText("✅ All data cleared successfully");
                
                // Reapply sidebar state to restore icons if collapsed
                applySidebarState();
                
                showInfo("All data and settings have been cleared and reset successfully.\n\nYou can now load a new Excel file.");
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
                ExcelParserV5.MergeMode mergeMode = getCurrentMergeMode();
                ExcelParserV5 filteredParser = createFilteredParser();
                var json = ExcelParserV5.pretty(
                    lastGeneratedWasNurseSide ? filteredParser.buildNurseCallsJson(mergeMode) : filteredParser.buildClinicalsJson(mergeMode)
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
                showWarning("Engage rules will merge the selected adapters into one combined rule.");
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

    private static String sanitizeForPlantUml(String value) {
        if (value == null) {
            return "";
        }

        String cleaned = value.chars()
            .mapToObj(c -> {
                if (c < 32 || c == 127) {
                    return " ";
                }
                return String.valueOf((char) c);
            })
            .collect(Collectors.joining());

        String normalized = cleaned
            .replace("[", "(")
            .replace("]", ")")
            .replace("{", "(")
            .replace("}", ")")
            .replace("<", "(")
            .replace(">", ")")
            .replace("|", "/")
            .replace("\\", "/")
            .replace("\"", "'")
            .replaceAll("\\s+", " ")
            .trim();

        return normalized.length() > 0 ? normalized : "-";
    }

    private static String sanitizeLabelOrEmpty(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "";
        }

        String sanitized = sanitizeForPlantUml(value);
        return "-".equals(sanitized) ? "" : sanitized;
    }

    private static boolean isImmediateTime(String value) {
        if (value == null) {
            return false;
        }

        String normalized = value.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", "");
        if (normalized.isEmpty()) {
            return false;
        }

        if (normalized.equals("immediate") || normalized.equals("immediately") || normalized.equals("now")) {
            return true;
        }

        return normalized.equals("0")
            || normalized.equals("0s")
            || normalized.equals("0sec")
            || normalized.equals("0secs")
            || normalized.equals("0second")
            || normalized.equals("0seconds");
    }

    private String buildVisualFlowDiagram(String tabLabel, String configLabel, List<ExcelParserV5.FlowRow> flows) {
        StringBuilder plantuml = new StringBuilder();
        plantuml.append("@startuml\n");
        plantuml.append("top to bottom direction\n");
        plantuml.append("skinparam shadowing false\n");
        plantuml.append("skinparam backgroundColor #FFFFFF\n");
        plantuml.append("hide stereotype\n");
        plantuml.append("skinparam rectangle {\n");
        plantuml.append("  RoundCorner 16\n");
        plantuml.append("  FontSize 14\n");
        plantuml.append("  FontColor #111111\n");
        plantuml.append("}\n");
        plantuml.append("skinparam rectangle<<GlobalHeader>> {\n");
        plantuml.append("  BackgroundColor #ffffff\n");
        plantuml.append("  BorderColor #b5b5b5\n");
        plantuml.append("}\n");
        plantuml.append("skinparam rectangle<<FlowHeader>> {\n");
        plantuml.append("  BackgroundColor #dcdcdc\n");
        plantuml.append("  BorderColor #9a9a9a\n");
        plantuml.append("}\n");
        plantuml.append("skinparam rectangle<<StopA>> {\n");
        plantuml.append("  BackgroundColor #c8f7c5\n");
        plantuml.append("  BorderColor #4f9a4f\n");
        plantuml.append("}\n");
        plantuml.append("skinparam rectangle<<StopB>> {\n");
        plantuml.append("  BackgroundColor #cfe2ff\n");
        plantuml.append("  BorderColor #4a78c2\n");
        plantuml.append("}\n");
        plantuml.append("skinparam ArrowColor #333333\n");
        plantuml.append("skinparam ArrowFontSize 12\n");
        plantuml.append("skinparam ArrowThickness 1.4\n\n");

        String globalHeaderLabel = tabLabel + " — " + configLabel;
        plantuml.append("rectangle \"").append(globalHeaderLabel).append("\" as GlobalHeader_1 <<GlobalHeader>> {\n");
        plantuml.append("together {\n");

        int rowCounter = 1;
        for (ExcelParserV5.FlowRow row : flows) {
            String[] recipients = { row.r1, row.r2, row.r3, row.r4, row.r5 };
            String[] times = { row.t1, row.t2, row.t3, row.t4, row.t5 };

            List<Integer> steps = new ArrayList<>();
            for (int i = 0; i < recipients.length; i++) {
                if (recipients[i] != null && !recipients[i].trim().isEmpty()) {
                    steps.add(i);
                }
            }

            if (steps.isEmpty()) {
                continue;
            }

            List<String> headerLines = new ArrayList<>();
            headerLines.add(sanitizeForPlantUml(row.alarmName));
            String priority = sanitizeForPlantUml(row.priorityRaw);
            if (!"-".equals(priority)) {
                headerLines.add(priority);
            }
            String headerLabel = String.join("\\n", headerLines);
            String headerId = "FlowHeader_" + rowCounter;
            plantuml.append("  rectangle \"").append(headerLabel).append("\" as ")
                .append(headerId).append(" <<FlowHeader>>\n");

            String firstArrowLabel;
            if (isImmediateTime(times[0])) {
                firstArrowLabel = "Immediate";
            } else {
                firstArrowLabel = sanitizeLabelOrEmpty(times[0]);
            }

            String previousId = headerId;

            for (int i = 0; i < steps.size(); i++) {
                int idx = steps.get(i);
                String stageId = "Stop_" + rowCounter + "_" + (i + 1);
                
                // Split multiple recipients and display each on a separate line
                List<String> stageLabelLines = new ArrayList<>();
                stageLabelLines.add("Alarm Stop " + (i + 1));
                
                // Split recipients by comma, semicolon, or newline BEFORE sanitizing to preserve formatting
                String rawRecipient = recipients[idx];
                if (rawRecipient != null && !rawRecipient.isEmpty()) {
                    // Split by comma, semicolon, newline, or carriage return to handle both Unix and Windows line endings
                    String[] recipientParts = rawRecipient.split("[,;\\n\\r]+");
                    for (String part : recipientParts) {
                        String trimmed = part.trim();
                        if (!trimmed.isEmpty()) {
                            // Sanitize each individual recipient part
                            String sanitizedPart = sanitizeForPlantUml(trimmed);
                            if (!sanitizedPart.isEmpty() && !"-".equals(sanitizedPart)) {
                                stageLabelLines.add(sanitizedPart);
                            }
                        }
                    }
                }
                
                String stageLabel = String.join("\\n", stageLabelLines);

                String stereo = (i % 2 == 0) ? "StopA" : "StopB";

                plantuml.append("  rectangle \"").append(stageLabel).append("\" as ")
                    .append(stageId).append(" <<").append(stereo).append(">>\n");

                String arrowLabel = "";
                if (previousId.equals(headerId)) {
                    arrowLabel = firstArrowLabel;
                } else {
                    arrowLabel = sanitizeLabelOrEmpty(times[idx]);
                }

                plantuml.append("  ").append(previousId).append(" -down-> ").append(stageId);
                if (arrowLabel != null && !arrowLabel.isEmpty()) {
                    plantuml.append(" : ").append(arrowLabel);
                }
                plantuml.append("\n");

                previousId = stageId;
            }

            plantuml.append("\n");
            rowCounter++;
        }

        plantuml.append("}\n");
        plantuml.append("}\n\n");
        plantuml.append("@enduml\n");
        return plantuml.toString();
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
    
    // ---------- Theme Toggle ----------
    private void toggleTheme() {
        isDarkMode = !isDarkMode;
        
        // Save preference
        Preferences prefs = Preferences.userNodeForPackage(AppController.class);
        prefs.putBoolean(PREF_KEY_DARK_MODE, isDarkMode);
        
        // Apply theme
        applyTheme();
        updateThemeButton();
        
        statusLabel.setText(isDarkMode ? "Switched to Dark Mode" : "Switched to Light Mode");
    }
    
    private void applyTheme() {
        try {
            var scene = getStage().getScene();
            if (scene != null) {
                scene.getStylesheets().clear();
                
                String themePath = isDarkMode ? 
                    "/css/dark-theme.css" : 
                    "/css/vocera-theme.css";
                
                var cssResource = getClass().getResource(themePath);
                if (cssResource != null) {
                    scene.getStylesheets().add(cssResource.toExternalForm());
                } else {
                    System.err.println("Warning: " + themePath + " not found in resources.");
                }
            }
        } catch (Exception ex) {
            System.err.println("Failed to apply theme: " + ex.getMessage());
        }
    }
    
    private void updateThemeButton() {
        if (themeToggleButton != null) {
            if (isTopBarCollapsed) {
                // If collapsed, keep the icon and update tooltip
                setCollapsedButton(themeToggleButton, "/icons/darkmode.png", isDarkMode ? "Light Mode" : "Dark Mode");


            } else {
                // If expanded, show full text
                themeToggleButton.setText(isDarkMode ? "☀️ Light Mode" : "🌙 Dark Mode");
                themeToggleButton.setGraphic(null); // Clear icon
            }
        }
    }
    
    // ---------- Sidebar Toggle ----------
    @FXML
   
    private void toggleSidebar() {
        isSidebarCollapsed = !isSidebarCollapsed;
        
        // Save preference
        Preferences prefs = Preferences.userNodeForPackage(AppController.class);
        prefs.putBoolean(PREF_KEY_SIDEBAR_COLLAPSED, isSidebarCollapsed);
        
        // Apply state
        applySidebarState();
        
        statusLabel.setText(isSidebarCollapsed ? "Sidebar collapsed" : "Sidebar expanded");
    }
    
    // ---------- Update Custom Tab Stats ----------
    private void updateCustomTabStats() {
        if (customTabStatsLabel == null) return;
        
        if (parser == null) {
            customTabStatsLabel.setText("");
            return;
        }
        
        Map<String, Integer> customTabCounts = parser.getCustomTabRowCounts();
        if (customTabCounts.isEmpty()) {
            customTabStatsLabel.setText("");
            return;
        }
        
        int totalCustomRows = customTabCounts.values().stream().mapToInt(Integer::intValue).sum();
        if (totalCustomRows == 0) {
            customTabStatsLabel.setText("No rows found in custom tabs");
            return;
        }
        
        StringBuilder stats = new StringBuilder();
        stats.append("Last load: ");
        
        int processedTabs = 0;
        for (Map.Entry<String, Integer> entry : customTabCounts.entrySet()) {
            if (entry.getValue() > 0) {
                if (processedTabs > 0) stats.append(", ");
                stats.append(entry.getKey()).append(" (").append(entry.getValue()).append(")");
                processedTabs++;
            }
        }
        
        if (processedTabs == 0) {
            stats.append("0 custom tab rows");
        }
        
        customTabStatsLabel.setText(stats.toString());
    }
    
    private void applySidebarState() {
        if (sidebarContainer != null && sidebarContent != null && sidebarToggleButton != null) {
            if (isSidebarCollapsed) {
                // Collapse: show icons only, make container narrow but visible
                collapseSidebarToIcons();
                sidebarToggleButton.setText("▶"); // Arrow pointing right
            } else {
                // Expand: show full content, make container normal width
                expandSidebarToFull();
                sidebarToggleButton.setText("◀"); // Arrow pointing left
            }
        }
    }
    
    /**
     * Store original button texts for sidebar collapse/expand functionality
     */
    private void storeOriginalButtonTexts() {
        // Store regular button texts
        if (loadNdwButton != null) originalButtonTexts.put(loadNdwButton, loadNdwButton.getText());
        if (loadXmlButton != null) originalButtonTexts.put(loadXmlButton, loadXmlButton.getText());
        if (loadJsonButton != null) originalButtonTexts.put(loadJsonButton, loadJsonButton.getText());
        if (clearAllButton != null) originalButtonTexts.put(clearAllButton, clearAllButton.getText());
        if (generateJsonButton != null) originalButtonTexts.put(generateJsonButton, generateJsonButton.getText());
        if (exportNurseJsonButton != null) originalButtonTexts.put(exportNurseJsonButton, exportNurseJsonButton.getText());
        if (exportClinicalJsonButton != null) originalButtonTexts.put(exportClinicalJsonButton, exportClinicalJsonButton.getText());
        if (exportOrdersJsonButton != null) originalButtonTexts.put(exportOrdersJsonButton, exportOrdersJsonButton.getText());
        if (visualFlowButton != null) originalButtonTexts.put(visualFlowButton, visualFlowButton.getText());
        
        // Store top bar button texts
        if (settingsButton != null) originalButtonTexts.put(settingsButton, settingsButton.getText());
        if (saveExcelButton != null) originalButtonTexts.put(saveExcelButton, saveExcelButton.getText());
        if (themeToggleButton != null) originalButtonTexts.put(themeToggleButton, themeToggleButton.getText());
        if (helpButton != null) originalButtonTexts.put(helpButton, helpButton.getText());
        
        // Store toggle button texts
        if (navUnits != null) originalToggleTexts.put(navUnits, navUnits.getText());
        if (navNurseCalls != null) originalToggleTexts.put(navNurseCalls, navNurseCalls.getText());
        if (navClinicals != null) originalToggleTexts.put(navClinicals, navClinicals.getText());
        if (navOrders != null) originalToggleTexts.put(navOrders, navOrders.getText());
    }
    
    /**
     * Collapse sidebar to show only icons
     */
    private void collapseSidebarToIcons() {
        // Keep sidebar visible but make it narrow
        sidebarContent.setVisible(true);
        sidebarContent.setManaged(true);
        sidebarContainer.setPrefWidth(60);
        
        // Add collapsed style class for CSS styling
        if (!sidebarContent.getStyleClass().contains("sidebar-collapsed")) {
            sidebarContent.getStyleClass().add("sidebar-collapsed");
        }
        
        // Hide section labels and show short text for collapsed state
        hideLabelsAndShowShortText();
    }
    
    /**
     * Expand sidebar to show full content
     */
    private void expandSidebarToFull() {
        sidebarContent.setVisible(true);
        sidebarContent.setManaged(true);
        sidebarContainer.setPrefWidth(200);
        
        // Remove collapsed style class
        sidebarContent.getStyleClass().remove("sidebar-collapsed");
        
        // Show section labels and restore full text for expanded state
        showLabelsAndRestoreFullText();
    }
    
    /**
     * Hide labels and show short text on buttons (collapsed mode)
     */
    private void hideLabelsAndShowShortText() {
        // Hide section labels in collapsed mode
        if (loadDataLabel != null) {
            loadDataLabel.setVisible(false);
            loadDataLabel.setManaged(false);
        }
        if (exportJsonLabel != null) {
            exportJsonLabel.setVisible(false);
            exportJsonLabel.setManaged(false);
        }
        
        // Set distinct icons for collapsed mode - use PNG icons instead of emojis
        setCollapsedButton(loadNdwButton, "/icons/load-ndw.png", "Load NDW");
        setCollapsedButton(loadXmlButton, "/icons/load-xml.png", "Load Engage XML");
        setCollapsedButton(loadJsonButton, "/icons/load-json.png", "Load Engage Rules");
        setCollapsedButton(clearAllButton, "/icons/clear.png", "Clear All");
        setCollapsedButton(generateJsonButton, "/icons/preview.png", "Preview JSON");
        // Export JSON: use distinct icons
        setCollapsedButton(exportNurseJsonButton, "/icons/export-nurse.png", "Nursecall");
        setCollapsedButton(exportClinicalJsonButton, "/icons/export-clinical.png", "Clinicals");
        setCollapsedButton(exportOrdersJsonButton, "/icons/export-orders.png", "Orders");
        setCollapsedButton(visualFlowButton, "/icons/visual-flow.png", "Visual CallFlow");

        // Table tabs: use PNG icons instead of emojis
        setCollapsedTab(navUnits, "/icons/unit.png", "Units");
        setCollapsedTab(navNurseCalls, "/icons/nurse.png", "Nurse Calls");
        setCollapsedTab(navClinicals, "/icons/clinical.png", "Clinicals");
        setCollapsedTab(navOrders, "/icons/orders.png", "Orders");
    }
    
    /**
     * Show labels and restore full text on buttons (expanded mode)
     */
    private void showLabelsAndRestoreFullText() {
        // Show section labels in expanded mode
        if (loadDataLabel != null) {
            loadDataLabel.setVisible(true);
            loadDataLabel.setManaged(true);
        }
        if (exportJsonLabel != null) {
            exportJsonLabel.setVisible(true);
            exportJsonLabel.setManaged(true);
        }
        
        // Restore button texts
        restoreButtonText(loadNdwButton);
        restoreButtonText(loadXmlButton);
        restoreButtonText(loadJsonButton);
        restoreButtonText(clearAllButton);
        restoreButtonText(generateJsonButton);
        restoreButtonText(exportNurseJsonButton);
        restoreButtonText(exportClinicalJsonButton);
        restoreButtonText(exportOrdersJsonButton);
        restoreButtonText(visualFlowButton);

        // Restore toggle button texts (table tabs)
        restoreToggleButtonText(navUnits);
        restoreToggleButtonText(navNurseCalls);
        restoreToggleButtonText(navClinicals);
        restoreToggleButtonText(navOrders);

        // Remove tooltips in expanded mode
        removeTooltip(loadNdwButton);
        removeTooltip(loadXmlButton);
        removeTooltip(loadJsonButton);
        removeTooltip(clearAllButton);
        removeTooltip(generateJsonButton);
        removeTooltip(exportNurseJsonButton);
        removeTooltip(exportClinicalJsonButton);
        removeTooltip(exportOrdersJsonButton);
        removeTooltip(visualFlowButton);
        removeTooltip(navUnits);
        removeTooltip(navNurseCalls);
        removeTooltip(navClinicals);
        removeTooltip(navOrders);
    }
    
    /**
     * Load an icon from resources and create an ImageView
     */
    private ImageView loadIcon(String iconPath) {
        try {
            var iconStream = getClass().getResourceAsStream(iconPath);
            if (iconStream != null) {
                Image image = new Image(iconStream);
                ImageView imageView = new ImageView(image);
                imageView.setFitWidth(16);
                imageView.setFitHeight(16);
                imageView.setPreserveRatio(true);
                return imageView;
            }
        } catch (Exception e) {
            System.err.println("Failed to load icon: " + iconPath + " - " + e.getMessage());
        }
        return null;
    }

    /**
     * Set a button to collapsed mode with icon and tooltip
     */
    private void setCollapsedButton(Button button, String iconPath, String tooltip) {
        if (button != null) {
            ImageView icon = loadIcon(iconPath);
            if (icon != null) {
                button.setGraphic(icon);
                button.setText("");
            }
            button.setTooltip(new Tooltip(tooltip));
        }
    }

    /**
     * Set a toggle button (tab) to collapsed mode (icon only, tooltip for full name)
     */
    private void setCollapsedTab(ToggleButton button, String iconPath, String tooltip) {
        if (button != null) {
            ImageView icon = loadIcon(iconPath);
            if (icon != null) {
                button.setGraphic(icon);
                button.setText("");
            }
            button.setTooltip(new Tooltip(tooltip));
        }
    }

    /**
     * Remove tooltip from a button or toggle button
     */
    private void removeTooltip(ButtonBase button) {
        if (button != null) button.setTooltip(null);
    }
    
    /**
     * Convert a button to show only its icon
     */
    private void convertButtonToIcon(Button button) {
        if (button != null && button.getUserData() != null) {
            String icon = button.getUserData().toString();
            button.setText(icon);
            
            // Add tooltip with full text
            String originalText = originalButtonTexts.get(button);
            if (originalText != null) {
                // Remove emoji from tooltip text
                String tooltipText = originalText.replaceAll("[\ud83c-\ud83f][\udc00-\udfff]|[\ud800-\udbff][\udc00-\udfff]|[\u2600-\u27ff]", "").trim();
                button.setTooltip(new Tooltip(tooltipText));
            }
        }
    }
    
    /**
     * Convert a toggle button to show only its icon
     */
    private void convertToggleButtonToIcon(ToggleButton button) {
        if (button != null && button.getUserData() != null) {
            String icon = button.getUserData().toString();
            button.setText(icon);
            
            // Add tooltip with full text
            String originalText = originalToggleTexts.get(button);
            if (originalText != null) {
                // Remove emoji from tooltip text
                String tooltipText = originalText.replaceAll("[\ud83c-\ud83f][\udc00-\udfff]|[\ud800-\udbff][\udc00-\udfff]|[\u2600-\u27ff]", "").trim();
                button.setTooltip(new Tooltip(tooltipText));
            }
        }
    }
    
    /**
     * Restore a button's original text
     */
    private void restoreButtonText(Button button) {
        if (button != null) {
            String originalText = originalButtonTexts.get(button);
            if (originalText != null) {
                button.setText(originalText);
                button.setGraphic(null);  // Clear the icon graphic
                button.setTooltip(null);
            }
        }
    }
    
    /**
     * Restore a toggle button's original text
     */
    private void restoreToggleButtonText(ToggleButton button) {
        if (button != null) {
            String originalText = originalToggleTexts.get(button);
            if (originalText != null) {
                button.setText(originalText);
                button.setGraphic(null);  // Clear the icon graphic
                button.setTooltip(null);
            }
        }
    }
    
    /**
     * Makes a table column "sticky" (frozen) by preventing it from being reordered
     * and keeping it visible during horizontal scrolling.
     * This implementation uses CSS transforms to keep the column fixed in place.
     */
    private <T> void makeStickyColumn(TableView<T> table, TableColumn<T, ?> column) {
        if (table == null || column == null) return;
        
        // Prevent the column from being moved
        column.setReorderable(false);
        
        // Add a style class to visually indicate the sticky column
        column.getStyleClass().add("sticky-column");
        
        // Make the column frozen during horizontal scrolling
        // We need to wait for the table to be fully rendered before accessing the scroll bar
        table.skinProperty().addListener((obs, oldSkin, newSkin) -> {
            if (newSkin != null) {
                freezeColumn(table, column);
            }
        });
        
        // If skin already exists, apply freezing immediately
        if (table.getSkin() != null) {
            freezeColumn(table, column);
        }
    }
    
    /**
     * Applies the freeze effect to a column by listening to horizontal scroll events
     * and translating the column to compensate for scrolling.
     */
    private <T> void freezeColumn(TableView<T> table, TableColumn<T, ?> column) {
        // Use a Timer to retry attaching the scroll listener with proper delays
        // This ensures we catch the scrollbar even if the table is rendered later
        java.util.Timer timer = new java.util.Timer(true);
        java.util.concurrent.atomic.AtomicInteger attempts = new java.util.concurrent.atomic.AtomicInteger(0);
        
        timer.schedule(new java.util.TimerTask() {
            @Override
            public void run() {
                javafx.application.Platform.runLater(() -> {
                    boolean success = attachScrollListener(table, column);
                    if (success || attempts.incrementAndGet() >= 5) {
                        timer.cancel();
                    }
                });
            }
        }, 0, 500); // Try every 500ms, up to 5 times
    }
    
    /**
     * Helper method to attach scroll listener to freeze a column
     * @return true if successfully attached, false otherwise
     */
    private <T> boolean attachScrollListener(TableView<T> table, TableColumn<T, ?> column) {
        // Find the horizontal scroll bar
        javafx.scene.control.ScrollBar horizontalScrollBar = null;
        for (javafx.scene.Node node : table.lookupAll(".scroll-bar")) {
            if (node instanceof javafx.scene.control.ScrollBar sb) {
                if (sb.getOrientation() == javafx.geometry.Orientation.HORIZONTAL) {
                    horizontalScrollBar = sb;
                    break;
                }
            }
        }
        
        if (horizontalScrollBar != null) {
            final javafx.scene.control.ScrollBar scrollBar = horizontalScrollBar;
            
            // Listen to scroll position changes
            scrollBar.valueProperty().addListener((obs, oldVal, newVal) -> {
                updateColumnPosition(table, column, scrollBar);
            });
            
            // Initial position update
            updateColumnPosition(table, column, scrollBar);
            return true;
        }
        return false;
    }
    
    /**
     * Updates the position of a frozen column based on scroll position
     */
    private <T> void updateColumnPosition(TableView<T> table, TableColumn<T, ?> column, javafx.scene.control.ScrollBar scrollBar) {
        // No need for Platform.runLater here since we're already on the JavaFX thread
        // (called from a listener on the scrollBar)
        
        // The scroll bar value in JavaFX TableView represents the horizontal pixel offset
        // To keep the column fixed, we translate it by the same amount the table scrolled
        double hScrollValue = scrollBar.getValue();
        
        // Find and translate the column header
        java.util.Set<javafx.scene.Node> headers = table.lookupAll(".column-header");
        for (javafx.scene.Node header : headers) {
            // Check if this header belongs to our sticky column
            if (header.getStyleClass().contains("sticky-column") || 
                isHeaderForColumn(header, column)) {
                header.setTranslateX(hScrollValue);
                header.setMouseTransparent(false);
                header.setPickOnBounds(true);
                header.setViewOrder(-1); // keep header above scrolling content
                header.toFront();
            }
        }
        
        // Find and translate the column cells
        java.util.Set<javafx.scene.Node> cells = table.lookupAll(".table-cell");
        for (javafx.scene.Node cell : cells) {
            if (cell instanceof javafx.scene.control.TableCell<?, ?> tableCell) {
                if (tableCell.getTableColumn() == column) {
                    cell.setTranslateX(hScrollValue);
                    cell.setMouseTransparent(false);
                    cell.setPickOnBounds(true);
                    cell.setViewOrder(-1); // ensure checkbox stays clickable
                    cell.toFront();
                }
            }
        }
    }
    
    /**
     * Checks if a header node belongs to a specific column
     */
    private <T> boolean isHeaderForColumn(javafx.scene.Node header, TableColumn<T, ?> column) {
        if (!(header instanceof javafx.scene.layout.Region)) return false;
        
        javafx.scene.layout.Region region = (javafx.scene.layout.Region) header;
        for (javafx.scene.Node child : region.getChildrenUnmodifiable()) {
            if (child instanceof javafx.scene.control.Label label) {
                if (column.getText() != null && column.getText().equals(label.getText())) {
                    return true;
                }
            }
        }
        return false;
    }
    
    // ---------- Combine Config Group Methods ----------
    
    /**
     * Combine rows with identical columns (except config group) into single rows
     * with merged unique config group values
     */
    private void combineConfigGroupRows() {
        // Don't combine if no data loaded
        if (parser == null || parser.nurseCalls.isEmpty() && parser.clinicals.isEmpty() && parser.orders.isEmpty()) {
            return;
        }
        
        // Backup original data before combining
        originalNurseCalls = new ArrayList<>(parser.nurseCalls);
        originalClinicals = new ArrayList<>(parser.clinicals);
        originalOrders = new ArrayList<>(parser.orders);
        
        // Combine each table (modify in place)
        List<ExcelParserV5.FlowRow> combinedNurse = combineFlowRows(parser.nurseCalls);
        parser.nurseCalls.clear();
        parser.nurseCalls.addAll(combinedNurse);
        
        List<ExcelParserV5.FlowRow> combinedClinicals = combineFlowRows(parser.clinicals);
        parser.clinicals.clear();
        parser.clinicals.addAll(combinedClinicals);
        
        List<ExcelParserV5.FlowRow> combinedOrders = combineFlowRows(parser.orders);
        parser.orders.clear();
        parser.orders.addAll(combinedOrders);
        
        // Update full lists and filtered lists properly
        if (nurseCallsFullList != null) {
            nurseCallsFullList.setAll(parser.nurseCalls);
        }
        if (clinicalsFullList != null) {
            clinicalsFullList.setAll(parser.clinicals);
        }
        if (ordersFullList != null) {
            ordersFullList.setAll(parser.orders);
        }
        
        // Reapply filters
        applyNurseFilter();
        applyClinicalFilter();
        applyOrdersFilter();
    }
    
    /**
     * Revert to original uncombined data
     */
    private void revertCombinedConfigGroupRows() {
        if (originalNurseCalls != null) {
            parser.nurseCalls.clear();
            parser.nurseCalls.addAll(originalNurseCalls);
            if (nurseCallsFullList != null) {
                nurseCallsFullList.setAll(originalNurseCalls);
            }
            originalNurseCalls = null;
        }
        if (originalClinicals != null) {
            parser.clinicals.clear();
            parser.clinicals.addAll(originalClinicals);
            if (clinicalsFullList != null) {
                clinicalsFullList.setAll(originalClinicals);
            }
            originalClinicals = null;
        }
        if (originalOrders != null) {
            parser.orders.clear();
            parser.orders.addAll(originalOrders);
            if (ordersFullList != null) {
                ordersFullList.setAll(originalOrders);
            }
            originalOrders = null;
        }
        
        // Reapply filters
        applyNurseFilter();
        applyClinicalFilter();
        applyOrdersFilter();
    }
    
    /**
     * Combine flow rows with identical fields (except configGroup)
     * Returns a new list with combined rows
     */
    private List<ExcelParserV5.FlowRow> combineFlowRows(List<ExcelParserV5.FlowRow> rows) {
        if (rows == null || rows.isEmpty()) return rows;
        
        Map<String, List<ExcelParserV5.FlowRow>> groupedRows = new LinkedHashMap<>();
        
        for (ExcelParserV5.FlowRow row : rows) {
            // Create a key based on all fields except configGroup
            String key = createRowKey(row);
            groupedRows.computeIfAbsent(key, k -> new ArrayList<>()).add(row);
        }
        
        List<ExcelParserV5.FlowRow> combinedRows = new ArrayList<>();
        
        for (List<ExcelParserV5.FlowRow> group : groupedRows.values()) {
            if (group.size() == 1) {
                // No duplicates, add as is
                combinedRows.add(group.getFirst());
            } else {
                // Multiple rows with same fields but different config groups
                // Combine them into one row
                ExcelParserV5.FlowRow combined = new ExcelParserV5.FlowRow();
                ExcelParserV5.FlowRow first = group.getFirst();
                
                // Copy all fields from first row
                copyFlowRow(first, combined);
                
                // Merge unique config group values
                Set<String> uniqueConfigGroups = new LinkedHashSet<>();
                for (ExcelParserV5.FlowRow r : group) {
                    if (r.configGroup != null && !r.configGroup.trim().isEmpty()) {
                        // Split by common separators and add unique values
                        for (String part : r.configGroup.split("[,/\\|]")) {
                            String trimmed = part.trim();
                            if (!trimmed.isEmpty()) {
                                uniqueConfigGroups.add(trimmed);
                            }
                        }
                    }
                }
                
                // Join unique config groups
                combined.configGroup = String.join(" / ", uniqueConfigGroups);
                combinedRows.add(combined);
            }
        }
        
        return combinedRows;
    }
    
    /**
     * Create a unique key for a flow row based on all fields except configGroup
     */
    private String createRowKey(ExcelParserV5.FlowRow row) {
        return String.join("|",
            nvl(row.type),
            nvl(row.alarmName),
            nvl(row.sendingName),
            nvl(row.priorityRaw),
            nvl(row.deviceA),
            nvl(row.deviceB),
            nvl(row.ringtone),
            nvl(row.responseOptions),
            nvl(row.breakThroughDND),
            nvl(row.multiUserAccept),
            nvl(row.escalateAfter),
            nvl(row.ttlValue),
            nvl(row.enunciate),
            nvl(row.emdan),
            nvl(row.t1), nvl(row.r1),
            nvl(row.t2), nvl(row.r2),
            nvl(row.t3), nvl(row.r3),
            nvl(row.t4), nvl(row.r4),
            nvl(row.t5), nvl(row.r5),
            String.valueOf(row.inScope)
        );
    }
    
    /**
     * Null-value helper for key creation
     */
    private String nvl(String value) {
        return value == null ? "" : value;
    }
    
    /**
     * Copy all fields from source to destination FlowRow
     */
    private void copyFlowRow(ExcelParserV5.FlowRow source, ExcelParserV5.FlowRow dest) {
        dest.inScope = source.inScope;
        dest.type = source.type;
        dest.alarmName = source.alarmName;
        dest.sendingName = source.sendingName;
        dest.configGroup = source.configGroup;
        dest.priorityRaw = source.priorityRaw;
        dest.deviceA = source.deviceA;
        dest.deviceB = source.deviceB;
        dest.ringtone = source.ringtone;
        dest.responseOptions = source.responseOptions;
        dest.breakThroughDND = source.breakThroughDND;
        dest.multiUserAccept = source.multiUserAccept;
        dest.escalateAfter = source.escalateAfter;
        dest.ttlValue = source.ttlValue;
        dest.enunciate = source.enunciate;
        dest.emdan = source.emdan;
        dest.t1 = source.t1;
        dest.r1 = source.r1;
        dest.t2 = source.t2;
        dest.r2 = source.r2;
        dest.t3 = source.t3;
        dest.r3 = source.r3;
        dest.t4 = source.t4;
        dest.r4 = source.r4;
        dest.t5 = source.t5;
        dest.r5 = source.r5;
    }
    
    /**
     * Refresh all flow tables
     */
    private void refreshAllTables() {
        if (tableNurseCalls != null) {
            tableNurseCalls.setItems(FXCollections.observableArrayList(parser.nurseCalls));
            tableNurseCalls.refresh();
        }
        if (tableClinicals != null) {
            tableClinicals.setItems(FXCollections.observableArrayList(parser.clinicals));
            tableClinicals.refresh();
        }
        if (tableOrders != null) {
            tableOrders.setItems(FXCollections.observableArrayList(parser.orders));
            tableOrders.refresh();
        }
    }

    // ---------- Generate Visual Flow PDF ----------
    private void generateVisualFlow() {
        try {
            if (parser == null) {
                showError("Please load an Excel file first.");
                return;
            }
            if (jsonPreview == null) {
                showError("UI not fully initialized. Please try again.");
                return;
            }
            // Collect checked rows
            List<ExcelParserV5.FlowRow> checkedRows = new ArrayList<>();
            if (nurseCallsFullList != null) {
                checkedRows.addAll(nurseCallsFullList.stream().filter(r -> r.inScope).collect(Collectors.toList()));
            }
            if (clinicalsFullList != null) {
                checkedRows.addAll(clinicalsFullList.stream().filter(r -> r.inScope).collect(Collectors.toList()));
            }
            if (ordersFullList != null) {
                checkedRows.addAll(ordersFullList.stream().filter(r -> r.inScope).collect(Collectors.toList()));
            }
            if (checkedRows.isEmpty()) {
                showError("No rows are checked (in scope). Please check some rows first.");
                return;
            }
            final String groupDelimiter = "||";
            Map<String, List<ExcelParserV5.FlowRow>> grouped = checkedRows.stream()
                .collect(Collectors.groupingBy(r -> {
                    String tabName = (r.customTabSource != null && !r.customTabSource.isBlank())
                        ? r.customTabSource
                        : safe(r.type);
                    String config = (r.configGroup != null && !r.configGroup.isBlank())
                        ? r.configGroup
                        : "Unknown";
                    return tabName + groupDelimiter + config;
                }));

            List<String> diagrams = new ArrayList<>();

            for (String key : grouped.keySet().stream().sorted().collect(Collectors.toList())) {
                String[] parts = key.split("\\Q" + groupDelimiter + "\\E", 2);
                String tabLabel = sanitizeForPlantUml(parts.length > 0 ? parts[0] : "");
                if ("-".equals(tabLabel)) {
                    tabLabel = "Unknown Tab";
                }
                String configLabel = sanitizeForPlantUml(parts.length > 1 ? parts[1] : "Unknown");

                List<ExcelParserV5.FlowRow> flows = grouped.get(key).stream()
                    .sorted(Comparator.comparing(r -> safe(r.alarmName)))
                    .collect(Collectors.toList());

                for (int i = 0; i < flows.size(); i += 3) {
                    List<ExcelParserV5.FlowRow> pageFlows = flows.subList(i, Math.min(i + 3, flows.size()));
                    diagrams.add(buildVisualFlowDiagram(tabLabel, configLabel, pageFlows));
                }
            }

            if (diagrams.isEmpty()) {
                showError("No valid flows found to export.");
                return;
            }
            // File chooser - check if stage is available
            Stage stage = getStage();
            if (stage == null) {
                showError("Application window not available. Please try again.");
                return;
            }
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Save Visual Flow PDF");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
            chooser.setInitialFileName("AlarmFlow.pdf");
            if (lastJsonDir != null && lastJsonDir.exists()) {
                chooser.setInitialDirectory(lastJsonDir);
            }
            File file = chooser.showSaveDialog(stage);
            if (file == null) return;
            // Remember directory
            rememberDirectory(file, false);
            try (PDDocument document = new PDDocument()) {
                for (String diagram : diagrams) {
                    byte[] diagramBytes;
                    try (ByteArrayOutputStream pngOutput = new ByteArrayOutputStream()) {
                        SourceStringReader reader = new SourceStringReader(diagram);
                        reader.outputImage(pngOutput, new FileFormatOption(FileFormat.PNG));
                        diagramBytes = pngOutput.toByteArray();
                    } catch (IOException e) {
                        showError("Error generating diagram: " + e.getMessage());
                        return;
                    }

                    BufferedImage diagramImage;
                    try (ByteArrayInputStream bais = new ByteArrayInputStream(diagramBytes)) {
                        diagramImage = ImageIO.read(bais);
                    } catch (IOException e) {
                        showError("Error reading generated diagram: " + e.getMessage());
                        return;
                    }

                    if (diagramImage == null) {
                        showError("Generated diagram could not be read.");
                        return;
                    }

                    float imageWidthPoints = diagramImage.getWidth() * 72f / 96f;
                    float imageHeightPoints = diagramImage.getHeight() * 72f / 96f;

                    PDPage page = new PDPage(PDRectangle.LETTER);
                    document.addPage(page);

                    float pageWidth = page.getMediaBox().getWidth();
                    float pageHeight = page.getMediaBox().getHeight();
                    float margin = 36f; // half-inch margin
                    float availableWidth = Math.max(pageWidth - 2 * margin, 1f);
                    float availableHeight = Math.max(pageHeight - 2 * margin, 1f);
                    float scale = Math.min(availableWidth / imageWidthPoints, availableHeight / imageHeightPoints);
                    if (!Float.isFinite(scale) || scale <= 0) {
                        scale = 1f;
                    }

                    float drawWidth = imageWidthPoints * scale;
                    float drawHeight = imageHeightPoints * scale;
                    float startX = (pageWidth - drawWidth) / 2f;
                    float startY = (pageHeight - drawHeight) / 2f;

                    PDImageXObject pdImage = LosslessFactory.createFromImage(document, diagramImage);
                    try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                        contentStream.drawImage(pdImage, startX, startY, drawWidth, drawHeight);
                    }
                }

                document.save(file);
            } catch (IOException e) {
                showError("Error writing PDF: " + e.getMessage());
                return;
            }

            showInfo("Visual Flow PDF saved to:\n" + file.getAbsolutePath());
        } catch (Exception ex) {
            java.io.StringWriter sw = new java.io.StringWriter();
            java.io.PrintWriter pw = new java.io.PrintWriter(sw);
            ex.printStackTrace(pw);
            String stackTrace = sw.toString();
            String errorMsg = ex.getMessage();
            if (errorMsg == null || errorMsg.trim().isEmpty()) {
                errorMsg = "Unknown error occurred";
            }
            showError("Error generating visual flow: " + errorMsg + "\n\nStack trace:\n" + stackTrace);
        }
    }
    
    // ---------- Voice Group Validation Methods ----------

    private void loadVoiceGroups() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select Voice Group File");
        chooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Spreadsheets", "*.xlsx", "*.xls", "*.csv"),
            new FileChooser.ExtensionFilter("All Files", "*.*")
        );
        
        if (lastExcelDir != null && lastExcelDir.exists()) {
            chooser.setInitialDirectory(lastExcelDir);
        }
        
        File file = chooser.showOpenDialog(getStage());
        if (file == null) return;

        setButtonLoading(loadVoiceGroupButton, true);
        
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                Set<String> groups = new HashSet<>();
                String name = file.getName().toLowerCase();
                
                if (name.endsWith(".csv")) {
                    try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                        String line;
                        while ((line = br.readLine()) != null) {
                            String[] parts = line.split(",");
                            if (parts.length > 0 && !parts[0].trim().isEmpty()) {
                                groups.add(parts[0].trim());
                            }
                        }
                    }
                } else {
                    try (Workbook workbook = WorkbookFactory.create(file)) {
                        Sheet sheet = workbook.getSheetAt(0);
                        for (Row row : sheet) {
                            org.apache.poi.ss.usermodel.Cell cell = row.getCell(0);
                            if (cell != null) {
                                String val = new DataFormatter().formatCellValue(cell).trim();
                                if (!val.isEmpty()) {
                                    groups.add(val);
                                }
                            }
                        }
                    }
                }
                
                synchronized(loadedVoiceGroups) {
                    loadedVoiceGroups.clear();
                    loadedVoiceGroups.addAll(groups);
                }
                return null;
            }
        };
        
        task.setOnSucceeded(e -> {
            setButtonLoading(loadVoiceGroupButton, false);
            setButtonLoaded(loadVoiceGroupButton, true);
            autoClearLoaded(loadVoiceGroupButton, loadedTimeoutSeconds);
            
            updateVoiceGroupStats();
            refreshAllTables(); // Trigger re-validation/repainting
            showInfo("Loaded " + loadedVoiceGroups.size() + " voice groups.");
        });
        
        task.setOnFailed(e -> {
            setButtonLoading(loadVoiceGroupButton, false);
            showError("Failed to load voice groups: " + (task.getException() != null ? task.getException().getMessage() : "Unknown error"));
        });
        
        new Thread(task).start();
    }

    private void clearVoiceGroups() {
        synchronized(loadedVoiceGroups) {
            loadedVoiceGroups.clear();
        }
        updateVoiceGroupStats();
        refreshAllTables();
        
        // Clear the loaded state and checkmark from the Load Voice Group button
        if (loadVoiceGroupButton != null) {
            setButtonLoaded(loadVoiceGroupButton, false);
            setButtonLoading(loadVoiceGroupButton, false);
            loadVoiceGroupButton.setTooltip(null);
        }
        
        if (statusLabel != null) statusLabel.setText("Voice groups cleared.");
    }

    private void updateVoiceGroupStats() {
        if (voiceGroupStatsLabel != null) {
            if (loadedVoiceGroups.isEmpty()) {
                voiceGroupStatsLabel.setText("No groups loaded");
            } else {
                voiceGroupStatsLabel.setText(loadedVoiceGroups.size() + " groups loaded");
            }
        }
    }

    private Node createValidatedCellGraphic(String text) {
        if (text == null || text.isEmpty()) return null;
        
        // Only process text that contains VGroup/Group patterns, not just the word anywhere
        if (loadedVoiceGroups.isEmpty() || !VGROUP_KEYWORD_PATTERN.matcher(text).find()) {
            Label label = new Label(text);
            label.setWrapText(true);
            return label;
        }

        // Use a single TextFlow constrained to cell height
        TextFlow flow = new TextFlow();
        flow.setPadding(new Insets(2, 5, 2, 5)); // Small padding for readability
        flow.setLineSpacing(0);
        // Constrain the TextFlow to prevent cell expansion
        flow.setMaxHeight(24);
        flow.setPrefHeight(24);
        
        List<List<com.example.exceljson.util.VoiceGroupValidator.Segment>> allLineSegments;
        synchronized(loadedVoiceGroups) {
            allLineSegments = com.example.exceljson.util.VoiceGroupValidator.parseAndValidateMultiLine(text, loadedVoiceGroups);
        }

        // Flatten all segments into a single TextFlow with newline text nodes
        boolean firstLine = true;
        for (List<com.example.exceljson.util.VoiceGroupValidator.Segment> lineSegments : allLineSegments) {
            // Add newline between lines (but not before the first line)
            if (!firstLine) {
                Text newline = new Text("\n");
                newline.setFill(isDarkMode ? Color.WHITE : Color.BLACK);
                flow.getChildren().add(newline);
            }
            firstLine = false;
            
            for (com.example.exceljson.util.VoiceGroupValidator.Segment segment : lineSegments) {
                Text t = new Text(segment.text);
                if (segment.status == com.example.exceljson.util.VoiceGroupValidator.ValidationStatus.INVALID) {
                    t.setFill(Color.RED);
                } else {
                    // Keyword and valid groups use normal color
                    t.setFill(isDarkMode ? Color.WHITE : Color.BLACK);
                }
                flow.getChildren().add(t);
            }
        }
        
        return flow;
    }

    private void setupAutoComplete(TextInputControl input) {
        if (loadedVoiceGroups.isEmpty()) return;
        
        suggestionPopup = new ContextMenu();
        // Make the popup semi-transparent with dark/light mode support
        String bgColor = isDarkMode ? "rgba(50, 50, 50, 0.95)" : "rgba(255, 255, 255, 0.95)";
        suggestionPopup.setStyle("-fx-background-color: " + bgColor + "; -fx-background-radius: 5;");
        
        input.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null || newVal.length() < 2) {
                suggestionPopup.hide();
                return;
            }
            
            // Find the word being typed (last token)
            String text = newVal;
            
            // Regex to find the last partial word that looks like a group name
            // Handles "VGroup: <partial>", "Group: <partial>", or standalone words
            // Reduced minimum from 3 to 2 characters for better UX (e.g., "OB ")
            Pattern p = Pattern.compile("(?:^|[,;\\n\\s:]\\s*)([a-zA-Z0-9_\\-\\s]{2,})$");
            Matcher m = p.matcher(text);
            
            String partial = null;
            // Find the last match
            while (m.find()) {
                partial = m.group(1).trim();  // Trim to handle extra spaces
            }
            
            if (partial != null) {
                String search = partial.toLowerCase();
                List<String> matches;
                synchronized(loadedVoiceGroups) {
                    matches = loadedVoiceGroups.stream()
                        .filter(g -> g.toLowerCase().contains(search))
                        // Prioritize matches that start with the search term
                        .sorted((a, b) -> {
                            String aLower = a.toLowerCase();
                            String bLower = b.toLowerCase();
                            boolean aStarts = aLower.startsWith(search);
                            boolean bStarts = bLower.startsWith(search);
                            if (aStarts && !bStarts) return -1;
                            if (!aStarts && bStarts) return 1;
                            return a.compareTo(b);  // Alphabetical if both start or both don't start
                        })
                        .limit(5)  // TOP 5 matches as requested
                        .collect(Collectors.toList());
                }
                
                if (!matches.isEmpty()) {
                    populatePopup(matches, input, partial);
                    if (!suggestionPopup.isShowing()) {
                        suggestionPopup.show(input, Side.BOTTOM, 0, 0);
                    }
                } else {
                    suggestionPopup.hide();
                }
            } else {
                suggestionPopup.hide();
            }
        });
        
        // Hide on focus lost
        input.focusedProperty().addListener((obs, oldV, newV) -> {
            if (!newV) suggestionPopup.hide();
        });
    }

    private void populatePopup(List<String> matches, TextInputControl input, String partial) {
        suggestionPopup.getItems().clear();
        for (String match : matches) {
            MenuItem item = new MenuItem(match);
            item.setOnAction(e -> {
                // Replace partial with match
                String text = input.getText();
                int lastIndex = text.lastIndexOf(partial);
                if (lastIndex >= 0) {
                    String newText = text.substring(0, lastIndex) + match + text.substring(lastIndex + partial.length());
                    input.setText(newText);
                    input.positionCaret(lastIndex + match.length());
                }
            });
            suggestionPopup.getItems().add(item);
        }
    }

    private void setupValidatedColumn(TableColumn<ExcelParserV5.FlowRow, String> col, 
                                      Function<ExcelParserV5.FlowRow, String> getter, 
                                      BiConsumer<ExcelParserV5.FlowRow, String> setter,
                                      boolean useTextArea,
                                      java.util.function.Predicate<String> legacyValidator) {
        if (col == null) return;
        col.setCellValueFactory(d -> new SimpleStringProperty(safe(getter.apply(d.getValue()))));
        
        col.setCellFactory(column -> new TableCell<ExcelParserV5.FlowRow, String>() {
            private TextInputControl inputControl;
            
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText(null);
                    setGraphic(null);
                    setStyle("");
                } else {
                    // Voice Group Validation Logic
                    boolean hasVoiceGroups = !loadedVoiceGroups.isEmpty();
                    boolean hasKeywords = item != null && VGROUP_KEYWORD_PATTERN.matcher(item).find();
                    
                    if (hasVoiceGroups && hasKeywords) {
                        setText(null);
                        Node graphic = createValidatedCellGraphic(item);
                        // Wrap in a constrained container to prevent cell height expansion
                        if (graphic != null) {
                            StackPane container = new StackPane(graphic);
                            container.setMaxHeight(24); // Match default cell height
                            container.setPrefHeight(24);
                            setGraphic(container);
                        } else {
                            setGraphic(graphic);
                        }
                        setStyle(""); 
                    } else {
                        setText(item);
                        setGraphic(null);
                        if (legacyValidator != null && parser != null && !legacyValidator.test(item)) {
                             setStyle("-fx-background-color: #FFE4B5;");
                        } else {
                             setStyle("");
                        }
                    }
                }
            }
            
            @Override
            public void startEdit() {
                super.startEdit();
                if (inputControl == null) {
                    if (useTextArea) {
                        TextArea ta = new TextArea();
                        ta.setWrapText(true);
                        ta.setMinHeight(60);
                        ta.setPrefRowCount(3);
                        ta.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
                            if (event.getCode() == KeyCode.ENTER) {
                                if (event.isShiftDown()) {
                                    ta.insertText(ta.getCaretPosition(), "\n");
                                } else {
                                    commitEdit(ta.getText());
                                }
                                event.consume();
                            } else if (event.getCode() == KeyCode.ESCAPE) {
                                cancelEdit();
                                event.consume();
                            } else if (event.getCode() == KeyCode.TAB) {
                                commitEdit(ta.getText());
                                event.consume();
                            }
                        });
                        ta.focusedProperty().addListener((obs, was, is) -> {
                            if (!is && isEditing()) commitEdit(ta.getText());
                        });
                        inputControl = ta;
                    } else {
                        TextField tf = new TextField();
                        tf.setOnAction(e -> commitEdit(tf.getText()));
                        tf.focusedProperty().addListener((obs, was, is) -> {
                            if (!is && isEditing()) commitEdit(tf.getText());
                        });
                        inputControl = tf;
                    }
                    setupAutoComplete(inputControl);
                }
                inputControl.setText(getItem() == null ? "" : getItem());
                setText(null);
                setGraphic(inputControl);
                inputControl.selectAll();
                inputControl.requestFocus();
            }
            
            @Override
            public void cancelEdit() {
                super.cancelEdit();
                updateItem(getItem(), false);
            }
            
            @Override
            public void commitEdit(String newValue) {
                super.commitEdit(newValue);
                ExcelParserV5.FlowRow row = getTableRow().getItem();
                if (row != null) {
                    setter.accept(row, newValue);
                    if (getTableView() != null) getTableView().refresh();
                }
            }
        });
        col.setEditable(true);
        col.setOnEditCommit(ev -> {
            ExcelParserV5.FlowRow row = ev.getRowValue();
            setter.accept(row, ev.getNewValue());
            if (col.getTableView() != null) col.getTableView().refresh();
        });
    }
}
