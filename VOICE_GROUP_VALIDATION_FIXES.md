# Voice Group Validation Fixes - November 22, 2025

## Issues Fixed

### 1. Clear All Button Not Resetting Voice Groups
**Problem:** The "Clear All" button was only clearing the loaded voice group file path indicator but not actually clearing the voice group data from memory. The original table data remained unchanged.

**Solution:** Updated the `clearAllData()` method in `AppController.java` to:
- Clear the `loadedVoiceGroups` set completely
- Reset all voice group-related button states (loaded indicators, tooltips)
- Update the voice group statistics label
- Ensure both Load and Clear Voice Group buttons are properly reset

**Code Changes:**
```java
// Clear voice groups data and button state
synchronized(loadedVoiceGroups) {
    loadedVoiceGroups.clear();
}
updateVoiceGroupStats();
setButtonLoaded(loadVoiceGroupButton, false);
setButtonLoading(loadVoiceGroupButton, false);
if (loadVoiceGroupButton != null) loadVoiceGroupButton.setTooltip(null);

// Also clear the Clear Voice Group button state
if (clearVoiceGroupButton != null) {
    setButtonLoaded(clearVoiceGroupButton, false);
    setButtonLoading(clearVoiceGroupButton, false);
    clearVoiceGroupButton.setTooltip(null);
}
```

### 2. Incorrect Validation Logic
**Problem:** The validation was checking if the keyword "VGroup" (case-insensitive) existed anywhere in the text and then turning ALL group names red, rather than validating only the specific group name that appears after the keyword.

**Root Cause:** The validation logic was not properly isolating the group name portion for validation. It was validating the entire matched text instead of just the group name after the keyword.

**Goal:** Only validate the group name portion (the text after "VGroup:" or "Group:") against the loaded voice groups file. The keyword itself should remain black, and only invalid group names should turn red.

**Solution:** Updated `VoiceGroupValidator.parseAndValidate()` method to:
- Extract the keyword prefix separately (e.g., "VGroup: " or "Group: ")
- Extract and trim the group name that follows the keyword
- Clean the group name for validation (remove trailing special characters)
- Validate ONLY the group name portion against loaded voice groups (case-insensitive)
- Mark only the group name as VALID (black) or INVALID (red)
- Keep the keyword prefix as PLAIN (black) always

**Code Changes in VoiceGroupValidator.java:**
```java
// The prefix (e.g. "VGroup: " or "Group: ")
String prefix = m.group(1);
segments.add(new Segment(prefix, ValidationStatus.PLAIN)); // Keyword stays black

// The group name - this is what we validate
String groupName = m.group(2).trim();

// Clean group name for validation (remove trailing special chars like #)
String nameToValidate = groupName.replaceAll("[^a-zA-Z0-9_\\-]+$", "").trim();

// Check if this specific group name exists in loaded groups (case-insensitive)
boolean isValid = false;
if (!nameToValidate.isEmpty()) {
    for (String validGroup : loadedVoiceGroups) {
        if (validGroup.equalsIgnoreCase(nameToValidate)) {
            isValid = true;
            break;
        }
    }
}

// Only the group name is colored red if invalid, not the entire text
segments.add(new Segment(groupName, isValid ? ValidationStatus.VALID : ValidationStatus.INVALID));
```

### 3. Cell Height Expansion on Validation
**Problem:** When voice groups were loaded, all rows containing cells with the "VGroup" keyword were expanding in height, making the table difficult to read and navigate.

**Root Cause:** The `TextFlow` component used for validation rendering was not properly constrained in height, causing cells to grow to accommodate the content.

**Solution:** Implemented multiple height constraint strategies:

1. **Updated `createValidatedCellGraphic()`:**
   - Changed from zero padding to minimal padding (2,5,2,5) for better readability
   - Removed `setMaxHeight(Control.USE_PREF_SIZE)` which was ineffective
   - Used proper Label wrapping for non-validated text

2. **Updated `setupValidatedColumn()`:**
   - Wrapped the validated graphic in a `StackPane` container
   - Set explicit height constraints on the container:
     - `setMaxHeight(24)` - Match default cell height
     - `setPrefHeight(24)` - Preferred height
   - This prevents the cell from expanding regardless of content

**Code Changes in AppController.java:**
```java
if (hasVoiceGroups && hasKeywords) {
    setText(null);
    Node graphic = createValidatedCellGraphic(item);
    // Wrap in a constrained container to prevent cell height expansion
    if (graphic != null) {
        StackPane container = new StackPane(graphic);
        container.setMaxHeight(24); // Match default cell height
        container.setPrefHeight(24);
        setGraphic(container);
    } else {
        setGraphic(graphic);
    }
    setStyle(""); 
}
```

## Testing Recommendations

### Test Case 1: Clear All Button
1. Load an Excel file with data
2. Load a voice groups file (e.g., CSV or XLSX with group names)
3. Verify voice group stats show "X groups loaded"
4. Click "Clear All" button
5. ✅ **Expected:** All data cleared, voice group stats show "No groups loaded", all tables empty

### Test Case 2: Validation Logic
1. Load a voice groups file containing: `TeamA`, `TeamB`, `TeamC`
2. In a recipient column, enter: `VGroup: TeamA, VGroup: TeamB, VGroup: InvalidGroup`
3. ✅ **Expected:** 
   - "VGroup: " keyword remains black
   - "TeamA" remains black (valid)
   - "TeamB" remains black (valid)  
   - "InvalidGroup" turns red (invalid)
4. Enter: `Group: teamc` (lowercase)
5. ✅ **Expected:** "teamc" remains black (case-insensitive match)

### Test Case 3: Cell Height Constraint
1. Load a voice groups file
2. In recipient columns, enter cells with:
   - Single line: `VGroup: TeamA`
   - Multiple groups: `VGroup: TeamA, VGroup: TeamB, VGroup: TeamC`
   - Multi-line text with groups
3. ✅ **Expected:** All cells maintain consistent height (24px), no expansion
4. Scroll through table
5. ✅ **Expected:** All rows have uniform height, easy to read and navigate

## Files Modified

1. **AppController.java**
   - `clearAllData()` - Added voice group clearing logic
   - `createValidatedCellGraphic()` - Improved padding and label handling
   - `setupValidatedColumn()` - Added StackPane container with height constraints

2. **VoiceGroupValidator.java**
   - `parseAndValidate()` - Fixed validation to only check group names, not entire text

## Key Improvements

✅ **Clear All now acts as a true reset button** - Clears all data including voice groups  
✅ **Validation is targeted and accurate** - Only group names are validated, keywords remain black  
✅ **UI remains stable** - No cell height expansion during validation  
✅ **Case-insensitive matching** - "TeamA", "teama", "TEAMA" all match correctly  
✅ **Better user experience** - Consistent table layout, clear visual feedback
