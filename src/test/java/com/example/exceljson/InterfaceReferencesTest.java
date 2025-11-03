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

class InterfaceReferencesTest {

    /**
     * Helper to create a minimal test workbook with different device types
     */
    private void createTestWorkbook(File excelFile) throws Exception {
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
            unitRow.createCell(1).setCellValue("ICU");
            unitRow.createCell(2).setCellValue("Nurse Group 1");
            unitRow.createCell(3).setCellValue("Clinical Group 1");

            // Nurse Call sheet with different device types
            Sheet nurseSheet = wb.createSheet("Nurse Call");
            Row nurseHeader = nurseSheet.createRow(2);
            nurseHeader.createCell(0).setCellValue("Configuration Group");
            nurseHeader.createCell(1).setCellValue("Common Alert or Alarm Name");
            nurseHeader.createCell(2).setCellValue("Sending System Alert Name");
            nurseHeader.createCell(3).setCellValue("Priority");
            nurseHeader.createCell(4).setCellValue("Device - A");
            nurseHeader.createCell(5).setCellValue("Ringtone Device - A");
            nurseHeader.createCell(10).setCellValue("Time to 1st Recipient");
            nurseHeader.createCell(11).setCellValue("1st Recipient");

            // Row with Edge device
            Row nurseRow1 = nurseSheet.createRow(3);
            nurseRow1.createCell(0).setCellValue("Nurse Group 1");
            nurseRow1.createCell(1).setCellValue("Edge Alarm");
            nurseRow1.createCell(2).setCellValue("Edge Test");
            nurseRow1.createCell(3).setCellValue("Normal");
            nurseRow1.createCell(4).setCellValue("iPhone-Edge");
            nurseRow1.createCell(5).setCellValue("Ringtone1");
            nurseRow1.createCell(10).setCellValue("0");
            nurseRow1.createCell(11).setCellValue("Nurse Team");

            // Row with VCS device
            Row nurseRow2 = nurseSheet.createRow(4);
            nurseRow2.createCell(0).setCellValue("Nurse Group 1");
            nurseRow2.createCell(1).setCellValue("VCS Alarm");
            nurseRow2.createCell(2).setCellValue("VCS Test");
            nurseRow2.createCell(3).setCellValue("Normal");
            nurseRow2.createCell(4).setCellValue("Vocera VCS");
            nurseRow2.createCell(5).setCellValue("Ringtone2");
            nurseRow2.createCell(10).setCellValue("0");
            nurseRow2.createCell(11).setCellValue("Nurse Team");

            // Row with blank device (should have no interfaces)
            Row nurseRow3 = nurseSheet.createRow(5);
            nurseRow3.createCell(0).setCellValue("Nurse Group 1");
            nurseRow3.createCell(1).setCellValue("No Device Alarm");
            nurseRow3.createCell(2).setCellValue("No Device Test");
            nurseRow3.createCell(3).setCellValue("Normal");
            nurseRow3.createCell(4).setCellValue("");
            nurseRow3.createCell(5).setCellValue("Ringtone3");
            nurseRow3.createCell(10).setCellValue("0");
            nurseRow3.createCell(11).setCellValue("Nurse Team");

            // Patient Monitoring sheet (empty for this test)
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
    void testDefaultInterfaceReferences() throws Exception {
        Path tempDir = Files.createTempDirectory("interface-ref-test");
        File excelFile = tempDir.resolve("test.xlsx").toFile();

        createTestWorkbook(excelFile);

        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(excelFile);

        // Don't set custom references - use defaults
        var nurseJson = parser.buildNurseCallsJson();
        var flows = (List<?>) nurseJson.get("deliveryFlows");
        
        assertEquals(3, flows.size(), "Should have 3 nurse call flows");

        // Check Edge device flow
        var edgeFlow = (Map<?, ?>) flows.get(0);
        var edgeInterfaces = (List<?>) edgeFlow.get("interfaces");
        assertEquals(1, edgeInterfaces.size(), "Edge flow should have 1 interface");
        var edgeInterface = (Map<?, ?>) edgeInterfaces.get(0);
        assertEquals("OutgoingWCTP", edgeInterface.get("componentName"), "Edge should use OutgoingWCTP component");
        assertEquals("OutgoingWCTP", edgeInterface.get("referenceName"), "Edge should use default OutgoingWCTP reference");

        // Check VCS device flow
        var vcsFlow = (Map<?, ?>) flows.get(1);
        var vcsInterfaces = (List<?>) vcsFlow.get("interfaces");
        assertEquals(1, vcsInterfaces.size(), "VCS flow should have 1 interface");
        var vcsInterface = (Map<?, ?>) vcsInterfaces.get(0);
        assertEquals("VMP", vcsInterface.get("componentName"), "VCS should use VMP component");
        assertEquals("VMP", vcsInterface.get("referenceName"), "VCS should use default VMP reference");

        // Check blank device flow
        var blankFlow = (Map<?, ?>) flows.get(2);
        var blankInterfaces = (List<?>) blankFlow.get("interfaces");
        assertTrue(blankInterfaces.isEmpty(), "Blank device should have no interfaces");

        // Clean up
        Files.deleteIfExists(excelFile.toPath());
        Files.deleteIfExists(tempDir);
    }

    @Test
    void testCustomInterfaceReferences() throws Exception {
        Path tempDir = Files.createTempDirectory("interface-ref-custom-test");
        File excelFile = tempDir.resolve("test.xlsx").toFile();

        createTestWorkbook(excelFile);

        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(excelFile);

        // Set custom references
        parser.setInterfaceReferences("CustomEdge", "CustomVCS");

        var nurseJson = parser.buildNurseCallsJson();
        var flows = (List<?>) nurseJson.get("deliveryFlows");
        
        assertEquals(3, flows.size(), "Should have 3 nurse call flows");

        // Check Edge device flow with custom reference
        var edgeFlow = (Map<?, ?>) flows.get(0);
        var edgeInterfaces = (List<?>) edgeFlow.get("interfaces");
        var edgeInterface = (Map<?, ?>) edgeInterfaces.get(0);
        assertEquals("OutgoingWCTP", edgeInterface.get("componentName"), "Edge component name should stay the same");
        assertEquals("CustomEdge", edgeInterface.get("referenceName"), "Edge should use custom reference");

        // Check VCS device flow with custom reference
        var vcsFlow = (Map<?, ?>) flows.get(1);
        var vcsInterfaces = (List<?>) vcsFlow.get("interfaces");
        var vcsInterface = (Map<?, ?>) vcsInterfaces.get(0);
        assertEquals("VMP", vcsInterface.get("componentName"), "VCS component name should stay the same");
        assertEquals("CustomVCS", vcsInterface.get("referenceName"), "VCS should use custom reference");

        // Clean up
        Files.deleteIfExists(excelFile.toPath());
        Files.deleteIfExists(tempDir);
    }

    @Test
    void testCaseInsensitiveDeviceMatching() throws Exception {
        Path tempDir = Files.createTempDirectory("interface-ref-case-test");
        File excelFile = tempDir.resolve("test.xlsx").toFile();

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

            // Nurse Call sheet with various case variations
            Sheet nurseSheet = wb.createSheet("Nurse Call");
            Row nurseHeader = nurseSheet.createRow(2);
            nurseHeader.createCell(0).setCellValue("Configuration Group");
            nurseHeader.createCell(1).setCellValue("Common Alert or Alarm Name");
            nurseHeader.createCell(4).setCellValue("Device - A");
            nurseHeader.createCell(10).setCellValue("Time to 1st Recipient");
            nurseHeader.createCell(11).setCellValue("1st Recipient");

            // Test "EDGE" (uppercase)
            Row row1 = nurseSheet.createRow(3);
            row1.createCell(0).setCellValue("Nurse Group 1");
            row1.createCell(1).setCellValue("Test1");
            row1.createCell(4).setCellValue("EDGE");
            row1.createCell(10).setCellValue("0");
            row1.createCell(11).setCellValue("Team");

            // Test "vcs" (lowercase)
            Row row2 = nurseSheet.createRow(4);
            row2.createCell(0).setCellValue("Nurse Group 1");
            row2.createCell(1).setCellValue("Test2");
            row2.createCell(4).setCellValue("vcs");
            row2.createCell(10).setCellValue("0");
            row2.createCell(11).setCellValue("Team");

            // Patient Monitoring sheet (empty)
            Sheet clinicalSheet = wb.createSheet("Patient Monitoring");
            Row clinicalHeader = clinicalSheet.createRow(2);
            clinicalHeader.createCell(0).setCellValue("Configuration Group");

            try (FileOutputStream fos = new FileOutputStream(excelFile)) {
                wb.write(fos);
            }
        }

        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(excelFile);

        var nurseJson = parser.buildNurseCallsJson();
        var flows = (List<?>) nurseJson.get("deliveryFlows");

        // Both should match despite different cases
        var flow1 = (Map<?, ?>) flows.get(0);
        var interfaces1 = (List<?>) flow1.get("interfaces");
        assertEquals(1, interfaces1.size(), "EDGE (uppercase) should match");
        var iface1 = (Map<?, ?>) interfaces1.get(0);
        assertEquals("OutgoingWCTP", iface1.get("componentName"));

        var flow2 = (Map<?, ?>) flows.get(1);
        var interfaces2 = (List<?>) flow2.get("interfaces");
        assertEquals(1, interfaces2.size(), "vcs (lowercase) should match");
        var iface2 = (Map<?, ?>) interfaces2.get(0);
        assertEquals("VMP", iface2.get("componentName"));

        // Clean up
        Files.deleteIfExists(excelFile.toPath());
        Files.deleteIfExists(tempDir);
    }

    @Test
    void testMergedFlowsWithInterfaces() throws Exception {
        Path tempDir = Files.createTempDirectory("interface-ref-merged-test");
        File excelFile = tempDir.resolve("test.xlsx").toFile();

        createTestWorkbook(excelFile);

        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(excelFile);
        parser.setInterfaceReferences("MergedEdge", "MergedVCS");

        // Build with merge mode enabled
        var nurseJson = parser.buildNurseCallsJson(true);
        var flows = (List<?>) nurseJson.get("deliveryFlows");

        // Verify that merged flows also get the correct interfaces
        for (Object flowObj : flows) {
            var flow = (Map<?, ?>) flowObj;
            var interfaces = (List<?>) flow.get("interfaces");
            
            if (interfaces.isEmpty()) {
                // Blank device should have no interfaces
                continue;
            }

            var iface = (Map<?, ?>) interfaces.get(0);
            String refName = (String) iface.get("referenceName");
            
            // Should be either the custom Edge or VCS reference
            assertTrue(
                "MergedEdge".equals(refName) || "MergedVCS".equals(refName),
                "Merged flow should use custom reference names"
            );
        }

        // Clean up
        Files.deleteIfExists(excelFile.toPath());
        Files.deleteIfExists(tempDir);
    }
}
