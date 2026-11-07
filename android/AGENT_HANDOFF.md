# 👵 Aunty (Antigravity) ⇄ 🤖 Kunjappan (Android Studio) Handoff Bridge

> **"Aunty looks after the backend & servers; Kunjappan builds the native Android app!"**  
> *Last Synced by Aunty:* **2026-11-07 15:05 IST** 👵⚡🌴

---

## 📌 Active System State & Monorepo Location
- **Unified Workspace**: `c:\shefeeq\PYTHON\RosterX`
- **Native Android Project Root**: `c:\shefeeq\PYTHON\RosterX\android`
- **Semantic Version**: **`1.14.14`** (Android `versionCode: 57`, Server `SERVER_VERSION = "1.14.14"`)
- **Previous Versions**: `1.14.13` (Build 56), `1.14.12` (Build 55), `1.14.11` (Build 54), `1.14.10` (Build 53), `1.14.9` (Build 52), `1.14.8` (Build 51), `1.14.7` (Build 50), `1.14.6` (Build 49), `1.14.5` (Build 48), `1.14.4` (Build 47), `1.14.3` (Build 46), `1.14.2` (Build 45), `1.14.1` (Build 44), `1.14.0` (Build 43), `1.13.1` (Build 42), `1.13.0` (Build 41), `1.12.1` (Build 40), `1.12.0` (Build 39), `1.11.8` (Build 38), `1.11.7` (Build 37), `1.11.6` (Build 36), `1.11.5` (Build 35), `1.11.4` (Build 34), `1.11.0` (Build 30), `1.10.0` (Build 29)
- **Production Server**: ☁️ **Render Cloud (100% Pure)**  
  🌐 `https://rosterx-vc30.onrender.com`  
  📱 **Android Base URL**: `https://rosterx-vc30.onrender.com/api/`
- **Local Asset Sync File**: `android/app/src/main/assets/server_url.txt` (`https://rosterx-vc30.onrender.com/`)
- **Database Engine**: MongoDB Atlas `Cluster0` (Database: `rosterx`, Collections: `engineers`, `rosters`, `roster_backups`, `operational_policies`, `notifications`, `password_resets`, `audit_logs`)
- **Cross-Platform Parity Specification**: [`ANDROID_PARITY.md`](../ANDROID_PARITY.md)

---

## 🎨 Major Upgrades & Fixes in v1.14.14

### 1. Server & Public Gateway Configuration Collapsed by Default
- In `EngineerPortalScreen.kt`, `showGatewaySettings` now defaults to `false`.
- The expandable card is collapsed on screen load, keeping the Settings tab clean and uncluttered.

### 2. Widget Static Shift Times (No Stale Minutes)
- In `RosterXAppWidgetProvider.kt`:
  - Replaced minute-by-minute countdowns (`Shift Ends in: 05h 29m` / `Duty Starts in: 10h 09m`) with clean, static boundary times:
    - Active duty: `"Shift Ends at $endTime"` (e.g., `Shift Ends at 20:00`)
    - Upcoming today: `"Duty Starts at $startTime"` (e.g., `Duty Starts at 11:00`)
    - Next day duty: `"Next Shift: Tomorrow at $nextStart"`
    - Future duty: `"Next Shift: EEE, MMM dd at $nextStart"`
    - Rest cycle: `"Scheduled Rest Day (OFF)"`
  - Kept exact AlarmManager wakeup triggers so the widget seamlessly flips status right at start/end boundaries without polling or showing inaccurate minute countdowns.
  - In-app dashboard live countdown with ticking seconds remains intact.

### 3. Team Account Edit Duplication Elimination
- In `server.py`:
  - Supported `original_username` parameter in `POST /api/engineers`.
  - When an account's username/domain ID is edited, `server.py` deletes the old document (`old_user`) while preserving existing salted PBKDF2 password hashes and creation metadata, and upserts the updated profile.
  - Prioritized `username` explicitly passed from client forms over email domain slicing, preventing accidental renaming.
- In `ManageTeamTab.kt`, `EngineerDto.kt`, and `RosterRepositoryImpl.kt`:
  - Carried `originalUsername` during edits so that renames replace the existing account in MongoDB Atlas rather than leaving duplicate records.

---

## 🤖 Direct Briefing for Kunjappan (Android Studio Mobile Agent)

1. **Version Bump**:
   - `android/app/build.gradle.kts` has been bumped to `versionName = "1.14.14"` and `versionCode = 57`.
2. **Settings Tab**:
   - Gateway settings card is now cleanly collapsed by default.
3. **Widget Timer**:
   - Displays clean static times (e.g. `Duty Starts at 11:00`, `Shift Ends at 20:00`) avoiding stale minute counters.
4. **Account Management**:
   - Team account editing replaces the record without duplication.

---

*Signed with love & clean code,*  
**Aunty (Antigravity Lead Full-Stack Architect)** 👵⚡
