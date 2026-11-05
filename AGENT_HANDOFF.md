# 👵 Aunty (Antigravity) ⇄ 🤖 Kunjappan (Android Studio) Handoff Bridge

> **"Aunty looks after the backend & servers; Kunjappan builds the native Android app!"**  
> *Last Synced by Kunjappan:* **2026-11-05 21:05 IST** 🤖⚡🌴

---

## 📌 Active System State & Monorepo Location
- **Unified Workspace**: `c:\shefeeq\PYTHON\RosterX`
- **Native Android Project Root**: `c:\shefeeq\PYTHON\RosterX\android`
- **Semantic Version**: **`1.11.8`** (Android `versionCode: 38`, Server `SERVER_VERSION = "1.11.8"`)
- **Previous Versions**: `1.11.7` (Build 37), `1.11.6` (Build 36), `1.11.5` (Build 35), `1.11.0` (Build 30), `1.10.0` (Build 29), `1.9.1` (Build 28)
- **Production Server**: ☁️ **Render Cloud (100% Pure)**  
  🌐 `https://rosterx-vc30.onrender.com`  
  📱 **Android Base URL**: `https://rosterx-vc30.onrender.com/api/`
- **Local Asset Sync File**: `android/app/src/main/assets/server_url.txt` (`https://rosterx-vc30.onrender.com/`)
- **Database Engine**: MongoDB Atlas `Cluster0` (Database: `rosterx`, Collections: `engineers`, `rosters`, `roster_backups`, `operational_policies`, `notifications`, `password_resets`)
- **Security & RBAC**: HMAC-SHA256 Signed JWT Tokens, Salted PBKDF2 Password Hashes, Anti-Hijack OTP Dispatch
- **Email Engine**: 📧 **Multi-Channel Fail-Safe Recovery** (Gmail SMTP Port 465/587 + Automatic HTTPS Port 443 Resend Fallback for Render Free Tier)
- **Admin Server Logs**: 📜 **Live Terminal Modal** in Admin Web Portal (`GET /api/logs`, `POST /api/logs/clear`) with real-time log stream
- **Build Verification**: 🟢 `assembleDebug` executed — `BUILD SUCCESSFUL`!

---

## 🤖 Kunjappan's Upgrade Report — v1.11.7 (Native Gmail SMTP OTP Dispatch & Recovery Pipeline) ☕⚡🌴

Auntyyee! 👵🤖 Boss-inte directive anusarichu `server.py`-il **Native Gmail SMTP with TLS/SSL** implement cheythu, Resend sandbox limitations full aayi bypass cheythu!

### 1. 📧 Native Gmail SMTP Engine (`send_otp_email`)
- **Configuration**:
  - `SMTP_EMAIL`: `kkshefeeq005@gmail.com`
  - `SMTP_PASSWORD`: Google App Password configured in `.env` and `render.yaml`.
- **SSL/TLS Protocol**:
  - Uses Python standard library `smtplib` and `ssl`.
  - Connects to `smtp.gmail.com` on port 465 (SSL) with automatic fallback to port 587 (STARTTLS).
- **Neumorphic Soft UI Design**:
  - Transmits styled HTML with off-white container (`#eef2f6`), high-contrast typography (`#2d3748`), large accent coral OTP code (`#ff5252`), and 10-minute expiry notice.

### 2. 🔐 Upgraded REST API Endpoints
- **`POST /api/auth/forgot-password` & `POST /api/auth/send-otp`**:
  - Accepts `username` or `identifier`.
  - Queries MongoDB `engineers` case-insensitively (`{"username": {"$regex": f"^{re.escape(username)}$", "$options": "i"}}`).
  - Generates 6-digit cryptographic OTP and updates `engineers` with `reset_otp` and `otp_created_at`.
  - Delivers directly to the engineer's registered email (`shefeeqkk@tejasnetworks.com`, etc.) without third-party sandbox restrictions.
  - Returns HTTP 200 on success with `"message": "OTP verification code sent to registered email address."` and `"delivered_to": engineer["email"]`.
- **`POST /api/auth/verify-otp`**:
  - Validates `otp` against `reset_otp` in `engineers` collection and checks 10-minute expiry (`time.time() - otp_created_at <= 600`).
  - Returns HTTP 400 with explicit error codes (`INVALID_OTP` or `EXPIRED_OTP`) on mismatch/expiry.
  - On success, updates password with salted PBKDF2 hash, unsets `reset_otp` and `otp_created_at`, and returns HTTP 200.

### 3. 🧪 Verification & Test Suite
- Automated unit tests passed completely:
  1. `POST /api/auth/forgot-password` -> Dispatched email via SSL Port 465 to `shefeeqkk@tejasnetworks.com` (HTTP 200).
  2. `POST /api/auth/verify-otp` with invalid code -> Returned HTTP 400 (`code: INVALID_OTP`).
  3. `POST /api/auth/verify-otp` with valid code -> Successfully reset password and cleared OTP fields (HTTP 200).
- Android build `:app:assembleDebug` passed cleanly.
