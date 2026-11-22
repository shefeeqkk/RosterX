# 📋 Execution Plan for Vijayan (UI Specialist — Android & Web)

**Status:** `COMPLETED BY VIJAYAN` (Sprint v2.3.18 - Build 170: Boundary Gesture Contention Elimination, Native Clean Clamping & Zero-Lag Tab 0 / Tab 4 Swiping)

---

## 🔐 Mandatory Production Rule from Gafoor (System Architect 🕶️)
> **Public GitHub Repo + Auto-Deploying Render Production Server:**
> - **ZERO hardcoded personal names, usernames, emails, phone numbers, passwords, or secrets** anywhere in HTML, CSS, JS, Kotlin, comments, or placeholders.
> - **Mandatory `git push origin main`** immediately after verification so Render (`https://api.rosterx.online`) deploys immediately!

---

## 🚀 Active Sprint: Task 14 — Eliminate Boundary Dead-Zone & Millisecond Lag on Tab 0 (Dashboard) & Tab 4 (More) (v2.3.18 - Build 170)

### 1. Root Cause Breakdown (Confirmed via Video Inspection `recording_213644.mp4` & Code Audit)
1. **Boundary-Only NestedScroll Interception (`boundaryNestedScrollConnection`)**:
   - In `SwipeableTabContainer.kt`, a custom `boundaryNestedScrollConnection` was uniquely conditioned on:
     `val isAtStart = pagerState.currentPage == 0 && available.x > 0f`
     `val isAtEnd = pagerState.currentPage == (pagerState.pageCount - 1) && available.x < 0f`
   - On middle tabs (Tabs 1, 2, 3), `currentPage` is neither 0 nor 4. So `isAtStart` and `isAtEnd` were always false. The nested scroll connection remained completely dormant, permitting `HorizontalPager` with 5dp touch slop to track fingers instantaneously without resistance.
   - On Tab 0 and Tab 4, any micro-movement or thumb wobble against the boundary upon touchdown triggered `onPostScroll`, consuming delta, launching coroutines, and mutating `overscrollAnim.value`.
   - The outer `Box` was wrapped in `.graphicsLayer { translationX = overscrollAnim.value }`, creating visual fighting, boundary displacement, and a ~150ms dead-zone before inward swiping could proceed.
   - **Remediation**: Completely stripped `boundaryNestedScrollConnection` and outer `graphicsLayer` translation from `RealtimeSwipeableTabPager`. Enforced Clean Native Clamping so Tab 0 and Tab 4 stop cleanly at screen edges and initiate inward swiping with 100% instant, zero-lag responsiveness identical to middle tabs.

2. **Clean Fractional Tracking on Bottom Nav**:
   - Removed `boundaryOverscrollAnim` from `FluidNeumorphicBottomNav`, `AdminMainScreen`, and `EngineerPortalScreen`.
   - Bottom navigation bubble strictly tracks `fractionalPosition = { pagerState.currentPage + pagerState.currentPageOffsetFraction }` with zero boundary skew.

---

### 2. Versioning & Build Protocol
- `android/app/build.gradle.kts`: `versionName = "2.3.18"`, `versionCode = 170`
- `server.py`: `SERVER_VERSION = "2.3.18"`
- `web/sw.js`: `CACHE_NAME = 'rosterx-pwa-v2.3.18'`
- `web/user-portal.html`, `web/index.html`: bump cache busters to `2.3.18`
- Safe build: `cd android && .\safe_build.bat assembleDebug`
- Push to ADB: `adb install -r android/app/build/outputs/apk/debug/app-debug.apk`
- Commit & push: `git push origin main`
