# ⚡ RosterX — Enterprise 24/7 NOC Shift Roster & Workforce Management

[![Version](https://img.shields.io/badge/Release-v1.4.5-blue.svg)](https://github.com/shefeeqkk/RosterX/releases)
[![Android](https://img.shields.io/badge/Android-SDK%2035%20%7C%20Compose-3DDC84.svg?logo=android&logoColor=white)](https://developer.android.com/)
[![Python 3.10+](https://img.shields.io/badge/Python-3.10+-3776AB.svg?logo=python&logoColor=white)](https://www.python.org/)
[![MongoDB Atlas](https://img.shields.io/badge/MongoDB-Atlas%20M0-47A248.svg?logo=mongodb&logoColor=white)](https://www.mongodb.com/atlas)
[![Google Gemini](https://img.shields.io/badge/Google%20Gemini-AI%20Engine-8E75C2.svg?logo=google&logoColor=white)](https://ai.google.dev/)
[![Cloudflare Tunnel](https://img.shields.io/badge/Cloudflare-Edge%20Tunnel-F38020.svg?logo=cloudflare&logoColor=white)](https://developers.cloudflare.com/cloudflare-one/connections/connect-networks/)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

**RosterX** is a mission-critical, enterprise-grade shift roster and workforce management platform purpose-built for 24/7 Network Operations Centers (NOC). It combines **Google Gemini AI automated scheduling**, **circadian fatigue safety enforcement**, **swap-first peer coverage tracking**, **MongoDB Atlas cloud synchronization**, and a full-featured **Native Android Mobile App** powered by Jetpack Compose.

---

## 🌟 Key Platform Capabilities

### 📱 1. Native Android Application (Jetpack Compose & Material 3)
- **Modern Jetpack Compose UI**: Vibrant, reactive NOC dark theme with fluid animations, micro-interactions, and collapsible status headers.
- **Biometric Security**: Hardware-backed fingerprint and biometric authentication using `BiometricPrompt` with dedicated admin privilege safeguards.
- **Android 13+ Notification Runtime Handshake**: Automated `POST_NOTIFICATIONS` permission lifecycle management.
- **Engineer Self-Service Portal**:
  - Live **Today / Tomorrow** duty briefing cards.
  - Interactive personal calendar view with shift color codes.
  - Team daily shift inspection and jump-to-today action.
  - Bottom-anchored **Operational Broadcast Notifications** card with individual dismiss and "Clear All" persistence across app restarts.
- **Admin Mobile Command Suite**:
  - **Manage Team**: Live CRUD (add, edit specs, permanent deletion with double confirmation) synchronized across MongoDB Atlas.
  - **Month Rosters Grid**: Interactive spreadsheet navigator with flexible resignation date filtering (historical shift preservation for retired engineers).
  - **Violation Auditor**: Fatigue compliance inspector alerting to consecutive work day breaches and mandatory rest gaps.
  - **Broadcast Dispatch**: Multi-channel emergency and operations announcement broadcaster (in-app alerts + optional Resend email delivery).
  - **Settings & Diagnostics**: Real-time network latency ping, dynamic base URL switching, and live server logs streaming.

### 💻 2. Admin Web Operations Console & Engineer Web Portal
- **Spreadsheet-Style Interactive Duty Grid**: High-speed spreadsheet navigation, cell editing, keyboard shortcuts, and instant shift code validation.
- **Circadian & Fatigue Guards**: Automatic validation against post-night shift rest violations (24–48h mandatory rest), max consecutive work days (cap: 6), and night shift cluster limits.
- **Swap-First Arbitration**: First-class support for peer-to-peer coverage codes (`SW[Name]`), ensuring transparent swap accounting while maintaining minimum headcount quotas.
- **Gemini AI Copilot**: Natural language conversational assistant for whole-month schedule generation, automatic rule compliance, and bulk shift healing.
- **High-Fidelity Excel Engine**: Pixel-perfect `.xlsx` import and export powered by ExcelJS with official NOC styling and frozen headers.

### ☁️ 3. Backend Architecture & Edge Connectivity
- **Native Python Driver Bridge**: Threaded HTTP server (`server.py`) leveraging PyMongo directly against MongoDB Atlas M0.
- **Cloudflare Edge Quick Tunnel**: Instant public HTTPS gateway generation (`python server.py --tunnel`) enabling secure access from mobile devices anywhere in the world on cellular 4G/5G or external Wi-Fi.
- **Automated Mobile URL Sync**: Cloudflare tunnel URLs automatically write to `android/app/src/main/assets/server_url.txt` for automatic app connectivity.
- **Point-in-Time Snapshot Recovery**: Every published schedule is backed up as a versioned recovery snapshot with 1-click instant rollback (`/api/roster-backups/restore`).
- **Resend Transactional Email Engine**: Automated OTP dispatch for password resets and critical operational broadcasts.

---

## 📐 System Architecture

```
                                  ┌─────────────────────────────┐
                                  │      Google Gemini AI       │
                                  │  (gemini-1.5-flash Engine)  │
                                  └──────────────▲──────────────┘
                                                 │
                                                 ▼
┌─────────────────────────────┐    HTTP/JSON     ┌─────────────────────────────┐    PyMongo      ┌─────────────────────────────┐
│    Native Android App       │◄────────────────►│    RosterX Python Server    │◄───────────────►│      MongoDB Atlas M0       │
│  (Kotlin, Compose, Hilt)    │                  │        (server.py)          │                 │ (rosterx: rosters, engine-  │
└─────────────────────────────┘                  └──────────────▲──────────────┘                 │  ers, policies, backups)    │
                                                                │                                └─────────────────────────────┘
┌─────────────────────────────┐    HTTP/JSON                    │ Cloudflare Tunnel
│   Admin Web Operations UI   │◄────────────────────────────────┼───────────────────────────────► 🌐 Worldwide Public HTTPS
│   (HTML5, Vanilla CSS, JS)  │                                 │ (Zero-Config Edge Proxy)
└─────────────────────────────┘                                 ▼
                                                 ┌─────────────────────────────┐
                                                 │   Resend Email Gateway      │
                                                 │ (OTP & Operations Briefing) │
                                                 └─────────────────────────────┘
```

---

## 🚀 Getting Started

### Prerequisites
- **Python**: 3.10 or higher
- **Android Development** *(for building mobile app)*: Android Studio Ladybug / Meerkat or Gradle with Android SDK 35 (JDK 17+)
- **MongoDB Atlas**: Free M0 Cluster or local MongoDB instance

### 1. Clone the Repository
```bash
git clone https://github.com/shefeeqkk/RosterX.git
cd RosterX
```

### 2. Configure Environment
```bash
pip install -r requirements.txt
cp .env.example .env
```
Populate `.env` with your credentials:
```env
# MongoDB Atlas Connection URI
MONGODB_URI=mongodb+srv://<user>:<password>@cluster0.xxxxx.mongodb.net/rosterx?retryWrites=true&w=majority

# Google Gemini API Key
GEMINI_API_KEY=your_gemini_api_key

# Optional: Resend Email Dispatch
RESEND_API_KEY=re_your_resend_key
ALERT_EMAIL=admin@yourdomain.com
```

### 3. Launch Backend & Web Server (Local / Self-Hosted)
```bash
# Start server with integrated Cloudflare Tunnel
python server.py --tunnel

# Or start on local Wi-Fi only
python server.py --open
```

Once started, the interactive console displays:
- **🌐 Worldwide Public URL**: Cloudflare Edge URL (accessible from any 4G/5G/WiFi network)
- **🏠 Local Wi-Fi URL**: `http://192.168.x.x:8000`
- **💻 Local Web App**: `http://localhost:8000`
- **📱 Android Synced Base URL**: Automatically written to `android/app/src/main/assets/server_url.txt`

### 4. Deploy 24/7 to Render (Free Tier $0/mo)
Deploy directly from your GitHub repository to Render with 1-click:
[![Deploy to Render](https://render.com/images/deploy-to-render-button.svg)](https://render.com/deploy)

See the complete step-by-step instructions in [`RENDER_DEPLOYMENT_GUIDE.md`](./RENDER_DEPLOYMENT_GUIDE.md).

---

## 📱 Building & Running the Android App

### Option A: Via Android Studio
1. Open the [`android/`](file:///c:/shefeeq/PYTHON/RosterX/android) folder in Android Studio.
2. Ensure JDK 17+ is selected in **Settings → Build Tools → Gradle**.
3. Sync Gradle project files.
4. Run on a physical Android device or Emulator (API 26 to 35).

### Option B: Via Command Line (Gradle)
```bash
cd android

# Compile and check for errors
./gradlew compileDebugKotlin

# Assemble debug APK
./gradlew assembleDebug

# Install directly to connected device / emulator
./gradlew installDebug
```
*Generated APK location:* `android/app/build/outputs/apk/debug/app-debug.apk`

---

## 📡 REST API Reference

The server exposes 18 verified REST endpoints consumed by both the Web Portal and Android Mobile App:

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/test-connection` | Health check, Atlas M0 ping latency, and public URLs |
| `GET` | `/api/server-info` | Server version, uptime, cluster status, and route list |
| `GET` | `/api/logs` | Real-time terminal server log streaming |
| `POST` | `/api/auth/login` | Authenticate user via username/identifier & password |
| `POST` | `/api/auth/send-otp` | Generate and dispatch 6-digit verification code via email |
| `POST` | `/api/auth/verify-otp` | Verify OTP code and reset user account password |
| `GET` | `/api/engineers` | Retrieve all active & resigned engineers |
| `POST` | `/api/engineers` | Bulk sync/upsert engineer profiles to MongoDB Atlas |
| `POST` | `/api/engineers/delete` | Permanently delete engineer from database and memory |
| `GET` | `/api/roster/{monthKey}` | Retrieve published shift roster for specified month (e.g. `2026-10`) |
| `POST` | `/api/roster` | Save and publish monthly roster grid with snapshot creation |
| `GET` | `/api/roster-backups/{monthKey}` | Fetch historical point-in-time recovery snapshots |
| `POST` | `/api/roster-backups/restore` | 1-Click rollback to any prior published snapshot |
| `GET` | `/api/constraints` | Fetch operational scheduling policies and fatigue constraints |
| `POST` | `/api/constraints` | Synchronize updated operational policies across platforms |
| `GET` | `/api/notifications` | Fetch recent operations and broadcast announcements |
| `POST` | `/api/notifications/send` | Dispatch in-app operations notices and email alerts |

---

## 📂 Project Directory Structure

```text
RosterX/
├── server.py                         # Threaded Python server, PyMongo bridge & Cloudflare tunnel runner
├── requirements.txt                  # Python dependencies (pymongo, dnspython, etc.)
├── public_tunnel_url.txt             # Active Cloudflare public URL for external clients
├── .env.example                      # Environment variables template
├── admin-web/                        # Web Operations & Engineer Self-Service Portal
│   ├── index.html                    # Admin Control Center, Spreadsheet Grid & AI Copilot
│   ├── login.html                    # Unified Login & OTP Password Reset Portal
│   ├── user-portal.html              # Engineer Self-Service Dashboard
│   ├── css/
│   │   └── styles.css                # Enterprise dark theme tokens, grid styles & animations
│   └── js/
│       ├── app.js                    # Admin Master Application Controller
│       ├── constants.js              # Canonical Shift definitions, fatigue rules & colors
│       ├── authService.js            # Session & RBAC Authentication Service
│       ├── atlasService.js           # PyMongo REST client & state synchronization
│       ├── rosterAiEngine.js         # Gemini AI prompt formulation & structured parsing
│       ├── excelService.js           # Enterprise ExcelJS import/export with high-contrast styling
│       └── userPortal.js             # Engineer personal shift schedule & calendar view
├── android/                          # Native Android Mobile App (Jetpack Compose)
│   ├── build.gradle.kts              # Root Gradle configuration
│   ├── AGENT_HANDOFF.md              # Backend ⇄ Android Studio sync & handoff protocol
│   └── app/
│       ├── build.gradle.kts          # Dependencies (Compose, Hilt, Retrofit, Biometrics)
│       └── src/main/
│           ├── AndroidManifest.xml   # Permissions (Internet, Biometric, Post Notifications)
│           ├── assets/
│           │   └── server_url.txt    # Synced server gateway URL
│           └── java/com/rosterx/
│               ├── MainActivity.kt   # Runtime permission requests & screen transitions
│               ├── data/
│               │   ├── remote/       # Retrofit API Service & HostSelectionInterceptor
│               │   └── repository/   # RosterRepository implementation & DTO mappers
│               ├── domain/model/     # Engineer, ShiftCode, OperationalConstraints models
│               ├── presentation/
│               │   ├── admin/        # Admin navigation, tabs (Team, Roster, Broadcast, Settings)
│               │   ├── portal/       # Engineer Portal screen, metrics & broadcast cards
│               │   └── login/        # Biometric & password authentication screen
│               └── util/             # BiometricPromptManager & date utilities
└── README.md
```

---

## 🛡️ License & Contributing

Distributed under the **MIT License**. See [`LICENSE`](LICENSE) for details. Contributions, issues, and feature requests are welcome!
