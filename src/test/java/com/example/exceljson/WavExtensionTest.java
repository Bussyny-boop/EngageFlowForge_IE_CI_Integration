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
 * Tests for .wav extension handling.
 * 
 * Requirements:
 * - XMPP alertSound: Use ringtone as-is (no .wav extension added)
 * - Vocera badgeAlertSound: Add .wav extension if not present, placed in parameterAttributes
 */
class WavExtensionTest {

    @Test
    void xmppAlertSoundWithoutWavExtension() throws Exception {
        Path tempDir = Files.createTempDirectory("xmpp-wav-test");
        File excelFile = tempDir.resolve("test.xlsx").toFile();

        createTestWorkbookWithDevice(excelFile, "XMPP", "list_pagers");

        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(excelFile);

        var nurseJson = parser.buildNurseCallsJson();
        var flows = (List<?>) nurseJson.get("deliveryFlows");
        var flow = (Map<?, ?>) flows.getFirst();
        var params = (List<?>) flow.get("parameterAttributes");

        // Find alertSound parameter
        boolean foundAlertSound = false;
        for (Object paramObj : params) {
            var param = (Map<?, ?>) paramObj;
            if ("alertSound".equals(param.get("name"))) {
                assertEquals("\"list_pagers\"", param.get("value"), 
                    "alertSound should use ringtone as-is without .wav");
                foundAlertSound = true;
            }
        }
        assertTrue(foundAlertSound, "Should have alertSound parameter");

        Files.deleteIfExists(excelFile.toPath());
        Files.deleteIfExists(tempDir);
    }

    @Test
    void xmppAlertSoundWithWavExtension() throws Exception {
        Path tempDir = Files.createTempDirectory("xmpp-wav-dup-test");
        File excelFile = tempDir.resolve("test.xlsx").toFile();

        createTestWorkbookWithDevice(excelFile, "XMPP", "list_pagers.wav");

        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(excelFile);

        var nurseJson = parser.buildNurseCallsJson();
        var flows = (List<?>) nurseJson.get("deliveryFlows");
        var flow = (Map<?, ?>) flows.getFirst();
        var params = (List<?>) flow.get("parameterAttributes");

        // Find alertSound parameter
        boolean foundAlertSound = false;
        for (Object paramObj : params) {
            var param = (Map<?, ?>) paramObj;
            if ("alertSound".equals(param.get("name"))) {
                assertEquals("\"list_pagers.wav\"", param.get("value"), 
                    "alertSound should use ringtone as-is, preserving .wav");
                foundAlertSound = true;
            }
        }
        assertTrue(foundAlertSound, "Should have alertSound parameter");

        Files.deleteIfExists(excelFile.toPath());
        Files.deleteIfExists(tempDir);
    }

    @Test
    void voceraInterfaceDynamicParameterWithoutWav() throws Exception {
        Path tempDir = Files.createTempDirectory("vocera-badge-test");
        File excelFile = tempDir.resolve("test.xlsx").toFile();

        createTestWorkbookWithDevice(excelFile, "Vocera", "list_pagers");

        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(excelFile);

        var nurseJson = parser.buildNurseCallsJson();
        var flows = (List<?>) nurseJson.get("deliveryFlows");
        var flow = (Map<?, ?>) flows.getFirst();
        var interfaces = (List<?>) flow.get("interfaces");
        
        assertEquals(1, interfaces.size(), "Should have 1 interface");
        
        var iface = (Map<?, ?>) interfaces.getFirst();
        assertEquals("Vocera", iface.get("componentName"), "Should be Vocera interface");
        
        // Check that dynamicParameters is NOT present
        var dynamicParams = (List<?>) iface.get("dynamicParameters");
        assertNull(dynamicParams, "Should NOT have dynamicParameters");
        
        // Check for badgeAlertSound in parameterAttributes instead
        var params = (List<?>) flow.get("parameterAttributes");
        boolean foundBadgeAlertSound = false;
        for (Object paramObj : params) {
            var param = (Map<?, ?>) paramObj;
            if ("badgeAlertSound".equals(param.get("name"))) {
                assertEquals("\"list_pagers.wav\"", param.get("value"), 
                    "badgeAlertSound should have .wav appended");
                foundBadgeAlertSound = true;
            }
        }
        assertTrue(foundBadgeAlertSound, "Should have badgeAlertSound in parameterAttributes");

        Files.deleteIfExists(excelFile.toPath());
        Files.deleteIfExists(tempDir);
    }

    @Test
    void voceraInterfaceDynamicParameterWithWav() throws Exception {
        Path tempDir = Files.createTempDirectory("vocera-badge-dup-test");
        File excelFile = tempDir.resolve("test.xlsx").toFile();

        createTestWorkbookWithDevice(excelFile, "Vocera", "list_pagers.wav");

        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(excelFile);

        var nurseJson = parser.buildNurseCallsJson();
        var flows = (List<?>) nurseJson.get("deliveryFlows");
        var flow = (Map<?, ?>) flows.getFirst();
        var interfaces = (List<?>) flow.get("interfaces");
        
        var iface = (Map<?, ?>) interfaces.getFirst();
        var dynamicParams = (List<?>) iface.get("dynamicParameters");
        assertNull(dynamicParams, "Should NOT have dynamicParameters");
        
        // Check for badgeAlertSound in parameterAttributes instead
        var params = (List<?>) flow.get("parameterAttributes");
        boolean foundBadgeAlertSound = false;
        for (Object paramObj : params) {
            var param = (Map<?, ?>) paramObj;
            if ("badgeAlertSound".equals(param.get("name"))) {
                assertEquals("\"list_pagers.wav\"", param.get("value"), 
                    "badgeAlertSound should NOT have duplicate .wav");
                foundBadgeAlertSound = true;
            }
        }
        assertTrue(foundBadgeAlertSound, "Should have badgeAlertSound in parameterAttributes");

        Files.deleteIfExists(excelFile.toPath());
        Files.deleteIfExists(tempDir);
    }

    @Test
    void voceraInterfaceWithoutRingtone() throws Exception {
        Path tempDir = Files.createTempDirectory("vocera-no-ringtone-test");
        File excelFile = tempDir.resolve("test.xlsx").toFile();

        createTestWorkbookWithDevice(excelFile, "Vocera", "");

        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(excelFile);

        var nurseJson = parser.buildNurseCallsJson();
        var flows = (List<?>) nurseJson.get("deliveryFlows");
        var flow = (Map<?, ?>) flows.getFirst();
        var interfaces = (List<?>) flow.get("interfaces");
        
        var iface = (Map<?, ?>) interfaces.getFirst();
        assertEquals("Vocera", iface.get("componentName"), "Should be Vocera interface");
        
        // When no ringtone, dynamicParameters should not be present
        var dynamicParams = (List<?>) iface.get("dynamicParameters");
        assertNull(dynamicParams, "Should NOT have dynamicParameters when ringtone is empty");
        
        // Also verify badgeAlertSound is not in parameterAttributes
        var params = (List<?>) flow.get("parameterAttributes");
        boolean foundBadgeAlertSound = params.stream()
            .anyMatch(p -> "badgeAlertSound".equals(((Map<?, ?>) p).get("name")));
        assertFalse(foundBadgeAlertSound, "Should NOT have badgeAlertSound when ringtone is empty");

        Files.deleteIfExists(excelFile.toPath());
        Files.deleteIfExists(tempDir);
    }

    @Test
    void xmppHasAllVmpParameters() throws Exception {
        Path tempDir = Files.createTempDirectory("xmpp-all-params-test");
        File excelFile = tempDir.resolve("test.xlsx").toFile();

        createTestWorkbookWithDevice(excelFile, "XMPP", "ringtone");

        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(excelFile);

        var nurseJson = parser.buildNurseCallsJson();
        var flows = (List<?>) nurseJson.get("deliveryFlows");
        var flow = (Map<?, ?>) flows.getFirst();
        var params = (List<?>) flow.get("parameterAttributes");

        // XMPP should have VMP parameters plus XMPP-specific ones
        // VMP params include: breakThrough, enunciate, message, patientMRN, patientName, etc.
        // XMPP-specific: audible, realert, multipleAccepts, delayedResponses, additionalContent
        
        // Check for some key VMP parameters
        boolean hasBreakThrough = params.stream()
            .anyMatch(p -> "breakThrough".equals(((Map<?, ?>) p).get("name")));
        assertTrue(hasBreakThrough, "XMPP should have breakThrough from VMP");
        
        boolean hasMessage = params.stream()
            .anyMatch(p -> "message".equals(((Map<?, ?>) p).get("name")));
        assertTrue(hasMessage, "XMPP should have message from VMP");
        
        boolean hasPatientMRN = params.stream()
            .anyMatch(p -> "patientMRN".equals(((Map<?, ?>) p).get("name")));
        assertTrue(hasPatientMRN, "XMPP should have patientMRN from VMP");
        
        // Check for XMPP-specific parameters
        boolean hasAudible = params.stream()
            .anyMatch(p -> "audible".equals(((Map<?, ?>) p).get("name")));
        assertTrue(hasAudible, "XMPP should have audible");
        
        boolean hasRealert = params.stream()
            .anyMatch(p -> "realert".equals(((Map<?, ?>) p).get("name")));
        assertTrue(hasRealert, "XMPP should have realert");
        
        boolean hasMultipleAccepts = params.stream()
            .anyMatch(p -> "multipleAccepts".equals(((Map<?, ?>) p).get("name")));
        assertTrue(hasMultipleAccepts, "XMPP should have multipleAccepts");
        
        boolean hasDelayedResponses = params.stream()
            .anyMatch(p -> "delayedResponses".equals(((Map<?, ?>) p).get("name")));
        assertTrue(hasDelayedResponses, "XMPP should have delayedResponses");

        Files.deleteIfExists(excelFile.toPath());
        Files.deleteIfExists(tempDir);
    }

    // Helper method to create test workbook
    private void createTestWorkbookWithDevice(File excelFile, String deviceName, String ringtone) throws Exception {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            // Unit Breakdown sheet
            Sheet unitSheet = wb.createSheet("Unit Breakdown");
            Row unitHeader = unitSheet.createRow(2);
            unitHeader.createCell(0).setCellValue("Facility");
            unitHeader.createCell(1).setCellValue("Common Unit Name");
            unitHeader.createCell(2).setCellValue("Nurse Call Configuration Group");

            Row unitRow = unitSheet.createRow(3);
            unitRow.createCell(0).setCellValue("Test Facility");
            unitRow.createCell(1).setCellValue("Test Unit");
            unitRow.createCell(2).setCellValue("TestGroup");

            // Nurse Call sheet
            Sheet nurseSheet = wb.createSheet("Nurse Call");
            Row nurseHeader = nurseSheet.createRow(2);
            nurseHeader.createCell(0).setCellValue("Configuration Group");
            nurseHeader.createCell(1).setCellValue("Common Alert or Alarm Name");
            nurseHeader.createCell(3).setCellValue("Priority");
            nurseHeader.createCell(4).setCellValue("Device - A");
            nurseHeader.createCell(5).setCellValue("Ringtone Device - A");
            nurseHeader.createCell(10).setCellValue("Time to 1st Recipient");
            nurseHeader.createCell(11).setCellValue("1st Recipient");

            Row nurseRow = nurseSheet.createRow(3);
            nurseRow.createCell(0).setCellValue("TestGroup");
            nurseRow.createCell(1).setCellValue("Test Alarm");
            nurseRow.createCell(3).setCellValue("Normal");
            nurseRow.createCell(4).setCellValue(deviceName);
            nurseRow.createCell(5).setCellValue(ringtone);
            nurseRow.createCell(10).setCellValue("0");
            nurseRow.createCell(11).setCellValue("Nurse Team");

            // Patient Monitoring sheet (empty)
            Sheet clinicalSheet = wb.createSheet("Patient Monitoring");
            Row clinicalHeader = clinicalSheet.createRow(2);
            clinicalHeader.createCell(0).setCellValue("Configuration Group");
            clinicalHeader.createCell(1).setCellValue("Common Alert or Alarm Name");

            try (FileOutputStream fos = new FileOutputStream(excelFile)) {
                wb.write(fos);
            }
        }
    }
}
