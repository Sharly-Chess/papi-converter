#!/bin/bash
set -e

echo "Setting up PAPI Converter dependencies..."

# Verify Homebrew installation
if ! command -v brew &>/dev/null; then
  echo "Homebrew is not installed. Installing Homebrew..."
  /bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
fi

# Install GraalVM
echo "Installing GraalVM..."
if ! brew list --cask graalvm-jdk22 &>/dev/null; then
  brew tap graalvm/tap
  brew install --cask graalvm-jdk22
else
  echo "GraalVM JDK 22 is already installed."
fi

# Set environment variables for this session
export JAVA_HOME="/Library/Java/JavaVirtualMachines/graalvm-jdk-22/Contents/Home"
export PATH="$JAVA_HOME/bin:$PATH"

echo "GraalVM installed at: $JAVA_HOME"
echo "Note: Add the following to your shell profile (~/.zshrc or ~/.bash_profile):"
echo "export JAVA_HOME=\"/Library/Java/JavaVirtualMachines/graalvm-jdk-22/Contents/Home\""
echo "export PATH=\"\$JAVA_HOME/bin:\$PATH\""

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

# Download SQLite JDBC driver
echo "Downloading SQLite JDBC driver..."
curl -L -o lib/sqlite-jdbc-3.44.1.0.jar "https://repo1.maven.org/maven2/org/xerial/sqlite-jdbc/3.44.1.0/sqlite-jdbc-3.44.1.0.jar"

# Download SLF4J logging libraries
echo "Downloading SLF4J API..."
curl -L -o lib/slf4j-api-2.0.9.jar "https://repo1.maven.org/maven2/org/slf4j/slf4j-api/2.0.9/slf4j-api-2.0.9.jar"

echo "Downloading SLF4J Simple..."
curl -L -o lib/slf4j-simple-2.0.9.jar "https://repo1.maven.org/maven2/org/slf4j/slf4j-simple/2.0.9/slf4j-simple-2.0.9.jar"

echo "Dependencies downloaded successfully!"
echo "You can now run: ./build_app_mac.sh"
