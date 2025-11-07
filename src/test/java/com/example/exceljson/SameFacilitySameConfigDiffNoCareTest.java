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
 * Test to verify that units within the SAME facility and SAME config group
 * but DIFFERENT No Caregiver Groups should NOT be merged.
 * 
 * Scenario:
 * - Hospital A, Unit 1, Config Group "SharedGroup", No Caregiver Group "NoCare-A"
 * - Hospital A, Unit 2, Config Group "SharedGroup", No Caregiver Group "NoCare-B"
 * - Both have identical nurse call alarms with same delivery parameters
 * 
 * Expected: Two separate flows (one for Unit 1, one for Unit 2)
 */
class SameFacilitySameConfigDiffNoCareTest {

    @Test
    void sameFacilitySameConfigDifferentNoCareGroupsShouldNotMerge() throws Exception {
        Path tempDir = Files.createTempDirectory("same-facility-diff-nocare-test");
        Path excelPath = tempDir.resolve("input.xlsx");
        Path jsonPath = tempDir.resolve("nurse.json");

        createWorkbookWithSameFacilitySameConfigDifferentNoCare(excelPath);

        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(excelPath.toFile());
        parser.writeNurseCallsJson(jsonPath.toFile(), true); // Enable merge

        assertTrue(Files.exists(jsonPath));
        String json = Files.readString(jsonPath);
        
        // Both alarms should be present
        assertTrue(json.contains("Alarm 1"), "Alarm 1 should be in the output");
        assertTrue(json.contains("Alarm 2"), "Alarm 2 should be in the output");
        
        // Count the number of flows - should be 2 (NOT merged because No Caregiver Groups differ)
        int flowCount = countOccurrences(json, "\"alarmsAlerts\":");
        assertEquals(2, flowCount, 
            "Flows with same facility and same config group but different No Caregiver Groups should NOT be merged");
        
        // Verify that each flow has only one unit
        assertTrue(json.contains("\"name\": \"Unit 1\""), "Unit 1 should be in the output");
        assertTrue(json.contains("\"name\": \"Unit 2\""), "Unit 2 should be in the output");
    }

    /**
     * Creates a workbook with:
     * - SAME facility ("Hospital A")
     * - SAME config group ("SharedGroup")
     * - TWO different units (Unit 1 and Unit 2)
     * - TWO different No Caregiver Groups (NoCare-A and NoCare-B)
     * - TWO nurse call alarms with identical delivery parameters
     */
    private static void createWorkbookWithSameFacilitySameConfigDifferentNoCare(Path target) throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            // Unit Breakdown with SAME facility and SAME config group for different units
            Sheet units = workbook.createSheet("Unit Breakdown");
            Row unitsHeader = units.createRow(2);
            unitsHeader.createCell(0).setCellValue("Facility");
            unitsHeader.createCell(1).setCellValue("Common Unit Name");
            unitsHeader.createCell(2).setCellValue("Nurse Call Configuration Group");
            unitsHeader.createCell(3).setCellValue("No Caregiver Group");
            
            // Unit 1 with No Caregiver Group "NoCare-A" and config group "SharedGroup"
            Row unitsRow1 = units.createRow(3);
            unitsRow1.createCell(0).setCellValue("Hospital A");
            unitsRow1.createCell(1).setCellValue("Unit 1");
            unitsRow1.createCell(2).setCellValue("SharedGroup");
            unitsRow1.createCell(3).setCellValue("NoCare-A");
            
            // Unit 2 with No Caregiver Group "NoCare-B" and config group "SharedGroup"
            Row unitsRow2 = units.createRow(4);
            unitsRow2.createCell(0).setCellValue("Hospital A");
            unitsRow2.createCell(1).setCellValue("Unit 2");
            unitsRow2.createCell(2).setCellValue("SharedGroup");
            unitsRow2.createCell(3).setCellValue("NoCare-B");

            // Nurse Call with identical delivery params and SAME config group
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
            
            // First alarm with SharedGroup
            Row nurseRow1 = nurseCalls.createRow(3);
            nurseRow1.createCell(0).setCellValue("SharedGroup");
            nurseRow1.createCell(1).setCellValue("Alarm 1");
            nurseRow1.createCell(2).setCellValue("High");
            nurseRow1.createCell(3).setCellValue("Badge");
            nurseRow1.createCell(4).setCellValue("Tone 1");
            nurseRow1.createCell(5).setCellValue("Accept");
            nurseRow1.createCell(6).setCellValue("0");
            nurseRow1.createCell(7).setCellValue("Nurse Team");
            
            // Second alarm with SharedGroup (same delivery parameters and config group)
            Row nurseRow2 = nurseCalls.createRow(4);
            nurseRow2.createCell(0).setCellValue("SharedGroup");
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
