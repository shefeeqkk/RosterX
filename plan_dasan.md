# 📋 Execution Plan for Dasan (Backend & Server-Side Filtering Engine)

**Status:** `READY FOR DASAN` (Sprint v2.2.0 - Build 151 Permanent Notification Deletion & Calendar Target State Engine)

---

## 🔐 Mandatory Production Rule from Gafoor (System Architect 🕶️)
> **Public GitHub Repo + Auto-Deploying Render Production Server:**
> - **ZERO hardcoded personal names, usernames, emails, phone numbers, passwords, or secrets** anywhere in code, comments, or tests.
> - **Mandatory `git push origin main`** immediately after verification so Render (`https://api.rosterx.online`) auto-deploys cleanly.

---

## 1. Task 1 — Permanent Server-Side Notification Filtering (`server.py`)
Fix the issue where cleared notifications reappear in Web or after uninstalling/reinstalling Android!

### Implementation Details:
1. **Filter Out Dismissed Notifications on the Server (`GET /api/notifications`)**:
   - In `GET /api/notifications`:
     - Retrieve caller's `dismissed_notifications` set from `ATLAS_DB.engineers` (and in-memory fallback `SERVER_USER_DISMISSED_NOTIFICATIONS`).
     - For non-admin callers (regular engineers), **filter out** any notification whose `id` or `_id` is present in `dismissed_notifications`:
       ```python
       dismissed_set = set(dismissed_ids)
       feed_notifs = [n for n in feed_notifs if str(n.get("id") or n.get("_id") or "") not in dismissed_set]
       ```
     - Return filtered `notifications: feed_notifs` AND `dismissed_notification_ids: dismissed_ids`.
     - **Result**: Once a notification is dismissed, the server NEVER serves it to that user again, even after a fresh app install or browser reload!

2. **Harden `POST /api/notifications/dismiss`**:
   - Accept both `notification_id` and `id` (`notif_id = str(body.get("notification_id") or body.get("id") or "").strip()`).
   - For `dismiss_all`:
     - Gather all notification IDs currently present in:
       1. `ATLAS_DB.notifications`
       2. Active celebration notifications (`check_and_trigger_celebrations()`)
       3. Active credential expiry notifications
       4. `SERVER_NOTIFICATIONS`
     - Save all collected IDs into `ATLAS_DB.engineers.dismissed_notifications` using `$addToSet: {"$each": ...}`.
     - Also update `SERVER_USER_DISMISSED_NOTIFICATIONS[username]`.

---

## 2. Task 2 — Target Calendar State & Sync Persistence (`server.py`)
Ensure the Target Calendar account selected on Android or Web is securely persisted in MongoDB Atlas:

1. **`POST /api/users/calendar-sync`**:
   - Accept `{ "month": "YYYY-MM", "count": N, "target": "user@example.com", "synced": true }`.
   - Update `ATLAS_DB.engineers`:
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
2. **`GET /api/users/calendar-sync`**:
   - Return `{ "success": True, "month": month, "sync": data.get("calendar_sync", {}).get(month), "synced": ... }`.
   - Ensure `calendar_sync` map is included in profile / login responses for instant client hydration.

---

## 3. Verification, Version Bump & Mandatory Push
1. Run `python -m py_compile server.py` and verify:
   - Test `POST /api/notifications/dismiss` (single & all) -> verifies database storage.
   - Test `GET /api/notifications` -> verifies dismissed notifications are omitted from feed.
   - Test `POST /api/users/calendar-sync` -> records target account and count.
   - Zero hardcoded personal names/emails/passwords.
2. Bump `SERVER_VERSION = "2.2.0"` in `server.py`.
3. Commit with semantic message (`feat(backend): permanent server-side notification deletion & calendar target state engine v2.2.0 (Build 151)`), update `AGENT_HANDOFF.md`, and **immediately run `git push origin main`**!
