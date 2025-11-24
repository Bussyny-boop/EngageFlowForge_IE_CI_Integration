# Save to NDW Fix - Implementation Summary

## Problem Statement
The "Save to NDW" button on the CI profile was only saving data into cells that were empty. Users wanted to update non-empty cells as well when changes were made, but didn't want cells that didn't receive any changes to update the NDW.

## Root Cause
The bug was in `AppController.trackFieldChange()` method which had this logic:

```java
String originalValue = unitRow.originalValues.get(fieldName);
if (originalValue != null) {  // ❌ Bug: skips tracking if null
    // Compare and track changes...
}
```

This prevented change tracking when:
1. Fields didn't have an original value stored (e.g., dynamically added custom columns)
2. Fields were added after the Excel file was loaded

## Solution

### 1. Fixed Change Tracking (AppController.java)
Changed the null check to use `getOrDefault("")`:

```java
// Before:
String originalValue = unitRow.originalValues.get(fieldName);
if (originalValue != null) { ... }

// After:
String originalValue = unitRow.originalValues.getOrDefault(fieldName, "");
// Now always tracks changes, treating missing values as ""
```

### 2. Added Custom Column Tracking (AppController.java, line ~2995)
Updated the `setOnEditCommit` for dynamic custom columns:

```java
newColumn.setOnEditCommit(ev -> {
    ExcelParserV5.UnitRow row = ev.getRowValue();
    String oldValue = row.customGroups.get(customTabName);
    String newValue = ev.getNewValue();
    row.customGroups.put(customTabName, newValue);
    
    // NEW: Track changes for custom group columns
    String fieldName = "customGroup_" + customTabName;
    trackFieldChange(row, fieldName, oldValue, newValue);
    
    if (parser != null) {
        parser.rebuildUnitMaps();
    }
    tableUnits.refresh();
});
```

### 3. Enhanced Unit Sheet Saving (ExcelParserV5.java, updateUnitSheet method)
Added logic to save custom group columns:

```java
// Update custom group columns
for (Map.Entry<String, String> customGroup : unit.customGroups.entrySet()) {
  String customTabName = customGroup.getKey();
  String customGroupValue = customGroup.getValue();
  
  int customColIndex = getCol(hm, customTabName);
  if (customColIndex >= 0) {
    String fieldName = "customGroup_" + customTabName;
    updateCellIfChanged(row, customColIndex, customGroupValue, fieldName, unit);
  }
}
```

## Testing

### New Tests
Created `ChangeTrackingWithoutOriginalValueTest.java` with 2 tests:
1. `testChangeTrackingWorksWhenOriginalValueNotSet()` - Verifies fields without original values are tracked
2. `testNonEmptyOriginalValueIsStillTracked()` - Ensures non-empty cells update correctly

### Test Results
- **Total tests**: 632
- **Passing**: 632 ✅
- **Failing**: 0
- **Build**: SUCCESS ✅

### Code Quality
- **Code review**: Passed with no comments ✅
- **Security scan (CodeQL)**: 0 vulnerabilities ✅

## Verification Scenarios

| Scenario | Original Value | User Edit | Previous Behavior | New Behavior |
|----------|---------------|-----------|-------------------|--------------|
| Empty cell filled | "" | "Value" | ✅ Saved | ✅ Saved |
| Non-empty changed | "ABC" | "XYZ" | ❌ NOT saved | ✅ Saved |
| Custom column | "Group A" | "Group B" | ❌ NOT saved | ✅ Saved |
| No original value | (not set) | "New" | ❌ NOT saved | ✅ Saved |
| Unchanged | "ABC" | "ABC" | ✅ Not updated | ✅ Not updated |

## Files Changed
1. `src/main/java/com/example/exceljson/AppController.java`
   - Fixed `trackFieldChange()` to use `getOrDefault("")`
   - Added change tracking to custom column edit callback

2. `src/main/java/com/example/exceljson/ExcelParserV5.java`
   - Enhanced `updateUnitSheet()` to save custom group columns

3. `src/test/java/com/example/exceljson/ChangeTrackingWithoutOriginalValueTest.java`
   - New test class with comprehensive coverage

## Impact
✅ **Non-empty cells now update** - The main bug is fixed  
✅ **Custom columns work** - Dynamically added columns are tracked  
✅ **Unchanged cells preserved** - Data integrity maintained  
✅ **Visual feedback** - Changed cells marked bold, italic, red  
✅ **No breaking changes** - All existing tests pass  
✅ **Security verified** - No vulnerabilities introduced  

## How to Verify the Fix

### Manual Testing Steps
1. Open the application in CI mode
2. Load an NDW Excel file
3. Edit a cell that has an existing value (non-empty)
4. Click "Save to NDW"
5. Open the saved Excel file
6. Verify the cell was updated with bold, italic, red formatting

### Automated Testing
```bash
mvn clean test
# All 632 tests should pass
```

## Technical Notes

The fix follows these principles:
- **Minimal changes**: Only modified the necessary logic
- **Backwards compatible**: All existing functionality preserved
- **Well tested**: Comprehensive test coverage
- **Security conscious**: No vulnerabilities introduced
- **Documentation**: Code comments explain the changes

## Conclusion

The "Save to NDW" button now correctly tracks and saves all field changes, including:
- Non-empty cells that are edited
- Dynamically added custom group columns  
- Fields without original values stored

Unchanged cells are still properly preserved, maintaining data integrity.

**Status**: ✅ COMPLETE
**Tests**: ✅ 632/632 passing
**Security**: ✅ No vulnerabilities
**Build**: ✅ SUCCESS
