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
 * Tests for VMP interface badgeAlertSound parameter.
 * 
 * Requirements:
 * - VMP interface should use badgeAlertSound instead of alertSound
 * - badgeAlertSound value should include .wav extension
 * - badgeAlertSound should NOT be added when ringtone is "Global Setting"
 */
class VmpBadgeAlertSoundTest {

    @Test
    void vmpInterfaceUsesBadgeAlertSoundWithoutWav() throws Exception {
        Path tempDir = Files.createTempDirectory("vmp-badge-test");
        File excelFile = tempDir.resolve("test.xlsx").toFile();

        createTestWorkbookWithDevice(excelFile, "VCS", "list_pagers");

        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(excelFile);

        var nurseJson = parser.buildNurseCallsJson();
        var flows = (List<?>) nurseJson.get("deliveryFlows");
        var flow = (Map<?, ?>) flows.get(0);
        var interfaces = (List<?>) flow.get("interfaces");
        
        assertEquals(1, interfaces.size(), "Should have 1 interface");
        
        var iface = (Map<?, ?>) interfaces.get(0);
        assertEquals("VMP", iface.get("componentName"), "Should be VMP interface");
        
        // Check parameterAttributes
        var params = (List<?>) flow.get("parameterAttributes");
        
        // VMP should NOT have alertSound
        boolean hasAlertSound = params.stream()
            .anyMatch(p -> "alertSound".equals(((Map<?, ?>) p).get("name")));
        assertFalse(hasAlertSound, "VMP interface should NOT have alertSound parameter");
        
        // VMP should have badgeAlertSound with .wav extension
        boolean foundBadgeAlertSound = false;
        for (Object paramObj : params) {
            var param = (Map<?, ?>) paramObj;
            if ("badgeAlertSound".equals(param.get("name"))) {
                assertEquals("\"list_pagers.wav\"", param.get("value"), 
                    "badgeAlertSound should have .wav appended");
                foundBadgeAlertSound = true;
            }
        }
        assertTrue(foundBadgeAlertSound, "VMP interface should have badgeAlertSound in parameterAttributes");

        Files.deleteIfExists(excelFile.toPath());
        Files.deleteIfExists(tempDir);
    }

    @Test
    void vmpInterfaceUsesBadgeAlertSoundWithWav() throws Exception {
        Path tempDir = Files.createTempDirectory("vmp-badge-wav-test");
        File excelFile = tempDir.resolve("test.xlsx").toFile();

        createTestWorkbookWithDevice(excelFile, "Vocera VCS", "list_pagers.wav");

        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(excelFile);

        var nurseJson = parser.buildNurseCallsJson();
        var flows = (List<?>) nurseJson.get("deliveryFlows");
        var flow = (Map<?, ?>) flows.get(0);
        var interfaces = (List<?>) flow.get("interfaces");
        
        var iface = (Map<?, ?>) interfaces.get(0);
        assertEquals("VMP", iface.get("componentName"), "Should be VMP interface");
        
        var params = (List<?>) flow.get("parameterAttributes");
        
        // VMP should NOT have alertSound
        boolean hasAlertSound = params.stream()
            .anyMatch(p -> "alertSound".equals(((Map<?, ?>) p).get("name")));
        assertFalse(hasAlertSound, "VMP interface should NOT have alertSound parameter");
        
        // VMP should have badgeAlertSound without duplicate .wav
        boolean foundBadgeAlertSound = false;
        for (Object paramObj : params) {
            var param = (Map<?, ?>) paramObj;
            if ("badgeAlertSound".equals(param.get("name"))) {
                assertEquals("\"list_pagers.wav\"", param.get("value"), 
                    "badgeAlertSound should NOT have duplicate .wav");
                foundBadgeAlertSound = true;
            }
        }
        assertTrue(foundBadgeAlertSound, "VMP interface should have badgeAlertSound in parameterAttributes");

        Files.deleteIfExists(excelFile.toPath());
        Files.deleteIfExists(tempDir);
    }

    @Test
    void vmpInterfaceSkipsBadgeAlertSoundForGlobalSetting() throws Exception {
        Path tempDir = Files.createTempDirectory("vmp-global-setting-test");
        File excelFile = tempDir.resolve("test.xlsx").toFile();

        createTestWorkbookWithDevice(excelFile, "VCS", "Global Setting");

        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(excelFile);

        var nurseJson = parser.buildNurseCallsJson();
        var flows = (List<?>) nurseJson.get("deliveryFlows");
        var flow = (Map<?, ?>) flows.get(0);
        var interfaces = (List<?>) flow.get("interfaces");
        
        var iface = (Map<?, ?>) interfaces.get(0);
        assertEquals("VMP", iface.get("componentName"), "Should be VMP interface");
        
        var params = (List<?>) flow.get("parameterAttributes");
        
        // When ringtone is "Global Setting", VMP should NOT have badgeAlertSound
        boolean foundBadgeAlertSound = params.stream()
            .anyMatch(p -> "badgeAlertSound".equals(((Map<?, ?>) p).get("name")));
        assertFalse(foundBadgeAlertSound, "VMP interface should NOT have badgeAlertSound when ringtone is Global Setting");
        
        // Also should NOT have alertSound
        boolean hasAlertSound = params.stream()
            .anyMatch(p -> "alertSound".equals(((Map<?, ?>) p).get("name")));
        assertFalse(hasAlertSound, "VMP interface should NOT have alertSound when ringtone is Global Setting");

        Files.deleteIfExists(excelFile.toPath());
        Files.deleteIfExists(tempDir);
    }

    @Test
    void vmpInterfaceSkipsBadgeAlertSoundForGlobalSettingCaseInsensitive() throws Exception {
        Path tempDir = Files.createTempDirectory("vmp-global-case-test");
        File excelFile = tempDir.resolve("test.xlsx").toFile();

        createTestWorkbookWithDevice(excelFile, "VCS", "GLOBAL SETTING");

        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(excelFile);

        var nurseJson = parser.buildNurseCallsJson();
        var flows = (List<?>) nurseJson.get("deliveryFlows");
        var flow = (Map<?, ?>) flows.get(0);
        
        var params = (List<?>) flow.get("parameterAttributes");
        
        // Case-insensitive check: "GLOBAL SETTING" should also skip badgeAlertSound
        boolean foundBadgeAlertSound = params.stream()
            .anyMatch(p -> "badgeAlertSound".equals(((Map<?, ?>) p).get("name")));
        assertFalse(foundBadgeAlertSound, "VMP interface should NOT have badgeAlertSound when ringtone is GLOBAL SETTING");

        Files.deleteIfExists(excelFile.toPath());
        Files.deleteIfExists(tempDir);
    }

    @Test
    void vmpInterfaceWithoutRingtone() throws Exception {
        Path tempDir = Files.createTempDirectory("vmp-no-ringtone-test");
        File excelFile = tempDir.resolve("test.xlsx").toFile();

        createTestWorkbookWithDevice(excelFile, "VCS", "");

        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(excelFile);

        var nurseJson = parser.buildNurseCallsJson();
        var flows = (List<?>) nurseJson.get("deliveryFlows");
        var flow = (Map<?, ?>) flows.get(0);
        var interfaces = (List<?>) flow.get("interfaces");
        
        var iface = (Map<?, ?>) interfaces.get(0);
        assertEquals("VMP", iface.get("componentName"), "Should be VMP interface");
        
        // When no ringtone, VMP should not have badgeAlertSound
        var params = (List<?>) flow.get("parameterAttributes");
        boolean foundBadgeAlertSound = params.stream()
            .anyMatch(p -> "badgeAlertSound".equals(((Map<?, ?>) p).get("name")));
        assertFalse(foundBadgeAlertSound, "VMP interface should NOT have badgeAlertSound when ringtone is empty");

        Files.deleteIfExists(excelFile.toPath());
        Files.deleteIfExists(tempDir);
    }

    @Test
    void voceraInterfaceStillUsesAlertSoundAndBadgeAlertSound() throws Exception {
        Path tempDir = Files.createTempDirectory("vocera-both-test");
        File excelFile = tempDir.resolve("test.xlsx").toFile();

        createTestWorkbookWithDevice(excelFile, "Vocera", "list_pagers");

        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(excelFile);

        var nurseJson = parser.buildNurseCallsJson();
        var flows = (List<?>) nurseJson.get("deliveryFlows");
        var flow = (Map<?, ?>) flows.get(0);
        var interfaces = (List<?>) flow.get("interfaces");
        
        var iface = (Map<?, ?>) interfaces.get(0);
        assertEquals("Vocera", iface.get("componentName"), "Should be Vocera interface");
        
        var params = (List<?>) flow.get("parameterAttributes");
        
        // Vocera should have BOTH alertSound and badgeAlertSound
        boolean hasAlertSound = false;
        boolean hasBadgeAlertSound = false;
        
        for (Object paramObj : params) {
            var param = (Map<?, ?>) paramObj;
            String name = (String) param.get("name");
            if ("alertSound".equals(name)) {
                assertEquals("\"list_pagers\"", param.get("value"));
                hasAlertSound = true;
            }
            if ("badgeAlertSound".equals(name)) {
                assertEquals("\"list_pagers.wav\"", param.get("value"));
                hasBadgeAlertSound = true;
            }
        }
        
        assertTrue(hasAlertSound, "Vocera interface should have alertSound parameter");
        assertTrue(hasBadgeAlertSound, "Vocera interface should have badgeAlertSound parameter");

        Files.deleteIfExists(excelFile.toPath());
        Files.deleteIfExists(tempDir);
    }

    @Test
    void edgeInterfaceUsesAlertSoundOnly() throws Exception {
        Path tempDir = Files.createTempDirectory("edge-alert-test");
        File excelFile = tempDir.resolve("test.xlsx").toFile();

        createTestWorkbookWithDevice(excelFile, "iPhone-Edge", "list_pagers");

        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(excelFile);

        var nurseJson = parser.buildNurseCallsJson();
        var flows = (List<?>) nurseJson.get("deliveryFlows");
        var flow = (Map<?, ?>) flows.get(0);
        var interfaces = (List<?>) flow.get("interfaces");
        
        var iface = (Map<?, ?>) interfaces.get(0);
        assertEquals("OutgoingWCTP", iface.get("componentName"), "Should be Edge interface");
        
        var params = (List<?>) flow.get("parameterAttributes");
        
        // Edge should have alertSound but NOT badgeAlertSound
        boolean hasAlertSound = false;
        boolean hasBadgeAlertSound = false;
        
        for (Object paramObj : params) {
            var param = (Map<?, ?>) paramObj;
            String name = (String) param.get("name");
            if ("alertSound".equals(name)) {
                assertEquals("\"list_pagers\"", param.get("value"));
                hasAlertSound = true;
            }
            if ("badgeAlertSound".equals(name)) {
                hasBadgeAlertSound = true;
            }
        }
        
        assertTrue(hasAlertSound, "Edge interface should have alertSound parameter");
        assertFalse(hasBadgeAlertSound, "Edge interface should NOT have badgeAlertSound parameter");

        Files.deleteIfExists(excelFile.toPath());
        Files.deleteIfExists(tempDir);
    }

    private void createTestWorkbookWithDevice(File target, String deviceName, String ringtone) throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet units = workbook.createSheet("Unit Breakdown");
            Row unitsHeader = units.createRow(2);
            unitsHeader.createCell(0).setCellValue("Facility");
            unitsHeader.createCell(1).setCellValue("Common Unit Name");
            unitsHeader.createCell(2).setCellValue("Nurse Call Configuration Group");
            Row unitsRow = units.createRow(3);
            unitsRow.createCell(0).setCellValue("Test Facility");
            unitsRow.createCell(1).setCellValue("Test Unit");
            unitsRow.createCell(2).setCellValue("TestGroup");

            Sheet nurseCalls = workbook.createSheet("Nurse call");
            Row nurseHeader = nurseCalls.createRow(2);
            nurseHeader.createCell(0).setCellValue("Configuration Group");
            nurseHeader.createCell(1).setCellValue("Common Alert or Alarm Name");
            nurseHeader.createCell(3).setCellValue("Priority");
            nurseHeader.createCell(4).setCellValue("Device - A");
            nurseHeader.createCell(5).setCellValue("Ringtone");
            nurseHeader.createCell(10).setCellValue("Time to 1st Recipient");
            nurseHeader.createCell(11).setCellValue("1st Recipient");
            
            Row nurseRow = nurseCalls.createRow(3);
            nurseRow.createCell(0).setCellValue("TestGroup");
            nurseRow.createCell(1).setCellValue("Test Alarm");
            nurseRow.createCell(3).setCellValue("Normal");
            nurseRow.createCell(4).setCellValue(deviceName);
            nurseRow.createCell(5).setCellValue(ringtone);
            nurseRow.createCell(10).setCellValue("0");
            nurseRow.createCell(11).setCellValue("Nurse Team");

            try (FileOutputStream fos = new FileOutputStream(target)) {
                workbook.write(fos);
            }
        }
    }
}
