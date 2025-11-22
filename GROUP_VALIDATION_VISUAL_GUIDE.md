# Voice Group Validation - Visual Guide

## Feature Overview

This document provides visual examples of how the enhanced Voice Group validation feature works.

---

## Example 1: Loading Voice Groups

### Step 1: Before Loading Groups

**Voice Groups File (groups.xlsx):**
```
┌─────────────────┐
│ Column A        │
├─────────────────┤
│ Code Blue       │
│ Acute Care      │
│ OB Nurse        │
│ ICU Team        │
│ Rapid Response  │
└─────────────────┘
```

**UI State:**
```
┌────────────────────────────────────┐
│ Settings > Voice Group Validation  │
├────────────────────────────────────┤
│ [Load Voice Group]                 │
│ Status: No groups loaded           │
└────────────────────────────────────┘
```

### Step 2: After Loading Groups

**UI State:**
```
┌────────────────────────────────────┐
│ Settings > Voice Group Validation  │
├────────────────────────────────────┤
│ [Load Voice Group]                 │
│ Status: 5 groups loaded ✓          │
└────────────────────────────────────┘
```

---

## Example 2: Per-Group Name Validation

### Scenario: User enters groups, one is invalid

**Input (Recipient Field):**
```
VGroup: Code Blue
VGroup: Acute Care
VGroup: OB
```

**Visual Result:**

```
┌──────────────────────────────────────────┐
│ 1st Recipient                            │
├──────────────────────────────────────────┤
│ VGroup: Code Blue                        │  ← All BLACK (valid)
│ VGroup: Acute Care                       │  ← All BLACK (valid)
│ VGroup: OB                               │  ← "OB" is RED (invalid)
└──────────────────────────────────────────┘
```

**Breakdown:**
```
Line 1:
"VGroup: " → BLACK (prefix, always plain)
"Code Blue" → BLACK (valid - exact match found)

Line 2:
"VGroup: " → BLACK (prefix, always plain)
"Acute Care" → BLACK (valid - exact match found)

Line 3:
"VGroup: " → BLACK (prefix, always plain)
"OB" → RED (invalid - no match, only "OB Nurse" exists)
```

---

## Example 3: Correcting Invalid Group

### Before Correction

**Input:**
```
VGroup: OB
```

**Display:**
```
┌──────────────────────────────────────────┐
│ VGroup: OB                               │
│         ^^                               │
│         RED (invalid)                    │
└──────────────────────────────────────────┘
```

### After Correction

**Input:**
```
VGroup: OB Nurse
```

**Display:**
```
┌──────────────────────────────────────────┐
│ VGroup: OB Nurse                         │
│         ^^^^^^^^                         │
│         BLACK (valid)                    │
└──────────────────────────────────────────┘
```

**The color changes immediately from RED to BLACK!**

---

## Example 4: Typo Detection

### Scenario: User has a typo in group name

**Loaded Groups:**
- Code Blue
- Acute Care

**Input (with typo):**
```
VGroup: Code Blue
VGroup: Acutr Care
```

**Visual Result:**
```
┌──────────────────────────────────────────┐
│ VGroup: Code Blue                        │  ← BLACK (valid)
│ VGroup: Acutr Care                       │  ← "Acutr Care" is RED (typo)
│         ^^^^^^^^^^                       │
└──────────────────────────────────────────┘
```

**After fixing typo:**
```
┌──────────────────────────────────────────┐
│ VGroup: Code Blue                        │  ← BLACK (valid)
│ VGroup: Acute Care                       │  ← BLACK (valid, typo fixed)
└──────────────────────────────────────────┘
```

---

## Example 5: Autocomplete - TOP 5 Suggestions

### Scenario: User types "OB "

**Loaded Groups:**
- OB Nurse
- OB Tech
- OB Anesthesia
- Code Blue
- Lobby
- Global Team
- Rapid Response

**User types in edit mode:**
```
┌──────────────────────────────────────────┐
│ 1st Recipient (Edit Mode)                │
├──────────────────────────────────────────┤
│ VGroup: OB█                              │  ← User typed "OB"
└──────────────────────────────────────────┘
       ↓
┌──────────────────────┐
│ Suggestions:         │
├──────────────────────┤
│ 1. OB Nurse         │  ← Starts with "OB" (priority)
│ 2. OB Tech          │  ← Starts with "OB"
│ 3. OB Anesthesia    │  ← Starts with "OB"
│ 4. Lobby            │  ← Contains "ob" (lower priority)
│ 5. Global Team      │  ← Contains "ob"
└──────────────────────┘
```

**TOP 5 only** (not showing "Code Blue" or "Rapid Response")

### Scenario: User types "Code"

**User types:**
```
VGroup: Code█
```

**Autocomplete shows:**
```
┌──────────────────────┐
│ Suggestions:         │
├──────────────────────┤
│ 1. Code Blue        │  ← Starts with "Code"
│ 2. Code Gray        │  ← Starts with "Code"
│ 3. Code Red         │  ← Starts with "Code"
└──────────────────────┘
```

**Only 3 matches shown** (TOP 5 limit, but only 3 groups match)

---

## Example 6: Multiple Groups in One Line

### Input with comma separation

**Input:**
```
VGroup: Code Blue, VGroup: Invalid Team, VGroup: OB Nurse
```

**Visual Result:**
```
┌──────────────────────────────────────────────────────────────┐
│ VGroup: Code Blue, VGroup: Invalid Team, VGroup: OB Nurse   │
│         ^^^^^^^^^ (BLACK)     ^^^^^^^^^^^^ (RED) ^^^^^^^^ (BLACK)
└──────────────────────────────────────────────────────────────┘
```

**Breakdown:**
- "VGroup: " → BLACK (prefix)
- "Code Blue" → BLACK (valid)
- ", VGroup: " → BLACK (delimiter + prefix)
- "Invalid Team" → RED (not in loaded groups)
- ", VGroup: " → BLACK (delimiter + prefix)
- "OB Nurse" → BLACK (valid)

---

## Example 7: Case Insensitive Matching

### All these are VALID (loaded group: "Code Blue")

```
Input                    Display Color
─────────────────────────────────────────
VGroup: Code Blue        BLACK (exact match)
VGroup: code blue        BLACK (lowercase match)
VGroup: CODE BLUE        BLACK (uppercase match)
VGroup: CoDe BlUe        BLACK (mixed case match)
VGroup: Code  Blue       BLACK (extra spaces OK)
```

### These are INVALID

```
Input                    Display Color
─────────────────────────────────────────
VGroup: Code             RED (partial match)
VGroup: Blue             RED (partial match)
VGroup: Code Blue Team   RED (extra word)
VGroup: CodeBlue         RED (no space)
```

---

## Example 8: Multi-line Recipient Field

### Editing Mode (TextArea)

**User double-clicks recipient field:**

```
┌──────────────────────────────────────────┐
│ 1st Recipient (Edit Mode)                │
├──────────────────────────────────────────┤
│ VGroup: Code Blue█                       │  ← Larger editing area
│                                          │     (60px height)
│                                          │
├──────────────────────────────────────────┤
│ Autocomplete suggestions appear below    │
│ Press Shift+Enter for new line          │
│ Press Enter to save                      │
└──────────────────────────────────────────┘
```

### Display Mode (After Save)

**User clicks outside or presses Enter:**

```
┌──────────────────────────────────────────┐
│ VGroup: Code Blue                        │  ← Standard height (24px)
│ VGroup: OB Nurse                         │  ← Colored validation
│ VGroup: Rapid Response                   │  ← All visible
└──────────────────────────────────────────┘
```

**Height automatically shrinks from 60px to 24px!**

---

## Color Legend

### Dark Mode
```
DEFAULT (valid/prefix):  WHITE (#FFFFFF)
INVALID (wrong name):    RED (#FF0000)
```

### Light Mode
```
DEFAULT (valid/prefix):  BLACK (#000000)
INVALID (wrong name):    RED (#FF0000)
```

---

## Key Behavior Points

1. ✅ **Only invalid group names are RED**
   - Prefixes ("VGroup: ", "Group: ") are never red
   - Valid groups remain default color
   - Invalid groups turn red

2. ✅ **Real-time validation**
   - Changes color immediately when fixed
   - No need to reload or refresh

3. ✅ **TOP 5 autocomplete**
   - Shows maximum 5 suggestions
   - Prioritizes prefix matches
   - Appears after 2+ characters

4. ✅ **Case insensitive**
   - "Code Blue" = "code blue"
   - Exact match on words, not case

5. ✅ **Multi-word support**
   - "OB Nurse" works correctly
   - "Code Blue Team" works correctly
   - Spaces are preserved

---

## User Experience Flow

```
┌─────────────────┐
│ Load Groups     │
│ File            │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ Edit Recipient  │
│ Field           │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ Type "VGroup:   │
│ OB"             │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ See Autocomplete│
│ TOP 5           │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ Select/Continue │
│ Typing          │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ Press Enter     │
│ to Save         │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ See Validation: │
│ RED = Invalid   │
│ BLACK = Valid   │
└─────────────────┘
```

---

## Success Indicators

When the feature is working correctly, you should see:

✅ Autocomplete appears after typing 2 characters
✅ Maximum 5 suggestions shown
✅ Groups starting with your text appear first
✅ Invalid groups turn RED
✅ Valid groups remain default color
✅ Prefixes always remain default color
✅ Color changes immediately when corrected
✅ Case doesn't matter for matching

---

## Testing Checklist

Use this to verify the feature works:

- [ ] Load voice groups file (5+ groups)
- [ ] Edit recipient field
- [ ] Type "VGroup: OB" (if "OB Nurse" is loaded but "OB" is not)
- [ ] Verify "OB" appears in RED
- [ ] Verify "VGroup: " appears in default color (not red)
- [ ] Type to change "OB" to "OB Nurse"
- [ ] Verify color changes from RED to default
- [ ] Type "VGroup: " again and start typing
- [ ] Verify autocomplete appears with max 5 suggestions
- [ ] Verify groups starting with your text appear first
- [ ] Save and verify all valid groups are default color

All items should be checked ✓ for successful implementation!

---

## End of Visual Guide
