#!/bin/sh
set -eu

: "${SUPABASE_DATABASE_URL:?SUPABASE_DATABASE_URL is required}"
: "${SUPABASE_URL:?SUPABASE_URL is required}"
: "${SUPABASE_SERVICE_ROLE_KEY:?SUPABASE_SERVICE_ROLE_KEY is required}"
export SPRING_PROFILES_ACTIVE="${SPRING_PROFILES_ACTIVE:-supabase,prod}"
export SERVER_PORT="${SERVER_PORT:-8080}"
if [ ! -f "${SUPABASE_SSL_ROOT_CERT:-}" ] && [ -f "/app/prod-ca-2021.crt" ]; then
  export SUPABASE_SSL_ROOT_CERT="/app/prod-ca-2021.crt"
fi
exec gosu foundly java -jar /app/app.jar
