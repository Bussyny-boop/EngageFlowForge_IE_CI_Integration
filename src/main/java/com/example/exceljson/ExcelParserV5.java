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
    public String noCareGroup = "";
    public String comments = "";
  }

  public static final class FlowRow {
    public String type = ""; // NurseCalls or Clinicals
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

  // ---------- Config / parsing helpers ----------
  public final List<UnitRow> units = new ArrayList<>();
  public final List<FlowRow> nurseCalls = new ArrayList<>();
  public final List<FlowRow> clinicals = new ArrayList<>();

  private final Map<String, List<Map<String, String>>> nurseGroupToUnits = new LinkedHashMap<>();
  private final Map<String, List<Map<String, String>>> clinicalGroupToUnits = new LinkedHashMap<>();
  private final Map<String, String> noCaregiverByFacility = new LinkedHashMap<>();

  private static final String SHEET_UNIT = "Unit Breakdown";
  private static final String SHEET_NURSE = "Nurse Call";
  private static final String SHEET_CLINICAL = "Patient Monitoring";

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

  private void clear() {
    units.clear();
    nurseCalls.clear();
    clinicals.clear();
    nurseGroupToUnits.clear();
    clinicalGroupToUnits.clear();
    noCaregiverByFacility.clear();
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
    int cNoCare     = getCol(hm, "No Caregiver Alert Number or Group", "No Caregiver Group");
    int cComments   = getCol(hm, "Comments");

    for (int r = start; r <= sh.getLastRowNum(); r++) {
      Row row = sh.getRow(r);
      if (row == null) continue;

      String facility   = getCell(row, cFacility);
      String unitNames  = getCell(row, cUnitName);
      String nurseGroup = getCell(row, cNurseGroup);
      String clinGroup  = getCell(row, cClinGroup);
      String noCare     = stripVGroup(getCell(row, cNoCare));
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

      List<String> list = splitUnits(unitNames);
      if (!isBlank(nurseGroup)) {
        for (String name : list) {
          nurseGroupToUnits.computeIfAbsent(nurseGroup, k -> new ArrayList<>())
            .add(Map.of("facilityName", facility, "name", name));
        }
      }
      if (!isBlank(clinGroup)) {
        for (String name : list) {
          clinicalGroupToUnits.computeIfAbsent(clinGroup, k -> new ArrayList<>())
            .add(Map.of("facilityName", facility, "name", name));
        }
      }
      if (!isBlank(facility) && !isBlank(noCare)) {
        noCaregiverByFacility.put(facility, noCare);
      }
    }
  }

  // ---------- Rebuild unit maps from edited units list ----------
  public void rebuildUnitMaps() {
    nurseGroupToUnits.clear();
    clinicalGroupToUnits.clear();
    noCaregiverByFacility.clear();

    for (UnitRow u : units) {
      String facility = u.facility;
      String unitNames = u.unitNames;
      String nurseGroup = u.nurseGroup;
      String clinGroup = u.clinGroup;
      String noCare = u.noCareGroup;

      List<String> list = splitUnits(unitNames);
      if (!isBlank(nurseGroup)) {
        for (String name : list) {
          nurseGroupToUnits.computeIfAbsent(nurseGroup, k -> new ArrayList<>())
            .add(Map.of("facilityName", facility, "name", name));
        }
      }
      if (!isBlank(clinGroup)) {
        for (String name : list) {
          clinicalGroupToUnits.computeIfAbsent(clinGroup, k -> new ArrayList<>())
            .add(Map.of("facilityName", facility, "name", name));
        }
      }
      if (!isBlank(facility) && !isBlank(noCare)) {
        noCaregiverByFacility.put(facility, noCare);
      }
    }
  }

  // ---------- Parse: Flow Sheets ----------
  private void parseFlowSheet(Workbook wb, String sheetName, boolean nurseSide) {
    Sheet sh = findSheet(wb, sheetName);
    if (sh == null) return;

    Row header = findHeaderRow(sh);
    Map<String,Integer> hm = headerMap(header);
    int start = firstDataRow(sh, header);

    int cCfg     = getCol(hm, "Configuration Group");
    int cAlarm   = getCol(hm, "Common Alert or Alarm Name", "Alarm Name");
    int cSend    = getCol(hm, "Sending System Alert Name", "Sending System Alarm Name");
    int cPriority= getCol(hm, "Priority");
    int cDevice  = getCol(hm, "Device - A", "Device");
    int cRing    = getCol(hm, "Ringtone Device - A", "Ringtone");
    int cResp    = getCol(hm, "Response Options", "Response Option");
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
      f.type        = nurseSide ? "NurseCalls" : "Clinicals";
      f.configGroup = getCell(row, cCfg);
      f.alarmName   = getCell(row, cAlarm);
      f.sendingName = getCell(row, cSend);
      f.priorityRaw = getCell(row, cPriority);
      f.deviceA     = getCell(row, cDevice);
      f.ringtone    = getCell(row, cRing);
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

  // ---------- Public JSON builders ----------
  public Map<String,Object> buildNurseCallsJson() {
    return buildNurseCallsJson(false);
  }
  public Map<String,Object> buildNurseCallsJson(boolean mergeIdenticalFlows) {
    return buildJson(nurseCalls, nurseGroupToUnits, true, mergeIdenticalFlows);
  }
  public Map<String,Object> buildClinicalsJson() {
    return buildClinicalsJson(false);
  }
  public Map<String,Object> buildClinicalsJson(boolean mergeIdenticalFlows) {
    return buildJson(clinicals, clinicalGroupToUnits, false, mergeIdenticalFlows);
  }

  // ---------- Build JSON core ----------
  private Map<String,Object> buildJson(List<FlowRow> rows,
                                       Map<String,List<Map<String,String>>> groupToUnits,
                                       boolean nurseSide,
                                       boolean mergeIdenticalFlows) {
    Map<String,Object> root = new LinkedHashMap<>();
    root.put("version", "1.1.0");
    root.put("alarmAlertDefinitions", buildAlarmDefs(rows, nurseSide));

    List<Map<String,Object>> flows;
    if (mergeIdenticalFlows) {
      flows = buildFlowsMerged(rows, groupToUnits, nurseSide);
    } else {
      flows = buildFlowsNormal(rows, groupToUnits, nurseSide);
    }
    root.put("deliveryFlows", flows);
    return root;
  }

  // ---------- Build flows (normal mode) - one flow per row ----------
  private List<Map<String,Object>> buildFlowsNormal(List<FlowRow> rows,
                                                     Map<String,List<Map<String,String>>> groupToUnits,
                                                     boolean nurseSide) {
    List<Map<String,Object>> flows = new ArrayList<>();
    for (FlowRow r : rows) {
      if (isBlank(r.configGroup) && isBlank(r.alarmName) && isBlank(r.sendingName)) continue;

      List<Map<String,String>> unitRefs = groupToUnits.getOrDefault(r.configGroup, List.of());
      String mappedPriority = mapPrioritySafe(r.priorityRaw);

      Map<String,Object> flow = new LinkedHashMap<>();
      flow.put("alarmsAlerts", List.of(nvl(r.alarmName, r.sendingName)));
      flow.put("conditions", nurseSide ? nurseConditions() : List.of());
      flow.put("destinations", buildDestinationsMerged(r, unitRefs, nurseSide));
      flow.put("interfaces", List.of(Map.of("componentName","OutgoingWCTP","referenceName","OutgoingWCTP")));
      flow.put("name", buildFlowName(nurseSide, mappedPriority, r, unitRefs));
      flow.put("parameterAttributes", buildParamAttributesQuoted(r, nurseSide, mappedPriority));
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
                                                     boolean nurseSide) {
    // Group rows by their "merge key" (identical delivery parameters)
    Map<String, List<FlowRow>> groupedByMergeKey = new LinkedHashMap<>();
    
    for (FlowRow r : rows) {
      if (isBlank(r.configGroup) && isBlank(r.alarmName) && isBlank(r.sendingName)) continue;
      
      String mergeKey = buildMergeKey(r, groupToUnits);
      groupedByMergeKey.computeIfAbsent(mergeKey, k -> new ArrayList<>()).add(r);
    }

    // Build one flow per merge group
    List<Map<String,Object>> flows = new ArrayList<>();
    for (List<FlowRow> group : groupedByMergeKey.values()) {
      if (group.isEmpty()) continue;
      
      // Use the first row as the template
      FlowRow template = group.get(0);
      List<Map<String,String>> unitRefs = groupToUnits.getOrDefault(nvl(template.configGroup, ""), List.of());
      String mappedPriority = mapPrioritySafe(template.priorityRaw);

      // Collect all alarm names from the group
      List<String> alarmNames = group.stream()
        .map(r -> nvl(r.alarmName, r.sendingName))
        .distinct()
        .collect(Collectors.toList());

      Map<String,Object> flow = new LinkedHashMap<>();
      flow.put("alarmsAlerts", alarmNames);
      flow.put("conditions", nurseSide ? nurseConditions() : List.of());
      flow.put("destinations", buildDestinationsMerged(template, unitRefs, nurseSide));
      flow.put("interfaces", List.of(Map.of("componentName","OutgoingWCTP","referenceName","OutgoingWCTP")));
      flow.put("name", buildFlowNameMerged(nurseSide, mappedPriority, alarmNames, template, unitRefs));
      flow.put("parameterAttributes", buildParamAttributesQuoted(template, nurseSide, mappedPriority));
      flow.put("priority", mappedPriority.isEmpty() ? "normal" : mappedPriority);
      flow.put("status", "Active");
      if (!unitRefs.isEmpty()) flow.put("units", unitRefs);
      flows.add(flow);
    }
    return flows;
  }

  // ---------- Build merge key for grouping flows with identical delivery parameters ----------
  private String buildMergeKey(FlowRow r, Map<String,List<Map<String,String>>> groupToUnits) {
    List<Map<String,String>> unitRefs = groupToUnits.getOrDefault(nvl(r.configGroup, ""), List.of());
    String mappedPriority = mapPrioritySafe(r.priorityRaw);
    
    // Build a key from: priority, device, ringtone, recipients (r1-r5), timing (t1-t5), units
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
    key.append("units=").append(unitsKey);
    
    return key.toString();
  }

  // ---------- Build flow name for merged flows ----------
  private String buildFlowNameMerged(boolean nurseSide,
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
    parts.add(nurseSide ? "SEND NURSECALL" : "SEND CLINICAL");
    if (!isBlank(mappedPriority)) parts.add(mappedPriority.toUpperCase(Locale.ROOT));
    
    // If multiple alarms, show count instead of listing all
    if (alarmNames.size() == 1) {
      parts.add(alarmNames.get(0));
    } else {
      parts.add("(" + alarmNames.size() + " alarms)");
    }
    
    if (!isBlank(group)) parts.add(group);
    if (!unitNames.isEmpty()) parts.add(String.join(" / ", unitNames));
    else if (!isBlank(facility)) parts.add(facility);
    return String.join(" | ", parts);
  }

  private List<Map<String,Object>> buildAlarmDefs(List<FlowRow> rows, boolean nurseSide) {
    Map<String, Map<String,Object>> byKey = new LinkedHashMap<>();
    for (FlowRow r : rows) {
      String name = nvl(r.alarmName, r.sendingName);
      if (isBlank(name)) continue;
      String key = name + "|" + (nurseSide ? "N" : "C");
      if (byKey.containsKey(key)) continue;

      Map<String,Object> def = new LinkedHashMap<>();
      def.put("name", name);
      def.put("type", nurseSide ? "NurseCalls" : "Clinicals");

      Map<String,String> val = new LinkedHashMap<>();
      val.put("category", "");
      val.put("value", name);
      def.put("values", List.of(val));
      byKey.put(key, def);
    }
    return new ArrayList<>(byKey.values());
  }

  // ---------- Destinations: one per order, merge groups ----------
  private List<Map<String,Object>> buildDestinationsMerged(FlowRow r,
                                                           List<Map<String,String>> unitRefs,
                                                           boolean nurseSide) {
    String facility = unitRefs.isEmpty() ? "" : unitRefs.get(0).getOrDefault("facilityName", "");
    List<Map<String,Object>> out = new ArrayList<>();
    addOrder(out, facility, 0, r.t1, r.r1);
    addOrder(out, facility, 1, r.t2, r.r2);
    addOrder(out, facility, 2, r.t3, r.r3);
    addOrder(out, facility, 3, r.t4, r.r4);
    addOrder(out, facility, 4, r.t5, r.r5);

    if (!nurseSide && !isBlank(facility)) {
      String noCare = noCaregiverByFacility.getOrDefault(facility, "");
      if (!isBlank(noCare)) {
        Map<String,Object> d = new LinkedHashMap<>();
        d.put("order", out.size());
        d.put("delayTime", 0);
        d.put("destinationType", "NoDeliveries");
        d.put("users", List.of());
        d.put("functionalRoles", List.of());
        d.put("groups", List.of(Map.of("facilityName", facility, "name", noCare)));
        d.put("presenceConfig", "device");
        d.put("recipientType", "group");
        out.add(d);
      }
    }
    return out;
  }

  private void addOrder(List<Map<String,Object>> out,
                        String facility,
                        int order,
                        String delayText,
                        String recipientText) {
    if (isBlank(recipientText)) return;
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

    if (!roles.isEmpty()) {
      dest.put("presenceConfig", "user_and_device");
      dest.put("recipientType", "functional_role");
    } else {
      dest.put("presenceConfig", "device");
      dest.put("recipientType", "group");
    }
    out.add(dest);
  }

  // ---------- Parameter Attributes (correct Engage syntax) ----------
  private List<Map<String,Object>> buildParamAttributesQuoted(FlowRow r,
                                                              boolean nurseSide,
                                                              String mappedPriority) {
    List<Map<String,Object>> params = new ArrayList<>();
    boolean urgent = "urgent".equalsIgnoreCase(mappedPriority);

    if (!isBlank(r.ringtone)) {
      params.add(paQ("alertSound", r.ringtone));
    }

    if (nurseSide) {
      boolean hasAccept   = containsWord(r.responseOptions, "accept");
      boolean hasEscalate = containsWord(r.responseOptions, "escalate");
      boolean hasCallBack = containsWord(r.responseOptions, "call back") || containsWord(r.responseOptions, "callback");
      boolean noResponse  = containsWord(r.responseOptions, "no response");

      if (hasAccept) {
        params.add(paQ("accept", "Accepted"));
        params.add(paLiteral("acceptBadgePhrases", "[\"Accept\"]"));
      }
      if (hasCallBack) {
        params.add(paQ("acceptAndCall", "Call Back"));
      }
      if (hasEscalate) {
        params.add(paQ("decline", "Decline Primary"));
        params.add(paLiteral("declineBadgePhrases", "[\"Escalate\"]"));
      }

      params.add(paQ("breakThrough", urgent ? "voceraAndDevice" : "none"));
      params.add(paLiteralBool("enunciate", true));
      params.add(paQ("message", "Patient: #{bed.patient.last_name}, #{bed.patient.first_name}\\nRoom/Bed: #{bed.room.name} - #{bed.bed_number}"));
      params.add(paQ("patientMRN", "#{bed.patient.mrn}:#{bed.patient.visit_number}"));
      params.add(paQ("placeUid", "#{bed.uid}"));
      params.add(paQ("patientName", "#{bed.patient.first_name} #{bed.patient.middle_name} #{bed.patient.last_name}"));
      params.add(paLiteralBool("popup", true));
      params.add(paQ("eventIdentification", "NurseCalls:#{id}"));
      params.add(paQ("responseType", noResponse ? "None" : "Accept/Decline"));

      if (!noResponse) {
        params.add(paQ("respondingLine", "responses.line.number"));
        params.add(paQ("respondingUser", "responses.usr.login"));
        params.add(paQ("responsePath", "responses.action"));
      }

      params.add(paQ("shortMessage", "#{alert_type} #{bed.room.name}"));
      params.add(paQ("subject", "#{alert_type} #{bed.room.name}"));
      params.add(paLiteral("ttl", "10"));
      params.add(paLiteral("retractRules", "[\"ttlHasElapsed\"]"));
      params.add(paQ("vibrate", "short"));

      addDestNameParam(params, 0, firstToken(r.r1));
      addDestNameParam(params, 1, firstToken(r.r2));
      addDestNameParam(params, 2, firstToken(r.r3));
      addDestNameParam(params, 3, firstToken(r.r4));
      addDestNameParam(params, 4, firstToken(r.r5));

    } else {
      params.add(paOrderQ(0, "destinationName", "Nurse Alert"));
      params.add(paQ("alertSound", nvl(r.ringtone, "Vocera Tone 0 Long")));
      params.add(paQ("responseAllowed", "false"));
      params.add(paQ("breakThrough", urgent ? "voceraAndDevice" : "none"));
      params.add(paLiteralBool("enunciate", true));
      params.add(paQ("message", "Clinical Alert ${destinationName}\\nRoom: #{bed.room.name} - #{bed.bed_number}\\nAlert Type: #{alert_type}\\nAlarm Time: #{alarm_time.as_time}"));
      params.add(paQ("patientMRN", "#{clinical_patient.mrn}:#{clinical_patient.visit_number}"));
      params.add(paQ("patientName", "#{clinical_patient.first_name} #{clinical_patient.middle_name} #{clinical_patient.last_name}"));
      params.add(paQ("placeUid", "#{bed.uid}"));
      params.add(paLiteralBool("popup", true));
      params.add(paQ("eventIdentification", "#{id}"));
      params.add(paQ("responseType", "None"));
      params.add(paQ("shortMessage", "#{alert_type} #{bed.room.name}"));
      params.add(paQ("subject", "#{alert_type} #{bed.room.name}"));
      params.add(paLiteral("ttl", "10"));
      params.add(paLiteral("retractRules", "[\"ttlHasElapsed\"]"));
      params.add(paQ("vibrate", "short"));
      params.add(paOrderQ(1, "destinationName", "NoCaregivers"));
      params.add(paOrderQ(1, "message", "#{alert_type}\\nIssue: A Clinical Alert has been received without any caregivers assigned to room."));
      params.add(paOrderQ(1, "subject", "NoCaregiver assigned for #{alert_type} #{bed.room.name}"));
    }
    return params;
  }

  private void addDestNameParam(List<Map<String,Object>> params, int order, String raw) {
    if (isBlank(raw)) return;
    String lower = raw.toLowerCase(Locale.ROOT);
    String value;
    if (lower.startsWith("vassign")) {
      value = raw.replaceFirst("(?i)^\\s*vassign\\s*:\\s*\\[\\s*room\\s*]\\s*", "").trim();
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

  // ---------- Flow name ----------
  private String buildFlowName(boolean nurseSide,
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
    parts.add(nurseSide ? "SEND NURSECALL" : "SEND CLINICAL");
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
  public void writeJson(File summaryFile) throws Exception {
    ensureParent(summaryFile);
    Map<String,Object> summary = new LinkedHashMap<>();
    summary.put("note", "This build now writes two separate JSON files: NurseCalls.json and Clinicals.json");
    summary.put("nurseCalls", nurseCalls.size());
    summary.put("clinicals", clinicals.size());
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
        set(row,4,u.noCareGroup);
        set(row,5,u.comments);
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

      try (FileOutputStream fos = new FileOutputStream(dest)) {
        wb.write(fos);
      }
    }
  }

  private static String[] flowHeaders() {
    return new String[]{
      "Configuration Group",
      "Common Alert or Alarm Name",
      "Sending System Alert Name",
      "Priority",
      "Device - A",
      "Ringtone Device - A",
      "Response Options",
      "Time to 1st Recipient","1st Recipient",
      "Time to 2nd Recipient","2nd Recipient",
      "Time to 3rd Recipient","3rd Recipient",
      "Time to 4th Recipient","4th Recipient",
      "Time to 5th Recipient","5th Recipient"
    };
  }

  private static void writeFlowRow(Row row, FlowRow f) {
    set(row,0,f.configGroup);
    set(row,1,f.alarmName);
    set(row,2,f.sendingName);
    set(row,3,f.priorityRaw);
    set(row,4,f.deviceA);
    set(row,5,f.ringtone);
    set(row,6,f.responseOptions);
    set(row,7,f.t1); set(row,8,f.r1);
    set(row,9,f.t2); set(row,10,f.r2);
    set(row,11,f.t3); set(row,12,f.r3);
    set(row,13,f.t4); set(row,14,f.r4);
    set(row,15,f.t5); set(row,16,f.r5);
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
    if (text.isEmpty()) {
      return new ParsedRecipient(defaultFacility == null ? "" : defaultFacility, "", false);
    }
    String facility = defaultFacility == null ? "" : defaultFacility;
    String valuePortion = text;

    String lower = text.toLowerCase(Locale.ROOT);
    boolean isFunctionalRole = false;

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
    valuePortion = valuePortion.replaceAll("(?i)^\\[\\s*room\\s*]\\s*", "").trim();
    return new ParsedRecipient(facility, valuePortion, isFunctionalRole);
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
  private static List<String> splitUnits(String s) {
    if (s == null) return List.of();
    return Arrays.stream(s.split("[,;/\\n]"))
      .map(String::trim).filter(v -> !v.isEmpty())
      .collect(Collectors.toCollection(LinkedHashSet::new))
      .stream().toList();
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

  private static String mapPrioritySafe(String priority) {
    if (priority == null) return "";
    String norm = priority.trim().toLowerCase(Locale.ROOT)
      .replace("(edge)", " edge")
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
