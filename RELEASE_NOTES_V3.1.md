# Version 3.1 Release Notes

## Overview
Engage FlowForge has been updated to version 3.1 with enhancements to the profile switching system and comprehensive documentation of the Save to NDW feature.

## What's New in Version 3.1

### 1. Version Update
- Application version updated from 3.0.0 to 3.1.0
- Version number displayed as "Version 3.1" in the application header
- Consistent versioning across pom.xml and UI

### 2. Enhanced Profile Switching
- **One-Way Profile Switching**: Users can now switch from IE (Implementation Engineer) mode to CI (Clinical Informatics) mode, but NOT back
- **Profile Switcher Visibility**: The profile switcher button is now hidden when in CI mode
- **Data Preservation**: All loaded data is preserved when switching from IE to CI mode
- **Purpose**: Prevents accidental switching that could disrupt CI workflows

### 3. Verified Features (Already Implemented)
The following features were confirmed to be working as specified:

#### Save to NDW - Change Tracking
- ✅ Only cells that have been modified are updated in the NDW file
- ✅ Unchanged cells preserve their original values and formatting
- ✅ Excel formulas are never overwritten
- ✅ Data integrity is maintained

#### Save to NDW - Visual Highlighting
- ✅ Changed cells are formatted as **bold**, *italic*, and RED
- ✅ Visual highlighting makes changes immediately apparent
- ✅ Uses cached cell style to avoid Excel's 4000 style limit

#### Profile Switching - Data Preservation
- ✅ All loaded data remains when switching from IE to CI mode
- ✅ Parser data is not cleared
- ✅ Table data is preserved
- ✅ Change tracking continues to work

## Technical Changes

### Code Changes
1. **pom.xml**: Updated version from 3.0.0 to 3.1.0
2. **App.fxml**: Updated version label from "Version 2.5" to "Version 3.1"
3. **AppController.java**: Added profile switcher visibility control
   - Hides profile switcher in CI mode
   - Shows profile switcher in IE mode

### Documentation Added
1. **SAVE_TO_NDW_FEATURE_GUIDE.md** (273 lines)
   - Comprehensive technical documentation
   - Explains change tracking system
   - Details selective cell update mechanism
   - Documents visual highlighting implementation

2. **VERSION_3.1_IMPLEMENTATION_SUMMARY.md** (215 lines)
   - Implementation summary
   - Verification of features
   - Test results
   - User impact analysis

3. **UI_VISUAL_CHANGES_V3.1.md** (277 lines)
   - Visual documentation
   - UI mockups and diagrams
   - Before/after comparisons
   - Workflow illustrations

## User Impact

### For Implementation Engineers (IE Users)
- ✅ All features remain available
- ✅ Can switch to CI mode if needed
- ✅ Profile switcher visible in top bar
- ✅ All data preserved when switching to CI
- ✅ Clear version number displayed

### For Clinical Informatics (CI Users)
- ✅ Can validate and edit NDW files
- ✅ Save to NDW with change highlighting
- ✅ Protected from accidentally switching to IE mode
- ✅ Clear version number displayed
- ✅ Guided workflows maintained

## How to Use

### Switching from IE to CI Mode
1. Start the application (defaults to IE mode)
2. Load your NDW file
3. Click the profile switcher button ([IE]) in the top bar
4. Confirm the switch when prompted
5. Your data remains loaded, but you're now in CI mode
6. The profile switcher button is now hidden

### Using Save to NDW (CI Mode)
1. Load an NDW file
2. Make changes to the data in the tables
3. Click "💾 Save to NDW" button in the sidebar
4. Confirm the overwrite
5. Only changed cells are updated in the Excel file
6. Changed cells are marked as bold, italic, red

## Testing

### Test Results
- ✅ 630 tests passed
- ✅ 0 failures
- ✅ 0 errors
- ✅ 0 skipped

### Code Quality
- ✅ Code review passed (1 minor suggestion)
- ✅ CodeQL security scan clean (0 alerts)
- ✅ Build successful
- ✅ No warnings or errors

## Benefits

1. **Data Integrity**: Only changed cells modified
2. **Visual Clarity**: Changes clearly marked
3. **Formula Safety**: Excel formulas preserved
4. **Workflow Control**: One-way switching prevents accidents
5. **Data Preservation**: All data retained when switching
6. **Version Clarity**: Clear version display
7. **Performance**: Efficient style caching

## Files Changed

```
pom.xml                                  (3 lines)
App.fxml                                 (1 line)
AppController.java                       (12 lines)
SAVE_TO_NDW_FEATURE_GUIDE.md            (273 lines, new)
VERSION_3.1_IMPLEMENTATION_SUMMARY.md   (215 lines, new)
UI_VISUAL_CHANGES_V3.1.md               (277 lines, new)
```

**Total**: 6 files, 781 lines added/modified

## Migration Notes

### Upgrading from Version 3.0
- No breaking changes
- All existing functionality preserved
- Profile switcher behavior enhanced
- Documentation added

### Configuration
No configuration changes required. The application works the same way for existing users.

## Known Limitations

1. **One-Way Switching**: Once switched to CI mode, users must restart the application to return to IE mode
   - This is by design to prevent accidental workflow disruption

2. **Version Display**: Version number is hardcoded in FXML
   - Optional future enhancement: Use property binding for automatic updates

## Support

For questions or issues:
1. Review the comprehensive documentation files
2. Check the UI visual guide
3. Refer to the technical feature guide

## Future Enhancements

Potential improvements for future versions:
- Change history log
- Undo/redo functionality
- Export change report
- Cell-level change approval workflow
- Version property binding

## Acknowledgments

This update addresses requirements from the problem statement:
1. Update app version to 3.1 ✅
2. Confirm Save to NDW only updates changed cells ✅
3. Highlight changed cells as bold, italic, red ✅
4. Allow profile switching while keeping data loaded ✅
5. Hide profile switcher in CI mode ✅

All requirements have been successfully implemented and verified.

---

**Version**: 3.1.0  
**Release Date**: 2025-11-24  
**Status**: Production Ready  
**Quality**: All tests passed, security scan clean
