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

class VerifyRetractRulesTest {

    @Test
    void xmppDoesNotHaveRetractRules() throws Exception {
        Path tempDir = Files.createTempDirectory("xmpp-retract-test");
        File excelFile = tempDir.resolve("test.xlsx").toFile();

        createTestWorkbook(excelFile, "XMPP", "ringtone");

        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(excelFile);

        var nurseJson = parser.buildNurseCallsJson();
        var flows = (List<?>) nurseJson.get("deliveryFlows");
        var flow = (Map<?, ?>) flows.getFirst();
        var params = (List<?>) flow.get("parameterAttributes");

        // Verify that retractRules is NOT in XMPP parameters
        boolean hasRetractRules = params.stream()
            .anyMatch(p -> "retractRules".equals(((Map<?, ?>) p).get("name")));
        assertFalse(hasRetractRules, "XMPP should NOT have retractRules parameter");

        Files.deleteIfExists(excelFile.toPath());
        Files.deleteIfExists(tempDir);
    }

    @Test
    void vmpHasRetractRules() throws Exception {
        Path tempDir = Files.createTempDirectory("vmp-retract-test");
        File excelFile = tempDir.resolve("test.xlsx").toFile();

        createTestWorkbook(excelFile, "Edge", "ringtone");

        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(excelFile);

        var nurseJson = parser.buildNurseCallsJson();
        var flows = (List<?>) nurseJson.get("deliveryFlows");
        var flow = (Map<?, ?>) flows.getFirst();
        var params = (List<?>) flow.get("parameterAttributes");

        // Verify that retractRules IS in VMP/Edge parameters
        boolean hasRetractRules = params.stream()
            .anyMatch(p -> "retractRules".equals(((Map<?, ?>) p).get("name")));
        assertTrue(hasRetractRules, "VMP/Edge should have retractRules parameter");

        Files.deleteIfExists(excelFile.toPath());
        Files.deleteIfExists(tempDir);
    }

    private void createTestWorkbook(File excelFile, String deviceName, String ringtone) throws Exception {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet unitSheet = wb.createSheet("Unit Breakdown");
            Row unitHeader = unitSheet.createRow(2);
            unitHeader.createCell(0).setCellValue("Facility");
            unitHeader.createCell(1).setCellValue("Common Unit Name");
            unitHeader.createCell(2).setCellValue("Nurse Call Configuration Group");

            Row unitRow = unitSheet.createRow(3);
            unitRow.createCell(0).setCellValue("Test Facility");
            unitRow.createCell(1).setCellValue("Test Unit");
            unitRow.createCell(2).setCellValue("TestGroup");

            Sheet nurseSheet = wb.createSheet("Nurse Call");
            Row nurseHeader = nurseSheet.createRow(2);
            nurseHeader.createCell(0).setCellValue("Configuration Group");
            nurseHeader.createCell(1).setCellValue("Common Alert or Alarm Name");
            nurseHeader.createCell(3).setCellValue("Priority");
            nurseHeader.createCell(4).setCellValue("Device - A");
            nurseHeader.createCell(5).setCellValue("Ringtone Device - A");
            nurseHeader.createCell(10).setCellValue("Time to 1st Recipient");
            nurseHeader.createCell(11).setCellValue("1st Recipient");

            Row nurseRow = nurseSheet.createRow(3);
            nurseRow.createCell(0).setCellValue("TestGroup");
            nurseRow.createCell(1).setCellValue("Test Alarm");
            nurseRow.createCell(3).setCellValue("Normal");
            nurseRow.createCell(4).setCellValue(deviceName);
            nurseRow.createCell(5).setCellValue(ringtone);
            nurseRow.createCell(10).setCellValue("0");
            nurseRow.createCell(11).setCellValue("Nurse Team");

            Sheet clinicalSheet = wb.createSheet("Patient Monitoring");
            Row clinicalHeader = clinicalSheet.createRow(2);
            clinicalHeader.createCell(0).setCellValue("Configuration Group");
            clinicalHeader.createCell(1).setCellValue("Common Alert or Alarm Name");

            try (FileOutputStream fos = new FileOutputStream(excelFile)) {
                wb.write(fos);
            }
        }
    }
}
