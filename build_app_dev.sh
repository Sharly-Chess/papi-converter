#!/bin/bash
set -e

# Paths
ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"
BUILD_DIR="$ROOT_DIR/build"
DIST_DIR="$ROOT_DIR/dist"
JAVA_DIR="$ROOT_DIR/java"

# Use default system Java instead of GraalVM for faster compilation
# This is much faster than the GraalVM native-image build when in dev

if [ -z "$JAVA_HOME" ]; then
    export JAVA_HOME=$(/usr/libexec/java_home)
fi
export PATH="$JAVA_HOME/bin:$PATH"

echo "Using Java: $(java -version 2>&1 | head -n 1)"

# Build classpath for libraries (same as Windows version)
CP="$ROOT_DIR/lib/jackcess-4.0.5.jar:$ROOT_DIR/lib/commons-lang3-3.12.0.jar:$ROOT_DIR/lib/commons-logging-1.2.jar:$ROOT_DIR/lib/jackson-core-2.15.2.jar:$ROOT_DIR/lib/jackson-databind-2.15.2.jar:$ROOT_DIR/lib/jackson-annotations-2.15.2.jar:$ROOT_DIR/lib/h2-2.2.224.jar:$ROOT_DIR/lib/slf4j-api-2.0.9.jar:$ROOT_DIR/lib/slf4j-simple-2.0.9.jar"

echo "Cleaning previous build..."
rm -rf "$BUILD_DIR" "$DIST_DIR"
mkdir -p "$BUILD_DIR/classes" "$DIST_DIR/java"

echo "Compiling Java..."
javac --release 21 -cp "$CP" -d "$BUILD_DIR/classes" "$JAVA_DIR"/*.java

echo "Extracting dependencies..."
cd "$BUILD_DIR/classes"
jar xf "$ROOT_DIR/lib/jackcess-4.0.5.jar"
jar xf "$ROOT_DIR/lib/commons-lang3-3.12.0.jar"
jar xf "$ROOT_DIR/lib/commons-logging-1.2.jar"
jar xf "$ROOT_DIR/lib/jackson-core-2.15.2.jar"
jar xf "$ROOT_DIR/lib/jackson-databind-2.15.2.jar"
jar xf "$ROOT_DIR/lib/jackson-annotations-2.15.2.jar"
jar xf "$ROOT_DIR/lib/h2-2.2.224.jar"
jar xf "$ROOT_DIR/lib/slf4j-api-2.0.9.jar"
jar xf "$ROOT_DIR/lib/slf4j-simple-2.0.9.jar"

echo "Removing META-INF to avoid conflicts..."
rm -rf META-INF

echo "Creating fat JAR..."
jar cfe "$DIST_DIR/java/papiconverter.jar" org.sharlychess.papiconverter.PapiConverter .
cd "$ROOT_DIR"

echo "Copying static resources..."
cp -r "$ROOT_DIR/static" "$DIST_DIR/java/"

# Create a simple run script for convenience
cat > run_dev.sh << 'EOF'
#!/bin/bash
# Quick development runner for papi-converter
# Usage: ./run_dev.sh <arguments>

ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"
java -jar "$ROOT_DIR/dist/java/papiconverter.jar" "$@"
EOF

chmod +x run_dev.sh

echo ""
echo "Development build completed successfully!"
echo ""
echo "Fat JAR created: dist/java/papiconverter.jar"
echo "Quick runner: ./run_dev.sh"
echo ""
echo "Usage examples:"
echo "  ./run_dev.sh input.json output.papi"
echo "  ./run_dev.sh input.papi output.json"
echo "  ./run_dev.sh --playerdb Data.mdb players.sql"
echo ""
echo "Or run directly with:"
echo "  java -jar dist/java/papiconverter.jar <arguments>"
