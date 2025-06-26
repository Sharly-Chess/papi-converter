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
javac -d "$BUILD_DIR/classes" "$JAVA_DIR/PapiConverter.java"

echo "Creating runnable JAR..."
cd "$BUILD_DIR/classes"
jar cfe "$DIST_DIR/java/papiconverter.jar" org.sharlychess.papiconverter.PapiConverter org/sharlychess/papiconverter/PapiConverter.class
cd "$ROOT_DIR"
