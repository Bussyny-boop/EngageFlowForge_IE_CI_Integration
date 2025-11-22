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
 * Tests for the XMPP adapter interface detection and parameter attributes.
 * 
 * Requirements:
 * - Device containing "XMPP" (case-insensitive) should use XMPP interface
 * - XMPP interface should use specific parameter attributes (alertSound, additionalContent, etc.)
 * - alertSound should be ringtone + ".wav"
 * - multipleAccepts should come from "Platform: Multi-User Accept" column
 */
class XmppInterfaceTest {

    @Test
    void xmppDeviceUsesXmppInterface() throws Exception {
        Path tempDir = Files.createTempDirectory("xmpp-interface-test");
        File excelFile = tempDir.resolve("test.xlsx").toFile();

        createTestWorkbookWithDevice(excelFile, "XMPP", "low_priority", "true");

        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(excelFile);

        var nurseJson = parser.buildNurseCallsJson();
        var flows = (List<?>) nurseJson.get("deliveryFlows");
        
        assertEquals(1, flows.size(), "Should have 1 nurse call flow");

        var flow = (Map<?, ?>) flows.getFirst();
        var interfaces = (List<?>) flow.get("interfaces");
        assertEquals(1, interfaces.size(), "XMPP device should have 1 interface");
        
        var iface = (Map<?, ?>) interfaces.getFirst();
        assertEquals("XMPP", iface.get("componentName"), "Device 'XMPP' should use XMPP component");
        assertEquals("XMPP", iface.get("referenceName"), "XMPP should use default XMPP reference");

        Files.deleteIfExists(excelFile.toPath());
        Files.deleteIfExists(tempDir);
    }

    @Test
    void xmppCaseInsensitiveDetection() throws Exception {
        Path tempDir = Files.createTempDirectory("xmpp-case-test");
        File excelFile = tempDir.resolve("test.xlsx").toFile();

        createTestWorkbookWithDevice(excelFile, "xmpp", "ringtone1", "false");

        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(excelFile);

        var nurseJson = parser.buildNurseCallsJson();
        var flows = (List<?>) nurseJson.get("deliveryFlows");
        var flow = (Map<?, ?>) flows.getFirst();
        var interfaces = (List<?>) flow.get("interfaces");
        var iface = (Map<?, ?>) interfaces.getFirst();
        
        assertEquals("XMPP", iface.get("componentName"), "Lowercase 'xmpp' should be detected");

        Files.deleteIfExists(excelFile.toPath());
        Files.deleteIfExists(tempDir);
    }

    @Test
    void xmppParameterAttributesForNurseCalls() throws Exception {
        Path tempDir = Files.createTempDirectory("xmpp-params-nurse-test");
        File excelFile = tempDir.resolve("test.xlsx").toFile();

        String ringtone = "low_priority";
        String multiUserAccept = "true";
        createTestWorkbookWithDevice(excelFile, "XMPP", ringtone, multiUserAccept);

        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(excelFile);

        var nurseJson = parser.buildNurseCallsJson();
        var flows = (List<?>) nurseJson.get("deliveryFlows");
        var flow = (Map<?, ?>) flows.getFirst();
        var params = (List<?>) flow.get("parameterAttributes");

        // Verify alertSound = ringtone (no .wav extension)
        boolean foundAlertSound = false;
        for (Object paramObj : params) {
            var param = (Map<?, ?>) paramObj;
            if ("alertSound".equals(param.get("name"))) {
                assertEquals("\"low_priority\"", param.get("value"), "alertSound should be ringtone without .wav");
                foundAlertSound = true;
            }
        }
        assertTrue(foundAlertSound, "Should have alertSound parameter");

        // Verify additionalContent contains NurseCalls-specific template
        boolean foundAdditionalContent = false;
        for (Object paramObj : params) {
            var param = (Map<?, ?>) paramObj;
            if ("additionalContent".equals(param.get("name"))) {
                String value = (String) param.get("value");
                assertTrue(value.contains("Patient: #{bed.patient.last_name}"), "Should have nurse calls template");
                assertTrue(value.contains("Admitting Reason"), "Should have admitting reason");
                foundAdditionalContent = true;
            }
        }
        assertTrue(foundAdditionalContent, "Should have additionalContent parameter");

        // Verify audible = true
        boolean foundAudible = false;
        for (Object paramObj : params) {
            var param = (Map<?, ?>) paramObj;
            if ("audible".equals(param.get("name"))) {
                assertEquals("true", param.get("value"), "audible should be true");
                foundAudible = true;
            }
        }
        assertTrue(foundAudible, "Should have audible parameter");

        // Verify realert = false
        boolean foundRealert = false;
        for (Object paramObj : params) {
            var param = (Map<?, ?>) paramObj;
            if ("realert".equals(param.get("name"))) {
                assertEquals("false", param.get("value"), "realert should be false");
                foundRealert = true;
            }
        }
        assertTrue(foundRealert, "Should have realert parameter");

        // Verify multipleAccepts = true (from column)
        boolean foundMultipleAccepts = false;
        for (Object paramObj : params) {
            var param = (Map<?, ?>) paramObj;
            if ("multipleAccepts".equals(param.get("name"))) {
                assertEquals("true", param.get("value"), "multipleAccepts should be true from column");
                foundMultipleAccepts = true;
            }
        }
        assertTrue(foundMultipleAccepts, "Should have multipleAccepts parameter");

        // Verify delayedResponses = false
        boolean foundDelayedResponses = false;
        for (Object paramObj : params) {
            var param = (Map<?, ?>) paramObj;
            if ("delayedResponses".equals(param.get("name"))) {
                assertEquals("false", param.get("value"), "delayedResponses should be false");
                foundDelayedResponses = true;
            }
        }
        assertTrue(foundDelayedResponses, "Should have delayedResponses parameter");

        Files.deleteIfExists(excelFile.toPath());
        Files.deleteIfExists(tempDir);
    }

    @Test
    void xmppParameterAttributesForOrders() throws Exception {
        Path tempDir = Files.createTempDirectory("xmpp-params-orders-test");
        File excelFile = tempDir.resolve("test.xlsx").toFile();

        String ringtone = "high_priority";
        String multiUserAccept = "false";
        createTestOrdersWorkbookWithDevice(excelFile, "XMPP", ringtone, multiUserAccept);

        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(excelFile);

        var ordersJson = parser.buildOrdersJson();
        var flows = (List<?>) ordersJson.get("deliveryFlows");
        var flow = (Map<?, ?>) flows.getFirst();
        var params = (List<?>) flow.get("parameterAttributes");

        // Verify alertSound = ringtone (no .wav extension)
        boolean foundAlertSound = false;
        for (Object paramObj : params) {
            var param = (Map<?, ?>) paramObj;
            if ("alertSound".equals(param.get("name"))) {
                assertEquals("\"high_priority\"", param.get("value"), "alertSound should be ringtone without .wav");
                foundAlertSound = true;
            }
        }
        assertTrue(foundAlertSound, "Should have alertSound parameter");

        // Verify additionalContent contains Orders-specific template
        boolean foundAdditionalContent = false;
        for (Object paramObj : params) {
            var param = (Map<?, ?>) paramObj;
            if ("additionalContent".equals(param.get("name"))) {
                String value = (String) param.get("value");
                assertTrue(value.contains("Procedure:"), "Should have orders template with Procedure");
                assertTrue(value.contains("Order Notes"), "Should have Order Notes");
                foundAdditionalContent = true;
            }
        }
        assertTrue(foundAdditionalContent, "Should have additionalContent parameter");

        // Verify multipleAccepts = false (from column)
        boolean foundMultipleAccepts = false;
        for (Object paramObj : params) {
            var param = (Map<?, ?>) paramObj;
            if ("multipleAccepts".equals(param.get("name"))) {
                assertEquals("false", param.get("value"), "multipleAccepts should be false from column");
                foundMultipleAccepts = true;
            }
        }
        assertTrue(foundMultipleAccepts, "Should have multipleAccepts parameter");

        Files.deleteIfExists(excelFile.toPath());
        Files.deleteIfExists(tempDir);
    }

    @Test
    void xmppWithCustomReferenceName() throws Exception {
        Path tempDir = Files.createTempDirectory("xmpp-custom-ref-test");
        File excelFile = tempDir.resolve("test.xlsx").toFile();

        createTestWorkbookWithDevice(excelFile, "XMPP", "ringtone", "true");

        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(excelFile);
        parser.setInterfaceReferences("OutgoingWCTP", "VMP", "Vocera", "CustomXMPP");

        var nurseJson = parser.buildNurseCallsJson();
        var flows = (List<?>) nurseJson.get("deliveryFlows");
        var flow = (Map<?, ?>) flows.getFirst();
        var interfaces = (List<?>) flow.get("interfaces");
        var iface = (Map<?, ?>) interfaces.getFirst();
        
        assertEquals("XMPP", iface.get("componentName"), "Component name should stay 'XMPP'");
        assertEquals("CustomXMPP", iface.get("referenceName"), "Should use custom reference name");

        Files.deleteIfExists(excelFile.toPath());
        Files.deleteIfExists(tempDir);
    }

    @Test
    void xmppMultiUserAcceptFalse() throws Exception {
        Path tempDir = Files.createTempDirectory("xmpp-multi-false-test");
        File excelFile = tempDir.resolve("test.xlsx").toFile();

        createTestWorkbookWithDevice(excelFile, "XMPP", "tone", "false");

        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(excelFile);

        var nurseJson = parser.buildNurseCallsJson();
        var flows = (List<?>) nurseJson.get("deliveryFlows");
        var flow = (Map<?, ?>) flows.getFirst();
        var params = (List<?>) flow.get("parameterAttributes");

        boolean foundMultipleAccepts = false;
        for (Object paramObj : params) {
            var param = (Map<?, ?>) paramObj;
            if ("multipleAccepts".equals(param.get("name"))) {
                assertEquals("false", param.get("value"), "multipleAccepts should be false");
                foundMultipleAccepts = true;
            }
        }
        assertTrue(foundMultipleAccepts, "Should have multipleAccepts parameter set to false");

        Files.deleteIfExists(excelFile.toPath());
        Files.deleteIfExists(tempDir);
    }

    @Test
    void xmppParameterAttributesForClinicals() throws Exception {
        Path tempDir = Files.createTempDirectory("xmpp-params-clinical-test");
        File excelFile = tempDir.resolve("test.xlsx").toFile();

        String ringtone = "clinical_alert";
        String multiUserAccept = "true";
        createTestClinicalWorkbookWithDevice(excelFile, "XMPP", ringtone, multiUserAccept);

        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(excelFile);

        var clinicalJson = parser.buildClinicalsJson();
        var flows = (List<?>) clinicalJson.get("deliveryFlows");
        var flow = (Map<?, ?>) flows.getFirst();
        var params = (List<?>) flow.get("parameterAttributes");

        // Verify additionalContent contains Clinicals-specific template (for regular destination, not NoCaregivers)
        boolean foundAdditionalContent = false;
        for (Object paramObj : params) {
            var param = (Map<?, ?>) paramObj;
            if ("additionalContent".equals(param.get("name")) && !param.containsKey("destinationOrder")) {
                String value = (String) param.get("value");
                assertTrue(value.contains("Clinical Alert ${destinationName}"), "Should have Clinical Alert header");
                assertTrue(value.contains("#{alert_type}"), "Should have alert_type");
                assertTrue(value.contains("#{clinical_patient.first_name} #{clinical_patient.last_name}"), "Should have clinical patient name");
                assertTrue(value.contains("#{clinical_details.as_list(, detail_type, value, uom)}"), "Should have clinical details");
                foundAdditionalContent = true;
                break;
            }
        }
        assertTrue(foundAdditionalContent, "Should have additionalContent parameter for regular destination");

        // Verify NoCaregivers destination also has additionalContent
        boolean foundNoCareAdditionalContent = false;
        for (Object paramObj : params) {
            var param = (Map<?, ?>) paramObj;
            if ("additionalContent".equals(param.get("name")) && param.containsKey("destinationOrder")) {
                String value = (String) param.get("value");
                assertTrue(value.contains("Issue: A Clinical Alert has been received without any caregivers assigned to room"), 
                    "NoCaregivers additionalContent should have issue message");
                assertTrue(value.contains("#{clinical_patient.first_name} #{clinical_patient.last_name}"), 
                    "NoCaregivers should have clinical patient name");
                foundNoCareAdditionalContent = true;
                break;
            }
        }
        assertTrue(foundNoCareAdditionalContent, "Should have additionalContent parameter for NoCaregivers destination");

        Files.deleteIfExists(excelFile.toPath());
        Files.deleteIfExists(tempDir);
    }

    // Helper methods to create test workbooks

    private void createTestWorkbookWithDevice(File excelFile, String deviceName, String ringtone, String multiUserAccept) throws Exception {
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
            nurseHeader.createCell(6).setCellValue("Platform: Multi-User Accept");
            nurseHeader.createCell(10).setCellValue("Time to 1st Recipient");
            nurseHeader.createCell(11).setCellValue("1st Recipient");

            Row nurseRow = nurseSheet.createRow(3);
            nurseRow.createCell(0).setCellValue("TestGroup");
            nurseRow.createCell(1).setCellValue("Test Alarm");
            nurseRow.createCell(3).setCellValue("Normal");
            nurseRow.createCell(4).setCellValue(deviceName);
            nurseRow.createCell(5).setCellValue(ringtone);
            nurseRow.createCell(6).setCellValue(multiUserAccept);
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

    private void createTestOrdersWorkbookWithDevice(File excelFile, String deviceName, String ringtone, String multiUserAccept) throws Exception {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            // Unit Breakdown sheet
            Sheet unitSheet = wb.createSheet("Unit Breakdown");
            Row unitHeader = unitSheet.createRow(2);
            unitHeader.createCell(0).setCellValue("Facility");
            unitHeader.createCell(1).setCellValue("Common Unit Name");
            unitHeader.createCell(4).setCellValue("Orders Configuration Group");

            Row unitRow = unitSheet.createRow(3);
            unitRow.createCell(0).setCellValue("Test Facility");
            unitRow.createCell(1).setCellValue("Test Unit");
            unitRow.createCell(4).setCellValue("OrdersGroup");

            // Orders sheet
            Sheet ordersSheet = wb.createSheet("Order");
            Row ordersHeader = ordersSheet.createRow(2);
            ordersHeader.createCell(0).setCellValue("Configuration Group");
            ordersHeader.createCell(1).setCellValue("Common Alert or Alarm Name");
            ordersHeader.createCell(3).setCellValue("Priority");
            ordersHeader.createCell(4).setCellValue("Device - A");
            ordersHeader.createCell(5).setCellValue("Ringtone Device - A");
            ordersHeader.createCell(6).setCellValue("Platform: Multi-User Accept");
            ordersHeader.createCell(10).setCellValue("Time to 1st Recipient");
            ordersHeader.createCell(11).setCellValue("1st Recipient");

            Row ordersRow = ordersSheet.createRow(3);
            ordersRow.createCell(0).setCellValue("OrdersGroup");
            ordersRow.createCell(1).setCellValue("New Order");
            ordersRow.createCell(3).setCellValue("Normal");
            ordersRow.createCell(4).setCellValue(deviceName);
            ordersRow.createCell(5).setCellValue(ringtone);
            ordersRow.createCell(6).setCellValue(multiUserAccept);
            ordersRow.createCell(10).setCellValue("0");
            ordersRow.createCell(11).setCellValue("Pharmacist");

            // Nurse Call sheet (empty)
            Sheet nurseSheet = wb.createSheet("Nurse Call");
            Row nurseHeader = nurseSheet.createRow(2);
            nurseHeader.createCell(0).setCellValue("Configuration Group");
            nurseHeader.createCell(1).setCellValue("Common Alert or Alarm Name");

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

    private void createTestClinicalWorkbookWithDevice(File excelFile, String deviceName, String ringtone, String multiUserAccept) throws Exception {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            // Unit Breakdown sheet
            Sheet unitSheet = wb.createSheet("Unit Breakdown");
            Row unitHeader = unitSheet.createRow(2);
            unitHeader.createCell(0).setCellValue("Facility");
            unitHeader.createCell(1).setCellValue("Common Unit Name");
            unitHeader.createCell(3).setCellValue("Clinical Configuration Group");

            Row unitRow = unitSheet.createRow(3);
            unitRow.createCell(0).setCellValue("Test Facility");
            unitRow.createCell(1).setCellValue("Test Unit");
            unitRow.createCell(3).setCellValue("ClinicalGroup");

            // Patient Monitoring sheet
            Sheet clinicalSheet = wb.createSheet("Patient Monitoring");
            Row clinicalHeader = clinicalSheet.createRow(2);
            clinicalHeader.createCell(0).setCellValue("Configuration Group");
            clinicalHeader.createCell(1).setCellValue("Common Alert or Alarm Name");
            clinicalHeader.createCell(3).setCellValue("Priority");
            clinicalHeader.createCell(4).setCellValue("Device - A");
            clinicalHeader.createCell(5).setCellValue("Ringtone Device - A");
            clinicalHeader.createCell(6).setCellValue("Platform: Multi-User Accept");
            clinicalHeader.createCell(10).setCellValue("Time to 1st Recipient");
            clinicalHeader.createCell(11).setCellValue("1st Recipient");

            Row clinicalRow = clinicalSheet.createRow(3);
            clinicalRow.createCell(0).setCellValue("ClinicalGroup");
            clinicalRow.createCell(1).setCellValue("Critical Alert");
            clinicalRow.createCell(3).setCellValue("Normal");
            clinicalRow.createCell(4).setCellValue(deviceName);
            clinicalRow.createCell(5).setCellValue(ringtone);
            clinicalRow.createCell(6).setCellValue(multiUserAccept);
            clinicalRow.createCell(10).setCellValue("0");
            clinicalRow.createCell(11).setCellValue("Clinical Team");

            // Nurse Call sheet (empty)
            Sheet nurseSheet = wb.createSheet("Nurse Call");
            Row nurseHeader = nurseSheet.createRow(2);
            nurseHeader.createCell(0).setCellValue("Configuration Group");
            nurseHeader.createCell(1).setCellValue("Common Alert or Alarm Name");

            try (FileOutputStream fos = new FileOutputStream(excelFile)) {
                wb.write(fos);
            }
        }
    }
}
