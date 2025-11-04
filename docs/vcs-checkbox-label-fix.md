# VCS Checkbox Label Fix

## Problem
The checkbox for selecting the VMP (Vocera VCS) default interface was labeled "VMP" in the GUI, but users refer to it as the "VCS checkbox" because:
- Devices in Excel files are named "Vocera VCS"
- Users think in terms of VCS devices, not VMP components
- The problem statement explicitly mentioned "VCS checkbox"

This caused confusion when users tried to configure default interfaces for blank devices.

## Solution
Changed the checkbox label from "VMP" to "VCS" in the GUI to match user expectations.

### File Changed
- `src/main/resources/com/example/exceljson/App.fxml` (line 47)

### Before
```xml
<CheckBox fx:id="defaultVmpCheckbox" text="VMP">
```

### After
```xml
<CheckBox fx:id="defaultVmpCheckbox" text="VCS">
```

## Behavior
The checkbox label change does not affect functionality:

| Checkbox | Label (UI) | Component Generated | Reference Name |
|----------|-----------|---------------------|----------------|
| Edge     | "Edge"    | `OutgoingWCTP`      | `OutgoingWCTP` |
| VCS      | "VCS"     | `VMP`               | `VMP`          |

### Example JSON Output
When both checkboxes are selected for flows with blank Device A/B:

```json
{
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
}
```

## Testing
- All existing tests pass
- New test `VcsCheckboxLabelTest` validates the expected behavior
- Functionality remains unchanged - only the label is updated
- Package builds successfully

## User Impact
Users will now see a checkbox labeled "VCS" (instead of "VMP") which matches their mental model and terminology when working with Vocera VCS devices.
