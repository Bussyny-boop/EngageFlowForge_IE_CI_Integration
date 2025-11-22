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
 * Test to verify that the parser can detect headers in Row 1, Row 2, or Row 3
 * (0-indexed as 0, 1, 2) as per the requirement.
 */
class HeaderRowDetectionTest {

    @Test
    void testHeaderInRow1(@TempDir Path tempDir) throws Exception {
        File excelFile = tempDir.resolve("header_row1.xlsx").toFile();
        
        // Create workbook with header in row 1 (0-indexed as 0)
        createWorkbookWithHeaderAtRow(excelFile, 0);
        
        ExcelParserV5 parser = new ExcelParserV5();
        assertDoesNotThrow(() -> parser.load(excelFile), "Should load workbook with header in Row 1");
        
        // Verify data was loaded correctly
        assertEquals(1, parser.units.size(), "Should have loaded 1 unit");
        assertEquals(1, parser.nurseCalls.size(), "Should have loaded 1 nurse call");
        assertEquals("Test Alarm", parser.nurseCalls.getFirst().alarmName);
    }

    @Test
    void testHeaderInRow2(@TempDir Path tempDir) throws Exception {
        File excelFile = tempDir.resolve("header_row2.xlsx").toFile();
        
        // Create workbook with header in row 2 (0-indexed as 1)
        createWorkbookWithHeaderAtRow(excelFile, 1);
        
        ExcelParserV5 parser = new ExcelParserV5();
        assertDoesNotThrow(() -> parser.load(excelFile), "Should load workbook with header in Row 2");
        
        // Verify data was loaded correctly
        assertEquals(1, parser.units.size(), "Should have loaded 1 unit");
        assertEquals(1, parser.nurseCalls.size(), "Should have loaded 1 nurse call");
        assertEquals("Test Alarm", parser.nurseCalls.getFirst().alarmName);
    }

    @Test
    void testHeaderInRow3(@TempDir Path tempDir) throws Exception {
        File excelFile = tempDir.resolve("header_row3.xlsx").toFile();
        
        // Create workbook with header in row 3 (0-indexed as 2)
        createWorkbookWithHeaderAtRow(excelFile, 2);
        
        ExcelParserV5 parser = new ExcelParserV5();
        assertDoesNotThrow(() -> parser.load(excelFile), "Should load workbook with header in Row 3");
        
        // Verify data was loaded correctly
        assertEquals(1, parser.units.size(), "Should have loaded 1 unit");
        assertEquals(1, parser.nurseCalls.size(), "Should have loaded 1 nurse call");
        assertEquals("Test Alarm", parser.nurseCalls.getFirst().alarmName);
    }

    @Test
    void testHeaderInRow4NotPreferred(@TempDir Path tempDir) throws Exception {
        File excelFile = tempDir.resolve("header_row4.xlsx").toFile();
        
        // Create workbook with header in row 4 (0-indexed as 3)
        // This should still work as a fallback, but is not the primary search
        createWorkbookWithHeaderAtRow(excelFile, 3);
        
        ExcelParserV5 parser = new ExcelParserV5();
        assertDoesNotThrow(() -> parser.load(excelFile), "Should load workbook with header in Row 4 (fallback)");
        
        // Verify data was loaded correctly
        assertEquals(1, parser.units.size(), "Should have loaded 1 unit");
        assertEquals(1, parser.nurseCalls.size(), "Should have loaded 1 nurse call");
        assertEquals("Test Alarm", parser.nurseCalls.getFirst().alarmName);
    }

    @Test
    void testMultipleHeaderRows_UsesFirst(@TempDir Path tempDir) throws Exception {
        File excelFile = tempDir.resolve("multiple_headers.xlsx").toFile();
        
        // Create workbook with headers in both row 1 and row 3
        createWorkbookWithMultipleHeaders(excelFile);
        
        ExcelParserV5 parser = new ExcelParserV5();
        assertDoesNotThrow(() -> parser.load(excelFile), "Should load workbook and use first header found");
        
        // Verify it uses the first header (row 1)
        assertEquals(1, parser.units.size(), "Should have loaded 1 unit");
        // The parser will load all data rows after the header, including rows that look like headers
        // In this case, it loads: "First Header Alarm" (row 1), "Second header" row as data (row 2), "Second Header Alarm" (row 3)
        assertTrue(parser.nurseCalls.size() >= 1, "Should have loaded at least 1 nurse call");
        // Verify the first row is from the first header's data
        assertEquals("First Header Alarm", parser.nurseCalls.getFirst().alarmName);
    }

    // Helper methods to create test workbooks

    private void createWorkbookWithHeaderAtRow(File file, int headerRowIndex) throws Exception {
        try (Workbook wb = new XSSFWorkbook()) {
            // Create Unit Breakdown with header at specified row
            Sheet unitSheet = wb.createSheet("Unit Breakdown");
            Row unitHeader = unitSheet.createRow(headerRowIndex);
            unitHeader.createCell(0).setCellValue("Facility");
            unitHeader.createCell(1).setCellValue("Common Unit Name");
            unitHeader.createCell(2).setCellValue("Nurse Call Configuration Group");
            
            Row unitData = unitSheet.createRow(headerRowIndex + 1);
            unitData.createCell(0).setCellValue("Test Facility");
            unitData.createCell(1).setCellValue("Unit 1");
            unitData.createCell(2).setCellValue("Config Group 1");

            // Create Nurse call sheet with header at specified row
            Sheet nurseSheet = wb.createSheet("Nurse call");
            Row nurseHeader = nurseSheet.createRow(headerRowIndex);
            nurseHeader.createCell(0).setCellValue("Configuration Group");
            nurseHeader.createCell(1).setCellValue("Common Alert or Alarm Name");
            nurseHeader.createCell(2).setCellValue("Sending System Alert Name");
            nurseHeader.createCell(3).setCellValue("Priority");
            nurseHeader.createCell(4).setCellValue("Device - A");
            
            Row nurseData = nurseSheet.createRow(headerRowIndex + 1);
            nurseData.createCell(0).setCellValue("Config Group 1");
            nurseData.createCell(1).setCellValue("Test Alarm");
            nurseData.createCell(2).setCellValue("Test Alarm");
            nurseData.createCell(3).setCellValue("Normal");
            nurseData.createCell(4).setCellValue("Edge");
            
            try (FileOutputStream fos = new FileOutputStream(file)) {
                wb.write(fos);
            }
        }
    }

    private void createWorkbookWithMultipleHeaders(File file) throws Exception {
        try (Workbook wb = new XSSFWorkbook()) {
            // Create Unit Breakdown with header in row 1 (index 0)
            Sheet unitSheet = wb.createSheet("Unit Breakdown");
            Row unitHeader = unitSheet.createRow(0);
            unitHeader.createCell(0).setCellValue("Facility");
            unitHeader.createCell(1).setCellValue("Common Unit Name");
            unitHeader.createCell(2).setCellValue("Nurse Call Configuration Group");
            
            Row unitData = unitSheet.createRow(1);
            unitData.createCell(0).setCellValue("Test Facility");
            unitData.createCell(1).setCellValue("Unit 1");
            unitData.createCell(2).setCellValue("Config Group 1");

            // Create Nurse call sheet with headers in both row 1 and row 3
            Sheet nurseSheet = wb.createSheet("Nurse call");
            
            // First header at row 1 (index 0)
            Row nurseHeader1 = nurseSheet.createRow(0);
            nurseHeader1.createCell(0).setCellValue("Configuration Group");
            nurseHeader1.createCell(1).setCellValue("Common Alert or Alarm Name");
            nurseHeader1.createCell(2).setCellValue("Sending System Alert Name");
            nurseHeader1.createCell(3).setCellValue("Priority");
            nurseHeader1.createCell(4).setCellValue("Device - A");
            
            Row nurseData1 = nurseSheet.createRow(1);
            nurseData1.createCell(0).setCellValue("Config Group 1");
            nurseData1.createCell(1).setCellValue("First Header Alarm");
            nurseData1.createCell(2).setCellValue("First Header Alarm");
            nurseData1.createCell(3).setCellValue("Normal");
            nurseData1.createCell(4).setCellValue("Edge");
            
            // Second header at row 3 (index 2) - should be ignored
            Row nurseHeader2 = nurseSheet.createRow(2);
            nurseHeader2.createCell(0).setCellValue("Configuration Group");
            nurseHeader2.createCell(1).setCellValue("Common Alert or Alarm Name");
            
            Row nurseData2 = nurseSheet.createRow(3);
            nurseData2.createCell(0).setCellValue("Config Group 2");
            nurseData2.createCell(1).setCellValue("Second Header Alarm");
            
            try (FileOutputStream fos = new FileOutputStream(file)) {
                wb.write(fos);
            }
        }
    }
}
