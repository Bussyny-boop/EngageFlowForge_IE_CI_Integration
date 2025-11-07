package com.example.exceljson;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the Genie Enunciation feature.
 * Verifies that enunciation values are parsed correctly from Excel and converted to boolean in JSON.
 */
class EnunciationTest {

    /**
     * Helper method to find and verify the enunciate parameter value in a list of parameter attributes.
     */
    private void assertEnunciateValue(List<?> params, String expectedValue, String message) {
        boolean foundEnunciate = false;
        for (Object param : params) {
            var paramMap = (Map<?, ?>) param;
            if ("enunciate".equals(paramMap.get("name"))) {
                assertEquals(expectedValue, paramMap.get("value"), message);
                foundEnunciate = true;
                break;
            }
        }
        assertTrue(foundEnunciate, "Enunciate parameter should be present");
    }

    @Test
    void testEnunciateYesBecomesTrue() throws Exception {
        Path tempDir = Files.createTempDirectory("enunciate-yes-test");
        File excelFile = tempDir.resolve("test.xlsx").toFile();

        createTestWorkbookWithEnunciate(excelFile, "Yes");

        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(excelFile);

        // Build JSON for nurse calls
        var nurseJson = parser.buildNurseCallsJson();
        var flows = (List<?>) nurseJson.get("deliveryFlows");
        var flow = (Map<?, ?>) flows.get(0);
        var params = (List<?>) flow.get("parameterAttributes");

        // Verify enunciate is true (not quoted)
        assertEnunciateValue(params, "true", "Enunciate should be true for 'Yes'");

        Files.deleteIfExists(excelFile.toPath());
        Files.deleteIfExists(tempDir);
    }

    @Test
    void testEnunciateYLowercaseBecomesTrue() throws Exception {
        Path tempDir = Files.createTempDirectory("enunciate-y-test");
        File excelFile = tempDir.resolve("test.xlsx").toFile();

        createTestWorkbookWithEnunciate(excelFile, "y");

        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(excelFile);

        var nurseJson = parser.buildNurseCallsJson();
        var flows = (List<?>) nurseJson.get("deliveryFlows");
        var flow = (Map<?, ?>) flows.get(0);
        var params = (List<?>) flow.get("parameterAttributes");

        assertEnunciateValue(params, "true", "Enunciate should be true for 'y'");

        Files.deleteIfExists(excelFile.toPath());
        Files.deleteIfExists(tempDir);
    }

    @Test
    void testEnunciateEnunciateBecomesTrue() throws Exception {
        Path tempDir = Files.createTempDirectory("enunciate-enunciate-test");
        File excelFile = tempDir.resolve("test.xlsx").toFile();

        createTestWorkbookWithEnunciate(excelFile, "Enunciate");

        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(excelFile);

        var nurseJson = parser.buildNurseCallsJson();
        var flows = (List<?>) nurseJson.get("deliveryFlows");
        var flow = (Map<?, ?>) flows.get(0);
        var params = (List<?>) flow.get("parameterAttributes");

        assertEnunciateValue(params, "true", "Enunciate should be true for 'Enunciate'");

        Files.deleteIfExists(excelFile.toPath());
        Files.deleteIfExists(tempDir);
    }

    @Test
    void testEnunciateEnunciationBecomesTrue() throws Exception {
        Path tempDir = Files.createTempDirectory("enunciate-enunciation-test");
        File excelFile = tempDir.resolve("test.xlsx").toFile();

        createTestWorkbookWithEnunciate(excelFile, "Enunciation");

        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(excelFile);

        var nurseJson = parser.buildNurseCallsJson();
        var flows = (List<?>) nurseJson.get("deliveryFlows");
        var flow = (Map<?, ?>) flows.get(0);
        var params = (List<?>) flow.get("parameterAttributes");

        assertEnunciateValue(params, "true", "Enunciate should be true for 'Enunciation'");

        Files.deleteIfExists(excelFile.toPath());
        Files.deleteIfExists(tempDir);
    }

    @Test
    void testEnunciateTrueBecomesTrue() throws Exception {
        Path tempDir = Files.createTempDirectory("enunciate-true-test");
        File excelFile = tempDir.resolve("test.xlsx").toFile();

        createTestWorkbookWithEnunciate(excelFile, "True");

        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(excelFile);

        var nurseJson = parser.buildNurseCallsJson();
        var flows = (List<?>) nurseJson.get("deliveryFlows");
        var flow = (Map<?, ?>) flows.get(0);
        var params = (List<?>) flow.get("parameterAttributes");

        assertEnunciateValue(params, "true", "Enunciate should be true for 'True'");

        Files.deleteIfExists(excelFile.toPath());
        Files.deleteIfExists(tempDir);
    }

    @Test
    void testEnunciateNoBecomesFalse() throws Exception {
        Path tempDir = Files.createTempDirectory("enunciate-no-test");
        File excelFile = tempDir.resolve("test.xlsx").toFile();

        createTestWorkbookWithEnunciate(excelFile, "No");

        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(excelFile);

        var nurseJson = parser.buildNurseCallsJson();
        var flows = (List<?>) nurseJson.get("deliveryFlows");
        var flow = (Map<?, ?>) flows.get(0);
        var params = (List<?>) flow.get("parameterAttributes");

        assertEnunciateValue(params, "false", "Enunciate should be false for 'No'");

        Files.deleteIfExists(excelFile.toPath());
        Files.deleteIfExists(tempDir);
    }

    @Test
    void testEnunciateNBecomesFalse() throws Exception {
        Path tempDir = Files.createTempDirectory("enunciate-n-test");
        File excelFile = tempDir.resolve("test.xlsx").toFile();

        createTestWorkbookWithEnunciate(excelFile, "N");

        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(excelFile);

        var nurseJson = parser.buildNurseCallsJson();
        var flows = (List<?>) nurseJson.get("deliveryFlows");
        var flow = (Map<?, ?>) flows.get(0);
        var params = (List<?>) flow.get("parameterAttributes");

        assertEnunciateValue(params, "false", "Enunciate should be false for 'N'");

        Files.deleteIfExists(excelFile.toPath());
        Files.deleteIfExists(tempDir);
    }

    @Test
    void testEnunciateFalseBecomesFalse() throws Exception {
        Path tempDir = Files.createTempDirectory("enunciate-false-test");
        File excelFile = tempDir.resolve("test.xlsx").toFile();

        createTestWorkbookWithEnunciate(excelFile, "False");

        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(excelFile);

        var nurseJson = parser.buildNurseCallsJson();
        var flows = (List<?>) nurseJson.get("deliveryFlows");
        var flow = (Map<?, ?>) flows.get(0);
        var params = (List<?>) flow.get("parameterAttributes");

        assertEnunciateValue(params, "false", "Enunciate should be false for 'False'");

        Files.deleteIfExists(excelFile.toPath());
        Files.deleteIfExists(tempDir);
    }

    @Test
    void testEnunciateBlankDefaultsToTrue() throws Exception {
        Path tempDir = Files.createTempDirectory("enunciate-blank-test");
        File excelFile = tempDir.resolve("test.xlsx").toFile();

        createTestWorkbookWithEnunciate(excelFile, "");

        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(excelFile);

        var nurseJson = parser.buildNurseCallsJson();
        var flows = (List<?>) nurseJson.get("deliveryFlows");
        var flow = (Map<?, ?>) flows.get(0);
        var params = (List<?>) flow.get("parameterAttributes");

        assertEnunciateValue(params, "true", "Enunciate should default to true when blank");

        Files.deleteIfExists(excelFile.toPath());
        Files.deleteIfExists(tempDir);
    }

    @Test
    void testEnunciateFieldParsing() throws Exception {
        Path tempDir = Files.createTempDirectory("enunciate-parse-test");
        File excelFile = tempDir.resolve("test.xlsx").toFile();

        createTestWorkbookWithEnunciate(excelFile, "Yes");

        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(excelFile);

        // Verify the field is parsed correctly
        assertEquals(1, parser.nurseCalls.size(), "Should have 1 nurse call row");
        ExcelParserV5.FlowRow nurseCall = parser.nurseCalls.get(0);
        assertEquals("Yes", nurseCall.enunciate, "Enunciate field should be parsed as 'Yes'");

        Files.deleteIfExists(excelFile.toPath());
        Files.deleteIfExists(tempDir);
    }

    @Test
    void testEnunciatePreservedInExcelExport() throws Exception {
        Path tempDir = Files.createTempDirectory("enunciate-export-test");
        File inputFile = tempDir.resolve("input.xlsx").toFile();
        File outputFile = tempDir.resolve("output.xlsx").toFile();

        createTestWorkbookWithEnunciate(inputFile, "Yes");

        // Load and export
        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(inputFile);
        parser.writeExcel(outputFile);

        // Re-load exported file
        ExcelParserV5 parser2 = new ExcelParserV5();
        parser2.load(outputFile);

        // Verify field is preserved
        assertEquals(1, parser2.nurseCalls.size());
        ExcelParserV5.FlowRow nurseCall = parser2.nurseCalls.get(0);
        assertEquals("Yes", nurseCall.enunciate, "Enunciate field should be preserved in export");

        Files.deleteIfExists(inputFile.toPath());
        Files.deleteIfExists(outputFile.toPath());
        Files.deleteIfExists(tempDir);
    }

    @Test
    void testEnunciateColumnVariants() throws Exception {
        // Test different column name variants that should all be found
        String[] columnVariants = {
            "Genie Enunciation",
            "Phone: Alert Display / Genie Enunciation (if badge) B",
            "Badge Genie Enunciation",
            "Alert Genie Enunciation Display"
        };

        for (String columnName : columnVariants) {
            Path tempDir = Files.createTempDirectory("enunciate-variant-test");
            File excelFile = tempDir.resolve("test.xlsx").toFile();

            createTestWorkbookWithCustomEnunciateColumn(excelFile, columnName, "Yes");

            ExcelParserV5 parser = new ExcelParserV5();
            parser.load(excelFile);

            assertEquals(1, parser.nurseCalls.size(), "Should parse row with column: " + columnName);
            ExcelParserV5.FlowRow nurseCall = parser.nurseCalls.get(0);
            assertEquals("Yes", nurseCall.enunciate, 
                "Should parse enunciate from column: " + columnName);

            Files.deleteIfExists(excelFile.toPath());
            Files.deleteIfExists(tempDir);
        }
    }

    @Test
    void testEnunciateCaseInsensitive() throws Exception {
        // Test that the values are case-insensitive
        String[] trueValues = {"YES", "Yes", "yes", "Y", "y", "ENUNCIATE", "Enunciate", 
                               "ENUNCIATION", "Enunciation", "TRUE", "True"};
        
        for (String value : trueValues) {
            Path tempDir = Files.createTempDirectory("enunciate-case-test");
            File excelFile = tempDir.resolve("test.xlsx").toFile();

            createTestWorkbookWithEnunciate(excelFile, value);

            ExcelParserV5 parser = new ExcelParserV5();
            parser.load(excelFile);

            var nurseJson = parser.buildNurseCallsJson();
            var flows = (List<?>) nurseJson.get("deliveryFlows");
            var flow = (Map<?, ?>) flows.get(0);
            var params = (List<?>) flow.get("parameterAttributes");

            assertEnunciateValue(params, "true", 
                "Enunciate should be true for value: '" + value + "'");

            Files.deleteIfExists(excelFile.toPath());
            Files.deleteIfExists(tempDir);
        }
    }

    private void createTestWorkbookWithEnunciate(File target, String enunciateValue) throws Exception {
        createTestWorkbookWithCustomEnunciateColumn(target, "Genie Enunciation", enunciateValue);
    }

    private void createTestWorkbookWithCustomEnunciateColumn(File target, String columnName, 
                                                             String enunciateValue) throws Exception {
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
            nurseHeader.createCell(9).setCellValue("Engage/Edge Display Time (Time to Live) (Device - A)");
            nurseHeader.createCell(10).setCellValue(columnName);  // Use custom column name
            nurseHeader.createCell(11).setCellValue("Time to 1st Recipient");
            nurseHeader.createCell(12).setCellValue("1st Recipient");
            
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
            nurseRow.createCell(10).setCellValue(enunciateValue);  // Enunciate value
            nurseRow.createCell(11).setCellValue("0");
            nurseRow.createCell(12).setCellValue("Nurse Team");

            // Patient Monitoring sheet (minimal)
            Sheet clinicals = workbook.createSheet("Patient Monitoring");
            Row clinicalHeader = clinicals.createRow(2);
            clinicalHeader.createCell(0).setCellValue("Configuration Group");
            clinicalHeader.createCell(1).setCellValue("Common Alert or Alarm Name");

            try (FileOutputStream fos = new FileOutputStream(target)) {
                workbook.write(fos);
            }
        }
    }

    @Test
    void testEnunciateColumnFallbackBehavior() throws Exception {
        // Test that the fallback to exact column name works
        // This test uses "Genie Enunciation" which should be found by the substring search
        // but also validates that the fallback logic is in place
        Path tempDir = Files.createTempDirectory("enunciate-fallback-test");
        File excelFile = tempDir.resolve("test.xlsx").toFile();

        createTestWorkbookWithEnunciate(excelFile, "Yes");

        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(excelFile);

        assertEquals(1, parser.nurseCalls.size(), "Should parse row with Genie Enunciation column");
        ExcelParserV5.FlowRow nurseCall = parser.nurseCalls.get(0);
        assertEquals("Yes", nurseCall.enunciate, "Should parse enunciate value");

        var nurseJson = parser.buildNurseCallsJson();
        var flows = (List<?>) nurseJson.get("deliveryFlows");
        var flow = (Map<?, ?>) flows.get(0);
        var params = (List<?>) flow.get("parameterAttributes");

        assertEnunciateValue(params, "true", "Enunciate should be true for 'Yes'");

        Files.deleteIfExists(excelFile.toPath());
        Files.deleteIfExists(tempDir);
    }

    @Test
    void testMultipleEnunciateColumnsFirstHasValue() throws Exception {
        Path tempDir = Files.createTempDirectory("enunciate-multi-col-test");
        File excelFile = tempDir.resolve("test.xlsx").toFile();

        // Create workbook with two enunciation columns, first one has value
        createTestWorkbookWithMultipleEnunciateColumns(excelFile, "Yes", "");

        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(excelFile);

        // Should use the value from the first column
        assertEquals(1, parser.nurseCalls.size());
        ExcelParserV5.FlowRow nurseCall = parser.nurseCalls.get(0);
        assertEquals("Yes", nurseCall.enunciate, 
            "Should extract from first column when it has a value");

        var nurseJson = parser.buildNurseCallsJson();
        var flows = (List<?>) nurseJson.get("deliveryFlows");
        var flow = (Map<?, ?>) flows.get(0);
        var params = (List<?>) flow.get("parameterAttributes");

        assertEnunciateValue(params, "true", "Enunciate should be true from first column");

        Files.deleteIfExists(excelFile.toPath());
        Files.deleteIfExists(tempDir);
    }

    @Test
    void testMultipleEnunciateColumnsSecondHasValue() throws Exception {
        Path tempDir = Files.createTempDirectory("enunciate-multi-col-test");
        File excelFile = tempDir.resolve("test.xlsx").toFile();

        // Create workbook with two enunciation columns, second one has value
        createTestWorkbookWithMultipleEnunciateColumns(excelFile, "", "No");

        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(excelFile);

        // Should use the value from the second column
        assertEquals(1, parser.nurseCalls.size());
        ExcelParserV5.FlowRow nurseCall = parser.nurseCalls.get(0);
        assertEquals("No", nurseCall.enunciate, 
            "Should extract from second column when first is empty");

        var nurseJson = parser.buildNurseCallsJson();
        var flows = (List<?>) nurseJson.get("deliveryFlows");
        var flow = (Map<?, ?>) flows.get(0);
        var params = (List<?>) flow.get("parameterAttributes");

        assertEnunciateValue(params, "false", "Enunciate should be false from second column");

        Files.deleteIfExists(excelFile.toPath());
        Files.deleteIfExists(tempDir);
    }

    @Test
    void testMultipleEnunciateColumnsBothHaveValues() throws Exception {
        Path tempDir = Files.createTempDirectory("enunciate-multi-col-test");
        File excelFile = tempDir.resolve("test.xlsx").toFile();

        // Create workbook with two enunciation columns, both have values
        createTestWorkbookWithMultipleEnunciateColumns(excelFile, "Yes", "No");

        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(excelFile);

        // Should use the value from the first column (priority to first match)
        assertEquals(1, parser.nurseCalls.size());
        ExcelParserV5.FlowRow nurseCall = parser.nurseCalls.get(0);
        assertEquals("Yes", nurseCall.enunciate, 
            "Should extract from first column when both have values");

        var nurseJson = parser.buildNurseCallsJson();
        var flows = (List<?>) nurseJson.get("deliveryFlows");
        var flow = (Map<?, ?>) flows.get(0);
        var params = (List<?>) flow.get("parameterAttributes");

        assertEnunciateValue(params, "true", "Enunciate should be true from first column");

        Files.deleteIfExists(excelFile.toPath());
        Files.deleteIfExists(tempDir);
    }

    @Test
    void testMultipleEnunciateColumnsNeitherHasValue() throws Exception {
        Path tempDir = Files.createTempDirectory("enunciate-multi-col-test");
        File excelFile = tempDir.resolve("test.xlsx").toFile();

        // Create workbook with two enunciation columns, neither has value
        createTestWorkbookWithMultipleEnunciateColumns(excelFile, "", "");

        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(excelFile);

        // Should default to empty (which then defaults to true in JSON)
        assertEquals(1, parser.nurseCalls.size());
        ExcelParserV5.FlowRow nurseCall = parser.nurseCalls.get(0);
        assertEquals("", nurseCall.enunciate, 
            "Should be empty when both columns are empty");

        var nurseJson = parser.buildNurseCallsJson();
        var flows = (List<?>) nurseJson.get("deliveryFlows");
        var flow = (Map<?, ?>) flows.get(0);
        var params = (List<?>) flow.get("parameterAttributes");

        assertEnunciateValue(params, "true", "Enunciate should default to true when both empty");

        Files.deleteIfExists(excelFile.toPath());
        Files.deleteIfExists(tempDir);
    }

    private void createTestWorkbookWithMultipleEnunciateColumns(File target, 
                                                                String firstColumnValue,
                                                                String secondColumnValue) throws Exception {
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

            // Nurse Call sheet with TWO enunciation columns
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
            nurseHeader.createCell(9).setCellValue("Engage/Edge Display Time (Time to Live) (Device - A)");
            nurseHeader.createCell(10).setCellValue("Phone: Alert Display / Genie Enunciation (if badge) B");
            nurseHeader.createCell(11).setCellValue("Badge-Genie Enunciation");
            nurseHeader.createCell(12).setCellValue("Time to 1st Recipient");
            nurseHeader.createCell(13).setCellValue("1st Recipient");
            
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
            nurseRow.createCell(10).setCellValue(firstColumnValue);  // First enunciate column
            nurseRow.createCell(11).setCellValue(secondColumnValue);  // Second enunciate column
            nurseRow.createCell(12).setCellValue("0");
            nurseRow.createCell(13).setCellValue("Nurse Team");

            // Patient Monitoring sheet (minimal)
            Sheet clinicals = workbook.createSheet("Patient Monitoring");
            Row clinicalHeader = clinicals.createRow(2);
            clinicalHeader.createCell(0).setCellValue("Configuration Group");
            clinicalHeader.createCell(1).setCellValue("Common Alert or Alarm Name");

            try (FileOutputStream fos = new FileOutputStream(target)) {
                workbook.write(fos);
            }
        }
    }
}
