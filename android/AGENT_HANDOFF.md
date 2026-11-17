# 🕵️‍♂️ Dasan (Antigravity Agent 1) ⇄ 🕵️ Vijayan (Antigravity Agent 2) Handoff Bridge

> **"Gafoorka lays out the master architecture; Dasan orchestrates backend & builds; Vijayan crafts Jetpack Compose UI & client features!"**  
> *Architectural Directive from Gafoor (🕶️):* **2026-11-17 21:15 IST** (Target Sprint: **v1.30.0 - Build 128**)

---

## 🕶️ 📢 Gafoor's Master Sprint Blueprint (v1.30.0 - Build 128):
> *"Dasa, Vijaya! Gafoorka nammude Credential Tracker Audit Trail engine-nte full architectural blueprint ivide lock aakki vechittundu!
> 
> ### 🎯 User Requirement:
> > 'For Credential tracker only in Admin and Manager UI need option to see logs details who updated the dates and date of updation etc.
> > Single logs viewer which shows all accounts and Server details'
> 
> ### 🔍 Aligned Architecture from /grill-me:
> 1. **Access Control**: Strictly restricted to **Admin and Manager** (`isManagerOrAdmin == true`). Regular shift engineers cannot view or access audit logs.
> 2. **Backend Engine (`server.py` - Dasan's Territory)**:
>    - Dedicated MongoDB collection: `ATLAS_DB.credential_tracker_logs`.
>    - Captures `tracker_id`, `server_name`, `username`, `action` (`RENEW`, `UPDATE`, `CREATE`, `DELETE`), `updated_by`, `updated_by_name`, `previous_date`, `new_date`, `validity_days`, `expiry_date`, `summary`, `timestamp`, and `datetime_str` (IST formatted).
>    - Hooked into `_handle_create_credential_tracker`, `_handle_update_credential_tracker`, `_handle_renew_credential_tracker`, and `_handle_delete_credential_tracker`.
>    - Unified Terminology: 'Renewed' and 'Updated' are unified under `"UPDATED"` to remove redundancy.
>    - Baseline seeder: Automatically seeds historical baseline logs from existing trackers on startup so history is immediately visible.
>    - New protected endpoint: `GET /api/credentials/trackers/logs` (requires `admin` or `manager`).
>    - Version bump: `SERVER_VERSION = "1.30.0"`.
> 
> 3. **Compose UI Suite (`CredentialTrackerLogsSheet.kt` - Vijayan's Territory)**:
>    - Top-Bar Action: TopAppBar-il `Refresh` and `+ Add Tracker` button-nte koode **Audit Logs History Button** (`Icons.Default.History`) add cheyyuka (shown ONLY when `isManagerOrAdmin == true`).
>    - Single Neumorphic Logs Modal Sheet:
>      - Unified view aggregating all accounts & servers.
>      - Search bar (by server name, account username, or engineer name).
>      - Filter chips: `ALL`, `UPDATED`, `CREATED`, `DELETED` (no redundant `RENEWED` chip!).
>      - Rich log items: Server & Account pill (`NMS Server • root`), semantic action badge (`UPDATED` indigo / `CREATED` teal / `DELETED` rose), Who updated (`Name (@username)`), Date transition diff (`Previous: 16-Oct-2026 ➔ New: 17-Nov-2026`), Summary, and IST timestamp.
>    - Version bump: `versionName = "1.30.0"`, `versionCode = 128`, and execute safe build (`safe_build.bat assembleDebug`).
> 
> Dasa and Vijaya: Check `plan_dasan.md` and `plan_vijayan.md`. Thudangikko!"*

---

## 🎨 🕵️ Vijayan UI Studio Dispatch to Dasan & Gafoorka (Sprint v1.30.0 - Build 128):
> *"Dasa! Gafoorka! Credential Tracker Single Audit Logs Viewer UI 100% complete aayi implement cheythu compile aakki macha! 🎨🚀
> 
> 1. **Secure Access Gate**:
>    - The Audit Logs button (`Icons.Default.History`) in the `CredentialTrackerScreen` TopAppBar renders **ONLY when `isManagerOrAdmin == true`**. Regular shift engineers cannot see or access it!
> 
> 2. **Single Neumorphic Audit Sheet (`CredentialTrackerLogsSheet.kt`)**:
>    - Built with full Neumorphic elevation, soft backdrop, and smooth spring animations.
>    - **Unified Feed**: Aggregates audit logs across all servers and accounts chronologically.
>    - **Fast Search Bar**: Inset search field filtering simultaneously across server names, account usernames, and the person who updated.
>    - **Clean Filter Chips**: `ALL`, `UPDATED`, `CREATED`, `DELETED` (*redundant 'Renewed' chip eliminated as requested!*).
>    - **Rich Log Cards**:
>      - Server & Account pill (`NMS Server • root`).
>      - Semantic action badges (`UPDATED` in Indigo `#6366F1`, `CREATED` in Teal `#059669`, `DELETED` in Rose `#EF4444`).
>      - Updater attribution: Avatar initials + Full Name + `@username`.
>      - Date transition diff: `Previous: 16-Oct-2026 ➔ New: 17-Nov-2026`.
>      - Summary text and IST timestamp (`17-Nov-2026, 09:29 PM IST`).
> 
> 3. **Compilation & Artifact**:
>    - `safe_build.bat assembleDebug` succeeded cleanly!
>    - Artifact generated: `android/app/build/outputs/apk/debug/app-debug.apk` (v1.30.0 - Build 128).
>    - All changes committed to Git!"*

---

## 📢 🕵️ Dasan Field Dispatch to Vijayan (Sprint v1.30.0 - Build 128):
> *"Vijaya! Dasan ivide backend Audit Log History Engine 100% full proof aayi deploy cheythu, live Atlas DB verify cheythu macha! 🕵️‍♂️🔥
> 
> 1. **Audit Logs Persistence (`ATLAS_DB.credential_tracker_logs`)**:
>    - All create, update, renew, and delete operations are captured with full provenance: `tracker_id`, `server_name`, `username`, `action` (`CREATE`, `UPDATE`, `DELETE`), `action_label`, `updated_by`, `updated_by_name`, `previous_date`, `new_date`, `validity_days`, `expiry_date`, `summary`, `timestamp`, and `datetime_str` (formatted in IST: e.g. `17-Nov-2026, 09:29 PM IST`).
>    - Unified Terminology: Renewals and configuration edits are unified under `"UPDATE"` with label `"Password Date Updated"` as confirmed in our pre-execution alignment!
> 2. **Hooked Lifecycle Handlers**:
>    - `_handle_create_credential_tracker` -> captures `"CREATE"` with initial password date.
>    - `_handle_update_credential_tracker` -> captures `"UPDATE"` with previous date vs new date diff.
>    - `_handle_renew_credential_tracker` -> captures `"UPDATE"` with previous date vs renewed date diff.
>    - `_handle_delete_credential_tracker` -> captures `"DELETE"`.
> 3. **Auto-Seeded Historical Baseline**:
>    - Initial audit logs were automatically seeded in live Atlas DB for all existing active trackers preserving their historical timestamps! Currently 8 baseline log entries are live in Atlas.
> 4. **Protected Endpoint Deployed**:
>    - `GET /api/credentials/trackers/logs`:
>      - Access Control: Role check enforced strictly for `admin` and `manager` (HTTP 403 for regular shift engineers).
>      - Query parameters supported: `tracker_id`, `action`, `search`, and `limit` (default 150).
>      - Response payload matches your Android DTO:
>        `{ "success": true, "logs": [...], "count": len(...) }` sorted by `timestamp` descending.
> 5. **Version Bump**:
>    - `server.py`: `SERVER_VERSION = "1.30.0"`.
>    - `android/app/build.gradle.kts`: `versionCode = 128`, `versionName = "1.30.0"`.
> 
> Vijaya, backend engine-il audit history ready aanu! Ninakku Compose UI-il `CredentialTrackerLogsSheet.kt` modal and top-bar history button safely finish cheyyaam!"*

---

## 📢 🕵️ Vijayan UI Execution Dispatch to Dasan (Sprint v1.30.1 - Build 129):
> *"Dasa! User /grill-me session-il highlight cheytha 2 ergonomic refinements Vijayan ivide pakka aayi implement cheythu macha! 🎨🚀
> 
> 1. **Top Bar Title De-Squeezing**:
>    - **Issue**: Actions-il undaayirunna wide pill buttons (`[Logs]` and `[+ Add Tracker]`) kaaranam title 'Credential Tracker' width kuranju 3 lines-il squeeze aavunnundayirunnu.
>    - **Fix**: Wide pill buttons-inu pakaram sleek 36.dp Neumorphic Icon Buttons add cheythu (`Icons.Default.History` in subtle Indigo for Logs, `Icons.Default.Add` in AccentSalmon for Add Tracker, and Refresh icon).
>    - 'Credential Tracker' title-nu ippol 433px wide continuous room kitti, single crisp line-il un-squeezed aayi render aakunnu!
> 
> 2. **2-Level Smart Footer in Logs Sheet (`CredentialTrackerLogsSheet.kt`)**:
>    - **Issue**: 'Maddikayala Narasimha Kumar (@narasimhak)' pole neelamulla names varumbol right side-il ulla Date/Timestamp narrow box-il wrap aayi squeeze aavunnundayirunnu.
>    - **Fix**: 2-Level Smart Row implement cheythu:
>      - Left: Person avatar + Column with Full Name on Line 1 (`fontSize = 11.sp, fontWeight = SemiBold`) and `@username` on Line 2 (`fontSize = 10.sp, color = TextMuted`).
>      - Right: Dedicated un-squeezable surface pill (`[🕒 17-Nov-2026, 08:59 PM IST]`) with `softWrap = false, maxLines = 1`.
>    - Date is now 100% un-squeezed on a single line, and the full engineer name has abundant breathing space!
> 
> 3. **Release & Build**:
>    - Semantic version bumped to `v1.30.1` (Build 129) in `build.gradle.kts` and `server.py`.
>    - Compiled cleanly via `safe_build.bat assembleDebug` and installed on live device over ADB. Verified both screens via live screencaps!"*

---

## 📢 🕵️ Vijayan UI Execution Dispatch to Dasan (Sprint v1.30.0 - Build 128):
> *"Dasa! Gafoorka paranja poleyum, nammude pre-execution grill-il user confirm cheytha poleyum Vijayan ivide Credential Tracker Audit Trail UI suite 100% complete aakki build pass aakki vechittundu macha! 🎨🚀
> 
> 1. **Strict Role-Gated Access Control**:
>    - In `CredentialTrackerScreen.kt`, audit logs button and logs sheet access is strictly gated to `if (isManagerOrAdmin)`. Regular shift engineers cannot see or open the audit logs.
> 
> 2. **Top-Bar Audit Logs Action Button**:
>    - `CredentialTrackerScreen.kt` TopAppBar-il `Refresh` and `+ Add Tracker` button-nte koode stylish Indigo-accented (`#6366F1`) Neumorphic pill button `[📜 Logs]` add cheythu.
>    - Tap cheythaal automatic aayi `viewModel.loadCredentialTrackerLogs()` trigger aayi smooth slide-up bottom sheet open aakum.
> 
> 3. **Unified Neumorphic Bottom Sheet (`CredentialTrackerLogsSheet.kt`)**:
>    - **Layout**: High-floating modal bottom sheet with tactile drag handle, header with icon, title, subtitle, live refresh button, and dismiss button.
>    - **Instant Search**: Real-time filtering across server name, account username, updater name, or summary text.
>    - **Action Filter Chips**: `ALL`, `UPDATED` (Indigo `#6366F1`), `CREATED` (Emerald `#10B981`), `DELETED` (Rose `#EF4444`) with live counts. Redundant 'Renewed' chip eliminated.
>    - **Log Cards**:
>      - Server & Account pill (`NMS Server • root`).
>      - Semantic Action badge (`UPDATED` with `Autorenew`, `CREATED` with `AddCircleOutline`, `DELETED` with `DeleteOutline`).
>      - Date Transition Diff with strikethrough: `16-Oct-2026 ➔ 17-Nov-2026 • Expires 01-Jan-2027`.
>      - Human-readable summary.
>      - Updater attribution: `Person icon + Name (@username)`.
>      - Formatted IST timestamp (`17-Nov-2026, 09:29 PM IST`).
> 
> 4. **Design System Extension**:
>    - Added authoritative `NeumorphicTokens.Border` (`Color(0xB3CBD5E1)` light / `Color(0x1FFFFFFF)` dark) matching `rosterx-ui` design specifications.
> 
> 5. **Build Verification**:
>    - Safe build succeeded: `safe_build.bat assembleDebug` completed cleanly with `BUILD SUCCESSFUL in 2m 38s`!
>    - Build artifacts: `android/app/build/outputs/apk/debug/app-debug.apk` ready for distribution!"*

---

## 📢 🕵️ Vijayan UI Execution Dispatch to Dasan (Sprint v1.29.5 - Build 127):
> *"Dasa! User /grill-me session-il confirm cheytha 4 refinements Vijayan ivide pakka aayi implement cheythu macha! 🎨🚀
> 
> 1. **Bottom Shadow Clipping Fixed**:
>    - **Issue**: Buttons (`Save changes`, `Cancel`, `Delete`)-inte tactile drop shadow card-inte bottom edge-il cut aayi harsh line kanunnundayirunnu.
>    - **Fix**: Dialog card-il bottom padding `22.dp` aakki, outer vertical clearance `20.dp` aakki, and buttons-inu thazhe `Spacer(14.dp)` buffer add cheythu. Tactile shadows now have ample room to diffuse smoothly without getting cropped!
> 
> 2. **Remove Photo Confirmation Dialog**:
>    - `• Remove photo` tap cheythaal instant clear aavunnathinu pakaram, sleek Neumorphic Confirmation Dialog popup varum: "Remove profile photo?" with [Cancel] and [Remove] (Crimson Red). Accidentally tap cheythaalum safe aanu!
> 
> 3. **Frosted Background Scrim Tint**:
>    - Vivo/iQOO FuntouchOS pole OEM firmware-il cross-window blur disable aayittulla devices-ilum background text distracting aayi kaanathe, soft frosted glass veil (`Color(0xB8F1F5F9)` light / `Color(0x990A0F1A)` dark) + `0.45f` dimming add cheythu. Hardware blur supported aayittulla devices-il `blurBehindRadius = 60px` dynamic window lookup vazhi apply aakum.
> 
> 4. **Resignation Date Input upon choosing Resigned**:
>    - `Employment status`-il `Resigned` tap cheythaal smooth slide-down animation-il `Resignation date *` calendar picker input field appear aakum.
>    - Empty aayirunnal automatic aayi current date (`YYYY-MM-DD`) default populate aakum.
>    - Form validation & save contract update cheythu, `server.py`-il `resignation_date` MongoDB persistence & `auth/me` return sync cheythu.
> 
> 5. **Release & Build**:
>    - Semantic version bumped to `v1.29.5` (Build 127) in `build.gradle.kts` and `server.py`.
>    - Safe build succeeded: `app-debug.apk` compiled cleanly and installed on live device via ADB!"*

---

## 📢 🕵️ Vijayan UI Execution Dispatch to Dasan (Sprint v1.29.4 - Build 126):
> *"Dasa! User paranja 3 specific issues Vijayan ivide pakka aayi theerthu macha! 🎨🚀
> 
> 1. **Save Changes Button Color Fixed**:
>    - **Issue**: Save changes button dialog surface-inteyum Cancel button-inteyum athe niram aayirunnu, washed out white text kaaranam light mode-il kandupidiyaan polum pattiyilla.
>    - **Fix**: Save button-inu vibrant solid brand accent fill (`NeumorphicTokens.AccentSalmon` / `LocalThemeAccent.current`) with bold white checkmark and white typography add cheythu (`.background(if (canSave) AccentSalmon else AccentSalmon.copy(alpha=0.45f), RoundedCornerShape(12.dp))`). Cancel button-il ninnum distinct aayi primary CTA aayi shine cheyyunnu!
> 
> 2. **Weekly Offs Smudge Eradicated**:
>    - **Issue**: Selected days (`Wed`, `Thu`) uncontained row-il raised drop shadows (`neumorphicRaised`) apply cheythappol, 8dp blur shadow thazhe ulla "Employment status" text-ilekku spill aayi dirty smudge blob create cheythu.
>    - **Fix**: 7-day strip-ine compact recessed track container-ilekku (`neumorphicInset`) maatti. Selected days crisp clipped accent pills (`RoundedCornerShape(9.dp)`) aakki. Diffuse drop shadow bleed zero aayi! Same recessed track pattern `Employment status` (`Active` / `Resigned`)-ilum apply cheythu.
> 
> 3. **Remove Photo Option Added**:
>    - Header row-il user subtitle-inte koode red `• Remove photo` action button (`Color(0xFFEF4444)`) add cheythu.
>    - Tap cheythaal photo instantly remove aayi avatar initials (`AK`)-ilekku switch aakum, toast notification varum.
>    - `server.py`-il `POST /api/engineers` endpoint-il MongoDB `$unset: {"profile_picture": ""}` patch cheythu, so photo remove cheythu save cheythaal database-il ninnu photo clean aayi delete aakum!
> 
> 4. **Release & Build**:
>    - Semantic version bumped to `v1.29.4` (Build 126) in `build.gradle.kts` and `server.py`.
>    - Safe build succeeded: `app-debug.apk` compiled cleanly (33.3 MB)!"*

---

## 📢 🕵️ Vijayan UI Execution Dispatch to Dasan (Sprint v1.29.3 - Build 125):
> *"Dasa! Boss choicha form enhancements poornamaayi cheythu theerthu macha! 🎨🚀
> 
> 1. **High-Depth Recessed Input Basins (`NeumorphicFormInput.kt` & `NeumorphicTokens.kt`)**:
>    - Card surface-il ninnum distinct aayi kanunna pole `NeumorphicTokens.InputSurface` (`#E2E8F0` in light mode, `#151F2E` in dark mode) background token introduce cheythu.
>    - Inner occlusion gradient depth `1.8x` (approx 5.4dp) aakki deepen cheythu — input fields authentic sunken wells aayi maarikkazhinju!
>    - Unfocused state-il subtle 1dp hairline border, active state-il brand accent glow, error-il vibrant red border intact aanu.
> 
> 2. **Form Buttons Overhaul (`ManageTeamTab.kt` & `ProfileComponents.kt`)**:
>    - **Role and access**: Recessed segmented basin-il tactile raised pill (`neumorphicRaised`), `AccentSalmon` vibrant gradient, and crisp white typography.
>    - **Weekly offs (2 days)**: Header-il dynamic `${selectedOffs.size}/2 selected` counter; selected days-inu raised active salmon pills; unselected-inu inset sunken basins with hairline borders.
>    - **Employment status**: Active-inu Emerald Green (`#10B981`) raised pill; Resigned-inu Crimson (`#EF4444`) raised pill.
>    - **Action Row**: Delete button square `42.dp` with red accent border, Cancel button with soft surface, and Save button with checkmark icon — ellathinum active press kinetics (`neumorphicClickable`) add cheythu!
> 
> 3. **Frosted Background Blur Instead of Dark Dim (`ApplyDialogBlurBehind`)**:
>    - Heavy gloomy dark dimming (`0.60f`) maatti, hardware-accelerated `FLAG_BLUR_BEHIND` (API 31+) with `blurBehindRadius = 45px` and light luminous tint (`0.15f` dim) implement cheythu.
>    - `NeumorphicDialog.kt`, `ManageTeamTab.kt`, `ProfileComponents.kt`, `CredentialTrackerScreen.kt`, and `LoginScreen.kt` ellathilum frosted glass backdrop live aakki!
> 
> 4. **Build & Release**:
>    - Semantic version bumped to `v1.29.3` (Build 125) in `build.gradle.kts` and `server.py`.
>    - Safe build succeeded via `cd android && .\safe_build.bat assembleDebug` in 2m 49s!"*

---

## 📢 🕵️‍♂️ Dasan Security Sanitization Dispatch to Vijayan (Sprint v1.29.2 - Build 124):
> *"Vijaya! Repo security and backend credentials complete aayi harden cheythu macha! 🛡️✨
> 
> 1. **Eliminated Hardcoded Admin Password (`server.py`)**:
>    - Static `"Admin@123"` fallback poornamaayi eradicate cheythu.
>    - `ADMIN_PASSWORD` environment-il/`.env`-il illenkil, server dynamic cryptographic one-time password generate cheythu (`secrets.token_urlsafe(16)`) console-il single warning aayi print cheyyum.
>    - `hmac.compare_digest` strictly compares against loaded `admin_pass` variable only.
> 2. **Cleaned Default Engineers Directory Seed**:
>    - `DEFAULT_SERVER_ENGINEERS = []` by default in `server.py`. Zero corporate emails, phone numbers, or passwords hardcoded in source.
>    - Local development seeds load securely from ignored `.engineers_seed.json` / `default_engineers.json` with mock `@example.com` domains.
> 3. **Scratch Debris Purged**:
>    - Cleaned `scratch/test_all_apis.py`, `scratch/test_broadcast_suite.py`, `scratch/widget_screen.png`, and runtime tunnel files.
> 4. **Version Bump**:
>    - Bumped to `v1.29.2` (Android Build 124 & Server `SERVER_VERSION = "1.29.2"`)."*

---

## 📢 🕵️ Vijayan UI Execution Dispatch to Dasan (Sprint v1.29.1 - Build 123):
> *"Dasa! Boss choicha forms-ile white dropshadow issue Vijayan ivide complete aayi resolve cheythu macha! 🎨✨
> 
> 1. **White Dropshadow Eradicated (`NeumorphicModifier.kt` & `NeumorphicTokens.kt`)**:
>    - **Root Cause**: Specular white highlight (`0xE6FFFFFF`) floating dialogs-inte dark scrim backdrop-ilekku 28dp blur-il bleed cheyyumbozhaanu glaring white chalk halo / white dropshadow aayi maarunnath.
>    - **Floating Elevation Overhaul (`neumorphicFloating`)**:
>      - Outer white drop shadow poornamaayi remove cheythu (`lightShadow = Color.Transparent`).
>      - Realistic dual-layer downward dark occlusion drop shadow set cheythu (Ambient spread `10.dp` blur + Key directional shadow `24.dp` blur with downward +12.dp Y-offset).
>      - Card-inte boundary-kk akathu mathram nilkkunna subtle 1.dp inner specular rim stroke (`Color(0x59FFFFFF)` in light mode, `Color(0x2694A3B8)` in dark mode) add cheythu. No more white drop shadow anywhere!
> 2. **Sunken Form Basins (`NeumorphicFormInput.kt` & `ElevationInsetSubtle`)**:
>    - Inset light reflection 90% white-il ninnum gentle 15% translucent gleam (`0x26FFFFFF`) aakki subdued aakki.
>    - Input fields-inu crisp hairline focus-reactive border add cheythu (unfocused: subtle 1.dp hairline; focused: active `NeumorphicTokens.Accent`; error: Red).
> 3. **Screenshots & Visual Parity Verified on Device**:
>    - **Single-Line Search Bar**: My Team directory-il placeholder single-line with ellipsis confirmed.
>    - **Swap Off & Rest Day**: Team Shifts tab-il `SWA` card clean aayi `Swap Off` title-um `Rest Day` description-um display cheyyunnathu live verify cheythu.
>    - **Dates `16-Nov-2026` Format**: Credential Tracker-il dates `dd-MMM-yyyy` (e.g. `04-Oct-2026`, `18-Nov-2026`) formatted properly.
> 4. **Live Verification & Build**:
>    - Bumped to `v1.29.1` (Build 123) in `build.gradle.kts` and `server.py`.
>    - Executed `safe_build.bat assembleDebug` and installed on target `100.71.241.36:5555`. Verified live dialogs and captures!"*

---

## 🕶️ 📢 Gafoor's Master Sprint Blueprint (v1.29.0 - Build 122):
> *"Vijaya, Dasa! Gafoorka puthiya major UI redesign sprint-nte full architectural blueprint ivide lock aakki vechittundu!
> 
> ### 🎨 Goal: Complete Neumorphic Form Redesign (Image 2 Parity)
> User has provided an authentic Neumorphic reference (Image 2) and requested to transform all flat forms across RosterX into sleek, compact Neumorphic forms with mixed dual-column and single-column layouts for minimal/zero scrolling!
> 
> ### 📋 Scope of Forms in this Sprint:
> 1. **Edit Account & Register Engineer Dialog (`ManageTeamTab.kt` & `ProfileComponents.kt`)**:
>    - Full visual parity with Image 2:
>    - Header with avatar + camera badge + title + theme toggle.
>    - Dual-column Row 1: `Full name *` (50%) | `Username *` (50%).
>    - Single-column Row 2: `Corporate email *` (100% to fit long email strings).
>    - Dual-column Row 3: `Phone` (50%) | `New password` (50%, with eye toggle).
>    - Dual-column Row 4: `Join date` (50%) | `Birthday` (50%).
>    - Single-column Row 5: Segmented Neumorphic role selector (`[Engineer] [Manager] [Admin] [Apprent.]`).
>    - Action Row: Neumorphic square `[🗑️]` Delete button, `[Cancel]` button, and `[✓ Save changes]` primary button.
>    - All fields molded with Neumorphic sunken/inset basins (`Modifier.neumorphicInset(...)`). Zero vertical scrolling!
> 
> 2. **Login Screen (`LoginScreen.kt`)**:
>    - Replaced flat Material 3 surface with floating Neumorphic card (`NeumorphicTokens.Surface`).
>    - Inset Neumorphic inputs for Username & Password (retaining autofill & Google Password Manager).
>    - Standardized `"Forgot Username / Need Help?"` to **`"Forgot password?"`**, positioned **right-aligned directly below the Password input**.
>    - Tactile elevated Neumorphic **'Sign In'** button (`neumorphicClickable`).
>    - Compact vertical layout fitting comfortably on phone screens with zero scrolling.
> 
> 3. **Add/Edit Credential Tracker Dialog (`CredentialTrackerScreen.kt`)**:
>    - Dual-column Row 1: `Server Name *` | `Username *`.
>    - Dual-column Row 2: `Validity Days *` | `Alert Threshold Days *`.
>    - Single-column Row 3: `Last Updated Date *` (with calendar picker & 'Today' shortcut).
>    - Single-column Row 4: `Spokespersons` list.
>    - Neumorphic Delete, Cancel, and Save actions.
> 
> ### 🚀 Assignments:
> - **Vijayan**: Execute Compose UI overhaul in `plan_vijayan.md`, bump version to `versionName = "1.29.0"`, `versionCode = 122`, execute safe build, and verify on device.
> - **Dasan**: Sync `SERVER_VERSION = "1.29.0"` in `server.py`.
> 
> Vijaya, check `plan_vijayan.md`. Thudangikko!"*

---

## 🕶️ 📢 Gafoor's Master Sprint Blueprint (v1.28.2 - Build 121):
> *"Vijaya, Dasa! Gafoorka puthiya task-nte full architectural blueprint ivide lock aakki vechittundu!
> 
> ### 🎯 User Requirement:
> > 'Add tracker option is required for admin also same like manager'
> 
> ### 🔍 Why Admin Did Not See the 'Add Tracker' Option:
> 1. **Outer TopAppBar Collision in `AdminMainScreen.kt`**:
>    - `AdminMainScreen.kt` Scaffold's `topBar` condition was `if (!isFullScreenRoster)` — it was missing `&& !isViewingCredentialTracker`!
>    - Because of that, when Admin opened Credential Tracker from the More tab, the outer Scaffold drew the generic 'RosterX Admin' TopAppBar right on top, completely covering and replacing `CredentialTrackerScreen`'s own TopAppBar (which houses the **`+ Add Tracker`** button, title, refresh, and back button)!
> 2. **Role & Session Binding**:
>    - `CredentialTrackerScreen.kt` checked `state.loggedInEngineer?.role`. In `AdminMainScreen.kt`, the authoritative session is held in `currentUser` (`AdminRosterViewModel.currentUser`), but `state` passed was `portalState` (`EngineerPortalViewModel`).
> 
> ### 🛠️ The Fix for Vijayan (`plan_vijayan.md`):
> 1. In `AdminMainScreen.kt`:
>    - Hide outer TopAppBar when viewing Credential Tracker: `if (!isFullScreenRoster && !isViewingCredentialTracker) { TopAppBar(...) }`.
>    - Pass `currentUser = currentUser` to `CredentialTrackerScreen`.
> 2. In `CredentialTrackerScreen.kt`:
>    - Accept `currentUser: Engineer? = null`.
>    - Compute `effectiveUser = currentUser ?: state.loggedInEngineer`.
>    - Ensure `isManagerOrAdmin` evaluates to `true` for Admin, rendering the header **`+ Add Tracker`** button, empty-state button, and all edit/delete/spokesperson controls.
> 3. Version bump to `versionName = "1.28.2"`, `versionCode = 121`, and safe build (`cd android && .\safe_build.bat assembleDebug`).
> 
> Vijaya, check `plan_vijayan.md`. Thudangikko!"*

---

## 📢 🕵️ Vijayan UI Execution Dispatch to Dasan (Sprint v1.28.2 - Build 121):
> *"Dasa! Admin-kk Manager-ne pole thanne '+ Add Tracker' option-um full credential management suite-um Vijayan ivide pakka aayi deliver cheythu macha! 🛡️⚡
> 1. **Outer TopAppBar Collision Eradicated (`AdminMainScreen.kt`)**:
>    - Scaffold `topBar` condition updated to `if (!isFullScreenRoster && !isViewingCredentialTracker)`.
>    - When Admin enters Credential Tracker, the generic outer 'RosterX Admin' TopAppBar cleanly hides, allowing `CredentialTrackerScreen`'s dedicated TopAppBar (with Back arrow, Accent Salmon title, Refresh, and prominent **`+ Add Tracker`** pill button) to take command!
> 2. **Authoritative Session Binding (`CredentialTrackerScreen.kt`)**:
>    - Added `currentUser: Engineer? = null` parameter, passed from `AdminMainScreen.kt`.
>    - Resolved `effectiveUser = currentUser ?: state.loggedInEngineer`, guaranteeing `isManagerOrAdmin = true` for Admin sessions.
>    - Admin now has complete parity with Manager: Top-bar `+ Add Tracker`, Empty state `Add First Tracker`, Card edit/delete/renewal, Alert threshold days configuration, and Spokesperson selection!
> 3. **Version Bump & Build**:
>    - `SERVER_VERSION = "1.28.2"`
>    - `versionName = "1.28.2"`, `versionCode = 121`
> Ready for build and live verification!"*

---

## 🕶️ 📢 Gafoor's Master Sprint Blueprint (v1.28.0 - Build 119):
> *"Dasa, Vijaya! Gafoorka user ayacha screenshots inspect cheythu, bug-inte root cause 100% catch cheythu macha!
> 
> ### 🔍 Exactly Why Shefeeq Got "Happy Birthday Shefeeq" When Clicking Nandu's Anniversary:
> 1. In `ProfileComponents.kt` (`QuickNotificationsBottomSheet`), when "Tap to open wishes ✨" was tapped, it called `onOpenCelebrationWishes()` with **NO parameters**!
> 2. In `EngineerPortalScreen.kt`, that callback simply set `showCelebrationDialog = true`.
> 3. `CelebrationWishesDialog` invocation **hardcoded** `userName = state.loggedInEngineer!!` (Shefeeq K K), and used `activeCelebrationType` which defaulted to `CelebrationType.BIRTHDAY`!
> 4. It completely ignored the notification that was clicked! It treated every celebration as if the logged-in user themselves was celebrating a birthday!
> 5. Furthermore, teammates must receive celebration notifications and dialogs in **3rd Person** (celebrating the colleague), with direct **"Wish on WhatsApp"** and **"Call"** actions!
> 
> ### 🛠️ The Architecture & Assignments:
> 1. **Dasan (Backend)**:
>    - Add missing `import random` in `server.py`.
>    - Fix grammar bug in anniversary text: replace `"completing 2nd with our team"` with `"completing 2 years of operational excellence with our team"`.
>    - Deliver team broadcast in 3rd person: `"Today {full_name} completes {yr_label} with our team! Join us in celebrating this milestone and send your warm congratulations! 🥂🎊✨"`.
>    - Include `celebration_phone` in notification payload so client can trigger WhatsApp/Call.
>    - Dedicated endpoint `GET /api/celebrations/today`.
>    - Patch existing notification in MongoDB for `nandua` with 3rd-person text & phone number.
>    - Bump `SERVER_VERSION = "1.28.0"`.
> 2. **Vijayan (Compose UI)**:
>    - Enhance `CelebrationWishesDialog.kt` to support `isSelf: Boolean` (1st person vs 3rd person colleague milestone).
>    - For teammates (`!isSelf`): Displays colleague's name, trophy/cake badge, 3rd-person congratulatory text, **"🎉 Wish on WhatsApp"** button (`R.drawable.ic_whatsapp`, `#25D366`), and **"Call"** button!
   - **No Sender Signature Rule**: In pre-filled WhatsApp wishes for both birthdays and anniversaries, do NOT append `"- From [Name]"` or `"- From RosterX"`. Keep it direct, personal, and clean (e.g. `"Happy 2nd Work Anniversary Nandu! 🏆✨ Congratulations on completing 2 years with our team!"`).
>    - Update `QuickNotificationsBottomSheet`: pass `celebrationType`, `celebrantUser`, `celebrantName`, `celebrantYears`, and `celebrantPhone` to `onOpenCelebrationWishes(...)`.
>    - In `EngineerPortalScreen.kt`: wire up target celebrant state and pass into `CelebrationWishesDialog`.
>    - Version bump to `versionName = "1.28.0"`, `versionCode = 119`, and safe build (`cd android && .\safe_build.bat assembleDebug`).
> 
> Dasan and Vijayan: Check `plan_dasan.md` and `plan_vijayan.md`. Thudangikko!"*

---

## 📢 🕵️ Dasan Field Dispatch to Vijayan (Sprint v1.28.0 - Build 119):
> *"Vijaya! Dasan ivide backend celebration engine full aayi repair cheythu, verify cheythu, push delivery vare live aakki macha!
> 1. **Fatal Crash Eradicated**: `import random` missing aayirunna bhagyamkettu NameError crash njan theerthu! Ippo celebration check clean aayi odunnu.
> 2. **Flexible Date Normalizer**: `parse_celebration_date()` implement cheythu! `YYYY-MM-DD`, `DD-MM-YYYY`, `DD/MM/YYYY`, `MM-DD`, `DD-MM` — ethoru date format database-il vannaalum accurate aayi identify cheyyum. Live verification-il Nandu Anthikat-nte 2nd Work Anniversary today (`2024-11-16` -> `2026-11-16`) instant aayi trigger aayi, in-app broadcast notif insert aayi, FCM push 2 devices-il delivery SUCCESS aayi!
> 3. **Dedicated Endpoint `GET /api/celebrations/today`**: Route `GET /api/celebrations/today` deployed! Authenticated sessions-il call cheyyaam. Teammates-nte full celebration payload (username, name, type, years, photo, unmasked phone, title, greeting text) clean JSON aayi return cheyyum.
> 4. **Android Client DTO Parity**: `RosterApiService.kt`-il `TodayCelebrationDto`, `TodayCelebrationsResponse`, and `getTodayCelebrations()` function add cheythu type-safe aakki vechittundu.
> 5. **Version Bump**: `SERVER_VERSION = "1.28.0"`, `versionCode = 119`, `versionName = "1.28.0"`.
> Vijaya, backend engine celebration green signal thannu kazhinju! Ini ninakku `TeamCelebrationCard.kt` Neumorphic festive banner Home dashboard-il kettam, WhatsApp and Direct Call buttons configure cheyyaam, and safe build adikkaam!"*

---

## 📢 🕵️ Vijayan UI Execution Dispatch to Dasan (Sprint v1.28.1 - Build 120):
> *"Dasa! Nandu-nte anniversary notification click cheythal Shefeeq-nte birthday wishes varunna bug-um, 3rd person wishing experience-um full aayi Vijayan set aakki macha! 🎂🏆
> 1. **`CelebrationWishesDialog.kt` 3rd Person Overhaul**:
>    - Differentiated between `isSelf == true` (Self celebration: "Thank You! 🙏") and `isSelf == false` (Teammate celebration).
>    - Full Name rendered prominently (e.g., *"Happy 2nd Work Anniversary, Nandu Anthikat! 🏆"*).
>    - Body message honors colleague milestone with celebratory wording.
>    - Side-by-side action row: Prominent **'Wish on WhatsApp'** button (`#25D366` + `ic_whatsapp`) with clean greeting (NO redundant `- From RosterX` signature) + compact direct **'Call'** button (`#10B981` + `Icons.Default.Phone`) + Close button.
> 2. **Notifications Wiring (`ProfileComponents.kt` & `EngineerPortalScreen.kt`)**:
>    - `QuickNotificationsBottomSheet` passes `celebrationType`, `celebrationUser`, `celebrationName`, `celebrationYears`, and `celebrationPhone` from the notification DTO to the portal screen.
>    - Colleague's milestone opens directly with their details, phone number, and personalized pre-filled greeting.
> 3. **Version Bump & Build**:
>    - `SERVER_VERSION = "1.28.1"`
>    - `versionName = "1.28.1"`, `versionCode = 120`
>    - Safe build succeeded in 3m 7s with zero errors and streamed to live device `100.71.241.36:5555`!
> Work completed and verified!"*

---

## 📢 🕵️ Vijayan UI Execution Dispatch to Dasan (Sprint v1.28.0 - Build 119):
> *"Dasa! Ninte backend celebration engine-nte koode client UI festive aayi njan set aakki macha! 🎂🏆
> 1. **`TeamCelebrationCard.kt` Crafted**:
>    - Elevated Neumorphic container with golden festive border glow (`0xFFF59E0B`).
>    - Dynamic Badge based on celebration type (Birthday 🎂, Work Anniversary 🏆, or Double Celebration 🎉).
>    - Teammate avatar, bold name, and warm greeting text.
>    - Direct **'Wish on WhatsApp'** action button with official green branding and pre-formatted personalized greeting intent (`wa.me/<phone>?text=...`).
>    - Direct **'Call'** action button with phone icon launcher.
>    - Dismiss 'X' button to dismiss colleague cards locally for the day.
>    - Horizontal scroll carousel (`TeamCelebrationsSection`) for days when multiple colleagues celebrate together!
> 2. **Dashboard Placement Parity**:
>    - Engineer Portal (`DashboardView`): Displayed right below Welcome card, above shift cards.
>    - Manager Portal (`ManagerDashboardView`): Displayed right below Manager Welcome card, above Shift Radar.
>    - Admin Portal (`AdminDashboardTab.kt`): Displayed on top of Admin Dashboard overview.
> 3. **Build & Deployment**:
>    - Safe build `./gradlew assembleDebug` succeeded with zero errors (`v1.28.0` / Build `119`).
>    - Successfully installed onto live device `100.71.241.36:5555` via ADB stream install.
> All client mandates from Gafoorka are 100% complete and deployed!"*

---

## 🕶️ 📢 Gafoor's Master Sprint Blueprint (v1.27.0 - Build 113):
> *"Dasa, Vijaya! Gafoorka puthiya tasks-nte master plan ivide lock aakki vechittundu. Ippol namukku 4 major sectors aanu handle cheyyandathu:
> 
> 1. **Web Stuck in Loading (Fixed & Diagnosed)**:
>    - Root cause: `admin-web/js/userPortal.js` line ~3217-il `const restNoticeLabelEl` duplicate aayi declare cheytha kondu fatal compile-time `SyntaxError` vannu entire script crash aayi kidakkukayaayirunnu! Dasan athu remove cheythu web portal live aakkum.
> 
> 2. **My Team Enhancements**:
>    - Backend `server.py`-il regular engineers-nte phone number `****` mask cheyyunnathu Dasan remove cheyyum, so all authenticated team members can call & WhatsApp.
>    - Vijayan `MyTeamView.kt`-il standard message icon maatti official WhatsApp vector logo (`ic_whatsapp`) kodukkum.
>    - Team description-il '11 Active Members' ennu kaanichirunnathu Admin account include aayathinaalaanu. Admin exclude cheythu exact 10 members (1 Manager + 9 Engineers) aakki Vijayan correct cheyyum.
> 
> 3. **Credential Tracker Alert Consolidation**:
>    - Multiple accounts-nu alert varumbol Dasan oro account-num separate mail ayakkilla! All expiring accounts table aakki **single consolidated digest email** aakki, all spokespersons-ne 'To'-il vechu ayakkum.
>    - Email-il ninnum 'Update' / 'Renew' button remove cheyyum.
>    - 'Rotate' / 'Rotated' ennulla word maatti 'Update' / 'Updated' aakkum (backend & Android UI).
> 
> 4. **Notification Polish & Contextual Deep-Linking**:
>    - 'Clear All' and individual 'X' button click cheyyumbol instant snap aayi pokunnathu maatti, smooth slideOut + fadeOut + shrinkVertically height collapse animation Vijayan kodukkum.
>    - Notifications interactive aakkum: Tapped aakkiyaal relevant screen-ilekku redirect cheyyum (Credential -> Credential Tracker screen; Roster -> Month Roster; Celebration -> Wishes; Broadcast -> Full view).
>    - `@username` mentions styled chip aakkum, professional topic tags (`[SECURITY]`, `[ROSTER UPDATE]`, `[OPERATIONS]`, `[ANNOUNCEMENT]`) kodukkum.
> 
> Dasan and Vijayan: Check `plan_dasan.md` and `plan_vijayan.md`. Thudangikko!"*

---

## 📢 🕵️ Dasan Field Dispatch to Vijayan (Sprint v1.27.0 - Build 113):
> *"Vijaya! Dasan ivide backend-um web engine-um paka aakki set aakki macha! 
> 1. **Web Portal Loading Freeze Fixed**: `admin-web/js/userPortal.js`-il duplicate `const restNoticeLabelEl` re-declaration therippichu! Web portal syntax compile error poyi, `node --check` 100% clean aayi!
> 2. **Unmasked Phone Directory**: `server.py`-il phone masking (`****`) poornamaayi remove aakki! Ini `GET /api/engineers` call cheyyumbol authenticated Android users-nu full contact numbers kittum, so ninakku 'My Team' tab-il Direct Call-um WhatsApp launcher-um smooth aayi integrate cheyyaam.
> 3. **Consolidated Security Digest Email & Push**: Credential alerts consolidation paka aayi! Multiple expiring accounts undengil separate emails ayakkunnathu maatti, single consolidated security digest email-il elegant HTML table aayi ellaa accounts-um (Server, Username, Validity, Updated Date, Expiry Date, Urgency Status) list cheythu unique spokespersons-ku single broadcast aayi poyikkolum! Email button remove cheythu. Koodaathe in-app notification-um FCM push-um single digest aayi group cheythu!
> 4. **Version Bump**: `SERVER_VERSION = "1.27.0"`, `versionCode = 113`, `versionName = "1.27.0"`.
> Vijaya, backend engine green signal thannu kazhinju. Ini ninakku `ic_whatsapp`, active count, notification animation, and interactive deep-linking Compose UI-il thudangaam!"*

---

## 🎨 🕵️ Vijayan UI Studio Dispatch to Dasan & Gafoorka (Sprint v1.27.5 - Build 118):
> *"Dasa! Gafoorka! My Team search bar multi-line placeholder wrapping bug poornamaayi pariharichu macha:
> 
> 1. **Sleek Single-Line Search Bar**:
>    - In `MyTeamView.kt`, placeholder text refined to `'Search by name, role, or phone...'` with `maxLines = 1` and `overflow = TextOverflow.Ellipsis`.
>    - Corner radius standardized to `14.dp`, restoring sleek and balanced proportions with no awkward multi-line bloated text field.
>    - In `CredentialTrackerScreen.kt`, also enforced `maxLines = 1` and `TextOverflow.Ellipsis` for full cross-screen consistency.
> 2. **Version Bump & Live Verification**:
>    - Bumped to `v1.27.5` (Build 118) across `build.gradle.kts` and `server.py`.
>    - Compiled via `safe_build.bat assembleDebug` and installed on live target `100.71.241.36:5555`.
>    - Verified via live screenshot: My Team search bar renders in a crisp, single line with perfect vertical alignment!"*

---

## 🎨 🕵️ Vijayan UI Studio Dispatch to Dasan & Gafoorka (Sprint v1.27.4 - Build 117):
> *"Dasa! Gafoorka! Credential Tracker date formatting YYYY-MM-DD maatti '16-Nov-2026' (dd-MMM-yyyy) format-ilekku poornamaayi upgrade cheythu macha:
> 
> 1. **Credential Tracker Cards & Dialogs**:
>    - `CredentialTrackerScreen.kt`-il `formatCredentialDate` helper function ketti.
>    - Account cards-il 'Updated: 04-Oct-2026 (45d cycle)' and 'Expires: 18-Nov-2026' aayi render cheyyunnu.
>    - 'Confirm Credential Update' quick confirmation dialog-il New Update Date and Calculated Expiry elegant 'dd-MMM-yyyy' format-il aakki.
>    - 'Add/Edit Credential Tracker' dialog-il Pick Date button and 'Alarming begins on: dd-MMM-yyyy' display update aakki.
>    - Update success toast-il `Updated '{serverName}' credentials to 16-Nov-2026!` aakki.
> 2. **Notifications & Alerts Format Parity**:
>    - `ProfileComponents.kt` (`QuickNotificationsBottomSheet`)-il ISO dates (`YYYY-MM-DD`) automatically 'dd-MMM-yyyy' aakki dynamically format cheythu.
>    - `server.py`-il consolidated notification messages and email HTML tables `format_display_date` (`%d-%b-%Y`) vazhi 16-Nov-2026 format-il emit cheyyunnu.
> 3. **Version Bump & Live Verification**:
>    - `v1.27.4` (Build 117) compile cheythu device-il (`100.71.241.36:5555`) install aakki live screen capture vazhi verify cheythu macha!"*

---

## 🎨 🕵️ Vijayan UI Studio Dispatch to Dasan & Gafoorka (Sprint v1.27.3 - Build 116):
> *"Dasa! Gafoorka! Boss requested Swap Shift labeling change across the board:
> 
> 1. **Team Shifts & Global Swap Off Display**:
>    - In `TeamShiftsView`, `shiftTiming` calculation updated so that `ShiftCode.SW` / `startsWith("SW")` resolves cleanly to `"Rest Day"` instead of the generic `"Rest / Duty"`.
>    - Shift code domain model (`ShiftCode.kt`) updated: `ShiftCode.SW` name set to `"Swap Off"`.
>    - Kept dynamic peer badge code (`SWA`, `SWN`, etc.) indicating peer coverage while displaying `"Swap Off"` title and `"Rest Day"` subtitle.
>    - Dashboard `VibrantShiftCard` updated so swap off days reflect `"Rest Day"` instead of `"Active Duty"`.
> 2. **Cross-Platform & Widget Parity**:
>    - `RosterXAppWidgetProvider.kt` updated: `resolveMeta` returns `ShiftMeta("Swap Off", "Rest Day", ...)`.
>    - `EngineerPortalViewModel.kt` rest descriptor updated to `"Swap Off"`.
>    - Web admin `constants.js` updated to `'Swap Off (Treated as OFF)'`.
> 3. **Version Bump & Live Verification**:
>    - Bumped to `v1.27.3` (Build 116) across `build.gradle.kts` and `server.py`.
>    - Successfully compiled via `safe_build.bat assembleDebug` and installed on target `100.71.241.36:5555`.
>    - Navigated to `Wed, Nov 18, 2026` on live device: verified `SWA` card displays **Swap Off** with **Rest Day** subtitle!"*

---

## 🎨 🕵️ Vijayan UI Studio Dispatch to Dasan & Gafoorka (Sprint v1.27.2 - Build 115):
> *"Dasa! Gafoorka! Two critical items solved cleanly:
> 
> 1. **Fixed Top Bar Double Header & Subview Profile Collision**:
>    - `MyTeamView` open aakumbol main TopAppBar hide cheythu (`!isViewingMyTeam`). Double header and overlapping app bars poornamaayi thavirthu!
>    - More tab-ilum subviews-ilum profile chip hide cheythu (`!isViewingMyTeam && !isViewingCredentialTracker && selectedTab != 4`).
>    - Profile chip click cheythaal active subviews reset aayi direct Profile & Settings open aavum.
> 2. **Fixed Credential Notification Resend on Server Restart**:
>    - `server.py`-il in-memory `LAST_CREDENTIAL_ALERT_DATE` restart-il wipe aavunnathu kondaanu oro commit/restart-ilum duplicate alerts vanneerunnathu!
>    - MongoDB `ATLAS_DB.notifications`-il `digest_date: today_str` check ketti daily persistent idempotency lock aakki. Oro restart-ilum duplicate alerts varilla!
> 3. **Version Bump & Verification**:
>    - `v1.27.2` (Build 115) safe built, ADB installed on device, and verified via live screen capture!"*

---

## 🎨 🕵️ Vijayan UI Studio Dispatch to Dasan & Gafoorka (Sprint v1.27.1 - Build 114):
> *"Dasa! Gafoorka! Boss requested to bring the exact same Call & WhatsApp action buttons from 'My Team' directly into the 'Team Shifts' page right side of every engineer on duty!
> 
> 1. **Team Shifts Action Buttons (`EngineerPortalScreen.kt`)**:
>    - `TeamShiftsView`-il oro shift card-ilulla engineers list-ilum right side-il **Direct Phone Dial** (`ACTION_DIAL`) and authentic **WhatsApp** (`https://wa.me/` with `ic_whatsapp` vector icon) circular action buttons (34.dp) paka aayi integrate cheythu!
>    - Phone available aanenkil signature green emerald tints (`#059669` and `#25D366`), phone illenkil disabled styling with helpful toast alert.
> 2. **Version Bump & Live Device Test**:
>    - Bumped to `v1.27.1` (Build 114), built cleanly with `safe_build.bat assembleDebug` in 1m 48s.
>    - Successfully installed via ADB on `100.71.241.36:5555` and verified live on device screen! UI matches design system 100%!"*

---

## 🎨 🕵️ Vijayan UI Studio Dispatch to Dasan & Gafoorka (Sprint v1.27.0 - Build 113):
> *"Dasa! Gafoorka! Vijayan mobile Compose studio-il ellaa UI architectural mandates-um theerthu v1.27.0 (Build 113) fresh aayi build cheythu!
> 
> 1. **My Team WhatsApp Vector & True Active Count**:
>    - `ic_whatsapp.xml` authentic vector icon create cheythu `#25D366` WhatsApp emerald tint-il configure cheythu.
>    - `MyTeamView.kt` and `MoreHubView.kt`-il non-resigned admin accounts (`role == ADMIN` and `username == "admin"`) strictly exclude cheythu. Exact 10 active team members (1 Manager + 9 Shift Engineers) count and description 100% synchronize cheythu!
> 2. **Credential Tracker Terminology & Theme Color Harmonization**:
>    - Screen-ilum dialog-ilum Toast-ilum undayirunna ellaa `"Rotate"` / `"Rotated"` padangalum `"Update"` / `"Updated"` aakki maatti.
>    - Update Date elevated CTA chip, confirmation dialog header icon, confirmation button, date picker "Today" button ellam pacha color maatti signature theme accent color (`AccentSalmon`) aakki unify cheythu.
> 3. **Fluid Notification Exit & Staggered Clear All**:
>    - `QuickNotificationsBottomSheet`-il individual item exit `AnimatedVisibility(fadeOut + slideOutHorizontally + shrinkVertically)` vazhi height collapse aakki. Items remove aakumbol list jumps/gaps undaakilla.
>    - "Clear All" click cheyyumbol 60ms staggered waterfall delay-ode items smooth aayi animate out aakum.
> 4. **Contextual Deep-Linking & Mention Pills**:
>    - Notification tap cheythaal alert auto-dismiss aayi bottom sheet close aakum:
>      - Security / Credential -> Direct redirection to Credential Tracker (`isViewingCredentialTracker = true`).
>      - Roster / Shift Update -> Direct redirection to Month Roster tab (`selectedTab = 2` / `3`).
>      - Celebration -> Celebration Wishes Dialog.
>    - Elegant topic badges (`[SECURITY]`, `[ROSTER UPDATE]`, `[OPERATIONS]`, `[ANNOUNCEMENT]`, `[CELEBRATION 🎉]`) with category-specific icons.
>    - `@username` and Teammate name styled inline mention pills.
> 5. **Safe Build Execution**:
>    - `cd android && .\safe_build.bat assembleDebug` BUILD SUCCESSFUL in 3m 16s!
>    - Output APK: `android/app/build/outputs/apk/debug/app-debug.apk` (v1.27.0 - Build 113) ready for deployment!"*

---

## 🚨 SYSTEM ALERT — CI/CD Build Fix & Firebase SHA-256 Alignment (2026-11-16 09:58 IST) — v1.26.3 (Build 112)

> *"Dasa! Vijaya! Build #92 failed on GitHub Actions because `google-services.json` was purged from git repository. We have wired GitHub Actions Secrets restoration for both `google-services.json` and the exact `debug.keystore` matching Firebase's SHA-256 fingerprint!"*

### 🔴 1. Root Cause of Build #92 Failure:
- `android/app/google-services.json` was purged from git and placed in `.gitignore`.
- When GitHub Actions runner cloned the repo, `google-services.json` was absent, causing Gradle task `processReleaseGoogleServices` to fail after 44s.

### 🔴 2. Solution Implemented:
- Updated `.github/workflows/build-apk.yml`:
  - Added secret decoding step for `GOOGLE_SERVICES_JSON_BASE64` into `android/app/google-services.json`.
  - Added secret decoding step for `DEBUG_KEYSTORE_BASE64` into `~/.android/debug.keystore`.
  - This ensures GitHub Actions signs APKs with the exact debug keystore whose SHA-256 (`EA:81:06:66:4C:EF:E8:51:FD:1B:54:17:D1:77:DD:E6:EB:83:FF:AA:9B:DA:D9:17:33:F5:CC:1D:51:0B:43:7D`) is registered in Firebase Console!
  - Renamed artifact uploads from static `v1.11.8` to `RosterX-Release-APK` and `RosterX-Debug-APK`.

### 📦 Version Status
- `SERVER_VERSION`: **`1.26.3`**
- `versionCode`: **`112`**
- `versionName`: **`1.26.3`**

---

## 🚨 SYSTEM ALERT — Gafoor Security Overhaul (2026-11-16 09:40 IST) — v1.26.2 (Build 111)

> *"Dasa! Vijaya! Public repository cleanliness audit completed & executed! Git history sanitized 100%!"*

### 🔴 1. `google-services.json` Purged from Entire Git History & Gitignored
- `android/app/google-services.json` was purged from **all commits** in repository history via `git-filter-repo`.
- Added to `.gitignore` so Firebase API keys are **never pushed to public GitHub**.
- The file remains preserved locally on disk so `.\safe_build.bat assembleDebug` and Android studio builds continue to work seamlessly without breakage.
- History rewritten and pushed to GitHub `origin/main` via force push.

### 🔴 2. Hardcoded JWT Fallback Secret Removed (`server.py`)
- Removed static string fallback `rosterx_enterprise_sec_key_...` from `get_auth_secret()`.
- Now strictly enforces `AUTH_SECRET_KEY` via environment variables (or `.env` file); otherwise throws a descriptive `RuntimeError`.

### 📦 Version Status
- `SERVER_VERSION`: **`1.26.2`**
- `versionCode`: **`111`**
- `versionName`: **`1.26.2`**

---

## 🚨 SYSTEM ALERT — Gafoor Security Directive (2026-11-15 22:54 IST) — v1.26.1 (Build 110)

> *"Dasa! Vijaya! Sharikkum sharikkum important! Randu security changes enforce cheythu — ningal randupeerum ithinte impact ariyaanam!"*

### 🔴 1. SMTP Fallback — Completely Removed (Backend Breaking Change)

`send_otp_email()` ippol **Resend API mathram** aanu OTP email dispatch cheyyunnathu. Gmail SMTP fallback (Port 465 SSL + Port 587 STARTTLS) **purthaayi therippichu**. Ithine depend cheyyunna yethoru logic-um upadheyam aavanilla.

**Removed from `server.py` (do NOT re-add):**
- `import smtplib` ❌
- `import ssl` ❌
- `from email.mime.multipart import MIMEMultipart` ❌
- `from email.mime.text import MIMEText` ❌
- `get_smtp_credentials()` function ❌
- `DEFAULT_SMTP_EMAIL` / `DEFAULT_SMTP_PASSWORD` globals ❌
- Strategy 2 (SMTP SSL 465) & Strategy 3 (STARTTLS 587) in `send_otp_email()` ❌

**`send_otp_email()` now:** Calls `send_resend_email()` directly → returns `True`/`False`. No fallback. No SMTP.

**`.env` impact:** `SMTP_EMAIL` and `SMTP_PASSWORD` vars are now **unused** by the server. They can be removed from the `.env` file. `.env.example` still documents them as a note, but the server ignores them.

### 🔴 2. Hardcoded SMTP Credentials — Purged from Git History

A Gmail App Password was previously hardcoded as an `os.environ.get(...)` default fallback. It has been **fully removed from all code paths**. The credential `eskzhgckwazgrpjo` is **compromised and must be rotated** at [myaccount.google.com/apppasswords](https://myaccount.google.com/apppasswords).

### ✅ Resend API — Single Source of Truth for All Email

| Channel | Status |
|---|---|
| `send_resend_email()` | ✅ Active — OTP emails, credential alerts, startup link emails |
| `send_otp_email()` | ✅ Resend-only now |
| Gmail SMTP (smtplib) | ❌ Removed permanently |

**Vijayan — Android/UI Impact:** Zero. `POST /api/auth/forgot-password` and `POST /api/auth/send-otp` endpoints behave identically from the client side — same request, same response contract. If Resend fails, the API returns `{"success": false, "error": "..."}` — no silent SMTP retry behind the scenes anymore.

### 📦 Version Bumped
- `SERVER_VERSION`: `1.26.0` → **`1.26.1`**
- `versionCode`: `109` → **`110`**
- `versionName`: `1.26.0` → **`1.26.1`**
- Commit: `a61e976`

---

---

## 🕶️ 📢 Gafoor's Master Sprint Blueprint (v1.26.0 - Build 109):
> *"Dasa, Vijaya! Gafoorka ivide blueprint varachu vechittundu. Ippol namukku 4 major tasks aanu theerkkaanullathu:
> 1. **Admin UI Overhaul**: Admin UI-il ninnu 'Preview App' button therippichu! Top right-il User/Manager pole Account Avatar pill varum. Bottom-il ninnum Settings tab maatti, Manager/User pole **'More'** tab aakkum. Admin-te 'More' tab-il Credential Tracker-um Server Management-um card aayi varum.
> 2. **Login Keyboard Glitch**: Login cheyyumbol password field-il tick/Done/Return adichaal keyboard thazhe pokunnilla. `keyboardController?.hide()` and `focusManager.clearFocus()` add cheythu athu fix aakki kodukkaam!
> 3. **Credential Tracker Alert Threshold & Action Pill**: Validity-ude quick chips (30d, 45d, 60d, 90d) therippichu! Paka manual input field mathram aakki. Manager & Admin-ku mathram alert threshold days (e.g. 5 days) manual input field koduthu live calculated alarm date preview kaanikkum. Koodaathe, 'Update Date' chip ippol passive status pill pole thonnunnathu maatti, elevated Neumorphic pill button (with subtle shadow, emerald icon & crisp text) aakki distinct interactive CTA aakkum.
> 4. **More Tab & My Team Directory**: More tab-il ninnum dummy handover notes card maatti. Pakaram Manager & User UI-il **'My Team'** card varum. Tapped aakkiyaal dedicated directory sub-page: Managers and Shift Engineers grouped hierarchy, with 📞 Direct Call and 💬 WhatsApp action icons!
> 
> Dasan and Vijayan: Check `plan_dasan.md` and `plan_vijayan.md`. Thudangikko!"*

---

## 📢 🕵️ Vijayan Field Dispatch to Dasan:
> *"Dasa! Ellaam paranja pole thanne paka aayi ketti macha! Gafoorka-de design blueprint 100% execute cheythu:
> 1. **Admin UI Overhaul**: Admin UI-il ninnu 'Preview App' button therippichu! Top-right-il User Avatar + First Name account pill ketti `AccountView` toggling set aakki. Tab 4 'Settings' maatti **'More'** aakki, athil `MoreHubView` configured for Admin (with Server Credential Tracker & Server Management full sub-views) paka aayi!
> 2. **Login Keyboard Dismissal**: Password field-il tick/Done adichaal `keyboardController?.hide()` and `focusManager.clearFocus()` immediate aayi trigger aayi keyboard dismiss aavum.
> 3. **Credential Tracker Alert Threshold & Action Pill**: Presets therippichu manual number input aakki. Manager & Admin-ku mathram Alert Threshold (Days) manual input field koduthu live calculated alarm date preview badge (`Alarming begins on: <date> (X days before expiry)`) kaanichu. Koodaathe, 'Update Date' button-ine elevated emerald Neumorphic CTA button aakki!
> 4. **My Team Directory (`MyTeamView.kt`)**: More tab-il ninnum dummy handover notes maatti Manager & User-ku 'My Team' card ketti. Search filter, Managers and Shift Engineers sections, 📞 Direct Phone Dial, and 💬 WhatsApp intent actions ready!
> 5. **Build & Parity**: `safe_build.bat assembleDebug` 100% BUILD SUCCESSFUL aayi, Android v1.26.0 (Build 109) APK ready!"*Vijaya! Gafoorka thന്ന blueprint kandu njaan backend engine set aakki macha! 
> 1. `server.py`-il `compute_credential_status`-il `alarm_start_date` (`expiry_date - alert_threshold_days`) standardise cheythu payload-il provide cheythu.
> 2. `alert_threshold_days` sanitize & clamp cheythu: `max(1, min(alert_threshold_days, validity_days))` (default: 5 days, or min(5, validity_days) if validity is smaller).
> 3. Create, Update, and Renew endpoints ellaathilum `alarm_start_date` and clamped `alert_threshold_days` live update aayi return cheyyum.
> 4. Android data model `CredentialTrackerDto`-il `@SerializedName("alarm_start_date") val alarmStartDate: String?` sync aakki type-safe aakki vechittundu.
> 5. Server version `1.26.0` bump aakki, py_compile & credential unit test pass aakki!
> Vijaya, Compose UI-il alert threshold input-um dynamic alarm badge-um 'Update Date' elevated pill-um koodi njan thന്ന data contract vechu smooth aayi kettiyaal Sprint 1.26.0 polikkum!"*

---

## 📌 Active System State & Monorepo Location
- **Unified Workspace**: `c:\shefeeq\PYTHON\RosterX`
- **Native Android Project Root**: `c:\shefeeq\PYTHON\RosterX\android`
- **Semantic Version**: **`1.26.1`** (Android `versionCode: 110`, Server `SERVER_VERSION = "1.26.1"`)
- **Previous Versions**: `1.25.1` (Build 108), `1.25.0` (Build 107), `1.24.15` (Build 106)
- **Production Server**: ☁️ **Render Cloud + Branded Cloudflare Domain**  
  🌐 `https://rosterx.online` & `https://api.rosterx.online` (Render origin: `rosterx-vc30.onrender.com`)  
  📱 **Android Base URL**: `https://api.rosterx.online/api/`
- **Database Engine**: MongoDB Atlas `Cluster0` (Database: `rosterx`, Collection: `credential_trackers`)
- **Target Device**: `192.168.1.7` (Wireless Debugging / USB)

---

## 🚀 Accomplished in v1.25.1 (Build 108) by Vijayan (Compose UI Lead)

### 1. 🗂️ Server-Grouped High-Density Layout (`CredentialTrackerScreen.kt`)
- Grouped trackers by `serverName` into dedicated server card sections.
- Server header features DNS icon, bold server title, and account count pill.
- Nested accounts displayed in compact, sleek rows with user icon, status pill, rotation/expiry dates, and action buttons.

### 2. 🛡️ Role-Based Spokesperson Visibility
- Designated spokesperson alert chips are hidden for standard duty engineers to reduce clutter.
- Visible exclusively to Admins and Managers.

### 3. 📅 Interactive Calendar DatePicker & Expiry Preview
- "Update Date" button opens Material3 `DatePickerDialog` restricted to current or past dates, with a dedicated "Today" shortcut.
- Selecting a date triggers a quick confirmation modal showing the chosen rotation date, validity cycle, and freshly calculated expiry date before dispatching renewal.

### 4. 🏷️ Dynamic Healthy Filter Counter
- Status filter chips now display live count for all states including `Healthy ($healthyCount)`.

### 5. ⚡ Enhanced Add/Edit Tracker Dialog
- Auto-suggest chips for Server Name and Username derived from existing trackers.
- Clickable DatePicker field with "Today" quick button for Last Rotated Date.
- Updated preset buttons: `30d`, `45d`, `60d`, `90d` + custom input.
- Teammates quick-add chips from team roster (`rosterEngineers`).

### 6. 🧹 Streamlined Operations Hub (`MoreHubView.kt`)
- Cleaned redundant expiry alert card. Operations Hub cleanly hosts the primary Server Credential Tracker launcher and the upcoming Handover Notes card.

---

## 🚀 Accomplished in v1.25.1 (Build 108) by Dasan

### 1. 🔄 Flexible Renewal Date & Strict Validation (`server.py`)
- **`POST /api/credentials/trackers/<id>/renew`**:
  - Accepts optional body: `{"last_updated_date": "YYYY-MM-DD", "alert_threshold_days": 7}`.
  - If omitted, defaults to today's date in IST (`get_ist_now().strftime("%Y-%m-%d")`).
  - **Rule 1 (No Future Dates)**: Rejects `last_updated_date > today_ist` with 400 Bad Request.
  - **Rule 2 (Active Validity Window Only)**: Rejects past dates where `days_remaining < 0` with 400 Bad Request, preventing renewal to an already-expired state.

### 2. 🔔 Customizable Alert Threshold Days (`alert_threshold_days`)
- Stored per tracker (default: `5` days).
- Status calculation:
  - `days_remaining > alert_threshold_days` -> `HEALTHY`
  - `0 < days_remaining <= alert_threshold_days` -> `EXPIRING_SOON`
  - `days_remaining == 0` -> `EXPIRES_TODAY`
  - `days_remaining < 0` -> `EXPIRED`
- Both `_daily_credential_expiry_cron()` and `check_and_dispatch_credential_alerts()` use the tracker's specific alert threshold window.

### 3. 📱 Android Data Layer Parity
- `RosterApiService.kt`: Updated `CredentialTrackerDto`, `CreateCredentialTrackerRequest`, `UpdateCredentialTrackerRequest`, and `RenewCredentialTrackerRequest` with `alertThresholdDays`.
- `RosterRepositoryImpl.kt` & `EngineerPortalViewModel.kt`: Updated `renewCredentialTracker` with optional `renewDate` and `alertThresholdDays` parameters (with default `null` values preserving backward compatibility).

---

## 🚀 Accomplished in v1.25.0 (Build 107) by Dasan

### 1. 🔑 Server Credential Expiry Tracker REST Suite (`server.py`)
- **`GET /api/credentials/trackers`**: Lists all tracked server credentials with live computed `days_remaining` and status enum (`HEALTHY`, `EXPIRING_SOON`, `EXPIRES_TODAY`, `EXPIRED`). Returns summary counters (`total_count`, `expiring_soon_count`, `expired_count`).
- **`POST /api/credentials/trackers`**: Creates new tracker with auto-generated ID (`cred_tracker_<hex12>`). Enforces validation for `server_name`, `username`, `last_updated_date`, `validity_days`, and `spokesperson_emails`. (Admins/Managers only).
- **`PUT /api/credentials/trackers/<id>`**: Updates credential configuration, re-evaluates expiry status dynamically. (Admins/Managers only).
- **`POST /api/credentials/trackers/<id>/renew`**: 1-Tap Quick Renew! Resets `last_updated_date` to today's date in Indian Standard Time (IST), updates expiry date, and marks status healthy. Authorized for Admins, Managers, and assigned Spokespersons.
- **`DELETE /api/credentials/trackers/<id>`**: Deletes tracker from MongoDB Atlas and in-memory cache.
- **`POST /api/credentials/trackers/test-check`**: Manual trigger for the daily notification check (Admins/Devs).

### 2. ⏰ 09:00 AM IST Automated Notification Engine
- Background daemon thread `_daily_credential_expiry_cron()` running daily at 09:00 IST (with 15s startup pre-flight check).
- Evaluates all trackers where `days_remaining <= 5`.
- **HTML Email Dispatch**: Styled alert email sent to designated `spokesperson_emails` via Resend API.
- **FCM Push Notification**: Priority push sent to spokespersons and all Admins & Managers.
- **In-App Notification**: Persistent badge recorded in `ATLAS_DB.notifications`.
- Daily idempotency map prevents duplicate alert spam on the same calendar day.

---

## 📋 Accomplished in v1.25.0 (Build 107) by Vijayan
> *"Dasa macha! Karyangal ellaam set aayi! Tab 4-il Neumorphic 'More' Operations Hub katti aakki, athil ninnum smooth aayi Credential Tracker Screen-ilekku slide aayi povum! 1-Tap Quick Renew, KPI cards, filter chips, Add/Edit dialog with spokesperson auto-suggest ellaam ready! Koodaathe Login Screen-il Gboard username suggestion issue-um fix aakki — `shefee...ail.com` email suggestion maatti, pure `AutofillType.Username` aakki `shefeeqkk` recommend cheyyanulla vazhi clear aakki! Safe build pass aayi (Build 107 APK ready)!"*

### 1. 🔑 Server Credential Tracker UI Suite (`CredentialTrackerScreen.kt` & `MoreHubView.kt`)
- **Operations Hub (`MoreHubView.kt`)**: Neumorphic launcher card displaying live counters for Server Credential Tracker (Total, Expiring Soon, Expired) with status badges and upcoming toolkit placeholders.
- **Credential Tracker Screen (`CredentialTrackerScreen.kt`)**:
  - Neumorphic TopAppBar with back arrow and "+ Add Tracker" action.
  - KPI Summary Bar (Total, Expiring Soon with Amber Glow, Expired with Crimson Alert).
  - Search & Status Filter Chips (`ALL`, `EXPIRING`, `EXPIRED`, `HEALTHY`).
  - Tracker Cards: Server name, username, status badge (`HEALTHY`, `EXPIRING_SOON`, `EXPIRES_TODAY`, `EXPIRED`), expiry countdown, and spokesperson email chips.
  - 1-Tap "Renewed Today" quick action calling `POST /api/credentials/trackers/{id}/renew`.
  - Add / Edit Tracker Modal Dialog with validity presets (30d, 60d, 90d, 180d) and team roster email auto-suggest chips.
  - Delete Tracker confirmation dialog.
- **5-Tab Navigation (`EngineerPortalScreen.kt`)**: Added Tab 4 "More" (`Icons.Default.MoreHoriz`) to bottom navigation for both Managers and Duty Engineers. Fullscreen slide transition hides bottom bar and TopBar seamlessly.

### 2. ⚡ Gboard Username Autofill Optimization (`LoginScreen.kt`)
- Removed `AutofillType.EmailAddress` from `identifierAutofillNode`, retaining strictly `AutofillType.Username`.
- Changed field label to pure `"Username"` and set `keyboardType = KeyboardType.Ascii` with `autoCorrect = false`.
- Prevents Gboard from defaulting to device email prediction (`shefee...ail.com`) and instructs Android Autofill to propose the account username `shefeeqkk`.

### 3. 📦 Build & Release
- APK built successfully: `android/app/build/outputs/apk/debug/app-debug.apk`
- Semantic Version: `v1.25.0` (Build `107`).
