# Copilot Instructions for Engage Rules Generator

## Project Overview

This is a Java application that converts Vocera Engage Excel configuration sheets into JSON rule files. The application provides both a JavaFX GUI and a command-line interface for processing Excel workbooks containing nurse call and patient monitoring configurations.

## Build System

This project uses **Maven** as its build tool.

### Building the Project

```bash
mvn clean package
```

This command:
- Cleans the `target/` directory
- Compiles the source code
- Runs all tests
- Creates a shaded JAR with all dependencies at `target/engage-rules-generator-1.1.0.jar`

### Running Tests

```bash
mvn test
```

All tests must pass before code changes are considered complete. The project uses **JUnit 5** for testing.

## Project Structure

- `src/main/java/com/example/exceljson/` - Main application code
  - `Main.java` - JavaFX application entry point
  - `ExcelParserV5.java` - Core parser for Excel workbooks
  - `jobs/JobRunner.java` - Command-line interface for batch operations
  - `Config.java`, `UnitRow.java`, `FlowRow.java` - Data models
- `src/test/java/` - JUnit test files
- `docs/` - Documentation files
  - `sample-workbook.md` - Guide for creating test workbooks
  - `change-request.md` - Recent change history
- `pom.xml` - Maven configuration

## Key Dependencies

- **Java 17** - Required runtime version
- **JavaFX 21** - GUI framework
- **Apache POI 5.2.5** - Excel file processing
- **Jackson 2.17.2** - JSON/YAML serialization
- **Log4j 2.22.1** - Logging framework
- **JUnit 5.10.2** - Testing framework

## Code Conventions

### JSON Serialization
When working with JSON output, be careful about value types:
- Boolean flags (`enunciate`, `popup`) must be plain boolean strings (`true`/`false`), not quoted
- Numeric values like `ttl` must be literal numbers, not strings
- Arrays like `acceptBadgePhrases` and `retractRules` must be JSON array strings
- Preserve Excel-provided values for fields like ringtones and alert sounds

### Excel Parsing
The application expects specific Excel sheet structures:
- **Unit Breakdown** sheet - Maps facilities to units and configuration groups
- **Nurse call** sheet - Nurse call alarm configurations
- **Patient Monitoring** sheet - Patient monitoring configurations

See `docs/sample-workbook.md` for detailed sheet structure requirements.

## Running the Application

### GUI Mode
```bash
mvn javafx:run
```

### Command-Line Mode
```bash
java -cp target/engage-rules-generator-1.1.0.jar \
    com.example.exceljson.jobs.JobRunner export-json <input.xlsx> <output.json>
```

### Smoke Test
```bash
java -cp target/engage-rules-generator-1.1.0.jar \
    com.example.exceljson.jobs.JobRunner fail
```

## Continuous Integration

The project uses GitHub Actions for CI/CD:
- `.github/workflows/main.yml` - Builds an MSI installer for Windows
- Runs on: Windows with Liberica JDK 21 (includes JavaFX)
- Artifacts are retained for 14 days

## Files to Ignore

The following files are ignored by Git and should not be committed:
- `target/` - Maven build output
- `sample.xlsx` - Local test files
- `sample.json` - Generated output files

## Testing Guidelines

1. Run tests locally before pushing: `mvn test`
2. Ensure all existing tests pass
3. Add tests for new functionality following existing patterns in `src/test/java/`
4. The test suite includes unit tests for:
   - Excel parsing (`JobRunnerTest`)
   - CLI operations (`MainTest`)

## Common Tasks

### Adding a New Configuration Field
1. Update the relevant data model class (`FlowRow.java`, `UnitRow.java`, etc.)
2. Modify `ExcelParserV5.java` to parse the new field from Excel
3. Ensure proper JSON serialization (check boolean/numeric/array formatting)
4. Add test cases to verify the new field is parsed and serialized correctly
5. Update `docs/sample-workbook.md` if the Excel structure changes

### Modifying JSON Output Format
1. Review `change-request.md` for previous serialization issues
2. Be mindful of Engage's expected schema (no double-quoted booleans, proper array formatting)
3. Test with actual Excel files using the CLI
4. Verify the generated JSON is valid and matches Engage's expectations

## Development Environment

- Java 17 JDK is required
- Maven 3.6+ is recommended
- For JavaFX GUI development, use a JDK with JavaFX bundled (e.g., Liberica Full JDK) or ensure JavaFX modules are available

## Important Notes

- This application is designed for healthcare workflows (Vocera Engage system)
- Accuracy of configuration data is critical
- Always validate JSON output against Engage's schema requirements
- Preserve clinical data integrity when modifying parsing logic
