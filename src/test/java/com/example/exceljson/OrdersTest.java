package com.example.exceljson;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for Orders configuration functionality.
 */
class OrdersTest {

    @TempDir
    Path tempDir;

    /**
     * Creates a minimal Excel workbook with Orders sheet data.
     */
    private File createTestWorkbook(String ordersSheetName) throws Exception {
        File excelFile = tempDir.resolve("test-orders.xlsx").toFile();
        
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            // Create Unit Breakdown sheet
            Sheet unitSheet = wb.createSheet("Unit Breakdown");
            Row headerRow = unitSheet.createRow(2);
            headerRow.createCell(0).setCellValue("Facility");
            headerRow.createCell(1).setCellValue("Common Unit Name");
            headerRow.createCell(2).setCellValue("Nurse Call Configuration Group");
            headerRow.createCell(3).setCellValue("Patient Monitoring Configuration Group");
            headerRow.createCell(4).setCellValue("Orders Configuration Group");
            headerRow.createCell(5).setCellValue("No Caregiver Group");
            
            Row dataRow = unitSheet.createRow(3);
            dataRow.createCell(0).setCellValue("Test Facility");
            dataRow.createCell(1).setCellValue("Test Unit");
            dataRow.createCell(2).setCellValue("Nurse Group 1");
            dataRow.createCell(3).setCellValue("Clinical Group 1");
            dataRow.createCell(4).setCellValue("Orders Group 1");
            dataRow.createCell(5).setCellValue("No Caregiver Group");
            
            // Create Orders sheet with the specified name
            Sheet ordersSheet = wb.createSheet(ordersSheetName);
            Row orderHeader = ordersSheet.createRow(2);
            orderHeader.createCell(0).setCellValue("In scope");
            orderHeader.createCell(1).setCellValue("Configuration Group");
            orderHeader.createCell(2).setCellValue("Common Alert or Alarm Name");
            orderHeader.createCell(3).setCellValue("Sending System Alert Name");
            orderHeader.createCell(4).setCellValue("Priority");
            orderHeader.createCell(5).setCellValue("Device - A");
            orderHeader.createCell(6).setCellValue("Time to 1st Recipient");
            orderHeader.createCell(7).setCellValue("1st Recipient");
            
            Row orderData = ordersSheet.createRow(3);
            orderData.createCell(0).setCellValue("TRUE");
            orderData.createCell(1).setCellValue("Orders Group 1");
            orderData.createCell(2).setCellValue("Med Order");
            orderData.createCell(3).setCellValue("Medication Order");
            orderData.createCell(4).setCellValue("High");
            orderData.createCell(5).setCellValue("Edge");
            orderData.createCell(6).setCellValue("0");
            orderData.createCell(7).setCellValue("VAssign:[Room]");
            
            try (FileOutputStream fos = new FileOutputStream(excelFile)) {
                wb.write(fos);
            }
        }
        
        return excelFile;
    }

    @Test
    void ordersSheetCanBeNamedOrder() throws Exception {
        File excelFile = createTestWorkbook("Order");
        
        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(excelFile);
        
        assertEquals(1, parser.orders.size(), "Should parse 1 order from 'Order' sheet");
        assertEquals("Orders", parser.orders.get(0).type);
        assertEquals("Med Order", parser.orders.get(0).alarmName);
    }

    @Test
    void ordersSheetCanBeNamedMedOrder() throws Exception {
        File excelFile = createTestWorkbook("Med Order");
        
        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(excelFile);
        
        assertEquals(1, parser.orders.size(), "Should parse 1 order from 'Med Order' sheet");
        assertEquals("Orders", parser.orders.get(0).type);
    }

    @Test
    void ordersSheetCanBeNamedSTATMED() throws Exception {
        File excelFile = createTestWorkbook("STAT MED");
        
        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(excelFile);
        
        assertEquals(1, parser.orders.size(), "Should parse 1 order from 'STAT MED' sheet");
        assertEquals("Orders", parser.orders.get(0).type);
    }

    @Test
    void ordersSheetNameIsCaseInsensitive() throws Exception {
        File excelFile = createTestWorkbook("order");
        
        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(excelFile);
        
        assertEquals(1, parser.orders.size(), "Should parse orders regardless of case");
    }

    @Test
    void ordersJsonHasCorrectType() throws Exception {
        File excelFile = createTestWorkbook("Order");
        
        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(excelFile);
        
        Map<String, Object> ordersJson = parser.buildOrdersJson();
        
        assertNotNull(ordersJson);
        assertEquals("1.1.0", ordersJson.get("version"));
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> alarmDefs = (List<Map<String, Object>>) ordersJson.get("alarmAlertDefinitions");
        assertNotNull(alarmDefs);
        assertEquals(1, alarmDefs.size());
        assertEquals("Orders", alarmDefs.get(0).get("type"), "Alarm definition type should be 'Orders'");
    }

    @Test
    void ordersParameterAttributesAreHardcoded() throws Exception {
        File excelFile = createTestWorkbook("Order");
        
        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(excelFile);
        
        Map<String, Object> ordersJson = parser.buildOrdersJson();
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> flows = (List<Map<String, Object>>) ordersJson.get("deliveryFlows");
        assertNotNull(flows);
        assertEquals(1, flows.size());
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> params = (List<Map<String, Object>>) flows.get(0).get("parameterAttributes");
        assertNotNull(params);
        
        // Verify hardcoded parameters exist
        assertTrue(hasParameter(params, "message"));
        assertTrue(hasParameter(params, "patientMRN"));
        assertTrue(hasParameter(params, "patientName"));
        assertTrue(hasParameter(params, "eventIdentification"));
        assertTrue(hasParameter(params, "shortMessage"));
        assertTrue(hasParameter(params, "subject"));
        
        // Verify specific values
        String message = getParameterValue(params, "message");
        assertTrue(message.contains("#{patient.last_name}"), "message should contain patient.last_name");
        assertTrue(message.contains("#{patient.first_name}"), "message should contain patient.first_name");
        assertTrue(message.contains("#{category}"), "message should contain category");
        assertTrue(message.contains("#{description}"), "message should contain description");
        
        String patientMRN = getParameterValue(params, "patientMRN");
        assertTrue(patientMRN.contains("#{patient.mrn}"), "patientMRN should contain patient.mrn");
        assertTrue(patientMRN.contains("#{patient.visit_number}"), "patientMRN should contain patient.visit_number");
        
        String eventId = getParameterValue(params, "eventIdentification");
        assertTrue(eventId.contains("#{id}"), "eventIdentification should contain id");
    }

    @Test
    void ordersFlowNameIncludesSENDORDER() throws Exception {
        File excelFile = createTestWorkbook("Order");
        
        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(excelFile);
        
        Map<String, Object> ordersJson = parser.buildOrdersJson();
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> flows = (List<Map<String, Object>>) ordersJson.get("deliveryFlows");
        
        String flowName = (String) flows.get(0).get("name");
        assertTrue(flowName.startsWith("SEND ORDER"), "Flow name should start with 'SEND ORDER'");
    }

    @Test
    void ordersGroupToUnitsMapIsPopulated() throws Exception {
        File excelFile = createTestWorkbook("Order");
        
        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(excelFile);
        
        Map<String, Object> ordersJson = parser.buildOrdersJson();
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> flows = (List<Map<String, Object>>) ordersJson.get("deliveryFlows");
        
        @SuppressWarnings("unchecked")
        List<Map<String, String>> units = (List<Map<String, String>>) flows.get(0).get("units");
        
        assertNotNull(units);
        assertEquals(1, units.size());
        assertEquals("Test Facility", units.get(0).get("facilityName"));
        assertEquals("Test Unit", units.get(0).get("name"));
    }

    @Test
    void loadSummaryIncludesOrdersCount() throws Exception {
        File excelFile = createTestWorkbook("Order");
        
        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(excelFile);
        
        String summary = parser.getLoadSummary();
        
        assertTrue(summary.contains("Orders rows"), "Summary should mention Orders rows");
        assertTrue(summary.contains("Configuration Groups (Orders)"), "Summary should mention Orders config groups");
    }

    @Test
    void writeOrdersJsonCreatesFile() throws Exception {
        File excelFile = createTestWorkbook("Order");
        File jsonFile = tempDir.resolve("orders.json").toFile();
        
        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(excelFile);
        parser.writeOrdersJson(jsonFile);
        
        assertTrue(jsonFile.exists(), "Orders JSON file should be created");
        assertTrue(jsonFile.length() > 0, "Orders JSON file should not be empty");
    }

    // Helper methods
    private boolean hasParameter(List<Map<String, Object>> params, String name) {
        return params.stream().anyMatch(p -> name.equals(p.get("name")));
    }

    private String getParameterValue(List<Map<String, Object>> params, String name) {
        return params.stream()
            .filter(p -> name.equals(p.get("name")))
            .map(p -> (String) p.get("value"))
            .findFirst()
            .orElse(null);
    }
}
