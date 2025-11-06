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
 * Tests for eventIdentification parameter values for different flow types.
 * Verifies that the flow type prefix is correctly added to eventIdentification.
 */
class EventIdentificationTest {

    @TempDir
    File tempDir;

    @Test
    void testNursecallEventIdentification() throws Exception {
        File excelFile = new File(tempDir, "test-nursecall-event-id.xlsx");
        
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
            
            // Nurse Call sheet
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
            nurseData.createCell(7).setCellValue("Charge Nurse");
            
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
        
        // Check parameter attributes for eventIdentification
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> params = (List<Map<String, Object>>) flow.get("parameterAttributes");
        assertNotNull(params, "Should have parameter attributes");
        
        boolean foundEventId = false;
        for (Map<String, Object> param : params) {
            if ("eventIdentification".equals(param.get("name"))) {
                String value = (String) param.get("value");
                assertEquals("\"NurseCalls:#{id}\"", value, "NurseCalls eventIdentification should be NurseCalls:#{id}");
                foundEventId = true;
                break;
            }
        }
        assertTrue(foundEventId, "eventIdentification parameter should exist");
    }

    @Test
    void testClinicalEventIdentification() throws Exception {
        File excelFile = new File(tempDir, "test-clinical-event-id.xlsx");
        
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            // Unit Breakdown sheet
            Sheet unitSheet = wb.createSheet("Unit Breakdown");
            Row unitHeader = unitSheet.createRow(2);
            unitHeader.createCell(0).setCellValue("Facility");
            unitHeader.createCell(1).setCellValue("Common Unit Name");
            unitHeader.createCell(3).setCellValue("Clinicals Configuration Group");
            
            Row unitData = unitSheet.createRow(3);
            unitData.createCell(0).setCellValue("Test Facility");
            unitData.createCell(1).setCellValue("Unit 1");
            unitData.createCell(3).setCellValue("Clinical Group 1");
            
            // Patient Monitoring sheet
            Sheet clinSheet = wb.createSheet("Patient Monitoring");
            Row clinHeader = clinSheet.createRow(2);
            clinHeader.createCell(0).setCellValue("In scope");
            clinHeader.createCell(1).setCellValue("Configuration Group");
            clinHeader.createCell(2).setCellValue("Common Alert or Alarm Name");
            clinHeader.createCell(3).setCellValue("Priority");
            
            Row clinData = clinSheet.createRow(3);
            clinData.createCell(0).setCellValue("TRUE");
            clinData.createCell(1).setCellValue("Clinical Group 1");
            clinData.createCell(2).setCellValue("Test Clinical Alarm");
            clinData.createCell(3).setCellValue("Normal");
            
            try (FileOutputStream fos = new FileOutputStream(excelFile)) {
                wb.write(fos);
            }
        }
        
        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(excelFile);
        
        Map<String, Object> clinJson = parser.buildClinicalsJson();
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> flows = (List<Map<String, Object>>) clinJson.get("deliveryFlows");
        assertEquals(1, flows.size(), "Should have one flow");
        
        Map<String, Object> flow = flows.get(0);
        
        // Check parameter attributes for eventIdentification
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> params = (List<Map<String, Object>>) flow.get("parameterAttributes");
        assertNotNull(params, "Should have parameter attributes");
        
        boolean foundEventId = false;
        for (Map<String, Object> param : params) {
            if ("eventIdentification".equals(param.get("name"))) {
                String value = (String) param.get("value");
                assertEquals("\"Clinicals:#{id}\"", value, "Clinicals eventIdentification should be Clinicals:#{id}");
                foundEventId = true;
                break;
            }
        }
        assertTrue(foundEventId, "eventIdentification parameter should exist");
    }

    @Test
    void testOrdersEventIdentification() throws Exception {
        File excelFile = new File(tempDir, "test-orders-event-id.xlsx");
        
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            // Unit Breakdown sheet
            Sheet unitSheet = wb.createSheet("Unit Breakdown");
            Row unitHeader = unitSheet.createRow(2);
            unitHeader.createCell(0).setCellValue("Facility");
            unitHeader.createCell(1).setCellValue("Common Unit Name");
            unitHeader.createCell(4).setCellValue("Orders Configuration Group");
            
            Row unitData = unitSheet.createRow(3);
            unitData.createCell(0).setCellValue("Test Facility");
            unitData.createCell(1).setCellValue("Unit 1");
            unitData.createCell(4).setCellValue("Orders Group 1");
            
            // Orders sheet
            Sheet ordersSheet = wb.createSheet("Orders");
            Row ordersHeader = ordersSheet.createRow(2);
            ordersHeader.createCell(0).setCellValue("In scope");
            ordersHeader.createCell(1).setCellValue("Configuration Group");
            ordersHeader.createCell(2).setCellValue("Common Alert or Alarm Name");
            ordersHeader.createCell(3).setCellValue("Priority");
            ordersHeader.createCell(6).setCellValue("Time to 1st Recipient");
            ordersHeader.createCell(7).setCellValue("1st Recipient");
            
            Row ordersData = ordersSheet.createRow(3);
            ordersData.createCell(0).setCellValue("TRUE");
            ordersData.createCell(1).setCellValue("Orders Group 1");
            ordersData.createCell(2).setCellValue("Test Order");
            ordersData.createCell(3).setCellValue("Normal");
            ordersData.createCell(6).setCellValue("0");
            ordersData.createCell(7).setCellValue("Charge Nurse");
            
            try (FileOutputStream fos = new FileOutputStream(excelFile)) {
                wb.write(fos);
            }
        }
        
        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(excelFile);
        
        Map<String, Object> ordersJson = parser.buildOrdersJson();
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> flows = (List<Map<String, Object>>) ordersJson.get("deliveryFlows");
        assertEquals(1, flows.size(), "Should have one flow");
        
        Map<String, Object> flow = flows.get(0);
        
        // Check parameter attributes for eventIdentification
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> params = (List<Map<String, Object>>) flow.get("parameterAttributes");
        assertNotNull(params, "Should have parameter attributes");
        
        boolean foundEventId = false;
        for (Map<String, Object> param : params) {
            if ("eventIdentification".equals(param.get("name"))) {
                String value = (String) param.get("value");
                assertEquals("\"Orders:#{id}\"", value, "Orders eventIdentification should be Orders:#{id}");
                foundEventId = true;
                break;
            }
        }
        assertTrue(foundEventId, "eventIdentification parameter should exist");
    }
}
