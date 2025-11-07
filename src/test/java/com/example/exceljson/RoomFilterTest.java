package com.example.exceljson;

import static org.junit.jupiter.api.Assertions.*;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

/**
 * Test to verify room filter functionality for NurseCalls, Clinicals, and Orders.
 */
class RoomFilterTest {

    @Test
    void nursecallRoomFilterAddsCondition() throws Exception {
        Path tempDir = Files.createTempDirectory("room-filter-test");
        Path excelPath = tempDir.resolve("input.xlsx");

        // Create minimal workbook
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            // Unit Breakdown
            Sheet units = workbook.createSheet("Unit Breakdown");
            Row unitsHeader = units.createRow(2);
            unitsHeader.createCell(0).setCellValue("Facility");
            unitsHeader.createCell(1).setCellValue("Common Unit Name");
            unitsHeader.createCell(2).setCellValue("Nurse Call Configuration Group");
            Row unitsRow = units.createRow(3);
            unitsRow.createCell(0).setCellValue("TestFacility");
            unitsRow.createCell(1).setCellValue("TestUnit");
            unitsRow.createCell(2).setCellValue("TestGroup");

            // Nurse Call
            Sheet nurseCalls = workbook.createSheet("Nurse call");
            Row nurseHeader = nurseCalls.createRow(2);
            nurseHeader.createCell(0).setCellValue("Configuration Group");
            nurseHeader.createCell(1).setCellValue("Common Alert or Alarm Name");
            nurseHeader.createCell(2).setCellValue("Time to 1st Recipient");
            nurseHeader.createCell(3).setCellValue("1st Recipient");
            
            Row nurseRow = nurseCalls.createRow(3);
            nurseRow.createCell(0).setCellValue("TestGroup");
            nurseRow.createCell(1).setCellValue("Call Button");
            nurseRow.createCell(2).setCellValue("0");
            nurseRow.createCell(3).setCellValue("Nurse");

            // Empty Clinical and Order sheets
            Sheet clinicals = workbook.createSheet("Patient Monitoring");
            Row clinicalHeader = clinicals.createRow(2);
            clinicalHeader.createCell(0).setCellValue("Configuration Group");
            clinicalHeader.createCell(1).setCellValue("Common Alert or Alarm Name");
            
            Sheet orders = workbook.createSheet("Order");
            Row ordersHeader = orders.createRow(2);
            ordersHeader.createCell(0).setCellValue("Configuration Group");
            ordersHeader.createCell(1).setCellValue("Common Alert or Alarm Name");

            try (OutputStream os = Files.newOutputStream(excelPath)) {
                workbook.write(os);
            }
        }

        // Test with room filter
        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(excelPath.toFile());
        parser.setRoomFilters("123", "", "");
        
        Map<String, Object> json = parser.buildNurseCallsJson();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> flows = (List<Map<String, Object>>) json.get("deliveryFlows");
        
        assertNotNull(flows);
        assertEquals(1, flows.size());
        
        Map<String, Object> flow = flows.get(0);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> conditions = (List<Map<String, Object>>) flow.get("conditions");
        
        assertNotNull(conditions);
        // Should have NurseCallsCondition + Room Filter
        assertEquals(2, conditions.size());
        
        // Check that room filter condition exists
        Map<String, Object> roomFilterCond = conditions.get(1);
        assertEquals("Room Filter For TT", roomFilterCond.get("name"));
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> filters = (List<Map<String, Object>>) roomFilterCond.get("filters");
        assertEquals(1, filters.size());
        
        Map<String, Object> filter = filters.get(0);
        assertEquals("bed.room.room_number", filter.get("attributePath"));
        assertEquals("equal", filter.get("operator"));
        assertEquals("123", filter.get("value"));
    }

    @Test
    void nursecallWithoutRoomFilterHasOnlyDefaultCondition() throws Exception {
        Path tempDir = Files.createTempDirectory("room-filter-test");
        Path excelPath = tempDir.resolve("input.xlsx");

        // Create minimal workbook
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            // Unit Breakdown
            Sheet units = workbook.createSheet("Unit Breakdown");
            Row unitsHeader = units.createRow(2);
            unitsHeader.createCell(0).setCellValue("Facility");
            unitsHeader.createCell(1).setCellValue("Common Unit Name");
            unitsHeader.createCell(2).setCellValue("Nurse Call Configuration Group");
            Row unitsRow = units.createRow(3);
            unitsRow.createCell(0).setCellValue("TestFacility");
            unitsRow.createCell(1).setCellValue("TestUnit");
            unitsRow.createCell(2).setCellValue("TestGroup");

            // Nurse Call
            Sheet nurseCalls = workbook.createSheet("Nurse call");
            Row nurseHeader = nurseCalls.createRow(2);
            nurseHeader.createCell(0).setCellValue("Configuration Group");
            nurseHeader.createCell(1).setCellValue("Common Alert or Alarm Name");
            nurseHeader.createCell(2).setCellValue("Time to 1st Recipient");
            nurseHeader.createCell(3).setCellValue("1st Recipient");
            
            Row nurseRow = nurseCalls.createRow(3);
            nurseRow.createCell(0).setCellValue("TestGroup");
            nurseRow.createCell(1).setCellValue("Call Button");
            nurseRow.createCell(2).setCellValue("0");
            nurseRow.createCell(3).setCellValue("Nurse");

            // Empty Clinical and Order sheets
            Sheet clinicals = workbook.createSheet("Patient Monitoring");
            Row clinicalHeader = clinicals.createRow(2);
            clinicalHeader.createCell(0).setCellValue("Configuration Group");
            clinicalHeader.createCell(1).setCellValue("Common Alert or Alarm Name");
            
            Sheet orders = workbook.createSheet("Order");
            Row ordersHeader = orders.createRow(2);
            ordersHeader.createCell(0).setCellValue("Configuration Group");
            ordersHeader.createCell(1).setCellValue("Common Alert or Alarm Name");

            try (OutputStream os = Files.newOutputStream(excelPath)) {
                workbook.write(os);
            }
        }

        // Test without room filter
        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(excelPath.toFile());
        parser.setRoomFilters("", "", "");
        
        Map<String, Object> json = parser.buildNurseCallsJson();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> flows = (List<Map<String, Object>>) json.get("deliveryFlows");
        
        assertNotNull(flows);
        assertEquals(1, flows.size());
        
        Map<String, Object> flow = flows.get(0);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> conditions = (List<Map<String, Object>>) flow.get("conditions");
        
        assertNotNull(conditions);
        // Should have only NurseCallsCondition (no room filter)
        assertEquals(1, conditions.size());
        assertEquals("NurseCallsCondition", conditions.get(0).get("name"));
    }

    @Test
    void clinicalRoomFilterAddsCondition() throws Exception {
        Path tempDir = Files.createTempDirectory("room-filter-test");
        Path excelPath = tempDir.resolve("input.xlsx");

        // Create minimal workbook
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            // Unit Breakdown
            Sheet units = workbook.createSheet("Unit Breakdown");
            Row unitsHeader = units.createRow(2);
            unitsHeader.createCell(0).setCellValue("Facility");
            unitsHeader.createCell(1).setCellValue("Common Unit Name");
            unitsHeader.createCell(3).setCellValue("Patient Monitoring Configuration Group");
            Row unitsRow = units.createRow(3);
            unitsRow.createCell(0).setCellValue("TestFacility");
            unitsRow.createCell(1).setCellValue("TestUnit");
            unitsRow.createCell(3).setCellValue("ClinicalGroup");

            // Empty Nurse Call sheet
            Sheet nurseCalls = workbook.createSheet("Nurse call");
            Row nurseHeader = nurseCalls.createRow(2);
            nurseHeader.createCell(0).setCellValue("Configuration Group");
            nurseHeader.createCell(1).setCellValue("Common Alert or Alarm Name");

            // Clinical
            Sheet clinicals = workbook.createSheet("Patient Monitoring");
            Row clinicalHeader = clinicals.createRow(2);
            clinicalHeader.createCell(0).setCellValue("Configuration Group");
            clinicalHeader.createCell(1).setCellValue("Common Alert or Alarm Name");
            clinicalHeader.createCell(2).setCellValue("Time to 1st Recipient");
            clinicalHeader.createCell(3).setCellValue("1st Recipient");
            
            Row clinicalRow = clinicals.createRow(3);
            clinicalRow.createCell(0).setCellValue("ClinicalGroup");
            clinicalRow.createCell(1).setCellValue("SpO2 Low");
            clinicalRow.createCell(2).setCellValue("0");
            clinicalRow.createCell(3).setCellValue("Nurse");
            
            // Empty Order sheet
            Sheet orders = workbook.createSheet("Order");
            Row ordersHeader = orders.createRow(2);
            ordersHeader.createCell(0).setCellValue("Configuration Group");
            ordersHeader.createCell(1).setCellValue("Common Alert or Alarm Name");

            try (OutputStream os = Files.newOutputStream(excelPath)) {
                workbook.write(os);
            }
        }

        // Test with clinical room filter
        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(excelPath.toFile());
        parser.setRoomFilters("", "456", "");
        
        Map<String, Object> json = parser.buildClinicalsJson();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> flows = (List<Map<String, Object>>) json.get("deliveryFlows");
        
        assertNotNull(flows);
        assertEquals(1, flows.size());
        
        Map<String, Object> flow = flows.get(0);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> conditions = (List<Map<String, Object>>) flow.get("conditions");
        
        assertNotNull(conditions);
        // Clinicals should have the room filter condition (no default condition)
        assertEquals(1, conditions.size());
        
        Map<String, Object> roomFilterCond = conditions.get(0);
        assertEquals("Room Filter For TT", roomFilterCond.get("name"));
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> filters = (List<Map<String, Object>>) roomFilterCond.get("filters");
        assertEquals(1, filters.size());
        
        Map<String, Object> filter = filters.get(0);
        assertEquals("bed.room.room_number", filter.get("attributePath"));
        assertEquals("equal", filter.get("operator"));
        assertEquals("456", filter.get("value"));
    }

    @Test
    void ordersRoomFilterAddsConditionWithDifferentStructure() throws Exception {
        Path tempDir = Files.createTempDirectory("room-filter-test");
        Path excelPath = tempDir.resolve("input.xlsx");

        // Create minimal workbook
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            // Unit Breakdown
            Sheet units = workbook.createSheet("Unit Breakdown");
            Row unitsHeader = units.createRow(2);
            unitsHeader.createCell(0).setCellValue("Facility");
            unitsHeader.createCell(1).setCellValue("Common Unit Name");
            unitsHeader.createCell(4).setCellValue("Orders Configuration Group");
            Row unitsRow = units.createRow(3);
            unitsRow.createCell(0).setCellValue("TestFacility");
            unitsRow.createCell(1).setCellValue("TestUnit");
            unitsRow.createCell(4).setCellValue("OrdersGroup");

            // Empty Nurse Call and Clinical sheets
            Sheet nurseCalls = workbook.createSheet("Nurse call");
            Row nurseHeader = nurseCalls.createRow(2);
            nurseHeader.createCell(0).setCellValue("Configuration Group");
            nurseHeader.createCell(1).setCellValue("Common Alert or Alarm Name");
            
            Sheet clinicals = workbook.createSheet("Patient Monitoring");
            Row clinicalHeader = clinicals.createRow(2);
            clinicalHeader.createCell(0).setCellValue("Configuration Group");
            clinicalHeader.createCell(1).setCellValue("Common Alert or Alarm Name");

            // Orders
            Sheet orders = workbook.createSheet("Order");
            Row ordersHeader = orders.createRow(2);
            ordersHeader.createCell(0).setCellValue("Configuration Group");
            ordersHeader.createCell(1).setCellValue("Common Alert or Alarm Name");
            ordersHeader.createCell(2).setCellValue("Time to 1st Recipient");
            ordersHeader.createCell(3).setCellValue("1st Recipient");
            
            Row ordersRow = orders.createRow(3);
            ordersRow.createCell(0).setCellValue("OrdersGroup");
            ordersRow.createCell(1).setCellValue("New Order");
            ordersRow.createCell(2).setCellValue("0");
            ordersRow.createCell(3).setCellValue("Nurse");

            try (OutputStream os = Files.newOutputStream(excelPath)) {
                workbook.write(os);
            }
        }

        // Test with orders room filter
        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(excelPath.toFile());
        parser.setRoomFilters("", "", "789");
        
        Map<String, Object> json = parser.buildOrdersJson();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> flows = (List<Map<String, Object>>) json.get("deliveryFlows");
        
        assertNotNull(flows);
        assertEquals(1, flows.size());
        
        Map<String, Object> flow = flows.get(0);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> conditions = (List<Map<String, Object>>) flow.get("conditions");
        
        assertNotNull(conditions);
        // Orders should have Global Condition + Room Filter
        assertEquals(2, conditions.size());
        
        // Check Global Condition
        Map<String, Object> globalCond = conditions.get(0);
        assertEquals("Global Condition", globalCond.get("name"));
        
        // Check Room Filter
        Map<String, Object> roomFilterCond = conditions.get(1);
        assertEquals("Room Filter for TT", roomFilterCond.get("name"));
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> filters = (List<Map<String, Object>>) roomFilterCond.get("filters");
        assertEquals(1, filters.size());
        
        Map<String, Object> filter = filters.get(0);
        assertEquals("patient.current_place.locs.units.rooms.room_number", filter.get("attributePath"));
        assertEquals("in", filter.get("operator"));
        assertEquals("789", filter.get("value"));
    }
}
