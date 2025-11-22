# Data Validation Features Implementation Guide

## Overview
This document describes the three data validation features implemented in the FlowForge application:
1. **Voice Group Validation** (Enhanced)
2. **Assignment Roles Validation** (NEW)
3. **Bed List Validation** (NEW)

## Changes Summary

### 1. Row Expansion Fix
**Issue**: Previously, cells containing validated data (Voice Groups) were constrained to a fixed height of 24 pixels, causing content to be clipped.

**Solution**: Removed the `VALIDATED_CELL_HEIGHT` constraint and updated the cell rendering logic to allow TextFlow components to expand dynamically based on content. This ensures all multi-line data is fully visible.

**Files Modified**:
- `AppController.java`: Removed height constraints from `createValidatedCellGraphic()` method and cell factory

### 2. Voice Group Validation (Enhanced)
**Location**: Settings drawer → Data Validation → Voice Group Validation

**Features**:
- Load voice groups from Excel (.xlsx, .xls) or CSV files
- Validates cells containing "VGroup:" or "Group:" keywords
- Visual feedback: Valid groups in black, invalid groups in red
- Auto-complete suggestions while typing
- Rows now expand to show all content

**File Format**:
- Looks for "Group Name" header (case-insensitive)
- If no header found, reads from first column (Column A)

**Example CSV**:
```csv
Group Name
Code Blue
Rapid Response
OB Nurse
ICU Nurse
```

### 3. Assignment Roles Validation (NEW)
**Location**: Settings drawer → Data Validation → Assignment Roles Validation

**Features**:
- Load assignment roles from Excel or CSV files
- Validates cells containing "VAssign:" keyword followed by role name
- Syntax: `VAssign: [RoleName]` or `VAssign:`
- Visual feedback: Valid roles in black, invalid roles in red
- Auto-complete suggestions while typing
- Context-aware: Shows role suggestions when typing after "VAssign:"

**File Format**:
- Requires "Name" header (case-insensitive)
- Reads role names from the "Name" column

**Example CSV**:
```csv
Name
Room 101
Room 102
ICU Pod A
ED Zone 1
```

**Usage Example**:
```
VAssign: Room 101         ✓ (Valid - black text)
VAssign: InvalidRoom      ✗ (Invalid - red text)
VAssign:                  ✓ (Valid syntax without role)
```

### 4. Bed List Validation (NEW)
**Location**: Settings drawer → Data Validation → Bed List Validation

**Features**:
- Load unit/department list from Excel or CSV files
- Validates unit names in the **Units tab** against the loaded list
- No keywords required - validates entire cell content
- Visual feedback: Valid units in black, invalid units in red
- Supports multi-line unit names (comma-separated becomes newline-separated)

**File Format**:
- Looks for "Department" or "Unit" header (case-insensitive)
- Reads unit names from the matching column

**Example CSV**:
```csv
Department,Bed
ICU,Bed 1
ED,Bed 2
Medical/Surgical,Bed 3
Oncology,Bed 4
```

**Validation Target**:
- Column: "Unit Names" in the Units tab
- Each unit name is validated separately (supports multi-line)

## UI Layout

The Data Validation section in the Settings drawer is organized as follows:

```
Data Validation
├─ Voice Group Validation
│  ├─ Load Voice Group [Button]
│  ├─ Clear Loaded Groups [Button]
│  └─ [Status: X groups loaded]
│
├─ Assignment Roles Validation
│  ├─ Load AssignmentRoles [Button]
│  └─ [Status: X roles loaded]
│
└─ Bed List Validation
   ├─ Load Bed List [Button]
   └─ [Status: X units loaded]
```

## Implementation Details

### New Classes
1. **AssignmentRoleValidator** (`util/AssignmentRoleValidator.java`)
   - Similar structure to VoiceGroupValidator
   - Pattern: `(?i)(VAssign:\s*)([^,;\n]+)?`
   - Supports multi-line validation
   - Case-insensitive validation

### Modified Classes
1. **AppController.java**
   - Added fields for assignment roles and bed list data
   - New methods: `loadAssignmentRoles()`, `loadBedList()`
   - Enhanced `createValidatedCellGraphic()` to support multiple validation types
   - Enhanced `setupAutoComplete()` for context-aware suggestions
   - New method: `setupEditableUnitWithBedListValidation()` for Units tab

2. **App.fxml**
   - Added UI elements for new validation buttons
   - Reorganized under "Data Validation" section
   - Added stats labels for each validation type

### Button Loading States
All validation buttons support:
- Loading state (spinner/progress indicator)
- Loaded state (checkmark)
- Auto-clear after timeout (configurable)
- Tooltip showing last load time

## Testing

### Unit Tests
- **AssignmentRoleValidatorTest**: 6 comprehensive tests
  - Valid role validation
  - Invalid role validation
  - Multi-line validation
  - Case-insensitive validation
  - Mixed content handling
  - Empty role handling

### Test Coverage
- All 585 tests passing
- No regressions introduced
- CodeQL security scan: 0 vulnerabilities

## Usage Workflow

### Typical Workflow for Voice Groups
1. Click "Load Voice Group" in Settings
2. Select Excel/CSV file with voice group names
3. Navigate to Nurse Calls/Clinicals/Orders tab
4. Type in recipient columns (Device A, Device B, Recipients)
5. Use "VGroup: " or "Group: " prefix
6. See validation colors (red = invalid, black = valid)
7. Use auto-complete for suggestions

### Typical Workflow for Assignment Roles
1. Click "Load AssignmentRoles" in Settings
2. Select Excel/CSV file with role names (must have "Name" column)
3. Navigate to Nurse Calls/Clinicals/Orders tab
4. Type in recipient columns
5. Use "VAssign: " prefix followed by role name
6. See validation colors (red = invalid, black = valid)
7. Use auto-complete for suggestions

### Typical Workflow for Bed List
1. Click "Load Bed List" in Settings
2. Select Excel/CSV file with unit/department names
3. Navigate to Units tab
4. Edit "Unit Names" column
5. See validation colors (red = invalid, black = valid)
6. Each line is validated separately

## Validation Behavior

### Voice Group & Assignment Roles
- Only cells containing the respective keywords are validated
- Keywords are case-insensitive
- Invalid entries show in red text
- Valid entries show in normal text color
- Supports multi-line entries (each line validated separately)
- Auto-complete shows top 5 matches

### Bed List
- Validates all content in "Unit Names" column when bed list is loaded
- No keywords required
- Comma-separated values converted to newlines
- Each unit name validated separately
- Invalid units show in red text

## File Format Requirements

### Voice Groups File
- **Required**: At least one column
- **Optional Header**: "Group Name" (case-insensitive)
- **Fallback**: Reads from Column A if no header found

### Assignment Roles File
- **Required Header**: "Name" (case-insensitive)
- **Note**: If "Name" column not found, no roles are loaded

### Bed List File
- **Required Header**: "Department" OR "Unit" (case-insensitive)
- **Note**: If neither column found, no units are loaded

## Auto-Complete Behavior

### Context Detection
- After "VAssign:" → Shows assignment role suggestions
- After "VGroup:" or "Group:" → Shows voice group suggestions
- Matches partial text (minimum 2 characters)
- Shows top 5 matches sorted by:
  1. Starts with search term
  2. Alphabetical order

### Keyboard Shortcuts
- **Enter**: Insert selected suggestion
- **Escape**: Close suggestion popup
- **Tab**: Close popup and move to next cell

## Error Handling

### Loading Errors
- File not found or unreadable
- Invalid file format
- Missing required headers (for Assignment Roles and Bed List)
- Empty file

All errors display user-friendly messages via `showError()` dialog.

## Performance Considerations

### Validation Performance
- Validation runs on cell update/render
- Synchronized access to loaded data sets
- TextFlow components used for efficient rendering
- No performance impact on tables without validated data

### Memory Usage
- Sets used for O(1) lookup performance
- Case-insensitive comparison via iteration (acceptable for typical sizes)
- Data cleared when validation type is unloaded

## Security

### CodeQL Scan Results
- **0 vulnerabilities** found
- All file I/O uses proper try-with-resources
- No SQL injection risks (no database access)
- Input validation on all user-provided data

## Future Enhancements

Potential improvements for consideration:
1. Import/export validation data sets
2. Persistent storage of loaded validation data
3. Bulk validation across all rows
4. Validation statistics/reports
5. Custom validation rules editor
6. Integration with external data sources

## Support

For issues or questions about the data validation features:
1. Check this documentation
2. Review the test cases in `AssignmentRoleValidatorTest.java`
3. Examine the sample CSV files in `/tmp/`
4. Refer to the original Voice Group feature documentation

## Version History

- **v3.0.0** - Initial implementation of all three validation features
  - Voice Group validation (enhanced with row expansion)
  - Assignment Roles validation (new)
  - Bed List validation (new)
