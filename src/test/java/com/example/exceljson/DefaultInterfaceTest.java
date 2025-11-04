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
 * Test default interface functionality when Device A and Device B are blank
 */
class DefaultInterfaceTest {

    /**
     * Helper to create a minimal test workbook with blank device fields
     */
    private void createTestWorkbookWithBlankDevices(File excelFile) throws Exception {
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

            // Nurse Call sheet with blank device fields
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

            // Row with blank devices
            Row nurseRow1 = nurseSheet.createRow(3);
            nurseRow1.createCell(0).setCellValue("Nurse Group 1");
            nurseRow1.createCell(1).setCellValue("Blank Device Alarm");
            nurseRow1.createCell(2).setCellValue("Blank Test");
            nurseRow1.createCell(3).setCellValue("Normal");
            nurseRow1.createCell(4).setCellValue("");  // Device A blank
            nurseRow1.createCell(5).setCellValue("Ringtone1");
            nurseRow1.createCell(10).setCellValue("0");
            nurseRow1.createCell(11).setCellValue("Nurse Team");

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
    void testNoDefaultInterfaces() throws Exception {
        Path tempDir = Files.createTempDirectory("default-interface-none-test");
        File excelFile = tempDir.resolve("test.xlsx").toFile();

        createTestWorkbookWithBlankDevices(excelFile);

        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(excelFile);

        // Don't set default interfaces
        var nurseJson = parser.buildNurseCallsJson();
        var flows = (List<?>) nurseJson.get("deliveryFlows");
        
        assertEquals(1, flows.size(), "Should have 1 nurse call flow");

        // Check blank device flow has no interfaces
        var blankFlow = (Map<?, ?>) flows.get(0);
        var blankInterfaces = (List<?>) blankFlow.get("interfaces");
        assertTrue(blankInterfaces.isEmpty(), "Blank device with no defaults should have no interfaces");

        // Clean up
        Files.deleteIfExists(excelFile.toPath());
        Files.deleteIfExists(tempDir);
    }

    @Test
    void testDefaultEdgeInterface() throws Exception {
        Path tempDir = Files.createTempDirectory("default-interface-edge-test");
        File excelFile = tempDir.resolve("test.xlsx").toFile();

        createTestWorkbookWithBlankDevices(excelFile);

        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(excelFile);

        // Set default Edge interface
        parser.setDefaultInterfaces(true, false);

        var nurseJson = parser.buildNurseCallsJson();
        var flows = (List<?>) nurseJson.get("deliveryFlows");
        
        assertEquals(1, flows.size(), "Should have 1 nurse call flow");

        // Check blank device flow gets Edge interface
        var flow = (Map<?, ?>) flows.get(0);
        var interfaces = (List<?>) flow.get("interfaces");
        assertEquals(1, interfaces.size(), "Should have 1 interface from default");
        
        var iface = (Map<?, ?>) interfaces.get(0);
        assertEquals("OutgoingWCTP", iface.get("componentName"), "Should use OutgoingWCTP component");
        assertEquals("OutgoingWCTP", iface.get("referenceName"), "Should use default OutgoingWCTP reference");

        // Clean up
        Files.deleteIfExists(excelFile.toPath());
        Files.deleteIfExists(tempDir);
    }

    @Test
    void testDefaultVmpInterface() throws Exception {
        Path tempDir = Files.createTempDirectory("default-interface-vmp-test");
        File excelFile = tempDir.resolve("test.xlsx").toFile();

        createTestWorkbookWithBlankDevices(excelFile);

        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(excelFile);

        // Set default VMP interface
        parser.setDefaultInterfaces(false, true);

        var nurseJson = parser.buildNurseCallsJson();
        var flows = (List<?>) nurseJson.get("deliveryFlows");
        
        assertEquals(1, flows.size(), "Should have 1 nurse call flow");

        // Check blank device flow gets VMP interface
        var flow = (Map<?, ?>) flows.get(0);
        var interfaces = (List<?>) flow.get("interfaces");
        assertEquals(1, interfaces.size(), "Should have 1 interface from default");
        
        var iface = (Map<?, ?>) interfaces.get(0);
        assertEquals("VMP", iface.get("componentName"), "Should use VMP component");
        assertEquals("VMP", iface.get("referenceName"), "Should use default VMP reference");

        // Clean up
        Files.deleteIfExists(excelFile.toPath());
        Files.deleteIfExists(tempDir);
    }

    @Test
    void testBothDefaultInterfaces() throws Exception {
        Path tempDir = Files.createTempDirectory("default-interface-both-test");
        File excelFile = tempDir.resolve("test.xlsx").toFile();

        createTestWorkbookWithBlankDevices(excelFile);

        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(excelFile);

        // Set both default interfaces
        parser.setDefaultInterfaces(true, true);

        var nurseJson = parser.buildNurseCallsJson();
        var flows = (List<?>) nurseJson.get("deliveryFlows");
        
        assertEquals(1, flows.size(), "Should have 1 nurse call flow");

        // Check blank device flow gets both interfaces
        var flow = (Map<?, ?>) flows.get(0);
        var interfaces = (List<?>) flow.get("interfaces");
        assertEquals(2, interfaces.size(), "Should have 2 interfaces from defaults");
        
        var iface1 = (Map<?, ?>) interfaces.get(0);
        assertEquals("OutgoingWCTP", iface1.get("componentName"), "First should be OutgoingWCTP");
        assertEquals("OutgoingWCTP", iface1.get("referenceName"));
        
        var iface2 = (Map<?, ?>) interfaces.get(1);
        assertEquals("VMP", iface2.get("componentName"), "Second should be VMP");
        assertEquals("VMP", iface2.get("referenceName"));

        // Clean up
        Files.deleteIfExists(excelFile.toPath());
        Files.deleteIfExists(tempDir);
    }

    @Test
    void testDefaultInterfacesWithCustomReferences() throws Exception {
        Path tempDir = Files.createTempDirectory("default-interface-custom-ref-test");
        File excelFile = tempDir.resolve("test.xlsx").toFile();

        createTestWorkbookWithBlankDevices(excelFile);

        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(excelFile);

        // Set custom references and both defaults
        parser.setInterfaceReferences("CustomEdge", "CustomVMP");
        parser.setDefaultInterfaces(true, true);

        var nurseJson = parser.buildNurseCallsJson();
        var flows = (List<?>) nurseJson.get("deliveryFlows");
        
        var flow = (Map<?, ?>) flows.get(0);
        var interfaces = (List<?>) flow.get("interfaces");
        assertEquals(2, interfaces.size(), "Should have 2 interfaces");
        
        var iface1 = (Map<?, ?>) interfaces.get(0);
        assertEquals("OutgoingWCTP", iface1.get("componentName"));
        assertEquals("CustomEdge", iface1.get("referenceName"), "Should use custom Edge reference");
        
        var iface2 = (Map<?, ?>) interfaces.get(1);
        assertEquals("VMP", iface2.get("componentName"));
        assertEquals("CustomVMP", iface2.get("referenceName"), "Should use custom VMP reference");

        // Clean up
        Files.deleteIfExists(excelFile.toPath());
        Files.deleteIfExists(tempDir);
    }

    @Test
    void testDefaultInterfacesOnlyApplyWhenDevicesBlank() throws Exception {
        Path tempDir = Files.createTempDirectory("default-interface-only-blank-test");
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

            // Nurse Call sheet with one blank and one Edge device
            Sheet nurseSheet = wb.createSheet("Nurse Call");
            Row nurseHeader = nurseSheet.createRow(2);
            nurseHeader.createCell(0).setCellValue("Configuration Group");
            nurseHeader.createCell(1).setCellValue("Common Alert or Alarm Name");
            nurseHeader.createCell(4).setCellValue("Device - A");
            nurseHeader.createCell(10).setCellValue("Time to 1st Recipient");
            nurseHeader.createCell(11).setCellValue("1st Recipient");

            // Row with blank device - should use default
            Row row1 = nurseSheet.createRow(3);
            row1.createCell(0).setCellValue("Nurse Group 1");
            row1.createCell(1).setCellValue("Blank");
            row1.createCell(4).setCellValue("");
            row1.createCell(10).setCellValue("0");
            row1.createCell(11).setCellValue("Team");

            // Row with Edge device - should NOT use default
            Row row2 = nurseSheet.createRow(4);
            row2.createCell(0).setCellValue("Nurse Group 1");
            row2.createCell(1).setCellValue("Edge");
            row2.createCell(4).setCellValue("iPhone-Edge");
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

        // Set default VMP interface
        parser.setDefaultInterfaces(false, true);

        var nurseJson = parser.buildNurseCallsJson();
        var flows = (List<?>) nurseJson.get("deliveryFlows");
        
        assertEquals(2, flows.size(), "Should have 2 flows");

        // First flow (blank device) should use default VMP
        var blankFlow = (Map<?, ?>) flows.get(0);
        var blankInterfaces = (List<?>) blankFlow.get("interfaces");
        assertEquals(1, blankInterfaces.size(), "Blank device should use default");
        var blankIface = (Map<?, ?>) blankInterfaces.get(0);
        assertEquals("VMP", blankIface.get("componentName"), "Blank should use default VMP");

        // Second flow (Edge device) should use Edge, NOT default
        var edgeFlow = (Map<?, ?>) flows.get(1);
        var edgeInterfaces = (List<?>) edgeFlow.get("interfaces");
        assertEquals(1, edgeInterfaces.size(), "Edge device should have interface from device");
        var edgeIface = (Map<?, ?>) edgeInterfaces.get(0);
        assertEquals("OutgoingWCTP", edgeIface.get("componentName"), "Edge should use OutgoingWCTP from device");

        // Clean up
        Files.deleteIfExists(excelFile.toPath());
        Files.deleteIfExists(tempDir);
    }
}
