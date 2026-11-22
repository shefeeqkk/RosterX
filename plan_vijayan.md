# 📋 Execution Plan for Vijayan (UI Specialist — Android & Web)

**Status:** `COMPLETED BY VIJAYAN` (Sprint v2.3.17 - Build 169: Zero-Lag Dashboard Tab Swiping, FlowRow Collection Chips & Uniform 5dp Touch Slop)

---

## 🔐 Mandatory Production Rule from Gafoor (System Architect 🕶️)
> **Public GitHub Repo + Auto-Deploying Render Production Server:**
> - **ZERO hardcoded personal names, usernames, emails, phone numbers, passwords, or secrets** anywhere in HTML, CSS, JS, Kotlin, comments, or placeholders.
> - **Mandatory `git push origin main`** immediately after verification so Render (`https://api.rosterx.online`) deploys immediately!

---

## 🚀 Active Sprint: Task 13 — Eliminate Dashboard Swipe Jitter & Millisecond Lag (v2.3.17 - Build 169)

### 1. Root Cause Breakdown (Confirmed via Video Inspection `recording.mp4`)
1. **Child Horizontal Scroll Contention (`LazyRow`)**:
   - In `AdminDashboardTab.kt` (line 374), the "COLLECTION BREAKDOWN" chips were placed inside a `LazyRow`.
   - When the user's thumb touched down in the lower-middle half of the Dashboard (y ≈ 1750–1850) and began a horizontal swipe, the child `LazyRow` intercepted and consumed the horizontal touch events first before yielding to the parent `HorizontalPager`.
   - **Remediation**: Convert `LazyRow` to a modern `FlowRow` (or 2-column wrapped layout) with `Arrangement.spacedBy(8.dp)`. This displays all 5 collection chips neatly within the card with zero horizontal scroll interception.

2. **Tab 0 Hierarchy Flattening**:
   - In both `AdminMainScreen.kt` and `EngineerPortalScreen.kt`, Tab 0 was uniquely wrapped in:
     `Column(modifier = Modifier.fillMaxSize()) { ... Box(modifier = Modifier.weight(1f)) { Dashboard... } }`
     while Tabs 1–4 were flat direct children of the pager.
   - **Remediation**: Flatten Tab 0 to be a direct child (`AdminDashboardTab` and `DashboardView`), embedding `AppUpdateBannerCard` directly inside the screen's content or header when present.

3. **Uniform 5dp Touch Slop Across Hierarchy**:
   - In `SwipeableTabContainer.kt`, the inner `CompositionLocalProvider(LocalViewConfiguration provides currentViewConfig)` was restoring the bulky ~18dp–24dp Android platform touch slop on page contents.
   - **Remediation**: Remove the inner slop restorer so the calibrated `5.dp.toPx()` touch slop applies uniformly across the entire pager hierarchy, giving instant 1:1 tactile finger engagement from the very first millimeter.

---

### 2. Versioning & Build Protocol
- `android/app/build.gradle.kts`: `versionName = "2.3.17"`, `versionCode = 169`
- `server.py`: `SERVER_VERSION = "2.3.17"`
- `web/sw.js`: `CACHE_NAME = 'rosterx-pwa-v2.3.17'`
- `web/user-portal.html`, `web/index.html`: bump cache busters to `2.3.17`
- Safe build: `cd android && .\safe_build.bat assembleDebug`
- Push to ADB: `adb install -r android/app/build/outputs/apk/debug/app-debug.apk`
- Commit & push: `git push origin main`
