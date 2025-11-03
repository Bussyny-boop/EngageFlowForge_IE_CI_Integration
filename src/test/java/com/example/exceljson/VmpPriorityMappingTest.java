package com.example.exceljson;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

/**
 * Tests for VMP (VCS) priority mapping in ExcelParserV5.
 * Validates that VCS devices use VMP-specific priority mapping logic.
 */
class VmpPriorityMappingTest {

    @Test
    void vcsNormalMapsToNormal() throws Exception {
        String json = generateJsonWithVcsPriority("Normal");
        assertTrue(json.contains("\"priority\": \"normal\""), 
            "VCS Normal priority should map to 'normal'");
    }

    @Test
    void vcsNormalVcsMapsToNormal() throws Exception {
        String json = generateJsonWithVcsPriority("Normal (VCS)");
        assertTrue(json.contains("\"priority\": \"normal\""), 
            "VCS Normal (VCS) priority should map to 'normal'");
    }

    @Test
    void vcsHighMapsToHigh() throws Exception {
        String json = generateJsonWithVcsPriority("High");
        assertTrue(json.contains("\"priority\": \"high\""), 
            "VCS High priority should map to 'high'");
    }

    @Test
    void vcsHighVcsMapsToHigh() throws Exception {
        String json = generateJsonWithVcsPriority("High (VCS)");
        assertTrue(json.contains("\"priority\": \"high\""), 
            "VCS High (VCS) priority should map to 'high'");
    }

    @Test
    void vcsUrgentMapsToUrgent() throws Exception {
        String json = generateJsonWithVcsPriority("Urgent");
        assertTrue(json.contains("\"priority\": \"urgent\""), 
            "VCS Urgent priority should map to 'urgent'");
    }

    @Test
    void vcsUrgentVcsMapsToUrgent() throws Exception {
        String json = generateJsonWithVcsPriority("Urgent (VCS)");
        assertTrue(json.contains("\"priority\": \"urgent\""), 
            "VCS Urgent (VCS) priority should map to 'urgent'");
    }

    @Test
    void vcsEmptyPriorityDefaultsToNormal() throws Exception {
        String json = generateJsonWithVcsPriority("");
        assertTrue(json.contains("\"priority\": \"normal\""), 
            "VCS Empty priority should default to 'normal'");
    }

    @Test
    void edgeLowStillMapsToNormal() throws Exception {
        String json = generateJsonWithEdgePriority("Low");
        assertTrue(json.contains("\"priority\": \"normal\""), 
            "Edge Low priority should still map to 'normal'");
    }

    @Test
    void edgeMediumStillMapsToHigh() throws Exception {
        String json = generateJsonWithEdgePriority("Medium");
        assertTrue(json.contains("\"priority\": \"high\""), 
            "Edge Medium priority should still map to 'high'");
    }

    @Test
    void edgeHighStillMapsToUrgent() throws Exception {
        String json = generateJsonWithEdgePriority("High");
        assertTrue(json.contains("\"priority\": \"urgent\""), 
            "Edge High priority should still map to 'urgent'");
    }

    @Test
    void edgeLowEdgeStillMapsToNormal() throws Exception {
        String json = generateJsonWithEdgePriority("Low (Edge)");
        assertTrue(json.contains("\"priority\": \"normal\""), 
            "Edge Low (Edge) priority should still map to 'normal'");
    }

    @Test
    void edgeMediumEdgeStillMapsToHigh() throws Exception {
        String json = generateJsonWithEdgePriority("Medium (Edge)");
        assertTrue(json.contains("\"priority\": \"high\""), 
            "Edge Medium (Edge) priority should still map to 'high'");
    }

    @Test
    void edgeHighEdgeStillMapsToUrgent() throws Exception {
        String json = generateJsonWithEdgePriority("High (Edge)");
        assertTrue(json.contains("\"priority\": \"urgent\""), 
            "Edge High (Edge) priority should still map to 'urgent'");
    }

    @Test
    void mixedDevicesUseDifferentMappings() throws Exception {
        Path tempDir = Files.createTempDirectory("vmp-priority-mixed-test");
        Path excelPath = tempDir.resolve("test.xlsx");

        createWorkbookWithMixedDevices(excelPath);

        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(excelPath.toFile());
        
        String json = ExcelParserV5.pretty(parser.buildNurseCallsJson());
        
        // VCS device with "High" should map to "high"
        assertTrue(json.contains("VCS Alarm"), 
            "Should contain VCS alarm");
        
        // Edge device with "High" should map to "urgent"
        assertTrue(json.contains("Edge Alarm"), 
            "Should contain Edge alarm");
        
        // Check that the JSON contains the VCS flow with high priority
        // The flow name includes the priority: "SEND NURSECALL | HIGH | VCS Alarm"
        assertTrue(json.contains("SEND NURSECALL | HIGH | VCS Alarm"), 
            "VCS flow should have HIGH in the flow name (mapped from High priority on VCS device)");
        
        // Check that the JSON contains the Edge flow with urgent priority
        // The flow name includes the priority: "SEND NURSECALL | URGENT | Edge Alarm"
        assertTrue(json.contains("SEND NURSECALL | URGENT | Edge Alarm"), 
            "Edge flow should have URGENT in the flow name (mapped from High priority on Edge device)");
    }

    private String generateJsonWithVcsPriority(String priority) throws Exception {
        Path tempDir = Files.createTempDirectory("vmp-priority-test");
        Path excelPath = tempDir.resolve("test.xlsx");

        createWorkbookWithPriority(excelPath, priority, "Vocera VCS");

        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(excelPath.toFile());
        
        String json = ExcelParserV5.pretty(parser.buildNurseCallsJson());
        
        return json;
    }

    private String generateJsonWithEdgePriority(String priority) throws Exception {
        Path tempDir = Files.createTempDirectory("edge-priority-test");
        Path excelPath = tempDir.resolve("test.xlsx");

        createWorkbookWithPriority(excelPath, priority, "iPhone-Edge");

        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(excelPath.toFile());
        
        String json = ExcelParserV5.pretty(parser.buildNurseCallsJson());
        
        return json;
    }

    private void createWorkbookWithPriority(Path target, String priority, String deviceName) throws Exception {
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
            nurseRow.createCell(3).setCellValue(priority);
            nurseRow.createCell(4).setCellValue(deviceName);
            nurseRow.createCell(5).setCellValue("Tone 1");
            nurseRow.createCell(10).setCellValue("0");
            nurseRow.createCell(11).setCellValue("Nurse Team");

            try (OutputStream os = Files.newOutputStream(target)) {
                workbook.write(os);
            }
        }
    }

    private void createWorkbookWithMixedDevices(Path target) throws Exception {
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
            
            // VCS device with High priority
            Row vcsRow = nurseCalls.createRow(3);
            vcsRow.createCell(0).setCellValue("TestGroup");
            vcsRow.createCell(1).setCellValue("VCS Alarm");
            vcsRow.createCell(3).setCellValue("High");
            vcsRow.createCell(4).setCellValue("Vocera VCS");
            vcsRow.createCell(5).setCellValue("Tone 1");
            vcsRow.createCell(10).setCellValue("0");
            vcsRow.createCell(11).setCellValue("Nurse Team");

            // Edge device with High priority
            Row edgeRow = nurseCalls.createRow(4);
            edgeRow.createCell(0).setCellValue("TestGroup");
            edgeRow.createCell(1).setCellValue("Edge Alarm");
            edgeRow.createCell(3).setCellValue("High");
            edgeRow.createCell(4).setCellValue("iPhone-Edge");
            edgeRow.createCell(5).setCellValue("Tone 1");
            edgeRow.createCell(10).setCellValue("0");
            edgeRow.createCell(11).setCellValue("Nurse Team");

            try (OutputStream os = Files.newOutputStream(target)) {
                workbook.write(os);
            }
        }
    }
}
