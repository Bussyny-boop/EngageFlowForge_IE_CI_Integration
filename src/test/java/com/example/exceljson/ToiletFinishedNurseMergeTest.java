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
 * Test for the issue: "Toilet Finished" and "Nurse" should merge within same config group
 */
class ToiletFinishedNurseMergeTest {

    @Test
    void toiletFinishedAndNurse_ShouldMergeWithinSameConfigGroup() throws Exception {
        Path tempDir = Files.createTempDirectory("merge-test");
        Path excelPath = tempDir.resolve("input.xlsx");
        Path jsonPath = tempDir.resolve("output.json");

        // Create workbook matching the problem statement
        createWorkbookWithToiletAndNurseAlarms(excelPath);

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
        
        // Toilet Finished and Nurse have identical delivery params:
        // - Priority: Normal
        // - Device: VoceraVCS
        // - t1: Immediate, r1: VAssign: Room PCT/NA
        // - t2: 60 sec, r2: VAssign: Room Nurse
        // They should merge into ONE flow
        
        // However, Toilet Assist has different params (High priority, 120 sec to 2nd recipient)
        // And Pain has different params (different recipients)
        // So we expect 3 flows total:
        // 1. Toilet Assist (alone)
        // 2. Toilet Finished + Nurse (merged)
        // 3. Pain (alone)
        
        // But if they're NOT merging, we'd see 4 flows
        assertEquals(3, flowCount, 
            "Should have 3 flows: Toilet Assist (alone), Toilet Finished+Nurse (merged), Pain (alone)");
        
        // Both alarms should exist in the output
        assertTrue(json.contains("Toilet Finished"));
        assertTrue(json.contains("Nurse"));
    }

    private static void createWorkbookWithToiletAndNurseAlarms(Path target) throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            // Unit Breakdown
            Sheet units = workbook.createSheet("Unit Breakdown");
            Row unitsHeader = units.createRow(2);
            unitsHeader.createCell(0).setCellValue("Facility");
            unitsHeader.createCell(1).setCellValue("Common Unit Name");
            unitsHeader.createCell(2).setCellValue("Nurse Call Configuration Group");
            
            // Multiple units in same config group
            Row unitsRow1 = units.createRow(3);
            unitsRow1.createCell(0).setCellValue("Test Facility");
            unitsRow1.createCell(1).setCellValue("2ICU");
            unitsRow1.createCell(2).setCellValue("General NC");
            
            Row unitsRow2 = units.createRow(4);
            unitsRow2.createCell(0).setCellValue("Test Facility");
            unitsRow2.createCell(1).setCellValue("4AB");
            unitsRow2.createCell(2).setCellValue("General NC");

            // Nurse Call sheet matching the problem statement
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
            
            // Toilet Assist - High priority, 120 sec to 2nd recipient
            Row row1 = nurseCalls.createRow(3);
            row1.createCell(0).setCellValue("General NC");
            row1.createCell(1).setCellValue("Toilet Assist");
            row1.createCell(2).setCellValue("High");
            row1.createCell(3).setCellValue("VoceraVCS");
            row1.createCell(4).setCellValue("Global Setting");
            row1.createCell(5).setCellValue("Accept");
            row1.createCell(6).setCellValue("Immediate");
            row1.createCell(7).setCellValue("VAssign: Room PCT/NA");
            row1.createCell(8).setCellValue("120 sec");
            row1.createCell(9).setCellValue("VAssign: Room Nurse");
            
            // Toilet Finished - Normal priority, 60 sec to 2nd recipient
            Row row2 = nurseCalls.createRow(4);
            row2.createCell(0).setCellValue("General NC");
            row2.createCell(1).setCellValue("Toilet Finished");
            row2.createCell(2).setCellValue("Normal");
            row2.createCell(3).setCellValue("VoceraVCS");
            row2.createCell(4).setCellValue("Global Setting");
            row2.createCell(5).setCellValue("Accept");
            row2.createCell(6).setCellValue("Immediate");
            row2.createCell(7).setCellValue("VAssign: Room PCT/NA");
            row2.createCell(8).setCellValue("60 sec");
            row2.createCell(9).setCellValue("VAssign: Room Nurse");
            
            // Pain - Different recipients
            Row row3 = nurseCalls.createRow(5);
            row3.createCell(0).setCellValue("General NC");
            row3.createCell(1).setCellValue("Pain");
            row3.createCell(2).setCellValue("Normal");
            row3.createCell(3).setCellValue("VoceraVCS");
            row3.createCell(4).setCellValue("Global Setting");
            row3.createCell(5).setCellValue("Accept");
            row3.createCell(6).setCellValue("Immediate");
            row3.createCell(7).setCellValue("VAssign: Room Nurse");
            row3.createCell(8).setCellValue("180 sec");
            row3.createCell(9).setCellValue("VGroup: Charge Nurse");
            
            // Nurse - Same as Toilet Finished (should merge)
            Row row4 = nurseCalls.createRow(6);
            row4.createCell(0).setCellValue("General NC");
            row4.createCell(1).setCellValue("Nurse");
            row4.createCell(2).setCellValue("Normal");
            row4.createCell(3).setCellValue("VoceraVCS");
            row4.createCell(4).setCellValue("Global Setting");
            row4.createCell(5).setCellValue("Accept");
            row4.createCell(6).setCellValue("Immediate");
            row4.createCell(7).setCellValue("VAssign: Room PCT/NA");
            row4.createCell(8).setCellValue("60 sec");
            row4.createCell(9).setCellValue("VAssign: Room Nurse");

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
