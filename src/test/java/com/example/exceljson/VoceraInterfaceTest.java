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
 * Tests for the Vocera adapter interface detection and priority mapping.
 * 
 * Requirements:
 * - Device containing "Vocera" or "VMI" should use Vocera interface
 * - Device containing "Vocera VCS" or "VoceraVCS" should use VMP interface (not Vocera)
 * - Vocera interface should use VMP priority mapping (Normal->normal, High->high, Urgent->urgent)
 */
class VoceraInterfaceTest {

    @Test
    void voceraDeviceUsesVoceraInterface() throws Exception {
        Path tempDir = Files.createTempDirectory("vocera-interface-test");
        File excelFile = tempDir.resolve("test.xlsx").toFile();

        createTestWorkbookWithDevice(excelFile, "Vocera");

        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(excelFile);

        var nurseJson = parser.buildNurseCallsJson();
        var flows = (List<?>) nurseJson.get("deliveryFlows");
        
        assertEquals(1, flows.size(), "Should have 1 nurse call flow");

        var flow = (Map<?, ?>) flows.getFirst();
        var interfaces = (List<?>) flow.get("interfaces");
        assertEquals(1, interfaces.size(), "Vocera device should have 1 interface");
        
        var iface = (Map<?, ?>) interfaces.getFirst();
        assertEquals("Vocera", iface.get("componentName"), "Device 'Vocera' should use Vocera component");
        assertEquals("Vocera", iface.get("referenceName"), "Vocera should use default Vocera reference");

        Files.deleteIfExists(excelFile.toPath());
        Files.deleteIfExists(tempDir);
    }

    @Test
    void vmiDeviceUsesVoceraInterface() throws Exception {
        Path tempDir = Files.createTempDirectory("vmi-interface-test");
        File excelFile = tempDir.resolve("test.xlsx").toFile();

        createTestWorkbookWithDevice(excelFile, "VMI");

        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(excelFile);

        var nurseJson = parser.buildNurseCallsJson();
        var flows = (List<?>) nurseJson.get("deliveryFlows");
        
        var flow = (Map<?, ?>) flows.getFirst();
        var interfaces = (List<?>) flow.get("interfaces");
        assertEquals(1, interfaces.size(), "VMI device should have 1 interface");
        
        var iface = (Map<?, ?>) interfaces.getFirst();
        assertEquals("Vocera", iface.get("componentName"), "Device 'VMI' should use Vocera component");

        Files.deleteIfExists(excelFile.toPath());
        Files.deleteIfExists(tempDir);
    }

    @Test
    void voceraVcsUsesVmpInterface() throws Exception {
        Path tempDir = Files.createTempDirectory("vocera-vcs-test");
        File excelFile = tempDir.resolve("test.xlsx").toFile();

        createTestWorkbookWithDevice(excelFile, "Vocera VCS");

        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(excelFile);

        var nurseJson = parser.buildNurseCallsJson();
        var flows = (List<?>) nurseJson.get("deliveryFlows");
        
        var flow = (Map<?, ?>) flows.getFirst();
        var interfaces = (List<?>) flow.get("interfaces");
        assertEquals(1, interfaces.size(), "Vocera VCS device should have 1 interface");
        
        var iface = (Map<?, ?>) interfaces.getFirst();
        assertEquals("VMP", iface.get("componentName"), "Device 'Vocera VCS' should use VMP component (not Vocera)");
        assertEquals("VMP", iface.get("referenceName"), "Vocera VCS should use VMP reference");

        Files.deleteIfExists(excelFile.toPath());
        Files.deleteIfExists(tempDir);
    }

    @Test
    void voceraVcsNoSpaceUsesVmpInterface() throws Exception {
        Path tempDir = Files.createTempDirectory("voceravcs-test");
        File excelFile = tempDir.resolve("test.xlsx").toFile();

        createTestWorkbookWithDevice(excelFile, "VoceraVCS");

        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(excelFile);

        var nurseJson = parser.buildNurseCallsJson();
        var flows = (List<?>) nurseJson.get("deliveryFlows");
        
        var flow = (Map<?, ?>) flows.getFirst();
        var interfaces = (List<?>) flow.get("interfaces");
        
        var iface = (Map<?, ?>) interfaces.getFirst();
        assertEquals("VMP", iface.get("componentName"), "Device 'VoceraVCS' should use VMP component (not Vocera)");

        Files.deleteIfExists(excelFile.toPath());
        Files.deleteIfExists(tempDir);
    }

    @Test
    void voceraDeviceUsesVmpPriorityMapping() throws Exception {
        Path tempDir = Files.createTempDirectory("vocera-priority-test");
        File excelFile = tempDir.resolve("test.xlsx").toFile();

        createTestWorkbookWithDeviceAndPriority(excelFile, "Vocera", "High");

        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(excelFile);

        String json = ExcelParserV5.pretty(parser.buildNurseCallsJson());
        
        // Vocera should use VMP priority mapping: High -> high (not urgent like Edge)
        assertTrue(json.contains("\"priority\": \"high\""), 
            "Vocera device with 'High' priority should map to 'high' using VMP mapping");

        Files.deleteIfExists(excelFile.toPath());
        Files.deleteIfExists(tempDir);
    }

    @Test
    void voceraDeviceNormalMapsToNormal() throws Exception {
        Path tempDir = Files.createTempDirectory("vocera-normal-test");
        File excelFile = tempDir.resolve("test.xlsx").toFile();

        createTestWorkbookWithDeviceAndPriority(excelFile, "VMI", "Normal");

        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(excelFile);

        String json = ExcelParserV5.pretty(parser.buildNurseCallsJson());
        
        assertTrue(json.contains("\"priority\": \"normal\""), 
            "Vocera device with 'Normal' priority should map to 'normal'");

        Files.deleteIfExists(excelFile.toPath());
        Files.deleteIfExists(tempDir);
    }

    @Test
    void voceraDeviceUrgentMapsToUrgent() throws Exception {
        Path tempDir = Files.createTempDirectory("vocera-urgent-test");
        File excelFile = tempDir.resolve("test.xlsx").toFile();

        createTestWorkbookWithDeviceAndPriority(excelFile, "Vocera", "Urgent");

        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(excelFile);

        String json = ExcelParserV5.pretty(parser.buildNurseCallsJson());
        
        assertTrue(json.contains("\"priority\": \"urgent\""), 
            "Vocera device with 'Urgent' priority should map to 'urgent'");

        Files.deleteIfExists(excelFile.toPath());
        Files.deleteIfExists(tempDir);
    }

    @Test
    void customVoceraReferenceName() throws Exception {
        Path tempDir = Files.createTempDirectory("vocera-custom-ref-test");
        File excelFile = tempDir.resolve("test.xlsx").toFile();

        createTestWorkbookWithDevice(excelFile, "Vocera");

        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(excelFile);
        parser.setInterfaceReferences("CustomEdge", "CustomVMP", "CustomVocera");

        var nurseJson = parser.buildNurseCallsJson();
        var flows = (List<?>) nurseJson.get("deliveryFlows");
        var flow = (Map<?, ?>) flows.getFirst();
        var interfaces = (List<?>) flow.get("interfaces");
        var iface = (Map<?, ?>) interfaces.getFirst();
        
        assertEquals("Vocera", iface.get("componentName"), "Component name should stay 'Vocera'");
        assertEquals("CustomVocera", iface.get("referenceName"), "Should use custom reference name");

        Files.deleteIfExists(excelFile.toPath());
        Files.deleteIfExists(tempDir);
    }

    @Test
    void caseInsensitiveVoceraDetection() throws Exception {
        Path tempDir = Files.createTempDirectory("vocera-case-test");
        File excelFile = tempDir.resolve("test.xlsx").toFile();

        createTestWorkbookWithDevice(excelFile, "VOCERA");

        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(excelFile);

        var nurseJson = parser.buildNurseCallsJson();
        var flows = (List<?>) nurseJson.get("deliveryFlows");
        var flow = (Map<?, ?>) flows.getFirst();
        var interfaces = (List<?>) flow.get("interfaces");
        var iface = (Map<?, ?>) interfaces.getFirst();
        
        assertEquals("Vocera", iface.get("componentName"), "Uppercase 'VOCERA' should be detected");

        Files.deleteIfExists(excelFile.toPath());
        Files.deleteIfExists(tempDir);
    }

    @Test
    void deviceBWithVoceraUsesVoceraInterface() throws Exception {
        Path tempDir = Files.createTempDirectory("vocera-deviceb-test");
        File excelFile = tempDir.resolve("test.xlsx").toFile();

        createTestWorkbookWithDeviceB(excelFile, "VMI");

        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(excelFile);

        var nurseJson = parser.buildNurseCallsJson();
        var flows = (List<?>) nurseJson.get("deliveryFlows");
        var flow = (Map<?, ?>) flows.getFirst();
        var interfaces = (List<?>) flow.get("interfaces");
        var iface = (Map<?, ?>) interfaces.getFirst();
        
        assertEquals("Vocera", iface.get("componentName"), "Device-B with 'VMI' should use Vocera component");

        Files.deleteIfExists(excelFile.toPath());
        Files.deleteIfExists(tempDir);
    }

    @Test
    void multipleInterfacesWithEdgeAndVocera() throws Exception {
        Path tempDir = Files.createTempDirectory("edge-vocera-test");
        File excelFile = tempDir.resolve("test.xlsx").toFile();

        createTestWorkbookWithBothDevices(excelFile, "iPhone-Edge", "Vocera");

        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(excelFile);

        var nurseJson = parser.buildNurseCallsJson();
        var flows = (List<?>) nurseJson.get("deliveryFlows");
        var flow = (Map<?, ?>) flows.getFirst();
        var interfaces = (List<?>) flow.get("interfaces");
        
        assertEquals(2, interfaces.size(), "Should have both Edge and Vocera interfaces");
        
        var iface1 = (Map<?, ?>) interfaces.getFirst();
        var iface2 = (Map<?, ?>) interfaces.get(1);
        
        // Order: Edge first, then Vocera
        assertEquals("OutgoingWCTP", iface1.get("componentName"), "First interface should be Edge");
        assertEquals("Vocera", iface2.get("componentName"), "Second interface should be Vocera");

        Files.deleteIfExists(excelFile.toPath());
        Files.deleteIfExists(tempDir);
    }

    @Test
    void defaultVoceraCheckbox() throws Exception {
        Path tempDir = Files.createTempDirectory("default-vocera-test");
        File excelFile = tempDir.resolve("test.xlsx").toFile();

        // Create workbook with blank device
        createTestWorkbookWithDevice(excelFile, "");

        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(excelFile);
        
        // Enable default Vocera checkbox
        parser.setDefaultInterfaces(false, false, true);

        var nurseJson = parser.buildNurseCallsJson();
        var flows = (List<?>) nurseJson.get("deliveryFlows");
        var flow = (Map<?, ?>) flows.getFirst();
        var interfaces = (List<?>) flow.get("interfaces");
        
        assertEquals(1, interfaces.size(), "Should have default Vocera interface");
        
        var iface = (Map<?, ?>) interfaces.getFirst();
        assertEquals("Vocera", iface.get("componentName"), "Default interface should be Vocera");

        Files.deleteIfExists(excelFile.toPath());
        Files.deleteIfExists(tempDir);
    }

    // Helper methods to create test workbooks

    private void createTestWorkbookWithDevice(File excelFile, String deviceName) throws Exception {
        createTestWorkbookWithDeviceAndPriority(excelFile, deviceName, "Normal");
    }

    private void createTestWorkbookWithDeviceAndPriority(File excelFile, String deviceName, String priority) throws Exception {
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
            nurseRow.createCell(3).setCellValue(priority);
            nurseRow.createCell(4).setCellValue(deviceName);
            nurseRow.createCell(5).setCellValue("Ringtone1");
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

    private void createTestWorkbookWithDeviceB(File excelFile, String deviceBName) throws Exception {
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
            nurseHeader.createCell(5).setCellValue("Device - B");
            nurseHeader.createCell(10).setCellValue("Time to 1st Recipient");
            nurseHeader.createCell(11).setCellValue("1st Recipient");

            Row nurseRow = nurseSheet.createRow(3);
            nurseRow.createCell(0).setCellValue("TestGroup");
            nurseRow.createCell(1).setCellValue("Test Alarm");
            nurseRow.createCell(3).setCellValue("Normal");
            nurseRow.createCell(4).setCellValue(""); // Blank Device-A
            nurseRow.createCell(5).setCellValue(deviceBName); // Device-B
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

    private void createTestWorkbookWithBothDevices(File excelFile, String deviceA, String deviceB) throws Exception {
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
            nurseHeader.createCell(5).setCellValue("Device - B");
            nurseHeader.createCell(10).setCellValue("Time to 1st Recipient");
            nurseHeader.createCell(11).setCellValue("1st Recipient");

            Row nurseRow = nurseSheet.createRow(3);
            nurseRow.createCell(0).setCellValue("TestGroup");
            nurseRow.createCell(1).setCellValue("Test Alarm");
            nurseRow.createCell(3).setCellValue("Normal");
            nurseRow.createCell(4).setCellValue(deviceA);
            nurseRow.createCell(5).setCellValue(deviceB);
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
