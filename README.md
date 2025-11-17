# Engage FlowForge 3.0

**Engage FlowForge 3.0** is a Java application that converts Vocera Engage Excel configuration sheets into JSON rule files. The application provides both a graphical user interface (GUI) and a command-line interface (CLI) for processing Excel workbooks containing nurse call and patient monitoring configurations.

![Java](https://img.shields.io/badge/Java-17-orange)
![JavaFX](https://img.shields.io/badge/JavaFX-21.0.3-blue)
![Maven](https://img.shields.io/badge/Maven-3.9+-green)
![License](https://img.shields.io/badge/License-Proprietary-red)

## 🚀 Quick Start

### Prerequisites
- **Java 17 or higher** (Required for running the JAR file directly)
  - Download from [Oracle](https://www.oracle.com/java/technologies/downloads/) or [Adoptium](https://adoptium.net/)
- **Maven 3.6+** (only needed for building from source)

> **⚠️ Important:** If you don't have Java 17+ installed, use the **MSI installer** (Option 1 below) which includes a bundled Java runtime and doesn't require Java to be installed on your system.

### Download & Run

#### Option 1: Download Pre-built Installer (Recommended - No Java Required)
**Windows:**
1. Go to the [Actions](../../actions) tab
2. Find the latest successful build
3. Download `engage-rules-generator-msi` artifact
4. Extract and run `EngageFlowForge-3.0.msi`
5. The installer includes Java runtime - no separate Java installation needed

**macOS:**
1. Go to the [Actions](../../actions) tab
2. Find the latest successful build
3. Download `engage-rules-generator-dmg` artifact
4. Extract and run `EngageFlowForge-3.0.dmg`

#### Option 2: Run JAR File (Requires Java 17+)
```bash
# Verify you have Java 17 or higher
java -version

# If version is less than 17, download and install Java 17+ first
# Then run:
java -jar engage-rules-generator-3.0.0.jar
```

**Common Error:** If you see `UnsupportedClassVersionError: Unsupported major.minor version 61.0`, your Java version is too old. Either:
- Install Java 17+ and try again, OR
- Use the MSI installer (Option 1) which includes Java

#### Option 3: Build from Source
```bash
# Clone the repository
git clone https://github.com/Bussyny-boop/NDW-To-Engage-Rules.git
cd NDW-To-Engage-Rules

# Build with Maven
mvn clean package

# Run the application
java -jar target/engage-rules-generator-3.0.0.jar
```

## 📖 Features

- **Graphical User Interface (GUI)**: User-friendly interface for processing Excel workbooks
- **Command-Line Interface (CLI)**: Automation-ready batch processing
- **Excel to JSON Conversion**: Converts Nurse Call and Patient Monitoring configurations
- **Dark Mode Support**: Modern UI with dark theme option
- **Custom Units & Tabs**: Support for custom recipient configurations
- **Room Filtering**: Filter flows by specific room numbers
- **Configuration Merging**: Intelligently merge identical delivery flows
- **Comprehensive Validation**: Built-in validation for Excel structure and data

## 🖥️ GUI Mode

Launch the GUI by running the JAR file:

```bash
java -jar engage-rules-generator-2.0.0.jar
```

The GUI provides:
- File selection for Excel workbooks
- Output directory configuration
- Visual progress tracking
- Error reporting and validation
- Dark mode toggle
- Settings panel for advanced options

## 🔧 CLI Mode

For automation and batch processing:

```bash
# Export to JSON
java -cp engage-rules-generator-2.0.0.jar \
  com.example.exceljson.jobs.JobRunner export-json input.xlsx output.json

# Export to YAML
java -cp engage-rules-generator-2.0.0.jar \
  com.example.exceljson.jobs.JobRunner export-yaml input.xlsx output.yml

# Validate Excel workbook
java -cp engage-rules-generator-2.0.0.jar \
  com.example.exceljson.jobs.JobRunner validate input.xlsx
```

## 📚 Documentation

- **[User Guide](USER_GUIDE.md)**: Comprehensive documentation for all features
- **[Sample Workbook Guide](docs/sample-workbook.md)**: Excel structure requirements
- **[Custom Unit Feature](docs/CUSTOM_UNIT_FEATURE.md)**: Custom recipient configuration

## 🏗️ Building the Project

### Build JAR
```bash
mvn clean package
```

The shaded JAR will be created at: `target/engage-rules-generator-2.0.0.jar`

### Run Tests
```bash
mvn test
```

### Run GUI in Development
```bash
mvn javafx:run
```

## 🧪 Testing

The project includes comprehensive test coverage with 291+ unit tests covering:
- Excel parsing logic
- JSON/YAML serialization
- Flow merging algorithms
- Validation rules
- UI components

## 🛠️ Technology Stack

- **Java 17**: Core language
- **JavaFX 21.0.3**: GUI framework
- **Apache POI 5.2.5**: Excel file processing
- **Jackson 2.17.2**: JSON/YAML serialization
- **Log4j 2.22.1**: Logging framework
- **JUnit 5.10.2**: Testing framework
- **Maven**: Build and dependency management

## 🔐 Security

The project uses:
- Log4j 2.22.1 (patched against Log4Shell vulnerabilities)
- Regular dependency updates
- Automated security scanning via GitHub Actions

## 🤝 Contributing

Contributions are welcome! Please:
1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📝 License

This is proprietary software. All rights reserved.

## 🆘 Support

For issues or questions:
1. Check the [User Guide](USER_GUIDE.md)
2. Review existing [Issues](../../issues)
3. Create a new issue with detailed information

## 📋 Version History

See [CHANGELOG.md](CHANGELOG.md) for detailed version history.

**Current Version**: 2.0.0

---

**Made with ❤️ for healthcare workflows**
