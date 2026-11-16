# 📋 Execution Plan for Dasan (Backend & Core Orchestration)

**Status:** `READY FOR DASAN` (Sprint v1.28.0 - Build 119)

---

## 1. Architectural Mandates from Gafoor (System Architect 🕶️)

Dasa! Nammude team celebrations (Birthdays & Work Anniversaries) delivery engine-il 2 major issues Gafoorka solve cheythu blueprint ready aakki:
1. Teammates-num manager-kkum celebrations varumbol athu **3rd person team announcement** aayi varanam (not speaking as if the reader is the celebrant).
2. Anniversary notification text-il "completing 2nd with our team" ennulla grammar bug maatti proper "completing 2 years with our team" aakkanam.
3. Teammates-nu instant aayi WhatsApp or Call vazhi wish cheyyaan vendi `celebration_phone` notification doc-il incorporate cheyyanam.

---

## 2. Scope of Work for Dasan

### 1. Fix Missing Import (`server.py`)
- Add `import random` at the top of `server.py`.

### 2. Bulletproof Date Normalizer (`server.py`)
- Implement `parse_celebration_date(raw_date_str: str, now_ist: datetime) -> tuple[bool, int]`:
  - Handles `YYYY-MM-DD`, `DD-MM-YYYY`, `DD/MM/YYYY`, `YYYY/MM/DD`, `MM-DD`, `DD-MM`.
  - Accurately checks `(month, day) == (now_ist.month, now_ist.day)`.
  - For anniversary, computes `years = now_ist.year - join_year`.

### 3. Overhaul `check_and_trigger_celebrations(force=False)` & 3rd-Person Phrasing
- In `server.py`:
  - Calculate `yr_ord` (e.g. `2nd`) and `yr_label = f"{yr} years" if yr > 1 else "1 year"`.
  - **Work Anniversary**:
    - `title`: `f"Happy {yr_ord} Work Anniversary, {full_name}! 🏆"`
    - `msg`: `f"Today {full_name} completes {yr_label} of operational excellence with our team! Join us in celebrating this milestone and send your warm congratulations! 🥂🎊✨"`
  - **Birthday**:
    - `title`: `f"Happy Birthday, {full_name}! 🎂"`
    - `msg`: `f"Today is {full_name}'s birthday! Join the entire RosterX team in wishing them joy, vibrant health, and continued success! Tap to send wishes! 🎉🎈🥳"`
  - **Both**:
    - `title`: `f"Double Celebration: Birthday & {yr_ord} Anniversary for {full_name}! 🎂🏆"`
    - `msg`: `f"Today is doubly special for {full_name}! Join us in wishing them a very Happy Birthday and celebrating their {yr_ord} Work Anniversary with RosterX! Tap to send wishes! 🎉🎈🥂"`
  - Add `celebration_phone` to the notification dictionary:
    `"celebration_phone": str(eng.get("phone") or eng.get("phoneNumber") or eng.get("contact") or "").strip()`
  - Send high-priority FCM push with `{"target_user": "everyone", "type": "celebration"}`.

### 4. Dedicated Endpoint: `GET /api/celebrations/today`
- Returns today's active team celebrations with unmasked phone number, photo, celebration type, name, username, years, and 3rd-person greeting text template.

### 5. Patch Existing Today's Celebration Notification in MongoDB
- Update existing notification for `nandua` (`celeb_2026-11-16_nandua_anniversary`) with:
  - Corrected 3rd-person text (`Today Nandu Anthikat completes 2 years of operational excellence with our team!...`).
  - Corrected `celebration_phone` (unmasked).

### 6. Version Bump & Verification
- Update `SERVER_VERSION = "1.28.0"` in `server.py`.
- Run verification script in python:
  - Test `check_and_trigger_celebrations(force=True)`.
  - Test `GET /api/celebrations/today`.
- Commit changes and update `AGENT_HANDOFF.md`.
