package com.example.exceljson;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

/**
 * Tests for priority mapping in ExcelParserV5.
 * Validates that priorities are correctly mapped from Excel to JSON output.
 */
class PriorityMappingTest {

    @Test
    void lowPriorityMapsToNormal() throws Exception {
        String json = generateJsonWithPriority("Low");
        assertTrue(json.contains("\"priority\": \"normal\""), 
            "Low priority should map to 'normal'");
    }

    @Test
    void lPriorityMapsToNormal() throws Exception {
        String json = generateJsonWithPriority("L");
        assertTrue(json.contains("\"priority\": \"normal\""), 
            "L priority should map to 'normal'");
    }

    @Test
    void mediumPriorityMapsToHigh() throws Exception {
        String json = generateJsonWithPriority("Medium");
        assertTrue(json.contains("\"priority\": \"high\""), 
            "Medium priority should map to 'high'");
    }

    @Test
    void medPriorityMapsToHigh() throws Exception {
        String json = generateJsonWithPriority("Med");
        assertTrue(json.contains("\"priority\": \"high\""), 
            "Med priority should map to 'high'");
    }

    @Test
    void mPriorityMapsToHigh() throws Exception {
        String json = generateJsonWithPriority("M");
        assertTrue(json.contains("\"priority\": \"high\""), 
            "M priority should map to 'high'");
    }

    @Test
    void highPriorityMapsToUrgent() throws Exception {
        String json = generateJsonWithPriority("High");
        assertTrue(json.contains("\"priority\": \"urgent\""), 
            "High priority should map to 'urgent'");
    }

    @Test
    void hPriorityMapsToUrgent() throws Exception {
        String json = generateJsonWithPriority("H");
        assertTrue(json.contains("\"priority\": \"urgent\""), 
            "H priority should map to 'urgent'");
    }

    @Test
    void lowEdgePriorityMapsToNormal() throws Exception {
        String json = generateJsonWithPriority("Low (Edge)");
        assertTrue(json.contains("\"priority\": \"normal\""), 
            "Low (Edge) priority should map to 'normal'");
    }

    @Test
    void mediumEdgePriorityMapsToHigh() throws Exception {
        String json = generateJsonWithPriority("Medium (Edge)");
        assertTrue(json.contains("\"priority\": \"high\""), 
            "Medium (Edge) priority should map to 'high'");
    }

    @Test
    void highEdgePriorityMapsToUrgent() throws Exception {
        String json = generateJsonWithPriority("High (Edge)");
        assertTrue(json.contains("\"priority\": \"urgent\""), 
            "High (Edge) priority should map to 'urgent'");
    }

    @Test
    void normalPriorityMapsToNormal() throws Exception {
        String json = generateJsonWithPriority("Normal");
        assertTrue(json.contains("\"priority\": \"normal\""), 
            "Normal priority should map to 'normal'");
    }

    @Test
    void urgentPriorityMapsToUrgent() throws Exception {
        String json = generateJsonWithPriority("Urgent");
        assertTrue(json.contains("\"priority\": \"urgent\""), 
            "Urgent priority should map to 'urgent'");
    }

    @Test
    void emptyPriorityDefaultsToNormal() throws Exception {
        String json = generateJsonWithPriority("");
        assertTrue(json.contains("\"priority\": \"normal\""), 
            "Empty priority should default to 'normal'");
    }

    private String generateJsonWithPriority(String priority) throws Exception {
        Path tempDir = Files.createTempDirectory("priority-test");
        Path excelPath = tempDir.resolve("test.xlsx");

        createWorkbookWithPriority(excelPath, priority);

        ExcelParserV5 parser = new ExcelParserV5();
        parser.load(excelPath.toFile());
        
        String json = ExcelParserV5.pretty(parser.buildNurseCallsJson());
        
        return json;
    }

    private void createWorkbookWithPriority(Path target, String priority) throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet units = workbook.createSheet("Unit Breakdown");
            Row unitsHeader = units.createRow(0);
            unitsHeader.createCell(0).setCellValue("Facility");
            unitsHeader.createCell(2).setCellValue("Common Unit Name");
            unitsHeader.createCell(3).setCellValue("Nurse Call Configuration Group");
            Row unitsRow = units.createRow(1);
            unitsRow.createCell(0).setCellValue("Test Facility");
            unitsRow.createCell(2).setCellValue("Test Unit");
            unitsRow.createCell(3).setCellValue("TestGroup");

            Sheet nurseCalls = workbook.createSheet("Nurse call");
            Row nurseHeader = nurseCalls.createRow(0);
            nurseHeader.createCell(0).setCellValue("Configuration Group");
            nurseHeader.createCell(4).setCellValue("Common Alert or Alarm Name");
            nurseHeader.createCell(5).setCellValue("Priority");
            nurseHeader.createCell(7).setCellValue("Ringtone");
            nurseHeader.createCell(32).setCellValue("Response Options");
            nurseHeader.createCell(33).setCellValue("1st recipients");
            
            Row nurseRow = nurseCalls.createRow(1);
            nurseRow.createCell(0).setCellValue("TestGroup");
            nurseRow.createCell(4).setCellValue("Test Alarm");
            nurseRow.createCell(5).setCellValue(priority);
            nurseRow.createCell(7).setCellValue("Tone 1");
            nurseRow.createCell(32).setCellValue("Accept");
            nurseRow.createCell(33).setCellValue("Nurse Team");

            try (OutputStream os = Files.newOutputStream(target)) {
                workbook.write(os);
            }
        }
    }
}
