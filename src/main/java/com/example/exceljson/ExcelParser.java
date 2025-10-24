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

    // ---------------------- PARSING ----------------------
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
            String alert = get(row, hmap, config.aliases.ALERT_NAME_COMMON);
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

    // ---------------------- BUILD JSON ----------------------
    public Map<String, Object> buildJson() {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("version", "1.1.0");

        // Alarm Definitions
        List<Map<String,Object>> definitions = new ArrayList<>();
        for (var r : nurseCallRows) {
            String name = coalesce(r.get("Common Alert or Alarm Name"), r.get("Alarm Name"));
            if (name == null || name.isBlank()) continue;
            Map<String,Object> def = new LinkedHashMap<>();
            def.put("name", name);
            def.put("type", "NurseCalls");
            definitions.add(def);
        }
        for (var r : patientMonRows) {
            String name = coalesce(r.get("Alarm Name"), r.get("Common Alert or Alarm Name"));
            if (name == null || name.isBlank()) continue;
            Map<String,Object> def = new LinkedHashMap<>();
            def.put("name", name);
            def.put("type", "Clinicals");
            definitions.add(def);
        }
        root.put("alarmAlertDefinitions", definitions);

        // Group identical rows
        List<String> groupKeys = List.of("Configuration Group","Priority","Device - A",
                "1st Recipient","2nd Recipient","Response Options");
        Map<String,List<Map<String,String>>> groupedNC = groupRowsForFlows(nurseCallRows, groupKeys);
        List<Map<String,Object>> flows = new ArrayList<>();

        for (var entry : groupedNC.entrySet()) {
            Map<String,String> sample = entry.getValue().get(0);
            Map<String,Object> flow = baseFlow("temp", nvl(sample.get("Priority"),"Medium"));
            for (var r : entry.getValue()) addToSetList(flow,"alarmsAlerts",r.get("Common Alert or Alarm Name"));

            addInterfaces(flow, sample.get("Device - A"));
            addRecipients(flow, sample.get("Time to 1st Recipient"), sample.get("1st Recipient"), 1);
            addRecipients(flow, sample.get("Time to 2nd Recipient"), sample.get("2nd Recipient"), 2);

            // Build flow name
            String type = "NURSECALL";
            String alarms = String.join(" | ", (List<String>)flow.get("alarmsAlerts"));
            String cfg = nvl(sample.get("Configuration Group"), "");
            String unit = unitRows.isEmpty() ? "" : unitRows.get(0).getOrDefault("Common Unit Name","");
            String fac = unitRows.isEmpty() ? "" : unitRows.get(0).getOrDefault("Facility","");
            String name = String.format("SEND %s | %s | %s | %s | %s", type, alarms, cfg, unit, fac).trim();
            flow.put("name", name);
            flow.put("status", "Active");

            // Response params
            List<Map<String,Object>> dests = (List<Map<String,Object>>)flow.get("destinations");
            if (!dests.isEmpty()) {
                List<Map<String,Object>> roles = (List<Map<String,Object>>)dests.get(0).get("functionalRoles");
                addResponseParams(flow, sample.get("Response Options"), roles);
            }

            flows.add(flow);
        }

        root.put("deliveryFlows", flows);
        return root;
    }

    // ---------------------- HELPERS ----------------------
    private Map<String,List<Map<String,String>>> groupRowsForFlows(List<Map<String,String>> rows, List<String> keys){
        Map<String,List<Map<String,String>>> out=new LinkedHashMap<>();
        for(Map<String,String>r:rows){
            String k=keys.stream().map(x->nvl(r.get(x),"")).collect(Collectors.joining("|")).toLowerCase();
            out.computeIfAbsent(k,v->new ArrayList<>()).add(r);
        }
        return out;
    }

    private static Map<String,Object> baseFlow(String n,String p){
        Map<String,Object>f=new LinkedHashMap<>();
        f.put("name",n);f.put("priority",p);f.put("status","Enabled");
        f.put("alarmsAlerts",new ArrayList<>());f.put("conditions",new ArrayList<>());
        f.put("destinations",new ArrayList<>());f.put("interfaces",new ArrayList<>());
        f.put("parameterAttributes",new ArrayList<>());f.put("units",new ArrayList<>());
        return f;
    }

    @SuppressWarnings("unchecked")
    private void addInterfaces(Map<String,Object>flow,String dev){
        if(dev==null||dev.isBlank())return;
        List<Map<String,String>>i=(List<Map<String,String>>)flow.get("interfaces");
        if(dev.toLowerCase().contains("edge"))
            i.add(Map.of("componentName","OutgoingWCTP","referenceName","OutgoingWCTP"));
        else i.add(Map.of("componentName",dev,"referenceName",dev));
    }

    @SuppressWarnings("unchecked")
    private void addRecipients(Map<String,Object>flow,String delay,String rec,int order){
        if((rec==null||rec.isBlank())&&(delay==null||delay.isBlank()))return;
        List<Map<String,Object>>dests=(List<Map<String,Object>>)flow.get("destinations");
        List<String>items=splitList(rec);
        List<Map<String,Object>>roles=new ArrayList<>();
        List<Map<String,Object>>groups=new ArrayList<>();

        for(String item:items){
            String t=item.trim();
            if(t.isEmpty())continue;
            if(t.toLowerCase().startsWith("vassign")){
                String name=t.replaceFirst("(?i)vassign:\\s*\\[room\\]\\s*","").trim();
                String fac=unitRows.isEmpty()?"":unitRows.get(0).getOrDefault("Facility","");
                roles.add(Map.of("facilityName",fac,"name",name));
            }else if(t.toLowerCase().startsWith("vgroup")){
                String name=t.replaceFirst("(?i)vgroup:\\s*","").trim();
                String fac=unitRows.isEmpty()?"Global":unitRows.get(0).getOrDefault("Facility","Global");
                groups.add(Map.of("facilityName",fac,"name",name));
            }else{
                String fac=unitRows.isEmpty()?"Global":unitRows.get(0).getOrDefault("Facility","Global");
                groups.add(Map.of("facilityName",fac,"name",t));
            }
        }

        Map<String,Object>d=new LinkedHashMap<>();
        d.put("order",order);d.put("delayTime",parseIntSafe(delay));
        d.put("destinationType","Normal");d.put("users",new ArrayList<>());
        d.put("groups",groups);d.put("functionalRoles",roles);
        if(!roles.isEmpty()){d.put("presenceConfig","user_and_device");d.put("recipientType","functional_role");}
        else{d.put("presenceConfig","device");d.put("recipientType","group");}
        dests.add(d);
    }

    @SuppressWarnings("unchecked")
    private void addResponseParams(Map<String,Object>flow,String resp,List<Map<String,Object>>roles){
        if(resp==null||resp.isBlank())return;
        List<Map<String,Object>>p=(List<Map<String,Object>>)flow.get("parameterAttributes");
        String r=resp.toLowerCase();

        p.add(Map.of("name","popup","value","true"));
        p.add(Map.of("name","alertSound","value","\"Vocera Tone 3 Double\""));
        p.add(Map.of("name","breakThrough","value","\"none\""));
        p.add(Map.of("name","vibrate","value","\"short\""));

        if(r.contains("accept")){
            p.add(Map.of("name","accept","value","\"Accepted\""));
            p.add(Map.of("name","acceptBadgePhrases","value","[\"Accept\"]"));
        }
        if(r.contains("call back")){
            p.add(Map.of("name","acceptAndCall","value","\"Call Back\""));
        }
        if(r.contains("escalate")){
            p.add(Map.of("name","decline","value","\"Decline Primary\""));
            p.add(Map.of("name","declineBadgePhrases","value","[\"Escalate\"]"));
        }
        int ord=0;
        for(Map<String,Object>ro:roles){
            String dest=(String)ro.getOrDefault("name","");
            p.add(Map.of("destinationOrder",ord++,"name","destinationName","value","\""+dest+"\""));
        }
        p.add(Map.of("name","responseType","value","\"Accept/Decline\""));
        p.add(Map.of("name","ttl","value","10"));
        p.add(Map.of("name","retractRules","value","[\"ttlHasElapsed\"]"));
        p.add(Map.of("name","enunciate","value","true"));
    }

    private static List<String> splitList(String s){
        if(s==null)return List.of();
        return Arrays.stream(s.split("[;,\\n]")).map(String::trim)
                .filter(x->!x.isEmpty()).collect(Collectors.toList());
    }

    private static Integer parseIntSafe(String s){
        try{
            if(s==null||s.isBlank())return 0;
            String d=s.replaceAll("[^0-9]","");
            return d.isEmpty()?0:Integer.parseInt(d);
        }catch(Exception e){return 0;}
    }

    private static String nvl(String s,String d){return(s==null||s.isBlank())?d:s;}
    private static String coalesce(String...v){for(String x:v)if(x!=null&&!x.isBlank())return x;return null;}
    @SuppressWarnings("unchecked")
    private static void addToSetList(Map<String,Object>m,String k,String v){
        if(v==null||v.isBlank())return;
        List<Object>l=(List<Object>)m.computeIfAbsent(k,x->new ArrayList<>());
        if(!l.contains(v))l.add(v);
    }
}
