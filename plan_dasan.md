# 📋 Execution Plan for Dasan (Backend & Cloud-Backed Calendar Sync)

**Status:** `READY FOR DASAN` (Sprint v2.1.0 - Build 150 Cloud-Backed Calendar Sync Engine & User Sync State API)

---

## 🔐 Mandatory Production Rule from Gafoor (System Architect 🕶️)
> **Public GitHub Repo + Auto-Deploying Render Production Server:**
> - **ZERO hardcoded personal names, usernames, emails, phone numbers, passwords, or secrets** anywhere in code, comments, or tests.
> - **Mandatory `git push origin main`** immediately after verification so Render (`https://api.rosterx.online`) auto-deploys cleanly.

---

## 1. Task 1 — Cloud-Backed Calendar Sync Endpoints (`server.py`)
Enable Android and Web UI to stay 100% in sync regarding which months and Google accounts have been synced!

### Endpoints Specification:
1. **`POST /api/users/calendar-sync` (AUTHENTICATED)**:
   - Body: `{ "month": "2026-11", "count": 30, "target": "user@gmail.com", "synced": true }`
   - Store in MongoDB Atlas:
     ```python
     ATLAS_DB.engineers.update_one(
         {"username": caller_username},
         {"$set": {
             f"calendar_sync.{month}": {
                 "synced": bool(body.get("synced", True)),
                 "count": int(body.get("count", 0)),
                 "target": str(body.get("target") or "Google Calendar").strip(),
                 "synced_at": time.strftime("%Y-%m-%d %H:%M:%S UTC", time.gmtime()),
                 "timestamp": time.time()
             }
         }}
     )
     ```
   - Update in-memory user cache if applicable.
   - Return `{ "success": True, "message": f"Calendar sync recorded for {month}", "data": ... }`.
2. **`GET /api/users/calendar-sync` (AUTHENTICATED)**:
   - Optional query param `?month=2026-11`.
   - Query `ATLAS_DB.engineers.find_one({"username": caller_username}, {"calendar_sync": 1})`.
   - If `month` supplied, return specific month status `{ "success": True, "month": month, "sync": data.get("calendar_sync", {}).get(month) }`.
   - Otherwise return all synced months `{ "success": True, "calendar_sync": data.get("calendar_sync", {}) }`.
3. **`POST /api/users/calendar-sync/clear` (AUTHENTICATED)**:
   - Body: `{ "month": "2026-11" }`
   - Unset `calendar_sync.{month}` in `ATLAS_DB.engineers`.
   - Return `{ "success": True, "message": f"Calendar sync cleared for {month}" }`.
4. **Include `calendar_sync` in User Profile Payloads**:
   - In `GET /api/engineers` and login/session verification endpoints, ensure `calendar_sync` map is returned for the user so Web UI has it cached immediately on boot.

---

## 2. Task 2 — Notification Dismissal Sync Support (`server.py`)
Fix dismissal parity so notifications cleared on Android never reappear on Web!

1. **Accept both `notification_id` and `id` in `POST /api/notifications/dismiss`**:
   - In `server.py` line ~5087:
     ```python
     notif_id = str(body.get("notification_id") or body.get("id") or "").strip()
     dismiss_all = bool(body.get("all", False))
     ```
2. **Comprehensive `dismiss_all` across all in-feed notifications**:
   - In `dismiss_all`, collect notification IDs not just from `ATLAS_DB.notifications`, but also all active celebration notifications, credential alerts, and broadcast announcements so they are cleanly added to `ATLAS_DB.engineers` `dismissed_notifications` array!
3. **Return `dismissed_notification_ids` cleanly** in `GET /api/notifications` payload.

---

## 3. Verification, Version Bump & Mandatory Push
1. Run `python -m py_compile server.py` and verify:
   - Test `POST /api/users/calendar-sync` with test token → returns 200 and records in DB.
   - Test `GET /api/users/calendar-sync?month=2026-11` → returns recorded sync data.
   - Test `POST /api/users/calendar-sync/clear` → cleanly removes the month.
   - Test `POST /api/notifications/dismiss` with both `id` and `notification_id` keys → successfully adds to `dismissed_notifications`.
   - Zero hardcoded personal names/emails/passwords.
2. Bump `SERVER_VERSION = "2.1.0"` in `server.py`.
3. Commit with semantic message (`feat(backend): calendar sync endpoints & notification dismissal parity v2.1.0 (Build 150)`), update `AGENT_HANDOFF.md`, and **immediately run `git push origin main`**!
