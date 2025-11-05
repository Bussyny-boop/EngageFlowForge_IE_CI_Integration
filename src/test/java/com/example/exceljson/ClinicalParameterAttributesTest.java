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
 * Test class for Clinical flow parameter attributes.
 * Verifies shortMessage and subject parameters include bed number.
 */
class ClinicalParameterAttributesTest {

    @TempDir
    Path tempDir;

    /**
     * Creates a minimal Excel workbook with Patient Monitoring sheet data.
     */
    private File createClinicalTestWorkbook() throws Exception {
        File excelFile = tempDir.resolve("test-clinical.xlsx").toFile();
        
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            // Create Unit Breakdown sheet
            Sheet unitSheet = wb.createSheet("Unit Breakdown");
            Row headerRow = unitSheet.createRow(2);
            headerRow.createCell(0).setCellValue("Facility");
            headerRow.createCell(1).setCellValue("Common Unit Name");
            headerRow.createCell(2).setCellValue("Nurse Call Configuration Group");
            headerRow.createCell(3).setCellValue("Patient Monitoring Configuration Group");
            headerRow.createCell(4).setCellValue("No Caregiver Group");
            
            Row dataRow = unitSheet.createRow(3);
            dataRow.createCell(0).setCellValue("Test Facility");
            dataRow.createCell(1).setCellValue("Test Unit");
            dataRow.createCell(2).setCellValue("Nurse Group 1");
            dataRow.createCell(3).setCellValue("Clinical Group 1");
            dataRow.createCell(4).setCellValue("No Caregiver Group");
            
            // Create Patient Monitoring sheet
            Sheet clinicalSheet = wb.createSheet("Patient Monitoring");
            Row clinicalHeader = clinicalSheet.createRow(2);
            clinicalHeader.createCell(0).setCellValue("In scope");
            clinicalHeader.createCell(1).setCellValue("Configuration Group");
            clinicalHeader.createCell(2).setCellValue("Common Alert or Alarm Name");
            clinicalHeader.createCell(3).setCellValue("Sending System Alert Name");
            clinicalHeader.createCell(4).setCellValue("Priority");
            clinicalHeader.createCell(5).setCellValue("Device - A");
            clinicalHeader.createCell(6).setCellValue("Time to 1st Recipient");
            clinicalHeader.createCell(7).setCellValue("1st Recipient");
            
            Row clinicalData = clinicalSheet.createRow(3);
            clinicalData.createCell(0).setCellValue("TRUE");
            clinicalData.createCell(1).setCellValue("Clinical Group 1");
            clinicalData.createCell(2).setCellValue("SpO2 Alert");
            clinicalData.createCell(3).setCellValue("SpO2 Desaturation");
            clinicalData.createCell(4).setCellValue("Urgent");
            clinicalData.createCell(5).setCellValue("Badge");
            clinicalData.createCell(6).setCellValue("0");
            clinicalData.createCell(7).setCellValue("VAssign:[Room]");
            
            try (FileOutputStream fos = new FileOutputStream(excelFile)) {
                wb.write(fos);
            }
        }
        
        return excelFile;
    }

    @Test
    void clinicalFlowHasShortMessageWithBedNumber() throws Exception {
        File excelFile = createClinicalTestWorkbook();
        
        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(excelFile);
        
        Map<String, Object> clinicalsJson = parser.buildClinicalsJson();
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> flows = (List<Map<String, Object>>) clinicalsJson.get("deliveryFlows");
        assertNotNull(flows);
        assertTrue(flows.size() > 0);
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> params = (List<Map<String, Object>>) flows.get(0).get("parameterAttributes");
        assertNotNull(params);
        
        // Find shortMessage parameter
        String shortMessage = params.stream()
            .filter(p -> "shortMessage".equals(p.get("name")))
            .map(p -> (String) p.get("value"))
            .findFirst()
            .orElse(null);
        
        assertNotNull(shortMessage, "Clinical flow should have shortMessage parameter");
        assertTrue(shortMessage.contains("#{bed.room.name}"), "shortMessage should contain bed.room.name");
        assertTrue(shortMessage.contains("#{bed.bed_number}"), "shortMessage should contain bed.bed_number");
        assertEquals("\"#{alert_type} #{bed.room.name} Bed #{bed.bed_number}\"", shortMessage,
            "shortMessage should have correct format with bed number");
    }

    @Test
    void clinicalFlowHasSubjectWithBedNumber() throws Exception {
        File excelFile = createClinicalTestWorkbook();
        
        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(excelFile);
        
        Map<String, Object> clinicalsJson = parser.buildClinicalsJson();
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> flows = (List<Map<String, Object>>) clinicalsJson.get("deliveryFlows");
        assertNotNull(flows);
        assertTrue(flows.size() > 0);
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> params = (List<Map<String, Object>>) flows.get(0).get("parameterAttributes");
        assertNotNull(params);
        
        // Find subject parameter (non-destinationOrder)
        String subject = params.stream()
            .filter(p -> "subject".equals(p.get("name")) && p.get("destinationOrder") == null)
            .map(p -> (String) p.get("value"))
            .findFirst()
            .orElse(null);
        
        assertNotNull(subject, "Clinical flow should have subject parameter");
        assertTrue(subject.contains("#{bed.room.name}"), "subject should contain bed.room.name");
        assertTrue(subject.contains("#{bed.bed_number}"), "subject should contain bed.bed_number");
        assertEquals("\"#{alert_type} #{bed.room.name} Bed #{bed.bed_number}\"", subject,
            "subject should have correct format with bed number");
    }

    @Test
    void noCaregiverDestinationHasShortMessageWithBedNumber() throws Exception {
        File excelFile = createClinicalTestWorkbook();
        
        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(excelFile);
        
        Map<String, Object> clinicalsJson = parser.buildClinicalsJson();
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> flows = (List<Map<String, Object>>) clinicalsJson.get("deliveryFlows");
        assertNotNull(flows);
        assertTrue(flows.size() > 0);
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> params = (List<Map<String, Object>>) flows.get(0).get("parameterAttributes");
        assertNotNull(params);
        
        // Find NoCaregiver shortMessage parameter (destinationOrder = 1)
        String shortMessage = params.stream()
            .filter(p -> "shortMessage".equals(p.get("name")) && Integer.valueOf(1).equals(p.get("destinationOrder")))
            .map(p -> (String) p.get("value"))
            .findFirst()
            .orElse(null);
        
        assertNotNull(shortMessage, "NoCaregiver destination should have shortMessage parameter");
        assertTrue(shortMessage.contains("NoCaregiver Assigned"), "shortMessage should mention NoCaregiver");
        assertTrue(shortMessage.contains("#{bed.room.name}"), "shortMessage should contain bed.room.name");
        assertTrue(shortMessage.contains("#{bed.bed_number}"), "shortMessage should contain bed.bed_number");
        assertEquals("\"NoCaregiver Assigned for #{alert_type} in #{bed.room.name} Bed #{bed.bed_number}\"", shortMessage,
            "NoCaregiver shortMessage should have correct format");
    }

    @Test
    void noCaregiverDestinationHasSubjectWithBedNumber() throws Exception {
        File excelFile = createClinicalTestWorkbook();
        
        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(excelFile);
        
        Map<String, Object> clinicalsJson = parser.buildClinicalsJson();
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> flows = (List<Map<String, Object>>) clinicalsJson.get("deliveryFlows");
        assertNotNull(flows);
        assertTrue(flows.size() > 0);
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> params = (List<Map<String, Object>>) flows.get(0).get("parameterAttributes");
        assertNotNull(params);
        
        // Find NoCaregiver subject parameter (destinationOrder = 1)
        String subject = params.stream()
            .filter(p -> "subject".equals(p.get("name")) && Integer.valueOf(1).equals(p.get("destinationOrder")))
            .map(p -> (String) p.get("value"))
            .findFirst()
            .orElse(null);
        
        assertNotNull(subject, "NoCaregiver destination should have subject parameter");
        assertTrue(subject.contains("NoCaregiver assigned"), "subject should mention NoCaregiver");
        assertTrue(subject.contains("#{bed.room.name}"), "subject should contain bed.room.name");
        assertTrue(subject.contains("#{bed.bed_number}"), "subject should contain bed.bed_number");
        assertEquals("\"NoCaregiver assigned for #{alert_type} #{bed.room.name} Bed #{bed.bed_number}\"", subject,
            "NoCaregiver subject should have correct format");
    }

    @Test
    void clinicalFlowHasNoDeliveriesDestination() throws Exception {
        File excelFile = createClinicalTestWorkbook();
        
        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(excelFile);
        
        Map<String, Object> clinicalsJson = parser.buildClinicalsJson();
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> flows = (List<Map<String, Object>>) clinicalsJson.get("deliveryFlows");
        assertNotNull(flows);
        assertTrue(flows.size() > 0);
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> destinations = (List<Map<String, Object>>) flows.get(0).get("destinations");
        assertNotNull(destinations);
        
        // Verify that at least one destination has "NoDeliveries" as destinationType
        boolean hasNoDeliveries = destinations.stream()
            .anyMatch(d -> "NoDeliveries".equals(d.get("destinationType")));
        
        assertTrue(hasNoDeliveries, "Clinical flows should contain NoDeliveries destination");
    }
}
