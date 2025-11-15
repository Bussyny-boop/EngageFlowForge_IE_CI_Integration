# XML to GUI Visual Mapping Guide

## Overview
This document shows exactly how data from your `EngagexmLV1` XML file appears in your application's GUI tables.

---

## 🔄 Data Flow Architecture

```
EngagexmLV1 (XML File)
         │
         ├─► XmlParser.java
         │        │
         │        ├─► parseDatasets() → Build view definitions
         │        ├─► parseInterfaces() → Extract rules
         │        ├─► mergeStateBasedRules() → Group escalations
         │        └─► createFlowRows() → Generate FlowRow objects
         │
         └─► AppController.java
                  │
                  ├─► unitsFullList → Units Table
                  ├─► nurseCallsFullList → Nurse Calls Table
                  ├─► clinicalsFullList → Clinicals Table
                  └─► ordersFullList → Orders Table
```

---

## 📊 Table 1: Units Table

### XML Source
Units are derived from facility and unit names found in view filters across all rules.

**Example XML (from view filters):**
```xml
<view>
  <name>Facility_name_is_Northland</name>
  <filter relation="equal">
    <path>bed.room.unit.facility.name</path>
    <value>Northland</value>
  </filter>
</view>

<view>
  <name>Unit_name_is_NICU</name>
  <filter relation="equal">
    <path>bed.room.unit.name</path>
    <value>NICU</value>
  </filter>
</view>
```

### GUI Display
| Facility | Unit Names | Nurse Group | Clinical Group | Orders Group |
|----------|-----------|-------------|----------------|--------------|
| Northland | NICU | NurseCalls_NICU | Clinicals_NICU | Orders_NICU |
| Northland | ICU | NurseCalls_ICU | Clinicals_ICU | - |
| Ridges | 4E | - | Clinicals_4E | - |

### Field Mapping
- **Facility**: Extracted from `facility.name` filters
- **Unit Names**: Extracted from `unit.name` filters
- **Nurse Group**: Config group if any NurseCalls rules reference this unit
- **Clinical Group**: Config group if any Clinicals rules reference this unit
- **Orders Group**: Config group if any Orders rules reference this unit

---

## 📊 Table 2: Nurse Calls Table

### XML Source

**Example 1: Simple Immediate Alert**
```xml
<rule active="true" dataset="NurseCalls">
  <purpose>SEND | PATIENT CALL | VMP | PRIMARY | ALL UNITS</purpose>
  <trigger-on create="true"/>
  <condition>
    <view>Alert_is_at_primary_state</view>
    <view>Alert_type_is_Patient</view>
  </condition>
  <settings>{
    "destination":"Primary Nurse",
    "priority":"2",
    "ttl":10,
    "enunciate":"SYSTEM_DEFAULT"
  }</settings>
</rule>
```

**Interface:**
```xml
<interface component="VMP">
  <name>VMP</name>
  ...
  <!-- The rule above appears here -->
</interface>
```

### GUI Display
| ✓ | Config Group | Alarm Name | Sending Name | Priority | Device A | T1 | R1 | TTL | Enunciate |
|---|--------------|------------|--------------|----------|----------|----|----|-----|-----------|
| ✓ | NurseCalls | Patient | Patient | Normal | VMP | Immediate | Primary Nurse | 10 | SYSTEM_DEFAULT |

---

**Example 2: Alert with Escalation**

**XML (Multiple Rules):**
```xml
<!-- Rule 1: Initial send to Primary -->
<rule active="true" dataset="NurseCalls">
  <purpose>SEND | ROUTINE CALL | PRIMARY | VMP</purpose>
  <trigger-on create="true"/>
  <condition>
    <view>Alert_is_at_primary_state</view>
    <view>Alert_type_is_Routine</view>
  </condition>
  <settings>{
    "destination":"Staff Nurse",
    "priority":"2"
  }</settings>
</rule>

<!-- Rule 2: Escalate to Secondary after 2 minutes -->
<interface component="DataUpdate">
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
</interface>

<!-- Rule 3: Send to Secondary recipient -->
<rule active="true" dataset="NurseCalls">
  <purpose>SEND | ROUTINE CALL | SECONDARY | VMP</purpose>
  <trigger-on update="true"/>
  <condition>
    <view>Alert_is_at_secondary_state</view>
    <view>Alert_type_is_Routine</view>
  </condition>
  <settings>{
    "destination":"Charge Nurse",
    "priority":"1"
  }</settings>
</rule>

<!-- Rule 4: Escalate to Tertiary after 3 minutes -->
<interface component="DataUpdate">
  <rule active="true" dataset="NurseCalls">
    <purpose>ESCALATE | SECONDARY TO TERTIARY | 3MIN</purpose>
    <trigger-on update="true"/>
    <defer-delivery-by>180</defer-delivery-by>
    <condition>
      <view>Alert_is_at_secondary_state</view>
    </condition>
    <settings>{"parameters":[{"path":"state","value":"Tertiary"}]}</settings>
  </rule>
</interface>

<!-- Rule 5: Send to Tertiary recipient -->
<rule active="true" dataset="NurseCalls">
  <purpose>SEND | ROUTINE CALL | TERTIARY | VMP</purpose>
  <trigger-on update="true"/>
  <condition>
    <view>Alert_is_at_tertiary_state</view>
  </condition>
  <settings>{
    "destination":"Manager",
    "priority":"0"
  }</settings>
</rule>
```

### GUI Display (Merged into Single Row)
| ✓ | Config Group | Alarm Name | Priority | Device A | T1 | R1 | T2 | R2 | T3 | R3 |
|---|--------------|------------|----------|----------|----|----|----|----|----|----|
| ✓ | NurseCalls | Routine | Normal | VMP | Immediate | Staff Nurse | 2MIN | Charge Nurse | 3MIN | Manager |

**How Merging Works:**
1. Parser identifies rules with same alert type ("Routine")
2. Groups by state: Primary, Secondary, Tertiary
3. Maps Primary → R1, Secondary → R2, Tertiary → R3
4. Maps Primary escalation delay → T2, Secondary delay → T3

---

## 📊 Table 3: Clinicals Table

### XML Source

**Example: Critical Clinical Alarm**
```xml
<rule active="true" dataset="Clinicals">
  <purpose>SEND | ASYSTOLE | VMP | IMMEDIATE | NICU</purpose>
  <trigger-on create="true"/>
  <condition>
    <view>Alarm_is_at_primary_state</view>
    <view>Alarm_included_in_Asystole_VFib_VTach</view>
    <view>Unit_name_is_NICU</view>
  </condition>
  <settings>{
    "destination":"RN Bedside",
    "priority":"0",
    "ttl":5,
    "enunciate":"SYSTEM_DEFAULT",
    "overrideDND":true,
    "displayValues":["Respond","Call MD"],
    "storedValues":["Accepted","Call MD"]
  }</settings>
</rule>
```

**From VMP Interface:**
```xml
<interface component="VMP">
  <!-- Rule appears here -->
</interface>
```

### GUI Display
| ✓ | Config Group | Alarm Name | Sending Name | Priority | Device A | Response Options | Break Through DND | T1 | R1 | TTL | Enunciate |
|---|--------------|------------|--------------|----------|----------|------------------|-------------------|----|----|-----|-----------|
| ✓ | Clinicals_NICU | Asystole | Asystole | Urgent | VMP | Respond,Call MD | TRUE | Immediate | RN Bedside | 5 | SYSTEM_DEFAULT |

### Field Details
- **Alarm Name**: From alert_type filter in views
- **Sending Name**: Same as Alarm Name (displayed to recipient)
- **Priority**: "0"=Urgent, "1"=High, "2"=Normal
- **Response Options**: From displayValues in settings
- **Break Through DND**: From overrideDND in settings

---

**Example: Clinical with EMDAN Support**

```xml
<rule active="true" dataset="Clinicals">
  <purpose>SEND | BED EXIT | EMDAN | VMP</purpose>
  <trigger-on create="true"/>
  <condition>
    <view>Alarm_is_at_primary_state</view>
    <view>Nursecall_Alarm_Emdan_Bed_Exit_Safety_alarm</view>
  </condition>
  <settings>{
    "destination":"Staff Nurse",
    "priority":"1",
    "shortMessage":"Bed Exit Alert in Room #{bed.room.room_number}"
  }</settings>
</rule>
```

### GUI Display
| ✓ | Config Group | Alarm Name | Priority | Device A | Emdan | T1 | R1 |
|---|--------------|------------|----------|----------|-------|----|----|
| ✓ | Clinicals | Bed Exit | High | VMP | Yes | Immediate | Staff Nurse |

---

## 📊 Table 4: Orders Table

### XML Source

**Example: Epic Order**
```xml
<rule active="true" dataset="Orders">
  <purpose>SEND EPIC ORDER | CONSULT | VOCERA GROUP | VMP | ALL UNITS</purpose>
  <trigger-on create="true"/>
  <condition>
    <view>Alert_is_Epic_Order</view>
    <view>Order_code_is_for_Pallative_Care</view>
    <view>Facility_name_is_Northland</view>
  </condition>
  <settings>{
    "destination":"g-12345",
    "priority":"2",
    "ttl":20160,
    "displayValues":["Accept","Decline"],
    "storedValues":["Accepted","Declined"],
    "enunciate":"SYSTEM_DEFAULT",
    "subject":"Palliative Care Consult"
  }</settings>
</rule>
```

### GUI Display
| ✓ | Config Group | Alarm Name | Sending Name | Priority | Device A | T1 | R1 | TTL | Response Options | Enunciate |
|---|--------------|------------|--------------|----------|----------|----|----|-----|------------------|-----------|
| ✓ | Orders_Northland | Epic Order | Palliative Care Consult | Normal | VMP | Immediate | g-12345 | 20160 | Accept,Decline | SYSTEM_DEFAULT |

### Field Details
- **R1 (Recipient)**: "g-12345" indicates a Vocera group number
- **TTL**: 20160 minutes = 14 days (typical for orders)
- **Subject**: Used as Sending Name for orders

---

## 🔍 Special Cases

### Case 1: Rules Without Alert Types
Some rules don't specify alert types in views, just conditions:

```xml
<rule active="true" dataset="NurseCalls">
  <purpose>SEND | ALL CODES | GROUP | VMP</purpose>
  <trigger-on create="true"/>
  <condition>
    <view>Alert_is_at_Group_state</view>
    <view>Urgent_Priority_Alert_type</view>
  </condition>
  <settings>{"destination":"g-99999","priority":"0"}</settings>
</rule>
```

**Result:** Alarm Name is set to the purpose text or "Unknown"

### Case 2: DataUpdate-Only Rules (No Send)
Rules that only set state without sending messages:

```xml
<interface component="DataUpdate">
  <rule active="true" dataset="Clinicals">
    <purpose>SET TO PRIMARY | IMMEDIATE</purpose>
    <trigger-on create="true"/>
    <condition>
      <view>Alarm_has_no_state</view>
    </condition>
    <settings>{"parameters":[{"path":"state","value":"Primary"}]}</settings>
  </rule>
</interface>
```

**Result:** Not displayed in tables (no recipient, just state initialization)

### Case 3: Multiple Devices
Some alerts send to different device types:

```xml
<!-- Rule 1: VMP -->
<interface component="VMP">
  <rule active="true" dataset="NurseCalls">
    <condition><view>Alert_type_is_Code_Blue</view></condition>
    <settings>{"destination":"g-11111"}</settings>
  </rule>
</interface>

<!-- Rule 2: Vocera Badge -->
<interface component="Vocera">
  <rule active="true" dataset="NurseCalls">
    <condition><view>Alert_type_is_Code_Blue</view></condition>
    <settings>{"destination":"Code Team"}</settings>
  </rule>
</interface>
```

**Result:** Creates TWO rows:
| Alarm Name | Device A | R1 |
|------------|----------|-----|
| Code Blue | VMP | g-11111 |
| Code Blue | Vocera | Code Team |

---

## 🎯 Key Features of Your Parser

### ✅ Intelligent Escalation Merging
- Automatically combines multiple state-based rules into single rows
- Properly sequences Primary → Secondary → Tertiary → Quaternary
- Extracts timing from DataUpdate escalation rules

### ✅ Alert Type Extraction
- Parses view filters for alert_type conditions
- Handles "in" relations with comma-separated values
- Maps alert types from view names

### ✅ Facility/Unit Tracking
- Builds complete facility-to-unit mappings
- Creates UnitRow entries for each unique combination
- Associates config groups with appropriate units

### ✅ Settings JSON Parsing
- Extracts priority, ttl, destination, enunciate
- Handles response options (displayValues/storedValues)
- Maps overrideDND to Break Through DND field

---

## 🚀 Usage in GUI

### Loading XML File

1. **Click "Load" Button**
2. **Select File Type**: Choose XML files (*.xml)
3. **Pick File**: Select `EngagexmLV1`
4. **Wait for Progress**: "📥 Loading XML file..."
5. **View Results**: Tables populate automatically

### Viewing Parsed Data

**Status Bar Shows:**
```
✅ XML Load Complete

Loaded:
  • 12 Unit rows
  • 45 Nurse Call rows
  • 78 Clinical rows
  • 23 Orders rows
```

**Tables Update:**
- Units Tab: Shows all facility/unit combinations
- Nurse Calls Tab: All NurseCalls rules
- Clinicals Tab: All Clinicals rules  
- Orders Tab: All Orders rules

### Filtering Data

Use Config Group filters to narrow down:
- **Units Filter**: Show only specific units
- **Nurse Calls Filter**: Filter by config group (e.g., "NurseCalls_NICU")
- **Clinicals Filter**: Filter by config group
- **Orders Filter**: Filter by config group

---

## 📝 Summary

Your application successfully parses complex XML configurations from Vocera Engage and displays them in an intuitive tabular format. The parser:

1. **Extracts** dataset definitions and view filters
2. **Identifies** interface rules across multiple components
3. **Merges** state-based escalation rules intelligently
4. **Maps** XML fields to GUI table columns
5. **Generates** FlowRow and UnitRow objects for display

**Result:** A powerful visual editor for Vocera Engage configurations that would otherwise require manual XML editing!
