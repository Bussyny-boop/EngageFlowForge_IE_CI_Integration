# Dual-Profile Application Workflow Diagrams

## Complete Application Flow

```
┌─────────────────────────────────────────────────────────────────┐
│                    APPLICATION STARTUP                          │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                   ROLE SELECTION DIALOG                         │
│                   "What is your Role?"                          │
│                                                                 │
│                   ┌────────┐     ┌────────┐                    │
│                   │   IE   │     │   CI   │                    │
│                   └────────┘     └────────┘                    │
└─────────────────────────────────────────────────────────────────┘
                    │                    │
        ┌───────────┘                    └──────────┐
        ▼                                           ▼
┌──────────────────────┐              ┌──────────────────────────┐
│   IE MODE (FULL)     │              │   CI ACTION DIALOG       │
│                      │              │   "What would you like   │
│ All Features Enabled │              │   to do?"                │
│                      │              │                          │
│ • Load NDW           │              │ 1. Validate NDW          │
│ • Load XML           │              │ 2. Convert XML→Excel     │
│ • Load JSON          │              │ 3. Convert Rules→Excel   │
│ • Export JSON (all)  │              │ [Cancel]                 │
│ • Preview JSON       │              └──────────────────────────┘
│ • All Settings       │                     │
│ • Save / Save As     │       ┌─────────────┼─────────────┐
└──────────────────────┘       │             │             │
                               ▼             ▼             ▼
                        ┌──────────┐  ┌──────────┐  ┌──────────┐
                        │Validate  │  │Convert   │  │Convert   │
                        │NDW Flow  │  │XML Flow  │  │Rules Flow│
                        └──────────┘  └──────────┘  └──────────┘
                               │             │             │
                               │             └─────┬───────┘
                               ▼                   ▼
                    ┌──────────────────┐    ┌──────────────┐
                    │ Validation Data  │    │ Load File &  │
                    │ Loading Dialog   │    │ Return to    │
                    │                  │    │ CI Homepage  │
                    │ • Load Voice Grp │    └──────────────┘
                    │ • Load Assign    │           │
                    │ • Load Bedlist   │           │
                    │ • Clear Data     │           │
                    │                  │           │
                    │ [Begin] [Cancel] │           │
                    └──────────────────┘           │
                               │                   │
                               └───────────────────┘
                                       │
                                       ▼
                        ┌──────────────────────────┐
                        │   CI HOMEPAGE            │
                        │   (Restricted View)      │
                        │                          │
                        │ DISABLED:                │
                        │ ✗ Export JSON buttons    │
                        │ ✗ Preview JSON           │
                        │ ✗ Most settings          │
                        │                          │
                        │ ENABLED:                 │
                        │ ✓ Data Validation        │
                        │ ✓ Combine Config Group   │
                        │ ✓ Row Height Controls    │
                        │ ✓ Table Editing          │
                        │ ✓ Save on NDW (CI only!) │
                        └──────────────────────────┘
```

## Validate NDW Workflow (Detailed)

```
START: User selects "Validate NDW" in CI Action Dialog
│
├─► Step 1: Auto-trigger "Load NDW" file picker
│            │
│            ├─► User selects NDW Excel file
│            │   └─► File loads successfully
│            │
│            └─► User cancels file picker
│                └─► Return to CI Action Dialog
│
├─► Step 2: Show Validation Data Dialog
│            │
│            │   User can repeatedly:
│            │   ┌─────────────────────────────┐
│            │   │ Click "Load Voice Group"    │
│            │   │ Click "Load Assignment Role"│
│            │   │ Click "Load Bedlist"        │
│            │   │ Click "Clear Loaded Data"   │
│            │   └─────────────────────────────┘
│            │
│            ├─► User clicks "Begin Validation"
│            │   │
│            │   ├─► Run validation against loaded data
│            │   ├─► Refresh all tables
│            │   └─► Show "Validation complete" status
│            │       └─► User on CI Homepage
│            │
│            └─► User clicks "Cancel"
│                │
│                ├─► Clear all loaded validation data
│                ├─► Show "Validation cancelled" status
│                └─► User on CI Homepage
│
END: User on CI Homepage with NDW loaded
```

## UI State Comparison

```
┌────────────────────────────────────────────────────────────────┐
│                    FEATURE AVAILABILITY                        │
├──────────────────────────┬──────────────┬─────────────────────┤
│ Feature                  │   IE Mode    │     CI Mode         │
├──────────────────────────┼──────────────┼─────────────────────┤
│ Load NDW                 │      ✓       │        ✓            │
│ Load Engage XML          │      ✓       │        ✓            │
│ Load Engage Rules        │      ✓       │        ✓            │
│ Clear All                │      ✓       │        ✓            │
├──────────────────────────┼──────────────┼─────────────────────┤
│ Preview JSON             │      ✓       │        ✗            │
│ Export Nurse JSON        │      ✓       │        ✗            │
│ Export Clinical JSON     │      ✓       │        ✗            │
│ Export Orders JSON       │      ✓       │        ✗            │
├──────────────────────────┼──────────────┼─────────────────────┤
│ Save (Save As)           │      ✓       │        ✓            │
│ Save on NDW              │   HIDDEN     │   ✓ VISIBLE         │
├──────────────────────────┼──────────────┼─────────────────────┤
│ Merge Options            │      ✓       │        ✗            │
│ Interface Settings       │      ✓       │        ✗            │
│ Reset Defaults/Paths     │      ✓       │        ✗            │
│ Timeout Controls         │      ✓       │        ✗            │
│ Room Filters             │      ✓       │        ✗            │
│ Custom Tab Controls      │      ✓       │        ✗            │
├──────────────────────────┼──────────────┼─────────────────────┤
│ Voice Group Validation   │      ✓       │        ✓            │
│ Assignment Role Valid.   │      ✓       │        ✓            │
│ Bedlist Validation       │      ✓       │        ✓            │
│ Combine Config Group     │      ✓       │        ✓            │
│ Row Height Controls      │      ✓       │        ✓            │
├──────────────────────────┼──────────────┼─────────────────────┤
│ Table Editing            │      ✓       │        ✓            │
│ Navigation (Tabs)        │      ✓       │        ✓            │
│ Theme Toggle             │      ✓       │        ✓            │
└──────────────────────────┴──────────────┴─────────────────────┘

Legend: ✓ = Enabled, ✗ = Disabled (greyed out), HIDDEN = Not visible
```

## CI Save on NDW Button Location

```
Sidebar (CI Mode):
┌──────────────────────────┐
│ LOAD DATA                │
├──────────────────────────┤
│ 📄 Load NDW              │
│ 📋 Load Engage XML       │
│ 📥 Load Engage Rules     │
├──────────────────────────┤
│ 🗑️ Clear All             │
├──────────────────────────┤
│ 💾 Save on NDW  ◄────────┼── CI MODE ONLY
├──────────────────────────┤     (Hidden in IE Mode)
│ 📊 Units                 │
│ 🔔 Nurse Calls           │
│ 🏥 Clinicals             │
│ 💊 Orders                │
└──────────────────────────┘
```

## Dialog Interaction Flow

```
┌─────────────────────────────────────────────────────────────────┐
│ MODAL DIALOG BEHAVIOR                                           │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│ All dialogs in the dual-profile system are MODAL:              │
│                                                                 │
│ 1. Role Selection Dialog                                       │
│    • Blocks all app interaction until role selected            │
│    • Cannot close without selecting IE or CI                   │
│    • Sets profile for entire session                           │
│                                                                 │
│ 2. CI Action Dialog (CI Mode Only)                             │
│    • Shows immediately after role selection in CI mode         │
│    • User must select a workflow or cancel                     │
│    • Cancel returns to main app (with restrictions applied)    │
│                                                                 │
│ 3. Validation Data Dialog (Validate NDW workflow)              │
│    • Repeatable interactions for loading data                  │
│    • "Begin Validation" runs validation and closes             │
│    • "Cancel" clears data and closes                           │
│    • Load buttons trigger file pickers without closing dialog  │
│                                                                 │
│ 4. File Chooser Dialogs                                        │
│    • Standard JavaFX FileChooser                               │
│    • Used for NDW, XML, JSON, Voice Group, etc.                │
│    • User can cancel to abort file loading                     │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

## Data Flow in CI Mode

```
┌────────────────────────────────────────────────────────────────┐
│ CI MODE DATA FLOW                                              │
└────────────────────────────────────────────────────────────────┘

Option 1: Validate NDW
━━━━━━━━━━━━━━━━━━━━━
User Input:
  1. NDW Excel file
  2. Voice Group CSV/Excel (optional)
  3. Assignment Roles CSV/Excel (optional)
  4. Bedlist CSV/Excel (optional)

Processing:
  • Parse NDW into Units, Nurse Calls, Clinicals, Orders
  • Load validation datasets
  • Validate recipient columns against loaded data
  • Highlight invalid entries in tables

Output:
  • Populated tables with validation highlighting
  • Can edit and "Save on NDW" to update original file

Option 2: Convert Engage XML to Excel
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
User Input:
  1. Engage XML file

Processing:
  • Parse XML rules
  • Convert to Excel data model
  • Populate tables

Output:
  • Populated tables from XML rules
  • Can edit and "Save on NDW" to save as Excel

Option 3: Convert Engage Rules to Excel
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
User Input:
  1. Engage Rules JSON file

Processing:
  • Parse JSON rules
  • Convert to Excel data model
  • Populate tables

Output:
  • Populated tables from JSON rules
  • Can edit and "Save on NDW" to save as Excel
```

## Security and Workflow Notes

```
┌────────────────────────────────────────────────────────────────┐
│ IMPORTANT WORKFLOW CONSIDERATIONS                              │
├────────────────────────────────────────────────────────────────┤
│                                                                │
│ 1. Profile Selection is Permanent for Session                 │
│    • Once IE or CI is selected, it cannot be changed          │
│    • To switch profiles, user must restart application        │
│                                                                │
│ 2. Save on NDW Overwrites Original File                       │
│    • Confirmation dialog shown before overwrite               │
│    • Original file structure is preserved                     │
│    • Only changed values are updated                          │
│    • CI users should backup NDW files before editing          │
│                                                                │
│ 3. Validation Data is Session-Only                            │
│    • Loaded validation data not saved with NDW                │
│    • Must reload validation data each session                 │
│    • "Clear Loaded Data" removes all validation datasets      │
│                                                                │
│ 4. Cancel Operations Clean Up                                 │
│    • Canceling validation clears all loaded validation data   │
│    • Canceling file pickers returns to previous state         │
│    • No partial data is retained after cancel                 │
│                                                                │
│ 5. CI Mode Restrictions are UI-Level                          │
│    • Backend functionality still exists                       │
│    • Buttons are disabled, not removed                        │
│    • Profile is checked at button enable/disable time         │
│                                                                │
└────────────────────────────────────────────────────────────────┘
```
