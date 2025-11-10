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
 * Test for the fix: Flows with different units but same config group and delivery params should merge.
 * This tests the scenario from the issue: "Toilet Finished" and "Nurse" should merge
 * even though they apply to different sets of units.
 */
class MergeByConfigGroupDifferentUnitsTest {

    @Test
    void mergeByConfigGroup_DifferentUnits_ShouldMerge() throws Exception {
        Path tempDir = Files.createTempDirectory("merge-test");
        Path excelPath = tempDir.resolve("input.xlsx");
        Path jsonPath = tempDir.resolve("output.json");

        // Create workbook with flows that have:
        // - Same config group ("General NC")
        // - Same delivery parameters (Normal priority, same recipients/timing)
        // - Different unit lists
        createWorkbookWithDifferentUnits(excelPath);

        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(excelPath.toFile());
        parser.writeNurseCallsJson(jsonPath.toFile(), ExcelParserV5.MergeMode.MERGE_BY_CONFIG_GROUP);

        assertTrue(Files.exists(jsonPath));
        String json = Files.readString(jsonPath);
        
        System.out.println("=== Generated JSON ===");
        System.out.println(json);
        System.out.println("======================");
        
        // Count flows
        int flowCount = countOccurrences(json, "\"alarmsAlerts\":");
        System.out.println("Flow count: " + flowCount);
        
        // Toilet Finished and Nurse have:
        // - Same config group: "General NC"
        // - Same delivery params: Normal priority, VoceraVCS, PCT/NA -> Nurse
        // - Different units: Toilet Finished has 4 units, Nurse has 6 units
        // They SHOULD merge into ONE flow with combined units (6 total unique units)
        
        // Expected flows:
        // 1. Toilet Finished + Nurse (merged) - 6 units combined
        // Total: 1 flow
        
        assertEquals(1, flowCount, 
            "Should have 1 merged flow (Toilet Finished + Nurse)");
        
        // Both alarms should exist in the same flow
        assertTrue(json.contains("Toilet Finished"));
        assertTrue(json.contains("Nurse"));
        
        // Verify that both alarms appear in the same alarmsAlerts array
        // The JSON should contain: "alarmsAlerts": [ "Toilet Finished", "Nurse" ]
        assertTrue(json.contains("\"alarmsAlerts\": ["), "Should have alarmsAlerts array");
        
        // Check that both alarms appear close to each other (within the same flow)
        int toiletIndex = json.indexOf("Toilet Finished");
        int nurseIndex = json.indexOf("Nurse");
        assertTrue(toiletIndex > 0, "Should contain Toilet Finished");
        assertTrue(nurseIndex > 0, "Should contain Nurse");
        
        // They should be close together (within 100 characters) since they're in the same array
        int distance = Math.abs(toiletIndex - nurseIndex);
        assertTrue(distance < 200, "Both alarms should be close together in the same alarmsAlerts array (distance: " + distance + ")");
        
        // Verify all 6 unique units are present
        assertTrue(json.contains("Unit1"), "Should contain Unit1");
        assertTrue(json.contains("Unit2"), "Should contain Unit2");
        assertTrue(json.contains("Unit3"), "Should contain Unit3");
        assertTrue(json.contains("Unit4"), "Should contain Unit4");
        assertTrue(json.contains("Unit5"), "Should contain Unit5");
        assertTrue(json.contains("Unit6"), "Should contain Unit6");
    }

    @Test
    void mergeAll_DifferentUnits_StillMerges() throws Exception {
        Path tempDir = Files.createTempDirectory("merge-all-test");
        Path excelPath = tempDir.resolve("input.xlsx");
        Path jsonPath = tempDir.resolve("output.json");

        createWorkbookWithDifferentUnits(excelPath);

        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(excelPath.toFile());
        parser.writeNurseCallsJson(jsonPath.toFile(), ExcelParserV5.MergeMode.MERGE_ACROSS_CONFIG_GROUP);

        assertTrue(Files.exists(jsonPath));
        String json = Files.readString(jsonPath);
        
        // With MERGE_ALL, identical delivery params should merge regardless of units or config groups
        // (This was the previous behavior and should still work)
        int flowCount = countOccurrences(json, "\"alarmsAlerts\":");
        assertEquals(1, flowCount, "MERGE_ALL should merge flows with same delivery params");
    }

    @Test
    void noMerge_DifferentUnits_KeepsSeparate() throws Exception {
        Path tempDir = Files.createTempDirectory("no-merge-test");
        Path excelPath = tempDir.resolve("input.xlsx");
        Path jsonPath = tempDir.resolve("output.json");

        createWorkbookWithDifferentUnits(excelPath);

        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(excelPath.toFile());
        parser.writeNurseCallsJson(jsonPath.toFile(), ExcelParserV5.MergeMode.NONE);

        assertTrue(Files.exists(jsonPath));
        String json = Files.readString(jsonPath);
        
        // With NONE, each alarm should have its own flow
        int flowCount = countOccurrences(json, "\"alarmsAlerts\":");
        assertEquals(2, flowCount, "NONE should keep each alarm in separate flows");
    }

    private static void createWorkbookWithDifferentUnits(Path target) throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            // Unit Breakdown - Create 6 units all in "General NC" config group
            Sheet units = workbook.createSheet("Unit Breakdown");
            Row unitsHeader = units.createRow(2);
            unitsHeader.createCell(0).setCellValue("Facility");
            unitsHeader.createCell(1).setCellValue("Common Unit Name");
            unitsHeader.createCell(2).setCellValue("Nurse Call Configuration Group");
            
            // Create 6 units - Toilet Finished uses first 4, Nurse uses all 6
            String[] unitNames = {"Unit1", "Unit2", "Unit3", "Unit4", "Unit5", "Unit6"};
            for (int i = 0; i < unitNames.length; i++) {
                Row row = units.createRow(3 + i);
                row.createCell(0).setCellValue("Test Facility");
                row.createCell(1).setCellValue(unitNames[i]);
                row.createCell(2).setCellValue("General NC");
            }

            // Nurse Call sheet
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
            nurseHeader.createCell(8).setCellValue("Time to 2nd Recipient");
            nurseHeader.createCell(9).setCellValue("2nd Recipient");
            nurseHeader.createCell(10).setCellValue("Notes");
            
            // Toilet Finished - applies to Units 1-4
            // Same delivery params as Nurse
            Row row1 = nurseCalls.createRow(3);
            row1.createCell(0).setCellValue("General NC");
            row1.createCell(1).setCellValue("Toilet Finished");
            row1.createCell(2).setCellValue("Normal");
            row1.createCell(3).setCellValue("VoceraVCS");
            row1.createCell(4).setCellValue("Global Setting");
            row1.createCell(5).setCellValue("Accept");
            row1.createCell(6).setCellValue("Immediate");
            row1.createCell(7).setCellValue("VAssign: Room PCT/NA");
            row1.createCell(8).setCellValue("60 sec");
            row1.createCell(9).setCellValue("VAssign: Room Nurse");
            row1.createCell(10).setCellValue("Unit1,Unit2,Unit3,Unit4");
            
            // Nurse - applies to Units 1-6 (superset)
            // Same delivery params as Toilet Finished
            Row row2 = nurseCalls.createRow(4);
            row2.createCell(0).setCellValue("General NC");
            row2.createCell(1).setCellValue("Nurse");
            row2.createCell(2).setCellValue("Normal");
            row2.createCell(3).setCellValue("VoceraVCS");
            row2.createCell(4).setCellValue("Global Setting");
            row2.createCell(5).setCellValue("Accept");
            row2.createCell(6).setCellValue("Immediate");
            row2.createCell(7).setCellValue("VAssign: Room PCT/NA");
            row2.createCell(8).setCellValue("60 sec");
            row2.createCell(9).setCellValue("VAssign: Room Nurse");
            row2.createCell(10).setCellValue("Unit1,Unit2,Unit3,Unit4,Unit5,Unit6");

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
