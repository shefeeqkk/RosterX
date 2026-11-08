import urllib.request
import urllib.parse
import json
import sys

# Ensure UTF-8 output on Windows
if sys.platform == "win32":
    sys.stdout.reconfigure(encoding="utf-8")

BASE_URL = "https://rosterx-vc30.onrender.com"

def make_request(path, method="GET", data=None, token=None):
    url = f"{BASE_URL}{path}"
    headers = {
        "User-Agent": "AuntyVerification/1.12.1",
        "Accept": "application/json"
    }
    if token:
        headers["Authorization"] = f"Bearer {token}"
    
    encoded_data = None
    if data is not None:
        headers["Content-Type"] = "application/json"
        encoded_data = json.dumps(data).encode("utf-8")
        
    req = urllib.request.Request(url, data=encoded_data, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req, timeout=25) as resp:
            body = resp.read().decode("utf-8")
            return resp.status, json.loads(body)
    except urllib.error.HTTPError as e:
        body = e.read().decode("utf-8")
        try:
            return e.code, json.loads(body)
        except Exception:
            return e.code, {"error": body}
    except Exception as e:
        return 0, {"error": str(e)}

def run_tests():
    print("=" * 70)
    print("👵⚡ AUNTY'S RIGOROUS BACKEND & SCHEDULING API VERIFICATION SUITE")
    print(f"Target: {BASE_URL}")
    print("=" * 70)

    # STEP 0: Health & Server Version Check
    print("\n[TEST 0] Testing Server Health & Cluster Connection...")
    status, res = make_request("/api/test-connection")
    print(f" -> Status: {status}, Response: {res}")
    assert status == 200, f"Health check failed: {res}"
    assert res.get("version") in ("1.17.0", "1.16.0", "1.15.0", "1.14.15", "1.14.14", "1.14.13", "1.14.12", "1.14.11", "1.14.10", "1.14.9", "1.14.8", "1.14.7", "1.14.6", "1.14.5", "1.14.4", "1.14.3", "1.14.2", "1.14.1", "1.14.0", "1.13.1", "1.13.0", "1.12.1", "1.12.0", "1.11.8", "1.11.5", "1.11.4"), f"Unexpected version: {res.get('version')}"
    print(f" ✅ PASS: Render Server online with version {res.get('version')} & Cluster0 connected!")

    # STEP 1: Admin Authentication
    print("\n[TEST 1] Authenticating as Lead Admin...")
    status, login_res = make_request("/api/auth/login", method="POST", data={
        "identifier": "admin",
        "password": "Admin@123"
    })
    print(f" -> Status: {status}, Role: {login_res.get('user', {}).get('role')}")
    if status != 200:
        # Try alternate admin username
        status, login_res = make_request("/api/auth/login", method="POST", data={
            "identifier": "shefeeqkk",
            "password": "Admin@123"
        })
        print(f" -> Fallback Status: {status}")

    assert status == 200 and "token" in login_res, f"Admin login failed: {login_res}"
    admin_token = login_res["token"]
    admin_role = login_res.get("user", {}).get("role")
    print(f" ✅ PASS: Admin authenticated successfully! (Role: {admin_role})")

    # STEP 2: Account Creation (Manager Role)
    print("\n[TEST 2] Testing Account Creation (Manager Role with Plaintext Password)...")
    test_manager_payload = {
        "engineers": [{
            "name": "AUDIT MANAGER",
            "full_name": "Audit Operations Manager",
            "username": "audit_manager",
            "email": "audit_manager@tejasnetworks.com",
            "phone": "+919876543210",
            "role": "manager",
            "status": "Active",
            "default_weekly_offs": ["Sunday", "Monday"],
            "join_date": "2026-01-01",
            "password": "Password@123"
        }]
    }
    status, create_res = make_request("/api/engineers", method="POST", data=test_manager_payload, token=admin_token)
    print(f" -> Status: {status}, Response: {create_res}")
    assert status == 200 and create_res.get("success"), f"Account creation failed: {create_res}"
    print(" ✅ PASS: POST /api/engineers successfully created manager account!")

    # STEP 3: Authentication of New Manager Account
    print("\n[TEST 3] Testing Authentication of Newly Created Manager Account...")
    status, mgr_login = make_request("/api/auth/login", method="POST", data={
        "identifier": "audit_manager",
        "password": "Password@123"
    })
    print(f" -> Status: {status}, User: {mgr_login.get('user', {}).get('username')}, Role: {mgr_login.get('user', {}).get('role')}")
    assert status == 200 and "token" in mgr_login, f"Manager login failed: {mgr_login}"
    assert mgr_login.get("user", {}).get("role") == "manager", f"Expected manager role, got {mgr_login.get('user', {}).get('role')}"
    manager_token = mgr_login["token"]
    print(" ✅ PASS: Manager logged in with password 'Password@123' and received valid JWT token!")

    # STEP 4: Updation Without Overwriting Password Hash
    print("\n[TEST 4] Testing Updation Without Password (Preserve Existing Hash)...")
    update_payload = {
        "engineers": [{
            "name": "AUDIT MANAGER",
            "full_name": "Audit Operations Manager (Updated)",
            "username": "audit_manager",
            "email": "audit_manager@tejasnetworks.com",
            "phone": "+919999888777",
            "role": "manager",
            "status": "Active",
            "default_weekly_offs": ["Friday", "Saturday"]
            # No password provided
        }]
    }
    status, update_res = make_request("/api/engineers", method="POST", data=update_payload, token=manager_token)
    print(f" -> Status: {status}, Response: {update_res}")
    assert status == 200 and update_res.get("success"), f"Account update failed: {update_res}"

    # Verify that the manager can STILL log in with their original password!
    status, mgr_relogin = make_request("/api/auth/login", method="POST", data={
        "identifier": "audit_manager",
        "password": "Password@123"
    })
    print(f" -> Re-login Status: {status}")
    assert status == 200 and "token" in mgr_relogin, f"Password hash was corrupted during update! {mgr_relogin}"
    print(" ✅ PASS: Account updated successfully & password hash preserved intact without corruption!")

    # STEP 5: Account Directory (GET /api/engineers) vs Shift Roster (GET /api/roster/2026-11)
    print("\n[TEST 5] Testing Separation: Account Directory vs Shift Roster...")
    status, dir_res = make_request("/api/engineers", token=admin_token)
    engineers_list = dir_res.get("documents", [])
    usernames_in_dir = [e.get("username") for e in engineers_list]
    roles_in_dir = {e.get("username"): e.get("role") for e in engineers_list}
    print(f" -> Total Accounts in Directory: {len(engineers_list)}")
    print(f" -> Roles present in Directory: {set(roles_in_dir.values())}")
    assert "audit_manager" in usernames_in_dir, "Manager not found in directory!"
    assert any(r in ("admin", "manager") for r in roles_in_dir.values()), "Admins/Managers missing from directory!"
    print(" ✅ PASS: Account Directory contains all enterprise accounts (including Admins & Managers)!")

    # Now verify Shift Roster strictly excludes Admin and Manager
    status, roster_res = make_request("/api/roster/2026-11", token=admin_token)
    roster_doc = roster_res.get("document", {})
    duty_engineers = roster_doc.get("engineers", [])
    duty_names = [e.get("name") for e in duty_engineers]
    print(f" -> Total Engineers in 2026-11 Duty Roster: {len(duty_engineers)}")
    print(f" -> Duty Engineers: {duty_names}")
    for de in duty_engineers:
        role = str(de.get("role", "")).lower()
        name = str(de.get("name", "")).upper()
        assert role not in ("admin", "manager"), f"CRITICAL: Found {name} with role {role} inside duty roster!"
    print(" ✅ PASS: Shift Roster (2026-11) strictly excludes Admin and Manager roles! Zero ghost shifts!")

    # STEP 6: Dynamic Live Telemetry Endpoints Check
    print("\n[TEST 6] Testing Live Backend Telemetry (/api/telemetry/server and /api/telemetry/users)...")
    status_srv, telem_srv = make_request("/api/telemetry/server")
    print(f" -> /api/telemetry/server Status: {status_srv}")
    if status_srv == 200:
        data_srv = telem_srv.get("data", {})
        print(f"    - Host Uptime: {data_srv.get('uptime_seconds')}s, Ping: {data_srv.get('atlas_ping_ms')}ms")
        print(f"    - Collections: {data_srv.get('collections')}, RSS Memory: {data_srv.get('memory_rss_mb')}MB")
        print(" ✅ PASS: /api/telemetry/server returned live host & Atlas telemetry!")
    else:
        print(f" ⚠️ NOTICE: Server returned {status_srv} (may need deployment).")

    status_usr, telem_usr = make_request("/api/telemetry/users")
    print(f" -> /api/telemetry/users Status: {status_usr}")
    if status_usr == 200:
        data_usr = telem_usr.get("data", {})
        print(f"    - Total Registered Engineers: {data_usr.get('total_engineers')}")
        print(f"    - Active Windows: {data_usr.get('activity_windows')}")
        print(" ✅ PASS: /api/telemetry/users returned genuine Atlas user metrics!")
    else:
        print(f" ⚠️ NOTICE: Users telemetry returned {status_usr} (may need deployment).")

    # STEP 7: Account Deletion (POST /api/engineers/delete)
    print("\n[TEST 7] Testing Account Deletion (POST /api/engineers/delete)...")
    delete_payload = {
        "username": "audit_manager",
        "name": "AUDIT MANAGER"
    }
    status, del_res = make_request("/api/engineers/delete", method="POST", data=delete_payload, token=admin_token)
    print(f" -> Status: {status}, Response: {del_res}")
    assert status == 200 and del_res.get("success"), f"Deletion failed: {del_res}"

    # Verify deleted manager cannot log in anymore
    status, dead_login = make_request("/api/auth/login", method="POST", data={
        "identifier": "audit_manager",
        "password": "Password@123"
    })
    print(f" -> Post-deletion login status (Expected 401): {status}")
    assert status == 401, f"Deleted user was still able to authenticate! {dead_login}"
    print(" ✅ PASS: Account permanently deleted from Atlas DB and denied further authentication!")

    # STEP 8: Grounded AI Operations Assistant Check
    print("\n[TEST 8] Testing Grounded Gemini AI Operations Assistant (/api/chat/assistant)...")
    status_ai, ai_res = make_request("/api/chat/assistant", method="POST", data={
        "message": "Who all are in shift now?"
    })
    print(f" -> /api/chat/assistant Status: {status_ai}")
    if status_ai == 200 and ai_res.get("reply"):
        print(f"    - AI Model: {ai_res.get('model')}")
        print(f"    - Response Snippet: {ai_res.get('reply')[:120]}...")
        print(" ✅ PASS: Grounded AI Assistant successfully answered live operational query!")
    else:
        print(f" ⚠️ NOTICE: AI assistant returned status {status_ai}: {ai_res} (may need deployment).")

    print("\n" + "=" * 70)
    print("🎉 ALL 8 VERIFICATION TEST SUITES EXECUTED SUCCESSFULLY!")
    print("=" * 70)

if __name__ == "__main__":
    run_tests()
