# Changelog

All notable changes to Engage FlowForge will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [2.0.0] - 2025-11-09

### Changed
- **Version Update**: Updated application version from 1.1.0 to 2.0.0
- **MSI Installer**: Updated MSI installer name from `EngageFlowForge-1.1.msi` to `EngageFlowForge-2.0.msi`
- **Build Artifact**: JAR file now named `engage-rules-generator-2.0.0.jar`
- **JSON Output**: JSON version field updated to "2.0.0"
- **Documentation**: All user-facing documentation updated with version 2.0.0 references

### Added
- **README.md**: Comprehensive project overview and quick start guide
- **CHANGELOG.md**: Version history tracking following Keep a Changelog format
- **Enhanced .gitignore**: Improved to exclude IDE and OS-specific files

### Infrastructure
- GitHub Actions workflow updated to build `EngageFlowForge-2.0.msi`
- All 291 tests passing with new version

## [1.1.0] - Previous Release

### Features from 1.1.0 and earlier:
- Graphical User Interface (GUI) with JavaFX
- Command-Line Interface (CLI) for batch processing
- Dark Mode support
- Custom Unit recipient feature
- Custom Tab configuration support
- Room filtering capability
- Configuration group filtering
- Flow merging for identical configurations
- EMDAN (Emergency Department Alert Network) support
- Breakthrough DND (Do Not Disturb) handling
- Enhanced enunciation options
- VCS (Vocera Communication Server) checkbox labels
- Patient monitoring integration
- Nurse call alarm processing
- Orders workflow support
- Multiple interface type support (Device A, Device B, VMP)
- Comprehensive validation and error reporting
- Excel parsing with flexible header detection
- JSON and YAML export formats
- Retract rules support
- Accept badge phrases configuration
- Priority mapping for VMP events
- Clinical parameter attributes
- Response options (Accept, Reject, Acknowledge)
- Tab animations and visual enhancements
- Settings UI improvements
- Status bar with processing feedback
- Icon resources and branding
- Sidebar collapse/expand functionality
- Left panel redesign
- Theme upgrades (Vocera branding)

### Technical Improvements:
- Java 17 runtime
- JavaFX 21.0.3
- Apache POI 5.2.5 for Excel processing
- Jackson 2.17.2 for JSON/YAML serialization
- Log4j 2.22.1 (security patches applied)
- 291+ comprehensive unit tests
- Maven Shade plugin for fat JAR creation
- GitHub Actions CI/CD pipeline
- MSI installer generation for Windows
- Liberica JDK with bundled JavaFX

---

## Version Numbering

This project follows [Semantic Versioning](https://semver.org/):
- **Major version (2.x.x)**: Breaking changes or significant feature releases
- **Minor version (x.1.x)**: New features, backward compatible
- **Patch version (x.x.0)**: Bug fixes, backward compatible

## Links

- [User Guide](USER_GUIDE.md)
- [Sample Workbook Guide](docs/sample-workbook.md)
- [GitHub Repository](https://github.com/Bussyny-boop/NDW-To-Engage-Rules)
