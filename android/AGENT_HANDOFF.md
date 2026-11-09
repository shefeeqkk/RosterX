# 👵 Aunty (Antigravity) ⇄ 🤖 Kunjappan (Android Studio) Handoff Bridge

> **"Aunty looks after the backend & servers; Kunjappan builds the native Android app!"**  
> *Last Synced by Aunty:* **2026-11-09 22:45 IST** 👵⚡🌴

---

## 📌 Active System State & Monorepo Location
- **Unified Workspace**: `c:\shefeeq\PYTHON\RosterX`
- **Native Android Project Root**: `c:\shefeeq\PYTHON\RosterX\android`
- **Semantic Version**: **`1.22.3`** (Android `versionCode: 78`, Server `SERVER_VERSION = "1.22.3"`)
- **Previous Versions**: `1.22.2` (Build 77), `1.22.1` (Build 76), `1.22.0` (Build 75), `1.21.9` (Build 74), `1.21.8` (Build 73), `1.21.7` (Build 72), `1.21.6` (Build 71)
- **Production Server**: ☁️ **Render Cloud + Branded Cloudflare Domain**  
  🌐 `https://rosterx.online` & `https://api.rosterx.online`  
  📱 **Android Base URL**: `https://api.rosterx.online/api/`
- **Local Asset Sync File**: `android/app/src/main/assets/server_url.txt` (`https://api.rosterx.online/`)
- **Email Infrastructure**: Verified Custom Domain `rosterx.online` on Resend. Primary dispatch: `RosterX Operations <notify@rosterx.online>` via HTTPS REST (Port 443).
- **Database Engine**: MongoDB Atlas `Cluster0` (Database: `rosterx`, Collections: `engineers`, `rosters`, `roster_backups`, `operational_policies`, `notifications`, `broadcast_templates`, `password_resets`, `audit_logs`)
- **Tailscale Static IP for Phone**: `100.71.241.36`
- **Cross-Platform Parity Specification**: [`ANDROID_PARITY.md`](../ANDROID_PARITY.md)
- **Remote Agent Guide**: [`REMOTE_AGENT_GUIDE.md`](../REMOTE_AGENT_GUIDE.md)

---

## 🚀 Major Upgrades in v1.22.3

### 1. ✏️ Top Bar Pencil Icon Eliminated (Single Pencil Standard Enforced)
- Removed the conditional `IconButton` with `Icons.Default.Edit` from `TopAppBar` `actions` in `EngineerPortalScreen.kt`.
- The top app bar now cleanly contains only the profile chip.
- The solitary pencil icon in the entire Account & Settings interface is situated at the top-right of the avatar card (`StandardProfileHeaderCard`), exactly fulfilling the user design specification.

### 2. 📱 Authoritative Profile Hydration & Own Phone Unmasking
- **`GET /api/auth/me` Integration**:
  - Added `getMe(): CurrentUserResponse` to `RosterApiService` and `getCurrentUser(): Result<Engineer>` to `RosterRepository`.
  - Added authoritative profile hydration directly into `EngineerPortalViewModel.loadPortalData()`, guaranteeing the logged-in user's profile is initialized with unmasked data (`+91 9809226169`).
- **Asterisk Contamination Prevention**:
  - In `UserSessionManager.setCurrentUser()`, if an incoming `user.phone` contains `*` but the local session already has a clean unmasked phone, the clean phone is preserved.
  - In `EngineerPortalViewModel`, merging directory engineers into `loggedInEngineer` ignores phone numbers containing `*`.
  - In `UnifiedEditProfileDialog`, if `currentPhone` contains asterisks, the text field defaults to empty instead of pre-populating asterisks.
- **Backend Role & Identity Checks**:
  - In `server.py` `GET /api/engineers`, hardened `is_own_profile` matching across `username`, `id` (`eng_*`), `email`, `name`, and `full_name`.
  - In `server.py` `GET /api/auth/me`, added `default_weekly_offs` to returned claims.

---

## 🚀 Major Upgrades in v1.22.2

### 1. 🎂 & 🏆 Birthday & Work Anniversary Personal Celebrations (`CelebrationWishesDialog`)
- **Animated Celebratory Modal**:
  - Custom full-screen modal with infinite pulsing and wobble animations (`scaleAnim`, `rotateAnim`), festive multi-stop radial gradient badge, and emoji accents.
  - Dynamically customizes title, subtitle, and warm heartfelt greetings based on `CelebrationType.BIRTHDAY`, `CelebrationType.ANNIVERSARY` (with ordinal years e.g., 1st, 2nd, 3rd), or `CelebrationType.BOTH`.
- **Automatic Welcoming On Special Day**:
  - `LaunchedEffect(state.loggedInEngineer)` in `EngineerPortalScreen` checks if today's date matches the engineer's birthday or work anniversary.
  - Checks SharedPreferences `seen_{username}_{todayStr}` so the user is greeted once per day upon opening the app without repeated interruptions.
- **Interactive Celebration Banners**:
  - Rendered at the top of `DashboardView`, `ManagerDashboardView`, and `AccountView`.
  - Displays `"TODAY! 🎉"` and `"TODAY! 🏆 (${years}y)"` in Account Details rows.
  - Tapping any celebration banner re-opens the animated dialog anytime.

### 2. 📢 Automated Daily Event Broadcasts & FCM Push to Team
- **Automated Server Engine (`check_and_trigger_celebrations`)**:
  - Periodically detects birthdays and work anniversaries for all team members in Indian Standard Time (IST, UTC+05:30).
  - Automatically posts an official high-priority celebration announcement in `ATLAS_DB.notifications` and `SERVER_NOTIFICATIONS` from `"RosterX Celebrations Desk"`.
  - Dispatches Google FCM push notifications to all registered devices across the team so everyone is alerted.
  - Idempotent: indexed by unique `celebration_id` (`celeb_YYYY-MM-DD_username_type`) to guarantee exactly once delivery per day.
  - Supported endpoints: `POST /api/celebrations/check` and automatic check on `GET /api/notifications` and `GET /api/engineers`.

### 3. ✏️ Strict Single Pencil Icon Standard
- **Eliminated Duplicate Edit Entrypoints**:
  - Removed duplicate pencil icons from `TopAppBar` actions and the `ACCOUNT DETAILS` card header.
  - Kept **ONLY ONE unified edit pencil button** on the top-right of the avatar profile card (`StandardProfileHeaderCard`).
  - Removed clickability from the entire card and avatar box — edit dialog opens **only** when tapping the dedicated top-right pencil button.

### 4. 🔒 Current Password Verification for Password Changes
- **Enhanced Account Security**:
  - `UnifiedEditProfileDialog` now requires entering **Current Password** whenever changing password.
  - Full eye icon toggle for show/hide password across all three password fields.
  - `server.py` verifies `current_password` against the PBKDF2 hash using constant-time verification (`verify_password`).

### 5. 🗑️ Photo Removal Confirmation Alert
- **Prevent Accidental Photo Deletion**:
  - Clicking "Remove" in `UnifiedEditProfileDialog` shows an `AlertDialog` confirming whether the user truly wants to remove their profile photo before wiping it.

### 6. 📱 Own Contact Phone Unmasked & Asterisk Overwrite Protection
- **Phone Visibility**:
  - In `server.py` (`GET /api/engineers`), `can_view_phone = is_admin or (caller_role == "manager") or is_own_profile` ensures logged-in engineers view their own phone unmasked.
  - In `POST /api/users/profile`, strings containing `*` are rejected from phone updates, preventing masked strings from corrupting the database.
  - Restored clean phone number `+91 9809226169` for `shefeeqkk` in Atlas DB.

---

## 🚀 Major Upgrades in v1.22.0

### 1. ⚙️ Unified Account Settings Popup & Single Edit Pencil Standard
- **Eliminated Fragmented Edit Dialogs**:
  - Removed separate single-field name dialog, separate photo action dialog, and separate expandable password accordion.
  - Placed **ONE unified edit pencil icon (✏️)** on the top right of the Settings screen (both in the TopAppBar actions and on `StandardProfileHeaderCard`).
  - Clicking this pencil opens the comprehensive **`UnifiedEditProfileDialog`** modal popup.
- **Unified Profile & Security Editing**:
  - **Profile Picture**: In-dialog photo preview with "Change Photo" (via Android PhotoPicker) and "Remove" action.
  - **Personal Details**: Full Name, Contact Phone, Registered Email.
  - **Date Pickers**: Joining Date and Birthday (🎂) with native `DatePickerDialog`.
  - **Security & Password**: Optional password reset (New Password + Confirm Password with eye toggle and validation).

### 2. 🎂 Birthday Field Full-Stack Integration
- **Database, Backend, Web & Mobile Parity**:
  - **MongoDB Atlas**: Persists `birthday` attribute in `engineers` collection.
  - **Python Backend (`server.py`)**: `GET /api/auth/me` returns `join_date` and `birthday`; `POST /api/users/profile` handles unified updates.
  - **Admin Web Console (`admin-web/`)**: Added birthday date picker input in engineer modal and displays `🎂 YYYY-MM-DD` in engineer tables.
  - **Web User Portal (`user-portal.html` & `userPortal.js`)**: Displays birthday card in engineer account overview.
  - **Android Data & Domain Models**: Added `birthday` to `Engineer` and `EngineerDto`.

### 3. 🖼️ Manager Dashboard Hero Welcome Card Profile Picture
- **Replaced Initial Monogram with Real Photo**:
  - `ManagerDashboardView` now uses `UserAvatar` displaying the manager's real `profilePicture` (with fallback to initials monogram if not set) in the top hero welcome card.

### 4. 📊 Firebase Screen & Behaviour Analytics Telemetry
- **Complete Screen View Tracking**:
  - Automatically logs `FirebaseAnalytics.Event.SCREEN_VIEW` for every tab in `EngineerPortalScreen` ("Dashboard", "Team Shifts", "My Calendar", "Month Roster", "Settings") and `AdminMainScreen` ("Admin Dashboard", "Admin Manage Team", "Admin Month Rosters", "Admin Notifications", "Admin Server Management").
- **Behaviour Analytics Events**:
  - `tab_switched` / `admin_tab_switched`: Tracks tab navigation with user role.
  - `team_shifts_date_selected`: Tracks date navigation in team shifts radar.
  - `month_roster_changed`: Tracks previous/next month browsing.
  - `notification_dismissed` / `notifications_cleared_all`: Tracks notification interactions.
  - `theme_palette_changed` / `theme_mode_changed`: Tracks customization preferences.
  - `profile_updated`: Tracks profile modifications with parameter flags.

---

## 🚀 Major Upgrades in v1.21.9

### 1. 🎬 Fluid In-App Notification Dismissal Animations
- **Slide & Fade Exit Transitions**:
  - *Root Cause*: Previously, tapping the close button on an operational broadcast card immediately dispatched `onDismissNotification(id)` to the ViewModel. This mutated `state.dismissedNotificationIds`, causing the card to be immediately unmounted from `items(activeNotifs)` during recomposition before `AnimatedVisibility` could execute its exit transition.
  - *Fix*: Introduced local `dismissingIds` state in both `ManagerDashboardView` and `DashboardView`. When the close button or "Clear All" is pressed, the ID is immediately added to `dismissingIds` (keeping it present in `activeNotifs`), triggering a 320ms `slideOutHorizontally(targetOffsetX = { it }) + fadeOut() + shrinkVertically()` animation before calling ViewModel dismiss.

### 2. 📱 2-Column Manager Widget (4x2 and 3x2 Sizes)
- **Optimized Screen Real Estate**:
  - Eliminated the wide, blank whitespace on the right side of the manager widget by grouping on-duty and rest engineers into responsive 2-column paired rows (`item_widget_shift_pair.xml`).
  - Displays the engineer's first name only (e.g. `[FS] Nandu` | `[FS] Dhanush`) and compact shift timing (e.g. `07:30 - 04:30`), fitting 6+ on-duty engineers in just 3 vertical rows without truncation.
  - Gracefully handles odd engineer counts by rendering the right column as `View.INVISIBLE` to preserve balanced 50/50 sizing.

### 3. 🖼️ Widget Showcase Mockup Preview
- **Realistic "Add Widget" Screen Preview**:
  - *Root Cause*: The Android widget picker previously showed only the app hexagon logo (`ic_rosterx_widget_logo`) in a plain grey box because `android:previewLayout` was missing.
  - *Fix*: Added `android:previewLayout="@layout/widget_preview_mockup"` in `rosterx_widget_info.xml`. Designed a realistic Neumorphic preview (`widget_preview_mockup.xml`) showing authentic Today & Tomorrow shift cards, colored squircle badges, active duty indicators, and rest day pills.

---

## 🚀 Major Upgrades in v1.21.3

### 1. 👤 Dynamic Per-Recipient FCM Personalization
- **Fixed Generic "Team Member" in FCM Push**:
  - *Root Cause*: `server.py` was batch-sending FCM push payloads with `{name}` collapsed to a fallback `"Team Member"` placeholder.
  - *Fix*: Refactored `server.py` to iterate through each recipient engineer individually and dynamically substitute `{name}` with their actual `fullName` or `name` (e.g. `Dear SHEFEEQ,` or `Dear Nandu Anthikat,`) and `{username}` with their username.
- **Synchronized with Web Admin Simulator**: Now the live simulator in the Web Admin Console and the physical phone notification panel show the exact same personalized engineer greeting!

---

## 🚀 Major Upgrades in v1.21.2

### 1. 📊 Google Firebase Analytics Integration
- **SDK Integration**: Added `firebase-analytics` to `android/gradle/libs.versions.toml` and `android/app/build.gradle.kts`.
- **`AnalyticsManager` Singleton**:
  - Injected across app (`MainActivity`, `RosterXFirebaseMessagingService`).
  - Automatically captures screen transitions (`login`, `admin`, `engineer`), user ID and role properties, login events, and notification receipt telemetry.
- **Instant DebugView Support**: Configured for testing via `adb shell setprop debug.firebase.analytics.app com.rosterx`.

### 2. 🔔 FCM Backend Authentication Architecture & Render Configuration
- Added `firebase-admin>=6.5.0` to `requirements.txt`.
- Enhanced `send_fcm_push()` in `server.py` to support `FIREBASE_SERVICE_ACCOUNT_JSON` environment variable (for Render cloud deployment) alongside local `firebase-adminsdk.json`.
- Strict telemetry logging for token delivery and credential status.

### 3. 🕒 Local Timezone Formatting for Broadcast History
- Replaced raw UTC date rendering (`2026-11-09 00:21:26 UTC`) with intelligent client-side timezone formatting:
  - Web Admin: Uses browser `Intl.DateTimeFormat` to show local Indian Standard Time (e.g. `09 Nov 2026, 05:51:26 AM (IST)`).
  - Android App: Formats timestamps via `java.time.ZoneId.systemDefault()` into localized strings (`dd MMM, hh:mm a`).

---

## 🚀 Major Upgrades in v1.21.1

### 1. 🛠️ Critical Broadcast Bug Fixes (Web Admin & Backend)
- **Resolved Specific Engineer Selection Highlighting All Chips**:
  - *Root Cause*: Default engineers lacked an `id` field, making `has(eng.id)` check `has(undefined)` and highlighting all chips upon clicking any engineer.
  - *Fix*: Created robust key helper `getEngineerKey(eng)` mapping across `id`, `username`, and normalized `eng_<name>`.
- **Fixed Live `@` Mention Selection Not Applying**:
  - *Root Cause*: Textarea blur event fired before suggestion click; cursor selection shifted and missing usernames led to `@undefined`.
  - *Fix*: Added `mousedown` listener with `e.preventDefault()`, tracked exact character replacement ranges (`mentionMatchStartIndex`/`EndIndex`), and applied fallback usernames.
- **Fixed Message Dispatch & Visibility When Typing in Web Admin**:
  - *Root Cause*: Broadcasts dispatched with `send_in_app: false` were previously skipped from DB insertion, leaving history blank.
  - *Fix*: Backend now ALWAYS records all broadcasts into `ATLAS_DB.notifications` and memory for full audit trail and read receipts. `send_in_app` now selectively controls visibility for non-admin engineer portal feeds.
  - Added `Ctrl+Enter` shortcut in composer textarea for instant broadcast dispatch.

### 2. 📱 Full Android App Feature Parity for Broadcast Console (`NotificationsTab.kt`)
- **Operational Templates Carousel**: Integrated quick-load template chips (Shift Announcement, Urgent Handover, System Maintenance, Swap Request) fetching from server API and local defaults.
- **Tri-Channel Delivery Controls**: Added dedicated Phone Panel (FCM Push) toggle alongside In-App and Email.
- **Inline `@` Mention Suggestions**: Floating suggestion dropdown appears in Compose whenever `@` is typed, inserting `@username` or `@all` and auto-selecting the engineer.
- **Dynamic Live Preview**: Translates `@username` to full name badges in real time and simulator chips.
- **Broadcast History Read Receipts & Soft-Retraction**:
  - Added live read receipts display (`X Read / Y Pending`), FCM push count, and email count on each card.
  - Added "Clone" notice button to load history notices back into composer.
  - Added "Retract" action with confirmation dialog calling `POST /api/notifications/retract`.

---

## 🚀 Major Upgrades in v1.21.0

### 1. 🔔 Free Firebase Cloud Messaging (FCM) & Phone Notification Panel Integration
- **100% Free at Scale**: Integrated Google Firebase Cloud Messaging with zero subscription or message quota charges.
- **`google-services.json` Installed**: Configured for package `com.rosterx` under Firebase Project `rosterx-tejas`.
- **`RosterXFirebaseMessagingService`**:
  - Implemented service extending `FirebaseMessagingService` to capture `onNewToken` and `onMessageReceived`.
  - Creates Android notification channel `"rosterx_operational_broadcasts"` ("Operations & Broadcasts") with `IMPORTANCE_HIGH`, sound, and vibration.
  - Posts native `NotificationCompat.Builder` notifications to the Android status bar and drop-down notification drawer.
  - Tapping notification launches `MainActivity` with deep link extras.
- **Device Token Registration**:
  - `MainActivity` and `RosterApiService` automatically fetch `FirebaseMessaging.getInstance().token` and send to `POST /api/users/fcm-token`.
  - Backend stores tokens in MongoDB Atlas `engineers.fcm_tokens` via `$addToSet`.
- **Multi-Channel Push Dispatch**:
  - When admin broadcasts via `/api/notifications/send` with `send_push: true`, the backend dispatches instant FCM push notifications to all recipients.

### 2. 💬 Live `@` Mention Autocomplete & Dynamic Full Name Translation
- **Live `@` Mention Autocomplete**:
  - Typing `@` in the broadcast composer textarea opens an inline popup dropdown showing active team engineers with avatars, names, and `@username`.
  - Supports keyboard navigation (`ArrowUp`, `ArrowDown`, `Enter`, `Tab`, `Escape`) and click selection.
  - Selecting a user inserts `@username` and automatically includes them in the recipient audience.
- **Dynamic Name Translation**:
  - In all final rendered messages (Android app, Web Engineer Portal, Resend HTML Email, and Phone push notifications), every `@username` is dynamically translated to the engineer's **Full Name** styled as a high-visibility badge (e.g. `@Alice Johnson`).
  - Supports team shortcuts like `@all` and `@everyone` rendering as `@Everyone (All Team)`.

### 3. 💾 Database-Persisted Operational Templates
- **MongoDB Atlas Collection (`broadcast_templates`)**:
  - Backed by REST endpoints:
    - `GET /api/broadcast/templates`: Authenticated retrieval; auto-seeds 4 standard operational templates (Shift Update, Urgent Coverage, Maintenance, Handover).
    - `POST /api/broadcast/templates`: Saves or updates custom operational templates.
    - `DELETE /api/broadcast/templates`: Deletes custom templates.
  - Admins can save their custom operational notices straight to the cloud DB from the web or Android console.

### 4. 📊 Live Read Receipts & Audit-Preserving Soft Retraction
- **Real-Time Read Receipts**:
  - Backend computes `read_by` (engineers who have dismissed the notification) and `pending` (engineers who have not yet read/dismissed).
  - Admin history displays live status badge (`X Read / Y Pending`) with modal breakdown.
- **Soft Retraction (`is_retracted: true`)**:
  - `POST /api/notifications/retract` marks announcement as retracted with timestamp and admin name.
  - Immediately hides the announcement from all engineer in-app portal views while preserving the complete immutable audit trail in Admin History.

### 5. 🌐 Web Admin Broadcast Console & Web Engineer Portal Parity
- **Web Admin Console (`data-tab="broadcast"`)**:
  - Dedicated 6th navigation tab with Neumorphic banner, Audience selector, Priority chips, Template manager, Live Preview simulator, and History feed.
- **Web Engineer Portal Notification Cards**:
  - Dismissible notification cards placed below dashboard metrics in `user-portal.html`, with individual dismiss and "Clear All" calling `/api/notifications/dismiss`.
  - Visual pulse badge and top toast for Urgent announcements.

---

## 🚀 Major Upgrades in v1.20.0

### 1. Ubiquitous User Profile Picture Architecture & Database Standardization
- **Standardized Database Storage**:
  - Profile pictures are stored directly in MongoDB Atlas (`rosterx.engineers` collection) as Base64 data URIs (`data:image/jpeg;base64,...`) in the `profile_picture` field.
  - Sizing & Optimization: Strict backend processing via Pillow (`process_profile_picture_base64()`) center-crops to 1:1 square, handles EXIF orientation, and downscales to 256×256 px at JPEG quality 85. Output size is ~20–35 KB per avatar.
  - Sizing verification: For a 20–50 member team, total DB footprint is ~1 MB, keeping Atlas M0 free tier (512 MB) completely unencumbered without third-party object storage.
- **RESTful Endpoints**:
  - `POST /api/users/profile-picture`: Authenticated upload, update, and deletion (`null`/`""`) with automatic session claim refresh and `audit_logs` entry.
  - `GET /api/users/<username>/avatar`: Streaming binary JPEG endpoint with HTTP cache headers (`Cache-Control: public, max-age=86400, stale-while-revalidate=604800`) for standard web/external image consumers.
  - Claims refresh: `POST /api/auth/login`, `GET /api/auth/me`, and `POST /api/engineers` all include `profile_picture`.

### 2. Native Android Ubiquitous Avatar Display & Fast In-Memory LRU
- **`UserAvatar` Composable**:
  - High-performance avatar component with built-in LRU cache (`avatarBitmapCache`), preventing repetitive Base64 decoding during list scrolls.
  - Graceful fallback: If no avatar is uploaded, extracts engineer initials and displays a neumorphic monogram with deterministic role/name-tinted background.
- **Ubiquitous Placement Across Android**:
  - **TopAppBar Profile Pill**: Replaced text-only initial circle with `UserAvatar(size = 24.dp)`.
  - **Account Settings Tab**: Interactive 76dp avatar with camera badge, modern Android Photo Picker (`ActivityResultContracts.PickVisualMedia`), and "Change Photo" / "Remove" actions.
  - **Today's Shift Radar (`DutyGroupCard`)**: Replaced 26dp initials with `UserAvatar(size = 28.dp)` next to each active/upcoming engineer.
  - **Team Shifts Tab**: Integrated `UserAvatar(size = 20.dp)` into the fixed engineer name column.
  - **Admin Team Directory Tab (`ManageTeamTab`)**: Replaced monogram badge with `UserAvatar(size = 44.dp)` in directory list items, and added avatar upload/remove controls to the Create/Edit Engineer dialog.
  - **Admin Month Rosters Tab (`MonthRostersTab`)**: Integrated `UserAvatar(size = 20.dp)` into row headers of the Master Grid, and `UserAvatar(size = 36.dp)` in the Shift Assignment / Swap bottom sheet.
- **Device-Side Compression**: Added `compressImageUriToBase64()` to scale images to 512×512 prior to network upload.

### 3. Web Admin & User Portal Ubiquitous Avatar Integration
- **Top Navigation User Pills**: Profile pills in both `admin-web/user-portal.html` and `admin-web/index.html` render user avatar thumbnails.
- **Interactive Upload Modal**: Profile modals allow instant image selection, client-side canvas square cropping and resizing, preview, and upload/removal.
- **Team Directory Cards**: Engineer directory cards render avatar thumbnails alongside badges and contact info.

---

## 🚀 Major Upgrades in v1.19.0

### 1. Widget Loading View & 12-Hour AM/PM Time Format
- **Zero-Height Loading View**: Implemented `widget_empty_loading.xml` inside `RosterXWidgetService.getLoadingView()`, completely removing the default system `"Loading..."` text above widget items.
- **12-Hour AM/PM Shifts**: Updated `shiftRegistry` in `RosterXAppWidgetProvider` from 24h format to standard 12-hour AM/PM format (`07:30 AM – 04:30 PM`, `09:00 PM – 07:30 AM (+1)`).

### 2. Terminology Update (Staff $\rightarrow$ Engineer)
- Replaced all user-facing instances of `"Staff"` with `"Engineer"` / `"Engineers"` across the Manager Dashboard cards (`DutyGroupCard`) and Widget section headers (`⚡ ON DUTY`, `☕ REST & LEAVE`), with pluralization checks.

### 3. Overnight Night Shift (NS) Rollover & Active Duty Accuracy
- **Midnight Transition Fix (00:00 - 07:30 AM)**: 
  - Yesterday's Night Shift engineer (whose shift runs from 21:00 yesterday to 07:30 today) is now correctly displayed under the **Night Shift** card in the app and pinned to the top of `⚡ ON DUTY` in the widget with `[• ACTIVE NOW]`.
  - Excluded from "Scheduled Off & Leave" / rest while on active duty overnight.
  - Today's upcoming NS engineer is displayed without the active tag until their shift starts at 09:00 PM tonight.

---

## 🚀 Major Upgrades in v1.18.0

### 1. Manager Widget 2-Card Redesign & Chronological Shift Sorting
- **Clear Summary String**: Updated to `"Total on Duty: 4 Engineers | Rest: 5 Engineers"` (no confusing day/night breakdown).
- **Two High-Density Cards**:
  - **⚡ ON DUTY**: Engineers listed strictly sorted by shift start timing (`FS` 07:30, `FG` 08:30, `G` 09:30, `SG` 11:00, `SS` 13:00, `NS` 21:00).
  - **☕ REST & LEAVE**: Rest day staff on `OFF`, `SW*`, and `L`.
- **Ultra-Compact RemoteViews**: Reduced padding and badge sizes (`24dp`), enabling 6-8 engineers visible at a glance with minimal scroll.

### 2. Theming & Drop Shadow Glow Unification
- **Dynamic Accent Glow**: Replaced hardcoded salmon/pinkish drop shadow (`0x59FF5252`) with dynamic `LocalThemeAccent.current.copy(alpha = 0.35f)`, ensuring all neumorphic CTA glows and the floating bottom navigation bubble match Tory Blue `#174EAC` / `#7294CC` (or selected palette).
- **Collapsible Theme Card**: The "Appearance & Theme" card in the Settings tab is now collapsible, **closed by default**, displaying an active summary pill (`Dark • Tory Blue`) when collapsed.

### 3. Manager Dashboard Layout Polish & Health Liveliness
- **Prevent Staff Counter Squeeze**: Replaced the right header row in `DutyGroupCard` with compact micro-tagging (`CURRENT`) and guaranteed single-line text for staff pills (`maxLines = 1, softWrap = false`), preventing wrap onto two lines.
- **Liveliness Feedback on Health Check**: Tapping refresh on the System & DB Health card now provides immediate visual feedback: 360° continuous spinning icon, amber pulsing status banner with `"Pinging Atlas M0 Cluster..."`, and animated `"Pinging..."` sync state.

---

## 🚀 Major Upgrades in v1.17.0

### 1. Global Theming Engine & Color Palette Customization
- **Theme Mode Toggle**: Implemented System, Light, and Dark mode switching in Account/Settings tab, persisted via `UserSessionManager`.
- **Dual-State WCAG Brand Palettes**:
  - **Tory Blue (`#174EAC` Light / `#7294CC` Dark) — Default**: Complies with the 20-30% desaturation and WCAG 4.5:1 text / 3:1 UI rule for dark slate surfaces (`#1E293B`) without changing brand hue (~218°).
  - Additional palettes: **Crimson Coral** (`#FF5252` / `#FB7185`), **Emerald Green** (`#059669` / `#34D399`), **Indigo Violet** (`#6366F1` / `#818CF8`), **Sunset Amber** (`#D97706` / `#FBBF24`), **Ocean Cyan** (`#0284C7` / `#38BDF8`).
- **Dynamic Neumorphic Tokens**: `NeumorphicTokens.Accent` and `NeumorphicTokens.AccentSalmon` dynamically resolve to `LocalThemeAccent.current`.

### 2. Manager UI Enhancements & Compact Roster Widget
- **Compact Manager Home Screen Widget**: Replaced split cards with a sleek, compact, scrollable `ListView` showing all engineers on duty today with shift badges, timings, and active status (`RosterXWidgetService` + `RemoteViewsFactory`).
- **Sleek Manager Dashboard Header**: Removed verbose "Operation manager" and "Operation Hub" headlines; made header ultra-compact and focused.
- **Layout Order**: Reordered dashboard to place **Today's Shift Radar** *first*, followed by the **System & Database Health** card.
- **Accurate Operational Shift Windows**:
  - Morning (`FS`, `FG`, `G`, `SG`): **07:30 AM – 08:00 PM**
  - Afternoon (`SS`): **01:00 PM – 10:00 PM**
  - Night (`NS`): **09:00 PM – 07:30 AM** (next day)
  - Granular minute-by-minute active duty detection for each shift code.
- **Resigned Staff Exclusion**: Resigned engineers (e.g. `RAJ KUMAR`, resigned Oct 25, 2026) are strictly excluded from shifts, countings, and scheduled off pools in all subsequent months.

---

## 🚀 Major Upgrades in v1.16.0

### 1. Manager Profile Reconstruction & Shift Exclusion
- **Zero Shift Assignment for Managers**: `Engineer.isManagementAccount` unconditionally returns `true` for `EngineerRole.MANAGER`. Managers are automatically purged from Team Shifts, Month Roster grids, shift assignments, and swap pools.
- **Dynamic 4-Tab Navigation**: Managers now see 4 tabs (`Dashboard`, `Team Shifts`, `Month Roster`, `Settings`), completely removing the irrelevant "My Calendar" tab, while standard engineers retain all 5 tabs.
- **Advanced Operations Dashboard (`ManagerDashboardView`)**:
  - **Operations Hub Hero**: Dedicated manager welcome card with `OPERATIONS MANAGER` badge.
  - **Server & MongoDB Atlas Health Radar**: Live status indicator (connected dot, latency in ms, cluster `Cluster0`, database `rosterx`, environment `Render Cloud / Atlas M0`, and on-demand refresh).
  - **Today's Shift Radar (`ManagerRadarSection`)**: Categorized into Morning (`FS/FG/G/SG`), Afternoon (`SS`), Night (`NS`), and Rest (`OFF/L/SW*`) duty groups.
  - **Active Duty Now Detection**: Live IST hour detection highlights currently active engineers on duty with an `ACTIVE NOW` badge.
  - **Purged Personal Shift Cards**: Removed today & tomorrow personal shift cards, monthly duties counters, and rest day countdowns from the Manager's dashboard.
- **Operations Manager Home Screen Widget**:
  - When a manager account is logged in, `RosterXAppWidgetProvider` automatically renders the Operations Radar widget showing current on-duty staff, active duty badge, upcoming shift handoff, and total daily coverage breakdown.

---

## 🚀 Major Upgrades in v1.15.0

### 1. Antigravity Cloud Remote Agent & Tailscale Wireless ADB
- **Full Cloud Agent Migration**: Added complete headless automation for building and debugging the Android application directly from the Antigravity Cloud Remote Agent connected to GitHub [`shefeeqkk/RosterX`](https://github.com/shefeeqkk/RosterX).
- **Automation Scripts**:
  - `scripts/setup-remote-agent.sh`: Provisions OpenJDK 17, ADB, Python, and headlessly connects Tailscale via `TAILSCALE_AUTHKEY`.
  - `scripts/remote-deploy.sh`: Builds debug APK via `./gradlew assembleDebug --no-daemon`, connects wirelessly via Tailscale to `100.71.241.36:5555`, installs the APK, launches MainActivity, and captures verification screencap.
  - `scripts/tailscale-connect.sh`: Diagnostic helper checking Tailscale status, tailnet peers, and pinging target device.
- **Dynamic Port & Multi-Wi-Fi Persistence**:
  - By binding the ADB daemon to `0.0.0.0:5555` via `adb tcpip 5555`, the connection survives home Wi-Fi, office Wi-Fi, and 5G cellular switches without losing the static Tailscale IP (`100.71.241.36`).
  - Fallback prompt handles dynamic Android 11+ wireless ports gracefully if the phone is rebooted.

---

## 🎨 Major Upgrades & Fixes in v1.14.15

### 1. Surgical Account Deletion Targeting
- In `server.py` (`POST /api/engineers/delete`) and `AdminRosterViewModel.kt`:
  - When deleting an account, queries strictly target unique `username` or `id`.
  - Removed broad `$or` name matching that previously caused multiple accounts sharing the same display name (e.g. `"System Administrator"`) to be deleted together.
  - Deleting an individual duplicate now deletes *only* that specific account.
- Master administrator account is safely preserved in MongoDB Atlas with single `@admin` account (`kkshefeeq005@gmail.com`).

### 1. Server & Public Gateway Configuration Collapsed by Default
- In `EngineerPortalScreen.kt`, `showGatewaySettings` now defaults to `false`.
- The expandable card is collapsed on screen load, keeping the Settings tab clean and uncluttered.

### 2. Widget Static Shift Times (No Stale Minutes)
- In `RosterXAppWidgetProvider.kt`:
  - Replaced minute-by-minute countdowns (`Shift Ends in: 05h 29m` / `Duty Starts in: 10h 09m`) with clean, static boundary times:
    - Active duty: `"Shift Ends at $endTime"` (e.g., `Shift Ends at 20:00`)
    - Upcoming today: `"Duty Starts at $startTime"` (e.g., `Duty Starts at 11:00`)
    - Next day duty: `"Next Shift: Tomorrow at $nextStart"`
    - Future duty: `"Next Shift: EEE, MMM dd at $nextStart"`
    - Rest cycle: `"Scheduled Rest Day (OFF)"`
  - Kept exact AlarmManager wakeup triggers so the widget seamlessly flips status right at start/end boundaries without polling or showing inaccurate minute countdowns.
  - In-app dashboard live countdown with ticking seconds remains intact.

### 3. Team Account Edit Duplication Elimination
- In `server.py`:
  - Supported `original_username` parameter in `POST /api/engineers`.
  - When an account's username/domain ID is edited, `server.py` deletes the old document (`old_user`) while preserving existing salted PBKDF2 password hashes and creation metadata, and upserts the updated profile.
  - Prioritized `username` explicitly passed from client forms over email domain slicing, preventing accidental renaming.
- In `ManageTeamTab.kt`, `EngineerDto.kt`, and `RosterRepositoryImpl.kt`:
  - Carried `originalUsername` during edits so that renames replace the existing account in MongoDB Atlas rather than leaving duplicate records.

---

## 🤖 Direct Briefing for Kunjappan (Android Studio Mobile Agent)

1. **Version Bump**:
   - `android/app/build.gradle.kts` has been bumped to `versionName = "1.14.14"` and `versionCode = 57`.
2. **Settings Tab**:
   - Gateway settings card is now cleanly collapsed by default.
3. **Widget Timer**:
   - Displays clean static times (e.g. `Duty Starts at 11:00`, `Shift Ends at 20:00`) avoiding stale minute counters.
4. **Account Management**:
   - Team account editing replaces the record without duplication.

---

*Signed with love & clean code,*  
**Aunty (Antigravity Lead Full-Stack Architect)** 👵⚡
