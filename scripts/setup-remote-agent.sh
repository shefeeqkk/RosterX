#!/usr/bin/env bash
# ==============================================================================
# 👵⚡ AUNTY'S ANTIGRAVITY REMOTE AGENT BOOTSTRAPPER (RosterX v1.15.0)
# ==============================================================================
# This script provisions the Antigravity Cloud Remote Agent (Linux/Debian/Ubuntu)
# for zero-touch Android building, testing, and Tailscale wireless ADB debugging.
# ==============================================================================

set -euo pipefail

echo "======================================================================"
echo "👵⚡ Setting up RosterX Antigravity Remote Agent Environment..."
echo "======================================================================"

# 1. Update OS packages & install system build dependencies
echo "📦 [1/6] Installing Linux packages (JDK 17, ADB, Python, Git, Curl)..."
if command -v apt-get >/dev/null 2>&1; then
    sudo apt-get update -y
    sudo apt-get install -y --no-install-recommends \
        openjdk-17-jdk \
        adb \
        curl \
        wget \
        jq \
        git \
        netcat-openbsd \
        python3 \
        python3-pip \
        python3-venv
else
    echo "⚠️ Non-Debian OS detected. Please ensure OpenJDK 17 and ADB are installed."
fi

# 2. Configure Java Environment
echo "☕ [2/6] Configuring JDK 17 environment..."
JAVA_CANDIDATE="/usr/lib/jvm/java-17-openjdk-amd64"
if [ -d "$JAVA_CANDIDATE" ]; then
    export JAVA_HOME="$JAVA_CANDIDATE"
    echo "export JAVA_HOME=$JAVA_CANDIDATE" >> "$HOME/.bashrc"
fi
java -version

# 3. Install & Configure Tailscale WireGuard Mesh
echo "🌐 [3/6] Installing Tailscale mesh network..."
if ! command -v tailscale >/dev/null 2>&1; then
    curl -fsSL https://tailscale.com/install.sh | sh
fi

# 4. Connect Tailscale headlessly if TAILSCALE_AUTHKEY is provided
echo "🔑 [4/6] Connecting to Tailscale Tailnet..."
if [ -n "${TAILSCALE_AUTHKEY:-}" ]; then
    echo "Connecting headlessly via TAILSCALE_AUTHKEY..."
    sudo tailscale up \
        --authkey="${TAILSCALE_AUTHKEY}" \
        --hostname="antigravity-remote-rosterx" \
        --accept-routes \
        --reset
    echo "✅ Tailscale connected! Local Tailscale IP: $(tailscale ip -4 || true)"
else
    if sudo tailscale status >/dev/null 2>&1; then
        echo "✅ Tailscale is already logged in and active."
    else
        echo "⚠️ TAILSCALE_AUTHKEY environment variable not set."
        echo "👉 Run 'sudo tailscale up' or set TAILSCALE_AUTHKEY in Remote Agent Secrets."
    fi
fi

# 5. Make Android Gradle wrapper executable
echo "🔨 [5/6] Ensuring Gradle wrapper permissions..."
if [ -f "android/gradlew" ]; then
    chmod +x android/gradlew
    echo "✅ android/gradlew marked executable."
else
    echo "⚠️ android/gradlew not found at $(pwd)/android/gradlew"
fi

# 6. Python Environment Setup
echo "🐍 [6/6] Setting up Python dependencies..."
if [ -f "requirements.txt" ]; then
    pip3 install --break-system-packages -r requirements.txt || pip3 install -r requirements.txt
    echo "✅ Python dependencies installed."
fi

echo "======================================================================"
echo "🎉 Remote Agent Setup Complete! Ready for wireless deployment."
echo "👉 Run './scripts/remote-deploy.sh' to compile and deploy to your phone."
echo "======================================================================"
