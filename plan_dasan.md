# 📋 Execution Plan for Dasan (Backend & Server Infrastructure Lead)

**Status:** `COMPLETED` (Sprint v2.3.0 - Build 152 Calendar Accounts Synchronization & Target Calendar Multi-Account Engine Verified & Synced)

---

## 🔐 Mandatory Production Rule from Gafoor (System Architect 🕶️)
> **Public GitHub Repo + Auto-Deploying Render Production Server:**
> - **ZERO hardcoded personal names, usernames, emails, phone numbers, passwords, or secrets** anywhere in code, comments, or tests.
> - **Mandatory `git push origin main`** immediately after verification so Render (`https://api.rosterx.online`) auto-deploys cleanly.

---

## 1. Task 1 — Device Calendar Accounts Synchronization API (`server.py`)
Bridge Android native `CalendarContract` discovery with the Web UI by persisting device Google accounts and secondary calendars into MongoDB Atlas:

### Implementation Details:
1. **`POST /api/users/calendar-accounts`**:
   - Authenticated endpoint for Android/Web clients.
   - Payload:
     ```json
     {
       "calendar_accounts": [
         {
           "id": "1",
           "name": "Work Shifts",
           "account_name": "user@gmail.com",
           "account_type": "com.google",
           "is_primary": false,
           "color": "#174EAC"
         }
       ]
     }
     ```
   - Validation & Hygiene:
     - Ensure `account_name` and `name` are trimmed strings.
     - Avoid saving null/empty accounts.
   - Database Persistence:
     - Update caller's engineer profile in `ATLAS_DB.engineers`:
       ```python
       ATLAS_DB.engineers.update_one(
           {"username": caller_username},
           {"$set": {
               "calendar_accounts": validated_accounts,
               "calendar_accounts_updated_at": time.strftime("%Y-%m-%d %H:%M:%S UTC", time.gmtime())
           }}
       )
       ```
     - Also maintain in-memory fallback cache for fast responses.

2. **`GET /api/users/calendar-accounts`**:
   - Returns caller's stored calendar accounts:
     ```json
     {
       "success": true,
       "calendar_accounts": [...],
       "corporate_email": "user@corporate.com"
     }
     ```
   - Also ensure `calendar_accounts` is returned in `/api/profile` and `/api/login` so the Web UI gets hydrated instantly without extra roundtrips.

---

## 2. Task 2 — Target Calendar State & Sync Persistence (`server.py`)
Ensure Target Calendar selection supports Corporate, Personal Google, and Secondary Calendars:

1. **`POST /api/users/calendar-sync`**:
   - Accept `{ "month": "YYYY-MM", "count": N, "target": "Work Shifts (user@gmail.com)", "target_type": "secondary"|"corporate"|"google", "synced": true }`.
   - Update `ATLAS_DB.engineers`:
     ```python
     ATLAS_DB.engineers.update_one(
         {"username": caller_username},
         {"$set": {
             f"calendar_sync.{month}": {
                 "synced": bool(body.get("synced", True)),
                 "count": int(body.get("count", 0)),
                 "target": str(body.get("target") or "Google Calendar").strip(),
                 "target_type": str(body.get("target_type") or "google").strip(),
                 "synced_at": time.strftime("%Y-%m-%d %H:%M:%S UTC", time.gmtime()),
                 "timestamp": time.time()
             }
         }}
     )
     ```
2. **`GET /api/users/calendar-sync`**:
   - Returns `{ "success": True, "month": month, "sync": data.get("calendar_sync", {}).get(month), "synced": ... }`.

---

## 3. Task 3 — Verification, Version Bump & Mandatory Push
1. Run `python -m py_compile server.py` and verify:
   - Test `POST /api/users/calendar-accounts` -> stores accounts cleanly in MongoDB Atlas.
   - Test `GET /api/users/calendar-accounts` -> returns stored accounts.
   - Test `POST /api/users/calendar-sync` -> records target account and count.
   - Test `POST /api/chat/assistant` multi-turn memory & conversational responses.
   - Ensure ZERO hardcoded personal names, real emails, or secrets.
2. Bump `SERVER_VERSION = "2.3.0"` in `server.py`.
3. Commit with semantic message (`feat(backend): calendar accounts synchronization API & multi-calendar target engine v2.3.0 (Build 152)`), update `AGENT_HANDOFF.md`, and **immediately run `git push origin main`**!
