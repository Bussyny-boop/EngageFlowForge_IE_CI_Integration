package com.example.exceljson;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test for Custom Unit feature to verify the complete JSON structure.
 */
class CustomUnitManualVerificationTest {

    @TempDir
    File tempDir;

    @Test
    void testCustomUnitCompleteJsonStructure() throws Exception {
        File excelFile = new File(tempDir, "test-custom-unit-complete.xlsx");
        
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
            unitData.createCell(2).setCellValue("Nurse Group 1");
            
            // Nurse Call sheet with both normal and urgent priority Custom Unit recipients
            Sheet nurseSheet = wb.createSheet("Nurse Call");
            Row nurseHeader = nurseSheet.createRow(2);
            nurseHeader.createCell(0).setCellValue("In scope");
            nurseHeader.createCell(1).setCellValue("Configuration Group");
            nurseHeader.createCell(2).setCellValue("Common Alert or Alarm Name");
            nurseHeader.createCell(3).setCellValue("Priority");
            nurseHeader.createCell(6).setCellValue("Time to 1st Recipient");
            nurseHeader.createCell(7).setCellValue("1st Recipient");
            
            // Normal priority Custom Unit
            Row nurseData1 = nurseSheet.createRow(3);
            nurseData1.createCell(0).setCellValue("TRUE");
            nurseData1.createCell(1).setCellValue("Nurse Group 1");
            nurseData1.createCell(2).setCellValue("Normal Alarm");
            nurseData1.createCell(3).setCellValue("Normal");
            nurseData1.createCell(6).setCellValue("5");
            nurseData1.createCell(7).setCellValue("Custom Unit Nurse, CNA");
            
            // Urgent priority Custom Unit
            Row nurseData2 = nurseSheet.createRow(4);
            nurseData2.createCell(0).setCellValue("TRUE");
            nurseData2.createCell(1).setCellValue("Nurse Group 1");
            nurseData2.createCell(2).setCellValue("Urgent Alarm");
            nurseData2.createCell(3).setCellValue("Urgent");
            nurseData2.createCell(6).setCellValue("3");
            nurseData2.createCell(7).setCellValue("Custom Unit All Nurse, All CNA, Charge Nurse");
            
            try (FileOutputStream fos = new FileOutputStream(excelFile)) {
                wb.write(fos);
            }
        }
        
        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(excelFile);
        
        Map<String, Object> nurseJson = parser.buildNurseCallsJson();
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> flows = (List<Map<String, Object>>) nurseJson.get("deliveryFlows");
        assertEquals(2, flows.size(), "Should have two flows (normal and urgent)");
        
        // Verify Normal Priority Flow
        Map<String, Object> normalFlow = flows.getFirst();
        assertEquals("normal", normalFlow.get("priority"));
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> normalConditions = (List<Map<String, Object>>) normalFlow.get("conditions");
        assertEquals(1, normalConditions.size(), "Normal flow should have 1 condition");
        
        Map<String, Object> normalCondition = normalConditions.getFirst();
        assertEquals("Custom All Assigned Nurse and CNA", normalCondition.get("name"));
        assertEquals(0, normalCondition.get("destinationOrder"));
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> normalFilters = (List<Map<String, Object>>) normalCondition.get("filters");
        assertEquals(3, normalFilters.size(), "Normal priority should have 3 filters");
        
        // Verify filter structure
        Map<String, Object> roleFilter = normalFilters.getFirst();
        assertEquals("bed.room.unit.rooms.beds.locs.assignments.role.name", roleFilter.get("attributePath"));
        assertEquals("in", roleFilter.get("operator"));
        assertEquals("Nurse, CNA", roleFilter.get("value"));
        
        Map<String, Object> stateFilter = normalFilters.get(1);
        assertEquals("bed.room.unit.rooms.beds.locs.assignments.state", stateFilter.get("attributePath"));
        assertEquals("Active", stateFilter.get("value"));
        
        Map<String, Object> deviceFilter = normalFilters.get(2);
        assertEquals("bed.room.unit.rooms.beds.locs.assignments.usr.devices.status", deviceFilter.get("attributePath"));
        assertEquals("Registered, Disconnected", deviceFilter.get("value"));
        
        // Verify Normal destination
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> normalDestinations = (List<Map<String, Object>>) normalFlow.get("destinations");
        assertEquals(1, normalDestinations.size());
        
        Map<String, Object> normalDest = normalDestinations.getFirst();
        assertEquals("custom", normalDest.get("recipientType"));
        assertEquals("bed.room.unit.rooms.beds.locs.assignments.usr.devices.lines.number", normalDest.get("attributePath"));
        assertEquals(5, normalDest.get("delayTime"), "Should use the delay time from Excel");
        assertEquals("OutgoingWCTP", normalDest.get("interfaceReferenceName"));
        
        // Verify Urgent Priority Flow
        Map<String, Object> urgentFlow = flows.get(1);
        assertEquals("urgent", urgentFlow.get("priority"));
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> urgentConditions = (List<Map<String, Object>>) urgentFlow.get("conditions");
        assertEquals(1, urgentConditions.size(), "Urgent flow should have 1 condition");
        
        Map<String, Object> urgentCondition = urgentConditions.getFirst();
        assertEquals("Custom All Assigned Nurse and CNA and Charge Nurse", urgentCondition.get("name"));
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> urgentFilters = (List<Map<String, Object>>) urgentCondition.get("filters");
        assertEquals(4, urgentFilters.size(), "Urgent priority should have 4 filters (includes presence)");
        
        // Verify presence filter exists
        Map<String, Object> presenceFilter = urgentFilters.get(3);
        assertEquals("bed.room.unit.rooms.beds.locs.assignments.usr.presence_show", presenceFilter.get("attributePath"));
        assertEquals("Chat, Available", presenceFilter.get("value"));
        
        // Verify Urgent destination
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> urgentDestinations = (List<Map<String, Object>>) urgentFlow.get("destinations");
        assertEquals(1, urgentDestinations.size());
        
        Map<String, Object> urgentDest = urgentDestinations.getFirst();
        assertEquals("custom", urgentDest.get("recipientType"));
        assertEquals(3, urgentDest.get("delayTime"), "Should use the delay time from Excel");
        
        // Print JSON for manual verification
        System.out.println("\n=== NORMAL PRIORITY CUSTOM UNIT ===");
        System.out.println("Condition: " + normalCondition);
        System.out.println("Destination: " + normalDest);
        
        System.out.println("\n=== URGENT PRIORITY CUSTOM UNIT ===");
        System.out.println("Condition: " + urgentCondition);
        System.out.println("Destination: " + urgentDest);
    }

    @Test
    void testMixedCustomUnitAndRegularRecipients() throws Exception {
        File excelFile = new File(tempDir, "test-mixed-recipients.xlsx");
        
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
            unitData.createCell(2).setCellValue("Nurse Group 1");
            
            // Nurse Call sheet with Custom Unit in first recipient, regular in second
            Sheet nurseSheet = wb.createSheet("Nurse Call");
            Row nurseHeader = nurseSheet.createRow(2);
            nurseHeader.createCell(0).setCellValue("In scope");
            nurseHeader.createCell(1).setCellValue("Configuration Group");
            nurseHeader.createCell(2).setCellValue("Common Alert or Alarm Name");
            nurseHeader.createCell(3).setCellValue("Priority");
            nurseHeader.createCell(6).setCellValue("Time to 1st Recipient");
            nurseHeader.createCell(7).setCellValue("1st Recipient");
            nurseHeader.createCell(8).setCellValue("Time to 2nd Recipient");
            nurseHeader.createCell(9).setCellValue("2nd Recipient");
            
            Row nurseData = nurseSheet.createRow(3);
            nurseData.createCell(0).setCellValue("TRUE");
            nurseData.createCell(1).setCellValue("Nurse Group 1");
            nurseData.createCell(2).setCellValue("Mixed Alarm");
            nurseData.createCell(3).setCellValue("Normal");
            nurseData.createCell(6).setCellValue("0");
            nurseData.createCell(7).setCellValue("Custom Unit Nurse, CNA");
            nurseData.createCell(8).setCellValue("5");
            nurseData.createCell(9).setCellValue("Room Charge Nurse");
            
            try (FileOutputStream fos = new FileOutputStream(excelFile)) {
                wb.write(fos);
            }
        }
        
        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(excelFile);
        
        Map<String, Object> nurseJson = parser.buildNurseCallsJson();
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> flows = (List<Map<String, Object>>) nurseJson.get("deliveryFlows");
        assertEquals(1, flows.size(), "Should have one flow");
        
        Map<String, Object> flow = flows.getFirst();
        
        // Check that we have 1 condition (for Custom Unit)
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> conditions = (List<Map<String, Object>>) flow.get("conditions");
        assertEquals(1, conditions.size(), "Should have 1 condition for Custom Unit");
        
        // Check that we have 2 destinations (Custom Unit + Regular)
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> destinations = (List<Map<String, Object>>) flow.get("destinations");
        assertEquals(2, destinations.size(), "Should have 2 destinations");
        
        // First destination should be Custom Unit
        Map<String, Object> customDest = destinations.getFirst();
        assertEquals("custom", customDest.get("recipientType"));
        assertEquals(0, customDest.get("order"));
        
        // Second destination should be regular functional role
        Map<String, Object> regularDest = destinations.get(1);
        assertEquals("functional_role", regularDest.get("recipientType"));
        assertEquals(1, regularDest.get("order"));
        
        System.out.println("\n=== MIXED RECIPIENTS TEST ===");
        System.out.println("Conditions: " + conditions);
        System.out.println("Custom Destination: " + customDest);
        System.out.println("Regular Destination: " + regularDest);
    }
}
