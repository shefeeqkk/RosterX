# 📋 Execution Plan for Vijayan (UI & Jetpack Compose)

**Status:** `COMPLETED` (Sprint v1.26.0 - Build 109)

---

## 1. Architectural Mandates from Gafoor (All Executed & Verified)

Vijaya, Gafoorka layout clear aakki thannittundu. Client UI and Admin UI require surgical enhancements. Follow the Neumorphic design tokens and Jetpack Compose best practices strictly.

### 🎯 Scope of Work:
1. **Admin UI Overhaul (`AdminMainScreen.kt`)**:
   - **Remove Preview Mode**:
     - Remove `previewAsEngineer` state, the top-bar "Preview App" button, and the floating "Switch to Admin" button.
   - **Top-Right Account Pill**:
     - Place the User Avatar + First Name pill on the top-right of `AdminMainScreen` (identical to `EngineerPortalScreen`).
     - Tapping this toggles `AccountView` / `isViewingSettings` (Profile picture, name edit, theme toggle, logout).
   - **Bottom Navigation**:
     - Change tab 4 from "Settings" (`Icons.Default.Settings`) to "More" (`Icons.Default.MoreHoriz`).
     - Tabs: `[0: Dashboard, 1: Register, 2: Rosters, 3: Broadcast, 4: More]`.
   - **Admin More Tab**:
     - Render `MoreHubView` configured for Admin:
       - **Server Credential Tracker** card (navigates to `CredentialTrackerScreen` with Admin powers).
       - **Server Management** card (opens the `ServerManagementTab` view with Backend URL, Atlas diagnostics, and Server logs).
       - *Note: "My Team" card is excluded for Admin because Admin already has the Register/Manage Team tab.*

2. **Login Screen Soft Keyboard Fix (`LoginScreen.kt`)**:
   - In the password `OutlinedTextField`:
     - Inside `keyboardActions = KeyboardActions(onDone = { ... })`:
       - Add `keyboardController?.hide()` and `focusManager.clearFocus()` immediately before `viewModel.login(...)`.
     - Ensure the IME soft keyboard reliably closes when clicking the tick/Done/Return action on the virtual keyboard.

3. **Credential Tracker Threshold, Validity & Action Pill Styling (`CredentialTrackerScreen.kt`)**:
   - **Remove Validity Quick Chips**:
     - Remove the `30d, 45d, 60d, 90d` preset chips from `AddEditCredentialTrackerDialog`.
     - Keep only the clean manual number input: "Validity Cycle (Days)".
   - **Manager/Admin Alert Threshold Setting**:
     - Add "Alert Threshold (Days before expiry)" manual number input (visible ONLY when `isManagerOrAdmin == true`, default 5).
     - Display a dynamic calculation badge:
       `"Alarming begins on: <expiry_date - threshold_days> (X days before expiry)"`.
     - Update `onSave` lambda, `EngineerPortalViewModel.createCredentialTracker`, and `updateCredentialTracker` to pass `alertThresholdDays`.
   - **Redesign 'Update Date' Action Pill**:
     - Currently looks identical to the passive status pill ("28d remaining").
     - Redesign as an **Elevated Neumorphic Action Pill**:
       - Full stadium / pill shape (`RoundedCornerShape(20.dp)` or `CircleShape`).
       - Subtle shadow elevation (`3.dp` or `NeumorphicElevationStyle.RaisedSubtle`) with Neumorphic Surface color.
       - Emerald action accent: Emerald calendar icon (`Icons.Default.EditCalendar`), crisp bold text `"Update Date"`, and subtle emerald border highlight.
       - Visually distinct as an interactive CTA button rather than a flat status tag.

4. **More Tab & "My Team" Directory (`MoreHubView.kt` & `MyTeamView.kt`)**:
   - **Remove Dummy Card**: Remove "Duty Handover Notes" card from `MoreHubView.kt`.
   - **Add "My Team" Card** (visible in Manager and User More tabs):
     - Title: "My Team"
     - Description: "Team directory, hierarchy, and direct contact options."
     - Icon: `Icons.Default.Groups`
     - Badge: e.g. "X Active Members"
     - Tapping opens the dedicated full sub-page `MyTeamView`.
   - **`MyTeamView.kt` Specifications**:
     - Top bar with Back button (returns to More Hub).
     - Search bar for quick filtering by name or designation.
     - Section 1: **"Management / Managers"** (Engineers with `role == EngineerRole.MANAGER`).
     - Section 2: **"Shift Engineers / Team"** (Engineers with `role == EngineerRole.ENGINEER || role == EngineerRole.APPRENTICE`, excluding `status == EngineerStatus.RESIGNED`).
     - Member Item Card:
       - Neumorphic surface card with Avatar, Full Name, Designation/Role, and Phone Number.
       - **Call Button**: `Intent(Intent.ACTION_DIAL, Uri.parse("tel:$cleanPhone"))`. Grayed out/disabled if phone is empty with Toast.
       - **WhatsApp Button**: `Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$waPhone"))`. Grayed out/disabled if phone is empty with Toast.
       - Phone normalizer: Strips spaces/dashes; ensures country prefix (+91 for 10-digit numbers).

5. **Post-Build Activities (Mandatory)**:
   - Version bump in `android/app/build.gradle.kts`:
     - `versionName = "1.26.0"`
     - `versionCode = 109`
   - Build: `cd android && .\safe_build.bat assembleDebug`.
   - ADB Install: `adb install -r android/app/build/outputs/apk/debug/app-debug.apk`.
   - Verify UI on emulator/device.
   - Commit changes and update `AGENT_HANDOFF.md`.
