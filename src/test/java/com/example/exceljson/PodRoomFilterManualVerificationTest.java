package com.example.exceljson;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

/**
 * Manual verification test for POD room filter feature.
 * This test creates a sample workbook and outputs the generated JSON
 * to show how the POD room filter condition is added to flows.
 */
class PodRoomFilterManualVerificationTest {

    @Test
    void demonstrateNursecallWithPodFilter() throws Exception {
        Path tempDir = Files.createTempDirectory("pod-filter-demo");
        Path excelPath = tempDir.resolve("input.xlsx");

        // Create sample workbook
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            // Unit Breakdown with POD filter
            Sheet units = workbook.createSheet("Unit Breakdown");
            Row unitsHeader = units.createRow(2);
            unitsHeader.createCell(0).setCellValue("Facility");
            unitsHeader.createCell(1).setCellValue("Common Unit Name");
            unitsHeader.createCell(2).setCellValue("Filter for POD Rooms (Optional)");
            unitsHeader.createCell(3).setCellValue("Nurse Call Configuration Group");
            Row unitsRow = units.createRow(3);
            unitsRow.createCell(0).setCellValue("TestFacility");
            unitsRow.createCell(1).setCellValue("ICU");
            unitsRow.createCell(2).setCellValue("POD 1");
            unitsRow.createCell(3).setCellValue("ICU-Group");

            // Nurse Call
            Sheet nurseCalls = workbook.createSheet("Nurse call");
            Row nurseHeader = nurseCalls.createRow(2);
            nurseHeader.createCell(0).setCellValue("Configuration Group");
            nurseHeader.createCell(1).setCellValue("Common Alert or Alarm Name");
            nurseHeader.createCell(2).setCellValue("Priority");
            nurseHeader.createCell(3).setCellValue("Time to 1st Recipient");
            nurseHeader.createCell(4).setCellValue("1st Recipient");
            
            Row nurseRow = nurseCalls.createRow(3);
            nurseRow.createCell(0).setCellValue("ICU-Group");
            nurseRow.createCell(1).setCellValue("Call Button");
            nurseRow.createCell(2).setCellValue("Normal");
            nurseRow.createCell(3).setCellValue("0");
            nurseRow.createCell(4).setCellValue("Nurse");

            // Empty Clinical and Order sheets
            Sheet clinicals = workbook.createSheet("Patient Monitoring");
            Row clinicalHeader = clinicals.createRow(2);
            clinicalHeader.createCell(0).setCellValue("Configuration Group");
            clinicalHeader.createCell(1).setCellValue("Common Alert or Alarm Name");
            
            Sheet orders = workbook.createSheet("Order");
            Row ordersHeader = orders.createRow(2);
            ordersHeader.createCell(0).setCellValue("Configuration Group");
            ordersHeader.createCell(1).setCellValue("Common Alert or Alarm Name");

            try (OutputStream os = Files.newOutputStream(excelPath)) {
                workbook.write(os);
            }
        }

        // Parse and generate JSON
        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(excelPath.toFile());
        
        Map<String, Object> json = parser.buildNurseCallsJson();
        
        // Pretty print the JSON
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        String prettyJson = mapper.writeValueAsString(json);
        
        System.out.println("\n=== NURSECALL JSON WITH POD ROOM FILTER 'POD 1' ===");
        System.out.println(prettyJson);
        System.out.println("======================\n");
        
        // Verify the conditions structure
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> flows = (List<Map<String, Object>>) json.get("deliveryFlows");
        Map<String, Object> flow = flows.get(0);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> conditions = (List<Map<String, Object>>) flow.get("conditions");
        
        System.out.println("Flow count: " + flows.size());
        System.out.println("Condition count: " + conditions.size());
        System.out.println("\nConditions:");
        for (int i = 0; i < conditions.size(); i++) {
            Map<String, Object> cond = conditions.get(i);
            System.out.println("  " + (i+1) + ". " + cond.get("name"));
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> filters = (List<Map<String, Object>>) cond.get("filters");
            for (Map<String, Object> filter : filters) {
                System.out.println("     - " + filter.get("attributePath") + " " + 
                                 filter.get("operator") + " " + filter.get("value"));
            }
        }
    }

    @Test
    void demonstrateClinicalWithPodFilter() throws Exception {
        Path tempDir = Files.createTempDirectory("pod-filter-clinical-demo");
        Path excelPath = tempDir.resolve("input.xlsx");

        // Create sample workbook
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            // Unit Breakdown with POD filter for clinicals
            Sheet units = workbook.createSheet("Unit Breakdown");
            Row unitsHeader = units.createRow(2);
            unitsHeader.createCell(0).setCellValue("Facility");
            unitsHeader.createCell(1).setCellValue("Common Unit Name");
            unitsHeader.createCell(2).setCellValue("Filter for POD Rooms (Optional)");
            unitsHeader.createCell(3).setCellValue("Patient Monitoring Configuration Group");
            Row unitsRow = units.createRow(3);
            unitsRow.createCell(0).setCellValue("TestFacility");
            unitsRow.createCell(1).setCellValue("Cardiac Care");
            unitsRow.createCell(2).setCellValue("POD A, POD B, POD C");
            unitsRow.createCell(3).setCellValue("Cardiac-Group");

            // Empty Nurse Call
            Sheet nurseCalls = workbook.createSheet("Nurse call");
            Row nurseHeader = nurseCalls.createRow(2);
            nurseHeader.createCell(0).setCellValue("Configuration Group");
            nurseHeader.createCell(1).setCellValue("Common Alert or Alarm Name");

            // Clinical
            Sheet clinicals = workbook.createSheet("Patient Monitoring");
            Row clinicalHeader = clinicals.createRow(2);
            clinicalHeader.createCell(0).setCellValue("Configuration Group");
            clinicalHeader.createCell(1).setCellValue("Common Alert or Alarm Name");
            clinicalHeader.createCell(2).setCellValue("Priority");
            clinicalHeader.createCell(3).setCellValue("Time to 1st Recipient");
            clinicalHeader.createCell(4).setCellValue("1st Recipient");
            
            Row clinicalRow = clinicals.createRow(3);
            clinicalRow.createCell(0).setCellValue("Cardiac-Group");
            clinicalRow.createCell(1).setCellValue("Arrhythmia Alert");
            clinicalRow.createCell(2).setCellValue("Urgent");
            clinicalRow.createCell(3).setCellValue("0");
            clinicalRow.createCell(4).setCellValue("Cardiologist");
            
            Sheet orders = workbook.createSheet("Order");
            Row ordersHeader = orders.createRow(2);
            ordersHeader.createCell(0).setCellValue("Configuration Group");
            ordersHeader.createCell(1).setCellValue("Common Alert or Alarm Name");

            try (OutputStream os = Files.newOutputStream(excelPath)) {
                workbook.write(os);
            }
        }

        // Parse and generate JSON
        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(excelPath.toFile());
        
        Map<String, Object> json = parser.buildClinicalsJson();
        
        // Pretty print the JSON
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        String prettyJson = mapper.writeValueAsString(json);
        
        System.out.println("\n=== CLINICAL JSON WITH POD ROOM FILTER 'POD A, POD B, POD C' ===");
        System.out.println(prettyJson);
        System.out.println("======================\n");
        
        // Verify the conditions structure
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> flows = (List<Map<String, Object>>) json.get("deliveryFlows");
        Map<String, Object> flow = flows.get(0);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> conditions = (List<Map<String, Object>>) flow.get("conditions");
        
        System.out.println("Flow count: " + flows.size());
        System.out.println("Condition count: " + conditions.size());
        System.out.println("\nConditions:");
        for (int i = 0; i < conditions.size(); i++) {
            Map<String, Object> cond = conditions.get(i);
            System.out.println("  " + (i+1) + ". " + cond.get("name"));
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> filters = (List<Map<String, Object>>) cond.get("filters");
            for (Map<String, Object> filter : filters) {
                System.out.println("     - " + filter.get("attributePath") + " " + 
                                 filter.get("operator") + " " + filter.get("value"));
            }
        }
    }
}
