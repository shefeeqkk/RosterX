# Project RosterX - Workspace Guidelines & Versioning

## Mandatory Versioning Standard
Whenever performing modifications and applying changes to this project:
- **Small Update / Patch**: Increment the patch (last) decimal (e.g., `1.0.0` -> `1.0.1`).
- **Semi-Big Update / Minor Feature**: Increment the middle (minor) decimal (e.g., `1.0.1` -> `1.1.0`).
- **Major Update**: Increment the primary (major) number (e.g., `1.1.0` -> `2.0.0`).

### Files to Update on Every Version Change:
1. Android project: `android/app/build.gradle.kts`:
   - `versionName`: update string to match semantic version (currently `"1.1.0"`).
   - `versionCode`: increment integer by +1 (currently `3`).
2. Server backend: `server.py` (`SERVER_VERSION = "1.1.0"`).
3. Cloudflare tunnel sync: `android/app/src/main/assets/server_url.txt` and `public_tunnel_url.txt`.

## 🔄 Two-Way Agent Synchronization ("Aunty" & "Kunjappan")

### 🤖 Agent Nicknames:
- **Aunty** (👵/⚡) = Antigravity Agent (You!)
- **Kunjappan** (🤖) = Android Studio Mobile Agent

### ⚡ Shortcut Commands:
Whenever the user types:
- **`Tell Kunjappan`**, **`Brief Kunjappan`**, **`Connect to Kunjappan`**, or **`Hey Aunty, sync with Kunjappan`**:
  👉 **Mandatory Action**: Immediately summarize recent backend changes, endpoints, version bumps, and pending mobile tasks directly into [`android/AGENT_HANDOFF.md`](./android/AGENT_HANDOFF.md) and confirm in a witty Aunty style!

### ⚙️ Nickname Customization (Internal to IDE only):
- Nicknames are defined in [`.agents/agent_config.json`](./.agents/agent_config.json).
- If the user asks to change or rename either agent (e.g., *"Rename Aunty to Jarvis"* or *"Change Kunjappan to Friday"*):
  1. Update the `default_name` and `aliases` in [`.agents/agent_config.json`](./.agents/agent_config.json) and both `GEMINI.md` files.
  2. Adopt the new names immediately in subsequent conversations.
  3. **Strict Boundary**: This is strictly an IDE-internal developer workflow. Never expose these agent nicknames in user-facing web pages, client portals, or Android mobile screens.
