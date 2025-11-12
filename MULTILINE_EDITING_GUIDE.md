# Multiline Cell Editing with Shift+Enter

## Feature Overview

The application now supports multiline text entry in editable table cells, similar to Microsoft Excel's behavior. This allows users to enter multiple lines of text within a single cell.

## How to Use

### Entering Multiline Text

1. **Start Editing**: Double-click on any editable cell in the tables (Units, Nurse Calls, Clinicals, or Orders tabs)

2. **Enter Text**: Type your text normally

3. **Add Newlines**: 
   - Press **Shift + Enter** to insert a newline within the cell
   - The cursor will move to the next line within the same cell
   - You can add multiple lines this way

4. **Commit Changes**:
   - Press **Enter** (without Shift) to save your changes and exit editing mode
   - Press **Tab** to save and move to the next cell
   - Click outside the cell to save automatically

5. **Cancel Editing**:
   - Press **Escape** to discard changes and exit editing mode

## Example

When editing a cell, you can now enter text like:

```
Primary Recipient
Secondary Recipient
Tertiary Recipient
```

Instead of being limited to a single line.

## Technical Details

### What Changed

- All editable text columns now use a `TextArea` instead of a `TextField` for editing
- The editing area automatically expands to show 3 lines by default
- Text wrapping is enabled for better readability
- The cell height adjusts when in editing mode to accommodate multiple lines

### Keyboard Shortcuts Summary

| Key Combination | Action |
|----------------|---------|
| Double-click | Start editing cell |
| **Shift + Enter** | Insert newline (stay in edit mode) |
| **Enter** | Commit edit and exit |
| **Tab** | Commit edit and move to next cell |
| **Escape** | Cancel edit |

## Benefits

- **Better Data Organization**: Enter structured text with proper line breaks
- **Excel Compatibility**: Familiar behavior for users coming from Excel
- **Improved Readability**: Multiline text is easier to read and format
- **No Data Loss**: Text is preserved exactly as entered, including newlines

## Affected Areas

This feature is available in all editable text columns across all tabs:
- Units tab (all text columns)
- Nurse Calls tab (all text columns, including recipient columns R1-R5)
- Clinicals tab (all text columns, including recipient columns R1-R5)
- Orders tab (all text columns, including recipient columns R1-R5)
- Custom tab columns (dynamically added columns)

## Notes

- Newline characters are preserved when saving to Excel
- The cell displays the full multiline text when not in editing mode
- Very long text may be truncated in the display view, but all content is preserved
