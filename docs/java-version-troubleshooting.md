# Java Version Troubleshooting Guide

## Error: UnsupportedClassVersionError

If you encounter this error when trying to run Engage FlowForge:

```
Exception in thread "main" java.lang.UnsupportedClassVersionError: com/example/exceljson/Main : Unsupported major.minor version 61.0
```

### What This Means

This error occurs when you try to run the application with a Java version that's too old. The number `61.0` indicates the application was compiled with Java 17.

### Quick Fix: Use the MSI Installer (Recommended)

The easiest solution is to use the **MSI installer** instead of the JAR file:

1. Go to the [Actions](../../actions) tab in GitHub
2. Find the latest successful build
3. Download the `engage-rules-generator-msi` artifact
4. Extract and run `EngageFlowForge-3.0.msi`

**Advantage:** The MSI installer includes a bundled Java runtime, so you don't need to install Java separately.

### Alternative: Install Java 17 or Higher

If you prefer to run the JAR file directly, you need to install Java 17 or higher:

1. **Check your current Java version:**
   ```bash
   java -version
   ```

2. **If the version is less than 17, download and install Java 17+:**
   - **Oracle JDK:** https://www.oracle.com/java/technologies/downloads/
   - **OpenJDK (Adoptium):** https://adoptium.net/

3. **After installation, verify the version:**
   ```bash
   java -version
   ```
   Should show something like: `openjdk version "17.0.x"` or higher

4. **Run the application:**
   ```bash
   java -jar engage-rules-generator-3.0.0.jar
   ```

### Java Version Reference

| Java Version | Class Version | Compatible? |
|--------------|---------------|-------------|
| Java 6       | 50.0          | ❌ No        |
| Java 7       | 51.0          | ❌ No        |
| Java 8       | 52.0          | ❌ No        |
| Java 11      | 55.0          | ❌ No        |
| Java 17      | 61.0          | ✅ Yes       |
| Java 21      | 65.0          | ✅ Yes       |

### Why Java 17 is Required

Engage FlowForge uses JavaFX 21 for its graphical user interface, which requires Java 17 as the minimum version. This is a requirement from the JavaFX framework and cannot be changed.

### Still Having Issues?

If you've installed Java 17+ and still see the error:

1. **Multiple Java Installations:** You might have multiple Java versions installed. Check which one is being used:
   ```bash
   which java
   java -version
   ```

2. **Update PATH:** Ensure your system PATH points to Java 17+ installation

3. **Use Full Path:** Specify the full path to Java 17:
   ```bash
   "C:\Program Files\Java\jdk-17\bin\java.exe" -jar engage-rules-generator-3.0.0.jar
   ```

4. **Use MSI Installer:** When in doubt, use the MSI installer which works regardless of your system Java version
