# 📋 Execution Plan for Vijayan (UI Specialist — Android & Web)

**Status:** `READY FOR VIJAYAN` (Sprint v2.1.0 - Build 150 Android-Web Calendar Sync Parity + Forgot Password Visibility Overhaul + Lively 1:1 AI Assistant)

---

## 🔐 Mandatory Production Rule from Gafoor (System Architect 🕶️)
> **Public GitHub Repo + Auto-Deploying Render Production Server:**
> - **ZERO hardcoded personal names, usernames, emails, phone numbers, passwords, or secrets** anywhere in HTML, CSS, JS, Kotlin, comments, or placeholders.
> - **Mandatory `git push origin main`** immediately after verification so Render (`https://api.rosterx.online` / `https://rosterx.online`) deploys immediately!

---

## 1. Task 1 — Calendar Sync Parity Between Android & Web (`CalendarSyncDialog.kt` & `calendarSync.js`)

### A. Android App Side (`CalendarSyncDialog.kt`, `RosterApiService.kt`):
1. **Report Sync to Server on Successful Google Calendar Sync**:
   - In `CalendarSyncDialog.kt` when `ShiftCalendarExporter.syncMonthToCalendar(...)` succeeds:
     - Get chosen calendar account name (`cal.accountName`, e.g. `user@gmail.com`).
     - Launch coroutine calling `apiService.saveCalendarSyncStatus(CalendarSyncRequest(month = "$year-${pad2(month)}", count = syncedCount, target = cal.accountName, synced = true))`.
   - When user taps **Clear Month** and deletion succeeds:
     - Call `apiService.clearCalendarSyncStatus(CalendarSyncClearRequest(month = "$year-${pad2(month)}"))`.
2. **DTO & Retrofit Interface**:
   - Add `saveCalendarSyncStatus` and `clearCalendarSyncStatus` to `RosterApiService.kt` with clean data classes.

### B. Web UI Side (`web/js/calendarSync.js`):
1. **Reflect Target Calendar & Sync Status (1:1 Android Parity)**:
   - On opening the modal, fetch `GET /api/users/calendar-sync?month=${year}-${pad2(month)}` (or read from cached `currentUser.calendar_sync`).
   - If synced:
     - Render **`TARGET CALENDAR`** section with **`Synced (N)`** green pill badge!
     - Display target email (e.g. `● user@gmail.com ✔`).
     - Change primary CTA to **`[ 🔄 Re-Sync N Days ]`**.
     - Add bottom row: **`[ 📤 Export .ics ]`** | **`[ 🗑️ Clear Month ]`** (calling `/api/users/calendar-sync/clear` on confirm).
   - If not synced:
     - Show clean export preview + reminder picker + primary CTA: **`[ 📅 Add N Days to Calendar (.ics) ]`** + 1-tap **Google Calendar Web Add** option.

---

## 2. Task 2 — Complete Visibility & Light/Dark Mode Overhaul (`login.html` & Modals)

### A. Forgot Password Modal (`web/login.html`):
- **Root Cause**: `#forgotPasswordModal` used hardcoded dark Tailwind classes (`text-white`, `text-slate-300`, `bg-slate-900/80`) while `.glass-card` switched to pure white in light mode (`html:not(.dark)`), making labels and instructions completely invisible!
- **Overhaul**:
  - Convert `#forgotPasswordModal` to use the unified RosterX Neumorphic system with adaptive CSS variables:
    - Card: `background: var(--nm-surface)`, `border: 1px solid var(--nm-border)`, `box-shadow: var(--nm-shadow-floating)`
    - Title & Headings: `color: var(--nm-text)` (crisp dark slate in light mode, crisp bright white in dark mode)
    - Subtitles & Labels: `color: var(--nm-text-muted)` with bold weight and minimum 4.5:1 contrast
    - Inputs (`#resetIdentifierInput`, `#otpInput`, `#newPasswordInput`, `#confirmPasswordInput`):
      - `background: var(--nm-input-surface)`
      - `border: 1px solid var(--nm-border)`
      - `color: var(--nm-text)`
      - `box-shadow: var(--nm-shadow-inset-subtle)`
      - Clean focus ring with `var(--nm-accent)`
  - Fix Step 1 and Step 2 alert boxes, submit buttons, and helper texts for flawless contrast in BOTH Light and Dark mode.

### B. Light/Dark Mode Contrast Audit Across All Web Modals:
- Audit and ensure 100% WCAG contrast across:
  1. `web/login.html` (Main Login Card, Demo Credentials Pill, Forgot Password Modal)
  2. `web/user-portal.html` (Profile Edit Modal, Credential Tracker, Audit Logs Modal, Calendar Sync Modal)
  3. `web/index.html` (Admin Team Edit Modal, Broadcast Templates Dialog, Roster Restore Modal)

---

## 3. Task 3 — Lively AI Assistant with Full Space Utilization (`web/js/userPortal.js` & `mobile-shell.css`)

### A. Responsive Space Utilization:
- In `web/css/mobile-shell.css`:
  - For `#rxAiChatDialog`:
    - Desktop: `width: min(94vw, 560px)`, `height: min(85dvh, 760px)`, `border-radius: 24px`.
    - Mobile (`max-width: 640px`): True **Bottom Sheet** docked at the bottom (`width: 100%`, `height: 85dvh`, `border-radius: 28px 28px 0 0`, `margin-bottom: 0`, top drag handle).
    - Chat message list: `flex: 1`, `overflow-y: auto`, `min-height: 0` so it expands to occupy all available vertical space! Zero wasted space!

### B. 1:1 Android Feature Parity:
- **Header**:
  - 3D Bot icon badge (`ph-robot` with accent gradient glow)
  - Title `"RosterX Assistant"`
  - Live status indicator: pulsating green dot + `"Grounded Gemini • Live Atlas NOC"`
  - Action buttons: **Clear Chat** (`ph-trash` icon) + **Close** (`ph-x` icon)
- **Quick Prompt Chips (Horizontal Scrollable Strip)**:
  - Add 1-tap quick prompt pills:
    - `⚡ On duty now?`
    - `🌙 NS tomorrow?`
    - `📅 My shift today?`
    - `📋 Duties tomorrow?`
    - `🏖️ Off tomorrow?`
    - `🌙 Next night shift?`
    - `🏖️ Rest days count?`
  - Tapping a chip immediately dispatches the prompt.
- **Chat Experience**:
  - Assistant bubbles: Bot avatar + clean Neumorphic bubble + Markdown rendering (bold, lists, shift code badges like `NS`, `FS`, `OFF`).
  - User bubbles: Right-aligned accent bubble.
  - Lively Thinking Indicator: 3 animated pulsing/bouncing dots.
  - Auto-scroll to latest message on new response.
  - Input: Neumorphic sunken basin with placeholder `"Ask NOC schedules or active shifts..."` + Send button + Enter key submission.

---

## 4. Verification, Version Bump & Mandatory Push
1. Bump version to **`v2.1.0` (Build 150)**:
   - `android/app/build.gradle.kts`: `versionName = "2.1.0"`, `versionCode = 150`
   - `server.py`: `SERVER_VERSION = "2.1.0"` (synced with Dasan)
2. Safe build & ADB install:
   - `cmd /c "cd android && safe_build.bat assembleDebug"`
   - `adb install -r android/app/build/outputs/apk/debug/app-debug.apk`
3. Verify on device & Web:
   - Sync calendar on Android -> Open Web UI Calendar Sync modal -> verify `Synced (30)` badge and target calendar appear on Web!
   - Open Forgot Password modal in `login.html` in Light and Dark mode -> verify crisp contrast, no invisible white text, and beautiful Neumorphic inputs.
   - Open AI Assistant on mobile and desktop -> verify full 85dvh height, 3D bot header, quick chips, animated typing dots, and Markdown bubbles!
4. Commit with semantic message, update `AGENT_HANDOFF.md` + `android/AGENT_HANDOFF.md`, and **immediately run `git push origin main`**!
