package com.example.exceljson;

import com.example.exceljson.jobs.JobRunner;
import javafx.application.Application;
import java.awt.GraphicsEnvironment;

public final class Main {
    public static void main(String[] args) {
        if (isHeadless()) {
            // Headless environment: try to run CLI job if specified, otherwise print guidance
            int status = runCliIfPossible(args);
            if (status == Integer.MIN_VALUE) {
                System.err.println("No display available and no known CLI job specified.\n" +
                        "To run in CLI mode, use e.g.:\n" +
                        "  java -cp engage-rules-generator-3.0.0.jar com.example.exceljson.jobs.JobRunner export-json <input.xlsx> <output.json>");
                System.exit(1);
            }
            System.exit(status);
        }

        try {
            Application.launch(ExcelJsonApplication.class, args);
        } catch (Throwable t) {
            // If JavaFX fails to start (e.g., no DISPLAY), provide a helpful message and attempt CLI
            System.err.println("Failed to launch GUI: " + t.getMessage());
            int status = runCliIfPossible(args);
            if (status == Integer.MIN_VALUE) {
                System.err.println("No known CLI job specified. See JobRunner --help for options.");
                System.exit(1);
            }
            System.exit(status);
        }
    }

    private static boolean isHeadless() {
        // Only check GraphicsEnvironment on Linux/Unix - Windows/Mac should always attempt GUI
        String os = System.getProperty("os.name", "").toLowerCase();
        boolean isUnix = os.contains("linux") || os.contains("unix") || os.contains("solaris");
        
        if (!isUnix) {
            // On Windows/Mac, always attempt GUI launch (jpackage includes runtime)
            return false;
        }
        
        // On Unix-like systems, check for headless mode
        try {
            if (GraphicsEnvironment.isHeadless()) return true;
        } catch (Throwable ignore) { /* fall through */ }
        
        // Additional check for DISPLAY environment variable on X11 systems
        String display = System.getenv("DISPLAY");
        return display == null || display.isBlank();
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
