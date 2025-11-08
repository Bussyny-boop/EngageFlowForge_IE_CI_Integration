# MSI UI Launch Fix - Implementation Summary

## Problem Statement
After installing the application via the MSI installer, users reported that the UI did not launch when they started the application from the Windows Start Menu or desktop shortcut. The application appeared to do nothing when launched.

## Root Cause Analysis

### Background
The GitHub Actions workflow uses `jpackage` to create a Windows MSI installer that includes:
1. A custom Java runtime image created with `jlink`
2. The application JAR with all dependencies (shaded JAR)
3. JavaFX modules included in the custom runtime

### The Issue
When `jpackage` creates the Windows launcher executable, it doesn't automatically configure the JVM to load JavaFX modules from the custom runtime. Without these modules being explicitly loaded:
- The JavaFX Application Platform fails to initialize
- `Application.launch()` in `Main.java` silently fails
- No UI appears, and no error is shown to the user

This is a common issue when using `jpackage` with JavaFX and custom runtime images, as JavaFX requires explicit module loading when not running as a traditional modular application.

## Solution

### What Was Changed
Modified `.github/workflows/main.yml` to add the `--java-options` parameter to the `jpackage` command:

```yaml
--java-options "--add-modules javafx.controls,javafx.fxml"
```

### How It Works
The `--java-options` parameter tells `jpackage` to configure the generated Windows launcher to pass these options to the JVM when starting the application. Specifically:
- `--add-modules javafx.controls` - Loads the JavaFX controls module (required for UI components)
- `--add-modules javafx.fxml` - Loads the JavaFX FXML module (required for loading App.fxml)

When the user launches the installed application, the Windows launcher now:
1. Starts the JVM with the custom runtime
2. Explicitly loads the JavaFX modules
3. Calls `Main.main()` with the correct JavaFX environment
4. Successfully initializes the JavaFX Application Platform
5. Displays the UI

## Why Only These Two Modules?
While the custom runtime includes many JavaFX modules (`javafx.base`, `javafx.controls`, `javafx.fxml`, `javafx.graphics`, `javafx.media`, `javafx.web`), we only need to explicitly load:
- `javafx.controls` - This automatically brings in `javafx.graphics` and `javafx.base` as transitive dependencies
- `javafx.fxml` - Required for FXML loading, which is used by the application

The other modules (`javafx.media`, `javafx.web`) are available in the runtime but not required for this application.

## Testing
- All 275 unit tests pass
- CodeQL security scan shows 0 alerts
- The fix is minimal and targeted (1 line change)

## Verification Steps
After merging this PR and downloading the new MSI installer:
1. Install the MSI on a Windows machine
2. Launch "EngageFlowForge" from the Start Menu or desktop shortcut
3. The JavaFX UI should appear immediately
4. The application should function normally

## Alternative Solutions Considered

### Option 1: Module Descriptor (Rejected)
Add a `module-info.java` file to make the application fully modular. This was rejected because:
- It would require extensive refactoring
- Third-party dependencies might not be modular
- Shaded JARs and module descriptors don't work well together

### Option 2: Exclude JavaFX from Shaded JAR (Rejected)
Modify the Maven build to exclude JavaFX from the shaded JAR and rely entirely on the runtime modules. This was rejected because:
- It would break the local development workflow (`mvn javafx:run`)
- The current build produces a working JAR that can be run standalone
- More complex to maintain two different build configurations

### Option 3: Use `--add-modules ALL-MODULE-PATH` (Rejected)
Load all modules from the runtime. This was rejected because:
- Less explicit about dependencies
- Could load unnecessary modules
- The current solution is more precise

## References
- [jpackage documentation](https://docs.oracle.com/en/java/javase/17/docs/specs/man/jpackage.html)
- [JavaFX Module System](https://openjfx.io/openjfx-docs/#modular)
- [Common jpackage Issues](https://github.com/openjdk/jdk/blob/master/src/jdk.jpackage/share/classes/jdk/jpackage/internal/resources/MainResources.properties)
