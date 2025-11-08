# Engage FlowForge 2.0 - Complete Feature Guide

## Quick Start

### Launching the Application
```bash
# GUI Mode
mvn javafx:run

# Or run the JAR directly
java -jar target/engage-rules-generator-1.1.0.jar
```

### First-Time Setup
1. Launch the application
2. Click "📂 Load Excel" to select your configuration workbook
3. Review the loaded data in the tabs
4. Adjust adapter settings if needed
5. Generate or export JSON configurations

## Core Features

### 1. Excel Import & Export

#### Load Excel
- **Button**: 📂 Load Excel
- **Shortcut**: None (click button)
- **Function**: Import Excel workbook with flow configurations
- **Supported Format**: .xlsx (Excel 2007+)
- **Features**:
  - Automatic sheet detection (Units, Nurse Calls, Clinicals, Orders)
  - Header row auto-detection
  - EMDAN compliant row migration
  - Data validation on load
  - Shows summary of loaded data

#### Save Changes
- **Button**: 💾 Save Changes
- **Function**: Save modifications to a new Excel file
- **Output**: `<original>_Generated.xlsx`
- **Features**:
  - Preserves all formatting
  - Includes all edits made in tables
  - Prompts for save location
  - Confirms before overwrite

### 2. Flow Configuration

#### Preview JSON
Generate JSON preview without saving:
- **👨‍⚕️ Nursecall**: Preview Nursecall flow JSON
- **🏥 Clinical**: Preview Clinical flow JSON
- **📋 Orders**: Preview Orders flow JSON

**Features**:
- Instant preview in bottom panel
- Syntax-highlighted display
- Formatted with proper indentation
- Updates live with filter changes

#### Export JSON
Save JSON to file:
- **💾 Nursecall**: Export Nursecall JSON
- **💾 Clinical**: Export Clinical JSON
- **💾 Orders**: Export Orders JSON

**Features**:
- Choose export location
- Automatic .json extension
- Remembers last directory
- Merge identical flows option

### 3. Adapter Settings (Collapsible)

#### Adapter Reference Names
Configure custom adapter names:
- **Edge Adapter**: OutgoingWCTP interface name
- **VMP Adapter**: VMP interface name
- **Vocera Adapter**: Vocera interface name
- **XMPP Adapter**: XMPP interface name

**Default Values**:
```
Edge:   OutgoingWCTP
VMP:    VMP
Vocera: Vocera
XMPP:   XMPP
```

**Reset Button**: Restore all to defaults

#### Default Badge Alert Interfaces
Set default interfaces when Device A/B are blank:
- ☐ Via Edge
- ☐ Via VMP
- ☐ Via Vocera
- ☐ Via XMPP

**Note**: Selecting both Edge and VMP creates combined endpoints

#### Room Filters
Filter flows by room number:
- **Nursecall**: Room number filter
- **Clinical**: Room number filter
- **Orders**: Room number filter

**Collapse/Expand**:
- Click **▼** to collapse section
- Click **▶** to expand section
- Saves vertical space when not needed

### 4. Data Tables & Editing

#### Units Tab
View and edit facility/unit mappings:

**Columns**:
- Facility
- Unit Names
- Nurse Group
- Clinical Group
- Orders Group
- No Caregiver Group
- Comments

**Editing**:
- Double-click any cell to edit
- Press Enter to save
- Press Esc to cancel
- Changes saved in memory

**Sorting**: Click any column header to sort

**Filtering**: Use "🔍 Filter by Config Group" dropdown

#### Nurse Calls Tab
Configure nursecall alarm flows:

**Key Columns**:
- In Scope (checkbox - include in export)
- Config Group
- Alarm Name
- Sending Name
- Priority (Urgent, Normal, etc.)
- Device A/B (Badge interface)
- Ringtone
- Response Options
- Escalation settings
- Time/Recipient pairs (1-5)

**Features**:
- Editable cells
- Header checkbox to select/deselect all
- Column sorting
- Config group filtering

#### Clinicals Tab
Configure clinical monitoring flows:

**Additional Fields**:
- EMDAN Compliant flag
- All nursecall features

**Auto-Migration**:
- EMDAN-flagged rows auto-move from Nurse to Clinical

#### Orders Tab
Configure order-based flows:
- Similar structure to Nurse Calls
- Specialized for order workflows

### 5. Advanced Features

#### 🌙 Dark Mode
Toggle between light and dark themes:

**How to Toggle**:
1. Click "🌙 Dark Mode" button in header
2. Button changes to "☀️ Light Mode"
3. All UI elements adapt instantly

**Theme Comparison**:
| Element | Light Mode | Dark Mode |
|---------|-----------|-----------|
| Background | #f7f8fa | #1e1e1e |
| Cards | #ffffff | #3e3e42 |
| Text | #333333 | #e0e0e0 |
| Accent | #00979D | #00979D |

**Benefits**:
- Reduced eye strain
- Better for low-light environments
- Modern appearance
- Energy saving (OLED screens)

#### Column Sorting
Click any column header to sort data:

**Features**:
- **First Click**: Sort ascending
- **Second Click**: Sort descending
- **Third Click**: Remove sort (original order)
- **Visual Indicator**: Arrow shows sort direction
- **Works with Filters**: Sorts visible rows only

**Sortable Data Types**:
- Text: Alphabetical
- Numbers: Numerical
- Booleans: True before False

#### Merge Identical Flows
Checkbox in header to combine similar flows:

**When Enabled**:
- Combines flows with identical:
  - Priority
  - Device
  - Ringtone
  - Recipients
  - Timing
- Reduces JSON file size
- Simplifies rule management

**When Disabled**:
- Each alarm gets separate flow
- More explicit configuration
- Easier to debug

#### Progress Indicators
Visual feedback during operations:

**Appears During**:
- Excel file loading
- JSON generation
- JSON file export

**Location**: Next to status label (bottom)

**Indicator**: Spinning circle (teal color)

### 6. Status & Feedback

#### Status Label
Bottom-left corner shows current state:

**Examples**:
```
Ready
Loading Excel file...
Excel loaded successfully
Generating JSON...
Generated NurseCall JSON
Exporting JSON...
Units: 10/10 | Nurse: 25/25 | Clinical: 15/15
```

#### Window Title
Shows current file:
```
Engage FlowForge 2.0
↓ (after loading)
Engage FlowForge 2.0 - [MyConfig.xlsx]
```

#### Dialogs
- **Info**: ✅ Success messages
- **Warning**: ⚠️ Important notices
- **Error**: ❌ Error messages
- **Confirm**: Questions requiring user choice

### 7. File Management

#### Load Excel
1. Click "📂 Load Excel"
2. Browse to .xlsx file
3. Click "Open"
4. Wait for progress indicator
5. Review loaded data

#### Clear All
1. Click "🗑️ Clear All"
2. Confirm action (⚠️ warning)
3. All data cleared
4. Ready for new file

#### Reset Paths
1. Click "🔄 Reset Paths"
2. File chooser directories reset to default
3. Next open/save starts at home directory

## Keyboard Shortcuts

### Table Navigation
- **Tab**: Move to next cell
- **Shift+Tab**: Move to previous cell
- **Enter**: Edit selected cell / Save edit
- **Esc**: Cancel edit
- **↑↓**: Navigate rows
- **←→**: Navigate columns

### General
- **Ctrl+C**: Copy selected cell
- **Ctrl+V**: Paste into selected cell
- **F2**: Edit selected cell

## Tips & Tricks

### 1. Efficient Workflow
```
1. Load Excel
2. Filter by Config Group
3. Review/edit specific group
4. Preview JSON to verify
5. Export when satisfied
6. Load next group or file
```

### 2. Using Filters Effectively
- Start with "All" to see full dataset
- Select specific group to focus work
- Filters work with sorting
- "In Scope" checkbox auto-updates with filter

### 3. Managing Large Files
- Use Config Group filters to work on sections
- Collapse Adapter Settings when not needed
- Sort by relevant columns to find data quickly
- Use Room Filter for specific locations

### 4. JSON Export Best Practices
- Always preview before export
- Check "Merge Identical Flows" for production
- Uncheck merge for troubleshooting
- Export to version-controlled directory

### 5. Dark Mode Usage
- Use in evening/night hours
- Matches modern development tools
- Easier on eyes during long sessions
- Toggle based on environment lighting

### 6. Data Quality
- Sort by Config Group to check grouping
- Sort by Alarm Name to find duplicates
- Use filters to isolate problems
- Edit directly in tables - no spreadsheet needed

## Troubleshooting

### Excel Won't Load
**Problem**: File chooser opens but file won't load

**Solutions**:
- Ensure file is .xlsx format (not .xls)
- Close file in Excel if open
- Check file isn't corrupted
- Verify sheet names match expected

### JSON Preview Shows Errors
**Problem**: JSON preview shows error messages

**Solutions**:
- Load an Excel file first
- Ensure data is in correct format
- Check adapter reference names
- Try clearing all and reloading

### Table Edits Not Saving
**Problem**: Changes don't persist

**Solutions**:
- Press Enter after editing
- Don't just click away
- Ensure cell is not disabled
- Check if file is loaded

### Dark Mode Looks Wrong
**Problem**: Colors don't match expected

**Solutions**:
- Toggle theme off and on again
- Restart application
- Check CSS files exist
- Verify JavaFX version

### Sorting Not Working
**Problem**: Clicking headers doesn't sort

**Solutions**:
- Click on header text, not column border
- Wait for previous sort to complete
- Try different column
- Reload data if needed

## Preferences & Settings

### Automatically Saved
- Last Excel directory
- Last JSON directory
- Window size/position (JavaFX default)

### Reset Options
- **Reset Defaults**: Adapter reference names only
- **Reset Paths**: File chooser directories only
- **Clear All**: All loaded data (with confirmation)

### Not Saved (Session Only)
- Theme selection (resets to light)
- Adapter section collapse state
- Filter selections
- Sort orders
- Table edits (until exported)

## Data Format Requirements

### Excel Workbook Structure
Required sheets:
1. **Unit Breakdown**: Facility/Unit mappings
2. **Nurse Call**: Nursecall configurations
3. **Patient Monitoring**: Clinical configurations
4. **Orders** (optional): Order configurations

### Expected Columns
See `docs/sample-workbook.md` for detailed schema

### Data Validation
- Headers detected automatically (row 1-5)
- Empty rows ignored
- EMDAN column triggers migration
- Boolean values: true/false, yes/no, 1/0

## Performance Notes

### Typical Performance
- **Load Time**: 1-3 seconds for 1000 rows
- **Sort Time**: < 200ms for 1000 rows
- **JSON Generation**: < 1 second
- **Theme Toggle**: < 100ms

### Large Files (5000+ rows)
- Load time increases linearly
- Use filters to work on subsets
- Sorting still fast (< 500ms)
- JSON export may take 2-3 seconds

### Memory Usage
- Base: ~100 MB
- Per 1000 rows: +10 MB
- Dark theme: +1 MB
- Typical total: 150-200 MB

## Version Information

**Current Version**: 1.1.0

**JavaFX Version**: 21.0.3

**Java Version**: 17 (minimum)

**Build Date**: See pom.xml

## Getting Help

### In-App Help
- Hover over buttons for tooltips
- Check status label for current state
- Dialog messages explain issues

### Documentation
- `USER_GUIDE.md` - End user guide
- `UI_IMPROVEMENTS_SUMMARY.md` - UI changes
- `CSS_STYLING_GUIDE.md` - Theme customization
- `FUTURE_ENHANCEMENTS_SUMMARY.md` - Advanced features

### Support
- GitHub Issues: Report bugs
- Pull Requests: Contribute improvements
- Discussions: Ask questions

## Appendix: Button Reference

| Button | Icon | Function | Location |
|--------|------|----------|----------|
| Load Excel | 📂 | Import workbook | Excel Import |
| Save Changes | 💾 | Save to new file | Excel Import |
| Clear All | 🗑️ | Clear all data | Excel Import |
| Reset Paths | 🔄 | Reset directories | Excel Import |
| Preview Nursecall | 👨‍⚕️ | Preview JSON | Flow Config |
| Preview Clinical | 🏥 | Preview JSON | Flow Config |
| Preview Orders | 📋 | Preview JSON | Flow Config |
| Export Nursecall | 💾 | Export to file | Flow Config |
| Export Clinical | 💾 | Export to file | Flow Config |
| Export Orders | 💾 | Export to file | Flow Config |
| Reset Defaults | Reset | Adapter defaults | Adapter Settings |
| Collapse/Expand | ▼/▶ | Toggle section | Adapter Settings |
| Dark Mode | 🌙/☀️ | Toggle theme | Header Bar |

## Appendix: Color Reference

### Light Mode
```
Primary:    #00979D (Teal)
Background: #f7f8fa (Light gray)
Cards:      #ffffff (White)
Text:       #333333 (Dark gray)
Border:     #e0e4e8 (Light border)
```

### Dark Mode
```
Primary:    #00979D (Teal - same)
Background: #1e1e1e (Dark)
Cards:      #3e3e42 (Dark gray)
Text:       #e0e0e0 (Light gray)
Border:     #555555 (Medium gray)
```

## Appendix: File Locations

```
Project Root
├── src/main/java/com/example/exceljson/
│   ├── Main.java
│   ├── ExcelJsonApplication.java
│   └── AppController.java
├── src/main/resources/
│   ├── com/example/exceljson/
│   │   └── App.fxml
│   └── css/
│       ├── vocera-theme.css
│       └── vocera-theme-dark.css
├── target/
│   └── engage-rules-generator-1.1.0.jar
└── docs/
    └── sample-workbook.md
```
