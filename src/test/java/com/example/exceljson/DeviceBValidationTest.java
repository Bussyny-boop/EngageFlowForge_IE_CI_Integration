package com.example.exceljson;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.*;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Device-B validation highlighting and interface logic.
 * 
 * Requirements:
 * 1. Device-B cells with invalid keywords (not VCS, Edge, Vocera, XMPP) should be highlighted
 * 2. Empty Device-B cells should NOT be highlighted
 * 3. When Device-A has valid keyword and Device-B is non-empty with invalid keyword,
 *    apply both Device-A interface AND default checkbox interfaces
 */
public class DeviceBValidationTest {

    @TempDir
    Path tempDir;

    /**
     * Test that Device-B with invalid keyword triggers default interface addition
     * when Device-A has valid keyword and default VMP checkbox is selected.
     */
    @Test
    public void testDeviceAValidDeviceBInvalidWithDefaultCheckbox() throws Exception {
        File xlsxFile = tempDir.resolve("test-deviceb-validation.xlsx").toFile();
        createTestWorkbook(xlsxFile, "Edge", "Invalid");

        ExcelParserV5 parser = new ExcelParserV5();
        parser.setDefaultInterfaces(false, true, false, false); // Enable default VMP checkbox only
        parser.load(xlsxFile);

        Map<String, Object> nurseJson = parser.buildNurseCallsJson();
        List<?> flows = (List<?>) nurseJson.get("deliveryFlows");
        assertNotNull(flows);
        assertTrue(flows.size() > 0);

        // Find the flow with Device-A = "Edge" and Device-B = "Invalid"
        Map<?, ?> flow = (Map<?, ?>) flows.getFirst();
        assertNotNull(flow, "Should find the test flow");

        // Check interfaces - should have both Edge (from Device-A) and VMP (from default checkbox)
        List<?> interfaces = (List<?>) flow.get("interfaces");
        assertNotNull(interfaces, "Interfaces should not be null");
        assertEquals(2, interfaces.size(), "Should have 2 interfaces: Edge from Device-A and VMP from default checkbox");

        // Verify Edge interface (from Device-A)
        boolean hasEdge = interfaces.stream()
            .anyMatch(iface -> "OutgoingWCTP".equals(((Map<?, ?>) iface).get("componentName")));
        assertTrue(hasEdge, "Should have Edge interface from Device-A");

        // Verify VMP interface (from default checkbox)
        boolean hasVmp = interfaces.stream()
            .anyMatch(iface -> "VMP".equals(((Map<?, ?>) iface).get("componentName")));
        assertTrue(hasVmp, "Should have VMP interface from default checkbox");
    }

    /**
     * Test that Device-B with valid keyword does NOT trigger default interface addition.
     */
    @Test
    public void testDeviceAValidDeviceBValidNoDefaultAdded() throws Exception {
        File xlsxFile = tempDir.resolve("test-deviceb-valid.xlsx").toFile();
        createTestWorkbook(xlsxFile, "Edge", "VCS");

        ExcelParserV5 parser = new ExcelParserV5();
        parser.setDefaultInterfaces(false, true, false, false); // Enable default VMP checkbox
        parser.load(xlsxFile);

        Map<String, Object> nurseJson = parser.buildNurseCallsJson();
        List<?> flows = (List<?>) nurseJson.get("deliveryFlows");
        assertNotNull(flows);
        assertTrue(flows.size() > 0);

        // Find the flow with Device-A = "Edge" and Device-B = "VCS"
        Map<?, ?> flow = (Map<?, ?>) flows.getFirst();
        assertNotNull(flow, "Should find the test flow");

        // Check interfaces - should have both Edge and VMP from devices, NOT from default checkbox
        List<?> interfaces = (List<?>) flow.get("interfaces");
        assertNotNull(interfaces, "Interfaces should not be null");
        assertEquals(2, interfaces.size(), "Should have 2 interfaces: Edge from Device-A and VMP from Device-B");

        // Verify Edge interface
        boolean hasEdge = interfaces.stream()
            .anyMatch(iface -> "OutgoingWCTP".equals(((Map<?, ?>) iface).get("componentName")));
        assertTrue(hasEdge, "Should have Edge interface");

        // Verify VMP interface
        boolean hasVmp = interfaces.stream()
            .anyMatch(iface -> "VMP".equals(((Map<?, ?>) iface).get("componentName")));
        assertTrue(hasVmp, "Should have VMP interface");
    }

    /**
     * Test that empty Device-B does NOT trigger default interface addition.
     */
    @Test
    public void testDeviceAValidDeviceBEmptyNoDefaultAdded() throws Exception {
        File xlsxFile = tempDir.resolve("test-deviceb-empty.xlsx").toFile();
        createTestWorkbook(xlsxFile, "Edge", "");

        ExcelParserV5 parser = new ExcelParserV5();
        parser.setDefaultInterfaces(false, true, false, false); // Enable default VMP checkbox
        parser.load(xlsxFile);

        Map<String, Object> nurseJson = parser.buildNurseCallsJson();
        List<?> flows = (List<?>) nurseJson.get("deliveryFlows");
        assertNotNull(flows);
        assertTrue(flows.size() > 0);

        // Find the flow with Device-A = "Edge" and Device-B = empty
        Map<?, ?> flow = (Map<?, ?>) flows.getFirst();
        assertNotNull(flow, "Should find the test flow");

        // Check interfaces - should have only Edge from Device-A, no default VMP
        List<?> interfaces = (List<?>) flow.get("interfaces");
        assertNotNull(interfaces, "Interfaces should not be null");
        assertEquals(1, interfaces.size(), "Should have only 1 interface: Edge from Device-A");

        // Verify Edge interface
        boolean hasEdge = interfaces.stream()
            .anyMatch(iface -> "OutgoingWCTP".equals(((Map<?, ?>) iface).get("componentName")));
        assertTrue(hasEdge, "Should have Edge interface from Device-A");
    }

    /**
     * Test validation method for Device-B keyword checking.
     */
    @Test
    public void testHasValidRecipientKeywordForDeviceB() {
        ExcelParserV5 parser = new ExcelParserV5();

        // Valid keywords (case-insensitive)
        assertTrue(parser.hasValidRecipientKeyword("VCS"));
        assertTrue(parser.hasValidRecipientKeyword("vcs"));
        assertTrue(parser.hasValidRecipientKeyword("Edge"));
        assertTrue(parser.hasValidRecipientKeyword("edge"));
        assertTrue(parser.hasValidRecipientKeyword("Vocera"));
        assertTrue(parser.hasValidRecipientKeyword("vocera"));
        assertTrue(parser.hasValidRecipientKeyword("XMPP"));
        assertTrue(parser.hasValidRecipientKeyword("xmpp"));
        assertTrue(parser.hasValidRecipientKeyword("iPhone-Edge"));
        assertTrue(parser.hasValidRecipientKeyword("VMP"));
        assertTrue(parser.hasValidRecipientKeyword("vmp"));
        assertTrue(parser.hasValidRecipientKeyword("OutgoingWCTP"));
        assertTrue(parser.hasValidRecipientKeyword("outgoingwctp"));
        assertTrue(parser.hasValidRecipientKeyword("OUTGOINGWCTP"));

        // Empty/blank should be considered valid (not highlighted)
        assertTrue(parser.hasValidRecipientKeyword(""));
        assertTrue(parser.hasValidRecipientKeyword(null));
        assertTrue(parser.hasValidRecipientKeyword("   "));

        // Invalid keywords should return false
        assertFalse(parser.hasValidRecipientKeyword("Invalid"));
        assertFalse(parser.hasValidRecipientKeyword("Unknown"));
        assertFalse(parser.hasValidRecipientKeyword("Test Device"));
    }

    // Helper method to create test workbook

    private void createTestWorkbook(File xlsxFile, String deviceA, String deviceB) throws IOException {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            // Unit Breakdown sheet
            Sheet unitSheet = wb.createSheet("Unit Breakdown");
            Row unitHeader = unitSheet.createRow(2);
            unitHeader.createCell(0).setCellValue("Facility");
            unitHeader.createCell(1).setCellValue("Common Unit Name");
            unitHeader.createCell(2).setCellValue("Nurse Call Configuration Group");
            unitHeader.createCell(3).setCellValue("Patient Monitoring Configuration Group");

            Row unitRow = unitSheet.createRow(3);
            unitRow.createCell(0).setCellValue("Test Facility");
            unitRow.createCell(1).setCellValue("Test Unit");
            unitRow.createCell(2).setCellValue("Test Group");
            unitRow.createCell(3).setCellValue("");

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

            // Data row
            Row dataRow = nurseSheet.createRow(3);
            dataRow.createCell(0).setCellValue("Test Group");
            dataRow.createCell(1).setCellValue("Test Alarm");
            dataRow.createCell(2).setCellValue("Test Sending Name");
            dataRow.createCell(3).setCellValue("Normal");
            dataRow.createCell(4).setCellValue(deviceA);
            dataRow.createCell(5).setCellValue(deviceB);
            dataRow.createCell(6).setCellValue("Ringtone1");
            dataRow.createCell(10).setCellValue("0");
            dataRow.createCell(11).setCellValue("Group: Test Group");

            // Patient Monitoring sheet (empty)
            Sheet clinicalSheet = wb.createSheet("Patient Monitoring");
            Row clinicalHeader = clinicalSheet.createRow(2);
            clinicalHeader.createCell(0).setCellValue("Configuration Group");
            clinicalHeader.createCell(1).setCellValue("Common Alert or Alarm Name");

            try (FileOutputStream fos = new FileOutputStream(xlsxFile)) {
                wb.write(fos);
            }
        }
    }
}
