# GUI Changes - Device B Column

## Nurse Calls Tab - Column Layout

Before:
```
┌─────────────┬──────────┬────────┬──────────┬──────────┬───────────┐
│ Config Group│...       │Priority│Device A  │Ringtone  │Response..│
├─────────────┼──────────┼────────┼──────────┼──────────┼───────────┤
│ Group 1     │...       │ Normal │iPhone-Edg│Ringtone1 │None      │
└─────────────┴──────────┴────────┴──────────┴──────────┴───────────┘
```

After (with Device B):
```
┌─────────────┬──────────┬────────┬──────────┬──────────┬──────────┬───────────┐
│ Config Group│...       │Priority│Device A  │Device B  │Ringtone  │Response..│
├─────────────┼──────────┼────────┼──────────┼──────────┼──────────┼───────────┤
│ Group 1     │...       │ Normal │iPhone-Edg│          │Ringtone1 │None      │
│ Group 1     │...       │ Normal │          │VCS       │Ringtone2 │None      │
│ Group 1     │...       │ High   │iPhone-Edg│VCS       │Ringtone3 │None      │
└─────────────┴──────────┴────────┴──────────┴──────────┴──────────┴───────────┘
                                       ↑          ↑
                                    NEW COLUMN!
```

## Clinicals Tab - Column Layout

Before:
```
┌─────────────┬──────────┬────────┬──────────┬──────────┬───────────┐
│ Config Group│...       │Priority│Device A  │Ringtone  │Response..│
├─────────────┼──────────┼────────┼──────────┼──────────┼───────────┤
│ Group 1     │...       │ Urgent │Vocera VCS│Ringtone1 │Accept    │
└─────────────┴──────────┴────────┴──────────┴──────────┴───────────┘
```

After (with Device B):
```
┌─────────────┬──────────┬────────┬──────────┬──────────┬──────────┬───────────┐
│ Config Group│...       │Priority│Device A  │Device B  │Ringtone  │Response..│
├─────────────┼──────────┼────────┼──────────┼──────────┼──────────┼───────────┤
│ Group 1     │...       │ Urgent │Vocera VCS│          │Ringtone1 │Accept    │
│ Group 1     │...       │ Urgent │Edge      │VCS       │Ringtone2 │Accept    │
└─────────────┴──────────┴────────┴──────────┴──────────┴──────────┴───────────┘
                                       ↑          ↑
                                    NEW COLUMN!
```

## Combined Interface Example

When a row has values in both Device A and Device B containing "Edge" or "VCS":

Excel Row:
```
Device A: "iPhone-Edge"    Device B: "VCS"
```

Generated JSON interfaces:
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

## Column Properties

- **Column Name**: "Device B"
- **Position**: Immediately after "Device A" column
- **Width**: 140.0 pixels (same as Device A)
- **Editable**: Yes (users can edit directly in the GUI)
- **Required**: No (optional column)

## Key Features

✅ Fully editable in the GUI
✅ Persisted when saving Excel
✅ Automatically parsed from Excel files
✅ Triggers combined interface generation when appropriate
✅ Works with both Nurse Calls and Clinicals tabs
