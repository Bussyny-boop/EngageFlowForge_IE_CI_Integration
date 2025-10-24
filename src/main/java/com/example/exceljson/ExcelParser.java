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

    private final List<Map<String,String>> unitRows = new ArrayList<>();
    private final List<Map<String,String>> nurseCallRows = new ArrayList<>();
    private final List<Map<String,String>> patientMonRows = new ArrayList<>();

    public ExcelParser(Config config) { this.config = config; }

    // -------------------- LOAD & PARSE --------------------
    public void load(File excel) throws Exception {
        try (FileInputStream fis = new FileInputStream(excel)) {
            wb = new XSSFWorkbook(fis);
        }
        parseUnitBreakdown();
        parseNurseCall();
        parsePatientMonitoring();
    }

    // Public getters for AppController preview
    public List<Map<String,String>> getUnitBreakdownRows(){ return unitRows; }
    public List<Map<String,String>> getNurseCallRows(){ return nurseCallRows; }
    public List<Map<String,String>> getPatientMonitoringRows(){ return patientMonRows; }

    // -------------------- SHEET PARSERS --------------------
    private Map<String,Integer> headerIndex(Row header) {
        Map<String,Integer> map = new LinkedHashMap<>();
        for (int c = 0; c < header.getLastCellNum(); c++) {
            String key = normalize(getString(header, c));
            if (!key.isBlank()) map.put(key, c);
        }
        return map;
    }

    private void parseUnitBreakdown() {
        Sheet sheet = wb.getSheet(config.sheets.unitBreakdown);
        if (sheet == null) return;
        int headerRowIdx = HeaderFinder.findHeaderRow(sheet,
                new HashSet<>(Arrays.asList("facility","common unit name","groups")), 30);
        if (headerRowIdx < 0) return;

        Row header = sheet.getRow(headerRowIdx);
        Map<String,Integer> hmap = headerIndex(header);

        for (int r = headerRowIdx + 1; r <= sheet.getLastRowNum(); r++) {
            Row row = sheet.getRow(r);
            if (row == null) continue;
            String facility = getString(row, hmap.getOrDefault("facility",-1));
            String unit = getString(row, hmap.getOrDefault("common unit name",-1));
            String groups = getString(row, hmap.getOrDefault("groups",-1));
            if (facility.isBlank() && unit.isBlank() && groups.isBlank()) continue;
            Map<String,String> map = new LinkedHashMap<>();
            map.put("Facility", facility);
            map.put("Common Unit Name", unit);
            map.put("Groups", groups);
            unitRows.add(map);
        }
    }

    private void parseNurseCall() { parseAlertSheet(wb.getSheet(config.sheets.nurseCall), nurseCallRows); }
    private void parsePatientMonitoring() { parseAlertSheet(wb.getSheet(config.sheets.patientMonitoring), patientMonRows); }

    /** shared parser for both alert sheets */
    private void parseAlertSheet(Sheet sheet, List<Map<String,String>> out){
        if (sheet == null) return;
        int headerRowIdx = HeaderFinder.findHeaderRow(sheet,
                new HashSet<>(Arrays.asList("configuration group","priority","device a","response options")), 40);
        if (headerRowIdx < 0) return;
        Row header = sheet.getRow(headerRowIdx);
        Map<String,Integer> hmap = headerIndex(header);

        for (int r = headerRowIdx + 1; r <= sheet.getLastRowNum(); r++) {
            Row row = sheet.getRow(r);
            if (row == null) continue;
            String cfg = getString(row, hmap.getOrDefault("configuration group",-1));
            String alert = getString(row, hmap.getOrDefault("common alert or alarm name",-1));
            if (cfg.isBlank() && alert.isBlank()) continue;

            Map<String,String> m = new LinkedHashMap<>();
            m.put("Configuration Group", cfg);
            m.put("Common Alert or Alarm Name", alert);
            m.put("Priority", mapPriority(getString(row, hmap.getOrDefault("priority",-1))));
            m.put("Device - A", getString(row, hmap.getOrDefault("device a",-1)));
            m.put("Ringtone Device - A", getString(row, hmap.getOrDefault("ringtone device a",-1)));
            m.put("Response Options", getString(row, hmap.getOrDefault("response options",-1)));

            // up to 5 recipients
            for (int i = 1; i <= 5; i++) {
                m.put("Time to " + i + getOrdinal(i) + " Recipient",
                        getString(row, hmap.getOrDefault(("time to " + i + getOrdinal(i) + " recipient").toLowerCase(), -1)));
                m.put(i + getOrdinal(i) + " Recipient",
                        getString(row, hmap.getOrDefault((i + getOrdinal(i) + " recipient").toLowerCase(), -1)));
            }
            out.add(m);
        }
    }

    private static String getOrdinal(int n){
        switch(n){case 1:return "st";case 2:return "nd";case 3:return "rd";default:return "th";}
    }

    // -------------------- JSON BUILDER --------------------
    public Map<String,Object> buildJson(){
        Map<String,Object> root = new LinkedHashMap<>();
        root.put("version","1.1.0");

        List<Map<String,Object>> flows = new ArrayList<>();
        flows.addAll(buildFlowsForType("NURSECALL", nurseCallRows));
        flows.addAll(buildFlowsForType("CLINICAL", patientMonRows));

        root.put("deliveryFlows", flows);
        return root;
    }

    private List<Map<String,Object>> buildFlowsForType(String type, List<Map<String,String>> rows){
        List<String> groupKeys = List.of("Configuration Group","Priority","Device - A","Response Options");
        Map<String,List<Map<String,String>>> grouped = groupRowsForFlows(rows, groupKeys);
        List<Map<String,Object>> out = new ArrayList<>();

        for (var entry : grouped.entrySet()){
            Map<String,String> sample = entry.getValue().get(0);
            String cfg = nvl(sample.get("Configuration Group"),"General");
            String priority = nvl(sample.get("Priority"),"normal");
            String ringtone = nvl(sample.get("Ringtone Device - A"),"");

            Map<String,Object> flow = baseFlow(cfg, priority);

            // merge alarms
            for (var r : entry.getValue()){
                String alert = nvl(r.get("Common Alert or Alarm Name"),"");
                addToSetList(flow,"alarmsAlerts",alert);
            }

            addInterfaces(flow, sample.get("Device - A"));
            addRecipients(flow, sample); // up to 5 recipients

            // Build flow name
            String alarms = String.join(" | ", (List<String>)flow.get("alarmsAlerts"));
            String unitsConcat = collectUnitsForCfg(cfg);
            String facility = findFacilityForCfg(cfg);
            String name = String.format("SEND %s | %s | %s | %s | %s | %s",
                    type, priority.toUpperCase(), alarms, cfg, unitsConcat, facility);
            flow.put("name", name);
            flow.put("status","Active");

            // Add type-specific parameters
            if(type.equals("NURSECALL"))
                addNurseCallParams(flow, sample, ringtone, priority);
            else
                addClinicalParams(flow, sample, ringtone, priority);

            // Attach all units for this configuration group
            flow.put("units", findUnits(cfg));
            out.add(flow);
        }
        return out;
    }

    // -------------------- RECIPIENTS --------------------
    /** Handles up to 5 recipient levels (1–5) */
    @SuppressWarnings("unchecked")
    private void addRecipients(Map<String,Object> flow, Map<String,String> row){
        List<Map<String,Object>> dests = (List<Map<String,Object>>) flow.get("destinations");
        String cfg = row.getOrDefault("Configuration Group", "");

        for (int i = 1; i <= 5; i++){
            String delay = row.getOrDefault("Time to " + i + getOrdinal(i) + " Recipient", "");
            String rec   = row.getOrDefault(i + getOrdinal(i) + " Recipient", "");
            if ((rec == null || rec.isBlank()) && (delay == null || delay.isBlank())) continue;

            List<Map<String,Object>> roles = new ArrayList<>();
            List<Map<String,Object>> groups = new ArrayList<>();
            String facility = findFacilityForCfg(cfg);

            for (String token : splitList(rec)){
                String t = token.trim();
                if (t.isEmpty()) continue;
                if (t.toLowerCase().startsWith("vassign")){
                    String name = t.replaceFirst("(?i)vassign:\\s*\\[room\\]\\s*", "").trim();
                    roles.add(Map.of("facilityName", facility, "name", name));
                } else if (t.toLowerCase().startsWith("vgroup")){
                    String name = t.replaceFirst("(?i)vgroup:\\s*", "").trim();
                    groups.add(Map.of("facilityName", facility, "name", name));
                } else {
                    groups.add(Map.of("facilityName", facility, "name", t));
                }
            }

            Map<String,Object> dest = new LinkedHashMap<>();
            dest.put("order", i);
            dest.put("delayTime", parseIntSafe(delay));
            dest.put("destinationType", "Normal");
            dest.put("users", new ArrayList<>());
            dest.put("groups", groups);
            dest.put("functionalRoles", roles);

            if (!roles.isEmpty()){
                dest.put("presenceConfig","user_and_device");
                dest.put("recipientType","functional_role");
            } else {
                dest.put("presenceConfig","device");
                dest.put("recipientType","group");
            }
            dests.add(dest);
        }
    }

    // -------------------- PARAMETER BLOCKS --------------------
    @SuppressWarnings("unchecked")
    private void addNurseCallParams(Map<String,Object> flow, Map<String,String> row,
                                    String ringtone, String priority){
        List<Map<String,Object>> p = (List<Map<String,Object>>) flow.get("parameterAttributes");
        String resp = nvl(row.get("Response Options"),"").toLowerCase();

        p.add(Map.of("name","destinationName","value","\"Group\""));
        if(!ringtone.isBlank()) p.add(Map.of("name","alertSound","value","\"" + ringtone + "\""));
        p.add(Map.of("name","popup","value","true"));
        p.add(Map.of("name","breakThrough","value",
                priority.equalsIgnoreCase("urgent") ? "\"voceraAndDevice\"" : "\"none\""));
        p.add(Map.of("name","enunciate","value","true"));

        if(resp.contains("accept")) {
            p.add(Map.of("name","accept","value","\"Accepted\""));
            p.add(Map.of("name","acceptBadgePhrases","value","[\"Accept\"]"));
        }
        if(resp.contains("call back"))
            p.add(Map.of("name","acceptAndCall","value","\"Call Back\""));
        if(resp.contains("escalate")) {
            p.add(Map.of("name","decline","value","\"Decline Primary\""));
            p.add(Map.of("name","declineBadgePhrases","value","[\"Escalate\"]"));
        }

        p.add(Map.of("name","message","value",
                "\"Patient: #{bed.patient.last_name}, #{bed.patient.first_name}\\nRoom/Bed: #{bed.room.name} - #{bed.bed_number}\""));
        p.add(Map.of("name","patientMRN","value","\"#{bed.patient.mrn}:#{bed.patient.visit_number}\""));
        p.add(Map.of("name","placeUid","value","\"#{bed.uid}\""));
        p.add(Map.of("name","patientName","value","\"#{bed.patient.first_name} #{bed.patient.middle_name} #{bed.patient.last_name}\""));
        p.add(Map.of("name","eventIdentification","value","\"NurseCalls:#{id}\""));
        p.add(Map.of("name","respondingLine","value","\"responses.line.number\""));
        p.add(Map.of("name","respondingUser","value","\"responses.usr.login\""));
        p.add(Map.of("name","responsePath","value","\"responses.action\""));

        if(resp.contains("no response"))
            p.add(Map.of("name","responseType","value","\"None\""));
        else
            p.add(Map.of("name","responseType","value","\"Accept/Decline\""));

        p.add(Map.of("name","shortMessage","value","\"#{alert_type} #{bed.room.name}\""));
        p.add(Map.of("name","subject","value","\"#{alert_type} #{bed.room.name}\""));
        p.add(Map.of("name","ttl","value","10"));
        p.add(Map.of("name","retractRules","value","[\"ttlHasElapsed\"]"));
        p.add(Map.of("name","vibrate","value","\"short\""));
    }

    @SuppressWarnings("unchecked")
    private void addClinicalParams(Map<String,Object> flow, Map<String,String> row,
                                   String ringtone, String priority){
        List<Map<String,Object>> p = (List<Map<String,Object>>) flow.get("parameterAttributes");

        p.add(Map.of("destinationOrder",0,"name","destinationName","value","\"Nurse Alert\""));
        p.add(Map.of("destinationOrder",1,"name","destinationName","value","\"NoCaregivers\""));
        p.add(Map.of("destinationOrder",1,"name","message","value",
                "\"#{alert_type}\\nIssue: A Clinical Alert has been received without any caregivers assigned to room.\\nRoom/Bed: #{bed.room.name} - #{bed.bed_number} \\nAlarm Time: #{alarm_time.as_time}\""));
        p.add(Map.of("destinationOrder",1,"name","shortMessage","value",
                "\"No Caregivers Assigned for #{alert_type} in #{bed.room.name} #{bed.bed_number}\""));
        p.add(Map.of("destinationOrder",1,"name","subject","value","\"Alert Without Caregivers\""));

        if(!ringtone.isBlank()) p.add(Map.of("name","alertSound","value","\"" + ringtone + "\""));
        p.add(Map.of("name","responseAllowed","value","false"));
        p.add(Map.of("name","breakThrough","value",
                priority.equalsIgnoreCase("urgent") ? "\"voceraAndDevice\"" : "\"none\""));
        p.add(Map.of("name","enunciate","value","true"));
        p.add(Map.of("name","message","value",
                "\"Clinical Alert ${destinationName}\\nRoom: #{bed.room.name} - #{bed.bed_number}\\nAlert Type: #{alert_type}\\nAlarm Time: #{alarm_time.as_time}\""));
        p.add(Map.of("name","patientMRN","value","\"#{clinical_patient.mrn}:#{clinical_patient.visit_number}\""));
        p.add(Map.of("name","patientName","value","\"#{clinical_patient.first_name} #{clinical_patient.middle_name} #{clinical_patient.last_name}\""));
        p.add(Map.of("name","placeUid","value","\"#{bed.uid}\""));
        p.add(Map.of("name","popup","value","true"));
        p.add(Map.of("name","eventIdentification","value","\"#{id}\""));
        p.add(Map.of("name","responseType","value","\"None\""));
        p.add(Map.of("name","shortMessage","value","\"#{alert_type} #{bed.room.name}\""));
        p.add(Map.of("name","subject","value","\"#{alert_type} #{bed.room.name}\""));
        p.add(Map.of("name","ttl","value","10"));
        p.add(Map.of("name","retractRules","value","[\"ttlHasElapsed\"]"));
        p.add(Map.of("name","vibrate","value","\"short\""));
    }

    // -------------------- HELPERS --------------------
    private String mapPriority(String p){
        if(p==null)return"normal";
        String v=p.trim().toLowerCase();
        if(v.contains("low(edge)"))return"normal";
        if(v.contains("medium(edge)"))return"high";
        if(v.contains("high(edge)"))return"urgent";
        return v;
    }

    private Map<String,List<Map<String,String>>> groupRowsForFlows(List<Map<String,String>> rows,List<String>keys){
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

    private List<Map<String,String>> findUnits(String cfg){
        if(cfg==null||cfg.isBlank())return List.of();
        List<Map<String,String>> out=new ArrayList<>();
        for(var r:unitRows){
            List<String> groups=splitList(r.getOrDefault("Groups",""));
            if(groups.stream().anyMatch(g->g.equalsIgnoreCase(cfg))){
                out.add(Map.of("facilityName",r.get("Facility"),"name",r.get("Common Unit Name")));
            }
        }
        return out;
    }

    private String collectUnitsForCfg(String cfg){
        return findUnits(cfg).stream().map(u->u.get("name")).distinct()
                .collect(Collectors.joining("/"));
    }

    /** Always derives facility from Unit Breakdown tab */
    private String findFacilityForCfg(String cfg){
        if (cfg == null || cfg.isBlank()) return "";
        for (var r : unitRows){
            List<String> groups = splitList(r.getOrDefault("Groups", ""));
            for (String g : groups){
                if (g.equalsIgnoreCase(cfg)){
                    return r.getOrDefault("Facility", "");
                }
            }
        }
        return "";
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
    @SuppressWarnings("unchecked")
    private static void addToSetList(Map<String,Object>m,String k,String v){
        if(v==null||v.isBlank())return;
        List<Object>l=(List<Object>)m.computeIfAbsent(k,x->new ArrayList<>());
        if(!l.contains(v))l.add(v);
    }
}
