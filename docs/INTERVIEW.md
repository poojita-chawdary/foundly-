# Beginner walkthrough and interview practice

## Follow one report

1. Start in `frontend/src/pages/ReportForm.tsx`. React collects form values and calls the typed client in `api.ts`.
2. The client sends JSON and a CSRF header. The session cookie travels automatically on the same origin.
3. `ReportController` accepts the request. Bean Validation rejects missing or invalid fields.
4. `ReportService` looks up the signed-in database user, checks campus and catalog permissions, saves the report, and asks `MatchingService` to refresh suggestions.
5. `Store` talks to the database through JPA’s EntityManager. Flyway created the schema beforehand.
6. The service returns `ReportDto`, which explicitly includes safe report fields. It does not return the entity or any email/password/claim secret.
7. React navigates to the saved report. If a photo was selected, it is uploaded separately. A photo failure does not throw away the saved report.

A database transaction is like completing one whole notebook entry before closing the book. Approval, conflicting-claim rejection, notifications, and audit records belong together. If any database step fails, none of those changes should be committed.

## Questions to practise

**Why keep business rules in Java instead of React?**
A user can bypass React and call an API directly. Java checks permissions and workflow transitions regardless of the client. React’s job is to guide people and display results.

**Why sessions rather than JWTs for this application?**
One browser origin and one backend instance make server sessions easy to understand and revoke on logout. HttpOnly cookies avoid putting bearer tokens in browser storage. Cookies require a deliberate CSRF defence. Multiple backend instances would need shared sessions.

**What does CSRF protection do?**
A browser may automatically send login cookies with a request. Requiring a session-tied token on changes helps the server reject requests initiated without the application’s cooperation. Spring’s CSRF documentation explains token retrieval and refresh after login/logout: https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html

**How do you prevent someone claiming their own found report?**
`ClaimService.submit` compares the authenticated user ID with the report owner ID. It also requires a same-campus ACTIVE FOUND report and stores the claimant from the authenticated identity rather than accepting it from the request.

**How do you prevent two approved owners?**
Approval obtains a database write lock on the report inside a transaction. Only an ACTIVE report can be approved. The winner changes it to CLAIMED, and other pending claims become REJECTED. A later approval sees the changed state and fails. A real database concurrency test remains an important follow-up.

**How do private answers stay private?**
The verification text is stored separately in `claim_secrets`. Report DTOs and claim summaries never include it. A dedicated evidence endpoint checks finder/moderator permissions before reading it. The claim input is not a public report field.

**What is the matching score?**
An explainable weighted comparison of category, word overlap, colour, location, and date. The default weights add to 100, and custom weights are normalised. The score is not a probability and cannot prove ownership.

**What prevents duplicate suggestions?**
The service updates existing pairs, and the database has a unique constraint on `(lost_report_id, found_report_id)`. Campus write serialization reduces concurrent upsert conflicts.

**Why Flyway instead of Hibernate auto-update?**
Versioned SQL records what changed and can be reviewed and reproduced. Hibernate validates that entity mappings match the resulting schema.

**How do image uploads avoid trusting filenames?**
The server ignores client filenames, checks allowed content type and actual decoded format, limits size and dimensions, generates a UUID, re-encodes the image, and stores it in a configured directory. Read requests check report visibility.

**Why are the tests not proof of production security?**
Tests check specific boundaries and behaviours. They do not test every input, production proxy settings, real MySQL behaviour, browser rendering, or infrastructure. Explain the verification limits honestly.

**What would you improve next?**
Start with real MySQL/browser acceptance tests, concurrent approval coverage, and account recovery. Then improve typed domain models, repository organisation, private coordination, persistence/scaling, and campus identity integration.
