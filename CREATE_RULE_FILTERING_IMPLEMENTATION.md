# CREATE Rule Filtering Implementation Summary

## Overview
This implementation ensures that SEND (adapter/interface) rules ONLY process alert types, facilities, and units that are explicitly defined in CREATE rules (DataUpdate or Adapter with create=true).

## Problem Statement
The system needed to validate that:
1. SEND rules can only process alert types defined in CREATE rules
2. SEND rules can only process facilities defined in CREATE rules
3. SEND rules can only process units defined in CREATE rules
4. Negation relations (NOT_IN, NOT_EQUAL, not_like) are properly supported for exclusion logic

## Solution

### Architecture
The validation logic is implemented in `XmlParser.java` in the `shouldProcessRule()` method, which is called during the parsing of interface rules to determine if a SEND rule should be processed.

### Key Components

#### 1. Facility Filtering
- **extractFacilitiesFromRule()**: Extracts facility names from a SEND rule's condition views
- **isFacilityPath()**: Checks if a filter path refers to facility.name
- **dataUpdateRuleCoversFacilities()**: Validates that at least one CREATE rule covers the SEND rule's facilities
- **filterCoversFacility()**: Checks if a single facility filter covers a specific facility
  - Handles positive relations (IN, EQUAL)
  - Handles negative relations (NOT_IN, NOT_EQUAL, not_like)

#### 2. Unit Filtering
- **extractUnitsFromRule()**: Extracts unit names from a SEND rule's condition views
- **isUnitPath()**: Checks if a filter path refers to unit.name
- **dataUpdateRuleCoversUnits()**: Validates that at least one CREATE rule covers the SEND rule's units
- **filterCoversUnit()**: Checks if a single unit filter covers a specific unit
  - Handles positive relations (IN, EQUAL)
  - Handles negative relations (NOT_IN, NOT_EQUAL, not_like)

#### 3. Integration with shouldProcessRule()
The `shouldProcessRule()` method now performs three validations:
1. Alert type validation (existing)
2. Facility validation (new)
3. Unit validation (new)

A SEND rule is only processed if ALL three validations pass.

### Relation Handling

#### Positive Relations (IN, EQUAL)
- The filter value is a list of allowed items
- A target item is covered if it's IN the list
- Example: `facility.name IN "Hospital A"` means only "Hospital A" is allowed

#### Negative Relations (NOT_IN, NOT_EQUAL, not_like)
- The filter value is a list of excluded items
- A target item is covered if it's NOT IN the list
- Example: `unit.name NOT_IN "Psych"` means all units EXCEPT "Psych" are allowed

### Empty Filter Handling
If a CREATE rule has NO facility or unit filters, it means the rule applies to ALL facilities/units, so the validation returns `true` (matches any facility/unit).

## Test Coverage

### Test File: FacilityUnitFilteringTest.java
Four test cases validate the filtering behavior:

1. **testFacilityAndUnitFilteringWithPositiveFilters**
   - Verifies that only correct facility/unit combinations are processed
   - Tests that invalid facilities and units are filtered out

2. **testNotInRelationForUnits**
   - Verifies that NOT_IN relation works correctly
   - Tests that units NOT in the exclusion list are processed

3. **testInvalidFacilityFiltered**
   - Verifies that SEND rules with facilities not in CREATE rules are filtered out

4. **testInvalidUnitFiltered**
   - Verifies that SEND rules with units not in CREATE rules are filtered out

### Test XML: test-facility-unit-filtering.xml
The test XML defines scenarios with:
- Positive filtering (specific facilities and units)
- Negative filtering (NOT_IN relation for units)
- Invalid combinations that should be filtered out

## Examples

### Example 1: Positive Filtering
```xml
<!-- CREATE rule defines: Need RN for Hospital A, ICU only -->
<rule active="true" dataset="NurseCalls">
  <purpose>CREATE | Need RN | Hospital A | ICU</purpose>
  <trigger-on create="true"/>
  <condition>
    <view>Alert_type_Need_RN</view>
    <view>Facility_Hospital_A</view>
    <view>Unit_ICU</view>
  </condition>
</rule>

<!-- SEND rule 1: VALID - Hospital A, ICU -->
<rule active="true" dataset="NurseCalls">
  <purpose>SEND | Need RN | Hospital A | ICU</purpose>
  <trigger-on update="true"/>
  <condition>
    <view>Alert_type_Need_RN</view>
    <view>Facility_Hospital_A</view>
    <view>Unit_ICU</view>
  </condition>
</rule>
✅ PROCESSED

<!-- SEND rule 2: INVALID - Hospital B not in CREATE -->
<rule active="true" dataset="NurseCalls">
  <purpose>SEND | Need RN | Hospital B | ICU</purpose>
  <trigger-on update="true"/>
  <condition>
    <view>Alert_type_Need_RN</view>
    <view>Facility_Hospital_B</view>
    <view>Unit_ICU</view>
  </condition>
</rule>
❌ FILTERED OUT

<!-- SEND rule 3: INVALID - ER not in CREATE -->
<rule active="true" dataset="NurseCalls">
  <purpose>SEND | Need RN | Hospital A | ER</purpose>
  <trigger-on update="true"/>
  <condition>
    <view>Alert_type_Need_RN</view>
    <view>Facility_Hospital_A</view>
    <view>Unit_ER</view>
  </condition>
</rule>
❌ FILTERED OUT
```

### Example 2: Negative Filtering (NOT_IN)
```xml
<!-- CREATE rule defines: Call Button for Hospital A, all units EXCEPT Psych -->
<rule active="true" dataset="NurseCalls">
  <purpose>CREATE | Call Button | Hospital A | All Units except Psych</purpose>
  <trigger-on create="true"/>
  <condition>
    <view>Alert_type_Call_Button</view>
    <view>Facility_Hospital_A</view>
    <view>Unit_NOT_Psych</view> <!-- NOT_IN relation -->
  </condition>
</rule>

<!-- SEND rule 1: VALID - ICU is NOT Psych -->
<rule active="true" dataset="NurseCalls">
  <purpose>SEND | Call Button | Hospital A | ICU</purpose>
  <trigger-on update="true"/>
  <condition>
    <view>Alert_type_Call_Button</view>
    <view>Facility_Hospital_A</view>
    <view>Unit_ICU</view>
  </condition>
</rule>
✅ PROCESSED (ICU is NOT in the exclusion list)

<!-- SEND rule 2: VALID - ER is NOT Psych -->
<rule active="true" dataset="NurseCalls">
  <purpose>SEND | Call Button | Hospital A | ER</purpose>
  <trigger-on update="true"/>
  <condition>
    <view>Alert_type_Call_Button</view>
    <view>Facility_Hospital_A</view>
    <view>Unit_ER</view>
  </condition>
</rule>
✅ PROCESSED (ER is NOT in the exclusion list)

<!-- SEND rule 3: INVALID - Psych IS Psych -->
<rule active="true" dataset="NurseCalls">
  <purpose>SEND | Call Button | Hospital A | Psych</purpose>
  <trigger-on update="true"/>
  <condition>
    <view>Alert_type_Call_Button</view>
    <view>Facility_Hospital_A</view>
    <view>Unit_Psych</view>
  </condition>
</rule>
❌ FILTERED OUT (Psych IS in the exclusion list)
```

## Validation Flow

```
SEND Rule Processing:
┌────────────────────────┐
│ Parse SEND rule        │
└───────────┬────────────┘
            │
            ▼
┌────────────────────────┐      Yes    ┌──────────────────┐
│ Extract alert types,   │─────────────▶│ Find CREATE      │
│ facilities, units      │              │ rules in dataset │
└───────────┬────────────┘              └────────┬─────────┘
            │                                    │
            │No alert types/facilities/units     │
            │                                    ▼
            ▼                          ┌────────────────────┐
┌────────────────────────┐             │ Validate alert     │
│ Skip validation        │             │ types covered      │
│ (no filters)           │             └────────┬───────────┘
└────────────────────────┘                      │
                                                ▼
                                       ┌────────────────────┐
                                       │ Validate           │
                                       │ facilities covered │
                                       └────────┬───────────┘
                                                │
                                                ▼
                                       ┌────────────────────┐
                                       │ Validate units     │
                                       │ covered            │
                                       └────────┬───────────┘
                                                │
                                    ┌───────────┴───────────┐
                                    │                       │
                              All covered            Not covered
                                    │                       │
                                    ▼                       ▼
                           ┌────────────────┐    ┌─────────────────┐
                           │ PROCESS RULE   │    │ FILTER OUT RULE │
                           └────────────────┘    └─────────────────┘
```

## Impact

### Clinical Safety
- Ensures SEND rules can only process alerts for authorized facilities and units
- Prevents accidental delivery of alerts to unintended facilities/units
- Supports exclusion logic for flexible configuration

### Backward Compatibility
- All existing tests pass (465 tests)
- No breaking changes to existing functionality
- New filtering is transparent to existing configurations without facility/unit filters

### Performance
- Minimal performance impact (validation happens once during rule parsing)
- Efficient set operations for matching

## Files Modified
1. `src/main/java/com/example/exceljson/XmlParser.java` - Added filtering logic
2. `src/test/java/com/example/exceljson/FacilityUnitFilteringTest.java` - New test suite
3. `src/test/resources/test-facility-unit-filtering.xml` - Test scenarios

## Test Results
- Total tests: 469
- Passed: 469
- Failed: 0
- CodeQL vulnerabilities: 0

## Conclusion
The implementation successfully adds facility and unit filtering to ensure SEND rules only process alerts for facilities and units defined in CREATE rules, with full support for negation relations (NOT_IN, NOT_EQUAL, not_like).
