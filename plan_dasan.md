# 📋 Execution Plan for Dasan (Backend, Web & Production Hygiene)

**Status:** `COMPLETED` (Sprint v1.33.0 - Build 137 Broadcast Title `@` Mentions + 100% Cloudflared Removal + Zero-PII DB-Driven Roster Metadata + Zero-Knowledge Onboarding verified & pushed)

---

## 🔐 Mandatory Production Rule from Gafoor (System Architect 🕶️)
> **Public GitHub Repo + Auto-Deploying Render Production Server:**
> - **ZERO hardcoded personal names, usernames, emails, phone numbers, passwords, or secrets** anywhere in code, comments, templates, or tests! Always use env vars, MongoDB fields, or synthetic examples (`user@example.com`, `Team Member`, `member_username`).
> - **Mandatory `git push origin main`** immediately after verified commits so Render (`https://api.rosterx.online`) stays version-synced.

---

## 1. Task 1 — Broadcast Title `@` Mention Personalization (`server.py`)
1. **Unified Personalization Helper (`_personalize_broadcast_text`)**:
   - Create a reusable helper shared by Title and Body across `POST /api/notifications/send` and `GET /api/notifications` that resolves:
     - `@name` / `{name}` / `{their name}` → recipient's **Full Name**
     - `{username}` / `@{username}` → recipient's username
     - `@all` / `@everyone` / `@team` → `@Everyone` (plain text) or styled badge (HTML)
     - `@<username>` → `@<Full Name>` via `name_by_user` / `name_by_id` lookup
2. **Apply to Broadcast `title` Across All 3 Channels**:
   - **Email (Resend)** (`POST /api/notifications/send`):
     - Subject: `f"📢 [{priority.upper()}] {p_title_plain}"`
     - HTML `<h2>`: `{html_p_title}`
   - **FCM Push** (`POST /api/notifications/send`):
     - Push title: `f"📢 [{priority.upper()}] {p_title_plain}"`
   - **In-App Feed** (`GET /api/notifications`):
     - For non-admin callers, resolve `@name`, `{name}`, `@all`, and `@<username>` in **both** `item["title"]` and `item["raw_message"]`.
     - Preserve raw `title` in MongoDB `ATLAS_DB.notifications` and Admin history views.

---

## 2. Task 2 — 100% Zero-Trace Cloudflared Tunnel Removal
Remove EVERY Cloudflared / `trycloudflare.com` reference from backend, config, and scripts:
1. **`server.py`**:
   - Delete `PUBLIC_TUNNEL_URL`, `TUNNEL_PROC`, `find_cloudflared_path()`, `save_public_tunnel_url()` (`public_tunnel_url.txt` writer), and `start_cloudflared_tunnel()`.
   - Remove `.trycloudflare.com` from CORS origin allowlist (keep localhost, `.onrender.com`, and `rosterx.online`).
   - Clean `/api/server-info` and status JSON responses (remove `tunnel_active` / `PUBLIC_TUNNEL_URL` references; expose official production/LAN URL cleanly).
   - Remove `--tunnel` / `--no-tunnel` CLI flags, `ENABLE_TUNNEL` env var handling, and `enable_tunnel` parameter from `run_server()`.
2. **`render.yaml`**:
   - Update `startCommand: python server.py --no-open` (remove `--no-tunnel`).
   - Remove `ENABLE_TUNNEL` env var entry.
3. **Delete Unused Tunnel Scripts**:
   - Remove `tools/start_tunnel.bat` and `tools/start_tunnel.ps1` via `git rm`.
4. **`.gitignore`**:
   - Remove obsolete `public_tunnel_url.txt` line.

---

## 3. Task 3 — Zero-PII & DB-Driven Roster Metadata (`server.py` + `admin-web/`)
1. **Shared API Contract (`ATLAS_DB.engineers`)**:
   - Ensure engineer GET/POST/PUT endpoints preserve & accept optional DB metadata fields:
     - `include_in_roster` (`bool`, optional)
     - `display_order` (`int`, optional)
     - `swap_code` (`str`, optional)
2. **Scrub All Remaining Hardcoded Team Names/Usernames in `admin-web/` & `server.py`**:
   - `admin-web/js/app.js` & `admin-web/js/userPortal.js`:
     - Replace hardcoded operational-admin username checks with DB-driven `include_in_roster` check + generic fallback (exclude `manager` role and generic system accounts like `admin`/`sysadmin`/`administrator`, never personal usernames).
     - Replace real email/name examples in comments with generic `user@example.com`.
   - `admin-web/js/constants.js`:
     - Replace hardcoded name-to-swap-code `if` block in `getEngineerSwapCode(engName, allEngineers)` with:
       1. Use `eng.swap_code` if provided from DB,
       2. Otherwise algorithmic generator: `'SW'` + 1st letter (or 2 letters / first+last initials when multiple active engineers share the same starting letter).
     - Replace hardcoded `KNOWN_ORDER` name dictionary in `getEngineerOrderWeight(eng)` with:
       1. `eng.display_order` (or `eng.displayOrder`) if set in DB,
       2. Numeric ID fallback (`eng.id`),
       3. Alphabetical name tie-breaker.
   - `admin-web/index.html`, `admin-web/js/excelService.js`, `admin-web/js/rosterAiAssistant.js`, `admin-web/js/rosterAiEngine.js`, `admin-web/README.md`, `server.py` line 333 comment:
     - Replace all remaining real employee names and corporate email examples with generic synthetic placeholders (`Team Member`, `Member A`, `user@example.com`).

---

## 4. Task 4 — Zero-Knowledge Account Onboarding & Block Admin Password Overwrites (`server.py` + `admin-web/`)
Security fix: Admins/Managers must NEVER be able to change or know any user's or manager's password from Team Management!
1. **Backend Enforcement (`server.py` — `POST /api/engineers`)**:
   - Check whether the target account already exists in `ATLAS_DB.engineers` (by `eng_user` or `old_user`).
   - **Existing Account**: Strictly `eng.pop("password", None)` — NEVER overwrite an existing user's or manager's password hash via `POST /api/engineers`! (Users/managers change their own password only via self-service Profile `POST /api/auth/change-password` or `"Forgot password?"` OTP.)
   - **Brand-New Account**: Ignore any client-supplied password; auto-generate a cryptographically secure random password (`secrets.token_urlsafe(24)`), store its hash via `hash_password(...)` (never return it in the HTTP response), and if `eng_email` is present, dispatch a clean **Welcome to RosterX** email with their username instructing them to set their private password via **"Forgot password?" (Email OTP)** on the login screen.
2. **Admin Web Modal (`admin-web/index.html` & `admin-web/js/app.js`)**:
   - Remove `#formEngPassword` (`Initial Password *`) input & visibility toggle from the Add/Edit Team Member modal and never send `password` in `POST /api/engineers` from the web portal. Add a subtle security note: *"Users set and manage their own password privately via Forgot Password (OTP)."*

---

## 5. Verification, Version Bump & Mandatory Push
1. Run `python -m py_compile server.py` and verify:
   - Broadcast title personalization across Email, FCM, and `GET /api/notifications`.
   - Zero `cloudflared` / `trycloudflare` / `public_tunnel_url` matches in `server.py`, `render.yaml`, `tools/`.
   - Zero hardcoded personal names/emails/passwords in `server.py` and `admin-web/`.
   - `POST /api/engineers` refuses to overwrite existing account passwords and auto-generates random password hash + welcome email on new account creation.
2. Bump `SERVER_VERSION = "1.33.0"` in `server.py`.
3. Commit with semantic message, update `AGENT_HANDOFF.md`, and **run `git push origin main`** so Render (`https://api.rosterx.online`) deploys immediately!
