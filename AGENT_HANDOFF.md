# 🕵️‍♂️ Dasan (Antigravity Agent 1) ⇄ 🕵️ Vijayan (Antigravity Agent 2) Handoff Bridge

> **"Gafoorka lays out the master architecture; Dasan orchestrates backend & builds; Vijayan crafts Jetpack Compose UI & client features!"**  
> *Architectural Directive from Gafoor (🕶️):* **2026-11-19 12:45 IST** (Target Sprint: **v2.1.0 - Build 150**)

---

## 🕵️ Vijayan Dispatch to Dasan & Gafoorka (v2.1.0 - Build 150 — Web & Mobile Parity, Lively AI Assistant & Desktop 2-Column Grid Complete):
> *"Dasaa! Gafoorka! Vijayan UI sprint **`v2.1.0` (Build 150)** kothu kothi spotless aayi theerthu! 🕵️✨
>
> ### 🎨 Delivered UI Enhancements:
> 1. **Calendar Sync Parity (`web/js/calendarSync.js` & Android `CalendarSyncDialog.kt`)**:
>    - Web `openCalendarSyncModal` queries live `/api/users/calendar-sync?month=YYYY-MM`.
>    - When synced, displays `TARGET CALENDAR` card with green `Synced (N)` badge, target account, Re-Sync CTA, and Clear Month action.
>    - Exporting `.ics` or tapping Re-Sync automatically registers sync state to `POST /api/users/calendar-sync`.
>    - Full 1:1 parity with Android native calendar sync!
> 2. **Forgot Password Visibility Overhaul (`web/login.html`)**:
>    - Overhauled `#forgotPasswordModal` with Neumorphic adaptive CSS variables (`var(--nm-surface)`, `var(--nm-text)`, `var(--nm-text-muted)`, `var(--nm-input-surface)`).
>    - Flawless contrast in BOTH Light and Dark mode for titles, labels, inputs, alerts, and back links!
> 3. **Lively AI Assistant with Full Space Utilization (`web/js/userPortal.js` & `mobile-shell.css`)**:
>    - True Bottom Sheet docked to bottom on mobile (85dvh) with top drag handle; centered 560x760 modal on desktop.
>    - Header with 3D Bot icon badge, pulsing green live NOC indicator, Clear Chat, and Close buttons.
>    - Horizontal scrollable quick chips strip (`⚡ On duty now?`, `🌙 NS tomorrow?`, `📅 My shift today?`, `📋 Duties tomorrow?`, `🏖️ Off tomorrow?`, `🌙 Next night shift?`, `🏖️ Rest days count?`).
>    - Markdown-style bold and shift badges (`[NS]`, `[FS]`, etc.) + 3-dot bouncing typing indicator.
> 4. **Desktop Balanced 2-Column Architecture & Profile Accordion**:
>    - Constrained desktop width to 1100px max-width centered.
>    - Profile & Settings: Left column for user details; Right column with collapsed Appearance & Theme accordion (with session persistence), "RosterX Web" application spec, and Sign Out.
>    - My Calendar: 2-column layout with month grid on left and Sync CTA + Shift Legend on right.
> 5. **Notification Parity & Slide-Out Animations**:
>    - Timezone formatted in local IST time (no raw UTC strings).
>    - Slide-out dismissal animation (`rxNotifSlideOut`) for single dismiss and staggered cascade for Clear All.
> 6. **Month Roster Grid Enlargement**:
>    - Scaled cell dimensions and paddings for optimal tap targets and crisp visibility on both mobile and desktop.
> 7. **Build & Version Sync**:
>    - Full Android `safe_build.bat assembleDebug` succeeded with 0 errors!
>    - Version bumped to `v2.1.0` (Build 150) in `android/app/build.gradle.kts`, `server.py`, and `web/sw.js` (`rosterx-pwa-v2.1.0`)."*

---

## 🕵️‍♂️ Dasan Dispatch to Vijayan & Gafoorka (v2.1.0 - Build 150 — Cloud-Backed Calendar Sync Endpoints & Notification Dismissal Parity):
> *"Vijaya! Gafoorka! Dasan backend sprint **`v2.1.0` (Build 150)** 100% spotless aayi finish cheythu live Render production deploymentilottu push cheythu macha! 🕵️‍♂️🔥
>
> ### 🚀 Key Implementations Delivered:
> 1. **Cloud-Backed Calendar Sync Endpoints (`server.py`)**:
>    - **`POST /api/users/calendar-sync`**: Saves `{ month: "YYYY-MM", count, target, synced: true }` into MongoDB Atlas (`calendar_sync.{month}`) and updates in-memory fallback.
>    - **`GET /api/users/calendar-sync`**: Returns all synced months `{ success: true, calendar_sync: {...} }` or specific month status via `?month=YYYY-MM`.
>    - **`POST /api/users/calendar-sync/clear`**: Unsets `calendar_sync.{month}` (or all syncs if no month/all: true).
>    - **Profile Payload Integration**: Included `calendar_sync` map in `GET /api/auth/me`, `POST /api/auth/login`, and `GET /api/engineers` responses so clients have sync state cached immediately on startup.
> 2. **Notification Dismissal Sync Parity (`server.py`)**:
>    - **`POST /api/notifications/dismiss`**: Accepts both `notification_id` and `id` parameter names.
>    - **Comprehensive `dismiss_all`**: Sweeps across broadcast notices, celebrations, and credential alerts into `dismissed_notifications` on `ATLAS_DB.engineers`.
>    - **Zero-Latency Reconciliation**: Automatically returns the full updated `dismissed_notification_ids` list in the response so Android and Web can update their badge and UI without an extra GET query!
> 3. **Android Client Kotlin Parity Check**:
>    - Fixed unreferenced `viewModel` in `EngineerPortalScreen.kt` by passing `onSyncRecorded` and `onSyncCleared` lambdas to `MyCalendarView`.
>    - Verified with `safe_build.bat compileDebugKotlin` → **BUILD SUCCESSFUL**!
> 4. **Version Bump & Zero-PII Verification**:
>    - Bumped `SERVER_VERSION = "2.1.0"` in `server.py` and verified `android/app/build.gradle.kts` matches `versionName = "2.1.0"`, `versionCode = 150`.
>    - Zero hardcoded PII/secrets or personal names in code or comments.
>
> Vijaya, backend and routes are live and 100% ready for your Web UI and Android Compose tests! Over to you for the final touch!"*

---

## 🕶️ 📢 Gafoor's Master Sprint Blueprint (v2.1.0 - Build 150 — Cloud-Backed Calendar Sync Parity, Desktop Balanced Layout, Notifications Parity & Lively 1:1 AI Assistant):
> *"Dasa, Vijaya! User `/grill-me` session-il nammude next MAJOR Sprint **`v2.1.0` (Build 150)** 100% lock aakki vechittundu:
>
> ### 🎯 Core Objectives:
> 1. **Cross-Device Calendar Sync Parity (Android ⇄ Web)**:
>    - **Dasan**: Add `POST /api/users/calendar-sync`, `GET /api/users/calendar-sync`, and `POST /api/users/calendar-sync/clear` backed by MongoDB Atlas (`calendar_sync.{month}`).
>    - **Vijayan (Android)**: When Google Calendar sync succeeds in `CalendarSyncDialog.kt`, report `{ month, count, target: cal.accountName }` to `POST /api/users/calendar-sync`. On Clear Month, call `/clear`.
>    - **Vijayan (Web)**: In `web/js/calendarSync.js`, fetch cloud sync state and render **`TARGET CALENDAR [Synced (N)]`** badge, target account email, **`Re-Sync N Days`** button, and **`Clear Month`** button!
> 2. **Desktop Stretched View Fix & 2-Column Balanced Architecture**:
>    - **Vijayan**: Constrain container width (`max-width: 1100px`) for standard views.
>    - **Profile & Settings**: Left = Header Banner, Tenure/Birthday, Default Weekly Offs, Account Details; Right = Collapsed Appearance & Theme Accordion (summary pill `System • Tory Blue` with chevron toggle), About Specs (updated to strictly `RosterX Web`), Sign Out.
>    - **My Calendar**: Left = Month Selector + Day Grid; Right = Sync to Calendar CTA + Shift Legend & Timings side-by-side (zero vertical scrolling!).
> 3. **Notification Parity, Device Local/IST Timezone & Clear Animations**:
>    - **Dasan**: In `POST /api/notifications/dismiss`, accept both `notification_id` and `id`, and ensure `dismiss_all` sweeps all current in-feed alerts (celebrations + credential alerts) into `ATLAS_DB.engineers`.
>    - **Vijayan (Android)**: In `DismissNotificationRequest`, serialize both `notification_id` and `id`.
>    - **Vijayan (Web)**: Format notification timestamps using device local time with IST fallback (`19 Nov 2026, 05:56 AM IST`), and add smooth slide/fade-out animations for single dismiss and cascading Clear All.
> 4. **Month Roster Grid Enlargement (Web Desktop & Mobile)**:
>    - **Vijayan**: Enlarge table cells (Desktop: `44px` min-width, `38px` row height, `12px` bold font, `160px` engineer column; Mobile: `40px` min-width, `36px` row height, `11.5px` font) with prominent shift pills.
> 5. **Complete Visibility & Contrast Overhaul (`login.html` & All Modals)**:
>    - **Vijayan**: Rebuild `#forgotPasswordModal` using adaptive Neumorphic CSS variables (`--nm-surface`, `--nm-input-surface`, `--nm-text`, `--nm-border`) with 100% WCAG contrast in BOTH Light and Dark modes.
> 6. **Lively AI Assistant with Full Space Utilization (`userPortal.js` & `mobile-shell.css`)**:
>    - **Vijayan**: Full `85dvh` height with 3D bot header, pulsating live NOC status, horizontal quick prompt chips (`⚡ On duty now?`, `🌙 NS tomorrow?`, etc.), animated 3-dot typing indicator, and Markdown response rendering!
>
> ### 🔀 Execution Flow:
> - **Dasan**: Run `plan_dasan.md` -> Backend endpoints + bump `SERVER_VERSION = "2.1.0"` -> Commit & `git push origin main`.
> - **Vijayan**: Run `plan_vijayan.md` -> Android & Web UI implementation -> safe_build + ADB install + bump `2.1.0` (Build 150) -> Commit & `git push origin main`.
>
> **Strict Zero-PII Policy & Mandatory Push Discipline apply!** Check `plan_dasan.md` & `plan_vijayan.md`!"*

---

## 🎨 🕵️ Vijayan Hotfix Dispatch to Dasan & Gafoorka (v2.0.2 - Build 145 — Username-First Roster Matching & Dashboard-Only AI Bot):
> *"Dasa! Gafoorka! Hotfix **`v2.0.2` (Build 145)** complete aayi verify + Android safe-build + ADB sync cheythu `origin/main`-il push cheythu macha! 🎨✨
> - **Username-First Roster Row Matching (`web/js/userPortal.js`, `server.py`)**: Upgraded `getRosterMapForEngineer()` to match `eng.username` across all canonical row keys (`r.username`, `r.engineer_id`, `r.engineerId`, `r.id`, and `eng_<slug>`) first, with exact name and safe first-name/prefix fallbacks matching Android `RosterRepositoryImpl.kt`. Also updated `GET /api/roster/<month>` in `server.py` to cross-check live `include_in_roster: true` from `ATLAS_DB.engineers` so operational admins are never stripped from roster responses.
> - **AI Assistant Bot Only on Dashboard (Landing) Tab (Web + Android 1:1 Parity)**: Hid floating 3D AI Bot on all non-Dashboard tabs (`Team Shifts`, `My Calendar`, `Month Roster`, `Broadcast`, `More`, and subviews) across `web/js/userPortal.js`, `EngineerPortalScreen.kt` (`showPortalChrome && selectedTab == 0`), and `AdminMainScreen.kt` (`showAdminChrome && selectedTab == 0`).
> - **Cache Bust & Version Sync**: Bumped `sw.js` (`rosterx-pwa-v2.0.2`), `?v=2.0.2` imports, `android/app/build.gradle.kts` (`145` / `2.0.2`), and `server.py` (`2.0.2`)."*

---

## 🎨 🕵️ Vijayan Hotfix Dispatch to Dasan & Gafoorka (v2.0.1 - Build 144 — Web Portal Sync, Roster Normalization, WRK/NS/OFF Counters & Bell Parity):
> *"Dasa! Gafoorka! Hotfix **`v2.0.1` (Build 144)** 100% spotless aayi implement cheythu, syntax + unit + live HTTP + Zero-PII + Android safe-build & ADB verify cheythu `origin/main`-il push cheythu macha! 🎨🚑🔥
> - **Task 1 — Sync Crash & Credential Tracker (`userPortal.js`, `credentialTracker.js`, `myTeamView.js`)**: Unwrapped nested `payload?.data?.trackers` & `data?.data?.logs` arrays safely inside isolated try/catch guards (zero `trackers.filter is not a function` aborts!), and added `hideBackButton: true` so embedded subviews (`Credential Tracker` & `My Team`) display a single clean header with zero duplicate back arrows.
> - **Task 2 — Roster Normalization & Operational Admin Inclusion (`userPortal.js`)**: Normalized both 0-indexed shift arrays (`shifts[0]` → Day 1) and 1-indexed maps from `doc.engineers` / `doc.roster`, respected `include_in_roster === true` BEFORE role checks (so operational admins stay on roster & in My Team), and applied exact Android `sortEngineersByWeekOffs` ordering.
> - **Task 3 — Blank Unpublished Cells + Right-Side `WRK` / `NS` / `OFF` Counters (`userPortal.js`)**: Removed hardcoded `|| 'OFF'` fallbacks across Month Roster, My Calendar, Dashboard, and Team Shifts — unassigned/upcoming months (like December 2026) render clean transparent blank cells + `"No roster published for <Month Year> yet"` pill, and Month Roster now includes right-side NOC summary columns **`WRK`**, **`NS`**, **`OFF`** with aligned coverage footer rows.
> - **Task 4 — Unread Bell Badge Parity (`userPortal.js`)**: Subtracted `dismissed_notification_ids` alongside expired celebration auto-sweep so Web Bell badge matches Android unread count 1:1.
> - **Task 5 — PWA Cache Bust (`web/sw.js`, `user-portal.html`, `index.html`, `app.js`)**: Bumped `CACHE_NAME = 'rosterx-pwa-v2.0.1'`, switched `.js`/`.css` to Network-First with offline cache fallback so installed iOS & Desktop PWAs pick up hotfixes instantly, and versioned script/CSS URLs to `?v=2.0.1`.
> - **Version Sync**: Bumped `android/app/build.gradle.kts` (`144` / `2.0.1`) & `server.py` (`SERVER_VERSION = "2.0.1"`), safe-built & ADB-verified cleanly!"*

---

## 🕶️ 🚑 Gafoor's Hotfix Blueprint (v2.0.1 - Build 144 — Web Portal Sync, Credential Tracker, Month Roster & Bell Parity):
> *"Vijaya! `v2.0.0` Web Portal-il 6 critical bugs diagnose cheythu `plan_vijayan.md`-il surgical fix blueprint lock aakki vechittundu (Dasan = `IDLE / NO ACTIVE TASK`, backend healthy!):
> 1. **Fix `Sync warning: trackers.filter is not a function`** (`web/js/userPortal.js`, `web/js/credentialTracker.js`): Unwrap nested `payload.data.trackers` array properly (prevents sync crash that blocked Dashboard, My Calendar & Credential Tracker!). Remove duplicate `← Credential Tracker` header when embedded in subview.
> 2. **Normalize 0-Indexed Shift Arrays & `include_in_roster`** (`web/js/userPortal.js`): Convert array `e.shifts[i]` → 1-indexed `"1".."31"` map (fixes off-by-one shift corruption!) and support both `doc.engineers` / `doc.roster` + operational admins with `include_in_roster: true`.
> 3. **Month Roster Blank Unpublished Cells + Right-Side `WRK` / `NS` / `OFF` Counters**: Remove hardcoded `|| 'OFF'` fallback in `renderMonthRoster()` so unassigned/upcoming months (like December 2026) render clean transparent blank cells + `"No roster published for <Month Year> yet"` banner, and add the 3 right-side NOC stat columns (`WRK`, `NS`, `OFF`) matching Android 1:1.
> 4. **Bell Badge Parity**: Subtract `dismissed_notification_ids` from `this.notifications` (fixes `45` → actual unread count).
> 5. **PWA Cache Bust**: Bump `web/sw.js` cache version + `?v=2.0.1` script imports so installed PWAs immediately load the fix!
> 6. **Version Sync & Push**: Bump to `v2.0.1` (Build 144), verify cleanly, commit & **`git push origin main`**!"*

---

## 🕶️ 📢 Gafoor's Master Sprint Blueprint (v2.0.0 - Build 143 — Unified `web/` iOS PWA & 1:1 Android Feature Parity):
> *"Dasa, Vijaya! User `/grill-me` session-il nammude next MAJOR release **`v2.0.0` (Build 143)** 100% lock aakki vechittundu — `admin-web/` → **`web/`** rename + full **Installable iOS PWA** + **1:1 Android 5-Tab & Feature Parity** across Engineer, Manager, and Admin!
>
> ### 🔀 Strict Execution Order (Zero Merge Conflicts):
> 1. **STEP 1 — Dasan (`plan_dasan.md`)**:
>    - `git mv admin-web web` + update `server.py` (`WEB_DIR`), `README.md`, `.agents/skills/rosterx-ui/SKILL.md`.
>    - Add iOS PWA MIME/header support in `server.py` (`.webmanifest`, `sw.js`, `.ics`) + verify `/open/*` hash redirects and Credential Tracker / Profile Photo / Manager Broadcast / Zero-Knowledge Team APIs.
>    - Test `server.py`, commit & **`git push origin main`** so `web/` is live for Vijayan!
> 2. **STEP 2 — Vijayan (`plan_vijayan.md`)**:
>    - Build Installable iOS PWA shell (`manifest.webmanifest`, `sw.js`, Apple safe-area/notch tags) + **6 Android Brand Color Palettes**.
>    - Remove unwanted **Swap Request** tabs from BOTH portals (preserving `SW*` shift codes in roster grids).
>    - Build adaptive Mobile/iOS Neumorphic Shell: **5-tab Fluid Neumorphic Bottom Nav** (portrait) + **slim `42px` Top Bar & Left Vertical Nav Rail** (landscape, Month Roster full-screen untouched!) + keep rich Desktop layout.
>    - Enforce **1:1 Android 5-tab role parity** (Engineer, Manager, Admin) & add all missing Android features: **My Calendar + iOS `.ics` Sync to Calendar modal**, **Month Roster**, **More Hub → Server Credential Tracker & My Team Directory**, **Team Celebrations & Notification Drawer (`@` mentions + auto-sweep)**, **Profile Tenure/Birthday/Remove Photo**, **Admin Broadcast Title `@` mentions & Zero-Knowledge Team**.
>    - Bump to **`v2.0.0` (Build `143`)**, verify across iPhone portrait/landscape & desktop, commit & **`git push origin main`**!
>
> **Strict Zero-PII & Mandatory `git push origin main` apply!** Check `plan_dasan.md` & `plan_vijayan.md`!"*

---

## 🎨 🕵️ Vijayan UI Studio Dispatch to Dasan & Gafoorka (v2.0.0 - Build 143 — Unified `web/` Installable iOS PWA & 1:1 Android Parity):
> *"Dasa! Gafoorka! MAJOR Sprint **`v2.0.0` (Build 143)** — Unified `web/` Installable iOS PWA + 1:1 Android Feature Parity — 100% complete aayi live HTTP + RFC-5545 `.ics` + Zero-PII + Android safe-build verify cheythu `origin/main`-il push cheythu macha! 🎨📱🍏🔥
> - **Installable iOS PWA Shell**: Added `web/manifest.webmanifest`, `web/sw.js` (network-first for `/api/*` & HTML, stale-while-revalidate for static assets), `web/js/pwaHelper.js` (SW registration, dismissible iOS Safari "Add to Home Screen" banner, and frosted-blur `<dialog closedby="any">` with Safari bounding-rect backdrop-click fallback), and full iOS safe-area/Dynamic Island meta tags across `login.html`, `user-portal.html`, and `index.html`.
> - **6 Android Brand Palettes + 3-State Theme**: Upgraded `web/css/tokens.css` & `web/js/themeController.js` with all 6 Android palettes (`tory_blue`, `crimson_coral`, `emerald_green`, `indigo_violet`, `sunset_amber`, `ocean_cyan`), downward-only floating dialog shadows (zero white outer halo), `<meta name="color-scheme">` CSS reflection, and cross-tab sync.
> - **Adaptive Mobile Portrait + Landscape Rail Shell**: Created `web/css/mobile-shell.css` & `web/js/mobileNavShell.js` delivering Android's **5-tab Fluid Neumorphic Bottom Dock** in portrait, **ultra-slim `42px` Top Bar + Left Vertical `FluidNeumorphicNavRail`** (`72px`) in mobile landscape, dedicated **full-screen landscape Month Roster / Rosters** (`body.rx-fullscreen-roster`), and full desktop layout preservation.
> - **Exact 1:1 Android Role Parity & Swap Removal**:
>   - Removed unwanted **Swap Request** tabs & modals from BOTH portals (`user-portal.html` & `index.html`) while keeping `SW*` shift codes 100% intact inside roster grids.
>   - `admin` → `index.html` (5 tabs: `Dashboard`, `Register`, `Rosters`, `Broadcast`, `More`); `manager` → `user-portal.html` Manager Mode (5 tabs: `Dashboard` Radar + Health, `Team Shifts`, `Month Roster`, `Broadcast`, `More`); `engineer`/`apprentice` → `user-portal.html` (5 tabs: `Dashboard`, `Team Shifts`, `My Calendar`, `Month Roster`, `More`).
> - **All Missing Android Features Added to `web/`**:
>   - **`web/js/calendarSync.js`**: Personal **`Sync to Calendar`** modal (triggered strictly from `My Calendar`), generating RFC-5545 `.ics` with timed Busy IST working shifts + `VALUE=DATE` All-Day Free `OFF`/`L`/`SW*` + configurable `VALARM` (`None`/`15m`/`30m`/`1h`) for native 1-tap Apple Calendar import on iOS.
>   - **`web/js/credentialTracker.js`**: Full Server Credential Tracker mounted inside `More` Hub for ALL 3 roles (KPI cards, search/filter, server-grouped cards, 1-tap Renew Date picker with past/today guard, Add/Edit/Delete + Audit Logs for Admin/Manager).
>   - **`web/js/myTeamView.js`**: Searchable Operations Management & Shift Engineers directory with direct Call (`tel:`) and WhatsApp (`wa.me`).
>   - **Celebrations, Notifications & Profile Parity**: Today's Team Celebrations + Wishes modal, 3D Notification Bell drawer (`@` mention resolution + expired celebration auto-sweep `< today IST`), Profile Tenure + dynamic Birthday countdown + Weekly Offs + Remove Photo confirmation, and Admin Broadcast **Title `@` mention autocomplete** + expired celebration auto-sweep.
> - **Version Sync & Verification**: Bumped `android/app/build.gradle.kts` (`versionCode = 143`, `versionName = "2.0.0"`) & `server.py` (`SERVER_VERSION = "2.0.0"`). All 22 live HTTP/PWA endpoints, RFC-5545 `.ics` unit tests, JS syntax checks, Zero-PII scan, and Android safe-build passed 100%!"*

---

## 📢 🕵️ Dasan Field Dispatch to Vijayan & Gafoorka (Sprint v2.0.0 STEP 1 — Unified `web/`, iOS PWA Serving & API Parity):
> *"Vijaya! Gafoorka! Sprint **`v2.0.0` STEP 1** 100% spotless aayi execute cheythu, live HTTP test suite pass aakki `origin/main`-il push cheythu kazhinju macha — ini stage poornamaayum ninakkullathaanu Vijaya! 🕵️‍♂️🔥🕶️
> 
> 1. **Task 1 — Clean Directory Rename (`admin-web/` → `web/`)**:
>    - Executed `git mv admin-web web` preserving full git history for every HTML/CSS/JS file!
>    - Updated `WEB_DIR = ROOT_DIR / "web"` + docstrings/errors in `server.py`, plus `README.md`, `web/README.md`, and `.agents/skills/rosterx-ui/SKILL.md`. Zero broken `admin-web` references remain!
> 2. **Task 2 — iOS PWA Static Serving & MIME Contract (`server.py`)**:
>    - Added explicit `extensions_map`: `.webmanifest` (`application/manifest+json; charset=utf-8`), `.ics` (`text/calendar; charset=utf-8`), `.js`/`.mjs` (`application/javascript; charset=utf-8`).
>    - `GET /sw.js` emits `Service-Worker-Allowed: /` + `Cache-Control: no-cache, no-store, must-revalidate` so iOS Safari always picks up new PWA builds cleanly.
>    - `GET /manifest.webmanifest` emits clean `Cache-Control: public, max-age=3600` (without conflicting `no-cache`/`Pragma` headers).
>    - `GET /open/*` deep-link fallback now covers `/open/dashboard` → `/#dashboard` alongside `/#roster`, `/#notifications`, `/#credentials`, `/#profile`, and `/`.
> 3. **Task 3 — Full Web–Android REST API Parity (`server.py`)**:
>    - Added seamless `_normalize_api_aliases()` so BOTH canonical Android routes AND REST shorthand routes work identically:
>      - `/api/credentials` ⇄ `/api/credentials/trackers`
>      - `/api/credentials/logs` ⇄ `/api/credentials/trackers/logs`
>      - `/api/credentials/<id>` ⇄ `/api/credentials/trackers/<id>` (spokespersons can also update `last_updated` / `last_updated_date` cleanly via `PUT` or `POST .../renew`!)
>      - `/api/broadcast-templates` ⇄ `/api/broadcast/templates` (plus path-based `DELETE /api/broadcast/templates/<id>` & in-memory sync!)
>    - Added `DELETE /api/notifications/<id>` (Admin & Manager soft-retract parity alongside `POST /api/notifications/retract`).
>    - Confirmed `POST /api/users/profile-picture` (upload + `""`/`null` photo removal) and Zero-Knowledge `POST /api/engineers` are 100% ready for Web JWT Bearer callers.
> 4. **Versioning & Push**:
>    - Kept version at **`v1.36.0` (Build `142`)** per pre-execution alignment so Vijayan lands the single clean **`v2.0.0` (Build `143`)** major release bump once the full `web/` iOS PWA & 1:1 Android parity frontend is complete.
>    - Verified live via ephemeral HTTP server (`GET /`, `/login.html`, `/user-portal.html`, `/.well-known/assetlinks.json`, `/open/*`, `/manifest.webmanifest`, `/sw.js`, `.ics`, and API route/auth guards) — **100% PASS** — and pushed to `origin/main`!"*

---

## 🎨 🕵️ Vijayan UI Studio Dispatch to Dasan & Gafoorka (v1.36.0 - Build 142 — Responsive Landscape Architecture):
> *"Dasa! Gafoorka! Full **Responsive Landscape Suite (`v1.36.0`, Build 142)** complete aayi implement cheythu, safe-build & live landscape ADB verify cheythu `origin/main`-il push cheythu macha! 🎨📱🔄
> - **Month Roster Untouched**: Both Admin `Rosters` and Portal `Month Roster` keep their custom `isFullScreenRoster` landscape modes 100% untouched.
> - **Slim 42dp Landscape Top Bar + Left `FluidNeumorphicNavRail`**: Added display-cutout-safe Left vertical Neumorphic Nav Rail (`~72dp` wide, icon + compact label, spring-animated raised active bubble) + ultra-slim `42dp` landscape top bar across BOTH Engineer/Manager Portal and Admin Console.
> - **Multi-Column / 2-Pane Responsive Tab Layouts**:
>   - **My Calendar**: 2-pane split — Left full-month compact grid (all 5–6 weeks fit on screen without scrolling!) + Right `Sync to Calendar` pill & Shift Legend.
>   - **Dashboard (Engineer & Manager)**, **Team Shifts**, **More Hub**, **My Team**, **Profile & Settings**, **Credential Tracker**, **Admin Dashboard**, **Register/Manage Team**, **Broadcast History**, and **Server Management**: Clean 2-pane / 2-column responsive layouts with tighter landscape padding.
> - **Floating AI Bot & Dialogs**: Compact `44dp` bottom-right placement in landscape (no 82dp bottom-nav offset) + `NeumorphicDialog`, `UnifiedEditProfileDialog`, and `ManageTeamTab` Edit dialog capped at `widthIn(max = 540.dp)` in landscape.
> - **Version Sync**: Bumped `android/app/build.gradle.kts` (`142` / `1.36.0`) and `server.py` (`SERVER_VERSION = "1.36.0"`)."*

---

## 🎨 🕵️ Vijayan UI Studio Dispatch to Dasan & Gafoorka (v1.35.1 - Build 141 — Authentic Neumorphic `Sync to Calendar` Polish):
> *"Dasa! Gafoorka! `CalendarSyncDialog` full Neumorphic polish + label rename complete aayi safe-build & live ADB verify cheythu `origin/main`-il push cheythu macha! 🎨✨
> - **Frosted Blur & Zero White Halo**: Switched `CalendarSyncDialog.kt` from raw `Dialog + neumorphicRaised` to the app's native `NeumorphicDialog` (`ApplyDialogBlurBehind()` + frosted scrim + `neumorphicFloating` downward-only shadow with zero white top-left outer halo and zero clipped inner shadows).
> - **Full Neumorphic Controls**: Upgraded all inner controls to authentic `NeumorphicButton` (`PrimaryAccent` & `Surface`), tactile raised/accent `NeumorphicCard` reminder pills (`None`/`15m`/`30m`/`1h`), and `neumorphicInset` wells for personal scope & target calendar list.
> - **Smart Single Primary CTA & Rename**: Renamed `"Sync My Shifts"` → **`"Sync to Calendar"`** in both `My Calendar` and the dialog header; hid the redundant disabled Sync button before permission is granted so only **`Allow Calendar Access`** + **`Share .ics`** show upfront.
> - **Version Sync**: Bumped `android/app/build.gradle.kts` (`141` / `1.35.1`) and `server.py` (`SERVER_VERSION = "1.35.1"`)."*

---

## 🎨 🕵️ Vijayan UI Studio Dispatch to Dasan & Gafoorka (v1.35.0 - Build 140 — Personal Calendar Smart Sync):
> *"Dasa! Gafoorka! User `/grill-me` design session-il lock cheytha **Personal Calendar Smart Sync (`v1.35.0`, Build 140)** clean aayi implement cheythu, safe-build adichu, live ADB verify cheythu `origin/main`-il push cheythu macha! 🎨📅🔥
>
> 1. **Personal-Only Scope & `My Calendar`-Only Placement (`EngineerPortalScreen.kt`)**:
>    - Removed the calendar icon completely from the team `Month Roster` header so there is zero confusion with the team grid.
>    - Upgraded `My Calendar`'s action pill to **`Sync My Shifts`** opening our new Neumorphic `CalendarSyncDialog` with explicit personal scope (`<Your Name> • @username • Personal schedule only`, `XX Shifts`, `YY Off/Leave/Swap`).
> 2. **Native Android `CalendarContract` Smart Diff Sync (`ShiftCalendarExporter.kt`, `CalendarSyncDialog.kt`, `AndroidManifest.xml`)**:
>    - Added runtime-requested `READ_CALENDAR` / `WRITE_CALENDAR` permissions + in-app writable Calendar Account picker (Google Calendar, Work Outlook, etc.) with saved calendar & reminder preferences.
>    - **Working Shifts (`FS`/`FG`/`G`/`SG`/`SS`/`NS`)**: Timed IST (`Asia/Kolkata`, `NS` overnight rollover) + **Busy** (`AVAILABILITY_BUSY`) + configurable reminder chips (`None` / `15m` / `30m` default / `1h`).
>    - **Non-Working Days (`OFF`/`L`/`SW*` Swap Off)**: **All-Day events** (`ALL_DAY = 1`, UTC midnight-to-midnight per Android spec, no clock time) + **Free** (`AVAILABILITY_FREE`) + **no reminder**.
>    - **Zero-Duplicate Re-Sync & Undo**: Deterministic `[RosterX-Sync:YYYY-MM:<username>:<dd>]` tagging updates modified days cleanly, deletes stale days on re-sync, supports **`Clear Month`** undo, and NEVER touches personal calendar events.
>    - **`.ics` Fallback Parity**: **`Share .ics`** inside the same sheet mirrors the exact same timed Busy + All-Day Free rules with zero permissions required.
> 3. **Version Sync (`v1.35.0` / Build `140`)**: Bumped `android/app/build.gradle.kts` (`140` / `1.35.0`) and `server.py` (`SERVER_VERSION = "1.35.0"`)."*

---

## 🕶️ 📢 Gafoor's Master Sprint Blueprint (v1.34.0 - Build 139 — Flagship Native Android Platform Suite):
> *"Dasa, Vijaya! Sprint `v1.33.1` clean aayi land aayi! Ippol nammude next flagship release **`v1.34.0` (Build 139)** lock aakki vechittundu — Android Verified App Links + 4 major native Android OS features!
>
> ### 1. 🔗 Verified Android App Links (`api.rosterx.online` → "Open by default"):
> - **Dasan (`server.py`, `render.yaml`, `.env.example`)**:
>   - Serve `GET /.well-known/assetlinks.json` (public JSON) reading comma-separated SHA-256 fingerprints from `ANDROID_APP_LINK_SHA256` env var (zero hardcoding).
>   - Add desktop fallback `GET /open/*` redirecting to `/` so non-Android clicks never hit 404.
>   - Add clickable **"Open in RosterX App →"** buttons in automated HTML emails (`/open/notifications`, `/open/credentials`, `/open/roster`, `/open/profile`).
>   - Enhance `GET /api/server-info` with `latest_android_version`, `min_supported_version`, and `apk_download_url` (from `APK_DOWNLOAD_URL` env var, defaulting to GitHub Releases latest).
> - **Vijayan (`AndroidManifest.xml`, `MainActivity.kt`, `AdminMainScreen.kt`, `EngineerPortalScreen.kt`)**:
>   - Add `<intent-filter android:autoVerify="true">` (`launchMode="singleTop"`) for `https://api.rosterx.online` covering `/` and `/open` (do NOT intercept `/api/*`!).
>   - Handle `intent.data` in `onCreate` & `onNewIntent` to jump straight to `ROSTER`, `NOTIFICATIONS`, `CREDENTIALS`, or `PROFILE`.
>
> ### 2. ⚡ Launcher Long-Press App Shortcuts + Native Splash Screen + Predictive Back (Vijayan):
> - Static shortcuts (`res/xml/shortcuts.xml`: Today's Roster, Notifications, My Profile) + dynamic role-aware Credential Tracker shortcut (`ShortcutManagerCompat`) for Admin/Manager.
> - Android 12+ `androidx.core:core-splashscreen` (`Theme.RosterX.Starting`, `installSplashScreen()`) + `android:enableOnBackInvokedCallback="true"`.
>
> ### 3. 📅 1-Tap Export Monthly Shifts to Google / Device Calendar (Vijayan):
> - `FileProvider` + RFC-5545 `.ics` batch generator (`ShiftCalendarExporter.kt`) skipping OFF/SW/Leave, mapping exact IST shift times + 30-min `VALARM` + stable `UID`, triggered from Month Roster header.
>
> ### 4. 🔔 In-App Update Banner (Vijayan):
> - Compare `BuildConfig.VERSION_NAME` vs `/api/server-info` `latest_android_version` semantically; display sleek dismissible Neumorphic update banner opening `apk_download_url`.
>
> **Strict Zero-PII & Mandatory `git push origin main` apply!** Check `plan_dasan.md` & `plan_vijayan.md`. Thudangikko!"*

---

## 📢 🕵️ Dasan Field Dispatch to Vijayan & Gafoorka (Sprint v1.34.0 - Build 139):
> *"Vijaya! Gafoorka! Sprint `v1.34.0` (Build 139) Android Verified App Links, Deep-Link Fallbacks, Branded Email CTAs & In-App Update Metadata backend 4/4 tasks 100% live HTTP-tested aakki `origin/main`-il push cheythu kazhinju macha! 🕵️‍♂️🔥🕶️
> 
> 1. **Task 1 — Public Android Digital Asset Links (`GET /.well-known/assetlinks.json`)**:
>    - Removed hardcoded keystore SHA-256 fingerprint completely! Now reads comma/newline-separated fingerprints strictly from `ANDROID_APP_LINK_SHA256` (env var or `.env`), normalizes (trim/uppercase/deduplicate), and serves HTTP `200` with `Content-Type: application/json; charset=utf-8` and clean `Cache-Control: public, max-age=3600` (fixed `end_headers()` so `no-store`/`Pragma` never overrides custom cache headers!).
>    - Preserved both `delegate_permission/common.handle_all_urls` (App Links) and `delegate_permission/common.get_login_creds` (Google Password Manager autofill) for `com.rosterx`.
>    - Documented `ANDROID_APP_LINK_SHA256` in `.env.example` and added `sync: false` entry in `render.yaml`.
> 2. **Task 2 — Graceful Desktop Fallback (`GET /open` & `GET /open/*`)**:
>    - Non-Android/desktop requests to `/open/roster`, `/open/notifications`, `/open/credentials`, `/open/profile`, or `/open` respond with clean `302 Found` (`Location: /#roster`, `/#notifications`, `/#credentials`, `/#profile`, or `/`) — zero 404s!
> 3. **Task 3 — Branded `"Open in RosterX App →"` Deep-Link CTAs in Automated Emails**:
>    - Added `APP_PUBLIC_BASE_URL` (default `https://api.rosterx.online`) and `_render_open_in_app_cta(...)` pill buttons across all 4 automated HTML emails:
>      - Broadcast Emails → `/open/notifications`
>      - Credential Expiry Alert Emails → `/open/credentials`
>      - Welcome / Onboarding Emails → `/open/profile`
>      - Server Gateway Emails → `/open/roster`
> 4. **Task 4 — In-App Update Metadata (`GET /api/server-info`)**:
>    - Enhanced `/api/server-info` (preserving all existing keys) with `latest_android_version` (`1.34.0`), `min_supported_version` (`1.30.0`, overridable via `MIN_SUPPORTED_VERSION`), and `apk_download_url` (overridable via `APK_DOWNLOAD_URL`).
> 5. **Verification & Render Deploy (`v1.34.0` - Build 139)**:
>    - `python -m py_compile server.py` + live ephemeral HTTP server test suite passed 4/4 (HTTP 200/302, headers, JSON schema, CTAs).
>    - Bumped `SERVER_VERSION = "1.34.0"`, committed ONLY Dasan's backend/infra scope (leaving Vijayan's active `android/` files untouched!), and pushed to `origin/main` for instant Render auto-deploy!"*

---

## 🎨 🕵️ Vijayan UI Studio Dispatch to Dasan & Gafoorka (Sprint v1.34.0 - Build 139):
> *"Dasa! Gafoorka! Sprint `v1.34.0` (Build 139) Flagship Native Android Platform Suite 5/5 tasks 100% spotless aayi implement cheythu, safe-build adichu `origin/main`-il push cheythu macha! 🎨🚀
> 
> 1. **Task 1 — Verified Android App Links & Deep-Link Router (`AndroidManifest.xml`, `MainActivity.kt`, `AdminMainScreen.kt`, `EngineerPortalScreen.kt`)**:
>    - Set `launchMode="singleTop"` + `<intent-filter android:autoVerify="true">` for `https://api.rosterx.online` covering `/` and `/open` (strictly excluding `/api/*`).
>    - Implemented `AppDeepLinkTarget` (`ROSTER`, `NOTIFICATIONS`, `CREDENTIALS`, `PROFILE`, `HOME`) parsed in both `onCreate()` and `onNewIntent()`, consumed reactively in both Admin and Engineer portals.
> 2. **Task 2 — Launcher Long-Press App Shortcuts (`res/xml/shortcuts.xml` + `ShortcutManagerCompat`)**:
>    - Added 3 static shortcuts (`Today's Roster`, `Notifications`, `My Profile`) with dedicated vector icons (`ic_shortcut_*.xml`).
>    - Dynamically publishes **Credential Tracker** shortcut for Admin/Manager sessions and removes it on logout or Engineer/Apprentice login.
> 3. **Task 3 — Android 12+ Native Splash Screen & Predictive Back (`libs.versions.toml`, `build.gradle.kts`, `themes.xml`, `values-night/themes.xml`, `AndroidManifest.xml`, `MainActivity.kt`)**:
>    - Added `androidx.core:core-splashscreen:1.0.1`, light (`#EEF2F6`) & dark (`#141923`) `Theme.RosterX.Starting`, `installSplashScreen()` before `super.onCreate()`, and `android:enableOnBackInvokedCallback="true"`.
> 4. **Task 4 — 1-Tap Monthly Calendar `.ics` Export (`file_paths.xml`, `ShiftCalendarExporter.kt`, `EngineerPortalScreen.kt`)**:
>    - Configured `FileProvider` (`${applicationId}.fileprovider`) and built RFC-5545 `.ics` generator using exact live RosterX IST shift windows (`FS` 07:30–16:30, `FG` 08:30–17:30, `G` 09:30–18:30, `SG` 11:00–20:00, `SS` 13:00–22:00, `NS` 21:00–07:30 +1), 30-min `VALARM`, and stable `UID` (`rosterx-<YYYY>-<MM>-<DD>-<user>@rosterx.online`).
>    - Added compact **"Export to Calendar"** buttons in BOTH **My Calendar** and **Month Roster** headers (with `ACTION_VIEW` + `ACTION_SEND` chooser fallback, zero runtime permissions).
> 5. **Task 5 — In-App Update Banner (`RosterApiService.kt`, `RosterRepository.kt`, `RosterRepositoryImpl.kt`, `ProfileComponents.kt`, `MainActivity.kt`, `AdminMainScreen.kt`, `EngineerPortalScreen.kt`)**:
>    - Wired `GET /api/server-info` (`latest_android_version`, `min_supported_version`, `apk_download_url`) + numerical `compareSemanticVersions(...)` + `AppUpdateBannerCard` (dismissible for regular updates, pinned without `×` if installed < `min_supported_version`).
> 6. **Build & Sync (`v1.34.0` - Build 139)**:
>    - Built cleanly via `safe_build.bat assembleDebug` (`42 actionable tasks`, 0 errors), pushed to `origin/main`. *(ADB port `100.71.241.36:5555` refused connection after build — ready for 1-command ADB install + `pm get-app-links com.rosterx` test once phone re-enables port 5555!)*"*

---

## 🕶️ 📢 Gafoor's Master Sprint Blueprint (v1.33.0 - Build 137):
> *"Dasa, Vijaya! High-priority production directives lock aakki vechittundu — read carefully before writing a single line of code:
> 
> ### 1. 🔐 STRICT PUBLIC REPO & ZERO-HARDCODED-PII RULE (`GEMINI.md` & `android/GEMINI.md` Updated!):
> - **This is a PUBLIC GitHub repository that auto-deploys directly to the Render production server (`https://api.rosterx.online`) on every push!**
> - **NEVER hardcode** personal names, real team usernames, emails, phone numbers, passwords, API keys, or secrets in code, comments, XML placeholders, AI prompts, or tests. Always use environment variables, MongoDB fields, or generic synthetic placeholders (`user@example.com`, `Team Member`, `member_username`).
> - **Mandatory `git push origin main`** immediately after every verified commit so Render stays 100% version-synced with ADB installs.
> 
> ### 2. 📢 Broadcast `@` Mentioning in Title — Full 3-Channel Parity:
> - **Dasan (`server.py`)**: Personalize Broadcast `title` (`@name` -> Full Name, `@all` -> `@Everyone`, `@username` -> `@Full Name`) across Email Subject & HTML `<h2>`, FCM Push Title, and `GET /api/notifications` (preserving raw title in MongoDB & Admin history).
> - **Vijayan (`NotificationsTab.kt`, `ProfileComponents.kt`)**: Upgrade `Notification Title *` to `TextFieldValue` + shared focus-isolated, bounds-safe `CompactCursorMentionPopup` (`@name`, `@all`, `@username` only for engineers), resolve Title in Live Preview Card + Notification Drawer.
> 
> ### 3. 🧹 100% Zero-Trace Cloudflared Tunnel Removal:
> - **Dasan (`server.py`, `render.yaml`, `tools/`, `.gitignore`)**: Delete all Cloudflared functions, `PUBLIC_TUNNEL_URL`, `public_tunnel_url.txt` writer, `.trycloudflare.com` CORS, `--tunnel`/`--no-tunnel` CLI flags, `ENABLE_TUNNEL`, and `tools/start_tunnel.bat` + `tools/start_tunnel.ps1`. Update `render.yaml` `startCommand` to `python server.py --no-open`.
> - **Vijayan (`LoginScreen.kt`, `UserSessionManager.kt`)**: Replace `trycloudflare.com` placeholder with `https://api.rosterx.online/api/` and remove all `trycloudflare.com` checks.
> 
> ### 4. 🗄️ DB-Driven Roster Metadata & Zero-PII Scrub (`include_in_roster`, `display_order`, `swap_code`):
> - **Dasan (`server.py`, `admin-web/`)**: Support optional `include_in_roster`, `display_order`, `swap_code` in `/api/engineers`; remove all hardcoded team names/usernames/emails from `admin-web/js/app.js`, `userPortal.js`, `constants.js`, `index.html`, `excelService.js`, `rosterAi*.js`, `admin-web/README.md`, and `server.py` comments.
> - **Vijayan (`Engineer.kt`, `EngineerPortalViewModel.kt`, `EngineerPortalScreen.kt`, `GeminiRosterService.kt`, widget XML/provider)**: Wire `includeInRoster`, `displayOrder`, `swapCode` DTO fields; remove hardcoded username allowlists and 10-member name-order map; scrub all personal names from prompts, comments, and widget XML.
> 
> ### 5. 🛡️ Zero-Knowledge Onboarding (Remove Admin Password Change) & Weekly Offs Shadow Fix:
> - **Dasan (`server.py`, `admin-web/`)**: Block password overwrites on existing accounts in `POST /api/engineers`; auto-generate a secure random password hash on brand-new account creation + send Welcome email guiding new members to set their private password via **"Forgot password?" (Email OTP)**; remove `#formEngPassword` from `admin-web/index.html` & `app.js`.
> - **Vijayan (`ManageTeamTab.kt`, `ProfileComponents.kt`)**: Remove password input from BOTH Edit Account and Add Member dialogs (make `Phone` full-width + show subtle OTP onboarding note on Add Member); fix Weekly Offs adjacent selected-chip white shadow bleed (`Wed`/`Thu`) by replacing `.neumorphicRaised(...)` inside the recessed rail with a crisp dark-only `.shadow(2.dp)` pill + `4.dp` spacing; fix Profile Birthday card dynamic label (`TenureAndBirthdayRow` → Today: `"BIRTHDAY"` / `"Today! 🎉"`, Tomorrow: `"BIRTHDAY"` / `"Tomorrow! 🎂"`, 2+ days: `"BIRTHDAY IN"` / `"$daysUntilBday days"`, unset: `"BIRTHDAY"` / `"--"`).
> 
> Bump to **`v1.33.0` (Build 137)**, verify, commit, and **`git push origin main`**. Check `plan_dasan.md` and `plan_vijayan.md`. Thudangikko!"*

---

## 📢 🕵️ Dasan Field Dispatch to Vijayan & Gafoorka (Sprint v1.33.0 - Build 137):
> *"Vijaya! Gafoorka! Sprint `v1.33.0` (Build 137) Backend, Web Portal & Production Hygiene 4/4 tasks 100% spotless aayi execute cheythu, zero-PII / zero-Cloudflared audit pass aakki, live Atlas DB metadata backfill cheythu Render-il auto-deploy aakkaan `origin/main`-il push cheythu macha! 🕵️‍♂️🔥🕶️
> 
> 1. **Task 1 — Broadcast Title `@` Mention Personalization (`server.py`)**:
>    - Implemented shared `_personalize_broadcast_text(...)` resolver (with trailing sentence-punctuation-safe mention regex!) used across BOTH Broadcast Title and Body.
>    - Resolves `@name` / `{name}` / `{their name}` → recipient's Full Name, `{username}` / `@{username}` → username, `@all` / `@everyone` / `@team` → `@Everyone` (plain) or badge (HTML), and `@<username>` → `@<Full Name>`.
>    - Wired across all 3 channels: Resend Email Subject & HTML `<h2>`, FCM Push Title, and `GET /api/notifications` (non-admin feeds), while preserving raw Title & Body in MongoDB and Admin history.
> 2. **Task 2 — 100% Zero-Trace Cloudflared Removal (`server.py`, `render.yaml`, `.gitignore`, `tools/`)**:
>    - Removed `PUBLIC_TUNNEL_URL`, `TUNNEL_PROC`, `find_cloudflared_path()`, `start_cloudflared_tunnel()`, `public_tunnel_url.txt` writer, `.trycloudflare.com` CORS entry, `--tunnel` / `--no-tunnel` CLI flags, and `ENABLE_TUNNEL`.
>    - Updated `render.yaml` (`startCommand: python server.py --no-open`) and `.gitignore`, and deleted `tools/start_tunnel.bat` & `tools/start_tunnel.ps1` via `git rm`.
>    - `/api/server-info`, `/api/telemetry/server`, and `/api/test-connection` now expose `PUBLIC_SERVER_URL` (`https://api.rosterx.online`) cleanly.
> 3. **Task 3 — Zero-PII & DB-Driven Roster Metadata (`server.py`, `admin-web/`, Live Atlas Backfill)**:
>    - Backfilled `display_order`, `swap_code`, and `include_in_roster` directly into all 12 live `ATLAS_DB.engineers` documents (zero PII in git!).
>    - Updated `server.py` (`is_management_account`, `POST /api/engineers`) and `admin-web/js/app.js`, `userPortal.js`, `constants.js` to drive roster inclusion, display order, and swap codes from DB fields with generic algorithmic fallbacks.
>    - Scrubbed every remaining hardcoded personal name, username, and corporate email from `server.py`, `admin-web/index.html`, `admin-web/README.md`, `excelService.js`, `rosterAiAssistant.js`, and `rosterAiEngine.js`. Verified **0 PII / 0 Cloudflared matches** across the entire backend & web codebase!
> 4. **Task 4 — Zero-Knowledge Account Onboarding & Overwrite Protection (`server.py`, `admin-web/`)**:
>    - `POST /api/engineers` now strictly strips any client-supplied `password`, never overwrites an existing account's password hash, and on brand-new account creation generates a cryptographically secure random password hash (`secrets.token_urlsafe(24)` via PBKDF2-SHA256) and dispatches a **Welcome to RosterX** email asynchronously guiding the user to set their private password via **"Forgot password?" (Email OTP)**.
>    - Removed `#formEngPassword` input & toggle from `admin-web/index.html` and `admin-web/js/app.js`, replacing it with a subtle security notice.
> 5. **Verification & Render Deploy (`v1.33.0` - Build 137)**:
>    - `python -m py_compile server.py` + 4/4 automated verification suites passed 100%, committed atomically, and pushed to `origin/main` so Render (`https://api.rosterx.online`) deploys live immediately!"*

---

## 🎨 🕵️ Vijayan UI Studio Dispatch to Dasan & Gafoorka (v1.33.1 - Build 138):
> *"Dasa! Gafoorka! Manage Team search box invisible-text hotfix complete aayi live verify cheythu push cheythu macha! 🎨🚀
> - **Root Cause (`ManageTeamTab.kt`)**: Material 3 `TextField` inside a `38.dp`-tall container had `32.dp` of default vertical padding (`16.dp` top + `16.dp` bottom), leaving only `6.dp` of height and clipping all typed characters.
> - **Fix**: Replaced with a compact sunken Neumorphic `BasicTextField` (`12.sp`, `lineHeight = 16.sp`, centered `decorationBox`, and clear `×` icon).
> - **Verified Live**: Built `v1.33.1` (Build 138), installed on `100.71.241.36:5555`, confirmed typed text (`admin`) renders crisply inside the search bar, and pushed to `origin/main`."*

---

## 🎨 🕵️ Vijayan UI Studio Dispatch to Dasan & Gafoorka (Sprint v1.33.0 - Build 137):
> *"Dasa! Gafoorka! Sprint `v1.33.0` (Build 137) Android UI & Production Hygiene 5/5 tasks 100% spotless aayi complete cheythu, zero-PII / zero-Cloudflared audit pass aakki, safe-build adichu live device-il verify cheythu push cheythu macha! 🎨🚀
> 
> 1. **Task 1 — Broadcast Title `@` Mention Popup & Full Resolution (`NotificationsTab.kt`, `ProfileComponents.kt`)**:
>    - Upgraded `Notification Title *` to `TextFieldValue` inside a compact single-line Neumorphic sunken well with aligned placeholder + focus border.
>    - Extracted shared focus-isolated, bounds-safe `CompactCursorMentionPopup(...)` (`@name`, `@all`, and **only `@username`** for engineers) shared cleanly across BOTH Title and Message Body (`onFocusChanged` ensures only the focused editor opens a popup).
>    - Extracted shared `resolveBroadcastMentions(...)` helper and wired full Title + Message personalization into both the Live Preview Card and `QuickNotificationsBottomSheet`.
>    - Updated template loads, history clones, and post-send resets to cleanly manage `titleTextFieldValue`.
> 2. **Task 2 — 100% Zero-Trace Cloudflared Removal (`LoginScreen.kt`, `UserSessionManager.kt`)**:
>    - Replaced `trycloudflare.com` placeholder & copy in `LoginScreen.kt` with `https://api.rosterx.online/api/` and stripped `trycloudflare.com` from `UserSessionManager.kt`. Verified `git grep -i "trycloudflare|cloudflared" android/app/src/` returns **0 matches**.
> 3. **Task 3 — Zero-PII & DB-Driven Roster Metadata**:
>    - Added `includeInRoster`, `displayOrder`, and `swapCode` across `Engineer.kt`, `EngineerDto` (`RosterApiService.kt`), `RosterRepositoryImpl.kt`, `RosterSyncWorker.kt`, and `ManageTeamTab.kt` (preserved on edit).
>    - Removed all hardcoded personal username allowlists (`Engineer.kt`, `EngineerPortalViewModel.kt`), the 10-name sort map (`EngineerPortalScreen.kt`), and all personal names in `GeminiRosterService.kt`, `RosterXAppWidgetProvider.kt`, and `item_widget_shift_pair.xml`. Verified **0 PII hits** across `android/app/src/`.
> 4. **Task 4 — Zero-Knowledge Admin Team Management + Non-Bleeding Selected Chip Shadows (`ManageTeamTab.kt`)**:
>    - Removed password inputs completely from BOTH Add Member and Edit Account dialogs (always sends `password = null`), made `Phone` full-width, and added the subtle OTP onboarding note on Add New Member.
>    - Replaced `.neumorphicRaised(...)` on selected chips across **Weekly Offs**, **Role and Access**, and **Employment Status** recessed rails with crisp dark-only `.shadow(2.dp)` pills + `4.dp` spacing — eliminating all white outer shadow bleed onto adjacent chips (`Wed` + `Thu`).
> 5. **Task 5 — Dynamic Profile Birthday Label (`ProfileComponents.kt`)**:
>    - Updated `TenureAndBirthdayRow`: `0` → `"BIRTHDAY"` / `"Today! 🎉"`, `1` → `"BIRTHDAY"` / `"Tomorrow! 🎂"`, `2+` → `"BIRTHDAY IN"` / `"$daysUntilBday days"`, `null` → `"BIRTHDAY"` / `"--"`.
> 6. **Build, Verification & Sync (`v1.33.0` - Build 137)**:
>    - Built via `safe_build.bat assembleDebug`, installed on `100.71.241.36:5555`, verified live via `android layout` & `android screen capture`, committed ONLY Vijayan's Android scope (keeping Dasan's staged backend files untouched!), and pushed to `origin/main`."*

---

## 🕶️ 📢 Gafoor's Master Sprint Blueprint (v1.32.0 - Build 132):
> *"Dasa, Vijaya! Nammude user oru critical bug report cheythu: Birthday & Work Anniversary notifications aa divasam kazhinjaalum drawer-il linger aayi nilkkukayaanu ("Yesterday, 05:52 PM"). Even user clear cheythillekilum aa calendar day kazhinjaal auto-remove aakanam ennaanu user-nte demand!
> 
> ### 🔍 Aligned Architecture from /grill-me:
> 1. **Hard Purge in MongoDB (`server.py` - Dasan)**:
>    - Aa calendar date (IST midnight) kazhinjaal, MongoDB `notifications` collection-il ninnum expired celebration notices permanently **delete** cheyyanam (`delete_many`).
>    - Filter criteria: `category == 'celebration'` or `celebration_id` exists, AND (`timestamp < start_of_today_ist` or `celebration_id` date < `today_str`).
> 2. **Clean Sweep (Remove Everywhere)**:
>    - Expired celebrations must be completely removed from BOTH regular user notification drawers and Admin/Manager Broadcast History.
>    - In `GET /api/notifications`: Run `purge_expired_celebrations()`, and double-filter `notifs` feed defensively so past celebration notices are NEVER returned.
> 3. **Client-side Defensive Date Filter (`ProfileComponents.kt` - Vijayan)**:
>    - Notification drawer-il `activeNotifs` filter cheyyumbol, celebration items-nte timestamp/date check cheythu if it's before today's IST date, immediately auto-drop it so that stale cached notices never flash on screen.
> 4. **Cursor-Anchored Compact `@` Mention Popup (`NotificationsTab.kt` - Vijayan)**:
>    - Currently `@` suggestions appear below the entire Message Body box (clipped by keyboard) and look bulky with full names/descriptions/avatars.
>    - Anchor a compact Compose `Popup(focusable = false, clippingEnabled = true)` directly beneath the cursor using `BasicTextField`'s `onTextLayout` -> `getCursorRect(cursorIndex)`.
>    - Remove `"SUGGESTED MENTIONS"` header & verbose descriptions. Keep `@name` / `@all` ultra-compact single-line, and for engineers render **ONLY `@username`** (no full name, no avatar, no extra details).
> 5. **Safe Build & On-Device Verification**:
>    - Bump version to `v1.32.0` (Build 132).
>    - Build via `cmd /c "cd android && safe_build.bat assembleDebug"`.
>    - Install on phone: `adb install -r android/app/build/outputs/apk/debug/app-debug.apk`.
>    - Verify both fixes on device: (1) yesterday's celebration notification is gone; (2) `@` mention popup floats compactly right below the cursor with usernames only for engineers!
> 
> Plans are written in `plan_dasan.md` and `plan_vijayan.md`. Dasa and Vijaya, thudangikko!"*

---

## 📢 🕵️ Dasan Field Dispatch to Vijayan (Sprint v1.32.0 - Build 133):
> *"Vijaya! Dasan ivide Expired Celebration Auto-Purge & Clean Sweep Engine 100% hard-purge mode-il deploy cheythu, live MongoDB Atlas-il thanne yesterday's lingering celebration sweep cheythu verify aakki macha! 🕵️‍♂️🔥
> 
> 1. **Hard Purge Engine (`purge_expired_celebrations()` & `is_expired_celebration()` in `server.py`)**:
>    - Midnight IST (`00:00:00 Asia/Kolkata`) kazhinja ethoru Birthday / Work Anniversary notification-um (`category == "celebration"`, `celebration_id`, `celebration_type`, or `notif_celeb_*`) MongoDB `ATLAS_DB.notifications`-il ninnum **permanently delete (`delete_many`)** cheyyum!
>    - **Orphaned Dismissed-ID Hygiene**: Purged celebration IDs are also automatically pulled (`$pull`) out of `ATLAS_DB.engineers.dismissed_notifications` so user docs stay spotless with zero dead IDs!
>    - **In-Memory Deque Sync**: `SERVER_NOTIFICATIONS` buffer-il ninnum expired celebrations simultaneously sweep aakum.
> 2. **Clean Sweep Lifecycle Hooks**:
>    - `GET /api/notifications`: Runs `purge_expired_celebrations()` before DB query AND applies a defensive clean-sweep filter across BOTH regular engineer drawers and Admin/Manager Broadcast History!
>    - `check_and_trigger_celebrations()`: Runs `purge_expired_celebrations()` before evaluating today's events.
>    - `_daily_celebration_cron()`: Runs `purge_expired_celebrations()` on server boot and daily at 08:30 IST.
> 3. **Live Atlas Verification**:
>    - Executed live sweep against MongoDB Atlas: **1 real lingering expired celebration doc was immediately caught and permanently deleted**!
>    - Synthetic test suite confirmed: yesterday's celebration purged + orphaned dismissed IDs pruned, today's celebration preserved, yesterday's normal operational broadcasts untouched.
> 4. **Version Bump**:
>    - `server.py`: `SERVER_VERSION = "1.32.0"`.
>    - `android/app/build.gradle.kts`: `versionCode = 133`, `versionName = "1.32.0"` (+1 monotonic bump from Build 132).
> 
> Vijaya, backend clean sweep 100% sealed aanu! Ini ninakku client-side defensive date filter (`ProfileComponents.kt`), cursor-anchored compact `@` mention popup (`NotificationsTab.kt`), and `safe_build.bat assembleDebug` adicholoo!"*

---

## 🎨 🕵️ Vijayan UI Studio Dispatch to Dasan & Gafoorka (v1.32.2 - Build 135):
> *"Dasa! Gafoorka! Edit Profile 'Remove photo' immediate delete flow + full scroll-container shadow & text clipping sweep complete aayi build adichu macha! 🎨🚀
> 
> 1. **Fixed Unresponsive 'Remove photo' in Edit Profile (`ProfileComponents.kt`, `AdminMainScreen.kt`, `EngineerPortalScreen.kt`, `EngineerPortalViewModel.kt`)**:
>    - Root cause: `showConfirmRemovePhotoDialog` was set to `true` on click in `UnifiedEditProfileDialog`, but no confirmation dialog was ever rendered!
>    - Added a Neumorphic **"Remove profile photo?"** confirmation dialog inside `UnifiedEditProfileDialog` with an `onRemovePhoto` callback wired to `viewModel.removeProfilePicture(...)` in both Admin and Engineer portals.
>    - On confirm, immediately deletes the photo on the server (`POST /api/users/profile-picture`), updates `sessionManager.updateCurrentUserProfilePicture(null)`, reverts both the dialog avatar and background profile card to initials right away, and shows a toast.
>    - Fixed `EngineerPortalViewModel.loadData()` merge logic (`profilePicture = fullProfile.profilePicture`) so a removed photo is never resurrected from stale session cache.
> 
> 2. **Full Scroll-Container Shadow & Placeholder Clipping Sweep**:
>    - Moved `.padding(...)` **inside** `.verticalScroll(rememberScrollState())` (with `.clip(RoundedCornerShape(24.dp))` on the outer dialog surface) across `UnifiedEditProfileDialog` (`ProfileComponents.kt`), Register/Edit Account (`ManageTeamTab.kt`), Add/Edit Credential Tracker (`CredentialTrackerScreen.kt`), and Shift Assignment Sheet (`MonthRostersTab.kt`) so zero raised Neumorphic shadows (`X`, `Cancel`, `Save changes`, chips) clip against the scroll viewport.
>    - Moved `Remove photo` cleanly below the subtitle line with `Icons.Default.DeleteOutline` in both `UnifiedEditProfileDialog` and `ManageTeamTab` so it never collides with the top-right `X` close button.
>    - Shortened `"Repeat new password"` → `"Confirm password"` so dual-column placeholders never clip mid-word.
> 
> 3. **Build Status (`v1.32.2` - Build 135)**:
>    - Compiled cleanly via `safe_build.bat assembleDebug`. Ready for ADB install once phone reconnects."*

---

## 🎨 🕵️ Vijayan UI Studio Dispatch to Dasan & Gafoorka (v1.32.1 - Build 134):
> *"Dasa! Gafoorka! Broadcast `@` mention crash fix, Neumorphic placeholder/cursor pixel alignment, and form placeholder cleanup 100% complete aayi safe-build adichu macha! 🎨🚀
> 
> 1. **Fixed `@` Mention Crash (`NotificationsTab.kt`)**:
>    - Root cause: `messageTextLayoutResult.getCursorRect(cursorIndex)` threw `IllegalArgumentException: offset(1) is out of bounds [0, 0]` on the first frame after typing `@` because `onTextLayout` runs after composition.
>    - Clamped `safeIndex` to `layout.layoutInput.text.length` and guarded with `runCatching { layout.getCursorRect(safeIndex) }.getOrNull()`.
> 
> 2. **Pixel-Perfect Placeholder & Cursor Alignment (`NeumorphicFormInput.kt`, `NeumorphicTextField.kt`, `NotificationsTab.kt`, `ProfileComponents.kt`, `EngineerPortalScreen.kt`)**:
>    - Root cause: Material 3 `Text` placeholder inherited `lineHeight = 24.sp` while `BasicTextField` used `lineHeight = Unspecified` inside a `TopStart`-aligned `Box`.
>    - Moved placeholders inside `BasicTextField`'s `decorationBox` sharing the exact same `LocalTextStyle.current.copy(fontSize, lineHeight, ...)` and `Alignment.CenterStart` (single-line) / `Alignment.TopStart` (multi-line), with `TextOverflow.Ellipsis`.
> 
> 3. **Instructional Form Placeholders & 6-Digit OTP Guard (`LoginScreen.kt`, `ManageTeamTab.kt`, `ProfileComponents.kt`)**:
>    - Forgot Password: `"e.g. johnd or user@tejasnetworks.com"` -> `"Enter username or email"`.
>    - OTP: `"123456"` -> `"Enter 6-digit OTP"` with strict numeric 6-char filter (`it.filter { ch -> ch.isDigit() }.take(6)`).
>    - Team Management & Edit Profile: Replaced `"e.g. John Doe"`, `"e.g. johnd"`, `"e.g. user@tejasnetworks.com"`, and `"+91 98765 43210"` with `"Enter full name"`, `"Enter username"`, `"Enter email address"`, and `"Enter phone number"`.
> 
> 4. **Build Status (`v1.32.1` - Build 134)**:
>    - Compiled cleanly via `safe_build.bat assembleDebug`. Ready to install via ADB once the phone reconnects (`100.71.241.36:5555` timed out)."*

---

## 🎨 🕵️ Vijayan UI Studio Dispatch to Dasan & Gafoorka (Sprint v1.32.0 - Build 133):
> *"Dasa! Gafoorka! Sprint v1.32.0 (Build 133) UI tasks randum 100% crisp & clean aayi complete cheythu, safe-build adichu phone-il install aakki macha! 🎨🚀
> 
> 1. **Task A — Client-Side Defensive Celebration Auto-Expiration Clean Sweep**:
>    - Added shared `NotificationDto.isExpiredCelebration()` extension in `RosterApiService.kt` comparing `timestamp` (and `createdAt` UTC/ISO fallback) against `LocalDate.now(ZoneId.of("Asia/Kolkata"))`.
>    - **`ProfileComponents.kt` (`QuickNotificationsBottomSheet`)**: Drops expired celebration notifications immediately from `activeNotifs` so cached notices never flash.
>    - **`EngineerPortalScreen.kt`**: Excluded expired celebrations from the top-bar bell `unreadCount` badge & bell wiggle animation so zero phantom badges ever appear.
>    - **`NotificationsTab.kt`**: Filtered expired celebrations out of both the `Broadcast History (${count})` tab pill and the history feed (`activeHistoryNotifications`).
> 
> 2. **Task B — Cursor-Anchored Ultra-Compact `@` Mention Floating Popup (`NotificationsTab.kt`)**:
>    - Removed the old bulky below-box `AnimatedVisibility` dropdown and `"SUGGESTED MENTIONS"` header completely.
>    - Tracked `TextLayoutResult` on `BasicTextField` via `onTextLayout`, computing `cursorRect = messageTextLayoutResult?.getCursorRect(cursorIndex)` and anchoring a Compose `Popup(alignment = Alignment.TopStart, offset = IntOffset(x, y), properties = PopupProperties(focusable = false, dismissOnBackPress = true, dismissOnClickOutside = true, clippingEnabled = true))` directly beneath the cursor (`cursorRect.bottom + 4.dp`).
>    - `focusable = false` keeps the software keyboard active and typing uninterrupted while tapping any suggestion inserts `@tag ` cleanly at the cursor.
>    - Ultra-compact card (`widthIn(135..195.dp)`, `heightIn(max = 170.dp)`, scrollable full match list): `@name` (`recipient`) and `@all` (`team`) single-line rows, and for engineers **ONLY `@username`** (`12.sp`, single line, no avatar/full name/role clutter).
> 
> 3. **Safe Build & Installation (`v1.32.0` - Build 133)**:
>    - Built cleanly via `safe_build.bat assembleDebug` and installed onto `100.71.241.36:5555` (`Performing Streamed Install -> Success`).
>    - *(Device screen was locked during automated capture; ready for live unlock & tap-through on phone!)*"*

---

## 🎨 🕵️ Vijayan UI Studio Dispatch to Dasan & Gafoorka (v1.31.2 - Build 132):
> *"Dasa! Gafoorka! Admin UI Pull-to-Refresh & Real-Time Profile Sync fixes complete aayi build & install cheythu macha! 🎨🚀
> 
> 1. **Global Admin UI Pull-to-Refresh (`AdminMainScreen.kt` & `AdminRosterViewModel.kt`)**:
>    - Added Material 3 `rememberPullToRefreshState()` and `PullToRefreshContainer` wired with `.nestedScroll(pullRefreshState.nestedScrollConnection)` across all Admin views (`Dashboard`, `Manage Team`, `Month Rosters`, `Broadcast`, and `Account & Settings`).
>    - Added `AdminRosterViewModel.refreshAllData(onComplete)` so pulling down refreshes both Admin data (`engineers`, `roster`, `constraints`, `backups`, telemetry) and `portalViewModel.refreshAllData()` silently without flickering the screen.
> 
> 2. **Bi-Directional Admin Profile & Session Sync (`AdminRosterViewModel.kt`, `EngineerPortalViewModel.kt`, `AdminMainScreen.kt`)**:
>    - Fixed the stale profile bug where editing the Admin/Manager name or contact details in **Team Management** (`ManageTeamTab`) did not update **Account & Settings** (`AccountView`) or the top-bar profile pill.
>    - Updated `AdminRosterViewModel.syncEngineers()`, `loadData()`, `updateUserProfile()`, `updateProfileName()`, and `uploadProfilePicture()` to immediately call `sessionManager.setCurrentUser(...)` whenever the logged-in account is modified or refreshed.
>    - Fixed `EngineerPortalViewModel.loadData()` merge logic to copy `name`, `fullName`, and `profilePicture` alongside phone/email/dates, and ensured `updateUserProfile()` persists updates to `sessionManager`.
>    - Updated `AdminMainScreen.kt` to bind the top-bar pill, `AccountView`, and `UnifiedEditProfileDialog` directly to the live engineer record from `state.engineers` matching `currentUser.username` for instant zero-lag UI updates.
> 
> 3. **Verification & Build (`v1.31.2` - Build 132)**:
>    - Compiled cleanly with `safe_build.bat assembleDebug` and installed onto `100.71.241.36:5555`.
>    - Live UI hierarchy confirmed the top-bar profile pill immediately renders `AD` / `Administrator` upon launch (previously stuck on `SA` / `System Administrator`)."*

---

## 🕶️ 📢 Gafoor's Master Sprint Blueprint (v1.31.0 - Build 129):
> *"Dasa, Vijaya! Gafoorka nammude Broadcast Console modernization-te full architectural blueprint ivide lock aakki vechittundu!
> 
> ### 🎯 User Requirement:
> > 1. Make the broadcast tab in admin and manager UI updated neumorphic with compact and responsive styles.
> > 2. Make unified standardised interactive editor with '@' triggering mentions like @name, @all, and username mentions. Remove quick chips and make it a subtle description about these.
> > 3. Remove preloaded templates and add option to add to templates while editing.
> 
> ### 🔍 Aligned Architecture from /grill-me:
> 1. **Top Segmented Navigation**:
>    - `[ 📢 Compose Broadcast ]`  |  `[ 📜 Broadcast History (count) ]`
>    - Portrait mobile-il clean separation between editor and history feed; landscape/tablet-il side-by-side dual pane!
> 
> 2. **Compose Editor Overhaul (`NotificationsTab.kt` - Vijayan's Territory)**:
>    - **Clean Slate (No Pre-filled Message)**: Remove the hardcoded initial title and message (`"Shift Operations Announcement"` and `"Dear {name}, Please review..."`). The composer starts with completely empty fields (`notificationTitle = ""`, `notificationMessage = ""`) with clean placeholders.
>    - **Templates Overhaul**: All preloaded/dummy templates are permanently removed! No pre-seeded text or personal names. In header, a sleek Neumorphic **`[ 💾 Save as Template ]`** button allows saving the current broadcast into MongoDB custom templates. Custom templates render as a compact horizontal pill strip above the title only when user saves them.
>    - **Inputs (Neumorphic Inset)**: Sunken wells for Title and Message editor (`Modifier.neumorphicInset(...)`).
>    - **Interactive `@` Mention Dropdown**: Typing `@` triggers popup with:
>      - `@name` (Dynamic recipient personalization: each engineer sees their own name)
>      - `@all` (Alert entire team)
>      - Specific teammates: Avatar + Full Name + `@username`.
>    - **Quick Chips Removed**: Removed redundant chip toolbar (`{name}`, `@all`, `@ Mention...`). Replaced with subtle helper note below editor:
>      `💡 Tip: Type @ to insert dynamic @name, alert @all, or mention specific teammates.`
>    - **Channels & Priority**: Compact toggle pills for `In-App`, `Push (FCM)`, `Email` and Priority.
>    - **Dispatch Action**: Neumorphic primary button `[ 📤 Dispatch Broadcast ]`.
> 
> 3. **Backend Support (`server.py` - Dasan's Territory)**:
>    - In `GET /api/broadcast/templates`: Remove auto-upserting `DEFAULT_BROADCAST_TEMPLATES`; return only user-created custom templates from `ATLAS_DB.broadcast_templates`.
>    - In broadcast notification processing: Support dynamic `@name` token resolution, replacing `@name` with the recipient's first name/display name in personalized emails and in-app feeds.
>    - Version bump: `SERVER_VERSION = "1.31.0"`.
> 
> 4. **Mobile App Build**:
>    - Version bump: `versionName = "1.31.0"`, `versionCode = 129`.
>    - Safe build: `cmd /c "cd android && safe_build.bat assembleDebug"`.
> 
> Dasa and Vijaya: Check `plan_dasan.md` and `plan_vijayan.md`. Thudangikko!"*

---

## 📢 🕵️ Dasan Field Dispatch to Vijayan (Sprint v1.31.0 - Build 129):
> *"Vijaya! Dasan ivide Broadcast Console-nte backend engine 100% clean slate aakki deploy cheythu, verified aakki macha! 🕵️‍♂️🔥
> 
> 1. **Dummy & Mock Templates Eradicated**:
>    - `DEFAULT_BROADCAST_TEMPLATES` poornamaayi purge cheythu!
>    - Live MongoDB Atlas-il ninnum 4 legacy dummy templates (`tpl_shift_update`, `tpl_urgent_coverage`, etc.) wipe cheythu. Atlas DB-il currently zero templates aanu — 100% clean slate!
>    - `GET /api/broadcast/templates`: User save cheyyunna custom templates maathram return cheyyum. Default injection permanent aayi theerthu.
> 2. **Custom Templates CRUD Ready**:
>    - `POST /api/broadcast/templates`: Admins and Managers-nu `name`, `title`, `message`, `priority`, `target_default` vechu custom template save cheyyaam.
>    - `POST /api/broadcast/templates/delete`: Template ID vechu clean delete support cheyyunnu.
> 3. **Dynamic `@name` Personalization Active**:
>    - Broadcast message-il `@name` or `{name}` vannaal:
>      - **Email (Resend)**: Recipient-inte Full Name (e.g. `Shefeeq K K`, as confirmed in our pre-execution grill) vechu dynamically replace aakum!
>      - **FCM Push Notification**: Status bar-il individual engineer-nte Full Name vechu personalize cheythu push deliver aakum!
>      - **In-App Feed**: Logged-in team member notification feed nokkumbol `@name` automatic aayi avarude Full Name aayi display aakum!
> 4. **Version Bump**:
>    - `server.py`: `SERVER_VERSION = "1.31.0"`.
>    - `android/app/build.gradle.kts`: `versionCode = 130`, `versionName = "1.31.0"`.
> 
> Vijaya, backend engine-il pure clean slate ready aanu! Ninakku Compose UI-il Neumorphic Segmented Control, interactive `@` autocomplete, and `[ 💾 Save as Template ]` finish cheyyaam!"*

---

## 🕶️ 📢 Gafoor's Master Sprint Blueprint (v1.30.0 - Build 128):
> *"Dasa, Vijaya! Gafoorka nammude Credential Tracker Audit Trail engine-nte full architectural blueprint ivide lock aakki vechittundu!
> 
> ### 🎯 User Requirement:
> > 'For Credential tracker only in Admin and Manager UI need option to see logs details who updated the dates and date of updation etc.
> > Single logs viewer which shows all accounts and Server details'
> 
> ### 🔍 Aligned Architecture from /grill-me:
> 1. **Access Control**: Strictly restricted to **Admin and Manager** (`isManagerOrAdmin == true`). Regular shift engineers cannot view or access audit logs.
> 2. **Backend Engine (`server.py` - Dasan's Territory)**:
>    - Dedicated MongoDB collection: `ATLAS_DB.credential_tracker_logs`.
>    - Captures `tracker_id`, `server_name`, `username`, `action` (`RENEW`, `UPDATE`, `CREATE`, `DELETE`), `updated_by`, `updated_by_name`, `previous_date`, `new_date`, `validity_days`, `expiry_date`, `summary`, `timestamp`, and `datetime_str` (IST formatted).
>    - Hooked into `_handle_create_credential_tracker`, `_handle_update_credential_tracker`, `_handle_renew_credential_tracker`, and `_handle_delete_credential_tracker`.
>    - Unified Terminology: 'Renewed' and 'Updated' are unified under `"UPDATED"` to remove redundancy.
>    - Baseline seeder: Automatically seeds historical baseline logs from existing trackers on startup so history is immediately visible.
>    - New protected endpoint: `GET /api/credentials/trackers/logs` (requires `admin` or `manager`).
>    - Version bump: `SERVER_VERSION = "1.30.0"`.
> 
> 3. **Compose UI Suite (`CredentialTrackerLogsSheet.kt` - Vijayan's Territory)**:
>    - Top-Bar Action: TopAppBar-il `Refresh` and `+ Add Tracker` button-nte koode **Audit Logs History Button** (`Icons.Default.History`) add cheyyuka (shown ONLY when `isManagerOrAdmin == true`).
>    - Single Neumorphic Logs Modal Sheet:
>      - Unified view aggregating all accounts & servers.
>      - Search bar (by server name, account username, or engineer name).
>      - Filter chips: `ALL`, `UPDATED`, `CREATED`, `DELETED` (no redundant `RENEWED` chip!).
>      - Rich log items: Server & Account pill (`NMS Server • root`), semantic action badge (`UPDATED` indigo / `CREATED` teal / `DELETED` rose), Who updated (`Name (@username)`), Date transition diff (`Previous: 16-Oct-2026 ➔ New: 17-Nov-2026`), Summary, and IST timestamp.
>    - Version bump: `versionName = "1.30.0"`, `versionCode = 128`, and execute safe build (`safe_build.bat assembleDebug`).
> 
> Dasa and Vijaya: Check `plan_dasan.md` and `plan_vijayan.md`. Thudangikko!"*

---

## 📢 🕵️ Dasan Field Dispatch to Vijayan (Sprint v1.31.0 - Build 129):
> *"Vijaya! Dasan ivide Broadcast Console-nte backend engine 100% clean slate aakki deploy cheythu, verified aakki macha! 🕵️‍♂️🔥
> 
> 1. **Dummy & Mock Templates Eradicated**:
>    - `DEFAULT_BROADCAST_TEMPLATES` poornamaayi purge cheythu!
>    - Live MongoDB Atlas-il ninnum 4 legacy dummy templates (`tpl_shift_update`, `tpl_urgent_coverage`, etc.) wipe cheythu. Atlas DB-il currently zero templates aanu — 100% clean slate!
>    - `GET /api/broadcast/templates`: User save cheyyunna custom templates maathram return cheyyum. Default injection permanent aayi theerthu.
> 2. **Custom Templates CRUD Ready**:
>    - `POST /api/broadcast/templates`: Admins and Managers-nu `name`, `title`, `message`, `priority`, `target_default` vechu custom template save cheyyaam.
>    - `POST /api/broadcast/templates/delete`: Template ID vechu clean delete support cheyyunnu.
> 3. **Dynamic `@name` Personalization Active**:
>    - Broadcast message-il `@name` or `{name}` vannaal:
>      - **Email (Resend)**: Recipient-inte Full Name (e.g. `Shefeeq K K`, as confirmed in our pre-execution grill) vechu dynamically replace aakum!
>      - **FCM Push Notification**: Status bar-il individual engineer-nte Full Name vechu personalize cheythu push deliver aakum!
>      - **In-App Feed**: Logged-in team member notification feed nokkumbol `@name` automatic aayi avarude Full Name aayi display aakum!
> 4. **Version Bump**:
>    - `server.py`: `SERVER_VERSION = "1.31.0"`.
>    - `android/app/build.gradle.kts`: `versionCode = 130`, `versionName = "1.31.0"`.
> 
> Vijaya, backend engine-il pure clean slate ready aanu! Ninakku Compose UI-il Neumorphic Segmented Control, interactive `@` autocomplete, and `[ 💾 Save as Template ]` finish cheyyaam!"*

---

## 🎨 🕵️ Vijayan UI Studio Dispatch to Dasan & Gafoorka (Sprint v1.30.0 - Build 128):
> *"Dasa! Gafoorka! Credential Tracker Single Audit Logs Viewer UI 100% complete aayi implement cheythu compile aakki macha! 🎨🚀
> 
> 1. **Secure Access Gate**:
>    - The Audit Logs button (`Icons.Default.History`) in the `CredentialTrackerScreen` TopAppBar renders **ONLY when `isManagerOrAdmin == true`**. Regular shift engineers cannot see or access it!
> 
> 2. **Single Neumorphic Audit Sheet (`CredentialTrackerLogsSheet.kt`)**:
>    - Built with full Neumorphic elevation, soft backdrop, and smooth spring animations.
>    - **Unified Feed**: Aggregates audit logs across all servers and accounts chronologically.
>    - **Fast Search Bar**: Inset search field filtering simultaneously across server names, account usernames, and the person who updated.
>    - **Clean Filter Chips**: `ALL`, `UPDATED`, `CREATED`, `DELETED` (*redundant 'Renewed' chip eliminated as requested!*).
>    - **Rich Log Cards**:
>      - Server & Account pill (`NMS Server • root`).
>      - Semantic action badges (`UPDATED` in Indigo `#6366F1`, `CREATED` in Teal `#059669`, `DELETED` in Rose `#EF4444`).
>      - Updater attribution: Avatar initials + Full Name + `@username`.
>      - Date transition diff: `Previous: 16-Oct-2026 ➔ New: 17-Nov-2026`.
>      - Summary text and IST timestamp (`17-Nov-2026, 09:29 PM IST`).
> 
> 3. **Compilation & Artifact**:
>    - `safe_build.bat assembleDebug` succeeded cleanly!
>    - Artifact generated: `android/app/build/outputs/apk/debug/app-debug.apk` (v1.30.0 - Build 128).
>    - All changes committed to Git!"*

---

## 📢 🕵️ Dasan Field Dispatch to Vijayan (Sprint v1.31.0 - Build 129):
> *"Vijaya! Dasan ivide Broadcast Console-nte backend engine 100% clean slate aakki deploy cheythu, verified aakki macha! 🕵️‍♂️🔥
> 
> 1. **Dummy & Mock Templates Eradicated**:
>    - `DEFAULT_BROADCAST_TEMPLATES` poornamaayi purge cheythu!
>    - Live MongoDB Atlas-il ninnum 4 legacy dummy templates (`tpl_shift_update`, `tpl_urgent_coverage`, etc.) wipe cheythu. Atlas DB-il currently zero templates aanu — 100% clean slate!
>    - `GET /api/broadcast/templates`: User save cheyyunna custom templates maathram return cheyyum. Default injection permanent aayi theerthu.
> 2. **Custom Templates CRUD Ready**:
>    - `POST /api/broadcast/templates`: Admins and Managers-nu `name`, `title`, `message`, `priority`, `target_default` vechu custom template save cheyyaam.
>    - `POST /api/broadcast/templates/delete`: Template ID vechu clean delete support cheyyunnu.
> 3. **Dynamic `@name` Personalization Active**:
>    - Broadcast message-il `@name` or `{name}` vannaal:
>      - **Email (Resend)**: Recipient-inte Full Name (e.g. `Shefeeq K K`, as confirmed in our pre-execution grill) vechu dynamically replace aakum!
>      - **FCM Push Notification**: Status bar-il individual engineer-nte Full Name vechu personalize cheythu push deliver aakum!
>      - **In-App Feed**: Logged-in team member notification feed nokkumbol `@name` automatic aayi avarude Full Name aayi display aakum!
> 4. **Version Bump**:
>    - `server.py`: `SERVER_VERSION = "1.31.0"`.
>    - `android/app/build.gradle.kts`: `versionCode = 130`, `versionName = "1.31.0"`.
> 
> Vijaya, backend engine-il pure clean slate ready aanu! Ninakku Compose UI-il Neumorphic Segmented Control, interactive `@` autocomplete, and `[ 💾 Save as Template ]` finish cheyyaam!"*

---

## 📢 🕵️ Vijayan UI Execution Dispatch to Dasan (Sprint v1.30.1 - Build 129):
> *"Dasa! User /grill-me session-il highlight cheytha 2 ergonomic refinements Vijayan ivide pakka aayi implement cheythu macha! 🎨🚀
> 
> 1. **Top Bar Title De-Squeezing**:
>    - **Issue**: Actions-il undaayirunna wide pill buttons (`[Logs]` and `[+ Add Tracker]`) kaaranam title 'Credential Tracker' width kuranju 3 lines-il squeeze aavunnundayirunnu.
>    - **Fix**: Wide pill buttons-inu pakaram sleek 36.dp Neumorphic Icon Buttons add cheythu (`Icons.Default.History` in subtle Indigo for Logs, `Icons.Default.Add` in AccentSalmon for Add Tracker, and Refresh icon).
>    - 'Credential Tracker' title-nu ippol 433px wide continuous room kitti, single crisp line-il un-squeezed aayi render aakunnu!
> 
> 2. **2-Level Smart Footer in Logs Sheet (`CredentialTrackerLogsSheet.kt`)**:
>    - **Issue**: 'Maddikayala Narasimha Kumar (@narasimhak)' pole neelamulla names varumbol right side-il ulla Date/Timestamp narrow box-il wrap aayi squeeze aavunnundayirunnu.
>    - **Fix**: 2-Level Smart Row implement cheythu:
>      - Left: Person avatar + Column with Full Name on Line 1 (`fontSize = 11.sp, fontWeight = SemiBold`) and `@username` on Line 2 (`fontSize = 10.sp, color = TextMuted`).
>      - Right: Dedicated un-squeezable surface pill (`[🕒 17-Nov-2026, 08:59 PM IST]`) with `softWrap = false, maxLines = 1`.
>    - Date is now 100% un-squeezed on a single line, and the full engineer name has abundant breathing space!
> 
> 3. **Release & Build**:
>    - Semantic version bumped to `v1.30.1` (Build 129) in `build.gradle.kts` and `server.py`.
>    - Compiled cleanly via `safe_build.bat assembleDebug` and installed on live device over ADB. Verified both screens via live screencaps!"*

---

## 📢 🕵️ Vijayan UI Execution Dispatch to Dasan (Sprint v1.30.0 - Build 128):
> *"Dasa! Gafoorka paranja poleyum, nammude pre-execution grill-il user confirm cheytha poleyum Vijayan ivide Credential Tracker Audit Trail UI suite 100% complete aakki build pass aakki vechittundu macha! 🎨🚀
> 
> 1. **Strict Role-Gated Access Control**:
>    - In `CredentialTrackerScreen.kt`, audit logs button and logs sheet access is strictly gated to `if (isManagerOrAdmin)`. Regular shift engineers cannot see or open the audit logs.
> 
> 2. **Top-Bar Audit Logs Action Button**:
>    - `CredentialTrackerScreen.kt` TopAppBar-il `Refresh` and `+ Add Tracker` button-nte koode stylish Indigo-accented (`#6366F1`) Neumorphic pill button `[📜 Logs]` add cheythu.
>    - Tap cheythaal automatic aayi `viewModel.loadCredentialTrackerLogs()` trigger aayi smooth slide-up bottom sheet open aakum.
> 
> 3. **Unified Neumorphic Bottom Sheet (`CredentialTrackerLogsSheet.kt`)**:
>    - **Layout**: High-floating modal bottom sheet with tactile drag handle, header with icon, title, subtitle, live refresh button, and dismiss button.
>    - **Instant Search**: Real-time filtering across server name, account username, updater name, or summary text.
>    - **Action Filter Chips**: `ALL`, `UPDATED` (Indigo `#6366F1`), `CREATED` (Emerald `#10B981`), `DELETED` (Rose `#EF4444`) with live counts. Redundant 'Renewed' chip eliminated.
>    - **Log Cards**:
>      - Server & Account pill (`NMS Server • root`).
>      - Semantic Action badge (`UPDATED` with `Autorenew`, `CREATED` with `AddCircleOutline`, `DELETED` with `DeleteOutline`).
>      - Date Transition Diff with strikethrough: `16-Oct-2026 ➔ 17-Nov-2026 • Expires 01-Jan-2027`.
>      - Human-readable summary.
>      - Updater attribution: `Person icon + Name (@username)`.
>      - Formatted IST timestamp (`17-Nov-2026, 09:29 PM IST`).
> 
> 4. **Design System Extension**:
>    - Added authoritative `NeumorphicTokens.Border` (`Color(0xB3CBD5E1)` light / `Color(0x1FFFFFFF)` dark) matching `rosterx-ui` design specifications.
> 
> 5. **Build Verification**:
>    - Safe build succeeded: `safe_build.bat assembleDebug` completed cleanly with `BUILD SUCCESSFUL in 2m 38s`!
>    - Build artifacts: `android/app/build/outputs/apk/debug/app-debug.apk` ready for distribution!"*

---

## 📢 🕵️ Vijayan UI Execution Dispatch to Dasan (Sprint v1.29.5 - Build 127):
> *"Dasa! User /grill-me session-il confirm cheytha 4 refinements Vijayan ivide pakka aayi implement cheythu macha! 🎨🚀
> 
> 1. **Bottom Shadow Clipping Fixed**:
>    - **Issue**: Buttons (`Save changes`, `Cancel`, `Delete`)-inte tactile drop shadow card-inte bottom edge-il cut aayi harsh line kanunnundayirunnu.
>    - **Fix**: Dialog card-il bottom padding `22.dp` aakki, outer vertical clearance `20.dp` aakki, and buttons-inu thazhe `Spacer(14.dp)` buffer add cheythu. Tactile shadows now have ample room to diffuse smoothly without getting cropped!
> 
> 2. **Remove Photo Confirmation Dialog**:
>    - `• Remove photo` tap cheythaal instant clear aavunnathinu pakaram, sleek Neumorphic Confirmation Dialog popup varum: "Remove profile photo?" with [Cancel] and [Remove] (Crimson Red). Accidentally tap cheythaalum safe aanu!
> 
> 3. **Frosted Background Scrim Tint**:
>    - Vivo/iQOO FuntouchOS pole OEM firmware-il cross-window blur disable aayittulla devices-ilum background text distracting aayi kaanathe, soft frosted glass veil (`Color(0xB8F1F5F9)` light / `Color(0x990A0F1A)` dark) + `0.45f` dimming add cheythu. Hardware blur supported aayittulla devices-il `blurBehindRadius = 60px` dynamic window lookup vazhi apply aakum.
> 
> 4. **Resignation Date Input upon choosing Resigned**:
>    - `Employment status`-il `Resigned` tap cheythaal smooth slide-down animation-il `Resignation date *` calendar picker input field appear aakum.
>    - Empty aayirunnal automatic aayi current date (`YYYY-MM-DD`) default populate aakum.
>    - Form validation & save contract update cheythu, `server.py`-il `resignation_date` MongoDB persistence & `auth/me` return sync cheythu.
> 
> 5. **Release & Build**:
>    - Semantic version bumped to `v1.29.5` (Build 127) in `build.gradle.kts` and `server.py`.
>    - Safe build succeeded: `app-debug.apk` compiled cleanly and installed on live device via ADB!"*

---

## 📢 🕵️ Vijayan UI Execution Dispatch to Dasan (Sprint v1.29.4 - Build 126):
> *"Dasa! User paranja 3 specific issues Vijayan ivide pakka aayi theerthu macha! 🎨🚀
> 
> 1. **Save Changes Button Color Fixed**:
>    - **Issue**: Save changes button dialog surface-inteyum Cancel button-inteyum athe niram aayirunnu, washed out white text kaaranam light mode-il kandupidiyaan polum pattiyilla.
>    - **Fix**: Save button-inu vibrant solid brand accent fill (`NeumorphicTokens.AccentSalmon` / `LocalThemeAccent.current`) with bold white checkmark and white typography add cheythu (`.background(if (canSave) AccentSalmon else AccentSalmon.copy(alpha=0.45f), RoundedCornerShape(12.dp))`). Cancel button-il ninnum distinct aayi primary CTA aayi shine cheyyunnu!
> 
> 2. **Weekly Offs Smudge Eradicated**:
>    - **Issue**: Selected days (`Wed`, `Thu`) uncontained row-il raised drop shadows (`neumorphicRaised`) apply cheythappol, 8dp blur shadow thazhe ulla "Employment status" text-ilekku spill aayi dirty smudge blob create cheythu.
>    - **Fix**: 7-day strip-ine compact recessed track container-ilekku (`neumorphicInset`) maatti. Selected days crisp clipped accent pills (`RoundedCornerShape(9.dp)`) aakki. Diffuse drop shadow bleed zero aayi! Same recessed track pattern `Employment status` (`Active` / `Resigned`)-ilum apply cheythu.
> 
> 3. **Remove Photo Option Added**:
>    - Header row-il user subtitle-inte koode red `• Remove photo` action button (`Color(0xFFEF4444)`) add cheythu.
>    - Tap cheythaal photo instantly remove aayi avatar initials (`AK`)-ilekku switch aakum, toast notification varum.
>    - `server.py`-il `POST /api/engineers` endpoint-il MongoDB `$unset: {"profile_picture": ""}` patch cheythu, so photo remove cheythu save cheythaal database-il ninnu photo clean aayi delete aakum!
> 
> 4. **Release & Build**:
>    - Semantic version bumped to `v1.29.4` (Build 126) in `build.gradle.kts` and `server.py`.
>    - Safe build succeeded: `app-debug.apk` compiled cleanly (33.3 MB)!"*

---

## 📢 🕵️ Vijayan UI Execution Dispatch to Dasan (Sprint v1.29.3 - Build 125):
> *"Dasa! Boss choicha form enhancements poornamaayi cheythu theerthu macha! 🎨🚀
> 
> 1. **High-Depth Recessed Input Basins (`NeumorphicFormInput.kt` & `NeumorphicTokens.kt`)**:
>    - Card surface-il ninnum distinct aayi kanunna pole `NeumorphicTokens.InputSurface` (`#E2E8F0` in light mode, `#151F2E` in dark mode) background token introduce cheythu.
>    - Inner occlusion gradient depth `1.8x` (approx 5.4dp) aakki deepen cheythu — input fields authentic sunken wells aayi maarikkazhinju!
>    - Unfocused state-il subtle 1dp hairline border, active state-il brand accent glow, error-il vibrant red border intact aanu.
> 
> 2. **Form Buttons Overhaul (`ManageTeamTab.kt` & `ProfileComponents.kt`)**:
>    - **Role and access**: Recessed segmented basin-il tactile raised pill (`neumorphicRaised`), `AccentSalmon` vibrant gradient, and crisp white typography.
>    - **Weekly offs (2 days)**: Header-il dynamic `${selectedOffs.size}/2 selected` counter; selected days-inu raised active salmon pills; unselected-inu inset sunken basins with hairline borders.
>    - **Employment status**: Active-inu Emerald Green (`#10B981`) raised pill; Resigned-inu Crimson (`#EF4444`) raised pill.
>    - **Action Row**: Delete button square `42.dp` with red accent border, Cancel button with soft surface, and Save button with checkmark icon — ellathinum active press kinetics (`neumorphicClickable`) add cheythu!
> 
> 3. **Frosted Background Blur Instead of Dark Dim (`ApplyDialogBlurBehind`)**:
>    - Heavy gloomy dark dimming (`0.60f`) maatti, hardware-accelerated `FLAG_BLUR_BEHIND` (API 31+) with `blurBehindRadius = 45px` and light luminous tint (`0.15f` dim) implement cheythu.
>    - `NeumorphicDialog.kt`, `ManageTeamTab.kt`, `ProfileComponents.kt`, `CredentialTrackerScreen.kt`, and `LoginScreen.kt` ellathilum frosted glass backdrop live aakki!
> 
> 4. **Build & Release**:
>    - Semantic version bumped to `v1.29.3` (Build 125) in `build.gradle.kts` and `server.py`.
>    - Safe build succeeded via `cd android && .\safe_build.bat assembleDebug` in 2m 49s!"*

---

## 📢 🕵️‍♂️ Dasan Security Sanitization Dispatch to Vijayan (Sprint v1.29.2 - Build 124):
> *"Vijaya! Repo security and backend credentials complete aayi harden cheythu macha! 🛡️✨
> 
> 1. **Eliminated Hardcoded Admin Password (`server.py`)**:
>    - Static `"Admin@123"` fallback poornamaayi eradicate cheythu.
>    - `ADMIN_PASSWORD` environment-il/`.env`-il illenkil, server dynamic cryptographic one-time password generate cheythu (`secrets.token_urlsafe(16)`) console-il single warning aayi print cheyyum.
>    - `hmac.compare_digest` strictly compares against loaded `admin_pass` variable only.
> 2. **Cleaned Default Engineers Directory Seed**:
>    - `DEFAULT_SERVER_ENGINEERS = []` by default in `server.py`. Zero corporate emails, phone numbers, or passwords hardcoded in source.
>    - Local development seeds load securely from ignored `.engineers_seed.json` / `default_engineers.json` with mock `@example.com` domains.
> 3. **Scratch Debris Purged**:
>    - Cleaned `scratch/test_all_apis.py`, `scratch/test_broadcast_suite.py`, `scratch/widget_screen.png`, and runtime tunnel files.
> 4. **Version Bump**:
>    - Bumped to `v1.29.2` (Android Build 124 & Server `SERVER_VERSION = "1.29.2"`)."*

---

## 📢 🕵️ Vijayan UI Execution Dispatch to Dasan (Sprint v1.29.1 - Build 123):
> *"Dasa! Boss choicha forms-ile white dropshadow issue Vijayan ivide complete aayi resolve cheythu macha! 🎨✨
> 
> 1. **White Dropshadow Eradicated (`NeumorphicModifier.kt` & `NeumorphicTokens.kt`)**:
>    - **Root Cause**: Specular white highlight (`0xE6FFFFFF`) floating dialogs-inte dark scrim backdrop-ilekku 28dp blur-il bleed cheyyumbozhaanu glaring white chalk halo / white dropshadow aayi maarunnath.
>    - **Floating Elevation Overhaul (`neumorphicFloating`)**:
>      - Outer white drop shadow poornamaayi remove cheythu (`lightShadow = Color.Transparent`).
>      - Realistic dual-layer downward dark occlusion drop shadow set cheythu (Ambient spread `10.dp` blur + Key directional shadow `24.dp` blur with downward +12.dp Y-offset).
>      - Card-inte boundary-kk akathu mathram nilkkunna subtle 1.dp inner specular rim stroke (`Color(0x59FFFFFF)` in light mode, `Color(0x2694A3B8)` in dark mode) add cheythu. No more white drop shadow anywhere!
> 2. **Sunken Form Basins (`NeumorphicFormInput.kt` & `ElevationInsetSubtle`)**:
>    - Inset light reflection 90% white-il ninnum gentle 15% translucent gleam (`0x26FFFFFF`) aakki subdued aakki.
>    - Input fields-inu crisp hairline focus-reactive border add cheythu (unfocused: subtle 1.dp hairline; focused: active `NeumorphicTokens.Accent`; error: Red).
> 3. **Screenshots & Visual Parity Verified on Device**:
>    - **Single-Line Search Bar**: My Team directory-il placeholder single-line with ellipsis confirmed.
>    - **Swap Off & Rest Day**: Team Shifts tab-il `SWA` card clean aayi `Swap Off` title-um `Rest Day` description-um display cheyyunnathu live verify cheythu.
>    - **Dates `16-Nov-2026` Format**: Credential Tracker-il dates `dd-MMM-yyyy` (e.g. `04-Oct-2026`, `18-Nov-2026`) formatted properly.
> 4. **Live Verification & Build**:
>    - Bumped to `v1.29.1` (Build 123) in `build.gradle.kts` and `server.py`.
>    - Executed `safe_build.bat assembleDebug` and installed on target `100.71.241.36:5555`. Verified live dialogs and captures!"*

---

## 🕶️ 📢 Gafoor's Master Sprint Blueprint (v1.29.0 - Build 122):
> *"Vijaya, Dasa! Gafoorka puthiya major UI redesign sprint-nte full architectural blueprint ivide lock aakki vechittundu!
> 
> ### 🎨 Goal: Complete Neumorphic Form Redesign (Image 2 Parity)
> User has provided an authentic Neumorphic reference (Image 2) and requested to transform all flat forms across RosterX into sleek, compact Neumorphic forms with mixed dual-column and single-column layouts for minimal/zero scrolling!
> 
> ### 📋 Scope of Forms in this Sprint:
> 1. **Edit Account & Register Engineer Dialog (`ManageTeamTab.kt` & `ProfileComponents.kt`)**:
>    - Full visual parity with Image 2:
>    - Header with avatar + camera badge + title + theme toggle.
>    - Dual-column Row 1: `Full name *` (50%) | `Username *` (50%).
>    - Single-column Row 2: `Corporate email *` (100% to fit long email strings).
>    - Dual-column Row 3: `Phone` (50%) | `New password` (50%, with eye toggle).
>    - Dual-column Row 4: `Join date` (50%) | `Birthday` (50%).
>    - Single-column Row 5: Segmented Neumorphic role selector (`[Engineer] [Manager] [Admin] [Apprent.]`).
>    - Action Row: Neumorphic square `[🗑️]` Delete button, `[Cancel]` button, and `[✓ Save changes]` primary button.
>    - All fields molded with Neumorphic sunken/inset basins (`Modifier.neumorphicInset(...)`). Zero vertical scrolling!
> 
> 2. **Login Screen (`LoginScreen.kt`)**:
>    - Replaced flat Material 3 surface with floating Neumorphic card (`NeumorphicTokens.Surface`).
>    - Inset Neumorphic inputs for Username & Password (retaining autofill & Google Password Manager).
>    - Standardized `"Forgot Username / Need Help?"` to **`"Forgot password?"`**, positioned **right-aligned directly below the Password input**.
>    - Tactile elevated Neumorphic **'Sign In'** button (`neumorphicClickable`).
>    - Compact vertical layout fitting comfortably on phone screens with zero scrolling.
> 
> 3. **Add/Edit Credential Tracker Dialog (`CredentialTrackerScreen.kt`)**:
>    - Dual-column Row 1: `Server Name *` | `Username *`.
>    - Dual-column Row 2: `Validity Days *` | `Alert Threshold Days *`.
>    - Single-column Row 3: `Last Updated Date *` (with calendar picker & 'Today' shortcut).
>    - Single-column Row 4: `Spokespersons` list.
>    - Neumorphic Delete, Cancel, and Save actions.
> 
> ### 🚀 Assignments:
> - **Vijayan**: Execute Compose UI overhaul in `plan_vijayan.md`, bump version to `versionName = "1.29.0"`, `versionCode = 122`, execute safe build, and verify on device.
> - **Dasan**: Sync `SERVER_VERSION = "1.29.0"` in `server.py`.
> 
> Vijaya, check `plan_vijayan.md`. Thudangikko!"*

---

## 🕶️ 📢 Gafoor's Master Sprint Blueprint (v1.28.2 - Build 121):
> *"Vijaya, Dasa! Gafoorka puthiya task-nte full architectural blueprint ivide lock aakki vechittundu!
> 
> ### 🎯 User Requirement:
> > 'Add tracker option is required for admin also same like manager'
> 
> ### 🔍 Why Admin Did Not See the 'Add Tracker' Option:
> 1. **Outer TopAppBar Collision in `AdminMainScreen.kt`**:
>    - `AdminMainScreen.kt` Scaffold's `topBar` condition was `if (!isFullScreenRoster)` — it was missing `&& !isViewingCredentialTracker`!
>    - Because of that, when Admin opened Credential Tracker from the More tab, the outer Scaffold drew the generic 'RosterX Admin' TopAppBar right on top, completely covering and replacing `CredentialTrackerScreen`'s own TopAppBar (which houses the **`+ Add Tracker`** button, title, refresh, and back button)!
> 2. **Role & Session Binding**:
>    - `CredentialTrackerScreen.kt` checked `state.loggedInEngineer?.role`. In `AdminMainScreen.kt`, the authoritative session is held in `currentUser` (`AdminRosterViewModel.currentUser`), but `state` passed was `portalState` (`EngineerPortalViewModel`).
> 
> ### 🛠️ The Fix for Vijayan (`plan_vijayan.md`):
> 1. In `AdminMainScreen.kt`:
>    - Hide outer TopAppBar when viewing Credential Tracker: `if (!isFullScreenRoster && !isViewingCredentialTracker) { TopAppBar(...) }`.
>    - Pass `currentUser = currentUser` to `CredentialTrackerScreen`.
> 2. In `CredentialTrackerScreen.kt`:
>    - Accept `currentUser: Engineer? = null`.
>    - Compute `effectiveUser = currentUser ?: state.loggedInEngineer`.
>    - Ensure `isManagerOrAdmin` evaluates to `true` for Admin, rendering the header **`+ Add Tracker`** button, empty-state button, and all edit/delete/spokesperson controls.
> 3. Version bump to `versionName = "1.28.2"`, `versionCode = 121`, and safe build (`cd android && .\safe_build.bat assembleDebug`).
> 
> Vijaya, check `plan_vijayan.md`. Thudangikko!"*

---

## 📢 🕵️ Vijayan UI Execution Dispatch to Dasan (Sprint v1.28.2 - Build 121):
> *"Dasa! Admin-kk Manager-ne pole thanne '+ Add Tracker' option-um full credential management suite-um Vijayan ivide pakka aayi deliver cheythu macha! 🛡️⚡
> 1. **Outer TopAppBar Collision Eradicated (`AdminMainScreen.kt`)**:
>    - Scaffold `topBar` condition updated to `if (!isFullScreenRoster && !isViewingCredentialTracker)`.
>    - When Admin enters Credential Tracker, the generic outer 'RosterX Admin' TopAppBar cleanly hides, allowing `CredentialTrackerScreen`'s dedicated TopAppBar (with Back arrow, Accent Salmon title, Refresh, and prominent **`+ Add Tracker`** pill button) to take command!
> 2. **Authoritative Session Binding (`CredentialTrackerScreen.kt`)**:
>    - Added `currentUser: Engineer? = null` parameter, passed from `AdminMainScreen.kt`.
>    - Resolved `effectiveUser = currentUser ?: state.loggedInEngineer`, guaranteeing `isManagerOrAdmin = true` for Admin sessions.
>    - Admin now has complete parity with Manager: Top-bar `+ Add Tracker`, Empty state `Add First Tracker`, Card edit/delete/renewal, Alert threshold days configuration, and Spokesperson selection!
> 3. **Version Bump & Build**:
>    - `SERVER_VERSION = "1.28.2"`
>    - `versionName = "1.28.2"`, `versionCode = 121`
> Ready for build and live verification!"*

---

## 🕶️ 📢 Gafoor's Master Sprint Blueprint (v1.28.0 - Build 119):
> *"Dasa, Vijaya! Gafoorka user ayacha screenshots inspect cheythu, bug-inte root cause 100% catch cheythu macha!
> 
> ### 🔍 Exactly Why Shefeeq Got "Happy Birthday Shefeeq" When Clicking Nandu's Anniversary:
> 1. In `ProfileComponents.kt` (`QuickNotificationsBottomSheet`), when "Tap to open wishes ✨" was tapped, it called `onOpenCelebrationWishes()` with **NO parameters**!
> 2. In `EngineerPortalScreen.kt`, that callback simply set `showCelebrationDialog = true`.
> 3. `CelebrationWishesDialog` invocation **hardcoded** `userName = state.loggedInEngineer!!` (Shefeeq K K), and used `activeCelebrationType` which defaulted to `CelebrationType.BIRTHDAY`!
> 4. It completely ignored the notification that was clicked! It treated every celebration as if the logged-in user themselves was celebrating a birthday!
> 5. Furthermore, teammates must receive celebration notifications and dialogs in **3rd Person** (celebrating the colleague), with direct **"Wish on WhatsApp"** and **"Call"** actions!
> 
> ### 🛠️ The Architecture & Assignments:
> 1. **Dasan (Backend)**:
>    - Add missing `import random` in `server.py`.
>    - Fix grammar bug in anniversary text: replace `"completing 2nd with our team"` with `"completing 2 years of operational excellence with our team"`.
>    - Deliver team broadcast in 3rd person: `"Today {full_name} completes {yr_label} with our team! Join us in celebrating this milestone and send your warm congratulations! 🥂🎊✨"`.
>    - Include `celebration_phone` in notification payload so client can trigger WhatsApp/Call.
>    - Dedicated endpoint `GET /api/celebrations/today`.
>    - Patch existing notification in MongoDB for `nandua` with 3rd-person text & phone number.
>    - Bump `SERVER_VERSION = "1.28.0"`.
> 2. **Vijayan (Compose UI)**:
>    - Enhance `CelebrationWishesDialog.kt` to support `isSelf: Boolean` (1st person vs 3rd person colleague milestone).
>    - For teammates (`!isSelf`): Displays colleague's name, trophy/cake badge, 3rd-person congratulatory text, **"🎉 Wish on WhatsApp"** button (`R.drawable.ic_whatsapp`, `#25D366`), and **"Call"** button!
   - **No Sender Signature Rule**: In pre-filled WhatsApp wishes for both birthdays and anniversaries, do NOT append `"- From [Name]"` or `"- From RosterX"`. Keep it direct, personal, and clean (e.g. `"Happy 2nd Work Anniversary Nandu! 🏆✨ Congratulations on completing 2 years with our team!"`).
>    - Update `QuickNotificationsBottomSheet`: pass `celebrationType`, `celebrantUser`, `celebrantName`, `celebrantYears`, and `celebrantPhone` to `onOpenCelebrationWishes(...)`.
>    - In `EngineerPortalScreen.kt`: wire up target celebrant state and pass into `CelebrationWishesDialog`.
>    - Version bump to `versionName = "1.28.0"`, `versionCode = 119`, and safe build (`cd android && .\safe_build.bat assembleDebug`).
> 
> Dasan and Vijayan: Check `plan_dasan.md` and `plan_vijayan.md`. Thudangikko!"*

---

## 📢 🕵️ Dasan Field Dispatch to Vijayan (Sprint v1.28.0 - Build 119):
> *"Vijaya! Dasan ivide backend celebration engine full aayi repair cheythu, verify cheythu, push delivery vare live aakki macha!
> 1. **Fatal Crash Eradicated**: `import random` missing aayirunna bhagyamkettu NameError crash njan theerthu! Ippo celebration check clean aayi odunnu.
> 2. **Flexible Date Normalizer**: `parse_celebration_date()` implement cheythu! `YYYY-MM-DD`, `DD-MM-YYYY`, `DD/MM/YYYY`, `MM-DD`, `DD-MM` — ethoru date format database-il vannaalum accurate aayi identify cheyyum. Live verification-il Nandu Anthikat-nte 2nd Work Anniversary today (`2024-11-16` -> `2026-11-16`) instant aayi trigger aayi, in-app broadcast notif insert aayi, FCM push 2 devices-il delivery SUCCESS aayi!
> 3. **Dedicated Endpoint `GET /api/celebrations/today`**: Route `GET /api/celebrations/today` deployed! Authenticated sessions-il call cheyyaam. Teammates-nte full celebration payload (username, name, type, years, photo, unmasked phone, title, greeting text) clean JSON aayi return cheyyum.
> 4. **Android Client DTO Parity**: `RosterApiService.kt`-il `TodayCelebrationDto`, `TodayCelebrationsResponse`, and `getTodayCelebrations()` function add cheythu type-safe aakki vechittundu.
> 5. **Version Bump**: `SERVER_VERSION = "1.28.0"`, `versionCode = 119`, `versionName = "1.28.0"`.
> Vijaya, backend engine celebration green signal thannu kazhinju! Ini ninakku `TeamCelebrationCard.kt` Neumorphic festive banner Home dashboard-il kettam, WhatsApp and Direct Call buttons configure cheyyaam, and safe build adikkaam!"*

---

## 📢 🕵️ Vijayan UI Execution Dispatch to Dasan (Sprint v1.28.1 - Build 120):
> *"Dasa! Nandu-nte anniversary notification click cheythal Shefeeq-nte birthday wishes varunna bug-um, 3rd person wishing experience-um full aayi Vijayan set aakki macha! 🎂🏆
> 1. **`CelebrationWishesDialog.kt` 3rd Person Overhaul**:
>    - Differentiated between `isSelf == true` (Self celebration: "Thank You! 🙏") and `isSelf == false` (Teammate celebration).
>    - Full Name rendered prominently (e.g., *"Happy 2nd Work Anniversary, Nandu Anthikat! 🏆"*).
>    - Body message honors colleague milestone with celebratory wording.
>    - Side-by-side action row: Prominent **'Wish on WhatsApp'** button (`#25D366` + `ic_whatsapp`) with clean greeting (NO redundant `- From RosterX` signature) + compact direct **'Call'** button (`#10B981` + `Icons.Default.Phone`) + Close button.
> 2. **Notifications Wiring (`ProfileComponents.kt` & `EngineerPortalScreen.kt`)**:
>    - `QuickNotificationsBottomSheet` passes `celebrationType`, `celebrationUser`, `celebrationName`, `celebrationYears`, and `celebrationPhone` from the notification DTO to the portal screen.
>    - Colleague's milestone opens directly with their details, phone number, and personalized pre-filled greeting.
> 3. **Version Bump & Build**:
>    - `SERVER_VERSION = "1.28.1"`
>    - `versionName = "1.28.1"`, `versionCode = 120`
>    - Safe build succeeded in 3m 7s with zero errors and streamed to live device `100.71.241.36:5555`!
> Work completed and verified!"*

---

## 📢 🕵️ Vijayan UI Execution Dispatch to Dasan (Sprint v1.28.0 - Build 119):
> *"Dasa! Ninte backend celebration engine-nte koode client UI festive aayi njan set aakki macha! 🎂🏆
> 1. **`TeamCelebrationCard.kt` Crafted**:
>    - Elevated Neumorphic container with golden festive border glow (`0xFFF59E0B`).
>    - Dynamic Badge based on celebration type (Birthday 🎂, Work Anniversary 🏆, or Double Celebration 🎉).
>    - Teammate avatar, bold name, and warm greeting text.
>    - Direct **'Wish on WhatsApp'** action button with official green branding and pre-formatted personalized greeting intent (`wa.me/<phone>?text=...`).
>    - Direct **'Call'** action button with phone icon launcher.
>    - Dismiss 'X' button to dismiss colleague cards locally for the day.
>    - Horizontal scroll carousel (`TeamCelebrationsSection`) for days when multiple colleagues celebrate together!
> 2. **Dashboard Placement Parity**:
>    - Engineer Portal (`DashboardView`): Displayed right below Welcome card, above shift cards.
>    - Manager Portal (`ManagerDashboardView`): Displayed right below Manager Welcome card, above Shift Radar.
>    - Admin Portal (`AdminDashboardTab.kt`): Displayed on top of Admin Dashboard overview.
> 3. **Build & Deployment**:
>    - Safe build `./gradlew assembleDebug` succeeded with zero errors (`v1.28.0` / Build `119`).
>    - Successfully installed onto live device `100.71.241.36:5555` via ADB stream install.
> All client mandates from Gafoorka are 100% complete and deployed!"*

---

## 🕶️ 📢 Gafoor's Master Sprint Blueprint (v1.27.0 - Build 113):
> *"Dasa, Vijaya! Gafoorka puthiya tasks-nte master plan ivide lock aakki vechittundu. Ippol namukku 4 major sectors aanu handle cheyyandathu:
> 
> 1. **Web Stuck in Loading (Fixed & Diagnosed)**:
>    - Root cause: `admin-web/js/userPortal.js` line ~3217-il `const restNoticeLabelEl` duplicate aayi declare cheytha kondu fatal compile-time `SyntaxError` vannu entire script crash aayi kidakkukayaayirunnu! Dasan athu remove cheythu web portal live aakkum.
> 
> 2. **My Team Enhancements**:
>    - Backend `server.py`-il regular engineers-nte phone number `****` mask cheyyunnathu Dasan remove cheyyum, so all authenticated team members can call & WhatsApp.
>    - Vijayan `MyTeamView.kt`-il standard message icon maatti official WhatsApp vector logo (`ic_whatsapp`) kodukkum.
>    - Team description-il '11 Active Members' ennu kaanichirunnathu Admin account include aayathinaalaanu. Admin exclude cheythu exact 10 members (1 Manager + 9 Engineers) aakki Vijayan correct cheyyum.
> 
> 3. **Credential Tracker Alert Consolidation**:
>    - Multiple accounts-nu alert varumbol Dasan oro account-num separate mail ayakkilla! All expiring accounts table aakki **single consolidated digest email** aakki, all spokespersons-ne 'To'-il vechu ayakkum.
>    - Email-il ninnum 'Update' / 'Renew' button remove cheyyum.
>    - 'Rotate' / 'Rotated' ennulla word maatti 'Update' / 'Updated' aakkum (backend & Android UI).
> 
> 4. **Notification Polish & Contextual Deep-Linking**:
>    - 'Clear All' and individual 'X' button click cheyyumbol instant snap aayi pokunnathu maatti, smooth slideOut + fadeOut + shrinkVertically height collapse animation Vijayan kodukkum.
>    - Notifications interactive aakkum: Tapped aakkiyaal relevant screen-ilekku redirect cheyyum (Credential -> Credential Tracker screen; Roster -> Month Roster; Celebration -> Wishes; Broadcast -> Full view).
>    - `@username` mentions styled chip aakkum, professional topic tags (`[SECURITY]`, `[ROSTER UPDATE]`, `[OPERATIONS]`, `[ANNOUNCEMENT]`) kodukkum.
> 
> Dasan and Vijayan: Check `plan_dasan.md` and `plan_vijayan.md`. Thudangikko!"*

---

## 📢 🕵️ Dasan Field Dispatch to Vijayan (Sprint v1.27.0 - Build 113):
> *"Vijaya! Dasan ivide backend-um web engine-um paka aakki set aakki macha! 
> 1. **Web Portal Loading Freeze Fixed**: `admin-web/js/userPortal.js`-il duplicate `const restNoticeLabelEl` re-declaration therippichu! Web portal syntax compile error poyi, `node --check` 100% clean aayi!
> 2. **Unmasked Phone Directory**: `server.py`-il phone masking (`****`) poornamaayi remove aakki! Ini `GET /api/engineers` call cheyyumbol authenticated Android users-nu full contact numbers kittum, so ninakku 'My Team' tab-il Direct Call-um WhatsApp launcher-um smooth aayi integrate cheyyaam.
> 3. **Consolidated Security Digest Email & Push**: Credential alerts consolidation paka aayi! Multiple expiring accounts undengil separate emails ayakkunnathu maatti, single consolidated security digest email-il elegant HTML table aayi ellaa accounts-um (Server, Username, Validity, Updated Date, Expiry Date, Urgency Status) list cheythu unique spokespersons-ku single broadcast aayi poyikkolum! Email button remove cheythu. Koodaathe in-app notification-um FCM push-um single digest aayi group cheythu!
> 4. **Version Bump**: `SERVER_VERSION = "1.27.0"`, `versionCode = 113`, `versionName = "1.27.0"`.
> Vijaya, backend engine green signal thannu kazhinju. Ini ninakku `ic_whatsapp`, active count, notification animation, and interactive deep-linking Compose UI-il thudangaam!"*

---

## 🎨 🕵️ Vijayan UI Studio Dispatch to Dasan & Gafoorka (Sprint v1.27.5 - Build 118):
> *"Dasa! Gafoorka! My Team search bar multi-line placeholder wrapping bug poornamaayi pariharichu macha:
> 
> 1. **Sleek Single-Line Search Bar**:
>    - In `MyTeamView.kt`, placeholder text refined to `'Search by name, role, or phone...'` with `maxLines = 1` and `overflow = TextOverflow.Ellipsis`.
>    - Corner radius standardized to `14.dp`, restoring sleek and balanced proportions with no awkward multi-line bloated text field.
>    - In `CredentialTrackerScreen.kt`, also enforced `maxLines = 1` and `TextOverflow.Ellipsis` for full cross-screen consistency.
> 2. **Version Bump & Live Verification**:
>    - Bumped to `v1.27.5` (Build 118) across `build.gradle.kts` and `server.py`.
>    - Compiled via `safe_build.bat assembleDebug` and installed on live target `100.71.241.36:5555`.
>    - Verified via live screenshot: My Team search bar renders in a crisp, single line with perfect vertical alignment!"*

---

## 🎨 🕵️ Vijayan UI Studio Dispatch to Dasan & Gafoorka (Sprint v1.27.4 - Build 117):
> *"Dasa! Gafoorka! Credential Tracker date formatting YYYY-MM-DD maatti '16-Nov-2026' (dd-MMM-yyyy) format-ilekku poornamaayi upgrade cheythu macha:
> 
> 1. **Credential Tracker Cards & Dialogs**:
>    - `CredentialTrackerScreen.kt`-il `formatCredentialDate` helper function ketti.
>    - Account cards-il 'Updated: 04-Oct-2026 (45d cycle)' and 'Expires: 18-Nov-2026' aayi render cheyyunnu.
>    - 'Confirm Credential Update' quick confirmation dialog-il New Update Date and Calculated Expiry elegant 'dd-MMM-yyyy' format-il aakki.
>    - 'Add/Edit Credential Tracker' dialog-il Pick Date button and 'Alarming begins on: dd-MMM-yyyy' display update aakki.
>    - Update success toast-il `Updated '{serverName}' credentials to 16-Nov-2026!` aakki.
> 2. **Notifications & Alerts Format Parity**:
>    - `ProfileComponents.kt` (`QuickNotificationsBottomSheet`)-il ISO dates (`YYYY-MM-DD`) automatically 'dd-MMM-yyyy' aakki dynamically format cheythu.
>    - `server.py`-il consolidated notification messages and email HTML tables `format_display_date` (`%d-%b-%Y`) vazhi 16-Nov-2026 format-il emit cheyyunnu.
> 3. **Version Bump & Live Verification**:
>    - `v1.27.4` (Build 117) compile cheythu device-il (`100.71.241.36:5555`) install aakki live screen capture vazhi verify cheythu macha!"*

---

## 🎨 🕵️ Vijayan UI Studio Dispatch to Dasan & Gafoorka (Sprint v1.27.3 - Build 116):
> *"Dasa! Gafoorka! Boss requested Swap Shift labeling change across the board:
> 
> 1. **Team Shifts & Global Swap Off Display**:
>    - In `TeamShiftsView`, `shiftTiming` calculation updated so that `ShiftCode.SW` / `startsWith("SW")` resolves cleanly to `"Rest Day"` instead of the generic `"Rest / Duty"`.
>    - Shift code domain model (`ShiftCode.kt`) updated: `ShiftCode.SW` name set to `"Swap Off"`.
>    - Kept dynamic peer badge code (`SWA`, `SWN`, etc.) indicating peer coverage while displaying `"Swap Off"` title and `"Rest Day"` subtitle.
>    - Dashboard `VibrantShiftCard` updated so swap off days reflect `"Rest Day"` instead of `"Active Duty"`.
> 2. **Cross-Platform & Widget Parity**:
>    - `RosterXAppWidgetProvider.kt` updated: `resolveMeta` returns `ShiftMeta("Swap Off", "Rest Day", ...)`.
>    - `EngineerPortalViewModel.kt` rest descriptor updated to `"Swap Off"`.
>    - Web admin `constants.js` updated to `'Swap Off (Treated as OFF)'`.
> 3. **Version Bump & Live Verification**:
>    - Bumped to `v1.27.3` (Build 116) across `build.gradle.kts` and `server.py`.
>    - Successfully compiled via `safe_build.bat assembleDebug` and installed on target `100.71.241.36:5555`.
>    - Navigated to `Wed, Nov 18, 2026` on live device: verified `SWA` card displays **Swap Off** with **Rest Day** subtitle!"*

---

## 🎨 🕵️ Vijayan UI Studio Dispatch to Dasan & Gafoorka (Sprint v1.27.2 - Build 115):
> *"Dasa! Gafoorka! Two critical items solved cleanly:
> 
> 1. **Fixed Top Bar Double Header & Subview Profile Collision**:
>    - `MyTeamView` open aakumbol main TopAppBar hide cheythu (`!isViewingMyTeam`). Double header and overlapping app bars poornamaayi thavirthu!
>    - More tab-ilum subviews-ilum profile chip hide cheythu (`!isViewingMyTeam && !isViewingCredentialTracker && selectedTab != 4`).
>    - Profile chip click cheythaal active subviews reset aayi direct Profile & Settings open aavum.
> 2. **Fixed Credential Notification Resend on Server Restart**:
>    - `server.py`-il in-memory `LAST_CREDENTIAL_ALERT_DATE` restart-il wipe aavunnathu kondaanu oro commit/restart-ilum duplicate alerts vanneerunnathu!
>    - MongoDB `ATLAS_DB.notifications`-il `digest_date: today_str` check ketti daily persistent idempotency lock aakki. Oro restart-ilum duplicate alerts varilla!
> 3. **Version Bump & Verification**:
>    - `v1.27.2` (Build 115) safe built, ADB installed on device, and verified via live screen capture!"*

---

## 🎨 🕵️ Vijayan UI Studio Dispatch to Dasan & Gafoorka (Sprint v1.27.1 - Build 114):
> *"Dasa! Gafoorka! Boss requested to bring the exact same Call & WhatsApp action buttons from 'My Team' directly into the 'Team Shifts' page right side of every engineer on duty!
> 
> 1. **Team Shifts Action Buttons (`EngineerPortalScreen.kt`)**:
>    - `TeamShiftsView`-il oro shift card-ilulla engineers list-ilum right side-il **Direct Phone Dial** (`ACTION_DIAL`) and authentic **WhatsApp** (`https://wa.me/` with `ic_whatsapp` vector icon) circular action buttons (34.dp) paka aayi integrate cheythu!
>    - Phone available aanenkil signature green emerald tints (`#059669` and `#25D366`), phone illenkil disabled styling with helpful toast alert.
> 2. **Version Bump & Live Device Test**:
>    - Bumped to `v1.27.1` (Build 114), built cleanly with `safe_build.bat assembleDebug` in 1m 48s.
>    - Successfully installed via ADB on `100.71.241.36:5555` and verified live on device screen! UI matches design system 100%!"*

---

## 🎨 🕵️ Vijayan UI Studio Dispatch to Dasan & Gafoorka (Sprint v1.27.0 - Build 113):
> *"Dasa! Gafoorka! Vijayan mobile Compose studio-il ellaa UI architectural mandates-um theerthu v1.27.0 (Build 113) fresh aayi build cheythu!
> 
> 1. **My Team WhatsApp Vector & True Active Count**:
>    - `ic_whatsapp.xml` authentic vector icon create cheythu `#25D366` WhatsApp emerald tint-il configure cheythu.
>    - `MyTeamView.kt` and `MoreHubView.kt`-il non-resigned admin accounts (`role == ADMIN` and `username == "admin"`) strictly exclude cheythu. Exact 10 active team members (1 Manager + 9 Shift Engineers) count and description 100% synchronize cheythu!
> 2. **Credential Tracker Terminology & Theme Color Harmonization**:
>    - Screen-ilum dialog-ilum Toast-ilum undayirunna ellaa `"Rotate"` / `"Rotated"` padangalum `"Update"` / `"Updated"` aakki maatti.
>    - Update Date elevated CTA chip, confirmation dialog header icon, confirmation button, date picker "Today" button ellam pacha color maatti signature theme accent color (`AccentSalmon`) aakki unify cheythu.
> 3. **Fluid Notification Exit & Staggered Clear All**:
>    - `QuickNotificationsBottomSheet`-il individual item exit `AnimatedVisibility(fadeOut + slideOutHorizontally + shrinkVertically)` vazhi height collapse aakki. Items remove aakumbol list jumps/gaps undaakilla.
>    - "Clear All" click cheyyumbol 60ms staggered waterfall delay-ode items smooth aayi animate out aakum.
> 4. **Contextual Deep-Linking & Mention Pills**:
>    - Notification tap cheythaal alert auto-dismiss aayi bottom sheet close aakum:
>      - Security / Credential -> Direct redirection to Credential Tracker (`isViewingCredentialTracker = true`).
>      - Roster / Shift Update -> Direct redirection to Month Roster tab (`selectedTab = 2` / `3`).
>      - Celebration -> Celebration Wishes Dialog.
>    - Elegant topic badges (`[SECURITY]`, `[ROSTER UPDATE]`, `[OPERATIONS]`, `[ANNOUNCEMENT]`, `[CELEBRATION 🎉]`) with category-specific icons.
>    - `@username` and Teammate name styled inline mention pills.
> 5. **Safe Build Execution**:
>    - `cd android && .\safe_build.bat assembleDebug` BUILD SUCCESSFUL in 3m 16s!
>    - Output APK: `android/app/build/outputs/apk/debug/app-debug.apk` (v1.27.0 - Build 113) ready for deployment!"*

---

## 🚨 SYSTEM ALERT — CI/CD Build Fix & Firebase SHA-256 Alignment (2026-11-16 09:58 IST) — v1.26.3 (Build 112)

> *"Dasa! Vijaya! Build #92 failed on GitHub Actions because `google-services.json` was purged from git repository. We have wired GitHub Actions Secrets restoration for both `google-services.json` and the exact `debug.keystore` matching Firebase's SHA-256 fingerprint!"*

### 🔴 1. Root Cause of Build #92 Failure:
- `android/app/google-services.json` was purged from git and placed in `.gitignore`.
- When GitHub Actions runner cloned the repo, `google-services.json` was absent, causing Gradle task `processReleaseGoogleServices` to fail after 44s.

### 🔴 2. Solution Implemented:
- Updated `.github/workflows/build-apk.yml`:
  - Added secret decoding step for `GOOGLE_SERVICES_JSON_BASE64` into `android/app/google-services.json`.
  - Added secret decoding step for `DEBUG_KEYSTORE_BASE64` into `~/.android/debug.keystore`.
  - This ensures GitHub Actions signs APKs with the exact debug keystore whose SHA-256 (`EA:81:06:66:4C:EF:E8:51:FD:1B:54:17:D1:77:DD:E6:EB:83:FF:AA:9B:DA:D9:17:33:F5:CC:1D:51:0B:43:7D`) is registered in Firebase Console!
  - Renamed artifact uploads from static `v1.11.8` to `RosterX-Release-APK` and `RosterX-Debug-APK`.

### 📦 Version Status
- `SERVER_VERSION`: **`1.26.3`**
- `versionCode`: **`112`**
- `versionName`: **`1.26.3`**

---

## 🚨 SYSTEM ALERT — Gafoor Security Overhaul (2026-11-16 09:40 IST) — v1.26.2 (Build 111)

> *"Dasa! Vijaya! Public repository cleanliness audit completed & executed! Git history sanitized 100%!"*

### 🔴 1. `google-services.json` Purged from Entire Git History & Gitignored
- `android/app/google-services.json` was purged from **all commits** in repository history via `git-filter-repo`.
- Added to `.gitignore` so Firebase API keys are **never pushed to public GitHub**.
- The file remains preserved locally on disk so `.\safe_build.bat assembleDebug` and Android studio builds continue to work seamlessly without breakage.
- History rewritten and pushed to GitHub `origin/main` via force push.

### 🔴 2. Hardcoded JWT Fallback Secret Removed (`server.py`)
- Removed static string fallback `rosterx_enterprise_sec_key_...` from `get_auth_secret()`.
- Now strictly enforces `AUTH_SECRET_KEY` via environment variables (or `.env` file); otherwise throws a descriptive `RuntimeError`.

### 📦 Version Status
- `SERVER_VERSION`: **`1.26.2`**
- `versionCode`: **`111`**
- `versionName`: **`1.26.2`**

---

## 🚨 SYSTEM ALERT — Gafoor Security Directive (2026-11-15 22:54 IST) — v1.26.1 (Build 110)

> *"Dasa! Vijaya! Sharikkum sharikkum important! Randu security changes enforce cheythu — ningal randupeerum ithinte impact ariyaanam!"*

### 🔴 1. SMTP Fallback — Completely Removed (Backend Breaking Change)

`send_otp_email()` ippol **Resend API mathram** aanu OTP email dispatch cheyyunnathu. Gmail SMTP fallback (Port 465 SSL + Port 587 STARTTLS) **purthaayi therippichu**. Ithine depend cheyyunna yethoru logic-um upadheyam aavanilla.

**Removed from `server.py` (do NOT re-add):**
- `import smtplib` ❌
- `import ssl` ❌
- `from email.mime.multipart import MIMEMultipart` ❌
- `from email.mime.text import MIMEText` ❌
- `get_smtp_credentials()` function ❌
- `DEFAULT_SMTP_EMAIL` / `DEFAULT_SMTP_PASSWORD` globals ❌
- Strategy 2 (SMTP SSL 465) & Strategy 3 (STARTTLS 587) in `send_otp_email()` ❌

**`send_otp_email()` now:** Calls `send_resend_email()` directly → returns `True`/`False`. No fallback. No SMTP.

**`.env` impact:** `SMTP_EMAIL` and `SMTP_PASSWORD` vars are now **unused** by the server. They can be removed from the `.env` file. `.env.example` still documents them as a note, but the server ignores them.

### 🔴 2. Hardcoded SMTP Credentials — Purged from Git History

A Gmail App Password was previously hardcoded as an `os.environ.get(...)` default fallback. It has been **fully removed from all code paths**. The credential `eskzhgckwazgrpjo` is **compromised and must be rotated** at [myaccount.google.com/apppasswords](https://myaccount.google.com/apppasswords).

### ✅ Resend API — Single Source of Truth for All Email

| Channel | Status |
|---|---|
| `send_resend_email()` | ✅ Active — OTP emails, credential alerts, startup link emails |
| `send_otp_email()` | ✅ Resend-only now |
| Gmail SMTP (smtplib) | ❌ Removed permanently |

**Vijayan — Android/UI Impact:** Zero. `POST /api/auth/forgot-password` and `POST /api/auth/send-otp` endpoints behave identically from the client side — same request, same response contract. If Resend fails, the API returns `{"success": false, "error": "..."}` — no silent SMTP retry behind the scenes anymore.

### 📦 Version Bumped
- `SERVER_VERSION`: `1.26.0` → **`1.26.1`**
- `versionCode`: `109` → **`110`**
- `versionName`: `1.26.0` → **`1.26.1`**
- Commit: `a61e976`

---

---

## 🕶️ 📢 Gafoor's Master Sprint Blueprint (v1.26.0 - Build 109):
> *"Dasa, Vijaya! Gafoorka ivide blueprint varachu vechittundu. Ippol namukku 4 major tasks aanu theerkkaanullathu:
> 1. **Admin UI Overhaul**: Admin UI-il ninnu 'Preview App' button therippichu! Top right-il User/Manager pole Account Avatar pill varum. Bottom-il ninnum Settings tab maatti, Manager/User pole **'More'** tab aakkum. Admin-te 'More' tab-il Credential Tracker-um Server Management-um card aayi varum.
> 2. **Login Keyboard Glitch**: Login cheyyumbol password field-il tick/Done/Return adichaal keyboard thazhe pokunnilla. `keyboardController?.hide()` and `focusManager.clearFocus()` add cheythu athu fix aakki kodukkaam!
> 3. **Credential Tracker Alert Threshold & Action Pill**: Validity-ude quick chips (30d, 45d, 60d, 90d) therippichu! Paka manual input field mathram aakki. Manager & Admin-ku mathram alert threshold days (e.g. 5 days) manual input field koduthu live calculated alarm date preview kaanikkum. Koodaathe, 'Update Date' chip ippol passive status pill pole thonnunnathu maatti, elevated Neumorphic pill button (with subtle shadow, emerald icon & crisp text) aakki distinct interactive CTA aakkum.
> 4. **More Tab & My Team Directory**: More tab-il ninnum dummy handover notes card maatti. Pakaram Manager & User UI-il **'My Team'** card varum. Tapped aakkiyaal dedicated directory sub-page: Managers and Shift Engineers grouped hierarchy, with 📞 Direct Call and 💬 WhatsApp action icons!
> 
> Dasan and Vijayan: Check `plan_dasan.md` and `plan_vijayan.md`. Thudangikko!"*

---

## 📢 🕵️ Vijayan Field Dispatch to Dasan:
> *"Dasa! Ellaam paranja pole thanne paka aayi ketti macha! Gafoorka-de design blueprint 100% execute cheythu:
> 1. **Admin UI Overhaul**: Admin UI-il ninnu 'Preview App' button therippichu! Top-right-il User Avatar + First Name account pill ketti `AccountView` toggling set aakki. Tab 4 'Settings' maatti **'More'** aakki, athil `MoreHubView` configured for Admin (with Server Credential Tracker & Server Management full sub-views) paka aayi!
> 2. **Login Keyboard Dismissal**: Password field-il tick/Done adichaal `keyboardController?.hide()` and `focusManager.clearFocus()` immediate aayi trigger aayi keyboard dismiss aavum.
> 3. **Credential Tracker Alert Threshold & Action Pill**: Presets therippichu manual number input aakki. Manager & Admin-ku mathram Alert Threshold (Days) manual input field koduthu live calculated alarm date preview badge (`Alarming begins on: <date> (X days before expiry)`) kaanichu. Koodaathe, 'Update Date' button-ine elevated emerald Neumorphic CTA button aakki!
> 4. **My Team Directory (`MyTeamView.kt`)**: More tab-il ninnum dummy handover notes maatti Manager & User-ku 'My Team' card ketti. Search filter, Managers and Shift Engineers sections, 📞 Direct Phone Dial, and 💬 WhatsApp intent actions ready!
> 5. **Build & Parity**: `safe_build.bat assembleDebug` 100% BUILD SUCCESSFUL aayi, Android v1.26.0 (Build 109) APK ready!"*Vijaya! Gafoorka thന്ന blueprint kandu njaan backend engine set aakki macha! 
> 1. `server.py`-il `compute_credential_status`-il `alarm_start_date` (`expiry_date - alert_threshold_days`) standardise cheythu payload-il provide cheythu.
> 2. `alert_threshold_days` sanitize & clamp cheythu: `max(1, min(alert_threshold_days, validity_days))` (default: 5 days, or min(5, validity_days) if validity is smaller).
> 3. Create, Update, and Renew endpoints ellaathilum `alarm_start_date` and clamped `alert_threshold_days` live update aayi return cheyyum.
> 4. Android data model `CredentialTrackerDto`-il `@SerializedName("alarm_start_date") val alarmStartDate: String?` sync aakki type-safe aakki vechittundu.
> 5. Server version `1.26.0` bump aakki, py_compile & credential unit test pass aakki!
> Vijaya, Compose UI-il alert threshold input-um dynamic alarm badge-um 'Update Date' elevated pill-um koodi njan thന്ന data contract vechu smooth aayi kettiyaal Sprint 1.26.0 polikkum!"*

---

## 📌 Active System State & Monorepo Location
- **Unified Workspace**: `c:\shefeeq\PYTHON\RosterX`
- **Native Android Project Root**: `c:\shefeeq\PYTHON\RosterX\android`
- **Semantic Version**: **`1.26.1`** (Android `versionCode: 110`, Server `SERVER_VERSION = "1.26.1"`)
- **Previous Versions**: `1.25.1` (Build 108), `1.25.0` (Build 107), `1.24.15` (Build 106)
- **Production Server**: ☁️ **Render Cloud + Branded Cloudflare Domain**  
  🌐 `https://rosterx.online` & `https://api.rosterx.online` (Render origin: `rosterx-vc30.onrender.com`)  
  📱 **Android Base URL**: `https://api.rosterx.online/api/`
- **Database Engine**: MongoDB Atlas `Cluster0` (Database: `rosterx`, Collection: `credential_trackers`)
- **Target Device**: `192.168.1.7` (Wireless Debugging / USB)

---

## 🚀 Accomplished in v1.25.1 (Build 108) by Vijayan (Compose UI Lead)

### 1. 🗂️ Server-Grouped High-Density Layout (`CredentialTrackerScreen.kt`)
- Grouped trackers by `serverName` into dedicated server card sections.
- Server header features DNS icon, bold server title, and account count pill.
- Nested accounts displayed in compact, sleek rows with user icon, status pill, rotation/expiry dates, and action buttons.

### 2. 🛡️ Role-Based Spokesperson Visibility
- Designated spokesperson alert chips are hidden for standard duty engineers to reduce clutter.
- Visible exclusively to Admins and Managers.

### 3. 📅 Interactive Calendar DatePicker & Expiry Preview
- "Update Date" button opens Material3 `DatePickerDialog` restricted to current or past dates, with a dedicated "Today" shortcut.
- Selecting a date triggers a quick confirmation modal showing the chosen rotation date, validity cycle, and freshly calculated expiry date before dispatching renewal.

### 4. 🏷️ Dynamic Healthy Filter Counter
- Status filter chips now display live count for all states including `Healthy ($healthyCount)`.

### 5. ⚡ Enhanced Add/Edit Tracker Dialog
- Auto-suggest chips for Server Name and Username derived from existing trackers.
- Clickable DatePicker field with "Today" quick button for Last Rotated Date.
- Updated preset buttons: `30d`, `45d`, `60d`, `90d` + custom input.
- Teammates quick-add chips from team roster (`rosterEngineers`).

### 6. 🧹 Streamlined Operations Hub (`MoreHubView.kt`)
- Cleaned redundant expiry alert card. Operations Hub cleanly hosts the primary Server Credential Tracker launcher and the upcoming Handover Notes card.

---

## 🚀 Accomplished in v1.25.1 (Build 108) by Dasan

### 1. 🔄 Flexible Renewal Date & Strict Validation (`server.py`)
- **`POST /api/credentials/trackers/<id>/renew`**:
  - Accepts optional body: `{"last_updated_date": "YYYY-MM-DD", "alert_threshold_days": 7}`.
  - If omitted, defaults to today's date in IST (`get_ist_now().strftime("%Y-%m-%d")`).
  - **Rule 1 (No Future Dates)**: Rejects `last_updated_date > today_ist` with 400 Bad Request.
  - **Rule 2 (Active Validity Window Only)**: Rejects past dates where `days_remaining < 0` with 400 Bad Request, preventing renewal to an already-expired state.

### 2. 🔔 Customizable Alert Threshold Days (`alert_threshold_days`)
- Stored per tracker (default: `5` days).
- Status calculation:
  - `days_remaining > alert_threshold_days` -> `HEALTHY`
  - `0 < days_remaining <= alert_threshold_days` -> `EXPIRING_SOON`
  - `days_remaining == 0` -> `EXPIRES_TODAY`
  - `days_remaining < 0` -> `EXPIRED`
- Both `_daily_credential_expiry_cron()` and `check_and_dispatch_credential_alerts()` use the tracker's specific alert threshold window.

### 3. 📱 Android Data Layer Parity
- `RosterApiService.kt`: Updated `CredentialTrackerDto`, `CreateCredentialTrackerRequest`, `UpdateCredentialTrackerRequest`, and `RenewCredentialTrackerRequest` with `alertThresholdDays`.
- `RosterRepositoryImpl.kt` & `EngineerPortalViewModel.kt`: Updated `renewCredentialTracker` with optional `renewDate` and `alertThresholdDays` parameters (with default `null` values preserving backward compatibility).

---

## 🚀 Accomplished in v1.25.0 (Build 107) by Dasan

### 1. 🔑 Server Credential Expiry Tracker REST Suite (`server.py`)
- **`GET /api/credentials/trackers`**: Lists all tracked server credentials with live computed `days_remaining` and status enum (`HEALTHY`, `EXPIRING_SOON`, `EXPIRES_TODAY`, `EXPIRED`). Returns summary counters (`total_count`, `expiring_soon_count`, `expired_count`).
- **`POST /api/credentials/trackers`**: Creates new tracker with auto-generated ID (`cred_tracker_<hex12>`). Enforces validation for `server_name`, `username`, `last_updated_date`, `validity_days`, and `spokesperson_emails`. (Admins/Managers only).
- **`PUT /api/credentials/trackers/<id>`**: Updates credential configuration, re-evaluates expiry status dynamically. (Admins/Managers only).
- **`POST /api/credentials/trackers/<id>/renew`**: 1-Tap Quick Renew! Resets `last_updated_date` to today's date in Indian Standard Time (IST), updates expiry date, and marks status healthy. Authorized for Admins, Managers, and assigned Spokespersons.
- **`DELETE /api/credentials/trackers/<id>`**: Deletes tracker from MongoDB Atlas and in-memory cache.
- **`POST /api/credentials/trackers/test-check`**: Manual trigger for the daily notification check (Admins/Devs).

### 2. ⏰ 09:00 AM IST Automated Notification Engine
- Background daemon thread `_daily_credential_expiry_cron()` running daily at 09:00 IST (with 15s startup pre-flight check).
- Evaluates all trackers where `days_remaining <= 5`.
- **HTML Email Dispatch**: Styled alert email sent to designated `spokesperson_emails` via Resend API.
- **FCM Push Notification**: Priority push sent to spokespersons and all Admins & Managers.
- **In-App Notification**: Persistent badge recorded in `ATLAS_DB.notifications`.
- Daily idempotency map prevents duplicate alert spam on the same calendar day.

---

## 📋 Accomplished in v1.25.0 (Build 107) by Vijayan
> *"Dasa macha! Karyangal ellaam set aayi! Tab 4-il Neumorphic 'More' Operations Hub katti aakki, athil ninnum smooth aayi Credential Tracker Screen-ilekku slide aayi povum! 1-Tap Quick Renew, KPI cards, filter chips, Add/Edit dialog with spokesperson auto-suggest ellaam ready! Koodaathe Login Screen-il Gboard username suggestion issue-um fix aakki — `shefee...ail.com` email suggestion maatti, pure `AutofillType.Username` aakki `shefeeqkk` recommend cheyyanulla vazhi clear aakki! Safe build pass aayi (Build 107 APK ready)!"*

### 1. 🔑 Server Credential Tracker UI Suite (`CredentialTrackerScreen.kt` & `MoreHubView.kt`)
- **Operations Hub (`MoreHubView.kt`)**: Neumorphic launcher card displaying live counters for Server Credential Tracker (Total, Expiring Soon, Expired) with status badges and upcoming toolkit placeholders.
- **Credential Tracker Screen (`CredentialTrackerScreen.kt`)**:
  - Neumorphic TopAppBar with back arrow and "+ Add Tracker" action.
  - KPI Summary Bar (Total, Expiring Soon with Amber Glow, Expired with Crimson Alert).
  - Search & Status Filter Chips (`ALL`, `EXPIRING`, `EXPIRED`, `HEALTHY`).
  - Tracker Cards: Server name, username, status badge (`HEALTHY`, `EXPIRING_SOON`, `EXPIRES_TODAY`, `EXPIRED`), expiry countdown, and spokesperson email chips.
  - 1-Tap "Renewed Today" quick action calling `POST /api/credentials/trackers/{id}/renew`.
  - Add / Edit Tracker Modal Dialog with validity presets (30d, 60d, 90d, 180d) and team roster email auto-suggest chips.
  - Delete Tracker confirmation dialog.
- **5-Tab Navigation (`EngineerPortalScreen.kt`)**: Added Tab 4 "More" (`Icons.Default.MoreHoriz`) to bottom navigation for both Managers and Duty Engineers. Fullscreen slide transition hides bottom bar and TopBar seamlessly.

### 2. ⚡ Gboard Username Autofill Optimization (`LoginScreen.kt`)
- Removed `AutofillType.EmailAddress` from `identifierAutofillNode`, retaining strictly `AutofillType.Username`.
- Changed field label to pure `"Username"` and set `keyboardType = KeyboardType.Ascii` with `autoCorrect = false`.
- Prevents Gboard from defaulting to device email prediction (`shefee...ail.com`) and instructs Android Autofill to propose the account username `shefeeqkk`.

### 3. 📦 Build & Release
- APK built successfully: `android/app/build/outputs/apk/debug/app-debug.apk`
- Semantic Version: `v1.25.0` (Build `107`).

---

## 📋 Accomplished in v1.31.1 (Build 131) by Vijayan
> *"Dasa macha! Karyangal poornamaayi set aakki! Broadcast Tab-inte pazhaya clutter poornamaayi maati, sleek Neumorphic Segmented Tab Row `[ 📢 Compose Broadcast ]` vs `[ 📜 Broadcast History ]` aakki kattiyaakki! Clean slate initialization (no default announcements), zero dummy templates, Save as Template dialog, and cursor-aware live `@` mention autocomplete popup (`@name`, `@all`, teammates) set aakki! Koodaathe user paranja pole chips-nte unselected state poornamaayi SUNKEN (`neumorphicInset`) aakki — Priority Level, Delivery Channels, Specific Engineers selector-ilum, Edit Account form-ile Role & Access, Weekly Offs, and Employment Status-ilum tactile sunken basins aakki maatti! Live device-il stream install cheythu visual checks run cheythu 100% verified!"*

### 1. 📢 Neumorphic Broadcast Console Overhaul (`NotificationsTab.kt`)
- **Segmented Control**: Molded recessed track hosting `[ 📢 Compose Broadcast ]` and `[ 📜 Broadcast History (count) ]` with vibrant glowing accent pill on active selection.
- **Clean Slate Initialization**: Eliminated legacy pre-filled default message body and title. Sunken well inputs with crisp placeholders (`Enter broadcast title...`, `Type broadcast message...`).
- **Dynamic Reusable Templates Strip**: Removed hardcoded dummy templates. Displays custom saved template pills with delete ('×') button only when templates exist in Atlas DB.
- **Save as Template**: Neumorphic action button opening a tactile dialog to name and save custom reusable announcements directly to `POST /api/broadcast/templates`.
- **Cursor-Aware `@` Mention Autocomplete**:
  - Replaced noisy static suggestion chips toolbar with an inline live suggested mentions popup.
  - Dynamically triggers when `@` is typed at cursor position:
    - `@name`: Dynamic recipient full name personalization.
    - `@all`: Operational team-wide broadcast alert.
    - Matching active teammates with avatar, name, and `@username`.
- **Inline Live Personalized Preview Card**: Real-time feedback for dynamic recipient resolution and enabled channel badges.
- **Dedicated History Section**: Tab 1 displays full past announcements feed with clone button (loads announcement into composer and auto-switches to Tab 0), channel badges, and retraction.

### 2. 🔘 Sunken Chips for Unselected State (`ManageTeamTab.kt` & `NotificationsTab.kt`)
- **Broadcast Console**:
  - Delivery Channels (`In-App`, `Push (FCM)`, `Email`): Sunken (`neumorphicInset`) when unselected, raised (`neumorphicRaised`) with color border when active.
  - Priority Level (`Info`, `Shift Update`, `Urgent`, `Broadcast`): Sunken (`neumorphicInset`) when unselected, raised with vibrant color glow when active.
  - Specific Engineers multi-select chips: Sunken (`neumorphicInset`) when unselected, raised with accent tint when active.
- **Manage Team / Edit Account Dialog**:
  - `Role and access`: Unselected roles are sunken into the track (`neumorphicInset`), selected role is raised with high-contrast accent.
  - `Weekly offs`: Unselected days are sunken into the track (`neumorphicInset`), selected days are raised.
  - `Employment status`: `Active` / `Resigned` chips are sunken (`neumorphicInset`) when inactive, raised with status color when active.

### 3. 📦 Build & Release
- APK built successfully: `android/app/build/outputs/apk/debug/app-debug.apk`
- Semantic Version: `v1.31.1` (Build `131`).
- Verified via ADB on live device `100.71.241.36:5555`.
