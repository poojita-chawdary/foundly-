#!/bin/sh
set -eu

: "${DB_USER:?DB_USER is required}"
: "${DB_PASSWORD:?DB_PASSWORD is required}"
if [ -z "${DB_URL:-}" ]; then
  : "${DB_HOST:?DB_HOST is required when DB_URL is not set}"
  export DB_URL="jdbc:mysql://${DB_HOST}:3306/foundly?sslMode=${DB_SSL_MODE:-REQUIRED}&connectionTimeZone=UTC&forceConnectionTimeZoneToSession=true"
fi
export UPLOAD_DIR="${UPLOAD_DIR:-/var/data/images}"
export SPRING_PROFILES_ACTIVE="${SPRING_PROFILES_ACTIVE:-prod}"
export SERVER_PORT="${SERVER_PORT:-8080}"
# Render mounts the disk after the image build; initialise its child directory
# at runtime, then replace this process with an unprivileged JVM.
case "$UPLOAD_DIR" in
  /var/data/images) ;;
  *) echo "Expected UPLOAD_DIR=/var/data/images for this container" >&2; exit 1 ;;
esac
mkdir -p "$UPLOAD_DIR"
chown foundly:foundly "$UPLOAD_DIR"
chmod 750 "$UPLOAD_DIR"
exec gosu foundly java -jar /app/app.jar
