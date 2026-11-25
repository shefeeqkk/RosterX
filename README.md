# ⚡ RosterX — Enterprise 24/7 Shift Roster & Workforce Management

[![Release](https://img.shields.io/badge/Release-v2.5.31-2563EB.svg)](../../releases/latest)
[![Minimum Supported](https://img.shields.io/badge/Min%20Version-v2.5.31-059669.svg)](../../releases/latest)
[![Android](https://img.shields.io/badge/Android-SDK%2035%20%7C%20Jetpack%20Compose-3DDC84.svg?logo=android&logoColor=white)](https://developer.android.com/)
[![Python 3.10+](https://img.shields.io/badge/Python-3.10+-3776AB.svg?logo=python&logoColor=white)](https://www.python.org/)
[![MongoDB](https://img.shields.io/badge/Database-MongoDB-47A248.svg?logo=mongodb&logoColor=white)](https://www.mongodb.com/)
[![Google Gemini](https://img.shields.io/badge/AI-Google%20Gemini-8E75C2.svg?logo=google&logoColor=white)](https://ai.google.dev/)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

**RosterX** is a production-grade, cross-platform shift roster and operations management platform built for 24/7 engineering and Network Operations teams. It unifies **Google Gemini AI shift scheduling**, **Two-Phase Prioritized Startup Acceleration**, **circadian fatigue compliance**, **leave & peer swap management**, **server credential expiry auditing**, **multi-channel team broadcasts**, and a **Neumorphic Native Android App** (Jetpack Compose) alongside an installable **Web Operations Portal & PWA**.

---

## 🌟 Core Platform Capabilities

### ⚡ 1. Two-Phase Prioritized Startup Acceleration
- **Concurrent Dashboard Fast Path**: Parallelizes critical Dashboard viewport requests (`getEngineers()`, `getCurrentUser()`, and live month roster) via non-blocking asynchronous coroutines over HTTP/2. Dismisses the loading spinner in **~300–400 ms** for instant launch.
- **Asynchronous Background Prefetch**: Defers secondary tabs (Month Roster grid, Notifications, Leave queues, Credentials, and Server Telemetry) to a silent background worker, completely eliminating secondary loading delays when switching views.
- **De-conflicted Network Bandwidth**: Eliminates redundant startup diagnostics pings and artificial delays, guaranteeing maximum network throughput for initial frame rendering.

### 🏖️ 2. Comprehensive Leave & Shift Swap Engine
- **Self-Service Leave Application**: Interactive visual calendar picker highlighting default weekly offs, eligible dates, remaining quota balances, and partner swap options.
- **Policy-Driven Annual Capping**: Configurable annual leave quotas with dynamic capping enforcement, automated quota deductions, and instant self-cancellation.
- **Administrative Approvals Queue**: Dedicated queue for Admins and Managers to review, approve, or reject pending applications with custom operational review notes.
- **One-Click Revocation & Real-Time Sync**: Seamless approval revocation with automatic calendar rollback and real-time team notifications across Android and Web.

### 🤖 3. Automated Shift Roster Generator & Safety Guards
- **Circadian & Fatigue Safety Compliance**: Strict 5-night consecutive duty capping, mandatory 24h–48h post-night rest auto-stamping, and consecutive working day limits.
- **Automated Night Coverage Solver**: Algorithmic night-shift allocation ensuring unbroken 24/7 operations room coverage while balancing workload equity across engineers.
- **Apprentice Alternating Schedule Engine**: Specialized scheduling rules enforcing alternating 6-day off cycles and global apprentice grouping.
- **Multi-Cell Selection & Undo/Redo**: Click-and-drag matrix cell selection, continuous duty fill modes, and keyboard shortcuts (`Ctrl+Z` / `Ctrl+Y`).

### 📱 4. Native Android Application (Jetpack Compose & Material 3)
- **Unified Neumorphic (Soft UI) Design System**: Tactile light and dark dual-theming with sunken input wells, subtle depth surfaces, flat day cells, and responsive tablet/mobile layouts.
- **Role-Based Operational Portals**:
  - **Admin Command Suite**: Team Lifecycle Management, Monthly Roster Matrix, Fatigue Violation Auditor, Multi-Channel Broadcast Console, Server Credential Tracker & Audit Logs, and Live Telemetry Diagnostics.
  - **Manager Operations Console**: Real-time Shift Radar, Team Directory, Broadcast Console, Credential Expiry Tracker & Audit History, and Roster Inspection.
  - **Shift Engineer & Apprentice Portal**: Live Today/Tomorrow duty briefing cards, countdown timers, interactive monthly calendar, Team Shifts directory with 1-tap Call & WhatsApp actions, and collapsible password management.
- **Deep Native Android Integration**:
  - **Verified Android App Links**: Seamless deep-linking into Roster, Notifications, Credentials, and Profile screens via `/.well-known/assetlinks.json`, with automatic desktop browser fallback (`/open/*`).
  - **Launcher Long-Press App Shortcuts**: Instant home-screen jump actions (Today's Roster, Notifications, My Profile, and dynamic role-aware Credential Tracker for Admins/Managers).
  - **1-Tap Calendar Export (`.ics` & Android CalendarContract)**: Direct sync to device calendar accounts and RFC-5545 `.ics` export with exact shift timings and 30-minute reminders.
  - **Resizable Neumorphic Home Screen Widget**: Live shift countdowns and on-duty coverage right on the Android home screen.
  - **Biometric Authentication & FCM Push**: Hardware-backed `BiometricPrompt` login and real-time Firebase Cloud Messaging alerts.

### 🌐 5. Cross-Platform Web Operations Portal & PWA
- **Installable iOS & Android PWA**: Full Progressive Web App support (`/web`) with offline asset caching, standalone display mode, and fluid gesture navigation.
- **1:1 Android Visual Parity**: Matching Neumorphic fluid navigation dock (mobile portrait), adaptive left rail (landscape), and desktop 2-column layout.
- **Gesture-Driven Tab Switching**: Fluid horizontal swipe tab switching with edge spring recoil and Android-style double-back exit protection.

### 🔐 6. Server Credential Expiry Tracker & Audit History
- **Proactive Expiry Surveillance**: Track critical infrastructure credentials with configurable validity periods, custom renewal dates (with validity guards), and per-tracker alert thresholds.
- **Automated Daily Digest Alerts**: Consolidated morning notifications via In-App feed, FCM Push, and HTML Email with deep-link action buttons.
- **Authoritative Audit Log Viewer (Admin & Manager Only)**: Full chronological audit trail (`CREATED`, `UPDATED`, `DELETED`) capturing updater attribution, previous vs. new password dates, search, and action filters.

### 📢 7. Multi-Channel Broadcast Console & Team Celebrations
- **Unified Interactive `@` Mentions**: Cursor-anchored, ultra-compact mention autocomplete (`@name`, `@all`, and `@username`) across **both** Broadcast Title and Message Body.
- **Per-Recipient Personalization**: Dynamic `@name` / `{name}` resolution across Resend HTML Email, FCM Push, and In-App Notification feeds, plus custom reusable broadcast templates and live read receipts.
- **Automated Team Celebrations Engine**: Daily Birthday and Work Anniversary detection with colleague cards, direct wishing actions, and automatic midnight IST hard purge of expired celebrations.

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
│    Web Operations Portal    │◄──────────────────────────────┤
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
Copy the template and populate your credentials and deployment settings:
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
MIN_SUPPORTED_VERSION=2.5.31
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
RosterX includes a ready-to-use [`render.yaml`](./render.yaml) blueprint for zero-config cloud deployment. Configure production secrets (`MONGODB_URI`, `AUTH_SECRET_KEY`, `ADMIN_PASSWORD`, `RESEND_API_KEY`, `ANDROID_APP_LINK_SHA256`, `GEMINI_API_KEY`) directly in your hosting provider's dashboard.

---

## 📱 Building the Android App

### Download Pre-Built APK
Download the latest official release APK (`v2.5.31+`) from the **GitHub Releases** page.

### Build from Source
1. Configure your server base URL in `android/app/src/main/assets/server_url.txt` (e.g., `https://your-rosterx-server.example.com/`).
2. Place your Firebase project configuration (`google-services.json`) in `android/app/` if enabling FCM push notifications.
3. Build using Gradle:
```bash
cd android

# Debug APK
.\safe_build.bat assembleDebug

# Release APK
.\safe_build.bat assembleRelease
```

---

## 📡 Core REST API Surface

| Category | Endpoints |
|---|---|
| **Discovery & Health** | `GET /api/server-info`, `GET /api/test-connection`, `GET /.well-known/assetlinks.json`, `GET /open/*` |
| **Authentication & Security** | `POST /api/auth/login`, `GET /api/auth/me`, `POST /api/auth/change-password`, `POST /api/auth/send-otp`, `POST /api/auth/verify-otp` |
| **Team Directory & Profiles** | `GET /api/engineers`, `POST /api/engineers`, `POST /api/engineers/delete`, `POST /api/users/profile`, `POST /api/users/avatar`, `GET /api/users/{username}/avatar`, `POST /api/users/fcm-token` |
| **Rosters & Snapshots** | `GET /api/roster/{monthKey}`, `POST /api/roster`, `GET /api/roster-backups/{monthKey}`, `POST /api/roster-backups/restore`, `GET /api/constraints`, `POST /api/constraints` |
| **Leave & Shift Swap Engine** | `GET /api/leave/my-requests`, `POST /api/leave/request`, `POST /api/leave/cancel`, `GET /api/leave/admin-queue`, `POST /api/leave/admin-action`, `POST /api/leave/policy-settings` |
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
├── web/                       # Responsive Web Operations Console & iOS PWA Self-Service Portal
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
            ├── util/          # Biometrics, calendar sync exporter & date utilities
            ├── widget/        # Resizable home-screen shift widget provider
            └── worker/        # Background roster sync & FCM messaging service
```

---

## 🛡️ License

Distributed under the **MIT License**. See [`LICENSE`](LICENSE) for full details.
