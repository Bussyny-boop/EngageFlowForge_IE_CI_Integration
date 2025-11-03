# Device-B Feature Documentation

## Overview
This feature adds support for a "Device-B" column in the Excel interface and implements logic to combine interfaces when both Device-A and Device-B contain Edge or VCS values.

## Changes Made

### 1. Data Model Updates

#### ExcelParserV5.FlowRow
- Added `deviceB` field to store the Device-B column value from Excel

#### FlowRow.java (JavaFX)
- Added `deviceB` property with getter/setter methods

### 2. Excel Parsing

The parser now recognizes and extracts the "Device - B" column from both:
- Nurse Call sheet
- Patient Monitoring sheet

### 3. GUI Updates (FXML)

Added "Device B" column to both tables in the GUI:
- **Nurse Calls** tab: New column after "Device A"
- **Clinicals** tab: New column after "Device A"

### 4. Interface Logic

The `buildInterfacesForDevice()` method now:
- Accepts both `deviceA` and `deviceB` parameters
- Checks both devices for Edge or VCS keywords
- **Combines interfaces** when both Edge and VCS are present across the two device columns

## Interface Combination Logic

### Scenarios:

1. **Device-A has Edge, Device-B is blank**
   ```json
   "interfaces": [
     {
       "componentName": "OutgoingWCTP",
       "referenceName": "OutgoingWCTP"
     }
   ]
   ```

2. **Device-A is blank, Device-B has VCS**
   ```json
   "interfaces": [
     {
       "componentName": "VMP",
       "referenceName": "VMP"
     }
   ]
   ```

3. **Device-A has Edge, Device-B has VCS** (COMBINED)
   ```json
   "interfaces": [
     {
       "componentName": "OutgoingWCTP",
       "referenceName": "OutgoingWCTP"
     },
     {
       "componentName": "VMP",
       "referenceName": "VMP"
     }
   ]
   ```

4. **Device-A has VCS, Device-B has Edge** (COMBINED - order preserved)
   ```json
   "interfaces": [
     {
       "componentName": "OutgoingWCTP",
       "referenceName": "OutgoingWCTP"
     },
     {
       "componentName": "VMP",
       "referenceName": "VMP"
     }
   ]
   ```

## Excel Column Format

The new column should be added after "Device - A":

| Configuration Group | ... | Device - A | Device - B | Ringtone Device - A | ... |
|---------------------|-----|------------|------------|---------------------|-----|
| Nurse Group 1       | ... | iPhone-Edge | VCS       | Ringtone1          | ... |
| Nurse Group 1       | ... | Edge       |            | Ringtone2          | ... |
| Nurse Group 1       | ... |            | Vocera VCS | Ringtone3          | ... |

## Testing

### New Test Suite: DeviceBTest

Created comprehensive tests to verify:
- ✅ Device-B column parsing from Excel
- ✅ Single device interface generation (Edge or VCS in either column)
- ✅ Combined interface generation when both Edge and VCS are present
- ✅ Custom reference name support with combined interfaces
- ✅ Merged flows mode with combined interfaces

### Test Results
All 133 tests pass, including:
- 4 new DeviceBTest tests
- 4 existing InterfaceReferencesTest tests (backward compatibility)
- All other existing tests

## Usage Example

### In Excel:
1. Add "Device - B" column after "Device - A"
2. Enter device values (e.g., "Edge", "VCS", "iPhone-Edge", "Vocera VCS")
3. If both columns contain Edge/VCS keywords, the generated JSON will include both interfaces

### In GUI:
1. Load Excel file
2. Navigate to "Nurse Calls" or "Clinicals" tab
3. See and edit the new "Device B" column
4. Generate JSON to see combined interfaces in the output

## Backward Compatibility

✅ **Fully backward compatible**
- Existing Excel files without "Device - B" column work as before
- The column is optional - if not present, parser handles gracefully
- All existing tests pass without modification
