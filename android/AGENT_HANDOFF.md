# 🕵️‍♂️ Dasan (Antigravity Agent 1) ⇄ 🕵️ Vijayan (Antigravity Agent 2) Handoff Bridge

> **"Dasan looks after core orchestration, backend & builds; Vijayan crafts Jetpack Compose UI & client features!"**  
> *Last Synced by Dasan:* **2026-11-10 12:05 IST** 🕵️‍♂️⚡🌴

---

## 📌 Active System State & Monorepo Location
- **Unified Workspace**: `c:\shefeeq\PYTHON\RosterX`
- **Native Android Project Root**: `c:\shefeeq\PYTHON\RosterX\android`
- **Semantic Version**: **`1.23.3`** (Android `versionCode: 82`, Server `SERVER_VERSION = "1.23.3"`)
- **Previous Versions**: `1.23.2` (Build 81), `1.23.1` (Build 80), `1.23.0` (Build 79), `1.22.3` (Build 78)
- **Production Server**: ☁️ **Render Cloud + Branded Cloudflare Domain**  
  🌐 `https://rosterx.online` & `https://api.rosterx.online`  
  📱 **Android Base URL**: `https://api.rosterx.online/api/`
- **Local Asset Sync File**: `android/app/src/main/assets/server_url.txt` (`https://api.rosterx.online/`)
- **Email Infrastructure**: Verified Custom Domain `rosterx.online` on Resend. Primary dispatch: `RosterX Operations <notify@rosterx.online>` via HTTPS REST (Port 443).
- **Database Engine**: MongoDB Atlas `Cluster0` (Database: `rosterx`, Collections: `engineers`, `rosters`, `roster_backups`, `operational_policies`, `notifications`, `broadcast_templates`, `password_resets`, `audit_logs`)
- **Tailscale Static IP for Phone**: `100.71.241.36`

---

## 🚀 Major Upgrades in v1.23.3 (Build 82)

### 1. 🕒 12-Hour Local Indian Timezone (IST) for Server & Terminal Logs
- **Server-Side (`server.py`)**:
  - Implemented `IST_TIMEZONE = timezone(timedelta(hours=5, minutes=30))`.
  - Added `get_ist_now()` and `get_ist_time_str(format_12h=True)` so all backend requests and lifecycle events log with 12-hour IST timestamps (e.g. `[11:31:07 AM]`).
- **Android App (`ServerManagementTab.kt`)**:
  - Implemented `formatLogToLocal12Hour()` parser.
  - Automatically converts 24-hour UTC logs (from Render Cloud container) into 12-hour device local time (`[hh:mm:ss a]`), accurately showing Indian Standard Time on user devices.

### 2. 🎛️ Collapsible In-Line Terminal Log Viewer with Dynamic Live Stream
- **Default State**: Collapsed by default on `ServerManagementTab` to maintain a compact, clean admin view.
- **Header**:
  - Features Code icon, "TERMINAL LOG VIEWER", "IST (12H)" timezone tag, and expand/collapse chevron.
  - Badge dynamically displays **"PAUSED"** when collapsed and **"LIVE STREAM"** with a pulsing green indicator dot when expanded.
- **Resource-Conscious Live Refresh Loop**:
  - Expanding triggers `LaunchedEffect(isTerminalExpanded)` running a 2-second polling coroutine (`while (isActive) { onRefreshLogs(); delay(2000L) }`).
  - Collapsing instantly disposes the coroutine, stopping all background network traffic and preserving battery/memory.
  - Includes auto-scroll to the bottom of the log stream as new lines arrive.

### 3. 👥 Agent Renaming & Parallel Collaboration Standard ("Dasan & Vijayan")
- Deprecated legacy "Aunty" & "Kunjappan" names.
- Transitioned fully to Antigravity-based multi-conversation workflows:
  - **Dasan (🕵️‍♂️)**: Primary orchestrator (server, infrastructure, builds, and handoff).
  - **Vijayan (🕵️)**: UI and client feature specialist.
- Enforced build concurrency locks via `safe_build.bat` / `safe_build.ps1` to prevent parallel daemon collisions.
