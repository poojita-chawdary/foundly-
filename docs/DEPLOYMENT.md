# Deployment guide

No deployment has been performed. There are no generated hosting URLs or live credentials.

## Supported initial topology

Run one Nginx frontend container, one Java 21 backend instance, MySQL 8, and a persistent image volume. Keep the database and backend private on the application network. Expose only the frontend through an HTTPS reverse proxy or load balancer. The web container proxies `/api` to Java, preserving the single-origin session/CSRF design.

The application needs no paid AI, mail, or external image-recognition service. A hosting provider may charge for compute, database, persistent disks, backups, and traffic. Check the selected provider’s current pricing before provisioning; no provider-specific costs are assumed here.

## Build and configure

1. Run `./mvnw verify` with JDK 21 and `npm ci && npm test && npm run build` before release.
2. Build the included Dockerfiles. Backend build runs Maven verification; frontend uses the committed lockfile.
3. Supply configuration through the platform’s secret manager/environment, never committed files:

| Variable                      | Purpose                                                                |
| ----------------------------- | ---------------------------------------------------------------------- |
| `SPRING_PROFILES_ACTIVE=prod` | Enables required external configuration and Secure cookies             |
| `DB_URL`                      | Managed MySQL JDBC connection string; use provider CA/TLS requirements |
| `DB_USER`                     | Dedicated application database user                                    |
| `DB_PASSWORD`                 | Application database password                                          |
| `UPLOAD_DIR`                  | Absolute path on a persistent mounted volume                           |
| `MEMBERSHIP_MODE=MANUAL`      | Human campus verification, rather than demo open registration          |

4. Use a managed MySQL 8-compatible server with verified TLS. Example connection parameters include `sslMode=VERIFY_IDENTITY`; install the provider CA through Java’s supported trust-store configuration as required. Do not disable certificate validation.
5. Run Flyway migrations once as part of startup/release. Hibernate uses `ddl-auto=validate`. Give migrations the required schema privileges; for a larger deployment, use separate migration and runtime database roles.
6. Terminate HTTPS at a trusted proxy, and keep direct backend access private. The production Spring profile enables Secure, HttpOnly, SameSite=Lax session cookies. Never enable OPEN membership unintentionally for a real campus.
7. Create and manually verify the first administrator using the documented database operator process. No known admin password is built in.
8. Back up MySQL and the image volume together. Practise restoring them to a test environment. Keep `.env`, volumes, and backups out of source control.

## Reverse proxy details

The supplied Nginx file is intended for local Compose. For production HTTPS termination:

- At the TLS edge, overwrite forwarded headers; never pass untrusted caller-supplied values as authoritative.
- If the web container receives trusted `X-Forwarded-Proto` from the edge, preserve the trusted HTTPS value when forwarding to Spring, rather than overwriting it with the internal HTTP `$scheme`.
- Set HSTS at the HTTPS edge after verifying the HTTPS configuration.
- Keep the configured CSP and nosniff headers, and apply upload/body limits and request timeouts.
- Configure IP-aware login/write limits at the trusted edge. The application’s simple rate limiter otherwise sees the proxy’s IP, sharing its quota among users.

Keep frontend and API on the same origin. If you intentionally split origins, you must separately design explicit credentialed CORS allowlists, cookie domain/SameSite handling, and CSRF token delivery. The included configuration intentionally does not implement that topology.

## Health and release checks

`GET /actuator/health` exposes a minimal health summary. The backend container is not published directly by Compose. A platform health check should reach this endpoint on the private application network. Nginx serves the built frontend and proxies API paths.

Before accepting users, validate **against real MySQL**: clean migrations, registration/session/CSRF, two-account report→claim→return, image persistence after restart, hidden-report privacy, role restrictions, manual membership verification, backup/restore, and HTTPS cookies. The automated H2 suite is useful but does not verify MySQL collation, production deadlocks, disk permissions, reverse-proxy settings, or browser rendering.

## Scaling and persistent storage

Images use a configured persistent filesystem path, not ephemeral container storage. There is no object-storage implementation in this MVP. Use a durable volume for one instance. To scale horizontally, implement object storage behind an image storage interface, shared sessions (Spring Session/JDBC or Redis), distributed abuse controls, and background candidate matching. Do not run multiple instances against separate local image folders.

## Migrations and recovery

Do not edit an applied migration in a running environment. Add a new `V2__...sql` migration. Back up first and test against a restored copy of the production database. For rollback, redeploy a compatible previous application version; a destructive schema rollback requires a separately reviewed recovery plan. Removing a Docker volume is not a migration strategy.
