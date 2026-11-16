# 📋 Execution Plan for Dasan (Backend & Core Orchestration)

**Status:** `COMPLETED` (Sprint v1.27.0 - Build 113 web portal crash fix, phone unmasking, consolidated digest email & version bump verified)

---

## 1. Architectural Mandates from Gafoor

Dasa, Gafoorka ivide blueprint ready aakki vechittundu. Web portal crash fix, backend phone unmasking, and credential email digest bundling aanu Dasan-te main responsibilities.

### 🔐 Scope of Work for Dasan:

1. **Fix Web Portal Crash (`admin-web/js/userPortal.js`)**:
   - **Root Cause**: Duplicate `const restNoticeLabelEl` declaration in `updateCountdownTimer()` (lines ~3097 and ~3217) causing fatal compile-time `SyntaxError: Identifier 'restNoticeLabelEl' has already been declared`.
   - **Fix**: Remove the redundant duplicate `const restNoticeLabelEl`, `const restNoticeValEl`, and `const cardRestDayEl` re-declarations at line ~3217.
   - Verify web portal loads with `node --check admin-web/js/userPortal.js` and local server test.

2. **Unmask Phone Numbers for Authenticated Engineers (`server.py`)**:
   - In `GET /api/engineers` (line ~2962-2965):
     - Remove phone masking `p[:len(p)-4] + "****"`.
     - Allow authenticated users to receive full phone numbers so calling and WhatsApp features work seamlessly across the team.

3. **Credential Tracker Multi-Account Digest Email & Alert Consolidation (`server.py`)**:
   - In `check_and_dispatch_credential_alerts()`:
     - When multiple accounts need alerts, **DO NOT** dispatch separate emails per account.
     - Group all expiring/expired trackers into a **single consolidated security digest email**.
     - Extract all unique `spokesperson_emails` across all alerted trackers and send the digest to all spokespersons in "To".
     - Build a single HTML table listing all accounts (Server, Username, Validity, Last Updated, Expiry Date, Urgency Status).
     - **Remove** the "Open RosterX & Renew Credential" / Update button from the email template completely.
     - Also group the in-app notification and FCM push so team members receive 1 clean digest notification (e.g., `⚠️ 3 Server Credentials Require Password Update`).

4. **Terminology Standardization**:
   - Replace the word `"Rotate"` / `"Rotated"` with `"Update"` / `"Updated"` in server logs and email templates.

5. **Version Bump**:
   - In `server.py`: `SERVER_VERSION = "1.27.0"`.

---

## 2. Technical Blueprint for Dasan

### 📁 Primary Files:
1. `server.py`
2. `admin-web/js/userPortal.js`

### 🛠️ Execution & Verification:
1. Test `userPortal.js` syntax: `node --check admin-web/js/userPortal.js`.
2. Test `check_and_dispatch_credential_alerts()` with sample trackers to verify 1 single email is generated with all accounts listed.
3. Verify `GET /api/engineers` returns full phone numbers for authenticated requests.
4. Update `AGENT_HANDOFF.md` upon completion.
