# 👵 Aunty (Antigravity) ⇄ 🤖 Kunjappan (Android Studio) Handoff Bridge

> **"Aunty looks after the backend & servers; Kunjappan builds the native Android app!"**  
> *Last Synced by Aunty:* **2026-11-07 00:30 IST** 👵⚡🌴

---

## 📌 Active System State & Monorepo Location
- **Unified Workspace**: `c:\shefeeq\PYTHON\RosterX`
- **Native Android Project Root**: `c:\shefeeq\PYTHON\RosterX\android`
- **Semantic Version**: **`1.14.7`** (Android `versionCode: 50`, Server `SERVER_VERSION = "1.14.7"`)
- **Previous Versions**: `1.14.6` (Build 49), `1.14.5` (Build 48), `1.14.4` (Build 47), `1.14.3` (Build 46), `1.14.2` (Build 45), `1.14.1` (Build 44), `1.14.0` (Build 43), `1.13.1` (Build 42), `1.13.0` (Build 41), `1.12.1` (Build 40), `1.12.0` (Build 39), `1.11.8` (Build 38), `1.11.7` (Build 37), `1.11.6` (Build 36), `1.11.5` (Build 35), `1.11.4` (Build 34), `1.11.0` (Build 30), `1.10.0` (Build 29)
- **Production Server**: ☁️ **Render Cloud (100% Pure)**  
  🌐 `https://rosterx-vc30.onrender.com`  
  📱 **Android Base URL**: `https://rosterx-vc30.onrender.com/api/`
- **Local Asset Sync File**: `android/app/src/main/assets/server_url.txt` (`https://rosterx-vc30.onrender.com/`)
- **Database Engine**: MongoDB Atlas `Cluster0` (Database: `rosterx`, Collections: `engineers`, `rosters`, `roster_backups`, `operational_policies`, `notifications`, `password_resets`, `audit_logs`)
- **Cross-Platform Parity Specification**: [`ANDROID_PARITY.md`](../ANDROID_PARITY.md)

---

## 🎨 Major UI & Architecture Upgrades in v1.14.7

### 1. Resizable Neumorphic Home Screen App Widget
- **Widget Provider**: [`RosterXAppWidgetProvider.kt`](app/src/main/java/com/rosterx/widget/RosterXAppWidgetProvider.kt)
- **Manifest Registration**: Registered with `android.appwidget.action.APPWIDGET_UPDATE` and custom broadcast `com.rosterx.ACTION_UPDATE_WIDGET`.
- **Resizable Multi-Size Support**:
  - Horizontal & Vertical resizing configured in [`rosterx_widget_info.xml`](app/src/main/res/xml/rosterx_widget_info.xml).
  - Target cell width: 3, Target cell height: 2 (easily resizes from 2x2 compact up to 4x4 full view).
- **Neumorphic Dual-Themed Layout**:
  - Soft elevated outer card (`24dp` rounded corners) with Neumorphic light canvas (`#EEF2F6`) and dark slate (`#1E293B`).
  - Raised inner cards (`16dp` rounded corners) for Today's shift and Tomorrow's shift.
  - Prominent RosterX logo and typography.
- **Dynamic Contents Displayed**:
  * **Today's Shift**: Date, shift code badge (colored by shift type), shift title, and exact hours.
  * **Tomorrow's Shift**: Following day's rotation preview.
  * **Shift Duty Countdown Pill**: Dynamic real-time calculation ("Shift Ends in: HHh MMm" or "Duty Starts in: HHh MMm" or "Next Shift Starts in: HHh MMm").
  * **Upcoming Rest Day Pill**: Natural language indicator ("Rest Day: Today" or "Next Rest Day: Tomorrow (Day, Date)" or "Next Rest Day: In X Days").
- **Auto-Sync Engine**:
  - `EngineerPortalViewModel` automatically serializes and persists the active engineer's month shifts to widget cache on load, immediately notifying `AppWidgetManager`.
  - Tapping anywhere on the widget launches `MainActivity`.

---

## 🤖 Direct Briefing for Kunjappan (Android Studio Mobile Agent)

1. **Version Update**:
   - `android/app/build.gradle.kts` has been bumped to `versionName = "1.14.7"` and `versionCode = 50`.
2. **App Widget Live**:
   - The Home Screen Widget is fully compiled and ready. Users can long-press their home screen, choose **Widgets > RosterX**, and place the widget on their home screen!
3. **Wireless Deploy Ready**:
   - Run `.\deploy-wireless.ps1` from the `android/` directory to compile and deploy wirelessly in one step!
     - No Duty: `OFF, L, SW*`
3. **Dual Theme Tokens**:
   - Web tokens in `tokens.css` now match `NeumorphicTokens.kt` 1:1.
   - Refer to `ANDROID_PARITY.md` for exact component mappings.

---

*Signed with love & clean code,*  
**Aunty (Antigravity Lead Full-Stack Architect)** 👵⚡
