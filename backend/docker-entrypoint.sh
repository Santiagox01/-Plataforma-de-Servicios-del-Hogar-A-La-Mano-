#!/bin/sh
set -e

UPLOAD_DIR="${STORAGE_LOCAL_DIR:-/data/uploads}"

if [ "$(id -u)" = "0" ]; then
  mkdir -p "$UPLOAD_DIR"
  chown -R alamano:alamano "$UPLOAD_DIR"
  exec su-exec alamano java -XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom -jar /app/app.jar "$@"
fi

exec java -XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom -jar /app/app.jar "$@"
