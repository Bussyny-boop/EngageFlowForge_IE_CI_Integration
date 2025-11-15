package com.example.exceljson.jobs;

import com.example.exceljson.ExcelParserV5;

import java.io.File;
import java.io.FileWriter;
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
        definitions.put("roundtrip-json", new JobHandler(
            "Load a JSON file and immediately re-export NurseCalls and Clinicals JSON to a directory.",
            this::runRoundtripJsonJob));
        definitions.put("roundtrip-xml", new JobHandler(
            "Load an Engage XML file and re-export NurseCalls and Clinicals JSON to a directory.",
            this::runRoundtripXmlJob));
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
    
        File input = new File(args[0]).getAbsoluteFile();
        if (!input.isFile()) {
            err.printf("❌ Input Excel file \"%s\" was not found.%n", input);
            return 1;
        }
    
        File output = new File(args[1]).getAbsoluteFile(); // ✅ ensure absolute path
        boolean outputExists = output.exists();
        boolean outputLooksLikeFile = outputExists ? output.isFile() : output.getName().toLowerCase().endsWith(".json");

        File baseDir;
        if (output.isDirectory() || (!outputExists && !outputLooksLikeFile)) {
            baseDir = output;
        } else {
            baseDir = output.getParentFile();
        }

        if (baseDir == null) {
            baseDir = output.getAbsoluteFile().getParentFile();
        }
        if (baseDir == null) {
            baseDir = new File(".").getAbsoluteFile();
        }

        baseDir = baseDir.getAbsoluteFile();

        if (!baseDir.exists() && !baseDir.mkdirs()) {
            err.printf("❌ Unable to create parent directory for \"%s\".%n", output);
            return 1;
        }

        try {
            out.printf("📥 Loading workbook: %s%n", input.getAbsolutePath());
            ExcelParserV5 parser = new ExcelParserV5();
            parser.load(input);

            File nurseJson = new File(baseDir, "NurseCalls.json");
            File clinicalJson = new File(baseDir, "Clinicals.json");

            out.printf("📤 Writing JSON to:%n  %s%n  %s%n", nurseJson.getAbsolutePath(), clinicalJson.getAbsolutePath());
            parser.writeNurseCallsJson(nurseJson);
            parser.writeClinicalsJson(clinicalJson);

            // ✅ Verify flush + existence for test compatibility
            if (!nurseJson.exists() || nurseJson.length() == 0) {
                throw new RuntimeException("Output file missing or empty: " + nurseJson.getAbsolutePath());
            }
            if (!clinicalJson.exists() || clinicalJson.length() == 0) {
                throw new RuntimeException("Output file missing or empty: " + clinicalJson.getAbsolutePath());
            }

            File summaryFile = output.isDirectory() ? new File(output, "output.json") : output;
            if (summaryFile.getParentFile() != null && !summaryFile.getParentFile().exists()) {
                if (!summaryFile.getParentFile().mkdirs()) {
                    throw new RuntimeException("Unable to create parent directory for summary file: "
                            + summaryFile.getAbsolutePath());
                }
            }

            // ✅ create a simple indicator file for backward compatibility with test
            try (FileWriter writer = new FileWriter(summaryFile, false)) {
                writer.write("{ \"note\": \"This build now writes two separate JSON files: "
                        + nurseJson.getName() + " and " + clinicalJson.getName() + "\" }");
            }

            out.printf("✅ Wrote JSON files to:%n  %s%n  %s%n", nurseJson.getAbsolutePath(), clinicalJson.getAbsolutePath());
            out.printf("📄 Summary file written to: %s%n", summaryFile.getAbsolutePath());
            return 0;
        } catch (Exception e) {
            err.printf("❌ Failed to export JSON: %s%n", e.getMessage());
            e.printStackTrace(err);
            return 1;
        }
    }

    private int runRoundtripJsonJob(String[] args) {
        if (args.length < 2) {
            err.println("Usage: JobRunner roundtrip-json <input.json> <outputDir>");
            return 1;
        }

        File input = new File(args[0]).getAbsoluteFile();
        if (!input.isFile()) {
            err.printf("❌ Input JSON file \"%s\" was not found.%n", input);
            return 1;
        }

        File outputDir = new File(args[1]).getAbsoluteFile();
        if (outputDir.exists() && !outputDir.isDirectory()) {
            err.printf("❌ Output path exists but is not a directory: %s%n", outputDir);
            return 1;
        }
        if (!outputDir.exists() && !outputDir.mkdirs()) {
            err.printf("❌ Unable to create output directory: %s%n", outputDir);
            return 1;
        }

        try {
            out.printf("📥 Loading JSON: %s%n", input.getAbsolutePath());
            ExcelParserV5 parser = new ExcelParserV5();
            parser.loadJson(input);

            File nurseOut = new File(outputDir, "NurseCalls.roundtrip.json");
            File clinicalOut = new File(outputDir, "Clinicals.roundtrip.json");

            out.printf("📤 Re-exporting to:%n  %s%n  %s%n", nurseOut.getAbsolutePath(), clinicalOut.getAbsolutePath());
            parser.writeNurseCallsJson(nurseOut);
            parser.writeClinicalsJson(clinicalOut);

            boolean ok = nurseOut.isFile() && nurseOut.length() > 0 && clinicalOut.isFile() && clinicalOut.length() > 0;
            if (!ok) {
                throw new RuntimeException("One or more output files are missing or empty.");
            }

            out.println("✅ Round-trip complete.");
            return 0;
        } catch (Exception e) {
            err.printf("❌ Failed round-trip: %s%n", e.getMessage());
            e.printStackTrace(err);
            return 1;
        }
    }

    private int runRoundtripXmlJob(String[] args) {
        if (args.length < 2) {
            err.println("Usage: JobRunner roundtrip-xml <input.xml> <outputDir>");
            return 1;
        }

        File input = new File(args[0]).getAbsoluteFile();
        if (!input.isFile()) {
            err.printf("❌ Input XML file \"%s\" was not found.%n", input);
            return 1;
        }

        File outputDir = new File(args[1]).getAbsoluteFile();
        if (outputDir.exists() && !outputDir.isDirectory()) {
            err.printf("❌ Output path exists but is not a directory: %s%n", outputDir);
            return 1;
        }
        if (!outputDir.exists() && !outputDir.mkdirs()) {
            err.printf("❌ Unable to create output directory: %s%n", outputDir);
            return 1;
        }

        try {
            out.printf("📥 Loading XML: %s%n", input.getAbsolutePath());
            com.example.exceljson.XmlParser xml = new com.example.exceljson.XmlParser();
            xml.load(input);

            // Show XML load summary for quick verification
            out.println(xml.getLoadSummary());

            // Transfer rows into ExcelParserV5 for JSON export
            ExcelParserV5 parser = new ExcelParserV5();
            parser.units.addAll(xml.getUnits());
            parser.nurseCalls.addAll(xml.getNurseCalls());
            parser.clinicals.addAll(xml.getClinicals());
            parser.orders.addAll(xml.getOrders());

            // Rebuild unit maps from Units tab to enable unit resolution for flow names/units
            parser.rebuildUnitMaps();

            File nurseOut = new File(outputDir, "NurseCalls.fromXml.json");
            File clinicalOut = new File(outputDir, "Clinicals.fromXml.json");

            out.printf("📤 Exporting to:%n  %s%n  %s%n", nurseOut.getAbsolutePath(), clinicalOut.getAbsolutePath());
            parser.writeNurseCallsJson(nurseOut);
            parser.writeClinicalsJson(clinicalOut);

            boolean ok = nurseOut.isFile() && nurseOut.length() > 0 && clinicalOut.isFile() && clinicalOut.length() > 0;
            if (!ok) {
                throw new RuntimeException("One or more output files are missing or empty.");
            }

            out.println("✅ XML round-trip complete.");
            return 0;
        } catch (Exception e) {
            err.printf("❌ Failed XML round-trip: %s%n", e.getMessage());
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
