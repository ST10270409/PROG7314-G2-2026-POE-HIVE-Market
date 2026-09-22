# Deploying HiveMarket API to Render — tonight's checklist

This matches the Android app's existing `HiveMarketApi.kt` contract exactly, field for field. No changes needed on the Android side except pointing `BASE_URL` at the real URL once it's live.

## 1. Push this folder to GitHub

This can be its own repository, or a folder inside the group's existing HiveMarket repo (e.g. `HiveMarket/api/`) — either works for Render.

```bash
cd HiveMarketApi
git init
git add .
git commit -m "HiveMarket REST API — ASP.NET Core, matches Android client contract"
git branch -M main
git remote add origin <your repo URL>
git push -u origin main
```

## 2. Create the database first — Render dashboard → New → PostgreSQL

- Name: `hivemarket-db` (or anything)
- Plan: **Free**
- Region: pick anything close to you
- Click **Create Database**
- Wait for it to finish provisioning (~1 minute)
- Once ready, copy the **Internal Database URL** shown on its page — you'll need it in step 4. It looks like `postgresql://user:password@host/dbname`.

## 3. Create the web service — Render dashboard → New → Web Service

- Connect the GitHub repo from step 1
- **Runtime**: Docker (Render should auto-detect the `Dockerfile`)
- **Root Directory**: leave blank if this is its own repo; set to `api` (or wherever you put this folder) if it's inside the group's existing repo
- Plan: **Free**

## 4. Set environment variables — on the web service's page, "Environment" tab

Add these two:

| Key | Value |
|---|---|
| `DATABASE_URL` | Paste the **Internal Database URL** from step 2 |
| `FIREBASE_PROJECT_ID` | `hivemarket-prototype` (only change this if your group's real Firebase project has a different ID — check `google-services.json`'s `"project_id"` field to confirm) |

## 5. Deploy and verify it's actually alive

Render will build and deploy automatically after step 4 is saved. This takes a few minutes the first time (Docker build + Postgres connection).

**Once it says "Live"**, open the service's URL directly in a browser (something like `https://hivemarket-api-xxxx.onrender.com`). You should see:
```json
{"status":"HiveMarket API is running"}
```
If you see this, the deployment itself worked — the database connected and the app started. If you see an error page instead, check the **Logs** tab on the Render dashboard; the most likely cause is `DATABASE_URL` being wrong or missing.

## 6. Point the Android app at it

In `NetworkModule.kt`, find `BASE_URL` and change it from the placeholder to your real Render URL (with a trailing slash):

```kotlin
private const val BASE_URL = "https://hivemarket-api-xxxx.onrender.com/"
```

Rebuild the app. Login, then check logcat — you should see real `200`/`201` responses instead of `404` on every `okhttp.OkHttpClient` line.

## 7. Known limitation worth knowing before you demo

**Render's free tier spins the service down after ~15 minutes of no traffic**, and the first request after that takes 30-60 seconds to wake it back up (it'll look like a hang, not an error). **Before recording your demo video, open the app and make one API call first** (e.g. just load Browse) to wake the service up, wait for it to respond, *then* start recording. Otherwise your video will have an awkward 45-second pause on the very first network call, which looks like something is broken.

## If something goes wrong and you're out of time

The Android app is already built to degrade gracefully when the API isn't reachable (that's the whole offline-first design) — Browse falls back to cache, Settings falls back to local storage, and none of it crashes. If Render deployment genuinely doesn't work tonight, the app still runs and demos Login/Settings/UI navigation fine; you'd only lose the "successful data round-trip" and "data visible in the database" shots in the demo video, not the whole app. Don't let a deployment problem block finishing everything else.
