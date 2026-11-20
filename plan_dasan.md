# 📋 Execution Plan for Dasan (Backend & Server Infrastructure Lead)

**Status:** `COMPLETED` (Sprint v2.3.1 - Build 153 Dynamic Festival App Config, Bidirectional Sync Support & Version Bump Verified & Synced)

---

## 🔐 Mandatory Production Rule from Gafoor (System Architect 🕶️)
> **Public GitHub Repo + Auto-Deploying Render Production Server:**
> - **ZERO hardcoded personal names, usernames, emails, phone numbers, passwords, or secrets** anywhere in code, comments, or tests.
> - **Mandatory `git push origin main`** immediately after verification so Render (`https://api.rosterx.online`) auto-deploys cleanly.

---

## 1. Task 1 — App Configuration & Festival Theme Engine (`server.py`)
Support the dynamic festive launcher icon and celebration system:

1. **`GET /api/app-config`**:
   - Return active festival/celebration config:
     ```json
     {
       "success": true,
       "version": "2.3.1",
       "active_theme": "default",
       "active_celebration": null,
       "festivals": {
         "diwali": {"start": "2026-11-06", "end": "2026-11-12"},
         "christmas": {"start": "2026-12-20", "end": "2026-12-27"},
         "new_year": {"start": "2026-12-30", "end": "2027-01-03"}
       }
     }
     ```
   - Automatically check if today is any team member's birthday or work anniversary from `ATLAS_DB.engineers`. If yes, populate `active_celebration` with `{ type, username, name, profile_picture, years }`.

2. **Digital Asset Links Verification (`/.well-known/assetlinks.json`)**:
   - Ensure `/.well-known/assetlinks.json` reliably returns the debug and release cert fingerprints with `delegate_permission/common.handle_all_urls` and `delegate_permission/common.get_login_creds` for `com.rosterx`.

---

## 2. Task 2 — Verification, Version Bump & Mandatory Push
1. Run `python -m py_compile server.py` and verify:
   - Test `GET /api/app-config` -> returns valid JSON with active celebration/theme.
   - Test `GET /.well-known/assetlinks.json` -> returns valid delegation targets.
   - Zero hardcoded personal names, real emails, or secrets.
2. Bump `SERVER_VERSION = "2.3.1"` in `server.py`.
3. Commit with semantic message (`feat(backend): festival app-config engine & version bump v2.3.1 (Build 153)`), update `AGENT_HANDOFF.md`, and **immediately run `git push origin main`**!
