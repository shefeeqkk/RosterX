# 👵 Aunty (Antigravity) ⇄ 🤖 Kunjappan (Android Studio) Handoff Bridge

> **"Aunty looks after the backend & servers; Kunjappan builds the native Android app!"**  
> *Last Synced by Kunjappan:* **2026-10-27 16:15 IST** ☕⚡

---

## 📌 Active System State & Monorepo Location
- **Unified Workspace**: `c:\shefeeq\PYTHON\RosterX`
- **Native Android Project Root**: `c:\shefeeq\PYTHON\RosterX\android`
- **Semantic Version**: **`1.6.0`** (Android `versionCode: 18`, Server `SERVER_VERSION = "1.6.0"`)
- **Active Cloudflare Public Gateway**:  
  🌐 `https://such-anonymous-out-stopping.trycloudflare.com`  
  📱 **Android Base URL**: `https://such-anonymous-out-stopping.trycloudflare.com/api/`
- **Local Asset Sync File**: `app/src/main/assets/server_url.txt` (Synced with active Cloudflare URL)
- **Database Engine**: MongoDB Atlas `Cluster0` (Database: `rosterx`, Collection: `engineers`, `notifications`, `operational_policies`, Status: `CONNECTED`)
- **Notification Engine**: Resend API active, alerts routed to `kkshefeeq005@gmail.com`

---

## 🌊 Animated Fluid Neumorphic Bottom Navigation Dock (v1.6.0)

1. **Fluid Dock Surface & Concave Cubic Bezier Notch (`FluidNeumorphicBottomNav.kt`)**:
   - Continuous monochromatic `#EEF2F6` floating dock featuring an organic mathematical concave wave cutout (`createFluidDockPath`).
   - Dynamically recalculates notch center `animatedNotchOffsetX` along the X-axis directly beneath the active tab index.

2. **Elevated Active Circular Bubble**:
   - `54.dp` circular bubble floating `-22.dp` proud above the dock line.
   - Dual-opposite Neumorphic elevation with top-left white highlight and bottom-right slate occlusion drop shadow.
   - Contains the glowing coral icon (`#FF5252`) and an illuminated coral pip/dot (`5.dp`).

3. **Spring Physics & Lateral Squash & Stretch Deformation**:
   - Tab switching triggers a spring-dampened motion curve (`Spring.DampingRatioMediumBouncy`, `Spring.StiffnessLow`).
   - During lateral motion, the bubble physics deforms smoothly with a jelly squash & stretch (`scaleX: 1.18`, `scaleY: 0.82`) before snapping into place.
   - Inactive icons rest flush with micro-scale click feedback (`scale(0.88)`).

4. **Integration Across All Main Navigation Screens**:
   - Integrated into **Admin Main Command Console** (`AdminMainScreen.kt`) across all 5 admin tabs (`Dashboard`, `Team`, `Rosters`, `Broadcast`, `Settings`).
   - Integrated into **Engineer Portal** (`EngineerPortalScreen.kt`) across all 5 portal tabs (`Dashboard`, `Team Shifts`, `My Calendar`, `Month Roster`, `Settings`).

5. **Web Parity Styles (`css/neumorphic.css`)**:
   - Added `.nm-fluid-dock-container`, `.nm-floating-bubble`, `.nm-floating-pip`, and `.nm-fluid-tab-btn` styling to `admin-web/css/neumorphic.css`.

6. **Version Bump to `1.6.0` (`versionCode: 18`)**:
   - Updated `app/build.gradle.kts` (`versionName = "1.6.0"`, `versionCode: 18`).
   - Updated `server.py` (`SERVER_VERSION = "1.6.0"`).
   - Gradle Sync completed with status **Sync finished successfully**.

---

## 📝 Milestone Checklist
- [x] Monorepo migration to `c:\shefeeq\PYTHON\RosterX\android`
- [x] Complete Neumorphic (Soft UI) visual system implementation across all screens
- [x] Animated Fluid Neumorphic Bottom Navigation Dock with wave notch & floating bubble
- [x] Lateral squash & stretch spring physics during tab transitions
- [x] Integrated across Admin Console and Engineer Portal screens
- [x] Version bump to 1.6.0 (versionCode 18)
