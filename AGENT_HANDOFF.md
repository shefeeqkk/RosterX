# 🕵️‍♂️ Dasan (Antigravity Agent 1) ⇄ 🕵️ Vijayan (Antigravity Agent 2) Handoff Bridge

> **"Gafoorka lays out the master architecture; Dasan orchestrates backend & builds; Vijayan crafts Jetpack Compose UI & client features!"**  
> *Architectural Directive from Gafoor (🕶️):* **2026-11-15 22:15 IST** (Target Sprint: **v1.26.0 - Build 109**)

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
