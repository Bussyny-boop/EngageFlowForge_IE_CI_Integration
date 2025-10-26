package com.example.exceljson;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Excel -> Engage JSON (v1.1.0) builder with parameterAttributes, recipients (with delays),
 * response options behavior, alarmAlertDefinitions, and unit linking from Unit Breakdown.
 */
public class ExcelParserV5 {

    // ---- Parsed row holders ---------------------------------------------------

    public static final class UnitRow {
        public final String facility;
        public final String unitNamesCsv; // "MedSurg, ICU"
        public final String nurseCfg;     // Configuration Group (Nurse Call)
        public final String clinCfg;      // Configuration Group (Patient Monitoring)
        public final String noCareGroup;  // "No Caregiver Alert Number or Group"

        public UnitRow(String facility, String unitNamesCsv, String nurseCfg, String clinCfg, String noCareGroup) {
            this.facility = nvl(facility);
            this.unitNamesCsv = nvl(unitNamesCsv);
            this.nurseCfg = nvl(nurseCfg);
            this.clinCfg = nvl(clinCfg);
            this.noCareGroup = nvl(noCareGroup);
        }
    }

    public static final class FlowRow {
        public String type;        // "NurseCalls" or "Clinicals"
        public String configGroup;
        public String alarmName;
        public String sendingName;
        public String priorityRaw; // original priority cell (for mapping)
        public String priority;    // mapped: Low|Low(Edge)->normal, Medium->high, High->urgent (null if empty)
        public String deviceA;
        public String ringtone;
        public String responseOptions;

        // recipients + delays (strings as read; we’ll parse on build)
        public String t1, r1;
        public String t2, r2;
        public String t3, r3;
        public String t4, r4;
        public String t5, r5;
    }

    // ---- Storage for parsed sheets -------------------------------------------

    public final List<UnitRow> units = new ArrayList<>();
    public final List<FlowRow> nurseCalls = new ArrayList<>();
    public final List<FlowRow> clinicals  = new ArrayList<>();

    // quick lookups
    private final Map<String, List<Map<String,String>>> nurseCfgToUnits = new LinkedHashMap<>();
    private final Map<String, List<Map<String,String>>> clinCfgToUnits  = new LinkedHashMap<>();
    private final Map<String, String> noCaregiverByFacility = new LinkedHashMap<>();

    // ---- Sheet names (normalized matching supported) -------------------------

    private static final String SHEET_UNIT     = "Unit Breakdown";
    private static final String SHEET_NURSE    = "Nurse Call";
    private static final String SHEET_CLINICAL = "Patient Monitoring";

    // ---- Public API -----------------------------------------------------------

    public ExcelParserV5() {}

    public void load(File excelFile) throws Exception {
        units.clear();
        nurseCalls.clear();
        clinicals.clear();
        nurseCfgToUnits.clear();
        clinCfgToUnits.clear();
        noCaregiverByFacility.clear();

        try (FileInputStream fis = new FileInputStream(excelFile);
             Workbook wb = new XSSFWorkbook(fis)) {
            parseUnitBreakdown(wb);
            parseNurseCall(wb);
            parseClinical(wb);
        }
    }

    public String getLoadSummary() {
        return String.format(
                "Loaded %d Unit rows; %d Nurse Call rows; %d Patient Monitoring rows. Linked Nurse cfg=%d, Clinical cfg=%d.",
                units.size(), nurseCalls.size(), clinicals.size(),
                nurseCfgToUnits.size(), clinCfgToUnits.size());
    }

    // ---- JSON builders --------------------------------------------------------

    /** Final combined Engage JSON document. */
    public Map<String, Object> buildCombinedJson() {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("version", "1.1.0");

        // alarmAlertDefinitions
        List<Map<String, Object>> defs = new ArrayList<>();
        defs.addAll(buildAlarmDefs(nurseCalls, "NurseCalls"));
        defs.addAll(buildAlarmDefs(clinicals, "Clinicals"));
        // de-dup by name|type
        defs = defs.stream().collect(Collectors.toMap(
                m -> (m.get("name")+"|"+m.get("type")),
                m -> m, (a,b)->a, LinkedHashMap::new
        )).values().stream().collect(Collectors.toList());
        root.put("alarmAlertDefinitions", defs);

        // deliveryFlows (grouping identical config columns isn’t required here; we emit one per config-group bundle)
        List<Map<String,Object>> flows = new ArrayList<>();
        flows.addAll(buildFlows(nurseCalls, true));
        flows.addAll(buildFlows(clinicals, false));
        root.put("deliveryFlows", flows);

        return root;
    }

    /** Write one combined JSON file (NurseCalls + Clinicals). */
    public void writeJson(File out) throws Exception {
        Map<String,Object> doc = buildCombinedJson();
        try (FileWriter fw = new FileWriter(out, false)) {
            fw.write(pretty(doc, 0));
            fw.flush();
        }
    }

    // ---- Builders (details) ---------------------------------------------------

    private List<Map<String,Object>> buildAlarmDefs(List<FlowRow> src, String type) {
        List<Map<String,Object>> list = new ArrayList<>();
        for (FlowRow r : src) {
            if (isBlank(r.alarmName)) continue;
            Map<String,Object> def = new LinkedHashMap<>();
            def.put("name", r.alarmName);
            def.put("type", type);
            List<Map<String,String>> values = new ArrayList<>();
            // per your "don’t populate Category", use empty string:
            values.add(Map.of("category", "", "value", nvl(r.sendingName, r.alarmName)));
            def.put("values", values);
            list.add(def);
        }
        return list;
    }

    private List<Map<String,Object>> buildFlows(List<FlowRow> src, boolean isNurse) {
        // Bundle by config-group signature: same cfg + same key knobs yields one flow with many alarms
        // Signature keys (excluding the alarm name itself)
        Map<String, List<FlowRow>> buckets = new LinkedHashMap<>();
        for (FlowRow r : src) {
            if (isBlank(r.configGroup) || isBlank(r.alarmName)) continue;
            String sig = String.join("|",
                    safe(r.type),
                    safe(r.configGroup),
                    safe(r.priority),
                    safe(r.ringtone),
                    safe(r.responseOptions),
                    safe(r.deviceA),
                    safe(r.t1), safe(r.r1),
                    safe(r.t2), safe(r.r2),
                    safe(r.t3), safe(r.r3),
                    safe(r.t4), safe(r.r4),
                    safe(r.t5), safe(r.r5)
            );
            buckets.computeIfAbsent(sig, k -> new ArrayList<>()).add(r);
        }

        List<Map<String,Object>> flows = new ArrayList<>();
        for (var e : buckets.entrySet()) {
            List<FlowRow> rows = e.getValue();
            FlowRow first = rows.get(0);

            // alarms in this bundle
            List<String> alarms = rows.stream()
                    .map(r -> r.alarmName)
                    .distinct()
                    .collect(Collectors.toList());

            // units: from Unit Breakdown link by config group
            List<Map<String,String>> unitsList = isNurse
                    ? nurseCfgToUnits.getOrDefault(first.configGroup, List.of())
                    : clinCfgToUnits.getOrDefault(first.configGroup, List.of());

            // Build base flow
            Map<String,Object> flow = new LinkedHashMap<>();
            flow.put("alarmsAlerts", alarms);
            flow.put("conditions", isNurse ? nurseConditions() : List.of());
            flow.put("destinations", buildDestinations(first, unitsList));
            flow.put("interfaces", List.of(Map.of(
                    "componentName", "OutgoingWCTP",
                    "referenceName", "OutgoingWCTP"
            )));
            flow.put("name", buildFlowName(first, unitsList, isNurse));
            flow.put("parameterAttributes", buildParams(first, unitsList, isNurse));
            if (!isBlank(first.priority)) flow.put("priority", first.priority);
            flow.put("status", "Active");
            if (!unitsList.isEmpty()) flow.put("units", unitsList);

            flows.add(flow);
        }
        return flows;
    }

    private List<Map<String,Object>> nurseConditions() {
        List<Map<String,Object>> filters = new ArrayList<>();
        filters.add(Map.of("attributePath","bed", "operator","not_null"));
        filters.add(Map.of("attributePath","to.type", "operator","not_equal", "value","TargetGroups"));
        return List.of(Map.of("name","NurseCallsCondition", "filters", filters));
    }

    private String buildFlowName(FlowRow r, List<Map<String,String>> unitsList, boolean isNurse) {
        String kind = isNurse ? "NURSECALL" : "CLINICAL";
        String pr   = isBlank(r.priority) ? "" : (r.priority.toUpperCase(Locale.ROOT) + " | ");
        String alarms = r.alarmName;

        // Units string (unique order preserved)
        LinkedHashSet<String> unitNames = new LinkedHashSet<>();
        for (var u : unitsList) unitNames.add(nvl(u.get("name")));
        String unitsStr = unitNames.isEmpty() ? "" : String.join(" | ", unitNames) + " | ";

        // Facility: if mixed, choose the first facility (your samples use single-facility flows)
        String facility = unitsList.isEmpty() ? "" : nvl(unitsList.get(0).get("facilityName"));

        return "SEND " + kind + " | " + pr + alarms + " | " + unitsStr + facility;
    }

    private List<Map<String,Object>> buildParams(FlowRow r, List<Map<String,String>> unitsList, boolean isNurse) {
        List<Map<String,Object>> params = new ArrayList<>();

        // Destination names by order from functional roles (if any)
        List<Map<String,Object>> dests = buildDestinations(r, unitsList);
        for (Map<String,Object> d : dests) {
            int order = (int) d.getOrDefault("order", 0);
            @SuppressWarnings("unchecked")
            List<Map<String,String>> roles = (List<Map<String,String>>) d.getOrDefault("functionalRoles", List.of());
            if (!roles.isEmpty()) {
                // destinationName := role name
                String roleName = roles.get(0).getOrDefault("name", "Role");
                params.add(pa(order, "destinationName", quote(roleName)));
            } else {
                // group
                params.add(pa(order, "destinationName", quote("Group")));
            }
        }

        // Alert sound from spreadsheet column H
        if (!isBlank(r.ringtone)) {
            params.add(pa("alertSound", quote(r.ringtone)));
        }

        if (isNurse) {
            // Response behavior
            String resp = norm(r.responseOptions);
            if (resp.contains("no response")) {
                params.add(pa("responseType", quote("None")));
                params.add(pa("responseAllowed", "false"));
            } else {
                params.add(pa("accept",          quote("Accepted")));
                params.add(pa("acceptAndCall",   quote(resp.contains("call back") ? "Call Back" : "Accepted")));
                params.add(pa("acceptBadgePhrases", "[\"Accept\"]"));
                // Show Accept/Decline path metadata
                params.add(pa("respondingLine", quote("responses.line.number")));
                params.add(pa("respondingUser", quote("responses.usr.login")));
                params.add(pa("responsePath",   quote("responses.action")));
                params.add(pa("responseType",   quote("Accept/Decline")));
                // Decline / escalate
                if (resp.contains("escalate")) {
                    params.add(pa(0, "decline",               quote("Decline Primary")));
                    params.add(pa(0, "declineBadgePhrases",   "[\"Escalate\"]"));
                }
            }
            // Breakthrough by priority
            if ("urgent".equalsIgnoreCase(nvl(r.priority))) {
                params.add(pa("breakThrough", quote("voceraAndDevice")));
            } else {
                params.add(pa("breakThrough", quote("none")));
            }
            // Common nurse params
            params.add(pa("popup", "true"));
            params.add(pa("enunciate", "true"));
            params.add(pa("message", quote("Patient: #{bed.patient.last_name}, #{bed.patient.first_name}\\nRoom/Bed: #{bed.room.name} - #{bed.bed_number}")));
            params.add(pa("patientMRN", quote("#{bed.patient.mrn}:#{bed.patient.visit_number}")));
            params.add(pa("placeUid", quote("#{bed.uid}")));
            params.add(pa("patientName", quote("#{bed.patient.first_name} #{bed.patient.middle_name} #{bed.patient.last_name}")));
            params.add(pa("eventIdentification", quote("NurseCalls:#{id}")));
            params.add(pa("shortMessage", quote("#{alert_type} #{bed.room.name}")));
            params.add(pa("subject", quote("#{alert_type} #{bed.room.name}")));
            params.add(pa("ttl", "10"));
            params.add(pa("retractRules", "[\"ttlHasElapsed\"]"));
            params.add(pa("vibrate", quote("short")));
        } else {
            // Clinicals template
            params.add(pa(0, "destinationName", quote("Nurse Alert")));
            // NoCaregivers (destination 1)
            params.add(pa(1, "destinationName", quote("NoCaregivers")));
            params.add(pa(1, "message", quote("#{alert_type}\\nIssue: A Clinical Alert has been received without any caregivers assigned to room.\\nRoom/Bed: #{bed.room.name} - #{bed.bed_number} \\nAlarm Time: #{alarm_time.as_time}")));
            params.add(pa(1, "shortMessage", quote("No Caregivers Assigned for #{alert_type} in #{bed.room.name} #{bed.bed_number}")));
            params.add(pa(1, "subject", quote("Alert Without Caregivers")));

            params.add(pa("alertSound", quote(!isBlank(r.ringtone) ? r.ringtone : "Vocera Tone 0 Long")));
            params.add(pa("responseAllowed", "false"));
            params.add(pa("breakThrough", quote("voceraAndDevice"))); // matches sample for urgent; you can gate by priority if you prefer
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
        }

        return params;
    }

    private List<Map<String,Object>> buildDestinations(FlowRow r, List<Map<String,String>> unitsList) {
        List<Map<String,Object>> dests = new ArrayList<>();

        // Up to 5
        addDest(dests, 0, r.t1, r.r1, unitsList);
        addDest(dests, 1, r.t2, r.r2, unitsList);
        addDest(dests, 2, r.t3, r.r3, unitsList);
        addDest(dests, 3, r.t4, r.r4, unitsList);
        addDest(dests, 4, r.t5, r.r5, unitsList);

        // For Clinicals we also add "NoCaregivers" group as NoDeliveries? (Sample shows both Normal and NoDeliveries cases)
        // Here we keep it simple: ONLY add NoDeliveries group row if we actually have a no-care group at the facility.
        if ("Clinicals".equalsIgnoreCase(nvl(r.type))) {
            String facility = unitsList.isEmpty() ? "" : nvl(unitsList.get(0).get("facilityName"));
            String nc = noCaregiverByFacility.getOrDefault(facility, "");
            String groupName = extractVGroupName(nc);
            if (!isBlank(groupName)) {
                Map<String,Object> d = new LinkedHashMap<>();
                d.put("delayTime", 0);
                d.put("destinationType", "NoDeliveries");
                d.put("functionalRoles", new ArrayList<>());
                d.put("groups", List.of(Map.of("facilityName", facility, "name", groupName)));
                d.put("order", Math.max(dests.size(), 1));
                d.put("presenceConfig", "device");
                d.put("recipientType", "group");
                d.put("users", new ArrayList<>());
                dests.add(d);
            }
        }

        return dests;
    }

    private void addDest(List<Map<String,Object>> dests, int order, String delayStr, String recipients, List<Map<String,String>> unitsList) {
        if (isBlank(recipients) && isBlank(delayStr)) return;

        int delay = parseDelay(delayStr);
        List<String> tokens = splitList(recipients);

        List<Map<String,String>> roles = new ArrayList<>();
        List<Map<String,String>> groups = new ArrayList<>();

        String facility = unitsList.isEmpty() ? "" : nvl(unitsList.get(0).get("facilityName"));

        for (String t : tokens) {
            String s = t.trim();
            if (s.toLowerCase(Locale.ROOT).startsWith("vassign")) {
                String role = extractVAssignRole(s);      // after "VAssign: [Room] "
                if (!isBlank(role)) {
                    roles.add(Map.of("facilityName", facility, "name", role));
                }
            } else if (s.toLowerCase(Locale.ROOT).startsWith("vgroup")) {
                String g = extractVGroupName(s);          // after "VGroup: "
                if (!isBlank(g)) {
                    groups.add(Map.of("facilityName", facility, "name", g));
                }
            } else {
                // literal fallback → treat as group name
                groups.add(Map.of("facilityName", facility, "name", s));
            }
        }

        Map<String,Object> d = new LinkedHashMap<>();
        d.put("delayTime", delay);
        d.put("destinationType", "Normal");
        d.put("functionalRoles", roles);
        d.put("groups", groups);
        d.put("order", order);
        if (!roles.isEmpty()) {
            d.put("presenceConfig", "user_and_device");
            d.put("recipientType", "functional_role");
        } else {
            d.put("presenceConfig", "device");
            d.put("recipientType", "group");
        }
        d.put("users", new ArrayList<>());

        dests.add(d);
    }

    // ---- Excel parsing --------------------------------------------------------

    private void parseUnitBreakdown(Workbook wb) {
        Sheet sh = findSheet(wb, SHEET_UNIT);
        if (sh == null) return;

        Map<String,Integer> h = headerMap(sh.getRow(0));
        int cFacility   = getCol(h, "Facility");
        int cUnitName   = getCol(h, "Common Unit Name");
        int cNurseCfg   = getCol(h, "Nurse Call", "Configuration Group", "Nurse call");
        int cClinCfg    = getCol(h, "Patient Monitoring", "Configuration Group", "Patient monitoring");
        int cNoCare     = getCol(h, "No Caregiver Alert Number or Group");

        for (int r = 1; r <= sh.getLastRowNum(); r++) {
            Row row = sh.getRow(r);
            if (row == null) continue;
            String facility = getCell(row, cFacility);
            String unitsCsv = getCell(row, cUnitName);
            String nurseCfg = getCell(row, cNurseCfg);
            String clinCfg  = getCell(row, cClinCfg);
            String noCare   = getCell(row, cNoCare);

            if (isBlank(facility) && isBlank(unitsCsv)) continue;

            UnitRow u = new UnitRow(facility, unitsCsv, nurseCfg, clinCfg, noCare);
            units.add(u);

            for (String name : splitUnits(unitsCsv)) {
                if (!isBlank(nurseCfg)) {
                    nurseCfgToUnits.computeIfAbsent(nurseCfg, k -> new ArrayList<>())
                            .add(Map.of("facilityName", facility, "name", name));
                }
                if (!isBlank(clinCfg)) {
                    clinCfgToUnits.computeIfAbsent(clinCfg, k -> new ArrayList<>())
                            .add(Map.of("facilityName", facility, "name", name));
                }
            }
            if (!isBlank(facility) && !isBlank(noCare)) {
                noCaregiverByFacility.put(facility, noCare);
            }
        }
    }

    private void parseNurseCall(Workbook wb) {
        Sheet sh = findSheet(wb, SHEET_NURSE);
        if (sh == null) return;
        Map<String,Integer> h = headerMap(sh.getRow(0));

        int cCfg   = getCol(h, "Configuration Group");
        int cAlarm = getCol(h, "Common Alert or Alarm Name", "Alarm Name");
        int cSend  = getCol(h, "Sending System Alert Name", "Sending System Alarm Name");
        int cPrio  = getCol(h, "Priority");
        int cDev   = getCol(h, "Device - A", "Device");
        int cRing  = getCol(h, "Ringtone Device - A", "Ringtone");
        int cResp  = getCol(h, "Response Options");

        int cT1 = getCol(h, "Time to 1st Recipient (after alarm triggers)", "Time to 1st Recipient", "Time to first recipient");
        int cR1 = getCol(h, "1st Recipient", "First Recipient");
        int cT2 = getCol(h, "Time to 2nd Recipient", "Time to second recipient");
        int cR2 = getCol(h, "2nd Recipient", "Second Recipient");
        int cT3 = getCol(h, "Time to 3rd Recipient");
        int cR3 = getCol(h, "3rd Recipient");
        int cT4 = getCol(h, "Time to 4th Recipient");
        int cR4 = getCol(h, "4th Recipient");
        int cT5 = getCol(h, "Time to 5th Recipient");
        int cR5 = getCol(h, "5th Recipient");

        for (int r = 1; r <= sh.getLastRowNum(); r++) {
            Row row = sh.getRow(r);
            if (row == null) continue;
            FlowRow f = new FlowRow();
            f.type = "NurseCalls";
            f.configGroup    = getCell(row, cCfg);
            f.alarmName      = getCell(row, cAlarm);
            f.sendingName    = getCell(row, cSend);
            f.priorityRaw    = getCell(row, cPrio);
            f.priority       = mapPriority(f.priorityRaw);   // may be null if blank
            f.deviceA        = getCell(row, cDev);
            f.ringtone       = getCell(row, cRing);
            f.responseOptions= getCell(row, cResp);
            f.t1 = getCell(row, cT1); f.r1 = getCell(row, cR1);
            f.t2 = getCell(row, cT2); f.r2 = getCell(row, cR2);
            f.t3 = getCell(row, cT3); f.r3 = getCell(row, cR3);
            f.t4 = getCell(row, cT4); f.r4 = getCell(row, cR4);
            f.t5 = getCell(row, cT5); f.r5 = getCell(row, cR5);

            if (!isBlank(f.alarmName)) nurseCalls.add(f);
        }
    }

    private void parseClinical(Workbook wb) {
        Sheet sh = findSheet(wb, SHEET_CLINICAL);
        if (sh == null) return;
        Map<String,Integer> h = headerMap(sh.getRow(0));

        int cCfg   = getCol(h, "Configuration Group");
        int cAlarm = getCol(h, "Alarm Name", "Common Alert or Alarm Name");
        int cSend  = getCol(h, "Sending System Alarm Name", "Sending System Alert Name");
        int cPrio  = getCol(h, "Priority");
        int cDev   = getCol(h, "Device - A", "Device");
        int cRing  = getCol(h, "Ringtone Device - A", "Ringtone");
        int cResp  = getCol(h, "Response Options");

        int cT1 = getCol(h, "Time to 1st Recipient (after alarm triggers)", "Time to 1st Recipient", "Time to first recipient");
        int cR1 = getCol(h, "1st Recipient", "First Recipient");
        int cT2 = getCol(h, "Time to 2nd Recipient", "Time to second recipient");
        int cR2 = getCol(h, "2nd Recipient", "Second Recipient");
        int cT3 = getCol(h, "Time to 3rd Recipient");
        int cR3 = getCol(h, "3rd Recipient");
        int cT4 = getCol(h, "Time to 4th Recipient");
        int cR4 = getCol(h, "4th Recipient");
        int cT5 = getCol(h, "Time to 5th Recipient");
        int cR5 = getCol(h, "5th Recipient");

        for (int r = 1; r <= sh.getLastRowNum(); r++) {
            Row row = sh.getRow(r);
            if (row == null) continue;
            FlowRow f = new FlowRow();
            f.type = "Clinicals";
            f.configGroup    = getCell(row, cCfg);
            f.alarmName      = getCell(row, cAlarm);
            f.sendingName    = getCell(row, cSend);
            f.priorityRaw    = getCell(row, cPrio);
            f.priority       = mapPriority(f.priorityRaw);
            f.deviceA        = getCell(row, cDev);
            f.ringtone       = getCell(row, cRing);
            f.responseOptions= getCell(row, cResp);
            f.t1 = getCell(row, cT1); f.r1 = getCell(row, cR1);
            f.t2 = getCell(row, cT2); f.r2 = getCell(row, cR2);
            f.t3 = getCell(row, cT3); f.r3 = getCell(row, cR3);
            f.t4 = getCell(row, cT4); f.r4 = getCell(row, cR4);
            f.t5 = getCell(row, cT5); f.r5 = getCell(row, cR5);

            if (!isBlank(f.alarmName)) clinicals.add(f);
        }
    }

    // ---- Small utils ----------------------------------------------------------

    /** Pretty JSON-ish writer (no external libs needed). */
    public static String pretty(Object obj, int indent) {
        String ind = "  ".repeat(indent);
        String ind2 = "  ".repeat(indent + 1);
        if (obj == null) return "null";
        if (obj instanceof Map<?,?> m) {
            StringBuilder sb = new StringBuilder("{\n");
            int i = 0;
            for (var e : m.entrySet()) {
                if (i++ > 0) sb.append(",\n");
                sb.append(ind2).append("\"").append(e.getKey()).append("\": ").append(pretty(e.getValue(), indent+1));
            }
            return sb.append("\n").append(ind).append("}").toString();
        }
        if (obj instanceof Collection<?> col) {
            String inner = col.stream().map(o -> pretty(o, indent+1))
                    .collect(Collectors.joining(",\n" + ind2));
            return "[\n" + ind2 + inner + "\n" + ind + "]";
        }
        if (obj instanceof String s) {
            return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
        }
        return String.valueOf(obj);
    }

    private static String quote(String s) { return "\"" + s + "\""; }

    private static Map<String,Object> pa(String name, String value) {
        Map<String,Object> m = new LinkedHashMap<>();
        m.put("name", name);
        m.put("value", value);
        return m;
    }
    private static Map<String,Object> pa(int order, String name, String value) {
        Map<String,Object> m = new LinkedHashMap<>();
        m.put("destinationOrder", order);
        m.put("name", name);
        m.put("value", value);
        return m;
    }

    private static boolean isBlank(String s) { return s == null || s.trim().isEmpty(); }
    private static String nvl(String s) { return s == null ? "" : s; }
    private static String nvl(String s, String dflt) { return isBlank(s) ? dflt : s; }
    private static String safe(String s) { return s == null ? "" : s; }

    private static String norm(String s) {
        return isBlank(s) ? "" : s.trim().toLowerCase(Locale.ROOT);
    }

    private static String mapPriority(String raw) {
        if (isBlank(raw)) return null; // do NOT default null priority
        String s = norm(raw);
        if (s.contains("high"))   return "urgent";
        if (s.contains("medium")) return "high";
        if (s.contains("low"))    return "normal";
        return raw; // unknown string -> pass through
    }

    private static List<String> splitUnits(String csv) {
        if (isBlank(csv)) return List.of();
        return Arrays.stream(csv.split("[,;]"))
                .map(String::trim)
                .filter(x -> !x.isEmpty())
                .collect(Collectors.toList());
    }

    private static List<String> splitList(String s) {
        if (isBlank(s)) return List.of();
        return Arrays.stream(s.split("[;,\n]"))
                .map(String::trim)
                .filter(x -> !x.isEmpty())
                .collect(Collectors.toList());
    }

    private static String extractVAssignRole(String s) {
        // e.g. "VAssign: [Room] CNA" -> "CNA"
        String txt = s.replaceFirst("(?i)^vassign\\s*:\\s*\\[room\\]\\s*", "");
        return txt.trim();
    }

    private static String extractVGroupName(String s) {
        // e.g. "VGroup: House Supervisor" -> "House Supervisor"
        return s.replaceFirst("(?i)^vgroup\\s*:\\s*", "").trim();
    }

    private static int parseDelay(String s) {
        if (isBlank(s)) return 0;
        String t = s.trim().toLowerCase(Locale.ROOT);
        // patterns like "60", "60s", "1m", "1 min", "1 minute", "2 minutes", "00:30", "1:00"
        if (t.matches("^\\d+$")) return Integer.parseInt(t);
        if (t.matches("^\\d+\\s*s(ec(ond)?s?)?$")) {
            return Integer.parseInt(t.replaceAll("\\D", ""));
        }
        if (t.matches("^\\d+\\s*m(in(ute)?s?)?$")) {
            return Integer.parseInt(t.replaceAll("\\D", "")) * 60;
        }
        if (t.matches("^\\d{1,2}:\\d{2}$")) {
            String[] parts = t.split(":");
            return Integer.parseInt(parts[0]) * 60 + Integer.parseInt(parts[1]);
        }
        // fallback: digits only from anywhere
        String digits = t.replaceAll("[^0-9]", "");
        if (!digits.isEmpty()) return Integer.parseInt(digits);
        return 0;
    }

    // ---- Sheet helpers --------------------------------------------------------

    private static String normSheetName(String name) {
        if (name == null) return "";
        return name.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", " ");
        // e.g., "Patient Monitoring" -> "patient monitoring"
    }

    private Sheet findSheet(Workbook wb, String... names) {
        if (wb == null) return null;
        if (names != null && names.length > 0) {
            // first try exact matches
            for (String n : names) {
                if (n == null) continue;
                Sheet s = wb.getSheet(n);
                if (s != null) return s;
            }
            // then normalized matching
            Set<String> wanted = Arrays.stream(names)
                    .filter(Objects::nonNull)
                    .map(ExcelParserV5::normSheetName)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            for (int i = 0; i < wb.getNumberOfSheets(); i++) {
                String nm = wb.getSheetName(i);
                if (wanted.contains(normSheetName(nm))) return wb.getSheetAt(i);
            }
        }
        return null;
    }

    private static Map<String,Integer> headerMap(Row header) {
        Map<String,Integer> map = new LinkedHashMap<>();
        if (header == null) return map;
        for (int i = 0; i < header.getLastCellNum(); i++) {
            String key = norm(getCell(header, i));
            if (!key.isEmpty()) map.put(key, i);
        }
        return map;
    }

    private static String getCell(Row r, int idx) {
        if (r == null || idx < 0) return "";
        Cell c = r.getCell(idx);
        if (c == null) return "";
        try {
            switch (c.getCellType()) {
                case STRING:  return c.getStringCellValue().trim();
                case NUMERIC:
                    if (DateUtil.isCellDateFormatted(c)) {
                        // not expected for our headers, but guard anyway
                        return c.getDateCellValue().toString();
                    }
                    // drop trailing ".0" for integers
                    double d = c.getNumericCellValue();
                    long l = (long) d;
                    return (d == l) ? String.valueOf(l) : String.valueOf(d);
                case BOOLEAN: return String.valueOf(c.getBooleanCellValue());
                default:      return nvl(c.toString()).trim();
            }
        } catch (Exception e) {
            return nvl(c.toString()).trim();
        }
    }

    private static int getCol(Map<String,Integer> map, String... names) {
        for (String n : names) {
            if (n == null) continue;
            int idx = map.getOrDefault(norm(n), -1);
            if (idx >= 0) return idx;
        }
        return -1;
    }
}
