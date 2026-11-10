# 🕵️‍♂️ Dasan (Antigravity Agent 1) ⇄ 🕵️ Vijayan (Antigravity Agent 2) Handoff Bridge

> **"Dasan looks after core orchestration, backend & builds; Vijayan crafts Jetpack Compose UI & client features!"**  
> *Last Synced by Dasan:* **2026-11-10 13:00 IST** 🕵️‍♂️⚡🌴

---

## 📌 Active System State & Monorepo Location
- **Unified Workspace**: `c:\shefeeq\PYTHON\RosterX`
- **Native Android Project Root**: `c:\shefeeq\PYTHON\RosterX\android`
- **Semantic Version**: **`1.23.4`** (Android `versionCode: 83`, Server `SERVER_VERSION = "1.23.4"`)
- **Previous Versions**: `1.23.3` (Build 82), `1.23.2` (Build 81), `1.23.1` (Build 80), `1.23.0` (Build 79)
- **Production Server**: ☁️ **Render Cloud + Branded Cloudflare Domain**  
  🌐 `https://rosterx.online` & `https://api.rosterx.online`  
  📱 **Android Base URL**: `https://api.rosterx.online/api/`
- **Local Asset Sync File**: `android/app/src/main/assets/server_url.txt` (`https://api.rosterx.online/`)
- **Email Infrastructure**: Verified Custom Domain `rosterx.online` on Resend. Primary dispatch: `RosterX Operations <notify@rosterx.online>` via HTTPS REST (Port 443).
- **Database Engine**: MongoDB Atlas `Cluster0` (Database: `rosterx`, Collections: `engineers`, `rosters`, `roster_backups`, `operational_policies`, `notifications`, `broadcast_templates`, `password_resets`, `audit_logs`)
- **Tailscale Static IP for Phone**: `100.71.241.36`

---

## 🚀 Major Upgrades in v1.23.4 (Build 83)

### 1. 🛡️ Seamless Removal of Week Offs for Admin & Manager Roles
- **Android App (`EngineerPortalScreen.kt`)**:
  - Guarded `WeeklyOffsCard` with role check:
    `if (engineer?.role != EngineerRole.ADMIN && engineer?.role != EngineerRole.MANAGER)`.
  - For Leadership (Admin & Manager), the card is completely omitted. The profile layout flows seamlessly from Tenure & Birthday into Account Details without any gaps.
- **Android Team Management (`ManageTeamTab.kt`)**:
  - Enforced `defaultWeeklyOffs = emptyList()` when creating or updating Admin/Manager accounts.
- **Web Portal (`admin-web/`)**:
  - User Portal (`userPortal.js` & `user-portal.html`): Hides the `Scheduled Weekly Offs` section on profile for Admin and Manager accounts.
  - Team Management (`app.js` & `index.html`): Hides the weekly off day selectors when role is switched to Admin or Manager, saving empty array `[]` and rendering `Non-Shift Operations` in directory tags.
- **Backend & MongoDB Atlas (`server.py`)**:
  - Auto-sanitizes database on startup: `update_many({"role": {"$in": ["admin", "manager", "ADMIN", "MANAGER"]}}, {"$set": {"default_weekly_offs": []}})`.
  - Enforces `default_weekly_offs = []` in `/api/register` and `/api/engineers` endpoints.
