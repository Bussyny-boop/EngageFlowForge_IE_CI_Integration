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
 * Manual verification test demonstrating the fix for the No Caregiver Group merge issue.
 * 
 * Scenario: Two hospitals (Hospital A and Hospital B) share the same "Critical Care" 
 * configuration group, but have different No Caregiver Groups. When alarms with identical
 * delivery parameters are defined, they should create SEPARATE flows for each hospital,
 * not a single merged flow.
 */
class ManualVerificationTest {

    @Test
    void demonstrateFix() throws Exception {
        Path tempDir = Files.createTempDirectory("manual-verification");
        Path excelPath = tempDir.resolve("input.xlsx");
        Path jsonPath = tempDir.resolve("nurse.json");

        createRealWorldScenario(excelPath);

        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(excelPath.toFile());
        parser.writeNurseCallsJson(jsonPath.toFile(), true); // Enable merge

        assertTrue(Files.exists(jsonPath));
        String json = Files.readString(jsonPath);
        
        System.out.println("\n=== MANUAL VERIFICATION TEST ===");
        System.out.println("\nScenario:");
        System.out.println("- Two hospitals share the 'Critical Care' config group");
        System.out.println("- Hospital A has No Caregiver Group 'No-Care-Team-A'");
        System.out.println("- Hospital B has No Caregiver Group 'No-Care-Team-B'");
        System.out.println("- Two alarms with identical delivery parameters:");
        System.out.println("  1. Code Blue");
        System.out.println("  2. Rapid Response");
        
        System.out.println("\nGenerated JSON:");
        System.out.println(json);
        
        // Verify both alarms are present
        assertTrue(json.contains("Code Blue"), "Code Blue alarm should be in the output");
        assertTrue(json.contains("Rapid Response"), "Rapid Response alarm should be in the output");
        
        // Count the number of flows
        int flowCount = countOccurrences(json, "\"alarmsAlerts\":");
        
        System.out.println("\n=== VERIFICATION RESULTS ===");
        System.out.println("Number of flows generated: " + flowCount);
        System.out.println("Expected: 2 separate flows (one for Hospital A, one for Hospital B)");
        System.out.println("Result: " + (flowCount == 2 ? "PASS ✓" : "FAIL ✗"));
        
        // Verify Hospital A has its own flow
        assertTrue(json.contains("Hospital A"), "Hospital A should have its own flow");
        
        // Verify Hospital B has its own flow
        assertTrue(json.contains("Hospital B"), "Hospital B should have its own flow");
        
        // The flows should be separate, not merged
        assertEquals(2, flowCount, 
            "Two separate flows should be created (one per No Caregiver Group), not merged into one");
        
        System.out.println("\nFix verified successfully!");
        System.out.println("Before fix: Would have created 1 merged flow for both hospitals");
        System.out.println("After fix: Creates 2 separate flows, one for each No Caregiver Group");
    }

    /**
     * Creates a realistic scenario mimicking the user's problem:
     * - Two hospitals with the same config group
     * - Different No Caregiver Groups
     * - Two alarms with identical delivery parameters
     */
    private static void createRealWorldScenario(Path target) throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            // Unit Breakdown
            Sheet units = workbook.createSheet("Unit Breakdown");
            Row unitsHeader = units.createRow(2);
            unitsHeader.createCell(0).setCellValue("Facility");
            unitsHeader.createCell(1).setCellValue("Common Unit Name");
            unitsHeader.createCell(2).setCellValue("Nurse Call Configuration Group");
            unitsHeader.createCell(3).setCellValue("No Caregiver Group");
            
            // Hospital A - ICU with Critical Care config group
            Row unitsRow1 = units.createRow(3);
            unitsRow1.createCell(0).setCellValue("Hospital A");
            unitsRow1.createCell(1).setCellValue("ICU");
            unitsRow1.createCell(2).setCellValue("Critical Care");
            unitsRow1.createCell(3).setCellValue("No-Care-Team-A");
            
            // Hospital B - ICU with Critical Care config group (same as Hospital A)
            Row unitsRow2 = units.createRow(4);
            unitsRow2.createCell(0).setCellValue("Hospital B");
            unitsRow2.createCell(1).setCellValue("ICU");
            unitsRow2.createCell(2).setCellValue("Critical Care");
            unitsRow2.createCell(3).setCellValue("No-Care-Team-B");
            
            // Nurse Call - Two alarms with identical delivery parameters
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
            
            // Code Blue alarm
            Row nurseRow1 = nurseCalls.createRow(3);
            nurseRow1.createCell(0).setCellValue("Critical Care");
            nurseRow1.createCell(1).setCellValue("Code Blue");
            nurseRow1.createCell(2).setCellValue("High");
            nurseRow1.createCell(3).setCellValue("Badge");
            nurseRow1.createCell(4).setCellValue("Alert Tone");
            nurseRow1.createCell(5).setCellValue("Accept");
            nurseRow1.createCell(6).setCellValue("0");
            nurseRow1.createCell(7).setCellValue("Primary RN");
            
            // Rapid Response alarm (identical delivery parameters)
            Row nurseRow2 = nurseCalls.createRow(4);
            nurseRow2.createCell(0).setCellValue("Critical Care");
            nurseRow2.createCell(1).setCellValue("Rapid Response");
            nurseRow2.createCell(2).setCellValue("High");
            nurseRow2.createCell(3).setCellValue("Badge");
            nurseRow2.createCell(4).setCellValue("Alert Tone");
            nurseRow2.createCell(5).setCellValue("Accept");
            nurseRow2.createCell(6).setCellValue("0");
            nurseRow2.createCell(7).setCellValue("Primary RN");
            
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
