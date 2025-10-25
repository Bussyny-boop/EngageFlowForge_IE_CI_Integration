package com.example.exceljson;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * ExcelParserV4
 * - Reads Unit Breakdown, Nurse Call, Patient Monitoring
 * - Links flows to units by Configuration Group (from Unit Breakdown)
 * - Applies all mapping rules (priority, breakthrough, ringtone->alertSound, VGroup/VAssign parsing, fail-safe)
 * - Exposes editable lists for UI (units, nurseCalls, clinicals)
 * - Writes two JSON files (NurseCalls.json, Clinicals.json) next to the source Excel
 * - Writes a clean edited Excel using current in-memory tables
 *
 * NOTE: This class expects FlowRow.java to be present (from your previous step).
 */
public class ExcelParserV4 {

    // ---- Public editable models (bound by the UI) ----
    public final List<UnitRow> units = new ArrayList<>();
    public final List<FlowRow> nurseCalls = new ArrayList<>();
    public final List<FlowRow> clinicals = new ArrayList<>();

    private static final String UNIT_SHEET_NAME = "Unit Breakdown";
    private static final String NURSE_SHEET_NAME = "Nurse call";
    private static final String CLINICAL_SHEET_NAME = "Patient Monitoring";

    // For Clinicals fail-safe: Facility -> "No Caregiver Alert Number or Group"
    private final Map<String, String> noCaregiverByFacility = new LinkedHashMap<>();

    // For unit lookups: group -> list of unit refs (separate maps per domain)
    private final Map<String, List<Map<String,String>>> nurseGroupToUnits = new LinkedHashMap<>();
    private final Map<String, List<Map<String,String>>> clinicalGroupToUnits = new LinkedHashMap<>();

    private File sourceExcel;

    public ExcelParserV4() {}

    // ------------------------------------------------------
    // Load
    // ------------------------------------------------------
    public void load(File excelFile) throws Exception {
        this.sourceExcel = excelFile;

        units.clear();
        nurseCalls.clear();
        clinicals.clear();
        noCaregiverByFacility.clear();
        nurseGroupToUnits.clear();
        clinicalGroupToUnits.clear();

        try (FileInputStream fis = new FileInputStream(excelFile);
             Workbook wb = new XSSFWorkbook(fis)) {

            parseUnitBreakdown(wb);        // fills units, group maps, fail-safe by facility
            parseNurseCall(wb);            // fills nurseCalls
            parseClinical(wb);             // fills clinicals
            // Nothing else needed; unit linking happens during JSON build using the group maps
        }
    }

    // ------------------------------------------------------
    // Parsing helpers
    // ------------------------------------------------------
    private static String norm(String s) {
        if (s == null) return "";
        return s.toLowerCase()
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
                        yield String.valueOf(cell.getDateCellValue().getTime());
                    } else {
                        double d = cell.getNumericCellValue();
                        if (Math.floor(d) == d) yield String.valueOf((long) d);
                        yield String.valueOf(d);
                    }
                }
                case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
                case FORMULA -> {
                    try { yield cell.getStringCellValue(); }
                    catch (Exception e) { yield String.valueOf(cell.getNumericCellValue()); }
                }
                default -> "";
            };
        } catch (Exception e) { return ""; }
    }
    private static Map<String,Integer> headerMap(Row header) {
        Map<String,Integer> map = new LinkedHashMap<>();
        if (header == null) return map;
        for (int c = 0; c < header.getLastCellNum(); c++) {
            String key = norm(cellString(header, c));
            if (!key.isEmpty()) map.put(key, c);
        }
        return map;
    }
    private static Integer col(Map<String,Integer> hm, String... names) {
        for (String n : names) {
            Integer idx = hm.get(norm(n));
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
    private static boolean isBlank(String s) { return s == null || s.isBlank(); }
    private static String nonEmpty(String v, String fallback) { return isBlank(v) ? fallback : v; }

    // ------------------------------------------------------
    // Unit Breakdown
    // ------------------------------------------------------
    private void parseUnitBreakdown(Workbook wb) {
        Sheet sh = wb.getSheet(UNIT_SHEET_NAME);
        if (sh == null) return;

        int headerRow = findLikelyHeader(sh, List.of("Facility","Common Unit Name","Nurse Call","Patient Monitoring"));
        if (headerRow < 0) return;

        Map<String,Integer> hm = headerMap(sh.getRow(headerRow));
        Integer cFacility = col(hm, "Facility");
        Integer cUnitName = col(hm, "Common Unit Name");
        Integer cNurseGroup = col(hm, "Nurse Call");
        Integer cClinGroup  = col(hm, "Patient Monitoring");
        Integer cNoCare     = col(hm, "No Caregiver Alert Number or Group");

        for (int r = headerRow + 1; r <= sh.getLastRowNum(); r++) {
            Row row = sh.getRow(r);
            if (row == null) continue;
            String facility = get(row, cFacility);
            String unitName = get(row, cUnitName);
            String nurseGroup = get(row, cNurseGroup);
            String clinGroup  = get(row, cClinGroup);
            String noCare     = get(row, cNoCare);

            if (facility.isBlank() && unitName.isBlank() && nurseGroup.isBlank() && clinGroup.isBlank()) continue;

            UnitRow u = new UnitRow(facility, unitName, nurseGroup, clinGroup, noCare);
            units.add(u);

            // map group -> unit refs
            if (!isBlank(nurseGroup) && !isBlank(facility) && !isBlank(unitName)) {
                nurseGroupToUnits.computeIfAbsent(nurseGroup, k -> new ArrayList<>())
                        .add(Map.of("facilityName", facility, "name", unitName));
            }
            if (!isBlank(clinGroup) && !isBlank(facility) && !isBlank(unitName)) {
                clinicalGroupToUnits.computeIfAbsent(clinGroup, k -> new ArrayList<>())
                        .add(Map.of("facilityName", facility, "name", unitName));
            }

            // fail-safe (No Caregiver) lookup
            if (!isBlank(facility) && !isBlank(noCare)) {
                noCaregiverByFacility.put(facility, noCare);
            }
        }
    }

    private static int findLikelyHeader(Sheet sh, List<String> mustContainAny) {
        int best = -1, bestScore = -1;
        for (int r = 0; r <= Math.min(40, sh.getLastRowNum()); r++) {
            Row row = sh.getRow(r);
            if (row == null) continue;
            StringBuilder sb = new StringBuilder();
            for (int c = 0; c < row.getLastCellNum(); c++) sb.append(' ').append(cellString(row, c));
            String joined = norm(sb.toString());
            int score = 0;
            for (String m : mustContainAny) if (joined.contains(norm(m))) score++;
            if (score > bestScore) { bestScore = score; best = r; }
        }
        return best;
    }

    // ------------------------------------------------------
    // Nurse Call
    // ------------------------------------------------------
    private void parseNurseCall(Workbook wb) {
        Sheet sh = wb.getSheet(NURSE_SHEET_NAME);
        if (sh == null) return;

        int headerRow = findLikelyHeader(sh, List.of("Configuration Group","Common Alert or Alarm Name","Priority"));
        if (headerRow < 0) return;

        Map<String,Integer> hm = headerMap(sh.getRow(headerRow));

        Integer cCfg      = col(hm, "Configuration Group");
        Integer cAlarm    = col(hm, "Common Alert or Alarm Name", "Alarm Name");
        Integer cSending  = col(hm, "Sending System Alert Name");
        Integer cPriority = col(hm, "Priority");
        Integer cDeviceA  = col(hm, "Device - A");
        Integer cRingA    = col(hm, "Ringtone Device - A");

        Integer cT1 = col(hm, "Time to 1st Recipient (after alarm triggers)");
        Integer cR1 = col(hm, "1st Recipient");
        Integer cT2 = col(hm, "Time to 2nd Recipient");
        Integer cR2 = col(hm, "2nd Recipient");
        Integer cT3 = col(hm, "Time to 3rd Recipient");
        Integer cR3 = col(hm, "3rd Recipient");
        Integer cT4 = col(hm, "Time to 4th Recipient");
        Integer cR4 = col(hm, "4th Recipient");

        Integer cResp    = col(hm, "Response Options");
        Integer cEMDAN   = col(hm, "EMDAN Compliant? (Y/N)");
        Integer cComments= col(hm, "Comments (Device - A)");

        for (int r = headerRow + 1; r <= sh.getLastRowNum(); r++) {
            Row row = sh.getRow(r);
            if (row == null) continue;

            String cfg     = get(row, cCfg);
            String alarm   = get(row, cAlarm);
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

            f.setT1(get(row, cT1)); f.setR1(get(row, cR1));
            f.setT2(get(row, cT2)); f.setR2(get(row, cR2));
            f.setT3(get(row, cT3)); f.setR3(get(row, cR3));
            f.setT4(get(row, cT4)); f.setR4(get(row, cR4));

            nurseCalls.add(f);
        }
    }

    // ------------------------------------------------------
    // Clinical (Patient Monitoring)
    // ------------------------------------------------------
    private void parseClinical(Workbook wb) {
        Sheet sh = wb.getSheet(CLINICAL_SHEET_NAME);
        if (sh == null) return;

        int headerRow = findLikelyHeader(sh, List.of("Configuration Group","Alarm Name","Priority"));
        if (headerRow < 0) return;

        Map<String,Integer> hm = headerMap(sh.getRow(headerRow));

        Integer cCfg      = col(hm, "Configuration Group");
        Integer cAlarm    = col(hm, "Alarm Name");
        Integer cSending  = col(hm, "Sending System Alarm Name");
        Integer cPriority = col(hm, "Priority");
        Integer cDeviceA  = col(hm, "Device - A");
        Integer cRingA    = col(hm, "Ringtone Device - A");

        Integer cT1 = col(hm, "Time to 1st Recipient (after alarm triggers)");
        Integer cR1 = col(hm, "1st Recipient");
        Integer cT2 = col(hm, "Time to 2nd Recipient");
        Integer cR2 = col(hm, "2nd Recipient");

        Integer cResp     = col(hm, "Response Options");
        Integer cEMDAN    = col(hm, "EMDAN Compliant? (Y)");
        Integer cComments = col(hm, "Comments (Device - A)");

        for (int r = headerRow + 1; r <= sh.getLastRowNum(); r++) {
            Row row = sh.getRow(r);
            if (row == null) continue;

            String cfg     = get(row, cCfg);
            String alarm   = get(row, cAlarm);
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

            f.setT1(get(row, cT1)); f.setR1(get(row, cR1));
            f.setT2(get(row, cT2)); f.setR2(get(row, cR2));

            clinicals.add(f);
        }
    }

    private static String mapPriority(String p) {
        String s = norm(p);
        if (s.contains("high"))   return "urgent";
        if (s.contains("medium")) return "high";
        if (s.contains("low"))    return "normal";
        return "normal";
    }
    private static boolean isUrgent(String priority) { return "urgent".equalsIgnoreCase(priority); }
    private static boolean isNoResponse(String resp) { return norm(resp).equals("no response"); }
    private static String quote(String s) {
        if (s == null) return "\"\"";
        StringBuilder sb = new StringBuilder();
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            switch (ch) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\b' -> sb.append("\\b");
                case '\f' -> sb.append("\\f");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (ch < 0x20) {
                        sb.append(String.format("\\u%04x", (int) ch));
                    } else {
                        sb.append(ch);
                    }
                }
            }
        }
        sb.append('"');
        return sb.toString();
    }
    private static String safeOneLine(String s) { return s == null ? "" : s.replaceAll("[\\r\\n]+"," ").trim(); }

    // ------------------------------------------------------
    // JSON build: NurseCalls
    // ------------------------------------------------------
    public Map<String,Object> buildNurseCallsJson() {
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
        defs = dedup(defs, m -> m.get("name")+"|"+m.get("type"));
        root.put("alarmAlertDefinitions", defs);

        // deliveryFlows (one per row)
        List<Map<String,Object>> flows = new ArrayList<>();
        for (FlowRow f : nurseCalls) {
            if (isBlank(f.getAlarmName())) continue;
            Map<String,Object> flow = new LinkedHashMap<>();

            String flowName = "SEND NURSECALL | "
                    + f.getPriority().toUpperCase() + " | "
                    + safeOneLine(f.getAlarmName()) + " | "
                    + safeOneLine(f.getConfigGroup()) + " | "
                    + joinedFacilities(nurseGroupToUnits.get(f.getConfigGroup()));
            flow.put("name", flowName);
            flow.put("priority", f.getPriority());
            flow.put("status", "Active");
            flow.put("alarmsAlerts", List.of(f.getAlarmName()));
            flow.put("interfaces", List.of(Map.of("componentName","OutgoingWCTP","referenceName","OutgoingWCTP")));

            // parameters
            List<Map<String,Object>> params = new ArrayList<>();
            if (!isBlank(f.getRingtone())) params.add(param("alertSound", f.getRingtone()));
            if (isUrgent(f.getPriority())) params.add(param("breakThrough", "voceraAndDevice"));
            if (isNoResponse(f.getResponseOptions())) params.add(param("responseType", "None"));
            flow.put("parameterAttributes", params);

            // destinations (up to 4)
            List<Map<String,Object>> dests = new ArrayList<>();
            addDest(dests, 0, f.getT1(), f.getR1(), nurseGroupToUnits.get(f.getConfigGroup()));
            addDest(dests, 1, f.getT2(), f.getR2(), nurseGroupToUnits.get(f.getConfigGroup()));
            addDest(dests, 2, f.getT3(), f.getR3(), nurseGroupToUnits.get(f.getConfigGroup()));
            addDest(dests, 3, f.getT4(), f.getR4(), nurseGroupToUnits.get(f.getConfigGroup()));
            flow.put("destinations", dests);

            // units
            List<Map<String,String>> unitsForGroup = nurseGroupToUnits.getOrDefault(f.getConfigGroup(), List.of());
            flow.put("units", new ArrayList<>(unitsForGroup));

            flows.add(flow);
        }
        root.put("deliveryFlows", flows);
        return root;
    }

    // ------------------------------------------------------
    // JSON build: Clinicals
    // ------------------------------------------------------
    public Map<String,Object> buildClinicalsJson() {
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
        defs = dedup(defs, m -> m.get("name")+"|"+m.get("type"));
        root.put("alarmAlertDefinitions", defs);

        // deliveryFlows (one per row)
        List<Map<String,Object>> flows = new ArrayList<>();
        for (FlowRow f : clinicals) {
            if (isBlank(f.getAlarmName())) continue;
            Map<String,Object> flow = new LinkedHashMap<>();

            String flowName = "SEND CLINICAL | "
                    + f.getPriority().toUpperCase() + " | "
                    + safeOneLine(f.getAlarmName()) + " | "
                    + safeOneLine(f.getConfigGroup()) + " | "
                    + joinedFacilities(clinicalGroupToUnits.get(f.getConfigGroup()));
            flow.put("name", flowName);
            flow.put("priority", f.getPriority());
            flow.put("status", "Active");
            flow.put("alarmsAlerts", List.of(f.getAlarmName()));
            flow.put("interfaces", List.of(Map.of("componentName","OutgoingWCTP","referenceName","OutgoingWCTP")));

            // parameters
            List<Map<String,Object>> params = new ArrayList<>();
            if (!isBlank(f.getRingtone())) params.add(param("alertSound", f.getRingtone()));
            if (isUrgent(f.getPriority())) params.add(param("breakThrough", "voceraAndDevice"));
            if (isNoResponse(f.getResponseOptions())) params.add(param("responseType", "None"));
            flow.put("parameterAttributes", params);

            // destinations: up to 2 recipients + fail-safe (NoDeliveries)
            List<Map<String,Object>> dests = new ArrayList<>();
            addDest(dests, 0, f.getT1(), f.getR1(), clinicalGroupToUnits.get(f.getConfigGroup()));
            addDest(dests, 1, f.getT2(), f.getR2(), clinicalGroupToUnits.get(f.getConfigGroup()));
            addFailSafe(dests, clinicalGroupToUnits.get(f.getConfigGroup()));
            flow.put("destinations", dests);

            // units
            List<Map<String,String>> unitsForGroup = clinicalGroupToUnits.getOrDefault(f.getConfigGroup(), List.of());
            flow.put("units", new ArrayList<>(unitsForGroup));

            flows.add(flow);
        }
        root.put("deliveryFlows", flows);
        return root;
    }

    // ---- helpers for JSON build ----
    private static Map<String,Object> param(String name, Object value) {
        Map<String,Object> m = new LinkedHashMap<>();
        m.put("name", name);
        m.put("value", value);
        return m;
    }
    private static <T> List<T> dedup(List<T> list, Function<Map<String,Object>, String> keyFn) {
        LinkedHashMap<String, T> map = new LinkedHashMap<>();
        for (T t : list) {
            @SuppressWarnings("unchecked")
            Map<String,Object> mm = (Map<String,Object>) t;
            map.putIfAbsent(keyFn.apply(mm), t);
        }
        return new ArrayList<>(map.values());
    }
    private static String joinedFacilities(List<Map<String,String>> unitRefs) {
        if (unitRefs == null || unitRefs.isEmpty()) return "";
        return unitRefs.stream()
                .map(m -> m.getOrDefault("facilityName",""))
                .filter(s -> !s.isBlank())
                .distinct()
                .collect(Collectors.joining("/"));
    }

    private void addFailSafe(List<Map<String,Object>> dests, List<Map<String,String>> unitRefs) {
        // For each distinct facility in the flow's units, add a NoDeliveries group if we have one
        if (unitRefs == null || unitRefs.isEmpty()) return;
        int order = dests.size();
        Set<String> facs = unitRefs.stream()
                .map(m -> m.getOrDefault("facilityName",""))
                .filter(s -> !s.isBlank())
                .collect(Collectors.toCollection(LinkedHashSet::new));
        for (String fac : facs) {
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

    private void addDest(List<Map<String,Object>> dests, int order, String delayStr, String recipient, List<Map<String,String>> unitRefs) {
        if (isBlank(recipient) && isBlank(delayStr)) return;
        Map<String,Object> d = baseDest(order, parseDelay(delayStr));

        Recipient parsed = parseRecipient(recipient);
        String fac = firstFacility(unitRefs);

        if ("group".equals(parsed.kind)) {
            d.put("recipientType", "group");
            d.put("presenceConfig", "device");
            d.put("groups", fac == null ? List.of() : List.of(Map.of("facilityName", fac, "name", parsed.value)));
            d.put("functionalRoles", List.of());
        } else if ("functional_role".equals(parsed.kind)) {
            d.put("recipientType", "functional_role");
            d.put("presenceConfig", "user_and_device");
            d.put("functionalRoles", fac == null ? List.of() : List.of(Map.of("facilityName", fac, "name", parsed.value)));
            d.put("groups", List.of());
        } else {
            // default to group
            d.put("recipientType", "group");
            d.put("presenceConfig", "device");
            d.put("groups", fac == null ? List.of() : List.of(Map.of("facilityName", fac, "name", parsed.value)));
            d.put("functionalRoles", List.of());
        }
        dests.add(d);
    }

    private static Map<String,Object> baseDest(int order, int delay) {
        Map<String,Object> d = new LinkedHashMap<>();
        d.put("order", order);
        d.put("delayTime", delay);
        d.put("destinationType", "Normal");
        d.put("users", List.of());
        return d;
    }

    private static String firstFacility(List<Map<String,String>> unitRefs) {
        if (unitRefs == null || unitRefs.isEmpty()) return null;
        for (Map<String,String> m : unitRefs) {
            String fac = m.get("facilityName");
            if (!isBlank(fac)) return fac;
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
            String name = v.replaceFirst("(?i)vassign\\s*:\\s*\\[\\s*room\\s*]\\s*", "").trim();
            return new Recipient("functional_role", name);
        }
        return new Recipient("group", v);
    }
    private record Recipient(String kind, String value) {}

    // ------------------------------------------------------
    // Pretty JSON
    // ------------------------------------------------------
    public static String pretty(Object obj) { return pretty(obj, 0); }
    @SuppressWarnings("unchecked")
    public static String pretty(Object obj, int indent) {
        if (obj == null) return "null";
        if (obj instanceof String s) return quote(s);
        if (obj instanceof Number || obj instanceof Boolean) return String.valueOf(obj);

        String ind = "  ".repeat(indent);
        String ind2 = "  ".repeat(indent + 1);

        if (obj instanceof Map<?,?> map) {
            StringBuilder sb = new StringBuilder();
            sb.append("{\n");
            int i = 0;
            for (var e : map.entrySet()) {
                if (i++ > 0) sb.append(",\n");
                sb.append(ind2).append(quote(String.valueOf(e.getKey()))).append(": ");
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
        return quote(String.valueOf(obj));
    }

    // ------------------------------------------------------
    // Write JSON next to the source Excel
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
        Map<String,Object> combined = new LinkedHashMap<>();
        combined.put("nurseCalls", buildNurseCallsJson());
        combined.put("clinicals", buildClinicalsJson());
        try (FileWriter fw = new FileWriter(output)) {
            fw.write(pretty(combined));
            fw.write("\n");
        }
        return output;
    }
    private File writeJson(Map<String,Object> json, String name) throws IOException {
        File dir = (sourceExcel != null && sourceExcel.getParentFile() != null)
                ? sourceExcel.getParentFile() : new File(".");
        File out = new File(dir, name);
        try (FileWriter fw = new FileWriter(out)) {
            fw.write(pretty(json));
            fw.write("\n");
        }
        return out;
    }

    // ------------------------------------------------------
    // Export edited Excel (writes from current in-memory tables)
    // ------------------------------------------------------
    public void exportEditedExcel(File out) throws Exception {
        try (Workbook wb = new XSSFWorkbook()) {
            // Unit Breakdown
            Sheet su = wb.createSheet(UNIT_SHEET_NAME);
            writeRow(su, 0,
                    "Facility","Common Unit Name","Nurse Call","Patient Monitoring","No Caregiver Alert Number or Group");
            int r = 1;
            for (UnitRow u : units) {
                writeRow(su, r++,
                        nvl(u.getFacility()), nvl(u.getUnitName()),
                        nvl(u.getNurseCallGroup()), nvl(u.getPatientMonitoringGroup()),
                        nvl(u.getNoCaregiverGroup()));
            }

            // Nurse Call
            Sheet sn = wb.createSheet(NURSE_SHEET_NAME);
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
                        nvl(f.getConfigGroup()), nvl(f.getAlarmName()), nvl(f.getSendingName()),
                        originalPriority(nvl(f.getPriority())), nvl(f.getDeviceA()), nvl(f.getRingtone()),
                        nvl(f.getT1()), nvl(f.getR1()),
                        nvl(f.getT2()), nvl(f.getR2()),
                        nvl(f.getT3()), nvl(f.getR3()),
                        nvl(f.getT4()), nvl(f.getR4()),
                        nvl(f.getResponseOptions()), nvl(f.getEmdan()), nvl(f.getComments()));
            }

            // Patient Monitoring
            Sheet sc = wb.createSheet(CLINICAL_SHEET_NAME);
            writeRow(sc, 0,
                    "Configuration Group","Alarm Name","Sending System Alarm Name",
                    "Priority","Device - A","Ringtone Device - A",
                    "Time to 1st Recipient (after alarm triggers)","1st Recipient",
                    "Time to 2nd Recipient","2nd Recipient",
                    "Response Options","EMDAN Compliant? (Y)","Comments (Device - A)");
            r = 1;
            for (FlowRow f : clinicals) {
                writeRow(sc, r++,
                        nvl(f.getConfigGroup()), nvl(f.getAlarmName()), nvl(f.getSendingName()),
                        originalPriority(nvl(f.getPriority())), nvl(f.getDeviceA()), nvl(f.getRingtone()),
                        nvl(f.getT1()), nvl(f.getR1()),
                        nvl(f.getT2()), nvl(f.getR2()),
                        nvl(f.getResponseOptions()), nvl(f.getEmdan()), nvl(f.getComments()));
            }

            try (FileOutputStream fos = new FileOutputStream(out)) {
                wb.write(fos);
            }
        }
    }

    private static String nvl(String s) { return s == null ? "" : s; }
    private static void writeRow(Sheet sh, int r, String... vals) {
        Row row = sh.createRow(r);
        for (int i = 0; i < vals.length; i++) {
            Cell c = row.createCell(i, CellType.STRING);
            c.setCellValue(vals[i] == null ? "" : vals[i]);
        }
    }
    private static String originalPriority(String mapped) {
        if ("urgent".equalsIgnoreCase(mapped)) return "High";
        if ("high".equalsIgnoreCase(mapped))   return "Medium";
        return "Low";
    }
}
