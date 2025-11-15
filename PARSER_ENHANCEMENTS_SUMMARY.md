# Parser Enhancements Summary

## Overview
Enhanced both XML and JSON parsers to create config groups using **Facility + Unit + Dataset** format and to fully extract recipient/timing information.

## Changes Made

### 1. XML Parser Config Group Enhancement

**File**: `src/main/java/com/example/exceljson/XmlParser.java`

**Change**: Updated `createConfigGroup()` method signature and logic

**Before**:
```java
private String createConfigGroup(String dataset, Set<String> units) {
    String unit = units.isEmpty() ? "" : units.iterator().next();
    return unit.isEmpty() ? dataset : dataset + "_" + unit;
}
```

**After**:
```java
private String createConfigGroup(String dataset, Set<String> facilities, Set<String> units) {
    String facility = facilities.isEmpty() ? "" : facilities.iterator().next();
    String unit = units.isEmpty() ? "" : units.iterator().next();
    
    if (facility.isEmpty() && unit.isEmpty()) {
        return dataset;
    } else if (facility.isEmpty()) {
        return dataset + "_" + unit;
    } else if (unit.isEmpty()) {
        return dataset + "_" + facility;
    } else {
        return facility + "_" + unit + "_" + dataset;
    }
}
```

**Config Group Formats**:
- Facility + Unit + Dataset: `Main Hospital_ICU_NurseCalls`
- Dataset + Facility (no unit): `NurseCalls_East Wing`
- Dataset + Unit (no facility): `NurseCalls_4East`
- Dataset only: `GlobalAlerts`

**Updated Calls**:
- Line ~556: `flow.configGroup = createConfigGroup(dataset, refRule.facilities, refRule.units);`
- Line ~569: `flow.configGroup = createConfigGroup(dataset, sendRule.facilities, sendRule.units);`

---

### 2. JSON Parser Complete Enhancement

**File**: `src/main/java/com/example/exceljson/ExcelParserV5.java`

**Method**: `parseJsonFlows()` (lines 429-583)

**New Capabilities**:

#### A. Recipient Extraction (r1-r5)
Extracts all 5 recipient slots from `destinations` array:
- Parses `functionalRoles` array → applies `VAssign:[Room]` prefix
- Parses `groups` array → plain group names
- Combines multiple recipients with **newline separator** (`\n`)
- Maps to FlowRow fields: `r1`, `r2`, `r3`, `r4`, `r5`

**Example**:
```json
"destinations": [{
  "order": 0,
  "functionalRoles": [
    {"facilityName": "Main", "name": "Nurse"},
    {"facilityName": "Main", "name": "Tech"}
  ],
  "groups": [
    {"facilityName": "Main", "name": "Team A"}
  ]
}]
```
Results in: `r1 = "VAssign:[Room] Nurse\nVAssign:[Room] Tech\nTeam A"`

#### B. Timing Extraction (t1-t5)
Extracts delay times from `destinations` array:
- Reads `delayTime` field (in seconds)
- Converts to string (empty if 0)
- Maps to FlowRow fields: `t1`, `t2`, `t3`, `t4`, `t5`

**Example**:
```json
{"order": 1, "delayTime": 60}  →  t2 = "60"
{"order": 0, "delayTime": 0}   →  t1 = ""
```

#### C. Units and Config Group Extraction
Parses `units` array to build **Facility_Unit_Dataset** config groups:
- Extracts `facilityName` and `name` from first unit
- Builds config group: `facilityName + "_" + unitName + "_" + dataset`
- Populates group-to-units mappings for JSON export

**Example**:
```json
"units": [{"facilityName": "Hospital", "name": "ICU"}]
```
With `alarmsAlerts: ["Alert"]` → `configGroup = "Hospital_ICU_Alert"`

#### D. Parameter Attributes
Already parsed:
- `breakThrough` → `breakThroughDND`
- `enunciate` → `enunciate`
- `alertSound`/`badgeAlertSound` → `ringtone` (strips .wav)
- `ttl` → `ttlValue`
- `responseType` → `responseOptions`

---

### 3. Test Updates

#### XmlParserVAssignTest.java
Updated expectations to match new Facility_Unit_Dataset format:

**Line 223**: 
```java
// Old: assertEquals("NurseCalls_ICU", flow.configGroup);
assertEquals("BCH_ICU_NurseCalls", flow.configGroup,
    "Config group should be 'BCH_ICU_NurseCalls' (Facility_Unit_Dataset format)");
```

**Line 312**:
```java
// Old: assertEquals("Clinicals_ER", flow.configGroup);
assertEquals("MainHospital_ER_Clinicals", flow.configGroup,
    "Config group should be 'MainHospital_ER_Clinicals' (Facility_Unit_Dataset format)");
```

#### New JsonParserEnhancedTest.java
Created comprehensive test suite with 6 test cases:

1. **testJsonParserExtractsRecipients**
   - Validates r1/r2/r3 extraction
   - Validates t1/t2/t3 timing
   - Tests VAssign:[Room] prefix for functional roles

2. **testJsonParserExtractsMultipleRecipientsPerDestination**
   - Tests newline separator for multiple recipients
   - Validates functional roles + groups combined

3. **testJsonParserExtractsUnitsAndConfigGroup**
   - Validates Facility_Unit_Dataset config group format
   - Tests units array parsing

4. **testJsonParserExtractsParameterAttributes**
   - Tests breakThrough, enunciate, ringtone, ttl extraction

5. **testJsonParserHandlesAllFiveRecipients**
   - Validates all r1-r5 and t1-t5 fields
   - Tests escalation flows with 5 destinations

6. **testJsonParserRoundTripWithRecipientsAndUnits**
   - Complete bidirectional test
   - Validates JSON → App → JSON workflow

---

## Test Results

### All Tests Passing ✅
```
Tests run: 450, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

**New Tests**: 6 (JsonParserEnhancedTest)
**Updated Tests**: 2 (XmlParserVAssignTest)

---

## Usage Examples

### XML Parser
```xml
<dataset active="true">
  <name>NurseCalls</name>
  <views>
    <view>
      <name>ICU_View</name>
      <filters>
        <filter relation="equal">
          <path>bed.facility.name</path>
          <value>Main Hospital</value>
        </filter>
        <filter relation="equal">
          <path>bed.unit.name</path>
          <value>ICU</value>
        </filter>
      </filters>
    </view>
  </views>
</dataset>
```
Results in: `configGroup = "Main Hospital_ICU_NurseCalls"`

### JSON Parser
```json
{
  "nurseCalls": {
    "deliveryFlows": [{
      "name": "SEND NURSECALL | URGENT | Code Blue | Config1 | ",
      "priority": "urgent",
      "alarmsAlerts": ["Code Blue"],
      "destinations": [
        {
          "order": 0,
          "delayTime": 0,
          "functionalRoles": [{"facilityName": "West", "name": "Nurse"}]
        },
        {
          "order": 1,
          "delayTime": 60,
          "groups": [{"facilityName": "West", "name": "RN Group"}]
        }
      ],
      "units": [{"facilityName": "West", "name": "3North"}]
    }]
  }
}
```

Parsed FlowRow:
- `r1 = "VAssign:[Room] Nurse"`
- `t1 = ""`
- `r2 = "RN Group"`
- `t2 = "60"`
- `configGroup = "West_3North_Code Blue"`

---

## Benefits

### 1. Comprehensive Config Groups
- Unique identification using Facility + Unit + Dataset
- Better organization for multi-facility/multi-unit deployments
- Backward compatible (handles missing facility/unit)

### 2. Complete JSON Bidirectionality
- Load JSON → populate all fields (recipients, timing, units)
- Edit in GUI → export to JSON
- Full round-trip capability

### 3. Data Integrity
- All recipient escalation chains preserved (r1-r5, t1-t5)
- Timing information maintained
- Unit associations retained
- VAssign:[Room] prefix for role-based routing

---

## Git Commits

**Commit**: 91ede8d
**Message**: feat: Enhance XML and JSON parsers with Facility+Unit+Dataset config groups and complete recipient/timing extraction

**Files Changed**:
- `src/main/java/com/example/exceljson/XmlParser.java`
- `src/main/java/com/example/exceljson/ExcelParserV5.java`
- `src/test/java/com/example/exceljson/XmlParserVAssignTest.java`
- `src/test/java/com/example/exceljson/JsonParserEnhancedTest.java` (NEW)

---

## Next Steps

Both parsers now provide:
✅ Facility + Unit + Dataset config groups
✅ Complete recipient extraction (r1-r5)
✅ Complete timing extraction (t1-t5)
✅ VAssign:[Room] prefix for functional roles
✅ Newline-separated recipients
✅ Full bidirectional JSON support

The application is ready for production use with comprehensive XML and JSON parsing capabilities.
