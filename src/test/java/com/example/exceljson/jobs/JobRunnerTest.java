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
        assertTrue(outBuffer.toString().contains("Wrote JSON"));
        assertTrue(errBuffer.toString().isEmpty());
    }

    private static void createSampleWorkbook(Path target) throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet units = workbook.createSheet("Unit Breakdown");
            Row unitsHeader = units.createRow(0);
            unitsHeader.createCell(0).setCellValue("Facility");
            unitsHeader.createCell(2).setCellValue("Common Unit Name");
            unitsHeader.createCell(3).setCellValue("Configuration Group");
            Row unitsRow = units.createRow(1);
            unitsRow.createCell(0).setCellValue("Facility A");
            unitsRow.createCell(2).setCellValue("Unit A");
            unitsRow.createCell(3).setCellValue("Group 1");

            Sheet nurseCalls = workbook.createSheet("Nurse call");
            Row nurseHeader = nurseCalls.createRow(0);
            nurseHeader.createCell(0).setCellValue("Configuration Group");
            nurseHeader.createCell(4).setCellValue("Alarm Name");
            nurseHeader.createCell(5).setCellValue("Priority");
            nurseHeader.createCell(7).setCellValue("Ringtone");
            nurseHeader.createCell(32).setCellValue("Response Options");
            nurseHeader.createCell(33).setCellValue("1st recipients");
            Row nurseRow = nurseCalls.createRow(1);
            nurseRow.createCell(0).setCellValue("Group 1");
            nurseRow.createCell(4).setCellValue("Alarm 1");
            nurseRow.createCell(5).setCellValue("High");
            nurseRow.createCell(7).setCellValue("Tone 1");
            nurseRow.createCell(32).setCellValue("Ack");
            nurseRow.createCell(33).setCellValue("Nurse Team");

            Sheet clinicals = workbook.createSheet("Patient Monitoring");
            Row clinicalHeader = clinicals.createRow(0);
            clinicalHeader.createCell(0).setCellValue("Configuration Group");
            clinicalHeader.createCell(4).setCellValue("Alarm Name");
            clinicalHeader.createCell(5).setCellValue("Priority");
            clinicalHeader.createCell(7).setCellValue("Ringtone");
            clinicalHeader.createCell(32).setCellValue("Response Options");
            clinicalHeader.createCell(33).setCellValue("1st recipients");
            clinicalHeader.createCell(34).setCellValue("Fail safe recipients");
            Row clinicalRow = clinicals.createRow(1);
            clinicalRow.createCell(0).setCellValue("Group 1");
            clinicalRow.createCell(4).setCellValue("Clinical Alarm");
            clinicalRow.createCell(5).setCellValue("Medium");
            clinicalRow.createCell(7).setCellValue("Tone 2");
            clinicalRow.createCell(32).setCellValue("Escalate");
            clinicalRow.createCell(33).setCellValue("Primary");
            clinicalRow.createCell(34).setCellValue("Backup");

            try (OutputStream os = Files.newOutputStream(target)) {
                workbook.write(os);
            }
        }
    }
}
