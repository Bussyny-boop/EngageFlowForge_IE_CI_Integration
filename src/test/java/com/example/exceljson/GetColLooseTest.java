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
 * Tests for the getColLoose() method to ensure it properly detects column headers
 * with various punctuation and formatting variations.
 */
class GetColLooseTest {

    /**
     * Create a test workbook with different EMDAN header variations.
     */
    private void createTestWorkbookWithEmdanVariants(File file, String emdanHeader) throws Exception {
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

            // Nurse Call sheet with custom EMDAN header
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
            nurseHeader.createCell(11).setCellValue(emdanHeader);  // Custom EMDAN header
            nurseHeader.createCell(12).setCellValue("Time to 1st Recipient");
            nurseHeader.createCell(13).setCellValue("1st Recipient");

            // Add a test row with EMDAN=Y
            Row nurseData = nurseSheet.createRow(3);
            nurseData.createCell(0).setCellValue("Nurse Group 1");
            nurseData.createCell(1).setCellValue("Bed Exit Call");
            nurseData.createCell(2).setCellValue("System Bed Exit");
            nurseData.createCell(3).setCellValue("Normal");
            nurseData.createCell(4).setCellValue("Badge");
            nurseData.createCell(5).setCellValue("Ringtone 1");
            nurseData.createCell(6).setCellValue("Accept");
            nurseData.createCell(7).setCellValue("No");
            nurseData.createCell(8).setCellValue("");
            nurseData.createCell(9).setCellValue("10");
            nurseData.createCell(10).setCellValue("Yes");
            nurseData.createCell(11).setCellValue("Y");  // EMDAN compliant
            nurseData.createCell(12).setCellValue("0");
            nurseData.createCell(13).setCellValue("vGroup:Test Group");

            // Patient Monitoring sheet
            Sheet clinicalSheet = wb.createSheet("Patient Monitoring");
            Row clinicalHeader = clinicalSheet.createRow(2);
            clinicalHeader.createCell(0).setCellValue("Configuration Group");
            clinicalHeader.createCell(1).setCellValue("Common Alert or Alarm Name");

            try (FileOutputStream fos = new FileOutputStream(file)) {
                wb.write(fos);
            }
        }
    }

    @Test
    void testEmdanHeaderWithParentheses() throws Exception {
        Path tempDir = Files.createTempDirectory("emdan-header-test");
        File excelFile = tempDir.resolve("test.xlsx").toFile();

        // Test with standard header: "EMDAN Compliant? (Y/N)"
        createTestWorkbookWithEmdanVariants(excelFile, "EMDAN Compliant? (Y/N)");

        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(excelFile);

        // Verify: the EMDAN row should be in clinicals
        assertEquals(0, parser.nurseCalls.size(), "EMDAN alarm should be moved from NurseCalls");
        assertEquals(1, parser.clinicals.size(), "EMDAN alarm should be in Clinicals");
        assertEquals("Bed Exit Call", parser.clinicals.getFirst().alarmName, "EMDAN alarm should be in Clinicals");
        assertEquals(1, parser.getEmdanMovedCount(), "Should count 1 EMDAN row moved");
    }

    @Test
    void testEmdanHeaderWithSlashes() throws Exception {
        Path tempDir = Files.createTempDirectory("emdan-header-test");
        File excelFile = tempDir.resolve("test.xlsx").toFile();

        // Test with alternative format: "EMDAN Compliant Y/N"
        createTestWorkbookWithEmdanVariants(excelFile, "EMDAN Compliant Y/N");

        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(excelFile);

        assertEquals(0, parser.nurseCalls.size(), "EMDAN alarm should be moved from NurseCalls");
        assertEquals(1, parser.clinicals.size(), "EMDAN alarm should be in Clinicals");
        assertEquals(1, parser.getEmdanMovedCount(), "Should count 1 EMDAN row moved");
    }

    @Test
    void testEmdanHeaderSimple() throws Exception {
        Path tempDir = Files.createTempDirectory("emdan-header-test");
        File excelFile = tempDir.resolve("test.xlsx").toFile();

        // Test with simple header: "EMDAN"
        createTestWorkbookWithEmdanVariants(excelFile, "EMDAN");

        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(excelFile);

        assertEquals(0, parser.nurseCalls.size(), "EMDAN alarm should be moved from NurseCalls");
        assertEquals(1, parser.clinicals.size(), "EMDAN alarm should be in Clinicals");
        assertEquals(1, parser.getEmdanMovedCount(), "Should count 1 EMDAN row moved");
    }

    @Test
    void testEmdanHeaderLowerCase() throws Exception {
        Path tempDir = Files.createTempDirectory("emdan-header-test");
        File excelFile = tempDir.resolve("test.xlsx").toFile();

        // Test with lowercase: "emdan compliant"
        createTestWorkbookWithEmdanVariants(excelFile, "emdan compliant");

        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(excelFile);

        assertEquals(0, parser.nurseCalls.size(), "EMDAN alarm should be moved from NurseCalls");
        assertEquals(1, parser.clinicals.size(), "EMDAN alarm should be in Clinicals");
        assertEquals(1, parser.getEmdanMovedCount(), "Should count 1 EMDAN row moved");
    }

    @Test
    void testEmdanHeaderWithNewlines() throws Exception {
        Path tempDir = Files.createTempDirectory("emdan-header-test");
        File excelFile = tempDir.resolve("test.xlsx").toFile();

        // Test with newlines: "EMDAN\nCompliant?\n(Y/N)"
        createTestWorkbookWithEmdanVariants(excelFile, "EMDAN\nCompliant?\n(Y/N)");

        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(excelFile);

        assertEquals(0, parser.nurseCalls.size(), "EMDAN alarm should be moved from NurseCalls");
        assertEquals(1, parser.clinicals.size(), "EMDAN alarm should be in Clinicals");
        assertEquals(1, parser.getEmdanMovedCount(), "Should count 1 EMDAN row moved");
    }

    @Test
    void testEmdanHeaderMixedCase() throws Exception {
        Path tempDir = Files.createTempDirectory("emdan-header-test");
        File excelFile = tempDir.resolve("test.xlsx").toFile();

        // Test with mixed case: "EmDaN CoMpLiAnT?"
        createTestWorkbookWithEmdanVariants(excelFile, "EmDaN CoMpLiAnT?");

        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(excelFile);

        assertEquals(0, parser.nurseCalls.size(), "EMDAN alarm should be moved from NurseCalls");
        assertEquals(1, parser.clinicals.size(), "EMDAN alarm should be in Clinicals");
        assertEquals(1, parser.getEmdanMovedCount(), "Should count 1 EMDAN row moved");
    }
}
