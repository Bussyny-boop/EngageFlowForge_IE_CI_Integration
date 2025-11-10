# Merge Modes - JSON Output Examples

This document shows actual JSON output examples for all three merge modes using the scenario from the problem statement.

## Test Scenario

**Facility:** BCH  
**Units:**
- MedSurg, ICU → Acute Care NC
- LDR → OB NC

**Alarms (all with identical delivery parameters):**
1. Normal Call (Acute Care NC)
2. Pain Meds (Acute Care NC)
3. Shower Call (OB NC)

**Identical Delivery Parameters:**
- Priority: High
- Device-A: Badge
- Ringtone: Tone1
- Response Options: Accept
- Time to 1st Recipient: 0
- 1st Recipient: CNA
- Time to 2nd Recipient: 60
- 2nd Recipient: Nurse

---

## Mode 1: Standard (No Merge)

**Button:** "Standard (No Merge)"  
**Result:** 3 separate flows (one per alarm)

```json
{
  "deliveryFlows": [
    {
      "alarmsAlerts": ["Normal Call"],
      "name": "SEND NURSECALL | URGENT | Normal Call | Acute Care NC | MedSurg / ICU",
      "units": [
        {"facilityName": "BCH", "name": "MedSurg"},
        {"facilityName": "BCH", "name": "ICU"}
      ],
      "priority": "urgent",
      "destinations": [ /* ... */ ]
    },
    {
      "alarmsAlerts": ["Pain Meds"],
      "name": "SEND NURSECALL | URGENT | Pain Meds | Acute Care NC | MedSurg / ICU",
      "units": [
        {"facilityName": "BCH", "name": "MedSurg"},
        {"facilityName": "BCH", "name": "ICU"}
      ],
      "priority": "urgent",
      "destinations": [ /* ... */ ]
    },
    {
      "alarmsAlerts": ["Shower Call"],
      "name": "SEND NURSECALL | URGENT | Shower Call | OB NC | LDR",
      "units": [
        {"facilityName": "BCH", "name": "LDR"}
      ],
      "priority": "urgent",
      "destinations": [ /* ... */ ]
    }
  ]
}
```

---

## Mode 2: Merge Multiple Config Groups ✨ FIXED

**Button:** "Merge Multiple Config Groups"  
**Result:** 1 merged flow containing all alarms from both config groups

**Key Feature:** The flow name includes **ALL** config groups: `"Acute Care NC / OB NC"`

```json
{
  "deliveryFlows": [
    {
      "alarmsAlerts": [
        "Normal Call",
        "Pain Meds",
        "Shower Call"
      ],
      "name": "SEND NURSECALL | URGENT | Normal Call / Pain Meds / Shower Call | Acute Care NC / OB NC | MedSurg / ICU / LDR",
      "units": [
        {"facilityName": "BCH", "name": "MedSurg"},
        {"facilityName": "BCH", "name": "ICU"},
        {"facilityName": "BCH", "name": "LDR"}
      ],
      "priority": "urgent",
      "destinations": [
        {
          "order": 0,
          "delayTime": 0,
          "destinationType": "Normal",
          "groups": [
            {
              "facilityName": "BCH",
              "name": "CNA"
            }
          ],
          "presenceConfig": "user_and_device",
          "recipientType": "group"
        },
        {
          "order": 1,
          "delayTime": 60,
          "destinationType": "Normal",
          "groups": [
            {
              "facilityName": "BCH",
              "name": "Nurse"
            }
          ],
          "presenceConfig": "user_and_device",
          "recipientType": "group"
        }
      ],
      "conditions": [ /* ... */ ],
      "interfaces": [ /* ... */ ],
      "parameterAttributes": [ /* ... */ ],
      "status": "Active"
    }
  ]
}
```

**Note:** Before the fix, the name field would have been:  
❌ `"SEND NURSECALL | URGENT | Normal Call / Pain Meds / Shower Call | OB NC | MedSurg / ICU / LDR"`  
(missing "Acute Care NC")

---

## Mode 3: Merge by Single Config Group

**Button:** "Merge by Single Config Group"  
**Result:** 2 separate flows (one per config group)

```json
{
  "deliveryFlows": [
    {
      "alarmsAlerts": [
        "Normal Call",
        "Pain Meds"
      ],
      "name": "SEND NURSECALL | URGENT | Normal Call / Pain Meds | Acute Care NC | MedSurg / ICU",
      "units": [
        {"facilityName": "BCH", "name": "MedSurg"},
        {"facilityName": "BCH", "name": "ICU"}
      ],
      "priority": "urgent",
      "destinations": [
        {
          "order": 0,
          "delayTime": 0,
          "destinationType": "Normal",
          "groups": [
            {
              "facilityName": "BCH",
              "name": "CNA"
            }
          ],
          "presenceConfig": "user_and_device",
          "recipientType": "group"
        },
        {
          "order": 1,
          "delayTime": 60,
          "destinationType": "Normal",
          "groups": [
            {
              "facilityName": "BCH",
              "name": "Nurse"
            }
          ],
          "presenceConfig": "user_and_device",
          "recipientType": "group"
        }
      ],
      "conditions": [ /* ... */ ],
      "interfaces": [ /* ... */ ],
      "parameterAttributes": [ /* ... */ ],
      "status": "Active"
    },
    {
      "alarmsAlerts": [
        "Shower Call"
      ],
      "name": "SEND NURSECALL | URGENT | Shower Call | OB NC | LDR",
      "units": [
        {"facilityName": "BCH", "name": "LDR"}
      ],
      "priority": "urgent",
      "destinations": [
        {
          "order": 0,
          "delayTime": 0,
          "destinationType": "Normal",
          "groups": [
            {
              "facilityName": "BCH",
              "name": "CNA"
            }
          ],
          "presenceConfig": "user_and_device",
          "recipientType": "group"
        },
        {
          "order": 1,
          "delayTime": 60,
          "destinationType": "Normal",
          "groups": [
            {
              "facilityName": "BCH",
              "name": "Nurse"
            }
          ],
          "presenceConfig": "user_and_device",
          "recipientType": "group"
        }
      ],
      "conditions": [ /* ... */ ],
      "interfaces": [ /* ... */ ],
      "parameterAttributes": [ /* ... */ ],
      "status": "Active"
    }
  ]
}
```

---

## Quick Comparison

| Mode | Button Label | # of Flows | Config Groups in Name | Units |
|------|-------------|------------|----------------------|-------|
| **1** | Standard (No Merge) | 3 | Single (per flow) | Per config group |
| **2** | Merge Multiple Config Groups | 1 | **ALL** (Acute Care NC / OB NC) ✨ | Combined (MedSurg / ICU / LDR) |
| **3** | Merge by Single Config Group | 2 | Single (per flow) | Per config group |

---

## Name Field Format

All merged flow names follow this format:

```
SEND NURSECALL | [PRIORITY] | [Alarm1 / Alarm2 / ...] | [Config Group(s)] | [Unit1 / Unit2 / ...]
```

**Examples:**
- Mode 1: `SEND NURSECALL | URGENT | Normal Call | Acute Care NC | MedSurg / ICU`
- Mode 2: `SEND NURSECALL | URGENT | Normal Call / Pain Meds / Shower Call | Acute Care NC / OB NC | MedSurg / ICU / LDR`
- Mode 3: `SEND NURSECALL | URGENT | Normal Call / Pain Meds | Acute Care NC | MedSurg / ICU`

**Key Difference:**
- Mode 2 includes **multiple** config groups separated by " / " when merging across groups
- Modes 1 and 3 include only a **single** config group per flow

---

## Verification

These outputs match the expected format from the problem statement:

✅ Mode 2 (Merge Multiple Config Groups) now correctly shows:
```
"Acute Care NC / OB NC"
```
instead of just:
```
"OB NC"
```

✅ All flows have correct alarm lists, units, and delivery parameters.
