# 📋 Execution Plan for Dasan (Backend & Core Orchestration)

**Status:** `COMPLETED` (Sprint v1.26.0 - Build 109 backend threshold validation, alarm_start_date contract & version bump verified)

---

## 1. Architectural Mandates from Gafoor

Dasa, Gafoorka ivide blueprint ready aakki vechittundu. UI-yil valiya maatrangalaanu Vijayan cheyyunnathu. Backend-il credential threshold integrity-yum version consistency-yum strict aayi enforce cheyyanam.

### 🔐 API & Data Contract Checklist:
1. **Credential Tracker Alert Threshold**:
   - `alert_threshold_days` (integer, default 5) is accepted in:
     - `POST /api/credentials/trackers`
     - `PUT /api/credentials/trackers/<id>`
     - `POST /api/credentials/trackers/<id>/renew`
   - Verify validation: `alert_threshold_days >= 1`. If omitted, fallback to `5`.
   - Verify `compute_credential_status`:
     - `alarm_start_date = expiry_date - alert_threshold_days`.
     - Status is `EXPIRING_SOON` when `days_remaining <= alert_threshold_days` and `days_remaining > 0`.
2. **Version Bump**:
   - Update `server.py`: `SERVER_VERSION = "1.26.0"`.
3. **Backend Health Check**:
   - Run syntax check and verify all credential test endpoints respond with valid JSON contracts.

---

## 2. Technical Blueprint for Dasan

### 📁 Primary Files:
* `server.py`

### 🛠️ Execution Steps:
1. Review `_handle_create_credential_tracker`, `_handle_update_credential_tracker`, and `_handle_renew_credential_tracker` in `server.py` to ensure `alert_threshold_days` is safely parsed, validated, and returned in responses.
2. Bump `SERVER_VERSION = "1.26.0"` in `server.py`.
3. Verify server startup / sanity checks.
4. Update `AGENT_HANDOFF.md` upon completion.
