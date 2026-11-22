# 📋 Execution Plan for Vijayan (UI Specialist — Android & Web)

**Status:** `COMPLETED & VERIFIED ON PHYSICAL DEVICE` (Sprint v2.4.0 - Build 171: Web & Android Leave Request Card, Interactive Month Calendar Date Picker, Off-Day Partner Ranking & Admin Review Subscreen)

---

## 🔐 Mandatory Production Rule from Gafoor (System Architect 🕶️)
> **Public GitHub Repo + Auto-Deploying Render Production Server:**
> - **ZERO hardcoded personal names, usernames, emails, phone numbers, passwords, or secrets** anywhere in HTML, CSS, JS, Kotlin, comments, or placeholders.
> - Sourced dynamically from session auth and MongoDB Atlas endpoints.
> - **Mandatory `git push origin main`** immediately after verification so Render (`https://api.rosterx.online`) auto-deploys cleanly.

---

## 🚀 Active Sprint: Task 15 — Leave & Swap Request UI (Sprint v2.4.0 - Build 171)

### 1. Web Portal Integration (`web/user-portal.html`, `web/js/userPortal.js`)
1. **Operations Hub / More Tab Entry Card**:
   - In `renderMoreHub()`, add a 3rd Neumorphic card alongside `Server Credential Tracker` and `My Team`:
     - Icon: `ph-calendar-plus` (with soft purple/emerald background)
     - Title: `Leave & Swap Request`
     - Subtitle: `Apply for duty leaves or swap shifts with administrative approval.`
     - Badge: Dynamic badge showing `X Pending` or `Apply Leave`.
     - Clicking opens full subview `#subview_leave_request`.
2. **Interactive Multi-Date Calendar Date Picker**:
   - Subview `#subview_leave_request`:
     - Month navigation header: `< November 2026 >` with quick prev/next navigation.
     - 7-column weekday grid (`Sun` - `Sat`).
     - Day cell states:
       - Past dates (< today): Disabled, subtle opacity, pointer-events none.
       - Scheduled Off-days: Badged with small `OFF` tag, muted appearance, unclickable with tooltip *"Scheduled Weekly Off — No leave required"*.
       - Active Duty days: Clickable! Tapping toggles selection (highlighted with `--nm-accent`, rounded badge).
       - Selection counter: Displays `Selected: X / 5 days (Max 5 days)`.
3. **Preferred Swap Partner Dropdown**:
   - Render dropdown select populated dynamically from `/api/leave/eligible-partners`.
   - **Smart Ranking**: Colleagues who are scheduled `OFF` on the selected date appear grouped at the top under `[Scheduled Off - Recommended]`, followed by `[Other Team Members]`.
   - Clear option: `No preference / Any available partner`.
4. **Optional Reason Input & Submission**:
   - Neumorphic text input: `Reason / Notes (Optional)`.
   - Primary action button: `Submit Leave Request`.
   - Client-side validation: 1 to 5 days, no past dates, no off days.
5. **My Requests History Tab / Card**:
   - Cards showing past and active submissions:
     - Requested Dates chips.
     - Status pill: `Pending Review` (Amber), `Approved as Swap` (Emerald), `Approved as Leave` (Blue), `Rejected` (Red).
     - Details on assigned swap partner and approved SW shift code.
6. **Admin Review Queue (Admin Web Portal)**:
   - Floating review panel / modal in Admin view listing pending requests.
   - For each request: displays candidate partners, shifts, and two primary action buttons:
     - `Approve as Swap` (Recommended) -> Select partner & SW code.
     - `Approve as Leave` (Fallback) -> Approves without partner exchange.
     - `Reject Request`.

---

### 2. Android Jetpack Compose Integration (`android/app/...`)
1. **More Tab Composable Update (`EngineerPortalScreen.kt`, `AdminMainScreen.kt`)**:
   - Add Neumorphic `LeaveRequestCard` to the Operations Hub list.
   - Wire navigation to `LeaveRequestScreen`.
2. **`LeaveRequestScreen.kt` Composable**:
   - Header with back navigation.
   - Multi-date monthly calendar with Compose `LazyVerticalGrid(GridCells.Fixed(7))`.
   - Real-time disabled state for past days and off-days.
   - Dynamic partner dropdown with Off-day recommendation badges.
   - Submit button with Neumorphic spring state.
3. **FCM Push Notification Handling (`RosterXFirebaseMessagingService.kt`)**:
   - Deep-link payload handling: clicking "Leave Request Approved" notification navigates directly to the Leave Status subview.

---

### 3. Versioning & Build Protocol
- `android/app/build.gradle.kts`: `versionName = "2.4.0"`, `versionCode = 171`
- `server.py`: `SERVER_VERSION = "2.4.0"`
- `web/sw.js`: `CACHE_NAME = 'rosterx-pwa-v2.4.0'`
- Cache busters: update `?v=2.4.0` across web files.
- Safe build: `cd android && .\safe_build.bat assembleDebug`.
- Install & verify on device: `adb install -r android/app/build/outputs/apk/debug/app-debug.apk`.
- Push to remote: `git push origin main`.
