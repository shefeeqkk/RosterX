# 📋 Execution Plan for Dasan (Backend & Core Orchestration)

**Status:** `COMPLETED` (Sprint v1.28.0 - Build 119 Celebration engine restored, missing import fixed, flexible date normalizer & GET /api/celebrations/today deployed and verified)

---

## 1. Architectural Mandates from Gafoor (System Architect 🕶️)

Dasa! Nammude team celebrations (Birthdays & Work Anniversaries) teammates-num manager-kkum ethathathinte root causes Gafoorka deep investigation nadathi kandupidichu vechittundu. Ippol backend engine full robust aakki set aakkenda responsibility Dasan-te aanu!

### 🔍 Root Causes Identified:
1. **Fatal `random` NameError Crash in `check_and_trigger_celebrations`**:
   - `server.py` line ~982-il `notif_id = f"notif_celeb_{int(now_time*1000)}_{random.randint(100, 999)}"` ennu call cheythittundu, pakshe `server.py`-il `import random` import cheyithirunnilla!
   - Ithumoolam ethoru celebration event detect cheythaalum (e.g. Nandua's anniversary today), execution instant aayi `NameError: name 'random' is not defined` crash aayi silent `try...except`-il poyi empty list `[]` aayi return aayirunnu!
   - Result: In-app notification insert aayilla, FCM push dispatch aayilla, teammates-kkum manager-kkum onnum poyilla!
2. **Brittle Date Format Parsing**:
   - Birthday & Join date check relied on `bday_raw.endswith(f"-{today_md}")` or `bday_raw[-5:] == today_md` where `today_md = MM-DD` (e.g. `11-16`).
   - Profile edit-ilo database-ilo dates `DD-MM-YYYY` (e.g. `16-11-1995` or `16/11/1995`) aayi enter cheythaal, `bday_raw[-5:]` gives `-1995`, which NEVER matches `11-16`!
3. **Missing Dedicated Celebrations Endpoint**:
   - App client-nu today's team celebrations direct aayi pull cheyyaan oru dedicated endpoint illayirunnu.

---

## 2. Scope of Work for Dasan

### 1. Fix Missing Import (`server.py`)
- Add `import random` at top of `server.py` alongside other standard library imports.

### 2. Bulletproof Celebration Date Normalizer (`server.py`)
- Implement a flexible helper function `parse_celebration_date(raw_date_str: str, now_ist: datetime) -> tuple[bool, int]`:
  - Strips leading/trailing spaces, replaces `/` and `.` with `-`.
  - Intelligently parses all standard formats:
    - `YYYY-MM-DD` (e.g., `2024-11-16`)
    - `DD-MM-YYYY` (e.g., `16-11-2024` or `16-11-1995`)
    - `MM-DD` (e.g., `11-16`)
    - `DD-MM` (e.g., `16-11` disambiguated against today's day/month)
  - Compares `(month, day)` cleanly with `(now_ist.month, now_ist.day)`.
  - For work anniversary, computes `years = now_ist.year - join_year`. If `years >= 1`, flags `is_anniversary = True` and returns `years`.

### 3. Overhaul `check_and_trigger_celebrations(force=False)`
- Use the robust date normalizer for both `birthday` and `join_date` / `joinDate`.
- Keep the idempotency key `cid = f"celeb_{today_str}_{uname}_{event_type}"` to ensure events trigger only once per day.
- Create broadcast notification in `ATLAS_DB.notifications` with:
  - `"target": "everyone"`
  - `"target_type": "everyone"`
  - `"priority": "high"`
  - `"send_in_app": True`
  - `"send_push": True`
- Dispatch high-priority FCM push to all registered devices (`all_fcm_tokens`) with payload `{"target_user": "everyone", "type": "celebration"}`.

### 4. Implement Dedicated Endpoint: `GET /api/celebrations/today`
- Route: `GET /api/celebrations/today`
- Authenticated endpoint (requires valid token, or accessible by logged-in users & managers).
- Triggers `check_and_trigger_celebrations()` if not checked in current throttle window.
- Scans active engineers (excluding resigned / system admin accounts).
- For each engineer celebrating today (Birthday, Work Anniversary, or Both):
  - Returns:
    ```json
    {
      "celebrations": [
        {
          "username": "nandua",
          "name": "Nandu A",
          "type": "anniversary",
          "years": 2,
          "photo": "...",
          "phone": "9876543210",
          "title": "2nd Work Anniversary",
          "greeting_text": "Happy 2nd Work Anniversary, Nandu A! 🏆✨ Thank you for your dedication to the team!"
        }
      ]
    }
    ```
  - Unmasks phone number so teammates can launch WhatsApp or Call directly.

### 5. Version Bump & Verification
- Update `SERVER_VERSION = "1.28.0"` in `server.py`.
- Run verification script in python:
  - Verify `check_and_trigger_celebrations(force=True)` runs with zero errors and detects `nandua`'s 2nd anniversary today (`2024-11-16` -> `2026-11-16`).
  - Verify `GET /api/celebrations/today` returns the celebration payload with unmasked phone number.
- Commit changes and update `AGENT_HANDOFF.md`.
