package com.example.exceljson;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.w3c.dom.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Clean XML Parser for Vocera Engage configuration files.
 * Parses datasets and interface rules to populate GUI tables.
 */
public class XmlParser {
    
    // Output collections
    private final List<ExcelParserV5.UnitRow> units = new ArrayList<>();
    private final List<ExcelParserV5.FlowRow> nurseCalls = new ArrayList<>();
    private final List<ExcelParserV5.FlowRow> clinicals = new ArrayList<>();
    private final List<ExcelParserV5.FlowRow> orders = new ArrayList<>();
    
    // View definitions: dataset -> view name -> view
    private final Map<String, Map<String, View>> datasetViews = new HashMap<>();
    
    // Facility/unit tracking
    private final Map<String, Set<String>> facilityUnits = new HashMap<>();
    // Track config groups by (facility, unit) per flow type to build Unit rows reliably
    private final Map<String, Map<String, java.util.Set<String>>> nurseCfgByFacUnit = new HashMap<>();
    private final Map<String, Map<String, java.util.Set<String>>> clinicalCfgByFacUnit = new HashMap<>();
    private final Map<String, Map<String, java.util.Set<String>>> ordersCfgByFacUnit = new HashMap<>();
    
    // Rule collection before processing
    private final List<Rule> allRules = new ArrayList<>();
    
    /**
     * View definition with filters
     */
    private static class View {
        String name;
        List<Filter> filters = new ArrayList<>();
    }
    
    /**
     * Filter within a view
     */
    private static class Filter {
        String relation; // "equal", "in", "not_in"
        String path;
        String value;
    }
    
    /**
     * Rule from interface
     */
    private static class Rule {
        String dataset;
        String component;
        String purpose;
        boolean isActive;
        boolean triggerCreate;
        boolean triggerUpdate;
        String deferDeliveryBy;
        List<String> viewNames = new ArrayList<>();
        Map<String, Object> settings = new HashMap<>();
        
        // Extracted from views
        Set<String> alertTypes = new HashSet<>();
        Set<String> facilities = new HashSet<>();
        Set<String> units = new HashSet<>();
        String state;
        String role;
        boolean roleFromView; // Track if role was extracted from view filter
    }
    
    /**
     * Load and parse XML file
     */
    public void load(File xmlFile) throws Exception {
        clear();
        
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(xmlFile);
        doc.getDocumentElement().normalize();
        
        // Step 1: Parse dataset views
        parseDatasetViews(doc);
        
        // Step 2: Parse interface rules
        parseInterfaceRules(doc);
        
        // Step 3: Enrich rules with view data
        enrichRulesWithViews();
        
        // Step 4: Create flow rows
        createFlowRows();
        
        // Step 5: Create unit rows
        createUnitRows();
    }
    
    /**
     * Parse dataset section to extract view definitions
     */
    private void parseDatasetViews(Document doc) {
        NodeList datasets = doc.getElementsByTagName("dataset");
        
        for (int i = 0; i < datasets.getLength(); i++) {
            Element dataset = (Element) datasets.item(i);
            
            String active = dataset.getAttribute("active");
            if ("false".equalsIgnoreCase(active)) continue;
            
            String datasetName = getChildText(dataset, "name");
            if (datasetName == null || datasetName.isEmpty()) continue;
            
            Map<String, View> views = new HashMap<>();
            NodeList viewNodes = dataset.getElementsByTagName("view");
            
            for (int j = 0; j < viewNodes.getLength(); j++) {
                Element viewElem = (Element) viewNodes.item(j);
                View view = parseView(viewElem);
                if (view != null && view.name != null) {
                    views.put(view.name, view);
                }
            }
            
            datasetViews.put(datasetName, views);
        }
    }
    
    /**
     * Parse a single view element
     */
    private View parseView(Element viewElem) {
        View view = new View();
        view.name = getChildText(viewElem, "name");
        
        NodeList filters = viewElem.getElementsByTagName("filter");
        for (int i = 0; i < filters.getLength(); i++) {
            Element filterElem = (Element) filters.item(i);
            Filter filter = new Filter();
            filter.relation = filterElem.getAttribute("relation");
            filter.path = getChildText(filterElem, "path");
            filter.value = getChildText(filterElem, "value");
            
            if (filter.path != null && filter.value != null) {
                view.filters.add(filter);
            }
        }
        
        return view;
    }
    
    /**
     * Parse interface rules
     */
    private void parseInterfaceRules(Document doc) {
        List<Rule> tempRules = new ArrayList<>();
        NodeList interfaces = doc.getElementsByTagName("interface");
        
        // First pass: collect all rules
        for (int i = 0; i < interfaces.getLength(); i++) {
            Element interfaceElem = (Element) interfaces.item(i);
            String component = interfaceElem.getAttribute("component");
            
            NodeList rules = interfaceElem.getElementsByTagName("rule");
            for (int j = 0; j < rules.getLength(); j++) {
                Element ruleElem = (Element) rules.item(j);
                Rule rule = parseRule(ruleElem, component);
                if (rule != null && rule.isActive) {
                    tempRules.add(rule);
                }
            }
        }
        
        // Second pass: validate and add rules
        for (Rule rule : tempRules) {
            if (shouldProcessRule(rule, tempRules)) {
                allRules.add(rule);
            }
        }
    }
    
    /**
     * Validate if a rule should be processed based on DataUpdate "Create" rules.
     * 
     * For all datasets (Nurse Call, Clinical, Orders):
     * - DataUpdate rules are always processed
     * - Other interface rules are always processed regardless of DataUpdate coverage
     * 
     * This ensures consistent behavior across all dataset types.
     */
    private boolean shouldProcessRule(Rule rule, List<Rule> allParsedRules) {
        // All active rules are processed
        // No filtering based on DataUpdate rules
        return true;
    }
    
    // ========== Deprecated Methods (kept for backward compatibility, no longer used) =========
    
    /**
     * Parse a single rule
     */
    private Rule parseRule(Element ruleElem, String component) {
        Rule rule = new Rule();
        rule.component = component;
        rule.dataset = ruleElem.getAttribute("dataset");
        rule.purpose = getChildText(ruleElem, "purpose");
        rule.isActive = !"false".equalsIgnoreCase(ruleElem.getAttribute("active"));
        rule.deferDeliveryBy = getChildText(ruleElem, "defer-delivery-by");
        
        // Parse trigger
        Element trigger = getChildElement(ruleElem, "trigger-on");
        if (trigger != null) {
            rule.triggerCreate = "true".equalsIgnoreCase(trigger.getAttribute("create"));
            rule.triggerUpdate = "true".equalsIgnoreCase(trigger.getAttribute("update"));
        }
        
        // Parse condition views
        Element condition = getChildElement(ruleElem, "condition");
        if (condition != null) {
            NodeList viewNodes = condition.getElementsByTagName("view");
            for (int i = 0; i < viewNodes.getLength(); i++) {
                String viewName = viewNodes.item(i).getTextContent().trim();
                if (!viewName.isEmpty()) {
                    rule.viewNames.add(viewName);
                }
            }
        }
        
        // Parse settings JSON
        String settingsJson = getChildText(ruleElem, "settings");
        if (settingsJson != null && !settingsJson.isEmpty()) {
            rule.settings = parseSettings(settingsJson);
        }
        
        return rule;
    }
    
    /**
     * Enrich rules with data from views
     */
    private void enrichRulesWithViews() {
        for (Rule rule : allRules) {
            if (rule.dataset == null) continue;
            
            Map<String, View> views = datasetViews.get(rule.dataset);
            if (views == null) continue;
            
            for (String viewName : rule.viewNames) {
                View view = views.get(viewName);
                if (view == null) continue;
                
                for (Filter filter : view.filters) {
                    extractFilterData(rule, filter);
                }
            }
        }
    }
    
    /**
     * Extract data from filter into rule
     */
    private void extractFilterData(Rule rule, Filter filter) {
        if (filter.path == null || filter.value == null) return;
        
        // Alert types
        if (filter.path.equals("alert_type")) {
            for (String type : filter.value.split(",")) {
                String trimmed = type.trim();
                if (!trimmed.isEmpty()) {
                    rule.alertTypes.add(trimmed);
                }
            }
        }
        
        // Facilities
        if (filter.path.contains("facility.name")) {
            rule.facilities.add(filter.value.trim());
        }
        
        // Units
        if (filter.path.contains("unit.name")) {
            for (String unit : filter.value.split(",")) {
                String trimmed = unit.trim();
                if (!trimmed.isEmpty()) {
                    rule.units.add(trimmed);
                }
            }
        }
        
        // State
        if (filter.path.equals("state")) {
            rule.state = filter.value.trim();
        }
        
        // Role - extract from various role-related filter paths
        if (filter.path.contains("role.name") || 
            filter.path.contains("assignments.role") || 
            filter.path.equals("role")) {
            // Store the role value directly from the filter
            rule.role = filter.value.trim();
            rule.roleFromView = true;
        }
        
        // Also try to extract role from assignment level description paths
        if (filter.path.contains("assignment_level") && filter.path.contains("description")) {
            rule.role = filter.value.trim();
            rule.roleFromView = true;
        }
    }
    
    /**
     * Create flow rows from rules
     */
    private void createFlowRows() {
        // Group rules by dataset + alert types for escalation merging
        Map<String, List<Rule>> grouped = new HashMap<>();
        
        // Collect global escalation rules (no alert type - apply to all)
        List<Rule> globalEscalationRules = new ArrayList<>();
        
        for (Rule rule : allRules) {
            // Track facilities/units
            for (String facility : rule.facilities) {
                // If facility is a placeholder (e.g. "#{bed.room.facility.name}"), skip adding to facilityUnits
                if (facility != null && facility.trim().equals("#{bed.room.facility.name}")) {
                    continue;
                }
                facilityUnits.computeIfAbsent(facility, k -> new HashSet<>()).addAll(rule.units);
            }
            
            // If rule has no alert types but has state and defer-delivery-by, it's global escalation
            if (rule.alertTypes.isEmpty() && rule.state != null && !rule.state.isEmpty() 
                && rule.deferDeliveryBy != null && !hasDestination(rule)) {
                globalEscalationRules.add(rule);
                continue;
            }
            
            // Skip other rules without alert types
            if (rule.alertTypes.isEmpty()) continue;
            
            // Group by dataset + alert types + facility + units (for config group separation)
            for (String alertType : rule.alertTypes) {
                // Sort units for consistent grouping
                String unitsKey = rule.units.stream().sorted().collect(java.util.stream.Collectors.joining(","));
                // Sort facilities for consistent grouping
                String facilitiesKey = rule.facilities.stream().sorted().collect(java.util.stream.Collectors.joining(","));
                
                String key = rule.dataset + "|" + alertType + "|" + facilitiesKey + "|" + unitsKey;
                grouped.computeIfAbsent(key, k -> new ArrayList<>()).add(rule);
            }
        }
        
        // Add global escalation rules to each alert type group in the same dataset
        for (Rule globalRule : globalEscalationRules) {
            for (Map.Entry<String, List<Rule>> entry : grouped.entrySet()) {
                String[] parts = entry.getKey().split("\\|", 4);
                String dataset = parts[0];
                String facilitiesKey = parts.length > 2 ? parts[2] : "";
                String unitsKey = parts.length > 3 ? parts[3] : "";
                
                // Match dataset and check if facilities/units overlap
                if (dataset.equals(globalRule.dataset)) {
                    boolean facilitiesMatch = facilitiesKey.isEmpty() || globalRule.facilities.isEmpty() ||
                        globalRule.facilities.stream().anyMatch(f -> facilitiesKey.contains(f));
                    boolean unitsMatch = unitsKey.isEmpty() || globalRule.units.isEmpty() ||
                        globalRule.units.stream().anyMatch(u -> unitsKey.contains(u));
                    
                    if (facilitiesMatch && unitsMatch) {
                        entry.getValue().add(globalRule);
                    }
                }
            }
        }
        
        // Process each group
        for (Map.Entry<String, List<Rule>> entry : grouped.entrySet()) {
            String[] parts = entry.getKey().split("\\|", 4);
            String dataset = parts[0];
            String alertType = parts[1];
            List<Rule> rules = entry.getValue();
            
            // Check if this is an escalation group
            boolean hasEscalation = rules.stream().anyMatch(r -> r.state != null && !r.state.isEmpty());
            
            if (hasEscalation) {
                createEscalationFlow(dataset, alertType, rules);
            } else {
                createSimpleFlow(dataset, alertType, rules);
            }
        }
    }
    
    /**
     * Create flow row with escalation
     */
    private void createEscalationFlow(String dataset, String alertType, List<Rule> rules) {
        // Separate send rules and escalate rules
        Map<String, Rule> sendByState = new HashMap<>();
        Map<String, String> escalateDelay = new HashMap<>();
        
        for (Rule rule : rules) {
            if (rule.state == null || rule.state.isEmpty()) continue;
            
            String state = normalizeState(rule.state);
            
            // If has destination, it's a send rule
            if (hasDestination(rule)) {
                sendByState.put(state, rule);
            }
            // If has defer-delivery-by but no destination, it's escalation timing
            else if (rule.deferDeliveryBy != null && !hasDestination(rule)) {
                escalateDelay.put(state, rule.deferDeliveryBy);
            }
        }
        
        if (sendByState.isEmpty()) return;
        
        // Build a template flow with common fields and recipients/timing
        ExcelParserV5.FlowRow template = new ExcelParserV5.FlowRow();
        template.inScope = true;
        template.type = normalizeDataset(dataset);
        template.alarmName = alertType;
        template.sendingName = alertType;

        // Get reference rule for common fields
        Rule refRule = sendByState.values().iterator().next();
        template.deviceA = mapComponent(refRule.component);

        // Map states to recipients and timing on the template
        String[] states = {"Primary", "Secondary", "Tertiary", "Quaternary", "Quinary"};
        for (int i = 0; i < states.length; i++) {
            String state = states[i];
            Rule sendRule = sendByState.get(state);

            if (sendRule != null) {
                String recipient = extractDestination(sendRule);
                setRecipient(template, i + 1, recipient, sendRule.roleFromView);

                // Apply settings from first send rule
                if (i == 0) {
                    applySettings(template, sendRule);
                    if (sendRule.triggerCreate) {
                        template.t1 = "Immediate";
                    } else if (template.t1 == null || template.t1.isEmpty()) {
                        // Default to Immediate if not specified
                        template.t1 = "Immediate";
                    }
                }
            }

            // Set escalation delay (maps to next recipient's time)
            // Use raw seconds value, not formatted
            String delay = escalateDelay.get(state);
            if (delay != null && i < states.length - 1) {
                switch (i + 2) {
                    case 2: template.t2 = delay; break;
                    case 3: template.t3 = delay; break;
                    case 4: template.t4 = delay; break;
                    case 5: template.t5 = delay; break;
                }
            }
        }

        // Compute unions of facilities and units for splitting
        Set<String> facs = new LinkedHashSet<>();
        Set<String> uns = new LinkedHashSet<>();
        for (Rule r : rules) {
            facs.addAll(r.facilities);
            uns.addAll(r.units);
        }
        if (facs.isEmpty()) facs.add("");
        if (uns.isEmpty()) uns.add("");

        // Emit one flow per (facility, unit) pair for accurate Units tab population
        for (String fac : facs) {
            for (String un : uns) {
                ExcelParserV5.FlowRow flow = new ExcelParserV5.FlowRow();
                // Copy from template
                flow.inScope = template.inScope;
                flow.type = template.type;
                flow.alarmName = template.alarmName;
                flow.sendingName = template.sendingName;
                flow.priorityRaw = template.priorityRaw;
                flow.deviceA = template.deviceA;
                flow.deviceB = template.deviceB;
                flow.ringtone = template.ringtone;
                flow.responseOptions = template.responseOptions;
                flow.breakThroughDND = template.breakThroughDND;
                flow.multiUserAccept = template.multiUserAccept;
                flow.escalateAfter = template.escalateAfter;
                flow.ttlValue = template.ttlValue;
                flow.enunciate = template.enunciate;
                flow.emdan = template.emdan;
                flow.t1 = template.t1; flow.r1 = template.r1;
                flow.t2 = template.t2; flow.r2 = template.r2;
                flow.t3 = template.t3; flow.r3 = template.r3;
                flow.t4 = template.t4; flow.r4 = template.r4;
                flow.t5 = template.t5; flow.r5 = template.r5;

                // Config group built from explicit facility/unit
                Set<String> fset = fac.isEmpty() ? Collections.emptySet() : Set.of(fac);
                Set<String> uset = un.isEmpty() ? Collections.emptySet() : Set.of(un);
                flow.configGroup = createConfigGroup(dataset, fset, uset);

                addToList(flow);
                // Track config group per (facility, unit) and type for Unit rows
                trackConfigGroupForUnit(flow.type, fac, un, flow.configGroup);
            }
        }
    }
    
    /**
     * Create simple flow row (no escalation)
     */
    private void createSimpleFlow(String dataset, String alertType, List<Rule> rules) {
        // Prefer rules with destination, but allow rules without if none have destination
        Rule sendRule = rules.stream()
            .filter(this::hasDestination)
            .findFirst()
            .or(() -> rules.stream().findFirst())
            .orElse(null);
        
        if (sendRule == null) return;
        
        // Build a template flow
        ExcelParserV5.FlowRow template = new ExcelParserV5.FlowRow();
        template.inScope = true;
        template.type = normalizeDataset(dataset);
        template.alarmName = alertType;
        template.sendingName = alertType;
        template.deviceA = mapComponent(sendRule.component);

        // Set recipient and timing (recipient is optional)
        String recipient = extractDestination(sendRule);
        setRecipient(template, 1, recipient, sendRule.roleFromView);

        if (sendRule.triggerCreate) {
            template.t1 = sendRule.deferDeliveryBy != null ? sendRule.deferDeliveryBy : "Immediate";
        } else if (template.t1 == null || template.t1.isEmpty()) {
            // Default to Immediate if not specified
            template.t1 = "Immediate";
        }

        applySettings(template, sendRule);

        // Compute unions for splitting
        Set<String> facs = new LinkedHashSet<>(sendRule.facilities);
        Set<String> uns = new LinkedHashSet<>(sendRule.units);
        if (facs.isEmpty()) facs.add("");
        if (uns.isEmpty()) uns.add("");

        for (String fac : facs) {
            for (String un : uns) {
                ExcelParserV5.FlowRow flow = new ExcelParserV5.FlowRow();
                // Copy from template
                flow.inScope = template.inScope;
                flow.type = template.type;
                flow.alarmName = template.alarmName;
                flow.sendingName = template.sendingName;
                flow.priorityRaw = template.priorityRaw;
                flow.deviceA = template.deviceA;
                flow.deviceB = template.deviceB;
                flow.ringtone = template.ringtone;
                flow.responseOptions = template.responseOptions;
                flow.breakThroughDND = template.breakThroughDND;
                flow.multiUserAccept = template.multiUserAccept;
                flow.escalateAfter = template.escalateAfter;
                flow.ttlValue = template.ttlValue;
                flow.enunciate = template.enunciate;
                flow.emdan = template.emdan;
                flow.t1 = template.t1; flow.r1 = template.r1;
                flow.t2 = template.t2; flow.r2 = template.r2;
                flow.t3 = template.t3; flow.r3 = template.r3;
                flow.t4 = template.t4; flow.r4 = template.r4;
                flow.t5 = template.t5; flow.r5 = template.r5;

                // Config group per facility/unit
                Set<String> fset = fac.isEmpty() ? Collections.emptySet() : Set.of(fac);
                Set<String> uset = un.isEmpty() ? Collections.emptySet() : Set.of(un);
                flow.configGroup = createConfigGroup(dataset, fset, uset);

                addToList(flow);
                // Track config group per (facility, unit) and type for Unit rows
                trackConfigGroupForUnit(flow.type, fac, un, flow.configGroup);
            }
        }
    }
    
    /**
     * Create unit rows
     */
    private void createUnitRows() {
        // Build union of all facilities and units across types
        Set<String> facilities = new LinkedHashSet<>();
        facilities.addAll(nurseCfgByFacUnit.keySet());
        facilities.addAll(clinicalCfgByFacUnit.keySet());
        facilities.addAll(ordersCfgByFacUnit.keySet());
        // Focus on facility.name: exclude generic entries with no facility
        facilities.remove("");

        for (String facility : facilities) {
            // Union of units for this facility across all types
            Set<String> unitsForFacility = new LinkedHashSet<>();
            unitsForFacility.addAll(nurseCfgByFacUnit.getOrDefault(facility, Map.of()).keySet());
            unitsForFacility.addAll(clinicalCfgByFacUnit.getOrDefault(facility, Map.of()).keySet());
            unitsForFacility.addAll(ordersCfgByFacUnit.getOrDefault(facility, Map.of()).keySet());

            for (String unit : unitsForFacility) {
                ExcelParserV5.UnitRow unitRow = new ExcelParserV5.UnitRow();
                unitRow.facility = facility;
                unitRow.unitNames = unit;

                // Nurse groups
                for (String cfg : nurseCfgByFacUnit
                        .getOrDefault(facility, Map.of())
                        .getOrDefault(unit, Set.of())) {
                    if (unitRow.nurseGroup.isEmpty()) unitRow.nurseGroup = cfg;
                    else if (!unitRow.nurseGroup.contains(cfg)) unitRow.nurseGroup += ", " + cfg;
                }

                // Clinical groups
                for (String cfg : clinicalCfgByFacUnit
                        .getOrDefault(facility, Map.of())
                        .getOrDefault(unit, Set.of())) {
                    if (unitRow.clinGroup.isEmpty()) unitRow.clinGroup = cfg;
                    else if (!unitRow.clinGroup.contains(cfg)) unitRow.clinGroup += ", " + cfg;
                }

                // Orders groups
                for (String cfg : ordersCfgByFacUnit
                        .getOrDefault(facility, Map.of())
                        .getOrDefault(unit, Set.of())) {
                    if (unitRow.ordersGroup.isEmpty()) unitRow.ordersGroup = cfg;
                    else if (!unitRow.ordersGroup.contains(cfg)) unitRow.ordersGroup += ", " + cfg;
                }

                units.add(unitRow);
            }
        }
    }
    
    /**
     * Helper method to extract facility and unit from a flow's config group and track it
     */
    // Helper: track config groups per (facility, unit) using emitted flow context
    private void trackConfigGroupForUnit(String flowType, String facility, String unit, String configGroup) {
        String fac = facility == null ? "" : facility;
        String un = unit == null ? "" : unit;
        if ("NurseCalls".equals(flowType)) {
            nurseCfgByFacUnit
                .computeIfAbsent(fac, k -> new HashMap<>())
                .computeIfAbsent(un, k -> new java.util.HashSet<>())
                .add(configGroup);
        } else if ("Orders".equals(flowType)) {
            ordersCfgByFacUnit
                .computeIfAbsent(fac, k -> new HashMap<>())
                .computeIfAbsent(un, k -> new java.util.HashSet<>())
                .add(configGroup);
        } else {
            clinicalCfgByFacUnit
                .computeIfAbsent(fac, k -> new HashMap<>())
                .computeIfAbsent(un, k -> new java.util.HashSet<>())
                .add(configGroup);
        }
    }
    
    // ========== Helper Methods ==========
    
    private String normalizeState(String state) {
        if ("Group".equalsIgnoreCase(state)) return "Primary";
        return state;
    }
    
    private String normalizeDataset(String dataset) {
        if (dataset == null) return "Clinicals";
        if (dataset.toLowerCase().contains("nurse")) return "NurseCalls";
        if (dataset.toLowerCase().contains("order")) return "Orders";
        return "Clinicals";
    }
    
    private String mapComponent(String component) {
        if (component == null) return "";
        switch (component.toUpperCase()) {
            case "VMP": return "VMP";
            case "DATAUPDATE": return "Edge";
            case "VOCERA": return "Vocera";
            case "XMPP": return "XMPP";
            default: return component;
        }
    }
    
    private String createConfigGroup(String dataset, Set<String> facilities, Set<String> units) {
        String facility = facilities.isEmpty() ? "" : facilities.iterator().next();
        String unit = units.isEmpty() ? "" : units.iterator().next();
        // Always place dataset last. Use "All_Facilities" and "AllUnits" when they cannot be determined
        List<String> parts = new ArrayList<>();
        // Treat placeholder as missing facility
        if (facility != null && !facility.isEmpty() && !facility.equals("#{bed.room.facility.name}")) {
            parts.add(facility);
        } else {
            // Hardcode "All_Facilities" when facility name is not found or is a placeholder
            parts.add("All_Facilities");
        }
        if (unit != null && !unit.isEmpty()) {
            parts.add(unit);
        } else {
            // Hardcode "AllUnits" when unit name is not found
            parts.add("AllUnits");
        }
        if (dataset != null && !dataset.isEmpty()) parts.add(dataset);
        return String.join("_", parts);
    }
    
    private boolean hasDestination(Rule rule) {
        return rule.settings.containsKey("destination") && 
               rule.settings.get("destination") != null &&
               !rule.settings.get("destination").toString().isEmpty();
    }
    
    private String extractDestination(Rule rule) {
        String destination = null;
        if (rule.settings.containsKey("destination")) {
            destination = rule.settings.get("destination").toString();
        }
        
        // If destination is a group (starts with g-), use it directly
        if (destination != null && destination.startsWith("g-")) {
            return destination;
        }
        
        // Otherwise, prefer role name from view filters over raw destination template
        if (rule.role != null && !rule.role.isEmpty()) {
            return rule.role;
        }
        
        // Fall back to destination if no role is found (for templates or other values)
        return destination != null ? destination : "";
    }
    
    private String formatRecipient(String recipient, boolean isFromRole) {
        if (recipient == null || recipient.isEmpty()) return "";
        
        // Check if recipient contains comma-separated values
        if (recipient.contains(",")) {
            String[] parts = recipient.split(",");
            StringBuilder result = new StringBuilder();
            for (int i = 0; i < parts.length; i++) {
                String part = parts[i].trim();
                if (i > 0) {
                    result.append("\n");
                }
                if (isFromRole) {
                    result.append("VAssign:[Room] ").append(part);
                } else {
                    result.append("VGroup ").append(part);
                }
            }
            return result.toString();
        }
        
        // Single recipient
        if (isFromRole) {
            return "VAssign:[Room] " + recipient;
        } else {
            return "VGroup " + recipient;
        }
    }
    
    private void setRecipient(ExcelParserV5.FlowRow flow, int index, String recipient, boolean isFromRole) {
        if (recipient == null || recipient.isEmpty()) return;
        String formatted = formatRecipient(recipient, isFromRole);
        switch (index) {
            case 1: flow.r1 = formatted; break;
            case 2: flow.r2 = formatted; break;
            case 3: flow.r3 = formatted; break;
            case 4: flow.r4 = formatted; break;
            case 5: flow.r5 = formatted; break;
        }
    }
    
    private void setTime(ExcelParserV5.FlowRow flow, int index, String time) {
        if (time == null || time.isEmpty()) return;
        String formatted = formatTime(time);
        switch (index) {
            case 1: flow.t1 = formatted; break;
            case 2: flow.t2 = formatted; break;
            case 3: flow.t3 = formatted; break;
            case 4: flow.t4 = formatted; break;
            case 5: flow.t5 = formatted; break;
        }
    }
    
    private String formatTime(String seconds) {
        try {
            int sec = Integer.parseInt(seconds);
            if (sec == 0) return "Immediate";
            if (sec < 60) return sec + "SEC";
            return (sec / 60) + "MIN";
        } catch (NumberFormatException e) {
            return seconds;
        }
    }
    
    private void applySettings(ExcelParserV5.FlowRow flow, Rule rule) {
        Map<String, Object> settings = rule.settings;
        
        if (settings.containsKey("priority")) {
            String priorityRaw = settings.get("priority").toString();
            flow.priorityRaw = mapPriority(priorityRaw);
        }
        if (settings.containsKey("ttl")) {
            flow.ttlValue = settings.get("ttl").toString();
        }
        if (settings.containsKey("enunciate")) {
            String enunciate = settings.get("enunciate").toString();
            flow.enunciate = normalizeEnunciate(enunciate);
        }
        if (settings.containsKey("overrideDND")) {
            boolean override = Boolean.parseBoolean(settings.get("overrideDND").toString());
            flow.breakThroughDND = override ? "TRUE" : "FALSE";
        }
        if (settings.containsKey("displayValues")) {
            flow.responseOptions = settings.get("displayValues").toString();
        }
    }
    
    private String normalizeEnunciate(String enunciate) {
        if ("ENUNCIATE_ALWAYS".equals(enunciate)) {
            return "ENUNCIATE";
        }
        return enunciate;
    }
    
    private String mapPriority(String priority) {
        switch (priority) {
            case "0":
            case "3": return "Urgent";
            case "1": return "High";
            case "2": return "Normal";
            default: return priority;
        }
    }
    
    private void addToList(ExcelParserV5.FlowRow flow) {
        if ("NurseCalls".equals(flow.type)) {
            nurseCalls.add(flow);
        } else if ("Orders".equals(flow.type)) {
            orders.add(flow);
        } else {
            clinicals.add(flow);
        }
    }
    
    private boolean hasConfigGroup(String configGroup) {
        return nurseCalls.stream().anyMatch(f -> configGroup.equals(f.configGroup)) ||
               clinicals.stream().anyMatch(f -> configGroup.equals(f.configGroup)) ||
               orders.stream().anyMatch(f -> configGroup.equals(f.configGroup));
    }
    
    private Map<String, Object> parseSettings(String json) {
        Map<String, Object> result = new HashMap<>();
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(json);
            
            if (root.has("priority")) result.put("priority", root.get("priority").asText());
            if (root.has("ttl")) result.put("ttl", root.get("ttl").asText());
            if (root.has("enunciate")) result.put("enunciate", root.get("enunciate").asText());
            if (root.has("overrideDND")) result.put("overrideDND", root.get("overrideDND").asBoolean());
            if (root.has("destination")) result.put("destination", root.get("destination").asText());
            
            if (root.has("displayValues") && root.get("displayValues").isArray()) {
                List<String> values = new ArrayList<>();
                root.get("displayValues").forEach(n -> values.add(n.asText()));
                result.put("displayValues", String.join(",", values));
            }
        } catch (Exception e) {
            // Ignore JSON parse errors
        }
        return result;
    }
    
    private String getChildText(Element parent, String tagName) {
        NodeList nodes = parent.getElementsByTagName(tagName);
        return nodes.getLength() > 0 ? nodes.item(0).getTextContent().trim() : null;
    }
    
    private Element getChildElement(Element parent, String tagName) {
        NodeList nodes = parent.getElementsByTagName(tagName);
        return nodes.getLength() > 0 ? (Element) nodes.item(0) : null;
    }
    
    private void clear() {
        units.clear();
        nurseCalls.clear();
        clinicals.clear();
        orders.clear();
        datasetViews.clear();
        facilityUnits.clear();
        allRules.clear();
        nurseCfgByFacUnit.clear();
        clinicalCfgByFacUnit.clear();
        ordersCfgByFacUnit.clear();
    }
    
    // ========== Public Getters ==========
    
    public List<ExcelParserV5.UnitRow> getUnits() {
        return new ArrayList<>(units);
    }
    
    public List<ExcelParserV5.FlowRow> getNurseCalls() {
        return new ArrayList<>(nurseCalls);
    }
    
    public List<ExcelParserV5.FlowRow> getClinicals() {
        return new ArrayList<>(clinicals);
    }
    
    public List<ExcelParserV5.FlowRow> getOrders() {
        return new ArrayList<>(orders);
    }
    
    public String getLoadSummary() {
        // Compute unique config group counts per flow type
        int nurseCfgs = (int) nurseCalls.stream()
                .map(r -> r.configGroup == null ? "" : r.configGroup.trim())
                .filter(s -> !s.isEmpty())
                .distinct()
                .count();
        int clinicalCfgs = (int) clinicals.stream()
                .map(r -> r.configGroup == null ? "" : r.configGroup.trim())
                .filter(s -> !s.isEmpty())
                .distinct()
                .count();
        int ordersCfgs = (int) orders.stream()
                .map(r -> r.configGroup == null ? "" : r.configGroup.trim())
                .filter(s -> !s.isEmpty())
                .distinct()
                .count();
        int totalCfgs = nurseCfgs + clinicalCfgs + ordersCfgs;

        return String.format(
            "✅ XML Load Complete%n%n" +
            "Loaded:%n" +
            "  • %d Unit rows%n" +
            "  • %d Nurse Call rows%n" +
            "  • %d Clinical rows%n" +
            "  • %d Orders rows%n" +
            "  • Config Groups: %d (Nurse), %d (Clinical), %d (Orders) — Total %d",
            units.size(), nurseCalls.size(), clinicals.size(), orders.size(),
            nurseCfgs, clinicalCfgs, ordersCfgs, totalCfgs
        );
    }
}
