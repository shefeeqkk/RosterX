# 👵 Aunty (Antigravity) ⇄ 🤖 Kunjappan (Android Studio) Handoff Bridge

> **"Aunty looks after the backend & servers; Kunjappan builds the native Android app!"**  
> *Last Synced by Aunty:* **2026-10-27 14:15 IST** ☕⚡

---

## 📌 Active System State & Monorepo Location
- **Unified Workspace**: `c:\shefeeq\PYTHON\RosterX`
- **Native Android Project Root**: `c:\shefeeq\PYTHON\RosterX\android`
- **Semantic Version**: **`1.5.0`** (Android `versionCode: 16`, Server `SERVER_VERSION = "1.5.0"`)
- **Active Cloudflare Public Gateway**:  
  🌐 `https://such-anonymous-out-stopping.trycloudflare.com`  
  📱 **Android Base URL**: `https://such-anonymous-out-stopping.trycloudflare.com/api/`
- **Local Asset Sync File**: `app/src/main/assets/server_url.txt` (Synced with active Cloudflare URL)
- **Database Engine**: MongoDB Atlas `Cluster0` (Database: `rosterx`, Collection: `engineers`, `notifications`, `operational_policies`, Status: `CONNECTED`)
- **Notification Engine**: Resend API active, alerts routed to `kkshefeeq005@gmail.com`

---

## 🎨 Unified Neumorphic (Soft UI) Design System Architecture (👵⚡ v1.5.0)

Kunjappan, my brilliant boy! Look at the gorgeous new visual architecture Aunty just established across both the Web Portal and our Compose screens:

1. **Light Physics & Baseline Canvas**:
   - Monochromatic cool-slate off-white baseline: `#EEF2F6` (Color `0xFFEEF2F6`).
   - 135° Ambient Key Light: Top-Left specular highlight (`rgba(255,255,255,0.90)` / `0xE6FFFFFF`) and Bottom-Right occlusion shadow (`rgba(166,180,200,0.65)` / `0xA6A6B4C8`).
   - Primary Kinetic Energy Accent: Dynamic Coral/Salmon gradient (`#FF6B6B` $\rightarrow$ `#FF5252`) with ambient glow (`rgba(255, 82, 82, 0.35)`).
   - Strict contrast guard: Dark slate text (`#2D3748`) on the `#EEF2F6` surface canvas.

2. **Compose Neumorphic Engine (`com.rosterx.ui.neumorphic`)**:
   - `NeumorphicTokens.kt`: All light-source vectors, elevation strata (`ElevationRaised`, `ElevationInset`, `ElevationDeepWell`, `ElevationFloating`), corner radii (`CornerSmall = 14dp`, `CornerMedium = 20dp`, `CornerContainer = 28dp`), and accent gradients.
   - `NeumorphicModifier.kt`:
     - `.neumorphicRaised(shape, elevation, surfaceColor)`: Dual-opposite Canvas rendering (-X, -Y specular, +X, +Y occlusion).
     - `.neumorphicInset(shape, elevation, surfaceColor)`: Sunken well inner shadow simulation with hardware-accelerated clipping.
     - `.neumorphicFloating(shape, surfaceColor)`: Diffuse elevation for modals/dialogs.
     - `.neumorphicClickable(...)`: Tactile press physics with spring transition between Raised and Inset.
     - `.neumorphicAccentGlow(...)`: Ambient kinetic glow.

3. **Soft UI Reusable Components (`com.rosterx.presentation.components`)**:
   - `NeumorphicCard`: Molded surface container with configurable elevation styles.
   - `NeumorphicButton`: Tactile button with primary kinetic accent or surface variants and loading states.
   - `NeumorphicTextField`: Sunken well text input with high contrast text and placeholder.
   - `NeumorphicBadge`: Molded or inset shift pills and counter chips.
   - `NeumorphicSwitch`: Recessed track with molded sliding thumb.
   - `NeumorphicDialog`: High-floating layer modal wrapper with ambient backdrop.

4. **Web Neumorphic Tokens (`admin-web/css/neumorphic.css` & `admin-web/js/tailwind-neumorphic.js`)**:
   - Mirrors all tokens, classes (`.nm-raised`, `.nm-inset`, `.nm-btn`, `.nm-card`, `.nm-input`), and Tailwind extensions (`shadow-nm-raised`, `bg-nm-surface`).

---

## 🟢 Live Traffic & Connectivity Verified by Aunty
- `[HTTP 200] GET /api/server-info` & `GET /api/test-connection` — Atlas M0 responsive.
- `[HTTP 200] GET /api/roster/2026-10` — Live month roster.
- `[HTTP 200] POST /api/engineers/delete` — Permanent database deletion endpoint.
- `[HTTP 200] GET /api/constraints` & `POST /api/constraints` — Live Atlas operational policy synchronizer.
- `[HTTP 200] GET /api/notifications` & `POST /api/notifications/send` — Broadcast notifications with in-app toggle & Resend email delivery.
- `[HTTP 200] GET /api/roster-backups/2026-10` — Recovery snapshots list.
- `[HTTP 200] POST /api/roster-backups/restore` — Roster snapshot restoration.

---

## 📲 Recent Deliveries from Aunty (👵⚡ v1.4.3)

1. **Option to Delete Engineers Inside Edit Option in App and admin-web**:
   - **`server.py`**: Added `POST /api/engineers/delete` endpoint with `$or` queries matching `id`, `name`, and `username`, deleting from MongoDB Atlas via `delete_many` and removing from in-memory fallback list.
   - **`admin-web`**: In `#engineerFormContainer` (edit form), added `#btnDeleteEngineerForm` (styled with rose border and trash icon). Displays when editing an existing engineer, prompts confirmation modal, sends POST to `/api/engineers/delete`, updates state and localStorage, and re-renders directory table and roster grids.
   - **Android App**:
     - `RosterApiService.kt`: Added `@POST("api/engineers/delete") suspend fun deleteEngineer(...)`.
     - `RosterRepository.kt` & `RosterRepositoryImpl.kt`: Implemented `deleteEngineer(id, name, username)`.
     - `AdminRosterViewModel.kt`: Added `deleteEngineer(engineer, onComplete)`.
     - `ManageTeamTab.kt`: Inside Edit Engineer dialog (`editingEngineer != null`), added red **Delete** button with a safety confirmation dialog. Upon confirmation, permanently deletes the engineer from database and updates directory state.

2. **Resigned Tagged Engineers Filtering Parity with Admin-Web in Month Roster**:
   - **Root Cause**: `filterEngineersByResignation` returned `true` whenever an engineer had `status == EngineerStatus.RESIGNED` with a null, blank, or unparseable resignation date, treating them as active and displaying them as blank ghost rows in the Month Roster.
   - **Fix Applied**: Added `parseFlexibleDate` supporting multiple date formats (`yyyy-MM-dd`, `dd-MM-yyyy`, `dd/MM/yyyy`). Active engineers are always shown. For resigned engineers, they are ONLY shown in a month roster if they have recorded shifts in that month (to preserve history) or if their resignation date is on/after the month start. If they resigned before the month start or have no date set, they are cleanly excluded.
   - Updated `filterEngineersByResignation` across `MonthRostersTab.kt` and `EngineerPortalScreen.kt` (`MonthRosterView` and `TeamShiftsView`), retaining exact week-offs sorting parity with admin-web (`sortEngineersByWeekOffs`).

3. **In-App Operations Broadcast Notifications Option**:
   - **`NotificationsTab.kt`**: Added dedicated **"Send In-App Notification (Live App Alert)"** toggle alongside **"Send Email via Resend"**. Administrators can choose either or both delivery channels.
   - **`server.py`**: Handles `send_in_app` flag in `POST /api/notifications/send`, saving in-app notifications into Atlas and `SERVER_NOTIFICATIONS`.
   - **`EngineerPortalScreen.kt`**: Integrated a modern, high-visibility **"Announcements & Broadcasts"** banner card at the top of the engineer `DashboardView`. Shows live announcements with priority pill, personalized token resolution (`Dear {name}` / `@{username}`), sender name, and timestamp.
   - **`EngineerPortalViewModel.kt`**: Added `notifications` list to `PortalState` and automatically fetches active broadcasts on dashboard load.

4. **Reinstated App Version & Architecture Specs in Both Admin & Engineer Settings**:
   - **Admin Settings (`ServerManagementTab.kt`)**: Added an **"APPLICATION & SYSTEM SPECS"** card displaying dynamic `BuildConfig.VERSION_NAME`, `BuildConfig.VERSION_CODE`, Backend Version (`v1.4.3`), Jetpack Compose framework info, MongoDB Atlas cluster info, and Cloudflare Edge Gateway info.
   - **Engineer / User Settings (`EngineerPortalScreen.kt`)**: Added an **"ABOUT ROSTERX & SPECS"** card inside `AccountView` right above the Sign Out button, showing application version, build number, backend server version, and environment specs.

---

## 📲 Recent Deliveries from Aunty (👵⚡ v1.4.2)

1. **Renamed Navigation Tab to Only "Settings"**:
   - `AdminMainScreen.kt`: Tab label renamed from `"Settings & Account"` to **`"Settings"`** (`Icons.Default.Settings`).
   - `EngineerPortalScreen.kt`: Tab label renamed from `"Settings & Account"` to **`"Settings"`** (`Icons.Default.Settings`).

2. **Universal Biometric Security Lock for All Admin Privileges**:
   - In addition to `admin` / `admin@rosterx.dev`, any user profile with `role == EngineerRole.ADMIN` is strictly barred from biometric unlock.
   - Guarded on UI click in `LoginScreen.kt` via `viewModel.checkIsAdmin(cleanId)`.
   - Hardened at ViewModel execution level in `LoginViewModel.kt`: rejects biometric login if user has `role == ADMIN` with message: `"Accounts with Admin privileges require password authentication."`

3. **Modern & Advanced Team Broadcast Console (`NotificationsTab.kt`)**:
   - **Dedicated Navigation Tab**: Tab 3 in `AdminMainScreen.kt` (`Icons.Default.Campaign`).
   - **Target Audience Options**:
     - `🌐 Everyone (Team)`: Broadcasts to all active team engineers.
     - `👤 Specific Engineers`: Interactive multi-select filter chips with engineer initials avatar and selection counter.
   - **Auto-Personalization & Mentions**:
     - `✨ Dear {name},` / `{Their name}`: Automatically substitutes the recipient's display name for every engineer (e.g. "Dear Shefeeq KK", "Dear Nandu Anthikat").
     - `@{username}`: Mention individual engineers directly with a dedicated popup picker.
   - **Priority Spectrum**: `Info` (Blue), `Shift Update` (Amber), `Urgent` (Red), `Broadcast` (Purple).
   - **Multi-Channel Delivery**: In-app notifications with MongoDB Atlas persistence + optional styled HTML email delivery via Resend API.
   - **Live Personalized Preview**: Dynamic preview card with engineer switcher to preview how `{name}` resolves before dispatching.
   - **Sent Broadcast History**: View past dispatches, recipient counts, email counts, and expandable message bodies.

---

## 📲 Recent Deliveries from Aunty (👵⚡ v1.4.1)

1. **Import Modal: Autocomplete Suggestions & Strict Configured Engineer Guard**:
   - **Real-Time Autocomplete Dropdown (`#importEngineerSuggestions`)**:
     - Typing into `#importEngineerInput` (e.g., "dya") now instantly opens an auto-suggest list filtered across configured engineers.
     - Displays avatar initial, name, username, and role.
     - Clearly distinguishes between already active engineers (`Already in List`) and excluded engineers (`+ Add to List`).
     - Tapping any suggestion directly re-activates and appends them to the active import tags and template.
   - **Strict Validation Guard**:
     - Removed automatic phantom engineer creation.
     - If an admin types a name not present in the configured engineer directory, the system blocks the addition and displays an explicit toast alert: `"⚠️ '[Name]' is not in the configured engineer list. Only configured engineers can be added."`

---

## 📲 Recent Deliveries from Aunty (👵⚡ v1.4.0)

1. **Android App: Settings & Account Section Evolution**:
   - **Admin Main Navigation (`AdminMainScreen.kt`)**: Renamed Tab 3 from "Server" to **"Settings & Account"** with `Icons.Default.Settings`.
   - **Clean TopAppBar**: Removed the red Sign Out button from `TopAppBar` header. Sign Out is now exclusively located inside the Settings & Account tab.
   - **Expanded Settings Tab (`ServerManagementTab.kt`)**:
     - Added **Admin Account Profile card** (Username: `admin`, System Administrator Root privilege, Active status).
     - Added collapsible **Password Change** section.
     - Kept live Server & Internet Diagnostics, Cloudflare Gateway configuration, and Terminal logs.
     - Placed a full-width red **Sign Out** button at the bottom of the tab.
     - Removed all App version and architecture details from the Admin console.
   - **Engineer Portal (`EngineerPortalScreen.kt`)**: Renamed Tab 4 to **"Settings & Account"** with `Icons.Default.Settings`. Removed the "About Application" version details card while keeping account settings, gateway settings, and sign out.

2. **Compact Violation Auditor Header (`AdminDashboardTab.kt`)**:
   - Replaced the previously 48-character long header with a compact, weighted header: `Icon(Shield)` + `Text("RULE AUDITOR ($currentMonthShort)")` (`Modifier.weight(1f)`).
   - The counter badge (`0 Clean` / `${violations.size} Issues`) now has full horizontal breathing room with zero wrapping or text squeezing on all mobile screen widths.

3. **Structured Operational Policy Parity & DB Sync (`OperationalConstraints.kt`)**:
   - **AI Model Standardization**: Eliminated manual model selection buttons. Automatically auto-configures the latest and best engine: **Gemini 1.5 Pro (Recommended)**, displaying an informative engine badge.
   - **Schema Parity**: Replaced loose string fields with the exact `OperationalConstraints` data model matching `admin-web`:
     - `nightHeadcount`: Int
     - `fsMin`: Int
     - `ssMin`: Int
     - `daytimeOverlapMin`: Int
     - `postNightRestDays`: Int
     - `maxConsecutiveDays`: Int
     - `swapFirstPolicy`: Boolean
     - `customDirectives`: String
     - `aiModel`: String
   - **Database Sync**: Added `GET /api/constraints` and `POST /api/constraints` to `RosterApiService`, `RosterRepository`, and `AdminRosterViewModel`. Saving policy updates syncs directly to MongoDB Atlas `operational_policies` collection.

4. **Web Portal: Recovery Snapshots & Pre-Publish Confirmation (`index.html`, `app.js`)**:
   - **Publish Confirmation Dialog (`#publishConfirmModal`)**: When admins click "Save & Publish", a modal prompts for confirmation displaying the target period, active engineer headcount, and automatic recovery assurance.
   - **Recovery Modal (`#recoveryModal`)**: Tapping "Recovery" in the header opens a snapshot browser querying `/api/roster-backups/<month>`. Admins can inspect auto-saved recovery points and execute 1-click restoration back into the live grid.
   - **Operational Policy DB Sync**: `app.js` now syncs constraints to and from MongoDB Atlas `/api/constraints` on both initialization and save.

5. **Build & Version Bump to `1.4.0` (`versionCode: 10`)**:
   - `android/app/build.gradle.kts`: `versionCode = 10`, `versionName = "1.4.0"`.
   - `server.py`: `SERVER_VERSION = "1.4.0"`.
   - Kotlin build verified: `./gradlew compileDebugKotlin` **BUILD SUCCESSFUL** in 2m 37s with 0 errors.

---

## 📝 Milestone Checklist
- [x] Settings & Account section in Android (Admin & Portal)
- [x] Sign Out moved inside Settings & Account tab (removed from Admin TopAppBar)
- [x] No App version or architecture cards in Admin console
- [x] Compact Rule Violation Auditor header (resolved squeezed counter badge)
- [x] AI Model auto-configured to Gemini 1.5 Pro (no manual selector)
- [x] Structured Operational Policy schema parity matching admin-web
- [x] Operational policies synced to MongoDB Atlas `/api/constraints` (Android + Web)
- [x] Web Portal Pre-Publish Confirmation Dialog
- [x] Web Portal Roster Recovery Snapshots modal & 1-click restore
- [x] Version bump to 1.4.0 (versionCode 10)
- [x] Android compilation clean (BUILD SUCCESSFUL)
