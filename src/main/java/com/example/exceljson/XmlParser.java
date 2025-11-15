package com.example.exceljson;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.w3c.dom.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.*;

/**
 * Parser for Vocera Engage XML configuration files.
 * Extracts datasets and interface rules to populate the GUI.
 */
public class XmlParser {
    
    private final List<ExcelParserV5.UnitRow> units = new ArrayList<>();
    private final List<ExcelParserV5.FlowRow> nurseCalls = new ArrayList<>();
    private final List<ExcelParserV5.FlowRow> clinicals = new ArrayList<>();
    private final List<ExcelParserV5.FlowRow> orders = new ArrayList<>();
    
    // Map: dataset name -> view name -> ViewDefinition
    private final Map<String, Map<String, ViewDefinition>> datasetViews = new LinkedHashMap<>();
    
    // Track facilities and units found
    private final Map<String, Set<String>> facilityToUnits = new LinkedHashMap<>();
    
    // Temporary storage for rules before merging
    private final List<RuleData> collectedRules = new ArrayList<>();
    
    private static class ViewDefinition {
        String name;
        String description;
        List<FilterDefinition> filters = new ArrayList<>();
    }
    
    private static class FilterDefinition {
        String relation;  // "equal", "in", "not_in"
        String path;
        String value;
    }
    
    private static class RuleData {
        String dataset;
        String purpose;
        String component;
        String deferDeliveryBy;
        boolean isCreate;
        boolean isUpdate;
        List<String> viewNames;
        Map<String, Object> settings;
        Set<String> alertTypes;
        Set<String> facilities;
        Set<String> units;
        String role;
        String state; // Primary, Secondary, Tertiary, Quaternary
    }
    
    /**
     * Load and parse an XML file.
     */
    public void load(File xmlFile) throws Exception {
        clear();
        
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(xmlFile);
        doc.getDocumentElement().normalize();
        
        // Parse datasets first (to build view definitions)
        parseDatasets(doc);
        
        // Parse interfaces (to collect rules)
        parseInterfaces(doc);
        
        // Merge rules based on state escalation patterns
        mergeStateBasedRules();
        
        // Create unit rows from collected facility/unit combinations
        createUnitRows();
    }
    
    /**
     * Parse the <datasets> section to extract view definitions.
     */
    private void parseDatasets(Document doc) {
        NodeList datasetNodes = doc.getElementsByTagName("dataset");
        
        for (int i = 0; i < datasetNodes.getLength(); i++) {
            Element datasetElement = (Element) datasetNodes.item(i);
            
            // Check if dataset is active
            String active = datasetElement.getAttribute("active");
            if ("false".equalsIgnoreCase(active)) {
                continue;
            }
            
            // Get dataset name
            String datasetName = getElementText(datasetElement, "name");
            if (datasetName == null || datasetName.isEmpty()) {
                continue;
            }
            
            Map<String, ViewDefinition> viewMap = new LinkedHashMap<>();
            
            // Parse all views in this dataset
            NodeList viewNodes = datasetElement.getElementsByTagName("view");
            for (int j = 0; j < viewNodes.getLength(); j++) {
                Element viewElement = (Element) viewNodes.item(j);
                ViewDefinition view = parseView(viewElement);
                if (view != null && view.name != null) {
                    viewMap.put(view.name, view);
                }
            }
            
            datasetViews.put(datasetName, viewMap);
        }
    }
    
    /**
     * Parse a single <view> element.
     */
    private ViewDefinition parseView(Element viewElement) {
        ViewDefinition view = new ViewDefinition();
        view.name = getElementText(viewElement, "name");
        view.description = getElementText(viewElement, "description");
        
        // Parse all filters
        NodeList filterNodes = viewElement.getElementsByTagName("filter");
        for (int i = 0; i < filterNodes.getLength(); i++) {
            Element filterElement = (Element) filterNodes.item(i);
            FilterDefinition filter = new FilterDefinition();
            filter.relation = filterElement.getAttribute("relation");
            filter.path = getElementText(filterElement, "path");
            filter.value = getElementText(filterElement, "value");
            
            if (filter.path != null && filter.value != null) {
                view.filters.add(filter);
            }
        }
        
        return view;
    }
    
    /**
     * Parse the <interfaces> section to create flow rows.
     */
    private void parseInterfaces(Document doc) {
        NodeList interfaceNodes = doc.getElementsByTagName("interface");
        
        for (int i = 0; i < interfaceNodes.getLength(); i++) {
            Element interfaceElement = (Element) interfaceNodes.item(i);
            
            String component = interfaceElement.getAttribute("component");
            String interfaceName = getElementText(interfaceElement, "name");
            
            // Parse all rules in this interface
            NodeList ruleNodes = interfaceElement.getElementsByTagName("rule");
            for (int j = 0; j < ruleNodes.getLength(); j++) {
                Element ruleElement = (Element) ruleNodes.item(j);
                
                // Check if rule is active
                String active = ruleElement.getAttribute("active");
                if ("false".equalsIgnoreCase(active)) {
                    continue;
                }
                
                parseRule(ruleElement, component);
            }
        }
    }
    
    /**
     * Parse a single <rule> element and collect rule data.
     */
    private void parseRule(Element ruleElement, String component) {
        // Get basic rule attributes
        String dataset = ruleElement.getAttribute("dataset");
        String purpose = getElementText(ruleElement, "purpose");
        
        // Get timing information
        String deferDeliveryBy = getElementText(ruleElement, "defer-delivery-by");
        
        // Check trigger type
        Element triggerOn = getFirstChildElement(ruleElement, "trigger-on");
        boolean isCreate = false;
        boolean isUpdate = false;
        if (triggerOn != null) {
            isCreate = "true".equalsIgnoreCase(triggerOn.getAttribute("create"));
            isUpdate = "true".equalsIgnoreCase(triggerOn.getAttribute("update"));
        }
        
        // Get settings JSON
        String settingsJson = getElementText(ruleElement, "settings");
        
        // Get condition views
        List<String> viewNames = new ArrayList<>();
        Element condition = getFirstChildElement(ruleElement, "condition");
        if (condition != null) {
            NodeList viewNodes = condition.getElementsByTagName("view");
            for (int i = 0; i < viewNodes.getLength(); i++) {
                String viewName = viewNodes.item(i).getTextContent().trim();
                if (!viewName.isEmpty()) {
                    viewNames.add(viewName);
                }
            }
        }
        
        // Extract alert types, facilities, units, role, and state from views
        Set<String> alertTypes = new HashSet<>();
        Set<String> facilities = new HashSet<>();
        Set<String> units = new HashSet<>();
        String role = null;
        String state = null;
        
        if (datasetViews.containsKey(dataset)) {
            Map<String, ViewDefinition> views = datasetViews.get(dataset);
            for (String viewName : viewNames) {
                ViewDefinition view = views.get(viewName);
                if (view != null) {
                    for (FilterDefinition filter : view.filters) {
                        extractFilterData(filter, alertTypes, facilities, units);
                        if (filter.path != null && filter.path.contains("role.name")) {
                            role = filter.value;
                        }
                        // Extract state value
                        if (filter.path != null && filter.path.equals("state")) {
                            state = filter.value;
                        }
                    }
                }
            }
        }
        
        // Parse settings JSON to extract additional fields
        Map<String, Object> settings = parseSettingsJson(settingsJson);
        
        // Create RuleData and add to collection
        RuleData ruleData = new RuleData();
        ruleData.dataset = dataset;
        ruleData.purpose = purpose;
        ruleData.component = component;
        ruleData.deferDeliveryBy = deferDeliveryBy;
        ruleData.isCreate = isCreate;
        ruleData.isUpdate = isUpdate;
        ruleData.viewNames = viewNames;
        ruleData.settings = settings;
        ruleData.alertTypes = alertTypes;
        ruleData.facilities = facilities;
        ruleData.units = units;
        ruleData.role = role;
        ruleData.state = state;
        
        collectedRules.add(ruleData);
    }
    
    /**
     * Extract data from filter definitions.
     */
    private void extractFilterData(FilterDefinition filter, Set<String> alertTypes, 
                                   Set<String> facilities, Set<String> units) {
        if (filter.path == null || filter.value == null) {
            return;
        }
        
        // Extract alert types
        if (filter.path.equals("alert_type")) {
            String[] types = filter.value.split(",");
            for (String type : types) {
                String trimmed = type.trim();
                if (!trimmed.isEmpty()) {
                    alertTypes.add(trimmed);
                }
            }
        }
        
        // Extract facilities
        if (filter.path.contains("facility.name")) {
            facilities.add(filter.value.trim());
        }
        
        // Extract units
        if (filter.path.contains("unit.name")) {
            if ("in".equals(filter.relation) || "not_in".equals(filter.relation)) {
                String[] unitList = filter.value.split(",");
                for (String unit : unitList) {
                    String trimmed = unit.trim();
                    if (!trimmed.isEmpty()) {
                        units.add(trimmed);
                    }
                }
            } else {
                units.add(filter.value.trim());
            }
        }
    }
    
    /**
     * Parse settings JSON string.
     */
    private Map<String, Object> parseSettingsJson(String settingsJson) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (settingsJson == null || settingsJson.isEmpty()) {
            return result;
        }
        
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(settingsJson);
            
            // Extract relevant fields
            if (root.has("priority")) {
                result.put("priority", root.get("priority").asText());
            }
            if (root.has("ttl")) {
                result.put("ttl", root.get("ttl").asText());
            }
            if (root.has("enunciate")) {
                result.put("enunciate", root.get("enunciate").asText());
            }
            if (root.has("overrideDND")) {
                result.put("overrideDND", root.get("overrideDND").asBoolean() ? "TRUE" : "FALSE");
            }
            if (root.has("destination")) {
                result.put("destination", root.get("destination").asText());
            }
            if (root.has("displayValues")) {
                JsonNode displayValues = root.get("displayValues");
                if (displayValues.isArray()) {
                    List<String> values = new ArrayList<>();
                    for (JsonNode node : displayValues) {
                        values.add(node.asText());
                    }
                    result.put("displayValues", String.join(",", values));
                }
            }
            if (root.has("storedValues")) {
                JsonNode storedValues = root.get("storedValues");
                if (storedValues.isArray()) {
                    List<String> values = new ArrayList<>();
                    for (JsonNode node : storedValues) {
                        values.add(node.asText());
                    }
                    result.put("storedValues", String.join(",", values));
                }
            }
        } catch (Exception e) {
            // Ignore JSON parsing errors
        }
        
        return result;
    }
    
    /**
     * Merge rules based on state escalation patterns.
     * Groups rules by dataset and alert types, then merges escalation states.
     */
    private void mergeStateBasedRules() {
        // Group rules by dataset and alert types
        Map<String, List<RuleData>> groupedRules = new LinkedHashMap<>();
        
        for (RuleData rule : collectedRules) {
            // Create a key based on dataset + alert types + units
            String key = createRuleGroupKey(rule);
            groupedRules.computeIfAbsent(key, k -> new ArrayList<>()).add(rule);
        }
        
        // Process each group
        for (Map.Entry<String, List<RuleData>> entry : groupedRules.entrySet()) {
            List<RuleData> rules = entry.getValue();
            
            // Check if this group has state-based escalation
            if (hasStateBasedEscalation(rules)) {
                mergeEscalationGroup(rules);
            } else {
                // No state escalation, create flows normally
                for (RuleData rule : rules) {
                    createFlowRowsFromRule(rule);
                }
            }
        }
    }
    
    /**
     * Create a grouping key for rules based on dataset and units.
     * Alert types are excluded because escalation rules don't have them.
     */
    private String createRuleGroupKey(RuleData rule) {
        StringBuilder key = new StringBuilder();
        key.append(rule.dataset != null ? rule.dataset : "");
        key.append("|");
        
        // Add units
        List<String> sortedUnits = new ArrayList<>(rule.units);
        Collections.sort(sortedUnits);
        key.append(String.join(",", sortedUnits));
        
        return key.toString();
    }
    
    /**
     * Check if a group of rules has state-based escalation pattern.
     */
    private boolean hasStateBasedEscalation(List<RuleData> rules) {
        for (RuleData rule : rules) {
            if (rule.state != null && !rule.state.isEmpty()) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Merge an escalation group into consolidated flow rows.
     */
    private void mergeEscalationGroup(List<RuleData> rules) {
        // Separate rules by whether they send messages or just escalate
        Map<String, Map<String, RuleData>> sendRulesByStateAndAlertType = new LinkedHashMap<>();
        // Map from SOURCE state (the state being checked in condition) to escalation rule
        Map<String, RuleData> escalateRulesBySourceState = new LinkedHashMap<>();
        
        // Track whether we have any DataUpdate escalation rules
        boolean hasDataUpdateEscalation = false;
        
        for (RuleData rule : rules) {
            if (rule.state != null && !rule.state.isEmpty()) {
                // Check if this is a SEND rule (has destination/role) or ESCALATE rule
                if (rule.role != null && !rule.role.isEmpty() || 
                    (rule.settings.containsKey("destination") && 
                     rule.settings.get("destination") != null && 
                     !((String)rule.settings.get("destination")).isEmpty())) {
                    // SEND rule - group by state and alert type
                    for (String alertType : rule.alertTypes) {
                        String key = rule.state + "|" + alertType;
                        sendRulesByStateAndAlertType.computeIfAbsent(key, k -> new LinkedHashMap<>())
                            .put(rule.state, rule);
                    }
                } else {
                    // ESCALATE rule - the state value indicates the SOURCE state (in the condition)
                    // This rule transitions FROM this state TO another state
                    // Track if we have DataUpdate escalation rules
                    if ("DataUpdate".equalsIgnoreCase(rule.component)) {
                        hasDataUpdateEscalation = true;
                    }
                    escalateRulesBySourceState.put(rule.state, rule);
                }
            }
        }
        
        // If we have DataUpdate escalation rules, filter out non-DataUpdate escalation rules
        // This ensures timing comes ONLY from DataUpdate interface when it's present
        if (hasDataUpdateEscalation) {
            escalateRulesBySourceState.entrySet().removeIf(entry -> 
                !"DataUpdate".equalsIgnoreCase(entry.getValue().component));
        }
        
        // Find all unique alert types across the group (only from SEND rules)
        Set<String> allAlertTypes = new HashSet<>();
        for (RuleData rule : rules) {
            // Only include alert types from rules that actually send messages
            if (rule.role != null && !rule.role.isEmpty() || 
                (rule.settings.containsKey("destination") && 
                 rule.settings.get("destination") != null && 
                 !((String)rule.settings.get("destination")).isEmpty())) {
                allAlertTypes.addAll(rule.alertTypes);
            }
        }
        
        // Skip if no alert types found (only escalation rules without send rules)
        if (allAlertTypes.isEmpty()) {
            return;
        }
        
        // Create merged flow rows for each alert type
        for (String alertType : allAlertTypes) {
            createMergedEscalationFlow(alertType, rules, sendRulesByStateAndAlertType, escalateRulesBySourceState);
        }
    }
    
    /**
     * Create a merged flow row for a specific alert type with escalation.
     */
    private void createMergedEscalationFlow(String alertType, List<RuleData> allRules,
                                           Map<String, Map<String, RuleData>> sendRulesByStateAndAlertType,
                                           Map<String, RuleData> escalateRulesBySourceState) {
        // Get a reference rule for common properties
        RuleData refRule = allRules.get(0);
        
        ExcelParserV5.FlowRow flow = new ExcelParserV5.FlowRow();
        flow.inScope = true;
        flow.alarmName = alertType;
        flow.sendingName = alertType;
        
        // Determine flow type from dataset
        if (refRule.dataset != null) {
            if (refRule.dataset.equalsIgnoreCase("NurseCalls") || refRule.dataset.contains("Nurse")) {
                flow.type = "NurseCalls";
            } else if (refRule.dataset.equalsIgnoreCase("Orders") || refRule.dataset.contains("Order")) {
                flow.type = "Orders";
            } else {
                flow.type = "Clinicals";
            }
        }
        
        // Set config group
        String unit = refRule.units.isEmpty() ? "" : refRule.units.iterator().next();
        if (refRule.dataset != null && !unit.isEmpty()) {
            flow.configGroup = refRule.dataset + "_" + unit;
        } else if (refRule.dataset != null) {
            flow.configGroup = refRule.dataset;
        }
        
        // Track facility/unit combinations
        for (String facility : refRule.facilities) {
            facilityToUnits.computeIfAbsent(facility, k -> new LinkedHashSet<>()).addAll(refRule.units);
        }
        
        // Map component to device - use component from SEND rules, not escalation rules
        // Find the first SEND rule to get the component that actually sends messages
        RuleData sendRuleForDevice = null;
        for (RuleData rule : allRules) {
            // Check if this is a SEND rule (has destination/role)
            if (rule.role != null && !rule.role.isEmpty() || 
                (rule.settings.containsKey("destination") && 
                 rule.settings.get("destination") != null && 
                 !((String)rule.settings.get("destination")).isEmpty())) {
                sendRuleForDevice = rule;
                break;
            }
        }
        
        if (sendRuleForDevice != null && sendRuleForDevice.component != null) {
            flow.deviceA = mapComponentToDevice(sendRuleForDevice.component);
        } else if (refRule.component != null) {
            // Fallback to refRule if no SEND rule found (shouldn't happen in normal cases)
            flow.deviceA = mapComponentToDevice(refRule.component);
        }
        
        // Process escalation states in order: Primary -> Secondary -> Tertiary -> Quaternary
        // According to requirements:
        // - state=Primary determines time to 2nd recipient (T2/R2)
        // - state=Secondary determines time to 3rd recipient (T3/R3)
        // - state=Tertiary determines time to 4th recipient (T4/R4)
        // - state=Quaternary determines time to 5th recipient (T5/R5)
        String[] states = {"Primary", "Secondary", "Tertiary", "Quaternary"};
        
        for (int i = 0; i < states.length; i++) {
            String state = states[i];
            
            // Find SEND rule for this state and alert type
            String sendKey = state + "|" + alertType;
            Map<String, RuleData> sendRulesForAlert = sendRulesByStateAndAlertType.get(sendKey);
            RuleData sendRule = (sendRulesForAlert != null) ? sendRulesForAlert.get(state) : null;
            
            // Find ESCALATE rule that has this state in its CONDITION (source state)
            RuleData escalateRule = escalateRulesBySourceState.get(state);
            
            // Recipient index based on state:
            // Primary state -> 1st recipient (R1)
            // Secondary state -> 2nd recipient (R2)
            // Tertiary state -> 3rd recipient (R3)
            // Quaternary state -> 4th recipient (R4)
            int recipientIndex = i + 1;
            
            if (sendRule != null) {
                // Extract recipient from send rule
                String recipient = extractRecipient(sendRule);
                setRecipient(flow, recipientIndex, recipient);
                
                // Set timing based on whether this is create or update
                if (sendRule.isCreate) {
                    // Primary state (create) uses T1 = Immediate
                    if (i == 0) {
                        flow.t1 = "Immediate";
                    }
                }
                
                // Apply settings from the send rule (only for the first matching state)
                if (flow.priorityRaw == null || flow.priorityRaw.isEmpty()) {
                    applySettings(flow, sendRule.settings);
                }
            }
            
            // Set timing from escalation rule
            // The escalate rule checks state=X and transitions to next state
            // According to requirements, state=X determines time to NEXT recipient
            // So: state=Primary (i=0) determines T2 (i+2)
            //     state=Secondary (i=1) determines T3 (i+2)
            //     state=Tertiary (i=2) determines T4 (i+2)
            //     state=Quaternary (i=3) determines T5 (i+2)
            if (escalateRule != null && escalateRule.deferDeliveryBy != null && !escalateRule.deferDeliveryBy.isEmpty()) {
                int timeIndex = i + 2; // Next recipient after current state
                setTime(flow, timeIndex, escalateRule.deferDeliveryBy);
            }
        }
        
        // Add to appropriate list
        if ("NurseCalls".equals(flow.type)) {
            nurseCalls.add(flow);
        } else if ("Orders".equals(flow.type)) {
            orders.add(flow);
        } else {
            clinicals.add(flow);
        }
    }
    
    /**
     * Extract recipient from a rule.
     */
    private String extractRecipient(RuleData rule) {
        String recipient = "";
        
        if (rule.settings.containsKey("destination")) {
            String destination = (String) rule.settings.get("destination");
            if (destination != null && !destination.isEmpty()) {
                if (destination.startsWith("g-")) {
                    recipient = destination;
                } else if (rule.role != null && !rule.role.isEmpty()) {
                    recipient = rule.role;
                } else {
                    // When no role is found, use the exact destination value from settings
                    recipient = destination;
                }
            }
        } else if (rule.role != null && !rule.role.isEmpty()) {
            recipient = rule.role;
        }
        
        return recipient;
    }
    
    /**
     * Set recipient at the specified index (1-5).
     */
    private void setRecipient(ExcelParserV5.FlowRow flow, int index, String recipient) {
        if (recipient == null || recipient.isEmpty()) {
            return;
        }
        
        switch (index) {
            case 1:
                flow.r1 = recipient;
                break;
            case 2:
                flow.r2 = recipient;
                break;
            case 3:
                flow.r3 = recipient;
                break;
            case 4:
                flow.r4 = recipient;
                break;
            case 5:
                flow.r5 = recipient;
                break;
        }
    }
    
    /**
     * Set time at the specified index (1-5).
     */
    private void setTime(ExcelParserV5.FlowRow flow, int index, String time) {
        if (time == null || time.isEmpty()) {
            return;
        }
        
        switch (index) {
            case 1:
                if (flow.t1 == null || flow.t1.isEmpty()) {
                    flow.t1 = time;
                }
                break;
            case 2:
                flow.t2 = time;
                break;
            case 3:
                flow.t3 = time;
                break;
            case 4:
                flow.t4 = time;
                break;
            case 5:
                flow.t5 = time;
                break;
        }
    }
    
    /**
     * Apply settings to a flow row.
     */
    private void applySettings(ExcelParserV5.FlowRow flow, Map<String, Object> settings) {
        if (settings.containsKey("priority")) {
            flow.priorityRaw = (String) settings.get("priority");
        }
        if (settings.containsKey("ttl")) {
            flow.ttlValue = (String) settings.get("ttl");
        }
        if (settings.containsKey("enunciate")) {
            flow.enunciate = (String) settings.get("enunciate");
        }
        if (settings.containsKey("overrideDND")) {
            flow.breakThroughDND = (String) settings.get("overrideDND");
        }
        if (settings.containsKey("displayValues")) {
            flow.responseOptions = (String) settings.get("displayValues");
        }
    }
    
    /**
     * Create flow rows from a single rule (non-escalation case).
     */
    private void createFlowRowsFromRule(RuleData rule) {
        createFlowRows(rule.dataset, rule.purpose, rule.component, rule.alertTypes,
                      rule.facilities, rule.units, rule.deferDeliveryBy, rule.isCreate,
                      rule.isUpdate, rule.settings, rule.role);
    }
    
    /**
     * Create flow rows from extracted data.
     */
    private void createFlowRows(String dataset, String purpose, String component,
                                Set<String> alertTypes, Set<String> facilities, Set<String> units,
                                String deferDeliveryBy, boolean isCreate, boolean isUpdate,
                                Map<String, Object> settings, String role) {
        
        // Track facility/unit combinations
        for (String facility : facilities) {
            facilityToUnits.computeIfAbsent(facility, k -> new LinkedHashSet<>()).addAll(units);
        }
        
        // If no alert types found, create a single row with the purpose
        if (alertTypes.isEmpty()) {
            alertTypes.add(purpose != null ? purpose : "");
        }
        
        // If no units found, use empty string
        if (units.isEmpty()) {
            units.add("");
        }
        
        // Determine recipient from destination or role
        String recipient = "";
        if (settings.containsKey("destination")) {
            String destination = (String) settings.get("destination");
            if (destination != null && !destination.isEmpty()) {
                // If destination starts with "g-", use it as the recipient
                if (destination.startsWith("g-")) {
                    recipient = destination;
                } else if (role != null && !role.isEmpty()) {
                    // Otherwise use the role name (functional role)
                    recipient = role;
                } else {
                    // When no role is found, use the exact destination value from settings
                    recipient = destination;
                }
            }
        } else if (role != null && !role.isEmpty()) {
            // If no destination, just use role
            recipient = role;
        }
        
        // Create a flow row for each alert type
        for (String alertType : alertTypes) {
            for (String unit : units) {
                ExcelParserV5.FlowRow flow = new ExcelParserV5.FlowRow();
                flow.inScope = true;
                
                // Determine flow type from dataset
                if (dataset != null) {
                    if (dataset.equalsIgnoreCase("NurseCalls") || dataset.contains("Nurse")) {
                        flow.type = "NurseCalls";
                    } else if (dataset.equalsIgnoreCase("Orders") || dataset.contains("Order")) {
                        flow.type = "Orders";
                    } else {
                        flow.type = "Clinicals";
                    }
                }
                
                // Config group: dataset + unit name
                if (dataset != null && !unit.isEmpty()) {
                    flow.configGroup = dataset + "_" + unit;
                } else if (dataset != null) {
                    flow.configGroup = dataset;
                } else {
                    flow.configGroup = "";
                }
                
                flow.alarmName = alertType;
                flow.sendingName = alertType;
                
                // Map component to device
                if (component != null) {
                    String deviceValue = mapComponentToDevice(component);
                    flow.deviceA = deviceValue;
                }
                
                // Set timing
                if (isCreate && deferDeliveryBy != null && !deferDeliveryBy.isEmpty()) {
                    flow.t1 = deferDeliveryBy;
                } else if (isCreate) {
                    flow.t1 = "Immediate";
                }
                
                if (isUpdate && deferDeliveryBy != null && !deferDeliveryBy.isEmpty()) {
                    flow.escalateAfter = deferDeliveryBy;
                }
                
                // Set recipient
                if (!recipient.isEmpty()) {
                    flow.r1 = recipient;
                }
                
                // Apply settings
                if (settings.containsKey("priority")) {
                    flow.priorityRaw = (String) settings.get("priority");
                }
                if (settings.containsKey("ttl")) {
                    flow.ttlValue = (String) settings.get("ttl");
                }
                if (settings.containsKey("enunciate")) {
                    flow.enunciate = (String) settings.get("enunciate");
                }
                if (settings.containsKey("overrideDND")) {
                    flow.breakThroughDND = (String) settings.get("overrideDND");
                }
                if (settings.containsKey("displayValues")) {
                    flow.responseOptions = (String) settings.get("displayValues");
                }
                
                // Add to appropriate list
                if ("NurseCalls".equals(flow.type)) {
                    nurseCalls.add(flow);
                } else if ("Orders".equals(flow.type)) {
                    orders.add(flow);
                } else {
                    clinicals.add(flow);
                }
            }
        }
    }
    
    /**
     * Map component attribute to device column value.
     */
    private String mapComponentToDevice(String component) {
        if (component == null) {
            return "";
        }
        
        switch (component.toUpperCase()) {
            case "VMP":
                return "VMP";
            case "DATAUPDATE":
                return "Edge";
            case "VOCERA":
                return "Vocera";
            case "XMPP":
                return "XMPP";
            default:
                return component;
        }
    }
    
    /**
     * Create unit rows from collected facility/unit data.
     */
    private void createUnitRows() {
        for (Map.Entry<String, Set<String>> entry : facilityToUnits.entrySet()) {
            String facility = entry.getKey();
            
            for (String unit : entry.getValue()) {
                ExcelParserV5.UnitRow unitRow = new ExcelParserV5.UnitRow();
                unitRow.facility = facility;
                unitRow.unitNames = unit;
                
                // Determine config groups based on which flows reference this unit
                Set<String> nurseGroups = new HashSet<>();
                Set<String> clinicalGroups = new HashSet<>();
                Set<String> orderGroups = new HashSet<>();
                
                for (ExcelParserV5.FlowRow flow : nurseCalls) {
                    if (flow.configGroup.contains(unit)) {
                        nurseGroups.add(flow.configGroup);
                    }
                }
                for (ExcelParserV5.FlowRow flow : clinicals) {
                    if (flow.configGroup.contains(unit)) {
                        clinicalGroups.add(flow.configGroup);
                    }
                }
                for (ExcelParserV5.FlowRow flow : orders) {
                    if (flow.configGroup.contains(unit)) {
                        orderGroups.add(flow.configGroup);
                    }
                }
                
                unitRow.nurseGroup = String.join(",", nurseGroups);
                unitRow.clinGroup = String.join(",", clinicalGroups);
                unitRow.ordersGroup = String.join(",", orderGroups);
                
                units.add(unitRow);
            }
        }
        
        // If no units were created, add a default one
        if (units.isEmpty()) {
            ExcelParserV5.UnitRow defaultUnit = new ExcelParserV5.UnitRow();
            defaultUnit.facility = "Default";
            defaultUnit.unitNames = "All Units";
            units.add(defaultUnit);
        }
    }
    
    /**
     * Get text content of a child element.
     */
    private String getElementText(Element parent, String tagName) {
        NodeList nodes = parent.getElementsByTagName(tagName);
        if (nodes.getLength() > 0) {
            return nodes.item(0).getTextContent().trim();
        }
        return null;
    }
    
    /**
     * Get first child element with given tag name.
     */
    private Element getFirstChildElement(Element parent, String tagName) {
        NodeList nodes = parent.getElementsByTagName(tagName);
        if (nodes.getLength() > 0) {
            return (Element) nodes.item(0);
        }
        return null;
    }
    
    /**
     * Clear all data.
     */
    private void clear() {
        units.clear();
        nurseCalls.clear();
        clinicals.clear();
        orders.clear();
        datasetViews.clear();
        facilityToUnits.clear();
        collectedRules.clear();
    }
    
    // Getters
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
        return String.format(Locale.ROOT,
            "✅ XML Load Complete%n%n" +
            "Loaded:%n" +
            "  • %d Unit rows%n" +
            "  • %d Nurse Call rows%n" +
            "  • %d Clinical rows%n" +
            "  • %d Orders rows",
            units.size(), nurseCalls.size(), clinicals.size(), orders.size());
    }
}
