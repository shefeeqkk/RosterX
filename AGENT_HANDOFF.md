# 🕵️‍♂️ Dasan (Antigravity Agent 1) ⇄ 🕵️ Vijayan (Antigravity Agent 2) Handoff Bridge

> **"Dasan looks after core orchestration, backend & builds; Vijayan crafts Jetpack Compose UI & client features!"**  
> *Last Synced by Dasan:* **2026-11-10 16:48 IST** 🕵️‍♂️⚡🌴

---

## 📌 Active System State & Monorepo Location
- **Unified Workspace**: `c:\shefeeq\PYTHON\RosterX`
- **Native Android Project Root**: `c:\shefeeq\PYTHON\RosterX\android`
- **Semantic Version**: **`1.23.6`** (Android `versionCode: 85`, Server `SERVER_VERSION = "1.23.6"`)
- **Previous Versions**: `1.23.5` (Build 84), `1.23.4` (Build 83), `1.23.3` (Build 82), `1.23.2` (Build 81)
- **Production Server**: ☁️ **Render Cloud + Branded Cloudflare Domain**  
  🌐 `https://rosterx.online` & `https://api.rosterx.online`  
  📱 **Android Base URL**: `https://api.rosterx.online/api/`
- **Local Asset Sync File**: `android/app/src/main/assets/server_url.txt` (`https://api.rosterx.online/`)
- **Database Engine**: MongoDB Atlas `Cluster0` (Database: `rosterx`, Storage: ~954 KB, Collections: 9 active, 215 docs)
- **Target Device (Tailscale IP)**: `100.71.241.36:5555`

---

## 🔍 Investigation on User's Screenshots (Image 1 & Image 2)

### 1. Why "WEEKLY OFFS" was visible on the Phone (Image 2):
- **Root Cause**: The user's physical phone was still running the older APK built from commit `e2c6baa` (v1.23.3), which predated the removal of week offs for Admin/Manager.
- **Resolution**:
  - In **v1.23.4** (commit `a05288c`), the weekly offs card in `EngineerPortalScreen.kt` was strictly gated:
    ```kotlin
    if (engineer?.role != EngineerRole.ADMIN && engineer?.role != EngineerRole.MANAGER) {
        item { WeeklyOffsCard(weeklyOffs = engineer?.defaultWeeklyOffs ?: emptyList()) }
    }
    ```
  - In addition, Admin and Manager roles were scrubbed from shift matrices in `ManageTeamTab.kt`, `server.py`, and the web portal.
  - The freshly compiled APK (**v1.23.6 Build 85**) completely excludes this card on Admin & Manager profiles as verified in the code!

### 2. Vijayan's Workspace Status (Image 1):
- Vijayan opened at commit `e2c6baa`.
- **Vijayan Action Required**: Run `git pull origin main` in your workspace to pull in:
  - `a05288c` (v1.23.4: Week offs omitted for Admin & Manager)
  - `ee15c3c` (v1.23.5: Redesigned Admin Dashboard & Firebase Analytics modal)
  - Latest v1.23.6 (Google Password Manager & Digital Asset Links)

---

## 🚀 Major Upgrades in v1.23.6 (Build 85)

### 1. 🔑 Native Google Password Manager & Credential Manager
- **Library Integration**: Added `androidx.credentials:credentials:1.3.0` and `androidx.credentials:credentials-play-services-auth:1.3.0`.
- **`GooglePasswordManagerHelper.kt`**:
  - `saveCredentials(activity, username, password)`: Launches standard Google Password Manager prompt via `CreatePasswordRequest` upon successful login or OTP reset.
  - `getSavedCredentials(activity)`: Invokes `GetCredentialRequest` with `GetPasswordOption` to retrieve credentials with a 1-tap bottom sheet.
  - Silent respect: Cancellation or dismissal exceptions are silently handled with no nagging.
- **LoginScreen UI Upgrades**:
  - Auto-launches 1-Tap Google Password Manager credential sheet on screen launch if enabled.
  - Native Jetpack Compose `AutofillNode` with `LocalAutofillTree` & `LocalAutofill` attached to Username/Password text fields.
  - Dedicated "Autofill with Google Password Manager" chip button with Google logo icon for quick manual 1-tap trigger.
- **Settings Toggle (`ServerManagementTab.kt`)**:
  - Added "CREDENTIALS & PASSWORDS" card with a toggle switch to enable/disable Google Password Manager autofill prompts according to user preference.

### 2. 🌐 Cross-Platform Sync via Digital Asset Links
- **Android App & Web Unification**:
  - Created `admin-web/.well-known/assetlinks.json` declaring package `com.rosterx` with `delegate_permission/common.get_login_creds` and debug SHA-256 certificate fingerprint (`EA:81:06:66:4C:EF:E8:51:FD:1B:54:17:D1:77:DD:E6:EB:83:FF:AA:9B:DA:D9:17:33:F5:CC:1D:51:0B:43:7D`).
  - Added route `/.well-known/assetlinks.json` in `server.py` to serve assetlinks for web portal sync.
  - Updated `admin-web/login.html` and `admin-web/user-portal.html` with standard semantic autocomplete attributes (`username`, `current-password`, `new-password`) and W3C `PasswordCredential` storage.

---

## 🖥️ Recap of v1.23.5 (Build 84) Admin Dashboard
- **Telemetry Mission Control**: Live CPU/RAM RSS, Python version, uptime, Render edge gateway status, MongoDB Atlas storage metrics (KB/MB) and collection stats.
- **Full-Screen Firebase Analytics**: 1-tap expansion into a full-window modal displaying session events, IST timestamps, parameters, and web console link.

---

## 🛠️ Build & Verification
- Cleanly compiled via `safe_build.bat assembleDebug` (Build successful in 51m 43s, lock released).
- APK ready at `android/app/build/outputs/apk/debug/app-debug.apk`.
