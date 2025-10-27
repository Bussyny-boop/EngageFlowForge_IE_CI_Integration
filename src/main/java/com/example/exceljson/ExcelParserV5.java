package com.example.exceljson;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * ExcelParserV5
 *
 * - Parses the Engage Excel workbook.
 * - Outputs JSON matching the approved structure:
 *   * NurseCallsCondition for nurse flows' "conditions".
 *   * Ignores blank / "N/A" cells.
 *   * For each recipient order, aggregates multiple recipients into a single destination:
 *       - Groups → one destination with "groups":[...]
 *       - Functional roles → one destination with "functionalRoles":[...]
 *   * ParameterAttributes follow the reference format (quoted string values where required).
 */
public class ExcelParserV5 {

    // ---------------------------------------------------------------------
    // Row types exposed to the JavaFX UI
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

    private static final ObjectMapper PRETTY_MAPPER = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    // NurseCallsCondition (approved structure)
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

        NURSE_CONDITIONS = Collections.unmodifiableList(List.of(cond));
    }

    private final Config config;
    private final Pattern functionalRolePattern;

    public final List<UnitRow> units = new ArrayList<>();
    public final List<FlowRow> nurseCalls = new ArrayList<>();
    public final List<FlowRow> clinicals = new ArrayList<>();

    private final Map<String, List<Map<String, String>>> nurseGroupToUnits = new LinkedHashMap<>();
    private final Map<String, List<Map<String, String>>> clinicalGroupToUnits = new LinkedHashMap<>();
    private final Map<String, String> noCaregiverByFacility = new LinkedHashMap<>();

    private static final String SHEET_UNIT = "Unit Breakdown";
    private static final String SHEET_NURSE = "Nurse Call";
    private static final String SHEET_CLINICAL = "Patient Monitoring";

    public ExcelParserV5() {
        this(Config.defaultConfig());
    }
    public ExcelParserV5(Config config) {
        this.config = (config == null) ? Config.defaultConfig() : config;
        this.functionalRolePattern = Pattern.compile(this.config.recipientParsing.functionalRoleRegex);
    }

    // ------------------------------------------------------------------
    // Loading
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
                "✅ Excel Load Complete\n\n" +
                "Loaded:\n" +
                "  • %d Unit Breakdown rows\n" +
                "  • %d Nurse Call rows\n" +
                "  • %d Patient Monitoring rows\n\n" +
                "Linked:\n" +
                "  • %d Configuration Groups (Nurse)\n" +
                "  • %d Configuration Groups (Clinical)",
                units.size(), nurseCalls.size(), clinicals.size(),
                nurseGroupToUnits.size(), clinicalGroupToUnits.size());
    }

    // ------------------------------------------------------------------
    // Parsing helpers
    private void parseUnitBreakdown(Workbook wb) {
        Sheet sheet = findSheet(wb, SHEET_UNIT);
        if (sheet == null) return;

        Row header = findHeaderRow(sheet);
        Map<String, Integer> columns = headerMap(header);

        int startRow = firstDataRow(sheet, header);
        int cFacility = getCol(columns, "Facility");
        int cUnitName = getCol(columns, "Common Unit Name");
        int cNurseGroup = getCol(columns, "Nurse Call", "Configuration Group", "Nurse call");
        int cClinGroup = getCol(columns, "Patient Monitoring", "Configuration Group", "Patient monitoring");
        int cNoCare = getCol(columns, "No Caregiver Alert Number or Group", "No Caregiver Group");
        int cComments = getCol(columns, "Comments");

        for (int r = startRow; r <= sheet.getLastRowNum(); r++) {
            Row row = sheet.getRow(r);
            if (row == null) continue;

            String facility = getCell(row, cFacility);
            String unitNames = getCell(row, cUnitName);
            String nurseGroup = getCell(row, cNurseGroup);
            String clinGroup = getCell(row, cClinGroup);
            String noCare = stripVGroup(getCell(row, cNoCare));
            String comments = getCell(row, cComments);

            if (isBlank(facility) && isBlank(unitNames)) continue;

            UnitRow unit = new UnitRow();
            unit.facility = facility;
            unit.unitNames = unitNames;
            unit.nurseGroup = nurseGroup;
            unit.clinGroup = clinGroup;
            unit.noCareGroup = noCare;
            unit.comments = comments;
            units.add(unit);

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

    private void parseFlowSheet(Workbook wb, String sheetName, boolean nurseSide) {
        Sheet sheet = findSheet(wb, sheetName);
        if (sheet == null) return;

        Row header = findHeaderRow(sheet);
        Map<String, Integer> columns = headerMap(header);
        int startRow = firstDataRow(sheet, header);

        int cCfg = getCol(columns, "Configuration Group");
        int cAlarm = getCol(columns, "Common Alert or Alarm Name", "Alarm Name");
        int cSend = getCol(columns, "Sending System Alert Name", "Sending System Alarm Name");
        int cPriority = getCol(columns, "Priority");
        int cDevice = getCol(columns, "Device - A", "Device");
        int cRing = getCol(columns, "Ringtone Device - A", "Ringtone");
        int cResp = getCol(columns, "Response Options", "Response Option");
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

            FlowRow flow = new FlowRow();
            flow.type = nurseSide ? "NurseCalls" : "Clinicals";
            flow.configGroup = getCell(row, cCfg);
            flow.alarmName = getCell(row, cAlarm);
            flow.sendingName = getCell(row, cSend);
            flow.priorityRaw = getCell(row, cPriority);
            flow.deviceA = getCell(row, cDevice);
            flow.ringtone = getCell(row, cRing);
            flow.responseOptions = getCell(row, cResp);
            flow.t1 = getCell(row, cT1); flow.r1 = getCell(row, cR1);
            flow.t2 = getCell(row, cT2); flow.r2 = getCell(row, cR2);
            flow.t3 = getCell(row, cT3); flow.r3 = getCell(row, cR3);
            flow.t4 = getCell(row, cT4); flow.r4 = getCell(row, cR4);
            flow.t5 = getCell(row, cT5); flow.r5 = getCell(row, cR5);

            if (isBlank(flow.alarmName) && isBlank(flow.sendingName)) continue;

            if (nurseSide) nurseCalls.add(flow); else clinicals.add(flow);
        }
    }

    // ------------------------------------------------------------------
    // JSON builders
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

            Map<String, Object> flow = new LinkedHashMap<>();
            String mappedPriority = mapPrioritySafe(row.priorityRaw);
            flow.put("name", buildFlowName(nurseSide, mappedPriority, row, unitRefs));
            if (!isBlank(mappedPriority)) flow.put("priority", mappedPriority);
            flow.put("status", "Active");
            flow.put("alarmsAlerts", List.of(nvl(row.alarmName, row.sendingName)));
            flow.put("interfaces", List.of(Map.of(
                    "componentName", "OutgoingWCTP",
                    "referenceName", "OutgoingWCTP")));
            flow.put("parameterAttributes", buildParamAttributes(row, nurseSide, mappedPriority));
            flow.put("destinations", buildDestinationsAggregated(row, unitRefs, nurseSide));
            if (!unitRefs.isEmpty()) flow.put("units", unitRefs);
            flow.put("conditions", nurseSide ? nurseConditions() : List.of());

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
        if (!unitNames.isEmpty()) parts.add(String.join(" / ", unitNames));
        else if (!isBlank(facility)) parts.add(facility);

        return String.join(" | ", parts);
    }

    private List<Map<String, Object>> nurseConditions() {
        // defensive copy
        return NURSE_CONDITIONS.stream().map(LinkedHashMap::new).collect(Collectors.toList());
    }

    // ------------------------------------------------------------------
    // Destinations (AGGREGATED per order)
    private List<Map<String, Object>> buildDestinationsAggregated(FlowRow row,
                                                                  List<Map<String, String>> unitRefs,
                                                                  boolean nurseSide) {
        List<Map<String, Object>> destinations = new ArrayList<>();
        String facility = unitRefs.isEmpty() ? "" : unitRefs.get(0).getOrDefault("facilityName", "");

        addAggregatedDestination(destinations, facility, 0, row.t1, row.r1);
        addAggregatedDestination(destinations, facility, 1, row.t2, row.r2);
        addAggregatedDestination(destinations, facility, 2, row.t3, row.r3);
        addAggregatedDestination(destinations, facility, 3, row.t4, row.r4);
        addAggregatedDestination(destinations, facility, 4, row.t5, row.r5);

        // Clinicals: add NoDeliveries (NoCaregivers) destination at the end if configured
        if (!nurseSide && !facility.isEmpty()) {
            String noCare = noCaregiverByFacility.getOrDefault(facility, "");
            if (!isBlank(noCare)) {
                Map<String, Object> d = new LinkedHashMap<>();
                d.put("order", destinations.size());
                d.put("delayTime", 0);
                d.put("destinationType", "NoDeliveries");
                d.put("users", List.of());
                d.put("functionalRoles", List.of());
                d.put("groups", List.of(Map.of("facilityName", facility, "name", noCare)));
                d.put("presenceConfig", "device");
                d.put("recipientType", "group");
                destinations.add(d);
            }
        }
        return destinations;
    }

    private void addAggregatedDestination(List<Map<String, Object>> out,
                                          String defaultFacility,
                                          int order,
                                          String delayText,
                                          String recipientText) {
        // Ignore if recipient is blank / "N/A"
        if (isBlank(recipientText)) return;

        List<String> parts = Arrays.stream(recipientText.split("[,;\\n]"))
                .map(String::trim)
                .filter(s -> !s.isEmpty() && !"N/A".equalsIgnoreCase(s) && !"NA".equalsIgnoreCase(s))
                .collect(Collectors.toList());
        if (parts.isEmpty()) return;

        // Accumulate groups and roles separately for this order
        List<Map<String, String>> groups = new ArrayList<>();
        List<Map<String, String>> roles  = new ArrayList<>();

        for (String recipient : parts) {
            ParsedRecipient pr = parseRecipient(recipient, defaultFacility);
            if (isBlank(pr.value)) continue;
            if (pr.isFunctionalRole) {
                roles.add(Map.of("facilityName", pr.facility, "name", pr.value));
            } else {
                groups.add(Map.of("facilityName", pr.facility, "name", pr.value));
            }
        }

        int delay = parseDelay(delayText);

        if (!groups.isEmpty()) {
            Map<String, Object> d = new LinkedHashMap<>();
            d.put("order", order);
            d.put("delayTime", delay);
            d.put("destinationType", "Normal");
            d.put("users", List.of());
            d.put("functionalRoles", List.of());   // none here
            d.put("groups", groups);               // aggregated groups
            d.put("presenceConfig", "device");
            d.put("recipientType", "group");
            out.add(d);
        }
        if (!roles.isEmpty()) {
            Map<String, Object> d = new LinkedHashMap<>();
            d.put("order", order);
            d.put("delayTime", delay);
            d.put("destinationType", "Normal");
            d.put("users", List.of());
            d.put("functionalRoles", roles);       // aggregated roles
            d.put("groups", List.of());            // none here
            d.put("presenceConfig", "user_and_device");
            d.put("recipientType", "functional_role");
            out.add(d);
        }
    }

    // ------------------------------------------------------------------
    // Parameter Attributes (match reference formatting)
    private List<Map<String, Object>> buildParamAttributes(FlowRow row,
                                                           boolean nurseSide,
                                                           String mappedPriority) {
        List<Map<String, Object>> params = new ArrayList<>();
        boolean urgent = "urgent".equalsIgnoreCase(mappedPriority);

        // Ringtone → quoted string value
        if (!isBlank(row.ringtone)) {
            params.add(pa("alertSound", quote(row.ringtone)));
        }

        if (nurseSide) {
            boolean hasAccept   = containsWord(row.responseOptions, "accept");
            boolean hasEscalate = containsWord(row.responseOptions, "escalate");
            boolean hasCallBack = containsWord(row.responseOptions, "call back") || containsWord(row.responseOptions, "callback");
            boolean noResponse  = containsWord(row.responseOptions, "no response");

            if (hasAccept) {
                params.add(pa("accept", quote("Accepted")));
                params.add(pa("acceptBadgePhrases", "[\"Accept\"]"));
            }
            if (hasCallBack) {
                params.add(pa("acceptAndCall", quote("Call Back")));
            }
            if (hasEscalate) {
                params.add(pa("decline", quote("Decline Primary")));
                params.add(pa("declineBadgePhrases", "[\"Escalate\"]"));
            }

            params.add(pa("breakThrough", quote(urgent ? "voceraAndDevice" : "none")));
            params.add(pa("enunciate", "true"));
            params.add(pa("message", quote("Patient: #{bed.patient.last_name}, #{bed.patient.first_name}\\nRoom/Bed: #{bed.room.name} - #{bed.bed_number}")));
            params.add(pa("patientMRN", quote("#{bed.patient.mrn}:#{bed.patient.visit_number}")));
            params.add(pa("placeUid", quote("#{bed.uid}")));
            params.add(pa("patientName", quote("#{bed.patient.first_name} #{bed.patient.middle_name} #{bed.patient.last_name}")));
            params.add(pa("popup", "true"));
            params.add(pa("eventIdentification", quote("NurseCalls:#{id}")));
            params.add(pa("responseType", quote(noResponse ? "None" : "Accept/Decline")));
            params.add(pa("shortMessage", quote("#{alert_type} #{bed.room.name}")));
            params.add(pa("subject", quote("#{alert_type} #{bed.room.name}")));
            params.add(pa("ttl", "10"));
            params.add(pa("retractRules", "[\"ttlHasElapsed\"]"));
            params.add(pa("vibrate", quote("short")));

            // destinationName by order:
            addDestinationNameParams(params, 0, row.r1);
            addDestinationNameParams(params, 1, row.r2);
            addDestinationNameParams(params, 2, row.r3);
            addDestinationNameParams(params, 3, row.r4);
            addDestinationNameParams(params, 4, row.r5);

        } else {
            // Clinicals default parameters
            params.add(paOrder(0, "destinationName", quote("Nurse Alert")));
            params.add(pa("alertSound", quote(nvl(row.ringtone, "Vocera Tone 0 Long"))));
            params.add(pa("responseAllowed", "false"));
            params.add(pa("breakThrough", quote(urgent ? "voceraAndDevice" : "none")));
            params.add(pa("enunciate", "true"));
            params.add(pa("message", quote("Clinical Alert ${destinationName}\\nRoom: #{bed.room.name} - #{bed.bed_number}\\nAlert Type: #{alert_type}\\nAlarm Time: #{alarm_time.as_time}")));
            params.add(pa("patientMRN", quote("#{clinical_patient.mrn}:#{clinical_patient.visit_number}")));
            params.add(pa("patientName", quote("#{clinical_patient.first_name} #{clinical_patient.middle_name} #{clinical_patient.last_name}")));
            params.add(pa("placeUid", quote("#{bed.uid}")));
            params.add(pa("popup", "true"));
            params.add(pa("eventIdentification", quote("#{id}")));
            params.add(pa("responseType", quote("None")));
            params.add(pa("shortMessage", quote("#{alert_type} #{bed.room.name}")));
            params.add(pa("subject", quote("#{alert_type} #{bed.room.name}")));
            params.add(pa("ttl", "10"));
            params.add(pa("retractRules", "[\"ttlHasElapsed\"]"));
            params.add(pa("vibrate", quote("short")));

            // NoCaregivers destination params (order 1)
            params.add(paOrder(1, "destinationName", quote("NoCaregivers")));
            params.add(paOrder(1, "message", quote("#{alert_type}\\nIssue: A Clinical Alert has been received without any caregivers assigned to room.\\nRoom/Bed: #{bed.room.name} - #{bed.bed_number} \\nAlarm Time: #{alarm_time.as_time}")));
            params.add(paOrder(1, "shortMessage", quote("No Caregivers Assigned for #{alert_type} in #{bed.room.name} #{bed.bed_number}")));
            params.add(paOrder(1, "subject", quote("Alert Without Caregivers")));
        }

        return params;
    }

    private void addDestinationNameParams(List<Map<String, Object>> params, int order, String recipientsRaw) {
        if (isBlank(recipientsRaw)) return;

        // If any group exists at this order → use "Group"
        // Else if at least one functional role → use that role name.
        List<String> tokens = Arrays.stream(recipientsRaw.split("[,;\\n]"))
                .map(String::trim)
                .filter(s -> !s.isEmpty() && !"N/A".equalsIgnoreCase(s) && !"NA".equalsIgnoreCase(s))
                .collect(Collectors.toList());
        if (tokens.isEmpty()) return;

        boolean hasGroup = false;
        String firstRole = null;

        for (String t : tokens) {
            ParsedRecipient pr = parseRecipient(t, "");
            if (isBlank(pr.value)) continue;
            if (pr.isFunctionalRole) {
                if (firstRole == null) firstRole = pr.value;
            } else {
                hasGroup = true;
            }
        }

        if (hasGroup) {
            params.add(paOrder(order, "destinationName", quote("Group")));
        } else if (firstRole != null) {
            params.add(paOrder(order, "destinationName", quote(firstRole)));
        }
    }

    // ------------------------------------------------------------------
    // File writers
    public void writeNurseCallsJson(File nurseFile) throws Exception {
        ensureParent(nurseFile);
        try (FileWriter out = new FileWriter(nurseFile, false)) {
            out.write(pretty(buildNurseCallsJson()));
        }
    }

    public void writeClinicalsJson(File clinicalFile) throws Exception {
        ensureParent(clinicalFile);
        try (FileWriter out = new FileWriter(clinicalFile, false)) {
            out.write(pretty(buildClinicalsJson()));
        }
    }

    /** Compatibility summary writer (single file notice) */
    public void writeJson(File summaryFile) throws Exception {
        ensureParent(summaryFile);
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("note", "This build writes two separate JSON files: NurseCalls.json and Clinicals.json");
        summary.put("nurseCalls", nurseCalls.size());
        summary.put("clinicals", clinicals.size());
        try (FileWriter out = new FileWriter(summaryFile, false)) {
            out.write(pretty(summary));
        }
    }

    // ------------------------------------------------------------------
    // Pretty printing helpers
    public static String pretty(Map<String, Object> value) throws JsonProcessingException {
        return PRETTY_MAPPER.writeValueAsString(value);
    }
    public static String pretty(Map<String, Object> value, int indentLevel) throws JsonProcessingException {
        String pretty = pretty(value);
        if (indentLevel <= 0) return pretty;
        String indent = "  ".repeat(indentLevel);
        return Arrays.stream(pretty.split("\\R"))
                .map(line -> indent + line)
                .collect(Collectors.joining("\n"));
    }

    // ------------------------------------------------------------------
    // Utility helpers
    private Sheet findSheet(Workbook wb, String name) {
        if (wb == null || name == null) return null;
        for (int i = 0; i < wb.getNumberOfSheets(); i++) {
            Sheet sh = wb.getSheetAt(i);
            if (sh != null && name.equalsIgnoreCase(sh.getSheetName())) return sh;
        }
        return null;
    }

    private Row findHeaderRow(Sheet sheet) {
        if (sheet == null) return null;
        int start = Math.max(0, 2);
        int end = Math.min(sheet.getLastRowNum(), start + 3);
        for (int r = start; r <= end; r++) {
            Row row = sheet.getRow(r);
            if (row == null) continue;
            int nonEmpty = 0;
            for (int c = 0; c < row.getLastCellNum(); c++) {
                String v = getCell(row, c);
                if (!v.isBlank()) nonEmpty++;
            }
            if (nonEmpty >= 3) return row;
        }
        for (int r = 0; r <= sheet.getLastRowNum(); r++) {
            Row row = sheet.getRow(r);
            if (row != null) return row;
        }
        return null;
    }

    private int firstDataRow(Sheet sheet, Row header) {
        if (sheet == null) return 0;
        int firstRow = Math.max(sheet.getFirstRowNum(), 0);
        if (header != null) {
            int afterHeader = header.getRowNum() + 1;
            if (afterHeader > firstRow) return afterHeader;
        }
        return firstRow;
    }

    private Map<String, Integer> headerMap(Row header) {
        Map<String, Integer> map = new LinkedHashMap<>();
        if (header == null) return map;
        for (int c = 0; c < header.getLastCellNum(); c++) {
            String value = getCell(header, c);
            if (!value.isBlank()) map.put(normalize(value), c);
        }
        return map;
    }

    private int getCol(Map<String, Integer> map, String... candidates) {
        if (map.isEmpty()) return -1;
        for (String candidate : candidates) {
            if (candidate == null) continue;
            String norm = normalize(candidate);
            if (map.containsKey(norm)) return map.get(norm);
        }
        // fallback partial
        for (String candidate : candidates) {
            if (candidate == null) continue;
            String norm = normalize(candidate);
            for (Map.Entry<String, Integer> e : map.entrySet()) {
                if (e.getKey().contains(norm)) return e.getValue();
            }
        }
        return -1;
    }

    private static String getCell(Row row, int column) {
        if (row == null || column < 0) return "";
        try {
            Cell cell = row.getCell(column);
            if (cell == null) return "";
            String value = switch (cell.getCellType()) {
                case STRING -> cell.getStringCellValue().trim();
                case NUMERIC -> String.valueOf(cell.getNumericCellValue());
                case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
                case FORMULA -> cell.getCellFormula();
                default -> "";
            };
            // Normalize: treat "N/A"/"NA"/blank as empty
            if (value.equalsIgnoreCase("N/A") || value.equalsIgnoreCase("NA") || value.isBlank()) return "";
            return value.trim();
        } catch (Exception e) {
            return "";
        }
    }

    private List<String> splitUnits(String unitNames) {
        if (isBlank(unitNames)) return List.of();
        Set<String> names = new LinkedHashSet<>();
        for (String token : unitNames.split("[,;/\\n]")) {
            String t = token.trim();
            if (!t.isEmpty()) names.add(t);
        }
        if (names.isEmpty()) names.add(unitNames.trim());
        return new ArrayList<>(names);
    }

    private String stripVGroup(String value) {
        if (value == null) return "";
        return value.replaceFirst("(?i)^v(group|assign)[: ]*", "").trim();
    }

    private ParsedRecipient parseRecipient(String raw, String defaultFacility) {
        String text = raw == null ? "" : raw.trim();
        if (text.isEmpty()) {
            String facility = defaultFacility == null ? "" : defaultFacility;
            return new ParsedRecipient(facility, "", false);
        }

        String facility = defaultFacility == null ? "" : defaultFacility;
        String valuePortion = text;

        // Optional <Facility>:: <Value> or <Facility>: <Value>
        String lower = text.toLowerCase(Locale.ROOT);
        int sepIdx = -1, sepLen = 0;
        if (lower.contains("::")) { sepIdx = lower.indexOf("::"); sepLen = 2; }
        else {
            int colon = lower.indexOf(':');
            if (colon >= 0) { sepIdx = colon; sepLen = 1; }
        }
        if (sepIdx > 0) {
            String prefix = text.substring(0, sepIdx).trim();
            String suffix = text.substring(sepIdx + sepLen).trim();
            if (!prefix.isEmpty() && !suffix.isEmpty()
                    && !prefix.equalsIgnoreCase("vgroup")
                    && !prefix.equalsIgnoreCase("vassign")) {
                facility = prefix;
                valuePortion = suffix;
            }
        }

        // Remove leading VGroup:/VAssign:[Room]
        String cleaned = valuePortion
                .replaceAll("(?i)^\\s*vgroup\\s*:\\s*", "")
                .replaceAll("(?i)^\\s*vassign\\s*:\\s*\\[\\s*room\\s*]\\s*", "")
                .trim();
        if (cleaned.isEmpty()) cleaned = stripVGroup(valuePortion);
        else cleaned = stripVGroup(cleaned);
        cleaned = cleaned.replaceFirst("(?i)^\\[\\s*room\\s*]\\s*", "").trim();

        Matcher matcher = functionalRolePattern.matcher(valuePortion);
        boolean functionalRole = matcher.find(); // per your regex config
        return new ParsedRecipient(facility, cleaned, functionalRole);
    }

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

    private void ensureParent(File file) throws IOException {
        if (file == null) throw new IOException("File reference is null");
        File parent = file.getAbsoluteFile().getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IOException("Unable to create directory: " + parent.getAbsolutePath());
        }
    }

    private String normalize(String header) {
        return header == null ? "" : header.trim().toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", " ").trim();
    }

    private String nvl(String first, String second) {
        return isBlank(first) ? (second == null ? "" : second) : first;
    }

    private String mapPrioritySafe(String priority) {
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

    private boolean containsWord(String text, String probe) {
        if (isBlank(text) || isBlank(probe)) return false;
        return text.toLowerCase(Locale.ROOT).contains(probe.toLowerCase(Locale.ROOT));
    }

    private boolean isBlank(String v) {
        return v == null || v.trim().isEmpty();
    }

    private int parseDelay(String text) {
        if (isBlank(text)) return 0;
        try {
            String digits = text.replaceAll("[^0-9]", "");
            if (digits.isEmpty()) return 0;
            return Integer.parseInt(digits);
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    // ----- parameterAttribute helpers -----
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
    private static String quote(String s) {
        if (s == null) return "\"\"";
        String escaped = s.replace("\\","\\\\").replace("\"","\\\"");
        return "\"" + escaped + "\"";
    }
}
