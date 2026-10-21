# Verification report

Verified on 20 October 2026. No external deployment was performed.

## Successful checks

| Check | Actual result |
| --- | --- |
| Java backend compilation and executable JAR packaging | Maven `verify`: BUILD SUCCESS, Java 21.0.12.1 |
| Backend automated tests | 32 passed, 0 failures, 0 errors, 0 skipped |
| React/TypeScript production build | `npm run build`: passed; Vite built production assets |
| Frontend interaction tests | `npm test`: 7 passed |
| Flyway migration and Hibernate schema validation | Executed successfully by the Spring integration tests against H2 in MySQL mode |

### Backend suites

- `campus.foundly.BoundaryIntegrationTest`: 9 passed.
- `campus.foundly.JourneyIntegrationTest`: 14 passed.
- `campus.foundly.MatchScorerTest`: 9 passed.

The tests exercise registration, hashing, session login/logout, CSRF rejection, report validation/ownership, search filters, match scoring and exclusion, duplicate prevention, claim privacy and reviewer permissions, conflicting claims, return transitions, database role checks, notifications, image handling, cross-campus isolation, manual membership, hiding, withdrawal, and close/audit behaviour.

The integration journey uses Spring Boot, MockMvc, real JPA persistence, the Flyway schema, and H2. It is not a mocked backend. Frontend tests use mocked network responses to check UI behaviour separately; they do not prove a live browser-to-MySQL deployment.

### Frontend cases

1. Home renders actual catalog responses and a signed-out noticeboard state.
2. The dashboard routes signed-out visitors to login.
3. Authentication errors appear and allow retry.
4. Safety navigation opens the guide.
5. Initial loading feedback is shown.
6. Switching between claims and reports does not render stale data.
7. The report form posts its fields to the API and navigates to the saved item.

## Environment details

The supplied environment initially had Java 17 and no Maven. A local Microsoft JDK 21 and Maven 3.9.9 were downloaded for verification. The final backend verification used Java 21 with the platform's trusted CA store and network proxy settings. These environment-only settings are not embedded in application source. Initial dependency/tooling failures were resolved; the final passing logs are included in `docs/evidence/`.

Mockito's non-attaching mock maker is configured in test resources for restricted environments. Tests do not require a paid service or email credentials.

## Verification limits

- No MySQL server or Docker daemon was available, so the Compose stack, real MySQL collation/locking behaviour, and persistent-volume restart behaviour were not executed here.
- No supported browser-control capability was available for this workflow. No visual browser review, mobile device test, screenshot, or full live HTTP/browser journey is claimed.
- No concurrency/load test, vulnerability audit, penetration test, production TLS/proxy check, or provider deployment was performed.
- The next acceptance step is to run Compose locally, walk through the two-account journey in a browser, then validate MySQL, HTTPS cookies, permissions, and backup/restore before deployment.

See `LIMITATIONS.md` for unfinished product features and scaling constraints.
