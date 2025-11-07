package com.example.exceljson;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test to verify that "Device - B" field is properly preserved 
 * when exporting and re-importing Excel files.
 */
class DeviceBRoundTripTest {

    @Test
    void testDeviceBPreservedInExportImport(@TempDir Path tempDir) throws Exception {
        File originalFile = tempDir.resolve("original.xlsx").toFile();
        File exportedFile = tempDir.resolve("exported.xlsx").toFile();

        // Create a test workbook with Device - B data
        createTestWorkbookWithDeviceB(originalFile);

        // Load the original file
        ExcelParserV5 parser1 = new ExcelParserV5();
        parser1.load(originalFile);

        // Verify Device B was loaded
        assertEquals(1, parser1.nurseCalls.size(), "Should have one nurse call");
        ExcelParserV5.FlowRow original = parser1.nurseCalls.get(0);
        assertEquals("Edge", original.deviceA, "Device A should be Edge");
        assertEquals("VCS", original.deviceB, "Device B should be VCS");

        // Export the data to a new file
        parser1.writeExcel(exportedFile);

        // Re-import the exported file
        ExcelParserV5 parser2 = new ExcelParserV5();
        parser2.load(exportedFile);

        // Verify Device B is still present after round-trip
        assertEquals(1, parser2.nurseCalls.size(), "Should have one nurse call after round-trip");
        ExcelParserV5.FlowRow roundTrip = parser2.nurseCalls.get(0);
        assertEquals("Edge", roundTrip.deviceA, "Device A should be preserved");
        assertEquals("VCS", roundTrip.deviceB, "Device B should be preserved after round-trip");
        assertEquals("Test Alarm", roundTrip.alarmName, "Alarm name should be preserved");
    }

    @Test
    void testDeviceBHeaderExistsInExportedFile(@TempDir Path tempDir) throws Exception {
        File originalFile = tempDir.resolve("original.xlsx").toFile();
        File exportedFile = tempDir.resolve("exported.xlsx").toFile();

        // Create a test workbook
        createTestWorkbookWithDeviceB(originalFile);

        // Load and export
        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(originalFile);
        parser.writeExcel(exportedFile);

        // Open the exported file and verify "Device - B" header exists
        try (Workbook wb = WorkbookFactory.create(exportedFile)) {
            Sheet nurseSheet = wb.getSheet("Nurse call");
            assertNotNull(nurseSheet, "Nurse call sheet should exist");
            
            Row headerRow = nurseSheet.getRow(0);
            assertNotNull(headerRow, "Header row should exist");

            // Find "Device - B" column
            boolean foundDeviceB = false;
            int deviceBCol = -1;
            for (int i = 0; i < headerRow.getLastCellNum(); i++) {
                Cell cell = headerRow.getCell(i);
                if (cell != null && "Device - B".equals(cell.getStringCellValue())) {
                    foundDeviceB = true;
                    deviceBCol = i;
                    break;
                }
            }

            assertTrue(foundDeviceB, "Device - B header should exist in exported file");
            
            // Verify the data row has Device B value
            Row dataRow = nurseSheet.getRow(1);
            assertNotNull(dataRow, "Data row should exist");
            Cell deviceBCell = dataRow.getCell(deviceBCol);
            assertNotNull(deviceBCell, "Device B cell should exist");
            assertEquals("VCS", deviceBCell.getStringCellValue(), "Device B value should be VCS");
        }
    }

    @Test
    void testDeviceBInClinicalSheet(@TempDir Path tempDir) throws Exception {
        File originalFile = tempDir.resolve("original.xlsx").toFile();
        File exportedFile = tempDir.resolve("exported.xlsx").toFile();

        // Create a test workbook with clinical alarm
        createTestWorkbookWithClinicalDeviceB(originalFile);

        // Load and export
        ExcelParserV5 parser1 = new ExcelParserV5();
        parser1.load(originalFile);
        
        assertEquals(1, parser1.clinicals.size(), "Should have one clinical");
        assertEquals("VMP", parser1.clinicals.get(0).deviceB, "Device B should be VMP");

        parser1.writeExcel(exportedFile);

        // Re-import
        ExcelParserV5 parser2 = new ExcelParserV5();
        parser2.load(exportedFile);

        assertEquals(1, parser2.clinicals.size(), "Should have one clinical after round-trip");
        assertEquals("VMP", parser2.clinicals.get(0).deviceB, "Device B should be preserved in clinicals");
    }

    private void createTestWorkbookWithDeviceB(File file) throws Exception {
        try (Workbook wb = new XSSFWorkbook()) {
            // Create Unit Breakdown sheet
            Sheet unitsSheet = wb.createSheet("Unit Breakdown");
            Row unitHeader = unitsSheet.createRow(0);
            unitHeader.createCell(0).setCellValue("Facility");
            unitHeader.createCell(1).setCellValue("Common Unit Name");
            unitHeader.createCell(2).setCellValue("Nurse Call Configuration Group");
            unitHeader.createCell(3).setCellValue("Patient Monitoring Configuration Group");

            Row unitData = unitsSheet.createRow(1);
            unitData.createCell(0).setCellValue("Test Facility");
            unitData.createCell(1).setCellValue("Unit 1");
            unitData.createCell(2).setCellValue("Config Group 1");
            unitData.createCell(3).setCellValue("");

            // Create Nurse call sheet with Device - B
            Sheet nurseSheet = wb.createSheet("Nurse call");
            Row nurseHeader = nurseSheet.createRow(0);
            nurseHeader.createCell(0).setCellValue("In scope");
            nurseHeader.createCell(1).setCellValue("Configuration Group");
            nurseHeader.createCell(2).setCellValue("Common Alert or Alarm Name");
            nurseHeader.createCell(3).setCellValue("Sending System Alert Name");
            nurseHeader.createCell(4).setCellValue("Priority");
            nurseHeader.createCell(5).setCellValue("Device - A");
            nurseHeader.createCell(6).setCellValue("Device - B");
            nurseHeader.createCell(7).setCellValue("Ringtone Device - A");

            Row nurseData = nurseSheet.createRow(1);
            nurseData.createCell(0).setCellValue("TRUE");
            nurseData.createCell(1).setCellValue("Config Group 1");
            nurseData.createCell(2).setCellValue("Test Alarm");
            nurseData.createCell(3).setCellValue("Test Alarm");
            nurseData.createCell(4).setCellValue("Normal");
            nurseData.createCell(5).setCellValue("Edge");
            nurseData.createCell(6).setCellValue("VCS");
            nurseData.createCell(7).setCellValue("Alert");

            try (FileOutputStream fos = new FileOutputStream(file)) {
                wb.write(fos);
            }
        }
    }

    private void createTestWorkbookWithClinicalDeviceB(File file) throws Exception {
        try (Workbook wb = new XSSFWorkbook()) {
            // Create Unit Breakdown sheet
            Sheet unitsSheet = wb.createSheet("Unit Breakdown");
            Row unitHeader = unitsSheet.createRow(0);
            unitHeader.createCell(0).setCellValue("Facility");
            unitHeader.createCell(1).setCellValue("Common Unit Name");
            unitHeader.createCell(2).setCellValue("Nurse Call Configuration Group");
            unitHeader.createCell(3).setCellValue("Patient Monitoring Configuration Group");

            Row unitData = unitsSheet.createRow(1);
            unitData.createCell(0).setCellValue("Test Facility");
            unitData.createCell(1).setCellValue("Unit 1");
            unitData.createCell(2).setCellValue("");
            unitData.createCell(3).setCellValue("Clinical Config 1");

            // Create Patient Monitoring sheet with Device - B
            Sheet clinicalSheet = wb.createSheet("Patient Monitoring");
            Row clinicalHeader = clinicalSheet.createRow(0);
            clinicalHeader.createCell(0).setCellValue("In scope");
            clinicalHeader.createCell(1).setCellValue("Configuration Group");
            clinicalHeader.createCell(2).setCellValue("Common Alert or Alarm Name");
            clinicalHeader.createCell(3).setCellValue("Sending System Alert Name");
            clinicalHeader.createCell(4).setCellValue("Priority");
            clinicalHeader.createCell(5).setCellValue("Device - A");
            clinicalHeader.createCell(6).setCellValue("Device - B");
            clinicalHeader.createCell(7).setCellValue("Ringtone Device - A");

            Row clinicalData = clinicalSheet.createRow(1);
            clinicalData.createCell(0).setCellValue("TRUE");
            clinicalData.createCell(1).setCellValue("Clinical Config 1");
            clinicalData.createCell(2).setCellValue("Heart Rate Alert");
            clinicalData.createCell(3).setCellValue("HR Alert");
            clinicalData.createCell(4).setCellValue("Urgent");
            clinicalData.createCell(5).setCellValue("VMP");
            clinicalData.createCell(6).setCellValue("VMP");
            clinicalData.createCell(7).setCellValue("Alert");

            try (FileOutputStream fos = new FileOutputStream(file)) {
                wb.write(fos);
            }
        }
    }
}
