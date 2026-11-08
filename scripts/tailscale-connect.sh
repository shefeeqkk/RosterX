#!/usr/bin/env bash
# ==============================================================================
# 👵⚡ AUNTY'S TAILSCALE STATUS & PHONE CONNECTIVITY CHECKER
# ==============================================================================

set -euo pipefail

PHONE_IP="${1:-${TAILSCALE_PHONE_IP:-100.71.241.36}}"
PORT="${2:-${ADB_PORT:-5555}}"

echo "======================================================================"
echo "👵⚡ Tailscale Network & Peer Diagnostics"
echo "Target Phone: ${PHONE_IP}:${PORT}"
echo "======================================================================"

if ! command -v tailscale >/dev/null 2>&1; then
    echo "❌ Tailscale is not installed on this machine."
    exit 1
fi

echo "📊 [1] Tailscale Local Status:"
tailscale status || true
echo ""

echo "🌐 [2] Local Tailscale IP:"
tailscale ip -4 || true
echo ""

echo "📡 [3] Tailscale Ping to Phone (${PHONE_IP}):"
tailscale ping -c 3 "${PHONE_IP}" || echo "Ping failed or ICMP filtered."
echo ""

echo "🔌 [4] ADB Port ${PORT} Socket Reachability Check:"
if command -v nc >/dev/null 2>&1; then
    if nc -zv -w 3 "${PHONE_IP}" "${PORT}" 2>&1; then
        echo "✅ Port ${PORT} is open and accepting TCP connections!"
    else
        echo "❌ Port ${PORT} is closed or unreachable."
    fi
else
    python3 -c "
import socket
s = socket.socket()
s.settimeout(3)
try:
    s.connect(('${PHONE_IP}', int('${PORT}')))
    print('✅ Port ${PORT} is open and reachable via Python socket test!')
    s.close()
except Exception as e:
    print('❌ Port ${PORT} connection error:', e)
"
fi

echo "======================================================================"
