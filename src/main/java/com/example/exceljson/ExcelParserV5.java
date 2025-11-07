package com.example.exceljson;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Excel → Engage JSON generator (streamlined).
 * - Skips blank/NA cells
 * - NurseCallsCondition block for conditions
 * - One destination per order, with merged groups
 * - Parameter attributes default to literal quotes unless Engage expects plain strings
 * - Deterministic JSON order + 2-space indentation
 * - NEW: writeExcel(File) to "Save As" the currently edited data
 */
public class ExcelParserV5 {

  // ---------- Row DTOs ----------
  public static final class UnitRow {
    public String facility = "";
    public String unitNames = "";
    public String nurseGroup = "";
    public String clinGroup = "";
    public String ordersGroup = "";
    public String noCareGroup = "";
    public String comments = "";
  }

  public static final class FlowRow {
    public boolean inScope = true; // Default to true
    public String type = ""; // NurseCalls or Clinicals
    public String configGroup = "";
    public String alarmName = "";
    public String sendingName = "";
    public String priorityRaw = "";
    public String deviceA = "";
    public String deviceB = "";
    public String ringtone = "";
    public String responseOptions = "";
    public String breakThroughDND = "";
    public String escalateAfter = "";
    public String ttlValue = "";
    public String enunciate = "";
    public String emdan = "";
    public String t1 = ""; public String r1 = "";
    public String t2 = ""; public String r2 = "";
    public String t3 = ""; public String r3 = "";
    public String t4 = ""; public String r4 = "";
    public String t5 = ""; public String r5 = "";
  }

  // ---------- Config / parsing helpers ----------
  public final List<UnitRow> units = new ArrayList<>();
  public final List<FlowRow> nurseCalls = new ArrayList<>();
  public final List<FlowRow> clinicals = new ArrayList<>();
  public final List<FlowRow> orders = new ArrayList<>();

  private final Map<String, List<Map<String, String>>> nurseGroupToUnits = new LinkedHashMap<>();
  private final Map<String, List<Map<String, String>>> clinicalGroupToUnits = new LinkedHashMap<>();
  private final Map<String, List<Map<String, String>>> ordersGroupToUnits = new LinkedHashMap<>();
  // Map from (facility, configGroup) -> No Caregiver Group value
  // Key format: "facilityName|configGroupType|configGroup" where configGroupType is "nurse", "clinical", or "orders"
  private final Map<String, String> noCaregiverByFacilityAndGroup = new LinkedHashMap<>();
  
  private int emdanMovedCount = 0;
  
  // Default interface reference names (editable via GUI)
  private String edgeReferenceName = "OutgoingWCTP";
  private String vcsReferenceName = "VMP";
  
  // Default interface flags (when Device A/B are blank)
  private boolean useDefaultEdge = false;
  private boolean useDefaultVmp = false;
  
  // Room filter values (optional filters for each flow type)
  private String roomFilterNursecall = "";
  private String roomFilterClinical = "";
  private String roomFilterOrders = "";

  public void setInterfaceReferences(String edgeRef, String vcsRef) {
    // Basic validation - ensure references are reasonable
    if (edgeRef != null && !edgeRef.isBlank() && edgeRef.length() <= 100) {
      this.edgeReferenceName = edgeRef.trim();
    }
    if (vcsRef != null && !vcsRef.isBlank() && vcsRef.length() <= 100) {
      this.vcsReferenceName = vcsRef.trim();
    }
  }
  
  public void setDefaultInterfaces(boolean defaultEdge, boolean defaultVmp) {
    this.useDefaultEdge = defaultEdge;
    this.useDefaultVmp = defaultVmp;
  }
  
  public void setRoomFilters(String nursecall, String clinical, String orders) {
    this.roomFilterNursecall = (nursecall != null && !nursecall.isBlank()) ? nursecall.trim() : "";
    this.roomFilterClinical = (clinical != null && !clinical.isBlank()) ? clinical.trim() : "";
    this.roomFilterOrders = (orders != null && !orders.isBlank()) ? orders.trim() : "";
  }

  private static final String SHEET_UNIT = "Unit Breakdown";
  private static final String SHEET_NURSE = "Nurse Call";
  private static final String SHEET_CLINICAL = "Patient Monitoring";
  private static final String SHEET_ORDERS = "Order";
  
  // Regex pattern to strip special characters from Custom Unit role names
  private static final String SPECIAL_CHARS_PATTERN = "[^a-zA-Z0-9\\s]";
  
  // Regex pattern to strip leading special characters after "Room" keyword
  private static final String LEADING_SPECIAL_CHARS_PATTERN = "^[^a-zA-Z0-9]+";

  // NurseCallsCondition (requested default)
  private static final List<Map<String, Object>> NURSE_CONDITIONS;
  static {
    Map<String, Object> f1 = new LinkedHashMap<>();
    f1.put("attributePath", "bed");
    f1.put("operator", "not_null");
    Map<String, Object> f2 = new LinkedHashMap<>();
    f2.put("attributePath", "to.type");
    f2.put("operator", "not_equal");
    f2.put("value", "TargetGroups");
    Map<String, Object> cond = new LinkedHashMap<>();
    cond.put("filters", List.of(f1, f2));
    cond.put("name", "NurseCallsCondition");
    NURSE_CONDITIONS = List.of(cond);
  }

  // ---------- Load ----------
  public void load(File excelFile) throws Exception {
    Objects.requireNonNull(excelFile, "excelFile");
    clear();
    try (FileInputStream fis = new FileInputStream(excelFile);
         Workbook wb = new XSSFWorkbook(fis)) {
      parseUnitBreakdown(wb);
      parseFlowSheet(wb, SHEET_NURSE, true, false);
      parseFlowSheet(wb, SHEET_CLINICAL, false, false);
      parseFlowSheet(wb, SHEET_ORDERS, false, true);
    }
  }

  public String getLoadSummary() {
    return String.format(Locale.ROOT,
      "✅ Excel Load Complete%n%n" +
      "Loaded:%n" +
      "  • %d Unit Breakdown rows%n" +
      "  • %d Nurse Call rows%n" +
      "  • %d Patient Monitoring rows%n" +
      "  • %d Orders rows%n%n" +
      "Linked:%n" +
      "  • %d Configuration Groups (Nurse)%n" +
      "  • %d Configuration Groups (Clinical)%n" +
      "  • %d Configuration Groups (Orders)",
      units.size(), nurseCalls.size(), clinicals.size(), orders.size(),
      nurseGroupToUnits.size(), clinicalGroupToUnits.size(), ordersGroupToUnits.size());
  }
  
  public int getEmdanMovedCount() {
    return emdanMovedCount;
  }

  private void clear() {
    units.clear();
    nurseCalls.clear();
    clinicals.clear();
    orders.clear();
    nurseGroupToUnits.clear();
    clinicalGroupToUnits.clear();
    ordersGroupToUnits.clear();
    noCaregiverByFacilityAndGroup.clear();
    emdanMovedCount = 0;
  }

  // ---------- Parse: Unit Breakdown ----------
  private void parseUnitBreakdown(Workbook wb) {
    Sheet sh = findSheet(wb, SHEET_UNIT);
    if (sh == null) return;

    Row header = findHeaderRow(sh);
    Map<String,Integer> hm = headerMap(header);

    int start = firstDataRow(sh, header);
    int cFacility   = getCol(hm, "Facility");
    int cUnitName   = getCol(hm, "Common Unit Name");
    int cNurseGroup = getCol(hm, "Nurse Call", "Configuration Group", "Nurse call");
    int cClinGroup  = getCol(hm, "Patient Monitoring", "Configuration Group", "Patient monitoring");
    int cOrdersGroup = getCol(hm, "Orders", "Configuration Group", "Order", "Med Order", "STAT MED");
    int cNoCare     = getCol(hm, "No Caregiver Alert Number or Group", "No Caregiver Group");
    int cComments   = getCol(hm, "Comments");

    for (int r = start; r <= sh.getLastRowNum(); r++) {
      Row row = sh.getRow(r);
      if (row == null) continue;

      String facility   = getCell(row, cFacility);
      String unitNames  = getCell(row, cUnitName);
      String nurseGroup = getCell(row, cNurseGroup);
      String clinGroup  = getCell(row, cClinGroup);
      String ordersGroup = getCell(row, cOrdersGroup);
      String noCare     = stripVGroup(getCell(row, cNoCare));
      String comments   = getCell(row, cComments);

      if (isBlank(facility) && isBlank(unitNames)) continue;

      UnitRow u = new UnitRow();
      u.facility = facility;
      u.unitNames = unitNames;
      u.nurseGroup = nurseGroup;
      u.clinGroup = clinGroup;
      u.ordersGroup = ordersGroup;
      u.ordersGroup = ordersGroup;
      u.noCareGroup = noCare;
      u.comments = comments;
      units.add(u);

      List<String> list = splitUnits(unitNames);
      if (!isBlank(nurseGroup)) {
        for (String name : list) {
          nurseGroupToUnits.computeIfAbsent(nurseGroup, k -> new ArrayList<>())
            .add(Map.of("facilityName", facility, "name", name));
        }
        // Store No Caregiver Group for this (facility, nurseGroup) pair
        if (!isBlank(facility) && !isBlank(noCare)) {
          String key = buildNoCaregiverKey(facility, "nurse", nurseGroup);
          noCaregiverByFacilityAndGroup.put(key, noCare);
        }
      }
      if (!isBlank(clinGroup)) {
        for (String name : list) {
          clinicalGroupToUnits.computeIfAbsent(clinGroup, k -> new ArrayList<>())
            .add(Map.of("facilityName", facility, "name", name));
        }
        // Store No Caregiver Group for this (facility, clinGroup) pair
        if (!isBlank(facility) && !isBlank(noCare)) {
          String key = buildNoCaregiverKey(facility, "clinical", clinGroup);
          noCaregiverByFacilityAndGroup.put(key, noCare);
        }
      }
      if (!isBlank(ordersGroup)) {
        for (String name : list) {
          ordersGroupToUnits.computeIfAbsent(ordersGroup, k -> new ArrayList<>())
            .add(Map.of("facilityName", facility, "name", name));
        }
        // Store No Caregiver Group for this (facility, ordersGroup) pair
        if (!isBlank(facility) && !isBlank(noCare)) {
          String key = buildNoCaregiverKey(facility, "orders", ordersGroup);
          noCaregiverByFacilityAndGroup.put(key, noCare);
        }
      }
    }
  }

  // ---------- Rebuild unit maps from edited units list ----------
  public void rebuildUnitMaps() {
    nurseGroupToUnits.clear();
    clinicalGroupToUnits.clear();
    ordersGroupToUnits.clear();
    noCaregiverByFacilityAndGroup.clear();

    for (UnitRow u : units) {
      String facility = u.facility;
      String unitNames = u.unitNames;
      String nurseGroup = u.nurseGroup;
      String clinGroup = u.clinGroup;
      String ordersGroup = u.ordersGroup;
      String noCare = u.noCareGroup;

      List<String> list = splitUnits(unitNames);
      if (!isBlank(nurseGroup)) {
        for (String name : list) {
          nurseGroupToUnits.computeIfAbsent(nurseGroup, k -> new ArrayList<>())
            .add(Map.of("facilityName", facility, "name", name));
        }
        // Store No Caregiver Group for this (facility, nurseGroup) pair
        if (!isBlank(facility) && !isBlank(noCare)) {
          String key = buildNoCaregiverKey(facility, "nurse", nurseGroup);
          noCaregiverByFacilityAndGroup.put(key, noCare);
        }
      }
      if (!isBlank(clinGroup)) {
        for (String name : list) {
          clinicalGroupToUnits.computeIfAbsent(clinGroup, k -> new ArrayList<>())
            .add(Map.of("facilityName", facility, "name", name));
        }
        // Store No Caregiver Group for this (facility, clinGroup) pair
        if (!isBlank(facility) && !isBlank(noCare)) {
          String key = buildNoCaregiverKey(facility, "clinical", clinGroup);
          noCaregiverByFacilityAndGroup.put(key, noCare);
        }
      }
      if (!isBlank(ordersGroup)) {
        for (String name : list) {
          ordersGroupToUnits.computeIfAbsent(ordersGroup, k -> new ArrayList<>())
            .add(Map.of("facilityName", facility, "name", name));
        }
        // Store No Caregiver Group for this (facility, ordersGroup) pair
        if (!isBlank(facility) && !isBlank(noCare)) {
          String key = buildNoCaregiverKey(facility, "orders", ordersGroup);
          noCaregiverByFacilityAndGroup.put(key, noCare);
        }
      }
    }
  }

  // ---------- Parse: Flow Sheets ----------
  private void parseFlowSheet(Workbook wb, String sheetName, boolean nurseSide, boolean ordersType) {
    // For Orders sheets, try exact matches first, then any sheet containing "Order"
    Sheet sh = null;
    if (ordersType) {
      // First try exact matches for known variations
      sh = findSheetCaseInsensitive(wb, "Order", "Med Order", "STAT MED");
      // If not found, try finding any sheet containing "Order" in its name
      if (sh == null) {
        sh = findSheetContaining(wb, "Order");
      }
    } else {
      sh = findSheet(wb, sheetName);
    }
    if (sh == null) return;

    Row header = findHeaderRow(sh);
    Map<String,Integer> hm = headerMap(header);
    int start = firstDataRow(sh, header);

    int cInScope = getCol(hm, "In scope", "In Scope");
    int cCfg     = getCol(hm, "Configuration Group");
    int cAlarm   = getCol(hm, "Common Alert or Alarm Name", "Alarm Name");
    int cSend    = getCol(hm, "Sending System Alert Name", "Sending System Alarm Name");
    int cPriority= getCol(hm, "Priority");
    int cDevice  = getCol(hm, "Device - A", "Device");
    int cDeviceB = getCol(hm, "Device - B");
    int cRing    = getCol(hm, "Ringtone Device - A", "Ringtone");
    int cResp    = getCol(hm, "Response Options", "Response Option");
    int cBreakDND= getCol(hm, "Break Through DND");
    int cEscalateAfter = getCol(hm, "Engage 6.6+: Escalate after all declines or 1 decline");
    int cTTL = getCol(hm, "Engage/Edge Display Time (Time to Live) (Device - A)");
    
    // Find all columns that contain "Genie Enunciation" to support multiple column variants
    List<Integer> enunciateColumns = new ArrayList<>();
    String searchTerm = normalize("Genie Enunciation");
    for (Map.Entry<String, Integer> entry : hm.entrySet()) {
      if (entry.getKey().contains(searchTerm)) {
        enunciateColumns.add(entry.getValue());
      }
    }
    // Also try exact column name matching as fallback
    int exactCol = getCol(hm, "Genie Enunciation");
    if (exactCol != -1 && !enunciateColumns.contains(exactCol)) {
      enunciateColumns.add(exactCol);
    }
    
    int cEmdan = getColLoose(hm, "emdan");
    
    int cT1 = getCol(hm, "Time to 1st Recipient", "Delay to 1st", "Time to 1st Recipient (after alarm triggers)");
    int cR1 = getCol(hm, "1st Recipient", "First Recipient", "1st recipients");
    int cT2 = getCol(hm, "Time to 2nd Recipient", "Delay to 2nd");
    int cR2 = getCol(hm, "2nd Recipient", "Second Recipient");
    int cT3 = getCol(hm, "Time to 3rd Recipient", "Delay to 3rd");
    int cR3 = getCol(hm, "3rd Recipient", "Third Recipient");
    int cT4 = getCol(hm, "Time to 4th Recipient");
    int cR4 = getCol(hm, "4th Recipient");
    int cT5 = getCol(hm, "Time to 5th Recipient");
    int cR5 = getCol(hm, "5th Recipient");

    for (int r = start; r <= sh.getLastRowNum(); r++) {
      Row row = sh.getRow(r);
      if (row == null) continue;

      FlowRow f = new FlowRow();
      // Set type based on sheet type
      if (ordersType) {
        f.type = "Orders";
      } else {
        f.type = nurseSide ? "NurseCalls" : "Clinicals";
      }
      
      // Parse "In scope" column - default to true if column not present or value is blank
      String inScopeStr = getCell(row, cInScope);
      if (cInScope == -1 || isBlank(inScopeStr)) {
        // Default to true if Config Group is present
        f.inScope = !isBlank(getCell(row, cCfg));
      } else {
        // Parse checkbox value: TRUE, YES, Y, X, or checked symbol
        f.inScope = parseBooleanValue(inScopeStr);
      }
      
      f.configGroup = getCell(row, cCfg);
      f.alarmName   = getCell(row, cAlarm);
      f.sendingName = getCell(row, cSend);
      f.priorityRaw = getCell(row, cPriority);
      f.deviceA     = getCell(row, cDevice);
      f.deviceB     = getCell(row, cDeviceB);
      f.ringtone    = getCell(row, cRing);
      f.responseOptions = getCell(row, cResp);
      f.breakThroughDND = getCell(row, cBreakDND);
      f.escalateAfter = getCell(row, cEscalateAfter);
      f.ttlValue = getCell(row, cTTL);
      
      // Extract enunciate from multiple possible columns, use first non-empty value
      f.enunciate = getFirstNonEmptyValue(row, enunciateColumns);
      
      // Extract EMDAN column
      f.emdan = getCell(row, cEmdan);
      
      f.t1 = getCell(row, cT1); f.r1 = getCell(row, cR1);
      f.t2 = getCell(row, cT2); f.r2 = getCell(row, cR2);
      f.t3 = getCell(row, cT3); f.r3 = getCell(row, cR3);
      f.t4 = getCell(row, cT4); f.r4 = getCell(row, cR4);
      f.t5 = getCell(row, cT5); f.r5 = getCell(row, cR5);

      if (isBlank(f.alarmName) && isBlank(f.sendingName)) continue;

      // EMDAN Reclassification: if reading from Nurse Call sheet and EMDAN is Y/Yes, move to Clinicals
      if (nurseSide && isEmdanCompliant(f.emdan)) {
        f.type = "Clinicals";
        clinicals.add(f);
        emdanMovedCount++;
        
        // Resolve facility from configuration group for better logging
        // Use nurseSide=true because the alarm originated from Nurse Call sheet (source mapping)
        String facility = resolveFacilityFromConfig(f.configGroup, true);
        String alarmDisplay = nvl(f.alarmName, f.sendingName);
        String priorityDisplay = isBlank(f.priorityRaw) ? "default" : f.priorityRaw;
        
        // Enhanced logging with facility, priority, and configuration details
        if (!isBlank(facility)) {
          System.out.println("✅ EMDAN: Moved '" + alarmDisplay + "' from Nurse Call to Clinicals " +
                           "[Facility: " + facility + ", Priority: " + priorityDisplay + ", Config: " + f.configGroup + "]");
        } else {
          // Fallback note when facility cannot be resolved
          System.out.println("✅ EMDAN: Moved '" + alarmDisplay + "' from Nurse Call to Clinicals " +
                           "[Priority: " + priorityDisplay + ", Config: " + f.configGroup + "] " +
                           "(Note: Facility not resolved - verify Unit Breakdown mapping)");
        }
      } else if (ordersType) {
        orders.add(f);
      } else if (nurseSide) {
        nurseCalls.add(f);
      } else {
        clinicals.add(f);
      }
    }
  }

  // ---------- Public JSON builders ----------
  public Map<String,Object> buildNurseCallsJson() {
    return buildNurseCallsJson(false);
  }
  public Map<String,Object> buildNurseCallsJson(boolean mergeIdenticalFlows) {
    return buildJson(nurseCalls, nurseGroupToUnits, "NurseCalls", mergeIdenticalFlows);
  }
  public Map<String,Object> buildClinicalsJson() {
    return buildClinicalsJson(false);
  }
  public Map<String,Object> buildClinicalsJson(boolean mergeIdenticalFlows) {
    return buildJson(clinicals, clinicalGroupToUnits, "Clinicals", mergeIdenticalFlows);
  }
  public Map<String,Object> buildOrdersJson() {
    return buildOrdersJson(false);
  }
  public Map<String,Object> buildOrdersJson(boolean mergeIdenticalFlows) {
    return buildJson(orders, ordersGroupToUnits, "Orders", mergeIdenticalFlows);
  }

  // ---------- Build JSON core ----------
  private Map<String,Object> buildJson(List<FlowRow> rows,
                                       Map<String,List<Map<String,String>>> groupToUnits,
                                       String flowType,
                                       boolean mergeIdenticalFlows) {
    boolean nurseSide = "NurseCalls".equals(flowType);
    boolean ordersType = "Orders".equals(flowType);
    
    Map<String,Object> root = new LinkedHashMap<>();
    root.put("version", "1.1.0");
    root.put("alarmAlertDefinitions", buildAlarmDefs(rows, flowType));

    List<Map<String,Object>> flows;
    if (mergeIdenticalFlows) {
      flows = buildFlowsMerged(rows, groupToUnits, flowType);
    } else {
      flows = buildFlowsNormal(rows, groupToUnits, flowType);
    }
    root.put("deliveryFlows", flows);
    return root;
  }
  
  /**
   * Resolve unit references for a configuration group.
   * For clinical flows, checks both clinical and nurse group maps to support EMDAN-moved alarms.
   * 
   * @param configGroup The configuration group to look up
   * @param primaryMap The primary map to check (clinicalGroupToUnits or nurseGroupToUnits)
   * @param nurseSide true if building nurse calls, false if building clinicals
   * @return List of unit references with facility names and unit names, or empty list if the
   *         configuration group is blank or not found in any map
   */
  private List<Map<String,String>> resolveUnitRefs(String configGroup,
                                                   Map<String,List<Map<String,String>>> primaryMap,
                                                   boolean nurseSide) {
    if (isBlank(configGroup)) return List.of();
    
    // First check the primary map
    List<Map<String,String>> units = primaryMap.getOrDefault(configGroup, List.of());
    
    // For clinicals, also check nurse groups map (for EMDAN-moved alarms)
    if (!nurseSide && units.isEmpty()) {
      units = nurseGroupToUnits.getOrDefault(configGroup, List.of());
    }
    
    return units;
  }

  // ---------- Build flows (normal mode) - one flow per row ----------
  private List<Map<String,Object>> buildFlowsNormal(List<FlowRow> rows,
                                                     Map<String,List<Map<String,String>>> groupToUnits,
                                                     String flowType) {
    boolean nurseSide = "NurseCalls".equals(flowType);
    boolean ordersType = "Orders".equals(flowType);
    
    List<Map<String,Object>> flows = new ArrayList<>();
    for (FlowRow r : rows) {
      if (isBlank(r.configGroup) && isBlank(r.alarmName) && isBlank(r.sendingName)) continue;
      
      // Skip rows that are not in scope
      if (!r.inScope) continue;

      List<Map<String,String>> unitRefs = resolveUnitRefs(r.configGroup, groupToUnits, nurseSide);
      String mappedPriority = mapPrioritySafe(r.priorityRaw, r.deviceA);
      
      // Build destinations and conditions
      DestinationsAndConditions dac = buildDestinationsAndConditions(r, unitRefs, flowType, mappedPriority);

      Map<String,Object> flow = new LinkedHashMap<>();
      flow.put("alarmsAlerts", List.of(nvl(r.alarmName, r.sendingName)));
      
      // Build conditions with room filter if applicable
      List<Map<String,Object>> flowConditions = new ArrayList<>();
      
      if (!dac.conditions.isEmpty()) {
        flowConditions.addAll(dac.conditions);
      } else if (nurseSide) {
        flowConditions.addAll(nurseConditions());
      } else if (ordersType) {
        // Orders need a global condition
        Map<String, Object> filter = new LinkedHashMap<>();
        filter.put("attributePath", "patient.current_place");
        filter.put("operator", "not_null");
        
        Map<String, Object> globalCond = new LinkedHashMap<>();
        globalCond.put("filters", List.of(filter));
        globalCond.put("name", "Global Condition");
        flowConditions.add(globalCond);
      }
      
      // Add room filter condition based on flow type
      Map<String,Object> roomFilterCond = null;
      if (nurseSide && !roomFilterNursecall.isEmpty()) {
        roomFilterCond = buildRoomFilterCondition(roomFilterNursecall);
      } else if ("Clinicals".equals(flowType) && !roomFilterClinical.isEmpty()) {
        roomFilterCond = buildRoomFilterCondition(roomFilterClinical);
      } else if (ordersType && !roomFilterOrders.isEmpty()) {
        roomFilterCond = buildOrdersRoomFilterCondition(roomFilterOrders);
      }
      
      if (roomFilterCond != null) {
        flowConditions.add(roomFilterCond);
      }
      
      flow.put("conditions", flowConditions);
      
      flow.put("destinations", dac.destinations);
      flow.put("interfaces", buildInterfacesForDevice(r.deviceA, r.deviceB));
      flow.put("name", buildFlowName(flowType, mappedPriority, r, unitRefs));
      flow.put("parameterAttributes", buildParamAttributesQuoted(r, flowType, mappedPriority));
      flow.put("priority", mappedPriority.isEmpty() ? "normal" : mappedPriority);
      flow.put("status", "Active");
      if (!unitRefs.isEmpty()) flow.put("units", unitRefs);
      flows.add(flow);
    }
    return flows;
  }

  // ---------- Build flows (merge mode) - merge flows with identical delivery parameters ----------
  private List<Map<String,Object>> buildFlowsMerged(List<FlowRow> rows,
                                                     Map<String,List<Map<String,String>>> groupToUnits,
                                                     String flowType) {
    boolean nurseSide = "NurseCalls".equals(flowType);
    
    // Group rows by their "merge key" (identical delivery parameters)
    Map<String, List<FlowRow>> groupedByMergeKey = new LinkedHashMap<>();
    
    for (FlowRow r : rows) {
      if (isBlank(r.configGroup) && isBlank(r.alarmName) && isBlank(r.sendingName)) continue;
      
      // Skip rows that are not in scope
      if (!r.inScope) continue;
      
      String mergeKey = buildMergeKey(r, groupToUnits, flowType);
      groupedByMergeKey.computeIfAbsent(mergeKey, k -> new ArrayList<>()).add(r);
    }

    // Build one flow per merge group
    List<Map<String,Object>> flows = new ArrayList<>();
    for (List<FlowRow> group : groupedByMergeKey.values()) {
      if (group.isEmpty()) continue;
      
      // Use the first row as the template
      FlowRow template = group.get(0);
      List<Map<String,String>> unitRefs = resolveUnitRefs(template.configGroup, groupToUnits, nurseSide);
      String mappedPriority = mapPrioritySafe(template.priorityRaw, template.deviceA);

      // Collect all alarm names from the group
      List<String> alarmNames = group.stream()
        .map(r -> nvl(r.alarmName, r.sendingName))
        .distinct()
        .collect(Collectors.toList());
      
      // Build destinations and conditions
      DestinationsAndConditions dac = buildDestinationsAndConditions(template, unitRefs, flowType, mappedPriority);

      Map<String,Object> flow = new LinkedHashMap<>();
      flow.put("alarmsAlerts", alarmNames);
      
      // Build conditions with room filter if applicable
      boolean ordersType = "Orders".equals(flowType);
      List<Map<String,Object>> flowConditions = new ArrayList<>();
      
      if (!dac.conditions.isEmpty()) {
        flowConditions.addAll(dac.conditions);
      } else if (nurseSide) {
        flowConditions.addAll(nurseConditions());
      } else if (ordersType) {
        // Orders need a global condition
        Map<String, Object> filter = new LinkedHashMap<>();
        filter.put("attributePath", "patient.current_place");
        filter.put("operator", "not_null");
        
        Map<String, Object> globalCond = new LinkedHashMap<>();
        globalCond.put("filters", List.of(filter));
        globalCond.put("name", "Global Condition");
        flowConditions.add(globalCond);
      }
      
      // Add room filter condition based on flow type
      Map<String,Object> roomFilterCond = null;
      if (nurseSide && !roomFilterNursecall.isEmpty()) {
        roomFilterCond = buildRoomFilterCondition(roomFilterNursecall);
      } else if ("Clinicals".equals(flowType) && !roomFilterClinical.isEmpty()) {
        roomFilterCond = buildRoomFilterCondition(roomFilterClinical);
      } else if (ordersType && !roomFilterOrders.isEmpty()) {
        roomFilterCond = buildOrdersRoomFilterCondition(roomFilterOrders);
      }
      
      if (roomFilterCond != null) {
        flowConditions.add(roomFilterCond);
      }
      
      flow.put("conditions", flowConditions);
      
      flow.put("destinations", dac.destinations);
      flow.put("interfaces", buildInterfacesForDevice(template.deviceA, template.deviceB));
      flow.put("name", buildFlowNameMerged(flowType, mappedPriority, alarmNames, template, unitRefs));
      flow.put("parameterAttributes", buildParamAttributesQuoted(template, flowType, mappedPriority));
      flow.put("priority", mappedPriority.isEmpty() ? "normal" : mappedPriority);
      flow.put("status", "Active");
      if (!unitRefs.isEmpty()) flow.put("units", unitRefs);
      flows.add(flow);
    }
    return flows;
  }

  // ---------- Build merge key for grouping flows with identical delivery parameters ----------
  private String buildMergeKey(FlowRow r, Map<String,List<Map<String,String>>> groupToUnits, String flowType) {
    boolean nurseSide = "NurseCalls".equals(flowType);
    List<Map<String,String>> unitRefs = resolveUnitRefs(r.configGroup, groupToUnits, nurseSide);
    String mappedPriority = mapPrioritySafe(r.priorityRaw, r.deviceA);
    
    // Determine the config group type for No Caregiver Group lookup
    String configGroupType = getConfigGroupType(flowType);
    
    // Build a key from: priority, device, ringtone, recipients (r1-r5), timing (t1-t5), units, noCareGroup
    StringBuilder key = new StringBuilder();
    key.append("priority=").append(mappedPriority).append("|");
    key.append("device=").append(nvl(r.deviceA, "")).append("|");
    key.append("ringtone=").append(nvl(r.ringtone, "")).append("|");
    key.append("responseOptions=").append(nvl(r.responseOptions, "")).append("|");
    key.append("t1=").append(nvl(r.t1, "")).append("|");
    key.append("r1=").append(nvl(r.r1, "")).append("|");
    key.append("t2=").append(nvl(r.t2, "")).append("|");
    key.append("r2=").append(nvl(r.r2, "")).append("|");
    key.append("t3=").append(nvl(r.t3, "")).append("|");
    key.append("r3=").append(nvl(r.r3, "")).append("|");
    key.append("t4=").append(nvl(r.t4, "")).append("|");
    key.append("r4=").append(nvl(r.r4, "")).append("|");
    key.append("t5=").append(nvl(r.t5, "")).append("|");
    key.append("r5=").append(nvl(r.r5, "")).append("|");
    key.append("configGroup=").append(nvl(r.configGroup, "")).append("|");
    
    // Add units to the key
    String unitsKey = unitRefs.stream()
      .map(u -> u.get("facilityName") + ":" + u.get("name"))
      .sorted()
      .collect(Collectors.joining(","));
    key.append("units=").append(unitsKey).append("|");
    
    // Add No Caregiver Group to the key
    // For each facility, look up the No Caregiver Group specific to this (facility, configGroup) combination
    String noCareKey = unitRefs.stream()
      .map(u -> u.get("facilityName"))
      .distinct()
      .sorted()
      .map(facility -> {
        String lookupKey = buildNoCaregiverKey(facility, configGroupType, r.configGroup);
        String noCareValue = noCaregiverByFacilityAndGroup.getOrDefault(lookupKey, "");
        return facility + ":" + noCareValue;
      })
      .collect(Collectors.joining(","));
    key.append("noCareGroup=").append(noCareKey);
    
    return key.toString();
  }

  // ---------- Build flow name for merged flows ----------
  private String buildFlowNameMerged(String flowType,
                                     String mappedPriority,
                                     List<String> alarmNames,
                                     FlowRow template,
                                     List<Map<String,String>> unitRefs) {
    String group = template.configGroup == null ? "" : template.configGroup.trim();
    String facility = unitRefs.isEmpty() ? "" : unitRefs.get(0).getOrDefault("facilityName", "");
    List<String> unitNames = unitRefs.stream()
      .map(u -> u.getOrDefault("name",""))
      .filter(s -> !isBlank(s))
      .distinct().collect(Collectors.toList());

    List<String> parts = new ArrayList<>();
    String prefix = switch(flowType) {
      case "NurseCalls" -> "SEND NURSECALL";
      case "Clinicals" -> "SEND CLINICAL";
      case "Orders" -> "SEND ORDER";
      default -> "SEND " + flowType.toUpperCase(Locale.ROOT);
    };
    parts.add(prefix);
    if (!isBlank(mappedPriority)) parts.add(mappedPriority.toUpperCase(Locale.ROOT));
    
    // List all alarm names separated by " / "
    if (!alarmNames.isEmpty()) {
      parts.add(String.join(" / ", alarmNames));
    }
    
    if (!isBlank(group)) parts.add(group);
    if (!unitNames.isEmpty()) parts.add(String.join(" / ", unitNames));
    else if (!isBlank(facility)) parts.add(facility);
    return String.join(" | ", parts);
  }

  private List<Map<String,Object>> buildAlarmDefs(List<FlowRow> rows, String flowType) {
    Map<String, Map<String,Object>> byKey = new LinkedHashMap<>();
    for (FlowRow r : rows) {
      // Skip rows that are not in scope
      if (!r.inScope) continue;
      
      String name = nvl(r.alarmName, r.sendingName);
      if (isBlank(name)) continue;
      String key = name + "|" + flowType;
      if (byKey.containsKey(key)) continue;

      Map<String,Object> def = new LinkedHashMap<>();
      def.put("name", name);
      def.put("type", flowType);

      Map<String,String> val = new LinkedHashMap<>();
      val.put("category", "");
      val.put("value", name);
      def.put("values", List.of(val));
      byKey.put(key, def);
    }
    return new ArrayList<>(byKey.values());
  }

  // ---------- Destinations: one per order, merge groups ----------
  
  /**
   * Result of building destinations, potentially with custom conditions.
   */
  private static final class DestinationsAndConditions {
    final List<Map<String, Object>> destinations;
    final List<Map<String, Object>> conditions;
    
    DestinationsAndConditions(List<Map<String, Object>> destinations, List<Map<String, Object>> conditions) {
      this.destinations = destinations;
      this.conditions = conditions;
    }
  }
  
  private DestinationsAndConditions buildDestinationsAndConditions(FlowRow r,
                                                                    List<Map<String,String>> unitRefs,
                                                                    String flowType,
                                                                    String mappedPriority) {
    String facility = unitRefs.isEmpty() ? "" : unitRefs.get(0).getOrDefault("facilityName", "");
    boolean nurseSide = "NurseCalls".equals(flowType);
    
    // Determine presenceConfig based on Break Through DND value
    String presenceConfig = getPresenceConfigFromBreakThrough(r.breakThroughDND);
    
    // Determine interface reference name based on device A/B
    String interfaceRef = determineInterfaceReferenceName(r.deviceA, r.deviceB);
    
    List<DestinationWithConditions> results = new ArrayList<>();
    addOrderWithConditions(results, facility, 0, r.t1, r.r1, presenceConfig, mappedPriority, interfaceRef, flowType);
    addOrderWithConditions(results, facility, 1, r.t2, r.r2, presenceConfig, mappedPriority, interfaceRef, flowType);
    addOrderWithConditions(results, facility, 2, r.t3, r.r3, presenceConfig, mappedPriority, interfaceRef, flowType);
    addOrderWithConditions(results, facility, 3, r.t4, r.r4, presenceConfig, mappedPriority, interfaceRef, flowType);
    addOrderWithConditions(results, facility, 4, r.t5, r.r5, presenceConfig, mappedPriority, interfaceRef, flowType);
    
    List<Map<String,Object>> destinations = new ArrayList<>();
    List<Map<String,Object>> conditions = new ArrayList<>();
    
    for (DestinationWithConditions dwc : results) {
      destinations.add(dwc.destination);
      if (dwc.condition != null) {
        conditions.add(dwc.condition);
      }
    }

    // Only add NoDeliveries for Clinicals, not for Orders
    boolean ordersType = "Orders".equals(flowType);
    if (!nurseSide && !ordersType && !isBlank(facility)) {
      // Determine the config group type for No Caregiver Group lookup
      String configGroupType = getConfigGroupType(flowType);
      String lookupKey = buildNoCaregiverKey(facility, configGroupType, r.configGroup);
      String noCare = noCaregiverByFacilityAndGroup.getOrDefault(lookupKey, "");
      if (!isBlank(noCare)) {
        Map<String,Object> d = new LinkedHashMap<>();
        d.put("order", destinations.size());
        d.put("delayTime", 0);
        d.put("destinationType", "NoDeliveries");
        d.put("users", List.of());
        d.put("functionalRoles", List.of());
        d.put("groups", List.of(Map.of("facilityName", facility, "name", noCare)));
        d.put("presenceConfig", presenceConfig);
        d.put("recipientType", "group");
        destinations.add(d);
      }
    }
    
    return new DestinationsAndConditions(destinations, conditions);
  }
  
  /**
   * Determine the primary interface reference name based on device A/B.
   * Returns the Edge reference if Edge is detected, otherwise VMP.
   */
  private String determineInterfaceReferenceName(String deviceA, String deviceB) {
    boolean hasEdgeA = containsEdge(deviceA);
    boolean hasEdgeB = containsEdge(deviceB);
    boolean hasVcsA = containsVcs(deviceA);
    boolean hasVcsB = containsVcs(deviceB);
    
    // Prefer Edge if present
    if (hasEdgeA || hasEdgeB) {
      return edgeReferenceName;
    }
    
    // Otherwise use VCS
    if (hasVcsA || hasVcsB) {
      return vcsReferenceName;
    }
    
    // Default to Edge if no device specified and default is set
    if (useDefaultEdge) {
      return edgeReferenceName;
    }
    
    if (useDefaultVmp) {
      return vcsReferenceName;
    }
    
    // Final fallback to Edge
    return edgeReferenceName;
  }
  
  private List<Map<String,Object>> buildDestinationsMerged(FlowRow r,
                                                           List<Map<String,String>> unitRefs,
                                                           String flowType) {
    String mappedPriority = mapPrioritySafe(r.priorityRaw, r.deviceA);
    DestinationsAndConditions dac = buildDestinationsAndConditions(r, unitRefs, flowType, mappedPriority);
    return dac.destinations;
  }

  /**
   * Dynamically selects the interface block based on the Device-A and Device-B columns.
   * Uses the editable reference names provided from the GUI.
   * If both devices contain "Edge" or "VCS", returns both OutgoingWCTP and VMP interfaces.
   * If devices are blank/empty, uses default interface checkboxes if configured.
   */
  private List<Map<String, Object>> buildInterfacesForDevice(String deviceA, String deviceB) {
    boolean hasEdgeA = containsEdge(deviceA);
    boolean hasVcsA = containsVcs(deviceA);
    boolean hasEdgeB = containsEdge(deviceB);
    boolean hasVcsB = containsVcs(deviceB);

    // If both devices have Edge or VCS, combine both interfaces
    if ((hasEdgeA || hasEdgeB) && (hasVcsA || hasVcsB)) {
      List<Map<String, Object>> interfaces = new ArrayList<>();
      
      Map<String, Object> edgeIface = new LinkedHashMap<>();
      edgeIface.put("componentName", "OutgoingWCTP");
      edgeIface.put("referenceName", edgeReferenceName);
      interfaces.add(edgeIface);
      
      Map<String, Object> vcsIface = new LinkedHashMap<>();
      vcsIface.put("componentName", "VMP");
      vcsIface.put("referenceName", vcsReferenceName);
      interfaces.add(vcsIface);
      
      return interfaces;
    }

    // Single device logic - check deviceA first, then deviceB
    if (hasEdgeA || hasEdgeB) {
      Map<String, Object> iface = new LinkedHashMap<>();
      iface.put("componentName", "OutgoingWCTP");
      iface.put("referenceName", edgeReferenceName);
      return List.of(iface);
    }

    if (hasVcsA || hasVcsB) {
      Map<String, Object> iface = new LinkedHashMap<>();
      iface.put("componentName", "VMP");
      iface.put("referenceName", vcsReferenceName);
      return List.of(iface);
    }

    // Device A and B cannot determine interface - use default checkboxes if set
    if (shouldApplyDefaultInterfaces(deviceA, deviceB, hasEdgeA, hasEdgeB, hasVcsA, hasVcsB)) {
      // If both default checkboxes are selected, return both interfaces
      if (useDefaultEdge && useDefaultVmp) {
        List<Map<String, Object>> interfaces = new ArrayList<>();
        
        Map<String, Object> edgeIface = new LinkedHashMap<>();
        edgeIface.put("componentName", "OutgoingWCTP");
        edgeIface.put("referenceName", edgeReferenceName);
        interfaces.add(edgeIface);
        
        Map<String, Object> vcsIface = new LinkedHashMap<>();
        vcsIface.put("componentName", "VMP");
        vcsIface.put("referenceName", vcsReferenceName);
        interfaces.add(vcsIface);
        
        return interfaces;
      }
      
      // If only Edge default is selected
      if (useDefaultEdge) {
        Map<String, Object> iface = new LinkedHashMap<>();
        iface.put("componentName", "OutgoingWCTP");
        iface.put("referenceName", edgeReferenceName);
        return List.of(iface);
      }
      
      // If only VMP default is selected
      if (useDefaultVmp) {
        Map<String, Object> iface = new LinkedHashMap<>();
        iface.put("componentName", "VMP");
        iface.put("referenceName", vcsReferenceName);
        return List.of(iface);
      }
    }

    return List.of();
  }

  private boolean containsEdge(String deviceName) {
    if (isBlank(deviceName)) return false;
    String lower = deviceName.toLowerCase(Locale.ROOT);
    return lower.contains("edge") || lower.contains("iphone-edge");
  }

  private boolean containsVcs(String deviceName) {
    if (isBlank(deviceName)) return false;
    String lower = deviceName.toLowerCase(Locale.ROOT);
    return lower.contains("vocera vcs") || lower.contains("vcs");
  }

  /**
   * Determines if default interfaces should be applied based on device values.
   * Defaults apply when:
   * 1. Both devices are blank/empty, OR
   * 2. Neither device contains "Edge" or "VCS"
   */
  private boolean shouldApplyDefaultInterfaces(String deviceA, String deviceB,
                                                 boolean hasEdgeA, boolean hasEdgeB,
                                                 boolean hasVcsA, boolean hasVcsB) {
    boolean deviceABlank = isBlank(deviceA);
    boolean deviceBBlank = isBlank(deviceB);
    
    return (deviceABlank && deviceBBlank) || 
           (!hasEdgeA && !hasEdgeB && !hasVcsA && !hasVcsB);
  }

  private void addOrder(List<Map<String,Object>> out,
                        String facility,
                        int order,
                        String delayText,
                        String recipientText,
                        String presenceConfig) {
    List<DestinationWithConditions> results = new ArrayList<>();
    addOrderWithConditions(results, facility, order, delayText, recipientText, presenceConfig, "normal", edgeReferenceName, "Orders");
    for (DestinationWithConditions dwc : results) {
      out.add(dwc.destination);
    }
  }
  
  private void addOrderWithConditions(List<DestinationWithConditions> out,
                                      String facility,
                                      int order,
                                      String delayText,
                                      String recipientText,
                                      String presenceConfig,
                                      String mappedPriority,
                                      String interfaceReferenceName,
                                      String flowType) {
    if (isBlank(recipientText)) return;
    
    // First check if this is a Custom Unit recipient (before splitting by comma)
    String recipientTextTrimmed = recipientText.trim();
    String normalizedLower = recipientTextTrimmed.toLowerCase(Locale.ROOT).replaceAll("[\\s\\-_]+", "");
    
    if (normalizedLower.contains("customunit")) {
      // This is a Custom Unit recipient - parse it as a whole
      ParsedRecipient pr = parseRecipient(recipientTextTrimmed, facility);
      if (pr.isCustomUnit && !pr.customUnitRoles.isEmpty()) {
        int delay = parseDelay(delayText);
        addCustomUnitDestination(out, order, delay, pr.customUnitRoles, presenceConfig, mappedPriority, interfaceReferenceName, flowType);
        return;
      }
    }
    
    // Regular recipient processing - split by comma/semicolon
    List<String> recipients = Arrays.stream(recipientText.split("[,;\\n]"))
      .map(String::trim)
      .filter(s -> !s.isEmpty() && !s.equalsIgnoreCase("N/A"))
      .collect(Collectors.toList());
    if (recipients.isEmpty()) return;

    int delay = parseDelay(delayText);
    
    List<Map<String,String>> groups = new ArrayList<>();
    List<Map<String,String>> roles  = new ArrayList<>();

    for (String raw : recipients) {
      ParsedRecipient pr = parseRecipient(raw, facility);
      if (pr.isFunctionalRole) {
        roles.add(Map.of("facilityName", pr.facility, "name", pr.value));
      } else {
        groups.add(Map.of("facilityName", pr.facility, "name", pr.value));
      }
    }

    if (groups.isEmpty() && roles.isEmpty()) return;

    Map<String,Object> dest = new LinkedHashMap<>();
    dest.put("order", order);
    dest.put("delayTime", delay);
    dest.put("destinationType", "Normal");
    dest.put("users", List.of());
    dest.put("functionalRoles", roles);
    dest.put("groups", groups);

    // Use presenceConfig from Break Through DND value
    dest.put("presenceConfig", presenceConfig);
    
    if (!roles.isEmpty()) {
      dest.put("recipientType", "functional_role");
    } else {
      dest.put("recipientType", "group");
    }
    
    out.add(new DestinationWithConditions(dest, null));
  }
  
  /**
   * Creates a custom unit destination with associated conditions.
   * Based on priority:
   * - normal/high: filters for role name, state, device status
   * - urgent: adds presence filter
   * 
   * For Orders flows, uses "patient.current_place.locs.units" path.
   * For other flows, uses "bed.room.unit.rooms.beds" path.
   */
  private void addCustomUnitDestination(List<DestinationWithConditions> out,
                                        int order,
                                        int delay,
                                        List<String> roles,
                                        String presenceConfig,
                                        String mappedPriority,
                                        String interfaceReferenceName,
                                        String flowType) {
    if (roles.isEmpty()) return;
    
    // Determine the base path based on flow type
    boolean isOrdersFlow = "Orders".equals(flowType);
    String basePath = isOrdersFlow 
        ? "patient.current_place.locs.units.locs.assignments"
        : "bed.room.unit.rooms.beds.locs.assignments";
    
    // Build the condition name from the roles
    String roleNames = String.join(" and ", roles);
    String conditionName = "Custom All Assigned " + roleNames;
    
    // Build the value string for the role filter (comma-separated)
    String roleValue = String.join(", ", roles);
    
    // Build filters based on priority
    List<Map<String, Object>> filters = new ArrayList<>();
    
    // Filter 1: role name
    Map<String, Object> roleFilter = new LinkedHashMap<>();
    roleFilter.put("attributePath", basePath + ".role.name");
    roleFilter.put("operator", "in");
    roleFilter.put("value", roleValue);
    filters.add(roleFilter);
    
    // Filter 2: state
    Map<String, Object> stateFilter = new LinkedHashMap<>();
    stateFilter.put("attributePath", basePath + ".state");
    stateFilter.put("operator", "in");
    stateFilter.put("value", "Active");
    filters.add(stateFilter);
    
    // Filter 3: device status
    Map<String, Object> deviceFilter = new LinkedHashMap<>();
    deviceFilter.put("attributePath", basePath + ".usr.devices.status");
    deviceFilter.put("operator", "in");
    deviceFilter.put("value", "Registered, Disconnected");
    filters.add(deviceFilter);
    
    // Filter 4: presence (only for urgent priority)
    boolean isUrgent = "urgent".equalsIgnoreCase(mappedPriority);
    if (isUrgent) {
      Map<String, Object> presenceFilter = new LinkedHashMap<>();
      presenceFilter.put("attributePath", basePath + ".usr.presence_show");
      presenceFilter.put("operator", "in");
      presenceFilter.put("value", "Chat, Available");
      filters.add(presenceFilter);
    }
    
    // Build the condition
    Map<String, Object> condition = new LinkedHashMap<>();
    condition.put("destinationOrder", order);
    condition.put("filters", filters);
    condition.put("name", conditionName);
    
    // Build the destination
    Map<String, Object> dest = new LinkedHashMap<>();
    dest.put("attributePath", basePath + ".usr.devices.lines.number");
    dest.put("delayTime", delay);
    dest.put("destinationType", "Normal");
    dest.put("functionalRoles", List.of());
    dest.put("groups", List.of());
    dest.put("interfaceReferenceName", interfaceReferenceName);
    dest.put("order", order);
    dest.put("presenceConfig", "none");  // Always "none" for Custom Unit workflows
    dest.put("recipientType", "custom");
    dest.put("users", List.of());
    
    out.add(new DestinationWithConditions(dest, condition));
  }

  // ---------- Parameter Attributes (correct Engage syntax) ----------
  private List<Map<String,Object>> buildParamAttributesQuoted(FlowRow r,
                                                              String flowType,
                                                              String mappedPriority) {
    List<Map<String,Object>> params = new ArrayList<>();
    boolean urgent = "urgent".equalsIgnoreCase(mappedPriority);
    boolean nurseSide = "NurseCalls".equals(flowType);
    boolean ordersType = "Orders".equals(flowType);

    // ---------- Unified logic for NurseCalls, Clinicals, and Orders ----------
    
    // Parse response options (case-insensitive, ignore whitespace)
    // Note: replaceAll("\\s+", "") intentionally converts "call back" to "callback"
    String resp = nvl(r.responseOptions, "").toLowerCase(Locale.ROOT).replaceAll("\\s+", "");
    boolean hasAccept = resp.contains("accept") && !resp.contains("noresponse");
    boolean hasAcknowledge = resp.contains("acknowledge");
    boolean hasEscalate = resp.contains("escalate");
    boolean hasDecline = resp.contains("decline");
    boolean hasReject = resp.contains("reject");
    boolean hasCallBack = resp.contains("callback");
    boolean noResponse = resp.isEmpty() || resp.contains("noresponse");
    
    // Determine which variation to use for badge phrases
    // Priority: Acknowledge > Accept for accept phrases
    String acceptPhrase = hasAcknowledge ? "Acknowledge" : "Accept";
    // Priority: Decline > Reject > Escalate for decline phrases
    String declinePhrase = hasDecline ? "Decline" : (hasReject ? "Reject" : "Escalate");
    
    // Combine flags: treat "Acknowledge" as "Accept" and "Decline"/"Reject" as "Escalate" for flow logic
    // The specific phrase used will be reflected in the badge phrases above
    hasAccept = hasAccept || hasAcknowledge;
    hasEscalate = hasEscalate || hasDecline || hasReject;

    // 1. Add ringtone FIRST if available
    if (!isBlank(r.ringtone)) {
      params.add(paQ("alertSound", r.ringtone));
    }

    // 2. Add response logic parameters
    if (noResponse) {
      params.add(paQ("responseType", "None"));
    } 
    else if (hasAccept && hasEscalate && hasCallBack) {
      // Accept, Escalate, Call Back: add callbackNumber before accept
      params.add(paQ("responseType", "Accept/Decline"));
      params.add(paQ("callbackNumber", "#{bed.pillow_number}"));
      params.add(paQ("accept", "Accepted"));
      params.add(paLiteral("acceptBadgePhrases", "[\"" + acceptPhrase + "\"]"));
      params.add(paQ("acceptAndCall", "Call Back"));
      params.add(paQ("decline", nurseSide ? "Decline Primary" : "Decline"));
      params.add(paLiteral("declineBadgePhrases", "[\"" + declinePhrase + "\"]"));
      params.add(paQ("respondingLine", "responses.line.number"));
      params.add(paQ("responsePath", "responses.action"));
      params.add(paQ("respondingUser", "responses.usr.login"));
    } 
    else if (hasAccept && hasEscalate) {
      // Accept and Escalate
      params.add(paQ("responseType", "Accept/Decline"));
      params.add(paQ("accept", "Accepted"));
      params.add(paLiteral("acceptBadgePhrases", "[\"" + acceptPhrase + "\"]"));
      params.add(paQ("decline", nurseSide ? "Decline Primary" : "Decline"));
      params.add(paLiteral("declineBadgePhrases", "[\"" + declinePhrase + "\"]"));
      params.add(paQ("respondingLine", "responses.line.number"));
      params.add(paQ("responsePath", "responses.action"));
      params.add(paQ("respondingUser", "responses.usr.login"));
    } 
    else if (hasAccept) {
      // Accept only: use "Accept/Decline" and include responding fields
      params.add(paQ("responseType", "Accept/Decline"));
      params.add(paQ("accept", "Accepted"));
      params.add(paLiteral("acceptBadgePhrases", "[\"" + acceptPhrase + "\"]"));
      params.add(paQ("respondingLine", "responses.line.number"));
      params.add(paQ("responsePath", "responses.action"));
      params.add(paQ("respondingUser", "responses.usr.login"));
    } 
    else {
      params.add(paQ("responseType", "None"));
    }

    // 2a. Add declineCount if escalateAfter contains "all"
    if (!isBlank(r.escalateAfter)) {
      String lower = r.escalateAfter.trim().toLowerCase(Locale.ROOT);
      if (lower.contains("all")) {
        params.add(paQ("declineCount", "All Recipients"));
      }
    }

    // 3. Add shared attributes
    // Use breakThroughDND from Excel if available, otherwise fallback to priority-based logic
    String breakThroughValue;
    if (!isBlank(r.breakThroughDND)) {
      breakThroughValue = mapBreakThroughDND(r.breakThroughDND, urgent);
    } else {
      // Backward compatibility: use priority-based logic if column not provided
      breakThroughValue = urgent ? "voceraAndDevice" : "none";
    }
    params.add(paQ("breakThrough", breakThroughValue));
    
    // Use parsed enunciate value if available, otherwise default to true
    boolean enunciateValue = isBlank(r.enunciate) ? true : parseEnunciateToBoolean(r.enunciate);
    params.add(paLiteralBool("enunciate", enunciateValue));
    
    // Message differs between NurseCall, Clinical, and Orders
    if (ordersType) {
      params.add(paQ("message", "Patient: #{patient.last_name}, #{patient.first_name}\\nRoom/Bed: #{patient.current_place.room.name} #{patient.current_place.bed_number}\\nProcedure #{category} #{description}"));
      params.add(paQ("patientMRN", "#{patient.mrn}:#{patient.visit_number}"));
      params.add(paQ("patientName", "#{patient.first_name} #{patient.middle_name} #{patient.last_name}"));
    } else if (nurseSide) {
      params.add(paQ("message", "Patient: #{bed.patient.last_name}, #{bed.patient.first_name}\\nRoom/Bed: #{bed.room.name} - #{bed.bed_number}"));
      params.add(paQ("patientMRN", "#{bed.patient.mrn}:#{bed.patient.visit_number}"));
      params.add(paQ("patientName", "#{bed.patient.first_name} #{bed.patient.middle_name} #{bed.patient.last_name}"));
    } else {
      params.add(paQ("message", "Clinical Alert ${destinationName}\\nRoom: #{bed.room.name} - #{bed.bed_number}\\nAlert Type: #{alert_type}\\nAlarm Time: #{alarm_time.as_time}"));
      params.add(paQ("patientMRN", "#{clinical_patient.mrn}:#{clinical_patient.visit_number}"));
      params.add(paQ("patientName", "#{clinical_patient.first_name} #{clinical_patient.middle_name} #{clinical_patient.last_name}"));
    }
    
    // placeUid differs for Orders vs NurseCalls/Clinicals
    if (!ordersType) {
      params.add(paQ("placeUid", "#{bed.uid}"));
    }
    
    params.add(paLiteralBool("popup", true));
    
    // eventIdentification, shortMessage, and subject differ by flow type
    if (ordersType) {
      params.add(paQ("eventIdentification", "Orders:#{id}"));
      params.add(paQ("shortMessage", "#{alert_type} \\\\nProcedure #{category} #{description}"));
      params.add(paQ("subject", "#{alert_type} #{patient.current_place.room.name} - #{patient.current_place.bed_number}"));
    } else if (nurseSide) {
      params.add(paQ("eventIdentification", "NurseCalls:#{id}"));
      params.add(paQ("shortMessage", "#{alert_type} #{bed.room.name} Bed #{bed.bed_number}"));
      params.add(paQ("subject", "#{alert_type} #{bed.room.name} Bed #{bed.bed_number}"));
    } else {
      params.add(paQ("eventIdentification", "Clinicals:#{id}"));
      params.add(paQ("shortMessage", "#{alert_type} #{bed.room.name} Bed #{bed.bed_number}"));
      params.add(paQ("subject", "#{alert_type} #{bed.room.name} Bed #{bed.bed_number}"));
    }
    
    String ttlStr = isBlank(r.ttlValue) ? "10" : String.valueOf(parseDelay(r.ttlValue));
    params.add(paLiteral("ttl", ttlStr));
    params.add(paLiteral("retractRules", "[\"ttlHasElapsed\"]"));
    params.add(paQ("vibrate", "short"));

    // 4. Add destination names
    if (nurseSide || ordersType) {
      addDestNameParam(params, 0, firstToken(r.r1));
      addDestNameParam(params, 1, firstToken(r.r2));
      addDestNameParam(params, 2, firstToken(r.r3));
      addDestNameParam(params, 3, firstToken(r.r4));
      addDestNameParam(params, 4, firstToken(r.r5));
    } else {
      // For Clinicals flows only
      params.add(paOrderQ(1, "destinationName", "NoCaregivers"));
      params.add(paOrderQ(1, "message", "#{alert_type}\\nIssue: A Clinical Alert has been received without any caregivers assigned to room."));
      params.add(paOrderQ(1, "shortMessage", "NoCaregiver Assigned for #{alert_type} in #{bed.room.name} Bed #{bed.bed_number}"));
      params.add(paOrderQ(1, "subject", "NoCaregiver assigned for #{alert_type} #{bed.room.name} Bed #{bed.bed_number}"));
    }
    
    return params;
  }

  private void addDestNameParam(List<Map<String,Object>> params, int order, String raw) {
    if (isBlank(raw)) return;
    String value;
    
    // Extract text after "Room" keyword (case-insensitive)
    // Examples: "VAssign: Room Charge Nurse" -> "Charge Nurse"
    //           "Rld: R5: CS 1: Room PCT" -> "PCT"
    //           "VAssign:[Room] CNA" -> "CNA"
    //           "Room] Nurse" -> "Nurse"
    //           "Room - RN" -> "RN"
    int roomIdx = raw.toLowerCase(Locale.ROOT).indexOf("room");
    if (roomIdx >= 0 && roomIdx + 4 < raw.length()) {
      // Extract everything after "room" (skip the word itself) and trim
      String afterRoom = raw.substring(roomIdx + 4).trim();
      // Remove leading special characters (brackets, parentheses, dashes, etc.) and spaces
      // Keep only alphanumeric text and spaces within the role name
      afterRoom = afterRoom.replaceAll(LEADING_SPECIAL_CHARS_PATTERN, "").trim();
      value = afterRoom.isEmpty() ? "" : afterRoom;
    } else if (roomIdx >= 0) {
      // "room" is at the end or near the end
      value = "";
    } else {
      value = "Group";
    }
    
    params.add(paOrderQ(order, "destinationName", value));
  }

  private String firstToken(String s) {
    if (isBlank(s)) return "";
    return Arrays.stream(s.split("[,;\\n]"))
      .map(String::trim)
      .filter(t -> !t.isEmpty())
      .findFirst().orElse("");
  }

  // ---------- Conditions ----------
  private List<Map<String,Object>> nurseConditions() {
    List<Map<String,Object>> out = new ArrayList<>();
    for (Map<String,Object> m : NURSE_CONDITIONS) out.add(new LinkedHashMap<>(m));
    return out;
  }
  
  /**
   * Build room filter condition for Nursecall or Clinical flows.
   * Only adds the condition if roomValue is not blank.
   */
  private Map<String,Object> buildRoomFilterCondition(String roomValue) {
    if (isBlank(roomValue)) return null;
    
    Map<String, Object> filter = new LinkedHashMap<>();
    filter.put("attributePath", "bed.room.room_number");
    filter.put("operator", "equal");
    filter.put("value", roomValue);
    
    Map<String, Object> condition = new LinkedHashMap<>();
    condition.put("filters", List.of(filter));
    condition.put("name", "Room Filter For TT");
    
    return condition;
  }
  
  /**
   * Build room filter condition for Orders flows (different structure).
   * Only adds the condition if roomValue is not blank.
   */
  private Map<String,Object> buildOrdersRoomFilterCondition(String roomValue) {
    if (isBlank(roomValue)) return null;
    
    Map<String, Object> filter = new LinkedHashMap<>();
    filter.put("attributePath", "patient.current_place.locs.units.rooms.room_number");
    filter.put("operator", "in");
    filter.put("value", roomValue);
    
    Map<String, Object> condition = new LinkedHashMap<>();
    condition.put("filters", List.of(filter));
    condition.put("name", "Room Filter for TT");
    
    return condition;
  }

  // ---------- Flow name ----------
  private String buildFlowName(String flowType,
                               String mappedPriority,
                               FlowRow row,
                               List<Map<String,String>> unitRefs) {
    String alarm = nvl(row.alarmName, row.sendingName);
    String group = row.configGroup == null ? "" : row.configGroup.trim();
    String facility = unitRefs.isEmpty() ? "" : unitRefs.get(0).getOrDefault("facilityName", "");
    List<String> unitNames = unitRefs.stream()
      .map(u -> u.getOrDefault("name",""))
      .filter(s -> !isBlank(s))
      .distinct().collect(Collectors.toList());

    List<String> parts = new ArrayList<>();
    String prefix = switch(flowType) {
      case "NurseCalls" -> "SEND NURSECALL";
      case "Clinicals" -> "SEND CLINICAL";
      case "Orders" -> "SEND ORDER";
      default -> "SEND " + flowType.toUpperCase(Locale.ROOT);
    };
    parts.add(prefix);
    if (!isBlank(mappedPriority)) parts.add(mappedPriority.toUpperCase(Locale.ROOT));
    if (!isBlank(alarm)) parts.add(alarm);
    if (!isBlank(group)) parts.add(group);
    if (!unitNames.isEmpty()) parts.add(String.join(" / ", unitNames));
    else if (!isBlank(facility)) parts.add(facility);
    return String.join(" | ", parts);
  }

  // ---------- File writers (JSON) ----------
  public void writeNurseCallsJson(File nurseFile) throws Exception {
    writeNurseCallsJson(nurseFile, false);
  }
  public void writeNurseCallsJson(File nurseFile, boolean useAdvancedMerge) throws Exception {
    ensureParent(nurseFile);
    try (FileWriter out = new FileWriter(nurseFile, false)) {
      out.write(pretty(buildNurseCallsJson(useAdvancedMerge)));
    }
  }
  public void writeClinicalsJson(File clinicalFile) throws Exception {
    writeClinicalsJson(clinicalFile, false);
  }
  public void writeClinicalsJson(File clinicalFile, boolean useAdvancedMerge) throws Exception {
    ensureParent(clinicalFile);
    try (FileWriter out = new FileWriter(clinicalFile, false)) {
      out.write(pretty(buildClinicalsJson(useAdvancedMerge)));
    }
  }
  public void writeOrdersJson(File ordersFile) throws Exception {
    writeOrdersJson(ordersFile, false);
  }
  public void writeOrdersJson(File ordersFile, boolean useAdvancedMerge) throws Exception {
    ensureParent(ordersFile);
    try (FileWriter out = new FileWriter(ordersFile, false)) {
      out.write(pretty(buildOrdersJson(useAdvancedMerge)));
    }
  }
  public void writeJson(File summaryFile) throws Exception {
    ensureParent(summaryFile);
    Map<String,Object> summary = new LinkedHashMap<>();
    summary.put("note", "This build now writes three separate JSON files: NurseCalls.json, Clinicals.json, and Orders.json");
    summary.put("nurseCalls", nurseCalls.size());
    summary.put("clinicals", clinicals.size());
    summary.put("orders", orders.size());
    try (FileWriter out = new FileWriter(summaryFile, false)) {
      out.write(pretty(summary));
    }
  }

  // ---------- NEW: Save As Excel ----------
  public void writeExcel(File dest) throws IOException {
    Objects.requireNonNull(dest, "dest");
    ensureParent(dest);

    try (Workbook wb = new XSSFWorkbook()) {
      // Unit Breakdown
      Sheet su = wb.createSheet(SHEET_UNIT);
      String[] uh = new String[]{
        "Facility",
        "Common Unit Name",
        "Nurse Call Configuration Group",
        "Patient Monitoring Configuration Group",
        "Orders Configuration Group",
        "No Caregiver Group",
        "Comments"
      };
      writeHeader(su, uh);
      int r = 1;
      for (UnitRow u : units) {
        Row row = su.createRow(r++);
        set(row,0,u.facility);
        set(row,1,u.unitNames);
        set(row,2,u.nurseGroup);
        set(row,3,u.clinGroup);
        set(row,4,u.ordersGroup);
        set(row,5,u.noCareGroup);
        set(row,6,u.comments);
      }
      autosize(su, uh.length);

      // Nurse Call
      Sheet sn = wb.createSheet(SHEET_NURSE);
      String[] fh = flowHeaders();
      writeHeader(sn, fh);
      r = 1;
      for (FlowRow f : nurseCalls) {
        Row row = sn.createRow(r++);
        writeFlowRow(row, f);
      }
      autosize(sn, fh.length);

      // Patient Monitoring
      Sheet sc = wb.createSheet(SHEET_CLINICAL);
      writeHeader(sc, fh);
      r = 1;
      for (FlowRow f : clinicals) {
        Row row = sc.createRow(r++);
        writeFlowRow(row, f);
      }
      autosize(sc, fh.length);

      // Orders
      Sheet so = wb.createSheet(SHEET_ORDERS);
      writeHeader(so, fh);
      r = 1;
      for (FlowRow f : orders) {
        Row row = so.createRow(r++);
        writeFlowRow(row, f);
      }
      autosize(so, fh.length);

      try (FileOutputStream fos = new FileOutputStream(dest)) {
        wb.write(fos);
      }
    }
  }

  private static String[] flowHeaders() {
    return new String[]{
      "In scope",
      "Configuration Group",
      "Common Alert or Alarm Name",
      "Sending System Alert Name",
      "Priority",
      "Device - A",
      "Ringtone Device - A",
      "Response Options",
      "Break Through DND",
      "Engage 6.6+: Escalate after all declines or 1 decline",
      "Engage/Edge Display Time (Time to Live) (Device - A)",
      "Genie Enunciation",
      "EMDAN Compliant? (Y/N)",
      "Time to 1st Recipient","1st Recipient",
      "Time to 2nd Recipient","2nd Recipient",
      "Time to 3rd Recipient","3rd Recipient",
      "Time to 4th Recipient","4th Recipient",
      "Time to 5th Recipient","5th Recipient"
    };
  }

  private static void writeFlowRow(Row row, FlowRow f) {
    // Column indices: 0=In scope, 1=Config Group, 2=Alarm Name, etc.
    // Keep in sync with flowHeaders() array
    set(row,0,f.inScope ? "TRUE" : "FALSE");
    set(row,1,f.configGroup);
    set(row,2,f.alarmName);
    set(row,3,f.sendingName);
    set(row,4,f.priorityRaw);
    set(row,5,f.deviceA);
    set(row,6,f.ringtone);
    set(row,7,f.responseOptions);
    set(row,8,f.breakThroughDND);
    set(row,9,f.escalateAfter);
    set(row,10,f.ttlValue);
    set(row,11,f.enunciate);
    set(row,12,f.emdan);
    set(row,13,f.t1); set(row,14,f.r1);
    set(row,15,f.t2); set(row,16,f.r2);
    set(row,17,f.t3); set(row,18,f.r3);
    set(row,19,f.t4); set(row,20,f.r4);
    set(row,21,f.t5); set(row,22,f.r5);
  }

  private static void writeHeader(Sheet s, String[] headers) {
    Row h = s.createRow(0);
    for (int i=0;i<headers.length;i++) set(h,i,headers[i]);
  }

  private static void autosize(Sheet s, int cols) {
    for (int i=0;i<cols;i++) s.autoSizeColumn(i);
  }

  private static void set(Row row, int col, String value) {
    Cell c = row.createCell(col, CellType.STRING);
    c.setCellValue(value == null ? "" : value);
  }

  // ---------- Minimal JSON writer (2 spaces, preserves order) ----------
  public static String pretty(Map<String,Object> map) {
    StringBuilder sb = new StringBuilder();
    writeJson(map, sb, 0);
    return sb.toString();
  }
  public static String pretty(Object any) {
    StringBuilder sb = new StringBuilder();
    writeJson(any, sb, 0);
    return sb.toString();
  }
  @SuppressWarnings("unchecked")
  private static void writeJson(Object o, StringBuilder sb, int indent) {
    if (o == null) { sb.append("null"); return; }
    if (o instanceof String s) { sb.append('"').append(escape(s)).append('"'); return; }
    if (o instanceof Number || o instanceof Boolean) { sb.append(String.valueOf(o)); return; }
    if (o instanceof Map<?,?> m) {
      sb.append("{\n");
      int i=0, size=m.size();
      for (Map.Entry<?,?> e : ((Map<Object,Object>)m).entrySet()) {
        indent(sb, indent+1);
        sb.append('"').append(escape(String.valueOf(e.getKey()))).append('"').append(':').append(' ');
        writeJson(e.getValue(), sb, indent+1);
        if (++i < size) sb.append(',');
        sb.append('\n');
      }
      indent(sb, indent);
      sb.append('}');
      return;
    }
    if (o instanceof Collection<?> c) {
      sb.append("[\n");
      int i=0, size=c.size();
      for (Object v : c) {
        indent(sb, indent+1);
        writeJson(v, sb, indent+1);
        if (++i < size) sb.append(',');
        sb.append('\n');
      }
      indent(sb, indent);
      sb.append(']');
      return;
    }
    sb.append('"').append(escape(String.valueOf(o))).append('"');
  }
  private static void indent(StringBuilder sb, int indent) {
    for (int i=0;i<indent;i++) sb.append("  ");
  }
  private static String escape(String s) {
    return s.replace("\\","\\\\").replace("\"","\\\"").replace("\r","\\r").replace("\n","\\n");
  }

  // ---------- ParameterAttribute helpers ----------
  private static Map<String,Object> paQ(String name, String raw) {
    Map<String,Object> m = new LinkedHashMap<>();
    m.put("name", name);
    m.put("value", addOuterQuotes(nvl(raw,"")));
    return m;
  }
  private static Map<String,Object> paLiteral(String name, String raw) {
    Map<String,Object> m = new LinkedHashMap<>();
    m.put("name", name);
    m.put("value", nvl(raw, ""));
    return m;
  }
  private static Map<String,Object> paLiteralBool(String name, boolean value) {
    return paLiteral(name, value ? "true" : "false");
  }
  private static Map<String,Object> paOrderQ(int order, String name, String raw) {
    Map<String,Object> m = paQ(name, raw);
    m.put("destinationOrder", order);
    return m;
  }
  private static String addOuterQuotes(String s) {
    String inner = s.replace("\\","\\\\").replace("\"","\\\"");
    return "\"" + inner + "\"";
  }

  // ---------- Recipients ----------
  
  /**
   * Result of building a destination, potentially with custom conditions.
   */
  private static final class DestinationWithConditions {
    final Map<String, Object> destination;
    final Map<String, Object> condition; // null if no custom condition
    
    DestinationWithConditions(Map<String, Object> destination, Map<String, Object> condition) {
      this.destination = destination;
      this.condition = condition;
    }
  }
  
  private static final class ParsedRecipient {
    final String facility;
    final String value;
    final boolean isFunctionalRole;
    final boolean isCustomUnit;
    final List<String> customUnitRoles;
    
    ParsedRecipient(String facility, String value, boolean isFunctionalRole) {
      this(facility, value, isFunctionalRole, false, List.of());
    }
    
    ParsedRecipient(String facility, String value, boolean isFunctionalRole, 
                    boolean isCustomUnit, List<String> customUnitRoles) {
      this.facility = facility == null ? "" : facility;
      this.value = value == null ? "" : value;
      this.isFunctionalRole = isFunctionalRole;
      this.isCustomUnit = isCustomUnit;
      this.customUnitRoles = customUnitRoles == null ? List.of() : new ArrayList<>(customUnitRoles);
    }
  }
  private ParsedRecipient parseRecipient(String raw, String defaultFacility) {
    String text = raw == null ? "" : raw.trim();
    if (text.isEmpty()) {
      return new ParsedRecipient(defaultFacility == null ? "" : defaultFacility, "", false);
    }
    String facility = defaultFacility == null ? "" : defaultFacility;
    String valuePortion = text;

    String lower = text.toLowerCase(Locale.ROOT);
    boolean isFunctionalRole = false;

    // Check for "Custom Unit" keyword (case-insensitive, ignoring special characters and spaces)
    String normalizedLower = lower.replaceAll("[\\s\\-_]+", "");
    if (normalizedLower.contains("customunit")) {
      return parseCustomUnitRecipient(text, facility);
    }

    if (lower.startsWith("vassign room:") || lower.startsWith("vassign:")) {
      isFunctionalRole = true;
      valuePortion = text.substring(text.indexOf(':') + 1).trim();
    } else if (lower.startsWith("vgroup:")) {
      isFunctionalRole = false;
      valuePortion = text.substring(text.indexOf(':') + 1).trim();
    } else {
      int sepIdx = text.indexOf(':');
      if (sepIdx > 0) {
        valuePortion = text.substring(sepIdx + 1).trim();
      }
    }
    
    // Extract text after "Room" keyword (case-insensitive)
    // Examples: "VAssign: Room Charge Nurse" -> "Charge Nurse"
    //           "Rld: R5: CS 1: Room PCT" -> "PCT"
    //           "[Room] Nurse" -> "Nurse"
    //           "Room]  Charge Nurse" -> "Charge Nurse"
    //           "Room)PCT" -> "PCT"
    //           "Room - CNA" -> "CNA"
    int roomIdx = valuePortion.toLowerCase(Locale.ROOT).indexOf("room");
    if (roomIdx >= 0 && roomIdx + 4 < valuePortion.length()) {
      // Skip "room" (4 chars) and any following whitespace/special characters
      String afterRoom = valuePortion.substring(roomIdx + 4).trim();
      // Remove leading special characters (brackets, parentheses, dashes, etc.) and spaces
      // Keep only alphanumeric text and spaces within the role name
      afterRoom = afterRoom.replaceAll(LEADING_SPECIAL_CHARS_PATTERN, "").trim();
      if (!afterRoom.isEmpty()) {
        valuePortion = afterRoom;
        isFunctionalRole = true; // If "Room" keyword found, it's a functional role
      }
    } else if (roomIdx >= 0) {
      // "room" is at the end, nothing after it
      // Keep the original value before "room" or use empty
      valuePortion = "";
    }
    
    return new ParsedRecipient(facility, valuePortion, isFunctionalRole);
  }
  
  /**
   * Parses "Custom Unit" recipient patterns.
   * Examples:
   *   "Custom Unit Nurse, CNA" -> roles: ["Nurse", "CNA"]
   *   "Custom UNIT all Nurse, CNA" -> roles: ["Nurse", "CNA"]
   *   "Custom Unit All Nurse, All CNA, Charge Nurse" -> roles: ["Nurse", "CNA", "Charge Nurse"]
   */
  private ParsedRecipient parseCustomUnitRecipient(String text, String facility) {
    String lower = text.toLowerCase(Locale.ROOT);
    
    // Find the position of "custom" and "unit" keywords
    String normalizedLower = lower.replaceAll("[\\s\\-_]+", "");
    int customUnitIdx = normalizedLower.indexOf("customunit");
    if (customUnitIdx < 0) {
      // Shouldn't happen, but fallback to regular parsing
      return new ParsedRecipient(facility, text, false);
    }
    
    // Find where "Custom Unit" ends in the original text
    // We need to find the end of "unit" in the original string
    int searchIdx = 0;
    int foundCustom = -1;
    int foundUnit = -1;
    
    // Find "custom" (case-insensitive)
    for (int i = 0; i < text.length() - 5; i++) {
      if (text.substring(i, i + 6).equalsIgnoreCase("custom")) {
        foundCustom = i;
        searchIdx = i + 6;
        break;
      }
    }
    
    // Find "unit" after "custom" (case-insensitive)
    if (foundCustom >= 0) {
      for (int i = searchIdx; i < text.length() - 3; i++) {
        if (text.substring(i, i + 4).equalsIgnoreCase("unit")) {
          foundUnit = i;
          searchIdx = i + 4;
          break;
        }
      }
    }
    
    if (foundUnit < 0) {
      // Couldn't find "unit" after "custom", fallback
      return new ParsedRecipient(facility, text, false);
    }
    
    // Get everything after "Custom Unit"
    String afterCustomUnit = text.substring(searchIdx).trim();
    
    // Parse the roles, ignoring "All" keyword and treating comma-separated values as different roles
    List<String> roles = new ArrayList<>();
    String[] parts = afterCustomUnit.split(",");
    
    for (String part : parts) {
      part = part.trim();
      if (part.isEmpty()) continue;
      
      // Remove "All" keyword (case-insensitive) from the beginning
      String partLower = part.toLowerCase(Locale.ROOT);
      if (partLower.startsWith("all ")) {
        part = part.substring(4).trim();
      } else if (partLower.equals("all")) {
        // Skip standalone "All"
        continue;
      }
      
      // Strip all special characters, keeping only alphanumeric and spaces
      // This handles cases like "Nurse]", "CNA#", "Charge Nurse@" -> "Nurse", "CNA", "Charge Nurse"
      part = part.replaceAll(SPECIAL_CHARS_PATTERN, "").trim();
      
      if (!part.isEmpty()) {
        roles.add(part);
      }
    }
    
    // Return a custom unit recipient with the parsed roles
    return new ParsedRecipient(facility, "", false, true, roles);
  }

  // ---------- Sheet helpers ----------
  private static Sheet findSheet(Workbook wb, String name) {
    if (wb == null || name == null) return null;
    for (int i=0;i<wb.getNumberOfSheets();i++) {
      Sheet s = wb.getSheetAt(i);
      if (s != null && name.equalsIgnoreCase(s.getSheetName())) return s;
    }
    return null;
  }
  
  /**
   * Find a sheet by trying multiple possible names (case-insensitive).
   * Returns the first sheet found matching any of the provided names.
   */
  private static Sheet findSheetCaseInsensitive(Workbook wb, String... names) {
    if (wb == null || names == null) return null;
    for (String name : names) {
      Sheet sh = findSheet(wb, name);
      if (sh != null) return sh;
    }
    return null;
  }
  
  /**
   * Find a sheet whose name contains the given substring (case-insensitive).
   * Returns the first sheet found that contains the substring.
   */
  private static Sheet findSheetContaining(Workbook wb, String substring) {
    if (wb == null || substring == null || substring.isEmpty()) return null;
    String searchTerm = substring.toLowerCase();
    for (int i = 0; i < wb.getNumberOfSheets(); i++) {
      Sheet s = wb.getSheetAt(i);
      if (s != null && s.getSheetName().toLowerCase().contains(searchTerm)) {
        return s;
      }
    }
    return null;
  }
  
  private static Row findHeaderRow(Sheet sh) {
    if (sh == null) return null;

    // First try expected positions (row 2..5)
    int start = 2;
    int end = Math.min(sh.getLastRowNum(), start + 3);
    for (int r = start; r <= end; r++) {
      Row row = sh.getRow(r);
      if (row == null) continue;
      int nonEmpty = 0;
      for (int c = 0; c < row.getLastCellNum(); c++)
        if (!getCell(row, c).isBlank()) nonEmpty++;
      if (nonEmpty >= 3) return row;
    }

    // Fallback: detect header near top (row 0..3) for re-saved Excel files
    for (int r = 0; r <= Math.min(sh.getLastRowNum(), 3); r++) {
      Row row = sh.getRow(r);
      if (row == null) continue;
      int nonEmpty = 0;
      for (int c = 0; c < row.getLastCellNum(); c++)
        if (!getCell(row, c).isBlank()) nonEmpty++;
      if (nonEmpty >= 3) return row;
    }

    // Final fallback
    for (int r = 0; r <= sh.getLastRowNum(); r++) {
      Row row = sh.getRow(r);
      if (row != null) return row;
    }
    return null;
  }
  private static int firstDataRow(Sheet sh, Row header) {
    if (sh == null) return 0;
    int first = Math.max(sh.getFirstRowNum(), 0);
    if (header != null) {
      int after = header.getRowNum() + 1;
      if (after > first) return after;
    }
    return first;
  }
  private static Map<String,Integer> headerMap(Row header) {
    Map<String,Integer> map = new LinkedHashMap<>();
    if (header == null) return map;
    for (int c=0;c<header.getLastCellNum();c++) {
      String v = getCell(header,c);
      if (!v.isBlank()) map.put(normalize(v), c);
    }
    return map;
  }
  private static int getCol(Map<String,Integer> map, String... names) {
    if (map.isEmpty()) return -1;
    for (String n : names) {
      if (n == null) continue;
      String key = normalize(n);
      if (map.containsKey(key)) return map.get(key);
    }
    for (String n : names) {
      if (n == null) continue;
      String key = normalize(n);
      for (Map.Entry<String,Integer> e : map.entrySet())
        if (e.getKey().contains(key)) return e.getValue();
    }
    return -1;
  }
  
  /**
   * Finds a column that contains a keyword anywhere in the header text
   * (case-insensitive, ignores punctuation, spaces, and newlines).
   * This ensures headers like "EMDAN Compliant? (Y/N)" or multi-line labels are detected.
   * Note: The header map keys are already normalized (lowercase, non-alphanumeric replaced with spaces).
   */
  private static int getColLoose(Map<String,Integer> map, String keyword) {
    if (map.isEmpty() || keyword == null) return -1;
    String search = normalize(keyword);
    for (Map.Entry<String,Integer> e : map.entrySet()) {
      if (e.getKey().contains(search)) {
        return e.getValue();
      }
    }
    return -1;
  }
  
  private static String getCell(Row row, int col) {
    if (row == null || col < 0) return "";
    try {
      Cell cell = row.getCell(col);
      if (cell == null) return "";
      String val = switch (cell.getCellType()) {
        case STRING -> cell.getStringCellValue().trim();
        case NUMERIC -> DateUtil.isCellDateFormatted(cell)
          ? cell.getLocalDateTimeCellValue().toString()
          : String.valueOf(cell.getNumericCellValue());
        case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
        case FORMULA -> cell.getCellFormula();
        default -> "";
      };
      if (val.equalsIgnoreCase("N/A") || val.equalsIgnoreCase("NA") || val.isBlank()) return "";
      return val.trim();
    } catch (Exception e) {
      return "";
    }
  }
  
  /**
   * Get the first non-empty value from multiple columns.
   * Used for extracting values that may exist in multiple possible columns.
   * @param row The row to read from
   * @param columns List of column indices to check
   * @return The first non-empty value found, or empty string if none found
   */
  private static String getFirstNonEmptyValue(Row row, List<Integer> columns) {
    if (row == null || columns == null || columns.isEmpty()) return "";
    for (int col : columns) {
      String value = getCell(row, col);
      if (!isBlank(value)) {
        return value;
      }
    }
    return "";
  }
  private static List<String> splitUnits(String s) {
    if (s == null) return List.of();
    return Arrays.stream(s.split("[,;/\\n]"))
      .map(String::trim).filter(v -> !v.isEmpty())
      .collect(Collectors.toCollection(LinkedHashSet::new))
      .stream().toList();
  }
  
  /**
   * Resolve facility name from a configuration group.
   * Searches the appropriate group map (nurse or clinical) for the given configuration group
   * and returns the facility name if found.
   * 
   * @param configGroup The configuration group name to look up
   * @param nurseSide true if searching nurse groups, false for clinical groups
   * @return The facility name associated with the configuration group, or empty string if not found
   */
  private String resolveFacilityFromConfig(String configGroup, boolean nurseSide) {
    if (isBlank(configGroup)) return "";
    
    // Choose the appropriate map based on nurseSide flag
    Map<String, List<Map<String, String>>> groupMap = nurseSide 
      ? nurseGroupToUnits 
      : clinicalGroupToUnits;
    
    // Look up the configuration group in the map
    List<Map<String, String>> units = groupMap.get(configGroup);
    if (units != null && !units.isEmpty()) {
      // Return the facility name from the first unit
      String facility = units.get(0).get("facilityName");
      return facility != null ? facility : "";
    }
    
    return "";
  }
  private static String stripVGroup(String v) {
    if (isBlank(v)) return "";
    return v.replaceFirst("(?i)^v(group|assign)[: ]*", "").trim();
  }
  private static void ensureParent(File f) throws IOException {
    if (f == null) throw new IOException("File is null");
    File p = f.getAbsoluteFile().getParentFile();
    if (p != null && !p.exists() && !p.mkdirs())
      throw new IOException("Unable to create directory: " + p.getAbsolutePath());
  }

  // ---------- misc helpers ----------
  private static String normalize(String s) {
    return s == null ? "" : s.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+"," ").trim();
  }
  private static boolean isBlank(String s) { return s == null || s.trim().isEmpty(); }
  private static String nvl(String a, String b) { return isBlank(a) ? (b == null ? "" : b) : a; }

  /**
   * Build a key for looking up No Caregiver Group values.
   * Key format: "facilityName|configGroupType|configGroup"
   * 
   * @param facility The facility name
   * @param configGroupType One of "nurse", "clinical", or "orders"
   * @param configGroup The configuration group name
   * @return The lookup key
   */
  private static String buildNoCaregiverKey(String facility, String configGroupType, String configGroup) {
    return facility + "|" + configGroupType + "|" + nvl(configGroup, "");
  }

  /**
   * Determine the config group type string based on flow type.
   * 
   * @param flowType One of "NurseCalls", "Clinicals", or "Orders"
   * @return The config group type string ("nurse", "clinical", or "orders")
   */
  private static String getConfigGroupType(String flowType) {
    if ("NurseCalls".equals(flowType)) {
      return "nurse";
    } else if ("Clinicals".equals(flowType)) {
      return "clinical";
    } else if ("Orders".equals(flowType)) {
      return "orders";
    } else {
      return "nurse"; // default
    }
  }

  /**
   * Parse a boolean value from Excel cell content (typically a checkbox column).
   * Supports common checkbox representations:
   * - Text values: "TRUE", "YES", "Y"
   * - Symbols: "X", "✓", "☑"
   * - Numeric: "1"
   * 
   * All comparisons are case-insensitive. Returns false for blank/empty values
   * or any unrecognized text.
   * 
   * @param value The cell value to parse
   * @return true if the value represents a checked/enabled state, false otherwise
   */
  private static boolean parseBooleanValue(String value) {
    if (isBlank(value)) return false;
    String normalized = value.trim().toUpperCase(Locale.ROOT);
    return normalized.equals("TRUE") || normalized.equals("YES") || 
           normalized.equals("Y") || normalized.equals("X") || 
           normalized.equals("✓") || normalized.equals("☑") ||
           normalized.equals("1");
  }

  /**
   * Map Break Through DND values from Excel to Engage API values.
   * 
   * Mapping rules:
   * - "Yes" or "Y" (case insensitive) -> "voceraAndDevice"
   * - "No" or "N" (case insensitive) -> "none"
   * - Known API values ("voceraAndDevice", "device", "none") are passed through as-is
   * - Empty/blank -> falls back to priority-based logic
   * 
   * @param excelValue The value from the Excel "Break Through DND" column
   * @param fallbackToUrgent When excelValue is blank, determines fallback: true -> "voceraAndDevice", false -> "none"
   * @return The mapped Engage API value
   */
  private static String mapBreakThroughDND(String excelValue, boolean fallbackToUrgent) {
    if (isBlank(excelValue)) {
      // Empty value: use priority-based logic based on fallbackToUrgent
      return fallbackToUrgent ? "voceraAndDevice" : "none";
    }
    
    String normalized = excelValue.trim().toLowerCase(Locale.ROOT);
    
    // Map user-friendly Yes/Y to voceraAndDevice
    if (normalized.equals("yes") || normalized.equals("y")) {
      return "voceraAndDevice";
    }
    
    // Map user-friendly No/N to none
    if (normalized.equals("no") || normalized.equals("n")) {
      return "none";
    }
    
    // Validate and pass through known API values
    if (normalized.equals("voceraanddevice") || normalized.equals("device") || normalized.equals("none")) {
      return normalized.equals("voceraanddevice") ? "voceraAndDevice" : normalized;
    }
    
    // Unknown value: pass through with original casing but log warning in future
    // This allows for new API values without code changes
    return excelValue.trim();
  }

  /**
   * Determine presenceConfig based on Break Through DND value.
   * 
   * Mapping rules:
   * - "Yes" or "Y" (case insensitive) -> "device"
   * - "No" or "N" (case insensitive) -> "user_and_device"
   * - Otherwise (including empty/blank) -> "user_and_device" (default)
   * 
   * @param excelValue The value from the Excel "Break Through DND" column
   * @return The presenceConfig value for destinations
   */
  private static String getPresenceConfigFromBreakThrough(String excelValue) {
    if (isBlank(excelValue)) {
      return "user_and_device";
    }
    
    String normalized = excelValue.trim().toLowerCase(Locale.ROOT);
    
    // Yes/Y means breakthrough is enabled -> use device only
    if (normalized.equals("yes") || normalized.equals("y")) {
      return "device";
    }
    
    // No/N or any other value -> use user_and_device
    return "user_and_device";
  }

  /**
   * Determine the interface component name based on the device name.
   * Returns "OutgoingWCTP" for Edge devices, "VMP" for VCS devices, or empty string otherwise.
   */
  private static String getInterfaceComponentName(String deviceName) {
    if (isBlank(deviceName)) return "";
    String lower = deviceName.toLowerCase(Locale.ROOT);
    if (lower.contains("edge") || lower.contains("iphone-edge")) {
      return "OutgoingWCTP";
    }
    if (lower.contains("vocera vcs") || lower.contains("vcs")) {
      return "VMP";
    }
    return "";
  }

  /**
   * Map priority based on the interface component name.
   * For OutgoingWCTP (Edge): Low->normal, Medium->high, High->urgent
   * For VMP (VCS): Normal(VCS)->normal, High(VCS)->high, Urgent(VCS)->urgent
   * For other interfaces or empty device: use OutgoingWCTP logic as default
   */
  private static String mapPrioritySafe(String priority, String deviceName) {
    if (priority == null) return "";
    
    String interfaceComponent = getInterfaceComponentName(deviceName);
    String norm = priority.trim().toLowerCase(Locale.ROOT);
    
    // VMP (VCS) priority mapping
    if ("VMP".equals(interfaceComponent)) {
      norm = norm.replace("(vcs)", " vcs")
                 .replaceAll("\\s+", " ")
                 .trim();
      return switch (norm) {
        case "urgent", "u" -> "urgent";
        case "urgent vcs" -> "urgent";
        case "high", "h" -> "high";
        case "high vcs" -> "high";
        case "normal", "n" -> "normal";
        case "normal vcs" -> "normal";
        default -> norm.isEmpty() ? "normal" : "";
      };
    }
    
    // OutgoingWCTP (Edge) priority mapping - also used as default
    norm = norm.replace("(edge)", " edge")
               .replaceAll("\\s+", " ")
               .trim();
    return switch (norm) {
      case "urgent", "u" -> "urgent";
      case "high", "h" -> "urgent";
      case "medium", "med", "m" -> "high";
      case "low", "l" -> "normal";
      case "normal", "n" -> "normal";
      case "low edge" -> "normal";
      case "medium edge" -> "high";
      case "high edge" -> "urgent";
      default -> "";
    };
  }
  
  /**
   * Convert Excel enunciation value to boolean.
   * Returns true if value is "Yes", "Y", "Enunciate", "Enunciation", or "True" (case-insensitive).
   * Returns false for "No", "N", "False", or any other value.
   * Note: This method is only called for non-blank values; blank values are handled by the caller.
   */
  private static boolean parseEnunciateToBoolean(String excelValue) {
    if (isBlank(excelValue)) {
      return false;  // Should not happen; caller checks blank first
    }
    String normalized = excelValue.trim().toLowerCase(Locale.ROOT);
    // True values
    if (normalized.equals("yes") || 
        normalized.equals("y") || 
        normalized.equals("enunciate") || 
        normalized.equals("enunciation") || 
        normalized.equals("true")) {
      return true;
    }
    // All other values (including "no", "n", "false") return false
    return false;
  }
  
  /**
   * Check if EMDAN value indicates compliance.
   * Returns true if value is "Yes" or "Y" (case-insensitive).
   * Returns false for any other value (including blank, "No", "N", etc.).
   */
  public static boolean isEmdanCompliant(String emdanValue) {
    if (isBlank(emdanValue)) {
      return false;
    }
    String normalized = emdanValue.trim().toLowerCase(Locale.ROOT);
    return normalized.equals("yes") || normalized.equals("y");
  }
  
  private static boolean containsWord(String hay, String needle) {
    if (isBlank(hay) || isBlank(needle)) return false;
    return hay.toLowerCase(Locale.ROOT).contains(needle.toLowerCase(Locale.ROOT));
  }
  private static int parseDelay(String s) {
    if (isBlank(s)) return 0;
    String digits = s.replaceAll("[^0-9]","");
    if (digits.isEmpty()) return 0;
    try { return Integer.parseInt(digits); } catch (Exception ignore) { return 0; }
  }
}
