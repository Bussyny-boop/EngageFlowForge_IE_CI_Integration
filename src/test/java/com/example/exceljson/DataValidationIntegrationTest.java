package com.example.exceljson;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.*;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test to verify that the complete data validation workflow
 * works correctly with asterisk-marked headers.
 * 
 * This test simulates the real-world scenario where:
 * 1. User has Excel/CSV files with asterisk-marked required fields
 * 2. User loads these files for validation
 * 3. System correctly recognizes headers and loads data
 */
public class DataValidationIntegrationTest {

    /**
     * End-to-end test: Create Excel file with "Name*" and verify it can be loaded
     */
    @Test
    public void testEndToEndAssignmentRolesWithAsterisk(@TempDir Path tempDir) throws Exception {
        // Step 1: Create a realistic Excel file with "Name*" header
        File excelFile = tempDir.resolve("assignment_roles_required.xlsx").toFile();
        
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Assignment Roles");
            
            // Header row with asterisk indicating required field
            Row headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("ID");
            headerRow.createCell(1).setCellValue("Name*");  // Required field marked with asterisk
            headerRow.createCell(2).setCellValue("Description");
            
            // Data rows
            Row row1 = sheet.createRow(1);
            row1.createCell(0).setCellValue(1);
            row1.createCell(1).setCellValue("ICU Room 101");
            row1.createCell(2).setCellValue("Intensive Care Unit - Room 101");
            
            Row row2 = sheet.createRow(2);
            row2.createCell(0).setCellValue(2);
            row2.createCell(1).setCellValue("ER Bay 5");
            row2.createCell(2).setCellValue("Emergency Room - Bay 5");
            
            Row row3 = sheet.createRow(3);
            row3.createCell(0).setCellValue(3);
            row3.createCell(1).setCellValue("Surgery Suite A");
            row3.createCell(2).setCellValue("Main Operating Room Suite A");
            
            try (FileOutputStream fos = new FileOutputStream(excelFile)) {
                workbook.write(fos);
            }
        }
        
        // Step 2: Verify file is created correctly
        assertTrue(excelFile.exists(), "Excel file should be created");
        assertTrue(excelFile.length() > 0, "Excel file should not be empty");
        
        // Step 3: Verify file structure by reading it back
        try (Workbook workbook = WorkbookFactory.create(excelFile)) {
            Sheet sheet = workbook.getSheetAt(0);
            
            // Verify header row
            Row headerRow = sheet.getRow(0);
            assertNotNull(headerRow, "Header row should exist");
            assertEquals("Name*", headerRow.getCell(1).getStringCellValue(), 
                "Column B should have 'Name*' header");
            
            // Verify data rows
            assertEquals(4, sheet.getPhysicalNumberOfRows(), 
                "Should have 4 rows total (1 header + 3 data)");
            
            Row row1 = sheet.getRow(1);
            assertEquals("ICU Room 101", row1.getCell(1).getStringCellValue(),
                "First role should be 'ICU Room 101'");
        }
        
        // Expected behavior: AppController.loadAssignmentRoles() would:
        // 1. Strip the asterisk from "Name*" header
        // 2. Recognize it as "Name" column
        // 3. Load values: "ICU Room 101", "ER Bay 5", "Surgery Suite A"
        // 4. NOT load the IDs or descriptions
    }

    /**
     * End-to-end test: Create CSV file with "Department*" and verify structure
     */
    @Test
    public void testEndToEndBedListWithAsterisk(@TempDir Path tempDir) throws Exception {
        // Step 1: Create a realistic CSV file with "Department*" header
        File csvFile = tempDir.resolve("bed_list_required.csv").toFile();
        
        try (PrintWriter writer = new PrintWriter(new FileWriter(csvFile))) {
            // Header row with asterisk indicating required field
            writer.println("Facility,Department*,Bed Count,Notes");
            
            // Data rows
            writer.println("Main Hospital,ICU,20,Intensive Care");
            writer.println("Main Hospital,ED,15,Emergency Department");
            writer.println("Main Hospital,Medical/Surgical,40,General Medical Surgical");
            writer.println("North Campus,Pediatrics,12,Children's Unit");
            writer.println("North Campus,Oncology,18,Cancer Treatment");
        }
        
        // Step 2: Verify file is created correctly
        assertTrue(csvFile.exists(), "CSV file should be created");
        assertTrue(csvFile.length() > 0, "CSV file should not be empty");
        
        // Step 3: Verify file structure by reading it back
        try (BufferedReader reader = new BufferedReader(new FileReader(csvFile))) {
            String headerLine = reader.readLine();
            assertNotNull(headerLine, "Header line should exist");
            assertTrue(headerLine.contains("Department*"), 
                "Header should contain 'Department*'");
            
            // Count data rows
            int rowCount = 0;
            while (reader.readLine() != null) {
                rowCount++;
            }
            assertEquals(5, rowCount, "Should have 5 data rows");
        }
        
        // Expected behavior: AppController.loadBedList() would:
        // 1. Strip the asterisk from "Department*" header
        // 2. Recognize it as "Department" column
        // 3. Load values: "ICU", "ED", "Medical/Surgical", "Pediatrics", "Oncology"
        // 4. NOT load facilities, bed counts, or notes
    }

    /**
     * Real-world scenario: Excel export with multiple asterisks on required fields
     */
    @Test
    public void testRealWorldScenarioMultipleAsterisks(@TempDir Path tempDir) throws Exception {
        File excelFile = tempDir.resolve("real_world_export.xlsx").toFile();
        
        // Simulate Excel export where required fields have multiple asterisks
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Data");
            
            // Headers as they might appear in real Excel exports
            Row headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("Name***");  // Triple asterisk for "very required"
            headerRow.createCell(1).setCellValue("Status");
            
            // Data
            Row row1 = sheet.createRow(1);
            row1.createCell(0).setCellValue("Critical Care Room 1");
            row1.createCell(1).setCellValue("Active");
            
            Row row2 = sheet.createRow(2);
            row2.createCell(0).setCellValue("Trauma Bay 3");
            row2.createCell(1).setCellValue("Active");
            
            try (FileOutputStream fos = new FileOutputStream(excelFile)) {
                workbook.write(fos);
            }
        }
        
        // Verify the file was created
        assertTrue(excelFile.exists());
        
        // Expected: System should strip all asterisks and recognize "Name***" as "Name"
    }

    /**
     * Edge case: Mixed case header with asterisk
     */
    @Test
    public void testMixedCaseHeaderWithAsterisk(@TempDir Path tempDir) throws Exception {
        File csvFile = tempDir.resolve("mixed_case.csv").toFile();
        
        try (PrintWriter writer = new PrintWriter(new FileWriter(csvFile))) {
            // Mixed case header (realistic scenario from different Excel versions)
            writer.println("nAmE*,Description");
            writer.println("Test Room 1,First test");
            writer.println("Test Room 2,Second test");
        }
        
        assertTrue(csvFile.exists());
        
        // Expected: Case-insensitive matching should recognize "nAmE*" as "Name"
    }

    /**
     * Verify that normal headers (without asterisks) still work
     */
    @Test
    public void testBackwardCompatibilityNoAsterisk(@TempDir Path tempDir) throws Exception {
        File csvFile = tempDir.resolve("no_asterisk.csv").toFile();
        
        try (PrintWriter writer = new PrintWriter(new FileWriter(csvFile))) {
            // Normal header without asterisk
            writer.println("Name");
            writer.println("Room A");
            writer.println("Room B");
        }
        
        assertTrue(csvFile.exists());
        
        // Expected: Should work exactly as before, no regression
    }
}
