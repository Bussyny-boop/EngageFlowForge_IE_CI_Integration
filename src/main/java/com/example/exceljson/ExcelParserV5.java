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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Excel parser that understands the Engage workbook layout and exposes helper
 * methods for converting the Excel content into the Engage JSON shape.  The
 * previous revision of this file was truncated which left a number of methods
 * outside of the class definition and caused compilation failures.  The class
 * has been rebuilt so that all parsing and JSON helpers are again available in
 * a single coherent implementation.
 */
public class ExcelParserV5 {

    // ---------------------------------------------------------------------
    // Public row types exposed to the JavaFX UI
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

    private static final List<Map<String, Object>> NURSE_CONDITIONS;

    static {
        List<Map<String, Object>> conditions = new ArrayList<>();
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("type", "eventType");
        event.put("value", "Alarm");
        conditions.add(event);
        Map<String, Object> platform = new LinkedHashMap<>();
        platform.put("type", "platform");
        platform.put("value", "Vocera Platform");
        conditions.add(platform);
        NURSE_CONDITIONS = Collections.unmodifiableList(conditions);
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
        this.config = config == null ? Config.defaultConfig() : config;
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
        if (sheet == null) {
            return;
        }

        Row header = findHeaderRow(sheet);
        Map<String, Integer> columns = headerMap(header);

        int startRow = header == null ? sheet.getFirstRowNum() + 1 : header.getRowNum() + 1;
        int cFacility = getCol(columns, "Facility");
        int cUnitName = getCol(columns, "Common Unit Name");
        int cNurseGroup = getCol(columns, "Nurse Call", "Configuration Group", "Nurse call");
        int cClinGroup = getCol(columns, "Patient Monitoring", "Configuration Group", "Patient monitoring");
        int cNoCare = getCol(columns, "No Caregiver Alert Number or Group", "No Caregiver Group");
        int cComments = getCol(columns, "Comments");

        for (int r = startRow; r <= sheet.getLastRowNum(); r++) {
            Row row = sheet.getRow(r);
            if (row == null) {
                continue;
            }
            String facility = getCell(row, cFacility);
            String unitNames = getCell(row, cUnitName);
            String nurseGroup = getCell(row, cNurseGroup);
            String clinGroup = getCell(row, cClinGroup);
            String noCare = stripVGroup(getCell(row, cNoCare));
            String comments = getCell(row, cComments);

            if (isBlank(facility) && isBlank(unitNames)) {
                continue;
            }

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
        if (sheet == null) {
            return;
        }

        Row header = findHeaderRow(sheet);
        Map<String, Integer> columns = headerMap(header);
        int startRow = header == null ? sheet.getFirstRowNum() + 1 : header.getRowNum() + 1;

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
            if (row == null) {
                continue;
            }
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

            if (isBlank(flow.alarmName) && isBlank(flow.sendingName)) {
                continue;
            }

            if (nurseSide) {
                nurseCalls.add(flow);
            } else {
                clinicals.add(flow);
            }
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
            if (isBlank(row.configGroup) && isBlank(row.alarmName) && isBlank(row.sendingName)) {
                continue;
            }

            List<Map<String, String>> unitRefs = groupToUnits.getOrDefault(row.configGroup, List.of());
            Map<String, Object> flow = new LinkedHashMap<>();
            String mappedPriority = mapPrioritySafe(row.priorityRaw);
            flow.put("name", buildFlowName(nurseSide, mappedPriority, row, unitRefs));
            if (!isBlank(mappedPriority)) {
                flow.put("priority", mappedPriority);
            }
            flow.put("status", "Active");
            flow.put("alarmsAlerts", List.of(nvl(row.alarmName, row.sendingName)));
            flow.put("interfaces", List.of(Map.of(
                    "componentName", "OutgoingWCTP",
                    "referenceName", "OutgoingWCTP")));
            flow.put("parameterAttributes", buildParamAttributes(row, nurseSide, mappedPriority));
            flow.put("destinations", buildDestinations(row, unitRefs, nurseSide));
            if (!unitRefs.isEmpty()) {
                flow.put("units", unitRefs);
            }
            if (nurseSide) {
                flow.put("conditions", nurseConditions());
            } else {
                flow.put("conditions", List.of());
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
            if (isBlank(name)) {
                continue;
            }
            String key = name + "|" + (nurseSide ? "N" : "C");
            if (defs.containsKey(key)) {
                continue;
            }
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

        List<String> pieces = new ArrayList<>();
        pieces.add(nurseSide ? "SEND NURSECALL" : "SEND CLINICAL");
        if (!isBlank(mappedPriority)) {
            pieces.add(mappedPriority.toUpperCase(Locale.ROOT));
        }
        if (!isBlank(alarm)) {
            pieces.add(alarm);
        }
        if (!isBlank(group)) {
            pieces.add(group);
        }
        if (!unitNames.isEmpty()) {
            pieces.add(String.join(" / ", unitNames));
        } else if (!isBlank(facility)) {
            pieces.add(facility);
        }
        return String.join(" | ", pieces);
    }

    private List<Map<String, Object>> nurseConditions() {
        // return a defensive copy to avoid accidental mutation from callers
        return NURSE_CONDITIONS.stream()
                .map(original -> new LinkedHashMap<>(original))
                .collect(Collectors.toList());
    }

    private List<Map<String, Object>> buildDestinations(FlowRow row,
                                                        List<Map<String, String>> unitRefs,
                                                        boolean nurseSide) {
        List<Map<String, Object>> destinations = new ArrayList<>();
        String facility = unitRefs.isEmpty() ? "" : unitRefs.get(0).getOrDefault("facilityName", "");

        addOneDestination(destinations, facility, 0, row.t1, row.r1);
        addOneDestination(destinations, facility, 1, row.t2, row.r2);
        addOneDestination(destinations, facility, 2, row.t3, row.r3);
        addOneDestination(destinations, facility, 3, row.t4, row.r4);
        addOneDestination(destinations, facility, 4, row.t5, row.r5);

        if (!nurseSide && !isBlank(facility)) {
            String noCare = noCaregiverByFacility.getOrDefault(facility, "");
            if (!isBlank(noCare)) {
                Map<String, Object> dest = new LinkedHashMap<>();
                dest.put("order", destinations.size());
                dest.put("delayTime", 0);
                dest.put("destinationType", "NoDeliveries");
                dest.put("users", List.of());
                dest.put("functionalRoles", List.of());
                dest.put("groups", List.of(Map.of(
                        "facilityName", facility,
                        "name", noCare)));
                dest.put("presenceConfig", "device");
                dest.put("recipientType", "group");
                destinations.add(dest);
            }
        }

        return destinations;
    }

    private void addOneDestination(List<Map<String, Object>> destinations,
                                   String defaultFacility,
                                   int order,
                                   String delayText,
                                   String recipientText) {
        if (isBlank(recipientText) && isBlank(delayText)) {
            return;
        }

        List<String> recipients = Arrays.stream(recipientText == null ? new String[0] : recipientText.split("[,;\\n]"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());

        if (recipients.isEmpty()) {
            recipients = List.of(recipientText == null ? "" : recipientText.trim());
        }

        for (String recipient : recipients) {
            ParsedRecipient parsed = parseRecipient(recipient, defaultFacility);
            Map<String, Object> dest = new LinkedHashMap<>();
            dest.put("order", order);
            dest.put("delayTime", parseDelay(delayText));
            dest.put("destinationType", "Normal");
            dest.put("users", List.of());
            if (parsed.isFunctionalRole) {
                dest.put("functionalRoles", List.of(Map.of(
                        "facilityName", parsed.facility,
                        "name", parsed.value)));
                dest.put("groups", List.of());
                dest.put("presenceConfig", "user_and_device");
                dest.put("recipientType", "functional_role");
            } else {
                dest.put("functionalRoles", List.of());
                dest.put("groups", List.of(Map.of(
                        "facilityName", parsed.facility,
                        "name", parsed.value)));
                dest.put("presenceConfig", "device");
                dest.put("recipientType", "group");
            }
            destinations.add(dest);
        }
    }

    private List<Map<String, Object>> buildParamAttributes(FlowRow row,
                                                           boolean nurseSide,
                                                           String mappedPriority) {
        List<Map<String, Object>> params = new ArrayList<>();
        boolean urgent = "urgent".equalsIgnoreCase(mappedPriority);

        if (!isBlank(row.ringtone)) {
            params.add(pa("alertSound", row.ringtone));
        }

        if (nurseSide) {
            boolean hasAccept = containsWord(row.responseOptions, "accept");
            boolean hasEscalate = containsWord(row.responseOptions, "escalate");
            boolean hasCallBack = containsWord(row.responseOptions, "call back") || containsWord(row.responseOptions, "callback");
            boolean noResponse = containsWord(row.responseOptions, "no response");

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
            params.add(pa("message", "Patient: #{bed.patient.last_name}, #{bed.patient.first_name}\\n" +
                    "Room/Bed: #{bed.room.name} - #{bed.bed_number}"));
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
            params.add(pa("message", "Clinical Alert ${destinationName}\\n" +
                    "Room: #{bed.room.name} - #{bed.bed_number}\\n" +
                    "Alert Type: #{alert_type}\\n" +
                    "Alarm Time: #{alarm_time.as_time}"));
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
            params.add(paOrder(1, "destinationName", "NoCaregivers"));
            params.add(paOrder(1, "message", "#{alert_type}\\nIssue: A Clinical Alert has been received without any caregivers assigned to room."));
        }

        return params;
    }

    private void addDestNameParamsFromRecipients(List<Map<String, Object>> params, FlowRow row) {
        Map<Integer, String> firstRecipients = new LinkedHashMap<>();
        firstRecipients.put(0, firstRecipient(row.r1));
        firstRecipients.put(1, firstRecipient(row.r2));
        firstRecipients.put(2, firstRecipient(row.r3));
        firstRecipients.put(3, firstRecipient(row.r4));
        firstRecipients.put(4, firstRecipient(row.r5));

        firstRecipients.entrySet().stream()
                .filter(entry -> !isBlank(entry.getValue()))
                .forEach(entry -> params.add(paOrder(entry.getKey(), "destinationName", entry.getValue())));
    }

    private String firstRecipient(String value) {
        if (isBlank(value)) {
            return "";
        }
        return Arrays.stream(value.split("[,;\\n]"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .findFirst()
                .orElse(value.trim());
    }

    private Map<String, Object> pa(String name, String value) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("name", name);
        map.put("value", value == null ? "" : value);
        return map;
    }

    private Map<String, Object> paOrder(int order, String name, String value) {
        Map<String, Object> map = pa(name, value);
        map.put("destinationOrder", order);
        return map;
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

    /**
     * Writes a small summary JSON file noting that the main payload is now
     * emitted into two dedicated JSON files.  This preserves compatibility with
     * earlier automation that expected a single JSON output file.
     */
    public void writeJson(File summaryFile) throws Exception {
        ensureParent(summaryFile);
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("note", "This build now writes two separate JSON files: NurseCalls.json and Clinicals.json");
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
        if (indentLevel <= 0) {
            return pretty;
        }
        String indent = "  ".repeat(indentLevel);
        return Arrays.stream(pretty.split("\\R"))
                .map(line -> indent + line)
                .collect(Collectors.joining("\n"));
    }

    // ------------------------------------------------------------------
    // Utility helpers
    private Sheet findSheet(Workbook wb, String name) {
        if (wb == null || name == null) {
            return null;
        }
        for (int i = 0; i < wb.getNumberOfSheets(); i++) {
            Sheet sheet = wb.getSheetAt(i);
            if (sheet == null) {
                continue;
            }
            if (name.equalsIgnoreCase(sheet.getSheetName())) {
                return sheet;
            }
        }
        return null;
    }

    private Row findHeaderRow(Sheet sheet) {
        if (sheet == null) {
            return null;
        }
        int start = Math.max(0, 2);
        int end = Math.min(sheet.getLastRowNum(), start + 3);
        for (int r = start; r <= end; r++) {
            Row row = sheet.getRow(r);
            if (row == null) {
                continue;
            }
            int nonEmpty = 0;
            for (int c = 0; c < row.getLastCellNum(); c++) {
                if (!getCell(row, c).isBlank()) {
                    nonEmpty++;
                }
            }
            if (nonEmpty >= 3) {
                return row;
            }
        }
        for (int r = 0; r <= sheet.getLastRowNum(); r++) {
            Row row = sheet.getRow(r);
            if (row != null) {
                return row;
            }
        }
        return null;
    }

    private Map<String, Integer> headerMap(Row header) {
        Map<String, Integer> map = new LinkedHashMap<>();
        if (header == null) {
            return map;
        }
        for (int c = 0; c < header.getLastCellNum(); c++) {
            String value = getCell(header, c);
            if (value.isBlank()) {
                continue;
            }
            map.put(normalize(value), c);
        }
        return map;
    }

    private int getCol(Map<String, Integer> map, String... candidates) {
        if (map.isEmpty()) {
            return -1;
        }
        for (String candidate : candidates) {
            if (candidate == null) {
                continue;
            }
            String norm = normalize(candidate);
            if (map.containsKey(norm)) {
                return map.get(norm);
            }
        }
        // fallback: attempt partial match
        for (String candidate : candidates) {
            if (candidate == null) {
                continue;
            }
            String norm = normalize(candidate);
            for (Map.Entry<String, Integer> entry : map.entrySet()) {
                if (entry.getKey().contains(norm)) {
                    return entry.getValue();
                }
            }
        }
        return -1;
    }

    private String getCell(Row row, int column) {
        if (row == null || column < 0) {
            return "";
        }
        try {
            Cell cell = row.getCell(column);
            if (cell == null) {
                return "";
            }
            return switch (cell.getCellType()) {
                case STRING -> cell.getStringCellValue();
                case NUMERIC -> String.valueOf(cell.getNumericCellValue());
                case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
                case FORMULA -> cell.getCellFormula();
                default -> "";
            };
        } catch (Exception e) {
            return "";
        }
    }

    private List<String> splitUnits(String unitNames) {
        if (isBlank(unitNames)) {
            return List.of();
        }
        Set<String> names = new LinkedHashSet<>();
        for (String token : unitNames.split("[,;/\\n]")) {
            String trimmed = token.trim();
            if (!trimmed.isEmpty()) {
                names.add(trimmed);
            }
        }
        if (names.isEmpty()) {
            names.add(unitNames.trim());
        }
        return new ArrayList<>(names);
    }

    private String stripVGroup(String value) {
        if (value == null) {
            return "";
        }
        return value.replaceFirst("(?i)^v(group|assign)[: ]*", "").trim();
    }

    private ParsedRecipient parseRecipient(String raw, String defaultFacility) {
        String text = raw == null ? "" : raw.trim();
        if (text.isEmpty()) {
            return new ParsedRecipient(defaultFacility, "", false);
        }

        String facility = defaultFacility == null ? "" : defaultFacility;
        String value = text;

        // allow Facility::Group or Facility:Group syntax
        if (text.contains("::")) {
            String[] parts = text.split("::", 2);
            facility = parts[0].trim();
            value = parts[1].trim();
        } else if (text.contains(":")) {
            String[] parts = text.split(":", 2);
            if (parts.length == 2 && parts[0].trim().length() > 0 && parts[1].trim().length() > 0) {
                facility = parts[0].trim();
                value = parts[1].trim();
            }
        }

        Matcher matcher = functionalRolePattern.matcher(value);
        boolean functionalRole = matcher.find();
        return new ParsedRecipient(facility, stripVGroup(value), functionalRole);
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
        if (file == null) {
            throw new IOException("File reference is null");
        }
        File parent = file.getAbsoluteFile().getParentFile();
        if (parent != null && !parent.exists()) {
            if (!parent.mkdirs()) {
                throw new IOException("Unable to create directory: " + parent.getAbsolutePath());
            }
        }
    }

    private String normalize(String header) {
        return header == null ? "" : header.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", " ").trim();
    }

    private String nvl(String first, String second) {
        return isBlank(first) ? (second == null ? "" : second) : first;
    }

    private String mapPrioritySafe(String priority) {
        if (priority == null) {
            return "";
        }
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
        if (isBlank(text) || isBlank(probe)) {
            return false;
        }
        return text.toLowerCase(Locale.ROOT).contains(probe.toLowerCase(Locale.ROOT));
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private int parseDelay(String text) {
        if (isBlank(text)) {
            return 0;
        }
        try {
            String digits = text.replaceAll("[^0-9]", "");
            if (digits.isEmpty()) {
                return 0;
            }
            return Integer.parseInt(digits);
        } catch (NumberFormatException ex) {
            return 0;
        }
    }
}
