# Application Icon Resource Instructions

## Overview

The FlowForge application has been updated to load a custom application icon (`ICON.ico`) from the project's resources directory. This icon will appear in the application window title bar and taskbar.

## Icon File Location

The application expects the icon file at:
```
src/main/resources/ICON.ico
```

## Current Status

**Note**: A default `ICON.ico` file has been copied from the existing `icon.ico` in the resources directory. If you need to use a different icon, follow the instructions below.

## How to Add or Replace the Icon

### Option 1: Use the Default Icon (Already Done)

The repository currently includes a copy of the existing icon.ico as ICON.ico. If this is acceptable, no further action is needed.

### Option 2: Provide a Custom Icon

If you need to use a custom icon:

1. **Prepare Your Icon File**
   - Format: `.ico` file (Windows icon format)
   - Recommended sizes: 16x16, 32x32, 48x48, and 256x256 pixels
   - Name: Must be exactly `ICON.ico` (uppercase)

2. **Add the Icon to Resources**
   ```bash
   # Navigate to the resources directory
   cd src/main/resources/
   
   # Replace the existing ICON.ico with your custom icon
   cp /path/to/your/custom-icon.ico ICON.ico
   ```

3. **Rebuild the Application**
   ```bash
   # Clean and rebuild
   mvn clean package
   
   # Or run with JavaFX
   mvn javafx:run
   ```

## Testing the Icon

After adding or replacing the icon, run the application to verify:

```bash
# Run the JavaFX application
mvn javafx:run
```

The application window should display:
- Title: "FlowForge V1.1"
- Icon: Your custom icon in the title bar and taskbar

## Troubleshooting

### Icon Not Showing

If the icon doesn't appear:

1. **Check File Location**: Verify the file is at `src/main/resources/ICON.ico`
2. **Check File Name**: Must be exactly `ICON.ico` (all uppercase)
3. **Check Console**: Look for warning messages like:
   ```
   Warning: ICON.ico not found in resources. Using default application icon.
   ```
4. **Rebuild**: Run `mvn clean package` to ensure resources are properly included

### Building a Distribution

When building a distribution package (e.g., MSI installer), ensure the icon is included in the resources directory before running the build.

## Technical Details

The icon is loaded in `ExcelJsonApplication.java` using:
```java
try (InputStream iconStream = getClass().getResourceAsStream("/ICON.ico")) {
    if (iconStream != null) {
        primaryStage.getIcons().add(new Image(iconStream));
    } else {
        System.err.println("Warning: ICON.ico not found in resources.");
    }
} catch (Exception e) {
    System.err.println("Warning: Failed to load ICON.ico: " + e.getMessage());
}
```

The code handles missing or invalid icons gracefully by logging a warning and using the default Java icon.
