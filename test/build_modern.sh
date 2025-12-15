#!/bin/bash
# Build the project with FlatLaf support

cd "$(dirname "$0")/.."

echo "Building NomaUBL with FlatLaf support..."

# Clean build directory
rm -rf build/Frames
rm -rf build/custom
rm -rf build/nomaubl
mkdir -p build

# Compile all source files with FlatLaf in classpath
javac -d build -cp "lib/*" \
    src/custom/resources/*.java \
    src/custom/ubl/*.java \
    src/nomaubl/*.java \
    src/Frames/*.java

# Copy form files and resources
cp -r src/Frames/*.form build/Frames/ 2>/dev/null || true
cp -r src/META-INF build/ 2>/dev/null || true

echo "Build complete!"
echo ""
echo "To run the modern config manager:"
echo "  ./test/run_modern_config.sh"
echo ""
echo "Or with main GUI:"
echo "  java -cp build:lib/* Frames.Main"
