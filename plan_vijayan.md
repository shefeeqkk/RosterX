# 📋 Execution Plan for Vijayan (UI & Jetpack Compose)

**Status:** `COMPLETED` (Sprint v1.28.2 - Build 121)

---

## 1. Architectural Mandates from Gafoor (System Architect 🕶️)

Vijaya! User report cheytha requirement:
> *"Add tracker option is required for admin also same like manager"*

Gafoorka codebase deep inspect cheythu root cause 100% catch cheythu vechittundu. Manager-kk `+ Add Tracker` button undaayirunnu, pakshe Admin-kk athu kittathathinte kaaryam:

### 🔍 Root Cause:
1. **Outer TopAppBar Collision in `AdminMainScreen.kt`**:
   - In `AdminMainScreen.kt` (line ~96), Scaffold's `topBar` condition was:
     `if (!isFullScreenRoster)`
     Notice it was missing `&& !isViewingCredentialTracker`!
   - In `EngineerPortalScreen.kt` (Manager), line 716 had:
     `if (!isFullScreenRoster && !isViewingCredentialTracker && !isViewingMyTeam)`
     So when Manager opened Credential Tracker, the outer TopAppBar was hidden, and `CredentialTrackerScreen`'s own TopAppBar (which houses the **`+ Add Tracker`** button & Back button) took over cleanly!
   - In `AdminMainScreen.kt`, because `!isViewingCredentialTracker` was missing, the outer "RosterX Admin" TopAppBar remained drawn on top, completely obscuring and replacing `CredentialTrackerScreen`'s top-bar!
2. **Authority / Role Binding in `CredentialTrackerScreen.kt`**:
   - `CredentialTrackerScreen.kt` checked:
     `val isManagerOrAdmin = state.loggedInEngineer?.role == EngineerRole.ADMIN || state.loggedInEngineer?.role == EngineerRole.MANAGER`
   - `AdminMainScreen.kt` holds the active admin session in `viewModel.currentUser` (`AdminRosterViewModel`), but passes `state = portalState` (`EngineerPortalViewModel`).
   - If `portalState.loggedInEngineer` was delayed or null, `isManagerOrAdmin` evaluated to `false`!

---

## 2. Scope of Work for Vijayan

### 1. Fix TopAppBar Collision in `AdminMainScreen.kt`
- In `AdminMainScreen.kt` line ~96:
  Change:
  ```kotlin
  if (!isFullScreenRoster) {
  ```
  to:
  ```kotlin
  if (!isFullScreenRoster && !isViewingCredentialTracker) {
  ```
  This immediately hides the outer "RosterX Admin" TopAppBar when the Admin enters Credential Tracker, allowing `CredentialTrackerScreen`'s dedicated TopAppBar (with Back arrow, Title, Refresh, and **`+ Add Tracker`**) to render cleanly and prominently — identical to Manager!

- In `AdminMainScreen.kt` subview 1 (line ~276):
  Pass `currentUser = currentUser`:
  ```kotlin
  CredentialTrackerScreen(
      state = portalState,
      viewModel = portalViewModel,
      onBackClick = { isViewingCredentialTracker = false },
      currentUser = currentUser
  )
  ```

### 2. Authoritative Role Binding in `CredentialTrackerScreen.kt`
- In `CredentialTrackerScreen.kt`:
  - Update function signature to accept `currentUser`:
    ```kotlin
    @Composable
    fun CredentialTrackerScreen(
        state: EngineerPortalState,
        viewModel: EngineerPortalViewModel,
        onBackClick: () -> Unit,
        currentUser: Engineer? = null,
        modifier: Modifier = Modifier
    )
    ```
  - Resolve effective user and permissions:
    ```kotlin
    val effectiveUser = currentUser ?: state.loggedInEngineer
    val isManagerOrAdmin = effectiveUser?.role == EngineerRole.ADMIN ||
            effectiveUser?.role == EngineerRole.MANAGER
    val currentUserEmail = effectiveUser?.email?.trim()?.lowercase() ?: ""
    val currentUsername = effectiveUser?.username?.trim()?.lowercase() ?: ""
    ```
- This guarantees:
  1. Top-bar **`+ Add Tracker`** button renders for Admin.
  2. Empty state **`Add First Tracker`** button renders for Admin.
  3. Card actions: Edit, Delete, and Spokesperson management render for Admin.
  4. Add/Edit dialog: Alert Threshold Days & Teammates dropdown render for Admin.

### 3. Post-Build Activities (Mandatory)
- Version bump in `android/app/build.gradle.kts`:
  - `versionName = "1.28.2"`
  - `versionCode = 121`
- Version bump in `server.py`:
  - `SERVER_VERSION = "1.28.2"`
- Execute safe build:
  - `cd android && .\safe_build.bat assembleDebug`
- Install on device via ADB:
  - `adb install -r android/app/build/outputs/apk/debug/app-debug.apk`
- Verification:
  - Launch Admin profile on device/emulator.
  - Navigate to More (Operations Hub) -> Server Credential Tracker.
  - Verify `CredentialTrackerScreen` TopAppBar renders cleanly with Back button, Title, Refresh, and **`+ Add Tracker`** button.
  - Tap `+ Add Tracker` and verify dialog opens with all manager/admin privileges.
- Commit changes and update `AGENT_HANDOFF.md`.
