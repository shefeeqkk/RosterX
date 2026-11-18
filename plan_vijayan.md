# 📋 Execution Plan for Vijayan (Web Portal Hotfix & PWA Cache Parity)

**Status:** `READY FOR VIJAYAN` (Hotfix `v2.0.1` — Build 144 Web Portal Sync Crash, Credential Tracker, Dashboard, Month Roster Grid & Bell Badge Fix)

---

## 🔐 Mandatory Production Rule from Gafoor (System Architect 🕶️)
> **Public GitHub Repo + Auto-Deploying Render Production Server:**
> - **ZERO hardcoded personal names, usernames, emails, phone numbers, passwords, or secrets** anywhere in HTML, CSS, JS, Kotlin, comments, or placeholders.
> - **Mandatory `git push origin main`** immediately after verification so Render (`https://api.rosterx.online` / `https://rosterx.online`) deploys immediately!

---

## 1. Task 1 — Fix Sync Crash (`trackers.filter is not a function`) & Credential Tracker (`userPortal.js`, `credentialTracker.js`)
**Root Cause:** `GET /api/credentials` returns `{ success: true, data: { trackers: [...], total_count, expiring_soon_count, expired_count } }`. Both `userPortal.js:364` and `credentialTracker.js:84` read `payload.trackers || payload.data || []`, receiving the wrapper **object** instead of the array — throwing `trackers.filter is not a function`, aborting `refreshAllData()` before rosters/dashboard/calendar load, and leaving Credential Tracker stuck on `"Loading credential trackers..."`!

### Implementation:
1. **Safe Array Unwrapper** in BOTH `web/js/userPortal.js` (`refreshAllData`, line ~364) and `web/js/credentialTracker.js` (`load`, line ~84):
   ```js
   const trackers = Array.isArray(payload?.data?.trackers)
     ? payload.data.trackers
     : Array.isArray(payload?.trackers)
       ? payload.trackers
       : Array.isArray(payload?.data)
         ? payload.data
         : [];
   ```
   - Also defensively unwrap `logs` in `credentialTracker.js` (`openAuditLogsModal`, line ~516) with `Array.isArray(data?.logs) ? data.logs : (Array.isArray(data?.data?.logs) ? data.data.logs : [])`.
   - Wrap the `credRes` block in `refreshAllData()` in its own safe guard so a single failing widget NEVER aborts roster loading or `renderActiveView()`.
2. **Eliminate Duplicate `← Credential Tracker` Header**:
   - Currently `userPortal.js` renders a subview top bar (`← Credential Tracker`) AND `credentialTracker.js` renders a second `← Credential Tracker` header directly below it.
   - Add an option `hideBackButton: true` / `embedded: true` (or remove the redundant top back button when hosted inside `userPortal.js`'s subview bar, keeping only the action buttons: Refresh, Audit Logs, Add Tracker) so there is ONE clean header!

---

## 2. Task 2 — Fix Roster Data Normalization & Operational Admin Inclusion (`userPortal.js`)
**Root Cause:**
- MongoDB roster documents store `e.shifts` as a **0-indexed Array** (`shifts[0]` = Day 1, `shifts[17]` = Day 18) OR a **1-indexed Object** (`shifts["1"]` = Day 1), and rows may live under `rosterDoc.engineers` OR `rosterDoc.roster`.
- `userPortal.js:417` returned `matched?.shifts || {}` directly and read `shifts[String(d)]`, causing **off-by-one shift corruption** when `shifts` is an array!
- `getRosterEngineers()` line 424 checked `if (role === 'admin' || role === 'manager') return false;` BEFORE checking `e.include_in_roster === true`, accidentally dropping operational admins who are on the shift roster!

### Implementation (`web/js/userPortal.js`):
1. **Normalize `getRosterMapForEngineer(rosterDoc, eng)`**:
   - Read rows from `rosterDoc.engineers?.length ? rosterDoc.engineers : (rosterDoc.roster || [])`.
   - Match engineer flexibly by `username`, `name`, `full_name`, `fullName` (trimmed, case-insensitive).
   - Convert `matched.shifts` into a clean **1-indexed map (`"1".."31"`)**:
     - If `Array.isArray(rawShifts)`: map index `i` → key `String(i + 1)` (only when value is non-empty string!).
     - If plain object: copy keys `String(k).trim()` → trimmed non-empty string value.
2. **Fix `getRosterEngineers(rosterDoc)`**:
   - Check `if (e.include_in_roster === true)` FIRST (keep on roster!), then `if (e.include_in_roster === false || role === 'manager') return false;`, and exclude generic system admin accounts (`admin`, `sysadmin`, `administrator`, or `role === 'admin'` with `include_in_roster !== true` unless they appear in `rosterDoc`!).
   - Sort cleanly by week-off pair order + `display_order` (matching Android `sortEngineersByWeekOffs`).

---

## 3. Task 3 — Month Roster Blank Unpublished Cells + Right-Side `WRK` / `NS` / `OFF` Counters (`userPortal.js`)
**Root Cause:**
- `userPortal.js:1156` hardcodes `getShiftVisual(shifts[String(d)] || 'OFF')` → every empty cell in upcoming months (e.g. `DECEMBER 2026`) renders bright yellow **`OFF`** instead of **blank**!
- Right-side summary columns (`WRK`, `NS`, `OFF`) were completely missing!

### Implementation (`web/js/userPortal.js`):
1. **Blank Transparent Cells for Unassigned / Unpublished Dates**:
   - In `renderMonthRoster()`: read `const rawShift = String(shifts[String(d)] || '').trim();`.
   - If `!rawShift`, render an empty transparent cell: `<td style="background:transparent;color:transparent;"></td>` (NEVER `'OFF'`!).
   - Detect `const hasAnyShifts = filtered.some((eng) => Object.keys(this.getRosterMapForEngineer(doc, eng)).length > 0);`.
   - If `!hasAnyShifts`, display a sleek subtle Neumorphic notice pill above the grid: `"No roster published for <Month Year> yet"`.
2. **Add Right-Side Summary Counter Columns (`WRK`, `NS`, `OFF`) — 1:1 Android Parity**:
   - Right after the day columns in `<thead>`:
     - `<th style="color:#059669;">WRK</th>`
     - `<th style="color:#4F46E5;">NS</th>`
     - `<th style="color:#D97706;">OFF</th>`
   - For each engineer row in `<tbody>`, compute across `1..daysInMonth` ONLY from assigned non-blank shifts (exact NOC standard matching Android `EngineerPortalScreen.kt:5035-5040`):
     - `WRK`: count of `FS`, `FG`, `G`, `SG`, `SS`, `NS`
     - `NS`: count of `NS`
     - `OFF`: count of `OFF`, `L`, `CO`, `AL`, `SL`, `ML`, or starts with `SW`
   - Render the 3 styled stat `<td>` cells at the end of each engineer row (and 3 empty `<td>` cells at the end of the `DAY COVERAGE (≥3)` and `NIGHT COVERAGE (1)` footer rows so column alignment is pixel-perfect!).
3. **Dashboard & Team Shifts Unpublished Guard**:
   - When today/tomorrow has no shift assigned in `renderEngineerDashboard`, `renderManagerDashboard`, and `renderTeamShifts`, show `'—'` / `'Not Scheduled'` instead of faking `'OFF'`.

---

## 4. Task 4 — Unread Notification Bell Badge Parity (`userPortal.js`)
- In `refreshAllData()` (`lines ~356-361`):
  - Read `const dismissedSet = new Set((notifRes.value?.dismissed_notification_ids || []).map(String));`
  - Filter `this.notifications` by BOTH `!isExpiredCelebration(n, todayIso)` AND `!dismissedSet.has(String(n.id || n._id || ''))`!
  - Result: Web Bell badge matches Android (`3` unread instead of `45`!).

---

## 5. Task 5 — PWA Service Worker & Script Cache Bust (`web/sw.js`, `web/user-portal.html`, `web/index.html`)
- Bump Service Worker cache name in `web/sw.js` (e.g., `rosterx-pwa-v2.0.1`) so installed desktop/mobile PWAs immediately evict old cached `userPortal.js` & `credentialTracker.js`.
- Update script import query versions (`?v=2.0.1`) in `web/user-portal.html` and `web/index.html`.

---

## 6. Verification, Version Bump & Mandatory Push
1. Bump version to **`v2.0.1` (Build 144)**:
   - `android/app/build.gradle.kts`: `versionName = "2.0.1"`, `versionCode = 144`
   - `server.py`: `SERVER_VERSION = "2.0.1"`
2. Verify all JS syntax (`node --check web/js/userPortal.js web/js/credentialTracker.js web/sw.js`) and run live HTTP/browser checks against local `server.py`:
   - Zero `trackers.filter is not a function` warnings.
   - Dashboard, My Calendar (November 2026), Credential Tracker (single clean header + real servers loaded), and Month Roster (accurate day alignment, right-side `WRK`/`NS`/`OFF` counters, December 2026 blank cells + unpublished banner) all verified!
   - Bell badge excludes dismissed notifications.
   - Run `cmd /c "cd android && safe_build.bat assembleDebug"` (version sync) if needed or keep Android APK version synced on device.
3. Commit with semantic message (`fix(web): resolve trackers.filter sync crash, normalize 0-indexed roster shifts, add WRK/NS/OFF counters, blank upcoming months & sync bell badge v2.0.1 (Build 144)`), update `AGENT_HANDOFF.md` + `android/AGENT_HANDOFF.md`, and **immediately run `git push origin main`**!
