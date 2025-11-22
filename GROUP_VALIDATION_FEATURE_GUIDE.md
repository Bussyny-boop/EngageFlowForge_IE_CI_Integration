# Voice Group Validation - Feature Guide

## Overview

The Voice Group Validation feature provides real-time validation and autocomplete for recipient fields containing voice group references. This ensures that all referenced groups exist in your loaded voice groups file.

## Key Features

### 1. Per-Group Name Validation

When you load a voice groups file and reference groups in recipient fields, each group name is validated individually:

- ✅ **Valid groups** remain in default color (black or white depending on theme)
- ❌ **Invalid groups** turn RED
- ✅ **Prefixes** ("VGroup: " or "Group: ") always remain in default color

#### Example Scenario

**Loaded Groups File (Column A):**
```
Code Blue
Acute Care
OB Nurse
```

**Recipient Field Input:**
```
VGroup: Code Blue
VGroup: Acute Care
VGroup: OB
```

**Visual Result:**
- "VGroup: " → Default color (black/white)
- "Code Blue" → Default color (valid ✓)
- "VGroup: " → Default color (black/white)
- "Acute Care" → Default color (valid ✓)
- "VGroup: " → Default color (black/white)
- "OB" → **RED** (invalid ❌ - only "OB Nurse" exists)

### 2. Real-Time Correction

When you fix an invalid group name, the color updates immediately:

**Before:** `VGroup: OB` → "OB" appears in **RED**
**After:** `VGroup: OB Nurse` → "OB Nurse" appears in **default color**

### 3. Typo Detection

The validation is case-insensitive but exact match required:

**Loaded:** "Acute Care"
**Input:** "VGroup: Acutr Care" → "Acutr Care" appears in **RED** (typo detected)
**Input:** "VGroup: acute care" → "acute care" appears in **default color** (case-insensitive match)

### 4. Smart Autocomplete

As you type in recipient fields, autocomplete suggestions appear:

- **Trigger**: Type at least 2 characters
- **Display**: Shows TOP 5 matching groups
- **Sorting**: Prioritizes matches that start with your text
  - "OB" → Shows "OB Nurse" first, then other groups containing "OB"
- **Activation**: Appears below the input field as you type
- **Selection**: Click a suggestion to insert it

#### Autocomplete Examples

**Typing "OB ":**
```
Suggestions (TOP 5):
1. OB Nurse          ← Starts with "OB" (priority)
2. OB Tech
3. OB Anesthesia
4. Lobby             ← Contains "OB" (lower priority)
5. Mobile Team
```

**Typing "Code":**
```
Suggestions (TOP 5):
1. Code Blue         ← Starts with "Code"
2. Code Gray
3. Code Red
4. Postal Code Team  ← Contains "Code"
```

## How to Use

### Step 1: Load Voice Groups File

1. Open **Settings** (gear icon in top bar)
2. Scroll to **Voice Group Validation** section
3. Click **Load Voice Group** button
4. Select your Excel (.xlsx, .xls) or CSV (.csv) file
   - The file should have group names in **Column A** of the first sheet
5. Status shows number of groups loaded (e.g., "15 groups loaded")

### Step 2: Edit Recipient Fields

1. Navigate to **Nurse Calls**, **Clinicals**, or **Orders** tab
2. Find recipient columns (R1, R2, R3, R4, R5, Device A, Device B)
3. Double-click a cell to edit
4. Type "VGroup: " followed by a group name
5. Use **Shift+Enter** for new lines, **Enter** to save

### Step 3: Use Autocomplete

1. Start typing after "VGroup: "
2. After 2+ characters, a dropdown appears with TOP 5 matches
3. Click a suggestion or continue typing
4. Press **Enter** to save, **Escape** to cancel

### Step 4: Verify Validation

1. Click outside the cell to exit edit mode
2. Invalid group names appear in **RED**
3. Valid group names appear in **default color**
4. Prefixes ("VGroup: ", "Group: ") always in **default color**

## Supported Formats

### Input Format

Both formats are supported (case-insensitive):

- `VGroup: <group name>`
- `Group: <group name>`

### Multi-Line Support

You can have multiple groups in one cell (Shift+Enter for new lines):

```
VGroup: Code Blue
VGroup: Rapid Response
VGroup: Stroke Team
```

### Single Line with Multiple Groups

You can also use commas or semicolons:

```
VGroup: Team A, VGroup: Team B; VGroup: Team C
```

Each group is validated individually.

## Validation Rules

### Case Sensitivity
- **Loaded:** "Code Blue"
- **Valid inputs:** "Code Blue", "code blue", "CODE BLUE", "CoDe BlUe"
- **Invalid input:** "Code Blue Team" (different name)

### Exact Match Required
- **Loaded:** "OB Nurse"
- **Valid input:** "VGroup: OB Nurse"
- **Invalid input:** "VGroup: OB" (partial match not accepted)

### Prefix Ignored in Validation
- Only the group name after "VGroup: " or "Group: " is validated
- The prefix itself is never marked as invalid

## Technical Details

### Code Components

- **VoiceGroupValidator.java**: Core validation logic
  - `parseAndValidate()`: Validates single line
  - `parseAndValidateMultiLine()`: Validates multi-line text
  - Returns segments with validation status (VALID, INVALID, PLAIN)

- **AppController.java**: UI integration
  - `setupAutoComplete()`: Autocomplete implementation
  - `createValidatedCellGraphic()`: Visual rendering with colors
  - `setupValidatedColumn()`: Applies to table columns

### Configuration

- **Autocomplete limit**: TOP 5 matches
- **Minimum characters**: 2 (to support short names like "OB")
- **Matching algorithm**: Contains match, prioritizing prefix matches
- **Sorting**: Alphabetical within priority groups

## Troubleshooting

### Autocomplete Not Showing

**Possible causes:**
1. No voice groups loaded → Load voice groups file first
2. Typed less than 2 characters → Type at least 2 characters
3. No matches found → Check your search text

### Validation Not Working

**Possible causes:**
1. No voice groups loaded → Load voice groups file first
2. Cell doesn't contain "VGroup: " or "Group: " → Add the prefix
3. Voice groups cleared → Re-load the voice groups file

### All Groups Showing as Invalid

**Possible causes:**
1. Wrong file loaded → Verify you loaded the correct file
2. Groups file has wrong format → Ensure groups are in Column A
3. Voice groups cleared → Re-load the voice groups file

## Best Practices

1. **Load groups before editing** - Load your voice groups file before starting to edit recipient fields
2. **Use autocomplete** - Start typing and select from suggestions to avoid typos
3. **Verify visually** - Red text = invalid, default color = valid
4. **Fix immediately** - Correct red (invalid) groups before saving your work
5. **Case doesn't matter** - Don't worry about capitalization, it's case-insensitive

## Example Workflow

1. Load voice groups file: "hospital_groups.xlsx"
   - Contains: Code Blue, Rapid Response, OB Nurse, ICU Team
2. Edit Nurse Call recipient field
3. Type: "VGroup: OB"
   - Autocomplete shows: "OB Nurse"
   - Click suggestion → "VGroup: OB Nurse"
4. Add another line (Shift+Enter)
5. Type: "VGroup: Code"
   - Autocomplete shows: "Code Blue"
   - Click suggestion → "VGroup: Code Blue"
6. Press Enter to save
7. Both groups appear in default color (valid ✓)

## Summary

The Voice Group Validation feature ensures data quality by:
- ✅ Validating each group name against your loaded groups
- ✅ Highlighting invalid groups in RED
- ✅ Providing TOP 5 autocomplete suggestions
- ✅ Supporting case-insensitive matching
- ✅ Working in real-time as you type and edit

This helps prevent errors and ensures all referenced groups actually exist in your system.
