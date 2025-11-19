# XML Parser First Recipient Issue - Debugging Findings

## Problem
Battery Low and Desat alerts for CDH facility are not storing the first recipient (PRIMARY rule recipient), but 2nd and 3rd recipients are stored correctly.

## Changes Made
1. ✅ Added role extraction from view names (e.g., `Role_PCT_Online_VMP` → "PCT")
2. ✅ Added group extraction from view names (e.g., `Groups_1_caregiver_is_online_with_a_VMP_phone` → "Group 1")
3. ✅ Fixed role extraction from filter values to use only first value from comma-separated lists
4. ✅ Added group detection from destination paths (e.g., `#{bed.room.unit.groups_1.users...}` → "Group 1")
5. ✅ Created comprehensive test (`NorthWesternCDHRecipientTest`) that reproduces the issue

## Root Cause Identified
The PRIMARY SEND rule for CDH Battery Low **is NOT being included in the escalation rule group**.

### Evidence
When processing Battery Low for CDH, the parser creates this group:
```
DEBUG GROUP: Key=Clinicals|Battery Low|CDH|2A,2B,2C,3A,3B,3C,3S,4A,4B,4C, NumRules=4
  - State=Primary, HasDest=false, Purpose=bbb CDH | ESCALATE TO SECONDARY | NO PRIMARY RESPONSE
  - State=Secondary, HasDest=false, Purpose=bbb CDH | ESCALATE TO TERTIARY | NO SECONDARY RESPONSE
  - State=Secondary, HasDest=true, Purpose=bbb CDH | SEND | Battery Low | ... | SECONDARY | RN
  - State=Tertiary, HasDest=true, Purpose=bbb CDH | SEND | Battery Low | ... | TERTIARY FINAL
```

**Missing**: PRIMARY SEND rule with HasDest=true

### The XML Contains the Rule
Line 49088 in `north_western_cdh_test.xml`:
```xml
<rule active="true" dataset="Clinicals" no_loopback="true">
  <purpose>bbb CDH | SEND | Battery Low | GenAdultPM (2A, 2B, 2C, 3A, 3B, 3C, 3S, 4A, 4B, 4C) | PRIMARY | PCT | Normal | VMP</purpose>
  <trigger-on update="true"/>
  <condition>
    <view>Alert_is_BatteryLow</view>
    <view>Role_PCT_Online_VMP</view>
    <view>Alarm_has_not_been_responded_to</view>
    <view>Alarm_is_active</view>
    <view>Alarm_is_at_primary_state</view>
    <view>Facility_is_CDH</view>
    <view>Unit_CDH_GenAdultPM</view>
  </condition>
  <settings>{"destination":"#{bed.locations.assignments.usr.devices.lines.number}"...}</settings>
</rule>
```

### Comparison with Working Rules (Delnor)
Delnor Battery Low rules work correctly and include ALL three SEND rules:
```
DEBUG GROUP: Key=Clinicals|Battery Low|Delnor|CCU, NumRules=5
  - State=Primary, HasDest=false, Purpose=aaa Delnor | ESCALATE TO SECONDARY
  - State=Secondary, HasDest=false, Purpose=aaa Delnor | ESCALATE TO TERTIARY
  - State=Primary, HasDest=true, Purpose=aaa Delnor | SEND | Battery Low | CriticalCarePM (CCU) | PRIMARY | PCT ✅
  - State=Secondary, HasDest=true, Purpose=aaa Delnor | SEND | Battery Low | CriticalCarePM (CCU) | SECONDARY | PCT
  - State=Tertiary, HasDest=true, Purpose=aaa Delnor | SEND | Battery Low | CriticalCarePM (CCU) | TERTIARY FINAL | RN
```

## Hypotheses
The PRIMARY SEND rule is likely being filtered out due to one of these reasons:
1. **Alert types are being removed** during `filterUncoveredAlertTypes()` processing
2. **Alert types are not being extracted** from the `Alert_is_BatteryLow` view in the first place
3. **The rule is being placed in a different group** due to facility/unit mismatch
4. **The rule is being filtered based on some other criteria** (component type, state, etc.)

## Next Steps to Debug
1. Add debug output in `extractFilterData()` to see if alert types are being extracted from the `Alert_is_BatteryLow` view
2. Add debug output before `filterUncoveredAlertTypes()` is called to see what alert types the PRIMARY rule has
3. Check if `canonicalizeAlertName()` is returning null/empty for "Battery Low"
4. Compare how "bbb CDH" rules are processed vs "aaa Delnor" rules

## Test Results
The test `NorthWesternCDHRecipientTest` currently FAILS with:
```
Battery Low Rule:
  Config Group: CDH_2A_Clinicals
  Alarm Name: Battery Low
  R1:                           ← EMPTY (should be PCT)
  T1: Immediate
  R2: VAssign:[Room] RN         ← Correct
  T2: 300
  R3: VAssign:[Room] Charge Nurse  ← Correct
  T3: 300
```

## Files Modified
- `src/main/java/com/example/exceljson/XmlParser.java` - Added role/group extraction logic and extensive debugging
- `src/test/java/com/example/exceljson/NorthWesternCDHRecipientTest.java` - New test that reproduces the issue

## Debug Output Added
- `extractRoleOrGroupFromViewName()` - Extracts roles/groups from view names
- `extractDestination()` - Enhanced to detect group paths
- `extractFilterData()` - Fixed to take first value from comma-separated role lists
- Multiple DEBUG print statements throughout the flow creation process

## To Remove Debug Output
Search for `System.out.println("DEBUG` in `XmlParser.java` and remove those lines once the issue is resolved.
