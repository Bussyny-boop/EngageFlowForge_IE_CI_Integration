package com.example.exceljson;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

/**
 * Tests for dynamic parsing of responseOptions field for Clinical (Patient Monitoring) flows
 */
class ClinicalResponseOptionsTest {

    @Test
    void clinicalNoResponseSetsResponseTypeToNone() throws Exception {
        String json = generateJsonForClinicalResponseOption("No Response");
        
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
    void clinicalAcceptOnlySetsResponseTypeToAccept() throws Exception {
        String json = generateJsonForClinicalResponseOption("Accept");
        
        assertTrue(json.contains("\"name\": \"responseType\""), 
            "Should have responseType parameter");
        assertTrue(json.contains("\"value\": \"\\\"Accept/Decline\\\"\""), 
            "Accept only should set responseType value to \\\"Accept/Decline\\\"");
        assertTrue(json.contains("\"name\": \"accept\""), 
            "Accept should include accept parameter");
        assertTrue(json.contains("\"acceptBadgePhrases\""), 
            "Accept should include acceptBadgePhrases");
        assertTrue(json.contains("\"name\": \"respondingLine\""), 
            "Accept only should include respondingLine");
        assertTrue(json.contains("\"name\": \"respondingUser\""), 
            "Accept only should include respondingUser");
        assertTrue(json.contains("\"name\": \"responsePath\""), 
            "Accept only should include responsePath");
        assertTrue(!json.contains("\"name\": \"decline\""), 
            "Accept only should not include decline parameter");
    }

    @Test
    void clinicalAcceptEscalateSetsResponseTypeToAcceptDecline() throws Exception {
        String json = generateJsonForClinicalResponseOption("Accept,Escalate");
        
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
    void clinicalAcceptEscalateCallBackIncludesAllParameters() throws Exception {
        String json = generateJsonForClinicalResponseOption("Accept,Escalate,Call Back");
        
        assertTrue(json.contains("\"name\": \"responseType\""), 
            "Should have responseType parameter");
        assertTrue(json.contains("\"value\": \"\\\"Accept/Decline\\\"\""), 
            "Accept,Escalate,Call Back should set responseType value to \\\"Accept/Decline\\\"");
        assertTrue(json.contains("\"name\": \"callbackNumber\""), 
            "Should include callbackNumber parameter");
        assertTrue(json.contains("\"name\": \"accept\""), 
            "Should include accept parameter");
        assertTrue(json.contains("\"name\": \"decline\""), 
            "Should include decline parameter with text 'Decline Primary'");
        assertTrue(json.contains("\"value\": \"\\\"Decline Primary\\\"\""), 
            "Should have decline value 'Decline Primary' for all three options");
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
    void clinicalResponseOptionsIgnoresCaseAndWhitespace() throws Exception {
        String json = generateJsonForClinicalResponseOption("ACCEPT , escalate ,  Call Back");
        
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
    void clinicalRingtoneAppearsBeforeResponseType() throws Exception {
        String json = generateJsonForClinicalResponseOption("Accept,Escalate");
        
        // Find positions in the JSON
        int alertSoundPos = json.indexOf("\"name\": \"alertSound\"");
        int responseTypePos = json.indexOf("\"name\": \"responseType\"");
        int breakThroughPos = json.indexOf("\"name\": \"breakThrough\"");
        
        assertTrue(alertSoundPos > 0, "Should have alertSound parameter");
        assertTrue(responseTypePos > alertSoundPos, "responseType should be after alertSound");
        assertTrue(breakThroughPos > responseTypePos, "breakThrough should be after responseType");
    }

    @Test
    void clinicalEmptyResponseOptionsDefaultsToNoResponse() throws Exception {
        String json = generateJsonForClinicalResponseOption("");
        
        assertTrue(json.contains("\"name\": \"responseType\""), 
            "Should have responseType parameter");
        assertTrue(json.contains("\"value\": \"\\\"None\\\"\""), 
            "Empty response options should default to None");
        assertTrue(!json.contains("\"name\": \"respondingLine\""), 
            "Empty response options should not include respondingLine");
    }

    @Test
    void clinicalMessageContainsClinicalSpecificFields() throws Exception {
        String json = generateJsonForClinicalResponseOption("Accept");
        
        // Verify Clinical-specific message template
        assertTrue(json.contains("Clinical Alert"), 
            "Message should contain 'Clinical Alert'");
        assertTrue(json.contains("#{alert_type}"), 
            "Message should contain alert_type placeholder");
        assertTrue(json.contains("#{alarm_time.as_time}"), 
            "Message should contain alarm_time placeholder");
        assertTrue(json.contains("#{clinical_patient.mrn}"), 
            "Should use clinical_patient.mrn instead of bed.patient.mrn");
    }

    /**
     * Helper method to generate JSON for a specific Clinical responseOption value
     */
    private String generateJsonForClinicalResponseOption(String responseOption) throws Exception {
        Path tempDir = Files.createTempDirectory("clinical-response-options-test");
        Path excelPath = tempDir.resolve("test.xlsx");
        Path jsonPath = tempDir.resolve("Clinicals.json");

        createWorkbookWithClinicalResponseOption(excelPath, responseOption);

        // Use the parser directly
        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(excelPath.toFile());
        parser.writeClinicalsJson(jsonPath.toFile());

        assertTrue(Files.exists(jsonPath), "Clinicals.json should exist");
        
        return Files.readString(jsonPath);
    }

    private static void createWorkbookWithClinicalResponseOption(Path target, String responseOption) throws Exception {
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

            // Patient Monitoring sheet with response option
            Sheet clinicals = workbook.createSheet("Patient Monitoring");
            Row clinicalHeader = clinicals.createRow(0);
            clinicalHeader.createCell(0).setCellValue("Configuration Group");
            clinicalHeader.createCell(1).setCellValue("Common Alert or Alarm Name");
            clinicalHeader.createCell(2).setCellValue("Sending System Alert Name");
            clinicalHeader.createCell(3).setCellValue("Priority");
            clinicalHeader.createCell(4).setCellValue("Device - A");
            clinicalHeader.createCell(5).setCellValue("Ringtone Device - A");
            clinicalHeader.createCell(6).setCellValue("Response Options");
            clinicalHeader.createCell(7).setCellValue("Time to 1st Recipient");
            clinicalHeader.createCell(8).setCellValue("1st Recipient");
            
            Row clinicalRow = clinicals.createRow(1);
            clinicalRow.createCell(0).setCellValue("TestGroup");
            clinicalRow.createCell(1).setCellValue("Test Clinical Alarm");
            clinicalRow.createCell(2).setCellValue("");
            clinicalRow.createCell(3).setCellValue("High");
            clinicalRow.createCell(4).setCellValue("Badge");
            clinicalRow.createCell(5).setCellValue("Test Clinical Ringtone");
            clinicalRow.createCell(6).setCellValue(responseOption);
            clinicalRow.createCell(7).setCellValue("0");
            clinicalRow.createCell(8).setCellValue("Clinical Team");

            try (OutputStream os = Files.newOutputStream(target)) {
                workbook.write(os);
            }
        }
    }
}
