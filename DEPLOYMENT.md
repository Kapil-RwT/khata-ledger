# Deployment Guide: Render + Vercel (Free Tier)

Goal: get the Khata Ledger Service live on the internet for free, with a stable URL you can put on your resume.

- **Backend + Postgres** on [Render](https://render.com)
- **Frontend** on [Vercel](https://vercel.com)
- **Total cost**: $0/month
- **Time to deploy**: ~45 minutes the first time

Cold-start tradeoff: the free Render web service sleeps after 15 minutes of inactivity. The first request after a quiet period takes 30-60 seconds to wake up. Warm up the URL by hitting it once before any interview demo.

---

## Prerequisites

- A [GitHub](https://github.com) account
- A [Render](https://render.com) account (sign up with GitHub)
- A [Vercel](https://vercel.com) account (sign up with GitHub)
- Optional: a [Google AI Studio](https://aistudio.google.com/apikey) Gemini API key (you already have one from the Legal Contract Analyzer project per your resume)

---

## Step 0: Prerequisite — rotate any leaked API key

If at any point you pasted your real Gemini API key into a source file, **rotate it before pushing to GitHub**:

1. Go to https://aistudio.google.com/apikey
2. Delete the old key
3. Generate a new key — keep it in your password manager only

The project is now configured so the key never sits in source. It comes from the `GEMINI_API_KEY` env var on Render (Step 3 below).

## Production-readiness code changes — already done

These two changes have been applied to the codebase already; no manual edits needed at deploy time:

- **`backend/src/main/java/com/khataledger/config/SecurityConfig.java`** reads `ALLOWED_ORIGINS` from env (comma-separated list) with `http://localhost:*` as the local default.
- **`backend/src/main/resources/application.yml`** sets `server.port: ${PORT:8080}` so Render can inject its own port. `GEMINI_API_KEY` is also env-driven.
- **`frontend/src/api/client.js`** reads `VITE_API_BASE_URL` from the Vite build environment; falls back to `/api` (with proxy) for local dev.

---

## Step 1: Push to GitHub

From your `khata-ledger` folder:

```bash
cd ~/Documents/Claude/khata-ledger
git init
git add .
git commit -m "Initial commit: Khata Ledger Service"
```

On GitHub.com, create a new public repository named `khata-ledger`. Then:

```bash
git branch -M main
git remote add origin https://github.com/<your-username>/khata-ledger.git
git push -u origin main
```

---

## Step 2: Provision Postgres on Render

1. Go to https://dashboard.render.com -> **New +** -> **PostgreSQL**.
2. **Name**: `khata-ledger-db`
3. **Database**: `khataledger`
4. **User**: `khata`
5. **Region**: pick the one closest to you (Singapore is closest to India)
6. **Plan**: **Free**
7. Click **Create Database**.

When it finishes provisioning (~2-3 min), open the database page and copy:
- **Internal Database URL** (looks like `postgresql://khata:xxx@dpg-xxx-a/khataledger`)
- **External Database URL** (used for connecting from your laptop if you want to inspect data via psql)

Convert the internal URL to a JDBC URL: replace `postgresql://` with `jdbc:postgresql://`, drop the username/password (you'll set those as separate env vars).

For example, `postgresql://khata:abc123@dpg-xxx-a/khataledger` becomes JDBC URL `jdbc:postgresql://dpg-xxx-a/khataledger` with username `khata` and password `abc123`.

Note: Free Render Postgres expires after **90 days**. You'll get an email reminder. Either upgrade ($7/mo) or recreate. For a job-hunt window this is fine.

---

## Step 3: Deploy the backend on Render

1. Render dashboard -> **New +** -> **Web Service**.
2. Connect your GitHub account if prompted, then select the `khata-ledger` repo.
3. Configure:
   - **Name**: `khata-ledger-api`
   - **Region**: same as the database
   - **Branch**: `main`
   - **Root Directory**: `backend`
   - **Runtime**: Render auto-detects the `Dockerfile`. Leave it as Docker.
   - **Instance Type**: **Free**
4. Add **Environment Variables** (click "Advanced"):

   | Key                 | Value                                                  |
   |---------------------|--------------------------------------------------------|
   | `DB_URL`            | the JDBC URL from Step 2 (jdbc:postgresql://...)       |
   | `DB_USERNAME`       | `khata`                                                |
   | `DB_PASSWORD`       | the password from Step 2                               |
   | `JWT_SECRET`        | a random 40+ char string (use `openssl rand -hex 32`)  |
   | `GEMINI_API_KEY`    | your Gemini key (or leave blank — local router kicks in) |
   | `ALLOWED_ORIGINS`   | placeholder for now, fill in after Vercel deploys      |

5. Click **Create Web Service**. Render builds the Dockerfile (~5-8 min the first time) and starts the container.
6. When it's healthy, note the URL — e.g. `https://khata-ledger-api.onrender.com`.
7. Test it: `curl https://khata-ledger-api.onrender.com/actuator/health` — should return `{"status":"UP"}`. (First request will take 30-60s due to cold start.)
8. Open `https://khata-ledger-api.onrender.com/swagger-ui.html` to confirm Swagger UI loads.

---

## Step 4: Deploy the frontend on Vercel

1. Go to https://vercel.com/new -> import your `khata-ledger` repo.
2. Configure:
   - **Framework Preset**: Vite (Vercel auto-detects)
   - **Root Directory**: `frontend`
   - **Build Command**: `npm run build`
   - **Output Directory**: `dist`
3. Add **Environment Variable**:
   - **Key**: `VITE_API_BASE_URL`
   - **Value**: `https://khata-ledger-api.onrender.com/api` (from Step 3, with `/api` suffix)
4. Click **Deploy**. Build takes ~1-2 minutes.
5. Note the URL — e.g. `https://khata-ledger.vercel.app`.

---

## Step 5: Wire CORS so frontend can talk to backend

1. Back to your Render backend service -> **Environment** tab.
2. Set `ALLOWED_ORIGINS` to: `https://khata-ledger.vercel.app,http://localhost:5173`
   (replace `khata-ledger.vercel.app` with your actual Vercel URL)
3. Click **Save Changes**. Render redeploys (~30 seconds).

---

## Step 6: Verify the live app end-to-end

1. Open your Vercel URL in an incognito window.
2. Wait for cold start — the first signup may take a minute as Render wakes the backend.
3. Sign up with a test phone + password.
4. Add a customer.
5. Record one CREDIT and one DEBIT entry.
6. Open the customer page -> ask the ledger "who owes me the most?"
7. Open Swagger at `https://khata-ledger-api.onrender.com/swagger-ui.html` to see the deployed API contract.

---

## Step 7: Put the live URL on your resume

In your projects section, under the Khata Ledger entry, add:

- **Live**: https://khata-ledger.vercel.app
- **API**: https://khata-ledger-api.onrender.com/swagger-ui.html
- **Source**: https://github.com/your-username/khata-ledger

Recruiters click "Live" first. If they're technical, they click Swagger. If they're impressed, they click Source. All three should be there.

---

## Troubleshooting

| Symptom                                                  | Likely cause                                         | Fix |
|----------------------------------------------------------|------------------------------------------------------|-----|
| Frontend loads, login returns "Network Error"            | CORS not configured                                  | Recheck `ALLOWED_ORIGINS` on Render includes the exact Vercel URL (no trailing slash). Redeploy backend. |
| Backend won't start, logs show "Flyway migration failed" | DB credentials wrong, or DB region mismatch          | Verify `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` against the values on the Render Postgres dashboard. |
| First request hangs for 60 seconds                       | Free-tier cold start                                 | Expected. Warm up the URL ~1 minute before any demo. Consider a free uptime ping (e.g. UptimeRobot) hitting `/actuator/health` every 5 minutes. |
| Gemini calls all fail                                    | Wrong / missing `GEMINI_API_KEY`                     | Either set a valid key, or leave it blank — the local intent router will handle questions deterministically. |
| Postgres "your database has been suspended"              | 90-day free Postgres expiry                          | Create a new free Postgres on Render, dump-and-restore data, or upgrade to the $7/mo plan. |

---

## Going beyond the free tier later

When you want to remove cold starts and the 90-day Postgres expiry:

- **Render Starter**: $7/mo backend + $7/mo Postgres = $14/mo. No cold starts, persistent DB.
- **Railway**: ~$5-10/mo for the whole stack with better DX.
- **AWS Lightsail**: $5/mo for a small VPS. More setup, but uses skills already on your resume.

For an active job hunt (3-6 months), free tier with daily warm-up is perfectly fine.
