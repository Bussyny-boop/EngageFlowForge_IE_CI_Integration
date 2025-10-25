package com.example.exceljson.jobs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.junit.jupiter.api.Test;

class JobRunnerTest {

    @Test
    void failJobReturnsNonZeroStatus() {
        ByteArrayOutputStream outBuffer = new ByteArrayOutputStream();
        ByteArrayOutputStream errBuffer = new ByteArrayOutputStream();
        JobRunner runner = new JobRunner(new PrintStream(outBuffer), new PrintStream(errBuffer));

        int status = runner.run("fail");

        assertEquals(1, status, "Fail job should exit with status code 1");
        assertTrue(errBuffer.toString().contains("Fail job triggered"));
    }

    @Test
    void helpReturnsSuccessStatus() {
        ByteArrayOutputStream outBuffer = new ByteArrayOutputStream();
        ByteArrayOutputStream errBuffer = new ByteArrayOutputStream();
        JobRunner runner = new JobRunner(new PrintStream(outBuffer), new PrintStream(errBuffer));

        int status = runner.run("--help");

        assertEquals(0, status);
        assertTrue(outBuffer.toString().contains("Usage: JobRunner"));
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
}
