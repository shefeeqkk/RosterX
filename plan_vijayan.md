# 📋 Execution Plan for Vijayan (Android Platform Features & Jetpack Compose)

**Status:** `COMPLETED` (Sprint v1.34.0 - Build 139 Verified App Links + Launcher Shortcuts + Native Splash & Predictive Back + 1-Tap Calendar Export + In-App Update Banner built, committed & pushed)

---

## 🔐 Mandatory Production Rule from Gafoor (System Architect 🕶️)
> **Public GitHub Repo + Auto-Deploying Render Production Server:**
> - **ZERO hardcoded personal names, usernames, emails, phone numbers, passwords, or secrets** anywhere in Kotlin, XML, comments, placeholders, or tests.
> - **Mandatory `git push origin main`** immediately after build, ADB verify & commit so Render stays synced.

---

## 1. Task 1 — Android Verified App Links ("Open by default") & Deep-Link Router
Turns Settings → RosterX → Open by default from `0 verified links` into **`api.rosterx.online` (verified)** and routes links straight to the matching in-app destination!

### Implementation (`AndroidManifest.xml`, `MainActivity.kt`, `AdminMainScreen.kt`, `EngineerPortalScreen.kt`):
1. **`AndroidManifest.xml`**:
   - Set `android:launchMode="singleTop"` on `MainActivity` so deep links reuse the running task cleanly without stacking duplicate activities.
   - Add verified App Link `<intent-filter android:autoVerify="true">` covering root `/` and `/open` (WITHOUT intercepting raw JSON `/api/*` endpoints!):
     ```xml
     <intent-filter android:autoVerify="true">
         <action android:name="android.intent.action.VIEW" />
         <category android:name="android.intent.category.DEFAULT" />
         <category android:name="android.intent.category.BROWSABLE" />
         <data android:scheme="https" android:host="api.rosterx.online" android:path="/" />
         <data android:scheme="https" android:host="api.rosterx.online" android:pathPrefix="/open" />
     </intent-filter>
     ```
2. **Unified Deep-Link Parser (`MainActivity.kt`)**:
   - Define a clean sealed/enum route `AppDeepLinkTarget`: `ROSTER`, `NOTIFICATIONS`, `CREDENTIALS`, `PROFILE`, `HOME`.
   - Parse `intent?.data` (and shortcut extra `"deep_link_target"`) in both `onCreate()` and `onNewIntent()`:
     - `/open/roster` → `ROSTER`
     - `/open/notifications` → `NOTIFICATIONS`
     - `/open/credentials` → `CREDENTIALS`
     - `/open/profile` → `PROFILE`
     - `/` or unknown → `HOME`
   - Expose `pendingDeepLink: AppDeepLinkTarget?` as Compose state into `AdminMainScreen` and `EngineerPortalScreen`, consume it once the user is authenticated (switch to the matching tab or open the Notifications drawer / Credential Tracker screen), and clear via `onConsumeDeepLink()`.

---

## 2. Task 2 — Launcher Long-Press App Shortcuts (`res/xml/shortcuts.xml` + Dynamic Role Shortcuts)
1. **Static Shortcuts (`android/app/src/main/res/xml/shortcuts.xml`)** linked via `<meta-data android:name="android.app.shortcuts" android:resource="@xml/shortcuts" />` inside `MainActivity`:
   - **Today's Roster** → opens `https://api.rosterx.online/open/roster`
   - **Notifications** → opens `https://api.rosterx.online/open/notifications`
   - **My Profile** → opens `https://api.rosterx.online/open/profile`
2. **Role-Aware Dynamic Shortcut (`ShortcutManagerCompat`)**:
   - When an **Admin** or **Manager** is logged in, dynamically publish a **Credential Tracker** launcher shortcut (`https://api.rosterx.online/open/credentials`).
   - On logout or Engineer/Apprentice login, remove the Credential Tracker dynamic shortcut so role boundaries stay clean.

---

## 3. Task 3 — Android 12+ Native Splash Screen & Predictive Back Gesture
1. **Dependency (`gradle/libs.versions.toml` & `app/build.gradle.kts`)**:
   - Add `androidx.core:core-splashscreen:1.0.1`.
2. **Splash Theme (`res/values/themes.xml`)**:
   - Create `Theme.RosterX.Starting` inheriting from `Theme.SplashScreen`:
     - `windowSplashScreenBackground`: matches RosterX Neumorphic surface tone
     - `windowSplashScreenAnimatedIcon`: `@mipmap/ic_launcher_round` (or adaptive icon)
     - `postSplashScreenTheme`: `@style/Theme.RosterX`
   - Apply `android:theme="@style/Theme.RosterX.Starting"` to `MainActivity` in `AndroidManifest.xml`.
   - Call `installSplashScreen()` right before `super.onCreate(savedInstanceState)` in `MainActivity.kt`.
3. **Predictive Back (`AndroidManifest.xml`)**:
   - Add `android:enableOnBackInvokedCallback="true"` on `<application>` so Android 14/15 smooth edge-swipe back previews work natively.

---

## 4. Task 4 — 1-Tap Export Monthly Shifts to Google / Device Calendar (`.ics` via `FileProvider`)
Allows engineers to export an entire month's working shifts directly into Google Calendar / Samsung Calendar in 1 tap with **ZERO runtime calendar permissions**!
1. **`FileProvider` Setup (`AndroidManifest.xml` & `res/xml/file_paths.xml`)**:
   - Declare `androidx.core.content.FileProvider` (`authorities="${applicationId}.fileprovider"`, `grantUriPermissions="true"`) exposing `<cache-path name="calendar_exports" path="calendar/" />`.
2. **RFC-5545 `.ics` Generator Utility (`util/ShiftCalendarExporter.kt`)**:
   - Given the engineer's name, month/year, and `Map<Int, String>` (day → shift code):
   - Skip non-working codes (`OFF`, `L`, `LEAVE`, `SW*`, blank).
   - Map RosterX shift codes to exact IST (`Asia/Kolkata`) start/end windows (e.g. `FS`: 06:00–14:30, `FG`: 07:30–16:00, `G`: 09:00–17:30, `SG`: 11:30–20:00, `SS`: 14:00–22:30, `NS`: 22:00–06:30 next day; handle midnight rollover cleanly!).
   - Include a 30-minute `VALARM` reminder per shift and stable `UID` (`rosterx-<year>-<month>-<day>-<username>@rosterx.online`) so re-exporting updates cleanly without creating duplicate events!
   - Write to `cacheDir/calendar/rosterx_shifts_<YYYY>_<MM>.ics`, obtain `FileProvider.getUriForFile(...)`, and launch `Intent.ACTION_VIEW` with `text/calendar` + `FLAG_GRANT_READ_URI_PERMISSION` (with graceful fallback to `Intent.ACTION_SEND` chooser if no calendar viewer directly accepts `.ics`).
3. **UI Entry Point (`EngineerPortalScreen.kt`)**:
   - Add a sleek compact Neumorphic **"Export to Calendar"** icon/pill button (`Icons.Default.EventAvailable`) in the engineer's Month Roster header (only when shifts exist for that month) with confirmation toast/snackbar.

---

## 5. Task 5 — In-App Update Banner (`/api/server-info` Version Check)
1. **DTO Update (`RosterApiService.kt`)**:
   - Add optional fields to `ServerInfoResponse`:
     - `@SerializedName("latest_android_version") val latestAndroidVersion: String? = null`
     - `@SerializedName("min_supported_version") val minSupportedVersion: String? = null`
     - `@SerializedName("apk_download_url") val apkDownloadUrl: String? = null`
2. **Semantic Version Comparator + Banner UI (`ProfileComponents.kt`, `AdminMainScreen.kt`, `EngineerPortalScreen.kt`)**:
   - Compare `BuildConfig.VERSION_NAME` against `serverInfo.latestAndroidVersion ?: serverInfo.version` numerically (`major.minor.patch`, never lexicographical string compare!).
   - When `remoteVersion > installedVersion` (and not dismissed for that session):
     - Display a sleek compact Neumorphic banner at the top of the home/dashboard view:
       - Title: `"Update available • v<remoteVersion>"`
       - Subtitle: `"Installed: v<BuildConfig.VERSION_NAME>"`
       - Primary pill button: **`Download`** (opens `apkDownloadUrl` via `Intent.ACTION_VIEW`) + subtle **`X`** dismiss button.

---

## 6. Post-Build Activities (Mandatory)
1. Bump version in `android/app/build.gradle.kts` (`versionName = "1.34.0"`, `versionCode = 139`) and confirm `server.py` (`SERVER_VERSION = "1.34.0"`).
2. Run safe build:
   - `cmd /c "cd android && safe_build.bat assembleDebug"`
3. Install via ADB:
   - `adb install -r android/app/build/outputs/apk/debug/app-debug.apk`
4. Automated On-Device Verification via ADB:
   - Verify App Link deep-link routing:
     - `adb shell am start -a android.intent.action.VIEW -d "https://api.rosterx.online/open/notifications" com.rosterx`
     - `adb shell pm get-app-links com.rosterx` (shows `api.rosterx.online` registered!)
   - Verify cold start splash screen + no crash.
   - Verify Launcher Shortcuts registered, Calendar Export `.ics` generation works, and Update Banner logic compares semantic versions cleanly.
5. Commit with semantic message, update `AGENT_HANDOFF.md`, and **immediately run `git push origin main`**!
