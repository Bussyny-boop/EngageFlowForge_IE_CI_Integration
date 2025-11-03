# Alert Name Transformation Feature

## Overview

The alert name transformation feature allows you to configure how alert type names appear in the `"name"` attribute of the generated JSON flows. This is particularly useful when dealing with long alert type names that may exceed character limits.

## Key Features

- **Configurable Mappings**: Define custom transformations in `config.yml`
- **Targeted Transformation**: Only affects the `"name"` attribute in JSON output
- **Preserves Original Data**: Alert names in `alarmsAlerts` arrays remain unchanged
- **Case-Insensitive Matching**: Transformations work regardless of case

## How It Works

### Before (Old Behavior)
When using Advanced Merge Logic, the flow name would show:
```json
"name": "SEND CLINICAL | URGENT | (2 alarms) | General PM | MedSurg"
```

### After (New Behavior)
The flow name now shows the actual alert types with configured transformations:
```json
"name": "SEND CLINICAL | URGENT | Ext Tachy | SpO2 Desat | General PM | MedSurg"
```

## Configuration

### Location
Transformations are configured in: `src/main/resources/config.yml`

### Format
```yaml
alertNameTransformations:
  "Original Alert Name": "Shortened Name"
  "Another Alert Name": "Short Name"
```

### Default Transformations
The following transformations are pre-configured:

```yaml
alertNameTransformations:
  "Extreme Tachycardic": "Ext Tachy"
  "SpO2 Desaturation": "SpO2 Desat"
  "Severe Bradycardia": "Sev Brady"
  "Ventricular Tachycardia": "V-Tach"
  "Atrial Fibrillation": "A-Fib"
  "Respiratory Rate High": "RR High"
  "Respiratory Rate Low": "RR Low"
```

## Adding New Transformations

To add a new transformation:

1. Open `src/main/resources/config.yml`
2. Add a new line under `alertNameTransformations:`
3. Use the format: `"Full Alert Name": "Abbreviated Name"`
4. Save the file
5. Rebuild the application with `mvn clean package`

### Example
```yaml
alertNameTransformations:
  "Extreme Tachycardic": "Ext Tachy"
  "Your Custom Alert Name Here": "Custom Short"
```

## Important Notes

### What Gets Transformed
- ✅ The `"name"` attribute in `deliveryFlows` objects
- ✅ Both merged and non-merged flow names

### What Stays Original
- ❌ Alert names in `alarmsAlerts` arrays
- ❌ Alert names in `alarmAlertDefinitions`
- ❌ Excel data (transformations are applied only during JSON generation)

### Case Sensitivity
- Transformations are case-insensitive
- "Extreme Tachycardic", "extreme tachycardic", and "EXTREME TACHYCARDIC" will all be transformed to "Ext Tachy"

### No Transformation
- If an alert name doesn't have a configured transformation, it will appear as-is
- Example: "Regular Alert" → "Regular Alert" (unchanged)

## Examples

### Single Flow (No Merge)
**Excel Input:**
- Alert Name: "Extreme Tachycardic"
- Priority: Urgent
- Config Group: General PM

**JSON Output:**
```json
{
  "name": "SEND CLINICAL | URGENT | Ext Tachy | General PM | MedSurg",
  "alarmsAlerts": ["Extreme Tachycardic"]
}
```

### Merged Flow (Advanced Merge)
**Excel Input:**
- Alert 1: "Extreme Tachycardic"
- Alert 2: "SpO2 Desaturation"
- Both with same priority and delivery parameters

**JSON Output:**
```json
{
  "name": "SEND CLINICAL | URGENT | Ext Tachy | SpO2 Desat | General PM | MedSurg",
  "alarmsAlerts": ["Extreme Tachycardic", "SpO2 Desaturation"]
}
```

## Benefits

1. **Character Limit Compliance**: Shortened names help stay within character limits for the `name` attribute
2. **Better Readability**: Abbreviated names are easier to scan in flow lists
3. **Flexibility**: End-users can customize transformations without code changes
4. **Data Integrity**: Original alert names are preserved in the data arrays

## Troubleshooting

### Transformations Not Working
1. Verify the transformation is defined in `config.yml`
2. Check for typos in the alert name (case-insensitive but must match exactly)
3. Rebuild the application after config changes: `mvn clean package`
4. Check console output for "✅ Loaded X alert name transformations" message

### Console Output
When the application starts, you should see:
```
✅ Loaded 7 alert name transformations
```

If you see:
```
ℹ️ No alert name transformations defined in config.yml
```
Then the configuration section may be missing or incorrectly formatted.

## Technical Details

### Implementation
- **Class**: `AlertNameTransformer.java`
- **Config Loading**: Reads from `config.yml` at application startup
- **Application**: Applied in `buildFlowName()` and `buildFlowNameMerged()` methods

### Testing
- Test file: `AlertNameTransformationTest.java`
- Tests verify:
  - Configuration loading
  - Transformation application
  - Case-insensitive matching
  - Merged and single flow name generation
