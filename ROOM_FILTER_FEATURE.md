# Room Filter Feature - Implementation Summary

## Overview
Added three text input fields under a "Room Filter" label in the GUI that allow users to filter NurseCalls, Clinicals, and Orders flows by room number. When a room number is entered, the corresponding flow type will include a room filter condition in the generated JSON.

## UI Changes

### FXML (App.fxml)
Added a new HBox containing:
- Label: "Room Filter:"
- Label: "Nursecall:" + TextField (prompt: "Room number")
- Label: "Clinical:" + TextField (prompt: "Room number")
- Label: "Orders:" + TextField (prompt: "Room number")

The new section is placed between the "Vocera Badges Alert Interface" controls and the separator line.

### Controller (AppController.java)
Added three new @FXML TextField fields:
- `roomFilterNursecallField`
- `roomFilterClinicalField`
- `roomFilterOrdersField`

Updated methods:
- `applyInterfaceReferences()` - now also applies room filters to parser
- `createFilteredParser()` - copies room filter values to filtered parser

## Backend Changes

### ExcelParserV5.java
Added fields:
- `roomFilterNursecall` - stores nursecall room filter value
- `roomFilterClinical` - stores clinical room filter value  
- `roomFilterOrders` - stores orders room filter value

Added methods:
- `setRoomFilters(String nursecall, String clinical, String orders)` - setter for all three filters
- `buildRoomFilterCondition(String roomValue)` - creates room filter condition for Nursecall/Clinical
- `buildOrdersRoomFilterCondition(String roomValue)` - creates room filter condition for Orders (different structure)

Updated methods:
- `buildFlowsNormal()` - conditionally adds room filter conditions based on flow type
- `buildFlowsMerged()` - conditionally adds room filter conditions based on flow type

## Condition Structures

### Nursecall and Clinical Flows
When a room number is entered (e.g., "305"), the following condition is added:
```json
{
  "filters": [
    {
      "attributePath": "bed.room.room_number",
      "operator": "equal",
      "value": "305"
    }
  ],
  "name": "Room Filter For TT"
}
```

For Nursecalls, this is added AFTER the default NurseCallsCondition.
For Clinicals, this may be the only condition (no default clinical condition).

### Orders Flows
When a room number is entered (e.g., "500"), the following condition is added:
```json
{
  "filters": [
    {
      "attributePath": "patient.current_place.locs.units.rooms.room_number",
      "operator": "in",
      "value": "500"
    }
  ],
  "name": "Room Filter for TT"
}
```

This is added AFTER the Global Condition for Orders flows.

## Behavior

1. **Empty Filter**: If a room filter field is empty or blank, no room filter condition is added to the JSON
2. **Trim Whitespace**: Input values are trimmed to remove leading/trailing whitespace
3. **Flow-Specific**: Each flow type (Nursecall, Clinical, Orders) uses its own room filter value
4. **Merge Mode**: Works in both normal and merged flow modes
5. **Live Update**: Changes to room filter fields are applied when Preview or Export buttons are clicked

## Tests

### RoomFilterTest.java
Comprehensive unit tests covering:
- Nursecall with room filter adds correct condition
- Nursecall without room filter has only default condition
- Clinical with room filter adds correct condition
- Orders with room filter adds condition with different structure

### RoomFilterManualVerificationTest.java
Manual verification tests that output complete JSON for visual inspection:
- Demonstrates Nursecall JSON with room filter
- Demonstrates Clinical JSON with room filter
- Demonstrates Orders JSON with room filter

### RoomFilterUITest.java
UI component test verifying:
- All three TextField components exist
- Prompt text is set correctly
- FXML binding works properly

## Example Usage

1. Load an Excel file with the "Load Excel" button
2. Enter a room number in one or more of the Room Filter fields:
   - Nursecall: "305"
   - Clinical: "410"
   - Orders: "500"
3. Click "Preview Nursecall" to see the JSON with the room filter condition
4. Click "Export Nursecall" to save the JSON to a file

The generated JSON will include the appropriate room filter condition only when a value is entered.
