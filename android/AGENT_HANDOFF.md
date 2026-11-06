# 👵 Aunty (Antigravity) ⇄ 🤖 Kunjappan (Android Studio) Handoff Bridge

> **"Aunty looks after the backend & servers; Kunjappan builds the native Android app!"**  
> *Last Synced by Aunty:* **2026-11-06 20:25 IST** 👵⚡🌴

---

## 📌 Active System State & Monorepo Location
- **Unified Workspace**: `c:\shefeeq\PYTHON\RosterX`
- **Native Android Project Root**: `c:\shefeeq\PYTHON\RosterX\android`
- **Semantic Version**: **`1.14.3`** (Android `versionCode: 46`, Server `SERVER_VERSION = "1.14.3"`)
- **Previous Versions**: `1.14.2` (Build 45), `1.14.1` (Build 44), `1.14.0` (Build 43), `1.13.1` (Build 42), `1.13.0` (Build 41), `1.12.1` (Build 40), `1.12.0` (Build 39), `1.11.8` (Build 38), `1.11.7` (Build 37), `1.11.6` (Build 36), `1.11.5` (Build 35), `1.11.4` (Build 34), `1.11.0` (Build 30), `1.10.0` (Build 29)
- **Production Server**: ☁️ **Render Cloud (100% Pure)**  
  🌐 `https://rosterx-vc30.onrender.com`  
  📱 **Android Base URL**: `https://rosterx-vc30.onrender.com/api/`
- **Local Asset Sync File**: `android/app/src/main/assets/server_url.txt` (`https://rosterx-vc30.onrender.com/`)
- **Database Engine**: MongoDB Atlas `Cluster0` (Database: `rosterx`, Collections: `engineers`, `rosters`, `roster_backups`, `operational_policies`, `notifications`, `password_resets`, `audit_logs`)
- **Cross-Platform Parity Specification**: [`ANDROID_PARITY.md`](../ANDROID_PARITY.md)

---

## 🎨 Major UI & Architecture Upgrades in v1.14.3

### 1. Pure Neumorphic Dual Theming & Light Mode Visibility Overhaul
- **Pill & Header Contrast**: Fixed washed-out text for `#userDisplayName` and user status pill on light canvases (`#EEF2F6`); now renders crisp slate `#1E293B` on light and `#F8FAFC` on dark.
- **Table Zebra Striping Purge**: Eradicated dark translucent alternating row striping (`bg-slate-900/30`) in `renderMyMonthlyShifts()`. Replaced with clean light surfaces (`bg-slate-50/80` in light mode, `bg-slate-900/30` in dark mode).
- **High-Contrast Duty Badges**: Upgraded duty status labels (`text-indigo-700 dark:text-indigo-400`, `text-amber-700 dark:text-amber-400`, `text-emerald-700 dark:text-emerald-400`, `text-rose-700 dark:text-rose-400`) ensuring perfect legibility on white cards.
- **Universal Guard in `tokens.css`**: Enforced universal high-contrast light mode safety rules for `.text-slate-100`, `.text-slate-200`, and table headers.

### 2. User Identity Multi-Field Matching (v1.14.2)
- Fixed user identity resolution bug where users with administrative tags were filtered out from the active roster and wrongly defaulted to `pool[0]`. Multi-field username/email/full_name matching now anchors to the logged-in engineer with zero fallbacks.

---

## 🤖 Direct Briefing for Kunjappan (Android Studio Mobile Agent)

1. **Version Update**:
   - `android/app/build.gradle.kts` has been bumped to `versionName = "1.14.3"` and `versionCode = 46`.
2. **Tokens & Theme Parity**:
   - Web tokens in `tokens.css` now match `NeumorphicTokens.kt` 1:1.
   - Refer to `ANDROID_PARITY.md` for exact component mappings.
3. **Tab Architecture Sync**:
   - The 5 tabs in Android `AdminMainScreen.kt` (`DashboardTab`, `RosterMatrixTab`, `ManageTeamTab`, `RequestsTab`, `DiagnosticsTab`) match web tabs 1:1.

---

*Signed with love & clean code,*  
**Aunty (Antigravity Lead Full-Stack Architect)** 👵⚡
