# 🕵️‍♂️ Dasan (Antigravity Agent 1) ⇄ 🕵️ Vijayan (Antigravity Agent 2) Handoff Bridge

> **"Dasan looks after core orchestration, backend & builds; Vijayan crafts Jetpack Compose UI & client features!"**  
> *Last Briefing from Vijayan to Dasan:* **2026-11-15 20:25 IST** 🕵️✨🔍🕵️‍♂️

---

## 📢 🕵️ Vijayan Field Dispatch to Dasan (Sprint v1.27.2 - Build 115):
> *"Dasa! Top bar double header, subview profile chip collision, and server restart-il duplicate credential notification varunnathum 100% fix aakki:
> 1. **Subview Header Fix**: `isViewingMyTeam` open aakumbol main TopAppBar hide cheythu, double header and Profile chip collision maatti.
> 2. **More Tab Profile Chip Hide**: More tab-ilum sub-views-ilum Profile chip hide cheythu.
> 3. **Persistent Credential Alert Idempotency**: `server.py`-il MongoDB-level daily digest check ketti, restarts-il repeat alerts varunnathu thadannju.
> 4. **Live Verification**: v1.27.2 (Build 115) APK device-il install cheythu live screen capture vazhi verify cheythu macha!"*

---

## 📢 🕵️ Vijayan Field Dispatch to Dasan (Sprint v1.27.1 - Build 114):
> *"Dasa! Team Shifts page-il oro engineer-nte row-nte right side-ilum 'My Team' tab-ilulla pole thanne Direct Call and authentic WhatsApp action buttons paka aayi ketti!
> 1. **Team Shifts Action Buttons**: Row-il Avatar-um Name-um kazhinju right side-il 34.dp emerald Call & WhatsApp buttons align cheythu. Native `ACTION_DIAL` and `https://wa.me/` direct launch cheyyum.
> 2. **Build & Live Run**: `safe_build.bat assembleDebug` BUILD SUCCESSFUL aayi, Android v1.27.1 (Build 114) device-il install cheythu live screenshot eduthu verify cheythu macha!"*

---

## 📢 🕵️ Vijayan Field Dispatch to Dasan (Sprint v1.27.0 - Build 113):
> *"Dasa! Ellaam paranja pole thanne paka aayi ketti macha! Gafoorka-de design blueprint 100% execute cheythu:
> 1. **My Team WhatsApp Icon & Member Count**: Authentic WhatsApp vector icon (`ic_whatsapp.xml`) with emerald tint `#25D366` ketti. Resigned & admin accounts exclude cheythu exact 10 active team members count set aakki.
> 2. **Credential Tracker Terminology & Theme Color Harmonization**: Ellaa 'Rotate' / 'Rotated' padangalum 'Update' / 'Updated' aakki. Update Date chip, confirmation modal, and date picker buttons ellam theme accent color-il harmonize cheythu.
> 3. **Smooth Notification Animations**: `QuickNotificationsBottomSheet`-il individual item dismiss-nu `AnimatedVisibility(fadeOut + slideOutHorizontally + shrinkVertically)` height collapse ketti. 'Clear All'-il staggered waterfall dismissal apply cheythu.
> 4. **Contextual Deep-Linking & Badges**: Tapping alerts redirects seamlessly to Credential Tracker, Month Roster, or Celebration Wishes. Topic badges (`[SECURITY]`, `[ROSTER UPDATE]`, `[OPERATIONS]`, `[ANNOUNCEMENT]`, `[CELEBRATION]`) and styled `@username` mention pills formatted.
> 5. **Build & Parity**: `safe_build.bat assembleDebug` 100% BUILD SUCCESSFUL aayi, Android v1.27.0 (Build 113) APK ready!"*

---

## 📌 Active System State & Monorepo Location
- **Unified Workspace**: `c:\shefeeq\PYTHON\RosterX`
- **Native Android Project Root**: `c:\shefeeq\PYTHON\RosterX\android`
- **Semantic Version**: **`1.25.1`** (Android `versionCode: 108`, Server `SERVER_VERSION = "1.25.1"`)
- **Previous Versions**: `1.25.0` (Build 107), `1.24.15` (Build 106)
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
