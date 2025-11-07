package com.example.exceljson.jobs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

class JobRunnerTest {

    @Test
    void failJobSucceedsByDefault() {
        ByteArrayOutputStream outBuffer = new ByteArrayOutputStream();
        ByteArrayOutputStream errBuffer = new ByteArrayOutputStream();
        JobRunner runner = new JobRunner(new PrintStream(outBuffer), new PrintStream(errBuffer));

        int status = runner.run("fail");

        assertEquals(0, status, "Fail job should succeed unless a failure is explicitly requested");
        assertTrue(outBuffer.toString().contains("Fail job completed successfully"));
        assertTrue(errBuffer.toString().isEmpty());
    }

    @Test
    void failJobCanBeForcedToFail() {
        ByteArrayOutputStream outBuffer = new ByteArrayOutputStream();
        ByteArrayOutputStream errBuffer = new ByteArrayOutputStream();
        JobRunner runner = new JobRunner(new PrintStream(outBuffer), new PrintStream(errBuffer));

        int status = runner.run("fail", "--expect-failure");

        assertEquals(1, status, "Fail job should exit with status code 1 when forced to fail");
        assertTrue(errBuffer.toString().contains("Fail job triggered"));
    }

    @Test
    void helpReturnsSuccessStatus() {
        ByteArrayOutputStream outBuffer = new ByteArrayOutputStream();
        ByteArrayOutputStream errBuffer = new ByteArrayOutputStream();
        JobRunner runner = new JobRunner(new PrintStream(outBuffer), new PrintStream(errBuffer));

        int status = runner.run("--help");

        assertEquals(0, status);
        String helpText = outBuffer.toString();
        assertTrue(helpText.contains("Usage: JobRunner"));
        assertTrue(helpText.contains("export-json"));
    }

    @Test
    void unknownJobPrintsError() {
        ByteArrayOutputStream outBuffer = new ByteArrayOutputStream();
        ByteArrayOutputStream errBuffer = new ByteArrayOutputStream();
        JobRunner runner = new JobRunner(new PrintStream(outBuffer), new PrintStream(errBuffer));

        int status = runner.run("missing");

        assertEquals(1, status);
        String errOutput = errBuffer.toString();
        assertTrue(errOutput.contains("Unknown job \"missing\"."));
        assertTrue(outBuffer.toString().contains("Available jobs"));
    }

    @Test
    void exportJsonJobRequiresArguments() {
        ByteArrayOutputStream outBuffer = new ByteArrayOutputStream();
        ByteArrayOutputStream errBuffer = new ByteArrayOutputStream();
        JobRunner runner = new JobRunner(new PrintStream(outBuffer), new PrintStream(errBuffer));

        int status = runner.run("export-json");

        assertEquals(1, status);
        assertTrue(errBuffer.toString().contains("Usage: JobRunner export-json"));
    }

    @Test
    void exportJsonJobWritesOutputFile() throws Exception {
        Path tempDir = Files.createTempDirectory("job-runner-test");
        Path excelPath = tempDir.resolve("input.xlsx");
        Path jsonPath = tempDir.resolve("output.json");

        createSampleWorkbook(excelPath);

        ByteArrayOutputStream outBuffer = new ByteArrayOutputStream();
        ByteArrayOutputStream errBuffer = new ByteArrayOutputStream();
        JobRunner runner = new JobRunner(new PrintStream(outBuffer), new PrintStream(errBuffer));

        int status = runner.run("export-json", excelPath.toString(), jsonPath.toString());

        assertEquals(0, status);
        assertTrue(Files.exists(jsonPath));
        String json = Files.readString(jsonPath);
        assertTrue(json.contains("This build now writes two separate JSON files"));

        Path nurseJson = jsonPath.getParent().resolve("NurseCalls.json");
        Path clinicalJson = jsonPath.getParent().resolve("Clinicals.json");

        assertTrue(Files.exists(nurseJson));
        assertTrue(Files.exists(clinicalJson));

        String nurseContent = Files.readString(nurseJson);
        String clinicalContent = Files.readString(clinicalJson);

        assertTrue(nurseContent.contains("Alarm 1"));
        assertTrue(clinicalContent.contains("Clinical Alarm"));
        assertTrue(clinicalContent.contains("\"name\": \"subject\""));
        assertTrue(clinicalContent.contains("\"value\": \"\\\"NoCaregiver assigned for #{alert_type} #{bed.room.name} Bed #{bed.bed_number}\\\"\""));
        assertTrue(outBuffer.toString().contains("Wrote JSON"));
        assertTrue(errBuffer.toString().isEmpty());
    }

    private static void createSampleWorkbook(Path target) throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet units = workbook.createSheet("Unit Breakdown");
            Row unitsHeader = units.createRow(2);
            unitsHeader.createCell(0).setCellValue("Facility");
            unitsHeader.createCell(1).setCellValue("Common Unit Name");
            unitsHeader.createCell(2).setCellValue("Nurse Call Configuration Group");
            unitsHeader.createCell(3).setCellValue("Patient Monitoring Configuration Group");
            Row unitsRow = units.createRow(3);
            unitsRow.createCell(0).setCellValue("Facility A");
            unitsRow.createCell(1).setCellValue("Unit A");
            unitsRow.createCell(2).setCellValue("Group 1");
            unitsRow.createCell(3).setCellValue("Group 1");

            Sheet nurseCalls = workbook.createSheet("Nurse call");
            Row nurseHeader = nurseCalls.createRow(2);
            nurseHeader.createCell(0).setCellValue("Configuration Group");
            nurseHeader.createCell(1).setCellValue("Common Alert or Alarm Name");
            nurseHeader.createCell(2).setCellValue("Priority");
            nurseHeader.createCell(3).setCellValue("Device - A");
            nurseHeader.createCell(4).setCellValue("Ringtone");
            nurseHeader.createCell(5).setCellValue("Response Options");
            nurseHeader.createCell(6).setCellValue("1st Recipient");
            Row nurseRow = nurseCalls.createRow(3);
            nurseRow.createCell(0).setCellValue("Group 1");
            nurseRow.createCell(1).setCellValue("Alarm 1");
            nurseRow.createCell(2).setCellValue("High");
            nurseRow.createCell(3).setCellValue("Edge");
            nurseRow.createCell(4).setCellValue("Tone 1");
            nurseRow.createCell(5).setCellValue("Ack");
            nurseRow.createCell(6).setCellValue("Nurse Team");

            Sheet clinicals = workbook.createSheet("Patient Monitoring");
            Row clinicalHeader = clinicals.createRow(2);
            clinicalHeader.createCell(0).setCellValue("Configuration Group");
            clinicalHeader.createCell(1).setCellValue("Common Alert or Alarm Name");
            clinicalHeader.createCell(2).setCellValue("Priority");
            clinicalHeader.createCell(3).setCellValue("Device - A");
            clinicalHeader.createCell(4).setCellValue("Ringtone");
            clinicalHeader.createCell(5).setCellValue("Response Options");
            clinicalHeader.createCell(6).setCellValue("1st Recipient");
            clinicalHeader.createCell(7).setCellValue("Fail safe recipients");
            Row clinicalRow = clinicals.createRow(3);
            clinicalRow.createCell(0).setCellValue("Group 1");
            clinicalRow.createCell(1).setCellValue("Clinical Alarm");
            clinicalRow.createCell(2).setCellValue("Medium");
            clinicalRow.createCell(3).setCellValue("VMP");
            clinicalRow.createCell(4).setCellValue("Tone 2");
            clinicalRow.createCell(5).setCellValue("Escalate");
            clinicalRow.createCell(6).setCellValue("Primary");
            clinicalRow.createCell(7).setCellValue("Backup");

            try (OutputStream os = Files.newOutputStream(target)) {
                workbook.write(os);
            }
        }
    }

    @Test
    void alertTypeUsesCommonAlarmNameNotSendingName() throws Exception {
        Path tempDir = Files.createTempDirectory("alert-type-test");
        Path excelPath = tempDir.resolve("test.xlsx");
        Path jsonPath = tempDir.resolve("output.json");

        // Create workbook with both Common Alert Name AND Sending System Name
        createWorkbookWithBothNameColumns(excelPath);

        ByteArrayOutputStream outBuffer = new ByteArrayOutputStream();
        ByteArrayOutputStream errBuffer = new ByteArrayOutputStream();
        JobRunner runner = new JobRunner(new PrintStream(outBuffer), new PrintStream(errBuffer));

        int status = runner.run("export-json", excelPath.toString(), jsonPath.toString());

        assertEquals(0, status);
        
        Path nurseJson = jsonPath.getParent().resolve("NurseCalls.json");
        assertTrue(Files.exists(nurseJson));
        
        String nurseContent = Files.readString(nurseJson);
        
        // The alert definition value should use the Common Alert Name, not the Sending System Name
        assertTrue(nurseContent.contains("\"value\": \"Common Alarm Name\""), 
            "Alert type should use Common Alarm Name from the 'Common Alert or Alarm Name' column");
        assertTrue(!nurseContent.contains("\"value\": \"Sending System Alarm Name\""), 
            "Alert type should NOT use Sending System Alarm Name");
    }

    private static void createWorkbookWithBothNameColumns(Path target) throws Exception {
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
            nurseHeader.createCell(2).setCellValue("Sending System Alert Name");
            nurseHeader.createCell(3).setCellValue("Priority");
            nurseHeader.createCell(4).setCellValue("Device - A");
            nurseHeader.createCell(5).setCellValue("Ringtone");
            nurseHeader.createCell(6).setCellValue("Response Options");
            nurseHeader.createCell(7).setCellValue("1st Recipient");
            
            Row nurseRow = nurseCalls.createRow(3);
            nurseRow.createCell(0).setCellValue("TestGroup");
            nurseRow.createCell(1).setCellValue("Common Alarm Name");
            nurseRow.createCell(2).setCellValue("Sending System Alarm Name");
            nurseRow.createCell(3).setCellValue("High");
            nurseRow.createCell(4).setCellValue("Edge");
            nurseRow.createCell(5).setCellValue("Tone 1");
            nurseRow.createCell(6).setCellValue("Accept");
            nurseRow.createCell(7).setCellValue("Nurse Team");

            // Add empty Patient Monitoring sheet
            Sheet clinicals = workbook.createSheet("Patient Monitoring");
            Row clinicalHeader = clinicals.createRow(2);
            clinicalHeader.createCell(0).setCellValue("Configuration Group");
            clinicalHeader.createCell(1).setCellValue("Common Alert or Alarm Name");

            try (OutputStream os = Files.newOutputStream(target)) {
                workbook.write(os);
            }
        }
    }
}
