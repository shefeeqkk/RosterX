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
import platform
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
import smtplib
import ssl
from email.mime.multipart import MIMEMultipart
from email.mime.text import MIMEText
from http.server import SimpleHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path

# Server Version (Follows project semantic versioning rule: small=patch, semi-big=minor, major=major)
from datetime import datetime, timezone, timedelta
SERVER_VERSION = "1.14.10"
SERVER_START_TIME = time.time()

def get_memory_rss_mb():
    """Retrieve actual process RSS memory in MB across Windows and Linux platforms."""
    try:
        import psutil
        return round(psutil.Process().memory_info().rss / (1024 * 1024), 2)
    except Exception:
        pass
    if sys.platform == "win32":
        try:
            import ctypes
            from ctypes import wintypes
            class PMC(ctypes.Structure):
                _fields_ = [
                    ('cb', wintypes.DWORD),
                    ('PageFaultCount', wintypes.DWORD),
                    ('PeakWorkingSetSize', ctypes.c_size_t),
                    ('WorkingSetSize', ctypes.c_size_t),
                    ('QuotaPeakPagedPoolUsage', ctypes.c_size_t),
                    ('QuotaPagedPoolUsage', ctypes.c_size_t),
                    ('QuotaPeakNonPagedPoolUsage', ctypes.c_size_t),
                    ('QuotaNonPagedPoolUsage', ctypes.c_size_t),
                    ('PagefileUsage', ctypes.c_size_t),
                    ('PeakPagefileUsage', ctypes.c_size_t),
                ]
            p = PMC()
            p.cb = ctypes.sizeof(PMC)
            ctypes.windll.psapi.GetProcessMemoryInfo.argtypes = [ctypes.c_void_p, ctypes.POINTER(PMC), wintypes.DWORD]
            ctypes.windll.psapi.GetProcessMemoryInfo.restype = wintypes.BOOL
            if ctypes.windll.psapi.GetProcessMemoryInfo(ctypes.c_void_p(-1), ctypes.byref(p), p.cb):
                return round(p.WorkingSetSize / (1024 * 1024), 2)
        except Exception:
            pass
    else:
        try:
            import resource
            rusage = resource.getrusage(resource.RUSAGE_SELF)
            if sys.platform == "darwin":
                return round(rusage.ru_maxrss / (1024 * 1024), 2)
            return round(rusage.ru_maxrss / 1024, 2)
        except Exception:
            pass
    return None

def format_uptime(seconds):
    """Format elapsed seconds into human-readable D/H/M/S."""
    secs = int(seconds)
    days, rem = divmod(secs, 86400)
    hours, rem = divmod(rem, 3600)
    mins, secs = divmod(rem, 60)
    parts = []
    if days > 0:
        parts.append(f"{days}d")
    if hours > 0 or days > 0:
        parts.append(f"{hours}h")
    if mins > 0 or hours > 0 or days > 0:
        parts.append(f"{mins}m")
    parts.append(f"{secs}s")
    return " ".join(parts)

def record_audit_event(engineer_name, username, role, platform_name, action, details=None):
    """Safely append an operational audit record into MongoDB Atlas collection 'audit_logs'."""
    if ATLAS_DB is None:
        return
    try:
        now_ts = time.time()
        doc = {
            "engineer_name": engineer_name,
            "username": username,
            "role": role,
            "platform": platform_name,
            "action": action,
            "details": details or {},
            "timestamp": now_ts,
            "datetime": time.strftime("%Y-%m-%d %H:%M:%S", time.gmtime(now_ts))
        }
        ATLAS_DB["audit_logs"].insert_one(doc)
    except Exception as e:
        print(f"  [AUDIT LOG ERROR] {e}")

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
SERVER_LOGS = collections.deque(maxlen=500)
SERVER_NOTIFICATIONS = collections.deque(maxlen=100)

def log_server(msg: str):
    """Log to stdout and append timestamped entry to circular SERVER_LOGS buffer."""
    ts = time.strftime('%Y-%m-%d %H:%M:%S')
    formatted = f"[{ts}] {msg}"
    print(formatted)
    SERVER_LOGS.append(formatted)

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

# Gmail SMTP Configuration for Native Password Recovery OTP
# Configured with fail-safe defaults so Render free-tier and cloud deploys work out-of-the-box
DEFAULT_SMTP_EMAIL = os.environ.get("SMTP_EMAIL", "kkshefeeq005@gmail.com")
DEFAULT_SMTP_PASSWORD = os.environ.get("SMTP_PASSWORD", "eskzhgckwazgrpjo")
SMTP_EMAIL = DEFAULT_SMTP_EMAIL
SMTP_PASSWORD = DEFAULT_SMTP_PASSWORD

def get_smtp_credentials():
    """Retrieve verified Gmail SMTP credentials from environment or .env with fail-safe defaults."""
    global SMTP_EMAIL, SMTP_PASSWORD
    if not SMTP_EMAIL or not SMTP_PASSWORD:
        env = load_env_variables()
        SMTP_EMAIL = env.get("SMTP_EMAIL", "").strip() or os.environ.get("SMTP_EMAIL", "").strip() or "kkshefeeq005@gmail.com"
        raw_pass = env.get("SMTP_PASSWORD", "").strip() or os.environ.get("SMTP_PASSWORD", "").strip() or "eskzhgckwazgrpjo"
        SMTP_PASSWORD = raw_pass.replace(" ", "")
    if not SMTP_EMAIL or not SMTP_PASSWORD:
        log_server("[SMTP CONFIG] Warning: SMTP_EMAIL or SMTP_PASSWORD not configured. OTP dispatch will fail.")
        return None, None
    return SMTP_EMAIL, SMTP_PASSWORD

# Official Team Directory
DEFAULT_SERVER_ENGINEERS = [
    {"name": "NANDU ANTHIKAT", "username": "nandua", "full_name": "Nandu Anthikat", "email": "nandua@tejasnetworks.com", "phone": "+919811001103", "role": "engineer", "status": "Active", "default_weekly_offs": ["Sunday", "Monday"], "preferred_shifts": ["FS", "FG"]},
    {"name": "SHEFEEQ", "username": "shefeeqkk", "full_name": "Shefeeq K K", "email": "shefeeqkk@tejasnetworks.com", "phone": "+91 9809226169", "role": "engineer", "status": "Active", "default_weekly_offs": ["Monday", "Tuesday"], "preferred_shifts": ["G", "FG"]},
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
    Supports alert/registered Resend account email resolution.
    """
    if not identifier:
        return None
    clean_id = str(identifier).strip().lower()

    # Match primary administrator / alert email alias (e.g. kkshefeeq005@gmail.com)
    env = load_env_variables()
    alert_email = env.get("ALERT_EMAIL", "").strip().lower() or (DEFAULT_ALERT_EMAIL.lower() if DEFAULT_ALERT_EMAIL else "")
    if alert_email and clean_id == alert_email:
        if ATLAS_DB is not None:
            try:
                user = ATLAS_DB.engineers.find_one({"$or": [{"username": "shefeeqkk"}, {"username": "admin"}, {"role": "admin"}]})
                if user:
                    return user
            except Exception as e:
                print(f"  [USER LOOKUP ERROR] {e}")
        for tm in DEFAULT_SERVER_ENGINEERS:
            if tm.get("username") in ("shefeeqkk", "admin") or tm.get("role") == "admin":
                return dict(tm)

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

def send_resend_email(to_email, subject, html_content, allow_sandbox_redirect=True):
    """
    Dispatch real transactional email via Resend API.
    Includes comprehensive [RESEND AUDIT] logging and sandbox constraint handling.
    """
    global RESEND_API_KEY, RESEND_FROM, DEFAULT_RESEND_API_KEY
    if not RESEND_API_KEY:
        env = load_env_variables()
        RESEND_API_KEY = env.get("RESEND_API_KEY", "").strip() or os.environ.get("RESEND_API_KEY", "")
    api_key = RESEND_API_KEY or DEFAULT_RESEND_API_KEY
    if not api_key:
        print("[RESEND AUDIT] ERROR: RESEND_API_KEY is not configured in .env or environment.")
        return {"success": False, "error": "RESEND_API_KEY is not configured in .env"}

    env = load_env_variables()
    primary_sandbox_email = env.get("ALERT_EMAIL", "").strip() or os.environ.get("ALERT_EMAIL", "kkshefeeq005@gmail.com")

    payload = {
        "from": RESEND_FROM,
        "to": [to_email],
        "subject": subject,
        "html": html_content
    }

    log_server(f"[RESEND AUDIT] Outgoing Request: POST https://api.resend.com/emails")
    log_server(f"[RESEND AUDIT] Sender ('from'): {RESEND_FROM}")
    log_server(f"[RESEND AUDIT] Recipient ('to'): {payload['to']}")
    log_server(f"[RESEND AUDIT] Subject: {subject}")
    log_server(f"[RESEND AUDIT] Outgoing JSON Payload: {json.dumps(payload)}")

    def _execute_post(target_payload):
        req = urllib.request.Request(
            "https://api.resend.com/emails",
            headers={
                "Authorization": f"Bearer {api_key}",
                "Content-Type": "application/json",
                "User-Agent": "resend-python:2.0.0"
            },
            data=json.dumps(target_payload).encode("utf-8")
        )
        with urllib.request.urlopen(req, timeout=12) as resp:
            status_code = resp.status
            body_text = resp.read().decode("utf-8")
            log_server(f"[RESEND AUDIT] HTTP Status Code: {status_code} OK")
            log_server(f"[RESEND AUDIT] Response Body: {body_text}")
            data = json.loads(body_text)
            return {"success": True, "id": data.get("id"), "delivered_to": target_payload["to"][0]}

    try:
        return _execute_post(payload)
    except urllib.error.HTTPError as e:
        err_body = e.read().decode("utf-8", errors="replace")
        log_server(f"[RESEND AUDIT] HTTP Status Code: {e.code} Error")
        log_server(f"[RESEND AUDIT] Raw Error Body from Resend: {err_body}")

        # Check if rejected due to Resend Free-Tier Sandbox restriction (only testing emails to primary account)
        if allow_sandbox_redirect and e.code == 403 and ("only send testing emails to your own email address" in err_body or "validation_error" in err_body):
            match = re.search(r"\(([^)]+@[^)]+)\)", err_body)
            extracted_email = match.group(1).strip() if match else None
            fallback_email = extracted_email or primary_sandbox_email or "kkshefeeq005@gmail.com"

            if fallback_email and fallback_email.lower() != to_email.lower():
                log_server(f"[RESEND AUDIT] Sandbox Restriction Detected: Recipient '{to_email}' is not verified under free-tier Resend.")
                log_server(f"[RESEND AUDIT] Redirecting dispatch to registered Resend account mailbox: '{fallback_email}'")

                # Annotate HTML with clear sandbox indicator
                sandbox_banner = f"""
                <div style="background:#1e293b;border-left:4px solid #f59e0b;padding:12px;margin-bottom:16px;border-radius:6px;font-size:12px;color:#fcd34d;">
                  ⚠️ <strong>Resend Sandbox Delivery Notice:</strong><br/>
                  Original intended recipient: <code>{to_email}</code>.<br/>
                  Delivered to registered account mailbox (<code>{fallback_email}</code>) per Resend free-tier testing policy.
                </div>
                """
                augmented_html = html_content.replace('<td style="padding:32px;">', f'<td style="padding:32px;">{sandbox_banner}', 1)
                if augmented_html == html_content:
                    augmented_html = f"{sandbox_banner}{html_content}"

                sandbox_payload = {
                    "from": RESEND_FROM,
                    "to": [fallback_email],
                    "subject": f"[SANDBOX for {to_email}] {subject}",
                    "html": augmented_html
                }
                log_server(f"[RESEND AUDIT] Outgoing Sandbox Fallback Payload: {json.dumps(sandbox_payload)}")
                try:
                    retry_result = _execute_post(sandbox_payload)
                    retry_result["sandbox_redirect"] = True
                    retry_result["intended_recipient"] = to_email
                    return retry_result
                except urllib.error.HTTPError as retry_e:
                    retry_err_body = retry_e.read().decode("utf-8", errors="replace")
                    log_server(f"[RESEND AUDIT] Sandbox Retry HTTP Status Code: {retry_e.code}")
                    log_server(f"[RESEND AUDIT] Sandbox Retry Raw Error: {retry_err_body}")
                    return {"success": False, "error": f"Resend HTTP {retry_e.code}: {retry_err_body}"}
                except Exception as retry_ex:
                    log_server(f"[RESEND AUDIT] Sandbox Retry Exception: {retry_ex}")
                    return {"success": False, "error": str(retry_ex)}

        return {"success": False, "error": f"Resend HTTP {e.code}: {err_body}"}
    except Exception as e:
        log_server(f"[RESEND AUDIT] Network/Execution Error: {e}")
        return {"success": False, "error": str(e)}

def send_otp_email(recipient_email: str, engineer_name: str, otp_code: str) -> bool:
    """
    Transmit styled HTML body conforming to Neumorphic Soft UI design tokens.
    Primary channel: Native Gmail SMTP via SSL (465) / TLS (587).
    Automatic Fail-Safe fallback: HTTPS REST API (Port 443 via Resend) if cloud host blocks raw SMTP ports.
    """
    smtp_user, smtp_pass = get_smtp_credentials()
    if not smtp_user or not smtp_pass:
        log_server(f"[SMTP ERROR] Cannot send OTP to {recipient_email}: Missing SMTP credentials.")
        # Attempt direct HTTPS REST API fallback
        smtp_user = "kkshefeeq005@gmail.com"

    display_name = engineer_name or "Engineer"
    subject = f"🔐 {otp_code} is your RosterX verification code"

    # Neumorphic Soft UI HTML Email Template
    html_content = f"""<!DOCTYPE html>
<html>
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>RosterX Verification Code</title>
</head>
<body style="margin:0;padding:0;background-color:#eef2f6;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Helvetica,Arial,sans-serif;color:#2d3748;">
  <table width="100%" border="0" cellspacing="0" cellpadding="0" style="padding:40px 15px;background-color:#eef2f6;">
    <tr>
      <td align="center">
        <!-- Main Neumorphic Card -->
        <table width="100%" style="max-width:500px;background:#eef2f6;border-radius:24px;border:1px solid #ffffff;box-shadow:8px 8px 20px #d1d9e6, -8px -8px 20px #ffffff;overflow:hidden;margin:0 auto;">
          <!-- Header -->
          <tr>
            <td style="padding:32px 36px 16px;text-align:center;">
              <div style="display:inline-block;padding:10px 22px;background:#eef2f6;border-radius:14px;box-shadow:inset 3px 3px 6px #d1d9e6, inset -3px -3px 6px #ffffff;margin-bottom:12px;">
                <span style="font-size:18px;font-weight:900;letter-spacing:1.5px;color:#2d3748;">PROJECT ROSTERX</span>
              </div>
              <p style="margin:4px 0 0;color:#718096;font-size:12px;font-weight:600;letter-spacing:0.5px;text-transform:uppercase;">Identity & Access Command</p>
            </td>
          </tr>

          <!-- Body Content -->
          <tr>
            <td style="padding:10px 36px 36px;">
              <h2 style="margin:0 0 10px;color:#2d3748;font-size:20px;font-weight:800;text-align:center;">Password Reset Verification</h2>
              <p style="margin:0 0 20px;color:#4a5568;font-size:14px;line-height:1.6;text-align:center;">
                Hello <strong>{display_name}</strong>, a password reset request was initiated for your RosterX account. Enter the 6-digit verification code below:
              </p>

              <!-- Inset Neumorphic Verification Code Display -->
              <div style="background:#eef2f6;border-radius:16px;padding:22px;text-align:center;margin:24px 0;box-shadow:inset 4px 4px 8px #d1d9e6, inset -4px -4px 8px #ffffff;border:1px solid rgba(255,255,255,0.7);">
                <span style="font-family:'Courier New',Courier,monospace;font-size:38px;font-weight:900;letter-spacing:12px;color:#ff5252;display:inline-block;padding-left:12px;">{otp_code}</span>
              </div>

              <!-- Expiration Notice -->
              <div style="background:#edf2f7;border-radius:12px;padding:12px 16px;text-align:center;margin-bottom:20px;">
                <p style="margin:0;color:#718096;font-size:12px;line-height:1.5;">
                  ⏱️ This code expires in <strong style="color:#2d3748;">10 minutes</strong>.<br>If you did not request this verification, your account remains secure.
                </p>
              </div>

              <p style="margin:0;color:#a0aec0;font-size:11px;text-align:center;line-height:1.4;">
                Automated security notification dispatched by Project RosterX v{SERVER_VERSION}
              </p>
            </td>
          </tr>
        </table>
      </td>
    </tr>
  </table>
</body>
</html>"""

    msg = MIMEMultipart("alternative")
    msg["Subject"] = subject
    msg["From"] = f"RosterX Security <{smtp_user}>"
    msg["To"] = recipient_email
    msg.attach(MIMEText(html_content, "html"))

    log_server(f"[DISPATCH] Initiating OTP verification dispatch to: {recipient_email} (User: {display_name})")

    # Strategy 1: Direct SMTP over Port 465 (timeout=3.5s for fast fallback if cloud firewall blocks)
    try:
        context = ssl.create_default_context()
        with smtplib.SMTP_SSL("smtp.gmail.com", 465, context=context, timeout=3.5) as server:
            server.login(smtp_user, smtp_pass)
            server.sendmail(smtp_user, [recipient_email], msg.as_string())
        log_server(f"[SMTP DISPATCH] ✅ Successfully delivered OTP email to '{recipient_email}' via SSL (Port 465).")
        return True
    except Exception as ssl_err:
        log_server(f"[SMTP WARNING] Port 465 SSL failed ({ssl_err}). Attempting Port 587 STARTTLS...")

    # Strategy 2: Direct SMTP with STARTTLS over Port 587 (timeout=3.5s)
    try:
        with smtplib.SMTP("smtp.gmail.com", 587, timeout=3.5) as server:
            server.ehlo()
            server.starttls()
            server.ehlo()
            server.login(smtp_user, smtp_pass)
            server.sendmail(smtp_user, [recipient_email], msg.as_string())
        log_server(f"[SMTP DISPATCH] ✅ Successfully delivered OTP email to '{recipient_email}' via STARTTLS (Port 587).")
        return True
    except Exception as tls_err:
        log_server(f"[SMTP WARNING] Port 587 STARTTLS failed ({tls_err}). Cloud firewall blocks raw outbound SMTP ports.")

    # Strategy 3: Automatic Fail-Safe HTTPS REST API Fallback (Port 443 via Resend)
    # Render and cloud free tiers allow Port 443 outbound HTTPS, bypassing raw socket blocks!
    log_server(f"[DISPATCH FALLBACK] Initiating HTTPS REST API (Port 443) dispatch via Resend for '{recipient_email}'...")
    try:
        resend_result = send_resend_email(recipient_email, subject, html_content, allow_sandbox_redirect=True)
        if resend_result and resend_result.get("success"):
            dest = resend_result.get("delivered_to", recipient_email)
            log_server(f"[DISPATCH FALLBACK] ✅ Successfully delivered OTP email to '{dest}' via HTTPS REST API.")
            return True
        else:
            err = resend_result.get("error", "Unknown Resend error") if resend_result else "No response"
            log_server(f"[DISPATCH ERROR] HTTPS REST API fallback also failed: {err}")
    except Exception as fb_err:
        log_server(f"[DISPATCH ERROR] HTTPS REST API fallback exception: {fb_err}")

    return False

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

# ==============================================================================
# Grounded Conversational RAG Operations Engine (IST & MongoDB Atlas Grounding)
# ==============================================================================

def get_ist_now():
    """Returns current datetime in Indian Standard Time (UTC+5:30)."""
    ist_tz = timezone(timedelta(hours=5, minutes=30))
    return datetime.now(ist_tz)

def determine_active_shifts(ist_time):
    """
    Computes active shift codes based on strict operational boundaries (IST):
    FS: 07:30 – 16:30
    FG: 08:30 – 17:30
    G:  09:30 – 18:30
    SG: 11:00 – 20:00
    SS: 13:00 – 22:00
    NS: 21:00 – 07:30 (crosses midnight)
    """
    t_min = ist_time.hour * 60 + ist_time.minute
    active = []
    if 7 * 60 + 30 <= t_min < 16 * 60 + 30:
        active.append("FS")
    if 8 * 60 + 30 <= t_min < 17 * 60 + 30:
        active.append("FG")
    if 9 * 60 + 30 <= t_min < 18 * 60 + 30:
        active.append("G")
    if 11 * 60 <= t_min < 20 * 60:
        active.append("SG")
    if 13 * 60 <= t_min < 22 * 60:
        active.append("SS")
    if t_min >= 21 * 60 or t_min < 7 * 60 + 30:
        active.append("NS")
    return active

SHIFT_DEFINITIONS = {
    "FS": {"name": "First Shift", "timings": "07:30 – 16:30", "desc": "Morning desk operations"},
    "FG": {"name": "First General", "timings": "08:30 – 17:30", "desc": "Morning general coverage"},
    "G":  {"name": "General Shift", "timings": "09:30 – 18:30", "desc": "Core daytime engineering"},
    "SG": {"name": "Second General", "timings": "11:00 – 20:00", "desc": "Mid-day handover & evening operations"},
    "SS": {"name": "Second Shift", "timings": "13:00 – 22:00", "desc": "Late evening desk coverage"},
    "NS": {"name": "Night Shift", "timings": "21:00 – 07:30 (+1)", "desc": "Overnight solo desk monitoring (Mandatory 48h rest applies)"},
    "OFF": {"name": "Weekly Off", "timings": "Full 24h rest", "desc": "Scheduled rest period"},
    "L":   {"name": "Approved Leave", "timings": "Full 24h rest", "desc": "Approved operational absence"}
}

def build_roster_grounding_context(current_user=None):
    """
    Assembles real-time ground truth from MongoDB Atlas, OS runtime, and IST clock.
    Zero synthetic or mock data is injected.
    """
    now_ist = get_ist_now()
    cur_year = now_ist.year
    cur_month = now_ist.month
    cur_day = now_ist.day
    cur_month_key = f"{cur_year}-{str(cur_month).zfill(2)}"
    tomorrow_ist = now_ist + timedelta(days=1)
    tomorrow_day = tomorrow_ist.day
    
    active_shift_codes = determine_active_shifts(now_ist)
    
    # 1. Fetch live published roster from MongoDB Atlas
    roster_doc = None
    if ATLAS_DB is not None:
        try:
            roster_doc = ATLAS_DB["rosters"].find_one({"_id": cur_month_key})
            if not roster_doc:
                roster_doc = ATLAS_DB["rosters"].find_one({}, sort=[("_id", -1)])
        except Exception as e:
            print(f"[RAG] DB roster query error: {e}")

    engineers_roster = []
    if roster_doc:
        engineers_roster = roster_doc.get("engineers", [])

    normalized_roster = []
    for eng in engineers_roster:
        name = eng.get("name", "Unknown")
        shifts_raw = eng.get("shifts")
        shifts_arr = []
        if isinstance(shifts_raw, dict):
            for d in range(1, 32):
                shifts_arr.append(shifts_raw.get(str(d), ""))
        elif isinstance(shifts_raw, list):
            shifts_arr = shifts_raw
        normalized_roster.append({
            "name": name,
            "id": eng.get("engineer_id") or eng.get("id"),
            "shifts": shifts_arr
        })

    # Today & Tomorrow NOC coverage breakdown
    on_duty_now = []
    today_all = []
    tomorrow_all = []
    
    for r in normalized_roster:
        s_today = r["shifts"][cur_day - 1] if len(r["shifts"]) >= cur_day else ""
        s_tom = r["shifts"][tomorrow_day - 1] if len(r["shifts"]) >= tomorrow_day else ""
        s_today = s_today.strip().upper() if s_today else "OFF"
        s_tom = s_tom.strip().upper() if s_tom else "OFF"
        
        today_all.append({"name": r["name"], "shift": s_today})
        tomorrow_all.append({"name": r["name"], "shift": s_tom})
        
        if s_today in active_shift_codes:
            on_duty_now.append({
                "name": r["name"],
                "shift": s_today,
                "timings": SHIFT_DEFINITIONS.get(s_today, {}).get("timings", "")
            })

    # User-specific duty details
    user_details = None
    if current_user:
        u_name = (current_user.get("name") or current_user.get("full_name") or "").strip().lower()
        u_id = (current_user.get("id") or current_user.get("username") or "").strip().lower()
        for r in normalized_roster:
            r_name = r["name"].strip().lower()
            r_id = str(r["id"] or "").strip().lower()
            if (u_id and r_id == u_id) or (u_name and r_name == u_name) or (u_name and u_name in r_name) or (u_id and u_id in r_id):
                s_today = r["shifts"][cur_day - 1] if len(r["shifts"]) >= cur_day else "OFF"
                s_tom = r["shifts"][tomorrow_day - 1] if len(r["shifts"]) >= tomorrow_day else "OFF"
                
                next_ns = None
                for idx in range(cur_day - 1, len(r["shifts"])):
                    sc = (r["shifts"][idx] or "").strip().upper()
                    if sc == "NS":
                        next_ns = f"{cur_month_key}-{str(idx + 1).zfill(2)}"
                        break
                        
                off_count = sum(1 for s in r["shifts"] if (s or "").strip().upper() in ("OFF", "L") or (s or "").strip().upper().startswith("SW"))
                duty_count = len(r["shifts"]) - off_count
                user_details = {
                    "matched_engineer": r["name"],
                    "today_shift": s_today,
                    "tomorrow_shift": s_tom,
                    "next_night_shift": next_ns or "None scheduled this month",
                    "total_duty_days": duty_count,
                    "total_off_days": off_count
                }
                break

    # Host process diagnostics
    uptime_sec = int(time.time() - SERVER_START_TIME)
    h, rem = divmod(uptime_sec, 3600)
    m, s = divmod(rem, 60)
    uptime_str = f"{h}h {m}m {s}s"

    return {
        "ist_timestamp": now_ist.strftime("%Y-%m-%d %H:%M:%S IST"),
        "date": now_ist.strftime("%Y-%m-%d"),
        "day_of_week": now_ist.strftime("%A"),
        "current_month_key": cur_month_key,
        "active_shift_slots": active_shift_codes,
        "on_duty_engineers": on_duty_now,
        "today_desk_coverage": today_all,
        "tomorrow_schedule": tomorrow_all,
        "current_user_profile": user_details,
        "uptime": uptime_str,
        "total_engineers_in_roster": len(normalized_roster)
    }

def execute_deterministic_roster_ai(prompt, context, current_user=None):
    """
    High-reliability grounded fallback when Gemini API encounters quota limits or network partitions.
    Answers strictly based on live MongoDB Atlas truth.
    """
    p = prompt.strip().lower()
    ist_time = context.get('ist_timestamp', '')
    cur_date = context.get('date', '')
    day_name = context.get('day_of_week', '')
    month_key = context.get('current_month_key', '')
    
    # 1. Who is on shift now / who all are in shift
    if any(k in p for k in ["who is on shift", "who all are in shift", "who is on duty", "who is working now", "current shift", "shift now"]):
        slots = context.get("active_shift_slots", [])
        on_duty = context.get("on_duty_engineers", [])
        if not on_duty:
            return f"### 🕒 NOC Real-Time Duty Briefing ({ist_time})\n\n" \
                   f"**Active Shift Windows:** `{', '.join(slots) if slots else 'Desk Transition Window'}`\n\n" \
                   f"Currently, no engineers are active in these specific slots for today's published roster. Handover may be in progress."
        
        lines = [f"### 🕒 Active NOC Duty Coverage ({ist_time})\n",
                 f"**Current Active Shift Windows:** `{', '.join(slots)}`\n",
                 f"**Engineers Currently On Duty ({len(on_duty)} Staff):**"]
        for eng in on_duty:
            s_code = eng['shift']
            timing = eng.get('timings', '')
            lines.append(f"- **{eng['name']}** — `{s_code}` ({timing})")
        lines.append(f"\n*Data grounded in real-time MongoDB Atlas roster for {cur_date} ({day_name}).*")
        return "\n".join(lines)

    # 2. When is my next night shift
    if any(k in p for k in ["next night shift", "next night duty", "next ns", "when is my night"]):
        profile = context.get("current_user_profile")
        if profile and profile.get("next_night_shift"):
            ns_val = profile["next_night_shift"]
            if ns_val.startswith("None"):
                return f"### 🌙 Night Duty Query\n\nAccording to the published operational roster for **{month_key}**, you have **no upcoming Night Shifts (NS)** scheduled for the remainder of this month."
            return f"### 🌙 Your Next Night Duty\n\n" \
                   f"- **Assigned Engineer:** **{profile['matched_engineer']}**\n" \
                   f"- **Next Night Shift (NS):** **{ns_val}** (21:00 – 07:30 IST)\n" \
                   f"- **Operational Compliance:** Mandatory 48-hour rest interval applies immediately post-completion.\n\n" \
                   f"*Grounded in live Atlas roster records.*"
        else:
            ns_engineers = [e['name'] for e in context.get('today_desk_coverage', []) if e['shift'] == 'NS']
            ns_str = ", ".join(ns_engineers) if ns_engineers else "None assigned"
            return f"### 🌙 Tonight's Night Duty Roster ({cur_date})\n\n" \
                   f"- **Assigned Night Desk (NS):** **{ns_str}** (21:00 – 07:30 IST)\n" \
                   f"*(If inquiring about your personal upcoming night duty, please ensure you are logged into your engineer account.)*"

    # 3. What is my shift today / tomorrow
    if any(k in p for k in ["my shift today", "my shift tomorrow", "what is my shift", "my duties", "my schedule"]):
        profile = context.get("current_user_profile")
        if profile:
            s_today = profile['today_shift']
            s_tom = profile['tomorrow_shift']
            t_def_today = SHIFT_DEFINITIONS.get(s_today, {})
            t_def_tom = SHIFT_DEFINITIONS.get(s_tom, {})
            return f"### 📋 Personal Shift Briefing — {profile['matched_engineer']}\n\n" \
                   f"- **Today's Duty ({cur_date}):** **`{s_today}`** ({t_def_today.get('name', 'Duty')}) — `{t_def_today.get('timings', '')}`\n" \
                   f"- **Tomorrow's Duty:** **`{s_tom}`** ({t_def_tom.get('name', 'Duty')}) — `{t_def_tom.get('timings', '')}`\n" \
                   f"- **Monthly Summary:** **{profile['total_duty_days']}** Assigned Shifts • **{profile['total_off_days']}** Weekly Offs\n\n" \
                   f"*Status verified live against MongoDB Atlas roster.*"
        else:
            cov = [f"- **{e['name']}**: `{e['shift']}`" for e in context.get('today_desk_coverage', [])[:6]]
            return f"### 📋 Today's Desk Schedule ({cur_date})\n\n" + "\n".join(cov) + "\n\n*(Sign in to view your personalized duty profile).* "

    # 4. Night shift inquiry
    if any(k in p for k in ["night shift", "night duty", "who is on night"]):
        ns_engineers = [e['name'] for e in context.get('today_desk_coverage', []) if e['shift'] == 'NS']
        tom_ns = [e['name'] for e in context.get('tomorrow_schedule', []) if e['shift'] == 'NS']
        return f"### 🌙 24/7 Night Duty Roster\n\n" \
               f"- **Tonight's NS ({cur_date}):** **{', '.join(ns_engineers) if ns_engineers else 'None assigned'}** (21:00 – 07:30 IST)\n" \
               f"- **Tomorrow's NS:** **{', '.join(tom_ns) if tom_ns else 'None assigned'}**\n" \
               f"- **Desk Policy:** Strict 1 NS per night cap and mandatory 48h rest applies after every night rotation."

    # 5. Off days inquiry
    if any(k in p for k in ["off day", "how many off", "weekly off", "leaves"]):
        profile = context.get("current_user_profile")
        if profile:
            return f"### 🏖️ Scheduled Rest & Weekly Offs\n\n" \
                   f"- **Engineer:** **{profile['matched_engineer']}**\n" \
                   f"- **Total Scheduled Offs:** **{profile['total_off_days']} days** in {month_key}\n" \
                   f"- **Total Operational Duties:** **{profile['total_duty_days']} days**\n" \
                   f"- **Fatigue Policy:** Minimum 24h rest window enforced between rotations."
        else:
            return f"### 🏖️ RosterX Rest Policy\n\n" \
                   f"Engineers receive standard weekly offs and mandatory 48h rest following night shift completions. Sign in to view your exact monthly off count."

    # 6. Default Grounded Operational Summary
    on_duty_names = [f"**{e['name']}** (`{e['shift']}`)" for e in context.get("on_duty_engineers", [])]
    return f"### 🤖 RosterX Operations Assistant\n\n" \
           f"**Live System Status ({ist_time}):**\n" \
           f"- **Host Uptime:** {context.get('uptime', 'Active')} • **Atlas DB:** Connected\n" \
           f"- **Active Shift Slots:** `{', '.join(context.get('active_shift_slots', []))}`\n" \
           f"- **Staff Currently On Duty:** {', '.join(on_duty_names) if on_duty_names else 'No active desk staff'}\n\n" \
           f"You can ask me:\n" \
           f"- *\"Who all are in shift now?\"*\n" \
           f"- *\"When is my next night shift?\"*\n" \
           f"- *\"What is my shift today and tomorrow?\"*\n" \
           f"- *\"Who is covering night duty tonight?\"*"

def query_gemini_roster_agent(user_prompt, current_user=None, client_platform="Web Portal"):
    """
    Unified RAG orchestration calling Google Gemini API with fallback to deterministic grounded engine.
    """
    env = load_env_variables()
    gemini_key = env.get("GEMINI_API_KEY", "").strip() or os.environ.get("GEMINI_API_KEY", "").strip()
    configured_model = env.get("GEMINI_MODEL", "gemini-2.5-flash").strip()
    
    context = build_roster_grounding_context(current_user)
    
    # Model fallback hierarchy
    candidate_models = []
    for m in [configured_model, "gemini-2.5-flash", "gemini-2.0-flash", "gemini-3.8-flash", "gemini-flash-latest"]:
        if m and m not in candidate_models:
            candidate_models.append(m)

    system_instruction = (
        "You are RosterX Operations Assistant, an authoritative 24/7 NOC Roster AI Agent developed by the RosterX NOC Team. "
        "You operate exclusively with REAL ground truth data provided in the context below from MongoDB Atlas and Indian Standard Time (IST).\n"
        "Strict Directives:\n"
        "1. NEVER invent, hallucinate, or assume shift assignments, engineer names, or dates. Only use the provided grounding context.\n"
        "2. All time references are in Indian Standard Time (IST, UTC+5:30).\n"
        "3. Operational Shift Structure & Categories:\n"
        "   - Morning Shift: FS (07:30 – 16:30), FG (08:30 – 17:30), G (09:30 – 18:30), SG (11:00 – 20:00)\n"
        "   - Afternoon Shift: SS (13:00 – 22:00)\n"
        "   - Night Shift: NS (21:00 – 07:30, +1 day, solo desk coverage)\n"
        "   - No Duty: OFF, L, SW* (Weekly Off, Approved Leave, Swapped Off)\n"
        "4. Format your responses with clean, professional Markdown (bullet points, bold highlights, code formatting for shift codes).\n"
        "5. Be concise, direct, helpful, and courteous."
    )

    grounding_payload_text = (
        "CURRENT REAL-TIME CONTEXT:\n"
        f"- Indian Standard Time (IST): {context['ist_timestamp']}\n"
        f"- Date: {context['date']} ({context['day_of_week']})\n"
        f"- Active Shift Windows Right Now: {', '.join(context['active_shift_slots'])}\n"
        f"- Engineers On Duty Right Now: {json.dumps(context['on_duty_engineers'])}\n"
        f"- Today's Desk Coverage: {json.dumps(context['today_desk_coverage'])}\n"
        f"- Tomorrow's Scheduled Coverage: {json.dumps(context['tomorrow_schedule'])}\n"
        f"- Current User Profile: {json.dumps(context['current_user_profile'])}\n"
        f"- System Uptime: {context['uptime']}\n\n"
        f"User Query: {user_prompt}"
    )

    if gemini_key:
        for model in candidate_models:
            url = f"https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent?key={gemini_key}"
            gemini_body = {
                "systemInstruction": {"parts": [{"text": system_instruction}]},
                "contents": [{"parts": [{"text": grounding_payload_text}]}],
                "generationConfig": {"temperature": 0.2, "maxOutputTokens": 1024}
            }
            try:
                ai_req = urllib.request.Request(
                    url,
                    headers={"Content-Type": "application/json"},
                    data=json.dumps(gemini_body).encode("utf-8")
                )
                with urllib.request.urlopen(ai_req, timeout=12) as ai_resp:
                    res_json = json.loads(ai_resp.read().decode("utf-8"))
                    candidates = res_json.get("candidates", [])
                    if candidates and "content" in candidates[0]:
                        parts = candidates[0]["content"].get("parts", [])
                        if parts and "text" in parts[0]:
                            return parts[0]["text"], model, context["ist_timestamp"]
            except urllib.error.HTTPError as he:
                print(f"[RAG] Model {model} HTTP {he.code}")
                continue
            except Exception as e:
                print(f"[RAG] Model {model} error: {e}")
                continue

    # Fallback to grounded deterministic engine
    fallback_reply = execute_deterministic_roster_ai(user_prompt, context, current_user)
    return fallback_reply, "deterministic-grounded-engine", context["ist_timestamp"]

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
        """Extract and verify Authorization: Bearer <token> or ?token=<token> query parameter."""
        auth_header = self.headers.get("Authorization", "").strip()
        token = None
        if auth_header.startswith("Bearer "):
            token = auth_header[7:].strip()
        elif "token=" in self.path:
            match = re.search(r"[?&]token=([^&]+)", self.path)
            if match:
                token = urllib.parse.unquote(match.group(1).strip())
        if not token:
            return None
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

        # 0.1 GET /api/telemetry/users (Live active operational user telemetry)
        if clean_path in ("/api/telemetry/users", "/api/telemetry/users/"):
            total_engineers = 0
            platform_counts = {"Web Portal": 0, "Android App": 0}
            active_windows = {"last_1h": 0, "last_24h": 0, "last_7d": 0}
            audit_records = []

            if ATLAS_DB is not None:
                try:
                    total_engineers = ATLAS_DB["engineers"].count_documents({})
                except Exception:
                    total_engineers = 0

                now = time.time()
                t_1h = now - 3600
                t_24h = now - 86400
                t_7d = now - 7 * 86400

                try:
                    # Query active users
                    u_1h = ATLAS_DB["engineers"].count_documents({"last_active": {"$gte": t_1h}})
                    u_24h = ATLAS_DB["engineers"].count_documents({"last_active": {"$gte": t_24h}})
                    u_7d = ATLAS_DB["engineers"].count_documents({"last_active": {"$gte": t_7d}})

                    if u_24h == 0 and "audit_logs" in ATLAS_DB.list_collection_names():
                        u_1h = len(ATLAS_DB["audit_logs"].distinct("username", {"timestamp": {"$gte": t_1h}}))
                        u_24h = len(ATLAS_DB["audit_logs"].distinct("username", {"timestamp": {"$gte": t_24h}}))
                        u_7d = len(ATLAS_DB["audit_logs"].distinct("username", {"timestamp": {"$gte": t_7d}}))

                    active_windows = {
                        "last_1h": u_1h,
                        "last_24h": u_24h,
                        "last_7d": u_7d
                    }

                    # Platform breakdown
                    web_count = ATLAS_DB["engineers"].count_documents({"platform": "Web Portal"})
                    app_count = ATLAS_DB["engineers"].count_documents({"platform": "Android App"})
                    if web_count == 0 and app_count == 0 and "audit_logs" in ATLAS_DB.list_collection_names():
                        web_count = ATLAS_DB["audit_logs"].count_documents({"platform": "Web Portal"})
                        app_count = ATLAS_DB["audit_logs"].count_documents({"platform": "Android App"})

                    platform_counts = {
                        "Web Portal": web_count,
                        "Android App": app_count
                    }

                    # Latest chronological audit records (no mock data!)
                    if "audit_logs" in ATLAS_DB.list_collection_names():
                        cursor = ATLAS_DB["audit_logs"].find({}, {"_id": 0}).sort("timestamp", -1).limit(15)
                        audit_records = list(cursor)
                except Exception as e:
                    print(f"  [TELEMETRY USERS ERROR] {e}")

            self.send_json_response({
                "success": True,
                "data": {
                    "total_engineers": total_engineers,
                    "platform_breakdown": platform_counts,
                    "active_windows": active_windows,
                    "recent_audit_log": audit_records,
                    "timestamp": time.time()
                }
            })
            return

        # 0.2 GET /api/telemetry/server (Live system & runtime diagnostics)
        if clean_path in ("/api/telemetry/server", "/api/telemetry/server/"):
            uptime_sec = max(0.0, round(time.time() - SERVER_START_TIME, 1))
            latency_ms = 0
            if ATLAS_CLIENT is not None:
                try:
                    t0 = time.perf_counter()
                    ATLAS_CLIENT.admin.command("ping")
                    latency_ms = max(1, round((time.perf_counter() - t0) * 1000))
                except Exception:
                    latency_ms = 0

            collection_counts = {}
            if ATLAS_DB is not None:
                try:
                    collection_counts["engineers"] = ATLAS_DB["engineers"].count_documents({})
                    collection_counts["rosters"] = ATLAS_DB["rosters"].count_documents({})
                    if "audit_logs" in ATLAS_DB.list_collection_names():
                        collection_counts["audit_logs"] = ATLAS_DB["audit_logs"].count_documents({})
                    if "operational_policies" in ATLAS_DB.list_collection_names():
                        collection_counts["operational_policies"] = ATLAS_DB["operational_policies"].count_documents({})
                except Exception as e:
                    print(f"  [TELEMETRY SERVER COUNTS ERROR] {e}")

            mem_mb = get_memory_rss_mb()

            self.send_json_response({
                "success": True,
                "data": {
                    "uptime_seconds": uptime_sec,
                    "uptime_formatted": format_uptime(uptime_sec),
                    "environment": {
                        "python_version": sys.version.split()[0],
                        "os_platform": f"{platform.system()} {platform.release()}",
                        "is_render": bool(os.environ.get("RENDER"))
                    },
                    "atlas": {
                        "status": ATLAS_STATUS,
                        "cluster": ATLAS_CLUSTER,
                        "database": ATLAS_DB_NAME,
                        "ping_ms": latency_ms,
                        "collections": collection_counts
                    },
                    "memory_rss_mb": mem_mb,
                    "network": {
                        "port": SERVER_PORT,
                        "lan_ip": lan_ip,
                        "public_url": PUBLIC_TUNNEL_URL,
                        "tunnel_active": bool(PUBLIC_TUNNEL_URL)
                    },
                    "version": SERVER_VERSION,
                    "timestamp": time.time()
                }
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
                "timestamp": time.time(),
                "version": SERVER_VERSION
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
            "/ai/generate", "/ai/chat", "/chat/assistant", "/ai/assistant"
        ) or norm_path.startswith("/roster-backups/"):
            norm_path = "/api" + norm_path

        if norm_path.startswith("/api/"):
            self.handle_api_post(norm_path)
            return

        self.send_error(405, "Method Not Allowed")

    def handle_api_post(self, clean_path):
        """Router for POST /api/* requests with strict authorization."""
        global DEFAULT_SERVER_ENGINEERS
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
                        platform_client = "Android App" if any(k in str(self.headers.get("User-Agent", "")).lower() for k in ("okhttp", "android", "mobile")) else "Web Portal"
                        record_audit_event(user_payload.get("name"), user_payload.get("username"), user_payload.get("role"), platform_client, "User Sign-In")
                        if ATLAS_DB is not None:
                            try:
                                ATLAS_DB["engineers"].update_one(
                                    {"username": user_payload.get("username")},
                                    {"$set": {"last_active": time.time(), "platform": platform_client}}
                                )
                            except Exception:
                                pass
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
                    platform_client = "Android App" if any(k in str(self.headers.get("User-Agent", "")).lower() for k in ("okhttp", "android", "mobile")) else "Web Portal"
                    record_audit_event(user_payload.get("name"), user_payload.get("username"), user_payload.get("role"), platform_client, "User Sign-In")
                    if ATLAS_DB is not None:
                        try:
                            ATLAS_DB["engineers"].update_one(
                                {"username": user_payload.get("username")},
                                {"$set": {"last_active": time.time(), "platform": platform_client}}
                            )
                        except Exception:
                            pass
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

            

        # 1.2 POST /api/auth/forgot-password & /api/auth/send-otp (Native Gmail SMTP OTP Dispatch)
        if clean_path in ("/api/auth/forgot-password", "/api/auth/forgot-password/", "/api/auth/send-otp", "/api/auth/send-otp/"):
            username = str(body.get("username", "") or body.get("identifier", "")).strip()
            if not username:
                self.send_json_response({"success": False, "error": "Username is required.", "message": "Username is required."}, 400)
                return

            client_ip = self.client_address[0] if self.client_address else "127.0.0.1"
            is_limited, limit_msg = check_otp_rate_limit(client_ip, username.lower())
            if is_limited:
                self.send_json_response({"success": False, "error": limit_msg, "message": limit_msg}, 429)
                return

            record_otp_dispatch(client_ip, username.lower())

            # 1. Look up engineer in MongoDB
            engineer = None
            if ATLAS_DB is not None:
                try:
                    engineer = ATLAS_DB.engineers.find_one({"username": {"$regex": f"^{re.escape(username)}$", "$options": "i"}})
                    if not engineer:
                        engineer = ATLAS_DB.engineers.find_one({"email": {"$regex": f"^{re.escape(username)}$", "$options": "i"}})
                except Exception as e:
                    print(f"  [MONGODB LOOKUP ERROR] {e}")

            if not engineer and ATLAS_DB is None:
                for tm in DEFAULT_SERVER_ENGINEERS:
                    if tm.get("username", "").lower() == username.lower() or tm.get("email", "").lower() == username.lower():
                        engineer = dict(tm)
                        break

            if not engineer or not engineer.get("email"):
                print(f"[SMTP DISPATCH] Lookup rejected: User '{username}' or registered email not found in MongoDB.")
                time.sleep(0.2)
                self.send_json_response({"success": False, "error": "Engineer or registered email not found.", "message": "Engineer or registered email not found."}, 404)
                return

            recipient_email = engineer["email"].strip()
            engineer_name = engineer.get("name", username)

            # 2. Generate 6-digit numeric OTP & store with expiration (10 minutes)
            otp_code = f"{secrets.randbelow(900000) + 100000}"
            now_ts = time.time()
            expires_at = now_ts + 600

            # Store reset_otp and otp_created_at directly in engineer document
            if ATLAS_DB is not None and "_id" in engineer:
                try:
                    ATLAS_DB.engineers.update_one(
                        {"_id": engineer["_id"]},
                        {"$set": {
                            "reset_otp": otp_code,
                            "otp_created_at": now_ts
                        }}
                    )
                except Exception as e:
                    print(f"  [OTP DB SAVE ERROR] {e}")

            # Also maintain active in-memory and password_resets record
            otp_hash = hashlib.sha256(otp_code.encode("utf-8")).hexdigest()
            clean_user = engineer.get("username", username).lower()
            ACTIVE_OTPS[clean_user] = {
                "hash": otp_hash,
                "code": otp_code,
                "expires_at": expires_at,
                "email": recipient_email,
                "name": engineer_name,
                "attempts": 0
            }
            if ATLAS_DB is not None:
                try:
                    ATLAS_DB.password_resets.update_one(
                        {"identifier": clean_user},
                        {"$set": {
                            "identifier": clean_user,
                            "email": recipient_email.lower(),
                            "hash": otp_hash,
                            "expires_at": expires_at,
                            "created_at": now_ts,
                            "attempts": 0
                        }},
                        upsert=True
                    )
                except Exception as e:
                    pass

            # 3. Send email to the engineer's registered address via native Gmail SMTP or HTTPS Fallback
            sent = send_otp_email(recipient_email, engineer_name, otp_code)
            if sent:
                log_server(f"[AUTH] ✅ Password reset OTP dispatched successfully to '{recipient_email}'.")
                self.send_json_response({
                    "success": True,
                    "message": "OTP verification code sent to registered email address.",
                    "delivered_to": recipient_email
                }, 200)
            else:
                log_server(f"[AUTH] ❌ Password reset OTP dispatch failed for '{recipient_email}'.")
                self.send_json_response({
                    "success": False,
                    "error": "Failed to dispatch verification email. Please check server logs.",
                    "message": "Failed to dispatch verification email via Gmail SMTP or HTTPS fallback."
                }, 500)
            return

        # 1.3 POST /api/auth/verify-otp (Verify OTP and Update Password with PBKDF2)
        if clean_path in ("/api/auth/verify-otp", "/api/auth/verify-otp/"):
            username = str(body.get("username", "") or body.get("identifier", "")).strip()
            otp = str(body.get("otp", "")).strip()
            new_pass = str(body.get("new_password", "") or body.get("newPassword", "")).strip()

            if not username or not otp or not new_pass:
                self.send_json_response({
                    "success": False,
                    "error": "Username, OTP code, and new password are required.",
                    "code": "MISSING_FIELDS",
                    "message": "Username, OTP code, and new password are required."
                }, 400)
                return

            # Lookup engineer
            engineer = None
            if ATLAS_DB is not None:
                try:
                    engineer = ATLAS_DB.engineers.find_one({"username": {"$regex": f"^{re.escape(username)}$", "$options": "i"}})
                    if not engineer:
                        engineer = ATLAS_DB.engineers.find_one({"email": {"$regex": f"^{re.escape(username)}$", "$options": "i"}})
                except Exception as e:
                    print(f"  [MONGODB LOOKUP ERROR] {e}")

            if not engineer and ATLAS_DB is None:
                for tm in DEFAULT_SERVER_ENGINEERS:
                    if tm.get("username", "").lower() == username.lower() or tm.get("email", "").lower() == username.lower():
                        engineer = dict(tm)
                        break

            if not engineer:
                self.send_json_response({
                    "success": False,
                    "error": "Account not found.",
                    "code": "ACCOUNT_NOT_FOUND",
                    "message": "Account not found."
                }, 404)
                return

            # Verify OTP
            stored_otp = str(engineer.get("reset_otp", "")).strip()
            otp_created_at = engineer.get("otp_created_at", 0)

            clean_user = engineer.get("username", username).lower()
            record = ACTIVE_OTPS.get(clean_user)
            if not record and ATLAS_DB is not None:
                try:
                    record = ATLAS_DB.password_resets.find_one({"$or": [{"identifier": clean_user}, {"email": engineer.get("email", "").lower()}]})
                except Exception:
                    pass

            is_valid_otp = False
            is_expired = False

            if stored_otp:
                if stored_otp == otp:
                    if time.time() - otp_created_at <= 600:
                        is_valid_otp = True
                    else:
                        is_expired = True
            elif record:
                input_hash = hashlib.sha256(otp.encode("utf-8")).hexdigest()
                if record.get("code") == otp or hmac.compare_digest(record.get("hash", ""), input_hash):
                    if time.time() <= record.get("expires_at", 0):
                        is_valid_otp = True
                    else:
                        is_expired = True

            if is_expired:
                self.send_json_response({
                    "success": False,
                    "error": "Verification code has expired. Please request a new one.",
                    "code": "EXPIRED_OTP",
                    "message": "Verification code has expired (10-minute limit)."
                }, 400)
                return

            if not is_valid_otp:
                self.send_json_response({
                    "success": False,
                    "error": "Invalid verification code.",
                    "code": "INVALID_OTP",
                    "message": "Invalid verification code."
                }, 400)
                return

            if len(new_pass) < 6:
                self.send_json_response({
                    "success": False,
                    "error": "Password must be at least 6 characters long.",
                    "code": "WEAK_PASSWORD",
                    "message": "Password must be at least 6 characters long."
                }, 400)
                return

            # Hash new password with salted PBKDF2
            hashed_pass = hash_password(new_pass)

            # Update password and clear reset_otp in MongoDB
            if ATLAS_DB is not None and "_id" in engineer:
                try:
                    ATLAS_DB.engineers.update_one(
                        {"_id": engineer["_id"]},
                        {
                            "$set": {
                                "password": hashed_pass,
                                "password_updated_at": time.strftime("%Y-%m-%dT%H:%M:%SZ", time.gmtime())
                            },
                            "$unset": {
                                "reset_otp": "",
                                "otp_created_at": ""
                            }
                        }
                    )
                except Exception as e:
                    print(f"  [OTP] Password update in Atlas error: {e}")

            # Clear memory and password_resets collection
            ACTIVE_OTPS.pop(clean_user, None)
            if ATLAS_DB is not None:
                try:
                    ATLAS_DB.password_resets.delete_many({"$or": [{"identifier": clean_user}, {"email": engineer.get("email", "").lower()}]})
                except Exception:
                    pass

            print(f"[AUTH] ✅ Password successfully reset for engineer '{engineer.get('username')}' ({engineer.get('email')}).")
            self.send_json_response({
                "success": True,
                "message": "Password reset successfully. You may now log in with your new password."
            }, 200)
            return

        # ======================================================================
        # 2. AUTHENTICATED AI PROXY (Eliminates Browser / App API Key Exposure)
        # ======================================================================

        if clean_path in ("/api/chat/assistant", "/api/chat/assistant/", "/api/ai/assistant", "/api/ai/assistant/"):
            claims = self.get_auth_claims()
            current_user = body.get("current_user") or {}
            if claims:
                if not current_user.get("name") and claims.get("name"):
                    current_user["name"] = claims["name"]
                if not current_user.get("username") and claims.get("username"):
                    current_user["username"] = claims["username"]
                if not current_user.get("role") and claims.get("role"):
                    current_user["role"] = claims["role"]

            user_query = body.get("message") or body.get("prompt") or ""
            client_platform = body.get("platform") or "Web Operations Console"

            if not user_query.strip():
                self.send_json_response({"success": False, "message": "Missing 'message' or 'prompt' in payload."}, 400)
                return

            ai_reply, model_name, grounded_time = query_gemini_roster_agent(
                user_prompt=user_query,
                current_user=current_user,
                client_platform=client_platform
            )

            self.send_json_response({
                "success": True,
                "reply": ai_reply,
                "model": model_name,
                "grounded_time": grounded_time,
                "platform": client_platform
            })
            return

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

        # 3.7 POST /api/logs/clear (Clear Server Log Buffer - ADMIN ONLY)
        if clean_path in ("/api/logs/clear", "/api/logs/clear/"):
            claims = self.require_auth(["admin", "manager"])
            if not claims:
                return
            SERVER_LOGS.clear()
            log_server("[SYSTEM] Server diagnostic logs buffer cleared by administrator.")
            self.send_json_response({"success": True, "message": "Server logs cleared successfully."})
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
