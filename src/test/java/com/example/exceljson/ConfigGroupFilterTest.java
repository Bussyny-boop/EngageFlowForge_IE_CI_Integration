package com.example.exceljson;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for config group filtering functionality that updates inScope checkbox
 * based on filtered rows.
 */
class ConfigGroupFilterTest {

    @TempDir
    File tempDir;

    @Test
    void testInScopeUpdatedByFilter() throws Exception {
        File excelFile = new File(tempDir, "test-config-group-filter.xlsx");
        
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            // Unit Breakdown sheet
            Sheet unitSheet = wb.createSheet("Unit Breakdown");
            Row unitHeader = unitSheet.createRow(2);
            unitHeader.createCell(0).setCellValue("Facility");
            unitHeader.createCell(1).setCellValue("Common Unit Name");
            unitHeader.createCell(2).setCellValue("Nurse Call Configuration Group");
            
            Row unitData = unitSheet.createRow(3);
            unitData.createCell(0).setCellValue("Test Facility");
            unitData.createCell(1).setCellValue("Unit 1");
            unitData.createCell(2).setCellValue("Group A");
            
            // Nurse Call sheet with multiple config groups
            Sheet nurseSheet = wb.createSheet("Nurse Call");
            Row nurseHeader = nurseSheet.createRow(2);
            nurseHeader.createCell(0).setCellValue("In scope");
            nurseHeader.createCell(1).setCellValue("Configuration Group");
            nurseHeader.createCell(2).setCellValue("Common Alert or Alarm Name");
            nurseHeader.createCell(3).setCellValue("Priority");
            nurseHeader.createCell(6).setCellValue("Time to 1st Recipient");
            nurseHeader.createCell(7).setCellValue("1st Recipient");
            
            // Group A alarm
            Row nurseData1 = nurseSheet.createRow(3);
            nurseData1.createCell(0).setCellValue("TRUE");
            nurseData1.createCell(1).setCellValue("Group A");
            nurseData1.createCell(2).setCellValue("Alarm A");
            nurseData1.createCell(3).setCellValue("Normal");
            nurseData1.createCell(6).setCellValue("0");
            nurseData1.createCell(7).setCellValue("Charge Nurse");
            
            // Group B alarm
            Row nurseData2 = nurseSheet.createRow(4);
            nurseData2.createCell(0).setCellValue("TRUE");
            nurseData2.createCell(1).setCellValue("Group B");
            nurseData2.createCell(2).setCellValue("Alarm B");
            nurseData2.createCell(3).setCellValue("Normal");
            nurseData2.createCell(6).setCellValue("0");
            nurseData2.createCell(7).setCellValue("Charge Nurse");
            
            // Another Group A alarm
            Row nurseData3 = nurseSheet.createRow(5);
            nurseData3.createCell(0).setCellValue("TRUE");
            nurseData3.createCell(1).setCellValue("Group A");
            nurseData3.createCell(2).setCellValue("Alarm A2");
            nurseData3.createCell(3).setCellValue("Normal");
            nurseData3.createCell(6).setCellValue("0");
            nurseData3.createCell(7).setCellValue("Charge Nurse");
            
            try (FileOutputStream fos = new FileOutputStream(excelFile)) {
                wb.write(fos);
            }
        }
        
        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(excelFile);
        
        // Initially all flows should have inScope=true
        assertEquals(3, parser.nurseCalls.size());
        assertTrue(parser.nurseCalls.get(0).inScope, "First flow should be in scope initially");
        assertTrue(parser.nurseCalls.get(1).inScope, "Second flow should be in scope initially");
        assertTrue(parser.nurseCalls.get(2).inScope, "Third flow should be in scope initially");
        
        // Simulate filtering by Group A - only Group A flows should have inScope=true
        String filterGroup = "Group A";
        for (ExcelParserV5.FlowRow flow : parser.nurseCalls) {
            flow.inScope = filterGroup.equals(flow.configGroup);
        }
        
        // Verify filtering worked
        assertTrue(parser.nurseCalls.get(0).inScope, "Group A flow should be in scope after filter");
        assertFalse(parser.nurseCalls.get(1).inScope, "Group B flow should NOT be in scope after filter");
        assertTrue(parser.nurseCalls.get(2).inScope, "Group A flow should be in scope after filter");
        
        // Build JSON with filtered data - only flows with inScope=true should be included
        Map<String, Object> nurseJson = parser.buildNurseCallsJson();
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> flows = (List<Map<String, Object>>) nurseJson.get("deliveryFlows");
        
        // Should only have 2 flows (the two Group A alarms)
        assertEquals(2, flows.size(), "Should only generate JSON for in-scope (Group A) flows");
        
        // Verify the correct flows were included
        boolean foundAlarmA = false;
        boolean foundAlarmA2 = false;
        boolean foundAlarmB = false;
        
        for (Map<String, Object> flow : flows) {
            String name = (String) flow.get("name");
            if (name.contains("Alarm A2")) {
                foundAlarmA2 = true;
            } else if (name.contains("Alarm A")) {
                foundAlarmA = true;
            } else if (name.contains("Alarm B")) {
                foundAlarmB = true;
            }
        }
        
        assertTrue(foundAlarmA, "Should include Alarm A in JSON output");
        assertTrue(foundAlarmA2, "Should include Alarm A2 in JSON output");
        assertFalse(foundAlarmB, "Should NOT include Alarm B (Group B) in JSON output");
    }

    @Test
    void testInScopeAllCheckedWhenFilterSetToAll() throws Exception {
        File excelFile = new File(tempDir, "test-config-group-all-filter.xlsx");
        
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            // Unit Breakdown sheet
            Sheet unitSheet = wb.createSheet("Unit Breakdown");
            Row unitHeader = unitSheet.createRow(2);
            unitHeader.createCell(0).setCellValue("Facility");
            unitHeader.createCell(1).setCellValue("Common Unit Name");
            unitHeader.createCell(2).setCellValue("Nurse Call Configuration Group");
            
            Row unitData = unitSheet.createRow(3);
            unitData.createCell(0).setCellValue("Test Facility");
            unitData.createCell(1).setCellValue("Unit 1");
            unitData.createCell(2).setCellValue("Group A");
            
            // Nurse Call sheet with multiple config groups
            Sheet nurseSheet = wb.createSheet("Nurse Call");
            Row nurseHeader = nurseSheet.createRow(2);
            nurseHeader.createCell(0).setCellValue("In scope");
            nurseHeader.createCell(1).setCellValue("Configuration Group");
            nurseHeader.createCell(2).setCellValue("Common Alert or Alarm Name");
            nurseHeader.createCell(3).setCellValue("Priority");
            nurseHeader.createCell(6).setCellValue("Time to 1st Recipient");
            nurseHeader.createCell(7).setCellValue("1st Recipient");
            
            // Group A alarm
            Row nurseData1 = nurseSheet.createRow(3);
            nurseData1.createCell(0).setCellValue("TRUE");
            nurseData1.createCell(1).setCellValue("Group A");
            nurseData1.createCell(2).setCellValue("Alarm A");
            nurseData1.createCell(3).setCellValue("Normal");
            nurseData1.createCell(6).setCellValue("0");
            nurseData1.createCell(7).setCellValue("Charge Nurse");
            
            // Group B alarm
            Row nurseData2 = nurseSheet.createRow(4);
            nurseData2.createCell(0).setCellValue("TRUE");
            nurseData2.createCell(1).setCellValue("Group B");
            nurseData2.createCell(2).setCellValue("Alarm B");
            nurseData2.createCell(3).setCellValue("Normal");
            nurseData2.createCell(6).setCellValue("0");
            nurseData2.createCell(7).setCellValue("Charge Nurse");
            
            // Group C alarm
            Row nurseData3 = nurseSheet.createRow(5);
            nurseData3.createCell(0).setCellValue("TRUE");
            nurseData3.createCell(1).setCellValue("Group C");
            nurseData3.createCell(2).setCellValue("Alarm C");
            nurseData3.createCell(3).setCellValue("Normal");
            nurseData3.createCell(6).setCellValue("0");
            nurseData3.createCell(7).setCellValue("Charge Nurse");
            
            try (FileOutputStream fos = new FileOutputStream(excelFile)) {
                wb.write(fos);
            }
        }
        
        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(excelFile);
        
        // Initially all flows should have inScope=true
        assertEquals(3, parser.nurseCalls.size());
        
        // Simulate filtering by Group A - only Group A flows should have inScope=true
        String filterGroup = "Group A";
        for (ExcelParserV5.FlowRow flow : parser.nurseCalls) {
            flow.inScope = filterGroup.equals(flow.configGroup);
        }
        
        // Verify only Group A is in scope
        assertTrue(parser.nurseCalls.get(0).inScope, "Group A flow should be in scope");
        assertFalse(parser.nurseCalls.get(1).inScope, "Group B flow should NOT be in scope");
        assertFalse(parser.nurseCalls.get(2).inScope, "Group C flow should NOT be in scope");
        
        // Now simulate switching filter to "All" - all rows should be checked
        for (ExcelParserV5.FlowRow flow : parser.nurseCalls) {
            flow.inScope = true;
        }
        
        // Verify ALL flows are now in scope
        assertTrue(parser.nurseCalls.get(0).inScope, "Group A flow should be in scope when 'All' is selected");
        assertTrue(parser.nurseCalls.get(1).inScope, "Group B flow should be in scope when 'All' is selected");
        assertTrue(parser.nurseCalls.get(2).inScope, "Group C flow should be in scope when 'All' is selected");
        
        // Build JSON with all flows - should include all 3 flows
        Map<String, Object> nurseJson = parser.buildNurseCallsJson();
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> flows = (List<Map<String, Object>>) nurseJson.get("deliveryFlows");
        
        // Should have all 3 flows when "All" is selected
        assertEquals(3, flows.size(), "Should generate JSON for all flows when 'All' filter is selected");
    }
}
