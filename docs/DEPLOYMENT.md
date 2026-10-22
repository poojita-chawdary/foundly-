# Deployment

## Hosted design

```text
Browser -> Vercel frontend -> /api/* rewrite -> Render Spring API
                                                -> Supabase Postgres
                                                -> Supabase private Storage
```

Vercel's CDN logs will show `/api/reports/{id}/image` because the browser requests that URL from the Vercel origin. Vercel rewrites it to Render; the image bytes should be fetched by the backend from the private `foundly-images` Supabase Storage bucket. This preserves same-origin session cookies and CSRF handling. Never expose the Supabase service-role key or point frontend code directly at the bucket.

## Render environment

Configure the backend service with:

| Variable | Purpose |
| --- | --- |
| `SPRING_PROFILES_ACTIVE=supabase,prod` | Hosted database/storage profile and secure cookies |
| `SUPABASE_DATABASE_URL` | Postgres session-pooler URL, port 5432 and `sslmode=verify-full` |
| `SUPABASE_URL` | Project URL, e.g. `https://<project-ref>.supabase.co` |
| `SUPABASE_SERVICE_ROLE_KEY` | Private backend-only Storage access |
| `SUPABASE_STORAGE_BUCKET=foundly-images` | Private PNG bucket |
| `MEMBERSHIP_MODE=MANUAL` | Production membership workflow |
| `COOKIE_SECURE=true` | HTTPS-only session cookie |

The Supabase database URL remains required: the app's backend must authenticate to its database. Remove the old MySQL-specific `DB_HOST`, `DB_USER`, `DB_PASSWORD`, and `DB_SSL_MODE` from Render after confirming the Supabase connection. No database or storage secrets belong on Vercel. Render CLI v2.28 has no environment-variable subcommand; use the service's Environment settings for secrets.

## Supabase CLI

The linked project is `foundly` (`afnbdrltoxyilkvxrgny`, Singapore). The private image bucket is provisioned by `supabase/migrations/20261022090000_foundly_private_images_bucket.sql`:

```powershell
npx supabase login
npx supabase link --project-ref afnbdrltoxyilkvxrgny
npx supabase db push --linked
```

The migration is already applied. It restricts objects to PNG and 5 MiB. Existing Postgres report rows currently reference six image keys, but the bucket has zero objects. The repository contains no source copies; recover those images from their original files or a surviving Render copy and re-upload them. The logs alone contain request paths, not image payloads.

## Vercel

The Vercel project Root Directory is `frontend`; `frontend/vercel.json` configures Vite and rewrites `/api/*` to `https://foundly-xoro.onrender.com/api/*`. The client uses relative `/api` calls and needs no `VITE_API_URL`. Keep frontend and API same-origin from the browser's perspective, don't add wildcard CORS, and ensure API responses remain `no-store`.

## Release checks

1. Confirm Supabase database URL and service-role key are configured privately on Render.
2. Apply reviewed database/storage migrations using the Supabase CLI.
3. Run backend verification and frontend tests/build; push the reviewed commit.
4. Deploy the pushed backend commit to the existing Render service (auto-deploy is currently off); Vercel deploys from its connected GitHub project.
5. Check Render `/actuator/health`, then exercise login, CSRF, report listing, image upload/read, and logout in a signed-in browser. Vercel `/api/*` requests are expected; inspect their status and Render application logs for failures.

This is not a production sign-off. The current Render backend uses one free instance, so idle sleep and in-memory session loss on restart are expected. Before a real launch, validate secure cookies and proxy behavior, abuse controls, campus verification, backup/restore of Postgres and Storage, alerts, and account recovery. Local Compose development still uses MySQL and local image storage; see [RENDER.md](RENDER.md) for local run steps and the split-hosting details.
