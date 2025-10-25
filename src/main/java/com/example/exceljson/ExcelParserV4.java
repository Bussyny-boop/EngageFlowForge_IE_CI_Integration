package com.example.exceljson;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.*;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * ExcelParserV4 (Updated)
 * - Fixes configuration groups not being treated as units
 * - Handles comma-separated units
 * - Provides count summary for popup display
 */
public class ExcelParserV4 {

    public final List<UnitRow> units = new ArrayList<>();
    public final List<FlowRow> nurseCalls = new ArrayList<>();
    public final List<FlowRow> clinicals = new ArrayList<>();

    private static final String UNIT_SHEET_NAME = "Unit Breakdown";
    private static final String NURSE_SHEET_NAME = "Nurse call";
    private static final String CLINICAL_SHEET_NAME = "Patient Monitoring";

    // For Clinicals fail-safe: Facility -> "No Caregiver Alert Number or Group"
    private final Map<String, String> noCaregiverByFacility = new LinkedHashMap<>();
    private final Map<String, List<Map<String,String>>> nurseGroupToUnits = new LinkedHashMap<>();
    private final Map<String, List<Map<String,String>>> clinicalGroupToUnits = new LinkedHashMap<>();
    private File sourceExcel;

    public ExcelParserV4() {}

    public void load(File excelFile) throws Exception {
        this.sourceExcel = excelFile;
        units.clear(); nurseCalls.clear(); clinicals.clear();
        nurseGroupToUnits.clear(); clinicalGroupToUnits.clear(); noCaregiverByFacility.clear();

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

    // -------------------- UNIT BREAKDOWN --------------------
    private void parseUnitBreakdown(Workbook wb) {
        Sheet sh = wb.getSheet(UNIT_SHEET_NAME);
        if (sh == null) return;

        Map<String,Integer> hm = headerMap(findHeader(sh));
        Integer cFacility = col(hm, "Facility");
        Integer cUnitName = col(hm, "Common Unit Name");
        Integer cNurseGroup = col(hm, "Nurse Call");
        Integer cClinGroup = col(hm, "Patient Monitoring");
        Integer cNoCare = col(hm, "No Caregiver Alert Number or Group");

        for (int r = 1; r <= sh.getLastRowNum(); r++) {
            Row row = sh.getRow(r);
            if (row == null) continue;

            String facility = get(row, cFacility);
            String unitNames = get(row, cUnitName);
            String nurseGroup = get(row, cNurseGroup);
            String clinGroup = get(row, cClinGroup);
            String noCare = get(row, cNoCare);

            if (facility.isBlank() && unitNames.isBlank()) continue;

            UnitRow u = new UnitRow(facility, unitNames, nurseGroup, clinGroup, noCare);
            units.add(u);

            List<String> unitList = splitUnits(unitNames);
            for (String name : unitList) {
                if (!nurseGroup.isBlank())
                    nurseGroupToUnits.computeIfAbsent(nurseGroup, k -> new ArrayList<>())
                            .add(Map.of("facilityName", facility, "name", name));
                if (!clinGroup.isBlank())
                    clinicalGroupToUnits.computeIfAbsent(clinGroup, k -> new ArrayList<>())
                            .add(Map.of("facilityName", facility, "name", name));
            }

            if (!facility.isBlank() && !noCare.isBlank())
                noCaregiverByFacility.put(facility, noCare);
        }
    }

    private static List<String> splitUnits(String s) {
        if (s == null) return List.of();
        return Arrays.stream(s.split("[,;]"))
                .map(String::trim)
                .filter(v -> !v.isEmpty())
                .collect(Collectors.toList());
    }

    // -------------------- NURSE CALL --------------------
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
        Integer cDevice = col(hm, "Device - A");
        Integer cRingtone = col(hm, "Ringtone Device - A");
        Integer cResp = col(hm, "Response Options");
        Integer cT1 = col(hm, "Time to 1st Recipient (after alarm triggers)");
        Integer cR1 = col(hm, "1st Recipient");
        Integer cT2 = col(hm, "Time to 2nd Recipient");
        Integer cR2 = col(hm, "2nd Recipient");

        for (int r = 1; r <= sh.getLastRowNum(); r++) {
            Row row = sh.getRow(r);
            if (row == null) continue;
            FlowRow f = new FlowRow();
            f.setType("NurseCalls");
            f.setConfigGroup(get(row, cCfg));
            f.setAlarmName(get(row, cAlarm));
            f.setSendingName(get(row, cSend));
            f.setPriority(mapPriority(get(row, cPriority)));
            f.setDeviceA(get(row, cDevice));
            f.setRingtone(get(row, cRingtone));
            f.setResponseOptions(get(row, cResp));
            f.setT1(get(row, cT1)); f.setR1(get(row, cR1));
            f.setT2(get(row, cT2)); f.setR2(get(row, cR2));
            nurseCalls.add(f);
        }
    }

    // -------------------- CLINICALS --------------------
    private void parseClinical(Workbook wb) {
        Sheet sh = wb.getSheet(CLINICAL_SHEET_NAME);
        if (sh == null) return;

        Map<String,Integer> hm = headerMap(findHeader(sh));
        Integer cCfg = col(hm, "Configuration Group");
        Integer cAlarm = col(hm, "Alarm Name");
        Integer cSend = col(hm, "Sending System Alarm Name");
        Integer cPriority = col(hm, "Priority");
        Integer cDevice = col(hm, "Device - A");
        Integer cRingtone = col(hm, "Ringtone Device - A");
        Integer cResp = col(hm, "Response Options");
        Integer cT1 = col(hm, "Time to 1st Recipient (after alarm triggers)");
        Integer cR1 = col(hm, "1st Recipient");
        Integer cT2 = col(hm, "Time to 2nd Recipient");
        Integer cR2 = col(hm, "2nd Recipient");

        for (int r = 1; r <= sh.getLastRowNum(); r++) {
            Row row = sh.getRow(r);
            if (row == null) continue;
            FlowRow f = new FlowRow();
            f.setType("Clinicals");
            f.setConfigGroup(get(row, cCfg));
            f.setAlarmName(get(row, cAlarm));
            f.setSendingName(get(row, cSend));
            f.setPriority(mapPriority(get(row, cPriority)));
            f.setDeviceA(get(row, cDevice));
            f.setRingtone(get(row, cRingtone));
            f.setResponseOptions(get(row, cResp));
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
            StringBuilder sb = new StringBuilder("{\n");
            int i = 0;
            for (var e : map.entrySet()) {
                if (i++ > 0) sb.append(",\n");
                sb.append(ind2).append(quote(String.valueOf(e.getKey()))).append(": ");
                sb.append(pretty(e.getValue(), indent + 1));
            }
            return sb.append("\n").append(ind).append("}").toString();
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
        return "\"" + obj.toString() + "\"";
    }

    // -------------------- HELPERS --------------------
    private static Map<String,Integer> headerMap(Row r) {
        Map<String,Integer> m = new LinkedHashMap<>();
        if (r == null) return m;
        for (int i=0;i<r.getLastCellNum();i++){
            String key = norm(cellString(r,i));
            if(!key.isEmpty())m.put(key,i);
        }
        return m;
    }
    private static Row findHeader(Sheet s){return s.getRow(0);}
    private static Integer col(Map<String,Integer> hm,String...names){
        for(String n:names){Integer i=hm.get(norm(n));if(i!=null)return i;}
        return null;
    }
    private static String get(Row r,Integer i){return i==null?"":cellString(r,i).trim();}
    private static String cellString(Row r,int c){if(r==null)return"";Cell x=r.getCell(c);if(x==null)return"";return x.toString();}
    private static String norm(String s){return s==null?"":s.toLowerCase().replaceAll("[^a-z0-9]+"," ").trim();}
    private static String mapPriority(String p){String s=norm(p);if(s.contains("high"))return"urgent";if(s.contains("medium"))return"high";return"normal";}
}
