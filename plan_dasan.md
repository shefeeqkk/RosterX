# 📋 Execution Plan for Dasan (Backend & Server Infrastructure Lead)

**Status:** `READY FOR DASAN EXECUTION` (Sprint v2.4.0 - Build 171: Leave & Swap Request Engine, MongoDB Atlas Schemas, 14-Day Quota Enforcement, FCM Push Alerts & Audit Logging)

---

## 🔐 Mandatory Production Rule from Gafoor (System Architect 🕶️)
> **Public GitHub Repo + Auto-Deploying Render Production Server:**
> - **ZERO hardcoded personal names, usernames, emails, phone numbers, passwords, or secrets** anywhere in code, comments, or tests.
> - Sourced strictly from environment variables and live MongoDB Atlas collections.
> - **Mandatory `git push origin main`** immediately after verification so Render (`https://api.rosterx.online`) auto-deploys cleanly.

---

## 🚀 Architectural Contract: Leave & Swap Request System (Sprint v2.4.0 - Build 171)

### 1. MongoDB Atlas Schema & Collections (`server.py`)
Create/manage collection `leave_requests` in `ATLAS_DB`:
```json
{
  "_id": "ObjectId",
  "request_id": "LR-20261122-XXXX",
  "engineer_id": "string (unique username)",
  "engineer_name": "string (display name)",
  "dates": ["YYYY-MM-DD"],
  "month_keys": ["YYYY-MM"],
  "reason": "string (optional)",
  "preferred_partner": "string (optional username)",
  "annual_year": 2026,
  "days_count": 2,
  "status": "pending", // "pending" | "approved_swap" | "approved_leave" | "rejected" | "cancelled"
  "created_at": "ISO-8601 UTC timestamp",
  "approval": {
    "approved_by": "admin_username",
    "approved_at": "ISO-8601 UTC timestamp",
    "action_type": "swap", // "swap" | "leave" | "reject"
    "swap_partner": "username",
    "swap_shift_code": "SWSH", // Pre-programmed SW-prefix codes
    "admin_note": "string"
  }
}
```

### 2. Validation & Business Logic Engine (`server.py`)
1. **Date Selection Bounds**:
   - `len(dates)` must be between 1 and 5.
   - Every date must be strictly `>= today_IST` (Zero past dates allowed).
2. **Scheduled Off-Day Check**:
   - For every requested date, inspect applicant's shift in the active monthly roster. If shift is already `'OFF'`, reject submission with clear error: *"Date YYYY-MM-DD is already your scheduled Weekly Off"*.
3. **Overlapping & Duplicate Guard**:
   - Query `leave_requests` for the same `engineer_id` where `status in ['pending', 'approved_swap', 'approved_leave']`.
   - If any requested date is already present in an active request, reject with: *"A request for YYYY-MM-DD is already pending or approved"*.
4. **Annual 14-Day Quota Enforcement**:
   - Calculate approved leave days for `engineer_id` in current calendar year (`annual_year == current_year`).
   - If `current_approved_days + len(new_dates) > 14`, reject with: *"Annual leave allowance exceeded. Max 14 days per calendar year"*.
5. **Deferred Roster Cell Modification**:
   - Submitting a request makes **ZERO** immediate changes to roster grid cells. Cells are modified strictly when admin executes approval.

### 3. API Endpoints Specification
- **`POST /api/leave/apply`** (Authenticated Engineer):
  - Validates all rules above.
  - Inserts document into `leave_requests`.
  - Dispatches FCM push notification to all active Admin device tokens:
    - Title: `New Leave/Swap Request`
    - Body: `[Applicant Name] submitted a request for [X] dates.`
    - Data: `{ "type": "leave_request", "request_id": "..." }`
- **`GET /api/leave/my-requests`** (Authenticated Engineer):
  - Returns applicant's personal request history, active count, and remaining annual leave balance (14 - approved).
- **`GET /api/leave/eligible-partners?date=YYYY-MM-DD`** (Authenticated):
  - Looks up all active team members from `ATLAS_DB.engineers`.
  - Determines each member's scheduled shift on that date.
  - Sorts members having `shift == 'OFF'` to the top of the list with flag `is_off: true`.
- **`GET /api/leave/admin-queue`** (Admin Only):
  - Returns all pending and historical leave requests with candidate swap suggestions per requested date.
- **`POST /api/leave/admin-action`** (Admin Only):
  - Body: `{ request_id, action: "swap"|"leave"|"reject", swap_partner, swap_code, admin_note }`.
  - Updates `leave_requests` document.
  - If `action == "swap"`: Updates applicant's cell to `swap_code` (e.g., SWSH) and updates partner's cell to applicant's original shift.
  - If `action == "leave"`: Updates applicant's cell to fallback code (e.g., 'LV' or 'OFF').
  - Dispatches targeted FCM push notification **strictly to the applicant**:
    - Title: `Leave Request Approved` (or `Rejected`)
    - Body: `Your request for [dates] has been approved as [Swap/Leave].`
  - Records structured audit log in `SERVER_LOGS` and `ATLAS_DB.audit_logs`.

### 4. Verification & Safe Push
1. Test endpoints via curl/script with dynamic synthetic IDs.
2. Verify zero hardcoded PII.
3. Bump `SERVER_VERSION = "2.4.0"` in `server.py`.
4. Git commit and push to `origin/main` for auto Render deployment.
