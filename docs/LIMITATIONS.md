# Current limits and next steps

This is a coherent local MVP, with the main register → report → search/match → claim → return journey implemented. It is not a finished campus production service.

## What remains

- No external deployment, browser screenshots, browser layout verification, or live MySQL verification was performed in this environment. Review the verification report for the exact checks that ran.
- Campus membership supports OPEN demo mode and MANUAL administrative verification. There is no automated campus identity provider, email verification, password reset, password change, or account recovery flow.
- Email delivery is not implemented. Persistent in-app notifications work without email credentials.
- Safe coordination is a reviewer-provided private handover plan. There is no private chat, claimant counterproposal, calendar scheduling, two-party return acknowledgement, or dispute/reopen workflow.
- A report/claimant pair can have only one claim ever. Rejected/withdrawn claims cannot be resubmitted. Reopening reports and reversing mistaken approvals require future reviewed workflows.
- The found report is marked RETURNED after authorised handover. Related LOST reports are not automatically marked RETURNED; their owners can close them separately. A previous match is not proof they refer to the same object.
- Abuse reporting targets listings. Moderators can review their owners’ accounts; there is no separate user-report, appeal, or sophisticated suspicious-activity detection workflow. Returned records are retained and cannot be hidden using the current moderation action.
- Categories are global; locations and users are campus-scoped. The first version seeds one demonstration campus. There is no UI to create campuses or assign staff to several campuses.
- Catalog deactivation/rename is implemented, but the UI lists active catalog entries only and does not offer reactivation of inactive entries. The authenticated administrator API can reactivate a known ID.
- Matching is synchronous and scans active opposite-type reports for the campus. A campus lock is intentionally simple and limits write throughput. Catalog and match responses currently cap at 1000 records; most other lists have explicit pagination.
- Image storage is a persistent local directory. There is no object-storage adapter, antivirus service, image deletion endpoint, content moderation, or retention/cleanup job. Image re-encoding removes metadata but cannot remove visible private details from the pixels.
- Sessions and rate-limit windows are in memory on one backend instance. Restarting the backend logs people out. The edge must enforce production rate limiting. No horizontal deployment is supported yet.
- Domain workflow values currently use validated strings and SQL constraints rather than Java enums. Entity fields are kept accessible to reduce introductory boilerplate. A useful next refactor is typed enums, encapsulated entity transition methods, dedicated Spring Data repositories, and explicit pagination for every list.
- No compliance certification, penetration test, dependency vulnerability audit, load test, encryption of claim evidence at application level, retention policy, or privacy export/deletion workflow is claimed. Restrict database access, use encrypted storage/backups, and define institutional policies before storing real identities.

## Suggested development order

1. Run the MySQL and HTTPS acceptance checklist, and visually review mobile/desktop journeys in a real browser.
2. Add concurrent approval and MySQL integration tests using Testcontainers in CI, plus upload, cross-campus, and moderation coverage.
3. Improve account recovery and integrate a real campus verification process.
4. Add two-party return acknowledgement, disputes, approval cancellation, and safe reopening.
5. Refactor enums and repositories as the learner becomes comfortable with the initial code.
6. Add persistent/shared sessions, object storage, queued matching, distributed limits, and a retention/appeals policy when scaling becomes necessary.
