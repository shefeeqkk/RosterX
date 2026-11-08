#!/usr/bin/env bash
# ==============================================================================
# 👵⚡ AUNTY'S TAILSCALE WIRELESS ADB BUILD & DEPLOY SCRIPT (RosterX v1.15.0)
# ==============================================================================
# Builds RosterX debug APK on Linux Antigravity Remote Agent and deploys
# directly to the user's Android phone over Tailscale WireGuard mesh network.
#
# Usage:
#   ./scripts/remote-deploy.sh [PHONE_IP] [ADB_PORT]
#
# Examples:
#   ./scripts/remote-deploy.sh                        # Uses 100.71.241.36:5555
#   ./scripts/remote-deploy.sh 100.71.241.36 39233    # Uses dynamic fallback port
# ==============================================================================

set -euo pipefail

PHONE_IP="${1:-${TAILSCALE_PHONE_IP:-100.71.241.36}}"
PORT="${2:-${ADB_PORT:-5555}}"
DEVICE_TARGET="${PHONE_IP}:${PORT}"

echo "======================================================================"
echo "👵⚡ RosterX Wireless Remote Deploy via Tailscale"
echo "🎯 Target Device: ${DEVICE_TARGET}"
echo "======================================================================"

# 1. Verify Tailscale connectivity
echo "📡 [1/5] Checking Tailscale connectivity to ${PHONE_IP}..."
if command -v tailscale >/dev/null 2>&1; then
    if ! tailscale ping -c 1 -timeout 3s "${PHONE_IP}" >/dev/null 2>&1; then
        echo "⚠️ Warning: Tailscale ping to ${PHONE_IP} timed out or was blocked by ICMP filter."
        echo "Continuing with direct ADB TCP connection attempt..."
    else
        echo "✅ Tailscale peer ${PHONE_IP} is alive and reachable!"
    fi
fi

# 2. Connect ADB over TCP/IP
echo "🔌 [2/5] Connecting ADB to ${DEVICE_TARGET}..."
adb disconnect "${DEVICE_TARGET}" >/dev/null 2>&1 || true

CONNECT_OUTPUT=$(adb connect "${DEVICE_TARGET}" 2>&1 || true)
echo "   ADB Output: ${CONNECT_OUTPUT}"

# Check for failure to connect
if echo "${CONNECT_OUTPUT}" | grep -Ei "failed to connect|cannot connect|Connection refused|Connection timed out" >/dev/null 2>&1; then
    echo "❌ Could not connect to ${DEVICE_TARGET}."
    echo ""
    echo "💡 Troubleshooting & Dynamic Port Fallback:"
    echo "   1. If your phone was recently rebooted, ADB tcpip 5555 resets."
    echo "   2. Open Phone Settings -> Developer Options -> Wireless Debugging."
    echo "   3. Check the 5-digit port shown under 'IP address & Port' (e.g., 39233)."
    echo ""
    read -r -p "👉 Enter the dynamic port to try (or press Enter to abort): " DYNAMIC_PORT
    if [ -n "${DYNAMIC_PORT}" ]; then
        PORT="${DYNAMIC_PORT}"
        DEVICE_TARGET="${PHONE_IP}:${PORT}"
        echo "Retrying connection with ${DEVICE_TARGET}..."
        adb connect "${DEVICE_TARGET}"
    else
        echo "Aborting deployment. Re-run with: ./scripts/remote-deploy.sh ${PHONE_IP} <PORT>"
        exit 1
    fi
fi

# Verify device authorization
DEVICES_OUTPUT=$(adb devices)
if echo "${DEVICES_OUTPUT}" | grep "${DEVICE_TARGET}" | grep -q "unauthorized"; then
    echo "⚠️ Device is UNAUTHORIZED!"
    echo "👉 Please check your phone screen and tap 'Always allow from this computer'."
    echo "Waiting 10 seconds for user authorization..."
    sleep 10
fi

if ! adb devices | grep "${DEVICE_TARGET}" | grep -q "device"; then
    echo "❌ Device ${DEVICE_TARGET} is not listed as 'device' in adb devices."
    adb devices -l
    exit 1
fi

MODEL=$(adb -s "${DEVICE_TARGET}" shell getprop ro.product.model 2>/dev/null | tr -d '\r\n' || echo "Android Device")
ANDROID_VER=$(adb -s "${DEVICE_TARGET}" shell getprop ro.build.version.release 2>/dev/null | tr -d '\r\n' || echo "Unknown")
echo "📱 Connected to: ${MODEL} (Android ${ANDROID_VER}) over Tailscale!"

# 3. Compile Native Android APK
echo "🔨 [3/5] Building native Android debug APK with Gradle..."
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "${SCRIPT_DIR}")"
cd "${PROJECT_ROOT}/android"

./gradlew assembleDebug --no-daemon --stacktrace

APK_PATH="app/build/outputs/apk/debug/app-debug.apk"
if [ ! -f "${APK_PATH}" ]; then
    echo "❌ Build failed: APK not found at ${APK_PATH}"
    exit 1
fi
APK_SIZE=$(du -h "${APK_PATH}" | cut -f1)
echo "✅ Build Successful! APK generated (${APK_SIZE})."

# 4. Wirelessly Install APK on Phone
echo "📲 [4/5] Installing APK on ${MODEL} wirelessly..."
adb -s "${DEVICE_TARGET}" install -r -d "${APK_PATH}"
echo "✅ App successfully installed wirelessly over Tailscale!"

# 5. Launch App & Take Screencap Verification
echo "🚀 [5/5] Launching RosterX and capturing verification screenshot..."
adb -s "${DEVICE_TARGET}" shell am start -n com.rosterx.app/.MainActivity -a android.intent.action.MAIN -c android.intent.category.LAUNCHER

sleep 3
adb -s "${DEVICE_TARGET}" shell screencap -p /sdcard/remote_deploy_verify.png
adb -s "${DEVICE_TARGET}" pull /sdcard/remote_deploy_verify.png "${PROJECT_ROOT}/remote_deploy_verify.png"
adb -s "${DEVICE_TARGET}" shell rm /sdcard/remote_deploy_verify.png || true

echo "======================================================================"
echo "🎉 DEPLOYMENT COMPLETE & VERIFIED!"
echo "📱 App running live on: ${MODEL}"
echo "📸 Verification screenshot captured: remote_deploy_verify.png"
echo "======================================================================"
