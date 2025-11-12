# POD Room Filter Feature - Implementation Summary

## Overview
Added a new optional column "Filter for POD Rooms (Optional)" to the Unit Breakdown sheet. When values are entered in this column, the application adds a filter condition to NurseCalls and Clinicals flows in the JSON output, restricting those flows to specific room numbers.

## Changes Made

### 1. Data Model Updates (`ExcelParserV5.java`)
- Added `podRoomFilter` field to the `UnitRow` class to store the filter value from Excel
- Updated unit maps to include the `podRoomFilter` field in all unit reference maps

### 2. Excel Parsing Updates
- Modified `parseUnitBreakdown()` to read the new column "Filter for POD Rooms (Optional)"
- Updated `rebuildUnitMaps()` to propagate the POD room filter value to unit maps
- Column lookup supports both "Filter for POD Rooms (Optional)" and "Filter for POD Rooms"

### 3. JSON Generation
Created `buildPodRoomFilterCondition()` method that:
- Returns `null` if no filter value is provided (preserves default behavior)
- Strips special characters from the input, keeping only alphanumeric characters, spaces, and commas
- Generates a filter condition with:
  - `attributePath`: "bed.room.room_number"
  - `operator`: "in"
  - `value`: cleaned filter value (e.g., "POD 1" or "POD 1, POD 2")
  - `name`: "POD rooms filter"

### 4. Flow Building Updates
Updated both `buildFlowsNormal()` and `buildFlowsMerged()` to:
- Extract the POD room filter from unit references
- Add the POD room filter condition after other conditions
- Apply only to NurseCalls and Clinicals flows (not Orders)

## JSON Output Example

### NurseCalls Flow with POD Filter
```json
{
  "conditions": [
    {
      "filters": [
        {
          "attributePath": "bed",
          "operator": "not_null"
        },
        {
          "attributePath": "to.type",
          "operator": "not_equal",
          "value": "TargetGroups"
        }
      ],
      "name": "NurseCallsCondition"
    },
    {
      "filters": [
        {
          "attributePath": "bed.room.room_number",
          "operator": "in",
          "value": "POD 1"
        }
      ],
      "name": "POD rooms filter"
    }
  ]
}
```

### Clinicals Flow with POD Filter
```json
{
  "conditions": [
    {
      "filters": [
        {
          "attributePath": "bed.room.room_number",
          "operator": "in",
          "value": "POD A, POD B, POD C"
        }
      ],
      "name": "POD rooms filter"
    }
  ]
}
```

## Feature Behavior

### When Filter is Provided
- Parses the value from the "Filter for POD Rooms (Optional)" column
- Strips special characters (e.g., "POD #1" becomes "POD 1")
- Adds a condition with the "in" operator to the flow's conditions array
- Supports comma-separated values for multiple rooms

### When Filter is Empty or Missing
- No POD room filter condition is added
- Flows continue to work as they did before (default behavior)
- This makes the feature truly optional with no impact on existing configurations

### Scope
- Applies to: **NurseCalls** and **Clinicals** flows
- Does NOT apply to: **Orders** flows
- Works in both normal and merge modes

## Testing

### Unit Tests (`PodRoomFilterTest.java`)
1. `nursecallPodRoomFilterAddsCondition` - Verifies NurseCalls flows get POD filter
2. `clinicalPodRoomFilterAddsCondition` - Verifies Clinicals flows get POD filter with comma-separated values
3. `noPodRoomFilterWhenColumnEmpty` - Verifies empty column doesn't add filter
4. `podRoomFilterStripsSpecialCharacters` - Verifies special character removal
5. `noPodRoomFilterWhenColumnMissing` - Verifies missing column doesn't break existing behavior

### Manual Verification Tests (`PodRoomFilterManualVerificationTest.java`)
- Demonstrates NurseCalls JSON with POD filter "POD 1"
- Demonstrates Clinicals JSON with POD filter "POD A, POD B, POD C"
- Shows the complete JSON structure for visual inspection

### Test Results
- All 5 new tests pass
- All 399 existing tests continue to pass
- No regressions detected

## Security
- CodeQL scan: **0 alerts** (no security vulnerabilities)
- Input sanitization: Special characters are stripped to prevent injection
- Backward compatibility: Empty/missing column preserves default behavior

## Excel Sheet Structure

The Unit Breakdown sheet should include the column after "Common Unit Name":

| Facility | Common Unit Name | Filter for POD Rooms (Optional) | Nurse Call Configuration Group | ... |
|----------|------------------|----------------------------------|-------------------------------|-----|
| TestFac  | ICU             | POD 1                           | ICU-Group                     | ... |
| TestFac  | CCU             | POD A, POD B                    | CCU-Group                     | ... |

## Key Design Decisions

1. **"in" operator**: Unlike the existing room filters that use "equal", this filter uses "in" to support comma-separated values
2. **Special character stripping**: Removes characters like `#`, `@`, etc., to clean user input
3. **Optional behavior**: Empty/missing column has zero impact on existing flows
4. **NurseCalls and Clinicals only**: Orders flows have different structure and weren't in scope
5. **Unit-level filter**: Applied at the unit level, so all flows for that unit get the same filter

## Compatibility
- ✅ Works with existing Excel templates (column is optional)
- ✅ Backward compatible (no changes to default behavior)
- ✅ Works with both normal and merge modes
- ✅ Compatible with all other features (room filters, custom tabs, etc.)
