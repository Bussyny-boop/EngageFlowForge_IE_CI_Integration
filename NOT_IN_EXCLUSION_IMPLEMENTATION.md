# NOT_IN/NOT_LIKE/NOT_EQUAL Exclusion Logic Implementation

## Summary

Implemented support for `NOT_IN`, `NOT_LIKE`, and `NOT_EQUAL` filter operators in the XML parser to handle unit exclusions and case-insensitive alert matching across all datasets (NurseCalls, Clinicals, Orders).

## Changes Made

### 1. Case-Insensitive Alert Matching

**Modified `extractAlertKeysFromRule()`** (`XmlParser.java` line ~328):
- Alert names are now normalized to lowercase when extracted for comparison
- Example: "PROBE DISCONNECT" and "Probe Disconnect" are treated as the same alert
- Alert keys stored in lowercase: `keys.add(val.toLowerCase())`

**Modified `filterCoversAlert()`** (`XmlParser.java` line ~437):
- Added case-insensitive comparison for alert filters
- Both filter values and target alerts are normalized to lowercase before comparison
- Supports all relation types: `in`, `equal`, `not_in`, `not_like`, `not_equal`

### 2. Exclusion Operator Support

**Modified `extractFilterData()`** (`XmlParser.java` line ~786):
- Detects exclusion operators: `not_in`, `not_like`, `not_equal`
- For unit.name filters with exclusions:
  - Stores excluded units in `rule.settings.get("excludedUnits")` as a Set
  - Does NOT add excluded units to `rule.units` (only inclusions go there)
- For facility.name filters with exclusions:
  - Skips adding to `rule.facilities` (only inclusions go there)

### 3. Config Group Naming with "Except" Suffix

**Modified `createConfigGroup()`** (`XmlParser.java` line ~1618):
- Added overload that accepts `Set<String> excludedUnits` parameter
- Original signature preserved for backward compatibility
- When exclusions exist:
  - Generates names like `"AllUnits_Except_PACU"` or `"AllUnits_Except_PACU_ICU"`
  - Multiple excluded units are sorted alphabetically and joined with underscores
- Example: `<filter relation="not_in"><path>bed.room.unit.name</path><value>PACU</value></filter>`
  - Creates config group: `"All_Facilities_AllUnits_Except_PACU_NurseCalls"`

### 4. Unit Population for Exclusions

**Modified flow creation methods**:
- `createEscalationFlowFromMap()` (line ~1175)
- `createSimpleFlowFromRule()` (line ~1295)
- `createSimpleFlow()` (line ~1458)

**Logic**:
1. Collect `excludedUnits` from all rules in the group
2. If exclusions exist: clear the `uns` set (don't populate specific unit names)
3. Pass `excludedUnits` to `createConfigGroup()` for proper naming
4. Result: Flows with exclusions show empty unit list but have descriptive config group names

## Example Usage

### XML Configuration
```xml
<view name="AllUnitsExceptPACU_ProbeDisconnect">
  <filter relation="not_in">
    <path>bed.room.unit.name</path>
    <value>PACU</value>
  </filter>
  <filter relation="equal">
    <path>alert_type</path>
    <value>Probe Disconnect</value>
  </filter>
</view>
```

### Resulting Flow
- **Config Group**: `All_Facilities_AllUnits_Except_PACU_Clinicals`
- **Unit List**: Empty (applies to all units except PACU)
- **Alert Name**: "Probe Disconnect" (original case preserved for display)
- **Alert Matching**: Case-insensitive ("PROBE DISCONNECT" also matches)

## Testing

### Test Results
- **Total Tests**: 481
- **Failures**: 0
- **Errors**: 0
- **Skipped**: 0
- **Status**: ✅ **BUILD SUCCESS**

### Key Tests Verified
1. `TimingIssueRegressionTest` - Validates XML parsing with real XMLParser.xml
2. `DataUpdateNotInTest` - Tests NOT_IN filter logic
3. All existing tests continue to pass with new logic

## Technical Details

### Alert Name Case Preservation
- **Display names** (`flow.alarmName`): Preserves original case from XML
- **Comparison keys**: Normalized to lowercase for matching
- **Rationale**: UI shows original formatting, but matching is case-insensitive

### Exclusion Detection
```java
String relation = filter.relation == null ? "" : filter.relation.toLowerCase();
boolean isExclusion = relation.equals("not_in") || 
                     relation.equals("not_like") || 
                     relation.equals("not_equal");
```

### Config Group Naming Pattern
```java
if (excludedUnits != null && !excludedUnits.isEmpty()) {
    StringBuilder unitPart = new StringBuilder("AllUnits_Except");
    List<String> sortedExclusions = new ArrayList<>(excludedUnits);
    Collections.sort(sortedExclusions);
    for (String excluded : sortedExclusions) {
        unitPart.append("_").append(excluded);
    }
    parts.add(unitPart.toString());
}
```

## Datasets Supported

✅ **NurseCalls** - Nurse call alert exclusions  
✅ **Clinicals** - Clinical monitor alert exclusions  
✅ **Orders** - Order alert exclusions  

All three datasets use the same exclusion logic through the common XmlParser infrastructure.

## Files Modified

1. `src/main/java/com/example/exceljson/XmlParser.java`
   - `extractAlertKeysFromRule()` - Line ~328
   - `filterCoversAlert()` - Line ~437
   - `extractFilterData()` - Line ~786
   - `createConfigGroup()` - Line ~1618 (new overload)
   - `createEscalationFlowFromMap()` - Line ~1175
   - `createSimpleFlowFromRule()` - Line ~1295
   - `createSimpleFlow()` - Line ~1458

## Backward Compatibility

✅ All existing functionality preserved  
✅ Original `createConfigGroup(dataset, facilities, units)` signature still works  
✅ Non-exclusion filters continue to work as before  
✅ All 481 existing tests pass without modification  

## Date
2025-11-18
