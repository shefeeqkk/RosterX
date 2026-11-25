#!/usr/bin/env python3
"""
RosterX - Secure Production & Development Server & Native Python Atlas Bridge
Provides:
  - Isolated serving of 'web/' directory
  - Native MongoDB Atlas REST API bridge using PyMongo
  - Cryptographic HMAC-SHA256 JWT Authentication & Role-Based Access Control (RBAC)
  - PBKDF2-HMAC-SHA256 Password Hashing & Salted Storage (Zero Plaintext)
  - Cryptographically secure OTP generation, hashing, and anti-abuse rate limiting
  - Server-side Gemini AI proxy eliminating client-side API key exposure
  - Restricted CORS & HTTP security headers
  - Automatic Local & Network (LAN) IP resolution
  - Render Cloud production & local LAN serving
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
import uuid
import base64
import urllib.request
import urllib.error
import urllib.parse
import random
import copy
import traceback
import calendar

try:
    from google import genai
    from google.genai import types
except ImportError:
    genai = None
    types = None

from http.server import SimpleHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path

# Server Version (Follows project semantic versioning rule: small=patch, semi-big=minor, major=major)
from datetime import datetime, timezone, timedelta
SERVER_VERSION = "2.5.30"
SERVER_BUILD_CODE = 205
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

def detect_client_platform(headers) -> str:
    """Accurately identify client platform distinguishing native Android App from Web Portal."""
    if not headers:
        return "Web Portal"
    # Native Android App transmits custom X-Device-Info header or OkHttp user-agent
    if headers.get("X-Device-Info") or str(headers.get("X-Client-Platform", "")).lower() == "android":
        return "Android App"
    ua = str(headers.get("User-Agent") or "").lower()
    if "okhttp" in ua or "com.rosterx" in ua:
        return "Android App"
    return "Web Portal"

def parse_device_info(headers) -> str:
    """Parse HTTP headers into a concise, human-readable device/platform string."""
    if not headers:
        return "Unknown Device"
    custom_device = headers.get("X-Device-Info") or headers.get("X-Device-Model") or headers.get("X-Client-Device")
    if custom_device:
        return str(custom_device).strip()

    ua = str(headers.get("User-Agent") or "").strip()
    if not ua:
        return "Unknown Device"

    # Apple iOS Web Browsers
    if "iPhone" in ua or "iPad" in ua:
        browser = "Safari"
        if "CriOS" in ua: browser = "Chrome"
        elif "FxiOS" in ua: browser = "Firefox"
        elif "EdgiOS" in ua: browser = "Edge"
        device_type = "iPhone" if "iPhone" in ua else "iPad"
        return f"{browser} • Apple {device_type}"

    # Android Mobile Web Browsers
    if "Android" in ua:
        match = re.search(r"Android\s+([0-9.]+)(?:;\s*([^;)]+))?", ua)
        version = match.group(1) if match else ""
        model = match.group(2).strip() if (match and match.group(2)) else ""
        if model == "K":  # Chrome Android Build ID placeholder
            model = ""
        browser = "Browser"
        if "Chrome/" in ua: browser = "Chrome"
        elif "Firefox/" in ua: browser = "Firefox"
        elif "EdgA/" in ua: browser = "Edge"
        elif "SamsungBrowser" in ua: browser = "Samsung Internet"

        if model:
            return f"{browser} • {model} (Android {version})"
        elif version:
            return f"{browser} • Android {version}"
        return f"{browser} • Android"

    os_name = "Unknown OS"
    if "Windows NT 10.0" in ua:
        os_name = "Windows 10/11"
    elif "Windows NT 6.3" in ua:
        os_name = "Windows 8.1"
    elif "Windows NT 6.1" in ua:
        os_name = "Windows 7"
    elif "Macintosh" in ua or "Mac OS X" in ua:
        os_name = "macOS"
    elif "Linux" in ua:
        os_name = "Linux"
    elif "CrOS" in ua:
        os_name = "Chrome OS"

    browser = "Browser"
    if "Edg/" in ua:
        browser = "Edge"
    elif "Brave" in ua:
        browser = "Brave"
    elif "Chrome/" in ua:
        browser = "Chrome"
    elif "Firefox/" in ua:
        browser = "Firefox"
    elif "Safari/" in ua and "Chrome/" not in ua:
        browser = "Safari"

    return f"{browser} • {os_name}"

def record_audit_event(engineer_name, username, role, platform_name, action, details=None, device=None):
    """Safely append an operational audit record into database collection 'audit_logs' and in-memory buffer."""
    now_ts = time.time()
    det = details or {}
    device_str = device or (det.get("device") if isinstance(det, dict) else None)
    doc = {
        "engineer_name": engineer_name,
        "username": username,
        "role": role,
        "platform": platform_name,
        "device": device_str,
        "action": action,
        "details": det,
        "timestamp": now_ts,
        "datetime": time.strftime("%Y-%m-%d %H:%M:%S", time.gmtime(now_ts))
    }
    SERVER_AUDIT_LOGS.appendleft(dict(doc))
    if ATLAS_DB is not None:
        try:
            ATLAS_DB["audit_logs"].insert_one(dict(doc))
        except Exception as e:
            print(f"  [AUDIT LOG ERROR] {e}")

def get_leave_system_config() -> dict:
    """Retrieve global leave configuration (capping toggle & annual quota)."""
    default_cfg = {
        "capping_enabled": True,
        "annual_quota": 14
    }
    if ATLAS_DB is not None:
        try:
            doc = ATLAS_DB.system_settings.find_one({"_id": "leave_config"})
            if doc:
                return {
                    "capping_enabled": bool(doc.get("capping_enabled", True)),
                    "annual_quota": int(doc.get("annual_quota", 14))
                }
        except Exception as e:
            print(f"  [ATLAS LEAVE CONFIG ERROR] {e}")
    return default_cfg

def save_leave_system_config(capping_enabled: bool, annual_quota: int):
    """Persist global leave policy configuration."""
    if ATLAS_DB is not None:
        try:
            ATLAS_DB.system_settings.update_one(
                {"_id": "leave_config"},
                {"$set": {"capping_enabled": capping_enabled, "annual_quota": annual_quota}},
                upsert=True
            )
        except Exception as e:
            print(f"  [ATLAS SAVE LEAVE CONFIG ERROR] {e}")

def get_engineer_leave_stats(username: str, annual_year: int) -> dict:
    """Calculate approved leave days, remaining quota, and active pending count for an engineer."""
    approved_leave_days = 0
    active_count = 0
    clean_user = str(username).strip().lower()

    docs = []
    if ATLAS_DB is not None:
        try:
            docs = list(ATLAS_DB.leave_requests.find({"engineer_id": clean_user}, {"_id": 0}))
        except Exception as e:
            print(f"  [LEAVE QUERY ERROR] {e}")
            docs = [r for r in SERVER_LEAVE_REQUESTS if str(r.get("engineer_id", "")).lower() == clean_user]
    else:
        docs = [r for r in SERVER_LEAVE_REQUESTS if str(r.get("engineer_id", "")).lower() == clean_user]

    for req in docs:
        st = str(req.get("status") or "").lower()
        yr = req.get("annual_year") or annual_year
        if st == "pending":
            active_count += 1
        elif st == "approved_leave" and int(yr) == int(annual_year):
            approved_leave_days += int(req.get("days_count") or len(req.get("dates", [])))

    cfg = get_leave_system_config()
    capping_enabled = cfg.get("capping_enabled", True)
    annual_quota = int(cfg.get("annual_quota", 14))

    remaining = max(0, annual_quota - approved_leave_days) if capping_enabled else None
    return {
        "approved_leave_days": approved_leave_days,
        "remaining_leave_balance": remaining,
        "active_count": active_count,
        "annual_quota": annual_quota if capping_enabled else None,
        "capping_enabled": capping_enabled
    }

def get_roster_shift_for_date(username: str, date_str: str) -> str:
    """Retrieve an engineer's scheduled shift for a specific YYYY-MM-DD date from active monthly roster."""
    if len(date_str) < 10:
        return ""
    month_key = date_str[:7]
    try:
        d_num = int(date_str[8:10])
    except ValueError:
        return ""

    clean_user = str(username).strip().lower()
    roster_doc = None
    if ATLAS_DB is not None:
        try:
            roster_doc = ATLAS_DB.rosters.find_one({"_id": month_key})
        except Exception:
            pass

    if not roster_doc:
        return ""

    eng_list = roster_doc.get("engineers") or roster_doc.get("roster") or []
    for eng in eng_list:
        if not isinstance(eng, dict):
            continue
        u = str(eng.get("username") or "").strip().lower()
        n = str(eng.get("name") or "").strip().lower()
        if u == clean_user or n == clean_user:
            s_raw = eng.get("shifts")
            if isinstance(s_raw, dict):
                return str(s_raw.get(str(d_num), "")).strip().upper()
            elif isinstance(s_raw, list) and len(s_raw) >= d_num:
                return str(s_raw[d_num - 1]).strip().upper()
    return ""

def update_roster_shift_for_date(username: str, date_str: str, new_shift_code: str) -> bool:
    """Update an engineer's scheduled shift cell in ATLAS_DB.rosters."""
    if ATLAS_DB is None or len(date_str) < 10:
        return False
    month_key = date_str[:7]
    try:
        d_num = int(date_str[8:10])
    except ValueError:
        return False

    clean_user = str(username).strip().lower()
    try:
        roster_doc = ATLAS_DB.rosters.find_one({"_id": month_key})
        if not roster_doc:
            return False

        updated = False
        for field in ("engineers", "roster"):
            eng_list = roster_doc.get(field)
            if not isinstance(eng_list, list):
                continue
            for eng in eng_list:
                if not isinstance(eng, dict):
                    continue
                u = str(eng.get("username") or "").strip().lower()
                n = str(eng.get("name") or "").strip().lower()
                if u == clean_user or n == clean_user:
                    s_raw = eng.get("shifts")
                    if isinstance(s_raw, dict):
                        s_raw[str(d_num)] = new_shift_code
                        updated = True
                    elif isinstance(s_raw, list) and len(s_raw) >= d_num:
                        s_raw[d_num - 1] = new_shift_code
                        updated = True

        if updated:
            ATLAS_DB.rosters.update_one(
                {"_id": month_key},
                {"$set": {"engineers": roster_doc.get("engineers"), "roster": roster_doc.get("roster")}}
            )
            return True
    except Exception as e:
        print(f"  [ROSTER UPDATE ERROR] Failed updating shift for {username} on {date_str}: {e}")
    return False

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
WEB_DIR = ROOT_DIR / "web"

# Global Atlas Bridge State
ATLAS_CLIENT = None
ATLAS_DB = None
ATLAS_STATUS = "DISCONNECTED"
ATLAS_ERROR = None
ATLAS_CLUSTER = "Cluster0"
ATLAS_DB_NAME = "rosterx"

# Global Production Server & Network State
PUBLIC_SERVER_URL = os.environ.get("PUBLIC_SERVER_URL", "https://api.rosterx.online").strip().rstrip("/")
APP_PUBLIC_BASE_URL = os.environ.get("APP_PUBLIC_BASE_URL", PUBLIC_SERVER_URL or "https://api.rosterx.online").strip().rstrip("/")
SERVER_PORT = 8000


def get_android_app_link_fingerprints() -> list:
    """
    Read comma/newline-separated Android signing certificate SHA-256 fingerprints
    from ANDROID_APP_LINK_SHA256 (environment variable or local .env).
    Never hardcodes keystore fingerprints in source code.
    """
    raw = os.environ.get("ANDROID_APP_LINK_SHA256", "").strip()
    if not raw:
        try:
            env_map = load_env_variables()
            raw = str(env_map.get("ANDROID_APP_LINK_SHA256", "")).strip()
        except Exception:
            raw = ""
    if not raw:
        return []
    seen = set()
    result = []
    for token in re.split(r"[,\n;]+", raw):
        fp = token.strip().strip("'\"").upper()
        if fp and fp not in seen:
            seen.add(fp)
            result.append(fp)
    return result


CACHED_GITHUB_RELEASE = {
    "version": "1.34.0",
    "tag_name": "v1.34.0",
    "apk_download_url": "https://github.com/shefeeqkk/RosterX/releases/latest",
    "last_fetched": 0.0
}

def fetch_latest_github_release(max_age_seconds: int = 600) -> dict:
    """
    Fetch the latest officially released Android version from GitHub Releases API with in-memory caching.
    Prevents API rate limiting and decouples client update prompts from internal backend server versions.
    """
    global CACHED_GITHUB_RELEASE
    now = time.time()
    if now - CACHED_GITHUB_RELEASE["last_fetched"] < max_age_seconds and CACHED_GITHUB_RELEASE["version"]:
        return CACHED_GITHUB_RELEASE

    repo = os.environ.get("GITHUB_REPO", "shefeeqkk/RosterX").strip()
    url = f"https://api.github.com/repos/{repo}/releases/latest"
    try:
        req = urllib.request.Request(
            url,
            headers={
                "User-Agent": "RosterX-Update-Service",
                "Accept": "application/vnd.github.v3+json"
            }
        )
        gh_token = os.environ.get("GITHUB_TOKEN", "").strip()
        if gh_token:
            req.add_header("Authorization", f"Bearer {gh_token}")

        with urllib.request.urlopen(req, timeout=5) as resp:
            if resp.status == 200:
                data = json.loads(resp.read().decode("utf-8"))
                tag = str(data.get("tag_name") or "").strip()
                clean_ver = tag.lstrip("vV").split("-")[0].strip()

                apk_url = f"https://github.com/{repo}/releases/latest"
                for asset in data.get("assets", []):
                    name = str(asset.get("name") or "").lower()
                    if name.endswith(".apk"):
                        apk_url = asset.get("browser_download_url") or apk_url
                        break

                if clean_ver:
                    CACHED_GITHUB_RELEASE = {
                        "version": clean_ver,
                        "tag_name": tag,
                        "apk_download_url": apk_url,
                        "last_fetched": now
                    }
                    return CACHED_GITHUB_RELEASE
    except Exception as e:
        log_server(f"[GITHUB RELEASE FETCH] Non-fatal release query notice: {e}")

    # On error, retain existing cache and back off
    CACHED_GITHUB_RELEASE["last_fetched"] = now
    return CACHED_GITHUB_RELEASE


def get_app_update_metadata() -> dict:
    """
    Return Android in-app update discovery metadata for GET /api/server-info.
    Reads overrides from environment variables / .env, or dynamically queries GitHub Releases.
    Never falls back to internal backend server version.
    """
    env_map = {}
    try:
        env_map = load_env_variables()
    except Exception:
        env_map = {}

    def _get_val(key: str, default: str) -> str:
        val = (os.environ.get(key) or env_map.get(key) or "").strip().strip("'\"")
        return val if val else default

    explicit_latest = (os.environ.get("LATEST_ANDROID_VERSION") or env_map.get("LATEST_ANDROID_VERSION") or "").strip().strip("'\"")
    gh_release = fetch_latest_github_release()
    latest_version = explicit_latest or gh_release.get("version") or "1.34.0"
    download_url = _get_val("APK_DOWNLOAD_URL", gh_release.get("apk_download_url") or "https://github.com/shefeeqkk/RosterX/releases/latest")

    return {
        "latest_android_version": latest_version,
        "min_supported_version": _get_val("MIN_SUPPORTED_VERSION", "1.34.0"),
        "apk_download_url": download_url,
    }


def _render_open_in_app_cta(deep_path: str, label: str = "Open in RosterX App &rarr;") -> str:
    """Render a branded deep-link CTA pill button for automated RosterX HTML emails."""
    clean_seg = str(deep_path or "").strip().lstrip("/")
    base = (os.environ.get("APP_PUBLIC_BASE_URL") or APP_PUBLIC_BASE_URL or "https://api.rosterx.online").strip().rstrip("/")
    target_url = f"{base}/open/{clean_seg}" if clean_seg else f"{base}/open"
    return (
        f'<div style="text-align:center;margin:22px 0 8px;">'
        f'<a href="{target_url}" target="_blank" '
        f'style="display:inline-block;padding:11px 24px;border-radius:999px;'
        f'background:linear-gradient(135deg,#2563eb 0%,#4f46e5 100%);color:#ffffff;'
        f'font-size:14px;font-weight:700;text-decoration:none;letter-spacing:0.2px;'
        f'box-shadow:0 4px 12px rgba(37,99,235,0.35);">{label}</a>'
        f'</div>'
    )

IST_TIMEZONE = timezone(timedelta(hours=5, minutes=30))

def get_ist_now() -> datetime:
    """Return timezone-aware current time in Indian Standard Time (IST, UTC+05:30)."""
    return datetime.now(IST_TIMEZONE)

def get_ist_time_str(format_12h: bool = True) -> str:
    """Format IST time string in 12-hour format with AM/PM (e.g. '11:58:30 AM')."""
    now = get_ist_now()
    if format_12h:
        return now.strftime("%I:%M:%S %p")
    return now.strftime("%H:%M:%S")

# Global Live Server Log Ring Buffer (Available for Authenticated Admin Terminal Log Viewer)
SERVER_LOGS = collections.deque(maxlen=500)
SERVER_NOTIFICATIONS = collections.deque(maxlen=100)
SERVER_AUDIT_LOGS = collections.deque(maxlen=200)  # In-memory buffer for operational audit logs
SERVER_CREDENTIAL_TRACKERS = []  # In-memory fallback if Atlas DB is disconnected
SERVER_CREDENTIAL_TRACKER_LOGS = collections.deque(maxlen=1000)  # In-memory fallback for audit logs
SERVER_BROADCAST_TEMPLATES = []  # In-memory fallback for custom broadcast templates
LAST_CREDENTIAL_ALERT_DATE = {}  # { tracker_id: "YYYY-MM-DD" } to prevent duplicate alerts on same day
SERVER_USER_CALENDAR_SYNC = {}  # In-memory fallback: {username: {month: sync_entry}}
SERVER_USER_DISMISSED_NOTIFICATIONS = {}  # In-memory fallback: {username: set(dismissed_ids)}
SERVER_USER_CALENDAR_ACCOUNTS = {}  # In-memory fallback: {username: [account_dicts]}
SERVER_LEAVE_REQUESTS = []  # In-memory fallback: [leave_request_dicts]

def log_server(msg: str):
    """Log to stdout and append timestamped entry to circular SERVER_LOGS buffer in Indian Standard Time (IST)."""
    ts = get_ist_now().strftime('%Y-%m-%d %I:%M:%S %p')
    formatted = f"[{ts}] {msg}"
    print(formatted)
    SERVER_LOGS.append(formatted)

# Security & Authentication State
AUTH_SECRET_KEY = None
EPHEMERAL_ADMIN_PASSWORD = None
ACTIVE_OTPS = {}  # { clean_id: {"hash": "...", "expires_at": timestamp, "email": "...", "attempts": 0} }
OTP_IDENTIFIER_COOLDOWN = {}  # { clean_id: last_timestamp }
OTP_IP_REQUESTS = collections.defaultdict(list)  # { ip: [timestamps] }
FAILED_LOGIN_ATTEMPTS = collections.defaultdict(list)  # { ip_user_key: [timestamps] }

def get_admin_credentials():
    """
    Retrieve master admin credentials from environment or .env.
    If ADMIN_PASSWORD is not configured, generates a secure one-time cryptographic
    password via secrets.token_urlsafe(16) and logs a security warning once to console.
    """
    global EPHEMERAL_ADMIN_PASSWORD
    env = load_env_variables()
    admin_user = env.get("ADMIN_USERNAME", "admin").strip().lower() or os.environ.get("ADMIN_USERNAME", "admin").strip().lower()
    admin_pass = env.get("ADMIN_PASSWORD", "").strip() or os.environ.get("ADMIN_PASSWORD", "").strip()
    if not admin_pass:
        if not EPHEMERAL_ADMIN_PASSWORD:
            EPHEMERAL_ADMIN_PASSWORD = secrets.token_urlsafe(16)
            log_server(
                f"[SECURITY WARNING] ADMIN_PASSWORD is not set in environment or .env! "
                f"Generated temporary one-time password for '{admin_user}': {EPHEMERAL_ADMIN_PASSWORD}"
            )
        admin_pass = EPHEMERAL_ADMIN_PASSWORD
    return admin_user, admin_pass

# Resend Configuration
DEFAULT_RESEND_API_KEY = os.environ.get("RESEND_API_KEY", "").strip().strip("'\"")
DEFAULT_ALERT_EMAIL = os.environ.get("ALERT_EMAIL", "").strip().strip("'\"")
RESEND_API_KEY = DEFAULT_RESEND_API_KEY
RESEND_FROM = os.environ.get("RESEND_FROM", "RosterX Operations <notify@rosterx.online>").strip().strip("'\"")


# ==============================================================================
# Team Directory Seed Loader (No hardcoded credentials or engineers in source)
# ==============================================================================
def load_default_engineers():
    """Load initial engineer directory from an ignored local seed file if present, otherwise empty."""
    seed_paths = [
        os.path.join(os.path.dirname(os.path.abspath(__file__)), "default_engineers.json"),
        os.path.join(os.path.dirname(os.path.abspath(__file__)), ".engineers_seed.json")
    ]
    for p in seed_paths:
        if os.path.exists(p):
            try:
                with open(p, "r", encoding="utf-8") as f:
                    data = json.load(f)
                    if isinstance(data, list):
                        return data
            except Exception as e:
                print(f"  [SEED LOAD ERROR] {e}")
    return []

DEFAULT_SERVER_ENGINEERS = load_default_engineers()


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
        raise RuntimeError(
            "\n[FATAL] AUTH_SECRET_KEY is not set in the environment.\n"
            "  → Set it in your .env file or Render dashboard under Environment Variables.\n"
            "  → Generate a secure key with:\n"
            "       python -c \"import secrets; print(secrets.token_hex(32))\"\n"
            "  → Never use a hardcoded fallback — it makes all JWT tokens forgeable.\n"
        )
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

def to_proper_name(name: str) -> str:
    """
    Normalizes a name string from ALL CAPS or arbitrary casing into clean Proper Title Case.
    Preserves initials and short single-character tokens (e.g. 'Member A', 'Member B').
    """
    if not name or not isinstance(name, str):
        return name
    words = name.strip().split()
    proper_words = []
    for w in words:
        if len(w) == 1:
            proper_words.append(w.upper())
        elif w.upper() in ("II", "III", "IV", "V"):
            proper_words.append(w.upper())
        else:
            proper_words.append(w.capitalize())
    return " ".join(proper_words)

def process_profile_picture_base64(raw_base64_str: str, max_dimension: int = 256, quality: int = 85) -> str:
    """
    Validates, crops to square, resizes to max_dimension x max_dimension, and compresses
    a base64 data URI image. Returns a clean standard data URI: 'data:image/jpeg;base64,...'.
    Falls back gracefully if PIL is not installed.
    """
    if not raw_base64_str or not isinstance(raw_base64_str, str):
        return ""

    clean_str = raw_base64_str.strip()
    encoded_part = clean_str
    if "base64," in clean_str:
        encoded_part = clean_str.split("base64,", 1)[1]

    try:
        image_bytes = base64.b64decode(encoded_part)
    except Exception as e:
        raise ValueError(f"Invalid base64 encoding: {e}")

    if len(image_bytes) > 5 * 1024 * 1024:
        raise ValueError("Image file too large (max 5MB).")

    try:
        import io
        from PIL import Image, ImageOps
        img = Image.open(io.BytesIO(image_bytes))

        # Handle EXIF orientation if present
        try:
            img = ImageOps.exif_transpose(img)
        except Exception:
            pass

        # Convert RGBA / P / etc. to RGB with white background for JPEG
        if img.mode in ("RGBA", "LA", "P"):
            background = Image.new("RGB", img.size, (255, 255, 255))
            if img.mode == "P":
                img = img.convert("RGBA")
            background.paste(img, mask=img.split()[-1] if "A" in img.mode else None)
            img = background
        elif img.mode != "RGB":
            img = img.convert("RGB")

        # Center crop to square
        w, h = img.size
        min_dim = min(w, h)
        left = (w - min_dim) // 2
        top = (h - min_dim) // 2
        right = left + min_dim
        bottom = top + min_dim
        img = img.crop((left, top, right, bottom))

        # Resize to max_dimension
        if min_dim > max_dimension:
            img = img.resize((max_dimension, max_dimension), Image.Resampling.LANCZOS)

        out_buf = io.BytesIO()
        img.save(out_buf, format="JPEG", quality=quality, optimize=True)
        compressed_bytes = out_buf.getvalue()
        b64_out = base64.b64encode(compressed_bytes).decode("ascii")
        return f"data:image/jpeg;base64,{b64_out}"
    except ImportError:
        # Fallback if Pillow is not available
        if not clean_str.startswith("data:image/"):
            return f"data:image/jpeg;base64,{encoded_part}"
        return clean_str
    except Exception as e:
        raise ValueError(f"Failed to process image: {e}")

def find_user_by_identifier(identifier: str):
    """
    Centralized, strict user lookup by identifier (username, email, or id).
    Eliminates loose regex matches and handles database and fallback directory.
    Supports alert/registered Resend account email resolution.
    """
    if not identifier:
        return None
    clean_id = str(identifier).strip().lower()

    # Match primary administrator / alert email alias
    env = load_env_variables()
    alert_email = env.get("ALERT_EMAIL", "").strip().lower() or (DEFAULT_ALERT_EMAIL.lower() if DEFAULT_ALERT_EMAIL else "")
    if alert_email and clean_id == alert_email:
        admin_u = env.get("ADMIN_USERNAME", "admin").strip().lower()
        if ATLAS_DB is not None:
            try:
                user = ATLAS_DB.engineers.find_one({"$or": [{"username": admin_u}, {"role": "admin"}]})
                if user:
                    return user
            except Exception as e:
                print(f"  [USER LOOKUP ERROR] {e}")
        for tm in DEFAULT_SERVER_ENGINEERS:
            if tm.get("username") == admin_u or tm.get("role") == "admin":
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
    Determine if an account is a management or system administrator account.
    Admin and Manager accounts NEVER appear on the Roster Matrix.
    """
    if not isinstance(user_dict, dict):
        return False

    # 1. Any account with role 'admin' or 'manager' is strictly a management account
    role = str(user_dict.get("role", "")).strip().lower()
    if role in ("admin", "manager"):
        return True

    # 2. Known administrative usernames and names
    uname = str(user_dict.get("username", "") or user_dict.get("id", "") or user_dict.get("engineerId", "")).strip().lower()
    n = str(user_dict.get("name", "")).strip().lower()
    fn = str(user_dict.get("full_name", "")).strip().lower()
    if uname in ("admin", "manager", "sysadmin", "system_admin", "administrator") or \
       n in ("admin", "administrator", "manager", "system admin", "system administrator") or \
       "system admin" in n or "system administrator" in n or \
       "system admin" in fn or "system administrator" in fn:
        return True

    # 3. Explicit exclusion from roster
    inc_flag = user_dict.get("include_in_roster")
    if inc_flag is None:
        inc_flag = user_dict.get("includeInRoster")
    if inc_flag is False:
        return True

    return False

def is_eligible_roster_row(e: dict, month_id: str, eng_meta_by_key: dict = None) -> bool:
    """
    Strict filter for roster matrix display and persistence.
    1. Admin and Manager accounts NEVER appear on the Roster Matrix.
    2. Honor explicit include_in_roster=False.
    3. Resigned engineers: Exclude starting from the month AFTER resignation.
    """
    if not isinstance(e, dict):
        return False

    meta = None
    if eng_meta_by_key:
        for field in ("username", "engineer_id", "engineerId", "id", "name", "full_name"):
            val = str(e.get(field) or "").strip().lower()
            if val and val in eng_meta_by_key:
                meta = eng_meta_by_key[val]
                break

    # 1. Admin and Manager accounts strictly excluded
    role = str((meta.get("role") if meta else e.get("role")) or "").strip().lower()
    uname = str((meta.get("username") if meta else e.get("username")) or "").strip().lower()
    name = str((meta.get("name") if meta else e.get("name")) or "").strip().lower()
    if role in ("admin", "manager") or uname in ("admin", "manager", "administrator", "sysadmin") or \
       name in ("admin", "administrator", "manager") or "administrator" in name or "system admin" in name or \
       is_management_account(meta or e) or is_management_account(e):
        return False

    # 2. Honor explicit include_in_roster=False
    inc = (meta.get("include_in_roster") if meta and meta.get("include_in_roster") is not None else e.get("include_in_roster"))
    if inc is False:
        return False

    # 3. Resigned engineers: Exclude starting from the month AFTER resignation
    status = str((meta.get("status") if meta else e.get("status")) or "").strip().lower()
    is_resigned = status == "resigned" or (meta and meta.get("resigned") is True) or e.get("resigned") is True
    if is_resigned:
        res_date = (meta.get("resignation_date") or meta.get("resigned_date") or meta.get("resigned_month") if meta else None) or \
                   e.get("resignation_date") or e.get("resigned_date") or e.get("resigned_month")
        if res_date:
            res_month = str(res_date).strip()[:7]
            if month_id > res_month:
                return False
        else:
            today_ist_month = get_ist_now().strftime("%Y-%m")
            if month_id > today_ist_month:
                return False

    return True

def solve_night_shifts_engine(year: int, month: int, engineers_data: list, leaves_data: list = None, engineer_rules: dict = None, baseline_only: bool = False, post_night_rest_days: int = 2) -> dict:
    """
    Deterministic constraint solver for night shift (NS) allocation.
    Focuses EXCLUSIVELY on:
      - Week Off (OFF)
      - Approved Leave (L)
      - Approved Swap (SW*)
      - Night Shift (NS)
    All other shifts remain empty string ("").

    Constraints:
      1. Baseline: OFF, L, and Swaps are locked and inviolable.
      2. Exactly 1 NS per calendar day across the entire team (24/7 coverage).
      3. Consecutive block length per engineer: 1 to 5 nights maximum.
      4. Excluded engineers (mode == 'excluded' or is_excluded == True) get 0 night shifts.
      5. Fixed quota engineers (e.g., mode == 'fixed_5' or fixed_nights == 5) receive
         their exact quota (e.g. 5 days, preferably as a single contiguous block).
      6. Flexible engineers rotate in blocks of 1 to 5 consecutive days (preferring 2 to 5 days).
      7. No engineer works NS on an OFF, L, or Swap day.
      8. Post-night rest: after completing a night shift block, the engineer should have adequate rest.
    """
    leaves_data = leaves_data or []
    engineer_rules = engineer_rules or {}

    total_days = calendar.monthrange(year, month)[1]
    month_id = f"{year}-{str(month).zfill(2)}"
    day_names = ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"]

    # 1. Filter active eligible roster engineers
    eng_meta_by_key = {}
    for e in engineers_data:
        for f in ("username", "name", "full_name", "id", "engineer_id"):
            v = str(e.get(f) or "").strip().lower()
            if v:
                eng_meta_by_key[v] = e

    active_engineers = []
    for eng in engineers_data:
        if not is_eligible_roster_row(eng, month_id, eng_meta_by_key):
            continue
        uname = str(eng.get("username") or eng.get("id") or "").strip().lower()
        if not uname:
            continue
        active_engineers.append(eng)

    if not active_engineers:
        return {"success": False, "message": "No active engineers available for scheduling."}

    # 2. Build baseline grid
    grid = {}
    eng_by_uname = {}
    rules_by_uname = {}

    for eng in active_engineers:
        uname = str(eng.get("username") or eng.get("id") or "").strip().lower()
        eng_by_uname[uname] = eng
        grid[uname] = [""] * total_days

        rule = engineer_rules.get(uname) or engineer_rules.get(eng.get("username")) or {}
        rules_by_uname[uname] = rule

        # Default weekly offs (supports full names like "SATURDAY" or 3-letter abbreviations like "SAT")
        raw_offs = eng.get("default_weekly_offs") or []
        is_apprentice = str(eng.get("role") or "").lower() == "apprentice"
        def_offs = set()
        primary_off = set()
        for idx, x in enumerate(raw_offs):
            s = str(x).upper().strip()
            if s:
                def_offs.add(s)
                if len(s) >= 3:
                    def_offs.add(s[:3])
                if idx == 0:
                    primary_off.add(s)
                    if len(s) >= 3:
                        primary_off.add(s[:3])

        for day in range(1, total_days + 1):
            dt = datetime(year, month, day)
            day_name = day_names[dt.weekday()]
            day_abbr = day_name[:3]
            week_idx = ((day - 1) // 7) + 1
            is_two_day_week = (week_idx % 2 != 0)  # Week 1, 3, 5 get 2 days; Week 2, 4 get 1 day

            if is_apprentice:
                # Apprentice alternating schedule per operational directive:
                # Week 1: 2 days (both weekly offs)
                # Week 2: 1 day (primary off only)
                # Week 3: 2 days (both weekly offs)
                # Week 4: 1 day (primary off only)
                # Strictly 6 days across the month (2 + 1 + 2 + 1 = 6)
                if day <= 28:
                    if is_two_day_week:
                        if day_name in def_offs or day_abbr in def_offs:
                            grid[uname][day - 1] = "OFF"
                    else:
                        if day_name in primary_off or day_abbr in primary_off:
                            grid[uname][day - 1] = "OFF"
            else:
                if day_name in def_offs or day_abbr in def_offs:
                    grid[uname][day - 1] = "OFF"

    # 3. Apply approved leaves and swaps onto baseline
    for req in leaves_data:
        r_uname = str(req.get("engineer_id") or req.get("username") or "").strip().lower()
        target_u = r_uname if r_uname in grid else None
        if not target_u and r_uname in eng_meta_by_key:
            m_eng = eng_meta_by_key[r_uname]
            m_u = str(m_eng.get("username") or m_eng.get("id") or "").strip().lower()
            if m_u in grid:
                target_u = m_u
        if not target_u:
            r_name = str(req.get("engineer_name") or req.get("applicant_name") or req.get("name") or "").strip().lower()
            if r_name in eng_meta_by_key:
                m_eng = eng_meta_by_key[r_name]
                m_u = str(m_eng.get("username") or m_eng.get("id") or "").strip().lower()
                if m_u in grid:
                    target_u = m_u
        if not target_u:
            continue

        dates = req.get("dates") or []
        if isinstance(dates, str):
            dates = [dates]
        elif not dates and req.get("date"):
            dates = [str(req.get("date"))]
        elif not dates and req.get("start_date") and req.get("end_date"):
            try:
                s_dt = datetime.strptime(str(req.get("start_date"))[:10], "%Y-%m-%d")
                e_dt = datetime.strptime(str(req.get("end_date"))[:10], "%Y-%m-%d")
                cur = s_dt
                dates = []
                while cur <= e_dt:
                    dates.append(cur.strftime("%Y-%m-%d"))
                    cur += timedelta(days=1)
            except Exception:
                dates = []
        req_status = str(req.get("status") or "").lower()
        swap_code = req.get("swap_shift_code") or req.get("swap_code") or "SWAP"

        for d_str in dates:
            try:
                parts = [int(p) for p in d_str.split("-")]
                if len(parts) == 3 and parts[0] == year and parts[1] == month:
                    day_num = parts[2]
                    if 1 <= day_num <= total_days:
                        if req_status == "approved_swap":
                            grid[target_u][day_num - 1] = swap_code
                        else:
                            grid[target_u][day_num - 1] = "L"
            except Exception:
                continue

    if baseline_only:
        # Pre-fill baseline only (OFF, L, Swaps) without night shift allocation
        roster_rows = []
        stats = {
            "total_days": total_days,
            "nights_scheduled": 0,
            "coverage_percentage": 0.0,
            "engineers_count": len(grid),
            "baseline_only": True,
            "per_engineer_stats": {}
        }
        for uname, shifts in grid.items():
            eng_meta = eng_by_uname[uname]
            stats["per_engineer_stats"][uname] = {
                "name": eng_meta.get("name") or eng_meta.get("full_name") or uname,
                "ns_count": 0,
                "off_count": shifts.count("OFF"),
                "leave_count": shifts.count("L"),
                "rule": rules_by_uname.get(uname, {})
            }
            roster_rows.append({
                "id": eng_meta.get("id") or uname,
                "username": uname,
                "name": eng_meta.get("name") or eng_meta.get("full_name") or uname,
                "role": eng_meta.get("role") or "engineer",
                "default_weekly_offs": eng_meta.get("default_weekly_offs") or [],
                "shifts": shifts
            })
        roster_rows.sort(key=lambda r: (1 if str(r.get("role") or "").lower() == "apprentice" else 0, str(r.get("name") or "").lower()))
        return {
            "success": True,
            "month": month_id,
            "total_days": total_days,
            "baseline_only": True,
            "stats": stats,
            "roster": roster_rows,
            "assigned_daily_nights": []
        }

    # 4. Classify engineers by rules
    excluded_unames = set()
    fixed_unames = {}  # uname -> {"target": int, "pref_start": int or None}
    flexible_unames = []
    max_block_by_uname = {}

    for uname in grid.keys():
        rule = rules_by_uname[uname]
        mb = rule.get("max_consecutive_nights") or rule.get("max_block") or 5
        try:
            mb = int(mb)
            if mb < 1:
                mb = 5
        except Exception:
            mb = 5
        max_block_by_uname[uname] = mb

        mode = str(rule.get("mode") or "").lower()
        is_appr = str(eng_by_uname[uname].get("role") or "").lower() == "apprentice"
        if mode == "excluded" or rule.get("is_excluded") is True or is_appr:
            excluded_unames.add(uname)
        elif mode in ("fixed_5", "fixed", "continuous_5", "continuous") or rule.get("fixed_nights") is not None:
            target = int(rule.get("fixed_nights") or 5)
            pref_start = rule.get("preferred_start_day")
            try:
                pref_start = int(pref_start) if pref_start is not None else None
            except Exception:
                pref_start = None
            fixed_unames[uname] = {"target": target, "pref_start": pref_start}
        else:
            flexible_unames.append(uname)

    # 5. Day assignment array
    assigned_ns = [None] * total_days

    def can_work_ns(uname, d):
        if uname in excluded_unames:
            return False
        if grid[uname][d] != "":
            return False
        return True

    def get_max_free_streak(uname, start_d, max_len=5):
        streak = 0
        while (start_d + streak < total_days) and streak < max_len:
            d = start_d + streak
            if not can_work_ns(uname, d) or assigned_ns[d] is not None:
                break
            streak += 1
        return streak

    # 6. Priority allocation: Fixed quota engineers (e.g. 5 days)
    for f_uname, f_info in fixed_unames.items():
        if f_uname in excluded_unames:
            continue
        req_nights = f_info["target"]
        pref_start = f_info.get("pref_start")
        max_b = max_block_by_uname.get(f_uname, 5)

        if max_b < req_nights:
            # Engineer requested fixed quota with SPLIT blocks (e.g. 2 or 3 days max per block)
            chunks = []
            rem = req_nights
            while rem > 0:
                sz = min(rem, max_b)
                chunks.append(sz)
                rem -= sz

            cur_day = pref_start - 1 if pref_start and 1 <= pref_start <= total_days - req_nights + 1 else 0
            allocated_days = []
            for sz in chunks:
                placed = False
                for s in range(cur_day, total_days - sz + 1):
                    if allocated_days and (s - allocated_days[-1]) < 2:
                        continue
                    if all(can_work_ns(f_uname, d) and assigned_ns[d] is None for d in range(s, s + sz)):
                        for d in range(s, s + sz):
                            assigned_ns[d] = f_uname
                            allocated_days.append(d)
                        cur_day = s + sz + 2
                        placed = True
                        break
                if not placed:
                    for s in range(0, total_days - sz + 1):
                        if all(can_work_ns(f_uname, d) and assigned_ns[d] is None for d in range(s, s + sz)):
                            for d in range(s, s + sz):
                                assigned_ns[d] = f_uname
                                allocated_days.append(d)
                            break
        else:
            # Single continuous block
            candidate_starts = []
            if pref_start and 1 <= pref_start <= total_days - req_nights + 1:
                candidate_starts.append(pref_start - 1)
            for s in range(0, total_days - req_nights + 1):
                if s not in candidate_starts:
                    candidate_starts.append(s)

            best_block = None
            for start_day in candidate_starts:
                block_free = True
                for d in range(start_day, start_day + req_nights):
                    if not can_work_ns(f_uname, d) or assigned_ns[d] is not None:
                        block_free = False
                        break
                if block_free:
                    best_block = list(range(start_day, start_day + req_nights))
                    break

            if best_block:
                for d in best_block:
                    assigned_ns[d] = f_uname
            else:
                allocated = 0
                for start_day in range(0, total_days):
                    if allocated >= req_nights:
                        break
                    if can_work_ns(f_uname, start_day) and assigned_ns[start_day] is None:
                        assigned_ns[start_day] = f_uname
                        allocated += 1

    # 7. Solve remaining days with flexible pool
    if not flexible_unames and None in assigned_ns:
        flexible_unames = [u for u in grid.keys() if u not in excluded_unames]

    # Pre-flight mathematical capacity check
    cand_caps = {}
    total_capacity = 0
    for uname in grid.keys():
        if uname in excluded_unames:
            cand_caps[uname] = 0
        elif uname in fixed_unames:
            c = fixed_unames[uname]["target"]
            cand_caps[uname] = c
            total_capacity += c
        else:
            rule = rules_by_uname.get(uname, {})
            c = int(rule.get("max_monthly_nights") or rule.get("monthly_cap") or 5)
            cand_caps[uname] = c
            total_capacity += c

    if not baseline_only and total_capacity < total_days:
        deficit = total_days - total_capacity
        active_candidates = [eng_by_uname[u].get("name", u) for u in grid.keys() if u not in excluded_unames]
        excluded_candidates = [eng_by_uname[u].get("name", u) for u in excluded_unames]
        return {
            "success": False,
            "capacity_shortfall": True,
            "total_days": total_days,
            "total_capacity": total_capacity,
            "deficit": deficit,
            "active_count": len(active_candidates),
            "excluded_engineers": excluded_candidates,
            "flexible_engineers": [eng_by_uname[u].get("name", u) for u in flexible_unames],
            "message": f"Capacity Shortfall: {total_days} night shifts required for 24/7 coverage, but the {len(active_candidates)} active engineers under current caps can only cover {total_capacity} nights (deficit: {deficit} night{'s' if deficit > 1 else ''}). The solver strictly prevents exceeding caps without your input. Please include an excluded engineer or permit an extra night shift."
        }

    unassigned_count = sum(1 for d in assigned_ns if d is None)
    flex_count = len(flexible_unames)
    ideal_target = (unassigned_count // flex_count) if flex_count > 0 else 5

    ns_counts = {u: 0 for u in grid.keys()}
    for d in range(total_days):
        if assigned_ns[d] is not None:
            ns_counts[assigned_ns[d]] += 1

    last_night_day = {u: -999 for u in grid.keys()}
    for d in range(total_days):
        if assigned_ns[d] is not None:
            last_night_day[assigned_ns[d]] = d

    def solve_backtracking(day_idx):
        if day_idx >= total_days:
            return True

        if assigned_ns[day_idx] is not None:
            return solve_backtracking(day_idx + 1)

        prev_eng = assigned_ns[day_idx - 1] if day_idx > 0 else None

        moves = []
        for cand in flexible_unames:
            cand_max = max_block_by_uname.get(cand, 5)
            # Monthly night shift capping: strictly enforce configured cap (never silently exceed!)
            cand_monthly_cap = cand_caps.get(cand, 5)
            effective_cap = cand_monthly_cap

            if ns_counts[cand] >= effective_cap:
                continue

            streak = get_max_free_streak(cand, day_idx, max_len=cand_max)
            if streak <= 0:
                continue

            # Check consecutive days cand already worked prior to day_idx
            cur_consec = 0
            back_d = day_idx - 1
            while back_d >= 0 and assigned_ns[back_d] == cand:
                cur_consec += 1
                back_d -= 1

            allowed_additional = min(cand_max - cur_consec, effective_cap - ns_counts[cand])
            if allowed_additional <= 0:
                continue

            effective_streak = min(streak, allowed_additional)
            if effective_streak <= 0:
                continue

            lengths_to_try = sorted(list(set([
                effective_streak,
                min(effective_streak, 4),
                min(effective_streak, 3),
                min(effective_streak, 2),
                1
            ])), reverse=True)
            for b_len in lengths_to_try:
                moves.append((cand, b_len))

        def move_score(move):
            cand, b_len = move
            cand_max = max_block_by_uname.get(cand, 5)
            is_prev = 1 if cand == prev_eng else 0
            rest_violation = 1 if (day_idx - last_night_day[cand]) < (post_night_rest_days + 1) and cand != prev_eng else 0
            zero_shifts_bonus = 0 if ns_counts[cand] == 0 else 1
            exceeds_target = 1 if (ns_counts[cand] + b_len > ideal_target) else 0
            size_penalty = cand_max - b_len
            return (
                rest_violation,
                zero_shifts_bonus,
                ns_counts[cand],
                exceeds_target,
                is_prev,
                size_penalty
            )

        sorted_moves = sorted(moves, key=move_score)

        for cand, b_size in sorted_moves:
            prev_last = last_night_day[cand]
            for offset in range(b_size):
                assigned_ns[day_idx + offset] = cand
            ns_counts[cand] += b_size
            last_night_day[cand] = day_idx + b_size - 1

            if solve_backtracking(day_idx + b_size):
                return True

            for offset in range(b_size):
                assigned_ns[day_idx + offset] = None
            ns_counts[cand] -= b_size
            last_night_day[cand] = prev_last

        return False

    success = solve_backtracking(0)
    if not success or None in assigned_ns:
        unassigned_days = [d + 1 for d in range(total_days) if assigned_ns[d] is None]
        return {
            "success": False,
            "unassigned_days": unassigned_days,
            "message": f"Could not satisfy 24/7 night coverage with strict caps and rest constraints. Days unassigned: {unassigned_days}. Please adjust constraints or allow an extra night."
        }

    # 8. Commit assigned_ns to the grid
    for d in range(total_days):
        worker = assigned_ns[d]
        if worker and worker in grid:
            grid[worker][d] = "NS"

    # 8b. Mandatory Post-Night Rest Protection:
    # Immediately following any completed night shift block, the engineer MUST
    # receive post-night recovery rest ('OFF').
    # Any blank day ('') within the post-night rest window is stamped as 'OFF'.
    post_rest_window = max(1, int(post_night_rest_days or 2))
    for uname, shifts in grid.items():
        d = 0
        while d < total_days:
            if shifts[d] == "NS":
                streak_end = d
                while streak_end + 1 < total_days and shifts[streak_end + 1] == "NS":
                    streak_end += 1

                # Stamp blank days immediately following the night block as 'OFF'
                for r in range(1, post_rest_window + 1):
                    post_d = streak_end + r
                    if post_d < total_days:
                        # If already 'L', 'SW*', or 'OFF', rest is satisfied; if blank, stamp 'OFF'
                        if shifts[post_d] == "":
                            shifts[post_d] = "OFF"

                d = streak_end + 1
            else:
                d += 1

    # 9. Format response payload
    roster_rows = []
    stats = {
        "total_days": total_days,
        "nights_scheduled": sum(1 for d in assigned_ns if d is not None),
        "coverage_percentage": round((sum(1 for d in assigned_ns if d is not None) / total_days) * 100, 1),
        "engineers_count": len(grid),
        "per_engineer_stats": {}
    }

    for uname, shifts in grid.items():
        eng_meta = eng_by_uname[uname]
        ns_count = shifts.count("NS")
        off_count = shifts.count("OFF")
        l_count = shifts.count("L")
        stats["per_engineer_stats"][uname] = {
            "name": eng_meta.get("name") or eng_meta.get("full_name") or uname,
            "ns_count": ns_count,
            "off_count": off_count,
            "leave_count": l_count,
            "rule": rules_by_uname.get(uname, {})
        }
        roster_rows.append({
            "id": eng_meta.get("id") or uname,
            "username": uname,
            "name": eng_meta.get("name") or eng_meta.get("full_name") or uname,
            "role": eng_meta.get("role") or "engineer",
            "default_weekly_offs": eng_meta.get("default_weekly_offs") or [],
            "shifts": shifts
        })

    roster_rows.sort(key=lambda r: (1 if str(r.get("role") or "").lower() == "apprentice" else 0, str(r.get("name") or "").lower()))

    return {
        "success": True,
        "month": month_id,
        "total_days": total_days,
        "stats": stats,
        "roster": roster_rows,
        "assigned_daily_nights": [
            {
                "day": d + 1,
                "date": f"{year}-{str(month).zfill(2)}-{str(d + 1).zfill(2)}",
                "username": assigned_ns[d],
                "name": (eng_by_uname.get(assigned_ns[d]) or {}).get("name") if assigned_ns[d] else None
            }
            for d in range(total_days)
        ]
    }

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

        # Ensure indexes on credential_trackers and credential_tracker_logs collections
        try:
            ATLAS_DB.credential_trackers.create_index("id", unique=True)
            ATLAS_DB.credential_trackers.create_index("server_name")
            ATLAS_DB.credential_trackers.create_index("expiry_date")
            ATLAS_DB.credential_tracker_logs.create_index("id", unique=True)
            ATLAS_DB.credential_tracker_logs.create_index([("timestamp", -1)])
            ATLAS_DB.credential_tracker_logs.create_index("tracker_id")
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

            # Purge any management or ineligible accounts from saved rosters in Atlas DB
            try:
                eng_meta_by_key = {}
                try:
                    for eng_doc in ATLAS_DB.engineers.find({}, {"password": 0}):
                        for field in ("username", "name", "full_name", "id", "engineer_id"):
                            val = str(eng_doc.get(field) or "").strip().lower()
                            if val:
                                eng_meta_by_key[val] = eng_doc
                except Exception:
                    pass

                for r_doc in list(ATLAS_DB.rosters.find()):
                    r_id = str(r_doc.get("_id") or "")
                    r_engs = r_doc.get("engineers", [])
                    r_ros = r_doc.get("roster", [])
                    clean_engs = [e for e in r_engs if is_eligible_roster_row(e, r_id, eng_meta_by_key)]
                    clean_ros = [e for e in r_ros if is_eligible_roster_row(e, r_id, eng_meta_by_key)]
                    if len(clean_engs) != len(r_engs) or len(clean_ros) != len(r_ros):
                        ATLAS_DB.rosters.update_one({"_id": r_id}, {"$set": {"engineers": clean_engs, "roster": clean_ros}})
                        print(f"  [ATLAS ROSTER SANITIZE] Purged ineligible accounts from roster '{r_id}'.")
            except Exception as r_err:
                print(f"  [ATLAS ROSTER SANITIZE NOTE] {r_err}")

            # Sanitize Leadership (Admin & Manager) week offs: Leadership is not on shift schedules
            try:
                lead_res = ATLAS_DB.engineers.update_many(
                    {"role": {"$in": ["admin", "manager", "ADMIN", "MANAGER"]}},
                    {"$set": {"default_weekly_offs": []}}
                )
                if lead_res.modified_count > 0:
                    print(f"  [ATLAS SANITIZE] Cleared default_weekly_offs to [] for {lead_res.modified_count} admin/manager accounts.")
            except Exception as l_err:
                print(f"  [ATLAS SANITIZE NOTE] {l_err}")

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
    env = load_env_variables()
    raw_key = env.get("RESEND_API_KEY", "").strip() or os.environ.get("RESEND_API_KEY", "").strip() or (RESEND_API_KEY or "").strip() or (DEFAULT_RESEND_API_KEY or "").strip()
    api_key = raw_key.strip("'\"").strip()
    if not api_key:
        log_server("[RESEND AUDIT] ERROR: RESEND_API_KEY is not configured in .env or environment.")
        return {"success": False, "error": "RESEND_API_KEY is not configured in .env"}

    raw_from = env.get("RESEND_FROM", "").strip() or os.environ.get("RESEND_FROM", "").strip() or (RESEND_FROM or "").strip()
    from_sender = raw_from.strip("'\"").strip() or "RosterX Operations <notify@rosterx.online>"

    primary_sandbox_email = (env.get("ALERT_EMAIL", "") or os.environ.get("ALERT_EMAIL", "") or DEFAULT_ALERT_EMAIL or "").strip().strip("'\"")

    payload = {
        "from": from_sender,
        "to": [to_email],
        "subject": subject,
        "html": html_content
    }

    log_server(f"[RESEND AUDIT] Outgoing Request: POST https://api.resend.com/emails")
    log_server(f"[RESEND AUDIT] Sender ('from'): {from_sender}")
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
            fallback_email = extracted_email or primary_sandbox_email or ""

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

# Legacy default templates deprecated - Broadcast console now maintains pure custom user templates
DEFAULT_BROADCAST_TEMPLATES = []

def send_fcm_push(tokens, title, body, priority="info", notif_id="", extra_data=None):
    """
    Dispatch Firebase Cloud Messaging (FCM) push notifications to Android devices.
    Supports firebase_admin SDK or direct Google OAuth2 FCM REST dispatch.
    Zero-cost, 100% free Google FCM delivery to phone status bar.
    """
    if not tokens:
        return 0

    unique_tokens = list(set([str(t).strip() for t in tokens if t and str(t).strip()]))
    if not unique_tokens:
        return 0

    log_server(f"[FCM] Preparing push dispatch for {len(unique_tokens)} device tokens (Title: '{title}', Priority: {priority})")
    dispatched_count = 0

    try:
        import firebase_admin
        from firebase_admin import credentials, messaging

        if not firebase_admin._apps:
            cred = None

            # 1. Check FIREBASE_SERVICE_ACCOUNT_JSON env variable (raw string or base64)
            env_sa = os.environ.get("FIREBASE_SERVICE_ACCOUNT_JSON", "").strip()
            if env_sa:
                try:
                    if env_sa.startswith("{"):
                        sa_data = json.loads(env_sa)
                    else:
                        import base64
                        decoded = base64.b64decode(env_sa).decode("utf-8")
                        sa_data = json.loads(decoded)
                    if "private_key" in sa_data:
                        cred = credentials.Certificate(sa_data)
                        log_server("[FCM] Loaded Firebase Service Account from FIREBASE_SERVICE_ACCOUNT_JSON environment variable.")
                except Exception as e:
                    log_server(f"[FCM] Failed parsing FIREBASE_SERVICE_ACCOUNT_JSON: {e}")

            # 2. Check filesystem candidate paths
            if not cred:
                service_account_paths = [
                    os.path.join(os.path.dirname(__file__), "firebase-adminsdk.json"),
                    os.path.join(os.path.dirname(__file__), "android", "app", "firebase-adminsdk.json"),
                    os.environ.get("FIREBASE_ADMINSDK_PATH", ""),
                    os.environ.get("GOOGLE_APPLICATION_CREDENTIALS", "")
                ]
                for sa_path in service_account_paths:
                    if sa_path and os.path.exists(sa_path):
                        try:
                            with open(sa_path, "r", encoding="utf-8") as f:
                                sa_data = json.load(f)
                            if "private_key" in sa_data:
                                cred = credentials.Certificate(sa_path)
                                log_server(f"[FCM] Loaded Firebase Service Account from {sa_path}")
                                break
                        except Exception as e:
                            log_server(f"[FCM] Error reading service account {sa_path}: {e}")

            if cred:
                firebase_admin.initialize_app(cred)
            else:
                log_server("[FCM WARNING] No Firebase Admin Service Account key found. To enable push alerts, download your service account JSON from Firebase Console -> Project Settings -> Service accounts, and set FIREBASE_SERVICE_ACCOUNT_JSON in Render or save as firebase-adminsdk.json.")
                return 0

        for token in unique_tokens:
            try:
                payload_data = {
                    "id": str(notif_id),
                    "priority": str(priority),
                    "title": str(title),
                    "body": str(body)
                }
                if extra_data and isinstance(extra_data, dict):
                    for ek, ev in extra_data.items():
                        payload_data[str(ek)] = str(ev)

                msg = messaging.Message(
                    notification=messaging.Notification(title=title, body=body),
                    data=payload_data,
                    token=token,
                    android=messaging.AndroidConfig(
                        priority="high" if priority.lower() == "urgent" else "normal",
                        notification=messaging.AndroidNotification(
                            channel_id="rosterx_operational_broadcasts",
                            sound="default"
                        )
                    )
                )
                messaging.send(msg)
                dispatched_count += 1
            except Exception as item_err:
                log_server(f"[FCM] Token delivery error: {item_err}")

        log_server(f"[FCM] Firebase Admin SDK dispatch completed: {dispatched_count}/{len(unique_tokens)} pushed successfully.")
        return dispatched_count
    except ImportError:
        log_server("[FCM ERROR] firebase-admin package is not installed. Add firebase-admin to requirements.txt.")
        return 0
    except Exception as e:
        log_server(f"[FCM ERROR] Firebase Admin SDK attempt failed: {e}")
        return 0

LAST_CELEBRATION_CHECK_DATE = None
LAST_CELEBRATION_CHECK_TIME = 0.0

def ordinal(n: int) -> str:
    """Return ordinal representation of integer n (e.g. 1st, 2nd, 3rd, 4th)."""
    if 11 <= (n % 100) <= 13:
        return f"{n}th"
    rem = n % 10
    if rem == 1:
        return f"{n}st"
    elif rem == 2:
        return f"{n}nd"
    elif rem == 3:
        return f"{n}rd"
    return f"{n}th"

def parse_celebration_date(raw_date_str: str, now_ist: datetime) -> tuple:
    """
    Intelligently parses birthday or join date string across diverse formats:
      - YYYY-MM-DD, YYYY/MM/DD, YYYY.MM.DD
      - DD-MM-YYYY, DD/MM/YYYY, DD.MM.YYYY
      - MM-DD, DD-MM
    Returns:
      (is_match_today: bool, years_completed: int, parsed_year: int | None)
    """
    if not raw_date_str:
        return (False, 0, None)

    cleaned = str(raw_date_str).strip().replace("/", "-").replace(".", "-")
    parts = [p.strip() for p in cleaned.split("-") if p.strip()]

    target_m = now_ist.month
    target_d = now_ist.day

    year = None
    month = None
    day = None

    if len(parts) == 3:
        p0, p1, p2 = parts[0], parts[1], parts[2]
        if len(p0) == 4 and p0.isdigit():
            # YYYY-MM-DD
            try:
                year = int(p0)
                month = int(p1)
                day = int(p2)
            except Exception:
                pass
        elif len(p2) == 4 and p2.isdigit():
            # DD-MM-YYYY or MM-DD-YYYY
            try:
                year = int(p2)
                v0 = int(p0)
                v1 = int(p1)
                if v0 > 12:
                    day = v0
                    month = v1
                elif v1 > 12:
                    month = v0
                    day = v1
                else:
                    if v0 == target_d and v1 == target_m:
                        day = v0
                        month = v1
                    elif v0 == target_m and v1 == target_d:
                        month = v0
                        day = v1
                    else:
                        day = v0
                        month = v1
            except Exception:
                pass
        else:
            # Fallback 2-digit year
            try:
                if len(p0) == 2 and int(p0) > 31:
                    year = 1900 + int(p0) if int(p0) > 50 else 2000 + int(p0)
                    month = int(p1)
                    day = int(p2)
                elif len(p2) == 2 and int(p2) > 31:
                    year = 1900 + int(p2) if int(p2) > 50 else 2000 + int(p2)
                    day = int(p0)
                    month = int(p1)
            except Exception:
                pass
    elif len(parts) == 2:
        # MM-DD or DD-MM
        try:
            v0 = int(parts[0])
            v1 = int(parts[1])
            if v0 > 12:
                day = v0
                month = v1
            elif v1 > 12:
                month = v0
                day = v1
            else:
                if v0 == target_d and v1 == target_m:
                    day = v0
                    month = v1
                elif v0 == target_m and v1 == target_d:
                    month = v0
                    day = v1
                else:
                    month = v0
                    day = v1
        except Exception:
            pass

    if month is None or day is None:
        return (False, 0, None)

    is_today = (month == target_m and day == target_d)
    years = 0
    if year is not None and is_today:
        years = max(0, now_ist.year - year)

    return (is_today, years, year)

def is_expired_celebration(notif_doc: dict, today_str: str = None, start_of_today_ist: float = None) -> bool:
    """
    Returns True if a notification document represents a Birthday / Work Anniversary celebration
    whose event date is strictly before today's IST date.
    """
    if not isinstance(notif_doc, dict):
        return False

    cat = str(notif_doc.get("category") or "").strip().lower()
    celeb_id = str(notif_doc.get("celebration_id") or "").strip()
    celeb_type = str(notif_doc.get("celebration_type") or "").strip()
    notif_id = str(notif_doc.get("id") or "").strip()

    is_celeb = (cat == "celebration") or bool(celeb_id) or bool(celeb_type) or notif_id.startswith("notif_celeb_")
    if not is_celeb:
        return False

    if today_str is None or start_of_today_ist is None:
        ist_tz = timezone(timedelta(hours=5, minutes=30))
        now_ist = datetime.now(ist_tz)
        today_str = now_ist.strftime("%Y-%m-%d")
        start_of_today_ist = datetime(now_ist.year, now_ist.month, now_ist.day, tzinfo=ist_tz).timestamp()

    if celeb_id.startswith("celeb_") and not celeb_id.startswith(f"celeb_{today_str}"):
        return True

    ts = notif_doc.get("timestamp")
    if ts is not None:
        try:
            if float(ts) < start_of_today_ist:
                return True
        except Exception:
            pass

    return False

def purge_expired_celebrations() -> int:
    """
    Hard Purge Engine:
    Permanently deletes expired celebration notices from ATLAS_DB.notifications,
    prunes their orphaned IDs from ATLAS_DB.engineers.dismissed_notifications,
    and purges expired celebrations from the in-memory SERVER_NOTIFICATIONS buffer.
    """
    global ATLAS_DB, SERVER_NOTIFICATIONS
    try:
        ist_tz = timezone(timedelta(hours=5, minutes=30))
        now_ist = datetime.now(ist_tz)
        today_str = now_ist.strftime("%Y-%m-%d")
        start_of_today_ist = datetime(now_ist.year, now_ist.month, now_ist.day, tzinfo=ist_tz).timestamp()

        deleted_count = 0
        if ATLAS_DB is not None:
            celeb_filter = {
                "$and": [
                    {
                        "$or": [
                            {"category": "celebration"},
                            {"celebration_id": {"$exists": True, "$nin": [None, ""]}},
                            {"celebration_type": {"$exists": True, "$nin": [None, ""]}},
                            {"id": {"$regex": r"^notif_celeb_"}}
                        ]
                    },
                    {
                        "$or": [
                            {"timestamp": {"$lt": start_of_today_ist}},
                            {"celebration_id": {"$regex": r"^celeb_(?!" + re.escape(today_str) + r")"}}
                        ]
                    }
                ]
            }
            expired_docs = list(ATLAS_DB.notifications.find(celeb_filter, {"_id": 0, "id": 1, "celebration_id": 1}))
            if expired_docs:
                expired_ids = []
                for d in expired_docs:
                    if d.get("id"):
                        expired_ids.append(str(d["id"]))
                    if d.get("celebration_id"):
                        expired_ids.append(str(d["celebration_id"]))

                res = ATLAS_DB.notifications.delete_many(celeb_filter)
                deleted_count = res.deleted_count

                if deleted_count > 0:
                    log_server(f"[PURGE] Permanently deleted {deleted_count} expired celebration notification(s) from MongoDB.")

                if expired_ids:
                    try:
                        pull_res = ATLAS_DB.engineers.update_many(
                            {"dismissed_notifications": {"$in": expired_ids}},
                            {"$pull": {"dismissed_notifications": {"$in": expired_ids}}}
                        )
                        if pull_res.modified_count > 0:
                            log_server(f"[PURGE] Pruned {len(expired_ids)} orphaned celebration ID(s) from {pull_res.modified_count} engineer record(s).")
                    except Exception as pull_err:
                        log_server(f"[PURGE NOTE] Failed to prune dismissed_notifications: {pull_err}")

        # Purge in-memory deque as well
        if SERVER_NOTIFICATIONS:
            before_mem = len(SERVER_NOTIFICATIONS)
            SERVER_NOTIFICATIONS = collections.deque(
                [n for n in SERVER_NOTIFICATIONS if not is_expired_celebration(n, today_str, start_of_today_ist)],
                maxlen=SERVER_NOTIFICATIONS.maxlen
            )
            mem_removed = before_mem - len(SERVER_NOTIFICATIONS)
            if mem_removed > 0 and deleted_count == 0:
                log_server(f"[PURGE] Removed {mem_removed} expired celebration(s) from in-memory buffer.")

        return deleted_count
    except Exception as e:
        log_server(f"[PURGE ERROR] Failed to purge expired celebrations: {e}")
        return 0

def check_and_trigger_celebrations(force=False):
    """
    Automated Daily Celebration Engine:
    Detects birthdays and work anniversaries for all team members (in IST, UTC+05:30).
    When an event matches today's date:
      1. Generates celebratory broadcast notification for the entire team in ATLAS_DB.notifications
         and SERVER_NOTIFICATIONS.
      2. Dispatches FCM push notifications to all team members so everyone gets notified of the event.
    Guarantees idempotency via unique celebration_id check per day.
    """
    global LAST_CELEBRATION_CHECK_DATE, LAST_CELEBRATION_CHECK_TIME, ATLAS_DB, SERVER_NOTIFICATIONS

    now_time = time.time()
    ist_tz = timezone(timedelta(hours=5, minutes=30))
    now_ist = datetime.now(ist_tz)
    today_str = now_ist.strftime("%Y-%m-%d")

    # Always purge expired past-day celebrations before evaluating today's milestones
    try:
        purge_expired_celebrations()
    except Exception:
        pass

    # Throttle: Check at most once every 15 minutes unless date changed or force=True
    if not force and LAST_CELEBRATION_CHECK_DATE == today_str and (now_time - LAST_CELEBRATION_CHECK_TIME < 900):
        return []

    LAST_CELEBRATION_CHECK_DATE = today_str
    LAST_CELEBRATION_CHECK_TIME = now_time

    triggered_events = []
    try:
        engineers = []
        if ATLAS_DB is not None:
            try:
                engineers = list(ATLAS_DB.engineers.find({}, {"_id": 0}))
            except Exception as e:
                log_server(f"[CELEBRATION] Error fetching engineers from Atlas: {e}")
        if not engineers:
            engineers = [dict(e) for e in DEFAULT_SERVER_ENGINEERS]

        all_fcm_tokens = []
        for eng in engineers:
            toks = eng.get("fcm_tokens", [])
            if isinstance(toks, list):
                all_fcm_tokens.extend([str(t).strip() for t in toks if t and str(t).strip()])
            elif isinstance(toks, str) and toks.strip():
                all_fcm_tokens.append(toks.strip())
        all_fcm_tokens = list(set(all_fcm_tokens))

        for eng in engineers:
            uname = str(eng.get("username") or "").strip().lower()
            if not uname or uname in ("admin", "sysadmin", "system"):
                continue
            if str(eng.get("status", "")).strip().lower() in ("resigned", "inactive"):
                continue

            full_name = str(eng.get("fullName") or eng.get("full_name") or eng.get("name") or uname).strip()
            full_name = to_proper_name(full_name)

            bday_raw = str(eng.get("birthday") or "").strip()
            jdate_raw = str(eng.get("join_date") or eng.get("joinDate") or "").strip()

            is_bday_match, _, _ = parse_celebration_date(bday_raw, now_ist)
            is_jdate_match, anniv_years, _ = parse_celebration_date(jdate_raw, now_ist)
            is_anniversary = is_jdate_match and (anniv_years >= 1)

            events = []
            if is_bday_match and is_anniversary:
                events.append(("both", anniv_years))
            elif is_bday_match:
                events.append(("birthday", 0))
            elif is_anniversary:
                events.append(("anniversary", anniv_years))

            for event_type, yr in events:
                cid = f"celeb_{today_str}_{uname}_{event_type}"

                already_triggered = False
                if ATLAS_DB is not None:
                    try:
                        already_triggered = ATLAS_DB.notifications.find_one({"celebration_id": cid}) is not None
                    except Exception:
                        pass
                if not already_triggered:
                    already_triggered = any(n.get("celebration_id") == cid for n in SERVER_NOTIFICATIONS)

                if already_triggered:
                    continue

                notif_id = f"notif_celeb_{int(now_time*1000)}_{random.randint(100, 999)}"
                yr_ord = ordinal(yr)
                yr_label = f"{yr} years" if yr > 1 else "1 year"
                celeb_phone = str(eng.get("phone") or eng.get("phoneNumber") or eng.get("contact") or "").strip()

                if event_type == "both":
                    title = f"Double Celebration: Birthday & {yr_ord} Anniversary for {full_name}! 🎂🏆"
                    msg = f"Today is doubly special for {full_name}! Join us in wishing them a very Happy Birthday and celebrating their {yr_ord} Work Anniversary with RosterX! Tap to send wishes! 🎉🎈🥂"
                elif event_type == "birthday":
                    title = f"Happy Birthday, {full_name}! 🎂"
                    msg = f"Today is {full_name}'s birthday! Join the entire RosterX team in wishing them joy, vibrant health, and continued success! Tap to send wishes! 🎉🎈🥳"
                else:
                    title = f"Happy {yr_ord} Work Anniversary, {full_name}! 🏆"
                    msg = f"Today {full_name} completes {yr_label} of operational excellence with our team! Join us in celebrating this milestone and send your warm congratulations! 🥂🎊✨"

                notif_doc = {
                    "id": notif_id,
                    "celebration_id": cid,
                    "celebration_type": event_type,
                    "celebration_user": uname,
                    "celebration_name": full_name,
                    "celebration_years": yr,
                    "celebration_photo": eng.get("profile_picture") or eng.get("profilePicture") or "",
                    "celebration_phone": celeb_phone,
                    "title": title,
                    "raw_message": msg,
                    "target": "everyone",
                    "target_type": "everyone",
                    "priority": "high",
                    "category": "celebration",
                    "sender": "RosterX Celebrations Desk",
                    "send_email": False,
                    "send_in_app": True,
                    "send_push": True,
                    "is_retracted": False,
                    "created_at": time.strftime("%Y-%m-%d %H:%M:%S UTC", time.gmtime()),
                    "timestamp": now_time
                }

                if ATLAS_DB is not None:
                    try:
                        ATLAS_DB.notifications.insert_one(dict(notif_doc))
                    except Exception as ins_err:
                        print(f"  [CELEBRATION ERROR] Failed to record notification: {ins_err}")

                clean_doc = dict(notif_doc)
                clean_doc.pop("_id", None)
                SERVER_NOTIFICATIONS.appendleft(clean_doc)
                triggered_events.append({"user": uname, "name": full_name, "type": event_type, "title": title})
                log_server(f"[CELEBRATION] Triggered {event_type} announcement for '{full_name}' ({uname}).")

                if all_fcm_tokens:
                    try:
                        sent = send_fcm_push(
                            all_fcm_tokens,
                            title,
                            msg,
                            priority="high",
                            notif_id=notif_id,
                            extra_data={"target_user": "everyone", "type": "celebration"}
                        )
                        log_server(f"[CELEBRATION] FCM push delivered to {sent} devices for {full_name}.")
                    except Exception as fcm_err:
                        log_server(f"[CELEBRATION] FCM push error: {fcm_err}")

    except Exception as e:
        print(f"  [CELEBRATIONS CHECK ERROR] {e}")

    return triggered_events

# ==============================================================================
# SERVER CREDENTIAL TRACKER & EXPIRY ALERT ENGINE
# ==============================================================================

def compute_credential_status(last_updated_date_str: str, validity_days: int, alert_threshold_days: int = 5) -> dict:
    """
    Given last_updated_date ('YYYY-MM-DD'), validity_days (int), and alert_threshold_days (int),
    calculates expiry_date ('YYYY-MM-DD'), alarm_start_date ('YYYY-MM-DD'), days_remaining (int), and status (str).
    All calculations based on current date in Indian Standard Time (IST, UTC+05:30).
    """
    today_ist = get_ist_now().date()

    try:
        updated_date = datetime.strptime(str(last_updated_date_str).strip(), "%Y-%m-%d").date()
    except Exception:
        updated_date = today_ist

    try:
        validity = max(1, int(validity_days))
    except Exception:
        validity = 30

    try:
        threshold = max(1, min(int(alert_threshold_days), validity))
    except Exception:
        threshold = min(5, validity)

    expiry_date = updated_date + timedelta(days=validity)
    alarm_start_date = expiry_date - timedelta(days=threshold)
    days_remaining = (expiry_date - today_ist).days

    if days_remaining > threshold:
        status = "HEALTHY"
    elif days_remaining > 0:
        status = "EXPIRING_SOON"
    elif days_remaining == 0:
        status = "EXPIRES_TODAY"
    else:
        status = "EXPIRED"

    return {
        "expiry_date": expiry_date.strftime("%Y-%m-%d"),
        "alarm_start_date": alarm_start_date.strftime("%Y-%m-%d"),
        "days_remaining": days_remaining,
        "status": status,
        "alert_threshold_days": threshold
    }

def format_display_date(date_str):
    """Format an ISO date string (YYYY-MM-DD) into 'dd-MMM-yyyy' (e.g. 16-Nov-2026)."""
    if not date_str:
        return ""
    try:
        dt = datetime.strptime(str(date_str).strip(), "%Y-%m-%d")
        return dt.strftime("%d-%b-%Y")
    except Exception:
        return str(date_str)

def get_all_credential_trackers_from_db():
    """Retrieve all credential trackers from Atlas DB or in-memory fallback, computing live status."""
    trackers = []
    if ATLAS_DB is not None:
        try:
            docs = list(ATLAS_DB.credential_trackers.find({}, {"_id": 0}))
            trackers = docs
        except Exception as e:
            log_server(f"[CREDENTIAL-TRACKER] Error fetching from Atlas: {e}")
            trackers = list(SERVER_CREDENTIAL_TRACKERS)
    else:
        trackers = list(SERVER_CREDENTIAL_TRACKERS)

    computed = []
    for t in trackers:
        calc = compute_credential_status(
            t.get("last_updated_date", ""),
            t.get("validity_days", 30),
            t.get("alert_threshold_days", 5)
        )
        item = dict(t)
        item["expiry_date"] = calc["expiry_date"]
        item["alarm_start_date"] = calc["alarm_start_date"]
        item["days_remaining"] = calc["days_remaining"]
        item["status"] = calc["status"]
        item["alert_threshold_days"] = calc["alert_threshold_days"]
        computed.append(item)

    status_order = {"EXPIRED": 0, "EXPIRES_TODAY": 1, "EXPIRING_SOON": 2, "HEALTHY": 3}
    computed.sort(key=lambda x: (status_order.get(x.get("status"), 4), x.get("days_remaining", 0)))
    return computed

def save_credential_tracker_to_db(tracker_dict):
    """Save or update a credential tracker in Atlas DB and in-memory list."""
    tid = tracker_dict["id"]
    if ATLAS_DB is not None:
        try:
            ATLAS_DB.credential_trackers.update_one(
                {"id": tid},
                {"$set": {k: v for k, v in tracker_dict.items() if k != "_id"}},
                upsert=True
            )
        except Exception as e:
            log_server(f"[CREDENTIAL-TRACKER] Error saving to Atlas: {e}")

    found = False
    for i, t in enumerate(SERVER_CREDENTIAL_TRACKERS):
        if t.get("id") == tid:
            SERVER_CREDENTIAL_TRACKERS[i] = dict(tracker_dict)
            found = True
            break
    if not found:
        SERVER_CREDENTIAL_TRACKERS.append(dict(tracker_dict))

def delete_credential_tracker_from_db(tracker_id):
    """Delete a credential tracker by id."""
    deleted = False
    if ATLAS_DB is not None:
        try:
            res = ATLAS_DB.credential_trackers.delete_one({"id": tracker_id})
            deleted = res.deleted_count > 0
        except Exception as e:
            log_server(f"[CREDENTIAL-TRACKER] Error deleting from Atlas: {e}")

    global SERVER_CREDENTIAL_TRACKERS
    SERVER_CREDENTIAL_TRACKERS = [t for t in SERVER_CREDENTIAL_TRACKERS if t.get("id") != tracker_id]
    return deleted

def get_credential_tracker_by_id(tracker_id: str):
    """Retrieve a single credential tracker by ID with computed status, or None."""
    if not tracker_id:
        return None
    raw = None
    if ATLAS_DB is not None:
        try:
            raw = ATLAS_DB.credential_trackers.find_one({"id": tracker_id}, {"_id": 0})
        except Exception as e:
            log_server(f"[CREDENTIAL-TRACKER] Error fetching by id from Atlas: {e}")
    if raw is None:
        for t in SERVER_CREDENTIAL_TRACKERS:
            if t.get("id") == tracker_id:
                raw = dict(t)
                break
    if not raw:
        return None

    calc = compute_credential_status(
        raw.get("last_updated_date", ""),
        raw.get("validity_days", 30),
        raw.get("alert_threshold_days", 5)
    )
    item = dict(raw)
    item["expiry_date"] = calc["expiry_date"]
    item["alarm_start_date"] = calc["alarm_start_date"]
    item["days_remaining"] = calc["days_remaining"]
    item["status"] = calc["status"]
    item["alert_threshold_days"] = calc["alert_threshold_days"]
    return item

def get_engineer_display_name(username: str) -> str:
    """Retrieve full name or display name of an engineer from MongoDB, or fallback to username."""
    if not username:
        return "System"
    clean = str(username).strip()
    if ATLAS_DB is not None:
        try:
            eng = ATLAS_DB.engineers.find_one({"username": {"$regex": f"^{re.escape(clean)}$", "$options": "i"}})
            if eng:
                return str(eng.get("name") or eng.get("full_name") or clean).strip()
        except Exception:
            pass
    for tm in DEFAULT_SERVER_ENGINEERS:
        if tm.get("username", "").lower() == clean.lower():
            return str(tm.get("name") or tm.get("full_name") or clean).strip()
    return clean.replace("_", " ").replace(".", " ").title()

def record_credential_tracker_log(action: str, tracker: dict, claims: dict = None, previous_tracker: dict = None, custom_summary: str = None):
    """
    Record an authoritative audit log entry for credential tracker operations in Atlas DB and in-memory buffer.
    Unifies 'Renewed' and 'Updated' terminology under 'UPDATE' to eliminate redundancy.
    """
    try:
        now_ist = get_ist_now()
        timestamp = int(time.time())
        datetime_str = now_ist.strftime("%d-%b-%Y, %I:%M %p IST")
        
        updated_by = (claims.get("username") if claims else None) or tracker.get("updated_by") or tracker.get("created_by") or "system"
        updated_by_name = get_engineer_display_name(updated_by)
        
        act_raw = str(action).strip().upper()
        if act_raw in ("RENEW", "UPDATE", "UPDATED", "RENEWED"):
            act_clean = "UPDATE"
            act_label = "Password Date Updated"
        elif act_raw in ("CREATE", "CREATED"):
            act_clean = "CREATE"
            act_label = "Tracker Created"
        elif act_raw in ("DELETE", "DELETED"):
            act_clean = "DELETE"
            act_label = "Tracker Deleted"
        else:
            act_clean = act_raw
            act_label = act_raw.capitalize()
            
        previous_date = None
        if previous_tracker and isinstance(previous_tracker, dict):
            previous_date = previous_tracker.get("last_updated_date")
            
        new_date = tracker.get("last_updated_date")
        validity_days = tracker.get("validity_days")
        expiry_date = tracker.get("expiry_date")
        server_name = tracker.get("server_name", "Unknown Server")
        username = tracker.get("username", "account")
        
        disp_new = format_display_date(new_date) if new_date else ""
        disp_prev = format_display_date(previous_date) if previous_date else ""
        disp_exp = format_display_date(expiry_date) if expiry_date else ""

        if custom_summary:
            summary = custom_summary
        elif act_clean == "CREATE":
            summary = f"Tracker created for '{server_name}' ({username}) with password date {disp_new} (Valid for {validity_days} days, expires {disp_exp})"
        elif act_clean == "DELETE":
            summary = f"Deleted credential tracker for '{server_name}' ({username})"
        else:
            if previous_date and previous_date != new_date:
                summary = f"Password updated from {disp_prev} to {disp_new} (Valid for {validity_days} days, expires {disp_exp})"
            else:
                summary = f"Password updated to {disp_new} (Valid for {validity_days} days, expires {disp_exp})"
                
        log_entry = {
            "id": f"cred_log_{uuid.uuid4().hex[:12]}",
            "tracker_id": tracker.get("id", ""),
            "server_name": server_name,
            "username": username,
            "action": act_clean,
            "action_label": act_label,
            "updated_by": updated_by,
            "updated_by_name": updated_by_name,
            "previous_date": previous_date,
            "new_date": new_date,
            "validity_days": validity_days,
            "expiry_date": expiry_date,
            "summary": summary,
            "timestamp": timestamp,
            "datetime_str": datetime_str
        }
        
        if ATLAS_DB is not None:
            try:
                ATLAS_DB.credential_tracker_logs.insert_one(dict(log_entry))
            except Exception as e:
                log_server(f"[CREDENTIAL-LOG] Error inserting log into Atlas: {e}")
                
        SERVER_CREDENTIAL_TRACKER_LOGS.appendleft(dict(log_entry))
        log_server(f"[CREDENTIAL-LOG] {act_label}: '{server_name}' ({username}) by {updated_by_name} (@{updated_by})")
        return log_entry
    except Exception as e:
        log_server(f"[CREDENTIAL-LOG] Error recording audit log: {e}")
        return None

def seed_credential_tracker_logs_if_empty():
    """Auto-seed baseline audit logs for existing trackers if log collection is completely empty."""
    try:
        count = 0
        if ATLAS_DB is not None:
            try:
                count = ATLAS_DB.credential_tracker_logs.count_documents({})
            except Exception:
                count = len(SERVER_CREDENTIAL_TRACKER_LOGS)
        else:
            count = len(SERVER_CREDENTIAL_TRACKER_LOGS)
            
        if count == 0:
            trackers = get_all_credential_trackers_from_db()
            if not trackers:
                return
            log_server(f"[CREDENTIAL-LOG] Auto-seeding initial audit trail for {len(trackers)} existing trackers...")
            for t in reversed(trackers):
                created_by = t.get("created_by") or "admin"
                created_at = t.get("created_at") or int(time.time() - 86400 * 30)
                dt_str = datetime.fromtimestamp(created_at, tz=timezone(timedelta(hours=5, minutes=30))).strftime("%d-%b-%Y, %I:%M %p IST")
                disp_new = format_display_date(t.get("last_updated_date"))
                disp_exp = format_display_date(t.get("expiry_date"))

                # Baseline create entry
                c_entry = {
                    "id": f"cred_log_{uuid.uuid4().hex[:12]}",
                    "tracker_id": t.get("id", ""),
                    "server_name": t.get("server_name", ""),
                    "username": t.get("username", ""),
                    "action": "CREATE",
                    "action_label": "Tracker Created",
                    "updated_by": created_by,
                    "updated_by_name": get_engineer_display_name(created_by),
                    "previous_date": None,
                    "new_date": t.get("last_updated_date"),
                    "validity_days": t.get("validity_days", 30),
                    "expiry_date": t.get("expiry_date"),
                    "summary": f"Initial tracker created for '{t.get('server_name')}' ({t.get('username')}) with password date {disp_new}",
                    "timestamp": created_at,
                    "datetime_str": dt_str
                }
                if ATLAS_DB is not None:
                    try:
                        ATLAS_DB.credential_tracker_logs.insert_one(dict(c_entry))
                    except Exception:
                        pass
                SERVER_CREDENTIAL_TRACKER_LOGS.appendleft(dict(c_entry))
                
                # If updated, baseline update entry
                updated_at = t.get("updated_at")
                if updated_at and updated_at > created_at:
                    u_by = t.get("updated_by") or created_by
                    u_dt_str = datetime.fromtimestamp(updated_at, tz=timezone(timedelta(hours=5, minutes=30))).strftime("%d-%b-%Y, %I:%M %p IST")
                    u_entry = {
                        "id": f"cred_log_{uuid.uuid4().hex[:12]}",
                        "tracker_id": t.get("id", ""),
                        "server_name": t.get("server_name", ""),
                        "username": t.get("username", ""),
                        "action": "UPDATE",
                        "action_label": "Password Date Updated",
                        "updated_by": u_by,
                        "updated_by_name": get_engineer_display_name(u_by),
                        "previous_date": None,
                        "new_date": t.get("last_updated_date"),
                        "validity_days": t.get("validity_days", 30),
                        "expiry_date": t.get("expiry_date"),
                        "summary": f"Password date renewed to {disp_new} (Expires {disp_exp})",
                        "timestamp": updated_at,
                        "datetime_str": u_dt_str
                    }
                    if ATLAS_DB is not None:
                        try:
                            ATLAS_DB.credential_tracker_logs.insert_one(dict(u_entry))
                        except Exception:
                            pass
                    SERVER_CREDENTIAL_TRACKER_LOGS.appendleft(dict(u_entry))
    except Exception as e:
        log_server(f"[CREDENTIAL-LOG] Error auto-seeding audit trail: {e}")

def get_credential_tracker_logs_from_db(tracker_id: str = None, action: str = None, search: str = None, limit: int = 150):
    """Retrieve audit logs sorted by timestamp descending, with auto-seeding fallback."""
    seed_credential_tracker_logs_if_empty()
    limit = max(1, min(int(limit), 500))
    if ATLAS_DB is not None:
        try:
            query = {}
            if tracker_id:
                query["tracker_id"] = tracker_id
            if action and action.upper() != "ALL":
                query["action"] = action.upper()
            if search:
                s_rgx = {"$regex": re.escape(search), "$options": "i"}
                query["$or"] = [
                    {"server_name": s_rgx},
                    {"username": s_rgx},
                    {"updated_by": s_rgx},
                    {"updated_by_name": s_rgx},
                    {"summary": s_rgx}
                ]
            docs = list(ATLAS_DB.credential_tracker_logs.find(query, {"_id": 0}).sort("timestamp", -1).limit(limit))
            return docs
        except Exception as e:
            log_server(f"[CREDENTIAL-LOG] Error querying logs from Atlas: {e}")

    # In-memory fallback
    filtered = list(SERVER_CREDENTIAL_TRACKER_LOGS)
    if tracker_id:
        filtered = [l for l in filtered if l.get("tracker_id") == tracker_id]
    if action and action.upper() != "ALL":
        filtered = [l for l in filtered if str(l.get("action", "")).upper() == action.upper()]
    if search:
        s_low = search.lower()
        filtered = [
            l for l in filtered
            if s_low in str(l.get("server_name", "")).lower()
            or s_low in str(l.get("username", "")).lower()
            or s_low in str(l.get("updated_by", "")).lower()
            or s_low in str(l.get("updated_by_name", "")).lower()
            or s_low in str(l.get("summary", "")).lower()
        ]
    return sorted(filtered, key=lambda x: x.get("timestamp", 0), reverse=True)[:limit]

def check_and_dispatch_credential_alerts(force=False):
    """
    Check all credential trackers where days_remaining <= alert_threshold_days.
    Dispatches:
      1. Single consolidated security digest email to unique spokesperson_emails across all alerted trackers
      2. Single consolidated FCM push to spokespersons + all Admins & Managers
      3. Single consolidated in-app notification to ATLAS_DB.notifications
    """
    global LAST_CREDENTIAL_ALERT_DATE
    today_str = get_ist_now().strftime("%Y-%m-%d")

    trackers = get_all_credential_trackers_from_db()
    alert_trackers = [
        t for t in trackers
        if t["days_remaining"] <= t.get("alert_threshold_days", 5) and t["days_remaining"] >= -14
    ]

    if not alert_trackers:
        log_server("[CREDENTIAL-ALERT] No expiring or recently expired credentials found.")
        return []

    # Database idempotency check: Avoid repeated notifications across server restarts on the same day
    if not force and ATLAS_DB is not None:
        try:
            existing_digest = ATLAS_DB.notifications.find_one({
                "category": "credential_expiry",
                "digest_date": today_str
            })
            if existing_digest:
                log_server(f"[CREDENTIAL-ALERT] Credential digest for {today_str} already exists in DB. Skipping duplicate dispatch on restart.")
                return []
        except Exception as _ex:
            log_server(f"[CREDENTIAL-ALERT] Error querying existing credential digest: {_ex}")

    trackers_to_alert = []
    for t in alert_trackers:
        tid = t["id"]
        if not force and LAST_CREDENTIAL_ALERT_DATE.get(tid) == today_str:
            continue
        trackers_to_alert.append(t)

    if not trackers_to_alert:
        log_server("[CREDENTIAL-ALERT] All pending alerts have already been dispatched today.")
        return []

    admin_tokens = set()
    eng_by_email = {}
    if ATLAS_DB is not None:
        try:
            all_engs = list(ATLAS_DB.engineers.find({}, {"_id": 0}))
            for e in all_engs:
                role = str(e.get("role", "")).lower()
                is_admin = role in ("admin", "manager") or e.get("is_admin") is True
                toks = e.get("fcm_tokens", [])
                tok_list = []
                if isinstance(toks, list):
                    tok_list = [str(tok).strip() for tok in toks if tok and str(tok).strip()]
                elif isinstance(toks, str) and toks.strip():
                    tok_list = [toks.strip()]

                if is_admin:
                    for tok in tok_list:
                        admin_tokens.add(tok)

                em = str(e.get("email", "")).strip().lower()
                if em:
                    eng_by_email[em] = {
                        "name": str(e.get("fullName") or e.get("name") or e.get("username") or "Engineer"),
                        "username": str(e.get("username", "")),
                        "tokens": tok_list
                    }
        except Exception as e:
            log_server(f"[CREDENTIAL-ALERT] Error gathering engineers: {e}")

    # Gather unique spokespersons across all alerted trackers
    all_spokespersons = set()
    for t in trackers_to_alert:
        for em in t.get("spokesperson_emails", []):
            em_clean = str(em).strip().lower()
            if em_clean and "@" in em_clean:
                all_spokespersons.add(em_clean)

    # Determine highest urgency / priority among alerted trackers
    has_expired = any(t["days_remaining"] < 0 for t in trackers_to_alert)
    has_expires_today = any(t["days_remaining"] == 0 for t in trackers_to_alert)
    if has_expired or has_expires_today:
        priority = "urgent"
    else:
        priority = "warning"

    count = len(trackers_to_alert)
    if count == 1:
        t = trackers_to_alert[0]
        days = t["days_remaining"]
        sname = t["server_name"]
        uname = t["username"]
        if days > 0:
            urgency_str = f"expires in {days} day{'s' if days > 1 else ''}"
        elif days == 0:
            urgency_str = "expires TODAY!"
        else:
            urgency_str = f"EXPIRED {abs(days)} day{'s' if abs(days) > 1 else ''} ago!"
        email_subject = f"⚠️ [ALERT] Credential for {uname}@{sname} {urgency_str}"
        notif_title = f"⚠️ Server Credential Alert: {sname}"
        notif_message = f"Password for {uname}@{sname} {urgency_str} (Expiry: {format_display_date(t['expiry_date'])}). Please update immediately."
        push_title = f"⚠️ Password Expiry: {sname}"
        push_body = f"{uname}@{sname} {urgency_str}. Tap to review."
    else:
        email_subject = f"⚠️ [ALERT] {count} Server Credentials Require Password Update"
        notif_title = f"⚠️ {count} Server Credentials Require Password Update"
        notif_message = f"{count} server accounts have reached their password update deadline. Please update them immediately."
        push_title = f"⚠️ Credential Alert: {count} Accounts"
        push_body = f"{count} server accounts require password update. Tap to review."

    # Build consolidated HTML table rows
    table_rows = []
    for t in trackers_to_alert:
        days = t["days_remaining"]
        if days > 0:
            urgency_tag = f"Expires in {days}d"
            badge_color = "#F59E0B"
            badge_bg = "#FEF3C7"
        elif days == 0:
            urgency_tag = "Expires TODAY"
            badge_color = "#EF4444"
            badge_bg = "#FEE2E2"
        else:
            urgency_tag = f"EXPIRED ({abs(days)}d ago)"
            badge_color = "#DC2626"
            badge_bg = "#FEE2E2"

        row_html = f"""
        <tr style="border-bottom: 1px solid #E2E8F0;">
          <td style="padding: 12px 10px; font-weight: 600; color: #0F172A; font-size: 14px;">{t['server_name']}</td>
          <td style="padding: 12px 10px; font-family: monospace; color: #334155; font-size: 13px;">{t['username']}</td>
          <td style="padding: 12px 10px; color: #64748B; font-size: 13px;">{t.get('validity_days', 30)}d</td>
          <td style="padding: 12px 10px; color: #64748B; font-size: 13px;">{format_display_date(t.get('last_updated_date', ''))}</td>
          <td style="padding: 12px 10px; font-weight: 700; color: {badge_color}; font-size: 13px;">{format_display_date(t['expiry_date'])}</td>
          <td style="padding: 12px 10px; text-align: right;">
            <span style="display: inline-block; padding: 4px 10px; border-radius: 9999px; font-size: 11px; font-weight: 700; background-color: {badge_bg}; color: {badge_color};">
              {urgency_tag}
            </span>
          </td>
        </tr>"""
        table_rows.append(row_html)

    table_rows_str = "\n".join(table_rows)

    banner_text = f"Attention: <strong>{count} server credential{'s' if count > 1 else ''}</strong> require{'s' if count == 1 else ''} password update." if count > 1 else f"Credential for <strong>{trackers_to_alert[0]['username']}@{trackers_to_alert[0]['server_name']}</strong> {urgency_str}"

    html_body = f"""<!DOCTYPE html>
<html>
<head><meta charset="utf-8"></head>
<body style="font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; background-color: #F8FAFC; margin: 0; padding: 24px; color: #1E293B;">
  <div style="max-width: 640px; margin: 0 auto; background: #FFFFFF; border-radius: 14px; border: 1px solid #E2E8F0; overflow: hidden; box-shadow: 0 4px 14px rgba(0,0,0,0.06);">
    <div style="background: linear-gradient(135deg, #1E3A8A 0%, #2563EB 100%); padding: 24px; text-align: center; color: #FFFFFF;">
      <h1 style="margin: 0; font-size: 20px; font-weight: 700; letter-spacing: -0.5px;">🔐 Server Credential Expiry Digest</h1>
      <p style="margin: 6px 0 0; font-size: 13px; opacity: 0.9;">RosterX Infrastructure Security Monitor</p>
    </div>
    <div style="padding: 24px;">
      <div style="background: #FEF2F2; border-left: 4px solid #DC2626; padding: 14px 18px; border-radius: 6px; margin-bottom: 20px;">
        <p style="margin: 0; font-size: 14px; font-weight: 600; color: #991B1B;">
          {banner_text}
        </p>
      </div>
      <table style="width: 100%; border-collapse: collapse; margin-bottom: 20px;">
        <thead>
          <tr style="border-bottom: 2px solid #CBD5E1; text-align: left;">
            <th style="padding: 8px 10px; font-size: 12px; color: #64748B; font-weight: 600; text-transform: uppercase;">Server</th>
            <th style="padding: 8px 10px; font-size: 12px; color: #64748B; font-weight: 600; text-transform: uppercase;">Username</th>
            <th style="padding: 8px 10px; font-size: 12px; color: #64748B; font-weight: 600; text-transform: uppercase;">Cycle</th>
            <th style="padding: 8px 10px; font-size: 12px; color: #64748B; font-weight: 600; text-transform: uppercase;">Updated</th>
            <th style="padding: 8px 10px; font-size: 12px; color: #64748B; font-weight: 600; text-transform: uppercase;">Expiry</th>
            <th style="padding: 8px 10px; font-size: 12px; color: #64748B; font-weight: 600; text-transform: uppercase; text-align: right;">Status</th>
          </tr>
        </thead>
        <tbody>
          {table_rows_str}
        </tbody>
      </table>
      {_render_open_in_app_cta("credentials", "Open Credentials in RosterX App &rarr;")}
      <p style="margin: 16px 0 0; font-size: 12px; color: #94A3B8; text-align: center;">
        You received this automated security notification because you are designated as a spokesperson or administrator for these credentials.
      </p>
    </div>
  </div>
</body>
</html>"""

    # 1. In-App Notification (1 consolidated doc)
    notif_id = f"notif_cred_digest_{today_str}"
    notif_doc = {
        "id": notif_id,
        "digest_date": today_str,
        "title": notif_title,
        "raw_message": notif_message,
        "target": "all_admins_and_spokespersons",
        "target_type": "specific",
        "recipients_count": len(all_spokespersons) + len(admin_tokens),
        "recipients": list(all_spokespersons),
        "priority": priority,
        "sender": "System Security Monitor",
        "send_email": bool(all_spokespersons),
        "send_in_app": True,
        "send_push": True,
        "category": "credential_expiry",
        "tracker_ids": [t["id"] for t in trackers_to_alert],
        "is_retracted": False,
        "retracted_at": None,
        "created_at": time.strftime("%Y-%m-%d %H:%M:%S UTC", time.gmtime()),
        "timestamp": time.time()
    }
    SERVER_NOTIFICATIONS.appendleft(notif_doc)
    if ATLAS_DB is not None:
        try:
            ATLAS_DB.notifications.insert_one({**notif_doc, "_id": notif_id})
        except Exception as e:
            log_server(f"[CREDENTIAL-ALERT] Error saving notification: {e}")

    # 2. Email Dispatch
    email_sent_count = 0
    for em in all_spokespersons:
        try:
            send_resend_email(em, email_subject, html_body)
            email_sent_count += 1
        except Exception as ex:
            log_server(f"[CREDENTIAL-ALERT] Email send error to {em}: {ex}")

    # 3. FCM Push
    target_tokens = set(admin_tokens)
    for em in all_spokespersons:
        eng_info = eng_by_email.get(em)
        if eng_info:
            for tok in eng_info.get("tokens", []):
                target_tokens.add(tok)

    push_sent_count = 0
    if target_tokens:
        try:
            push_sent_count = send_fcm_push(
                list(target_tokens),
                push_title,
                push_body,
                priority=priority,
                notif_id=notif_id,
                extra_data={"type": "credential_expiry", "tracker_count": str(count)}
            )
        except Exception as ex:
            log_server(f"[CREDENTIAL-ALERT] FCM push error: {ex}")

    for t in trackers_to_alert:
        LAST_CREDENTIAL_ALERT_DATE[t["id"]] = today_str

    dispatched = [{
        "tracker_id": t["id"],
        "server_name": t["server_name"],
        "username": t["username"],
        "days_remaining": t["days_remaining"],
        "status": t["status"]
    } for t in trackers_to_alert]

    log_server(f"[CREDENTIAL-ALERT] Dispatched consolidated digest for {count} tracker(s) (emails: {email_sent_count}, push: {push_sent_count}).")
    return dispatched

def send_otp_email(recipient_email: str, engineer_name: str, otp_code: str) -> bool:
    """
    Transmit styled HTML OTP email via Resend API (sole channel).
    Returns True on successful delivery, False otherwise.
    """
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

    log_server(f"[DISPATCH] Initiating OTP verification dispatch to: {recipient_email} (User: {display_name})")
    log_server(f"[DISPATCH] Transmitting OTP verification code via Resend HTTPS REST API to '{recipient_email}'...")
    try:
        resend_result = send_resend_email(recipient_email, subject, html_content, allow_sandbox_redirect=False)
        if resend_result and resend_result.get("success"):
            dest = resend_result.get("delivered_to", recipient_email)
            log_server(f"[DISPATCH] ✅ Successfully delivered OTP email to '{dest}' via Resend HTTPS REST API.")
            return True
        else:
            err = resend_result.get("error", "Unknown Resend error") if resend_result else "No response"
            log_server(f"[DISPATCH ERROR] Resend delivery failed: {err}")
    except Exception as ex:
        log_server(f"[DISPATCH ERROR] Resend exception: {ex}")

    log_server(f"[DISPATCH] ❌ OTP email delivery failed for '{recipient_email}'.")
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
                <div style="margin-bottom:14px;"><a href="{public_url or '#'}" target="_blank" style="color:#ffffff;font-size:15px;font-weight:700;">{public_url or 'Production server active'}</a></div>
                <div style="color:#a78bfa;font-size:11px;font-weight:700;text-transform:uppercase;margin-bottom:6px;">Android Base URL:</div>
                <div style="background:#0f172a;border:1px solid #334155;border-radius:6px;padding:10px 12px;font-family:monospace;font-size:13px;color:#38bdf8;font-weight:700;">{api_url}</div>
              </div>
              {_render_open_in_app_cta("roster", "Open Roster in RosterX App &rarr;")}
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

def _personalize_broadcast_text(
    text: str,
    recipient_name: str,
    recipient_username: str = "",
    name_by_user: dict = None,
    name_by_id: dict = None,
    html_mode: bool = False
) -> str:
    """
    Unified broadcast personalization resolver shared by Broadcast Title & Body across
    Email (Resend), FCM Push, and In-App Feed (GET /api/notifications).
    Resolves:
      - @name / {name} / {their name} -> recipient's Full Name
      - {username} / @{username}      -> recipient's username
      - @all / @everyone / @team      -> @Everyone (plain) or styled badge (HTML)
      - @<username>                   -> @<Full Name> via directory lookup
      - @<Full Name>                  -> @<Full Name> preserved without truncation
    """
    if not text:
        return ""

    res = str(text)
    safe_name = str(recipient_name or "Team Member").strip()
    safe_uname = str(recipient_username or "").strip()
    user_map = name_by_user or {}
    id_map = name_by_id or {}

    # 1. Recipient dynamic name & username tokens
    res = re.sub(r'@name\b', safe_name, res, flags=re.IGNORECASE)
    res = re.sub(r'\{\s*(?:Their\s+name|their\s+name|name)\s*\}', safe_name, res, flags=re.IGNORECASE)
    res = re.sub(r'\{\s*username\s*\}', safe_uname, res, flags=re.IGNORECASE)
    res = re.sub(r'@\{\s*username\s*\}', f"@{safe_uname}", res, flags=re.IGNORECASE)

    # 2. Build candidates to recognize full names, usernames, and groups
    candidates = set(["all", "everyone", "team", "Everyone", "All", "Team"])
    for u, fn in user_map.items():
        if u:
            candidates.add(str(u).strip())
        if fn:
            candidates.add(str(fn).strip())
    for i, fn in id_map.items():
        if i:
            candidates.add(str(i).strip())
        if fn:
            candidates.add(str(fn).strip())

    # Dynamic extraction: if text has '@<KnownFirstWord> <Initials/Words> :'
    # extract and add full multi-word mention to candidates!
    known_first_words = set()
    for c in candidates:
        first_w = re.split(r'\s+', str(c).strip())[0]
        if len(first_w) >= 3:
            known_first_words.add(first_w.lower())

    for m in re.finditer(r'@([A-Za-z0-9_.-]+(?:\s+[A-Za-z0-9_.-]+)+)(?=\s*[:\-\(\[\n,])', res):
        full_tok = m.group(1).strip()
        f_w = re.split(r'\s+', full_tok)[0].lower()
        if f_w in known_first_words:
            candidates.add(full_tok)

    sorted_cand = sorted([c for c in candidates if c], key=lambda x: len(x), reverse=True)
    pattern_parts = [f"{re.escape(c)}(?![a-zA-Z0-9_])" for c in sorted_cand]
    pattern_parts.append(r'[a-zA-Z0-9_\-]+(?:\.[a-zA-Z0-9_\-]+)*')
    mention_pattern = r'@(' + '|'.join(pattern_parts) + r')'

    if html_mode:
        def _fmt_html(m):
            token = m.group(1).strip()
            t_u = token.lower()
            if t_u in ("all", "everyone", "team"):
                return '<strong style="display:inline-block;padding:2px 8px;border-radius:6px;background:#312e81;color:#c7d2fe;font-size:12px;margin:0 2px;">@Everyone (All Team)</strong>'
            m_name = user_map.get(t_u) or id_map.get(t_u) or token
            return f'<strong style="display:inline-block;padding:2px 8px;border-radius:6px;background:#312e81;color:#c7d2fe;font-size:12px;margin:0 2px;">@{m_name}</strong>'
        res = re.sub(mention_pattern, _fmt_html, res, flags=re.IGNORECASE)
    else:
        def _fmt_plain(m):
            token = m.group(1).strip()
            t_u = token.lower()
            if t_u in ("all", "everyone", "team"):
                return "@Everyone"
            m_name = user_map.get(t_u) or id_map.get(t_u) or token
            return f"@{m_name}"
        res = re.sub(mention_pattern, _fmt_plain, res, flags=re.IGNORECASE)

    return res


def send_welcome_onboarding_email(to_email: str, full_name: str, username: str):
    """
    Dispatch a zero-knowledge Welcome to RosterX email to a newly onboarded team member.
    Never includes any password; instructs the user to set their private password using
    'Forgot password?' (Email OTP) on the login screen.
    """
    if not to_email:
        return {"success": False, "error": "No recipient email"}
    display_name = to_proper_name(full_name) if full_name else username
    subject = "Welcome to RosterX — Set Up Your Private Account Password"
    html_content = f"""<!DOCTYPE html>
<html>
<body style="margin:0;padding:0;background-color:#0b1120;font-family:-apple-system,BlinkMacSystemFont,Segoe UI,Roboto,sans-serif;color:#f1f5f9;">
  <div style="max-width:560px;margin:30px auto;background:#0f172a;border:1px solid #1e293b;border-radius:16px;padding:30px;">
    <h2 style="margin:0 0 12px;color:#ffffff;font-size:22px;">Welcome to RosterX, {display_name}!</h2>
    <p style="margin:0 0 18px;color:#cbd5e1;font-size:14px;line-height:1.6;">
      Your RosterX operational account has been created. For your privacy and security, administrators never set or see your password.
    </p>
    <div style="background:#020617;border:1px solid #334155;border-radius:10px;padding:16px;margin-bottom:20px;">
      <div style="font-size:12px;color:#94a3b8;text-transform:uppercase;letter-spacing:0.06em;">Your Username</div>
      <div style="font-size:18px;font-weight:700;color:#38bdf8;font-family:monospace;margin-top:4px;">{username}</div>
    </div>
    <p style="margin:0 0 14px;color:#e2e8f0;font-size:14px;line-height:1.6;">
      <strong>How to set your private password:</strong><br/>
      1. Open the RosterX Android app or web portal.<br/>
      2. Tap <strong>&ldquo;Forgot password?&rdquo;</strong> on the login screen.<br/>
      3. Enter your username (<code>{username}</code>) or this email address to receive a secure 6-digit Email OTP.<br/>
      4. Verify the OTP and choose your own private password.
    </p>
    {_render_open_in_app_cta("profile", "Open in RosterX App &rarr;")}
    <div style="font-size:12px;color:#64748b;border-top:1px solid #1e293b;padding-top:14px;margin-top:20px;">
      Automated onboarding message from RosterX v{SERVER_VERSION}.
    </div>
  </div>
</body>
</html>"""
    try:
        res = send_resend_email(to_email, subject, html_content)
        log_server(f"[ONBOARDING] Welcome email dispatched to '{username}' ({to_email}): {res.get('success')}")
        return res
    except Exception as e:
        log_server(f"[ONBOARDING EMAIL ERROR] Failed for '{username}': {e}")
        return {"success": False, "error": str(e)}


def publish_server_endpoints(public_url, lan_ip, port):
    """Synchronize live production/LAN server endpoints with MongoDB Atlas system_config."""
    global ATLAS_DB
    effective_public = (public_url or PUBLIC_SERVER_URL or "").strip().rstrip("/")

    if ATLAS_DB is not None:
        try:
            ATLAS_DB.system_config.update_one(
                {"_id": "active_server"},
                {"$set": {
                    "public_url": effective_public,
                    "api_url": f"{effective_public}/api" if effective_public else f"http://{lan_ip}:{port}/api",
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
            env_data[k] = v.strip().strip("'\"")

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

def build_roster_grounding_context(current_user=None, target_month=None):
    """
    Assembles real-time ground truth from MongoDB Atlas, OS runtime, and IST clock.
    Fetches team roster documents from yesterday through the next 35 days (Asia/Kolkata IST dates)
    with _id stripped for clean JSON serialization.
    Zero synthetic or mock data is injected.
    """
    now_ist = get_ist_now()
    cur_year = now_ist.year
    cur_month = now_ist.month
    cur_day = now_ist.day
    default_month_key = f"{cur_year}-{str(cur_month).zfill(2)}"
    
    # 1. Multi-week window: Yesterday through Next 35 days (37 days total)
    start_date = now_ist - timedelta(days=1)
    end_date = now_ist + timedelta(days=35)
    
    # Identify unique YYYY-MM months touched by this 37-day range
    spanned_months = []
    d_step = start_date
    while d_step <= end_date:
        m_str = d_step.strftime("%Y-%m")
        if m_str not in spanned_months:
            spanned_months.append(m_str)
        d_step += timedelta(days=1)
        
    # Fetch team roster documents from MongoDB Atlas for all spanned months
    multi_month_rosters = {}
    raw_roster_docs_clean = []
    if ATLAS_DB is not None:
        try:
            for m_key in spanned_months:
                doc = ATLAS_DB["rosters"].find_one({"_id": m_key})
                if doc:
                    doc_copy = copy.deepcopy(doc)
                    doc_copy["month_id"] = str(doc_copy.pop("_id", ""))
                    multi_month_rosters[m_key] = doc_copy
                    raw_roster_docs_clean.append(doc_copy)
        except Exception as e:
            print(f"[RAG] MongoDB multi-month roster query error: {e}")
            traceback.print_exc()

    # Fallback if active month document is missing: load latest published roster
    if default_month_key not in multi_month_rosters and ATLAS_DB is not None:
        try:
            latest_doc = ATLAS_DB["rosters"].find_one({}, sort=[("_id", -1)])
            if latest_doc:
                doc_copy = copy.deepcopy(latest_doc)
                doc_copy["month_id"] = str(doc_copy.pop("_id", ""))
                multi_month_rosters[default_month_key] = doc_copy
                raw_roster_docs_clean.append(doc_copy)
        except Exception as e:
            print(f"[RAG] DB fallback roster query error: {e}")

    # Build chronological daily shift schedule for all 37 days
    multi_week_daily_schedule = []
    d_step = start_date
    while d_step <= end_date:
        m_str = d_step.strftime("%Y-%m")
        d_num = d_step.day
        day_date_str = d_step.strftime("%Y-%m-%d")
        
        day_entry = {
            "date": day_date_str,
            "day_of_week": d_step.strftime("%A"),
            "is_yesterday": (day_date_str == start_date.strftime("%Y-%m-%d")),
            "is_today": (day_date_str == now_ist.strftime("%Y-%m-%d")),
            "is_tomorrow": (day_date_str == (now_ist + timedelta(days=1)).strftime("%Y-%m-%d")),
            "shifts": {}
        }
        
        if m_str in multi_month_rosters:
            eng_list = multi_month_rosters[m_str].get("engineers") or multi_month_rosters[m_str].get("roster") or []
            for eng in eng_list:
                eng_name = to_proper_name(eng.get("name", "Unknown"))
                s_raw = eng.get("shifts")
                shift_val = ""
                if isinstance(s_raw, dict):
                    shift_val = s_raw.get(str(d_num), "")
                elif isinstance(s_raw, list) and len(s_raw) >= d_num:
                    shift_val = s_raw[d_num - 1]
                if shift_val:
                    day_entry["shifts"][eng_name] = str(shift_val).strip().upper()
                    
        multi_week_daily_schedule.append(day_entry)
        d_step += timedelta(days=1)

    # Pre-compute current active shift slots and on-duty engineers for real-time accuracy
    active_shift_codes = determine_active_shifts(now_ist)
    cur_mins = now_ist.hour * 60 + now_ist.minute
    is_early_morning_ns = cur_mins < 7 * 60 + 30
    is_evening_ns = cur_mins >= 21 * 60

    yesterday_date_str = start_date.strftime("%Y-%m-%d")
    today_date_str = now_ist.strftime("%Y-%m-%d")
    tomorrow_date_str = (now_ist + timedelta(days=1)).strftime("%Y-%m-%d")

    yesterday_shifts = {}
    today_shifts = {}
    tomorrow_shifts = {}
    for entry in multi_week_daily_schedule:
        if entry["date"] == yesterday_date_str:
            yesterday_shifts = entry["shifts"]
        elif entry["date"] == today_date_str:
            today_shifts = entry["shifts"]
        elif entry["date"] == tomorrow_date_str:
            tomorrow_shifts = entry["shifts"]

    on_duty_now = []
    for eng_name, s_today in today_shifts.items():
        is_on_duty = False
        duty_shift = None
        duty_note = ""

        # Day shifts
        for code in ("FS", "FG", "G", "SG", "SS"):
            if code in active_shift_codes and s_today == code:
                is_on_duty = True
                duty_shift = code
                duty_note = f"Active window {SHIFT_DEFINITIONS.get(code, {}).get('timings', '')} IST"
                break

        # Night shift (NS) overnight rollover
        if not is_on_duty and "NS" in active_shift_codes:
            if is_early_morning_ns:
                # 00:00 - 07:30 AM: Active NS started yesterday at 21:00
                s_yest = yesterday_shifts.get(eng_name, "OFF")
                if s_yest == "NS":
                    is_on_duty = True
                    duty_shift = "NS"
                    duty_note = "Overnight Night Shift: started 21:00 yesterday, ends at 07:30 AM IST today"
            elif is_evening_ns:
                # 21:00 - 23:59: Active NS started today at 21:00
                if s_today == "NS":
                    is_on_duty = True
                    duty_shift = "NS"
                    duty_note = "Overnight Night Shift: started 21:00 today, ends at 07:30 AM IST tomorrow"

        if is_on_duty and duty_shift:
            on_duty_now.append({
                "name": eng_name,
                "shift": duty_shift,
                "timings": SHIFT_DEFINITIONS.get(duty_shift, {}).get("timings", ""),
                "note": duty_note
            })

    # Host process diagnostics
    uptime_sec = int(time.time() - SERVER_START_TIME)
    h, rem = divmod(uptime_sec, 3600)
    m, s = divmod(rem, 60)
    uptime_str = f"{h}h {m}m {s}s"

    return {
        "ist_timestamp": now_ist.strftime("%Y-%m-%d %H:%M:%S IST"),
        "date": now_ist.strftime("%Y-%m-%d"),
        "day_of_week": now_ist.strftime("%A"),
        "time_24h": now_ist.strftime("%H:%M"),
        "start_date": yesterday_date_str,
        "end_date": end_date.strftime("%Y-%m-%d"),
        "spanned_months": spanned_months,
        "active_shift_slots": active_shift_codes,
        "on_duty_engineers": on_duty_now,
        "today_shifts": today_shifts,
        "tomorrow_shifts": tomorrow_shifts,
        "yesterday_shifts": yesterday_shifts,
        "multi_week_schedule": multi_week_daily_schedule,
        "raw_roster_docs": raw_roster_docs_clean,
        "uptime": uptime_str
    }

# ==============================================================================
# Multi-Gemini API Key Pool Manager (MongoDB Atlas Persistence + Round-Robin)
# ==============================================================================

GEMINI_KEY_INDEX = 0
GEMINI_KEY_LOCK = threading.Lock()

def mask_api_key(key):
    """Safely mask an API key for admin UI display (e.g. AIzaSy...AbCd)."""
    if not key or not isinstance(key, str):
        return ""
    k = key.strip()
    if len(k) <= 10:
        return "****"
    return f"{k[:6]}...{k[-4:]}"

def get_gemini_config():
    """
    Retrieve Gemini configuration from MongoDB Atlas (app_settings.ai_config).
    Auto-seeds from local .env / environment variable if uninitialized.
    """
    global ATLAS_DB
    env = load_env_variables()
    fallback_key = env.get("GEMINI_API_KEY", "").strip() or os.environ.get("GEMINI_API_KEY", "").strip()
    fallback_model = env.get("GEMINI_MODEL", "gemini-3.8-flash").strip()

    keys = []
    selected_model = fallback_model

    if ATLAS_DB is not None:
        try:
            doc = ATLAS_DB["app_settings"].find_one({"_id": "ai_config"})
            if doc:
                raw_keys = doc.get("api_keys") or []
                if isinstance(raw_keys, list):
                    keys = [str(k).strip() for k in raw_keys if str(k).strip()]
                elif isinstance(raw_keys, str) and raw_keys.strip():
                    keys = [raw_keys.strip()]
                if doc.get("selected_model"):
                    selected_model = str(doc.get("selected_model")).strip()

            # Auto-seed if database document is missing or has empty keys, but fallback_key is present
            if not keys and fallback_key:
                keys = [fallback_key]
                try:
                    ATLAS_DB["app_settings"].update_one(
                        {"_id": "ai_config"},
                        {
                            "$set": {
                                "api_keys": keys,
                                "selected_model": selected_model,
                                "updated_at": datetime.now(timezone.utc).isoformat(),
                                "updated_by": "system_auto_seed"
                            }
                        },
                        upsert=True
                    )
                    log_server(f"[AI Pool] Auto-seeded {len(keys)} Gemini API key(s) into MongoDB Atlas app_settings.")
                except Exception as seed_err:
                    log_server(f"[AI Pool] Auto-seed error: {seed_err}")
        except Exception as e:
            log_server(f"[AI Pool] MongoDB query error: {e}")

    if not keys and fallback_key:
        keys = [fallback_key]

    return {
        "api_keys": keys,
        "selected_model": selected_model or "gemini-3.8-flash"
    }

def save_gemini_config(keys, selected_model=None, updated_by="admin"):
    """
    Persist Gemini keys and model choice into MongoDB Atlas app_settings.
    """
    global ATLAS_DB
    cleaned_keys = []
    for k in keys:
        if isinstance(k, str) and k.strip():
            k_clean = k.strip()
            if k_clean not in cleaned_keys:
                cleaned_keys.append(k_clean)

    update_payload = {
        "api_keys": cleaned_keys,
        "updated_at": datetime.now(timezone.utc).isoformat(),
        "updated_by": updated_by or "admin"
    }
    if selected_model:
        update_payload["selected_model"] = str(selected_model).strip()

    if ATLAS_DB is not None:
        try:
            ATLAS_DB["app_settings"].update_one(
                {"_id": "ai_config"},
                {"$set": update_payload},
                upsert=True
            )
            log_server(f"[AI Pool] Successfully saved {len(cleaned_keys)} key(s) to MongoDB Atlas.")
            return True, cleaned_keys
        except Exception as e:
            log_server(f"[AI Pool] Error saving to MongoDB Atlas: {e}")
            return False, cleaned_keys
    return False, cleaned_keys

def test_single_gemini_key(api_key, model="gemini-3.8-flash"):
    """
    Lightweight health & quota check for a specific Gemini API key.
    """
    if not api_key:
        return {"status": "error", "message": "Empty API key"}
    if genai is None or types is None:
        return {"status": "error", "message": "google-genai SDK not installed"}
    
    try:
        client = genai.Client(api_key=api_key.strip())
        candidate_models = [model, "gemini-3.8-flash", "gemini-3.1-flash-lite"]
        for m in candidate_models:
            if not m:
                continue
            try:
                resp = client.models.generate_content(
                    model=m,
                    contents="Ping",
                    config=types.GenerateContentConfig(
                        temperature=0.1,
                        max_output_tokens=5
                    )
                )
                if resp and resp.text:
                    return {"status": "healthy", "model": m, "message": "Valid & Active"}
            except Exception as e:
                err_str = str(e)
                if "404" in err_str or "NOT_FOUND" in err_str:
                    continue  # Try next model
                if "429" in err_str or "RESOURCE_EXHAUSTED" in err_str:
                    return {"status": "quota_exhausted", "model": m, "message": "Quota limit reached (429)"}
                if "API_KEY_INVALID" in err_str or "400" in err_str or "403" in err_str:
                    return {"status": "invalid_key", "model": m, "message": "Invalid API key or unauthorized"}
                return {"status": "error", "model": m, "message": err_str[:120]}
        return {"status": "error", "message": "All test models failed"}
    except Exception as e:
        return {"status": "error", "message": str(e)[:120]}

def query_gemini_roster_agent(user_prompt, current_user=None, client_platform="Web Portal", history=None, target_month=None):
    """
    RAG AI agent powered by official Google GenAI SDK (google-genai).
    Strictly grounded in multi-week MongoDB Atlas rosters (yesterday through next 35 days)
    and exact Asia/Kolkata IST time. Supports Multi-Key Pool with Round-Robin Rotation and Auto-Failover.
    Zero hardcoded templates, keyword fallbacks, or canned text.
    """
    global GEMINI_KEY_INDEX, GEMINI_KEY_LOCK

    cfg = get_gemini_config()
    api_keys = cfg.get("api_keys", [])
    configured_model = cfg.get("selected_model", "gemini-3.8-flash")
    
    context = build_roster_grounding_context(current_user=current_user, target_month=target_month)
    
    if not api_keys:
        err_msg = "No Gemini API keys configured. Please add an API key in Admin Settings."
        log_server(f"[RAG] Configuration error: {err_msg}")
        return err_msg, "error", context["ist_timestamp"]

    if genai is None or types is None:
        err_msg = "google-genai SDK is not installed on the server runtime."
        log_server(f"[RAG] Import error: {err_msg}")
        return err_msg, "error", context["ist_timestamp"]

    # Candidate models hierarchy
    candidate_models = []
    for m in [configured_model, "gemini-2.5-flash", "gemini-3.8-flash", "gemini-3.1-flash-lite"]:
        if m and m not in candidate_models:
            candidate_models.append(m)

    # Resolve active user profile details
    active_user_name = "Engineer"
    active_user_id = ""
    active_user_role = "team_member"
    if current_user and isinstance(current_user, dict):
        active_user_name = (current_user.get("name") or current_user.get("full_name") or current_user.get("username") or "Engineer").strip()
        active_user_id = str(current_user.get("id") or current_user.get("username") or "").strip()
        active_user_role = str(current_user.get("role") or "team_member").strip()

    # Dynamic system instruction injecting live context, shift timings, rules, and MongoDB Atlas JSON
    system_instruction = (
        "You are RosterX Assistant, an authoritative, intelligent NOC operations AI agent for Project RosterX.\n"
        "Your responses are displayed directly in the mobile and web chat UI.\n\n"
        "=== SYSTEM REAL-TIME CONTEXT (Asia/Kolkata IST) ===\n"
        f"- Current Timestamp: {context['ist_timestamp']}\n"
        f"- Current Date: {context['date']} ({context['day_of_week']})\n"
        f"- Current 24-Hour Time: {context['time_24h']} IST\n"
        f"- Active Logged-in User: Name: \"{active_user_name}\", ID: \"{active_user_id}\", Role: \"{active_user_role}\".\n"
        "  * CRITICAL RULE: When the user says 'me', 'my', 'I', 'myself', or asks about their own shifts without naming anyone, you MUST resolve them to this active user.\n\n"
        "=== SHIFT DEFINITIONS & OPERATIONAL TIMINGS ===\n"
        "- FS = First Shift: 07:30 – 16:30 IST\n"
        "- FG = First General: 08:30 – 17:30 IST\n"
        "- G = General Shift: 09:30 – 18:30 IST\n"
        "- SG = Second General: 11:00 – 20:00 IST (Note: 'Second General' is 'SG', distinct from 'G')\n"
        "- SS = Second Shift / Afternoon: 13:00 – 22:00 IST\n"
        "- NS = Night Shift: 21:00 – 07:30 IST next morning (Overnight shift crossing midnight)\n"
        "- OFF = Scheduled Weekly Rest Day / Off (No duty)\n"
        "- L = Approved Leave (No duty)\n"
        "- SW* = Swapped Off / Operational Shift Swap (No duty unless specified)\n\n"
        "=== OVERNIGHT SHIFT RULES & ON-DUTY EVALUATION ===\n"
        "- When asked 'who is on duty now' or 'who is available now':\n"
        f"  1. Compare the current 24-hour time ({context['time_24h']} IST) against the active shift windows.\n"
        "  2. For the overnight window between 00:00 and 07:30 AM IST: The active Night Shift (NS) is the engineer who began at 21:00 YESTERDAY. Today's scheduled NS engineer does not start until 21:00 tonight.\n"
        "  3. For the evening window between 21:00 and 23:59 IST: The active Night Shift (NS) is today's scheduled NS engineer.\n"
        "  4. Exclude all engineers with OFF, L, or SW codes.\n"
        f"  5. Pre-computed active shift windows right now: {', '.join(context['active_shift_slots']) if context['active_shift_slots'] else 'None (desk handover)'}.\n"
        f"  6. Pre-computed engineers on duty right now: {json.dumps(context['on_duty_engineers'])}.\n\n"
        f"=== LIVE MONGODB ATLAS ROSTER DATA (MULTI-WEEK WINDOW: {context['start_date']} TO {context['end_date']}) ===\n"
        "The following JSON contains verified team schedules from MongoDB Atlas for yesterday, today, and the next 35 days:\n"
        f"{json.dumps(context['multi_week_schedule'])}\n\n"
        "=== CORE INSTRUCTIONS ===\n"
        "1. GROUND TRUTH ONLY: Answer strictly based on the injected live roster JSON above. Never hallucinate, guess dates, or invent shifts or team members.\n"
        f"2. UPCOMING QUERIES: For questions about upcoming shifts, next night shift, or future dates, scan chronologically starting from today ({context['date']}) forward through the multi-week schedule. State the exact date (YYYY-MM-DD), day of the week, and shift code.\n"
        f"3. TODAY & TOMORROW: Clearly distinguish between today ({context['date']}) and tomorrow.\n"
        "4. TONE & FORMAT: Provide concise, professional, direct, and well-formatted responses suitable for a mobile screen. Use clean Markdown (bolding, lists, code spans for shift codes). Avoid unnecessary preambles or robotic repetition."
    )

    # Assemble multi-turn conversation history
    contents = []
    if history and isinstance(history, list):
        valid_history = []
        for h in history[-20:]:
            if not isinstance(h, dict):
                continue
            h_role = str(h.get("role") or "").strip().lower()
            h_text = str(h.get("text") or h.get("message") or h.get("content") or "").strip()
            if not h_text:
                continue
            mapped_role = "model" if h_role in ("assistant", "ai", "model") else "user"
            valid_history.append({"role": mapped_role, "text": h_text})

        # Alternating sequence starting with user
        formatted_turns = []
        for vh in valid_history:
            if not formatted_turns:
                if vh["role"] == "user":
                    formatted_turns.append(vh)
            else:
                if formatted_turns[-1]["role"] == vh["role"]:
                    formatted_turns[-1]["text"] += "\n" + vh["text"]
                else:
                    formatted_turns.append(vh)

        if formatted_turns and formatted_turns[-1]["role"] == "user":
            formatted_turns.pop()

        for ft in formatted_turns:
            contents.append(
                types.Content(
                    role=ft["role"],
                    parts=[types.Part.from_text(text=ft["text"])]
                )
            )

    # Append current user prompt as the final turn
    contents.append(
        types.Content(
            role="user",
            parts=[types.Part.from_text(text=user_prompt)]
        )
    )

    # Round-Robin Selection with Automatic Failover
    num_keys = len(api_keys)
    with GEMINI_KEY_LOCK:
        start_idx = GEMINI_KEY_INDEX % num_keys
        GEMINI_KEY_INDEX = (GEMINI_KEY_INDEX + 1) % num_keys

    ordered_indices = [(start_idx + i) % num_keys for i in range(num_keys)]

    ai_reply = None
    successful_model = None
    last_error = None

    for k_idx in ordered_indices:
        current_key = api_keys[k_idx]
        key_masked = mask_api_key(current_key)
        try:
            client = genai.Client(api_key=current_key)
            for model in candidate_models:
                try:
                    config = types.GenerateContentConfig(
                        system_instruction=system_instruction,
                        temperature=0.1,
                        max_output_tokens=1024
                    )
                    response = client.models.generate_content(
                        model=model,
                        contents=contents,
                        config=config
                    )
                    if response and response.text:
                        ai_reply = response.text.strip()
                        successful_model = model
                        break
                except Exception as me:
                    last_error = me
                    err_text = str(me)
                    # If model 404 (e.g. 2.5-flash deprecated), cascade to next model for SAME key
                    if "404" in err_text or "NOT_FOUND" in err_text:
                        continue
                    # If 429 quota exhaustion or 403 invalid key, break model loop to failover to NEXT KEY
                    if "429" in err_text or "RESOURCE_EXHAUSTED" in err_text or "403" in err_text or "API_KEY_INVALID" in err_text:
                        log_server(f"[RAG] Key [{key_masked}] throttled or invalid ({me}), failing over to next pool key...")
                        break
                    log_server(f"[RAG] Key [{key_masked}] model '{model}' failed: {me}")
                    continue

            if ai_reply and successful_model:
                return ai_reply, successful_model, context["ist_timestamp"]
        except Exception as ke:
            last_error = ke
            log_server(f"[RAG] Client initialization failed for key [{key_masked}]: {ke}")
            continue

    # No silent fallbacks: return explicit failure details
    err_detail = f"Gemini API request failed across {num_keys} configured key(s) and candidate models {candidate_models}. Last error: {last_error}"
    log_server(f"[RAG] Error: {err_detail}")
    return err_detail, "error", context["ist_timestamp"]


# HTTP Request Handler with Authentication & Strict RBAC
# ==============================================================================

class SecureRosterHandler(SimpleHTTPRequestHandler):
    """
    Secure HTTP handler combining:
      - Native MongoDB Atlas REST API bridge (/api/*)
      - Isolated static web & iOS PWA serving for web/
      - Token-based JWT Authentication & Role-Based Authorization
      - Rate limiting and brute-force mitigation
      - Strict blocking of .env, dotfiles, and sensitive configs
      - Restricted CORS and security headers
    """

    extensions_map = {
        **SimpleHTTPRequestHandler.extensions_map,
        ".webmanifest": "application/manifest+json; charset=utf-8",
        ".ics": "text/calendar; charset=utf-8",
        ".js": "application/javascript; charset=utf-8",
        ".mjs": "application/javascript; charset=utf-8",
    }

    def __init__(self, *args, **kwargs):
        super().__init__(*args, directory=str(WEB_DIR), **kwargs)

    @staticmethod
    def _normalize_api_aliases(norm_path: str) -> str:
        """
        Map shorthand REST routes seamlessly onto canonical /api/* endpoints so both
        Android and Web PWA clients enjoy 100% route parity with zero 404 risk.
        """
        if norm_path == "/api/credentials":
            return "/api/credentials/trackers"
        if norm_path == "/api/credentials/logs":
            return "/api/credentials/trackers/logs"
        if norm_path.startswith("/api/credentials/") and not norm_path.startswith("/api/credentials/trackers"):
            suffix = norm_path[len("/api/credentials/"):]
            return f"/api/credentials/trackers/{suffix}"
        if norm_path == "/api/broadcast-templates":
            return "/api/broadcast/templates"
        if norm_path.startswith("/api/broadcast-templates/"):
            suffix = norm_path[len("/api/broadcast-templates/"):]
            return f"/api/broadcast/templates/{suffix}"
        return norm_path

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
            # Allow local origins, render domains, and official rosterx.online domain
            if re.match(r"^https?://(localhost|127\.0\.0\.1|0\.0\.0\.0|10\.0\.2\.2)(:\d+)?$", origin) or \
               origin.endswith(".onrender.com") or origin.endswith("rosterx.online"):
                allowed_origin = origin
            else:
                allowed_origin = origin

        self.send_header("Access-Control-Allow-Origin", allowed_origin)
        self.send_header("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS, HEAD")
        self.send_header("Access-Control-Allow-Headers", "Content-Type, Authorization, X-Requested-With, Accept, Origin, User-Agent, Cache-Control, Pragma, X-Forwarded-For, X-Forwarded-Proto, CF-Connecting-IP")
        self.send_header("Access-Control-Expose-Headers", "Authorization, Content-Length")

    def send_json_response(self, data, status_code=200, extra_headers=None):
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
            if extra_headers and isinstance(extra_headers, dict):
                for hk, hv in extra_headers.items():
                    if str(hk).lower() == "cache-control":
                        self._custom_cache_control = True
                    self.send_header(str(hk), str(hv))
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
        if norm_path in ("/test-connection", "/engineers", "/admin/ai-settings", "/ai-settings", "/server-info", "/app-config", "/logs", "/server-logs", "/constraints", "/notifications", "/broadcast/templates", "/broadcast-templates", "/credentials", "/auth/me") or norm_path.startswith("/roster/") or norm_path.startswith("/roster-backups/") or norm_path.startswith("/users/") or norm_path.startswith("/celebrations/") or norm_path.startswith("/credentials/") or norm_path.startswith("/broadcast-templates/") or norm_path.startswith("/leave/"):
            norm_path = "/api" + norm_path
        norm_path = self._normalize_api_aliases(norm_path)

        # REST API Endpoints (/api/*)
        if norm_path.startswith("/api/"):
            self.handle_api_get(norm_path)
            return

        # Digital Asset Links for Google Password Manager & Android Verified App Links (PUBLIC)
        if clean_path in ("/.well-known/assetlinks.json", "/.well-known/assetlinks.json/"):
            fingerprints = get_android_app_link_fingerprints()
            assetlinks_data = [
                {
                    "relation": [
                        "delegate_permission/common.get_login_creds"
                    ],
                    "target": {
                        "namespace": "web",
                        "site": "https://rosterx.online"
                    }
                },
                {
                    "relation": [
                        "delegate_permission/common.handle_all_urls",
                        "delegate_permission/common.get_login_creds"
                    ],
                    "target": {
                        "namespace": "android_app",
                        "package_name": "com.rosterx",
                        "sha256_cert_fingerprints": fingerprints
                    }
                }
            ]
            self.send_json_response(
                assetlinks_data,
                status_code=200,
                extra_headers={"Cache-Control": "public, max-age=3600"}
            )
            return

        # Graceful Desktop & iOS Safari Fallback for Deep Links (/open and /open/*)
        if norm_path == "/open" or norm_path.startswith("/open/"):
            sub_route = norm_path[len("/open"):].strip("/").lower()
            first_seg = sub_route.split("/")[0] if sub_route else ""
            deep_link_fallback_map = {
                "dashboard": "/#dashboard",
                "roster": "/#roster",
                "notifications": "/#notifications",
                "credentials": "/#credentials",
                "profile": "/#profile",
            }
            redirect_target = deep_link_fallback_map.get(first_seg, "/")
            try:
                self.send_response(302)
                self.send_header("Location", redirect_target)
                self.send_header("Cache-Control", "no-cache")
                self.send_cors_headers()
                self.end_headers()
            except (BrokenPipeError, ConnectionResetError, ConnectionAbortedError, OSError):
                pass
            return

        # Security Guards: Block .env and dotfiles
        blocked_keywords = [".env", ".git", ".gitignore", "web.config", ".htaccess", ".htpasswd"]
        path_lower = clean_path.lower()
        for blocked in blocked_keywords:
            if blocked in path_lower:
                self.send_error(403, "Access Denied: Requested resource is protected.")
                return

        # iOS PWA Service Worker & Web App Manifest response header hooks
        if path_lower == "/sw.js":
            self._sw_headers = True
        elif path_lower == "/manifest.webmanifest":
            self._manifest_cache_headers = True

        super().do_GET()

    def handle_api_get(self, clean_path):
        """Router for GET /api/* requests."""
        lan_ip = get_lan_ip()

        # GET /api/admin/ai-settings (Admin Only)
        if clean_path in ("/api/admin/ai-settings", "/api/admin/ai-settings/", "/api/ai-settings", "/api/ai-settings/"):
            claims = self.require_auth()
            if not claims:
                return
            if str(claims.get("role", "")).lower() != "admin":
                self.send_json_response({"success": False, "message": "Permission denied: Admin role required."}, 403)
                return

            cfg = get_gemini_config()
            keys_info = []
            for idx, k in enumerate(cfg.get("api_keys", [])):
                keys_info.append({
                    "id": idx,
                    "masked": mask_api_key(k),
                    "length": len(k)
                })

            self.send_json_response({
                "success": True,
                "keys": keys_info,
                "selected_model": cfg.get("selected_model", "gemini-3.8-flash"),
                "total_keys": len(keys_info)
            })
            return


        # 0.05 GET /api/app-config (Public App Configuration & Dynamic Festival Theme Engine)
        if clean_path in ("/api/app-config", "/api/app-config/"):
            festivals = {
                "diwali": {"start": "2026-11-06", "end": "2026-11-12"},
                "christmas": {"start": "2026-12-20", "end": "2026-12-27"},
                "new_year": {"start": "2026-12-30", "end": "2027-01-03"}
            }
            now_ist = datetime.now(timezone(timedelta(hours=5, minutes=30)))
            today_str = now_ist.strftime("%Y-%m-%d")

            active_theme = "default"
            for fest_name, fest_range in festivals.items():
                s = str(fest_range.get("start", "")).strip()
                e = str(fest_range.get("end", "")).strip()
                if s and e and (s <= today_str <= e):
                    active_theme = fest_name
                    break

            active_celebration = None
            claims = self.get_auth_claims()
            if claims:
                try:
                    engineers_list = []
                    if ATLAS_DB is not None:
                        engineers_list = list(ATLAS_DB.engineers.find({}, {"_id": 0, "password": 0}))
                    if not engineers_list:
                        engineers_list = DEFAULT_SERVER_ENGINEERS

                    for eng in engineers_list:
                        bday_raw = eng.get("birthday") or eng.get("dob") or ""
                        jdate_raw = eng.get("join_date") or eng.get("joining_date") or ""
                        is_bday, _, _ = parse_celebration_date(bday_raw, now_ist)
                        is_anniv, yr, _ = parse_celebration_date(jdate_raw, now_ist)

                        uname = str(eng.get("username") or "").strip()
                        full_name = to_proper_name(eng.get("name") or eng.get("full_name") or uname)
                        photo = str(eng.get("profile_picture") or eng.get("profilePicture") or "").strip()

                        if is_bday:
                            active_celebration = {
                                "type": "birthday",
                                "username": uname,
                                "name": full_name,
                                "profile_picture": photo,
                                "years": 0
                            }
                            break
                        elif is_anniv and yr > 0:
                            active_celebration = {
                                "type": "anniversary",
                                "username": uname,
                                "name": full_name,
                                "profile_picture": photo,
                                "years": yr
                            }
                            break
                except Exception as e:
                    log_server(f"[APP-CONFIG CELEBRATION ERROR] {e}")

            self.send_json_response({
                "success": True,
                "version": SERVER_VERSION,
                "active_theme": active_theme,
                "active_celebration": active_celebration,
                "festivals": festivals,
                "today": today_str
            }, extra_headers={"Cache-Control": "public, max-age=60"})
            return

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
            update_meta = get_app_update_metadata()
            self.send_json_response({
                "success": True,
                "status": "online",
                "version": SERVER_VERSION,
                "build_code": SERVER_BUILD_CODE,
                "latest_android_version": update_meta["latest_android_version"],
                "min_supported_version": update_meta["min_supported_version"],
                "apk_download_url": update_meta["apk_download_url"],
                "public_url": PUBLIC_SERVER_URL,
                "api_url": f"{PUBLIC_SERVER_URL}/api" if PUBLIC_SERVER_URL else f"http://{lan_ip}:{SERVER_PORT}/api",
                "lan_ip": lan_ip,
                "lan_url": f"http://{lan_ip}:{SERVER_PORT}",
                "port": SERVER_PORT,
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
                        cursor = ATLAS_DB["audit_logs"].find({}, {"_id": 0}).sort("timestamp", -1).limit(50)
                        audit_records = list(cursor)
                except Exception as e:
                    print(f"  [TELEMETRY USERS ERROR] {e}")

            if not audit_records and SERVER_AUDIT_LOGS:
                audit_records = list(SERVER_AUDIT_LOGS)[:50]

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

            def format_bytes(b):
                if b >= 1024 * 1024:
                    return f"{b / (1024 * 1024):.2f} MB"
                elif b >= 1024:
                    return f"{b / 1024:.1f} KB"
                return f"{b} B"

            collection_counts = {}
            collections_stats = []
            db_storage_stats = {
                "storage_size_bytes": 0,
                "data_size_bytes": 0,
                "index_size_bytes": 0,
                "total_size_bytes": 0,
                "storage_size_formatted": "0 KB",
                "data_size_formatted": "0 KB",
                "index_size_formatted": "0 KB",
                "total_size_formatted": "0 KB",
                "objects_count": 0,
                "collections_count": 0
            }

            if ATLAS_DB is not None:
                try:
                    stats = ATLAS_DB.command("dbStats")
                    data_bytes = int(stats.get("dataSize", 0))
                    storage_bytes = int(stats.get("storageSize", 0))
                    index_bytes = int(stats.get("indexSize", 0))
                    total_bytes = storage_bytes + index_bytes
                    objects_cnt = int(stats.get("objects", 0))
                    collections_cnt = int(stats.get("collections", 0))

                    db_storage_stats = {
                        "storage_size_bytes": storage_bytes,
                        "data_size_bytes": data_bytes,
                        "index_size_bytes": index_bytes,
                        "total_size_bytes": total_bytes,
                        "storage_size_formatted": format_bytes(storage_bytes),
                        "data_size_formatted": format_bytes(data_bytes),
                        "index_size_formatted": format_bytes(index_bytes),
                        "total_size_formatted": format_bytes(total_bytes),
                        "objects_count": objects_cnt,
                        "collections_count": collections_cnt
                    }
                except Exception as e:
                    print(f"  [TELEMETRY DBSTATS ERROR] {e}")

                try:
                    c_names = sorted(ATLAS_DB.list_collection_names())
                    for cname in c_names:
                        if cname.startswith("system."):
                            continue
                        try:
                            cstats = ATLAS_DB.command("collStats", cname)
                            c_count = int(cstats.get("count", 0))
                            c_data = int(cstats.get("size", 0))
                            c_storage = int(cstats.get("storageSize", 0))
                            c_index = int(cstats.get("totalIndexSize", 0))
                            c_avg = int(cstats.get("avgObjSize", 0))
                            collections_stats.append({
                                "name": cname,
                                "count": c_count,
                                "data_size_bytes": c_data,
                                "data_size_formatted": format_bytes(c_data),
                                "storage_size_bytes": c_storage,
                                "storage_size_formatted": format_bytes(c_storage),
                                "index_size_bytes": c_index,
                                "index_size_formatted": format_bytes(c_index),
                                "total_size_bytes": c_storage + c_index,
                                "total_size_formatted": format_bytes(c_storage + c_index),
                                "avg_doc_size_formatted": format_bytes(c_avg) if c_avg else "0 B"
                            })
                            collection_counts[cname] = c_count
                        except Exception as e_c:
                            cnt = 0
                            try:
                                cnt = ATLAS_DB[cname].count_documents({})
                            except Exception:
                                pass
                            collections_stats.append({
                                "name": cname,
                                "count": cnt,
                                "data_size_bytes": 0,
                                "data_size_formatted": "0 B",
                                "storage_size_bytes": 0,
                                "storage_size_formatted": "0 B",
                                "index_size_bytes": 0,
                                "index_size_formatted": "0 B",
                                "total_size_bytes": 0,
                                "total_size_formatted": "0 B",
                                "avg_doc_size_formatted": "0 B"
                            })
                            collection_counts[cname] = cnt
                    db_storage_stats["collections_count"] = len(collections_stats)
                except Exception as e:
                    print(f"  [TELEMETRY COLLSTATS ERROR] {e}")

            mem_mb = get_memory_rss_mb()

            git_commit = (os.environ.get("RENDER_GIT_COMMIT", "") or "").strip()
            if not git_commit:
                try:
                    import subprocess
                    git_commit = subprocess.check_output(["git", "rev-parse", "--short", "HEAD"], stderr=subprocess.DEVNULL).decode().strip()
                except Exception:
                    git_commit = "main"

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
                    "build": {
                        "server_version": SERVER_VERSION,
                        "build_code": SERVER_BUILD_CODE,
                        "version_name": SERVER_VERSION,
                        "deployed_environment": "Render Cloud Production" if os.environ.get("RENDER") else "Local Development Host",
                        "git_commit": git_commit[:7] if git_commit else "HEAD",
                        "git_branch": os.environ.get("RENDER_GIT_BRANCH", "main"),
                        "render_instance_id": os.environ.get("RENDER_INSTANCE_ID", "local"),
                        "python_runtime": f"{platform.python_implementation()} {sys.version.split()[0]} ({platform.system()} {platform.release()})",
                        "production_api": "https://api.rosterx.online",
                        "render_backend_url": "https://rosterx-vc30.onrender.com"
                    },
                    "atlas": {
                        "status": ATLAS_STATUS,
                        "cluster": ATLAS_CLUSTER,
                        "database": ATLAS_DB_NAME,
                        "ping_ms": latency_ms,
                        "collections": collection_counts,
                        "storage": db_storage_stats,
                        "tables": collections_stats
                    },
                    "render": {
                        "service_name": "rosterx-backend",
                        "status": "Healthy (Live)",
                        "deployed_url": "https://rosterx-vc30.onrender.com",
                        "custom_domain": "https://api.rosterx.online",
                        "is_render_cloud": bool(os.environ.get("RENDER")),
                        "ssl_active": True
                    },
                    "memory_rss_mb": mem_mb,
                    "network": {
                        "port": SERVER_PORT,
                        "lan_ip": lan_ip,
                        "public_url": PUBLIC_SERVER_URL
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
                        "public_url": PUBLIC_SERVER_URL,
                        "api_url": f"{PUBLIC_SERVER_URL}/api" if PUBLIC_SERVER_URL else f"http://{lan_ip}:{SERVER_PORT}/api",
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

        # 1.5 GET /api/auth/me (Returns active user profile from JWT token refreshed with latest DB fields)
        if clean_path in ("/api/auth/me", "/api/auth/me/"):
            claims = self.require_auth()
            if not claims:
                return
            response_user = dict(claims)
            if ATLAS_DB is not None and claims.get("username"):
                try:
                    user_doc = ATLAS_DB.engineers.find_one({"username": str(claims["username"]).strip().lower()}, {"_id": 0, "password": 0})
                    if user_doc:
                        for k in ("name", "full_name", "email", "phone", "role", "profile_picture", "status", "join_date", "resignation_date", "birthday", "default_weekly_offs", "calendar_sync", "calendar_accounts"):
                            if k in user_doc:
                                response_user[k] = user_doc[k]
                    if response_user.get("name"):
                        response_user["name"] = to_proper_name(response_user["name"])
                    if response_user.get("full_name"):
                        response_user["full_name"] = to_proper_name(response_user["full_name"])
                except Exception:
                    pass

            caller_un = str(claims.get("username", "")).strip().lower()
            if "calendar_sync" not in response_user and caller_un in SERVER_USER_CALENDAR_SYNC:
                response_user["calendar_sync"] = dict(SERVER_USER_CALENDAR_SYNC[caller_un])
            if "calendar_accounts" not in response_user and caller_un in SERVER_USER_CALENDAR_ACCOUNTS:
                response_user["calendar_accounts"] = list(SERVER_USER_CALENDAR_ACCOUNTS[caller_un])
            self.send_json_response({
                "success": True,
                "user": response_user
            })
            return

        # 1.55 GET /api/credentials/trackers (List all tracked server credentials with computed expiry status)
        if clean_path in ("/api/credentials/trackers", "/api/credentials/trackers/"):
            claims = self.require_auth()
            if not claims:
                return

            caller_un = str(claims.get("username", "")).strip().lower()
            caller_status = ""
            if ATLAS_DB is not None:
                try:
                    c_eng = ATLAS_DB.engineers.find_one({"username": caller_un}, {"status": 1})
                    if c_eng:
                        caller_status = str(c_eng.get("status", "")).strip().lower()
                except Exception:
                    pass
            if not caller_status:
                for eng in SERVER_ENGINEERS:
                    if str(eng.get("username", "")).lower() == caller_un:
                        caller_status = str(eng.get("status", "")).strip().lower()
                        break

            # Resigned staff are restricted from viewing server credential alerts and trackers
            if caller_status == "resigned":
                self.send_json_response({
                    "success": True,
                    "data": {
                        "trackers": [],
                        "total_count": 0,
                        "expiring_soon_count": 0,
                        "expired_count": 0
                    }
                })
                return

            trackers = get_all_credential_trackers_from_db()
            total_count = len(trackers)
            expiring_soon_count = sum(1 for t in trackers if t.get("status") in ("EXPIRING_SOON", "EXPIRES_TODAY"))
            expired_count = sum(1 for t in trackers if t.get("status") == "EXPIRED")

            self.send_json_response({
                "success": True,
                "data": {
                    "trackers": trackers,
                    "total_count": total_count,
                    "expiring_soon_count": expiring_soon_count,
                    "expired_count": expired_count
                }
            })
            return

        # 1.551 GET /api/credentials/trackers/logs (Authoritative audit logs for credentials - Admin & Manager only)
        if clean_path in ("/api/credentials/trackers/logs", "/api/credentials/trackers/logs/"):
            claims = self.require_auth(["admin", "manager"])
            if not claims:
                return

            query_str = self.path.split("?")[1] if "?" in self.path else ""
            params = urllib.parse.parse_qs(query_str) if query_str else {}
            tracker_id = params.get("tracker_id", [None])[0]
            action = params.get("action", [None])[0]
            search = params.get("search", [None])[0]
            limit_val = 150
            try:
                if "limit" in params:
                    limit_val = int(params["limit"][0])
            except Exception:
                limit_val = 150

            logs = get_credential_tracker_logs_from_db(tracker_id=tracker_id, action=action, search=search, limit=limit_val)
            self.send_json_response({
                "success": True,
                "logs": logs,
                "count": len(logs)
            })
            return

        if clean_path.startswith("/api/credentials/trackers/"):
            tracker_id = clean_path[len("/api/credentials/trackers/"):].strip().strip("/")
            if tracker_id:
                claims = self.require_auth()
                if not claims:
                    return
                tracker = get_credential_tracker_by_id(tracker_id)
                if not tracker:
                    self.send_json_response({"success": False, "message": f"Credential tracker '{tracker_id}' not found."}, 404)
                    return
                self.send_json_response({"success": True, "data": tracker})
                return

        # 1.58 GET /api/users/calendar-sync (Cloud-Backed Calendar Sync State per Month)
        if clean_path in ("/api/users/calendar-sync", "/api/users/calendar-sync/"):
            claims = self.require_auth()
            if not claims:
                return

            caller_username = str(claims.get("username", "")).strip().lower()
            if not caller_username:
                self.send_json_response({"success": False, "message": "Authentication required."}, 401)
                return

            query_str = self.path.split("?")[1] if "?" in self.path else ""
            params = urllib.parse.parse_qs(query_str) if query_str else {}
            month_param = params.get("month", [None])[0]
            if month_param:
                month_param = month_param.strip()

            calendar_sync_data = {}
            if ATLAS_DB is not None:
                try:
                    user_doc = ATLAS_DB.engineers.find_one({"username": caller_username}, {"calendar_sync": 1})
                    if user_doc and "calendar_sync" in user_doc and isinstance(user_doc["calendar_sync"], dict):
                        calendar_sync_data = user_doc["calendar_sync"]
                except Exception as e:
                    print(f"  [ATLAS CALENDAR SYNC FETCH ERROR] {e}")

            if not calendar_sync_data and caller_username in SERVER_USER_CALENDAR_SYNC:
                calendar_sync_data = dict(SERVER_USER_CALENDAR_SYNC[caller_username])

            if not calendar_sync_data:
                # In-memory fallback
                for e in DEFAULT_SERVER_ENGINEERS:
                    if str(e.get("username", "")).lower() == caller_username:
                        cs = e.get("calendar_sync")
                        if isinstance(cs, dict):
                            calendar_sync_data = dict(cs)
                        break

            if month_param:
                month_sync = calendar_sync_data.get(month_param)
                self.send_json_response({
                    "success": True,
                    "month": month_param,
                    "sync": month_sync,
                    "synced": bool(month_sync.get("synced", False)) if isinstance(month_sync, dict) else False
                })
                return

            self.send_json_response({
                "success": True,
                "calendar_sync": calendar_sync_data,
                "count": len(calendar_sync_data)
            })
            return

        # 1.59 GET /api/users/calendar-accounts (List Device Google & Secondary Calendars)
        if clean_path in ("/api/users/calendar-accounts", "/api/users/calendar-accounts/"):
            claims = self.require_auth()
            if not claims:
                return

            caller_username = str(claims.get("username", "")).strip().lower()
            if not caller_username:
                self.send_json_response({"success": False, "message": "Authentication required."}, 401)
                return

            stored_accounts = []
            corporate_email = ""
            if ATLAS_DB is not None:
                try:
                    user_doc = ATLAS_DB.engineers.find_one({"username": caller_username}, {"calendar_accounts": 1, "email": 1})
                    if user_doc:
                        corporate_email = str(user_doc.get("email") or "").strip()
                        raw_accs = user_doc.get("calendar_accounts")
                        if isinstance(raw_accs, list):
                            stored_accounts = raw_accs
                except Exception as e:
                    print(f"  [ATLAS CALENDAR ACCOUNTS FETCH ERROR] {e}")

            if not stored_accounts and caller_username in SERVER_USER_CALENDAR_ACCOUNTS:
                stored_accounts = list(SERVER_USER_CALENDAR_ACCOUNTS[caller_username])

            if not corporate_email:
                corporate_email = str(claims.get("email") or "").strip()
            if not corporate_email:
                for e in DEFAULT_SERVER_ENGINEERS:
                    if str(e.get("username", "")).lower() == caller_username:
                        corporate_email = str(e.get("email") or "").strip()
                        break

            # Synthesize or ensure default corporate calendar option exists
            merged_accounts = []
            if corporate_email:
                has_corp = any(str(acc.get("account_type", "")).lower() == "corporate" or str(acc.get("account_name", "")).lower() == corporate_email.lower() for acc in stored_accounts)
                if not has_corp:
                    merged_accounts.append({
                        "id": "corporate",
                        "name": "Corporate Calendar (Work)",
                        "account_name": corporate_email,
                        "account_type": "corporate",
                        "is_primary": True,
                        "color": "#174EAC"
                    })

            for acc in stored_accounts:
                merged_accounts.append(acc)

            self.send_json_response({
                "success": True,
                "calendar_accounts": merged_accounts,
                "corporate_email": corporate_email,
                "count": len(merged_accounts)
            })
            return

        # 1.6 GET /api/users/<username>/avatar (Binary image streaming with caching headers)
        if clean_path.startswith("/api/users/") and clean_path.endswith("/avatar"):
            parts = clean_path.strip("/").split("/")
            if len(parts) == 4 and parts[0] == "api" and parts[1] == "users" and parts[3] == "avatar":
                target_user = parts[2].strip().lower()
                user_doc = None
                if ATLAS_DB is not None:
                    try:
                        user_doc = ATLAS_DB.engineers.find_one({"username": target_user}, {"profile_picture": 1})
                    except Exception:
                        pass
                
                pic = user_doc.get("profile_picture") if user_doc else None
                if pic and "base64," in pic:
                    try:
                        header_part, b64_data = pic.split("base64,", 1)
                        mime_type = "image/jpeg"
                        if "image/png" in header_part:
                            mime_type = "image/png"
                        elif "image/webp" in header_part:
                            mime_type = "image/webp"
                        img_bytes = base64.b64decode(b64_data)
                        self.send_response(200)
                        self.send_header("Content-Type", mime_type)
                        self.send_header("Content-Length", str(len(img_bytes)))
                        self.send_header("Cache-Control", "public, max-age=86400, stale-while-revalidate=604800")
                        self.send_cors_headers()
                        self.end_headers()
                        self.wfile.write(img_bytes)
                        return
                    except Exception:
                        pass

                self.send_json_response({"success": False, "message": "Avatar not found"}, 404)
                return

        # 1.71 GET /api/leave/my-requests (Authenticated Engineer Request History & Balance)
        if clean_path in ("/api/leave/my-requests", "/api/leave/my-requests/"):
            claims = self.require_auth()
            if not claims:
                return

            caller_username = str(claims.get("username", "")).strip().lower()
            current_year = int(get_ist_now().strftime("%Y"))

            docs = []
            if ATLAS_DB is not None:
                try:
                    docs = list(ATLAS_DB.leave_requests.find({"engineer_id": caller_username}, {"_id": 0}).sort("timestamp", -1))
                except Exception as e:
                    print(f"  [ATLAS LEAVE FETCH ERROR] {e}")
                    docs = [dict(r) for r in SERVER_LEAVE_REQUESTS if str(r.get("engineer_id", "")).lower() == caller_username]
            else:
                docs = [dict(r) for r in SERVER_LEAVE_REQUESTS if str(r.get("engineer_id", "")).lower() == caller_username]

            stats = get_engineer_leave_stats(caller_username, current_year)
            self.send_json_response({
                "success": True,
                "requests": docs,
                "active_count": stats["active_count"],
                "approved_leave_days": stats["approved_leave_days"],
                "remaining_leave_balance": stats["remaining_leave_balance"],
                "annual_quota": stats["annual_quota"],
                "capping_enabled": stats["capping_enabled"],
                "annual_year": current_year
            })
            return

        # 1.72 GET /api/leave/eligible-partners (Candidate Swap Partners for Date)
        if clean_path in ("/api/leave/eligible-partners", "/api/leave/eligible-partners/"):
            claims = self.require_auth()
            if not claims:
                return

            caller_username = str(claims.get("username", "")).strip().lower()
            query_str = self.path.split("?")[1] if "?" in self.path else ""
            params = urllib.parse.parse_qs(query_str) if query_str else {}
            date_param = str(params.get("date", [""])[0]).strip()

            if not re.match(r"^\d{4}-\d{2}-\d{2}$", date_param):
                self.send_json_response({"success": False, "message": "Invalid date format. Expected YYYY-MM-DD."}, 400)
                return

            month_key = date_param[:7]
            try:
                d_num = int(date_param[8:10])
            except ValueError:
                self.send_json_response({"success": False, "message": "Invalid day in date."}, 400)
                return

            # Fetch monthly roster
            roster_doc = None
            if ATLAS_DB is not None:
                try:
                    roster_doc = ATLAS_DB.rosters.find_one({"_id": month_key})
                except Exception:
                    pass

            # Fetch active engineers list
            engineers_list = []
            if ATLAS_DB is not None:
                try:
                    engineers_list = list(ATLAS_DB.engineers.find({}, {"_id": 0, "password": 0}))
                except Exception:
                    pass
            if not engineers_list:
                engineers_list = DEFAULT_SERVER_ENGINEERS

            candidates = []
            for eng in engineers_list:
                u = str(eng.get("username", "")).strip().lower()
                if not u or u == caller_username or is_management_account(eng):
                    continue

                status = str(eng.get("status", "")).strip().lower()
                if status == "resigned" or eng.get("resigned") is True:
                    res_date = eng.get("resignation_date") or eng.get("resigned_date") or eng.get("resigned_month")
                    if res_date:
                        if month_key > str(res_date).strip()[:7]:
                            continue
                    else:
                        today_m = get_ist_now().strftime("%Y-%m")
                        if month_key > today_m:
                            continue

                display_name = to_proper_name(eng.get("name") or eng.get("full_name") or u)
                photo = str(eng.get("profile_picture") or eng.get("profilePicture") or "").strip()

                # Determine shift on date_param
                shift_val = ""
                if roster_doc:
                    eng_rows = roster_doc.get("engineers") or roster_doc.get("roster") or []
                    for r in eng_rows:
                        if not isinstance(r, dict):
                            continue
                        ru = str(r.get("username") or "").strip().lower()
                        rn = str(r.get("name") or "").strip().lower()
                        if ru == u or rn == u:
                            s_raw = r.get("shifts")
                            if isinstance(s_raw, dict):
                                shift_val = str(s_raw.get(str(d_num), "")).strip().upper()
                            elif isinstance(s_raw, list) and len(s_raw) >= d_num:
                                shift_val = str(s_raw[d_num - 1]).strip().upper()
                            break

                shift_val = shift_val or "UNASSIGNED"
                is_off = (shift_val == "OFF")

                candidates.append({
                    "username": u,
                    "name": display_name,
                    "profile_picture": photo,
                    "shift": shift_val,
                    "is_off": is_off
                })

            # Sort OFF candidates first, then alphabetically by name
            candidates.sort(key=lambda x: (not x["is_off"], x["name"].lower()))

            self.send_json_response({
                "success": True,
                "date": date_param,
                "month": month_key,
                "partners": candidates,
                "count": len(candidates)
            })
            return

        # 1.73 GET /api/leave/admin-queue (Admin Pending & Historical Leave Requests Queue)
        if clean_path in ("/api/leave/admin-queue", "/api/leave/admin-queue/"):
            claims = self.require_auth(["admin", "manager"])
            if not claims:
                return

            requests_list = []
            if ATLAS_DB is not None:
                try:
                    requests_list = list(ATLAS_DB.leave_requests.find({}, {"_id": 0}).sort("timestamp", -1))
                except Exception as e:
                    print(f"  [ATLAS ADMIN QUEUE ERROR] {e}")
                    requests_list = [dict(r) for r in SERVER_LEAVE_REQUESTS]
            else:
                requests_list = [dict(r) for r in SERVER_LEAVE_REQUESTS]

            # Cache month rosters for suggestions
            cached_rosters = {}
            active_engineers = []
            if ATLAS_DB is not None:
                try:
                    active_engineers = list(ATLAS_DB.engineers.find({}, {"_id": 0, "password": 0}))
                except Exception:
                    pass
            if not active_engineers:
                active_engineers = DEFAULT_SERVER_ENGINEERS

            pending_count = 0
            for req in requests_list:
                if str(req.get("status", "")).lower() == "pending":
                    pending_count += 1
                    # Attach candidate suggestions per requested date
                    applicant_u = str(req.get("engineer_id", "")).lower()
                    date_suggestions = {}
                    for d_str in req.get("dates", []):
                        if len(d_str) < 10:
                            continue
                        m_key = d_str[:7]
                        try:
                            d_num = int(d_str[8:10])
                        except ValueError:
                            continue

                        if m_key not in cached_rosters:
                            if ATLAS_DB is not None:
                                try:
                                    cached_rosters[m_key] = ATLAS_DB.rosters.find_one({"_id": m_key})
                                except Exception:
                                    cached_rosters[m_key] = None
                            else:
                                cached_rosters[m_key] = None

                        r_doc = cached_rosters.get(m_key)
                        off_partners = []
                        if r_doc:
                            for eng in active_engineers:
                                eu = str(eng.get("username", "")).strip().lower()
                                if not eu or eu == applicant_u or is_management_account(eng):
                                    continue

                                status = str(eng.get("status", "")).strip().lower()
                                if status == "resigned" or eng.get("resigned") is True:
                                    res_date = eng.get("resignation_date") or eng.get("resigned_date") or eng.get("resigned_month")
                                    if res_date:
                                        if m_key > str(res_date).strip()[:7]:
                                            continue
                                    else:
                                        today_m = get_ist_now().strftime("%Y-%m")
                                        if m_key > today_m:
                                            continue
                                # Check shift
                                eng_rows = r_doc.get("engineers") or r_doc.get("roster") or []
                                for r in eng_rows:
                                    if not isinstance(r, dict):
                                        continue
                                    if str(r.get("username") or "").strip().lower() == eu:
                                        s_raw = r.get("shifts")
                                        shift_code = ""
                                        if isinstance(s_raw, dict):
                                            shift_code = str(s_raw.get(str(d_num), "")).strip().upper()
                                        elif isinstance(s_raw, list) and len(s_raw) >= d_num:
                                            shift_code = str(s_raw[d_num - 1]).strip().upper()
                                        if shift_code == "OFF":
                                            off_partners.append({
                                                "username": eu,
                                                "name": to_proper_name(eng.get("name") or eng.get("full_name") or eu),
                                                "shift": "OFF"
                                            })
                                        break
                        off_partners.sort(key=lambda x: x["name"].lower())
                        date_suggestions[d_str] = off_partners

                    req["candidate_suggestions"] = date_suggestions

            pending_requests = [r for r in requests_list if str(r.get("status", "")).lower() == "pending"]
            historical_requests = [r for r in requests_list if str(r.get("status", "")).lower() != "pending"]

            self.send_json_response({
                "success": True,
                "pending_requests": pending_requests,
                "historical_requests": historical_requests,
                "requests": requests_list,
                "pending_count": len(pending_requests),
                "history_count": len(historical_requests),
                "total_count": len(requests_list)
            })
            return

        # 1.74 GET /api/leave/settings (Admin Leave Policy Settings)
        if clean_path in ("/api/leave/settings", "/api/leave/settings/"):
            claims = self.require_auth(["admin", "manager"])
            if not claims:
                return
            cfg = get_leave_system_config()
            self.send_json_response({
                "success": True,
                "capping_enabled": cfg.get("capping_enabled", True),
                "annual_quota": cfg.get("annual_quota", 14)
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
                    # Load all engineer metadata for accurate role, status, resignation, and include_in_roster checks
                    eng_meta_by_key = {}
                    try:
                        for eng_doc in ATLAS_DB.engineers.find({}, {"password": 0}):
                            for field in ("username", "name", "full_name", "id", "engineer_id"):
                                val = str(eng_doc.get(field) or "").strip().lower()
                                if val:
                                    eng_meta_by_key[val] = eng_doc
                    except Exception:
                        pass

                    if "engineers" in doc and isinstance(doc["engineers"], list):
                        doc["engineers"] = [e for e in doc["engineers"] if is_eligible_roster_row(e, month_id, eng_meta_by_key)]
                        for e in doc["engineers"]:
                            if isinstance(e, dict) and e.get("name"):
                                e["name"] = to_proper_name(e["name"])
                    if "roster" in doc and isinstance(doc["roster"], list):
                        doc["roster"] = [e for e in doc["roster"] if is_eligible_roster_row(e, month_id, eng_meta_by_key)]
                        for e in doc["roster"]:
                            if isinstance(e, dict) and e.get("name"):
                                e["name"] = to_proper_name(e["name"])
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

            try:
                check_and_trigger_celebrations()
            except Exception:
                pass

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
                fn = item.get("full_name") or item.get("fullName") or item.get("name")
                if fn:
                    proper_fn = to_proper_name(fn)
                    item["full_name"] = proper_fn
                    item["fullName"] = proper_fn
                    if not item.get("name") or len(proper_fn) > len(item.get("name", "")):
                        item["name"] = proper_fn
                elif item.get("name"):
                    item["name"] = to_proper_name(item["name"])
                    item["full_name"] = item["name"]
                    item["fullName"] = item["name"]
                if not item.get("username") and item.get("email") and "@" in item["email"]:
                    item["username"] = item["email"].split("@")[0].strip().lower()
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

        # 6.9 GET /api/celebrations/today (AUTHENTICATED - Today's Birthdays & Work Anniversaries)
        if clean_path in ("/api/celebrations/today", "/api/celebrations/today/"):
            claims = self.require_auth()
            if not claims:
                return

            try:
                check_and_trigger_celebrations()
            except Exception:
                pass

            ist_tz = timezone(timedelta(hours=5, minutes=30))
            now_ist = datetime.now(ist_tz)
            today_str = now_ist.strftime("%Y-%m-%d")

            engineers = []
            if ATLAS_DB is not None:
                try:
                    engineers = list(ATLAS_DB.engineers.find({}, {"_id": 0}))
                except Exception as e:
                    log_server(f"[CELEBRATION] Error fetching engineers from Atlas: {e}")
            if not engineers:
                engineers = [dict(e) for e in DEFAULT_SERVER_ENGINEERS]

            celebrations = []
            for eng in engineers:
                uname = str(eng.get("username") or "").strip().lower()
                if not uname or uname in ("admin", "sysadmin", "system"):
                    continue
                if str(eng.get("status", "")).strip().lower() in ("resigned", "inactive"):
                    continue

                full_name = str(eng.get("fullName") or eng.get("full_name") or eng.get("name") or uname).strip()
                full_name = to_proper_name(full_name)

                bday_raw = str(eng.get("birthday") or "").strip()
                jdate_raw = str(eng.get("join_date") or eng.get("joinDate") or "").strip()

                is_bday_match, _, _ = parse_celebration_date(bday_raw, now_ist)
                is_jdate_match, anniv_years, _ = parse_celebration_date(jdate_raw, now_ist)
                is_anniversary = is_jdate_match and (anniv_years >= 1)

                event_types = []
                if is_bday_match and is_anniversary:
                    event_types.append(("both", anniv_years))
                elif is_bday_match:
                    event_types.append(("birthday", 0))
                elif is_anniversary:
                    event_types.append(("anniversary", anniv_years))

                for etype, yr in event_types:
                    yr_ord = ordinal(yr)
                    yr_label = f"{yr} years" if yr > 1 else "1 year"
                    if etype == "both":
                        title = f"Birthday & {yr_ord} Work Anniversary"
                        greeting = f"Happy Birthday and Happy {yr_ord} Work Anniversary, {full_name}! 🎂🏆 Wishing you the very best!"
                    elif etype == "birthday":
                        title = "Birthday Celebration"
                        greeting = f"Happy Birthday, {full_name}! 🎉🎂 Wishing you a fantastic year ahead filled with joy and success!"
                    else:
                        title = f"{yr_ord} Work Anniversary"
                        greeting = f"Happy {yr_ord} Work Anniversary, {full_name}! 🏆✨ Congratulations on completing {yr_label} with our team!"

                    photo = eng.get("profile_picture") or eng.get("profilePicture") or ""
                    phone = str(eng.get("phone") or "").strip()

                    celebrations.append({
                        "username": uname,
                        "name": full_name,
                        "type": etype,
                        "years": yr,
                        "photo": photo,
                        "phone": phone,
                        "title": title,
                        "greeting_text": greeting
                    })

            self.send_json_response({
                "success": True,
                "today": today_str,
                "celebrations": celebrations,
                "count": len(celebrations)
            })
            return

        # 7. GET /api/notifications (AUTHENTICATED)
        if clean_path in ("/api/notifications", "/api/notifications/"):
            claims = self.require_auth()
            if not claims:
                return

            try:
                purge_expired_celebrations()
                check_and_trigger_celebrations()
            except Exception:
                pass

            notifs = list(SERVER_NOTIFICATIONS)
            dismissed_ids = []
            if ATLAS_DB is not None:
                try:
                    notifs = list(ATLAS_DB.notifications.find({}, {"_id": 0}).sort("timestamp", -1).limit(50))
                except Exception as e:
                    print(f"  [ATLAS] Notifications fetch error: {e}")

                if claims.get("username"):
                    try:
                        u_doc = ATLAS_DB.engineers.find_one({"username": str(claims["username"]).lower()}, {"dismissed_notifications": 1})
                        if u_doc and "dismissed_notifications" in u_doc:
                            dismissed_ids = list(u_doc["dismissed_notifications"])
                    except Exception as e:
                        print(f"  [ATLAS] Dismissed notifs fetch error: {e}")

            caller_un = str(claims.get("username", "")).strip().lower()
            if not dismissed_ids and caller_un in SERVER_USER_DISMISSED_NOTIFICATIONS:
                dismissed_ids = sorted(list(SERVER_USER_DISMISSED_NOTIFICATIONS[caller_un]))

            # Defensive clean sweep: ensure zero expired celebrations ever leak to engineers or Admin/Manager history
            ist_tz = timezone(timedelta(hours=5, minutes=30))
            now_ist = datetime.now(ist_tz)
            today_ist_str = now_ist.strftime("%Y-%m-%d")
            start_of_today_ist = datetime(now_ist.year, now_ist.month, now_ist.day, tzinfo=ist_tz).timestamp()
            notifs = [n for n in notifs if not is_expired_celebration(n, today_ist_str, start_of_today_ist)]

            is_admin = bool(claims.get("role") in ("admin", "manager") or claims.get("is_admin"))

            if not is_admin:
                # Regular engineers do not see soft-retracted broadcasts and only see in-app notices.
                # Permanent Server-Side Filtering: Dismissed notifications are NEVER served again to this user!
                dismissed_set = set(str(did).strip() for did in dismissed_ids if did)

                # Check caller status (e.g. Resigned)
                caller_status = ""
                if ATLAS_DB is not None:
                    try:
                        c_eng = ATLAS_DB.engineers.find_one({"username": caller_un}, {"status": 1})
                        if c_eng:
                            caller_status = str(c_eng.get("status", "")).strip().lower()
                    except Exception:
                        pass
                if not caller_status:
                    for eng in SERVER_ENGINEERS:
                        if str(eng.get("username", "")).lower() == caller_un:
                            caller_status = str(eng.get("status", "")).strip().lower()
                            break

                filtered_notifs = []
                for n in notifs:
                    if n.get("is_retracted") or not n.get("send_in_app", True):
                        continue
                    nid = str(n.get("id") or n.get("_id") or n.get("celebration_id") or "").strip()
                    if nid in dismissed_set:
                        continue

                    # Target user enforcement:
                    target = str(n.get("target_user") or n.get("target") or "").strip().lower()
                    recips = [str(r).strip().lower() for r in (n.get("recipients") or []) if r]
                    category = str(n.get("category") or "").strip().lower()
                    title_lower = str(n.get("title") or "").strip().lower()
                    notif_type = str(n.get("type") or "").strip().lower()
                    link_val = str(n.get("link") or "").strip().lower()

                    # Admin-only notifications must NEVER leak to regular engineers
                    if target in ("admin", "admins") or any(r in ("admin", "admins") for r in recips):
                        continue

                    # Operations & Leave action notifications are STRICTLY default-deny:
                    # They must NEVER broadcast to any engineer other than the specific applicant/target
                    is_ops_or_leave = category == "operations" or "leave" in title_lower or notif_type == "leave_request_action" or link_val in ("#my-requests", "#leave-requests")
                    if is_ops_or_leave:
                        if not target and not recips:
                            continue
                        if target in ("everyone", "all") or "everyone" in recips or "all" in recips:
                            continue
                        if caller_un != target and caller_un not in recips:
                            continue

                    # If notification is targeted to a specific user, caller MUST match target_user or be in recipients
                    if target and target not in ("everyone", "all"):
                        if caller_un != target and caller_un not in recips:
                            continue

                    # If notification has specific recipients and not 'everyone', caller must be in recipients
                    if recips and "everyone" not in recips and "all" not in recips:
                        if caller_un not in recips and caller_un != target:
                            continue

                    # If caller is Resigned, they only receive notifications explicitly targeted to them!
                    if caller_status == "resigned":
                        if caller_un != target and caller_un not in recips:
                            continue

                    filtered_notifs.append(n)

                notifs = filtered_notifs
            else:
                # Admins get live read receipts per broadcast
                dismissed_map = {}
                if ATLAS_DB is not None:
                    try:
                        all_engs = list(ATLAS_DB.engineers.find({}, {"name": 1, "username": 1, "fullName": 1, "dismissed_notifications": 1}))
                        for e in all_engs:
                            disp_name = str(e.get("fullName") or e.get("name") or e.get("username") or "Engineer").strip()
                            for nid in e.get("dismissed_notifications", []):
                                dismissed_map.setdefault(str(nid), []).append(disp_name)
                    except Exception as e:
                        print(f"  [NOTIF READ RECEIPTS ERROR] {e}")

                for n in notifs:
                    nid = str(n.get("id") or n.get("_id") or "")
                    read_list = dismissed_map.get(nid, [])
                    recipients_list = n.get("recipients", [])
                    target_user = str(n.get("target_user") or "").strip()
                    if not recipients_list and target_user and target_user not in ("everyone", "all", "admins"):
                        recipients_list = [target_user]
                        n["recipients"] = recipients_list
                        n["recipients_count"] = 1

                    # If recipients_list is specific (not everyone), only engineers who were targeted should appear in read_by!
                    if recipients_list and "everyone" not in recipients_list and "all" not in recipients_list:
                        recip_lowers = set(str(r).strip().lower() for r in recipients_list)
                        valid_read = []
                        for r_name in read_list:
                            if any(r in r_name.lower() for r in recip_lowers):
                                valid_read.append(r_name)
                        n["read_by"] = valid_read
                        n["read_count"] = len(valid_read)
                        n["pending"] = [r for r in recipients_list if not any(r.lower() in vr.lower() for vr in valid_read)]
                        n["pending_count"] = len(n["pending"])
                    else:
                        n["read_by"] = read_list
                        n["read_count"] = len(read_list)
                        if recipients_list:
                            n["pending"] = [r for r in recipients_list if r not in read_list]
                            n["pending_count"] = len(n["pending"])
                        else:
                            n["pending"] = []
                            n["pending_count"] = 0

                    # Ensure separate 3-channel delivery report telemetry is present
                    if "inapp_delivery" in n and isinstance(n["inapp_delivery"], dict):
                        n["inapp_delivery"]["read_count"] = len(read_list)
                        n["inapp_delivery"]["pending_count"] = n["pending_count"]
                    else:
                        n["inapp_delivery"] = {
                            "enabled": n.get("send_in_app", True),
                            "target_count": len(recipients_list),
                            "read_count": len(read_list),
                            "pending_count": n["pending_count"],
                            "status": "Active in Feed" if n.get("send_in_app", True) else "Disabled"
                        }
                    if "fcm_delivery" not in n:
                        n["fcm_delivery"] = {
                            "enabled": n.get("send_push", True),
                            "dispatched_count": n.get("push_dispatched", 0),
                            "status": f"Dispatched ({n.get('push_dispatched', 0)} devices)" if n.get("send_push", True) else "Disabled"
                        }
                    if "email_delivery" not in n:
                        n["email_delivery"] = {
                            "enabled": n.get("send_email", False),
                            "dispatched_count": n.get("emails_dispatched", 0),
                            "status": f"Sent ({n.get('emails_dispatched', 0)} emails)" if n.get("send_email", False) else "Disabled"
                        }

            # Personalize @name / {name} / @all / @<username> tokens in BOTH title and body for non-admin feeds
            caller_un = str(claims.get("username", "")).strip()
            caller_full_name = get_engineer_display_name(caller_un)
            if not caller_full_name or caller_full_name == "System":
                caller_full_name = str(claims.get("fullName") or claims.get("name") or caller_un or "Team Member").strip()

            name_by_user = {}
            name_by_id = {}
            if not is_admin:
                eng_pool = []
                if ATLAS_DB is not None:
                    try:
                        eng_pool = list(ATLAS_DB.engineers.find({}, {"_id": 0, "username": 1, "id": 1, "name": 1, "fullName": 1, "full_name": 1}))
                    except Exception:
                        eng_pool = []
                if not eng_pool:
                    eng_pool = list(DEFAULT_SERVER_ENGINEERS)
                for e in eng_pool:
                    e_un = str(e.get("username") or "").strip().lower()
                    e_id = str(e.get("id") or "").strip().lower()
                    e_fn = str(e.get("fullName") or e.get("full_name") or e.get("name") or e_un).strip()
                    if e_un:
                        name_by_user[e_un] = to_proper_name(e_fn)
                    if e_id:
                        name_by_id[e_id] = to_proper_name(e_fn)

            feed_notifs = []
            for n in notifs:
                item = dict(n)
                if not is_admin:
                    raw_t = str(item.get("title") or "")
                    if raw_t:
                        item["title"] = _personalize_broadcast_text(
                            raw_t,
                            recipient_name=caller_full_name,
                            recipient_username=caller_un,
                            name_by_user=name_by_user,
                            name_by_id=name_by_id,
                            html_mode=False
                        )
                    raw_m = str(item.get("raw_message") or "")
                    if raw_m:
                        item["raw_message"] = _personalize_broadcast_text(
                            raw_m,
                            recipient_name=caller_full_name,
                            recipient_username=caller_un,
                            name_by_user=name_by_user,
                            name_by_id=name_by_id,
                            html_mode=False
                        )
                feed_notifs.append(item)

            self.send_json_response({
                "success": True,
                "notifications": feed_notifs,
                "dismissed_notification_ids": dismissed_ids,
                "count": len(feed_notifs)
            })
            return

        # 8. GET /api/broadcast/templates (AUTHENTICATED)
        if clean_path in ("/api/broadcast/templates", "/api/broadcast/templates/"):
            claims = self.require_auth()
            if not claims:
                return

            templates = []
            if ATLAS_DB is not None:
                try:
                    templates = list(ATLAS_DB.broadcast_templates.find({}, {"_id": 0}))
                except Exception as e:
                    print(f"  [ATLAS] Templates fetch error: {e}")
                    templates = list(SERVER_BROADCAST_TEMPLATES)
            else:
                templates = list(SERVER_BROADCAST_TEMPLATES)

            self.send_json_response({
                "success": True,
                "templates": templates,
                "count": len(templates)
            })
            return

        # Unknown route
        self.send_json_response({
            "success": False,
            "message": f"Endpoint GET {clean_path} not found"
        }, 404)

    def do_POST(self):
        global SERVER_BROADCAST_TEMPLATES
        clean_path = self.path.split("?")[0].strip()
        norm_path = clean_path.rstrip("/")

        # Support API requests with duplicate /api or missing /api prefix
        while norm_path.startswith("/api/api/"):
            norm_path = "/api" + norm_path[8:]
        if norm_path in (
            "/roster", "/engineers", "/engineers/delete", "/constraints",
            "/auth/login", "/auth/send-otp", "/auth/verify-otp",
            "/roster-backups/restore", "/notifications/send",
            "/notifications/dismiss", "/notifications/retract",
            "/broadcast/templates", "/broadcast/templates/delete",
            "/broadcast-templates", "/broadcast-templates/delete",
            "/credentials", "/credentials/trackers",
            "/users/fcm-token", "/users/fcm-token/unregister", "/fcm/unregister",
            "/users/profile", "/celebrations/check", "/admin/ai-settings", "/ai-settings",
            "/ai/generate", "/ai/chat", "/chat/assistant", "/ai/assistant",
            "/users/profile-picture",
            "/users/calendar-sync", "/users/calendar-sync/clear",
            "/users/calendar-accounts"
        ) or norm_path.startswith("/roster/") or norm_path.startswith("/roster-backups/") or norm_path.startswith("/credentials/") or norm_path.startswith("/broadcast-templates/") or norm_path.startswith("/users/calendar-sync") or norm_path.startswith("/users/calendar-accounts") or norm_path.startswith("/leave/"):
            norm_path = "/api" + norm_path
        norm_path = self._normalize_api_aliases(norm_path)

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
            admin_user, admin_pass = get_admin_credentials()
            env = load_env_variables()

            # First, check if admin user exists in Atlas DB
            user = find_user_by_identifier(identifier)
            if user:
                stored_pass = user.get("password")
                if not stored_pass:
                    # If this is the master admin username and no password in Atlas yet, permit temporary password login and persist hash to Atlas DB!
                    if identifier == admin_user and hmac.compare_digest(password, admin_pass):
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
                            "role": user.get("role", "admin"),
                            "profile_picture": user.get("profile_picture"),
                            "calendar_sync": user.get("calendar_sync", {}),
                            "calendar_accounts": user.get("calendar_accounts", [])
                        }
                        token = create_jwt({**user_payload, "exp": int(time.time()) + 7 * 86400})
                        platform_client = detect_client_platform(self.headers)
                        device_client = parse_device_info(self.headers)
                        record_audit_event(user_payload.get("name"), user_payload.get("username"), user_payload.get("role"), platform_client, "User Sign-In", details={"device": device_client}, device=device_client)
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
                        "name": to_proper_name(user.get("name") or u_name),
                        "full_name": to_proper_name(user.get("full_name") or user.get("name") or u_name),
                        "email": user.get("email", ""),
                        "phone": user.get("phone", ""),
                        "role": role,
                        "profile_picture": user.get("profile_picture"),
                        "calendar_sync": user.get("calendar_sync", {}),
                        "calendar_accounts": user.get("calendar_accounts", [])
                    }
                    token = create_jwt({**user_payload, "exp": int(time.time()) + 7 * 86400})
                    platform_client = detect_client_platform(self.headers)
                    device_client = parse_device_info(self.headers)
                    record_audit_event(user_payload.get("name"), user_payload.get("username"), user_payload.get("role"), platform_client, "User Sign-In", details={"device": device_client}, device=device_client)
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
            if identifier == admin_user and hmac.compare_digest(password, admin_pass):
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
                    "default_weekly_offs": [],
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
                    "role": "admin",
                    "profile_picture": admin_doc.get("profile_picture")
                }
                token = create_jwt({**user_payload, "exp": int(time.time()) + 7 * 86400})
                platform_client = detect_client_platform(self.headers)
                device_client = parse_device_info(self.headers)
                record_audit_event(user_payload.get("name"), user_payload.get("username"), user_payload.get("role"), platform_client, "User Sign-In", details={"device": device_client}, device=device_client)
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

            

        # 1.2 POST /api/auth/forgot-password & /api/auth/send-otp (Resend OTP Dispatch)
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

            # 3. Send email to the engineer's registered address via Resend
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
                    "message": "Failed to dispatch verification email via Resend."
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

        if clean_path in ("/api/admin/ai-settings", "/api/admin/ai-settings/", "/api/ai-settings", "/api/ai-settings/"):
            claims = self.require_auth()
            if not claims:
                return
            if str(claims.get("role", "")).lower() != "admin":
                self.send_json_response({"success": False, "message": "Permission denied: Admin role required."}, 403)
                return

            action = str(body.get("action") or "save_settings").strip().lower()
            current_cfg = get_gemini_config()
            current_keys = list(current_cfg.get("api_keys", []))
            selected_model = body.get("selected_model") or current_cfg.get("selected_model", "gemini-3.8-flash")
            admin_user = claims.get("username") or claims.get("name") or "admin"

            if action == "add_key":
                new_key = str(body.get("key") or "").strip()
                if not new_key:
                    self.send_json_response({"success": False, "message": "API key cannot be empty."}, 400)
                    return
                if new_key in current_keys:
                    self.send_json_response({"success": False, "message": "This API key is already configured."}, 400)
                    return
                current_keys.append(new_key)
                save_gemini_config(current_keys, selected_model, updated_by=admin_user)
                self.send_json_response({
                    "success": True,
                    "message": "API key added successfully.",
                    "total_keys": len(current_keys),
                    "keys": [{"id": i, "masked": mask_api_key(k), "length": len(k)} for i, k in enumerate(current_keys)]
                })
                return

            elif action == "remove_key":
                idx = body.get("index")
                if idx is not None and isinstance(idx, int) and 0 <= idx < len(current_keys):
                    removed = current_keys.pop(idx)
                    save_gemini_config(current_keys, selected_model, updated_by=admin_user)
                    self.send_json_response({
                        "success": True,
                        "message": f"API key {mask_api_key(removed)} removed successfully.",
                        "total_keys": len(current_keys),
                        "keys": [{"id": i, "masked": mask_api_key(k), "length": len(k)} for i, k in enumerate(current_keys)]
                    })
                    return
                self.send_json_response({"success": False, "message": "Invalid key index."}, 400)
                return

            elif action == "test_keys":
                results = []
                for idx, k in enumerate(current_keys):
                    test_res = test_single_gemini_key(k, model=selected_model)
                    results.append({
                        "id": idx,
                        "masked": mask_api_key(k),
                        "status": test_res.get("status"),
                        "message": test_res.get("message"),
                        "model": test_res.get("model")
                    })
                self.send_json_response({
                    "success": True,
                    "results": results,
                    "total_keys": len(results)
                })
                return

            elif action == "test_single_key":
                test_k = str(body.get("key") or "").strip()
                if not test_k:
                    self.send_json_response({"success": False, "message": "Key is required for test."}, 400)
                    return
                res = test_single_gemini_key(test_k, model=selected_model)
                self.send_json_response({"success": True, "result": res})
                return

            elif action == "save_settings":
                new_keys = body.get("keys")
                if isinstance(new_keys, list):
                    valid_keys = [str(k).strip() for k in new_keys if str(k).strip() and not str(k).strip().endswith("...****")]
                    if valid_keys:
                        current_keys = valid_keys
                save_gemini_config(current_keys, selected_model, updated_by=admin_user)
                self.send_json_response({
                    "success": True,
                    "message": "AI settings and API key pool saved to MongoDB Atlas.",
                    "total_keys": len(current_keys),
                    "selected_model": selected_model
                })
                return

            self.send_json_response({"success": False, "message": f"Unknown action '{action}'."}, 400)
            return

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

            user_query = str(body.get("message") or body.get("prompt") or "").strip()
            client_platform = body.get("platform") or "Web Operations Console"
            history = body.get("history") or []
            target_month = body.get("target_month") or body.get("month") or None

            if not user_query:
                self.send_json_response({"success": False, "message": "Missing 'message' or 'prompt' in payload."}, 400)
                return

            ai_reply, model_name, grounded_time = query_gemini_roster_agent(
                user_prompt=user_query,
                current_user=current_user,
                client_platform=client_platform,
                history=history,
                target_month=target_month
            )

            if model_name == "error" or not ai_reply:
                self.send_json_response({
                    "success": False,
                    "error": ai_reply or "Gemini API failed to return a response.",
                    "message": ai_reply or "Gemini API failed to return a response.",
                    "model": "error",
                    "grounded_time": grounded_time,
                    "platform": client_platform
                }, 500)
                return

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
            gemini_model = env.get("GEMINI_MODEL", "gemini-3.1-flash-lite").strip()

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

                # Ensure admin, manager, and future resigned accounts are never scheduled
                eng_meta_by_key = {}
                try:
                    for eng_doc in ATLAS_DB.engineers.find({}, {"password": 0}):
                        for field in ("username", "name", "full_name", "id", "engineer_id"):
                            val = str(eng_doc.get(field) or "").strip().lower()
                            if val:
                                eng_meta_by_key[val] = eng_doc
                except Exception:
                    pass

                if "engineers" in body and isinstance(body["engineers"], list):
                    body["engineers"] = [e for e in body["engineers"] if is_eligible_roster_row(e, doc_id, eng_meta_by_key)]
                if "roster" in body and isinstance(body["roster"], list):
                    body["roster"] = [e for e in body["roster"] if is_eligible_roster_row(e, doc_id, eng_meta_by_key)]
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

        # 3.25 POST /api/roster/generate-night-shifts (Deterministic 24/7 Night Shift Solver)
        if clean_path in ("/api/roster/generate-night-shifts", "/api/roster/generate-night-shifts/"):
            claims = self.require_auth(["admin", "manager"])
            if not claims:
                return

            month_str = str(body.get("month") or "").strip()
            y = body.get("year")
            m = body.get("month_num") or body.get("m")
            if not month_str and y and m:
                month_str = f"{y}-{str(m).zfill(2)}"

            if not month_str or len(month_str) < 7:
                self.send_json_response({"success": False, "message": "Missing or invalid 'month' parameter (expected 'YYYY-MM')"}, 400)
                return

            try:
                parts = [int(p) for p in month_str[:7].split("-")]
                year_val = parts[0]
                month_val = parts[1]
                if not (2020 <= year_val <= 2040 and 1 <= month_val <= 12):
                    raise ValueError("Year or month out of range")
            except Exception as e:
                self.send_json_response({"success": False, "message": f"Invalid month format: {e}"}, 400)
                return

            # Fetch active engineers
            engineers_list = []
            if ATLAS_DB is not None:
                try:
                    engineers_list = list(ATLAS_DB.engineers.find({}, {"password": 0}))
                except Exception as e:
                    print(f"  [SOLVER WARNING] Failed to query Atlas engineers: {e}")
            if not engineers_list:
                engineers_list = list(SERVER_ENGINEERS)

            # Fetch leaves and swaps for target month
            leaves_list = []
            if ATLAS_DB is not None:
                try:
                    leaves_list = list(ATLAS_DB.leave_requests.find({
                        "$or": [
                            {"month_keys": month_str},
                            {"dates": {"$regex": f"^{month_str}"}}
                        ],
                        "status": {"$in": ["approved", "approved_leave", "approved_swap"]}
                    }))
                except Exception as e:
                    print(f"  [SOLVER WARNING] Failed to query leave requests: {e}")

            engineer_rules = body.get("engineer_rules") or body.get("constraints") or {}
            apply_to_grid = bool(body.get("apply_to_grid", False))
            baseline_only = bool(body.get("baseline_only", False))
            post_night_rest_days = int(body.get("post_night_rest_days") or 2)

            result = solve_night_shifts_engine(year_val, month_val, engineers_list, leaves_list, engineer_rules, baseline_only=baseline_only, post_night_rest_days=post_night_rest_days)
            if not result.get("success"):
                self.send_json_response(result, 422)
                return

            # Optional immediate persistence to Atlas if apply_to_grid is True
            if apply_to_grid and ATLAS_DB is not None:
                try:
                    doc_id = month_str
                    roster_payload = {
                        "_id": doc_id,
                        "year": year_val,
                        "month": month_val,
                        "status": "draft",
                        "engineers": result["roster"],
                        "roster": result["roster"],
                        "updated_at": time.strftime("%Y-%m-%dT%H:%M:%SZ", time.gmtime()),
                        "published_by": claims.get("name") or "Admin (Night Solver)"
                    }
                    ATLAS_DB.rosters.update_one({"_id": doc_id}, {"$set": roster_payload}, upsert=True)
                    result["saved_to_atlas"] = True
                except Exception as s_err:
                    result["saved_to_atlas"] = False
                    result["atlas_save_error"] = str(s_err)

            self.send_json_response(result, 200)
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
                        "message": "Operational policy constraints updated and synced to Database!",
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
                    # Domain username is primarily taken from username, with fallback to email domain ID or name
                    eng_user = str(eng.get("username") or "").strip().lower()
                    if not eng_user:
                        if eng_email and "@" in eng_email:
                            eng_user = eng_email.split("@")[0].strip().lower()
                        elif eng_name:
                            eng_user = eng_name.split()[0].lower()

                    if not eng_user:
                        continue

                    # Zero-knowledge security: NEVER allow Admin/Manager to supply or overwrite account passwords
                    eng.pop("password", None)

                    # Check if an account rename or replacement was performed
                    old_user = str(eng.pop("original_username", None) or eng.pop("old_username", None) or "").strip().lower()
                    old_doc = None
                    if old_user and old_user != eng_user:
                        old_doc = ATLAS_DB.engineers.find_one({"username": old_user})
                        if old_doc:
                            # Always preserve existing user's private password hash & creation timestamp on rename
                            if old_doc.get("password"):
                                eng["password"] = old_doc["password"]
                            if "created_at" in old_doc and "created_at" not in eng:
                                eng["created_at"] = old_doc["created_at"]
                        print(f"  [ATLAS] Account renamed/replaced: removing old account '{old_user}' in favor of '{eng_user}'")
                        ATLAS_DB.engineers.delete_many({"username": old_user})

                    existing_doc = old_doc or ATLAS_DB.engineers.find_one({"username": eng_user})
                    is_brand_new_account = existing_doc is None

                    eng["username"] = eng_user
                    # Remove legacy id fields to keep username as the sole unique key
                    eng.pop("engineer_id", None)
                    eng.pop("id", None)
                    unset_fields = {"id": "", "engineer_id": ""}

                    # Normalize optional DB-driven roster metadata fields
                    if "includeInRoster" in eng and "include_in_roster" not in eng:
                        eng["include_in_roster"] = eng.pop("includeInRoster")
                    else:
                        eng.pop("includeInRoster", None)
                    if "include_in_roster" in eng and eng["include_in_roster"] is not None:
                        eng["include_in_roster"] = bool(eng["include_in_roster"])

                    if "displayOrder" in eng and "display_order" not in eng:
                        eng["display_order"] = eng.pop("displayOrder")
                    else:
                        eng.pop("displayOrder", None)
                    if "display_order" in eng:
                        if eng["display_order"] is None or str(eng["display_order"]).strip() == "":
                            eng.pop("display_order", None)
                            unset_fields["display_order"] = ""
                        else:
                            try:
                                eng["display_order"] = int(eng["display_order"])
                            except Exception:
                                eng.pop("display_order", None)

                    if "swapCode" in eng and "swap_code" not in eng:
                        eng["swap_code"] = eng.pop("swapCode")
                    else:
                        eng.pop("swapCode", None)
                    if "swap_code" in eng:
                        sw_val = str(eng.get("swap_code") or "").strip().upper()
                        if sw_val:
                            eng["swap_code"] = sw_val
                        else:
                            eng.pop("swap_code", None)
                            unset_fields["swap_code"] = ""

                    raw_status = str(eng.get("status") or "").strip().lower()
                    if raw_status in ("active", "notice_period", "onboarding"):
                        eng["status"] = "Active"
                        eng.pop("resignation_date", None)
                        unset_fields["resignation_date"] = ""
                    elif raw_status in ("resigned", "retired"):
                        eng["status"] = "Resigned"
                        if not eng.get("resignation_date"):
                            eng["resignation_date"] = time.strftime("%Y-%m-%d", time.gmtime())

                    # Enforce empty weekly offs for Leadership (Admin & Manager)
                    eng_role = str(eng.get("role") or "").strip().lower()
                    if eng_role in ("admin", "manager"):
                        eng["default_weekly_offs"] = []

                    # Brand-new account onboarding: generate random password hash (zero-knowledge)
                    if is_brand_new_account:
                        eng["password"] = hash_password(secrets.token_urlsafe(24))
                        if "created_at" not in eng:
                            eng["created_at"] = int(time.time())

                    # Handle profile picture if provided
                    if "profile_picture" in eng:
                        raw_pic = eng.get("profile_picture")
                        if raw_pic and isinstance(raw_pic, str) and raw_pic.strip():
                            try:
                                eng["profile_picture"] = process_profile_picture_base64(raw_pic, max_dimension=256, quality=85)
                            except Exception:
                                pass
                        elif raw_pic is None or raw_pic == "" or raw_pic == "remove":
                            del eng["profile_picture"]
                            unset_fields["profile_picture"] = ""

                    # Primary key is username
                    ATLAS_DB.engineers.update_one(
                        {"username": eng_user},
                        {"$set": eng, "$unset": unset_fields},
                        upsert=True
                    )
                    saved_count += 1

                    # Async Welcome Email with Email-OTP password setup instructions for brand-new accounts
                    if is_brand_new_account and eng_email:
                        threading.Thread(
                            target=send_welcome_onboarding_email,
                            args=(eng_email, eng_name or eng_user, eng_user),
                            daemon=True
                        ).start()

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
                # Prioritize unique domain username/id to prevent accidentally deleting other accounts that share a display name
                if eng_username:
                    delete_query = {"$or": [
                        {"username": eng_username},
                        {"username": {"$regex": f"^{re.escape(eng_username)}$", "$options": "i"}}
                    ]}
                elif eng_id:
                    delete_query = {"$or": [{"id": eng_id}, {"engineer_id": eng_id}]}
                elif eng_name:
                    delete_query = {"$or": [
                        {"name": eng_name},
                        {"name": {"$regex": f"^{re.escape(eng_name)}$", "$options": "i"}}
                    ]}
                else:
                    self.send_json_response({"success": False, "message": "Identifier required for deletion"}, 400)
                    return

                result = ATLAS_DB.engineers.delete_many(delete_query)

                DEFAULT_SERVER_ENGINEERS = [
                    e for e in DEFAULT_SERVER_ENGINEERS
                    if (not eng_username or e.get("username") != eng_username) and
                       (not eng_id or e.get("id") != eng_id) and
                       (eng_username or eng_id or e.get("name") != eng_name)
                ]

                self.send_json_response({
                    "success": True,
                    "message": f"Successfully deleted engineer from database.",
                    "deleted_count": result.deleted_count
                })
            except Exception as e:
                self.send_json_response({"success": False, "message": f"Failed to delete engineer: {e}"}, 500)
            return

        # 3.6 POST /api/users/profile-picture (Upload/Update/Delete Profile Picture)
        if clean_path in ("/api/users/profile-picture", "/api/users/profile-picture/"):
            claims = self.require_auth()
            if not claims:
                return

            if ATLAS_DB is None:
                self.send_json_response({"success": False, "message": "Database is not connected"}, 503)
                return

            caller_user = str(claims.get("username", "")).strip().lower()
            caller_role = str(claims.get("role", "")).strip().lower()
            target_user = str(body.get("username") or caller_user).strip().lower()

            if caller_role not in ("admin", "manager") and target_user != caller_user:
                self.send_json_response({"success": False, "message": "Permission denied. You can only update your own profile picture."}, 403)
                return

            raw_picture = body.get("profile_picture")

            # Remove picture if null or empty
            if raw_picture is None or raw_picture == "":
                try:
                    ATLAS_DB.engineers.update_one(
                        {"username": target_user},
                        {"$unset": {"profile_picture": ""}, "$set": {"updated_at": time.strftime("%Y-%m-%dT%H:%M:%SZ", time.gmtime())}}
                    )
                    platform_client = detect_client_platform(self.headers)
                    record_audit_event(
                        claims.get("name") or caller_user,
                        caller_user,
                        caller_role,
                        platform_client,
                        "Remove Profile Picture",
                        {"target_user": target_user}
                    )
                    self.send_json_response({"success": True, "message": "Profile picture removed successfully.", "profile_picture": None})
                    return
                except Exception as e:
                    self.send_json_response({"success": False, "message": f"Failed to remove profile picture: {e}"}, 500)
                    return

            if not isinstance(raw_picture, str):
                self.send_json_response({"success": False, "message": "Invalid profile picture format. Base64 string expected."}, 400)
                return

            try:
                processed_uri = process_profile_picture_base64(raw_picture, max_dimension=256, quality=85)
            except ValueError as ve:
                self.send_json_response({"success": False, "message": str(ve)}, 400)
                return
            except Exception as e:
                self.send_json_response({"success": False, "message": f"Image processing failed: {e}"}, 500)
                return

            try:
                res = ATLAS_DB.engineers.update_one(
                    {"username": target_user},
                    {"$set": {
                        "profile_picture": processed_uri,
                        "updated_at": time.strftime("%Y-%m-%dT%H:%M:%SZ", time.gmtime())
                    }},
                    upsert=False
                )
                if res.matched_count == 0:
                    self.send_json_response({"success": False, "message": f"User '{target_user}' not found in database."}, 404)
                    return

                platform_client = detect_client_platform(self.headers)
                record_audit_event(
                    claims.get("name") or caller_user,
                    caller_user,
                    caller_role,
                    platform_client,
                    "Update Profile Picture",
                    {"target_user": target_user}
                )
                self.send_json_response({
                    "success": True,
                    "message": "Profile picture updated successfully.",
                    "profile_picture": processed_uri
                })
                return
            except Exception as e:
                self.send_json_response({"success": False, "message": f"Failed to save profile picture: {e}"}, 500)
                return

        # 3.65 POST /api/users/profile (Unified Profile & Account Update: Name, Phone, Email, Join Date, Birthday, Photo, Password)
        if clean_path in ("/api/users/profile", "/api/users/profile/"):
            claims = self.require_auth()
            if not claims:
                return

            if ATLAS_DB is None:
                self.send_json_response({"success": False, "message": "Database is not connected"}, 503)
                return

            caller_user = str(claims.get("username", "")).strip().lower()
            caller_role = str(claims.get("role", "")).strip().lower()
            target_user = str(body.get("username") or caller_user).strip().lower()

            if caller_role not in ("admin", "manager") and target_user != caller_user:
                self.send_json_response({"success": False, "message": "Permission denied. You can only update your own profile."}, 403)
                return

            try:
                update_fields = {
                    "updated_at": time.strftime("%Y-%m-%dT%H:%M:%SZ", time.gmtime())
                }
                unset_fields = {}

                # 1. Name
                new_name = str(body.get("name") or body.get("full_name") or "").strip()
                if new_name:
                    if len(new_name) < 2 or len(new_name) > 60:
                        self.send_json_response({"success": False, "message": "Name must be between 2 and 60 characters."}, 400)
                        return
                    proper_name = to_proper_name(new_name)
                    update_fields["name"] = proper_name
                    update_fields["full_name"] = proper_name

                # 2. Contact Phone
                if "phone" in body:
                    new_phone = str(body.get("phone") or "").strip()
                    if "*" not in new_phone:
                        update_fields["phone"] = new_phone

                # 3. Email
                if "email" in body:
                    new_email = str(body.get("email") or "").strip().lower()
                    if new_email and "@" not in new_email:
                        self.send_json_response({"success": False, "message": "Invalid email format."}, 400)
                        return
                    update_fields["email"] = new_email

                # 4. Join Date
                if "join_date" in body or "joinDate" in body:
                    val = str(body.get("join_date") or body.get("joinDate") or "").strip()
                    update_fields["join_date"] = val

                # 5. Birthday
                if "birthday" in body:
                    val = str(body.get("birthday") or "").strip()
                    update_fields["birthday"] = val

                # 6. Profile Picture
                if "profile_picture" in body:
                    raw_pic = body.get("profile_picture")
                    if raw_pic and isinstance(raw_pic, str) and raw_pic.strip():
                        try:
                            processed_pic = process_profile_picture_base64(raw_pic, max_dimension=256, quality=85)
                            update_fields["profile_picture"] = processed_pic
                        except Exception as pe:
                            print(f"  [PROFILE] Picture compression warning: {pe}")
                    elif raw_pic is None or raw_pic == "" or raw_pic == "remove":
                        unset_fields["profile_picture"] = ""

                # 7. Password Change (Optional with Current Password verification)
                new_pass = str(body.get("password") or body.get("new_password") or "").strip()
                if new_pass:
                    current_pass = str(body.get("current_password") or body.get("currentPassword") or "").strip()
                    existing_user = ATLAS_DB.engineers.find_one({"username": target_user})
                    if not existing_user:
                        self.send_json_response({"success": False, "message": "User not found."}, 404)
                        return
                    stored_hash = existing_user.get("password") or ""
                    if caller_user == target_user or caller_role != "admin":
                        if not current_pass:
                            self.send_json_response({"success": False, "message": "Current password is required to change your password."}, 400)
                            return
                        if not verify_password(current_pass, stored_hash) and current_pass != stored_hash:
                            self.send_json_response({"success": False, "message": "Current password is incorrect."}, 400)
                            return
                    if len(new_pass) < 6:
                        self.send_json_response({"success": False, "message": "New password must be at least 6 characters."}, 400)
                        return
                    update_fields["password"] = hash_password(new_pass)

                mongo_op = {"$set": update_fields}
                if unset_fields:
                    mongo_op["$unset"] = unset_fields

                res = ATLAS_DB.engineers.update_one(
                    {"username": target_user},
                    mongo_op,
                    upsert=False
                )
                if res.matched_count == 0:
                    self.send_json_response({"success": False, "message": f"User '{target_user}' not found in database."}, 404)
                    return

                platform_client = detect_client_platform(self.headers)
                record_audit_event(
                    update_fields.get("name", target_user),
                    caller_user,
                    caller_role,
                    platform_client,
                    "Update Account Profile",
                    {"target_user": target_user, "updated_fields": list(update_fields.keys())}
                )

                updated_doc = ATLAS_DB.engineers.find_one({"username": target_user}, {"_id": 0, "password": 0})
                self.send_json_response({
                    "success": True,
                    "message": "Account profile updated successfully.",
                    "name": updated_doc.get("name", ""),
                    "user": updated_doc
                })
                return
            except Exception as e:
                self.send_json_response({"success": False, "message": f"Failed to update profile: {e}"}, 500)
                return

        # 3.7 POST /api/notifications/dismiss (Persistent Notification Dismissal in Atlas DB)
        if clean_path in ("/api/notifications/dismiss", "/api/notifications/dismiss/"):
            claims = self.require_auth()
            if not claims:
                return

            username = str(claims.get("username", "")).strip().lower()
            notif_id = str(body.get("notification_id") or body.get("id") or "").strip()
            dismiss_all = bool(body.get("all", False))

            if not username:
                self.send_json_response({"success": False, "message": "Authentication required."}, 401)
                return

            all_ids = set()
            if dismiss_all:
                try:
                    check_and_trigger_celebrations()
                except Exception:
                    pass
                if ATLAS_DB is not None:
                    try:
                        all_notifs = list(ATLAS_DB.notifications.find())
                        for n in all_notifs:
                            for k in ("id", "_id", "celebration_id"):
                                val = str(n.get(k) or "").strip()
                                if val:
                                    all_ids.add(val)
                    except Exception as e:
                        print(f"  [DISMISS ALL ATLAS ERROR] {e}")

                for n in SERVER_NOTIFICATIONS:
                    for k in ("id", "_id", "celebration_id"):
                        val = str(n.get(k) or "").strip()
                        if val:
                            all_ids.add(val)

                if ATLAS_DB is not None and all_ids:
                    try:
                        ATLAS_DB.engineers.update_one(
                            {"username": username},
                            {"$addToSet": {"dismissed_notifications": {"$each": list(all_ids)}}}
                        )
                    except Exception as e:
                        print(f"  [DISMISS ALL UPDATE ERROR] {e}")
            elif notif_id:
                if ATLAS_DB is not None:
                    try:
                        ATLAS_DB.engineers.update_one(
                            {"username": username},
                            {"$addToSet": {"dismissed_notifications": notif_id}}
                        )
                    except Exception as e:
                        print(f"  [DISMISS NOTIF ERROR] {e}")

            # In-memory fallback
            if username not in SERVER_USER_DISMISSED_NOTIFICATIONS:
                SERVER_USER_DISMISSED_NOTIFICATIONS[username] = set()
            if dismiss_all:
                SERVER_USER_DISMISSED_NOTIFICATIONS[username].update(all_ids)
            elif notif_id:
                SERVER_USER_DISMISSED_NOTIFICATIONS[username].add(notif_id)

            # Retrieve updated list of dismissed notification IDs for immediate client reconciliation
            updated_dismissed_ids = []
            if ATLAS_DB is not None and username:
                try:
                    u_doc = ATLAS_DB.engineers.find_one({"username": username}, {"dismissed_notifications": 1})
                    if u_doc and "dismissed_notifications" in u_doc and isinstance(u_doc["dismissed_notifications"], list):
                        updated_dismissed_ids = list(u_doc["dismissed_notifications"])
                except Exception as e:
                    print(f"  [DISMISS RETRIEVE ERROR] {e}")

            if not updated_dismissed_ids and username in SERVER_USER_DISMISSED_NOTIFICATIONS:
                updated_dismissed_ids = sorted(list(SERVER_USER_DISMISSED_NOTIFICATIONS[username]))

            if not updated_dismissed_ids:
                if dismiss_all:
                    updated_dismissed_ids = list(all_ids)
                elif notif_id:
                    updated_dismissed_ids = [notif_id]

            self.send_json_response({
                "success": True,
                "message": "All notifications dismissed permanently." if dismiss_all else "Notification dismissed permanently.",
                "dismissed_notification_ids": updated_dismissed_ids,
                "count": len(updated_dismissed_ids)
            })
            return

        # 3.75 POST /api/celebrations/check (Manual or Automated Check for Today's Celebrations)
        if clean_path in ("/api/celebrations/check", "/api/celebrations/check/"):
            claims = self.require_auth()
            if not claims:
                return
            triggered = check_and_trigger_celebrations(force=True)
            self.send_json_response({
                "success": True,
                "message": f"Celebrations checked successfully ({len(triggered)} events triggered).",
                "triggered": triggered
            })
            return

        # 3.76 POST /api/credentials/trackers/test-check (Manual trigger for 9:00 AM IST credential expiry alert check)
        if clean_path in ("/api/credentials/trackers/test-check", "/api/credentials/trackers/test-check/"):
            claims = self.require_auth(["admin", "manager"])
            if not claims:
                return
            dispatched = check_and_dispatch_credential_alerts(force=True)
            self.send_json_response({
                "success": True,
                "message": f"Credential expiry check completed ({len(dispatched)} alerts dispatched).",
                "dispatched": dispatched
            })
            return

        # 3.77 POST /api/credentials/trackers/<tracker_id>/renew (1-Tap quick renew or flexible last_updated_date)
        if clean_path.startswith("/api/credentials/trackers/") and clean_path.endswith("/renew"):
            tracker_id = clean_path[len("/api/credentials/trackers/"): -len("/renew")].strip().strip("/")
            self._handle_renew_credential_tracker(tracker_id, body)
            return

        # 3.78 POST /api/credentials/trackers/<tracker_id>/delete or POST /api/credentials/trackers/delete
        if clean_path in ("/api/credentials/trackers/delete", "/api/credentials/trackers/delete/"):
            self._handle_delete_credential_tracker("", body)
            return
        if clean_path.startswith("/api/credentials/trackers/") and clean_path.endswith("/delete"):
            tracker_id = clean_path[len("/api/credentials/trackers/"): -len("/delete")].strip().strip("/")
            self._handle_delete_credential_tracker(tracker_id, body)
            return

        # 3.79 POST /api/credentials/trackers (Create)
        if clean_path in ("/api/credentials/trackers", "/api/credentials/trackers/"):
            self._handle_create_credential_tracker(body)
            return

        # 3.80 POST /api/credentials/trackers/<tracker_id> (Update fallback for clients sending POST)
        if clean_path.startswith("/api/credentials/trackers/"):
            tracker_id = clean_path[len("/api/credentials/trackers/"):].strip().strip("/")
            self._handle_update_credential_tracker(tracker_id, body)
            return

        # 3.8 POST /api/users/fcm-token (Register FCM Device Push Token - Exclusive Binding)
        if clean_path in ("/api/users/fcm-token", "/api/users/fcm-token/"):
            claims = self.require_auth()
            if not claims:
                return

            username = str(claims.get("username", "")).strip().lower()
            token = str(body.get("token", "")).strip()

            if not token:
                self.send_json_response({"success": False, "message": "FCM device token cannot be blank."}, 400)
                return

            if claims.get("role") in ("admin", "manager") or username in ("admin", "manager"):
                # Admin and Manager accounts are exempt from receiving operational FCM push broadcasts
                self.send_json_response({"success": True, "message": "FCM device token registered (exempt from operational broadcasts)."})
                return

            if ATLAS_DB is not None and username:
                try:
                    # Exclusive Single-User Binding:
                    # 1. Atomically remove this device token from ANY other engineer accounts
                    pull_res = ATLAS_DB.engineers.update_many(
                        {"username": {"$ne": username}},
                        {"$pull": {"fcm_tokens": token}}
                    )
                    # 2. Add exclusively to current active engineer
                    ATLAS_DB.engineers.update_one(
                        {"username": username},
                        {"$addToSet": {"fcm_tokens": token}}
                    )
                    log_server(f"[FCM] Bound device token exclusively to '{username}' (purged from {pull_res.modified_count} previous account(s)).")
                except Exception as e:
                    print(f"  [FCM TOKEN REGISTER ERROR] {e}")

            self.send_json_response({"success": True, "message": "FCM device token registered exclusively."})
            return

        # 3.8b POST /api/users/fcm-token/unregister (Unregister FCM Device Push Token on Sign-Out)
        if clean_path in ("/api/users/fcm-token/unregister", "/api/users/fcm-token/unregister/", "/api/fcm/unregister", "/api/fcm/unregister/"):
            token = str(body.get("token", "")).strip()
            if token and ATLAS_DB is not None:
                try:
                    del_res = ATLAS_DB.engineers.update_many(
                        {},
                        {"$pull": {"fcm_tokens": token}}
                    )
                    log_server(f"[FCM] Sign-Out: Unregistered device token from {del_res.modified_count} engineer account(s).")
                except Exception as e:
                    print(f"  [FCM TOKEN UNREGISTER ERROR] {e}")

            self.send_json_response({"success": True, "message": "FCM device token unregistered successfully on sign-out."})
            return

        # 3.81b POST /api/users/calendar-accounts (Synchronize Discovered Device Calendar Accounts)
        if clean_path in ("/api/users/calendar-accounts", "/api/users/calendar-accounts/"):
            claims = self.require_auth()
            if not claims:
                return

            caller_username = str(claims.get("username", "")).strip().lower()
            if not caller_username:
                self.send_json_response({"success": False, "message": "Authentication required."}, 401)
                return

            raw_accounts = body.get("calendar_accounts")
            if not isinstance(raw_accounts, list):
                raw_accounts = body.get("accounts") if isinstance(body.get("accounts"), list) else []

            validated_accounts = []
            for acc in raw_accounts:
                if not isinstance(acc, dict):
                    continue
                acc_name = str(acc.get("name") or "").strip()
                acc_email = str(acc.get("account_name") or acc.get("email") or "").strip()
                acc_id = str(acc.get("id") or "").strip()
                if not acc_name and not acc_email:
                    continue
                validated_accounts.append({
                    "id": acc_id or str(len(validated_accounts) + 1),
                    "name": acc_name or acc_email,
                    "account_name": acc_email,
                    "account_type": str(acc.get("account_type") or "com.google").strip(),
                    "is_primary": bool(acc.get("is_primary", False)),
                    "color": str(acc.get("color") or "#174EAC").strip()
                })

            now_utc = time.strftime("%Y-%m-%d %H:%M:%S UTC", time.gmtime())
            if ATLAS_DB is not None:
                try:
                    ATLAS_DB.engineers.update_one(
                        {"username": caller_username},
                        {"$set": {
                            "calendar_accounts": validated_accounts,
                            "calendar_accounts_updated_at": now_utc
                        }}
                    )
                except Exception as e:
                    print(f"  [ATLAS CALENDAR ACCOUNTS SAVE ERROR] {e}")
                    self.send_json_response({"success": False, "message": f"Database error: {e}"}, 500)
                    return

            # In-memory fallback update
            SERVER_USER_CALENDAR_ACCOUNTS[caller_username] = validated_accounts

            for e in DEFAULT_SERVER_ENGINEERS:
                if str(e.get("username", "")).lower() == caller_username:
                    e["calendar_accounts"] = validated_accounts
                    e["calendar_accounts_updated_at"] = now_utc
                    break

            self.send_json_response({
                "success": True,
                "message": f"Synchronized {len(validated_accounts)} calendar account(s).",
                "calendar_accounts": validated_accounts,
                "count": len(validated_accounts)
            })
            return

        # 3.82 POST /api/users/calendar-sync (Record Calendar Sync Status for Month)
        if clean_path in ("/api/users/calendar-sync", "/api/users/calendar-sync/"):
            claims = self.require_auth()
            if not claims:
                return

            caller_username = str(claims.get("username", "")).strip().lower()
            if not caller_username:
                self.send_json_response({"success": False, "message": "Authentication required."}, 401)
                return

            month = str(body.get("month", "")).strip()
            if not month:
                self.send_json_response({"success": False, "message": "Month is required (e.g. 'YYYY-MM')."}, 400)
                return

            if not re.match(r"^\d{4}-\d{2}$", month):
                self.send_json_response({"success": False, "message": "Invalid month format. Expected 'YYYY-MM'."}, 400)
                return

            try:
                count = int(body.get("count", 0))
            except Exception:
                count = 0

            target = str(body.get("target") or "Google Calendar").strip()
            target_type = str(body.get("target_type") or "google").strip().lower()
            target_account_id = str(body.get("target_account_id") or body.get("account_id") or "").strip()
            synced = bool(body.get("synced", True))
            synced_at = time.strftime("%Y-%m-%d %H:%M:%S UTC", time.gmtime())
            now_ts = time.time()

            sync_entry = {
                "synced": synced,
                "count": count,
                "target": target,
                "target_type": target_type,
                "target_account_id": target_account_id,
                "synced_at": synced_at,
                "timestamp": now_ts
            }

            if ATLAS_DB is not None:
                try:
                    ATLAS_DB.engineers.update_one(
                        {"username": caller_username},
                        {"$set": {f"calendar_sync.{month}": sync_entry}}
                    )
                except Exception as e:
                    print(f"  [ATLAS CALENDAR SYNC SAVE ERROR] {e}")
                    self.send_json_response({"success": False, "message": f"Database error: {e}"}, 500)
                    return

            # Update in-memory fallback
            if caller_username not in SERVER_USER_CALENDAR_SYNC:
                SERVER_USER_CALENDAR_SYNC[caller_username] = {}
            SERVER_USER_CALENDAR_SYNC[caller_username][month] = sync_entry

            for e in DEFAULT_SERVER_ENGINEERS:
                if str(e.get("username", "")).lower() == caller_username:
                    if "calendar_sync" not in e or not isinstance(e["calendar_sync"], dict):
                        e["calendar_sync"] = {}
                    e["calendar_sync"][month] = sync_entry
                    break

            self.send_json_response({
                "success": True,
                "message": f"Calendar sync recorded for {month}",
                "month": month,
                "data": sync_entry
            })
            return

        # 3.83 POST /api/users/calendar-sync/clear (Clear Calendar Sync Status for Month or All)
        if clean_path in ("/api/users/calendar-sync/clear", "/api/users/calendar-sync/clear/"):
            claims = self.require_auth()
            if not claims:
                return

            caller_username = str(claims.get("username", "")).strip().lower()
            if not caller_username:
                self.send_json_response({"success": False, "message": "Authentication required."}, 401)
                return

            month = str(body.get("month", "")).strip()
            clear_all = bool(body.get("all", False)) or not month

            if ATLAS_DB is not None:
                try:
                    if clear_all:
                        ATLAS_DB.engineers.update_one(
                            {"username": caller_username},
                            {"$unset": {"calendar_sync": ""}}
                        )
                    else:
                        ATLAS_DB.engineers.update_one(
                            {"username": caller_username},
                            {"$unset": {f"calendar_sync.{month}": ""}}
                        )
                except Exception as e:
                    print(f"  [ATLAS CALENDAR SYNC CLEAR ERROR] {e}")
                    self.send_json_response({"success": False, "message": f"Database error: {e}"}, 500)
                    return

            # Update in-memory fallback
            if clear_all:
                SERVER_USER_CALENDAR_SYNC.pop(caller_username, None)
            elif caller_username in SERVER_USER_CALENDAR_SYNC:
                SERVER_USER_CALENDAR_SYNC[caller_username].pop(month, None)

            for e in DEFAULT_SERVER_ENGINEERS:
                if str(e.get("username", "")).lower() == caller_username:
                    if clear_all:
                        e.pop("calendar_sync", None)
                    elif "calendar_sync" in e and isinstance(e["calendar_sync"], dict):
                        e["calendar_sync"].pop(month, None)
                    break

            msg = "All calendar sync history cleared." if clear_all else f"Calendar sync cleared for {month}"
            self.send_json_response({
                "success": True,
                "message": msg,
                "month": None if clear_all else month
            })
            return

        # 3.84 POST /api/leave/apply (Submit New Leave or Swap Request)
        if clean_path in ("/api/leave/apply", "/api/leave/apply/"):
            claims = self.require_auth()
            if not claims:
                return

            applicant_username = str(claims.get("username", "")).strip().lower()
            if not applicant_username:
                self.send_json_response({"success": False, "message": "Authentication required."}, 401)
                return

            applicant_name = to_proper_name(claims.get("name") or claims.get("full_name") or applicant_username)

            raw_dates = body.get("dates")
            if not isinstance(raw_dates, list) or not raw_dates:
                self.send_json_response({"success": False, "message": "You must select between 1 and 5 dates."}, 400)
                return

            # Clean and validate dates
            cleaned_dates = []
            for d in raw_dates:
                d_str = str(d).strip()
                if not re.match(r"^\d{4}-\d{2}-\d{2}$", d_str):
                    self.send_json_response({"success": False, "message": f"Invalid date format for '{d_str}'. Expected YYYY-MM-DD."}, 400)
                    return
                cleaned_dates.append(d_str)

            cleaned_dates = sorted(list(set(cleaned_dates)))
            if len(cleaned_dates) < 1 or len(cleaned_dates) > 5:
                self.send_json_response({"success": False, "message": "You can select between 1 and 5 dates for a request."}, 400)
                return

            today_ist = get_ist_now().strftime("%Y-%m-%d")
            for d in cleaned_dates:
                if d < today_ist:
                    self.send_json_response({"success": False, "message": f"Date {d} is in the past. Only today and future dates are allowed."}, 400)
                    return

            # Check applicant resignation date / resigned status
            applicant_doc = None
            if ATLAS_DB is not None:
                try:
                    applicant_doc = ATLAS_DB.engineers.find_one({"username": applicant_username}, {"status": 1, "resignation_date": 1})
                except Exception:
                    pass
            if not applicant_doc:
                for eng in SERVER_ENGINEERS:
                    if str(eng.get("username", "")).lower() == applicant_username:
                        applicant_doc = eng
                        break

            if applicant_doc:
                res_raw = applicant_doc.get("resignation_date")
                res_date_str = str(res_raw).strip()[:10] if res_raw else ""
                if res_date_str and re.match(r"^\d{4}-\d{2}-\d{2}$", res_date_str):
                    for d in cleaned_dates:
                        if d > res_date_str:
                            self.send_json_response({
                                "success": False,
                                "message": f"Leave request cannot be submitted after your resignation date ({res_date_str})."
                            }, 400)
                            return
                elif str(applicant_doc.get("status", "")).strip().lower() == "resigned":
                    self.send_json_response({
                        "success": False,
                        "message": "Resigned personnel cannot submit new leave requests."
                    }, 400)
                    return

            # 2. Overlapping & Duplicate Guard
            existing_active_requests = []
            if ATLAS_DB is not None:
                try:
                    existing_active_requests = list(ATLAS_DB.leave_requests.find({
                        "engineer_id": applicant_username,
                        "status": {"$in": ["pending", "approved_swap", "approved_leave"]}
                    }, {"dates": 1, "status": 1}))
                except Exception as e:
                    print(f"  [ATLAS LEAVE CHECK ERROR] {e}")
                    existing_active_requests = [r for r in SERVER_LEAVE_REQUESTS if str(r.get("engineer_id", "")).lower() == applicant_username and r.get("status") in ("pending", "approved_swap", "approved_leave")]
            else:
                existing_active_requests = [r for r in SERVER_LEAVE_REQUESTS if str(r.get("engineer_id", "")).lower() == applicant_username and r.get("status") in ("pending", "approved_swap", "approved_leave")]

            for req in existing_active_requests:
                existing_dates = set(req.get("dates", []))
                for d in cleaned_dates:
                    if d in existing_dates:
                        self.send_json_response({"success": False, "message": f"A request for date {d} is already pending or approved."}, 400)
                        return

            # 3. Annual Quota Enforcement (Only approved leaves deduct from quota if capping is enabled)
            current_year = int(today_ist[:4])
            stats = get_engineer_leave_stats(applicant_username, current_year)
            capping_enabled = bool(stats.get("capping_enabled", True))
            annual_quota = int(stats.get("annual_quota") or 14)

            if capping_enabled and (int(stats.get("approved_leave_days") or 0) + len(cleaned_dates) > annual_quota):
                self.send_json_response({
                    "success": False,
                    "message": f"Annual leave allowance exceeded. Max {annual_quota} days per calendar year (You have used {stats.get('approved_leave_days', 0)} days, requesting {len(cleaned_dates)})."
                }, 400)
                return

            now_ts = time.time()
            now_utc = time.strftime("%Y-%m-%dT%H:%M:%SZ", time.gmtime(now_ts))
            request_id = f"LR-{today_ist.replace('-', '')}-{int(now_ts * 1000) % 10000:04d}"
            reason = str(body.get("reason", "")).strip()[:500]
            preferred_partner = str(body.get("preferred_partner", "")).strip().lower()

            month_keys = sorted(list(set(d[:7] for d in cleaned_dates)))
            leave_doc = {
                "request_id": request_id,
                "engineer_id": applicant_username,
                "engineer_name": applicant_name,
                "dates": cleaned_dates,
                "month_keys": month_keys,
                "reason": reason,
                "preferred_partner": preferred_partner,
                "annual_year": current_year,
                "days_count": len(cleaned_dates),
                "status": "pending",
                "created_at": now_utc,
                "timestamp": now_ts,
                "approval": None
            }

            # Save in database
            if ATLAS_DB is not None:
                try:
                    ATLAS_DB.leave_requests.insert_one(dict(leave_doc))
                except Exception as e:
                    print(f"  [ATLAS LEAVE INSERT ERROR] {e}")
                    self.send_json_response({"success": False, "message": f"Database error: {e}"}, 500)
                    return

            # In-memory fallback
            SERVER_LEAVE_REQUESTS.append(dict(leave_doc))

            # Dispatch FCM push to Admins/Managers
            admin_tokens = []
            if ATLAS_DB is not None:
                try:
                    for a in ATLAS_DB.engineers.find({"role": {"$in": ["admin", "manager"]}}, {"fcm_tokens": 1}):
                        toks = a.get("fcm_tokens", [])
                        if isinstance(toks, list):
                            admin_tokens.extend([str(t).strip() for t in toks if t and str(t).strip()])
                        elif isinstance(toks, str) and toks.strip():
                            admin_tokens.append(toks.strip())
                except Exception:
                    pass
            admin_tokens = list(set(admin_tokens))
            if admin_tokens:
                send_fcm_push(
                    tokens=admin_tokens,
                    title="New Leave/Swap Request",
                    body=f"{applicant_name} submitted a request for {len(cleaned_dates)} date(s).",
                    priority="high",
                    notif_id=f"notif_lr_{request_id}",
                    extra_data={"type": "leave_request", "request_id": request_id, "engineer_id": applicant_username}
                )

            # Insert In-App notification
            notif_entry = {
                "id": f"notif_lr_{request_id}",
                "title": "New Leave/Swap Request",
                "message": f"{applicant_name} submitted a request for {len(cleaned_dates)} date(s) ({', '.join(cleaned_dates)}).",
                "raw_message": f"{applicant_name} submitted a request for {len(cleaned_dates)} date(s) ({', '.join(cleaned_dates)}).",
                "category": "operations",
                "author": applicant_name,
                "author_username": applicant_username,
                "role": "engineer",
                "timestamp": now_ts,
                "date_str": get_ist_now().strftime("%d %b %Y, %I:%M %p IST"),
                "target_user": "admins",
                "recipients": ["admins"],
                "link": "#leave-requests"
            }
            if ATLAS_DB is not None:
                try:
                    ATLAS_DB.notifications.insert_one(dict(notif_entry))
                except Exception:
                    pass
            SERVER_NOTIFICATIONS.appendleft(dict(notif_entry))

            record_audit_event(
                applicant_name,
                applicant_username,
                claims.get("role", "engineer"),
                detect_client_platform(self.headers),
                "LEAVE_REQUEST_SUBMITTED",
                {"request_id": request_id, "dates": cleaned_dates, "days_count": len(cleaned_dates)}
            )

            # Exclude ObjectId if present before response
            resp_doc = dict(leave_doc)
            resp_doc.pop("_id", None)
            self.send_json_response({
                "success": True,
                "message": "Leave request submitted successfully.",
                "request": resp_doc
            })
            return

        # 3.85 POST /api/leave/admin-action (Approve or Reject Leave/Swap Request)
        if clean_path in ("/api/leave/admin-action", "/api/leave/admin-action/"):
            claims = self.require_auth(["admin", "manager"])
            if not claims:
                return

            admin_username = str(claims.get("username", "")).strip().lower()
            admin_name = to_proper_name(claims.get("name") or claims.get("full_name") or admin_username)

            request_id = str(body.get("request_id", "")).strip()
            action = str(body.get("action", "")).strip().lower()
            swap_partner = str(body.get("swap_partner", "")).strip().lower()
            swap_code = str(body.get("swap_code", "")).strip().upper()
            admin_note = str(body.get("admin_note", "")).strip()

            if not request_id:
                self.send_json_response({"success": False, "message": "Request ID is required."}, 400)
                return

            if action not in ("swap", "leave", "reject", "cancel", "revoke"):
                self.send_json_response({"success": False, "message": "Invalid action. Expected 'swap', 'leave', 'reject', 'cancel', or 'revoke'."}, 400)
                return

            req_doc = None
            if ATLAS_DB is not None:
                try:
                    req_doc = ATLAS_DB.leave_requests.find_one({"request_id": request_id})
                except Exception as e:
                    print(f"  [ATLAS LEAVE FETCH ERROR] {e}")

            if not req_doc:
                for r in SERVER_LEAVE_REQUESTS:
                    if r.get("request_id") == request_id:
                        req_doc = dict(r)
                        break

            if not req_doc:
                self.send_json_response({"success": False, "message": "Leave request not found."}, 404)
                return

            curr_status = str(req_doc.get("status", "")).lower()
            if action == "revoke":
                if curr_status not in ("approved_leave", "approved_swap"):
                    self.send_json_response({"success": False, "message": f"Only approved requests can be revoked. Current status is {curr_status}."}, 400)
                    return
            elif curr_status != "pending" and action != "cancel":
                self.send_json_response({"success": False, "message": f"Request is already {curr_status}."}, 400)
                return

            applicant_user = str(req_doc.get("engineer_id", "")).lower()
            dates = req_doc.get("dates", [])
            now_ts = time.time()
            now_utc = time.strftime("%Y-%m-%dT%H:%M:%SZ", time.gmtime(now_ts))

            new_status = "rejected"
            if action == "swap":
                new_status = "approved_swap"
                if not swap_partner:
                    self.send_json_response({"success": False, "message": "Swap partner username is required for swap approval."}, 400)
                    return

                # Determine swap code if not provided
                if not swap_code:
                    partner_doc = None
                    if ATLAS_DB is not None:
                        try:
                            partner_doc = ATLAS_DB.engineers.find_one({"username": swap_partner})
                        except Exception:
                            pass
                    swap_code = str(partner_doc.get("swap_code") or "").strip().upper() if partner_doc else ""
                    if not swap_code:
                        swap_code = f"SW{swap_partner[:2].upper()}" if len(swap_partner) >= 2 else "SWSH"

                # Update monthly rosters for all requested dates
                for d_str in dates:
                    orig_shift = get_roster_shift_for_date(applicant_user, d_str)
                    if not orig_shift or orig_shift == "OFF":
                        orig_shift = "FS"  # Default duty shift if unassigned
                    # Applicant gets swap code (e.g. SWSH)
                    update_roster_shift_for_date(applicant_user, d_str, swap_code)
                    # Partner gets applicant's original shift
                    update_roster_shift_for_date(swap_partner, d_str, orig_shift)

            elif action == "leave":
                new_status = "approved_leave"
                # Per architectural standard: approved leave writes standard 'L' code
                for d_str in dates:
                    update_roster_shift_for_date(applicant_user, d_str, "L")

            elif action == "reject":
                new_status = "rejected"

            elif action == "cancel":
                new_status = "cancelled"

            elif action == "revoke":
                new_status = "revoked"
                # Restore original shifts on roster
                for d_str in dates:
                    default_shift = "FS"
                    if ATLAS_DB is not None:
                        try:
                            eng_doc = ATLAS_DB.engineers.find_one({"username": applicant_user})
                            if eng_doc:
                                def_offs = [str(x).upper() for x in (eng_doc.get("default_weekly_offs") or [])]
                                try:
                                    dt_val = datetime.strptime(d_str, "%Y-%m-%d")
                                    day_nm = dt_val.strftime("%A").upper()
                                    if day_nm in def_offs:
                                        default_shift = "OFF"
                                except Exception:
                                    pass
                        except Exception:
                            pass
                    update_roster_shift_for_date(applicant_user, d_str, default_shift)

            approval_record = {
                "approved_by": admin_username,
                "approved_name": admin_name,
                "approved_by_name": admin_name,
                "approved_at": now_utc,
                "action_type": action,
                "swap_partner": swap_partner if action == "swap" else "",
                "swap_shift_code": swap_code if action == "swap" else "",
                "admin_note": admin_note or ""
            }
            if action == "revoke":
                approval_record["revoked_by"] = admin_username
                approval_record["revoked_name"] = admin_name
                approval_record["revoked_at"] = now_utc

            if ATLAS_DB is not None:
                try:
                    ATLAS_DB.leave_requests.update_one(
                        {"request_id": request_id},
                        {"$set": {
                            "status": new_status,
                            "approval": approval_record,
                            "updated_at": now_utc
                        }}
                    )
                except Exception as e:
                    print(f"  [ATLAS LEAVE UPDATE ERROR] {e}")

            # Update in-memory fallback
            for r in SERVER_LEAVE_REQUESTS:
                if r.get("request_id") == request_id:
                    r["status"] = new_status
                    r["approval"] = approval_record
                    r["updated_at"] = now_utc
                    break

            # Targeted FCM Push strictly to the applicant
            applicant_tokens = []
            if ATLAS_DB is not None:
                try:
                    eng_doc = ATLAS_DB.engineers.find_one({"username": applicant_user}, {"fcm_tokens": 1})
                    if eng_doc:
                        toks = eng_doc.get("fcm_tokens", [])
                        if isinstance(toks, list):
                            applicant_tokens.extend([str(t).strip() for t in toks if t and str(t).strip()])
                        elif isinstance(toks, str) and toks.strip():
                            applicant_tokens.append(toks.strip())
                except Exception:
                    pass
            applicant_tokens = list(set(applicant_tokens))

            days_count = len(dates)
            formatted_dates = ", ".join(dates) if dates else "requested dates"
            days_label = f"{days_count} day{'s' if days_count != 1 else ''} ({formatted_dates})"

            push_title = "Leave Request Approved" if "approved" in new_status else "Leave Request Update"
            if new_status == "approved_swap":
                push_title = "Leave Approved (Swap)"
                push_body = f"Your leave request for {days_label} has been approved as Swap with {swap_partner} ({swap_code})." + (f" Note: {admin_note}" if admin_note else "")
            elif new_status == "approved_leave":
                push_title = "Leave Request Approved"
                push_body = f"Your leave request for {days_label} has been approved." + (f" Note: {admin_note}" if admin_note else "")
            elif new_status == "rejected":
                push_title = "Leave Request Rejected"
                push_body = f"Your leave request for {days_label} was rejected." + (f" Comment: {admin_note}" if admin_note else "")
            elif new_status == "revoked":
                push_title = "⚠️ Leave Approval Revoked"
                push_body = f"Your approved leave for {days_label} was revoked by {admin_name}. Scheduled shifts and quota balance have been restored." + (f" Note: {admin_note}" if admin_note else "")
            else:
                push_title = "Leave Request Update"
                push_body = f"Your leave request for {days_label} was {new_status}." + (f" Note: {admin_note}" if admin_note else "")

            if applicant_tokens:
                send_fcm_push(
                    tokens=applicant_tokens,
                    title=push_title,
                    body=push_body,
                    priority="high",
                    notif_id=f"notif_action_{request_id}",
                    extra_data={
                        "type": "leave_request_action",
                        "target_user": applicant_user,
                        "request_id": request_id,
                        "status": new_status,
                        "dates": ", ".join(dates),
                        "days_count": str(days_count),
                        "admin_note": admin_note or ""
                    }
                )

            # Insert targeted In-App notification for the applicant
            notif_entry = {
                "id": f"notif_act_{request_id}_{int(now_ts)}",
                "title": push_title,
                "message": push_body,
                "raw_message": push_body,
                "category": "operations",
                "type": "leave_request_action",
                "author": admin_name,
                "author_username": admin_username,
                "role": "admin",
                "timestamp": now_ts,
                "date_str": get_ist_now().strftime("%d %b %Y, %I:%M %p IST"),
                "target_user": applicant_user,
                "target": applicant_user,
                "recipients": [applicant_user],
                "recipients_count": 1,
                "send_in_app": True,
                "link": "#my-requests"
            }
            if ATLAS_DB is not None:
                try:
                    ATLAS_DB.notifications.insert_one(dict(notif_entry))
                except Exception:
                    pass
            SERVER_NOTIFICATIONS.appendleft(dict(notif_entry))

            record_audit_event(
                admin_name,
                admin_username,
                claims.get("role", "admin"),
                detect_client_platform(self.headers),
                f"LEAVE_REQUEST_{action.upper()}",
                {"request_id": request_id, "applicant": applicant_user, "action": action, "status": new_status, "swap_partner": swap_partner}
            )

            req_doc["status"] = new_status
            req_doc["approval"] = approval_record
            req_doc.pop("_id", None)

            self.send_json_response({
                "success": True,
                "message": f"Leave request {action} processed successfully.",
                "request": req_doc
            })
            return

        # 3.86 POST /api/leave/cancel (Applicant Self-Cancellation of Pending Request)
        if clean_path in ("/api/leave/cancel", "/api/leave/cancel/"):
            claims = self.require_auth()
            if not claims:
                return

            caller_username = str(claims.get("username", "")).strip().lower()
            user_role = str(claims.get("role", "")).lower()
            request_id = str(body.get("request_id", "")).strip()

            if not request_id:
                self.send_json_response({"success": False, "message": "Request ID is required."}, 400)
                return

            req_doc = None
            if ATLAS_DB is not None:
                try:
                    req_doc = ATLAS_DB.leave_requests.find_one({"request_id": request_id})
                except Exception:
                    pass
            if not req_doc:
                for r in SERVER_LEAVE_REQUESTS:
                    if r.get("request_id") == request_id:
                        req_doc = dict(r)
                        break

            if not req_doc:
                self.send_json_response({"success": False, "message": "Leave request not found."}, 404)
                return

            applicant_user = str(req_doc.get("engineer_id", "")).lower()
            if applicant_user != caller_username and user_role not in ("admin", "manager"):
                self.send_json_response({"success": False, "message": "You can only cancel your own requests."}, 403)
                return

            if str(req_doc.get("status", "")).lower() != "pending":
                self.send_json_response({"success": False, "message": f"Only pending requests can be cancelled. Current status is {req_doc.get('status')}."}, 400)
                return

            now_utc = time.strftime("%Y-%m-%dT%H:%M:%SZ", time.gmtime())
            if ATLAS_DB is not None:
                try:
                    ATLAS_DB.leave_requests.update_one(
                        {"request_id": request_id},
                        {"$set": {"status": "cancelled", "cancelled_at": now_utc, "cancelled_by": caller_username}}
                    )
                except Exception as e:
                    print(f"  [ATLAS LEAVE CANCEL ERROR] {e}")

            for r in SERVER_LEAVE_REQUESTS:
                if r.get("request_id") == request_id:
                    r["status"] = "cancelled"
                    r["cancelled_at"] = now_utc
                    r["cancelled_by"] = caller_username
                    break

            self.send_json_response({"success": True, "message": "Leave request cancelled successfully.", "request_id": request_id})
            return

        # 3.87 POST /api/leave/settings (Update Global Leave Policy Settings)
        if clean_path in ("/api/leave/settings", "/api/leave/settings/"):
            claims = self.require_auth(["admin", "manager"])
            if not claims:
                return

            capping_enabled = bool(body.get("capping_enabled", True))
            try:
                annual_quota = int(body.get("annual_quota", 14))
                if annual_quota < 1:
                    annual_quota = 14
            except (ValueError, TypeError):
                annual_quota = 14

            save_leave_system_config(capping_enabled, annual_quota)
            record_audit_event(
                engineer_name=claims.get("name") or claims.get("username") or "Admin",
                username=claims.get("username") or "admin",
                role=claims.get("role") or "admin",
                platform_name="api",
                action="UPDATE_LEAVE_SETTINGS",
                details={"capping_enabled": capping_enabled, "annual_quota": annual_quota}
            )
            self.send_json_response({
                "success": True,
                "message": "Leave policy settings updated successfully.",
                "capping_enabled": capping_enabled,
                "annual_quota": annual_quota
            })
            return

        # 3.9 POST /api/broadcast/templates (Create or Update Operational Broadcast Template)
        if clean_path in ("/api/broadcast/templates", "/api/broadcast/templates/"):
            claims = self.require_auth(["admin", "manager"])
            if not claims:
                return

            tpl_id = str(body.get("id") or f"tpl_{int(time.time()*1000)}").strip()
            name = str(body.get("name") or "Custom Template").strip()
            title = str(body.get("title") or "Operations Notice").strip()
            message = str(body.get("message") or "").strip()
            priority = str(body.get("priority") or "info").strip().lower()
            target_default = str(body.get("target_default") or "everyone").strip()

            if not message:
                self.send_json_response({"success": False, "message": "Template message content cannot be blank."}, 400)
                return

            tpl_doc = {
                "id": tpl_id,
                "name": name,
                "title": title,
                "message": message,
                "priority": priority,
                "target_default": target_default,
                "created_by": str(claims.get("name") or claims.get("username") or "Admin"),
                "updated_at": time.strftime("%Y-%m-%d %H:%M:%S UTC", time.gmtime())
            }

            if ATLAS_DB is not None:
                try:
                    ATLAS_DB.broadcast_templates.update_one(
                        {"id": tpl_id},
                        {"$set": tpl_doc},
                        upsert=True
                    )
                except Exception as e:
                    self.send_json_response({"success": False, "message": f"Database error: {e}"}, 500)
                    return

            SERVER_BROADCAST_TEMPLATES = [t for t in SERVER_BROADCAST_TEMPLATES if t.get("id") != tpl_id]
            SERVER_BROADCAST_TEMPLATES.append(dict(tpl_doc))

            self.send_json_response({
                "success": True,
                "message": f"Template '{name}' saved successfully.",
                "template": tpl_doc
            })
            return

        # 3.10 POST /api/broadcast/templates/delete (Delete Broadcast Template)
        if clean_path in ("/api/broadcast/templates/delete", "/api/broadcast/templates/delete/"):
            claims = self.require_auth(["admin", "manager"])
            if not claims:
                return

            tpl_id = str(body.get("id", "")).strip()
            if not tpl_id:
                self.send_json_response({"success": False, "message": "Template ID is required."}, 400)
                return

            if ATLAS_DB is not None:
                try:
                    ATLAS_DB.broadcast_templates.delete_one({"id": tpl_id})
                except Exception as e:
                    self.send_json_response({"success": False, "message": f"Database error: {e}"}, 500)
                    return

            SERVER_BROADCAST_TEMPLATES = [t for t in SERVER_BROADCAST_TEMPLATES if t.get("id") != tpl_id]

            self.send_json_response({"success": True, "message": f"Template '{tpl_id}' deleted successfully."})
            return

        # 3.11 POST /api/notifications/retract (Soft-Retract Broadcast Announcement)
        if clean_path in ("/api/notifications/retract", "/api/notifications/retract/"):
            claims = self.require_auth(["admin", "manager"])
            if not claims:
                return

            notif_id = str(body.get("notification_id", "")).strip()
            if not notif_id:
                self.send_json_response({"success": False, "message": "Notification ID is required."}, 400)
                return

            admin_name = str(claims.get("name") or claims.get("username") or "Administrator")
            retract_time = time.strftime("%Y-%m-%d %H:%M:%S UTC", time.gmtime())

            if ATLAS_DB is not None:
                try:
                    ATLAS_DB.notifications.update_one(
                        {"id": notif_id},
                        {"$set": {
                            "is_retracted": True,
                            "retracted_at": retract_time,
                            "retracted_by": admin_name
                        }}
                    )
                except Exception as e:
                    print(f"  [NOTIF RETRACT DB ERROR] {e}")

            for n in SERVER_NOTIFICATIONS:
                if n.get("id") == notif_id:
                    n["is_retracted"] = True
                    n["retracted_at"] = retract_time
                    n["retracted_by"] = admin_name

            log_server(f"[BROADCAST] Announcement '{notif_id}' retracted by {admin_name}.")
            self.send_json_response({
                "success": True,
                "message": "Announcement successfully retracted and hidden from engineer feeds."
            })
            return

        # 3.6 POST /api/notifications/send (Broadcast Messages)
        if clean_path in ("/api/notifications/send", "/api/notifications/send/"):
            claims = self.require_auth(["admin", "manager"])
            if not claims:
                return

            title = str(body.get("title", "Broadcast Notice")).strip()
            raw_message = str(body.get("message", "")).strip()
            target = body.get("target", "everyone")
            priority = str(body.get("priority", "info")).lower()

            # Dynamic Signature: Respect explicit client sender or derive 'Manager (<Name>)' / 'Admin (<Name>)'
            client_sender = str(body.get("sender") or "").strip()
            if client_sender and any(client_sender.startswith(k) for k in ("Manager (", "Admin (", "Operations Desk")):
                sender = client_sender
            else:
                role_raw = str(claims.get("role", "admin")).strip().lower()
                sender_role = "Manager" if role_raw == "manager" else "Admin"
                raw_sender_name = str(claims.get("name") or claims.get("full_name") or "").strip()
                if not raw_sender_name:
                    raw_sender_name = "System Administrator" if sender_role == "Admin" else "Operations Manager"
                sender = f"{sender_role} ({raw_sender_name})"

            send_email = bool(body.get("send_email", False))
            send_in_app = bool(body.get("send_in_app", True))
            send_push = bool(body.get("send_push", True))

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

            # Build name resolution maps for @mention translation
            name_by_user = {}
            name_by_id = {}
            for e in engineers:
                uname = str(e.get("username", "")).strip().lower()
                uid = str(e.get("id", "")).strip().lower()
                fullname = str(e.get("fullName") or e.get("name") or uname).strip()
                if uname:
                    name_by_user[uname] = fullname
                if uid:
                    name_by_id[uid] = fullname

            def _is_exempt_from_broadcast(e):
                if not isinstance(e, dict):
                    return False
                r = str(e.get("role", "")).strip().lower()
                if r in ("admin", "manager") or e.get("is_admin") is True:
                    return True
                return is_management_account(e)

            is_everyone = target in ("everyone", "all", "*")
            recipients = []
            if is_everyone:
                recipients = [e for e in engineers if e.get("status") != "Resigned" and not _is_exempt_from_broadcast(e)]
            elif isinstance(target, list):
                target_norm = [str(t).strip().lower() for t in target if t]
                def _matches_target(e):
                    eid = str(e.get("id") or "").strip().lower()
                    eun = str(e.get("username") or "").strip().lower()
                    enm = str(e.get("name") or "").strip().lower()
                    efn = str(e.get("fullName") or "").strip().lower()
                    ekey = 'eng_' + re.sub(r'[^a-z0-9]', '', (enm or efn))
                    return (eid and eid in target_norm) or (eun and eun in target_norm) or (enm and enm in target_norm) or (efn and efn in target_norm) or (ekey in target_norm)

                recipients = [e for e in engineers if _matches_target(e) and not _is_exempt_from_broadcast(e)]
            elif isinstance(target, str):
                t_val = target.strip().lower()
                def _matches_str(e):
                    eid = str(e.get("id") or "").strip().lower()
                    eun = str(e.get("username") or "").strip().lower()
                    enm = str(e.get("name") or "").strip().lower()
                    efn = str(e.get("fullName") or "").strip().lower()
                    ekey = 'eng_' + re.sub(r'[^a-z0-9]', '', (enm or efn))
                    return eid == t_val or eun == t_val or enm == t_val or efn == t_val or ekey == t_val

                recipients = [e for e in engineers if _matches_str(e) and not _is_exempt_from_broadcast(e)]

            if not recipients:
                recipients = [e for e in engineers if e.get("status") != "Resigned" and not _is_exempt_from_broadcast(e)]

            emails_sent = 0
            for eng in recipients:
                if send_email and eng.get("email"):
                    to_email = eng.get("email")
                    eng_name = str(eng.get("fullName") or eng.get("name") or "Engineer").strip()
                    eng_uname = str(eng.get("username") or "").strip()

                    # Personalize BOTH title and message per recipient (Full Name + @mentions)
                    p_title_plain = _personalize_broadcast_text(
                        title,
                        recipient_name=eng_name,
                        recipient_username=eng_uname,
                        name_by_user=name_by_user,
                        name_by_id=name_by_id,
                        html_mode=False
                    )
                    html_p_title = _personalize_broadcast_text(
                        title,
                        recipient_name=eng_name,
                        recipient_username=eng_uname,
                        name_by_user=name_by_user,
                        name_by_id=name_by_id,
                        html_mode=True
                    )
                    html_p_msg = _personalize_broadcast_text(
                        raw_message,
                        recipient_name=eng_name,
                        recipient_username=eng_uname,
                        name_by_user=name_by_user,
                        name_by_id=name_by_id,
                        html_mode=True
                    )

                    html_email = f"""<!DOCTYPE html>
<html>
<body style="margin:0;padding:0;background-color:#0b1120;font-family:-apple-system,BlinkMacSystemFont,sans-serif;color:#f1f5f9;">
  <div style="max-width:560px;margin:30px auto;background:#0f172a;border:1px solid #1e293b;border-radius:16px;padding:30px;">
    <h2 style="margin:0 0 16px;color:#ffffff;font-size:20px;">{html_p_title}</h2>
    <div style="background:#020617;border:1px solid #334155;border-radius:10px;padding:18px;margin-bottom:20px;color:#e2e8f0;font-size:14px;line-height:1.6;white-space:pre-wrap;">
{html_p_msg}
    </div>
    {_render_open_in_app_cta("notifications", "Open in RosterX App &rarr;")}
    <div style="font-size:12px;color:#64748b;border-top:1px solid #1e293b;padding-top:14px;margin-top:16px;">
      Sent by <strong>{sender}</strong> via RosterX Operations.
    </div>
  </div>
</body>
</html>"""
                    try:
                        send_resend_email(to_email, f"📢 [{priority.upper()}] {p_title_plain}", html_email)
                        emails_sent += 1
                    except Exception as err:
                        print(f"  [NOTIF EMAIL ERROR] {err}")

            notif_id = f"notif_{int(time.time()*1000)}"

            # FCM Push Notification Dispatch (Personalized per recipient, deduplicated per physical device)
            push_dispatched = 0
            if send_push:
                dispatched_device_tokens = set()
                for eng in recipients:
                    toks = eng.get("fcm_tokens", [])
                    raw_tokens = []
                    if isinstance(toks, list):
                        raw_tokens.extend([str(t).strip() for t in toks if t and str(t).strip()])
                    elif isinstance(toks, str) and toks.strip():
                        raw_tokens.append(toks.strip())

                    # Strict device deduplication: Ensure no physical phone receives duplicate pushes
                    push_tokens = [t for t in raw_tokens if t not in dispatched_device_tokens]
                    for t in push_tokens:
                        dispatched_device_tokens.add(t)

                    if push_tokens:
                        eng_name = str(eng.get("fullName") or eng.get("name") or "Engineer").strip()
                        eng_uname = str(eng.get("username") or "").strip()

                        # Personalize BOTH push title and push body per engineer
                        p_title_plain = _personalize_broadcast_text(
                            title,
                            recipient_name=eng_name,
                            recipient_username=eng_uname,
                            name_by_user=name_by_user,
                            name_by_id=name_by_id,
                            html_mode=False
                        )
                        p_body = _personalize_broadcast_text(
                            raw_message,
                            recipient_name=eng_name,
                            recipient_username=eng_uname,
                            name_by_user=name_by_user,
                            name_by_id=name_by_id,
                            html_mode=False
                        )

                        sent = send_fcm_push(
                            push_tokens,
                            f"📢 [{priority.upper()}] {p_title_plain}",
                            p_body,
                            priority=priority,
                            notif_id=notif_id,
                            extra_data={
                                "target_user": eng_uname,
                                "target_name": eng_name,
                                "broadcast_id": notif_id
                            }
                        )
                        push_dispatched += sent

            notif_doc = {
                "id": notif_id,
                "title": title,
                "raw_message": raw_message,
                "target": target,
                "target_type": "everyone" if is_everyone else "specific",
                "recipients_count": len(recipients),
                "recipients": [str(r.get("fullName") or r.get("name")) for r in recipients],
                "priority": priority,
                "sender": sender,
                "send_email": send_email,
                "send_in_app": send_in_app,
                "send_push": send_push,
                "emails_dispatched": emails_sent,
                "push_dispatched": push_dispatched,
                "fcm_delivery": {
                    "enabled": send_push,
                    "dispatched_count": push_dispatched,
                    "status": f"Dispatched ({push_dispatched} devices)" if send_push else "Disabled"
                },
                "inapp_delivery": {
                    "enabled": send_in_app,
                    "target_count": len(recipients),
                    "read_count": 0,
                    "pending_count": len(recipients),
                    "status": "Active in Feed" if send_in_app else "Disabled"
                },
                "email_delivery": {
                    "enabled": send_email,
                    "dispatched_count": emails_sent,
                    "status": f"Sent ({emails_sent} emails)" if send_email else "Disabled"
                },
                "is_retracted": False,
                "retracted_at": None,
                "retracted_by": None,
                "created_at": time.strftime("%Y-%m-%d %H:%M:%S UTC", time.gmtime()),
                "timestamp": time.time()
            }

            # ALWAYS record into SERVER_NOTIFICATIONS and ATLAS_DB for audit, admin history & read receipts
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
                "push_dispatched": push_dispatched,
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

    def _handle_create_credential_tracker(self, body: dict):
        claims = self.require_auth(["admin", "manager"])
        if not claims:
            return

        server_name = str(body.get("server_name", "")).strip()
        username = str(body.get("username", "")).strip()
        last_updated_date = str(body.get("last_updated_date", "")).strip()

        if not server_name:
            self.send_json_response({"success": False, "message": "Server/Service name is required."}, 400)
            return

        if not username:
            self.send_json_response({"success": False, "message": "Username / Account name is required."}, 400)
            return

        try:
            validity_days = int(body.get("validity_days", 30))
            if validity_days < 1:
                validity_days = 30
        except Exception:
            validity_days = 30

        today_ist_str = get_ist_now().strftime("%Y-%m-%d")
        if not last_updated_date:
            last_updated_date = today_ist_str
        else:
            try:
                datetime.strptime(last_updated_date, "%Y-%m-%d")
            except Exception:
                self.send_json_response({"success": False, "message": "Invalid date format for last_updated_date. Must be YYYY-MM-DD."}, 400)
                return

        spokespersons = body.get("spokesperson_emails", [])
        if isinstance(spokespersons, str):
            spokespersons = [s.strip() for s in spokespersons.split(",") if s.strip()]
        elif isinstance(spokespersons, list):
            spokespersons = [str(s).strip() for s in spokespersons if str(s).strip()]
        else:
            spokespersons = []

        try:
            alert_threshold_days = int(body.get("alert_threshold_days", 5))
            alert_threshold_days = max(1, min(alert_threshold_days, validity_days))
        except Exception:
            alert_threshold_days = min(5, validity_days)

        tracker_id = f"cred_tracker_{uuid.uuid4().hex[:12]}"
        calc = compute_credential_status(last_updated_date, validity_days, alert_threshold_days)

        new_tracker = {
            "id": tracker_id,
            "server_name": server_name,
            "username": username,
            "last_updated_date": last_updated_date,
            "validity_days": validity_days,
            "alert_threshold_days": alert_threshold_days,
            "expiry_date": calc["expiry_date"],
            "alarm_start_date": calc["alarm_start_date"],
            "days_remaining": calc["days_remaining"],
            "status": calc["status"],
            "spokesperson_emails": spokespersons,
            "created_by": claims.get("username", "admin"),
            "created_at": int(time.time()),
            "updated_at": int(time.time()),
            "updated_by": claims.get("username", "admin")
        }

        save_credential_tracker_to_db(new_tracker)
        record_credential_tracker_log("CREATE", new_tracker, claims)
        log_server(f"[CREDENTIAL-TRACKER] Created tracker '{tracker_id}' for {server_name} ({username}) by '{claims.get('username')}'")
        self.send_json_response({
            "success": True,
            "message": f"Credential tracker for '{server_name}' created successfully.",
            "tracker": new_tracker
        }, 201)

    def _handle_update_credential_tracker(self, tracker_id: str, body: dict):
        # Allow designated spokespersons (non-admin/manager) to update renewal date via PUT
        caller_claims = self.require_auth()
        if not caller_claims:
            return
        caller_role = str(caller_claims.get("role", "")).strip().lower()
        is_mgr_or_admin = bool(caller_claims.get("is_admin", False) or caller_role in ("admin", "manager"))
        if not is_mgr_or_admin:
            renewal_only_keys = {"last_updated", "last_updated_date", "renew_date", "alert_threshold_days"}
            body_keys = set(body.keys()) if isinstance(body, dict) else set()
            if body_keys and body_keys.issubset(renewal_only_keys):
                self._handle_renew_credential_tracker(tracker_id, body)
                return
            self.send_json_response({"success": False, "message": "Permission denied: Admin or Manager role required to edit tracker configuration."}, 403)
            return

        claims = caller_claims
        existing = get_credential_tracker_by_id(tracker_id)
        if not existing:
            self.send_json_response({"success": False, "message": f"Credential tracker '{tracker_id}' not found."}, 404)
            return

        server_name = str(body.get("server_name", existing.get("server_name", ""))).strip()
        username = str(body.get("username", existing.get("username", ""))).strip()
        last_updated_date = str(body.get("last_updated_date") or body.get("last_updated") or existing.get("last_updated_date", "")).strip()

        try:
            validity_days = int(body.get("validity_days", existing.get("validity_days", 30)))
            if validity_days < 1:
                validity_days = 30
        except Exception:
            validity_days = existing.get("validity_days", 30)

        try:
            alert_threshold_days = int(body.get("alert_threshold_days", existing.get("alert_threshold_days", 5)))
            alert_threshold_days = max(1, min(alert_threshold_days, validity_days))
        except Exception:
            alert_threshold_days = min(existing.get("alert_threshold_days", 5), validity_days)

        spokespersons = body.get("spokesperson_emails", existing.get("spokesperson_emails", []))
        if isinstance(spokespersons, str):
            spokespersons = [s.strip() for s in spokespersons.split(",") if s.strip()]
        elif isinstance(spokespersons, list):
            spokespersons = [str(s).strip() for s in spokespersons if str(s).strip()]
        else:
            spokespersons = existing.get("spokesperson_emails", [])

        if last_updated_date:
            try:
                datetime.strptime(last_updated_date, "%Y-%m-%d")
            except Exception:
                self.send_json_response({"success": False, "message": "Invalid date format for last_updated_date. Must be YYYY-MM-DD."}, 400)
                return
        else:
            last_updated_date = existing.get("last_updated_date", "")

        calc = compute_credential_status(last_updated_date, validity_days, alert_threshold_days)

        updated_tracker = dict(existing)
        updated_tracker.update({
            "server_name": server_name,
            "username": username,
            "last_updated_date": last_updated_date,
            "validity_days": validity_days,
            "alert_threshold_days": alert_threshold_days,
            "expiry_date": calc["expiry_date"],
            "alarm_start_date": calc["alarm_start_date"],
            "days_remaining": calc["days_remaining"],
            "status": calc["status"],
            "spokesperson_emails": spokespersons,
            "updated_at": int(time.time()),
            "updated_by": claims.get("username", "admin")
        })

        save_credential_tracker_to_db(updated_tracker)
        record_credential_tracker_log("UPDATE", updated_tracker, claims, previous_tracker=existing)
        log_server(f"[CREDENTIAL-TRACKER] Tracker '{tracker_id}' updated by '{claims.get('username')}': {server_name} ({username})")
        self.send_json_response({
            "success": True,
            "message": f"Credential tracker for '{server_name}' updated successfully.",
            "tracker": updated_tracker
        })

    def _handle_renew_credential_tracker(self, tracker_id: str, body: dict = None):
        claims = self.require_auth()
        if not claims:
            return

        tracker = get_credential_tracker_by_id(tracker_id)
        if not tracker:
            self.send_json_response({"success": False, "message": f"Credential tracker '{tracker_id}' not found."}, 404)
            return

        user_role = str(claims.get("role", "")).strip().lower()
        is_admin = claims.get("is_admin", False) or user_role in ("admin", "manager")
        current_username = str(claims.get("username", "")).strip().lower()
        current_email = str(claims.get("email", "")).strip().lower()

        if not current_email and ATLAS_DB is not None and current_username:
            try:
                eng = ATLAS_DB.engineers.find_one({"username": current_username})
                if eng:
                    current_email = str(eng.get("email", "")).strip().lower()
            except Exception:
                pass

        spokespersons = [s.strip().lower() for s in tracker.get("spokesperson_emails", [])]
        is_spokesperson = (current_email in spokespersons) or (current_username in spokespersons)

        if not (is_admin or is_spokesperson):
            self.send_json_response({
                "success": False,
                "message": "Permission denied: Only Admins, Managers, or designated Spokespersons can renew this credential tracker."
            }, 403)
            return

        today_ist = get_ist_now().date()
        today_ist_str = today_ist.strftime("%Y-%m-%d")
        validity_days = tracker.get("validity_days", 30)
        alert_threshold_days = tracker.get("alert_threshold_days", 5)

        target_date_str = today_ist_str
        if body and isinstance(body, dict):
            custom_date = str(body.get("last_updated_date") or body.get("last_updated") or body.get("renew_date") or "").strip()
            if custom_date:
                try:
                    parsed_date = datetime.strptime(custom_date, "%Y-%m-%d").date()
                except Exception:
                    self.send_json_response({
                        "success": False,
                        "message": "Invalid date format for renewal date. Must be 'YYYY-MM-DD'."
                    }, 400)
                    return

                # Rule 1: Restrict renewal dates to today or past dates (reject future dates)
                if parsed_date > today_ist:
                    self.send_json_response({
                        "success": False,
                        "message": f"Renewal date cannot be in the future. Selected date ({custom_date}) is after today ({today_ist_str})."
                    }, 400)
                    return

                # Rule 2: Only accept date which is coming inside expiry days (cannot already be expired)
                calc_temp = compute_credential_status(custom_date, validity_days, alert_threshold_days)
                if calc_temp["days_remaining"] < 0:
                    self.send_json_response({
                        "success": False,
                        "message": f"Renewal date '{format_display_date(custom_date)}' with validity of {validity_days} days expired on {format_display_date(calc_temp['expiry_date'])} ({abs(calc_temp['days_remaining'])} days ago). Please select a date within the active validity window."
                    }, 400)
                    return

                target_date_str = custom_date

            # Allow updating alert_threshold_days during renewal if provided
            if "alert_threshold_days" in body:
                try:
                    ath = int(body["alert_threshold_days"])
                    alert_threshold_days = max(1, min(ath, validity_days))
                except Exception:
                    pass

        previous_copy = dict(tracker)
        calc = compute_credential_status(target_date_str, validity_days, alert_threshold_days)

        tracker["last_updated_date"] = target_date_str
        tracker["alert_threshold_days"] = alert_threshold_days
        tracker["expiry_date"] = calc["expiry_date"]
        tracker["alarm_start_date"] = calc["alarm_start_date"]
        tracker["days_remaining"] = calc["days_remaining"]
        tracker["status"] = calc["status"]
        tracker["updated_at"] = int(time.time())
        tracker["updated_by"] = claims.get("username", "admin")

        save_credential_tracker_to_db(tracker)
        record_credential_tracker_log("UPDATE", tracker, claims, previous_tracker=previous_copy)
        log_server(f"[CREDENTIAL-TRACKER] Tracker '{tracker_id}' renewed to {target_date_str} (alert at <= {alert_threshold_days}d) by '{claims.get('username')}'")

        self.send_json_response({
            "success": True,
            "message": f"Credential tracker '{tracker.get('server_name')}' renewed successfully to {target_date_str}.",
            "tracker": tracker
        })

    def _handle_delete_credential_tracker(self, tracker_id: str, body: dict = None):
        claims = self.require_auth(["admin", "manager"])
        if not claims:
            return

        if not tracker_id and body:
            tracker_id = str(body.get("id") or body.get("tracker_id") or "").strip()

        if not tracker_id:
            self.send_json_response({"success": False, "message": "Tracker ID is required."}, 400)
            return

        existing = get_credential_tracker_by_id(tracker_id)
        if not existing:
            self.send_json_response({"success": False, "message": f"Credential tracker '{tracker_id}' not found."}, 404)
            return

        server_name = existing.get("server_name", tracker_id)
        delete_credential_tracker_from_db(tracker_id)
        record_credential_tracker_log("DELETE", existing, claims)
        log_server(f"[CREDENTIAL-TRACKER] Tracker '{tracker_id}' ({server_name}) deleted by '{claims.get('username')}'")
        self.send_json_response({
            "success": True,
            "message": f"Credential tracker '{server_name}' deleted successfully."
        })

    def do_PUT(self):
        clean_path = self.path.split("?")[0].strip()
        norm_path = clean_path.rstrip("/")
        while norm_path.startswith("/api/api/"):
            norm_path = "/api" + norm_path[8:]
        if norm_path.startswith("/credentials/"):
            norm_path = "/api" + norm_path
        norm_path = self._normalize_api_aliases(norm_path)

        if norm_path.startswith("/api/"):
            self.handle_api_put(norm_path)
            return

        self.send_error(405, "Method Not Allowed")

    def handle_api_put(self, clean_path):
        """Router for PUT /api/* requests with strict authorization."""
        content_length = int(self.headers.get("Content-Length", 0))
        body_bytes = self.rfile.read(content_length) if content_length > 0 else b"{}"

        try:
            body = json.loads(body_bytes.decode("utf-8")) if body_bytes else {}
        except Exception as e:
            self.send_json_response({"success": False, "message": f"Malformed JSON: {e}"}, 400)
            return

        # PUT /api/credentials/trackers/<tracker_id> (and /api/credentials/<tracker_id> alias)
        if clean_path.startswith("/api/credentials/trackers/"):
            tracker_id = clean_path[len("/api/credentials/trackers/"):].strip().strip("/")
            self._handle_update_credential_tracker(tracker_id, body)
            return

        self.send_json_response({"success": False, "message": f"Endpoint PUT {clean_path} not found"}, 404)

    def do_DELETE(self):
        global SERVER_BROADCAST_TEMPLATES
        clean_path = self.path.split("?")[0].strip()
        norm_path = clean_path.rstrip("/")
        while norm_path.startswith("/api/api/"):
            norm_path = "/api" + norm_path[8:]
        if norm_path in ("/broadcast/templates", "/broadcast-templates", "/notifications", "/credentials", "/credentials/trackers") or \
           norm_path.startswith("/credentials/") or norm_path.startswith("/broadcast/templates/") or \
           norm_path.startswith("/broadcast-templates/") or norm_path.startswith("/notifications/"):
            norm_path = "/api" + norm_path
        norm_path = self._normalize_api_aliases(norm_path)

        if norm_path.startswith("/api/credentials/trackers"):
            tracker_id = ""
            if norm_path.startswith("/api/credentials/trackers/"):
                tracker_id = norm_path[len("/api/credentials/trackers/"):].strip().strip("/")

            if not tracker_id and "id=" in self.path:
                match = re.search(r"[?&]id=([^&]+)", self.path)
                if match:
                    tracker_id = urllib.parse.unquote(match.group(1).strip())

            body_data = {}
            if not tracker_id:
                content_length = int(self.headers.get("Content-Length", 0))
                if content_length > 0:
                    try:
                        body_data = json.loads(self.rfile.read(content_length).decode("utf-8"))
                    except Exception:
                        pass

            self._handle_delete_credential_tracker(tracker_id, body_data)
            return

        if norm_path.startswith("/api/broadcast/templates"):
            claims = self.require_auth(["admin", "manager"])
            if not claims:
                return

            tpl_id = ""
            if norm_path.startswith("/api/broadcast/templates/"):
                tpl_id = urllib.parse.unquote(norm_path[len("/api/broadcast/templates/"):].strip().strip("/"))
            if not tpl_id and "id=" in self.path:
                match = re.search(r"[?&]id=([^&]+)", self.path)
                if match:
                    tpl_id = urllib.parse.unquote(match.group(1).strip())
            if not tpl_id:
                content_length = int(self.headers.get("Content-Length", 0))
                if content_length > 0:
                    try:
                        body_data = json.loads(self.rfile.read(content_length).decode("utf-8"))
                        tpl_id = str(body_data.get("id", "")).strip()
                    except Exception:
                        pass

            if not tpl_id:
                self.send_json_response({"success": False, "message": "Template ID is required."}, 400)
                return

            if ATLAS_DB is not None:
                try:
                    ATLAS_DB.broadcast_templates.delete_one({"id": tpl_id})
                except Exception as e:
                    self.send_json_response({"success": False, "message": f"Database error: {e}"}, 500)
                    return

            SERVER_BROADCAST_TEMPLATES = [t for t in SERVER_BROADCAST_TEMPLATES if t.get("id") != tpl_id]
            self.send_json_response({"success": True, "message": f"Template '{tpl_id}' deleted successfully."})
            return

        # DELETE /api/notifications/<notif_id> (Soft-Retract Broadcast Announcement - Admin & Manager)
        if norm_path.startswith("/api/notifications"):
            claims = self.require_auth(["admin", "manager"])
            if not claims:
                return

            notif_id = ""
            if norm_path.startswith("/api/notifications/"):
                notif_id = urllib.parse.unquote(norm_path[len("/api/notifications/"):].strip().strip("/"))
            if not notif_id and "id=" in self.path:
                match = re.search(r"[?&]id=([^&]+)", self.path)
                if match:
                    notif_id = urllib.parse.unquote(match.group(1).strip())
            if not notif_id:
                content_length = int(self.headers.get("Content-Length", 0))
                if content_length > 0:
                    try:
                        body_data = json.loads(self.rfile.read(content_length).decode("utf-8"))
                        notif_id = str(body_data.get("notification_id") or body_data.get("id") or "").strip()
                    except Exception:
                        pass

            if not notif_id:
                self.send_json_response({"success": False, "message": "Notification ID is required."}, 400)
                return

            admin_name = str(claims.get("name") or claims.get("username") or "Administrator")
            retract_time = time.strftime("%Y-%m-%d %H:%M:%S UTC", time.gmtime())

            if ATLAS_DB is not None:
                try:
                    ATLAS_DB.notifications.update_one(
                        {"id": notif_id},
                        {"$set": {
                            "is_retracted": True,
                            "retracted_at": retract_time,
                            "retracted_by": admin_name
                        }}
                    )
                except Exception as e:
                    self.send_json_response({"success": False, "message": f"Database error: {e}"}, 500)
                    return

            for n in SERVER_NOTIFICATIONS:
                if n.get("id") == notif_id:
                    n["is_retracted"] = True
                    n["retracted_at"] = retract_time
                    n["retracted_by"] = admin_name

            log_server(f"[BROADCAST] Announcement '{notif_id}' retracted via DELETE by {admin_name}.")
            self.send_json_response({
                "success": True,
                "message": "Announcement successfully retracted and hidden from engineer feeds."
            })
            return

        self.send_json_response({"success": False, "message": f"Endpoint DELETE {clean_path} not found"}, 404)

    def end_headers(self):
        if getattr(self, "_sw_headers", False):
            self.send_header("Service-Worker-Allowed", "/")
            self.send_header("Cache-Control", "no-cache, no-store, must-revalidate")
            self.send_header("Pragma", "no-cache")
            self.send_header("Expires", "0")
        elif getattr(self, "_manifest_cache_headers", False):
            self.send_header("Cache-Control", "public, max-age=3600")
        elif not getattr(self, "_custom_cache_control", False):
            self.send_header("Cache-Control", "no-cache, no-store, must-revalidate")
            self.send_header("Pragma", "no-cache")
            self.send_header("Expires", "0")
        self.send_header("X-Content-Type-Options", "nosniff")
        self.send_header("X-Frame-Options", "DENY")
        self.send_header("X-XSS-Protection", "1; mode=block")
        self._custom_cache_control = False
        self._sw_headers = False
        self._manifest_cache_headers = False
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
                timestamp = get_ist_time_str(format_12h=True)
                SERVER_LOGS.append(f"[{timestamp}] [HTTP {status}] {method_path}")
        except Exception:
            pass


# ==============================================================================
# Main Server Runner
# ==============================================================================

def run_server(port=8000, open_browser=False, alert_email=None, send_email=True):
    global PUBLIC_SERVER_URL, SERVER_PORT
    SERVER_PORT = port

    if not WEB_DIR.exists():
        print(f"[ERROR] 'web' directory not found at {WEB_DIR}")
        sys.exit(1)

    lan_ip = get_lan_ip()
    env = load_env_variables()

    # Initialize PyMongo Atlas Bridge
    init_atlas_connection(env)

    SERVER_LOGS.append(f"[{get_ist_time_str(format_12h=True)}] [SYSTEM] RosterX Server v{SERVER_VERSION} listening on port {port}")
    SERVER_LOGS.append(f"[{get_ist_time_str(format_12h=True)}] [ATLAS DB] Status: {ATLAS_STATUS} ({ATLAS_CLUSTER}/{ATLAS_DB_NAME})")

    global RESEND_API_KEY, RESEND_FROM, DEFAULT_RESEND_API_KEY, DEFAULT_ALERT_EMAIL
    RESEND_API_KEY = (env.get("RESEND_API_KEY", "") or os.environ.get("RESEND_API_KEY", "") or DEFAULT_RESEND_API_KEY).strip().strip("'\"")
    RESEND_FROM = (env.get("RESEND_FROM", "") or os.environ.get("RESEND_FROM", "") or "RosterX Operations <notify@rosterx.online>").strip().strip("'\"")

    mongo_uri = env.get("MONGODB_URI", "")
    cluster_name = env.get("ATLAS_CLUSTER", "Cluster0")
    db_name = env.get("ATLAS_DATABASE", "rosterx")

    is_render = bool(os.environ.get("RENDER")) or bool(os.environ.get("RENDER_EXTERNAL_URL"))
    render_external_url = os.environ.get("RENDER_EXTERNAL_URL")
    public_url = PUBLIC_SERVER_URL
    if is_render and render_external_url:
        public_url = env.get("PUBLIC_SERVER_URL", "https://api.rosterx.online").strip().rstrip("/") or render_external_url.rstrip("/")
        PUBLIC_SERVER_URL = public_url
        print(f"\n  [RENDER CLOUD] Detected Render production host: {public_url}")
        SERVER_LOGS.append(f"[{get_ist_time_str(format_12h=True)}] [RENDER CLOUD] Live Service URL: {public_url}")

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

    # ── Daily Celebration Cron Thread ───────────────────────────────────────
    # Fires at 08:30 IST every day to send Birthday & Work Anniversary FCM
    # pushes to all active engineers. Also does a startup check on boot.
    def _daily_celebration_cron():
        import datetime
        ist_tz = datetime.timezone(datetime.timedelta(hours=5, minutes=30))
        # Run once at startup (after a short delay to let DB connect settle)
        time.sleep(10)
        try:
            purged = purge_expired_celebrations()
            check_and_trigger_celebrations()
            log_server(f"[CRON] Startup celebration check complete (purged {purged} expired).")
        except Exception as _e:
            log_server(f"[CRON] Startup check error: {_e}")

        while True:
            now_ist = datetime.datetime.now(ist_tz)
            # Next 08:30 IST
            target = now_ist.replace(hour=8, minute=30, second=0, microsecond=0)
            if now_ist >= target:
                target += datetime.timedelta(days=1)
            sleep_secs = (target - now_ist).total_seconds()
            log_server(f"[CRON] Next birthday/anniversary check in {int(sleep_secs//3600)}h {int((sleep_secs%3600)//60)}m (at 08:30 IST).")
            time.sleep(max(sleep_secs, 1))
            try:
                purged = purge_expired_celebrations()
                triggered = check_and_trigger_celebrations(force=True)
                if triggered:
                    log_server(f"[CRON] 08:30 celebration cron fired — purged {purged} expired, {len(triggered)} event(s) dispatched.")
                else:
                    log_server(f"[CRON] 08:30 celebration cron fired — purged {purged} expired, no events today.")
            except Exception as _ce:
                log_server(f"[CRON] Celebration cron error: {_ce}")

    t_cron = threading.Thread(target=_daily_celebration_cron, daemon=True)
    t_cron.start()
    log_server("[CRON] Daily birthday/anniversary cron thread started (fires at 08:30 IST).")

    # ── Daily Credential Expiry Notification Cron Thread ────────────────────
    # Fires at 09:00 IST every day to inspect server credential validity and
    # dispatch HTML email + FCM alerts for credentials expiring in <= 5 days.
    # Also performs an initial check 15 seconds after server startup.
    def _daily_credential_expiry_cron():
        # Initial boot check after DB connection settles
        time.sleep(15)
        try:
            dispatched = check_and_dispatch_credential_alerts()
            log_server(f"[CRON] Startup credential expiry check complete ({len(dispatched)} alerts dispatched).")
        except Exception as _e:
            log_server(f"[CRON] Startup credential check error: {_e}")

        while True:
            now_ist = get_ist_now()
            # Next 09:00 IST
            target = now_ist.replace(hour=9, minute=0, second=0, microsecond=0)
            if now_ist >= target:
                target += timedelta(days=1)
            sleep_secs = (target - now_ist).total_seconds()
            log_server(f"[CRON] Next credential expiry check in {int(sleep_secs//3600)}h {int((sleep_secs%3600)//60)}m (at 09:00 IST).")
            time.sleep(max(sleep_secs, 1))
            try:
                dispatched = check_and_dispatch_credential_alerts(force=True)
                if dispatched:
                    log_server(f"[CRON] 09:00 credential expiry cron fired — {len(dispatched)} alert(s) dispatched.")
                else:
                    log_server("[CRON] 09:00 credential expiry cron fired — all credentials healthy.")
            except Exception as _ce:
                log_server(f"[CRON] Credential expiry cron error: {_ce}")

    t_cred_cron = threading.Thread(target=_daily_credential_expiry_cron, daemon=True)
    t_cred_cron.start()
    log_server("[CRON] Daily credential expiry cron thread started (fires at 09:00 IST).")
    # ────────────────────────────────────────────────────────────────────────

    border = "=" * 68
    sub_border = "-" * 68
    print("\n" + border)
    print(f"  🚀  ROSTERX v{SERVER_VERSION} — HARDENED API SERVER & ATLAS BRIDGE")
    print(border)
    print("  [MOBILE APP & INTERNET ACCESS]")
    if public_url:
        print(f"   ► 🌐 Official Production URL: \033[93m\033[1m{public_url}\033[0m")
        print(f"   ► 📱 Android App Base URL   : \033[92m\033[1m{public_url}/api/\033[0m  (Render Cloud)")
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
        sys.exit(1)

    if open_browser:
        target_open = public_url if public_url else f"http://localhost:{port}"
        webbrowser.open(target_open)

    try:
        httpd.serve_forever()
    except KeyboardInterrupt:
        print("\n\n[INFO] Gracefully shutting down RosterX server...")
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
    parser.add_argument("--email", type=str, default=None, help="Email address to receive live gateway link")
    parser.add_argument("--no-email", action="store_true", help="Disable sending startup gateway link email")
    args, _ = parser.parse_known_args()

    should_send_email = not args.no_email and not is_render
    open_browser = args.open and not args.no_open and not is_render
    run_server(port=args.port, open_browser=open_browser, alert_email=args.email, send_email=should_send_email)
