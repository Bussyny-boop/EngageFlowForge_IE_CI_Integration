# Custom Unit Quick Reference

## Syntax Examples

| Excel Input | Parsed Roles | Notes |
|-------------|--------------|-------|
| `Custom Unit Nurse, CNA` | `["Nurse", "CNA"]` | Basic usage |
| `Custom UNIT all Nurse, CNA` | `["Nurse", "CNA"]` | "All" ignored |
| `Custom Unit All Nurse, All CNA, Charge Nurse` | `["Nurse", "CNA", "Charge Nurse"]` | "All" prefix removed |
| `CUSTOM-UNIT nurse` | `["nurse"]` | Case insensitive, hyphen ignored |
| `Custom_Unit PCT, RN` | `["PCT", "RN"]` | Underscore ignored |

## Priority Differences

### Normal/High Priority
- 3 filters: role name, state, device status
- No presence filtering

### Urgent Priority
- 4 filters: role name, state, device status, **presence**
- Presence filter: `Chat, Available`

## Condition Name Format

The condition name follows this pattern:
```
Custom All Assigned <role1> and <role2> and <role3>
```

Examples:
- `Custom All Assigned Nurse and CNA`
- `Custom All Assigned Nurse and CNA and Charge Nurse`

## Static Values

These values are hardcoded for Custom Unit recipients:

| Field | Value |
|-------|-------|
| `recipientType` | `"custom"` |
| `attributePath` | `"bed.room.unit.rooms.beds.locs.assignments.usr.devices.lines.number"` |
| `destinationType` | `"Normal"` |
| State filter value | `"Active"` |
| Device status filter value | `"Registered, Disconnected"` |
| Presence filter value | `"Chat, Available"` (urgent only) |

## Dynamic Values

These values come from Excel or configuration:

| Field | Source |
|-------|--------|
| `delayTime` | Time to Recipient column |
| `interfaceReferenceName` | Device A/B columns + interface settings |
| `presenceConfig` | Break Through DND column |
| `order` | Recipient order (1st, 2nd, 3rd, etc.) |
| `destinationOrder` | Matches destination `order` |

## Common Patterns

### Pattern 1: Single Custom Unit Recipient
```
1st Recipient: Custom Unit Nurse, CNA
```
**Result**: 1 destination with custom conditions

### Pattern 2: Multiple Recipients with Custom Unit First
```
1st Recipient: Custom Unit Nurse, CNA
2nd Recipient: Room Charge Nurse
```
**Result**: 2 destinations (1 custom, 1 functional role)

### Pattern 3: Escalation with Custom Unit
```
1st Recipient: Custom Unit Nurse
Time to 2nd: 5
2nd Recipient: Custom Unit Charge Nurse
Time to 3rd: 10
3rd Recipient: Supervisor Group
```
**Result**: 3 destinations (2 custom with different conditions, 1 group)

## Testing Checklist

- [ ] Custom Unit keyword detected (case insensitive)
- [ ] "All" keyword filtered out
- [ ] Roles parsed correctly
- [ ] Condition name generated correctly
- [ ] Normal priority: 3 filters
- [ ] Urgent priority: 4 filters (includes presence)
- [ ] Destination has `recipientType: "custom"`
- [ ] Destination has correct `attributePath`
- [ ] Delay time from Excel used
- [ ] Interface reference determined correctly
- [ ] Works with mixed recipient types

## Error Prevention

### ❌ Don't Do This
```
1st Recipient: Custom Unit All
```
**Problem**: No roles specified after "All"

### ✅ Do This Instead
```
1st Recipient: Custom Unit Nurse, CNA
```

### ❌ Don't Do This
```
1st Recipient: CustomUnit Nurse, CNA
```
**Problem**: No space between "Custom" and "Unit" - but this actually works! The parser handles it.

### ❌ Don't Do This
```
1st Recipient: Nurse, CNA
```
**Problem**: Missing "Custom Unit" keyword - will be treated as regular recipients split by comma

### ✅ Do This Instead
```
1st Recipient: Custom Unit Nurse, CNA
```
