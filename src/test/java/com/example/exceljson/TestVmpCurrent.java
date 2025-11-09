package com.example.exceljson;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public class TestVmpCurrent {
    public static void main(String[] args) throws Exception {
        Path tempDir = Files.createTempDirectory("vmp-test");
        File excelFile = tempDir.resolve("test.xlsx").toFile();

        // Create test workbook with VCS device
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet units = workbook.createSheet("Unit Breakdown");
            Row unitsHeader = units.createRow(2);
            unitsHeader.createCell(0).setCellValue("Facility");
            unitsHeader.createCell(1).setCellValue("Common Unit Name");
            unitsHeader.createCell(2).setCellValue("Nurse Call Configuration Group");
            Row unitsRow = units.createRow(3);
            unitsRow.createCell(0).setCellValue("Test Facility");
            unitsRow.createCell(1).setCellValue("Test Unit");
            unitsRow.createCell(2).setCellValue("TestGroup");

            Sheet nurseCalls = workbook.createSheet("Nurse call");
            Row nurseHeader = nurseCalls.createRow(2);
            nurseHeader.createCell(0).setCellValue("Configuration Group");
            nurseHeader.createCell(1).setCellValue("Common Alert or Alarm Name");
            nurseHeader.createCell(3).setCellValue("Priority");
            nurseHeader.createCell(4).setCellValue("Device - A");
            nurseHeader.createCell(5).setCellValue("Ringtone");
            nurseHeader.createCell(10).setCellValue("Time to 1st Recipient");
            nurseHeader.createCell(11).setCellValue("1st Recipient");
            
            Row nurseRow = nurseCalls.createRow(3);
            nurseRow.createCell(0).setCellValue("TestGroup");
            nurseRow.createCell(1).setCellValue("Test VCS Alarm");
            nurseRow.createCell(3).setCellValue("Normal");
            nurseRow.createCell(4).setCellValue("VCS");
            nurseRow.createCell(5).setCellValue("list_pagers");
            nurseRow.createCell(10).setCellValue("0");
            nurseRow.createCell(11).setCellValue("Nurse Team");

            try (FileOutputStream fos = new FileOutputStream(excelFile)) {
                workbook.write(fos);
            }
        }

        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(excelFile);
        
        var nurseJson = parser.buildNurseCallsJson();
        var flows = (List<?>) nurseJson.get("deliveryFlows");
        var flow = (Map<?, ?>) flows.get(0);
        
        System.out.println("Interface:");
        var interfaces = (List<?>) flow.get("interfaces");
        for (Object ifaceObj : interfaces) {
            var iface = (Map<?, ?>) ifaceObj;
            System.out.println("  componentName: " + iface.get("componentName"));
            System.out.println("  referenceName: " + iface.get("referenceName"));
        }
        
        System.out.println("\nParameter Attributes:");
        var params = (List<?>) flow.get("parameterAttributes");
        for (Object paramObj : params) {
            var param = (Map<?, ?>) paramObj;
            String name = (String) param.get("name");
            if (name.contains("alert") || name.contains("Alert")) {
                System.out.println("  " + name + ": " + param.get("value"));
            }
        }
        
        // Clean up
        Files.deleteIfExists(excelFile.toPath());
        Files.deleteIfExists(tempDir);
    }
}
