# EMDAN Compliance Reclassify Feature Implementation

## Overview
This document describes the implementation of the EMDAN Compliance Reclassify feature, which allows nurse call alarms to be automatically reclassified as clinical alarms based on their EMDAN compliance status.

## Feature Description

### Objective
- Read "EMDAN Compliant? (Y/N)" from Nurse Call & Clinical sheets
- Store value in FlowRow.emdan
- When loading Nurse Call sheet: if emdan is "Y"/"Yes", move the row to clinicals
- Show an editable EMDAN column in the Clinicals tab GUI
- Include EMDAN in "Save As Excel" output

### Implementation Details

#### 1. Data Model Changes (`ExcelParserV5.java`)
- Added `emdan` field to the inner `FlowRow` class to store the EMDAN compliance value
- Field type: `String` (stores raw Excel values like "Y", "Yes", "N", "No", or blank)

#### 2. Excel Parsing (`ExcelParserV5.java`)
- Added column detection for "EMDAN Compliant? (Y/N)" with variants:
  - "EMDAN Compliant? (Y/N)"
  - "EMDAN Compliant"
  - "EMDAN"
- Reads EMDAN value during flow sheet parsing
- Implements reclassification logic:
  - If parsing Nurse Call sheet AND EMDAN value is "Y" or "Yes" (case-insensitive)
  - Row type is set to "Clinicals" and added to clinicals list instead of nurseCalls list
  - Original Nurse Call rows with EMDAN = "N", "No", blank, or other values remain in nurseCalls

#### 3. Helper Method
- Added `isEmdanCompliant(String emdanValue)` method:
  - Returns `true` if value is "Y" or "Yes" (case-insensitive)
  - Returns `false` for any other value (including blank, "N", "No", etc.)
  - Follows same pattern as other boolean parsers in the codebase

#### 4. Excel Export (`ExcelParserV5.java`)
- Updated `flowHeaders()` to include "EMDAN Compliant? (Y/N)" column
- Updated `writeFlowRow()` to write EMDAN value to Excel
- EMDAN column appears after "Genie Enunciation" column
- Preserves EMDAN values during round-trip (load → edit → save)

#### 5. GUI Changes

##### FXML (`App.fxml`)
- Added `clinicalEmdanCol` column to Clinicals tab TableView
- Column appears after "Genie Enunciation" and before timing columns
- Column width: 140.0 pixels
- Column header: "EMDAN Compliant"

##### Controller (`AppController.java`)
- Added `@FXML private TableColumn<ExcelParserV5.FlowRow, String> clinicalEmdanCol;`
- Initialized EMDAN column in `initializeClinicalColumns()` method
- Column is editable (users can change EMDAN values in the GUI)
- Uses standard `setupEditable()` pattern consistent with other columns

#### 6. Testing (`EmdanTest.java`)
Created comprehensive test suite with 5 tests:

1. **testEmdanYesMovesToClinicals**: Verifies that alarms with EMDAN="Y" are moved to Clinicals
2. **testEmdanYesVariant**: Tests "Yes" (full word) variant works correctly
3. **testEmdanBlankStaysInNurseCalls**: Confirms blank EMDAN values keep alarms in NurseCalls
4. **testEmdanCaseInsensitive**: Verifies case-insensitive matching ("y", "YES", "Yes" all work)
5. **testEmdanExcelRoundTrip**: Ensures EMDAN values are preserved during load → save → load cycle

All tests pass successfully (94 total tests including 5 new EMDAN tests).

## Usage Examples

### Excel Input Example

**Nurse Call Sheet:**
```
| Configuration Group | Alarm Name      | ... | EMDAN Compliant? (Y/N) | ... |
|---------------------|-----------------|-----|------------------------|-----|
| Nurse Group 1       | Call Button     | ... | N                      | ... |
| Nurse Group 1       | Bed Exit        | ... | Y                      | ... |
| Nurse Group 1       | IV Pump Alert   | ... | Yes                    | ... |
```

**Result After Loading:**
- "Call Button" → remains in Nurse Calls (EMDAN = N)
- "Bed Exit" → moved to Clinicals (EMDAN = Y)
- "IV Pump Alert" → moved to Clinicals (EMDAN = Yes)

### GUI Display
After loading the above Excel file:
- **Nurse Calls Tab**: Shows 1 alarm (Call Button)
- **Clinicals Tab**: Shows 2 alarms (Bed Exit, IV Pump Alert) with EMDAN column displaying "Y" and "Yes"
- Users can edit EMDAN values directly in the Clinicals tab

### Excel Export
When using "Save Excel (Save As)" feature:
- Both Nurse Call and Patient Monitoring sheets include "EMDAN Compliant? (Y/N)" column
- EMDAN values are preserved exactly as stored in the FlowRow objects
- Round-trip integrity: load → edit → save → load preserves all EMDAN values

## Technical Notes

### Reclassification Logic
The reclassification happens during initial Excel parsing (`parseFlowSheet` method):
```java
// EMDAN Reclassification: if reading from Nurse Call sheet and EMDAN is Y/Yes, move to Clinicals
if (nurseSide && isEmdanCompliant(f.emdan)) {
  f.type = "Clinicals";
  clinicals.add(f);
} else if (nurseSide) {
  nurseCalls.add(f);
} else {
  clinicals.add(f);
}
```

### Case Sensitivity
- EMDAN comparison is case-insensitive
- "Y", "y", "Yes", "YES", "yes" all trigger reclassification
- Only exact matches of "Y" or "Yes" (after normalization) cause reclassification

### Backward Compatibility
- If EMDAN column is missing from Excel, the field remains empty ("")
- Empty EMDAN values are treated as non-compliant (no reclassification occurs)
- Existing Excel files without EMDAN column continue to work normally

### Type Field
- The `type` field in FlowRow is updated to "Clinicals" for reclassified alarms
- This ensures proper JSON generation and display in the GUI
- Original sheet name is not preserved (only the effective type after reclassification)

## Files Modified

1. **src/main/java/com/example/exceljson/ExcelParserV5.java**
   - Added `emdan` field to FlowRow inner class
   - Added EMDAN column detection and parsing
   - Implemented reclassification logic
   - Added `isEmdanCompliant()` helper method
   - Updated Excel export to include EMDAN column

2. **src/main/resources/com/example/exceljson/App.fxml**
   - Added EMDAN column to Clinicals tab

3. **src/main/java/com/example/exceljson/AppController.java**
   - Added `clinicalEmdanCol` field
   - Initialized EMDAN column in `initializeClinicalColumns()`

4. **src/test/java/com/example/exceljson/EmdanTest.java** (new file)
   - Created comprehensive test suite for EMDAN feature
   - 5 tests covering different scenarios

## Testing Results

All tests pass successfully:
```
[INFO] Tests run: 94, Failures: 0, Errors: 0, Skipped: 0
```

Including:
- 89 existing tests (all still passing - no regressions)
- 5 new EMDAN tests (all passing)

## Summary

The EMDAN Compliance Reclassify feature has been successfully implemented with:
- ✅ Excel parsing and column detection
- ✅ Automatic reclassification logic
- ✅ GUI column for editing
- ✅ Excel export support
- ✅ Comprehensive test coverage
- ✅ No breaking changes to existing functionality
- ✅ Case-insensitive matching
- ✅ Round-trip data integrity

The implementation follows existing patterns in the codebase and maintains consistency with other features like Enunciation and Break Through DND.
