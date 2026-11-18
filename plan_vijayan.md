# 📋 Execution Plan for Vijayan (UI & Jetpack Compose)

**Status:** `READY FOR VIJAYAN` (Sprint v1.33.0 - Build 137 Broadcast Title `@` Mention Popup + Title Preview/Drawer Resolution + Mandatory Auto-Push Sync)

---

## 1. Architectural Mandates from Gafoor (System Architect 🕶️)

Vijaya! Nammude Broadcast Composer-il Message Body-ile cursor-anchored compact `@` mention popup super aayi work cheyyunnundu. User-nte puthiya demand:
**Broadcast `Notification Title *` field-ilum `@` mentioning venam (exact same compact cursor-anchored popup + full personalization parity)!**
Athupole nammude **new workspace rule (`GEMINI.md`)**: Post-build verification & commit kazhinjaal **mandatory `git push origin main`** run cheyyuka, so Render (`https://api.rosterx.online`) never lags behind your ADB-installed APK!

### 🎯 Key Directives for Vijayan:
1. **Upgrade `Notification Title *` to Cursor-Aware Mention Input (`NotificationsTab.kt`)**:
   - Replace `var notificationTitle by rememberSaveable { mutableStateOf("") }` with `var titleTextFieldValue by remember { mutableStateOf(TextFieldValue("")) }` (`val notificationTitle = titleTextFieldValue.text`).
   - Replace the plain `NeumorphicFormInput` for `Notification Title *` with a compact single-line Neumorphic sunken `BasicTextField` (preserving the leading `Icons.Default.Title` icon, `"Enter broadcast title..."` placeholder aligned to cursor, and `singleLine = true`).
   - Track `titleTextLayoutResult` via `onTextLayout = { titleTextLayoutResult = it }`, detect `@([a-zA-Z0-9_\-\.]*)$` before the Title cursor, and show the **exact same cursor-anchored compact `Popup`** directly below the Title cursor (`@name`, `@all`, and **only `@username`** for engineers)!
   - **DRY Refactor**: Extract a reusable `@Composable fun CompactCursorMentionPopup(...)` so Title and Message Body share one clean, crash-proof implementation (keeping the `cursorIndex.coerceIn(0, layout.layoutInput.text.length)` bounds guard from Build 134!).
   - Also make sure selecting a saved template updates `titleTextFieldValue = TextFieldValue(tpl.title.orEmpty(), TextRange(tpl.title.orEmpty().length))` and clearing after send resets `titleTextFieldValue = TextFieldValue("")`.
2. **Update Live Personalized Recipient Preview Card (`NotificationsTab.kt`)**:
   - Extract a shared helper `resolveBroadcastMentions(rawText, engName, engUsername, activeEngineers)` and compute `resolvedPreviewTitle` alongside `resolvedPreviewMessage`.
   - Display `resolvedPreviewTitle` in bold at the top of the Live Preview Card whenever `notificationTitle.isNotBlank()`, so Admins/Managers immediately see personalized Titles (e.g., `"Urgent for Shefeeq K K: @Everyone"`) before dispatching!
3. **Client-Side Title Mention Resolution in Notification Drawer (`ProfileComponents.kt`)**:
   - In `QuickNotificationsBottomSheet` (`ProfileComponents.kt` around line 1961):
     - Resolve `@name` / `{name}` / `{their name}` (to logged-in engineer's Full Name), `@all` / `@everyone` (to `@Everyone`), and `@<username>` (to `@<Full Name>` via `engineers` lookup) in `notif.title` before rendering — matching `notif.rawMessage`!
4. **Version Bump, Safe Build, ADB Verify & Mandatory Push**:
   - Bump `android/app/build.gradle.kts` (`versionName = "1.33.0"`, `versionCode = 137`) and `server.py` (`SERVER_VERSION = "1.33.0"` if not already bumped by Dasan).
   - Build via `cmd /c "cd android && safe_build.bat assembleDebug"`.
   - Install via `adb install -r android/app/build/outputs/apk/debug/app-debug.apk`.
   - Verify on device:
     1. Open Admin/Manager Broadcast tab -> tap `Notification Title *` -> type `@` -> verify compact floating popup opens directly below the Title cursor (`@name`, `@all`, `@username` only), tapping inserts `@tag ` cleanly without crash, and typing continues smoothly.
     2. Verify Live Preview Card updates personalized Title + Body simultaneously.
     3. Verify Notification Drawer resolves `@name`, `@all`, and `@username` inside received broadcast titles.
   - Commit with semantic message, update `AGENT_HANDOFF.md`, and **run `git push origin main`**!

---

## 2. Scope of Work for Vijayan

### 📁 Primary Files:
1. `android/app/src/main/java/com/rosterx/presentation/admin/tabs/NotificationsTab.kt`
2. `android/app/src/main/java/com/rosterx/presentation/components/ProfileComponents.kt`
3. `android/app/build.gradle.kts`

### 🛠️ Technical Notes:
- **Focus Isolation**: Ensure `isTitleFocused` / `isMessageFocused` (via `Modifier.onFocusChanged`) guards each field's mention popup so only the actively focused field shows its cursor popup — never two popups at once!
- **Bounds Safety**: Always clamp `val safeCursorIndex = cursorIndex.coerceIn(0, layout.layoutInput.text.length)` before calling `layout.getCursorRect(safeCursorIndex)` so rapid edits or template inserts can never throw `IndexOutOfBoundsException`.
- **Single-Line Horizontal Scroll**: Since Title is `singleLine = true`, `cursorRect.left` stays cleanly clamped via `PopupProperties(focusable = false, clippingEnabled = true)`.
- **Remember**: Run **`git push origin main`** right after committing so Render (`https://api.rosterx.online`) stays synced to `v1.33.0`!
