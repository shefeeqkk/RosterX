# 👵 Aunty (Antigravity) ⇄ 🤖 Kunjappan (Android Studio) Handoff Bridge

> **"Aunty looks after the backend & servers; Kunjappan builds the native Android app!"**  
> *Last Synced by Aunty:* **2026-11-05 23:25 IST** 👵⚡🌴

---

## 📌 Active System State & Monorepo Location
- **Unified Workspace**: `c:\shefeeq\PYTHON\RosterX`
- **Native Android Project Root**: `c:\shefeeq\PYTHON\RosterX\android`
- **Semantic Version**: **`1.12.0`** (Android `versionCode: 39`, Server `SERVER_VERSION = "1.12.0"`)
- **Previous Versions**: `1.11.8` (Build 38), `1.11.7` (Build 37), `1.11.6` (Build 36), `1.11.5` (Build 35), `1.11.4` (Build 34), `1.11.0` (Build 30), `1.10.0` (Build 29)
- **Production Server**: ☁️ **Render Cloud (100% Pure)**  
  🌐 `https://rosterx-vc30.onrender.com`  
  📱 **Android Base URL**: `https://rosterx-vc30.onrender.com/api/`
- **Zero-G WebGL Showcase**: `https://rosterx-vc30.onrender.com/zero-g/`
- **Local Asset Sync File**: `android/app/src/main/assets/server_url.txt` (`https://rosterx-vc30.onrender.com/`)
- **Database Engine**: MongoDB Atlas `Cluster0` (Database: `rosterx`, Collections: `engineers`, `rosters`, `roster_backups`, `operational_policies`, `notifications`, `password_resets`, `audit_logs`)
- **Security & RBAC**: HMAC-SHA256 Signed JWT Tokens, Salted PBKDF2 Password Hashes, Anti-Hijack OTP Dispatch
- **Live Tabbed Architecture**:
  - **Admin Operations Console** (`admin-web/index.html`): 5 Tabs (Operations Dashboard with Kinetic Tilt & NOC Coverage Radar, Roster Matrix with 31-day sticky grid, Swaps & Leaves, AI Scheduling Studio, System Diagnostics & Logs)
  - **Engineer User Portal** (`admin-web/user-portal.html`): 4 Tabs (My Schedule with Live Handover Countdown, Desk Handover & Team View, Request Shift Swap, Monthly Overview)
  - **Responsive Dual Navigation Adapters**: Top horizontal Neumorphic dock on Desktop (>1024px); Floating bottom Neumorphic dock with `#FF5252` active indicator on Mobile (<768px).
- **Dynamic Backend Telemetry**:
  - `GET /api/telemetry/server`: Host execution uptime, Atlas round-trip ping latency (ms), collection counts, RSS memory footprint (MB) across Linux and Windows.
  - `GET /api/telemetry/users`: Active engineers tally, platform breakdown (`Web Portal` vs `Android App`), active windows (1h, 24h, 7d), and latest real audit logs from `audit_logs` collection.
- **Web Portal Unfreeze**: Fixed readyState race condition (`document.readyState === 'loading'`), mixed content / stale endpoint healing in `atlasService.js`, and implemented immediate optimistic initial rendering (<5ms).

---

## 🤖 Direct Briefing for Kunjappan (Android Studio Mobile Agent)

1. **Version Update**:
   - `android/app/build.gradle.kts` has been bumped to `versionName = "1.12.0"` and `versionCode = 39`.
2. **Backend Telemetry Endpoints Available for Android**:
   - `GET /api/telemetry/server`: Native Android can display host diagnostics, live Atlas ping latency, and server health.
   - `GET /api/telemetry/users`: User platform tallies and active session breakdown.
3. **Audit Log Recording**:
   - Android sign-ins or key actions can trigger audit events, automatically logged to Atlas.
4. **Shift Countdown & Handover Parity**:
   - The web user portal now features live shift countdown and shift handover indicators matching the mobile app's 24/7 duty rotation guidelines.

---

*Signed with love & clean code,*  
**Aunty (Antigravity Systems Architect)** 👵⚡
