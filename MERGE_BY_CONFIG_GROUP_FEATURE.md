# Merge by Config Group Feature

## Overview
This feature adds three mutually exclusive merge logic options to control how delivery flows are combined when generating JSON output. Only one option can be selected at a time.

## Feature Description

### Three Merge Modes

#### 1. Standard (No Merge) - DEFAULT
- **Behavior**: Each alarm/alert gets its own separate delivery flow
- **Use Case**: Maximum granularity, when you need complete control over each alarm
- **Example**: 4 alarms → 4 separate flows

#### 2. Merge All (Ignore Config Group)
- **Behavior**: Combines flows with identical delivery parameters across ALL Config Groups
- **Merge Criteria**: Priority, Device, Ringtone, Recipients, Timing, Units, No Caregiver Group (Config Group is IGNORED)
- **Use Case**: When you want to maximize flow consolidation regardless of Config Group
- **Example**: 
  - ICU_Group: Code Blue, Rapid Response (identical params)
  - ER_Group: Trauma Alert, Stroke Alert (identical params to ICU)
  - Result: **1 merged flow** containing all 4 alarms

#### 3. Merge Within Config Group - NEW
- **Behavior**: Combines flows with identical delivery parameters ONLY within the same Config Group
- **Merge Criteria**: Priority, Device, Ringtone, Recipients, Timing, No Caregiver Group, AND Config Group
  - **Note**: Units are NOT part of the merge criteria - flows will merge regardless of unit differences
  - When flows merge, their units are combined into a single list
- **Use Case**: When you want to keep Config Groups separate but merge within each group
- **Example**:
  - ICU_Group: Code Blue, Rapid Response (identical params)
  - ER_Group: Trauma Alert, Stroke Alert (identical params to ICU but different group)
  - Result: **2 separate flows** (one for ICU_Group, one for ER_Group)
- **Unit Combination Example**:
  - Toilet Finished: Units 1-4, Normal priority, PCT/NA → Nurse
  - Nurse: Units 1-6, Normal priority, PCT/NA → Nurse
  - Result: **1 merged flow** with Units 1-6 combined

## UI Location
**Settings Panel → Merge Logic Section**

The three checkboxes are displayed vertically with clear labels and enhanced tooltips:
- ☑️ Standard (No Merge) - *Selected by default*
- ☐ Merge All (Ignore Config Group)
- ☐ Merge Within Config Group

## Tooltips
Each checkbox has a detailed tooltip that appears on hover:

**Standard (No Merge)**
> Standard Mode: Each alarm/alert gets its own separate delivery flow. This is the default behavior and creates the most granular output.

**Merge All (Ignore Config Group)**
> Merge All: Combines flows with identical Priority, Device, Ringtone, Recipients, and Timing across ALL Config Groups. Multiple alarms will share the same delivery flow if their delivery parameters match, even if they're in different Config Groups.

**Merge Within Config Group**
> Merge Within Config Group: Combines flows with identical Priority, Device, Ringtone, Recipients, and Timing ONLY within the same Config Group. Flows in different Config Groups will remain separate, even if their delivery parameters match.

## Technical Implementation

### Code Changes

1. **ExcelParserV5.java**
   - Added `MergeMode` enum with three values: `NONE`, `MERGE_ALL`, `MERGE_BY_CONFIG_GROUP`
   - Updated `buildJson()` to accept `MergeMode` parameter
   - Modified `buildMergeKey()` to conditionally include `configGroup` based on merge mode
   - Added new method overloads for backward compatibility

2. **AppController.java**
   - Added three checkboxes: `noMergeCheckbox`, `mergeFlowsCheckbox`, `mergeByConfigGroupCheckbox`
   - Implemented mutual exclusion logic (only one can be selected)
   - Added `getCurrentMergeMode()` helper method
   - Updated all JSON generation and export methods to use the new merge modes
   - Enhanced `updateJsonModeLabel()` to display current mode

3. **App.fxml**
   - Added "Merge Logic" section header with bold styling
   - Added three mutually exclusive checkboxes with detailed tooltips
   - Improved visual organization of settings

### Testing

**New Test Suite: MergeByConfigGroupTest**
- Tests merge behavior across different config groups
- Tests merge behavior within the same config group
- Tests that MERGE_ALL ignores config groups
- Tests that NONE keeps all flows separate
- Tests backward compatibility with boolean parameters

**All Tests Pass**: 364 tests including 5 new comprehensive tests

## Usage Examples

### Example 1: Hospital with Multiple Units
**Scenario**: ICU and ER both have "Code Blue" alarms with identical delivery parameters

- **Standard (No Merge)**: 2 separate flows (one for ICU Code Blue, one for ER Code Blue)
- **Merge All**: 1 merged flow containing both Code Blues
- **Merge Within Config Group**: Depends on whether ICU and ER use the same Config Group

### Example 2: Same Unit, Multiple Config Groups
**Scenario**: ICU unit has two config groups with identical delivery setup

- **Standard (No Merge)**: Separate flows for each alarm
- **Merge All**: All alarms merged into one flow
- **Merge Within Config Group**: Separate flows for each config group (e.g., ICU_Day vs ICU_Night)

## Benefits

1. **Flexibility**: Users can choose the level of flow consolidation that matches their workflow
2. **Clarity**: Clear labels and tooltips help users understand the impact of each option
3. **Safety**: Mutual exclusion prevents conflicting merge modes
4. **Default Behavior**: Standard (No Merge) is selected by default for predictable behavior
5. **Backward Compatible**: Existing boolean parameters still work for API users

## Status Bar Indication

The application status bar shows the current merge mode:
- "JSON: Standard" - No merge active
- "JSON: Merge All" - Merge All mode active
- "JSON: Merge Within Config Group" - Merge Within Config Group mode active

## Version
Implemented in version 2.0.0

## Author
GitHub Copilot Agent
