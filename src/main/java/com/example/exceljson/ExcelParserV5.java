package com.example.exceljson;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * ExcelParserV5
 *
 * - Parses Engage-style Excel workbook (Unit Breakdown, Nurse Call, Patient Monitoring).
 * - Builds JSON for NurseCalls and Clinicals with exact structure and deterministic key order.
 * - Ignores blank/"N/A" cells for recipients/parameters.
 * - Splits multiple recipients into separate destinations (comma/semicolon/newline separated).
 * - Strips "VGroup:" and "VAssign:[Room]" prefixes (value only).
 * - Facility is taken from Unit Breakdown mapping (never from recipient text).
 * - NurseCalls "conditions" use the required NurseCallsCondition filter structure.
 * - Custom JSON writer enforces 2-space indentation and exact key ordering.
 *
 * Public surface:
 *   - load(File)
 *   - getLoadSummary()
 *   - buildNurseCallsJson(), buildClinicalsJson()
 *   - writeNurseCallsJson(File), writeClinicalsJson(File), writeJson(File[summary])
 *
 * Notes:
 *   - Keeps overloaded constructor ExcelParserV5(Config) for compatibility; if your project
 *     supplies a Config class with recipientParsing.functionalRoleRegex, it will be used.
 *     Otherwise, a safe default regex is applied.
 */
public class ExcelParserV5 {

  // ----------------------------- Public row types -----------------------------
  public static final class UnitRow {
    public String facility = "";
    public String unitNames = "";
    public String nurseGroup = "";
    public String clinGroup = "";
    public String noCareGroup = "";
    public String comments = "";
  }

  public static final class FlowRow {
    public String type = "";        // NurseCalls or Clinicals
    public String configGroup = "";
    public String alarmName = "";
    public String sendingName = "";
    public String priorityRaw = "";
    public String deviceA = "";
    public String ringtone = "";
    public String responseOptions = "";
    public String t1 = ""; public String r1 = "";
    public String t2 = ""; public String r2 = "";
    public String t3 = ""; public String r3 = "";
    public String t4 = ""; public String r4 = "";
    public String t5 = ""; public String r5 = "";
  }

  // ----------------------------- Configuration hook --------------------------
  // Optional external configuration type.
  // If your project has this class, the (Config) ctor will use it.
  public static final class RecipientParsingConfig {
    public String functionalRoleRegex = "(?i)^\\s*vassign\\s*:";
  }
  public static final class Config {
    public RecipientParsingConfig recipientParsing = new RecipientParsingConfig();
    public static Config defaultConfig() { return new Config(); }
  }

  // ----------------------------- Constants & State ---------------------------
  private static final String SHEET_UNIT      = "Unit Breakdown";
  private static final String SHEET_NURSE     = "Nurse Call";
  private static final String SHEET_CLINICAL  = "Patient Monitoring";

  private final Config config;
  private final Pattern functionalRolePattern;

  public final List<UnitRow> units = new ArrayList<>();
  public final List<FlowRow> nurseCalls = new ArrayList<>();
  public final List<FlowRow> clinicals = new ArrayList<>();

  private final Map<String, List<Map<String, String>>> nurseGroupToUnits = new LinkedHashMap<>();
  private final Map<String, List<Map<String, String>>> clinicalGroupToUnits = new LinkedHashMap<>();
  private final Map<String, String> noCaregiverByFacility = new LinkedHashMap<>();

  // NurseCallsCondition (fixed structure)
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

  // ----------------------------- Constructors --------------------------------
  public ExcelParserV5() {
    this(Config.defaultConfig());
  }

  public ExcelParserV5(Config config) {
    this.config = (config == null) ? Config.defaultConfig() : config;
    String regex = (this.config.recipientParsing == null || this.config.recipientParsing.functionalRoleRegex == null)
        ? "(?i)^\\s*vassign\\s*:"
        : this.config.recipientParsing.functionalRoleRegex;
    this.functionalRolePattern = Pattern.compile(regex);
  }

  // ----------------------------- Load & Summary ------------------------------
  public void load(File excelFile) throws Exception {
    Objects.requireNonNull(excelFile, "excelFile");
    units.clear();
    nurseCalls.clear();
    clinicals.clear();
    nurseGroupToUnits.clear();
    clinicalGroupToUnits.clear();
    noCaregiverByFacility.clear();

    try (FileInputStream fis = new FileInputStream(excelFile);
         Workbook wb = new XSSFWorkbook(fis)) {
      parseUnitBreakdown(wb);
      parseFlowSheet(wb, SHEET_NURSE, true);
      parseFlowSheet(wb, SHEET_CLINICAL, false);
    }
  }

  public String getLoadSummary() {
    return String.format(Locale.ROOT,
        "✅ Excel Load Complete%n%n" +
        "Loaded:%n" +
        "  • %d Unit Breakdown rows%n" +
        "  • %d Nurse Call rows%n" +
        "  • %d Patient Monitoring rows%n%n" +
        "Linked:%n" +
        "  • %d Configuration Groups (Nurse)%n" +
        "  • %d Configuration Groups (Clinical)",
        units.size(), nurseCalls.size(), clinicals.size(),
        nurseGroupToUnits.size(), clinicalGroupToUnits.size());
  }

  // ----------------------------- Parsing: Unit Breakdown ---------------------
  private void parseUnitBreakdown(Workbook wb) {
    Sheet sheet = findSheet(wb, SHEET_UNIT);
    if (sheet == null) return;

    Row header = findHeaderRow(sheet);
    Map<String, Integer> columns = headerMap(header);

    int startRow = firstDataRow(sheet, header);
    int cFacility   = getCol(columns, "Facility");
    int cUnitName   = getCol(columns, "Common Unit Name");
    int cNurseGroup = getCol(columns, "Nurse Call", "Configuration Group", "Nurse call");
    int cClinGroup  = getCol(columns, "Patient Monitoring", "Configuration Group", "Patient monitoring");
    int cNoCare     = getCol(columns, "No Caregiver Alert Number or Group", "No Caregiver Group");
    int cComments   = getCol(columns, "Comments");

    for (int r = startRow; r <= sheet.getLastRowNum(); r++) {
      Row row = sheet.getRow(r);
      if (row == null) continue;

      String facility   = getCell(row, cFacility);
      String unitNames  = getCell(row, cUnitName);
      String nurseGroup = getCell(row, cNurseGroup);
      String clinGroup  = getCell(row, cClinGroup);
      String noCare     = stripVPrefixes(getCell(row, cNoCare));
      String comments   = getCell(row, cComments);

      if (isBlank(facility) && isBlank(unitNames)) continue;

      UnitRow u = new UnitRow();
      u.facility = facility;
      u.unitNames = unitNames;
      u.nurseGroup = nurseGroup;
      u.clinGroup = clinGroup;
      u.noCareGroup = noCare;
      u.comments = comments;
      units.add(u);

      List<String> split = splitUnits(unitNames);
      if (!isBlank(nurseGroup)) {
        for (String name : split) {
          nurseGroupToUnits.computeIfAbsent(nurseGroup, k -> new ArrayList<>())
              .add(Map.of("facilityName", facility, "name", name));
        }
      }
      if (!isBlank(clinGroup)) {
        for (String name : split) {
          clinicalGroupToUnits.computeIfAbsent(clinGroup, k -> new ArrayList<>())
              .add(Map.of("facilityName", facility, "name", name));
        }
      }
      if (!isBlank(facility) && !isBlank(noCare)) {
        noCaregiverByFacility.put(facility, noCare);
      }
    }
  }

  // ----------------------------- Parsing: Flow Sheets ------------------------
  private void parseFlowSheet(Workbook wb, String sheetName, boolean nurseSide) {
    Sheet sheet = findSheet(wb, sheetName);
    if (sheet == null) return;

    Row header = findHeaderRow(sheet);
    Map<String, Integer> columns = headerMap(header);
    int startRow = firstDataRow(sheet, header);

    int cCfg   = getCol(columns, "Configuration Group");
    int cAlarm = getCol(columns, "Common Alert or Alarm Name", "Alarm Name");
    int cSend  = getCol(columns, "Sending System Alert Name", "Sending System Alarm Name");
    int cPrio  = getCol(columns, "Priority");
    int cDevA  = getCol(columns, "Device - A", "Device");
    int cRing  = getCol(columns, "Ringtone Device - A", "Ringtone");
    int cResp  = getCol(columns, "Response Options", "Response Option");

    int cT1 = getCol(columns, "Time to 1st Recipient", "Delay to 1st", "Time to 1st Recipient (after alarm triggers)");
    int cR1 = getCol(columns, "1st Recipient", "First Recipient", "1st recipients");
    int cT2 = getCol(columns, "Time to 2nd Recipient", "Delay to 2nd");
    int cR2 = getCol(columns, "2nd Recipient", "Second Recipient");
    int cT3 = getCol(columns, "Time to 3rd Recipient", "Delay to 3rd");
    int cR3 = getCol(columns, "3rd Recipient", "Third Recipient");
    int cT4 = getCol(columns, "Time to 4th Recipient");
    int cR4 = getCol(columns, "4th Recipient");
    int cT5 = getCol(columns, "Time to 5th Recipient");
    int cR5 = getCol(columns, "5th Recipient");

    for (int r = startRow; r <= sheet.getLastRowNum(); r++) {
      Row row = sheet.getRow(r);
      if (row == null) continue;

      FlowRow f = new FlowRow();
      f.type         = nurseSide ? "NurseCalls" : "Clinicals";
      f.configGroup  = getCell(row, cCfg);
      f.alarmName    = getCell(row, cAlarm);
      f.sendingName  = getCell(row, cSend);
      f.priorityRaw  = getCell(row, cPrio);
      f.deviceA      = getCell(row, cDevA);
      f.ringtone     = getCell(row, cRing);
      f.responseOptions = getCell(row, cResp);
      f.t1 = getCell(row, cT1); f.r1 = getCell(row, cR1);
      f.t2 = getCell(row, cT2); f.r2 = getCell(row, cR2);
      f.t3 = getCell(row, cT3); f.r3 = getCell(row, cR3);
      f.t4 = getCell(row, cT4); f.r4 = getCell(row, cR4);
      f.t5 = getCell(row, cT5); f.r5 = getCell(row, cR5);

      if (isBlank(f.alarmName) && isBlank(f.sendingName)) continue;
      if (nurseSide) nurseCalls.add(f); else clinicals.add(f);
    }
  }

  // ----------------------------- JSON Builders -------------------------------
  public Map<String, Object> buildNurseCallsJson() {
    return buildJson(nurseCalls, nurseGroupToUnits, true);
  }

  public Map<String, Object> buildClinicalsJson() {
    return buildJson(clinicals, clinicalGroupToUnits, false);
  }

  private Map<String, Object> buildJson(List<FlowRow> rows,
                                        Map<String, List<Map<String, String>>> groupToUnits,
                                        boolean nurseSide) {
    Map<String, Object> root = new LinkedHashMap<>();
    root.put("version", "1.1.0");
    root.put("alarmAlertDefinitions", buildAlarmDefs(rows, nurseSide));

    List<Map<String, Object>> flows = new ArrayList<>();
    for (FlowRow row : rows) {
      if (isBlank(row.configGroup) && isBlank(row.alarmName) && isBlank(row.sendingName)) continue;

      List<Map<String, String>> unitRefs = groupToUnits.getOrDefault(row.configGroup, List.of());
      String mappedPriority = mapPrioritySafe(row.priorityRaw);

      Map<String, Object> flow = new LinkedHashMap<>();
      flow.put("name", buildFlowName(nurseSide, mappedPriority, row, unitRefs));
      if (!isBlank(mappedPriority)) flow.put("priority", mappedPriority);
      flow.put("status", "Active");
      flow.put("alarmsAlerts", List.of(nvl(row.alarmName, row.sendingName)));
      flow.put("conditions", nurseSide ? nurseConditions() : List.of());
      flow.put("interfaces", List.of(Map.of(
          "componentName", "OutgoingWCTP",
          "referenceName", "OutgoingWCTP"
      )));
      flow.put("destinations", buildDestinations(row, unitRefs, nurseSide));
      flow.put("parameterAttributes", buildParamAttributes(row, nurseSide, mappedPriority));
      if (!unitRefs.isEmpty()) {
        flow.put("units", unitRefs);
      }
      flows.add(flow);
    }

    root.put("deliveryFlows", flows);
    return root;
  }

  private List<Map<String, Object>> buildAlarmDefs(List<FlowRow> rows, boolean nurseSide) {
    Map<String, Map<String, Object>> defs = new LinkedHashMap<>();
    for (FlowRow row : rows) {
      String name = nvl(row.alarmName, row.sendingName);
      if (isBlank(name)) continue;
      String key = name + "|" + (nurseSide ? "N" : "C");
      if (defs.containsKey(key)) continue;

      Map<String, Object> def = new LinkedHashMap<>();
      def.put("name", name);
      def.put("type", nurseSide ? "NurseCalls" : "Clinicals");

      Map<String, String> value = new LinkedHashMap<>();
      value.put("category", "");
      value.put("value", isBlank(row.sendingName) ? name : row.sendingName);

      def.put("values", List.of(value));
      defs.put(key, def);
    }
    return new ArrayList<>(defs.values());
  }

  private String buildFlowName(boolean nurseSide,
                               String mappedPriority,
                               FlowRow row,
                               List<Map<String, String>> unitRefs) {
    String alarm = nvl(row.alarmName, row.sendingName);
    String group = row.configGroup == null ? "" : row.configGroup.trim();
    String facility = unitRefs.isEmpty() ? "" : unitRefs.get(0).getOrDefault("facilityName", "");
    List<String> unitNames = unitRefs.stream()
        .map(u -> u.getOrDefault("name", ""))
        .filter(s -> !isBlank(s))
        .distinct()
        .collect(Collectors.toList());

    List<String> parts = new ArrayList<>();
    parts.add(nurseSide ? "SEND NURSECALL" : "SEND CLINICAL");
    if (!isBlank(mappedPriority)) parts.add(mappedPriority.toUpperCase(Locale.ROOT));
    if (!isBlank(alarm)) parts.add(alarm);
    if (!isBlank(group)) parts.add(group);
    if (!unitNames.isEmpty()) {
      parts.add(String.join(" / ", unitNames));
    } else if (!isBlank(facility)) {
      parts.add(facility);
    }
    return String.join(" | ", parts);
  }

  private List<Map<String, Object>> nurseConditions() {
    // defensive copy
    List<Map<String, Object>> out = new ArrayList<>();
    for (Map<String, Object> m : NURSE_CONDITIONS) out.add(new LinkedHashMap<>(m));
    return out;
  }

  // ----------------------------- Destinations --------------------------------
  private List<Map<String, Object>> buildDestinations(FlowRow r,
                                                      List<Map<String, String>> unitRefs,
                                                      boolean nurseSide) {
    List<Map<String, Object>> dests = new ArrayList<>();
    String facility = unitRefs.isEmpty() ? "" : unitRefs.get(0).getOrDefault("facilityName", "");

    addOneDestination(dests, facility, 0, r.t1, r.r1);
    addOneDestination(dests, facility, 1, r.t2, r.r2);
    addOneDestination(dests, facility, 2, r.t3, r.r3);
    addOneDestination(dests, facility, 3, r.t4, r.r4);
    addOneDestination(dests, facility, 4, r.t5, r.r5);

    // Clinicals: add NoDeliveries with NoCaregivers group if available
    if (!nurseSide && !isBlank(facility)) {
      String noCare = noCaregiverByFacility.getOrDefault(facility, "");
      if (!isBlank(noCare)) {
        Map<String, Object> d = new LinkedHashMap<>();
        d.put("order", dests.size());
        d.put("delayTime", 0);
        d.put("destinationType", "NoDeliveries");
        d.put("users", List.of());
        d.put("functionalRoles", List.of());
        d.put("groups", List.of(Map.of(
            "facilityName", facility,
            "name", noCare
        )));
        d.put("presenceConfig", "device");
        d.put("recipientType", "group");
        dests.add(d);
      }
    }
    return dests;
  }

  private void addOneDestination(List<Map<String, Object>> dests,
                                 String facility,
                                 int order,
                                 String delayText,
                                 String recipientText) {
    // Skip if recipient blank or "N/A"
    if (isBlank(recipientText) || "N/A".equalsIgnoreCase(recipientText) || "NA".equalsIgnoreCase(recipientText)) {
      return;
    }

    List<String> recipients = Arrays.stream(recipientText.split("[,;\\n]"))
        .map(String::trim)
        .filter(s -> !s.isEmpty() && !s.equalsIgnoreCase("N/A") && !s.equalsIgnoreCase("NA"))
        .collect(Collectors.toList());
    if (recipients.isEmpty()) return;

    int delay = parseDelay(delayText);
    for (String recip : recipients) {
      ParsedRecipient pr = parseRecipient(recip, facility);

      Map<String, Object> d = new LinkedHashMap<>();
      d.put("order", order);
      d.put("delayTime", delay);
      d.put("destinationType", "Normal");
      d.put("users", List.of());

      if (pr.isFunctionalRole) {
        d.put("functionalRoles", List.of(Map.of(
            "facilityName", pr.facility,
            "name", pr.value
        )));
        d.put("groups", List.of());
        d.put("presenceConfig", "user_and_device");
        d.put("recipientType", "functional_role");
      } else {
        d.put("functionalRoles", List.of());
        d.put("groups", List.of(Map.of(
            "facilityName", pr.facility,
            "name", pr.value
        )));
        d.put("presenceConfig", "device");
        d.put("recipientType", "group");
      }
      dests.add(d);
    }
  }

  // ----------------------------- Parameter Attributes ------------------------
  private List<Map<String, Object>> buildParamAttributes(FlowRow row,
                                                         boolean nurseSide,
                                                         String mappedPriority) {
    List<Map<String, Object>> params = new ArrayList<>();
    boolean urgent = "urgent".equalsIgnoreCase(mappedPriority);

    if (!isBlank(row.ringtone) && !row.ringtone.equalsIgnoreCase("N/A") && !row.ringtone.equalsIgnoreCase("NA")) {
      params.add(pa("alertSound", row.ringtone));
    }

    if (nurseSide) {
      boolean hasAccept   = containsWord(row.responseOptions, "accept");
      boolean hasEscalate = containsWord(row.responseOptions, "escalate");
      boolean hasCallBack = containsWord(row.responseOptions, "call back") || containsWord(row.responseOptions, "callback");
      boolean noResponse  = containsWord(row.responseOptions, "no response");

      if (hasAccept) {
        params.add(pa("accept", "Accepted"));
        params.add(pa("acceptBadgePhrases", "[Accept]"));
      }
      if (hasCallBack) {
        params.add(pa("acceptAndCall", "Call Back"));
      }
      if (hasEscalate) {
        params.add(pa("decline", "Decline Primary"));
        params.add(pa("declineBadgePhrases", "[Escalate]"));
      }

      params.add(pa("breakThrough", urgent ? "voceraAndDevice" : "none"));
      params.add(pa("enunciate", "true"));
      params.add(pa("message", "Patient: #{bed.patient.last_name}, #{bed.patient.first_name}\\nRoom/Bed: #{bed.room.name} - #{bed.bed_number}"));
      params.add(pa("patientMRN", "#{bed.patient.mrn}:#{bed.patient.visit_number}"));
      params.add(pa("placeUid", "#{bed.uid}"));
      params.add(pa("patientName", "#{bed.patient.first_name} #{bed.patient.middle_name} #{bed.patient.last_name}"));
      params.add(pa("popup", "true"));
      params.add(pa("eventIdentification", "NurseCalls:#{id}"));
      params.add(pa("responseType", noResponse ? "None" : "Accept/Decline"));
      params.add(pa("shortMessage", "#{alert_type} #{bed.room.name}"));
      params.add(pa("subject", "#{alert_type} #{bed.room.name}"));
      params.add(pa("ttl", "10"));
      params.add(pa("retractRules", "[ttlHasElapsed]"));
      params.add(pa("vibrate", "short"));

      addDestNameParamsFromRecipients(params, row);
    } else {
      params.add(paOrder(0, "destinationName", "Nurse Alert"));
      params.add(pa("alertSound", nvl(row.ringtone, "Vocera Tone 0 Long")));
      params.add(pa("responseAllowed", "false"));
      params.add(pa("breakThrough", urgent ? "voceraAndDevice" : "none"));
      params.add(pa("enunciate", "true"));
      params.add(pa("message", "Clinical Alert ${destinationName}\\nRoom: #{bed.room.name} - #{bed.bed_number}\\nAlert Type: #{alert_type}\\nAlarm Time: #{alarm_time.as_time}"));
      params.add(pa("patientMRN", "#{clinical_patient.mrn}:#{clinical_patient.visit_number}"));
      params.add(pa("patientName", "#{clinical_patient.first_name} #{clinical_patient.middle_name} #{clinical_patient.last_name}"));
      params.add(pa("placeUid", "#{bed.uid}"));
      params.add(pa("popup", "true"));
      params.add(pa("eventIdentification", "#{id}"));
      params.add(pa("responseType", "None"));
      params.add(pa("shortMessage", "#{alert_type} #{bed.room.name}"));
      params.add(pa("subject", "#{alert_type} #{bed.room.name}"));
      params.add(pa("ttl", "10"));
      params.add(pa("retractRules", "[ttlHasElapsed]"));
      params.add(pa("vibrate", "short"));

      // If NoDeliveries destination is appended (index 1), set its display values
      params.add(paOrder(1, "destinationName", "NoCaregivers"));
      params.add(paOrder(1, "message", "#{alert_type}\\nIssue: A Clinical Alert has been received without any caregivers assigned to room."));
    }

    return params;
  }

  private void addDestNameParamsFromRecipients(List<Map<String, Object>> params, FlowRow r) {
    Map<Integer, String> first = new LinkedHashMap<>();
    first.put(0, firstRecipient(r.r1));
    first.put(1, firstRecipient(r.r2));
    first.put(2, firstRecipient(r.r3));
    first.put(3, firstRecipient(r.r4));
    first.put(4, firstRecipient(r.r5));

    for (Map.Entry<Integer, String> e : first.entrySet()) {
      String v = e.getValue();
      if (!isBlank(v)) {
        params.add(paOrder(e.getKey(), "destinationName", v));
      }
    }
  }

  private String firstRecipient(String value) {
    if (isBlank(value)) return "";
    return Arrays.stream(value.split("[,;\\n]"))
        .map(String::trim)
        .filter(s -> !s.isEmpty() && !s.equalsIgnoreCase("N/A") && !s.equalsIgnoreCase("NA"))
        .findFirst()
        .orElse("");
  }

  private Map<String, Object> pa(String name, String value) {
    Map<String, Object> m = new LinkedHashMap<>();
    m.put("name", name);
    m.put("value", value == null ? "" : value);
    return m;
  }

  private Map<String, Object> paOrder(int order, String name, String value) {
    Map<String, Object> m = pa(name, value);
    m.put("destinationOrder", order);
    return m;
  }

  // ----------------------------- Writers -------------------------------------
  public void writeNurseCallsJson(File nurseFile) throws Exception {
    ensureParent(nurseFile);
    try (FileWriter out = new FileWriter(nurseFile, false)) {
      writeJsonObject(out, buildNurseCallsJson());
    }
  }

  public void writeClinicalsJson(File clinicalFile) throws Exception {
    ensureParent(clinicalFile);
    try (FileWriter out = new FileWriter(clinicalFile, false)) {
      writeJsonObject(out, buildClinicalsJson());
    }
  }

  /**
   * Writes a tiny summary JSON (for backward compatibility if something expects a single file).
   */
  public void writeJson(File summaryFile) throws Exception {
    ensureParent(summaryFile);
    Map<String, Object> summary = new LinkedHashMap<>();
    summary.put("note", "This build writes two separate JSON files: NurseCalls.json and Clinicals.json");
    summary.put("nurseCalls", nurseCalls.size());
    summary.put("clinicals", clinicals.size());
    try (FileWriter out = new FileWriter(summaryFile, false)) {
      writeJsonObject(out, summary);
    }
  }

  // ----------------------------- Deterministic JSON Writer -------------------
  // 2-space indentation, exact key order by known schemas, stable writing.
  private void writeJsonObject(Writer w, Map<String, Object> obj) throws IOException {
    JsonWriter jw = new JsonWriter(w, 2);
    jw.writeRoot(obj);
  }

  private static final class JsonWriter {
    private final Writer out;
    private final int indentSize;
    private int indent = 0;

    JsonWriter(Writer out, int indentSize) {
      this.out = out;
      this.indentSize = Math.max(2, indentSize);
    }

    void writeRoot(Map<String, Object> root) throws IOException {
      writeObject(root, RootOrder.INSTANCE);
      out.write("\n");
    }

    private void write(Object v) throws IOException {
      if (v == null) {
        out.write("null");
      } else if (v instanceof String s) {
        writeString(s);
      } else if (v instanceof Number || v instanceof Boolean) {
        out.write(String.valueOf(v));
      } else if (v instanceof Map<?,?> m) {
        writeObject(castMap(m), GenericOrder.INSTANCE);
      } else if (v instanceof Collection<?> c) {
        writeArray(c);
      } else {
        // Fallback to string
        writeString(String.valueOf(v));
      }
    }

    private void writeObject(Map<String, Object> m, KeyOrder order) throws IOException {
      out.write("{\n");
      indent++;
      List<String> keys = new ArrayList<>(m.keySet());
      order.order(keys, m);
      for (int i = 0; i < keys.size(); i++) {
        String k = keys.get(i);
        Object v = m.get(k);
        if (v == null) continue;
        writeIndent();
        writeString(k);
        out.write(": ");
        // Choose object writers with schema-specific order where needed
        if (k.equals("deliveryFlows") && v instanceof Collection<?> c) {
          writeFlowsArray(c);
        } else if (k.equals("alarmAlertDefinitions") && v instanceof Collection<?> c) {
          writeAlarmDefsArray(c);
        } else {
          write(v);
        }
        if (i < keys.size() - 1) out.write(",");
        out.write("\n");
      }
      indent--;
      writeIndent();
      out.write("}");
    }

    private void writeArray(Collection<?> c) throws IOException {
      out.write("[\n");
      indent++;
      int i = 0;
      for (Object v : c) {
        writeIndent();
        write(v);
        if (++i < c.size()) out.write(",");
        out.write("\n");
      }
      indent--;
      writeIndent();
      out.write("]");
    }

    // Specialized writers for schema parts to enforce inner key order
    private void writeFlowsArray(Collection<?> flows) throws IOException {
      out.write("[\n");
      indent++;
      int idx = 0;
      for (Object v : flows) {
        writeIndent();
        if (v instanceof Map<?,?> m) {
          writeObject(castMap(m), FlowOrder.INSTANCE);
        } else {
          write(v);
        }
        if (++idx < flows.size()) out.write(",");
        out.write("\n");
      }
      indent--;
      writeIndent();
      out.write("]");
    }

    private void writeAlarmDefsArray(Collection<?> defs) throws IOException {
      out.write("[\n");
      indent++;
      int idx = 0;
      for (Object v : defs) {
        writeIndent();
        if (v instanceof Map<?,?> m) {
          writeObject(castMap(m), AlarmDefOrder.INSTANCE);
        } else {
          write(v);
        }
        if (++idx < defs.size()) out.write(",");
        out.write("\n");
      }
      indent--;
      writeIndent();
      out.write("]");
    }

    private void writeString(String s) throws IOException {
      out.write("\"");
      for (int i = 0; i < s.length(); i++) {
        char ch = s.charAt(i);
        switch (ch) {
          case '"'  -> out.write("\\\"");
          case '\\' -> out.write("\\\\");
          case '\b' -> out.write("\\b");
          case '\f' -> out.write("\\f");
          case '\n' -> out.write("\\n");
          case '\r' -> out.write("\\r");
          case '\t' -> out.write("\\t");
          default -> {
            if (ch < 32) {
              out.write(String.format("\\u%04x", (int) ch));
            } else {
              out.write(ch);
            }
          }
        }
      }
      out.write("\"");
    }

    private void writeIndent() throws IOException {
      out.write(" ".repeat(indent * indentSize));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> castMap(Map<?,?> m) {
      Map<String, Object> r = new LinkedHashMap<>();
      for (Map.Entry<?,?> e : m.entrySet()) {
        r.put(String.valueOf(e.getKey()), e.getValue());
      }
      return r;
    }

    // -------- Key order strategies --------
    private interface KeyOrder {
      void order(List<String> keys, Map<String, Object> obj);
    }

    private static final class RootOrder implements KeyOrder {
      static final RootOrder INSTANCE = new RootOrder();
      private static final List<String> ORDER = List.of("version", "alarmAlertDefinitions", "deliveryFlows");
      public void order(List<String> keys, Map<String, Object> obj) {
        stableOrder(keys, ORDER);
      }
    }

    private static final class FlowOrder implements KeyOrder {
      static final FlowOrder INSTANCE = new FlowOrder();
      private static final List<String> ORDER = List.of(
          "name", "priority", "status", "alarmsAlerts", "conditions",
          "interfaces", "destinations", "parameterAttributes", "units"
      );
      public void order(List<String> keys, Map<String, Object> obj) {
        // Hide priority if absent (already absent in map)
        stableOrder(keys, ORDER);
      }
    }

    private static final class AlarmDefOrder implements KeyOrder {
      static final AlarmDefOrder INSTANCE = new AlarmDefOrder();
      private static final List<String> ORDER = List.of("name", "type", "values");
      public void order(List<String> keys, Map<String, Object> obj) {
        stableOrder(keys, ORDER);
      }
    }

    private static final class GenericOrder implements KeyOrder {
      static final GenericOrder INSTANCE = new GenericOrder();
      public void order(List<String> keys, Map<String, Object> obj) {
        // Keep insertion order as-is for generic nodes
      }
    }

    private static void stableOrder(List<String> keys, List<String> desired) {
      Set<String> set = new LinkedHashSet<>(keys);
      keys.clear();
      for (String k : desired) if (set.remove(k)) keys.add(k);
      keys.addAll(set); // append any extras deterministically
    }
  }

  // ----------------------------- Helpers: recipients --------------------------
  private static final class ParsedRecipient {
    final String facility;
    final String value;
    final boolean isFunctionalRole;
    ParsedRecipient(String facility, String value, boolean isFunctionalRole) {
      this.facility = facility == null ? "" : facility;
      this.value = value == null ? "" : value;
      this.isFunctionalRole = isFunctionalRole;
    }
  }

  private ParsedRecipient parseRecipient(String raw, String defaultFacility) {
    String text = raw == null ? "" : raw.trim();
    if (text.isEmpty()) return new ParsedRecipient(nz(defaultFacility), "", false);

    // Facility is never taken from recipient text (per requirement).
    String facility = nz(defaultFacility);
    String valuePortion = text;

    // Strip VGroup: and VAssign:[Room] prefixes
    valuePortion = valuePortion
        .replaceAll("(?i)^\\s*vgroup\\s*:\\s*", "")
        .replaceAll("(?i)^\\s*vassign\\s*:\\s*\\[\\s*room\\s*]\\s*", "")
        .replaceFirst("(?i)^\\[\\s*room\\s*]\\s*", "")
        .trim();

    // Determine if it is a functional role (via regex, based on the original text)
    boolean isFunctionalRole = functionalRolePattern.matcher(raw).find();

    return new ParsedRecipient(facility, valuePortion, isFunctionalRole);
  }

  // ----------------------------- Utilities -----------------------------------
  private static String stripVPrefixes(String v) {
    if (v == null) return "";
    return v.replaceFirst("(?i)^\\s*vgroup\\s*:\\s*", "")
            .replaceFirst("(?i)^\\s*vassign\\s*:\\s*\\[\\s*room\\s*]\\s*", "")
            .trim();
  }

  private static List<String> splitUnits(String unitNames) {
    if (isBlank(unitNames)) return List.of();
    Set<String> names = new LinkedHashSet<>();
    for (String token : unitNames.split("[,;/\\n]")) {
      String t = token.trim();
      if (!t.isEmpty()) names.add(t);
    }
    if (names.isEmpty()) names.add(unitNames.trim());
    return new ArrayList<>(names);
  }

  private static String getCell(Row row, int col) {
    if (row == null || col < 0) return "";
    try {
      Cell c = row.getCell(col);
      if (c == null) return "";
      String value = switch (c.getCellType()) {
        case STRING  -> c.getStringCellValue().trim();
        case NUMERIC -> String.valueOf(c.getNumericCellValue());
        case BOOLEAN -> String.valueOf(c.getBooleanCellValue());
        case FORMULA -> c.getCellFormula();
        default      -> "";
      };
      // Normalize "N/A" / "NA" / blank -> empty
      if (value.equalsIgnoreCase("N/A") || value.equalsIgnoreCase("NA") || value.isBlank()) return "";
      return value.trim();
    } catch (Exception e) {
      return "";
    }
  }

  private static int parseDelay(String s) {
    if (isBlank(s)) return 0;
    String digits = s.replaceAll("[^0-9]", "");
    if (digits.isEmpty()) return 0;
    try { return Integer.parseInt(digits); } catch (Exception ignore) { return 0; }
  }

  private static String nvl(String a, String b) { return isBlank(a) ? (b == null ? "" : b) : a; }
  private static String nz(String s) { return s == null ? "" : s; }
  private static boolean isBlank(String s) { return s == null || s.trim().isEmpty(); }

  private static String normalize(String header) {
    return header == null ? "" : header.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", " ").trim();
  }

  private static int getCol(Map<String, Integer> map, String... names) {
    if (map.isEmpty() || names == null) return -1;
    for (String n : names) {
      if (n == null) continue;
      String norm = normalize(n);
      Integer idx = map.get(norm);
      if (idx != null) return idx;
    }
    // fallback: partial contains
    for (String n : names) {
      if (n == null) continue;
      String norm = normalize(n);
      for (Map.Entry<String,Integer> e : map.entrySet()) {
        if (e.getKey().contains(norm)) return e.getValue();
      }
    }
    return -1;
  }

  private static Map<String, Integer> headerMap(Row header) {
    Map<String, Integer> m = new LinkedHashMap<>();
    if (header == null) return m;
    for (int c = 0; c < header.getLastCellNum(); c++) {
      String v = getCell(header, c);
      if (v.isBlank()) continue;
      m.put(normalize(v), c);
    }
    return m;
  }

  private static Row findHeaderRow(Sheet sh) {
    if (sh == null) return null;
    int start = Math.max(0, 2);
    int end = Math.min(sh.getLastRowNum(), start + 3);
    for (int r = start; r <= end; r++) {
      Row row = sh.getRow(r);
      if (row == null) continue;
      int nonEmpty = 0;
      for (int c = 0; c < row.getLastCellNum(); c++) if (!getCell(row, c).isBlank()) nonEmpty++;
      if (nonEmpty >= 3) return row;
    }
    for (int r = 0; r <= sh.getLastRowNum(); r++) {
      Row row = sh.getRow(r);
      if (row != null) return row;
    }
    return null;
  }

  private static int firstDataRow(Sheet sh, Row header) {
    if (sh == null) return 0;
    int firstRow = Math.max(sh.getFirstRowNum(), 0);
    if (header != null) {
      int afterHeader = header.getRowNum() + 1;
      if (afterHeader > firstRow) return afterHeader;
    }
    return firstRow;
  }

  private static Sheet findSheet(Workbook wb, String name) {
    if (wb == null || name == null) return null;
    for (int i = 0; i < wb.getNumberOfSheets(); i++) {
      Sheet s = wb.getSheetAt(i);
      if (s != null && name.equalsIgnoreCase(s.getSheetName())) return s;
    }
    return null;
  }

  private static boolean containsWord(String text, String probe) {
    if (isBlank(text) || isBlank(probe)) return false;
    return text.toLowerCase(Locale.ROOT).contains(probe.toLowerCase(Locale.ROOT));
  }

  private static String mapPrioritySafe(String priority) {
    if (priority == null) return "";
    String norm = priority.trim().toLowerCase(Locale.ROOT);
    return switch (norm) {
      case "urgent", "u" -> "urgent";
      case "high", "h" -> "high";
      case "medium", "med", "m" -> "medium";
      case "low", "l" -> "low";
      case "normal", "n" -> "normal";
      default -> "";
    };
  }

  private static void ensureParent(File f) throws IOException {
    if (f == null) throw new IOException("File reference is null");
    File p = f.getAbsoluteFile().getParentFile();
    if (p != null && !p.exists() && !p.mkdirs()) {
      throw new IOException("Unable to create directory: " + p.getAbsolutePath());
    }
  }
}
