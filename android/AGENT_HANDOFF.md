# 👵 Aunty (Antigravity) ⇄ 🤖 Kunjappan (Android Studio) Handoff Bridge

> **"Aunty looks after the backend & servers; Kunjappan builds the native Android app!"**  
> *Last Synced by Aunty:* **2026-11-02 04:35 IST** ☕⚡

---

## 📌 Active System State & Monorepo Location
- **Unified Workspace**: `d:\RosterX\RosterX`
- **Native Android Project Root**: `d:\RosterX\RosterX\android`
- **Semantic Version**: **`1.8.0`** (Android `versionCode: 26`, Server `SERVER_VERSION = "1.8.0"`)
- **Active Cloudflare Public Gateway**:  
  🌐 `https://rosterx.onrender.com`  
  📱 **Android Base URL**: `https://rosterx.onrender.com/api/`
- **24/7 Render Cloud Production**:  
  ☁️ `https://rosterx.onrender.com` (Auto-configured with `render.yaml` 1-click Blueprint)
- **Local Asset Sync File**: `android/app/src/main/assets/server_url.txt`
- **Database Engine**: MongoDB Atlas `Cluster0` (Database: `rosterx`, Collections: `engineers`, `rosters`, `roster_backups`, `operational_policies`, `notifications`, `password_resets`)
- **Security & RBAC**: HMAC-SHA256 Signed JWT Tokens, Salted PBKDF2 Password Hashes, Anti-Hijack OTP Dispatch

---

## 👵 Aunty's Briefing to Kunjappan (v1.8.0 Major Security Hardening)

Ayyada Kunjappaaaa! 👵☕ Put down your filter coffee and listen up carefully! We just completed an enterprise-grade security hardening across the entire backend and mobile architecture following our security review:

### 1. 🔐 Cryptographic Authentication & RBAC Layer (`server.py` & `HostSelectionInterceptor.kt`):
- **JWT Access Tokens**: All logins on `POST /api/auth/login` now issue signed HMAC-SHA256 JWT tokens with 7-day expiration.
- **Android Interceptor Token Forwarding**: `HostSelectionInterceptor` now reads `sessionManager.getAuthToken()` and automatically injects `Authorization: Bearer <token>` into all outgoing OkHttp requests.
- **Strict Role-Based Access Control**:
  - `admin`: Full read/write access to rosters, backups, constraints, engineers, and live server logs.
  - `engineer` / `apprentice`: Authenticated read access to schedules, personal notifications, and self-profile.
  - Unauthenticated access returns `401 Unauthorized`; role violations return `403 Forbidden`.

### 2. 🛡️ Password Security & Zero-Plaintext Storage:
- **Salted PBKDF2 Hashing**: Passwords stored on the server now use `PBKDF2-HMAC-SHA256` with 200,000 rounds and unique 16-byte cryptographic salts.
- **Zero Default Passwords**: Removed hardcoded `admin123` and `user123` backdoors.
- **Zero Browser Caching**: Passwords are no longer cached or verified in `localStorage`.

### 3. ✉️ Anti-Hijack OTP Dispatch (`POST /api/auth/send-otp`):
- **Server-Derived Destinations**: The server completely ignores any `email` or `name` field passed in request bodies. The recipient address is looked up strictly from the database.
- **Cryptographic OTPs**: Codes are generated with `secrets.randbelow()` (not `random.randint`), and stored as SHA-256 hashes with TTL expiry.
- **Zero User Enumeration**: The response message is always uniform ("If the account exists, a 6-digit verification code has been dispatched"), preventing attackers from harvesting usernames or reading coworker emails.

### 4. 📱 Android App Security & Data-Loss Protection:
- **`NetworkSecurityConfig`**: Created `network_security_config.xml` to disallow cleartext HTTP traffic across the public internet, restricting HTTP strictly to local emulator/dev addresses (`10.0.2.2`, `localhost`, `127.0.0.1`).
- **Logcat Privacy**: `HttpLoggingInterceptor` is now guarded by `if (BuildConfig.DEBUG)` so sensitive credentials and payloads are NEVER logged to logcat in release builds.
- **Backup Shield**: Set `android:allowBackup="false"` in `AndroidManifest.xml` to prevent ADB data extraction.
- **Constraint Overwrite Shield**: `getConstraints()` in `RosterRepositoryImpl.kt` now returns `Result.failure(e)` on network error instead of fake success with built-in defaults, preventing accidental erasure of customized shift rules.
- **`ShiftCode.UNKNOWN`**: Added `ShiftCode.UNKNOWN` to eliminate silent conversion of unknown codes into `OFF`, with strict validation warnings.
- **Rest Hours Parity**: Aligned `OFF` and `L` `minRestHours` to 0.0 across Android and Web.

### 5. 👥 Privacy & Hygiene:
- Scrubbed all real employee names, `@tejasnetworks.com` email addresses, and personal phone numbers from source code, backups, and documentation.
- Untracked legacy 20MB APK binaries from Git.

---

## 🟢 Verified Endpoints
- `[HTTP 200] GET /api/server-info` — Public status & discovery
- `[HTTP 200] GET /api/test-connection` — Atlas connectivity health check
- `[HTTP 200] POST /api/auth/login` — Issues signed JWT Bearer token
- `[HTTP 200] POST /api/auth/send-otp` — Anti-hijack verification dispatch
- `[HTTP 200] POST /api/auth/verify-otp` — Salted PBKDF2 password reset
- `[HTTP 401] GET /api/engineers` — Rejected without token (Verified!)
- `[HTTP 403] GET /api/logs` — Blocked for non-admins (Verified!)
- `[HTTP 429] POST /api/auth/login` — Brute force rate limiting (Verified!)

---

## 📝 Milestone Checklist
- [x] Version bump to 1.8.0 (versionCode 26) across Android, server.py, and assets
- [x] In-memory security test suite (8/8 tests passing)
- [x] HostSelectionInterceptor with Authorization Bearer token header
- [x] Network Security Config & allowBackup=false
- [x] Logcat HttpLoggingInterceptor DEBUG guard
- [x] ShiftCode.kt & RosterValidator.kt UNKNOWN guard and parity
- [x] Gradle wrapper distribution corrected to official `gradle-8.9-bin.zip` with valid SHA-256 sum
- [x] Pre-installed and unpacked Gradle 8.9 in `.gradle/wrapper/dists/` with `.ok` stamp
- [x] Removed invalid `gradle-daemon-jvm.properties` (Java 25 requirement) and pinned `org.gradle.java.home` to JDK 17 in `gradle.properties`
- [x] Added GitHub Actions Cloud CI/CD pipeline (`.github/workflows/build-apk.yml`) for automated APK compilation without Android Studio
