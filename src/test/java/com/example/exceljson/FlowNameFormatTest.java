package com.example.exceljson;

import static org.junit.jupiter.api.Assertions.*;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

/**
 * Test to verify the flow name format includes alarm names instead of "(X alarms)" count.
 */
class FlowNameFormatTest {

    @Test
    void mergedFlowNameIncludesAllAlarmNames() throws Exception {
        Path tempDir = Files.createTempDirectory("flow-name-test");
        Path excelPath = tempDir.resolve("input.xlsx");
        Path jsonPath = tempDir.resolve("nurse-merged.json");

        // Create workbook with two alarms having identical delivery parameters
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

            // Nurse Call with two alarms
            Sheet nurseCalls = workbook.createSheet("Nurse call");
            Row nurseHeader = nurseCalls.createRow(2);
            nurseHeader.createCell(0).setCellValue("Configuration Group");
            nurseHeader.createCell(1).setCellValue("Common Alert or Alarm Name");
            nurseHeader.createCell(2).setCellValue("Priority");
            nurseHeader.createCell(3).setCellValue("Device - A");
            nurseHeader.createCell(4).setCellValue("Ringtone Device - A");
            nurseHeader.createCell(5).setCellValue("Time to 1st Recipient");
            nurseHeader.createCell(6).setCellValue("1st Recipient");
            
            // First alarm
            Row nurseRow1 = nurseCalls.createRow(3);
            nurseRow1.createCell(0).setCellValue("TestGroup");
            nurseRow1.createCell(1).setCellValue("Call Button");
            nurseRow1.createCell(2).setCellValue("High");
            nurseRow1.createCell(3).setCellValue("Badge");
            nurseRow1.createCell(4).setCellValue("Tone 1");
            nurseRow1.createCell(5).setCellValue("0");
            nurseRow1.createCell(6).setCellValue("Nurse Team");
            
            // Second alarm (identical delivery)
            Row nurseRow2 = nurseCalls.createRow(4);
            nurseRow2.createCell(0).setCellValue("TestGroup");
            nurseRow2.createCell(1).setCellValue("Emergency");
            nurseRow2.createCell(2).setCellValue("High");
            nurseRow2.createCell(3).setCellValue("Badge");
            nurseRow2.createCell(4).setCellValue("Tone 1");
            nurseRow2.createCell(5).setCellValue("0");
            nurseRow2.createCell(6).setCellValue("Nurse Team");

            // Empty Clinical sheet
            Sheet clinicals = workbook.createSheet("Patient Monitoring");
            Row clinicalHeader = clinicals.createRow(2);
            clinicalHeader.createCell(0).setCellValue("Configuration Group");

            try (OutputStream os = Files.newOutputStream(excelPath)) {
                workbook.write(os);
            }
        }

        // Parse and generate merged JSON
        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(excelPath.toFile());
        parser.writeNurseCallsJson(jsonPath.toFile(), true);

        // Verify the flow name format
        String json = Files.readString(jsonPath);
        
        // Should NOT contain "(2 alarms)"
        assertFalse(json.contains("(2 alarms)"), "Flow name should not use alarm count format");
        
        // Should contain both alarm names
        assertTrue(json.contains("Call Button"), "Flow name should include 'Call Button' alarm name");
        assertTrue(json.contains("Emergency"), "Flow name should include 'Emergency' alarm name");
        
        // Should contain SEND NURSECALL prefix
        assertTrue(json.contains("SEND NURSECALL"), "Flow name should include 'SEND NURSECALL' prefix");
        
        // Flow name should contain alarm names separated by " / "
        assertTrue(json.contains("Call Button / Emergency") || json.contains("Emergency / Call Button"), 
                   "Flow name should list alarm names separated by ' / '");
    }

    @Test
    void mergedClinicalFlowNameIncludesAllAlarmNames() throws Exception {
        Path tempDir = Files.createTempDirectory("clinical-flow-name-test");
        Path excelPath = tempDir.resolve("input.xlsx");
        Path jsonPath = tempDir.resolve("clinical-merged.json");

        // Create workbook with two clinical alarms having identical delivery parameters
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            // Unit Breakdown
            Sheet units = workbook.createSheet("Unit Breakdown");
            Row unitsHeader = units.createRow(2);
            unitsHeader.createCell(0).setCellValue("Facility");
            unitsHeader.createCell(1).setCellValue("Common Unit Name");
            unitsHeader.createCell(2).setCellValue("Patient Monitoring Configuration Group");
            Row unitsRow = units.createRow(3);
            unitsRow.createCell(0).setCellValue("TestFacility");
            unitsRow.createCell(1).setCellValue("TestUnit");
            unitsRow.createCell(2).setCellValue("ClinicalGroup");

            // Empty Nurse Call sheet
            Sheet nurseCalls = workbook.createSheet("Nurse call");
            Row nurseHeader = nurseCalls.createRow(2);
            nurseHeader.createCell(0).setCellValue("Configuration Group");

            // Clinical with two alarms
            Sheet clinicals = workbook.createSheet("Patient Monitoring");
            Row clinicalHeader = clinicals.createRow(2);
            clinicalHeader.createCell(0).setCellValue("Configuration Group");
            clinicalHeader.createCell(1).setCellValue("Common Alert or Alarm Name");
            clinicalHeader.createCell(2).setCellValue("Priority");
            clinicalHeader.createCell(3).setCellValue("Device - A");
            clinicalHeader.createCell(4).setCellValue("Ringtone Device - A");
            clinicalHeader.createCell(5).setCellValue("Time to 1st Recipient");
            clinicalHeader.createCell(6).setCellValue("1st Recipient");
            
            // First alarm
            Row clinicalRow1 = clinicals.createRow(3);
            clinicalRow1.createCell(0).setCellValue("ClinicalGroup");
            clinicalRow1.createCell(1).setCellValue("SpO2 Low");
            clinicalRow1.createCell(2).setCellValue("Medium");
            clinicalRow1.createCell(3).setCellValue("Badge");
            clinicalRow1.createCell(4).setCellValue("Tone 2");
            clinicalRow1.createCell(5).setCellValue("0");
            clinicalRow1.createCell(6).setCellValue("RT Team");
            
            // Second alarm (identical delivery)
            Row clinicalRow2 = clinicals.createRow(4);
            clinicalRow2.createCell(0).setCellValue("ClinicalGroup");
            clinicalRow2.createCell(1).setCellValue("HR High");
            clinicalRow2.createCell(2).setCellValue("Medium");
            clinicalRow2.createCell(3).setCellValue("Badge");
            clinicalRow2.createCell(4).setCellValue("Tone 2");
            clinicalRow2.createCell(5).setCellValue("0");
            clinicalRow2.createCell(6).setCellValue("RT Team");

            try (OutputStream os = Files.newOutputStream(excelPath)) {
                workbook.write(os);
            }
        }

        // Parse and generate merged JSON
        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(excelPath.toFile());
        parser.writeClinicalsJson(jsonPath.toFile(), true);

        // Verify the flow name format
        String json = Files.readString(jsonPath);
        
        // Should NOT contain "(2 alarms)"
        assertFalse(json.contains("(2 alarms)"), "Flow name should not use alarm count format");
        
        // Should contain both alarm names
        assertTrue(json.contains("SpO2 Low"), "Flow name should include 'SpO2 Low' alarm name");
        assertTrue(json.contains("HR High"), "Flow name should include 'HR High' alarm name");
        
        // Should contain SEND CLINICAL prefix
        assertTrue(json.contains("SEND CLINICAL"), "Flow name should include 'SEND CLINICAL' prefix");
        
        // Flow name should contain alarm names separated by " / "
        assertTrue(json.contains("SpO2 Low / HR High") || json.contains("HR High / SpO2 Low"), 
                   "Flow name should list alarm names separated by ' / '");
    }
}
