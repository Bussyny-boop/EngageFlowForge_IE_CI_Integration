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
 * Test to verify that the "No Caregiver Group" column is included in merge logic.
 * Flows with different No Caregiver Groups should NOT be merged, even if all other
 * delivery parameters are identical.
 */
class NoCareGroupMergeTest {

    @Test
    void flowsWithDifferentNoCareGroupsShouldNotMerge() throws Exception {
        Path tempDir = Files.createTempDirectory("nocare-merge-test");
        Path excelPath = tempDir.resolve("input.xlsx");
        Path jsonPath = tempDir.resolve("nurse.json");

        createWorkbookWithDifferentNoCareGroups(excelPath);

        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(excelPath.toFile());
        parser.writeNurseCallsJson(jsonPath.toFile(), true); // Enable merge

        assertTrue(Files.exists(jsonPath));
        String json = Files.readString(jsonPath);
        
        // Both alarms should be present
        assertTrue(json.contains("Alarm 1"));
        assertTrue(json.contains("Alarm 2"));
        
        // Count the number of flows - should be 2 (NOT merged because No Caregiver Groups differ)
        int flowCount = countOccurrences(json, "\"alarmsAlerts\":");
        assertEquals(2, flowCount, "Flows with different No Caregiver Groups should NOT be merged");
    }

    @Test
    void flowsWithSameNoCareGroupShouldMerge() throws Exception {
        Path tempDir = Files.createTempDirectory("nocare-merge-test");
        Path excelPath = tempDir.resolve("input.xlsx");
        Path jsonPath = tempDir.resolve("nurse.json");

        createWorkbookWithSameNoCareGroups(excelPath);

        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(excelPath.toFile());
        parser.writeNurseCallsJson(jsonPath.toFile(), true); // Enable merge

        assertTrue(Files.exists(jsonPath));
        String json = Files.readString(jsonPath);
        
        // Both alarms should be present
        assertTrue(json.contains("Alarm 1"));
        assertTrue(json.contains("Alarm 2"));
        
        // Count the number of flows - should be 1 (merged because all params including No Caregiver Group match)
        int flowCount = countOccurrences(json, "\"alarmsAlerts\":");
        assertEquals(1, flowCount, "Flows with same No Caregiver Groups should be merged");
    }

    @Test
    void flowsWithDifferentNoCareGroupsInClinicalsShouldNotMerge() throws Exception {
        Path tempDir = Files.createTempDirectory("nocare-clinical-merge-test");
        Path excelPath = tempDir.resolve("input.xlsx");
        Path jsonPath = tempDir.resolve("clinical.json");

        createWorkbookWithDifferentNoCareGroupsClinicals(excelPath);

        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(excelPath.toFile());
        parser.writeClinicalsJson(jsonPath.toFile(), true); // Enable merge

        assertTrue(Files.exists(jsonPath));
        String json = Files.readString(jsonPath);
        
        // Both alarms should be present
        assertTrue(json.contains("Clinical 1"));
        assertTrue(json.contains("Clinical 2"));
        
        // Count the number of flows - should be 2 (NOT merged because No Caregiver Groups differ)
        int flowCount = countOccurrences(json, "\"alarmsAlerts\":");
        assertEquals(2, flowCount, "Clinical flows with different No Caregiver Groups should NOT be merged");
    }

    /**
     * Creates a workbook with two nurse call alarms that have identical delivery parameters
     * but different No Caregiver Groups (different config groups for different facilities).
     */
    private static void createWorkbookWithDifferentNoCareGroups(Path target) throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            // Unit Breakdown with two SEPARATE config groups for different facilities
            Sheet units = workbook.createSheet("Unit Breakdown");
            Row unitsHeader = units.createRow(2);
            unitsHeader.createCell(0).setCellValue("Facility");
            unitsHeader.createCell(1).setCellValue("Common Unit Name");
            unitsHeader.createCell(2).setCellValue("Nurse Call Configuration Group");
            unitsHeader.createCell(3).setCellValue("No Caregiver Group");
            
            // Facility 1 with No Caregiver Group "NoCare-A" and config group "GroupA"
            Row unitsRow1 = units.createRow(3);
            unitsRow1.createCell(0).setCellValue("Facility A");
            unitsRow1.createCell(1).setCellValue("Unit 1");
            unitsRow1.createCell(2).setCellValue("GroupA");
            unitsRow1.createCell(3).setCellValue("NoCare-A");
            
            // Facility 2 with No Caregiver Group "NoCare-B" and config group "GroupB"
            Row unitsRow2 = units.createRow(4);
            unitsRow2.createCell(0).setCellValue("Facility B");
            unitsRow2.createCell(1).setCellValue("Unit 2");
            unitsRow2.createCell(2).setCellValue("GroupB");
            unitsRow2.createCell(3).setCellValue("NoCare-B");

            // Nurse Call with duplicate delivery params but DIFFERENT config groups
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
            
            // First alarm with GroupA
            Row nurseRow1 = nurseCalls.createRow(3);
            nurseRow1.createCell(0).setCellValue("GroupA");
            nurseRow1.createCell(1).setCellValue("Alarm 1");
            nurseRow1.createCell(2).setCellValue("High");
            nurseRow1.createCell(3).setCellValue("Badge");
            nurseRow1.createCell(4).setCellValue("Tone 1");
            nurseRow1.createCell(5).setCellValue("Accept");
            nurseRow1.createCell(6).setCellValue("0");
            nurseRow1.createCell(7).setCellValue("Nurse Team");
            
            // Second alarm with GroupB (same delivery params, but different config group and No Caregiver Group)
            Row nurseRow2 = nurseCalls.createRow(4);
            nurseRow2.createCell(0).setCellValue("GroupB");
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
     * Creates a workbook with two nurse call alarms that have identical delivery parameters
     * and same No Caregiver Group (same facility).
     */
    private static void createWorkbookWithSameNoCareGroups(Path target) throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            // Unit Breakdown with single facility and No Caregiver Group
            Sheet units = workbook.createSheet("Unit Breakdown");
            Row unitsHeader = units.createRow(2);
            unitsHeader.createCell(0).setCellValue("Facility");
            unitsHeader.createCell(1).setCellValue("Common Unit Name");
            unitsHeader.createCell(2).setCellValue("Nurse Call Configuration Group");
            unitsHeader.createCell(3).setCellValue("No Caregiver Group");
            
            Row unitsRow = units.createRow(3);
            unitsRow.createCell(0).setCellValue("Test Facility");
            unitsRow.createCell(1).setCellValue("Test Unit");
            unitsRow.createCell(2).setCellValue("TestGroup");
            unitsRow.createCell(3).setCellValue("NoCare-Same");

            // Nurse Call with duplicate delivery params and same No Caregiver Group
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
            
            // Second alarm (identical to first)
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
     * Creates a workbook with two clinical alarms that have identical delivery parameters
     * but different No Caregiver Groups (different config groups for different facilities).
     */
    private static void createWorkbookWithDifferentNoCareGroupsClinicals(Path target) throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            // Unit Breakdown with two SEPARATE config groups for different facilities
            Sheet units = workbook.createSheet("Unit Breakdown");
            Row unitsHeader = units.createRow(2);
            unitsHeader.createCell(0).setCellValue("Facility");
            unitsHeader.createCell(1).setCellValue("Common Unit Name");
            unitsHeader.createCell(2).setCellValue("Patient Monitoring Configuration Group");
            unitsHeader.createCell(3).setCellValue("No Caregiver Group");
            
            // Facility 1 with No Caregiver Group "NoCare-X" and config group "GroupX"
            Row unitsRow1 = units.createRow(3);
            unitsRow1.createCell(0).setCellValue("Facility X");
            unitsRow1.createCell(1).setCellValue("Unit X");
            unitsRow1.createCell(2).setCellValue("GroupX");
            unitsRow1.createCell(3).setCellValue("NoCare-X");
            
            // Facility 2 with No Caregiver Group "NoCare-Y" and config group "GroupY"
            Row unitsRow2 = units.createRow(4);
            unitsRow2.createCell(0).setCellValue("Facility Y");
            unitsRow2.createCell(1).setCellValue("Unit Y");
            unitsRow2.createCell(2).setCellValue("GroupY");
            unitsRow2.createCell(3).setCellValue("NoCare-Y");

            // Empty Nurse Call sheet
            Sheet nurseCalls = workbook.createSheet("Nurse call");
            Row nurseHeader = nurseCalls.createRow(2);
            nurseHeader.createCell(0).setCellValue("Configuration Group");
            nurseHeader.createCell(1).setCellValue("Alarm Name");

            // Clinical with duplicate delivery params but DIFFERENT config groups
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
            
            // First alarm with GroupX
            Row clinicalRow1 = clinicals.createRow(3);
            clinicalRow1.createCell(0).setCellValue("GroupX");
            clinicalRow1.createCell(1).setCellValue("Clinical 1");
            clinicalRow1.createCell(2).setCellValue("Medium");
            clinicalRow1.createCell(3).setCellValue("Badge");
            clinicalRow1.createCell(4).setCellValue("Tone 2");
            clinicalRow1.createCell(5).setCellValue("None");
            clinicalRow1.createCell(6).setCellValue("0");
            clinicalRow1.createCell(7).setCellValue("Primary Team");
            
            // Second alarm with GroupY (same delivery params but different config group and No Caregiver Group)
            Row clinicalRow2 = clinicals.createRow(4);
            clinicalRow2.createCell(0).setCellValue("GroupY");
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
