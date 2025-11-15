# XML Parsing Guide for Clinicals, NurseCalls, and Orders

## Overview

This guide explains how the `XmlParser.java` already in your application parses the Vocera Engage XML configuration file (`EngagexmLV1`) to populate your JavaFX GUI with Clinicals, NurseCalls, and Orders data.

## XML File Structure

The XML file contains two main sections relevant to our parsing:

### 1. **Datasets Section** (`<datasets>`)
Defines the data structures and views for each alert type:
- **Clinicals** (line 3378): Clinical alarms from patient monitoring equipment
- **NurseCalls** (line 7548): Nurse call system alerts
- **Orders** (line 10566): Order notifications from Epic/EMR systems

Each dataset contains:
- **Attributes**: Field definitions (alert_type, patient info, timestamps, etc.)
- **Views**: Named filter conditions (e.g., "Alert_is_at_primary_state", "Alarm_included_in_Asystole_VFib_VTach")

### 2. **Interfaces Section** (`<interfaces>`)
Contains the actual routing rules that determine how alerts are delivered:
- **DataUpdate** interfaces: Set alert states and control escalation timing
- **VMP** interfaces: Send messages to VMP (Vocera Mobile Platform) phones
- **Vocera** interfaces: Send to Vocera badges
- **XMPP** interfaces: Send to XMPP-based devices

## How Your Current Parser Works

Your `XmlParser.java` already implements a sophisticated parsing strategy:

### Step 1: Parse Datasets to Build Views
```java
private void parseDatasets(Document doc) {
    // Extracts all view definitions from each dataset
    // Stores them in: Map<String, Map<String, ViewDefinition>> datasetViews
}
```

**What it captures:**
- View names (e.g., "Alert_is_at_primary_state")
- Filter conditions within each view
- Alert types, facilities, units, states, roles

### Step 2: Parse Interface Rules
```java
private void parseInterfaces(Document doc) {
    // Iterates through all <interface> elements
    // For each <rule>, calls parseRule()
}
```

**What each rule contains:**
- `dataset`: "Clinicals", "NurseCalls", or "Orders"
- `purpose`: Human-readable description
- `trigger-on`: create="true" or update="true"
- `defer-delivery-by`: Time delay in seconds
- `condition`: List of view names that must match
- `settings`: JSON with priority, ttl, destination, etc.

### Step 3: Extract Data from Rules
```java
private void parseRule(Element ruleElement, String component) {
    // Collects:
    // - Alert types from view filters
    // - Facilities and units from view filters
    // - Recipients from settings (destination or role)
    // - State information (Primary, Secondary, etc.)
    // - Timing information (defer-delivery-by)
}
```

### Step 4: Merge State-Based Escalation Rules
```java
private void mergeStateBasedRules() {
    // Groups rules by dataset and alert types
    // Identifies escalation patterns:
    //   - Primary state -> 1st recipient (R1)
    //   - Secondary state -> 2nd recipient (R2)
    //   - Tertiary state -> 3rd recipient (R3)
    //   - Quaternary state -> 4th recipient (R4)
}
```

## Data Flow: XML → GUI

### Example 1: Clinicals Alert

**XML Rule (Simplified):**
```xml
<rule active="true" dataset="Clinicals">
  <purpose>SEND | APNEA | PRIMARY | VMP | NICU</purpose>
  <trigger-on create="true"/>
  <condition>
    <view>Alarm_is_at_primary_state</view>
    <view>Alarm_included_in_Asystole_VFib_VTach_Desat_APNEA</view>
    <view>Unit_name_is_NICU</view>
  </condition>
  <settings>{"priority":"0","destination":"Nurse","ttl":10}</settings>
</rule>
```

**Resulting FlowRow in GUI:**
| Field | Value |
|-------|-------|
| Type | Clinicals |
| Config Group | Clinicals_NICU |
| Alarm Name | APNEA |
| Sending Name | APNEA |
| Priority | Urgent (mapped from "0") |
| Device A | VMP |
| T1 | Immediate |
| R1 | Nurse |
| TTL | 10 |

### Example 2: NurseCalls with Escalation

**XML Rules (Multiple):**
```xml
<!-- Rule 1: Set to Primary state -->
<rule active="true" dataset="NurseCalls">
  <purpose>SET TO PRIMARY | ALL ROOM ALERTS</purpose>
  <trigger-on create="true"/>
  <condition>
    <view>Alert_is_at_primary_state</view>
    <view>Alert_type_is_Patient</view>
  </condition>
  <settings>{"parameters":[{"path":"state","value":"Primary"}]}</settings>
</rule>

<!-- Rule 2: Send to Primary recipient -->
<rule active="true" dataset="NurseCalls">
  <purpose>SEND | PATIENT | PRIMARY | VMP</purpose>
  <trigger-on create="true"/>
  <condition>
    <view>Alert_is_at_primary_state</view>
    <view>Alert_type_is_Patient</view>
  </condition>
  <settings>{"destination":"Primary Nurse","priority":"2"}</settings>
</rule>

<!-- Rule 3: Escalate to Secondary after 2 minutes -->
<rule active="true" dataset="NurseCalls">
  <purpose>ESCALATE | PRIMARY TO SECONDARY | 2MIN</purpose>
  <trigger-on update="true"/>
  <defer-delivery-by>120</defer-delivery-by>
  <condition>
    <view>Alert_is_at_primary_state</view>
    <view>Alert_has_not_been_responded_to</view>
  </condition>
  <settings>{"parameters":[{"path":"state","value":"Secondary"}]}</settings>
</rule>

<!-- Rule 4: Send to Secondary recipient -->
<rule active="true" dataset="NurseCalls">
  <purpose>SEND | PATIENT | SECONDARY | VMP</purpose>
  <trigger-on update="true"/>
  <condition>
    <view>Alert_is_at_secondary_state</view>
  </condition>
  <settings>{"destination":"Charge Nurse","priority":"1"}</settings>
</rule>
```

**Resulting FlowRow in GUI:**
| Field | Value |
|-------|-------|
| Type | NurseCalls |
| Alarm Name | Patient |
| T1 | Immediate |
| R1 | Primary Nurse |
| T2 | 2MIN |
| R2 | Charge Nurse |

### Example 3: Orders

**XML Rule:**
```xml
<rule active="true" dataset="Orders">
  <purpose>SEND EPIC ORDER | CONSULT | VMP | ALL UNITS</purpose>
  <trigger-on create="true"/>
  <condition>
    <view>Alert_is_Epic_Order</view>
    <view>Order_code_is_Consult</view>
  </condition>
  <settings>{
    "destination":"g-12345",
    "priority":"2",
    "ttl":20160,
    "displayValues":["Accept","Decline"]
  }</settings>
</rule>
```

**Resulting FlowRow in GUI:**
| Field | Value |
|-------|-------|
| Type | Orders |
| Alarm Name | Epic Order |
| Priority | Normal |
| Device A | VMP |
| T1 | Immediate |
| R1 | g-12345 |
| TTL | 20160 |
| Response Options | Accept, Decline |

## Key Mapping Logic

### 1. **Component → Device**
```java
private String mapComponentToDevice(String component) {
    switch (component.toUpperCase()) {
        case "VMP": return "VMP";
        case "DATAUPDATE": return "Edge";
        case "VOCERA": return "Vocera";
        case "XMPP": return "XMPP";
        default: return component;
    }
}
```

### 2. **Priority Mapping**
- "0" → "Urgent"
- "1" → "High"  
- "2" → "Normal"

### 3. **State → Recipient Index**
- Primary state → R1
- Secondary state → R2
- Tertiary state → R3
- Quaternary state → R4
- Quinary state → R5

### 4. **Timing from Escalation Rules**
The parser looks for DataUpdate rules with `defer-delivery-by` and maps them:
- Rule with state=Primary sets T2 (time to escalate from R1 to R2)
- Rule with state=Secondary sets T3 (time to escalate from R2 to R3)
- Rule with state=Tertiary sets T4 (time to escalate from R3 to R4)

## Using the Parser

### Load XML File
```java
XmlParser parser = new XmlParser();
try {
    parser.load(new File("EngagexmLV1"));
    
    // Get parsed data
    List<ExcelParserV5.UnitRow> units = parser.getUnits();
    List<ExcelParserV5.FlowRow> clinicals = parser.getClinicals();
    List<ExcelParserV5.FlowRow> nurseCalls = parser.getNurseCalls();
    List<ExcelParserV5.FlowRow> orders = parser.getOrders();
    
    // Display summary
    System.out.println(parser.getLoadSummary());
    
} catch (Exception e) {
    e.printStackTrace();
}
```

### Expected Output
```
✅ XML Load Complete

Loaded:
  • 12 Unit rows
  • 45 Nurse Call rows
  • 78 Clinical rows
  • 23 Orders rows
```

## Integration with AppController

Your `AppController.java` should populate the tables with the parsed data:

```java
@FXML
public void loadXmlFile() {
    FileChooser fc = new FileChooser();
    fc.setTitle("Select XML Configuration File");
    fc.getExtensionFilters().add(
        new FileChooser.ExtensionFilter("XML Files", "*.xml", "*")
    );
    
    File file = fc.showOpenDialog(stage);
    if (file != null) {
        try {
            XmlParser xmlParser = new XmlParser();
            xmlParser.load(file);
            
            // Populate Units table
            unitsFullList.setAll(xmlParser.getUnits());
            
            // Populate NurseCalls table
            nurseCallsFullList.setAll(xmlParser.getNurseCalls());
            
            // Populate Clinicals table
            clinicalsFullList.setAll(xmlParser.getClinicals());
            
            // Populate Orders table
            ordersFullList.setAll(xmlParser.getOrders());
            
            statusLabel.setText("Loaded: " + file.getName());
            currentFileLabel.setText(file.getName());
            
        } catch (Exception e) {
            showError("Failed to load XML file", e.getMessage());
        }
    }
}
```

## Common Patterns in the XML

### 1. **Immediate Send Rules**
```xml
<rule active="true" dataset="NurseCalls">
  <trigger-on create="true"/>
  <!-- No defer-delivery-by = Immediate -->
</rule>
```

### 2. **Delayed Send Rules**
```xml
<rule active="true" dataset="Clinicals">
  <trigger-on create="true"/>
  <defer-delivery-by>30</defer-delivery-by>
  <!-- Sends after 30 seconds -->
</rule>
```

### 3. **Escalation Rules**
```xml
<rule active="true" dataset="NurseCalls">
  <trigger-on update="true"/>
  <defer-delivery-by>120</defer-delivery-by>
  <condition>
    <view>Alert_is_at_primary_state</view>
    <view>Alert_has_not_been_responded_to</view>
  </condition>
  <!-- Escalates after 2 minutes if no response -->
</rule>
```

### 4. **Cancel/Clear Rules**
```xml
<rule active="true" dataset="Clinicals">
  <trigger-on update="true"/>
  <condition>
    <view>Alarm_is_inactive</view>
  </condition>
  <settings>{"ruleAction":"CANCEL"}</settings>
</rule>
```

## Troubleshooting

### Issue: No data appears in tables
**Check:**
1. Is the XML file valid?
2. Are there any active rules for your datasets?
3. Check console for parsing exceptions

### Issue: Escalation not merging correctly
**Check:**
1. Do you have both SEND and ESCALATE rules?
2. Are state values consistent (Primary, Secondary, etc.)?
3. Are alert types matching between rules?

### Issue: Recipients not appearing
**Check:**
1. Does the settings JSON have "destination" or is there a role in the view?
2. Is the destination in the correct format (group number, role name, etc.)?

## Next Steps

1. **Add XML File Picker to GUI**: Add a menu item or button to load XML files
2. **Test with Sample Data**: Use the EngagexmLV1 file to verify parsing
3. **Add Validation**: Ensure parsed data matches expected format
4. **Export Functionality**: Enable exporting modified data back to XML if needed

## Summary

Your `XmlParser.java` provides a complete solution for parsing Vocera Engage XML files into your application's data model. The parser:

✅ Extracts dataset definitions and views  
✅ Parses interface rules from multiple components (VMP, DataUpdate, etc.)  
✅ Identifies escalation patterns and merges rules intelligently  
✅ Maps XML data to FlowRow and UnitRow objects  
✅ Handles complex scenarios like state-based escalation  

The parsed data seamlessly integrates with your existing JavaFX tables, allowing users to view and edit Clinicals, NurseCalls, and Orders configurations visually.
