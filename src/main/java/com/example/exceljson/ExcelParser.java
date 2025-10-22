package com.example.exceljson;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.util.*;
import java.util.stream.Collectors;

import static com.example.exceljson.HeaderFinder.*;

public class ExcelParser {
    private final Config config;
    private Workbook wb;

    // Raw rows as maps (for preview tables)
    private List<Map<String,String>> unitRows = new ArrayList<>();
    private List<Map<String,String>> nurseCallRows = new ArrayList<>();
    private List<Map<String,String>> patientMonRows = new ArrayList<>();

    public ExcelParser(Config config) { this.config = config; }

    public void load(File excel) throws Exception {
        try (FileInputStream fis = new FileInputStream(excel)) {
            wb = new XSSFWorkbook(fis);
        }
        parseUnitBreakdown();
        parseNurseCall();
        parsePatientMonitoring();
    }

    public List<Map<String,String>> getUnitBreakdownRows(){ return unitRows; }
    public List<Map<String,String>> getNurseCallRows(){ return nurseCallRows; }
    public List<Map<String,String>> getPatientMonitoringRows(){ return patientMonRows; }

    // ---- Parsing helpers ----
    private Map<String,Integer> headerIndex(Row header) {
        Map<String,Integer> map = new LinkedHashMap<>();
        for (int c = 0; c < header.getLastCellNum(); c++) {
            String key = normalize(getString(header, c));
            if (!key.isBlank()) map.put(key, c);
        }
        return map;
    }
    private String get(Row row, Map<String,Integer> hmap, Set<String> aliases){
        for (String a : aliases) {
            Integer idx = hmap.get(a);
            if (idx != null) return HeaderFinder.getString(row, idx).trim();
        }
        return "";
    }

    private void parseUnitBreakdown() {
        Sheet sheet = wb.getSheet(config.sheets.unitBreakdown);
        if (sheet == null) return;
        Set<String> expected = new HashSet<>();
        expected.addAll(config.aliases.FACILITY);
        expected.addAll(config.aliases.UNIT_NAME);
        expected.addAll(config.aliases.UNIT_GROUPS);

        int headerRowIdx = HeaderFinder.findHeaderRow(sheet, expected, 30);
        if (headerRowIdx < 0) return;
        Row header = sheet.getRow(headerRowIdx);
        Map<String,Integer> hmap = headerIndex(header);

        for (int r = headerRowIdx + 1; r <= sheet.getLastRowNum(); r++) {
            Row row = sheet.getRow(r);
            if (row == null) continue;
            String facility = get(row, hmap, config.aliases.FACILITY);
            String unit = get(row, hmap, config.aliases.UNIT_NAME);
            String groups = get(row, hmap, config.aliases.UNIT_GROUPS);
            if (facility.isBlank() && unit.isBlank() && groups.isBlank()) continue;

            Map<String,String> map = new LinkedHashMap<>();
            map.put("Facility", facility);
            map.put("Common Unit Name", unit);
            map.put("Groups", groups);
            unitRows.add(map);
        }
    }

    private void parseNurseCall() {
        Sheet sheet = wb.getSheet(config.sheets.nurseCall);
        if (sheet == null) return;
        Set<String> expected = new HashSet<>() {{
            addAll(config.aliases.CFG_GROUP);
            addAll(config.aliases.ALERT_NAME_COMMON);
            addAll(config.aliases.SENDING_NAME);
            addAll(config.aliases.PRIORITY);
        }};
        int headerRowIdx = HeaderFinder.findHeaderRow(sheet, expected, 40);
        if (headerRowIdx < 0) return;
        Row header = sheet.getRow(headerRowIdx);
        Map<String,Integer> hmap = headerIndex(header);

        for (int r = headerRowIdx + 1; r <= sheet.getLastRowNum(); r++) {
            Row row = sheet.getRow(r);
            if (row == null) continue;
            String cfg = get(row, hmap, config.aliases.CFG_GROUP);
            String alert = get(row, hmap, config.aliases.ALERT_NAME_COMMON);
            String send = get(row, hmap, config.aliases.SENDING_NAME);
            if (cfg.isBlank() && alert.isBlank() && send.isBlank()) continue;

            Map<String,String> m = new LinkedHashMap<>();
            m.put("Configuration Group", cfg);
            m.put("Common Alert or Alarm Name", alert);
            m.put("Sending System Alert Name", send);
            m.put("Priority", get(row, hmap, config.aliases.PRIORITY));
            m.put("Device - A", get(row, hmap, config.aliases.DEVICE_A));
            m.put("Ringtone Device - A", get(row, hmap, config.aliases.RINGTONE_A));
            m.put("Time to 1st Recipient", get(row, hmap, config.aliases.T1));
            m.put("1st Recipient", get(row, hmap, config.aliases.R1));
            m.put("Time to 2nd Recipient", get(row, hmap, config.aliases.T2));
            m.put("2nd Recipient", get(row, hmap, config.aliases.R2));
            m.put("Response Options", get(row, hmap, config.aliases.RESPONSE));
            m.put("EMDAN Compliant?", get(row, hmap, config.aliases.EMDAN));
            m.put("Comments", get(row, hmap, config.aliases.COMMENTS));
            nurseCallRows.add(m);
        }
    }

    private void parsePatientMonitoring() {
        Sheet sheet = wb.getSheet(config.sheets.patientMonitoring);
        if (sheet == null) return;
        Set<String> expected = new HashSet<>() {{
            addAll(config.aliases.CFG_GROUP);
            addAll(config.aliases.ALERT_NAME_COMMON);
            addAll(config.aliases.SENDING_NAME);
            addAll(config.aliases.PRIORITY);
        }};
        int headerRowIdx = HeaderFinder.findHeaderRow(sheet, expected, 40);
        if (headerRowIdx < 0) return;
        Row header = sheet.getRow(headerRowIdx);
        Map<String,Integer> hmap = headerIndex(header);

        for (int r = headerRowIdx + 1; r <= sheet.getLastRowNum(); r++) {
            Row row = sheet.getRow(r);
            if (row == null) continue;
            String cfg = get(row, hmap, config.aliases.CFG_GROUP);
            String alert = get(row, hmap, config.aliases.ALERT_NAME_COMMON); // "Alarm Name"
            String send = get(row, hmap, config.aliases.SENDING_NAME);
            if (cfg.isBlank() && alert.isBlank() && send.isBlank()) continue;

            Map<String,String> m = new LinkedHashMap<>();
            m.put("Configuration Group", cfg);
            m.put("Alarm Name", alert);
            m.put("Sending System Alarm Name", send);
            m.put("Priority", get(row, hmap, config.aliases.PRIORITY));
            m.put("Device - A", get(row, hmap, config.aliases.DEVICE_A));
            m.put("1st Recipient", get(row, hmap, config.aliases.R1));
            m.put("2nd Recipient", get(row, hmap, config.aliases.R2));
            m.put("Response Options", get(row, hmap, config.aliases.RESPONSE));
            patientMonRows.add(m);
        }
    }

    // ---- Build JSON in desired structure ----
    public Map<String, Object> buildJson() {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("version", config.outputVersion);

        // alarmAlertDefinitions (from both NC + PM)
        List<Map<String, Object>> definitions = new ArrayList<>();

        for (var r : nurseCallRows) {
            String name = coalesce(r.get("Common Alert or Alarm Name"), r.get("Alarm Name"));
            String send = coalesce(r.get("Sending System Alert Name"), r.get("Sending System Alarm Name"));
            if (name == null || name.isBlank()) continue;
            Map<String,Object> def = new LinkedHashMap<>();
            def.put("name", name);
            def.put("type", "NurseCall");
            List<Map<String,String>> values = new ArrayList<>();
            if (send != null && !send.isBlank()) values.add(Map.of("category","SendingSystem","value", send));
            def.put("values", values);
            definitions.add(def);
        }
        for (var r : patientMonRows) {
            String name = coalesce(r.get("Alarm Name"), r.get("Common Alert or Alarm Name"));
            String send = coalesce(r.get("Sending System Alarm Name"), r.get("Sending System Alert Name"));
            if (name == null || name.isBlank()) continue;
            Map<String,Object> def = new LinkedHashMap<>();
            def.put("name", name);
            def.put("type", "PatientMonitoring");
            List<Map<String,String>> values = new ArrayList<>();
            if (send != null && !send.isBlank()) values.add(Map.of("category","SendingSystem","value", send));
            def.put("values", values);
            definitions.add(def);
        }
        // de-duplicate by name+type
        definitions = definitions.stream()
                .collect(Collectors.toMap(
                        m -> (m.get("name")+"|"+m.get("type")),
                        m -> m,
                        (a,b)->a,
                        LinkedHashMap::new
                ))
                .values().stream().collect(Collectors.toList());
        root.put("alarmAlertDefinitions", definitions);

        // deliveryFlows grouped by Configuration Group
        Map<String, Map<String,Object>> flowsByGroup = new LinkedHashMap<>();

        // Nurse Call rows
        for (var r : nurseCallRows) {
            String cfg = nvl(r.get("Configuration Group"), "General NC");
            Map<String,Object> flow = flowsByGroup.computeIfAbsent(cfg, k -> baseFlow(k, nvl(r.get("Priority"), "Medium")));
            // alarmsAlerts link
            String alert = nvl(r.get("Common Alert or Alarm Name"), null);
            if (alert != null) addToSetList(flow, "alarmsAlerts", alert);
            // interfaces and params
            addInterfaces(flow, r.get("Device - A"));
            addParam(flow, 0, "RingtoneDeviceA", r.get("Ringtone Device - A"));
            addParam(flow, 0, "ResponseOptions", r.get("Response Options"));
            addParam(flow, 0, "EMDAN", r.get("EMDAN Compliant?"));
            addParam(flow, 0, "Comments", r.get("Comments"));
            // recipients with parsing
            addRecipients(flow, r.get("Time to 1st Recipient"), r.get("1st Recipient"), 1);
            addRecipients(flow, r.get("Time to 2nd Recipient"), r.get("2nd Recipient"), 2);
        }
        // Patient Monitoring rows
        for (var r : patientMonRows) {
            String cfg = nvl(r.get("Configuration Group"), "General PM");
            Map<String,Object> flow = flowsByGroup.computeIfAbsent(cfg, k -> baseFlow(k, nvl(r.get("Priority"), "Medium")));
            String alert = nvl(r.get("Alarm Name"), null);
            if (alert != null) addToSetList(flow, "alarmsAlerts", alert);
            addInterfaces(flow, r.get("Device - A"));
            addParam(flow, 0, "ResponseOptions", r.get("Response Options"));
            // recipients
            addRecipients(flow, null, r.get("1st Recipient"), 1);
            addRecipients(flow, null, r.get("2nd Recipient"), 2);
        }

        // units scoping
        Map<String, List<Map<String,String>>> unitsByGroup = indexUnitsByGroup(unitRows);
        for (var entry : flowsByGroup.entrySet()) {
            String group = entry.getKey();
            Map<String,Object> flow = entry.getValue();
            List<Map<String,String>> unitRefs = unitsByGroup.getOrDefault(group, Collections.emptyList());
            if (unitRefs.isEmpty() && config.attachAllUnitsIfNoMatch) {
                // fallback attach all
                unitRefs = allUnits(unitRows);
            }
            if (!unitRefs.isEmpty()) flow.put("units", unitRefs);
            flow.putIfAbsent("status", "Enabled");
        }

        root.put("deliveryFlows", new ArrayList<>(flowsByGroup.values()));
        return root;
    }

    private static Map<String,Object> baseFlow(String name, String priority) {
        Map<String,Object> flow = new LinkedHashMap<>();
        flow.put("name", name);
        flow.put("priority", priority);
        flow.put("status", "Enabled");
        flow.put("alarmsAlerts", new ArrayList<>());
        flow.put("conditions", new ArrayList<>());
        flow.put("destinations", new ArrayList<>());
        flow.put("interfaces", new ArrayList<>());
        flow.put("parameterAttributes", new ArrayList<>());
        flow.put("units", new ArrayList<>());
        return flow;
    }

    private void addInterfaces(Map<String,Object> flow, String deviceA) {
        if (deviceA == null || deviceA.isBlank()) return;
        List<Map<String,String>> ifaces = (List<Map<String,String>>) flow.get("interfaces");
        ifaces.add(Map.of("componentName", deviceA, "referenceName", deviceA));
    }

    private void addParam(Map<String,Object> flow, int order, String name, String value) {
        if (value == null || value.isBlank()) return;
        List<Map<String,Object>> params = (List<Map<String,Object>>) flow.get("parameterAttributes");
        Map<String,Object> p = new LinkedHashMap<>();
        p.put("destinationOrder", order);
        p.put("name", name);
        p.put("value", value);
        params.add(p);
    }

    private void addRecipients(Map<String,Object> flow, String delayStr, String recipients, int order) {
        if ((recipients == null || recipients.isBlank()) && (delayStr == null || delayStr.isBlank())) return;
        List<Map<String,Object>> dests = (List<Map<String,Object>>) flow.get("destinations");
        // parse recipients (split by comma/semicolon/newline)
        List<String> items = splitList(recipients);
        List<String> groups = new ArrayList<>();
        List<String> roles = new ArrayList<>();
        for (String item : items) {
            String trimmed = item.trim();
            if (trimmed.isEmpty()) continue;
            if (trimmed.matches(config.recipientParsing.functionalRoleRegex)) {
                roles.add(trimmed);
            } else {
                groups.add(trimmed);
            }
        }
        Map<String,Object> d = new LinkedHashMap<>();
        d.put("order", order);
        d.put("delayTime", parseIntSafe(delayStr));
        d.put("recipientType", roles.isEmpty() ? "Groups" : "Mixed");
        d.put("destinationType", "Mobile");
        d.put("users", new ArrayList<>());
        d.put("groups", groups);
        d.put("functionalRoles", roles);
        d.put("presenceConfig", "");
        dests.add(d);
    }

    private static List<String> splitList(String s) {
        if (s == null) return List.of();
        return Arrays.stream(s.split("[;,
]"))
                .map(String::trim)
                .filter(x -> !x.isEmpty())
                .collect(Collectors.toList());
    }

    private Map<String, List<Map<String,String>>> indexUnitsByGroup(List<Map<String,String>> units) {
        Map<String, List<Map<String,String>>> map = new LinkedHashMap<>();
        for (var r : units) {
            String facility = nvl(r.get("Facility"), null);
            String name = nvl(r.get("Common Unit Name"), null);
            String groups = nvl(r.get("Groups"), "");
            if (facility == null || name == null) continue;
            List<String> groupList = splitList(groups);
            if (groupList.isEmpty()) continue;
            Map<String,String> unitRef = Map.of("facilityName", facility, "name", name);
            for (String g : groupList) {
                map.computeIfAbsent(g, k -> new ArrayList<>()).add(unitRef);
            }
        }
        return map;
    }

    private static List<Map<String,String>> allUnits(List<Map<String,String>> unitsRaw) {
        List<Map<String,String>> out = new ArrayList<>();
        for (var r : unitsRaw) {
            String facility = nvl(r.get("Facility"), null);
            String name = nvl(r.get("Common Unit Name"), null);
            if (facility != null && name != null) out.add(Map.of("facilityName", facility, "name", name));
        }
        return out;
    }

    private static Integer parseIntSafe(String s) {
        try {
            if (s == null) return 0;
            s = s.trim();
            if (s.isEmpty()) return 0;
            String digits = s.replaceAll("[^0-9]", "");
            if (digits.isEmpty()) return 0;
            return Integer.parseInt(digits);
        } catch (Exception e) { return 0; }
    }

    private static String nvl(String s, String d) { return (s == null || s.isBlank()) ? d : s; }
    private static String coalesce(String... vals) {
        for (String v : vals) if (v != null && !v.isBlank()) return v; return null;
    }
}
```java
package com.example.exceljson;

import com.example.exceljson.model.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.util.*;
import java.util.stream.Collectors;

import static com.example.exceljson.HeaderFinder.*;

public class ExcelParser {
    private Workbook wb;

    // Raw rows as maps (for preview tables)
    private List<Map<String,String>> unitRows = new ArrayList<>();
    private List<Map<String,String>> nurseCallRows = new ArrayList<>();
    private List<Map<String,String>> patientMonRows = new ArrayList<>();

    public void load(File excel) throws Exception {
        try (FileInputStream fis = new FileInputStream(excel)) {
            wb = new XSSFWorkbook(fis);
        }
        parseUnitBreakdown();
        parseNurseCall();
        parsePatientMonitoring();
    }

    public List<Map<String,String>> getUnitBreakdownRows(){ return unitRows; }
    public List<Map<String,String>> getNurseCallRows(){ return nurseCallRows; }
    public List<Map<String,String>> getPatientMonitoringRows(){ return patientMonRows; }

    // ---- Parsing helpers ----
    private static Map<String,Integer> headerIndex(Row header) {
        Map<String,Integer> map = new LinkedHashMap<>();
        for (int c = 0; c < header.getLastCellNum(); c++) {
            String key = normalize(getString(header, c));
            if (!key.isBlank()) map.put(key, c);
        }
        return map;
    }
    private static String get(Row row, Map<String,Integer> hmap, Set<String> aliases){
        for (String a : aliases) {
            Integer idx = hmap.get(a);
            if (idx != null) return HeaderFinder.getString(row, idx).trim();
        }
        return "";
    }

    private void parseUnitBreakdown() {
        Sheet sheet = wb.getSheet("Unit Breakdown");
        if (sheet == null) return;
        Set<String> expected = new HashSet<>();
        expected.addAll(MappingAliases.FACILITY);
        expected.addAll(MappingAliases.UNIT_NAME);

        int headerRowIdx = HeaderFinder.findHeaderRow(sheet, expected, 20);
        if (headerRowIdx < 0) return;
        Row header = sheet.getRow(headerRowIdx);
        Map<String,Integer> hmap = headerIndex(header);

        for (int r = headerRowIdx + 1; r <= sheet.getLastRowNum(); r++) {
            Row row = sheet.getRow(r);
            if (row == null) continue;
            String facility = get(row, hmap, MappingAliases.FACILITY);
            String unit = get(row, hmap, MappingAliases.UNIT_NAME);
            if (facility.isBlank() && unit.isBlank()) continue;

            Map<String,String> map = new LinkedHashMap<>();
            map.put("Facility", facility);
            map.put("Common Unit Name", unit);
            unitRows.add(map);
        }
    }

    private void parseNurseCall() {
        Sheet sheet = wb.getSheet("Nurse Call");
        if (sheet == null) return;
        Set<String> expected = new HashSet<>() {{
            addAll(MappingAliases.CFG_GROUP);
            addAll(MappingAliases.ALERT_NAME_COMMON);
            addAll(MappingAliases.SENDING_NAME);
            addAll(MappingAliases.PRIORITY);
        }};
        int headerRowIdx = HeaderFinder.findHeaderRow(sheet, expected, 30);
        if (headerRowIdx < 0) return;
        Row header = sheet.getRow(headerRowIdx);
        Map<String,Integer> hmap = headerIndex(header);

        for (int r = headerRowIdx + 1; r <= sheet.getLastRowNum(); r++) {
            Row row = sheet.getRow(r);
            if (row == null) continue;
            String cfg = get(row, hmap, MappingAliases.CFG_GROUP);
            String alert = get(row, hmap, MappingAliases.ALERT_NAME_COMMON);
            String send = get(row, hmap, MappingAliases.SENDING_NAME);
            if (cfg.isBlank() && alert.isBlank() && send.isBlank()) continue;

            Map<String,String> m = new LinkedHashMap<>();
            m.put("Configuration Group", cfg);
            m.put("Common Alert or Alarm Name", alert);
            m.put("Sending System Alert Name", send);
            m.put("Priority", get(row, hmap, MappingAliases.PRIORITY));
            m.put("Device - A", get(row, hmap, MappingAliases.DEVICE_A));
            m.put("Ringtone Device - A", get(row, hmap, MappingAliases.RINGTONE_A));
            m.put("Time to 1st Recipient", get(row, hmap, MappingAliases.T1));
            m.put("1st Recipient", get(row, hmap, MappingAliases.R1));
            m.put("Time to 2nd Recipient", get(row, hmap, MappingAliases.T2));
            m.put("2nd Recipient", get(row, hmap, MappingAliases.R2));
            m.put("Response Options", get(row, hmap, MappingAliases.RESPONSE));
            m.put("EMDAN Compliant?", get(row, hmap, MappingAliases.EMDAN));
            m.put("Comments", get(row, hmap, MappingAliases.COMMENTS));
            nurseCallRows.add(m);
        }
    }

    private void parsePatientMonitoring() {
        Sheet sheet = wb.getSheet("Patient Monitoring");
        if (sheet == null) return;
        Set<String> expected = new HashSet<>() {{
            addAll(MappingAliases.CFG_GROUP);
            addAll(MappingAliases.ALERT_NAME_COMMON); // covers "Alarm Name"
            addAll(MappingAliases.SENDING_NAME);
            addAll(MappingAliases.PRIORITY);
        }};
        int headerRowIdx = HeaderFinder.findHeaderRow(sheet, expected, 30);
        if (headerRowIdx < 0) return;
        Row header = sheet.getRow(headerRowIdx);
        Map<String,Integer> hmap = headerIndex(header);

        for (int r = headerRowIdx + 1; r <= sheet.getLastRowNum(); r++) {
            Row row = sheet.getRow(r);
            if (row == null) continue;
            String cfg = get(row, hmap, MappingAliases.CFG_GROUP);
            String alert = get(row, hmap, MappingAliases.ALERT_NAME_COMMON); // "Alarm Name"
            String send = get(row, hmap, MappingAliases.SENDING_NAME);
            if (cfg.isBlank() && alert.isBlank() && send.isBlank()) continue;

            Map<String,String> m = new LinkedHashMap<>();
            m.put("Configuration Group", cfg);
            m.put("Alarm Name", alert);
            m.put("Sending System Alarm Name", send);
            m.put("Priority", get(row, hmap, MappingAliases.PRIORITY));
            m.put("Device - A", get(row, hmap, MappingAliases.DEVICE_A));
            m.put("1st Recipient", get(row, hmap, MappingAliases.R1));
            m.put("2nd Recipient", get(row, hmap, MappingAliases.R2));
            m.put("Response Options", get(row, hmap, MappingAliases.RESPONSE));
            patientMonRows.add(m);
        }
    }

    // ---- Build JSON in desired structure ----
    public Map<String, Object> buildJson() {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("version", "1.0");

        // alarmAlertDefinitions (from both NC + PM)
        List<Map<String, Object>> definitions = new ArrayList<>();

        for (var r : nurseCallRows) {
            String name = coalesce(r.get("Common Alert or Alarm Name"), r.get("Alarm Name"));
            String send = coalesce(r.get("Sending System Alert Name"), r.get("Sending System Alarm Name"));
            if (name == null || name.isBlank()) continue;
            Map<String,Object> def = new LinkedHashMap<>();
            def.put("name", name);
            def.put("type", "NurseCall");
            List<Map<String,String>> values = new ArrayList<>();
            if (send != null && !send.isBlank()) {
                values.add(Map.of("category","SendingSystem","value", send));
            }
            def.put("values", values);
            definitions.add(def);
        }
        for (var r : patientMonRows) {
            String name = coalesce(r.get("Alarm Name"), r.get("Common Alert or Alarm Name"));
            String send = coalesce(r.get("Sending System Alarm Name"), r.get("Sending System Alert Name"));
            if (name == null || name.isBlank()) continue;
            Map<String,Object> def = new LinkedHashMap<>();
            def.put("name", name);
            def.put("type", "PatientMonitoring");
            List<Map<String,String>> values = new ArrayList<>();
            if (send != null && !send.isBlank()) {
                values.add(Map.of("category","SendingSystem","value", send));
            }
            def.put("values", values);
            definitions.add(def);
        }
        // de-duplicate by name+type
        definitions = definitions.stream()
                .collect(Collectors.toMap(
                        m -> (m.get("name")+"|"+m.get("type")),
                        m -> m,
                        (a,b)->a,
                        LinkedHashMap::new
                ))
                .values().stream().collect(Collectors.toList());
        root.put("alarmAlertDefinitions", definitions);

        // deliveryFlows grouped by Configuration Group
        Map<String, Map<String,Object>> flowsByGroup = new LinkedHashMap<>();

        // helper to add destination
        java.util.function.BiConsumer<Map<String,Object>, Map<String,String>> addDestinations = (flow, row) -> {
            List<Map<String,Object>> dests = (List<Map<String,Object>>) flow.get("destinations");
            if (dests == null) { dests = new ArrayList<>(); flow.put("destinations", dests); }

            addDest(dests, 1, row.get("Time to 1st Recipient"), row.get("1st Recipient"));
            addDest(dests, 2, row.get("Time to 2nd Recipient"), row.get("2nd Recipient"));
        };

        // Nurse Call rows
        for (var r : nurseCallRows) {
            String cfg = nvl(r.get("Configuration Group"), "General NC");
            Map<String,Object> flow = flowsByGroup.computeIfAbsent(cfg, k -> baseFlow(k, nvl(r.get("Priority"), "Medium")));
            // alarmsAlerts link
            String alert = nvl(r.get("Common Alert or Alarm Name"), null);
            if (alert != null) addToSetList(flow, "alarmsAlerts", alert);
            // interfaces and params
            addInterfaces(flow, r.get("Device - A"));
            addParam(flow, 0, "RingtoneDeviceA", r.get("Ringtone Device - A"));
            addParam(flow, 0, "ResponseOptions", r.get("Response Options"));
            addParam(flow, 0, "EMDAN", r.get("EMDAN Compliant?"));
            addParam(flow, 0, "Comments", r.get("Comments"));
            // recipients
            addDestinations.accept(flow, r);
        }
        // Patient Monitoring rows
        for (var r : patientMonRows) {
            String cfg = nvl(r.get("Configuration Group"), "General PM");
            Map<String,Object> flow = flowsByGroup.computeIfAbsent(cfg, k -> baseFlow(k, nvl(r.get("Priority"), "Medium")));
            String alert = nvl(r.get("Alarm Name"), null);
            if (alert != null) addToSetList(flow, "alarmsAlerts", alert);
            addInterfaces(flow, r.get("Device - A"));
            addParam(flow, 0, "ResponseOptions", r.get("Response Options"));
            // recipients
            List<Map<String,Object>> dests = (List<Map<String,Object>>) flow.get("destinations");
            if (dests == null) { dests = new ArrayList<>(); flow.put("destinations", dests); }
            addDest(dests, 1, null, r.get("1st Recipient"));
            addDest(dests, 2, null, r.get("2nd Recipient"));
        }

        // units (from Unit Breakdown)
        List<Map<String,String>> unitsRaw = unitRows;
        List<Map<String,String>> unitRefs = new ArrayList<>();
        for (var r : unitsRaw) {
            String facility = nvl(r.get("Facility"), null);
            String name = nvl(r.get("Common Unit Name"), null);
            if (facility != null && name != null) {
                unitRefs.add(Map.of("facilityName", facility, "name", name));
            }
        }
        for (var f : flowsByGroup.values()) {
            if (!unitRefs.isEmpty()) f.put("units", unitRefs);
            f.putIfAbsent("status", "Enabled");
        }

        root.put("deliveryFlows", new ArrayList<>(flowsByGroup.values()));
        return root;
    }

    private static Map<String,Object> baseFlow(String name, String priority) {
        Map<String,Object> flow = new LinkedHashMap<>();
        flow.put("name", name);
        flow.put("priority", priority);
        flow.put("status", "Enabled");
        flow.put("alarmsAlerts", new ArrayList<>());
        flow.put("conditions", new ArrayList<>());
        flow.put("destinations", new ArrayList<>());
        flow.put("interfaces", new ArrayList<>());
        flow.put("parameterAttributes", new ArrayList<>());
        flow.put("units", new ArrayList<>());
        return flow;
    }

    private static void addToSetList(Map<String,Object> obj, String key, String value) {
        List<String> list = (List<String>) obj.computeIfAbsent(key, k -> new ArrayList<String>());
        if (!list.contains(value)) list.add(value);
    }

    private static void addInterfaces(Map<String,Object> flow, String deviceA) {
        if (deviceA == null || deviceA.isBlank()) return;
        List<Map<String,String>> ifaces = (List<Map<String,String>>) flow.get("interfaces");
        ifaces.add(Map.of("componentName", deviceA, "referenceName", deviceA));
    }

    private static void addParam(Map<String,Object> flow, int order, String name, String value) {
        if (value == null || value.isBlank()) return;
        List<Map<String,Object>> params = (List<Map<String,Object>>) flow.get("parameterAttributes");
        Map<String,Object> p = new LinkedHashMap<>();
        p.put("destinationOrder", order);
        p.put("name", name);
        p.put("value", value);
        params.add(p);
    }

    private static void addDest(List<Map<String,Object>> dests, int order, String delayStr, String recipient) {
        if ((recipient == null || recipient.isBlank()) && (delayStr == null || delayStr.isBlank())) return;
        Map<String,Object> d = new LinkedHashMap<>();
        d.put("order", order);
        d.put("delayTime", parseIntSafe(delayStr));
        d.put("recipientType", "Groups");
        d.put("destinationType", "Mobile");
        d.put("users", new ArrayList<>());
        d.put("groups", recipient == null ? new ArrayList<>() : List.of(recipient));
        d.put("functionalRoles", new ArrayList<>());
        d.put("presenceConfig", "");
        dests.add(d);
    }

    private static Integer parseIntSafe(String s) {
        try {
            if (s == null) return 0;
            s = s.trim();
            if (s.isEmpty()) return 0;
            // accept values like "10", "10 min", "10 minutes"
            String digits = s.replaceAll("[^0-9]", "");
            if (digits.isEmpty()) return 0;
            return Integer.parseInt(digits);
        } catch (Exception e) { return 0; }
    }

    private static String nvl(String s, String d) { return (s == null || s.isBlank()) ? d : s; }
    private static String coalesce(String... vals) {
        for (String v : vals) if (v != null && !v.isBlank()) return v; return null;
    }
}
