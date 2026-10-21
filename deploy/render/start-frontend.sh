#!/bin/sh
set -eu
: "${BACKEND_HOST:?BACKEND_HOST is required}"
export PORT="${PORT:-10000}"
case "$PORT" in ''|*[!0-9]*) echo "PORT must be numeric" >&2; exit 1 ;; esac
case "$BACKEND_HOST" in ''|*[!a-zA-Z0-9.-]*) echo "Invalid BACKEND_HOST" >&2; exit 1 ;; esac
export DNS_RESOLVER="$(awk '/^nameserver / { print $2; exit }' /etc/resolv.conf)"
: "${DNS_RESOLVER:?No DNS resolver available}"
# Substitute only deployment variables, leaving Nginx's $host/$uri intact.
export NGINX_ENVSUBST_FILTER='^(PORT|BACKEND_HOST|DNS_RESOLVER)$'
exec /docker-entrypoint.sh "$@"
