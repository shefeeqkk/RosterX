# 👵 Aunty (Antigravity) ⇄ 🤖 Kunjappan (Android Studio) Handoff Bridge

> **"Aunty looks after the backend & servers; Kunjappan builds the native Android app!"**  
> *Last Synced by Aunty:* **2026-11-06 23:10 IST** 👵⚡🌴

---

## 📌 Active System State & Monorepo Location
- **Unified Workspace**: `c:\shefeeq\PYTHON\RosterX`
- **Native Android Project Root**: `c:\shefeeq\PYTHON\RosterX\android`
- **Semantic Version**: **`1.14.5`** (Android `versionCode: 48`, Server `SERVER_VERSION = "1.14.5"`)
- **Previous Versions**: `1.14.4` (Build 47), `1.14.3` (Build 46), `1.14.2` (Build 45), `1.14.1` (Build 44), `1.14.0` (Build 43), `1.13.1` (Build 42), `1.13.0` (Build 41), `1.12.1` (Build 40), `1.12.0` (Build 39), `1.11.8` (Build 38), `1.11.7` (Build 37), `1.11.6` (Build 36), `1.11.5` (Build 35), `1.11.4` (Build 34), `1.11.0` (Build 30), `1.10.0` (Build 29)
- **Production Server**: ☁️ **Render Cloud (100% Pure)**  
  🌐 `https://rosterx-vc30.onrender.com`  
  📱 **Android Base URL**: `https://rosterx-vc30.onrender.com/api/`
- **Local Asset Sync File**: `android/app/src/main/assets/server_url.txt` (`https://rosterx-vc30.onrender.com/`)
- **Database Engine**: MongoDB Atlas `Cluster0` (Database: `rosterx`, Collections: `engineers`, `rosters`, `roster_backups`, `operational_policies`, `notifications`, `password_resets`, `audit_logs`)
- **Cross-Platform Parity Specification**: [`ANDROID_PARITY.md`](../ANDROID_PARITY.md)

---

## 🎨 Major UI & Architecture Upgrades in v1.14.5

### 1. Top Bar Rearrangement & Matrix Tab Consolidation
- **Clean Single-Row Header**:
  - Removed duplicate and redundant buttons from the top bar: `btnOpenTeamModal` (covered by Team Directory tab), `btnOpenSwapsModal` (covered by Swaps & Leaves tab), and `btnOpenLogsModal` (covered by Hub & Diagnostics tab).
  - Consolidated matrix-specific controls into an elegant dedicated operations toolbar directly inside the **Roster Matrix** tab:
    * Month/Period selector (`#monthSelect`)
    * Constraints modal trigger (`#btnOpenConstraintsModal`)
    * Export Data button (`#btnExportXlsx`)
    * Import Data button (`#btnOpenImportModal`)
    * Recovery button (`#btnOpenRecoveryModal`)
    * Save & Publish button (`#btnSaveAtlasDirect`)
  - Top header now contains strictly core system utilities: Logo + Title on the left, and AI Assistant, Theme Toggle (Auto/Light/Dark), Admin Pill, and Sign Out on the right.

### 2. Marketing Slogan Purge Across Entire Web Portal
- Purged promotional taglines including `"Dynamic 24/7 NOC Roster • Zero-Hardcoding • Gemini AI Smart Swaps"`, `"24/7 Intelligent NOC Operations Portal"`, and `"24/7 Shift Rotation & Real-time Duty Briefings"`.
- Replaced with clean, professional operational titles across:
  * `admin-web/index.html` (Operations Console Header, Dashboard Banner, Coverage Radar)
  * `admin-web/user-portal.html` (User Portal Header, Schedule Banner, Handover Coordination, Duty Table)
  * `admin-web/login.html` (Portal Header)

### 3. Mobile View Compactness & Horizontal Scroll Prevention
- Enforced strict viewport constraints (`max-width: 100vw; overflow-x: hidden;`) to prevent mobile horizontal page wobbling.
- Added smooth `.no-scrollbar` styling to tab docks and rule bars for swipable horizontal navigation.
- Reduced vertical spacing and padding on mobile screens (`p-2.5 sm:p-4 lg:p-5`) for higher information density and less vertical scrolling.

---

## 🤖 Direct Briefing for Kunjappan (Android Studio Mobile Agent)

1. **Version Update**:
   - `android/app/build.gradle.kts` has been bumped to `versionName = "1.14.5"` and `versionCode = 48`.
2. **Compact Layout Consistency**:
   - Web has transitioned to a compact, non-wobbling layout. Keep Android Compose screens streamlined with tight padding on phones (`8.dp` - `12.dp`) and clean sticky top bars.
3. **No Promotional Clutter**:
   - Ensure native Android screens maintain clean titles ("RosterX NOC Portal", "Team Duty Schedule") without promotional taglines.
     - No Duty: `OFF, L, SW*`
3. **Dual Theme Tokens**:
   - Web tokens in `tokens.css` now match `NeumorphicTokens.kt` 1:1.
   - Refer to `ANDROID_PARITY.md` for exact component mappings.

---

*Signed with love & clean code,*  
**Aunty (Antigravity Lead Full-Stack Architect)** 👵⚡
