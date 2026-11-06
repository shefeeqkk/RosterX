# Project RosterX — Cross-Platform Parity Specification

> **Audience:** Kunjappan (Android Studio Mobile Agent) & Aunty (Antigravity Agent)  
> **Source of Truth:** Android Jetpack Compose (`NeumorphicTokens.kt`, `Theme.kt`, `AdminMainScreen.kt`, `EngineerPortalScreen.kt`) & Web Console (`design-tokens.json`, `tokens.css`, `dateUtils.js`)  
> **Current Version:** `1.14.0` (Android `versionCode: 43`)

---

## 1. Unified 5-Tab Architecture Alignment

Both platforms strictly adhere to the identical 5-tab schema for Admin and 4-tab schema for Engineers:

### A. Admin Operations Console:
| Tab # | Web Tab ID | Android Destination | Icon (Phosphor / Material) | Label | Purpose |
| :---: | :--- | :--- | :--- | :--- | :--- |
| **1** | `dashboard` | `DashboardTab` | `ph-gauge` / `Icons.Default.Speed` | Operations Console | Today's 24/7 NOC Coverage Radar (#1), Live Rule Violations Alert Stream (#2), Operational Audit Stream (#3) |
| **2** | `matrix` | `RosterMatrixTab` | `ph-table` / `Icons.Default.CalendarMonth` | Roster Matrix | 31-day shift grid, sticky pinned engineer names, dynamic month navigator, shift filter chips |
| **3** | `team` | `ManageTeamTab` | `ph-users-three` / `Icons.Default.People` | Team Directory | Engineer profiles, lifecycle statuses (Active/Resigned), weekly off day anchors, credentials |
| **4** | `swaps` | `RequestsTab` | `ph-arrows-left-right` / `Icons.Default.SwapHoriz` | Swaps & Leaves | Operational mutual swap requests, validation queue, approval actions |
| **5** | `diagnostics` | `DiagnosticsTab` | `ph-dots-three-circle` / `Icons.Default.MoreHoriz` | Hub & Diagnostics | Host uptime, Atlas roundtrip ping latency, RSS memory, live terminal logs, AI Studio launcher |

### B. Engineer User Portal:
| Tab # | Web Tab ID | Android Destination | Icon | Label | Purpose |
| :---: | :--- | :--- | :--- | :--- | :--- |
| **1** | `schedule` | `MyScheduleTab` | `ph-calendar-check` | My Schedule | Today's shift badge, countdown to next duty shift, personal assigned monthly duties |
| **2** | `handover` | `DeskHandoverTab` | `ph-users-three` | Desk Handover | Live active duty briefing (Morning, Second, Night, Off) with engineer names & contact info |
| **3** | `swap` | `SwapRequestTab` | `ph-arrows-left-right` | Request Swap | Form to submit mutual shift swap requests with peers |
| **4** | `overview` | `MonthlyOverviewTab` | `ph-calendar` | Monthly Overview | Full personal monthly shift calendar with shift color chips and Excel export |

---

## 2. Design Tokens & Neumorphic Physics

Android Compose definitions in `NeumorphicTokens.kt` are the canonical reference:

```kotlin
// Android Jetpack Compose Tokens (NeumorphicTokens.kt)
val LightCanvas = Color(0xFFEEF2F6)
val LightSurface = Color(0xFFEEF2F6)
val LightSurfaceContrast = Color(0xFFE2E8F0)
val LightTextMain = Color(0xFF1E293B)
val LightTextMuted = Color(0xFF64748B)

val DarkCanvas = Color(0xFF1E293B)
val DarkSurface = Color(0xFF1E293B)
val DarkSurfaceContrast = Color(0xFF334155)
val DarkTextMain = Color(0xFFF8FAFC)
val DarkTextMuted = Color(0xFF94A3B8)

val AccentSalmon = Color(0xFFFF5252)
val AccentCoral = Color(0xFFFF6B6B)
```

Web CSS custom properties in `tokens.css` match these values identically:
- `--nm-canvas`: `#EEF2F6` (light) / `#1E293B` (dark)
- `--nm-surface`: `#EEF2F6` (light) / `#1E293B` (dark)
- `--nm-surface-contrast`: `#E2E8F0` (light) / `#334155` (dark)
- `--nm-accent`: `#FF5252`
- `--nm-accent-coral`: `#FF6B6B`

---

## 3. Dynamic Date Engine & Timezone Rule

- **Universal Anchor:** `Asia/Kolkata` (IST, UTC+5:30).
- Neither Android nor Web should use hardcoded month arrays. All month generation derives dynamically from year and month numbers.
- Default period opens to the current operational month (e.g., `2026-11` for November 2026).
- When a shift ends, the hero timer computes and displays: `"Next Shift Starts in: [HHh MMm SSs]"` until the next scheduled duty.

---

## 4. Mobile Floating Action Button (FAB) Clearances

- The Floating AI Bot button must never block the bottom navigation dock.
- Position: strictly `bottom-28` (or `bottom-24`) on mobile screens to preserve at least 16dp / 16px of clearance above the dock.
- Styling: Pure 3D cutout bot without artificial round background discs.

