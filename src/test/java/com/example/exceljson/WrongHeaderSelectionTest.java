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
 * Test to verify that the parser selects the CORRECT header when there are
 * multiple rows with 3+ non-empty cells
 */
class WrongHeaderSelectionTest {
    
    @Test
    void testDoesNotSelectWrongRowAsHeader(@TempDir Path tempDir) throws Exception {
        File excelFile = tempDir.resolve("wrong_header.xlsx").toFile();
        
        try (Workbook wb = new XSSFWorkbook()) {
            // Create Unit Breakdown sheet
            Sheet unitSheet = wb.createSheet("Unit Breakdown");
            
            // Row 1 (index 0) - has 3 non-empty cells but NOT the header
            Row row0 = unitSheet.createRow(0);
            row0.createCell(0).setCellValue("Report Title");
            row0.createCell(1).setCellValue("Generated:");
            row0.createCell(2).setCellValue("2024-01-01");
            
            // Row 2 (index 1) - has 3 non-empty cells but NOT the header
            Row row1 = unitSheet.createRow(1);
            row1.createCell(0).setCellValue("Department");
            row1.createCell(1).setCellValue("Location");
            row1.createCell(2).setCellValue("Date");
            
            // Row 3 (index 2) - THE ACTUAL HEADER with expected column names
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
            
            // Rows 1-2 with 3+ cells each
            Row nRow0 = nurseSheet.createRow(0);
            nRow0.createCell(0).setCellValue("Nurse Call Configuration");
            nRow0.createCell(1).setCellValue("Version");
            nRow0.createCell(2).setCellValue("2.0");
            
            Row nRow1 = nurseSheet.createRow(1);
            nRow1.createCell(0).setCellValue("Some");
            nRow1.createCell(1).setCellValue("Other");
            nRow1.createCell(2).setCellValue("Data");
            
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
        
        System.out.println("\n=== Testing file where rows 0-2 all have 3+ cells ===");
        
        // This will likely select row 0 as the header (wrong!) because it has 3+ non-empty cells
        assertDoesNotThrow(() -> parser.load(excelFile), "Should load workbook");
        
        System.out.println("Units loaded: " + parser.units.size());
        System.out.println("Nurse calls loaded: " + parser.nurseCalls.size());
        
        if (parser.units.size() > 0) {
            System.out.println("First unit facility: '" + parser.units.get(0).facility + "'");
            System.out.println("Expected: 'Test Facility', Got: '" + parser.units.get(0).facility + "'");
        }
        
        // This should now work correctly with the smart header detection
        assertEquals(1, parser.units.size(), "Should have loaded 1 unit");
        assertEquals("Test Facility", parser.units.get(0).facility);
        assertEquals(1, parser.nurseCalls.size(), "Should have loaded 1 nurse call");
        assertEquals("Test Alarm", parser.nurseCalls.get(0).alarmName);
    }
}
