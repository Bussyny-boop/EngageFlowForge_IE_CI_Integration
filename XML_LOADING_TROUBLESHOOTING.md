# XML Loading Troubleshooting Guide

## Issue: "XML file loads but no data appears in GUI"

### Root Cause

Your `EngagexmLV1` XML file contains **mostly DataUpdate rules** with very few active VMP/Vocera/XMPP send rules.

**What this means:**
- ✅ The XML parser is working correctly
- ✅ All active rules are being processed
- ❌ Most active rules don't create GUI rows because they only set states (no recipients)

### Understanding Rule Types

#### 1. DataUpdate Rules (State-Setting Only)
These rules initialize or change alert states but **do NOT send messages**:

```xml
<rule active="true" dataset="NurseCalls">
  <purpose>1. SET TO PRIMARY | ALL ROOM ALERTS</purpose>
  <settings>{"parameters":[{"path":"state","value":"Primary"}]}</settings>
</rule>
```

**Result**: No GUI row created (no destination/recipient)

#### 2. VMP/Vocera/XMPP Rules (Message-Sending)
These rules actually send messages to recipients:

```xml
<rule active="true" dataset="NurseCalls">
  <purpose>SEND | PATIENT CALL | PRIMARY | VMP</purpose>
  <settings>{"destination":"Primary Nurse","priority":"2"}</settings>
</rule>
```

**Result**: Creates GUI row with R1="Primary Nurse"

### Analyzing Your XML File

Run this command to see rule distribution:

```bash
# Count active vs inactive rules
grep -c 'active="true"' EngagexmLV1
grep -c 'active="false"' EngagexmLV1

# Count active rules by interface
grep -B5 'active="true"' EngagexmLV1 | grep 'interface component' | sort | uniq -c
```

**Expected Result:**
- Most active rules are in DataUpdate interface
- Most VMP/Vocera rules are inactive (`active="false"`)

### Solution Options

#### Option 1: Activate More Send Rules
Edit `EngagexmLV1` and change `active="false"` to `active="true"` for VMP/Vocera rules you want to use.

Example - Find lines like:
```xml
<rule active="false" dataset="NurseCalls">
  <purpose>SEND | PATIENT CALL | PRIMARY | VMP</purpose>
```

Change to:
```xml
<rule active="true" dataset="NurseCalls">
  <purpose>SEND | PATIENT CALL | PRIMARY | VMP</purpose>
```

#### Option 2: Verify Escalation Merging
Some rules might be getting merged. Check if you have matching escalation patterns:

**Required for merging:**
1. DataUpdate rule with state filter
2. VMP/Vocera rule with **same state** and **same alert type**

Example of proper escalation setup:
```xml
<!-- DataUpdate: Set to Primary -->
<rule active="true" dataset="Clinicals">
  <condition><view>Alarm_has_no_state</view></condition>
  <settings>{"parameters":[{"path":"state","value":"Primary"}]}</settings>
</rule>

<!-- VMP: Send to Primary recipient -->
<rule active="true" dataset="Clinicals">
  <condition>
    <view>Alarm_is_at_primary_state</view>
    <view>Alarm_included_in_APNEA</view>
  </condition>
  <settings>{"destination":"RN Bedside","priority":"0"}</settings>
</rule>
```

#### Option 3: Use Excel Format Instead
If you have an Excel workbook version of your configuration, load that instead:
1. Click **Load**
2. Select your Excel file (`.xlsx`)
3. All sheets will be parsed correctly

### Verifying Parser Output

After loading XML, check the JSON Preview panel for:

```
✅ XML Load Complete

Loaded:
  • 0 Unit rows          ← If 0, no units were found
  • 0 Nurse Call rows    ← If 0, no active NurseCalls send rules
  • 0 Clinical rows      ← If 0, no active Clinicals send rules
  • 0 Orders rows        ← If 0, no active Orders send rules
```

**If all counts are 0**: Your XML has no active send rules!

### Parser Logic Summary

The `XmlParser.java` correctly implements:

1. ✅ Parse datasets to extract view definitions
2. ✅ Parse interface rules from all components
3. ✅ Skip rules with `active="false"`
4. ✅ Extract alert types from view filters
5. ✅ Merge state-based escalation rules
6. ✅ Create FlowRow objects for rules with destinations/recipients
7. ✅ **Skip** rules without destinations (DataUpdate state-setters)

### Testing with Sample Data

To verify the parser works, create a minimal test XML:

```xml
<package>
  <contents>
    <datasets>
      <dataset active="true">
        <name>NurseCalls</name>
        <view>
          <name>Alert_type_is_Patient</name>
          <filter relation="equal">
            <path>alert_type</path>
            <value>Patient</value>
          </filter>
        </view>
      </dataset>
    </datasets>
    <interfaces>
      <interface component="VMP">
        <rule active="true" dataset="NurseCalls">
          <trigger-on create="true"/>
          <condition><view>Alert_type_is_Patient</view></condition>
          <settings>{"destination":"Test Nurse","priority":"2"}</settings>
        </rule>
      </interface>
    </interfaces>
  </contents>
</package>
```

**Expected Result**: 1 Nurse Call row with R1="Test Nurse"

### Conclusion

**The parser is working correctly**. The issue is that your `EngagexmLV1` file has very few active send rules.

**Action Items:**
1. Review which rules should be active in your XML
2. Activate VMP/Vocera/XMPP rules that should send messages
3. Ensure state-based rules have matching send rules
4. Reload the XML file after making changes

**Alternative:** Use the Excel format if available, as it's easier to edit and visualize.
