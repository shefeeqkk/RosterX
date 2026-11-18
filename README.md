# ⚡ RosterX — Enterprise 24/7 Shift Roster & Workforce Management

[![Release](https://img.shields.io/badge/Release-v1.34.0-2563EB.svg)](../../releases/latest)
[![Minimum Supported](https://img.shields.io/badge/Min%20Version-v1.34.0-059669.svg)](../../releases/latest)
[![Android](https://img.shields.io/badge/Android-SDK%2035%20%7C%20Jetpack%20Compose-3DDC84.svg?logo=android&logoColor=white)](https://developer.android.com/)
[![Python 3.10+](https://img.shields.io/badge/Python-3.10+-3776AB.svg?logo=python&logoColor=white)](https://www.python.org/)
[![MongoDB](https://img.shields.io/badge/Database-MongoDB-47A248.svg?logo=mongodb&logoColor=white)](https://www.mongodb.com/)
[![Google Gemini](https://img.shields.io/badge/AI-Google%20Gemini-8E75C2.svg?logo=google&logoColor=white)](https://ai.google.dev/)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

**RosterX** is a production-grade, cross-platform shift roster and operations management platform built for 24/7 engineering and Network Operations teams. It unifies **Google Gemini AI shift scheduling**, **circadian fatigue compliance**, **swap-first peer coverage**, **server credential expiry auditing**, **multi-channel team broadcasts**, and a **Neumorphic Native Android App** (Jetpack Compose) alongside a responsive **Web Operations Portal**.

---

## 🌟 Core Platform Capabilities

### 📱 1. Native Android Application (Jetpack Compose & Material 3)
- **Unified Neumorphic (Soft UI) Design System**: Tactile light and dark dual-theming with sunken input wells, subtle depth surfaces, global pull-to-refresh, and responsive tablet/mobile layouts.
- **Role-Based Operational Portals**:
  - **Admin Command Suite**: Team Lifecycle Management, Monthly Roster Matrix, Fatigue Violation Auditor, Multi-Channel Broadcast Console, Server Credential Tracker & Audit Logs, and Live Telemetry Diagnostics.
  - **Manager Operations Console**: Real-time Shift Radar, Team Directory, Broadcast Console, Credential Expiry Tracker & Audit History, and Roster Inspection.
  - **Shift Engineer & Apprentice Portal**: Live Today/Tomorrow duty briefing cards, countdown timers, interactive monthly calendar, Team Shifts directory with 1-tap Call & WhatsApp actions, and self-service Profile management.
- **Deep Native Android Integration**:
  - **Verified Android App Links**: Seamless deep-linking into Roster, Notifications, Credentials, and Profile screens via `/.well-known/assetlinks.json`, with automatic desktop browser fallback (`/open/*`).
  - **Launcher Long-Press App Shortcuts**: Instant home-screen jump actions (Today's Roster, Notifications, My Profile, and dynamic role-aware Credential Tracker for Admins/Managers).
  - **1-Tap Calendar Export (`.ics`)**: Batch export monthly working shifts as RFC-5545 calendar events (skipping OFF/Swap/Leave days, including exact shift timings and 30-minute reminders).
  - **Resizable Neumorphic Home Screen Widget**: Live shift countdowns and on-duty coverage right on the Android home screen.
  - **Native Splash Screen & Predictive Back**: Android 12+ `SplashScreen` API and smooth predictive back gestures.
  - **In-App Update Banner**: Automatic semantic version comparison against backend release metadata with 1-tap APK release link.
  - **Biometric Authentication & FCM Push**: Hardware-backed `BiometricPrompt` login and real-time Firebase Cloud Messaging alerts.

### 🔐 2. Server Credential Expiry Tracker & Audit History
- **Proactive Expiry Surveillance**: Track critical infrastructure credentials with configurable validity periods, custom renewal dates (with validity guards), and per-tracker alert thresholds.
- **Automated Daily Digest Alerts**: Consolidated morning notifications via In-App feed, FCM Push, and HTML Email with deep-link action buttons.
- **Authoritative Audit Log Viewer (Admin & Manager Only)**: Full chronological audit trail (`CREATED`, `UPDATED`, `DELETED`) capturing who updated each credential, previous vs. new password dates, search, and action filters.

### 📢 3. Multi-Channel Broadcast Console & Team Celebrations
- **Unified Interactive `@` Mentions**: Cursor-anchored, ultra-compact mention autocomplete (`@name`, `@all`, and `@username`) across **both** Broadcast Title and Message Body.
- **Per-Recipient Personalization**: Dynamic `@name` / `{name}` resolution across Resend HTML Email, FCM Push, and In-App Notification feeds, plus custom reusable broadcast templates and live read receipts.
- **Automated Team Celebrations Engine**: Daily Birthday and Work Anniversary detection with 3rd-person colleague cards, direct WhatsApp/Call wishing actions, and automatic midnight IST hard purge of expired celebrations.

### 🤖 4. AI Scheduling Engine & Web Operations Console
- **Grounded Gemini AI Operations Assistant**: Natural-language schedule generation, daily coverage analysis, and RAG-backed roster Q&A via a secure server-side AI proxy.
- **Circadian & Fatigue Safety Guards**: Enforces mandatory post-night rest windows (24h–48h), consecutive working day caps, and night-shift block constraints.
- **DB-Driven Swap-First Arbitration**: Supports peer coverage codes (`SW...`) configured via database metadata or algorithmic initial disambiguation.
- **Point-in-Time Snapshot Recovery & Excel Engine**: Versioned roster backups with 1-click rollback and styled `.xlsx` import/export via ExcelJS.

### 🛡️ 5. Security & Zero-Knowledge Account Architecture
- **Zero-Knowledge Account Onboarding**: Administrators create accounts without ever setting, viewing, or overwriting user passwords. New accounts receive a cryptographic random password hash and an automated Welcome email guiding private password setup via **Email OTP**.
- **Hardened Authentication**: HMAC-SHA256 signed JWT tokens with role-based access control (RBAC), PBKDF2-HMAC-SHA256 password hashing (200,000 iterations + per-user salt), SHA-256 hashed 6-digit OTP verification, and per-IP/per-account brute-force rate limiting.
- **Database-Driven Metadata**: Team roster visibility (`include_in_roster`), display order (`display_order`), and swap codes (`swap_code`) are stored cleanly in MongoDB with zero hardcoded user data.

---

## 📐 System Architecture

```text
                                  ┌───────────────────────────────┐
                                  │       Google Gemini AI        │
                                  │  (Server-Side Secure Proxy)   │
                                  └───────────────▲───────────────┘
                                                  │
                                                  ▼
┌─────────────────────────────┐   HTTPS/JSON   ┌─────────────────────────────┐    PyMongo    ┌─────────────────────────────┐
│    Native Android App       │◄──────────────►│    RosterX Python Server    │◄─────────────►│          MongoDB            │
│ (Kotlin, Compose, Hilt, FCM)│                │         (server.py)         │               │ (rosters, engineers, logs,  │
└─────────────────────────────┘                └──────────────▲──────────────┘               │  trackers, notifications)   │
                                                              │                              └─────────────────────────────┘
┌─────────────────────────────┐   HTTPS/JSON                  │
│   Web Operations Portal     │◄──────────────────────────────┤
│  (Admin, Manager, Engineer) │                               │
└─────────────────────────────┘                               ▼
                                               ┌─────────────────────────────┐
                                               │  Resend Email & Google FCM  │
                                               │ (OTP, Broadcasts & Alerts)  │
                                               └─────────────────────────────┘
```

---

## 🚀 Getting Started

### Prerequisites
- **Python**: 3.10 or higher
- **MongoDB**: MongoDB Atlas or self-hosted MongoDB instance
- **Android Development** *(optional, for building the mobile app)*: Android Studio (JDK 17+, Android SDK 35)

### 1. Clone & Install Dependencies
```bash
git clone <repository-url>
cd RosterX
pip install -r requirements.txt
```

### 2. Configure Environment Variables
Copy the template and populate your own credentials and deployment settings:
```bash
cp .env.example .env
```

Key environment variables (see [`.env.example`](./.env.example) for the complete list):
```env
# Database
MONGODB_URI=mongodb+srv://<user>:<password>@<cluster-host>/<database>?retryWrites=true&w=majority
ATLAS_CLUSTER=<your-cluster-name>
ATLAS_DATABASE=rosterx

# Security & Authentication
AUTH_SECRET_KEY=<64-char-random-hex-key>
ADMIN_USERNAME=admin
ADMIN_PASSWORD=<your-strong-admin-password>

# AI & Transactional Email
GEMINI_API_KEY=<your-gemini-api-key>
RESEND_API_KEY=<your-resend-api-key>
ALERT_EMAIL=admin@example.com

# Android App Links & Update Metadata
ANDROID_APP_LINK_SHA256=<YOUR_SIGNING_KEYSTORE_SHA256>
APP_PUBLIC_BASE_URL=https://your-rosterx-server.example.com
MIN_SUPPORTED_VERSION=1.34.0
APK_DOWNLOAD_URL=https://github.com/<owner>/<repo>/releases/latest
```

### 3. Run the Backend & Web Portal
```bash
# Production / Headless mode
python server.py --no-open

# Local development mode (opens web portal automatically)
python server.py --open
```

### 4. Cloud Deployment
RosterX includes a ready-to-use [`render.yaml`](./render.yaml) blueprint for zero-config cloud deployment. Set secret environment variables (`MONGODB_URI`, `AUTH_SECRET_KEY`, `ADMIN_PASSWORD`, `RESEND_API_KEY`, `ANDROID_APP_LINK_SHA256`, `GEMINI_API_KEY`) directly in your hosting provider's dashboard.

---

## 📱 Building the Android App

### Download Pre-Built APK
Download the latest official release APK (`v1.34.0+`) from the **GitHub Releases** page.

### Build from Source
1. Configure your server base URL in `android/app/src/main/assets/server_url.txt` (e.g., `https://your-rosterx-server.example.com/`).
2. Place your Firebase project configuration (`google-services.json`) in `android/app/` if enabling FCM push notifications.
3. Build using Gradle:
```bash
cd android

# Debug APK
./gradlew assembleDebug

# Release APK
./gradlew assembleRelease
```

---

## 📡 Core REST API Surface

| Category | Endpoints |
|---|---|
| **Discovery & Health** | `GET /api/server-info`, `GET /api/test-connection`, `GET /.well-known/assetlinks.json`, `GET /open/*` |
| **Authentication & Security** | `POST /api/auth/login`, `GET /api/auth/me`, `POST /api/auth/change-password`, `POST /api/auth/send-otp`, `POST /api/auth/verify-otp` |
| **Team Directory & Profiles** | `GET /api/engineers`, `POST /api/engineers`, `POST /api/engineers/delete`, `POST /api/users/profile`, `POST /api/users/avatar`, `GET /api/users/{username}/avatar`, `POST /api/users/fcm-token` |
| **Rosters & Snapshots** | `GET /api/roster/{monthKey}`, `POST /api/roster`, `GET /api/roster-backups/{monthKey}`, `POST /api/roster-backups/restore`, `GET /api/constraints`, `POST /api/constraints` |
| **Credential Tracker & Audit** | `GET /api/credentials/trackers`, `POST /api/credentials/trackers`, `PUT /api/credentials/trackers/{id}`, `POST /api/credentials/trackers/{id}/renew`, `DELETE /api/credentials/trackers/{id}`, `GET /api/credentials/trackers/logs` |
| **Broadcasts & Celebrations** | `GET /api/notifications`, `POST /api/notifications/send`, `POST /api/notifications/dismiss`, `POST /api/notifications/retract`, `GET/POST /api/broadcast/templates`, `GET /api/celebrations/today` |
| **AI & Telemetry** | `POST /api/ai/chat`, `POST /api/ai/generate-roster`, `GET /api/telemetry/server`, `GET /api/telemetry/users`, `GET /api/logs` |

---

## 📂 Project Structure

```text
RosterX/
├── server.py                  # Hardened Python HTTP server, RBAC JWT auth, Atlas bridge & cron jobs
├── render.yaml                # Cloud deployment blueprint
├── requirements.txt           # Python runtime dependencies
├── .env.example               # Environment configuration template
├── admin-web/                 # Responsive Web Operations Console & Self-Service Portal
│   ├── index.html             # Admin Control Center, Roster Matrix & AI Copilot
│   ├── login.html             # Unified Login & Email OTP Recovery
│   ├── user-portal.html       # Shift Engineer Self-Service Dashboard
│   ├── css/                   # Neumorphic light/dark design tokens & responsive styles
│   └── js/                    # Controllers, constants, auth, AI assistant & ExcelJS engine
└── android/                   # Native Android App (Kotlin, Jetpack Compose, Material 3, Hilt)
    └── app/src/main/
        ├── AndroidManifest.xml
        ├── res/               # Adaptive icons, launcher shortcuts, splash themes & widgets
        └── java/com/rosterx/
            ├── data/          # Retrofit API service, repositories & session manager
            ├── domain/        # Models, shift rules & AI roster service
            ├── presentation/  # Admin, Manager, Engineer screens & Neumorphic components
            ├── util/          # Biometrics, calendar .ics exporter & date utilities
            ├── widget/        # Resizable home-screen shift widget provider
            └── worker/        # Background roster sync & FCM messaging service
```

---

## 🛡️ License

Distributed under the **MIT License**. See [`LICENSE`](LICENSE) for full details.
