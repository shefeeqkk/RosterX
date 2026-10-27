# 🚀 RosterX — Render Free Tier ($0/month) Cloud Deployment Guide

This guide walks you step-by-step through deploying **RosterX** directly from your GitHub repository ([`shefeeqkk/RosterX`](https://github.com/shefeeqkk/RosterX)) to **Render** on the **100% Free Plan ($0/month)** with automated HTTPS SSL, global CDN, and automatic continuous deployment.

---

## 📋 Prerequisites
1. A **GitHub account** with access to your repository: `https://github.com/shefeeqkk/RosterX`.
2. A free **Render account**: Sign up for free at [render.com](https://render.com) using your GitHub account.
3. Your **MongoDB Atlas Connection URI** (e.g., `mongodb+srv://user:pass@cluster0.xxxxx.mongodb.net/?retryWrites=true&w=majority`).

---

## ⚡ Method 1: Instant Blueprint Deployment (Recommended)

Because RosterX includes a pre-configured [`render.yaml`](./render.yaml) blueprint, Render can configure everything automatically with 1 click.

1. Log into your [Render Dashboard](https://dashboard.render.com).
2. Click the **"New +"** button in the top navigation bar.
3. Select **"Blueprint"**.
4. Connect your GitHub repository: **`shefeeqkk/RosterX`**.
5. Render will automatically parse `render.yaml` and show:
   * **Service Name**: `rosterx`
   * **Environment**: `Python`
   * **Plan**: `Free ($0)`
   * **Start Command**: `python server.py --no-tunnel --no-open`
6. Under **Environment Variables**:
   * Enter your `MONGODB_URI` value.
   * *(Optional)* Enter your `RESEND_API_KEY` for email dispatch.
7. Click **"Apply"** / **"Create Blueprint"**.
8. Render will pull from GitHub, install dependencies via `pip install -r requirements.txt`, start the service, and generate your live URL (e.g. `https://rosterx.onrender.com`).

---

## 🛠️ Method 2: Manual Web Service Setup (Step-by-Step)

If you prefer to configure the Web Service manually:

### Step 1: Create Web Service
1. Go to [dashboard.render.com](https://dashboard.render.com).
2. Click **New +** ➔ **Web Service**.
3. Choose **"Build and deploy from a Git repository"** and select **`shefeeqkk/RosterX`**.

### Step 2: Configure Service Settings
Fill in the following fields:

| Field | Value |
| :--- | :--- |
| **Name** | `rosterx` *(or any name you choose)* |
| **Region** | Choose nearest to you (e.g., `Oregon (US West)` or `Frankfurt (EU)`) |
| **Branch** | `main` |
| **Root Directory** | *(Leave blank)* |
| **Runtime** | `Python 3` |
| **Build Command** | `pip install -r requirements.txt` |
| **Start Command** | `python server.py --no-tunnel --no-open` |
| **Instance Type** | **Free ($0/month)** |

### Step 3: Add Environment Variables
Scroll down to the **Environment Variables** section and add:

| Key | Value | Notes |
| :--- | :--- | :--- |
| `PYTHON_VERSION` | `3.11.9` | Ensures modern Python runtime |
| `MONGODB_URI` | `mongodb+srv://...` | Your MongoDB Atlas connection string |
| `ATLAS_CLUSTER` | `Cluster0` | Your Atlas cluster name |
| `ATLAS_DATABASE` | `rosterx` | Target database name |
| `ENABLE_TUNNEL` | `false` | Disables local Cloudflare tunnel |
| `RESEND_API_KEY` | `re_...` | *(Optional)* For transactional emails & OTP |
| `ALERT_EMAIL` | `kkshefeeq005@gmail.com` | Administrator email |

### Step 4: Deploy
1. Click **"Create Web Service"** at the bottom of the page.
2. Render will build and deploy your service in ~1–2 minutes.
3. Once the log says `Status: Server is actively listening on all interfaces (0.0.0.0)`, your app is live!

---

## 🌐 Accessing Your Hosted Application

Your live public URL will be displayed at the top of the Render dashboard (e.g., `https://rosterx.onrender.com`):

* **Desktop Admin Panel**: `https://rosterx.onrender.com` (automatically redirects unauthenticated users to `/login.html`).
* **Engineer Portal**: `https://rosterx.onrender.com/user-portal.html`
* **Health Check & Diagnostic Info**: `https://rosterx.onrender.com/api/server-info`
* **REST API Endpoint**: `https://rosterx.onrender.com/api/`

---

## 📱 Connecting the Android Mobile App to Render

1. Open the **RosterX Android App** on your phone.
2. On the Login screen, tap the **⚙️ Settings icon** (or navigate to **Settings ➔ Server Connection**).
3. Set the **Server URL** to your Render URL with `/api/`:
   ```
   https://rosterx.onrender.com/api/
   ```
4. Tap **Save & Apply**.
5. The Android app will now communicate directly with your Render cloud backend over 4G/5G/Wi-Fi without needing any local computer or tunnel running!

---

## 💡 Render Free Plan Considerations
* **Cost**: $0.00 / month forever.
* **Inactivity Sleep**: Render's free tier spins down the web service after 15 minutes of inactivity. When a new request arrives, it wakes up automatically in ~20–30 seconds.
* **Auto-Deploy**: Every time you push a git commit to `main`, Render automatically rebuilds and deploys the newest version within 60 seconds!
