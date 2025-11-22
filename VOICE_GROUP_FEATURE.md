# Voice Group Validation Feature

## Overview
This feature allows users to load a list of valid Voice Groups from an Excel or CSV file and validates entries in the application against this list.

## Features

### 1. Load Voice Groups
- **Supported Formats**: Excel (.xlsx, .xls) and CSV (.csv).
- **Data Source**: Reads the first column (Column A) of the first sheet.
- **Action**: Click the "Load Voice Group" button in the Settings drawer.
- **Feedback**: Displays the number of loaded groups.

### 2. Validation Logic
- **Trigger**: Applies to cells containing "VGroup" or "Group" (case-insensitive).
- **Format**: Expects "VGroup: [Name]" or "Group: [Name]".
- **Visual Feedback**:
  - **Valid Group**: The group name is displayed in **Black**.
  - **Invalid Group**: The group name is displayed in **Red**.
  - The prefix ("VGroup:" or "Group:") is always displayed in Black.
- **Scope**: Applied to "Device A", "Device B", "1st Recipient", and other recipient columns.

### 3. Auto-Complete
- **Trigger**: Typing in a validated cell.
- **Behavior**: Shows a popup list of matching Voice Groups from the loaded list.
- **Selection**: Clicking a suggestion inserts "VGroup: [Name]" into the cell.

### 4. Dynamic Updates
- Validation is applied immediately when data is loaded or edited.
- Clearing the loaded groups (via "Clear Loaded Groups" button) resets validation (all groups appear valid/black).

## Usage
1. Open the **Settings** drawer (gear icon).
2. Scroll to the **Voice Group Validation** section.
3. Click **Load Voice Group** and select your file.
4. Navigate to **Nurse Calls**, **Clinicals**, or **Orders** tabs.
5. Edit columns like **Device A**, **Device B**, or **Recipients**.
6. Type "VGroup:" to see validation in action.

## Technical Details
- **Class**: `AppController.java`
- **Key Methods**:
  - `loadVoiceGroups()`: Loads data.
  - `createValidatedCellGraphic()`: Handles rendering.
  - `setupAutoComplete()`: Handles suggestions.
  - `setupValidatedColumn()`: Applies logic to TableColumns.
