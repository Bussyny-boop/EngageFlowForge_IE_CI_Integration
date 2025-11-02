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
- "Badge-Genie Enunciation"

### Multiple Column Support

The parser supports **multiple columns** containing "Genie Enunciation" in the same sheet. This is useful when different workbook templates use different column names or when migrating from one template to another.

**Behavior when multiple columns exist:**
- The parser finds all columns with "Genie Enunciation" in the header
- For each row, it checks all these columns in order (left to right)
- It uses the **first non-empty value** found
- If all columns are empty, it defaults to `true`

**Example:** If a workbook has both "Phone: Alert Display / Genie Enunciation (if badge) B" and "Badge-Genie Enunciation" columns:
- Row 1 has "Yes" in first column, empty in second → uses "Yes" (true)
- Row 2 has empty in first column, "No" in second → uses "No" (false)
- Row 3 has "Yes" in first column, "No" in second → uses "Yes" (true, first column takes priority)
- Row 4 has empty in both columns → defaults to true

### Column Position

The "Genie Enunciation" column is typically placed after "Engage/Edge Display Time (Time to Live)" and before the time/recipient columns. The exact column letter doesn't matter as the parser uses header name matching.

Example column order:
```
Configuration Group | Alarm Name | ... | TTL | Genie Enunciation | Time to 1st Recipient | 1st Recipient | ...
```

Or with multiple columns:
```
Configuration Group | ... | TTL | Phone: Alert Display / Genie Enunciation (if badge) B | Badge-Genie Enunciation | Time to 1st | ...
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

### Example 4: Multiple Columns with Fallback
```
Alarm Name       | Phone: Alert Display / Genie Enunciation | Badge-Genie Enunciation
Code Blue        | Yes                                       | 
Patient Request  |                                           | No
Fall Alert       | Yes                                       | No
Bed Exit         |                                           | 
```
**Results:**
- Code Blue: uses "Yes" from first column → `"enunciate": "true"`
- Patient Request: uses "No" from second column → `"enunciate": "false"`
- Fall Alert: uses "Yes" from first column (has priority) → `"enunciate": "true"`
- Bed Exit: both empty, defaults → `"enunciate": "true"`

## Implementation Details

1. The parser searches for **all** column headers containing "Genie Enunciation" (case-insensitive)
2. For each row, it checks all matching columns in order and uses the first non-empty value
3. Values are normalized to lowercase and matched against the list of accepted values
4. If no columns are present or all cells are blank, enunciation defaults to `true`
5. The boolean value is stored as a string in the JSON output to maintain consistency with the Engage API format

## Testing

The feature includes comprehensive test coverage:
- Parsing of various "Yes" values (Yes, Y, Enunciate, Enunciation, True)
- Parsing of "No" and "False" values
- Default behavior when blank
- Case-insensitive value matching
- Multiple column name variants
- Excel export/import round-trip preservation
- **Multi-column extraction with fallback logic:**
  - Value in first column only
  - Value in second column only
  - Values in both columns (first takes priority)
  - Empty values in all columns (defaults to true)

Run tests with:
```bash
mvn test -Dtest=EnunciationTest
```

## Migration Notes

Existing Excel files without a "Genie Enunciation" column will continue to work unchanged, with all alerts defaulting to `enunciate: true` (the previous hardcoded behavior). To change this behavior, simply add the column and specify the desired values.
