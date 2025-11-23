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
 * Tests for the updateExcel method to ensure it preserves formatting
 * while updating data.
 */
class UpdateExcelTest {

    @TempDir
    Path tempDir;

    @Test
    void testUpdateExcelPreservesFormatting() throws Exception {
        // Create a test Excel file with some formatting
        File testFile = tempDir.resolve("test.xlsx").toFile();
        
        // Create initial workbook with formatted cells
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
            
            // Create a data row with some formatting
            Row dataRow = unitSheet.createRow(1);
            Cell cell0 = dataRow.createCell(0);
            cell0.setCellValue("Original Facility");
            
            // Add bold formatting to the cell
            CellStyle boldStyle = wb.createCellStyle();
            Font boldFont = wb.createFont();
            boldFont.setBold(true);
            boldStyle.setFont(boldFont);
            cell0.setCellStyle(boldStyle);
            
            dataRow.createCell(1).setCellValue("Original Unit");
            dataRow.createCell(2).setCellValue("Original Nurse Group");
            dataRow.createCell(3).setCellValue("Original Clinical Group");
            dataRow.createCell(4).setCellValue("");
            dataRow.createCell(5).setCellValue("");
            dataRow.createCell(6).setCellValue("");
            
            // Save the workbook
            try (FileOutputStream fos = new FileOutputStream(testFile)) {
                wb.write(fos);
            }
        }
        
        // Load the file with ExcelParserV5
        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(testFile);
        
        // Verify initial data
        assertEquals(1, parser.units.size());
        assertEquals("Original Facility", parser.units.get(0).facility);
        
        // Modify the data
        parser.units.get(0).facility = "Updated Facility";
        parser.units.get(0).unitNames = "Updated Unit";
        parser.units.get(0).nurseGroup = "Updated Nurse Group";
        
        // Update the Excel file
        parser.updateExcel(testFile);
        
        // Verify the file was updated and formatting preserved
        try (FileInputStream fis = new FileInputStream(testFile);
             Workbook wb = WorkbookFactory.create(fis)) {
            Sheet sheet = wb.getSheet("Unit Breakdown");
            assertNotNull(sheet);
            
            Row dataRow = sheet.getRow(1);
            assertNotNull(dataRow);
            
            // Check that data was updated
            Cell cell0 = dataRow.getCell(0);
            assertEquals("Updated Facility", cell0.getStringCellValue());
            assertEquals("Updated Unit", dataRow.getCell(1).getStringCellValue());
            assertEquals("Updated Nurse Group", dataRow.getCell(2).getStringCellValue());
            
            // Verify formatting was preserved (cell should still be bold)
            CellStyle style = cell0.getCellStyle();
            assertNotNull(style);
            Font font = wb.getFontAt(style.getFontIndex());
            assertTrue(font.getBold(), "Cell formatting (bold) should be preserved");
        }
    }

    @Test
    void testUpdateExcelWithFlowRows() throws Exception {
        // Create a test Excel file with flow data
        File testFile = tempDir.resolve("test_flows.xlsx").toFile();
        
        // Create initial workbook
        try (Workbook wb = new XSSFWorkbook()) {
            // Create Unit sheet
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
            dataRow.createCell(0).setCellValue("Test Facility");
            dataRow.createCell(1).setCellValue("Unit 1");
            dataRow.createCell(2).setCellValue("Group A");
            dataRow.createCell(3).setCellValue("Group B");
            dataRow.createCell(4).setCellValue("");
            dataRow.createCell(5).setCellValue("");
            dataRow.createCell(6).setCellValue("");
            
            // Create Nurse Call sheet
            Sheet nurseSheet = wb.createSheet("Nurse call");
            Row nurseHeader = nurseSheet.createRow(0);
            String[] headers = {
                "In scope", "Configuration Group", "Common Alert or Alarm Name",
                "Sending System Alert Name", "Priority", "Device - A", "Device - B",
                "Ringtone Device - A", "Response Options", "Break Through DND",
                "Engage 6.6+: Escalate after all declines or 1 decline",
                "Engage/Edge Display Time (Time to Live) (Device - A)",
                "Genie Enunciation", "EMDAN Compliant? (Y/N)",
                "Time to 1st Recipient", "1st Recipient",
                "Time to 2nd Recipient", "2nd Recipient",
                "Time to 3rd Recipient", "3rd Recipient",
                "Time to 4th Recipient", "4th Recipient",
                "Time to 5th Recipient", "5th Recipient"
            };
            for (int i = 0; i < headers.length; i++) {
                nurseHeader.createCell(i).setCellValue(headers[i]);
            }
            
            Row nurseDataRow = nurseSheet.createRow(1);
            nurseDataRow.createCell(0).setCellValue("TRUE");
            nurseDataRow.createCell(1).setCellValue("Group A");
            nurseDataRow.createCell(2).setCellValue("Original Alarm");
            nurseDataRow.createCell(3).setCellValue("Original Sending");
            
            // Save the workbook
            try (FileOutputStream fos = new FileOutputStream(testFile)) {
                wb.write(fos);
            }
        }
        
        // Load the file
        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(testFile);
        
        // Verify initial data
        assertEquals(1, parser.nurseCalls.size());
        assertEquals("Original Alarm", parser.nurseCalls.get(0).alarmName);
        
        // Modify flow data
        parser.nurseCalls.get(0).alarmName = "Updated Alarm";
        parser.nurseCalls.get(0).sendingName = "Updated Sending";
        
        // Update the Excel file
        parser.updateExcel(testFile);
        
        // Verify the update
        try (FileInputStream fis = new FileInputStream(testFile);
             Workbook wb = WorkbookFactory.create(fis)) {
            Sheet nurseSheet = wb.getSheet("Nurse call");
            assertNotNull(nurseSheet);
            
            Row dataRow = nurseSheet.getRow(1);
            assertNotNull(dataRow);
            
            assertEquals("Updated Alarm", dataRow.getCell(2).getStringCellValue());
            assertEquals("Updated Sending", dataRow.getCell(3).getStringCellValue());
        }
    }
}
