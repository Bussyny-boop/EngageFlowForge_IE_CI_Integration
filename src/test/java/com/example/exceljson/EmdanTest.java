package com.example.exceljson;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the EMDAN Compliance Reclassify feature.
 * Verifies that EMDAN values are parsed correctly and rows are reclassified from NurseCalls to Clinicals.
 */
class EmdanTest {

    /**
     * Create a test workbook with Unit Breakdown, Nurse Call, and Patient Monitoring sheets.
     * The Nurse Call sheet includes an EMDAN column.
     */
    private void createTestWorkbook(File file, String emdanValue1, String emdanValue2) throws Exception {
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
            unitData.createCell(0).setCellValue("Test Facility");
            unitData.createCell(1).setCellValue("Test Unit");
            unitData.createCell(2).setCellValue("Nurse Group 1");
            unitData.createCell(3).setCellValue("Clinical Group 1");
            unitData.createCell(4).setCellValue("No Caregiver Group");

            // Nurse Call sheet with EMDAN column
            Sheet nurseSheet = wb.createSheet("Nurse Call");
            Row nurseHeader = nurseSheet.createRow(2);
            nurseHeader.createCell(0).setCellValue("Configuration Group");
            nurseHeader.createCell(1).setCellValue("Common Alert or Alarm Name");
            nurseHeader.createCell(2).setCellValue("Sending System Alert Name");
            nurseHeader.createCell(3).setCellValue("Priority");
            nurseHeader.createCell(4).setCellValue("Device - A");
            nurseHeader.createCell(5).setCellValue("Ringtone Device - A");
            nurseHeader.createCell(6).setCellValue("Response Options");
            nurseHeader.createCell(7).setCellValue("Break Through DND");
            nurseHeader.createCell(8).setCellValue("Engage 6.6+: Escalate after all declines or 1 decline");
            nurseHeader.createCell(9).setCellValue("Engage/Edge Display Time (Time to Live) (Device - A)");
            nurseHeader.createCell(10).setCellValue("Genie Enunciation");
            nurseHeader.createCell(11).setCellValue("EMDAN Compliant? (Y/N)");
            nurseHeader.createCell(12).setCellValue("Time to 1st Recipient");
            nurseHeader.createCell(13).setCellValue("1st Recipient");

            // First row with EMDAN value
            Row nurseData1 = nurseSheet.createRow(3);
            nurseData1.createCell(0).setCellValue("Nurse Group 1");
            nurseData1.createCell(1).setCellValue("Test Alarm 1");
            nurseData1.createCell(2).setCellValue("System Alarm 1");
            nurseData1.createCell(3).setCellValue("Normal");
            nurseData1.createCell(4).setCellValue("Badge");
            nurseData1.createCell(5).setCellValue("Ringtone 1");
            nurseData1.createCell(6).setCellValue("Accept");
            nurseData1.createCell(7).setCellValue("No");
            nurseData1.createCell(8).setCellValue("");
            nurseData1.createCell(9).setCellValue("10");
            nurseData1.createCell(10).setCellValue("Yes");
            nurseData1.createCell(11).setCellValue(emdanValue1);
            nurseData1.createCell(12).setCellValue("0");
            nurseData1.createCell(13).setCellValue("vGroup:Test Group");

            // Second row with different EMDAN value
            Row nurseData2 = nurseSheet.createRow(4);
            nurseData2.createCell(0).setCellValue("Nurse Group 1");
            nurseData2.createCell(1).setCellValue("Test Alarm 2");
            nurseData2.createCell(2).setCellValue("System Alarm 2");
            nurseData2.createCell(3).setCellValue("High");
            nurseData2.createCell(4).setCellValue("Badge");
            nurseData2.createCell(5).setCellValue("Ringtone 2");
            nurseData2.createCell(6).setCellValue("Accept");
            nurseData2.createCell(7).setCellValue("Yes");
            nurseData2.createCell(8).setCellValue("");
            nurseData2.createCell(9).setCellValue("15");
            nurseData2.createCell(10).setCellValue("No");
            nurseData2.createCell(11).setCellValue(emdanValue2);
            nurseData2.createCell(12).setCellValue("0");
            nurseData2.createCell(13).setCellValue("vGroup:Test Group 2");

            // Patient Monitoring sheet (empty for this test)
            Sheet clinicalSheet = wb.createSheet("Patient Monitoring");
            Row clinicalHeader = clinicalSheet.createRow(2);
            clinicalHeader.createCell(0).setCellValue("Configuration Group");
            clinicalHeader.createCell(1).setCellValue("Common Alert or Alarm Name");
            clinicalHeader.createCell(2).setCellValue("EMDAN Compliant? (Y/N)");

            try (FileOutputStream fos = new FileOutputStream(file)) {
                wb.write(fos);
            }
        }
    }

    @Test
    void testEmdanYesMovesToClinicals() throws Exception {
        Path tempDir = Files.createTempDirectory("emdan-yes-test");
        File excelFile = tempDir.resolve("test.xlsx").toFile();

        // Create workbook with one EMDAN=Y and one EMDAN=N
        createTestWorkbook(excelFile, "Y", "N");

        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(excelFile);

        // Verify: the Y alarm should be in clinicals, N alarm should be in nurseCalls
        assertEquals(1, parser.nurseCalls.size(), "One alarm should remain in NurseCalls");
        assertEquals(1, parser.clinicals.size(), "One alarm should be moved to Clinicals");

        // Verify the correct alarms are in the right lists
        assertEquals("Test Alarm 2", parser.nurseCalls.get(0).alarmName, "Non-EMDAN alarm should be in NurseCalls");
        assertEquals("Test Alarm 1", parser.clinicals.get(0).alarmName, "EMDAN alarm should be in Clinicals");

        // Verify EMDAN values are stored
        assertEquals("Y", parser.clinicals.get(0).emdan, "EMDAN value should be stored");
        assertEquals("N", parser.nurseCalls.get(0).emdan, "EMDAN value should be stored");

        // Verify type is set correctly
        assertEquals("Clinicals", parser.clinicals.get(0).type, "Type should be Clinicals for EMDAN alarm");
        assertEquals("NurseCalls", parser.nurseCalls.get(0).type, "Type should be NurseCalls for non-EMDAN alarm");
    }

    @Test
    void testEmdanYesVariant() throws Exception {
        Path tempDir = Files.createTempDirectory("emdan-yes-variant-test");
        File excelFile = tempDir.resolve("test.xlsx").toFile();

        // Test with "Yes" (full word)
        createTestWorkbook(excelFile, "Yes", "No");

        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(excelFile);

        assertEquals(1, parser.nurseCalls.size(), "One alarm should remain in NurseCalls");
        assertEquals(1, parser.clinicals.size(), "One alarm should be moved to Clinicals");
        assertEquals("Test Alarm 1", parser.clinicals.get(0).alarmName, "EMDAN=Yes alarm should be in Clinicals");
    }

    @Test
    void testEmdanBlankStaysInNurseCalls() throws Exception {
        Path tempDir = Files.createTempDirectory("emdan-blank-test");
        File excelFile = tempDir.resolve("test.xlsx").toFile();

        // Test with blank EMDAN values
        createTestWorkbook(excelFile, "", "");

        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(excelFile);

        assertEquals(2, parser.nurseCalls.size(), "Both alarms should remain in NurseCalls when EMDAN is blank");
        assertEquals(0, parser.clinicals.size(), "No alarms should be in Clinicals when EMDAN is blank");
    }

    @Test
    void testEmdanCaseInsensitive() throws Exception {
        Path tempDir = Files.createTempDirectory("emdan-case-test");
        File excelFile = tempDir.resolve("test.xlsx").toFile();

        // Test with mixed case
        createTestWorkbook(excelFile, "y", "YES");

        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(excelFile);

        assertEquals(0, parser.nurseCalls.size(), "No alarms should be in NurseCalls");
        assertEquals(2, parser.clinicals.size(), "Both alarms should be in Clinicals (case insensitive)");
    }

    @Test
    void testEmdanExcelRoundTrip() throws Exception {
        Path tempDir = Files.createTempDirectory("emdan-roundtrip-test");
        File excelFile = tempDir.resolve("test.xlsx").toFile();
        File outputFile = tempDir.resolve("output.xlsx").toFile();

        // Create and load test workbook
        createTestWorkbook(excelFile, "Y", "N");

        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(excelFile);

        // Save to new Excel file
        parser.writeExcel(outputFile);

        // Load the saved file
        ExcelParserV5 parser2 = new ExcelParserV5();
        parser2.load(outputFile);

        // Verify data is preserved
        assertEquals(1, parser2.nurseCalls.size(), "Nurse calls should be preserved");
        assertEquals(1, parser2.clinicals.size(), "Clinicals should be preserved");
        assertEquals("Y", parser2.clinicals.get(0).emdan, "EMDAN value should be preserved in clinicals");
        assertEquals("N", parser2.nurseCalls.get(0).emdan, "EMDAN value should be preserved in nurse calls");
    }
    
    @Test
    void testEmdanMovedCount() throws Exception {
        Path tempDir = Files.createTempDirectory("emdan-count-test");
        File excelFile = tempDir.resolve("test.xlsx").toFile();

        // Create workbook with one EMDAN=Y and one EMDAN=N
        createTestWorkbook(excelFile, "Y", "N");

        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(excelFile);

        // Verify the moved count is tracked correctly
        assertEquals(1, parser.getEmdanMovedCount(), "Should count 1 EMDAN row moved");
    }
    
    @Test
    void testEmdanFacilityResolution() throws Exception {
        Path tempDir = Files.createTempDirectory("emdan-facility-test");
        File excelFile = tempDir.resolve("test.xlsx").toFile();

        // Create workbook with EMDAN=Y alarm
        createTestWorkbook(excelFile, "Y", "N");

        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(excelFile);

        // Verify the EMDAN alarm was moved and facility was resolved
        // The facility should be resolved from the Unit Breakdown mapping
        assertEquals(1, parser.clinicals.size(), "One alarm should be in Clinicals");
        assertEquals("Test Alarm 1", parser.clinicals.get(0).alarmName, "EMDAN alarm should be in Clinicals");
        
        // Verify configuration group is preserved (needed for facility resolution)
        assertEquals("Nurse Group 1", parser.clinicals.get(0).configGroup, 
                    "Configuration group should be preserved for facility resolution");
    }
}
