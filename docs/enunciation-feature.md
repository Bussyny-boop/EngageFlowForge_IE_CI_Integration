# Genie Enunciation Feature

The "Genie Enunciation" feature allows users to explicitly control the `enunciate` parameter in generated Engage JSON files via an Excel column. This controls whether alerts are spoken aloud (enunciated) on badge devices.

## Overview

Previously, the `enunciate` parameter was hardcoded to `true` for all alerts. Now users can control this on a per-alert basis through an Excel column.

## Excel Column

A column containing "Genie Enunciation" in its header name has been added support for both:
- **Nurse Call** sheet
- **Patient Monitoring** sheet

The column can have various names as long as it contains "Genie Enunciation" (case-insensitive). Common variants include:
- "Genie Enunciation"
- "Phone: Alert Display / Genie Enunciation (if badge) B"
- "Badge Genie Enunciation"

### Column Position

The "Genie Enunciation" column is typically placed after "Engage/Edge Display Time (Time to Live)" and before the time/recipient columns. The exact column letter doesn't matter as the parser uses header name matching.

Example column order:
```
Configuration Group | Alarm Name | ... | TTL | Genie Enunciation | Time to 1st Recipient | 1st Recipient | ...
```

### Accepted Values

The column accepts the following values (case-insensitive):

**Values that result in `enunciate: true`:**
- "Yes"
- "Y"
- "Enunciate"
- "Enunciation"
- "True"
- Empty/blank (defaults to true)

**Values that result in `enunciate: false`:**
- "No"
- "N"
- "False"
- Any other value

## Usage Instructions

1. Add a column header containing "Genie Enunciation" to your Nurse Call and/or Patient Monitoring sheets
2. For each alarm/alert row, enter one of the accepted values
3. Leave blank to use the default behavior (enunciation enabled)

## GUI Support

The "Genie Enunciation" field appears in both the Nurse Calls and Clinicals tabs and can be edited directly in the table view.

When saving Excel files from the GUI or re-exporting, the "Genie Enunciation" column is preserved with the user's configured values.

## Generated JSON

The Excel value is converted to a boolean and appears in the `parameterAttributes` section of each delivery flow:

```json
{
  "name": "enunciate",
  "value": "true"
}
```

or

```json
{
  "name": "enunciate",
  "value": "false"
}
```

Note: The value is a string representation of the boolean, consistent with other boolean parameters in the Engage API.

## Examples

### Example 1: Enable Enunciation
```
Alarm Name       | Priority | Genie Enunciation
Code Blue        | Urgent   | Yes
```
**Result:** `"enunciate": "true"`

### Example 2: Disable Enunciation
```
Alarm Name         | Priority | Genie Enunciation
Patient Request    | Normal   | No
```
**Result:** `"enunciate": "false"`

### Example 3: Alternative True Values
```
Alarm Name     | Priority | Genie Enunciation
Fall Alert     | High     | Enunciation
Bed Exit       | High     | (blank)
```
**Result:** Both produce `"enunciate": "true"`

## Implementation Details

1. The parser searches for any column header containing "Genie Enunciation" (case-insensitive)
2. Values are normalized to lowercase and matched against the list of accepted values
3. If the column is not present or the cell is blank, enunciation defaults to `true`
4. The boolean value is stored as a string in the JSON output to maintain consistency with the Engage API format

## Testing

The feature includes comprehensive test coverage:
- Parsing of various "Yes" values (Yes, Y, Enunciate, Enunciation, True)
- Parsing of "No" and "False" values
- Default behavior when blank
- Case-insensitive value matching
- Multiple column name variants
- Excel export/import round-trip preservation

Run tests with:
```bash
mvn test -Dtest=EnunciationTest
```

## Migration Notes

Existing Excel files without a "Genie Enunciation" column will continue to work unchanged, with all alerts defaulting to `enunciate: true` (the previous hardcoded behavior). To change this behavior, simply add the column and specify the desired values.
