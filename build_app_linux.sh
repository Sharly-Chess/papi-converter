#!/bin/bash
set -e

# Paths
ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"
BUILD_DIR="$ROOT_DIR/build"
DIST_DIR="$ROOT_DIR/dist"
JAVA_DIR="$ROOT_DIR/java"

# Use system Java (OpenJDK ≥ 21)
if [ -z "$JAVA_HOME" ]; then
    JAVA_HOME=$(dirname "$(dirname "$(readlink -f "$(which javac)")")")
    export JAVA_HOME
fi
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
jar cfe "$DIST_DIR/java/papiconverter.jar" org.sharlychess.papiconverter.PapiConverter .

# Copy static resources to distribution
echo "Copying static resources..."
cp -r "$ROOT_DIR/static" "$DIST_DIR/"

# Create quick runner as a binary
cat > "$ROOT_DIR/papi-converter" << 'EOF'
#!/bin/bash
DIR="$(cd "$(dirname "$0")" && pwd)"
"$DIR/jre-linux/bin/java" -cp "$DIR/dist/java/papiconverter.jar" org.sharlychess.papiconverter.PapiConverter "$@"
EOF

chmod +x "$ROOT_DIR/papi-converter"

echo "Build completed successfully! Binary is located at $ROOT_DIR/papi-converter"
