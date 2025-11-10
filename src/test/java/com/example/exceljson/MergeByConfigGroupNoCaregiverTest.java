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
 * Tests that verify "No Caregiver Group" is properly included in the merge criteria
 * when using MERGE_BY_CONFIG_GROUP mode.
 */
class MergeByConfigGroupNoCaregiverTest {

    @Test
    void mergeByConfigGroup_SeparatesFlowsByNoCaregiverGroup() throws Exception {
        Path tempDir = Files.createTempDirectory("merge-nocare-test");
        Path excelPath = tempDir.resolve("input.xlsx");
        Path jsonPath = tempDir.resolve("output.json");

        // Create workbook with identical flows but different No Caregiver Groups
        createWorkbookWithDifferentNoCaregiverGroups(excelPath);

        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(excelPath.toFile());
        parser.writeNurseCallsJson(jsonPath.toFile(), ExcelParserV5.MergeMode.MERGE_BY_CONFIG_GROUP);

        assertTrue(Files.exists(jsonPath));
        String json = Files.readString(jsonPath);
        
        // With MERGE_BY_CONFIG_GROUP, flows with different No Caregiver Groups should NOT merge
        // even if all other delivery parameters are identical
        int flowCount = countOccurrences(json, "\"alarmsAlerts\":");
        assertEquals(2, flowCount, "Should have 2 separate flows (one per No Caregiver Group)");
        
        // Each flow should contain the alarms from its No Caregiver Group
        assertTrue(json.contains("Alarm_NoCare1"));
        assertTrue(json.contains("Alarm_NoCare2"));
    }

    @Test
    void mergeByConfigGroup_MergesWithSameNoCaregiverGroup() throws Exception {
        Path tempDir = Files.createTempDirectory("merge-nocare-same-test");
        Path excelPath = tempDir.resolve("input.xlsx");
        Path jsonPath = tempDir.resolve("output.json");

        // Create workbook with identical flows including same No Caregiver Group
        createWorkbookWithSameNoCaregiverGroup(excelPath);

        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(excelPath.toFile());
        parser.writeNurseCallsJson(jsonPath.toFile(), ExcelParserV5.MergeMode.MERGE_BY_CONFIG_GROUP);

        assertTrue(Files.exists(jsonPath));
        String json = Files.readString(jsonPath);
        
        // With MERGE_BY_CONFIG_GROUP, flows with same No Caregiver Group should merge
        int flowCount = countOccurrences(json, "\"alarmsAlerts\":");
        assertEquals(1, flowCount, "Should have 1 merged flow (same No Caregiver Group)");
        
        // The flow should contain both alarms
        assertTrue(json.contains("Alarm1"));
        assertTrue(json.contains("Alarm2"));
    }

    /**
     * Creates a workbook with identical flows but different No Caregiver Groups.
     */
    private static void createWorkbookWithDifferentNoCaregiverGroups(Path target) throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            // Unit Breakdown - Same config group, same units, different No Caregiver Groups
            Sheet units = workbook.createSheet("Unit Breakdown");
            Row unitsHeader = units.createRow(2);
            unitsHeader.createCell(0).setCellValue("Facility");
            unitsHeader.createCell(1).setCellValue("Common Unit Name");
            unitsHeader.createCell(2).setCellValue("Nurse Call Configuration Group");
            unitsHeader.createCell(3).setCellValue("No Caregiver Group");
            
            Row unitsRow1 = units.createRow(3);
            unitsRow1.createCell(0).setCellValue("Test Facility");
            unitsRow1.createCell(1).setCellValue("Unit 1");
            unitsRow1.createCell(2).setCellValue("TestGroup");
            unitsRow1.createCell(3).setCellValue("NoCare_1");
            
            Row unitsRow2 = units.createRow(4);
            unitsRow2.createCell(0).setCellValue("Test Facility");
            unitsRow2.createCell(1).setCellValue("Unit 2");
            unitsRow2.createCell(2).setCellValue("TestGroup");
            unitsRow2.createCell(3).setCellValue("NoCare_2");

            // Nurse Call with identical flows but targeting different units with different No Caregiver Groups
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
            
            // Alarm for Unit 1 (NoCare_1)
            Row row1 = nurseCalls.createRow(3);
            row1.createCell(0).setCellValue("TestGroup");
            row1.createCell(1).setCellValue("Alarm_NoCare1");
            row1.createCell(2).setCellValue("High");
            row1.createCell(3).setCellValue("Badge");
            row1.createCell(4).setCellValue("Tone1");
            row1.createCell(5).setCellValue("Accept");
            row1.createCell(6).setCellValue("0");
            row1.createCell(7).setCellValue("Nurse Team");
            
            // Alarm for Unit 2 (NoCare_2) - identical delivery params but different No Caregiver Group
            Row row2 = nurseCalls.createRow(4);
            row2.createCell(0).setCellValue("TestGroup");
            row2.createCell(1).setCellValue("Alarm_NoCare2");
            row2.createCell(2).setCellValue("High");
            row2.createCell(3).setCellValue("Badge");
            row2.createCell(4).setCellValue("Tone1");
            row2.createCell(5).setCellValue("Accept");
            row2.createCell(6).setCellValue("0");
            row2.createCell(7).setCellValue("Nurse Team");

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
     * Creates a workbook with identical flows including same No Caregiver Group.
     */
    private static void createWorkbookWithSameNoCaregiverGroup(Path target) throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            // Unit Breakdown - Same config group, same No Caregiver Group
            Sheet units = workbook.createSheet("Unit Breakdown");
            Row unitsHeader = units.createRow(2);
            unitsHeader.createCell(0).setCellValue("Facility");
            unitsHeader.createCell(1).setCellValue("Common Unit Name");
            unitsHeader.createCell(2).setCellValue("Nurse Call Configuration Group");
            unitsHeader.createCell(3).setCellValue("No Caregiver Group");
            
            Row unitsRow1 = units.createRow(3);
            unitsRow1.createCell(0).setCellValue("Test Facility");
            unitsRow1.createCell(1).setCellValue("Unit 1");
            unitsRow1.createCell(2).setCellValue("TestGroup");
            unitsRow1.createCell(3).setCellValue("NoCare_Same");
            
            Row unitsRow2 = units.createRow(4);
            unitsRow2.createCell(0).setCellValue("Test Facility");
            unitsRow2.createCell(1).setCellValue("Unit 2");
            unitsRow2.createCell(2).setCellValue("TestGroup");
            unitsRow2.createCell(3).setCellValue("NoCare_Same");

            // Nurse Call with identical flows targeting units with same No Caregiver Group
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
            
            // Alarm 1
            Row row1 = nurseCalls.createRow(3);
            row1.createCell(0).setCellValue("TestGroup");
            row1.createCell(1).setCellValue("Alarm1");
            row1.createCell(2).setCellValue("High");
            row1.createCell(3).setCellValue("Badge");
            row1.createCell(4).setCellValue("Tone1");
            row1.createCell(5).setCellValue("Accept");
            row1.createCell(6).setCellValue("0");
            row1.createCell(7).setCellValue("Nurse Team");
            
            // Alarm 2 - identical to Alarm 1
            Row row2 = nurseCalls.createRow(4);
            row2.createCell(0).setCellValue("TestGroup");
            row2.createCell(1).setCellValue("Alarm2");
            row2.createCell(2).setCellValue("High");
            row2.createCell(3).setCellValue("Badge");
            row2.createCell(4).setCellValue("Tone1");
            row2.createCell(5).setCellValue("Accept");
            row2.createCell(6).setCellValue("0");
            row2.createCell(7).setCellValue("Nurse Team");

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
