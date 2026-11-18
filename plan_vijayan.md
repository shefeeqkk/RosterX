# 📋 Execution Plan for Vijayan (Unified `web/` iOS PWA & 1:1 Android Feature Parity)

**Status:** `COMPLETED (v2.0.0 — Build 143)` ✅
**Execution Summary:** All 6 sprint tasks implemented, verified live against `server.py` (22/22 HTTP & PWA header checks + RFC-5545 `.ics` test + Zero-PII audit), Android safe-built (`versionCode = 143`, `versionName = "2.0.0"`), and ready for immediate `origin/main` push!

---

## 🔐 Mandatory Production Rule from Gafoor (System Architect 🕶️)
> **Public GitHub Repo + Auto-Deploying Render Production Server:**
> - **ZERO hardcoded personal names, usernames, emails, phone numbers, passwords, or secrets** anywhere in HTML, CSS, JS, Kotlin, comments, or placeholders.
> - **Mandatory `git push origin main`** immediately after full verification so Render (`https://api.rosterx.online`) stays synced.

---

## 🎯 Master Objective (Locked via `/grill-me`)
Transform `web/` into an **installable iOS PWA & adaptive mobile-first web app** that delivers **1:1 tab, visual, and feature parity with our Android app (`v1.36.0`)** on iPhone/mobile while preserving a rich Desktop layout on laptops/PCs:
- **Remove unwanted Swap Request tabs** from BOTH portals (keep `SW*` shift codes intact in roster grids!).
- **Exact 1:1 Android 5-Tab Role Parity** across **Engineer**, **Manager**, and **Admin**.
- **Seamless iOS Mobile Experience**: 5-tab Fluid Neumorphic Bottom Nav (portrait) + Slim `42px` Top Bar & Left Vertical Neumorphic Nav Rail (landscape, leaving Month Roster full-screen landscape!).
- **Native iOS `.ics` Calendar Sync** (zero setup — opens Apple Calendar's native "Add All" sheet directly!).

---

## 1. Task 1 — Installable iOS PWA Shell & 6 Brand Color Palettes (`web/`)
1. **Create `web/manifest.webmanifest`**:
   - `"name": "RosterX"`, `"short_name": "RosterX"`, `"start_url": "/login.html"`, `"display": "standalone"`, `"orientation": "any"`, `"theme_color": "#174EAC"`, `"background_color": "#EEF2F6"`, icons pointing to `assets/logo.png` / `assets/favicon.png`.
2. **Create `web/sw.js`** (Lightweight PWA Service Worker):
   - Network-first for `/api/*` (never serve stale auth/roster JSON!).
   - Stale-while-revalidate / cache fallback for static assets (`css/`, `js/`, `assets/`) so iOS Home Screen launches feel instant.
   - Register in `login.html`, `user-portal.html`, and `index.html`.
3. **iOS Safari Standalone & Safe-Area Meta Tags** (all 3 HTML entry points):
   - `<meta name="viewport" content="width=device-width, initial-scale=1.0, viewport-fit=cover">`
   - `<meta name="apple-mobile-web-app-capable" content="yes">`
   - `<meta name="apple-mobile-web-app-status-bar-style" content="default">`
   - `<meta name="apple-mobile-web-app-title" content="RosterX">`
   - `<link rel="apple-touch-icon" href="assets/logo.png">`
   - `<link rel="manifest" href="manifest.webmanifest">`
   - Full `env(safe-area-inset-top/bottom/left/right)` CSS handling for iPhone Dynamic Island, notch, and home indicator + a subtle, dismissible iOS Safari **"Install RosterX on Home Screen"** tip banner (shown only on non-standalone iOS Safari).
4. **6 Android Brand Color Palettes + 3-State Theme (`web/css/tokens.css`, `web/css/neumorphic.css`, `web/js/themeController.js`)**:
   - Mirror Android `BrandPalettes` (`NeumorphicTokens.kt`) 1:1:
     - `tory_blue` (`#174EAC` / `#7294CC`) *(default)*
     - `crimson_coral` (`#FF5252` / `#FB7185`)
     - `emerald_green` (`#059669` / `#34D399`)
     - `indigo_violet` (`#6366F1` / `#818CF8`)
     - `sunset_amber` (`#D97706` / `#FBBF24`)
     - `ocean_cyan` (`#0284C7` / `#38BDF8`)
   - Persist `rosterx_palette` + `rosterx_theme` (`system` / `light` / `dark`) in `localStorage` with anti-FOUC initialization.

---

## 2. Task 2 — Remove Unwanted Swap Request Features (Both Portals)
1. **User Portal (`web/user-portal.html`, `web/js/userPortal.js`)**:
   - Remove the `Request Shift Swap` desktop nav tab, mobile nav tab (`data-tab="request_swap"`), `userTabContent_request_swap` section, and swap-submission handlers.
2. **Admin Console (`web/index.html`, `web/js/app.js`)**:
   - Remove the `Swaps & Leaves` nav tab (`data-tab="swaps"`), `tabContent_swaps` section, and `swapsModal` HTML/handlers.
   - **CRITICAL**: Keep `SW*` shift codes, color styling, and badge rendering 100% intact inside the Roster Matrix / Month Roster grids!

---

## 3. Task 3 — Adaptive Mobile/iOS Neumorphic Shell (Portrait Bottom Nav + Landscape Left Rail)
Build shared **`web/css/mobile-shell.css`** and **`web/js/mobileNavShell.js`** used across BOTH `user-portal.html` and `index.html`:
1. **Mobile Portrait (`< 768px`)**:
   - Exact Android-style **5-tab Fluid Neumorphic Bottom Navigation Dock** with soft elevated surface, concave active highlight / floating active bubble, icon + crisp label, and `env(safe-area-inset-bottom)` clearance.
   - Compact Neumorphic Top Bar: RosterX logo + title on left, 3D Notification Bell + unread crimson badge (for duty engineers) + raised circular Profile Avatar Pill on right.
2. **Mobile Landscape (`max-height: 520px` and `orientation: landscape`)**:
   - **Ultra-slim `42px` Top Bar** (safe top + horizontal cutout insets).
   - **Left Vertical `FluidNeumorphicNavRail`** (`72px` wide + `env(safe-area-inset-left)` safe inset) with raised active circular Neumorphic bubble!
   - Multi-column / 2-pane responsive layouts for My Calendar (full 5–6 week month fits on left + Sync & Legend on right!), Dashboard, Team Shifts, More Hub, and Admin tabs.
   - **Month Roster / Rosters tab** enters dedicated **full-screen landscape** (hides Top Bar + Side Rail so the 31-day grid owns 100% of the screen — matching Android!).
3. **Desktop (`>= 1024px`)**:
   - Preserves the rich wide desktop Operations Console & Portal header/nav while exposing all new tabs & modals cleanly!

---

## 4. Task 4 — Exact 1:1 Role Routing & 5-Tab Parity (`web/js/authService.js`, `web/login.html`, `web/user-portal.html`, `web/index.html`)
1. **Role Routing**:
   - `admin` → `index.html` (5 tabs: **`Dashboard`**, **`Register`**, **`Rosters`**, **`Broadcast`**, **`More`**)
   - `manager` → `user-portal.html` in Manager Mode (5 Android Manager tabs: **`Dashboard`** [Manager Shift Radar + System Health], **`Team Shifts`**, **`Month Roster`**, **`Broadcast`**, **`More`**)
   - `engineer` / `apprentice` → `user-portal.html` in Engineer Mode (5 Android Engineer tabs: **`Dashboard`**, **`Team Shifts`**, **`My Calendar`**, **`Month Roster`**, **`More`**)
2. **Deep-Link Hash Handling**:
   - Parse `window.location.hash` (`#roster`, `#notifications`, `#credentials`, `#profile`, `#dashboard`) on load so `/open/*` email links jump straight to the matching tab/drawer/subscreen!

---

## 5. Task 5 — Add All Missing Android Features to `web/`
Keep code modular with clean ES modules inside `web/js/`:
1. **`web/js/calendarSync.js` — Personal `Sync to Calendar` (`.ics`) Modal**:
   - Triggered ONLY from **My Calendar** (`Sync to Calendar` raised Neumorphic pill — never on the team Month Roster header!).
   - Authentic frosted-blur Neumorphic modal (`max-width: 520px`, zero white halo) showing:
     - Personal scope summary (`<Your Name> • @username • Personal schedule only`, `XX Shifts • Timed`, `YY Off/Leave • All-Day`).
     - Shift Reminder chips (`None`, `15m`, `30m` default, `1h` — saved in `localStorage`).
     - **Working shifts** (`FS 07:30–16:30`, `FG 08:30–17:30`, `G 09:30–18:30`, `SG 11:00–20:00`, `SS 13:00–22:00`, `NS 21:00–07:30 +1d`): timed IST (`Asia/Kolkata`), `TRANSP:OPAQUE` (Busy), selected `VALARM`.
     - **Non-working days** (`OFF`, `L`, `SW*` Swap Off): `VALUE=DATE` All-Day events (no clock time), `TRANSP:TRANSPARENT` (Free), no `VALARM`.
     - Downloads/opens `rosterx_<username>_YYYY_MM.ics` (`text/calendar`) — natively launches **Apple Calendar "Add All Events"** on iOS Safari!
2. **`web/js/credentialTracker.js` — Server Credential Tracker (in `More` Hub for ALL 3 Roles)**:
   - KPI summary cards (`Total`, `Expiring Soon`, `Expired`), search bar, status filter chips (`All`, `Expiring Soon`, `Expired`, `Healthy`), server-grouped cards.
   - 1-tap **Update Date** renewal modal (past/today date guard) for authorized spokespersons, Managers, and Admins.
   - **Add / Edit / Delete Tracker** + **Audit Logs** drawer for Managers & Admins.
3. **`web/js/myTeamView.js` — My Team Directory (in `More` Hub for Engineer & Manager)**:
   - Searchable Operations Management + Shift Engineers & Apprentices directory with role badges, direct `tel:` Call and `https://wa.me/...` WhatsApp buttons.
4. **User Portal 5-Tab Feature Completion (`web/user-portal.html`, `web/js/userPortal.js`)**:
   - **Tab 1 (`Dashboard`)**: Welcome Hero + live Next Shift & Rest Day countdown timers + Today/Tomorrow Shift cards + Monthly Duties & Rest metrics + Today's Team Celebrations banner (with animated Wishes modal!); Manager mode shows Manager Shift Radar + System Health!
   - **Tab 2 (`Team Shifts`)**: Date-navigable shift groups (`TODAY` / `YESTERDAY` / `TOMORROW` badge + Jump to Today FAB) with Call & WhatsApp buttons.
   - **Tab 3 (`My Calendar` — Engineer / `Month Roster` — Manager)**: Interactive color-coded monthly grid with breathing today glow + `Sync to Calendar` + Shift Legend & Timings.
   - **Tab 4 (`Month Roster` — Engineer / `Broadcast` — Manager)**: Full team monthly shift matrix with sticky engineer names, search filter, Day/Night coverage footers, and full-screen landscape mode! (For Manager, embeds full Broadcast Console with `@` mentions + templates + history!)
   - **Tab 5 (`More` — Operations Hub)**: Raised Neumorphic tiles launching **Server Credential Tracker** and **My Team**!
   - **Profile & Settings View** (via top-right avatar pill): Standard Profile Header Card + Edit Profile modal (with Photo Upload & **Remove Photo** confirmation modal + OTP password reset), Tenure & dynamic Birthday countdown (`BIRTHDAY` vs `BIRTHDAY IN`), Weekly Offs pills, Account Details (copy email / dial phone), **Appearance & 6 Brand Palettes**, Server Status, About RosterX version card, Sign Out!
   - **Notification Bell Drawer**: 3D Bell icon + unread count badge + drawer with `@username` / `@name` / `@all` mention resolution + client-side expired celebration auto-sweep (`< today IST`).
5. **Admin Console Parity (`web/index.html`, `web/js/app.js`, `web/js/broadcastController.js`)**:
   - Exact 5 Admin tabs on mobile (`Dashboard`, `Register`, `Rosters`, `Broadcast`, `More`) with Fluid Neumorphic Bottom Nav (portrait) + Left Side Rail (landscape) + Full-screen Landscape `Rosters`.
   - `More` Hub → **Server Credential Tracker** + **Server Management** (diagnostics & live logs).
   - **Broadcast Console**: Add Title `@` mention autocomplete & live resolution (matching Android v1.33.0) + expired celebration auto-sweep in Broadcast History.
   - **Zero-Knowledge Team Management (`Register`)**: Remove manual password input fields from Add/Edit Account modals (matching Android v1.33.0 — new accounts onboard via OTP email).

---

## 6. Verification, Version Bump (`v2.0.0`, Build `143`) & Mandatory Push
1. Start `python server.py` locally and verify end-to-end across **iPhone portrait (390×844)**, **iPhone landscape (844×390)**, and **Desktop (1440×900)** viewports:
   - Login flows for Engineer, Manager, and Admin (exact 5 tabs each, zero Swap tabs!).
   - My Calendar + `.ics` download output (valid RFC-5545 with timed Busy shifts + `VALUE=DATE` Free OFF/L/SW*).
   - Month Roster full-screen landscape intact.
   - More Hub → Credential Tracker & My Team Directory working against live APIs.
   - 6 Brand Palettes + Dark/Light/System switching with zero console errors.
   - `manifest.webmanifest` and `sw.js` load cleanly with HTTP 200.
   - Strict Zero-PII & zero `trycloudflare.com` hygiene check.
2. Bump semantic version to **`v2.0.0`** (Major Release):
   - `android/app/build.gradle.kts`: `versionCode = 143`, `versionName = "2.0.0"`
   - `server.py`: `SERVER_VERSION = "2.0.0"`
   - Verify Android still builds cleanly (`cd android && .\safe_build.bat assembleDebug`) so APK and Web stay 100% version-synced.
3. Update `plan_vijayan.md` (`COMPLETED`), `AGENT_HANDOFF.md`, and `android/AGENT_HANDOFF.md`, commit with semantic message, and **immediately run `git push origin main`**!
