# 📋 Execution Plan for Dasan (Backend & Core Orchestration)

**Status:** `COMPLETED` (Backend endpoints & daily 9:00 AM IST notification engine live in v1.25.0 Build 107)

---

## 1. Shared API & Data Contract

### 🔐 Endpoints & HTTP Methods

| Method | Endpoint | Description | Auth Required |
|---|---|---|---|
| `GET` | `/api/credentials/trackers` | List all tracked server credentials with computed expiry status | Yes |
| `POST` | `/api/credentials/trackers` | Create a new server credential tracker | Yes |
| `PUT` | `/api/credentials/trackers/<tracker_id>` | Update an existing credential tracker | Yes |
| `POST` | `/api/credentials/trackers/<tracker_id>/renew` | 1-Tap quick renew: resets `last_updated_date` to today | Yes |
| `DELETE` | `/api/credentials/trackers/<tracker_id>` | Delete a credential tracker | Yes |
| `POST` | `/api/credentials/trackers/test-check` | Manual trigger for 9:00 AM IST notification check (Admin/Dev) | Yes |

### 📄 Data Schema: `CredentialTracker`

```json
{
  "id": "cred_tracker_abc123",
  "server_name": "prod-db-cluster-01",
  "username": "svc_analytics",
  "last_updated_date": "2026-10-15",
  "validity_days": 30,
  "expiry_date": "2026-11-14",
  "days_remaining": -1,
  "status": "EXPIRED",
  "spokesperson_emails": [
    "shefeeq@example.com",
    "lead_ops@example.com"
  ],
  "created_by": "user_id_or_username",
  "created_at": 1731600000,
  "updated_at": 1731600000
}
```

#### Status Enum:
* `"HEALTHY"`: `days_remaining > 5`
* `"EXPIRING_SOON"`: `1 <= days_remaining <= 5`
* `"EXPIRES_TODAY"`: `days_remaining == 0`
* `"EXPIRED"`: `days_remaining < 0`

### 📥 Request Payloads:

* **Create / Update Payload (`POST /api/credentials/trackers` & `PUT /api/credentials/trackers/<id>`)**:
```json
{
  "server_name": "prod-api-edge",
  "username": "deploy_user",
  "last_updated_date": "2026-11-01",
  "validity_days": 45,
  "spokesperson_emails": ["ops1@rosterx.online", "admin@rosterx.online"]
}
```

* **Standard API Response (`GET /api/credentials/trackers`)**:
```json
{
  "success": true,
  "data": {
    "trackers": [ /* array of CredentialTracker */ ],
    "total_count": 5,
    "expiring_soon_count": 2,
    "expired_count": 1
  }
}
```

---

## 2. Technical Blueprint for Dasan

### 📁 Primary File to Modify:
* `server.py`

### 🛠️ Step-by-Step Backend Tasks:

1. **MongoDB Collection Setup**:
   - Collection name: `ATLAS_DB.credential_trackers`.
   - In-memory/local JSON fallback: `SERVER_CREDENTIAL_TRACKERS = []`.
   - On DB connect, ensure index on `server_name` and `expiry_date`.

2. **Computation Logic (`compute_credential_status`)**:
   - Helper function taking `last_updated_date` (format `YYYY-MM-DD`) and `validity_days` (int).
   - In Indian Standard Time (IST, UTC+05:30):
     - `expiry_date = parse_date(last_updated_date) + timedelta(days=validity_days)`
     - `days_remaining = (expiry_date - today_ist).days`
     - Determine `status` string based on `days_remaining`.

3. **Implement REST Handlers in `server.py`**:
   - `handle_get_credential_trackers()`: Query all trackers, compute live `days_remaining` and `status`, return sorted with expired/expiring soon first.
   - `handle_create_credential_tracker()`: Validate `server_name`, `username`, `last_updated_date`, `validity_days > 0`, and list of valid `spokesperson_emails`. Generate ID, save to Atlas DB, return saved object.
   - `handle_update_credential_tracker()`: Update fields, recalculate expiry date.
   - `handle_renew_credential_tracker()`: 1-Tap renewal. Sets `last_updated_date` to current IST date (`YYYY-MM-DD`), recalculates expiry date, updates DB.
   - `handle_delete_credential_tracker()`: Remove by `id`.

4. **Daily 9:00 AM IST Notification Engine (`check_and_dispatch_credential_alerts`)**:
   - Runs in a background daemon thread (`_daily_credential_expiry_cron`).
   - Runs daily at 09:00 IST (with initial 15-second startup check):
     - Finds all trackers where `days_remaining <= 5`.
     - Daily Idempotency: Maintain `LAST_CREDENTIAL_ALERT_DATE` per tracker per day to avoid spam.
     - **Email Dispatch**: Call `send_resend_email` sending HTML table of expiring credentials to `spokesperson_emails`.
     - **FCM Push**: Call `send_fcm_push` to device tokens of users matching `spokesperson_emails` + all Admins & Managers (`"⚠️ Password Expiry Alert: {username}@{server_name} expires in {days} days"`).
     - **In-App Notification**: Insert into `ATLAS_DB.notifications` with priority `"warning"` / `"urgent"`.

5. **Local Verification**:
   - Verify endpoints via `Invoke-RestMethod` or curl.
   - Test `POST /api/credentials/trackers/test-check` to confirm alert generation.

6. **Git Commit**:
   - Commit backend changes:
     ```powershell
     git add server.py AGENT_HANDOFF.md plan_dasan.md
     git commit -m "feat(api): add server credential tracker endpoints and daily 9AM expiry notification engine"
     ```
   - Update `AGENT_HANDOFF.md` with status and endpoint details for Vijayan.
