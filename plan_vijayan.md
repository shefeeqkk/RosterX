# 📋 Execution Plan for Vijayan (UI Specialist — Android & Web)

**Status:** `READY FOR VIJAYAN` (Sprint v2.2.0 - Build 151 1:1 Android-Web Parity: Raised Notifications, Teammate Celebrations, Target Calendar & Progressive Back-Stack)

---

## 🔐 Mandatory Production Rule from Gafoor (System Architect 🕶️)
> **Public GitHub Repo + Auto-Deploying Render Production Server:**
> - **ZERO hardcoded personal names, usernames, emails, phone numbers, passwords, or secrets** anywhere in HTML, CSS, JS, Kotlin, comments, or placeholders.
> - **Mandatory `git push origin main`** immediately after verification so Render (`https://api.rosterx.online` / `https://rosterx.online`) deploys immediately!

---

## 1. Task 1 — Raised Neumorphic Notification Cards in Web (`userPortal.js`, `mobile-shell.css`)
Match Android's notification drawer 1:1 (raised Neumorphic cards with category pill badges instead of sunken wells):

1. **Card Architecture**:
   - Replace sunken `.rx-well` in `openNotificationsDialog()` with raised Neumorphic cards (`.rx-card`, `border-radius: 20px`, `box-shadow: var(--nm-shadow-raised-subtle)`).
2. **Category Pill Badges**:
   - Top-left header pill:
     - `🎉 CELEBRATION`: Amber/Gold soft pill (`background: rgba(245,158,11,0.14); color: #B45309;`)
     - `🛡️ SECURITY`: Rose/Red soft pill (`background: rgba(239,68,68,0.14); color: #B91C1C;`)
     - `ℹ️ OPERATIONS`: Emerald/Teal soft pill (`background: rgba(16,185,129,0.14); color: #047857;`)
     - `📅 SHIFT`: Indigo/Purple soft pill (`background: rgba(99,102,241,0.14); color: #4338CA;`)
3. **Card Content & Meta**:
   - Title next to badge + soft Neumorphic 'x' dismiss button.
   - Body text with personalized `@name` tokens.
   - Footer row: Author `"By RosterX Celebrations Desk"` / `"By Operations Desk"`, formatted time `"Today, 05:56 AM IST"`.
   - Action links: `"Tap to open wishes ✨"`, `"Tap to view Credential Tracker →"`.

---

## 2. Task 2 — Teammate vs Self Celebration Wishes Parity (`userPortal.js` & Android)
Fix celebration popup so tapping a coworker's anniversary shows teammate actions (WhatsApp, Call, Email) instead of "Thank You!":

1. **Web (`web/js/userPortal.js` — `openCelebrationDialog`)**:
   - Check if the celebrant is the current logged-in user (`isSelf = (celebrantUsername === currentUsername)`).
   - **If `isSelf == true`**:
     - Header: "Happy Birthday / Anniversary! 🏆"
     - Body: "Wishing you a wonderful day filled with joy, health, and continued success..."
     - Button: `[ Thank You! 🙏 ]` with confetti animation!
   - **If `isSelf == false` (Teammate Celebration)**:
     - Header: "Celebrating <Name>'s 1st Work Anniversary!" (or Birthday)
     - Body: "Today <Name> completes 1 year of operational excellence with our team! Join us in celebrating this milestone and send your warm congratulations!"
     - Action Buttons:
       - `[ 💬 Wish on WhatsApp ]`: Opens `https://wa.me/<phone>?text=<encoded_wish>` with prefilled greeting!
       - `[ 📞 Call ]`: Opens `tel:<phone>`.
       - `[ ✉️ Email ]`: Opens `mailto:<email>`.
       - `[ Close ]`.
2. **Android (`EngineerPortalScreen.kt` & `CelebrationWishesDialog.kt`)**:
   - Fix `isCelebrationSelf`: when tapping a teammate's banner on Dashboard or in Notifications, set `isCelebrationSelf = false` so it renders the WhatsApp and Call actions instead of "Thank You!".

---

## 3. Task 3 — Web Calendar Sync Target Calendar Selector & DB Persistence (`calendarSync.js`)
Ensure Web has the Target Calendar section visible before and after sync, matching Android:

1. **Always-Visible Target Calendar Section**:
   - In `web/js/calendarSync.js`:
     - Render `TARGET CALENDAR` card **by default** (not just after syncing!).
     - If not yet synced:
       - Show target account selector/input pre-filled with the user's registered email or Google account from DB (`calendar_sync.{month}.target` or `currentUser.email`).
       - Choices: `Google Calendar (Web)`, `Apple Calendar / iCal (.ics)`, or custom account.
     - If synced:
       - Show `TARGET CALENDAR` with green `[ Synced (N) ]` badge, target account email, `Re-Sync N Days` CTA, and `Clear Month` button.
2. **Persist to Database**:
   - When sync is performed, dispatch `POST /api/users/calendar-sync` with `{ month, count, target, synced: true }` so Android and Web stay 100% in sync!

---

## 4. Task 4 — Web Progressive Back-Stack & Session Preservation (`login.html`, `userPortal.js`)
Fix the issue where pressing the back button logs the user out and opens the login screen:

1. **Fix `web/login.html` Session Destruction**:
   - Remove the destructive loop breaker in `login.html` lines 87-97 that wiped `localStorage.removeItem('rosterx_auth_user')` and `localStorage.removeItem('rosterx_auth_token')`!
   - On login, use `window.location.replace('user-portal.html')` (or `index.html`) so `login.html` is NEVER in the browser history stack.
   - If a logged-in user arrives at `login.html`, cleanly redirect them into the portal without wiping their session.
2. **Progressive Back-Stack in `web/js/userPortal.js`**:
   - Handle `popstate`:
     1. If a modal `<dialog class="rx-modal">` is open -> close the dialog first.
     2. If a subview is active (Profile, Credential Tracker, My Team) -> return to Dashboard.
     3. If on a non-dashboard tab -> switch to Dashboard tab (`selectedTab = 0`).
     4. If on Dashboard root -> exit app/browser without ever wiping credentials or logging out! Users stay logged in until they explicitly tap the "Sign Out" button!

---

## 5. Verification, Version Bump & Mandatory Push
1. Bump version to **`v2.2.0` (Build 151)**:
   - `android/app/build.gradle.kts`: `versionName = "2.2.0"`, `versionCode = 151`
   - `server.py`: `SERVER_VERSION = "2.2.0"` (synced with Dasan)
2. Safe build & ADB install:
   - `cmd /c "cd android && safe_build.bat assembleDebug"`
   - `adb install -r android/app/build/outputs/apk/debug/app-debug.apk`
3. Verify on device & Web:
   - Verify notifications in Web are raised Neumorphic cards with category pill badges (`CELEBRATION`, `SECURITY`, `OPERATIONS`), author attribution, and formatted time.
   - Verify tapping coworker's celebration opens Teammate Celebration modal with WhatsApp/Call/Email actions.
   - Verify Web Calendar Sync has Target Calendar section with account selector and syncs with Android.
   - Verify pressing back on Web closes modals/subviews without logging out.
   - Verify cleared notifications are permanently deleted on server and never reappear after reinstall.
4. Commit with semantic message, update `AGENT_HANDOFF.md` + `android/AGENT_HANDOFF.md`, and **immediately run `git push origin main`**!
