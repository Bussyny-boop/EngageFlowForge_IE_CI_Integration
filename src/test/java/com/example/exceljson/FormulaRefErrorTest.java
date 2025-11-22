package com.example.exceljson;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests to ensure that formula strings (e.g., "=COUNTA(INDEX(...))") are never
 * returned as cell values, especially for formulas containing #REF! errors.
 * This addresses the issue where users see formula text instead of evaluated values.
 */
public class FormulaRefErrorTest {

  @Test
  public void testFormulaWithRefErrorReturnsEmpty(@TempDir Path tempDir) throws Exception {
    // Create a test Excel file with a formula containing #REF! error
    File excelFile = tempDir.resolve("test-ref-error.xlsx").toFile();
    
    try (Workbook wb = new XSSFWorkbook()) {
      // Create Unit Breakdown sheet
      Sheet unitSheet = wb.createSheet("Unit Breakdown");
      Row unitHeader = unitSheet.createRow(0);
      unitHeader.createCell(0).setCellValue("Facility");
      unitHeader.createCell(1).setCellValue("Common Unit Name");
      unitHeader.createCell(2).setCellValue("Nurse Call");
      
      Row unitData = unitSheet.createRow(1);
      unitData.createCell(0).setCellValue("Test Hospital");
      unitData.createCell(1).setCellValue("Test Unit");
      unitData.createCell(2).setCellValue("Nurse Group 1");
      
      // Create Nurse Call sheet with formula that will have #REF! error
      Sheet nurseSheet = wb.createSheet("Nurse call");
      Row nurseHeader = nurseSheet.createRow(0);
      nurseHeader.createCell(0).setCellValue("In Scope?");
      nurseHeader.createCell(1).setCellValue("Configuration Group");
      nurseHeader.createCell(2).setCellValue("Common Alert or Alarm Name");
      nurseHeader.createCell(3).setCellValue("Sending System Alert Name");
      
      Row nurseData = nurseSheet.createRow(1);
      nurseData.createCell(0).setCellValue("Yes");
      nurseData.createCell(1).setCellValue("Nurse Group 1");
      nurseData.createCell(2).setCellValue("Test Alarm");
      
      // Create a formula that references a non-existent named range
      // This simulates the user's scenario: COUNTA(INDEX(DeviceValData,,MATCH('Nurse Call'!G44,#REF!,0)))
      Cell formulaCell = nurseData.createCell(3);
      // We can't create an exact #REF! in POI easily, but we can create formulas that will fail
      // Let's create a formula that will evaluate to an error
      formulaCell.setCellFormula("1/0"); // This creates #DIV/0! error, which should be handled the same way
      
      // Save without evaluating (simulates files where formulas haven't been evaluated)
      try (FileOutputStream fos = new FileOutputStream(excelFile)) {
        wb.write(fos);
      }
    }
    
    // Load the Excel file using ExcelParserV5
    ExcelParserV5 parser = new ExcelParserV5();
    parser.load(excelFile);
    
    // Verify that the error formula returns empty string, NOT the formula string
    assertEquals(1, parser.nurseCalls.size(), "Should have one nurse call");
    ExcelParserV5.FlowRow flow = parser.nurseCalls.getFirst();
    
    // The sending name should be empty, not the formula string
    assertNotNull(flow.sendingName, "Sending name should not be null");
    assertEquals("", flow.sendingName, 
        "Error formula should return empty string, not formula text like '=1/0'");
    assertFalse(flow.sendingName.startsWith("="), 
        "Cell value should NEVER start with '=' (formula marker)");
  }

  @Test
  public void testComplexFormulaWithErrorReturnsEmpty(@TempDir Path tempDir) throws Exception {
    // Test with a more complex formula structure similar to the user's issue
    File excelFile = tempDir.resolve("test-complex-formula.xlsx").toFile();
    
    try (Workbook wb = new XSSFWorkbook()) {
      // Create Unit Breakdown sheet
      Sheet unitSheet = wb.createSheet("Unit Breakdown");
      Row unitHeader = unitSheet.createRow(0);
      unitHeader.createCell(0).setCellValue("Facility");
      unitHeader.createCell(1).setCellValue("Common Unit Name");
      unitHeader.createCell(2).setCellValue("Nurse Call");
      
      Row unitData = unitSheet.createRow(1);
      unitData.createCell(0).setCellValue("Test Hospital");
      unitData.createCell(1).setCellValue("Test Unit");
      unitData.createCell(2).setCellValue("Nurse Group 1");
      
      // Create Nurse Call sheet
      Sheet nurseSheet = wb.createSheet("Nurse call");
      Row nurseHeader = nurseSheet.createRow(0);
      nurseHeader.createCell(0).setCellValue("In Scope?");
      nurseHeader.createCell(1).setCellValue("Configuration Group");
      nurseHeader.createCell(2).setCellValue("Common Alert or Alarm Name");
      nurseHeader.createCell(3).setCellValue("Sending System Alert Name");
      nurseHeader.createCell(4).setCellValue("Priority");
      
      Row nurseData = nurseSheet.createRow(1);
      nurseData.createCell(0).setCellValue("Yes");
      nurseData.createCell(1).setCellValue("Nurse Group 1");
      nurseData.createCell(2).setCellValue("Test Alarm");
      // Create a complex nested formula that will fail
      Cell formulaCell = nurseData.createCell(3);
      formulaCell.setCellFormula("IF(ISERROR(INDIRECT(\"InvalidSheet!A1\")),\"Error\",\"OK\")");
      nurseData.createCell(4).setCellValue("Normal");
      
      // Try to evaluate (this will fail for the invalid reference)
      FormulaEvaluator evaluator = wb.getCreationHelper().createFormulaEvaluator();
      try {
        evaluator.evaluateAll();
      } catch (Exception e) {
        // Expected - formula has invalid reference
      }
      
      // Save the workbook
      try (FileOutputStream fos = new FileOutputStream(excelFile)) {
        wb.write(fos);
      }
    }
    
    // Load and verify
    ExcelParserV5 parser = new ExcelParserV5();
    parser.load(excelFile);
    
    assertEquals(1, parser.nurseCalls.size(), "Should have one nurse call");
    ExcelParserV5.FlowRow flow = parser.nurseCalls.getFirst();
    
    // Should not contain formula text
    assertFalse(flow.sendingName.contains("INDIRECT"), 
        "Should not contain formula function names");
    assertFalse(flow.sendingName.contains("ISERROR"), 
        "Should not contain formula function names");
    assertFalse(flow.sendingName.startsWith("="), 
        "Should never start with = (formula marker)");
  }

  @Test
  public void testUnevaluatedFormulasAreHandled(@TempDir Path tempDir) throws Exception {
    // Test formulas that haven't been evaluated yet (no cached result)
    File excelFile = tempDir.resolve("test-unevaluated.xlsx").toFile();
    
    try (Workbook wb = new XSSFWorkbook()) {
      // Create Unit Breakdown sheet
      Sheet unitSheet = wb.createSheet("Unit Breakdown");
      Row unitHeader = unitSheet.createRow(0);
      unitHeader.createCell(0).setCellValue("Facility");
      unitHeader.createCell(1).setCellValue("Common Unit Name");
      unitHeader.createCell(2).setCellValue("Nurse Call");
      
      Row unitData = unitSheet.createRow(1);
      unitData.createCell(0).setCellValue("Test Hospital");
      // Use a formula in the unit name
      Cell unitNameCell = unitData.createCell(1);
      unitNameCell.setCellFormula("CONCATENATE(\"Test\",\" \",\"Unit\")");
      unitData.createCell(2).setCellValue("Nurse Group 1");
      
      // Create Nurse Call sheet
      Sheet nurseSheet = wb.createSheet("Nurse call");
      Row nurseHeader = nurseSheet.createRow(0);
      nurseHeader.createCell(0).setCellValue("In Scope?");
      nurseHeader.createCell(1).setCellValue("Configuration Group");
      nurseHeader.createCell(2).setCellValue("Common Alert or Alarm Name");
      
      Row nurseData = nurseSheet.createRow(1);
      nurseData.createCell(0).setCellValue("Yes");
      nurseData.createCell(1).setCellValue("Nurse Group 1");
      nurseData.createCell(2).setCellValue("Test Alarm");
      
      // DON'T evaluate formulas before saving (simulates unevaluated formulas)
      // Save the workbook
      try (FileOutputStream fos = new FileOutputStream(excelFile)) {
        wb.write(fos);
      }
    }
    
    // Load and verify
    ExcelParserV5 parser = new ExcelParserV5();
    parser.load(excelFile);
    
    assertEquals(1, parser.units.size(), "Should have one unit");
    ExcelParserV5.UnitRow unit = parser.units.getFirst();
    
    // The formula should be evaluated by the parser's formula evaluator
    assertEquals("Test Unit", unit.unitNames, 
        "Formula should be evaluated to 'Test Unit', not returned as formula text");
    assertFalse(unit.unitNames.contains("CONCATENATE"), 
        "Should not contain formula function names");
    assertFalse(unit.unitNames.startsWith("="), 
        "Should never start with = (formula marker)");
  }

  @Test
  public void testFormulaStringNeverLeaksThrough(@TempDir Path tempDir) throws Exception {
    // Final test: Ensure that even in worst-case scenarios, formula strings never appear
    File excelFile = tempDir.resolve("test-no-leak.xlsx").toFile();
    
    try (Workbook wb = new XSSFWorkbook()) {
      // Create Unit Breakdown sheet
      Sheet unitSheet = wb.createSheet("Unit Breakdown");
      Row unitHeader = unitSheet.createRow(0);
      unitHeader.createCell(0).setCellValue("Facility");
      unitHeader.createCell(1).setCellValue("Common Unit Name");
      unitHeader.createCell(2).setCellValue("Nurse Call");
      
      Row unitData = unitSheet.createRow(1);
      unitData.createCell(0).setCellValue("Test Hospital");
      unitData.createCell(1).setCellValue("Test Unit");
      unitData.createCell(2).setCellValue("Nurse Group 1");
      
      // Create Nurse Call sheet with various problematic formulas
      Sheet nurseSheet = wb.createSheet("Nurse call");
      Row nurseHeader = nurseSheet.createRow(0);
      nurseHeader.createCell(0).setCellValue("In Scope?");
      nurseHeader.createCell(1).setCellValue("Configuration Group");
      nurseHeader.createCell(2).setCellValue("Common Alert or Alarm Name");
      nurseHeader.createCell(3).setCellValue("Sending System Alert Name");
      nurseHeader.createCell(4).setCellValue("Priority");
      
      // Row 1: Error formula
      Row row1 = nurseSheet.createRow(1);
      row1.createCell(0).setCellValue("Yes");
      row1.createCell(1).setCellValue("Nurse Group 1");
      row1.createCell(2).setCellValue("Alarm 1");
      row1.createCell(3).setCellFormula("1/0"); // #DIV/0! error
      row1.createCell(4).setCellValue("Normal");
      
      // Row 2: Valid formula
      Row row2 = nurseSheet.createRow(2);
      row2.createCell(0).setCellValue("Yes");
      row2.createCell(1).setCellValue("Nurse Group 1");
      row2.createCell(2).setCellValue("Alarm 2");
      row2.createCell(3).setCellFormula("\"Device\" & \" \" & \"A\""); // Should evaluate to "Device A"
      row2.createCell(4).setCellValue("Normal");
      
      // Row 3: Formula with ISERROR
      Row row3 = nurseSheet.createRow(3);
      row3.createCell(0).setCellValue("Yes");
      row3.createCell(1).setCellValue("Nurse Group 1");
      row3.createCell(2).setCellValue("Alarm 3");
      row3.createCell(3).setCellFormula("IF(ISERROR(1/0),\"\",\"OK\")"); // Should evaluate to empty
      row3.createCell(4).setCellValue("Normal");
      
      // Evaluate formulas
      FormulaEvaluator evaluator = wb.getCreationHelper().createFormulaEvaluator();
      evaluator.evaluateAll();
      
      // Save the workbook
      try (FileOutputStream fos = new FileOutputStream(excelFile)) {
        wb.write(fos);
      }
    }
    
    // Load and verify ALL rows
    ExcelParserV5 parser = new ExcelParserV5();
    parser.load(excelFile);
    
    assertEquals(3, parser.nurseCalls.size(), "Should have three nurse calls");
    
    // Check all rows - NONE should have formula strings
    for (int i = 0; i < parser.nurseCalls.size(); i++) {
      ExcelParserV5.FlowRow flow = parser.nurseCalls.get(i);
      assertFalse(flow.sendingName.startsWith("="), 
          "Row " + (i+1) + ": Should never start with = (formula marker)");
      assertFalse(flow.sendingName.contains("IF("), 
          "Row " + (i+1) + ": Should not contain formula syntax");
      assertFalse(flow.sendingName.contains("ISERROR"), 
          "Row " + (i+1) + ": Should not contain formula function names");
    }
    
    // Specific checks
    assertEquals("", parser.nurseCalls.getFirst().sendingName, "Error formula should be empty");
    assertEquals("Device A", parser.nurseCalls.get(1).sendingName, "Valid formula should evaluate");
    assertEquals("", parser.nurseCalls.get(2).sendingName, "ISERROR formula should evaluate to empty");
  }
}
