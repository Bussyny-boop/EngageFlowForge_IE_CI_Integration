# Save to NDW Feature - Technical Guide

## Overview
The "Save to NDW" feature is a CI (Clinical Informatics) mode exclusive functionality that allows users to save their modifications back to the original NDW Excel file while maintaining data integrity and highlighting changes.

## Version Information
- **Application Version**: 3.1.0
- **Feature Status**: Fully Implemented
- **Mode Availability**: CI Mode Only

## Key Features

### 1. Change Tracking System ✅
**Implementation**: `ExcelParserV5.java`

Every data row (UnitRow and FlowRow) includes a `changedFields` Set that tracks which fields have been modified:

```java
public static final class UnitRow {
    // ... field declarations
    public final Set<String> changedFields = new LinkedHashSet<>();
}

public static final class FlowRow {
    // ... field declarations
    public final Set<String> changedFields = new LinkedHashSet<>();
}
```

**How It Works**:
- When a user edits a cell in the UI, the corresponding field name is added to the `changedFields` Set
- Only fields in this Set are updated when saving to NDW
- Unchanged fields preserve their original values and formatting

### 2. Selective Cell Updates ✅
**Implementation**: `ExcelParserV5.updateCellIfChanged()`

The update method only modifies cells that have been changed:

```java
private void updateCellIfChanged(Row row, int columnIndex, String value, 
                                 String fieldName, Object dataRow) {
    Set<String> changedFields = null;
    
    // Extract changed fields set from the data row
    if (dataRow instanceof UnitRow unitRow) {
        changedFields = unitRow.changedFields;
    } else if (dataRow instanceof FlowRow flowRow) {
        changedFields = flowRow.changedFields;
    }
    
    // Only update if this field was changed
    if (changedFields != null && changedFields.contains(fieldName)) {
        Cell cell = row.getCell(columnIndex);
        // ... update cell value
        applyChangedCellFormatting(cell);
    }
    // If not changed, don't update the cell (preserve original)
}
```

**Benefits**:
- ✅ Preserves original data for unchanged cells
- ✅ Maintains Excel formulas (formula cells are skipped)
- ✅ Keeps original cell formatting for unchanged cells
- ✅ Ensures data integrity

### 3. Visual Change Highlighting ✅
**Implementation**: `ExcelParserV5.applyChangedCellFormatting()`

Changed cells are formatted with:
- **Bold** text
- **Italic** text
- **Red** color (IndexedColors.RED)

```java
private void applyChangedCellFormatting(Cell cell) {
    Workbook wb = cell.getSheet().getWorkbook();
    
    // Create cached style and font if not already created
    if (changedCellStyle == null) {
        changedCellStyle = wb.createCellStyle();
        changedCellFont = wb.createFont();
        
        // Set bold, italic, and red color
        changedCellFont.setBold(true);
        changedCellFont.setItalic(true);
        changedCellFont.setColor(IndexedColors.RED.getIndex());
        
        changedCellStyle.setFont(changedCellFont);
    }
    
    // Apply the cached style to the cell
    cell.setCellStyle(changedCellStyle);
}
```

**Why Cached Styles?**:
- Excel has a limit of ~4000 cell styles per workbook
- Using a cached style for all changed cells prevents exceeding this limit
- All changed cells share the same style object

### 4. Save Operation Workflow
**Implementation**: `AppController.saveOnNdw()`

1. **Validation**:
   - Checks if parser is loaded
   - Verifies current Excel file exists
   
2. **Confirmation**:
   - Shows dialog with file path
   - User must confirm overwrite
   
3. **Data Sync**:
   - Calls `syncEditsToParser()` to sync UI changes to parser data
   
4. **Excel Update**:
   - Calls `parser.updateExcel(currentExcelFile)`
   - Updates only changed cells with formatting
   - Preserves all unchanged data and formulas
   
5. **Success Feedback**:
   - Shows success message
   - Updates status label

## Profile System Integration

### IE Mode (Implementation Engineer)
- ❌ "Save to NDW" button is **HIDDEN**
- ✅ Standard "Save" and "Save As" buttons available
- ✅ Full export functionality
- ✅ Profile switcher visible (can switch to CI mode)

### CI Mode (Clinical Informatics)
- ✅ "Save to NDW" button is **VISIBLE** and **ENABLED**
- ✅ Standard "Save" button still available
- ❌ Export JSON functionality hidden
- ❌ Profile switcher **HIDDEN** (cannot switch back to IE mode)

## Profile Switching and Data Preservation

### Switching from IE to CI ✅
**Implementation**: `AppController.toggleProfile()`

When switching profiles:
1. User profile setting is updated (`userProfile = UserProfile.CI`)
2. UI restrictions are applied (`applyProfileRestrictions()`)
3. **ALL LOADED DATA IS PRESERVED**:
   - Parser data remains intact
   - Table data is not cleared
   - Filters and settings are maintained
   - Change tracking continues to work

**Data Preservation Guarantee**:
```java
private void toggleProfile(boolean isIEMode) {
    // Update user profile
    userProfile = isIEMode ? UserProfile.IE : UserProfile.CI;
    
    // Update button text
    updateProfileSwitcherText();
    
    // Apply profile restrictions (show/hide buttons, enable/disable features)
    applyProfileRestrictions();
    
    // NO DATA CLEARING OR RELOADING
    // Parser, tables, and all loaded information remain intact
}
```

### One-Way Switching (IE → CI Only)
**Requirement**: Users can switch from IE to CI, but NOT from CI back to IE

**Implementation**:
```java
private void applyProfileRestrictions() {
    if (userProfile == UserProfile.CI) {
        // ... other CI restrictions
        
        // Hide profile switcher in CI mode (users cannot switch back to IE from CI)
        if (profileSwitcher != null) {
            profileSwitcher.setVisible(false);
            profileSwitcher.setManaged(false);
        }
    } else {
        // ... other IE settings
        
        // IE mode: ensure profile switcher is visible (allows switching to CI)
        if (profileSwitcher != null) {
            profileSwitcher.setVisible(true);
            profileSwitcher.setManaged(true);
            profileSwitcher.setDisable(false);
        }
    }
}
```

## Technical Details

### Files Updated by Save to NDW
1. **Unit Breakdown Sheet**:
   - Facility, Unit Names, POD Room Filter
   - Nurse Group, Clinical Group, Orders Group
   - No Caregiver Group, Comments

2. **Nurse Call Sheet**:
   - In Scope, Config Group, Alarm Name, Sending Name
   - Priority, Devices, Ringtone, Response Options
   - Timing, Recipients, and all configuration fields

3. **Patient Monitoring Sheet**:
   - Same structure as Nurse Call
   - Plus EMDAN field

4. **Orders Sheet**:
   - Same structure as Nurse Call

### Formula Cell Protection
Formula cells are explicitly skipped to preserve Excel calculations:

```java
// Skip formula cells to preserve them
if (cell.getCellType() == CellType.FORMULA) {
    return; // Don't update formula cells
}
```

## User Experience Flow

### Complete CI Workflow with Save to NDW

1. **Start Application** → Select "CI" profile
2. **Choose Workflow** → "Validate NDW"
3. **Load NDW File** → Select Excel file
4. **Load Validation Data** (Optional):
   - Voice Groups
   - Assignment Roles
   - Bed List
5. **Begin Validation** → Review data
6. **Edit Data** → Make changes in tables
   - Changes are tracked in `changedFields` Set
7. **Save to NDW** → Click "💾 Save to NDW" button
   - Confirm overwrite
   - Only changed cells are updated
   - Changed cells are marked bold, italic, red
8. **Result** → Original NDW file updated with changes highlighted

## Benefits Summary

✅ **Data Integrity**: Only changed cells are modified
✅ **Visual Clarity**: Changed cells are clearly marked
✅ **Formula Safety**: Excel formulas are preserved
✅ **Efficient Updates**: No unnecessary cell modifications
✅ **Profile Flexibility**: Can switch IE → CI with data preserved
✅ **Change Tracking**: Automatic tracking of all modifications
✅ **One-Way Workflow**: CI mode prevents accidental profile switching
✅ **Performance**: Cached styles prevent Excel limitations

## Testing

All existing tests pass (630 tests, 0 failures):
- Change tracking system tested
- Profile switching tested
- Data preservation verified
- UI restrictions validated

## Future Enhancements

Potential improvements (not in current scope):
- Change history log
- Undo/redo for changes
- Export change report
- Cell-level change approval workflow
