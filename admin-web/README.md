# RosterX — Dynamic Admin Engine & AI Smart Roster Generator

A zero-server enterprise shift roster management platform designed for a 24/7 Network Operations Center (NOC) team, powered by **Google Gemini API Structured Outputs**, **Swap-First Arbitration**, and **MongoDB Atlas M0**.

---

## 🚀 Key Modules & Architecture

### 1. Zero-Hardcoding Dynamic Architecture
- **Zero-Server Backend:** Standalone web application (HTML5, Tailwind CSS, Modular JavaScript) interacting directly with **MongoDB Atlas Free Tier (M0)** via Atlas App Services / Atlas HTTPS Endpoints, with offline-ready `localStorage` persistence.
- **Dynamic Entities:** Engineer records, operational constraints, shift definitions, and swap contracts are completely dynamic and editable via dedicated UI modals.

---

### 2. Module A: Engineer Directory & Lifecycle Management (CRUD)
- **Directory Schema:**
  - `id`: Unique identifier (e.g., `eng_01`)
  - `name`: Display name on roster (e.g., `SHEFEEQ`)
  - `full_name`: Official name (e.g., `Shefeeq K K`)
  - `email`: Work / notification email (used for password reset OTP)
  - `phone`: Contact number
  - `role`: Access role (`admin` or `engineer`)
  - `status`: Lifecycle status (`Active`, `Notice Period`, `Onboarding`, `Resigned`)
  - `join_date` & `resignation_date`: Start date and effective exit date
  - `default_weekly_offs`: Array of two day-names (e.g., `["Monday", "Tuesday"]`)
  - `preferred_shifts`: List of preferred shifts (e.g., `["G", "FG"]`)
- **Lifecycle Controls:**
  - **Exclusion/Resignation:** Marking an engineer as `Resigned` or setting an exit date automatically excludes them from upcoming month roster generations while preserving historical rosters.
  - **New Joiners:** New engineers can be added mid-month with shifts generated only from their `join_date` onward.

---

### 3. Module B: Editable Operational Constraints & Shift Engine
- **Adjustable Headcount Parameters:**
  - **Night Shift (`NS`):** Strictly `1` engineer per night (configurable).
  - **First Shift (`FS`):** Minimum `1` engineer daily.
  - **Second Shift (`SS`):** Minimum `1` engineer daily.
  - **Daytime Overlap:** Minimum required staffing across `FG`, `G`, and `SG` (default `3`).
- **Fatigue & Circadian Safety Guards:**
  - **Post-Night Rest:** Mandatory 24h–48h rest (`OFF` or Swap Code `SW...`) immediately following `NS`. Sequential `NS` $\rightarrow$ `FS` or `NS` $\rightarrow$ `FG` is strictly blocked.
  - **Consecutive Work Cap:** Maximum 6 consecutive working days before a mandatory `OFF`.
  - **Night Shift Blocks:** Night shifts grouped into contiguous blocks (3 to 5 nights).
- **Custom Directives Input:**
  - A multi-line textarea allowing lead engineers to inject temporary operational directives (e.g., *"Maintenance window on Nov 14: Ensure senior engineer on SS"*), which are dynamically appended to the Gemini prompt context.

---

### 4. Module C: Shift Codes & Reference Timings
| Code | Shift Name | Timings | Category / Color Theme (Hex) |
| :--- | :--- | :--- | :--- |
| **FS** | First Shift | 07:30 AM – 04:30 PM | Soft Mint (`#bbf7d0`, text `#14532d`) |
| **FG** | First General Shift | 08:30 AM – 05:30 PM | Forest Green (`#3f6212`, text `#ffffff`) |
| **G** | General Shift | 09:30 AM – 06:30 PM | Periwinkle Blue (`#93c5fd`, text `#1e3a8a`) |
| **SG** | Second General Shift | 11:00 AM – 08:00 PM | Terracotta Orange (`#c2410c`, text `#ffffff`) |
| **SS** | Second Shift | 01:00 PM – 10:00 PM | Salmon / Peach (`#fdba74`, text `#7c2d12`) |
| **NS** | Night Shift | 09:00 PM – 07:30 AM | Slate Grey (`#64748b`, text `#ffffff`) |
| **OFF** | Weekly Off | Rest Day | Bright Yellow (`#fef08a`, text `#713f12`) |
| **SW[Name]**| Swap Code (Treated as OFF) | Peer Covers Shift | Light Slate (`#e2e8f0`, text `#334155`, dashed border) |
| **L** | Approved Leave | Standalone Leave | Red (`#ef4444`, text `#ffffff`) |

*Standardized Swap Code Notation:*
- Prefix `SW` designates an engineer as **OFF** on that day.
- Followed by the initial(s) of the covering peer (e.g., `SWM` = Swap with Manoj, `SWSH` = Swap with Shefeeq, `SWSI` = Swap with Silambarasan, `SWDY` = Swap with Dyani, `SWDH` = Swap with Dhanush).
- If multiple engineers share the same first initial, the first 2 letters are used for disambiguation.

---

### 5. Module D: Smart Swaps, Re-swaps & "Swap-First" Policy
- **Swap-First Rule:** Swapping is strictly prioritized over unstaffed Leave (`L`). If an engineer requests an absence, the AI searches for an eligible peer who can cover without violating rest rules or working caps. Only when zero team members are available does the system designate the absence as Leave (`L`).
- **Two Swap Modes:**
  1. **Pre-Locked Mutual Swap (Manual):** Admin specifies both parties: Engineer A receives Swap Code for Peer B on Date 1 (e.g., `SWM`), and Engineer B receives Swap Code for Peer A on Date 2 (e.g., `SWSH`). These cells are locked `🔒` prior to AI generation and treated as OFF for the requesting party.
  2. **AI-Arbitrated Swap (Smart Mode):** Admin specifies only: *"Engineer X requests off on Date D"*. Gemini evaluates the roster, picks an optimal covering engineer, and proposes a reciprocal payback date (`Re-swap`) later in the month.

---

### 6. Gemini API Integration & Structured Output
- **Dynamic Prompt Builder:** System prompt dynamically constructed with live headcount parameters, rest window days, consecutive day limits, and custom directives.
- **Strict Response Schema (JSON):**
  - `month`, `year`
  - `roster`: Array of engineers with `day`, `shift_code`, `is_swap`, `swap_partner_id`, `note`
  - `ai_arbitrated_swaps`: Array of `{ requester_id, covered_by_id, swap_date, reswap_date, reasoning }`
  - `audit_metrics`: `{ night_coverage_valid, zero_fatigue_violations, swaps_vs_leaves_ratio }`
- **Deterministic Fallback:** Includes a client-side heuristic engine guaranteeing 100% compliant generation even without an active Gemini API key.

---

### 7. Interactive UI & Spreadsheet Parity
- **Sticky Master Matrix:**
  - Sticky Engineer Names column with `#` order badge
  - Sticky double-row header: Day Numbers (1 to 31) and Days of Week (`MON`, `TUE`...) with red weekend badges
  - Row totals for `WRK` (Working Days), `NS` (Night Shifts), `OFF` (Rest Days)
  - **`STATUS` Column:** Visual lifecycle status pill (`Active`, `Notice Period`, `Onboarding`, `Resigned`)
  - **Sticky Staffing Summary Bar:** Real-time headcount counters for FS, SS, and NS with threshold warnings
- **Cell Override Popover:** Click any cell to manually switch shift codes with live headcount recalculation and `🔒` cell locking.
- **SheetJS Excel Export:** Exports `.xlsx` matching operational NOC spreadsheets with exact headers, totals, and lifecycle statuses.

---

## 💻 Running & Testing Locally

1. **Start the local HTTP server** (if not already running):
   ```bash
   cd admin-web
   python -m http.server 8000
   ```
2. Open **[http://localhost:8000](http://localhost:8000)** in your browser.
3. **Try the features:**
   - Click **`👥 Team`** to inspect the engineer directory, add a new joiner, or mark an engineer as Resigned.
   - Click **`⚙️ Constraints`** to adjust headcount targets, post-night rest, or enter custom directives.
   - Click **`🔄 Swaps & Leaves`** to register pre-locked mutual swaps or submit an AI-arbitrated absence request.
   - Click **`✨ AI Generate`** to run the Gemini AI Smart Roster Generator and view the scorecard and arbitrated swaps.
   - Click **`Export XLSX`** to download the complete spreadsheet.
   - Click **`Save & Publish`** to sync directly with MongoDB Atlas.
