# Room Filter Feature - Visual Guide

## GUI Layout Changes

The Room Filter fields have been added to the top section of the application, just below the "Vocera Badges Alert Interface" controls:

```
┌─────────────────────────────────────────────────────────────────────┐
│ Vocera Badges Alert Interface:  [✓] Via Edge  [ ] Via VMP          │
│ [🔄 Reset Paths]                                                    │
│                                                                     │
│ Room Filter:  Nursecall: [Room number          ] ← NEW             │
│               Clinical:  [Room number          ] ← NEW             │
│               Orders:    [Room number          ] ← NEW             │
│                                                                     │
│ ──────────────────────────────────────────────────────────────      │
└─────────────────────────────────────────────────────────────────────┘
```

## How to Use

### Step 1: Enter Room Numbers
Type a room number (e.g., "305", "410", "500") into any of the three Room Filter fields:
- **Nursecall**: Filters nursecall flows
- **Clinical**: Filters clinical flows  
- **Orders**: Filters orders flows

### Step 2: Generate or Export JSON
Click one of the Preview or Export buttons:
- 🔧 Preview Nursecall
- 🏥 Preview Clinical
- 💊 Preview Orders
- 📤 Export Nursecall/Clinicals/Orders

### Step 3: View Results
The generated JSON will include room filter conditions based on which fields have values.

## Example Outputs

### Example 1: Nursecall with Room Filter "305"

**Input:**
- Room Filter Nursecall: `305`
- Room Filter Clinical: _(empty)_
- Room Filter Orders: _(empty)_

**Output JSON (conditions section):**
```json
"conditions": [
  {
    "filters": [
      {"attributePath": "bed", "operator": "not_null"},
      {"attributePath": "to.type", "operator": "not_equal", "value": "TargetGroups"}
    ],
    "name": "NurseCallsCondition"
  },
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
]
```

### Example 2: Clinical with Room Filter "410"

**Input:**
- Room Filter Nursecall: _(empty)_
- Room Filter Clinical: `410`
- Room Filter Orders: _(empty)_

**Output JSON (conditions section):**
```json
"conditions": [
  {
    "filters": [
      {
        "attributePath": "bed.room.room_number",
        "operator": "equal",
        "value": "410"
      }
    ],
    "name": "Room Filter For TT"
  }
]
```

### Example 3: Orders with Room Filter "500"

**Input:**
- Room Filter Nursecall: _(empty)_
- Room Filter Clinical: _(empty)_
- Room Filter Orders: `500`

**Output JSON (conditions section):**
```json
"conditions": [
  {
    "filters": [
      {
        "attributePath": "patient.current_place",
        "operator": "not_null"
      }
    ],
    "name": "Global Condition"
  },
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
]
```

### Example 4: No Room Filters

**Input:**
- Room Filter Nursecall: _(empty)_
- Room Filter Clinical: _(empty)_
- Room Filter Orders: _(empty)_

**Result:** No room filter conditions are added to any flows (original behavior preserved)

## Key Features

✅ **Optional Filtering** - Leave fields empty to skip filtering
✅ **Independent Filters** - Each flow type has its own room filter
✅ **Automatic Trimming** - Whitespace is automatically removed
✅ **Both Modes** - Works with normal and merged flow modes
✅ **Persistent During Session** - Values are maintained until changed
✅ **Clean JSON** - Empty filters are completely omitted from output

## Technical Details

### Condition Structures

The room filter uses different JSON structures depending on the flow type:

**Nursecall & Clinical:**
- Attribute Path: `bed.room.room_number`
- Operator: `equal`
- Name: "Room Filter For TT"

**Orders:**
- Attribute Path: `patient.current_place.locs.units.rooms.room_number`  
- Operator: `in`
- Name: "Room Filter for TT"

### Placement in Conditions Array

**Nursecall:** Room filter is added AFTER the default NurseCallsCondition
**Clinical:** Room filter may be the only condition (no default for clinicals)
**Orders:** Room filter is added AFTER the Global Condition

## Testing

All functionality has been thoroughly tested:
- ✅ 213 tests pass
- ✅ Room filter conditions are correctly added
- ✅ Empty filters are properly omitted
- ✅ Different condition structures for each flow type
- ✅ UI components load correctly
- ✅ No security vulnerabilities
