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
    private final Map<String, String> canonicalAlertNames = new HashMap<>();
    
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
        
        // Step 4.5: Merge flows with overlapping units in escalation chains
        mergeOverlappingEscalationFlows();
        
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
        // Don't process DataUpdate rules whose purpose starts with "RESET"
        if ("DataUpdate".equalsIgnoreCase(rule.component)) {
            if (rule.purpose != null && rule.purpose.toUpperCase().startsWith("RESET")) {
                return false;
            }
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
        // Only include ACTIVE rules
        List<Rule> dataUpdateCreateRules = allParsedRules.stream()
            .filter(r -> "DataUpdate".equalsIgnoreCase(r.component))
            .filter(r -> r.dataset != null && r.dataset.equals(rule.dataset))
            .filter(r -> r.triggerCreate)
            .filter(r -> r.isActive)
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
        
        // Extract facilities and units from the adapter rule's views
        Set<String> ruleFacilities = extractFacilitiesFromRule(rule);
        Set<String> ruleUnits = extractUnitsFromRule(rule);
        
        for (Rule dataUpdateRule : dataUpdateRules) {
            // Check if alert types match
            if (!dataUpdateRuleCoversAlertKeys(dataUpdateRule, ruleAlertKeys)) {
                continue;
            }
            
            // Check if facilities match (if adapter rule has facility filters)
            if (!ruleFacilities.isEmpty() && !dataUpdateRuleCoversFacilities(dataUpdateRule, ruleFacilities)) {
                continue;
            }
            
            // Check if units match (if adapter rule has unit filters)
            if (!ruleUnits.isEmpty() && !dataUpdateRuleCoversUnits(dataUpdateRule, ruleUnits)) {
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
            
            // Alert types, facilities, units, and states all match
            return true;
        }

        // No valid DataUpdate rule found that covers this adapter rule's alerts, facilities, units, AND sets the required state
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
                    // Normalize alert names to lowercase for case-insensitive matching
                    Set<String> values = parseListValues(filter.value);
                    for (String val : values) {
                        keys.add(val.toLowerCase());
                    }
                }
            }
        }
        return keys;
    }

    /**
     * Extract facility names from a rule's condition views.
     * Returns a set of facility names that must be positively covered by a DataUpdate(create) rule.
     */
    private Set<String> extractFacilitiesFromRule(Rule rule) {
        Set<String> facilities = new LinkedHashSet<>();
        if (rule == null || rule.dataset == null) return facilities;
        Map<String, View> views = datasetViews.get(rule.dataset);
        if (views == null) return facilities;

        for (String viewName : rule.viewNames) {
            View view = views.get(viewName);
            if (view == null || view.filters == null) continue;
            for (Filter filter : view.filters) {
                String path = filter.path == null ? "" : filter.path.trim();
                if (isFacilityPath(path)) {
                    // Normalize facility names to lowercase for case-insensitive matching
                    Set<String> values = parseListValues(filter.value);
                    for (String val : values) {
                        facilities.add(val.toLowerCase());
                    }
                }
            }
        }
        return facilities;
    }

    /**
     * Extract unit names from a rule's condition views.
     * Returns a set of unit names that must be positively covered by a DataUpdate(create) rule.
     */
    private Set<String> extractUnitsFromRule(Rule rule) {
        Set<String> units = new LinkedHashSet<>();
        if (rule == null || rule.dataset == null) return units;
        Map<String, View> views = datasetViews.get(rule.dataset);
        if (views == null) return units;

        for (String viewName : rule.viewNames) {
            View view = views.get(viewName);
            if (view == null || view.filters == null) continue;
            for (Filter filter : view.filters) {
                String path = filter.path == null ? "" : filter.path.trim();
                if (isUnitPath(path)) {
                    // Normalize unit names to lowercase for case-insensitive matching
                    Set<String> values = parseListValues(filter.value);
                    for (String val : values) {
                        units.add(val.toLowerCase());
                    }
                }
            }
        }
        return units;
    }

    /**
     * Check if a filter path refers to facility name
     */
    private boolean isFacilityPath(String path) {
        return path != null && path.toLowerCase().contains("facility.name");
    }

    /**
     * Check if a filter path refers to unit name
     */
    private boolean isUnitPath(String path) {
        return path != null && path.toLowerCase().contains("unit.name");
    }

    /**
     * Check if a DataUpdate(create) rule covers the given facility names using valid positive logic.
     * Similar to dataUpdateRuleCoversAlertKeys but for facilities.
     */
    private boolean dataUpdateRuleCoversFacilities(Rule dataUpdateRule, Set<String> targetFacilities) {
        if (dataUpdateRule == null || dataUpdateRule.dataset == null || !datasetViews.containsKey(dataUpdateRule.dataset)) {
            return false;
        }

        Map<String, View> views = datasetViews.get(dataUpdateRule.dataset);
        
        // Collect ALL facility filters from ALL views
        List<FacilityFilter> facilityFilters = new ArrayList<>();
        
        for (String viewName : dataUpdateRule.viewNames) {
            View view = views.get(viewName);
            if (view == null || view.filters == null) continue;
            for (Filter filter : view.filters) {
                String path = filter.path == null ? "" : filter.path.trim();
                if (isFacilityPath(path)) {
                    facilityFilters.add(new FacilityFilter(filter.relation, parseListValues(filter.value)));
                }
            }
        }
        
        // If no facility filters found, rule doesn't specifically target any facilities
        // This means it applies to ALL facilities, so return true
        if (facilityFilters.isEmpty()) {
            return true;
        }
        
        // ALL filters must agree that they cover at least one of the target facilities
        for (String targetFacility : targetFacilities) {
            boolean allFiltersCoverThis = true;
            for (FacilityFilter filter : facilityFilters) {
                if (!filterCoversFacility(filter.relation, filter.values, targetFacility)) {
                    allFiltersCoverThis = false;
                    break;
                }
            }
            // If all filters agree on covering this facility, the rule covers it
            if (allFiltersCoverThis) {
                return true;
            }
        }
        
        return false;
    }

    /**
     * Check if a DataUpdate(create) rule covers the given unit names using valid positive logic.
     * Similar to dataUpdateRuleCoversAlertKeys but for units.
     */
    private boolean dataUpdateRuleCoversUnits(Rule dataUpdateRule, Set<String> targetUnits) {
        if (dataUpdateRule == null || dataUpdateRule.dataset == null || !datasetViews.containsKey(dataUpdateRule.dataset)) {
            return false;
        }

        Map<String, View> views = datasetViews.get(dataUpdateRule.dataset);
        
        // Collect ALL unit filters from ALL views
        List<UnitFilter> unitFilters = new ArrayList<>();
        
        for (String viewName : dataUpdateRule.viewNames) {
            View view = views.get(viewName);
            if (view == null || view.filters == null) continue;
            for (Filter filter : view.filters) {
                String path = filter.path == null ? "" : filter.path.trim();
                if (isUnitPath(path)) {
                    unitFilters.add(new UnitFilter(filter.relation, parseListValues(filter.value)));
                }
            }
        }
        
        // If no unit filters found, rule doesn't specifically target any units
        // This means it applies to ALL units, so return true
        if (unitFilters.isEmpty()) {
            return true;
        }
        
        // ALL filters must agree that they cover at least one of the target units
        for (String targetUnit : targetUnits) {
            boolean allFiltersCoverThis = true;
            for (UnitFilter filter : unitFilters) {
                if (!filterCoversUnit(filter.relation, filter.values, targetUnit)) {
                    allFiltersCoverThis = false;
                    break;
                }
            }
            // If all filters agree on covering this unit, the rule covers it
            if (allFiltersCoverThis) {
                return true;
            }
        }
        
        return false;
    }

    /**
     * Check if a single facility filter covers a specific facility.
     * 
     * For "in" and "equal": facility is covered if it's IN the list
     * For "not_in", "not_like", "not_equal": facility is covered if it's NOT IN the exclusion list
     */
    private boolean filterCoversFacility(String relation, Set<String> filterValues, String targetFacility) {
        if (relation == null || filterValues.isEmpty() || targetFacility == null) {
            return false;
        }
        
        // Normalize for case-insensitive comparison
        Set<String> normalizedValues = new LinkedHashSet<>();
        for (String val : filterValues) {
            normalizedValues.add(val.toLowerCase());
        }
        String normalizedTarget = targetFacility.toLowerCase();
        
        switch (relation.toLowerCase()) {
            case "in":
            case "equal":
                return normalizedValues.contains(normalizedTarget);
                
            case "not_in":
            case "not_like":
            case "not_equal":
                return !normalizedValues.contains(normalizedTarget);
                
            default:
                return false;
        }
    }

    /**
     * Check if a single unit filter covers a specific unit.
     * 
     * For "in" and "equal": unit is covered if it's IN the list
     * For "not_in", "not_like", "not_equal": unit is covered if it's NOT IN the exclusion list
     */
    private boolean filterCoversUnit(String relation, Set<String> filterValues, String targetUnit) {
        if (relation == null || filterValues.isEmpty() || targetUnit == null) {
            return false;
        }
        
        // Normalize for case-insensitive comparison
        Set<String> normalizedValues = new LinkedHashSet<>();
        for (String val : filterValues) {
            normalizedValues.add(val.toLowerCase());
        }
        String normalizedTarget = targetUnit.toLowerCase();
        
        switch (relation.toLowerCase()) {
            case "in":
            case "equal":
                return normalizedValues.contains(normalizedTarget);
                
            case "not_in":
            case "not_like":
            case "not_equal":
                return !normalizedValues.contains(normalizedTarget);
                
            default:
                return false;
        }
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

    private String normalizeAlertType(String alertType) {
        return alertType == null ? "" : alertType.trim().toLowerCase(Locale.ROOT);
    }

    private boolean isAllCaps(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        boolean hasLetter = false;
        for (char ch : value.toCharArray()) {
            if (Character.isLetter(ch)) {
                hasLetter = true;
                if (!Character.isUpperCase(ch)) {
                    return false;
                }
            }
        }
        return hasLetter;
    }

    private String chooseAlertDisplayName(String current, String candidate) {
        if (candidate == null || candidate.isEmpty()) {
            return current;
        }
        if (current == null || current.isEmpty()) {
            return candidate;
        }
        boolean currentAllCaps = isAllCaps(current);
        boolean candidateAllCaps = isAllCaps(candidate);
        if (currentAllCaps && !candidateAllCaps) {
            return candidate;
        }
        return current;
    }

    private String buildAlertKey(String dataset, String alertType) {
        String ds = dataset == null ? "" : dataset.trim();
        return ds + "|" + normalizeAlertType(alertType);
    }

    private String canonicalizeAlertName(String dataset, String alertType, Map<String, String> canonicalNames) {
        if (alertType == null) {
            return null;
        }
        String key = buildAlertKey(dataset, alertType);
        String chosen = chooseAlertDisplayName(canonicalNames.get(key), alertType);
        canonicalNames.put(key, chosen);
        return chosen;
    }

    private String getDisplayAlertName(String dataset, String alertType) {
        if (alertType == null || alertType.isEmpty()) {
            return alertType;
        }
        String canonical = canonicalizeAlertName(dataset, alertType, canonicalAlertNames);
        return (canonical == null || canonical.isEmpty()) ? alertType : canonical;
    }

    private boolean ruleContainsAlertType(Rule rule, String targetAlertType) {
        if (rule == null || rule.alertTypes == null || rule.alertTypes.isEmpty() || targetAlertType == null) {
            return false;
        }
        String normalizedTarget = normalizeAlertType(targetAlertType);
        for (String existing : rule.alertTypes) {
            if (normalizeAlertType(existing).equals(normalizedTarget)) {
                return true;
            }
        }
        return false;
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
        
        // If no alert type filters found, rule applies to ALL alert types
        // This means it covers any target alerts (universal coverage)
        if (alertFilters.isEmpty()) {
            return true;
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
     * Helper class to store facility filter information
     */
    private static class FacilityFilter {
        String relation;
        Set<String> values;
        
        FacilityFilter(String relation, Set<String> values) {
            this.relation = relation;
            this.values = values;
        }
    }
    
    /**
     * Helper class to store unit filter information
     */
    private static class UnitFilter {
        String relation;
        Set<String> values;
        
        UnitFilter(String relation, Set<String> values) {
            this.relation = relation;
            this.values = values;
        }
    }
    
    /**
     * Check if a single alert type filter covers a specific alert.
     * 
     * For "in" and "equal": alert is covered if it's IN the list
     * For "not_in", "not_like", "not_equal": alert is covered if it's NOT IN the exclusion list
     */
    private boolean filterCoversAlert(String relation, Set<String> filterValues, String targetAlert) {
        if (relation == null || filterValues.isEmpty() || targetAlert == null) {
            return false;
        }
        
        // Normalize for case-insensitive comparison
        Set<String> normalizedValues = new LinkedHashSet<>();
        for (String val : filterValues) {
            normalizedValues.add(val.toLowerCase());
        }
        String normalizedTarget = targetAlert.toLowerCase();
        
        switch (relation.toLowerCase()) {
            case "in":
            case "equal":
                return normalizedValues.contains(normalizedTarget);
                
            case "not_in":
            case "not_like":
            case "not_equal":
                return !normalizedValues.contains(normalizedTarget);
                
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
                        Set<String> values = parseAlertTypeValues(filter.value);
                        for (String value : values) {
                            String canonical = canonicalizeAlertName(rule.dataset, value, canonicalAlertNames);
                            if (canonical != null && !canonical.isEmpty()) {
                                alertTypes.add(canonical);
                            }
                        }
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
        Set<String> normalizedData = dataUpdateAlertTypes.stream()
            .map(this::normalizeAlertType)
            .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        Set<String> normalizedTargets = targetAlertTypes.stream()
            .map(this::normalizeAlertType)
            .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        
        switch (relation.toLowerCase()) {
            case "in":
            case "equal":
                // Check if any target alert type is covered (positive match)
                for (String targetType : normalizedTargets) {
                    if (normalizedData.contains(targetType)) {
                        return true;
                    }
                }
                return false;
                
            case "not_in":
                // For not_in: alert is covered if it's NOT in the exclusion list
                // Check if any target alert type is NOT in the exclusion list
                for (String targetType : normalizedTargets) {
                    if (!normalizedData.contains(targetType)) {
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
        // First, extract data from views into all rules
        for (Rule rule : allRules) {
            if (rule.dataset == null) continue;
            
            Map<String, View> views = datasetViews.get(rule.dataset);
            if (views == null) continue;
            
            for (String viewName : rule.viewNames) {
                View view = views.get(viewName);
                if (view == null) continue;
                
                // Extract role/group information from view name
                extractRoleOrGroupFromViewName(rule, viewName);
                
                for (Filter filter : view.filters) {
                    extractFilterData(rule, filter);
                }
            }
        }
        
        // NOW collect all DataUpdate (create=true) rules grouped by dataset for filtering
        // Only include ACTIVE rules - inactive CREATE rules should not cause alerts to be processed
        // This must be done AFTER extracting data from views so alert types are populated
        Map<String, List<Rule>> dataUpdateByDataset = new HashMap<>();
        for (Rule rule : allRules) {
            if ("DataUpdate".equalsIgnoreCase(rule.component) && rule.triggerCreate && rule.isActive) {
                dataUpdateByDataset.computeIfAbsent(rule.dataset, k -> new ArrayList<>()).add(rule);
            }
        }
        
        // Then apply filtering based on DataUpdate rules
        for (Rule rule : allRules) {
            if (rule.dataset == null) continue;
            
            // Filter alert types: keep only those covered by CREATE DATAUPDATE rules
            // Skip this filtering for:
            // 1. DataUpdate rules themselves
            // 2. Escalation timing rules (defer-delivery-by without destination)
            // NOTE: We DO filter escalation SEND rules (Secondary/Tertiary/etc) because they should
            // only process alert types that have CREATE DATAUPDATE rules, just like Primary rules
            
            if (!"DataUpdate".equalsIgnoreCase(rule.component) && 
                !(rule.deferDeliveryBy != null && !hasDestination(rule))) {
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
                    Set<String> normalizedValues = values.stream()
                        .map(this::normalizeAlertType)
                        .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
                    String normalizedAlert = normalizeAlertType(alertType);
                    String relation = filter.relation == null ? "" : filter.relation.toLowerCase();
                    
                    // For not_in: alert is covered if it's NOT in the exclusion list
                    if (relation.equals("not_in")) {
                        if (!normalizedValues.contains(normalizedAlert)) {
                            return true;
                        }
                    } else {
                        // For in/equal: alert is covered if it's in the list
                        if (normalizedValues.contains(normalizedAlert)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }
    
    /**
     * Extract role or group information from view name.
     * Examples:
     * - "Role_PCT_Online_VMP" -> role = "PCT"
     * - "Role_RN_Online_VMP" -> role = "RN"
     * - "Role_Charge_Nurse_Online_VMP" -> role = "Charge Nurse"
     * - "Groups_1_caregiver_is_online_with_a_VMP_phone" -> role = "Group 1"
     * - "Groups_first_caregiver_is_online_with_a_VMP_phone" -> role = "Group first"
     */
    private void extractRoleOrGroupFromViewName(Rule rule, String viewName) {
        if (viewName == null || viewName.isEmpty()) return;
        
        // Pattern: Role_<RoleName>_Online_VMP or similar
        if (viewName.startsWith("Role_") && viewName.contains("_Online")) {
            String[] parts = viewName.split("_");
            if (parts.length >= 3) {
                // Extract the role name between "Role_" and "_Online"
                StringBuilder roleName = new StringBuilder();
                for (int i = 1; i < parts.length; i++) {
                    if (parts[i].equals("Online") || parts[i].equals("VMP") || 
                        parts[i].equals("XMPP") || parts[i].equals("Offline")) {
                        break;
                    }
                    if (roleName.length() > 0) {
                        roleName.append(" ");
                    }
                    roleName.append(parts[i]);
                }
                if (roleName.length() > 0 && rule.role == null) {
                    rule.role = roleName.toString();
                    rule.roleFromView = true;
                }
            }
        }
        
        // Pattern: Groups_<number>_caregiver_is_online or Groups_<name>_caregiver_is_online
        if (viewName.startsWith("Groups_") && viewName.contains("_caregiver")) {
            String[] parts = viewName.split("_");
            if (parts.length >= 2) {
                // Extract the group identifier (number or name)
                String groupId = parts[1];
                if (rule.role == null) {
                    rule.role = "Group " + groupId;
                    rule.roleFromView = false; // Mark as group, not role
                }
            }
        }
    }
    
    /**
     * Extract data from filter into rule
     */
    private void extractFilterData(Rule rule, Filter filter) {
        if (filter.path == null || filter.value == null) return;
        
        String relation = filter.relation == null ? "" : filter.relation.toLowerCase();
        boolean isExclusion = relation.equals("not_in") || relation.equals("not_like") || relation.equals("not_equal");
        
        // Alert types - keep user-friendly casing for display while matching case-insensitively
        if (filter.path.equals("alert_type")) {
            for (String type : filter.value.split(",")) {
                String trimmed = type.trim();
                if (!trimmed.isEmpty()) {
                    String canonical = canonicalizeAlertName(rule.dataset, trimmed, canonicalAlertNames);
                    if (canonical != null && !canonical.isEmpty()) {
                        rule.alertTypes.add(canonical);
                    }
                }
            }
        }
        
        // Facilities
        if (filter.path.contains("facility.name")) {
            if (!isExclusion) {
                // Split comma-separated facility names, just like units
                for (String facility : filter.value.split(",")) {
                    String trimmed = facility.trim();
                    if (!trimmed.isEmpty()) {
                        rule.facilities.add(trimmed);
                    }
                }
            }
        }
        
        // Units - handle exclusions
        if (filter.path.contains("unit.name")) {
            if (isExclusion) {
                // Mark this rule as having unit exclusions
                // Store exclusion info for config group naming
                if (rule.settings.get("excludedUnits") == null) {
                    rule.settings.put("excludedUnits", new LinkedHashSet<String>());
                }
                @SuppressWarnings("unchecked")
                Set<String> excludedUnits = (Set<String>) rule.settings.get("excludedUnits");
                for (String unit : filter.value.split(",")) {
                    String trimmed = unit.trim();
                    if (!trimmed.isEmpty()) {
                        excludedUnits.add(trimmed);
                    }
                }
            } else {
                // Positive inclusion
                for (String unit : filter.value.split(",")) {
                    String trimmed = unit.trim();
                    if (!trimmed.isEmpty()) {
                        rule.units.add(trimmed);
                    }
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
        // Extract ALL values when path ends with "role.name" with inclusive relations
        // Use exclusion notation for exclusive relations (not_in, not_like, not_equal)
        // ALWAYS prefer filter values over view name extraction for paths ending in "role.name"
        if (filter.path.endsWith("role.name") || 
            filter.path.contains("assignments.role") || 
            filter.path.equals("role")) {
            
            String roleValue = filter.value.trim();
            
            // Check if this is an exclusion relation
            boolean isExclusionRelation = "not_in".equalsIgnoreCase(relation) || 
                                        "not_like".equalsIgnoreCase(relation) || 
                                        "not_equal".equalsIgnoreCase(relation);
            
            // For paths ending with "role.name", ALWAYS use filter value (ignore view name extraction)
            if (filter.path.endsWith("role.name")) {
                // For exclusion relations, use "AllRoles_except_X" notation
                if (isExclusionRelation) {
                    rule.role = "AllRoles_except_" + roleValue;
                    rule.roleFromView = true;
                } else {
                    // For inclusive relations (in, like, equal, contains), keep ALL comma-separated values
                    rule.role = roleValue;
                    rule.roleFromView = true;
                }
            }
            // For non-role.name paths, only set if not already set (preserve view name extraction)
            else if (rule.role == null) {
                if (isExclusionRelation) {
                    rule.role = "AllRoles_except_" + roleValue;
                    rule.roleFromView = true;
                }
                // For non-role.name paths (legacy behavior for backwards compatibility)
                else if ("in".equalsIgnoreCase(relation) && roleValue.contains(",")) {
                    String[] parts = roleValue.split(",");
                    rule.role = parts[0].trim();
                    rule.roleFromView = true;
                } else {
                    rule.role = roleValue;
                    rule.roleFromView = true;
                }
            }
        }
        
        // Also try to extract role from assignment level description paths
        if (filter.path.contains("assignment_level") && filter.path.contains("description")) {
            if (rule.role == null) {
                rule.role = filter.value.trim();
                rule.roleFromView = true;
            }
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
            // BUT skip DataUpdate rules that have filter with not_equal on assignments.state = Active
            // These represent "No Primary Assigned" scenarios that shouldn't contribute to timing
            if (rule.alertTypes.isEmpty() && rule.state != null && !rule.state.isEmpty() 
                && rule.deferDeliveryBy != null && !hasDestination(rule)) {
                // Skip DataUpdate rules whose views contain filter excluding active assignments
                if (!("DataUpdate".equalsIgnoreCase(rule.component) && hasInactiveAssignmentStateFilter(rule))) {
                    globalEscalationRules.add(rule);
                }
                continue;
            }
            
            // Track DataUpdate CREATE rules for facility extraction
            // Only include ACTIVE rules
            if ("DataUpdate".equalsIgnoreCase(rule.component) && rule.triggerCreate && rule.isActive) {
                for (String alertType : rule.alertTypes) {
                    String key = buildAlertKey(rule.dataset, alertType);
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
            
            // Second pass: actually add the rule to groups
            for (String alertType : rule.alertTypes) {
                // Get DataUpdate CREATE rules for this alert type
                String dataUpdateKey = buildAlertKey(rule.dataset, alertType);
                List<Rule> dataUpdateRules = dataUpdateRulesByAlertType.getOrDefault(dataUpdateKey, Collections.emptyList());
                
                // Determine facilities to use: inherit from CREATE if SEND rule has none
                Set<String> facilitiesToUse = new LinkedHashSet<>();
                if (rule.facilities.isEmpty()) {
                    // No facilities in SEND rule - try to inherit from CREATE rules
                    for (Rule duRule : dataUpdateRules) {
                        if (!duRule.facilities.isEmpty()) {
                            // Only add facilities from CREATE rules with explicit facility filters (inclusive relations)
                            facilitiesToUse.addAll(duRule.facilities);
                        }
                    }
                    // If no facilities found in CREATE rules either, facilitiesToUse stays empty (all facilities)
                } else {
                    // Use facilities from SEND rule
                    facilitiesToUse.addAll(rule.facilities);
                }
                
                // Determine units to use: inherit from CREATE if SEND rule has none
                Set<String> unitsToUse = new LinkedHashSet<>();
                if (rule.units.isEmpty()) {
                    // No units in SEND rule - try to inherit from CREATE rules
                    for (Rule duRule : dataUpdateRules) {
                        if (!duRule.units.isEmpty()) {
                            // Only add units from CREATE rules with explicit unit filters (inclusive relations)
                            unitsToUse.addAll(duRule.units);
                        }
                    }
                    // If no units found in CREATE rules either, unitsToUse stays empty (all units)
                } else {
                    // Use units from SEND rule
                    unitsToUse.addAll(rule.units);
                }
                
                // Sort units for consistent grouping
                String unitsKey = unitsToUse.stream().sorted().collect(java.util.stream.Collectors.joining(","));
                
                // Create groups based on determined facilities and units
                if (facilitiesToUse.isEmpty()) {
                    // No facilities - single group for all facilities
                    String key = rule.dataset + "|" + alertType + "||" + unitsKey;
                    grouped.computeIfAbsent(key, k -> new ArrayList<>()).add(rule);
                } else {
                    // Create separate group for each facility
                    for (String facility : facilitiesToUse) {
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
            String dataUpdateKey = buildAlertKey(dataset, alertType);
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
     * Merge flows with the same facility, alert, but different unit sets that overlap.
     * This handles cases where PRIMARY/SECONDARY rules have one unit set (e.g., 2600,3600,2N,3S,3N,3100)
     * and TERTIARY rules have an overlapping but different unit set (e.g., 3600,2N,3S,3N,3100).
     * For overlapping units, we should have a single flow with all recipients, not multiple flows.
     */
    private void mergeOverlappingEscalationFlows() {
        mergeOverlappingFlowsInList(nurseCalls);
        mergeOverlappingFlowsInList(clinicals);
        mergeOverlappingFlowsInList(orders);
    }
    
    /**
     * Merge overlapping flows within a single list
     */
    private void mergeOverlappingFlowsInList(List<ExcelParserV5.FlowRow> flows) {
        // Group flows by (facility, unit, alarmName)
        Map<String, List<ExcelParserV5.FlowRow>> flowsByKey = new HashMap<>();
        
        for (ExcelParserV5.FlowRow flow : flows) {
            String key = flow.configGroup + "|" + flow.alarmName;
            flowsByKey.computeIfAbsent(key, k -> new ArrayList<>()).add(flow);
        }
        
        // For each group, check if we have flows that should be merged
        // Track ORIGINAL flows to remove and FINAL merged flows to add
        Set<ExcelParserV5.FlowRow> originalFlowsToRemove = new HashSet<>();
        List<ExcelParserV5.FlowRow> finalMergedFlows = new ArrayList<>();
        
        for (List<ExcelParserV5.FlowRow> group : flowsByKey.values()) {
            if (group.size() < 2) continue; // Nothing to merge
            
            // Track original flows in this group before any merging
            Set<ExcelParserV5.FlowRow> originalFlowsInGroup = new HashSet<>(group);
            
            // Keep merging until no more complementary pairs found
            boolean merged = true;
            while (merged) {
                merged = false;
                // Look for pairs of flows where one has partial recipients and another has different partial recipients
                for (int i = 0; i < group.size(); i++) {
                    for (int j = i + 1; j < group.size(); j++) {
                        ExcelParserV5.FlowRow flow1 = group.get(i);
                        ExcelParserV5.FlowRow flow2 = group.get(j);
                        
                        // Check if these flows are complementary (one has some recipients, the other has different ones)
                        if (areComplementaryFlows(flow1, flow2)) {
                            // Merge them
                            ExcelParserV5.FlowRow mergedFlow = mergeFlows(flow1, flow2);
                            // Remove from group
                            group.remove(flow1);
                            group.remove(flow2);
                            // Add merged back to group for potential further merging
                            group.add(mergedFlow);
                            merged = true; // Found a merge, try again
                            break; // Restart the loop
                        }
                    }
                    if (merged) break; // Restart outer loop
                }
            }
            
            // After all merging, if the group size changed, we need to remove the originals and add the finals
            if (group.size() < originalFlowsInGroup.size()) {
                // Merging happened - remove all original flows and add the final merged result(s)
                originalFlowsToRemove.addAll(originalFlowsInGroup);
                finalMergedFlows.addAll(group);
            }
        }
        
        // Apply changes
        flows.removeAll(originalFlowsToRemove);
        flows.addAll(finalMergedFlows);
    }
    
    /**
     * Check if two flows are complementary (should be merged).
     * Complementary flows have:
     * - Same config group and alarm name
     * - Different recipient patterns that don't conflict
     * - OR they're duplicates (identical flows that should be deduplicated)
     */
    private boolean areComplementaryFlows(ExcelParserV5.FlowRow f1, ExcelParserV5.FlowRow f2) {
        // Must have same config group and alarm
        if (!f1.configGroup.equals(f2.configGroup)) return false;
        if (!f1.alarmName.equals(f2.alarmName)) return false;
        
        // Check if the flows have complementary data - i.e., one has data the other doesn't
        // Count how many recipient/timing slots have conflicting data (both have values)
        int conflicts = 0;
        int complements = 0;
        int duplicates = 0;  // Count of identical values in same slot
        int bothEmpty = 0;    // Count of slots where both are empty
        
        // Check each recipient and timing field
        int[] r1 = checkField(f1.r1, f2.r1);
        complements += r1[0]; duplicates += r1[1]; bothEmpty += r1[2]; conflicts += r1[3];
        
        int[] r2 = checkField(f1.r2, f2.r2);
        complements += r2[0]; duplicates += r2[1]; bothEmpty += r2[2]; conflicts += r2[3];
        
        int[] r3 = checkField(f1.r3, f2.r3);
        complements += r3[0]; duplicates += r3[1]; bothEmpty += r3[2]; conflicts += r3[3];
        
        int[] r4 = checkField(f1.r4, f2.r4);
        complements += r4[0]; duplicates += r4[1]; bothEmpty += r4[2]; conflicts += r4[3];
        
        int[] r5 = checkField(f1.r5, f2.r5);
        complements += r5[0]; duplicates += r5[1]; bothEmpty += r5[2]; conflicts += r5[3];
        
        int[] t1 = checkField(f1.t1, f2.t1);
        complements += t1[0]; duplicates += t1[1]; bothEmpty += t1[2]; conflicts += t1[3];
        
        int[] t2 = checkField(f1.t2, f2.t2);
        complements += t2[0]; duplicates += t2[1]; bothEmpty += t2[2]; conflicts += t2[3];
        
        int[] t3 = checkField(f1.t3, f2.t3);
        complements += t3[0]; duplicates += t3[1]; bothEmpty += t3[2]; conflicts += t3[3];
        
        int[] t4 = checkField(f1.t4, f2.t4);
        complements += t4[0]; duplicates += t4[1]; bothEmpty += t4[2]; conflicts += t4[3];
        
        int[] t5 = checkField(f1.t5, f2.t5);
        complements += t5[0]; duplicates += t5[1]; bothEmpty += t5[2]; conflicts += t5[3];
        
        // They're complementary if:
        // 1. There are complementary fields (one has data the other doesn't) and no more than 2 conflicts
        // 2. OR they're duplicates (all filled fields are identical) - these should be merged to eliminate duplicates
        //    Duplicates are flows where all non-empty fields match and there are no conflicts
        //    Note: Empty fields in both flows don't prevent duplicate detection
        boolean isDuplicate = duplicates > 0 && conflicts == 0;
        boolean isComplementary = complements > 0 && conflicts <= 2;
        
        return isDuplicate || isComplementary;
    }
    
    /**
     * Check a pair of string fields and return counts: [complements, duplicates, bothEmpty, conflicts]
     */
    private int[] checkField(String v1, String v2) {
        boolean has1 = v1 != null && !v1.isEmpty();
        boolean has2 = v2 != null && !v2.isEmpty();
        int complements = 0, duplicates = 0, bothEmpty = 0, conflicts = 0;
        
        if (has1 && has2) {
            if (v1.equals(v2)) {
                duplicates = 1;
            } else {
                conflicts = 1;
            }
        } else if (has1 || has2) {
            complements = 1;
        } else {
            bothEmpty = 1;
        }
        
        return new int[]{complements, duplicates, bothEmpty, conflicts};
    }
    
    /**
     * Merge two complementary flows into one.
     * Takes non-empty values from both flows, preferring non-empty over empty.
     */
    private ExcelParserV5.FlowRow mergeFlows(ExcelParserV5.FlowRow f1, ExcelParserV5.FlowRow f2) {
        ExcelParserV5.FlowRow merged = new ExcelParserV5.FlowRow();
        
        // Merge common fields - take non-empty values from either flow
        merged.inScope = f1.inScope || f2.inScope;
        merged.type = mergeField(f1.type, f2.type);
        merged.alarmName = mergeField(f1.alarmName, f2.alarmName);
        merged.sendingName = mergeField(f1.sendingName, f2.sendingName);
        merged.configGroup = mergeField(f1.configGroup, f2.configGroup);
        merged.priorityRaw = mergeField(f1.priorityRaw, f2.priorityRaw);
        merged.deviceA = mergeField(f1.deviceA, f2.deviceA);
        merged.deviceB = mergeField(f1.deviceB, f2.deviceB);
        merged.ringtone = mergeField(f1.ringtone, f2.ringtone);
        merged.responseOptions = mergeField(f1.responseOptions, f2.responseOptions);
        merged.breakThroughDND = mergeField(f1.breakThroughDND, f2.breakThroughDND);
        merged.multiUserAccept = mergeField(f1.multiUserAccept, f2.multiUserAccept);
        merged.escalateAfter = mergeField(f1.escalateAfter, f2.escalateAfter);
        merged.ttlValue = mergeField(f1.ttlValue, f2.ttlValue);
        merged.enunciate = mergeField(f1.enunciate, f2.enunciate);
        merged.emdan = mergeField(f1.emdan, f2.emdan);
        
        // Merge recipients and timing - take non-empty values
        merged.r1 = mergeField(f1.r1, f2.r1);
        merged.r2 = mergeField(f1.r2, f2.r2);
        merged.r3 = mergeField(f1.r3, f2.r3);
        merged.r4 = mergeField(f1.r4, f2.r4);
        merged.r5 = mergeField(f1.r5, f2.r5);
        
        merged.t1 = mergeField(f1.t1, f2.t1);
        merged.t2 = mergeField(f1.t2, f2.t2);
        merged.t3 = mergeField(f1.t3, f2.t3);
        merged.t4 = mergeField(f1.t4, f2.t4);
        merged.t5 = mergeField(f1.t5, f2.t5);
        
        return merged;
    }
    
    /**
     * Merge a field by taking the non-empty value.
     * For timing fields with different values, combine them with comma separation.
     */
    private String mergeField(String v1, String v2) {
        if (v1 == null || v1.isEmpty()) return v2 != null ? v2 : "";
        if (v2 == null || v2.isEmpty()) return v1;
        
        // Both have values - check if they're the same
        if (v1.equals(v2)) return v1;
        
        // Different values - for timing fields (numeric or "Immediate"), combine with comma
        // This handles cases where rules have different timing that should be merged
        if (isTimingValue(v1) && isTimingValue(v2)) {
            return v1 + "," + v2;
        }
        
        // For non-timing fields, prefer the first non-empty value
        return v1;
    }
    
    /**
     * Check if a value is a timing value (numeric seconds or "Immediate")
     */
    private boolean isTimingValue(String value) {
        if (value == null || value.isEmpty()) return false;
        if ("Immediate".equalsIgnoreCase(value.trim())) return true;
        try {
            Integer.parseInt(value.trim());
            return true;
        } catch (NumberFormatException e) {
            return false;
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
        // For flows without facility filters, use delay from ANY matching CREATE rule
        for (Rule dataUpdateRule : dataUpdateRules) {
            if (dataUpdateRule.triggerCreate && ruleContainsAlertType(dataUpdateRule, alertType)) {
                // If this flow group has no facility filter (facilityFromKey is empty),
                // accept delays from CREATE rules with facility filters
                // This allows global flows to inherit timing from facility-specific rules
                boolean facilityMatches = facilityFromKey.isEmpty() || 
                                        dataUpdateRule.facilities.isEmpty() ||
                                        dataUpdateRule.facilities.contains(facilityFromKey);
                
                if (facilityMatches) {
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
        }
        
        for (Rule rule : rules) {
            if (rule.state == null || rule.state.isEmpty()) continue;
            
            String state = normalizeState(rule.state);
            
            // If has destination, it's a send rule
            if (hasDestination(rule)) {
                sendByState.computeIfAbsent(state, k -> new ArrayList<>()).add(rule);
            }
            // If has defer-delivery-by but no destination, it's escalation timing
            // BUT skip rules that have filter with not_equal on assignments.state = Active
            // These represent "No Primary Assigned" scenarios that shouldn't contribute to timing
            else if (rule.deferDeliveryBy != null && !hasDestination(rule)) {
                // Skip DataUpdate rules whose views contain filter excluding active assignments
                if ("DataUpdate".equalsIgnoreCase(rule.component) && hasInactiveAssignmentStateFilter(rule)) {
                    // Rule references views with "assignments.state not_equal Active" - skip for timing
                    continue;
                }
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
        String displayAlertName = getDisplayAlertName(dataset, alertType);
        template.alarmName = displayAlertName;
        template.sendingName = displayAlertName;

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
        
        // Collect excluded units from rules
        Set<String> excludedUnits = new LinkedHashSet<>();
        for (Rule r : allRules) {
            if (r.settings.containsKey("excludedUnits")) {
                @SuppressWarnings("unchecked")
                Set<String> excluded = (Set<String>) r.settings.get("excludedUnits");
                excludedUnits.addAll(excluded);
            }
        }
        
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
        
        // If we have exclusions, don't populate specific units (will be empty for "AllUnits_Except_X")
        if (!excludedUnits.isEmpty()) {
            uns.clear();
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
                flow.configGroup = createConfigGroup(dataset, fset, uset, excludedUnits);

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
        String displayAlertName = getDisplayAlertName(dataset, alertType);
        template.alarmName = displayAlertName;
        template.sendingName = displayAlertName;
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
        
        // Collect excluded units from sendRule and dataUpdateRules
        Set<String> excludedUnits = new LinkedHashSet<>();
        if (sendRule.settings.containsKey("excludedUnits")) {
            @SuppressWarnings("unchecked")
            Set<String> excluded = (Set<String>) sendRule.settings.get("excludedUnits");
            excludedUnits.addAll(excluded);
        }
        for (Rule r : dataUpdateRules) {
            if (r.settings.containsKey("excludedUnits")) {
                @SuppressWarnings("unchecked")
                Set<String> excluded = (Set<String>) r.settings.get("excludedUnits");
                excludedUnits.addAll(excluded);
            }
        }
        
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
        
        // If we have exclusions, don't populate specific units (will be empty for "AllUnits_Except_X")
        if (!excludedUnits.isEmpty()) {
            uns.clear();
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
                flow.configGroup = createConfigGroup(dataset, fset, uset, excludedUnits);

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
        String displayAlertName = getDisplayAlertName(dataset, alertType);
        template.alarmName = displayAlertName;
        template.sendingName = displayAlertName;
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
                if (dataUpdateRule.triggerCreate && ruleContainsAlertType(dataUpdateRule, alertType)) {
                    // If this flow group has no facility filter, accept delays from facility-specific CREATE rules
                    boolean facilityMatches = facilityFromKey.isEmpty() ||
                                            dataUpdateRule.facilities.isEmpty() ||
                                            dataUpdateRule.facilities.contains(facilityFromKey);
                    
                    if (facilityMatches) {
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
        
        // Collect excluded units from rules and dataUpdateRules
        Set<String> excludedUnits = new LinkedHashSet<>();
        for (Rule r : rules) {
            if (r.settings.containsKey("excludedUnits")) {
                @SuppressWarnings("unchecked")
                Set<String> excluded = (Set<String>) r.settings.get("excludedUnits");
                excludedUnits.addAll(excluded);
            }
        }
        for (Rule r : dataUpdateRules) {
            if (r.settings.containsKey("excludedUnits")) {
                @SuppressWarnings("unchecked")
                Set<String> excluded = (Set<String>) r.settings.get("excludedUnits");
                excludedUnits.addAll(excluded);
            }
        }
        
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
        
        // If we have exclusions, don't populate specific units (will be empty for "AllUnits_Except_X")
        if (!excludedUnits.isEmpty()) {
            uns.clear();
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
                flow.configGroup = createConfigGroup(dataset, fset, uset, excludedUnits);

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
        return createConfigGroup(dataset, facilities, units, null);
    }
    
    private String createConfigGroup(String dataset, Set<String> facilities, Set<String> units, Set<String> excludedUnits) {
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
        
        // Handle unit exclusions
        if (excludedUnits != null && !excludedUnits.isEmpty()) {
            // Create "AllUnits_Except_X" naming pattern
            StringBuilder unitPart = new StringBuilder("AllUnits_Except");
            List<String> sortedExclusions = new ArrayList<>(excludedUnits);
            Collections.sort(sortedExclusions);
            for (String excluded : sortedExclusions) {
                unitPart.append("_").append(excluded);
            }
            parts.add(unitPart.toString());
        } else if (unit != null && !unit.isEmpty()) {
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
    
    /**
     * Check if a rule's condition views contain filters that indicate "no caregiver assigned" scenarios.
     * This detects patterns like:
     * 
     * 1. Filter with "not_equal" on assignments.state = Active:
     * <filter relation="not_equal">
     *   <path>bed.locs.assignments.state</path>
     *   <value>Active</value>
     * </filter>
     * 
     * 2. Filter with negative relation (not_in, not_equal) on role.name:
     * <filter relation="not_in">
     *   <path>bed.locs.assignments.role.name</path>
     *   <value>Buddy Nurse, Nurse Buddy</value>
     * </filter>
     * 
     * Rules with such filters should be excluded from "time to recipients" calculation
     * because they represent scenarios where no caregiver is actively assigned (e.g., 
     * "No Primary Assigned" or "No role caregiver X is online" escalation rules).
     * 
     * @param rule The rule to check
     * @return true if the rule contains filters indicating "no caregiver assigned" scenario
     */
    private boolean hasInactiveAssignmentStateFilter(Rule rule) {
        if (rule == null || rule.dataset == null) return false;
        Map<String, View> views = datasetViews.get(rule.dataset);
        if (views == null) return false;
        
        for (String viewName : rule.viewNames) {
            View view = views.get(viewName);
            if (view == null || view.filters == null) continue;
            
            for (Filter filter : view.filters) {
                if (filter.path == null || filter.relation == null || filter.value == null) continue;
                
                String path = filter.path.trim().toLowerCase();
                String relation = filter.relation.trim().toLowerCase();
                String value = filter.value.trim().toLowerCase();
                
                // Check for "not_equal" relation on a path ending with "assignments.state" with value "active"
                if (relation.equals("not_equal") && 
                    path.endsWith("assignments.state") && 
                    value.equals("active")) {
                    return true;
                }
                
                // Check for negative relations (not_in, not_equal) on role.name paths
                // This captures scenarios like "No_role_caregiver_NURSE_BUDDY_is_online"
                // where the view filters for roles that are NOT assigned
                if (isNegativeRelation(relation) && isRoleNamePath(path)) {
                    return true;
                }
            }
        }
        return false;
    }
    
    /**
     * Check if a relation is a negative/exclusion relation.
     * @param relation The relation to check (already lowercase)
     * @return true if the relation represents negative/exclusion logic
     */
    private boolean isNegativeRelation(String relation) {
        return relation != null && (
            relation.equals("not_in") ||
            relation.equals("not_equal") ||
            relation.equals("not_like")
        );
    }
    
    /**
     * Check if a path refers to a role name field.
     * Matches paths like "bed.locs.assignments.role.name" or "assignments.role.name"
     * @param path The path to check (already lowercase)
     * @return true if the path refers to role.name
     */
    private boolean isRoleNamePath(String path) {
        // Use endsWith to be more precise and avoid false positives
        // Valid patterns: *.role.name, assignments.role.name
        return path != null && (path.endsWith("role.name") || path.endsWith(".role.name"));
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
        
        // Check if destination path indicates a group (e.g., groups_1, groups_2, etc.)
        if (destination != null && destination.contains("groups_")) {
            // Extract group name from path like #{bed.room.unit.groups_1.users...}
            int groupsIndex = destination.indexOf("groups_");
            if (groupsIndex >= 0) {
                int dotIndex = destination.indexOf(".", groupsIndex);
                if (dotIndex > groupsIndex) {
                    String groupPart = destination.substring(groupsIndex, dotIndex);
                    // Convert groups_1 to "Group 1", groups_first to "Group first", etc.
                    String groupId = groupPart.substring(7); // Skip "groups_"
                    rule.roleFromView = false; // Mark as group, not role assignment
                    return "Group " + groupId;
                }
            }
        }
        
        // Check if destination path indicates a named group (e.g., .first., .second., .third.)
        // ONLY if we also have a matching Groups_ view name, otherwise keep raw destination
        if (destination != null && rule.role != null && rule.role.startsWith("Group ")) {
            String[] groupNames = {".first.", ".second.", ".third.", ".fourth.", ".fifth."};
            for (String groupName : groupNames) {
                if (destination.contains(groupName)) {
                    String name = groupName.substring(1, groupName.length() - 1); // Remove dots
                    // Only use this if the role was set from a view (Groups_first, etc.)
                    if (!rule.roleFromView) {
                        return rule.role; // Use the role from view, not parsed from destination
                    }
                }
            }
        }
        
        // Otherwise, prefer role name from view filters/names over raw destination template
        if (rule.role != null && !rule.role.isEmpty()) {
            return rule.role;
        }
        
        // Fall back to destination if no role is found (for templates or other values)
        return destination != null ? destination : "";
    }
    
    private String formatRecipient(String recipient, boolean isFromRole) {
        if (recipient == null || recipient.isEmpty()) return "";
        
        // Check if this is an exclusion notation (AllRoles_except_X)
        if (recipient.startsWith("AllRoles_except_")) {
            // Return as-is without prefix - this is exclusion notation
            return recipient;
        }
        
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
                    result.append("VGroup:").append(part);
                }
            }
            return result.toString();
        }
        
        // Single recipient
        if (isFromRole) {
            return "VAssign:[Room] " + recipient;
        } else {
            return "VGroup:" + recipient;
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
        
        // Set EMDAN Compliant field based on dataset
        // "Yes" for Clinicals dataset, "No" for all others
        if ("Clinicals".equals(flow.type)) {
            flow.emdan = "Yes";
        } else {
            flow.emdan = "No";
        }
        
        // Set escalateAfter to "1 decline" when reading XML data
        flow.escalateAfter = "1 decline";
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
            
            // Handle displayValues array (original format)
            if (root.has("displayValues") && root.get("displayValues").isArray()) {
                List<String> values = new ArrayList<>();
                root.get("displayValues").forEach(n -> values.add(n.asText()));
                result.put("displayValues", String.join(",", values));
            }
            
            // Handle responses array (alternative format from north_western_cdh_test.xml)
            // Extract displayValue from each response object
            if (root.has("responses") && root.get("responses").isArray()) {
                List<String> displayValues = new ArrayList<>();
                for (JsonNode response : root.get("responses")) {
                    if (response.has("displayValue")) {
                        displayValues.add(response.get("displayValue").asText());
                    }
                }
                if (!displayValues.isEmpty()) {
                    // Store comma-delimited displayValues same as displayValues array
                    result.put("displayValues", String.join(",", displayValues));
                }
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
        canonicalAlertNames.clear();
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

        return (
            "✅ XML Load Complete%n%n" +
                "Loaded:%n" +
                "  • %d Unit rows%n" +
                "  • %d Nurse Call rows%n" +
                "  • %d Clinical rows%n" +
                "  • %d Orders rows%n" +
                "  • Config Groups: %d (Nurse), %d (Clinical), %d (Orders) — Total %d").formatted(
            units.size(), nurseCalls.size(), clinicals.size(), orders.size(),
            nurseCfgs, clinicalCfgs, ordersCfgs, totalCfgs
        );
    }
}
