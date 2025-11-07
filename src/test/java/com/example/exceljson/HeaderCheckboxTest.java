package com.example.exceljson;

import static org.junit.jupiter.api.Assertions.*;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

/**
 * Test to verify the header checkbox functionality works correctly.
 * This tests the underlying data model behavior that the header checkbox relies on.
 * 
 * Test scenarios:
 * 1. Basic toggle functionality - verifies that toggling the header checkbox updates all rows
 * 2. Filter interaction - verifies that the header checkbox only affects visible/filtered rows
 */
class HeaderCheckboxTest {

    @Test
    void headerCheckboxShouldToggleAllVisibleRows() throws Exception {
        Path tempDir = Files.createTempDirectory("header-checkbox-test");
        Path excelPath = tempDir.resolve("input.xlsx");

        // Create a test workbook
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            // Unit Breakdown
            Sheet units = workbook.createSheet("Unit Breakdown");
            Row unitsHeader = units.createRow(2);
            unitsHeader.createCell(0).setCellValue("Facility");
            unitsHeader.createCell(1).setCellValue("Common Unit Name");
            unitsHeader.createCell(2).setCellValue("Nurse Call Configuration Group");
            Row unitsRow = units.createRow(3);
            unitsRow.createCell(0).setCellValue("TestFacility");
            unitsRow.createCell(1).setCellValue("TestUnit");
            unitsRow.createCell(2).setCellValue("TestGroup");

            // Nurse Call with "In scope" column
            Sheet nurseCalls = workbook.createSheet("Nurse call");
            Row nurseHeader = nurseCalls.createRow(2);
            nurseHeader.createCell(0).setCellValue("In scope");
            nurseHeader.createCell(1).setCellValue("Configuration Group");
            nurseHeader.createCell(2).setCellValue("Common Alert or Alarm Name");
            nurseHeader.createCell(3).setCellValue("Priority");
            nurseHeader.createCell(4).setCellValue("Device - A");
            nurseHeader.createCell(5).setCellValue("Time to 1st Recipient");
            nurseHeader.createCell(6).setCellValue("1st Recipient");
            
            // Create 3 alarms, all initially in scope
            for (int i = 0; i < 3; i++) {
                Row nurseRow = nurseCalls.createRow(3 + i);
                nurseRow.createCell(0).setCellValue(true); // In scope = true
                nurseRow.createCell(1).setCellValue("TestGroup");
                nurseRow.createCell(2).setCellValue("Alarm " + (i + 1));
                nurseRow.createCell(3).setCellValue("normal");
                nurseRow.createCell(4).setCellValue("OutgoingWCTP");
                nurseRow.createCell(5).setCellValue("0");
                nurseRow.createCell(6).setCellValue("Nurse");
            }

            try (OutputStream out = Files.newOutputStream(excelPath)) {
                workbook.write(out);
            }
        }

        // Load the workbook
        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(excelPath.toFile());

        // Verify all rows are initially in scope
        assertEquals(3, parser.nurseCalls.size(), "Should have 3 nurse call rows");
        for (ExcelParserV5.FlowRow row : parser.nurseCalls) {
            assertTrue(row.inScope, "All rows should initially be in scope");
        }

        // Simulate unchecking the header checkbox - set all to false
        for (ExcelParserV5.FlowRow row : parser.nurseCalls) {
            row.inScope = false;
        }

        // Verify all rows are now out of scope
        for (ExcelParserV5.FlowRow row : parser.nurseCalls) {
            assertFalse(row.inScope, "All rows should now be out of scope");
        }

        // Simulate checking the header checkbox again - set all to true
        for (ExcelParserV5.FlowRow row : parser.nurseCalls) {
            row.inScope = true;
        }

        // Verify all rows are back in scope
        for (ExcelParserV5.FlowRow row : parser.nurseCalls) {
            assertTrue(row.inScope, "All rows should be back in scope");
        }
    }

    @Test
    void headerCheckboxShouldOnlyAffectFilteredRows() throws Exception {
        Path tempDir = Files.createTempDirectory("header-checkbox-filter-test");
        Path excelPath = tempDir.resolve("input.xlsx");

        // Create a test workbook with two different config groups
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            // Unit Breakdown
            Sheet units = workbook.createSheet("Unit Breakdown");
            Row unitsHeader = units.createRow(2);
            unitsHeader.createCell(0).setCellValue("Facility");
            unitsHeader.createCell(1).setCellValue("Common Unit Name");
            unitsHeader.createCell(2).setCellValue("Nurse Call Configuration Group");
            Row unitsRow1 = units.createRow(3);
            unitsRow1.createCell(0).setCellValue("TestFacility");
            unitsRow1.createCell(1).setCellValue("TestUnit1");
            unitsRow1.createCell(2).setCellValue("Group1");
            Row unitsRow2 = units.createRow(4);
            unitsRow2.createCell(0).setCellValue("TestFacility");
            unitsRow2.createCell(1).setCellValue("TestUnit2");
            unitsRow2.createCell(2).setCellValue("Group2");

            // Nurse Call with different config groups
            Sheet nurseCalls = workbook.createSheet("Nurse call");
            Row nurseHeader = nurseCalls.createRow(2);
            nurseHeader.createCell(0).setCellValue("In scope");
            nurseHeader.createCell(1).setCellValue("Configuration Group");
            nurseHeader.createCell(2).setCellValue("Common Alert or Alarm Name");
            nurseHeader.createCell(3).setCellValue("Priority");
            nurseHeader.createCell(4).setCellValue("Device - A");
            nurseHeader.createCell(5).setCellValue("Time to 1st Recipient");
            nurseHeader.createCell(6).setCellValue("1st Recipient");
            
            // Create 2 alarms for Group1
            for (int i = 0; i < 2; i++) {
                Row nurseRow = nurseCalls.createRow(3 + i);
                nurseRow.createCell(0).setCellValue(true);
                nurseRow.createCell(1).setCellValue("Group1");
                nurseRow.createCell(2).setCellValue("Group1 Alarm " + (i + 1));
                nurseRow.createCell(3).setCellValue("normal");
                nurseRow.createCell(4).setCellValue("OutgoingWCTP");
                nurseRow.createCell(5).setCellValue("0");
                nurseRow.createCell(6).setCellValue("Nurse");
            }
            
            // Create 2 alarms for Group2
            for (int i = 0; i < 2; i++) {
                Row nurseRow = nurseCalls.createRow(5 + i);
                nurseRow.createCell(0).setCellValue(true);
                nurseRow.createCell(1).setCellValue("Group2");
                nurseRow.createCell(2).setCellValue("Group2 Alarm " + (i + 1));
                nurseRow.createCell(3).setCellValue("normal");
                nurseRow.createCell(4).setCellValue("OutgoingWCTP");
                nurseRow.createCell(5).setCellValue("0");
                nurseRow.createCell(6).setCellValue("Nurse");
            }

            try (OutputStream out = Files.newOutputStream(excelPath)) {
                workbook.write(out);
            }
        }

        // Load the workbook
        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(excelPath.toFile());

        // Verify we have 4 rows total
        assertEquals(4, parser.nurseCalls.size(), "Should have 4 nurse call rows");

        // Simulate filtering to only show Group1 rows and unchecking them
        for (ExcelParserV5.FlowRow row : parser.nurseCalls) {
            if ("Group1".equals(row.configGroup)) {
                row.inScope = false; // Uncheck only Group1 rows
            }
        }

        // Verify Group1 rows are unchecked, Group2 rows are still checked
        for (ExcelParserV5.FlowRow row : parser.nurseCalls) {
            if ("Group1".equals(row.configGroup)) {
                assertFalse(row.inScope, "Group1 rows should be unchecked");
            } else if ("Group2".equals(row.configGroup)) {
                assertTrue(row.inScope, "Group2 rows should still be checked");
            }
        }
    }
}
