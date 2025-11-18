# Escalation Timing Fix Summary

## New Requirement Addressed

"SET TO PRIMARY | 60SEC | PROBE DISCONNECT | ALARM ACTIVE | ALL UNIT | NORTHLAND" - the "time to first recipient" should be "60" and the VMP SEND should have gone to different recipients at different escalation levels.

## Problem

Previously, when parsing XML configuration with delayed primary state escalation:
1. **First recipient timing (t1) was always "Immediate"** even when DataUpdate CREATE rule specified a 60-second delay
2. **Escalation timing (t2, t3, t4) was empty** - escalation delays were not being captured

Example of the bug:
```
Recipients and Timing:
t1: Immediate | r1: VAssign:[Room] NURSE  ❌ Should be 60
t2:           | r2: VAssign:[Room] CHARGE NURSE  ❌ Should be 60
t3:           | r3: VAssign:[Room] MONITOR TECH  ❌ Should be 60
t4:           | r4: VAssign:[Room] UNIT MANAGER  ❌ Should be 60
```

## Root Cause Analysis

### Issue 1: Initial Delay Ignored
In `createEscalationFlow()`, the code was checking if the first SEND rule had `triggerCreate` and blindly setting t1 to "Immediate". It wasn't considering that a DataUpdate CREATE rule might have specified a delay with `defer-delivery-by=60`.

### Issue 2: Escalation Timing Rules Skipped
In `createFlowRows()`, ALL DataUpdate rules were being skipped at line 679-682:
```java
// Skip DataUpdate rules - they're only used for validation, not for creating flows
if ("DataUpdate".equalsIgnoreCase(rule.component)) {
    continue;
}
```

This meant escalation timing rules (DataUpdate UPDATE rules with `defer-delivery-by` but no destination) were never being added to the grouped rules, so their timing information was lost.

## Solution

### Fix 1: Check DataUpdate CREATE Rules for Initial Delay
Modified `createEscalationFlow()` to check the `dataUpdateRules` parameter for CREATE rules with `defer-delivery-by`:

```java
// Check DataUpdate CREATE rules for initial delay
String initialDelay = null;
for (Rule dataUpdateRule : dataUpdateRules) {
    if (dataUpdateRule.triggerCreate && dataUpdateRule.deferDeliveryBy != null) {
        initialDelay = dataUpdateRule.deferDeliveryBy;
        break;
    }
}

// Later, when setting t1:
if (initialDelay != null) {
    template.t1 = initialDelay;
} else if (sendRule.triggerCreate) {
    template.t1 = sendRule.deferDeliveryBy != null ? sendRule.deferDeliveryBy : "Immediate";
} else {
    template.t1 = "Immediate";
}
```

### Fix 2: Include Escalation Timing Rules in Grouping
Modified `createFlowRows()` to keep DataUpdate rules that have `defer-delivery-by` and no destination (escalation timing rules):

```java
// Skip DataUpdate rules that have destinations (they're interface-like, not escalation timing)
// But KEEP DataUpdate rules with defer-delivery-by and no destination (escalation timing rules)
if ("DataUpdate".equalsIgnoreCase(rule.component)) {
    // Keep escalation timing rules (have defer-delivery-by but no destination)
    if (rule.deferDeliveryBy == null || hasDestination(rule)) {
        continue; // Skip non-escalation DataUpdate rules
    }
    // Fall through to add escalation timing DataUpdate rules to grouped
}
```

## Result

After the fix:
```
=== Probe Disconnect Escalation Flow ===
Alarm Name: Probe Disconnect
Device A: VMP
Config Group: Northland_AllUnits_Clinicals

Recipients and Timing:
t1: 60 | r1: VAssign:[Room] NURSE            ✅ Correct
t2: 60 | r2: VAssign:[Room] CHARGE NURSE    ✅ Correct
t3: 60 | r3: VAssign:[Room] MONITOR TECH    ✅ Correct
t4: 60 | r4: VAssign:[Room] UNIT MANAGER    ✅ Correct
```

## Test Coverage

Created comprehensive test case `DelayedPrimaryEscalationTest` with a realistic scenario:
- DataUpdate CREATE rule with defer-delivery-by=60 setting state to Primary
- DataUpdate UPDATE rules for escalating through Primary → Secondary → Tertiary → Quaternary
- VMP SEND rules for each state with different recipients (NURSE, CHARGE NURSE, MONITOR TECH, UNIT MANAGER)

## Files Changed

1. **src/main/java/com/example/exceljson/XmlParser.java**
   - Modified `createEscalationFlow()` to capture initial delay from DataUpdate CREATE rules
   - Modified `createFlowRows()` to include escalation timing DataUpdate rules in grouping

2. **src/test/java/com/example/exceljson/DelayedPrimaryEscalationTest.java** (new)
   - Comprehensive test for delayed primary escalation

3. **src/test/resources/test-escalation-delayed-primary.xml** (new)
   - Test XML matching real-world "SET TO PRIMARY | 60SEC" scenario

## Validation

✅ All 466 tests pass
✅ DelayedPrimaryEscalationTest passes with correct timing
✅ All existing XmlParser tests still pass
✅ No regressions detected
✅ Security scan: 0 vulnerabilities (from earlier scan)

## Impact

This fix ensures that:
1. **Time to first recipient accurately reflects the DataUpdate CREATE delay** (e.g., 60 seconds instead of Immediate)
2. **All escalation levels show their recipients and timing** (Primary, Secondary, Tertiary, Quaternary all populated)
3. **Real-world scenarios like PROBE DISCONNECT alerts** are correctly parsed with proper escalation timing

## Backward Compatibility

✅ Fully backward compatible:
- Existing flows without delayed primary continue to work (t1 defaults to "Immediate")
- Existing escalation flows continue to work
- No changes to flow creation logic beyond timing capture
