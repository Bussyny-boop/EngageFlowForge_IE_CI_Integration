package com.example.exceljson;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

/**
 * Tests for dynamic responseType based on Response Options column.
 */
class ResponseTypeTest {

    @Test
    void noResponseOptionSetsResponseTypeToNone() throws Exception {
        ExcelParserV5 parser = new ExcelParserV5();
        Path excelPath = createWorkbookWithResponseOption("No Response");
        
        parser.load(excelPath.toFile());
        Map<String, Object> json = parser.buildNurseCallsJson();
        
        // Verify responseType is "None"
        String responseType = extractResponseType(json);
        assertEquals("None", responseType, "Response type should be 'None' for 'No Response'");
        
        // Verify no responding parameters are present
        assertNoRespondingParameters(json);
    }

    @Test
    void emptyResponseOptionSetsResponseTypeToNone() throws Exception {
        ExcelParserV5 parser = new ExcelParserV5();
        Path excelPath = createWorkbookWithResponseOption("");
        
        parser.load(excelPath.toFile());
        Map<String, Object> json = parser.buildNurseCallsJson();
        
        // Verify responseType is "None"
        String responseType = extractResponseType(json);
        assertEquals("None", responseType, "Response type should be 'None' for empty response option");
        
        // Verify no responding parameters are present
        assertNoRespondingParameters(json);
    }

    @Test
    void acceptEscalateSetsResponseTypeToAcceptDecline() throws Exception {
        ExcelParserV5 parser = new ExcelParserV5();
        Path excelPath = createWorkbookWithResponseOption("Accept,Escalate");
        
        parser.load(excelPath.toFile());
        Map<String, Object> json = parser.buildNurseCallsJson();
        
        // Verify responseType is "Accept/Decline"
        String responseType = extractResponseType(json);
        assertEquals("Accept/Decline", responseType, "Response type should be 'Accept/Decline' for 'Accept,Escalate'");
        
        // Verify accept and decline parameters are present
        assertHasParameter(json, "accept", "\"Accepted\"");
        assertHasParameter(json, "acceptBadgePhrases", "[\"Accept\"]");
        assertHasParameter(json, "decline", "\"Decline Primary\"");
        assertHasParameter(json, "declineBadgePhrases", "[\"Escalate\"]");
        
        // Verify responding parameters are present
        assertHasRespondingParameters(json);
        
        // Verify acceptAndCall is NOT present
        assertNoParameter(json, "acceptAndCall");
    }

    @Test
    void acceptEscalateCallBackSetsResponseTypeToAcceptDeclineCall() throws Exception {
        ExcelParserV5 parser = new ExcelParserV5();
        Path excelPath = createWorkbookWithResponseOption("Accept,Escalate,Call Back");
        
        parser.load(excelPath.toFile());
        Map<String, Object> json = parser.buildNurseCallsJson();
        
        // Verify responseType is "Accept/Decline/Call"
        String responseType = extractResponseType(json);
        assertEquals("Accept/Decline/Call", responseType, "Response type should be 'Accept/Decline/Call' for 'Accept,Escalate,Call Back'");
        
        // Verify all parameters are present
        assertHasParameter(json, "accept", "\"Accepted\"");
        assertHasParameter(json, "acceptBadgePhrases", "[\"Accept\"]");
        assertHasParameter(json, "acceptAndCall", "\"Call Back\"");
        assertHasParameter(json, "decline", "\"Decline Primary\"");
        assertHasParameter(json, "declineBadgePhrases", "[\"Escalate\"]");
        
        // Verify responding parameters are present
        assertHasRespondingParameters(json);
    }

    @Test
    void responseOptionsCaseInsensitiveWithSpaces() throws Exception {
        ExcelParserV5 parser = new ExcelParserV5();
        Path excelPath = createWorkbookWithResponseOption("ACCEPT, ESCALATE, CALL BACK");
        
        parser.load(excelPath.toFile());
        Map<String, Object> json = parser.buildNurseCallsJson();
        
        // Verify responseType is "Accept/Decline/Call" (case insensitive, spaces handled)
        String responseType = extractResponseType(json);
        assertEquals("Accept/Decline/Call", responseType, "Response type should handle case-insensitive and spaces after commas");
    }

    @Test
    void responseTypeParameterOrderIsCorrect() throws Exception {
        ExcelParserV5 parser = new ExcelParserV5();
        Path excelPath = createWorkbookWithResponseOption("Accept,Escalate");
        
        parser.load(excelPath.toFile());
        Map<String, Object> json = parser.buildNurseCallsJson();
        
        // Get parameter attributes list
        List<Map<String, Object>> paramAttrs = extractParameterAttributes(json);
        
        // Find indices of key parameters
        int alertSoundIndex = findParameterIndex(paramAttrs, "alertSound");
        int responseTypeIndex = findParameterIndex(paramAttrs, "responseType");
        int destinationNameIndex = findParameterIndex(paramAttrs, "destinationName");
        
        // Verify alertSound comes before responseType
        assertTrue(alertSoundIndex >= 0, "alertSound should be present");
        assertTrue(responseTypeIndex > alertSoundIndex, 
            "responseType should come after alertSound");
        
        // Verify responseType comes before destination parameters
        if (destinationNameIndex >= 0) {
            assertTrue(responseTypeIndex < destinationNameIndex, 
                "responseType should come before destination parameters");
        }
    }

    // Helper methods

    private Path createWorkbookWithResponseOption(String responseOption) throws Exception {
        Path tempDir = Files.createTempDirectory("response-type-test");
        Path excelPath = tempDir.resolve("test.xlsx");
        
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            // Unit Breakdown sheet
            Sheet units = workbook.createSheet("Unit Breakdown");
            Row unitsHeader = units.createRow(0);
            unitsHeader.createCell(0).setCellValue("Facility");
            unitsHeader.createCell(2).setCellValue("Common Unit Name");
            unitsHeader.createCell(3).setCellValue("Configuration Group");
            Row unitsRow = units.createRow(1);
            unitsRow.createCell(0).setCellValue("Test Facility");
            unitsRow.createCell(2).setCellValue("Test Unit");
            unitsRow.createCell(3).setCellValue("TestGroup");

            // Nurse Call sheet
            Sheet nurseCalls = workbook.createSheet("Nurse call");
            Row nurseHeader = nurseCalls.createRow(0);
            nurseHeader.createCell(0).setCellValue("Configuration Group");
            nurseHeader.createCell(4).setCellValue("Alarm Name");
            nurseHeader.createCell(5).setCellValue("Priority");
            nurseHeader.createCell(7).setCellValue("Ringtone");
            nurseHeader.createCell(32).setCellValue("Response Options");
            nurseHeader.createCell(33).setCellValue("1st recipients");
            
            Row nurseRow = nurseCalls.createRow(1);
            nurseRow.createCell(0).setCellValue("TestGroup");
            nurseRow.createCell(4).setCellValue("Test Alarm");
            nurseRow.createCell(5).setCellValue("High");
            nurseRow.createCell(7).setCellValue("Tone 1");
            nurseRow.createCell(32).setCellValue(responseOption);
            nurseRow.createCell(33).setCellValue("Nurse Team");

            try (OutputStream os = Files.newOutputStream(excelPath)) {
                workbook.write(os);
            }
        }
        
        return excelPath;
    }

    @SuppressWarnings("unchecked")
    private String extractResponseType(Map<String, Object> json) {
        List<Map<String, Object>> flows = (List<Map<String, Object>>) json.get("deliveryFlows");
        if (flows == null || flows.isEmpty()) {
            throw new RuntimeException("No delivery flows found");
        }
        
        Map<String, Object> flow = flows.get(0);
        List<Map<String, Object>> paramAttrs = (List<Map<String, Object>>) flow.get("parameterAttributes");
        
        for (Map<String, Object> param : paramAttrs) {
            if ("responseType".equals(param.get("name"))) {
                String value = (String) param.get("value");
                // Remove surrounding quotes
                return value.substring(1, value.length() - 1);
            }
        }
        
        throw new RuntimeException("responseType parameter not found");
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> extractParameterAttributes(Map<String, Object> json) {
        List<Map<String, Object>> flows = (List<Map<String, Object>>) json.get("deliveryFlows");
        if (flows == null || flows.isEmpty()) {
            throw new RuntimeException("No delivery flows found");
        }
        
        Map<String, Object> flow = flows.get(0);
        return (List<Map<String, Object>>) flow.get("parameterAttributes");
    }

    @SuppressWarnings("unchecked")
    private void assertHasParameter(Map<String, Object> json, String paramName, String expectedValue) {
        List<Map<String, Object>> flows = (List<Map<String, Object>>) json.get("deliveryFlows");
        Map<String, Object> flow = flows.get(0);
        List<Map<String, Object>> paramAttrs = (List<Map<String, Object>>) flow.get("parameterAttributes");
        
        for (Map<String, Object> param : paramAttrs) {
            if (paramName.equals(param.get("name"))) {
                String value = (String) param.get("value");
                assertEquals(expectedValue, value, 
                    "Parameter '" + paramName + "' should have value '" + expectedValue + "'");
                return;
            }
        }
        
        throw new AssertionError("Parameter '" + paramName + "' not found");
    }

    @SuppressWarnings("unchecked")
    private void assertNoParameter(Map<String, Object> json, String paramName) {
        List<Map<String, Object>> flows = (List<Map<String, Object>>) json.get("deliveryFlows");
        Map<String, Object> flow = flows.get(0);
        List<Map<String, Object>> paramAttrs = (List<Map<String, Object>>) flow.get("parameterAttributes");
        
        for (Map<String, Object> param : paramAttrs) {
            if (paramName.equals(param.get("name"))) {
                throw new AssertionError("Parameter '" + paramName + "' should not be present");
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void assertHasRespondingParameters(Map<String, Object> json) {
        List<Map<String, Object>> flows = (List<Map<String, Object>>) json.get("deliveryFlows");
        Map<String, Object> flow = flows.get(0);
        List<Map<String, Object>> paramAttrs = (List<Map<String, Object>>) flow.get("parameterAttributes");
        
        boolean hasRespondingLine = false;
        boolean hasRespondingUser = false;
        boolean hasResponsePath = false;
        
        for (Map<String, Object> param : paramAttrs) {
            String name = (String) param.get("name");
            if ("respondingLine".equals(name)) hasRespondingLine = true;
            if ("respondingUser".equals(name)) hasRespondingUser = true;
            if ("responsePath".equals(name)) hasResponsePath = true;
        }
        
        assertTrue(hasRespondingLine, "respondingLine parameter should be present");
        assertTrue(hasRespondingUser, "respondingUser parameter should be present");
        assertTrue(hasResponsePath, "responsePath parameter should be present");
    }

    @SuppressWarnings("unchecked")
    private void assertNoRespondingParameters(Map<String, Object> json) {
        List<Map<String, Object>> flows = (List<Map<String, Object>>) json.get("deliveryFlows");
        Map<String, Object> flow = flows.get(0);
        List<Map<String, Object>> paramAttrs = (List<Map<String, Object>>) flow.get("parameterAttributes");
        
        for (Map<String, Object> param : paramAttrs) {
            String name = (String) param.get("name");
            if ("respondingLine".equals(name) || "respondingUser".equals(name) || "responsePath".equals(name)) {
                throw new AssertionError("Responding parameter '" + name + "' should not be present for 'None' response type");
            }
        }
    }

    private int findParameterIndex(List<Map<String, Object>> paramAttrs, String paramName) {
        for (int i = 0; i < paramAttrs.size(); i++) {
            if (paramName.equals(paramAttrs.get(i).get("name"))) {
                return i;
            }
        }
        return -1;
    }
}
