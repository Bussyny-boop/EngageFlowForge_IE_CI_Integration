package com.example.exceljson;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

/**
 * Tests for the merge identical flows feature to ensure both Generate and Export
 * buttons respect the "Merge Identical Flows (Advanced)" checkbox.
 */
class MergeFlowsTest {

    @Test
    void writeNurseCallsJsonWithoutMerge() throws Exception {
        Path tempDir = Files.createTempDirectory("merge-test");
        Path excelPath = tempDir.resolve("input.xlsx");
        Path jsonPath = tempDir.resolve("nurse-standard.json");

        createWorkbookWithDuplicateFlows(excelPath);

        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(excelPath.toFile());
        parser.writeNurseCallsJson(jsonPath.toFile(), false);

        assertTrue(Files.exists(jsonPath));
        String json = Files.readString(jsonPath);
        
        // Without merge, we should have separate flows for each alarm
        assertTrue(json.contains("Alarm 1"));
        assertTrue(json.contains("Alarm 2"));
        
        // Count the number of flows - should be 2
        int flowCount = countOccurrences(json, "\"alarmsAlerts\":");
        assertEquals(2, flowCount, "Should have 2 separate flows without merge");
    }

    @Test
    void writeNurseCallsJsonWithMerge() throws Exception {
        Path tempDir = Files.createTempDirectory("merge-test");
        Path excelPath = tempDir.resolve("input.xlsx");
        Path jsonPath = tempDir.resolve("nurse-merged.json");

        createWorkbookWithDuplicateFlows(excelPath);

        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(excelPath.toFile());
        parser.writeNurseCallsJson(jsonPath.toFile(), true);

        assertTrue(Files.exists(jsonPath));
        String json = Files.readString(jsonPath);
        
        // With merge, both alarms should be in a single flow
        assertTrue(json.contains("Alarm 1"));
        assertTrue(json.contains("Alarm 2"));
        
        // Count the number of flows - should be 1
        int flowCount = countOccurrences(json, "\"alarmsAlerts\":");
        assertEquals(1, flowCount, "Should have 1 merged flow with merge enabled");
    }

    @Test
    void writeClinicalsJsonWithoutMerge() throws Exception {
        Path tempDir = Files.createTempDirectory("merge-test");
        Path excelPath = tempDir.resolve("input.xlsx");
        Path jsonPath = tempDir.resolve("clinical-standard.json");

        createWorkbookWithDuplicateClinicals(excelPath);

        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(excelPath.toFile());
        parser.writeClinicalsJson(jsonPath.toFile(), false);

        assertTrue(Files.exists(jsonPath));
        String json = Files.readString(jsonPath);
        
        // Without merge, we should have separate flows for each alarm
        assertTrue(json.contains("Clinical 1"));
        assertTrue(json.contains("Clinical 2"));
        
        // Count the number of flows - should be 2
        int flowCount = countOccurrences(json, "\"alarmsAlerts\":");
        assertEquals(2, flowCount, "Should have 2 separate flows without merge");
    }

    @Test
    void writeClinicalsJsonWithMerge() throws Exception {
        Path tempDir = Files.createTempDirectory("merge-test");
        Path excelPath = tempDir.resolve("input.xlsx");
        Path jsonPath = tempDir.resolve("clinical-merged.json");

        createWorkbookWithDuplicateClinicals(excelPath);

        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(excelPath.toFile());
        parser.writeClinicalsJson(jsonPath.toFile(), true);

        assertTrue(Files.exists(jsonPath));
        String json = Files.readString(jsonPath);
        
        // With merge, both alarms should be in a single flow
        assertTrue(json.contains("Clinical 1"));
        assertTrue(json.contains("Clinical 2"));
        
        // Count the number of flows - should be 1
        int flowCount = countOccurrences(json, "\"alarmsAlerts\":");
        assertEquals(1, flowCount, "Should have 1 merged flow with merge enabled");
    }

    @Test
    void parameterlessMethodsDefaultToNoMerge() throws Exception {
        Path tempDir = Files.createTempDirectory("merge-test");
        Path nursePath = tempDir.resolve("nurse.json");
        Path clinicalPath = tempDir.resolve("clinical.json");

        // Test nurse calls
        Path nurseExcelPath = tempDir.resolve("nurse-input.xlsx");
        createWorkbookWithDuplicateFlows(nurseExcelPath);
        
        ExcelParserV5 nurseParser = new ExcelParserV5();
        nurseParser.load(nurseExcelPath.toFile());
        nurseParser.writeNurseCallsJson(nursePath.toFile());
        
        String nurseJson = Files.readString(nursePath);
        int nurseFlowCount = countOccurrences(nurseJson, "\"alarmsAlerts\":");
        assertEquals(2, nurseFlowCount, "Parameterless method should not merge nurse flows");

        // Test clinicals
        Path clinicalExcelPath = tempDir.resolve("clinical-input.xlsx");
        createWorkbookWithDuplicateClinicals(clinicalExcelPath);
        
        ExcelParserV5 clinicalParser = new ExcelParserV5();
        clinicalParser.load(clinicalExcelPath.toFile());
        clinicalParser.writeClinicalsJson(clinicalPath.toFile());
        
        String clinicalJson = Files.readString(clinicalPath);
        int clinicalFlowCount = countOccurrences(clinicalJson, "\"alarmsAlerts\":");
        assertEquals(2, clinicalFlowCount, "Parameterless method should not merge clinical flows");
    }

    /**
     * Creates a workbook with two nurse call alarms that have identical delivery parameters
     * (same priority, ringtone, recipients, timing) but different alarm names.
     */
    private static void createWorkbookWithDuplicateFlows(Path target) throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            // Unit Breakdown - header at row 2, data starts at row 3
            Sheet units = workbook.createSheet("Unit Breakdown");
            Row unitsHeader = units.createRow(2);
            unitsHeader.createCell(0).setCellValue("Facility");
            unitsHeader.createCell(1).setCellValue("Common Unit Name");
            unitsHeader.createCell(2).setCellValue("Nurse Call Configuration Group");
            Row unitsRow = units.createRow(3);
            unitsRow.createCell(0).setCellValue("Test Facility");
            unitsRow.createCell(1).setCellValue("Test Unit");
            unitsRow.createCell(2).setCellValue("TestGroup");

            // Nurse Call with duplicate flows - header at row 2, data starts at row 3
            Sheet nurseCalls = workbook.createSheet("Nurse call");
            Row nurseHeader = nurseCalls.createRow(2);
            nurseHeader.createCell(0).setCellValue("Configuration Group");
            nurseHeader.createCell(1).setCellValue("Common Alert or Alarm Name");
            nurseHeader.createCell(2).setCellValue("Priority");
            nurseHeader.createCell(3).setCellValue("Device - A");
            nurseHeader.createCell(4).setCellValue("Ringtone Device - A");
            nurseHeader.createCell(5).setCellValue("Response Options");
            nurseHeader.createCell(6).setCellValue("Time to 1st Recipient");
            nurseHeader.createCell(7).setCellValue("1st Recipient");
            
            // First alarm
            Row nurseRow1 = nurseCalls.createRow(3);
            nurseRow1.createCell(0).setCellValue("TestGroup");
            nurseRow1.createCell(1).setCellValue("Alarm 1");
            nurseRow1.createCell(2).setCellValue("High");
            nurseRow1.createCell(3).setCellValue("Badge");
            nurseRow1.createCell(4).setCellValue("Tone 1");
            nurseRow1.createCell(5).setCellValue("Accept");
            nurseRow1.createCell(6).setCellValue("0");
            nurseRow1.createCell(7).setCellValue("Nurse Team");
            
            // Second alarm (same delivery params as first)
            Row nurseRow2 = nurseCalls.createRow(4);
            nurseRow2.createCell(0).setCellValue("TestGroup");
            nurseRow2.createCell(1).setCellValue("Alarm 2");
            nurseRow2.createCell(2).setCellValue("High");
            nurseRow2.createCell(3).setCellValue("Badge");
            nurseRow2.createCell(4).setCellValue("Tone 1");
            nurseRow2.createCell(5).setCellValue("Accept");
            nurseRow2.createCell(6).setCellValue("0");
            nurseRow2.createCell(7).setCellValue("Nurse Team");

            // Empty Clinical sheet
            Sheet clinicals = workbook.createSheet("Patient Monitoring");
            Row clinicalHeader = clinicals.createRow(2);
            clinicalHeader.createCell(0).setCellValue("Configuration Group");
            clinicalHeader.createCell(1).setCellValue("Alarm Name");

            try (OutputStream os = Files.newOutputStream(target)) {
                workbook.write(os);
            }
        }
    }

    /**
     * Creates a workbook with two clinical alarms that have identical delivery parameters.
     */
    private static void createWorkbookWithDuplicateClinicals(Path target) throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            // Unit Breakdown - header at row 2, data starts at row 3
            Sheet units = workbook.createSheet("Unit Breakdown");
            Row unitsHeader = units.createRow(2);
            unitsHeader.createCell(0).setCellValue("Facility");
            unitsHeader.createCell(1).setCellValue("Common Unit Name");
            unitsHeader.createCell(2).setCellValue("Patient Monitoring Configuration Group");
            Row unitsRow = units.createRow(3);
            unitsRow.createCell(0).setCellValue("Test Facility");
            unitsRow.createCell(1).setCellValue("Test Unit");
            unitsRow.createCell(2).setCellValue("TestGroup");

            // Empty Nurse Call sheet
            Sheet nurseCalls = workbook.createSheet("Nurse call");
            Row nurseHeader = nurseCalls.createRow(2);
            nurseHeader.createCell(0).setCellValue("Configuration Group");
            nurseHeader.createCell(1).setCellValue("Alarm Name");

            // Clinical with duplicate flows - header at row 2, data starts at row 3
            Sheet clinicals = workbook.createSheet("Patient Monitoring");
            Row clinicalHeader = clinicals.createRow(2);
            clinicalHeader.createCell(0).setCellValue("Configuration Group");
            clinicalHeader.createCell(1).setCellValue("Common Alert or Alarm Name");
            clinicalHeader.createCell(2).setCellValue("Priority");
            clinicalHeader.createCell(3).setCellValue("Device - A");
            clinicalHeader.createCell(4).setCellValue("Ringtone Device - A");
            clinicalHeader.createCell(5).setCellValue("Response Options");
            clinicalHeader.createCell(6).setCellValue("Time to 1st Recipient");
            clinicalHeader.createCell(7).setCellValue("1st Recipient");
            
            // First alarm
            Row clinicalRow1 = clinicals.createRow(3);
            clinicalRow1.createCell(0).setCellValue("TestGroup");
            clinicalRow1.createCell(1).setCellValue("Clinical 1");
            clinicalRow1.createCell(2).setCellValue("Medium");
            clinicalRow1.createCell(3).setCellValue("Badge");
            clinicalRow1.createCell(4).setCellValue("Tone 2");
            clinicalRow1.createCell(5).setCellValue("None");
            clinicalRow1.createCell(6).setCellValue("0");
            clinicalRow1.createCell(7).setCellValue("Primary Team");
            
            // Second alarm (same delivery params as first)
            Row clinicalRow2 = clinicals.createRow(4);
            clinicalRow2.createCell(0).setCellValue("TestGroup");
            clinicalRow2.createCell(1).setCellValue("Clinical 2");
            clinicalRow2.createCell(2).setCellValue("Medium");
            clinicalRow2.createCell(3).setCellValue("Badge");
            clinicalRow2.createCell(4).setCellValue("Tone 2");
            clinicalRow2.createCell(5).setCellValue("None");
            clinicalRow2.createCell(6).setCellValue("0");
            clinicalRow2.createCell(7).setCellValue("Primary Team");

            try (OutputStream os = Files.newOutputStream(target)) {
                workbook.write(os);
            }
        }
    }

    /**
     * Counts occurrences of a substring in a string.
     */
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
