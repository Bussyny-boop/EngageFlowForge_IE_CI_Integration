# Break Through DND Feature

## Overview

The "Break Through DND" feature allows users to explicitly control the `breakThrough` parameter in generated Engage JSON files via an Excel column, overriding the previous automatic priority-based logic.

## Motivation

Previously, the `breakThrough` parameter was automatically set based on the Priority column:
- **Urgent** priority → `breakThrough: "voceraAndDevice"`
- **Normal/High** priority → `breakThrough: "none"`

This automatic behavior didn't provide enough flexibility for healthcare workflows where break-through behavior needs to be configured independently of priority levels.

## Implementation

### Excel Column

A new column "Break Through DND" has been added to both:
- **Nurse Call** sheet
- **Patient Monitoring** sheet

The column accepts the following values:
- `voceraAndDevice` - Breaks through DND on both Vocera devices and other devices
- `device` - Breaks through DND on devices only
- `none` - Does not break through DND
- *(empty)* - Falls back to priority-based logic for backward compatibility

### Column Position

The "Break Through DND" column should be placed after "Response Options" and before the time/recipient columns. The exact column letter doesn't matter as the parser uses header name matching.

Example header row structure:
```
Configuration Group | Alarm Name | ... | Response Options | Break Through DND | Time to 1st Recipient | 1st Recipient | ...
```

### Backward Compatibility

If the "Break Through DND" column:
- Does not exist in the Excel file
- Exists but the cell is empty for a given row

Then the system falls back to the original priority-based logic:
- Urgent → `voceraAndDevice`
- Normal/High → `none`

This ensures existing Excel files continue to work without modification.

### JSON Output

The Excel value is written directly to the `breakThrough` parameter in the generated JSON:

```json
{
  "name": "breakThrough",
  "value": "\"voceraAndDevice\""
}
```

## Usage

### In Excel

1. Add a column header "Break Through DND" to your Nurse Call and/or Patient Monitoring sheets
2. For each alarm, enter the desired break-through behavior:
   - `voceraAndDevice` for urgent alerts that must break through all DND modes
   - `device` for device-only break-through
   - `none` for alerts that respect DND settings
   - Leave empty to use priority-based default

### In the GUI

The "Break Through DND" column appears in both the Nurse Calls and Clinicals tabs and can be edited directly in the table view.

### Save/Load

When saving Excel files from the GUI or re-exporting, the "Break Through DND" column is preserved with the user's configured values.

## Examples

### Example 1: Override Urgent Alert to Not Break Through

**Excel:**
```
Alarm Name       | Priority | Break Through DND
Patient Vitals   | Urgent   | none
```

**Result:** Even though the priority is Urgent, the alert will NOT break through DND because the Excel column explicitly sets it to "none".

### Example 2: Force Normal Alert to Break Through

**Excel:**
```
Alarm Name         | Priority | Break Through DND
Equipment Warning  | Normal   | voceraAndDevice
```

**Result:** Even though the priority is Normal, the alert WILL break through DND on all devices because the Excel column explicitly sets it to "voceraAndDevice".

### Example 3: Use Default Behavior

**Excel:**
```
Alarm Name     | Priority | Break Through DND
Status Update  | High     | (empty)
```

**Result:** Falls back to priority-based logic. Since priority is High (not Urgent), breakThrough will be "none".

## Testing

The feature includes comprehensive test coverage in `BreakThroughDNDTest.java`:

1. ✅ Excel value overrides priority-based logic
2. ✅ Supports all valid breakThrough values (none, device, voceraAndDevice)
3. ✅ Empty column falls back to priority logic
4. ✅ Missing column falls back to priority logic
5. ✅ Works for both Nurse Call and Clinical flows
6. ✅ Save/load cycle preserves values

All tests pass, confirming the feature works correctly for all scenarios.

## Migration Guide

### For Existing Excel Files

No changes required. Existing files will continue to work with priority-based behavior.

### To Use the New Feature

1. Open your Excel configuration file
2. Add a new column "Break Through DND" after "Response Options"
3. Fill in values for rows where you want explicit control
4. Leave cells empty for rows that should use the default priority-based behavior
5. Save and re-generate JSON

## Technical Details

### Code Changes

- **FlowRow.java**: Added `breakThroughDND` JavaFX property
- **ExcelParserV5.java**: 
  - Added `breakThroughDND` field to internal FlowRow model
  - Updated `parseFlowSheet` to read the column
  - Modified `buildParamAttributesQuoted` to use Excel value with fallback
  - Updated `flowHeaders` and `writeFlowRow` for Excel export
- **App.fxml**: Added column definitions for both tables
- **AppController.java**: Added field declarations and bindings

### Priority Mapping Reference

For reference, the priority-based fallback uses this mapping:
- `Urgent` → `voceraAndDevice`
- `High`, `Normal`, `Low`, or any other value → `none`

The mapping is case-insensitive.
