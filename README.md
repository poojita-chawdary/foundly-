# Foundly — Campus Lost & Found

**Lost something? Let’s find it.**

A working student portfolio MVP: React + TypeScript on the frontend, Java 21 + Spring Boot 3.5 on the backend, and MySQL 8 with Flyway migrations. All item reports, claims, notifications, suggestions, and moderation statistics are database-backed. The application ships without invented reports or default user passwords.

## Start here

For the current repository audit and Render setup, see [docs/AUDIT.md](docs/AUDIT.md) and [docs/RENDER.md](docs/RENDER.md). The Render files prepare deployment; the audit lists application defects still requiring separate fixes.

The easiest local setup uses Docker Desktop or Docker Engine with Compose:

```sh
cp .env.example .env
# Edit .env and replace both password placeholders with your own values.
docker compose up --build
```

Open **http://localhost:8080**. Register two accounts using emails you control (no email is sent). In the default local OPEN membership mode, accounts can sign in immediately. Use one account to report a found item, and the other to report a similar lost item and submit a claim. In the finder’s dashboard, review the private evidence, approve the claim with a campus help-desk handover plan, and confirm the return after handing over the item.

The database has one **Demo Campus**, six categories, and five campus locations. It has **no demo users, passwords, or item reports**. Data survives container restarts through named database and image volumes. `docker compose down` keeps them; `docker compose down -v` deletes them.

## Run without containers

Prerequisites: **JDK 21**, **Node 22.12+** (Node 24 also supported), and **MySQL 8.x**. The Maven wrapper downloads Maven 3.9.9 on first use. Windows users can use `mvnw.cmd`.

Create an empty database named `foundly` and a dedicated MySQL account with privileges on that database. Set the following environment variables in the backend shell:

```sh
export DB_URL='jdbc:mysql://localhost:3306/foundly'
export DB_USER='foundly'
export DB_PASSWORD='your-own-local-password'
cd backend
./mvnw spring-boot:run
```

In a second shell:

```sh
cd frontend
npm ci
npm run dev
```

Open **http://localhost:5173**. Vite proxies `/api` to Java at port 8080, so browser requests stay same-origin. The root `.env` file is consumed by Compose; a standalone Java process needs exported environment variables. Flyway creates the tables and reference data at startup; Hibernate validates, rather than modifies, the schema.

## Tests and builds

```sh
cd backend
./mvnw verify
```

The test profile uses isolated H2 in MySQL compatibility mode and executes the actual Flyway migration. It does **not** require a running MySQL server. These tests are not a substitute for validating MySQL-specific behaviour before deployment.

```sh
cd frontend
npm ci
npm test
npm run build
```

See [docs/VERIFICATION.md](docs/VERIFICATION.md) for the results actually obtained in the build environment.

## Project structure

- `frontend/src/pages/`: home, authentication, browse, report form, item details, dashboard, moderation, and guides.
- `frontend/src/components/`: navigation, report cards, category icons, loading and error states.
- `frontend/src/api.ts`: typed API client and CSRF handling. No tokens are stored in localStorage.
- `backend/src/main/java/campus/foundly/`: domain entities, security, authentication, catalog, reports, matching, claims, notifications, moderation, and shared persistence/error helpers.
- `backend/src/main/resources/db/migration/`: versioned database schema and initial reference data.
- `backend/src/test/`: matching rule tests and a Spring/MockMvc integration journey.
- `compose.yml`, Dockerfiles, and `frontend/nginx.conf`: local packaging and same-origin reverse proxy.
- `docs/`: API reference, architecture/ER diagrams, deployment guidance, limitations, and interview notes.

## Features implemented

- Registration, BCrypt password hashing, session login/logout, current-user retrieval, and configurable OPEN/MANUAL campus membership.
- Campus isolation, report ownership checks, and database-backed USER/MODERATOR/ADMIN permissions.
- Lost and found reporting, edits, closure, optional image previews/uploads, safe server filenames, image decoding and metadata removal.
- Backend keyword/type/category/location/date filters, newest-first sorting, and pagination. Closed, claimed, returned, and hidden reports do not appear in active browsing.
- Weighted Java matching with stored unique pairs, explanations, notifications, refresh on report changes, and per-user dismissal.
- Private claim evidence in a separate table; duplicate claims blocked; approval/rejection/withdrawal; conflicting claims rejected; private handover plan; authorised return confirmation.
- Personal reports, matches, submitted/reviewable claims, persistent notifications, and mark-as-read actions.
- Listing flags, moderation hiding, flag review, account verification/suspension/restoration, catalog creation/rename/deactivation, real campus statistics, and action audits.
- Responsive warm cream/teal interface, labelled forms, keyboard focus, feedback, empty states, mobile navigation, Help, About, Safety, and Contact pages.

## Staff setup and membership

There is deliberately no public role-selection endpoint or default administrator. A trusted database operator may promote an account **after that person registers**:

```sql
UPDATE users SET role = 'ADMIN', membership = 'VERIFIED'
WHERE email = 'replace-with-your-registered-admin-email';
```

Use `role = 'MODERATOR'` for moderators. These are out-of-band operator actions; normal actions in the app are audited. Sign in again after changing a role. Do not grant database access to ordinary users.

- `MEMBERSHIP_MODE=OPEN`: local/demo self-registration; it does not verify a real campus identity.
- `MEMBERSHIP_MODE=MANUAL`: registration creates a pending account. An administrator verifies membership from Moderation → Users. Set up the first administrator through the database, using the command above.

Manual verification is a human administrative check, not an email-domain or automated identity check. No particular college email domain is assumed. The production profile defaults to MANUAL.

## Security model

The browser and API use one origin. Spring Security keeps the login session in an HttpOnly, SameSite=Lax cookie. The client retrieves a CSRF token from `/api/auth/csrf`, sends it on changes, and refreshes it after login/logout. Production enables Secure cookies. No permissive cross-origin configuration is enabled.

Every service action checks the current database user’s membership/suspension state; account suspension therefore also blocks existing sessions. Role and ownership checks happen on the backend. Public report DTOs never include emails, password hashes, or private claim evidence. Claim summaries omit evidence even for the claimant. The evidence endpoint is only for authorised reviewers on the same campus.

Passwords must be 12–72 characters and no more than 72 UTF-8 bytes because BCrypt truncates long byte sequences. Uploads are PNG/JPEG only, up to 5 MB and 12 megapixels, decoded and re-encoded as PNG under a generated UUID. Files live outside the database and are served through a permission-checked API. Static Nginx applies a content security policy. Users still need to avoid posting sensitive content in free text or photos.

Transactions and pessimistic report locks protect claim approval; a campus lock serialises report changes and matching upserts. Unique constraints provide an additional duplicate safeguard. Rate limits cover writes and authentication per IP per application instance; production needs proxy-level distributed abuse controls.

## Read next

- [Architecture and database diagrams](docs/ARCHITECTURE.md)
- [API reference and permissions](docs/API.md)
- [Deployment guide](docs/DEPLOYMENT.md)
- [Limitations and next development steps](docs/LIMITATIONS.md)
- [Interview questions and beginner walkthrough](docs/INTERVIEW.md)

This is an internship-oriented MVP, not a claim of audited production security. No external deployment has been performed.
