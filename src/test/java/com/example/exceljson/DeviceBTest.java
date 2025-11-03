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
 * Test Device-B column functionality and combined interface logic.
 */
class DeviceBTest {

    /**
     * Helper to create a test workbook with Device-B column
     */
    private void createTestWorkbookWithDeviceB(File excelFile) throws Exception {
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

            // Nurse Call sheet with Device-B column
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

            // Row 1: Device-A has Edge, Device-B is blank
            Row nurseRow1 = nurseSheet.createRow(3);
            nurseRow1.createCell(0).setCellValue("Nurse Group 1");
            nurseRow1.createCell(1).setCellValue("Edge Only");
            nurseRow1.createCell(2).setCellValue("Edge Only Test");
            nurseRow1.createCell(3).setCellValue("Normal");
            nurseRow1.createCell(4).setCellValue("iPhone-Edge");
            nurseRow1.createCell(5).setCellValue("");
            nurseRow1.createCell(6).setCellValue("Ringtone1");
            nurseRow1.createCell(10).setCellValue("0");
            nurseRow1.createCell(11).setCellValue("Nurse Team");

            // Row 2: Device-A is blank, Device-B has VCS
            Row nurseRow2 = nurseSheet.createRow(4);
            nurseRow2.createCell(0).setCellValue("Nurse Group 1");
            nurseRow2.createCell(1).setCellValue("VCS Only");
            nurseRow2.createCell(2).setCellValue("VCS Only Test");
            nurseRow2.createCell(3).setCellValue("Normal");
            nurseRow2.createCell(4).setCellValue("");
            nurseRow2.createCell(5).setCellValue("Vocera VCS");
            nurseRow2.createCell(6).setCellValue("Ringtone2");
            nurseRow2.createCell(10).setCellValue("0");
            nurseRow2.createCell(11).setCellValue("Nurse Team");

            // Row 3: Device-A has Edge, Device-B has VCS (should combine both interfaces)
            Row nurseRow3 = nurseSheet.createRow(5);
            nurseRow3.createCell(0).setCellValue("Nurse Group 1");
            nurseRow3.createCell(1).setCellValue("Combined Edge and VCS");
            nurseRow3.createCell(2).setCellValue("Combined Test");
            nurseRow3.createCell(3).setCellValue("High");
            nurseRow3.createCell(4).setCellValue("iPhone-Edge");
            nurseRow3.createCell(5).setCellValue("VCS");
            nurseRow3.createCell(6).setCellValue("Ringtone3");
            nurseRow3.createCell(10).setCellValue("0");
            nurseRow3.createCell(11).setCellValue("Nurse Team");

            // Row 4: Device-A has VCS, Device-B has Edge (should combine both interfaces)
            Row nurseRow4 = nurseSheet.createRow(6);
            nurseRow4.createCell(0).setCellValue("Nurse Group 1");
            nurseRow4.createCell(1).setCellValue("Combined VCS and Edge");
            nurseRow4.createCell(2).setCellValue("Combined Test 2");
            nurseRow4.createCell(3).setCellValue("Urgent");
            nurseRow4.createCell(4).setCellValue("Vocera VCS");
            nurseRow4.createCell(5).setCellValue("Edge");
            nurseRow4.createCell(6).setCellValue("Ringtone4");
            nurseRow4.createCell(10).setCellValue("0");
            nurseRow4.createCell(11).setCellValue("Nurse Team");

            // Row 5: Both devices blank
            Row nurseRow5 = nurseSheet.createRow(7);
            nurseRow5.createCell(0).setCellValue("Nurse Group 1");
            nurseRow5.createCell(1).setCellValue("No Device");
            nurseRow5.createCell(2).setCellValue("No Device Test");
            nurseRow5.createCell(3).setCellValue("Normal");
            nurseRow5.createCell(4).setCellValue("");
            nurseRow5.createCell(5).setCellValue("");
            nurseRow5.createCell(6).setCellValue("Ringtone5");
            nurseRow5.createCell(10).setCellValue("0");
            nurseRow5.createCell(11).setCellValue("Nurse Team");

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
    void testDeviceBParsing() throws Exception {
        Path tempDir = Files.createTempDirectory("device-b-test");
        File excelFile = tempDir.resolve("test.xlsx").toFile();

        createTestWorkbookWithDeviceB(excelFile);

        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(excelFile);

        // Verify that Device-B was parsed correctly
        assertEquals(5, parser.nurseCalls.size(), "Should have 5 nurse call rows");
        
        // Check first row: Edge in A, blank in B
        assertEquals("iPhone-Edge", parser.nurseCalls.get(0).deviceA);
        assertEquals("", parser.nurseCalls.get(0).deviceB);
        
        // Check second row: blank in A, VCS in B
        assertEquals("", parser.nurseCalls.get(1).deviceA);
        assertEquals("Vocera VCS", parser.nurseCalls.get(1).deviceB);
        
        // Check third row: Edge in A, VCS in B
        assertEquals("iPhone-Edge", parser.nurseCalls.get(2).deviceA);
        assertEquals("VCS", parser.nurseCalls.get(2).deviceB);
        
        // Check fourth row: VCS in A, Edge in B
        assertEquals("Vocera VCS", parser.nurseCalls.get(3).deviceA);
        assertEquals("Edge", parser.nurseCalls.get(3).deviceB);

        // Clean up
        Files.deleteIfExists(excelFile.toPath());
        Files.deleteIfExists(tempDir);
    }

    @Test
    void testCombinedInterfaces() throws Exception {
        Path tempDir = Files.createTempDirectory("device-b-combined-test");
        File excelFile = tempDir.resolve("test.xlsx").toFile();

        createTestWorkbookWithDeviceB(excelFile);

        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(excelFile);

        var nurseJson = parser.buildNurseCallsJson();
        var flows = (List<?>) nurseJson.get("deliveryFlows");
        
        assertEquals(5, flows.size(), "Should have 5 nurse call flows");

        // Flow 0: Edge in A only - should have OutgoingWCTP interface only
        var flow0 = (Map<?, ?>) flows.get(0);
        var interfaces0 = (List<?>) flow0.get("interfaces");
        assertEquals(1, interfaces0.size(), "Edge only flow should have 1 interface");
        var iface0 = (Map<?, ?>) interfaces0.get(0);
        assertEquals("OutgoingWCTP", iface0.get("componentName"));

        // Flow 1: VCS in B only - should have VMP interface only
        var flow1 = (Map<?, ?>) flows.get(1);
        var interfaces1 = (List<?>) flow1.get("interfaces");
        assertEquals(1, interfaces1.size(), "VCS only flow should have 1 interface");
        var iface1 = (Map<?, ?>) interfaces1.get(0);
        assertEquals("VMP", iface1.get("componentName"));

        // Flow 2: Edge in A and VCS in B - should have BOTH interfaces
        var flow2 = (Map<?, ?>) flows.get(2);
        var interfaces2 = (List<?>) flow2.get("interfaces");
        assertEquals(2, interfaces2.size(), "Combined flow should have 2 interfaces");
        
        var iface2_0 = (Map<?, ?>) interfaces2.get(0);
        assertEquals("OutgoingWCTP", iface2_0.get("componentName"), "First interface should be OutgoingWCTP");
        assertEquals("OutgoingWCTP", iface2_0.get("referenceName"), "First interface reference should be OutgoingWCTP");
        
        var iface2_1 = (Map<?, ?>) interfaces2.get(1);
        assertEquals("VMP", iface2_1.get("componentName"), "Second interface should be VMP");
        assertEquals("VMP", iface2_1.get("referenceName"), "Second interface reference should be VMP");

        // Flow 3: VCS in A and Edge in B - should have BOTH interfaces
        var flow3 = (Map<?, ?>) flows.get(3);
        var interfaces3 = (List<?>) flow3.get("interfaces");
        assertEquals(2, interfaces3.size(), "Combined flow (reversed) should have 2 interfaces");
        
        var iface3_0 = (Map<?, ?>) interfaces3.get(0);
        assertEquals("OutgoingWCTP", iface3_0.get("componentName"), "First interface should be OutgoingWCTP");
        
        var iface3_1 = (Map<?, ?>) interfaces3.get(1);
        assertEquals("VMP", iface3_1.get("componentName"), "Second interface should be VMP");

        // Flow 4: No devices - should have no interfaces
        var flow4 = (Map<?, ?>) flows.get(4);
        var interfaces4 = (List<?>) flow4.get("interfaces");
        assertEquals(0, interfaces4.size(), "No device flow should have 0 interfaces");

        // Clean up
        Files.deleteIfExists(excelFile.toPath());
        Files.deleteIfExists(tempDir);
    }

    @Test
    void testCombinedInterfacesWithCustomReferences() throws Exception {
        Path tempDir = Files.createTempDirectory("device-b-custom-ref-test");
        File excelFile = tempDir.resolve("test.xlsx").toFile();

        createTestWorkbookWithDeviceB(excelFile);

        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(excelFile);
        
        // Set custom reference names
        parser.setInterfaceReferences("CustomEdge", "CustomVCS");

        var nurseJson = parser.buildNurseCallsJson();
        var flows = (List<?>) nurseJson.get("deliveryFlows");

        // Flow 2: Edge in A and VCS in B - should have both with custom references
        var flow2 = (Map<?, ?>) flows.get(2);
        var interfaces2 = (List<?>) flow2.get("interfaces");
        assertEquals(2, interfaces2.size(), "Combined flow should have 2 interfaces");
        
        var iface2_0 = (Map<?, ?>) interfaces2.get(0);
        assertEquals("OutgoingWCTP", iface2_0.get("componentName"));
        assertEquals("CustomEdge", iface2_0.get("referenceName"), "Should use custom Edge reference");
        
        var iface2_1 = (Map<?, ?>) interfaces2.get(1);
        assertEquals("VMP", iface2_1.get("componentName"));
        assertEquals("CustomVCS", iface2_1.get("referenceName"), "Should use custom VCS reference");

        // Clean up
        Files.deleteIfExists(excelFile.toPath());
        Files.deleteIfExists(tempDir);
    }

    @Test
    void testMergedFlowsWithCombinedInterfaces() throws Exception {
        Path tempDir = Files.createTempDirectory("device-b-merged-test");
        File excelFile = tempDir.resolve("test.xlsx").toFile();

        createTestWorkbookWithDeviceB(excelFile);

        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(excelFile);

        // Build with merge mode enabled
        var nurseJson = parser.buildNurseCallsJson(true);
        var flows = (List<?>) nurseJson.get("deliveryFlows");

        // Find the flow with combined interfaces
        boolean foundCombined = false;
        for (Object flowObj : flows) {
            var flow = (Map<?, ?>) flowObj;
            var interfaces = (List<?>) flow.get("interfaces");
            
            if (interfaces.size() == 2) {
                foundCombined = true;
                var iface0 = (Map<?, ?>) interfaces.get(0);
                var iface1 = (Map<?, ?>) interfaces.get(1);
                
                assertEquals("OutgoingWCTP", iface0.get("componentName"));
                assertEquals("VMP", iface1.get("componentName"));
            }
        }

        assertTrue(foundCombined, "Should find at least one flow with combined interfaces in merged mode");

        // Clean up
        Files.deleteIfExists(excelFile.toPath());
        Files.deleteIfExists(tempDir);
    }
}
