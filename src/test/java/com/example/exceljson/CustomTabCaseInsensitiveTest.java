package com.example.exceljson;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test that custom tab mappings work case-insensitively.
 */
public class CustomTabCaseInsensitiveTest {

    @TempDir
    Path tempDir;

    @Test
    public void testCustomTabCaseInsensitive() throws Exception {
        // Create a test workbook with a custom tab named "IV Pump"
        File testFile = tempDir.resolve("test_custom_tab.xlsx").toFile();
        
        try (Workbook wb = new XSSFWorkbook()) {
            // Create Unit Breakdown sheet
            Sheet unitSheet = wb.createSheet("Unit Breakdown");
            Row headerRow = unitSheet.createRow(2);
            headerRow.createCell(0).setCellValue("Facility");
            headerRow.createCell(1).setCellValue("Common Unit Name");
            headerRow.createCell(2).setCellValue("Nurse Call Configuration Group");
            headerRow.createCell(3).setCellValue("Patient Monitoring Configuration Group");
            headerRow.createCell(4).setCellValue("Orders Configuration Group");
            
            Row dataRow = unitSheet.createRow(3);
            dataRow.createCell(0).setCellValue("Test Facility");
            dataRow.createCell(1).setCellValue("Test Unit");
            dataRow.createCell(2).setCellValue("NC Group");
            dataRow.createCell(3).setCellValue("Clinical Group");
            dataRow.createCell(4).setCellValue("Orders Group");
            
            // Create a custom tab named "IV Pump" (with specific case)
            Sheet customSheet = wb.createSheet("IV Pump");
            Row customHeader = customSheet.createRow(2);
            customHeader.createCell(0).setCellValue("In Scope");
            customHeader.createCell(1).setCellValue("Configuration Group");
            customHeader.createCell(2).setCellValue("Common Alert or Alarm Name");
            customHeader.createCell(3).setCellValue("Sending System Alert Name");
            customHeader.createCell(4).setCellValue("Priority");
            
            Row customData1 = customSheet.createRow(3);
            customData1.createCell(0).setCellValue("X");
            customData1.createCell(1).setCellValue("NC Group");
            customData1.createCell(2).setCellValue("IV Occlusion");
            customData1.createCell(3).setCellValue("IV Occlusion Alert");
            customData1.createCell(4).setCellValue("High");
            
            Row customData2 = customSheet.createRow(4);
            customData2.createCell(0).setCellValue("X");
            customData2.createCell(1).setCellValue("NC Group");
            customData2.createCell(2).setCellValue("IV Complete");
            customData2.createCell(3).setCellValue("IV Complete Alert");
            customData2.createCell(4).setCellValue("Normal");
            
            try (FileOutputStream fos = new FileOutputStream(testFile)) {
                wb.write(fos);
            }
        }
        
        // Test 1: Exact case match
        ExcelParserV5 parser1 = new ExcelParserV5();
        Map<String, String> mappings1 = Map.of("IV Pump", "NurseCalls");
        parser1.setCustomTabMappings(mappings1);
        parser1.load(testFile);
        
        Map<String, Integer> counts1 = parser1.getCustomTabRowCounts();
        assertTrue(counts1.containsKey("IV Pump"), "Should find 'IV Pump' mapping");
        assertEquals(2, counts1.get("IV Pump"), "Should load 2 rows from IV Pump tab");
        assertEquals(2, parser1.nurseCalls.size(), "Should have 2 nurse call flows");
        
        // Test 2: Lowercase tab name
        ExcelParserV5 parser2 = new ExcelParserV5();
        Map<String, String> mappings2 = Map.of("iv pump", "NurseCalls");
        parser2.setCustomTabMappings(mappings2);
        parser2.load(testFile);
        
        Map<String, Integer> counts2 = parser2.getCustomTabRowCounts();
        assertTrue(counts2.containsKey("iv pump"), "Should find 'iv pump' mapping");
        assertEquals(2, counts2.get("iv pump"), "Should load 2 rows from IV Pump tab (case-insensitive)");
        assertEquals(2, parser2.nurseCalls.size(), "Should have 2 nurse call flows (case-insensitive)");
        
        // Test 3: Uppercase tab name
        ExcelParserV5 parser3 = new ExcelParserV5();
        Map<String, String> mappings3 = Map.of("IV PUMP", "NurseCalls");
        parser3.setCustomTabMappings(mappings3);
        parser3.load(testFile);
        
        Map<String, Integer> counts3 = parser3.getCustomTabRowCounts();
        assertTrue(counts3.containsKey("IV PUMP"), "Should find 'IV PUMP' mapping");
        assertEquals(2, counts3.get("IV PUMP"), "Should load 2 rows from IV Pump tab (case-insensitive)");
        assertEquals(2, parser3.nurseCalls.size(), "Should have 2 nurse call flows (case-insensitive)");
        
        // Test 4: Mixed case tab name
        ExcelParserV5 parser4 = new ExcelParserV5();
        Map<String, String> mappings4 = Map.of("iV pUmP", "NurseCalls");
        parser4.setCustomTabMappings(mappings4);
        parser4.load(testFile);
        
        Map<String, Integer> counts4 = parser4.getCustomTabRowCounts();
        assertTrue(counts4.containsKey("iV pUmP"), "Should find 'iV pUmP' mapping");
        assertEquals(2, counts4.get("iV pUmP"), "Should load 2 rows from IV Pump tab (case-insensitive)");
        assertEquals(2, parser4.nurseCalls.size(), "Should have 2 nurse call flows (case-insensitive)");
    }
    
    @Test
    public void testCustomTabMappingToCliniccals() throws Exception {
        // Create a test workbook with a custom tab
        File testFile = tempDir.resolve("test_custom_clinical.xlsx").toFile();
        
        try (Workbook wb = new XSSFWorkbook()) {
            // Create Unit Breakdown sheet
            Sheet unitSheet = wb.createSheet("Unit Breakdown");
            Row headerRow = unitSheet.createRow(2);
            headerRow.createCell(0).setCellValue("Facility");
            headerRow.createCell(1).setCellValue("Common Unit Name");
            headerRow.createCell(2).setCellValue("Nurse Call Configuration Group");
            headerRow.createCell(3).setCellValue("Patient Monitoring Configuration Group");
            
            Row dataRow = unitSheet.createRow(3);
            dataRow.createCell(0).setCellValue("Test Facility");
            dataRow.createCell(1).setCellValue("Test Unit");
            dataRow.createCell(2).setCellValue("NC Group");
            dataRow.createCell(3).setCellValue("Clinical Group");
            
            // Create a custom tab for patient monitoring
            Sheet customSheet = wb.createSheet("Telemetry Alerts");
            Row customHeader = customSheet.createRow(2);
            customHeader.createCell(0).setCellValue("In Scope");
            customHeader.createCell(1).setCellValue("Configuration Group");
            customHeader.createCell(2).setCellValue("Common Alert or Alarm Name");
            customHeader.createCell(3).setCellValue("Sending System Alert Name");
            customHeader.createCell(4).setCellValue("Priority");
            
            Row customData1 = customSheet.createRow(3);
            customData1.createCell(0).setCellValue("X");
            customData1.createCell(1).setCellValue("Clinical Group");
            customData1.createCell(2).setCellValue("Bradycardia");
            customData1.createCell(3).setCellValue("HR Low Alert");
            customData1.createCell(4).setCellValue("Urgent");
            
            try (FileOutputStream fos = new FileOutputStream(testFile)) {
                wb.write(fos);
            }
        }
        
        // Test mapping custom tab to Clinicals (case-insensitive)
        ExcelParserV5 parser = new ExcelParserV5();
        Map<String, String> mappings = Map.of("telemetry ALERTS", "Clinicals");
        parser.setCustomTabMappings(mappings);
        parser.load(testFile);
        
        Map<String, Integer> counts = parser.getCustomTabRowCounts();
        assertTrue(counts.containsKey("telemetry ALERTS"), "Should find mapping");
        assertEquals(1, counts.get("telemetry ALERTS"), "Should load 1 row from custom tab");
        assertEquals(1, parser.clinicals.size(), "Should have 1 clinical flow");
        
        // Verify the flow was parsed correctly
        ExcelParserV5.FlowRow flow = parser.clinicals.getFirst();
        assertEquals("Bradycardia", flow.alarmName);
        assertEquals("HR Low Alert", flow.sendingName);
        assertEquals("Urgent", flow.priorityRaw);
    }
}
