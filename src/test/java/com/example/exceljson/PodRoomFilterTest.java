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
 * Test to verify POD room filter functionality for NurseCalls and Clinicals.
 * Verifies that the "Filter for POD Rooms (Optional)" column in Unit Breakdown
 * adds appropriate filter conditions to flows.
 */
class PodRoomFilterTest {

    @Test
    void nursecallPodRoomFilterAddsCondition() throws Exception {
        Path tempDir = Files.createTempDirectory("pod-filter-test");
        Path excelPath = tempDir.resolve("input.xlsx");

        // Create minimal workbook
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            // Unit Breakdown with POD filter
            Sheet units = workbook.createSheet("Unit Breakdown");
            Row unitsHeader = units.createRow(2);
            unitsHeader.createCell(0).setCellValue("Facility");
            unitsHeader.createCell(1).setCellValue("Common Unit Name");
            unitsHeader.createCell(2).setCellValue("Filter for POD Rooms (Optional)");
            unitsHeader.createCell(3).setCellValue("Nurse Call Configuration Group");
            Row unitsRow = units.createRow(3);
            unitsRow.createCell(0).setCellValue("TestFacility");
            unitsRow.createCell(1).setCellValue("TestUnit");
            unitsRow.createCell(2).setCellValue("POD 1");
            unitsRow.createCell(3).setCellValue("TestGroup");

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

        // Parse the file
        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(excelPath.toFile());
        
        Map<String, Object> json = parser.buildNurseCallsJson();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> flows = (List<Map<String, Object>>) json.get("deliveryFlows");
        
        assertNotNull(flows);
        assertEquals(1, flows.size());
        
        Map<String, Object> flow = flows.get(0);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> conditions = (List<Map<String, Object>>) flow.get("conditions");
        
        assertNotNull(conditions);
        // Should have NurseCallsCondition + POD Room Filter
        assertEquals(2, conditions.size());
        
        // Check that NurseCallsCondition exists
        Map<String, Object> nurseCallsCond = conditions.get(0);
        assertEquals("NurseCallsCondition", nurseCallsCond.get("name"));
        
        // Check that POD room filter condition exists
        Map<String, Object> podFilterCond = conditions.get(1);
        assertEquals("POD rooms filter", podFilterCond.get("name"));
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> filters = (List<Map<String, Object>>) podFilterCond.get("filters");
        assertEquals(1, filters.size());
        
        Map<String, Object> filter = filters.get(0);
        assertEquals("bed.room.room_number", filter.get("attributePath"));
        assertEquals("in", filter.get("operator"));
        assertEquals("POD 1", filter.get("value"));
    }

    @Test
    void clinicalPodRoomFilterAddsCondition() throws Exception {
        Path tempDir = Files.createTempDirectory("pod-filter-clinical-test");
        Path excelPath = tempDir.resolve("input.xlsx");

        // Create minimal workbook
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            // Unit Breakdown with POD filter
            Sheet units = workbook.createSheet("Unit Breakdown");
            Row unitsHeader = units.createRow(2);
            unitsHeader.createCell(0).setCellValue("Facility");
            unitsHeader.createCell(1).setCellValue("Common Unit Name");
            unitsHeader.createCell(2).setCellValue("Filter for POD Rooms (Optional)");
            unitsHeader.createCell(3).setCellValue("Patient Monitoring Configuration Group");
            Row unitsRow = units.createRow(3);
            unitsRow.createCell(0).setCellValue("TestFacility");
            unitsRow.createCell(1).setCellValue("TestUnit");
            unitsRow.createCell(2).setCellValue("POD 2, POD 3");
            unitsRow.createCell(3).setCellValue("TestGroup");

            // Nurse Call (empty)
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
            clinicalRow.createCell(0).setCellValue("TestGroup");
            clinicalRow.createCell(1).setCellValue("HR High");
            clinicalRow.createCell(2).setCellValue("0");
            clinicalRow.createCell(3).setCellValue("Nurse");
            
            Sheet orders = workbook.createSheet("Order");
            Row ordersHeader = orders.createRow(2);
            ordersHeader.createCell(0).setCellValue("Configuration Group");
            ordersHeader.createCell(1).setCellValue("Common Alert or Alarm Name");

            try (OutputStream os = Files.newOutputStream(excelPath)) {
                workbook.write(os);
            }
        }

        // Parse the file
        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(excelPath.toFile());
        
        Map<String, Object> json = parser.buildClinicalsJson();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> flows = (List<Map<String, Object>>) json.get("deliveryFlows");
        
        assertNotNull(flows);
        assertEquals(1, flows.size());
        
        Map<String, Object> flow = flows.get(0);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> conditions = (List<Map<String, Object>>) flow.get("conditions");
        
        assertNotNull(conditions);
        // Should have POD Room Filter only (Clinicals don't have default condition)
        assertEquals(1, conditions.size());
        
        // Check that POD room filter condition exists
        Map<String, Object> podFilterCond = conditions.get(0);
        assertEquals("POD rooms filter", podFilterCond.get("name"));
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> filters = (List<Map<String, Object>>) podFilterCond.get("filters");
        assertEquals(1, filters.size());
        
        Map<String, Object> filter = filters.get(0);
        assertEquals("bed.room.room_number", filter.get("attributePath"));
        assertEquals("in", filter.get("operator"));
        // Comma-separated values should be preserved
        assertEquals("POD 2, POD 3", filter.get("value"));
    }

    @Test
    void noPodRoomFilterWhenColumnEmpty() throws Exception {
        Path tempDir = Files.createTempDirectory("no-pod-filter-test");
        Path excelPath = tempDir.resolve("input.xlsx");

        // Create minimal workbook
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            // Unit Breakdown without POD filter value
            Sheet units = workbook.createSheet("Unit Breakdown");
            Row unitsHeader = units.createRow(2);
            unitsHeader.createCell(0).setCellValue("Facility");
            unitsHeader.createCell(1).setCellValue("Common Unit Name");
            unitsHeader.createCell(2).setCellValue("Filter for POD Rooms (Optional)");
            unitsHeader.createCell(3).setCellValue("Nurse Call Configuration Group");
            Row unitsRow = units.createRow(3);
            unitsRow.createCell(0).setCellValue("TestFacility");
            unitsRow.createCell(1).setCellValue("TestUnit");
            unitsRow.createCell(2).setCellValue(""); // Empty POD filter
            unitsRow.createCell(3).setCellValue("TestGroup");

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

        // Parse the file
        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(excelPath.toFile());
        
        Map<String, Object> json = parser.buildNurseCallsJson();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> flows = (List<Map<String, Object>>) json.get("deliveryFlows");
        
        assertNotNull(flows);
        assertEquals(1, flows.size());
        
        Map<String, Object> flow = flows.get(0);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> conditions = (List<Map<String, Object>>) flow.get("conditions");
        
        assertNotNull(conditions);
        // Should have only NurseCallsCondition (no POD filter)
        assertEquals(1, conditions.size());
        
        // Check that NurseCallsCondition exists
        Map<String, Object> nurseCallsCond = conditions.get(0);
        assertEquals("NurseCallsCondition", nurseCallsCond.get("name"));
    }

    @Test
    void podRoomFilterStripsSpecialCharacters() throws Exception {
        Path tempDir = Files.createTempDirectory("pod-filter-special-chars-test");
        Path excelPath = tempDir.resolve("input.xlsx");

        // Create minimal workbook
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            // Unit Breakdown with POD filter containing special characters
            Sheet units = workbook.createSheet("Unit Breakdown");
            Row unitsHeader = units.createRow(2);
            unitsHeader.createCell(0).setCellValue("Facility");
            unitsHeader.createCell(1).setCellValue("Common Unit Name");
            unitsHeader.createCell(2).setCellValue("Filter for POD Rooms (Optional)");
            unitsHeader.createCell(3).setCellValue("Nurse Call Configuration Group");
            Row unitsRow = units.createRow(3);
            unitsRow.createCell(0).setCellValue("TestFacility");
            unitsRow.createCell(1).setCellValue("TestUnit");
            unitsRow.createCell(2).setCellValue("POD #1, POD @2");
            unitsRow.createCell(3).setCellValue("TestGroup");

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

        // Parse the file
        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(excelPath.toFile());
        
        Map<String, Object> json = parser.buildNurseCallsJson();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> flows = (List<Map<String, Object>>) json.get("deliveryFlows");
        
        assertNotNull(flows);
        assertEquals(1, flows.size());
        
        Map<String, Object> flow = flows.get(0);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> conditions = (List<Map<String, Object>>) flow.get("conditions");
        
        assertNotNull(conditions);
        assertEquals(2, conditions.size());
        
        // Check that POD room filter condition exists with cleaned value
        Map<String, Object> podFilterCond = conditions.get(1);
        assertEquals("POD rooms filter", podFilterCond.get("name"));
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> filters = (List<Map<String, Object>>) podFilterCond.get("filters");
        assertEquals(1, filters.size());
        
        Map<String, Object> filter = filters.get(0);
        assertEquals("bed.room.room_number", filter.get("attributePath"));
        assertEquals("in", filter.get("operator"));
        // Special characters should be stripped, leaving only alphanumeric, spaces, and commas
        assertEquals("POD 1, POD 2", filter.get("value"));
    }

    @Test
    void noPodRoomFilterWhenColumnMissing() throws Exception {
        Path tempDir = Files.createTempDirectory("no-pod-column-test");
        Path excelPath = tempDir.resolve("input.xlsx");

        // Create minimal workbook without the POD filter column
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            // Unit Breakdown without POD filter column at all
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

        // Parse the file
        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(excelPath.toFile());
        
        Map<String, Object> json = parser.buildNurseCallsJson();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> flows = (List<Map<String, Object>>) json.get("deliveryFlows");
        
        assertNotNull(flows);
        assertEquals(1, flows.size());
        
        Map<String, Object> flow = flows.get(0);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> conditions = (List<Map<String, Object>>) flow.get("conditions");
        
        assertNotNull(conditions);
        // Should have only NurseCallsCondition (no POD filter)
        assertEquals(1, conditions.size());
        
        // Check that NurseCallsCondition exists
        Map<String, Object> nurseCallsCond = conditions.get(0);
        assertEquals("NurseCallsCondition", nurseCallsCond.get("name"));
    }
}
