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
 * Test to verify that Excel formulas are evaluated and their calculated values
 * are imported, not the formula strings themselves.
 */
class FormulaCellHandlingTest {

    @Test
    void testFormulaValuesAreImportedNotFormulas(@TempDir Path tempDir) throws Exception {
        File excelFile = tempDir.resolve("formula_test.xlsx").toFile();
        
        // Create a workbook with formulas in cells
        createWorkbookWithFormulas(excelFile);
        
        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(excelFile);
        
        // Verify that formulas were evaluated and values were imported
        // Check NurseCalls data
        var nurseCalls = parser.nurseCalls;
        assertFalse(nurseCalls.isEmpty(), "Should have loaded nurse calls");
        
        // Find the row with the test alarm
        var formulaRow = nurseCalls.stream()
            .filter(row -> row.alarmName.equals("Test Alarm"))
            .findFirst();
        
        assertTrue(formulaRow.isPresent(), "Should find the alarm with formula value");
        
        // Verify the escalateAfter field contains the calculated value (120) not the formula
        assertEquals("120.0", formulaRow.get().escalateAfter, 
            "Should import the calculated value (120.0) from formula =60+60, not the formula string");
        
        // Verify the ttlValue contains calculated value
        assertEquals("300.0", formulaRow.get().ttlValue,
            "Should import the calculated value (300.0) from formula =5*60, not the formula string");
    }
    
    @Test
    void testStringFormulaValuesAreImported(@TempDir Path tempDir) throws Exception {
        File excelFile = tempDir.resolve("string_formula_test.xlsx").toFile();
        
        // Create a workbook with string formulas
        createWorkbookWithStringFormulas(excelFile);
        
        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(excelFile);
        
        // Verify string formula was evaluated
        var nurseCalls = parser.nurseCalls;
        assertFalse(nurseCalls.isEmpty(), "Should have loaded nurse calls");
        
        var formulaRow = nurseCalls.get(0);
        
        // Verify the sendingName contains the concatenated value, not the formula
        assertEquals("Device Name", formulaRow.sendingName,
            "Should import concatenated value 'Device Name' from formula, not the formula string");
    }

    /**
     * Create a workbook with numeric formulas in cells
     */
    private void createWorkbookWithFormulas(File file) throws Exception {
        try (Workbook wb = new XSSFWorkbook()) {
            // Create Unit Breakdown sheet
            Sheet unitSheet = wb.createSheet("Unit Breakdown");
            Row unitHeader = unitSheet.createRow(0);
            unitHeader.createCell(0).setCellValue("Facility");
            unitHeader.createCell(1).setCellValue("Unit Name(s)");
            unitHeader.createCell(2).setCellValue("Nurse Group");
            
            Row unitData = unitSheet.createRow(1);
            unitData.createCell(0).setCellValue("Test Facility");
            unitData.createCell(1).setCellValue("Test Unit");
            unitData.createCell(2).setCellValue("Nurse Group 1");
            
            // Create Nurse Call sheet with formulas
            Sheet nurseSheet = wb.createSheet("Nurse call");
            Row header = nurseSheet.createRow(0);
            header.createCell(0).setCellValue("In Scope?");
            header.createCell(1).setCellValue("Configuration Group");
            header.createCell(2).setCellValue("Common Alert or Alarm Name");
            header.createCell(3).setCellValue("Sending System Alert Name");
            header.createCell(4).setCellValue("Priority");
            header.createCell(5).setCellValue("Engage 6.6+: Escalate after all declines or 1 decline");
            header.createCell(6).setCellValue("Engage/Edge Display Time (Time to Live) (Device - A)");
            
            Row data = nurseSheet.createRow(1);
            data.createCell(0).setCellValue("Yes");
            data.createCell(1).setCellValue("Nurse Group 1");
            data.createCell(2).setCellValue("Test Alarm");
            data.createCell(3).setCellValue("Test Device");
            data.createCell(4).setCellValue("Normal");
            // Use a formula for Escalate After: =60+60 (should evaluate to 120)
            Cell formulaCell = data.createCell(5);
            formulaCell.setCellFormula("60+60");
            // Use a formula for TTL: =5*60 (should evaluate to 300)
            Cell ttlFormulaCell = data.createCell(6);
            ttlFormulaCell.setCellFormula("5*60");
            
            // Evaluate formulas before saving
            FormulaEvaluator evaluator = wb.getCreationHelper().createFormulaEvaluator();
            evaluator.evaluateAll();
            
            try (FileOutputStream fos = new FileOutputStream(file)) {
                wb.write(fos);
            }
        }
    }
    
    /**
     * Create a workbook with string formulas in cells
     */
    private void createWorkbookWithStringFormulas(File file) throws Exception {
        try (Workbook wb = new XSSFWorkbook()) {
            // Create Unit Breakdown sheet
            Sheet unitSheet = wb.createSheet("Unit Breakdown");
            Row unitHeader = unitSheet.createRow(0);
            unitHeader.createCell(0).setCellValue("Facility");
            unitHeader.createCell(1).setCellValue("Unit Name(s)");
            unitHeader.createCell(2).setCellValue("Nurse Group");
            
            Row unitData = unitSheet.createRow(1);
            unitData.createCell(0).setCellValue("Test Facility");
            unitData.createCell(1).setCellValue("Test Unit");
            unitData.createCell(2).setCellValue("Nurse Group 1");
            
            // Create Nurse Call sheet with string formula
            Sheet nurseSheet = wb.createSheet("Nurse call");
            Row header = nurseSheet.createRow(0);
            header.createCell(0).setCellValue("In Scope?");
            header.createCell(1).setCellValue("Configuration Group");
            header.createCell(2).setCellValue("Common Alert or Alarm Name");
            header.createCell(3).setCellValue("Sending System Alert Name");
            header.createCell(4).setCellValue("Priority");
            
            Row data = nurseSheet.createRow(1);
            data.createCell(0).setCellValue("Yes");
            data.createCell(1).setCellValue("Nurse Group 1");
            data.createCell(2).setCellValue("Test Alarm");
            // Use a string concatenation formula
            Cell stringFormulaCell = data.createCell(3);
            stringFormulaCell.setCellFormula("\"Device\" & \" \" & \"Name\"");
            data.createCell(4).setCellValue("Normal");
            
            // Evaluate formulas before saving
            FormulaEvaluator evaluator = wb.getCreationHelper().createFormulaEvaluator();
            evaluator.evaluateAll();
            
            try (FileOutputStream fos = new FileOutputStream(file)) {
                wb.write(fos);
            }
        }
    }
}
