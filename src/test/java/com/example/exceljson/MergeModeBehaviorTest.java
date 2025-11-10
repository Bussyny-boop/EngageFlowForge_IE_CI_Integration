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
 * Tests to verify that the merge modes behave as expected:
 * - MERGE_BY_CONFIG_GROUP should merge flows ACROSS all config groups (ignore config group boundaries)
 * - MERGE_ACROSS_CONFIG_GROUP should merge flows WITHIN each config group (respect config group boundaries)
 */
class MergeModeBehaviorTest {

    @Test
    void mergeByConfigGroup_MergesAcrossAllConfigGroups() throws Exception {
        Path tempDir = Files.createTempDirectory("merge-behavior-test");
        Path excelPath = tempDir.resolve("input.xlsx");
        Path jsonPath = tempDir.resolve("output.json");

        // Create workbook with identical flows in two different config groups
        createTestWorkbook(excelPath);

        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(excelPath.toFile());
        
        // Use MERGE_BY_CONFIG_GROUP - should merge across all config groups
        parser.writeNurseCallsJson(jsonPath.toFile(), ExcelParserV5.MergeMode.MERGE_BY_CONFIG_GROUP);

        assertTrue(Files.exists(jsonPath));
        String json = Files.readString(jsonPath);
        
        // Should have only 1 merged flow containing alarms from both config groups
        int flowCount = countOccurrences(json, "\"alarmsAlerts\":");
        assertEquals(1, flowCount, "MERGE_BY_CONFIG_GROUP should create 1 merged flow across all config groups");
        
        // The flow should contain alarms from both config groups
        assertTrue(json.contains("ConfigGroup1_Alarm"), "Should contain alarm from ConfigGroup1");
        assertTrue(json.contains("ConfigGroup2_Alarm"), "Should contain alarm from ConfigGroup2");
    }

    @Test
    void mergeAcrossConfigGroup_MergesWithinEachConfigGroup() throws Exception {
        Path tempDir = Files.createTempDirectory("merge-behavior-test");
        Path excelPath = tempDir.resolve("input.xlsx");
        Path jsonPath = tempDir.resolve("output.json");

        // Create workbook with identical flows in two different config groups
        createTestWorkbook(excelPath);

        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(excelPath.toFile());
        
        // Use MERGE_ACROSS_CONFIG_GROUP - should keep config groups separate
        parser.writeNurseCallsJson(jsonPath.toFile(), ExcelParserV5.MergeMode.MERGE_ACROSS_CONFIG_GROUP);

        assertTrue(Files.exists(jsonPath));
        String json = Files.readString(jsonPath);
        
        // Should have 2 separate flows (one per config group)
        int flowCount = countOccurrences(json, "\"alarmsAlerts\":");
        assertEquals(2, flowCount, "MERGE_ACROSS_CONFIG_GROUP should create 2 separate flows (one per config group)");
        
        // Each alarm should appear in the output
        assertTrue(json.contains("ConfigGroup1_Alarm"), "Should contain alarm from ConfigGroup1");
        assertTrue(json.contains("ConfigGroup2_Alarm"), "Should contain alarm from ConfigGroup2");
    }

    @Test
    void none_KeepsAllFlowsSeparate() throws Exception {
        Path tempDir = Files.createTempDirectory("merge-behavior-test");
        Path excelPath = tempDir.resolve("input.xlsx");
        Path jsonPath = tempDir.resolve("output.json");

        // Create workbook with identical flows in two different config groups
        createTestWorkbook(excelPath);

        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(excelPath.toFile());
        
        // Use NONE - should keep all flows separate
        parser.writeNurseCallsJson(jsonPath.toFile(), ExcelParserV5.MergeMode.NONE);

        assertTrue(Files.exists(jsonPath));
        String json = Files.readString(jsonPath);
        
        // Should have 2 separate flows (no merging)
        int flowCount = countOccurrences(json, "\"alarmsAlerts\":");
        assertEquals(2, flowCount, "NONE mode should create 2 separate flows (no merging)");
    }

    // Helper method to create a test workbook with identical flows in different config groups
    private void createTestWorkbook(Path excelPath) throws Exception {
        XSSFWorkbook wb = new XSSFWorkbook();
        
        // Create Unit Breakdown sheet - header on row 2
        Sheet unitSheet = wb.createSheet("Unit Breakdown");
        Row unitHeader = unitSheet.createRow(2);
        unitHeader.createCell(0).setCellValue("Facility");
        unitHeader.createCell(1).setCellValue("Common Unit Name");
        unitHeader.createCell(2).setCellValue("Nurse Call Configuration Group");
        
        Row unit1 = unitSheet.createRow(3);
        unit1.createCell(0).setCellValue("Hospital");
        unit1.createCell(1).setCellValue("Unit1");
        unit1.createCell(2).setCellValue("ConfigGroup1");
        
        Row unit2 = unitSheet.createRow(4);
        unit2.createCell(0).setCellValue("Hospital");
        unit2.createCell(1).setCellValue("Unit2");
        unit2.createCell(2).setCellValue("ConfigGroup2");
        
        // Create Nurse call sheet - header on row 2 with identical delivery parameters
        Sheet nurseSheet = wb.createSheet("Nurse call");
        Row ncHeader = nurseSheet.createRow(2);
        ncHeader.createCell(0).setCellValue("Configuration Group");
        ncHeader.createCell(1).setCellValue("Common Alert or Alarm Name");
        ncHeader.createCell(2).setCellValue("Priority");
        ncHeader.createCell(3).setCellValue("Device - A");
        ncHeader.createCell(4).setCellValue("Ringtone Device - A");
        ncHeader.createCell(5).setCellValue("Response Options");
        ncHeader.createCell(6).setCellValue("Time to 1st Recipient");
        ncHeader.createCell(7).setCellValue("1st Recipient");
        
        // Alarm from ConfigGroup1 with specific delivery parameters
        Row nc1 = nurseSheet.createRow(3);
        nc1.createCell(0).setCellValue("ConfigGroup1");
        nc1.createCell(1).setCellValue("ConfigGroup1_Alarm");
        nc1.createCell(2).setCellValue("High");
        nc1.createCell(3).setCellValue("Badge");
        nc1.createCell(4).setCellValue("Alert");
        nc1.createCell(5).setCellValue("Accept");
        nc1.createCell(6).setCellValue("0");
        nc1.createCell(7).setCellValue("Nurse");
        
        // Alarm from ConfigGroup2 with IDENTICAL delivery parameters
        Row nc2 = nurseSheet.createRow(4);
        nc2.createCell(0).setCellValue("ConfigGroup2");
        nc2.createCell(1).setCellValue("ConfigGroup2_Alarm");
        nc2.createCell(2).setCellValue("High");
        nc2.createCell(3).setCellValue("Badge");
        nc2.createCell(4).setCellValue("Alert");
        nc2.createCell(5).setCellValue("Accept");
        nc2.createCell(6).setCellValue("0");
        nc2.createCell(7).setCellValue("Nurse");
        
        try (OutputStream out = Files.newOutputStream(excelPath)) {
            wb.write(out);
        }
        wb.close();
    }

    private int countOccurrences(String text, String substring) {
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(substring, index)) != -1) {
            count++;
            index += substring.length();
        }
        return count;
    }
}
