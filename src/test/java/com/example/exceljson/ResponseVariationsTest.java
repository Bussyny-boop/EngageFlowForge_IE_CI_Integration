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
 * Tests for expanded response option variations:
 * - "Acknowledge" as alternative to "Accept"
 * - "Decline"/"Declined" as alternative to "Escalate"
 */
class ResponseVariationsTest {

    @Test
    void acknowledgeSetsAcceptBadgePhrasesToAcknowledge() throws Exception {
        String json = generateJsonForResponseOption("Acknowledge");
        
        assertTrue(json.contains("\"name\": \"responseType\""), 
            "Should have responseType parameter");
        assertTrue(json.contains("\"value\": \"\\\"Accept/Decline\\\"\""), 
            "Acknowledge should set responseType value to \\\"Accept/Decline\\\"");
        assertTrue(json.contains("\"name\": \"accept\""), 
            "Acknowledge should include accept parameter");
        assertTrue(json.contains("\"acceptBadgePhrases\""), 
            "Acknowledge should include acceptBadgePhrases");
        assertTrue(json.contains("\"value\": \"[\\\"Acknowledge\\\"]\""), 
            "Acknowledge should set acceptBadgePhrases to [\\\"Acknowledge\\\"]");
        assertTrue(json.contains("\"name\": \"respondingLine\""), 
            "Acknowledge should include respondingLine");
    }

    @Test
    void acknowledgedSetsAcceptBadgePhrasesToAcknowledge() throws Exception {
        String json = generateJsonForResponseOption("Acknowledged");
        
        assertTrue(json.contains("\"value\": \"[\\\"Acknowledge\\\"]\""), 
            "Acknowledged should also set acceptBadgePhrases to [\\\"Acknowledge\\\"]");
    }

    @Test
    void declineSetsDeclineBadgePhrasesToDecline() throws Exception {
        String json = generateJsonForResponseOption("Accept, Decline");
        
        assertTrue(json.contains("\"name\": \"responseType\""), 
            "Should have responseType parameter");
        assertTrue(json.contains("\"value\": \"\\\"Accept/Decline\\\"\""), 
            "Accept,Decline should set responseType value to \\\"Accept/Decline\\\"");
        assertTrue(json.contains("\"name\": \"accept\""), 
            "Should include accept parameter");
        assertTrue(json.contains("\"name\": \"decline\""), 
            "Should include decline parameter");
        assertTrue(json.contains("\"acceptBadgePhrases\""), 
            "Should include acceptBadgePhrases");
        assertTrue(json.contains("\"value\": \"[\\\"Accept\\\"]\""), 
            "Should set acceptBadgePhrases to [\\\"Accept\\\"]");
        assertTrue(json.contains("\"declineBadgePhrases\""), 
            "Should include declineBadgePhrases");
        assertTrue(json.contains("\"value\": \"[\\\"Decline\\\"]\""), 
            "Decline should set declineBadgePhrases to [\\\"Decline\\\"]");
    }

    @Test
    void declinedSetsDeclineBadgePhrasesToDecline() throws Exception {
        String json = generateJsonForResponseOption("Accept, Declined");
        
        assertTrue(json.contains("\"value\": \"[\\\"Decline\\\"]\""), 
            "Declined should also set declineBadgePhrases to [\\\"Decline\\\"]");
    }

    @Test
    void acknowledgeDeclineCombination() throws Exception {
        String json = generateJsonForResponseOption("Acknowledge, Decline");
        
        assertTrue(json.contains("\"name\": \"responseType\""), 
            "Should have responseType parameter");
        assertTrue(json.contains("\"value\": \"\\\"Accept/Decline\\\"\""), 
            "Acknowledge,Decline should set responseType value to \\\"Accept/Decline\\\"");
        assertTrue(json.contains("\"name\": \"accept\""), 
            "Should include accept parameter");
        assertTrue(json.contains("\"name\": \"decline\""), 
            "Should include decline parameter");
        assertTrue(json.contains("\"acceptBadgePhrases\""), 
            "Should include acceptBadgePhrases");
        assertTrue(json.contains("\"value\": \"[\\\"Acknowledge\\\"]\""), 
            "Should set acceptBadgePhrases to [\\\"Acknowledge\\\"]");
        assertTrue(json.contains("\"declineBadgePhrases\""), 
            "Should include declineBadgePhrases");
        assertTrue(json.contains("\"value\": \"[\\\"Decline\\\"]\""), 
            "Should set declineBadgePhrases to [\\\"Decline\\\"]");
    }

    @Test
    void acknowledgeDeclineCallBackIncludesAllParameters() throws Exception {
        String json = generateJsonForResponseOption("Acknowledge, Decline, Call Back");
        
        assertTrue(json.contains("\"name\": \"responseType\""), 
            "Should have responseType parameter");
        assertTrue(json.contains("\"value\": \"\\\"Accept/Decline\\\"\""), 
            "Should set responseType value to \\\"Accept/Decline\\\"");
        assertTrue(json.contains("\"name\": \"callbackNumber\""), 
            "Should include callbackNumber parameter");
        assertTrue(json.contains("\"value\": \"\\\"#{bed.pillow_number}\\\"\""), 
            "callbackNumber should have value #{bed.pillow_number}");
        assertTrue(json.contains("\"name\": \"accept\""), 
            "Should include accept parameter");
        assertTrue(json.contains("\"name\": \"decline\""), 
            "Should include decline parameter");
        assertTrue(json.contains("\"name\": \"acceptAndCall\""), 
            "Should include acceptAndCall parameter");
        assertTrue(json.contains("\"acceptBadgePhrases\""), 
            "Should include acceptBadgePhrases");
        assertTrue(json.contains("\"value\": \"[\\\"Acknowledge\\\"]\""), 
            "Should set acceptBadgePhrases to [\\\"Acknowledge\\\"]");
        assertTrue(json.contains("\"declineBadgePhrases\""), 
            "Should include declineBadgePhrases");
        assertTrue(json.contains("\"value\": \"[\\\"Decline\\\"]\""), 
            "Should set declineBadgePhrases to [\\\"Decline\\\"]");
    }

    @Test
    void mixedOldAndNewVariationsWork() throws Exception {
        // Mix Accept with Decline
        String json1 = generateJsonForResponseOption("Accept, Decline");
        assertTrue(json1.contains("\"value\": \"[\\\"Accept\\\"]\""), 
            "Accept should use Accept badge phrase");
        assertTrue(json1.contains("\"value\": \"[\\\"Decline\\\"]\""), 
            "Decline should use Decline badge phrase");

        // Mix Acknowledge with Escalate
        String json2 = generateJsonForResponseOption("Acknowledge, Escalate");
        assertTrue(json2.contains("\"value\": \"[\\\"Acknowledge\\\"]\""), 
            "Acknowledge should use Acknowledge badge phrase");
        assertTrue(json2.contains("\"value\": \"[\\\"Escalate\\\"]\""), 
            "Escalate should use Escalate badge phrase");
    }

    @Test
    void caseInsensitiveVariationsWork() throws Exception {
        String json1 = generateJsonForResponseOption("ACKNOWLEDGE");
        assertTrue(json1.contains("\"value\": \"[\\\"Acknowledge\\\"]\""), 
            "ACKNOWLEDGE in caps should work");

        String json2 = generateJsonForResponseOption("Accept, decline");
        assertTrue(json2.contains("\"value\": \"[\\\"Decline\\\"]\""), 
            "decline in lowercase should work with accept");

        String json3 = generateJsonForResponseOption("Accept, DECLINED");
        assertTrue(json3.contains("\"value\": \"[\\\"Decline\\\"]\""), 
            "DECLINED in caps should work");
    }

    /**
     * Helper method to generate JSON for a specific responseOption value
     */
    private String generateJsonForResponseOption(String responseOption) throws Exception {
        Path tempDir = Files.createTempDirectory("response-variations-test");
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
