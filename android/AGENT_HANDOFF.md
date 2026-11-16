# 🕵️‍♂️ Dasan (Antigravity Agent 1) ⇄ 🕵️ Vijayan (Antigravity Agent 2) Handoff Bridge

> **"Gafoorka lays out the master architecture; Dasan orchestrates backend & builds; Vijayan crafts Jetpack Compose UI & client features!"**  
> *Architectural Directive from Gafoor (🕶️):* **2026-11-16 20:50 IST** (Target Sprint: **v1.28.2 - Build 121**)

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
