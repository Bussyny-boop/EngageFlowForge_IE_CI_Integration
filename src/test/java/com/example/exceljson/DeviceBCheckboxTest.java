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
 * Test that default interface checkboxes apply correctly to Device-B
 * with the exception that when Device-B is empty, it should not affect
 * the checkbox logic.
 */
class DeviceBCheckboxTest {

    private void createTestWorkbook(File excelFile, String deviceA, String deviceB) throws Exception {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            // Unit Breakdown sheet
            Sheet unitSheet = wb.createSheet("Unit Breakdown");
            Row unitHeader = unitSheet.createRow(2);
            unitHeader.createCell(0).setCellValue("Facility");
            unitHeader.createCell(1).setCellValue("Common Unit Name");
            unitHeader.createCell(2).setCellValue("Nurse Call Configuration Group");

            Row unitRow = unitSheet.createRow(3);
            unitRow.createCell(0).setCellValue("Test Facility");
            unitRow.createCell(1).setCellValue("ICU");
            unitRow.createCell(2).setCellValue("Nurse Group 1");

            // Nurse Call sheet
            Sheet nurseSheet = wb.createSheet("Nurse Call");
            Row nurseHeader = nurseSheet.createRow(2);
            nurseHeader.createCell(0).setCellValue("Configuration Group");
            nurseHeader.createCell(1).setCellValue("Common Alert or Alarm Name");
            nurseHeader.createCell(2).setCellValue("Sending System Alert Name");
            nurseHeader.createCell(3).setCellValue("Priority");
            nurseHeader.createCell(4).setCellValue("Device - A");
            nurseHeader.createCell(5).setCellValue("Device - B");
            nurseHeader.createCell(6).setCellValue("Ringtone Device - A");
            nurseHeader.createCell(10).setCellValue("Time to 1st Recipient");
            nurseHeader.createCell(11).setCellValue("1st Recipient");

            Row nurseRow1 = nurseSheet.createRow(3);
            nurseRow1.createCell(0).setCellValue("Nurse Group 1");
            nurseRow1.createCell(1).setCellValue("Test Alarm");
            nurseRow1.createCell(2).setCellValue("Test");
            nurseRow1.createCell(3).setCellValue("Normal");
            nurseRow1.createCell(4).setCellValue(deviceA);
            nurseRow1.createCell(5).setCellValue(deviceB);
            nurseRow1.createCell(6).setCellValue("Ringtone1");
            nurseRow1.createCell(10).setCellValue("0");
            nurseRow1.createCell(11).setCellValue("Nurse Team");

            // Patient Monitoring sheet
            Sheet clinicalSheet = wb.createSheet("Patient Monitoring");
            Row clinicalHeader = clinicalSheet.createRow(2);
            clinicalHeader.createCell(0).setCellValue("Configuration Group");
            clinicalHeader.createCell(1).setCellValue("Common Alert or Alarm Name");

            try (FileOutputStream fos = new FileOutputStream(excelFile)) {
                wb.write(fos);
            }
        }
    }

    @Test
    void testDeviceABlankDeviceBBlank_WithCheckbox_ShouldApplyDefaults() throws Exception {
        // Both blank - should apply defaults
        Path tempDir = Files.createTempDirectory("deviceb-checkbox-test1");
        File excelFile = tempDir.resolve("test.xlsx").toFile();

        createTestWorkbook(excelFile, "", "");

        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(excelFile);
        parser.setDefaultInterfaces(false, true); // VMP checkbox checked

        var nurseJson = parser.buildNurseCallsJson();
        var flows = (List<?>) nurseJson.get("deliveryFlows");
        var flow = (Map<?, ?>) flows.get(0);
        var interfaces = (List<?>) flow.get("interfaces");

        assertEquals(1, interfaces.size(), "Both blank should apply VMP default");
        var iface = (Map<?, ?>) interfaces.get(0);
        assertEquals("VMP", iface.get("componentName"));

        Files.deleteIfExists(excelFile.toPath());
        Files.deleteIfExists(tempDir);
    }

    @Test
    void testDeviceAHasNoKeywordsDeviceBBlank_WithCheckbox_ShouldNOT_ApplyDefaults() throws Exception {
        // Device-A has content but no keywords, Device-B is blank
        // NEW REQUIREMENT: Should NOT apply defaults because Device-B is empty
        // (When Device-B is empty, only apply defaults if Device-A is also empty)
        Path tempDir = Files.createTempDirectory("deviceb-checkbox-test2");
        File excelFile = tempDir.resolve("test.xlsx").toFile();

        createTestWorkbook(excelFile, "Random Device", "");

        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(excelFile);
        parser.setDefaultInterfaces(false, true); // VMP checkbox checked

        var nurseJson = parser.buildNurseCallsJson();
        var flows = (List<?>) nurseJson.get("deliveryFlows");
        var flow = (Map<?, ?>) flows.get(0);
        var interfaces = (List<?>) flow.get("interfaces");

        assertEquals(0, interfaces.size(), "Device-A with no keywords and Device-B blank should NOT apply defaults");

        Files.deleteIfExists(excelFile.toPath());
        Files.deleteIfExists(tempDir);
    }

    @Test
    void testDeviceAHasEdgeDeviceBBlank_WithCheckbox_ShouldNotApplyDefaults() throws Exception {
        // Device-A has Edge keyword, Device-B is blank
        // Should use Edge interface from Device-A, not defaults
        Path tempDir = Files.createTempDirectory("deviceb-checkbox-test3");
        File excelFile = tempDir.resolve("test.xlsx").toFile();

        createTestWorkbook(excelFile, "iPhone-Edge", "");

        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(excelFile);
        parser.setDefaultInterfaces(false, true); // VMP checkbox checked

        var nurseJson = parser.buildNurseCallsJson();
        var flows = (List<?>) nurseJson.get("deliveryFlows");
        var flow = (Map<?, ?>) flows.get(0);
        var interfaces = (List<?>) flow.get("interfaces");

        assertEquals(1, interfaces.size(), "Device-A with Edge should use Edge, not defaults");
        var iface = (Map<?, ?>) interfaces.get(0);
        assertEquals("OutgoingWCTP", iface.get("componentName"));

        Files.deleteIfExists(excelFile.toPath());
        Files.deleteIfExists(tempDir);
    }

    @Test
    void testDeviceABlankDeviceBHasNoKeywords_WithCheckbox_SHOULD_ApplyDefaults() throws Exception {
        // Device-A is blank, Device-B has content but no keywords
        // NEW REQUIREMENT: Device-B has content (not blank), so normal checkbox logic applies
        // Both devices have no keywords, so defaults should apply
        Path tempDir = Files.createTempDirectory("deviceb-checkbox-test4");
        File excelFile = tempDir.resolve("test.xlsx").toFile();

        createTestWorkbook(excelFile, "", "Random Device");

        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(excelFile);
        parser.setDefaultInterfaces(false, true); // VMP checkbox checked

        var nurseJson = parser.buildNurseCallsJson();
        var flows = (List<?>) nurseJson.get("deliveryFlows");
        var flow = (Map<?, ?>) flows.get(0);
        var interfaces = (List<?>) flow.get("interfaces");

        // Device-B has content (not blank) but no keywords, so defaults should apply
        assertEquals(1, interfaces.size(), "Device-B with no keywords should apply VMP default");
        var iface = (Map<?, ?>) interfaces.get(0);
        assertEquals("VMP", iface.get("componentName"));

        Files.deleteIfExists(excelFile.toPath());
        Files.deleteIfExists(tempDir);
    }

    @Test
    void testDeviceABlankDeviceBHasVCS_WithCheckbox_ShouldNotApplyDefaults() throws Exception {
        // Device-A is blank, Device-B has VCS
        // Should use VCS interface from Device-B, not defaults
        Path tempDir = Files.createTempDirectory("deviceb-checkbox-test5");
        File excelFile = tempDir.resolve("test.xlsx").toFile();

        createTestWorkbook(excelFile, "", "Vocera VCS");

        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(excelFile);
        parser.setDefaultInterfaces(true, false); // Edge checkbox checked (different than VCS in Device-B)

        var nurseJson = parser.buildNurseCallsJson();
        var flows = (List<?>) nurseJson.get("deliveryFlows");
        var flow = (Map<?, ?>) flows.get(0);
        var interfaces = (List<?>) flow.get("interfaces");

        assertEquals(1, interfaces.size(), "Device-B with VCS should use VCS/VMP, not Edge default");
        var iface = (Map<?, ?>) interfaces.get(0);
        assertEquals("VMP", iface.get("componentName"));

        Files.deleteIfExists(excelFile.toPath());
        Files.deleteIfExists(tempDir);
    }

    @Test
    void testDeviceAHasNoKeywordsDeviceBHasNoKeywords_WithCheckbox_ShouldApplyDefaults() throws Exception {
        // Both have content but neither has keywords
        // Should apply defaults
        Path tempDir = Files.createTempDirectory("deviceb-checkbox-test6");
        File excelFile = tempDir.resolve("test.xlsx").toFile();

        createTestWorkbook(excelFile, "Random Device A", "Random Device B");

        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(excelFile);
        parser.setDefaultInterfaces(false, true); // VMP checkbox checked

        var nurseJson = parser.buildNurseCallsJson();
        var flows = (List<?>) nurseJson.get("deliveryFlows");
        var flow = (Map<?, ?>) flows.get(0);
        var interfaces = (List<?>) flow.get("interfaces");

        assertEquals(1, interfaces.size(), "Both devices with no keywords should apply VMP default");
        var iface = (Map<?, ?>) interfaces.get(0);
        assertEquals("VMP", iface.get("componentName"));

        Files.deleteIfExists(excelFile.toPath());
        Files.deleteIfExists(tempDir);
    }
}
