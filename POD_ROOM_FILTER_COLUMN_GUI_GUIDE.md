# POD Room Filter Column - Visual Guide

## Overview
This change adds the "Filter for POD Rooms (Optional)" column to the Units table in the GUI, making it visible and editable.

## GUI Change Location
The new column appears in the **Units** tab, positioned immediately after the "Unit Names" column.

## Table Layout - Before

```
┌──────────────┬─────────────┬──────────────┬─────────────────┬──────────────┬────────────────────┬──────────┐
│   Facility   │ Unit Names  │ Nurse Group  │ Clinical Group  │ Orders Group │ No Caregiver Group │ Comments │
├──────────────┼─────────────┼──────────────┼─────────────────┼──────────────┼────────────────────┼──────────┤
│ Test Hosp    │ ICU         │ ICU-Nurse    │ ICU-Clinical    │              │                    │          │
│ Test Hosp    │ CCU         │ CCU-Nurse    │ CCU-Clinical    │              │                    │          │
└──────────────┴─────────────┴──────────────┴─────────────────┴──────────────┴────────────────────┴──────────┘
```

## Table Layout - After

```
┌──────────────┬─────────────┬──────────────────────────────┬──────────────┬─────────────────┬──────────────┬────────────────────┬──────────┐
│   Facility   │ Unit Names  │ Filter for POD Rooms (Opt.)  │ Nurse Group  │ Clinical Group  │ Orders Group │ No Caregiver Group │ Comments │
├──────────────┼─────────────┼──────────────────────────────┼──────────────┼─────────────────┼──────────────┼────────────────────┼──────────┤
│ Test Hosp    │ ICU         │ POD 1                        │ ICU-Nurse    │ ICU-Clinical    │              │                    │          │
│ Test Hosp    │ CCU         │ POD A, POD B                 │ CCU-Nurse    │ CCU-Clinical    │              │                    │          │
│ Test Hosp    │ ER          │                              │ ER-Nurse     │ ER-Clinical     │              │                    │          │
└──────────────┴─────────────┴──────────────────────────────┴──────────────┴─────────────────┴──────────────┴────────────────────┴──────────┘
                                         ↑
                                    NEW COLUMN
                          (Right after "Unit Names")
```

## Column Properties
- **Column ID**: `unitPodRoomFilterCol`
- **Column Title**: "Filter for POD Rooms (Optional)"
- **Width**: 180 pixels
- **Position**: Column 3 (after Facility and Unit Names)
- **Editable**: Yes (double-click to edit)
- **Data Type**: String (text input)

## Usage
1. **Load an Excel file** with the Units table
2. **Navigate to the Units tab**
3. The "Filter for POD Rooms (Optional)" column will be visible
4. **Double-click** any cell in the column to edit
5. **Enter values** like:
   - `POD 1`
   - `POD A, POD B`
   - `Room 101, Room 102`
6. **Save** the file to preserve changes

## Data Binding
The column is bound to the `podRoomFilter` field in `ExcelParserV5.UnitRow`:
```java
@FXML private TableColumn<ExcelParserV5.UnitRow, String> unitPodRoomFilterCol;

// In initializeUnitColumns():
setupEditable(unitPodRoomFilterCol, 
    r -> r.podRoomFilter,           // Getter
    (r, v) -> r.podRoomFilter = v); // Setter
```

## Backward Compatibility
- If the Excel file does NOT have a "Filter for POD Rooms (Optional)" column, the GUI column will still appear but will be empty
- Users can manually enter values in the GUI even if the Excel file doesn't have the column
- The column is optional - leaving it blank is perfectly fine

## Testing
The following tests verify the functionality:

### Unit Tests
- `PodRoomFilterColumnTest.podRoomFilterFieldExists()` - Verifies the field exists and is accessible
- `PodRoomFilterColumnTest.podRoomFilterCanBeSetOnConstruction()` - Tests field initialization
- `PodRoomFilterColumnTest.podRoomFilterPositionedCorrectly()` - Documents correct position

### Integration Tests
- `PodRoomFilterIntegrationTest.podRoomFilterRoundTripTest()` - Tests Excel → Parser → GUI editing
- `PodRoomFilterIntegrationTest.podRoomFilterWithoutColumnDoesNotBreak()` - Tests backward compatibility

All tests pass ✓

## Files Modified
1. **App.fxml** - Added `<TableColumn fx:id="unitPodRoomFilterCol" .../>`
2. **AppController.java** - Added field declaration and initialization
3. **PodRoomFilterColumnTest.java** - New unit tests
4. **PodRoomFilterIntegrationTest.java** - New integration tests

## Impact on JSON Output
When values are entered in this column, they affect the generated JSON:
- **NurseCalls flows**: Add a POD room filter condition
- **Clinicals flows**: Add a POD room filter condition
- **Orders flows**: Not affected

This functionality was already implemented in the backend (ExcelParserV5). This change simply makes it **visible and editable in the GUI**.
