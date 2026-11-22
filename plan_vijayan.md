# 📋 Execution Plan for Vijayan (UI Specialist — Android & Web)

**Status:** `READY FOR VIJAYAN EXECUTION` (Sprint v2.3.16 - Build 168: Removal of Android Pull-to-Refresh, Circular Neumorphic Top Bar Refresh Button with 360° Spin Animation, 8dp Touch Slop & Web Gesture Isolation)

---

## 🔐 Mandatory Production Rule from Gafoor (System Architect 🕶️)
> **Public GitHub Repo + Auto-Deploying Render Production Server:**
> - **ZERO hardcoded personal names, usernames, emails, phone numbers, passwords, or secrets** anywhere in HTML, CSS, JS, Kotlin, comments, or placeholders.
> - **Mandatory `git push origin main`** immediately after verification so Render (`https://api.rosterx.online`) deploys immediately!

---

## 🚀 Active Sprint: Task 12 — Fluid Tab Swiping & Top Bar Refresh Overhaul (v2.3.16 - Build 168)

### 1. Android Jetpack Compose Remediation
1. **Completely Remove Pull-to-Refresh Contention**:
   - In `EngineerPortalScreen.kt`:
     - Remove `PullToRefreshContainer` and `rememberPullToRefreshState` imports.
     - Remove `val pullRefreshState = rememberPullToRefreshState()` and associated `LaunchedEffect`.
     - Remove `.nestedScroll(pullRefreshState.nestedScrollConnection)` modifier on Tab 0 (`DashboardView`).
     - Remove `PullToRefreshContainer(state = pullRefreshState, ...)` composable overlay.
   - In `AdminMainScreen.kt`:
     - Remove `PullToRefreshContainer` and `rememberPullToRefreshState` imports.
     - Remove `val pullRefreshState = rememberPullToRefreshState()` and associated `LaunchedEffect`.
     - Remove `.nestedScroll(pullRefreshState.nestedScrollConnection)` on Tab 0 (`AdminDashboardTab`).
     - Remove `PullToRefreshContainer(state = pullRefreshState, ...)`.

2. **Circular Neumorphic Top Bar Refresh Button**:
   - In `EngineerPortalScreen.kt` `TopAppBar` `actions`:
     - Insert a circular Neumorphic icon button placed immediately to the left of the 3D Notification Bell:
       `[ 🔄 Refresh ] [ 🔔 Bell ] [ 👤 Profile ]`
     - Style with `size(38.dp)`, `CircleShape`, `NeumorphicElevationStyle.RaisedSubtle`, `surfaceColor = NeumorphicTokens.Surface`.
     - Inside, render `Icons.Default.Refresh` with tint `NeumorphicTokens.AccentSalmon` and rotation animation driving 0° to 360° continuously when active.
     - On click: set `isManualRefreshing = true`, invoke `viewModel.refreshAllData { isManualRefreshing = false }`.
   - In `AdminMainScreen.kt` `TopAppBar` `actions`:
     - Insert matching circular Neumorphic refresh button immediately to the left of the user profile card:
       `[ 🔄 Refresh ] [ 👤 Profile ]`
     - On click: set `isManualRefreshing = true`, invoke `viewModel.refreshAllData { ... }` and `portalViewModel.refreshAllData { ... }`.

3. **Touch Slop Calibration (`SwipeableTabContainer.kt`)**:
   - Calibrate `touchSlop = with(density) { 8.dp.toPx() }` to ensure zero lag or dead zones during edge-to-edge tab gestures while filtering out vertical drag noise.

---

### 2. Web Client Gesture Isolation (`web/`)
1. **CSS Viewport Isolation (`web/css/mobile-shell.css`)**:
   - Scope `overscroll-behavior-y: contain !important; overscroll-behavior-x: none !important;` inside `@media (max-width: 1023px)` on `html, body, main.rx-main-shell` to suppress mobile browser pull-to-refresh spinners and edge swipe navigation.
   - Enforce `touch-action: pan-y !important;` on `.rx-tab-panel`.
2. **Directional Dominance (`web/js/swipeTabController.js`)**:
   - Calibrate minimum horizontal threshold to `8px` and directional dominance slope to `1.35x` (`absX > absY * 1.35`) before claiming horizontal swipe and invoking `e.preventDefault()`.

---

### 3. Versioning & Parity
- `android/app/build.gradle.kts`: `versionName = "2.3.16"`, `versionCode = 168`
- `server.py`: `SERVER_VERSION = "2.3.16"`
- `web/sw.js`: `CACHE_NAME = 'rosterx-pwa-v2.3.16'`
- `web/user-portal.html`, `web/index.html`: bump version query strings to `2.3.16`

---

## 1. Task 1 — Full-Fitted App Logo & Zero-Cropping Asset Overhaul (Android Splash, Launcher, Package Installer & Web PWA)
Eliminate all side wing and top/bottom cropping across Android and Web:

1. **Root Problem Analysis**:
   - Master logo content bounding box was `(64, 58, 961, 973)` occupying 88% width and 89% height.
   - Android adaptive icon safe keyline circle is only 66% (radius ~340px in 1024x1024), causing Android splash screen, package installer, and launcher squircles to slice off ~110px of the horizontal wings.

2. **Android 12+ Splash / Loading Screen (Seamless Floating Logo)**:
   - Create `@drawable/ic_rosterx_splash_logo`: Transparent background with full hexagonal badge and complete horizontal wings scaled safely within 325px radius from center.
   - Update `Theme.RosterX.Starting` in `values/themes.xml` and `values-night/themes.xml`:
     `<item name="windowSplashScreenAnimatedIcon">@drawable/ic_rosterx_splash_logo</item>`
   - Removes the awkward clipped white squircle box, rendering the floating logo cleanly on `#EEF2F6` (light) and `#141923` (dark).

3. **Android Launcher Icons & Package Installer (`ic_launcher`, `ic_launcher_round`)**:
   - Master `ic_rosterx_logo.png`: Rescale graphic so extreme points (left wing x=188, right wing x=836, top vertex y=188, bottom vertex y=836) sit well inside the 330px safe radius.
   - In `mipmap-anydpi-v26/ic_launcher.xml` and `ic_launcher_round.xml`, update foreground to use the safe-padded logo on `@color/ic_launcher_background_color` (`#FFFFFF`).
   - Regenerate all mipmap densities (`mdpi`, `hdpi`, `xhdpi`, `xxhdpi`, `xxxhdpi`) for `ic_launcher.png` and `ic_launcher_round.png` with clean white background and the complete logo centered inside, so the Android Package Installer ("Do you want to update this app?") displays the full logo with zero clipping!

4. **Web UI & PWA Shortcut Maskable Icon (`web/assets/logo.png`, `logo-maskable.png`)**:
   - Regenerate `web/assets/logo.png` (transparent background, perfectly proportioned with safe margin).
   - Generate `web/assets/logo-maskable.png` (pure white `#FFFFFF` background with the logo positioned within the W3C 65% safe circle).
   - In `web/manifest.webmanifest`, specify:
     - `src: "/assets/logo.png"`, `purpose: "any"`
     - `src: "/assets/logo-maskable.png"`, `purpose: "maskable"`
   - Result: Home screen Chrome shortcuts mask smoothly into squircles without clipping wing tips!

---

## 2. Task 2 — Chrome Shortcut / Mobile Web Back-Stack & Exit Guard (`manifest.webmanifest`, `login.html`, `userPortal.js`)
Fix the issue where pressing the back button on the Dashboard logs the user out instead of closing/minimizing the application:

1. **Manifest Start URL (`web/manifest.webmanifest`)**:
   - Change `"start_url": "/login.html"` to `"start_url": "/"`.
   - Ensures installed Chrome shortcuts / PWAs launch directly into the portal when authenticated, without having `/login.html` trapped at the bottom of the browser history stack.

2. **Login BFCache & Session Guard (`web/login.html`)**:
   - Add a `pageshow` listener to intercept back-navigation restores:
     ```javascript
     window.addEventListener('pageshow', function (event) {
       var token = localStorage.getItem('rosterx_auth_token');
       var raw = localStorage.getItem('rosterx_auth_user');
       if (token && raw) {
         try {
           var u = JSON.parse(raw);
           if (u && u.role === 'admin') window.location.replace('index.html');
           else window.location.replace('user-portal.html');
         } catch (_) {}
       }
     });
     ```
   - Never wipe session tokens on accidental back navigation to `login.html`.

3. **Dashboard Double-Back Exit Handling (`web/js/userPortal.js`)**:
   - Implement Android-style double-back press exit guard:
     - When user is at Dashboard root (`this.activeTab === 'dashboard'`, no open `<dialog>`, no active subview):
       - If user presses back for the first time within 2.5 seconds:
         - Show Neumorphic toast: *"Press back again to exit"*.
         - Record `this.lastBackPressTime = Date.now()`.
         - Re-push root state (`history.pushState({ scope: 'rosterx_root', tab: 'dashboard' }, '')`).
       - If user presses back again within 2500ms:
         - Allow default pop or in standalone mode call `window.close()` / let Android minimize the window!
         - **NEVER** clear `localStorage` auth tokens! User stays logged in permanently until they explicitly click the "Sign Out" button in Profile & Settings.

---

## 3. Task 3 — Background Scroll Lock & Backdrop Focus Lockdown (`pwaHelper.js`, `mobile-shell.css`)
Fix the issue where clicking outside or scrolling inside a floating dialog scrolls and focuses the underlying main screen:

1. **Scroll-Lock Hook in Dialog Manager (`web/js/pwaHelper.js`)**:
   - In `openNeumorphicDialog(dialogEl)`:
     - Add `document.documentElement.classList.add('rx-modal-open')` and `document.body.classList.add('rx-modal-open')`.
   - In `closeNeumorphicDialog(dialogEl)` and dialog event listeners (`'close'`, `'cancel'`):
     - Remove `rx-modal-open` class when no other dialogs are open:
       ```javascript
       if (!document.querySelector('dialog.rx-modal[open]')) {
         document.documentElement.classList.remove('rx-modal-open');
         document.body.classList.remove('rx-modal-open');
       }
       ```

2. **CSS Lockdown (`web/css/mobile-shell.css`)**:
   ```css
   html.rx-modal-open,
   body.rx-modal-open {
     overflow: hidden !important;
     overscroll-behavior: none !important;
     touch-action: none !important;
     height: 100% !important;
   }
   body.rx-modal-open main.rx-main-shell {
     pointer-events: none !important; /* Prevents clicks/touches from focusing main screen */
     user-select: none !important;
   }
   dialog.rx-modal {
     pointer-events: auto !important; /* Dialog card remains fully interactive */
     overscroll-behavior: contain !important;
   }
   .rx-modal-scroll {
     overscroll-behavior: contain !important; /* Prevents scroll chaining to parent */
     -webkit-overflow-scrolling: touch;
   }
   ```

3. **Touchmove Prevention on Backdrop**:
   - In `wireDialogLightDismiss`, add `touchmove` listener on dialog backdrop: if touch occurs on `dialogEl` outside the card rect, call `event.preventDefault()` to kill background scroll bleed.

---

## 4. Task 4 — Target Calendar Categorization & Device Calendars Integration (`calendarSync.js`, Android `ShiftCalendarExporter.kt`)
Properly categorize Corporate vs Personal accounts and support secondary calendars:

1. **Corporate Account Relabeling (`web/js/calendarSync.js`)**:
   - Change `Personal Account (${userEmail})` to:
     `🏢 Corporate / Work Calendar (${userEmail})`.
   - Clearly delineate that the registered profile email is the corporate domain account.

2. **Android Calendar Upload (`ShiftCalendarExporter.kt` & `EngineerPortalViewModel.kt`)**:
   - In Android app, when querying device calendars via `CalendarContract.Calendars`:
     - Collect available calendars: `id`, `displayName`, `accountName`, `accountType`, `isPrimary`, `color`.
     - Dispatch to `POST /api/users/calendar-accounts` via `RosterApiService` so MongoDB Atlas stores the user's available device calendars.

3. **Multi-Calendar Dropdown in Web (`web/js/calendarSync.js`)**:
   - Populate `TARGET CALENDAR` dropdown with:
     - `🏢 Corporate / Work Calendar (${userEmail})`
     - Discovered Google accounts from Atlas (`user.calendar_accounts`):
       - `📅 Google Calendar (${cal.account_name})`
       - `🗓️ Secondary: ${cal.name} (${cal.account_name})`
     - `🍎 Apple Calendar / iOS (.ics)`
     - `➕ Add Personal / Secondary Calendar...` (allows typing a custom calendar email/name if not synced from Android yet).
   - Persist selected target and target_type via `POST /api/users/calendar-sync`.

4. **Automatic Bidirectional Month Sync Status Bridging (`CalendarSyncDialog.kt`, `calendarSync.js`)**:
   - **The Missing Link**: Currently Android detects 30 synced events in its local device database, but only uploads this status to Atlas when the user taps the sync button. If already synced on the phone, Atlas never knew about it!
   - **Auto-Sync on Discovery (`CalendarSyncDialog.kt`)**: In `refreshCalendars()` or dialog init, if `existingSyncedCount > 0`, immediately call:
     ```kotlin
     val chosenAccount = found.firstOrNull { it.id == chosenId }?.let {
         if (it.displayName != it.accountName) "${it.displayName} (${it.accountName})" else it.accountName
     } ?: "Google Calendar"
     onSyncRecorded?.invoke(monthKey, existingSyncedCount, chosenAccount)
     ```
   - **Instant Web State**: Web immediately gets `{ synced: true, count: 30, target: "..." }` and renders `Synced (30)`, `Re-Sync 30 Days`, and `Clear Month` in 100% lockstep with Android!
   - **Bidirectional Clear**: Clicking `Clear Month` in Web unsets sync in Atlas; next time Android checks or opens, it cleans up device events. Clicking `Clear Month` in Android clears device events AND dispatches `POST /api/users/calendar-sync/clear`.

---

## 5. Task 5 — Smart Home Screen Widget Discovery & Pinning Engine (`RosterXAppWidgetProvider.kt`, `EngineerPortalScreen.kt`)
Prompt users about the Android Home Screen Widget, allow 1-tap pinning via native Android APIs, and automatically ignore users who already have it:

1. **Active Widget Detection**:
   - Use `AppWidgetManager.getInstance(context).getAppWidgetIds(ComponentName(context, RosterXAppWidgetProvider::class.java))`.
   - If `widgetIds.isNotEmpty()`: **User already has the widget on their home screen!** Automatically suppress all widget banners and prompts.

2. **Smart Cadence Banner on Dashboard (`EngineerPortalScreen.kt`)**:
   - Show a sleek Neumorphic prompt card on Dashboard:
     - Icon: `⚡` or `📱` (App Widget illustration).
     - Title: *"Glance at Shifts Instantly!"*
     - Subtitle: *"Add the RosterX widget to your Home Screen for live duty updates and rest day countdowns."*
     - Actions:
       - `[ 📌 Add Widget ]`: Calls `AppWidgetManager.requestPinAppWidget()`.
       - `[ ✕ ]`: Dismisses the banner and records timestamp in `SharedPreferences` to sleep for 7 days.
   - Cadence: Appears on every 4th or 5th app session (only if `widgetIds.isEmpty()` and sleep timer expired).

3. **1-Tap Pinning API (`AppWidgetManager.requestPinAppWidget`)**:
   - When the user taps `[ Add Widget ]`:
     - On Android 8.0+ (API 26+), call:
       ```kotlin
       val appWidgetManager = AppWidgetManager.getInstance(context)
       val provider = ComponentName(context, RosterXAppWidgetProvider::class.java)
       if (appWidgetManager.isRequestPinAppWidgetSupported) {
           appWidgetManager.requestPinAppWidget(provider, null, null)
       }
       ```
     - Triggers Android's official system prompt: *"Add to Home screen? Touch and hold the widget or tap Add to place it on your home screen."*
     - Once added by the user, `widgetIds` becomes non-empty and the banner vanishes forever!

4. **Permanent Option in Profile & Settings**:
   - In `ProfileSettingsScreen.kt`, add a row under Preferences / Quick Access:
     - Row: *"Home Screen Widget"*
     - Badge/Status: Shows *"Active"* with a green dot if `widgetIds.isNotEmpty()`, or `[ Add to Home Screen ]` button if not yet pinned.

---

## 6. Task 6 — Unified Edit Profile 1:1 Android Parity in Web (`userPortal.js`)
Add all missing Android options (Company Join Date & Security Password Change section) to Web's Edit Profile dialog:

1. **Missing Fields Added to Form (`openEditProfileDialog`)**:
   - **Company Join Date (`join_date`)**:
     - Dual-column row alongside `BIRTHDAY`: `[ JOIN DATE ]` & `[ BIRTHDAY ]` with native date pickers.
     - Pre-fills with existing `eng.joining_date` or `eng.join_date` (calculates work tenure & anniversary).
   - **Security & Password Section**:
     - Section Header: `SECURITY & PASSWORD` with muted helper `(Leave blank to keep current)`.
     - `Current Password`: `<input type="password">` with show/hide eye toggle button.
     - Dual-column row:
       - `New Password`: Min 6 characters, with show/hide eye toggle.
       - `Confirm Password`: Must match New Password, with show/hide eye toggle.

2. **Client Validation & Submission (`POST /api/users/profile`)**:
   - If any password field is touched:
     - Enforce `current_password.isNotBlank()`.
     - Enforce `new_password.length >= 6`.
     - Enforce `new_password === confirm_password`.
   - On submit, dispatch:
     ```json
     {
       "username": eng.username,
       "name": name,
       "full_name": name,
       "phone": phone,
       "email": email,
       "birthday": birthday,
       "join_date": joinDate,
       "current_password": currentPassword,
       "new_password": newPassword
     }
     ```
   - On success: Toast *"Profile & security updated successfully!"*, close dialog, and refresh profile view.

---

## 7. Task 7 — Ultra-Modern Glassmorphic Animated AI Assistant Dialog (`userPortal.js`, `mobile-shell.css`)
Replace the broken, bottom-stuck Neumorphic AI modal with a responsive, smoothly animated centered Glassmorphic dialog:

1. **Root Problem Analysis**:
   - In `mobile-shell.css`, `@media (max-width: 640px)` had `#rxAiChatDialog { margin: auto auto 0 auto; height: 85dvh; }` which docked it like a broken bottom sheet colliding with the bottom navigation bar (`rxMobileNav`).
   - Closing lacked animation, and closing state clashed with history `popstate`, freezing the Dashboard background.
   - Sunken Neumorphic wells wasted vertical space and looked heavy.

2. **Centered Floating Glassmorphic Dialog (`mobile-shell.css`)**:
   - Both Mobile & Desktop: Centered floating card with margins all around:
     ```css
     #rxAiChatDialog {
       margin: auto;
       width: min(92vw, 540px);
       height: min(82dvh, 720px);
       max-height: min(82dvh, 720px);
       border-radius: 28px;
       border: 1px solid rgba(255, 255, 255, 0.45);
       background: rgba(255, 255, 255, 0.78);
       backdrop-filter: blur(24px) saturate(180%);
       -webkit-backdrop-filter: blur(24px) saturate(180%);
       box-shadow: 0 24px 48px -12px rgba(15, 23, 42, 0.22);
       overflow: hidden;
     }
     html.dark #rxAiChatDialog {
       border: 1px solid rgba(255, 255, 255, 0.12);
       background: rgba(15, 23, 42, 0.82);
       box-shadow: 0 24px 48px -12px rgba(0, 0, 0, 0.55);
     }
     ```
   - Eliminates the bottom dock entirely. Floats above the bottom navigation bar with comfortable clearance!

3. **Scale & Fade Spring Open/Close Animations**:
   - CSS Transitions:
     ```css
     #rxAiChatDialog {
       opacity: 0;
       transform: scale(0.94);
       transition: opacity 240ms cubic-bezier(0.16, 1, 0.3, 1), transform 240ms cubic-bezier(0.16, 1, 0.3, 1);
     }
     #rxAiChatDialog.is-visible {
       opacity: 1;
       transform: scale(1);
     }
     #rxAiChatDialog.is-closing {
       opacity: 0;
       transform: scale(0.94);
       transition: opacity 180ms ease-in, transform 180ms ease-in;
     }
     ```
   - Wire clean animation lifecycle in `openAiAssistantDialog`: trigger `is-visible` via `requestAnimationFrame`, and on close, add `is-closing` before executing `dlg.close()`, releasing `rx-modal-open` cleanly so Dashboard is never stuck!

4. **Glassmorphic Chat UI Components**:
   - Header: 3D Bot icon with glowing green status pulse, title, subtitle, trash & close glass pills.
   - Quick chips: Frosted glass chips with subtle border and touch glow.
   - Messages area:
     - User message: Vibrant gradient bubble (`linear-gradient(135deg, #174EAC, #2563EB)`) with white text.
     - AI message: Frosted glass card with 3D bot avatar, code highlights, and colorful shift badges (`FS`, `NS`, `SG`, `OFF`).
   - Input bar: Floating translucent glass pill with glowing paper-plane send button.

---

## 8. Task 8 — Google Password Manager Domain Unification & Password Update Sync (`AndroidManifest.xml`, `GooglePasswordManagerHelper.kt`, `ProfileComponents.kt`, `LoginScreen.kt`)
Unify credentials under `rosterx.online`, eliminate `rosterx.com` and generic green robot, and prompt GPM to update passwords immediately on change or login:

1. **Digital Asset Links & Domain Unification (`AndroidManifest.xml`, `strings.xml`)**:
   - In `android/app/src/main/res/values/strings.xml`, add `asset_statements`:
     ```xml
     <string name="asset_statements" translatable="false">
     [{\n
       \"include\": \"https://rosterx.online/.well-known/assetlinks.json\"\n
     },{\n
       \"include\": \"https://api.rosterx.online/.well-known/assetlinks.json\"\n
     }]
     </string>
     ```
   - In `AndroidManifest.xml` inside `<application>`, add:
     ```xml
     <meta-data
         android:name="asset_statements"
         android:resource="@string/asset_statements" />
     ```
   - **Result**: Google Password Manager recognizes `com.rosterx` as belonging to `rosterx.online`. It stops saving under `rosterx.com`, replaces the green robot with the official RosterX logo, and shares credentials seamlessly with Chrome Web!

2. **Immediate GPM Update Prompt on Password Change (`ProfileComponents.kt`, `EngineerPortalViewModel.kt`)**:
   - In `UnifiedEditProfileDialog.kt`, when user changes password and the API call returns success:
     - Clear `last_saved_hash_<username>` in `GooglePasswordManagerHelper`.
     - Immediately invoke `googlePasswordManager.saveCredentials(activity, username, newPassword)`.
     - Google Password Manager detects the updated password for `username` under `rosterx.online` and prompts the native bottom sheet: *"Update password in Google Password Manager?"*.
     - Call `autofillManager.commit()` to sync OS autofill.

3. **Login Screen Auto-Update (`LoginScreen.kt`, `GooglePasswordManagerHelper.kt`)**:
   - On successful login, if the password does not match `last_saved_hash`:
     - Invalidate old hash and launch `saveCredentials(activity, saveUser, creds.second)`.
     - Prompts the user to update their saved password in GPM if it was changed on Web or elsewhere!
   - Saves under canonical username (`eng.username`).

---

## 9. Task 9 — Verification, Version Bump & Mandatory Push
1. Bump version to **`v2.3.1` (Build 153)**:
   - `android/app/build.gradle.kts`: `versionName = "2.3.1"`, `versionCode = 153`
   - `server.py`: `SERVER_VERSION = "2.3.1"` (synced with Dasan)
2. Safe build & ADB install:
   - `cmd /c "cd android && safe_build.bat assembleDebug"`
   - `adb install -r android/app/build/outputs/apk/debug/app-debug.apk`
3. Verify on device & Web:
   - Verify opening Chrome shortcut and pressing back on Dashboard shows toast *"Press back again to exit"* and does not log out or open `login.html`.
   - Verify opening floating modals (AI Assistant, Calendar Sync, Celebrations) completely locks background scroll so the main screen never scrolls or takes focus.
   - Verify Calendar Sync dropdown lists Corporate Account properly, displays synced personal/secondary Google accounts, and allows manual secondary calendar entry.
   - Verify GPM unifies credentials under `rosterx.online` with the official logo, and prompts immediate updates on password change.
   - Verify AI Assistant dialog opens as a centered floating Glassmorphic card with smooth spring animations.
   - Verify Edit Profile includes Join Date and Security Password change fields.
4. Commit with semantic message (`feat(ui): full parity suite, zero-cropping logo, glassmorphic AI & GPM sync v2.3.1 (Build 153)`), update `AGENT_HANDOFF.md`, and **immediately run `git push origin main`**!
