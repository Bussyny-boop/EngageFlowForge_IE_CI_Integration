# POD Room Filter Column - Screenshot Simulation

## GUI Table View - Units Tab

This simulates what the Units table looks like in the JavaFX GUI after the changes.

### Before the Change
The "Filter for POD Rooms (Optional)" column existed in the data model but was **not visible** in the GUI:

```
╔════════════════════════════════════════════════════════════════════════════════════════════════╗
║                                  Engage FlowForge 2.0 - Units                                  ║
╠════════════════════════════════════════════════════════════════════════════════════════════════╣
║  Facility    │  Unit Names  │  Nurse Group    │  Clinical Group  │  Orders Group  │  Comments  ║
╠══════════════╪══════════════╪═════════════════╪══════════════════╪════════════════╪════════════╣
║  TestHosp    │  ICU         │  ICU-Nurse      │  ICU-Clinical    │                │            ║
║  TestHosp    │  CCU         │  CCU-Nurse      │  CCU-Clinical    │                │            ║
║  TestHosp    │  ER          │  ER-Nurse       │  ER-Clinical     │                │            ║
╚══════════════╧══════════════╧═════════════════╧══════════════════╧════════════════╧════════════╝
```

### After the Change
The "Filter for POD Rooms (Optional)" column is now **visible and editable**:

```
╔══════════════════════════════════════════════════════════════════════════════════════════════════════════════════════╗
║                                       Engage FlowForge 2.0 - Units                                                   ║
╠══════════════════════════════════════════════════════════════════════════════════════════════════════════════════════╣
║  Facility    │  Unit Names  │  Filter for POD Rooms (Opt.)  │  Nurse Group    │  Clinical Group  │  Orders Group   ║
╠══════════════╪══════════════╪═══════════════════════════════╪═════════════════╪══════════════════╪═════════════════╣
║  TestHosp    │  ICU         │  POD 1                        │  ICU-Nurse      │  ICU-Clinical    │                 ║
║  TestHosp    │  CCU         │  POD A, POD B                 │  CCU-Nurse      │  CCU-Clinical    │                 ║
║  TestHosp    │  ER          │                               │  ER-Nurse       │  ER-Clinical     │                 ║
╚══════════════╧══════════════╧═══════════════════════════════╧═════════════════╧══════════════════╧═════════════════╝
                                              ↑
                                         NEW COLUMN
                               (Positioned after "Unit Names")
```

## Column Details

### Visual Properties
- **Column Header**: "Filter for POD Rooms (Optional)"
- **Width**: 180 pixels (wider than other columns due to longer header text)
- **Position**: 3rd column (immediately after "Unit Names")
- **Background**: White (editable field)
- **Text Alignment**: Left-aligned
- **Font**: Same as other table columns (system default)

### Interaction
1. **Single-click**: Selects the cell
2. **Double-click**: Enters edit mode
3. **Type text**: Enter POD room values (e.g., "POD 1" or "POD A, POD B")
4. **Tab/Enter**: Commits the change and moves to the next cell
5. **Escape**: Cancels the edit

### Example Values in Column
- `POD 1` - Single POD filter
- `POD A, POD B` - Multiple PODs (comma-separated)
- `Room 101, Room 102` - Room-based filter
- *(empty)* - No filter applied (optional field)

## Data Flow

### When Excel is Loaded
```
Excel File                      Parser                      GUI Table
┌────────────────┐             ┌────────────┐              ┌──────────────────┐
│ Unit Breakdown │  ─parse─>   │ UnitRow    │  ─display─>  │ Table Cell       │
│ Sheet:         │             │ .podRoom   │              │ "POD 1"          │
│ "POD 1"        │             │ Filter     │              │ (editable)       │
└────────────────┘             └────────────┘              └──────────────────┘
```

### When User Edits in GUI
```
GUI Edit                       Data Model                   Save to Excel
┌──────────────────┐          ┌────────────┐               ┌────────────────┐
│ User types:      │  ─set─>  │ UnitRow    │  ─write─>     │ Excel Cell:    │
│ "POD A, POD B"   │          │ .podRoom   │               │ "POD A, POD B" │
│                  │          │ Filter     │               │                │
└──────────────────┘          └────────────┘               └────────────────┘
```

## Impact on Workflow

### Before
❌ Users could not see POD Room Filter values from Excel
❌ Users could not edit POD Room Filter values in the GUI
❌ Had to edit Excel file directly to change filters

### After
✅ Users can see POD Room Filter values loaded from Excel
✅ Users can edit POD Room Filter values directly in the GUI
✅ Changes are saved back to Excel when using "Save As" feature
✅ Column is optional - can be left empty if not needed

## Compatibility

### With Existing Excel Files
- **Files WITH "Filter for POD Rooms (Optional)" column**: Values are loaded and displayed ✓
- **Files WITHOUT the column**: Column appears empty, can be manually filled ✓

### With JSON Generation
- **When column has values**: POD room filter conditions are added to flows ✓
- **When column is empty**: No filter conditions added (default behavior) ✓

## Table Scrolling
The table is horizontally scrollable, so users can:
1. See the new column along with other columns
2. Scroll left/right to view all columns
3. Resize column widths by dragging column borders

## Note
This is a **textual simulation** of the GUI. The actual JavaFX application will have:
- Standard JavaFX table styling
- Mouse hover effects
- Selection highlighting
- Column resize cursors
- Proper font rendering
- Theme-based colors (light/dark mode support)
