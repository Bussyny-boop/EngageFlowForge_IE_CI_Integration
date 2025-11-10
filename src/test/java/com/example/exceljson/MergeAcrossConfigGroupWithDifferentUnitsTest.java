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
 * Tests for MERGE_ACROSS_CONFIG_GROUP with different units.
 * 
 * This reproduces the issue from the problem statement:
 * - Two config groups: "OB NC" and "Acute Care NC"
 * - Different units using these groups
 * - Same No Caregiver Group
 * - Identical delivery parameters
 * 
 * Expected behavior: When MERGE_ACROSS_CONFIG_GROUP is selected,
 * flows should merge into one because they have:
 * - Same delivery parameters (priority, device, ringtone, recipients, etc.)
 * - Same No Caregiver Group (House Supervisor)
 * 
 * The config group and units should not prevent merging.
 */
class MergeAcrossConfigGroupWithDifferentUnitsTest {

    @Test
    void mergeAcrossConfigGroup_ShouldMergeDespiteDifferentUnits() throws Exception {
        Path tempDir = Files.createTempDirectory("merge-across-different-units");
        Path excelPath = tempDir.resolve("input.xlsx");
        Path jsonPath = tempDir.resolve("output.json");

        // Create workbook matching the user's scenario
        createWorkbookMatchingUserScenario(excelPath);

        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(excelPath.toFile());
        parser.writeNurseCallsJson(jsonPath.toFile(), ExcelParserV5.MergeMode.MERGE_ACROSS_CONFIG_GROUP);

        assertTrue(Files.exists(jsonPath));
        String json = Files.readString(jsonPath);
        
        // Debug output
        System.out.println("=== MERGE ACROSS CONFIG GROUP TEST ===");
        System.out.println(json);
        System.out.println("======================================");
        
        // With MERGE_ACROSS_CONFIG_GROUP, flows should merge into ONE
        // even though they're in different config groups with different units
        int flowCount = countOccurrences(json, "\"alarmsAlerts\":");
        assertEquals(1, flowCount, 
            "Should have 1 merged flow across all config groups when delivery params match");
        
        // The merged flow should contain alarms from both config groups
        assertTrue(json.contains("Normal Call"));
        assertTrue(json.contains("Pain Meds"));
    }

    @Test
    void mergeByConfigGroup_ShouldNotMergeDifferentConfigGroups() throws Exception {
        Path tempDir = Files.createTempDirectory("merge-by-config-different-units");
        Path excelPath = tempDir.resolve("input.xlsx");
        Path jsonPath = tempDir.resolve("output.json");

        // Create workbook matching the user's scenario
        createWorkbookMatchingUserScenario(excelPath);

        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(excelPath.toFile());
        parser.writeNurseCallsJson(jsonPath.toFile(), ExcelParserV5.MergeMode.MERGE_BY_CONFIG_GROUP);

        assertTrue(Files.exists(jsonPath));
        String json = Files.readString(jsonPath);
        
        // With MERGE_BY_CONFIG_GROUP, flows should stay separate (one per config group)
        int flowCount = countOccurrences(json, "\"alarmsAlerts\":");
        assertEquals(2, flowCount, 
            "Should have 2 separate flows (one per config group) with MERGE_BY_CONFIG_GROUP");
        
        // Both alarms should be present
        assertTrue(json.contains("Normal Call"));
        assertTrue(json.contains("Pain Meds"));
    }

    /**
     * Creates a workbook matching the user's scenario from the problem statement:
     * - BCH facility with multiple units (MedSurg, ICU, LDR)
     * - Two config groups: "Acute Care NC" and "OB NC"
     * - Same No Caregiver Group: "House Supervisor"
     * - Identical delivery parameters
     */
    private static void createWorkbookMatchingUserScenario(Path target) throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            // Unit Breakdown - matching the problem statement
            Sheet units = workbook.createSheet("Unit Breakdown");
            Row unitsHeader = units.createRow(2);
            unitsHeader.createCell(0).setCellValue("Facility");
            unitsHeader.createCell(1).setCellValue("Common Unit Name");
            unitsHeader.createCell(2).setCellValue("Nurse Call Configuration Group");
            unitsHeader.createCell(3).setCellValue("Patient Monitoring Configuration Group");
            unitsHeader.createCell(4).setCellValue("Orders Configuration Group");
            unitsHeader.createCell(5).setCellValue("No Caregiver Group");
            unitsHeader.createCell(6).setCellValue("Comments");
            
            // BCH + MedSurg,ICU + Acute Care NC
            Row unitsRow1 = units.createRow(3);
            unitsRow1.createCell(0).setCellValue("BCH");
            unitsRow1.createCell(1).setCellValue("MedSurg,ICU");
            unitsRow1.createCell(2).setCellValue("Acute Care NC");
            unitsRow1.createCell(3).setCellValue("General PM");
            unitsRow1.createCell(4).setCellValue("");
            unitsRow1.createCell(5).setCellValue("House Supervisor");
            unitsRow1.createCell(6).setCellValue("");
            
            // BCH + LDR + OB NC
            Row unitsRow2 = units.createRow(4);
            unitsRow2.createCell(0).setCellValue("BCH");
            unitsRow2.createCell(1).setCellValue("LDR");
            unitsRow2.createCell(2).setCellValue("OB NC");
            unitsRow2.createCell(3).setCellValue("OB PM");
            unitsRow2.createCell(4).setCellValue("");
            unitsRow2.createCell(5).setCellValue("House Supervisor");
            unitsRow2.createCell(6).setCellValue("");

            // Nurse Call with identical flows from the problem statement
            Sheet nurseCalls = workbook.createSheet("Nurse call");
            Row nurseHeader = nurseCalls.createRow(2);
            nurseHeader.createCell(0).setCellValue("In scope");
            nurseHeader.createCell(1).setCellValue("Configuration Group");
            nurseHeader.createCell(2).setCellValue("Common Alert or Alarm Name");
            nurseHeader.createCell(3).setCellValue("Sending System Alert Name");
            nurseHeader.createCell(4).setCellValue("Priority");
            nurseHeader.createCell(5).setCellValue("Device - A");
            nurseHeader.createCell(6).setCellValue("Device - B");
            nurseHeader.createCell(7).setCellValue("Ringtone Device - A");
            nurseHeader.createCell(8).setCellValue("Response Options");
            nurseHeader.createCell(9).setCellValue("Break Through DND");
            nurseHeader.createCell(10).setCellValue("Engage 6.6+: Escalate after all declines or 1 decline");
            nurseHeader.createCell(11).setCellValue("Engage/Edge Display Time (Time to Live) (Device - A)");
            nurseHeader.createCell(12).setCellValue("Genie Enunciation");
            nurseHeader.createCell(13).setCellValue("EMDAN Compliant? (Y/N)");
            nurseHeader.createCell(14).setCellValue("Time to 1st Recipient");
            nurseHeader.createCell(15).setCellValue("1st Recipient");
            nurseHeader.createCell(16).setCellValue("Time to 2nd Recipient");
            nurseHeader.createCell(17).setCellValue("2nd Recipient");
            
            // OB NC - Normal Call
            Row row1 = nurseCalls.createRow(3);
            row1.createCell(0).setCellValue("TRUE");
            row1.createCell(1).setCellValue("OB NC");
            row1.createCell(2).setCellValue("Normal Call");
            row1.createCell(3).setCellValue("Normal Call");
            row1.createCell(4).setCellValue("Medium(Edge)");
            row1.createCell(5).setCellValue("iPhone - Edge");
            row1.createCell(6).setCellValue("");
            row1.createCell(7).setCellValue("Vocera Tone 3 Doubled");
            row1.createCell(8).setCellValue("Accept, Escalate, Call Back");
            row1.createCell(9).setCellValue("No");
            row1.createCell(10).setCellValue("1 decline");
            row1.createCell(11).setCellValue("10 minutes");
            row1.createCell(12).setCellValue("");
            row1.createCell(13).setCellValue("No");
            row1.createCell(14).setCellValue("Immediate");
            row1.createCell(15).setCellValue("VAssign: [Room] CNA");
            row1.createCell(16).setCellValue("60 sec");
            row1.createCell(17).setCellValue("VAssign: [Room] Nurse");
            
            // Acute Care NC - Pain Meds (same delivery params)
            Row row2 = nurseCalls.createRow(4);
            row2.createCell(0).setCellValue("TRUE");
            row2.createCell(1).setCellValue("Acute Care NC");
            row2.createCell(2).setCellValue("Pain Meds");
            row2.createCell(3).setCellValue("Pain Meds (Patient is requesting pain medication.)");
            row2.createCell(4).setCellValue("Medium(Edge)");
            row2.createCell(5).setCellValue("iPhone - Edge");
            row2.createCell(6).setCellValue("");
            row2.createCell(7).setCellValue("Vocera Tone 3 Doubled");
            row2.createCell(8).setCellValue("Accept, Escalate, Call Back");
            row2.createCell(9).setCellValue("No");
            row2.createCell(10).setCellValue("1 decline");
            row2.createCell(11).setCellValue("10 minutes");
            row2.createCell(12).setCellValue("");
            row2.createCell(13).setCellValue("No");
            row2.createCell(14).setCellValue("Immediate");
            row2.createCell(15).setCellValue("VAssign: [Room] CNA");
            row2.createCell(16).setCellValue("60 sec");
            row2.createCell(17).setCellValue("VAssign: [Room] Nurse");

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
