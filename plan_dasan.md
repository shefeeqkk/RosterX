# 📋 Execution Plan for Dasan (Backend & Core Orchestration)

**Status:** `READY FOR DASAN` (Sprint v1.32.0 - Build 132 Auto-Purge & Clean Sweep for Expired Celebrations)

---

## 1. Architectural Mandates from Gafoor (System Architect 🕶️)

Dasa! Nammude user oru valiya issue report cheythittundu: Birthday-yum Work Anniversary-yum aaya notifications aa divasam kazhinjaalum drawer-il ninnu maaranilla! "Yesterday, 05:52 PM" ennum paranju linger aayi nilkkukayaanu.
Grill-me session-il user clear aayi paranju:
1. **Hard Purge in MongoDB**: Aa calendar date (IST midnight) kazhinjaal, MongoDB notifications collection-il ninnu past celebration documents poornamaayi **permanently delete** cheyyanam (`delete_many`).
2. **Clean Sweep (Remove Everywhere)**: User-nte notification drawer-il ninnu maathramalla, Admin/Manager-nte Broadcast History-il ninnum clean sweep aayi maarikkollanam.

### 🎯 Key Directives for Dasan:
1. **Dedicated Celebration Auto-Purge Function (`purge_expired_celebrations`) in `server.py`**:
   - Calculate current IST date (`today_str = now_ist.strftime("%Y-%m-%d")`) and start of today in IST timestamp:
     ```python
     start_of_today_ist = datetime(now_ist.year, now_ist.month, now_ist.day, tzinfo=ist_tz).timestamp()
     ```
   - Target celebration records:
     - Identification: `category: "celebration"` OR `celebration_id` exists OR `celebration_type` exists.
     - Condition for expiration: `timestamp < start_of_today_ist` OR `celebration_id` does NOT start with `celeb_{today_str}_`.
   - In MongoDB `ATLAS_DB.notifications`:
     - Run `delete_many` to permanently delete these expired celebration documents.
   - In `SERVER_NOTIFICATIONS` (in-memory deque):
     - Filter out expired celebration items.
2. **Integrate Purge Engine into Lifecycle**:
   - In `GET /api/notifications`:
     - Call `purge_expired_celebrations()` before fetching notifications.
     - Defensively filter the fetched `notifs` list so that no past celebration is returned under ANY circumstances.
   - In `check_and_trigger_celebrations(force=False)`:
     - Run `purge_expired_celebrations()` at the beginning of the function before evaluating today's milestone checks.
   - In `_daily_celebration_cron()`:
     - Run `purge_expired_celebrations()` every day at 08:30 IST and on initial boot.
3. **Version Bump**:
   - Bump `SERVER_VERSION = "1.32.0"` in `server.py`.

---

## 2. Technical Blueprint for Dasan

### 📁 Primary File:
- `server.py`

### 🛠️ Step-by-Step Implementation:
1. **Define `purge_expired_celebrations()`**:
   ```python
   def purge_expired_celebrations():
       """
       Hard Purge Engine:
       Permanently deletes celebration notices from ATLAS_DB.notifications and
       SERVER_NOTIFICATIONS where the celebration event date is strictly before today's IST date.
       """
       global ATLAS_DB, SERVER_NOTIFICATIONS
       try:
           ist_tz = timezone(timedelta(hours=5, minutes=30))
           now_ist = datetime.now(ist_tz)
           today_str = now_ist.strftime("%Y-%m-%d")
           start_of_today_ist = datetime(now_ist.year, now_ist.month, now_ist.day, tzinfo=ist_tz).timestamp()

           deleted_count = 0
           if ATLAS_DB is not None:
               celeb_filter = {
                   "$and": [
                       {
                           "$or": [
                               {"category": "celebration"},
                               {"celebration_id": {"$exists": True}},
                               {"celebration_type": {"$exists": True}}
                           ]
                       },
                       {
                           "$or": [
                               {"timestamp": {"$lt": start_of_today_ist}},
                               {"celebration_id": {"$regex": r"^celeb_(?!" + re.escape(today_str) + r")"}}
                           ]
                       }
                   ]
               }
               res = ATLAS_DB.notifications.delete_many(celeb_filter)
               deleted_count = res.deleted_count
               if deleted_count > 0:
                   log_server(f"[PURGE] Permanently deleted {deleted_count} expired celebration notifications from MongoDB.")

           # Purge in-memory deque as well
           if SERVER_NOTIFICATIONS:
               SERVER_NOTIFICATIONS = collections.deque(
                   [
                       n for n in SERVER_NOTIFICATIONS
                       if not (
                           (n.get("category") == "celebration" or n.get("celebration_id") or n.get("celebration_type"))
                           and (
                               n.get("timestamp", 0) < start_of_today_ist
                               or (str(n.get("celebration_id", "")).startswith("celeb_") and not str(n.get("celebration_id", "")).startswith(f"celeb_{today_str}"))
                           )
                       )
                   ],
                   maxlen=SERVER_NOTIFICATIONS.maxlen
               )
           return deleted_count
       except Exception as e:
           log_server(f"[PURGE ERROR] Failed to purge expired celebrations: {e}")
           return 0
   ```
2. **Invoke Purge Engine**:
   - In `check_and_trigger_celebrations(force=False)`:
     Call `purge_expired_celebrations()` right after computing `today_str`.
   - In `GET /api/notifications`:
     Call `purge_expired_celebrations()` before fetching `db_notifs`.
     Also apply defensive in-memory filter on `notifs` list for both regular engineers and admins!
3. **Verify with Python test**:
   - Create a dummy celebration notification with yesterday's timestamp/ID.
   - Run `purge_expired_celebrations()`.
   - Verify it is deleted and not returned by `GET /api/notifications`.
4. **Commit & Update Handoff**:
   - Update `SERVER_VERSION = "1.32.0"`.
   - Commit with message `feat(celebrations): hard purge expired celebration notifications past midnight IST v1.32.0 (Build 132)`.
   - Update `AGENT_HANDOFF.md`.
