package com.example.exceljson;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for Clinical flow destinationName extraction from recipient column.
 * Verifies that Clinical flows extract destinationName based on recipient role,
 * similar to how Nurse Call flows work.
 */
class ClinicalDestinationNameTest {

    @TempDir
    Path tempDir;

    /**
     * Creates a test Excel workbook with Clinical sheet data (via EMDAN).
     */
    private File createClinicalTestWorkbook() throws Exception {
        File excelFile = tempDir.resolve("test-clinical-destname.xlsx").toFile();
        
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            // Create Unit Breakdown sheet
            Sheet unitSheet = wb.createSheet("Unit Breakdown");
            Row headerRow = unitSheet.createRow(2);
            headerRow.createCell(0).setCellValue("Facility");
            headerRow.createCell(1).setCellValue("Common Unit Name");
            headerRow.createCell(2).setCellValue("Nurse Call Configuration Group");
            headerRow.createCell(3).setCellValue("Patient Monitoring Configuration Group");
            headerRow.createCell(5).setCellValue("No Caregiver Group");
            
            Row dataRow = unitSheet.createRow(3);
            dataRow.createCell(0).setCellValue("Test Facility");
            dataRow.createCell(1).setCellValue("Test Unit");
            dataRow.createCell(2).setCellValue("Nurse Group 1");
            dataRow.createCell(3).setCellValue("Clinical Group 1");
            dataRow.createCell(5).setCellValue("House Supervisor");
            
            // Create Nurse Call sheet with EMDAN=Y to move to Clinicals
            Sheet nurseSheet = wb.createSheet("Nurse Call");
            Row nurseHeader = nurseSheet.createRow(2);
            nurseHeader.createCell(0).setCellValue("In scope");
            nurseHeader.createCell(1).setCellValue("Configuration Group");
            nurseHeader.createCell(2).setCellValue("Common Alert or Alarm Name");
            nurseHeader.createCell(3).setCellValue("Sending System Alert Name");
            nurseHeader.createCell(4).setCellValue("Priority");
            nurseHeader.createCell(5).setCellValue("Device - A");
            nurseHeader.createCell(6).setCellValue("EMDAN");
            nurseHeader.createCell(7).setCellValue("Time to 1st Recipient");
            nurseHeader.createCell(8).setCellValue("1st Recipient");
            nurseHeader.createCell(9).setCellValue("Time to 2nd Recipient");
            nurseHeader.createCell(10).setCellValue("2nd Recipient");
            nurseHeader.createCell(11).setCellValue("Time to 3rd Recipient");
            nurseHeader.createCell(12).setCellValue("3rd Recipient");
            
            Row nurseData = nurseSheet.createRow(3);
            nurseData.createCell(0).setCellValue("TRUE");
            nurseData.createCell(1).setCellValue("Nurse Group 1");
            nurseData.createCell(2).setCellValue("SpO2 Alert");
            nurseData.createCell(3).setCellValue("SpO2 Low");
            nurseData.createCell(4).setCellValue("High");
            nurseData.createCell(5).setCellValue("Edge");
            nurseData.createCell(6).setCellValue("Y");  // EMDAN=Y moves to Clinicals
            nurseData.createCell(7).setCellValue("0");
            nurseData.createCell(8).setCellValue("VAssign: Room RN");
            nurseData.createCell(9).setCellValue("1");
            nurseData.createCell(10).setCellValue("Rld: R5: CS 1: Room CNA");
            nurseData.createCell(11).setCellValue("2");
            nurseData.createCell(12).setCellValue("Room Charge Nurse");
            
            try (FileOutputStream fos = new FileOutputStream(excelFile)) {
                wb.write(fos);
            }
        }
        
        return excelFile;
    }

    @Test
    void clinicalFlowHasDestinationNameParameters() throws Exception {
        File excelFile = createClinicalTestWorkbook();
        
        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(excelFile);
        
        Map<String, Object> clinicalsJson = parser.buildClinicalsJson();
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> flows = (List<Map<String, Object>>) clinicalsJson.get("deliveryFlows");
        assertNotNull(flows);
        assertTrue(flows.size() > 0, "Should have at least one clinical flow");
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> params = (List<Map<String, Object>>) flows.get(0).get("parameterAttributes");
        assertNotNull(params);
        
        // Find destinationName parameters by destinationOrder
        Map<String, Object> dest0 = findParameterWithDestinationOrder(params, "destinationName", 0);
        Map<String, Object> dest1 = findParameterWithDestinationOrder(params, "destinationName", 1);
        Map<String, Object> dest2 = findParameterWithDestinationOrder(params, "destinationName", 2);
        
        assertNotNull(dest0, "Should have destinationName parameter for order 0");
        assertNotNull(dest1, "Should have destinationName parameter for order 1");
        assertNotNull(dest2, "Should have destinationName parameter for order 2");
        
        // Verify the captured functional role names (text after "Room" keyword)
        assertEquals("\"RN\"", dest0.get("value"), "Should capture 'RN' from 'VAssign: Room RN'");
        assertEquals("\"CNA\"", dest1.get("value"), "Should capture 'CNA' from 'Rld: R5: CS 1: Room CNA'");
        assertEquals("\"Charge Nurse\"", dest2.get("value"), "Should capture 'Charge Nurse' from 'Room Charge Nurse'");
        
        // Verify destinationOrder values
        assertEquals(0, dest0.get("destinationOrder"));
        assertEquals(1, dest1.get("destinationOrder"));
        assertEquals(2, dest2.get("destinationOrder"));
    }

    @Test
    void clinicalFlowAlsoHasNoCaregiverParameters() throws Exception {
        File excelFile = createClinicalTestWorkbook();
        
        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(excelFile);
        
        Map<String, Object> clinicalsJson = parser.buildClinicalsJson();
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> flows = (List<Map<String, Object>>) clinicalsJson.get("deliveryFlows");
        assertNotNull(flows);
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> params = (List<Map<String, Object>>) flows.get(0).get("parameterAttributes");
        assertNotNull(params);
        
        // In the test workbook, we have 3 recipients (r1, r2, r3), so NoDeliveries should be at order 3
        int expectedNoCareOrder = 3;
        
        // Verify that Clinical flows still have NoCaregivers parameters
        Map<String, Object> noCareDestName = findParameterWithDestinationOrder(params, "destinationName", expectedNoCareOrder);
        assertNotNull(noCareDestName, "Should have destinationName for NoCaregivers");
        assertEquals("\"NoCaregivers\"", noCareDestName.get("value"), "Should have NoCaregivers as the value");
        
        // Verify other NoCaregivers-related parameters exist
        Map<String, Object> noCareMessage = findParameterWithDestinationOrder(params, "message", expectedNoCareOrder);
        assertNotNull(noCareMessage, "Should have message parameter for NoCaregivers");
        assertTrue(((String) noCareMessage.get("value")).contains("without any caregivers"), 
            "NoCaregivers message should mention 'without any caregivers'");
    }

    @Test
    void clinicalFlowExtractsTextAfterRoomKeyword() throws Exception {
        File excelFile = tempDir.resolve("test-clinical-room-extraction.xlsx").toFile();
        
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            // Create Unit Breakdown sheet
            Sheet unitSheet = wb.createSheet("Unit Breakdown");
            Row headerRow = unitSheet.createRow(2);
            headerRow.createCell(0).setCellValue("Facility");
            headerRow.createCell(1).setCellValue("Common Unit Name");
            headerRow.createCell(2).setCellValue("Nurse Call Configuration Group");
            headerRow.createCell(3).setCellValue("Patient Monitoring Configuration Group");
            
            Row dataRow = unitSheet.createRow(3);
            dataRow.createCell(0).setCellValue("BCH");
            dataRow.createCell(1).setCellValue("ICU");
            dataRow.createCell(2).setCellValue("ICU NC");
            dataRow.createCell(3).setCellValue("ICU PM");
            
            // Create Nurse Call sheet with EMDAN=Y
            Sheet nurseSheet = wb.createSheet("Nurse Call");
            Row nurseHeader = nurseSheet.createRow(2);
            nurseHeader.createCell(0).setCellValue("In scope");
            nurseHeader.createCell(1).setCellValue("Configuration Group");
            nurseHeader.createCell(2).setCellValue("Common Alert or Alarm Name");
            nurseHeader.createCell(5).setCellValue("EMDAN");
            nurseHeader.createCell(8).setCellValue("1st Recipient");
            
            Row nurseData = nurseSheet.createRow(3);
            nurseData.createCell(0).setCellValue("TRUE");
            nurseData.createCell(1).setCellValue("ICU NC");
            nurseData.createCell(2).setCellValue("Heart Rate Alert");
            nurseData.createCell(5).setCellValue("Y");  // EMDAN=Y moves to Clinicals
            nurseData.createCell(8).setCellValue("VAssign:[Room] CNA");  // Special chars around Room keyword
            
            try (FileOutputStream fos = new FileOutputStream(excelFile)) {
                wb.write(fos);
            }
        }
        
        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(excelFile);
        
        Map<String, Object> clinicalsJson = parser.buildClinicalsJson();
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> flows = (List<Map<String, Object>>) clinicalsJson.get("deliveryFlows");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> params = (List<Map<String, Object>>) flows.get(0).get("parameterAttributes");
        
        Map<String, Object> dest0 = findParameterWithDestinationOrder(params, "destinationName", 0);
        
        assertNotNull(dest0, "Should have destinationName for first recipient");
        assertEquals("\"CNA\"", dest0.get("value"), "Should strip special characters and extract 'CNA'");
    }

    // Helper method
    private Map<String, Object> findParameterWithDestinationOrder(List<Map<String, Object>> params, String name, int order) {
        return params.stream()
            .filter(p -> name.equals(p.get("name")) && Integer.valueOf(order).equals(p.get("destinationOrder")))
            .findFirst()
            .orElse(null);
    }
}
