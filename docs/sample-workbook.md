# Sample workbook

To run `JobRunner export-json`, you need an Engage configuration workbook with the
following sheets and columns. We avoid committing the spreadsheet directly to keep
the repository free of binary artifacts, so create a workbook locally using these
steps:

1. Create a worksheet named **Unit Breakdown** with a header row containing at
   least "Facility" in column A, "Common Unit Name" in column C, and
   "Configuration Group" in column D. Add one data row that maps `Facility A` to
   `Unit A` and configuration group `Group 1`.
2. Create a worksheet named **Nurse call** with headers for "Configuration
   Group" (column A), "Alarm Name" (column E), "Priority" (column F),
   "Ringtone" (column H), and "Response Options" (column AG). Add recipients in
   the remaining columns as needed.
3. Create a worksheet named **Patient Monitoring** mirroring the Nurse call
   structure and include a "Fail safe recipients" column (column AI).

Save the workbook as `sample.xlsx` in the project root and run:

```
java -cp target/engage-rules-generator-1.1.0.jar \
    com.example.exceljson.jobs.JobRunner export-json sample.xlsx sample.json
```

The unit tests (`JobRunnerTest`) demonstrate the minimal cell layout if you need
an example generated programmatically. The generated `sample.json` is ignored by
Git so you can review it locally without touching version control.

## Quick smoke check with the `fail` job

The CLI also exposes a lightweight diagnostics job that verifies the runtime
environment without touching any spreadsheets. First build the project so the
executable jar exists:

```
mvn -q package
```

Then run the smoke check to confirm that dependencies and logging are wired
correctly:

```
java -cp target/engage-rules-generator-1.1.0.jar \
    com.example.exceljson.jobs.JobRunner fail
```

The command exits with status code `0` and prints a reminder that you can pass
`--expect-failure` if you need to simulate a failing pipeline step.
