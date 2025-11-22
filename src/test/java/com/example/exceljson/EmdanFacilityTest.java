package com.example.exceljson;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests to verify that EMDAN-compliant alarms moved from Nurse Call to Clinicals
 * have their facility names correctly resolved in the generated JSON.
 */
class EmdanFacilityTest {

    /**
     * Test that facility names are correctly populated for functional roles
     * in EMDAN-compliant alarms that were moved from Nurse Call to Clinicals.
     */
    @Test
    void testEmdanFunctionalRoleHasFacilityName() throws Exception {
        Path tempDir = Files.createTempDirectory("emdan-facility-test");
        File excelFile = tempDir.resolve("test.xlsx").toFile();

        // Create test workbook with EMDAN=Y alarm
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            // Unit Breakdown sheet
            Sheet unitSheet = wb.createSheet("Unit Breakdown");
            Row unitHeader = unitSheet.createRow(2);
            unitHeader.createCell(0).setCellValue("Facility");
            unitHeader.createCell(1).setCellValue("Common Unit Name");
            unitHeader.createCell(2).setCellValue("Nurse Call Configuration Group");
            unitHeader.createCell(3).setCellValue("Patient Monitoring Configuration Group");
            unitHeader.createCell(4).setCellValue("No Caregiver Group");

            Row unitData = unitSheet.createRow(3);
            unitData.createCell(0).setCellValue("BCH");
            unitData.createCell(1).setCellValue("MedSurg");
            unitData.createCell(2).setCellValue("General PM");
            unitData.createCell(3).setCellValue("Clinical Group 1");
            unitData.createCell(4).setCellValue("House Supervisor");

            // Nurse Call sheet with EMDAN column
            Sheet nurseSheet = wb.createSheet("Nurse Call");
            Row nurseHeader = nurseSheet.createRow(2);
            nurseHeader.createCell(0).setCellValue("Configuration Group");
            nurseHeader.createCell(1).setCellValue("Common Alert or Alarm Name");
            nurseHeader.createCell(2).setCellValue("Sending System Alert Name");
            nurseHeader.createCell(3).setCellValue("Priority");
            nurseHeader.createCell(4).setCellValue("EMDAN Compliant? (Y/N)");
            nurseHeader.createCell(5).setCellValue("Time to 1st Recipient");
            nurseHeader.createCell(6).setCellValue("1st Recipient");

            Row nurseData1 = nurseSheet.createRow(3);
            nurseData1.createCell(0).setCellValue("General PM");
            nurseData1.createCell(1).setCellValue("SpO2 Desat");
            nurseData1.createCell(2).setCellValue("SpO2 Desat");
            nurseData1.createCell(3).setCellValue("Urgent");
            nurseData1.createCell(4).setCellValue("Y");
            nurseData1.createCell(5).setCellValue("60");
            nurseData1.createCell(6).setCellValue("vAssign:[room] Nurse");

            // Patient Monitoring sheet (empty for this test)
            Sheet clinicalSheet = wb.createSheet("Patient Monitoring");
            Row clinicalHeader = clinicalSheet.createRow(2);
            clinicalHeader.createCell(0).setCellValue("Configuration Group");
            clinicalHeader.createCell(1).setCellValue("Common Alert or Alarm Name");

            try (FileOutputStream fos = new FileOutputStream(excelFile)) {
                wb.write(fos);
            }
        }

        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(excelFile);

        // Verify the alarm was moved to clinicals
        assertEquals(1, parser.clinicals.size(), "EMDAN alarm should be in clinicals");
        assertEquals("SpO2 Desat", parser.clinicals.getFirst().alarmName);
        
        // Build clinicals JSON
        Map<String,Object> json = parser.buildClinicalsJson();
        
        // Extract the delivery flow
        @SuppressWarnings("unchecked")
        List<Map<String,Object>> flows = (List<Map<String,Object>>) json.get("deliveryFlows");
        assertNotNull(flows, "deliveryFlows should not be null");
        assertEquals(1, flows.size(), "Should have 1 delivery flow");
        
        Map<String,Object> flow = flows.getFirst();
        
        // Extract destinations
        @SuppressWarnings("unchecked")
        List<Map<String,Object>> destinations = (List<Map<String,Object>>) flow.get("destinations");
        assertNotNull(destinations, "destinations should not be null");
        assertTrue(destinations.size() > 0, "Should have at least 1 destination");
        
        // Check the first destination (functional role)
        Map<String,Object> destination = destinations.getFirst();
        
        @SuppressWarnings("unchecked")
        List<Map<String,String>> functionalRoles = (List<Map<String,String>>) destination.get("functionalRoles");
        assertNotNull(functionalRoles, "functionalRoles should not be null");
        assertEquals(1, functionalRoles.size(), "Should have 1 functional role");
        
        Map<String,String> role = functionalRoles.getFirst();
        assertEquals("Nurse", role.get("name"), "Role name should be Nurse");
        assertEquals("BCH", role.get("facilityName"), "Facility name should be BCH (not empty)");
        
        // Verify units are also attached
        @SuppressWarnings("unchecked")
        List<Map<String,String>> units = (List<Map<String,String>>) flow.get("units");
        assertNotNull(units, "units should not be null");
        assertEquals(1, units.size(), "Should have 1 unit");
        assertEquals("BCH", units.getFirst().get("facilityName"), "Unit facility should be BCH");
        assertEquals("MedSurg", units.getFirst().get("name"), "Unit name should be MedSurg");
    }

    /**
     * Test that facility names are correctly populated for groups
     * in EMDAN-compliant alarms that were moved from Nurse Call to Clinicals.
     */
    @Test
    void testEmdanGroupHasFacilityName() throws Exception {
        Path tempDir = Files.createTempDirectory("emdan-group-facility-test");
        File excelFile = tempDir.resolve("test.xlsx").toFile();

        // Create test workbook with EMDAN=Y alarm using group recipient
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            // Unit Breakdown sheet
            Sheet unitSheet = wb.createSheet("Unit Breakdown");
            Row unitHeader = unitSheet.createRow(2);
            unitHeader.createCell(0).setCellValue("Facility");
            unitHeader.createCell(1).setCellValue("Common Unit Name");
            unitHeader.createCell(2).setCellValue("Nurse Call Configuration Group");
            unitHeader.createCell(3).setCellValue("Patient Monitoring Configuration Group");
            unitHeader.createCell(4).setCellValue("No Caregiver Group");

            Row unitData = unitSheet.createRow(3);
            unitData.createCell(0).setCellValue("BCH");
            unitData.createCell(1).setCellValue("Acute Care");
            unitData.createCell(2).setCellValue("Acute Care NC");
            unitData.createCell(3).setCellValue("Clinical Group 1");
            unitData.createCell(4).setCellValue("House Supervisor");

            // Nurse Call sheet with EMDAN column
            Sheet nurseSheet = wb.createSheet("Nurse Call");
            Row nurseHeader = nurseSheet.createRow(2);
            nurseHeader.createCell(0).setCellValue("Configuration Group");
            nurseHeader.createCell(1).setCellValue("Common Alert or Alarm Name");
            nurseHeader.createCell(2).setCellValue("EMDAN Compliant? (Y/N)");
            nurseHeader.createCell(3).setCellValue("Time to 1st Recipient");
            nurseHeader.createCell(4).setCellValue("1st Recipient");

            Row nurseData1 = nurseSheet.createRow(3);
            nurseData1.createCell(0).setCellValue("Acute Care NC");
            nurseData1.createCell(1).setCellValue("Bed Exit Call");
            nurseData1.createCell(2).setCellValue("Y");
            nurseData1.createCell(3).setCellValue("0");
            nurseData1.createCell(4).setCellValue("vGroup:Acute Care Nurse");

            // Patient Monitoring sheet (empty for this test)
            Sheet clinicalSheet = wb.createSheet("Patient Monitoring");
            Row clinicalHeader = clinicalSheet.createRow(2);
            clinicalHeader.createCell(0).setCellValue("Configuration Group");
            clinicalHeader.createCell(1).setCellValue("Common Alert or Alarm Name");

            try (FileOutputStream fos = new FileOutputStream(excelFile)) {
                wb.write(fos);
            }
        }

        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(excelFile);
        
        // Build clinicals JSON
        Map<String,Object> json = parser.buildClinicalsJson();
        
        // Extract the delivery flow
        @SuppressWarnings("unchecked")
        List<Map<String,Object>> flows = (List<Map<String,Object>>) json.get("deliveryFlows");
        Map<String,Object> flow = flows.getFirst();
        
        // Extract destinations
        @SuppressWarnings("unchecked")
        List<Map<String,Object>> destinations = (List<Map<String,Object>>) flow.get("destinations");
        Map<String,Object> destination = destinations.getFirst();
        
        @SuppressWarnings("unchecked")
        List<Map<String,String>> groups = (List<Map<String,String>>) destination.get("groups");
        assertNotNull(groups, "groups should not be null");
        assertEquals(1, groups.size(), "Should have 1 group");
        
        Map<String,String> group = groups.getFirst();
        assertEquals("Acute Care Nurse", group.get("name"), "Group name should be Acute Care Nurse");
        assertEquals("BCH", group.get("facilityName"), "Facility name should be BCH (not empty)");
    }
}
