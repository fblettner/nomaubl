#!/bin/bash
# Run the modern main interface

cd "$(dirname "$0")/.."

# Build if needed
if [ ! -f "build/Frames/MainModern.class" ]; then
    echo "Building project..."
    ./test/build_modern.sh
fi

# Check for config parameter
CONFIG_FILE="${1:-./test/config/config.properties}"

if [ ! -f "$CONFIG_FILE" ]; then
    echo "Configuration file not found: $CONFIG_FILE"
    echo "Usage: $0 [config_file]"
    echo "Example: $0 ./test/config/config.properties"
    exit 1
fi

echo "Starting NomaUBL Modern UI..."
echo "Config: $CONFIG_FILE"
echo ""

# Run the modern main interface
java -cp "build:lib/*" Frames.MainModern "$CONFIG_FILE"
