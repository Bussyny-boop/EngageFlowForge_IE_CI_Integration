package com.example.exceljson;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

/**
 * Tests for custom tab mapping feature that allows users to map custom Excel tabs
 * to standard flow types (NurseCalls, Clinicals, Orders).
 */
class CustomTabMappingTest {

    @Test
    void testCustomTabMappingForClinicals() throws Exception {
        Path tempDir = Files.createTempDirectory("custom-tab-test");
        Path excelPath = tempDir.resolve("input.xlsx");

        // Create a workbook with standard tabs plus a custom "IV Pump" tab
        createWorkbookWithCustomTab(excelPath, "IV Pump");

        ExcelParserV5 parser = new ExcelParserV5();
        
        // Set custom tab mapping: "IV Pump" -> "Clinicals"
        Map<String, String> mappings = new LinkedHashMap<>();
        mappings.put("IV Pump", "Clinicals");
        parser.setCustomTabMappings(mappings);
        
        // Load the workbook
        parser.load(excelPath.toFile());

        // Verify that flows from both "Patient Monitoring" and "IV Pump" are loaded
        // We should have 2 clinical flows (1 from Patient Monitoring + 1 from IV Pump)
        assertEquals(2, parser.clinicals.size(), 
            "Should have loaded flows from both Patient Monitoring and IV Pump tabs");
        
        // Check that the IV Pump alarm is present
        boolean hasIVPumpAlarm = parser.clinicals.stream()
            .anyMatch(f -> "IV Pump Alert".equals(f.alarmName));
        assertTrue(hasIVPumpAlarm, "Should have loaded 'IV Pump Alert' from custom tab");
        
        // Check that the standard clinical alarm is also present
        boolean hasClinicalAlarm = parser.clinicals.stream()
            .anyMatch(f -> "Clinical Alert".equals(f.alarmName));
        assertTrue(hasClinicalAlarm, "Should have loaded 'Clinical Alert' from Patient Monitoring tab");
    }

    @Test
    void testCustomTabMappingForNurseCalls() throws Exception {
        Path tempDir = Files.createTempDirectory("custom-tab-test");
        Path excelPath = tempDir.resolve("input.xlsx");

        // Create a workbook with a custom "Emergency Calls" tab
        createWorkbookWithCustomTab(excelPath, "Emergency Calls");

        ExcelParserV5 parser = new ExcelParserV5();
        
        // Set custom tab mapping: "Emergency Calls" -> "NurseCalls"
        Map<String, String> mappings = new LinkedHashMap<>();
        mappings.put("Emergency Calls", "NurseCalls");
        parser.setCustomTabMappings(mappings);
        
        parser.load(excelPath.toFile());

        // Verify flows from both standard and custom tabs
        assertEquals(2, parser.nurseCalls.size(),
            "Should have loaded flows from both Nurse Call and Emergency Calls tabs");
    }

    @Test
    void testCustomTabMappingForOrders() throws Exception {
        Path tempDir = Files.createTempDirectory("custom-tab-test");
        Path excelPath = tempDir.resolve("input.xlsx");

        // Create a workbook with a custom "Lab Orders" tab
        createWorkbookWithCustomTab(excelPath, "Lab Orders");

        ExcelParserV5 parser = new ExcelParserV5();
        
        // Set custom tab mapping: "Lab Orders" -> "Orders"
        Map<String, String> mappings = new LinkedHashMap<>();
        mappings.put("Lab Orders", "Orders");
        parser.setCustomTabMappings(mappings);
        
        parser.load(excelPath.toFile());

        // Verify flows from both standard and custom tabs
        assertEquals(2, parser.orders.size(),
            "Should have loaded flows from both Order and Lab Orders tabs");
    }

    @Test
    void testMultipleCustomTabMappings() throws Exception {
        Path tempDir = Files.createTempDirectory("custom-tab-test");
        Path excelPath = tempDir.resolve("input.xlsx");

        // Create a workbook with multiple custom tabs
        createWorkbookWithMultipleCustomTabs(excelPath);

        ExcelParserV5 parser = new ExcelParserV5();
        
        // Set multiple custom tab mappings
        Map<String, String> mappings = new LinkedHashMap<>();
        mappings.put("IV Pump", "Clinicals");
        mappings.put("Telemetry", "Clinicals");
        mappings.put("Emergency Calls", "NurseCalls");
        parser.setCustomTabMappings(mappings);
        
        parser.load(excelPath.toFile());

        // Verify all flows are loaded correctly
        // Clinicals: 1 from Patient Monitoring + 1 from IV Pump + 1 from Telemetry = 3
        assertEquals(3, parser.clinicals.size(),
            "Should have loaded flows from Patient Monitoring, IV Pump, and Telemetry");
        
        // NurseCalls: 1 from Nurse Call + 1 from Emergency Calls = 2
        assertEquals(2, parser.nurseCalls.size(),
            "Should have loaded flows from Nurse Call and Emergency Calls");
    }

    @Test
    void testCustomTabMappingNormalization() throws Exception {
        ExcelParserV5 parser = new ExcelParserV5();
        
        // Test various input formats that should all normalize correctly
        Map<String, String> mappings = new LinkedHashMap<>();
        mappings.put("Tab1", "nursecall");  // lowercase
        mappings.put("Tab2", "CLINICALS");  // uppercase
        mappings.put("Tab3", "Orders");     // proper case
        mappings.put("Tab4", "Nurse Call"); // with space
        mappings.put("Tab5", "Clinical");   // singular
        
        parser.setCustomTabMappings(mappings);
        
        Map<String, String> result = parser.getCustomTabMappings();
        
        // Verify normalization
        assertEquals("NurseCalls", result.get("Tab1"));
        assertEquals("Clinicals", result.get("Tab2"));
        assertEquals("Orders", result.get("Tab3"));
        assertEquals("NurseCalls", result.get("Tab4"));
        assertEquals("Clinicals", result.get("Tab5"));
    }

    @Test
    void testCustomTabNotInWorkbook() throws Exception {
        Path tempDir = Files.createTempDirectory("custom-tab-test");
        Path excelPath = tempDir.resolve("input.xlsx");

        // Create a standard workbook without custom tabs
        createWorkbookWithCustomTab(excelPath, "IV Pump");

        ExcelParserV5 parser = new ExcelParserV5();
        
        // Map a tab that doesn't exist in the workbook
        Map<String, String> mappings = new LinkedHashMap<>();
        mappings.put("NonExistent Tab", "Clinicals");
        mappings.put("IV Pump", "Clinicals");
        parser.setCustomTabMappings(mappings);
        
        // Should not throw an error, just skip the non-existent tab
        assertDoesNotThrow(() -> parser.load(excelPath.toFile()));
        
        // Should still load the existing custom tab
        assertEquals(2, parser.clinicals.size(),
            "Should have loaded flows from existing tabs");
    }

    // ---------- Helper Methods ----------

    /**
     * Creates a workbook with standard tabs plus one custom tab.
     */
    private void createWorkbookWithCustomTab(Path excelPath, String customTabName) throws Exception {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            // Create Unit Breakdown sheet
            Sheet unitSheet = wb.createSheet("Unit Breakdown");
            Row unitHeader = unitSheet.createRow(0);
            unitHeader.createCell(0).setCellValue("Facility");
            unitHeader.createCell(1).setCellValue("Common Unit Name");
            unitHeader.createCell(2).setCellValue("Nurse Call Configuration Group");
            unitHeader.createCell(3).setCellValue("Patient Monitoring Configuration Group");
            unitHeader.createCell(4).setCellValue("Order Configuration Group");
            
            Row unitData = unitSheet.createRow(1);
            unitData.createCell(0).setCellValue("Test Facility");
            unitData.createCell(1).setCellValue("Unit 1");
            unitData.createCell(2).setCellValue("Nurse Group 1");
            unitData.createCell(3).setCellValue("Clinical Group 1");
            unitData.createCell(4).setCellValue("Orders Group 1");

            // Create Nurse Call sheet
            Sheet nurseSheet = wb.createSheet("Nurse Call");
            Row nurseHeader = nurseSheet.createRow(0);
            nurseHeader.createCell(0).setCellValue("Configuration Group");
            nurseHeader.createCell(1).setCellValue("Common Alert or Alarm Name");
            nurseHeader.createCell(2).setCellValue("Sending System Alert Name");
            nurseHeader.createCell(3).setCellValue("Priority");
            
            Row nurseData = nurseSheet.createRow(1);
            nurseData.createCell(0).setCellValue("Nurse Group 1");
            nurseData.createCell(1).setCellValue("Nurse Alert");
            nurseData.createCell(2).setCellValue("Nurse Call System");
            nurseData.createCell(3).setCellValue("Normal");

            // Create Patient Monitoring sheet
            Sheet clinicalSheet = wb.createSheet("Patient Monitoring");
            Row clinicalHeader = clinicalSheet.createRow(0);
            clinicalHeader.createCell(0).setCellValue("Configuration Group");
            clinicalHeader.createCell(1).setCellValue("Common Alert or Alarm Name");
            clinicalHeader.createCell(2).setCellValue("Sending System Alert Name");
            clinicalHeader.createCell(3).setCellValue("Priority");
            
            Row clinicalData = clinicalSheet.createRow(1);
            clinicalData.createCell(0).setCellValue("Clinical Group 1");
            clinicalData.createCell(1).setCellValue("Clinical Alert");
            clinicalData.createCell(2).setCellValue("Monitor System");
            clinicalData.createCell(3).setCellValue("Normal");

            // Create Order sheet
            Sheet orderSheet = wb.createSheet("Order");
            Row orderHeader = orderSheet.createRow(0);
            orderHeader.createCell(0).setCellValue("Configuration Group");
            orderHeader.createCell(1).setCellValue("Common Alert or Alarm Name");
            orderHeader.createCell(2).setCellValue("Sending System Alert Name");
            orderHeader.createCell(3).setCellValue("Priority");
            
            Row orderData = orderSheet.createRow(1);
            orderData.createCell(0).setCellValue("Orders Group 1");
            orderData.createCell(1).setCellValue("Order Alert");
            orderData.createCell(2).setCellValue("Order System");
            orderData.createCell(3).setCellValue("Normal");

            // Create custom tab
            Sheet customSheet = wb.createSheet(customTabName);
            Row customHeader = customSheet.createRow(0);
            customHeader.createCell(0).setCellValue("Configuration Group");
            customHeader.createCell(1).setCellValue("Common Alert or Alarm Name");
            customHeader.createCell(2).setCellValue("Sending System Alert Name");
            customHeader.createCell(3).setCellValue("Priority");
            
            Row customData = customSheet.createRow(1);
            customData.createCell(0).setCellValue("Clinical Group 1");
            customData.createCell(1).setCellValue(customTabName + " Alert");
            customData.createCell(2).setCellValue(customTabName + " System");
            customData.createCell(3).setCellValue("Normal");

            try (FileOutputStream fos = new FileOutputStream(excelPath.toFile())) {
                wb.write(fos);
            }
        }
    }

    /**
     * Creates a workbook with multiple custom tabs.
     */
    private void createWorkbookWithMultipleCustomTabs(Path excelPath) throws Exception {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            // Create Unit Breakdown sheet
            Sheet unitSheet = wb.createSheet("Unit Breakdown");
            Row unitHeader = unitSheet.createRow(0);
            unitHeader.createCell(0).setCellValue("Facility");
            unitHeader.createCell(1).setCellValue("Common Unit Name");
            unitHeader.createCell(2).setCellValue("Nurse Call Configuration Group");
            unitHeader.createCell(3).setCellValue("Patient Monitoring Configuration Group");
            unitHeader.createCell(4).setCellValue("Order Configuration Group");
            
            Row unitData = unitSheet.createRow(1);
            unitData.createCell(0).setCellValue("Test Facility");
            unitData.createCell(1).setCellValue("Unit 1");
            unitData.createCell(2).setCellValue("Nurse Group 1");
            unitData.createCell(3).setCellValue("Clinical Group 1");
            unitData.createCell(4).setCellValue("Orders Group 1");

            // Create standard sheets
            createStandardSheet(wb, "Nurse Call", "Nurse Group 1", "Nurse Alert");
            createStandardSheet(wb, "Patient Monitoring", "Clinical Group 1", "Clinical Alert");
            createStandardSheet(wb, "Order", "Orders Group 1", "Order Alert");

            // Create custom tabs
            createStandardSheet(wb, "IV Pump", "Clinical Group 1", "IV Pump Alert");
            createStandardSheet(wb, "Telemetry", "Clinical Group 1", "Telemetry Alert");
            createStandardSheet(wb, "Emergency Calls", "Nurse Group 1", "Emergency Alert");

            try (FileOutputStream fos = new FileOutputStream(excelPath.toFile())) {
                wb.write(fos);
            }
        }
    }

    private void createStandardSheet(XSSFWorkbook wb, String sheetName, String configGroup, String alarmName) {
        Sheet sheet = wb.createSheet(sheetName);
        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("Configuration Group");
        header.createCell(1).setCellValue("Common Alert or Alarm Name");
        header.createCell(2).setCellValue("Sending System Alert Name");
        header.createCell(3).setCellValue("Priority");
        
        Row data = sheet.createRow(1);
        data.createCell(0).setCellValue(configGroup);
        data.createCell(1).setCellValue(alarmName);
        data.createCell(2).setCellValue(sheetName + " System");
        data.createCell(3).setCellValue("Normal");
    }
}
