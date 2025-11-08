# Header Row Detection Fix - Row 3 Support

## Problem Statement
The program was not loading Excel files whose header starts from the 3rd row when earlier rows contained 3+ non-empty cells.

## Root Cause Analysis

### Previous Implementation
The `findHeaderRow()` method in `ExcelParserV5.java` used a simple heuristic:
- Return the **first row** with 3 or more non-empty cells
- This worked when headers were in row 1 or when earlier rows were empty/sparse

### Failure Scenario
When an Excel file had this structure:
```
Row 1: | Report Title        | Generated:  | 2024-01-01    |  (3 cells - incorrectly detected as header)
Row 2: | Department          | Location    | Version 2.0   |  (3 cells - could be detected as header)
Row 3: | Facility            | Common Unit Name | Nurse Call Configuration Group |  (ACTUAL HEADER)
Row 4: | General Hospital    | ICU         | ICU Config    |  (Data)
```

The parser would:
1. ❌ Select row 1 as the header (it has 3+ cells)
2. ❌ Try to find columns "Facility", "Common Unit Name", etc.
3. ❌ Fail validation because row 1 doesn't have the expected column names
4. ❌ Throw error: "Missing required headers"

## Solution: Smart Header Detection with Keyword Scoring

### New Algorithm
Instead of just counting cells, the new implementation:

1. **Defines expected header keywords** (Set of common column name tokens):
   - `facility`, `unit`, `name`, `configuration`, `group`, `config`
   - `alert`, `alarm`, `priority`, `device`, `sending`, `system`
   - `patient`, `monitoring`, `nurse`, `call`, `common`, `order`

2. **Scores each row** based on:
   - **Header matches**: How many cells contain expected keywords (weight: 100)
   - **Non-empty cells**: Total non-empty cells (weight: 1)
   - **Formula**: `score = headerMatches × 100 + nonEmpty`

3. **Selects the best match**:
   - Row must have at least 3 non-empty cells
   - Row with highest score is selected as the header

### Example Scoring
For the file structure above:

| Row | Non-Empty | Header Matches | Score | Selected? |
|-----|-----------|----------------|-------|-----------|
| 1   | 3         | 0              | 3     | ❌        |
| 2   | 3         | 0              | 3     | ❌        |
| 3   | 3         | 3              | 303   | ✅        |

Row 3 wins because it contains "Facility", "Unit", and "Configuration" - all expected header keywords!

## Implementation Details

### Code Changes
**File**: `src/main/java/com/example/exceljson/ExcelParserV5.java`

**Before** (simplified):
```java
private static Row findHeaderRow(Sheet sh) {
    for (int r = 0; r <= 2; r++) {
        Row row = sh.getRow(r);
        if (row == null) continue;
        int nonEmpty = 0;
        for (int c = 0; c < row.getLastCellNum(); c++)
            if (!getCell(row, c).isBlank()) nonEmpty++;
        if (nonEmpty >= 3) return row;  // Returns FIRST row with 3+ cells
    }
    // ... fallback logic
}
```

**After** (simplified):
```java
private static Row findHeaderRow(Sheet sh) {
    Set<String> expectedHeaderTokens = Set.of(
        "facility", "unit", "name", "configuration", "group", "config",
        "alert", "alarm", "priority", "device", "sending", "system",
        "patient", "monitoring", "nurse", "call", "common", "order"
    );
    
    int bestRow = -1;
    int bestScore = 0;
    
    for (int r = 0; r <= 2; r++) {
        Row row = sh.getRow(r);
        if (row == null) continue;
        
        int nonEmpty = 0;
        int headerMatches = 0;
        for (int c = 0; c < row.getLastCellNum(); c++) {
            String val = getCell(row, c);
            if (!val.isBlank()) {
                nonEmpty++;
                String normalized = normalize(val);
                for (String token : expectedHeaderTokens) {
                    if (normalized.contains(token)) {
                        headerMatches++;
                        break;
                    }
                }
            }
        }
        
        int score = headerMatches * 100 + nonEmpty;
        if (score > bestScore && nonEmpty >= 3) {
            bestScore = score;
            bestRow = r;
        }
    }
    
    if (bestRow >= 0) return sh.getRow(bestRow);
    // ... fallback logic
}
```

### Test Coverage
**New Test**: `WrongHeaderSelectionTest.java`
- Creates Excel file with title rows (rows 1-2) before actual header (row 3)
- Verifies that row 3 is correctly selected as the header
- Validates that data is loaded correctly

**Test Results**:
- ✅ All 250 tests pass
- ✅ No regression in existing functionality
- ✅ New test validates the fix

### Manual Verification
Created and successfully loaded an Excel file with:
- Row 1: "Hospital Configuration Report", "Generated:", "2024-01-15" (3 cells, no header keywords)
- Row 2: "Department", "Location", "Version 2.0" (3 cells, no header keywords)
- Row 3: "Facility", "Common Unit Name", "Nurse Call Configuration Group" (actual header)
- Row 4: "General Hospital", "ICU", "ICU Config" (data)

**Result**: ✅ File loaded successfully
- Header detected in row 3
- Data loaded: 1 unit, 1 nurse call
- Facility: "General Hospital"
- Unit: "ICU"
- Config Group: "ICU Config"

## Benefits

1. **✅ Robust Header Detection**: Works even with title/metadata rows before the header
2. **✅ Backward Compatible**: All existing files continue to work
3. **✅ Intelligent Matching**: Uses domain knowledge to identify true headers
4. **✅ Flexible**: Works with headers in rows 1, 2, or 3
5. **✅ No Breaking Changes**: Fallback logic preserved for edge cases

## Supported File Formats

### Format 1: Header in Row 1 (Most Common)
```
Row 1: | Facility | Common Unit Name | Configuration Group |
Row 2: | Hospital A | Unit 1 | Config 1 |
```
**Detection**: Row 1 scores highest (contains "facility", "unit", "configuration")

### Format 2: Header in Row 2
```
Row 1: | Hospital Configuration Report |
Row 2: | Facility | Common Unit Name | Configuration Group |
Row 3: | Hospital A | Unit 1 | Config 1 |
```
**Detection**: Row 2 scores highest (contains header keywords)

### Format 3: Header in Row 3 (Fixed by this PR)
```
Row 1: | Report Title | Generated: | 2024-01-01 |
Row 2: | Department | Location | Version |
Row 3: | Facility | Common Unit Name | Configuration Group |
Row 4: | Hospital A | Unit 1 | Config 1 |
```
**Detection**: Row 3 scores highest (contains header keywords)

## Performance Impact
**Minimal** - The enhanced logic:
- Checks the same number of rows (0-2, with fallback to 2-5)
- Adds keyword matching (O(keywords × cells) = O(20 × ~10) = O(200) operations per row)
- Total iterations: same as before (max 7 rows checked)
- No performance degradation observed in tests

## Security
✅ CodeQL scan: 0 vulnerabilities found
✅ No new security issues introduced

## Conclusion
This fix resolves the issue where Excel files with title rows before the actual header could not be loaded. The smart keyword-based scoring system ensures that the true header row is correctly identified, making the parser more robust and user-friendly.
