# 📋 Execution Plan for Dasan (Backend & Infrastructure)

**Status:** `COMPLETED` (Sprint v1.34.0 - Build 139 Android Verified App Links + Deep-Link Email CTAs + In-App Update Metadata verified live & pushed)

---

## 🔐 Mandatory Production Rule from Gafoor (System Architect 🕶️)
> **Public GitHub Repo + Auto-Deploying Render Production Server:**
> - **ZERO hardcoded personal names, usernames, emails, passwords, or secrets** anywhere in code, comments, or tests.
> - **Mandatory `git push origin main`** immediately after verification so Render (`https://api.rosterx.online`) auto-deploys cleanly.

---

## 1. Task 1 — Android Digital Asset Links (`GET /.well-known/assetlinks.json`)
Enables Android's **"Open by default" → Verified link (`api.rosterx.online`)** so RosterX links open straight into the native Android app without a browser chooser dialog.

### Technical Specification (`server.py`, `render.yaml`, `.env.example`):
1. **Endpoint `GET /.well-known/assetlinks.json` (PUBLIC — no auth required!)**:
   - Read comma-separated SHA-256 certificate fingerprints from environment variable `ANDROID_APP_LINK_SHA256` (also check `.env`).
   - Normalize each fingerprint: strip whitespace, uppercase, ignore empty strings.
   - Return HTTP `200` with `Content-Type: application/json` and `Cache-Control: public, max-age=3600`:
     ```json
     [
       {
         "relation": ["delegate_permission/common.handle_all_urls"],
         "target": {
           "namespace": "android_app",
           "package_name": "com.rosterx",
           "sha256_cert_fingerprints": ["<FINGERPRINT_1>", "..."]
         }
       }
     ]
     ```
   - Never hardcode keystore fingerprints in source code — document `ANDROID_APP_LINK_SHA256` in `.env.example` and add `ANDROID_APP_LINK_SHA256` (`sync: false`) in `render.yaml`.

---

## 2. Task 2 — Graceful Desktop Fallback for Deep Links (`GET /open/*`)
- When a user taps `https://api.rosterx.online/open/roster`, `/open/notifications`, `/open/credentials`, or `/open/profile`:
  - **On Android (with RosterX installed)**: Android App Links intercepts the URL and opens the exact screen inside the native app.
  - **On Desktop / Browser (without the app)**: `server.py` MUST NOT return 404! Add a clean handler for `/open` and `/open/*` that responds with `302 Found` (`Location: /` — or `/#roster`, `/#notifications`, `/#credentials`, `/#profile`) so desktop users seamlessly land on the RosterX Web Portal!

---

## 3. Task 3 — Clickable "Open in RosterX App" Deep-Link CTAs in Automated Emails (`server.py`)
- Define `APP_PUBLIC_BASE_URL = os.environ.get("APP_PUBLIC_BASE_URL", "https://api.rosterx.online").rstrip("/")`.
- Add a clean, branded pill button (`Open in RosterX App →`) inside RosterX's automated HTML emails:
  1. **Broadcast Emails** (`POST /api/notifications/send`) → `f"{APP_PUBLIC_BASE_URL}/open/notifications"`
  2. **Credential Expiry Alert Emails** (daily credential monitor) → `f"{APP_PUBLIC_BASE_URL}/open/credentials"`
  3. **Welcome / Onboarding Emails** (`POST /api/engineers` new account) → `f"{APP_PUBLIC_BASE_URL}/open/profile"` (or root `f"{APP_PUBLIC_BASE_URL}/"`)
  4. **Roster / Shift Emails** (if applicable) → `f"{APP_PUBLIC_BASE_URL}/open/roster"`

---

## 4. Task 4 — In-App Update Metadata in `GET /api/server-info` (`server.py`, `render.yaml`, `.env.example`)
- Enhance `GET /api/server-info` response (keeping all existing keys intact for backwards compatibility) with:
  ```json
  {
    "version": "1.34.0",
    "latest_android_version": "1.34.0",
    "min_supported_version": "1.30.0",
    "apk_download_url": "https://github.com/shefeeqkk/RosterX/releases/latest"
  }
  ```
- Read `APK_DOWNLOAD_URL` from environment variable (defaulting to `https://github.com/shefeeqkk/RosterX/releases/latest`) and `MIN_SUPPORTED_VERSION` (optional env var, default `"1.0.0"`). Document both in `.env.example` and `render.yaml`.

---

## 5. Verification, Version Bump & Mandatory Push
1. Run `python -m py_compile server.py` and verify:
   - `GET /.well-known/assetlinks.json` returns valid JSON array with `com.rosterx` and parsed `ANDROID_APP_LINK_SHA256` fingerprints.
   - `GET /open/credentials`, `/open/notifications`, `/open/roster`, `/open/profile` return clean `302` redirect to `/` (no 404).
   - `GET /api/server-info` returns `latest_android_version`, `min_supported_version`, and `apk_download_url`.
   - Zero hardcoded PII or secrets anywhere.
2. Bump `SERVER_VERSION = "1.34.0"` in `server.py`.
3. Commit with semantic message, update `AGENT_HANDOFF.md`, and **immediately run `git push origin main`**!
