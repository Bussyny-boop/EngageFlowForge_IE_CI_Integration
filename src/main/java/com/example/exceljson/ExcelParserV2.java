package com.example.exceljson;

import com.example.exceljson.FlowRow;
import com.example.exceljson.UnitRow;
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
 *    Recipients: headers "1st Recipient", "2nd Recipient", "3rd Recipient", "4th Recipient" (case-insensitive)
 *
 *  Patient Monitoring tab:
 *    Alarm Name: col E (4)
 *    Priority:   col F (5)
 *    Ringtone:   col H (7)
 *    Response Options: col AG (32)
 *    Recipients: same headers as above, plus "Fail safe Recipient"
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
 *  - "VGroup: NAME" => group Recipient with NAME only
 *  - "VAssign: [Room] ROLE" => functional role Recipient with ROLE only
 *  - presenceConfig: role -> "user_and_device"; group -> "device"
 *  - RecipientType: role -> "functional_role"; group -> "group"
 *
 * Defaults:
 *  - Interface: OutgoingWCTP for all flows
 *  - Parameter templates differ for NurseCalls vs Clinicals (close to your JSON)
 */
public class ExcelParserV2 {

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

        units.clear();
        nurseCalls.clear();
        clinicals.clear();

        // Gracefully try loading each sheet
        try {
            readUnits();
        } catch (Exception e) {
            System.err.println("⚠ Warning: Failed to parse Unit Breakdown sheet: " + e.getMessage());
        }

        try {
            readNurseCalls();
        } catch (Exception e) {
            System.err.println("⚠ Warning: Failed to parse Nurse Call sheet: " + e.getMessage());
        }

        try {
            readClinicals();
        } catch (Exception e) {
            System.err.println("⚠ Warning: Failed to parse Patient Monitoring sheet: " + e.getMessage());
        }

        System.out.println("Loaded " + nurseCalls.size() + " NurseCalls, " + clinicals.size() + " Clinicals, " + units.size() + " Units");
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
                r.createCell(0).setCellValue(nvl(u.getFacility(), ""));
                r.createCell(1).setCellValue(nvl(u.getUnitName(), ""));
                r.createCell(2).setCellValue(nvl(u.getConfigGroup(), ""));
            }

            // NurseCalls
            Sheet sn = out.createSheet("Nurse Calls (edited)");
            Row nh = sn.createRow(0);
            nh.createCell(0).setCellValue("Configuration Group");
            nh.createCell(1).setCellValue("Alarm Name");
            nh.createCell(2).setCellValue("Priority");
            nh.createCell(3).setCellValue("Ringtone");
            nh.createCell(4).setCellValue("Response Options");
            nh.createCell(5).setCellValue("1st Recipient");
            nh.createCell(6).setCellValue("2nd Recipient");
            nh.createCell(7).setCellValue("3rd Recipient");
            nh.createCell(8).setCellValue("4th Recipient");

            for (int i = 0; i < nurseCalls.size(); i++) {
                FlowRow f = nurseCalls.get(i);
                Row r = sn.createRow(i + 1);
                r.createCell(0).setCellValue(nvl(f.getConfigGroup(), ""));
                r.createCell(1).setCellValue(nvl(f.getAlarmName(), ""));
                r.createCell(2).setCellValue(nvl(f.getPriority(), ""));
                r.createCell(3).setCellValue(nvl(f.getRingtone(), ""));
                r.createCell(4).setCellValue(nvl(f.getResponseOptions(), ""));
                r.createCell(5).setCellValue(nvl(f.getR1(), ""));
                r.createCell(6).setCellValue(nvl(f.getR2(), ""));
                r.createCell(7).setCellValue(nvl(f.getR3(), ""));
                r.createCell(8).setCellValue(nvl(f.getR4(), ""));
            }

            // Clinicals
            Sheet sc = out.createSheet("Patient Monitoring (edited)");
            Row ch = sc.createRow(0);
            ch.createCell(0).setCellValue("Configuration Group");
            ch.createCell(1).setCellValue("Alarm Name");
            ch.createCell(2).setCellValue("Priority");
            ch.createCell(3).setCellValue("Ringtone");
            ch.createCell(4).setCellValue("Response Options");
            ch.createCell(5).setCellValue("1st Recipient");
            ch.createCell(6).setCellValue("2nd Recipient");
            ch.createCell(7).setCellValue("3rd Recipient");
            ch.createCell(8).setCellValue("4th Recipient");
            ch.createCell(9).setCellValue("Fail safe Recipient");

            for (int i = 0; i < clinicals.size(); i++) {
                FlowRow f = clinicals.get(i);
                Row r = sc.createRow(i + 1);
                r.createCell(0).setCellValue(nvl(f.getConfigGroup(), ""));
                r.createCell(1).setCellValue(nvl(f.getAlarmName(), ""));
                r.createCell(2).setCellValue(nvl(f.getPriority(), ""));
                r.createCell(3).setCellValue(nvl(f.getRingtone(), ""));
                r.createCell(4).setCellValue(nvl(f.getResponseOptions(), ""));
                r.createCell(5).setCellValue(nvl(f.getR1(), ""));
                r.createCell(6).setCellValue(nvl(f.getR2(), ""));
                r.createCell(7).setCellValue(nvl(f.getR3(), ""));
                r.createCell(8).setCellValue(nvl(f.getR4(), ""));
                r.createCell(9).setCellValue(nvl(f.getFailSafe(), ""));
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
        for (FlowRow f : nurseCalls) defs.add(buildDefinition(f.getAlarmName(), "NurseCalls"));
        for (FlowRow f : clinicals)  defs.add(buildDefinition(f.getAlarmName(), "Clinicals"));

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
        // Bundle by configGroup + priority + ringtone + response options + Recipient vector
        Map<String, List<FlowRow>> grouped = new LinkedHashMap<>();
        for (FlowRow r : rows) {
            String key = String.join("|",
                    nvl(r.getConfigGroup(), ""),
                    nvl(r.getPriority(), ""),
                    nvl(r.getRingtone(), ""),
                    nvl(r.getResponseOptions(), ""),
                    nvl(r.getR1(),""), nvl(r.getR2(),""), nvl(r.getR3(),""), nvl(r.getR4(),""),
                    typeToken.equals("CLINICAL") ? nvl(r.getFailSafe(),"") : ""
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
                    .map(FlowRow::getAlarmName)
                    .filter(x -> !isBlank(x))
                    .distinct()
                    .collect(Collectors.toList());
            flow.put("alarmsAlerts", alerts);

            // priority
            flow.put("priority", nvl(sample.getPriority(), "normal"));
            // status
            flow.put("status", "Active");

            // interfaces (always OutgoingWCTP)
            List<Map<String,String>> ifaces = new ArrayList<>();
            ifaces.add(Map.of("componentName", "OutgoingWCTP", "referenceName", "OutgoingWCTP"));
            flow.put("interfaces", ifaces);

            // destinations
            List<Map<String,Object>> dests = new ArrayList<>();
            addDestinationsFromRecipients(dests, sample, sample.getConfigGroup());
            // Clinical fail-safe destination if present
            if ("CLINICAL".equals(typeToken) && !isBlank(sample.getFailSafe())) {
                Map<String,Object> nd = buildGroupDest( // NoDeliveries as per your example
                        sample.getFailSafe(), findFirstFacilityForGroup(sample.getConfigGroup()), 1, "NoDeliveries");
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
            List<Map<String,String>> unitRefs = findUnitsForConfig(sample.getConfigGroup());
            if (unitRefs.isEmpty()) unitRefs = allUnits();
            flow.put("units", unitRefs);

            // name:
            // SEND <TYPE> | <PRIORITY> | <alarms> | <configGroup> | <unitsJoined> | <facility>
            String unitsJoined = unitRefs.stream().map(u -> u.get("name")).distinct().collect(Collectors.joining("/"));
            String facility = findFirstFacilityForGroup(sample.getConfigGroup());
            String name = String.format("SEND %s | %s | %s | %s | %s | %s",
                    "NURSECALL".equals(typeToken) ? "NURSECALL" : "CLINICAL",
                    nvl(sample.getPriority(), "normal").toUpperCase(),
                    String.join(" | ", alerts),
                    nvl(sample.getConfigGroup(), ""),
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
            if (!isBlank(r.getRingtone())) p.add(mapParam("alertSound", quote(r.getRingtone())));

            // Conditionally add based on ResponseOptions
            String ro = nvl(r.getResponseOptions(), "").toLowerCase();
            if (ro.contains("accept")) {
                p.add(mapParam("accept", "\"Accepted\""));
                p.add(mapParam("acceptBadgePhrases", "[\"Accept\"]"));
            }
            if (ro.contains("call back")) {
                p.add(mapParam("acceptAndCall", "\"Call Back\""));
            }
            if (ro.contains("escalate")) {
                p.add(mapParam("decline", "\"Decline Primary\""));
                p.add(mapParam("declineBadgePhrases", "[\"Escalate\"]"));
            }

        p.add(mapParam("popup", "true"));
        p.add(mapParam("alertSound", quoteOrEmpty(r.getRingtone()))); // keeps last
        if ("urgent".equalsIgnoreCase(r.getPriority())) {
            p.add(mapParam("breakThrough", "\"voceraAndDevice\"")); // only urgent
        } else {
            p.add(mapParam("breakThrough", "\"none\""));          // else none
        }
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
        boolean noResponse = containsIgnoreCase(r.getResponseOptions(), "no response");
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

        if (!isBlank(r.getRingtone())) p.add(mapParam("alertSound", quote(r.getRingtone())));
        p.add(mapParam("responseAllowed", "false"));
        if ("urgent".equalsIgnoreCase(r.getPriority())) {
            p.add(mapParam("breakThrough", "\"voceraAndDevice\""));
        } else {
            p.add(mapParam("breakThrough", "\"none\""));
        }
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

    // ----- Destinations from Recipient -----

    private void addDestinationsFromRecipient(List<Map<String,Object>> dests, FlowRow r, String cfgGroup){
        int order = 0;
        for (String recipient : Arrays.asList(r.getR1(), r.getR2(), r.getR3(), r.getR4())) {
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
                d.put("RecipientType", "functional_role");
            } else {
                d.put("presenceConfig", "device");
                d.put("RecipientType", "group");
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
        d.put("RecipientType", "group");
        return d;
    }

    // ----- Readers -----

    private void readUnits() {
        Sheet s = wb.getSheet(sheetUnits);
        if (s == null) {
            System.err.println("⚠ Unit Breakdown sheet not found: " + sheetUnits);
            return;
        }

        int headerIdx = findHeaderRowByContains(s, List.of("facility", "unit"), 10);
        if (headerIdx < 0) headerIdx = 0;
        Row header = s.getRow(headerIdx);

        Integer foundFacility = findCol(header, "facility");
        int colFacility = foundFacility != null ? foundFacility : 0;
        Integer foundUnit = findCol(header, "common unit");
        int colUnitName = foundUnit != null ? foundUnit : 2;
        Integer colCfg = findCol(header, "configuration group");

        for (int r = headerIdx + 1; r <= s.getLastRowNum(); r++) {
            Row row = s.getRow(r);
            if (row == null) continue;

            String facility = getStringSafe(row, colFacility);
            String unit = getStringSafe(row, colUnitName);
            String cfg = (colCfg != null) ? getStringSafe(row, colCfg) : "";

            if (isBlank(facility) && isBlank(unit)) continue;
            units.add(new UnitRow(facility, unit, cfg));
        }
    }

    private void readNurseCalls() {
        Sheet s = wb.getSheet(sheetNurseCalls);
        if (s == null) {
            System.err.println("⚠ Nurse Call sheet not found: " + sheetNurseCalls);
            return;
        }

        int headerIdx = findHeaderRowByContains(s, List.of("alarm", "priority"), 40);
        if (headerIdx < 0) headerIdx = 0;
        Row header = s.getRow(headerIdx);

        int cAlarm = safeCol(header, "alarm", 4);
        int cPriority = safeCol(header, "priority", 5);
        int cRingtone = safeCol(header, "ringtone", 7);
        int cResp = safeCol(header, "response", 32);
        Integer cCfg = findColLike(header, "config");
        Integer cR1 = findColLike(header, "1st");
        Integer cR2 = findColLike(header, "2nd");
        Integer cR3 = findColLike(header, "3rd");
        Integer cR4 = findColLike(header, "4th");

        for (int r = headerIdx + 1; r <= s.getLastRowNum(); r++) {
            Row row = s.getRow(r);
            if (row == null) continue;

            FlowRow f = new FlowRow();
            f.setConfigGroup(getStringSafe(row, cCfg));
            f.setAlarmName(getStringSafe(row, cAlarm));
            f.setPriority(normalizePriority(getStringSafe(row, cPriority)));
            f.setRingtone(getStringSafe(row, cRingtone));
            f.setResponseOptions(getStringSafe(row, cResp));
            f.setR1(getStringSafe(row, cR1));
            f.setR2(getStringSafe(row, cR2));
            f.setR3(getStringSafe(row, cR3));
            f.setR4(getStringSafe(row, cR4));

            if (!isBlank(f.getAlarmName())) nurseCalls.add(f);
        }
    }

    private void readClinicals() {
        Sheet s = wb.getSheet(sheetClinicals);
        if (s == null) {
            System.err.println("⚠ Patient Monitoring sheet not found: " + sheetClinicals);
            return;
        }

        int headerIdx = findHeaderRowByContains(s, List.of("alarm", "priority"), 40);
        if (headerIdx < 0) headerIdx = 0;
        Row header = s.getRow(headerIdx);

        int cAlarm = safeCol(header, "alarm", 4);
        int cPriority = safeCol(header, "priority", 5);
        int cRingtone = safeCol(header, "ringtone", 7);
        int cResp = safeCol(header, "response", 32);
        Integer cCfg = findColLike(header, "config");
        Integer cR1 = findColLike(header, "1st");
        Integer cR2 = findColLike(header, "2nd");
        Integer cR3 = findColLike(header, "3rd");
        Integer cR4 = findColLike(header, "4th");
        Integer cFail = findColLike(header, "fail");

        for (int r = headerIdx + 1; r <= s.getLastRowNum(); r++) {
            Row row = s.getRow(r);
            if (row == null) continue;

            FlowRow f = new FlowRow();
            f.setConfigGroup(getStringSafe(row, cCfg));
            f.setAlarmName(getStringSafe(row, cAlarm));
            f.setPriority(normalizePriority(getStringSafe(row, cPriority)));
            f.setRingtone(getStringSafe(row, cRingtone));
            f.setResponseOptions(getStringSafe(row, cResp));
            f.setR1(getStringSafe(row, cR1));
            f.setR2(getStringSafe(row, cR2));
            f.setR3(getStringSafe(row, cR3));
            f.setR4(getStringSafe(row, cR4));
            f.setFailSafe(getStringSafe(row, cFail));

            if (!isBlank(f.getAlarmName())) clinicals.add(f);
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
            if (isBlank(u.getConfigGroup()) || u.getConfigGroup().equalsIgnoreCase(cfg)) {
                out.add(Map.of("facilityName", nvl(u.getFacility(),""), "name", nvl(u.getUnitName(),"")));
            }
        }
        return out;
    }

    private List<Map<String,String>> allUnits(){
        return units.stream()
                .map(u -> Map.of("facilityName", nvl(u.getFacility(),""), "name", nvl(u.getUnitName(),"")))
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

    private static Integer findColLike(Row header, String keyword) {
        if (header == null || keyword == null) return null;
        String key = keyword.toLowerCase();
        for (int c = 0; c < header.getLastCellNum(); c++) {
            String v = getString(header, c).toLowerCase().replaceAll("\\s+", "");
            if (v.contains(key)) return c;
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

    private static String getString(Row row, Integer col){
        return col == null ? "" : getString(row, col.intValue());
    }

    private static String getStringSafe(Row row, Integer col) {
        if (col == null || row == null) return "";
        return getString(row, col.intValue());
    }

    private static int safeCol(Row header, String keyword, int defaultIndex) {
        Integer idx = findColLike(header, keyword);
        return (idx != null) ? idx : defaultIndex;
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
    public static String pretty(Object obj, int indent){
        String sp = "  ".repeat(indent);
        String sp1 = "  ".repeat(indent+1);
        if (obj instanceof Map){
            StringBuilder sb = new StringBuilder();
            sb.append("{\n");
            Iterator<? extends Map.Entry<?,?>> it = ((Map<?,?>)obj).entrySet().iterator();
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
