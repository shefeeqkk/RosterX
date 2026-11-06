# 👵 Aunty (Antigravity) ⇄ 🤖 Kunjappan (Android Studio) Handoff Bridge

> **"Aunty looks after the backend & servers; Kunjappan builds the native Android app!"**  
> *Last Synced by Aunty:* **2026-11-06 22:25 IST** 👵⚡🌴

---

## 📌 Active System State & Monorepo Location
- **Unified Workspace**: `c:\shefeeq\PYTHON\RosterX`
- **Native Android Project Root**: `c:\shefeeq\PYTHON\RosterX\android`
- **Semantic Version**: **`1.14.4`** (Android `versionCode: 47`, Server `SERVER_VERSION = "1.14.4"`)
- **Previous Versions**: `1.14.3` (Build 46), `1.14.2` (Build 45), `1.14.1` (Build 44), `1.14.0` (Build 43), `1.13.1` (Build 42), `1.13.0` (Build 41), `1.12.1` (Build 40), `1.12.0` (Build 39), `1.11.8` (Build 38), `1.11.7` (Build 37), `1.11.6` (Build 36), `1.11.5` (Build 35), `1.11.4` (Build 34), `1.11.0` (Build 30), `1.10.0` (Build 29)
- **Production Server**: ☁️ **Render Cloud (100% Pure)**  
  🌐 `https://rosterx-vc30.onrender.com`  
  📱 **Android Base URL**: `https://rosterx-vc30.onrender.com/api/`
- **Local Asset Sync File**: `android/app/src/main/assets/server_url.txt` (`https://rosterx-vc30.onrender.com/`)
- **Database Engine**: MongoDB Atlas `Cluster0` (Database: `rosterx`, Collections: `engineers`, `rosters`, `roster_backups`, `operational_policies`, `notifications`, `password_resets`, `audit_logs`)
- **Cross-Platform Parity Specification**: [`ANDROID_PARITY.md`](../ANDROID_PARITY.md)

---

## 🎨 Major UI & Architecture Upgrades in v1.14.4

### 1. Pure Dual-Theming (Light & Dark) for Admin Operations Console
- **Eliminated Black Blotches in Light Mode**:
  - Operations Dashboard Header Banner now uses an elegant light gradient (`from-white via-indigo-50/40 to-cyan-50/40 border border-slate-200 text-slate-900`) in Light Mode, and rich dark styling in Dark Mode (`dark:from-slate-900 dark:via-indigo-950 dark:to-slate-900`).
  - Server Logs button now uses clean neumorphic light button styling (`bg-slate-100 hover:bg-slate-200 text-slate-700 border-slate-300`) in Light Mode, avoiding the prior pitch-black rectangle.
  - AI Assistant button has an energetic, vibrant gradient in Light Mode (`from-indigo-600 via-indigo-700 to-cyan-600`) and refined dark mode in Dark Mode.
  - Active Tab Button uses solid, crisp Indigo (`bg-indigo-600 text-white`) in both modes, and inactive buttons adapt to their respective surfaces.
- **Enabled Tailwind Class-Based Dark Mode**:
  - Injected `darkMode: 'class'` into `tailwind-neumorphic.js` so all Tailwind `dark:` variants function dynamically with `<html class="dark">`.
- **Sanitized `tokens.css`**:
  - Removed harmful global overrides with `!important` on `.text-slate-100` that inadvertently made light-mode dark button text black/invisible.

### 2. Operational 4-Category Shift Restructuring
- In alignment with NOC Operational Directives, reorganized the Radar and Team Handover into 4 standardized pillars:
  1. **Morning Shift** (`FS · FG · G · SG` • 07:30 – 20:00)
     - Note: `SG` (Second General) is now correctly placed under Morning Shift (not grouped under Second Shift).
  2. **Afternoon Shift** (`SS` • 13:00 – 22:00)
     - Dedicated exclusively to `SS` (Second Shift).
  3. **Night Shift** (`NS (Solo Duty)` • 21:00 – 07:30 +1)
     - Light mode uses a clean, readable light indigo card (`bg-indigo-50/70 border-indigo-200 text-indigo-950`), completely removing the pitch-black card.
  4. **No Duty** (`OFF · L · SW*` • Rest & Leave)
     - Encompasses Weekly Offs (`OFF`), Approved Leave (`L`), and Swaps (`SW*`).
- **Synchronized Across**:
  - Admin Portal Coverage Radar (`admin-web/index.html` & `admin-web/js/app.js`)
  - User Portal Team Handover (`admin-web/user-portal.html` & `admin-web/js/userPortal.js`)
  - Server AI Grounding Directive (`server.py`)

---

## 🤖 Direct Briefing for Kunjappan (Android Studio Mobile Agent)

1. **Version Update**:
   - `android/app/build.gradle.kts` has been bumped to `versionName = "1.14.4"` and `versionCode = 47`.
2. **Shift Categorization Parity in Android UI**:
   - Please ensure any shift filter or duty grouping in Jetpack Compose follows the 4 pillars:
     - Morning Shift: `FS, FG, G, SG`
     - Afternoon Shift: `SS`
     - Night Shift: `NS`
     - No Duty: `OFF, L, SW*`
3. **Dual Theme Tokens**:
   - Web tokens in `tokens.css` now match `NeumorphicTokens.kt` 1:1.
   - Refer to `ANDROID_PARITY.md` for exact component mappings.

---

*Signed with love & clean code,*  
**Aunty (Antigravity Lead Full-Stack Architect)** 👵⚡
