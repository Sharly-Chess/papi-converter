# PAPI Converter

A Java utility to convert between JSON configuration files and PAPI (.mdb) tournament database files used by chess tournament management software.

## Features

- **JSON to PAPI**: Convert JSON tournament configuration files to PAPI database format
- **PAPI to JSON**: Convert PAPI database files to JSON format (in development)
- **Cross-platform**: Works on macOS, Linux, and Windows
- **Self-contained**: Includes minimal JRE runtime for easy distribution

## Requirements

- Java 21 or higher (for building)
- Internet connection (for downloading dependencies)

## Quick Start

### macOS/Linux

1. **Download dependencies:**
   ```bash
   ./setup_dependencies.sh
   ```

2. **Build the application:**
   ```bash
   ./build_app_mac.sh
   ```

3. **Create minimal JRE (optional):**
   ```bash
   ./build_jre_mac.sh
   ```

4. **Run the converter:**
   ```bash
   ./papi-converter.sh input.json [output.papi]
   ```

### Windows

1. **Download dependencies:**
   ```cmd
   setup_dependencies.bat
   ```

2. **Build the application:**
   ```cmd
   build_app_win.bat
   ```

3. **Create minimal JRE (optional):**
   ```cmd
   build_jre_win.bat
   ```

4. **Run the converter:**
   ```cmd
   papi-converter.bat input.json [output.papi]
   ```

## Usage

### Convert JSON to PAPI
```bash
# Using shell script (macOS/Linux)
./papi-converter.sh tournament.json tournament.papi

# Using batch file (Windows)
papi-converter.bat tournament.json tournament.papi

# Auto-generate output filename
./papi-converter.sh tournament.json
```

### Convert PAPI to JSON (in development)
```bash
./papi-converter.sh tournament.papi tournament.json
```

## JSON Format

The JSON configuration file should contain a `variables` object with tournament settings:

```json
{
  "variables": {
    "Nom": "Tournament Example",
    "Genre": "Swiss",
    "NbrRondes": "7",
    "Pairing": "Suisse",
    "Cadence": "90min+30sec",
    "ClassElo": "Standard",
    "EloBase1": "1200",
    "EloBase2": "2400",
    "Dep1": "75",
    "Dep2": "92",
    "Dep3": "94",
    "DecomptePoints": "1",
    "Lieu": "Paris Chess Club",
    "DateDebut": "2024-01-15",
    "DateFin": "2024-01-21",
    "Arbitre": "John Smith",
    "Homologation": "12345"
  }
}
```

See `example.json` for a complete example.

## Build Details

### Dependencies

The application uses the following libraries:
- **Jackcess 4.0.5**: For reading/writing Microsoft Access (.mdb) files
- **Apache Commons Lang 3.12.0**: Utility functions
- **Apache Commons Logging 1.2**: Logging framework
- **Jackson 2.15.2**: JSON processing (Core, Databind, Annotations)

### Build Process

1. **Dependency Setup**: Downloads JAR files from Maven Central
2. **Compilation**: Compiles Java source with dependencies on classpath
3. **Fat JAR Creation**: Extracts all dependencies and packages them into a single executable JAR
4. **JRE Creation**: Creates a minimal Java runtime with only required modules

### Project Structure

```
papi-converter/
├── java/                    # Java source files
│   └── PapiConverter.java
├── lib/                     # Downloaded dependencies (created by setup script)
├── static/                  # Template files
│   └── template-3.3.8.papi
├── build/                   # Build artifacts (created during build)
├── dist/                    # Distribution files (created during build)
├── jre-mac/                 # macOS JRE (created by build_jre_mac.sh)
├── jre-win/                 # Windows JRE (created by build_jre_win.bat)
├── setup_dependencies.sh   # macOS/Linux dependency installer
├── setup_dependencies.bat  # Windows dependency installer
├── build_app_mac.sh        # macOS/Linux build script
├── build_app_win.bat       # Windows build script
├── build_jre_mac.sh        # macOS/Linux JRE builder
├── build_jre_win.bat       # Windows JRE builder
├── papi-converter.sh       # macOS/Linux launcher
├── papi-converter.bat      # Windows launcher
└── example.json            # Example configuration file
```
