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

## 4. Task 4 — Desktop Balanced 2-Column Architecture & Profile Accordion (`userPortal.js`, `mobile-shell.css`)

### A. Constrain Desktop Viewport Width:
- In `web/css/mobile-shell.css` / `web/user-portal.html`:
  - Constrain `#subview_profile`, `#panel_my_calendar`, `#panel_team_shifts` to an optimal `max-width: 1100px` on desktop (`@media (min-width: 900px)`), centered with `margin: 0 auto`.
  - Leave Month Roster at full width (`max-width: 1720px`) for maximum day column visibility.

### B. Profile & Settings 2-Column Balanced Grid:
- In `web/js/userPortal.js` (`renderProfileView()`):
  - On desktop (`@media (min-width: 900px)`), structure `.rx-land-grid-2` as a true 2-column grid (`grid-template-columns: 1.15fr 0.85fr; gap: 20px; align-items: start;`):
    - **Left Column**:
      1. Blue Profile Banner (Avatar, Name, Role badge, `@username`, Edit Profile button)
      2. Tenure & Birthday Cards (2-column sub-grid)
      3. Default Weekly Offs Strip
      4. Account Details Card (Domain Username, Registered Email with Copy button, Contact Phone)
    - **Right Column**:
      1. **Collapsed Appearance & Theme Accordion**:
         - Default state: Collapsed!
         - Header bar shows: `APPEARANCE & THEME` + summary pill (e.g. `System • Tory Blue`) + chevron toggle (`ph-caret-down`).
         - On click: smoothly toggles open/close, revealing 3-state theme modes and 6 brand color swatches.
      2. **About RosterX & Specs**:
         - Change `Application` label from `RosterX Web / iOS PWA` to strictly **`RosterX Web`**!
         - Specs: Version `v2.1.0`, Gateway `api.rosterx.online`, Timezone `Asia/Kolkata (IST)`.
      3. **Sign Out Button**:
         - Red accented Neumorphic button cleanly positioned at bottom of right column.

### C. My Calendar 2-Column Desktop Grid:
- In `web/js/userPortal.js` (`renderMyCalendar()`):
  - On desktop (`@media (min-width: 900px)`), structure `.rx-calendar-split` as a 2-column grid (`grid-template-columns: 1.35fr 1fr; gap: 22px; align-items: start;`):
    - **Left Column**: Month Selector (`← Month Year →`) + Weekday Headers + Calendar Day Grid (with today glow and shift pills).
    - **Right Column**:
      1. `Sync to Calendar` CTA button (full width of right column, prominent accent)
      2. `Shift Legend & Timings` Card (crisp vertical list of shift codes, names, and hours)
  - Result: The entire calendar and legend fit above the fold without any vertical scrolling!

---

## 5. Task 5 — Notification Timezone, Clear Animations & Dismiss Parity (`userPortal.js`, `mobile-shell.css`, Android)

### A. Android Client Serialization Parity (`RosterApiService.kt`):
- In `DismissNotificationRequest`:
  ```kotlin
  data class DismissNotificationRequest(
      @SerializedName("notification_id") val notificationId: String? = null,
      @SerializedName("id") val id: String? = null,
      @SerializedName("all") val all: Boolean? = null
  )
  ```
  Pass both `notificationId` and `id` so both backend schemas are 100% satisfied.

### B. Device Local / IST Timezone Display:
- In `web/js/userPortal.js` (`openNotificationsDialog()`):
  - Parse `n.created_at || n.timestamp` and format in device local time with IST indication (e.g. `19 Nov 2026, 05:56 AM IST` / device locale formatted cleanly via `Intl.DateTimeFormat`).
  - Zero raw `UTC` strings visible to users!

### C. Clear Animation & Clear All Cascade:
- In `web/css/mobile-shell.css`:
  - Add `@keyframes rxNotifSlideOut` (`opacity: 1 -> 0; transform: translateX(0) -> translateX(40px); height: collapse`).
  - `.rx-notif-dismissing { animation: rxNotifSlideOut 0.28s ease-out forwards; }`
- In `web/js/userPortal.js`:
  - Individual dismissal: apply `.rx-notif-dismissing`, wait 260ms, then remove from `this.notifications` and persist dismissal.
  - "Clear All": apply staggered slide-out animation to all notification elements with 40ms stagger, then show `"You're all caught up! No active notifications."` empty state.

---

## 6. Task 6 — Month Roster Grid Enlargement (Desktop & Mobile)

- In `web/css/mobile-shell.css`:
  - **Desktop (`min-width: 900px`)**:
    - `.rx-roster-table tbody td`: `min-width: 44px; height: 38px; padding: 5px 4px; font-size: 12px; font-weight: 800;`
    - `.rx-roster-table thead th:first-child`, `.rx-roster-table tbody th`: `min-width: 160px; font-size: 12px;`
    - Shift pills inside cells: larger padding (`4px 6px`), higher contrast, crystal clear readability.
  - **Mobile (`max-width: 899px`)**:
    - `.rx-roster-table tbody td`: `min-width: 40px; height: 36px; padding: 4px 3px; font-size: 11.5px;`
    - `.rx-roster-table thead th:first-child`, `.rx-roster-table tbody th`: `min-width: 140px;`

---

## 7. Verification, Version Bump & Mandatory Push
1. Bump version to **`v2.1.0` (Build 150)**:
   - `android/app/build.gradle.kts`: `versionName = "2.1.0"`, `versionCode = 150`
   - `server.py`: `SERVER_VERSION = "2.1.0"` (synced with Dasan)
2. Safe build & ADB install:
   - `cmd /c "cd android && safe_build.bat assembleDebug"`
   - `adb install -r android/app/build/outputs/apk/debug/app-debug.apk`
3. Verify on device & Web:
   - Verify desktop 2-column layout on Profile & Settings and My Calendar (zero vertical scrolling, optimal width).
   - Verify Appearance & Theme is collapsed by default and expands smoothly.
   - Verify "RosterX Web" application string.
   - Verify notification timestamps show IST / device local time, and clear animations slide out smoothly on single dismiss and Clear All.
   - Verify Month Roster grid cells are enlarged, highly legible, and crisp on both desktop and mobile.
   - Verify Android Calendar sync state is reflected on Web with target email and Re-Sync / Clear Month actions.
   - Verify Forgot Password modal has 100% WCAG contrast in Light and Dark mode.
   - Verify AI Assistant is 85dvh height with 3D bot header, chips, and typing indicator.
4. Commit with semantic message, update `AGENT_HANDOFF.md` + `android/AGENT_HANDOFF.md`, and **immediately run `git push origin main`**!
