# 👵 Aunty (Antigravity) ⇄ 🤖 Kunjappan (Android Studio) Handoff Bridge

> **"Aunty looks after the backend & servers; Kunjappan builds the native Android app!"**  
> *Last Synced by Aunty:* **2026-11-06 17:15 IST** 👵⚡🌴

---

## 📌 Active System State & Monorepo Location
- **Unified Workspace**: `c:\shefeeq\PYTHON\RosterX`
- **Native Android Project Root**: `c:\shefeeq\PYTHON\RosterX\android`
- **Semantic Version**: **`1.14.0`** (Android `versionCode: 43`, Server `SERVER_VERSION = "1.14.0"`)
- **Previous Versions**: `1.13.1` (Build 42), `1.13.0` (Build 41), `1.12.1` (Build 40), `1.12.0` (Build 39), `1.11.8` (Build 38), `1.11.7` (Build 37), `1.11.6` (Build 36), `1.11.5` (Build 35), `1.11.4` (Build 34), `1.11.0` (Build 30), `1.10.0` (Build 29)
- **Production Server**: ☁️ **Render Cloud (100% Pure)**  
  🌐 `https://rosterx-vc30.onrender.com`  
  📱 **Android Base URL**: `https://rosterx-vc30.onrender.com/api/`
- **Local Asset Sync File**: `android/app/src/main/assets/server_url.txt` (`https://rosterx-vc30.onrender.com/`)
- **Database Engine**: MongoDB Atlas `Cluster0` (Database: `rosterx`, Collections: `engineers`, `rosters`, `roster_backups`, `operational_policies`, `notifications`, `password_resets`, `audit_logs`)
- **Cross-Platform Parity Specification**: [`ANDROID_PARITY.md`](../ANDROID_PARITY.md)

---

## 🎨 Major UI & Architecture Upgrades in v1.14.0

### 1. Unified 5-Tab Architecture (Admin & Android Parity)
- **Tab 1 — Operations Dashboard** (`ph-gauge` / `DashboardTab`):
  * **#1 NOC 24/7 Coverage Radar**: Live shift assignments for the current day.
  * **#2 Rule Compliance Stream**: Live fatigue violations alert banner and metrics.
  * **#3 Operational Audit Stream**: Clean, genuine MongoDB `audit_logs` feed with Neumorphic empty states (synthetic disclaimers permanently eradicated).
- **Tab 2 — Roster Matrix** (`ph-table` / `RosterMatrixTab`):
  * Dynamic month navigation anchored to real IST date (`dateUtils.js`).
  * Sticky pinned engineer names (`.nm-table-sticky-col`) with horizontal touch scroll (`.nm-table-container`).
  * Responsive Shift Picker: slides up as a bottom sheet on mobile screens (<768px) with a grab handle, and centered modal on desktop.
- **Tab 3 — Team Directory** (`ph-users-three` / `ManageTeamTab`):
  * First-class operational tab for engineer directory, roles, statuses (Active/Resigned), weekly off anchors, and credentials.
- **Tab 4 — Swaps & Leaves** (`ph-arrows-left-right` / `RequestsTab`):
  * Live swap requests queue, approval workflow, and automated rest-period compliance validation.
- **Tab 5 — Hub & Diagnostics** (`ph-dots-three-circle` / `DiagnosticsTab`):
  * Host Uptime, Atlas roundtrip latency ping, registered counts, active 24h activity, RSS memory cards.
  * Live server terminal logs viewer with clear buffer and refresh stream controls.
  * Launchers for AI Scheduling Studio, Recovery Snapshots, and Operational Rules.

### 2. Universal Date Engine (`dateUtils.js`)
- Permanently anchored to Indian Standard Time (`Asia/Kolkata`, UTC+5:30).
- Eradicated 20+ copy-pasted month arrays and hardcoded `'OCTOBER'` literals across the entire codebase.
- Dynamic period select population ensures the app always boots cleanly into the current operational month (e.g., `2026-11` November 2026).

### 3. Light / Dark / System Neumorphic Theming
- Token mappings generated in `design-tokens.json` & `tokens.css` directly derived from `NeumorphicTokens.kt` (`LightCanvas: #EEF2F6`, `DarkCanvas: #1E293B`, `AccentSalmon: #FF5252`, `AccentCoral: #FF6B6B`).
- Instant anti-FOUC `<script>` in `<head>` ensures zero theme flash on page refresh.
- 3-Way ThemeToggle controller persisted across sessions in `localStorage`.

### 4. Shift Countdown Timer Refinement
- When shift has ended or during weekly offs, timer dynamically calculates: `"Next Shift Starts in: [HHh MMm SSs]"` with the next scheduled duty.

### 5. Mobile Bot FAB UX Polish
- Cutout 3D bot elevated to `bottom-28` on mobile viewports, strictly clearing the bottom navigation dock.
- Removed round disc background for a pure cutout 3D floating character.

---

## 🤖 Direct Briefing for Kunjappan (Android Studio Mobile Agent)

1. **Version Update**:
   - `android/app/build.gradle.kts` has been bumped to `versionName = "1.14.0"` and `versionCode = 43`.
2. **Tokens & Theme Parity**:
   - Web tokens in `tokens.css` now match `NeumorphicTokens.kt` 1:1.
   - Refer to `ANDROID_PARITY.md` for exact component mappings.
3. **Tab Architecture Sync**:
   - The 5 tabs in Android `AdminMainScreen.kt` (`DashboardTab`, `RosterMatrixTab`, `ManageTeamTab`, `RequestsTab`, `DiagnosticsTab`) now match web tabs 1:1.
4. **Shift Countdown Timer Alignment**:
   - Use `"Next Shift Starts in: {HHh MMm SSs}"` formatting across both platforms.

---

*Signed with love & clean code,*  
**Aunty (Antigravity Lead Full-Stack Architect)** 👵⚡
