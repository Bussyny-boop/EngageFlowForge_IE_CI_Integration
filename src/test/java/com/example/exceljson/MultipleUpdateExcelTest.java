package com.example.exceljson;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests to verify that multiple calls to updateExcel work correctly.
 * This test specifically validates the fix for the "Style does not belong to workbook" error
 * that occurred when cached styles from one workbook were applied to another workbook instance.
 */
class MultipleUpdateExcelTest {

    @TempDir
    Path tempDir;

    @Test
    void testMultipleUpdateExcelCallsWorkCorrectly() throws Exception {
        // Create a test Excel file
        File testFile = tempDir.resolve("test.xlsx").toFile();
        
        // Create initial workbook
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet unitSheet = wb.createSheet("Unit Breakdown");
            
            // Create header row
            Row headerRow = unitSheet.createRow(0);
            headerRow.createCell(0).setCellValue("Facility");
            headerRow.createCell(1).setCellValue("Common Unit Name");
            headerRow.createCell(2).setCellValue("Nurse Call Configuration Group");
            headerRow.createCell(3).setCellValue("Patient Monitoring Configuration Group");
            headerRow.createCell(4).setCellValue("Orders Configuration Group");
            headerRow.createCell(5).setCellValue("No Caregiver Group");
            headerRow.createCell(6).setCellValue("Comments");
            
            // Create a data row
            Row dataRow = unitSheet.createRow(1);
            dataRow.createCell(0).setCellValue("Facility 1");
            dataRow.createCell(1).setCellValue("Unit 1");
            dataRow.createCell(2).setCellValue("Group 1");
            dataRow.createCell(3).setCellValue("");
            dataRow.createCell(4).setCellValue("");
            dataRow.createCell(5).setCellValue("");
            dataRow.createCell(6).setCellValue("");
            
            // Save the workbook
            try (FileOutputStream fos = new FileOutputStream(testFile)) {
                wb.write(fos);
            }
        }
        
        // FIRST UPDATE
        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(testFile);
        
        ExcelParserV5.UnitRow unit = parser.units.get(0);
        unit.facility = "Updated Facility 1";
        unit.changedFields.add("facility");
        
        // First call to updateExcel - this creates the cached styles
        parser.updateExcel(testFile);
        
        // Verify first update worked
        try (FileInputStream fis = new FileInputStream(testFile);
             Workbook wb = WorkbookFactory.create(fis)) {
            Sheet sheet = wb.getSheet("Unit Breakdown");
            Row dataRow = sheet.getRow(1);
            assertEquals("Updated Facility 1", dataRow.getCell(0).getStringCellValue());
        }
        
        // SECOND UPDATE - This would fail with the old code due to cached styles
        // Load the file again (creates a NEW workbook instance)
        ExcelParserV5 parser2 = new ExcelParserV5();
        parser2.load(testFile);
        
        ExcelParserV5.UnitRow unit2 = parser2.units.get(0);
        unit2.facility = "Updated Facility 2";
        unit2.unitNames = "Updated Unit 2";
        unit2.changedFields.add("facility");
        unit2.changedFields.add("unitNames");
        
        // Second call to updateExcel - with the bug, this would throw:
        // "This Style does not belong to the supplied Workbook Styles Source"
        // because it would try to apply cached styles from the first workbook
        assertDoesNotThrow(() -> parser2.updateExcel(testFile),
            "Second updateExcel call should not throw style mismatch error");
        
        // Verify second update worked
        try (FileInputStream fis = new FileInputStream(testFile);
             Workbook wb = WorkbookFactory.create(fis)) {
            Sheet sheet = wb.getSheet("Unit Breakdown");
            Row dataRow = sheet.getRow(1);
            assertEquals("Updated Facility 2", dataRow.getCell(0).getStringCellValue());
            assertEquals("Updated Unit 2", dataRow.getCell(1).getStringCellValue());
            
            // Verify formatting was applied (bold, italic, red)
            Cell cell0 = dataRow.getCell(0);
            CellStyle style = cell0.getCellStyle();
            Font font = wb.getFontAt(style.getFontIndex());
            assertTrue(font.getBold(), "Changed cells should be bold");
            assertTrue(font.getItalic(), "Changed cells should be italic");
            assertEquals(10, font.getColor(), "Changed cells should be red");
        }
        
        // THIRD UPDATE - Even more stress testing
        ExcelParserV5 parser3 = new ExcelParserV5();
        parser3.load(testFile);
        
        ExcelParserV5.UnitRow unit3 = parser3.units.get(0);
        unit3.facility = "Updated Facility 3";
        unit3.changedFields.add("facility");
        
        assertDoesNotThrow(() -> parser3.updateExcel(testFile),
            "Third updateExcel call should not throw style mismatch error");
        
        // Verify third update worked
        try (FileInputStream fis = new FileInputStream(testFile);
             Workbook wb = WorkbookFactory.create(fis)) {
            Sheet sheet = wb.getSheet("Unit Breakdown");
            Row dataRow = sheet.getRow(1);
            assertEquals("Updated Facility 3", dataRow.getCell(0).getStringCellValue());
        }
    }
    
    @Test
    void testMultipleUpdateExcelWithDifferentFiles() throws Exception {
        // This test ensures that styles are properly managed when working with multiple different files
        File testFile1 = tempDir.resolve("test1.xlsx").toFile();
        File testFile2 = tempDir.resolve("test2.xlsx").toFile();
        
        // Create two similar Excel files
        for (File file : new File[]{testFile1, testFile2}) {
            try (Workbook wb = new XSSFWorkbook()) {
                Sheet unitSheet = wb.createSheet("Unit Breakdown");
                
                Row headerRow = unitSheet.createRow(0);
                headerRow.createCell(0).setCellValue("Facility");
                headerRow.createCell(1).setCellValue("Common Unit Name");
                headerRow.createCell(2).setCellValue("Nurse Call Configuration Group");
                headerRow.createCell(3).setCellValue("Patient Monitoring Configuration Group");
                headerRow.createCell(4).setCellValue("Orders Configuration Group");
                headerRow.createCell(5).setCellValue("No Caregiver Group");
                headerRow.createCell(6).setCellValue("Comments");
                
                Row dataRow = unitSheet.createRow(1);
                dataRow.createCell(0).setCellValue("Facility A");
                dataRow.createCell(1).setCellValue("Unit A");
                dataRow.createCell(2).setCellValue("");
                dataRow.createCell(3).setCellValue("");
                dataRow.createCell(4).setCellValue("");
                dataRow.createCell(5).setCellValue("");
                dataRow.createCell(6).setCellValue("");
                
                try (FileOutputStream fos = new FileOutputStream(file)) {
                    wb.write(fos);
                }
            }
        }
        
        // Update first file
        ExcelParserV5 parser1 = new ExcelParserV5();
        parser1.load(testFile1);
        parser1.units.get(0).facility = "Updated File 1";
        parser1.units.get(0).changedFields.add("facility");
        parser1.updateExcel(testFile1);
        
        // Update second file - this should work without style errors
        ExcelParserV5 parser2 = new ExcelParserV5();
        parser2.load(testFile2);
        parser2.units.get(0).facility = "Updated File 2";
        parser2.units.get(0).changedFields.add("facility");
        
        assertDoesNotThrow(() -> parser2.updateExcel(testFile2),
            "Updating different file should not cause style mismatch error");
        
        // Verify both files were updated correctly
        try (FileInputStream fis = new FileInputStream(testFile1);
             Workbook wb = WorkbookFactory.create(fis)) {
            Sheet sheet = wb.getSheet("Unit Breakdown");
            assertEquals("Updated File 1", sheet.getRow(1).getCell(0).getStringCellValue());
        }
        
        try (FileInputStream fis = new FileInputStream(testFile2);
             Workbook wb = WorkbookFactory.create(fis)) {
            Sheet sheet = wb.getSheet("Unit Breakdown");
            assertEquals("Updated File 2", sheet.getRow(1).getCell(0).getStringCellValue());
        }
    }
}
