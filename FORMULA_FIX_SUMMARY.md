# Excel Formula Evaluation Fix

## Problem
Excel formula strings (e.g., `COUNTA(INDEX(DeviceValData,,MATCH('Nurse Call'!G68,#REF!,0)))`) were appearing in the application UI instead of their computed values.

## Root Cause
The `getCell()` method in `ExcelParserV5.java` was relying on cached formula results from Excel files. When:
- Formulas contained errors (like `#REF!`, `#DIV/0!`, etc.)
- Formulas hadn't been pre-evaluated when the workbook was saved
- The cached result type was ERROR

The method would either return an empty string OR in some edge cases, the formula string itself could leak through.

## Solution
Implemented proper formula evaluation using Apache POI's `FormulaEvaluator`:

### Changes Made
1. **Added FormulaEvaluator instance variable** (line 91)
   ```java
   private FormulaEvaluator formulaEvaluator = null;
   ```

2. **Initialize evaluator in load() method** (lines 267-269)
   ```java
   formulaEvaluator = wb.getCreationHelper().createFormulaEvaluator();
   ```

3. **Use evaluator in getCell() method** (lines 3664-3686)
   ```java
   if (formulaEvaluator != null) {
     CellValue cellValue = formulaEvaluator.evaluate(cell);
     String result = switch (cellValue.getCellType()) {
       case STRING -> cellValue.getStringValue().trim();
       case NUMERIC -> String.valueOf(cellValue.getNumberValue());
       case BOOLEAN -> String.valueOf(cellValue.getBooleanValue());
       case ERROR -> ""; // Return empty for error cells
       default -> "";
     };
     yield result;
   }
   ```

4. **Changed methods from static to instance methods**
   - `getCell()` - now uses instance variable `formulaEvaluator`
   - `getFirstNonEmptyValue()` - calls instance method `getCell()`
   - `findHeaderRow()` - calls instance method `getCell()`
   - `headerMap()` - calls instance method `getCell()`

## Benefits
1. **Proper formula evaluation**: All formulas are now evaluated using POI's built-in evaluator
2. **Error handling**: Formula errors (`#REF!`, `#DIV/0!`, `#VALUE!`, etc.) return empty strings
3. **Backward compatible**: Falls back to cached results if evaluator is not available
4. **Comprehensive testing**: 3 new tests verify formula evaluation works correctly

## Testing
- All 541 tests pass (including 3 new formula evaluation tests)
- Tested with sample Excel file - no formula strings in output
- Code review passed
- Security scan passed (0 alerts)
- Manual verification confirms complex formulas (COUNTA, INDEX, MATCH) work correctly

## Files Modified
1. `src/main/java/com/example/exceljson/ExcelParserV5.java` - Core fix
2. `src/test/java/com/example/exceljson/FormulaEvaluationTest.java` - New test file

## Example
**Before**: Cell shows `COUNTA(INDEX(DeviceValData,,MATCH('Nurse Call'!G68,#REF!,0)))`
**After**: Cell shows the computed value or empty string if the formula has an error
