#!/bin/sh
# Auto-downloads Gradle wrapper if missing
APP_BASE_NAME=$(basename "$0")
APP_HOME=$(dirname "$0")
CLASSPATH=$APP_HOME/gradle/wrapper/gradle-wrapper.jar

if [ ! -f "$CLASSPATH" ]; then
    echo "Downloading Gradle wrapper..."
    mkdir -p "$APP_HOME/gradle/wrapper"
    curl -sL "https://raw.githubusercontent.com/gradle/gradle/v8.10.2/gradle/wrapper/gradle-wrapper.jar" -o "$CLASSPATH"
    curl -sL "https://raw.githubusercontent.com/gradle/gradle/v8.10.2/gradle/wrapper/gradle-wrapper.properties" -o "$APP_HOME/gradle/wrapper/gradle-wrapper.properties"
fi

exec java -cp "$CLASSPATH" org.gradle.wrapper.GradleWrapperMain "$@"
