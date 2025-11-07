package com.example.exceljson;

import org.junit.jupiter.api.Test;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * Manual verification test that demonstrates the Room Filter feature
 * by generating complete JSON outputs.
 */
class RoomFilterManualVerificationTest {

    @Test
    void demonstrateNursecallRoomFilter() throws Exception {
        Path tempDir = Files.createTempDirectory("room-filter-demo");
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
            nurseHeader.createCell(2).setCellValue("Priority");
            nurseHeader.createCell(3).setCellValue("Time to 1st Recipient");
            nurseHeader.createCell(4).setCellValue("1st Recipient");
            
            Row nurseRow = nurseCalls.createRow(3);
            nurseRow.createCell(0).setCellValue("TestGroup");
            nurseRow.createCell(1).setCellValue("Call Button");
            nurseRow.createCell(2).setCellValue("High");
            nurseRow.createCell(3).setCellValue("0");
            nurseRow.createCell(4).setCellValue("Nurse");

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
        parser.setRoomFilters("305", "", "");
        
        Map<String, Object> json = parser.buildNurseCallsJson();
        String prettyJson = ExcelParserV5.pretty(json);
        
        System.out.println("\n=== NURSECALL JSON WITH ROOM FILTER '305' ===");
        System.out.println(prettyJson);
        System.out.println("==============================================\n");
    }

    @Test
    void demonstrateClinicalRoomFilter() throws Exception {
        Path tempDir = Files.createTempDirectory("room-filter-demo");
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
            unitsRow.createCell(1).setCellValue("ICU");
            unitsRow.createCell(3).setCellValue("ClinicalGroup");

            // Empty Nurse Call sheet
            Sheet nurseCalls = workbook.createSheet("Nurse call");
            Row nurseHeader = nurseCalls.createRow(2);
            nurseHeader.createCell(0).setCellValue("Configuration Group");

            // Clinical
            Sheet clinicals = workbook.createSheet("Patient Monitoring");
            Row clinicalHeader = clinicals.createRow(2);
            clinicalHeader.createCell(0).setCellValue("Configuration Group");
            clinicalHeader.createCell(1).setCellValue("Common Alert or Alarm Name");
            clinicalHeader.createCell(2).setCellValue("Priority");
            clinicalHeader.createCell(3).setCellValue("Time to 1st Recipient");
            clinicalHeader.createCell(4).setCellValue("1st Recipient");
            
            Row clinicalRow = clinicals.createRow(3);
            clinicalRow.createCell(0).setCellValue("ClinicalGroup");
            clinicalRow.createCell(1).setCellValue("SpO2 Low");
            clinicalRow.createCell(2).setCellValue("Urgent");
            clinicalRow.createCell(3).setCellValue("0");
            clinicalRow.createCell(4).setCellValue("RT");
            
            // Empty Order sheet
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
        parser.setRoomFilters("", "410", "");
        
        Map<String, Object> json = parser.buildClinicalsJson();
        String prettyJson = ExcelParserV5.pretty(json);
        
        System.out.println("\n=== CLINICAL JSON WITH ROOM FILTER '410' ===");
        System.out.println(prettyJson);
        System.out.println("==============================================\n");
    }

    @Test
    void demonstrateOrdersRoomFilter() throws Exception {
        Path tempDir = Files.createTempDirectory("room-filter-demo");
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
            
            Sheet clinicals = workbook.createSheet("Patient Monitoring");
            Row clinicalHeader = clinicals.createRow(2);
            clinicalHeader.createCell(0).setCellValue("Configuration Group");
            clinicalHeader.createCell(1).setCellValue("Common Alert or Alarm Name");

            // Orders
            Sheet orders = workbook.createSheet("Order");
            Row ordersHeader = orders.createRow(2);
            ordersHeader.createCell(0).setCellValue("Configuration Group");
            ordersHeader.createCell(1).setCellValue("Common Alert or Alarm Name");
            ordersHeader.createCell(2).setCellValue("Priority");
            ordersHeader.createCell(3).setCellValue("Time to 1st Recipient");
            ordersHeader.createCell(4).setCellValue("1st Recipient");
            
            Row ordersRow = orders.createRow(3);
            ordersRow.createCell(0).setCellValue("OrdersGroup");
            ordersRow.createCell(1).setCellValue("New Order");
            ordersRow.createCell(2).setCellValue("High");
            ordersRow.createCell(3).setCellValue("0");
            ordersRow.createCell(4).setCellValue("Pharmacist");

            try (OutputStream os = Files.newOutputStream(excelPath)) {
                workbook.write(os);
            }
        }

        // Test with room filter
        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(excelPath.toFile());
        parser.setRoomFilters("", "", "500");
        
        Map<String, Object> json = parser.buildOrdersJson();
        String prettyJson = ExcelParserV5.pretty(json);
        
        System.out.println("\n=== ORDERS JSON WITH ROOM FILTER '500' ===");
        System.out.println(prettyJson);
        System.out.println("===========================================\n");
    }
}
