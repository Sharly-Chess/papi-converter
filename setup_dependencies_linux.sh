#!/bin/bash
set -e

echo "Setting up PAPI Converter dependencies..."

# Check for Java 21 + JDK
INSTALL_JAVA=false
if command -v java >/dev/null 2>&1; then
    JAVA_VER=$(java -version 2>&1 | head -n 1 | awk -F '"' '{print $2}' | cut -d. -f1)
    echo "Java found: version $JAVA_VER"
    if (( "$JAVA_VER" < 21 )); then
        echo "Java version is too old (requires >= 21)"
        INSTALL_JAVA=true
    fi
else
    echo "Java not found"
    INSTALL_JAVA=true
fi

# Check if javac exists
if ! command -v javac >/dev/null 2>&1; then
    echo "javac (JDK) not found"
    INSTALL_JAVA=true
fi

# Install Java 21 + JDK if needed
if [ "$INSTALL_JAVA" = true ]; then
    # Detect package manager
    if command -v dnf >/dev/null 2>&1; then
        PM="dnf"
        INSTALL="sudo dnf install -y"
    elif command -v apt >/dev/null 2>&1; then
        PM="apt"
        INSTALL="sudo apt update && sudo apt install -y"
    elif command -v pacman >/dev/null 2>&1; then
        PM="pacman"
        INSTALL="sudo pacman -Sy --noconfirm"
    elif command -v zypper >/dev/null 2>&1; then
        PM="zypper"
        INSTALL="sudo zypper install -y"
    else
        echo "No compatible package manager detected."
        echo "Please install Java 21 + JDK manually and set JAVA_HOME."
        exit 1
    fi

    echo "→ Using package manager: $PM"
    echo "→ Installing OpenJDK 21 (JDK) ..."
    $INSTALL java-21-openjdk java-21-openjdk-devel curl || {
        echo "If 'java-21-openjdk-devel' is not available on this distribution,"
        echo "please install a JDK manually and set JAVA_HOME."
    }
else
    echo "Java 21 + JDK already installed, skipping installation"
fi

# Create lib directory
mkdir -p lib

# Download Jackcess and dependencies
echo "Downloading Jackcess..."
curl -L -o lib/jackcess-4.0.5.jar "https://repo1.maven.org/maven2/com/healthmarketscience/jackcess/jackcess/4.0.5/jackcess-4.0.5.jar"

echo "Downloading Apache Commons Lang..."
curl -L -o lib/commons-lang3-3.12.0.jar "https://repo1.maven.org/maven2/org/apache/commons/commons-lang3/3.12.0/commons-lang3-3.12.0.jar"

echo "Downloading Apache Commons Logging..."
curl -L -o lib/commons-logging-1.2.jar "https://repo1.maven.org/maven2/commons-logging/commons-logging/1.2/commons-logging-1.2.jar"

# Download Jackson JSON libraries
echo "Downloading Jackson Core..."
curl -L -o lib/jackson-core-2.15.2.jar "https://repo1.maven.org/maven2/com/fasterxml/jackson/core/jackson-core/2.15.2/jackson-core-2.15.2.jar"

echo "Downloading Jackson Databind..."
curl -L -o lib/jackson-databind-2.15.2.jar "https://repo1.maven.org/maven2/com/fasterxml/jackson/core/jackson-databind/2.15.2/jackson-databind-2.15.2.jar"

echo "Downloading Jackson Annotations..."
curl -L -o lib/jackson-annotations-2.15.2.jar "https://repo1.maven.org/maven2/com/fasterxml/jackson/core/jackson-annotations/2.15.2/jackson-annotations-2.15.2.jar"

# Download H2 Database (pure Java SQLite alternative)
echo "Downloading H2 Database..."
curl -L -o lib/h2-2.2.224.jar "https://repo1.maven.org/maven2/com/h2database/h2/2.2.224/h2-2.2.224.jar"

# Download SLF4J logging libraries
echo "Downloading SLF4J API..."
curl -L -o lib/slf4j-api-2.0.9.jar "https://repo1.maven.org/maven2/org/slf4j/slf4j-api/2.0.9/slf4j-api-2.0.9.jar"

echo "Downloading SLF4J Simple..."
curl -L -o lib/slf4j-simple-2.0.9.jar "https://repo1.maven.org/maven2/org/slf4j/slf4j-simple/2.0.9/slf4j-simple-2.0.9.jar"

echo "Dependencies downloaded successfully!"
echo "You can now run: ./build_app_linux.sh"
