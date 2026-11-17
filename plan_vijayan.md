# 📋 Execution Plan for Vijayan (UI & Jetpack Compose)

**Status:** `COMPLETED` (Sprint v1.32.0 - Build 133 Client-Side Celebration Auto-Expiration Clean Sweep + Cursor-Anchored Ultra-Compact `@` Mention Popup compiled & installed)

---

## 1. Architectural Mandates from Gafoor (System Architect 🕶️)

Vijaya! Sprint v1.32.0-il ninakku **2 crucial UI tasks** undu:

### Task A: Client-Side Defensive Celebration Auto-Expiration (`ProfileComponents.kt` & `NotificationsTab.kt`)
Dasan backend-il expired celebrations hard purge cheyyunnundu. Ennalum client-side-il network lag karanamo cached notifications karanamo "Yesterday" enna celebration card oru second polum flash aavaan paadilla!
1. **QuickNotificationsBottomSheet Defensive Date Filter**:
   - Notification Drawer-il `activeNotifs` calculate cheyyumbol, notification celebration category aano athil `celebrationType` undo ennu check cheyyuka.
   - Athinte timestamp or createdAt date parse cheythu IST (`Asia/Kolkata`) date edukkuka.
   - If that date is strictly earlier than today's IST date (`notifDate < todayIst`), **drop it immediately from the active list**!
2. **NotificationsTab History Parity**:
   - Admin/Manager `NotificationsTab` history section-ilum expired celebration notices clean sweep aayi hide aakanam.

### Task B: Cursor-Anchored Ultra-Compact `@` Mention Popup (`NotificationsTab.kt`)
User feedback + screenshot check cheythu:
- Currently `@` type cheyyumbol `SUGGESTED MENTIONS` card **entire Message Body box-nu thazhe** (below the Tip text) aanu varunnathu — software keyboard athu cut aakkunnu!
- Cards valare valuthum bulky-yum aanu (header, avatars, full names, multi-line descriptions).
- **Mandatory Fix**:
  1. **Anchor Directly Below the Text Cursor**:
     - Track `TextLayoutResult` via `BasicTextField(..., onTextLayout = { textLayoutResult = it })`.
     - Compute `cursorRect = textLayoutResult?.getCursorRect(cursorIndex)` and render the suggestion list inside a Compose `Popup(alignment = Alignment.TopStart, offset = IntOffset(x, y), properties = PopupProperties(focusable = false, clippingEnabled = true))` positioned directly beneath `cursorRect.bottom`!
     - **`focusable = false` is mandatory** so typing continues uninterrupted without closing the keyboard, while tapping a suggestion still inserts it.
  2. **Small & Ultra-Compact Design (Usernames Only for Engineers)**:
     - Remove the `"SUGGESTED MENTIONS"` header completely.
     - Compact floating card: `widthIn(min = 135.dp, max = 195.dp)`, `heightIn(max = 170.dp)` scrollable `Column`, `RoundedCornerShape(10.dp)`, `padding(4.dp)`.
     - Single-line ultra-compact rows (`padding(horizontal = 10.dp, vertical = 6.dp)`):
       - **`@name`**: Compact single line (`@name` bold + tiny muted tag `recipient`).
       - **`@all`**: Compact single line (`@all` bold + tiny muted tag `team`).
       - **Engineers**: Show **ONLY `@${eng.username}`** (`12.sp`, single line, `TextOverflow.Ellipsis`). **NO full name, NO avatar circle, NO role, NO extra details!**

---

## 2. Scope of Work for Vijayan

### 📁 Primary Files:
1. `android/app/src/main/java/com/rosterx/presentation/components/ProfileComponents.kt`
2. `android/app/src/main/java/com/rosterx/presentation/admin/tabs/NotificationsTab.kt`
3. `android/app/build.gradle.kts`

### 🛠️ Implementation Blueprint:

#### 1. `ProfileComponents.kt` (`QuickNotificationsBottomSheet`):
- Filter `activeNotifs` using today's `Asia/Kolkata` date (`yyyy-MM-dd`):
  ```kotlin
  val todayIstStr = remember {
      SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
          timeZone = TimeZone.getTimeZone("Asia/Kolkata")
      }.format(Date())
  }

  val activeNotifs = remember(notifications, dismissedNotificationIds, dismissingIds) {
      notifications.mapIndexed { idx, n ->
          val id = n.id ?: "idx_$idx"
          id to n
      }.filter { (id, n) ->
          val isCelebrationNotif = n.celebrationType != null ||
              n.sender?.contains("Celebration", ignoreCase = true) == true ||
              n.category.equals("celebration", ignoreCase = true) ||
              n.title?.contains("Birthday", ignoreCase = true) == true ||
              n.title?.contains("Anniversary", ignoreCase = true) == true

          if (isCelebrationNotif) {
              val notifDateStr = n.timestamp?.let { ts ->
                  try {
                      SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
                          timeZone = TimeZone.getTimeZone("Asia/Kolkata")
                      }.format(Date((ts * 1000).toLong()))
                  } catch (_: Exception) { null }
              } ?: n.createdAt?.take(10)

              if (notifDateStr != null && notifDateStr < todayIstStr) {
                  return@filter false // Expired! Auto-dropped cleanly
              }
          }

          id !in dismissedNotificationIds || id in dismissingIds
      }
  }
  ```

#### 2. `NotificationsTab.kt` (History Clean Sweep + Cursor-Anchored Compact `@` Mention Popup):
- **History list**: Filter out expired celebration notifications using the same IST date check.
- **Message Body Editor (`lines ~882-1080`)**:
  - Add `var messageTextLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }` and `val density = LocalDensity.current`.
  - Pass `onTextLayout = { messageTextLayoutResult = it }` to `BasicTextField`.
  - Remove the old `AnimatedVisibility` block below the tip text.
  - Inside the inner `Box` containing `BasicTextField`, render the cursor-anchored `Popup`:
    ```kotlin
    val showMentionPopup = isMentionActive && (
        matchingMentionEngineers.isNotEmpty() ||
        "name".contains(mentionQuery ?: "") ||
        "all".contains(mentionQuery ?: "") ||
        "everyone".contains(mentionQuery ?: "")
    )
    val cursorIndex = messageTextFieldValue.selection.start.coerceIn(0, messageTextFieldValue.text.length)
    val cursorRect = messageTextLayoutResult?.getCursorRect(cursorIndex)

    if (showMentionPopup && cursorRect != null) {
        val xOffset = cursorRect.left.toInt().coerceAtLeast(0)
        val yOffset = (cursorRect.bottom + with(density) { 4.dp.toPx() }).toInt()

        Popup(
            alignment = Alignment.TopStart,
            offset = IntOffset(x = xOffset, y = yOffset),
            properties = PopupProperties(
                focusable = false,
                dismissOnBackPress = true,
                dismissOnClickOutside = true,
                clippingEnabled = true
            )
        ) {
            Surface(
                modifier = Modifier
                    .widthIn(min = 135.dp, max = 195.dp)
                    .heightIn(max = 170.dp),
                shape = RoundedCornerShape(10.dp),
                color = NeumorphicTokens.Surface,
                shadowElevation = 8.dp,
                border = BorderStroke(1.dp, NeumorphicTokens.Accent.copy(alpha = 0.35f))
            ) {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(4.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    // 1. @name (compact single-line)
                    if ("name".contains(mentionQuery ?: "") || (mentionQuery ?: "").isEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { insertMention("name") }
                                .padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "@name",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeumorphicTokens.Accent,
                                maxLines = 1
                            )
                            Text(
                                text = "recipient",
                                fontSize = 10.sp,
                                color = NeumorphicTokens.TextMuted,
                                maxLines = 1
                            )
                        }
                    }

                    // 2. @all (compact single-line)
                    if ("all".contains(mentionQuery ?: "") || "everyone".contains(mentionQuery ?: "") || (mentionQuery ?: "").isEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { insertMention("all") }
                                .padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "@all",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF8B5CF6),
                                maxLines = 1
                            )
                            Text(
                                text = "team",
                                fontSize = 10.sp,
                                color = NeumorphicTokens.TextMuted,
                                maxLines = 1
                            )
                        }
                    }

                    // 3. Engineers — ONLY @username (no full name, no avatar, no extra details)
                    matchingMentionEngineers.forEach { eng ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { insertMention(eng.username) }
                                .padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "@${eng.username}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = NeumorphicTokens.TextMain,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
    ```

#### 3. Post-Build Activities (Mandatory):
- Bump version in `android/app/build.gradle.kts`:
  - `versionName = "1.32.0"`
  - `versionCode = 132`
- Run safe build:
  - `cmd /c "cd android && safe_build.bat assembleDebug"`
- Install APK via ADB:
  - `adb install -r android/app/build/outputs/apk/debug/app-debug.apk`
- On-Device Verification:
  1. Open Notifications Drawer and verify yesterday's celebration notification is completely gone.
  2. Open Admin/Manager Broadcast Composer -> Message Body -> type `@` on line 1 and line 3: verify compact floating popup opens **directly below the cursor**, stays visible above the keyboard, allows uninterrupted typing (`@sh...`), and shows **only `@username`** for engineers (no full name/details/avatars).
- Commit changes and update `AGENT_HANDOFF.md`.
