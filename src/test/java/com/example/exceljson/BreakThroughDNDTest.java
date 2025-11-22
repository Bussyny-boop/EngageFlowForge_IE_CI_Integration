package com.example.exceljson;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

/**
 * Tests for Break Through DND feature
 */
class BreakThroughDNDTest {

    @Test
    void breakThroughDNDFromExcelOverridesPriorityLogic() throws Exception {
        String json = generateJsonForBreakThroughDND("High", "none");
        
        assertTrue(json.contains("\"name\": \"breakThrough\""), 
            "Should have breakThrough parameter");
        assertTrue(json.contains("\"value\": \"\\\"none\\\"\""), 
            "Should use Excel value 'none' even though priority is High");
    }

    @Test
    void breakThroughDNDVoceraAndDeviceValue() throws Exception {
        String json = generateJsonForBreakThroughDND("Normal", "voceraAndDevice");
        
        assertTrue(json.contains("\"name\": \"breakThrough\""), 
            "Should have breakThrough parameter");
        assertTrue(json.contains("\"value\": \"\\\"voceraAndDevice\\\"\""), 
            "Should use Excel value 'voceraAndDevice' even though priority is Normal");
    }

    @Test
    void breakThroughDNDDeviceValue() throws Exception {
        String json = generateJsonForBreakThroughDND("Normal", "device");
        
        assertTrue(json.contains("\"name\": \"breakThrough\""), 
            "Should have breakThrough parameter");
        assertTrue(json.contains("\"value\": \"\\\"device\\\"\""), 
            "Should use Excel value 'device'");
    }

    @Test
    void emptyBreakThroughDNDFallsBackToPriorityLogic() throws Exception {
        String json = generateJsonForBreakThroughDND("Urgent", "");
        
        assertTrue(json.contains("\"name\": \"breakThrough\""), 
            "Should have breakThrough parameter");
        assertTrue(json.contains("\"value\": \"\\\"voceraAndDevice\\\"\""), 
            "Empty Break Through DND should fall back to priority-based logic (Urgent -> voceraAndDevice)");
    }

    @Test
    void nullBreakThroughDNDFallsBackToPriorityLogic() throws Exception {
        // When column doesn't exist at all, it will be null/empty
        String json = generateJsonWithoutBreakThroughDND("Normal");
        
        assertTrue(json.contains("\"name\": \"breakThrough\""), 
            "Should have breakThrough parameter");
        assertTrue(json.contains("\"value\": \"\\\"none\\\"\""), 
            "Missing Break Through DND column should fall back to priority-based logic (Normal -> none)");
    }

    @Test
    void clinicalFlowsAlsoSupportBreakThroughDND() throws Exception {
        String json = generateClinicalJsonForBreakThroughDND("High", "voceraAndDevice");
        
        assertTrue(json.contains("\"name\": \"breakThrough\""), 
            "Clinical flows should have breakThrough parameter");
        assertTrue(json.contains("\"value\": \"\\\"voceraAndDevice\\\"\""), 
            "Clinical flows should use Excel value from Break Through DND column");
    }

    @Test
    void breakThroughDNDYesMapsToVoceraAndDevice() throws Exception {
        String json = generateJsonForBreakThroughDND("Normal", "Yes");
        
        assertTrue(json.contains("\"name\": \"breakThrough\""), 
            "Should have breakThrough parameter");
        assertTrue(json.contains("\"value\": \"\\\"voceraAndDevice\\\"\""), 
            "Excel value 'Yes' should map to 'voceraAndDevice'");
    }

    @Test
    void breakThroughDNDYMapsToVoceraAndDevice() throws Exception {
        String json = generateJsonForBreakThroughDND("Normal", "Y");
        
        assertTrue(json.contains("\"name\": \"breakThrough\""), 
            "Should have breakThrough parameter");
        assertTrue(json.contains("\"value\": \"\\\"voceraAndDevice\\\"\""), 
            "Excel value 'Y' should map to 'voceraAndDevice'");
    }

    @Test
    void breakThroughDNDNoMapsToNone() throws Exception {
        String json = generateJsonForBreakThroughDND("Urgent", "No");
        
        assertTrue(json.contains("\"name\": \"breakThrough\""), 
            "Should have breakThrough parameter");
        assertTrue(json.contains("\"value\": \"\\\"none\\\"\""), 
            "Excel value 'No' should map to 'none' even when priority is Urgent");
    }

    @Test
    void breakThroughDNDNMapsToNone() throws Exception {
        String json = generateJsonForBreakThroughDND("Urgent", "N");
        
        assertTrue(json.contains("\"name\": \"breakThrough\""), 
            "Should have breakThrough parameter");
        assertTrue(json.contains("\"value\": \"\\\"none\\\"\""), 
            "Excel value 'N' should map to 'none' even when priority is Urgent");
    }

    @Test
    void breakThroughDNDCaseInsensitive() throws Exception {
        String json1 = generateJsonForBreakThroughDND("Normal", "yes");
        String json2 = generateJsonForBreakThroughDND("Normal", "YES");
        String json3 = generateJsonForBreakThroughDND("Urgent", "no");
        String json4 = generateJsonForBreakThroughDND("Urgent", "NO");
        
        assertTrue(json1.contains("\"value\": \"\\\"voceraAndDevice\\\"\""), 
            "Lowercase 'yes' should map to 'voceraAndDevice'");
        assertTrue(json2.contains("\"value\": \"\\\"voceraAndDevice\\\"\""), 
            "Uppercase 'YES' should map to 'voceraAndDevice'");
        assertTrue(json3.contains("\"value\": \"\\\"none\\\"\""), 
            "Lowercase 'no' should map to 'none'");
        assertTrue(json4.contains("\"value\": \"\\\"none\\\"\""), 
            "Uppercase 'NO' should map to 'none'");
    }

    @Test
    void breakThroughDNDDirectAPIValuesPassThrough() throws Exception {
        // Test that direct API values are normalized and passed through
        String json1 = generateJsonForBreakThroughDND("Normal", "VoceraAndDevice");
        String json2 = generateJsonForBreakThroughDND("Normal", "DEVICE");
        String json3 = generateJsonForBreakThroughDND("Urgent", "NONE");
        
        assertTrue(json1.contains("\"value\": \"\\\"voceraAndDevice\\\"\""), 
            "Direct API value 'VoceraAndDevice' should be normalized to 'voceraAndDevice'");
        assertTrue(json2.contains("\"value\": \"\\\"device\\\"\""), 
            "Direct API value 'DEVICE' should be normalized to 'device'");
        assertTrue(json3.contains("\"value\": \"\\\"none\\\"\""), 
            "Direct API value 'NONE' should be normalized to 'none'");
    }

    @Test
    void breakThroughDNDYesSetsPresenceConfigToDevice() throws Exception {
        String json = generateJsonForBreakThroughDND("Normal", "Yes");
        
        assertTrue(json.contains("\"presenceConfig\": \"device\""), 
            "Excel value 'Yes' should set presenceConfig to 'device'");
    }

    @Test
    void breakThroughDNDYSetsPresenceConfigToDevice() throws Exception {
        String json = generateJsonForBreakThroughDND("Normal", "Y");
        
        assertTrue(json.contains("\"presenceConfig\": \"device\""), 
            "Excel value 'Y' should set presenceConfig to 'device'");
    }

    @Test
    void breakThroughDNDNoSetsPresenceConfigToUserAndDevice() throws Exception {
        String json = generateJsonForBreakThroughDND("Urgent", "No");
        
        assertTrue(json.contains("\"presenceConfig\": \"user_and_device\""), 
            "Excel value 'No' should set presenceConfig to 'user_and_device'");
    }

    @Test
    void breakThroughDNDNSetsPresenceConfigToUserAndDevice() throws Exception {
        String json = generateJsonForBreakThroughDND("Urgent", "N");
        
        assertTrue(json.contains("\"presenceConfig\": \"user_and_device\""), 
            "Excel value 'N' should set presenceConfig to 'user_and_device'");
    }

    @Test
    void breakThroughDNDEmptySetsPresenceConfigToUserAndDevice() throws Exception {
        String json = generateJsonWithoutBreakThroughDND("Normal");
        
        assertTrue(json.contains("\"presenceConfig\": \"user_and_device\""), 
            "Empty Break Through DND should set presenceConfig to 'user_and_device' (default)");
    }

    @Test
    void excelSaveAndLoadPreservesBreakThroughDND() throws Exception {
        Path tempDir = Files.createTempDirectory("breakthrough-test");
        Path excelPath1 = tempDir.resolve("test1.xlsx");
        Path excelPath2 = tempDir.resolve("test2.xlsx");

        try {
            // Create workbook with Break Through DND value
            createWorkbookWithBreakThroughDND(excelPath1, "High", "device");
            
            // Load and save
            ExcelParserV5 parser = new ExcelParserV5();
            parser.load(excelPath1.toFile());
            parser.writeExcel(excelPath2.toFile());
            
            // Load again with a new parser and verify value is preserved
            ExcelParserV5 parser2 = new ExcelParserV5();
            parser2.load(excelPath2.toFile());
            
            assertEquals(1, parser2.nurseCalls.size(), "Should have one nurse call");
            assertEquals("device", parser2.nurseCalls.getFirst().breakThroughDND, 
                "Break Through DND value should be preserved after save/load cycle");
        } finally {
            Files.deleteIfExists(excelPath1);
            Files.deleteIfExists(excelPath2);
            Files.deleteIfExists(tempDir);
        }
    }

    // Helper methods

    private String generateJsonForBreakThroughDND(String priority, String breakThroughDND) throws Exception {
        Path tempDir = Files.createTempDirectory("breakthrough-test");
        Path excelPath = tempDir.resolve("test.xlsx");
        Path jsonPath = tempDir.resolve("NurseCalls.json");

        try {
            createWorkbookWithBreakThroughDND(excelPath, priority, breakThroughDND);
            
            ExcelParserV5 parser = new ExcelParserV5();
            parser.load(excelPath.toFile());
            parser.writeNurseCallsJson(jsonPath.toFile());

            assertTrue(Files.exists(jsonPath), "NurseCalls.json should exist");
            
            return Files.readString(jsonPath);
        } finally {
            Files.deleteIfExists(excelPath);
            Files.deleteIfExists(jsonPath);
            Files.deleteIfExists(tempDir);
        }
    }

    private String generateJsonWithoutBreakThroughDND(String priority) throws Exception {
        Path tempDir = Files.createTempDirectory("breakthrough-test");
        Path excelPath = tempDir.resolve("test.xlsx");
        Path jsonPath = tempDir.resolve("NurseCalls.json");

        try {
            createWorkbookWithoutBreakThroughDND(excelPath, priority);
            
            ExcelParserV5 parser = new ExcelParserV5();
            parser.load(excelPath.toFile());
            parser.writeNurseCallsJson(jsonPath.toFile());

            assertTrue(Files.exists(jsonPath), "NurseCalls.json should exist");
            
            return Files.readString(jsonPath);
        } finally {
            Files.deleteIfExists(excelPath);
            Files.deleteIfExists(jsonPath);
            Files.deleteIfExists(tempDir);
        }
    }

    private String generateClinicalJsonForBreakThroughDND(String priority, String breakThroughDND) throws Exception {
        Path tempDir = Files.createTempDirectory("breakthrough-test");
        Path excelPath = tempDir.resolve("test.xlsx");
        Path jsonPath = tempDir.resolve("Clinicals.json");

        try {
            createClinicalWorkbookWithBreakThroughDND(excelPath, priority, breakThroughDND);
            
            ExcelParserV5 parser = new ExcelParserV5();
            parser.load(excelPath.toFile());
            parser.writeClinicalsJson(jsonPath.toFile());

            assertTrue(Files.exists(jsonPath), "Clinicals.json should exist");
            
            return Files.readString(jsonPath);
        } finally {
            Files.deleteIfExists(excelPath);
            Files.deleteIfExists(jsonPath);
            Files.deleteIfExists(tempDir);
        }
    }

    private static void createWorkbookWithBreakThroughDND(Path target, String priority, String breakThroughDND) throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            // Unit Breakdown sheet
            Sheet units = workbook.createSheet("Unit Breakdown");
            Row unitsHeader = units.createRow(0);
            unitsHeader.createCell(0).setCellValue("Facility");
            unitsHeader.createCell(2).setCellValue("Common Unit Name");
            unitsHeader.createCell(3).setCellValue("Nurse Call Configuration Group");
            Row unitsRow = units.createRow(1);
            unitsRow.createCell(0).setCellValue("Test Facility");
            unitsRow.createCell(2).setCellValue("Test Unit");
            unitsRow.createCell(3).setCellValue("TestGroup");

            // Nurse Call sheet with Break Through DND column
            Sheet nurseCalls = workbook.createSheet("Nurse Call");
            Row nurseHeader = nurseCalls.createRow(0);
            nurseHeader.createCell(0).setCellValue("Configuration Group");
            nurseHeader.createCell(1).setCellValue("Common Alert or Alarm Name");
            nurseHeader.createCell(2).setCellValue("Sending System Alert Name");
            nurseHeader.createCell(3).setCellValue("Priority");
            nurseHeader.createCell(4).setCellValue("Device - A");
            nurseHeader.createCell(5).setCellValue("Ringtone Device - A");
            nurseHeader.createCell(6).setCellValue("Response Options");
            nurseHeader.createCell(7).setCellValue("Break Through DND");
            nurseHeader.createCell(8).setCellValue("Time to 1st Recipient");
            nurseHeader.createCell(9).setCellValue("1st Recipient");
            
            Row nurseRow = nurseCalls.createRow(1);
            nurseRow.createCell(0).setCellValue("TestGroup");
            nurseRow.createCell(1).setCellValue("Test Alarm");
            nurseRow.createCell(2).setCellValue("");
            nurseRow.createCell(3).setCellValue(priority);
            nurseRow.createCell(4).setCellValue("Badge");
            nurseRow.createCell(5).setCellValue("Test Ringtone");
            nurseRow.createCell(6).setCellValue("Accept");
            nurseRow.createCell(7).setCellValue(breakThroughDND);
            nurseRow.createCell(8).setCellValue("0");
            nurseRow.createCell(9).setCellValue("Nurse Team");

            try (OutputStream os = Files.newOutputStream(target)) {
                workbook.write(os);
            }
        }
    }

    private static void createWorkbookWithoutBreakThroughDND(Path target, String priority) throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            // Unit Breakdown sheet
            Sheet units = workbook.createSheet("Unit Breakdown");
            Row unitsHeader = units.createRow(0);
            unitsHeader.createCell(0).setCellValue("Facility");
            unitsHeader.createCell(2).setCellValue("Common Unit Name");
            unitsHeader.createCell(3).setCellValue("Nurse Call Configuration Group");
            Row unitsRow = units.createRow(1);
            unitsRow.createCell(0).setCellValue("Test Facility");
            unitsRow.createCell(2).setCellValue("Test Unit");
            unitsRow.createCell(3).setCellValue("TestGroup");

            // Nurse Call sheet WITHOUT Break Through DND column
            Sheet nurseCalls = workbook.createSheet("Nurse Call");
            Row nurseHeader = nurseCalls.createRow(0);
            nurseHeader.createCell(0).setCellValue("Configuration Group");
            nurseHeader.createCell(1).setCellValue("Common Alert or Alarm Name");
            nurseHeader.createCell(2).setCellValue("Sending System Alert Name");
            nurseHeader.createCell(3).setCellValue("Priority");
            nurseHeader.createCell(4).setCellValue("Device - A");
            nurseHeader.createCell(5).setCellValue("Ringtone Device - A");
            nurseHeader.createCell(6).setCellValue("Response Options");
            nurseHeader.createCell(7).setCellValue("Time to 1st Recipient");
            nurseHeader.createCell(8).setCellValue("1st Recipient");
            
            Row nurseRow = nurseCalls.createRow(1);
            nurseRow.createCell(0).setCellValue("TestGroup");
            nurseRow.createCell(1).setCellValue("Test Alarm");
            nurseRow.createCell(2).setCellValue("");
            nurseRow.createCell(3).setCellValue(priority);
            nurseRow.createCell(4).setCellValue("Badge");
            nurseRow.createCell(5).setCellValue("Test Ringtone");
            nurseRow.createCell(6).setCellValue("Accept");
            nurseRow.createCell(7).setCellValue("0");
            nurseRow.createCell(8).setCellValue("Nurse Team");

            try (OutputStream os = Files.newOutputStream(target)) {
                workbook.write(os);
            }
        }
    }

    private static void createClinicalWorkbookWithBreakThroughDND(Path target, String priority, String breakThroughDND) throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            // Unit Breakdown sheet
            Sheet units = workbook.createSheet("Unit Breakdown");
            Row unitsHeader = units.createRow(0);
            unitsHeader.createCell(0).setCellValue("Facility");
            unitsHeader.createCell(2).setCellValue("Common Unit Name");
            unitsHeader.createCell(4).setCellValue("Patient Monitoring Configuration Group");
            Row unitsRow = units.createRow(1);
            unitsRow.createCell(0).setCellValue("Test Facility");
            unitsRow.createCell(2).setCellValue("Test Unit");
            unitsRow.createCell(4).setCellValue("TestGroup");

            // Patient Monitoring sheet with Break Through DND column
            Sheet clinicals = workbook.createSheet("Patient Monitoring");
            Row clinicalHeader = clinicals.createRow(0);
            clinicalHeader.createCell(0).setCellValue("Configuration Group");
            clinicalHeader.createCell(1).setCellValue("Common Alert or Alarm Name");
            clinicalHeader.createCell(2).setCellValue("Sending System Alert Name");
            clinicalHeader.createCell(3).setCellValue("Priority");
            clinicalHeader.createCell(4).setCellValue("Device - A");
            clinicalHeader.createCell(5).setCellValue("Ringtone Device - A");
            clinicalHeader.createCell(6).setCellValue("Response Options");
            clinicalHeader.createCell(7).setCellValue("Break Through DND");
            clinicalHeader.createCell(8).setCellValue("Time to 1st Recipient");
            clinicalHeader.createCell(9).setCellValue("1st Recipient");
            
            Row clinicalRow = clinicals.createRow(1);
            clinicalRow.createCell(0).setCellValue("TestGroup");
            clinicalRow.createCell(1).setCellValue("Test Clinical Alert");
            clinicalRow.createCell(2).setCellValue("");
            clinicalRow.createCell(3).setCellValue(priority);
            clinicalRow.createCell(4).setCellValue("Badge");
            clinicalRow.createCell(5).setCellValue("Test Ringtone");
            clinicalRow.createCell(6).setCellValue("Accept");
            clinicalRow.createCell(7).setCellValue(breakThroughDND);
            clinicalRow.createCell(8).setCellValue("0");
            clinicalRow.createCell(9).setCellValue("Nurse Team");

            try (OutputStream os = Files.newOutputStream(target)) {
                workbook.write(os);
            }
        }
    }
}
