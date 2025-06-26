#!/bin/bash
set -e

# Paths
ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"
BUILD_DIR="$ROOT_DIR/build"
DIST_DIR="$ROOT_DIR/dist"
JAVA_DIR="$ROOT_DIR/java"

echo "Cleaning previous build..."
rm -rf "$BUILD_DIR" "$DIST_DIR"
mkdir -p "$BUILD_DIR/classes" "$DIST_DIR/java"

echo "Compiling Java..."
CP="$ROOT_DIR/lib/jackcess-4.0.5.jar:$ROOT_DIR/lib/commons-lang3-3.12.0.jar:$ROOT_DIR/lib/commons-logging-1.2.jar:$ROOT_DIR/lib/jackson-core-2.15.2.jar:$ROOT_DIR/lib/jackson-databind-2.15.2.jar:$ROOT_DIR/lib/jackson-annotations-2.15.2.jar"
javac -cp "$CP" -d "$BUILD_DIR/classes" "$JAVA_DIR/PapiConverter.java"

echo "Creating runnable JAR..."
cd "$BUILD_DIR/classes"

# Extract dependencies into build directory
jar xf "$ROOT_DIR/lib/jackcess-4.0.5.jar"
jar xf "$ROOT_DIR/lib/commons-lang3-3.12.0.jar"
jar xf "$ROOT_DIR/lib/commons-logging-1.2.jar"
jar xf "$ROOT_DIR/lib/jackson-core-2.15.2.jar"
jar xf "$ROOT_DIR/lib/jackson-databind-2.15.2.jar"
jar xf "$ROOT_DIR/lib/jackson-annotations-2.15.2.jar"

# Remove META-INF to avoid conflicts
rm -rf META-INF

# Create fat JAR with all dependencies
jar cfe "$DIST_DIR/java/papiconverter.jar" org.sharlychess.papiconverter.PapiConverter .
cd "$ROOT_DIR"
