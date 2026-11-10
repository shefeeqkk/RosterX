# 🕵️‍♂️ Dasan (Antigravity Agent 1) ⇄ 🕵️ Vijayan (Antigravity Agent 2) Handoff Bridge

> **"Dasan looks after core orchestration, backend & builds; Vijayan crafts Jetpack Compose UI & client features!"**  
> *Last Synced by Dasan:* **2026-11-11 01:21 IST** 🕵️‍♂️⚡🚀

---

## 📌 Active System State & Monorepo Location
- **Unified Workspace**: `c:\shefeeq\PYTHON\RosterX`
- **Native Android Project Root**: `c:\shefeeq\PYTHON\RosterX\android`
- **Semantic Version**: **`1.24.2`** (Android `versionCode: 93`, Server `SERVER_VERSION = "1.24.2"`)
- **Previous Versions**: `1.24.1` (Build 92), `1.24.0` (Build 91), `1.23.11` (Build 90), `1.23.10` (Build 89), `1.23.9` (Build 88)
- **Production Server**: ☁️ **Render Cloud + Branded Cloudflare Domain**  
  🌐 `https://rosterx.online` & `https://api.rosterx.online`  
  📱 **Android Base URL**: `https://api.rosterx.online/api/`
- **Database Engine**: MongoDB Atlas `Cluster0` (Database: `rosterx`)
- **Target Device (Tailscale IP)**: `100.71.241.36:5555` (Connected & Verified)

---

## 🚀 Solved in v1.24.2 (Build 93) - Notification Foundation & Account Isolation

### 1. 🛡️ Exclusive Device Token Binding (Server-Side)
- **Automatic Purge on Login**:
  - `POST /api/users/fcm-token` now executes an atomic `$pull` of the registering device token from all other engineer records before assigning it to the newly authenticated user.
  - Guarantees that a physical device token is **exclusively associated with only one active account at any moment**.

### 2. 🚪 Sign-Out FCM Unregistration & Tray Purge
- **Clean Disassociation on Logout**:
  - Added `POST /api/users/fcm-token/unregister` to server backend.
  - On tapping "Sign Out", `EngineerPortalViewModel` and `AdminRosterViewModel` notify the server to unregister the device token.
  - `UserSessionManager.clearSession()` executes `notificationManager.cancelAll()`, clearing all pending and historical RosterX notifications from the Android notification shade immediately.

### 3. 🎯 Device-Level Dispatch Deduplication
- **Broadcast Deduplication**:
  - `server.py` broadcast dispatch loop maintains a `dispatched_device_tokens` tracking set across recipients.
  - Ensures each unique physical device receives at most **one** push notification per broadcast ID.
  - Injects `target_user` into the FCM `data` payload for client verification.

### 4. 🔏 Client-Side Identity Gate
- **`RosterXFirebaseMessagingService` Filter**:
  - Checks `sessionManager.getCurrentUser()` on every incoming push.
  - If the device is signed out, the notification is silently dropped.
  - If `remoteMessage.data["target_user"]` does not match the active logged-in username (and is not `"everyone"`), the notification is silently dropped.
  - **Eliminates cross-account notification leakage entirely.**

---

## 🛠️ Build & Verification
- Compiled and built via `safe_build.bat assembleDebug --no-daemon`.
- Streamed & installed on physical device `100.71.241.36:5555`.
