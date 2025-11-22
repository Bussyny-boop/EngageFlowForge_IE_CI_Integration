# Voice Group and Clear All Fixes - Implementation Summary

## Date: November 22, 2025

## Overview
This document describes three critical fixes implemented for the FlowForge application:
1. Load Voice Group header detection
2. Cell height constraint for voice group validation
3. Preserve Merge Engage Rules settings when using Clear All

---

## Fix 1: Load Voice Group Header Detection

### Problem Statement
The "Load Voice Group" feature always extracted data from Column A (first column), regardless of whether the file had a "Group Name" header in a different column.

### Requirements
- If a loaded file has a header named "Group Name", extract the column data from that header's column
- If "Group Name" is not present, fall back to extracting data from Column A
- Support both Excel (.xlsx, .xls) and CSV (.csv) files
- Header detection must be case-insensitive ("Group Name", "group name", "GROUP NAME" all match)

### Implementation Details

#### CSV Files
The CSV parsing logic now:
1. Reads the first line and checks if any column contains "Group Name" (case-insensitive)
2. If found, records the column index and marks the first line as a header (skip it)
3. If not found, treats the first line as data
4. Reads all subsequent lines from the determined column index

```java
String headerLine = br.readLine();
int groupNameColumn = 0; // Default to column 0 (Column A)

// Check if first line contains "Group Name" header (case-insensitive)
if (headerLine != null) {
    String[] headers = headerLine.split(",");
    boolean hasGroupNameHeader = false;
    
    for (int i = 0; i < headers.length; i++) {
        if (headers[i].trim().equalsIgnoreCase("Group Name")) {
            groupNameColumn = i;
            hasGroupNameHeader = true;
            break;
        }
    }
    
    // If no "Group Name" header found, treat first line as data
    if (!hasGroupNameHeader) {
        String[] parts = headerLine.split(",");
        if (parts.length > groupNameColumn && !parts[groupNameColumn].trim().isEmpty()) {
            groups.add(parts[groupNameColumn].trim());
        }
    }
}

// Read remaining lines from the determined column
String line;
while ((line = br.readLine()) != null) {
    String[] parts = line.split(",");
    if (parts.length > groupNameColumn && !parts[groupNameColumn].trim().isEmpty()) {
        groups.add(parts[groupNameColumn].trim());
    }
}
```

#### Excel Files
The Excel parsing logic now:
1. Reads the first row and checks if any cell contains "Group Name" (case-insensitive)
2. If found, records the column index and starts reading data from row 1 (skipping header)
3. If not found, starts reading data from row 0
4. Reads all data rows from the determined column index

```java
int groupNameColumn = 0; // Default to column 0 (Column A)
int startRow = 0;

// Check first row for "Group Name" header (case-insensitive)
if (sheet.getPhysicalNumberOfRows() > 0) {
    Row headerRow = sheet.getRow(0);
    if (headerRow != null) {
        boolean hasGroupNameHeader = false;
        DataFormatter formatter = new DataFormatter();
        
        for (int i = 0; i < headerRow.getLastCellNum(); i++) {
            Cell cell = headerRow.getCell(i);
            if (cell != null) {
                String headerValue = formatter.formatCellValue(cell).trim();
                if (headerValue.equalsIgnoreCase("Group Name")) {
                    groupNameColumn = i;
                    hasGroupNameHeader = true;
                    startRow = 1; // Skip header row
                    break;
                }
            }
        }
        
        // If no "Group Name" header found, start from row 0
        if (!hasGroupNameHeader) {
            startRow = 0;
        }
    }
}

// Read data from the determined column
DataFormatter formatter = new DataFormatter();
for (int i = startRow; i < sheet.getPhysicalNumberOfRows(); i++) {
    Row row = sheet.getRow(i);
    if (row != null) {
        Cell cell = row.getCell(groupNameColumn);
        if (cell != null) {
            String val = formatter.formatCellValue(cell).trim();
            if (!val.isEmpty()) {
                groups.add(val);
            }
        }
    }
}
```

### Test Coverage
Created two comprehensive test files:

1. **CsvHeaderDetectionLogicTest.java** - Unit tests for CSV parsing logic
   - ✅ CSV with "Group Name" header in first column
   - ✅ CSV without "Group Name" header (fallback to Column A)
   - ✅ CSV with "Group Name" header in second column
   - ✅ Case-insensitive header detection (lowercase, uppercase, mixed case)
   - ✅ Handling of empty lines and edge cases
   - ✅ Empty CSV files
   - ✅ CSV with only header (no data)

2. **VoiceGroupHeaderDetectionTest.java** - Integration tests with actual files
   - ✅ Excel files with "Group Name" header in Column A
   - ✅ Excel files with "Group Name" header in Column B
   - ✅ Excel files without "Group Name" header (fallback behavior)
   - ✅ Case-insensitive header detection
   - ✅ Empty Excel files
   - ✅ Excel files with empty cells

### Example Usage

**File with "Group Name" header (extracted from Column B):**
```csv
ID,Group Name,Description
1,Cardiology,Heart specialists
2,Neurology,Brain specialists
```
Result: Loads `Cardiology` and `Neurology` (not the IDs)

**File without "Group Name" header (extracted from Column A):**
```csv
OB Nurses
ICU Staff
Surgery Team
```
Result: Loads all three groups including the first row

---

## Fix 2: Cell Height Constraint for Voice Group Validation

### Problem Statement
When "Load Voice Group" was activated, all rows containing cells with the "VGroup" or "Group" keyword were expanding in height, making the table difficult to read and navigate.

### Root Cause
The `TextFlow` component used for validation rendering was not properly constrained in height, causing cells to grow to accommodate multi-line content or long text.

### Implementation Details

Enhanced the `createValidatedCellGraphic()` method in AppController.java with:

1. **Added minimum height constraint:**
   ```java
   flow.setMinHeight(24);
   ```

2. **Added clipping region to prevent overflow:**
   ```java
   // Clip content that exceeds the height to prevent cell expansion
   javafx.scene.shape.Rectangle clip = new javafx.scene.Rectangle();
   clip.widthProperty().bind(flow.widthProperty());
   clip.setHeight(24);
   flow.setClip(clip);
   ```

The complete height constraint implementation:
```java
// Use a single TextFlow constrained to cell height
TextFlow flow = new TextFlow();
flow.setPadding(new Insets(2, 5, 2, 5)); // Small padding for readability
flow.setLineSpacing(0);
// Constrain the TextFlow to prevent cell expansion
flow.setMaxHeight(24);
flow.setPrefHeight(24);
flow.setMinHeight(24);
// Clip content that exceeds the height to prevent cell expansion
javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle();
clip.widthProperty().bind(flow.widthProperty());
clip.setHeight(24);
flow.setClip(clip);
```

### Benefits
- ✅ All cells maintain consistent 24px height
- ✅ No vertical expansion regardless of content
- ✅ Overflow content is clipped (not visible)
- ✅ Table remains easy to read and navigate
- ✅ Scrolling through rows is smooth and predictable

### Visual Impact
**Before:** Rows with validated voice groups would expand vertically, creating uneven row heights
**After:** All rows maintain uniform 24px height, creating a clean, professional table layout

---

## Fix 3: Preserve Merge Engage Rules Default Settings

### Problem Statement
The "Clear All" button was clearing ALL settings, including the "Merge Engage Rules" checkboxes, which should maintain their default state.

### Requirements
According to the FXML file (`App.fxml`), the default state for "Merge Engage Rules" is:
- ❌ Standard (No Merge) - `noMergeCheckbox` = `false`
- ❌ Merge Multiple Config Groups - `mergeByConfigGroupCheckbox` = `false`
- ✅ Merge by Single Config Group - `mergeAcrossConfigGroupCheckbox` = `true` (selected by default)

### Implementation Details

Modified the `clearAllData()` method in AppController.java:

**Before:**
```java
// Clear merge-related checkboxes
if (noMergeCheckbox != null) noMergeCheckbox.setSelected(false);
if (mergeByConfigGroupCheckbox != null) mergeByConfigGroupCheckbox.setSelected(false);
if (mergeAcrossConfigGroupCheckbox != null) mergeAcrossConfigGroupCheckbox.setSelected(false);
if (combineConfigGroupCheckbox != null) combineConfigGroupCheckbox.setSelected(false);
```

**After:**
```java
// Restore merge-related checkboxes to their default settings (not cleared)
// Default: mergeAcrossConfigGroupCheckbox is selected (as per FXML)
if (noMergeCheckbox != null) noMergeCheckbox.setSelected(false);
if (mergeByConfigGroupCheckbox != null) mergeByConfigGroupCheckbox.setSelected(false);
if (mergeAcrossConfigGroupCheckbox != null) mergeAcrossConfigGroupCheckbox.setSelected(true);
if (combineConfigGroupCheckbox != null) combineConfigGroupCheckbox.setSelected(false);
```

### Behavior
When the user clicks "Clear All":
- ✅ All loaded data is cleared (units, nurse calls, clinicals, orders)
- ✅ Voice groups are cleared
- ✅ Load button states are reset
- ✅ Interface reference names are reset to defaults
- ✅ Default interface checkboxes are cleared
- ✅ **"Merge Engage Rules" checkboxes are RESTORED to defaults** (not cleared)
- ✅ Room filter fields are cleared
- ✅ Tables are refreshed

### User Impact
Users can now safely use "Clear All" without losing their preferred Merge Engage Rules setting. The default "Merge by Single Config Group" option remains selected after clearing, matching the application's initial state.

---

## Files Modified

1. **AppController.java** (3 changes)
   - `loadVoiceGroups()` - Added CSV and Excel header detection logic
   - `createValidatedCellGraphic()` - Added cell height constraints and clipping
   - `clearAllData()` - Preserve Merge Engage Rules default settings

## Files Added

1. **CsvHeaderDetectionLogicTest.java** - Unit tests for CSV parsing logic
2. **VoiceGroupHeaderDetectionTest.java** - Integration tests with file I/O

---

## Testing

### Manual Testing Steps

#### Test Fix 1: Header Detection
1. Create a CSV file with "Group Name" in Column B:
   ```csv
   ID,Group Name,Description
   1,Cardiology,Heart specialists
   2,Neurology,Brain specialists
   ```
2. Open Settings → Voice Group Validation
3. Click "Load Voice Group" and select the CSV file
4. ✅ **Expected:** Stats show "2 groups loaded" (not 3)
5. ✅ **Expected:** Only "Cardiology" and "Neurology" are loaded (not "1" or "2")

#### Test Fix 2: Cell Height
1. Load a voice groups file with several groups
2. In a recipient column, enter multi-line text with voice groups:
   ```
   VGroup: TeamA, VGroup: TeamB, VGroup: TeamC, VGroup: TeamD
   ```
3. ✅ **Expected:** Cell height remains at 24px, no vertical expansion
4. ✅ **Expected:** Text is clipped if it exceeds cell height
5. Scroll through the table
6. ✅ **Expected:** All rows have uniform height, smooth scrolling

#### Test Fix 3: Merge Settings Preservation
1. Load an Excel file with data
2. Verify "Merge by Single Config Group" is selected (default)
3. Click "Clear All" and confirm
4. ✅ **Expected:** All data is cleared
5. ✅ **Expected:** "Merge by Single Config Group" is STILL selected
6. ✅ **Expected:** Other merge checkboxes remain unselected

### Automated Testing
Run the test suite to verify all changes:
```bash
mvn test -Dtest=CsvHeaderDetectionLogicTest
mvn test -Dtest=VoiceGroupHeaderDetectionTest
```

---

## Backward Compatibility

All changes maintain backward compatibility:

1. **Header Detection:** Files without "Group Name" header continue to work as before (Column A extraction)
2. **Cell Height:** Non-validated cells remain unchanged
3. **Merge Settings:** Default behavior matches original FXML configuration

---

## Known Limitations

1. **Header Detection:** Only looks for exact match of "Group Name" (case-insensitive). Similar headers like "Group_Name" or "GroupName" will not match.
2. **Cell Height Clipping:** Content that exceeds 24px height is clipped and not visible. Users should use shorter text or multi-line editing.
3. **CSV Parsing:** Uses simple comma-splitting. Does not handle quoted commas within values.

---

## Future Enhancements

Potential improvements for future releases:

1. Support for alternative header names (configurable pattern matching)
2. Dynamic cell height based on content (with max limit)
3. Advanced CSV parsing with proper quote handling
4. Visual indicator when content is clipped
5. Tooltip showing full content on hover for clipped cells

---

## Related Documentation

- `VOICE_GROUP_FEATURE.md` - Voice Group validation feature overview
- `VOICE_GROUP_VALIDATION_FIXES.md` - Previous validation fixes
- `VISUAL_GUIDE_TOPBAR_SETTINGS.md` - Settings UI documentation

---

## Author

Implementation completed by GitHub Copilot on November 22, 2025
