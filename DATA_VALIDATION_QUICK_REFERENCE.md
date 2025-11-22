# Data Validation Quick Reference

## Three Validation Types

### 1. Voice Group Validation
**Button**: Load Voice Group  
**File Column**: "Group Name" or Column A  
**Keywords**: `VGroup:` or `Group:`  
**Example**: `VGroup: Code Blue`  
**Validates**: Device A, Device B, Recipients columns

### 2. Assignment Roles Validation
**Button**: Load AssignmentRoles  
**File Column**: "Name" (required)  
**Keywords**: `VAssign:`  
**Example**: `VAssign: Room 101`  
**Validates**: Device A, Device B, Recipients columns

### 3. Bed List Validation
**Button**: Load Bed List  
**File Column**: "Department" or "Unit" (required)  
**Keywords**: None (validates entire cell)  
**Example**: `ICU` or `Medical/Surgical`  
**Validates**: Unit Names column in Units tab only

## Validation Colors
- ✅ **Black Text** = Valid entry
- ❌ **Red Text** = Invalid entry
- ⚪ **Normal Text** = No validation loaded or no keywords present

## Sample CSV Files

### Voice Groups (voice_groups.csv)
```csv
Group Name
Code Blue
Rapid Response
OB Nurse
```

### Assignment Roles (assignment_roles.csv)
```csv
Name
Room 101
Room 102
ICU Pod A
```

### Bed List (bed_list.csv)
```csv
Department
ICU
ED
Medical/Surgical
```

## Auto-Complete
- Type at least 2 characters
- Shows top 5 matches
- Press Enter to select
- Press Escape to cancel

## Row Expansion
- All validated cells now expand to show full content
- Multi-line entries are fully visible
- No more clipped text

## Keyboard Shortcuts
- **Enter**: Commit edit
- **Shift+Enter**: New line in multi-line cell
- **Tab**: Move to next cell
- **Escape**: Cancel edit

## Tips
1. Load validation data before editing cells
2. Use keywords exactly as shown (case-insensitive)
3. Each line in multi-line cells is validated separately
4. Clear button removes validation but keeps your data
5. Validation persists until cleared or app closed

## Common Issues

**Q**: Why is my text red?  
**A**: The value doesn't match any entry in the loaded validation file.

**Q**: Auto-complete not working?  
**A**: Ensure you've loaded the validation file and typed at least 2 characters after the keyword.

**Q**: Bed List not validating?  
**A**: Bed List only validates the "Unit Names" column in the Units tab.

**Q**: Can I use both VGroup and VAssign in the same cell?  
**A**: Yes, but VAssign takes priority if both are present.
