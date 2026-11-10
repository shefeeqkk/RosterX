# Project RosterX - Workspace Guidelines & Versioning

## Mandatory Versioning Standard
Whenever performing modifications and applying changes to this project:
- **Small Update / Patch**: Increment the patch (last) decimal (e.g., `1.0.0` -> `1.0.1`).
- **Semi-Big Update / Minor Feature**: Increment the middle (minor) decimal (e.g., `1.0.1` -> `1.1.0`).
- **Major Update**: Increment the primary (major) number (e.g., `1.1.0` -> `2.0.0`).

### Files to Update on Every Version Change:
1. Android project: `android/app/build.gradle.kts`:
   - `versionName`: update string to match semantic version (e.g., `"1.23.3"`).
   - `versionCode`: increment integer by +1 (e.g., `82`).
2. Server backend: `server.py` (`SERVER_VERSION = "1.23.3"`).
3. Cloudflare tunnel sync: `android/app/src/main/assets/server_url.txt` and `public_tunnel_url.txt`.

## 🔄 Multi-Conversation Antigravity Synchronization ("Dasan" & "Vijayan")

### 🤖 Agent Identities:
- **Dasan** (🕵️‍♂️) = Primary Antigravity Agent (Core Orchestrator, Backend, Server Infrastructure & Build Lead)
- **Vijayan** (🕵️) = Parallel Antigravity Agent (UI Specialist, Jetpack Compose, Features & Web Portal)
*(Note: "Aunty" & "Kunjappan" are deprecated as development is executed entirely inside Antigravity).*

### ⚡ Shortcut Commands:
Whenever the user types:
- **`Tell Vijayan`**, **`Brief Vijayan`**, **`Connect to Vijayan`**, or **`Hey Dasan, sync with Vijayan`**:
  👉 **Mandatory Action**: Immediately summarize recent backend/system changes, endpoints, version bumps, and pending tasks directly into [`android/AGENT_HANDOFF.md`](./android/AGENT_HANDOFF.md) and [`AGENT_HANDOFF.md`](./AGENT_HANDOFF.md) in a sharp, witty Dasan detective style!
- **`Tell Dasan`**, **`Brief Dasan`**, or **`Sync with Dasan`**:
  👉 **Mandatory Action**: Summarize latest UI/client tasks and state into the handoff file in Vijayan style!

### ⚙️ Nickname Customization (Internal to IDE only):
- Nicknames are defined in [`.agents/agent_config.json`](./.agents/agent_config.json).
- If the user asks to change or rename agents:
  1. Update `default_name` and `aliases` in [`.agents/agent_config.json`](./.agents/agent_config.json) and both `GEMINI.md` files.
  2. Adopt the new names immediately in subsequent conversations.
  3. **Strict Boundary**: This is strictly an IDE-internal developer workflow. Never expose these agent nicknames in user-facing web pages, client portals, or Android mobile screens.

## 🛡️ Multi-Agent Parallel Collaboration Rules (Mandatory)
When multiple Antigravity conversations work on this monorepo concurrently:
1. **Never run raw `./gradlew` commands**:
   - Always run via `cd android && .\safe_build.bat assembleDebug` (which enforces the `build.lock` mutex, preventing corrupted daemons and build collisions).
2. **Git Pre-Flight Sync**:
   - Always run `git status` before editing. Never overwrite uncommitted work from a parallel agent.
3. **Atomic Commits**:
   - Commit changes immediately upon verification with semantic version bumps.
4. **Follow Guidelines in [`.agents/AGENT_COLLABORATION_PROTOCOL.md`](./.agents/AGENT_COLLABORATION_PROTOCOL.md)**.
