# 👵 Aunty (Antigravity) ⇄ 🤖 Kunjappan (Android Studio) Handoff Bridge

> **"Aunty looks after the backend & servers; Kunjappan builds the native Android app!"**  
> *Last Synced by Aunty:* **2026-11-09 01:15 IST** 👵⚡🌴

---

## 📌 Active System State & Monorepo Location
- **Unified Workspace**: `c:\shefeeq\PYTHON\RosterX`
- **Native Android Project Root**: `c:\shefeeq\PYTHON\RosterX\android`
- **Semantic Version**: **`1.19.0`** (Android `versionCode: 63`, Server `SERVER_VERSION = "1.19.0"`)
- **Previous Versions**: `1.18.0` (Build 62), `1.17.0` (Build 61), `1.16.0` (Build 60), `1.15.0` (Build 59), `1.14.15` (Build 58), `1.14.14` (Build 57), `1.14.13` (Build 56), `1.14.12` (Build 55), `1.14.11` (Build 54), `1.14.10` (Build 53), `1.14.9` (Build 52), `1.14.8` (Build 51), `1.14.7` (Build 50), `1.14.6` (Build 49), `1.14.5` (Build 48), `1.14.4` (Build 47), `1.14.3` (Build 46), `1.14.2` (Build 45), `1.14.1` (Build 44), `1.14.0` (Build 43), `1.13.1` (Build 42), `1.13.0` (Build 41), `1.12.1` (Build 40), `1.12.0` (Build 39), `1.11.8` (Build 38), `1.11.7` (Build 37), `1.11.6` (Build 36), `1.11.5` (Build 35), `1.11.4` (Build 34), `1.11.0` (Build 30), `1.10.0` (Build 29)
- **Production Server**: ☁️ **Render Cloud (100% Pure)**  
  🌐 `https://rosterx-vc30.onrender.com`  
  📱 **Android Base URL**: `https://rosterx-vc30.onrender.com/api/`
- **Local Asset Sync File**: `android/app/src/main/assets/server_url.txt` (`https://rosterx-vc30.onrender.com/`)
- **Database Engine**: MongoDB Atlas `Cluster0` (Database: `rosterx`, Collections: `engineers`, `rosters`, `roster_backups`, `operational_policies`, `notifications`, `password_resets`, `audit_logs`)
- **Tailscale Static IP for Phone**: `100.71.241.36`
- **Cross-Platform Parity Specification**: [`ANDROID_PARITY.md`](../ANDROID_PARITY.md)
- **Remote Agent Guide**: [`REMOTE_AGENT_GUIDE.md`](../REMOTE_AGENT_GUIDE.md)

---

## 🚀 Major Upgrades in v1.19.0

### 1. Widget Loading View & 12-Hour AM/PM Time Format
- **Zero-Height Loading View**: Implemented `widget_empty_loading.xml` inside `RosterXWidgetService.getLoadingView()`, completely removing the default system `"Loading..."` text above widget items.
- **12-Hour AM/PM Shifts**: Updated `shiftRegistry` in `RosterXAppWidgetProvider` from 24h format to standard 12-hour AM/PM format (`07:30 AM – 04:30 PM`, `09:00 PM – 07:30 AM (+1)`).

### 2. Terminology Update (Staff $\rightarrow$ Engineer)
- Replaced all user-facing instances of `"Staff"` with `"Engineer"` / `"Engineers"` across the Manager Dashboard cards (`DutyGroupCard`) and Widget section headers (`⚡ ON DUTY`, `☕ REST & LEAVE`), with pluralization checks.

### 3. Overnight Night Shift (NS) Rollover & Active Duty Accuracy
- **Midnight Transition Fix (00:00 - 07:30 AM)**: 
  - Yesterday's Night Shift engineer (whose shift runs from 21:00 yesterday to 07:30 today) is now correctly displayed under the **Night Shift** card in the app and pinned to the top of `⚡ ON DUTY` in the widget with `[• ACTIVE NOW]`.
  - Excluded from "Scheduled Off & Leave" / rest while on active duty overnight.
  - Today's upcoming NS engineer is displayed without the active tag until their shift starts at 09:00 PM tonight.

---

## 🚀 Major Upgrades in v1.18.0

### 1. Manager Widget 2-Card Redesign & Chronological Shift Sorting
- **Clear Summary String**: Updated to `"Total on Duty: 4 Engineers | Rest: 5 Engineers"` (no confusing day/night breakdown).
- **Two High-Density Cards**:
  - **⚡ ON DUTY**: Engineers listed strictly sorted by shift start timing (`FS` 07:30, `FG` 08:30, `G` 09:30, `SG` 11:00, `SS` 13:00, `NS` 21:00).
  - **☕ REST & LEAVE**: Rest day staff on `OFF`, `SW*`, and `L`.
- **Ultra-Compact RemoteViews**: Reduced padding and badge sizes (`24dp`), enabling 6-8 engineers visible at a glance with minimal scroll.

### 2. Theming & Drop Shadow Glow Unification
- **Dynamic Accent Glow**: Replaced hardcoded salmon/pinkish drop shadow (`0x59FF5252`) with dynamic `LocalThemeAccent.current.copy(alpha = 0.35f)`, ensuring all neumorphic CTA glows and the floating bottom navigation bubble match Tory Blue `#174EAC` / `#7294CC` (or selected palette).
- **Collapsible Theme Card**: The "Appearance & Theme" card in the Settings tab is now collapsible, **closed by default**, displaying an active summary pill (`Dark • Tory Blue`) when collapsed.

### 3. Manager Dashboard Layout Polish & Health Liveliness
- **Prevent Staff Counter Squeeze**: Replaced the right header row in `DutyGroupCard` with compact micro-tagging (`CURRENT`) and guaranteed single-line text for staff pills (`maxLines = 1, softWrap = false`), preventing wrap onto two lines.
- **Liveliness Feedback on Health Check**: Tapping refresh on the System & DB Health card now provides immediate visual feedback: 360° continuous spinning icon, amber pulsing status banner with `"Pinging Atlas M0 Cluster..."`, and animated `"Pinging..."` sync state.

---

## 🚀 Major Upgrades in v1.17.0

### 1. Global Theming Engine & Color Palette Customization
- **Theme Mode Toggle**: Implemented System, Light, and Dark mode switching in Account/Settings tab, persisted via `UserSessionManager`.
- **Dual-State WCAG Brand Palettes**:
  - **Tory Blue (`#174EAC` Light / `#7294CC` Dark) — Default**: Complies with the 20-30% desaturation and WCAG 4.5:1 text / 3:1 UI rule for dark slate surfaces (`#1E293B`) without changing brand hue (~218°).
  - Additional palettes: **Crimson Coral** (`#FF5252` / `#FB7185`), **Emerald Green** (`#059669` / `#34D399`), **Indigo Violet** (`#6366F1` / `#818CF8`), **Sunset Amber** (`#D97706` / `#FBBF24`), **Ocean Cyan** (`#0284C7` / `#38BDF8`).
- **Dynamic Neumorphic Tokens**: `NeumorphicTokens.Accent` and `NeumorphicTokens.AccentSalmon` dynamically resolve to `LocalThemeAccent.current`.

### 2. Manager UI Enhancements & Compact Roster Widget
- **Compact Manager Home Screen Widget**: Replaced split cards with a sleek, compact, scrollable `ListView` showing all engineers on duty today with shift badges, timings, and active status (`RosterXWidgetService` + `RemoteViewsFactory`).
- **Sleek Manager Dashboard Header**: Removed verbose "Operation manager" and "Operation Hub" headlines; made header ultra-compact and focused.
- **Layout Order**: Reordered dashboard to place **Today's Shift Radar** *first*, followed by the **System & Database Health** card.
- **Accurate Operational Shift Windows**:
  - Morning (`FS`, `FG`, `G`, `SG`): **07:30 AM – 08:00 PM**
  - Afternoon (`SS`): **01:00 PM – 10:00 PM**
  - Night (`NS`): **09:00 PM – 07:30 AM** (next day)
  - Granular minute-by-minute active duty detection for each shift code.
- **Resigned Staff Exclusion**: Resigned engineers (e.g. `RAJ KUMAR`, resigned Oct 25, 2026) are strictly excluded from shifts, countings, and scheduled off pools in all subsequent months.

---

## 🚀 Major Upgrades in v1.16.0

### 1. Manager Profile Reconstruction & Shift Exclusion
- **Zero Shift Assignment for Managers**: `Engineer.isManagementAccount` unconditionally returns `true` for `EngineerRole.MANAGER`. Managers are automatically purged from Team Shifts, Month Roster grids, shift assignments, and swap pools.
- **Dynamic 4-Tab Navigation**: Managers now see 4 tabs (`Dashboard`, `Team Shifts`, `Month Roster`, `Settings`), completely removing the irrelevant "My Calendar" tab, while standard engineers retain all 5 tabs.
- **Advanced Operations Dashboard (`ManagerDashboardView`)**:
  - **Operations Hub Hero**: Dedicated manager welcome card with `OPERATIONS MANAGER` badge.
  - **Server & MongoDB Atlas Health Radar**: Live status indicator (connected dot, latency in ms, cluster `Cluster0`, database `rosterx`, environment `Render Cloud / Atlas M0`, and on-demand refresh).
  - **Today's Shift Radar (`ManagerRadarSection`)**: Categorized into Morning (`FS/FG/G/SG`), Afternoon (`SS`), Night (`NS`), and Rest (`OFF/L/SW*`) duty groups.
  - **Active Duty Now Detection**: Live IST hour detection highlights currently active engineers on duty with an `ACTIVE NOW` badge.
  - **Purged Personal Shift Cards**: Removed today & tomorrow personal shift cards, monthly duties counters, and rest day countdowns from the Manager's dashboard.
- **Operations Manager Home Screen Widget**:
  - When a manager account is logged in, `RosterXAppWidgetProvider` automatically renders the Operations Radar widget showing current on-duty staff, active duty badge, upcoming shift handoff, and total daily coverage breakdown.

---

## 🚀 Major Upgrades in v1.15.0

### 1. Antigravity Cloud Remote Agent & Tailscale Wireless ADB
- **Full Cloud Agent Migration**: Added complete headless automation for building and debugging the Android application directly from the Antigravity Cloud Remote Agent connected to GitHub [`shefeeqkk/RosterX`](https://github.com/shefeeqkk/RosterX).
- **Automation Scripts**:
  - `scripts/setup-remote-agent.sh`: Provisions OpenJDK 17, ADB, Python, and headlessly connects Tailscale via `TAILSCALE_AUTHKEY`.
  - `scripts/remote-deploy.sh`: Builds debug APK via `./gradlew assembleDebug --no-daemon`, connects wirelessly via Tailscale to `100.71.241.36:5555`, installs the APK, launches MainActivity, and captures verification screencap.
  - `scripts/tailscale-connect.sh`: Diagnostic helper checking Tailscale status, tailnet peers, and pinging target device.
- **Dynamic Port & Multi-Wi-Fi Persistence**:
  - By binding the ADB daemon to `0.0.0.0:5555` via `adb tcpip 5555`, the connection survives home Wi-Fi, office Wi-Fi, and 5G cellular switches without losing the static Tailscale IP (`100.71.241.36`).
  - Fallback prompt handles dynamic Android 11+ wireless ports gracefully if the phone is rebooted.

---

## 🎨 Major Upgrades & Fixes in v1.14.15

### 1. Surgical Account Deletion Targeting
- In `server.py` (`POST /api/engineers/delete`) and `AdminRosterViewModel.kt`:
  - When deleting an account, queries strictly target unique `username` or `id`.
  - Removed broad `$or` name matching that previously caused multiple accounts sharing the same display name (e.g. `"System Administrator"`) to be deleted together.
  - Deleting an individual duplicate now deletes *only* that specific account.
- Master administrator account is safely preserved in MongoDB Atlas with single `@admin` account (`kkshefeeq005@gmail.com`).

### 1. Server & Public Gateway Configuration Collapsed by Default
- In `EngineerPortalScreen.kt`, `showGatewaySettings` now defaults to `false`.
- The expandable card is collapsed on screen load, keeping the Settings tab clean and uncluttered.

### 2. Widget Static Shift Times (No Stale Minutes)
- In `RosterXAppWidgetProvider.kt`:
  - Replaced minute-by-minute countdowns (`Shift Ends in: 05h 29m` / `Duty Starts in: 10h 09m`) with clean, static boundary times:
    - Active duty: `"Shift Ends at $endTime"` (e.g., `Shift Ends at 20:00`)
    - Upcoming today: `"Duty Starts at $startTime"` (e.g., `Duty Starts at 11:00`)
    - Next day duty: `"Next Shift: Tomorrow at $nextStart"`
    - Future duty: `"Next Shift: EEE, MMM dd at $nextStart"`
    - Rest cycle: `"Scheduled Rest Day (OFF)"`
  - Kept exact AlarmManager wakeup triggers so the widget seamlessly flips status right at start/end boundaries without polling or showing inaccurate minute countdowns.
  - In-app dashboard live countdown with ticking seconds remains intact.

### 3. Team Account Edit Duplication Elimination
- In `server.py`:
  - Supported `original_username` parameter in `POST /api/engineers`.
  - When an account's username/domain ID is edited, `server.py` deletes the old document (`old_user`) while preserving existing salted PBKDF2 password hashes and creation metadata, and upserts the updated profile.
  - Prioritized `username` explicitly passed from client forms over email domain slicing, preventing accidental renaming.
- In `ManageTeamTab.kt`, `EngineerDto.kt`, and `RosterRepositoryImpl.kt`:
  - Carried `originalUsername` during edits so that renames replace the existing account in MongoDB Atlas rather than leaving duplicate records.

---

## 🤖 Direct Briefing for Kunjappan (Android Studio Mobile Agent)

1. **Version Bump**:
   - `android/app/build.gradle.kts` has been bumped to `versionName = "1.14.14"` and `versionCode = 57`.
2. **Settings Tab**:
   - Gateway settings card is now cleanly collapsed by default.
3. **Widget Timer**:
   - Displays clean static times (e.g. `Duty Starts at 11:00`, `Shift Ends at 20:00`) avoiding stale minute counters.
4. **Account Management**:
   - Team account editing replaces the record without duplication.

---

*Signed with love & clean code,*  
**Aunty (Antigravity Lead Full-Stack Architect)** 👵⚡
