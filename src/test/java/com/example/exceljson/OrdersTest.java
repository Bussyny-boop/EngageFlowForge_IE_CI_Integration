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
        assertEquals("Orders", parser.orders.getFirst().type);
        assertEquals("Med Order", parser.orders.getFirst().alarmName);
    }

    @Test
    void ordersSheetCanBeNamedMedOrder() throws Exception {
        File excelFile = createTestWorkbook("Med Order");
        
        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(excelFile);
        
        assertEquals(1, parser.orders.size(), "Should parse 1 order from 'Med Order' sheet");
        assertEquals("Orders", parser.orders.getFirst().type);
    }

    @Test
    void ordersSheetCanBeNamedSTATMED() throws Exception {
        File excelFile = createTestWorkbook("STAT MED");
        
        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(excelFile);
        
        assertEquals(1, parser.orders.size(), "Should parse 1 order from 'STAT MED' sheet");
        assertEquals("Orders", parser.orders.getFirst().type);
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
        assertEquals("Orders", alarmDefs.getFirst().get("type"), "Alarm definition type should be 'Orders'");
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
        List<Map<String, Object>> params = (List<Map<String, Object>>) flows.getFirst().get("parameterAttributes");
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
        
        String flowName = (String) flows.getFirst().get("name");
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
        List<Map<String, String>> units = (List<Map<String, String>>) flows.getFirst().get("units");
        
        assertNotNull(units);
        assertEquals(1, units.size());
        assertEquals("Test Facility", units.getFirst().get("facilityName"));
        assertEquals("Test Unit", units.getFirst().get("name"));
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

    @Test
    void ordersSheetCanHaveNameContainingOrder() throws Exception {
        File excelFile = createTestWorkbook("Med Orders Entry");
        
        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(excelFile);
        
        assertEquals(1, parser.orders.size(), "Should parse orders from sheet with 'Order' in name");
        assertEquals("Orders", parser.orders.getFirst().type);
    }

    @Test
    void ordersSheetPluralForm() throws Exception {
        File excelFile = createTestWorkbook("Orders");
        
        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(excelFile);
        
        assertEquals(1, parser.orders.size(), "Should parse orders from 'Orders' sheet");
        assertEquals("Orders", parser.orders.getFirst().type);
    }

    @Test
    void ordersIncludesParametersFromNursecallFlow() throws Exception {
        File excelFile = tempDir.resolve("test-orders-params.xlsx").toFile();
        
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
            
            // Create Orders sheet with all parameter columns
            Sheet ordersSheet = wb.createSheet("Order");
            Row orderHeader = ordersSheet.createRow(2);
            orderHeader.createCell(0).setCellValue("In scope");
            orderHeader.createCell(1).setCellValue("Configuration Group");
            orderHeader.createCell(2).setCellValue("Common Alert or Alarm Name");
            orderHeader.createCell(3).setCellValue("Sending System Alert Name");
            orderHeader.createCell(4).setCellValue("Priority");
            orderHeader.createCell(5).setCellValue("Device - A");
            orderHeader.createCell(6).setCellValue("Ringtone Device - A");
            orderHeader.createCell(7).setCellValue("Break Through DND");
            orderHeader.createCell(8).setCellValue("Genie Enunciation");
            orderHeader.createCell(9).setCellValue("Engage/Edge Display Time (Time to Live) (Device - A)");
            orderHeader.createCell(10).setCellValue("Time to 1st Recipient");
            orderHeader.createCell(11).setCellValue("1st Recipient");
            
            Row orderData = ordersSheet.createRow(3);
            orderData.createCell(0).setCellValue("TRUE");
            orderData.createCell(1).setCellValue("Orders Group 1");
            orderData.createCell(2).setCellValue("Med Order");
            orderData.createCell(3).setCellValue("Medication Order");
            orderData.createCell(4).setCellValue("High");
            orderData.createCell(5).setCellValue("Edge");
            orderData.createCell(6).setCellValue("Alert1");
            orderData.createCell(7).setCellValue("Vocera and Device");
            orderData.createCell(8).setCellValue("FALSE");
            orderData.createCell(9).setCellValue("20");
            orderData.createCell(10).setCellValue("0");
            orderData.createCell(11).setCellValue("VAssign:[Room]");
            
            try (FileOutputStream fos = new FileOutputStream(excelFile)) {
                wb.write(fos);
            }
        }
        
        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(excelFile);
        
        Map<String, Object> ordersJson = parser.buildOrdersJson();
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> flows = (List<Map<String, Object>>) ordersJson.get("deliveryFlows");
        assertNotNull(flows);
        assertEquals(1, flows.size());
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> params = (List<Map<String, Object>>) flows.getFirst().get("parameterAttributes");
        assertNotNull(params);
        
        // Verify all parameters from NursecallFlow logic are present
        assertTrue(hasParameter(params, "alertSound"), "Should have alertSound parameter");
        assertTrue(hasParameter(params, "breakThrough"), "Should have breakThrough parameter");
        assertTrue(hasParameter(params, "enunciate"), "Should have enunciate parameter");
        assertTrue(hasParameter(params, "popup"), "Should have popup parameter");
        assertTrue(hasParameter(params, "ttl"), "Should have ttl parameter");
        assertTrue(hasParameter(params, "retractRules"), "Should have retractRules parameter");
        assertTrue(hasParameter(params, "vibrate"), "Should have vibrate parameter");
        
        // Verify parameter values
        String alertSound = getParameterValue(params, "alertSound");
        assertTrue(alertSound.contains("Alert1"), "alertSound should be Alert1 from Excel");
        
        String breakThrough = getParameterValue(params, "breakThrough");
        assertTrue(breakThrough.contains("Vocera and Device"), "breakThrough should be 'Vocera and Device' from Excel");
        
        Object enunciate = getParameterObjectValue(params, "enunciate");
        assertEquals("false", enunciate, "enunciate should be false (string) from Excel");
        
        Object popup = getParameterObjectValue(params, "popup");
        assertEquals("true", popup, "popup should be true (string)");
        
        Object ttl = getParameterObjectValue(params, "ttl");
        assertEquals("20", ttl, "ttl should be 20 (from Excel)");
        
        Object retractRules = getParameterObjectValue(params, "retractRules");
        assertEquals("[\"ttlHasElapsed\"]", retractRules, "retractRules should be array");
        
        String vibrate = getParameterValue(params, "vibrate");
        assertTrue(vibrate.contains("short"), "vibrate should be 'short'");
    }

    @Test
    void ordersDestinationsDoNotHaveParameterAttributes() throws Exception {
        File excelFile = createTestWorkbook("Order");
        
        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(excelFile);
        
        Map<String, Object> ordersJson = parser.buildOrdersJson();
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> flows = (List<Map<String, Object>>) ordersJson.get("deliveryFlows");
        assertNotNull(flows);
        assertEquals(1, flows.size());
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> destinations = (List<Map<String, Object>>) flows.getFirst().get("destinations");
        assertNotNull(destinations);
        assertTrue(destinations.size() > 0, "Should have at least one destination");
        
        // Verify destinations have order, delayTime, destinationType as properties (not parameterAttributes)
        for (Map<String, Object> dest : destinations) {
            assertTrue(dest.containsKey("order"), "Destination should have 'order' property");
            assertTrue(dest.containsKey("delayTime"), "Destination should have 'delayTime' property");
            assertTrue(dest.containsKey("destinationType"), "Destination should have 'destinationType' property");
            
            // These should be direct properties of the destination object
            assertNotNull(dest.get("order"), "order should not be null");
            assertNotNull(dest.get("delayTime"), "delayTime should not be null");
            assertNotNull(dest.get("destinationType"), "destinationType should not be null");
        }
        
        // Verify these are NOT in parameterAttributes
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> params = (List<Map<String, Object>>) flows.getFirst().get("parameterAttributes");
        assertNotNull(params);
        
        assertFalse(hasParameter(params, "order"), "parameterAttributes should NOT contain 'order'");
        assertFalse(hasParameter(params, "delayTime"), "parameterAttributes should NOT contain 'delayTime'");
        assertFalse(hasParameter(params, "destinationType"), "parameterAttributes should NOT contain 'destinationType'");
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

    private Object getParameterObjectValue(List<Map<String, Object>> params, String name) {
        return params.stream()
            .filter(p -> name.equals(p.get("name")))
            .map(p -> p.get("value"))
            .findFirst()
            .orElse(null);
    }

    @Test
    void ordersFlowHasDestinationNameParameters() throws Exception {
        File excelFile = tempDir.resolve("test-orders-destname.xlsx").toFile();
        
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
            
            // Create Orders sheet with various recipient formats
            Sheet ordersSheet = wb.createSheet("Order");
            Row orderHeader = ordersSheet.createRow(2);
            orderHeader.createCell(0).setCellValue("In scope");
            orderHeader.createCell(1).setCellValue("Configuration Group");
            orderHeader.createCell(2).setCellValue("Common Alert or Alarm Name");
            orderHeader.createCell(3).setCellValue("Sending System Alert Name");
            orderHeader.createCell(4).setCellValue("Priority");
            orderHeader.createCell(5).setCellValue("Device - A");
            orderHeader.createCell(6).setCellValue("Time to 1st Recipient");
            orderHeader.createCell(7).setCellValue("1st Recipient");
            orderHeader.createCell(8).setCellValue("Time to 2nd Recipient");
            orderHeader.createCell(9).setCellValue("2nd Recipient");
            orderHeader.createCell(10).setCellValue("Time to 3rd Recipient");
            orderHeader.createCell(11).setCellValue("3rd Recipient");
            
            Row orderData = ordersSheet.createRow(3);
            orderData.createCell(0).setCellValue("TRUE");
            orderData.createCell(1).setCellValue("Orders Group 1");
            orderData.createCell(2).setCellValue("Med Order");
            orderData.createCell(3).setCellValue("Medication Order");
            orderData.createCell(4).setCellValue("High");
            orderData.createCell(5).setCellValue("Edge");
            orderData.createCell(6).setCellValue("0");
            orderData.createCell(7).setCellValue("VAssign: Room Charge Nurse");
            orderData.createCell(8).setCellValue("1");
            orderData.createCell(9).setCellValue("Rld: R5: CS 1: Room PCT");
            orderData.createCell(10).setCellValue("2");
            orderData.createCell(11).setCellValue("Rld: R5: CS 2: Room RN");
            
            try (FileOutputStream fos = new FileOutputStream(excelFile)) {
                wb.write(fos);
            }
        }
        
        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(excelFile);
        
        Map<String, Object> ordersJson = parser.buildOrdersJson();
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> flows = (List<Map<String, Object>>) ordersJson.get("deliveryFlows");
        assertNotNull(flows);
        assertEquals(1, flows.size());
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> params = (List<Map<String, Object>>) flows.getFirst().get("parameterAttributes");
        assertNotNull(params);
        
        // Find destinationName parameters by destinationOrder
        Map<String, Object> dest0 = findParameterWithDestinationOrder(params, "destinationName", 0);
        Map<String, Object> dest1 = findParameterWithDestinationOrder(params, "destinationName", 1);
        Map<String, Object> dest2 = findParameterWithDestinationOrder(params, "destinationName", 2);
        
        assertNotNull(dest0, "Should have destinationName parameter for order 0");
        assertNotNull(dest1, "Should have destinationName parameter for order 1");
        assertNotNull(dest2, "Should have destinationName parameter for order 2");
        
        // Verify the captured functional role names (text after "Room" keyword)
        assertEquals("\"Charge Nurse\"", dest0.get("value"), "Should capture 'Charge Nurse' from 'VAssign: Room Charge Nurse'");
        assertEquals("\"PCT\"", dest1.get("value"), "Should capture 'PCT' from 'Rld: R5: CS 1: Room PCT'");
        assertEquals("\"RN\"", dest2.get("value"), "Should capture 'RN' from 'Rld: R5: CS 2: Room RN'");
        
        // Verify destinationOrder values
        assertEquals(0, dest0.get("destinationOrder"));
        assertEquals(1, dest1.get("destinationOrder"));
        assertEquals(2, dest2.get("destinationOrder"));
    }
    
    // Helper to find a parameter by name and destinationOrder
    private Map<String, Object> findParameterWithDestinationOrder(List<Map<String, Object>> params, String name, int order) {
        return params.stream()
            .filter(p -> name.equals(p.get("name")) && Integer.valueOf(order).equals(p.get("destinationOrder")))
            .findFirst()
            .orElse(null);
    }

    @Test
    void ordersDestinationNameExtractsTextAfterRoom() throws Exception {
        File excelFile = tempDir.resolve("test-orders-room-extraction.xlsx").toFile();
        
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            // Create Unit Breakdown sheet
            Sheet unitSheet = wb.createSheet("Unit Breakdown");
            Row headerRow = unitSheet.createRow(2);
            headerRow.createCell(0).setCellValue("Facility");
            headerRow.createCell(1).setCellValue("Common Unit Name");
            headerRow.createCell(4).setCellValue("Orders Configuration Group");
            
            Row dataRow = unitSheet.createRow(3);
            dataRow.createCell(0).setCellValue("Test Facility");
            dataRow.createCell(1).setCellValue("Test Unit");
            dataRow.createCell(4).setCellValue("Orders Group 1");
            
            // Create Orders sheet with edge case recipients
            Sheet ordersSheet = wb.createSheet("Order");
            Row orderHeader = ordersSheet.createRow(2);
            orderHeader.createCell(0).setCellValue("In scope");
            orderHeader.createCell(1).setCellValue("Configuration Group");
            orderHeader.createCell(2).setCellValue("Common Alert or Alarm Name");
            orderHeader.createCell(7).setCellValue("1st Recipient");
            orderHeader.createCell(9).setCellValue("2nd Recipient");
            orderHeader.createCell(11).setCellValue("3rd Recipient");
            
            Row orderData = ordersSheet.createRow(3);
            orderData.createCell(0).setCellValue("TRUE");
            orderData.createCell(1).setCellValue("Orders Group 1");
            orderData.createCell(2).setCellValue("Med Order");
            orderData.createCell(7).setCellValue("room Nurse");  // lowercase
            orderData.createCell(9).setCellValue("ROOM Physician");  // uppercase
            orderData.createCell(11).setCellValue("Room Tech");  // mixed case
            
            try (FileOutputStream fos = new FileOutputStream(excelFile)) {
                wb.write(fos);
            }
        }
        
        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(excelFile);
        
        Map<String, Object> ordersJson = parser.buildOrdersJson();
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> flows = (List<Map<String, Object>>) ordersJson.get("deliveryFlows");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> params = (List<Map<String, Object>>) flows.getFirst().get("parameterAttributes");
        
        // Verify case-insensitive Room detection
        Map<String, Object> dest0 = findParameterWithDestinationOrder(params, "destinationName", 0);
        Map<String, Object> dest1 = findParameterWithDestinationOrder(params, "destinationName", 1);
        Map<String, Object> dest2 = findParameterWithDestinationOrder(params, "destinationName", 2);
        
        assertEquals("\"Nurse\"", dest0.get("value"), "Should extract text after 'room' (case-insensitive)");
        assertEquals("\"Physician\"", dest1.get("value"), "Should extract text after 'ROOM' (case-insensitive)");
        assertEquals("\"Tech\"", dest2.get("value"), "Should extract text after 'Room' (case-insensitive)");
    }

    @Test
    void ordersDestinationNameStripsSpecialCharactersAfterRoom() throws Exception {
        File excelFile = tempDir.resolve("test-orders-special-chars.xlsx").toFile();
        
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            // Create Unit Breakdown sheet
            Sheet unitSheet = wb.createSheet("Unit Breakdown");
            Row headerRow = unitSheet.createRow(2);
            headerRow.createCell(0).setCellValue("Facility");
            headerRow.createCell(1).setCellValue("Common Unit Name");
            headerRow.createCell(4).setCellValue("Orders Configuration Group");
            
            Row dataRow = unitSheet.createRow(3);
            dataRow.createCell(0).setCellValue("Test Facility");
            dataRow.createCell(1).setCellValue("Test Unit");
            dataRow.createCell(4).setCellValue("Orders Group 1");
            
            // Create Orders sheet with special characters between "Room" and role name
            Sheet ordersSheet = wb.createSheet("Order");
            Row orderHeader = ordersSheet.createRow(2);
            orderHeader.createCell(0).setCellValue("In scope");
            orderHeader.createCell(1).setCellValue("Configuration Group");
            orderHeader.createCell(2).setCellValue("Common Alert or Alarm Name");
            orderHeader.createCell(7).setCellValue("1st Recipient");
            orderHeader.createCell(9).setCellValue("2nd Recipient");
            orderHeader.createCell(11).setCellValue("3rd Recipient");
            orderHeader.createCell(13).setCellValue("4th Recipient");
            orderHeader.createCell(15).setCellValue("5th Recipient");
            
            Row orderData = ordersSheet.createRow(3);
            orderData.createCell(0).setCellValue("TRUE");
            orderData.createCell(1).setCellValue("Orders Group 1");
            orderData.createCell(2).setCellValue("Med Order");
            // Test various special character patterns that should be stripped
            orderData.createCell(7).setCellValue("VAssign:[Room] CNA");  // Bracket before role
            orderData.createCell(9).setCellValue("Rld: R5: Room] Nurse");  // Bracket after Room
            orderData.createCell(11).setCellValue("Room) PCT");  // Parenthesis after Room
            orderData.createCell(13).setCellValue("Room - RN");  // Dash after Room
            orderData.createCell(15).setCellValue("Room]  Charge Nurse");  // Bracket and spaces
            
            try (FileOutputStream fos = new FileOutputStream(excelFile)) {
                wb.write(fos);
            }
        }
        
        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(excelFile);
        
        Map<String, Object> ordersJson = parser.buildOrdersJson();
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> flows = (List<Map<String, Object>>) ordersJson.get("deliveryFlows");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> params = (List<Map<String, Object>>) flows.getFirst().get("parameterAttributes");
        
        // Verify special characters are stripped from destinationName values
        Map<String, Object> dest0 = findParameterWithDestinationOrder(params, "destinationName", 0);
        Map<String, Object> dest1 = findParameterWithDestinationOrder(params, "destinationName", 1);
        Map<String, Object> dest2 = findParameterWithDestinationOrder(params, "destinationName", 2);
        Map<String, Object> dest3 = findParameterWithDestinationOrder(params, "destinationName", 3);
        Map<String, Object> dest4 = findParameterWithDestinationOrder(params, "destinationName", 4);
        
        assertEquals("\"CNA\"", dest0.get("value"), "Should strip '] ' before 'CNA'");
        assertEquals("\"Nurse\"", dest1.get("value"), "Should strip '] ' before 'Nurse'");
        assertEquals("\"PCT\"", dest2.get("value"), "Should strip ') ' before 'PCT'");
        assertEquals("\"RN\"", dest3.get("value"), "Should strip '- ' before 'RN'");
        assertEquals("\"Charge Nurse\"", dest4.get("value"), "Should strip ']  ' before 'Charge Nurse'");
    }

    @Test
    void ordersFlowHandlesMultipleRoomsInOneRecipient() throws Exception {
        File excelFile = tempDir.resolve("test-orders-multiple-rooms.xlsx").toFile();
        
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            // Create Unit Breakdown sheet
            Sheet unitSheet = wb.createSheet("Unit Breakdown");
            Row headerRow = unitSheet.createRow(2);
            headerRow.createCell(0).setCellValue("Facility");
            headerRow.createCell(1).setCellValue("Common Unit Name");
            headerRow.createCell(4).setCellValue("Orders Configuration Group");
            
            Row dataRow = unitSheet.createRow(3);
            dataRow.createCell(0).setCellValue("BCH");
            dataRow.createCell(1).setCellValue("Test Unit");
            dataRow.createCell(4).setCellValue("Orders Group 1");
            
            // Create Orders sheet with multiple rooms in one recipient field
            Sheet ordersSheet = wb.createSheet("Order");
            Row orderHeader = ordersSheet.createRow(2);
            orderHeader.createCell(0).setCellValue("In scope");
            orderHeader.createCell(1).setCellValue("Configuration Group");
            orderHeader.createCell(2).setCellValue("Common Alert or Alarm Name");
            orderHeader.createCell(7).setCellValue("1st Recipient");
            
            Row orderData = ordersSheet.createRow(3);
            orderData.createCell(0).setCellValue("TRUE");
            orderData.createCell(1).setCellValue("Orders Group 1");
            orderData.createCell(2).setCellValue("Med Order");
            // Multiple rooms separated by newline
            orderData.createCell(7).setCellValue("Rld: R5: CS 1: Room Nurse\nRld: R5: CS 2: Room CNA");
            
            try (FileOutputStream fos = new FileOutputStream(excelFile)) {
                wb.write(fos);
            }
        }
        
        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(excelFile);
        
        Map<String, Object> ordersJson = parser.buildOrdersJson();
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> flows = (List<Map<String, Object>>) ordersJson.get("deliveryFlows");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> destinations = (List<Map<String, Object>>) flows.getFirst().get("destinations");
        
        assertEquals(1, destinations.size(), "Should have one destination");
        
        @SuppressWarnings("unchecked")
        List<Map<String, String>> functionalRoles = (List<Map<String, String>>) destinations.getFirst().get("functionalRoles");
        
        assertNotNull(functionalRoles);
        assertEquals(2, functionalRoles.size(), "Should have two functional roles from the two Room keywords");
        
        // Verify the two functional roles
        assertEquals("BCH", functionalRoles.getFirst().get("facilityName"));
        assertEquals("Nurse", functionalRoles.getFirst().get("name"));
        
        assertEquals("BCH", functionalRoles.get(1).get("facilityName"));
        assertEquals("CNA", functionalRoles.get(1).get("name"));
    }

    @Test
    void ordersFlowsDoNotHaveNoDeliveriesDestination() throws Exception {
        File excelFile = tempDir.resolve("test-orders-nodeliveries.xlsx").toFile();
        
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            // Create Unit Breakdown sheet with No Caregiver Group
            Sheet unitSheet = wb.createSheet("Unit Breakdown");
            Row headerRow = unitSheet.createRow(2);
            headerRow.createCell(0).setCellValue("Facility");
            headerRow.createCell(1).setCellValue("Common Unit Name");
            headerRow.createCell(2).setCellValue("Nurse Call Configuration Group");
            headerRow.createCell(3).setCellValue("Patient Monitoring Configuration Group");
            headerRow.createCell(4).setCellValue("Orders Configuration Group");
            headerRow.createCell(5).setCellValue("No Caregiver Group");
            
            Row dataRow = unitSheet.createRow(3);
            dataRow.createCell(0).setCellValue("BCH");
            dataRow.createCell(1).setCellValue("Test Unit");
            dataRow.createCell(2).setCellValue("Nurse Group 1");
            dataRow.createCell(3).setCellValue("Clinical Group 1");
            dataRow.createCell(4).setCellValue("Orders Group 1");
            dataRow.createCell(5).setCellValue("House Supervisor");
            
            // Create Orders sheet
            Sheet ordersSheet = wb.createSheet("Order");
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
        
        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(excelFile);
        
        Map<String, Object> ordersJson = parser.buildOrdersJson();
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> flows = (List<Map<String, Object>>) ordersJson.get("deliveryFlows");
        assertNotNull(flows);
        assertEquals(1, flows.size());
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> destinations = (List<Map<String, Object>>) flows.getFirst().get("destinations");
        assertNotNull(destinations);
        
        // Verify that none of the destinations have "NoDeliveries" as destinationType
        for (Map<String, Object> dest : destinations) {
            String destType = (String) dest.get("destinationType");
            assertNotEquals("NoDeliveries", destType, 
                "Orders flows should NOT contain NoDeliveries destinations");
        }
    }
}
