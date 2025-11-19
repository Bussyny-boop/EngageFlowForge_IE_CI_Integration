# NOT_IN/NOT_LIKE/NOT_EQUAL Exclusion Logic Implementation

## Summary

Implemented support for `NOT_IN`, `NOT_LIKE`, and `NOT_EQUAL` filter operators in the XML parser to handle unit exclusions and case-insensitive alert matching across all datasets (NurseCalls, Clinicals, Orders). Recent iterations extended this work to normalize alert names, eliminate stray `All_Facilities` flows, and keep recipient chains intact across facilities.

## Changes Made

### 1. Case-Insensitive Alert Matching + Canonical Display Names

**Modified `extractAlertKeysFromRule()` & `extractAlertTypesFromRule()`** (`XmlParser.java` lines ~328 & ~560):
- Alert names/alert types are normalized to lowercase for comparisons, but we now maintain a canonical display form via `canonicalizeAlertName()`
- Example: `"PROBE DISCONNECT"`, `"Probe Disconnect"`, and `"probe disconnect"` all normalize to the same key while the nicest casing is preserved for UI/JSON output

**New helpers** (`XmlParser.java` lines ~360-430):
- `normalizeAlertType()` and `ruleContainsAlertType()` centralize case-insensitive comparisons when inheriting timing/facilities from DataUpdate CREATE rules
- `getDisplayAlertName()` ensures every flow uses the canonical friendly casing regardless of the source rule’s capitalization

**Modified `filterCoversAlert()` / `coversAlertTypes()` / `dataUpdateRuleCoversAlertType()`** (`XmlParser.java` lines ~460-720 & ~840):
- Filter values and targets are all normalized before comparison, allowing `not_in`, `not_like`, and `not_equal` relations to behave consistently even if XML mixes cases
- Eliminates false negatives where DataUpdate entries used uppercase alert names while SEND rules used Title Case

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
### 5. Facility Inheritance & All_Facilities Guardrails

**Grouping logic** (`XmlParser.java` lines ~960-1110):
- DataUpdate CREATE rules are now hashed by `dataset + normalizedAlertType`
- When a SEND rule lacks explicit facilities, the parser inspects matching CREATE rules (case-insensitive) and emits one flow per facility, preventing duplicate `All_Facilities` rows
- `ruleContainsAlertType()` guarantees facility/timing inheritance still works even if CREATE rules use uppercase alert names and SEND rules do not

**Flow builders** (`createEscalationFlow*`, `createSimpleFlow*` lines ~1120-1530):
- Lookups for initial timing and facility-specific delays also use `ruleContainsAlertType()`
- Flow templates use `getDisplayAlertName()` so all downstream JSON rows reflect the canonical name chosen during parsing (no more mixed-case `Probe Disconnect` vs `PROBE DISCONNECT`)

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
- **Total Tests**: 483
- **Failures**: 0
- **Errors**: 0
- **Skipped**: 0
- **Status**: ✅ **BUILD SUCCESS**

### Key Tests Verified
1. `TimingIssueRegressionTest` - Validates XML parsing with real XMLParser.xml
2. `DataUpdateNotInTest` - Tests NOT_IN filter logic
3. `NotInExclusionSendRuleTest` - Verifies SEND rules are only processed when CREATE DataUpdate rule covers the alert type and does NOT exclude it via NOT_IN
   - **Test Case 1**: SEND rule for "Code Blue" is NOT processed when DataUpdate CREATE has `not_in` for "Code Blue"
   - **Test Case 2**: SEND rule for "Probe Disconnect" IS processed when DataUpdate CREATE has `not_in` for "Code Blue" (i.e., Probe Disconnect is not excluded)
4. All existing tests continue to pass with new logic

## Technical Details

### Alert Name Case Preservation
- **Display names** (`flow.alarmName` / `flow.sendingName`): We now select the best-cased variant observed per dataset/alert (`getDisplayAlertName`), so outputs stay human-friendly even if some XML entries are all caps
- **Comparison keys**: Still normalized to lowercase for deterministic matching and facility inheritance
- **Rationale**: UI receives consistent casing while logic remains case-insensitive

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
