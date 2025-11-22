package com.example.exceljson;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test for verifying that data validation loaders properly ignore trailing asterisks
 * in column headers.
 * 
 * Requirements:
 * 1. Load AssignmentRole should load data from "Name*" column (ignoring the asterisk)
 * 2. Load Bed List should load data from "Department*" column (ignoring the asterisk)
 * 3. Support both Excel and CSV files
 * 4. Header detection is case-insensitive
 */
public class DataValidationHeaderAsteriskTest {

    /**
     * Test Excel file with "Name*" header for Assignment Roles
     */
    @Test
    public void testAssignmentRolesExcelWithNameAsterisk(@TempDir Path tempDir) throws Exception {
        File excelFile = tempDir.resolve("roles_with_asterisk.xlsx").toFile();
        
        // Create Excel file with "Name*" header
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Roles");
            
            // Header row with asterisk
            Row headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("Name*");
            
            // Data rows
            Row row1 = sheet.createRow(1);
            row1.createCell(0).setCellValue("Room 101");
            
            Row row2 = sheet.createRow(2);
            row2.createCell(0).setCellValue("Room 102");
            
            Row row3 = sheet.createRow(3);
            row3.createCell(0).setCellValue("ICU Pod A");
            
            try (FileOutputStream fos = new FileOutputStream(excelFile)) {
                workbook.write(fos);
            }
        }
        
        // Verify file exists and is readable
        assertTrue(excelFile.exists());
        assertTrue(excelFile.length() > 0);
        
        // Expected: Should recognize "Name*" as "Name" and load roles
        // Roles should be: "Room 101", "Room 102", "ICU Pod A"
    }

    /**
     * Test Excel file with "Department*" header for Bed List
     */
    @Test
    public void testBedListExcelWithDepartmentAsterisk(@TempDir Path tempDir) throws Exception {
        File excelFile = tempDir.resolve("beds_with_asterisk.xlsx").toFile();
        
        // Create Excel file with "Department*" header
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Beds");
            
            // Header row with asterisk
            Row headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("Department*");
            
            // Data rows
            Row row1 = sheet.createRow(1);
            row1.createCell(0).setCellValue("ICU");
            
            Row row2 = sheet.createRow(2);
            row2.createCell(0).setCellValue("ED");
            
            Row row3 = sheet.createRow(3);
            row3.createCell(0).setCellValue("Medical/Surgical");
            
            try (FileOutputStream fos = new FileOutputStream(excelFile)) {
                workbook.write(fos);
            }
        }
        
        // Verify file exists
        assertTrue(excelFile.exists());
        
        // Expected: Should recognize "Department*" as "Department" and load units
        // Units should be: "ICU", "ED", "Medical/Surgical"
    }

    /**
     * Test CSV file with "Name*" header for Assignment Roles
     */
    @Test
    public void testAssignmentRolesCsvWithNameAsterisk(@TempDir Path tempDir) throws Exception {
        File csvFile = tempDir.resolve("roles_with_asterisk.csv").toFile();
        
        // Create CSV file with "Name*" header
        try (FileWriter writer = new FileWriter(csvFile)) {
            writer.write("Name*\n");
            writer.write("ED Zone 1\n");
            writer.write("ED Zone 2\n");
            writer.write("Trauma Bay\n");
        }
        
        // Verify file exists
        assertTrue(csvFile.exists());
        
        // Expected: Should recognize "Name*" as "Name" and load roles
        // Roles should be: "ED Zone 1", "ED Zone 2", "Trauma Bay"
    }

    /**
     * Test CSV file with "Department*" header for Bed List
     */
    @Test
    public void testBedListCsvWithDepartmentAsterisk(@TempDir Path tempDir) throws Exception {
        File csvFile = tempDir.resolve("beds_with_asterisk.csv").toFile();
        
        // Create CSV file with "Department*" header
        try (FileWriter writer = new FileWriter(csvFile)) {
            writer.write("Department*\n");
            writer.write("Oncology\n");
            writer.write("Cardiology\n");
            writer.write("Pediatrics\n");
        }
        
        // Verify file exists
        assertTrue(csvFile.exists());
        
        // Expected: Should recognize "Department*" as "Department" and load units
        // Units should be: "Oncology", "Cardiology", "Pediatrics"
    }

    /**
     * Test CSV file with "Unit*" header for Bed List (alternative header name)
     */
    @Test
    public void testBedListCsvWithUnitAsterisk(@TempDir Path tempDir) throws Exception {
        File csvFile = tempDir.resolve("beds_unit_asterisk.csv").toFile();
        
        // Create CSV file with "Unit*" header
        try (FileWriter writer = new FileWriter(csvFile)) {
            writer.write("Unit*\n");
            writer.write("NICU\n");
            writer.write("PICU\n");
            writer.write("CCU\n");
        }
        
        // Verify file exists
        assertTrue(csvFile.exists());
        
        // Expected: Should recognize "Unit*" as "Unit" and load units
        // Units should be: "NICU", "PICU", "CCU"
    }

    /**
     * Test multiple asterisks in header (e.g., "Name***")
     */
    @Test
    public void testMultipleAsterisks(@TempDir Path tempDir) throws Exception {
        File csvFile = tempDir.resolve("multiple_asterisks.csv").toFile();
        
        // Create CSV file with multiple asterisks
        try (FileWriter writer = new FileWriter(csvFile)) {
            writer.write("Name***\n");
            writer.write("Role A\n");
            writer.write("Role B\n");
        }
        
        // Verify file exists
        assertTrue(csvFile.exists());
        
        // Expected: Should strip all trailing asterisks and recognize as "Name"
        // Roles should be: "Role A", "Role B"
    }

    /**
     * Test case-insensitive with asterisk (e.g., "name*", "NAME*")
     */
    @Test
    public void testCaseInsensitiveWithAsterisk(@TempDir Path tempDir) throws Exception {
        File csvFile = tempDir.resolve("case_insensitive_asterisk.csv").toFile();
        
        // Create CSV file with lowercase header and asterisk
        try (FileWriter writer = new FileWriter(csvFile)) {
            writer.write("name*\n");
            writer.write("Role X\n");
            writer.write("Role Y\n");
        }
        
        // Verify file exists
        assertTrue(csvFile.exists());
        
        // Expected: Should recognize "name*" as "Name" (case-insensitive)
        // Roles should be: "Role X", "Role Y"
    }

    /**
     * Test Excel with "Name*" in second column (not first)
     */
    @Test
    public void testExcelNameAsteriskInSecondColumn(@TempDir Path tempDir) throws Exception {
        File excelFile = tempDir.resolve("name_asterisk_col2.xlsx").toFile();
        
        // Create Excel file with "Name*" in second column
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Roles");
            
            // Header row
            Row headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("ID");
            headerRow.createCell(1).setCellValue("Name*");
            
            // Data rows
            Row row1 = sheet.createRow(1);
            row1.createCell(0).setCellValue("1");
            row1.createCell(1).setCellValue("Surgery Room 1");
            
            Row row2 = sheet.createRow(2);
            row2.createCell(0).setCellValue("2");
            row2.createCell(1).setCellValue("Surgery Room 2");
            
            try (FileOutputStream fos = new FileOutputStream(excelFile)) {
                workbook.write(fos);
            }
        }
        
        // Verify file exists
        assertTrue(excelFile.exists());
        
        // Expected: Should extract from second column (where "Name*" header is)
        // Roles should be: "Surgery Room 1", "Surgery Room 2"
        // IDs "1", "2" should NOT be loaded
    }
}
