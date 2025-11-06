# Custom Unit Recipient Feature

## Overview

The Custom Unit recipient feature enables specialized routing for Vocera Engage alarms by creating custom conditions and destinations based on role assignments. When a recipient field contains the "Custom Unit" keyword, the system generates conditional filters that route alerts to assigned staff members based on their role, assignment state, device status, and presence.

## Syntax

The Custom Unit feature recognizes the following patterns in recipient fields:

```
Custom Unit <role1>, <role2>, <role3>
Custom UNIT All <role1>, <role2>
Custom-Unit All <role1>, All <role2>, <role3>
```

### Key Rules

1. **Case Insensitive**: The keywords "Custom" and "Unit" are not case-sensitive
2. **Special Characters Ignored**: Spaces, hyphens, and underscores between "Custom" and "Unit" are ignored
3. **"All" Keyword Ignored**: The word "All" (case-insensitive) is filtered out and not treated as a role
4. **Comma-Separated Roles**: Multiple roles are separated by commas

## Examples

### Example 1: Basic Usage
```
Custom Unit Nurse, CNA
```
**Parsed Roles**: `["Nurse", "CNA"]`

### Example 2: With "All" Keyword
```
Custom UNIT all Nurse, CNA
```
**Parsed Roles**: `["Nurse", "CNA"]` (the word "All" is ignored)

### Example 3: "All" Prefix on Each Role
```
Custom Unit All Nurse, All CNA, Charge Nurse
```
**Parsed Roles**: `["Nurse", "CNA", "Charge Nurse"]` ("All" prefix is removed from each role)

### Example 4: Case Variations
```
CUSTOM-UNIT all nurse, cna
```
**Parsed Roles**: `["nurse", "cna"]` (role names preserve their original case)

## Generated JSON Structure

### Normal Priority

For normal or high priority alarms, the system generates:

#### Conditions
```json
{
  "destinationOrder": 0,
  "filters": [
    {
      "attributePath": "bed.room.unit.rooms.beds.locs.assignments.role.name",
      "operator": "in",
      "value": "Nurse, CNA"
    },
    {
      "attributePath": "bed.room.unit.rooms.beds.locs.assignments.state",
      "operator": "in",
      "value": "Active"
    },
    {
      "attributePath": "bed.room.unit.rooms.beds.locs.assignments.usr.devices.status",
      "operator": "in",
      "value": "Registered, Disconnected"
    }
  ],
  "name": "Custom All Assigned Nurse and CNA"
}
```

#### Destination
```json
{
  "attributePath": "bed.room.unit.rooms.beds.locs.assignments.usr.devices.lines.number",
  "delayTime": 0,
  "destinationType": "Normal",
  "functionalRoles": [],
  "groups": [],
  "interfaceReferenceName": "OutgoingWCTP",
  "order": 0,
  "presenceConfig": "none",
  "recipientType": "custom",
  "users": []
}
```

### Urgent Priority

For urgent priority alarms, an additional presence filter is added:

#### Conditions
```json
{
  "destinationOrder": 0,
  "filters": [
    {
      "attributePath": "bed.room.unit.rooms.beds.locs.assignments.role.name",
      "operator": "in",
      "value": "Nurse, CNA"
    },
    {
      "attributePath": "bed.room.unit.rooms.beds.locs.assignments.state",
      "operator": "in",
      "value": "Active"
    },
    {
      "attributePath": "bed.room.unit.rooms.beds.locs.assignments.usr.devices.status",
      "operator": "in",
      "value": "Registered, Disconnected"
    },
    {
      "attributePath": "bed.room.unit.rooms.beds.locs.assignments.usr.presence_show",
      "operator": "in",
      "value": "Chat, Available"
    }
  ],
  "name": "Custom All Assigned Nurse and CNA"
}
```

## Filter Explanations

### Role Name Filter
```json
{
  "attributePath": "bed.room.unit.rooms.beds.locs.assignments.role.name",
  "operator": "in",
  "value": "Nurse, CNA"
}
```
Filters for staff members assigned to roles matching the specified names (comma-separated).

### Assignment State Filter
```json
{
  "attributePath": "bed.room.unit.rooms.beds.locs.assignments.state",
  "operator": "in",
  "value": "Active"
}
```
Ensures only actively assigned staff members receive the alert.

### Device Status Filter
```json
{
  "attributePath": "bed.room.unit.rooms.beds.locs.assignments.usr.devices.status",
  "operator": "in",
  "value": "Registered, Disconnected"
}
```
Targets staff members with registered or disconnected devices.

### Presence Filter (Urgent Only)
```json
{
  "attributePath": "bed.room.unit.rooms.beds.locs.assignments.usr.presence_show",
  "operator": "in",
  "value": "Chat, Available"
}
```
For urgent priority only, filters for staff members who are currently available (Chat or Available status).

## Configuration Parameters

### Delay Time
The `delayTime` value is taken from the "Time to Recipient" column in the Excel sheet.

### Interface Reference
The `interfaceReferenceName` is determined by the Device A/B columns:
- If Device A or B contains "Edge": uses `OutgoingWCTP` (default: "OutgoingWCTP")
- If Device A or B contains "VCS": uses `VMP` (default: "VMP")
- Otherwise: uses the default interface configured in the GUI

### Presence Config
The `presenceConfig` value is determined by the "Break Through DND" column using existing logic.

## Using with Excel Sheets

### Example Excel Configuration

| Configuration Group | Alarm Name | Priority | Time to 1st Recipient | 1st Recipient |
|---------------------|------------|----------|----------------------|---------------|
| Nurse Group 1 | Code Blue | Urgent | 0 | Custom Unit Nurse, CNA |
| Nurse Group 1 | Patient Fall | Normal | 5 | Custom Unit All Nurse, Charge Nurse |

### Mixed Recipients

You can mix Custom Unit recipients with regular recipients:

| Time to 1st | 1st Recipient | Time to 2nd | 2nd Recipient |
|-------------|---------------|-------------|---------------|
| 0 | Custom Unit Nurse, CNA | 5 | Room Charge Nurse |

This creates:
- **First destination** (order 0): Custom Unit with conditions for Nurse and CNA
- **Second destination** (order 1): Regular functional role for Charge Nurse

## Implementation Details

### Code Location
The Custom Unit logic is implemented in:
- `ExcelParserV5.java`: Main parsing and JSON generation logic
- `ParsedRecipient` class: Holds parsed recipient information including custom unit roles
- `parseCustomUnitRecipient()`: Parses Custom Unit patterns
- `addCustomUnitDestination()`: Generates conditions and destinations

### Testing
Comprehensive tests are available in:
- `CustomUnitTest.java`: Unit tests for various Custom Unit patterns
- `CustomUnitManualVerificationTest.java`: End-to-end verification tests

Run tests with:
```bash
mvn test -Dtest=CustomUnitTest
mvn test -Dtest=CustomUnitManualVerificationTest
```

## Troubleshooting

### Custom Unit Not Recognized

**Problem**: Recipient field contains "Custom Unit" but regular recipient processing occurs.

**Solutions**:
- Ensure "Custom" and "Unit" are spelled correctly (case doesn't matter)
- Check that there's text after "Custom Unit"
- Verify the role names are comma-separated

### Incorrect Roles Parsed

**Problem**: Wrong roles appear in the generated JSON.

**Solutions**:
- Check for proper comma separation between roles
- Verify "All" keywords are placed correctly (they should be filtered out)
- Ensure role names don't contain special characters that might interfere with parsing

### Missing Presence Filter

**Problem**: Urgent priority alarm doesn't include presence filter.

**Solutions**:
- Verify the Priority column is set to "Urgent" (case-insensitive)
- Check that the priority mapping hasn't overridden "urgent" to another value

## Version History

- **v1.1.0**: Initial implementation of Custom Unit recipient feature
  - Support for case-insensitive keyword detection
  - "All" keyword filtering
  - Normal and urgent priority handling
  - Integration with existing recipient system
