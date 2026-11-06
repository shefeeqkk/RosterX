# 👵 Aunty (Antigravity) ⇄ 🤖 Kunjappan (Android Studio) Handoff Bridge

> **"Aunty looks after the backend & servers; Kunjappan builds the native Android app!"**  
> *Last Synced by Aunty:* **2026-11-06 12:20 IST** 👵⚡🌴

---

## 📌 Active System State & Monorepo Location
- **Unified Workspace**: `c:\shefeeq\PYTHON\RosterX`
- **Native Android Project Root**: `c:\shefeeq\PYTHON\RosterX\android`
- **Semantic Version**: **`1.13.0`** (Android `versionCode: 41`, Server `SERVER_VERSION = "1.13.0"`)
- **Previous Versions**: `1.12.1` (Build 40), `1.12.0` (Build 39), `1.11.8` (Build 38), `1.11.7` (Build 37), `1.11.6` (Build 36), `1.11.5` (Build 35), `1.11.4` (Build 34), `1.11.0` (Build 30), `1.10.0` (Build 29)
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
  - Both Admin Operations Console and Shift Matrix dynamically initialize to the **real current month** (`2026-11` November 2026) instead of hardcoding October.
  - "Today's NOC 24/7 Coverage Radar" asynchronously verifies and fetches the current month roster from Atlas to guarantee 100% genuine daily duty rosters.
- **Enhanced Countdown Timer**:
  - When an engineer's shift has ended or on off-days, timer dynamically forward-scans the roster and computes the exact remaining countdown to their next scheduled duty: `"Next Shift Starts in: {Xh Ym Zs}"`.
- **Mobile AI FAB UX Polish**:
  - Floating 3D Bot Cutout elevated to `bottom-28` on mobile viewports so it never blocks the bottom navigation tabs.
  - Removed circular background disc for a pure, floating 3D bot cutout with cyan drop-shadow.
- **Dynamic Grounded Gemini AI Operations Agent (`POST /api/chat/assistant`)**:
  - Server-side RAG pipeline grounded in Indian Standard Time (IST, UTC+5:30), live duty shifts (`FS`, `FG`, `G`, `SG`, `SS`, `NS`), engineer directories, swap records, and host uptime.
  - Automatic model fallback sequence with a 100% reliable deterministic fallback that answers operational queries directly from MongoDB Atlas when API quotas are exceeded.
- **Native Android AI Integration**:
  - `RosterXAssistantBottomSheet.kt` added to Jetpack Compose with Material 3 Neumorphic styling, suggestion chips, thinking dots, and auto-scrolling chat history.
  - Wired into both `EngineerPortalScreen.kt` and `AdminMainScreen.kt` via viewmodels.

---

## 🤖 Direct Briefing for Kunjappan (Android Studio Mobile Agent)

1. **Version Update**:
   - `android/app/build.gradle.kts` has been bumped to `versionName = "1.13.0"` and `versionCode = 41`.
2. **AI Operations Assistant Endpoint**:
   - Kunjappan! Backend endpoint `POST /api/chat/assistant` is ready with payload `{"message": "...", "current_user": {...}}`.
   - `RosterApiService`, `RosterRepository`, and `RosterRepositoryImpl` have been updated with `queryAssistant`.
3. **Compose Assistant Bottom Sheet**:
   - `RosterXAssistantBottomSheet` is now integrated into `EngineerPortalScreen` and `AdminMainScreen`.
4. **Current Month Roster Alignment**:
   - Mobile app requests `/api/roster/<current-year>-<current-month>` as default on app startup (`2026-11`).
5. **Shift Countdown Timer Alignment**:
   - Mobile app duty countdown logic aligns with `"Next Shift Starts in: {Xh Ym Zs}"` when current duty ends.

---

*Signed with love & clean code,*  
**Aunty (Antigravity Systems Architect)** 👵⚡