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
 * Tests that verify "Sending Name" is NOT part of merge criteria,
 * while all other delivery-related columns ARE part of merge criteria.
 */
class MergeSendingNameExclusionTest {

    @Test
    void mergeByConfigGroup_MergesDespiteDifferentSendingName() throws Exception {
        Path tempDir = Files.createTempDirectory("merge-sending-name-test");
        Path excelPath = tempDir.resolve("input.xlsx");
        Path jsonPath = tempDir.resolve("output.json");

        // Create workbook with identical flows except for Sending Name
        createWorkbookWithDifferentSendingNames(excelPath);

        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(excelPath.toFile());
        parser.writeNurseCallsJson(jsonPath.toFile(), ExcelParserV5.MergeMode.MERGE_BY_CONFIG_GROUP);

        assertTrue(Files.exists(jsonPath));
        String json = Files.readString(jsonPath);
        
        // With MERGE_BY_CONFIG_GROUP, flows with different Sending Names SHOULD merge
        // because Sending Name is not part of the merge criteria
        int flowCount = countOccurrences(json, "\"alarmsAlerts\":");
        assertEquals(1, flowCount, "Should have 1 merged flow (Sending Name should be ignored)");
        
        // The flow should contain both alarms
        assertTrue(json.contains("Alarm1"));
        assertTrue(json.contains("Alarm2"));
    }

    @Test
    void mergeAll_MergesDespiteDifferentSendingName() throws Exception {
        Path tempDir = Files.createTempDirectory("merge-all-sending-name-test");
        Path excelPath = tempDir.resolve("input.xlsx");
        Path jsonPath = tempDir.resolve("output.json");

        // Create workbook with identical flows except for Sending Name
        createWorkbookWithDifferentSendingNames(excelPath);

        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(excelPath.toFile());
        parser.writeNurseCallsJson(jsonPath.toFile(), ExcelParserV5.MergeMode.MERGE_ACROSS_CONFIG_GROUP);

        assertTrue(Files.exists(jsonPath));
        String json = Files.readString(jsonPath);
        
        // With MERGE_ALL, flows with different Sending Names should still merge
        int flowCount = countOccurrences(json, "\"alarmsAlerts\":");
        assertEquals(1, flowCount, "Should have 1 merged flow (Sending Name should be ignored)");
    }

    @Test
    void mergeByConfigGroup_SeparatesFlowsByDeviceB() throws Exception {
        Path tempDir = Files.createTempDirectory("merge-deviceb-test");
        Path excelPath = tempDir.resolve("input.xlsx");
        Path jsonPath = tempDir.resolve("output.json");

        // Create workbook with identical flows except for Device B
        createWorkbookWithDifferentDeviceB(excelPath);

        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(excelPath.toFile());
        parser.writeNurseCallsJson(jsonPath.toFile(), ExcelParserV5.MergeMode.MERGE_BY_CONFIG_GROUP);

        assertTrue(Files.exists(jsonPath));
        String json = Files.readString(jsonPath);
        
        // Flows with different Device B should NOT merge
        int flowCount = countOccurrences(json, "\"alarmsAlerts\":");
        assertEquals(2, flowCount, "Should have 2 separate flows (Device B is part of merge criteria)");
    }

    @Test
    void mergeByConfigGroup_SeparatesFlowsByBreakThroughDND() throws Exception {
        Path tempDir = Files.createTempDirectory("merge-breakthrough-test");
        Path excelPath = tempDir.resolve("input.xlsx");
        Path jsonPath = tempDir.resolve("output.json");

        // Create workbook with identical flows except for Break Through DND
        createWorkbookWithDifferentBreakThroughDND(excelPath);

        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(excelPath.toFile());
        parser.writeNurseCallsJson(jsonPath.toFile(), ExcelParserV5.MergeMode.MERGE_BY_CONFIG_GROUP);

        assertTrue(Files.exists(jsonPath));
        String json = Files.readString(jsonPath);
        
        // Flows with different Break Through DND should NOT merge
        int flowCount = countOccurrences(json, "\"alarmsAlerts\":");
        assertEquals(2, flowCount, "Should have 2 separate flows (Break Through DND is part of merge criteria)");
    }

    @Test
    void mergeByConfigGroup_SeparatesFlowsByEnunciate() throws Exception {
        Path tempDir = Files.createTempDirectory("merge-enunciate-test");
        Path excelPath = tempDir.resolve("input.xlsx");
        Path jsonPath = tempDir.resolve("output.json");

        // Create workbook with identical flows except for Enunciate
        createWorkbookWithDifferentEnunciate(excelPath);

        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(excelPath.toFile());
        parser.writeNurseCallsJson(jsonPath.toFile(), ExcelParserV5.MergeMode.MERGE_BY_CONFIG_GROUP);

        assertTrue(Files.exists(jsonPath));
        String json = Files.readString(jsonPath);
        
        // Flows with different Enunciate values should NOT merge
        int flowCount = countOccurrences(json, "\"alarmsAlerts\":");
        assertEquals(2, flowCount, "Should have 2 separate flows (Enunciate is part of merge criteria)");
    }

    // Helper methods to create test workbooks

    private static void createWorkbookWithDifferentSendingNames(Path target) throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet units = workbook.createSheet("Unit Breakdown");
            Row unitsHeader = units.createRow(2);
            unitsHeader.createCell(0).setCellValue("Facility");
            unitsHeader.createCell(1).setCellValue("Common Unit Name");
            unitsHeader.createCell(2).setCellValue("Nurse Call Configuration Group");
            
            Row unitsRow = units.createRow(3);
            unitsRow.createCell(0).setCellValue("Test Facility");
            unitsRow.createCell(1).setCellValue("Unit 1");
            unitsRow.createCell(2).setCellValue("TestGroup");

            Sheet nurseCalls = workbook.createSheet("Nurse call");
            Row nurseHeader = nurseCalls.createRow(2);
            nurseHeader.createCell(0).setCellValue("Configuration Group");
            nurseHeader.createCell(1).setCellValue("Common Alert or Alarm Name");
            nurseHeader.createCell(2).setCellValue("Sending Name");
            nurseHeader.createCell(3).setCellValue("Priority");
            nurseHeader.createCell(4).setCellValue("Device - A");
            nurseHeader.createCell(5).setCellValue("Ringtone Device - A");
            nurseHeader.createCell(6).setCellValue("Response Options");
            nurseHeader.createCell(7).setCellValue("Time to 1st Recipient");
            nurseHeader.createCell(8).setCellValue("1st Recipient");
            
            // Alarm 1 with "SendingName1"
            Row row1 = nurseCalls.createRow(3);
            row1.createCell(0).setCellValue("TestGroup");
            row1.createCell(1).setCellValue("Alarm1");
            row1.createCell(2).setCellValue("SendingName1");
            row1.createCell(3).setCellValue("High");
            row1.createCell(4).setCellValue("Badge");
            row1.createCell(5).setCellValue("Tone1");
            row1.createCell(6).setCellValue("Accept");
            row1.createCell(7).setCellValue("0");
            row1.createCell(8).setCellValue("Nurse Team");
            
            // Alarm 2 with "SendingName2" - different sending name but otherwise identical
            Row row2 = nurseCalls.createRow(4);
            row2.createCell(0).setCellValue("TestGroup");
            row2.createCell(1).setCellValue("Alarm2");
            row2.createCell(2).setCellValue("SendingName2");
            row2.createCell(3).setCellValue("High");
            row2.createCell(4).setCellValue("Badge");
            row2.createCell(5).setCellValue("Tone1");
            row2.createCell(6).setCellValue("Accept");
            row2.createCell(7).setCellValue("0");
            row2.createCell(8).setCellValue("Nurse Team");

            Sheet clinicals = workbook.createSheet("Patient Monitoring");
            Row clinicalHeader = clinicals.createRow(2);
            clinicalHeader.createCell(0).setCellValue("Configuration Group");
            clinicalHeader.createCell(1).setCellValue("Alarm Name");

            try (OutputStream os = Files.newOutputStream(target)) {
                workbook.write(os);
            }
        }
    }

    private static void createWorkbookWithDifferentDeviceB(Path target) throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet units = workbook.createSheet("Unit Breakdown");
            Row unitsHeader = units.createRow(2);
            unitsHeader.createCell(0).setCellValue("Facility");
            unitsHeader.createCell(1).setCellValue("Common Unit Name");
            unitsHeader.createCell(2).setCellValue("Nurse Call Configuration Group");
            
            Row unitsRow = units.createRow(3);
            unitsRow.createCell(0).setCellValue("Test Facility");
            unitsRow.createCell(1).setCellValue("Unit 1");
            unitsRow.createCell(2).setCellValue("TestGroup");

            Sheet nurseCalls = workbook.createSheet("Nurse call");
            Row nurseHeader = nurseCalls.createRow(2);
            nurseHeader.createCell(0).setCellValue("Configuration Group");
            nurseHeader.createCell(1).setCellValue("Common Alert or Alarm Name");
            nurseHeader.createCell(2).setCellValue("Priority");
            nurseHeader.createCell(3).setCellValue("Device - A");
            nurseHeader.createCell(4).setCellValue("Device - B");
            nurseHeader.createCell(5).setCellValue("Ringtone Device - A");
            nurseHeader.createCell(6).setCellValue("Response Options");
            nurseHeader.createCell(7).setCellValue("Time to 1st Recipient");
            nurseHeader.createCell(8).setCellValue("1st Recipient");
            
            Row row1 = nurseCalls.createRow(3);
            row1.createCell(0).setCellValue("TestGroup");
            row1.createCell(1).setCellValue("Alarm1");
            row1.createCell(2).setCellValue("High");
            row1.createCell(3).setCellValue("Badge");
            row1.createCell(4).setCellValue("Smartphone");
            row1.createCell(5).setCellValue("Tone1");
            row1.createCell(6).setCellValue("Accept");
            row1.createCell(7).setCellValue("0");
            row1.createCell(8).setCellValue("Nurse Team");
            
            Row row2 = nurseCalls.createRow(4);
            row2.createCell(0).setCellValue("TestGroup");
            row2.createCell(1).setCellValue("Alarm2");
            row2.createCell(2).setCellValue("High");
            row2.createCell(3).setCellValue("Badge");
            row2.createCell(4).setCellValue("Desktop");
            row2.createCell(5).setCellValue("Tone1");
            row2.createCell(6).setCellValue("Accept");
            row2.createCell(7).setCellValue("0");
            row2.createCell(8).setCellValue("Nurse Team");

            Sheet clinicals = workbook.createSheet("Patient Monitoring");
            Row clinicalHeader = clinicals.createRow(2);
            clinicalHeader.createCell(0).setCellValue("Configuration Group");
            clinicalHeader.createCell(1).setCellValue("Alarm Name");

            try (OutputStream os = Files.newOutputStream(target)) {
                workbook.write(os);
            }
        }
    }

    private static void createWorkbookWithDifferentBreakThroughDND(Path target) throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet units = workbook.createSheet("Unit Breakdown");
            Row unitsHeader = units.createRow(2);
            unitsHeader.createCell(0).setCellValue("Facility");
            unitsHeader.createCell(1).setCellValue("Common Unit Name");
            unitsHeader.createCell(2).setCellValue("Nurse Call Configuration Group");
            
            Row unitsRow = units.createRow(3);
            unitsRow.createCell(0).setCellValue("Test Facility");
            unitsRow.createCell(1).setCellValue("Unit 1");
            unitsRow.createCell(2).setCellValue("TestGroup");

            Sheet nurseCalls = workbook.createSheet("Nurse call");
            Row nurseHeader = nurseCalls.createRow(2);
            nurseHeader.createCell(0).setCellValue("Configuration Group");
            nurseHeader.createCell(1).setCellValue("Common Alert or Alarm Name");
            nurseHeader.createCell(2).setCellValue("Priority");
            nurseHeader.createCell(3).setCellValue("Device - A");
            nurseHeader.createCell(4).setCellValue("Ringtone Device - A");
            nurseHeader.createCell(5).setCellValue("Response Options");
            nurseHeader.createCell(6).setCellValue("Break Through DND");
            nurseHeader.createCell(7).setCellValue("Time to 1st Recipient");
            nurseHeader.createCell(8).setCellValue("1st Recipient");
            
            Row row1 = nurseCalls.createRow(3);
            row1.createCell(0).setCellValue("TestGroup");
            row1.createCell(1).setCellValue("Alarm1");
            row1.createCell(2).setCellValue("High");
            row1.createCell(3).setCellValue("Badge");
            row1.createCell(4).setCellValue("Tone1");
            row1.createCell(5).setCellValue("Accept");
            row1.createCell(6).setCellValue("TRUE");
            row1.createCell(7).setCellValue("0");
            row1.createCell(8).setCellValue("Nurse Team");
            
            Row row2 = nurseCalls.createRow(4);
            row2.createCell(0).setCellValue("TestGroup");
            row2.createCell(1).setCellValue("Alarm2");
            row2.createCell(2).setCellValue("High");
            row2.createCell(3).setCellValue("Badge");
            row2.createCell(4).setCellValue("Tone1");
            row2.createCell(5).setCellValue("Accept");
            row2.createCell(6).setCellValue("FALSE");
            row2.createCell(7).setCellValue("0");
            row2.createCell(8).setCellValue("Nurse Team");

            Sheet clinicals = workbook.createSheet("Patient Monitoring");
            Row clinicalHeader = clinicals.createRow(2);
            clinicalHeader.createCell(0).setCellValue("Configuration Group");
            clinicalHeader.createCell(1).setCellValue("Alarm Name");

            try (OutputStream os = Files.newOutputStream(target)) {
                workbook.write(os);
            }
        }
    }

    private static void createWorkbookWithDifferentEnunciate(Path target) throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet units = workbook.createSheet("Unit Breakdown");
            Row unitsHeader = units.createRow(2);
            unitsHeader.createCell(0).setCellValue("Facility");
            unitsHeader.createCell(1).setCellValue("Common Unit Name");
            unitsHeader.createCell(2).setCellValue("Nurse Call Configuration Group");
            
            Row unitsRow = units.createRow(3);
            unitsRow.createCell(0).setCellValue("Test Facility");
            unitsRow.createCell(1).setCellValue("Unit 1");
            unitsRow.createCell(2).setCellValue("TestGroup");

            Sheet nurseCalls = workbook.createSheet("Nurse call");
            Row nurseHeader = nurseCalls.createRow(2);
            nurseHeader.createCell(0).setCellValue("Configuration Group");
            nurseHeader.createCell(1).setCellValue("Common Alert or Alarm Name");
            nurseHeader.createCell(2).setCellValue("Priority");
            nurseHeader.createCell(3).setCellValue("Device - A");
            nurseHeader.createCell(4).setCellValue("Ringtone Device - A");
            nurseHeader.createCell(5).setCellValue("Response Options");
            nurseHeader.createCell(6).setCellValue("Genie Enunciation");
            nurseHeader.createCell(7).setCellValue("Time to 1st Recipient");
            nurseHeader.createCell(8).setCellValue("1st Recipient");
            
            Row row1 = nurseCalls.createRow(3);
            row1.createCell(0).setCellValue("TestGroup");
            row1.createCell(1).setCellValue("Alarm1");
            row1.createCell(2).setCellValue("High");
            row1.createCell(3).setCellValue("Badge");
            row1.createCell(4).setCellValue("Tone1");
            row1.createCell(5).setCellValue("Accept");
            row1.createCell(6).setCellValue("TRUE");
            row1.createCell(7).setCellValue("0");
            row1.createCell(8).setCellValue("Nurse Team");
            
            Row row2 = nurseCalls.createRow(4);
            row2.createCell(0).setCellValue("TestGroup");
            row2.createCell(1).setCellValue("Alarm2");
            row2.createCell(2).setCellValue("High");
            row2.createCell(3).setCellValue("Badge");
            row2.createCell(4).setCellValue("Tone1");
            row2.createCell(5).setCellValue("Accept");
            row2.createCell(6).setCellValue("FALSE");
            row2.createCell(7).setCellValue("0");
            row2.createCell(8).setCellValue("Nurse Team");

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
