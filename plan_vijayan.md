# 📋 Execution Plan for Vijayan (UI & Jetpack Compose)

**Status:** `ACTIVE / READY TO EXECUTE`

---

## 1. Shared API & Data Contract

### 🔐 Endpoints & HTTP Methods
* `GET api/credentials/trackers` -> Returns `{ success: true, data: { trackers: [...], total_count, expiring_soon_count, expired_count } }`
* `POST api/credentials/trackers` -> Create new tracker (body: `server_name`, `username`, `last_updated_date`, `validity_days`, `spokesperson_emails`)
* `PUT api/credentials/trackers/{id}` -> Update tracker
* `POST api/credentials/trackers/{id}/renew` -> 1-Tap reset `last_updated_date` to today
* `DELETE api/credentials/trackers/{id}` -> Remove tracker

### 📄 DTO Schema (`CredentialTrackerDto`)
```kotlin
data class CredentialTrackerDto(
    val id: String,
    val serverName: String,
    val username: String,
    val lastUpdatedDate: String, // "YYYY-MM-DD"
    val validityDays: Int,
    val expiryDate: String, // "YYYY-MM-DD"
    val daysRemaining: Int,
    val status: String, // "HEALTHY", "EXPIRING_SOON", "EXPIRES_TODAY", "EXPIRED"
    val spokespersonEmails: List<String>,
    val createdBy: String? = null,
    val updatedAt: Long? = null
)
```

---

## 2. Technical Blueprint for Vijayan

### 📁 Primary Files to Modify / Create:
1. `android/app/src/main/java/com/rosterx/data/remote/RosterApiService.kt` (Add Retrofit endpoints & DTOs)
2. `android/app/src/main/java/com/rosterx/domain/repository/RosterRepository.kt` & `RosterRepositoryImpl.kt` (Data layer)
3. `android/app/src/main/java/com/rosterx/presentation/portal/EngineerPortalViewModel.kt` (State management)
4. `android/app/src/main/java/com/rosterx/presentation/portal/EngineerPortalScreen.kt` (Bottom Nav Tab 4: "More")
5. `android/app/src/main/java/com/rosterx/presentation/portal/more/MoreHubView.kt` (NEW - Feature launcher hub)
6. `android/app/src/main/java/com/rosterx/presentation/portal/credentials/CredentialTrackerScreen.kt` (NEW - Full tracker UI & Dialogs)

### 🛠️ Step-by-Step UI Implementation Tasks:

1. **Bottom Navigation Bar: Add Tab 4 "More" (`portalNavItems`)**:
   - In `EngineerPortalScreen.kt`:
     - **For Duty Engineers**:
       - `0`: Dashboard (`Icons.Default.Dashboard`)
       - `1`: Team Shifts (`Icons.Default.Group`)
       - `2`: My Calendar (`Icons.Default.CalendarMonth`)
       - `3`: Month Roster (`Icons.Default.GridView`)
       - `4`: More (`Icons.Default.MoreHoriz`)
     - **For Managers**:
       - `0`: Dashboard (`Icons.Default.Dashboard`)
       - `1`: Team Shifts (`Icons.Default.Group`)
       - `2`: Month Roster (`Icons.Default.GridView`)
       - `3`: Broadcast (`Icons.Default.Campaign`)
       - `4`: More (`Icons.Default.MoreHoriz`)
   - Tab 4 renders `MoreHubView`.
   - Settings remains accessible from the TopBar Profile Pill.

2. **Build `MoreHubView.kt`**:
   - Clean Neumorphic feature launcher dashboard.
   - Header with title "Operations Hub" or "Extra Features".
   - Feature Card: **"Server Credential Tracker"**:
     - Icon: `Icons.Default.VpnKey` or `Icons.Default.Password`.
     - Description: "Track server password validity & automated 9 AM expiry alerts".
     - Badges: Live count badge (e.g. `2 Expiring Soon` in warning orange, or `All Healthy` in green).
     - Clicking the card opens `CredentialTrackerScreen`.
   - Placeholder Card: "Operations Toolkit" (labeled "Upcoming").

3. **Build `CredentialTrackerScreen.kt`**:
   - **Header & Stats**:
     - TopBar with Back navigation to More Hub, title "Credential Tracker", and "+ Add Tracker" action.
     - Summary KPI Bar: Total Trackers | Expiring Soon (Orange) | Expired (Red).
   - **Search & Filter**: Filter by server name, username, or status chips (All, Expiring Soon, Expired, Healthy).
   - **Credential Cards (`CredentialTrackerCard`)**:
     - Server Name (Bold, Neumorphic heading) & Username.
     - Expiry Status Badge:
       - Expired: Red pulsing pill (`Expired Xd ago`).
       - Expires Today: Red pill (`Expires Today!`).
       - Expiring Soon: Amber pill (`Expires in X days`).
       - Healthy: Soft green pill (`X days remaining`).
     - Dates: Last Updated (`YYYY-MM-DD`), Validity period (`X days`), Expiry Date (`YYYY-MM-DD`).
     - Spokesperson Email Chips (rendered cleanly as flow row).
     - **1-Tap Quick Renewal Action**:
       - Button: "Renewed Today" (`Icons.Default.CheckCircle`).
       - Tapping prompts quick confirmation and calls `viewModel.renewCredentialTracker(id)`.
     - Overflow / Action Menu: Edit and Delete.

4. **Build "Add / Edit Credential Tracker Dialog"**:
   - Neumorphic modal dialog.
   - Text fields for:
     - Server Name (e.g. `prod-db-01`)
     - Username (e.g. `svc_analytics`)
     - Last Updated Date (Date picker dialog or formatted date field, defaulting to today)
     - Validity Days (Preset quick-select chips: 30d, 60d, 90d, 180d, plus custom input)
     - Spokesperson Emails: dynamic chip-adder where user types email, taps "+", and chips appear with (X) remove button.
   - Save button with loading indicator and validation checks.

5. **Connect State in `EngineerPortalViewModel.kt`**:
   - `credentialTrackers: List<CredentialTrackerDto>`
   - `isLoadingCredentials: Boolean`
   - Methods: `loadCredentialTrackers()`, `createCredentialTracker(...)`, `updateCredentialTracker(...)`, `renewCredentialTracker(id)`, `deleteCredentialTracker(id)`.

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
   - Launch app: `adb shell am start -n com.rosterx/.MainActivity`.
   - Verify:
     - Bottom navigation bar displays 5 tabs with "More" (`...`) as the 5th tab.
     - Tapping "More" opens `MoreHubView`.
     - Tapping "Server Credential Tracker" opens `CredentialTrackerScreen`.
     - Tap "+ Add Tracker", create a test entry, verify it appears in the list with calculated status.
     - Test "Renewed Today" 1-tap action.
4. **Version Bump & Git Commit**:
   - Minor feature release -> Bump to **`1.25.0` (Build 107)** in:
     - `android/app/build.gradle.kts` (`versionCode = 107`, `versionName = "1.25.0"`)
     - `server.py` (`SERVER_VERSION = "1.25.0"`)
   - Commit changes:
     ```powershell
     git add android/ server.py AGENT_HANDOFF.md plan_vijayan.md
     git commit -m "feat(credentials): v1.25.0 (Build 107) - add More tab feature hub and server credential tracker with 1-tap renewal"
     ```
5. **Update `AGENT_HANDOFF.md`** with Build 107 verification details.
