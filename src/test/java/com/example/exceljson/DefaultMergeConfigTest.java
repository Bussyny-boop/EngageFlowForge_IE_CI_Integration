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
 * Test to verify that MERGE_BY_CONFIG_GROUP is the expected default behavior.
 * This test ensures the merge logic works correctly when set as default.
 */
class DefaultMergeConfigTest {

    @Test
    void defaultBehavior_MergesByConfigGroup() throws Exception {
        Path tempDir = Files.createTempDirectory("default-merge-test");
        Path excelPath = tempDir.resolve("input.xlsx");
        Path jsonPath = tempDir.resolve("output.json");

        // Create workbook with flows in different config groups
        createWorkbookWithDifferentConfigGroups(excelPath);

        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(excelPath.toFile());
        
        // Use MERGE_BY_CONFIG_GROUP mode (the new default)
        parser.writeNurseCallsJson(jsonPath.toFile(), ExcelParserV5.MergeMode.MERGE_BY_CONFIG_GROUP);

        assertTrue(Files.exists(jsonPath));
        String json = Files.readString(jsonPath);
        
        // With MERGE_BY_CONFIG_GROUP, identical flows in different groups should NOT merge
        int flowCount = countOccurrences(json, "\"alarmsAlerts\":");
        assertEquals(2, flowCount, "Should have 2 separate flows (one per config group)");
        
        // Verify each group's alarms are present
        assertTrue(json.contains("GroupA_Alarm"));
        assertTrue(json.contains("GroupB_Alarm"));
    }

    @Test
    void defaultBehavior_MergesWithinSameGroup() throws Exception {
        Path tempDir = Files.createTempDirectory("default-merge-same-test");
        Path excelPath = tempDir.resolve("input.xlsx");
        Path jsonPath = tempDir.resolve("output.json");

        // Create workbook with multiple alarms in the same config group
        createWorkbookWithSameConfigGroup(excelPath);

        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(excelPath.toFile());
        
        // Use MERGE_BY_CONFIG_GROUP mode (the new default)
        parser.writeNurseCallsJson(jsonPath.toFile(), ExcelParserV5.MergeMode.MERGE_BY_CONFIG_GROUP);

        assertTrue(Files.exists(jsonPath));
        String json = Files.readString(jsonPath);
        
        // With MERGE_BY_CONFIG_GROUP, identical flows in same group SHOULD merge
        int flowCount = countOccurrences(json, "\"alarmsAlerts\":");
        assertEquals(1, flowCount, "Should have 1 merged flow within the same config group");
        
        // Verify both alarms are in the same flow
        assertTrue(json.contains("Alarm1"));
        assertTrue(json.contains("Alarm2"));
    }

    private static void createWorkbookWithDifferentConfigGroups(Path target) throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            // Unit Breakdown - Two units with different config groups
            Sheet units = workbook.createSheet("Unit Breakdown");
            Row unitsHeader = units.createRow(2);
            unitsHeader.createCell(0).setCellValue("Facility");
            unitsHeader.createCell(1).setCellValue("Common Unit Name");
            unitsHeader.createCell(2).setCellValue("Nurse Call Configuration Group");
            
            Row unitsRow1 = units.createRow(3);
            unitsRow1.createCell(0).setCellValue("Test Facility");
            unitsRow1.createCell(1).setCellValue("Unit A");
            unitsRow1.createCell(2).setCellValue("GroupA");
            
            Row unitsRow2 = units.createRow(4);
            unitsRow2.createCell(0).setCellValue("Test Facility");
            unitsRow2.createCell(1).setCellValue("Unit B");
            unitsRow2.createCell(2).setCellValue("GroupB");

            // Nurse Call with identical delivery params but different config groups
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
            
            // GroupA
            Row row1 = nurseCalls.createRow(3);
            row1.createCell(0).setCellValue("GroupA");
            row1.createCell(1).setCellValue("GroupA_Alarm");
            row1.createCell(2).setCellValue("High");
            row1.createCell(3).setCellValue("Badge");
            row1.createCell(4).setCellValue("Tone1");
            row1.createCell(5).setCellValue("Accept");
            row1.createCell(6).setCellValue("0");
            row1.createCell(7).setCellValue("Nurse Team");
            
            // GroupB (same delivery params, different group)
            Row row2 = nurseCalls.createRow(4);
            row2.createCell(0).setCellValue("GroupB");
            row2.createCell(1).setCellValue("GroupB_Alarm");
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

    private static void createWorkbookWithSameConfigGroup(Path target) throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            // Unit Breakdown
            Sheet units = workbook.createSheet("Unit Breakdown");
            Row unitsHeader = units.createRow(2);
            unitsHeader.createCell(0).setCellValue("Facility");
            unitsHeader.createCell(1).setCellValue("Common Unit Name");
            unitsHeader.createCell(2).setCellValue("Nurse Call Configuration Group");
            
            Row unitsRow = units.createRow(3);
            unitsRow.createCell(0).setCellValue("Test Facility");
            unitsRow.createCell(1).setCellValue("Unit 1");
            unitsRow.createCell(2).setCellValue("TestGroup");

            // Nurse Call with identical flows in the same config group
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
            
            // Alarm 2 (identical to Alarm 1)
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
