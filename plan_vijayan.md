# 📋 Execution Plan for Vijayan (UI & Jetpack Compose)

**Status:** `READY FOR VIJAYAN` (Sprint v1.27.0 - Build 113)

---

## 1. Architectural Mandates from Gafoor

Vijaya, Gafoorka client UI-ude full layout and interactive enhancements architect cheythu vechittundu. Follow Neumorphic design tokens and Jetpack Compose standards strictly.

### 🎯 Scope of Work for Vijayan:

1. **My Team Enhancements (`MyTeamView.kt`)**:
   - **WhatsApp Vector Icon**:
     - Add `ic_whatsapp.xml` to `android/app/src/main/res/drawable/`.
     - In `TeamMemberCard`, replace `Icons.Default.Chat` with the authentic WhatsApp icon (`painterResource(R.drawable.ic_whatsapp)`).
   - **Accurate Active Member Count**:
     - Exclude `role == EngineerRole.ADMIN` and username `"admin"` from `activeEngineers` count.
     - Filter strictly: `it.status != EngineerStatus.RESIGNED && it.role in listOf(EngineerRole.MANAGER, EngineerRole.ENGINEER, EngineerRole.APPRENTICE) && !it.username.equals("admin", ignoreCase = true)`.
     - Verify description text matches exact count of shown members (e.g. 10 Active Members = 1 Manager + 9 Engineers).

2. **Credential Tracker Terminology Refinement (`CredentialTrackerScreen.kt`)**:
   - Replace the word `"Rotated"` with `"Updated"` (e.g. `"Updated: ${tracker.lastUpdatedDate} (${tracker.validityDays}d cycle)"`).
   - Replace any other instances of `"Rotate"` with `"Update"`.

3. **Notification Animations (`ProfileComponents.kt` - `QuickNotificationsBottomSheet`)**:
   - **Smooth Item Dismiss & Clear All**:
     - Wrap notification items in `AnimatedVisibility` with:
       `exit = fadeOut(tween(250)) + slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(280)) + shrinkVertically(animationSpec = tween(250))`
     - Ensure item height collapses smoothly so remaining items slide up naturally without abrupt jumps or gaps.
     - On "Clear All", apply staggered dismissal so all items animate out fluidly before clearing state.

4. **Interactive Notifications & Professional Redirection**:
   - **Deep-Linking / Contextual Redirection**:
     - When tapping a notification card:
       - Credential Alert (`category == "credential_expiry"` or title contains "Credential"): Dismiss sheet and navigate to **Credential Tracker** (`isViewingCredentialTracker = true`).
       - Shift / Roster Alert (`priority == "shift_update"` or title/body contains "Roster" / "Shift"): Dismiss sheet and navigate to **Month Roster** tab (`selectedTab = 2` or `selectedTab = 3`).
       - Celebration (`isCelebrationNotif == true`): Dismiss sheet and open **Celebration Wishes Dialog**.
       - General Broadcast: Open detailed announcement view or expand full message.
   - **Professional Content Styling**:
     - Topic / Category Badge: `[SECURITY]`, `[ROSTER UPDATE]`, `[OPERATIONS]`, `[ANNOUNCEMENT]` with category-specific icon.
     - Enhanced `@username` Mention: Format `@username` and `{Their name}` mentions with a distinct, stylish accent chip/pill.

5. **Post-Build Activities (Mandatory)**:
   - Version bump in `android/app/build.gradle.kts`:
     - `versionName = "1.27.0"`
     - `versionCode = 113`
   - Build: `cd android && .\safe_build.bat assembleDebug`.
   - ADB Install: `adb install -r android/app/build/outputs/apk/debug/app-debug.apk`.
   - Verify UI on emulator/device.
   - Commit changes and update `AGENT_HANDOFF.md`.
