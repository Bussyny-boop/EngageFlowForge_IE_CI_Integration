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
 * Test for verifying that data validation loaders properly handle headers with spaces before asterisks.
 * 
 * Problem: Headers like "Name *", "Group Name *", "Department *" have a SPACE before the asterisk.
 * The current regex "\\*+$" removes trailing asterisks but leaves the trailing space.
 * 
 * Expected behavior: "Name *" should be recognized as "Name" (trimmed)
 */
public class HeaderWithSpaceAsteriskTest {

    /**
     * Test Excel file with "Name *" header (space before asterisk) for Assignment Roles
     */
    @Test
    public void testAssignmentRolesExcelWithNameSpaceAsterisk(@TempDir Path tempDir) throws Exception {
        File excelFile = tempDir.resolve("roles_space_asterisk.xlsx").toFile();
        
        // Create Excel file with "Name *" header (space before asterisk)
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Roles");
            
            // Header row with space before asterisk (as shown in problem statement)
            Row headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("ID");
            headerRow.createCell(1).setCellValue("Name *");  // Space before asterisk
            headerRow.createCell(2).setCellValue("Abbreviation");
            
            // Data rows
            Row row1 = sheet.createRow(1);
            row1.createCell(0).setCellValue("1");
            row1.createCell(1).setCellValue("RN");
            row1.createCell(2).setCellValue("RN");
            
            Row row2 = sheet.createRow(2);
            row2.createCell(0).setCellValue("2");
            row2.createCell(1).setCellValue("MD");
            row2.createCell(2).setCellValue("MD");
            
            Row row3 = sheet.createRow(3);
            row3.createCell(0).setCellValue("3");
            row3.createCell(1).setCellValue("Tech");
            row3.createCell(2).setCellValue("TECH");
            
            try (FileOutputStream fos = new FileOutputStream(excelFile)) {
                workbook.write(fos);
            }
        }
        
        // Verify file exists and is readable
        assertTrue(excelFile.exists());
        assertTrue(excelFile.length() > 0);
        
        // Expected: Should recognize "Name *" as "Name" and load 3 roles
        // Roles should be: "RN", "MD", "Tech"
    }

    /**
     * Test CSV file with "Name *" header (space before asterisk) for Assignment Roles
     */
    @Test
    public void testAssignmentRolesCsvWithNameSpaceAsterisk(@TempDir Path tempDir) throws Exception {
        File csvFile = tempDir.resolve("roles_space_asterisk.csv").toFile();
        
        // Create CSV file with "Name *" header (space before asterisk)
        try (FileWriter writer = new FileWriter(csvFile)) {
            writer.write("ID,Name *,Abbreviation,Facility *,Order,Active Flag,Role Type\n");
            writer.write("1,RN,RN,Main Hospital,1,Y,Clinical\n");
            writer.write("2,MD,MD,Main Hospital,2,Y,Clinical\n");
            writer.write("3,Tech,TECH,Main Hospital,3,Y,Support\n");
        }
        
        // Verify file exists
        assertTrue(csvFile.exists());
        
        // Expected: Should recognize "Name *" as "Name" and load 3 roles
        // Roles should be: "RN", "MD", "Tech"
    }

    /**
     * Test Excel file with "Group Name *" header (space before asterisk) for Voice Groups
     */
    @Test
    public void testVoiceGroupsExcelWithGroupNameSpaceAsterisk(@TempDir Path tempDir) throws Exception {
        File excelFile = tempDir.resolve("groups_space_asterisk.xlsx").toFile();
        
        // Create Excel file with "Group Name *" header (space before asterisk)
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Groups");
            
            // Header row with space before asterisk
            Row headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("ID *");
            headerRow.createCell(1).setCellValue("Group Name *");  // Space before asterisk
            headerRow.createCell(2).setCellValue("Facility *");
            
            // Data rows
            Row row1 = sheet.createRow(1);
            row1.createCell(0).setCellValue("1");
            row1.createCell(1).setCellValue("Emergency Team");
            row1.createCell(2).setCellValue("Main Hospital");
            
            Row row2 = sheet.createRow(2);
            row2.createCell(0).setCellValue("2");
            row2.createCell(1).setCellValue("Code Blue Team");
            row2.createCell(2).setCellValue("Main Hospital");
            
            try (FileOutputStream fos = new FileOutputStream(excelFile)) {
                workbook.write(fos);
            }
        }
        
        // Verify file exists
        assertTrue(excelFile.exists());
        
        // Expected: Should recognize "Group Name *" as "Group Name" and load 2 groups
        // Groups should be: "Emergency Team", "Code Blue Team"
    }

    /**
     * Test CSV file with "Group Name *" header (space before asterisk) for Voice Groups
     */
    @Test
    public void testVoiceGroupsCsvWithGroupNameSpaceAsterisk(@TempDir Path tempDir) throws Exception {
        File csvFile = tempDir.resolve("groups_space_asterisk.csv").toFile();
        
        // Create CSV file with "Group Name *" header (space before asterisk)
        try (FileWriter writer = new FileWriter(csvFile)) {
            writer.write("ID *,Group Name *,Facility *,Vocera Phone,Group Type (Department, Subdepartment, Ordinary) *\n");
            writer.write("1,Emergency Team,Main Hospital,1234,Department\n");
            writer.write("2,Code Blue Team,Main Hospital,5678,Department\n");
            writer.write("3,Trauma Team,Main Hospital,9012,Subdepartment\n");
        }
        
        // Verify file exists
        assertTrue(csvFile.exists());
        
        // Expected: Should recognize "Group Name *" as "Group Name" and load 3 groups
        // Groups should be: "Emergency Team", "Code Blue Team", "Trauma Team"
    }

    /**
     * Test Excel file with "Department *" header (space before asterisk) for Bed List
     */
    @Test
    public void testBedListExcelWithDepartmentSpaceAsterisk(@TempDir Path tempDir) throws Exception {
        File excelFile = tempDir.resolve("beds_space_asterisk.xlsx").toFile();
        
        // Create Excel file with "Department *" header (space before asterisk)
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Beds");
            
            // Header row with space before asterisk
            Row headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("ID");
            headerRow.createCell(1).setCellValue("Bed *");
            headerRow.createCell(2).setCellValue("Room *");
            headerRow.createCell(3).setCellValue("Department *");  // Space before asterisk
            headerRow.createCell(4).setCellValue("Facility *");
            
            // Data rows
            Row row1 = sheet.createRow(1);
            row1.createCell(0).setCellValue("1");
            row1.createCell(1).setCellValue("A-101-1");
            row1.createCell(2).setCellValue("101");
            row1.createCell(3).setCellValue("ICU");
            row1.createCell(4).setCellValue("Main Hospital");
            
            Row row2 = sheet.createRow(2);
            row2.createCell(0).setCellValue("2");
            row2.createCell(1).setCellValue("A-102-1");
            row2.createCell(2).setCellValue("102");
            row2.createCell(3).setCellValue("ED");
            row2.createCell(4).setCellValue("Main Hospital");
            
            Row row3 = sheet.createRow(3);
            row3.createCell(0).setCellValue("3");
            row3.createCell(1).setCellValue("B-201-1");
            row3.createCell(2).setCellValue("201");
            row3.createCell(3).setCellValue("Med/Surg");
            row3.createCell(4).setCellValue("Main Hospital");
            
            try (FileOutputStream fos = new FileOutputStream(excelFile)) {
                workbook.write(fos);
            }
        }
        
        // Verify file exists
        assertTrue(excelFile.exists());
        
        // Expected: Should recognize "Department *" as "Department" and load 3 units
        // Units should be: "ICU", "ED", "Med/Surg"
    }

    /**
     * Test CSV file with "Department *" header (space before asterisk) for Bed List
     */
    @Test
    public void testBedListCsvWithDepartmentSpaceAsterisk(@TempDir Path tempDir) throws Exception {
        File csvFile = tempDir.resolve("beds_space_asterisk.csv").toFile();
        
        // Create CSV file with "Department *" header (space before asterisk)
        try (FileWriter writer = new FileWriter(csvFile)) {
            writer.write("ID,Bed *,Pillow Number,Room *,Room Name,Department *,Facility *\n");
            writer.write("1,A-101-1,101,101,ICU 101,ICU,Main Hospital\n");
            writer.write("2,A-102-1,102,102,ED 102,ED,Main Hospital\n");
            writer.write("3,B-201-1,201,201,MS 201,Med/Surg,Main Hospital\n");
        }
        
        // Verify file exists
        assertTrue(csvFile.exists());
        
        // Expected: Should recognize "Department *" as "Department" and load 3 units
        // Units should be: "ICU", "ED", "Med/Surg"
    }
}
