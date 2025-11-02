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

class NewFieldsTest {

    @Test
    void testEscalateAfterAndTtlFieldsParsing() throws Exception {
        Path tempDir = Files.createTempDirectory("new-fields-test");
        File excelFile = tempDir.resolve("test.xlsx").toFile();

        // Create test workbook with new fields
        createTestWorkbook(excelFile);

        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(excelFile);

        // Verify nurse calls have the new fields
        assertEquals(1, parser.nurseCalls.size(), "Should have 1 nurse call row");
        ExcelParserV5.FlowRow nurseCall = parser.nurseCalls.get(0);
        assertEquals("All declines", nurseCall.escalateAfter, "Escalate After should be parsed");
        assertEquals("15", nurseCall.ttlValue, "TTL Value should be parsed");

        // Verify clinicals have the new fields
        assertEquals(1, parser.clinicals.size(), "Should have 1 clinical row");
        ExcelParserV5.FlowRow clinical = parser.clinicals.get(0);
        assertEquals("1 decline", clinical.escalateAfter, "Escalate After should be parsed for clinicals");
        assertEquals("20", clinical.ttlValue, "TTL Value should be parsed for clinicals");

        // Clean up
        Files.deleteIfExists(excelFile.toPath());
        Files.deleteIfExists(tempDir);
    }

    @Test
    void testNewFieldsInExcelExport() throws Exception {
        Path tempDir = Files.createTempDirectory("export-fields-test");
        File inputFile = tempDir.resolve("input.xlsx").toFile();
        File outputFile = tempDir.resolve("output.xlsx").toFile();

        // Create test workbook
        createTestWorkbook(inputFile);

        // Load and export
        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(inputFile);
        parser.writeExcel(outputFile);

        // Re-load exported file
        ExcelParserV5 parser2 = new ExcelParserV5();
        parser2.load(outputFile);

        // Verify fields are preserved
        assertEquals(1, parser2.nurseCalls.size());
        ExcelParserV5.FlowRow nurseCall = parser2.nurseCalls.get(0);
        assertEquals("All declines", nurseCall.escalateAfter, "Escalate After should be preserved");
        assertEquals("15", nurseCall.ttlValue, "TTL Value should be preserved");

        // Clean up
        Files.deleteIfExists(inputFile.toPath());
        Files.deleteIfExists(outputFile.toPath());
        Files.deleteIfExists(tempDir);
    }

    private void createTestWorkbook(File target) throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            // Unit Breakdown sheet
            Sheet units = workbook.createSheet("Unit Breakdown");
            Row unitsHeader = units.createRow(2);
            unitsHeader.createCell(0).setCellValue("Facility");
            unitsHeader.createCell(1).setCellValue("Common Unit Name");
            unitsHeader.createCell(2).setCellValue("Nurse Call Configuration Group");
            unitsHeader.createCell(3).setCellValue("Patient Monitoring Configuration Group");
            
            Row unitsRow = units.createRow(3);
            unitsRow.createCell(0).setCellValue("Test Facility");
            unitsRow.createCell(1).setCellValue("Test Unit");
            unitsRow.createCell(2).setCellValue("TestGroup");
            unitsRow.createCell(3).setCellValue("TestClinicalGroup");

            // Nurse Call sheet
            Sheet nurseCalls = workbook.createSheet("Nurse Call");
            Row nurseHeader = nurseCalls.createRow(2);
            nurseHeader.createCell(0).setCellValue("Configuration Group");
            nurseHeader.createCell(1).setCellValue("Common Alert or Alarm Name");
            nurseHeader.createCell(2).setCellValue("Sending System Alert Name");
            nurseHeader.createCell(3).setCellValue("Priority");
            nurseHeader.createCell(4).setCellValue("Device - A");
            nurseHeader.createCell(5).setCellValue("Ringtone Device - A");
            nurseHeader.createCell(6).setCellValue("Response Options");
            nurseHeader.createCell(7).setCellValue("Break Through DND");
            nurseHeader.createCell(8).setCellValue("Engage 6.6+: Escalate after all declines or 1 decline");
            nurseHeader.createCell(9).setCellValue("Engage/Edge Display Time (Time to Live)");
            nurseHeader.createCell(10).setCellValue("Time to 1st Recipient");
            nurseHeader.createCell(11).setCellValue("1st Recipient");
            
            Row nurseRow = nurseCalls.createRow(3);
            nurseRow.createCell(0).setCellValue("TestGroup");
            nurseRow.createCell(1).setCellValue("Test Alarm");
            nurseRow.createCell(2).setCellValue("System Alarm");
            nurseRow.createCell(3).setCellValue("High");
            nurseRow.createCell(4).setCellValue("Badge");
            nurseRow.createCell(5).setCellValue("Tone 1");
            nurseRow.createCell(6).setCellValue("Accept");
            nurseRow.createCell(7).setCellValue("Yes");
            nurseRow.createCell(8).setCellValue("All declines");
            nurseRow.createCell(9).setCellValue("15");
            nurseRow.createCell(10).setCellValue("0");
            nurseRow.createCell(11).setCellValue("Nurse Team");

            // Patient Monitoring sheet
            Sheet clinicals = workbook.createSheet("Patient Monitoring");
            Row clinicalHeader = clinicals.createRow(2);
            clinicalHeader.createCell(0).setCellValue("Configuration Group");
            clinicalHeader.createCell(1).setCellValue("Common Alert or Alarm Name");
            clinicalHeader.createCell(2).setCellValue("Sending System Alert Name");
            clinicalHeader.createCell(3).setCellValue("Priority");
            clinicalHeader.createCell(4).setCellValue("Device - A");
            clinicalHeader.createCell(5).setCellValue("Ringtone Device - A");
            clinicalHeader.createCell(6).setCellValue("Response Options");
            clinicalHeader.createCell(7).setCellValue("Break Through DND");
            clinicalHeader.createCell(8).setCellValue("Engage 6.6+: Escalate after all declines or 1 decline");
            clinicalHeader.createCell(9).setCellValue("Time to Live");
            clinicalHeader.createCell(10).setCellValue("Time to 1st Recipient");
            clinicalHeader.createCell(11).setCellValue("1st Recipient");
            
            Row clinicalRow = clinicals.createRow(3);
            clinicalRow.createCell(0).setCellValue("TestClinicalGroup");
            clinicalRow.createCell(1).setCellValue("Clinical Alert");
            clinicalRow.createCell(2).setCellValue("System Clinical Alert");
            clinicalRow.createCell(3).setCellValue("Medium");
            clinicalRow.createCell(4).setCellValue("Badge");
            clinicalRow.createCell(5).setCellValue("Tone 2");
            clinicalRow.createCell(6).setCellValue("Escalate");
            clinicalRow.createCell(7).setCellValue("No");
            clinicalRow.createCell(8).setCellValue("1 decline");
            clinicalRow.createCell(9).setCellValue("20");
            clinicalRow.createCell(10).setCellValue("0");
            clinicalRow.createCell(11).setCellValue("Primary Team");

            try (FileOutputStream fos = new FileOutputStream(target)) {
                workbook.write(fos);
            }
        }
    }
}
