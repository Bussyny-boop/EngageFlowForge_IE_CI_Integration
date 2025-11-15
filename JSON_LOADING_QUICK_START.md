# Quick Start Guide: Bidirectional JSON Loading

## Overview

The Engage Rules Generator now supports **bidirectional JSON loading**, allowing you to:
- Export JSON files from your Excel/XML data
- Load those JSON files back into the app
- Edit and re-export as needed

## Visual Workflow

```
┌─────────────────────────────────────────────────────────────────┐
│                    BIDIRECTIONAL WORKFLOW                        │
└─────────────────────────────────────────────────────────────────┘

Step 1: LOAD EXCEL/XML           Step 2: EXPORT JSON
┌──────────────────┐              ┌──────────────────┐
│  📂 Load File    │ ──────────→  │ 💾 Export        │
│                  │              │   Nursecall      │
│  Excel or XML    │              │   Clinical       │
│  file with data  │              │   Orders         │
└──────────────────┘              └──────────────────┘
        │                                  │
        ↓                                  ↓
┌──────────────────┐              ┌──────────────────┐
│  GUI Tables      │              │  JSON File       │
│  populated with  │              │  Saved to disk   │
│  all data        │              │                  │
└──────────────────┘              └──────────────────┘

                    ╔═══════════════════════╗
                    ║   BIDIRECTIONAL       ║
                    ║   ↓                   ║
                    ╚═══════════════════════╝

Step 3: LOAD JSON                Step 4: EDIT & RE-EXPORT
┌──────────────────┐              ┌──────────────────┐
│  📥 Load JSON    │ ──────────→  │  ✏️ Edit Data    │
│                  │              │                  │
│  Previously      │              │  Make changes    │
│  exported file   │              │  in GUI tables   │
└──────────────────┘              └──────────────────┘
        │                                  │
        ↓                                  ↓
┌──────────────────┐              ┌──────────────────┐
│  GUI Tables      │              │ 💾 Re-export     │
│  populated from  │              │  Updated JSON    │
│  JSON data       │              │                  │
└──────────────────┘              └──────────────────┘
```

## UI Location

The **📥 Load JSON** button is located in the left sidebar, right after the **📂 Load File** button:

```
┌─────────────────────────┐
│                         │
│  📂 Load File           │  ← Load Excel/XML
│                         │
│  📥 Load JSON           │  ← NEW! Load JSON
│                         │
│  🗑️ Clear All           │
│                         │
│  ─────────────────      │
│                         │
│  📊 Units               │
│  🔔 Nurse Calls         │
│  🏥 Clinicals           │
│  💊 Orders              │
│                         │
│  ─────────────────      │
│                         │
│  📋 Preview JSON        │
│                         │
│  ─────────────────      │
│                         │
│  💾 Export Nursecall    │
│  💾 Export Clinical     │
│  💾 Export Orders       │
│                         │
└─────────────────────────┘
```

## Step-by-Step Instructions

### 1️⃣ Export JSON

1. Load your Excel or XML file using **📂 Load File**
2. Wait for data to populate in the tables
3. Click one of the export buttons:
   - **💾 Export Nursecall** → saves NurseCalls.json
   - **💾 Export Clinical** → saves Clinicals.json
   - **💾 Export Orders** → saves Orders.json
4. Choose a location and save the file

### 2️⃣ Load JSON

1. Click **📥 Load JSON** in the sidebar
2. Select the JSON file you want to load
3. Click "Open"
4. The app will:
   - Parse the JSON file
   - Populate the GUI tables
   - Show a summary of what was loaded

### 3️⃣ View Loaded Data

After loading, you'll see:

**Load Summary in Preview Panel**:
```
✅ JSON loaded successfully

Loaded:
  • 25 Nurse Call flows
  • 18 Clinical flows
  • 12 Orders flows

⚠️ Note: Units data may be incomplete when loading from JSON.
Some fields may not be fully populated.
Consider loading the original Excel file for complete data.
```

**Tables Populated**:
- Navigate to **📊 Units**, **🔔 Nurse Calls**, **🏥 Clinicals**, or **💊 Orders**
- View and edit the loaded data
- All editable fields can be modified

### 4️⃣ Edit and Re-export (Optional)

1. Navigate to any tab (Units, Nurse Calls, etc.)
2. Click on any cell to edit
3. Make your changes
4. Click **💾 Export** again to save updated JSON

## Supported JSON Formats

### Format 1: Combined JSON
Exports all flow types in one file:

```json
{
  "nurseCalls": {
    "version": "1.1.0",
    "alarmAlertDefinitions": [...],
    "deliveryFlows": [...]
  },
  "clinicals": {
    "version": "1.1.0",
    "alarmAlertDefinitions": [...],
    "deliveryFlows": [...]
  },
  "orders": {
    "version": "1.1.0",
    "alarmAlertDefinitions": [...],
    "deliveryFlows": [...]
  }
}
```

### Format 2: Individual Flow Type
Exports a single flow type:

```json
{
  "version": "1.1.0",
  "alarmAlertDefinitions": [...],
  "deliveryFlows": [...]
}
```

**Both formats are supported** when loading!

## What Gets Loaded

✅ **Fully Supported**:
- Alarm names
- Configuration groups
- Priorities
- Device interfaces (Device A)
- Parameter attributes:
  - Break Through DND
  - Enunciate
  - Ringtone/Alert Sound
  - Response Options
  - TTL values

⚠️ **Partially Supported**:
- Recipients (primary recipient only)
- Units data (basic facility/group mapping)

❌ **Not Supported**:
- Complete unit breakdown details
- Room filters from units
- Complex escalation chains
- Custom tab source information

## Tips & Best Practices

### ✅ DO:
- Keep your original Excel/XML files for complete data
- Use JSON loading for quick reviews and minor edits
- Test round-trip with your specific data before relying on it
- Check the load summary for any warnings

### ⚠️ DON'T:
- Rely on JSON as your only data source
- Expect 100% field reconstruction from JSON
- Use JSON loading for complex unit configuration edits

## Troubleshooting

### "Failed to load JSON file"
- **Check**: Is the file valid JSON?
- **Solution**: Open the file in a JSON validator or text editor
- **Tip**: Ensure it was exported from this application

### "Some fields are empty after loading"
- **Expected**: JSON doesn't contain all Excel data
- **Solution**: Load the original Excel file instead
- **Alternative**: Manually fill in missing fields in the GUI

### "No data appears in tables"
- **Check**: Did you load a combined format with the correct keys?
- **Solution**: Verify the JSON has `nurseCalls`, `clinicals`, or `orders` keys
- **Alternative**: Try exporting a fresh JSON to compare format

## Example Use Cases

### Use Case 1: Quick Review
```
1. Export JSON from production Excel file
2. Load JSON to quickly review configurations
3. No editing needed
```

### Use Case 2: Minor Adjustments
```
1. Export JSON
2. Load JSON
3. Edit a few alarm names or priorities
4. Re-export updated JSON
```

### Use Case 3: Data Sharing
```
1. Export JSON
2. Share JSON with team member
3. They load JSON to review
4. Provide feedback
```

### Use Case 4: Round-Trip Testing
```
1. Export JSON from Excel
2. Load JSON back
3. Verify data matches expectations
4. Export again and compare
```

## Keyboard Shortcuts

While no specific shortcuts exist for JSON loading, you can:
- Use **Tab** to navigate between fields
- Use **Enter** to confirm edits
- Use **Esc** to cancel edits

## Related Features

This feature works well with:
- **📋 Preview JSON**: Generate preview before exporting
- **🗑️ Clear All**: Clear data before loading new JSON
- **💾 Export buttons**: Export individual or combined flow types

## Support

For issues or questions:
1. Check the console output for error messages
2. Verify your JSON format matches the expected structure
3. Refer to BIDIRECTIONAL_JSON_LOADING.md for technical details
4. Run tests: `mvn test -Dtest=JsonLoadingTest`

---

**Feature Status**: ✅ Production Ready  
**Version**: 2.5.0  
**Last Updated**: November 15, 2025
