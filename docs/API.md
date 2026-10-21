# API reference

Requests and responses use JSON unless specified. Error responses use `{ "message": "Helpful explanation" }`; status codes distinguish invalid input (400), missing authentication (401), forbidden access/CSRF (403), missing resources (404), conflicts (409), and rate limits (429). Unexpected server errors return a generic 500 message.

All endpoints except catalog, CSRF, registration, login, and health require an authenticated, verified, non-suspended account. Report and claim access is scoped to the user’s campus. All POST/PUT requests require the CSRF header. The backend does not permit cross-origin browser access by default.

## Authentication

| Method | Path                 | Body / result                                                          |
| ------ | -------------------- | ---------------------------------------------------------------------- |
| GET    | `/api/auth/csrf`     | `{token, headerName}`; retain in memory                                |
| POST   | `/api/auth/register` | JSON `{name,email,password,campusId}` → membership and message         |
| POST   | `/api/auth/login`    | **Form-urlencoded** `email=…&password=…` → session cookie              |
| POST   | `/api/auth/logout`   | Ends session; returns 204                                              |
| GET    | `/api/users/me`      | Own ID, name, role, campus, and membership; no password/contact fields |
| GET    | `/api/catalog`       | Active campuses, categories, locations; no private records             |
| GET    | `/actuator/health`   | Health summary                                                         |

Call the CSRF endpoint before the first write and again after login/logout. The token’s returned `headerName` is usually `X-CSRF-TOKEN`.

## Reports

| Method | Path                      | Permission / behaviour                                  |
| ------ | ------------------------- | ------------------------------------------------------- |
| GET    | `/api/reports`            | Campus active records; filters below                    |
| POST   | `/api/reports`            | Verified member creates a report                        |
| GET    | `/api/reports/{id}`       | Same campus; hidden items visible only to owner/staff   |
| PUT    | `/api/reports/{id}`       | Owner of an ACTIVE report; type cannot change           |
| POST   | `/api/reports/{id}/close` | Owner of an ACTIVE report                               |
| POST   | `/api/reports/{id}/image` | Owner of ACTIVE report; multipart field `file`          |
| GET    | `/api/reports/{id}/image` | Same permissions as item detail; PNG response, no-store |
| POST   | `/api/reports/{id}/flag`  | Same-campus member; JSON `{reason}` (5–1000 characters) |

Create/edit body:

```json
{
  "type": "FOUND",
  "title": "Blue water bottle",
  "categoryId": 6,
  "colour": "Blue",
  "description": "An insulated blue bottle near the library entrance.",
  "locationId": 1,
  "eventDate": "2026-10-20"
}
```

Use IDs from the catalog and a date that is not in the future. Title is required (max 120); description is 10–2000 characters; colour is optional (max 50). The service checks active category/location and campus membership. Image upload is a separate request after the report exists. If it fails, the report remains saved and the UI links to its edit flow.

Search query parameters: `q`, `type=LOST|FOUND`, `categoryId`, `locationId`, `from=YYYY-MM-DD`, `to=YYYY-MM-DD`, `page=0`, `size=12` (max 50), `mine=false`. `mine=true` returns only the user’s reports, including non-active statuses. Sort is always creation time descending, then ID descending. Response: `{content: ReportDto[], total, page, size}`. Search `%`, `_`, and `!` characters are escaped so they cannot expand the requested search unexpectedly.

## Matches and claims

| Method | Path                        | Permission / behaviour                                                            |
| ------ | --------------------------- | --------------------------------------------------------------------------------- |
| GET    | `/api/matches`              | Suggestions for own reports; optional `reportId` shows related campus suggestions |
| POST   | `/api/matches/{id}/dismiss` | Owner of either matched report; dismissal is personal                             |
| POST   | `/api/claims/report/{id}`   | Non-owner claiming an active FOUND report; JSON `{verification}`                  |
| GET    | `/api/claims`               | Own submitted claims; `page=0`, 30 per page                                       |
| GET    | `/api/claims?review=true`   | Finder’s claims, or all assigned-campus claims for moderators; 30 per page        |
| GET    | `/api/claims/{id}/evidence` | Finder or same-campus staff only; returns `{verification}`                        |
| POST   | `/api/claims/{id}/decision` | Reviewer approves/rejects; claimant can withdraw a pending claim                  |
| POST   | `/api/claims/{id}/returned` | Finder only; approved claim and CLAIMED report required                           |

Private verification text must be 10–2000 characters. A claimant cannot claim their own report, access another claim’s evidence, or approve their own claim even if they hold a moderator role. Report/claimant uniqueness applies for the lifetime of a claim.

Decision body: `{ "status": "APPROVED", "handoverMessage": "Campus help desk, Friday at 4pm" }`. Allowed decisions are APPROVED, REJECTED, WITHDRAWN. Approval requires a handover plan of 10–255 characters. Keep contact information out of this plan; use a public place and time. Evidence never appears in general claim responses. The summary has separate `canReview` and `canReturn` flags for appropriate UI actions.

## Notifications

| Method | Path                           | Behaviour                                    |
| ------ | ------------------------------ | -------------------------------------------- |
| GET    | `/api/notifications?page=0`    | Own notifications; newest first, 30 per page |
| POST   | `/api/notifications/{id}/read` | Recipient only                               |

## Moderation

All routes require a database role of MODERATOR or ADMIN unless marked ADMIN only.

| Method | Path                                 | Body / behaviour                                 |
| ------ | ------------------------------------ | ------------------------------------------------ |
| GET    | `/api/admin/stats`                   | Real aggregate counts in assigned campus         |
| GET    | `/api/admin/flags?page=0`            | Campus flags, 30 per page                        |
| GET    | `/api/admin/users?page=0`            | Campus account summaries, 30 per page            |
| GET    | `/api/admin/audit?page=0`            | Campus staff/user action trail, 30 per page      |
| POST   | `/api/admin/reports/{id}/hide`       | `{reason}`; cannot hide already returned records |
| POST   | `/api/admin/flags/{id}/review`       | `{reason}`                                       |
| POST   | `/api/admin/users/{id}/suspend`      | ADMIN only; `{reason}`                           |
| POST   | `/api/admin/users/{id}/restore`      | ADMIN only; `{reason}`                           |
| POST   | `/api/admin/users/{id}/verify`       | ADMIN only; `{reason}`                           |
| POST   | `/api/admin/catalog/categories`      | ADMIN only; `{name,active}`                      |
| PUT    | `/api/admin/catalog/categories/{id}` | ADMIN only; rename/deactivate                    |
| POST   | `/api/admin/catalog/locations`       | ADMIN only; new assigned-campus location         |
| PUT    | `/api/admin/catalog/locations/{id}`  | ADMIN only; rename/deactivate                    |

Reasons must contain 5–1000 characters. Administrators cannot suspend themselves or another administrator through these endpoints. Categories are global; locations belong to a campus. Deactivation preserves references on older reports. There is no hard-delete endpoint, role-change endpoint, or public administrator registration.

## Limits

Authentication writes: 20/minute per IP. Other API writes: 90/minute per IP. These are in-memory, per-instance limits, and the app does not trust caller-supplied forwarded IP headers. Configure stronger trusted-proxy limits for production. Most list endpoints are paginated; catalog/match queries currently have a 1000-record ceiling and need explicit pagination for large installations.
