# 📋 Execution Plan for Dasan (Backend, Folder Rename & iOS PWA Serving)

**Status:** `COMPLETED` (Step 1 of Sprint v2.0.0 — `admin-web/` → `web/` rename, iOS PWA MIME/header serving, `/open/*` redirects & full REST route-alias parity verified & pushed at v1.36.0; ready for Vijayan Step 2 & final v2.0.0 Build 143 release)
**Execution Order:** **STEP 1 (Run Dasan FIRST)** — Vijayan's `web/` UI overhaul builds directly on top of your `admin-web/` → `web/` rename and PWA static serving contract!

---

## 🔐 Mandatory Production Rule from Gafoor (System Architect 🕶️)
> **Public GitHub Repo + Auto-Deploying Render Production Server:**
> - **ZERO hardcoded personal names, usernames, emails, phone numbers, passwords, or secrets** anywhere in code, comments, or tests.
> - **Mandatory `git push origin main`** immediately after verification so Render (`https://api.rosterx.online`) auto-deploys cleanly and Vijayan can pull `web/` cleanly.

---

## 1. Task 1 — Clean Directory Rename (`admin-web/` → `web/`)
`admin-web/` serves Engineer, Manager, and Admin portals alike — rename it cleanly to **`web/`** alongside `android/`.

### Implementation Steps:
1. Run `git mv admin-web web` so Git preserves full commit history for all HTML/CSS/JS/asset files.
2. Update all repository references from `admin-web` → `web`:
   - `server.py`:
     - `WEB_DIR = ROOT_DIR / "web"` (currently line 139)
     - Docstrings, comments, and error messages referencing `'admin-web'`
   - `README.md`: update project structure tree & references from `admin-web/` → `web/`.
   - `.agents/skills/rosterx-ui/SKILL.md`: update `admin-web/js/dateUtils.js` reference to `web/js/dateUtils.js`.
   - Check `render.yaml`, `.gitignore`, and any root scripts for any stray `admin-web` paths (update if found; do NOT touch historical handoff log paragraphs unless needed).

---

## 2. Task 2 — iOS PWA Static Serving & MIME Contract (`server.py`)
Ensure `server.py`'s static file handler serves iOS PWA & Calendar export files with strict, Safari-compliant headers:
1. **Explicit MIME Types**:
   - `.webmanifest` → `application/manifest+json; charset=utf-8`
   - `.ics` → `text/calendar; charset=utf-8`
   - `sw.js` → `application/javascript; charset=utf-8`
2. **Service Worker & Manifest Headers**:
   - When serving `/sw.js`, include `Service-Worker-Allowed: /` and `Cache-Control: no-cache, no-store, must-revalidate` so iOS Safari always picks up new PWA releases cleanly.
   - When serving `/manifest.webmanifest`, include `Cache-Control: public, max-age=3600`.
3. **Deep-Link Fallback Routes (`GET /open/*`)**:
   - Verify `/open`, `/open/roster`, `/open/notifications`, `/open/credentials`, `/open/profile`, `/open/dashboard` return clean `302 Found` redirects to `/` or `/#roster`, `/#notifications`, `/#credentials`, `/#profile`, `/#dashboard` (so iOS Safari & desktop users clicking email CTAs jump straight into the matching web tab!).

---

## 3. Task 3 — Backend API Readiness Audit for Full Web–Android Parity (`server.py`)
Audit (and fix only if missing/blocked) every REST API endpoint that Vijayan's `web/` portal will consume with JWT Bearer auth:
1. **Credential Tracker API** (for Engineer read/renew + Manager/Admin full CRUD & logs):
   - `GET /api/credentials`
   - `POST /api/credentials`
   - `PUT /api/credentials/<id>`
   - `DELETE /api/credentials/<id>`
   - `GET /api/credentials/logs`
   - Confirm Engineer role can call `GET /api/credentials` and authorized spokespersons can update `last_updated` date cleanly (exact parity with Android `CredentialTrackerScreen`).
2. **Profile Picture Upload & Removal**:
   - Confirm web JWT users can upload AND remove (`profile_picture: ""` / `null` or dedicated endpoint) their own profile photo cleanly.
3. **Broadcast Templates & Retract**:
   - Confirm Manager AND Admin roles can access `GET/POST/DELETE /api/broadcast-templates`, `POST /api/notifications/send`, and `DELETE /api/notifications/<id>` (matching Android Manager Broadcast tab!).
4. **Zero-Knowledge Team Creation**:
   - Confirm `POST /api/engineers` works cleanly without a manual password field (auto-generating secure random password + OTP onboarding email, matching Android v1.33.0!).

---

## 4. Shared API & Routing Contract (Dasan ⇄ Vijayan)
- **Web Static Root**: `web/` served directly at `https://api.rosterx.online/`
- **Role Landing Routes (enforced in `web/js/authService.js` & `web/login.html` by Vijayan)**:
  - `admin` → `/index.html` (5 Admin tabs: `Dashboard`, `Register`, `Rosters`, `Broadcast`, `More`)
  - `manager` → `/user-portal.html` (5 Android Manager tabs: `Dashboard`, `Team Shifts`, `Month Roster`, `Broadcast`, `More`)
  - `engineer` / `apprentice` → `/user-portal.html` (5 Android Engineer tabs: `Dashboard`, `Team Shifts`, `My Calendar`, `Month Roster`, `More`)
- **PWA Files (created by Vijayan inside `web/`)**:
  - `/manifest.webmanifest`
  - `/sw.js`

---

## 5. Verification, Handoff & Mandatory Push
1. Run `python -m py_compile server.py`.
2. Start `server.py` locally (or test handler) and verify:
   - `GET /` and `GET /login.html` and `GET /user-portal.html` return HTTP `200` served from `web/`.
   - `GET /.well-known/assetlinks.json` still returns HTTP `200` JSON cleanly.
   - `GET /open/credentials` returns `302` cleanly.
   - Zero occurrences of broken `admin-web` paths in `server.py`.
   - Zero hardcoded PII or secrets.
3. Keep version at `1.36.0` (or bump to `1.37.0` if preferred; final **`v2.0.0` (Build `143`)** bump happens when Vijayan finishes the full iOS PWA & Android parity frontend — note your chosen version clearly in `AGENT_HANDOFF.md`).
4. Mark `plan_dasan.md` as `COMPLETED`, commit with a clean semantic message, update `AGENT_HANDOFF.md` & `android/AGENT_HANDOFF.md` briefing Vijayan that `web/` is live, and **immediately run `git push origin main`**!
