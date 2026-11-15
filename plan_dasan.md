# 📋 Execution Plan for Dasan (Backend & Core Orchestration)

**Status:** `COMPLETED` (Unified `last_updated_date` parameter standard implemented and verified in v1.25.1 Build 108)

---

## 1. Shared API & Data Contract

### 🔐 Endpoint Enhancement: Flexible Renewal Date
* **Endpoint**: `POST /api/credentials/trackers/<tracker_id>/renew`
* **Request Payload (Optional Body)**:
  ```json
  {
    "last_updated_date": "2026-11-15"
  }
  ```
  - If `last_updated_date` is provided (format `YYYY-MM-DD`):
    - Validates date format.
    - Validates date is not in the future (today or earlier).
    - Validates that the renewal date falls within the validity window (not already expired).
    - Sets `last_updated_date` to the provided date.
  - If `last_updated_date` is omitted or empty, fallback to today's date in IST (`get_ist_now().strftime("%Y-%m-%d")`).
  - Recalculates `expiry_date`, `days_remaining`, and `status`.

* **PUT Endpoint (Already Supported)**:
  * `PUT /api/credentials/trackers/<tracker_id>` accepts `{"last_updated_date": "YYYY-MM-DD"}` along with any other fields to update.

---

## 2. Technical Blueprint for Dasan

### 📁 Primary File Modified:
* `server.py`

### 🛠️ Verification:
1. Verified `POST /api/credentials/trackers/<id>/renew` with `{"last_updated_date": "2026-11-10"}`.
2. Verified future date rejection (`400 Bad Request`).
3. Verified already-expired renewal date rejection (`400 Bad Request`).
4. Verified empty body defaults to today IST.
