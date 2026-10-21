#!/bin/sh
set -eu

if [ -z "${SUPABASE_DATABASE_URL:-}" ]; then
  : "${DB_USER:?DB_USER is required}"
  : "${DB_PASSWORD:?DB_PASSWORD is required}"
  if [ -z "${DB_URL:-}" ]; then
    : "${DB_HOST:?DB_HOST is required when DB_URL is not set}"
    export DB_URL="jdbc:mysql://${DB_HOST}:3306/foundly?sslMode=${DB_SSL_MODE:-REQUIRED}&connectionTimeZone=UTC&forceConnectionTimeZoneToSession=true"
  fi
fi
export UPLOAD_DIR="${UPLOAD_DIR:-/var/data/images}"
export SPRING_PROFILES_ACTIVE="${SPRING_PROFILES_ACTIVE:-supabase,prod}"
export SERVER_PORT="${SERVER_PORT:-8080}"
if [ ! -f "${SUPABASE_SSL_ROOT_CERT:-}" ] && [ -f "/app/prod-ca-2021.crt" ]; then
  export SUPABASE_SSL_ROOT_CERT="/app/prod-ca-2021.crt"
fi
mkdir -p "$UPLOAD_DIR"
chown -R foundly:foundly "$UPLOAD_DIR" 2>/dev/null || true
chmod 750 "$UPLOAD_DIR" 2>/dev/null || true
exec gosu foundly java -jar /app/app.jar
