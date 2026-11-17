# 📋 Execution Plan for Vijayan (UI & Jetpack Compose)

**Status:** `READY FOR VIJAYAN` (Sprint v1.31.0 - Build 129)

---

## 1. Architectural Mandates from Gafoor (System Architect 🕶️)

Vijaya! User Broadcast Tab-il major modernisation request cheythittundu:
1. **Neumorphic & Compact Responsive Layout**:
   - Tab-nte mukalil Neumorphic Segmented Control vekkuka:
     `[ 📢 Compose Broadcast ]`  |  `[ 📜 Broadcast History (count) ]`
   - Ithumoolam editor-um history-yum separate aayi, ultra-compact, focused, and zero vertical clutter aayi maatum (landscape-il side-by-side responsive layout).
2. **Unified Interactive Editor with `@` Mentions**:
   - Message editor-il `@` type cheyyumbol inline autocomplete popup trigger aakanam:
     - `@name` (Icon `Person`: "Dynamic Recipient Name — Personalizes to each recipient's name")
     - `@all` (Icon `Groups`: "Alert entire operational team")
     - Specific teammates: Avatar + Full Name + `@username`.
   - **Quick Chips Toolbar Removed**: `{name}`, `@all`, `@ Mention...` chip toolbar poornamaayi remove cheyyuka!
   - **Subtle Helper Description**: Editor-nu thazhe subtle helper text add cheyyuka:
     `💡 Tip: Type @ to insert dynamic @name, alert @all, or mention specific teammates.`
3. **Template & Initial State Overhaul**:
   - **Clean Slate (No Pre-filled Default Message)**:
     - Remove the legacy pre-filled default title and message (`"Shift Operations Announcement"` and `"Dear {name}, Please review..."`).
     - Initialize `notificationTitle = ""` and `notificationMessage = ""`.
     - Use clean Neumorphic placeholders: `"Enter broadcast title..."` and `"Type broadcast message..."`.
   - **Preloaded Dummy Templates Removed**: Hardcoded `defaultTemplates` list poornamaayi remove cheyyuka!
   - **"Save as Template" Action**: Editor header-il sleek Neumorphic **"Save as Template"** button (`[ 💾 Save as Template ]` or Bookmark icon) add cheyyuka. Tapping prompts or saves Title + Message as a custom reusable template.
   - **Custom Saved Templates Strip**: Custom templates exist cheyyunnundengil maathram title-nu mukalil compact horizontal pill strip aayi show cheyyuka (with delete 'x' action). Template illengil clean aayi hide cheyyuka.

---

## 2. Scope of Work for Vijayan

### 1. Structure & Navigation (`NotificationsTab.kt`)
- At the top of `NotificationsTab`:
  ```kotlin
  var activeTabSection by rememberSaveable { mutableIntStateOf(0) } // 0: Compose, 1: History
  ```
- Neumorphic Segmented Control:
  - Tab 0: `📢 Compose Broadcast`
  - Tab 1: `📜 Broadcast History (${notifications.size})`
- Responsive layout:
  - If screen width >= 600dp (Landscape/Tablet): Render Compose on Left pane, History on Right pane.
  - If screen width < 600dp (Portrait Mobile): Switch cleanly between Tab 0 and Tab 1.

### 2. Modernize Compose Section (`NotificationsTab.kt`)
- **Card**: Molded Neumorphic surface (`NeumorphicTokens.Surface`, corner radius `20.dp`).
- **Templates Strip**:
  - Remove `defaultTemplates`!
  - If `state.broadcastTemplates.isNotEmpty()`: Render compact horizontal strip of saved custom template pills with subtle delete 'x' icon.
- **Header Actions**:
  - Title: `"New Broadcast Announcement"`
  - Right: Neumorphic action button **`[ 💾 Save as Template ]`** (saves current Title & Message via `viewModel.saveBroadcastTemplate(...)`).
- **Channels & Target Row**:
  - Delivery Channel toggle pills:
    `[ 📱 In-App ]` `[ 🔔 Push (FCM) ]` `[ 📧 Email ]`
  - Priority selector pills:
    `[ Info ]` `[ Shift Update ]` `[ Urgent ]` `[ Broadcast ]`
  - Target audience selector:
    `[ Everyone ]` `[ Shift Groups ]` `[ Specific Engineers ]`
- **Inputs (Neumorphic Inset Basins)**:
  - Title field: Sunken well (`Modifier.neumorphicInset(...)`).
  - Message editor: Sunken multiline well with comfortable typing.
  - **Remove quick mention chips toolbar** (`SuggestionChip` for `{name}`, `@all`, `@ Mention...`).
  - **Subtle helper description** below editor:
    `Text("💡 Tip: Type @ to insert dynamic @name, alert @all, or mention specific teammates.", fontSize = 11.sp, color = NeumorphicTokens.TextMuted)`
  - **Live `@` Mention Autocomplete Popup**:
    - Triggers as user types `@`.
    - Lists:
      1. `@name` (Dynamic recipient personalization)
      2. `@all` (Team-wide notification)
      3. Teammates matching query with avatar, full name, and `@username`.
    - Selecting an item inserts `@tag ` and places cursor right after it.
- **Bottom Action**:
  - Primary Neumorphic Send Button: **`[ 📤 Dispatch Broadcast ]`** with active sending spinner.

### 3. Modernize History Section
- Display list of past broadcasts in Neumorphic cards:
  - Header: Title, priority badge, relative IST timestamp.
  - Channels used: `In-App`, `Push`, `Email` status badges.
  - Target summary: e.g. "To: Everyone" or "To: @shefeeqkk".
  - Retract action button (for active broadcasts).

### 4. Post-Build Activities (Mandatory)
- Version bump in `android/app/build.gradle.kts`:
  - `versionName = "1.31.0"`
  - `versionCode = 129`
- Execute safe build:
  - `cmd /c "cd android && safe_build.bat assembleDebug"`
- Install on device via ADB:
  - `adb install -r android/app/build/outputs/apk/debug/app-debug.apk`
- Verification:
  - Open Broadcast tab in Admin and Manager profiles.
  - Verify Segmented Control toggles between `Compose` and `History`.
  - Type `@` in message: verify `@name`, `@all`, and usernames appear in autocomplete.
  - Verify quick chips toolbar is removed and replaced by subtle tip description.
  - Verify hardcoded templates are gone; compose message, tap `Save as Template`, verify it saves and appears as a custom pill.
- Commit changes and update `AGENT_HANDOFF.md`.
