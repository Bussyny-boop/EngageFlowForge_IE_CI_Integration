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
 * Comprehensive test to verify No Caregiver Group handling in various scenarios.
 * 
 * Scenarios tested:
 * 1. Same facility, same config group, different No Caregiver Groups → Should NOT merge
 * 2. Different facilities, same config group, different No Caregiver Groups → Should NOT merge
 * 3. Same facility, same config group, same No Caregiver Group → Should merge
 */
class NoCaregiverGroupComprehensiveTest {

    @Test
    void comprehensiveNoCaregiverGroupTest() throws Exception {
        Path tempDir = Files.createTempDirectory("nocare-comprehensive-test");
        Path excelPath = tempDir.resolve("input.xlsx");
        Path jsonPath = tempDir.resolve("nurse.json");

        createComprehensiveWorkbook(excelPath);

        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(excelPath.toFile());
        parser.writeNurseCallsJson(jsonPath.toFile(), true); // Enable merge

        assertTrue(Files.exists(jsonPath));
        String json = Files.readString(jsonPath);
        
        // All 4 alarms should be present
        assertTrue(json.contains("Alarm 1"), "Alarm 1 should be in the output");
        assertTrue(json.contains("Alarm 2"), "Alarm 2 should be in the output");
        assertTrue(json.contains("Alarm 3"), "Alarm 3 should be in the output");
        assertTrue(json.contains("Alarm 4"), "Alarm 4 should be in the output");
        
        // Count flows - should be 2:
        // Flow 1: Hospital A Unit 1 + Hospital A Unit 3 + Hospital B Unit 3 (all NoCare-A, merged)
        // Flow 2: Hospital A Unit 2 (NoCare-B, separate)
        int flowCount = countOccurrences(json, "\"alarmsAlerts\":");
        assertEquals(2, flowCount, 
            "Should have 2 flows: " +
            "1) Hospital A Unit 1 + Hospital A Unit 3 + Hospital B Unit 3 all with NoCare-A (merged), " +
            "2) Hospital A Unit 2 with NoCare-B (separate)");
        
        // Verify specific flow contents
        assertTrue(json.contains("\"name\": \"Unit 1\""), "Unit 1 should be in the output");
        assertTrue(json.contains("\"name\": \"Unit 2\""), "Unit 2 should be in the output");
        assertTrue(json.contains("\"name\": \"Unit 3\""), "Unit 3 should be in the output");
        
        // Verify the merged flow has Hospital A Unit 1, Hospital A Unit 3, and Hospital B Unit 3
        String[] flows = json.split("\\{\\s*\"alarmsAlerts\":");
        boolean foundMergedFlow = false;
        for (String flow : flows) {
            // Check if this flow has all three units with NoCare-A
            if (flow.contains("Hospital A") && flow.contains("Hospital B") && 
                flow.contains("Unit 1") && flow.contains("Unit 3")) {
                foundMergedFlow = true;
                assertTrue(flow.contains("Alarm 1") && flow.contains("Alarm 3") && flow.contains("Alarm 4"), 
                    "Merged flow should contain Alarm 1, Alarm 3, and Alarm 4");
                break;
            }
        }
        assertTrue(foundMergedFlow, "Should find a merged flow with Hospital A Unit 1, Hospital A Unit 3, and Hospital B Unit 3");
        
        // Verify the separate flow has only Hospital A Unit 2
        boolean foundSeparateFlow = false;
        for (String flow : flows) {
            if (flow.contains("Unit 2") && !flow.contains("Unit 1") && !flow.contains("Unit 3")) {
                foundSeparateFlow = true;
                assertTrue(flow.contains("Alarm 2"), "Separate flow should contain Alarm 2");
                break;
            }
        }
        assertTrue(foundSeparateFlow, "Should find a separate flow with only Hospital A Unit 2");
    }

    /**
     * Creates a comprehensive test workbook with:
     * - Hospital A, Unit 1, Config Group "SharedGroup", No Caregiver "NoCare-A"
     * - Hospital A, Unit 2, Config Group "SharedGroup", No Caregiver "NoCare-B"
     * - Hospital A, Unit 3, Config Group "SharedGroup", No Caregiver "NoCare-A"
     * - Hospital B, Unit 3, Config Group "SharedGroup", No Caregiver "NoCare-A"
     */
    private static void createComprehensiveWorkbook(Path target) throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            // Unit Breakdown
            Sheet units = workbook.createSheet("Unit Breakdown");
            Row unitsHeader = units.createRow(2);
            unitsHeader.createCell(0).setCellValue("Facility");
            unitsHeader.createCell(1).setCellValue("Common Unit Name");
            unitsHeader.createCell(2).setCellValue("Nurse Call Configuration Group");
            unitsHeader.createCell(3).setCellValue("No Caregiver Group");
            
            // Hospital A, Unit 1, NoCare-A
            Row unitsRow1 = units.createRow(3);
            unitsRow1.createCell(0).setCellValue("Hospital A");
            unitsRow1.createCell(1).setCellValue("Unit 1");
            unitsRow1.createCell(2).setCellValue("SharedGroup");
            unitsRow1.createCell(3).setCellValue("NoCare-A");
            
            // Hospital A, Unit 2, NoCare-B
            Row unitsRow2 = units.createRow(4);
            unitsRow2.createCell(0).setCellValue("Hospital A");
            unitsRow2.createCell(1).setCellValue("Unit 2");
            unitsRow2.createCell(2).setCellValue("SharedGroup");
            unitsRow2.createCell(3).setCellValue("NoCare-B");
            
            // Hospital A, Unit 3, NoCare-A (should merge with Unit 1 and Hospital B Unit 3)
            Row unitsRow3 = units.createRow(5);
            unitsRow3.createCell(0).setCellValue("Hospital A");
            unitsRow3.createCell(1).setCellValue("Unit 3");
            unitsRow3.createCell(2).setCellValue("SharedGroup");
            unitsRow3.createCell(3).setCellValue("NoCare-A");
            
            // Hospital B, Unit 3, NoCare-A (should merge with Hospital A Unit 3)
            Row unitsRow4 = units.createRow(6);
            unitsRow4.createCell(0).setCellValue("Hospital B");
            unitsRow4.createCell(1).setCellValue("Unit 3");
            unitsRow4.createCell(2).setCellValue("SharedGroup");
            unitsRow4.createCell(3).setCellValue("NoCare-A");

            // Nurse Call with 4 alarms, all with identical delivery params
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
            Row nurseRow1 = nurseCalls.createRow(3);
            nurseRow1.createCell(0).setCellValue("SharedGroup");
            nurseRow1.createCell(1).setCellValue("Alarm 1");
            nurseRow1.createCell(2).setCellValue("High");
            nurseRow1.createCell(3).setCellValue("Badge");
            nurseRow1.createCell(4).setCellValue("Tone 1");
            nurseRow1.createCell(5).setCellValue("Accept");
            nurseRow1.createCell(6).setCellValue("0");
            nurseRow1.createCell(7).setCellValue("Nurse Team");
            
            // Alarm 2
            Row nurseRow2 = nurseCalls.createRow(4);
            nurseRow2.createCell(0).setCellValue("SharedGroup");
            nurseRow2.createCell(1).setCellValue("Alarm 2");
            nurseRow2.createCell(2).setCellValue("High");
            nurseRow2.createCell(3).setCellValue("Badge");
            nurseRow2.createCell(4).setCellValue("Tone 1");
            nurseRow2.createCell(5).setCellValue("Accept");
            nurseRow2.createCell(6).setCellValue("0");
            nurseRow2.createCell(7).setCellValue("Nurse Team");
            
            // Alarm 3
            Row nurseRow3 = nurseCalls.createRow(5);
            nurseRow3.createCell(0).setCellValue("SharedGroup");
            nurseRow3.createCell(1).setCellValue("Alarm 3");
            nurseRow3.createCell(2).setCellValue("High");
            nurseRow3.createCell(3).setCellValue("Badge");
            nurseRow3.createCell(4).setCellValue("Tone 1");
            nurseRow3.createCell(5).setCellValue("Accept");
            nurseRow3.createCell(6).setCellValue("0");
            nurseRow3.createCell(7).setCellValue("Nurse Team");
            
            // Alarm 4
            Row nurseRow4 = nurseCalls.createRow(6);
            nurseRow4.createCell(0).setCellValue("SharedGroup");
            nurseRow4.createCell(1).setCellValue("Alarm 4");
            nurseRow4.createCell(2).setCellValue("High");
            nurseRow4.createCell(3).setCellValue("Badge");
            nurseRow4.createCell(4).setCellValue("Tone 1");
            nurseRow4.createCell(5).setCellValue("Accept");
            nurseRow4.createCell(6).setCellValue("0");
            nurseRow4.createCell(7).setCellValue("Nurse Team");

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
