# Device-B Feature - Example JSON Output

## Test Excel File

```
| Config Group | Alarm Name            | Priority | Device - A   | Device - B | Ringtone  |
|--------------|-----------------------|----------|--------------|------------|-----------|
| Nurse Group  | Edge Only Alarm       | Normal   | iPhone-Edge  |            | Ringtone1 |
| Nurse Group  | VCS Only Alarm        | Normal   |              | VCS        | Ringtone2 |
| Nurse Group  | Combined Edge and VCS | High     | iPhone-Edge  | VCS        | Ringtone3 |
```

## Generated JSON - Flow 1: Edge Only

```json
{
  "alarmsAlerts": ["Edge Only Alarm"],
  "interfaces": [
    {
      "componentName": "OutgoingWCTP",
      "referenceName": "OutgoingWCTP"
    }
  ],
  "priority": "normal"
}
```

**Result**: Single OutgoingWCTP interface ✅

## Generated JSON - Flow 2: VCS Only

```json
{
  "alarmsAlerts": ["VCS Only Alarm"],
  "interfaces": [
    {
      "componentName": "VMP",
      "referenceName": "VMP"
    }
  ],
  "priority": "normal"
}
```

**Result**: Single VMP interface ✅

## Generated JSON - Flow 3: Combined (Edge + VCS)

```json
{
  "alarmsAlerts": ["Combined Edge and VCS"],
  "interfaces": [
    {
      "componentName": "OutgoingWCTP",
      "referenceName": "OutgoingWCTP"
    },
    {
      "componentName": "VMP",
      "referenceName": "VMP"
    }
  ],
  "priority": "urgent"
}
```

**Result**: BOTH OutgoingWCTP AND VMP interfaces! ✅

## Key Observations

1. **Single Device Scenarios**: When only one device contains Edge or VCS, only one interface is generated
2. **Combined Scenario**: When Device-A has Edge and Device-B has VCS (or vice versa), BOTH interfaces are generated
3. **Order**: OutgoingWCTP always comes before VMP in the array
4. **Backward Compatibility**: If Device-B is blank or missing from Excel, behavior is identical to previous version

## Custom Reference Names

The reference names can be customized via the GUI:

```
Edge Reference Name: [CustomEdge]
VCS Reference Name:  [CustomVCS]
```

Results in:

```json
"interfaces": [
  {
    "componentName": "OutgoingWCTP",
    "referenceName": "CustomEdge"
  },
  {
    "componentName": "VMP",
    "referenceName": "CustomVCS"
  }
]
```

This allows flexibility in integration environments where different reference names are needed.
