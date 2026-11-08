---
name: rosterx-ui
description: Design system tokens, dynamic date derivation standards, Neumorphic components, responsive tabbed architecture, and cross-platform Android Jetpack Compose parity guidelines for Project RosterX.
---

# RosterX UI Design System, Date Standards & Cross-Platform Parity

This skill documents the UI design tokens, component architecture, time standard rules, and Android Jetpack Compose parity alignment for **Project RosterX**.

---

## 1. Unified Timezone & Date Standards

All date, month, and shift operations across RosterX must use the single authoritative date helper:
[`admin-web/js/dateUtils.js`](file:///c:/shefeeq/PYTHON/RosterX/admin-web/js/dateUtils.js).

### Core Standards:
- **Timezone Anchor:** `Asia/Kolkata` (Indian Standard Time, UTC+5:30).
- **Zero Hardcoded Months:** Never hardcode month arrays (`['JANUARY', ...]`) or fallback literals (e.g. `'OCTOBER'`).
- **Period Key Format:** `YYYY-MM` (e.g., `2026-11`).
- **Core Date Functions:**
  - `getIstNow()`: Returns the exact current `Date` adjusted to IST.
  - `currentPeriodKey()`: Returns `YYYY-MM` for the current IST month.
  - `parsePeriodKey(key)`: Parses `{ year, month }`.
  - `daysInMonth(year, month)`: Computes exact total days (28, 29, 30, 31), honoring leap years.
  - `monthLabel(key, style)`: Formats labels like `"November 2026"` or `"NOVEMBER 2026 (NOV-26)"`.
  - `populatePeriodSelect(selectEl, options)`: Dynamically populates `<select>` elements.
  - `todayFormatted(style)`: Returns formatted today string (e.g. `"Friday, November 6, 2026"`).

---

## 2. Neumorphic Design Tokens

The web presentation mirrors the Android Compose design system defined in:
[`android/app/src/main/java/com/rosterx/ui/neumorphic/NeumorphicTokens.kt`](file:///c:/shefeeq/PYTHON/RosterX/android/app/src/main/java/com/rosterx/ui/neumorphic/NeumorphicTokens.kt).

### Token Mappings:
| Token Property | Light Mode | Dark Mode | Semantic Meaning |
| :--- | :--- | :--- | :--- |
| `--nm-canvas` | `#EEF2F6` | `#1E293B` (or `#0F172A`) | Main application background |
| `--nm-surface` | `#EEF2F6` | `#1E293B` | Default card and panel surface |
| `--nm-surface-contrast` | `#E2E8F0` | `#334155` | Table headers & elevated pills |
| `--nm-text` | `#1E293B` | `#F8FAFC` | Primary text |
| `--nm-text-muted` | `#64748B` | `#94A3B8` | Subtitles, labels, timestamps |
| `--nm-border` | `rgba(203, 213, 225, 0.7)` | `rgba(255, 255, 255, 0.08)` | Card and modal borders |
| `--nm-accent` | `#FF5252` | `#FF5252` | Primary active accent (Salmon) |
| `--nm-accent-coral` | `#FF6B6B` | `#FF6B6B` | Secondary accent / glow |
| `--nm-shadow-dark` | `rgba(146, 163, 184, 0.55)` | `rgba(11, 17, 32, 0.80)` | Bottom-right drop shadow |
| `--nm-shadow-light` | `rgba(255, 255, 255, 0.90)` | `rgba(71, 85, 105, 0.25)` | Top-left specular highlight |

### 3-State Theme Controller:
- Values: `'system'` | `'light'` | `'dark'` stored in `localStorage.getItem('rosterx_theme')`.
- Anti-FOUC Guard: Inline `<script>` in `<head>` applies `data-theme` and `.dark` before first paint.

---

## 3. Architecture & Android Parity

Both the Admin Operations Console and Native Android app adhere to the synchronized schema:

| Tab Index | Tab ID | Label | Phosphor Icon | Compose Destination | Purpose |
| :---: | :--- | :--- | :--- | :--- | :--- |
| **1** | `dashboard` | Operations Dashboard | `ph-gauge` | `AdminDashboardTab` | Live 24/7 NOC Coverage Radar, Rule Violations Alert Stream, Operational Audit Stream |
| **2** | `matrix` | Roster Matrix | `ph-table` | `MonthRostersTab` | 31-day shift grid with sticky engineer name column (`.nm-table-sticky-col`), shift filtering chips |
| **3** | `team` | Team Directory | `ph-users-three` | `ManageTeamTab` | Engineer profiles, lifecycle statuses (Active/Resigned), weekly off day anchors, credentials |
| **4** | `swaps` | Swaps & Leaves | `ph-arrows-left-right` | `RequestsTab` | Operational mutual swap requests, validation queue, approval actions |
| **5** | `broadcast` | Broadcast Console | `ph-megaphone` | `NotificationsTab` | Multi-channel broadcast dispatch (In-App, FCM Push to Status Bar, Resend Email), templates, read receipts |
| **6** | `diagnostics` | Hub & Diagnostics | `ph-dots-three-circle` | `ServerManagementTab` | Host uptime, Atlas roundtrip ping latency, RSS memory, live terminal logs, AI Studio launcher |

---

## 4. Operational NOC Shift Codes & Timings

| Shift Code | Shift Name | Official Hours | Category | Notes |
| :---: | :--- | :---: | :---: | :--- |
| `FS` | First Shift | 07:30 – 16:30 | Day | Peak morning handoff |
| `FG` | First General | 08:30 – 17:30 | Day | General morning duty |
| `G` | General | 09:30 – 18:30 | Day | Core daytime desk duty |
| `SG` | Second General | 11:00 – 20:00 | Second | Midday to evening |
| `SS` | Second Shift | 13:00 – 22:00 | Second | Afternoon & evening monitoring |
| `NS` | Night Shift | 21:00 – 07:30 (next day) | Night | Strict 1 engineer cap; mandatory 48h rest following |
| `OFF` | Weekly Off | Full 24h rest | Off | Default 2 days per week (e.g. Sat/Sun) |
| `L` | Approved Leave | Full 24h leave | Leave | Pre-approved time off |
| `SW*` | Swapped Duty | Variable | Swap | Format: `SW` + Engineer Initial (e.g., `SWM`, `SWDY`) |

