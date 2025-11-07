package com.example.exceljson;

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
 * Tests for "Reject" response option handling:
 * - "Reject" should be treated as a decline phrase
 * - Priority: Decline > Reject > Escalate for decline badge phrases
 */
class RejectResponseTest {

    @Test
    void rejectSetsDeclineBadgePhrasesToReject() throws Exception {
        String json = generateJsonForResponseOption("Accept, Reject", "NurseCalls");
        
        assertTrue(json.contains("\"name\": \"responseType\""), 
            "Should have responseType parameter");
        assertTrue(json.contains("\"value\": \"\\\"Accept/Decline\\\"\""), 
            "Accept,Reject should set responseType value to \\\"Accept/Decline\\\"");
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
        assertTrue(json.contains("\"value\": \"[\\\"Reject\\\"]\""), 
            "Reject should set declineBadgePhrases to [\\\"Reject\\\"]");
    }

    @Test
    void acknowledgeRejectCombination() throws Exception {
        String json = generateJsonForResponseOption("Acknowledge, Reject", "NurseCalls");
        
        assertTrue(json.contains("\"name\": \"responseType\""), 
            "Should have responseType parameter");
        assertTrue(json.contains("\"value\": \"\\\"Accept/Decline\\\"\""), 
            "Acknowledge,Reject should set responseType value to \\\"Accept/Decline\\\"");
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
        assertTrue(json.contains("\"value\": \"[\\\"Reject\\\"]\""), 
            "Should set declineBadgePhrases to [\\\"Reject\\\"]");
    }

    @Test
    void declineHasPriorityOverReject() throws Exception {
        // When both Decline and Reject are present, Decline takes priority
        String json = generateJsonForResponseOption("Accept, Decline, Reject", "NurseCalls");
        
        assertTrue(json.contains("\"declineBadgePhrases\""), 
            "Should include declineBadgePhrases");
        assertTrue(json.contains("\"value\": \"[\\\"Decline\\\"]\""), 
            "Decline should take priority over Reject in declineBadgePhrases");
    }

    @Test
    void rejectHasPriorityOverEscalate() throws Exception {
        // When both Reject and Escalate are present, Reject takes priority
        String json = generateJsonForResponseOption("Accept, Reject, Escalate", "NurseCalls");
        
        assertTrue(json.contains("\"declineBadgePhrases\""), 
            "Should include declineBadgePhrases");
        assertTrue(json.contains("\"value\": \"[\\\"Reject\\\"]\""), 
            "Reject should take priority over Escalate in declineBadgePhrases");
    }

    @Test
    void ordersFlowSupportsResponseOptions() throws Exception {
        // Orders flow should now support response options just like Clinicals
        String json = generateJsonForResponseOption("Accept, Escalate", "Orders");
        
        assertTrue(json.contains("\"name\": \"responseType\""), 
            "Orders should have responseType parameter");
        assertTrue(json.contains("\"value\": \"\\\"Accept/Decline\\\"\""), 
            "Orders Accept,Escalate should set responseType value to \\\"Accept/Decline\\\"");
        assertTrue(json.contains("\"acceptBadgePhrases\""), 
            "Orders should include acceptBadgePhrases");
        assertTrue(json.contains("\"value\": \"[\\\"Accept\\\"]\""), 
            "Orders should set acceptBadgePhrases to [\\\"Accept\\\"]");
        assertTrue(json.contains("\"declineBadgePhrases\""), 
            "Orders should include declineBadgePhrases");
        assertTrue(json.contains("\"value\": \"[\\\"Escalate\\\"]\""), 
            "Orders should set declineBadgePhrases to [\\\"Escalate\\\"]");
    }

    @Test
    void ordersFlowWithReject() throws Exception {
        // Orders flow should handle Reject option
        String json = generateJsonForResponseOption("Acknowledge, Reject", "Orders");
        
        assertTrue(json.contains("\"acceptBadgePhrases\""), 
            "Orders should include acceptBadgePhrases");
        assertTrue(json.contains("\"value\": \"[\\\"Acknowledge\\\"]\""), 
            "Orders should set acceptBadgePhrases to [\\\"Acknowledge\\\"]");
        assertTrue(json.contains("\"declineBadgePhrases\""), 
            "Orders should include declineBadgePhrases");
        assertTrue(json.contains("\"value\": \"[\\\"Reject\\\"]\""), 
            "Orders should set declineBadgePhrases to [\\\"Reject\\\"]");
    }

    @Test
    void clinicalsFlowWithReject() throws Exception {
        // Clinicals flow should also handle Reject option
        String json = generateJsonForResponseOption("Accept, Reject", "Clinicals");
        
        assertTrue(json.contains("\"acceptBadgePhrases\""), 
            "Clinicals should include acceptBadgePhrases");
        assertTrue(json.contains("\"value\": \"[\\\"Accept\\\"]\""), 
            "Clinicals should set acceptBadgePhrases to [\\\"Accept\\\"]");
        assertTrue(json.contains("\"declineBadgePhrases\""), 
            "Clinicals should include declineBadgePhrases");
        assertTrue(json.contains("\"value\": \"[\\\"Reject\\\"]\""), 
            "Clinicals should set declineBadgePhrases to [\\\"Reject\\\"]");
    }

    // Helper method to generate JSON for a specific response option and flow type
    private String generateJsonForResponseOption(String responseOption, String flowType) throws Exception {
        Path tmpDir = Files.createTempDirectory("reject-test");
        File xlsxFile = tmpDir.resolve("test.xlsx").toFile();
        File jsonFile = tmpDir.resolve("test.json").toFile();

        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            // Create Unit Breakdown sheet
            Sheet unitSheet = wb.createSheet("Unit Breakdown");
            Row headerRow = unitSheet.createRow(0);
            headerRow.createCell(0).setCellValue("Facility");
            headerRow.createCell(1).setCellValue("Unit Names");
            headerRow.createCell(2).setCellValue("Nurse Group");
            headerRow.createCell(3).setCellValue("Clinical Group");
            headerRow.createCell(4).setCellValue("Orders Group");

            Row dataRow = unitSheet.createRow(1);
            dataRow.createCell(0).setCellValue("TestFacility");
            dataRow.createCell(1).setCellValue("TestUnit");
            dataRow.createCell(2).setCellValue("TestGroup");
            dataRow.createCell(3).setCellValue("TestGroup");
            dataRow.createCell(4).setCellValue("TestGroup");

            // Create the appropriate flow sheet based on flowType
            String sheetName;
            if (flowType.equals("NurseCalls")) {
                sheetName = "Nurse Call";
            } else if (flowType.equals("Clinicals")) {
                sheetName = "Patient Monitoring";
            } else {
                sheetName = "Order";  // Orders sheet name
            }
            
            Sheet flowSheet = wb.createSheet(sheetName);
            Row flowHeader = flowSheet.createRow(0);
            flowHeader.createCell(0).setCellValue("Configuration Group");
            flowHeader.createCell(1).setCellValue("Common Alert or Alarm Name");
            flowHeader.createCell(2).setCellValue("Sending System Alert Name");
            flowHeader.createCell(3).setCellValue("Priority");
            flowHeader.createCell(4).setCellValue("Device-A");
            flowHeader.createCell(5).setCellValue("Ringtone");
            flowHeader.createCell(6).setCellValue("Response Options");

            Row flowData = flowSheet.createRow(1);
            flowData.createCell(0).setCellValue("TestGroup");
            flowData.createCell(1).setCellValue("TestAlarm");
            flowData.createCell(2).setCellValue("TestSending");
            flowData.createCell(3).setCellValue("Urgent");
            flowData.createCell(4).setCellValue("Edge");
            flowData.createCell(5).setCellValue("Alert1");
            flowData.createCell(6).setCellValue(responseOption);

            try (OutputStream os = Files.newOutputStream(xlsxFile.toPath())) {
                wb.write(os);
            }
        }

        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(xlsxFile);
        
        if (flowType.equals("NurseCalls")) {
            parser.writeNurseCallsJson(jsonFile, false);
        } else if (flowType.equals("Clinicals")) {
            parser.writeClinicalsJson(jsonFile, false);
        } else {
            parser.writeOrdersJson(jsonFile, false);
        }
        
        String json = Files.readString(jsonFile.toPath());
        
        // Cleanup
        xlsxFile.delete();
        jsonFile.delete();
        tmpDir.toFile().delete();
        
        return json;
    }
}
