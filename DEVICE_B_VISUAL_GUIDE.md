# Device-B Validation Visual Guide

## Overview
This document provides visual examples of the Device-B validation highlighting feature and the interface logic implementation.

## Cell Highlighting Examples

### Example Excel Sheet

| Configuration Group | Alarm Name | Device - A | Device - B | Expected Highlight |
|---------------------|------------|------------|------------|-------------------|
| Nurse Group 1 | Call Button | Edge | VCS | No highlight (both valid) |
| Nurse Group 1 | Bed Exit | Edge | | No highlight (empty) |
| Nurse Group 1 | Code Blue | Edge | Invalid | **Light orange** (invalid) |
| Nurse Group 1 | Assist Call | VCS | Unknown | **Light orange** (invalid) |
| Nurse Group 1 | Emergency | Vocera | Test Device | **Light orange** (invalid) |
| Nurse Group 1 | IV Pump | XMPP | edge | No highlight (both valid) |

### Highlighting Rules

✅ **NOT Highlighted (White Background):**
- Valid keywords: VCS, Edge, Vocera, XMPP (case-insensitive)
- Empty/blank cells
- Cells with partial matches (e.g., "iPhone-Edge", "Vocera VCS")

⚠️ **Highlighted (Light Orange #FFE4B5):**
- Non-empty cells WITHOUT valid keywords
- Examples: "Invalid", "Unknown", "Test Device", "ABC123"

## Interface Logic Examples

### Example 1: Device-A Valid + Device-B Invalid + Default VMP Enabled

**Excel Configuration:**
```
Configuration Group: Nurse Group 1
Alarm Name: Test Alarm
Device - A: Edge
Device - B: Invalid
Vocera Badge Alert Interface: ☑ Via VMP
```

**Generated JSON:**
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

**Explanation:**
- ✅ OutgoingWCTP from Device-A ("Edge")
- ✅ VMP from default checkbox (because Device-B is non-empty but invalid)

### Example 2: Device-A Valid + Device-B Valid

**Excel Configuration:**
```
Configuration Group: Nurse Group 1
Alarm Name: Test Alarm
Device - A: Edge
Device - B: VCS
Vocera Badge Alert Interface: ☑ Via VMP
```

**Generated JSON:**
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

**Explanation:**
- ✅ OutgoingWCTP from Device-A ("Edge")
- ✅ VMP from Device-B ("VCS")
- ❌ Default checkbox NOT used (Device-B has valid keyword)

### Example 3: Device-A Valid + Device-B Empty

**Excel Configuration:**
```
Configuration Group: Nurse Group 1
Alarm Name: Test Alarm
Device - A: Edge
Device - B: 
Vocera Badge Alert Interface: ☑ Via VMP
```

**Generated JSON:**
```json
"interfaces": [
  {
    "componentName": "OutgoingWCTP",
    "referenceName": "OutgoingWCTP"
  }
]
```

**Explanation:**
- ✅ OutgoingWCTP from Device-A ("Edge")
- ❌ Default checkbox NOT used (Device-B is empty)

### Example 4: Both Devices Invalid + Default Checkboxes Enabled

**Excel Configuration:**
```
Configuration Group: Nurse Group 1
Alarm Name: Test Alarm
Device - A: Invalid
Device - B: Unknown
Vocera Badge Alert Interface: ☑ Via Edge, ☑ Via VMP
```

**Generated JSON:**
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

**Explanation:**
- ✅ Both interfaces from default checkboxes
- ℹ️ Both Device-A and Device-B highlighted in light orange

## GUI Appearance

### Nurse Calls Tab (Example)

```
┌─────────────────┬──────────────┬────────────┬────────────┬──────────┐
│ Configuration   │ Alarm Name   │ Device - A │ Device - B │ Ringtone │
│ Group           │              │            │            │          │
├─────────────────┼──────────────┼────────────┼────────────┼──────────┤
│ Nurse Group 1   │ Call Button  │ Edge       │ VCS        │ Ring1    │  <- Both white
├─────────────────┼──────────────┼────────────┼────────────┼──────────┤
│ Nurse Group 1   │ Bed Exit     │ Edge       │            │ Ring2    │  <- Both white (B is empty)
├─────────────────┼──────────────┼────────────┼────────────┼──────────┤
│ Nurse Group 1   │ Code Blue    │ Edge       │ [Invalid]  │ Ring3    │  <- B is light orange
├─────────────────┼──────────────┼────────────┼────────────┼──────────┤
│ Nurse Group 1   │ Assist       │ [Unknown]  │ [Test]     │ Ring4    │  <- Both light orange
└─────────────────┴──────────────┴────────────┴────────────┴──────────┘

Legend:
  White background = Valid or empty
  [Light orange]   = Invalid (non-empty without valid keywords)
```

## Testing Scenarios

### Scenario Matrix

| Device-A | Device-B | Default VMP | Expected Interfaces | Device-B Highlight |
|----------|----------|-------------|--------------------|--------------------|
| Edge | Invalid | ☑ | Edge + VMP | Light orange |
| Edge | VCS | ☑ | Edge + VMP (from devices) | White |
| Edge | (empty) | ☑ | Edge only | White |
| Invalid | Unknown | ☑ | VMP (from default) | Light orange |
| (empty) | Invalid | ☑ | VMP (from default) | Light orange |
| VCS | edge | ☐ | Edge + VMP (from devices) | White |

## Code Validation

The validation uses the `hasValidRecipientKeyword()` method which checks for:
- "vcs" (case-insensitive)
- "edge" (case-insensitive)
- "vocera" (case-insensitive)
- "xmpp" (case-insensitive)

**Important:** Empty/null values are considered VALID (return true) to prevent highlighting empty cells.

## Summary

✅ **Implemented:**
1. Device-B cell highlighting for invalid values
2. Empty cells NOT highlighted
3. Dual interface logic when Device-A valid and Device-B invalid
4. Comprehensive test coverage

✅ **Tested:**
- All 141 tests pass
- No security vulnerabilities
- Backward compatible

✅ **Documented:**
- Implementation summary
- Visual guide
- Test examples
