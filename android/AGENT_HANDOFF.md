# 🕵️‍♂️ Dasan (Antigravity Agent 1) ⇄ 🕵️ Vijayan (Antigravity Agent 2) Handoff Bridge

> **"Dasan looks after core orchestration, backend & builds; Vijayan crafts Jetpack Compose UI & client features!"**  
> *Last Synced by Vijayan:* **2026-11-10 22:36 IST** 🕵️🎨✨

---

## 📌 Active System State & Monorepo Location
- **Unified Workspace**: `c:\shefeeq\PYTHON\RosterX`
- **Native Android Project Root**: `c:\shefeeq\PYTHON\RosterX\android`
- **Semantic Version**: **`1.23.11`** (Android `versionCode: 90`, Server `SERVER_VERSION = "1.23.11"`)
- **Previous Versions**: `1.23.10` (Build 89), `1.23.9` (Build 88), `1.23.8` (Build 87), `1.23.7` (Build 86), `1.23.6` (Build 85)
- **Production Server**: ☁️ **Render Cloud + Branded Cloudflare Domain**  
  🌐 `https://rosterx.online` & `https://api.rosterx.online`  
  📱 **Android Base URL**: `https://api.rosterx.online/api/`
- **Database Engine**: MongoDB Atlas `Cluster0` (Database: `rosterx`)
- **Target Device (Tailscale IP)**: `100.71.241.36:5555` (Connected & Verified)

---

## 🚀 Solved in v1.23.11 (Build 90) - Vijayan UI Overhaul

### 1. 🔔 Neumorphic Quick Notifications Bottom Sheet Redesign
- **Rich Neumorphic Cards**:
  - Transformed `QuickNotificationsBottomSheet` from flat cards into tactile Neumorphic raised cards (`NeumorphicElevationStyle.Raised`, `RoundedCornerShape(20.dp)`).
  - Priority pills (`URGENT`, `SHIFT_UPDATE`, `CELEBRATION 🎉`, `BROADCAST`, `INFO`) with tint-coded alpha badges.
  - Neumorphic inset dismiss button (`CircleShape`, 32.dp) for individual card deletion.
  - Clean "Clear All" action in header and celebratory empty-state when all notifications are cleared.
- **Dynamic Mention & Token Interpolation**:
  - Dynamically replaces `{name}`, `{Their name}`, and `@{username}` tokens in notification bodies using the active logged-in engineer's profile (`state.loggedInEngineer`).
- **Interactive Celebration Cards**:
  - Tapping celebration cards automatically closes the sheet and launches the celebration wishes dialog (`onOpenCelebrationWishes`).
- **Smooth 320ms Exit Animations**:
  - Animated item deletion using `slideOutHorizontally` (fast-out-slow-in, 320ms) + `fadeOut` (220ms) + `shrinkVertically` (320ms).
  - Batch animated dismiss on "Clear All".

### 2. 🧹 Decluttered Dashboards
- **Removed In-App Notification Clutter**:
  - Extracted operational broadcast cards and clear-all actions from both `ManagerDashboardView` and `DashboardView` (Engineer).
  - Dashboards now strictly focus on key operational shifts, metrics, and duty matrix without redundant announcement blocks.
- **Top App Bar Unread Count Pill Badge**:
  - Replaced tiny static red dot with a bold unread badge pill (`minWidth = 16.dp`, bold white text) indicating exact pending notification count (e.g., `'1'`, `'3'`, `'99+'`).

---

## 🛠️ Build & Verification
- Compile strictly via `safe_build.bat assembleDebug`.
- Stream-install onto physical device `100.71.241.36:5555`.
