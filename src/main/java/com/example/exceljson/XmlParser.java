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
     * Required logic across ALL datasets (NurseCalls, Clinicals, Orders):
     * - DataUpdate rules themselves are always processed
     * - For adapter/interface rules (e.g., VMP, Vocera, XMPP, CUCM):
     *   - If the adapter rule itself uses trigger-on create="true", it should be processed WITHOUT
     *     requiring a DataUpdate CREATE rule, as the adapter is performing the "Create" operation
     *   - Otherwise, do NOT process unless there exists at least one DataUpdate (create=true) rule in the SAME dataset
     *     whose alert filter (type or name) positively includes this rule's alert(s)
     *   - If the DataUpdate rule uses invalid relations (not_in, not_like, not_equal, null), skip it
     *   - If this adapter rule has no alert type or alert name in its own views, do NOT process
     */
    private boolean shouldProcessRule(Rule rule, List<Rule> allParsedRules) {
        // Always keep DataUpdate rules
        if ("DataUpdate".equalsIgnoreCase(rule.component)) {
            return true;
        }
        
        // Always keep escalation timing rules (have defer-delivery-by but no destination)
        // These apply globally across alert types and will be enriched with state info later
        if (rule.deferDeliveryBy != null && !hasDestination(rule)) {
            return true;
        }

        // NEW: Allow adapter rules (VMP, XMPP, CUCM, Vocera) with create="true" to function independently
        // When an adapter rule itself uses "Create" in the send rule, it acts as both the interface
        // and the data creation mechanism, so it does not require a separate DataUpdate CREATE rule
        if (rule.triggerCreate && isAdapterComponent(rule.component)) {
            return true;
        }

        // Collect DataUpdate(create=true) rules for the same dataset
        List<Rule> dataUpdateCreateRules = allParsedRules.stream()
            .filter(r -> "DataUpdate".equalsIgnoreCase(r.component))
            .filter(r -> r.dataset != null && r.dataset.equals(rule.dataset))
            .filter(r -> r.triggerCreate)
            .collect(Collectors.toList());
        
        // Also collect DataUpdate(update=true) rules that set states for escalation
        List<Rule> dataUpdateUpdateRules = allParsedRules.stream()
            .filter(r -> "DataUpdate".equalsIgnoreCase(r.component))
            .filter(r -> r.dataset != null && r.dataset.equals(rule.dataset))
            .filter(r -> r.triggerUpdate)
            .collect(Collectors.toList());
        
        // Combine both CREATE and UPDATE DataUpdate rules
        List<Rule> dataUpdateRules = new ArrayList<>();
        dataUpdateRules.addAll(dataUpdateCreateRules);
        dataUpdateRules.addAll(dataUpdateUpdateRules);

        // If there are no create DataUpdate rules in this dataset, do NOT process adapter rules
        if (dataUpdateCreateRules.isEmpty()) {
            return false;
        }

        // Extract alert identifiers (type and/or name) from the adapter rule's views
        Set<String> ruleAlertKeys = extractAlertKeysFromRule(rule);

        // If no alert identifiers are present on the adapter rule, do NOT process
        if (ruleAlertKeys.isEmpty()) {
            return false;
        }
        
        // Extract the state requirement from the adapter rule's views (if any)
        String requiredState = extractStateFromRule(rule);
        
        // For escalation SEND rules (Secondary/Tertiary/Quaternary states), allow them through
        // if there are ANY DataUpdate rules for the alert type, without strict state matching
        // These rules fire on state transitions managed by DataUpdate UPDATE rules
        boolean isEscalationSendRule = requiredState != null && !requiredState.isEmpty() &&
            !"Primary".equalsIgnoreCase(requiredState) && !"Group".equalsIgnoreCase(requiredState);

        // Validate that at least one DataUpdate rule positively covers these alerts with valid logic
        // IMPORTANT: If the adapter rule requires a specific state (e.g., Group), the DataUpdate rule
        // must set alerts to that state
        //
        // However, if NO DataUpdate CREATE rules set any state, we allow the adapter rule through for backward
        // compatibility (alerts may get default states from the application logic)
        // We only consider CREATE rules for state checking, not escalation UPDATE rules
        boolean anyDataUpdateCreateSetsState = dataUpdateCreateRules.stream()
            .anyMatch(r -> r.state != null && !r.state.isEmpty());
        
        for (Rule dataUpdateRule : dataUpdateRules) {
            // Check if alert types match
            if (!dataUpdateRuleCoversAlertKeys(dataUpdateRule, ruleAlertKeys)) {
                continue;
            }
            
            // Check if states match (if adapter rule requires a specific state)
            // For escalation SEND rules, skip state validation - they just need DataUpdate coverage
            if (!isEscalationSendRule && requiredState != null && !requiredState.isEmpty() && anyDataUpdateCreateSetsState) {
                // Adapter rule requires a specific state, and DataUpdate CREATE rules DO set states
                // So we need to ensure state matching - but only check CREATE rules, not escalation UPDATE rules
                if (dataUpdateRule.triggerCreate) {
                    if (dataUpdateRule.state == null || !requiredState.equals(dataUpdateRule.state)) {
                        // DataUpdate CREATE rule doesn't set the required state
                        continue;
                    }
                } else {
                    // This is an UPDATE/escalation rule - skip state checking for these
                    // They don't set the initial state, they transition between states
                    continue;
                }
            }
            
            // Both alert types and states match (or no state requirement, or no DataUpdate sets states, or is escalation rule)
            return true;
        }

        // No valid DataUpdate rule found that covers this adapter rule's alerts AND sets the required state
        return false;
    }
    
    /**
     * Extract the state requirement from an adapter rule's condition views.
     * Returns the state value if found, or null if no state requirement.
     */
    private String extractStateFromRule(Rule rule) {
        if (rule == null || rule.dataset == null) return null;
        Map<String, View> views = datasetViews.get(rule.dataset);
        if (views == null) return null;

        for (String viewName : rule.viewNames) {
            View view = views.get(viewName);
            if (view == null || view.filters == null) continue;
            for (Filter filter : view.filters) {
                String path = filter.path == null ? "" : filter.path.trim();
                if ("state".equals(path)) {
                    return filter.value == null ? null : filter.value.trim();
                }
            }
        }
        return null;
    }

    /**
     * Extract alert identifiers (alert_type and/or alert name) from a rule's condition views.
     * Returns a set of alert keys that must be positively covered by a DataUpdate(create) rule.
     */
    private Set<String> extractAlertKeysFromRule(Rule rule) {
        Set<String> keys = new LinkedHashSet<>();
        if (rule == null || rule.dataset == null) return keys;
        Map<String, View> views = datasetViews.get(rule.dataset);
        if (views == null) return keys;

        for (String viewName : rule.viewNames) {
            View view = views.get(viewName);
            if (view == null || view.filters == null) continue;
            for (Filter filter : view.filters) {
                String path = filter.path == null ? "" : filter.path.trim();
                if (isAlertTypePath(path) || isAlertNamePath(path)) {
                    keys.addAll(parseListValues(filter.value));
                }
            }
        }
        return keys;
    }

    /**
     * Heuristic check if a filter path refers to an alert name (not facility/unit/role names).
     * Matches common patterns like "alert.name", "*.alert_name", or exactly "name" within alert views.
     */
    private boolean isAlertNamePath(String path) {
        if (path == null) return false;
        String p = path.toLowerCase();
        // Must reference both alert and name, or be a bare name
        boolean looksLikeAlertName = (p.contains("alert") && p.contains("name")) || p.equals("name");
        if (!looksLikeAlertName) return false;

        // Exclude common non-alert name paths
        if (p.contains("facility.name") || p.contains("unit.name") || p.contains("role.name") || p.contains("group.name")
            || p.contains("usr.") || p.contains("device.") || p.contains("provider.") || p.contains("patient.")
            || p.contains("room.") || p.contains("place.") || p.contains("location.") ) {
            return false;
        }
        return true;
    }

    /**
     * Parse a comma-separated list value into a normalized set (trimmed, non-empty).
     */
    private Set<String> parseListValues(String value) {
        Set<String> out = new LinkedHashSet<>();
        if (value == null) return out;
        for (String part : value.split(",")) {
            String t = part == null ? "" : part.trim();
            if (!t.isEmpty()) out.add(t);
        }
        return out;
    }

    /**
     * Check if a DataUpdate(create) rule covers the given alert keys using valid positive logic.
     * 
     * IMPORTANT: A DataUpdate rule covers an alert type if AND ONLY IF **ALL** alert type/name filters
     * in the rule's views positively match the alert. This ensures that rules with multiple "not_in"
     * filters (e.g., "NOT codes AND NOT OT alerts") are correctly evaluated.
     * 
     * Logic:
     * - Collect ALL alert type/name filters from ALL views in the rule
     * - If ANY filter uses invalid logic (except not_in), reject the entire rule
     * - ALL filters must agree that they cover the target alerts (positive match)
     * - If any filter excludes the alert, the rule does NOT cover it
     */
    private boolean dataUpdateRuleCoversAlertKeys(Rule dataUpdateRule, Set<String> targetAlertKeys) {
        if (dataUpdateRule == null || dataUpdateRule.dataset == null || !datasetViews.containsKey(dataUpdateRule.dataset)) {
            return false;
        }

        Map<String, View> views = datasetViews.get(dataUpdateRule.dataset);
        
        // Collect ALL alert type filters from ALL views
        List<AlertTypeFilter> alertFilters = new ArrayList<>();
        
        for (String viewName : dataUpdateRule.viewNames) {
            View view = views.get(viewName);
            if (view == null || view.filters == null) continue;
            for (Filter filter : view.filters) {
                String path = filter.path == null ? "" : filter.path.trim();
                if (isAlertTypePath(path) || isAlertNamePath(path)) {
                    // Reject invalid relations immediately
                    if (hasInvalidLogic(filter.relation)) {
                        return false;
                    }
                    alertFilters.add(new AlertTypeFilter(filter.relation, parseListValues(filter.value)));
                }
            }
        }
        
        // If no alert type filters found, rule doesn't specifically target any alerts
        if (alertFilters.isEmpty()) {
            return false;
        }
        
        // ALL filters must agree that they cover at least one of the target alerts
        for (String targetAlert : targetAlertKeys) {
            boolean allFiltersCoverThis = true;
            for (AlertTypeFilter filter : alertFilters) {
                if (!filterCoversAlert(filter.relation, filter.values, targetAlert)) {
                    allFiltersCoverThis = false;
                    break;
                }
            }
            // If all filters agree on covering this alert, the rule covers it
            if (allFiltersCoverThis) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Helper class to store alert type filter information
     */
    private static class AlertTypeFilter {
        String relation;
        Set<String> values;
        
        AlertTypeFilter(String relation, Set<String> values) {
            this.relation = relation;
            this.values = values;
        }
    }
    
    /**
     * Check if a single alert type filter covers a specific alert.
     * 
     * For "in" and "equal": alert is covered if it's IN the list
     * For "not_in": alert is covered if it's NOT IN the exclusion list
     */
    private boolean filterCoversAlert(String relation, Set<String> filterValues, String targetAlert) {
        if (relation == null || filterValues.isEmpty() || targetAlert == null) {
            return false;
        }
        
        switch (relation.toLowerCase()) {
            case "in":
            case "equal":
                return filterValues.contains(targetAlert);
                
            case "not_in":
                return !filterValues.contains(targetAlert);
                
            default:
                return false;
        }
    }
    
    /**
     * Extract alert types from a rule's condition views
     */
    private Set<String> extractAlertTypesFromRule(Rule rule) {
        Set<String> alertTypes = new HashSet<>();
        
        if (rule.dataset == null || !datasetViews.containsKey(rule.dataset)) {
            return alertTypes;
        }
        
        Map<String, View> views = datasetViews.get(rule.dataset);
        
        for (String viewName : rule.viewNames) {
            View view = views.get(viewName);
            if (view != null) {
                for (Filter filter : view.filters) {
                    if (isAlertTypePath(filter.path)) {
                        alertTypes.addAll(parseAlertTypeValues(filter.value));
                    }
                }
            }
        }
        
        return alertTypes;
    }
    
    /**
     * Check if a DataUpdate rule covers the given alert types with valid logic
     */
    private boolean dataUpdateRuleCoversAlertTypes(Rule dataUpdateRule, Set<String> targetAlertTypes) {
        if (dataUpdateRule.dataset == null || !datasetViews.containsKey(dataUpdateRule.dataset)) {
            return false;
        }
        
        Map<String, View> views = datasetViews.get(dataUpdateRule.dataset);
        
        for (String viewName : dataUpdateRule.viewNames) {
            View view = views.get(viewName);
            if (view != null) {
                for (Filter filter : view.filters) {
                    if (isAlertTypePath(filter.path)) {
                        // Check if this filter uses invalid logic
                        if (hasInvalidLogic(filter.relation)) {
                            return false;
                        }
                        
                        // Check if this filter covers the target alert types
                        Set<String> dataUpdateAlertTypes = parseAlertTypeValues(filter.value);
                        if (coversAlertTypes(filter.relation, dataUpdateAlertTypes, targetAlertTypes)) {
                            return true;
                        }
                    }
                }
            }
        }
        
        return false;
    }
    
    /**
     * Check if a filter path refers to alert type
     */
    private boolean isAlertTypePath(String path) {
        return path != null && path.toLowerCase().contains("alert_type");
    }
    
    /**
     * Check if the relation uses invalid logic that should exclude the rule.
     * Note: not_in is now supported and is NOT considered invalid logic.
     */
    private boolean hasInvalidLogic(String relation) {
        if (relation == null) return false;
        
        String lowerRelation = relation.toLowerCase();
        // not_in is now supported, so we exclude it from invalid logic check
        if (lowerRelation.equals("not_in")) {
            return false;
        }
        return lowerRelation.contains("not") || 
               lowerRelation.equals("null") ||
               lowerRelation.contains("not_like") ||
               lowerRelation.contains("not_equal");
    }
    
    /**
     * Parse alert type values from filter value (handles comma-separated values)
     */
    private Set<String> parseAlertTypeValues(String value) {
        Set<String> alertTypes = new HashSet<>();
        if (value != null && !value.trim().isEmpty()) {
            String[] parts = value.split(",");
            for (String part : parts) {
                String trimmed = part.trim();
                if (!trimmed.isEmpty()) {
                    alertTypes.add(trimmed);
                }
            }
        }
        return alertTypes;
    }
    
    /**
     * Check if a DataUpdate filter covers the target alert types.
     * 
     * For "in" and "equal" relations: alert is covered if it's IN the list
     * For "not_in" relation: alert is covered if it's NOT IN the exclusion list
     */
    private boolean coversAlertTypes(String relation, Set<String> dataUpdateAlertTypes, Set<String> targetAlertTypes) {
        if (relation == null || dataUpdateAlertTypes.isEmpty() || targetAlertTypes.isEmpty()) {
            return false;
        }
        
        switch (relation.toLowerCase()) {
            case "in":
            case "equal":
                // Check if any target alert type is covered (positive match)
                for (String targetType : targetAlertTypes) {
                    if (dataUpdateAlertTypes.contains(targetType)) {
                        return true;
                    }
                }
                return false;
                
            case "not_in":
                // For not_in: alert is covered if it's NOT in the exclusion list
                // Check if any target alert type is NOT in the exclusion list
                for (String targetType : targetAlertTypes) {
                    if (!dataUpdateAlertTypes.contains(targetType)) {
                        return true;
                    }
                }
                return false;
                
            default:
                // For other relations, assume they don't provide valid coverage
                return false;
        }
    }
    
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
            // For DataUpdate rules (both CREATE and UPDATE), extract the state they set from settings
            if ("DataUpdate".equalsIgnoreCase(component) && rule.settings.containsKey("state")) {
                rule.state = rule.settings.get("state").toString();
            }
        }
        
        return rule;
    }
    
    /**
     * Enrich rules with data from views
     */
    private void enrichRulesWithViews() {
        // Collect all DataUpdate (create=true) rules grouped by dataset for filtering
        Map<String, List<Rule>> dataUpdateByDataset = new HashMap<>();
        for (Rule rule : allRules) {
            if ("DataUpdate".equalsIgnoreCase(rule.component) && rule.triggerCreate) {
                dataUpdateByDataset.computeIfAbsent(rule.dataset, k -> new ArrayList<>()).add(rule);
            }
        }
        
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
            
            // Filter alert types: keep only those covered by CREATE DATAUPDATE rules
            // Skip this filtering for:
            // 1. DataUpdate rules themselves
            // 2. Escalation timing rules (defer-delivery-by without destination)
            // 3. Escalation SEND rules (have state that's not Primary/Group and are UPDATE-triggered)
            boolean isEscalationSendRule = rule.state != null && !rule.state.isEmpty() &&
                !"Primary".equalsIgnoreCase(rule.state) && !"Group".equalsIgnoreCase(rule.state) &&
                rule.triggerUpdate && hasDestination(rule);
            
            if (!"DataUpdate".equalsIgnoreCase(rule.component) && 
                !(rule.deferDeliveryBy != null && !hasDestination(rule)) &&
                !isEscalationSendRule) {
                filterUncoveredAlertTypes(rule, dataUpdateByDataset.getOrDefault(rule.dataset, Collections.emptyList()));
            }
        }
    }
    
    /**
     * Remove alert types from a rule that are not covered by any CREATE DATAUPDATE rule.
     * Each alert type must be individually validated against the DataUpdate rules.
     */
    private void filterUncoveredAlertTypes(Rule rule, List<Rule> dataUpdateRules) {
        if (rule.alertTypes.isEmpty() || dataUpdateRules.isEmpty()) {
            return;
        }
        
        // Keep only alert types that are covered by at least one DataUpdate rule
        Set<String> coveredAlertTypes = new HashSet<>();
        for (String alertType : rule.alertTypes) {
            for (Rule dataUpdateRule : dataUpdateRules) {
                if (dataUpdateRuleCoversAlertType(dataUpdateRule, alertType)) {
                    coveredAlertTypes.add(alertType);
                    break;
                }
            }
        }
        
        // Replace the alert types set with only covered ones
        rule.alertTypes = coveredAlertTypes;
    }
    
    /**
     * Check if a single alert type is covered by a DataUpdate rule.
     * 
     * For "in" and "equal" relations: alert is covered if it's in the list
     * For "not_in" relation: alert is covered if it's NOT in the exclusion list
     */
    private boolean dataUpdateRuleCoversAlertType(Rule dataUpdateRule, String alertType) {
        if (dataUpdateRule == null || dataUpdateRule.dataset == null || !datasetViews.containsKey(dataUpdateRule.dataset)) {
            return false;
        }
        
        Map<String, View> views = datasetViews.get(dataUpdateRule.dataset);
        for (String viewName : dataUpdateRule.viewNames) {
            View view = views.get(viewName);
            if (view == null || view.filters == null) continue;
            for (Filter filter : view.filters) {
                String path = filter.path == null ? "" : filter.path.trim();
                if (isAlertTypePath(path) || isAlertNamePath(path)) {
                    // Reject invalid relations immediately
                    if (hasInvalidLogic(filter.relation)) {
                        return false;
                    }
                    
                    Set<String> values = parseListValues(filter.value);
                    String relation = filter.relation == null ? "" : filter.relation.toLowerCase();
                    
                    // For not_in: alert is covered if it's NOT in the exclusion list
                    if (relation.equals("not_in")) {
                        if (!values.contains(alertType)) {
                            return true;
                        }
                    } else {
                        // For in/equal: alert is covered if it's in the list
                        if (values.contains(alertType)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
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
        
        // State - For DataUpdate rules, don't overwrite state from settings with state from views
        // DataUpdate rules: state in settings = what they SET, state in views = what they REQUIRE
        // Other rules (VMP, etc.): state in views = what they REQUIRE to trigger
        // EXCEPTION: For DataUpdate escalation rules (update with defer-delivery-by but no destination),
        // we DO need the state from views to know when to apply the timing
        if (filter.path.equals("state")) {
            if (!"DataUpdate".equalsIgnoreCase(rule.component)) {
                // Non-DataUpdate rules: state from view is what they require
                rule.state = filter.value.trim();
            } else if (rule.triggerUpdate && rule.deferDeliveryBy != null) {
                // DataUpdate escalation timing rules: state from view indicates when to apply timing
                rule.state = filter.value.trim();
            }
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
        
        // Track CREATE DATAUPDATE rules by dataset + alert type for facility extraction
        Map<String, List<Rule>> dataUpdateRulesByAlertType = new HashMap<>();
        
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
            
            // Track DataUpdate CREATE rules for facility extraction
            if ("DataUpdate".equalsIgnoreCase(rule.component) && rule.triggerCreate) {
                for (String alertType : rule.alertTypes) {
                    String key = rule.dataset + "|" + alertType;
                    dataUpdateRulesByAlertType.computeIfAbsent(key, k -> new ArrayList<>()).add(rule);
                }
            }
            
            // Skip DataUpdate rules that have destinations (they're interface-like, not escalation timing)
            // But KEEP DataUpdate rules with defer-delivery-by and no destination (escalation timing rules)
            if ("DataUpdate".equalsIgnoreCase(rule.component)) {
                // Keep escalation timing rules (have defer-delivery-by but no destination)
                if (rule.deferDeliveryBy == null || hasDestination(rule)) {
                    continue; // Skip non-escalation DataUpdate rules
                }
                // Fall through to add escalation timing DataUpdate rules to grouped
            }
            
            // Skip other rules without alert types
            if (rule.alertTypes.isEmpty()) continue;
            
            // Group by dataset + alert types + facility + units (for config group separation)
            // NEW: Separate by individual facility instead of grouping all facilities together
            for (String alertType : rule.alertTypes) {
                // Sort units for consistent grouping
                String unitsKey = rule.units.stream().sorted().collect(java.util.stream.Collectors.joining(","));
                
                // NEW: Create separate groups for each facility instead of merging them
                if (rule.facilities.isEmpty()) {
                    // No facilities specified in SEND rule - check DataUpdate CREATE rules for facilities
                    String dataUpdateKey = rule.dataset + "|" + alertType;
                    List<Rule> dataUpdateRules = dataUpdateRulesByAlertType.getOrDefault(dataUpdateKey, Collections.emptyList());
                    
                    Set<String> facilitiesFromDataUpdate = new LinkedHashSet<>();
                    for (Rule duRule : dataUpdateRules) {
                        facilitiesFromDataUpdate.addAll(duRule.facilities);
                    }
                    
                    if (facilitiesFromDataUpdate.isEmpty()) {
                        // No facilities from DataUpdate either - create a single group with empty facility
                        String key = rule.dataset + "|" + alertType + "||" + unitsKey;
                        grouped.computeIfAbsent(key, k -> new ArrayList<>()).add(rule);
                    } else {
                        // Use facilities from DataUpdate CREATE rules - create separate group for each
                        for (String facility : facilitiesFromDataUpdate) {
                            String key = rule.dataset + "|" + alertType + "|" + facility + "|" + unitsKey;
                            grouped.computeIfAbsent(key, k -> new ArrayList<>()).add(rule);
                        }
                    }
                } else {
                    // Create a separate group for each facility in the rule
                    for (String facility : rule.facilities) {
                        String key = rule.dataset + "|" + alertType + "|" + facility + "|" + unitsKey;
                        grouped.computeIfAbsent(key, k -> new ArrayList<>()).add(rule);
                    }
                }
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
            String facilityFromKey = parts.length > 2 ? parts[2] : ""; // Extract facility from group key
            List<Rule> rules = entry.getValue();
            
            // Get corresponding CREATE DATAUPDATE rules for this alert type
            String dataUpdateKey = dataset + "|" + alertType;
            List<Rule> dataUpdateRules = dataUpdateRulesByAlertType.getOrDefault(dataUpdateKey, Collections.emptyList());
            
            // Check if this is an escalation group
            boolean hasEscalation = rules.stream().anyMatch(r -> r.state != null && !r.state.isEmpty());
            
            if (hasEscalation) {
                createEscalationFlow(dataset, alertType, facilityFromKey, rules, dataUpdateRules);
            } else {
                createSimpleFlow(dataset, alertType, facilityFromKey, rules, dataUpdateRules);
            }
        }
    }
    
    /**
     * Create flow row with escalation
     */
    private void createEscalationFlow(String dataset, String alertType, String facilityFromKey, List<Rule> rules, List<Rule> dataUpdateRules) {
        // Separate send rules and escalate rules
        // Changed from Map<String, Rule> to Map<String, List<Rule>> to support multiple SEND rules at same state
        Map<String, List<Rule>> sendByState = new HashMap<>();
        Map<String, String> escalateDelay = new HashMap<>();
        String initialDelay = null; // Delay before first state (Primary)
        boolean hasCreateRuleWithoutDelay = false; // Track if there's a CREATE rule without delay (Immediate)
        
        // Check DataUpdate CREATE rules for initial delay
        // Only consider rules that explicitly match THIS alert type
        for (Rule dataUpdateRule : dataUpdateRules) {
            if (dataUpdateRule.triggerCreate && dataUpdateRule.alertTypes.contains(alertType)) {
                if (dataUpdateRule.deferDeliveryBy != null) {
                    initialDelay = dataUpdateRule.deferDeliveryBy;
                    break; // Use first one found
                } else {
                    // Found a CREATE rule without delay - this means Immediate
                    hasCreateRuleWithoutDelay = true;
                    break;
                }
            }
        }
        
        for (Rule rule : rules) {
            if (rule.state == null || rule.state.isEmpty()) continue;
            
            String state = normalizeState(rule.state);
            
            // If has destination, it's a send rule
            if (hasDestination(rule)) {
                sendByState.computeIfAbsent(state, k -> new ArrayList<>()).add(rule);
            }
            // If has defer-delivery-by but no destination, it's escalation timing
            else if (rule.deferDeliveryBy != null && !hasDestination(rule)) {
                escalateDelay.put(state, rule.deferDeliveryBy);
            }
        }
        
        if (sendByState.isEmpty()) return;
        
        // Check how many unique states have SEND rules
        int numStates = sendByState.size();
        
        // Check if any state has multiple SEND rules
        boolean hasMultipleRulesAtSameState = sendByState.values().stream().anyMatch(list -> list.size() > 1);
        
        // If there's only ONE state and it has multiple SEND rules, create separate single-level flows
        if (numStates == 1 && hasMultipleRulesAtSameState) {
            // Single state (e.g., "Group"/"Primary") with multiple SEND rules
            // Create a separate flow for each SEND rule
            String singleState = sendByState.keySet().iterator().next();
            List<Rule> sendRules = sendByState.get(singleState);
            
            for (Rule sendRule : sendRules) {
                createSimpleFlowFromRule(dataset, alertType, facilityFromKey, sendRule, dataUpdateRules, initialDelay, hasCreateRuleWithoutDelay);
            }
            return;
        }
        
        // If there are multiple states AND one state has multiple rules,
        // we need to create multiple escalation chains (one for each rule at the multi-rule state)
        if (numStates > 1 && hasMultipleRulesAtSameState) {
            // Find which state has multiple rules
            String multiRuleState = null;
            List<Rule> multiRules = null;
            for (Map.Entry<String, List<Rule>> entry : sendByState.entrySet()) {
                if (entry.getValue().size() > 1) {
                    multiRuleState = entry.getKey();
                    multiRules = entry.getValue();
                    break;
                }
            }
            
            // Create separate escalation chains for each rule at the multi-rule state
            for (Rule multiRule : multiRules) {
                // Build a modified sendByState map with only this rule for the multi-rule state
                Map<String, List<Rule>> modifiedSendByState = new HashMap<>();
                for (Map.Entry<String, List<Rule>> entry : sendByState.entrySet()) {
                    if (entry.getKey().equals(multiRuleState)) {
                        modifiedSendByState.put(entry.getKey(), List.of(multiRule));
                    } else {
                        modifiedSendByState.put(entry.getKey(), entry.getValue());
                    }
                }
                
                // Create escalation flow with this modified map
                createEscalationFlowFromMap(dataset, alertType, facilityFromKey, modifiedSendByState, escalateDelay, initialDelay, hasCreateRuleWithoutDelay, dataUpdateRules, rules);
            }
            return;
        }
        
        // Otherwise, create a normal single escalation flow
        createEscalationFlowFromMap(dataset, alertType, facilityFromKey, sendByState, escalateDelay, initialDelay, hasCreateRuleWithoutDelay, dataUpdateRules, rules);
    }
    
    /**
     * Create an escalation flow from the sendByState map
     */
    private void createEscalationFlowFromMap(String dataset, String alertType,
                                             String facilityFromKey,
                                             Map<String, List<Rule>> sendByState,
                                             Map<String, String> escalateDelay,
                                             String initialDelay,
                                             boolean hasCreateRuleWithoutDelay,
                                             List<Rule> dataUpdateRules,
                                             List<Rule> allRules) {
        // Build a template flow with common fields and recipients/timing
        ExcelParserV5.FlowRow template = new ExcelParserV5.FlowRow();
        template.inScope = true;
        template.type = normalizeDataset(dataset);
        template.alarmName = alertType;
        template.sendingName = alertType;

        // Get reference rule for common fields (use first rule from first state)
        Rule refRule = sendByState.values().iterator().next().get(0);
        template.deviceA = mapComponent(refRule.component);

        // Set initial timing (T1) based on CREATE rules
        // This should be set regardless of whether there's a SEND rule at Primary state
        if (hasCreateRuleWithoutDelay) {
            template.t1 = "Immediate";
        } else if (initialDelay != null) {
            template.t1 = initialDelay;
        } else {
            // Default to Immediate if not specified
            template.t1 = "Immediate";
        }

        // Map states to recipients and timing on the template
        String[] states = {"Primary", "Secondary", "Tertiary", "Quaternary", "Quinary"};
        for (int i = 0; i < states.length; i++) {
            String state = states[i];
            List<Rule> sendRules = sendByState.get(state);

            if (sendRules != null && !sendRules.isEmpty()) {
                // Use first rule for this state
                Rule sendRule = sendRules.get(0);
                String recipient = extractDestination(sendRule);
                setRecipient(template, i + 1, recipient, sendRule.roleFromView);

                // Apply settings from first send rule at Primary state
                if (i == 0) {
                    applySettings(template, sendRule);
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
        // Use facility from group key to ensure config group separation
        Set<String> facs = new LinkedHashSet<>();
        Set<String> uns = new LinkedHashSet<>();
        
        // NEW: Use facility from group key to separate by config group
        if (facilityFromKey != null && !facilityFromKey.isEmpty()) {
            facs.add(facilityFromKey);
        }
        
        // Collect units from the current group's rules
        Set<String> groupUnits = new LinkedHashSet<>();
        for (Rule r : allRules) {
            groupUnits.addAll(r.units);
        }
        
        // Filter DataUpdate CREATE rules to match the facility from key
        for (Rule r : dataUpdateRules) {
            // Only include units from DataUpdate rules that match this facility
            if (facilityFromKey.isEmpty() || r.facilities.isEmpty() || r.facilities.contains(facilityFromKey)) {
                if (!groupUnits.isEmpty() && !r.units.isEmpty()) {
                    Set<String> intersection = new LinkedHashSet<>(r.units);
                    intersection.retainAll(groupUnits);
                    uns.addAll(intersection);
                } else if (groupUnits.isEmpty()) {
                    uns.addAll(r.units);
                }
            }
        }
        
        // If no units found after filtering, use from group rules directly
        if (uns.isEmpty()) {
            uns.addAll(groupUnits);
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
     * Create a simple flow from a single rule (used for single-state with multiple SEND rules)
     */
    private void createSimpleFlowFromRule(String dataset, String alertType, String facilityFromKey, Rule sendRule, List<Rule> dataUpdateRules, String initialDelay, boolean hasCreateRuleWithoutDelay) {
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

        // Determine timing - prioritize CREATE rule without delay (Immediate)
        if (hasCreateRuleWithoutDelay) {
            template.t1 = "Immediate";
        } else if (initialDelay != null) {
            template.t1 = initialDelay;
        } else if (sendRule.triggerCreate) {
            template.t1 = sendRule.deferDeliveryBy != null ? sendRule.deferDeliveryBy : "Immediate";
        } else if (template.t1 == null || template.t1.isEmpty()) {
            // Default to Immediate if not specified
            template.t1 = "Immediate";
        }

        applySettings(template, sendRule);

        // Compute unions for splitting
        // Use facility from group key to ensure config group separation
        Set<String> facs = new LinkedHashSet<>();
        Set<String> uns = new LinkedHashSet<>();
        
        // NEW: Use facility from group key to separate by config group
        if (facilityFromKey != null && !facilityFromKey.isEmpty()) {
            facs.add(facilityFromKey);
        }
        
        // Collect units from the SEND rule
        Set<String> sendRuleUnits = new LinkedHashSet<>(sendRule.units);
        
        // Filter DataUpdate CREATE rules to match the facility from key
        for (Rule r : dataUpdateRules) {
            // Only include units from DataUpdate rules that match this facility
            if (facilityFromKey.isEmpty() || r.facilities.isEmpty() || r.facilities.contains(facilityFromKey)) {
                if (!sendRuleUnits.isEmpty() && !r.units.isEmpty()) {
                    Set<String> intersection = new LinkedHashSet<>(r.units);
                    intersection.retainAll(sendRuleUnits);
                    uns.addAll(intersection);
                } else if (sendRuleUnits.isEmpty()) {
                    uns.addAll(r.units);
                }
            }
        }
        
        // If no units found after filtering, use from sendRule directly
        if (uns.isEmpty()) {
            uns.addAll(sendRuleUnits);
        }
        
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
     * Create simple flow row (no escalation)
     */
    private void createSimpleFlow(String dataset, String alertType, String facilityFromKey, List<Rule> rules, List<Rule> dataUpdateRules) {
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

        // Check for initial delay from CREATE rules
        String initialDelay = null;
        boolean hasCreateRuleWithoutDelay = false;
        
        // First check the send rule itself
        if (sendRule.triggerCreate) {
            if (sendRule.deferDeliveryBy != null) {
                initialDelay = sendRule.deferDeliveryBy;
            } else {
                hasCreateRuleWithoutDelay = true;
            }
        }
        
        // If send rule doesn't have CREATE, check DataUpdate CREATE rules
        if (initialDelay == null && !hasCreateRuleWithoutDelay) {
            for (Rule dataUpdateRule : dataUpdateRules) {
                if (dataUpdateRule.triggerCreate && dataUpdateRule.alertTypes.contains(alertType)) {
                    if (dataUpdateRule.deferDeliveryBy != null) {
                        initialDelay = dataUpdateRule.deferDeliveryBy;
                        break;
                    } else {
                        hasCreateRuleWithoutDelay = true;
                        break;
                    }
                }
            }
        }
        
        // Set T1 based on what we found
        if (hasCreateRuleWithoutDelay) {
            template.t1 = "Immediate";
        } else if (initialDelay != null) {
            template.t1 = initialDelay;
        } else {
            template.t1 = "Immediate";
        }

        applySettings(template, sendRule);

        // Compute unions for splitting
        // Use facility from group key to ensure config group separation
        Set<String> facs = new LinkedHashSet<>();
        Set<String> uns = new LinkedHashSet<>();
        
        // NEW: Use facility from group key to separate by config group
        if (facilityFromKey != null && !facilityFromKey.isEmpty()) {
            facs.add(facilityFromKey);
        }
        
        // Collect units from the current group's rules
        Set<String> groupUnits = new LinkedHashSet<>();
        for (Rule r : rules) {
            groupUnits.addAll(r.units);
        }
        
        // Filter DataUpdate CREATE rules to match the facility from key
        for (Rule r : dataUpdateRules) {
            // Only include units from DataUpdate rules that match this facility
            if (facilityFromKey.isEmpty() || r.facilities.isEmpty() || r.facilities.contains(facilityFromKey)) {
                if (!groupUnits.isEmpty() && !r.units.isEmpty()) {
                    Set<String> intersection = new LinkedHashSet<>(r.units);
                    intersection.retainAll(groupUnits);
                    uns.addAll(intersection);
                } else if (groupUnits.isEmpty()) {
                    uns.addAll(r.units);
                }
            }
        }
        
        // If no units found after filtering, use from group rules directly
        if (uns.isEmpty()) {
            uns.addAll(groupUnits);
        }
        
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
    
    /**
     * Check if a component is an adapter/interface component (VMP, XMPP, CUCM, Vocera).
     * These are components that can send messages/alerts to external systems.
     */
    private boolean isAdapterComponent(String component) {
        if (component == null) return false;
        String upper = component.toUpperCase();
        return upper.equals("VMP") || 
               upper.equals("XMPP") || 
               upper.equals("CUCM") || 
               upper.equals("VOCERA") ||
               upper.equals("OUTGOINGWCTP");
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
            
            // Extract state from DataUpdate CREATE rule parameters
            if (root.has("parameters") && root.get("parameters").isArray()) {
                for (JsonNode param : root.get("parameters")) {
                    if (param.has("path") && param.has("value")) {
                        String path = param.get("path").asText();
                        if ("state".equals(path)) {
                            String value = param.get("value").asText();
                            result.put("state", value);
                            break;
                        }
                    }
                }
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
