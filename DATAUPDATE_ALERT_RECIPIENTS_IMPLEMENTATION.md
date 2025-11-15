# DataUpdate Alert Recipients Logic Implementation

## Summary

This implementation addresses the requirements for handling DataUpdate interface rules with global escalation patterns.

## Problem Statement

The requirements were:
1. When DataUpdate interface views don't return alert_type or unit.name values, apply the time to recipients to ALL alerts and ALL units that have more than one state escalation.
2. Treat a state value of "Group" like "Primary" for determining the recipient.
3. When interface rule cannot determine alert recipient, use the destination in the settings.

## Implementation Changes

### 1. Group State Normalization (XmlParser.java:227-232)

Added logic to normalize "Group" state to "Primary" when parsing state values from views:

```java
// Extract state value
if (filter.path != null && filter.path.equals("state")) {
    state = filter.value;
    // Normalize "Group" state to "Primary" as per requirements
    if ("Group".equalsIgnoreCase(state)) {
        state = "Primary";
    }
}
```

**Impact**: Rules with state="Group" are now treated identically to state="Primary" for recipient determination.

### 2. DataUpdate Global Escalation Support (XmlParser.java:362-418)

Modified `mergeStateBasedRules()` to handle DataUpdate escalation rules with no alert_type or unit.name:

**Key Changes**:
- Separates DataUpdate escalation rules with no alert types or units into a "global" list
- These global rules are applied to ALL groups with:
  - Same dataset
  - Multi-state escalation (more than one escalation state)
- Added `hasMultiStateEscalation()` method to check if a group has multiple states

**Logic**:
```java
// Check if this is a DataUpdate escalation rule with no alert types or units
if ("DataUpdate".equalsIgnoreCase(rule.component) && 
    rule.alertTypes.isEmpty() && 
    rule.units.isEmpty() &&
    rule.state != null && !rule.state.isEmpty() &&
    (rule.role == null || rule.role.isEmpty()) &&
    (!rule.settings.containsKey("destination") || 
     rule.settings.get("destination") == null || 
     ((String)rule.settings.get("destination")).isEmpty())) {
    // This is a global DataUpdate escalation rule
    globalDataUpdateRules.add(rule);
}
```

**Impact**: DataUpdate rules without specific alert types or units now apply their timing to all alerts and units in the same dataset with multi-state escalation.

### 3. Recipient Fallback (Already Implemented)

The existing `extractRecipient()` method (XmlParser.java:610-630) already handles fallback to destination in settings:

```java
if (destination != null && !destination.isEmpty()) {
    if (destination.startsWith("g-")) {
        recipient = destination;
    } else if (rule.role != null && !rule.role.isEmpty()) {
        recipient = rule.role;
    } else {
        // When no role is found, use the exact destination value from settings
        recipient = destination;
    }
}
```

**Impact**: When a role cannot be determined, the system falls back to using the destination value from settings.

## Test Coverage

Created comprehensive test suite `DataUpdateGlobalEscalationTest` with 4 tests:

1. **testDataUpdateAppliesToAllAlertsWhenNoAlertTypeSpecified**
   - Verifies that DataUpdate timing applies to all alerts (LHR, HHR)
   - Confirms T2=60 is set for all alerts from global DataUpdate rule

2. **testGroupStateTreatedAsPrimary**
   - Verifies that state="Group" is normalized to "Primary"
   - Confirms CHARGE NURSE is set as R1 (primary recipient)

3. **testDataUpdateOnlyAppliesIfMultipleStates**
   - Verifies global DataUpdate rules only apply to groups with multi-state escalation
   - Confirms flows have both R1 and R2 (multi-state pattern)

4. **testRecipientFallbackToDestination**
   - Verifies recipient extraction works correctly
   - Confirms fallback to destination values when needed

## Test Results

- **Total tests**: 427 (423 existing + 4 new)
- **Failures**: 0
- **Errors**: 0
- **Skipped**: 0

All tests pass successfully.

## Security Analysis

CodeQL analysis: **0 alerts**
- No new security vulnerabilities introduced
- All existing security standards maintained

## Example Use Case

Given the following XML configuration:

```xml
<!-- DataUpdate escalation rule with NO alert_type or unit.name -->
<rule active="true" dataset="Clinicals">
  <purpose>ESCALATE TO SECONDARY | 60 SEC DELAY | ALL ALARMS | ALL UNITS</purpose>
  <defer-delivery-by>60</defer-delivery-by>
  <condition>
    <view>Alarm_is_at_primary_state</view>
  </condition>
  <settings>...</settings>
</rule>

<!-- VMP SEND rules with specific alert types -->
<rule active="true" dataset="Clinicals">
  <condition>
    <view>Alarm_included_in_LowHR_HighHR</view> <!-- alert_type=LHR,HHR -->
    <view>Alarm_is_at_primary_state</view>
  </condition>
</rule>
```

**Before**: DataUpdate timing would NOT apply because groups don't match
**After**: DataUpdate timing (60 seconds) applies to ALL alerts (LHR, HHR) because:
- DataUpdate rule has no alert_type or unit.name
- VMP rules have alert_type=LHR,HHR
- System detects multi-state escalation (Primary -> Secondary)
- Global DataUpdate timing is applied to all matching flows

## Files Modified

1. **src/main/java/com/example/exceljson/XmlParser.java**
   - Added Group state normalization (line 227-232)
   - Modified mergeStateBasedRules() for global DataUpdate support (line 362-418)
   - Added hasMultiStateEscalation() method (line 447-458)

2. **src/test/java/com/example/exceljson/DataUpdateGlobalEscalationTest.java** (new)
   - Comprehensive test coverage for new behavior
   - Documents expected behavior with examples

3. **src/test/resources/test-dataupdate-global-escalation.xml** (new)
   - Test data for global DataUpdate escalation scenarios
   - Includes Group state examples

## Backward Compatibility

- **No breaking changes**: All existing tests (423) continue to pass
- **Additive changes only**: New behavior only activates when:
  - DataUpdate rules have no alert_type or unit.name
  - Groups have multi-state escalation
- **Safe migration**: Existing configurations work unchanged

## Conclusion

This implementation successfully addresses all three requirements from the problem statement:
1. ✅ DataUpdate rules with no alert_type/unit.name apply to ALL alerts/units with multi-state escalation
2. ✅ Group state is treated as Primary state
3. ✅ Recipient fallback to settings destination is supported

The changes are minimal, focused, and fully tested with comprehensive coverage.
