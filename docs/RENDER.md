# Foundly split deployment

The production topology is:

```text
Browser -> Vercel (React SPA; same-origin /api rewrite)
                    -> Render (Spring Boot API)
                         -> Supabase Postgres
                         -> Supabase Storage (private report images)
```

Vercel's request logs show incoming CDN requests, including `/api/reports/{id}/image`, even when those requests are rewritten upstream. That path does not mean Vercel stores the image. The Render API authenticates the session, checks campus/report access, then fetches the PNG from the private Supabase bucket. The browser must keep using relative `/api` URLs; do not expose the Supabase service-role key or configure a public bucket.

## Local development

The repo-root Compose setup remains the simplest local path and uses MySQL plus local filesystem image storage:

```powershell
Copy-Item .env.example .env  # only for a fresh clone; keep credentials private
docker compose up --build
```

Open http://localhost:8080. Stop with `docker compose down`; avoid `-v` unless you intend to erase the local DB and uploads. For standalone development, start MySQL 8, run `backend\mvnw.cmd spring-boot:run` with `DB_URL`, `DB_USER`, `DB_PASSWORD`, then run `npm ci` and `npm run dev` from `frontend`. The Vite dev server proxies `/api` to port 8080.

## Hosted configuration

The current Render backend is the existing `foundly-` web service (`srv-dbcc3im1egvs73a6fgg0`) and uses `deploy/render/backend.Dockerfile`. `render.yaml` describes a clean backend service for reference/new environments; `render blueprints validate ./render.yaml` reports one service creation, so do not apply it to the current workspace unless you intentionally want a second service. Set these on the existing Render backend only:

| Variable | Value / purpose |
| --- | --- |
| `SPRING_PROFILES_ACTIVE` | `supabase,prod` |
| `SUPABASE_DATABASE_URL` | Supabase Postgres session-pooler URL, port 5432, `sslmode=verify-full` |
| `SUPABASE_URL` | `https://<project-ref>.supabase.co` |
| `SUPABASE_SERVICE_ROLE_KEY` | Secret Supabase service-role key; backend only |
| `SUPABASE_STORAGE_BUCKET` | `foundly-images` |
| `MEMBERSHIP_MODE` | `MANUAL` for production membership checks |
| `COOKIE_SECURE` | `true` |

The API database URL is still required: moving the database to Supabase changes the connection target, not the need for backend DB credentials. Remove old MySQL-specific `DB_HOST`, `DB_USER`, `DB_PASSWORD`, and `DB_SSL_MODE` variables from Render once the Supabase connection is confirmed. Never place database credentials or the service-role key in Vercel variables or frontend build-time configuration.

The linked Supabase project is `foundly` (`afnbdrltoxyilkvxrgny`, Singapore). Its private `foundly-images` bucket has a 5 MiB limit and accepts PNG only. The bucket migration is in `supabase/migrations`; review and apply future migrations with:

```powershell
npx supabase login
npx supabase link --project-ref afnbdrltoxyilkvxrgny
npx supabase db push --linked
```

Render currently serves the API at `https://foundly-xoro.onrender.com`; Vercel's [frontend configuration](../frontend/vercel.json) rewrites `/api/*` to that origin. The current Vercel project has its Root Directory set to `frontend`. Keep that setting, and keep `vercel.json` in that directory. The React app does not need a `VITE_API_URL` because it calls relative paths.

## Releasing

1. Confirm both Supabase secrets are set on Render and the bucket migration is applied.
2. Build and verify the backend and frontend locally.
3. Commit and push the code to the configured GitHub branch. The current Render service has auto-deploy disabled, so trigger a deploy from Render after the tested commit is pushed. Vercel deploys the `frontend` root from its connected Git repository.
4. Wait for Render `/actuator/health` to report healthy, then check Vercel's latest deployment.
5. Test login, CSRF-protected writes, report list and image upload/read in a signed-in browser. Vercel `/api/*` request log entries are expected; check status codes and Render logs for failures.

The current Render service is a single free web instance, so it may sleep when idle and its in-memory login sessions are lost on restart. Supabase is the durable image and relational-data store; no Render image disk is needed. Production readiness still requires exercising HTTPS cookies, rate limits behind the proxy, membership verification, backups/restores, monitoring, and operational recovery. The existing six reports have image keys in Postgres, but their original bytes are not present in the new Storage bucket; recover/re-upload those separately if the original files are still available.
