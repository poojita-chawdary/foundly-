# Run Foundly locally and prepare Render

## Local development

The app already includes React/Vite, Spring Boot/Java 21, MySQL 8 and a Flyway schema. There is no missing backend to generate.

On Windows, start Docker Desktop. A root `.env` already exists in this checkout; keep it and set its `DB_PASSWORD` and `MYSQL_ROOT_PASSWORD` to different passwords. For a fresh clone only:

```powershell
Copy-Item .env.example .env
```

From the repository root:

```powershell
docker compose up --build
```

Open http://localhost:8080. Compose creates the database, runs migrations, starts Java and serves the frontend. The default local membership mode is OPEN. Register your own accounts. `docker compose down` stops the stack and keeps data; adding `-v` deletes its database and uploads.

For frontend hot reload without containers, install/start MySQL 8, create the database and application user, then use two terminals:

```powershell
# Terminal 1. Substitute actual local credentials.
$env:DB_URL = 'jdbc:mysql://localhost:3306/foundly'
$env:DB_USER = 'foundly'
$env:DB_PASSWORD = '<your local database password>'
cd backend
.\mvnw.cmd spring-boot:run
```

```powershell
# Terminal 2
cd frontend
npm.cmd ci
npm.cmd run dev -- --host 127.0.0.1
```

Open http://localhost:5173. A standalone Java process does not automatically load either `.env` file. Vite forwards `/api` to Java on port 8080. Keep the development server bound to loopback until the dependency findings in [AUDIT.md](AUDIT.md) are addressed.

## Render topology

```text
Browser -- HTTPS --> Render TLS edge --> foundly-web (Nginx + React)
                                          |
                                          +-- /api --> foundly-api:8080 (private Java)
                                                          |
                                                          +-- MySQL:3306 (private, TLS)
                                                          +-- /var/data/images (disk)
```

The frontend always calls relative `/api` URLs. Cookies and CSRF remain same-origin; no CORS or frontend API URL variable is needed. Do not deploy the current frontend as an independent static site pointing directly at another public API origin.

`render.yaml` defines three paid services in Singapore: MySQL and Java on `standard`, Nginx on `starter`, with 10 GB database and 1 GB upload disks. This is a conservative starting configuration, not a capacity benchmark. Review Render's current pricing in the creation screen before provisioning. All automatic deploy triggers are off so a database deployment is not triggered by each source change. The initial Blueprint creation still deploys the services.

Render's managed relational database is Postgres. This app uses the MySQL driver, Flyway MySQL support and MySQL-specific SQL. The included configuration runs **self-managed MySQL on a Render private service**, following [Render's MySQL deployment model](https://render.com/docs/deploy-mysql). Render hosting does not make this MySQL instance a managed database: you own patching, backups and recovery. A managed external MySQL service is an alternative without changing Java; managed Render Postgres requires a separate application migration.

## Files and configuration

| File | Purpose |
| --- | --- |
| `render.yaml` | Private service wiring, generated DB secrets, disks, plans, public health check |
| `deploy/render/backend.Dockerfile` | Java 21 build with tests; initialises mounted upload directory and runs JVM as UID 10001 |
| `deploy/render/frontend.Dockerfile` | Locked Node build with tests; Nginx runtime |
| `deploy/render/mysql.Dockerfile` | MySQL 8.4 with encrypted TCP connections required |
| `deploy/render/start-backend.sh` | Builds JDBC URL from private host; supports an explicit `DB_URL` override |
| `deploy/render/nginx.conf.template` | SPA fallback, `/api` proxy, `/healthz`, TLS-edge forwarding and runtime DNS resolution |
| `deploy/render/database/bootstrap.sql` | Optional empty database creation for an external MySQL server |
| `backend/src/main/resources/db/migration/V1__initial_schema.sql` | Canonical full table schema, indexes, constraints and reference data |
| `scripts/render.ps1` | Check tools, validate Blueprint, build images, deploy existing app services |
| `deploy/render/compose.smoke.yaml`, `scripts/render-smoke.py` | Isolated local verification using the Render images and real MySQL |

The existing V1 migration contains 12 application tables: campuses, categories, campus_locations, users, item_reports, matches, match_dismissals, claims, claim_secrets, notifications, moderation_actions and report_flags. Flyway adds its own history table at startup. There are no seeded users or passwords. V1 seeds only one demo campus, six categories and five locations.

**Let Flyway apply V1 on first startup.** Do not manually import its SQL and then start Flyway against those untracked tables. No duplicate schema file is introduced. Add later changes as V2/V3 migrations; do not edit an applied V1. Hibernate validates the schema.

The Blueprint generates separate MySQL root/application passwords and passes only the application password to Java. MySQL applies initialisation environment variables only on an empty data directory. Changing a password environment variable later does not rotate the database account; use an operator-controlled `ALTER USER` and update the dependent service secret together.

The internal JDBC default is `sslMode=REQUIRED`: encrypted transport, **without server identity verification** against MySQL's generated certificate. Before admitting real users, provision a trusted certificate/CA and use a `DB_URL` with `sslMode=VERIFY_IDENTITY` plus the Java trust configuration, or use managed external MySQL with verified TLS. Do not weaken TLS to solve connection errors.

## First deployment

The CLI was already installed here as `render v2.28.0`. Its saved token was expired during the audit. Authenticate in your terminal:

```powershell
render login
render workspace set
.\scripts\render.ps1 -Action Check
.\scripts\render.ps1 -Action Validate
```

The official [CLI documentation](https://render.com/docs/cli) describes authentication. Validation calls Render's API; an expired token returns HTTP 401. Local JSON Schema validation does not replace workspace validation.

1. Review and commit the deployment files, then push the tested commit to your GitHub repository. No commit/push was performed by this task.
2. In Render, choose **New > Blueprint** and connect the repository and branch containing `render.yaml`. Leave Root Directory unset; the Docker paths are relative to the repository root.
3. Review the three services, selected region, disks and cost. Create the Blueprint. No Render resources were provisioned by this task.
4. Wait for MySQL, then Java and the frontend. The application retries database connections during initialisation. `/healthz` on the frontend proxies Java's DB-aware health endpoint; a page load alone is not a healthy backend.
5. Open the actual frontend HTTPS URL from the dashboard. Register the first administrator; production defaults to MANUAL membership, so the account starts pending.
6. Open the MySQL service shell, run `mysql -uroot -p foundly`, and enter the root password privately. Promote only the intended registered account:

```sql
UPDATE users SET role = 'ADMIN', membership = 'VERIFIED'
WHERE email = 'replace-with-your-registered-admin-email';
```

Sign in, verify the other test accounts from Moderation > Users, and exercise a complete handover. Replace the demonstration campus/catalog with the actual campus's approved data before launch.

For later application releases, push the tested commit, then:

```powershell
.\scripts\render.ps1 -Action Deploy `
  -BackendServiceId srv-REPLACEBACKEND -FrontendServiceId srv-REPLACEFRONTEND `
  -Commit <full-40-character-pushed-commit-sha>
```

Replace the placeholders with real IDs of the form `srv-...`. The script validates configuration and deploys backend then frontend, waits for each, and stops on failure. It does not create services or redeploy the database. Database image upgrades require a backup and a separately tested procedure.

## Local deployment verification

```powershell
.\scripts\render.ps1 -Action Build
$env:RENDER_SMOKE_DB_PASSWORD = [guid]::NewGuid().ToString('N')
$env:RENDER_SMOKE_ROOT_PASSWORD = [guid]::NewGuid().ToString('N')
$smokeProject = 'foundly-render-audit-' + (Get-Date -Format yyyyMMddHHmmss)
docker compose -p $smokeProject -f deploy/render/compose.smoke.yaml up -d --wait --wait-timeout 180
python scripts/render-smoke.py --project $smokeProject
# This deletes ONLY the disposable smoke project's containers, DB and images volume.
docker compose -p $smokeProject -f deploy/render/compose.smoke.yaml down -v
```

Python 3's standard library is sufficient for the smoke script. Port 18080 must be free. The local client explicitly sends Secure cookies over loopback HTTP to simulate the Render TLS edge. This validates application/proxy/data wiring, not actual browser HTTPS behavior. Never use this test client to send production credentials.

## Production acceptance still required

Address the application defects in [AUDIT.md](AUDIT.md), including login throttling, before launch. Validate actual HTTPS session cookies, CSRF, rate limits and trusted client IP handling at the deployed edge. The template overwrites forwarded scheme/host and discards incoming `Forwarded`; it deliberately does not trust arbitrary caller IP headers. Until trusted edge IP extraction is designed, the Java write limiter can group users behind a proxy IP.

Keep one backend instance: sessions are in memory and uploads are on its disk. Restarts log users out. Render disks prevent horizontal scaling and zero-downtime deployments for their attached services; plan a maintenance window for these MVP releases. See [disk limitations](https://render.com/docs/disks).

Schedule MySQL logical backups using `mysqldump --single-transaction` and copy encrypted backups to independent storage. Back up uploaded images as well, and test restoring both to a separate environment. Render explicitly warns that disk snapshots alone are not a safe MySQL backup strategy. The repository does not yet automate this backup schedule.

Configure failure alerts, disk monitoring, account recovery and the campus's membership/support process. Test mobile/desktop browser journeys, concurrent claim approvals and recovery from backend/database failures. Successful container builds are deployment preparation, not production approval.
