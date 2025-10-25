package com.example.exceljson;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * ExcelParserV2
 * - Reads ONLY the columns you specified (by index and by well-known headers)
 * - Produces an in-memory model you can edit in the GUI
 * - Generates JSON very close to your provided smallconfig.json defaults
 * - Can export edited data to a new Excel (Save behavior B)
 *
 * Mapping (1-based column letters -> zero-based index used here):
 *  Nurse Call tab:
 *    Alarm Name: col E (index 4)
 *    Priority:   col F (index 5)
 *    Ringtone:   col H (index 7)
 *    Response Options: col AG (index 32)   (A=1, AG=33 -> zero-based 32)
 *    Recipients: headers "1st recipients", "2nd recipients", "3rd recipients", "4th recipients" (case-insensitive)
 *
 *  Patient Monitoring tab:
 *    Alarm Name: col E (4)
 *    Priority:   col F (5)
 *    Ringtone:   col H (7)
 *    Response Options: col AG (32)
 *    Recipients: same headers as above, plus "Fail safe recipients"
 *
 *  Unit Breakdown tab:
 *    Facility: col A (0)
 *    Common Unit Name: col C (2)
 *    (Optional) Configuration Group column: if present, we use it to scope units to flows.
 *    If no config-group linkage column is present, we attach ALL units to every flow.
 *
 * Priority mapping:
 *  "Low(Edge)" or "Low"     -> "normal"
 *  "Medium(Edge)" or "Medium" -> "high"
 *  "High(Edge)" or "High"   -> "urgent"
 *
 * Recipients:
 *  - Split on comma/semicolon/newline
 *  - "VGroup: NAME" => group recipient with NAME only
 *  - "VAssign: [Room] ROLE" => functional role recipient with ROLE only
 *  - presenceConfig: role -> "user_and_device"; group -> "device"
 *  - recipientType: role -> "functional_role"; group -> "group"
 *
 * Defaults:
 *  - Interface: OutgoingWCTP for all flows
 *  - Parameter templates differ for NurseCalls vs Clinicals (close to your JSON)
 */
public class ExcelParserV2 {

    // ----- Public DTOs you can bind to your GUI -----

    public static class UnitRow {
        public String facility;     // from Unit Breakdown col A
        public String unitName;     // from Unit Breakdown col C
        public String configGroup;  // optional link column (if present), else empty

        public UnitRow(String facility, String unitName, String configGroup) {
            this.facility = nvl(facility, "");
            this.unitName = nvl(unitName, "");
            this.configGroup = nvl(configGroup, "");
        }
    }

    public static class FlowRow {
        // Shared
        public String type;            // "NurseCalls" or "Clinicals"
        public String configGroup;     // if available (from sheet or left blank)
        public String alarmName;       // col E
        public String priority;        // normalized
        public String ringtone;        // col H
        public String responseOptions; // col AG

        // Recipients (up to 4) + Clinical fail-safe
        public String r1;
        public String r2;
        public String r3;
        public String r4;
        public String failSafe;        // Clinicals only; empty for NurseCalls

        public FlowRow(String type) { this.type = type; }
    }

    // ----- Workbook + Data -----

    private Workbook wb;
    private final List<UnitRow> units = new ArrayList<>();
    private final List<FlowRow> nurseCalls = new ArrayList<>();
    private final List<FlowRow> clinicals = new ArrayList<>();

    // Sheet names (configure to your actual names)
    private final String sheetUnits;
    private final String sheetNurseCalls;
    private final String sheetClinicals;

    public ExcelParserV2(String sheetUnits, String sheetNurseCalls, String sheetClinicals) {
        this.sheetUnits = sheetUnits;
        this.sheetNurseCalls = sheetNurseCalls;
        this.sheetClinicals = sheetClinicals;
    }

    // ----- LOAD -----

    public void load(File excel) throws Exception {
        try (FileInputStream fis = new FileInputStream(excel)) {
            this.wb = new XSSFWorkbook(fis);
        }
        readUnits();
        readNurseCalls();
        readClinicals();
    }

    // ----- GUI accessors -----

    public List<UnitRow> getUnits() { return units; }
    public List<FlowRow> getNurseCalls() { return nurseCalls; }
    public List<FlowRow> getClinicals() { return clinicals; }

    // ----- WRITE BACK (Save behavior B) -----

    /** Exports the edited data into a new, simple workbook (not preserving original formatting). */
    public void exportEditedExcel(File outFile) throws Exception {
        try (Workbook out = new XSSFWorkbook()) {
            // Units
            Sheet su = out.createSheet("Unit Breakdown (edited)");
            Row uh = su.createRow(0);
            uh.createCell(0).setCellValue("Facility");
            uh.createCell(1).setCellValue("Common Unit Name");
            uh.createCell(2).setCellValue("Configuration Group (optional)");

            for (int i = 0; i < units.size(); i++) {
                UnitRow u = units.get(i);
                Row r = su.createRow(i + 1);
                r.createCell(0).setCellValue(nvl(u.facility, ""));
                r.createCell(1).setCellValue(nvl(u.unitName, ""));
                r.createCell(2).setCellValue(nvl(u.configGroup, ""));
            }

            // NurseCalls
            Sheet sn = out.createSheet("Nurse Calls (edited)");
            Row nh = sn.createRow(0);
            nh.createCell(0).setCellValue("Configuration Group");
            nh.createCell(1).setCellValue("Alarm Name");
            nh.createCell(2).setCellValue("Priority");
            nh.createCell(3).setCellValue("Ringtone");
            nh.createCell(4).setCellValue("Response Options");
            nh.createCell(5).setCellValue("1st recipients");
            nh.createCell(6).setCellValue("2nd recipients");
            nh.createCell(7).setCellValue("3rd recipients");
            nh.createCell(8).setCellValue("4th recipients");

            for (int i = 0; i < nurseCalls.size(); i++) {
                FlowRow f = nurseCalls.get(i);
                Row r = sn.createRow(i + 1);
                r.createCell(0).setCellValue(nvl(f.configGroup, ""));
                r.createCell(1).setCellValue(nvl(f.alarmName, ""));
                r.createCell(2).setCellValue(nvl(f.priority, ""));
                r.createCell(3).setCellValue(nvl(f.ringtone, ""));
                r.createCell(4).setCellValue(nvl(f.responseOptions, ""));
                r.createCell(5).setCellValue(nvl(f.r1, ""));
                r.createCell(6).setCellValue(nvl(f.r2, ""));
                r.createCell(7).setCellValue(nvl(f.r3, ""));
                r.createCell(8).setCellValue(nvl(f.r4, ""));
            }

            // Clinicals
            Sheet sc = out.createSheet("Patient Monitoring (edited)");
            Row ch = sc.createRow(0);
            ch.createCell(0).setCellValue("Configuration Group");
            ch.createCell(1).setCellValue("Alarm Name");
            ch.createCell(2).setCellValue("Priority");
            ch.createCell(3).setCellValue("Ringtone");
            ch.createCell(4).setCellValue("Response Options");
            ch.createCell(5).setCellValue("1st recipients");
            ch.createCell(6).setCellValue("2nd recipients");
            ch.createCell(7).setCellValue("3rd recipients");
            ch.createCell(8).setCellValue("4th recipients");
            ch.createCell(9).setCellValue("Fail safe recipients");

            for (int i = 0; i < clinicals.size(); i++) {
                FlowRow f = clinicals.get(i);
                Row r = sc.createRow(i + 1);
                r.createCell(0).setCellValue(nvl(f.configGroup, ""));
                r.createCell(1).setCellValue(nvl(f.alarmName, ""));
                r.createCell(2).setCellValue(nvl(f.priority, ""));
                r.createCell(3).setCellValue(nvl(f.ringtone, ""));
                r.createCell(4).setCellValue(nvl(f.responseOptions, ""));
                r.createCell(5).setCellValue(nvl(f.r1, ""));
                r.createCell(6).setCellValue(nvl(f.r2, ""));
                r.createCell(7).setCellValue(nvl(f.r3, ""));
                r.createCell(8).setCellValue(nvl(f.r4, ""));
                r.createCell(9).setCellValue(nvl(f.failSafe, ""));
            }

            try (FileOutputStream fos = new FileOutputStream(outFile)) {
                out.write(fos);
            }
        }
    }

    // ----- JSON GENERATION -----

    /** Builds a JSON-like Map that mirrors your smallconfig.json structure closely. */
    public Map<String, Object> toJson() {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("version", "1.1.0");

        // A) alarmAlertDefinitions
        List<Map<String, Object>> defs = new ArrayList<>();
        for (FlowRow f : nurseCalls) defs.add(buildDefinition(f.alarmName, "NurseCalls"));
        for (FlowRow f : clinicals)  defs.add(buildDefinition(f.alarmName, "Clinicals"));

        // de-dup on (name,type)
        defs = defs.stream().collect(Collectors.toMap(
                d -> d.get("name") + "|" + d.get("type"),
                d -> d,
                (a,b)->a,
                LinkedHashMap::new
        )).values().stream().collect(Collectors.toList());
        root.put("alarmAlertDefinitions", defs);

        // B) deliveryFlows (group rows that share same config fields so they bundle alarmsAlerts)
        List<Map<String, Object>> flows = new ArrayList<>();
        flows.addAll(buildFlows("NURSECALL", nurseCalls));
        flows.addAll(buildFlows("CLINICAL", clinicals));
        root.put("deliveryFlows", flows);

        return root;
    }

    private Map<String,Object> buildDefinition(String name, String type){
        Map<String, Object> def = new LinkedHashMap<>();
        def.put("name", nvl(name, ""));
        def.put("type", type);
        // "values" with only "value" (no category), per your rule
        List<Map<String,String>> values = new ArrayList<>();
        if (!isBlank(name)) values.add(Map.of("value", name));
        def.put("values", values);
        return def;
    }

    private List<Map<String,Object>> buildFlows(String typeToken, List<FlowRow> rows){
        // Bundle by configGroup + priority + ringtone + response options + recipients vector
        Map<String, List<FlowRow>> grouped = new LinkedHashMap<>();
        for (FlowRow r : rows) {
            String key = String.join("|",
                    nvl(r.configGroup, ""),
                    nvl(r.priority, ""),
                    nvl(r.ringtone, ""),
                    nvl(r.responseOptions, ""),
                    nvl(r.r1,""), nvl(r.r2,""), nvl(r.r3,""), nvl(r.r4,""),
                    typeToken.equals("CLINICAL") ? nvl(r.failSafe,"") : ""
            ).toLowerCase();
            grouped.computeIfAbsent(key, k -> new ArrayList<>()).add(r);
        }

        List<Map<String,Object>> out = new ArrayList<>();
        for (var entry : grouped.entrySet()) {
            List<FlowRow> group = entry.getValue();
            FlowRow sample = group.get(0);

            Map<String,Object> flow = baseFlow();
            // alarmsAlerts
            List<String> alerts = group.stream()
                    .map(fr -> fr.alarmName)
                    .filter(x -> !isBlank(x))
                    .distinct()
                    .collect(Collectors.toList());
            flow.put("alarmsAlerts", alerts);

            // priority
            flow.put("priority", nvl(sample.priority, "normal"));
            // status
            flow.put("status", "Active");

            // interfaces (always OutgoingWCTP)
            List<Map<String,String>> ifaces = new ArrayList<>();
            ifaces.add(Map.of("componentName", "OutgoingWCTP", "referenceName", "OutgoingWCTP"));
            flow.put("interfaces", ifaces);

            // destinations
            List<Map<String,Object>> dests = new ArrayList<>();
            addDestinationsFromRecipients(dests, sample, sample.configGroup);
            // Clinical fail-safe destination if present
            if ("CLINICAL".equals(typeToken) && !isBlank(sample.failSafe)) {
                Map<String,Object> nd = buildGroupDest( // NoDeliveries as per your example
                        sample.failSafe, findFirstFacilityForGroup(sample.configGroup), 1, "NoDeliveries");
                dests.add(nd);
            }
            flow.put("destinations", dests);

            // parameters
            List<Map<String,Object>> params;
            if ("NURSECALL".equals(typeToken)) {
                params = buildNurseCallParams(sample);
            } else {
                params = buildClinicalParams(sample);
            }
            flow.put("parameterAttributes", params);

            // units (attach by config group if possible; else all units)
            List<Map<String,String>> unitRefs = findUnitsForConfig(sample.configGroup);
            if (unitRefs.isEmpty()) unitRefs = allUnits();
            flow.put("units", unitRefs);

            // name:
            // SEND <TYPE> | <PRIORITY> | <alarms> | <configGroup> | <unitsJoined> | <facility>
            String unitsJoined = unitRefs.stream().map(u -> u.get("name")).distinct().collect(Collectors.joining("/"));
            String facility = findFirstFacilityForGroup(sample.configGroup);
            String name = String.format("SEND %s | %s | %s | %s | %s | %s",
                    "NURSECALL".equals(typeToken) ? "NURSECALL" : "CLINICAL",
                    nvl(sample.priority, "normal").toUpperCase(),
                    String.join(" | ", alerts),
                    nvl(sample.configGroup, ""),
                    unitsJoined,
                    nvl(facility, ""));
            flow.put("name", name);

            out.add(flow);
        }

        return out;
    }

    // ----- Parameter templates (very close to your JSON defaults) -----

    private List<Map<String,Object>> buildNurseCallParams(FlowRow r){
        List<Map<String,Object>> p = new ArrayList<>();
        p.add(mapParam("destinationName", "\"Group\""));
        if (!isBlank(r.ringtone)) p.add(mapParam("alertSound", quote(r.ringtone)));
        p.add(mapParam("accept", "\"Accepted\""), r.responseOptions, "accept");
        p.add(mapParam("acceptAndCall", "\"Call Back\""), r.responseOptions, "call back");
        p.add(mapParam("acceptBadgePhrases", "[\"Accept\"]"), r.responseOptions, "accept");
        // escalate in decline set
        p.add(mapParam("decline", "\"Decline Primary\""), r.responseOptions, "escalate");
        p.add(mapParam("declineBadgePhrases", "[\"Escalate\"]"), r.responseOptions, "escalate");

        p.add(mapParam("popup", "true"));
        p.add(mapParam("alertSound", quoteOrEmpty(r.ringtone))); // keeps last
        p.add(mapParam("breakThrough", "\"voceraAndDevice\""), "urgent".equalsIgnoreCase(r.priority)); // only urgent
        p.add(mapParam("breakThrough", "\"none\""), !"urgent".equalsIgnoreCase(r.priority));          // else none
        p.add(mapParam("enunciate", "true"));
        p.add(mapParam("message", "\"Patient: #{bed.patient.last_name}, #{bed.patient.first_name}\\nRoom/Bed: #{bed.room.name} - #{bed.bed_number}\""));
        p.add(mapParam("patientMRN", "\"#{bed.patient.mrn}:#{bed.patient.visit_number}\""));
        p.add(mapParam("placeUid", "\"#{bed.uid}\""));
        p.add(mapParam("patientName", "\"#{bed.patient.first_name} #{bed.patient.middle_name} #{bed.patient.last_name}\""));
        p.add(mapParam("eventIdentification", "\"NurseCalls:#{id}\""));
        p.add(mapParam("respondingLine", "\"responses.line.number\""));
        p.add(mapParam("respondingUser", "\"responses.usr.login\""));
        p.add(mapParam("responsePath", "\"responses.action\""));
        // Response type None if "No response"
        boolean noResponse = containsIgnoreCase(r.responseOptions, "no response");
        p.add(mapParam("responseType", noResponse ? "\"None\"" : "\"Accept/Decline\""));

        p.add(mapParam("shortMessage", "\"#{alert_type} #{bed.room.name}\""));
        p.add(mapParam("subject", "\"#{alert_type} #{bed.room.name}\""));
        p.add(mapParam("ttl", "10"));
        p.add(mapParam("retractRules", "[\"ttlHasElapsed\"]"));
        p.add(mapParam("vibrate", "\"short\""));

        // Remove any empty dup param entries
        return p.stream().filter(m -> m.get("name")!=null && m.get("value")!=null).collect(Collectors.toList());
    }

    private List<Map<String,Object>> buildClinicalParams(FlowRow r){
        List<Map<String,Object>> p = new ArrayList<>();
        // Per your example
        p.add(mapParamOrder(0, "destinationName", "\"Nurse Alert\""));
        p.add(mapParamOrder(1, "destinationName", "\"NoCaregivers\""));
        p.add(mapParamOrder(1, "message", "\"#{alert_type}\\nIssue: A Clinical Alert has been received without any caregivers assigned to room.\\nRoom/Bed: #{bed.room.name} - #{bed.bed_number} \\nAlarm Time: #{alarm_time.as_time}\""));
        p.add(mapParamOrder(1, "shortMessage", "\"No Caregivers Assigned for #{alert_type} in #{bed.room.name} #{bed.bed_number}\""));
        p.add(mapParamOrder(1, "subject", "\"Alert Without Caregivers\""));

        if (!isBlank(r.ringtone)) p.add(mapParam("alertSound", quote(r.ringtone)));
        p.add(mapParam("responseAllowed", "false"));
        p.add(mapParam("breakThrough", "\"voceraAndDevice\""), "urgent".equalsIgnoreCase(r.priority));
        p.add(mapParam("breakThrough", "\"none\""), !"urgent".equalsIgnoreCase(r.priority));
        p.add(mapParam("enunciate", "true"));
        p.add(mapParam("message", "\"Clinical Alert ${destinationName}\\nRoom: #{bed.room.name} - #{bed.bed_number}\\nAlert Type: #{alert_type}\\nAlarm Time: #{alarm_time.as_time}\""));
        p.add(mapParam("patientMRN", "\"#{clinical_patient.mrn}:#{clinical_patient.visit_number}\""));
        p.add(mapParam("patientName", "\"#{clinical_patient.first_name} #{clinical_patient.middle_name} #{clinical_patient.last_name}\""));
        p.add(mapParam("placeUid", "\"#{bed.uid}\""));
        p.add(mapParam("popup", "true"));
        p.add(mapParam("eventIdentification", "\"#{id}\""));
        p.add(mapParam("responseType", "\"None\""));
        p.add(mapParam("shortMessage", "\"#{alert_type} #{bed.room.name}\""));
        p.add(mapParam("subject", "\"#{alert_type} #{bed.room.name}\""));
        p.add(mapParam("ttl", "10"));
        p.add(mapParam("retractRules", "[\"ttlHasElapsed\"]"));
        p.add(mapParam("vibrate", "\"short\""));

        return p;
    }

    // ----- Destinations from recipients -----

    private void addDestinationsFromRecipients(List<Map<String,Object>> dests, FlowRow r, String cfgGroup){
        int order = 0;
        for (String recipient : Arrays.asList(r.r1, r.r2, r.r3, r.r4)) {
            if (isBlank(recipient)) { order++; continue; }
            List<String> tokens = splitList(recipient);

            List<Map<String,String>> groups = new ArrayList<>();
            List<Map<String,String>> roles = new ArrayList<>();
            String fac = findFirstFacilityForGroup(cfgGroup);

            for (String t : tokens) {
                String s = t.trim();
                if (s.toLowerCase().startsWith("vgroup")) {
                    String name = s.replaceFirst("(?i)vgroup:\\s*", "").trim();
                    if (!isBlank(name)) groups.add(Map.of("facilityName", nvl(fac,""), "name", name));
                } else if (s.toLowerCase().startsWith("vassign")) {
                    String name = s.replaceFirst("(?i)vassign:\\s*\\[room\\]\\s*", "").trim();
                    if (!isBlank(name)) roles.add(Map.of("facilityName", nvl(fac,""), "name", name));
                } else {
                    // plain group text
                    if (!isBlank(s)) groups.add(Map.of("facilityName", nvl(fac,""), "name", s));
                }
            }

            Map<String,Object> d = new LinkedHashMap<>();
            d.put("order", order);
            d.put("delayTime", 0);
            d.put("destinationType", "Normal");
            d.put("users", new ArrayList<>());
            d.put("groups", groups);
            d.put("functionalRoles", roles);

            if (!roles.isEmpty()) {
                d.put("presenceConfig", "user_and_device");
                d.put("recipientType", "functional_role");
            } else {
                d.put("presenceConfig", "device");
                d.put("recipientType", "group");
            }
            dests.add(d);
            order++;
        }
    }

    private Map<String,Object> buildGroupDest(String groupName, String facility, int order, String destType){
        Map<String,Object> d = new LinkedHashMap<>();
        d.put("order", order);
        d.put("delayTime", 0);
        d.put("destinationType", destType);
        d.put("users", new ArrayList<>());
        d.put("functionalRoles", new ArrayList<>());
        d.put("groups", List.of(Map.of("facilityName", nvl(facility,""), "name", groupName)));
        d.put("presenceConfig", "device");
        d.put("recipientType", "group");
        return d;
    }

    // ----- Readers -----

    private void readUnits() {
        Sheet s = wb.getSheet(sheetUnits);
        if (s == null) return;

        // Try to detect optional config-group column by header text
        int headerIdx = findHeaderRowByContains(s, List.of("facility", "common unit name"), 10);
        if (headerIdx < 0) headerIdx = 0; // fallback first row as header if needed
        Row header = s.getRow(headerIdx);

        // indexes
        int colFacility = 0; // A
        int colUnitName = 2; // C
        Integer colCfg = findCol(header, "configuration group"); // optional

        for (int r = headerIdx + 1; r <= s.getLastRowNum(); r++) {
            Row row = s.getRow(r);
            if (row == null) continue;
            String facility = getString(row, colFacility);
            String unit = getString(row, colUnitName);
            if (isBlank(facility) && isBlank(unit)) continue;
            String cfg = (colCfg != null) ? getString(row, colCfg) : "";
            units.add(new UnitRow(facility, unit, cfg));
        }
    }

    private void readNurseCalls() {
        Sheet s = wb.getSheet(sheetNurseCalls);
        if (s == null) return;

        int headerIdx = findHeaderRowByContains(s, List.of("alarm", "priority"), 40);
        if (headerIdx < 0) headerIdx = 0;
        Row header = s.getRow(headerIdx);

        // Known fixed columns
        int cAlarm = 4; // E
        int cPriority = 5; // F
        int cRingtone = 7; // H
        int cResp = 32; // AG

        // Optional config group column if present
        Integer cCfg = findCol(header, "configuration group");

        // Recipients by header name
        Integer cR1 = findCol(header, "1st recipients");
        Integer cR2 = findCol(header, "2nd recipients");
        Integer cR3 = findCol(header, "3rd recipients");
        Integer cR4 = findCol(header, "4th recipients");

        for (int r = headerIdx + 1; r <= s.getLastRowNum(); r++) {
            Row row = s.getRow(r);
            if (row == null) continue;

            FlowRow f = new FlowRow("NurseCalls");
            f.configGroup = (cCfg != null) ? getString(row, cCfg) : "";
            f.alarmName = getString(row, cAlarm);
            f.priority = normalizePriority(getString(row, cPriority));
            f.ringtone = getString(row, cRingtone);
            f.responseOptions = getString(row, cResp);
            f.r1 = getString(row, cR1);
            f.r2 = getString(row, cR2);
            f.r3 = getString(row, cR3);
            f.r4 = getString(row, cR4);

            if (!isBlank(f.alarmName)) nurseCalls.add(f);
        }
    }

    private void readClinicals() {
        Sheet s = wb.getSheet(sheetClinicals);
        if (s == null) return;

        int headerIdx = findHeaderRowByContains(s, List.of("alarm", "priority"), 40);
        if (headerIdx < 0) headerIdx = 0;
        Row header = s.getRow(headerIdx);

        int cAlarm = 4; // E
        int cPriority = 5; // F
        int cRingtone = 7; // H
        int cResp = 32; // AG

        Integer cCfg = findCol(header, "configuration group");
        Integer cR1 = findCol(header, "1st recipients");
        Integer cR2 = findCol(header, "2nd recipients");
        Integer cR3 = findCol(header, "3rd recipients");
        Integer cR4 = findCol(header, "4th recipients");
        Integer cFail = findCol(header, "fail safe recipients");

        for (int r = headerIdx + 1; r <= s.getLastRowNum(); r++) {
            Row row = s.getRow(r);
            if (row == null) continue;

            FlowRow f = new FlowRow("Clinicals");
            f.configGroup = (cCfg != null) ? getString(row, cCfg) : "";
            f.alarmName = getString(row, cAlarm);
            f.priority = normalizePriority(getString(row, cPriority));
            f.ringtone = getString(row, cRingtone);
            f.responseOptions = getString(row, cResp);
            f.r1 = getString(row, cR1);
            f.r2 = getString(row, cR2);
            f.r3 = getString(row, cR3);
            f.r4 = getString(row, cR4);
            f.failSafe = getString(row, cFail);

            if (!isBlank(f.alarmName)) clinicals.add(f);
        }
    }

    // ----- Helpers -----

    private static Map<String,Object> baseFlow(){
        Map<String,Object> m = new LinkedHashMap<>();
        m.put("alarmsAlerts", new ArrayList<>());
        m.put("conditions", new ArrayList<>());
        m.put("destinations", new ArrayList<>());
        m.put("interfaces", new ArrayList<>());
        m.put("parameterAttributes", new ArrayList<>());
        m.put("priority", "normal");
        m.put("status", "Active");
        m.put("units", new ArrayList<>());
        return m;
    }

    private static Map<String,Object> mapParam(String name, String value){
        Map<String,Object> m = new LinkedHashMap<>();
        m.put("name", name);
        m.put("value", value);
        return m;
    }
    private static Map<String,Object> mapParam(String name, String value, boolean include){
        return include ? mapParam(name, value) : emptyParam();
    }
    private static Map<String,Object> mapParamOrder(int order, String name, String value){
        Map<String,Object> m = mapParam(name, value);
        m.put("destinationOrder", order);
        return m;
    }
    private static Map<String,Object> emptyParam(){ return new LinkedHashMap<>(); }

    private static boolean containsIgnoreCase(String haystack, String needle){
        if (haystack == null || needle == null) return false;
        return haystack.toLowerCase().contains(needle.toLowerCase());
    }

    private List<Map<String,String>> findUnitsForConfig(String cfg){
        if (isBlank(cfg)) return allUnits();
        List<Map<String,String>> out = new ArrayList<>();
        for (UnitRow u : units) {
            if (isBlank(u.configGroup) || u.configGroup.equalsIgnoreCase(cfg)) {
                out.add(Map.of("facilityName", nvl(u.facility,""), "name", nvl(u.unitName,"")));
            }
        }
        return out;
    }

    private List<Map<String,String>> allUnits(){
        return units.stream()
                .map(u -> Map.of("facilityName", nvl(u.facility,""), "name", nvl(u.unitName,"")))
                .collect(Collectors.toList());
    }

    private String findFirstFacilityForGroup(String cfg){
        List<Map<String,String>> list = findUnitsForConfig(cfg);
        if (!list.isEmpty()) return list.get(0).get("facilityName");
        return "";
        }

    // header utilities
    private static int findHeaderRowByContains(Sheet s, List<String> mustContain, int scanRows){
        int best = -1, bestHits = -1;
        for (int r = 0; r < Math.min(scanRows, s.getLastRowNum()+1); r++) {
            Row row = s.getRow(r);
            if (row == null) continue;
            int hits = 0;
            String rowText = rowToLower(row);
            for (String m : mustContain) if (rowText.contains(m.toLowerCase())) hits++;
            if (hits > bestHits) { bestHits = hits; best = r; }
        }
        return best;
    }
    private static String rowToLower(Row row){
        StringBuilder sb = new StringBuilder();
        for (int c = 0; c < row.getLastCellNum(); c++) {
            sb.append(getString(row, c).toLowerCase()).append(" ");
        }
        return sb.toString();
    }
    private static Integer findCol(Row header, String contains){
        if (header == null) return null;
        String needle = contains.toLowerCase();
        for (int c = 0; c < header.getLastCellNum(); c++) {
            String v = getString(header, c).toLowerCase();
            if (v.contains(needle)) return c;
        }
        return null;
    }

    // cell helpers
    private static String getString(Row row, int col){
        try{
            if (row == null || col < 0) return "";
            Cell cell = row.getCell(col);
            if (cell == null) return "";
            return switch (cell.getCellType()) {
                case STRING -> cell.getStringCellValue().trim();
                case NUMERIC -> (DateUtil.isCellDateFormatted(cell)
                        ? cell.getDateCellValue().toString()
                        : String.valueOf((long)cell.getNumericCellValue())).trim();
                case BOOLEAN -> String.valueOf(cell.getBooleanCellValue()).trim();
                default -> "";
            };
        }catch(Exception e){ return ""; }
    }

    private static List<String> splitList(String s){
        if (s == null) return List.of();
        return Arrays.stream(s.split("[;,\n]"))
                .map(String::trim)
                .filter(x -> !x.isEmpty())
                .collect(Collectors.toList());
    }

    private static String normalizePriority(String raw){
        String v = nvl(raw, "").toLowerCase();
        if (v.contains("low(edge)") || v.equals("low")) return "normal";
        if (v.contains("medium(edge)") || v.equals("medium")) return "high";
        if (v.contains("high(edge)") || v.equals("high")) return "urgent";
        return v.isEmpty() ? "normal" : v;
    }

    private static String quote(String s){ return "\"" + s.replace("\"","\\\"") + "\""; }
    private static String quoteOrEmpty(String s){ return isBlank(s) ? "\"\"" : quote(s); }
    private static boolean isBlank(String s){ return s == null || s.trim().isEmpty(); }
    private static String nvl(String s, String d){ return isBlank(s) ? d : s; }

    // ---------------- Convenience: JSON write ----------------
    /** Writes the JSON map returned by toJson() to a file. (App can call this after preview/edits.) */
    public void writeJson(File outFile) throws Exception {
        Map<String,Object> json = toJson();
        // simple pretty writer (no Jackson dependency)
        try (Writer w = new BufferedWriter(new FileWriter(outFile))) {
            w.write(pretty(json, 0));
        }
    }

    // very small JSON pretty printer (for this fixed structure)
    @SuppressWarnings("unchecked")
    private static String pretty(Object obj, int indent){
        String sp = "  ".repeat(indent);
        String sp1 = "  ".repeat(indent+1);
        if (obj instanceof Map){
            StringBuilder sb = new StringBuilder();
            sb.append("{\n");
            Iterator<Map.Entry<?,?>> it = ((Map<?,?>)obj).entrySet().iterator();
            while (it.hasNext()){
                var e = it.next();
                sb.append(sp1).append("\"").append(e.getKey()).append("\": ").append(pretty(e.getValue(), indent+1));
                if (it.hasNext()) sb.append(",");
                sb.append("\n");
            }
            sb.append(sp).append("}");
            return sb.toString();
        } else if (obj instanceof List){
            StringBuilder sb = new StringBuilder();
            sb.append("[");
            List<?> list = (List<?>) obj;
            if (!list.isEmpty()) sb.append("\n");
            for (int i=0;i<list.size();i++){
                sb.append(sp1).append(pretty(list.get(i), indent+1));
                if (i < list.size()-1) sb.append(",");
                sb.append("\n");
            }
            sb.append(sp).append("]");
            return sb.toString();
        } else if (obj instanceof String){
            return "\"" + obj.toString().replace("\"","\\\"") + "\"";
        } else if (obj == null){
            return "null";
        } else {
            return obj.toString();
        }
    }
}
