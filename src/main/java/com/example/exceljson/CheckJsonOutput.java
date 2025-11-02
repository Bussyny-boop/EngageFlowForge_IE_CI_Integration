package com.example.exceljson;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class CheckJsonOutput {

    private static void createTestWorkbook(File file) throws Exception {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            // Unit Breakdown sheet
            Sheet unitSheet = wb.createSheet("Unit Breakdown");
            Row unitHeader = unitSheet.createRow(2);
            unitHeader.createCell(0).setCellValue("Facility");
            unitHeader.createCell(1).setCellValue("Common Unit Name");
            unitHeader.createCell(2).setCellValue("Nurse Call Configuration Group");
            unitHeader.createCell(3).setCellValue("Patient Monitoring Configuration Group");
            unitHeader.createCell(4).setCellValue("No Caregiver Group");

            Row unitData = unitSheet.createRow(3);
            unitData.createCell(0).setCellValue("Test Facility");
            unitData.createCell(1).setCellValue("Test Unit");
            unitData.createCell(2).setCellValue("Nurse Group 1");
            unitData.createCell(3).setCellValue("Clinical Group 1");
            unitData.createCell(4).setCellValue("No Caregiver Group");

            // Nurse Call sheet with EMDAN column
            Sheet nurseSheet = wb.createSheet("Nurse Call");
            Row nurseHeader = nurseSheet.createRow(2);
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
            nurseHeader.createCell(10).setCellValue("Genie Enunciation");
            nurseHeader.createCell(11).setCellValue("EMDAN Compliant? (Y/N)");
            nurseHeader.createCell(12).setCellValue("Time to 1st Recipient");
            nurseHeader.createCell(13).setCellValue("1st Recipient");

            // EMDAN-compliant alarm that should be moved to clinicals
            Row nurseData1 = nurseSheet.createRow(3);
            nurseData1.createCell(0).setCellValue("Nurse Group 1");
            nurseData1.createCell(1).setCellValue("EMDAN Alarm");
            nurseData1.createCell(2).setCellValue("System EMDAN Alarm");
            nurseData1.createCell(3).setCellValue("Normal");
            nurseData1.createCell(4).setCellValue("Badge");
            nurseData1.createCell(5).setCellValue("Ringtone 1");
            nurseData1.createCell(6).setCellValue("Accept");
            nurseData1.createCell(7).setCellValue("No");
            nurseData1.createCell(8).setCellValue("");
            nurseData1.createCell(9).setCellValue("10");
            nurseData1.createCell(10).setCellValue("Yes");
            nurseData1.createCell(11).setCellValue("Y");  // EMDAN compliant
            nurseData1.createCell(12).setCellValue("0");
            nurseData1.createCell(13).setCellValue("vGroup:Test Group");

            // Patient Monitoring sheet (empty for this test)
            Sheet clinicalSheet = wb.createSheet("Patient Monitoring");
            Row clinicalHeader = clinicalSheet.createRow(2);
            clinicalHeader.createCell(0).setCellValue("Configuration Group");
            clinicalHeader.createCell(1).setCellValue("Common Alert or Alarm Name");
            clinicalHeader.createCell(2).setCellValue("EMDAN Compliant? (Y/N)");

            try (FileOutputStream fos = new FileOutputStream(file)) {
                wb.write(fos);
            }
        }
    }

    public static void main(String[] args) throws Exception {
        Path tempDir = Files.createTempDirectory("emdan-json-test");
        File excelFile = tempDir.resolve("test.xlsx").toFile();

        createTestWorkbook(excelFile);

        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(excelFile);

        // Build the clinicals JSON
        Map<String, Object> clinicalsJson = parser.buildClinicalsJson();
        
        // Pretty print the JSON
        String json = ExcelParserV5.pretty(clinicalsJson);
        System.out.println("=== CLINICALS JSON ===");
        System.out.println(json);
        
        // Check if shortMessage appears
        if (json.contains("shortMessage")) {
            System.out.println("\n✅ shortMessage is present in the JSON");
        } else {
            System.out.println("\n❌ shortMessage is MISSING from the JSON");
        }
    }
}
