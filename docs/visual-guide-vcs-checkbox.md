# Visual Guide: VCS Checkbox Label Change

## Before the Fix

When users opened the application, they saw:

```
┌─────────────────────────────────────────────────┐
│  Optional Default Interface:                    │
│  ☐ Edge    ☐ VMP                               │
│    ↓          ↓                                 │
│  Tooltip:  Tooltip:                             │
│  "Use      "Use VMP interface                   │
│  OutgoingWCTP as default when                   │
│  interface  Device A/B                          │
│  as default are blank"                          │
│  when                                           │
│  Device A/B                                     │
│  are blank"                                     │
└─────────────────────────────────────────────────┘
```

**Problem**: Users called it the "VCS checkbox" because they work with "Vocera VCS" devices, but the UI showed "VMP".

## After the Fix

Now users see:

```
┌─────────────────────────────────────────────────┐
│  Optional Default Interface:                    │
│  ☐ Edge    ☐ VCS                               │
│    ↓          ↓                                 │
│  Tooltip:  Tooltip:                             │
│  "Use      "Use VMP interface                   │
│  OutgoingWCTP as default when                   │
│  interface  Device A/B                          │
│  as default are blank"                          │
│  when                                           │
│  Device A/B                                     │
│  are blank"                                     │
└─────────────────────────────────────────────────┘
```

**Result**: The checkbox label now says "VCS" which matches user terminology!

## What Happens When Checkboxes Are Selected

### Scenario 1: Only Edge Checkbox Selected
```
☑ Edge    ☐ VCS
```

For flows with blank Device A/B, generates:
```json
"interfaces": [
  {
    "componentName": "OutgoingWCTP",
    "referenceName": "OutgoingWCTP"
  }
]
```

### Scenario 2: Only VCS Checkbox Selected
```
☐ Edge    ☑ VCS
```

For flows with blank Device A/B, generates:
```json
"interfaces": [
  {
    "componentName": "VMP",
    "referenceName": "VMP"
  }
]
```

### Scenario 3: Both Checkboxes Selected
```
☑ Edge    ☑ VCS
```

For flows with blank Device A/B, generates:
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

### Scenario 4: No Checkboxes Selected
```
☐ Edge    ☐ VCS
```

For flows with blank Device A/B, generates:
```json
"interfaces": []
```

## Key Points

1. ✅ **Label Changed**: "VMP" → "VCS" (to match user terminology)
2. ✅ **Component Name Unchanged**: Still generates "VMP" component (correct)
3. ✅ **Functionality Unchanged**: The code works exactly the same
4. ✅ **User Experience Improved**: UI now matches what users expect

## Excel File Context

Users work with device names like:
- "iPhone-Edge" → Edge checkbox
- "Vocera VCS" → VCS checkbox (formerly labeled VMP)

The label change makes the connection between Excel data and UI controls clearer!
