# Application Icon Resource Instructions

## Overview

The FlowForge application includes a custom application icon (`icon.ico`) that appears in the application window title bar, Windows taskbar, and Windows search results.

## Icon File Location

The icon file is located at:
```
src/main/resources/icon.ico
```

This file is tracked in the repository and included automatically in builds.

## How the Icon is Used

The icon is used in two places:

1. **JavaFX Application Window**: The icon is loaded in `ExcelJsonApplication.java` and set on the primary stage
2. **Windows Installer (MSI)**: The icon is embedded in the MSI installer via the `jpackage` command in the GitHub Actions workflow

## Replacing the Icon

If you need to replace the icon with a custom one:

1. **Prepare Your Icon File**
   - Format: `.ico` file (Windows icon format)
   - Recommended sizes: 16x16, 32x32, 48x48, and 256x256 pixels
   - Name: Must be exactly `icon.ico` (all lowercase)

2. **Replace the Icon File**
   ```bash
   # Navigate to the resources directory
   cd src/main/resources/
   
   # Replace with your icon file
   cp /path/to/your/custom-icon.ico icon.ico
   ```

3. **Rebuild the Application**
   ```bash
   # Clean and rebuild
   mvn clean package
   
   # Or run with JavaFX
   mvn javafx:run
   ```

## Testing the Icon

After replacing the icon, run the application to verify:

```bash
# Run the JavaFX application
mvn javafx:run
```

The application window should display your custom icon in the title bar. For taskbar and Windows search icons, you need to build the MSI installer using the GitHub Actions workflow.

## Troubleshooting

### Icon Not Showing in Application Window

If the icon doesn't appear in the application window:

1. **Check File Location**: Verify the file is at `src/main/resources/icon.ico`
2. **Check File Name**: Must be exactly `icon.ico` (all lowercase)
3. **Check Console**: Look for warning messages like:
   ```
   Warning: /icon.ico not found in resources. Using default application icon.
   ```
4. **Rebuild**: Run `mvn clean package` to ensure resources are properly included

### Icon Not Showing in Taskbar/Search

If the icon doesn't appear in the Windows taskbar or search:

- This requires building and installing the MSI package via the GitHub Actions workflow
- The icon is embedded during the `jpackage` step
- Download and install the MSI from GitHub Actions artifacts to test

## Technical Details

### JavaFX Window Icon

The icon is loaded in `ExcelJsonApplication.java` using:
```java
String iconPath = "/icon.ico";
try (InputStream iconStream = getClass().getResourceAsStream(iconPath)) {
    if (iconStream != null) {
        primaryStage.getIcons().add(new Image(iconStream));
    }
}
```

### Windows Installer Icon

The icon is embedded in the MSI installer via the GitHub Actions workflow:
```yaml
jpackage --icon src/main/resources/icon.ico ...
```

The `--icon` parameter tells jpackage to use this icon for the installed application, which affects the taskbar and Windows search appearance.
