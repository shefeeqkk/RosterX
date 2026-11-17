# 📋 Execution Plan for Vijayan (UI & Jetpack Compose)

**Status:** `READY FOR VIJAYAN` (Sprint v1.30.0 - Build 128)

---

## 1. Architectural Mandates from Gafoor (System Architect 🕶️)

Vijaya! Credential Tracker-il **Admin and Manager-kk maathram** password update/renewal history kaanaanulla **Single Audit Logs Viewer** architect cheythu blueprint ready aakki vechittundu.

### 🎯 Key Design Directives:
1. **Access Control**:
   - Strictly restricted to `isManagerOrAdmin == true`. Regular engineers should not see the logs button or viewer.
2. **Top-Bar Action Button**:
   - In `CredentialTrackerScreen.kt` TopAppBar actions:
     Next to `Refresh` and `+ Add Tracker`, place an **Audit Logs Button** (`Icons.Default.History`) with subtle Neumorphic styling, visible only if `isManagerOrAdmin == true`.
3. **Single Neumorphic Audit Modal (`CredentialTrackerLogsSheet.kt`)**:
   - Full-featured Neumorphic Bottom Sheet or Dialog showing an aggregated timeline of updates across **all accounts and servers**:
     - Header: Title `"Credential Update Logs"`, subtitle `"Authoritative audit trail of updates & renewals"`, Close button & Refresh button.
     - Search Bar: Quick search by server name, account username, or engineer who updated.
     - Action Filter Chips: `ALL`, `RENEWED`, `UPDATED`, `CREATED`.
     - Log Card Timeline:
       - Server & Account pill: `[NMS Server] • root`
       - Action Badge:
         - `RENEWED` (Emerald `#10B981` pill with `Icons.Default.Autorenew`)
         - `UPDATED` (Indigo `#6366F1` pill with `Icons.Default.Edit`)
         - `CREATED` (Teal `#059669` pill with `Icons.Default.Add`)
         - `DELETED` (Rose `#EF4444` pill with `Icons.Default.Delete`)
       - Who Updated: Avatar / user icon + Name + `@username` (e.g. `Shefeeq K K (@shefeeqkk)`)
       - Date Transition Diff:
         `Previous: 16-Oct-2026 ➔ New: 17-Nov-2026`
       - Summary: `Password updated to 17-Nov-2026 (Valid for 45 days, expires 01-Jan-2027)`
       - IST Timestamp: `17-Nov-2026, 09:29 PM IST`

---

## 2. Scope of Work for Vijayan

### 1. Data Layer (`RosterApiService.kt` & `RosterRepositoryImpl.kt`)
- Define DTOs in `com.rosterx.data.remote`:
  ```kotlin
  data class CredentialTrackerLogDto(
      val id: String = "",
      @SerializedName("tracker_id") val trackerId: String = "",
      @SerializedName("server_name") val serverName: String = "",
      val username: String = "",
      val action: String = "",
      @SerializedName("action_label") val actionLabel: String = "",
      @SerializedName("updated_by") val updatedBy: String = "",
      @SerializedName("updated_by_name") val updatedByName: String = "",
      @SerializedName("previous_date") val previousDate: String? = null,
      @SerializedName("new_date") val newDate: String? = null,
      @SerializedName("validity_days") val validityDays: Int? = null,
      @SerializedName("expiry_date") val expiryDate: String? = null,
      val summary: String = "",
      val timestamp: Long = 0L,
      @SerializedName("datetime_str") val datetimeStr: String = ""
  )

  data class CredentialTrackerLogsResponse(
      val success: Boolean = false,
      val logs: List<CredentialTrackerLogDto> = emptyList(),
      val count: Int = 0
  )
  ```
- In `RosterApiService.kt`:
  ```kotlin
  @GET("api/credentials/trackers/logs")
  suspend fun getCredentialTrackerLogs(): Response<CredentialTrackerLogsResponse>
  ```
- In `RosterRepository.kt` & `RosterRepositoryImpl.kt`:
  Add `suspend fun getCredentialTrackerLogs(): Result<List<CredentialTrackerLogDto>>`.

### 2. ViewModel Integration (`EngineerPortalViewModel.kt`)
- In `EngineerPortalState`:
  ```kotlin
  val credentialTrackerLogs: List<CredentialTrackerLogDto> = emptyList(),
  val isLoadingCredentialLogs: Boolean = false
  ```
- In `EngineerPortalViewModel`:
  ```kotlin
  fun loadCredentialTrackerLogs(silent: Boolean = false) {
      viewModelScope.launch {
          _state.value = _state.value.copy(isLoadingCredentialLogs = !silent)
          val result = repository.getCredentialTrackerLogs()
          result.onSuccess { logs ->
              _state.value = _state.value.copy(credentialTrackerLogs = logs, isLoadingCredentialLogs = false)
          }.onFailure {
              _state.value = _state.value.copy(isLoadingCredentialLogs = false)
          }
      }
  }
  ```

### 3. Modern Neumorphic Component (`CredentialTrackerLogsSheet.kt`)
- Create `android/app/src/main/java/com/rosterx/presentation/portal/credentials/CredentialTrackerLogsSheet.kt`.
- Present as `ModalBottomSheet` or Neumorphic Dialog:
  - Header: Title, subtitle, Close, and Refresh button.
  - Search field: `NeumorphicTextField` searching server, account, or updater name.
  - Action Chips: `ALL`, `RENEWED`, `UPDATED`, `CREATED`.
  - Empty state: Neumorphic card with "No audit logs found".
  - Scrollable LazyColumn of log cards with crisp typographic hierarchy and Neumorphic elevation.

### 4. Wire Up in `CredentialTrackerScreen.kt`
- Add state: `var showLogsSheet by remember { mutableStateOf(false) }`.
- In `TopAppBar` actions:
  ```kotlin
  if (isManagerOrAdmin) {
      IconButton(onClick = {
          viewModel.loadCredentialTrackerLogs()
          showLogsSheet = true
      }) {
          Icon(
              imageVector = Icons.Default.History,
              contentDescription = "Audit Logs",
              tint = NeumorphicTokens.AccentSalmon
          )
      }
  }
  ```
- Render `CredentialTrackerLogsSheet(show = showLogsSheet, onDismiss = { showLogsSheet = false }, logs = state.credentialTrackerLogs, isLoading = state.isLoadingCredentialLogs, onRefresh = { viewModel.loadCredentialTrackerLogs() })`.

### 5. Post-Build Activities (Mandatory)
- Version bump in `android/app/build.gradle.kts`:
  - `versionName = "1.30.0"`
  - `versionCode = 128`
- Execute safe build:
  - `cd android && .\safe_build.bat assembleDebug`
- Install on device via ADB:
  - `adb install -r android/app/build/outputs/apk/debug/app-debug.apk`
- Verification:
  - Open Credential Tracker in Admin and Manager profiles. Verify "History" icon appears in top bar.
  - Tap icon: verify single audit sheet opens with all servers & accounts listed.
  - Verify who updated, previous/new dates, and IST timestamps are displayed cleanly.
- Commit changes and update `AGENT_HANDOFF.md`.
