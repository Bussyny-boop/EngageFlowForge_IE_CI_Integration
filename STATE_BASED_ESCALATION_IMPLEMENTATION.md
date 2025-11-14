# State-Based Escalation Implementation

## Overview
This document describes the implementation of state-based escalation with view cross-referencing in the XmlParser.

## Problem Statement
The XML parser needed to support state-based escalation patterns where:
- Multiple rules with different states (Primary, Secondary, Tertiary, Quaternary) are merged into a single flow row
- Timing for each recipient is determined by the escalation rule for the previous state
- Recipients are determined by SEND rules that match each state
- Views in rules are cross-referenced with view definitions in datasets

## Solution

### Key Components

#### 1. RuleData Class
A new internal class that collects all rule information before creating flows:
```java
private static class RuleData {
    String dataset;
    String purpose;
    String component;
    String deferDeliveryBy;
    boolean isCreate;
    boolean isUpdate;
    List<String> viewNames;
    Map<String, Object> settings;
    Set<String> alertTypes;
    Set<String> facilities;
    Set<String> units;
    String role;
    String state; // Primary, Secondary, Tertiary, Quaternary
}
```

#### 2. Two-Phase Processing
The parser now processes rules in two phases:

**Phase 1: Collection**
- Parse all active rules (skip rules with active="false")
- Extract view information and cross-reference with dataset views
- Identify state values from views with path="state"
- Identify roles from views with path ending in "role.name"
- Store all rule data in `collectedRules` list

**Phase 2: Merging**
- Group rules by dataset and units (not alert types, since escalation rules don't have them)
- Detect state-based escalation patterns
- Merge SEND and ESCALATE rules into consolidated flow rows

#### 3. State Mapping Logic
According to requirements:
- **state=Primary** determines time to 2nd recipient (T2)
- **state=Secondary** determines time to 3rd recipient (T3)
- **state=Tertiary** determines time to 4th recipient (T4)
- **state=Quaternary** determines time to 5th recipient (T5)

This is implemented by:
- Primary state escalation rule (defer-delivery-by=30) → sets T2=30
- Secondary state escalation rule (defer-delivery-by=60) → sets T3=60
- Tertiary state escalation rule (defer-delivery-by=90) → sets T4=90
- Quaternary state escalation rule (defer-delivery-by=120) → sets T5=120

#### 4. Rule Types
**SEND Rules**: Rules with destination or role
- Provide recipient information
- Applied to R1, R2, R3, R4 based on state

**ESCALATE Rules**: Rules with defer-delivery-by but no destination/role
- Provide timing information
- The state in the condition determines which T column to set

### Example

Given these XML rules:

```xml
<!-- SEND PRIMARY -->
<rule active="true" dataset="Clinicals">
  <trigger-on create="true"/>
  <condition>
    <view>Alarm_is_at_primary_state</view> <!-- state=Primary -->
    <view>Primary_role_caregiver_is_online</view> <!-- role.name=Primary Caregiver -->
  </condition>
  <settings>{"destination":"..."}</settings>
</rule>

<!-- ESCALATE TO SECONDARY -->
<rule active="true" dataset="Clinicals">
  <trigger-on update="true"/>
  <defer-delivery-by>30</defer-delivery-by>
  <condition>
    <view>Alarm_is_at_primary_state</view> <!-- state=Primary -->
  </condition>
  <settings>{"parameters":[{"path":"state","value":"Secondary"}]}</settings>
</rule>

<!-- SEND SECONDARY -->
<rule active="true" dataset="Clinicals">
  <trigger-on update="true"/>
  <condition>
    <view>Alarm_is_at_secondary_state</view> <!-- state=Secondary -->
    <view>Secondary_role_caregiver_is_online</view> <!-- role.name=Secondary Caregiver -->
  </condition>
  <settings>{"destination":"..."}</settings>
</rule>
```

The parser produces a single merged flow:
- T1 = "Immediate", R1 = "Primary Caregiver" (from PRIMARY SEND rule)
- T2 = "30", R2 = "Secondary Caregiver" (T2 from PRIMARY ESCALATE rule, R2 from SECONDARY SEND rule)
- T3, T4, etc. follow the same pattern

## Implementation Details

### View Cross-Referencing
1. Parse all `<view>` elements in `<datasets>` to build view definitions
2. When parsing rules, extract view names from `<condition>` elements
3. Look up each view name in the dataset's view map
4. Extract state values from filters with `path="state"`
5. Extract role values from filters with `path` ending in `role.name"`

### Rule Grouping
Rules are grouped using a key of `dataset|units`:
- Alert types are NOT included in the key (escalation rules don't have them)
- This allows SEND and ESCALATE rules to be grouped together
- Within each group, rules are analyzed for state-based patterns

### Merging Algorithm
1. Separate rules into SEND and ESCALATE categories
2. Find all unique alert types from SEND rules
3. For each alert type:
   - Create a single flow row
   - Process states in order: Primary, Secondary, Tertiary, Quaternary
   - For each state:
     - Find SEND rule for that state → extract recipient (Rx)
     - Find ESCALATE rule with that state in condition → extract time (Tx+1)
4. Apply settings from the first SEND rule encountered

### Inactive Rule Handling
Rules with `active="false"` are skipped during the parsing phase:
```java
String active = ruleElement.getAttribute("active");
if ("false".equalsIgnoreCase(active)) {
    continue;
}
```

## Testing

### Test Coverage
Three comprehensive tests verify the implementation:

1. **testStateBasedEscalationParsing**: Verifies complete escalation chain
2. **testInactiveRulesAreSkipped**: Ensures inactive rules are excluded
3. **testViewCrossReferencing**: Validates view matching and settings application

### Test Data
`test-state-escalation.xml` contains:
- 4 SEND rules (Primary, Secondary, Tertiary, Quaternary states)
- 3 ESCALATE rules (Primary→Secondary, Secondary→Tertiary, Tertiary→Quaternary)
- 1 inactive rule (should be skipped)
- 2 alert types (APNEA, ASYSTOLE)
- Expected result: 2 merged flows with complete escalation chains

## Impact

### No Breaking Changes
- All existing tests (415) continue to pass
- Non-escalation rules work exactly as before
- Backward compatible with existing XML files

### Security
- CodeQL analysis: 0 alerts
- No new security vulnerabilities introduced
- Input validation maintained

## Files Modified

1. **XmlParser.java**
   - Added RuleData class
   - Modified parseRule() to collect rules
   - Added mergeStateBasedRules() and supporting methods
   - Updated clear() to clear collectedRules

2. **StateBasedEscalationTest.java** (new)
   - Comprehensive test coverage
   - Documents expected behavior

3. **test-state-escalation.xml** (new)
   - Test data for state-based escalation
   - Includes all escalation states and edge cases

## Future Enhancements

Potential improvements for future consideration:
1. Support for more than 4 escalation levels
2. Custom state names beyond Primary/Secondary/Tertiary/Quaternary
3. Parallel escalation paths (multiple recipients at same level)
4. Time-based conditions beyond simple defer-delivery-by
