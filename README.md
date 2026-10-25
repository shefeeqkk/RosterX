# ⚡ RosterX — 24/7 NOC Shift Roster & Workforce Management

[![Python 3.10+](https://img.shields.io/badge/python-3.10+-blue.svg)](https://www.python.org/downloads/)
[![MongoDB Atlas](https://img.shields.io/badge/MongoDB-Atlas%20M0-green.svg)](https://www.mongodb.com/atlas)
[![Gemini AI](https://img.shields.io/badge/Google%20Gemini-AI%20Engine-purple.svg)](https://ai.google.dev/)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

An enterprise-grade shift roster management platform purpose-built for 24/7 Network Operations Centers (NOC). Combines **Google Gemini AI structured scheduling**, **circadian fatigue safety enforcement**, **swap-first arbitration**, and real-time **MongoDB Atlas** persistence.

---

## ✨ Key Features

- 🤖 **Conversational AI Roster Assistant**: Natural language prompt-based grid manipulation, bulk healing, and automated whole-month generation powered by Google Gemini.
- 🛡️ **Circadian & Fatigue Guards**: Automatic validation against post-night shift rest violations (24–48h mandatory rest), max consecutive work days (cap: 6), and night shift cluster limits.
- 🔄 **Swap-First Arbitration**: First-class support for peer-to-peer coverage codes (`SW[Name]`), ensuring transparent swap accounting while maintaining minimum headcount quotas.
- 👥 **Role-Based Portals**:
  - **Admin Control Center**: Interactive spreadsheet grid, team CRUD, domain-based usernames (`domain_id`), dynamic shift filter toggles, and live headcount statistics.
  - **Engineer Self-Service Portal**: Personal shift calendars, team monthly schedule visibility, and password management with email OTP verification.
- 📊 **Enterprise Excel Engine**: High-fidelity `.xlsx` export/import using cell styling, official NOC color palettes, and frozen headers via ExcelJS.
- ☁️ **Flexible Storage & Sync**: Native Python PyMongo REST API bridge connected to MongoDB Atlas M0 with transparent offline fallback.

---

## 🚀 Quick Start

### 1. Clone & Setup
```bash
git clone https://github.com/shefeeqkk/RosterX.git
cd RosterX

# Install Python requirements
pip install -r requirements.txt

# Copy environment variables template
cp .env.example .env
```

### 2. Configure Credentials
Edit `.env` with your real keys:
```env
MONGODB_URI=mongodb+srv://<user>:<password>@cluster0.xxxxx.mongodb.net/?retryWrites=true&w=majority
GEMINI_API_KEY=your_gemini_api_key
```

### 3. Launch Secure Server
```bash
# Launch server (default port 8000)
python server.py

# Optional: automatically open in default browser
python server.py --open

# Optional: start Cloudflare Tunnel for secure remote / mobile access
python server.py --tunnel
```

---

## 📐 Project Structure

```text
RosterX/
├── server.py                   # High-performance PyMongo REST server + security proxy
├── requirements.txt            # Python dependencies (pymongo, dnspython, etc.)
├── .env.example                # Environment variables template
├── admin-web/
│   ├── index.html              # Admin Control Center & Spreadsheet Grid
│   ├── login.html              # Unified Login & OTP Password Reset Portal
│   ├── user-portal.html        # Engineer Self-Service Dashboard
│   ├── css/
│   │   └── styles.css          # Core animations, grid styles & theme tokens
│   ├── js/
│   │   ├── app.js              # Admin Master Application Controller
│   │   ├── constants.js        # Canonical Shift definitions, rules & helper utilities
│   │   ├── authService.js      # Session & RBAC Authentication Service
│   │   ├── atlasService.js     # PyMongo REST client & state synchronization
│   │   ├── rosterAiEngine.js   # Gemini AI prompt formulation & structured parsing
│   │   ├── rosterAiAssistant.js# Conversational interactive AI Copilot
│   │   ├── excelService.js     # Enterprise ExcelJS import/export with high-contrast styling
│   │   └── userPortal.js       # Engineer personal shift schedule & calendar view
│   └── serverless/
│       └── atlas-functions/    # MongoDB Atlas App Services triggers & OTP functions
└── README.md
```

---

## 🛡️ License
Licensed under the [MIT License](LICENSE).
