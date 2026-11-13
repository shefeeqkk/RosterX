# 📋 Execution Plan for Vijayan (UI & Jetpack Compose)

**Status:** `COMPLETED`

---

## 1. Shared API & Data Contract
* **Endpoints**: No API modifications. Zero backend changes.
* **Component Target**: `com.rosterx.presentation.portal.EngineerPortalScreen.kt`
* **Target Version Bump**: `1.24.15` (Build 106)

---

## 2. Technical Blueprint: Streamlined 4-Tab Bottom Nav & TopBar Profile Navigation

### 🎯 Objective:
Remove redundant "Settings" tab (Tab 4) from the bottom navigation bar in `EngineerPortalScreen.kt`. Consolidate Profile and Settings into a unified fullscreen view accessed exclusively through the TopBar Profile Pill, featuring a dedicated TopBar Back arrow, system `BackHandler`, and hidden bottom bar while viewing Settings.

### 📁 Primary File to Modify:
* `android/app/src/main/java/com/rosterx/presentation/portal/EngineerPortalScreen.kt`

### 🛠️ Step-by-Step Implementation Details:

1. **Remove Tab 4 from `portalNavItems`**:
   - In `EngineerPortalScreen.kt` (around lines 932–950):
     - **For Managers**: Reduce to exactly 4 tabs:
       - `0`: Dashboard (`Icons.Default.Dashboard`)
       - `1`: Team Shifts (`Icons.Default.Group`)
       - `2`: Month Roster (`Icons.Default.GridView`)
       - `3`: Broadcast (`Icons.Default.Campaign`)
     - **For Duty Engineers**: Reduce to exactly 4 tabs:
       - `0`: Dashboard (`Icons.Default.Dashboard`)
       - `1`: Team Shifts (`Icons.Default.Group`)
       - `2`: My Calendar (`Icons.Default.CalendarMonth`)
       - `3`: Month Roster (`Icons.Default.GridView`)

2. **Manage Settings Fullscreen Navigation State**:
   - Add state: `var isViewingSettings by rememberSaveable { mutableStateOf(false) }` and `var previousTab by rememberSaveable { mutableIntStateOf(0) }`.
   - When user taps the TopBar Profile Pill:
     ```kotlin
     previousTab = selectedTab
     isViewingSettings = true
     ```

3. **TopBar Adaptation for Profile & Settings**:
   - When `isViewingSettings == true`:
     - TopBar shows a Back Navigation Icon (`Icons.AutoMirrored.Filled.ArrowBack` or `Icons.Default.ArrowBack`) on the left to exit back to `previousTab`.
     - TopBar title displays "Profile & Settings" (styled with `NeumorphicTokens.AccentSalmon`, ExtraBold).
     - The Profile Pill in the TopBar actions is hidden.
   - When `isViewingSettings == false`:
     - Standard TopBar with RosterX logo, title, Notification Bell, and Profile Pill is displayed.

4. **Bottom Navigation Bar Visibility**:
   - Update `bottomBar` composable condition:
     - `if (!isFullScreenRoster && !isViewingSettings)` -> Render `FluidNeumorphicBottomNav`.
     - When `isViewingSettings == true`, the bottom nav is completely hidden, providing a clean fullscreen layout for `AccountView`.

5. **Screen Content Switching & Hardware Back Press**:
   - Inside the main content `Box`:
     - If `isViewingSettings == true`, render `AccountView` directly with standard padding.
     - Else, render the standard animated 4-tab content (`AnimatedContent(targetState = selectedTab)`).
   - Add system `BackHandler`:
     ```kotlin
     BackHandler(enabled = isViewingSettings) {
         isViewingSettings = false
         selectedTab = previousTab
     }
     ```

6. **Screen View Analytics**:
   - Update `LaunchedEffect(selectedTab, isViewingSettings)` to log screen view `"Profile & Settings"` when `isViewingSettings == true`.

---

## 3. Post-Build Activities (Mandatory for Vijayan)
1. **Safe Android Build**:
   ```powershell
   cd android; .\safe_build.bat assembleDebug
   ```
2. **Install on ADB Device**:
   ```powershell
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```
3. **Automated UI Interaction & Layout Verification**:
   - Launch app:
     ```powershell
     adb shell am start -n com.rosterx/.MainActivity
     ```
   - Verify:
     - Bottom navigation bar displays exactly 4 tabs.
     - Tap the TopBar profile pill -> navigates to fullscreen Profile & Settings with TopBar Back arrow.
     - Tap TopBar back arrow (or system back) -> returns smoothly to previous active tab with bottom bar restored.
4. **Version Bump & Git Commit**:
   - Bump version to **`1.24.15` (Build 106)** in:
     - `android/app/build.gradle.kts` (`versionCode = 106`, `versionName = "1.24.15"`)
     - `server.py` (`SERVER_VERSION = "1.24.15"`)
   - Commit changes:
     ```powershell
     git add android/app/src/main/java/com/rosterx/presentation/portal/EngineerPortalScreen.kt android/app/build.gradle.kts server.py AGENT_HANDOFF.md plan_vijayan.md
     git commit -m "feat(navigation): v1.24.15 (Build 106) - consolidate settings into topbar profile pill and streamline 4-tab bottom nav"
     ```
5. **Update `AGENT_HANDOFF.md`** with verification summary and Build 106 milestone.
