package com.example.exceljson;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * ExcelParserV3
 * - Parses three tabs (Unit Breakdown, Nurse Call, Patient Monitoring)
 * - Normalizes headers
 * - Exposes editable lists for UI
 * - Builds two JSON documents (NurseCalls and Clinicals) and writes them to disk
 */
public class ExcelParserV3 {

    // -------- Public editable models exposed to UI --------
    public final List<UnitRow> units = new ArrayList<>();
    public final List<FlowRow> nurseCalls = new ArrayList<>();
    public final List<FlowRow> clinicals = new ArrayList<>();

    // Sheet names (can be overridden via constructor)
    private final String unitSheetName;
    private final String nurseSheetName;
    private final String clinicalSheetName;

    // Cached "No Caregiver Alert Number or Group" per facility (from Unit Breakdown)
    private final Map<String, String> noCaregiverByFacility = new LinkedHashMap<>();

    // Workbook file used for export locations
    private File sourceExcel;

    public ExcelParserV3() {
        this("Unit Breakdown", "Nurse call", "Patient Monitoring");
    }
    public ExcelParserV3(String unitSheetName, String nurseSheetName, String clinicalSheetName) {
        this.unitSheetName = unitSheetName;
        this.nurseSheetName = nurseSheetName;
        this.clinicalSheetName = clinicalSheetName;
    }

    // ------------------------------------------------------
    // Loading
    // ------------------------------------------------------
    public void load(File excelFile) throws Exception {
        this.sourceExcel = excelFile;
        units.clear();
        nurseCalls.clear();
        clinicals.clear();
        noCaregiverByFacility.clear();

        try (FileInputStream fis = new FileInputStream(excelFile);
             Workbook wb = new XSSFWorkbook(fis)) {

            parseUnitBreakdown(wb);
            parseNurseCall(wb);
            parseClinical(wb);
        }
    }

    // ------------------------------------------------------
    // Parsing helpers
    // ------------------------------------------------------
    private static String norm(String s) {
        if (s == null) return "";
        return s
                .toLowerCase()
                .replaceAll("[\\r\\n]+", " ")
                .replaceAll("[^a-z0-9]+", " ")
                .replaceAll(" +", " ")
                .trim();
    }

    private static String cellString(Row row, int col) {
        if (row == null) return "";
        Cell cell = row.getCell(col, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (cell == null) return "";
        try {
            return switch (cell.getCellType()) {
                case STRING -> cell.getStringCellValue();
                case NUMERIC -> {
                    if (DateUtil.isCellDateFormatted(cell)) {
                        // treat dates/times as plain text minutes if needed; here just numeric
                        yield String.valueOf(cell.getDateCellValue().getTime());
                    } else {
                        double d = cell.getNumericCellValue();
                        // drop trailing .0 if it's whole
                        if (Math.floor(d) == d) yield String.valueOf((long) d);
                        yield String.valueOf(d);
                    }
                }
                case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
                case FORMULA -> {
                    try {
                        yield cell.getStringCellValue();
                    } catch (Exception e) {
                        yield String.valueOf(cell.getNumericCellValue());
                    }
                }
                default -> "";
            };
        } catch (Exception e) {
            return "";
        }
    }

    private static Map<String, Integer> headerMap(Row header) {
        Map<String, Integer> map = new LinkedHashMap<>();
        if (header == null) return map;
        for (int c = 0; c < header.getLastCellNum(); c++) {
            String k = norm(cellString(header, c));
            if (!k.isEmpty()) map.put(k, c);
        }
        return map;
    }

    private static Integer col(Map<String,Integer> h, String... candidates) {
        for (String c : candidates) {
            Integer idx = h.get(norm(c));
            if (idx != null) return idx;
        }
        return null;
    }

    private static String get(Row r, Integer idx) {
        if (idx == null) return "";
        return cellString(r, idx).trim();
    }

    private static int parseDelay(String s) {
        if (s == null) return 0;
        String digits = s.replaceAll("[^0-9]", "");
        if (digits.isEmpty()) return 0;
        try { return Integer.parseInt(digits); } catch (Exception e) { return 0; }
    }

    // ------------------------------------------------------
    // UNIT BREAKDOWN
    // ------------------------------------------------------
    private void parseUnitBreakdown(Workbook wb) {
        Sheet sh = wb.getSheet(unitSheetName);
        if (sh == null) return;

        // Headers of interest
        // Facility, Common Unit Name, Nurse Call, Patient Monitoring, No Caregiver Alert Number or Group
        int headerRowIndex = findLikelyHeader(sh, List.of("Facility","Common Unit Name","Nurse Call","Patient Monitoring"));
        if (headerRowIndex < 0) return;

        Row header = sh.getRow(headerRowIndex);
        Map<String,Integer> hm = headerMap(header);

        Integer cFacility   = col(hm, "Facility");
        Integer cUnitName   = col(hm, "Common Unit Name");
        Integer cNCFlag     = col(hm, "Nurse Call");
        Integer cPMFlag     = col(hm, "Patient Monitoring");
        Integer cNoCare     = col(hm, "No Caregiver Alert Number or Group");

        for (int r = headerRowIndex + 1; r <= sh.getLastRowNum(); r++) {
            Row row = sh.getRow(r);
            if (row == null) continue;

            String facility = get(row, cFacility);
            String unitName = get(row, cUnitName);
            if (facility.isBlank() && unitName.isBlank()) continue;

            boolean inNC = !get(row, cNCFlag).isBlank();
            boolean inPM = !get(row, cPMFlag).isBlank();
            String noCare = get(row, cNoCare);

            UnitRow u = new UnitRow(facility, unitName, inNC, inPM, noCare);
            units.add(u);

            if (!facility.isBlank() && !noCare.isBlank()) {
                noCaregiverByFacility.put(facility, noCare);
            }
        }
    }

    private static int findLikelyHeader(Sheet sh, List<String> mustContainAny) {
        int bestRow = -1;
        int bestScore = -1;
        for (int r = 0; r <= Math.min(40, sh.getLastRowNum()); r++) {
            Row row = sh.getRow(r);
            if (row == null) continue;
            String joined = "";
            for (int c = 0; c < row.getLastCellNum(); c++) {
                String v = cellString(row, c);
                if (!v.isBlank()) joined += " " + v;
            }
            int score = 0;
            for (String m : mustContainAny) {
                if (norm(joined).contains(norm(m))) score++;
            }
            if (score > bestScore) { bestScore = score; bestRow = r; }
        }
        return bestRow;
    }

    // ------------------------------------------------------
    // NURSE CALL
    // ------------------------------------------------------
    private void parseNurseCall(Workbook wb) {
        Sheet sh = wb.getSheet(nurseSheetName);
        if (sh == null) return;

        int headerRowIndex = findLikelyHeader(sh, List.of("Configuration Group","Common Alert or Alarm Name","Priority"));
        if (headerRowIndex < 0) return;

        Map<String,Integer> hm = headerMap(sh.getRow(headerRowIndex));

        Integer cCfg = col(hm, "Configuration Group");
        Integer cAlarmCommon = col(hm, "Common Alert or Alarm Name", "Alarm Name");
        Integer cSending = col(hm, "Sending System Alert Name");
        Integer cPriority = col(hm, "Priority");
        Integer cDeviceA = col(hm, "Device - A");
        Integer cRingA = col(hm, "Ringtone Device - A");
        Integer cT1 = col(hm, "Time to 1st Recipient (after alarm triggers)");
        Integer cR1 = col(hm, "1st Recipient");
        Integer cT2 = col(hm, "Time to 2nd Recipient");
        Integer cR2 = col(hm, "2nd Recipient");
        Integer cT3 = col(hm, "Time to 3rd Recipient");
        Integer cR3 = col(hm, "3rd Recipient");
        Integer cT4 = col(hm, "Time to 4th Recipient");
        Integer cR4 = col(hm, "4th Recipient");
        Integer cResp = col(hm, "Response Options");
        Integer cEMDAN = col(hm, "EMDAN Compliant? (Y/N)");
        Integer cComments = col(hm, "Comments (Device - A)");

        for (int r = headerRowIndex + 1; r <= sh.getLastRowNum(); r++) {
            Row row = sh.getRow(r);
            if (row == null) continue;

            String cfg = get(row, cCfg);
            String alarm = get(row, cAlarmCommon);
            String sending = get(row, cSending);
            if (cfg.isBlank() && alarm.isBlank() && sending.isBlank()) continue;

            FlowRow f = new FlowRow();
            f.setType("NurseCalls");
            f.setConfigGroup(nonEmpty(cfg, "Nurse Call"));
            f.setAlarmName(alarm);
            f.setSendingName(sending);
            f.setPriority(mapPriority(get(row, cPriority)));
            f.setDeviceA(get(row, cDeviceA));
            f.setRingtone(get(row, cRingA));
            f.setResponseOptions(get(row, cResp));
            f.setEmdan(get(row, cEMDAN));
            f.setComments(get(row, cComments));
            // recipients + times
            f.setT1(get(row, cT1));
            f.setR1(get(row, cR1));
            f.setT2(get(row, cT2));
            f.setR2(get(row, cR2));
            f.setT3(get(row, cT3));
            f.setR3(get(row, cR3));
            f.setT4(get(row, cT4));
            f.setR4(get(row, cR4));

            nurseCalls.add(f);
        }
    }

    // ------------------------------------------------------
    // CLINICAL (Patient Monitoring)
    // ------------------------------------------------------
    private void parseClinical(Workbook wb) {
        Sheet sh = wb.getSheet(clinicalSheetName);
        if (sh == null) return;

        int headerRowIndex = findLikelyHeader(sh, List.of("Configuration Group","Alarm Name","Priority"));
        if (headerRowIndex < 0) return;

        Map<String,Integer> hm = headerMap(sh.getRow(headerRowIndex));

        Integer cCfg = col(hm, "Configuration Group");
        Integer cAlarm = col(hm, "Alarm Name");
        Integer cSending = col(hm, "Sending System Alarm Name");
        Integer cPriority = col(hm, "Priority");
        Integer cDeviceA = col(hm, "Device - A");
        Integer cRingA = col(hm, "Ringtone Device - A");
        Integer cT1 = col(hm, "Time to 1st Recipient (after alarm triggers)");
        Integer cR1 = col(hm, "1st Recipient");
        Integer cT2 = col(hm, "Time to 2nd Recipient");
        Integer cR2 = col(hm, "2nd Recipient");
        // Fail Safe column from PM is intentionally ignored per your requirement
        Integer cResp = col(hm, "Response Options");
        Integer cEMDAN = col(hm, "EMDAN Compliant? (Y)");
        Integer cComments = col(hm, "Comments (Device - A)");

        for (int r = headerRowIndex + 1; r <= sh.getLastRowNum(); r++) {
            Row row = sh.getRow(r);
            if (row == null) continue;

            String cfg = get(row, cCfg);
            String alarm = get(row, cAlarm);
            String sending = get(row, cSending);
            if (cfg.isBlank() && alarm.isBlank() && sending.isBlank()) continue;

            FlowRow f = new FlowRow();
            f.setType("Clinicals");
            f.setConfigGroup(nonEmpty(cfg, "Patient Monitoring"));
            f.setAlarmName(alarm);
            f.setSendingName(sending);
            f.setPriority(mapPriority(get(row, cPriority)));
            f.setDeviceA(get(row, cDeviceA));
            f.setRingtone(get(row, cRingA));
            f.setResponseOptions(get(row, cResp));
            f.setEmdan(get(row, cEMDAN));
            f.setComments(get(row, cComments));
            // recipients + times
            f.setT1(get(row, cT1));
            f.setR1(get(row, cR1));
            f.setT2(get(row, cT2));
            f.setR2(get(row, cR2));

            clinicals.add(f);
        }
    }

    private static String nonEmpty(String v, String fallback) {
        return (v == null || v.isBlank()) ? fallback : v;
    }

    private static String mapPriority(String p) {
        String s = norm(p);
        if (s.contains("high")) return "urgent";
        if (s.contains("medium")) return "high";
        if (s.contains("low")) return "normal";
        return "normal";
    }

    // ------------------------------------------------------
    // Export edited Excel (Behavior B: write a new workbook from in-memory tables)
    // ------------------------------------------------------
    public void exportEditedExcel(File out) throws Exception {
        try (Workbook wb = new XSSFWorkbook()) {
            // Unit Breakdown
            Sheet su = wb.createSheet(unitSheetName);
            writeRow(su, 0,
                    "Facility","Common Unit Name","Nurse Call","Patient Monitoring","No Caregiver Alert Number or Group");
            int r = 1;
            for (UnitRow u : units) {
                writeRow(su, r++,
                        u.getFacility(), u.getUnitName(),
                        u.isInNurseCall() ? "Yes" : "",
                        u.isInPatientMonitoring() ? "Yes" : "",
                        u.getNoCaregiverGroup());
            }

            // Nurse Call
            Sheet sn = wb.createSheet(nurseSheetName);
            writeRow(sn, 0,
                    "Configuration Group","Common Alert or Alarm Name","Sending System Alert Name",
                    "Priority","Device - A","Ringtone Device - A",
                    "Time to 1st Recipient (after alarm triggers)","1st Recipient",
                    "Time to 2nd Recipient","2nd Recipient",
                    "Time to 3rd Recipient","3rd Recipient",
                    "Time to 4th Recipient","4th Recipient",
                    "Response Options","EMDAN Compliant? (Y/N)","Comments (Device - A)");
            r = 1;
            for (FlowRow f : nurseCalls) {
                writeRow(sn, r++,
                        f.getConfigGroup(), f.getAlarmName(), f.getSendingName(),
                        reversePriority(f.getPriority()), f.getDeviceA(), f.getRingtone(),
                        f.getT1(), f.getR1(),
                        f.getT2(), f.getR2(),
                        f.getT3(), f.getR3(),
                        f.getT4(), f.getR4(),
                        f.getResponseOptions(), f.getEmdan(), f.getComments());
            }

            // Patient Monitoring
            Sheet sc = wb.createSheet(clinicalSheetName);
            writeRow(sc, 0,
                    "Configuration Group","Alarm Name","Sending System Alarm Name",
                    "Priority","Device - A","Ringtone Device - A",
                    "Time to 1st Recipient (after alarm triggers)","1st Recipient",
                    "Time to 2nd Recipient","2nd Recipient",
                    "Response Options","EMDAN Compliant? (Y)","Comments (Device - A)");
            r = 1;
            for (FlowRow f : clinicals) {
                writeRow(sc, r++,
                        f.getConfigGroup(), f.getAlarmName(), f.getSendingName(),
                        reversePriority(f.getPriority()), f.getDeviceA(), f.getRingtone(),
                        f.getT1(), f.getR1(),
                        f.getT2(), f.getR2(),
                        f.getResponseOptions(), f.getEmdan(), f.getComments());
            }

            try (FileOutputStream fos = new FileOutputStream(out)) {
                wb.write(fos);
            }
        }
    }

    private static String reversePriority(String p) {
        String s = norm(p);
        if (s.equals("urgent")) return "High";
        if (s.equals("high")) return "Medium";
        return "Low";
    }

    private static void writeRow(Sheet sh, int r, String... vals) {
        Row row = sh.createRow(r);
        for (int i = 0; i < vals.length; i++) {
            Cell c = row.createCell(i, CellType.STRING);
            c.setCellValue(vals[i] == null ? "" : vals[i]);
        }
    }

    // ------------------------------------------------------
    // JSON BUILD
    // ------------------------------------------------------

    /** Builds "NurseCalls" JSON document. */
    public Map<String, Object> buildNurseCallsJson() {
        Map<String,Object> root = new LinkedHashMap<>();
        root.put("version", "1.1.0");

        // alarmAlertDefinitions
        List<Map<String,Object>> defs = new ArrayList<>();
        for (FlowRow f : nurseCalls) {
            if (isBlank(f.getAlarmName())) continue;
            Map<String,Object> def = new LinkedHashMap<>();
            def.put("name", f.getAlarmName());
            def.put("type", "NurseCalls");
            List<Map<String,String>> values = new ArrayList<>();
            if (!isBlank(f.getSendingName())) {
                Map<String,String> v = new LinkedHashMap<>();
                v.put("value", f.getSendingName());
                values.add(v);
            }
            def.put("values", values);
            defs.add(def);
        }
        // de-dup by name+type
        defs = dedupByKey(defs, m -> (m.get("name")+"|"+m.get("type")));
        root.put("alarmAlertDefinitions", defs);

        // deliveryFlows: one per row (simple + predictable)
        List<Map<String,Object>> flows = new ArrayList<>();
        for (FlowRow f : nurseCalls) {
            if (isBlank(f.getAlarmName())) continue;

            Map<String,Object> flow = new LinkedHashMap<>();
            String flowName = "SEND NURSECALL | " + f.getPriority().toUpperCase() + " | " +
                    safeName(f.getAlarmName()) + " | " + safeName(f.getConfigGroup()) + " | " +
                    joinedFacilitiesForType(true);
            flow.put("name", flowName);
            flow.put("priority", f.getPriority());
            flow.put("status", "Active");

            // alarmsAlerts
            flow.put("alarmsAlerts", List.of(f.getAlarmName()));
            // interfaces
            flow.put("interfaces", List.of(Map.of("componentName","OutgoingWCTP","referenceName","OutgoingWCTP")));
            // parameters
            List<Map<String,Object>> params = new ArrayList<>();
            if (!isBlank(f.getRingtone())) params.add(param("alertSound", quote(f.getRingtone())));
            if (isUrgent(f.getPriority())) params.add(param("breakThrough", "\"voceraAndDevice\""));
            if (isNoResponse(f.getResponseOptions())) params.add(param("responseType", "\"None\""));
            flow.put("parameterAttributes", params);

            // destinations
            List<Map<String,Object>> dests = new ArrayList<>();
            addDest(dests, 0, f.getT1(), f.getR1());
            addDest(dests, 1, f.getT2(), f.getR2());
            addDest(dests, 2, f.getT3(), f.getR3());
            addDest(dests, 3, f.getT4(), f.getR4());
            flow.put("destinations", dests);

            // units from Unit Breakdown where inNurseCall = true
            flow.put("units", collectUnits(true));

            flows.add(flow);
        }
        root.put("deliveryFlows", flows);
        return root;
    }

    /** Builds "Clinicals" JSON document. */
    public Map<String, Object> buildClinicalsJson() {
        Map<String,Object> root = new LinkedHashMap<>();
        root.put("version", "1.1.0");

        // alarmAlertDefinitions
        List<Map<String,Object>> defs = new ArrayList<>();
        for (FlowRow f : clinicals) {
            if (isBlank(f.getAlarmName())) continue;
            Map<String,Object> def = new LinkedHashMap<>();
            def.put("name", f.getAlarmName());
            def.put("type", "Clinicals");
            List<Map<String,String>> values = new ArrayList<>();
            if (!isBlank(f.getSendingName())) {
                Map<String,String> v = new LinkedHashMap<>();
                v.put("value", f.getSendingName());
                values.add(v);
            }
            def.put("values", values);
            defs.add(def);
        }
        defs = dedupByKey(defs, m -> (m.get("name")+"|"+m.get("type")));
        root.put("alarmAlertDefinitions", defs);

        // deliveryFlows
        List<Map<String,Object>> flows = new ArrayList<>();
        for (FlowRow f : clinicals) {
            if (isBlank(f.getAlarmName())) continue;

            Map<String,Object> flow = new LinkedHashMap<>();
            String flowName = "SEND CLINICAL | " + f.getPriority().toUpperCase() + " | " +
                    safeName(f.getAlarmName()) + " | " + safeName(f.getConfigGroup()) + " | " +
                    joinedFacilitiesForType(false);
            flow.put("name", flowName);
            flow.put("priority", f.getPriority());
            flow.put("status", "Active");

            flow.put("alarmsAlerts", List.of(f.getAlarmName()));
            flow.put("interfaces", List.of(Map.of("componentName","OutgoingWCTP","referenceName","OutgoingWCTP")));

            List<Map<String,Object>> params = new ArrayList<>();
            if (!isBlank(f.getRingtone())) params.add(param("alertSound", quote(f.getRingtone())));
            if (isUrgent(f.getPriority())) params.add(param("breakThrough", "\"voceraAndDevice\""));
            if (isNoResponse(f.getResponseOptions())) params.add(param("responseType", "\"None\""));
            flow.put("parameterAttributes", params);

            List<Map<String,Object>> dests = new ArrayList<>();
            addDest(dests, 0, f.getT1(), f.getR1());
            addDest(dests, 1, f.getT2(), f.getR2());
            // Add fail-safe from Unit Breakdown per facility (No Caregiver Alert Number or Group)
            addFailSafe(dests);
            flow.put("destinations", dests);

            flow.put("units", collectUnits(false));
            flows.add(flow);
        }
        root.put("deliveryFlows", flows);
        return root;
    }

    private static boolean isUrgent(String p) { return "urgent".equalsIgnoreCase(p); }
    private static boolean isNoResponse(String r) { return norm(r).equals("no response"); }
    private static String quote(String s) { return s == null ? "\"\"" : "\"" + s.replace("\"","\\\"") + "\""; }
    private static boolean isBlank(String s) { return s == null || s.isBlank(); }
    private static Map<String,Object> param(String name, Object value) {
        Map<String,Object> m = new LinkedHashMap<>();
        m.put("name", name);
        m.put("value", value);
        return m;
    }

    private static <T> List<T> dedupByKey(List<T> list, java.util.function.Function<T,String> keyFn) {
        LinkedHashMap<String,T> map = new LinkedHashMap<>();
        for (T t : list) map.putIfAbsent(keyFn.apply(t), t);
        return new ArrayList<>(map.values());
    }

    private static String safeName(String s) {
        if (s == null) return "";
        return s.replaceAll("[\\r\\n]+"," ").trim();
    }

    private List<Map<String,String>> collectUnits(boolean forNurseCall) {
        List<Map<String,String>> out = new ArrayList<>();
        for (UnitRow u : units) {
            if (forNurseCall && !u.isInNurseCall()) continue;
            if (!forNurseCall && !u.isInPatientMonitoring()) continue;
            if (isBlank(u.getFacility()) || isBlank(u.getUnitName())) continue;
            out.add(Map.of("facilityName", u.getFacility(), "name", u.getUnitName()));
        }
        return out;
    }

    private String joinedFacilitiesForType(boolean forNurseCall) {
        return units.stream()
                .filter(u -> forNurseCall ? u.isInNurseCall() : u.isInPatientMonitoring())
                .map(UnitRow::getFacility)
                .filter(s -> !isBlank(s))
                .distinct()
                .collect(Collectors.joining("/"));
    }

    private void addFailSafe(List<Map<String,Object>> dests) {
        // For each distinct facility in the PM units, if it has a NoCaregiver alert group, add a "NoDeliveries" group destination
        Set<String> facilities = units.stream()
                .filter(UnitRow::isInPatientMonitoring)
                .map(UnitRow::getFacility)
                .filter(s -> !isBlank(s))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        int order = dests.size();
        for (String fac : facilities) {
            String group = noCaregiverByFacility.get(fac);
            if (!isBlank(group)) {
                Map<String,Object> d = baseDest(order++, 0);
                d.put("destinationType", "NoDeliveries");
                d.put("recipientType", "group");
                d.put("presenceConfig", "device");
                d.put("groups", List.of(Map.of("facilityName", fac, "name", group)));
                d.put("functionalRoles", List.of());
                d.put("users", List.of());
                dests.add(d);
            }
        }
    }

    private void addDest(List<Map<String,Object>> dests, int order, String delayStr, String recipient) {
        if (isBlank(recipient) && isBlank(delayStr)) return;
        Map<String,Object> d = baseDest(order, parseDelay(delayStr));

        Recipient parsed = parseRecipient(recipient);
        if (parsed.kind.equals("group")) {
            d.put("recipientType", "group");
            d.put("presenceConfig", "device");
            // attach to first available facility in this flow's units, else leave empty
            String facility = firstFacility();
            d.put("groups", facility == null ? List.of() : List.of(Map.of("facilityName", facility, "name", parsed.value)));
            d.put("functionalRoles", List.of());
        } else if (parsed.kind.equals("functional_role")) {
            d.put("recipientType", "functional_role");
            d.put("presenceConfig", "user_and_device");
            String facility = firstFacility();
            d.put("functionalRoles", facility == null ? List.of() : List.of(Map.of("facilityName", facility, "name", parsed.value)));
            d.put("groups", List.of());
        } else {
            // unknown → treat as group name raw
            String facility = firstFacility();
            d.put("recipientType", "group");
            d.put("presenceConfig", "device");
            d.put("groups", facility == null ? List.of() : List.of(Map.of("facilityName", facility, "name", parsed.value)));
            d.put("functionalRoles", List.of());
        }
        dests.add(d);
    }

    private Map<String,Object> baseDest(int order, int delay) {
        Map<String,Object> d = new LinkedHashMap<>();
        d.put("order", order);
        d.put("delayTime", delay);
        d.put("destinationType", "Normal");
        d.put("users", List.of());
        // groups / functionalRoles set in addDest
        return d;
    }

    private String firstFacility() {
        // Prefer any facility from units (either NC or PM); this is used only to tag recipient objects
        for (UnitRow u : units) {
            if (!isBlank(u.getFacility())) return u.getFacility();
        }
        return null;
    }

    private static Recipient parseRecipient(String s) {
        if (isBlank(s)) return new Recipient("unknown", "");
        String v = s.trim();
        String lower = v.toLowerCase(Locale.ROOT);
        if (lower.startsWith("vgroup:")) {
            String name = v.substring(v.indexOf(':') + 1).trim();
            return new Recipient("group", name);
        }
        if (lower.startsWith("vassign")) {
            // strip "VAssign: [Room]" prefix, keep the role
            String name = v.replaceFirst("(?i)vassign\\s*:\\s*\\[\\s*room\\s*]\\s*", "").trim();
            return new Recipient("functional_role", name);
        }
        return new Recipient("group", v); // default to group
    }

    private record Recipient(String kind, String value) {}

    // ------------------------------------------------------
    // Pretty JSON (simple)
    // ------------------------------------------------------
    public static String pretty(Object obj) {
        return pretty(obj, 0);
    }
    @SuppressWarnings("unchecked")
    public static String pretty(Object obj, int indent) {
        if (obj == null) return "null";
        if (obj instanceof String s) {
            String trimmed = s.trim();
            if ((trimmed.startsWith("\"") && trimmed.endsWith("\""))
                    || (trimmed.startsWith("{") && trimmed.endsWith("}"))
                    || (trimmed.startsWith("[") && trimmed.endsWith("]"))) {
                return s;
            }
            return quote(s);
        }
        if (obj instanceof Number || obj instanceof Boolean) return String.valueOf(obj);

        String ind = "  ".repeat(indent);
        String ind2 = "  ".repeat(indent + 1);

        if (obj instanceof Map<?,?> map) {
            StringBuilder sb = new StringBuilder();
            sb.append("{\n");
            var it = map.entrySet().iterator();
            int i = 0;
            while (it.hasNext()) {
                var e = it.next();
                if (i++ > 0) sb.append(",\n");
                sb.append(ind2).append("\"").append(e.getKey()).append("\": ");
                sb.append(pretty(e.getValue(), indent + 1));
            }
            sb.append("\n").append(ind).append("}");
            return sb.toString();
        }
        if (obj instanceof Collection<?> col) {
            StringBuilder sb = new StringBuilder();
            sb.append("[");
            int i = 0;
            for (Object o : col) {
                if (i++ > 0) sb.append(", ");
                sb.append(pretty(o, indent));
            }
            sb.append("]");
            return sb.toString();
        }
        return "\""+obj.toString()+"\"";
    }

    // ------------------------------------------------------
    // Write JSON files to disk next to the Excel
    // ------------------------------------------------------
    public File writeNurseCallsJson() throws IOException {
        Map<String,Object> json = buildNurseCallsJson();
        return writeJson(json, "NurseCalls.json");
    }
    public File writeClinicalsJson() throws IOException {
        Map<String,Object> json = buildClinicalsJson();
        return writeJson(json, "Clinicals.json");
    }

    public File writeJson(File output) throws IOException {
        Objects.requireNonNull(output, "output");
        Map<String, Object> combined = new LinkedHashMap<>();
        combined.put("nurseCalls", buildNurseCallsJson());
        combined.put("clinicals", buildClinicalsJson());
        return writeJsonToFile(combined, output);
    }

    private File writeJson(Map<String,Object> json, String name) throws IOException {
        File dir = (sourceExcel != null && sourceExcel.getParentFile() != null)
                ? sourceExcel.getParentFile()
                : new File(".");
        File out = new File(dir, name);
        return writeJsonToFile(json, out);
    }

    private File writeJsonToFile(Map<String, Object> json, File output) throws IOException {
        File parent = output.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        try (FileWriter fw = new FileWriter(output)) {
            fw.write(pretty(json));
            fw.write("\n");
        }
        return output;
    }

    // ------------------------------------------------------
    // UnitRow (JavaFX properties) – used by the UI table
    // ------------------------------------------------------
    public static class UnitRow {
        private final StringProperty facility = new SimpleStringProperty("");
        private final StringProperty unitName = new SimpleStringProperty("");
        private boolean inNurseCall;
        private boolean inPatientMonitoring;
        private final StringProperty noCaregiverGroup = new SimpleStringProperty("");

        public UnitRow(String facility, String unitName, boolean inNC, boolean inPM, String noCare) {
            setFacility(facility);
            setUnitName(unitName);
            this.inNurseCall = inNC;
            this.inPatientMonitoring = inPM;
            setNoCaregiverGroup(noCare);
        }

        public String getFacility() { return facility.get(); }
        public void setFacility(String v) { facility.set(v == null ? "" : v); }
        public StringProperty facilityProperty() { return facility; }

        public String getUnitName() { return unitName.get(); }
        public void setUnitName(String v) { unitName.set(v == null ? "" : v); }
        public StringProperty unitNameProperty() { return unitName; }

        public boolean isInNurseCall() { return inNurseCall; }
        public void setInNurseCall(boolean v) { inNurseCall = v; }

        public boolean isInPatientMonitoring() { return inPatientMonitoring; }
        public void setInPatientMonitoring(boolean v) { inPatientMonitoring = v; }

        public String getNoCaregiverGroup() { return noCaregiverGroup.get(); }
        public void setNoCaregiverGroup(String v) { noCaregiverGroup.set(v == null ? "" : v); }
        public StringProperty noCaregiverGroupProperty() { return noCaregiverGroup; }
    }
}
