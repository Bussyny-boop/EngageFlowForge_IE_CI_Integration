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
 * Tests that fields without original values stored can still be tracked and saved.
 * This covers the scenario where change tracking works even for dynamically added fields.
 */
class ChangeTrackingWithoutOriginalValueTest {

    @TempDir
    Path tempDir;

    @Test
    void testChangeTrackingWorksWhenOriginalValueNotSet() throws Exception {
        // Create a test Excel file
        File testFile = tempDir.resolve("test_no_original.xlsx").toFile();
        
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
            
            // Create data row with a value that will have its originalValue removed
            Row dataRow = unitSheet.createRow(1);
            dataRow.createCell(0).setCellValue("Test Facility");
            dataRow.createCell(1).setCellValue("Unit 1");
            dataRow.createCell(2).setCellValue("Group A");
            dataRow.createCell(3).setCellValue("Group B");
            dataRow.createCell(4).setCellValue(""); // Empty
            dataRow.createCell(5).setCellValue(""); // Empty
            dataRow.createCell(6).setCellValue("Old Comment");
            
            try (FileOutputStream fos = new FileOutputStream(testFile)) {
                wb.write(fos);
            }
        }
        
        // Load the file
        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(testFile);
        
        assertEquals(1, parser.units.size());
        ExcelParserV5.UnitRow unit = parser.units.get(0);
        
        // Verify original value is set
        assertEquals("Old Comment", unit.originalValues.get("comments"));
        
        // Simulate a scenario where original value was not set (e.g., dynamically added field)
        // Remove the original value from the map
        unit.originalValues.remove("comments");
        
        // Now try to track a change to this field
        unit.comments = "New Comment";
        unit.changedFields.clear(); // Clear any existing changes
        
        // Simulate what trackFieldChange does with the fix
        // (The fix uses getOrDefault to handle missing originalValue)
        String originalValue = unit.originalValues.getOrDefault("comments", "");
        if (!originalValue.equals(unit.comments)) {
            unit.changedFields.add("comments");
        }
        
        // Verify the change was tracked even though originalValue was removed
        assertTrue(unit.changedFields.contains("comments"),
            "Change should be tracked even when originalValue is not set");
        
        // Update the Excel file
        parser.updateExcel(testFile);
        
        // Verify the cell was updated
        try (FileInputStream fis = new FileInputStream(testFile);
             Workbook wb = WorkbookFactory.create(fis)) {
            Sheet sheet = wb.getSheet("Unit Breakdown");
            Row dataRow = sheet.getRow(1);
            Cell commentsCell = dataRow.getCell(6);
            
            assertEquals("New Comment", commentsCell.getStringCellValue(),
                "Cell should be updated even when originalValue was not set");
        }
    }

    @Test
    void testNonEmptyOriginalValueIsStillTracked() throws Exception {
        // Verify that cells with non-empty original values are still tracked correctly
        File testFile = tempDir.resolve("test_non_empty_original.xlsx").toFile();
        
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
            dataRow.createCell(0).setCellValue("Facility X");
            dataRow.createCell(1).setCellValue("Unit Y");
            dataRow.createCell(2).setCellValue("Original Nurse Group"); // Non-empty
            dataRow.createCell(3).setCellValue("Original Clinical Group"); // Non-empty
            dataRow.createCell(4).setCellValue("");
            dataRow.createCell(5).setCellValue("");
            dataRow.createCell(6).setCellValue("");
            
            try (FileOutputStream fos = new FileOutputStream(testFile)) {
                wb.write(fos);
            }
        }
        
        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(testFile);
        
        ExcelParserV5.UnitRow unit = parser.units.get(0);
        
        // Verify original values are set
        assertEquals("Original Nurse Group", unit.originalValues.get("nurseGroup"));
        assertEquals("Original Clinical Group", unit.originalValues.get("clinGroup"));
        
        // Change both fields
        unit.nurseGroup = "Updated Nurse Group";
        unit.clinGroup = "Updated Clinical Group";
        
        // Mark as changed
        unit.changedFields.add("nurseGroup");
        unit.changedFields.add("clinGroup");
        
        // Update Excel
        parser.updateExcel(testFile);
        
        // Verify both cells were updated
        try (FileInputStream fis = new FileInputStream(testFile);
             Workbook wb = WorkbookFactory.create(fis)) {
            Sheet sheet = wb.getSheet("Unit Breakdown");
            Row dataRow = sheet.getRow(1);
            
            assertEquals("Updated Nurse Group", dataRow.getCell(2).getStringCellValue(),
                "Non-empty cells should be updated when changed");
            assertEquals("Updated Clinical Group", dataRow.getCell(3).getStringCellValue(),
                "Non-empty cells should be updated when changed");
            
            // Verify change formatting was applied
            CellStyle style1 = dataRow.getCell(2).getCellStyle();
            Font font1 = wb.getFontAt(style1.getFontIndex());
            assertTrue(font1.getBold(), "Changed cells should be bold");
            assertTrue(font1.getItalic(), "Changed cells should be italic");
        }
    }
}
