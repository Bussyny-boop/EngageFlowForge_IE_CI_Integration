# Engage FlowForge 2.0 - User Guide

## Table of Contents
1. [Overview](#overview)
2. [System Requirements](#system-requirements)
3. [Installation](#installation)
4. [Getting Started](#getting-started)
5. [Using the Graphical Interface (GUI)](#using-the-graphical-interface-gui)
6. [Using the Command-Line Interface (CLI)](#using-the-command-line-interface-cli)
7. [Excel Workbook Requirements](#excel-workbook-requirements)
8. [Features and Functionality](#features-and-functionality)
9. [Troubleshooting](#troubleshooting)
10. [FAQ](#faq)

---

## Overview

**Engage FlowForge 2.0** is a Java application that converts Vocera Engage Excel configuration sheets into JSON rule files. The application provides both a graphical user interface (GUI) and a command-line interface (CLI) for processing Excel workbooks containing nurse call and patient monitoring configurations.

### Key Capabilities
- Import Excel workbooks with nurse call and patient monitoring configurations
- Edit configuration data in an intuitive tabbed interface
- Automatically reclassify alarms based on EMDAN compliance
- Generate JSON output files compatible with Vocera Engage systems
- Support for multiple devices (Device A and Device B)
- Export modified configurations back to Excel
- Command-line batch processing for automation

---

## System Requirements

### Minimum Requirements
- **Operating System**: Windows 10/11, macOS 10.14+, or Linux (Ubuntu 18.04+)
- **Java Runtime**: Java 17 or higher
- **Memory**: 512 MB RAM minimum (1 GB recommended)
- **Disk Space**: 100 MB free space
- **Display**: 1024x768 minimum resolution (1280x800+ recommended)

### Required Software
- **Java Development Kit (JDK) 17+** with JavaFX support
  - For Windows/Linux: [Liberica JDK Full](https://bell-sw.com/pages/downloads/) (includes JavaFX)
  - For macOS: [Liberica JDK Full](https://bell-sw.com/pages/downloads/)
- **Microsoft Excel** or compatible spreadsheet application (for creating/editing input files)

### Verifying Java Installation
Open a terminal/command prompt and run:
```bash
java -version
```

You should see output indicating Java 17 or higher. For example:
```
openjdk version "17.0.x" 2023-xx-xx
OpenJDK Runtime Environment (build 17.0.x+x)
```

---

## Installation

### Option 1: Using Pre-built MSI Installer (Windows)
1. Download the latest MSI installer from the GitHub releases page
2. Double-click the MSI file to launch the installer
3. Follow the installation wizard prompts
4. The application will be installed with a desktop shortcut

### Option 2: Using the JAR File (All Platforms)
1. Download the latest JAR file: `engage-rules-generator-1.1.0.jar`
2. Place the JAR file in a convenient location (e.g., `C:\FlowForge\` on Windows or `/opt/flowforge/` on Linux)
3. Ensure Java 17+ is installed and accessible from the command line

### Option 3: Building from Source
If you have Maven installed:
```bash
git clone https://github.com/Bussyny-boop/NDW-To-Engage-Rules.git
cd NDW-To-Engage-Rules
mvn clean package
```

The built JAR will be available at: `target/engage-rules-generator-1.1.0.jar`

---

## Getting Started

### Quick Start - GUI Mode

1. **Launch the application:**
   - **Windows MSI Install**: Double-click the desktop shortcut or Start Menu entry
   - **JAR File**: Open terminal and run:
     ```bash
     java -jar engage-rules-generator-1.1.0.jar
     ```

2. **Load an Excel workbook:**
   - Click the **📂 Load Excel** button
   - Select your Excel configuration file (.xlsx)
   - The application will parse and display the data in four tabs

3. **Review and edit data:**
   - Navigate through the **Units**, **Nurse Calls**, **Clinicals**, and **Orders** tabs
   - Edit any cell by double-clicking it

4. **Generate JSON output:**
   - Click **🏥 Generate Clinical JSON** or **🔧 Generate NurseCall JSON**
   - Review the generated JSON in the preview pane
   - Click **📤 Export JSON** to save the file

### Quick Start - CLI Mode

For automated batch processing:

```bash
# Export JSON from an Excel workbook
java -cp engage-rules-generator-1.1.0.jar \
    com.example.exceljson.jobs.JobRunner export-json input.xlsx output.json

# Run diagnostic smoke test
java -cp engage-rules-generator-1.1.0.jar \
    com.example.exceljson.jobs.JobRunner fail
```

---

## Using the Graphical Interface (GUI)

### Application Layout

The GUI features a modern orange-themed interface with the following main areas:

```
┌─────────────────────────────────────────────────────────────┐
│  ⚙️ Engage FlowForge 2.0                                    │
├─────────────────────────────────────────────────────────────┤
│  [📂 Load Excel] [💾 Save Excel (Save As)]                 │
│  Ready                                                       │
│  [🔧 Preview Nursecall] [🏥 Preview Clinical]              │
│  [💊 Preview Orders]                                        │
│  [📤 Export Nursecall] [📤 Export Clinicals]               │
│  [📤 Export Orders]                                         │
│  ☑️ 🔀 Merge Identical Flows (Advanced)                     │
│  🔗 Edge Reference Name: [OutgoingWCTP]                     │
│  🔗 VCS Reference Name: [VMP]                               │
│  [🔄 Reset Defaults] [🔄 Reset Paths]                      │
│  Vocera Badges Alert Interface:                             │
│  ☐ Via Edge  ☐ Via VMP                                      │
├─────────────────────────────────────────────────────────────┤
│  ┌──────┬────────────────┬──────────────┬─────────┐        │
│  │📊Units│🔔 Nurse Calls │🏥 Clinicals  │💊Orders │        │
│  └──────┴────────────────┴──────────────┴─────────┘        │
│  Filter by Config Group: [Dropdown]                         │
│  ┌─────────────────────────────────────────┐               │
│  │                                         │               │
│  │  [Table with configuration data]        │               │
│  │                                         │               │
│  └─────────────────────────────────────────┘               │
├─────────────────────────────────────────────────────────────┤
│  [JSON Preview Pane]                                        │
└─────────────────────────────────────────────────────────────┘
```

### Control Buttons

| Button | Function | Description |
|--------|----------|-------------|
| **📂 Load Excel** | Import workbook | Opens a file dialog to load an Excel (.xlsx) file |
| **💾 Save Excel (Save As)** | Export to Excel | Saves the current configuration back to an Excel file |
| **🔧 Preview Nursecall** | Preview nurse call JSON | Generates and displays JSON for nurse call alarms in the preview pane |
| **🏥 Preview Clinical** | Preview clinical JSON | Generates and displays JSON for clinical alarms in the preview pane |
| **💊 Preview Orders** | Preview orders JSON | Generates and displays JSON for order-based flows in the preview pane |
| **📤 Export Nursecall** | Export nurse call JSON | Saves the nurse call JSON to a file |
| **📤 Export Clinicals** | Export clinical JSON | Saves the clinical JSON to a file |
| **📤 Export Orders** | Export orders JSON | Saves the orders JSON to a file |
| **🔄 Reset Defaults** | Reset default values | Resets reference names to default values |
| **🔄 Reset Paths** | Reset file paths | Clears previously selected file paths |

### Configuration Options

Below the main buttons, you'll find configuration fields and options:

- **🔀 Merge Identical Flows (Advanced)**: When checked, combines flows with identical Priority, Device, Ringtone, Recipients, and Timing to reduce duplication
- **🔗 Edge Reference Name**: Custom reference name for OutgoingWCTP interface (default: "OutgoingWCTP")
- **🔗 VCS Reference Name**: Custom reference name for VMP interface (default: "VMP")
- **Vocera Badges Alert Interface**:
  - **Via Edge**: Use OutgoingWCTP interface as default when Device A/B are blank
  - **Via VMP**: Use VMP interface as default when Device A/B are blank

### Tabs

#### 1. 📊 Units Tab
Displays the unit breakdown showing the relationship between facilities, units, and configuration groups.

**Columns:**
- Facility
- Unit Names
- Nurse Group
- Clinical Group
- Orders Group
- No Caregiver Group
- Comments

**Features:**
- Filter by Config Group dropdown
- Editable cells (double-click to edit)

**Usage:**
- Review facility mappings
- Verify configuration group assignments
- Edit unit properties by double-clicking cells
- Filter displayed units using the Config Group dropdown

#### 2. 🔔 Nurse Calls Tab
Shows all nurse call alarm configurations.

**Key Columns:**
- Configuration Group
- Facility
- Unit
- Alarm Name
- Priority (Normal, High, Urgent)
- Device A / Device B
- Ringtone
- Response Options
- Break Through DND
- EMDAN Compliant
- Enunciation settings
- Time to recipients
- Recipient columns

**Features:**
- Filter by Config Group dropdown
- Editable table
- Scroll horizontally for all columns

**Usage:**
- Configure nurse call alarms
- Set priority levels
- Assign recipients
- Define response options
- Mark EMDAN compliance

#### 3. 🏥 Clinicals Tab
Shows all clinical/patient monitoring alarm configurations.

**Key Columns:**
- Similar to Nurse Calls tab, plus:
- Fail Safe Recipients
- Additional enunciation settings

**Features:**
- Filter by Config Group dropdown
- Editable table
- Designed for patient monitoring alarms

**Usage:**
- Configure patient monitoring alarms
- Set up fail-safe escalation
- Configure genie enunciation

#### 4. 💊 Orders Tab
Shows order-based workflow configurations (if applicable).

**Features:**
- Filter by Config Group dropdown
- Configuration for order-related alerts and workflows

### Editing Data

1. **Edit a cell:**
   - Double-click any editable cell
   - Type the new value
   - Press Enter to confirm or Esc to cancel

2. **Select rows:**
   - Click a row to select it (highlighted in orange)
   - Use Ctrl+Click (Cmd+Click on Mac) for multiple selections

3. **Navigate:**
   - Use Tab to move between cells
   - Use arrow keys to navigate
   - Scroll with mouse wheel or scrollbar

### Generating JSON

1. **For Nurse Calls:**
   - Click **🔧 Preview Nursecall** to generate and view the JSON
   - Review the JSON in the preview pane at the bottom
   - Click **📤 Export Nursecall** to save the JSON to a file

2. **For Clinicals:**
   - Click **🏥 Preview Clinical** to generate and view the JSON
   - Review the JSON in the preview pane at the bottom
   - Click **📤 Export Clinicals** to save the JSON to a file

3. **For Orders:**
   - Click **💊 Preview Orders** to generate and view the JSON
   - Review the JSON in the preview pane at the bottom
   - Click **📤 Export Orders** to save the JSON to a file

The generated JSON is immediately visible in the preview pane at the bottom of the window.

### Saving Your Work

- **Save to Excel**: Click **💾 Save Excel (Save As)** to export all data back to an Excel file
- **Save JSON**: Click **📤 Export Nursecall**, **📤 Export Clinicals**, or **📤 Export Orders** after previewing to save the respective JSON output

---

## Using the Command-Line Interface (CLI)

The CLI is ideal for automated workflows, batch processing, and integration with CI/CD pipelines.

### Accessing CLI Commands

Use the `JobRunner` class to execute CLI commands:

```bash
java -cp engage-rules-generator-1.1.0.jar \
    com.example.exceljson.jobs.JobRunner <command> [arguments]
```

### Available Commands

#### 1. export-json
Generates Engage JSON from an Excel workbook.

**Syntax:**
```bash
java -cp engage-rules-generator-1.1.0.jar \
    com.example.exceljson.jobs.JobRunner export-json <input.xlsx> <output.json>
```

**Arguments:**
- `<input.xlsx>`: Path to the input Excel workbook
- `<output.json>`: Path for the generated JSON output

**Example:**
```bash
java -cp engage-rules-generator-1.1.0.jar \
    com.example.exceljson.jobs.JobRunner export-json \
    /path/to/config.xlsx \
    /path/to/output.json
```

**Output:**
The command will parse the Excel file and generate a JSON file containing all configuration rules.

#### 2. fail
Runs a diagnostic smoke check to verify the application environment.

**Syntax:**
```bash
java -cp engage-rules-generator-1.1.0.jar \
    com.example.exceljson.jobs.JobRunner fail [--expect-failure]
```

**Options:**
- `--expect-failure`: Forces a non-zero exit code (useful for testing error handling)

**Example:**
```bash
# Successful smoke test (exits with 0)
java -cp engage-rules-generator-1.1.0.jar \
    com.example.exceljson.jobs.JobRunner fail

# Forced failure (exits with non-zero)
java -cp engage-rules-generator-1.1.0.jar \
    com.example.exceljson.jobs.JobRunner fail --expect-failure
```

**Output:**
```
Fail job completed successfully. Use --expect-failure to force a failure exit code.
```

### Getting Help

```bash
java -cp engage-rules-generator-1.1.0.jar \
    com.example.exceljson.jobs.JobRunner --help
```

**Output:**
```
Usage: JobRunner <job> [job-args]
Available jobs:
  export-json
    Generate Engage JSON from an Excel workbook.
  fail
    Runs a diagnostics smoke check; add --expect-failure to force a non-zero exit.
```

### Exit Codes

- **0**: Success
- **1**: Error (invalid arguments, file not found, parsing error, etc.)

### Batch Processing Example

```bash
#!/bin/bash
# Process multiple configuration files

for config in /configs/*.xlsx; do
    output="${config%.xlsx}.json"
    echo "Processing $config..."
    
    java -cp engage-rules-generator-1.1.0.jar \
        com.example.exceljson.jobs.JobRunner export-json \
        "$config" "$output"
    
    if [ $? -eq 0 ]; then
        echo "✓ Successfully generated $output"
    else
        echo "✗ Failed to process $config"
        exit 1
    fi
done

echo "All files processed successfully!"
```

---

## Excel Workbook Requirements

### Required Sheets

Your Excel workbook must contain the following three sheets:

1. **Unit Breakdown** - Facility and unit mappings
2. **Nurse call** - Nurse call alarm configurations
3. **Patient Monitoring** - Clinical/patient monitoring configurations

### Sheet 1: Unit Breakdown

**Required Columns:**
- **Column A**: Facility
- **Column C**: Common Unit Name
- **Column D**: Configuration Group

**Example:**
```
| Facility   | (B) | Common Unit Name | Configuration Group | (E) | (F) |
|------------|-----|------------------|---------------------|-----|-----|
| Facility A |     | Unit A           | Group 1             |     |     |
| Facility B |     | Unit B           | Group 2             |     |     |
```

### Sheet 2: Nurse call

**Required Columns:**
- **Column A**: Configuration Group
- **Column E**: Alarm Name
- **Column F**: Priority
- **Column H**: Ringtone
- **Column AG**: Response Options

**Optional Columns:**
- **Device - A**: First device designation (e.g., "iPhone-Edge", "Vocera VCS")
- **Device - B**: Second device designation (for multi-device scenarios)
- **Break Through DND**: Break-through behavior ("voceraAndDevice", "device", "none")
- **EMDAN Compliant? (Y/N)**: EMDAN compliance flag ("Y", "Yes", "N", "No")
- **Time to [X] Recipient**: Timing columns
- **[X] Recipient**: Recipient assignment columns

**Example:**
```
| Config Group | ... | Alarm Name  | Priority | Device A    | Device B | Ringtone  | Response Options | EMDAN | Break Through DND |
|--------------|-----|-------------|----------|-------------|----------|-----------|------------------|-------|-------------------|
| Group 1      | ... | Call Button | Normal   | iPhone-Edge |          | Ringtone1 | None             | N     | none              |
| Group 1      | ... | Bed Exit    | Urgent   | iPhone-Edge | VCS      | Ringtone2 | Accept,Reject    | Y     | voceraAndDevice   |
```

### Sheet 3: Patient Monitoring

**Structure:** Similar to Nurse call sheet

**Additional Columns:**
- **Column AI**: Fail safe recipients

**Example:**
```
| Config Group | ... | Alarm Name    | Priority | Device A   | Device B | Ringtone  | Fail Safe Recipients |
|--------------|-----|---------------|----------|------------|----------|-----------|----------------------|
| PM Group     | ... | SpO2 Low      | Urgent   | VCS        |          | Ringtone3 | Nurse Station        |
| PM Group     | ... | Heart Rate Hi | High     | iPhone-Edge| VCS      | Ringtone4 | ICU Team             |
```

### Column Name Variations

The parser accepts multiple variations of column names for flexibility:

- **EMDAN**: "EMDAN Compliant? (Y/N)", "EMDAN Compliant", "EMDAN"
- **Device A**: "Device - A", "Device A"
- **Device B**: "Device - B", "Device B"
- **Break Through DND**: "Break Through DND", "BreakThrough"
- **Response Options**: "Response Options", "ResponseOptions"

### Important Notes

1. **Header Row**: First row must contain column headers
2. **Case Insensitive**: Column names are matched case-insensitively
3. **Position Flexible**: Parser uses header names, not column letters (except for specific legacy columns)
4. **Empty Cells**: Empty/blank cells are acceptable and won't cause errors
5. **File Format**: Must be .xlsx (Excel 2007+) format, not .xls

---

## Features and Functionality

### 1. EMDAN Compliance Reclassification

**Purpose:** Automatically reclassify nurse call alarms as clinical alarms based on EMDAN compliance.

**How It Works:**
- In the "Nurse call" sheet, add column "EMDAN Compliant? (Y/N)"
- Set value to "Y" or "Yes" for EMDAN-compliant alarms
- These alarms will automatically be moved to the Clinicals list during import
- Alarms with "N", "No", or blank values remain in Nurse Calls

**Usage:**
```
Nurse Call Sheet:
| Alarm Name | EMDAN Compliant? (Y/N) | → Result
|------------|------------------------|----------
| Call Button| N                      | → Stays in Nurse Calls
| Bed Exit   | Y                      | → Moved to Clinicals
| IV Pump    | Yes                    | → Moved to Clinicals
| Code Blue  | (blank)                | → Stays in Nurse Calls
```

**Benefits:**
- Ensures EMDAN-compliant alarms are properly categorized
- Automatic reclassification saves manual effort
- Maintains compliance with healthcare regulations

### 2. Multi-Device Support (Device A & Device B)

**Purpose:** Support configurations that route to multiple device types simultaneously.

**How It Works:**
- Configure "Device - A" column with first device (e.g., "iPhone-Edge")
- Configure "Device - B" column with second device (e.g., "VCS")
- If both contain "Edge" or "VCS" keywords, both interfaces are generated

**Generated Interfaces:**
- **Edge** devices → `OutgoingWCTP` interface
- **VCS** devices → `VMP` interface
- **Both** → Both interfaces in the JSON

**Example:**
```
Excel:
| Device A      | Device B | → Generated Interfaces
|---------------|----------|----------------------
| iPhone-Edge   | (blank)  | → OutgoingWCTP only
| (blank)       | VCS      | → VMP only
| iPhone-Edge   | VCS      | → OutgoingWCTP AND VMP
```

**Custom Reference Names:**
You can customize interface reference names in the GUI:
- Edge Reference Name: (default: "OutgoingWCTP")
- VCS Reference Name: (default: "VMP")

### 3. Break Through DND Control

**Purpose:** Explicitly control whether alarms break through Do Not Disturb mode.

**How It Works:**
- Add "Break Through DND" column to your Excel sheet
- Set value to:
  - `voceraAndDevice`: Breaks through on both Vocera and other devices
  - `device`: Breaks through on devices only
  - `none`: Does not break through DND
  - (blank): Uses priority-based default (Urgent → voceraAndDevice, Normal/High → none)

**Example:**
```
| Priority | Break Through DND | → Result
|----------|-------------------|----------
| Urgent   | voceraAndDevice   | → Breaks through on all devices
| Normal   | device            | → Breaks through on devices only
| High     | none              | → No break through
| Urgent   | (blank)           | → voceraAndDevice (priority default)
| Normal   | (blank)           | → none (priority default)
```

### 4. Response Options

**Purpose:** Define how recipients can respond to alarms.

**Supported Values:**
- `Accept`: Recipient can accept the alarm
- `Reject`: Recipient can reject the alarm
- `Accept,Reject`: Both options available
- `None`: No response options

**Example:**
```
| Alarm Name      | Response Options | → JSON
|-----------------|------------------|-------
| Critical Alert  | Accept,Reject    | → ["Accept", "Reject"]
| Info Message    | None             | → []
| Code Blue       | Accept           | → ["Accept"]
```

### 5. Enunciation Settings

**Purpose:** Control how alarms are announced audibly.

**Columns:**
- **Enunciate Alarm Name**: Whether to speak the alarm name
- **Enunciate Unit Name**: Whether to speak the unit name
- **Genie Enunciation**: Combined enunciation setting

**Values:**
- `Yes` / `Y` → Enabled
- `No` / `N` → Disabled
- (blank) → Default behavior

### 6. Priority Levels

**Available Priorities:**
- **Normal**: Standard priority alarms
- **High**: Elevated priority
- **Urgent**: Critical/highest priority alarms

**Impact:**
- Affects break-through behavior (when not explicitly set)
- Affects JSON priority field
- May affect routing and notification behavior

### 7. Recipient Configuration

**Time-based Escalation:**
Configure recipients with escalation timing:
- Time to 1st Recipient
- Time to 2nd Recipient
- Time to 3rd Recipient
- (etc.)

**Recipient Assignment:**
Assign specific recipients or roles:
- 1st Recipient
- 2nd Recipient
- 3rd Recipient
- (etc.)

**Fail Safe Recipients:**
For clinical alarms, configure fail-safe recipients when primary recipients don't respond.

### 8. Merge Identical Flows

**Purpose:** Reduce JSON file size by combining duplicate alarm configurations.

**How It Works:**
1. Enable the checkbox: **🔀 Merge Identical Flows**
2. Generate JSON
3. Identical alarm configurations will be combined into single flows

**Example:**
```
Before Merge (3 separate flows):
Flow 1: Alarm "Call Button", Priority Normal, Recipient "Nurse A"
Flow 2: Alarm "Assistance", Priority Normal, Recipient "Nurse A"
Flow 3: Alarm "Help Needed", Priority Normal, Recipient "Nurse A"

After Merge (1 combined flow):
Flow 1: Alarms ["Call Button", "Assistance", "Help Needed"], 
        Priority Normal, Recipient "Nurse A"
```

---

## Troubleshooting

### Common Issues and Solutions

#### Issue: "Could not find or load main class"

**Symptoms:**
```
Error: Could not find or load main class com.example.exceljson.Main
```

**Solutions:**
1. Verify Java 17+ is installed: `java -version`
2. Check that you're using the correct JAR file path
3. Ensure the JAR file is not corrupted (re-download if necessary)
4. Use the full path to the JAR file

#### Issue: Application Window Doesn't Open

**Symptoms:** Command runs but no GUI appears

**Solutions:**
1. Verify JavaFX is included in your Java installation
   - Use Liberica JDK Full or another JDK with JavaFX
2. Check for error messages in the console
3. Try launching with verbose output:
   ```bash
   java -jar engage-rules-generator-1.1.0.jar --verbose
   ```

#### Issue: "File Not Found" Error

**Symptoms:**
```
java.io.FileNotFoundException: sample.xlsx
```

**Solutions:**
1. Verify the file path is correct
2. Use absolute paths instead of relative paths
3. Check file permissions (ensure the file is readable)
4. Ensure the file extension is .xlsx (not .xls)

#### Issue: Excel Parsing Errors

**Symptoms:** Application reports parsing errors or missing data

**Solutions:**
1. Verify all required sheets exist:
   - Unit Breakdown
   - Nurse call
   - Patient Monitoring
2. Check that required columns have proper headers
3. Ensure header row is the first row in each sheet
4. Remove any merged cells in the header row
5. Check for special characters or formatting issues

#### Issue: Generated JSON is Invalid

**Symptoms:** JSON validation tools report errors

**Solutions:**
1. Verify source Excel data is complete
2. Check for unusual characters in Excel cells
3. Ensure priority values are valid (Normal, High, Urgent)
4. Verify boolean fields use proper values (Y/N, Yes/No)

#### Issue: Out of Memory Error

**Symptoms:**
```
java.lang.OutOfMemoryError: Java heap space
```

**Solutions:**
1. Increase Java heap size:
   ```bash
   java -Xmx1024m -jar engage-rules-generator-1.1.0.jar
   ```
2. Split large Excel files into smaller workbooks
3. Close other applications to free memory

#### Issue: Exported Excel File is Empty

**Symptoms:** Saved Excel file has no data

**Solutions:**
1. Ensure data is loaded before saving
2. Check that you've clicked "Load Excel" first
3. Verify the data appears in the GUI tabs
4. Try a different output file path with write permissions

### Debug Mode

To enable detailed logging for troubleshooting:

```bash
java -Dlog4j.configurationFile=log4j2.xml -jar engage-rules-generator-1.1.0.jar
```

Check the console output or log files for detailed error messages.

### Getting Support

If you encounter issues not covered here:

1. **Check the GitHub Issues**: Search for similar problems at the repository issues page
2. **Review Documentation**: Check `docs/` folder for feature-specific documentation
3. **Create an Issue**: If you find a bug, create a detailed issue report including:
   - Application version
   - Java version (`java -version` output)
   - Operating system
   - Steps to reproduce
   - Error messages
   - Sample input file (if possible)

---

## FAQ

### General Questions

**Q: What file formats are supported for input?**
A: The application supports Excel 2007+ (.xlsx) format only. Legacy .xls files are not supported.

**Q: Can I run this application without a graphical interface?**
A: Yes! Use the CLI mode with the `JobRunner` class for headless/automated operation.

**Q: Is my data saved automatically?**
A: No. You must explicitly save using "Save Excel" or "Export JSON" buttons.

**Q: Can I undo changes?**
A: There is no built-in undo. It's recommended to keep backup copies of your Excel files before making changes.

### Excel and Data Questions

**Q: Do I need all three sheets in my Excel file?**
A: Yes. The application requires "Unit Breakdown", "Nurse call", and "Patient Monitoring" sheets.

**Q: Can I add custom columns to the Excel sheets?**
A: Yes. The parser only reads recognized columns and ignores unknown ones.

**Q: What happens if I leave required fields blank?**
A: The application uses default values or falls back to automatic behavior. However, critical fields like "Alarm Name" should not be blank.

**Q: Can I use formulas in Excel cells?**
A: The application reads cell values, not formulas. Ensure formulas are evaluated before loading the file.

**Q: How do I handle special characters in alarm names?**
A: Special characters are generally supported, but avoid control characters and ensure proper encoding.

### EMDAN Questions

**Q: What is EMDAN compliance?**
A: EMDAN (Emergency Department Nurse) compliance indicates alarms that meet specific clinical alert standards. EMDAN-compliant alarms are automatically categorized as clinical alarms.

**Q: What happens if I don't have the EMDAN column?**
A: It's optional. If absent, no automatic reclassification occurs.

**Q: Can I manually override EMDAN reclassification?**
A: Yes. After import, you can manually move alarms between Nurse Calls and Clinicals tabs in the GUI.

### Device Questions

**Q: What's the difference between Device A and Device B?**
A: Device A and Device B allow routing the same alarm to two different device types (e.g., Edge phones and VCS devices).

**Q: Do I need to fill both Device A and Device B?**
A: No. You can use just one or both depending on your routing requirements.

**Q: Can I customize the interface reference names?**
A: Yes. Use the "Edge Reference Name" and "VCS Reference Name" fields in the GUI.

### JSON Output Questions

**Q: What format is the generated JSON?**
A: The application generates Vocera Engage-compatible JSON rule files.

**Q: Can I edit the JSON directly?**
A: Yes, after export. However, it's better to make changes in Excel and regenerate to maintain consistency.

**Q: Why is my JSON file so large?**
A: Enable "Merge Identical Flows" to combine duplicate configurations and reduce file size.

**Q: Can I generate separate JSON files for different facilities?**
A: Not directly. You'll need to create separate Excel workbooks for each facility or manually split the JSON.

### Performance Questions

**Q: How large of an Excel file can I process?**
A: The application can handle thousands of rows. Performance depends on available memory. For very large files (10,000+ rows), increase Java heap size.

**Q: Why is loading slow?**
A: Large files with many columns and complex formulas take longer. Consider simplifying the Excel structure if possible.

### Automation Questions

**Q: Can I automate JSON generation?**
A: Yes. Use the CLI mode with the `export-json` command in scripts or CI/CD pipelines.

**Q: Can I integrate this with other systems?**
A: Yes. The CLI interface allows integration with any system that can execute Java commands.

**Q: Is there an API?**
A: No formal API. Use the CLI or integrate with the Java classes directly if building custom solutions.

---

## Quick Reference

### Keyboard Shortcuts (GUI)
- **Tab**: Move to next cell
- **Shift+Tab**: Move to previous cell
- **Enter**: Confirm cell edit
- **Esc**: Cancel cell edit
- **Ctrl+C** / **Cmd+C**: Copy (in some contexts)
- **Double-Click**: Edit cell

### File Locations
- **JAR File**: `engage-rules-generator-1.1.0.jar`
- **Generated JSON**: User-specified location
- **Sample Files**: `sample.xlsx`, `sample.json` (git-ignored, local only)

### Important Column Names
- Configuration Group
- Alarm Name
- Priority
- Device - A / Device - B
- Ringtone
- Response Options
- Break Through DND
- EMDAN Compliant? (Y/N)
- Fail safe recipients

### Valid Priority Values
- Normal
- High
- Urgent

### Valid Response Options
- None
- Accept
- Reject
- Accept,Reject

### Valid Break Through DND Values
- none
- device
- voceraAndDevice

### Valid EMDAN Values
- Y / Yes → EMDAN compliant (moves to Clinicals)
- N / No → Not compliant (stays in Nurse Calls)
- (blank) → Not compliant

---

## Document Version

- **Document Version**: 1.0
- **Application Version**: 1.1.0
- **Last Updated**: November 2025
- **Compatibility**: Java 17+, Excel 2007+ (.xlsx)

---

## Additional Resources

### Documentation Files
- `docs/sample-workbook.md` - Guide for creating test workbooks
- `docs/emdan-feature.md` - EMDAN compliance feature details
- `docs/break-through-dnd-feature.md` - Break Through DND feature details
- `EXAMPLE_OUTPUT.md` - Example JSON output formats
- `GUI_CHANGES.md` - GUI feature descriptions

### External Links
- [GitHub Repository](https://github.com/Bussyny-boop/NDW-To-Engage-Rules)
- [Liberica JDK Downloads](https://bell-sw.com/pages/downloads/)
- [Apache POI Documentation](https://poi.apache.org/)

---

**End of User Guide**

For technical support or to report issues, please visit the GitHub repository's Issues page.
