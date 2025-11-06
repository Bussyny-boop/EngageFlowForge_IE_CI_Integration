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
 * Tests for Custom Unit recipient parsing and JSON generation.
 * Tests various patterns like "Custom Unit Nurse, CNA" and "Custom UNIT all Nurse, CNA"
 */
class CustomUnitTest {

    @TempDir
    File tempDir;

    @Test
    void testCustomUnitBasic() throws Exception {
        File excelFile = new File(tempDir, "test-custom-unit-basic.xlsx");
        
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
            
            // Nurse Call sheet with Custom Unit pattern
            Sheet nurseSheet = wb.createSheet("Nurse Call");
            Row nurseHeader = nurseSheet.createRow(2);
            nurseHeader.createCell(0).setCellValue("In scope");
            nurseHeader.createCell(1).setCellValue("Configuration Group");
            nurseHeader.createCell(2).setCellValue("Common Alert or Alarm Name");
            nurseHeader.createCell(3).setCellValue("Priority");
            nurseHeader.createCell(6).setCellValue("Time to 1st Recipient");
            nurseHeader.createCell(7).setCellValue("1st Recipient");
            
            Row nurseData = nurseSheet.createRow(3);
            nurseData.createCell(0).setCellValue("TRUE");
            nurseData.createCell(1).setCellValue("Nurse Group 1");
            nurseData.createCell(2).setCellValue("Test Alarm");
            nurseData.createCell(3).setCellValue("Normal");
            nurseData.createCell(6).setCellValue("0");
            nurseData.createCell(7).setCellValue("Custom Unit Nurse, CNA");
            
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
        
        Map<String, Object> flow = flows.get(0);
        
        // Check conditions
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> conditions = (List<Map<String, Object>>) flow.get("conditions");
        assertNotNull(conditions, "Should have conditions");
        assertEquals(1, conditions.size(), "Should have one condition");
        
        Map<String, Object> condition = conditions.get(0);
        assertEquals("Custom All Assigned Nurse and CNA", condition.get("name"));
        assertEquals(0, condition.get("destinationOrder"));
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> filters = (List<Map<String, Object>>) condition.get("filters");
        assertEquals(3, filters.size(), "Normal priority should have 3 filters");
        
        // Check filter 1: role name
        assertEquals("bed.room.unit.rooms.beds.locs.assignments.role.name", filters.get(0).get("attributePath"));
        assertEquals("in", filters.get(0).get("operator"));
        assertEquals("Nurse, CNA", filters.get(0).get("value"));
        
        // Check filter 2: state
        assertEquals("bed.room.unit.rooms.beds.locs.assignments.state", filters.get(1).get("attributePath"));
        assertEquals("in", filters.get(1).get("operator"));
        assertEquals("Active", filters.get(1).get("value"));
        
        // Check filter 3: device status
        assertEquals("bed.room.unit.rooms.beds.locs.assignments.usr.devices.status", filters.get(2).get("attributePath"));
        assertEquals("in", filters.get(2).get("operator"));
        assertEquals("Registered, Disconnected", filters.get(2).get("value"));
        
        // Check destinations
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> destinations = (List<Map<String, Object>>) flow.get("destinations");
        assertEquals(1, destinations.size(), "Should have one destination");
        
        Map<String, Object> dest = destinations.get(0);
        assertEquals("custom", dest.get("recipientType"));
        assertEquals("bed.room.unit.rooms.beds.locs.assignments.usr.devices.lines.number", dest.get("attributePath"));
        assertEquals(0, dest.get("delayTime"));
        assertEquals("OutgoingWCTP", dest.get("interfaceReferenceName"));
        assertEquals("none", dest.get("presenceConfig"), "Custom Unit should have presenceConfig set to none");
    }

    @Test
    void testCustomUnitWithAllKeyword() throws Exception {
        File excelFile = new File(tempDir, "test-custom-unit-all.xlsx");
        
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
            
            // Nurse Call sheet with "Custom UNIT all" pattern
            Sheet nurseSheet = wb.createSheet("Nurse Call");
            Row nurseHeader = nurseSheet.createRow(2);
            nurseHeader.createCell(0).setCellValue("In scope");
            nurseHeader.createCell(1).setCellValue("Configuration Group");
            nurseHeader.createCell(2).setCellValue("Common Alert or Alarm Name");
            nurseHeader.createCell(3).setCellValue("Priority");
            nurseHeader.createCell(6).setCellValue("Time to 1st Recipient");
            nurseHeader.createCell(7).setCellValue("1st Recipient");
            
            Row nurseData = nurseSheet.createRow(3);
            nurseData.createCell(0).setCellValue("TRUE");
            nurseData.createCell(1).setCellValue("Nurse Group 1");
            nurseData.createCell(2).setCellValue("Test Alarm");
            nurseData.createCell(3).setCellValue("Normal");
            nurseData.createCell(6).setCellValue("0");
            nurseData.createCell(7).setCellValue("Custom UNIT all Nurse, CNA");
            
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
        
        Map<String, Object> flow = flows.get(0);
        
        // Check conditions
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> conditions = (List<Map<String, Object>>) flow.get("conditions");
        Map<String, Object> condition = conditions.get(0);
        assertEquals("Custom All Assigned Nurse and CNA", condition.get("name"));
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> filters = (List<Map<String, Object>>) condition.get("filters");
        
        // Check that "all" was ignored and only roles remain
        assertEquals("Nurse, CNA", filters.get(0).get("value"));
    }

    @Test
    void testCustomUnitWithAllPrefixedRoles() throws Exception {
        File excelFile = new File(tempDir, "test-custom-unit-all-prefix.xlsx");
        
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
            
            // Nurse Call sheet with "All" prefixed to each role
            Sheet nurseSheet = wb.createSheet("Nurse Call");
            Row nurseHeader = nurseSheet.createRow(2);
            nurseHeader.createCell(0).setCellValue("In scope");
            nurseHeader.createCell(1).setCellValue("Configuration Group");
            nurseHeader.createCell(2).setCellValue("Common Alert or Alarm Name");
            nurseHeader.createCell(3).setCellValue("Priority");
            nurseHeader.createCell(6).setCellValue("Time to 1st Recipient");
            nurseHeader.createCell(7).setCellValue("1st Recipient");
            
            Row nurseData = nurseSheet.createRow(3);
            nurseData.createCell(0).setCellValue("TRUE");
            nurseData.createCell(1).setCellValue("Nurse Group 1");
            nurseData.createCell(2).setCellValue("Test Alarm");
            nurseData.createCell(3).setCellValue("Normal");
            nurseData.createCell(6).setCellValue("0");
            nurseData.createCell(7).setCellValue("Custom Unit All Nurse, All CNA, Charge Nurse");
            
            try (FileOutputStream fos = new FileOutputStream(excelFile)) {
                wb.write(fos);
            }
        }
        
        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(excelFile);
        
        Map<String, Object> nurseJson = parser.buildNurseCallsJson();
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> flows = (List<Map<String, Object>>) nurseJson.get("deliveryFlows");
        Map<String, Object> flow = flows.get(0);
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> conditions = (List<Map<String, Object>>) flow.get("conditions");
        Map<String, Object> condition = conditions.get(0);
        
        assertEquals("Custom All Assigned Nurse and CNA and Charge Nurse", condition.get("name"));
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> filters = (List<Map<String, Object>>) condition.get("filters");
        
        // Check that "All" prefix was removed from each role
        assertEquals("Nurse, CNA, Charge Nurse", filters.get(0).get("value"));
    }

    @Test
    void testCustomUnitUrgentPriority() throws Exception {
        File excelFile = new File(tempDir, "test-custom-unit-urgent.xlsx");
        
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
            
            // Nurse Call sheet with Urgent priority
            Sheet nurseSheet = wb.createSheet("Nurse Call");
            Row nurseHeader = nurseSheet.createRow(2);
            nurseHeader.createCell(0).setCellValue("In scope");
            nurseHeader.createCell(1).setCellValue("Configuration Group");
            nurseHeader.createCell(2).setCellValue("Common Alert or Alarm Name");
            nurseHeader.createCell(3).setCellValue("Priority");
            nurseHeader.createCell(6).setCellValue("Time to 1st Recipient");
            nurseHeader.createCell(7).setCellValue("1st Recipient");
            
            Row nurseData = nurseSheet.createRow(3);
            nurseData.createCell(0).setCellValue("TRUE");
            nurseData.createCell(1).setCellValue("Nurse Group 1");
            nurseData.createCell(2).setCellValue("Test Alarm");
            nurseData.createCell(3).setCellValue("Urgent");
            nurseData.createCell(6).setCellValue("0");
            nurseData.createCell(7).setCellValue("Custom Unit Nurse, CNA");
            
            try (FileOutputStream fos = new FileOutputStream(excelFile)) {
                wb.write(fos);
            }
        }
        
        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(excelFile);
        
        Map<String, Object> nurseJson = parser.buildNurseCallsJson();
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> flows = (List<Map<String, Object>>) nurseJson.get("deliveryFlows");
        Map<String, Object> flow = flows.get(0);
        
        // Check conditions
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> conditions = (List<Map<String, Object>>) flow.get("conditions");
        Map<String, Object> condition = conditions.get(0);
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> filters = (List<Map<String, Object>>) condition.get("filters");
        
        // Urgent priority should have 4 filters (includes presence)
        assertEquals(4, filters.size(), "Urgent priority should have 4 filters");
        
        // Check filter 4: presence (only for urgent)
        assertEquals("bed.room.unit.rooms.beds.locs.assignments.usr.presence_show", filters.get(3).get("attributePath"));
        assertEquals("in", filters.get(3).get("operator"));
        assertEquals("Chat, Available", filters.get(3).get("value"));
    }

    @Test
    void testCustomUnitCaseInsensitive() throws Exception {
        File excelFile = new File(tempDir, "test-custom-unit-case.xlsx");
        
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
            
            // Nurse Call sheet with different case variations
            Sheet nurseSheet = wb.createSheet("Nurse Call");
            Row nurseHeader = nurseSheet.createRow(2);
            nurseHeader.createCell(0).setCellValue("In scope");
            nurseHeader.createCell(1).setCellValue("Configuration Group");
            nurseHeader.createCell(2).setCellValue("Common Alert or Alarm Name");
            nurseHeader.createCell(3).setCellValue("Priority");
            nurseHeader.createCell(6).setCellValue("Time to 1st Recipient");
            nurseHeader.createCell(7).setCellValue("1st Recipient");
            
            Row nurseData = nurseSheet.createRow(3);
            nurseData.createCell(0).setCellValue("TRUE");
            nurseData.createCell(1).setCellValue("Nurse Group 1");
            nurseData.createCell(2).setCellValue("Test Alarm");
            nurseData.createCell(3).setCellValue("Normal");
            nurseData.createCell(6).setCellValue("0");
            nurseData.createCell(7).setCellValue("CUSTOM UNIT ALL nurse, cna");
            
            try (FileOutputStream fos = new FileOutputStream(excelFile)) {
                wb.write(fos);
            }
        }
        
        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(excelFile);
        
        Map<String, Object> nurseJson = parser.buildNurseCallsJson();
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> flows = (List<Map<String, Object>>) nurseJson.get("deliveryFlows");
        assertEquals(1, flows.size(), "Should parse case-insensitive Custom Unit");
        
        Map<String, Object> flow = flows.get(0);
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> conditions = (List<Map<String, Object>>) flow.get("conditions");
        Map<String, Object> condition = conditions.get(0);
        
        // Roles should be preserved with original case
        assertEquals("Custom All Assigned nurse and cna", condition.get("name"));
    }

    @Test
    void testCustomUnitWithSpecialCharacters() throws Exception {
        File excelFile = new File(tempDir, "test-custom-unit-special.xlsx");
        
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
            
            // Nurse Call sheet with special characters in role names (e.g., "]" after role)
            Sheet nurseSheet = wb.createSheet("Nurse Call");
            Row nurseHeader = nurseSheet.createRow(2);
            nurseHeader.createCell(0).setCellValue("In scope");
            nurseHeader.createCell(1).setCellValue("Configuration Group");
            nurseHeader.createCell(2).setCellValue("Common Alert or Alarm Name");
            nurseHeader.createCell(3).setCellValue("Priority");
            nurseHeader.createCell(6).setCellValue("Time to 1st Recipient");
            nurseHeader.createCell(7).setCellValue("1st Recipient");
            
            Row nurseData = nurseSheet.createRow(3);
            nurseData.createCell(0).setCellValue("TRUE");
            nurseData.createCell(1).setCellValue("Nurse Group 1");
            nurseData.createCell(2).setCellValue("Test Alarm");
            nurseData.createCell(3).setCellValue("Normal");
            nurseData.createCell(6).setCellValue("0");
            // Test with ] character that should be stripped
            nurseData.createCell(7).setCellValue("Custom Unit Nurse], CNA#, Charge Nurse@");
            
            try (FileOutputStream fos = new FileOutputStream(excelFile)) {
                wb.write(fos);
            }
        }
        
        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(excelFile);
        
        Map<String, Object> nurseJson = parser.buildNurseCallsJson();
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> flows = (List<Map<String, Object>>) nurseJson.get("deliveryFlows");
        assertEquals(1, flows.size(), "Should parse Custom Unit with special characters");
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> conditions = (List<Map<String, Object>>) flows.get(0).get("conditions");
        assertNotNull(conditions);
        assertEquals(1, conditions.size());
        
        // Verify that special characters are stripped but spaces are preserved
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> filters = (List<Map<String, Object>>) conditions.get(0).get("filters");
        assertEquals("Nurse, CNA, Charge Nurse", filters.get(0).get("value"), 
            "Special characters should be stripped, but spaces between words should be preserved");
    }

    @Test
    void testCustomUnitInOrdersFlowUsesPatientPath() throws Exception {
        File excelFile = new File(tempDir, "test-custom-unit-orders.xlsx");
        
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            // Unit Breakdown sheet with Orders configuration
            Sheet unitSheet = wb.createSheet("Unit Breakdown");
            Row unitHeader = unitSheet.createRow(2);
            unitHeader.createCell(0).setCellValue("Facility");
            unitHeader.createCell(1).setCellValue("Common Unit Name");
            unitHeader.createCell(2).setCellValue("Nurse Call Configuration Group");
            unitHeader.createCell(3).setCellValue("Patient Monitoring Configuration Group");
            unitHeader.createCell(4).setCellValue("Orders Configuration Group");
            unitHeader.createCell(5).setCellValue("No Caregiver Group");
            
            Row unitData = unitSheet.createRow(3);
            unitData.createCell(0).setCellValue("Test Facility");
            unitData.createCell(1).setCellValue("Unit 1");
            unitData.createCell(2).setCellValue("Nurse Group 1");
            unitData.createCell(3).setCellValue("Clinical Group 1");
            unitData.createCell(4).setCellValue("Orders Group 1");
            unitData.createCell(5).setCellValue("No Caregiver Group");
            
            // Orders sheet with Custom Unit pattern
            Sheet ordersSheet = wb.createSheet("Order");
            Row orderHeader = ordersSheet.createRow(2);
            orderHeader.createCell(0).setCellValue("In scope");
            orderHeader.createCell(1).setCellValue("Configuration Group");
            orderHeader.createCell(2).setCellValue("Common Alert or Alarm Name");
            orderHeader.createCell(3).setCellValue("Sending System Alert Name");
            orderHeader.createCell(4).setCellValue("Priority");
            orderHeader.createCell(5).setCellValue("Device - A");
            orderHeader.createCell(6).setCellValue("Time to 1st Recipient");
            orderHeader.createCell(7).setCellValue("1st Recipient");
            
            Row orderData = ordersSheet.createRow(3);
            orderData.createCell(0).setCellValue("TRUE");
            orderData.createCell(1).setCellValue("Orders Group 1");
            orderData.createCell(2).setCellValue("Test Order");
            orderData.createCell(3).setCellValue("Test Order System Name");
            orderData.createCell(4).setCellValue("Normal");
            orderData.createCell(5).setCellValue("Edge");
            orderData.createCell(6).setCellValue("0");
            orderData.createCell(7).setCellValue("Custom Unit Nurse, CNA");
            
            try (FileOutputStream fos = new FileOutputStream(excelFile)) {
                wb.write(fos);
            }
        }
        
        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(excelFile);
        
        Map<String, Object> ordersJson = parser.buildOrdersJson();
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> flows = (List<Map<String, Object>>) ordersJson.get("deliveryFlows");
        assertEquals(1, flows.size(), "Should have one Orders flow");
        
        Map<String, Object> flow = flows.get(0);
        
        // Check conditions
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> conditions = (List<Map<String, Object>>) flow.get("conditions");
        assertNotNull(conditions, "Should have conditions");
        assertEquals(1, conditions.size(), "Should have one condition");
        
        Map<String, Object> condition = conditions.get(0);
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> filters = (List<Map<String, Object>>) condition.get("filters");
        assertEquals(3, filters.size(), "Normal priority should have 3 filters");
        
        // Verify that Orders flow uses patient.current_place.locs.units path instead of bed.room.unit.rooms.beds
        assertEquals("patient.current_place.locs.units.locs.assignments.role.name", filters.get(0).get("attributePath"),
            "Orders flow should use patient.current_place.locs.units path");
        assertEquals("patient.current_place.locs.units.locs.assignments.state", filters.get(1).get("attributePath"),
            "Orders flow should use patient.current_place.locs.units path");
        assertEquals("patient.current_place.locs.units.locs.assignments.usr.devices.status", filters.get(2).get("attributePath"),
            "Orders flow should use patient.current_place.locs.units path");
        
        // Check destinations also use the correct path
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> destinations = (List<Map<String, Object>>) flow.get("destinations");
        assertEquals(1, destinations.size(), "Should have one destination");
        
        Map<String, Object> dest = destinations.get(0);
        assertEquals("patient.current_place.locs.units.locs.assignments.usr.devices.lines.number", dest.get("attributePath"),
            "Orders flow destination should use patient.current_place.locs.units path");
    }
}
