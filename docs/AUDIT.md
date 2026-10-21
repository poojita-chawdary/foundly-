# Repository audit — 21 October 2026

Scope: inspect existing behavior, explain running the app, and add Render deployment scaffolding. No application source, dependency versions, existing migration or existing Compose/Docker files were changed. The pre-existing untracked `backend/package-lock.json` was preserved.

## Result

This is a working database-backed MVP with reproducible defects and production gaps. It is not a frontend awaiting a backend: React calls Spring Boot, which persists reports, claims, matches, users and notifications in MySQL through JPA/Flyway.

| Check | Result |
| --- | --- |
| `backend/mvnw.cmd -B verify` | PASS: 32 tests, zero failures/errors/skips; executable JAR built |
| `frontend/npm ci` | PASS; audit reports six vulnerable packages |
| `npm test` | PASS: 7 tests |
| `npm run build` | PASS: TypeScript and Vite production build |
| New Render Dockerfiles | PASS: all three images built; tests also pass in Linux image builds |
| Real MySQL 8.4 through new Nginx/Java containers | PASS: clean migration and Hibernate schema validation |
| Local production-profile API journey | PASS: pending registration, manual verification, login/CSRF, reports, matches, image upload, private evidence, approval, return |
| Restart persistence | PASS: returned report and image survive Java restart |
| Runtime/config | PASS: JVM PID 1 runs as UID 10001; Nginx syntax valid; Compose configuration valid |
| Blueprint local validation | PASS against Render's current published JSON Schema |
| Render account/Blueprint API validation | BLOCKED: saved CLI token expired; API responds HTTP 401 |
| Live Render deployment and real HTTPS browser acceptance | NOT PERFORMED |

The existing Java tests use H2. The additional container smoke test used actual MySQL with the production profile, MANUAL membership and Secure cookies. Its client explicitly transports cookies over local loopback HTTP to simulate the TLS edge; it is not a substitute for browser testing on the real HTTPS deployment.

## Confirmed defects, left unfixed

### High: login bypasses the custom rate limiter

`backend/src/main/java/campus/foundly/security/AbuseFilter.java:11` registers a servlet filter with no earlier order. Its line 38 sets an authentication limit of 20 requests per minute. `SecurityConfig.java:53` configures Spring Security form login, which handles `/api/auth/login` before that later filter is reached.

Reproduction: 25 failed logins, each with valid CSRF, all returned HTTP 401 within one minute. None returned 429. Reproduced both on the isolated H2 server and through the Render Docker/Nginx/MySQL stack. Brute-force protection promised by this filter does not cover login. An ordered security filter or trusted edge control needs a separate implementation change and tests.

### Medium: editing a report fails once it has over 1,000 matches

`MatchingService.java:37` loads existing matches through the default `Store.list` overload. `Store.java:36` caps that overload at 1,000. The same refresh method scans all opposite reports (`MatchingService.java:51`) and tries to insert any match absent from the truncated list.

Reproduction on MySQL: create one LOST report and 1,001 similar FOUND reports; the first update creates 1,001 match rows. Repeat the same update: HTTP 409 from a duplicate pair unique constraint. The update transaction rolls back. This is a correctness defect beyond the documented limit on listing results, not just a slow large-campus query.

`scripts/render-audit.py --project foundly-render-audit-...` reproduces both defects against the isolated local smoke stack. It intentionally inserts synthetic data, including 1,001 reports. Run it only before disposing of that test project, not against a real deployment.

## Dependency findings

`npm audit` reports **6 affected packages: 2 critical, 3 high, 1 moderate**. The affected set is Vitest, tinypool, Vite, React Router, react-router-dom and @vitest/mocker. These are package-level audit ratings; they do not demonstrate six exploitable production vulnerabilities.

Vite/Vitest/tinypool are development/build tools and are absent from the Nginx runtime. The configured development command binds Vite to `0.0.0.0`, increasing exposure of the known development-server advisories. Use the loopback command in [RENDER.md](RENDER.md) while dependency updates are pending. The [Vite maintainer advisory](https://github.com/vitejs/vite/security/advisories/GHSA-v2wj-q39q-566r) describes the network-exposed dev-server condition; the [Vitest maintainer advisory](https://github.com/vitest-dev/vitest/security/advisories/GHSA-5xrq-8626-4rwp) concerns UI/API server exposure.

Several React Router advisories concern SSR/RSC/server-side features this SPA does not use. The dependency still needs an assessed upgrade; no claim of demonstrated remote code execution in this app is made. No `npm audit fix` or version changes were applied. A complete Java/transitive/container vulnerability scan remains outstanding.

## Existing deployment mismatches

- The original Nginx config hardcodes Compose DNS `backend:8080` and forwards `$scheme`, which becomes internal HTTP behind a TLS edge. It is a local Compose config, not a Render-ready proxy config.
- The browser API client uses relative `/api` URLs and `credentials: same-origin`. Hosting it at a separate origin from the API without a proxy breaks the intended session/CSRF flow. There is no existing credentialed CORS configuration.
- The database migration uses MySQL `AUTO_INCREMENT`, and the runtime has the MySQL driver and Flyway module. Pointing `DB_URL` at managed Render Postgres is not a valid deployment.
- Uploads need a persistent filesystem, and session state is in memory. Ephemeral storage loses uploads; multiple backend replicas would have inconsistent files and login state. Backend restart logs users out.
- Standalone Java does not load `.env` files. Missing `DB_PASSWORD`, an absent database, or an unset production `UPLOAD_DIR` prevents startup. Compose supplies these values.

The new deployment-specific files account for these packaging/configuration requirements while preserving the existing local setup and application behavior.

## Production work still required

1. Correct the confirmed login-throttling and matching defects, assess dependency upgrades, and rerun focused regressions.
2. Reauthenticate Render, validate the Blueprint against the selected workspace, push the reviewed files and provision the paid services. Nothing was committed, pushed or deployed by this task.
3. Configure verified database TLS. The included private MySQL defaults to encrypted `sslMode=REQUIRED`, which does not verify server identity; the runbook explains the production configuration requirement.
4. Schedule independent logical database/image backups and prove a restore. MySQL on Render is self-managed. Disk snapshots alone are not a safe database recovery plan.
5. Verify HTTPS cookie/CSRF behavior and trusted proxy/client-IP handling on Render. The template does not implement distributed abuse controls; proxy IP aggregation can also affect normal write limits.
6. Establish account recovery and campus identity/support procedures. There is no password reset, password change, email delivery or automated campus identity integration. MANUAL membership is available, but needs an administrator and operating process.
7. Add operational alerts, concurrency/load checks and actual desktop/mobile browser acceptance. Retain one backend instance until shared sessions and object storage are implemented.

These are separate follow-up changes. Deployment scaffolding alone does not make the MVP production-ready.

## Local tooling

Render CLI **v2.28.0** was already installed and runnable; installation was unnecessary. `render workspaces -o json` reported an expired token and `render blueprints validate render.yaml -o text` returned 401. Run `render login` interactively, then validation. Render advertised v2.30.0; no upgrade was required or performed.

Java 21.0.12.1 and Node 24.16.0 were available. Docker was installed but stopped; starting Docker Desktop made Engine 29.9.1 available for the container checks. The disposable audit containers and their synthetic test volumes are removed after verification; built images remain available for reuse.
