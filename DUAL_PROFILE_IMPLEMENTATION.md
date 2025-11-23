# Dual-Profile Application Framework - UI Flow Documentation

## Overview
The application now supports two distinct user profiles that determine available features and workflows:
- **IE (Implementation Engineer)**: Full functionality mode
- **CI (Clinical Informatics)**: Restricted mode with guided workflows

## Application Launch Flow

### 1. Role Selection (Startup)
When the application launches, the user is immediately presented with a modal dialog:

```
┌────────────────────────────────────┐
│      Role Selection                │
├────────────────────────────────────┤
│  What is your Role?                │
│                                    │
│    ┌────┐         ┌────┐          │
│    │ IE │         │ CI │          │
│    └────┘         └────┘          │
│                                    │
└────────────────────────────────────┘
```

- **IE Button**: Launches full application with all features enabled
- **CI Button**: Launches restricted mode and shows CI Action Dialog

### 2a. IE Mode (Full Application)
After selecting IE, the user sees the standard application with:
- ✅ All load buttons enabled (Load NDW, Load Engage XML, Load Engage Rules)
- ✅ All export buttons enabled (Preview JSON, Export Nurse JSON, etc.)
- ✅ All settings available
- ✅ Standard Save and Save As buttons
- ❌ "Save on NDW" button is HIDDEN

### 2b. CI Mode (Guided Workflow)
After selecting CI, the user immediately sees the CI Action Dialog:

```
┌────────────────────────────────────────────────────┐
│      Clinical Informatics                          │
├────────────────────────────────────────────────────┤
│  What would you like to do?                        │
│                                                    │
│  ┌──────────────────────────────────────────────┐ │
│  │         Validate NDW                         │ │
│  └──────────────────────────────────────────────┘ │
│                                                    │
│  ┌──────────────────────────────────────────────┐ │
│  │    Convert Engage XML to Excel               │ │
│  └──────────────────────────────────────────────┘ │
│                                                    │
│  ┌──────────────────────────────────────────────┐ │
│  │    Convert Engage Rules to Excel             │ │
│  └──────────────────────────────────────────────┘ │
│                                                    │
│  ┌────────┐                                       │
│  │ Cancel │                                       │
│  └────────┘                                       │
└────────────────────────────────────────────────────┘
```

## CI Workflow Details

### Option 1: Validate NDW Workflow

**Step 1**: Automatically triggers "Load NDW" file picker
- User selects NDW Excel file
- File loads into application

**Step 2**: Validation Data Dialog appears
```
┌──────────────────────────────────────────────────────┐
│      Validation Data                                 │
├──────────────────────────────────────────────────────┤
│  Load validation datasets                            │
│                                                      │
│  Current NDW file: example.xlsx                      │
│                                                      │
│  Use the buttons below to load validation data,      │
│  then click Begin Validation.                        │
│                                                      │
│  ┌──────────────┐  ┌────────────────────┐          │
│  │ Load Voice   │  │ Load Assignment    │          │
│  │    Group     │  │      Roles         │          │
│  └──────────────┘  └────────────────────┘          │
│                                                      │
│  ┌──────────────┐  ┌────────────────────┐          │
│  │ Load Bedlist │  │ Clear Loaded Data  │          │
│  └──────────────┘  └────────────────────┘          │
│                                                      │
│  ┌──────────────────┐  ┌────────┐                  │
│  │ Begin Validation │  │ Cancel │                  │
│  └──────────────────┘  └────────┘                  │
└──────────────────────────────────────────────────────┘
```

**Step 3**: After validation completes, user is on CI Homepage

### Option 2: Convert Engage XML to Excel
- Triggers "Load Engage XML" file picker
- After loading, returns to CI Homepage

### Option 3: Convert Engage Rules to Excel
- Triggers "Load Engage Rules" (JSON) file picker
- After loading, returns to CI Homepage

## CI Homepage (Restricted View)

When in CI mode, the main application view has the following restrictions:

### Disabled Elements (Greyed Out):
- ❌ All Export JSON buttons (Preview JSON, Export Nurse JSON, Export Clinical JSON, Export Orders JSON)
- ❌ Interface Settings (Edge, VCS, Vocera, XMPP reference names)
- ❌ Default checkboxes for interfaces
- ❌ Reset Defaults and Reset Paths buttons
- ❌ Merge options (No Merge, Merge by Config Group, Merge Across Config Group)
- ❌ Loaded timeout controls
- ❌ Room filter fields
- ❌ Custom tab controls

### Enabled Elements (Still Available):
- ✅ Load buttons (Load NDW, Load Engage XML, Load Engage Rules)
- ✅ Data Validation section (Load Voice Group, Load Assignment Roles, Load Bedlist, Clear buttons)
- ✅ Combine Configuration Group checkbox
- ✅ Table Row Height sliders for all tabs
- ✅ Navigation between tabs (Units, Nurse Calls, Clinicals, Orders)
- ✅ Table editing functionality
- ✅ **NEW: "Save on NDW" button** (appears in sidebar, replaces standard Save)

### Save on NDW Button (CI Mode Only)

Location: Sidebar, below "Clear All" button

```
┌────────────────────────┐
│ 📄 Load NDW            │
├────────────────────────┤
│ 📋 Load Engage XML     │
├────────────────────────┤
│ 📥 Load Engage Rules   │
├────────────────────────┤
│ 🗑️ Clear All           │
├────────────────────────┤
│ 💾 Save on NDW         │  ← CI MODE ONLY
└────────────────────────┘
```

**Functionality:**
- Saves modified data back to the originally loaded NDW Excel file
- Only updates values that were changed by the user
- Confirms overwrite before saving
- Maintains original file structure

## Key Differences: IE vs CI Mode

| Feature | IE Mode | CI Mode |
|---------|---------|---------|
| Role Selection at Startup | ✅ One-time | ✅ One-time |
| CI Action Dialog | ❌ Never shown | ✅ Shown after role selection |
| Export JSON Buttons | ✅ Enabled | ❌ Disabled |
| Preview JSON | ✅ Enabled | ❌ Disabled |
| Interface Settings | ✅ All available | ❌ Disabled |
| Merge Options | ✅ All available | ❌ Disabled |
| Data Validation | ✅ Available | ✅ Available |
| Combine Config Group | ✅ Available | ✅ Available |
| Row Height Controls | ✅ Available | ✅ Available |
| Standard Save/Save As | ✅ Enabled | ✅ Enabled |
| Save on NDW | ❌ Hidden | ✅ Visible & Enabled |

## Implementation Details

### Files Modified:
1. **UserProfile.java** - New enum for IE/CI modes
2. **ExcelJsonApplication.java** - Role selection dialog at startup
3. **AppController.java** - Profile management, UI restrictions, CI workflows
4. **App.fxml** - Save on NDW button added to sidebar

### Key Methods:
- `setUserProfile(UserProfile)` - Sets the active profile
- `applyProfileRestrictions()` - Applies UI restrictions based on profile
- `showCIActionDialog()` - Shows the CI workflow menu
- `handleValidateNdwWorkflow()` - Implements NDW validation flow
- `showValidationDataDialog()` - Validation data loading dialog
- `saveOnNdw()` - Saves changes back to original NDW file

### Technical Notes:
- Profile selection uses JavaFX Alert dialogs with modal behavior
- UI restrictions are applied once at profile selection time
- CI workflows use existing file loading methods
- All dialogs block background interaction until dismissed
- Cancel operations properly clean up temporary data
