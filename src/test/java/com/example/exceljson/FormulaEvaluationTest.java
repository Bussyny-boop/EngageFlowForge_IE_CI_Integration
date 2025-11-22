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
 * Tests that Excel formulas are evaluated correctly and don't return formula strings.
 */
public class FormulaEvaluationTest {

  @Test
  public void testFormulaEvaluation(@TempDir Path tempDir) throws Exception {
    // Create a test Excel file with formulas
    File excelFile = tempDir.resolve("test-formulas.xlsx").toFile();
    
    try (Workbook wb = new XSSFWorkbook()) {
      // Create Unit Breakdown sheet
      Sheet unitSheet = wb.createSheet("Unit Breakdown");
      Row headerRow = unitSheet.createRow(0);
      headerRow.createCell(0).setCellValue("Facility");
      headerRow.createCell(1).setCellValue("Common Unit Name");
      headerRow.createCell(2).setCellValue("Nurse Call");
      
      // Create a row with a formula
      Row dataRow = unitSheet.createRow(1);
      dataRow.createCell(0).setCellValue("Test Hospital");
      dataRow.createCell(1).setCellFormula("\"Unit \" & \"A\""); // Formula that concatenates to "Unit A"
      dataRow.createCell(2).setCellFormula("IF(TRUE,\"Nurse Group 1\",\"\")"); // Formula that returns "Nurse Group 1"
      
      // Create Nurse Call sheet with minimal data
      Sheet nurseSheet = wb.createSheet("Nurse call");
      Row nurseHeader = nurseSheet.createRow(0);
      nurseHeader.createCell(0).setCellValue("Configuration Group");
      nurseHeader.createCell(1).setCellValue("Common Alert or Alarm Name");
      
      Row nurseData = nurseSheet.createRow(1);
      nurseData.createCell(0).setCellFormula("\"Nurse \" & \"Group \" & \"1\""); // Formula that returns "Nurse Group 1"
      nurseData.createCell(1).setCellValue("Test Alarm");
      
      // Force formula evaluation
      FormulaEvaluator evaluator = wb.getCreationHelper().createFormulaEvaluator();
      evaluator.evaluateAll();
      
      // Save the workbook
      try (FileOutputStream fos = new FileOutputStream(excelFile)) {
        wb.write(fos);
      }
    }
    
    // Load the Excel file using ExcelParserV5
    ExcelParserV5 parser = new ExcelParserV5();
    parser.load(excelFile);
    
    // Verify that formulas were evaluated, not returned as strings
    assertEquals(1, parser.units.size(), "Should have one unit");
    ExcelParserV5.UnitRow unit = parser.units.get(0);
    
    // Check that the unit name is the evaluated result, not the formula string
    assertEquals("Unit A", unit.unitNames, 
        "Unit name should be evaluated formula result 'Unit A', not the formula string");
    
    // Check that the nurse group is the evaluated result
    assertEquals("Nurse Group 1", unit.nurseGroup, 
        "Nurse group should be evaluated formula result 'Nurse Group 1', not the formula string");
    
    // Check the nurse call data
    assertEquals(1, parser.nurseCalls.size(), "Should have one nurse call");
    ExcelParserV5.FlowRow flow = parser.nurseCalls.get(0);
    
    assertEquals("Nurse Group 1", flow.configGroup,
        "Config group should be evaluated formula result 'Nurse Group 1', not the formula string");
    assertEquals("Test Alarm", flow.alarmName, "Alarm name should be the string value");
  }
  
  @Test
  public void testFormulaWithError(@TempDir Path tempDir) throws Exception {
    // Create a test Excel file with an error formula
    File excelFile = tempDir.resolve("test-formula-error.xlsx").toFile();
    
    try (Workbook wb = new XSSFWorkbook()) {
      // Create Unit Breakdown sheet
      Sheet unitSheet = wb.createSheet("Unit Breakdown");
      Row headerRow = unitSheet.createRow(0);
      headerRow.createCell(0).setCellValue("Facility");
      headerRow.createCell(1).setCellValue("Common Unit Name");
      headerRow.createCell(2).setCellValue("Nurse Call");
      
      // Create a row with an error formula (reference error)
      Row dataRow = unitSheet.createRow(1);
      dataRow.createCell(0).setCellValue("Test Hospital");
      dataRow.createCell(1).setCellValue("Unit B"); // Normal value
      // Create a formula that will produce a #REF! error (similar to the issue description)
      // We'll use INDIRECT to reference a deleted/invalid sheet which causes #REF!
      Cell errorCell = dataRow.createCell(2);
      errorCell.setCellFormula("1/0"); // Division by zero error (easier to create than #REF!)
      
      // Note: Creating an actual #REF! error programmatically is difficult because
      // POI validates formulas. Division by zero (#DIV/0!) behaves the same way
      // as other error types (#REF!, #VALUE!, etc.) - they all return empty string.
      
      // Create Nurse Call sheet with minimal data
      Sheet nurseSheet = wb.createSheet("Nurse call");
      Row nurseHeader = nurseSheet.createRow(0);
      nurseHeader.createCell(0).setCellValue("Configuration Group");
      nurseHeader.createCell(1).setCellValue("Common Alert or Alarm Name");
      
      // Save the workbook without evaluating (to test error handling)
      try (FileOutputStream fos = new FileOutputStream(excelFile)) {
        wb.write(fos);
      }
    }
    
    // Load the Excel file using ExcelParserV5
    ExcelParserV5 parser = new ExcelParserV5();
    parser.load(excelFile);
    
    // Verify that the error formula returns empty string, not the formula string or error
    assertEquals(1, parser.units.size(), "Should have one unit");
    ExcelParserV5.UnitRow unit = parser.units.get(0);
    
    // Error formulas should return empty string
    assertEquals("", unit.nurseGroup, 
        "Error formula should return empty string, not formula text or error value");
    assertEquals("Unit B", unit.unitNames, "Normal value should work fine");
  }
  
  @Test
  public void testNumericFormula(@TempDir Path tempDir) throws Exception {
    // Create a test Excel file with numeric formulas
    File excelFile = tempDir.resolve("test-numeric-formula.xlsx").toFile();
    
    try (Workbook wb = new XSSFWorkbook()) {
      // Create Unit Breakdown sheet
      Sheet unitSheet = wb.createSheet("Unit Breakdown");
      Row headerRow = unitSheet.createRow(0);
      headerRow.createCell(0).setCellValue("Facility");
      headerRow.createCell(1).setCellValue("Common Unit Name");
      
      // Create a row with a numeric formula
      Row dataRow = unitSheet.createRow(1);
      dataRow.createCell(0).setCellValue("Test Hospital");
      // Create a cell with numeric formula that should be converted to string
      Cell numCell = dataRow.createCell(1);
      numCell.setCellFormula("5 + 3"); // Should evaluate to 8
      
      // Create Nurse Call sheet with minimal data
      Sheet nurseSheet = wb.createSheet("Nurse call");
      Row nurseHeader = nurseSheet.createRow(0);
      nurseHeader.createCell(0).setCellValue("Configuration Group");
      nurseHeader.createCell(1).setCellValue("Common Alert or Alarm Name");
      
      // Force formula evaluation
      FormulaEvaluator evaluator = wb.getCreationHelper().createFormulaEvaluator();
      evaluator.evaluateAll();
      
      // Save the workbook
      try (FileOutputStream fos = new FileOutputStream(excelFile)) {
        wb.write(fos);
      }
    }
    
    // Load the Excel file using ExcelParserV5
    ExcelParserV5 parser = new ExcelParserV5();
    parser.load(excelFile);
    
    // Verify numeric formula result
    assertEquals(1, parser.units.size(), "Should have one unit");
    ExcelParserV5.UnitRow unit = parser.units.get(0);
    
    // Numeric formula should be converted to string
    assertEquals("8.0", unit.unitNames, 
        "Numeric formula should be evaluated and converted to string");
  }
}
