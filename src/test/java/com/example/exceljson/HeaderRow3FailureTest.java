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
 * Test to reproduce the issue where headers in row 3 are not detected
 */
class HeaderRow3FailureTest {
    
    @Test
    void testHeaderInRow3WithFewNonEmptyCellsInEarlierRows(@TempDir Path tempDir) throws Exception {
        File excelFile = tempDir.resolve("header_row3_issue.xlsx").toFile();
        
        try (Workbook wb = new XSSFWorkbook()) {
            // Create Unit Breakdown sheet
            Sheet unitSheet = wb.createSheet("Unit Breakdown");
            
            // Row 1 (index 0) - has 1-2 non-empty cells (not enough to be considered header)
            Row row0 = unitSheet.createRow(0);
            row0.createCell(0).setCellValue("Some Title");
            
            // Row 2 (index 1) - has 1-2 non-empty cells
            Row row1 = unitSheet.createRow(1);
            row1.createCell(0).setCellValue("Subtitle");
            row1.createCell(1).setCellValue("Date: 2024");
            
            // Row 3 (index 2) - THE ACTUAL HEADER
            Row unitHeader = unitSheet.createRow(2);
            unitHeader.createCell(0).setCellValue("Facility");
            unitHeader.createCell(1).setCellValue("Common Unit Name");
            unitHeader.createCell(2).setCellValue("Nurse Call Configuration Group");
            
            Row unitData = unitSheet.createRow(3);
            unitData.createCell(0).setCellValue("Test Facility");
            unitData.createCell(1).setCellValue("Unit 1");
            unitData.createCell(2).setCellValue("Config Group 1");

            // Create Nurse call sheet with same pattern
            Sheet nurseSheet = wb.createSheet("Nurse call");
            
            // Rows 1-2 with partial data
            Row nRow0 = nurseSheet.createRow(0);
            nRow0.createCell(0).setCellValue("Nurse Call Configuration");
            
            Row nRow1 = nurseSheet.createRow(1);
            nRow1.createCell(0).setCellValue("Version 2.0");
            
            // Row 3 (index 2) - THE ACTUAL HEADER
            Row nurseHeader = nurseSheet.createRow(2);
            nurseHeader.createCell(0).setCellValue("Configuration Group");
            nurseHeader.createCell(1).setCellValue("Common Alert or Alarm Name");
            nurseHeader.createCell(2).setCellValue("Sending System Alert Name");
            nurseHeader.createCell(3).setCellValue("Priority");
            nurseHeader.createCell(4).setCellValue("Device - A");
            
            Row nurseData = nurseSheet.createRow(3);
            nurseData.createCell(0).setCellValue("Config Group 1");
            nurseData.createCell(1).setCellValue("Test Alarm");
            nurseData.createCell(2).setCellValue("Test Alarm");
            nurseData.createCell(3).setCellValue("Normal");
            nurseData.createCell(4).setCellValue("Edge");
            
            try (FileOutputStream fos = new FileOutputStream(excelFile)) {
                wb.write(fos);
            }
        }
        
        ExcelParserV5 parser = new ExcelParserV5();
        
        System.out.println("\n=== Testing file with header in row 3 (partial data in rows 1-2) ===");
        
        // This should work but might fail
        assertDoesNotThrow(() -> parser.load(excelFile), "Should load workbook with header in Row 3");
        
        // Verify data was loaded correctly
        System.out.println("Units loaded: " + parser.units.size());
        System.out.println("Nurse calls loaded: " + parser.nurseCalls.size());
        
        assertEquals(1, parser.units.size(), "Should have loaded 1 unit");
        assertEquals(1, parser.nurseCalls.size(), "Should have loaded 1 nurse call");
        assertEquals("Test Alarm", parser.nurseCalls.get(0).alarmName);
    }
}
