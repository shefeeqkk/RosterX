# 📋 Execution Plan for Vijayan (UI & Jetpack Compose)

**Status:** `COMPLETED` (Sprint v1.27.0 - Build 113)

---

## 1. Architectural Mandates from Gafoor

Vijaya, Gafoorka client UI-ude full layout and interactive enhancements architect cheythu vechittundu. Follow Neumorphic design tokens and Jetpack Compose standards strictly.

### 🎯 Scope of Work for Vijayan:

1. **My Team Enhancements (`MyTeamView.kt`)**: [COMPLETED]
   - **WhatsApp Vector Icon**:
     - Added `ic_whatsapp.xml` to `android/app/src/main/res/drawable/`.
     - In `TeamMemberCard`, replaced `Icons.Default.Chat` with authentic WhatsApp icon (`painterResource(R.drawable.ic_whatsapp)`) with signature emerald `#25D366`.
   - **Accurate Active Member Count**:
     - Excluded `role == EngineerRole.ADMIN` and username `"admin"` from `activeEngineers` count in `MyTeamView.kt` and `MoreHubView.kt`.
     - Filter strictly: `it.status != EngineerStatus.RESIGNED && it.role in listOf(EngineerRole.MANAGER, EngineerRole.ENGINEER, EngineerRole.APPRENTICE) && !it.username.equals("admin", ignoreCase = true)`.
     - Description text reflects exact count of active shift engineers & manager.

2. **Credential Tracker Terminology Refinement (`CredentialTrackerScreen.kt`)**: [COMPLETED]
   - Replaced all instances of `"Rotated"` with `"Updated"` (e.g. `"Updated: ${tracker.lastUpdatedDate} (${tracker.validityDays}d cycle)"`).
   - Replaced all dialog and button references of `"Rotate"` with `"Update"`.
   - Unified "Update Date" action pill, confirmation dialog CTA, and date picker buttons with the theme accent color (`AccentSalmon`) instead of green.

3. **Notification Animations (`ProfileComponents.kt` - `QuickNotificationsBottomSheet`)**: [COMPLETED]
   - **Smooth Item Dismiss & Clear All**:
     - Wrapped notification items in `AnimatedVisibility` with:
       `exit = fadeOut(tween(250)) + slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(280)) + shrinkVertically(animationSpec = tween(250))`
     - Smooth item height collapse with `shrinkVertically` and `animateItemPlacement` eliminates all abrupt height jumps and gaps.
     - On "Clear All", applied a 60ms staggered waterfall dismissal across all items before clearing state.

4. **Interactive Notifications & Professional Redirection**: [COMPLETED]
   - **Deep-Linking / Contextual Redirection**:
     - Tapping alert auto-dismisses notification and closes sheet:
       - Credential Alert (`category == "credential_expiry"` or title contains "Credential"): Navigates to **Credential Tracker** (`isViewingCredentialTracker = true`).
       - Shift / Roster Alert (`priority == "shift_update"` or title/body contains "Roster" / "Shift"): Navigates to **Month Roster** tab (`selectedTab = 2` or `3`).
       - Celebration (`isCelebrationNotif == true`): Opens **Celebration Wishes Dialog**.
   - **Professional Content Styling**:
     - Topic / Category Badges: `[SECURITY]`, `[ROSTER UPDATE]`, `[OPERATIONS]`, `[ANNOUNCEMENT]`, `[CELEBRATION 🎉]` with distinct icons, borders, and colors.
     - Enhanced `@username` & Teammate Mention: Styled mention pills using `SpanStyle` inline highlight.

5. **Post-Build Activities (Mandatory)**: [COMPLETED]
   - Version bump in `android/app/build.gradle.kts`: `versionName = "1.27.0"`, `versionCode = 113`.
   - Backend sync: `server.py` `SERVER_VERSION = "1.27.0"`.
   - Build: `cd android && .\safe_build.bat assembleDebug` (BUILD SUCCESSFUL).
   - Artifact ready: `android/app/build/outputs/apk/debug/app-debug.apk`.
   - ADB Install & Verification: Ready for device sync.
   - Commit changes and update `AGENT_HANDOFF.md`.
