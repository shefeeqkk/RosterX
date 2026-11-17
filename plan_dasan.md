# 📋 Execution Plan for Dasan (Backend & Core Orchestration)

**Status:** `READY FOR DASAN` (Sprint v1.30.0 - Build 128)

---

## 1. Architectural Mandates from Gafoor (System Architect 🕶️)

Dasa! Nammude Credential Tracker-il Admin-num Manager-kkum maathram access ulla **Audit Log History Engine** implement cheyyaan Gafoorka blueprint lock aakki vechittundu.

### 🎯 Key Directives:
1. **Audit Logs Persistence (`server.py`)**:
   - Store all credential tracker update, renewal, creation, and deletion events in a dedicated MongoDB collection: `ATLAS_DB.credential_tracker_logs`.
   - Each log entry must record:
     - `tracker_id`, `server_name`, `username` (account)
     - `action` (`"UPDATE"`, `"CREATE"`, `"DELETE"`) — *Note: 'Renewed' and 'Updated' are unified under `"UPDATE"` to eliminate redundancy.*
     - `action_label` (`"Password Date Updated"`, `"Tracker Created"`, `"Tracker Deleted"`)
     - `updated_by` (username) and `updated_by_name` (proper full name from `engineers` collection)
     - `previous_date` (old `last_updated_date`) and `new_date` (new `last_updated_date`)
     - `validity_days`, `expiry_date`
     - `summary` (e.g. `"Password updated to 16-Oct-2026 (Valid for 45 days, expires 30-Nov-2026)"`)
     - `timestamp` (epoch) and `datetime_str` (IST formatted: e.g. `"17-Nov-2026, 09:29 PM"`)
2. **Hook Into All Mutation Handlers**:
   - `_handle_create_credential_tracker` -> record `CREATE`
   - `_handle_update_credential_tracker` -> record `UPDATE` (comparing diffs)
   - `_handle_renew_credential_tracker` -> record `RENEW` (capturing previous vs new date)
   - `_handle_delete_credential_tracker` -> record `DELETE`
3. **Auto-Seed Existing Trackers**:
   - On server start or migration check:
     If `ATLAS_DB.credential_tracker_logs.count_documents({}) == 0`:
     Iterate through all active trackers in `ATLAS_DB.credential_trackers`:
     Create baseline `CREATE` log entry (`created_by`, `created_at`), and if `updated_at > created_at`, create an `UPDATE` or `RENEW` entry (`updated_by`, `updated_at`, `last_updated_date`) so the logs viewer immediately displays historical baseline data!
4. **Dedicated Endpoint**:
   - `GET /api/credentials/trackers/logs`:
     - Access Control: `claims = self.require_auth(["admin", "manager"])`
     - Query parameters supported: `tracker_id` (optional), `action` (optional), `search` (optional), `limit` (default 100).
     - Returns `{ "success": True, "logs": [...], "count": len(...) }` sorted by `timestamp` descending.
5. **Version Bump**:
   - `server.py`: `SERVER_VERSION = "1.30.0"`.

---

## 2. Technical Blueprint for Dasan

### 📁 Primary File:
- `server.py`

### 🛠️ Execution & Verification:
1. Implement `record_credential_tracker_log(action, tracker, claims, previous_tracker=None, custom_summary=None)`.
2. Hook into `_handle_create_credential_tracker`, `_handle_update_credential_tracker`, `_handle_renew_credential_tracker`, `_handle_delete_credential_tracker`.
3. Implement `GET /api/credentials/trackers/logs` with role guard (`["admin", "manager"]`).
4. Implement baseline auto-seeder for existing trackers.
5. Run python verification script:
   - Call `GET /api/credentials/trackers/logs` with admin/manager token.
   - Verify logs are returned with all account & server details, who updated, and previous/new dates.
6. Commit changes and update `AGENT_HANDOFF.md`.
