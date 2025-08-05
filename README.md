# PAPI Converter

A Java utility to convert between JSON configuration files and PAPI (.mdb) tournament database files used by chess tournament management software.

## Features

- **JSON to PAPI**: Convert JSON tournament configuration files to PAPI database format
- **PAPI to JSON**: Convert PAPI database files to JSON format
- **Data.mdb to sql dump**: Convert the FFE player database files to an DQLite dump format
- **Cross-platform**: Works on macOS, Linux, and Windows
- **Native Image**: Compiled to OS-native binary using GraalVM

## Requirements

- GraalVM Setup
- Internet connection (for downloading dependencies and dependencies)

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

3. **Run the converter:**
   ```bash
   ./dist/papi-converter-native input.json [output.papi]
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

3. **Create minimal JRE (you only need to do this once):**

   ```cmd
   build_jre_win.bat
   ```

4. **Run the converter:**
   ```cmd
   .\dist\papi-converter-native.exe input.json [output.papi]
   ```

## Usage

### Convert JSON to PAPI

```bash
# macOS/Linux
./dist/papi-converter-native tournament.json tournament.papi

# Windows
.\dist\papi-converter-native.exe tournament.json tournament.papi

# Auto-generate output filename
./dist/papi-converter-native tournament.json
```

### Convert PAPI to JSON

```bash
./dist/papi-converter-native tournament.papi tournament.json
```

## JSON Format

The JSON configuration file should contain a `variables` object with tournament settings:

```json
{
  "variables": {
    "name": "Tournament Example",
    "type": "Swiss",
    "rounds": "7",
    "pairing": "Suisse",
    "timeControl": "90min+30sec",
    "ratingClass": "Standard",
    "minRating": "1200",
    "maxRating": "2400",
    "tiebreak1": "Brésilien",
    "tiebreak2": "Performance",
    "tiebreak3": "94",
    "pointSystem": "1",
    "venue": "Paris Chess Club",
    "startDate": "2024-01-15",
    "endDate": "2024-01-21",
    "arbiter": "John Smith",
    "homologation": "12345"
  },
  "players": [
    {
      "lastName": "Player surname",
      "firstName": "Player first name",
      "gender": "M/F",
      "birthDate": "DD/MM/YYYY",
      "elo": 1850,
      "rapidElo": 1820,
      "blitzElo": 1800,
      "federation": "FRA",
      "club": "Club name",
      "email": "player@example.com",
      "phone": "0123456789",
      "rounds": [
        { "color": "B", "opponent": 3, "result": 3 },
        { "color": "N", "result": 2 }
      ]
    }
  ]
}
```

See `example.json` for a complete example.

### Tournament Variables

The converter uses English variable names in JSON and maps them to the corresponding French names in the PAPI database:

| English Name               | French Name    | Description                                |
| -------------------------- | -------------- | ------------------------------------------ |
| `name`                     | Nom            | Tournament name                            |
| `type`                     | Genre          | Tournament type (Swiss, Round Robin, etc.) |
| `rounds`                   | NbrRondes      | Number of rounds                           |
| `pairing`                  | Pairing        | Pairing system                             |
| `timeControl`              | Cadence        | Time control                               |
| `ratingClass`              | ClassElo       | Rating classification                      |
| `tieBreakLowerRatingLimit` | EloBase1       | Minimum rating                             |
| `tieBreakUpperRatingLimit` | EloBase2       | Maximum rating                             |
| `tiebreak1`                | Dep1           | First tiebreak system                      |
| `tiebreak2`                | Dep2           | Second tiebreak system                     |
| `tiebreak3`                | Dep3           | Third tiebreak system                      |
| `pointSystem`              | DecomptePoints | Point counting system                      |
| `venue`                    | Lieu           | Tournament venue                           |
| `startDate`                | DateDebut      | Start date                                 |
| `endDate`                  | DateFin        | End date                                   |
| `arbiter`                  | Arbitre        | Chief arbiter                              |
| `homologation`             | Homologation   | Homologation number                        |

### Round Structure

**Note**: Starting from version 2.0, rounds are represented as a dictionary/object where the key is the round number (as a string) and the value contains the round data. This ensures that byes and forfeits are correctly associated with their actual round numbers.

**New Dictionary Format (Recommended)**:

```json
"rounds": {
  "1": {"color": "B", "opponent": 3, "result": 3},
  "3": {"color": "N", "result": 2}
}
```

**Legacy Array Format (Still Supported)**:

```json
"rounds": [
  {"color": "B", "opponent": 3, "result": 3},
  {"color": "N", "result": 2}
]
```

Each player can have up to 24 rounds with the following structure:

- **color**: Player's piece color

  - `"B"` = Black pieces
  - `"N"` = White pieces (Note: N for "blanc" in French)
  - `"R"` = Unplayed
  - `"F"` = Forfeit

- **opponent**: Reference ID of the opponent player (optional for byes/forfeits)

- **result**: Game result from this player's perspective
  - `0` = No result/Not played
  - `1` = Loss
  - `2` = Draw
  - `3` = Win
  - `4` = Forfeit loss
  - `5` = Double forfeit
  - `6` = Forfeit win (bye round) - **Auto-detects opponent as EXEMPT**
  - `7` = Zero point bye
  - `8` = Half point bye
  - `9` = Pairing allocated bye
  - `10` = Full point bye
  - `11` = Unrated loss
  - `12` = Unrated draw
  - `13` = Unrated win
  - `14` = Rest game

### Bye Round Auto-Detection

When a player receives a bye (forfeit win), you can simply specify `"result": 6` without an opponent:

```json
{
  "rounds": [
    { "color": "B", "result": 6 } // Automatically plays against EXEMPT player
  ]
}
```

The converter will automatically:

- Set the opponent to player 1 (EXEMPT)
- Update the EXEMPT player's record to show they played as Black with result 0
- Create proper pairing records in both directions

### Important Player Data Notes

- Birth dates must be in `DD/MM/YYYY` format
- ClubRef is automatically set to 0
- Phone numbers are limited to 10 characters
- All unspecified rounds default to color `"R"` and result `0`
- Player references start from 2 (1 is reserved for EXEMPT)

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
