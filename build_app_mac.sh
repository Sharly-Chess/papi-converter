#!/bin/bash
set -e

# Paths
ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"
BUILD_DIR="$ROOT_DIR/build"
DIST_DIR="$ROOT_DIR/dist"
JAVA_DIR="$ROOT_DIR/java"

export JAVA_HOME="/Library/Java/JavaVirtualMachines/graalvm-jdk-22/Contents/Home"
export PATH="$JAVA_HOME/bin:$PATH"

# Clean previous build artifacts
echo "Cleaning previous build..."
rm -rf "$BUILD_DIR" "$DIST_DIR"
mkdir -p "$BUILD_DIR" "$DIST_DIR"

# Build classpath from lib directory
LIB_CLASSPATH=""
for jar in lib/*.jar; do
  if [ -z "$LIB_CLASSPATH" ]; then
    LIB_CLASSPATH="$jar"
  else
    LIB_CLASSPATH="$LIB_CLASSPATH:$jar"
  fi
done

# Compile Java source code
echo "Compiling Java..."
javac -cp "$LIB_CLASSPATH" -d "$BUILD_DIR" java/*.java

# Create a new JAR file with compiled classes
echo "Creating JAR..."
cd "$BUILD_DIR"
jar cfe "$ROOT_DIR/papiconverter.jar" org.sharlychess.papiconverter.PapiConverter .

# Build native image
echo "Building native binary..."
cd "$ROOT_DIR"
native-image --no-fallback \
  -H:+UnlockExperimentalVMOptions \
  -H:ReflectionConfigurationFiles=src/main/resources/META-INF/native-image/reflect-config.json \
  -H:ResourceConfigurationFiles=src/main/resources/META-INF/native-image/resource-config.json \
  -H:+JNI \
  --enable-url-protocols=http,https \
  -cp "$ROOT_DIR/papiconverter.jar:$LIB_CLASSPATH" org.sharlychess.papiconverter.PapiConverter "$DIST_DIR/papi-converter"

# Clean up intermediate files
rm "$ROOT_DIR/papiconverter.jar"
cd "$ROOT_DIR"

echo "Build completed successfully! Binary is located at $DIST_DIR/papi-converter"
