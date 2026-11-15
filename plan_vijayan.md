# 📋 Execution Plan for Vijayan (UI & Jetpack Compose)

**Status:** `ACTIVE / READY TO EXECUTE`

---

## 1. Shared API & Data Contract

### 🔐 Endpoint Enhancements
* `POST api/credentials/trackers/{id}/renew` now accepts optional body `{"last_updated_date": "YYYY-MM-DD"}` to allow picking any rotation date from a DatePicker.
* Target Version Bump: **`1.25.1` (Build 108)**.

---

## 2. Technical Blueprint for Vijayan

### 📁 Primary Files to Modify:
1. `android/app/src/main/java/com/rosterx/presentation/portal/more/MoreHubView.kt`
2. `android/app/src/main/java/com/rosterx/presentation/portal/credentials/CredentialTrackerScreen.kt`
3. `android/app/src/main/java/com/rosterx/presentation/portal/EngineerPortalViewModel.kt`
4. `android/app/src/main/java/com/rosterx/data/remote/RosterApiService.kt`

### 🛠️ Step-by-Step UI Enhancements:

1. **Clean Redundant Card in `MoreHubView.kt`**:
   - Remove Feature 2 ("Automated Expiry Alerts" card) completely.
   - Retain only the primary "Server Credential Tracker" card (with live expiry counters badge) and the upcoming placeholder card.

2. **Server-Grouped Compact Tracker List in `CredentialTrackerScreen.kt`**:
   - Group `filteredTrackers` by `serverName` (`val grouped = filteredTrackers.groupBy { it.serverName }`).
   - Render each Server as a distinct section card:
     - Header: Server Name (Bold, e.g. `prod-db-01`), server icon, and total accounts count badge.
     - Nested List: Sleek, compact rows for each username on that server:
       - Username (e.g. `root`, `svc_analytics`).
       - Compact Expiry status badge (e.g. `Expiring in 3d` in amber, `Expired 2d ago` in red, `28d remaining` in soft green).
       - Expiry date (`YYYY-MM-DD`).
       - Sleek **"Update Date"** action button (calendar icon) for rotating credentials.
       - Edit and Delete icon buttons (for Admins/Managers).
   - Significantly reduce vertical padding and visual bloat for a tight, high-density layout.

3. **Role-Based Spokesperson Email Display**:
   - Hide the spokesperson email chips on cards for regular duty engineers.
   - Show spokesperson emails ONLY when `isManagerOrAdmin == true`.

4. **Calendar DatePicker for Updating Rotation Date**:
   - Replace the static "Renewed Today" chip with a dedicated **"Update Date"** button (`Icons.Default.EditCalendar` or `Icons.Default.CalendarMonth`).
   - When clicked, open Material3 `DatePickerDialog`:
     - Highlight today's date.
     - Include a quick **"Today"** shortcut button at the top/bottom of the dialog.
     - On date selection, call `viewModel.renewCredentialTracker(id = tracker.id, renewDate = selectedDateStr)` to update the rotation date to the selected date.

5. **Healthy Filter Chip Counter**:
   - In the filter chips row:
     ```kotlin
     val healthyCount = state.credentialTrackers.count { it.status == "HEALTHY" }
     FilterChipItem(
         label = "Healthy ($healthyCount)",
         isSelected = selectedFilter == "HEALTHY",
         selectedColor = Color(0xFF059669),
         onClick = { selectedFilter = "HEALTHY" }
     )
     ```

6. **Autocomplete Dropdowns for Server Name & Username**:
   - In `AddEditCredentialTrackerDialog`:
     - Extract distinct existing server names (`state.credentialTrackers.map { it.serverName }.distinct()`).
     - Extract distinct existing usernames (`state.credentialTrackers.map { it.username }.distinct()`).
     - Provide dropdown suggestions (`ExposedDropdownMenuBox` or popup menu) when the user focuses or types in the Server Name and Username text fields.

7. **DatePicker in Add/Edit Tracker Dialog**:
   - Replace the raw text input for "Last Rotated Date" with a clickable field that opens `DatePickerDialog`.
   - Provide a quick "Set to Today" button alongside it.

8. **Pre-Made Validity Presets Update**:
   - Change `validityPresets` in `AddEditCredentialTrackerDialog` from `listOf(30, 60, 90, 180)` to **`listOf(30, 45, 60, 90)`**.
   - Retain custom day input for arbitrary validity periods.

9. **Teammates Dropdown for Adding Spokesperson Emails**:
   - In `AddEditCredentialTrackerDialog`, replace or enhance the email input with a dropdown/picker showing all teammates from `rosterEngineers`:
     - Shows each engineer's Full Name and Email address.
     - Selecting a teammate automatically adds their email as a spokesperson chip.
     - Also retain manual email entry for external/custom email addresses.

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
   - Launch: `adb shell am start -n com.rosterx/.MainActivity`.
   - Verify:
     - More tab shows only 1 clean Credential Tracker card.
     - Credential Tracker list is grouped by Server Name with compact username rows.
     - Healthy filter chip displays counter `Healthy (X)`.
     - Tapping "Update Date" opens Calendar DatePicker with Today option.
     - Add Tracker dialog offers Server Name / Username autocomplete, 30/45/60/90 validity presets, and Teammates dropdown.
     - Spokesperson emails are hidden for regular engineers and visible for Managers.
4. **Version Bump & Git Commit**:
   - Bump to **`1.25.1` (Build 108)** in:
     - `android/app/build.gradle.kts` (`versionCode = 108`, `versionName = "1.25.1"`)
     - `server.py` (`SERVER_VERSION = "1.25.1"`)
   - Commit changes:
     ```powershell
     git add android/ server.py AGENT_HANDOFF.md plan_vijayan.md
     git commit -m "feat(credentials): v1.25.1 (Build 108) - server-grouped compact layout, calendar date pickers, teammate dropdowns, and refined More hub"
     ```
5. **Update `AGENT_HANDOFF.md`** with Build 108 verification notes.
