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
 * Integration test to verify that the POD Room Filter column
 * can be properly parsed from Excel and edited via the GUI data model.
 */
public class PodRoomFilterIntegrationTest {

    @Test
    public void podRoomFilterRoundTripTest(@TempDir Path tempDir) throws Exception {
        // Create a test Excel workbook with POD Room Filter column
        File excelFile = tempDir.resolve("test-pod-filter.xlsx").toFile();
        
        try (Workbook workbook = new XSSFWorkbook()) {
            // Create Unit Breakdown sheet
            Sheet unitsSheet = workbook.createSheet("Unit Breakdown");
            
            // Create header row
            Row headerRow = unitsSheet.createRow(0);
            headerRow.createCell(0).setCellValue("Facility");
            headerRow.createCell(1).setCellValue("Common Unit Name");
            headerRow.createCell(2).setCellValue("Filter for POD Rooms (Optional)");
            headerRow.createCell(3).setCellValue("Nurse Call Configuration Group");
            headerRow.createCell(4).setCellValue("Clinical Configuration Group");
            headerRow.createCell(5).setCellValue("Orders Configuration Group");
            headerRow.createCell(6).setCellValue("No Caregiver Group");
            headerRow.createCell(7).setCellValue("Comments");
            
            // Create data rows
            Row dataRow1 = unitsSheet.createRow(1);
            dataRow1.createCell(0).setCellValue("Test Hospital");
            dataRow1.createCell(1).setCellValue("ICU");
            dataRow1.createCell(2).setCellValue("POD 1");
            dataRow1.createCell(3).setCellValue("ICU-Nurse");
            dataRow1.createCell(4).setCellValue("ICU-Clinical");
            dataRow1.createCell(5).setCellValue("");
            dataRow1.createCell(6).setCellValue("");
            dataRow1.createCell(7).setCellValue("Test comment");
            
            Row dataRow2 = unitsSheet.createRow(2);
            dataRow2.createCell(0).setCellValue("Test Hospital");
            dataRow2.createCell(1).setCellValue("CCU");
            dataRow2.createCell(2).setCellValue("POD A, POD B");
            dataRow2.createCell(3).setCellValue("CCU-Nurse");
            dataRow2.createCell(4).setCellValue("");
            dataRow2.createCell(5).setCellValue("");
            dataRow2.createCell(6).setCellValue("");
            dataRow2.createCell(7).setCellValue("");
            
            // Create empty Nurse Calls sheet
            Sheet nurseCallsSheet = workbook.createSheet("Nurse Calls");
            Row ncHeaderRow = nurseCallsSheet.createRow(0);
            ncHeaderRow.createCell(0).setCellValue("In Scope");
            ncHeaderRow.createCell(1).setCellValue("Configuration Group");
            ncHeaderRow.createCell(2).setCellValue("Alarm Name");
            
            // Save the workbook
            try (FileOutputStream fos = new FileOutputStream(excelFile)) {
                workbook.write(fos);
            }
        }
        
        // Parse the Excel file
        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(excelFile);
        
        // Verify the POD Room Filter was parsed correctly
        assertEquals(2, parser.units.size(), "Should have 2 units");
        
        ExcelParserV5.UnitRow unit1 = parser.units.get(0);
        assertEquals("Test Hospital", unit1.facility);
        assertEquals("ICU", unit1.unitNames);
        assertEquals("POD 1", unit1.podRoomFilter, "First unit should have POD 1 filter");
        assertEquals("ICU-Nurse", unit1.nurseGroup);
        assertEquals("Test comment", unit1.comments);
        
        ExcelParserV5.UnitRow unit2 = parser.units.get(1);
        assertEquals("Test Hospital", unit2.facility);
        assertEquals("CCU", unit2.unitNames);
        assertEquals("POD A, POD B", unit2.podRoomFilter, "Second unit should have POD A, POD B filter");
        assertEquals("CCU-Nurse", unit2.nurseGroup);
        
        // Simulate GUI editing - modify the POD Room Filter
        unit1.podRoomFilter = "POD 1, POD 2";
        assertEquals("POD 1, POD 2", unit1.podRoomFilter, "Should be able to edit POD filter");
        
        // Clear the filter on unit2
        unit2.podRoomFilter = "";
        assertEquals("", unit2.podRoomFilter, "Should be able to clear POD filter");
    }

    @Test
    public void podRoomFilterWithoutColumnDoesNotBreak(@TempDir Path tempDir) throws Exception {
        // Create a test Excel workbook WITHOUT POD Room Filter column
        File excelFile = tempDir.resolve("test-no-pod-filter.xlsx").toFile();
        
        try (Workbook workbook = new XSSFWorkbook()) {
            // Create Unit Breakdown sheet
            Sheet unitsSheet = workbook.createSheet("Unit Breakdown");
            
            // Create header row WITHOUT POD Room Filter column
            Row headerRow = unitsSheet.createRow(0);
            headerRow.createCell(0).setCellValue("Facility");
            headerRow.createCell(1).setCellValue("Common Unit Name");
            headerRow.createCell(2).setCellValue("Nurse Call Configuration Group");
            headerRow.createCell(3).setCellValue("Clinical Configuration Group");
            
            // Create data row
            Row dataRow = unitsSheet.createRow(1);
            dataRow.createCell(0).setCellValue("Test Hospital");
            dataRow.createCell(1).setCellValue("ICU");
            dataRow.createCell(2).setCellValue("ICU-Nurse");
            dataRow.createCell(3).setCellValue("ICU-Clinical");
            
            // Create empty Nurse Calls sheet
            Sheet nurseCallsSheet = workbook.createSheet("Nurse Calls");
            Row ncHeaderRow = nurseCallsSheet.createRow(0);
            ncHeaderRow.createCell(0).setCellValue("In Scope");
            ncHeaderRow.createCell(1).setCellValue("Configuration Group");
            ncHeaderRow.createCell(2).setCellValue("Alarm Name");
            
            // Save the workbook
            try (FileOutputStream fos = new FileOutputStream(excelFile)) {
                workbook.write(fos);
            }
        }
        
        // Parse the Excel file - should not throw an exception
        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(excelFile);
        
        // Verify the unit was parsed and POD Room Filter defaults to empty
        assertEquals(1, parser.units.size(), "Should have 1 unit");
        
        ExcelParserV5.UnitRow unit = parser.units.get(0);
        assertEquals("Test Hospital", unit.facility);
        assertEquals("ICU", unit.unitNames);
        assertEquals("", unit.podRoomFilter, "POD Room Filter should default to empty when column is missing");
        assertEquals("ICU-Nurse", unit.nurseGroup);
        
        // Should still be able to set a value via GUI
        unit.podRoomFilter = "POD 1";
        assertEquals("POD 1", unit.podRoomFilter, "Should be able to set POD filter even if not in Excel");
    }
}
