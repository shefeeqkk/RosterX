# 🕵️‍♂️ Dasan (Antigravity Agent 1) ⇄ 🕵️ Vijayan (Antigravity Agent 2) Handoff Bridge

> **"Dasan looks after core orchestration, backend & builds; Vijayan crafts Jetpack Compose UI & client features!"**  
> *Last Synced by Dasan:* **2026-11-10 18:48 IST** 🕵️‍♂️⚡🌴

---

## 📌 Active System State & Monorepo Location
- **Unified Workspace**: `c:\shefeeq\PYTHON\RosterX`
- **Native Android Project Root**: `c:\shefeeq\PYTHON\RosterX\android`
- **Semantic Version**: **`1.23.7`** (Android `versionCode: 86`, Server `SERVER_VERSION = "1.23.7"`)
- **Previous Versions**: `1.23.6` (Build 85), `1.23.5` (Build 84), `1.23.4` (Build 83), `1.23.3` (Build 82)
- **Production Server**: ☁️ **Render Cloud + Branded Cloudflare Domain**  
  🌐 `https://rosterx.online` & `https://api.rosterx.online`  
  📱 **Android Base URL**: `https://api.rosterx.online/api/`
- **Database Engine**: MongoDB Atlas `Cluster0` (Database: `rosterx`, Storage: ~954 KB, Collections: 9 active, 215 docs)
- **Target Device (Tailscale IP)**: `100.71.241.36:5555` (Connected & Verified)

---

## 🚀 Major Upgrades in v1.23.7 (Build 86)

### 1. 🔑 Native Google Password Manager & Save Dialog Resolution
- **Removed Redundant Autofill Button**: Eliminated the non-standard "Autofill with Google Password Manager" chip button from `LoginScreen.kt`. The login screen is now clean and standard.
- **Deferred Navigation for Save Prompt**:
  - Previously, `sessionManager.setCurrentUser` was called immediately on network success, instantly unmounting `LoginScreen` and navigating to Dashboard before Google Play Services could attach the "Save password to Google?" bottom sheet dialog.
  - Now, navigation is cleanly deferred until after `googlePasswordManager.saveCredentials(activity, ...)` presents the save dialog and the user interacts with it (Save/Never/Dismiss).
- **Explicit Autofill Session Commit**:
  - Added `autofillManager.commit()` via `activity.getSystemService(AutofillManager::class.java)` upon successful credential submission.
  - Fixes Google Play Services One-Tap skipping error `[28431] Skipping password saving since the user is likely prompted with Android Autofill`.
- **Keyboard & Field Autofill**:
  - Native Compose text fields maintain `AutofillType.Username` and `AutofillType.Password`.
  - When the user taps the fields, Gboard / system autofill bar suggests saved accounts right above the keyboard.

---

## 🛠️ Build & Verification
- Cleanly compiled via `safe_build.bat assembleDebug` (Build successful in 4m 55s, lock released).
- Successfully stream-installed onto physical device `100.71.241.36:5555` via ADB (`Performing Streamed Install -> Success`).
- Verified live screen via ADB screenshot `login_v1237.png`.
