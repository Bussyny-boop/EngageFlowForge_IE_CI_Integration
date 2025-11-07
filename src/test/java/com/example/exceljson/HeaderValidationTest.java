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
 * Test to verify that the parser validates required headers and provides
 * helpful error messages when headers are missing.
 */
class HeaderValidationTest {

    @Test
    void testMissingAlarmNameHeaderThrowsException(@TempDir Path tempDir) throws Exception {
        File excelFile = tempDir.resolve("missing_alarm_name.xlsx").toFile();
        
        // Create workbook without Alarm Name header (which is required)
        createWorkbookWithoutAlarmName(excelFile);
        
        ExcelParserV5 parser = new ExcelParserV5();
        Exception exception = assertThrows(Exception.class, () -> {
            parser.load(excelFile);
        });
        
        // Verify error message mentions the missing header
        String message = exception.getMessage();
        assertTrue(message.contains("Common Alert or Alarm Name"), 
            "Error message should mention 'Common Alert or Alarm Name' as it's a required header");
        assertTrue(message.contains("Missing required header") || message.contains("Invalid Excel file"),
            "Error message should indicate missing headers");
    }

    @Test
    void testMissingUnitBreakdownHeadersThrowsException(@TempDir Path tempDir) throws Exception {
        File excelFile = tempDir.resolve("missing_unit_headers.xlsx").toFile();
        
        // Create workbook with incomplete Unit Breakdown headers
        createWorkbookWithIncompleteUnitHeaders(excelFile);
        
        ExcelParserV5 parser = new ExcelParserV5();
        Exception exception = assertThrows(Exception.class, () -> {
            parser.load(excelFile);
        });
        
        String message = exception.getMessage();
        assertTrue(message.contains("Common Unit Name"), 
            "Error message should mention the missing 'Common Unit Name' header");
        assertTrue(message.contains("Unit Breakdown"),
            "Error message should mention the sheet name");
    }

    @Test
    void testMissingConfigGroupHeaderThrowsException(@TempDir Path tempDir) throws Exception {
        File excelFile = tempDir.resolve("missing_config_group.xlsx").toFile();
        
        // Create workbook without Configuration Group header
        createWorkbookWithoutConfigGroup(excelFile);
        
        ExcelParserV5 parser = new ExcelParserV5();
        Exception exception = assertThrows(Exception.class, () -> {
            parser.load(excelFile);
        });
        
        String message = exception.getMessage();
        assertTrue(message.contains("Configuration Group"), 
            "Error message should mention 'Configuration Group'");
    }

    @Test
    void testMissingPriorityHeaderDoesNotThrowException(@TempDir Path tempDir) throws Exception {
        File excelFile = tempDir.resolve("missing_priority.xlsx").toFile();
        
        // Create workbook without Priority header (it's optional now)
        createWorkbookWithoutPriority(excelFile);
        
        ExcelParserV5 parser = new ExcelParserV5();
        // Should NOT throw an exception since Priority is optional
        assertDoesNotThrow(() -> parser.load(excelFile));
    }

    @Test
    void testValidWorkbookLoadsSuccessfully(@TempDir Path tempDir) throws Exception {
        File excelFile = tempDir.resolve("valid_workbook.xlsx").toFile();
        
        // Create a complete, valid workbook
        createValidWorkbook(excelFile);
        
        ExcelParserV5 parser = new ExcelParserV5();
        // Should not throw any exception
        assertDoesNotThrow(() -> parser.load(excelFile));
        
        // Verify data was loaded
        assertEquals(1, parser.units.size(), "Should have loaded 1 unit");
        assertEquals(1, parser.nurseCalls.size(), "Should have loaded 1 nurse call");
    }

    @Test
    void testNoHeaderRowThrowsException(@TempDir Path tempDir) throws Exception {
        File excelFile = tempDir.resolve("no_headers.xlsx").toFile();
        
        // Create workbook with no header row at all
        createWorkbookWithNoHeaders(excelFile);
        
        ExcelParserV5 parser = new ExcelParserV5();
        Exception exception = assertThrows(Exception.class, () -> {
            parser.load(excelFile);
        });
        
        String message = exception.getMessage();
        assertTrue(message.contains("No headers found") || message.contains("Missing required header"),
            "Error message should indicate no headers were found");
    }

    // Helper methods to create test workbooks

    private void createWorkbookWithoutAlarmName(File file) throws Exception {
        try (Workbook wb = new XSSFWorkbook()) {
            // Unit Breakdown (valid)
            createValidUnitSheet(wb);
            
            // Nurse call sheet WITHOUT Common Alert or Alarm Name
            Sheet nurseSheet = wb.createSheet("Nurse call");
            Row nurseHeader = nurseSheet.createRow(0);
            nurseHeader.createCell(0).setCellValue("Configuration Group");
            // Missing: Common Alert or Alarm Name
            nurseHeader.createCell(1).setCellValue("Priority");
            nurseHeader.createCell(2).setCellValue("Device - A");
            
            try (FileOutputStream fos = new FileOutputStream(file)) {
                wb.write(fos);
            }
        }
    }

    private void createWorkbookWithIncompleteUnitHeaders(File file) throws Exception {
        try (Workbook wb = new XSSFWorkbook()) {
            // Unit Breakdown with missing "Common Unit Name"
            Sheet unitSheet = wb.createSheet("Unit Breakdown");
            Row unitHeader = unitSheet.createRow(0);
            unitHeader.createCell(0).setCellValue("Facility");
            // Missing: Common Unit Name
            
            try (FileOutputStream fos = new FileOutputStream(file)) {
                wb.write(fos);
            }
        }
    }

    private void createWorkbookWithoutConfigGroup(File file) throws Exception {
        try (Workbook wb = new XSSFWorkbook()) {
            createValidUnitSheet(wb);
            
            // Nurse call sheet without Configuration Group
            Sheet nurseSheet = wb.createSheet("Nurse call");
            Row nurseHeader = nurseSheet.createRow(0);
            // Missing: Configuration Group
            nurseHeader.createCell(0).setCellValue("Common Alert or Alarm Name");
            nurseHeader.createCell(1).setCellValue("Priority");
            nurseHeader.createCell(2).setCellValue("Device - A");
            
            try (FileOutputStream fos = new FileOutputStream(file)) {
                wb.write(fos);
            }
        }
    }

    private void createWorkbookWithoutPriority(File file) throws Exception {
        try (Workbook wb = new XSSFWorkbook()) {
            createValidUnitSheet(wb);
            
            // Nurse call sheet without Priority
            Sheet nurseSheet = wb.createSheet("Nurse call");
            Row nurseHeader = nurseSheet.createRow(0);
            nurseHeader.createCell(0).setCellValue("Configuration Group");
            nurseHeader.createCell(1).setCellValue("Common Alert or Alarm Name");
            // Missing: Priority
            nurseHeader.createCell(2).setCellValue("Device - A");
            
            try (FileOutputStream fos = new FileOutputStream(file)) {
                wb.write(fos);
            }
        }
    }

    private void createWorkbookWithNoHeaders(File file) throws Exception {
        try (Workbook wb = new XSSFWorkbook()) {
            // Empty Unit Breakdown sheet
            Sheet unitSheet = wb.createSheet("Unit Breakdown");
            // No header row, just empty
            
            try (FileOutputStream fos = new FileOutputStream(file)) {
                wb.write(fos);
            }
        }
    }

    private void createValidWorkbook(File file) throws Exception {
        try (Workbook wb = new XSSFWorkbook()) {
            // Valid Unit Breakdown
            createValidUnitSheet(wb);
            
            // Valid Nurse call sheet
            Sheet nurseSheet = wb.createSheet("Nurse call");
            Row nurseHeader = nurseSheet.createRow(0);
            nurseHeader.createCell(0).setCellValue("Configuration Group");
            nurseHeader.createCell(1).setCellValue("Common Alert or Alarm Name");
            nurseHeader.createCell(2).setCellValue("Sending System Alert Name");
            nurseHeader.createCell(3).setCellValue("Priority");
            nurseHeader.createCell(4).setCellValue("Device - A");
            nurseHeader.createCell(5).setCellValue("Device - B");
            
            Row nurseData = nurseSheet.createRow(1);
            nurseData.createCell(0).setCellValue("Config Group 1");
            nurseData.createCell(1).setCellValue("Test Alarm");
            nurseData.createCell(2).setCellValue("Test Alarm");
            nurseData.createCell(3).setCellValue("Normal");
            nurseData.createCell(4).setCellValue("Edge");
            nurseData.createCell(5).setCellValue("VCS");
            
            try (FileOutputStream fos = new FileOutputStream(file)) {
                wb.write(fos);
            }
        }
    }

    private void createValidUnitSheet(Workbook wb) {
        Sheet unitSheet = wb.createSheet("Unit Breakdown");
        Row unitHeader = unitSheet.createRow(0);
        unitHeader.createCell(0).setCellValue("Facility");
        unitHeader.createCell(1).setCellValue("Common Unit Name");
        unitHeader.createCell(2).setCellValue("Nurse Call Configuration Group");
        
        Row unitData = unitSheet.createRow(1);
        unitData.createCell(0).setCellValue("Test Facility");
        unitData.createCell(1).setCellValue("Unit 1");
        unitData.createCell(2).setCellValue("Config Group 1");
    }
}
