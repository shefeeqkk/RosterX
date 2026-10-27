# 🤖 Android Studio Agent Prompt: RosterX Dual Admin & Engineer Portal App

Use this prompt in **Android Studio (Gemini / AI Agent)** to generate the complete native Android application with Role-Based Access Control (RBAC), full write-access Admin Portal, and personalized Read-Only Engineer Portal.

---

```markdown
# Role & Architecture Guidelines for Android Studio Agent

You are a Principal Android Architect and Senior Kotlin Developer. 
Build a production-grade, native Android application for **RosterX Operations Portal** using:
- **Language**: Kotlin
- **UI Framework**: Jetpack Compose (Material 3) with Dark Theme by default (NOC Midnight space theme matching `#090D16`)
- **Architecture**: MVVM (Model-View-ViewModel) + Clean Architecture repository pattern
- **Network Stack**: Retrofit 2 + OkHttp 3 + Kotlinx Serialization / Gson
- **Persistence / Session**: Jetpack DataStore Preferences for user session, active token, and cached rosters
- **Asynchronous Flow**: Kotlin Coroutines & StateFlow

---

## 1. 🔐 Role-Based Access Control (RBAC) & Dynamic Routing

### Authentication API Contract
- **Endpoint**: `POST /api/auth/login`
- **Base URL**: Defaults to `http://10.0.2.2:8000/api/` (for Android Emulator) or configurable via Server settings.
- **Payload**:
  ```json
  {
    "identifier": "nandua",
    "password": "user123"
  }
  ```
  *(Identifier accepts username, registered email, or full engineer name)*
- **Response**:
  ```json
  {
    "success": true,
    "user": {
      "id": "eng_03",
      "username": "nandua",
      "name": "NANDU ANTHIKAT",
      "full_name": "NANDU ANTHIKAT",
      "email": "nandua@tejasnetworks.com",
      "phone": "+919811001100",
      "role": "engineer"
    }
  }
  ```

### Dynamic Routing Decision
Store the logged-in user in `DataStore`. Based on `user.role`:
1. **If `user.role == "admin"`**:
   - Route to `AdminMainScreen` with **full write permissions** and a 4-tab bottom navigation bar:
     - 📊 **Tab 1: Dashboard** (Overview of Server & AI)
     - 👥 **Tab 2: Manage Team** (Manage engineers and sync to MongoDB)
     - 📅 **Tab 3: Month Rosters** (2D Grid editing with webpage algorithm & Atlas publish)
     - 🖥️ **Tab 4: Server Tab** (Diagnostics, logs, MongoDB URI & API configuration)
2. **If `user.role != "admin"`** (`"engineer"` or `"apprentice"`):
   - Route to `EngineerPortalScreen` with **read-only permissions**, customized specifically to that user's schedule.

---

## 2. 👑 Admin Portal Feature Specifications (Full Write Access)

### 📊 Tab 1: Dashboard (`AdminDashboardTab.kt`)
1. **Server Health Card**:
   - Test connection indicator (`GET /api/test-connection`).
   - Atlas Cluster (`Cluster0`), Database (`rosterx`), ping latency in ms.
   - Cloudflare Quick Tunnel status and active public URL.
   - "Refresh Diagnostics" button.
2. **AI & Operational Policy Card**:
   - Configured Gemini Model (e.g. `gemini-1.5-flash` or `gemini-2.5-flash`).
   - Active Constraints badge display:
     - Night duty requirement: `1/night`
     - Morning duty requirement: `>=1 FS`
     - Fatigue compliance: `48h post-NS rest`
     - Policy: `Swap-First Arbitration`
3. **Real-Time Rule Violation Auditor**:
   - Scans loaded month roster and flags violations (e.g. double night shift, missing coverage, broken rest windows).
   - Displays clickable violation chips showing engineer name and violation day.

---

### 👥 Tab 2: Manage Team (`ManageTeamTab.kt`)
1. **Engineers Directory List (`GET /api/engineers`)**:
   - Displays all engineers sorted by weekly off groupings.
   - Information displayed: Name, Employee ID, Role (`Admin`, `Engineer`, `Apprentice`), Default Weekly Offs (e.g. Sat/Sun, Fri/Sat), Status (`Active`, `Resigned`), Email, and Phone.
2. **Engineer Form Dialog / Bottom Sheet**:
   - Add new engineer or edit existing details.
   - Form inputs: Full Name, Username, Email, Phone, Role dropdown, Default Weekly Offs multi-select, and Status toggle (`Active` vs `Resigned`).
3. **MongoDB Synchronization**:
   - "Save & Sync to MongoDB" button dispatches `POST /api/engineers` with updated engineer list.
   - Live search filter by engineer name or ID.

---

### 📅 Tab 3: Month Rosters (`MonthRostersTab.kt`)
1. **Month Selector & Actions Toolbar**:
   - Month dropdown / picker (e.g. `October 2026`, `November 2026`).
   - Navigation arrows (`<` and `>`).
   - "Run AI Roster Generation" button.
   - "Save & Publish to MongoDB" button (`POST /api/roster`).
2. **Interactive 2D Shift Matrix Table**:
   - **Sticky Columns**: First column stays fixed showing Engineer Names.
   - **Sticky Dual Header**: Row 1 showing dates (01 to 31), Row 2 showing weekdays (MO, TU, WE, etc.).
   - **Cell Interactions**:
     - **Tap Cell**: Opens Shift Selection BottomSheet allowing selection of `FS`, `FG`, `G`, `SG`, `SS`, `NS`, `OFF`, `L`, or custom swap `SW*`.
     - **Long-Press Cell**: Toggles **Lock Flag `🔒`**. Locked cells remain intact and untouched when running AI generation.
3. **Webpage Algorithm Parity (AI Engine & Constraints)**:
   - Implement the Swap-First logic from `rosterAiEngine.js`:
     - Mandatory 1 NS per night across all days.
     - 48-hour recovery window after NS before next assignment.
     - Swap-First matching for planned absences with reciprocal swap codes.
   - Color coding matching RosterX palette:
     - `FS` (#BBF7D0, text #064E3B)
     - `FG` (#166534, text #FFFFFF)
     - `G`  (#BAE6FD, text #0C4A6E)
     - `SG` (#EA580C, text #FFFFFF)
     - `SS` (#FED7AA, text #7C2D12)
     - `NS` (#1E293B, text #FFFFFF)
     - `OFF` / `SW*` (#FEF08A, text #713F12)
     - `L`  (#FECACA, text #991B1B)

---

### 🖥️ Tab 4: Server Tab (`ServerManagementTab.kt`)
1. **Server Status & Diagnostics**:
   - Run live diagnostic check to `GET /api/test-connection`.
   - Display server host, local LAN IP, and port (`8000`).
2. **API & MongoDB Configuration**:
   - Editable API Base URL (stored in `DataStore`, e.g. `http://10.0.2.2:8000/api/` or cloud tunnel).
   - Display masked MongoDB URI and active cluster name.
   - Resend API key status (Configured/Missing) for OTP password resets.
3. **Live Logs Viewer**:
   - Terminal-style card showing incoming HTTP requests, response status codes, and Atlas query events.
   - "Clear Logs" and "Copy Diagnostics" buttons.

---

## 3. 👤 Engineer Portal Feature Specifications (Read-Only)

When logged in with an engineer or apprentice username:
1. **Hero Welcome Dashboard Banner**:
   - Current formatted date (e.g. `Saturday, October 24, 2026`).
   - "Welcome, **<Engineer Name>**! 👋".
   - Live duty status badge (e.g., `"🟢 On Duty: First Shift"` or `"🏖️ Scheduled Rest Day"`).
2. **Personal Shift Spotlight Grid**:
   - **Today's Duty Card**: Current shift code, timings (`07:30 AM – 04:30 PM`), category border.
   - **Tomorrow's Duty Card**: Next day shift code, timings, and rotation sequence.
   - **Monthly Duty Counts**: Day shifts count vs Night shifts count.
   - **Rest Days Count**: Total weekly offs (`OFF`).
3. **24/7 Team Duty Schedule**:
   - Shows live duty assignments for the entire team for Today and Tomorrow.
   - **Logged-in user's row is prominently highlighted** with an amber star badge and glowing border.
4. **Month Shift Master Grid**:
   - Full 31-day read-only shift matrix with search filter and shift codes legend.

---

## 4. 📁 Complete Project & Package Structure

```text
app/src/main/java/com/rosterx/app/
├── data/
│   ├── api/
│   │   ├── RosterApiService.kt          // Retrofit API endpoints (/api/auth/login, /api/roster, /api/engineers)
│   │   └── NetworkModule.kt             // Retrofit & OkHttp with dynamic BaseUrl interceptor
│   ├── model/
│   │   ├── AuthModels.kt                // LoginRequest, LoginResponse, User
│   │   ├── EngineerModel.kt             // Engineer, Role, DefaultWeeklyOffs
│   │   └── RosterModel.kt               // RosterDocument, EngineerRoster, ShiftItem
│   └── repository/
│       ├── AuthRepository.kt
│       ├── EngineerRepository.kt
│       └── RosterRepository.kt
├── domain/
│   ├── engine/
│   │   ├── RosterAiEngine.kt            // Swap-First algorithm matching rosterAiEngine.js
│   │   └── RosterRuleAuditor.kt         // Real-time compliance validator
│   └── session/
│       └── SessionManager.kt            // Jetpack DataStore for user session & base URL
├── ui/
│   ├── admin/
│   │   ├── AdminMainScreen.kt           // Scaffold with 4 BottomNavigation tabs
│   │   ├── tabs/
│   │   │   ├── AdminDashboardTab.kt
│   │   │   ├── ManageTeamTab.kt
│   │   │   ├── MonthRostersTab.kt
│   │   │   └── ServerManagementTab.kt
│   │   └── viewmodel/
│   │       ├── AdminDashboardViewModel.kt
│   │       ├── ManageTeamViewModel.kt
│   │       ├── MonthRostersViewModel.kt
│   │       └── ServerManagementViewModel.kt
│   ├── engineer/
│   │   ├── EngineerPortalScreen.kt      // Hero banner, Today/Tomorrow cards, Team board, Month grid
│   │   └── EngineerPortalViewModel.kt
│   ├── login/
│   │   ├── LoginScreen.kt               // Username & Password authentication
│   │   └── LoginViewModel.kt
│   ├── theme/
│   │   ├── Color.kt                     // High-contrast NOC Space Dark Palette & Shift Colors
│   │   ├── Theme.kt
│   │   └── Type.kt
│   └── MainActivity.kt
└── AndroidManifest.xml                  // android:usesCleartextTraffic="true"
```

---

## 5. 📦 Dependencies (`app/build.gradle.kts`)

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.rosterx.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.rosterx.app"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
    }

    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }
}

dependencies {
    // Jetpack Compose BOM & Material 3
    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // Lifecycle & Coroutines
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.3")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.3")

    // Retrofit & OkHttp
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Preferences DataStore
    implementation("androidx.datastore:datastore-preferences:1.1.1")
}
```

---

## 6. 🚀 Execution Steps

1. Generate all data models and Retrofit API interface corresponding to `/api/auth/login`, `/api/roster`, `/api/engineers`, and `/api/test-connection`.
2. Implement `SessionManager` using DataStore to handle login persistence and role caching.
3. Implement `LoginScreen` and navigation controller routing to `AdminMainScreen` for `role == "admin"` and `EngineerPortalScreen` for other roles.
4. Construct `AdminMainScreen` with the 4 tabs:
   - `AdminDashboardTab`: Server & AI health overview.
   - `ManageTeamTab`: Complete engineer list CRUD and sync.
   - `MonthRostersTab`: Bi-directional scrollable table, cell tap edit, lock toggles, and Atlas publish.
   - `ServerManagementTab`: Diagnostics, logs, and API URL config.
5. Construct `EngineerPortalScreen` with Welcome Hero, Today/Tomorrow duty cards, highlighted team row, and read-only month grid.
```
