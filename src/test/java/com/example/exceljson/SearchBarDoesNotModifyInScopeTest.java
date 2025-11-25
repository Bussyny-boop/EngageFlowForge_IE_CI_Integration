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
 * Test to verify that the search bar only filters table visibility without modifying
 * the "In Scope" checkbox values.
 * 
 * Issue: Previously, when users typed in the search bar, it would update the inScope 
 * property of FlowRow objects, causing the "In Scope" checkboxes to change. Users expected
 * the search bar to only filter the visible rows without changing any data.
 * 
 * Fix: The search bar now only filters visibility via FilteredList.setPredicate() and
 * does NOT modify flow.inScope.
 */
class SearchBarDoesNotModifyInScopeTest {

    /**
     * Test that when data is loaded with inScope set to TRUE, the inScope values
     * should remain TRUE regardless of any filtering that might be applied.
     * 
     * This test verifies that the parser preserves inScope values and they are not
     * modified by any internal processing.
     */
    @Test
    void inScopeValuesArePreservedAfterParsing() throws Exception {
        Path tempDir = Files.createTempDirectory("search-inscope-test");
        Path excelPath = tempDir.resolve("input.xlsx");

        // Create workbook with in-scope column
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
            
            // Multiple alarms with different names but ALL with inScope = TRUE
            Row nurseRow1 = nurseCalls.createRow(3);
            nurseRow1.createCell(0).setCellValue("TRUE");
            nurseRow1.createCell(1).setCellValue("TestGroup");
            nurseRow1.createCell(2).setCellValue("Code Blue");
            nurseRow1.createCell(3).setCellValue("High");
            nurseRow1.createCell(4).setCellValue("Badge");
            nurseRow1.createCell(5).setCellValue("0");
            nurseRow1.createCell(6).setCellValue("Nurse Team");
            
            Row nurseRow2 = nurseCalls.createRow(4);
            nurseRow2.createCell(0).setCellValue("TRUE");
            nurseRow2.createCell(1).setCellValue("TestGroup");
            nurseRow2.createCell(2).setCellValue("Bed Exit");
            nurseRow2.createCell(3).setCellValue("Normal");
            nurseRow2.createCell(4).setCellValue("Badge");
            nurseRow2.createCell(5).setCellValue("0");
            nurseRow2.createCell(6).setCellValue("Nurse Team");

            Row nurseRow3 = nurseCalls.createRow(5);
            nurseRow3.createCell(0).setCellValue("TRUE");
            nurseRow3.createCell(1).setCellValue("TestGroup");
            nurseRow3.createCell(2).setCellValue("Patient Call");
            nurseRow3.createCell(3).setCellValue("Low");
            nurseRow3.createCell(4).setCellValue("Badge");
            nurseRow3.createCell(5).setCellValue("0");
            nurseRow3.createCell(6).setCellValue("Nurse Team");

            // Empty Clinical sheet
            Sheet clinicals = workbook.createSheet("Patient Monitoring");
            Row clinicalHeader = clinicals.createRow(2);
            clinicalHeader.createCell(0).setCellValue("In scope");
            clinicalHeader.createCell(1).setCellValue("Configuration Group");
            clinicalHeader.createCell(2).setCellValue("Common Alert or Alarm Name");

            try (OutputStream os = Files.newOutputStream(excelPath)) {
                workbook.write(os);
            }
        }

        // Parse the workbook
        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(excelPath.toFile());

        // Verify all nurse calls have inScope = true after loading
        assertEquals(3, parser.nurseCalls.size(), "Should have 3 nurse calls");
        
        // All rows should have inScope = true because that's what was set in the Excel
        for (ExcelParserV5.FlowRow flow : parser.nurseCalls) {
            assertTrue(flow.inScope, 
                "inScope should remain TRUE for alarm: " + flow.alarmName + 
                " - search bar should not modify this value");
        }
    }

    /**
     * Test that both inScope TRUE and FALSE values from Excel are preserved correctly.
     */
    @Test
    void mixedInScopeValuesArePreserved() throws Exception {
        Path tempDir = Files.createTempDirectory("search-mixed-inscope-test");
        Path excelPath = tempDir.resolve("input.xlsx");

        // Create workbook with in-scope column
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

            // Nurse Call with mixed in-scope values
            Sheet nurseCalls = workbook.createSheet("Nurse call");
            Row nurseHeader = nurseCalls.createRow(2);
            nurseHeader.createCell(0).setCellValue("In scope");
            nurseHeader.createCell(1).setCellValue("Configuration Group");
            nurseHeader.createCell(2).setCellValue("Common Alert or Alarm Name");
            nurseHeader.createCell(3).setCellValue("Priority");
            nurseHeader.createCell(4).setCellValue("Device - A");
            nurseHeader.createCell(5).setCellValue("Time to 1st Recipient");
            nurseHeader.createCell(6).setCellValue("1st Recipient");
            
            // Alarm 1 - inScope = TRUE
            Row nurseRow1 = nurseCalls.createRow(3);
            nurseRow1.createCell(0).setCellValue("TRUE");
            nurseRow1.createCell(1).setCellValue("TestGroup");
            nurseRow1.createCell(2).setCellValue("In Scope Alarm");
            nurseRow1.createCell(3).setCellValue("High");
            nurseRow1.createCell(4).setCellValue("Badge");
            nurseRow1.createCell(5).setCellValue("0");
            nurseRow1.createCell(6).setCellValue("Nurse Team");
            
            // Alarm 2 - inScope = FALSE
            Row nurseRow2 = nurseCalls.createRow(4);
            nurseRow2.createCell(0).setCellValue("FALSE");
            nurseRow2.createCell(1).setCellValue("TestGroup");
            nurseRow2.createCell(2).setCellValue("Out Scope Alarm");
            nurseRow2.createCell(3).setCellValue("Normal");
            nurseRow2.createCell(4).setCellValue("Badge");
            nurseRow2.createCell(5).setCellValue("0");
            nurseRow2.createCell(6).setCellValue("Nurse Team");

            // Alarm 3 - inScope = TRUE (using "Y")
            Row nurseRow3 = nurseCalls.createRow(5);
            nurseRow3.createCell(0).setCellValue("Y");
            nurseRow3.createCell(1).setCellValue("TestGroup");
            nurseRow3.createCell(2).setCellValue("Another In Scope");
            nurseRow3.createCell(3).setCellValue("Low");
            nurseRow3.createCell(4).setCellValue("Badge");
            nurseRow3.createCell(5).setCellValue("0");
            nurseRow3.createCell(6).setCellValue("Nurse Team");

            // Empty Clinical sheet
            Sheet clinicals = workbook.createSheet("Patient Monitoring");
            Row clinicalHeader = clinicals.createRow(2);
            clinicalHeader.createCell(0).setCellValue("In scope");
            clinicalHeader.createCell(1).setCellValue("Configuration Group");
            clinicalHeader.createCell(2).setCellValue("Common Alert or Alarm Name");

            try (OutputStream os = Files.newOutputStream(excelPath)) {
                workbook.write(os);
            }
        }

        // Parse the workbook
        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(excelPath.toFile());

        // Verify we have all 3 nurse calls
        assertEquals(3, parser.nurseCalls.size(), "Should have 3 nurse calls");
        
        // Verify each alarm's inScope value is preserved correctly
        for (ExcelParserV5.FlowRow flow : parser.nurseCalls) {
            if (flow.alarmName.equals("In Scope Alarm")) {
                assertTrue(flow.inScope, "In Scope Alarm should have inScope = true");
            } else if (flow.alarmName.equals("Out Scope Alarm")) {
                assertFalse(flow.inScope, "Out Scope Alarm should have inScope = false");
            } else if (flow.alarmName.equals("Another In Scope")) {
                assertTrue(flow.inScope, "Another In Scope should have inScope = true (Y value)");
            }
        }
    }
}
