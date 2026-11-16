# 📋 Execution Plan for Vijayan (UI & Jetpack Compose)

**Status:** `READY FOR VIJAYAN` (Sprint v1.28.0 - Build 119)

---

## 1. Architectural Mandates from Gafoor (System Architect 🕶️)

Vijaya! User report cheytha bug-inte root cause Gafoorka screen inspect cheythu kandu pidichu:
1. Notifications bottom sheet-il Nandu Anthikat-nte Work Anniversary notification-il "Tap to open wishes ✨" click cheythaal, currently `EngineerPortalScreen.kt` ONLY checks `state.loggedInEngineer` (Shefeeq), and opens dialog saying:
   `"Happy Birthday, Shefeeq K K!"`!
   - Root cause: `onOpenCelebrationWishes: () -> Unit` had no parameters, and `CelebrationWishesDialog` hardcoded `state.loggedInEngineer!!` and defaulted `activeCelebrationType` to `BIRTHDAY`!
2. Teammates must receive the notification & dialog **in 3rd person**!
   - When viewing a colleague's celebration, the dialog must say:
     - Title: `"Happy 2nd Work Anniversary, Nandu Anthikat! 🏆"` (or Birthday)
     - Subtitle: `"MILESTONE ACHIEVED 🌟"` / `"TEAM MEMBER BIRTHDAY 🎂"`
     - Body: Celebratory team message honoring Nandu's milestone.
     - Action buttons:
       - **"🎉 Wish on WhatsApp"** (signature WhatsApp green `#25D366` + `R.drawable.ic_whatsapp` icon) -> Launches WhatsApp with pre-filled message:
         - Anniversary: `"Happy 2nd Work Anniversary Nandu! 🏆✨ Congratulations on this milestone!"`
         - Birthday: `"Happy Birthday Nandu! 🎉🎂 Wishing you a wonderful year ahead!"`
       - **"Call"** button -> Launches phone dialer `Intent.ACTION_DIAL`.
       - **"Close"** text button.
   - When viewing one's own celebration (`isSelf = true`), it retains `"CELEBRATING YOUR SPECIAL DAY 🎉"` and `"🎉 Thank You! 🙏"`.

---

## 2. Scope of Work for Vijayan

### 1. Data Layer (`RosterApiService.kt`)
- In `NotificationDto`:
  - Add `@SerializedName("celebration_phone") val celebrationPhone: String? = null`
- In `TodayCelebrationDto` (for `GET /api/celebrations/today`):
  - Ensure fields: `username`, `name`, `type`, `years`, `photo`, `phone`, `title`, `greeting_text`.

### 2. Overhaul `CelebrationWishesDialog.kt`
- Add parameters:
  ```kotlin
  @Composable
  fun CelebrationWishesDialog(
      show: Boolean,
      celebrationType: CelebrationType,
      userName: String,
      anniversaryYears: Int = 1,
      isSelf: Boolean = true,
      celebrantPhone: String? = null,
      onDismiss: () -> Unit
  )
  ```
- Differentiate between `isSelf == true` vs `isSelf == false` (3rd Person):
  - **If `!isSelf` (Teammate / Manager perspective)**:
    - Subtitle:
      - Birthday: `"TEAM MEMBER BIRTHDAY 🎂"`
      - Anniversary: `"MILESTONE ACHIEVED 🌟"`
      - Both: `"DOUBLE MILESTONE 🌟"`
    - Title:
      - Birthday: `"Happy Birthday, $properName! 🎂"`
      - Anniversary: `"Happy $yrStr Work Anniversary, $properName! 🏆"`
      - Both: `"Double Celebration for $properName! 🎂🏆"`
    - Body:
      - Birthday: `"Today is $properName's birthday! Join the entire RosterX team in wishing them immense happiness, vibrant health, and continued success! 🎈🥳✨"`
      - Anniversary: `"Today $properName completes $yrLabel of operational excellence with our team! Let's celebrate their dedication, brilliance, and teamwork! 🥂🎊✨"`
      - Both: `"Today is doubly special for $properName celebrating their birthday and $yrLabel with RosterX! Send your warmest wishes! 🎈🥂🎉"`
    - Bottom Actions:
      - Primary Button: **"🎉 Wish on WhatsApp"**
        - Styled with `#25D366` background, white text, and `R.drawable.ic_whatsapp` icon.
        - Tapping launches WhatsApp via `Intent.ACTION_VIEW` (`https://api.whatsapp.com/send?phone=${cleanPhone}&text=${encodedGreeting}`).
      - Secondary Button / Icon: **"Call"** button with `Icons.Default.Phone` launching `Intent.ACTION_DIAL` (`tel:${phone}`).
      - Close button to dismiss.
  - **If `isSelf` (Direct perspective)**:
    - Retains personal celebratory message with `"🎉 Thank You! 🙏"` button.

### 3. Update `QuickNotificationsBottomSheet` in `ProfileComponents.kt`
- Update `onOpenCelebrationWishes` callback:
  ```kotlin
  onOpenCelebrationWishes: (
      celebrationType: CelebrationType,
      celebrantUser: String,
      celebrantName: String,
      anniversaryYears: Int,
      celebrantPhone: String?
  ) -> Unit = { _, _, _, _, _ -> }
  ```
- When tapping a celebration notification:
  - Parse `celebrationType`: check `notif.celebrationType` ("both" -> BOTH, "anniversary" -> ANNIVERSARY, else -> BIRTHDAY).
  - Extract `notif.celebrationUser`, `notif.celebrationName`, `notif.celebrationYears`, and `notif.celebrationPhone` (fallback to looking up user in `engineers` list if phone is blank).
  - Pass all these details into `onOpenCelebrationWishes(...)`.

### 4. Wire Up in `EngineerPortalScreen.kt`
- Maintain state:
  ```kotlin
  var celebrationDialogTargetUser by rememberSaveable { mutableStateOf("") }
  var celebrationDialogTargetName by rememberSaveable { mutableStateOf("") }
  var celebrationDialogType by rememberSaveable { mutableStateOf(CelebrationType.BIRTHDAY) }
  var celebrationDialogYears by rememberSaveable { mutableStateOf(1) }
  var celebrationDialogPhone by rememberSaveable { mutableStateOf("") }
  var isCelebrationSelf by rememberSaveable { mutableStateOf(true) }
  ```
- In `QuickNotificationsBottomSheet` call:
  ```kotlin
  onOpenCelebrationWishes = { type, user, name, years, phone ->
      showQuickNotificationsSheet = false
      celebrationDialogTargetUser = user
      celebrationDialogTargetName = name
      celebrationDialogType = type
      celebrationDialogYears = years
      celebrationDialogPhone = phone ?: ""
      val myUname = state.loggedInEngineer?.username.orEmpty()
      isCelebrationSelf = user.equals(myUname, ignoreCase = true)
      showCelebrationDialog = true
  }
  ```
- Pass `isSelf = isCelebrationSelf`, `userName = celebrationDialogTargetName`, `celebrationType = celebrationDialogType`, `anniversaryYears = celebrationDialogYears`, `celebrantPhone = celebrationDialogPhone` into `CelebrationWishesDialog`.

### 5. Post-Build Activities (Mandatory)
- Version bump in `android/app/build.gradle.kts`:
  - `versionName = "1.28.0"`
  - `versionCode = 119`
- Execute safe build:
  - `cd android && .\safe_build.bat assembleDebug`
- Install on device via ADB:
  - `adb install -r android/app/build/outputs/apk/debug/app-debug.apk`
- Test: Open notification drawer, tap Nandu's anniversary notification, verify 3rd-person dialog opens with Nandu's name, trophy icon, and WhatsApp/Call buttons.
- Commit changes and update `AGENT_HANDOFF.md`.
