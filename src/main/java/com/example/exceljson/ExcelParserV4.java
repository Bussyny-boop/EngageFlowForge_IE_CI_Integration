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

    private final Map<String, String> noCaregiverByFacility = new LinkedHashMap<>();
    private final Map<String, List<Map<String,String>>> nurseGroupToUnits = new LinkedHashMap<>();
    private final Map<String, List<Map<String,String>>> clinicalGroupToUnits = new LinkedHashMap<>();
    private File sourceExcel;

    private static final String SHEET_UNIT = "Unit Breakdown";
    private static final String SHEET_NURSE = "Nurse Call";
    private static final String SHEET_CLINICAL = "Patient Monitoring";

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
        Sheet sh = wb.getSheet(SHEET_UNIT);
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
        Sheet sh = wb.getSheet(SHEET_NURSE);
        if (sh == null) return;

        Map<String,Integer> hm = headerMap(findHeader(sh));
        Integer cCfg = col(hm, "Configuration Group");
        Integer cAlarm = col(hm, "Common Alert or Alarm Name");
        Integer cSend = col(hm, "Sending System Alert Name");
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
        Sheet sh = wb.getSheet(SHEET_CLINICAL);
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

    // -------------------- JSON GENERATION --------------------
    public static String pretty(Object obj) { return pretty(obj, 0); }
    @SuppressWarnings("unchecked")
    public static String pretty(Object obj, int indent) {
        if (obj == null) return "null";
        String ind = "  ".repeat(indent), ind2 = "  ".repeat(indent + 1);
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
            return col.stream().map(o -> pretty(o, indent + 1)).collect(Collectors.joining(", ", "[", "]"));
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
