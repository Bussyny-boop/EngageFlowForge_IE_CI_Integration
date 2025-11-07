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
 * Test to verify the "In scope" checkbox column functionality.
 */
class InScopeFilterTest {

    @Test
    void rowsWithInScopeFalseAreFilteredOut() throws Exception {
        Path tempDir = Files.createTempDirectory("inscope-test");
        Path excelPath = tempDir.resolve("input.xlsx");
        Path jsonPath = tempDir.resolve("nurse.json");

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
            
            // First alarm - In scope = TRUE
            Row nurseRow1 = nurseCalls.createRow(3);
            nurseRow1.createCell(0).setCellValue("TRUE");
            nurseRow1.createCell(1).setCellValue("TestGroup");
            nurseRow1.createCell(2).setCellValue("In Scope Alarm");
            nurseRow1.createCell(3).setCellValue("High");
            nurseRow1.createCell(4).setCellValue("Badge");
            nurseRow1.createCell(5).setCellValue("0");
            nurseRow1.createCell(6).setCellValue("Nurse Team");
            
            // Second alarm - In scope = FALSE (should be filtered out)
            Row nurseRow2 = nurseCalls.createRow(4);
            nurseRow2.createCell(0).setCellValue("FALSE");
            nurseRow2.createCell(1).setCellValue("TestGroup");
            nurseRow2.createCell(2).setCellValue("Out Of Scope Alarm");
            nurseRow2.createCell(3).setCellValue("High");
            nurseRow2.createCell(4).setCellValue("Badge");
            nurseRow2.createCell(5).setCellValue("0");
            nurseRow2.createCell(6).setCellValue("Nurse Team");

            // Third alarm - In scope = TRUE
            Row nurseRow3 = nurseCalls.createRow(5);
            nurseRow3.createCell(0).setCellValue("Y");
            nurseRow3.createCell(1).setCellValue("TestGroup");
            nurseRow3.createCell(2).setCellValue("Another In Scope Alarm");
            nurseRow3.createCell(3).setCellValue("High");
            nurseRow3.createCell(4).setCellValue("Badge");
            nurseRow3.createCell(5).setCellValue("0");
            nurseRow3.createCell(6).setCellValue("Nurse Team");

            // Empty Clinical sheet
            Sheet clinicals = workbook.createSheet("Patient Monitoring");
            Row clinicalHeader = clinicals.createRow(2);
            clinicalHeader.createCell(0).setCellValue("In scope");
            clinicalHeader.createCell(1).setCellValue("Configuration Group");

            try (OutputStream os = Files.newOutputStream(excelPath)) {
                workbook.write(os);
            }
        }

        // Parse and generate JSON
        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(excelPath.toFile());
        parser.writeNurseCallsJson(jsonPath.toFile(), false);

        // Verify the JSON content
        String json = Files.readString(jsonPath);
        
        // Should contain in-scope alarms
        assertTrue(json.contains("In Scope Alarm"), "JSON should include 'In Scope Alarm'");
        assertTrue(json.contains("Another In Scope Alarm"), "JSON should include 'Another In Scope Alarm'");
        
        // Should NOT contain out-of-scope alarm
        assertFalse(json.contains("Out Of Scope Alarm"), "JSON should NOT include 'Out Of Scope Alarm'");
    }

    @Test
    void defaultToTrueWhenColumnMissing() throws Exception {
        Path tempDir = Files.createTempDirectory("inscope-default-test");
        Path excelPath = tempDir.resolve("input.xlsx");
        Path jsonPath = tempDir.resolve("nurse.json");

        // Create workbook WITHOUT in-scope column (backward compatibility)
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

            // Nurse Call WITHOUT "In scope" column
            Sheet nurseCalls = workbook.createSheet("Nurse call");
            Row nurseHeader = nurseCalls.createRow(2);
            nurseHeader.createCell(0).setCellValue("Configuration Group");
            nurseHeader.createCell(1).setCellValue("Common Alert or Alarm Name");
            nurseHeader.createCell(2).setCellValue("Priority");
            nurseHeader.createCell(3).setCellValue("Device - A");
            nurseHeader.createCell(4).setCellValue("Time to 1st Recipient");
            nurseHeader.createCell(5).setCellValue("1st Recipient");
            
            // Alarm without in-scope column - should default to TRUE
            Row nurseRow1 = nurseCalls.createRow(3);
            nurseRow1.createCell(0).setCellValue("TestGroup");
            nurseRow1.createCell(1).setCellValue("Default Alarm");
            nurseRow1.createCell(2).setCellValue("High");
            nurseRow1.createCell(3).setCellValue("Badge");
            nurseRow1.createCell(4).setCellValue("0");
            nurseRow1.createCell(5).setCellValue("Nurse Team");

            // Empty Clinical sheet
            Sheet clinicals = workbook.createSheet("Patient Monitoring");
            Row clinicalHeader = clinicals.createRow(2);
            clinicalHeader.createCell(0).setCellValue("Configuration Group");
            clinicalHeader.createCell(1).setCellValue("Common Alert or Alarm Name");

            try (OutputStream os = Files.newOutputStream(excelPath)) {
                workbook.write(os);
            }
        }

        // Parse and generate JSON
        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(excelPath.toFile());
        parser.writeNurseCallsJson(jsonPath.toFile(), false);

        // Verify the alarm is included (defaulted to in-scope)
        String json = Files.readString(jsonPath);
        assertTrue(json.contains("Default Alarm"), "JSON should include alarm when in-scope column is missing");
    }

    @Test
    void mergedFlowsRespectInScope() throws Exception {
        Path tempDir = Files.createTempDirectory("inscope-merge-test");
        Path excelPath = tempDir.resolve("input.xlsx");
        Path jsonPath = tempDir.resolve("nurse-merged.json");

        // Create workbook with in-scope column and mergeable flows
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

            // Nurse Call with mergeable alarms
            Sheet nurseCalls = workbook.createSheet("Nurse call");
            Row nurseHeader = nurseCalls.createRow(2);
            nurseHeader.createCell(0).setCellValue("In scope");
            nurseHeader.createCell(1).setCellValue("Configuration Group");
            nurseHeader.createCell(2).setCellValue("Common Alert or Alarm Name");
            nurseHeader.createCell(3).setCellValue("Priority");
            nurseHeader.createCell(4).setCellValue("Device - A");
            nurseHeader.createCell(5).setCellValue("Ringtone Device - A");
            nurseHeader.createCell(6).setCellValue("Time to 1st Recipient");
            nurseHeader.createCell(7).setCellValue("1st Recipient");
            
            // First alarm - In scope
            Row nurseRow1 = nurseCalls.createRow(3);
            nurseRow1.createCell(0).setCellValue("TRUE");
            nurseRow1.createCell(1).setCellValue("TestGroup");
            nurseRow1.createCell(2).setCellValue("Alarm A");
            nurseRow1.createCell(3).setCellValue("High");
            nurseRow1.createCell(4).setCellValue("Badge");
            nurseRow1.createCell(5).setCellValue("Tone 1");
            nurseRow1.createCell(6).setCellValue("0");
            nurseRow1.createCell(7).setCellValue("Nurse Team");
            
            // Second alarm - Out of scope (same delivery params)
            Row nurseRow2 = nurseCalls.createRow(4);
            nurseRow2.createCell(0).setCellValue("FALSE");
            nurseRow2.createCell(1).setCellValue("TestGroup");
            nurseRow2.createCell(2).setCellValue("Alarm B");
            nurseRow2.createCell(3).setCellValue("High");
            nurseRow2.createCell(4).setCellValue("Badge");
            nurseRow2.createCell(5).setCellValue("Tone 1");
            nurseRow2.createCell(6).setCellValue("0");
            nurseRow2.createCell(7).setCellValue("Nurse Team");

            // Third alarm - In scope (same delivery params)
            Row nurseRow3 = nurseCalls.createRow(5);
            nurseRow3.createCell(0).setCellValue("TRUE");
            nurseRow3.createCell(1).setCellValue("TestGroup");
            nurseRow3.createCell(2).setCellValue("Alarm C");
            nurseRow3.createCell(3).setCellValue("High");
            nurseRow3.createCell(4).setCellValue("Badge");
            nurseRow3.createCell(5).setCellValue("Tone 1");
            nurseRow3.createCell(6).setCellValue("0");
            nurseRow3.createCell(7).setCellValue("Nurse Team");

            // Empty Clinical sheet
            Sheet clinicals = workbook.createSheet("Patient Monitoring");
            Row clinicalHeader = clinicals.createRow(2);
            clinicalHeader.createCell(0).setCellValue("In scope");
            clinicalHeader.createCell(1).setCellValue("Configuration Group");

            try (OutputStream os = Files.newOutputStream(excelPath)) {
                workbook.write(os);
            }
        }

        // Parse and generate merged JSON
        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(excelPath.toFile());
        parser.writeNurseCallsJson(jsonPath.toFile(), true);

        // Verify the JSON content
        String json = Files.readString(jsonPath);
        
        // Should merge only in-scope alarms
        assertTrue(json.contains("Alarm A"), "JSON should include 'Alarm A'");
        assertTrue(json.contains("Alarm C"), "JSON should include 'Alarm C'");
        assertFalse(json.contains("Alarm B"), "JSON should NOT include 'Alarm B'");
        
        // Should have one flow with two alarms (A and C merged)
        int flowCount = countOccurrences(json, "\"alarmsAlerts\":");
        assertEquals(1, flowCount, "Should have 1 merged flow for in-scope alarms");
    }

    private static int countOccurrences(String text, String substring) {
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(substring, index)) != -1) {
            count++;
            index += substring.length();
        }
        return count;
    }
}
