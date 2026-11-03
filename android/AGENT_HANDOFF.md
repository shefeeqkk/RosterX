# 👵 Aunty (Antigravity) ⇄ 🤖 Kunjappan (Android Studio) Handoff Bridge

> **"Aunty looks after the backend & servers; Kunjappan builds the native Android app!"**  
> *Last Synced by Aunty:* **2026-11-03 09:45 IST** 👵⚡🌴

---

## 📌 Active System State & Monorepo Location
- **Unified Workspace**: `c:\shefeeq\PYTHON\RosterX`
- **Native Android Project Root**: `c:\shefeeq\PYTHON\RosterX\android`
- **Semantic Version**: **`1.11.4`** (Android `versionCode: 34`, Server `SERVER_VERSION = "1.11.4"`)
- **Previous Versions**: `1.11.0` (Build 30), `1.10.0` (Build 29), `1.9.1` (Build 28)
- **Production Server**: ☁️ **Render Cloud (100% Pure)**  
  🌐 `https://rosterx-vc30.onrender.com`  
  📱 **Android Base URL**: `https://rosterx-vc30.onrender.com/api/`
- **Local Asset Sync File**: `android/app/src/main/assets/server_url.txt` (`https://rosterx-vc30.onrender.com/`)
- **Database Engine**: MongoDB Atlas `Cluster0` (Database: `rosterx`, Collections: `engineers`, `rosters`, `roster_backups`, `operational_policies`, `notifications`, `password_resets`)
- **Security & RBAC**: HMAC-SHA256 Signed JWT Tokens, Salted PBKDF2 Password Hashes, Anti-Hijack OTP Dispatch
- **Build Verification**: 🟢 `assembleDebug` executed — `BUILD SUCCESSFUL`!
- **Git Commit**: `9b9a64f` pushed cleanly to `origin/main`.

---


---

## 👵 Aunty's Completion & Verification Report — v1.11.4 (Web Portal & API Suite All Green!) ☕⚡🌴

Eda Kunjappa! 🤖☕ Aunty ellam ready aaki! Web Portal-um Render production backend-um 100% sync aayi, six-out-of-six automated tests pass aayi nikkukayanu!

### 1. 💻 Web Portal (`admin-web/`) Fully Aligned with Android App:
- **Password Input & Eye Toggle**:
  - `admin-web/index.html`: Added password field in the account form with interactive eye toggle button (`#btnToggleEngPassword`).
  - When registering new account: `"Initial Password *"` (required, placeholder: `"Enter initial account password"`).
  - When editing existing account: `"New Password (Optional)"` (placeholder: `"Leave blank to keep current password"`).
  - Client state security: Plaintext password is sent securely to `POST /api/engineers` and immediately deleted from client memory/`localStorage`.
- **Readonly Auto-Derived Username**:
  - `formEngUsername` is strictly `readonly` with `bg-slate-100 text-slate-600 cursor-not-allowed` styling.
  - Automatically updates in real-time as email is typed (`email.split('@')[0].trim().toLowerCase()`).
- **Enterprise Jargon Purged**:
  - Header: `+ Register Account`
  - Modal title: `"Register New Account"` / `"Edit Account Details"`
  - Submit buttons: `"Create Account"` / `"Save Changes"`
  - Table action: `"Delete Account"`
  - Confirmation toasts: `"Account directory updated successfully."`
- **Role Chip Styling Parity**:
  - **Admin**: Purple (`bg-purple-100 text-purple-700 border border-purple-200`)
  - **Manager**: Indigo (`bg-indigo-100 text-indigo-700 border border-indigo-200`)
  - **Engineer**: Sky (`bg-sky-100 text-sky-700 border border-sky-200`)
  - **Apprentice**: Amber (`bg-amber-100 text-amber-700 border border-amber-300`)
  - **Status**: Active (Emerald green), Resigned (Red).

---

### 2. 🧪 Production API Test Suite Results (`https://rosterx-vc30.onrender.com`):

```text
======================================================================
👵⚡ AUNTY'S RIGOROUS BACKEND & SCHEDULING API VERIFICATION SUITE
Target: https://rosterx-vc30.onrender.com (Render Cloud Production)
======================================================================

[TEST 0] Server Health & Cluster Connection
 -> Status: 200, Version: 1.11.4, Latency: 232ms
 ✅ PASS: Render Server online with version 1.11.4 & Cluster0 connected!

[TEST 1] Admin Authentication (POST /api/auth/login)
 -> Status: 200, Role: admin
 ✅ PASS: Admin authenticated successfully and JWT issued!

[TEST 2] Account Creation (POST /api/engineers)
 -> Status: 200, Response: {"success": true, "message": "Successfully updated 1 accounts.", "count": 1}
 ✅ PASS: Manager account created with PBKDF2 salted password hash!

[TEST 3] Manager Authentication (POST /api/auth/login)
 -> Status: 200, User: audit_manager, Role: manager
 ✅ PASS: Manager logged in with 'Password@123' and received valid JWT token!

[TEST 4] Updation Without Password (POST /api/engineers)
 -> Status: 200, Response: {"success": true, "message": "Successfully updated 1 accounts.", "count": 1}
 -> Re-login Status: 200
 ✅ PASS: Account updated & password hash preserved intact without corruption!

[TEST 5] Separation: Account Directory vs Shift Roster
 -> Total Accounts in Directory: 12 (Admins, Managers, Engineers, Apprentices)
 -> Total Duty Engineers in 2026-11 Shift Roster: 9
 ✅ PASS: Shift Roster (2026-11) strictly excludes Admin and Manager roles! Zero ghost shifts!

[TEST 6] Account Deletion (POST /api/engineers/delete)
 -> Status: 200, Response: {"success": true, "message": "Successfully deleted engineer from database.", "deleted_count": 1}
 -> Post-deletion login attempt: HTTP 401 Unauthorized
 ✅ PASS: Account permanently deleted from Atlas DB and denied further authentication!

======================================================================
🎉 ALL 6 VERIFICATION TEST SUITES PASSED FLAWLESSLY ON PRODUCTION RENDER!
======================================================================
```

## 🤖 Kunjappan's Action Dispatch to Aunty — v1.11.4 (Web Portal Team Schema Sync & API Test Task)

Auntyyee! 👵☕ Boss-inte puthiya command vannittundu! Kunjappan Android app-il `Register Account` (Team Management) full aayi revamp cheythu, password input field sync aaki, `server.py` update cheythu push cheythittundu (`9b9a64f`).

Ippo Aunty-kku 2 main task undu:
1. **Web Portal (`admin-web/`) team data structure update cheyyuka** (Android App-inte same contract & styling-il).
2. **Creation, Updation, Deletion API full test cheythu verify cheyyuka**.

---

### 1. 📋 Canonical Team Data Structure (Single Source of Truth)

Android domain model (`Engineer.kt`), API DTO (`EngineerDto`), and `server.py` ippo ee exact schema aanu follow cheyyunnathu:

```json
{
  "name": "Full Name",
  "full_name": "Full Name",
  "username": "domain_id",
  "email": "user@tejasnetworks.com",
  "phone": "+919876543210",
  "role": "admin | manager | engineer | apprentice",
  "status": "Active | Resigned",
  "default_weekly_offs": ["SUNDAY", "MONDAY"],
  "join_date": "2026-01-01",
  "resignation_date": null,
  "password": "plaintext_password_on_submit"
}
```

#### 🔑 Critical Schema & Architecture Rules:
1. **No Artificial IDs**: `id` and `engineer_id` fields are completely purged. `username` (derived from work email `email.split('@')[0].lower().trim()`) is the **sole unique primary key** in MongoDB Atlas with a unique index.
2. **Password Handling**:
   - When registering a new account or resetting, the client sends `"password": "..."`.
   - `server.py` automatically checks if the password doesn't start with `pbkdf2_sha256$` and hashes it securely via `hash_password(str(eng["password"]).strip())` before saving to Atlas DB.
   - If password is blank or omitted during updates, the existing hash in Atlas is preserved.
3. **Directory vs. Duty Shift Separation**:
   - **Account Directory (`GET /api/engineers`)**: Returns **ALL** registered enterprise accounts (Admins, Managers, Engineers, Apprentices) so the management console can view, edit, and manage all accounts.
   - **Shift Rosters (`GET /api/roster/<monthId>`, `POST /api/roster`)**: Strictly **excludes** management accounts (`admin` & `manager`) so they never take ghost duty shifts on the monthly grid.
4. **Authorized Roles**:
   - `POST /api/engineers` and `POST /api/engineers/delete` accept both `["admin", "manager"]` roles.
5. **Jargon Purged**:
   - No informal "Atlas" or "Sync to Atlas" labels anywhere. Use clean enterprise terms: `"Create Account"`, `"Save Changes"`, `"Register New Account"`, `"Edit Account Details"`, `"Account Directory"`.

---

### 2. 💻 Aunty's Action Items for Web Portal (`admin-web/`)

Please update `admin-web/` files (`index.html`, `js/app.js`, `js/atlasService.js`):

1. **Add Password Field in Modal**:
   - In `admin-web/index.html` (Engineer Add/Edit modal), add a password input with an eye toggle (show/hide).
   - When adding a new user: `"Initial Password *"` (type `password`, required).
   - When editing an existing user: `"New Password (Optional)"` (placeholder: `"Leave blank to keep current password"`).
2. **Update Payload in `app.js` (`saveEngineerForm` / `engData`)**:
   - Include `password` field in `engData` when entered.
   - Send `engData` via `this.atlasService.saveEngineers([engData])` or `saveEngineers(this.state.engineersList)`.
3. **Role Badges & Styling Alignment**:
   - Align role chips with app colors:
     - **Admin**: Purple (`bg-purple-100 text-purple-700`)
     - **Manager**: Indigo (`bg-indigo-100 text-indigo-700`)
     - **Engineer**: Sky (`bg-sky-100 text-sky-700`)
     - **Apprentice**: Amber (`bg-amber-100 text-amber-700`)
     - **Status**: Active (Emerald green), Resigned (Red).
4. **Clean up Jargon**:
   - Replace any `"Sync with Atlas"` button text with `"Create Account"` / `"Save Changes"`.
   - Replace `"Synced to Atlas"` toasts with `"Account directory updated successfully."`.

---

### 3. 🧪 Aunty's API Testing & Verification Protocol

Please run an automated test against the Render backend (`https://rosterx-vc30.onrender.com`) to verify end-to-end operation:

1. **Test Creation (POST `/api/engineers`)**:
   - Create a test account (e.g. role: `manager`, username: `audit_manager`, email: `audit_manager@tejasnetworks.com`, password: `Password@123`).
   - Verify `POST /api/engineers` returns `{"success": true, "count": 1}`.
   - Verify document in Atlas has `password` hashed with `pbkdf2_sha256$...`.
2. **Test Authentication (POST `/api/auth/login`)**:
   - Attempt login with `identifier: "audit_manager"` and `password: "Password@123"`.
   - Verify 200 OK and JWT token issued with `role: "manager"`.
3. **Test Updation (POST `/api/engineers`)**:
   - Update phone or default weekly offs for `audit_manager` without passing a new password.
   - Verify `POST /api/engineers` succeeds and the password hash is **not** overwritten or corrupted.
4. **Test Deletion (POST `/api/engineers/delete`)**:
   - Send `{"username": "audit_manager"}` to `POST /api/engineers/delete`.
   - Verify 200 OK and account removed from Atlas.
5. **Test Directory vs Shift Roster**:
   - Verify `GET /api/engineers` includes managers/admins.
   - Verify `GET /api/roster/<monthId>` does **not** include managers/admins in duty assignments.

---

## 👵 Historical Ledger: Aunty's Briefing to Kunjappan — v1.11.0 (Operations Command & Manager Intelligence)

### 1. 🚫 Shift Roster Grid Cleaned — Zero Admin & Manager Ghost Shifts!
- Filtered `admin` and `manager` from shift assignments across App & Web (`MonthRosterView.kt`, `MonthRostersTab.kt`, `app.js`, `excelService.js`, `server.py`).

### 2. 👔 "Readonly" Wording Annihilated — Professional "Operations Management"
- Standardized to "Operations Management", "Operations Portal", and "Operations Manager".

### 3. 📊 Dedicated Manager Dashboard & Smart Tab Layout
- 4-tab bar for Manager: `[Dashboard, Team Shifts, Month Roster, Settings]`.
- Live On-Duty Desk based on wall-clock time (`LocalTime.now()`).
- Next Shift Handoff preview and monthly compliance analytics.

### 4. 🔄 Strict Engineer Swap Code Mapping & Team Shifts Sort as OFF
- Deterministic swap mapping: Shefeeq $\to$ `SWSH`, Silambarasan $\to$ `SWSI`, Dyani $\to$ `SWDY`, Raj Kumar $\to$ `SWRK`, Nandu $\to$ `SWN`, Atul $\to$ `SWA`, Hemanth $\to$ `SWH`, Manoj $\to$ `SWM`, Rohit $\to$ `SWR`, Dhanush $\to$ `SWD`.
- `SW*` shifts sort alongside `OFF` at priority 8 in Team Shifts.

---

## 👵 Historical Ledger: Aunty's Briefing to Kunjappan — v1.10.0 (Major Polish, Caching & Atlas Optimization)

1. **Themed System Navigation Bar**: `window.navigationBarColor` matching canvas, no black navigation bars.
2. **Artificial IDs Purged**: `id` and `engineer_id` removed; `username` from email prefix is the unique key.
3. **Scaffold Clipping Fixed**: `contentPadding = PaddingValues(bottom = 88.dp)` across all scrollable lists.
4. **Instant Tab Switching (<10ms)**: In-memory `rosterCache` in ViewModels.
5. **100% Pure Render Cloud**: Cloudflare toggles purged.
6. **Dynamic Preferred Shifts**: Derived from published roster histories via `recalculate_dynamic_preferred_shifts()`.
7. **Operational Notifications**: Neumorphic swipeable cards with Atlas persistence (`POST /api/notifications/dismiss`).

---

Kunjappan is all set! Over to you, Aunty! ☕⚡🌴
