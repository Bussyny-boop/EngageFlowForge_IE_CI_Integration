package com.example.exceljson.jobs;

import com.example.exceljson.ExcelParserV4;

import java.io.File;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Simple command runner that exposes background jobs for automation.
 */
public final class JobRunner {

    private final PrintStream out;
    private final PrintStream err;
    private final Map<String, JobHandler> jobs;

    /**
     * Creates a runner that writes to {@link System#out} and {@link System#err}.
     */
    public JobRunner() {
        this(System.out, System.err);
    }

    JobRunner(PrintStream out, PrintStream err) {
        this.out = out;
        this.err = err;
        Map<String, JobHandler> definitions = new LinkedHashMap<>();
        definitions.put("fail", new JobHandler(
                "Runs a diagnostics smoke check; add --expect-failure to force a non-zero exit.",
                this::runFailJob));
        definitions.put("export-json", new JobHandler(
                "Generate Engage JSON from an Excel workbook.",
                this::runExportJsonJob));
        this.jobs = Collections.unmodifiableMap(definitions);
    }

    public static void main(String[] args) {
        int status = new JobRunner().run(args);
        if (status != 0) {
            System.exit(status);
        }
    }

    /**
     * Executes the supplied job name.
     *
     * @param args the command line arguments; the first value is treated as the job name
     * @return a process exit code (0 for success, non-zero for failure)
     */
    public int run(String... args) {
        if (args == null || args.length == 0) {
            printUsage("No job specified.");
            return 1;
        }

        String job = normalize(args[0]);

        if ("--help".equals(job) || "-h".equals(job)) {
            printUsage(null);
            return 0;
        }

        if (job.isEmpty()) {
            printUsage("Job name is empty.");
            return 1;
        }

        JobHandler handler = jobs.get(job);
        if (handler == null) {
            err.printf("Unknown job \"%s\".%n", args[0]);
            printAvailableJobs();
            return 1;
        }

        String[] jobArgs = Arrays.copyOfRange(args, 1, args.length);
        return handler.executor.run(jobArgs);
    }

    private void printUsage(String errorMessage) {
        if (errorMessage != null) {
            err.println(errorMessage);
        }
        out.println("Usage: JobRunner <job> [job-args]");
        printAvailableJobs();
    }

    private void printAvailableJobs() {
        out.println("Available jobs:");
        jobs.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> out.printf("  %s%n    %s%n", entry.getKey(), entry.getValue().description));
    }

    /**
     * Checks whether the supplied job name corresponds to a registered job.
     *
     * @param jobName the job identifier provided on the command line
     * @return {@code true} if the job exists, otherwise {@code false}
     */
    public boolean isKnownJob(String jobName) {
        if (jobName == null || jobName.isEmpty()) {
            return false;
        }
        return jobs.containsKey(normalize(jobName));
    }

    private static String normalize(String input) {
        return input == null ? "" : input.trim().toLowerCase(Locale.ROOT);
    }

    private int runFailJob(String[] args) {
        boolean expectFailure = false;
        if (args != null) {
            for (String arg : args) {
                if ("--expect-failure".equalsIgnoreCase(arg) || "-f".equalsIgnoreCase(arg)) {
                    expectFailure = true;
                    break;
                }
            }
        }

        if (expectFailure) {
            err.println("Fail job triggered – intentionally exiting with status 1.");
            return 1;
        }

        out.println("Fail job completed successfully. Use --expect-failure to force a failure exit code.");
        return 0;
    }

    private int runExportJsonJob(String[] args) {
        if (args.length < 2) {
            err.println("Usage: JobRunner export-json <input.xlsx> <output.json>");
            return 1;
        }

        File input = new File(args[0]);
        if (!input.isFile()) {
            err.printf("Input Excel file \"%s\" was not found.%n", args[0]);
            return 1;
        }

        File output = new File(args[1]).getAbsoluteFile();
        File parent = output.getAbsoluteFile().getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            err.printf("Unable to create parent directory for \"%s\".%n", output);
            return 1;
        }

        try {
            ExcelParserV4 parser = new ExcelParserV4();
            parser.load(input);
            parser.writeJson(output);
            out.printf("Wrote JSON to %s%n", output.getAbsolutePath());
            return 0;
        } catch (Exception e) {
            err.printf("Failed to export JSON: %s%n", e.getMessage());
            e.printStackTrace(err);
            return 1;
        }
    }

    private static final class JobHandler {
        private final String description;
        private final JobExecutor executor;

        private JobHandler(String description, JobExecutor executor) {
            this.description = description;
            this.executor = executor;
        }
    }

    @FunctionalInterface
    private interface JobExecutor {
        int run(String[] args);
    }
}
