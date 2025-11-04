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
 * Test to verify that the VCS checkbox (formerly labeled VMP) correctly generates
 * VMP component interfaces as specified in the requirements.
 */
class VcsCheckboxLabelTest {

    @Test
    void testEdgeCheckboxGeneratesOutgoingWCTP() throws Exception {
        Path tempDir = Files.createTempDirectory("vcs-label-edge-test");
        File excelFile = tempDir.resolve("test.xlsx").toFile();

        createTestWorkbookWithBlankDevices(excelFile);

        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(excelFile);

        // Set Edge checkbox (as user would do in GUI)
        parser.setDefaultInterfaces(true, false);

        var json = parser.buildNurseCallsJson();
        var flows = (List<?>) json.get("deliveryFlows");
        assertEquals(1, flows.size());

        var flow = (Map<?, ?>) flows.get(0);
        var interfaces = (List<?>) flow.get("interfaces");
        
        // Verify Edge checkbox generates OutgoingWCTP interface
        assertEquals(1, interfaces.size(), "Edge checkbox should generate 1 interface");
        var iface = (Map<?, ?>) interfaces.get(0);
        assertEquals("OutgoingWCTP", iface.get("componentName"), 
            "Edge checkbox should generate OutgoingWCTP component");
        assertEquals("OutgoingWCTP", iface.get("referenceName"),
            "Edge checkbox should use OutgoingWCTP reference");

        // Clean up
        Files.deleteIfExists(excelFile.toPath());
        Files.deleteIfExists(tempDir);
    }

    @Test
    void testVcsCheckboxGeneratesVMP() throws Exception {
        Path tempDir = Files.createTempDirectory("vcs-label-vcs-test");
        File excelFile = tempDir.resolve("test.xlsx").toFile();

        createTestWorkbookWithBlankDevices(excelFile);

        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(excelFile);

        // Set VCS checkbox (as user would do in GUI - checkbox is now labeled "VCS")
        parser.setDefaultInterfaces(false, true);

        var json = parser.buildNurseCallsJson();
        var flows = (List<?>) json.get("deliveryFlows");
        assertEquals(1, flows.size());

        var flow = (Map<?, ?>) flows.get(0);
        var interfaces = (List<?>) flow.get("interfaces");
        
        // Verify VCS checkbox generates VMP interface
        assertEquals(1, interfaces.size(), "VCS checkbox should generate 1 interface");
        var iface = (Map<?, ?>) interfaces.get(0);
        assertEquals("VMP", iface.get("componentName"), 
            "VCS checkbox should generate VMP component");
        assertEquals("VMP", iface.get("referenceName"),
            "VCS checkbox should use VMP reference");

        // Clean up
        Files.deleteIfExists(excelFile.toPath());
        Files.deleteIfExists(tempDir);
    }

    @Test
    void testBothCheckboxesGenerateBothInterfaces() throws Exception {
        Path tempDir = Files.createTempDirectory("vcs-label-both-test");
        File excelFile = tempDir.resolve("test.xlsx").toFile();

        createTestWorkbookWithBlankDevices(excelFile);

        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(excelFile);

        // Set both Edge and VCS checkboxes
        parser.setDefaultInterfaces(true, true);

        var json = parser.buildNurseCallsJson();
        var flows = (List<?>) json.get("deliveryFlows");
        assertEquals(1, flows.size());

        var flow = (Map<?, ?>) flows.get(0);
        var interfaces = (List<?>) flow.get("interfaces");
        
        // Verify both checkboxes generate both interfaces
        assertEquals(2, interfaces.size(), "Both checkboxes should generate 2 interfaces");
        
        var iface1 = (Map<?, ?>) interfaces.get(0);
        assertEquals("OutgoingWCTP", iface1.get("componentName"), 
            "First interface should be OutgoingWCTP");
        assertEquals("OutgoingWCTP", iface1.get("referenceName"));
        
        var iface2 = (Map<?, ?>) interfaces.get(1);
        assertEquals("VMP", iface2.get("componentName"), 
            "Second interface should be VMP");
        assertEquals("VMP", iface2.get("referenceName"));

        // Clean up
        Files.deleteIfExists(excelFile.toPath());
        Files.deleteIfExists(tempDir);
    }

    @Test
    void testInterfacesEmptyWhenCheckboxesUnchecked() throws Exception {
        Path tempDir = Files.createTempDirectory("vcs-label-empty-test");
        File excelFile = tempDir.resolve("test.xlsx").toFile();

        createTestWorkbookWithBlankDevices(excelFile);

        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(excelFile);

        // Don't set any checkboxes (default state)
        var json = parser.buildNurseCallsJson();
        var flows = (List<?>) json.get("deliveryFlows");
        assertEquals(1, flows.size());

        var flow = (Map<?, ?>) flows.get(0);
        var interfaces = (List<?>) flow.get("interfaces");
        
        // Verify interfaces are empty when no checkboxes are selected
        assertTrue(interfaces.isEmpty(), 
            "Interfaces should be empty when neither checkbox is selected");

        // Clean up
        Files.deleteIfExists(excelFile.toPath());
        Files.deleteIfExists(tempDir);
    }

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

            Row unitRow = unitSheet.createRow(3);
            unitRow.createCell(0).setCellValue("Test Facility");
            unitRow.createCell(1).setCellValue("ICU");
            unitRow.createCell(2).setCellValue("Nurse Group 1");

            // Nurse Call sheet with blank device field
            Sheet nurseSheet = wb.createSheet("Nurse Call");
            Row nurseHeader = nurseSheet.createRow(2);
            nurseHeader.createCell(0).setCellValue("Configuration Group");
            nurseHeader.createCell(1).setCellValue("Common Alert or Alarm Name");
            nurseHeader.createCell(4).setCellValue("Device - A");
            nurseHeader.createCell(10).setCellValue("Time to 1st Recipient");
            nurseHeader.createCell(11).setCellValue("1st Recipient");

            Row nurseRow = nurseSheet.createRow(3);
            nurseRow.createCell(0).setCellValue("Nurse Group 1");
            nurseRow.createCell(1).setCellValue("Test Alarm");
            nurseRow.createCell(4).setCellValue("");  // Blank device
            nurseRow.createCell(10).setCellValue("0");
            nurseRow.createCell(11).setCellValue("Team");

            // Patient Monitoring sheet (empty)
            Sheet clinicalSheet = wb.createSheet("Patient Monitoring");
            Row clinicalHeader = clinicalSheet.createRow(2);
            clinicalHeader.createCell(0).setCellValue("Configuration Group");

            try (FileOutputStream fos = new FileOutputStream(excelFile)) {
                wb.write(fos);
            }
        }
    }
}
