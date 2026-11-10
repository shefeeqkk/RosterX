# 🕵️‍♂️ Dasan (Antigravity Agent 1) ⇄ 🕵️ Vijayan (Antigravity Agent 2) Handoff Bridge

> **"Dasan looks after core orchestration, backend & builds; Vijayan crafts Jetpack Compose UI & client features!"**  
> *Last Synced by Dasan:* **2026-11-10 20:00 IST** 🕵️‍♂️⚡🌴

---

## 📌 Active System State & Monorepo Location
- **Unified Workspace**: `c:\shefeeq\PYTHON\RosterX`
- **Native Android Project Root**: `c:\shefeeq\PYTHON\RosterX\android`
- **Semantic Version**: **`1.23.10`** (Android `versionCode: 89`, Server `SERVER_VERSION = "1.23.10"`)
- **Previous Versions**: `1.23.9` (Build 88), `1.23.8` (Build 87), `1.23.7` (Build 86), `1.23.6` (Build 85)
- **Production Server**: ☁️ **Render Cloud + Branded Cloudflare Domain**  
  🌐 `https://rosterx.online` & `https://api.rosterx.online`  
  📱 **Android Base URL**: `https://api.rosterx.online/api/`
- **Database Engine**: MongoDB Atlas `Cluster0` (Database: `rosterx`)
- **Target Device (Tailscale IP)**: `100.71.241.36:5555` (Connected & Verified)

---

## 🚀 Solved in v1.23.10 (Build 89)

### 1. 🛡️ Complete Elimination of 401 Sign-Out Loop Error
- **Root Cause Isolated**:
  - When signing out, `sessionManager.clearSession()` cleared `auth_token`.
  - However, because `LoginViewModel` retained `loginSuccess = true` from the initial session, returning to `LoginScreen` immediately triggered `completeLogin()` which cached `saved_user` into SharedPreferences *without* a token.
  - Furthermore, `loadSavedUser()` previously loaded user profiles even when `auth_token` was null, leading `MainActivity` to navigate to `EngineerPortalScreen` with an unauthenticated session, resulting in a persistent 401 Unauthorized (`"Authentication required. Please log in."`) error screen.
- **The Fix**:
  - `UserSessionManager`: `loadSavedUser()` strictly returns `null` if `auth_token` is null or empty.
  - `HostSelectionInterceptor`: Intercepts any 401 HTTP response on authenticated endpoints and immediately calls `sessionManager.clearSession()`.
  - `EngineerPortalViewModel`: Resets session and state upon receiving a 401 error.
  - `LoginViewModel`: Cleanly resets `loginSuccess = false`, `lastSuccessCredentials = null`, and `loggedInEngineer = null` on logout and on `LoginScreen` mount.
  - **Result**: Tapping "Sign Out" now lands cleanly on `LoginScreen` with empty fields and no errors.

### 2. 🔑 Google Password Manager "Save password to Google?" Fixed
- **Root Cause Isolated**:
  - When the soft keyboard was active, Credential Manager's One-Tap save prompt was suppressed or backed down with `[28431] Skipping password saving since the user is likely prompted with Android Autofill`.
  - Single-Activity Compose architecture never called `AutofillManager.commit()`, so system autofill never received the trigger to show its save sheet.
- **The Fix**:
  - `LoginScreen.kt`: On successful credential verification, soft keyboard is dismissed with `keyboardController?.hide()`, `AutofillManager.commit()` is explicitly dispatched to Android OS, and a brief 250ms settle delay precedes the Google Credential Manager `CreatePasswordRequest`.
  - Canonical username (`shefeeqkk`) is used for the save request.
  - **Live Verification**: User confirmed: *"logged in now asking the prompt"* on physical device `100.71.241.36:5555`.

---

## 🛠️ Build & Verification
- Compiled via `safe_build.bat assembleDebug` (Build successful).
- Stream-installed onto physical device `100.71.241.36:5555` via ADB (`Performing Streamed Install -> Success`).
- Successfully authenticated `shefeeqkk` / `Shefeeq@123` directly on device, verified full Dashboard load, and verified clean Sign Out.
