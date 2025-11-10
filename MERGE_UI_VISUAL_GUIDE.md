# UI Changes for Merge by Config Group Feature

## Settings Panel - Merge Logic Section

```
┌─────────────────────────────────────────────────────┐
│                   ⚙️ Settings                        │
│                                                     │
│  Merge Logic                                        │
│  ┌─────────────────────────────────────────────┐   │
│  │ ☑️ Standard (No Merge)                      │   │
│  │                                             │   │
│  │   💡 Hover tooltip:                         │   │
│  │   "Standard Mode: Each alarm/alert gets     │   │
│  │   its own separate delivery flow. This is   │   │
│  │   the default behavior and creates the      │   │
│  │   most granular output."                    │   │
│  └─────────────────────────────────────────────┘   │
│                                                     │
│  ┌─────────────────────────────────────────────┐   │
│  │ ☐ Merge All (Ignore Config Group)          │   │
│  │                                             │   │
│  │   💡 Hover tooltip:                         │   │
│  │   "Merge All: Combines flows with identical │   │
│  │   Priority, Device, Ringtone, Recipients,   │   │
│  │   and Timing across ALL Config Groups..."   │   │
│  └─────────────────────────────────────────────┘   │
│                                                     │
│  ┌─────────────────────────────────────────────┐   │
│  │ ☐ Merge Within Config Group                │   │
│  │                                             │   │
│  │   💡 Hover tooltip:                         │   │
│  │   "Merge Within Config Group: Combines flows│   │
│  │   with identical Priority, Device, Ringtone,│   │
│  │   Recipients, and Timing ONLY within the    │   │
│  │   same Config Group..."                     │   │
│  └─────────────────────────────────────────────┘   │
│                                                     │
│  ─────────────────────────────────────────────     │
│                                                     │
│  Custom Tab Mappings                                │
│  ...                                                │
└─────────────────────────────────────────────────────┘
```

## Status Bar Display

When different merge modes are selected, the status bar updates:

### Standard (No Merge) Selected
```
┌─────────────────────────────────────────────────────┐
│ 📄 merge_test.xlsx    JSON: Standard              │
└─────────────────────────────────────────────────────┘
```

### Merge All Selected
```
┌─────────────────────────────────────────────────────┐
│ 📄 merge_test.xlsx    JSON: Merge All             │
└─────────────────────────────────────────────────────┘
```

### Merge Within Config Group Selected
```
┌─────────────────────────────────────────────────────┐
│ 📄 merge_test.xlsx    JSON: Merge Within Config Group│
└─────────────────────────────────────────────────────┘
```

## Behavior Demo

### Example Scenario
Excel file contains:
- ICU_Group: Code Blue, Rapid Response (identical delivery params)
- ER_Group: Trauma Alert, Stroke Alert (identical delivery params)

### Visual Results

#### Standard (No Merge) - 4 Flows
```
deliveryFlows: [
  { alarmsAlerts: ["Code Blue"] },
  { alarmsAlerts: ["Rapid Response"] },
  { alarmsAlerts: ["Trauma Alert"] },
  { alarmsAlerts: ["Stroke Alert"] }
]
```

#### Merge Within Config Group - 2 Flows
```
deliveryFlows: [
  { alarmsAlerts: ["Code Blue", "Rapid Response"] },    ← ICU_Group
  { alarmsAlerts: ["Trauma Alert", "Stroke Alert"] }    ← ER_Group
]
```

#### Merge All (Ignore Config Group) - 1 Flow
```
deliveryFlows: [
  { alarmsAlerts: ["Code Blue", "Rapid Response", 
                   "Trauma Alert", "Stroke Alert"] }
]
```

## Mutual Exclusion Behavior

When user clicks a checkbox:

**Before**: ☑️ Standard (No Merge)  
          ☐ Merge All  
          ☐ Merge Within Config Group

**Click "Merge All"**:  
          ☐ Standard (No Merge) ← Auto-deselected  
          ☑️ Merge All ← Now selected  
          ☐ Merge Within Config Group

**Click "Merge Within Config Group"**:  
          ☐ Standard (No Merge)  
          ☐ Merge All ← Auto-deselected  
          ☑️ Merge Within Config Group ← Now selected

**Click "Merge Within Config Group" again** (to deselect):  
          ☑️ Standard (No Merge) ← Auto-selected (default)  
          ☐ Merge All  
          ☐ Merge Within Config Group ← Deselected

## Integration Points

The merge mode is respected in:
1. ✅ **Generate JSON** button (Preview)
2. ✅ **Export NurseCalls JSON** button
3. ✅ **Export Clinicals JSON** button
4. ✅ **Export Orders JSON** button
5. ✅ Live JSON preview updates
6. ✅ Status bar display

## Visual Design Notes

- **Section Header**: "Merge Logic" in bold with tooltip
- **Checkboxes**: Standard vertical layout with clear spacing
- **Tooltips**: Detailed explanations that appear on hover
- **Default State**: "Standard (No Merge)" is selected by default
- **Visual Feedback**: Status bar updates immediately when selection changes
- **Consistent Naming**: All UI elements use the same terminology
