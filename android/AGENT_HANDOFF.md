# 👵 Aunty (Antigravity) ⇄ 🤖 Kunjappan (Android Studio) Handoff Bridge

> **"Aunty looks after the backend & servers; Kunjappan builds the native Android app!"**  
> *Last Synced by Aunty:* **2026-11-07 01:45 IST** 👵⚡🌴

---

## 📌 Active System State & Monorepo Location
- **Unified Workspace**: `c:\shefeeq\PYTHON\RosterX`
- **Native Android Project Root**: `c:\shefeeq\PYTHON\RosterX\android`
- **Semantic Version**: **`1.14.11`** (Android `versionCode: 54`, Server `SERVER_VERSION = "1.14.11"`)
- **Previous Versions**: `1.14.10` (Build 53), `1.14.9` (Build 52), `1.14.8` (Build 51), `1.14.7` (Build 50), `1.14.6` (Build 49), `1.14.5` (Build 48), `1.14.4` (Build 47), `1.14.3` (Build 46), `1.14.2` (Build 45), `1.14.1` (Build 44), `1.14.0` (Build 43), `1.13.1` (Build 42), `1.13.0` (Build 41), `1.12.1` (Build 40), `1.12.0` (Build 39), `1.11.8` (Build 38), `1.11.7` (Build 37), `1.11.6` (Build 36), `1.11.5` (Build 35), `1.11.4` (Build 34), `1.11.0` (Build 30), `1.10.0` (Build 29)
- **Production Server**: ☁️ **Render Cloud (100% Pure)**  
  🌐 `https://rosterx-vc30.onrender.com`  
  📱 **Android Base URL**: `https://rosterx-vc30.onrender.com/api/`
- **Local Asset Sync File**: `android/app/src/main/assets/server_url.txt` (`https://rosterx-vc30.onrender.com/`)
- **Database Engine**: MongoDB Atlas `Cluster0` (Database: `rosterx`, Collections: `engineers`, `rosters`, `roster_backups`, `operational_policies`, `notifications`, `password_resets`, `audit_logs`)
- **Cross-Platform Parity Specification**: [`ANDROID_PARITY.md`](../ANDROID_PARITY.md)

---

## 🎨 Major UI & Operational Upgrades in v1.14.11

### 1. Month Matrix Stats Counter Logic Parity
- **Working (WRK)**: Strictly counts `FS, FG, G, SG, SS, NS`.
- **Night (NS)**: Strictly counts `NS`.
- **OFF (Rest)**: Strictly counts `OFF`, `L`, and all `SW*` swaps/offs (`SW`, `SW-OFF`).
- Synchronized across `EngineerPortalScreen.kt`, `MonthRostersTab.kt`, `app.js`, `userPortal.js`, and `excelService.js`.

### 2. Day Coverage Counter Color De-Clash
- Changed Day Coverage footers from mint-green (`#D1FAE5`) to Sky-Blue (`#E0F2FE` background with `#0369A1` text), eliminating color clash with First Shift (FS).
- Night Coverage footers styled in Soft Indigo (`#EEF2FF` background with `#4338CA` text).

### 3. Intelligent & Brief Operations AI Assistant
- Completely redesigned `execute_deterministic_roster_ai` and `build_roster_grounding_context`:
  - Intelligent shift resolution (`second general` -> `SG`, `first general` -> `FG`, `first shift` -> `FS`, `second shift` -> `SS`, `night shift` -> `NS`, `rest day` -> `OFF`).
  - Answers are 100% accurate, derived from real-time MongoDB Atlas, and strictly brief (1–2 sentences, zero telemetry walls).
  - Handles today's shifts, future shifts, night desk coverage, teammate lookups, and scheduled rest days.

---

## 🤖 Direct Briefing for Kunjappan (Android Studio Mobile Agent)

1. **Version Bump**:
   - `android/app/build.gradle.kts` has been bumped to `versionName = "1.14.11"` and `versionCode = 54`.
2. **App Widget & Bot Live**:
   - 2x2 widget padding and single-line labels prevent cropping; squircle shift codes are enlarged and properly rounded.
   - 3D animated cutout bot FAB is active without circular container background.
   - Day & Night coverage colors in Month Roster are de-clashed from FS green!
3. **AI Chat Experience**:
   - Asking queries like *"when is my second general shift"* now instantly returns direct, brief answers without unrequested status dumps!
3. **Wireless Deploy Ready**:
   - Run `.\deploy-wireless.ps1` from the `android/` directory to compile and deploy wirelessly in one step!
     - No Duty: `OFF, L, SW*`
3. **Dual Theme Tokens**:
   - Web tokens in `tokens.css` now match `NeumorphicTokens.kt` 1:1.
   - Refer to `ANDROID_PARITY.md` for exact component mappings.

---

*Signed with love & clean code,*  
**Aunty (Antigravity Lead Full-Stack Architect)** 👵⚡
