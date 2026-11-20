# 📋 Execution Plan for Vijayan (UI Specialist — Android & Web)

**Status:** `COMPLETED BY VIJAYAN` (Sprint v2.3.0 - Build 152 Mobile Web Back-Stack Exit Guard, Background Scroll Lock, Device Calendar Sync & Smart Home Screen Widget Discovery)

---

## 🔐 Mandatory Production Rule from Gafoor (System Architect 🕶️)
> **Public GitHub Repo + Auto-Deploying Render Production Server:**
> - **ZERO hardcoded personal names, usernames, emails, phone numbers, passwords, or secrets** anywhere in HTML, CSS, JS, Kotlin, comments, or placeholders.
> - **Mandatory `git push origin main`** immediately after verification so Render (`https://api.rosterx.online`) deploys immediately!

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

## 7. Task 7 — Verification, Version Bump & Mandatory Push
1. Bump version to **`v2.3.0` (Build 152)**:
   - `android/app/build.gradle.kts`: `versionName = "2.3.0"`, `versionCode = 152`
   - `server.py`: `SERVER_VERSION = "2.3.0"` (synced with Dasan)
2. Safe build & ADB install:
   - `cmd /c "cd android && safe_build.bat assembleDebug"`
   - `adb install -r android/app/build/outputs/apk/debug/app-debug.apk`
3. Verify on device & Web:
   - Verify opening Chrome shortcut and pressing back on Dashboard shows toast *"Press back again to exit"* and does not log out or open `login.html`.
   - Verify opening floating modals (AI Assistant, Calendar Sync, Celebrations) completely locks background scroll so the main screen never scrolls or takes focus.
   - Verify Calendar Sync dropdown lists Corporate Account properly, displays synced personal/secondary Google accounts, and allows manual secondary calendar entry.
4. Commit with semantic message (`feat(ui): mobile back-stack exit guard, background scroll lock & calendar accounts discovery v2.3.0 (Build 152)`), update `AGENT_HANDOFF.md`, and **immediately run `git push origin main`**!
