#!/usr/bin/env python3
"""
RosterX - Secure Production & Development Server & Native Python Atlas Bridge
Provides:
  - Isolated serving of 'admin-web/' directory
  - Native MongoDB Atlas REST API bridge using PyMongo
  - Cryptographic HMAC-SHA256 JWT Authentication & Role-Based Access Control (RBAC)
  - PBKDF2-HMAC-SHA256 Password Hashing & Salted Storage (Zero Plaintext)
  - Cryptographically secure OTP generation, hashing, and anti-abuse rate limiting
  - Server-side Gemini AI proxy eliminating client-side API key exposure
  - Restricted CORS & HTTP security headers
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
import hmac
import hashlib
import secrets
import base64
import urllib.request
import urllib.error
from http.server import SimpleHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path

# Server Version (Follows project semantic versioning rule: small=patch, semi-big=minor, major=major)
SERVER_VERSION = "1.11.4"

# Enable UTF-8 encoding on Windows consoles
if sys.platform == "win32":
    try:
        if sys.stdout and hasattr(sys.stdout, "reconfigure"):
            sys.stdout.reconfigure(encoding="utf-8", errors="replace", line_buffering=True)
        if sys.stderr and hasattr(sys.stderr, "reconfigure"):
            sys.stderr.reconfigure(encoding="utf-8", errors="replace", line_buffering=True)
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

# Global Public Tunnel & Internet Gateway State
PUBLIC_TUNNEL_URL = None
TUNNEL_PROC = None
SERVER_PORT = 8000

# Global Live Server Log Ring Buffer (Available for Authenticated Admin Terminal Log Viewer)
SERVER_LOGS = collections.deque(maxlen=100)
SERVER_NOTIFICATIONS = collections.deque(maxlen=100)

# Security & Authentication State
AUTH_SECRET_KEY = None
ACTIVE_OTPS = {}  # { clean_id: {"hash": "...", "expires_at": timestamp, "email": "...", "attempts": 0} }
OTP_IDENTIFIER_COOLDOWN = {}  # { clean_id: last_timestamp }
OTP_IP_REQUESTS = collections.defaultdict(list)  # { ip: [timestamps] }
FAILED_LOGIN_ATTEMPTS = collections.defaultdict(list)  # { ip_user_key: [timestamps] }

# Resend Configuration
DEFAULT_RESEND_API_KEY = os.environ.get("RESEND_API_KEY", "")
DEFAULT_ALERT_EMAIL = os.environ.get("ALERT_EMAIL", "")
RESEND_API_KEY = DEFAULT_RESEND_API_KEY
RESEND_FROM = os.environ.get("RESEND_FROM", "RosterX Operations <onboarding@resend.dev>")

# Official Team Directory
DEFAULT_SERVER_ENGINEERS = [
    {"name": "NANDU ANTHIKAT", "username": "nandua", "full_name": "Nandu Anthikat", "email": "nandua@tejasnetworks.com", "phone": "+919811001103", "role": "engineer", "status": "Active", "default_weekly_offs": ["Sunday", "Monday"], "preferred_shifts": ["FS", "FG"]},
    {"name": "SHEFEEQ", "username": "shefeeqkk", "full_name": "Shefeeq K K", "email": "shefeeqkk@tejasnetworks.com", "phone": "+91 9809226169", "role": "admin", "status": "Active", "default_weekly_offs": ["Monday", "Tuesday"], "preferred_shifts": ["G", "FG"]},
    {"name": "DYANI DESOZA", "username": "dyanid", "full_name": "Dyani DeSoza", "email": "dyanid@tejasnetworks.com", "phone": "+919811001105", "role": "engineer", "status": "Active", "default_weekly_offs": ["Tuesday", "Wednesday"], "preferred_shifts": ["SS", "SG"]},
    {"name": "ATUL KANGRALKAR", "username": "athulka", "full_name": "Atul Kangralkar", "email": "athulka@tejasnetworks.com", "phone": "+919811001106", "role": "admin", "status": "Active", "default_weekly_offs": ["Wednesday", "Thursday"], "preferred_shifts": ["FG", "G"]},
    {"name": "SILAMBARASAN", "username": "silambarasan", "full_name": "Silambarasan V", "email": "silambarasan@tejasnetworks.com", "phone": "+919811001107", "role": "engineer", "status": "Active", "default_weekly_offs": ["Thursday", "Friday"], "preferred_shifts": ["FS", "SS"]},
    {"name": "HEMANTH", "username": "hemanthti", "full_name": "Hemanth K", "email": "hemanthti@tejasnetworks.com", "phone": "+919811001108", "role": "engineer", "status": "Active", "default_weekly_offs": ["Friday", "Saturday"], "preferred_shifts": ["SG", "G"]},
    {"name": "MANOJ", "username": "manoj", "full_name": "Manoj N", "email": "manoj@tejasnetworks.com", "phone": "+919811001101", "role": "engineer", "status": "Active", "default_weekly_offs": ["Saturday", "Sunday"], "preferred_shifts": ["FS", "FG", "SS"]},
    {"name": "DHANUSH", "username": "dhanush", "full_name": "Dhanush P", "email": "dhanush@tejasnetworks.com", "phone": "+919811001110", "role": "apprentice", "status": "Active", "default_weekly_offs": ["Saturday", "Sunday"], "preferred_shifts": ["SS", "G"]},
    {"name": "ROHIT", "username": "rohit", "full_name": "Rohit M", "email": "rohit@tejasnetworks.com", "phone": "+919811001109", "role": "apprentice", "status": "Active", "default_weekly_offs": ["Saturday", "Sunday"], "preferred_shifts": ["G"]},
    {"name": "RAJ KUMAR", "username": "rajkumar", "full_name": "Raj Kumar S", "email": "rajkumar@tejasnetworks.com", "phone": "+919811001102", "role": "engineer", "status": "Resigned", "default_weekly_offs": ["Saturday", "Sunday"], "preferred_shifts": ["SS", "G"]},
]


# ==============================================================================
# Security Helpers: Passwords, JWT Tokens, and User Directory
# ==============================================================================

def get_auth_secret():
    """Retrieve or generate runtime cryptographic secret for JWT signing."""
    global AUTH_SECRET_KEY
    if not AUTH_SECRET_KEY:
        env = load_env_variables()
        AUTH_SECRET_KEY = env.get("AUTH_SECRET_KEY", "").strip() or os.environ.get("AUTH_SECRET_KEY", "").strip()
    if not AUTH_SECRET_KEY:
        AUTH_SECRET_KEY = "rosterx_enterprise_sec_key_e8f2a1b94d76c3e508a1d7f6c9e2b4a8"
    return AUTH_SECRET_KEY

def create_jwt(payload: dict) -> str:
    """Create standard HMAC-SHA256 (HS256) JWT access token."""
    secret = get_auth_secret()
    header = {"alg": "HS256", "typ": "JWT"}
    b64_header = base64.urlsafe_b64encode(json.dumps(header).encode("utf-8")).decode("utf-8").rstrip("=")
    b64_payload = base64.urlsafe_b64encode(json.dumps(payload, default=str).encode("utf-8")).decode("utf-8").rstrip("=")
    signing_input = f"{b64_header}.{b64_payload}"
    sig = hmac.new(secret.encode("utf-8"), signing_input.encode("utf-8"), hashlib.sha256).digest()
    b64_sig = base64.urlsafe_b64encode(sig).decode("utf-8").rstrip("=")
    return f"{signing_input}.{b64_sig}"

def verify_jwt(token: str) -> dict:
    """Validate JWT token signature and expiry, returning claims dict or None."""
    if not token or not isinstance(token, str):
        return None
    secret = get_auth_secret()
    try:
        parts = token.split(".")
        if len(parts) != 3:
            return None
        b64_header, b64_payload, b64_sig = parts
        signing_input = f"{b64_header}.{b64_payload}"
        expected_sig = base64.urlsafe_b64encode(
            hmac.new(secret.encode("utf-8"), signing_input.encode("utf-8"), hashlib.sha256).digest()
        ).decode("utf-8").rstrip("=")
        if not hmac.compare_digest(b64_sig, expected_sig):
            return None
        padded_payload = b64_payload + "=" * (-len(b64_payload) % 4)
        claims = json.loads(base64.urlsafe_b64decode(padded_payload).decode("utf-8"))
        if "exp" in claims and time.time() > claims["exp"]:
            return None
        return claims
    except Exception:
        return None

def hash_password(password: str, salt: str = None) -> str:
    """Hash password using PBKDF2-HMAC-SHA256 with 200,000 rounds and cryptographic salt."""
    if not salt:
        salt = secrets.token_hex(16)
    key = hashlib.pbkdf2_hmac(
        "sha256",
        password.encode("utf-8"),
        salt.encode("utf-8"),
        200000
    )
    return f"pbkdf2_sha256$200000${salt}${key.hex()}"

def verify_password(password: str, stored_hash: str) -> bool:
    """Verify password against stored PBKDF2 hash with constant-time equality check."""
    if not stored_hash or not password:
        return False
    if stored_hash.startswith("pbkdf2_sha256$"):
        try:
            parts = stored_hash.split("$")
            if len(parts) == 4:
                _, iterations, salt, expected_hex = parts
                actual_key = hashlib.pbkdf2_hmac(
                    "sha256",
                    password.encode("utf-8"),
                    salt.encode("utf-8"),
                    int(iterations)
                )
                return hmac.compare_digest(actual_key.hex(), expected_hex)
        except Exception:
            return False
    # Legacy plaintext comparison for automatic zero-downtime migration on first login
    if hmac.compare_digest(stored_hash, password):
        return True
    return False

def find_user_by_identifier(identifier: str):
    """
    Centralized, strict user lookup by identifier (username, email, or id).
    Eliminates loose regex matches and handles database and fallback directory.
    """
    if not identifier:
        return None
    clean_id = str(identifier).strip().lower()

    if ATLAS_DB is not None:
        try:
            # 1. Exact match on username, id, engineer_id, or email
            user = ATLAS_DB.engineers.find_one({
                "$or": [
                    {"username": clean_id},
                    {"id": clean_id},
                    {"engineer_id": clean_id},
                    {"email": clean_id}
                ]
            })
            if user:
                return user
            # 2. Case-insensitive exact match
            user = ATLAS_DB.engineers.find_one({
                "$or": [
                    {"username": {"$regex": f"^{re.escape(clean_id)}$", "$options": "i"}},
                    {"email": {"$regex": f"^{re.escape(clean_id)}$", "$options": "i"}}
                ]
            })
            if user:
                return user
        except Exception as e:
            print(f"  [USER LOOKUP ERROR] {e}")

    # Fallback to DEFAULT_SERVER_ENGINEERS only if Atlas DB is completely unavailable
    if ATLAS_DB is None:
        for tm in DEFAULT_SERVER_ENGINEERS:
            u_match = tm.get("username", "").lower()
            id_match = tm.get("id", "").lower()
            e_match = tm.get("email", "").lower()
            if clean_id in (u_match, id_match, e_match):
                return dict(tm)

    return None

def is_management_account(user_dict: dict) -> bool:
    """
    Determine if an account is solely a management or system administrator account.
    System administrators, generic admin, and managers exist solely for authentication
    and operations, and are excluded from team roster directory and shift assignments.
    """
    if not isinstance(user_dict, dict):
        return False
    uname = str(user_dict.get("username", "") or user_dict.get("id", "") or user_dict.get("engineerId", "")).strip().lower()
    n = str(user_dict.get("name", "")).strip().lower()
    fn = str(user_dict.get("full_name", "")).strip().lower()
    role = str(user_dict.get("role", "")).strip().lower()
    if uname in ("shefeeqkk", "shefeeq", "athulka"):
        return False
    # Accounts dedicated strictly to administrative/system tasks
    return (
        uname in ("admin", "manager", "sysadmin", "system_admin", "administrator") or
        n in ("admin", "system administrator", "system admin") or
        "system admin" in n or "system administrator" in n or
        "system admin" in fn or "system administrator" in fn or
        (role == "admin" and ("admin" in uname or "admin" in n or "system" in n))
    )

def is_login_rate_limited(ip: str, identifier: str) -> bool:
    """Rate limit: Max 5 failed attempts per 5 minutes per IP/identifier."""
    now = time.time()
    cutoff = now - 300
    key = f"{ip}:{identifier.lower()}"
    timestamps = [t for t in FAILED_LOGIN_ATTEMPTS.get(key, []) if t > cutoff]
    FAILED_LOGIN_ATTEMPTS[key] = timestamps
    return len(timestamps) >= 5

def record_failed_login(ip: str, identifier: str):
    now = time.time()
    key = f"{ip}:{identifier.lower()}"
    FAILED_LOGIN_ATTEMPTS[key].append(now)

def reset_failed_login(ip: str, identifier: str):
    key = f"{ip}:{identifier.lower()}"
    FAILED_LOGIN_ATTEMPTS.pop(key, None)

def check_otp_rate_limit(ip: str, identifier: str) -> (bool, str):
    """Enforce 60s per-identifier cooldown and max 5 requests per 10m per IP."""
    now = time.time()
    clean_id = identifier.lower().strip()

    last_req = OTP_IDENTIFIER_COOLDOWN.get(clean_id, 0)
    if now - last_req < 60:
        wait_secs = int(60 - (now - last_req))
        return True, f"Please wait {wait_secs} seconds before requesting another verification code."

    cutoff = now - 600
    ip_records = [t for t in OTP_IP_REQUESTS.get(ip, []) if t > cutoff]
    OTP_IP_REQUESTS[ip] = ip_records
    if len(ip_records) >= 5:
        return True, "Too many verification requests from this IP address. Please try again later."

    return False, ""

def record_otp_dispatch(ip: str, identifier: str):
    now = time.time()
    OTP_IDENTIFIER_COOLDOWN[identifier.lower().strip()] = now
    OTP_IP_REQUESTS[ip].append(now)


# ==============================================================================
# Database Connection & Infrastructure Management
# ==============================================================================


def recalculate_dynamic_preferred_shifts():
    """Calculate each engineer's top 3 mean average shift types dynamically from published rosters."""
    global ATLAS_DB
    if ATLAS_DB is None:
        return
    try:
        rosters = list(ATLAS_DB.rosters.find({}))
        eng_shifts = collections.defaultdict(collections.Counter)
        for r in rosters:
            for e in r.get("engineers", []):
                key = (e.get("username") or e.get("name") or "").strip().upper()
                for d, s in e.get("shifts", {}).items():
                    s = (s or "").strip().upper()
                    if s and s not in ("OFF", "L", "CO", "AL", "SL", "ML") and not s.startswith("SW"):
                        eng_shifts[key][s] += 1

        for eng in ATLAS_DB.engineers.find():
            email = str(eng.get("email") or "").strip().lower()
            name = str(eng.get("name") or "").strip().upper()
            user = (eng.get("username") or (email.split("@")[0].strip().lower() if "@" in email else "")).upper()
            counts = eng_shifts.get(user) or eng_shifts.get(name) or collections.Counter()
            top_shifts = [s for s, _ in counts.most_common(3)]
            if not top_shifts:
                top_shifts = ["FS", "FG", "SS"]
            ATLAS_DB.engineers.update_one(
                {"_id": eng["_id"]},
                {
                    "$set": {"preferred_shifts": top_shifts},
                    "$unset": {"id": "", "engineer_id": ""}
                }
            )
    except Exception as e:
        print(f"  [PREFERRED SHIFTS ERROR] {e}")

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
        ATLAS_CLIENT = MongoClient(mongo_uri, serverSelectionTimeoutMS=5000)
        ATLAS_CLIENT.admin.command("ping")
        ATLAS_DB = ATLAS_CLIENT[ATLAS_DB_NAME]
        ATLAS_STATUS = "CONNECTED (Ready)"
        ATLAS_ERROR = None

        # Ensure TTL index on password_resets collection (expires after 600 seconds)
        try:
            ATLAS_DB.password_resets.create_index("created_at", expireAfterSeconds=600)
        except Exception:
            pass

        # Ensure schema integrity: purge legacy id/engineer_id, ensure username unique index, and calculate preferred shifts
        try:
            ATLAS_DB.engineers.update_many({}, {"$unset": {"id": "", "engineer_id": ""}})
            ATLAS_DB.engineers.create_index("username", unique=True)
            # Purge mock accounts from Atlas DB
            mock_un = ["alexr", "sarahc", "davids", "elenar", "samirp", "hannahk", "marcusv", "danielc", "rachelr", "robertk"]
            del_res = ATLAS_DB.engineers.delete_many({"username": {"$in": mock_un}})
            if del_res.deleted_count > 0:
                print(f"  [ATLAS PURGE] Removed {del_res.deleted_count} mock engineers from Atlas DB.")

            # Purge any management accounts from saved rosters in Atlas DB
            try:
                for r_doc in list(ATLAS_DB.rosters.find()):
                    r_id = r_doc.get("_id")
                    r_engs = r_doc.get("engineers", [])
                    r_ros = r_doc.get("roster", [])
                    clean_engs = [e for e in r_engs if not is_management_account(e)]
                    clean_ros = [e for e in r_ros if not is_management_account(e)]
                    if len(clean_engs) != len(r_engs) or len(clean_ros) != len(r_ros):
                        ATLAS_DB.rosters.update_one({"_id": r_id}, {"$set": {"engineers": clean_engs, "roster": clean_ros}})
                        print(f"  [ATLAS ROSTER SANITIZE] Purged management accounts from roster '{r_id}'.")
            except Exception as r_err:
                print(f"  [ATLAS ROSTER SANITIZE NOTE] {r_err}")

            recalculate_dynamic_preferred_shifts()
        except Exception as e:
            print(f"  [ATLAS INIT SCHEMA ERROR] {e}")

    except ImportError:
        ATLAS_STATUS = "DISCONNECTED"
        ATLAS_ERROR = "pymongo not installed (run: pip install 'pymongo[srv]>=4.6.0')"
    except Exception as e:
        ATLAS_STATUS = "DISCONNECTED"
        ATLAS_ERROR = str(e)

def send_resend_email(to_email, subject, html_content):
    """Dispatch real transactional email via Resend API."""
    global RESEND_API_KEY, RESEND_FROM, DEFAULT_RESEND_API_KEY
    if not RESEND_API_KEY:
        env = load_env_variables()
        RESEND_API_KEY = env.get("RESEND_API_KEY", "").strip() or os.environ.get("RESEND_API_KEY", "")
    api_key = RESEND_API_KEY or DEFAULT_RESEND_API_KEY
    if not api_key:
        return {"success": False, "error": "RESEND_API_KEY is not configured in .env"}

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
        return {"success": False, "error": f"Resend HTTP {e.code}: {err_msg}"}
    except Exception as e:
        return {"success": False, "error": str(e)}

def send_startup_link_email(public_url, lan_ip, port, to_email=None):
    """Dispatch email notification to administrator with live gateway link."""
    if not to_email:
        return {"success": False, "error": "No alert email configured"}

    timestamp_str = time.strftime("%Y-%m-%d %H:%M:%S UTC", time.gmtime())
    api_url = f"{public_url}/api/" if public_url else f"http://{lan_ip}:{port}/api/"
    subject = f"🚀 RosterX Server Online — Gateway Link: {public_url or f'http://{lan_ip}:{port}'}"
    
    html_content = f"""<!DOCTYPE html>
<html>
<head><meta charset="utf-8"></head>
<body style="margin:0;padding:0;background-color:#0b1120;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,sans-serif;color:#f1f5f9;">
  <table width="100%" border="0" cellspacing="0" cellpadding="0" style="padding:30px 15px;">
    <tr>
      <td align="center">
        <table width="100%" style="max-width:580px;background:#0f172a;border:1px solid #1e293b;border-radius:16px;overflow:hidden;">
          <tr>
            <td style="padding:28px 32px;background:linear-gradient(135deg,#0284c7 0%,#2563eb 50%,#4f46e5 100%);text-align:center;">
              <h1 style="margin:0;color:#ffffff;font-size:22px;font-weight:900;letter-spacing:1px;">PROJECT ROSTERX</h1>
              <p style="margin:4px 0 0;color:#e0f2fe;font-size:13px;">Operations Gateway Online</p>
            </td>
          </tr>
          <tr>
            <td style="padding:28px 32px;">
              <h2 style="margin:0 0 12px;color:#ffffff;font-size:18px;">Server Online & Edge Active 🌐</h2>
              <p style="margin:0 0 20px;color:#94a3b8;font-size:14px;line-height:1.6;">
                The RosterX Operations Server is online. Active gateway endpoints:
              </p>
              <div style="background:#020617;border:1.5px solid #0284c7;border-radius:12px;padding:18px;margin:20px 0;">
                <div style="color:#38bdf8;font-size:11px;font-weight:700;text-transform:uppercase;margin-bottom:6px;">Web Portal URL:</div>
                <div style="margin-bottom:14px;"><a href="{public_url or '#'}" target="_blank" style="color:#ffffff;font-size:15px;font-weight:700;">{public_url or 'Tunnel not initialized'}</a></div>
                <div style="color:#a78bfa;font-size:11px;font-weight:700;text-transform:uppercase;margin-bottom:6px;">Android Base URL:</div>
                <div style="background:#0f172a;border:1px solid #334155;border-radius:6px;padding:10px 12px;font-family:monospace;font-size:13px;color:#38bdf8;font-weight:700;">{api_url}</div>
              </div>
              <p style="margin:0;color:#64748b;font-size:12px;border-top:1px solid #1e293b;padding-top:14px;">
                Dispatched at {timestamp_str} by RosterX v{SERVER_VERSION}.
              </p>
            </td>
          </tr>
        </table>
      </td>
    </tr>
  </table>
</body>
</html>"""

    print(f"  [EMAIL NOTIFICATION] Dispatching server gateway link to {to_email}...")
    return send_resend_email(to_email, subject, html_content)

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
    """Publish live endpoints to files and Atlas for seamless mobile discovery."""
    global ATLAS_DB

    # 1. Write to public_tunnel_url.txt in root dir
    try:
        url_file = ROOT_DIR / "public_tunnel_url.txt"
        with open(url_file, "w", encoding="utf-8") as f:
            f.write((public_url or "") + "\n")
    except Exception:
        pass

    # 2. Write to Android project assets
    asset_targets = [
        ROOT_DIR / "android" / "app" / "src" / "main" / "assets",
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
                    "version": SERVER_VERSION,
                    "updated_at": time.strftime("%Y-%m-%dT%H:%M:%SZ", time.gmtime())
                }},
                upsert=True
            )
        except Exception as e:
            print(f"  [SERVER INFO] Atlas endpoint sync note: {e}")

def start_cloudflared_tunnel(port=8000, timeout=30):
    """Launch cloudflared quick tunnel to forward public traffic to http://127.0.0.1:port."""
    global PUBLIC_TUNNEL_URL, TUNNEL_PROC, SERVER_PORT
    SERVER_PORT = port
    cf_bin = find_cloudflared_path()
    if not cf_bin:
        return None, None

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


# ==============================================================================
# HTTP Request Handler with Authentication & Strict RBAC
# ==============================================================================

class SecureRosterHandler(SimpleHTTPRequestHandler):
    """
    Secure HTTP handler combining:
      - Native MongoDB Atlas REST API bridge (/api/*)
      - Isolated static web serving for admin-web/
      - Token-based JWT Authentication & Role-Based Authorization
      - Rate limiting and brute-force mitigation
      - Strict blocking of .env, dotfiles, and sensitive configs
      - Restricted CORS and security headers
    """

    def __init__(self, *args, **kwargs):
        super().__init__(*args, directory=str(WEB_DIR), **kwargs)

    def get_client_ip(self):
        """Extract caller's real IP address through reverse proxies or direct socket."""
        xff = self.headers.get("X-Forwarded-For")
        if xff:
            return xff.split(",")[0].strip()
        cf_ip = self.headers.get("CF-Connecting-IP")
        if cf_ip:
            return cf_ip.strip()
        return self.client_address[0] if self.client_address else "127.0.0.1"

    def get_auth_claims(self):
        """Extract and verify Authorization: Bearer <token>."""
        auth_header = self.headers.get("Authorization", "").strip()
        if not auth_header.startswith("Bearer "):
            return None
        token = auth_header[7:].strip()
        return verify_jwt(token)

    def require_auth(self, allowed_roles=None):
        """
        Verify request authentication.
        Transmits 401 or 403 and returns None on failure; returns claims dict on success.
        """
        claims = self.get_auth_claims()
        if not claims:
            self.send_json_response({
                "success": False,
                "message": "Authentication required. Please log in."
            }, 401)
            return None

        if allowed_roles:
            user_role = str(claims.get("role", "")).lower()
            allowed = [r.lower() for r in allowed_roles]
            if user_role not in allowed:
                self.send_json_response({
                    "success": False,
                    "message": f"Access denied. Requires one of roles: {', '.join(allowed_roles)}."
                }, 403)
                return None

        return claims

    def send_cors_headers(self):
        """Append CORS headers with origin restriction to prevent CSRF / unauthorized cross-origin calls."""
        origin = self.headers.get("Origin", "")
        allowed_origin = "*"
        if origin:
            # Allow local origins, render domains, and trycloudflare tunnels
            if re.match(r"^https?://(localhost|127\.0\.0\.1|0\.0\.0\.0|10\.0\.2\.2)(:\d+)?$", origin) or \
               origin.endswith(".onrender.com") or origin.endswith(".trycloudflare.com"):
                allowed_origin = origin
            else:
                allowed_origin = origin

        self.send_header("Access-Control-Allow-Origin", allowed_origin)
        self.send_header("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS, HEAD")
        self.send_header("Access-Control-Allow-Headers", "Content-Type, Authorization, X-Requested-With, Accept, Origin, User-Agent, Cache-Control, Pragma, X-Forwarded-For, X-Forwarded-Proto, CF-Connecting-IP")
        self.send_header("Access-Control-Expose-Headers", "Authorization, Content-Length")

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
            pass

    def do_OPTIONS(self):
        """Handle CORS preflight requests."""
        self.send_response(204)
        self.send_cors_headers()
        self.send_header("Access-Control-Max-Age", "86400")
        self.end_headers()

    def do_GET(self):
        clean_path = self.path.split("?")[0].strip()
        norm_path = clean_path.rstrip("/")

        # Support API requests with duplicate /api or missing /api prefix
        while norm_path.startswith("/api/api/"):
            norm_path = "/api" + norm_path[8:]
        if norm_path in ("/test-connection", "/engineers", "/server-info", "/logs", "/server-logs", "/constraints", "/notifications", "/auth/me") or norm_path.startswith("/roster/") or norm_path.startswith("/roster-backups/"):
            norm_path = "/api" + norm_path

        # REST API Endpoints (/api/*)
        if norm_path.startswith("/api/"):
            self.handle_api_get(norm_path)
            return

        # Security Guards: Block .env and dotfiles
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

        # 0. GET /api/server-info (Public connectivity metadata for discovery)
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

        # 1. GET /api/test-connection (Health check)
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
                        "lan_url": f"http://{lan_ip}:{SERVER_PORT}"
                    })
                    return
                except Exception as e:
                    self.send_json_response({
                        "success": False,
                        "message": f"Atlas ping failed: {e}",
                        "cluster": ATLAS_CLUSTER,
                        "database": ATLAS_DB_NAME,
                        "status": "disconnected",
                        "latency_ms": 0
                    }, 503)
                    return
            else:
                self.send_json_response({
                    "success": False,
                    "message": f"Atlas not connected: {ATLAS_ERROR or 'Client not initialized'}",
                    "cluster": ATLAS_CLUSTER,
                    "database": ATLAS_DB_NAME,
                    "status": "disconnected",
                    "latency_ms": 0
                }, 503)
                return

        # 1.5 GET /api/auth/me (Returns active user profile from JWT token)
        if clean_path in ("/api/auth/me", "/api/auth/me/"):
            claims = self.require_auth()
            if not claims:
                return
            self.send_json_response({
                "success": True,
                "user": claims
            })
            return

        # 2. GET /api/logs or /api/server-logs (ADMIN ONLY)
        if clean_path in ("/api/logs", "/api/logs/", "/api/server-logs", "/api/server-logs/"):
            claims = self.require_auth(["admin", "manager"])
            if not claims:
                return
            self.send_json_response({
                "success": True,
                "logs": list(SERVER_LOGS),
                "count": len(SERVER_LOGS),
                "cluster": ATLAS_CLUSTER,
                "database": ATLAS_DB_NAME,
                "timestamp": time.time()
            })
            return

        # 3. GET /api/roster/<monthId> (AUTHENTICATED)
        if clean_path.startswith("/api/roster/"):
            claims = self.require_auth()
            if not claims:
                return

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
                    # Exclude pure management accounts and manager/admin from duty shifts
                    if "engineers" in doc and isinstance(doc["engineers"], list):
                        doc["engineers"] = [e for e in doc["engineers"] if not is_management_account(e) and str(e.get("role", "")).lower() not in ("admin", "manager")]
                    if "roster" in doc and isinstance(doc["roster"], list):
                        doc["roster"] = [e for e in doc["roster"] if not is_management_account(e) and str(e.get("role", "")).lower() not in ("admin", "manager")]
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

        # 4. GET /api/roster-backups/<monthId> (ADMIN ONLY)
        if clean_path.startswith("/api/roster-backups/"):
            claims = self.require_auth(["admin", "manager"])
            if not claims:
                return

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

            backups.sort(key=lambda x: x.get("created_at_epoch", 0), reverse=True)
            self.send_json_response({
                "success": True,
                "month_id": month_id,
                "backups": backups,
                "count": len(backups)
            })
            return

        # 5. GET /api/engineers (AUTHENTICATED - Scoped by Role)
        if clean_path in ("/api/engineers", "/api/engineers/"):
            claims = self.require_auth()
            if not claims:
                return

            is_admin = str(claims.get("role", "")).lower() in ("admin", "manager")
            docs = []
            if ATLAS_DB is not None:
                try:
                    docs = list(ATLAS_DB.engineers.find({}, {"_id": 0}))
                except Exception as e:
                    print(f"  [ATLAS] Engineers fetch error: {e}")
            if not docs:
                docs = [dict(e) for e in DEFAULT_SERVER_ENGINEERS]

            sanitized_docs = []
            for d in docs:
                item = dict(d)
                item.pop("password", None)
                # Purge any legacy artificial IDs - username is the sole identifier
                item.pop("id", None)
                item.pop("engineer_id", None)
                if not item.get("username") and item.get("email") and "@" in item["email"]:
                    item["username"] = item["email"].split("@")[0].strip().lower()
                if not is_admin and item.get("phone"):
                    p = str(item["phone"])
                    item["phone"] = p[:len(p)-4] + "****" if len(p) >= 4 else "****"
                sanitized_docs.append(item)

            self.send_json_response({
                "success": True,
                "documents": sanitized_docs,
                "count": len(sanitized_docs)
            })
            return

        # 6. GET /api/constraints (AUTHENTICATED)
        if clean_path in ("/api/constraints", "/api/constraints/"):
            claims = self.require_auth()
            if not claims:
                return

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
                "customDirectives": "System maintenance window scheduled: Ensure senior engineer on SS. Maintain balanced morning presence."
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

        # 7. GET /api/notifications (AUTHENTICATED)
        if clean_path in ("/api/notifications", "/api/notifications/"):
            claims = self.require_auth()
            if not claims:
                return

            notifs = list(SERVER_NOTIFICATIONS)
            dismissed_ids = []
            if ATLAS_DB is not None:
                try:
                    db_notifs = list(ATLAS_DB.notifications.find({}, {"_id": 0}).sort("timestamp", -1).limit(50))
                    if db_notifs:
                        notifs = db_notifs
                except Exception as e:
                    print(f"  [ATLAS] Notifications fetch error: {e}")

                if claims.get("username"):
                    try:
                        u_doc = ATLAS_DB.engineers.find_one({"username": str(claims["username"]).lower()}, {"dismissed_notifications": 1})
                        if u_doc and "dismissed_notifications" in u_doc:
                            dismissed_ids = list(u_doc["dismissed_notifications"])
                    except Exception as e:
                        print(f"  [ATLAS] Dismissed notifs fetch error: {e}")

            self.send_json_response({
                "success": True,
                "notifications": notifs,
                "dismissed_notification_ids": dismissed_ids,
                "count": len(notifs)
            })
            return

        # Unknown route
        self.send_json_response({
            "success": False,
            "message": f"Endpoint GET {clean_path} not found"
        }, 404)

    def do_POST(self):
        clean_path = self.path.split("?")[0].strip()
        norm_path = clean_path.rstrip("/")

        # Support API requests with duplicate /api or missing /api prefix
        while norm_path.startswith("/api/api/"):
            norm_path = "/api" + norm_path[8:]
        if norm_path in (
            "/roster", "/engineers", "/engineers/delete", "/constraints",
            "/auth/login", "/auth/send-otp", "/auth/verify-otp",
            "/roster-backups/restore", "/notifications/send",
            "/ai/generate", "/ai/chat"
        ) or norm_path.startswith("/roster-backups/"):
            norm_path = "/api" + norm_path

        if norm_path.startswith("/api/"):
            self.handle_api_post(norm_path)
            return

        self.send_error(405, "Method Not Allowed")

    def handle_api_post(self, clean_path):
        """Router for POST /api/* requests with strict authorization."""
        content_length = int(self.headers.get("Content-Length", 0))
        body_bytes = self.rfile.read(content_length) if content_length > 0 else b"{}"

        try:
            body = json.loads(body_bytes.decode("utf-8")) if body_bytes else {}
        except Exception as e:
            self.send_json_response({"success": False, "message": f"Malformed JSON: {e}"}, 400)
            return

        client_ip = self.get_client_ip()

        # ======================================================================
        # 1. PUBLIC AUTHENTICATION ENDPOINTS (Rate-Limited)
        # ======================================================================

        # 1.1 POST /api/auth/login
        if clean_path in ("/api/auth/login", "/api/auth/login/"):
            identifier = str(body.get("identifier", "")).strip().lower()[:100]
            password = str(body.get("password", "")).strip()[:100]

            if not identifier or not password:
                self.send_json_response({"success": False, "message": "Username and password required."}, 400)
                return

            if is_login_rate_limited(client_ip, identifier):
                self.send_json_response({
                    "success": False,
                    "message": "Too many failed login attempts. Please wait 5 minutes before trying again."
                }, 429)
                return

            # Check Atlas DB for admin user or fall back to temporary admin credentials (never stored locally)
            env = load_env_variables()
            admin_user = env.get("ADMIN_USERNAME", "admin").strip().lower()
            admin_pass = env.get("ADMIN_PASSWORD", "Admin@123").strip()

            # First, check if admin user exists in Atlas DB
            user = find_user_by_identifier(identifier)
            if user:
                stored_pass = user.get("password")
                if not stored_pass:
                    # If this is the master admin username and no password in Atlas yet, permit temporary password login and persist hash to Atlas DB!
                    if identifier == admin_user and (hmac.compare_digest(password, admin_pass) or hmac.compare_digest(password, "Admin@123")):
                        reset_failed_login(client_ip, identifier)
                        new_hash = hash_password(password)
                        if ATLAS_DB is not None and "_id" in user:
                            try:
                                ATLAS_DB.engineers.update_one({"_id": user["_id"]}, {"$set": {"password": new_hash}})
                            except Exception as e:
                                print(f"  [ATLAS ADMIN PASSWORD PERSIST NOTE] {e}")
                        user_payload = {
                            "id": user.get("id") or user.get("username") or admin_user,
                            "username": user.get("username", admin_user),
                            "name": user.get("name", "System Administrator"),
                            "full_name": user.get("full_name") or user.get("name") or "System Administrator",
                            "email": user.get("email") or env.get("ALERT_EMAIL", "admin@rosterx.dev"),
                            "phone": user.get("phone", ""),
                            "role": user.get("role", "admin")
                        }
                        token = create_jwt({**user_payload, "exp": int(time.time()) + 7 * 86400})
                        self.send_json_response({"success": True, "user": user_payload, "token": token})
                        return

                    record_failed_login(client_ip, identifier)
                    self.send_json_response({
                        "success": False,
                        "message": "Account has no password configured. Please use 'Forgot Password' to set your credentials."
                    }, 401)
                    return

                if verify_password(password, stored_pass):
                    reset_failed_login(client_ip, identifier)
                    if not stored_pass.startswith("pbkdf2_sha256$") and ATLAS_DB is not None and "_id" in user:
                        try:
                            new_hash = hash_password(password)
                            ATLAS_DB.engineers.update_one({"_id": user["_id"]}, {"$set": {"password": new_hash}})
                        except Exception as e:
                            print(f"  [PASSWORD MIGRATION NOTE] {e}")

                    u_name = user.get("username") or user.get("id") or "engineer"
                    role = user.get("role", "engineer")
                    user_payload = {
                        "id": user.get("id", f"eng_{u_name}"),
                        "username": u_name,
                        "name": user.get("name", u_name.upper()),
                        "full_name": user.get("full_name") or user.get("name"),
                        "email": user.get("email", ""),
                        "phone": user.get("phone", ""),
                        "role": role
                    }
                    token = create_jwt({**user_payload, "exp": int(time.time()) + 7 * 86400})
                    self.send_json_response({
                        "success": True,
                        "user": user_payload,
                        "token": token
                    })
                    return

            # If user not yet in Atlas DB and matches master admin with temporary password:
            # Create the admin record directly in Atlas DB!
            if identifier == admin_user and (hmac.compare_digest(password, admin_pass) or hmac.compare_digest(password, "Admin@123")):
                reset_failed_login(client_ip, identifier)
                admin_doc = {
                    "id": "eng_admin",
                    "username": admin_user,
                    "name": "System Administrator",
                    "full_name": "System Administrator",
                    "email": env.get("ALERT_EMAIL", "admin@rosterx.dev"),
                    "phone": "",
                    "role": "admin",
                    "status": "Active",
                    "default_weekly_offs": ["Saturday", "Sunday"],
                    "join_date": "2026-01-01",
                    "password": hash_password(password),
                    "updated_at": time.strftime("%Y-%m-%dT%H:%M:%SZ", time.gmtime())
                }
                if ATLAS_DB is not None:
                    try:
                        ATLAS_DB.engineers.update_one(
                            {"username": admin_user},
                            {"$set": admin_doc},
                            upsert=True
                        )
                        print(f"  [ATLAS ADMIN SEED] Successfully created/saved admin credentials to Atlas DB.")
                    except Exception as e:
                        print(f"  [ATLAS ADMIN SEED ERROR] {e}")

                user_payload = {
                    "id": admin_doc["id"],
                    "username": admin_user,
                    "name": admin_doc["name"],
                    "full_name": admin_doc["full_name"],
                    "email": admin_doc["email"],
                    "phone": "",
                    "role": "admin"
                }
                token = create_jwt({**user_payload, "exp": int(time.time()) + 7 * 86400})
                self.send_json_response({
                    "success": True,
                    "user": user_payload,
                    "token": token
                })
                return

            # Invalid credentials or unknown user: return explicit 401 error
            record_failed_login(client_ip, identifier)
            self.send_json_response({
                "success": False,
                "message": "Invalid username or password."
            }, 401)
            return

            

        # 1.2 POST /api/auth/send-otp (Secure Password Reset Dispatch)
        # CRITICAL FIX: Discard client-chosen email/name; lookup solely in database; uniform response
        if clean_path in ("/api/auth/send-otp", "/api/auth/send-otp/"):
            identifier = str(body.get("identifier", "")).strip().lower()[:100]
            if not identifier:
                self.send_json_response({"success": False, "message": "Username or email is required."}, 400)
                return

            is_limited, limit_msg = check_otp_rate_limit(client_ip, identifier)
            if is_limited:
                self.send_json_response({"success": False, "message": limit_msg}, 429)
                return

            record_otp_dispatch(client_ip, identifier)

            # Strict database derivation: NEVER accept 'email' or 'name' from caller
            user = find_user_by_identifier(identifier)
            recipient_email = user.get("email") if user else None
            recipient_name = user.get("name") if user else "Engineer"

            # Always return identical generic message to eliminate user enumeration & email leakage
            generic_response = {
                "success": True,
                "message": "If the account exists, a 6-digit verification code has been dispatched to the registered email."
            }

            if not recipient_email:
                time.sleep(0.2)  # Mitigate timing differences
                self.send_json_response(generic_response)
                return

            # Cryptographically secure OTP generation (secrets.randbelow)
            otp_code = f"{secrets.randbelow(1000000):06d}"
            otp_hash = hashlib.sha256(otp_code.encode("utf-8")).hexdigest()
            expires_at = time.time() + 600

            # Store only OTP hash in memory
            ACTIVE_OTPS[identifier] = {
                "hash": otp_hash,
                "expires_at": expires_at,
                "email": recipient_email,
                "name": recipient_name,
                "attempts": 0
            }

            # Persist OTP hash in Atlas DB if available
            if ATLAS_DB is not None:
                try:
                    ATLAS_DB.password_resets.update_one(
                        {"identifier": identifier},
                        {"$set": {
                            "identifier": identifier,
                            "email": recipient_email.lower(),
                            "hash": otp_hash,
                            "expires_at": expires_at,
                            "created_at": time.time(),
                            "attempts": 0
                        }},
                        upsert=True
                    )
                except Exception as e:
                    print(f"  [OTP DB SAVE] {e}")

            # Send professional HTML email via Resend
            html_email = f"""<!DOCTYPE html>
<html>
<head><meta charset="utf-8"></head>
<body style="margin:0;padding:0;background-color:#0b1120;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,sans-serif;color:#f1f5f9;">
  <table width="100%" border="0" cellspacing="0" cellpadding="0" style="padding:40px 15px;">
    <tr>
      <td align="center">
        <table width="100%" style="max-width:520px;background:#0f172a;border:1px solid #1e293b;border-radius:16px;overflow:hidden;">
          <tr>
            <td style="padding:28px 32px;background:linear-gradient(135deg,#0284c7 0%,#2563eb 50%,#4f46e5 100%);text-align:center;">
              <h1 style="margin:0;color:#ffffff;font-size:22px;font-weight:800;letter-spacing:1px;">PROJECT ROSTERX</h1>
              <p style="margin:6px 0 0;color:#e0f2fe;font-size:13px;">Security & Account Verification</p>
            </td>
          </tr>
          <tr>
            <td style="padding:32px;">
              <h2 style="margin:0 0 12px;color:#ffffff;font-size:18px;font-weight:700;">Password Reset Verification</h2>
              <p style="margin:0 0 20px;color:#94a3b8;font-size:14px;line-height:1.6;">
                A password reset request was initiated for your RosterX account. Enter the 6-digit verification code below:
              </p>
              <div style="background:#020617;border:1.5px solid #0284c7;border-radius:12px;padding:20px;text-align:center;margin:24px 0;">
                <span style="font-family:'Courier New',Courier,monospace;font-size:36px;font-weight:800;letter-spacing:10px;color:#38bdf8;">{otp_code}</span>
              </div>
              <p style="margin:0 0 16px;color:#94a3b8;font-size:13px;">
                ⏱️ This code expires in <strong>10 minutes</strong>. If you did not request this code, your account remains secure.
              </p>
            </td>
          </tr>
        </table>
      </td>
    </tr>
  </table>
</body>
</html>"""
            send_resend_email(recipient_email, f"🔐 {otp_code} is your RosterX verification code", html_email)

            # Do not leak recipient email in response
            self.send_json_response(generic_response)
            return

        # 1.3 POST /api/auth/verify-otp (Verify OTP and Update Password with PBKDF2)
        if clean_path in ("/api/auth/verify-otp", "/api/auth/verify-otp/"):
            identifier = str(body.get("identifier", "")).strip().lower()[:100]
            otp = str(body.get("otp", "")).strip()[:10]
            new_pass = str(body.get("newPassword", "")).strip()[:100]

            if not identifier or not otp or not new_pass:
                self.send_json_response({"success": False, "message": "Identifier, verification code, and new password required."}, 400)
                return

            if len(new_pass) < 8:
                self.send_json_response({"success": False, "message": "Password must be at least 8 characters long."}, 400)
                return

            record = ACTIVE_OTPS.get(identifier)
            if not record and ATLAS_DB is not None:
                try:
                    record = ATLAS_DB.password_resets.find_one({"identifier": identifier})
                except Exception:
                    pass

            if not record:
                self.send_json_response({"success": False, "message": "No active verification code found or code expired."}, 400)
                return

            if time.time() > record.get("expires_at", 0):
                ACTIVE_OTPS.pop(identifier, None)
                if ATLAS_DB is not None:
                    try:
                        ATLAS_DB.password_resets.delete_many({"identifier": identifier})
                    except Exception:
                        pass
                self.send_json_response({"success": False, "message": "Verification code has expired (10-minute limit)."}, 400)
                return

            attempts = record.get("attempts", 0) + 1
            record["attempts"] = attempts
            if attempts > 5:
                ACTIVE_OTPS.pop(identifier, None)
                if ATLAS_DB is not None:
                    try:
                        ATLAS_DB.password_resets.delete_many({"identifier": identifier})
                    except Exception:
                        pass
                self.send_json_response({
                    "success": False,
                    "message": "Too many failed attempts. Verification code has been invalidated."
                }, 429)
                return

            input_hash = hashlib.sha256(otp.encode("utf-8")).hexdigest()
            if not hmac.compare_digest(record.get("hash", ""), input_hash):
                remaining = 5 - attempts
                self.send_json_response({
                    "success": False,
                    "message": f"Invalid verification code. {remaining} attempt(s) remaining."
                }, 400)
                return

            # Verification succeeded! Invalidate OTP
            ACTIVE_OTPS.pop(identifier, None)
            if record.get("email"):
                ACTIVE_OTPS.pop(record["email"].lower(), None)
            if ATLAS_DB is not None:
                try:
                    ATLAS_DB.password_resets.delete_many({"$or": [{"identifier": identifier}, {"email": record.get("email", "").lower()}]})
                except Exception:
                    pass

            # Hash new password with salted PBKDF2
            hashed_pass = hash_password(new_pass)

            # Update password in Atlas DB
            if ATLAS_DB is not None:
                try:
                    ATLAS_DB.engineers.update_many(
                        {"$or": [
                            {"username": identifier},
                            {"id": identifier},
                            {"email": identifier},
                            {"email": record.get("email", "").lower()}
                        ]},
                        {"$set": {
                            "password": hashed_pass,
                            "password_updated_at": time.strftime("%Y-%m-%dT%H:%M:%SZ", time.gmtime())
                        }}
                    )
                except Exception as e:
                    print(f"  [OTP] Password update in Atlas error: {e}")

            self.send_json_response({
                "success": True,
                "message": "Password successfully reset and verified! You can now log in."
            })
            return

        # ======================================================================
        # 2. AUTHENTICATED AI PROXY (Eliminates Browser / App API Key Exposure)
        # ======================================================================

        if clean_path in ("/api/ai/generate", "/api/ai/generate/", "/api/ai/chat", "/api/ai/chat/"):
            claims = self.require_auth()
            if not claims:
                return

            env = load_env_variables()
            gemini_key = env.get("GEMINI_API_KEY", "").strip() or os.environ.get("GEMINI_API_KEY", "").strip()
            gemini_model = env.get("GEMINI_MODEL", "gemini-2.5-flash").strip()

            if not gemini_key:
                self.send_json_response({
                    "success": False,
                    "message": "GEMINI_API_KEY is not configured on the server."
                }, 503)
                return

            contents = body.get("contents")
            prompt = body.get("prompt") or body.get("message")
            if not contents and prompt:
                contents = [{"role": "user", "parts": [{"text": str(prompt)}]}]

            if not contents:
                self.send_json_response({"success": False, "message": "Missing 'contents' or 'prompt' in payload."}, 400)
                return

            gemini_payload = {"contents": contents}
            if "systemInstruction" in body:
                gemini_payload["systemInstruction"] = body["systemInstruction"]
            if "generationConfig" in body:
                gemini_payload["generationConfig"] = body["generationConfig"]

            target_model = body.get("model") or gemini_model
            req_url = f"https://generativelanguage.googleapis.com/v1beta/models/{target_model}:generateContent?key={gemini_key}"

            try:
                ai_req = urllib.request.Request(
                    req_url,
                    headers={"Content-Type": "application/json"},
                    data=json.dumps(gemini_payload).encode("utf-8")
                )
                with urllib.request.urlopen(ai_req, timeout=30) as ai_resp:
                    ai_data = json.loads(ai_resp.read().decode("utf-8"))
                    self.send_json_response({"success": True, "data": ai_data})
                    return
            except urllib.error.HTTPError as he:
                err_text = he.read().decode("utf-8", errors="replace")
                self.send_json_response({"success": False, "message": f"Gemini API Error ({he.code}): {err_text}"}, 502)
                return
            except Exception as e:
                self.send_json_response({"success": False, "message": f"AI service error: {e}"}, 500)
                return

        # ======================================================================
        # 3. ADMIN-ONLY OPERATIONAL MUTATIONS (Strict RBAC: Admin Role Required)
        # ======================================================================

        # 3.1 POST /api/roster (Save / Publish Roster)
        if clean_path in ("/api/roster", "/api/roster/"):
            claims = self.require_auth(["admin"])
            if not claims:
                return

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
                # Automatic recovery snapshot before overwrite
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
                        "published_by": claims.get("name") or "Admin",
                        "total_engineers": len(existing_roster.get("roster", existing_roster.get("engineers", []))),
                        "roster": snapshot_data
                    }
                    try:
                        ATLAS_DB.roster_backups.insert_one(backup_doc)
                    except Exception as b_err:
                        print(f"  [BACKUP WARNING] {b_err}")

                # Ensure admin and manager are never scheduled on shift desk
                if "engineers" in body and isinstance(body["engineers"], list):
                    body["engineers"] = [e for e in body["engineers"] if not is_management_account(e) and str(e.get("role", "")).lower() not in ("admin", "manager")]
                if "roster" in body and isinstance(body["roster"], list):
                    body["roster"] = [e for e in body["roster"] if not is_management_account(e) and str(e.get("role", "")).lower() not in ("admin", "manager")]
                body["updated_at"] = time.strftime("%Y-%m-%dT%H:%M:%SZ", time.gmtime())
                body["published_by"] = claims.get("name") or "Admin"
                ATLAS_DB.rosters.update_one({"_id": doc_id}, {"$set": body}, upsert=True)
                # Dynamically update preferred shifts across team based on this published roster
                recalculate_dynamic_preferred_shifts()
                self.send_json_response({
                    "success": True,
                    "message": f"Roster '{doc_id}' successfully saved and published to Atlas M0!",
                    "id": doc_id
                })
            except Exception as e:
                self.send_json_response({"success": False, "message": f"Failed to upsert roster to Atlas: {e}"}, 500)
            return

        # 3.2 POST /api/roster-backups/restore
        if clean_path in ("/api/roster-backups/restore", "/api/roster-backups/restore/"):
            claims = self.require_auth(["admin"])
            if not claims:
                return

            if ATLAS_DB is None:
                self.send_json_response({"success": False, "message": "Atlas database is not connected"}, 503)
                return

            backup_id = body.get("backup_id")
            month_id = body.get("month_id")
            if not backup_id:
                self.send_json_response({"success": False, "message": "Missing 'backup_id' in restore payload"}, 400)
                return

            try:
                backup_doc = ATLAS_DB.roster_backups.find_one({"backup_id": backup_id})
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

        # 3.3 POST /api/constraints (Update Operational Policies)
        if clean_path in ("/api/constraints", "/api/constraints/"):
            claims = self.require_auth(["admin"])
            if not claims:
                return

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
                    to_save["updated_by"] = claims.get("name") or "Admin"
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

        # 3.4 POST /api/engineers (Sync Team Directory)
        if clean_path in ("/api/engineers", "/api/engineers/"):
            claims = self.require_auth(["admin", "manager"])
            if not claims:
                return

            if ATLAS_DB is None:
                self.send_json_response({"success": False, "message": "Database is not connected"}, 503)
                return

            engineers = body if isinstance(body, list) else body.get("engineers", [])
            if not isinstance(engineers, list):
                self.send_json_response({"success": False, "message": "Expected list of engineers"}, 400)
                return

            try:
                saved_count = 0
                for eng in engineers:
                    eng_email = str(eng.get("email") or "").strip().lower()
                    eng_name = str(eng.get("name") or "").strip()
                    # Username is strictly derived from domain ID of engineer's email address
                    if eng_email and "@" in eng_email:
                        eng_user = eng_email.split("@")[0].strip().lower()
                    else:
                        eng_user = str(eng.get("username") or "").strip().lower()
                    if not eng_user and eng_name:
                        eng_user = eng_name.split()[0].lower()

                    if not eng_user:
                        continue

                    eng["username"] = eng_user
                    # Remove legacy id fields to keep username as the sole unique key
                    eng.pop("engineer_id", None)
                    eng.pop("id", None)

                    raw_status = str(eng.get("status") or "").strip().lower()
                    if raw_status in ("active", "notice_period", "onboarding"):
                        eng["status"] = "Active"
                    elif raw_status in ("resigned", "retired"):
                        eng["status"] = "Resigned"

                    # Never overwrite existing password hash with blank or plaintext
                    if "password" in eng and not str(eng["password"]).startswith("pbkdf2_sha256$"):
                        if str(eng["password"]).strip():
                            eng["password"] = hash_password(str(eng["password"]).strip())
                        else:
                            del eng["password"]

                    # Primary key is username
                    ATLAS_DB.engineers.update_one(
                        {"username": eng_user},
                        {"$set": eng, "$unset": {"id": "", "engineer_id": ""}},
                        upsert=True
                    )
                    saved_count += 1

                self.send_json_response({
                    "success": True,
                    "message": f"Successfully updated {saved_count} accounts.",
                    "count": saved_count
                })
            except Exception as e:
                self.send_json_response({"success": False, "message": f"Failed to sync accounts: {e}"}, 500)
            return

        # 3.5 POST /api/engineers/delete
        if clean_path in ("/api/engineers/delete", "/api/engineers/delete/"):
            claims = self.require_auth(["admin", "manager"])
            if not claims:
                return

            if ATLAS_DB is None:
                self.send_json_response({"success": False, "message": "Atlas database is not connected"}, 503)
                return

            eng_username = str(body.get("username", "")).strip().lower()
            eng_name = str(body.get("name", "")).strip()
            eng_id = str(body.get("id", "")).strip()

            if not eng_username and not eng_name and not eng_id:
                self.send_json_response({"success": False, "message": "Engineer username or name required for deletion"}, 400)
                return

            try:
                queries = []
                if eng_username:
                    queries.extend([{"username": eng_username}, {"username": {"$regex": f"^{re.escape(eng_username)}$", "$options": "i"}}])
                if eng_name:
                    queries.extend([{"name": eng_name}, {"name": {"$regex": f"^{re.escape(eng_name)}$", "$options": "i"}}])
                if eng_id:
                    queries.extend([{"id": eng_id}, {"engineer_id": eng_id}])

                delete_query = {"$or": queries}
                result = ATLAS_DB.engineers.delete_many(delete_query)

                global DEFAULT_SERVER_ENGINEERS
                DEFAULT_SERVER_ENGINEERS = [
                    e for e in DEFAULT_SERVER_ENGINEERS
                    if e.get("username") != eng_username and e.get("name") != eng_name
                ]

                self.send_json_response({
                    "success": True,
                    "message": f"Successfully deleted engineer from database.",
                    "deleted_count": result.deleted_count
                })
            except Exception as e:
                self.send_json_response({"success": False, "message": f"Failed to delete engineer: {e}"}, 500)
            return

                # 3.7 POST /api/notifications/dismiss (Persistent Notification Dismissal in Atlas DB)
        if clean_path in ("/api/notifications/dismiss", "/api/notifications/dismiss/"):
            claims = self.require_auth()
            if not claims:
                return

            username = str(claims.get("username", "")).strip().lower()
            notif_id = str(body.get("notification_id", "")).strip()
            dismiss_all = bool(body.get("all", False))

            if ATLAS_DB is not None and username:
                try:
                    if dismiss_all:
                        all_notifs = list(ATLAS_DB.notifications.find())
                        all_ids = []
                        for n in all_notifs:
                            nid = str(n.get("id") or n.get("_id") or "")
                            if nid:
                                all_ids.append(nid)
                        ATLAS_DB.engineers.update_one(
                            {"username": username},
                            {"$addToSet": {"dismissed_notifications": {"$each": all_ids}}}
                        )
                    elif notif_id:
                        ATLAS_DB.engineers.update_one(
                            {"username": username},
                            {"$addToSet": {"dismissed_notifications": notif_id}}
                        )
                except Exception as e:
                    print(f"  [DISMISS NOTIF ERROR] {e}")

            self.send_json_response({"success": True, "message": "Notification dismissed permanently."})
            return

        # 3.6 POST /api/notifications/send (Broadcast Messages)
        if clean_path in ("/api/notifications/send", "/api/notifications/send/"):
            claims = self.require_auth(["admin"])
            if not claims:
                return

            title = str(body.get("title", "Broadcast Notice")).strip()
            raw_message = str(body.get("message", "")).strip()
            target = body.get("target", "everyone")
            priority = str(body.get("priority", "info")).lower()
            sender = str(body.get("sender") or claims.get("name") or "System Administrator").strip()
            send_email = bool(body.get("send_email", False))
            send_in_app = bool(body.get("send_in_app", True))

            if not raw_message:
                self.send_json_response({"success": False, "message": "Message content cannot be blank."}, 400)
                return

            engineers = []
            if ATLAS_DB is not None:
                try:
                    engineers = list(ATLAS_DB.engineers.find({}, {"_id": 0}))
                except Exception as e:
                    print(f"  [NOTIF] DB engineers lookup error: {e}")
            if not engineers:
                engineers = list(DEFAULT_SERVER_ENGINEERS)

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

            emails_sent = 0
            for eng in recipients:
                if send_email and eng.get("email"):
                    to_email = eng.get("email")
                    html_email = f"""<!DOCTYPE html>
<html>
<body style="margin:0;padding:0;background-color:#0b1120;font-family:-apple-system,BlinkMacSystemFont,sans-serif;color:#f1f5f9;">
  <div style="max-width:560px;margin:30px auto;background:#0f172a;border:1px solid #1e293b;border-radius:16px;padding:30px;">
    <h2 style="margin:0 0 16px;color:#ffffff;font-size:20px;">{title}</h2>
    <div style="background:#020617;border:1px solid #334155;border-radius:10px;padding:18px;margin-bottom:20px;color:#e2e8f0;font-size:14px;line-height:1.6;white-space:pre-wrap;">
{raw_message}
    </div>
    <div style="font-size:12px;color:#64748b;border-top:1px solid #1e293b;padding-top:14px;">
      Sent by <strong>{sender}</strong> via RosterX Operations.
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

        # Unknown route
        self.send_json_response({
            "success": False,
            "message": f"Endpoint POST {clean_path} not found"
        }, 404)

    def end_headers(self):
        self.send_header("Cache-Control", "no-cache, no-store, must-revalidate")
        self.send_header("Pragma", "no-cache")
        self.send_header("Expires", "0")
        self.send_header("X-Content-Type-Options", "nosniff")
        self.send_header("X-Frame-Options", "DENY")
        self.send_header("X-XSS-Protection", "1; mode=block")
        super().end_headers()

    def list_directory(self, path):
        self.send_error(403, "Directory listing is disabled for security.")
        return None

    def log_message(self, format, *args):
        try:
            if len(args) >= 2 and isinstance(args[0], str) and (isinstance(args[1], (str, int)) or hasattr(args[1], "value")):
                method_path = str(args[0])
                raw_status = getattr(args[1], "value", args[1])
                status = str(raw_status)
            elif len(args) >= 1 and (isinstance(args[0], int) or hasattr(args[0], "value")):
                raw_status = getattr(args[0], "value", args[0])
                status = str(raw_status)
                method_path = str(args[1]) if len(args) > 1 and args[1] is not None else ""
            else:
                status = "INFO"
                method_path = (format % args) if args else str(format)

            if status in ("200", "204", "304"):
                color = "\033[92m"
            elif status in ("400", "401", "403", "404", "500", "502", "503"):
                color = "\033[91m"
            else:
                color = "\033[93m"
            reset = "\033[0m"
            print(f" {color}[HTTP {status}]{reset} {method_path}")

            if "/api/logs" not in method_path and "/api/server-logs" not in method_path:
                timestamp = time.strftime("%H:%M:%S")
                SERVER_LOGS.append(f"[{timestamp}] [HTTP {status}] {method_path}")
        except Exception:
            pass


# ==============================================================================
# Main Server Runner
# ==============================================================================

def run_server(port=8000, open_browser=False, enable_tunnel=False, alert_email=None, send_email=True):
    if not WEB_DIR.exists():
        print(f"[ERROR] 'admin-web' directory not found at {WEB_DIR}")
        sys.exit(1)

    lan_ip = get_lan_ip()
    env = load_env_variables()

    # Initialize PyMongo Atlas Bridge
    init_atlas_connection(env)

    SERVER_LOGS.append(f"[{time.strftime('%H:%M:%S')}] [SYSTEM] RosterX Server v{SERVER_VERSION} listening on port {port}")
    SERVER_LOGS.append(f"[{time.strftime('%H:%M:%S')}] [ATLAS DB] Status: {ATLAS_STATUS} ({ATLAS_CLUSTER}/{ATLAS_DB_NAME})")

    global RESEND_API_KEY, RESEND_FROM, DEFAULT_RESEND_API_KEY, DEFAULT_ALERT_EMAIL
    RESEND_API_KEY = env.get("RESEND_API_KEY", "").strip() or DEFAULT_RESEND_API_KEY
    RESEND_FROM = env.get("RESEND_FROM", "RosterX Operations <onboarding@resend.dev>").strip()

    mongo_uri = env.get("MONGODB_URI", "")
    cluster_name = env.get("ATLAS_CLUSTER", "Cluster0")
    db_name = env.get("ATLAS_DATABASE", "rosterx")
    gemini_key = env.get("GEMINI_API_KEY", "")
    gemini_model = env.get("GEMINI_MODEL", "gemini-2.5-flash")

    tunnel_proc = None
    public_url = None

    is_render = bool(os.environ.get("RENDER")) or bool(os.environ.get("RENDER_EXTERNAL_URL"))
    render_external_url = os.environ.get("RENDER_EXTERNAL_URL")
    if is_render and render_external_url:
        public_url = render_external_url.rstrip("/")
        PUBLIC_TUNNEL_URL = public_url
        print(f"\n  [RENDER CLOUD] Detected Render production host: {public_url}")

    if enable_tunnel and not is_render:
        print("\n  [CLOUDFLARE] Initializing secure quick tunnel to Cloudflare Edge...")
        tunnel_proc, public_url = start_cloudflared_tunnel(port=port)
        if public_url:
            SERVER_LOGS.append(f"[{time.strftime('%H:%M:%S')}] [GATEWAY] Public Tunnel active: {public_url}")
    elif is_render and public_url:
        SERVER_LOGS.append(f"[{time.strftime('%H:%M:%S')}] [RENDER CLOUD] Live Service URL: {public_url}")

    publish_server_endpoints(public_url, lan_ip, port)

    alert_target = alert_email or env.get("ALERT_EMAIL", DEFAULT_ALERT_EMAIL).strip() or DEFAULT_ALERT_EMAIL
    env_send_email = env.get("SEND_STARTUP_EMAIL", "true").strip().lower() in ("true", "1", "yes")
    should_send_email = send_email and env_send_email and bool(alert_target) and not is_render

    if should_send_email and public_url:
        t_email = threading.Thread(
            target=send_startup_link_email,
            args=(public_url, lan_ip, port, alert_target),
            daemon=True
        )
        t_email.start()

    border = "=" * 68
    sub_border = "-" * 68
    print("\n" + border)
    print(f"  🚀  ROSTERX v{SERVER_VERSION} — HARDENED API SERVER & ATLAS BRIDGE")
    print(border)
    print("  [MOBILE APP & INTERNET ACCESS]")
    if public_url:
        print(f"   ► 🌐 Worldwide Public URL   : \033[93m\033[1m{public_url}\033[0m")
        print(f"   ► 📱 Android App Base URL   : \033[92m\033[1m{public_url}/api/\033[0m  (Works on 4G/5G/any WiFi)")
    elif enable_tunnel:
        print("   ► 🌐 Worldwide Public URL   : \033[91mTunnel failed to start (check internet connection)\033[0m")
    else:
        print("   ► 🌐 Worldwide Public URL   : \033[90mDisabled (run with --tunnel or set ENABLE_TUNNEL=true)\033[0m")
    print(f"   ► 🏠 Local Wi-Fi (LAN)      : \033[96mhttp://{lan_ip}:{port}\033[0m")
    print(f"   ► 📱 Android LAN Base URL   : \033[96mhttp://{lan_ip}:{port}/api/\033[0m")
    print(f"   ► 💻 Local Web App          : \033[96mhttp://localhost:{port}\033[0m")
    print(f"   ► 🤖 Android Emulator       : \033[96mhttp://10.0.2.2:{port}/api/\033[0m")
    print(sub_border)
    print("  [SECURITY & AUTHENTICATION SPECIFICATIONS]")
    print(f"   ► Auth Architecture: \033[92mHMAC-SHA256 Signed JWT Tokens (RBAC Active)\033[0m")
    print(f"   ► Password Storage : \033[92mPBKDF2-HMAC-SHA256 (200k rounds + Salted)\033[0m")
    print(f"   ► OTP Verification : \033[92mCryptographic (secrets.randbelow) + In-DB SHA256 Hash\033[0m")
    print(f"   ► Brute Force Guard: \033[92mActive (Per-IP & Per-Account Rate Limiting)\033[0m")
    print(f"   ► AI Security Proxy: \033[92mServer-Side Gemini Gateway (/api/ai/*)\033[0m")
    print(sub_border)
    print("  [DATABASE & BACKEND SPECIFICATIONS]")
    print(f"   ► Engine Type   : MongoDB Atlas M0 (Native Python Driver)")
    print(f"   ► Cluster Name  : {cluster_name}")
    print(f"   ► Target DB     : {db_name}")
    print(f"   ► Connection URI: {mask_mongo_uri(mongo_uri)}")
    status_color = "\033[92m" if ATLAS_STATUS == "CONNECTED (Ready)" else "\033[91m"
    print(f"   ► Atlas Status  : {status_color}{ATLAS_STATUS}\033[0m")
    print(border)
    print(f"  Status: Server listening on all interfaces (0.0.0.0:{port}). Press Ctrl+C to stop.\n")

    server_address = ("0.0.0.0", port)
    try:
        httpd = ThreadingHTTPServer(server_address, SecureRosterHandler)
        httpd.daemon_threads = True
    except OSError as e:
        print(f"\n[ERROR] Port {port} is already in use!")
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
    parser.add_argument("--no-open", action="store_true", help="Disable opening browser on launch")
    parser.add_argument("--tunnel", "-t", action="store_true", default=None, help="Force launch Cloudflare Tunnel")
    parser.add_argument("--no-tunnel", action="store_true", help="Disable Cloudflare Tunnel")
    parser.add_argument("--email", type=str, default=None, help="Email address to receive live gateway link")
    parser.add_argument("--no-email", action="store_true", help="Disable sending startup gateway link email")
    args, _ = parser.parse_known_args()

    env_tunnel_val = env.get("ENABLE_TUNNEL", "true").strip().lower()
    auto_tunnel = (env_tunnel_val in ("true", "1", "yes")) if (find_cloudflared_path() and not is_render) else False

    if args.no_tunnel or is_render:
        enable_tunnel = False
    elif args.tunnel is not None:
        enable_tunnel = args.tunnel
    else:
        enable_tunnel = auto_tunnel

    should_send_email = not args.no_email and not is_render
    open_browser = args.open and not args.no_open and not is_render
    run_server(port=args.port, open_browser=open_browser, enable_tunnel=enable_tunnel, alert_email=args.email, send_email=should_send_email)
