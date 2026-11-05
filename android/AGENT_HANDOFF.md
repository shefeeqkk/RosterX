# 👵 Aunty (Antigravity) ⇄ 🤖 Kunjappan (Android Studio) Handoff Bridge

> **"Aunty looks after the backend & servers; Kunjappan builds the native Android app!"**  
> *Last Synced by Aunty:* **2026-11-05 23:55 IST** 👵⚡🌴

---

## 📌 Active System State & Monorepo Location
- **Unified Workspace**: `c:\shefeeq\PYTHON\RosterX`
- **Native Android Project Root**: `c:\shefeeq\PYTHON\RosterX\android`
- **Semantic Version**: **`1.12.1`** (Android `versionCode: 40`, Server `SERVER_VERSION = "1.12.1"`)
- **Previous Versions**: `1.12.0` (Build 39), `1.11.8` (Build 38), `1.11.7` (Build 37), `1.11.6` (Build 36), `1.11.5` (Build 35), `1.11.4` (Build 34), `1.11.0` (Build 30), `1.10.0` (Build 29)
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
- **Dynamic Current Month Loading**:
  - Both Admin Operations Console and Shift Matrix now dynamically initialize to the **real current month** (`2026-11` November 2026) instead of hardcoding October.
  - "Today's NOC 24/7 Coverage Radar" persistently maps today's date against the genuine current month roster.
- **Enhanced Countdown Timer**:
  - When an engineer's shift is over or on off-days, timer dynamically computes the exact remaining countdown to their upcoming scheduled duty: `"Next Shift Starts in: {Xh Ym Zs}"`.
- **Mobile AI FAB UX Polish**:
  - Floating 3D Bot Cutout elevated to `bottom-20` on mobile viewports so it never blocks the bottom navigation tabs.
  - Removed circular background disk for a pure, floating 3D bot cutout with cyan drop-shadow.

---

## 🤖 Direct Briefing for Kunjappan (Android Studio Mobile Agent)

1. **Version Update**:
   - `android/app/build.gradle.kts` has been bumped to `versionName = "1.12.1"` and `versionCode = 40`.
2. **Current Month Roster Alignment**:
   - Mobile app should ensure it requests `/api/roster/<current-year>-<current-month>` as default on app startup (e.g. `2026-11`).
3. **Shift Countdown Timer Alignment**:
   - Mobile app duty countdown logic aligns with `"Next Shift Starts in: {Xh Ym Zs}"` when current duty ends.

---

*Signed with love & clean code,*  
**Aunty (Antigravity Systems Architect)** 👵⚡
