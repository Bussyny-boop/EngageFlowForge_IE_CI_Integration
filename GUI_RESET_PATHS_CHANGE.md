# GUI Changes - Reset Paths Button Moved

## What Changed

The "Reset Paths" button has been moved from its previous location to the top row of buttons beside "Save Excel".

## Before

The "Reset Paths" button was located in the third row, after the "Vocera Badges Alert Interface" checkboxes:

```
[📂 Load Excel] [💾 Save Excel (Save As)]

[Status Label]

[🔧 Preview Nursecall] [🏥 Preview Clinical] [💊 Preview Orders] [📤 Export Nursecall] [📤 Export Clinicals] [📤 Export Orders]

Vocera Badges Alert Interface: [✓ Via Edge] [✓ Via VMP] [🔄 Reset Paths]

Room Filter: Nursecall: [____] Clinical: [____] Orders: [____]
```

## After

The "Reset Paths" button is now in the first row beside "Save Excel":

```
[📂 Load Excel] [💾 Save Excel (Save As)] [🔄 Reset Paths]

[Status Label]

[🔧 Preview Nursecall] [🏥 Preview Clinical] [💊 Preview Orders] [📤 Export Nursecall] [📤 Export Clinicals] [📤 Export Orders]

Vocera Badges Alert Interface: [✓ Via Edge] [✓ Via VMP]

Room Filter: Nursecall: [____] Clinical: [____] Orders: [____]
```

## Benefits

1. **Better organization**: Path management (Load, Save, Reset Paths) are now grouped together
2. **Cleaner layout**: The "Vocera Badges Alert Interface" row is less cluttered
3. **Improved discoverability**: Users can more easily find the Reset Paths feature alongside other file operations

## Technical Details

The change was made in `/src/main/resources/com/example/exceljson/App.fxml`:
- Moved the `resetPathsButton` from the HBox containing the Vocera interface checkboxes (line 54)
- To the HBox containing Load and Save Excel buttons (new line 32)
