# 🕵️‍♂️ Dasan (Antigravity Agent 1) ⇄ 🕵️ Vijayan (Antigravity Agent 2) Handoff Bridge

> **"Dasan looks after core orchestration, backend & builds; Vijayan crafts Jetpack Compose UI & client features!"**  
> *Last Synced by Dasan:* **2026-11-11 00:20 IST** 🕵️‍♂️⚡🚀

---

## 📌 Active System State & Monorepo Location
- **Unified Workspace**: `c:\shefeeq\PYTHON\RosterX`
- **Native Android Project Root**: `c:\shefeeq\PYTHON\RosterX\android`
- **Semantic Version**: **`1.24.0`** (Android `versionCode: 91`, Server `SERVER_VERSION = "1.24.0"`)
- **Previous Versions**: `1.23.11` (Build 90), `1.23.10` (Build 89), `1.23.9` (Build 88), `1.23.8` (Build 87), `1.23.7` (Build 86), `1.23.6` (Build 85)
- **Production Server**: ☁️ **Render Cloud + Branded Cloudflare Domain**  
  🌐 `https://rosterx.online` & `https://api.rosterx.online`  
  📱 **Android Base URL**: `https://api.rosterx.online/api/`
- **Database Engine**: MongoDB Atlas `Cluster0` (Database: `rosterx`)
- **Target Device (Tailscale IP)**: `100.71.241.36:5555` (Connected & Verified)

---

## 🚀 Solved in v1.24.0 (Build 91) - Dasan & Vijayan Synergy

### 1. 🔔 Top App Bar Animated Ringing Bell
- **Continuous Wiggle/Ring Animation**:
  - Implemented `rememberInfiniteTransition` with `animateFloat` (-15° to +15°, `tween(180ms)`, `RepeatMode.Reverse`).
  - Applied via `Modifier.graphicsLayer { rotationZ = bellRotation }` on notification bell icon whenever `unreadCount > 0`.
  - Gracefully stops in upright position when all notifications are read or dismissed.

### 2. 🔴 Adaptive Rounded Notification Badge
- **Adaptive Bubble Geometry**:
  - Automatically switches between a perfect circle (`size(16.dp)`) for single-digit counts (< 10) and a pill/capsule shape (`defaultMinSize(minWidth = 20.dp, minHeight = 16.dp)` with horizontal padding) for 2+ digits (`"10"`, `"99+"`).
  - Strict circular border and centered 8sp bold white text ensuring numbers never overflow.

### 3. 🎂 Automated Daily Birthday & Anniversary Engine
- **Autonomous Daily Cron Thread (`server.py`)**:
  - Spawns a dedicated daemon thread `_daily_celebration_cron` running daily at **08:30 IST** + initial startup health check.
  - Automatically matches today's date against engineer `birthday` and `join_date`.
  - Dispatches company-wide FCM push notifications and generates CELEBRATION cards in in-app notification sheet for all active teammates with anti-duplicate idempotency (`celeb_YYYY-MM-DD_user_type`).

### 4. 🏷️ About Card Corrections (Settings Screen)
- **Application Name**: Fixed display to strictly say **"RosterX"** (previously `"Project RosterX"`).
- **Backend Server Version**: Removed obsolete static `"v1.7.3"`. Now dynamically bound to `state.serverVersion` resolved via `getServerTelemetry()` during diagnostics refresh, with seamless fallback to `v${BuildConfig.VERSION_NAME}`.

---

## 🛠️ Build & Verification
- Compiled and built via `safe_build.bat assembleDebug --no-daemon`.
- Streamed & installed on physical device `100.71.241.36:5555`.
