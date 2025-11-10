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
 * Tests for the merge features.
 * - MERGE_BY_CONFIG_GROUP: Merges flows with identical delivery parameters ONLY within the same config group
 * - MERGE_ACROSS_CONFIG_GROUP: Merges flows across config groups if No Caregiver Group matches
 */
class MergeByConfigGroupTest {

    @Test
    void mergeByConfigGroup_SeparatesFlowsByConfigGroup() throws Exception {
        Path tempDir = Files.createTempDirectory("merge-by-config-test");
        Path excelPath = tempDir.resolve("input.xlsx");
        Path jsonPath = tempDir.resolve("output.json");

        // Create workbook with identical flows in different config groups
        createWorkbookWithIdenticalFlowsInDifferentGroups(excelPath);

        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(excelPath.toFile());
        parser.writeNurseCallsJson(jsonPath.toFile(), ExcelParserV5.MergeMode.MERGE_BY_CONFIG_GROUP);

        assertTrue(Files.exists(jsonPath));
        String json = Files.readString(jsonPath);
        
        // With MERGE_BY_CONFIG_GROUP, we should have 2 separate flows (one per config group)
        // even though the delivery parameters are identical
        int flowCount = countOccurrences(json, "\"alarmsAlerts\":");
        assertEquals(2, flowCount, "Should have 2 separate flows (one per config group)");
        
        // Each flow should contain only alarms from its config group
        assertTrue(json.contains("Group1_Alarm1"));
        assertTrue(json.contains("Group1_Alarm2"));
        assertTrue(json.contains("Group2_Alarm1"));
        assertTrue(json.contains("Group2_Alarm2"));
    }

    @Test
    void mergeByConfigGroup_MergesWithinSameConfigGroup() throws Exception {
        Path tempDir = Files.createTempDirectory("merge-by-config-test");
        Path excelPath = tempDir.resolve("input.xlsx");
        Path jsonPath = tempDir.resolve("output.json");

        // Create workbook with identical flows in the same config group
        createWorkbookWithIdenticalFlowsInSameGroup(excelPath);

        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(excelPath.toFile());
        parser.writeNurseCallsJson(jsonPath.toFile(), ExcelParserV5.MergeMode.MERGE_BY_CONFIG_GROUP);

        assertTrue(Files.exists(jsonPath));
        String json = Files.readString(jsonPath);
        
        // With MERGE_BY_CONFIG_GROUP, identical flows in the same config group should merge
        int flowCount = countOccurrences(json, "\"alarmsAlerts\":");
        assertEquals(1, flowCount, "Should have 1 merged flow within the same config group");
        
        // The flow should contain both alarms
        assertTrue(json.contains("Alarm1"));
        assertTrue(json.contains("Alarm2"));
    }

    @Test
    void mergeAcrossConfigGroup_MergesAcrossConfigGroups() throws Exception {
        Path tempDir = Files.createTempDirectory("merge-across-test");
        Path excelPath = tempDir.resolve("input.xlsx");
        Path jsonPath = tempDir.resolve("output.json");

        // Create workbook with identical flows in different config groups
        createWorkbookWithIdenticalFlowsInDifferentGroups(excelPath);

        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(excelPath.toFile());
        parser.writeNurseCallsJson(jsonPath.toFile(), ExcelParserV5.MergeMode.MERGE_ACROSS_CONFIG_GROUP);

        assertTrue(Files.exists(jsonPath));
        String json = Files.readString(jsonPath);
        
        // With MERGE_ACROSS_CONFIG_GROUP, flows should merge across config groups
        int flowCount = countOccurrences(json, "\"alarmsAlerts\":");
        assertEquals(1, flowCount, "Should have 1 merged flow across all config groups");
        
        // The flow should contain all alarms from both groups
        assertTrue(json.contains("Group1_Alarm1"));
        assertTrue(json.contains("Group1_Alarm2"));
        assertTrue(json.contains("Group2_Alarm1"));
        assertTrue(json.contains("Group2_Alarm2"));
    }

    @Test
    void noMerge_KeepsAllFlowsSeparate() throws Exception {
        Path tempDir = Files.createTempDirectory("no-merge-test");
        Path excelPath = tempDir.resolve("input.xlsx");
        Path jsonPath = tempDir.resolve("output.json");

        // Create workbook with identical flows in different config groups
        createWorkbookWithIdenticalFlowsInDifferentGroups(excelPath);

        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(excelPath.toFile());
        parser.writeNurseCallsJson(jsonPath.toFile(), ExcelParserV5.MergeMode.NONE);

        assertTrue(Files.exists(jsonPath));
        String json = Files.readString(jsonPath);
        
        // With NONE, each alarm should have its own flow
        int flowCount = countOccurrences(json, "\"alarmsAlerts\":");
        assertEquals(4, flowCount, "Should have 4 separate flows (one per alarm)");
    }

    @Test
    void backwardCompatibility_BooleanParameterStillWorks() throws Exception {
        Path tempDir = Files.createTempDirectory("compat-test");
        Path excelPath = tempDir.resolve("input.xlsx");
        Path jsonPathTrue = tempDir.resolve("output-true.json");
        Path jsonPathFalse = tempDir.resolve("output-false.json");

        createWorkbookWithIdenticalFlowsInDifferentGroups(excelPath);

        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(excelPath.toFile());
        
        // Test that boolean parameter still works (for backward compatibility)
        parser.writeNurseCallsJson(jsonPathTrue.toFile(), true);
        parser.writeNurseCallsJson(jsonPathFalse.toFile(), false);

        String jsonTrue = Files.readString(jsonPathTrue);
        String jsonFalse = Files.readString(jsonPathFalse);
        
        // true should merge across config groups (MERGE_ACROSS_CONFIG_GROUP mode)
        assertEquals(1, countOccurrences(jsonTrue, "\"alarmsAlerts\":"), 
            "Boolean true should behave like MERGE_ACROSS_CONFIG_GROUP");
        
        // false should not merge (NONE mode)
        assertEquals(4, countOccurrences(jsonFalse, "\"alarmsAlerts\":"), 
            "Boolean false should behave like NONE");
    }

    /**
     * Creates a workbook with identical flows in different config groups.
     * IMPORTANT: Uses the SAME unit for both groups to ensure the only difference is the config group.
     */
    private static void createWorkbookWithIdenticalFlowsInDifferentGroups(Path target) throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            // Unit Breakdown - Same unit, different config groups
            Sheet units = workbook.createSheet("Unit Breakdown");
            Row unitsHeader = units.createRow(2);
            unitsHeader.createCell(0).setCellValue("Facility");
            unitsHeader.createCell(1).setCellValue("Common Unit Name");
            unitsHeader.createCell(2).setCellValue("Nurse Call Configuration Group");
            
            Row unitsRow1 = units.createRow(3);
            unitsRow1.createCell(0).setCellValue("Test Facility");
            unitsRow1.createCell(1).setCellValue("Test Unit");
            unitsRow1.createCell(2).setCellValue("Group1");
            
            Row unitsRow2 = units.createRow(4);
            unitsRow2.createCell(0).setCellValue("Test Facility");
            unitsRow2.createCell(1).setCellValue("Test Unit");
            unitsRow2.createCell(2).setCellValue("Group2");

            // Nurse Call with identical flows in different groups
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
            
            // Group1 - Alarm 1
            Row row1 = nurseCalls.createRow(3);
            row1.createCell(0).setCellValue("Group1");
            row1.createCell(1).setCellValue("Group1_Alarm1");
            row1.createCell(2).setCellValue("High");
            row1.createCell(3).setCellValue("Badge");
            row1.createCell(4).setCellValue("Tone1");
            row1.createCell(5).setCellValue("Accept");
            row1.createCell(6).setCellValue("0");
            row1.createCell(7).setCellValue("Nurse Team");
            
            // Group1 - Alarm 2
            Row row2 = nurseCalls.createRow(4);
            row2.createCell(0).setCellValue("Group1");
            row2.createCell(1).setCellValue("Group1_Alarm2");
            row2.createCell(2).setCellValue("High");
            row2.createCell(3).setCellValue("Badge");
            row2.createCell(4).setCellValue("Tone1");
            row2.createCell(5).setCellValue("Accept");
            row2.createCell(6).setCellValue("0");
            row2.createCell(7).setCellValue("Nurse Team");
            
            // Group2 - Alarm 1 (identical delivery params to Group1)
            Row row3 = nurseCalls.createRow(5);
            row3.createCell(0).setCellValue("Group2");
            row3.createCell(1).setCellValue("Group2_Alarm1");
            row3.createCell(2).setCellValue("High");
            row3.createCell(3).setCellValue("Badge");
            row3.createCell(4).setCellValue("Tone1");
            row3.createCell(5).setCellValue("Accept");
            row3.createCell(6).setCellValue("0");
            row3.createCell(7).setCellValue("Nurse Team");
            
            // Group2 - Alarm 2 (identical delivery params to Group1)
            Row row4 = nurseCalls.createRow(6);
            row4.createCell(0).setCellValue("Group2");
            row4.createCell(1).setCellValue("Group2_Alarm2");
            row4.createCell(2).setCellValue("High");
            row4.createCell(3).setCellValue("Badge");
            row4.createCell(4).setCellValue("Tone1");
            row4.createCell(5).setCellValue("Accept");
            row4.createCell(6).setCellValue("0");
            row4.createCell(7).setCellValue("Nurse Team");

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
     * Creates a workbook with identical flows in the same config group.
     */
    private static void createWorkbookWithIdenticalFlowsInSameGroup(Path target) throws Exception {
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

            // Nurse Call with identical flows in the same group
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
