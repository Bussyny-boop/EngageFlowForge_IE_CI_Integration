package com.example.exceljson;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * ExcelParserV5 (with header-row improvements)
 * - Headers begin at Excel row 3 (index 2), automatically detected.
 * - All existing Engage JSON logic retained.
 */
public class ExcelParserV5 {

    // -------------------- Data rows kept in memory --------------------
    public static final class UnitRow {
        public String facility = "";
        public String unitNames = "";
        public String nurseGroup = "";
        public String clinGroup = "";
        public String noCareGroup = "";
        public String comments = "";
    }

    public static final class FlowRow {
        public String type = "";
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

    public final List<UnitRow> units = new ArrayList<>();
    public final List<FlowRow> nurseCalls = new ArrayList<>();
    public final List<FlowRow> clinicals = new ArrayList<>();

    private final Map<String, List<Map<String,String>>> nurseGroupToUnits = new LinkedHashMap<>();
    private final Map<String, List<Map<String,String>>> clinicalGroupToUnits = new LinkedHashMap<>();
    private final Map<String, String> noCaregiverByFacility = new LinkedHashMap<>();

    private static final String SHEET_UNIT = "Unit Breakdown";
    private static final String SHEET_NURSE = "Nurse Call";
    private static final String SHEET_CLINICAL = "Patient Monitoring";

    public ExcelParserV5() {}

    // -------------------- Load --------------------
    public void load(File excelFile) throws Exception {
        try (FileInputStream fis = new FileInputStream(excelFile);
             Workbook wb = new XSSFWorkbook(fis)) {
            parseUnitBreakdown(wb);
            parseNurseCall(wb);
            parseClinical(wb);
        }
    }

    public String getLoadSummary() {
        return String.format("""
                ✅ Excel Load Complete

                Loaded:
                  • %d Unit Breakdown rows
                  • %d Nurse Call rows
                  • %d Patient Monitoring rows

                Linked:
                  • %d Configuration Groups (Nurse)
                  • %d Configuration Groups (Clinical)
                """,
                units.size(), nurseCalls.size(), clinicals.size(),
                nurseGroupToUnits.size(), clinicalGroupToUnits.size());
    }

    // -------------------- HEADER detection --------------------
    private static Row findHeaderRow(Sheet sh) {
        if (sh == null) return null;
        int start = Math.max(0, 2); // Excel row 3 = index 2
        int end = Math.min(sh.getLastRowNum(), start + 3);
        for (int r = start; r <= end; r++) {
            Row row = sh.getRow(r);
            if (row == null) continue;
            int nonEmpty = 0;
            for (int c = 0; c < row.getLastCellNum(); c++) {
                String v = getCell(row, c);
                if (!v.isBlank()) nonEmpty++;
            }
            if (nonEmpty >= 3) return row; // header must have at least 3 non-empty cells
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
            if (afterHeader > firstRow) {
                return afterHeader;
            }
        }
        return firstRow;
    }

    // -------------------- Unit Breakdown --------------------
    private void parseUnitBreakdown(Workbook wb) {
        Sheet sh = findSheet(wb, SHEET_UNIT);
        if (sh == null) return;

        Row header = findHeaderRow(sh);
        Map<String,Integer> hm = headerMap(header);
        int startRow = firstDataRow(sh, header);
        int cFacility   = getCol(hm, "Facility");
        int cUnitName   = getCol(hm, "Common Unit Name");
        int cNurseGroup = getCol(hm, "Nurse Call", "Configuration Group", "Nurse call");
        int cClinGroup  = getCol(hm, "Patient Monitoring", "Configuration Group", "Patient monitoring");
        int cNoCare     = getCol(hm, "No Caregiver Alert Number or Group");
        int cComments   = getCol(hm, "Comments");

        for (int r = startRow; r <= sh.getLastRowNum(); r++) { // start reading after header
            Row row = sh.getRow(r);
            if (row == null) continue;
            String facility = getCell(row, cFacility);
            String unitNames = getCell(row, cUnitName);
            String nurseGroup = getCell(row, cNurseGroup);
            String clinGroup = getCell(row, cClinGroup);
            String noCare = stripVGroup(getCell(row, cNoCare));
            String comments = getCell(row, cComments);
            if (isBlank(facility) && isBlank(unitNames)) continue;

            UnitRow u = new UnitRow();
            u.facility = facility; u.unitNames = unitNames;
            u.nurseGroup = nurseGroup; u.clinGroup = clinGroup;
            u.noCareGroup = noCare; u.comments = comments;
            units.add(u);

            List<String> list = splitUnits(unitNames);
            if (!isBlank(nurseGroup))
                for (String name : list)
                    nurseGroupToUnits.computeIfAbsent(nurseGroup, k -> new ArrayList<>())
                            .add(Map.of("facilityName", facility, "name", name));
            if (!isBlank(clinGroup))
                for (String name : list)
                    clinicalGroupToUnits.computeIfAbsent(clinGroup, k -> new ArrayList<>())
                            .add(Map.of("facilityName", facility, "name", name));
            if (!isBlank(facility) && !isBlank(noCare))
                noCaregiverByFacility.put(facility, noCare);
        }
    }

    // -------------------- Nurse Call --------------------
    private void parseNurseCall(Workbook wb) {
        Sheet sh = findSheet(wb, SHEET_NURSE);
        if (sh == null) return;

        Row header = findHeaderRow(sh);
        Map<String,Integer> hm = headerMap(header);
        int startRow = firstDataRow(sh, header);
        int cCfg = getCol(hm, "Configuration Group");
        int cAlarm = getCol(hm, "Common Alert or Alarm Name", "Alarm Name");
        int cSend = getCol(hm, "Sending System Alert Name", "Sending System Alarm Name");
        int cPriority = getCol(hm, "Priority");
        int cDevice = getCol(hm, "Device - A", "Device");
        int cRing = getCol(hm, "Ringtone Device - A", "Ringtone");
        int cResp = getCol(hm, "Response Options");
        int cT1 = getCol(hm, "Time to 1st Recipient (after alarm triggers)", "Time to 1st Recipient");
        int cR1 = getCol(hm, "1st Recipient");
        int cT2 = getCol(hm, "Time to 2nd Recipient");
        int cR2 = getCol(hm, "2nd Recipient");
        int cT3 = getCol(hm, "Time to 3rd Recipient");
        int cR3 = getCol(hm, "3rd Recipient");
        int cT4 = getCol(hm, "Time to 4th Recipient");
        int cR4 = getCol(hm, "4th Recipient");
        int cT5 = getCol(hm, "Time to 5th Recipient");
        int cR5 = getCol(hm, "5th Recipient");

        for (int r = startRow; r <= sh.getLastRowNum(); r++) {
            Row row = sh.getRow(r);
            if (row == null) continue;
            FlowRow f = new FlowRow();
            f.type = "NurseCalls";
            f.configGroup = getCell(row, cCfg);
            f.alarmName = getCell(row, cAlarm);
            f.sendingName = getCell(row, cSend);
            f.priorityRaw = getCell(row, cPriority);
            f.deviceA = getCell(row, cDevice);
            f.ringtone = getCell(row, cRing);
            f.responseOptions = getCell(row, cResp);
            f.t1 = getCell(row, cT1); f.r1 = getCell(row, cR1);
            f.t2 = getCell(row, cT2); f.r2 = getCell(row, cR2);
            f.t3 = getCell(row, cT3); f.r3 = getCell(row, cR3);
            f.t4 = getCell(row, cT4); f.r4 = getCell(row, cR4);
            f.t5 = getCell(row, cT5); f.r5 = getCell(row, cR5);
            if (isBlank(f.alarmName) && isBlank(f.sendingName)) continue;
            nurseCalls.add(f);
        }
    }

    // -------------------- Clinicals --------------------
    private void parseClinical(Workbook wb) {
        Sheet sh = findSheet(wb, SHEET_CLINICAL);
        if (sh == null) return;

        Row header = findHeaderRow(sh);
        Map<String,Integer> hm = headerMap(header);
        int startRow = firstDataRow(sh, header);
        int cCfg = getCol(hm, "Configuration Group");
        int cAlarm = getCol(hm, "Alarm Name", "Common Alert or Alarm Name");
        int cSend = getCol(hm, "Sending System Alarm Name", "Sending System Alert Name");
        int cPriority = getCol(hm, "Priority");
        int cDevice = getCol(hm, "Device - A", "Device");
        int cRing = getCol(hm, "Ringtone Device - A", "Ringtone");
        int cResp = getCol(hm, "Response Options");
        int cT1 = getCol(hm, "Time to 1st Recipient (after alarm triggers)", "Time to 1st Recipient");
        int cR1 = getCol(hm, "1st Recipient");
        int cT2 = getCol(hm, "Time to 2nd Recipient");
        int cR2 = getCol(hm, "2nd Recipient");
        int cT3 = getCol(hm, "Time to 3rd Recipient");
        int cR3 = getCol(hm, "3rd Recipient");
        int cT4 = getCol(hm, "Time to 4th Recipient");
        int cR4 = getCol(hm, "4th Recipient");
        int cT5 = getCol(hm, "Time to 5th Recipient");
        int cR5 = getCol(hm, "5th Recipient");

        for (int r = startRow; r <= sh.getLastRowNum(); r++) {
            Row row = sh.getRow(r);
            if (row == null) continue;
            FlowRow f = new FlowRow();
            f.type = "Clinicals";
            f.configGroup = getCell(row, cCfg);
            f.alarmName = getCell(row, cAlarm);
            f.sendingName = getCell(row, cSend);
            f.priorityRaw = getCell(row, cPriority);
            f.deviceA = getCell(row, cDevice);
            f.ringtone = getCell(row, cRing);
            f.responseOptions = getCell(row, cResp);
            f.t1 = getCell(row, cT1); f.r1 = getCell(row, cR1);
            f.t2 = getCell(row, cT2); f.r2 = getCell(row, cR2);
            f.t3 = getCell(row, cT3); f.r3 = getCell(row, cR3);
            f.t4 = getCell(row, cT4); f.r4 = getCell(row, cR4);
            f.t5 = getCell(row, cT5); f.r5 = getCell(row, cR5);
            if (isBlank(f.alarmName) && isBlank(f.sendingName)) continue;
            clinicals.add(f);
        }
    }

    /** Writes combined NurseCalls + Clinicals JSON into one file (for backward compatibility with JobRunnerTest). */
public void writeJson(File file) throws Exception {
    ensureParent(file);
    Map<String, Object> nurse = buildNurseCallsJson();
    Map<String, Object> clinical = buildClinicalsJson();
    try (FileWriter out = new FileWriter(file, false)) {
        out.write("{\n");
        out.write("  \"NurseCalls\": " + pretty(nurse, 1) + ",\n");
        out.write("  \"Clinicals\": " + pretty(clinical, 1) + "\n");
        out.write("}\n");
    }
    if (!file.exists() || file.length() == 0) {
        throw new IOException("Combined JSON write failed: " + file.getAbsolutePath());
    }
}

    // -------------------- All your existing buildJson / utilities --------------------
    // (unchanged — keep everything else from your current file)

    //  ... keep the rest of the ExcelParserV5 exactly as in your provided code ...

    // -------------------- Build JSON (per side) --------------------
    private Map<String,Object> buildJson(boolean nurseSide) {
        // Choose rows & group-to-units map
        List<FlowRow> rows = nurseSide ? nurseCalls : clinicals;
        Map<String, List<Map<String,String>>> groupToUnits = nurseSide ? nurseGroupToUnits : clinicalGroupToUnits;

        // alarmAlertDefinitions
        List<Map<String,Object>> defs = buildAlarmDefs(rows, nurseSide);

        // Group rows by identical flow-shaping columns (config, priority, ringtone, recipients, responseOptions)
        Map<String, List<FlowRow>> bundles = bundleRows(rows, nurseSide);

        // Build flows
        List<Map<String,Object>> flows = new ArrayList<>();
        for (var entry : bundles.entrySet()) {
            List<FlowRow> same = entry.getValue();
            if (same.isEmpty()) continue;

            // alarmsAlerts = list of alarm names in this bundle
            List<String> alarms = same.stream()
                    .map(fr -> nvl(fr.alarmName, fr.sendingName))
                    .filter(s -> !isBlank(s))
                    .distinct()
                    .collect(Collectors.toList());

            // Units from group (first row’s configGroup)
            String cfg = same.get(0).configGroup;
            List<Map<String,String>> unitRefs = groupToUnits.getOrDefault(cfg, List.of());

            // Facility for naming (use first unit’s facility if available)
            String facility = unitRefs.isEmpty() ? "" : unitRefs.get(0).getOrDefault("facilityName", "");

            // Units list for naming (Common Unit Name values)
            List<String> unitNames = unitRefs.stream()
                    .map(u -> u.getOrDefault("name",""))
                    .filter(s -> !isBlank(s))
                    .distinct()
                    .collect(Collectors.toList());

            // Priority (use mapped value from first row if present; do NOT default if empty)
            String mappedPriority = mapPrioritySafe(same.get(0).priorityRaw);

            // Flow name
            String name = buildFlowName(nurseSide, mappedPriority, alarms, unitNames, facility);

            // Flow shell
            Map<String,Object> flow = new LinkedHashMap<>();
            flow.put("name", name);
            if (!isBlank(mappedPriority)) flow.put("priority", mappedPriority);
            flow.put("status", "Active");
            flow.put("alarmsAlerts", alarms);

            // Conditions
            if (nurseSide) {
                flow.put("conditions", nurseConditions());
            } else {
                flow.put("conditions", List.of());
            }

            // Interfaces (always WCTP)
            flow.put("interfaces", List.of(Map.of(
                    "componentName", "OutgoingWCTP",
                    "referenceName", "OutgoingWCTP"
            )));

            // Destinations (from FIRST row of bundle – because rows are identical on these columns by bundling key)
            flow.put("destinations", buildDestinations(same.get(0), unitRefs, nurseSide));

            // ParameterAttributes
            flow.put("parameterAttributes", buildParamAttributes(same.get(0), nurseSide));

            // Units
            if (!unitRefs.isEmpty()) flow.put("units", unitRefs);

            flows.add(flow);
        }

        Map<String,Object> root = new LinkedHashMap<>();
        root.put("version", "1.1.0");
        root.put("alarmAlertDefinitions", defs);
        root.put("deliveryFlows", flows);
        return root;
    }

    // -------------------- Alarm Definitions --------------------
    private List<Map<String,Object>> buildAlarmDefs(List<FlowRow> rows, boolean nurseSide) {
        Map<String, Map<String,Object>> byKey = new LinkedHashMap<>();
        for (FlowRow r : rows) {
            String name = nvl(r.alarmName, r.sendingName);
            if (isBlank(name)) continue;
            String key = (name + "|" + r.type);
            if (byKey.containsKey(key)) continue;

            Map<String,Object> def = new LinkedHashMap<>();
            def.put("name", name);
            def.put("type", nurseSide ? "NurseCalls" : "Clinicals");

            String value = isBlank(r.sendingName) ? name : r.sendingName; // prefer sending name if present
            // Per earlier instruction: don't populate "category" with any value → keep empty
            Map<String,String> val = new LinkedHashMap<>();
            val.put("category", "");
            val.put("value", value);
            def.put("values", List.of(val));

            byKey.put(key, def);
        }
        return new ArrayList<>(byKey.values());
    }

    // -------------------- Bundling key --------------------
    private Map<String, List<FlowRow>> bundleRows(List<FlowRow> rows, boolean nurseSide) {
        Map<String, List<FlowRow>> groups = new LinkedHashMap<>();
        for (FlowRow r : rows) {
            String key = String.join("||",
                    nz(r.configGroup),
                    nz(mapPrioritySafe(r.priorityRaw)),           // mapped priority (may be empty)
                    nz(r.ringtone),
                    nz(normalizeRecipientsForKey(r.r1, r.r2, r.r3, r.r4, r.r5)),
                    nz(normalizeRecipientsForKey(r.t1, r.t2, r.t3, r.t4, r.t5)),
                    nz(r.responseOptions),
                    nz(r.deviceA),
                    nurseSide ? "N" : "C"
            );
            groups.computeIfAbsent(key, k -> new ArrayList<>()).add(r);
        }
        return groups;
    }

    private static String normalizeRecipientsForKey(String... vals) {
        // Keep exact text; bundling depends on identical text
        return Arrays.stream(vals == null ? new String[0] : vals)
                .map(s -> s == null ? "" : s.trim())
                .collect(Collectors.joining("|"));
    }

    // -------------------- Destinations builder --------------------
    private List<Map<String,Object>> buildDestinations(FlowRow r, List<Map<String,String>> unitRefs, boolean nurseSide) {
        List<Map<String,Object>> dests = new ArrayList<>();

        // Facility to tag groups/roles
        String facility = unitRefs.isEmpty() ? "" : unitRefs.get(0).getOrDefault("facilityName", "");

        // Recipients 1..5
        addOneDestination(dests, facility, 0, r.t1, r.r1);
        addOneDestination(dests, facility, 1, r.t2, r.r2);
        addOneDestination(dests, facility, 2, r.t3, r.r3);
        addOneDestination(dests, facility, 3, r.t4, r.r4);
        addOneDestination(dests, facility, 4, r.t5, r.r5);

        // For Clinicals: add “NoCaregivers” NoDeliveries destination (facility-based)
        if (!nurseSide && !facility.isEmpty()) {
            String noCare = noCaregiverByFacility.getOrDefault(facility, "");
            if (!isBlank(noCare)) {
                Map<String,Object> d = new LinkedHashMap<>();
                d.put("order", dests.size());
                d.put("delayTime", 0);
                d.put("destinationType", "NoDeliveries");
                d.put("users", List.of());
                d.put("functionalRoles", List.of()); // group only
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

    private void addOneDestination(List<Map<String,Object>> dests,
                                   String facility,
                                   int order,
                                   String delayText,
                                   String recipientText) {
        if (isBlank(recipientText) && isBlank(delayText)) return;

        ParsedRecipient pr = parseRecipient(recipientText, facility);

        Map<String,Object> d = new LinkedHashMap<>();
        d.put("order", order);
        d.put("delayTime", parseDelay(delayText));
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
        } else if (pr.isGroup) {
            d.put("functionalRoles", List.of());
            d.put("groups", List.of(Map.of(
                    "facilityName", pr.facility,
                    "name", pr.value
            )));
            d.put("presenceConfig", "device");
            d.put("recipientType", "group");
        } else {
            // Treat as group name without VGroup: prefix
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

    private static final class ParsedRecipient {
        String facility = "";
        String value = "";
        boolean isFunctionalRole = false;
        boolean isGroup = false;
    }

    private ParsedRecipient parseRecipient(String raw, String facility) {
        ParsedRecipient pr = new ParsedRecipient();
        pr.facility = nvl(facility, "");

        String s = nvl(raw, "").trim();
        if (isBlank(s)) { pr.value = ""; return pr; }

        String lower = s.toLowerCase(Locale.ROOT);

        if (lower.startsWith("vassign")) {
            // VAssign: [Room] Role
            String name = s.replaceFirst("(?i)^\\s*vassign\\s*:\\s*\\[\\s*room\\s*]\\s*", "").trim();
            pr.value = name;
            pr.isFunctionalRole = true;
            return pr;
        }
        if (lower.startsWith("vgroup")) {
            // VGroup: Group Name
            String name = s.replaceFirst("(?i)^\\s*vgroup\\s*:\\s*", "").trim();
            pr.value = name;
            pr.isGroup = true;
            return pr;
        }
        // Fall back: treat as group label without prefix
        pr.value = s;
        pr.isGroup = true;
        return pr;
    }

    // -------------------- Parameter Attributes --------------------
    private List<Map<String,Object>> buildParamAttributes(FlowRow r, boolean nurseSide) {
        List<Map<String,Object>> params = new ArrayList<>();

        // Common helpers
        String mappedPriority = mapPrioritySafe(r.priorityRaw);
        boolean urgent = "urgent".equalsIgnoreCase(mappedPriority);

        // ALERT SOUND from Excel (ringtone)
        if (!isBlank(r.ringtone)) params.add(pa("alertSound", quote(r.ringtone)));

        if (nurseSide) {
            // Response options
            boolean hasAccept     = containsWord(r.responseOptions, "accept");
            boolean hasEscalate   = containsWord(r.responseOptions, "escalate");
            boolean hasCallBack   = containsWord(r.responseOptions, "call back") || containsWord(r.responseOptions, "callback");
            boolean noResponse    = containsWord(r.responseOptions, "no response");

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

            // breakThrough: urgent => voceraAndDevice else none
            params.add(pa("breakThrough", quote(urgent ? "voceraAndDevice" : "none")));

            // Standard nurse call params (sample JSON)
            params.add(pa("enunciate", "true"));
            params.add(pa("message", quote("Patient: #{bed.patient.last_name}, #{bed.patient.first_name}\\nRoom/Bed: #{bed.room.name} - #{bed.bed_number}")));
            params.add(pa("patientMRN", quote("#{bed.patient.mrn}:#{bed.patient.visit_number}")));
            params.add(pa("placeUid", quote("#{bed.uid}")));
            params.add(pa("patientName", quote("#{bed.patient.first_name} #{bed.patient.middle_name} #{bed.patient.last_name}")));
            params.add(pa("popup", "true"));
            params.add(pa("eventIdentification", quote("NurseCalls:#{id}")));

            // Response type
            params.add(pa("responseType", quote(noResponse ? "None" : "Accept/Decline")));

            params.add(pa("shortMessage", quote("#{alert_type} #{bed.room.name}")));
            params.add(pa("subject", quote("#{alert_type} #{bed.room.name}")));
            params.add(pa("ttl", "10"));
            params.add(pa("retractRules", "[\"ttlHasElapsed\"]"));
            params.add(pa("vibrate", quote("short")));

            // Destination names per order from recipients
            addDestNameParamsFromRecipients(params, r);

        } else {
            // Clinicals params (sample JSON)
            // Order 0: Nurse Alert, Order 1: NoCaregivers (if we created the no-deliveries dest)
            params.add(paOrder(0, "destinationName", quote("Nurse Alert")));
            params.add(pa("alertSound", quote(nvl(r.ringtone, "Vocera Tone 0 Long"))));
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

            // If we added the special NoCaregivers destination (order = last), add its params
            // It’s always order 1 when at least one normal destination exists
            params.add(paOrder(1, "destinationName", quote("NoCaregivers")));
            params.add(paOrder(1, "message", quote("#{alert_type}\\nIssue: A Clinical Alert has been received without any caregivers assigned to room.\\nRoom/Bed: #{bed.room.name} - #{bed.bed_number} \\nAlarm Time: #{alarm_time.as_time}")));
            params.add(paOrder(1, "shortMessage", quote("No Caregivers Assigned for #{alert_type} in #{bed.room.name} #{bed.bed_number}")));
            params.add(paOrder(1, "subject", quote("Alert Without Caregivers")));
        }
        return params;
    }

    private void addDestNameParamsFromRecipients(List<Map<String,Object>> params, FlowRow r) {
        // For functional roles: destinationName = role name; for groups: "Group"
        addDestName(params, 0, r.r1);
        addDestName(params, 1, r.r2);
        addDestName(params, 2, r.r3);
        addDestName(params, 3, r.r4);
        addDestName(params, 4, r.r5);
    }
    private void addDestName(List<Map<String,Object>> params, int order, String raw) {
        if (isBlank(raw)) return;
        String lower = raw.trim().toLowerCase(Locale.ROOT);
        String value;
        if (lower.startsWith("vassign")) {
            value = raw.replaceFirst("(?i)^\\s*vassign\\s*:\\s*\\[\\s*room\\s*]\\s*", "").trim();
        } else {
            value = "Group";
        }
        params.add(paOrder(order, "destinationName", quote(value)));
    }

    // NurseCall conditions (as per sample)
    private List<Map<String,Object>> nurseConditions() {
        Map<String,Object> f1 = new LinkedHashMap<>();
        f1.put("attributePath","bed");
        f1.put("operator","not_null");
        Map<String,Object> f2 = new LinkedHashMap<>();
        f2.put("attributePath","to.type");
        f2.put("operator","not_equal");
        f2.put("value","TargetGroups");
        Map<String,Object> cond = new LinkedHashMap<>();
        cond.put("name","NurseCallsCondition");
        cond.put("filters", List.of(f1,f2));
        return List.of(cond);
    }

    // -------------------- Flow Name --------------------
    private String buildFlowName(boolean nurseSide,
                                 String mappedPriority,
                                 List<String> alarms,
                                 List<String> unitNames,
                                 String facility) {
        List<String> parts = new ArrayList<>();
        parts.add("SEND " + (nurseSide ? "NURSECALL" : "CLINICAL"));
        if (!isBlank(mappedPriority)) parts.add(mappedPriority.toUpperCase(Locale.ROOT));
        if (!alarms.isEmpty()) parts.add(String.join(" | ", alarms.stream().map(String::toUpperCase).collect(Collectors.toList())));
        if (!unitNames.isEmpty()) parts.add(String.join(" | ", unitNames.stream().map(ExcelParserV5::caps).collect(Collectors.toList())));
        if (!isBlank(facility)) parts.add(facility);
        return String.join(" | ", parts);
    }

    // -------------------- Utilities --------------------
    private static void clearAll() {
        // nothing static to clear; instance fields cleared in load()
    }

    private static List<String> splitUnits(String s) {
        if (s == null) return List.of();
        return Arrays.stream(s.split("[,;]"))
                .map(String::trim)
                .filter(v -> !v.isEmpty())
                .collect(Collectors.toList());
    }

    private static boolean containsWord(String haystack, String needle) {
        if (isBlank(haystack) || isBlank(needle)) return false;
        String h = haystack.toLowerCase(Locale.ROOT);
        String n = needle.toLowerCase(Locale.ROOT);
        return h.contains(n);
    }

    private static String stripVGroup(String s) {
        if (isBlank(s)) return "";
        return s.replaceFirst("(?i)^\\s*vgroup\\s*:\\s*", "").trim();
    }

    private static String mapPrioritySafe(String raw) {
        if (isBlank(raw)) return "";
        String s = norm(raw);
        if (s.contains("high")) return "urgent";
        if (s.contains("medium")) return "high";
        if (s.contains("low")) return "normal";
        return ""; // unknown → do not default
    }

    private static int parseDelay(String s) {
        if (isBlank(s)) return 0;
        String digits = s.replaceAll("[^0-9]", "");
        if (digits.isEmpty()) return 0;
        try { return Integer.parseInt(digits); } catch (Exception ignore) { return 0; }
    }

    private static String caps(String s) {
        if (isBlank(s)) return s;
        return s.substring(0,1).toUpperCase(Locale.ROOT) + s.substring(1);
    }

    private static void ensureParent(File f) {
        File p = f.getAbsoluteFile().getParentFile();
        if (p != null && !p.exists()) p.mkdirs();
    }

    // Sheet helpers
    private static Sheet findSheet(Workbook wb, String... names) {
        if (wb == null || names == null) return null;
        // exact first
        for (String n : names) {
            if (n == null) continue;
            Sheet s = wb.getSheet(n);
            if (s != null) return s;
        }
        // normalized
        Set<String> targets = Arrays.stream(names)
                .filter(Objects::nonNull)
                .map(ExcelParserV5::normSheetName)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        for (int i = 0; i < wb.getNumberOfSheets(); i++) {
            String nm = wb.getSheetName(i);
            if (targets.contains(normSheetName(nm))) return wb.getSheetAt(i);
        }
        return null;
    }

    private static String normSheetName(String name) {
        return name == null ? "" : name.trim().toLowerCase(Locale.ROOT);
    }

    private static Map<String,Integer> headerMap(Row r) {
        Map<String,Integer> map = new LinkedHashMap<>();
        if (r == null) return map;
        for (int i=0; i<r.getLastCellNum(); i++) {
            String key = norm(getCell(r,i));
            if (!key.isEmpty()) map.put(key, i);
        }
        return map;
    }

    private static String getCell(Row r, int i) {
        if (i < 0 || r == null) return "";
        Cell c = r.getCell(i);
        if (c == null) return "";
        try {
            switch (c.getCellType()) {
                case STRING:  return c.getStringCellValue().trim();
                case NUMERIC:
                    if (DateUtil.isCellDateFormatted(c)) {
                        return c.getLocalDateTimeCellValue().toString();
                    }
                    String s = String.valueOf(c.getNumericCellValue());
                    // Trim trailing .0
                    return s.endsWith(".0") ? s.substring(0, s.length()-2) : s;
                case BOOLEAN: return String.valueOf(c.getBooleanCellValue());
                default: return c.toString().trim();
            }
        } catch (Exception e) {
            return c.toString().trim();
        }
    }

    private static int getCol(Map<String,Integer> map, String... names) {
        if (names == null) return -1;
        for (String n : names) {
            if (n == null) continue;
            Integer idx = map.get(norm(n));
            if (idx != null) return idx;
        }
        return -1;
    }

    private static String norm(String s) {
        return s == null ? "" : s.toLowerCase(Locale.ROOT)
                .replaceAll("[\\r\\n]+"," ")
                .replaceAll("[^a-z0-9]+"," ")
                .trim();
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
    private static String nvl(String a, String b) { return isBlank(a) ? b : a; }
    private static String nz(String s) { return s == null ? "" : s; }

    // parameter attribute helpers
    private static Map<String,Object> pa(String name, String value) {
        Map<String,Object> m = new LinkedHashMap<>();
        m.put("name", name);
        m.put("value", value);
        return m;
    }
    private static Map<String,Object> paOrder(int order, String name, String value) {
        Map<String,Object> m = pa(name, value);
        m.put("destinationOrder", order);
        return m;
    }

    private static String quote(String s) {
        if (s == null) return "\"\"";
        String escaped = s.replace("\\","\\\\").replace("\"","\\\"");
        return "\"" + escaped + "\"";
    }

    // Pretty JSON (simple)
    public static String pretty(Object obj) { return pretty(obj, 0); }
    @SuppressWarnings("unchecked")
    public static String pretty(Object obj, int indent) {
        String ind = "  ".repeat(indent);
        String ind2 = "  ".repeat(indent + 1);
        if (obj == null) return "null";
        if (obj instanceof Map<?,?> map) {
            StringBuilder sb = new StringBuilder("{\n");
            int i = 0;
            for (var e : map.entrySet()) {
                if (i++ > 0) sb.append(",\n");
                sb.append(ind2).append("\"").append(e.getKey()).append("\": ")
                        .append(pretty(e.getValue(), indent + 1));
            }
            return sb.append("\n").append(ind).append("}").toString();
        }
        if (obj instanceof Collection<?> col) {
            String joined = col.stream().map(o -> pretty(o, indent + 1))
                    .collect(Collectors.joining(",\n" + ind2, "[\n" + ind2, "\n" + ind + "]"));
            return joined;
        }
        if (obj instanceof String s) {
            return s; // strings are already quoted by quote(), otherwise raw literals like 10, true
        }
        return String.valueOf(obj);
    }

        
            // -------------------- Public JSON builders for AppController & JobRunner --------------------
        
        /** Builds NurseCalls JSON object (used by AppController preview). */
        public Map<String,Object> buildNurseCallsJson() {
            return buildJson(true);
        }
        
        /** Builds Clinicals JSON object (used by AppController preview). */
        public Map<String,Object> buildClinicalsJson() {
            return buildJson(false);
        }
        
        /** Writes NurseCalls JSON file. */
        public void writeNurseCallsJson(File nurseFile) throws Exception {
            Map<String,Object> nc = buildNurseCallsJson();
            ensureParent(nurseFile);
            try (FileWriter out = new FileWriter(nurseFile, false)) {
                out.write(pretty(nc));
            }
            if (!nurseFile.exists() || nurseFile.length() == 0) {
                throw new IOException("Failed writing NurseCalls JSON to: " + nurseFile.getAbsolutePath());
            }
        }
        
        /** Writes Clinicals JSON file. */
        public void writeClinicalsJson(File clinicalFile) throws Exception {
            Map<String,Object> cl = buildClinicalsJson();
            ensureParent(clinicalFile);
            try (FileWriter out = new FileWriter(clinicalFile, false)) {
                out.write(pretty(cl));
            }
            if (!clinicalFile.exists() || clinicalFile.length() == 0) {
                throw new IOException("Failed writing Clinicals JSON to: " + clinicalFile.getAbsolutePath());
            }
        }

}
