package com.example.exceljson;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.io.OutputStream;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

/**
 * Tests to verify that MERGE_BY_CONFIG_GROUP (Merge Multiple Config Groups) 
 * correctly includes ALL config group names in the merged flow name.
 * 
 * This addresses the specific issue from the problem statement where
 * only one config group name was appearing in the merged flow name.
 */
class MergeMultipleConfigGroupsNameTest {

    @Test
    void mergeMultipleConfigGroups_IncludesAllConfigGroupsInName() throws Exception {
        Path tempDir = Files.createTempDirectory("merge-multiple-name-test");
        Path excelPath = tempDir.resolve("input.xlsx");
        Path jsonPath = tempDir.resolve("output.json");

        // Create workbook with identical flows in different config groups
        createWorkbookWithMultipleConfigGroups(excelPath);

        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(excelPath.toFile());
        parser.writeNurseCallsJson(jsonPath.toFile(), ExcelParserV5.MergeMode.MERGE_BY_CONFIG_GROUP);

        assertTrue(Files.exists(jsonPath));
        String json = Files.readString(jsonPath);
        
        System.out.println("=== MERGE MULTIPLE CONFIG GROUPS NAME TEST ===");
        System.out.println(json);
        System.out.println("==============================================");
        
        // Should have 1 merged flow
        int flowCount = countOccurrences(json, "\"alarmsAlerts\":");
        assertEquals(1, flowCount, "Should have 1 merged flow across all config groups");
        
        // The flow name should include ALL config groups separated by " / "
        // Expected: "SEND NURSECALL | HIGH | Normal Call / Pain Meds / Shower Call | Acute Care NC / OB NC | ..."
        // Note: Config group names preserve their original case from Excel
        assertTrue(json.contains("Acute Care NC / OB NC") || json.contains("OB NC / Acute Care NC"), 
            "Flow name should include all config groups: Acute Care NC and OB NC");
        
        // Verify all alarms are present
        assertTrue(json.contains("Normal Call"));
        assertTrue(json.contains("Pain Meds"));
        assertTrue(json.contains("Shower Call"));
        
        // Verify all units are present
        assertTrue(json.contains("MedSurg"));
        assertTrue(json.contains("ICU"));
        assertTrue(json.contains("LDR"));
    }

    @Test
    void mergeSingleConfigGroup_OnlyIncludesOneConfigGroupInName() throws Exception {
        Path tempDir = Files.createTempDirectory("merge-single-name-test");
        Path excelPath = tempDir.resolve("input.xlsx");
        Path jsonPath = tempDir.resolve("output.json");

        // Create workbook with identical flows in different config groups
        createWorkbookWithMultipleConfigGroups(excelPath);

        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(excelPath.toFile());
        parser.writeNurseCallsJson(jsonPath.toFile(), ExcelParserV5.MergeMode.MERGE_ACROSS_CONFIG_GROUP);

        assertTrue(Files.exists(jsonPath));
        String json = Files.readString(jsonPath);
        
        System.out.println("=== MERGE BY SINGLE CONFIG GROUP NAME TEST ===");
        System.out.println(json);
        System.out.println("==============================================");
        
        // Should have 2 separate flows (one per config group)
        int flowCount = countOccurrences(json, "\"alarmsAlerts\":");
        assertEquals(2, flowCount, "Should have 2 separate flows (one per config group)");
        
        // Each flow name should include only ONE config group
        // Count how many times each config group appears
        // Note: Config group names preserve their original case from Excel
        int acuteCareCount = countOccurrences(json, "Acute Care NC");
        int obCount = countOccurrences(json, "OB NC");
        
        // Each config group should appear exactly once (in its respective flow name)
        assertEquals(1, acuteCareCount, "Acute Care NC should appear once");
        assertEquals(1, obCount, "OB NC should appear once");
    }

    /**
     * Creates a workbook matching the problem statement scenario:
     * - BCH facility with MedSurg, ICU (Acute Care NC) and LDR (OB NC)
     * - Multiple alarms with identical delivery parameters in each config group
     */
    private static void createWorkbookWithMultipleConfigGroups(Path target) throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            // Unit Breakdown
            Sheet units = workbook.createSheet("Unit Breakdown");
            Row unitsHeader = units.createRow(2);
            unitsHeader.createCell(0).setCellValue("Facility");
            unitsHeader.createCell(1).setCellValue("Common Unit Name");
            unitsHeader.createCell(2).setCellValue("Nurse Call Configuration Group");
            unitsHeader.createCell(3).setCellValue("Patient Monitoring Configuration Group");
            unitsHeader.createCell(4).setCellValue("Orders Configuration Group");
            unitsHeader.createCell(5).setCellValue("No Caregiver Group");
            
            // BCH + MedSurg,ICU + Acute Care NC
            Row unitsRow1 = units.createRow(3);
            unitsRow1.createCell(0).setCellValue("BCH");
            unitsRow1.createCell(1).setCellValue("MedSurg,ICU");
            unitsRow1.createCell(2).setCellValue("Acute Care NC");
            unitsRow1.createCell(3).setCellValue("");
            unitsRow1.createCell(4).setCellValue("");
            unitsRow1.createCell(5).setCellValue("");
            
            // BCH + LDR + OB NC
            Row unitsRow2 = units.createRow(4);
            unitsRow2.createCell(0).setCellValue("BCH");
            unitsRow2.createCell(1).setCellValue("LDR");
            unitsRow2.createCell(2).setCellValue("OB NC");
            unitsRow2.createCell(3).setCellValue("");
            unitsRow2.createCell(4).setCellValue("");
            unitsRow2.createCell(5).setCellValue("");

            // Nurse Call - Identical delivery params across groups
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
            
            // Acute Care NC - Normal Call
            Row row1 = nurseCalls.createRow(3);
            row1.createCell(0).setCellValue("Acute Care NC");
            row1.createCell(1).setCellValue("Normal Call");
            row1.createCell(2).setCellValue("High");
            row1.createCell(3).setCellValue("Badge");
            row1.createCell(4).setCellValue("Tone1");
            row1.createCell(5).setCellValue("Accept");
            row1.createCell(6).setCellValue("0");
            row1.createCell(7).setCellValue("CNA");
            row1.createCell(8).setCellValue("60");
            row1.createCell(9).setCellValue("Nurse");
            
            // Acute Care NC - Pain Meds
            Row row2 = nurseCalls.createRow(4);
            row2.createCell(0).setCellValue("Acute Care NC");
            row2.createCell(1).setCellValue("Pain Meds");
            row2.createCell(2).setCellValue("High");
            row2.createCell(3).setCellValue("Badge");
            row2.createCell(4).setCellValue("Tone1");
            row2.createCell(5).setCellValue("Accept");
            row2.createCell(6).setCellValue("0");
            row2.createCell(7).setCellValue("CNA");
            row2.createCell(8).setCellValue("60");
            row2.createCell(9).setCellValue("Nurse");
            
            // OB NC - Shower Call (identical params to Acute Care NC)
            Row row3 = nurseCalls.createRow(5);
            row3.createCell(0).setCellValue("OB NC");
            row3.createCell(1).setCellValue("Shower Call");
            row3.createCell(2).setCellValue("High");
            row3.createCell(3).setCellValue("Badge");
            row3.createCell(4).setCellValue("Tone1");
            row3.createCell(5).setCellValue("Accept");
            row3.createCell(6).setCellValue("0");
            row3.createCell(7).setCellValue("CNA");
            row3.createCell(8).setCellValue("60");
            row3.createCell(9).setCellValue("Nurse");

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
