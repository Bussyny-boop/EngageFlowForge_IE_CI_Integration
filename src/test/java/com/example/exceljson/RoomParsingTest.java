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
 * Tests for Room keyword parsing logic.
 * Verifies that special characters and spaces after "Room" are properly stripped.
 */
class RoomParsingTest {

    @TempDir
    File tempDir;

    @Test
    void testRoomParsingWithBracket() throws Exception {
        File excelFile = new File(tempDir, "test-room-bracket.xlsx");
        
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
            
            // Orders sheet with [Room] Nurse pattern
            Sheet ordersSheet = wb.createSheet("Order");
            Row orderHeader = ordersSheet.createRow(2);
            orderHeader.createCell(0).setCellValue("In scope");
            orderHeader.createCell(1).setCellValue("Configuration Group");
            orderHeader.createCell(2).setCellValue("Common Alert or Alarm Name");
            orderHeader.createCell(7).setCellValue("1st Recipient");
            
            Row orderData = ordersSheet.createRow(3);
            orderData.createCell(0).setCellValue("TRUE");
            orderData.createCell(1).setCellValue("Orders Group 1");
            orderData.createCell(2).setCellValue("Test Alarm");
            orderData.createCell(7).setCellValue("[ROOM] Nurse");
            
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
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> destinations = (List<Map<String, Object>>) flows.get(0).get("destinations");
        assertEquals(1, destinations.size(), "Should have one destination");
        
        @SuppressWarnings("unchecked")
        List<Map<String, String>> functionalRoles = (List<Map<String, String>>) destinations.get(0).get("functionalRoles");
        
        assertNotNull(functionalRoles, "Should have functional roles");
        assertEquals(1, functionalRoles.size(), "Should have one functional role");
        assertEquals("Nurse", functionalRoles.get(0).get("name"), 
            "Should extract 'Nurse' from '[ROOM] Nurse' without bracket or spaces");
    }

    @Test
    void testRoomParsingWithMultipleSpecialChars() throws Exception {
        File excelFile = new File(tempDir, "test-room-special.xlsx");
        
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
            
            // Orders sheet with various special characters after Room
            Sheet ordersSheet = wb.createSheet("Order");
            Row orderHeader = ordersSheet.createRow(2);
            orderHeader.createCell(0).setCellValue("In scope");
            orderHeader.createCell(1).setCellValue("Configuration Group");
            orderHeader.createCell(2).setCellValue("Common Alert or Alarm Name");
            orderHeader.createCell(6).setCellValue("Time to 1st Recipient");
            orderHeader.createCell(7).setCellValue("1st Recipient");
            orderHeader.createCell(8).setCellValue("Time to 2nd Recipient");
            orderHeader.createCell(9).setCellValue("2nd Recipient");
            orderHeader.createCell(10).setCellValue("Time to 3rd Recipient");
            orderHeader.createCell(11).setCellValue("3rd Recipient");
            
            Row orderData = ordersSheet.createRow(3);
            orderData.createCell(0).setCellValue("TRUE");
            orderData.createCell(1).setCellValue("Orders Group 1");
            orderData.createCell(2).setCellValue("Test Alarm");
            orderData.createCell(6).setCellValue("0");
            orderData.createCell(7).setCellValue("Room]  Charge Nurse");
            orderData.createCell(8).setCellValue("1");
            orderData.createCell(9).setCellValue("Room)PCT");
            orderData.createCell(10).setCellValue("2");
            orderData.createCell(11).setCellValue("Room - CNA");
            
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
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> destinations = (List<Map<String, Object>>) flows.get(0).get("destinations");
        assertEquals(3, destinations.size(), "Should have three destinations (one per recipient)");
        
        // Test destination 1: "Room]  Charge Nurse" should extract "Charge Nurse"
        @SuppressWarnings("unchecked")
        List<Map<String, String>> functionalRoles0 = (List<Map<String, String>>) destinations.get(0).get("functionalRoles");
        assertNotNull(functionalRoles0, "Should have functional roles");
        assertEquals(1, functionalRoles0.size(), "Should have one functional role in first destination");
        assertEquals("Charge Nurse", functionalRoles0.get(0).get("name"), 
            "Should extract 'Charge Nurse' from 'Room]  Charge Nurse'");
        
        // Test destination 2: "Room)PCT" should extract "PCT"
        @SuppressWarnings("unchecked")
        List<Map<String, String>> functionalRoles1 = (List<Map<String, String>>) destinations.get(1).get("functionalRoles");
        assertNotNull(functionalRoles1, "Should have functional roles");
        assertEquals(1, functionalRoles1.size(), "Should have one functional role in second destination");
        assertEquals("PCT", functionalRoles1.get(0).get("name"), 
            "Should extract 'PCT' from 'Room)PCT'");
        
        // Test destination 3: "Room - CNA" should extract "CNA"
        @SuppressWarnings("unchecked")
        List<Map<String, String>> functionalRoles2 = (List<Map<String, String>>) destinations.get(2).get("functionalRoles");
        assertNotNull(functionalRoles2, "Should have functional roles");
        assertEquals(1, functionalRoles2.size(), "Should have one functional role in third destination");
        assertEquals("CNA", functionalRoles2.get(0).get("name"), 
            "Should extract 'CNA' from 'Room - CNA'");
    }
}
