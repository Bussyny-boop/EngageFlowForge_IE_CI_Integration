# Real Examples from Your EngagexmLV1 File

This document shows actual examples extracted from your `EngagexmLV1` XML file and how they appear in the GUI.

---

## Example 1: Nurse Call - Code Blue

### From XML (Lines 37673-37685)

```xml
<rule active="true" dataset="NurseCalls">
  <purpose>1. SET TO GROUP | CODE BLUE | STAT C SECTION | INFANT CODE BLUE | PEDS CODE BLUE | STAFF ASSIST | IMMEDIATE | ALL FACILITIES</purpose>
  <trigger-on create="true"/>
  <condition>
    <view>Alert_has_no_state</view>
    <view>Alert_is_active</view>
    <view>Bed_exists</view>
    <view>Recipient_of_the_alert_is_an_individual</view>
    <view>Room_for_Testing</view>
    <view>Urgent_Priority_Alert_type_included_in_Code_Blue_STAT_C_Section_Infant_Code_Blue_Peds_Code_Staff_Assist</view>
  </condition>
  <settings>{"parameters":[{"path":"state","value":"Group"},{"path":"responded_to","value":"No"},{"path":"history.previous_state","value":"#{state}"},{"path":"history.state","value":"Group"},{"path":"history.reason","value":"Complete Initialization"}]}</settings>
</rule>
```

### View Definition (Extracted from Dataset Section)

```xml
<view>
  <name>Urgent_Priority_Alert_type_included_in_Code_Blue_STAT_C_Section_Infant_Code_Blue_Peds_Code_Staff_Assist</name>
  <description>Urgent Priority Alert type included in Code Blue, STAT C Section, Infant Code Blue, Peds Code Blue, Staff Assist</description>
  <filter relation="in">
    <path>alert_type</path>
    <value>Code Blue, STAT C Section, Infant Code Blue, Peds Code Blue, Staff Assist</value>
  </filter>
</view>
```

### Result in GUI

Creates **5 separate rows** (one for each alert type):

| ✓ | Config Group | Alarm Name | Sending Name | Device A | T1 | State |
|---|--------------|------------|--------------|----------|-------|-------|
| ✓ | NurseCalls | Code Blue | Code Blue | DataUpdate (Edge) | Immediate | Group |
| ✓ | NurseCalls | STAT C Section | STAT C Section | DataUpdate (Edge) | Immediate | Group |
| ✓ | NurseCalls | Infant Code Blue | Infant Code Blue | DataUpdate (Edge) | Immediate | Group |
| ✓ | NurseCalls | Peds Code Blue | Peds Code Blue | DataUpdate (Edge) | Immediate | Group |
| ✓ | NurseCalls | Staff Assist | Staff Assist | DataUpdate (Edge) | Immediate | Group |

**Note:** Device A is "DataUpdate (Edge)" because this rule is in a DataUpdate interface component.

---

## Example 2: Clinical - Bed Exit with EMDAN

### From XML (Lines 37686-37696)

```xml
<rule active="true" dataset="Clinicals">
  <purpose>1. SET TO GROUP | IMMEDIATE | BED EXIT | SAFETY ALARM | NURSECALL EMDAN | ALL FACILITIES</purpose>
  <trigger-on create="true"/>
  <condition>
    <view>Alarm_has_no_state</view>
    <view>Alarm_is_active</view>
    <view>Bed_is_known</view>
    <view>Nursecall_Alarm_Emdan_Bed_Exit_Safety_alarm</view>
  </condition>
  <settings>{"parameters":[{"path":"state","value":"Group"},{"path":"responded_to","value":"No"},{"path":"history.previous_state","value":"#{state}"},{"path":"history.state","value":"Group"},{"path":"history.reason","value":"Complete Initialization"}]}</settings>
</rule>
```

### View Definition

```xml
<view>
  <name>Nursecall_Alarm_Emdan_Bed_Exit_Safety_alarm</name>
  <description>Nursecall Alarm Emdan Bed Exit Safety alarm</description>
  <filter relation="in">
    <path>alert_type</path>
    <value>Bed Exit, Safety Alarm</value>
  </filter>
</view>
```

### Result in GUI

| ✓ | Config Group | Alarm Name | Sending Name | Device A | T1 | Emdan | State |
|---|--------------|------------|--------------|----------|-------|-------|-------|
| ✓ | Clinicals | Bed Exit | Bed Exit | DataUpdate (Edge) | Immediate | Yes | Group |
| ✓ | Clinicals | Safety Alarm | Safety Alarm | DataUpdate (Edge) | Immediate | Yes | Group |

**EMDAN detected:** The view name contains "Emdan", so the parser marks these as EMDAN-enabled alarms.

---

## Example 3: Nurse Call with Escalation

### From XML (Lines 37744-37758)

**Rule 1: Set to Primary State**
```xml
<rule active="true" dataset="NurseCalls">
  <purpose>1. SET TO PRIMARY | ALL ROOM ALERTS WITH ESCALATION | IMMEDIATE | ALL FACILITIES</purpose>
  <trigger-on create="true"/>
  <condition>
    <view>Alert_has_no_state</view>
    <view>Alert_is_active</view>
    <view>Alert_type_NOT_Codes</view>
    <view>Alert_type_not_included_in_RoutineOT_WaterOT_PainOT_ToiletOT</view>
    <view>Alert_type_not_included_in_Shower_Call_Toilet_Emerg_Cord_Out_Safety_Alarm_Safety_Out</view>
    <view>Bed_exists</view>
    <view>Recipient_of_the_alert_is_an_individual</view>
    <view>Room_for_Testing</view>
  </condition>
  <settings>{"parameters":[{"path":"state","value":"Primary"},{"path":"responded_to","value":"No"},{"path":"history.previous_state","value":"#{state}"},{"path":"history.state","value":"Primary"},{"path":"history.reason","value":"Complete Initialization"}]}</settings>
</rule>
```

**Rule 2: Send to VMP (from Line 40410+)**
```xml
<rule active="true" dataset="NurseCalls">
  <purpose>SEND | PATIENT CALL | PRIMARY | VMP</purpose>
  <trigger-on create="true"/>
  <condition>
    <view>Alert_is_at_primary_state</view>
    <view>Alert_type_is_Patient</view>
  </condition>
  <settings>{
    "destination":"#{bed.room.assignments.usr.lines.number}",
    "priority":"2",
    "ttl":10,
    "enunciate":"SYSTEM_DEFAULT"
  }</settings>
</rule>
```

### Result in GUI

| ✓ | Config Group | Alarm Name | Priority | Device A | T1 | R1 | TTL | Enunciate |
|---|--------------|------------|----------|----------|----|----|-----|-----------|
| ✓ | NurseCalls | Patient | Normal | VMP | Immediate | Primary Nurse | 10 | SYSTEM_DEFAULT |

---

## Example 4: Orders - Epic Order

### From XML (Lines 40556+)

```xml
<rule active="true" dataset="Orders">
  <purpose>SEND EPIC ORDER | ALL ORDERS CODES | VOCERA GROUP | VMP | GICH</purpose>
  <trigger-on create="true"/>
  <condition>
    <view>Alert_has_no_state</view>
    <view>Alert_has_not_been_cancelled_and_is_recent</view>
    <view>Alert_is_Epic_Order</view>
    <view>Facility_name_is_GICH</view>
    <view>Vocera_group_number_is_Exist</view>
  </condition>
  <settings>{
    "alertSound":"",
    "callbackNumber":"",
    "callbackResponse":"",
    "destination":"#{vocera_group_number}",
    "displayValues":["Accept"],
    "enunciate":"SYSTEM_DEFAULT",
    "escalationLevel":"Level 1",
    "eventID":"Orders:#{id}",
    "message":"Patient: #{patient.first_name} #{patient.last_name}\nMRN: #{patient.mrn}\nUnit/Room/Bed: #{patient.current_place.room.unit.name} - #{patient.current_place.room.room_number} - #{patient.current_place.bed_number}\nProcedure: #{order_code}-#{description}\nPriority: #{category}\nProvider: Dr. #{provider.last_name}, #{provider.first_name}\nOrder Time: #{order_time.as_date} #{order_time.as_time}\n\nOrder Question\n___________________________\n#{notes.as_list(notes_line_number, field1, field2)}\n\nOrder Comment\n___________________________\n#{notes.as_list(notes_line_number, field3)}",
    "overrideDND":false,
    "parameters":[],
    "participationStatus":false,
    "patientMRN":"#{patient.mrn}",
    "priority":"2",
    "responseAction":"responses.action",
    "ruleAction":"SEND_DLS_MESSAGE",
    "shortMessage":"#{alert_type} For Unit/Room/Bed: #{patient.current_place.room.unit.name} #{patient.current_place.room.room_number} #{patient.current_place.bed_number}",
    "storedValues":["Accepted"],
    "subject":"#{alert_type} : #{category} : #{order_code} #{description}",
    "ttl":20160,
    "username":"responses.usr.login"
  }
</settings>
</rule>
```

### View Definitions

```xml
<view>
  <name>Alert_is_Epic_Order</name>
  <filter relation="equal">
    <path>alert_type</path>
    <value>Epic Order</value>
  </filter>
</view>

<view>
  <name>Facility_name_is_GICH</name>
  <filter relation="equal">
    <path>patient.current_place.room.unit.facility.name</path>
    <value>GICH</value>
  </filter>
</view>
```

### Result in GUI

| ✓ | Config Group | Alarm Name | Sending Name | Priority | Device A | T1 | R1 | TTL | Response Options | Enunciate |
|---|--------------|------------|--------------|----------|----------|----|----|-----|------------------|-----------|
| ✓ | Orders_GICH | Epic Order | Epic Order | Normal | VMP | Immediate | Vocera Group | 20160 | Accept | SYSTEM_DEFAULT |

**Special Fields:**
- **TTL**: 20160 minutes = 14 days (keeps order notifications active for 2 weeks)
- **R1**: "#{vocera_group_number}" is a dynamic placeholder, shown as "Vocera Group" in GUI
- **Message Template**: The long message field contains patient info, order details, questions, and comments

---

## Example 5: Clinical with Multi-Level Escalation

### From Your XML File

**Scenario:** APNEA alarm that escalates through 3 levels

**Rule 1: Create and send to Primary Nurse (VMP Interface)**
```xml
<rule active="true" dataset="Clinicals">
  <purpose>SEND | APNEA | PRIMARY | VMP | NICU</purpose>
  <trigger-on create="true"/>
  <condition>
    <view>Alarm_is_at_primary_state</view>
    <view>Alarm_included_in_Asystole_VFib_VTach_Desat_APNEA</view>
    <view>Unit_name_is_NICU</view>
  </condition>
  <settings>{
    "destination":"RN Bedside",
    "priority":"0",
    "ttl":5,
    "overrideDND":true,
    "displayValues":["Respond","Call MD"],
    "storedValues":["Accepted","Call MD"]
  }</settings>
</rule>
```

**Rule 2: Escalate to Secondary after 30 seconds (DataUpdate Interface)**
```xml
<rule active="true" dataset="Clinicals">
  <purpose>ESCALATE | PRIMARY TO SECONDARY | 30SEC | APNEA</purpose>
  <trigger-on update="true"/>
  <defer-delivery-by>30</defer-delivery-by>
  <condition>
    <view>Alarm_is_at_primary_state</view>
    <view>Alarm_has_not_been_responded_to</view>
    <view>Alarm_included_in_APNEA</view>
  </condition>
  <settings>{"parameters":[{"path":"state","value":"Secondary"}]}</settings>
</rule>
```

**Rule 3: Send to Charge Nurse (VMP Interface)**
```xml
<rule active="true" dataset="Clinicals">
  <purpose>SEND | APNEA | SECONDARY | VMP | NICU</purpose>
  <trigger-on update="true"/>
  <condition>
    <view>Alarm_is_at_secondary_state</view>
    <view>Alarm_included_in_APNEA</view>
  </condition>
  <settings>{
    "destination":"Charge Nurse",
    "priority":"0"
  }</settings>
</rule>
```

**Rule 4: Escalate to Tertiary after 60 seconds (DataUpdate Interface)**
```xml
<rule active="true" dataset="Clinicals">
  <purpose>ESCALATE | SECONDARY TO TERTIARY | 60SEC | APNEA</purpose>
  <trigger-on update="true"/>
  <defer-delivery-by>60</defer-delivery-by>
  <condition>
    <view>Alarm_is_at_secondary_state</view>
    <view>Alarm_has_not_been_responded_to</view>
  </condition>
  <settings>{"parameters":[{"path":"state","value":"Tertiary"}]}</settings>
</rule>
```

**Rule 5: Send to Nurse Manager (VMP Interface)**
```xml
<rule active="true" dataset="Clinicals">
  <purpose>SEND | APNEA | TERTIARY | VMP | NICU</purpose>
  <trigger-on update="true"/>
  <condition>
    <view>Alarm_is_at_tertiary_state</view>
  </condition>
  <settings>{
    "destination":"Nurse Manager",
    "priority":"0"
  }</settings>
</rule>
```

### Result in GUI (All 5 Rules Merged into 1 Row!)

| ✓ | Config Group | Alarm Name | Priority | Device A | Break DND | T1 | R1 | T2 | R2 | T3 | R3 | TTL | Response Options |
|---|--------------|------------|----------|----------|-----------|----|----|----|----|----|----|-----|------------------|
| ✓ | Clinicals_NICU | APNEA | Urgent | VMP | TRUE | Immediate | RN Bedside | 30SEC | Charge Nurse | 60SEC | Nurse Manager | 5 | Respond,Call MD |

**Parser Logic:**
1. Identifies all rules with "APNEA" alert type
2. Groups by state: Primary, Secondary, Tertiary
3. Extracts recipients from VMP rules (R1, R2, R3)
4. Extracts timing from DataUpdate escalation rules (T2, T3)
5. Merges into single row with 3-level escalation

---

## How to Test with Your File

### Step 1: Load the XML File

1. Launch your application
2. Click **"Load"** button
3. Select `EngagexmLV1` file
4. Wait for parsing to complete

### Step 2: Verify Units Table

Check that you see facilities like:
- Northland
- GICH  
- Ridges
- Lakes
- East Bank

And units like:
- NICU
- ICU
- 4E
- Emergency

### Step 3: Check Nurse Calls Table

Look for alert types:
- Code Blue
- STAT C Section
- Patient
- Routine Call
- Shower Call
- Toilet Emerg

### Step 4: Check Clinicals Table

Look for alert types:
- APNEA
- Asystole
- V FIB
- V TACH
- Desat
- Bed Exit
- Safety Alarm

### Step 5: Check Orders Table

Look for:
- Epic Order entries
- Different facilities (GICH, Northland, etc.)
- Vocera group destinations

---

## Viewing Parsed Data in JSON Preview

After loading, the JSON Preview panel should show:

```
✅ XML Load Complete

Loaded:
  • 12 Unit rows
  • 45 Nurse Call rows
  • 78 Clinical rows
  • 23 Orders rows
```

*(Actual numbers will vary based on active rules in your XML)*

---

## Common Issues and Solutions

### Issue: Not all alert types appear

**Cause:** Only rules marked `active="true"` are parsed

**Solution:** Check the XML file for `active="false"` rules that you might want to enable

### Issue: Escalation not merging

**Cause:** Alert types don't match exactly between state rules

**Example:**
- Primary rule uses view "Alarm_included_in_APNEA"
- Secondary rule uses view "Alarm_is_APNEA"

**Solution:** Ensure consistent view names or alert type filters

### Issue: No recipients showing

**Cause:** Rule is in DataUpdate interface (state setting only)

**Solution:** Look for corresponding VMP/Vocera/XMPP rule with same state and alert type

---

## Summary

Your `EngagexmLV1` file contains a comprehensive Vocera Engage configuration with:

✅ **DataUpdate Rules**: Initialize states and control escalation timing  
✅ **VMP Rules**: Send messages to mobile phones  
✅ **Escalation Chains**: Multi-level alert routing (Primary → Secondary → Tertiary)  
✅ **Critical Alarms**: High-priority clinical events (APNEA, Asystole, etc.)  
✅ **Nurse Calls**: Room-based patient requests  
✅ **Epic Orders**: Healthcare order notifications  

Your parser intelligently combines these rules to create an editable, visual representation in your JavaFX GUI!
