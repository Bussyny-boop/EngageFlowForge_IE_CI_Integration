package com.example.exceljson;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the escalateAfter field and its effect on JSON parameter attributes.
 * Specifically tests the addition of the declineCount parameter when escalateAfter contains "all".
 */
class EscalateAfterTest {

    /**
     * Helper method to find a parameter attribute by name in a list.
     */
    private Map<?, ?> findParameter(List<?> params, String name) {
        for (Object param : params) {
            var paramMap = (Map<?, ?>) param;
            if (name.equals(paramMap.get("name"))) {
                return paramMap;
            }
        }
        return null;
    }

    @Test
    void testDeclineCountAddedWhenEscalateAfterContainsAll() throws Exception {
        Path tempDir = Files.createTempDirectory("escalate-after-all-test");
        File excelFile = tempDir.resolve("test.xlsx").toFile();

        // Create test workbook with escalateAfter = "All declines"
        createTestWorkbook(excelFile, "All declines");

        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(excelFile);

        // Build JSON for nurse calls
        var nurseJson = parser.buildNurseCallsJson();
        var flows = (List<?>) nurseJson.get("deliveryFlows");
        assertNotNull(flows, "Delivery flows should not be null");
        assertEquals(1, flows.size(), "Should have 1 nurse call flow");

        var flow = (Map<?, ?>) flows.getFirst();
        var params = (List<?>) flow.get("parameterAttributes");
        assertNotNull(params, "Parameter attributes should not be null");

        // Verify declineCount parameter exists and has the correct value
        var declineCountParam = findParameter(params, "declineCount");
        assertNotNull(declineCountParam, "declineCount parameter should be present when escalateAfter contains 'all'");
        assertEquals("\"All Recipients\"", declineCountParam.get("value"), "declineCount value should be quoted 'All Recipients'");

        // Clean up
        Files.deleteIfExists(excelFile.toPath());
        Files.deleteIfExists(tempDir);
    }

    @Test
    void testDeclineCountNotAddedWhenEscalateAfterIsOneDecline() throws Exception {
        Path tempDir = Files.createTempDirectory("escalate-after-one-test");
        File excelFile = tempDir.resolve("test.xlsx").toFile();

        // Create test workbook with escalateAfter = "1 decline"
        createTestWorkbook(excelFile, "1 decline");

        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(excelFile);

        // Build JSON for nurse calls
        var nurseJson = parser.buildNurseCallsJson();
        var flows = (List<?>) nurseJson.get("deliveryFlows");
        var flow = (Map<?, ?>) flows.getFirst();
        var params = (List<?>) flow.get("parameterAttributes");

        // Verify declineCount parameter does NOT exist
        var declineCountParam = findParameter(params, "declineCount");
        assertNull(declineCountParam, "declineCount parameter should NOT be present when escalateAfter is '1 decline'");

        // Clean up
        Files.deleteIfExists(excelFile.toPath());
        Files.deleteIfExists(tempDir);
    }

    @Test
    void testDeclineCountNotAddedWhenEscalateAfterIsBlank() throws Exception {
        Path tempDir = Files.createTempDirectory("escalate-after-blank-test");
        File excelFile = tempDir.resolve("test.xlsx").toFile();

        // Create test workbook with blank escalateAfter
        createTestWorkbook(excelFile, "");

        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(excelFile);

        // Build JSON for nurse calls
        var nurseJson = parser.buildNurseCallsJson();
        var flows = (List<?>) nurseJson.get("deliveryFlows");
        var flow = (Map<?, ?>) flows.getFirst();
        var params = (List<?>) flow.get("parameterAttributes");

        // Verify declineCount parameter does NOT exist
        var declineCountParam = findParameter(params, "declineCount");
        assertNull(declineCountParam, "declineCount parameter should NOT be present when escalateAfter is blank");

        // Clean up
        Files.deleteIfExists(excelFile.toPath());
        Files.deleteIfExists(tempDir);
    }

    @Test
    void testDeclineCountAddedWhenEscalateAfterContainsAllWithDifferentCase() throws Exception {
        Path tempDir = Files.createTempDirectory("escalate-after-case-test");
        File excelFile = tempDir.resolve("test.xlsx").toFile();

        // Create test workbook with escalateAfter = "ALL Recipients"
        createTestWorkbook(excelFile, "ALL Recipients");

        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(excelFile);

        // Build JSON for nurse calls
        var nurseJson = parser.buildNurseCallsJson();
        var flows = (List<?>) nurseJson.get("deliveryFlows");
        var flow = (Map<?, ?>) flows.getFirst();
        var params = (List<?>) flow.get("parameterAttributes");

        // Verify declineCount parameter exists (case-insensitive check)
        var declineCountParam = findParameter(params, "declineCount");
        assertNotNull(declineCountParam, "declineCount parameter should be present when escalateAfter contains 'ALL' (case-insensitive)");
        assertEquals("\"All Recipients\"", declineCountParam.get("value"), "declineCount value should be quoted 'All Recipients'");

        // Clean up
        Files.deleteIfExists(excelFile.toPath());
        Files.deleteIfExists(tempDir);
    }

    @Test
    void testDeclineCountAddedForClinicals() throws Exception {
        Path tempDir = Files.createTempDirectory("escalate-after-clinical-test");
        File excelFile = tempDir.resolve("test.xlsx").toFile();

        // Create test workbook with clinical escalateAfter = "All declines"
        createTestWorkbookClinical(excelFile, "All declines");

        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(excelFile);

        // Build JSON for clinicals
        var clinicalJson = parser.buildClinicalsJson();
        var flows = (List<?>) clinicalJson.get("deliveryFlows");
        assertNotNull(flows, "Delivery flows should not be null");
        assertEquals(1, flows.size(), "Should have 1 clinical flow");

        var flow = (Map<?, ?>) flows.getFirst();
        var params = (List<?>) flow.get("parameterAttributes");
        assertNotNull(params, "Parameter attributes should not be null");

        // Verify declineCount parameter exists for clinical alerts too
        var declineCountParam = findParameter(params, "declineCount");
        assertNotNull(declineCountParam, "declineCount parameter should be present for clinicals when escalateAfter contains 'all'");
        assertEquals("\"All Recipients\"", declineCountParam.get("value"), "declineCount value should be quoted 'All Recipients'");

        // Clean up
        Files.deleteIfExists(excelFile.toPath());
        Files.deleteIfExists(tempDir);
    }

    private void createTestWorkbook(File target, String escalateAfterValue) throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            // Unit Breakdown sheet
            Sheet units = workbook.createSheet("Unit Breakdown");
            Row unitsHeader = units.createRow(2);
            unitsHeader.createCell(0).setCellValue("Facility");
            unitsHeader.createCell(1).setCellValue("Common Unit Name");
            unitsHeader.createCell(2).setCellValue("Nurse Call Configuration Group");
            unitsHeader.createCell(3).setCellValue("Patient Monitoring Configuration Group");
            
            Row unitsRow = units.createRow(3);
            unitsRow.createCell(0).setCellValue("Test Facility");
            unitsRow.createCell(1).setCellValue("Test Unit");
            unitsRow.createCell(2).setCellValue("TestGroup");
            unitsRow.createCell(3).setCellValue("TestClinicalGroup");

            // Nurse Call sheet
            Sheet nurseCalls = workbook.createSheet("Nurse Call");
            Row nurseHeader = nurseCalls.createRow(2);
            nurseHeader.createCell(0).setCellValue("Configuration Group");
            nurseHeader.createCell(1).setCellValue("Common Alert or Alarm Name");
            nurseHeader.createCell(2).setCellValue("Sending System Alert Name");
            nurseHeader.createCell(3).setCellValue("Priority");
            nurseHeader.createCell(4).setCellValue("Device - A");
            nurseHeader.createCell(5).setCellValue("Ringtone Device - A");
            nurseHeader.createCell(6).setCellValue("Response Options");
            nurseHeader.createCell(7).setCellValue("Break Through DND");
            nurseHeader.createCell(8).setCellValue("Engage 6.6+: Escalate after all declines or 1 decline");
            nurseHeader.createCell(9).setCellValue("Engage/Edge Display Time (Time to Live) (Device - A)");
            nurseHeader.createCell(10).setCellValue("Time to 1st Recipient");
            nurseHeader.createCell(11).setCellValue("1st Recipient");
            
            Row nurseRow = nurseCalls.createRow(3);
            nurseRow.createCell(0).setCellValue("TestGroup");
            nurseRow.createCell(1).setCellValue("Test Alarm");
            nurseRow.createCell(2).setCellValue("System Alarm");
            nurseRow.createCell(3).setCellValue("High");
            nurseRow.createCell(4).setCellValue("Badge");
            nurseRow.createCell(5).setCellValue("Tone 1");
            nurseRow.createCell(6).setCellValue("Accept, Escalate");
            nurseRow.createCell(7).setCellValue("Yes");
            if (!escalateAfterValue.isEmpty()) {
                nurseRow.createCell(8).setCellValue(escalateAfterValue);
            }
            nurseRow.createCell(9).setCellValue("15");
            nurseRow.createCell(10).setCellValue("0");
            nurseRow.createCell(11).setCellValue("Nurse Team");

            // Patient Monitoring sheet (minimal, not used in this test)
            Sheet clinicals = workbook.createSheet("Patient Monitoring");
            Row clinicalHeader = clinicals.createRow(2);
            clinicalHeader.createCell(0).setCellValue("Configuration Group");
            clinicalHeader.createCell(1).setCellValue("Common Alert or Alarm Name");

            try (FileOutputStream fos = new FileOutputStream(target)) {
                workbook.write(fos);
            }
        }
    }

    private void createTestWorkbookClinical(File target, String escalateAfterValue) throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            // Unit Breakdown sheet
            Sheet units = workbook.createSheet("Unit Breakdown");
            Row unitsHeader = units.createRow(2);
            unitsHeader.createCell(0).setCellValue("Facility");
            unitsHeader.createCell(1).setCellValue("Common Unit Name");
            unitsHeader.createCell(2).setCellValue("Nurse Call Configuration Group");
            unitsHeader.createCell(3).setCellValue("Patient Monitoring Configuration Group");
            
            Row unitsRow = units.createRow(3);
            unitsRow.createCell(0).setCellValue("Test Facility");
            unitsRow.createCell(1).setCellValue("Test Unit");
            unitsRow.createCell(2).setCellValue("TestGroup");
            unitsRow.createCell(3).setCellValue("TestClinicalGroup");

            // Nurse Call sheet (minimal)
            Sheet nurseCalls = workbook.createSheet("Nurse Call");
            Row nurseHeader = nurseCalls.createRow(2);
            nurseHeader.createCell(0).setCellValue("Configuration Group");
            nurseHeader.createCell(1).setCellValue("Common Alert or Alarm Name");

            // Patient Monitoring sheet
            Sheet clinicals = workbook.createSheet("Patient Monitoring");
            Row clinicalHeader = clinicals.createRow(2);
            clinicalHeader.createCell(0).setCellValue("Configuration Group");
            clinicalHeader.createCell(1).setCellValue("Common Alert or Alarm Name");
            clinicalHeader.createCell(2).setCellValue("Sending System Alert Name");
            clinicalHeader.createCell(3).setCellValue("Priority");
            clinicalHeader.createCell(4).setCellValue("Device - A");
            clinicalHeader.createCell(5).setCellValue("Ringtone Device - A");
            clinicalHeader.createCell(6).setCellValue("Response Options");
            clinicalHeader.createCell(7).setCellValue("Break Through DND");
            clinicalHeader.createCell(8).setCellValue("Engage 6.6+: Escalate after all declines or 1 decline");
            clinicalHeader.createCell(9).setCellValue("Engage/Edge Display Time (Time to Live) (Device - A)");
            clinicalHeader.createCell(10).setCellValue("Time to 1st Recipient");
            clinicalHeader.createCell(11).setCellValue("1st Recipient");
            
            Row clinicalRow = clinicals.createRow(3);
            clinicalRow.createCell(0).setCellValue("TestClinicalGroup");
            clinicalRow.createCell(1).setCellValue("Clinical Alert");
            clinicalRow.createCell(2).setCellValue("System Clinical Alert");
            clinicalRow.createCell(3).setCellValue("Medium");
            clinicalRow.createCell(4).setCellValue("Badge");
            clinicalRow.createCell(5).setCellValue("Tone 2");
            clinicalRow.createCell(6).setCellValue("Escalate");
            clinicalRow.createCell(7).setCellValue("No");
            if (!escalateAfterValue.isEmpty()) {
                clinicalRow.createCell(8).setCellValue(escalateAfterValue);
            }
            clinicalRow.createCell(9).setCellValue("20");
            clinicalRow.createCell(10).setCellValue("0");
            clinicalRow.createCell(11).setCellValue("Primary Team");

            try (FileOutputStream fos = new FileOutputStream(target)) {
                workbook.write(fos);
            }
        }
    }
}
