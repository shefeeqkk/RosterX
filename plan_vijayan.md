# 📋 Execution Plan for Vijayan (UI, Jetpack Compose & Production Hygiene)

**Status:** `READY FOR VIJAYAN` (Sprint v1.33.0 - Build 137 Broadcast Title `@` Mention Popup + 100% Cloudflared Removal + Zero-PII DB-Driven Roster Metadata)

---

## 🔐 Mandatory Production Rule from Gafoor (System Architect 🕶️)
> **Public GitHub Repo + Auto-Deploying Render Production Server:**
> - **ZERO hardcoded personal names, usernames, emails, phone numbers, passwords, or secrets** anywhere in Kotlin code, XML layouts, comments, UI placeholders, or AI prompt examples! Always use backend DTO fields or synthetic placeholders (`user@example.com`, `Team Member`, `member_username`).
> - **Mandatory `git push origin main`** immediately after verified commits so Render (`https://api.rosterx.online`) stays synced with your ADB builds.

---

## 1. Task 1 — Broadcast Title `@` Mention Popup + Full Resolution (`NotificationsTab.kt`, `ProfileComponents.kt`)
1. **Upgrade `Notification Title *` (`NotificationsTab.kt`)**:
   - Replace `var notificationTitle by rememberSaveable { mutableStateOf("") }` with `var titleTextFieldValue by remember { mutableStateOf(TextFieldValue("")) }` (`val notificationTitle = titleTextFieldValue.text`).
   - Replace plain `NeumorphicFormInput` for `Notification Title *` with a compact single-line Neumorphic sunken `BasicTextField` (keeping leading `Icons.Default.Title` icon, `"Enter broadcast title..."` placeholder aligned to cursor, `singleLine = true`).
   - Extract a reusable, focus-isolated, bounds-safe `@Composable fun CompactCursorMentionPopup(...)` shared by **both** Title and Message Body (`@name`, `@all`, and **only `@username`** for engineers; `cursorIndex.coerceIn(0, layout.layoutInput.text.length)` bounds guard + `Modifier.onFocusChanged` focus isolation).
   - Ensure template selection populates `titleTextFieldValue` (cursor at end) and post-send clear resets `titleTextFieldValue = TextFieldValue("")`.
2. **Live Personalized Recipient Preview Card (`NotificationsTab.kt`)**:
   - Extract shared helper `resolveBroadcastMentions(rawText, engName, engUsername, activeEngineers)` and render `resolvedPreviewTitle` (bold) right above `resolvedPreviewMessage` whenever `notificationTitle.isNotBlank()`.
3. **Notification Drawer Title Resolution (`ProfileComponents.kt`)**:
   - In `QuickNotificationsBottomSheet`, resolve `@name` / `{name}` / `{their name}` (logged-in engineer's Full Name), `@all` / `@everyone` (`@Everyone`), and `@<username>` (`@<Full Name>`) inside `notif.title` before rendering — matching `notif.rawMessage`.

---

## 2. Task 2 — 100% Zero-Trace Cloudflared Removal in Android
1. **`LoginScreen.kt` (line ~570)**:
   - Replace placeholder `"e.g. https://xxx.trycloudflare.com/api/"` with `"e.g. https://api.rosterx.online/api/"`.
2. **`UserSessionManager.kt` (lines ~194-195)**:
   - Remove the `trycloudflare.com` reference completely (keep only clean `onrender.com` migration if needed, zero `cloudflared`/`trycloudflare` strings anywhere in `android/`).

---

## 3. Task 3 — Zero-PII & DB-Driven Roster Metadata in Android
1. **Model & DTO Contract (`Engineer.kt`, `RosterApiService.kt`, Mappers)**:
   - Add optional fields to `Engineer` & `EngineerDto`:
     - `includeInRoster: Boolean? = null` (`@SerializedName("include_in_roster")`)
     - `displayOrder: Int? = null` (`@SerializedName("display_order")`)
     - `swapCode: String? = null` (`@SerializedName("swap_code")`)
2. **Remove ALL Hardcoded Personal Usernames & Names**:
   - **`Engineer.kt` (lines ~39-42)** & **`EngineerPortalViewModel.kt` (line ~1161)**:
     - Remove the hardcoded personal username `setOf(...)` allowlist!
     - Drive `isManagementAccount` cleanly via:
       1. If `includeInRoster == true` → `false` (operational team member on roster).
       2. If `includeInRoster == false` or `role == EngineerRole.MANAGER` → `true`.
       3. Otherwise treat as management account ONLY for generic system accounts (`admin`, `manager`, `sysadmin`, `system_admin`, `administrator`, or `role == ADMIN` whose username/name contains `admin`/`system`).
   - **`EngineerPortalScreen.kt` (`getEngineerOrderWeight`, lines ~221-251)**:
     - Delete the hardcoded 10-name `when` map!
     - Return `eng.displayOrder ?: extractNumericId(eng.id) ?: Int.MAX_VALUE` (preserving alphabetical tie-break in `sortEngineersByWeekOffs`).
   - **`GeminiRosterService.kt` (lines ~76-78)**:
     - Replace hardcoded real name/username in AI prompt JSON example with generic `"Team Member"` / `"member_username"`.
   - **`RosterXAppWidgetProvider.kt` (line ~189)** & **`item_widget_shift_pair.xml` (line ~72)**:
     - Replace real names in comment and XML `android:text` placeholder with generic `"Alex Morgan" -> "Alex"` / `"Member"`.

---

## 4. Task 4 — Zero-Knowledge Admin Team Management + Fix Weekly Offs Shadow Overlap (`ManageTeamTab.kt`)
1. **Remove Password Input from BOTH Edit Account & Add Member (`ManageTeamTab.kt`)**:
   - Security requirement: Admins/Managers must NEVER be able to change or set passwords for users or managers from Team Management!
   - Delete `passwordInput`, `passwordVisible`, the `"Password *"` / `"New password"` `NeumorphicFormInput` (lines ~365–384), and the `if (target == null && passwordInput.isBlank())` check (lines ~769–772).
   - Always pass `password = null` when constructing `Engineer(...)` on save.
   - Make `Phone` cleanly full-width on Row 3 (`Modifier.fillMaxWidth()`) so there is no awkward empty half-column.
   - On Add New Member (`editingEngineer == null`), show a sleek 1-line muted info note below the form fields: `"🔒 New members set their own password privately via Forgot password? (Email OTP)."`.
2. **Fix Weekly Offs Selected Chip Shadow Overlap (`ManageTeamTab.kt`, lines ~521–575)**:
   - **Root Cause**: Inside the recessed Weekly Offs rail (`spacedBy(3.dp)`), `.neumorphicRaised(elevation = ElevationRaisedSubtle)` paints a bright white top-left outer shadow that bleeds directly onto the right edge of the previous selected chip (e.g., `Thu`'s white glow washing over `Wed`!).
   - **Fix**:
     - Increase spacing inside the recessed rail to `Arrangement.spacedBy(4.dp)` with `.padding(4.dp)`.
     - For **selected** day chips: Replace `.neumorphicRaised(...)` with a crisp, non-bleeding elevated pill — `.shadow(elevation = 2.dp, shape = RoundedCornerShape(9.dp), ambientColor = Color.Black.copy(alpha = 0.22f), spotColor = Color.Black.copy(alpha = 0.28f))` + `.clip(RoundedCornerShape(9.dp))` + `.background(NeumorphicTokens.AccentSalmon)` — **ZERO white outer highlight bleeding onto neighboring chips!**
     - For **unselected** day chips: Clean recessed/flat surface inside the rail (`RoundedCornerShape(9.dp)`, no oversized outer shadows).
     - Inspect the **Role and access** and **Employment status** rails in the same dialog and ensure their selected chips also never cast white shadow bleed onto adjacent segments.

---

## 5. Task 5 — Profile Birthday Countdown Dynamic Label (`ProfileComponents.kt`)
- In `TenureAndBirthdayRow` (`ProfileComponents.kt`, lines ~457–495):
  - Replace hardcoded `"BIRTHDAY IN"` title with dynamic label based on `daysUntilBday`:
    - **`daysUntilBday == 0`**: Title `"BIRTHDAY"` → Value `"Today! 🎉"` (gold `#F59E0B`)
    - **`daysUntilBday == 1`**: Title `"BIRTHDAY"` → Value `"Tomorrow! 🎂"` (accent/warm highlight)
    - **`daysUntilBday != null && daysUntilBday >= 2`**: Title `"BIRTHDAY IN"` → Value `"$daysUntilBday days"`
    - **`daysUntilBday == null`**: Title `"BIRTHDAY"` → Value `"--"`
  - Eliminates awkward `"BIRTHDAY IN TODAY! 🎉"` / `"BIRTHDAY IN TOMORROW!"` grammar.

---

## 6. Post-Build Activities (Mandatory)
1. Bump version in `android/app/build.gradle.kts` (`versionName = "1.33.0"`, `versionCode = 137`) and verify `server.py` (`SERVER_VERSION = "1.33.0"`).
2. Run safe build:
   - `cmd /c "cd android && safe_build.bat assembleDebug"`
3. Install via ADB:
   - `adb install -r android/app/build/outputs/apk/debug/app-debug.apk`
4. Verify on device:
   - Test `@` mention popup in Broadcast Title (cursor-anchored, compact, `@username` only for engineers, Live Preview resolves Title + Body).
   - Open Admin Team Management -> Edit & Add Account: verify password field is completely gone, `Phone` layout is balanced, and selecting two adjacent Weekly Offs (e.g. `Wed` + `Thu`) has **zero white shadow overlap** between chips!
   - Verify Roster Grid & Portal load cleanly with dynamic DB-driven ordering & management filtering.
   - Verify `git grep -i "trycloudflare\|cloudflared"` returns 0 matches in `android/`.
5. Commit with semantic message, update `AGENT_HANDOFF.md`, and **immediately run `git push origin main`**!
