# 📋 Execution Plan for Dasan (Backend & Core Orchestration)

**Status:** `READY FOR DASAN` (Sprint v1.33.0 - Build 137 Broadcast Title `@` Mention Personalization + Mandatory Auto-Push Sync)

---

## 1. Architectural Mandates from Gafoor (System Architect 🕶️)

Dasa! Nammude Broadcast Composer-il ippol Message Body-il maathramaanu `@name`, `@all`, and `@username` personalization work cheyyunnathu. User-nte puthiya demand: **Broadcast Title-ilum `@` mentioning full parity-ode work aakanam!**
Athupole nammude **new workspace policy** (updated in `GEMINI.md`): Verified commit kazhinjaal **mandatory `git push origin main`** run cheyyuka, so Render (`https://api.rosterx.online`) automatically sync aayi App & Backend version mismatch zero aakum!

### 🎯 Key Directives for Dasan:
1. **Unified Personalization Helper (`_personalize_broadcast_text`) in `server.py`**:
   - Avoid duplicating regexes across Title & Body. Create a clean helper in `POST /api/notifications/send` and `GET /api/notifications` that resolves:
     - `@name` / `{name}` / `{their name}` → recipient's **Full Name** (e.g. `Shefeeq K K`)
     - `{username}` / `@{username}` → recipient's username
     - `@all` / `@everyone` / `@team` → `@Everyone` (plain text) or styled badge (HTML email body)
     - `@<username>` → `@<Full Name>` via `name_by_user` / `name_by_id` lookup (plain text for Email Subject & FCM Push Title, styled badge or `@Full Name` in Email HTML `<h2>`)
2. **Apply Personalization to Broadcast `title` Across ALL 3 Channels**:
   - **Email (Resend)** inside `POST /api/notifications/send`:
     - **Email Subject**: Personalize `title` in plain text (`p_title_plain`) → `f"📢 [{priority.upper()}] {p_title_plain}"`.
     - **Email HTML `<h2>`**: Personalize `title` (`html_p_title` or `p_title_plain`) so the email heading is equally customized per recipient!
   - **FCM Push Notification** inside `POST /api/notifications/send`:
     - Personalize `title` per recipient (`p_title_plain`) → `f"📢 [{priority.upper()}] {p_title_plain}"`.
   - **In-App Notification Feed** inside `GET /api/notifications`:
     - When `!is_admin`: Personalize `item["title"]` for `@name`, `{name}`, `@all`, and `@username` using the caller's Full Name + engineer lookup map (exact parity with `item["raw_message"]`).
     - Keep original raw `title` intact in MongoDB `ATLAS_DB.notifications` (and for Admin/Manager history view) so Admins can see the raw template tokens while engineers see their personalized title!
3. **Version Bump & Mandatory Push**:
   - Update `SERVER_VERSION = "1.33.0"` in `server.py`.
   - Test Email subject/HTML, FCM title, and `GET /api/notifications` title resolution with a quick verification script.
   - Commit with semantic message, update `AGENT_HANDOFF.md`, and **run `git push origin main`** so Render deploys `v1.33.0` immediately!

---

## 2. Technical Blueprint for Dasan

### 📁 Primary File:
- `server.py`

### 🛠️ Implementation Checklist:
1. **In `POST /api/notifications/send` (`lines ~5160-5252`)**:
   - For **Email loop**:
     - Compute `p_title_plain` (replaces `@name`, `{name}`, `{username}`, `@all` → `@Everyone`, `@<username>` → `@<Full Name>`).
     - Compute `html_p_title` (HTML-safe or inline styled mention pill for `<h2>` heading).
     - Pass `f"📢 [{priority.upper()}] {p_title_plain}"` as email subject and `{html_p_title}` inside `<h2>`.
   - For **FCM Push loop**:
     - Compute `p_title_plain` per recipient using the exact same rules as `p_body`.
     - Pass `f"📢 [{priority.upper()}] {p_title_plain}"` as the FCM title.
2. **In `GET /api/notifications` (`lines ~3715-3735`)**:
   - Build a lightweight `name_by_user` lookup map (from `ATLAS_DB.engineers` or `DEFAULT_SERVER_ENGINEERS`).
   - For non-admin callers, resolve `@name` / `{name}` / `@all` / `@<username>` in **both** `item["title"]` and `item["raw_message"]`.
3. **Verify & Deploy**:
   - Run automated Python verification covering:
     - Title `"Urgent for @name: @shefeeqkk & @all please review"` → resolves properly per recipient in plain text, HTML, and `GET /api/notifications`.
   - Bump `SERVER_VERSION = "1.33.0"`.
   - Commit, update `AGENT_HANDOFF.md`, and **`git push origin main`**.
