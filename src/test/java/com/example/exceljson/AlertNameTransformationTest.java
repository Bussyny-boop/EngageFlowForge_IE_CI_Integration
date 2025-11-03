package com.example.exceljson;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

/**
 * Tests for alert name transformation feature.
 * Verifies that the AlertNameTransformer properly transforms alert names in flow names.
 */
class AlertNameTransformationTest {

    @Test
    void testAlertNameTransformerLoadsConfig() {
        AlertNameTransformer transformer = new AlertNameTransformer();
        
        // Verify that some transformations are loaded from config.yml
        assertNotNull(transformer.getTransformations());
        assertTrue(transformer.getTransformations().size() > 0, 
            "Should have at least one transformation loaded from config.yml");
    }

    @Test
    void testAlertNameTransformation() {
        AlertNameTransformer transformer = new AlertNameTransformer();
        
        // Test a configured transformation (from config.yml)
        String transformed = transformer.transform("Extreme Tachycardic");
        assertEquals("Ext Tachy", transformed, 
            "Should transform 'Extreme Tachycardic' to 'Ext Tachy'");
        
        // Test another configured transformation
        transformed = transformer.transform("SpO2 Desaturation");
        assertEquals("SpO2 Desat", transformed, 
            "Should transform 'SpO2 Desaturation' to 'SpO2 Desat'");
    }

    @Test
    void testAlertNameNoTransformation() {
        AlertNameTransformer transformer = new AlertNameTransformer();
        
        // Test an alert name without a transformation configured
        String original = "Some Unknown Alert";
        String result = transformer.transform(original);
        assertEquals(original, result, 
            "Should return original name when no transformation is configured");
    }

    @Test
    void testMergedFlowNameUsesTransformation() throws Exception {
        Path tempDir = Files.createTempDirectory("alert-transform-test");
        Path excelPath = tempDir.resolve("input.xlsx");
        Path jsonPath = tempDir.resolve("clinical-merged.json");

        createWorkbookWithTransformableAlerts(excelPath);

        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(excelPath.toFile());
        parser.writeClinicalsJson(jsonPath.toFile(), true);

        assertTrue(Files.exists(jsonPath));
        String json = Files.readString(jsonPath);
        
        // Verify the transformed names appear in the flow name
        assertTrue(json.contains("Ext Tachy"), 
            "Flow name should contain transformed 'Ext Tachy' instead of 'Extreme Tachycardic'");
        assertTrue(json.contains("SpO2 Desat"), 
            "Flow name should contain transformed 'SpO2 Desat' instead of 'SpO2 Desaturation'");
        
        // Verify the original names still appear in alarmsAlerts array
        assertTrue(json.contains("Extreme Tachycardic"), 
            "Original 'Extreme Tachycardic' should still appear in alarmsAlerts");
        assertTrue(json.contains("SpO2 Desaturation"), 
            "Original 'SpO2 Desaturation' should still appear in alarmsAlerts");
        
        // Verify "(2 alarms)" pattern does NOT appear
        assertFalse(json.contains("(2 alarms)"), 
            "Flow name should not contain '(2 alarms)' pattern");
    }

    @Test
    void testSingleFlowNameUsesTransformation() throws Exception {
        Path tempDir = Files.createTempDirectory("alert-transform-test");
        Path excelPath = tempDir.resolve("input.xlsx");
        Path jsonPath = tempDir.resolve("clinical-single.json");

        createWorkbookWithTransformableAlerts(excelPath);

        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(excelPath.toFile());
        parser.writeClinicalsJson(jsonPath.toFile(), false);

        assertTrue(Files.exists(jsonPath));
        String json = Files.readString(jsonPath);
        
        // Verify the transformed names appear in the flow names
        assertTrue(json.contains("Ext Tachy"), 
            "Flow name should contain transformed 'Ext Tachy'");
        assertTrue(json.contains("SpO2 Desat"), 
            "Flow name should contain transformed 'SpO2 Desat'");
    }

    @Test
    void testCaseInsensitiveTransformation() {
        AlertNameTransformer transformer = new AlertNameTransformer();
        
        // Test case-insensitive matching
        String result = transformer.transform("extreme tachycardic");
        assertEquals("Ext Tachy", result, 
            "Should transform case-insensitively");
        
        result = transformer.transform("EXTREME TACHYCARDIC");
        assertEquals("Ext Tachy", result, 
            "Should transform case-insensitively");
    }

    /**
     * Creates a workbook with clinical alarms that have transformable names.
     */
    private static void createWorkbookWithTransformableAlerts(Path target) throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            // Unit Breakdown - header at row 2, data starts at row 3
            Sheet units = workbook.createSheet("Unit Breakdown");
            Row unitsHeader = units.createRow(2);
            unitsHeader.createCell(0).setCellValue("Facility");
            unitsHeader.createCell(1).setCellValue("Common Unit Name");
            unitsHeader.createCell(2).setCellValue("Patient Monitoring Configuration Group");
            Row unitsRow = units.createRow(3);
            unitsRow.createCell(0).setCellValue("Test Facility");
            unitsRow.createCell(1).setCellValue("Test Unit");
            unitsRow.createCell(2).setCellValue("TestGroup");

            // Empty Nurse Call sheet
            Sheet nurseCalls = workbook.createSheet("Nurse call");
            Row nurseHeader = nurseCalls.createRow(2);
            nurseHeader.createCell(0).setCellValue("Configuration Group");
            nurseHeader.createCell(1).setCellValue("Alarm Name");

            // Clinical with transformable alert names
            Sheet clinicals = workbook.createSheet("Patient Monitoring");
            Row clinicalHeader = clinicals.createRow(2);
            clinicalHeader.createCell(0).setCellValue("Configuration Group");
            clinicalHeader.createCell(1).setCellValue("Common Alert or Alarm Name");
            clinicalHeader.createCell(2).setCellValue("Priority");
            clinicalHeader.createCell(3).setCellValue("Device - A");
            clinicalHeader.createCell(4).setCellValue("Ringtone Device - A");
            clinicalHeader.createCell(5).setCellValue("Response Options");
            clinicalHeader.createCell(6).setCellValue("Time to 1st Recipient");
            clinicalHeader.createCell(7).setCellValue("1st Recipient");
            
            // First alarm - should be transformed
            Row clinicalRow1 = clinicals.createRow(3);
            clinicalRow1.createCell(0).setCellValue("TestGroup");
            clinicalRow1.createCell(1).setCellValue("Extreme Tachycardic");
            clinicalRow1.createCell(2).setCellValue("Medium");
            clinicalRow1.createCell(3).setCellValue("Badge");
            clinicalRow1.createCell(4).setCellValue("Tone 2");
            clinicalRow1.createCell(5).setCellValue("None");
            clinicalRow1.createCell(6).setCellValue("0");
            clinicalRow1.createCell(7).setCellValue("Primary Team");
            
            // Second alarm - should be transformed (same delivery params for merge)
            Row clinicalRow2 = clinicals.createRow(4);
            clinicalRow2.createCell(0).setCellValue("TestGroup");
            clinicalRow2.createCell(1).setCellValue("SpO2 Desaturation");
            clinicalRow2.createCell(2).setCellValue("Medium");
            clinicalRow2.createCell(3).setCellValue("Badge");
            clinicalRow2.createCell(4).setCellValue("Tone 2");
            clinicalRow2.createCell(5).setCellValue("None");
            clinicalRow2.createCell(6).setCellValue("0");
            clinicalRow2.createCell(7).setCellValue("Primary Team");

            try (OutputStream os = Files.newOutputStream(target)) {
                workbook.write(os);
            }
        }
    }
}
