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
from http.server import HTTPServer, SimpleHTTPRequestHandler
from pathlib import Path

# Enable UTF-8 encoding on Windows consoles
if sys.platform == "win32":
    try:
        if sys.stdout and hasattr(sys.stdout, "reconfigure"):
            sys.stdout.reconfigure(encoding="utf-8", errors="replace")
        if sys.stderr and hasattr(sys.stderr, "reconfigure"):
            sys.stderr.reconfigure(encoding="utf-8", errors="replace")
    except Exception:
        pass

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
RESEND_API_KEY = ""
RESEND_FROM = "RosterX Operations <onboarding@resend.dev>"

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
    global RESEND_API_KEY, RESEND_FROM
    if not RESEND_API_KEY:
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
            "Authorization": f"Bearer {RESEND_API_KEY}",
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
                    "Authorization": f"Bearer {RESEND_API_KEY}",
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

def start_cloudflared_tunnel(port=8000, timeout=15):
    """
    Launch cloudflared quick tunnel to forward public traffic to http://localhost:port.
    Returns (process, public_url).
    """
    cf_bin = find_cloudflared_path()
    if not cf_bin:
        return None, None

    cmd = [cf_bin, "tunnel", "--url", f"http://localhost:{port}"]
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
    """Parse .env file safely without third-party dependencies."""
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
        """Append CORS headers to allow cross-origin and local access."""
        self.send_header("Access-Control-Allow-Origin", "*")
        self.send_header("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS")
        self.send_header("Access-Control-Allow-Headers", "Content-Type, Authorization, X-Requested-With")

    def send_json_response(self, data, status_code=200):
        """Helper to serialize and transmit JSON payload with CORS."""
        try:
            payload = json.dumps(data, default=str).encode("utf-8")
        except Exception as e:
            payload = json.dumps({"success": False, "message": f"Serialization error: {e}"}).encode("utf-8")
            status_code = 500

        self.send_response(status_code)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Content-Length", str(len(payload)))
        self.send_cors_headers()
        self.end_headers()
        self.wfile.write(payload)

    def do_OPTIONS(self):
        """Handle CORS preflight requests smoothly."""
        self.send_response(204)
        self.send_cors_headers()
        self.send_header("Access-Control-Max-Age", "86400")
        self.end_headers()

    def do_GET(self):
        # Normalize and inspect path
        clean_path = self.path.split("?")[0].strip()

        # ==========================================
        # REST API Endpoints (/api/*)
        # ==========================================
        if clean_path.startswith("/api/"):
            self.handle_api_get(clean_path)
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
        # 1. GET /api/test-connection
        if clean_path == "/api/test-connection":
            if ATLAS_CLIENT is not None:
                try:
                    ATLAS_CLIENT.admin.command("ping")
                    self.send_json_response({
                        "success": True,
                        "message": f"Connected to {ATLAS_CLUSTER}",
                        "cluster": ATLAS_CLUSTER,
                        "database": ATLAS_DB_NAME,
                        "status": "connected"
                    })
                    return
                except Exception as e:
                    self.send_json_response({
                        "success": False,
                        "message": f"Atlas ping failed: {e}",
                        "cluster": ATLAS_CLUSTER,
                        "database": ATLAS_DB_NAME,
                        "status": "disconnected"
                    }, 503)
                    return
            else:
                self.send_json_response({
                    "success": False,
                    "message": f"Atlas not connected: {ATLAS_ERROR or 'Client not initialized'}",
                    "cluster": ATLAS_CLUSTER,
                    "database": ATLAS_DB_NAME,
                    "status": "disconnected"
                }, 503)
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

        # Catch-all for unknown /api/ GET
        self.send_json_response({
            "success": False,
            "message": f"Endpoint GET {clean_path} not found"
        }, 404)

    def do_POST(self):
        clean_path = self.path.split("?")[0].strip()

        if clean_path.startswith("/api/"):
            self.handle_api_post(clean_path)
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
                    eng_id = eng.get("id")
                    eng_name = eng.get("name")
                    query = {}
                    if eng_id and eng_name:
                        query = {"$or": [
                            {"id": eng_id},
                            {"name": eng_name},
                            {"name": {"$regex": f"^{re.escape(eng_name)}$", "$options": "i"}}
                        ]}
                    elif eng_id:
                        query = {"id": eng_id}
                    elif eng_name:
                        query = {"$or": [
                            {"name": eng_name},
                            {"name": {"$regex": f"^{re.escape(eng_name)}$", "$options": "i"}}
                        ]}

                    if query:
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

def run_server(port=8000, open_browser=False, enable_tunnel=False):
    if not WEB_DIR.exists():
        print(f"[ERROR] 'admin-web' directory not found at {WEB_DIR}")
        sys.exit(1)

    lan_ip = get_lan_ip()
    env = load_env_variables()

    # Initialize PyMongo Atlas Bridge
    init_atlas_connection(env)

    global RESEND_API_KEY, RESEND_FROM
    RESEND_API_KEY = env.get("RESEND_API_KEY", "").strip()
    RESEND_FROM = env.get("RESEND_FROM", "RosterX Operations <onboarding@resend.dev>").strip()

    mongo_uri = env.get("MONGODB_URI", "")
    cluster_name = env.get("ATLAS_CLUSTER", "Cluster0")
    db_name = env.get("ATLAS_DATABASE", "rosterx")
    gemini_key = env.get("GEMINI_API_KEY", "")
    gemini_model = env.get("GEMINI_MODEL", "gemini-1.5-flash")

    # Start Cloudflare Tunnel if requested
    tunnel_proc = None
    public_url = None
    if enable_tunnel:
        print("\n  [CLOUDFLARE] Initializing secure quick tunnel to Cloudflare Edge...")
        tunnel_proc, public_url = start_cloudflared_tunnel(port=port)

    # Banner UI
    border = "=" * 68
    sub_border = "-" * 68
    print("\n" + border)
    print("  🚀  ROSTERX — SECURE ADMIN SERVER & NATIVE ATLAS BRIDGE")
    print(border)
    print("  [ACCESS LINKS]")
    print(f"   ► Local Web App : \033[96mhttp://localhost:{port}\033[0m")
    print(f"   ► Localhost IPv4: \033[96mhttp://127.0.0.1:{port}\033[0m")
    print(f"   ► Network (LAN) : \033[92mhttp://{lan_ip}:{port}\033[0m  (Mobile/Other PCs)")
    print(f"   ► REST API Base : \033[96mhttp://localhost:{port}/api\033[0m  (Native PyMongo Bridge)")
    if public_url:
        print(f"   ► Public Tunnel : \033[93m\033[1m{public_url}\033[0m  (Worldwide Public Internet)")
    elif enable_tunnel:
        print("   ► Public Tunnel : \033[91mFailed to obtain public URL (check connection)\033[0m")
    else:
        cf_status = "Available (run with --tunnel or double-click start_tunnel.bat)" if find_cloudflared_path() else "Run winget install Cloudflare.cloudflared"
        print(f"   ► Public Tunnel : \033[90m{cf_status}\033[0m")
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

    print(f"   ► REST Routes   : /api/test-connection, /api/roster/<id>, /api/engineers")
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
    print(border)
    print("  Status: Server is actively listening. Press Ctrl+C to stop.\n")

    server_address = ("", port)
    try:
        httpd = HTTPServer(server_address, SecureRosterHandler)
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
    parser = argparse.ArgumentParser(description="Run RosterX Secure Server & Atlas Bridge")
    parser.add_argument("--port", type=int, default=8000, help="Port to bind (default: 8000)")
    parser.add_argument("--open", action="store_true", help="Automatically open browser on launch")
    parser.add_argument("--tunnel", "-t", action="store_true", help="Launch Cloudflare Tunnel to expose localhost to the public Internet")
    args = parser.parse_args()

    run_server(port=args.port, open_browser=args.open, enable_tunnel=args.tunnel)
