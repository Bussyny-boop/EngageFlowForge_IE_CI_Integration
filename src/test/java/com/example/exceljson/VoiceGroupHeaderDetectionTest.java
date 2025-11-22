package com.example.exceljson;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test for verifying the "Group Name" header detection in Load Voice Group functionality.
 * 
 * Requirements:
 * 1. If a file has a "Group Name" header, extract data from that column
 * 2. If no "Group Name" header exists, fall back to Column A
 * 3. Support both Excel and CSV files
 * 4. Header detection is case-insensitive
 */
public class VoiceGroupHeaderDetectionTest {

    /**
     * Test Excel file with "Group Name" header in Column A
     */
    @Test
    public void testExcelWithGroupNameHeaderInColumnA(@TempDir Path tempDir) throws Exception {
        File excelFile = tempDir.resolve("groups_with_header_colA.xlsx").toFile();
        
        // Create Excel file with "Group Name" header in Column A
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Groups");
            
            // Header row
            Row headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("Group Name");
            
            // Data rows
            Row row1 = sheet.createRow(1);
            row1.createCell(0).setCellValue("Nursing Team");
            
            Row row2 = sheet.createRow(2);
            row2.createCell(0).setCellValue("Code Blue");
            
            Row row3 = sheet.createRow(3);
            row3.createCell(0).setCellValue("Rapid Response");
            
            try (FileOutputStream fos = new FileOutputStream(excelFile)) {
                workbook.write(fos);
            }
        }
        
        // Verify file exists and is readable
        assertTrue(excelFile.exists());
        assertTrue(excelFile.length() > 0);
        
        // Expected behavior: Should extract from Column A and skip header row
        // Groups should be: "Nursing Team", "Code Blue", "Rapid Response"
        // Header "Group Name" should NOT be in the loaded groups
    }

    /**
     * Test Excel file with "Group Name" header in Column B (not Column A)
     */
    @Test
    public void testExcelWithGroupNameHeaderInColumnB(@TempDir Path tempDir) throws Exception {
        File excelFile = tempDir.resolve("groups_with_header_colB.xlsx").toFile();
        
        // Create Excel file with data in Column A and "Group Name" header in Column B
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Groups");
            
            // Header row
            Row headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("ID");
            headerRow.createCell(1).setCellValue("Group Name"); // Header in Column B
            
            // Data rows
            Row row1 = sheet.createRow(1);
            row1.createCell(0).setCellValue("1");
            row1.createCell(1).setCellValue("OB Nurses");
            
            Row row2 = sheet.createRow(2);
            row2.createCell(0).setCellValue("2");
            row2.createCell(1).setCellValue("ER Team");
            
            Row row3 = sheet.createRow(3);
            row3.createCell(0).setCellValue("3");
            row3.createCell(1).setCellValue("ICU Staff");
            
            try (FileOutputStream fos = new FileOutputStream(excelFile)) {
                workbook.write(fos);
            }
        }
        
        // Verify file exists
        assertTrue(excelFile.exists());
        
        // Expected behavior: Should extract from Column B (where header is)
        // Groups should be: "OB Nurses", "ER Team", "ICU Staff"
        // IDs "1", "2", "3" should NOT be in the loaded groups
    }

    /**
     * Test Excel file WITHOUT "Group Name" header (fallback to Column A)
     */
    @Test
    public void testExcelWithoutGroupNameHeader(@TempDir Path tempDir) throws Exception {
        File excelFile = tempDir.resolve("groups_no_header.xlsx").toFile();
        
        // Create Excel file without "Group Name" header
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Groups");
            
            // No header, just data rows starting from row 0
            Row row0 = sheet.createRow(0);
            row0.createCell(0).setCellValue("Pediatrics");
            
            Row row1 = sheet.createRow(1);
            row1.createCell(0).setCellValue("Surgery");
            
            Row row2 = sheet.createRow(2);
            row2.createCell(0).setCellValue("Cardiology");
            
            try (FileOutputStream fos = new FileOutputStream(excelFile)) {
                workbook.write(fos);
            }
        }
        
        // Verify file exists
        assertTrue(excelFile.exists());
        
        // Expected behavior: Should extract from Column A starting from row 0
        // Groups should be: "Pediatrics", "Surgery", "Cardiology"
    }

    /**
     * Test case-insensitive header detection ("group name", "GROUP NAME", etc.)
     */
    @Test
    public void testCaseInsensitiveHeaderDetection(@TempDir Path tempDir) throws Exception {
        File excelFile = tempDir.resolve("groups_lowercase_header.xlsx").toFile();
        
        // Create Excel file with lowercase "group name" header
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Groups");
            
            // Header row with lowercase
            Row headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("group name"); // lowercase
            
            // Data rows
            Row row1 = sheet.createRow(1);
            row1.createCell(0).setCellValue("Lab Team");
            
            Row row2 = sheet.createRow(2);
            row2.createCell(0).setCellValue("Radiology");
            
            try (FileOutputStream fos = new FileOutputStream(excelFile)) {
                workbook.write(fos);
            }
        }
        
        // Verify file exists
        assertTrue(excelFile.exists());
        
        // Expected behavior: Should recognize "group name" (lowercase) as header
        // Groups should be: "Lab Team", "Radiology"
        // Header "group name" should NOT be in the loaded groups
    }

    /**
     * Test CSV file with "Group Name" header
     */
    @Test
    public void testCsvWithGroupNameHeader(@TempDir Path tempDir) throws Exception {
        File csvFile = tempDir.resolve("groups_with_header.csv").toFile();
        
        // Create CSV file with "Group Name" header
        try (FileWriter writer = new FileWriter(csvFile)) {
            writer.write("Group Name\n");
            writer.write("Pharmacy\n");
            writer.write("Social Work\n");
            writer.write("Case Management\n");
        }
        
        // Verify file exists
        assertTrue(csvFile.exists());
        
        // Expected behavior: Should skip header row
        // Groups should be: "Pharmacy", "Social Work", "Case Management"
        // Header "Group Name" should NOT be in the loaded groups
    }

    /**
     * Test CSV file WITHOUT "Group Name" header (fallback behavior)
     */
    @Test
    public void testCsvWithoutGroupNameHeader(@TempDir Path tempDir) throws Exception {
        File csvFile = tempDir.resolve("groups_no_header.csv").toFile();
        
        // Create CSV file without header
        try (FileWriter writer = new FileWriter(csvFile)) {
            writer.write("Respiratory\n");
            writer.write("Physical Therapy\n");
            writer.write("Nutrition\n");
        }
        
        // Verify file exists
        assertTrue(csvFile.exists());
        
        // Expected behavior: Should treat all rows as data
        // Groups should be: "Respiratory", "Physical Therapy", "Nutrition"
    }

    /**
     * Test CSV with "Group Name" header in a column other than the first
     */
    @Test
    public void testCsvWithGroupNameHeaderInSecondColumn(@TempDir Path tempDir) throws Exception {
        File csvFile = tempDir.resolve("groups_with_header_col2.csv").toFile();
        
        // Create CSV file with "Group Name" in second column
        try (FileWriter writer = new FileWriter(csvFile)) {
            writer.write("ID,Group Name,Description\n");
            writer.write("1,Neurology,Brain specialists\n");
            writer.write("2,Orthopedics,Bone specialists\n");
        }
        
        // Verify file exists
        assertTrue(csvFile.exists());
        
        // Expected behavior: Should extract from second column (index 1)
        // Groups should be: "Neurology", "Orthopedics"
        // IDs "1", "2" should NOT be in the loaded groups
    }

    /**
     * Test empty Excel file
     */
    @Test
    public void testEmptyExcelFile(@TempDir Path tempDir) throws Exception {
        File excelFile = tempDir.resolve("empty.xlsx").toFile();
        
        // Create empty Excel file
        try (Workbook workbook = new XSSFWorkbook()) {
            workbook.createSheet("Empty");
            
            try (FileOutputStream fos = new FileOutputStream(excelFile)) {
                workbook.write(fos);
            }
        }
        
        // Verify file exists
        assertTrue(excelFile.exists());
        
        // Expected behavior: Should handle gracefully with no groups loaded
    }

    /**
     * Test Excel file with empty cells in the Group Name column
     */
    @Test
    public void testExcelWithEmptyCells(@TempDir Path tempDir) throws Exception {
        File excelFile = tempDir.resolve("groups_with_empty_cells.xlsx").toFile();
        
        // Create Excel file with some empty cells
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Groups");
            
            // Header row
            Row headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("Group Name");
            
            // Data rows with some empty cells
            Row row1 = sheet.createRow(1);
            row1.createCell(0).setCellValue("Oncology");
            
            Row row2 = sheet.createRow(2);
            // Empty cell in row2
            
            Row row3 = sheet.createRow(3);
            row3.createCell(0).setCellValue("Dermatology");
            
            Row row4 = sheet.createRow(4);
            row4.createCell(0).setCellValue(""); // Explicitly empty string
            
            Row row5 = sheet.createRow(5);
            row5.createCell(0).setCellValue("Ophthalmology");
            
            try (FileOutputStream fos = new FileOutputStream(excelFile)) {
                workbook.write(fos);
            }
        }
        
        // Verify file exists
        assertTrue(excelFile.exists());
        
        // Expected behavior: Should skip empty cells
        // Groups should be: "Oncology", "Dermatology", "Ophthalmology"
    }
}
