#!/bin/sh
# setup.sh - Run this ONCE before building to download Gradle wrapper

echo "=== HyperBoost V5 - Gradle Setup ==="

# Check Java
if ! command -v java >/dev/null 2>&1; then
    echo "ERROR: Java 21+ is required. Please install JDK 21."
    exit 1
fi

JAVA_VER=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d'.' -f1)
echo "Java version detected: $JAVA_VER"

# Check if gradle is installed locally
if command -v gradle >/dev/null 2>&1; then
    echo "Gradle found. Generating wrapper..."
    gradle wrapper --gradle-version=8.8
    echo "Wrapper generated! You can now run: ./gradlew build"
else
    echo "Gradle not found locally. Downloading gradle-wrapper.jar directly..."
    mkdir -p gradle/wrapper
    curl -Lo gradle/wrapper/gradle-wrapper.jar \
        "https://raw.githubusercontent.com/gradle/gradle/v8.8.0/gradle/wrapper/gradle-wrapper.jar" \
        || wget -O gradle/wrapper/gradle-wrapper.jar \
        "https://raw.githubusercontent.com/gradle/gradle/v8.8.0/gradle/wrapper/gradle-wrapper.jar"

    if [ -s gradle/wrapper/gradle-wrapper.jar ]; then
        chmod +x gradlew
        echo "Done! Now run: ./gradlew build"
    else
        echo ""
        echo "Auto-download failed. Manual steps:"
        echo "1. Install Gradle 8.8 from https://gradle.org/releases/"
        echo "2. In this folder, run: gradle wrapper"
        echo "3. Then run: ./gradlew build"
    fi
fi
