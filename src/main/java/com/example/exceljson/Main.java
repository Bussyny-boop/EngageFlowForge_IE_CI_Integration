package com.example.exceljson;

import com.example.exceljson.jobs.JobRunner;
import javafx.application.Application;

public final class Main {
    public static void main(String[] args) {
        // Always attempt to launch GUI first, regardless of environment
        // This allows JavaFX to use its own detection and fallback mechanisms
        try {
            Application.launch(ExcelJsonApplication.class, args);
        } catch (Throwable t) {
            // If JavaFX fails to start (e.g., no DISPLAY), provide a helpful message and attempt CLI
            System.err.println("Failed to launch GUI: " + t.getMessage());
            t.printStackTrace(); // Print full stack trace for debugging
            int status = runCliIfPossible(args);
            if (status == Integer.MIN_VALUE) {
                System.err.println("No known CLI job specified. See JobRunner --help for options.");
                System.exit(1);
            }
            System.exit(status);
        }
    }

    private static int runCliIfPossible(String[] args) {
        if (args != null && args.length > 0) {
            JobRunner runner = new JobRunner();
            if (runner.isKnownJob(args[0])) {
                return runner.run(args);
            }
        }
        return Integer.MIN_VALUE; // indicates no CLI executed
    }
}
