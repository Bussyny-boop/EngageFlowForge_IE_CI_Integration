# Config Group Filter Fix

## Problem Statement

The config group filter was working correctly in terms of showing/hiding rows based on the selected config group, but it had an issue with the "In Scope" checkboxes:

- When a specific config group was selected (e.g., "Group A"), only rows matching that group would be checked (inScope=true), and the rest would be unchecked
- However, when switching back to "All" config groups, the checkboxes remained in their previous state instead of all being checked

## Expected Behavior

When "All" is selected in the config group filter dropdown:
- All rows should be visible (already working)
- All rows should have their "In Scope" checkbox checked (this was missing)

## Solution

Updated the filter logic in three methods in `AppController.java`:
1. `applyNurseFilter()`
2. `applyClinicalFilter()`
3. `applyOrdersFilter()`

Each method now includes logic to set `inScope = true` for all rows when "All" is selected:

```java
if (selected == null || selected.equals("All")) {
    // Check all rows when "All" is selected
    if (nurseCallsFullList != null) {
        for (ExcelParserV5.FlowRow flow : nurseCallsFullList) {
            flow.inScope = true;
        }
    }
    nurseCallsFilteredList.setPredicate(flow -> true); // Show all
} else {
    // Update inScope based on filter: uncheck filtered-out rows, keep checked for visible rows
    if (nurseCallsFullList != null) {
        for (ExcelParserV5.FlowRow flow : nurseCallsFullList) {
            flow.inScope = selected.equals(flow.configGroup);
        }
    }
    nurseCallsFilteredList.setPredicate(flow -> selected.equals(flow.configGroup));
}

if (tableNurseCalls != null) tableNurseCalls.refresh();
```

## How to Test

### Manual Testing (GUI)

1. Build and run the application:
   ```bash
   mvn clean package
   java -jar target/engage-rules-generator-2.0.0.jar
   ```

2. Load an Excel file with multiple config groups

3. Select a specific config group from the dropdown (e.g., "Group A")
   - ✅ Only rows with "Group A" should be visible
   - ✅ Only visible rows should have "In Scope" checked

4. Switch the filter back to "All"
   - ✅ All rows should be visible
   - ✅ **All rows should now have "In Scope" checked** (this is the fix)

### Before and After Comparison

**BEFORE FIX:**
```
Filter: [Group A ▼]  (showing only Group A rows)
┌──────────┬──────────────┬──────────────┐
│ In Scope │ Config Group │ Alarm Name   │
├──────────┼──────────────┼──────────────┤
│    ☑     │   Group A    │ Alarm 1      │
│    ☑     │   Group A    │ Alarm 2      │
└──────────┴──────────────┴──────────────┘

Switch to: [All ▼]
┌──────────┬──────────────┬──────────────┐
│ In Scope │ Config Group │ Alarm Name   │
├──────────┼──────────────┼──────────────┤
│    ☑     │   Group A    │ Alarm 1      │ <- Still checked
│    ☑     │   Group A    │ Alarm 2      │ <- Still checked
│    ☐     │   Group B    │ Alarm 3      │ <- NOT checked (BUG!)
│    ☐     │   Group C    │ Alarm 4      │ <- NOT checked (BUG!)
└──────────┴──────────────┴──────────────┘
```

**AFTER FIX:**
```
Filter: [Group A ▼]  (showing only Group A rows)
┌──────────┬──────────────┬──────────────┐
│ In Scope │ Config Group │ Alarm Name   │
├──────────┼──────────────┼──────────────┤
│    ☑     │   Group A    │ Alarm 1      │
│    ☑     │   Group A    │ Alarm 2      │
└──────────┴──────────────┴──────────────┘

Switch to: [All ▼]
┌──────────┬──────────────┬──────────────┐
│ In Scope │ Config Group │ Alarm Name   │
├──────────┼──────────────┼──────────────┤
│    ☑     │   Group A    │ Alarm 1      │ <- Checked ✓
│    ☑     │   Group A    │ Alarm 2      │ <- Checked ✓
│    ☑     │   Group B    │ Alarm 3      │ <- Now checked ✓
│    ☑     │   Group C    │ Alarm 4      │ <- Now checked ✓
└──────────┴──────────────┴──────────────┘
```

## Automated Tests

A new test was added to `ConfigGroupFilterTest.java`:

```java
@Test
void testInScopeAllCheckedWhenFilterSetToAll()
```

This test:
1. Creates test data with multiple config groups
2. Filters by a specific group and verifies only those rows are checked
3. Switches to "All" and verifies ALL rows are now checked
4. Verifies JSON generation includes all flows when "All" is selected

Test results: **201/201 tests passing** ✅

## Files Modified

1. `src/main/java/com/example/exceljson/AppController.java`
   - Modified: `applyNurseFilter()`
   - Modified: `applyClinicalFilter()`
   - Modified: `applyOrdersFilter()`

2. `src/test/java/com/example/exceljson/ConfigGroupFilterTest.java`
   - Added: `testInScopeAllCheckedWhenFilterSetToAll()`

## Impact

This fix affects:
- **Nurse Call** tab config group filter
- **Clinical** tab config group filter
- **Orders** tab config group filter

The fix ensures consistency: when "All" is selected, the user sees all rows with all checkboxes checked, making it clear that all items are in scope for JSON generation.

## Security

✅ No security vulnerabilities introduced
✅ CodeQL scan: 0 alerts
✅ Code review: No issues found
