# POD Room Filter - Visual Example

## Excel Configuration

### Unit Breakdown Sheet

| Facility | Common Unit Name | **Filter for POD Rooms (Optional)** | Nurse Call Configuration Group |
|----------|------------------|-------------------------------------|-------------------------------|
| TestFacility | ICU | **POD 1** | ICU-Group |

### Nurse Call Sheet

| Configuration Group | Common Alert or Alarm Name | Priority | Time to 1st Recipient | 1st Recipient |
|--------------------|---------------------------|----------|---------------------|---------------|
| ICU-Group | Call Button | Normal | 0 | Nurse |

---

## Generated JSON Output

### Without POD Filter (Previous Behavior)

When the "Filter for POD Rooms (Optional)" column is empty or missing:

```json
{
  "conditions": [
    {
      "filters": [
        {
          "attributePath": "bed",
          "operator": "not_null"
        },
        {
          "attributePath": "to.type",
          "operator": "not_equal",
          "value": "TargetGroups"
        }
      ],
      "name": "NurseCallsCondition"
    }
  ]
}
```

### With POD Filter (New Feature)

When "POD 1" is entered in the "Filter for POD Rooms (Optional)" column:

```json
{
  "conditions": [
    {
      "filters": [
        {
          "attributePath": "bed",
          "operator": "not_null"
        },
        {
          "attributePath": "to.type",
          "operator": "not_equal",
          "value": "TargetGroups"
        }
      ],
      "name": "NurseCallsCondition"
    },
    {
      "filters": [
        {
          "attributePath": "bed.room.room_number",
          "operator": "in",
          "value": "POD 1"
        }
      ],
      "name": "POD rooms filter"
    }
  ]
}
```

---

## Key Features Demonstrated

✅ **New Condition Added**: The "POD rooms filter" condition is appended to the conditions array

✅ **Correct Operator**: Uses "in" operator (not "equal")

✅ **Correct Attribute Path**: Uses "bed.room.room_number"

✅ **Proper Name**: Condition is named "POD rooms filter"

✅ **Value Preserved**: The value "POD 1" from Excel is preserved in the JSON

✅ **Backward Compatible**: When column is empty, only NurseCallsCondition is present (default behavior)

---

## Multiple Room Example

### Excel Configuration

| Facility | Common Unit Name | **Filter for POD Rooms (Optional)** | Patient Monitoring Configuration Group |
|----------|------------------|-------------------------------------|---------------------------------------|
| TestFacility | Cardiac Care | **POD A, POD B, POD C** | Cardiac-Group |

### Generated JSON Output

```json
{
  "conditions": [
    {
      "filters": [
        {
          "attributePath": "bed.room.room_number",
          "operator": "in",
          "value": "POD A, POD B, POD C"
        }
      ],
      "name": "POD rooms filter"
    }
  ]
}
```

**Note**: Clinicals flows don't have a default condition, so the POD filter is the only condition present.

---

## Special Character Handling

### Input with Special Characters

| Filter for POD Rooms (Optional) |
|--------------------------------|
| POD #1, POD @2 |

### Generated JSON

```json
{
  "filters": [
    {
      "attributePath": "bed.room.room_number",
      "operator": "in",
      "value": "POD 1, POD 2"
    }
  ],
  "name": "POD rooms filter"
}
```

**Result**: Special characters `#` and `@` are stripped, leaving only alphanumeric characters, spaces, and commas.
