# JPackage Configuration Snippet for Maven

## Overview

This document provides a sample configuration snippet for the `javafx-maven-plugin` to build native installers with custom application name, version, and icon using JPackage.

## Maven Plugin Configuration

Add or update the `javafx-maven-plugin` in your `pom.xml` to include JPackage options:

```xml
<plugin>
    <groupId>org.openjfx</groupId>
    <artifactId>javafx-maven-plugin</artifactId>
    <version>0.0.8</version>
    <configuration>
        <mainClass>com.example.exceljson.Main</mainClass>
        
        <!-- JPackage configuration for building native installers -->
        <jlinkImageName>FlowForge</jlinkImageName>
        <launcher>FlowForge</launcher>
        
        <!-- JPackage options -->
        <options>
            <option>--name</option>
            <option>FlowForge</option>
            
            <option>--app-version</option>
            <option>2.0</option>
            
            <option>--icon</option>
            <option>${project.basedir}/src/main/resources/ICON.ico</option>
            
            <option>--description</option>
            <option>FlowForge - Vocera Engage Rules Generator</option>
            
            <option>--vendor</option>
            <option>Your Organization Name</option>
            
            <!-- Windows-specific options (for MSI installer) -->
            <option>--win-menu</option>
            <option>--win-shortcut</option>
            <option>--win-dir-chooser</option>
        </options>
    </configuration>
    
    <executions>
        <execution>
            <id>build-installer</id>
            <phase>package</phase>
            <goals>
                <goal>jlink</goal>
                <goal>jpackage</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

## Usage

### Build Native Installer

To build a native installer with the icon:

```bash
# Clean and package
mvn clean package

# Run jpackage goal
mvn javafx:jpackage
```

This will create a native installer in the `target/dist` directory.

### Platform-Specific Installers

#### Windows (MSI/EXE)
```bash
mvn clean javafx:jpackage
```
- Creates MSI or EXE installer
- Icon file must be `.ico` format
- Installer will be in `target/dist/`

#### macOS (DMG/PKG)
```bash
mvn clean javafx:jpackage
```
- Creates DMG or PKG installer
- Icon file should be `.icns` format for macOS
- You may need to provide a separate macOS icon file

#### Linux (DEB/RPM)
```bash
mvn clean javafx:jpackage
```
- Creates DEB or RPM package
- Icon file should be `.png` format for Linux
- You may need to provide a separate Linux icon file

## Icon File Requirements

### Windows (.ico)
- Location: `src/main/resources/ICON.ico`
- Format: Windows icon format
- Recommended sizes: 16x16, 32x32, 48x48, 256x256

### macOS (.icns)
- Location: `src/main/resources/ICON.icns`
- Format: Apple icon format
- Generate from PNG using: `iconutil -c icns icon.iconset`

### Linux (.png)
- Location: `src/main/resources/ICON.png`
- Format: PNG image
- Recommended size: 512x512 or 256x256

## Alternative: GitHub Actions CI/CD

For automated builds, you can configure GitHub Actions to build installers. The project already has `.github/workflows/main.yml` which builds an MSI installer for Windows.

To customize the installer in CI/CD:

1. Ensure `ICON.ico` is committed to the repository
2. Update the workflow to pass icon path to the build
3. Configure jpackage options in the workflow or pom.xml

## Notes

1. **Current Configuration**: The existing `pom.xml` uses the `javafx-maven-plugin` for running the app locally with `mvn javafx:run`. The above snippet is for building native installers.

2. **No Changes to Existing Build**: This configuration is **informational only** and should not replace the existing build configuration unless you specifically want to enable native installer builds.

3. **Module System**: JPackage works best with Java modules. If your project doesn't use modules, you may need additional configuration.

4. **Testing**: Always test the generated installer on the target platform to ensure the icon appears correctly.

## Resources

- [JavaFX Maven Plugin Documentation](https://github.com/openjfx/javafx-maven-plugin)
- [JPackage Guide](https://docs.oracle.com/en/java/javase/17/jpackage/packaging-overview.html)
- [JavaFX JPackage Tutorial](https://openjfx.io/openjfx-docs/#install-java)
