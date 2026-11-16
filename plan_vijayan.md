# 📋 Execution Plan for Vijayan (UI & Jetpack Compose)

**Status:** `READY FOR VIJAYAN` (Sprint v1.28.0 - Build 119)

---

## 1. Architectural Mandates from Gafoor (System Architect 🕶️)

Vijaya! Client side-il team celebrations (birthdays & work anniversaries) teammates-num manager-kkum display aakkaanulla complete UI architecture Gafoorka blueprint cheythittundu. Neumorphic design principles and Jetpack Compose standards strictly follow cheyyuka!

### 🔍 Current Client UI Flaws:
1. In `EngineerPortalScreen.kt` (lines ~613–657), celebration check ONLY evaluates `state.loggedInEngineer` (the user's own profile). Teammates never get notified or prompted when someone else on the team has a birthday or anniversary!
2. In `AdminMainScreen.kt`, neither Admin nor Manager has any celebration detection or greeting card on their dashboard.
3. Teammates have no quick one-tap action to wish their colleague directly via WhatsApp or Phone call from the dashboard.

---

## 2. Scope of Work for Vijayan

### 1. Data Layer Integration
- In `RosterApiService.kt`:
  - Add endpoint:
    ```kotlin
    @GET("api/celebrations/today")
    suspend fun getTodayCelebrations(): Response<CelebrationsResponse>
    ```
- In `com.rosterx.data.remote`:
  - Define `CelebrationsResponse`:
    ```kotlin
    data class CelebrationsResponse(
        val celebrations: List<TeamCelebrationDto> = emptyList()
    )
    data class TeamCelebrationDto(
        val username: String = "",
        val name: String = "",
        val type: String = "", // "birthday" | "anniversary" | "both"
        val years: Int = 0,
        val photo: String = "",
        val phone: String = "",
        val title: String = "",
        val greeting_text: String = ""
    )
    ```
- In `EngineerPortalViewModel.kt`:
  - Add `todayCelebrations: StateFlow<List<TeamCelebrationDto>>`
  - Fetch on dashboard load / refresh alongside roster and engineers.

### 2. Festive `TeamCelebrationCard.kt` Component
- Create `android/app/src/main/java/com/rosterx/presentation/components/TeamCelebrationCard.kt`.
- **Neumorphic Festive Design**:
  - Soft elevated Neumorphic card with subtle celebratory amber/gold or coral accent border/glow.
  - Header badge:
    - If `birthday`: "🎂 Birthday Today!" with festive cake icon & confetti styling.
    - If `anniversary`: "🏆 ${years} Years Work Anniversary!" with gold trophy icon.
    - If `both`: "🎉 Double Celebration: Birthday & ${years}y Anniversary!"
  - Colleague Information:
    - Colleague Profile Picture (or gradient avatar with initials).
    - Colleague Full Name in bold typography.
    - Subtitle wishing text (e.g., *"Join the team in wishing them a wonderful day!"*).
  - **Action Suite**:
    1. **"Wish on WhatsApp" Button**:
       - Styled with WhatsApp green accent (`#25D366`) and `R.drawable.ic_whatsapp` vector icon.
       - Tapping launches WhatsApp via `Intent.ACTION_VIEW` (`https://api.whatsapp.com/send?phone=${cleanPhone}&text=${encodedGreeting}`) with a warm pre-filled message:
         - Birthday: `"Happy Birthday ${name}! 🎉🎂 Wishing you an amazing year ahead! - From RosterX"`
         - Anniversary: `"Happy ${years} Year Work Anniversary, ${name}! 🏆✨ Congratulations on this milestone!"`
    2. **"Call" Button**:
       - Initiates phone call via `Intent.ACTION_DIAL` (`tel:${phone}`).
    3. **Dismiss 'X' button**:
       - Saves dismissed celebration ID in `SharedPreferences` (`seen_team_celeb_${username}_${todayStr}`) so it doesn't disturb the user repeatedly on the same day once acknowledged.

### 3. Dashboard Integration
- In `EngineerPortalScreen.kt`:
  - Place `TeamCelebrationCard` right at the top of the Home / Shifts dashboard (above Today's Shift Card / Active Roster) whenever `todayCelebrations` is not empty.
- In `AdminMainScreen.kt`:
  - Also display `TeamCelebrationCard` on the Admin / Manager overview dashboard so managers and admin never miss team celebrations!

### 4. Post-Build Activities (Mandatory)
- Version bump in `android/app/build.gradle.kts`:
  - `versionName = "1.28.0"`
  - `versionCode = 119`
- Execute safe build:
  - `cd android && .\safe_build.bat assembleDebug`
- Install on device via ADB:
  - `adb install -r android/app/build/outputs/apk/debug/app-debug.apk`
- Verify celebration card renders cleanly and WhatsApp intent fires properly.
- Commit changes and update `AGENT_HANDOFF.md`.
