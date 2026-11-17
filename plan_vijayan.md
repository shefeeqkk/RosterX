# 📋 Execution Plan for Vijayan (UI & Jetpack Compose)

**Status:** `READY FOR VIJAYAN` (Sprint v1.32.0 - Build 132 Client-side Defensive Celebration Auto-Expiration)

---

## 1. Architectural Mandates from Gafoor (System Architect 🕶️)

Vijaya! Dasan backend-il expired celebrations hard purge cheyyunnundu. Ennalum client-side-il network lag karanamo cached notifications karanamo "Yesterday" enna celebration card oru second polum flash aavaan paadilla!
Athukondu client-side-il:
1. **QuickNotificationsBottomSheet Defensive Date Filter**:
   - Notification Drawer-il `activeNotifs` calculate cheyyumbol, notification celebration category aano athil `celebrationType` undo ennu check cheyyuka.
   - Athinte timestamp or createdAt date parse cheythu IST (`Asia/Kolkata`) date edukkuka.
   - If that date is strictly earlier than today's IST date (`notifDate < todayIst`), **drop it immediately from the active list**!
2. **NotificationsTab History Parity**:
   - Admin/Manager `NotificationsTab` history section-ilum expired celebration notices clean sweep aayi hide aakanam.
3. **Build & On-Device Verification**:
   - `android/app/build.gradle.kts`:
     - `versionName = "1.32.0"`
     - `versionCode = 132`
   - Build via `cmd /c "cd android && safe_build.bat assembleDebug"`.
   - Install via `adb install -r android/app/build/outputs/apk/debug/app-debug.apk`.
   - Verify notification drawer: confirm Nandu Anthikat-nte yesterday's anniversary notification is completely gone and no longer lingers!

---

## 2. Scope of Work for Vijayan

### 📁 Primary Files:
1. `android/app/src/main/java/com/rosterx/presentation/components/ProfileComponents.kt`
2. `android/app/src/main/java/com/rosterx/presentation/manager/NotificationsTab.kt`
3. `android/app/build.gradle.kts`

### 🛠️ Implementation Steps:
1. **In `ProfileComponents.kt` (`QuickNotificationsBottomSheet`)**:
   - When filtering `activeNotifs`:
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
2. **In `NotificationsTab.kt`**:
   - Ensure the history list also filters out any expired celebration notifications if they slipped through memory cache.
3. **Bump Version & Safe Build**:
   - `versionName = "1.32.0"`
   - `versionCode = 132`
   - Run safe build:
     `cmd /c "cd android && safe_build.bat assembleDebug"`
   - Install APK via ADB:
     `adb install -r android/app/build/outputs/apk/debug/app-debug.apk`
4. **Verification & Handoff**:
   - Open drawer and verify Nandu Anthikat's card from yesterday is gone!
   - Update `AGENT_HANDOFF.md` and commit.
