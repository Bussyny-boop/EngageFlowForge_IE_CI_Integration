package com.example.exceljson.jobs;

import java.io.PrintStream;
import java.util.Locale;
import java.util.Set;

/**
 * Simple command runner that exposes tiny background jobs for automation.
 *
 * <p>The initial requirement is to expose a "fail" job that intentionally
 * exits with a non-zero status.  This is useful when wiring smoke tests for
 * CI/CD pipelines where a deterministic failure signal is required.</p>
 */
public final class JobRunner {

    private static final Set<String> AVAILABLE_JOBS = Set.of("fail");

    private final PrintStream out;
    private final PrintStream err;

    /**
     * Creates a runner that writes to {@link System#out} and {@link System#err}.
     */
    public JobRunner() {
        this(System.out, System.err);
    }

    JobRunner(PrintStream out, PrintStream err) {
        this.out = out;
        this.err = err;
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

        if ("fail".equals(job)) {
            err.println("Fail job triggered – intentionally exiting with status 1.");
            return 1;
        }

        err.printf("Unknown job \"%s\".%n", args[0]);
        printAvailableJobs();
        return 1;
    }

    private void printUsage(String errorMessage) {
        if (errorMessage != null) {
            err.println(errorMessage);
        }
        out.println("Usage: JobRunner <job>");
        printAvailableJobs();
    }

    private void printAvailableJobs() {
        out.println("Available jobs:");
        AVAILABLE_JOBS.stream()
                .sorted()
                .forEach(job -> out.printf("  %s%n", job));
    }

    private static String normalize(String input) {
        return input == null ? "" : input.trim().toLowerCase(Locale.ROOT);
    }
}
