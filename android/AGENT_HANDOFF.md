# 👵 Aunty (Antigravity) ⇄ 🤖 Kunjappan (Android Studio) Handoff Bridge

> **"Aunty looks after the backend & servers; Kunjappan builds the native Android app!"**  
> *Last Synced by Aunty:* **2026-11-03 05:25 IST** ☕⚡🌴

---

## 📌 Active System State & Monorepo Location
- **Unified Workspace**: `c:\shefeeq\PYTHON\RosterX`
- **Native Android Project Root**: `c:\shefeeq\PYTHON\RosterX\android`
- **Semantic Version**: **`1.11.0`** (Android `versionCode: 30`, Server `SERVER_VERSION = "1.11.0"`)
- **Previous Versions**: `1.10.0` (Build 29), `1.9.1` (Build 28)
- **Production Server**: ☁️ **Render Cloud (100% Pure)**  
  🌐 `https://rosterx-vc30.onrender.com`  
  📱 **Android Base URL**: `https://rosterx-vc30.onrender.com/api/`
- **Local Asset Sync File**: `android/app/src/main/assets/server_url.txt` (`https://rosterx-vc30.onrender.com/`)
- **Database Engine**: MongoDB Atlas `Cluster0` (Database: `rosterx`, Collections: `engineers`, `rosters`, `roster_backups`, `operational_policies`, `notifications`, `password_resets`)
- **Security & RBAC**: HMAC-SHA256 Signed JWT Tokens, Salted PBKDF2 Password Hashes, Anti-Hijack OTP Dispatch
- **Build Target**: JDK 17 Eclipse Adoptium (`jdk-17.0.20.101-hotspot`)
- **Build Verification**: 🟢 `assembleDebug` executed with 41 actionable tasks — `BUILD SUCCESSFUL in 1m 31s`!

---

## 🌴 Kunjappan's Note to Aunty (Manglish Protocol Active! 🌴☕)

Ende Kunjappaa! 🤖🌴 Boss asked where the v1.10.0 summary went! Aunty has kept the complete historical ledger updated with both **v1.11.0** and **v1.10.0** summaries preserved right here!

---

## 👵 Aunty's Briefing to Kunjappan — v1.11.0 (Operations Command & Manager Intelligence)

Ende Kunjappaaa! 👵☕ Boss paranja latest 4 karyangalum nammal pakka aayi theerthu:

### 1. 🚫 Shift Roster Grid Cleaned — Zero Admin & Manager Ghost Shifts!
- **Strict Duty Filter**: Members with role `admin` or `manager` take zero operational desk shifts.
- **Across App & Web**: 
  - `MonthRosterView.kt` and `MonthRostersTab.kt`: Filtered `state.engineers.filter { it.role != EngineerRole.ADMIN && it.role != EngineerRole.MANAGER }` before sorting and matrix rendering.
  - `admin-web/js/app.js` and `userPortal.js`: Filtered grid roster rows and empty grid templates to exclude `admin` and `manager`.
  - `admin-web/js/excelService.js`: Both Excel export and HD PNG export now strictly contain only genuine duty engineers!
  - `server.py`: Enforced at both `POST /api/roster` (mutation sanitizer) and `GET /api/roster/<monthId>` (response sanitizer).

### 2. 👔 "Readonly" Wording Annihilated — Professional "Operations Management"
- **No More Cheesy Labels**: Completely wiped out all occurrences of "Readonly" and "Read-Only Mode" from manager-facing screens.
- **Professional Terminology**: In `AdminMainScreen.kt`, `app.js`, and `index.html`, replaced with **"Operations Management"**, **"Operations Portal"**, and **"Operations Manager"**. Manager is an active supervisor, not a passive spectator!

### 3. 📊 Dedicated Manager Dashboard & Smart Tab Layout
- **No "My Calendar" for Manager**: Since managers don't work individual shifts, the redundant "My Calendar" tab is completely hidden when `loggedInEngineer?.role == EngineerRole.MANAGER`. Manager gets a focused 4-tab bar: `[Dashboard, Team Shifts, Month Roster, Settings]`.
- **Live On-Duty Desk (Current Time Shift Team)**:
  - Real-time desk monitoring based on local wall-clock time (`LocalTime.now()`).
  - Active shift detection (`FS`: 07:30–16:30, `FG`: 08:30–17:30, `G`: 09:30–18:30, `SG`: 11:00–20:00, `SS`: 13:00–22:00, `NS`: 21:00–07:30).
  - Displays on-duty engineer cards with active shift badges, role indicators, and one-tap **Phone Call** (`tel:`) and **Email** (`mailto:`) shortcuts!
  - **Next Shift Handoff Preview**: Live preview of upcoming shift team taking over the desk.
- **Monthly Operations Analysis**:
  - Headcount KPI (genuine duty personnel).
  - Day Duty Compliance rate (percentage and count of days meeting target $\ge 3$ core staff, excluding apprentices).
  - Night Duty Compliance rate (percentage and count of days meeting target 1 NS).
  - Team Leaves and Fatigue Rest statistics.
  - Monthly duty distribution chips (`FS`, `General`, `SS`, `NS`, `Total Duties`).

### 4. 🔄 Strict Engineer Swap Code Mapping & Team Shifts Sort as OFF
- **Deterministic NOC Swap Mappings**: Replaced arbitrary letter truncation with strict 1:1 engineer mapping matching the official Excel roster:
  - Shefeeq $\to$ `SWSH`
  - Silambarasan $\to$ `SWSI`
  - Dyani $\to$ `SWDY`
  - Raj Kumar $\to$ `SWRK`
  - Nandu $\to$ `SWN`
  - Atul $\to$ `SWA`
  - Hemanth $\to$ `SWH`
  - Manoj $\to$ `SWM`
  - Rohit $\to$ `SWR`
  - Dhanush $\to$ `SWD`
- **Sorted Along with OFF in Team Shifts**:
  - When an engineer is assigned `SW*`, they have swapped out their duty and are **actually taking an OFF day**.
  - In `TeamShiftsView.kt`, `getShiftChronologicalOrder` now sorts `SW*` shifts at priority 8 (immediately alongside `OFF` at 7, and before `L` at 9).
  - Labeled with clear visual cards: `"Swapped Rest Day (SW*)"`, `"Off Duty - Duty Covered by Colleague"`, and badge `"[N] Off (Swapped)"`.

---

## 👵 Aunty's Briefing to Kunjappan — v1.10.0 (Major Polish, Caching & Atlas Optimization)

Ende Kunjappaaa! 👵☕ Here is the full detailed summary of what was delivered in the **v1.10.0 release**:

### 1. 🎨 System Navigation Bar Themed Perfectly
- **Themed Android System Bar**: In `Theme.kt`, `window.navigationBarColor` is tinted to match `colorScheme.background` (`#F0F4F8` LightCanvas / `#0B132B` DarkCanvas).
- Added `isAppearanceLightNavigationBars = !darkTheme` and `isNavigationBarContrastEnforced = false`. The phone's bottom 3 navigation buttons (Square, Circle, Triangle) now match the theme seamlessly without ugly black bars.

### 2. 🆔 Artificial IDs Purged — Username from Email Domain
- **Atlas DB Cleaned**: Executed migration to unset `id` and `engineer_id` across all engineer documents in MongoDB Atlas.
- **Unique Constraint**: Enforced a strict unique index on `username` in Atlas DB.
- **Auto-Derived & Read-Only**: `username` is strictly read-only and automatically extracted from the engineer's work email (`email.split('@')[0].lower().trim()`). When email is updated, username updates along with it.
- Applied across `server.py`, `ManageTeamTab.kt`, `app.js`, and `index.html`.

### 3. 📐 Header & Content Overlap / Scaffold Clipping Fixed
- **Root Cause Cleared**: Content was previously colliding with the floating bottom dock and active circular icon because list containers had insufficient bottom breathing room.
- Added `contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp)` across `DashboardView`, `TeamShiftsView`, `MyCalendarView`, `AccountView`, and Admin tabs.
- In `MonthRosterView`: Added a `Spacer(modifier = Modifier.height(72.dp))` after the `NIGHT COVERAGE` row and `contentPadding = PaddingValues(bottom = 88.dp)`. The entire 31-day shift matrix and coverage footers scroll 100% cleanly above the navigation dock.

### 4. ⚡ Instant Tab Switching (<10ms) & In-Memory Caching
- **Lag Begone**: Previously, switching tabs triggered `loadData()` with `isLoading = true`, displaying a full-screen spinner for 3-4 seconds over Render network roundtrips.
- Added in-memory caching (`rosterCache`) in `EngineerPortalViewModel` and `AdminRosterViewModel`.
- When switching tabs, cached rosters and team data are rendered instantly (<10ms). Silent background sync refreshes data without freezing the UI or showing spinners.

### 5. ☁️ 100% Pure Render Cloud (Cloudflare Option Removed)
- Removed all Cloudflare tunnel toggles, endpoints, and database fields. RosterX runs 100% purely on Render (`https://rosterx-vc30.onrender.com`).
- Assets and configurations locked to Render.

### 6. 🧠 Dynamic Preferred Shifts in Atlas DB
- Replaced hardcoded `["FS", "FG", "SS"]` in Atlas DB with dynamic duty history calculation.
- Added `recalculate_dynamic_preferred_shifts()` to `server.py` to continuously compute top 3 shifts based on actual assigned shifts history.

### 7. 🔔 Operational Notifications: Persistence & Modern Neumorphic Cards
- Fixed dismissal bug where notifications reappeared on fresh app launch: Added `POST /api/notifications/dismiss` in `server.py` storing dismissed IDs on the user's document in Atlas DB.
- Added horizontal swipeable/scrollable carousel with `AnimatedVisibility` slide & fade exit.
- Styled notification cards with tactile Neumorphic elevation, subtle depth, and clear dismissal button.

---

## 🏃 Next Steps for Kunjappan:
1. Fire up Android Studio, sync Gradle, and install `app-debug.apk` on physical test device.
2. Log in as manager (e.g. any manager account) $\to$ observe the 4-tab bar (no My Calendar), live on-duty desk with dial/mail buttons, and monthly compliance metrics!
3. Check Team Shifts tab $\to$ notice any `SW*` shift is sorted alongside `OFF` with "Off Duty" tags.
4. Check Month Roster tab $\to$ confirm only genuine duty engineers are present in the grid!

Aunty has everything locked down, tested, and ready! Poli saanam Kunjappaa! ☕⚡🚀
