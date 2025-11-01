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
 * Tests for dynamic parsing of responseOptions field
 */
class ResponseOptionsTest {

    @Test
    void noResponseSetsResponseTypeToNone() throws Exception {
        String json = generateJsonForResponseOption("No Response");
        
        assertTrue(json.contains("\"name\": \"responseType\""), 
            "Should have responseType parameter");
        assertTrue(json.contains("\"value\": \"\\\"None\\\"\""), 
            "No Response should set responseType value to \\\"None\\\"");
        assertTrue(!json.contains("\"name\": \"respondingLine\""), 
            "No Response should not include respondingLine");
        assertTrue(!json.contains("\"name\": \"accept\""), 
            "No Response should not include accept parameter");
    }

    @Test
    void acceptOnlySetsResponseTypeToAccept() throws Exception {
        String json = generateJsonForResponseOption("Accept");
        
        assertTrue(json.contains("\"name\": \"responseType\""), 
            "Should have responseType parameter");
        assertTrue(json.contains("\"value\": \"\\\"Accept/Decline\\\"\""), 
            "Accept only should set responseType value to \\\"Accept/Decline\\\"");
        assertTrue(json.contains("\"name\": \"accept\""), 
            "Accept should include accept parameter");
        assertTrue(json.contains("\"acceptBadgePhrases\""), 
            "Accept should include acceptBadgePhrases");
        assertTrue(json.contains("\"name\": \"respondingLine\""), 
            "Accept should include respondingLine");
        assertTrue(json.contains("\"name\": \"respondingUser\""), 
            "Accept should include respondingUser");
        assertTrue(json.contains("\"name\": \"responsePath\""), 
            "Accept should include responsePath");
        assertTrue(!json.contains("\"name\": \"decline\""), 
            "Accept only should not include decline parameter");
    }

    @Test
    void acceptEscalateSetsResponseTypeToAcceptDecline() throws Exception {
        String json = generateJsonForResponseOption("Accept,Escalate");
        
        assertTrue(json.contains("\"name\": \"responseType\""), 
            "Should have responseType parameter");
        assertTrue(json.contains("\"value\": \"\\\"Accept/Decline\\\"\""), 
            "Accept,Escalate should set responseType value to \\\"Accept/Decline\\\"");
        assertTrue(json.contains("\"name\": \"accept\""), 
            "Accept,Escalate should include accept parameter");
        assertTrue(json.contains("\"name\": \"decline\""), 
            "Accept,Escalate should include decline parameter");
        assertTrue(json.contains("\"acceptBadgePhrases\""), 
            "Accept,Escalate should include acceptBadgePhrases");
        assertTrue(json.contains("\"declineBadgePhrases\""), 
            "Accept,Escalate should include declineBadgePhrases");
        assertTrue(json.contains("\"name\": \"respondingLine\""), 
            "Accept,Escalate should include respondingLine");
    }

    @Test
    void acceptEscalateCallBackIncludesAllParameters() throws Exception {
        String json = generateJsonForResponseOption("Accept,Escalate,Call Back");
        
        assertTrue(json.contains("\"name\": \"responseType\""), 
            "Should have responseType parameter");
        assertTrue(json.contains("\"value\": \"\\\"Accept/Decline\\\"\""), 
            "Accept,Escalate,Call Back should set responseType value to \\\"Accept/Decline\\\"");
        assertTrue(json.contains("\"name\": \"callbackNumber\""), 
            "Should include callbackNumber parameter");
        assertTrue(json.contains("\"name\": \"accept\""), 
            "Should include accept parameter");
        assertTrue(json.contains("\"name\": \"decline\""), 
            "Should include decline parameter");
        assertTrue(json.contains("\"name\": \"acceptAndCall\""), 
            "Should include acceptAndCall parameter");
        assertTrue(json.contains("\"name\": \"respondingLine\""), 
            "Should include respondingLine");
        
        // Verify callbackNumber comes before accept
        int callbackPos = json.indexOf("\"name\": \"callbackNumber\"");
        int acceptPos = json.indexOf("\"name\": \"accept\"");
        assertTrue(callbackPos > 0 && acceptPos > callbackPos, 
            "callbackNumber should appear before accept");
    }

    @Test
    void responseOptionsIgnoresCaseAndWhitespace() throws Exception {
        String json = generateJsonForResponseOption("ACCEPT , escalate ,  Call Back");
        
        assertTrue(json.contains("\"name\": \"responseType\""), 
            "Should have responseType parameter");
        assertTrue(json.contains("\"value\": \"\\\"Accept/Decline\\\"\""), 
            "Mixed case and extra whitespace should still work");
        assertTrue(json.contains("\"name\": \"accept\""), 
            "Should include accept parameter");
        assertTrue(json.contains("\"name\": \"decline\""), 
            "Should include decline parameter");
        assertTrue(json.contains("\"name\": \"acceptAndCall\""), 
            "Should include acceptAndCall parameter");
    }

    @Test
    void ringtoneAppearsAfterResponseParametersBeforeBreakThrough() throws Exception {
        String json = generateJsonForResponseOption("Accept,Escalate");
        
        // Find positions in the JSON
        int alertSoundPos = json.indexOf("\"name\": \"alertSound\"");
        int responseTypePos = json.indexOf("\"name\": \"responseType\"");
        int acceptPos = json.indexOf("\"name\": \"accept\"");
        int declinePos = json.indexOf("\"name\": \"decline\"");
        int breakThroughPos = json.indexOf("\"name\": \"breakThrough\"");
        
        assertTrue(alertSoundPos > 0, "Should have alertSound parameter");
        assertTrue(responseTypePos > alertSoundPos, "responseType should be after alertSound");
        assertTrue(acceptPos > responseTypePos, "Accept should be after responseType");
        assertTrue(declinePos > acceptPos, "Decline should be after accept");
        assertTrue(breakThroughPos > declinePos, "breakThrough should be after response parameters");
    }

    @Test
    void emptyResponseOptionsDefaultsToNoResponse() throws Exception {
        String json = generateJsonForResponseOption("");
        
        assertTrue(json.contains("\"name\": \"responseType\""), 
            "Should have responseType parameter");
        assertTrue(json.contains("\"value\": \"\\\"None\\\"\""), 
            "Empty response options should default to None");
        assertTrue(!json.contains("\"name\": \"respondingLine\""), 
            "Empty response options should not include respondingLine");
    }

    /**
     * Helper method to generate JSON for a specific responseOption value
     */
    private String generateJsonForResponseOption(String responseOption) throws Exception {
        Path tempDir = Files.createTempDirectory("response-options-test");
        Path excelPath = tempDir.resolve("test.xlsx");
        Path jsonPath = tempDir.resolve("NurseCalls.json");

        createWorkbookWithResponseOption(excelPath, responseOption);

        // Use the parser directly instead of JobRunner
        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(excelPath.toFile());
        parser.writeNurseCallsJson(jsonPath.toFile());

        assertTrue(Files.exists(jsonPath), "NurseCalls.json should exist");
        
        return Files.readString(jsonPath);
    }

    private static void createWorkbookWithResponseOption(Path target, String responseOption) throws Exception {
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

            // Nurse Call sheet with response option
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
            nurseRow.createCell(3).setCellValue("High");
            nurseRow.createCell(4).setCellValue("Badge");
            nurseRow.createCell(5).setCellValue("Test Ringtone");
            nurseRow.createCell(6).setCellValue(responseOption);
            nurseRow.createCell(7).setCellValue("0");
            nurseRow.createCell(8).setCellValue("Nurse Team");

            try (OutputStream os = Files.newOutputStream(target)) {
                workbook.write(os);
            }
        }
    }
}
