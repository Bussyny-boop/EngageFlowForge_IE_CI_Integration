package com.example.exceljson;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

public class ExcelParserV4 {

    public final List<UnitRow> units = new ArrayList<>();
    public final List<FlowRow> nurseCalls = new ArrayList<>();
    public final List<FlowRow> clinicals = new ArrayList<>();

    private final Map<String, List<Map<String,String>>> nurseGroupToUnits = new LinkedHashMap<>();
    private final Map<String, List<Map<String,String>>> clinicalGroupToUnits = new LinkedHashMap<>();
    private final Map<String, String> noCaregiverByFacility = new LinkedHashMap<>();

    private static final String SHEET_UNIT = "Unit Breakdown";
    private static final String SHEET_NURSE = "Nurse Call";
    private static final String SHEET_CLINICAL = "Patient Monitoring";

    public ExcelParserV4() {}

    // -------------------- MAIN LOAD --------------------
    public void load(File excelFile) throws Exception {
        units.clear(); nurseCalls.clear(); clinicals.clear();
        nurseGroupToUnits.clear(); clinicalGroupToUnits.clear(); noCaregiverByFacility.clear();

        try (FileInputStream fis = new FileInputStream(excelFile);
             Workbook wb = new XSSFWorkbook(fis)) {
            parseUnitBreakdown(wb);
            parseNurseCall(wb);
            parseClinical(wb);
        }
    }

    // -------------------- SUMMARY --------------------
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
        Sheet sh = findSheet(wb, SHEET_UNIT);
        if (sh == null) {
            System.out.println("⚠️ No Unit Breakdown sheet found.");
            return;
        }

        // 🔍 Find header row dynamically
        int headerRowIndex = findHeaderRow(sh, List.of("facility", "common unit name"));
        if (headerRowIndex < 0) {
            System.out.println("⚠️ Could not locate header row in Unit Breakdown.");
            return;
        }

        Row headerRow = sh.getRow(headerRowIndex);
        Map<String,Integer> hm = headerMap(headerRow);
        System.out.println("🧭 Unit Breakdown headers detected: " + hm.keySet());

        int cFacility = getCol(hm, "Facility");
        int cUnitName = getCol(hm, "Common Unit Name");
        int cNurseGroup = getCol(hm, "Nurse Call");
        int cClinGroup = getCol(hm, "Patient Monitoring");
        int cNoCare = getCol(hm, "No Caregiver Alert Number or Group");

        for (int r = headerRowIndex + 1; r <= sh.getLastRowNum(); r++) {
            Row row = sh.getRow(r);
            if (row == null) continue;

            String facility = getCell(row, cFacility);
            String unitNames = getCell(row, cUnitName);
            String nurseGroup = getCell(row, cNurseGroup);
            String clinGroup = getCell(row, cClinGroup);
            String noCare = getCell(row, cNoCare);

            if (facility.isEmpty() && unitNames.isEmpty()) continue;

            UnitRow u = new UnitRow(facility, unitNames, nurseGroup, clinGroup, noCare);
            units.add(u);

            List<String> list = splitUnits(unitNames);
            for (String name : list) {
                if (!nurseGroup.isEmpty())
                    nurseGroupToUnits.computeIfAbsent(nurseGroup, k -> new ArrayList<>())
                            .add(Map.of("facilityName", facility, "name", name));
                if (!clinGroup.isEmpty())
                    clinicalGroupToUnits.computeIfAbsent(clinGroup, k -> new ArrayList<>())
                            .add(Map.of("facilityName", facility, "name", name));
            }

            if (!facility.isEmpty() && !noCare.isEmpty())
                noCaregiverByFacility.put(facility, noCare);
        }
    }

    // -------------------- NURSE CALL --------------------
    private void parseNurseCall(Workbook wb) {
        Sheet sh = findSheet(wb, SHEET_NURSE);
        if (sh == null) {
            System.out.println("⚠️ No Nurse Call sheet found.");
            return;
        }

        int headerRowIndex = findHeaderRow(sh, List.of("configuration group", "common alert"));
        if (headerRowIndex < 0) {
            System.out.println("⚠️ Could not locate header row in Nurse Call sheet.");
            return;
        }

        Row headerRow = sh.getRow(headerRowIndex);
        Map<String,Integer> hm = headerMap(headerRow);
        System.out.println("🧭 Nurse Call headers detected: " + hm.keySet());

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

        for (int r = headerRowIndex + 1; r <= sh.getLastRowNum(); r++) {
            Row row = sh.getRow(r);
            if (row == null) continue;

            FlowRow f = new FlowRow();
            f.setType("NurseCalls");
            f.setConfigGroup(getCell(row, cCfg));
            f.setAlarmName(getCell(row, cAlarm));
            f.setSendingName(getCell(row, cSend));

            String rawPriority = getCell(row, cPriority);
            f.setPriority(rawPriority.isEmpty() ? "" : mapPriority(rawPriority));

            f.setDeviceA(getCell(row, cDevice));
            f.setRingtone(getCell(row, cRing));
            f.setResponseOptions(getCell(row, cResp));
            f.setT1(getCell(row, cT1));
            f.setR1(getCell(row, cR1));
            f.setT2(getCell(row, cT2));
            f.setR2(getCell(row, cR2));

            nurseCalls.add(f);
        }
    }

    // -------------------- CLINICAL --------------------
    private void parseClinical(Workbook wb) {
        Sheet sh = findSheet(wb, SHEET_CLINICAL);
        if (sh == null) {
            System.out.println("⚠️ No Patient Monitoring sheet found.");
            return;
        }

        int headerRowIndex = findHeaderRow(sh, List.of("configuration group", "alarm name"));
        if (headerRowIndex < 0) {
            System.out.println("⚠️ Could not locate header row in Patient Monitoring sheet.");
            return;
        }

        Row headerRow = sh.getRow(headerRowIndex);
        Map<String,Integer> hm = headerMap(headerRow);
        System.out.println("🧭 Patient Monitoring headers detected: " + hm.keySet());

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

        for (int r = headerRowIndex + 1; r <= sh.getLastRowNum(); r++) {
            Row row = sh.getRow(r);
            if (row == null) continue;

            FlowRow f = new FlowRow();
            f.setType("Clinicals");
            f.setConfigGroup(getCell(row, cCfg));
            f.setAlarmName(getCell(row, cAlarm));
            f.setSendingName(getCell(row, cSend));

            String rawPriority = getCell(row, cPriority);
            f.setPriority(rawPriority.isEmpty() ? "" : mapPriority(rawPriority));

            f.setDeviceA(getCell(row, cDevice));
            f.setRingtone(getCell(row, cRing));
            f.setResponseOptions(getCell(row, cResp));
            f.setT1(getCell(row, cT1));
            f.setR1(getCell(row, cR1));
            f.setT2(getCell(row, cT2));
            f.setR2(getCell(row, cR2));

            clinicals.add(f);
        }
    }

    // -------------------- JSON PRETTY --------------------
    public static String pretty(Object obj) { return pretty(obj, 0); }

    @SuppressWarnings("unchecked")
    public static String pretty(Object obj, int indent) {
        if (obj == null) return "null";
        String ind = "  ".repeat(indent);
        String ind2 = "  ".repeat(indent + 1);
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
        if (obj instanceof Collection<?> col)
            return col.stream().map(o -> pretty(o, indent + 1)).collect(Collectors.joining(", ", "[", "]"));
        return "\"" + obj.toString() + "\"";
    }

    // -------------------- HELPERS --------------------
    private Sheet findSheet(Workbook wb, String... names) {
        if (wb == null) return null;
        Set<String> normalized = Arrays.stream(names)
                .filter(Objects::nonNull)
                .map(ExcelParserV4::normSheetName)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        for (int i = 0; i < wb.getNumberOfSheets(); i++) {
            Sheet sheet = wb.getSheetAt(i);
            if (normalized.contains(normSheetName(wb.getSheetName(i))))
                return sheet;
        }
        return null;
    }

    private static int findHeaderRow(Sheet sh, List<String> keywords) {
        for (int i = 0; i < 10 && i <= sh.getLastRowNum(); i++) {
            Row row = sh.getRow(i);
            if (row == null) continue;
            for (Cell cell : row) {
                String val = cell.toString().trim().toLowerCase();
                for (String key : keywords) {
                    if (val.contains(key)) return i;
                }
            }
        }
        return -1;
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
        return c == null ? "" : c.toString().trim();
    }

    private static int getCol(Map<String,Integer> map, String... names) {
        if (names == null) return -1;
        for (String name : names) {
            if (name == null) continue;
            int idx = map.getOrDefault(norm(name), -1);
            if (idx >= 0) return idx;
        }
        return -1;
    }

    private static String norm(String s) {
        return s == null ? "" : s.toLowerCase().replaceAll("[^a-z0-9]+", " ").trim();
    }

    private static String mapPriority(String p) {
        if (p == null) return "";
        String s = norm(p);
        if (s.isEmpty()) return "";
        if (s.contains("high")) return "urgent";
        if (s.contains("medium")) return "high";
        if (s.contains("low")) return "normal";
        return s; // preserve custom text like “Critical”
    }

    private static List<String> splitUnits(String s) {
        if (s == null) return List.of();
        return Arrays.stream(s.split("[,;]"))
                .map(String::trim)
                .filter(v -> !v.isEmpty())
                .collect(Collectors.toList());
    }

    // -------------------- BUILD JSON OUTPUT --------------------
    public Map<String, Object> buildNurseCallsJson() {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("version", "1.1.0");
        List<Map<String, Object>> flows = new ArrayList<>();

        for (FlowRow row : nurseCalls) {
            if (row.getAlarmName().isEmpty()) continue;
            Map<String, Object> flow = new LinkedHashMap<>();
            flow.put("name", row.getAlarmName());
            flow.put("type", "NurseCalls");
            if (!row.getPriority().isEmpty()) flow.put("priority", row.getPriority());
            if (!row.getRingtone().isEmpty()) flow.put("ringtone", row.getRingtone());
            if (!row.getResponseOptions().isEmpty()) flow.put("responseOptions", row.getResponseOptions());

            List<Map<String, String>> unitsList = getUnitsForGroup(row.getConfigGroup(), nurseGroupToUnits);
            if (!unitsList.isEmpty()) flow.put("units", unitsList);

            flows.add(flow);
        }

        root.put("deliveryFlows", flows);
        return root;
    }

    public Map<String, Object> buildClinicalsJson() {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("version", "1.1.0");
        List<Map<String, Object>> flows = new ArrayList<>();

        for (FlowRow row : clinicals) {
            if (row.getAlarmName().isEmpty()) continue;
            Map<String, Object> flow = new LinkedHashMap<>();
            flow.put("name", row.getAlarmName());
            flow.put("type", "Clinicals");
            if (!row.getPriority().isEmpty()) flow.put("priority", row.getPriority());
            if (!row.getRingtone().isEmpty()) flow.put("ringtone", row.getRingtone());
            if (!row.getResponseOptions().isEmpty()) flow.put("responseOptions", row.getResponseOptions());

            List<Map<String, String>> unitsList = getUnitsForGroup(row.getConfigGroup(), clinicalGroupToUnits);
            if (!unitsList.isEmpty()) flow.put("units", unitsList);

            flows.add(flow);
        }

        root.put("deliveryFlows", flows);
        return root;
    }

    private List<Map<String,String>> getUnitsForGroup(String group, Map<String, List<Map<String,String>>> map) {
        if (group == null || group.isEmpty()) return List.of();
        return map.getOrDefault(group, List.of());
    }

    public void writeJson(File file) throws Exception {
        if (file.getParentFile() != null && !file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }

        Map<String, Object> nurseCallsJson = buildNurseCallsJson();
        Map<String, Object> clinicalsJson = buildClinicalsJson();

        try (FileWriter writer = new FileWriter(file, false)) {
            writer.write("{\n");
            writer.write("  \"NurseCalls\": " + pretty(nurseCallsJson, 1) + ",\n");
            writer.write("  \"Clinicals\": " + pretty(clinicalsJson, 1) + "\n");
            writer.write("}\n");
            writer.flush();
        }

        if (!file.exists() || file.length() == 0)
            throw new RuntimeException("File write failed or empty: " + file.getAbsolutePath());

        System.out.println("✅ JSON successfully written to " + file.getAbsolutePath());
    }
}
