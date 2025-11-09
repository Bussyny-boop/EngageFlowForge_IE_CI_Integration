# Recipient Cell Validation - Visual Guide

## Overview
This feature implements light orange cell highlighting for recipient columns (R1-R5) based on keyword validation.

## Validation Rules

### Valid Keywords (case-insensitive)
Cells containing ANY of these keywords will NOT be highlighted:
- **VCS**
- **Edge**
- **XMPP**
- **Vocera**

### Invalid Keywords (trigger highlighting)
Cells containing ONLY these keywords (without valid keywords) will be highlighted:
- **Custom unit**
- **Group**
- **Assigned**
- **CS**
- Any other text without valid keywords

## Behavior by Column

### R1 (1st Recipients)
**Highlighted (Light Orange) when:**
- Cell is empty/blank, OR
- No valid keywords found

**Examples:**
| Cell Value | Highlighted? | Reason |
|------------|--------------|--------|
| *(empty)* | ✅ Yes | Empty cell |
| "Custom unit" | ✅ Yes | No valid keywords |
| "Group" | ✅ Yes | No valid keywords |
| "Assigned" | ✅ Yes | No valid keywords |
| "CS" | ✅ Yes | No valid keywords |
| "Random text" | ✅ Yes | No valid keywords |
| "VCS" | ❌ No | Valid keyword |
| "Edge" | ❌ No | Valid keyword |
| "XMPP" | ❌ No | Valid keyword |
| "Vocera" | ❌ No | Valid keyword |
| "Custom unit using VCS" | ❌ No | Contains valid keyword |

### R2-R5 (2nd-5th Recipients)
**Highlighted (Light Orange) when:**
- No valid keywords found

**NOT highlighted when:**
- Cell is empty/blank

**Examples:**
| Cell Value | Highlighted? | Reason |
|------------|--------------|--------|
| *(empty)* | ❌ No | Empty is valid for R2-R5 |
| "Custom unit" | ✅ Yes | No valid keywords |
| "Group" | ✅ Yes | No valid keywords |
| "Assigned" | ✅ Yes | No valid keywords |
| "CS" | ✅ Yes | No valid keywords |
| "Random text" | ✅ Yes | No valid keywords |
| "VCS" | ❌ No | Valid keyword |
| "Edge" | ❌ No | Valid keyword |
| "XMPP" | ❌ No | Valid keyword |
| "Vocera" | ❌ No | Valid keyword |
| "Custom unit using VCS" | ❌ No | Contains valid keyword |

## Key Difference

The **critical difference** between R1 and R2-R5:

```
┌─────────────┬────────────┬──────────────┐
│ Cell Value  │ R1 (1st)   │ R2-R5 (2-5)  │
├─────────────┼────────────┼──────────────┤
│ (empty)     │ 🟠 Orange  │ ⬜ Normal    │
│ "Group"     │ 🟠 Orange  │ 🟠 Orange    │
│ "VCS"       │ ⬜ Normal  │ ⬜ Normal    │
└─────────────┴────────────┴──────────────┘
```

## Affected Tabs

This validation applies to recipient columns in all three tabs:
1. **Nurse Call** - nurseR1Col through nurseR5Col
2. **Clinical** - clinicalR1Col through clinicalR5Col  
3. **Orders** - ordersR1Col through ordersR5Col

## Color Used

**Light Orange (Moccasin)**: `#FFE4B5`

This is a subtle, non-intrusive color that indicates the cell needs attention.

## Technical Implementation

### ExcelParserV5.java
- `isValidFirstRecipient(String)` - Validates R1 (returns false for empty or no valid keywords)
- `isValidOtherRecipient(String)` - Validates R2-R5 (returns false only for no valid keywords)

### AppController.java
- `setupFirstRecipientColumn()` - Applies validation to R1 columns
- `setupOtherRecipientColumn()` - Applies validation to R2-R5 columns

## Testing

Comprehensive test suite in:
- `RecipientColumnValidationTest.java` - 32+ unit tests
- `RecipientValidationIntegrationTest.java` - Integration tests based on problem statement

All tests pass ✅

## Usage Example

When opening an Excel workbook in the GUI:
1. Navigate to the Nurse Call, Clinical, or Orders tab
2. Look at the recipient columns (R1-R5)
3. Cells with light orange background need attention:
   - For R1: Either add valid keywords (VCS/Edge/XMPP/Vocera) or fill in the empty cell
   - For R2-R5: Add valid keywords if the cell is not empty
