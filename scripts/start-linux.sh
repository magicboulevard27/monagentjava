#!/usr/bin/env bash
set -euo pipefail

APP_HOME="${APP_HOME:-/opt/monagent}"
JAVA_BIN="${JAVA_BIN:-java}"
JAR_FILE="${JAR_FILE:-$APP_HOME/app/monagentjava.jar}"
JAVA_OPTS="${JAVA_OPTS:--XX:+UseG1GC -XX:MaxRAMPercentage=75.0 -Duser.timezone=UTC}"
PROFILE="${SPRING_PROFILES_ACTIVE:-production}"

exec "$JAVA_BIN" $JAVA_OPTS -jar "$JAR_FILE" --spring.profiles.active="$PROFILE"

