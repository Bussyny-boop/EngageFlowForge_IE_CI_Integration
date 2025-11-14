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
        
        // Parse interfaces (to create flow rows)
        parseInterfaces(doc);
        
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
     * Parse a single <rule> element and create flow rows.
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
        
        // Extract alert types, facilities, and units from views
        Set<String> alertTypes = new HashSet<>();
        Set<String> facilities = new HashSet<>();
        Set<String> units = new HashSet<>();
        String role = null;
        
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
                    }
                }
            }
        }
        
        // Parse settings JSON to extract additional fields
        Map<String, Object> settings = parseSettingsJson(settingsJson);
        
        // Create flow rows for each alert type and unit combination
        createFlowRows(dataset, purpose, component, alertTypes, facilities, units, 
                      deferDeliveryBy, isCreate, isUpdate, settings, role);
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
                } else {
                    // Otherwise use the role name (functional role)
                    if (role != null && !role.isEmpty()) {
                        recipient = role;
                    }
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
