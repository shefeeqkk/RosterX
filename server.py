#!/usr/bin/env python3
"""
RosterX - Secure Local Development Server & Native Python Atlas Bridge
Provides:
  - Isolated serving of 'admin-web/' directory
  - Native MongoDB Atlas REST API bridge using PyMongo (test-connection, rosters, engineers)
  - Full CORS & preflight OPTIONS support for browser clients
  - Strict blocking of .env, dotfiles, and sensitive configs (403 Forbidden)
  - Automatic Local & Network (LAN) IP resolution
  - Cloudflare Quick Tunnel integration
  - Interactive terminal dashboard with live Atlas M0 status
"""

import os
import sys
import socket
import argparse
import webbrowser
import subprocess
import shutil
import re
import time
import json
import threading
import collections
from http.server import HTTPServer, SimpleHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path

SERVER_VERSION = "1.6.0"

# Enable UTF-8 encoding on Windows consoles
if sys.platform == "win32":
    try:
        if sys.stdout and hasattr(sys.stdout, "reconfigure"):
            sys.stdout.reconfigure(encoding="utf-8", errors="replace", line_buffering=True)
        if sys.stderr and hasattr(sys.stderr, "reconfigure"):
            sys.stderr.reconfigure(encoding="utf-8", errors="replace", line_buffering=True)
    except Exception:
        pass

SERVER_VERSION = "1.4.5"

# Base Paths
ROOT_DIR = Path(__file__).resolve().parent
WEB_DIR = ROOT_DIR / "admin-web"

# Global Atlas Bridge State
ATLAS_CLIENT = None
ATLAS_DB = None
ATLAS_STATUS = "DISCONNECTED"
ATLAS_ERROR = None
ATLAS_CLUSTER = "Cluster0"
ATLAS_DB_NAME = "rosterx"

# Server Version (Follows project semantic versioning rule: small=patch, semi-big=minor, major=major)
SERVER_VERSION = "1.6.0"

# Global Public Tunnel & Internet Gateway State
PUBLIC_TUNNEL_URL = None
TUNNEL_PROC = None
SERVER_PORT = 8000

# Global Live Server Log Ring Buffer (Available for Mobile Terminal Log Viewer)
SERVER_LOGS = collections.deque(maxlen=100)
SERVER_NOTIFICATIONS = collections.deque(maxlen=100)

def init_atlas_connection(env):
    """Initialize persistent PyMongo client connection to MongoDB Atlas."""
    global ATLAS_CLIENT, ATLAS_DB, ATLAS_STATUS, ATLAS_ERROR, ATLAS_CLUSTER, ATLAS_DB_NAME

    mongo_uri = env.get("MONGODB_URI", "").strip()
    ATLAS_CLUSTER = env.get("ATLAS_CLUSTER", "Cluster0").strip()
    ATLAS_DB_NAME = env.get("ATLAS_DATABASE", "rosterx").strip()

    if not mongo_uri:
        ATLAS_STATUS = "DISCONNECTED"
        ATLAS_ERROR = "MONGODB_URI not found in environment or .env"
        return

    try:
        from pymongo import MongoClient
        # 5-second server selection timeout for responsive failure handling
        ATLAS_CLIENT = MongoClient(mongo_uri, serverSelectionTimeoutMS=5000)
        ATLAS_CLIENT.admin.command("ping")
        ATLAS_DB = ATLAS_CLIENT[ATLAS_DB_NAME]
        ATLAS_STATUS = "CONNECTED (Ready)"
        ATLAS_ERROR = None
    except ImportError:
        ATLAS_STATUS = "DISCONNECTED"
        ATLAS_ERROR = "pymongo not installed (run: pip install 'pymongo[srv]>=4.6.0')"
    except Exception as e:
        ATLAS_STATUS = "DISCONNECTED"
        ATLAS_ERROR = str(e)

# Active In-Memory OTP Store & Resend Configuration
ACTIVE_OTPS = {}  # { clean_id: {"code": "123456", "expiresAt": timestamp, "email": "...", "attempts": 0} }
OTP_RATE_LIMIT = {}  # { clean_id: last_request_timestamp }
DEFAULT_RESEND_API_KEY = os.environ.get("RESEND_API_KEY", "")
DEFAULT_ALERT_EMAIL = os.environ.get("ALERT_EMAIL", "kkshefeeq005@gmail.com")
RESEND_API_KEY = DEFAULT_RESEND_API_KEY
RESEND_FROM = os.environ.get("RESEND_FROM", "RosterX Operations <onboarding@resend.dev>")

DEFAULT_SERVER_ENGINEERS = [
    {"id": "eng_03", "name": "NANDU ANTHIKAT", "username": "nandua", "email": "nandua@tejasnetworks.com", "role": "engineer"},
    {"id": "eng_04", "name": "SHEFEEQ", "username": "shefeeqkk", "email": "shefeeqkk@tejasnetworks.com", "role": "admin"},
    {"id": "eng_05", "name": "DYANI DESOZA", "username": "dyanid", "email": "dyanid@tejasnetworks.com", "role": "engineer"},
    {"id": "eng_06", "name": "ATUL KANGRALKAR", "username": "athulka", "email": "athulka@tejasnetworks.com", "role": "admin"},
    {"id": "eng_07", "name": "SILAMBARASAN", "username": "silambarasan", "email": "silambarasan@tejasnetworks.com", "role": "engineer"},
    {"id": "eng_08", "name": "HEMANTH", "username": "hemanthti", "email": "hemanthti@tejasnetworks.com", "role": "engineer"},
    {"id": "eng_01", "name": "MANOJ", "username": "manoj", "email": "manoj@tejasnetworks.com", "role": "engineer"},
    {"id": "eng_10", "name": "DHANUSH", "username": "dhanush", "email": "dhanush@tejasnetworks.com", "role": "apprentice"},
    {"id": "eng_09", "name": "ROHIT", "username": "rohit", "email": "rohit@tejasnetworks.com", "role": "apprentice"},
    {"id": "eng_02", "name": "RAJ KUMAR", "username": "rajkumar", "email": "rajkumar@tejasnetworks.com", "role": "engineer"},
]

def send_resend_email(to_email, subject, html_content):
    """Dispatch real transactional email via Resend API with sandbox auto-fallback."""
    global RESEND_API_KEY, RESEND_FROM, DEFAULT_RESEND_API_KEY
    if not RESEND_API_KEY:
        env = load_env_variables()
        RESEND_API_KEY = env.get("RESEND_API_KEY", "").strip() or os.environ.get("RESEND_API_KEY", "")
    api_key = RESEND_API_KEY or DEFAULT_RESEND_API_KEY
    if not api_key:
        return {"success": False, "error": "RESEND_API_KEY is not configured in .env"}

    import urllib.request
    payload = {
        "from": RESEND_FROM,
        "to": [to_email],
        "subject": subject,
        "html": html_content
    }

    req = urllib.request.Request(
        "https://api.resend.com/emails",
        headers={
            "Authorization": f"Bearer {api_key}",
            "Content-Type": "application/json",
            "User-Agent": "resend-python:2.0.0"
        },
        data=json.dumps(payload).encode("utf-8")
    )

    try:
        with urllib.request.urlopen(req, timeout=12) as resp:
            data = json.loads(resp.read().decode("utf-8"))
            return {"success": True, "id": data.get("id"), "delivered_to": to_email}
    except urllib.error.HTTPError as e:
        err_msg = e.read().decode("utf-8", errors="replace")
        # Handle Resend Free Tier Sandbox restriction (can only send to registered Resend account owner)
        if "kkshefeeq005@gmail.com" in err_msg and to_email.lower() != "kkshefeeq005@gmail.com":
            print("  [RESEND SANDBOX] Retrying dispatch to verified account owner kkshefeeq005@gmail.com...")
            payload["to"] = ["kkshefeeq005@gmail.com"]
            retry_req = urllib.request.Request(
                "https://api.resend.com/emails",
                headers={
                    "Authorization": f"Bearer {api_key}",
                    "Content-Type": "application/json",
                    "User-Agent": "resend-python:2.0.0"
                },
                data=json.dumps(payload).encode("utf-8")
            )
            try:
                with urllib.request.urlopen(retry_req, timeout=12) as rresp:
                    rdata = json.loads(rresp.read().decode("utf-8"))
                    return {
                        "success": True, 
                        "id": rdata.get("id"), 
                        "delivered_to": "kkshefeeq005@gmail.com", 
                        "notice": "Resend Sandbox Mode: Delivered to verified account owner kkshefeeq005@gmail.com"
                    }
            except Exception as re_err:
                return {"success": False, "error": f"Resend retry error: {re_err}"}
        return {"success": False, "error": f"Resend HTTP {e.code}: {err_msg}"}
    except Exception as e:
        return {"success": False, "error": str(e)}

def send_startup_link_email(public_url, lan_ip, port, to_email="kkshefeeq005@gmail.com"):
    """
    Dispatch a professional HTML email notification to the administrator with the live Cloudflare link
    and connection instructions for the Android app and web portal.
    """
    global ATLAS_CLUSTER, ATLAS_DB_NAME, ATLAS_STATUS

    if not to_email:
        to_email = "kkshefeeq005@gmail.com"

    timestamp_str = time.strftime("%Y-%m-%d %H:%M:%S UTC", time.gmtime())
    active_url = public_url or f"http://{lan_ip}:{port}"
    api_url = f"{public_url}/api/" if public_url else f"http://{lan_ip}:{port}/api/"
    
    subject = f"🚀 RosterX Server Online — Public Gateway Link: {public_url or f'http://{lan_ip}:{port}'}"
    
    html_content = f"""<!DOCTYPE html>
<html>
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>RosterX Server Online</title>
</head>
<body style="margin:0;padding:0;background-color:#0b1120;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Helvetica,Arial,sans-serif;color:#f1f5f9;">
  <table width="100%" border="0" cellspacing="0" cellpadding="0" style="background-color:#0b1120;padding:40px 15px;">
    <tr>
      <td align="center">
        <table width="100%" style="max-width:600px;background:#0f172a;border:1px solid #1e293b;border-radius:18px;overflow:hidden;box-shadow:0 15px 35px rgba(0,0,0,0.6);">
          <!-- Header -->
          <tr>
            <td style="padding:32px 36px;background:linear-gradient(135deg,#0284c7 0%,#2563eb 50%,#4f46e5 100%);text-align:center;">
              <h1 style="margin:0;color:#ffffff;font-size:24px;font-weight:900;letter-spacing:1px;text-transform:uppercase;">PROJECT ROSTERX</h1>
              <p style="margin:6px 0 0 0;color:#e0f2fe;font-size:14px;font-weight:600;">24/7 NOC Shift Operations & Dynamic Gateway</p>
            </td>
          </tr>
          
          <!-- Content Body -->
          <tr>
            <td style="padding:36px;">
              <div style="display:inline-block;padding:4px 12px;background:#064e3b;border:1px solid #059669;border-radius:20px;color:#a7f3d0;font-size:12px;font-weight:700;margin-bottom:16px;">
                ● LIVE SERVER & TUNNEL ACTIVE
              </div>
              <h2 style="margin:0 0 14px 0;color:#ffffff;font-size:20px;font-weight:800;">
                New Gateway Established! 🌐
              </h2>
              <p style="margin:0 0 24px 0;color:#94a3b8;font-size:14px;line-height:1.6;">
                Hello <strong>Shefeeq</strong>,<br>
                Your RosterX Operations Server has initialized successfully. A secure Cloudflare Public Edge Gateway is active, making the system reachable across <strong>all mobile cellular networks (4G/5G)</strong> and foreign Wi-Fi connections worldwide.
              </p>

              <!-- Primary Gateway Box -->
              <div style="background:#020617;border:2px solid #0284c7;border-radius:14px;padding:22px;margin:24px 0;">
                <div style="color:#38bdf8;font-size:11px;font-weight:800;letter-spacing:1.5px;text-transform:uppercase;margin-bottom:6px;">
                  🌐 Worldwide Public Web Portal:
                </div>
                <div style="margin-bottom:18px;">
                  <a href="{public_url or '#'}" target="_blank" style="color:#ffffff;font-size:16px;font-weight:800;text-decoration:underline;word-break:break-all;">
                    {public_url or 'Tunnel not initialized'}
                  </a>
                </div>

                <div style="color:#a78bfa;font-size:11px;font-weight:800;letter-spacing:1.5px;text-transform:uppercase;margin-bottom:6px;">
                  📱 Android App Base URL (Copy & Paste):
                </div>
                <div style="background:#0f172a;border:1px solid #334155;border-radius:8px;padding:12px 14px;font-family:'Courier New',Courier,monospace;font-size:14px;color:#38bdf8;font-weight:700;word-break:break-all;user-select:all;">
                  {api_url}
                </div>
              </div>

              <!-- Quick Setup Guide for Android App -->
              <h3 style="margin:24px 0 10px 0;color:#ffffff;font-size:15px;font-weight:700;">
                📱 How to Connect from the RosterX Android App:
              </h3>
              <ol style="margin:0 0 24px 0;padding-left:20px;color:#cbd5e1;font-size:13px;line-height:1.7;">
                <li>Open the <strong>RosterX Android App</strong> on your phone.</li>
                <li>On the Login screen, tap the <strong>⚙️ Settings icon</strong> in the top-right corner.</li>
                <li>Paste the <strong>Android App Base URL</strong> above: <code style="color:#38bdf8;background:#020617;padding:2px 6px;border-radius:4px;">{api_url}</code></li>
                <li>Tap <strong>Save & Apply</strong>. You can now log in over 4G/5G or any Wi-Fi!</li>
              </ol>

              <!-- Infrastructure Specs Table -->
              <table width="100%" border="0" cellspacing="0" cellpadding="0" style="background:#020617;border:1px solid #1e293b;border-radius:12px;font-size:12px;margin:16px 0;">
                <tr style="border-bottom:1px solid #1e293b;">
                  <td style="padding:10px 14px;color:#64748b;font-weight:600;">MongoDB Atlas</td>
                  <td style="padding:10px 14px;color:#10b981;font-weight:700;text-align:right;">{ATLAS_STATUS} ({ATLAS_CLUSTER})</td>
                </tr>
                <tr style="border-bottom:1px solid #1e293b;">
                  <td style="padding:10px 14px;color:#64748b;font-weight:600;">Target Database</td>
                  <td style="padding:10px 14px;color:#e2e8f0;font-weight:700;text-align:right;">{ATLAS_DB_NAME}</td>
                </tr>
                <tr style="border-bottom:1px solid #1e293b;">
                  <td style="padding:10px 14px;color:#64748b;font-weight:600;">Local Wi-Fi LAN</td>
                  <td style="padding:10px 14px;color:#e2e8f0;font-weight:600;text-align:right;">http://{lan_ip}:{port}</td>
                </tr>
                <tr>
                  <td style="padding:10px 14px;color:#64748b;font-weight:600;">Dispatch Time</td>
                  <td style="padding:10px 14px;color:#94a3b8;text-align:right;">{timestamp_str}</td>
                </tr>
              </table>

              <p style="margin:24px 0 0 0;color:#64748b;font-size:12px;line-height:1.5;border-top:1px solid #1e293b;padding-top:16px;">
                🔒 This is an automated operational briefing generated by RosterX Server on host machine.
              </p>
            </td>
          </tr>

          <!-- Footer -->
          <tr>
            <td style="padding:18px 36px;background:#0b1120;border-top:1px solid #1e293b;text-align:center;color:#475569;font-size:11px;">
              © 2026 Project RosterX Operations • Automated Gateway Dispatch via Resend
            </td>
          </tr>
        </table>
      </td>
    </tr>
  </table>
</body>
</html>"""

    print(f"  [EMAIL NOTIFICATION] Dispatching live server gateway link to {to_email}...")
    res = send_resend_email(to_email, subject, html_content)
    if res.get("success"):
        print(f"  \033[92m[EMAIL SUCCESS]\033[0m Professional gateway briefing successfully sent to {to_email} (ID: {res.get('id')})")
    else:
        print(f"  \033[91m[EMAIL NOTICE]\033[0m Email delivery notice: {res.get('error')}")
    return res

def find_cloudflared_path():
    """Locate the cloudflared executable from PATH or common installation paths."""
    cmd = shutil.which("cloudflared")
    if cmd:
        return cmd

    candidates = [
        Path(os.environ.get("ProgramFiles(x86)", "C:/Program Files (x86)")) / "cloudflared" / "cloudflared.exe",
        Path(os.environ.get("ProgramFiles", "C:/Program Files")) / "cloudflared" / "cloudflared.exe",
        Path(os.environ.get("LOCALAPPDATA", "")) / "Microsoft" / "WinGet" / "Packages" / "Cloudflare.cloudflared" / "cloudflared.exe",
    ]
    for p in candidates:
        if p.exists():
            return str(p)
    return None

def publish_server_endpoints(public_url, lan_ip, port):
    """Publish live endpoints to files and Atlas for seamless mobile discovery across any network."""
    global ATLAS_DB

    # 1. Write to public_tunnel_url.txt in root dir
    try:
        url_file = ROOT_DIR / "public_tunnel_url.txt"
        with open(url_file, "w", encoding="utf-8") as f:
            f.write((public_url or "") + "\n")
    except Exception:
        pass

    # 2. Write to Android project assets (both local unified repo and external Android Studio folder)
    asset_targets = [
        ROOT_DIR / "android" / "app" / "src" / "main" / "assets",
        Path("C:/Users/hp/AndroidStudioProjects/RosterX/app/src/main/assets")
    ]
    for target in asset_targets:
        try:
            target.mkdir(parents=True, exist_ok=True)
            with open(target / "server_url.txt", "w", encoding="utf-8") as f:
                f.write((public_url or "") + "\n")
        except Exception:
            pass

    # 3. Synchronize with MongoDB Atlas system_config
    if ATLAS_DB is not None:
        try:
            ATLAS_DB.system_config.update_one(
                {"_id": "active_server"},
                {"$set": {
                    "public_url": public_url,
                    "api_url": f"{public_url}/api" if public_url else f"http://{lan_ip}:{port}/api",
                    "lan_url": f"http://{lan_ip}:{port}",
                    "lan_api_url": f"http://{lan_ip}:{port}/api",
                    "port": port,
                    "status": "online",
                    "updated_at": time.strftime("%Y-%m-%dT%H:%M:%SZ", time.gmtime())
                }},
                upsert=True
            )
        except Exception as e:
            print(f"  [SERVER INFO] Atlas endpoint sync note: {e}")

def start_cloudflared_tunnel(port=8000, timeout=30):
    """
    Launch cloudflared quick tunnel to forward public traffic to http://127.0.0.1:port.
    Returns (process, public_url).
    """
    global PUBLIC_TUNNEL_URL, TUNNEL_PROC, SERVER_PORT
    SERVER_PORT = port
    cf_bin = find_cloudflared_path()
    if not cf_bin:
        return None, None

    # Use 127.0.0.1 explicitly to prevent Windows IPv6 binding issues
    cmd = [cf_bin, "tunnel", "--url", f"http://127.0.0.1:{port}"]
    try:
        proc = subprocess.Popen(
            cmd,
            stdout=subprocess.PIPE,
            stderr=subprocess.STDOUT,
            text=True,
            bufsize=1
        )
    except Exception as e:
        print(f"  [CLOUDFLARE TUNNEL] Failed to launch cloudflared: {e}")
        return None, None

    public_url = None
    start_time = time.time()

    # Read output until the trycloudflare.com URL is printed
    while time.time() - start_time < timeout:
        line = proc.stdout.readline()
        if not line:
            break
        match = re.search(r"https://[a-zA-Z0-9-]+\.trycloudflare\.com", line)
        if match:
            public_url = match.group(0)
            break

    PUBLIC_TUNNEL_URL = public_url
    TUNNEL_PROC = proc

    # Spawn daemon thread to drain stdout so process doesn't block
    def drain_output():
        try:
            for _ in proc.stdout:
                pass
        except Exception:
            pass

    t = threading.Thread(target=drain_output, daemon=True)
    t.start()

    return proc, public_url

def get_lan_ip():
    """Retrieve the primary local area network (LAN) IPv4 address."""
    try:
        s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        s.settimeout(1.0)
        s.connect(("8.8.8.8", 80))
        ip = s.getsockname()[0]
        s.close()
        return ip
    except Exception:
        return "127.0.0.1"

def load_env_variables():
    """Parse .env file safely without third-party dependencies, overlaid with system environment."""
    env_data = {}
    env_file = ROOT_DIR / ".env"
    if not env_file.exists():
        env_file = WEB_DIR / ".env"

    if env_file.exists():
        try:
            with open(env_file, "r", encoding="utf-8") as f:
                for line in f:
                    line = line.strip()
                    if line and not line.startswith("#") and "=" in line:
                        k, v = line.split("=", 1)
                        env_data[k.strip()] = v.strip().strip("'\"")
        except Exception:
            pass

    # System environment variables (Render, Docker, Cloud) take precedence over .env file
    for k, v in os.environ.items():
        if v:
            env_data[k] = v.strip()

    return env_data

def mask_mongo_uri(uri):
    """Safely mask MongoDB credentials for console logging."""
    if not uri:
        return "Not configured"
    try:
        if "@" in uri:
            prefix, rest = uri.split("@", 1)
            scheme = prefix.split("://")[0]
            if ":" in prefix.replace(scheme + "://", ""):
                user = prefix.replace(scheme + "://", "").split(":")[0]
                return f"{scheme}://{user}:{'*' * 8}@{rest}"
        return uri[:15] + "..."
    except Exception:
        return "Configured (Protected)"

def mask_api_key(key):
    """Safely mask API key."""
    if not key:
        return "Not Configured"
    if len(key) <= 8:
        return "********"
    return f"{key[:6]}...{key[-4:]}"

class SecureRosterHandler(SimpleHTTPRequestHandler):
    """
    Secure HTTP handler combining:
      - Native MongoDB Atlas REST API bridge (/api/*)
      - Isolated static web serving for admin-web/
      - Strict blocking of .env, dotfiles, and sensitive configs
      - Full CORS & preflight OPTIONS support
    """

    def __init__(self, *args, **kwargs):
        super().__init__(*args, directory=str(WEB_DIR), **kwargs)

    def send_cors_headers(self):
        """Append CORS headers to allow cross-origin, mobile, and reverse-proxy access."""
        self.send_header("Access-Control-Allow-Origin", "*")
        self.send_header("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS, HEAD")
        self.send_header("Access-Control-Allow-Headers", "Content-Type, Authorization, X-Requested-With, Accept, Origin, User-Agent, Cache-Control, Pragma, X-Forwarded-For, X-Forwarded-Proto, CF-Connecting-IP")
        self.send_header("Access-Control-Expose-Headers", "*")

    def send_json_response(self, data, status_code=200):
        """Helper to serialize and transmit JSON payload with CORS and socket drop protection."""
        try:
            payload = json.dumps(data, default=str).encode("utf-8")
        except Exception as e:
            payload = json.dumps({"success": False, "message": f"Serialization error: {e}"}).encode("utf-8")
            status_code = 500

        try:
            self.send_response(status_code)
            self.send_header("Content-Type", "application/json; charset=utf-8")
            self.send_header("Content-Length", str(len(payload)))
            self.send_cors_headers()
            self.end_headers()
            self.wfile.write(payload)
        except (BrokenPipeError, ConnectionResetError, ConnectionAbortedError, OSError):
            # Client disconnected mid-transfer (common on mobile cellular networks)
            pass

    def do_OPTIONS(self):
        """Handle CORS preflight requests smoothly."""
        self.send_response(204)
        self.send_cors_headers()
        self.send_header("Access-Control-Max-Age", "86400")
        self.end_headers()

    def do_GET(self):
        # Normalize and inspect path
        clean_path = self.path.split("?")[0].strip()
        norm_path = clean_path.rstrip("/")

        # Support API requests with or without /api prefix
        if norm_path in ("/test-connection", "/engineers", "/server-info", "/logs", "/server-logs", "/constraints", "/notifications") or norm_path.startswith("/roster/") or norm_path.startswith("/roster-backups/"):
            norm_path = "/api" + norm_path

        # ==========================================
        # REST API Endpoints (/api/*)
        # ==========================================
        if norm_path.startswith("/api/"):
            self.handle_api_get(norm_path)
            return

        # ==========================================
        # Security Guards: Block .env and dotfiles
        # ==========================================
        blocked_keywords = [".env", ".git", ".gitignore", "web.config", ".htaccess", ".htpasswd"]
        path_lower = clean_path.lower()
        for blocked in blocked_keywords:
            if blocked in path_lower:
                self.send_error(403, "Access Denied: Requested resource is protected.")
                return

        super().do_GET()

    def handle_api_get(self, clean_path):
        """Router for GET /api/* requests."""
        lan_ip = get_lan_ip()

        # 0. GET /api/server-info (Full connectivity metadata for mobile discovery)
        if clean_path in ("/api/server-info", "/api/server-info/"):
            latency_ms = 0
            if ATLAS_CLIENT is not None:
                try:
                    t0 = time.perf_counter()
                    ATLAS_CLIENT.admin.command("ping")
                    latency_ms = max(1, round((time.perf_counter() - t0) * 1000))
                except Exception:
                    latency_ms = 0
            self.send_json_response({
                "success": True,
                "status": "online",
                "version": SERVER_VERSION,
                "public_url": PUBLIC_TUNNEL_URL,
                "api_url": f"{PUBLIC_TUNNEL_URL}/api" if PUBLIC_TUNNEL_URL else f"http://{lan_ip}:{SERVER_PORT}/api",
                "lan_ip": lan_ip,
                "lan_url": f"http://{lan_ip}:{SERVER_PORT}",
                "port": SERVER_PORT,
                "tunnel_active": bool(PUBLIC_TUNNEL_URL),
                "cluster": ATLAS_CLUSTER,
                "database": ATLAS_DB_NAME,
                "atlas_status": ATLAS_STATUS,
                "latency_ms": latency_ms,
                "timestamp": time.time()
            })
            return

        # 1. GET /api/test-connection
        if clean_path in ("/api/test-connection", "/api/test-connection/"):
            if ATLAS_CLIENT is not None:
                try:
                    t0 = time.perf_counter()
                    ATLAS_CLIENT.admin.command("ping")
                    latency_ms = max(1, round((time.perf_counter() - t0) * 1000))
                    self.send_json_response({
                        "success": True,
                        "version": SERVER_VERSION,
                        "message": f"Connected to {ATLAS_CLUSTER}",
                        "cluster": ATLAS_CLUSTER,
                        "database": ATLAS_DB_NAME,
                        "status": "connected",
                        "latency_ms": latency_ms,
                        "public_url": PUBLIC_TUNNEL_URL,
                        "api_url": f"{PUBLIC_TUNNEL_URL}/api" if PUBLIC_TUNNEL_URL else f"http://{lan_ip}:{SERVER_PORT}/api",
                        "lan_url": f"http://{lan_ip}:{SERVER_PORT}",
                        "logs": list(SERVER_LOGS)
                    })
                    return
                except Exception as e:
                    self.send_json_response({
                        "success": False,
                        "message": f"Atlas ping failed: {e}",
                        "cluster": ATLAS_CLUSTER,
                        "database": ATLAS_DB_NAME,
                        "status": "disconnected",
                        "latency_ms": 0,
                        "logs": list(SERVER_LOGS)
                    }, 503)
                    return
            else:
                self.send_json_response({
                    "success": False,
                    "message": f"Atlas not connected: {ATLAS_ERROR or 'Client not initialized'}",
                    "cluster": ATLAS_CLUSTER,
                    "database": ATLAS_DB_NAME,
                    "status": "disconnected",
                    "latency_ms": 0,
                    "logs": list(SERVER_LOGS)
                }, 503)
                return

        # 1.5 GET /api/logs or /api/server-logs (Live terminal log streaming for Android app)
        if clean_path in ("/api/logs", "/api/logs/", "/api/server-logs", "/api/server-logs/"):
            self.send_json_response({
                "success": True,
                "logs": list(SERVER_LOGS),
                "count": len(SERVER_LOGS),
                "cluster": ATLAS_CLUSTER,
                "database": ATLAS_DB_NAME,
                "timestamp": time.time()
            })
            return

        # 2. GET /api/roster/<monthId>
        if clean_path.startswith("/api/roster/"):
            month_id = clean_path[len("/api/roster/"):].strip("/")
            if not month_id:
                self.send_json_response({"success": False, "message": "Month ID is required"}, 400)
                return

            if ATLAS_DB is None:
                self.send_json_response({
                    "success": False,
                    "message": "Atlas database is not connected",
                    "document": None
                }, 503)
                return

            try:
                doc = ATLAS_DB.rosters.find_one({"_id": month_id})
                if doc:
                    self.send_json_response({
                        "success": True,
                        "document": doc,
                        "data": doc
                    })
                else:
                    self.send_json_response({
                        "success": False,
                        "message": f"Roster '{month_id}' not found in Atlas",
                        "document": None
                    }, 404)
            except Exception as e:
                self.send_json_response({
                    "success": False,
                    "message": f"Atlas query error: {e}",
                    "document": None
                }, 500)
            return

        # 2.5 GET /api/roster-backups/<monthId> (Project-wide recovery snapshots)
        if clean_path.startswith("/api/roster-backups/"):
            month_id = clean_path[len("/api/roster-backups/"):].strip("/")
            if not month_id:
                self.send_json_response({"success": False, "message": "Month ID is required"}, 400)
                return

            backups = []
            if ATLAS_DB is not None:
                try:
                    cursor = ATLAS_DB.roster_backups.find({"month_id": month_id}, {"_id": 0, "roster": 0}).sort("created_at_epoch", -1).limit(25)
                    backups = list(cursor)
                except Exception as e:
                    print(f"  [BACKUP QUERY ERROR] {e}")

            # Also check local disk backups folder as fallback
            try:
                backup_dir = ROOT_DIR / "backups"
                if backup_dir.exists():
                    for f in backup_dir.glob(f"backup_{month_id}_*.json"):
                        with open(f, "r", encoding="utf-8") as bf:
                            data = json.load(bf)
                            b_id = data.get("backup_id")
                            if not any(b.get("backup_id") == b_id for b in backups):
                                backups.append({
                                    "backup_id": b_id,
                                    "month_id": data.get("month_id", month_id),
                                    "timestamp": data.get("timestamp"),
                                    "created_at_epoch": data.get("created_at_epoch", 0),
                                    "published_by": data.get("published_by", "local_backup"),
                                    "total_engineers": data.get("total_engineers", 0)
                                })
            except Exception:
                pass

            backups.sort(key=lambda x: x.get("created_at_epoch", 0), reverse=True)
            self.send_json_response({
                "success": True,
                "month_id": month_id,
                "backups": backups,
                "count": len(backups)
            })
            return

        # 3. GET /api/engineers
        if clean_path in ("/api/engineers", "/api/engineers/"):
            docs = []
            if ATLAS_DB is not None:
                try:
                    docs = list(ATLAS_DB.engineers.find({}, {"_id": 0}))
                except Exception as e:
                    print(f"  [ATLAS] Engineers fetch error: {e}")
            if not docs:
                docs = DEFAULT_SERVER_ENGINEERS
            self.send_json_response({
                "success": True,
                "documents": docs,
                "count": len(docs)
            })
            return

        # 4. GET /api/constraints
        if clean_path in ("/api/constraints", "/api/constraints/"):
            default_constraints = {
                "nightHeadcount": 1,
                "fsMin": 1,
                "ssMin": 1,
                "daytimeOverlapMin": 3,
                "postNightRestDays": 2,
                "maxConsecutiveDays": 6,
                "nightBlockMin": 3,
                "nightBlockMax": 5,
                "swapFirstPolicy": True,
                "customDirectives": "Maintenance window on Nov 14: Ensure senior engineer on SS. Maintain balanced morning presence."
            }
            constraints = default_constraints
            if ATLAS_DB is not None:
                try:
                    stored = ATLAS_DB.operational_policies.find_one({"_id": "active_constraints"}, {"_id": 0})
                    if stored:
                        constraints = {**default_constraints, **stored}
                except Exception as e:
                    print(f"  [ATLAS] Constraints fetch error: {e}")
            self.send_json_response({
                "success": True,
                "constraints": constraints
            })
            return

        # 5. GET /api/notifications
        if clean_path in ("/api/notifications", "/api/notifications/"):
            notifs = list(SERVER_NOTIFICATIONS)
            if ATLAS_DB is not None:
                try:
                    db_notifs = list(ATLAS_DB.notifications.find({}, {"_id": 0}).sort("timestamp", -1).limit(50))
                    if db_notifs:
                        notifs = db_notifs
                except Exception as e:
                    print(f"  [ATLAS] Notifications fetch error: {e}")
            self.send_json_response({
                "success": True,
                "notifications": notifs,
                "count": len(notifs)
            })
            return

        # Catch-all for unknown /api/ GET
        self.send_json_response({
            "success": False,
            "message": f"Endpoint GET {clean_path} not found"
        }, 404)

    def do_POST(self):
        clean_path = self.path.split("?")[0].strip()
        norm_path = clean_path.rstrip("/")

        # Support API requests with or without /api prefix
        if norm_path in ("/roster", "/engineers", "/engineers/delete", "/constraints", "/auth/login", "/auth/send-otp", "/auth/verify-otp", "/roster-backups/restore", "/notifications/send") or norm_path.startswith("/roster-backups/"):
            norm_path = "/api" + norm_path

        if norm_path.startswith("/api/"):
            self.handle_api_post(norm_path)
            return

        self.send_error(405, "Method Not Allowed")

    def handle_api_post(self, clean_path):
        """Router for POST /api/* requests."""
        content_length = int(self.headers.get("Content-Length", 0))
        body_bytes = self.rfile.read(content_length) if content_length > 0 else b"{}"

        try:
            body = json.loads(body_bytes.decode("utf-8")) if body_bytes else {}
        except Exception as e:
            self.send_json_response({"success": False, "message": f"Malformed JSON: {e}"}, 400)
            return

        # 1. POST /api/roster
        if clean_path in ("/api/roster", "/api/roster/"):
            if ATLAS_DB is None:
                self.send_json_response({"success": False, "message": "Atlas database is not connected"}, 503)
                return

            doc_id = body.get("_id")
            if not doc_id:
                y = body.get("year")
                m = body.get("month")
                if y and m:
                    doc_id = f"{y}-{str(m).zfill(2)}"
                    body["_id"] = doc_id
                else:
                    self.send_json_response({"success": False, "message": "Missing '_id' in roster payload"}, 400)
                    return

            try:
                # Automatic recovery snapshot: If an existing roster exists for doc_id, backup before overwrite
                existing_roster = ATLAS_DB.rosters.find_one({"_id": doc_id})
                if existing_roster and (existing_roster.get("roster") or existing_roster.get("engineers")):
                    b_timestamp_epoch = time.time()
                    b_id = f"b_{doc_id}_{int(b_timestamp_epoch)}"
                    b_time_str = time.strftime("%Y-%m-%d %H:%M:%S UTC", time.gmtime(b_timestamp_epoch))
                    
                    snapshot_data = dict(existing_roster)
                    if "_id" in snapshot_data:
                        snapshot_data["original_id"] = str(snapshot_data.pop("_id"))
                    
                    backup_doc = {
                        "backup_id": b_id,
                        "month_id": doc_id,
                        "timestamp": b_time_str,
                        "created_at_epoch": b_timestamp_epoch,
                        "published_by": existing_roster.get("published_by") or body.get("published_by") or "Admin",
                        "total_engineers": len(existing_roster.get("roster", existing_roster.get("engineers", []))),
                        "roster": snapshot_data
                    }
                    try:
                        ATLAS_DB.roster_backups.insert_one(backup_doc)
                    except Exception as b_err:
                        print(f"  [BACKUP WARNING] Atlas backup insert failed: {b_err}")
                        
                    try:
                        backup_dir = ROOT_DIR / "backups"
                        backup_dir.mkdir(parents=True, exist_ok=True)
                        disk_file = backup_dir / f"backup_{doc_id}_{int(b_timestamp_epoch)}.json"
                        with open(disk_file, "w", encoding="utf-8") as f:
                            json.dump(backup_doc, f, default=str, indent=2)
                    except Exception as disk_err:
                        print(f"  [BACKUP WARNING] Disk backup failed: {disk_err}")

                body["updated_at"] = time.strftime("%Y-%m-%dT%H:%M:%SZ", time.gmtime())
                ATLAS_DB.rosters.update_one({"_id": doc_id}, {"$set": body}, upsert=True)
                self.send_json_response({
                    "success": True,
                    "message": f"Roster '{doc_id}' successfully saved and published to Atlas M0!",
                    "id": doc_id
                })
            except Exception as e:
                self.send_json_response({"success": False, "message": f"Failed to upsert roster to Atlas: {e}"}, 500)
            return

        # 1.5 POST /api/roster-backups/restore (Restore prior published roster)
        if clean_path in ("/api/roster-backups/restore", "/api/roster-backups/restore/"):
            if ATLAS_DB is None:
                self.send_json_response({"success": False, "message": "Atlas database is not connected"}, 503)
                return

            backup_id = body.get("backup_id")
            month_id = body.get("month_id")
            if not backup_id:
                self.send_json_response({"success": False, "message": "Missing 'backup_id' in restore payload"}, 400)
                return

            try:
                # Find the backup in Atlas DB
                backup_doc = ATLAS_DB.roster_backups.find_one({"backup_id": backup_id})
                
                # Fallback to local disk file if not in DB
                if not backup_doc:
                    backup_dir = ROOT_DIR / "backups"
                    if backup_dir.exists():
                        for f in backup_dir.glob(f"*{backup_id}*.json"):
                            with open(f, "r", encoding="utf-8") as bf:
                                backup_doc = json.load(bf)
                                break

                if not backup_doc or "roster" not in backup_doc:
                    self.send_json_response({"success": False, "message": f"Backup '{backup_id}' not found or corrupted"}, 404)
                    return

                restored_roster = dict(backup_doc["roster"])
                target_month = month_id or backup_doc.get("month_id") or restored_roster.get("original_id") or restored_roster.get("_id")
                if not target_month:
                    self.send_json_response({"success": False, "message": "Unable to determine month ID for restore"}, 400)
                    return

                restored_roster["_id"] = target_month
                restored_roster["restored_from"] = backup_id
                restored_roster["restored_at"] = time.strftime("%Y-%m-%dT%H:%M:%SZ", time.gmtime())
                restored_roster["updated_at"] = time.strftime("%Y-%m-%dT%H:%M:%SZ", time.gmtime())

                ATLAS_DB.rosters.update_one({"_id": target_month}, {"$set": restored_roster}, upsert=True)
                self.send_json_response({
                    "success": True,
                    "message": f"Roster '{target_month}' successfully restored from backup '{backup_id}'!",
                    "month_id": target_month,
                    "document": restored_roster
                })
            except Exception as e:
                self.send_json_response({"success": False, "message": f"Restore failed: {e}"}, 500)
            return

        # 1.8 POST /api/constraints (Sync operational policy to Atlas)
        if clean_path in ("/api/constraints", "/api/constraints/"):
            if ATLAS_DB is None:
                self.send_json_response({"success": False, "message": "Atlas database is not connected"}, 503)
                return
            try:
                constraints_data = body.get("constraints", body)
                if isinstance(constraints_data, dict):
                    to_save = dict(constraints_data)
                    if "_id" in to_save:
                        del to_save["_id"]
                    to_save["updated_at"] = time.strftime("%Y-%m-%dT%H:%M:%SZ", time.gmtime())
                    ATLAS_DB.operational_policies.update_one(
                        {"_id": "active_constraints"},
                        {"$set": to_save},
                        upsert=True
                    )
                    self.send_json_response({
                        "success": True,
                        "message": "Operational policy constraints updated and synced to Atlas M0!",
                        "constraints": to_save
                    })
                else:
                    self.send_json_response({"success": False, "message": "Invalid constraints payload format"}, 400)
            except Exception as e:
                self.send_json_response({"success": False, "message": f"Failed to save constraints: {e}"}, 500)
            return

        # 2. POST /api/engineers
        if clean_path in ("/api/engineers", "/api/engineers/"):
            if ATLAS_DB is None:
                self.send_json_response({"success": False, "message": "Atlas database is not connected"}, 503)
                return

            engineers = body if isinstance(body, list) else body.get("engineers", [])
            if not isinstance(engineers, list):
                self.send_json_response({"success": False, "message": "Expected list of engineers or { 'engineers': [...] }"}, 400)
                return

            try:
                saved_count = 0
                for eng in engineers:
                    eng_id = str(eng.get("id") or eng.get("engineer_id") or "").strip()
                    eng_name = str(eng.get("name") or "").strip()
                    eng_user = str(eng.get("username") or "").strip()

                    # Normalize status to Title Case for full admin-web and android parity
                    raw_status = str(eng.get("status") or "").strip().lower()
                    if raw_status in ("active", "notice_period", "onboarding"):
                        eng["status"] = "Active"
                    elif raw_status in ("resigned", "retired"):
                        eng["status"] = "Resigned"

                    or_conditions = []
                    if eng_id:
                        or_conditions.extend([{"id": eng_id}, {"engineer_id": eng_id}])
                    if eng_user:
                        or_conditions.extend([
                            {"username": eng_user},
                            {"username": {"$regex": f"^{re.escape(eng_user)}$", "$options": "i"}}
                        ])
                    if eng_name:
                        or_conditions.extend([
                            {"name": eng_name},
                            {"name": {"$regex": f"^{re.escape(eng_name)}$", "$options": "i"}}
                        ])

                    if or_conditions:
                        query = {"$or": or_conditions}
                        ATLAS_DB.engineers.update_one(query, {"$set": eng}, upsert=True)
                        saved_count += 1
                self.send_json_response({
                    "success": True,
                    "message": f"Successfully synchronized {saved_count} engineers to MongoDB Atlas!",
                    "count": saved_count
                })
            except Exception as e:
                self.send_json_response({"success": False, "message": f"Failed to sync engineers: {e}"}, 500)
            return

        # 2.5 POST /api/engineers/delete
        if clean_path in ("/api/engineers/delete", "/api/engineers/delete/"):
            if ATLAS_DB is None:
                self.send_json_response({"success": False, "message": "Atlas database is not connected"}, 503)
                return

            eng_id = str(body.get("id", "")).strip()
            eng_name = str(body.get("name", "")).strip()
            eng_username = str(body.get("username", "")).strip()

            if not eng_id and not eng_name and not eng_username:
                self.send_json_response({"success": False, "message": "Engineer id, name, or username is required for deletion"}, 400)
                return

            try:
                queries = []
                if eng_id:
                    queries.extend([{"id": eng_id}, {"engineer_id": eng_id}, {"_id": eng_id}])
                if eng_name:
                    queries.extend([{"name": eng_name}, {"name": {"$regex": f"^{re.escape(eng_name)}$", "$options": "i"}}])
                if eng_username:
                    queries.extend([{"username": eng_username}, {"username": {"$regex": f"^{re.escape(eng_username)}$", "$options": "i"}}])

                delete_query = {"$or": queries}
                result = ATLAS_DB.engineers.delete_many(delete_query)

                # Also remove from DEFAULT_SERVER_ENGINEERS memory list if present
                global DEFAULT_SERVER_ENGINEERS
                DEFAULT_SERVER_ENGINEERS = [
                    e for e in DEFAULT_SERVER_ENGINEERS 
                    if e.get("id") != eng_id and e.get("name", "").lower() != eng_name.lower() and e.get("username", "").lower() != eng_username.lower()
                ]

                self.send_json_response({
                    "success": True,
                    "message": f"Successfully deleted engineer ({eng_name or eng_id}) from database.",
                    "deleted_count": result.deleted_count
                })
            except Exception as e:
                self.send_json_response({"success": False, "message": f"Failed to delete engineer: {e}"}, 500)
            return

        # 3. POST /api/auth/send-otp
        if clean_path in ("/api/auth/send-otp", "/api/auth/send-otp/"):
            identifier = str(body.get("identifier", "")).strip().lower()[:100]
            if not identifier:
                self.send_json_response({"success": False, "message": "Username or email is required"}, 400)
                return

            # Rate limit check (30-second cooldown per identifier)
            now = time.time()
            last_req = OTP_RATE_LIMIT.get(identifier, 0)
            if now - last_req < 30:
                wait_secs = int(30 - (now - last_req))
                self.send_json_response({
                    "success": False,
                    "message": f"Rate limit reached. Please wait {wait_secs} seconds before requesting a new OTP."
                }, 429)
                return
            OTP_RATE_LIMIT[identifier] = now

            import random
            otp_code = f"{random.randint(100000, 999999)}"
            recipient_email = str(body.get("email", "")).strip()[:150]
            recipient_name = str(body.get("name", "")).strip()[:100] or identifier.upper()

            # If recipient email not supplied, look up in Atlas DB
            if not recipient_email and ATLAS_DB is not None:
                try:
                    eng = ATLAS_DB.engineers.find_one({
                        "$or": [
                            {"username": {"$regex": f"^{re.escape(identifier)}$", "$options": "i"}},
                            {"name": {"$regex": f"^{re.escape(identifier)}$", "$options": "i"}},
                            {"id": identifier},
                            {"email": identifier},
                            {"email": {"$regex": f"^{re.escape(identifier)}@", "$options": "i"}}
                        ]
                    })
                    if eng:
                        recipient_email = eng.get("email", "")
                        recipient_name = eng.get("name", "") or eng.get("full_name", "") or recipient_name
                except Exception as e:
                    print(f"  [OTP] Engineer lookup warning: {e}")

            # Fallback to DEFAULT_SERVER_ENGINEERS if not found in DB
            if not recipient_email:
                for tm in DEFAULT_SERVER_ENGINEERS:
                    u_match = tm.get("username", "").lower()
                    n_match = tm.get("name", "").lower()
                    e_match = tm.get("email", "").lower()
                    p_match = e_match.split("@")[0] if "@" in e_match else ""
                    if identifier in (u_match, n_match, e_match, p_match):
                        recipient_email = tm.get("email", "")
                        recipient_name = tm.get("name", "")
                        break

            if not recipient_email and "@" in identifier:
                recipient_email = identifier

            if not recipient_email:
                self.send_json_response({
                    "success": False,
                    "message": f"No registered email address found for '{identifier}'. Please check your username or contact an administrator."
                }, 404)
                return

            # Store active OTP in server memory with 10-minute expiry and attempt counter
            expires_at = time.time() + 600
            ACTIVE_OTPS[identifier] = {
                "code": otp_code,
                "expiresAt": expires_at,
                "email": recipient_email,
                "name": recipient_name,
                "attempts": 0
            }
            if recipient_email:
                ACTIVE_OTPS[recipient_email.lower()] = ACTIVE_OTPS[identifier]

            # Build HTML transactional email
            html_email = f"""<!DOCTYPE html>
<html>
<head><meta charset="utf-8"></head>
<body style="margin:0;padding:0;background-color:#0b1120;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,sans-serif;color:#f1f5f9;">
  <table width="100%" border="0" cellspacing="0" cellpadding="0" style="background-color:#0b1120;padding:40px 15px;">
    <tr>
      <td align="center">
        <table width="100%" style="max-width:520px;background:#0f172a;border:1px solid #1e293b;border-radius:16px;overflow:hidden;box-shadow:0 10px 30px rgba(0,0,0,0.5);">
          <tr>
            <td style="padding:28px 32px;background:linear-gradient(135deg,#0284c7 0%,#2563eb 50%,#4f46e5 100%);text-align:center;">
              <h1 style="margin:0;color:#ffffff;font-size:22px;font-weight:800;letter-spacing:1px;">PROJECT ROSTERX</h1>
              <p style="margin:6px 0 0 0;color:#e0f2fe;font-size:13px;font-weight:500;">Operations & Account Security</p>
            </td>
          </tr>
          <tr>
            <td style="padding:32px;">
              <h2 style="margin:0 0 12px 0;color:#ffffff;font-size:18px;font-weight:700;">Password Reset Verification</h2>
              <p style="margin:0 0 20px 0;color:#94a3b8;font-size:14px;line-height:1.6;">
                Hello <strong style="color:#38bdf8;">{recipient_name}</strong>,<br>
                A password reset request was initiated for your account (<strong>{identifier}</strong>). Use the 6-digit verification code below to verify your identity:
              </p>
              <div style="background:#020617;border:1.5px solid #0284c7;border-radius:12px;padding:20px;text-align:center;margin:24px 0;">
                <span style="font-family:'Courier New',Courier,monospace;font-size:36px;font-weight:800;letter-spacing:10px;color:#38bdf8;">{otp_code}</span>
              </div>
              <p style="margin:0 0 16px 0;color:#94a3b8;font-size:13px;line-height:1.5;">
                ⏱️ This verification code is valid for <strong>10 minutes</strong>.
              </p>
              <p style="margin:0;color:#64748b;font-size:12px;line-height:1.5;border-top:1px solid #1e293b;padding-top:16px;">
                If you did not request this verification code, please ignore this email. Your password will remain unchanged.
              </p>
            </td>
          </tr>
          <tr>
            <td style="padding:16px 32px;background:#0b1120;border-top:1px solid #1e293b;text-align:center;color:#475569;font-size:11px;">
              © 2026 Project RosterX Operations · Automated Email Dispatch
            </td>
          </tr>
        </table>
      </td>
    </tr>
  </table>
</body>
</html>"""

            subject = f"🔐 {otp_code} is your RosterX verification code"
            print(f"  [OTP DISPATCH] Sending real email to {recipient_email} via Resend...")
            res_email = send_resend_email(recipient_email, subject, html_email)

            if res_email.get("success"):
                delivered = res_email.get("delivered_to", recipient_email)
                print(f"  [OTP DISPATCH] Email successfully sent! (Delivered to: {delivered})")
                self.send_json_response({
                    "success": True,
                    "message": f"Real OTP code successfully sent to {delivered}",
                    "delivered_to": delivered,
                    "notice": res_email.get("notice"),
                    "user": {
                        "username": identifier,
                        "name": recipient_name,
                        "email": delivered
                    }
                })
            else:
                err = res_email.get("error", "Unknown error")
                print(f"  [OTP DISPATCH] Email delivery failed: {err}")
                self.send_json_response({
                    "success": False,
                    "message": f"Email dispatch failed: {err}"
                }, 500)
            return

        # 4. POST /api/auth/verify-otp
        if clean_path in ("/api/auth/verify-otp", "/api/auth/verify-otp/"):
            identifier = str(body.get("identifier", "")).strip().lower()[:100]
            otp = str(body.get("otp", "")).strip()[:10]
            new_pass = str(body.get("newPassword", "")).strip()[:100]

            record = ACTIVE_OTPS.get(identifier)
            if not record and "@" in identifier:
                record = ACTIVE_OTPS.get(identifier.lower())

            if not record:
                self.send_json_response({"success": False, "message": "No active OTP request found or code expired."}, 400)
                return

            if time.time() > record["expiresAt"]:
                ACTIVE_OTPS.pop(identifier, None)
                self.send_json_response({"success": False, "message": "Verification code has expired (10-minute limit)."}, 400)
                return

            # Brute force protection: maximum 5 failed attempts per OTP
            attempts = record.get("attempts", 0) + 1
            record["attempts"] = attempts
            if attempts > 5:
                ACTIVE_OTPS.pop(identifier, None)
                self.send_json_response({
                    "success": False,
                    "message": "Too many failed attempts. This OTP has been invalidated for security. Please request a new code."
                }, 429)
                return

            if record["code"] != otp:
                remaining = 5 - attempts
                self.send_json_response({
                    "success": False, 
                    "message": f"Invalid verification code. {remaining} attempt(s) remaining."
                }, 400)
                return

            # Code is valid! Remove used OTP
            ACTIVE_OTPS.pop(identifier, None)
            if record.get("email"):
                ACTIVE_OTPS.pop(record["email"].lower(), None)

            # Update password in Atlas DB if user exists
            if new_pass and ATLAS_DB is not None:
                try:
                    ATLAS_DB.engineers.update_one(
                        {"$or": [
                            {"username": {"$regex": f"^{re.escape(identifier)}$", "$options": "i"}},
                            {"name": {"$regex": f"^{re.escape(identifier)}$", "$options": "i"}},
                            {"id": identifier},
                            {"email": identifier},
                            {"email": {"$regex": f"^{re.escape(identifier)}@", "$options": "i"}}
                        ]},
                        {"$set": {"password": new_pass, "updated_at": time.strftime("%Y-%m-%dT%H:%M:%SZ", time.gmtime())}}
                    )
                except Exception as e:
                    print(f"  [OTP] Password update in Atlas DB error: {e}")

            self.send_json_response({
                "success": True,
                "message": "Password reset verified and updated successfully!"
            })
            return

        # 5. POST /api/auth/login
        if clean_path in ("/api/auth/login", "/api/auth/login/"):
            identifier = str(body.get("identifier", "")).strip().lower()[:100]
            password = str(body.get("password", "")).strip()[:100]

            if not identifier or not password:
                self.send_json_response({"success": False, "message": "Identifier and password required"}, 400)
                return

            # Check master admin
            if identifier in ("admin", "admin@rosterx.dev") and password in ("admin123", "password123"):
                self.send_json_response({
                    "success": True,
                    "user": {
                        "id": "admin_01",
                        "username": "admin",
                        "name": "System Administrator",
                        "email": "admin@rosterx.dev",
                        "role": "admin"
                    }
                })
                return

            matched_eng = None
            if ATLAS_DB is not None:
                try:
                    matched_eng = ATLAS_DB.engineers.find_one({
                        "$or": [
                            {"username": {"$regex": f"^{re.escape(identifier)}$", "$options": "i"}},
                            {"name": {"$regex": f"^{re.escape(identifier)}$", "$options": "i"}},
                            {"id": identifier},
                            {"email": identifier},
                            {"email": {"$regex": f"^{re.escape(identifier)}@", "$options": "i"}}
                        ]
                    })
                except Exception as e:
                    print(f"  [LOGIN ERROR] DB query error: {e}")

            if not matched_eng:
                for tm in DEFAULT_SERVER_ENGINEERS:
                    u_match = tm.get("username", "").lower()
                    n_match = tm.get("name", "").lower()
                    e_match = tm.get("email", "").lower()
                    p_match = e_match.split("@")[0] if "@" in e_match else ""
                    if identifier in (u_match, n_match, e_match, p_match):
                        matched_eng = tm
                        break

            if matched_eng:
                stored_pass = matched_eng.get("password", "user123")
                if stored_pass == password:
                    u_name = matched_eng.get("username") or (matched_eng.get("email", "").split("@")[0] if "@" in matched_eng.get("email", "") else matched_eng.get("name", "").lower())
                    self.send_json_response({
                        "success": True,
                        "user": {
                            "id": matched_eng.get("id", f"eng_{u_name}"),
                            "username": u_name,
                            "name": matched_eng.get("name", identifier.upper()),
                            "full_name": matched_eng.get("full_name") or matched_eng.get("name"),
                            "email": matched_eng.get("email", ""),
                            "phone": matched_eng.get("phone", "+919811001100"),
                            "role": matched_eng.get("role", "engineer")
                        }
                    })
                    return

            self.send_json_response({"success": False, "message": "Invalid credentials."}, 401)
            return

        # 6. POST /api/notifications/send
        if clean_path in ("/api/notifications/send", "/api/notifications/send/"):
            title = str(body.get("title", "Broadcast Notice")).strip()
            raw_message = str(body.get("message", "")).strip()
            target = body.get("target", "everyone")
            priority = str(body.get("priority", "info")).lower()
            sender = str(body.get("sender", "System Administrator")).strip()
            send_email = bool(body.get("send_email", False))
            send_in_app = bool(body.get("send_in_app", True))

            if not raw_message:
                self.send_json_response({"success": False, "message": "Message content cannot be blank."}, 400)
                return

            # Fetch active engineers list
            engineers = []
            if ATLAS_DB is not None:
                try:
                    engineers = list(ATLAS_DB.engineers.find({}, {"_id": 0}))
                except Exception as e:
                    print(f"  [NOTIF] DB engineers lookup error: {e}")
            if not engineers:
                engineers = list(DEFAULT_SERVER_ENGINEERS)

            # Determine target recipients
            is_everyone = target in ("everyone", "all", "*")
            recipients = []
            if is_everyone:
                recipients = [e for e in engineers if e.get("status") != "Resigned"]
            elif isinstance(target, list):
                target_norm = [str(t).strip().lower() for t in target]
                recipients = [e for e in engineers if e.get("id", "").lower() in target_norm or e.get("username", "").lower() in target_norm or e.get("name", "").lower() in target_norm]
            elif isinstance(target, str):
                target_norm = target.strip().lower()
                recipients = [e for e in engineers if e.get("id", "").lower() == target_norm or e.get("username", "").lower() == target_norm or e.get("name", "").lower() == target_norm]

            if not recipients:
                recipients = engineers[:1]

            # Personalize messages
            sample_personalized = ""
            emails_sent = 0
            for eng in recipients:
                eng_name = eng.get("full_name") or eng.get("name") or "Engineer"
                eng_username = eng.get("username") or eng_name.lower().replace(" ", "")
                # Personalization substitutions
                personalized_msg = raw_message
                personalized_msg = re.sub(r'\{\s*(?:Their\s+name|their\s+name|name)\s*\}', eng_name, personalized_msg, flags=re.IGNORECASE)
                personalized_msg = re.sub(r'\{\s*username\s*\}', eng_username, personalized_msg, flags=re.IGNORECASE)
                personalized_msg = re.sub(r'@\s*\{\s*username\s*\}', f"@{eng_username}", personalized_msg, flags=re.IGNORECASE)

                if not sample_personalized:
                    sample_personalized = personalized_msg

                if send_email and eng.get("email"):
                    to_email = eng.get("email")
                    priority_colors = {
                        "urgent": "#ef4444",
                        "shift_update": "#f59e0b",
                        "broadcast": "#8b5cf6",
                        "info": "#3b82f6"
                    }
                    p_color = priority_colors.get(priority, "#3b82f6")
                    html_email = f"""<!DOCTYPE html>
<html>
<body style="margin:0;padding:0;background-color:#0b1120;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,sans-serif;color:#f1f5f9;">
  <div style="max-width:560px;margin:30px auto;background:#0f172a;border:1px solid #1e293b;border-radius:16px;padding:30px;">
    <div style="display:inline-block;padding:4px 10px;background:{p_color}22;border:1px solid {p_color};border-radius:12px;color:{p_color};font-size:11px;font-weight:700;text-transform:uppercase;margin-bottom:12px;">
      {priority.upper()}
    </div>
    <h2 style="margin:0 0 16px 0;color:#ffffff;font-size:20px;font-weight:800;">{title}</h2>
    <div style="background:#020617;border:1px solid #334155;border-radius:10px;padding:18px;margin-bottom:20px;color:#e2e8f0;font-size:14px;line-height:1.6;white-space:pre-wrap;">
{personalized_msg}
    </div>
    <div style="font-size:12px;color:#64748b;border-top:1px solid #1e293b;padding-top:14px;">
      Sent by <strong>{sender}</strong> via RosterX Operations Broadcast.
    </div>
  </div>
</body>
</html>"""
                    try:
                        send_resend_email(to_email, f"📢 [{priority.upper()}] {title}", html_email)
                        emails_sent += 1
                    except Exception as err:
                        print(f"  [NOTIF EMAIL ERROR] {err}")

            notif_id = f"notif_{int(time.time()*1000)}"
            notif_doc = {
                "id": notif_id,
                "title": title,
                "raw_message": raw_message,
                "target": target,
                "target_type": "everyone" if is_everyone else "specific",
                "recipients_count": len(recipients),
                "recipients": [r.get("name") for r in recipients],
                "priority": priority,
                "sender": sender,
                "send_email": send_email,
                "send_in_app": send_in_app,
                "emails_dispatched": emails_sent,
                "sample_personalized": sample_personalized,
                "created_at": time.strftime("%Y-%m-%d %H:%M:%S UTC", time.gmtime()),
                "timestamp": time.time()
            }

            if send_in_app:
                SERVER_NOTIFICATIONS.appendleft(notif_doc)

                if ATLAS_DB is not None:
                    try:
                        ATLAS_DB.notifications.insert_one({**notif_doc, "_id": notif_id})
                    except Exception as e:
                        print(f"  [NOTIF DB SAVE ERROR] {e}")

            self.send_json_response({
                "success": True,
                "message": f"Broadcast successfully sent to {len(recipients)} engineers.",
                "recipients_count": len(recipients),
                "emails_dispatched": emails_sent,
                "notification": notif_doc
            })
            return

        # Catch-all for unknown /api/ POST
        self.send_json_response({
            "success": False,
            "message": f"Endpoint POST {clean_path} not found"
        }, 404)

    def end_headers(self):
        # Prevent browser caching of JS/CSS during development
        self.send_header("Cache-Control", "no-cache, no-store, must-revalidate")
        self.send_header("Pragma", "no-cache")
        self.send_header("Expires", "0")
        super().end_headers()

    def list_directory(self, path):
        """Block directory listing to prevent information leakage."""
        self.send_error(403, "Directory listing is disabled for security.")
        return None

    def log_message(self, format, *args):
        # Format HTTP requests cleanly
        status = args[1] if len(args) > 1 else ""
        method_path = args[0] if len(args) > 0 else ""
        if status in ("200", "204", "304"):
            color = "\033[92m" # Green
        elif status in ("403", "404"):
            color = "\033[91m" # Red
        else:
            color = "\033[93m" # Yellow
        reset = "\033[0m"
        print(f" {color}[HTTP {status}]{reset} {method_path}")

        # Store in dynamic server log ring buffer for Android Terminal Log Viewer
        # Avoid filling log with repeated polling /api/logs calls
        if "/api/logs" not in method_path:
            timestamp = time.strftime("%H:%M:%S")
            SERVER_LOGS.append(f"[{timestamp}] [HTTP {status}] {method_path}")

def run_server(port=8000, open_browser=False, enable_tunnel=False, alert_email=None, send_email=True):
    if not WEB_DIR.exists():
        print(f"[ERROR] 'admin-web' directory not found at {WEB_DIR}")
        sys.exit(1)

    lan_ip = get_lan_ip()
    env = load_env_variables()

    # Initialize PyMongo Atlas Bridge
    init_atlas_connection(env)

    # Initial log entries
    SERVER_LOGS.append(f"[{time.strftime('%H:%M:%S')}] [SYSTEM] RosterX Server listening on port {port}")
    SERVER_LOGS.append(f"[{time.strftime('%H:%M:%S')}] [ATLAS DB] Status: {ATLAS_STATUS} ({ATLAS_CLUSTER}/{ATLAS_DB_NAME})")

    global RESEND_API_KEY, RESEND_FROM, DEFAULT_RESEND_API_KEY, DEFAULT_ALERT_EMAIL
    RESEND_API_KEY = env.get("RESEND_API_KEY", "").strip() or DEFAULT_RESEND_API_KEY
    RESEND_FROM = env.get("RESEND_FROM", "RosterX Operations <onboarding@resend.dev>").strip()

    mongo_uri = env.get("MONGODB_URI", "")
    cluster_name = env.get("ATLAS_CLUSTER", "Cluster0")
    db_name = env.get("ATLAS_DATABASE", "rosterx")
    gemini_key = env.get("GEMINI_API_KEY", "")
    gemini_model = env.get("GEMINI_MODEL", "gemini-1.5-flash")

    # Auto-detect Render Cloud Environment
    is_render = bool(os.environ.get("RENDER")) or bool(os.environ.get("RENDER_EXTERNAL_URL"))
    render_external_url = os.environ.get("RENDER_EXTERNAL_URL")
    if is_render and render_external_url:
        public_url = render_external_url.rstrip("/")
        PUBLIC_TUNNEL_URL = public_url
        print(f"\n  [RENDER CLOUD] Detected Render production host: {public_url}")

    # Start Cloudflare Tunnel if requested (disabled on cloud platforms like Render)
    tunnel_proc = None
    if enable_tunnel and not is_render:
        print("\n  [CLOUDFLARE] Initializing secure quick tunnel to Cloudflare Edge...")
        tunnel_proc, public_url = start_cloudflared_tunnel(port=port)
        if public_url:
            SERVER_LOGS.append(f"[{time.strftime('%H:%M:%S')}] [GATEWAY] Public Tunnel active: {public_url}")
    elif is_render and public_url:
        SERVER_LOGS.append(f"[{time.strftime('%H:%M:%S')}] [RENDER CLOUD] Live Service URL: {public_url}")

    # Publish active endpoints to local files, Android assets, and Atlas
    publish_server_endpoints(public_url, lan_ip, port)

    # Dispatch professional email notification with live gateway link to administrator
    alert_target = alert_email or env.get("ALERT_EMAIL", DEFAULT_ALERT_EMAIL).strip() or DEFAULT_ALERT_EMAIL
    env_send_email = env.get("SEND_STARTUP_EMAIL", "true").strip().lower() in ("true", "1", "yes")
    should_send_email = send_email and env_send_email and not is_render

    if should_send_email and public_url:
        t_email = threading.Thread(
            target=send_startup_link_email,
            args=(public_url, lan_ip, port, alert_target),
            daemon=True
        )
        t_email.start()

    # Banner UI
    border = "=" * 68
    sub_border = "-" * 68
    print("\n" + border)
    print("  🚀  ROSTERX — SECURE ADMIN SERVER & NATIVE ATLAS BRIDGE")
    print(border)
    print("  [MOBILE APP & INTERNET ACCESS]")
    if public_url:
        print(f"   ► 🌐 Worldwide Public URL   : \033[93m\033[1m{public_url}\033[0m")
        print(f"   ► 📱 Android App Base URL   : \033[92m\033[1m{public_url}/api/\033[0m  (Works on 4G/5G/any WiFi)")
    elif enable_tunnel:
        print("   ► 🌐 Worldwide Public URL   : \033[91mTunnel failed to start (check internet connection)\033[0m")
    else:
        print("   ► 🌐 Worldwide Public URL   : \033[90mDisabled (run with --tunnel or set ENABLE_TUNNEL=true)\033[0m")
    print(f"   ► 🏠 Local Wi-Fi (LAN)      : \033[96mhttp://{lan_ip}:{port}\033[0m  (Same Wi-Fi only)")
    print(f"   ► 📱 Android LAN Base URL   : \033[96mhttp://{lan_ip}:{port}/api/\033[0m")
    print(f"   ► 💻 Local Web App          : \033[96mhttp://localhost:{port}\033[0m")
    print(f"   ► 🤖 Android Emulator       : \033[96mhttp://10.0.2.2:{port}/api/\033[0m")
    print(sub_border)
    print("  [AUTOMATED NOTIFICATIONS]")
    if should_send_email and public_url:
        print(f"   ► ✉️ Startup Alert Mail    : \033[92mDispatched to {alert_target}\033[0m (Via Resend)")
    elif not public_url:
        print(f"   ► ✉️ Startup Alert Mail    : \033[90mSkipped (no public tunnel URL active)\033[0m")
    else:
        print(f"   ► ✉️ Startup Alert Mail    : \033[90mDisabled (--no-email)\033[0m")
    print(sub_border)
    print("  [DATABASE & BACKEND SPECIFICATIONS]")
    print(f"   ► Engine Type   : MongoDB Atlas M0 (Native Python Driver)")
    print(f"   ► Cluster Name  : {cluster_name}")
    print(f"   ► Target DB     : {db_name}")
    print(f"   ► Connection URI: {mask_mongo_uri(mongo_uri)}")

    if ATLAS_STATUS == "CONNECTED (Ready)":
        status_color = "\033[92m" # Green
    else:
        status_color = "\033[91m" # Red
    print(f"   ► Atlas Status  : {status_color}{ATLAS_STATUS}\033[0m")
    if ATLAS_ERROR and ATLAS_STATUS != "CONNECTED (Ready)":
        print(f"   ► Reason        : \033[91m{ATLAS_ERROR}\033[0m")

    print(f"   ► REST Routes   : /api/test-connection, /api/server-info, /api/roster/<id>, /api/engineers")
    print(sub_border)
    print("  [AI ROSTER ENGINE]")
    print(f"   ► Model Target  : {gemini_model}")
    print(f"   ► Gemini API Key: {mask_api_key(gemini_key)}")
    print(sub_border)
    print("  [SECURITY & ENVIRONMENT GUARDS]")
    print(f"   ► Serving Root  : {WEB_DIR}")
    print(f"   ► Directory View: \033[92mDISABLED\033[0m (Prevent directory disclosure)")
    print(f"   ► .env Guard    : \033[92mBLOCKED (HTTP 403 Forbidden)\033[0m")
    print(f"   ► CORS Preflight: \033[92mENABLED (Full OPTIONS Support)\033[0m")
    print(f"   ► Concurrency   : \033[92mMulti-threaded (ThreadingHTTPServer)\033[0m")
    print(border)
    print("  Status: Server is actively listening on all interfaces (0.0.0.0). Press Ctrl+C to stop.\n")

    server_address = ("0.0.0.0", port)
    try:
        httpd = ThreadingHTTPServer(server_address, SecureRosterHandler)
        httpd.daemon_threads = True
    except OSError as e:
        print(f"\n[ERROR] Port {port} is already in use!")
        print("Please terminate the existing server process or specify another port:")
        print(f"  python server.py --port {port + 1}\n")
        if tunnel_proc:
            tunnel_proc.terminate()
        sys.exit(1)

    if open_browser:
        target_open = public_url if public_url else f"http://localhost:{port}"
        webbrowser.open(target_open)

    try:
        httpd.serve_forever()
    except KeyboardInterrupt:
        print("\n\n[INFO] Gracefully shutting down RosterX server...")
        if tunnel_proc:
            print("[INFO] Terminating Cloudflare Tunnel...")
            try:
                tunnel_proc.terminate()
                tunnel_proc.wait(timeout=2)
            except Exception:
                pass
        httpd.server_close()
        print("[INFO] Goodbye!\n")

if __name__ == "__main__":
    env = load_env_variables()
    default_port = int(env.get("PORT", os.environ.get("PORT", 8000)))
    is_render = bool(os.environ.get("RENDER")) or bool(os.environ.get("RENDER_EXTERNAL_URL"))

    parser = argparse.ArgumentParser(description="Run RosterX Secure Server & Atlas Bridge")
    parser.add_argument("--port", type=int, default=default_port, help=f"Port to bind (default: {default_port})")
    parser.add_argument("--open", action="store_true", help="Automatically open browser on launch")
    parser.add_argument("--tunnel", "-t", action="store_true", default=None, help="Force launch Cloudflare Tunnel")
    parser.add_argument("--no-tunnel", action="store_true", help="Disable Cloudflare Tunnel (local network only)")
    parser.add_argument("--email", type=str, default=None, help="Email address to receive live gateway link (default: kkshefeeq005@gmail.com)")
    parser.add_argument("--no-email", action="store_true", help="Disable sending startup gateway link email")
    args = parser.parse_args()

    env_tunnel_val = env.get("ENABLE_TUNNEL", "true").strip().lower()
    auto_tunnel = (env_tunnel_val in ("true", "1", "yes")) if (find_cloudflared_path() and not is_render) else False

    if args.no_tunnel or is_render:
        enable_tunnel = False
    elif args.tunnel is not None:
        enable_tunnel = args.tunnel
    else:
        enable_tunnel = auto_tunnel

    should_send_email = not args.no_email and not is_render
    open_browser = args.open and not is_render
    run_server(port=args.port, open_browser=open_browser, enable_tunnel=enable_tunnel, alert_email=args.email, send_email=should_send_email)
