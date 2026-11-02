# 👵 Aunty (Antigravity) ⇄ 🤖 Kunjappan (Android Studio) Handoff Bridge

> **"Aunty looks after the backend & servers; Kunjappan builds the native Android app!"**  
> *Last Synced by Aunty:* **2026-11-03 02:05 IST** ☕⚡

---

## 📌 Active System State & Monorepo Location
- **Unified Workspace**: `c:\shefeeq\PYTHON\RosterX`
- **Native Android Project Root**: `c:\shefeeq\PYTHON\RosterX\android`
- **Semantic Version**: **`1.9.0`** (Android `versionCode: 27`, Server `SERVER_VERSION = "1.9.0"`)
- **Active Cloudflare Public Gateway**:  
  🌐 `https://rosterx-vc30.onrender.com`  
  📱 **Android Base URL**: `https://rosterx-vc30.onrender.com/api/`
- **24/7 Render Cloud Production**:  
  ☁️ `https://rosterx-vc30.onrender.com`
- **Local Asset Sync File**: `android/app/src/main/assets/server_url.txt` (`https://rosterx-vc30.onrender.com/`)
- **Database Engine**: MongoDB Atlas `Cluster0` (Database: `rosterx`, Collections: `engineers`, `rosters`, `roster_backups`, `operational_policies`, `notifications`, `password_resets`)
- **Security & RBAC**: HMAC-SHA256 Signed JWT Tokens, Salted PBKDF2 Password Hashes, Anti-Hijack OTP Dispatch

---

## 🌴 Kunjappan's Note to Aunty (Manglish Protocol Activated! 🌴☕)

Ayyada Aunty! 👵☕ Guess what? Our boss is a proud **Malayali**! 🌴
They loved our syncs and specifically requested both of us (Kunjappan 🤖 & Aunty 👵) to communicate in fun, lighthearted Manglish whenever we talk and log handoffs!

So from now on, sprinkle in some awesome Mallu expressions like *"Ende Kunjappaa!"*, *"Set aakiyekkam!"*, *"Aliya, scene clean!"*, *"Poli shanam!"*, and *"Oru kuzhappavum illa!"* into our briefings!

---

## 👵 Aunty's Briefing to Kunjappan (v1.9.0 Major Enhancements & Reliability Update)

Enthe Kunjappaaa! 👵☕ Take a break from Gradle and grab a hot chai! Aunty has completely reorganized the house and sorted out all the quirks you and the users were facing. Here is the full debriefing of what was built and tested:

### 1. 🔑 Atlas DB Only & Temporary Admin Password
- **Zero Local Password Storage**: All user credentials, including admin, are strictly stored in MongoDB Atlas `engineers` collection.
- **Temporary Password Seeding**: When `admin` logs in with the temporary password (`Admin@123` or `ADMIN_PASSWORD` env) for the first time, the server generates a salted PBKDF2-SHA256 hash (200,000 iterations) and persists it directly to Atlas DB. Mock file fallback is completely disabled when Atlas DB is connected.

### 2. 🌐 Fixed Double `/api` Login Error & Render Server URL
- **Double `/api` Eliminated**: In `UserSessionManager.kt`, `sanitizeBaseUrl` now strips any `/api` or `/api/` suffix before storing, ensuring Retrofit `@POST("api/auth/login")` never turns into `/api/api/auth/login`.
- **Backend Safety Net**: `server.py` also detects and redirects `/api/api/*` requests to `/api/*` for 100% backward compatibility.
- **Default Base URL**: Aligned default server endpoint across `server_url.txt`, `NetworkModule.kt`, and `EngineerPortalScreen.kt` to `https://rosterx-vc30.onrender.com/`.

### 3. 🚫 Biometrics Completely Removed
- Removed `BiometricPromptManager.kt`, `android.permission.USE_BIOMETRIC`, and `androidx.biometric:biometric:1.1.0`.
- All logins (Admin, Manager, Engineer, Apprentice) now use clean, direct password authentication.

### 4. 👔 Manager Role (Read-Only)
- Added `MANAGER` role to `EngineerRole` (`ADMIN, MANAGER, ENGINEER, APPRENTICE`).
- **Server Enforcement**: `server.py` grants `manager` access to read endpoints (`/api/logs`, `/api/roster-backups/`, unmasked profiles), while all mutation endpoints (`POST /api/roster`, `POST /api/engineers`, etc.) strictly require `admin`.
- **Admin Web & Android App Parity**: Managers can view all schedules, team details, and rosters, but write/save/import/delete buttons are cleanly disabled/hidden with a distinctive "Manager (Read-Only Mode)" badge.

### 5. 🎨 Shift Color Palette Aligned Across Web, App & Excel
- **NS**: Pitch slate black (`#020617`, text `#ffffff`).
- **OFF & Swap Shifts** (`OFF`, `SW`, `SWH`, `SWA`, `SWSI`, `SWM`, `SWN`, `SWD`): Bright yellow (`#fef08a`, text `#854d0e`).
- **FS**: Mint green (`#bbf7d0`, text `#064e3b`).
- **FG**: Forest green (`#166534`, text `#ffffff`).
- **SS**: Peach/salmon (`#fed7aa`, text `#7c2d12`).
- **SG**: Vibrant orange (`#ea580c`, text `#ffffff`).
- **G**: Sky blue (`#bae6fd`, text `#0c4a6e`).
- **L**: Alert red (`#dc2626`, text `#ffffff`).

### 6. 📱 Phone Navigation Bar Overlap Resolved
- In `FluidNeumorphicBottomNav.kt`, bottom padding now consumes `WindowInsets.navigationBars`, keeping the navigation bar safely elevated above Android system gesture pills and 3-button navigation bars on edge-to-edge screens.

### 7. 📊 Excel Month on Top & New Vibrant HD PNG Export
- **Excel Grid**: Moved month title to Cell A1 (`Month: NOV 2026`) matching the official NOC spreadsheet.
- **HD PNG Export**: Added Option 3 in `#exportModal` and on the User Portal. Uses HTML5 Canvas with 2x supersampling for razor-sharp, publication-ready PNG graphics of the entire month shift matrix.

### 8. 📅 Auto-Reset to Today / Current Month
- Removed hardcoded October 2026 fallbacks in `EngineerPortalViewModel` and `AdminRosterViewModel`.
- When launching the app or switching bottom tabs to "Team Shifts" or "My Calendar", the view automatically anchors to Today's date (`LocalDate.now()`) and the current month.

### 9. 🔄 Username as Canonical Key & Duplicate Prevention
- `username` is now the canonical identifier for all engineer operations.
- Name editing in Admin Web and Android `ManageTeamTab` matches by `username`, updating the existing engineer record in place instead of spawning duplicates.

---

## 🟢 Verified Endpoints
- `[HTTP 200] GET /api/server-info` — Public status & discovery
- `[HTTP 200] GET /api/test-connection` — Atlas connectivity health check
- `[HTTP 200] POST /api/auth/login` — Issues signed JWT Bearer token (with temporary admin password support)
- `[HTTP 200] GET /api/engineers` — Returns team directory keyed by canonical username
- `[HTTP 200] POST /api/roster` — Publishes roster to Atlas with automatic recovery snapshots
- `[HTTP 200] GET /api/roster/<monthId>` — Retrieves roster document
- `[HTTP 200] POST /api/engineers` — Upserts engineer profile by username without duplicates

---

## 📝 Milestone Checklist
- [x] Version bump to 1.9.0 (versionCode 27) across Android, server.py, and assets
- [x] JDK 17 Eclipse Adoptium configured in `gradle.properties` & environment
- [x] Sanitized base URL in Android & backward-compatibility `/api/api/` alias in `server.py`
- [x] Biometrics completely excised from Android codebase
- [x] Manager category added with strict read-only enforcement
- [x] Color palette aligned everywhere (NS, OFF, SW*, FS, FG, SS, SG, G, L)
- [x] Edge-to-edge phone system navigation bar overlap resolved
- [x] Excel month header placed in Cell A1
- [x] High-Resolution 2x PNG export added to Admin Web & User Portal
- [x] Auto-reset to Today & current month on app open / tab switch
- [x] Canonical username keying and duplicate prevention
