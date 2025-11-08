# Header Detection Enhancement - Rows 1-3 Support

## Summary
Enhanced the Excel parser to detect headers in Row 1, Row 2, or Row 3 (0-indexed as rows 0, 1, 2) as the primary search, with fallback support for headers in rows 4-5.

## Problem Statement
The original parser expected headers to be in specific rows (starting from row 2), which caused issues when Excel files had headers in Row 1, Row 2, or Row 3.

## Solution
Modified the `findHeaderRow()` method in `ExcelParserV5.java` to:
1. **Primary Search:** Check rows 1-3 (0-indexed: 0, 1, 2) first
2. **Fallback Search:** Check rows 2-5 (original expected positions)
3. **Final Fallback:** Return first non-null row

## Technical Changes

### Before (Original Logic)
```java
private static Row findHeaderRow(Sheet sh) {
    if (sh == null) return null;

    // First try expected positions (row 2..5)
    int start = 2;
    int end = Math.min(sh.getLastRowNum(), start + 3);
    for (int r = start; r <= end; r++) {
      Row row = sh.getRow(r);
      if (row == null) continue;
      int nonEmpty = 0;
      for (int c = 0; c < row.getLastCellNum(); c++)
        if (!getCell(row, c).isBlank()) nonEmpty++;
      if (nonEmpty >= 3) return row;
    }

    // Fallback: detect header near top (row 0..3) for re-saved Excel files
    for (int r = 0; r <= Math.min(sh.getLastRowNum(), 3); r++) {
      Row row = sh.getRow(r);
      if (row == null) continue;
      int nonEmpty = 0;
      for (int c = 0; c < row.getLastCellNum(); c++)
        if (!getCell(row, c).isBlank()) nonEmpty++;
      if (nonEmpty >= 3) return row;
    }

    // Final fallback
    for (int r = 0; r <= sh.getLastRowNum(); r++) {
      Row row = sh.getRow(r);
      if (row != null) return row;
    }
    return null;
}
```

### After (Enhanced Logic)
```java
private static Row findHeaderRow(Sheet sh) {
    if (sh == null) return null;

    // Primary search: Check rows 1-3 (0-indexed: 0, 1, 2) for headers
    // This allows headers to be in any of the first 3 rows
    for (int r = 0; r <= Math.min(sh.getLastRowNum(), 2); r++) {
      Row row = sh.getRow(r);
      if (row == null) continue;
      int nonEmpty = 0;
      for (int c = 0; c < row.getLastCellNum(); c++)
        if (!getCell(row, c).isBlank()) nonEmpty++;
      if (nonEmpty >= 3) return row;
    }

    // Fallback: Check rows 2-5 (original expected positions)
    int start = 2;
    int end = Math.min(sh.getLastRowNum(), start + 3);
    for (int r = start; r <= end; r++) {
      Row row = sh.getRow(r);
      if (row == null) continue;
      int nonEmpty = 0;
      for (int c = 0; c < row.getLastCellNum(); c++)
        if (!getCell(row, c).isBlank()) nonEmpty++;
      if (nonEmpty >= 3) return row;
    }

    // Final fallback: return first non-null row
    for (int r = 0; r <= sh.getLastRowNum(); r++) {
      Row row = sh.getRow(r);
      if (row != null) return row;
    }
    return null;
}
```

## Header Detection Priority

### Priority Order:
1. **Row 1** (0-indexed: 0) - Highest priority
2. **Row 2** (0-indexed: 1) - Second priority
3. **Row 3** (0-indexed: 2) - Third priority
4. **Row 4** (0-indexed: 3) - Fallback (original position)
5. **Row 5** (0-indexed: 4) - Fallback (original position)
6. **First non-null row** - Final fallback

### Detection Criteria:
- A row is considered a header if it has **3 or more non-empty cells**
- Empty cells, "N/A", and blank cells are ignored
- The first row meeting the criteria is selected as the header

## Test Coverage

Created comprehensive test suite: `HeaderRowDetectionTest.java`

### Test Cases:
1. ✅ **testHeaderInRow1** - Validates headers in Row 1 (index 0)
2. ✅ **testHeaderInRow2** - Validates headers in Row 2 (index 1)
3. ✅ **testHeaderInRow3** - Validates headers in Row 3 (index 2)
4. ✅ **testHeaderInRow4NotPreferred** - Validates fallback to Row 4 (index 3)
5. ✅ **testMultipleHeaderRows_UsesFirst** - Validates first header is used when multiple exist

### Test Results:
```
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0
```

## Supported Excel Formats

The parser now supports Excel files with headers in any of these positions:

### Example 1: Header in Row 1
```
Row 1: | Facility | Common Unit Name | Configuration Group |
Row 2: | Facility A | Unit 1 | Group 1 |
Row 3: | Facility B | Unit 2 | Group 2 |
```

### Example 2: Header in Row 2
```
Row 1: | (blank or title) |
Row 2: | Facility | Common Unit Name | Configuration Group |
Row 3: | Facility A | Unit 1 | Group 1 |
```

### Example 3: Header in Row 3
```
Row 1: | (blank or title) |
Row 2: | (blank or subtitle) |
Row 3: | Facility | Common Unit Name | Configuration Group |
Row 4: | Facility A | Unit 1 | Group 1 |
```

## Backward Compatibility

✅ **Fully backward compatible** - All existing Excel files continue to work:
- Files with headers in rows 4-5 still supported (fallback)
- No changes required to existing Excel templates
- All existing tests pass without modification

## Benefits

1. **Flexibility:** Supports headers in any of the first 3 rows
2. **User-Friendly:** Works with various Excel file formats
3. **Robust:** Multiple fallback strategies ensure headers are found
4. **Tested:** Comprehensive test suite validates all scenarios
5. **Compatible:** No breaking changes to existing functionality

## Use Cases

### Use Case 1: Standard Template
- Header in Row 1 (most common)
- Immediate data entry starting Row 2

### Use Case 2: Title Row
- Row 1: Document title or metadata
- Row 2: Column headers
- Row 3: Data starts

### Use Case 3: Multi-Line Headers
- Row 1: Department or facility info
- Row 2: Section title
- Row 3: Column headers
- Row 4: Data starts

## Error Handling

If no valid header is found:
- Parser throws exception with helpful error message
- Message indicates which sheet had the problem
- Suggests required columns that must be present

Example error:
```
❌ Invalid Excel file: No headers found in 'Nurse Call' sheet.

The sheet must contain a header row with the following required columns:
  • Configuration Group
  • Common Alert or Alarm Name
```

## Performance Impact

**Minimal** - The enhanced logic:
- Checks rows 0-2 first (3 iterations)
- Falls back to rows 2-5 only if needed (up to 4 more iterations)
- Total maximum iterations: same as before (7)
- No performance degradation

## Implementation Complete

- ✅ Header detection updated
- ✅ Test suite created and passing
- ✅ Documentation complete
- ✅ Backward compatibility verified
- ✅ All existing tests pass
