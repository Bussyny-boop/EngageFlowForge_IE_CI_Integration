# Merge Button Logic Fix - Summary

## Overview
This document summarizes the changes made to fix and clarify the merge button logic for the NurseCall JSON generator.

## Problem
The original implementation had confusing naming and incorrect behavior:
- The merge mode names didn't match their actual behavior
- When merging across config groups, only one config group name appeared in the flow name instead of all involved groups

## Solution
1. **Swapped enum semantics** to match UI expectations
2. **Updated flow name generation** to include all config groups when merging across multiple groups
3. **Renamed UI labels** for clarity

## Three Merge Modes

### Mode 1: Standard (No Merge) ✅ (Already Working)
**Button Label:** "Standard (No Merge)"  
**Enum Value:** `MergeMode.NONE`  
**Behavior:** One deliveryFlow per alert row. No merging occurs.

**Example Output:**
```json
{
  "name": "SEND NURSECALL | HIGH | Pain Meds | Acute Care NC | MedSurg / ICU",
  "alarmsAlerts": ["Pain Meds"],
  "units": [
    {"facilityName": "BCH", "name": "MedSurg"},
    {"facilityName": "BCH", "name": "ICU"}
  ]
}
```

---

### Mode 2: Merge Multiple Config Groups (FIXED)
**Old Button Label:** "Merge by Config Group"  
**New Button Label:** "Merge Multiple Config Groups"  
**Enum Value:** `MergeMode.MERGE_BY_CONFIG_GROUP`  
**Behavior:** Merge alerts across MULTIPLE configuration groups if their parameters match (same priority, ringtone, destinations, etc.)

**Key Fix:** The flow name now includes **ALL** involved config groups separated by " / "

**Example Output:**
```json
{
  "name": "SEND NURSECALL | HIGH | Normal Call / Pain Meds / Shower Call | Acute Care NC / OB NC | LDR / MedSurg / ICU",
  "alarmsAlerts": ["Normal Call", "Pain Meds", "Shower Call"],
  "units": [
    {"facilityName": "BCH", "name": "LDR"},
    {"facilityName": "BCH", "name": "MedSurg"},
    {"facilityName": "BCH", "name": "ICU"}
  ],
  "priority": "high",
  "destinations": [
    {
      "order": 0,
      "functionalRoles": [{"name": "CNA", "facilityName": "BCH"}]
    },
    {
      "order": 1,
      "functionalRoles": [{"name": "Nurse", "facilityName": "BCH"}]
    }
  ]
}
```

**Rules:**
- ✅ Combine all alerts with identical parameters, even if Config Groups differ
- ✅ Append ALL involved Config Group names in the "name" section (e.g., "Acute Care NC / OB NC")
- ✅ Combine unit names from all involved rows
- ✅ Keep "destinations", "parameterAttributes", and "conditions" identical

---

### Mode 3: Merge by Single Config Group (RENAMED)
**Old Button Label:** "Merge Across Config Group"  
**New Button Label:** "Merge by Single Config Group"  
**Enum Value:** `MergeMode.MERGE_ACROSS_CONFIG_GROUP`  
**Behavior:** Merge alerts ONLY within the same Config Group when their parameters match.

**Example Output:**
```json
{
  "name": "SEND NURSECALL | HIGH | Pain Meds / Shower Call | Acute Care NC | MedSurg / ICU",
  "alarmsAlerts": ["Pain Meds", "Shower Call"],
  "units": [
    {"facilityName": "BCH", "name": "MedSurg"},
    {"facilityName": "BCH", "name": "ICU"}
  ],
  "priority": "high",
  "destinations": [
    {
      "order": 0,
      "functionalRoles": [{"name": "CNA", "facilityName": "BCH"}]
    },
    {
      "order": 1,
      "functionalRoles": [{"name": "Nurse", "facilityName": "BCH"}]
    }
  ]
}
```

**Rules:**
- ✅ Merge alerts only within the same Config Group (Acute Care NC, OB NC, etc.)
- ✅ Combine alarmsAlerts with identical parameters
- ✅ Preserve unique units belonging to that config group
- ✅ Name format: `SEND NURSECALL | HIGH | [Alert Names joined by /] | [Config Group Name] | [Unit Names joined by /]`

---

## Code Changes Summary

### 1. ExcelParserV5.java
**Enum Comments Updated:**
```java
public enum MergeMode {
  NONE,                          // No merging - each alarm gets its own flow
  MERGE_BY_CONFIG_GROUP,         // Merge identical flows across multiple config groups (include all config group names in flow name)
  MERGE_ACROSS_CONFIG_GROUP      // Merge identical flows within a single config group (No Caregiver Group must match)
}
```

**buildMergeKey Logic Swapped:**
- `MERGE_BY_CONFIG_GROUP`: Does NOT include configGroup in merge key → allows merging across groups
- `MERGE_ACROSS_CONFIG_GROUP`: DOES include configGroup in merge key → keeps groups separate

**buildFlowNameMerged Updated:**
- Now accepts `List<String> configGroups` instead of single FlowRow template
- For `MERGE_BY_CONFIG_GROUP`: Joins all config groups with " / "
- For `MERGE_ACROSS_CONFIG_GROUP`: Uses only the first config group
- Added `MergeMode` parameter to determine which behavior to use

**buildFlowsMerged Updated:**
- Collects all config groups from merged flows
- Passes list of config groups to buildFlowNameMerged

### 2. App.fxml
**UI Label Changes:**
```xml
<!-- OLD -->
<CheckBox fx:id="mergeByConfigGroupCheckbox" text="Merge by Config Group">
<CheckBox fx:id="mergeAcrossConfigGroupCheckbox" text="Merge Across Config Group">

<!-- NEW -->
<CheckBox fx:id="mergeByConfigGroupCheckbox" text="Merge Multiple Config Groups">
<CheckBox fx:id="mergeAcrossConfigGroupCheckbox" text="Merge by Single Config Group">
```

**Tooltip Updates:**
- Tooltips now accurately describe the new behavior

### 3. AppController.java
**JSON Mode Label Updates:**
```java
// OLD
if (isMergedByConfigGroup) {
    mode = "Merge by Config Group";
} else if (isMergedAcrossConfigGroup) {
    mode = "Merge Across Config Group";
}

// NEW
if (isMergedByConfigGroup) {
    mode = "Merge Multiple Config Groups";
} else if (isMergedAcrossConfigGroup) {
    mode = "Merge by Single Config Group";
}
```

**Export Status Messages Updated:**
```java
// OLD
case MERGE_BY_CONFIG_GROUP -> "Merged by Config Group";
case MERGE_ACROSS_CONFIG_GROUP -> "Merged Across Config Groups";

// NEW
case MERGE_BY_CONFIG_GROUP -> "Merged Multiple Config Groups";
case MERGE_ACROSS_CONFIG_GROUP -> "Merged by Single Config Group";
```

### 4. Tests
**Updated Test Files:**
- `MergeByConfigGroupTest.java` - Swapped test expectations to match new semantics
- `DefaultMergeConfigTest.java` - Updated to expect merging across groups for MERGE_BY_CONFIG_GROUP
- `MergeAcrossConfigGroupWithDifferentUnitsTest.java` - Swapped test expectations

**New Test File:**
- `MergeMultipleConfigGroupsNameTest.java` - Verifies that config group names are properly collected:
  - Test 1: MERGE_BY_CONFIG_GROUP includes all config groups in name
  - Test 2: MERGE_ACROSS_CONFIG_GROUP includes only single config group in name

---

## Test Results
✅ All 391 tests passing

---

## Backward Compatibility
The boolean parameter methods still work:
- `buildNurseCallsJson(true)` → Uses `MERGE_ACROSS_CONFIG_GROUP` (merge within single group)
- `buildNurseCallsJson(false)` → Uses `NONE` (no merging)

**Note:** The semantics of the boolean parameter are maintained from the old implementation, so existing code will continue to work as expected.

---

## Comparison Table

| Aspect | Mode 1: Standard (No Merge) | Mode 2: Merge Multiple Config Groups | Mode 3: Merge by Single Config Group |
|--------|----------------------------|--------------------------------------|--------------------------------------|
| **Enum** | `NONE` | `MERGE_BY_CONFIG_GROUP` | `MERGE_ACROSS_CONFIG_GROUP` |
| **Button Label** | Standard (No Merge) | Merge Multiple Config Groups | Merge by Single Config Group |
| **Merges Across Groups?** | No | Yes ✅ | No |
| **Merges Within Group?** | No | Yes ✅ | Yes ✅ |
| **Config Group in Name** | Single | **All involved** ✅ | Single |
| **Flows per Alarm** | 1 | Variable (based on merge) | Variable (based on merge) |

---

## Example Scenario (from Problem Statement)

**Input:**
- 2 Config Groups: "Acute Care NC" and "OB NC"
- Units:
  - MedSurg, ICU → Acute Care NC
  - LDR → OB NC
- 3 Alarms with identical delivery params:
  - Normal Call (Acute Care NC)
  - Pain Meds (Acute Care NC)
  - Shower Call (OB NC)

**Output with Different Modes:**

### Standard (No Merge):
- 3 separate flows (one per alarm)

### Merge Multiple Config Groups:
- **1 merged flow**
- Name includes: `"Acute Care NC / OB NC"`
- All alarms: `["Normal Call", "Pain Meds", "Shower Call"]`
- All units: `["MedSurg", "ICU", "LDR"]`

### Merge by Single Config Group:
- **2 flows** (one per config group)
- Flow 1 - Name: `"... Acute Care NC ..."`
  - Alarms: `["Normal Call", "Pain Meds"]`
  - Units: `["MedSurg", "ICU"]`
- Flow 2 - Name: `"... OB NC ..."`
  - Alarms: `["Shower Call"]`
  - Units: `["LDR"]`

---

## Verification
The changes have been verified through:
1. ✅ Unit tests (391 passing)
2. ✅ Build success (mvn clean package)
3. ✅ Code review of merge logic
4. ✅ Test coverage for config group name collection

Ready for final integration testing and UI verification.
